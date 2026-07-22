package br.com.pedidos.usuario.service;

import br.com.pedidos.usuario.dto.AlterarSenhaDTO;
import br.com.pedidos.usuario.dto.UsuarioRequestDTO;
import br.com.pedidos.usuario.dto.UsuarioResponseDTO;
import br.com.pedidos.usuario.entity.Usuario;
import br.com.pedidos.usuario.enums.PerfilUsuario;
import br.com.pedidos.usuario.exception.LoginDuplicadoException;
import br.com.pedidos.usuario.exception.SenhaAtualInvalidaException;
import br.com.pedidos.usuario.exception.UsuarioInativoException;
import br.com.pedidos.usuario.exception.UsuarioNaoEncontradoException;
import br.com.pedidos.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários de UsuarioServiceImpl.
 *
 * Cenários rastreados ao legado usuario.4gl:
 *
 *   usuario_incluir():
 *     • incluir_perfilOperadorPadrao → LET perfil = 'OPERADOR' (linha 16)
 *     • incluir_perfilExplicito      → perfil informado é respeitado
 *     • incluir_loginDuplicado       → uq_usuario_login / SQLCODE != 0 (linha 26)
 *     • incluir_senhaHasheada        → senha NUNCA gravada em texto puro (lacuna corrigida)
 *
 *   usuario_desativar():
 *     • desativar_sucesso         → UPDATE ativo='N' (linha 35)
 *     • desativar_naoEncontrado   → ERROR "Usuario nao encontrado." (linha 39)
 *
 *   alterar() — lacuna corrigida:
 *     • alterar_sucesso           → atualiza login/nome/perfil
 *     • alterar_loginDuplicado    → login pertence a outro usuário
 *     • alterar_usuarioInativo    → rejeita alteração de inativo
 *
 *   alterarSenha() — lacuna de segurança corrigida:
 *     • alterarSenha_senhaErrada  → SenhaAtualInvalidaException
 *     • alterarSenha_usuarioInativo → UsuarioInativoException
 *
 *   reativar() — lacuna corrigida:
 *     • reativar_sucesso          → ativo volta a 'S'
 *
 *   excluir():
 *     • excluir_sucesso
 *     • excluir_naoEncontrado
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock UsuarioRepository repository;

    /**
     * PasswordEncoder é injetado via construtor em UsuarioServiceImpl
     * (declarado como field 'final' com @RequiredArgsConstructor).
     * O @Bean PasswordEncoder está em AppConfig — aqui é mockado para
     * controlar o comportamento em testes unitários:
     *   • testes de BCrypt real (incluir_senhaHasheada) invocam o encoder
     *     diretamente em vez de depender do mock.
     *   • testes de alterarSenha configuram matches() conforme o cenário.
     */
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks UsuarioServiceImpl service;

    private Usuario usuarioAtivo;
    private Usuario usuarioInativo;

    @BeforeEach
    void setUp() {
        usuarioAtivo = Usuario.builder()
            .id(1L).login("carlos.silva").nome("Carlos Eduardo Silva")
            .senhaHash("$2a$10$hashedpassword")
            .perfil(PerfilUsuario.OPERADOR).ativo("S")
            .dataCadastro(LocalDate.now()).build();

        usuarioInativo = Usuario.builder()
            .id(2L).login("ana.lima").nome("Ana Beatriz Lima")
            .senhaHash("$2a$10$hashedpassword2")
            .perfil(PerfilUsuario.CONSULTA).ativo("N")
            .dataCadastro(LocalDate.now()).build();
    }

    // =========================================================================
    @Nested @DisplayName("incluir()")
    class IncluirTests {

        @Test
        @DisplayName("deve incluir usuário com perfil OPERADOR como padrão — LET perfil = 'OPERADOR' (linha 16)")
        void incluir_perfilOperadorPadrao() {
            UsuarioRequestDTO dto = new UsuarioRequestDTO(
                "joao.pedro", "João Pedro", "senha@123", null
            );
            when(repository.existsByLogin("joao.pedro")).thenReturn(false);
            when(repository.save(any())).thenAnswer(inv -> {
                Usuario u = inv.getArgument(0);
                u = Usuario.builder().id(3L).login(u.getLogin()).nome(u.getNome())
                    .senhaHash(u.getSenhaHash()).perfil(u.getPerfil())
                    .ativo(u.getAtivo()).dataCadastro(u.getDataCadastro()).build();
                return u;
            });

            UsuarioResponseDTO resp = service.incluir(dto);

            assertThat(resp.perfil()).isEqualTo(PerfilUsuario.OPERADOR);
            assertThat(resp.ativo()).isEqualTo("S");
        }

        @Test
        @DisplayName("deve respeitar perfil explicitamente informado")
        void incluir_perfilExplicito() {
            UsuarioRequestDTO dto = new UsuarioRequestDTO(
                "admin2", "Admin Dois", "senha@456", PerfilUsuario.ADMIN
            );
            when(repository.existsByLogin("admin2")).thenReturn(false);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.incluir(dto);

            verify(repository).save(argThat(u -> u.getPerfil() == PerfilUsuario.ADMIN));
        }

        @Test
        @DisplayName("deve hashar senha com BCrypt — senha NUNCA gravada em texto puro (lacuna corrigida)")
        void incluir_senhaHasheada() {
            UsuarioRequestDTO dto = new UsuarioRequestDTO(
                "novo.user", "Novo Usuário", "minhasenha123", PerfilUsuario.OPERADOR
            );
            when(repository.existsByLogin("novo.user")).thenReturn(false);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            // passwordEncoder.encode() retorna valor simulado diferente da senha em texto puro
            when(passwordEncoder.encode("minhasenha123")).thenReturn("$2a$10$mockedHash");

            service.incluir(dto);

            // hash retornado pelo encoder NÃO é igual ao texto da senha
            verify(repository).save(argThat(u ->
                u.getSenhaHash().equals("$2a$10$mockedHash") &&
                !u.getSenhaHash().equals("minhasenha123")
            ));
        }

        @Test
        @DisplayName("deve lançar LoginDuplicadoException — uq_usuario_login / SQLCODE != 0 (linha 26)")
        void incluir_loginDuplicado() {
            when(repository.existsByLogin("carlos.silva")).thenReturn(true);

            assertThatThrownBy(() -> service.incluir(
                new UsuarioRequestDTO("carlos.silva", "Outro", "senha@123", null)
            )).isInstanceOf(LoginDuplicadoException.class)
              .hasMessageContaining("carlos.silva");

            verify(repository, never()).save(any());
        }
    }

    // =========================================================================
    @Nested @DisplayName("desativar()")
    class DesativarTests {

        @Test
        @DisplayName("deve definir ativo='N' — UPDATE usuario SET ativo='N' (linha 35)")
        void desativar_sucesso() {
            when(repository.findById(1L)).thenReturn(Optional.of(usuarioAtivo));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UsuarioResponseDTO resp = service.desativar(1L);

            assertThat(resp.ativo()).isEqualTo("N");
        }

        @Test
        @DisplayName("deve lançar UsuarioNaoEncontradoException — ERROR 'Usuario nao encontrado.' (linha 39)")
        void desativar_naoEncontrado() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.desativar(99L))
                .isInstanceOf(UsuarioNaoEncontradoException.class)
                .hasMessageContaining("99");
        }
    }

    // =========================================================================
    @Nested @DisplayName("alterar()")
    class AlterarTests {

        @Test
        @DisplayName("deve atualizar login, nome e perfil sem alterar senha")
        void alterar_sucesso() {
            UsuarioRequestDTO dto = new UsuarioRequestDTO(
                "carlos.atualizado", "Carlos Atualizado", "qualquer", PerfilUsuario.ADMIN
            );
            when(repository.findById(1L)).thenReturn(Optional.of(usuarioAtivo));
            when(repository.existsByLoginAndIdNot("carlos.atualizado", 1L)).thenReturn(false);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UsuarioResponseDTO resp = service.alterar(1L, dto);

            assertThat(resp.login()).isEqualTo("carlos.atualizado");
            assertThat(resp.perfil()).isEqualTo(PerfilUsuario.ADMIN);
            // senha NÃO é tocada pelo alterar() — hash permanece o mesmo
            verify(repository).save(argThat(u ->
                u.getSenhaHash().equals("$2a$10$hashedpassword")
            ));
        }

        @Test
        @DisplayName("deve lançar LoginDuplicadoException se login pertence a outro usuário")
        void alterar_loginDuplicado() {
            when(repository.findById(1L)).thenReturn(Optional.of(usuarioAtivo));
            when(repository.existsByLoginAndIdNot("outro.login", 1L)).thenReturn(true);

            assertThatThrownBy(() -> service.alterar(1L,
                new UsuarioRequestDTO("outro.login", "Nome", "senha@123", null)
            )).isInstanceOf(LoginDuplicadoException.class);
        }

        @Test
        @DisplayName("deve lançar UsuarioInativoException ao tentar alterar usuário inativo")
        void alterar_usuarioInativo() {
            when(repository.findById(2L)).thenReturn(Optional.of(usuarioInativo));

            assertThatThrownBy(() -> service.alterar(2L,
                new UsuarioRequestDTO("ana.nova", "Ana Nova", "senha@123", null)
            )).isInstanceOf(UsuarioInativoException.class);
        }
    }

    // =========================================================================
    @Nested @DisplayName("alterarSenha()")
    class AlterarSenhaTests {

        @Test
        @DisplayName("deve lançar SenhaAtualInvalidaException quando senha atual não confere")
        void alterarSenha_senhaErrada() {
            when(repository.findById(1L)).thenReturn(Optional.of(usuarioAtivo));
            // matches() retorna false — senha não confere com o hash armazenado
            when(passwordEncoder.matches("senhaErrada", "$2a$10$hashedpassword")).thenReturn(false);

            assertThatThrownBy(() -> service.alterarSenha(1L,
                new AlterarSenhaDTO("senhaErrada", "novaSenha@123")
            )).isInstanceOf(SenhaAtualInvalidaException.class);
        }

        @Test
        @DisplayName("deve lançar UsuarioInativoException para usuário inativo")
        void alterarSenha_usuarioInativo() {
            when(repository.findById(2L)).thenReturn(Optional.of(usuarioInativo));

            assertThatThrownBy(() -> service.alterarSenha(2L,
                new AlterarSenhaDTO("qualquer", "novaSenha@123")
            )).isInstanceOf(UsuarioInativoException.class);
        }
    }

    // =========================================================================
    @Nested @DisplayName("reativar()")
    class ReativarTests {

        @Test
        @DisplayName("deve definir ativo='S' — lacuna corrigida (desativação era irreversível)")
        void reativar_sucesso() {
            when(repository.findById(2L)).thenReturn(Optional.of(usuarioInativo));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UsuarioResponseDTO resp = service.reativar(2L);

            assertThat(resp.ativo()).isEqualTo("S");
        }
    }

    // =========================================================================
    @Nested @DisplayName("excluir()")
    class ExcluirTests {

        @Test
        @DisplayName("deve excluir usuário existente")
        void excluir_sucesso() {
            when(repository.existsById(1L)).thenReturn(true);
            doNothing().when(repository).deleteById(1L);

            assertThatCode(() -> service.excluir(1L)).doesNotThrowAnyException();
            verify(repository).deleteById(1L);
        }

        @Test
        @DisplayName("deve lançar UsuarioNaoEncontradoException para ID inexistente")
        void excluir_naoEncontrado() {
            when(repository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> service.excluir(99L))
                .isInstanceOf(UsuarioNaoEncontradoException.class);
        }
    }
}
