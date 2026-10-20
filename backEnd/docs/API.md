# Sportfy API — guia para Front, Mobile e Chatbot

Base: `http://100.68.85.13:8080` (com o Tailscale ligado) · Documentação interativa: `/swagger-ui.html`

- Corpo das requisições e respostas em **JSON** (`Content-Type: application/json`).
- Rotas protegidas exigem o header **`Authorization: Bearer <token>`**.
- **Ids são UUID** (texto), ex.: `"c3062661-3635-4eee-aeea-c6a4fae056c9"`. Só esporte tem id numérico.
- Datas no formato ISO **sem fuso**, horário de Brasília: `2026-10-25T19:00:00`.
- Campos `null` não aparecem na resposta.

## Como os dados se ligam (banco do Enzo)

```
usuarios ─┬─< locais (dono) ──< quadras >── esportes
          │                       │
          ├─< partidas (criador) >┘   a partida acontece numa quadra; esporte e local vêm dela
          │        │
          └─< partida_participantes >┘
```
Para criar uma partida é preciso ter **local → quadra**. Fluxo da tela Criar Partida:
escolher (ou cadastrar) o local, escolher a quadra, informar horário de início e fim.

## Erros (formato único)
```json
{
  "timestamp": "2026-10-19T12:00:00",
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "Dados inválidos. Verifique os campos.",
  "caminho": "/api/auth/registro",
  "campos": [ { "campo": "email", "mensagem": "deve ser um e-mail válido" } ]
}
```
| Status | Quando | O que o front faz |
|---|---|---|
| 400 | Campo inválido (`campos` diz qual) ou id em formato errado | Mostrar a mensagem embaixo do campo |
| 401 | Sem token, token vencido ou senha errada | Mandar para a tela de Login |
| 403 | Sem permissão (ex.: editar partida ou local de outra pessoa) | Mostrar `mensagem` |
| 404 | Não existe | Mostrar `mensagem` |
| 409 | Conflito (e-mail já cadastrado, já participa, quadra ocupada no horário) | Mostrar `mensagem` |
| 422 | Regra de negócio (partida lotada, cancelada, já começou) | Mostrar `mensagem` |
| 429 | Muitas tentativas de login | Pedir para esperar 15 min |

---

## Autenticação (telas Login e Criar Conta)

### `POST /api/auth/registro` — público
```json
{
  "nome": "Ana Souza",
  "email": "ana@email.com",
  "senha": "Senha1234",
  "telefone": "(11) 98888-7777",
  "nivelHabilidade": 3
}
```
Obrigatórios: `nome`, `email`, `senha` (8+ caracteres, letra e número).
`nivelHabilidade` vai de **1 (iniciante) a 5 (avançado)**, opcional. Não há aceite de termos.
Resposta **201** = mesma do login.

### `POST /api/auth/login` — público
```json
{ "email": "ana@email.com", "senha": "Senha1234" }
```
Resposta **200**:
```json
{
  "token": "eyJhbGciOi...",
  "tipo": "Bearer",
  "expiraEmSegundos": 86400,
  "usuario": {
    "id": "c3062661-3635-4eee-aeea-c6a4fae056c9",
    "nome": "Ana Souza",
    "email": "ana@email.com",
    "telefone": "(11) 98888-7777",
    "nivelHabilidade": 3,
    "criadoEm": "2026-10-19T22:33:07"
  }
}
```
Guarde o `token` (DataStore/SharedPreferences no Android, memória/localStorage no web).
Depois de 5 senhas erradas o e-mail fica bloqueado por 15 minutos (**429**).

---

## Conta / Perfil

