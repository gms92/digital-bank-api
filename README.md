# Digital Bank API

API REST para um banco digital simplificado com transferências entre contas, consulta de extrato e notificação ao cliente por email.

---

## Como rodar o projeto

### Pré-requisitos

- Java 21
- Maven 3.9+
- Docker e Docker Compose

### 1. Configurar variáveis de ambiente

A aplicação possui valores default para as variáveis de ambiente e sobe normalmente sem elas. Porém, para o envio de email funcionar, crie uma conta gratuita no [Mailtrap](https://mailtrap.io), acesse **Email Sandbox > SMTP Credentials** e copie as credenciais:

```
  MAIL_USERNAME=seu_username
  MAIL_PASSWORD=sua_password
```

### 2. Subir as dependências

```bash
  docker compose up -d
```

Isso sobe:
- **PostgreSQL 16** na porta `5432`
- **Kafka** na porta `9092`

### 3. Rodar a aplicação

Sem notificação por email:

```bash
  mvn spring-boot:run
```

Com notificação por email (credenciais do Mailtrap):

```bash
  MAIL_USERNAME=seu_username MAIL_PASSWORD=sua_password mvn spring-boot:run
```

A API ficará disponível em `http://localhost:8080`.

### 4. Acessar o Swagger ou importar collection no Postman

```
http://localhost:8080/swagger-ui.html
```

A descrição detalhada de cada campo dos requests e responses está disponível na seção **Schemas** no final da página do Swagger.

Como alternativa, a collection do Postman está disponível em `docs/digital-bank.postman_collection.json`.

### 5. Rodar os testes

Apenas testes unitários:

```bash
  mvn test
```

Testes unitários + integração:

```bash
  mvn verify
```

Os testes de integração usam **Testcontainers**, que sobem containers Docker automaticamente, sem necessidade de configuração manual.

---

## Decisões de design e arquitetura

### Arquitetura

Arquitetura **MVC** com services como camada de aplicação. Controllers delegam para services, que acessam repositórios JPA e publicam eventos Kafka.

```
Controller → Service → Repository (JPA/PostgreSQL)
                  ↓
             Kafka Producer → topic: transfer-funds-completed
                                    ↓
                     TransferFundsNotificationConsumer
```

Estrutura de pacotes por domínio: `account`, `transfer`, `notification`, `shared`.

Os controllers recebem e retornam DTOs (`AccountRequest`, `AccountResponse`, `TransferRequest`, `TransferResponse`, `StatementResponse`). O mapeamento entre DTOs e classes de domínio (`Account`, `Transfer`, `Statement`) é feito nos próprios DTOs via métodos estáticos `from()`.

### Autenticação

A API não possui autenticação intencionalmente. O foco do projeto é demonstrar consistência financeira, idempotência e comunicação assíncrona.

### Consistência e Alta Concorrência

Em um cenário de transferências concorrentes, duas transações podem tentar debitar e creditar as mesmas contas ao mesmo tempo. Sem controle, isso resulta em race condition e saldo inconsistente.

Para garantir consistência, transferências usam **lock pessimista** (`SELECT FOR UPDATE`) via `@Lock(LockModeType.PESSIMISTIC_WRITE)` no repositório JPA. Isso faz com que a primeira transação a adquirir o lock processe a transferência integralmente antes que qualquer outra possa modificar as mesmas contas.

As duas contas são bloqueadas em uma única query (`SELECT FOR UPDATE`) sempre na mesma ordem (UUID ASC). Isso garante que duas transferências concorrentes envolvendo as mesmas contas sempre adquiram os locks na mesma sequência, evitando que o PostgreSQL detecte deadlock e aborte uma das transações.

A transação tem timeout de 5 segundos (`@Transactional(timeout = 5)`) para evitar lock starvation. Se uma transação segurar o lock por muito tempo, ela é abortada automaticamente, liberando o lock para as demais.

### Idempotência

A operação de transferência é idempotente. O cliente gera um `transferId` (UUID) antes de enviar. O servidor verifica se o ID já existe antes de processar, cobrindo três cenários:

| Cenário | Status |
|---|---|
| `transferId` novo | `201 Created` — transferência processada normalmente |
| `transferId` repetido com os mesmos dados | `200 OK` — retorna a transferência existente sem reprocessar |
| `transferId` repetido com dados diferentes | `409 Conflict` — rejeita a requisição para evitar que um bug ou tentativa de fraude use um ID antigo para validar uma operação nova |

Isso garante que retries de rede não causem débitos duplicados, e que IDs reutilizados com dados divergentes sejam explicitamente rejeitados.

### Notificações

Após commit da transferência, um evento `TransferFundsCompletedEvent` é publicado no tópico Kafka `transfer-funds-completed`. O `TransferFundsNotificationConsumer` escuta o tópico e envia um email via **Mailtrap**. O evento só é publicado após o commit da transação no banco, ou seja, nunca é disparado se a transferência falhar. A publicação Kafka é **best-effort**: falha no broker não reverte a transferência.

### Banco de Dados

- **PostgreSQL 16** com schema versionado via **Liquibase**. As migrations ficam em `src/main/resources/db/changelog/` e rodam automaticamente na inicialização da aplicação, controladas pela tabela `DATABASECHANGELOG`
- Foreign keys entre as tabelas (`source_account_id`, `target_account_id`, `account_id`, `transfer_id`)
- Índices criados nas colunas `source_account_id`, `target_account_id`, `account_id` e `transfer_id`
- **HikariCP** com pool de conexões configurado


### Testes

- **Unitários**: services testados com Mockito (repositórios e Kafka mockados)
- **Integração**: controllers e fluxos completos testados com `@SpringBootTest` + **Testcontainers** (PostgreSQL e Kafka reais)
