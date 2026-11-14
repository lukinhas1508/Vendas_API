# Vendas API

API REST para gestão de **clientes, produtos, pedidos, estoque e vendas**, com autenticação JWT.
Projeto de portfólio de backend com Java e Spring Boot.

## Tecnologias

| Área | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 3 (Web, Data JPA, Security, Validation) |
| Banco de dados | PostgreSQL 16, migrações com Flyway |
| Segurança | Spring Security + JWT (jjwt), senhas com BCrypt |
| Documentação | Swagger / OpenAPI (springdoc) |
| Testes | JUnit 5, Mockito, AssertJ, MockMvc |
| Infra | Docker, Docker Compose, GitHub Actions |

## Funcionalidades

- **Autenticação**: cadastro de usuário, login com JWT e dois perfis (`ADMIN` e `VENDEDOR`).
- **Clientes**: CRUD com validação de CPF e e-mail, sem duplicidade.
- **Produtos**: CRUD com exclusão lógica, para preservar o histórico dos pedidos.
- **Estoque**: entradas manuais e histórico de movimentações por produto.
- **Pedidos**: criação com baixa automática de estoque, pagamento e cancelamento com devolução ao estoque.
- **Vendas**: resumo do período (quantidade, total e ticket médio) e ranking de produtos mais vendidos.

## Como rodar

Pré-requisitos: Docker. Para rodar fora do Docker, também Java 21 e Maven.

**1. Configure as variáveis de ambiente**

```bash
cp .env.example .env
```

Edite o `.env` e troque as senhas e o `JWT_SECRET` (mínimo de 32 caracteres).
O `.env` está no `.gitignore`: nenhuma senha vai para o repositório.

**2a. Tudo com Docker**

```bash
docker compose up --build
```

**2b. Ou só o banco no Docker e a API pelo Maven**

```bash
docker compose up -d db
mvn spring-boot:run
```

**3. Acesse a documentação**

http://localhost:8080/swagger-ui.html

Na primeira execução, a API cria as tabelas (Flyway) e o usuário administrador
definido em `ADMIN_EMAIL` / `ADMIN_PASSWORD`.

## Como usar

1. Faça login em `POST /api/auth/login` com o e-mail e a senha do administrador.
2. No Swagger, clique em **Authorize** e cole o token recebido.
3. Cadastre um cliente e um produto, crie um pedido e confirme o pagamento.

O mesmo fluxo com `curl`:

```bash
# login (troque e-mail e senha pelos do seu .env)
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@vendas.local","senha":"sua-senha"}' | sed 's/.*"token":"\([^"]*\)".*/\1/')

# cliente
curl -X POST http://localhost:8080/api/clientes \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"nome":"Ana Souza","email":"ana@email.com","cpf":"529.982.247-25","telefone":"11999990000"}'

# produto com 10 unidades em estoque
curl -X POST http://localhost:8080/api/produtos \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"nome":"Teclado mecânico","descricao":"ABNT2","preco":199.90,"estoqueInicial":10}'

# pedido de 2 unidades: o estoque cai para 8
curl -X POST http://localhost:8080/api/pedidos \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"clienteId":1,"itens":[{"produtoId":1,"quantidade":2}]}'

# pagamento: o pedido passa a contar como venda
curl -X POST http://localhost:8080/api/pedidos/1/pagamento -H "Authorization: Bearer $TOKEN"
```

## Endpoints

