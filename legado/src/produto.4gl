FUNCTION produtos_menu()
    MENU "Produtos"
        COMMAND "Incluir" CALL produto_incluir()
        COMMAND "Consultar" CALL produto_consultar()
        COMMAND "Excluir" CALL produto_excluir()
        COMMAND "Voltar" EXIT MENU
    END MENU
END FUNCTION

FUNCTION produto_incluir()
    DEFINE codigo LIKE produto.codigo
    DEFINE descricao LIKE produto.descricao
    DEFINE preco LIKE produto.preco_venda
    DEFINE estoque LIKE produto.estoque_atual
    DEFINE ativo LIKE produto.ativo
    OPEN FORM f_produto FROM "produto"
    DISPLAY FORM f_produto
    LET ativo = 'S'
    DISPLAY BY NAME ativo
    INPUT BY NAME codigo, descricao, preco, estoque, ativo
    END INPUT
    IF NOT INT_FLAG THEN
        INSERT INTO produto (codigo, descricao, preco_venda, estoque_atual, ativo)
        VALUES (codigo, descricao, preco, estoque, ativo)
        IF SQLCA.SQLCODE = 0 THEN
            MESSAGE "Produto incluido."
        ELSE
            ERROR "Erro ao incluir produto."
        END IF
    END IF
    CLOSE FORM f_produto
END FUNCTION

FUNCTION produto_consultar()
    DEFINE id LIKE produto.produto_id
    DEFINE codigo LIKE produto.codigo
    DEFINE descricao LIKE produto.descricao
    DEFINE preco LIKE produto.preco_venda
    DEFINE estoque LIKE produto.estoque_atual
    DEFINE ativo LIKE produto.ativo
    PROMPT "Codigo interno: " FOR id
    SELECT codigo, descricao, preco_venda, estoque_atual, ativo
      INTO codigo, descricao, preco, estoque, ativo FROM produto WHERE produto_id = id
    IF SQLCA.SQLCODE <> 0 THEN
        ERROR "Produto nao encontrado."
        RETURN
    END IF
    OPEN FORM f_produto FROM "produto"
    DISPLAY FORM f_produto
    DISPLAY BY NAME codigo, descricao, preco, estoque, ativo
    PROMPT "Enter para voltar." FOR id
    CLOSE FORM f_produto
END FUNCTION

FUNCTION produto_excluir()
    DEFINE id LIKE produto.produto_id
    PROMPT "Codigo interno: " FOR id
    DELETE FROM produto WHERE produto_id = id
    IF SQLCA.SQLCODE = 0 THEN
        MESSAGE "Produto excluido."
    ELSE
        ERROR "Produto possui itens ou nao existe."
    END IF
END FUNCTION