| Método | Rota | O quê |
|---|---|---|
| GET | `/api/usuarios/me` | Dados do usuário logado |
| PUT | `/api/usuarios/me` | Atualiza `nome`, `telefone`, `nivelHabilidade` (o que vier `null` é apagado) |
| PUT | `/api/usuarios/me/senha` | `{ "senhaAtual": "...", "novaSenha": "..." }` → devolve **token novo** (o antigo para de valer) |
| GET | `/api/usuarios/me/partidas` | `{ "criadas": [...], "participando": [...] }` |
| GET | `/api/usuarios/me/dados` | LGPD: exporta tudo o que o Sportfy guarda da pessoa |
| POST | `/api/usuarios/me/exclusao` | LGPD: `{ "senha": "..." }` → **204**. Apaga a conta e as partidas que ela criou |
| GET | `/api/usuarios/{id}` | Perfil público: `id`, `nome`, `nivelHabilidade` (sem e-mail/telefone) |

---

## Esportes — público, só leitura

`GET /api/esportes` →
```json
[ { "id": 2, "nome": "Futsal", "quantidadeJogadoresPadrao": 10 }, ... ]
```
`GET /api/esportes/{id}` → um esporte. Quem cadastra esportes é o responsável pelo banco.

---

## Locais

| Método | Rota | Login | O quê |
|---|---|---|---|
| GET | `/api/locais?texto=&latitude=&longitude=&raioKm=` | não | Lista. `texto` busca no nome e endereço; com latitude/longitude ordena pelos mais próximos (e traz `distanciaKm`) |
| GET | `/api/locais/{id}` | não | Detalhe com a lista de `quadras` |
| POST | `/api/locais` | sim | Cadastra; quem cadastra vira o **dono** |
| PUT | `/api/locais/{id}` | dono | Edita |
| DELETE | `/api/locais/{id}` | dono | Exclui o local e as quadras (409 se alguma quadra tiver partidas) |

Corpo do POST/PUT:
```json
{
  "nome": "Parque Ibirapuera",
  "endereco": "Av. Pedro Álvares Cabral - Vila Mariana, São Paulo/SP",
  "latitude": -23.5874,
  "longitude": -46.6576
}
```
Latitude e longitude são opcionais, mas vão **juntas**. Resposta:
```json
{
  "id": "fe0b55e3-2a82-45ed-aea0-303559a8c7fc",
  "nome": "Parque Ibirapuera",
  "endereco": "Av. Pedro Álvares Cabral - Vila Mariana, São Paulo/SP",
  "latitude": -23.5874,
  "longitude": -46.6576,
  "dono": { "id": "c3062661-...", "nome": "Ana Souza" },
  "quadras": [ { "id": "5fa848b1-...", "nome": "Quadra 1", "precoHora": 80.50,
                 "esporte": { "id": 2, "nome": "Futsal", "quantidadeJogadoresPadrao": 10 } } ]
}
```

## Quadras

| Método | Rota | Login | O quê |
|---|---|---|---|
| GET | `/api/quadras?localId=&esporteId=` | não | Lista (filtros opcionais) |
| GET | `/api/quadras/{id}` | não | Detalhe (com o local) |
| POST | `/api/quadras` | dono do local | Cadastra |
| PUT | `/api/quadras/{id}` | dono do local | Edita nome, esporte e preço (`localId` é ignorado) |
| DELETE | `/api/quadras/{id}` | dono do local | Exclui (409 se tiver partidas) |

```json
{ "localId": "fe0b55e3-...", "esporteId": 2, "nome": "Quadra 1", "precoHora": 80.50 }
```
`precoHora` é opcional.

---

## Partidas (telas Home, Ver Partidas e Criar Partida)

### `GET /api/partidas` — público
Filtros opcionais: `esporteId`, `localId`, `quadraId`, `texto` (nome do local, endereço ou nome da quadra),
`dataInicio`, `dataFim`, `latitude` + `longitude` + `raioKm`, `incluirEncerradas`, `incluirCanceladas`,
`pagina` (começa em 0), `tamanho` (1 a 50, padrão 20).
Por padrão traz só partidas que ainda não terminaram e não foram canceladas, da mais próxima no tempo para a mais distante.

