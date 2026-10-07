# Sportfy — Backend (API REST)

API do **Sportfy – Pratique & Conecte-se & Divirta-se**, Projeto Integrador SENAI (4º semestre).
Conecta pessoas para organizar partidas esportivas casuais conforme esporte, nível e localização.

| Item | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 3.5 (Web, Data JPA, Security, Validation, Actuator) |
| Banco | PostgreSQL (tabelas criadas e versionadas pelo **Flyway**) |
| Autenticação | JWT (perfis `USER` e `ADMIN`), senhas com BCrypt |
| Documentação | Swagger / OpenAPI em `/swagger-ui.html` |
| Testes | JUnit 5, Mockito, MockMvc + H2 |
| Deploy | Docker / Docker Compose + GitHub Actions (CI) |

Contrato das rotas para o front, mobile e chatbot: **[docs/API.md](docs/API.md)**.

---

## 1. Rodar na sua máquina

### Opção A — tudo no Docker (mais fácil)
```bash
docker compose up -d --build
```
Sobe o PostgreSQL e a API. Acesse **http://localhost:8080/swagger-ui.html**.
Admin de desenvolvimento: `admin@sportfy.local` / `Admin12345`.

### Opção B — pela IDE (IntelliJ/VS Code) ou terminal
Requisitos: JDK 21, Maven 3.9+ e um PostgreSQL rodando.
```bash
# variáveis mínimas
export DB_URL=jdbc:postgresql://localhost:5432/sportfy
export DB_USER=back_sportfy
export DB_PASSWORD=sua-senha
export JWT_SECRET=$(openssl rand -base64 64)

mvn spring-boot:run
```
No Windows (PowerShell) use `$env:DB_URL="..."` no lugar de `export`.

## 2. Testes
```bash
mvn verify
```
- **Unitários** (`service/`, `domain/`, `util/`, `security/`): regras de negócio com Mockito, sem banco.
- **Integração** (`integracao/`): sobem a API inteira e testam rotas + segurança + banco H2
  rodando as **mesmas migrations** do PostgreSQL.

O GitHub Actions (`.github/workflows/backEnd-ci.yml`, na raiz do repositório) roda tudo a cada push
que mexe em `backEnd/` e gera a imagem Docker.

## 3. Deploy no servidor (Postgres já instalado, acesso via Tailscale + SSH)

A API roda **no próprio servidor**, em Docker, e fala com o Postgres por `localhost`.
O banco não precisa ficar exposto na rede; o time acessa só a API pelo IP do Tailscale.

```bash
ssh usuario@100.68.85.13
git clone -b backEnd https://github.com/Rafael-Ribeiro-G/projetoIntegradorSportfy.git
cd projetoIntegradorSportfy/backEnd

# 1) uma vez só, como superusuário do Postgres (cria o banco se faltar e dá as permissões)
sudo -u postgres psql -v db=NOME_DO_BANCO -f sql/00_preparar_banco.sql

# 2) configuração
cp .env.example .env && chmod 600 .env
nano .env        # DB_URL (nome do banco), DB_PASSWORD, JWT_SECRET (openssl rand -base64 64), CORS_ORIGENS, ADMIN_*

# 3) conferir tudo (banco, permissões, schema, Docker, porta) — não altera nada
bash scripts/verificar-servidor.sh

# 4) subir e esperar ficar no ar
bash scripts/deploy.sh
```
Os scripts funcionam com `bash scripts/...` mesmo se o Git (vindo do Windows) tirar a permissão de execução.
Para deixá-los executáveis no repositório, rode uma vez no Windows antes do commit:
`git update-index --chmod=+x scripts/deploy.sh scripts/verificar-servidor.sh`.
O `verificar-servidor.sh` diz exatamente o que corrigir se algo estiver errado: nome do banco
(lista os bancos acessíveis), senha, falta de permissão, schema ocupado, Docker ou porta em uso.

**Schema próprio:** as tabelas ficam no schema `sportfy` (variável `DB_SCHEMA`), que o Flyway cria
sozinho. Se o banco já tiver outras tabelas no `public`, não há conflito.

Para atualizar depois: `git pull && bash scripts/deploy.sh`.
Logs: `docker compose -f docker-compose.prod.yml logs -f`.

> Sem Docker no servidor: instale o JDK 21, rode `mvn package -DskipTests` e
> `java -jar target/sportfy-backend.jar` com as variáveis do `.env` exportadas.

