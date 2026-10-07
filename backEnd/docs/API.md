# Sportfy API — guia para Front, Mobile e Chatbot

Base: `http://<servidor>:8080` · Documentação interativa: `/swagger-ui.html`

- Corpo das requisições e respostas em **JSON** (`Content-Type: application/json`).
- Rotas protegidas exigem o header **`Authorization: Bearer <token>`**.
- Datas no formato ISO **sem fuso**, horário de Brasília: `2026-10-20T19:30:00`.
- Campos `null` não aparecem na resposta.

## Erros (formato único)
```json
{
  "timestamp": "2026-10-07T12:00:00",
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "Dados inválidos. Verifique os campos.",
  "caminho": "/api/auth/registro",
  "campos": [ { "campo": "email", "mensagem": "deve ser um e-mail válido" } ]
}
```
| Status | Quando | O que o front faz |
|---|---|---|
| 400 | Campo inválido (`campos` diz qual) | Mostrar a mensagem embaixo do campo |
| 401 | Sem token, token vencido ou senha errada | Mandar para a tela de Login |
| 403 | Sem permissão (ex.: editar evento de outra pessoa) | Mostrar `mensagem` |
| 404 | Não existe | Mostrar `mensagem` |
| 409 | Conflito (e-mail já cadastrado, já participa) | Mostrar `mensagem` |
| 422 | Regra de negócio (evento lotado, cancelado, já começou) | Mostrar `mensagem` |
| 429 | Muitas tentativas de login | Pedir para esperar |

---

## Autenticação (telas Login e Criar Conta)

