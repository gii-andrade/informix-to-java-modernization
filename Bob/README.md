# IBM Bob — Modernização do Sistema de Pedidos Informix-4GL

Este documento conta **como** o IBM Bob foi usado para modernizar um sistema legado de terminal Informix-4GL para uma API REST Java/Spring Boot 3.

---

## O que é o IBM Bob

O IBM Bob é um assistente de IA especializado em engenharia de software, integrado ao ambiente de desenvolvimento. Ele lê, entende e gera código — incluindo código legado obscuro como Informix-4GL — e consegue conduzir migrações de ponta a ponta com rastreabilidade entre o legado e o código modernizado.

---

## Ponto de Partida

O repositório começou com apenas o código legado em `legado/` (arquivos `.4gl`, formulários `.per` e schema SQL Informix) e dois documentos de contexto em `docs/`:

- [`docs/as-is.md`](../docs/as-is.md) — análise do estado atual do legado
- [`docs/arquitetura.md`](../docs/arquitetura.md) — arquitetura e preparação para modernização

O objetivo declarado: transformar o sistema em Spring Boot 3 separando Controller / Service / Repository.

---

## Etapas da Modernização

### 1. Análise do Legado

O Bob leu todos os arquivos `.4gl` e mapeou:

- **Fluxos de tela** — como `pedido_incluir()` chama `pedido_incluir_itens()` e depois `pedido_recalcular_total()`
- **Regras de negócio embutidas** — ex.: `SELECT preco_venda WHERE ativo = 'S'` (produto deve estar ativo)
- **Lacunas de segurança** — senha gravada em texto puro (`INSERT INTO usuario ... VALUES (login, nome, **senha**, perfil)`)
- **Lacunas de negócio** — status `CONFIRMADO` e `FATURADO` existiam no schema mas nunca tinham operação na TUI
- **Bug de transação** — cabeçalho do pedido era inserido **fora de transação** antes dos itens

Cada achado foi documentado em comentários JavaDoc nos services gerados, rastreando a linha exata do `.4gl` de origem.

### 2. Geração da Estrutura Base

Com o mapeamento feito, o Bob gerou:

```
moderno/
├── pom.xml                        # Spring Boot 3.3 + JPA + Validation + H2
└── src/main/java/br/com/pedidos/
    ├── PedidosApplication.java
    ├── cliente/                   # Primeiro módulo — padrão para os demais
    │   ├── controller/
    │   ├── dto/
    │   ├── entity/
    │   ├── exception/
    │   ├── repository/
    │   └── service/
    └── ...
```

Cada entidade foi mapeada diretamente do `schema.sql` Informix — nomes de tabelas, colunas, constraints e índices preservados para compatibilidade.

### 3. Portagem dos Módulos

Para cada módulo legado, o processo foi:

1. Ler o `.4gl` e identificar as funções
2. Mapear cada função para um método REST com o HTTP method correto
3. Preservar regras de negócio com comentário referenciando linha do legado
4. Corrigir lacunas identificadas na análise

Exemplo de rastreabilidade no [`PedidoServiceImpl.java`](../moderno/src/main/java/br/com/pedidos/pedido/service/PedidoServiceImpl.java):

```java
// pedido_incluir_itens(), linha 48-51 do pedido.4gl:
//   SELECT preco_venda INTO v_preco FROM produto
//    WHERE produto_id = v_produto_id AND ativo = 'S'
//   IF SQLCA.SQLCODE <> 0 THEN ERROR "Produto inexistente ou inativo."
Produto produto = produtoRepository.findById(dto.produtoId())
    .filter(p -> "S".equals(p.getAtivo()))
    .orElseThrow(() -> new ProdutoInativoOuInexistenteException(dto.produtoId()));
```

### 4. Módulo de Relatórios

O `relatorio.4gl` tinha apenas um relatório: pedidos agrupados por status (`SELECT status, COUNT(*), SUM(valor_total) FROM pedido GROUP BY status`). O Bob portou isso para:

- `GET /relatorios/pedidos-por-status` → retorna JSON com status, quantidade e valor total
- Query JPQL com `new br.com.pedidos.relatorio.dto.RelatorioPedidosStatusDTO(...)` para projeção direta

### 5. Autenticação e Autorização

O legado não tinha tela de login — quem tinha acesso ao servidor usava o sistema. Com a API exposta na rede, o Bob implementou:

- `POST /auth/login` — endpoint público que devolve JWT (HMAC-SHA256, 24 h)
- `JwtAuthFilter` — valida Bearer token em cada requisição
- Perfis mapeados do campo `perfil` que existia no schema mas nunca era verificado:
  - `CONSULTA` → somente GET
  - `OPERADOR` → GET + escrita em clientes/produtos/pedidos
  - `ADMIN` → acesso total (inclui /usuarios)

### 6. Testes

Para cada módulo, o Bob gerou dois níveis de teste:

- **Unitários** (Mockito) — testam a lógica de negócio de cada `ServiceImpl` em isolamento
- **Integração** (MockMvc + H2) — exercitam o fluxo HTTP completo até o banco em memória

Cada cenário de teste foi rastreado à função legada correspondente:

```java
// Cenários rastreados ao legado pedido.4gl:
//   criar_produtoInativo  → ERROR "Produto inexistente ou inativo." (linha 51)
//   cancelar_semValidacao → UPDATE SET status='CANCELADO' sem checar status atual
```

### 7. Containerização e Observabilidade

O Bob gerou o `Dockerfile` multi-stage e o `docker-compose.yml` que monta o `schema.sql` e `seed.sql` do legado diretamente no PostgreSQL 16 — sem precisar reescrever o schema.

Adicionou Spring Boot Actuator com endpoints de health, info, metrics e loggers, e configurou CI via GitHub Actions para rodar `mvn test` em cada push.

---

## Lacunas Encontradas e Corrigidas

| # | Arquivo legado | Linha | Problema | Solução |
|---|---|---|---|---|
| 1 | `usuario.4gl` | 21-22 | Senha em texto puro | BCrypt em toda persistência |
| 2 | `pedido.4gl` | 24-25 | INSERT do cabeçalho fora de transação | `@Transactional` atômica |
| 3 | `pedido.4gl` | 98 | Cancelamento sem validar status | Máquina de estados no enum `StatusPedido` |
| 4 | `pedido.4gl` | 57 | Desconto hardcoded como 0 | Campo operacional no DTO |
| 5 | `pedido.4gl` | — | Estoque nunca atualizado | Desconto na adição, restituição no cancelamento |
| 6 | `schema.sql` | 53 | Status CONFIRMADO/FATURADO sem operação na TUI | PATCH /confirmar e /faturar |
| 7 | `usuario.4gl` | 35 | Desativação irreversível | PATCH /reativar |
| 8 | Todos | — | Sem tela de login | JWT + Spring Security |
| 9 | Todos | — | Sem API | REST completo com RFC 7807 |

---

## Resultado

| Métrica | Valor |
|---|---|
| Arquivos legados analisados | 10 (6x `.4gl`, 4x `.per`) |
| Arquivos Java gerados | 62 produção + 11 teste = 73 |
| Linhas de código produção | 3.923 |
| Linhas de teste | 2.653 |
| Cobertura funcional | 100% dos módulos do legado |
| Lacunas corrigidas | 9 identificadas, 9 resolvidas |

---

## Conclusão

O IBM Bob acelerou a modernização ao:

1. **Eliminar o trabalho de leitura do legado** — ler e entender Informix-4GL manualmente levaria dias; o Bob fez em minutos
2. **Garantir rastreabilidade** — cada método Java documenta a função `.4gl` de origem e a linha exata
3. **Identificar lacunas invisíveis** — bugs de transação e problemas de segurança que poderiam passar despercebidos numa migração manual
4. **Gerar testes junto com o código** — não só a implementação, mas os testes que validam cada regra portada
5. **Entregar completo** — do código à containerização ao CI, sem etapas manuais

O legado ficou intacto em `legado/` para referência. A versão modernizada em `moderno/` é pronta para produção.
