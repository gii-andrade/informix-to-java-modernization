# Arquitetura — Antes e Depois

## Legado (Informix-4GL)

O sistema é uma aplicação TUI escrita em Informix-4GL. Menu principal em
`main.4gl`; cada módulo concentra navegação, validação, regra de negócio e
SQL embutido num único arquivo `.4gl`. As telas são formulários `.PER`
compilados separadamente.

### Diagrama legado

```mermaid
graph TD
    U[Usuário terminal] --> M[main.4gl — Menu principal]
    M --> C[cliente.4gl]
    M --> P[produto.4gl]
    M --> PD[pedido.4gl]
    M --> US[usuario.4gl]
    M --> R[relatorio.4gl]
    C  --> DB[(Banco Informix)]
    P  --> DB
    PD --> DB
    US --> DB
    R  --> DB

    style DB fill:#f5f5f5,stroke:#999
```

Características:
- Interface de texto dirigida por menus.
- Cada módulo concentra navegação, validação, regra de negócio e SQL.
- Formulários PER definem a apresentação e a entrada de dados.
- Pedidos possuem cabeçalho (`pedido`) e itens (`item_pedido`); o total é
  recalculado por `pedido_recalcular_total()` após a inclusão dos itens.

---

## Modernizado (Spring Boot 3)

A aplicação foi separada em camadas seguindo Clean Architecture:

```mermaid
graph TD
    Client[Cliente HTTP\nPostman / Front-end / cURL]
    Client -->|REST JSON| Security

    subgraph Spring Boot 3
        Security[Spring Security\nJwtAuthFilter]
        Controller[Controllers\nCliente · Produto · Pedido\nUsuário · Relatório · Auth]
        Service[Services\nregras de negócio\n@Transactional]
        Repo[Repositories\nSpring Data JPA]
        Security --> Controller
        Controller --> Service
        Service --> Repo
    end

    Repo --> PG[(PostgreSQL 16)]

    style PG fill:#f5f5f5,stroke:#999
```

### Máquina de estados do Pedido

```mermaid
stateDiagram-v2
    [*] --> ABERTO : POST /pedidos
    ABERTO --> CONFIRMADO : PATCH /confirmar
    ABERTO --> CANCELADO  : PATCH /cancelar
    CONFIRMADO --> FATURADO  : PATCH /faturar
    CONFIRMADO --> CANCELADO : PATCH /cancelar
    FATURADO --> [*]
    CANCELADO --> [*]
```

---

## Schema de Banco (compatível Informix → PostgreSQL)

```
usuario ──┐
           │ FK usuario_id
cliente ──┐│
           ││ FK cliente_id
           ▼▼
         pedido ◄──── item_pedido ──► produto
```

Tabelas: `usuario`, `cliente`, `produto`, `pedido`, `item_pedido`
