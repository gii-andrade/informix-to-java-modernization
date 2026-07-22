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
# Instalar podman-compose (uma vez)
pip install podman-compose        # Linux
brew install podman-compose       # macOS

# Subir os serviços
cd moderno
podman compose up --build
```

> O Podman roda rootless (sem daemon root), tornando-o preferível em ambientes
> corporativos com restrições de segurança.

Aguarde ~30 s até ambos os serviços ficarem `healthy`. A API sobe em `http://localhost:8080/api/v1`.

### Autenticar e usar

```bash
# 1. Login — obtém o token JWT
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"login":"admin","senha":"admin123"}'

# Resposta: { "token": "eyJ...", "perfil": "ADMIN", "expiresInMs": 86400000 }

# 2. Usar o token
TOKEN="eyJ..."
curl http://localhost:8080/api/v1/clientes \
  -H "Authorization: Bearer $TOKEN"
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
