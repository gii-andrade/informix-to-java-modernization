# Sistema de Pedidos — Modernização Informix-4GL → Spring Boot 3

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![CI](https://github.com/gii-andrade/informix-to-java-modernization/actions/workflows/ci.yml/badge.svg)](https://github.com/gii-andrade/informix-to-java-modernization/actions/workflows/ci.yml)

Projeto educacional de **modernização de sistema legado** realizado com assistência do **IBM Bob** (IA especializada em migração de código). Converte uma aplicação terminal Informix-4GL em API REST moderna, mantendo todas as regras de negócio e corrigindo lacunas de segurança do legado.

> 👉 **Leia como o IBM Bob conduziu a modernização:** [`Bob/README.md`](Bob/README.md)

---

## O Problema

O sistema legado é uma aplicação TUI (Text User Interface) escrita em **Informix-4GL**:

- Interface de texto com menus e formulários `.PER`
- SQL embutido diretamente nas rotinas `.4gl`
- **Senhas em texto puro** no banco de dados
- Sem API — impossível integrar com outros sistemas
- Sem testes automatizados, containerização ou observabilidade
- Código monolítico misturando apresentação, regras e persistência no mesmo módulo

---

## A Solução

### Stack

| Legado | Modernizado |
|---|---|
| Informix-4GL + formulários PER | Java 21 + Spring Boot 3.3 |
| SQL embutido | Spring Data JPA + Hibernate |
| Senhas em texto puro | BCrypt (Spring Security) |
| Sem autenticação | JWT (JJWT 0.12.6) + perfis ADMIN / OPERADOR / CONSULTA |
| Terminal local | API REST com RFC 7807 (Problem Details) |
| Sem testes | Mockito (unit) + MockMvc + H2 (integração) |
| Sem observabilidade | Spring Boot Actuator + logging estruturado |
| Sem containerização | Dockerfile multi-stage + Docker/Podman Compose (PostgreSQL 16) |

### Módulos Portados

| Módulo | Legado | Endpoint | Status |
|---|---|---|---|
| Cliente | `cliente.4gl` | `/clientes` | ✅ Completo |
| Produto | `produto.4gl` | `/produtos` | ✅ Completo |
| Pedido | `pedido.4gl` | `/pedidos` + máquina de estados | ✅ Completo |
| Usuário | `usuario.4gl` | `/usuarios` | ✅ Completo |
| Relatório | `relatorio.4gl` | `/relatorios/pedidos-por-status` | ✅ Completo |
| Auth | *(não existia)* | `POST /auth/login` → JWT | ✅ Novo |

---

## Arquitetura

### Antes — Informix-4GL

```
Usuário (terminal)
  └─ menu.4gl
       ├─ cliente.4gl  ─┐
       ├─ produto.4gl   ├─ SQL embutido ──► Banco Informix
       ├─ pedido.4gl    │
       ├─ usuario.4gl   │   Navegação + validação + regra
       └─ relatorio.4gl ┘   de negócio + acesso a dados
                            tudo no mesmo módulo
```

### Depois — Spring Boot 3

```
Cliente HTTP
    │
    ▼
JwtAuthFilter (Spring Security)
    │  valida Bearer token → extrai perfil
    ▼
Controller  ←── @Valid (Bean Validation)
    │
    ▼
Service  ←── @Transactional
    │  regras de negócio isoladas
    ▼
Repository (Spring Data JPA)
    │
    ▼
PostgreSQL 16
```

Diagrama completo: [`docs/arquitetura.md`](docs/arquitetura.md)

---

## Como Rodar

### Com Docker

```bash
cd moderno
docker compose up --build
```

### Com Podman (alternativa para ambientes corporativos IBM/Red Hat)

O [`Dockerfile`](moderno/Dockerfile) e o [`docker-compose.yml`](moderno/docker-compose.yml) são 100% compatíveis com Podman — nenhuma alteração necessária.

```bash
# 1. Instalar Podman e podman-compose (uma vez)
brew install podman
brew install podman-compose

# 2. Criar e iniciar a VM Linux local (uma vez por máquina)
podman machine init
podman machine start

# 3. Subir os serviços
cd moderno
podman compose up --build
```

> **`podman machine init/start` é necessário no macOS** porque containers Linux
> não rodam nativamente — o Podman cria uma VM leve em background (o Docker Desktop
> faz o mesmo por baixo dos panos). No Linux corporativo esses dois comandos não
> são necessários.
>
> O Podman roda rootless (sem daemon root), tornando-o preferível em ambientes
> corporativos com restrições de segurança.

Aguarde ~30 s até ambos os serviços ficarem `healthy`. A API sobe em `http://localhost:8080/api/v1`.

### Referência completa da API

Abra [`docs/api-reference.html`](docs/api-reference.html) no navegador para ver todos os endpoints com campos, exemplos curl e regras de autorização organizados por módulo.

### Autenticar e usar

```bash
# ── 1. LOGIN — obtém o token JWT ─────────────────────────────────────────────
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"login":"admin","senha":"password"}'

# Resposta: { "token": "eyJ...", "perfil": "ADMIN", "expiresInMs": 86400000 }

TOKEN="eyJ..."   # cole aqui o token recebido
BASE="http://localhost:8080/api/v1"
AUTH='-H "Authorization: Bearer '$TOKEN'"'

# ── 2. CLIENTES ───────────────────────────────────────────────────────────────
# Listar todos
curl "$BASE/clientes" -H "Authorization: Bearer $TOKEN"

# Criar cliente
curl -X POST "$BASE/clientes" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "razaoSocial": "Empresa Exemplo Ltda",
    "documento":   "12.345.678/0001-90",
    "email":       "contato@exemplo.com",
    "telefone":    "(11) 99999-0000",
    "cidade":      "São Paulo",
    "uf":          "SP",
    "ativo":       "S"
  }'

# Buscar por ID
curl "$BASE/clientes/1" -H "Authorization: Bearer $TOKEN"

# Atualizar (PUT completo)
curl -X PUT "$BASE/clientes/1" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "razaoSocial": "Empresa Exemplo S.A.",
    "documento":   "12.345.678/0001-90",
    "email":       "novo@exemplo.com",
    "uf":          "SP",
    "ativo":       "S"
  }'

# ── 3. PRODUTOS ───────────────────────────────────────────────────────────────
# Listar todos
curl "$BASE/produtos" -H "Authorization: Bearer $TOKEN"

# Criar produto
curl -X POST "$BASE/produtos" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "codigo":        "PROD-001",
    "descricao":     "Caneta Esferográfica Azul",
    "unidadeMedida": "UN",
    "precoVenda":    2.50,
    "estoqueAtual":  100.000,
    "ativo":         "S"
  }'

# Atualizar preço
curl -X PATCH "$BASE/produtos/1/preco?valor=3.00" \
  -H "Authorization: Bearer $TOKEN"

# Ajustar estoque
curl -X PATCH "$BASE/produtos/1/estoque?valor=150.000" \
  -H "Authorization: Bearer $TOKEN"

# ── 4. PEDIDOS ────────────────────────────────────────────────────────────────
# Criar pedido com itens (clienteId e usuarioId precisam existir no banco)
curl -X POST "$BASE/pedidos" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "clienteId":  1,
    "usuarioId":  1,
    "observacao": "Entrega urgente",
    "itens": [
      { "produtoId": 1, "quantidade": 10.000, "desconto": 0.00 },
      { "produtoId": 2, "quantidade":  2.000, "desconto": 5.00 }
    ]
  }'

# Listar todos os pedidos
curl "$BASE/pedidos" -H "Authorization: Bearer $TOKEN"

# Filtrar por status
curl "$BASE/pedidos?status=ABERTO" -H "Authorization: Bearer $TOKEN"

# Buscar pedido por ID
curl "$BASE/pedidos/1" -H "Authorization: Bearer $TOKEN"

# Confirmar pedido (ABERTO → CONFIRMADO)
curl -X PATCH "$BASE/pedidos/1/confirmar" -H "Authorization: Bearer $TOKEN"

# Faturar pedido (CONFIRMADO → FATURADO)
curl -X PATCH "$BASE/pedidos/1/faturar" -H "Authorization: Bearer $TOKEN"

# Cancelar pedido (ABERTO ou CONFIRMADO → CANCELADO)
curl -X PATCH "$BASE/pedidos/1/cancelar" -H "Authorization: Bearer $TOKEN"

# Adicionar item a pedido ABERTO
curl -X POST "$BASE/pedidos/1/itens" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{ "produtoId": 3, "quantidade": 5.000, "desconto": 0.00 }'

# Remover item de pedido ABERTO
curl -X DELETE "$BASE/pedidos/1/itens/3" -H "Authorization: Bearer $TOKEN"

# ── 5. USUÁRIOS (requer perfil ADMIN) ─────────────────────────────────────────
# Listar todos
curl "$BASE/usuarios" -H "Authorization: Bearer $TOKEN"

# Filtrar por perfil
curl "$BASE/usuarios?perfil=OPERADOR" -H "Authorization: Bearer $TOKEN"

# Criar usuário
curl -X POST "$BASE/usuarios" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "login":  "joao.silva",
    "nome":   "João Silva",
    "senha":  "senha1234",
    "perfil": "OPERADOR"
  }'

# Desativar usuário
curl -X PATCH "$BASE/usuarios/2/desativar" -H "Authorization: Bearer $TOKEN"

# Reativar usuário (lacuna corrigida — irreversível no legado)
curl -X PATCH "$BASE/usuarios/2/reativar" -H "Authorization: Bearer $TOKEN"

# Alterar senha (confirmar senha atual obrigatório)
curl -X PATCH "$BASE/usuarios/2/senha" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{ "senhaAtual": "senha1234", "senhaNova": "novaSenha99" }'

# ── 6. RELATÓRIO ──────────────────────────────────────────────────────────────
# Pedidos agrupados por status (porta relatorio_pedidos_status() do legado)
curl "$BASE/relatorios/pedidos-por-status" -H "Authorization: Bearer $TOKEN"

# Resposta exemplo:
# [
#   { "status": "ABERTO",     "quantidade": 3, "valorTotal": 540.00 },
#   { "status": "CONFIRMADO", "quantidade": 1, "valorTotal": 200.00 },
#   { "status": "FATURADO",   "quantidade": 5, "valorTotal": 1350.00 }
# ]
```

### Testes

```bash
cd moderno
mvn test
```

11 classes de teste — unitários (Mockito) e integração (MockMvc + H2).

---

## Estrutura do Repositório

```
.
├── legado/                # Sistema original Informix-4GL
│   ├── forms/             # Formulários .PER (telas de terminal)
│   ├── sql/               # schema.sql + seed.sql
│   └── src/               # Código-fonte .4gl
├── moderno/               # API Spring Boot 3
│   ├── Dockerfile
│   ├── docker-compose.yml
│   ├── pom.xml
│   └── src/
│       ├── main/java/br/com/pedidos/
│       │   ├── cliente/   # Controller, Service, Repository, Entity, DTOs
│       │   ├── produto/
│       │   ├── pedido/
│       │   ├── usuario/
│       │   ├── relatorio/
│       │   ├── security/  # JWT + filtros
│       │   └── config/    # Spring Security
│       └── test/
├── docs/
│   ├── as-is.md           # Análise do legado
│   └── arquitetura.md     # Diagramas detalhados
└── Bob/
    └── README.md          # Como o IBM Bob foi usado
```

---

## Segurança

- **Senhas:** BCrypt (fator 10) — nunca armazenadas em texto puro
- **Tokens:** JWT HMAC-SHA256, validade de 24 h
- **Perfis:** `CONSULTA` (GET only) · `OPERADOR` (GET + escrita) · `ADMIN` (acesso total)
- **Endpoints públicos:** `POST /auth/login` · `GET /actuator/health` · `GET /actuator/info`

> ⚠️ Troque `jwt.secret` em `application.properties` antes de qualquer deploy:
> ```bash
> openssl rand -base64 48
> ```

---

## Autora

**Giovanna de Andrade** — [@gii-andrade](https://github.com/gii-andrade)

Projeto desenvolvido com assistência do **IBM Bob** para demonstrar modernização de sistemas legados com IA.
