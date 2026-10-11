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

| Método | Rota | Acesso | Descrição |
|--------|------|--------|-----------|
| GET | `/public/restaurant` | público | Dados do restaurante |
| GET | `/public/menu` | público | Cardápio paginado |
| POST | `/public/orders` | público | Cria pedido |
| GET | `/public/orders/{trackingCode}` | público | Consulta pedido pelo código UUID (não sequencial) |
| POST | `/auth/login` | público | Login admin (JWT) |
| GET | `/admin/menu` | JWT | Lista cardápio (paginado) |
| POST | `/admin/menu` | JWT | Cria item |
| PUT | `/admin/menu/{id}` | JWT | Atualiza item |
| DELETE | `/admin/menu/{id}` | JWT | Soft-delete (indisponível) |
| GET | `/admin/orders` | JWT | Lista pedidos (paginado) |
| PUT | `/admin/orders/{id}/status` | JWT | Atualiza status |

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

- **Banco:** Neon (Postgres). Use a connection string direta, convertida para JDBC (veja `byfood/.env.example`).
- **API:** Render, via `render.yaml` (Blueprint) com Docker. Health check em `/actuator/health`.
- **Front:** Vercel, apontando `VITE_API_URL` para a URL da API no Render.

## Segurança

- Rate limit por IP em `/auth/login` (5/min) e `POST /public/orders`
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

## Testes

```bash
.\mvnw.cmd clean verify
```

Requer Docker Desktop rodando (Testcontainers).
