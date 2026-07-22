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
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Implementação do serviço de usuários.
 *
 * Cada método documenta sua correspondência com o legado usuario.4gl e as
 * regras de negócio preservadas ou lacunas corrigidas.
 *
 * BCrypt é registrado como @Bean em AppConfig e injetado pelo construtor
 * via @RequiredArgsConstructor — compatível com testes (injetável via Mockito
 * ou instanciado diretamente em contexto Spring).
 */
@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository repository;

    /**
     * Encoder BCrypt injetado via construtor.
     * Declarado como @Bean em br.com.pedidos.config.AppConfig.
     *
     * Corrige a lacuna crítica de segurança do legado:
     * a senha era gravada em texto puro (usuario_incluir, linhas 21-22).
     * A API NUNCA armazena ou retorna senhas sem hash BCrypt.
     */
    private final PasswordEncoder passwordEncoder;

    // =========================================================================
    // usuario_incluir() — linha 9 do usuario.4gl
    //
    // Regras preservadas:
    //   • login, nome, senha e perfil obrigatórios (REQUIRED no usuario.per)
    //   • perfil padrão = 'OPERADOR' (LET perfil = 'OPERADOR', linha 16)
    //   • login deve ser único (uq_usuario_login)
    //   • ativo inicia como 'S' (DEFAULT schema)
    //   • data_cadastro = hoje (DEFAULT TODAY)
    //
    // Lacuna de segurança corrigida:
    //   • Senha convertida para BCrypt antes de persistir
    //   • Política mínima de senha (8 caracteres) aplicada no DTO
    // =========================================================================
    @Override
    @Transactional
    public UsuarioResponseDTO incluir(UsuarioRequestDTO dto) {
        if (repository.existsByLogin(dto.login())) {
            throw new LoginDuplicadoException(dto.login());
        }

        Usuario usuario = Usuario.builder()
            .login(dto.login())
            .nome(dto.nome())
            .senhaHash(passwordEncoder.encode(dto.senha()))   // BCrypt — nunca texto puro
            .perfil(dto.perfil() != null ? dto.perfil() : PerfilUsuario.OPERADOR)
            .ativo("S")
            .dataCadastro(LocalDate.now())
            .build();

        return UsuarioResponseDTO.from(repository.save(usuario));
    }

    // =========================================================================
    // buscarPorId — sem correspondente na TUI legada (lacuna corrigida)
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarPorId(Long id) {
        return UsuarioResponseDTO.from(
            repository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException(id))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listarTodos() {
        return repository.findAll().stream()
            .map(UsuarioResponseDTO::from)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listarPorPerfil(PerfilUsuario perfil) {
        return repository.findByPerfil(perfil).stream()
            .map(UsuarioResponseDTO::from)
            .toList();
    }

    // =========================================================================
    // alterar — sem correspondente na TUI legada (lacuna corrigida)
    //
    // Permite alterar login, nome e perfil. Senha tem endpoint próprio.
    // Regras aplicadas:
    //   • Usuário deve existir
    //   • Usuário deve estar ativo para ser alterado
    //   • Login mantém unicidade (pode manter o próprio login)
    // =========================================================================
    @Override
    @Transactional
    public UsuarioResponseDTO alterar(Long id, UsuarioRequestDTO dto) {
        Usuario usuario = buscarEntidade(id);

        if ("N".equals(usuario.getAtivo())) {
            throw new UsuarioInativoException(id);
        }

        if (repository.existsByLoginAndIdNot(dto.login(), id)) {
            throw new LoginDuplicadoException(dto.login());
        }

        usuario.setLogin(dto.login());
        usuario.setNome(dto.nome());
        usuario.setPerfil(dto.perfil() != null ? dto.perfil() : usuario.getPerfil());
        // senha só é alterada via alterarSenha() — ignorada aqui intencionalmente

        return UsuarioResponseDTO.from(repository.save(usuario));
    }

    // =========================================================================
    // alterarSenha — lacuna crítica de segurança corrigida
    //
    // O legado não tinha função de alterar senha — qualquer mudança exigia
    // desativar e recriar o usuário. A nova senha era sempre texto puro.
    //
    // Regras:
    //   • Usuário deve existir e estar ativo
    //   • Senha atual deve conferir com o hash armazenado (BCrypt matches)
    //   • Nova senha é encodada com BCrypt antes de persistir
    // =========================================================================
    @Override
    @Transactional
    public void alterarSenha(Long id, AlterarSenhaDTO dto) {
        Usuario usuario = buscarEntidade(id);

        if ("N".equals(usuario.getAtivo())) {
            throw new UsuarioInativoException(id);
        }

        if (!passwordEncoder.matches(dto.senhaAtual(), usuario.getSenhaHash())) {
            throw new SenhaAtualInvalidaException();
        }

        usuario.setSenhaHash(passwordEncoder.encode(dto.senhaNova()));
        repository.save(usuario);
    }

    // =========================================================================
    // usuario_desativar() — linha 32 do usuario.4gl
    //
    // Regra preservada:
    //   • UPDATE usuario SET ativo = 'N' WHERE usuario_id = id (linha 35)
    //   • ERROR "Usuario nao encontrado." se SQLCODE <> 0 (linha 39)
    //
    // Melhoria: lança exceção tipada em vez de SQLCODE genérico.
    // =========================================================================
    @Override
    @Transactional
    public UsuarioResponseDTO desativar(Long id) {
        Usuario usuario = buscarEntidade(id);
        usuario.setAtivo("N");
        return UsuarioResponseDTO.from(repository.save(usuario));
    }

    // =========================================================================
    // reativar — lacuna corrigida (desativação era irreversível no legado)
    // =========================================================================
    @Override
    @Transactional
    public UsuarioResponseDTO reativar(Long id) {
        Usuario usuario = buscarEntidade(id);
        usuario.setAtivo("S");
        return UsuarioResponseDTO.from(repository.save(usuario));
    }

    // =========================================================================
    // excluir — sem correspondente no legado (usuário nunca era excluído)
    //
    // Preferir desativar() para preservar histórico de pedidos vinculados.
    // A FK fk_pedido_usuario impede exclusão de usuários com pedidos.
    // =========================================================================
    @Override
    @Transactional
    public void excluir(Long id) {
        if (!repository.existsById(id)) {
            throw new UsuarioNaoEncontradoException(id);
        }
        repository.deleteById(id);
    }

    // =========================================================================
    // Auxiliar privado
    // =========================================================================

    private Usuario buscarEntidade(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new UsuarioNaoEncontradoException(id));
    }
}
