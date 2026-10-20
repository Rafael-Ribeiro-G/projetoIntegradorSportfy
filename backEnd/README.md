# Sportfy — Backend (API REST)

API do **Sportfy – Pratique & Conecte-se & Divirta-se**, Projeto Integrador SENAI (4º semestre).
Conecta pessoas para organizar partidas esportivas casuais conforme esporte, nível e localização.

| Item | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 3.5 (Web, Data JPA, Security, Validation, Actuator) |
| Banco | PostgreSQL — **estrutura do Enzo** (schema `public`); a API só lê e grava dados |
| Autenticação | JWT, senhas com BCrypt |
| Documentação | Swagger / OpenAPI em `/swagger-ui.html` |
| Testes | JUnit 5, Mockito, MockMvc + H2 com a mesma estrutura do banco do Enzo |
| Deploy | Docker / Docker Compose + GitHub Actions (CI) |

Contrato das rotas para o front, mobile e chatbot: **[docs/API.md](docs/API.md)**.

---

## 1. Rodar na sua máquina

### Opção A — tudo no Docker (mais fácil)
```bash
docker compose up -d --build
```
Sobe um PostgreSQL local **com a mesma estrutura do servidor** (`sql/estrutura_banco_enzo.sql` + 12 esportes)
e a API. Acesse **http://localhost:8080/swagger-ui.html**.
Se você já tinha rodado a versão antiga, recrie o banco local: `docker compose down -v && docker compose up -d --build`.

### Opção B — pela IDE (IntelliJ/VS Code) ou terminal
Requisitos: JDK 21, Maven 3.9+ e um PostgreSQL com a estrutura do Enzo
(rode `sql/estrutura_banco_enzo.sql` e `sql/01_esportes_iniciais.sql` num banco vazio).
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
  criado com a **mesma estrutura do banco do Enzo** (`src/test/resources/schema-enzo-h2.sql`).

O GitHub Actions (`.github/workflows/backEnd-ci.yml`, na raiz do repositório) roda tudo a cada push
que mexe em `backEnd/` e gera a imagem Docker.

## 3. Deploy no servidor (Postgres já instalado, acesso via Tailscale + SSH)

A API roda **no próprio servidor**, em Docker, e fala com o Postgres por `localhost`.
O banco não precisa ficar exposto na rede; o time acessa só a API pelo IP do Tailscale.

**Quem faz o quê:**
- **Responsável pelo banco (Enzo)**: cria e altera as tabelas, e dá ao `back_sportfy` permissão
  para ler e gravar dados (`sql/00_preparar_banco.sql`). Se a tabela `esportes` estiver vazia,
  roda `sql/01_esportes_iniciais.sql`.
- **API**: não cria nem altera tabelas. Na subida, o Hibernate só **confere** se as tabelas e colunas
  que ela usa existem (se faltar algo, a API não sobe e o log diz o que falta).

```bash
ssh usuario@100.68.85.13
git clone -b backEnd https://github.com/Rafael-Ribeiro-G/projetoIntegradorSportfy.git
cd projetoIntegradorSportfy/backEnd

# 1) uma vez só, pelo responsável do banco (só dados: SELECT/INSERT/UPDATE/DELETE)
sudo -u postgres psql -d sportfy -f sql/00_preparar_banco.sql
sudo -u postgres psql -d sportfy -f sql/01_esportes_iniciais.sql   # se não houver esportes

# 2) configuração
cp .env.example .env && chmod 600 .env
nano .env        # DB_PASSWORD, JWT_SECRET (openssl rand -base64 64), CORS_ORIGENS

# 3) conferir tudo (tabelas, permissões, esportes, Docker, porta) — não altera nada
bash scripts/verificar-servidor.sh

# 4) subir e esperar ficar no ar
bash scripts/deploy.sh
```
Para atualizar depois: `git pull && bash scripts/deploy.sh`.
Logs: `docker compose -f docker-compose.prod.yml logs -f`.

### Atualizando a partir da versão anterior (até 19/10/2026)
A versão anterior criava tabelas próprias no schema `sportfy`. Agora a API usa as tabelas do Enzo:
1. No `.env`, troque `DB_SCHEMA=sportfy` por `DB_SCHEMA=public` (ou apague a linha) e apague
   `ADMIN_EMAIL`/`ADMIN_SENHA` (não são mais usados).
2. `git pull && bash scripts/verificar-servidor.sh && bash scripts/deploy.sh`.
3. Opcional, depois que tudo funcionar: apagar o schema antigo (só tinha dados de teste) e tirar do
   `back_sportfy` a permissão de criar schemas:
   ```bash
   sudo -u postgres psql -d sportfy -c 'DROP SCHEMA sportfy CASCADE;' -c 'REVOKE CREATE ON DATABASE sportfy FROM back_sportfy;'
   ```

Os scripts funcionam com `bash scripts/...` mesmo se o Git (vindo do Windows) tirar a permissão de execução.

> Sem Docker no servidor: instale o JDK 21, rode `mvn package -DskipTests` e
> `java -jar target/sportfy-backend.jar` com as variáveis do `.env` exportadas.