### `POST /api/auth/registro` — público
```json
{
  "nome": "Ana Souza",
  "email": "ana@email.com",
  "senha": "Senha1234",
  "telefone": "(11) 98888-7777",
  "dataNascimento": "2002-05-10",
  "cidade": "São Paulo",
  "aceiteTermos": true
}
```
Obrigatórios: `nome`, `email`, `senha` (8+ caracteres, letra e número), `aceiteTermos: true`.
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
  "usuario": { "id": 7, "nome": "Ana Souza", "email": "ana@email.com", "perfil": "USER", "esportesFavoritos": [] }
}
```
Guarde o `token` (web: memória/cookie seguro; mobile: armazenamento seguro). Não há rota de logout: basta apagar o token.

---

## Esportes

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| GET | `/api/esportes` | público | Lista para filtros e formulário: `[{"id":1,"nome":"Futebol",...}]` |
| GET | `/api/esportes/{id}` | público | Detalhe |
| POST/PUT/DELETE | `/api/esportes[/{id}]` | ADMIN | Manutenção |

---

## Eventos (telas Home, Ver Eventos, Criar Evento)

### `GET /api/eventos` — público (se mandar token, vem `usuarioParticipa`)
Filtros opcionais (query string):

| Parâmetro | Exemplo | |
|---|---|---|
| `esporteId` | `1` | |
| `cidade` | `São Paulo` | contém, sem diferenciar maiúsculas |
| `texto` | `pelada` | busca em título, descrição e local |
| `nivel` | `INICIANTE` | `TODOS`, `INICIANTE`, `INTERMEDIARIO`, `AVANCADO` |
| `dataInicio` / `dataFim` | `2026-10-20T00:00:00` | |
| `latitude` + `longitude` + `raioKm` | `-23.55`, `-46.63`, `10` | proximidade (raio máx. 500) |
| `pagina` / `tamanho` | `0` / `20` | tamanho máx. 50 |

Por padrão só traz eventos **futuros e ativos**, ordenados pela data.
Resposta:
```json
{
  "conteudo": [
    {
      "id": 15,
      "titulo": "Pelada de quarta",
      "descricao": "Traga água",
      "esporte": { "id": 1, "nome": "Futebol" },
      "organizador": { "id": 7, "nome": "Ana Souza" },
      "dataHoraInicio": "2026-10-21T19:30:00",
      "dataHoraFim": "2026-10-21T21:00:00",
      "duracaoMinutos": 90,
      "localNome": "Quadra do Parque",
      "endereco": "Rua A, 100",
      "cidade": "São Paulo",
      "latitude": -23.56,
      "longitude": -46.65,
      "maxParticipantes": 14,
      "participantesConfirmados": 6,
      "vagasRestantes": 8,
      "nivel": "TODOS",
      "situacao": "ABERTO",
      "distanciaKm": 2.4,
      "usuarioParticipa": false,
      "criadoEm": "2026-10-07T12:00:00"
    }
  ],
  "pagina": 0, "tamanho": 20, "totalElementos": 1, "totalPaginas": 1, "ultima": true
}
```
`situacao`: `ABERTO`, `LOTADO`, `EM_ANDAMENTO`, `ENCERRADO`, `CANCELADO`.

### Demais rotas de eventos
| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| GET | `/api/eventos/{id}` | público | Detalhe (mesmo formato de um item acima) |
| GET | `/api/eventos/sugestoes?limite=5` | logado | Sugestões: esportes favoritos + mais perto do usuário |
| POST | `/api/eventos` | logado | Cria evento (**201**). Criador vira organizador e ocupa 1 vaga |
| PUT | `/api/eventos/{id}` | organizador/ADMIN | Edita (antes de começar) |
| DELETE | `/api/eventos/{id}` | organizador/ADMIN | Cancela (**204**) |
| POST | `/api/eventos/{id}/participacao` | logado | Entrar no evento → evento atualizado |
| DELETE | `/api/eventos/{id}/participacao` | logado | Sair do evento → evento atualizado |
| GET | `/api/eventos/{id}/participantes` | logado | `[{"usuarioId":7,"nome":"Ana","nivel":"INICIANTE","organizador":true,"confirmadoEm":"..."}]` |

Corpo para criar/editar:
```json
{
  "titulo": "Pelada de quarta",
  "descricao": "Traga água",
  "esporteId": 1,
  "dataHoraInicio": "2026-10-21T19:30:00",
  "duracaoMinutos": 90,
  "localNome": "Quadra do Parque",
  "endereco": "Rua A, 100",
  "cidade": "São Paulo",
  "latitude": -23.56,
  "longitude": -46.65,
  "maxParticipantes": 14,
  "nivel": "TODOS"
}
```
Obrigatórios: `titulo`, `esporteId`, `dataHoraInicio` (futura), `duracaoMinutos` (15–720), `localNome`, `cidade`, `maxParticipantes` (2–100).
`latitude`/`longitude` são opcionais, mas vão **juntas**.

---

## Conta / Perfil

| Método | Rota | Descrição |
|---|---|---|
| GET | `/api/usuarios/me` | Meus dados |
| PUT | `/api/usuarios/me` | Atualiza perfil (ver corpo abaixo) |
| PUT | `/api/usuarios/me/senha` | `{"senhaAtual":"...","novaSenha":"..."}` → **200** com token novo (o antigo deixa de valer) |
| GET | `/api/usuarios/me/eventos` | `{"organizados":[...],"participando":[...]}` |
| GET | `/api/usuarios/me/dados` | LGPD: exporta todos os meus dados |
| POST | `/api/usuarios/me/exclusao` | LGPD: `{"senha":"..."}` → **204**, conta anonimizada |
| GET | `/api/usuarios/{id}` | Perfil público de outra pessoa (sem e-mail/telefone) |

Corpo do `PUT /api/usuarios/me` (substitui os dados; só `nome` é obrigatório):
```json
{
  "nome": "Ana Souza",
  "telefone": "(11) 98888-7777",
  "dataNascimento": "2002-05-10",
  "cidade": "São Paulo",
  "bio": "Jogo vôlei aos sábados",
  "nivel": "INTERMEDIARIO",
  "latitude": -23.55,
  "longitude": -46.63,
  "esportesFavoritosIds": [1, 4]
}
```
Dica: o app mobile pode mandar a localização do GPS aqui para as sugestões ficarem por proximidade.

---

## Chatbot (Python)
O chatbot consome a mesma API repassando o token do usuário que está conversando:
```python
import requests
API = "http://<servidor>:8080"
headers = {"Authorization": f"Bearer {token_do_usuario}"}

sugestoes = requests.get(f"{API}/api/eventos/sugestoes", params={"limite": 3}, headers=headers).json()
volei_sp = requests.get(f"{API}/api/eventos", params={"esporteId": 4, "cidade": "São Paulo"}).json()["conteudo"]
esportes = requests.get(f"{API}/api/esportes").json()
```

## Administração (perfil ADMIN)
| Método | Rota | Descrição |
|---|---|---|
| GET | `/api/admin/usuarios?busca=ana` | Lista paginada |
| PATCH | `/api/admin/usuarios/{id}/status` | `{"ativo": false}` bloqueia o usuário |
| PATCH | `/api/admin/usuarios/{id}/perfil` | `{"perfil": "ADMIN"}` |
| GET | `/api/admin/esportes` | Todos os esportes, inclusive inativos |

## Saúde
`GET /actuator/health` → `{"status":"UP"}` (público, usado pelo Docker).
