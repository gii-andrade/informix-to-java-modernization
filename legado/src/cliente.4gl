FUNCTION clientes_menu()
    MENU "Clientes"
        COMMAND "Incluir" CALL cliente_incluir()
        COMMAND "Consultar" CALL cliente_consultar()
        COMMAND "Alterar" CALL cliente_alterar()
        COMMAND "Excluir" CALL cliente_excluir()
        COMMAND "Voltar" EXIT MENU
    END MENU
END FUNCTION

FUNCTION cliente_incluir()
    DEFINE razao LIKE cliente.razao_social
    DEFINE documento LIKE cliente.documento
    DEFINE email LIKE cliente.email
    DEFINE ativo LIKE cliente.ativo
    OPEN FORM f_cliente FROM "cliente"
    DISPLAY FORM f_cliente
    LET ativo = 'S'
    DISPLAY BY NAME ativo
    INPUT BY NAME razao, documento, email, ativo
    END INPUT
    IF NOT INT_FLAG THEN
        INSERT INTO cliente (razao_social, documento, email, ativo)
        VALUES (razao, documento, email, ativo)
        IF SQLCA.SQLCODE = 0 THEN
            MESSAGE "Cliente incluido."
        ELSE
            ERROR "Erro ao incluir cliente."
        END IF
    END IF
    CLOSE FORM f_cliente
END FUNCTION

FUNCTION cliente_consultar()
    DEFINE id LIKE cliente.cliente_id
    DEFINE razao LIKE cliente.razao_social
    DEFINE documento LIKE cliente.documento
    DEFINE email LIKE cliente.email
    DEFINE ativo LIKE cliente.ativo
    PROMPT "Codigo do cliente: " FOR id
    SELECT razao_social, documento, email, ativo
      INTO razao, documento, email, ativo FROM cliente WHERE cliente_id = id
    IF SQLCA.SQLCODE <> 0 THEN
        ERROR "Cliente nao encontrado."
        RETURN
    END IF
    OPEN FORM f_cliente FROM "cliente"
    DISPLAY FORM f_cliente
    DISPLAY BY NAME razao, documento, email, ativo
    PROMPT "Enter para voltar." FOR id
    CLOSE FORM f_cliente
END FUNCTION

FUNCTION cliente_alterar()
    DEFINE id LIKE cliente.cliente_id
    DEFINE razao LIKE cliente.razao_social
    DEFINE documento LIKE cliente.documento
    DEFINE email LIKE cliente.email
    DEFINE ativo LIKE cliente.ativo
    PROMPT "Codigo do cliente: " FOR id
    SELECT razao_social, documento, email, ativo
      INTO razao, documento, email, ativo FROM cliente WHERE cliente_id = id
    IF SQLCA.SQLCODE <> 0 THEN
        ERROR "Cliente nao encontrado."
        RETURN
    END IF
    OPEN FORM f_cliente FROM "cliente"
    DISPLAY FORM f_cliente
    DISPLAY BY NAME razao, documento, email, ativo
    INPUT BY NAME razao, documento, email, ativo
    END INPUT
    IF NOT INT_FLAG THEN
        UPDATE cliente SET razao_social = razao, documento = documento,
            email = email, ativo = ativo WHERE cliente_id = id
        IF SQLCA.SQLCODE = 0 THEN
            MESSAGE "Cliente alterado."
        ELSE
            ERROR "Erro ao alterar cliente."
        END IF
    END IF
    CLOSE FORM f_cliente
END FUNCTION

FUNCTION cliente_excluir()
    DEFINE id LIKE cliente.cliente_id
    PROMPT "Codigo do cliente: " FOR id
    DELETE FROM cliente WHERE cliente_id = id
    IF SQLCA.SQLCODE = 0 THEN
        MESSAGE "Cliente excluido."
    ELSE
        ERROR "Cliente possui pedidos ou nao existe."
    END IF
END FUNCTION
