FUNCTION relatorios_menu()
    MENU "Relatorios"
        COMMAND "Pedidos por status" CALL relatorio_pedidos_status()
        COMMAND "Voltar" EXIT MENU
    END MENU
END FUNCTION

FUNCTION relatorio_pedidos_status()
    DEFINE v_status LIKE pedido.status
    DEFINE v_quantidade INTEGER
    DEFINE v_valor DECIMAL(12,2)
    DECLARE c_status CURSOR FOR
        SELECT status, COUNT(*), SUM(valor_total)
          FROM pedido GROUP BY status ORDER BY status
    OPEN WINDOW relatorio AT 2, 2 WITH 12 ROWS, 70 COLUMNS
        ATTRIBUTE (BORDER)
    DISPLAY "STATUS" AT 2, 2
    DISPLAY "QTD" AT 2, 25
    DISPLAY "VALOR" AT 2, 35
    FOREACH c_status INTO v_status, v_quantidade, v_valor
        DISPLAY v_status AT 4, 2
        DISPLAY v_quantidade AT 4, 25
        DISPLAY v_valor AT 4, 35
    END FOREACH
    PROMPT "Enter para voltar." FOR v_status
    CLOSE WINDOW relatorio
END FUNCTION
