# byFood API

API de restaurante (estilo iFood) para gerenciar cardápio e pedidos, com confirmação via WhatsApp. Spring Boot 4.

## Stack

- Java 17, Spring Boot 4.1.0, Maven
- Spring Data JPA + PostgreSQL 16
- Flyway (schema versionado)
- Spring Security + JWT (jjwt 0.12.6)
- Springdoc OpenAPI (Swagger UI)
- Testcontainers (testes de integração)
- Docker + Docker Compose

## Como rodar

```bash
# Com Docker (recomendado)
docker compose up --build -d

# Sem Docker (precisa de Postgres rodando na porta 5432)
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

A API sobe na porta **8080**.

### Credenciais admin

- Usuário: `admin`
- Senha local: `admin123` (em produção é substituída por `ADMIN_PASSWORD`)

### Swagger UI

- `http://localhost:8080/swagger-ui.html`

## Endpoints

Perfis: **visitante**, **CUSTOMER** (cliente) e **STORE** (dono de loja). Cada loja só enxerga e altera os próprios dados.

| Método | Rota | Acesso | Descrição |
|--------|------|--------|-----------|
| GET | `/public/restaurants` | público | Vitrine de lojas (`q`, `cuisine`, paginada) |
| GET | `/public/restaurants/cuisines` | público | Tipos de comida disponíveis |
| GET | `/public/restaurants/{slug}` | público | Dados da loja |
| GET | `/public/restaurants/{slug}/menu` | público | Cardápio (itens disponíveis) |
| POST | `/public/restaurants/{slug}/orders` | público | Cria pedido (visitante ou cliente logado) |
| GET | `/public/orders/{trackingCode}` | público | Acompanha pedido pelo código UUID |
| POST | `/auth/login` | público | Login (JWT com papel) |
| POST | `/auth/register/customer` | público | Cadastro de cliente |
| POST | `/auth/register/store` | público | Cadastro de loja + dono |
| GET | `/me`, `/me/orders` | CUSTOMER | Perfil e histórico de pedidos |
| GET/PUT | `/store/restaurant` | STORE | Dados da própria loja |
| GET/POST/PUT/DELETE | `/store/menu[/{id}]` | STORE | Cardápio da própria loja (DELETE = remoção lógica) |
| GET | `/store/orders` | STORE | Pedidos da loja (`status`, paginado) |
| PUT | `/store/orders/{id}/status` | STORE | Atualiza status |

## Variáveis de ambiente

| Variável | Dev (default) | Prod |
|----------|---------------|------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/byfood` | obrigatório |
| `DB_USERNAME` | `postgres` | obrigatório |
| `DB_PASSWORD` | `123456` | obrigatório |
| `JWT_SECRET` | `byfood-dev-secret-key-...` | obrigatório (≥ 32 bytes) |
| `JWT_EXPIRATION` | `86400000` | opcional (`43200000`) |
| `ADMIN_USERNAME` | `admin` | opcional |
| `ADMIN_PASSWORD` | vazio (mantém `admin123` do seed) | **obrigatório**, mín. 8 caracteres |
| `CORS_ALLOWED_ORIGINS` | `localhost:3000,5173,4173` | **obrigatório** (aceita curingas, ex.: `https://byfood-*.vercel.app`) |
| `DB_POOL_SIZE` | `5` | opcional |

## Deploy

- **Banco:** Neon (Postgres). Use a connection string direta (sem `-pooler`). `DB_URL` aceita tanto o formato JDBC quanto o `postgresql://user:senha@host/db?sslmode=require` copiado do Neon (usuário/senha são extraídos automaticamente).
- **API:** Render, via `render.yaml` (Blueprint) com Docker. Health check em `/actuator/health`.
- **Front:** Vercel, apontando `VITE_API_URL` para a URL da API no Render (sem ela o front chama `/api`, que na Vercel não existe). O plano free do Render "dorme" após inatividade: a primeira requisição pode levar 30–60 s.

## Segurança

- Rate limit por IP em `/auth/login` (5/min), cadastros e criação de pedidos
- Pedidos públicos identificados por UUID (`trackingCode`), evitando enumeração
- CORS restrito, headers de segurança, autorização deny-by-default, Swagger desabilitado em produção

## Migrations

| Versão | Conteúdo |
|--------|----------|
| V1 | Tabelas `restaurant` e `menu_item` + seed restaurante |
| V2 | Tabela `admin_user` + seed admin |
| V3 | Tabelas `customer_order` e `order_item` |
| V4 | Seed de 16 itens no cardápio (Pizzas, Burgers, Bebidas, Sobremesas) |
| V5 | Imagens Unsplash para todos os itens |
| V6 | `tracking_code` (UUID) em pedidos e índice do cardápio |
| V7 | Multi-loja: `app_user`, lojas com dono/slug, cardápio e pedidos por loja (o `admin` vira dono da loja existente) |

## Testes

```bash
.\mvnw.cmd clean verify
```

Requer Docker Desktop rodando (Testcontainers).
