DATABASE pedidos_db

MAIN
    DEFER INTERRUPT
    DEFER QUIT
    OPEN WINDOW principal AT 1, 1 WITH 22 ROWS, 78 COLUMNS
        ATTRIBUTE (BORDER, PROMPT LINE LAST, MENU LINE 2)
    MENU "Sistema de Pedidos"
        COMMAND "Clientes" CALL clientes_menu()
        COMMAND "Produtos" CALL produtos_menu()
        COMMAND "Pedidos" CALL pedidos_menu()
        COMMAND "Relatorios" CALL relatorios_menu()
        COMMAND "Usuarios" CALL usuarios_menu()
        COMMAND "Sair" EXIT MENU
    END MENU
    CLOSE WINDOW principal
END MAIN
