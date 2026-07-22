package br.com.pedidos.cliente.exception;

import br.com.pedidos.pedido.exception.ItemDuplicadoNoPedidoException;
import br.com.pedidos.pedido.exception.PedidoNaoEncontradoException;
import br.com.pedidos.pedido.exception.ProdutoInativoOuInexistenteException;
import br.com.pedidos.pedido.exception.TransicaoStatusInvalidaException;
import br.com.pedidos.produto.exception.CodigoProdutoDuplicadoException;
import br.com.pedidos.produto.exception.ProdutoNaoEncontradoException;
import br.com.pedidos.produto.exception.ProdutoPossuiItensException;
import br.com.pedidos.usuario.exception.LoginDuplicadoException;
import br.com.pedidos.usuario.exception.SenhaAtualInvalidaException;
import br.com.pedidos.usuario.exception.UsuarioInativoException;
import br.com.pedidos.usuario.exception.UsuarioNaoEncontradoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tratamento centralizado de exceções da API.
 *
 * Usa RFC 7807 (Problem Details) via {@link ProblemDetail} — padrão do Spring 6.
 *
 * Módulo Clientes:
 *   "Cliente nao encontrado."          → 404 Not Found
 *   "Cliente possui pedidos."          → 409 Conflict
 *   "Erro ao incluir/alterar cliente." → 409 Conflict (doc duplicado)
 *
 * Módulo Produtos:
 *   "Produto nao encontrado."          → 404 Not Found
 *   "Produto possui itens."            → 409 Conflict
 *   "Erro ao incluir produto."         → 409 Conflict (código duplicado)
 *
 * Módulo Pedidos:
 *   "Pedido nao encontrado."           → 404 Not Found
 *   "Produto inexistente ou inativo."  → 422 Unprocessable Entity
 *   "Item duplicado no pedido."        → 409 Conflict
 *   "Transição de status inválida."    → 409 Conflict
 *
 * Módulo Usuários:
 *   "Usuario nao encontrado."          → 404 Not Found
 *   "Erro ao incluir usuario."         → 409 Conflict (login duplicado)
 *   "Senha atual incorreta."           → 422 Unprocessable Entity
 *   "Usuario inativo."                 → 409 Conflict
 *
 * Validação:
 *   Violações de @Valid                → 422 Unprocessable Entity
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // -------------------------------------------------------------------------
    // Módulo Clientes
    // -------------------------------------------------------------------------

    @ExceptionHandler(ClienteNaoEncontradoException.class)
    public ProblemDetail handleClienteNaoEncontrado(ClienteNaoEncontradoException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Cliente não encontrado");
        return pd;
    }

    @ExceptionHandler(DocumentoDuplicadoException.class)
    public ProblemDetail handleDocumentoDuplicado(DocumentoDuplicadoException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Documento já cadastrado");
        return pd;
    }

    @ExceptionHandler(ClientePossuiPedidosException.class)
    public ProblemDetail handleClientePossuiPedidos(ClientePossuiPedidosException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Cliente possui pedidos");
        return pd;
    }

    // -------------------------------------------------------------------------
    // Módulo Produtos
    // -------------------------------------------------------------------------

    @ExceptionHandler(ProdutoNaoEncontradoException.class)
    public ProblemDetail handleProdutoNaoEncontrado(ProdutoNaoEncontradoException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Produto não encontrado");
        return pd;
    }

    @ExceptionHandler(CodigoProdutoDuplicadoException.class)
    public ProblemDetail handleCodigoProdutoDuplicado(CodigoProdutoDuplicadoException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Código de produto já cadastrado");
        return pd;
    }

    @ExceptionHandler(ProdutoPossuiItensException.class)
    public ProblemDetail handleProdutoPossuiItens(ProdutoPossuiItensException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Produto possui itens de pedido");
        return pd;
    }

    // -------------------------------------------------------------------------
    // Módulo Pedidos
    // -------------------------------------------------------------------------

    @ExceptionHandler(PedidoNaoEncontradoException.class)
    public ProblemDetail handlePedidoNaoEncontrado(PedidoNaoEncontradoException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Pedido não encontrado");
        return pd;
    }

    @ExceptionHandler(ProdutoInativoOuInexistenteException.class)
    public ProblemDetail handleProdutoInativoOuInexistente(ProdutoInativoOuInexistenteException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        pd.setTitle("Produto inexistente ou inativo");
        return pd;
    }

    @ExceptionHandler(ItemDuplicadoNoPedidoException.class)
    public ProblemDetail handleItemDuplicado(ItemDuplicadoNoPedidoException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Item duplicado no pedido");
        return pd;
    }

    @ExceptionHandler(TransicaoStatusInvalidaException.class)
    public ProblemDetail handleTransicaoStatusInvalida(TransicaoStatusInvalidaException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Transição de status inválida");
        return pd;
    }

    // -------------------------------------------------------------------------
    // Módulo Usuários
    // -------------------------------------------------------------------------

    @ExceptionHandler(UsuarioNaoEncontradoException.class)
    public ProblemDetail handleUsuarioNaoEncontrado(UsuarioNaoEncontradoException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Usuário não encontrado");
        return pd;
    }

    @ExceptionHandler(LoginDuplicadoException.class)
    public ProblemDetail handleLoginDuplicado(LoginDuplicadoException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Login já cadastrado");
        return pd;
    }

    @ExceptionHandler(SenhaAtualInvalidaException.class)
    public ProblemDetail handleSenhaAtualInvalida(SenhaAtualInvalidaException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        pd.setTitle("Senha atual incorreta");
        return pd;
    }

    @ExceptionHandler(UsuarioInativoException.class)
    public ProblemDetail handleUsuarioInativo(UsuarioInativoException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Usuário inativo");
        return pd;
    }

    // -------------------------------------------------------------------------
    // Segurança — autenticação
    // -------------------------------------------------------------------------

    /** Credenciais inválidas no login → 401 Unauthorized. */
    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        pd.setTitle("Credenciais inválidas");
        return pd;
    }

    // -------------------------------------------------------------------------
    // Validação de entrada (@Valid)
    // -------------------------------------------------------------------------

    /** Erros de validação de campos (@Valid / @NotBlank / @Pattern / @DecimalMin etc.) */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidacao(MethodArgumentNotValidException ex) {
        Map<String, String> campos = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "inválido",
                (a, b) -> a
            ));

        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        pd.setTitle("Dados de entrada inválidos");
        pd.setDetail("Um ou mais campos não passaram na validação.");
        pd.setProperty("campos", campos);
        return pd;
    }
}
