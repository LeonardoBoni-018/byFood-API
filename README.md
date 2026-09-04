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
- Senha: `admin123`

### Swagger UI

- `http://localhost:8080/swagger-ui.html`

## Endpoints

| Método | Rota | Acesso | Descrição |
|--------|------|--------|-----------|
| GET | `/public/restaurant` | público | Dados do restaurante |
| GET | `/public/menu` | público | Cardápio paginado |
| POST | `/public/orders` | público | Cria pedido |
| GET | `/public/orders/{id}` | público | Consulta pedido |
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
| `JWT_EXPIRATION` | `86400000` | obrigatório |

## Migrations

| Versão | Conteúdo |
|--------|----------|
| V1 | Tabelas `restaurant` e `menu_item` + seed restaurante |
| V2 | Tabela `admin_user` + seed admin |
| V3 | Tabelas `customer_order` e `order_item` |
| V4 | Seed de 16 itens no cardápio (Pizzas, Burgers, Bebidas, Sobremesas) |
| V5 | Imagens Unsplash para todos os itens |

## Testes

```bash
.\mvnw.cmd clean verify
```

Requer Docker Desktop rodando (Testcontainers).
