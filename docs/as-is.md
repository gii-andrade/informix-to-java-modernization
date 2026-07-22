# Estado atual (As-Is)

## Tecnologia atual: Informix-4GL

O Sistema de Pedidos é uma aplicação legada de terminal escrita em
Informix-4GL. As telas são formulários PER, a lógica de navegação está nos
arquivos 4GL e o acesso ao banco Informix é realizado por SQL embutido.

O banco utiliza as tabelas usuario, cliente, produto, pedido e item_pedido.

## Arquitetura atual

    Usuário
      -> Menu terminal Informix-4GL
         -> Módulos 4GL (clientes, produtos, pedidos, usuários e relatórios)
            -> SQL embutido nas rotinas
               -> Banco Informix

Características do desenho atual:

- Interface de texto dirigida por menus.
- Cada módulo concentra navegação, validação, regra de negócio e SQL.
- Formulários PER definem a apresentação e a entrada de dados.
- Pedidos possuem cabeçalho e itens; o total é recalculado após a inclusão.
- Integridade referencial e regras básicas também são aplicadas no banco.

## Limitações

- Acoplamento entre interface, regras de negócio e acesso a dados.
- Ausência de API para integração com outros sistemas ou front-ends.
- Baixa reutilização das regras fora das telas de terminal.
- Validações e tratamento de erros dependem do fluxo interativo do 4GL.
- Senhas exigem evolução para armazenamento seguro com hash forte e política
  de autenticação apropriada.
- Não há testes automatizados, observabilidade ou empacotamento para execução
  em contêiner.
- Evoluções na interface exigem alteração dos formulários de terminal.

## Objetivo da modernização

Transformar o sistema em uma aplicação web Java com Spring Boot, mantendo as
regras de negócio e o banco Informix inicialmente, mas separando as
responsabilidades em camadas:

    Antes: Informix-4GL
    Menu + telas + regras + SQL no mesmo módulo

    Depois: Java/Spring Boot
    Web/API -> Controllers -> Services -> Repositories -> Informix

Resultados esperados:

- APIs REST para clientes, produtos, pedidos, usuários e relatórios.
- Regras de negócio centralizadas em serviços testáveis.
- Persistência isolada em repositórios, com JPA/Hibernate ou JDBC.
- Interface web independente do back-end.
- Autenticação e autorização baseadas em padrões atuais.
- Testes automatizados, logs estruturados, métricas e execução em contêiner.
- Pipeline de CI com GitHub Actions (build + testes em cada push).
- Base clara para o IBM Bob analisar o legado e apoiar a transformação para
  Java/Spring Boot.