| Método | Rota | Descrição | Acesso |
|---|---|---|---|
| POST | `/api/auth/registrar` | Cadastra usuário (perfil VENDEDOR) | Público |
| POST | `/api/auth/login` | Devolve o token JWT | Público |
| GET | `/api/clientes` | Lista clientes (paginado) | Autenticado |
| GET | `/api/clientes/{id}` | Busca cliente | Autenticado |
| POST | `/api/clientes` | Cadastra cliente | Autenticado |
| PUT | `/api/clientes/{id}` | Atualiza cliente | Autenticado |
| DELETE | `/api/clientes/{id}` | Exclui cliente sem pedidos | ADMIN |
| GET | `/api/produtos?nome=` | Lista produtos ativos (paginado) | Autenticado |
| GET | `/api/produtos/{id}` | Busca produto | Autenticado |
| POST | `/api/produtos` | Cadastra produto | Autenticado |
| PUT | `/api/produtos/{id}` | Atualiza produto | Autenticado |
| DELETE | `/api/produtos/{id}` | Desativa produto | ADMIN |
| POST | `/api/produtos/{id}/estoque/entradas` | Entrada de estoque | Autenticado |
| GET | `/api/produtos/{id}/estoque/movimentacoes` | Histórico do estoque | Autenticado |
| POST | `/api/pedidos` | Cria pedido e baixa o estoque | Autenticado |
| GET | `/api/pedidos?status=` | Lista pedidos (paginado) | Autenticado |
| GET | `/api/pedidos/{id}` | Busca pedido | Autenticado |
| POST | `/api/pedidos/{id}/pagamento` | Confirma o pagamento | Autenticado |
| POST | `/api/pedidos/{id}/cancelamento` | Cancela e devolve o estoque | Autenticado |
| GET | `/api/vendas/resumo?inicio=&fim=` | Total, quantidade e ticket médio | ADMIN |
| GET | `/api/vendas/produtos-mais-vendidos?inicio=&fim=&limite=` | Ranking de produtos | ADMIN |

Listagens aceitam `page`, `size` e `sort` (ex.: `?page=0&size=10&sort=nome,asc`). Datas no formato `AAAA-MM-DD`.

### Códigos de erro

Os erros seguem o padrão *Problem Details* (RFC 7807):

| Código | Quando |
|---|---|
| 400 | Campos inválidos (a resposta lista o erro de cada campo) |
| 401 | Token ausente, inválido ou expirado; e-mail ou senha incorretos |
| 403 | Usuário sem o perfil exigido |
| 404 | Recurso não encontrado |
| 409 | E-mail ou CPF duplicado, cliente com pedidos, alteração concorrente |
| 422 | Regra de negócio violada (estoque insuficiente, pedido já pago etc.) |

## Regras de negócio

- Um pedido nasce como `AGUARDANDO_PAGAMENTO` e já **reserva o estoque** dos itens.
- Se faltar estoque para qualquer item, o pedido inteiro é recusado (a transação é desfeita).
- O item guarda o **preço do momento da compra**: mudar o preço do produto não altera pedidos antigos.
- Pedido pago vira **venda** e entra nos relatórios; pedido pago não pode ser cancelado.
- Cancelar um pedido devolve os itens ao estoque.
- Toda alteração de estoque gera uma **movimentação** (entrada ou saída) com o motivo.
- Dois pedidos simultâneos para o último item não vendem o mesmo estoque duas vezes:
  o produto usa *lock* otimista (`@Version`) e o segundo pedido recebe 409.

## Arquitetura

O código é organizado por funcionalidade. Dentro de cada pacote, as camadas são as tradicionais:

```
Controller  ->  Service  ->  Repository  ->  PostgreSQL
 (HTTP, DTOs)   (regras e     (Spring Data
                transações)    JPA)
```

```
src/main/java/com/lucas/vendasapi
├── auth        cadastro e login
├── cliente     CRUD de clientes
├── produto     CRUD de produtos
├── estoque     entradas, saídas e histórico
├── pedido      pedidos e itens
├── venda       relatórios de vendas
├── security    geração e validação do JWT
├── config      Spring Security, Swagger e criação do admin
└── exception   exceções de negócio e tratamento global de erros
```

Algumas decisões:

- **DTOs como `record`**: as entidades JPA nunca são expostas na API.
- **Regras na entidade quando cabem nela** (`Produto.baixarEstoque`, `Pedido.pagar`), o que deixa os testes simples.
- **Flyway** cria o schema; o Hibernate só valida (`ddl-auto: validate`).
- **API stateless**: sem sessão, cada requisição é autenticada pelo token.

## Testes

```bash
mvn test
```

Os testes são unitários e não precisam de banco de dados:

- **Services** com Mockito: pedidos, estoque, clientes, produtos e relatórios de vendas.
- **Entidade** `Produto`: regras de estoque.
- **JWT**: token válido, adulterado, expirado e de outro segredo.
- **Controller** de clientes com MockMvc: rotas, validação e códigos HTTP.

O workflow em `.github/workflows/ci.yml` compila o projeto e roda os testes a cada push.