Resposta:
```json
{
  "conteudo": [
    {
      "id": "6acc73c7-e8f5-4634-8d1b-7e9d429885b9",
      "esporte": { "id": 2, "nome": "Futsal", "quantidadeJogadoresPadrao": 10 },
      "quadra": { "id": "5fa848b1-...", "nome": "Quadra 1", "precoHora": 80.50 },
      "local": { "id": "fe0b55e3-...", "nome": "Parque Ibirapuera", "endereco": "...",
                 "latitude": -23.5874, "longitude": -46.6576 },
      "criador": { "id": "c3062661-...", "nome": "Ana Souza" },
      "dataHoraInicio": "2026-10-25T19:00:00",
      "dataHoraFim": "2026-10-25T20:30:00",
      "duracaoMinutos": 90,
      "maxJogadores": 10,
      "participantesConfirmados": 4,
      "vagasRestantes": 6,
      "status": "ABERTA",
      "situacao": "ABERTA",
      "distanciaKm": 4.2,
      "usuarioParticipa": false
    }
  ],
  "pagina": 0, "tamanho": 20, "totalElementos": 1, "totalPaginas": 1, "ultima": true
}
```
- `situacao` (use esta na tela): `ABERTA`, `LOTADA`, `EM_ANDAMENTO`, `ENCERRADA`, `CANCELADA`.
- `status` é o que está gravado no banco: `ABERTA`, `CANCELADA` ou `FINALIZADA`.
- `vagasRestantes` some quando a partida não tem limite de jogadores.
- `distanciaKm` só vem quando a busca manda latitude/longitude.
- `usuarioParticipa` só vem quando a requisição tem token.

### `GET /api/partidas/sugestoes?latitude=&longitude=&esporteId=&limite=5` — logado
Próximas partidas **com vaga** em que o usuário ainda não está (nem criou). Com latitude/longitude, as mais
próximas vêm primeiro. Usado pela Home e pelo chatbot.

### `GET /api/partidas/{id}` — público
### `POST /api/partidas` — logado
```json
{
  "quadraId": "5fa848b1-8451-458a-a82e-c717ea916d0b",
  "dataHoraInicio": "2026-10-25T19:00:00",
  "dataHoraFim": "2026-10-25T20:30:00",
  "maxJogadores": 10
}
```
- `maxJogadores` (2 a 100) é opcional: sem ele, usa o padrão do esporte da quadra.
- Quem cria já entra como participante. Resposta **201**.
- **409** se a quadra já tiver outra partida nesse horário.
- Duração de 15 min a 12 h; no máximo 1 ano de antecedência.

### `PUT /api/partidas/{id}` — só quem criou, antes de começar
Mesmo corpo do POST. O limite não pode ficar menor que o número de confirmados.

### `DELETE /api/partidas/{id}` — só quem criou
Cancela (**204**). A partida continua no banco com status `cancelada`.

### `POST /api/partidas/{id}/participacao` — logado
Entra na partida. **409** se já está, **422** se lotada/cancelada/já começou.

### `DELETE /api/partidas/{id}/participacao` — logado
Sai da partida. Quem criou não pode sair (deve cancelar).

### `GET /api/partidas/{id}/participantes` — logado
```json
[ { "id": "c3062661-...", "nome": "Ana Souza", "nivelHabilidade": 3, "criador": true }, ... ]
```

---

## Mudanças em relação à versão anterior (até 19/10)
- `eventos` virou **`partidas`** (`/api/eventos` não existe mais). Partida não tem título nem descrição:
  ela é ligada a uma **quadra**, que pertence a um **local** e tem um **esporte**.
- Ids agora são **UUID** (texto). Esporte continua numérico.
- Cadastro sem `aceiteTermos`, `dataNascimento` e `cidade`. Nível de habilidade virou número de 1 a 5.
- Não existe mais área de admin nem perfil `ADMIN`/`USER`.
- Sem esportes favoritos: as sugestões usam proximidade (latitude/longitude enviadas pelo app) e o filtro `esporteId`.
