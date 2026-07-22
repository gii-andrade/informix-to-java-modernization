FUNCTION pedidos_menu()
    MENU "Pedidos"
        COMMAND "Incluir" CALL pedido_incluir()
        COMMAND "Consultar" CALL pedido_consultar()
        COMMAND "Cancelar" CALL pedido_cancelar()
        COMMAND "Excluir" CALL pedido_excluir()
        COMMAND "Voltar" EXIT MENU
    END MENU
END FUNCTION

FUNCTION pedido_incluir()
    DEFINE v_cliente LIKE pedido.cliente_id
    DEFINE v_usuario LIKE pedido.usuario_id
    DEFINE v_observacao LIKE pedido.observacao
    DEFINE v_pedido_id LIKE pedido.pedido_id
    OPEN FORM f_pedido FROM "pedido"
    DISPLAY FORM f_pedido
    INPUT BY NAME v_cliente, v_usuario, v_observacao
    END INPUT
    IF INT_FLAG THEN
        CLOSE FORM f_pedido
        RETURN
    END IF
    INSERT INTO pedido (cliente_id, usuario_id, observacao)
    VALUES (v_cliente, v_usuario, v_observacao)
    IF SQLCA.SQLCODE <> 0 THEN
        ERROR "Erro ao incluir pedido."
        CLOSE FORM f_pedido
        RETURN
    END IF
    LET v_pedido_id = SQLCA.SQLERRD[2]
    CALL pedido_incluir_itens(v_pedido_id)
    CALL pedido_recalcular_total(v_pedido_id)
    MESSAGE "Pedido incluido."
    CLOSE FORM f_pedido
END FUNCTION

FUNCTION pedido_incluir_itens(p_pedido_id)
    DEFINE p_pedido_id LIKE pedido.pedido_id
    DEFINE v_produto_id LIKE produto.produto_id
    DEFINE v_quantidade LIKE item_pedido.quantidade
    DEFINE v_preco LIKE produto.preco_venda
    DEFINE v_total LIKE item_pedido.valor_total
    DEFINE continuar CHAR(1)
    LET continuar = 'S'
    WHILE continuar = 'S'
        PROMPT "Produto: " FOR v_produto_id
        SELECT preco_venda INTO v_preco FROM produto
         WHERE produto_id = v_produto_id AND ativo = 'S'
        IF SQLCA.SQLCODE <> 0 THEN
            ERROR "Produto inexistente ou inativo."
        ELSE
            PROMPT "Quantidade: " FOR v_quantidade
            LET v_total = v_quantidade * v_preco
            INSERT INTO item_pedido
                (pedido_id, produto_id, quantidade, preco_unitario, desconto, valor_total)
            VALUES (p_pedido_id, v_produto_id, v_quantidade, v_preco, 0, v_total)
        END IF
        PROMPT "Outro item (S/N)? " FOR continuar
    END WHILE
END FUNCTION

FUNCTION pedido_recalcular_total(p_pedido_id)
    DEFINE p_pedido_id LIKE pedido.pedido_id
    DEFINE v_total LIKE pedido.valor_total
    SELECT SUM(valor_total) INTO v_total FROM item_pedido WHERE pedido_id = p_pedido_id
    IF v_total IS NULL THEN
        LET v_total = 0
    END IF
    UPDATE pedido SET valor_total = v_total WHERE pedido_id = p_pedido_id
END FUNCTION

FUNCTION pedido_consultar()
    DEFINE id LIKE pedido.pedido_id
    DEFINE v_cliente LIKE pedido.cliente_id
    DEFINE v_usuario LIKE pedido.usuario_id
    DEFINE v_status LIKE pedido.status
    DEFINE v_total LIKE pedido.valor_total
    DEFINE v_observacao LIKE pedido.observacao
    PROMPT "Numero do pedido: " FOR id
    SELECT cliente_id, usuario_id, status, valor_total, observacao
      INTO v_cliente, v_usuario, v_status, v_total, v_observacao
      FROM pedido WHERE pedido_id = id
    IF SQLCA.SQLCODE <> 0 THEN
        ERROR "Pedido nao encontrado."
        RETURN
    END IF
    OPEN FORM f_pedido FROM "pedido"
    DISPLAY FORM f_pedido
    DISPLAY BY NAME v_cliente, v_usuario, v_status, v_total, v_observacao
    PROMPT "Enter para voltar." FOR id
    CLOSE FORM f_pedido
END FUNCTION

FUNCTION pedido_cancelar()
    DEFINE id LIKE pedido.pedido_id
    PROMPT "Numero do pedido: " FOR id
    UPDATE pedido SET status = 'CANCELADO' WHERE pedido_id = id
    IF SQLCA.SQLCODE = 0 THEN
        MESSAGE "Pedido cancelado."
    ELSE
        ERROR "Pedido nao encontrado."
    END IF
END FUNCTION

FUNCTION pedido_excluir()
    DEFINE id LIKE pedido.pedido_id
    PROMPT "Numero do pedido: " FOR id
    BEGIN WORK
    DELETE FROM item_pedido WHERE pedido_id = id
    DELETE FROM pedido WHERE pedido_id = id
    IF SQLCA.SQLCODE = 0 THEN
        COMMIT WORK
        MESSAGE "Pedido excluido."
    ELSE
        ROLLBACK WORK
        ERROR "Erro ao excluir pedido."
    END IF
END FUNCTION
