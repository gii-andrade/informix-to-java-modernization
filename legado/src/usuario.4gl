FUNCTION usuarios_menu()
    MENU "Usuarios"
        COMMAND "Incluir" CALL usuario_incluir()
        COMMAND "Desativar" CALL usuario_desativar()
        COMMAND "Voltar" EXIT MENU
    END MENU
END FUNCTION

FUNCTION usuario_incluir()
    DEFINE login LIKE usuario.login
    DEFINE nome LIKE usuario.nome
    DEFINE senha LIKE usuario.senha_hash
    DEFINE perfil LIKE usuario.perfil
    OPEN FORM f_usuario FROM "usuario"
    DISPLAY FORM f_usuario
    LET perfil = 'OPERADOR'
    DISPLAY BY NAME perfil
    INPUT BY NAME login, nome, senha, perfil
    END INPUT
    IF NOT INT_FLAG THEN
        INSERT INTO usuario (login, nome, senha_hash, perfil)
        VALUES (login, nome, senha, perfil)
        IF SQLCA.SQLCODE = 0 THEN
            MESSAGE "Usuario incluido."
        ELSE
            ERROR "Erro ao incluir usuario."
        END IF
    END IF
    CLOSE FORM f_usuario
END FUNCTION

FUNCTION usuario_desativar()
    DEFINE id LIKE usuario.usuario_id
    PROMPT "Codigo do usuario: " FOR id
    UPDATE usuario SET ativo = 'N' WHERE usuario_id = id
    IF SQLCA.SQLCODE = 0 THEN
        MESSAGE "Usuario desativado."
    ELSE
        ERROR "Usuario nao encontrado."
    END IF
END FUNCTION