> **Front na Vercel (HTTPS) + API em HTTP** não funciona por bloqueio de "mixed content" do navegador.
> Para a apresentação, exponha a API com HTTPS (ex.: `tailscale funnel 8080`, Cloudflare Tunnel ou Nginx + certificado)
> — isso conversa com a etapa de HTTPS da Cibersegurança.

**Desenvolvendo na sua máquina contra o banco do servidor** (sem expor o Postgres):
`ssh -L 5432:localhost:5432 usuario@100.68.85.13` e use `DB_URL=jdbc:postgresql://localhost:5432/NOME_DO_BANCO`.

## 4. Variáveis de ambiente

| Variável | Obrigatória | Padrão | Descrição |
|---|---|---|---|
| `DB_URL` | sim | `jdbc:postgresql://localhost:5432/sportfy` | URL JDBC do Postgres (com o nome do banco) |
| `DB_USER` / `DB_PASSWORD` | sim | `back_sportfy` / — | Usuário e senha do banco |
| `DB_SCHEMA` | não | `sportfy` | Schema onde ficam as tabelas (criado pelo Flyway) |
| `JWT_SECRET` | **sim** | — | Segredo do token (mín. 32 caracteres). Sem ele a API não sobe |
| `JWT_EXPIRACAO_MINUTOS` | não | `1440` (24h) | Validade do token |
| `CORS_ORIGENS` | não | `http://localhost:3000` | Origens do front, separadas por vírgula |
| `ADMIN_EMAIL` / `ADMIN_SENHA` | não | — | Cria o primeiro ADMIN na subida |
| `LOGIN_MAX_TENTATIVAS` | não | `5` | Erros de senha antes do bloqueio |
| `LOGIN_BLOQUEIO_MINUTOS` | não | `15` | Tempo de bloqueio |
| `SWAGGER_ENABLED` | não | `true` | Desligue em produção final se quiser |
| `JPA_DDL_AUTO` | não | `validate` | Hibernate só confere o schema; use `none` se der conflito |

## 5. Estrutura
```
src/main/java/br/senai/sportfy
├── config/        Segurança (Spring Security + CORS), propriedades, Swagger, admin inicial
├── controller/    Rotas REST (auth, usuários, esportes, eventos, admin)
├── domain/        Entidades JPA (Usuario, Esporte, Evento, Participacao) e enums
├── dto/           Objetos de entrada/saída com as validações
├── exception/     Exceções de negócio + tratamento global de erros
├── repository/    Acesso ao banco (Spring Data) e busca dinâmica de eventos
├── security/      JWT, filtro de autenticação, bloqueio de tentativas de login
├── service/       Regras de negócio
└── util/          Cálculo de distância (Haversine)
src/main/resources/db/migration   Scripts SQL versionados (Flyway)
sql/00_preparar_banco.sql         Prepara banco e permissões no servidor (superusuário, uma vez)
scripts/verificar-servidor.sh     Diagnóstico do servidor antes do deploy
scripts/deploy.sh                 Verifica, sobe o container e espera o health check
```

## 6. Modelo de dados
```
usuarios ──< usuario_esportes >── esportes
   │                                 │
   │ organiza                        │
   ├──────────────< eventos >────────┘
   │                   │
   └──< participacoes >┘   (único por evento+usuário; status CONFIRMADO/CANCELADO)
```
Para mudar o banco, **não edite** `V1__...sql` depois que ele rodou no servidor: crie um novo arquivo
`V3__descricao.sql` com o `ALTER TABLE`. O Flyway aplica só o que é novo.

## 7. Segurança e LGPD (resumo)
- Senhas com **BCrypt**; nunca aparecem em resposta ou log.
- **JWT** assinado (HMAC-SHA); usuário desativado perde o acesso na hora.
- **Bloqueio de força bruta**: 5 erros → login bloqueado por 15 min (resposta 429).
- **Validação** de todos os campos; `<` e `>` bloqueados em textos (XSS); consultas parametrizadas (SQL Injection).
- **Tratamento global de erros**: respostas padronizadas, sem stack trace nem SQL vazando.
- Headers de segurança (CSP, X-Frame-Options, HSTS, nosniff), CORS restrito às origens configuradas.
- Container roda com usuário sem privilégios; segredos só em variáveis de ambiente.
- **LGPD**: aceite obrigatório dos termos (com data registrada), perfil público sem e-mail/telefone,
  exportação dos dados (`GET /api/usuarios/me/dados`) e exclusão/anonimização da conta (`POST /api/usuarios/me/exclusao`).