> **Front na Vercel (HTTPS) + API em HTTP** não funciona por bloqueio de "mixed content" do navegador.
> Para a apresentação, exponha a API com HTTPS (ex.: `tailscale funnel 8080`, Cloudflare Tunnel ou Nginx + certificado)
> — isso conversa com a etapa de HTTPS da Cibersegurança.

**Desenvolvendo na sua máquina contra o banco do servidor** (sem expor o Postgres):
`ssh -L 5432:localhost:5432 usuario@100.68.85.13` e use `DB_URL=jdbc:postgresql://localhost:5432/sportfy`.

## 4. Variáveis de ambiente

| Variável | Obrigatória | Padrão | Descrição |
|---|---|---|---|
| `DB_URL` | sim | `jdbc:postgresql://localhost:5432/sportfy` | URL JDBC do Postgres (com o nome do banco) |
| `DB_USER` / `DB_PASSWORD` | sim | `back_sportfy` / — | Usuário e senha do banco |
| `DB_SCHEMA` | não | `public` | Schema onde estão as tabelas do Enzo |
| `JWT_SECRET` | **sim** | — | Segredo do token (mín. 32 caracteres). Sem ele a API não sobe |
| `JWT_EXPIRACAO_MINUTOS` | não | `1440` (24h) | Validade do token |
| `CORS_ORIGENS` | não | `http://localhost:3000` | Origens do front, separadas por vírgula |
| `LOGIN_MAX_TENTATIVAS` | não | `5` | Erros de senha antes do bloqueio |
| `LOGIN_BLOQUEIO_MINUTOS` | não | `15` | Tempo de bloqueio |
| `SWAGGER_ENABLED` | não | `true` | Desligue em produção final se quiser |
| `JPA_DDL_AUTO` | não | `validate` | Hibernate só confere as tabelas na subida; nunca use `update`/`create` (a estrutura é do Enzo) |

## 5. Estrutura
```
src/main/java/br/senai/sportfy
├── config/        Segurança (Spring Security + CORS), propriedades, Swagger
├── controller/    Rotas REST (auth, usuários, esportes, locais, quadras, partidas)
├── domain/        Entidades JPA mapeadas nas tabelas do Enzo e enums
├── dto/           Objetos de entrada/saída com as validações
├── exception/     Exceções de negócio + tratamento global de erros
├── repository/    Acesso ao banco (Spring Data) e busca dinâmica de partidas
├── security/      JWT, filtro de autenticação, bloqueio de tentativas de login
├── service/       Regras de negócio
└── util/          Cálculo de distância (Haversine)
sql/estrutura_banco_enzo.sql      Cópia de referência da estrutura do banco (pg_dump do servidor)
sql/00_preparar_banco.sql         Permissões do back_sportfy (responsável do banco, uma vez)
sql/01_esportes_iniciais.sql      12 esportes básicos (se a tabela estiver vazia)
scripts/verificar-servidor.sh     Diagnóstico do servidor antes do deploy
scripts/deploy.sh                 Verifica, sobe o container e espera o health check
```

## 6. Modelo de dados (banco do Enzo, schema `public`)
```
usuarios ─┬─< locais (dono_id) ──< quadras >── esportes
          │                          │
          ├─< partidas (criador_id) >┘
          │        │
          └─< partida_participantes >┘   chave (partida_id, usuario_id)
```
Tabelas usadas pela API: `usuarios`, `esportes`, `locais`, `quadras`, `partidas`, `partida_participantes`.
Ainda sem rotas: `avaliacoes_quadras`, `avaliacoes_usuarios`, `campeonatos`, `campeonato_equipes`.

Convenções que a API segue ao gravar:
- `locais.coordenadas` (point) = `(longitude,latitude)`, mesma ordem dos mapas e do PostGIS;
- `partidas.status` = `aberta` | `cancelada` (a API também entende `finalizada`);
- `partida_participantes.status_confirmacao` = `confirmado` quando a pessoa entra
  (registros `pendente` não ocupam vaga; ao sair, a linha é apagada).

Se o Enzo mudar o banco (nova coluna, nova tabela), atualize `sql/estrutura_banco_enzo.sql` e
`src/test/resources/schema-enzo-h2.sql` para os testes continuarem iguais ao servidor.

## 7. Segurança e LGPD (resumo)
- Senhas com **BCrypt**; nunca aparecem em resposta ou log.
- **JWT** assinado (HMAC-SHA); trocar a senha ou excluir a conta invalida os tokens antigos.
- **Bloqueio de força bruta**: 5 erros → login bloqueado por 15 min (resposta 429).
- **Validação** de todos os campos; `<` e `>` bloqueados em textos (XSS); consultas parametrizadas (SQL Injection).
- **Tratamento global de erros**: respostas padronizadas, sem stack trace nem SQL vazando.
- Headers de segurança (CSP, X-Frame-Options, HSTS, nosniff), CORS restrito às origens configuradas.
- Usuário do banco da API **sem permissão para criar ou alterar tabelas** (só dados).
- Container roda com usuário sem privilégios; segredos só em variáveis de ambiente.
- **LGPD**: perfil público sem e-mail/telefone, exportação dos dados (`GET /api/usuarios/me/dados`)
  e exclusão da conta (`POST /api/usuarios/me/exclusao`).
