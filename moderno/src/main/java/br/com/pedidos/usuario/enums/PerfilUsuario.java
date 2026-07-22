package br.com.pedidos.usuario.enums;

/**
 * Perfis de acesso do sistema — espelha a constraint ck_usuario_perfil do schema Informix:
 *   CHECK (perfil IN ('ADMIN','OPERADOR','CONSULTA'))
 *
 * Semântica de cada perfil (inferida do legado):
 *   ADMIN    → acesso total, incluindo gestão de usuários
 *   OPERADOR → cria e opera pedidos, clientes e produtos (usuário padrão)
 *   CONSULTA → somente leitura
 *
 * O legado atribuía 'OPERADOR' como padrão na inclusão:
 *   LET perfil = 'OPERADOR'  (usuario_incluir, linha 16 do usuario.4gl)
 */
public enum PerfilUsuario {
    ADMIN,
    OPERADOR,
    CONSULTA
}
