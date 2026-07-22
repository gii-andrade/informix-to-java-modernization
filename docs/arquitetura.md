# Arquitetura legada

O programa é uma aplicação terminal Informix-4GL com menu principal e módulos
de clientes, produtos, pedidos, usuários e relatórios. As telas PER são
compiladas separadamente e abertas pelos respectivos módulos.

Pedido é o cabeçalho; item_pedido guarda os itens. A rotina de inclusão
persiste o cabeçalho, inclui itens e recalcula o valor total.

## Preparação para modernização

O código usa módulos e SQL explícito. A futura conversão deve separar
controladores/telas, serviços de regra de negócio e repositórios de dados.
Senhas devem ser substituídas por hash forte antes da implantação web.
