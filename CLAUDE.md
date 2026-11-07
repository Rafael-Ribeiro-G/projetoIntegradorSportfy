# Sportfy Web — Instruções para o Claude Code

> Este arquivo fica na raiz do repositório do front (`projetoIntegradorSportfy-frontEnd/`).
> O Claude Code lê o `CLAUDE.md` automaticamente em toda sessão.

## 1. Contexto do projeto

- **Sportfy** ("Pratique, Conecte e Divirta-se") é o Projeto Integrador / TCC do 4º semestre de ADS (SENAI São Caetano do Sul).
- O app conecta pessoas para praticar esportes coletivos: elas criam eventos esportivos (peladas, corridas, treinos) e participam deles, filtrando por esporte, local e distância.
- **Esta aplicação web é a versão web do app mobile (Android/Java).** Ela tem as mesmas telas, os mesmos fluxos e a mesma API.
- Como o grupo já tem o aplicativo mobile, a web é um **site de verdade, responsivo**: pensado para o navegador do computador e que se adapta bem ao celular. **Não** é uma "interface de celular" dentro do navegador (nada de coluna estreita fixa nem barra de navegação inferior).
- Sou o responsável pelo frontend. Sou estudante: **explique o que está fazendo e por quê** em cada etapa, de forma curta e em português.

## 2. Stack obrigatória (a mesma das aulas de Aplicações Web, Prof. Lozano)

Não troque nem acrescente tecnologias por conta própria. A stack é:

| Item | Escolha |
|---|---|
| Framework | **Next.js 16.3.8** (App Router) |
| Linguagem | **TypeScript** |
| Estilo | **Tailwind CSS** (versão atual: sem `tailwind.config.js`; o tema fica em `src/app/globals.css` com `@theme`) |
| Linter | **ESLint** |
| Pasta do código | `src/` |
| Alias de import | `@/*` (padrão) |
| React Compiler | **desligado** |
| Gerenciador | **npm** |
| Editor | VS Code |

Regras:
- **Não instale bibliotecas extras** (axios, zustand, react-hook-form, shadcn, etc.) sem me perguntar antes. Prefira o que já vem com o React/Next: `fetch`, `useState`, `useEffect`, Context API, `next/navigation`, `next/link`, `next/image`.
- Não altere `next.config.ts`, `tsconfig.json`, `eslint.config.mjs` ou `postcss.config.mjs`, a não ser que seja necessário. Se for, avise e explique o motivo.
- Se uma aula futura apresentar um padrão novo (por exemplo, um jeito de fazer data fetching), esse padrão passa a valer. Vou atualizar este arquivo quando isso acontecer.

## 3. Criação do projeto (só na primeira vez)

Estrutura desejada, espelhando o repositório do back (`projetoIntegradorSportfy-backEnd/backEnd`):

```
projetoIntegradorSportfy-frontEnd/   ← raiz do repositório (único .git aqui)
├── CLAUDE.md
├── README.md
└── frontEnd/                        ← projeto Next.js
```

Comando (rodar dentro de `projetoIntegradorSportfy-frontEnd/`). O nome do pacote precisa ser minúsculo; depois disso, só a pasta é renomeada:

```bash
npx create-next-app@16.3.8 sportfyfront --use-npm --typescript --tailwind --eslint --app --src-dir --no-react-compiler --no-agents-md
```

- Se aparecer "Would you like to use the recommended Next.js defaults?", escolha **"No, customize settings"**.
- Renomeie a pasta `sportfyfront` para `frontEnd`. O `package.json` continua com `"name": "sportfyfront"`.
- Não pode existir `.git` dentro de `frontEnd/`. Se o create-next-app criar um, apague.
- Windows/PowerShell bloqueando o `npx`? Rode: `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`.
- Para rodar: `cd frontEnd && npm install && npm run dev`, depois abra http://localhost:3000.

## 4. API (backend Java/Spring do grupo)

- **Contrato completo:** `../projetoIntegradorSportfy-backEnd/backEnd/docs/API.md` (se o repo do back estiver ao lado). **Leia esse arquivo antes de implementar qualquer tela**, porque ele é a fonte da verdade. Swagger: `{API}/swagger-ui.html`.
- Base URL na variável de ambiente `frontEnd/.env.local` (não versionar). Crie também um `.env.example`:
  ```
  NEXT_PUBLIC_API_URL=http://100.68.85.13:8080
  ```
  (O IP só funciona com o **Tailscale ligado**. Para rodar o back local, use `http://localhost:8080`.)
- **CORS:** o back libera `http://localhost:3000` por padrão (`CORS_ORIGENS`). Se o front rodar em outra porta ou domínio, quem cuida do back precisa adicionar a origem.
- **Mixed content:** front em HTTPS (Vercel) chamando a API em HTTP é bloqueado pelo navegador. Para a apresentação, a API precisa estar em HTTPS.

### Regras do contrato que o front precisa respeitar
- JSON em tudo. Rotas protegidas usam o header `Authorization: Bearer <token>`.
- **Ids são UUID (string)**; só `esporte.id` é número.
- **Datas ISO sem fuso**, no horário de Brasília: `2026-10-25T19:00:00`.
  - Um `<input type="datetime-local">` devolve `2026-10-25T19:00`; basta acrescentar `:00`.
  - **Nunca use `toISOString()`**, porque ele converte para UTC e muda o horário.
  - Para exibir, formate com `Intl.DateTimeFormat('pt-BR')` a partir da string, sem converter fuso.
- Campos `null` **não vêm** na resposta. Tipar como opcionais (`campo?: tipo`).
- `/api/partidas` e `/api/eventos` são a mesma coisa. Use **`/api/partidas`** no código.
- Na tela, use o campo **`situacao`** (`ABERTA`, `LOTADA`, `EM_ANDAMENTO`, `ENCERRADA`, `CANCELADA`), e não `status`.

### Erros (formato único)
```json
{ "status": 400, "erro": "...", "mensagem": "...", "caminho": "...", "campos": [{ "campo": "email", "mensagem": "..." }] }
```
| Status | O que o front faz |
|---|---|
| 400 | Mostrar cada `campos[].mensagem` embaixo do campo correspondente |
| 401 | Limpar o token e mandar para `/login` |
| 403, 404, 409, 422 | Mostrar `mensagem` ao usuário (toast/alerta) |
| 429 | Login: "Muitas tentativas, aguarde 15 minutos". Chatbot: "Muitas perguntas, aguarde um pouco" |
| 502/503/504 (chatbot) | "SportIA indisponível no momento" |

## 5. Arquitetura do código

```
frontEnd/src/
├── app/
│   ├── layout.tsx              ← layout raiz (fonte, AuthProvider)
│   ├── globals.css             ← Tailwind + @theme com as cores do Sportfy
│   ├── login/page.tsx
│   ├── cadastro/page.tsx       ← "Criar Conta"
│   └── (logado)/               ← grupo de rotas protegidas (com cabeçalho de navegação no topo)
│       ├── layout.tsx          ← verifica login + BarraNavegacao + botão do SportIA
│       ├── page.tsx            ← Home  (rota "/")
│       ├── eventos/page.tsx            ← Ver Eventos
│       ├── eventos/[id]/page.tsx       ← Detalhe do evento
│       ├── eventos/novo/page.tsx       ← Criar Evento
│       ├── eventos/[id]/editar/page.tsx
│       ├── perfil/page.tsx             ← Conta/Perfil
│       └── perfil/editar/page.tsx
├── components/                 ← componentes reutilizáveis (Botao, CampoTexto, EventoCard, BarraNavegacao, ChatSportIA...)
├── contexts/AuthContext.tsx    ← token + usuário logado, login(), logout()
├── services/                   ← uma função por rota da API
│   ├── api.ts                  ← wrapper do fetch (base URL, token, tratamento de erro)
│   ├── auth.ts  usuarios.ts  esportes.ts  locais.ts  quadras.ts  partidas.ts  uploads.ts  chatbot.ts
├── types/                      ← interfaces TypeScript espelhando os DTOs do API.md
└── lib/                        ← utilitários (formatar data, montar query string, geolocalização)
```

Convenções:
- **Componentes e telas em português**, seguindo os nomes da API (`Partida`, `Local`, `Quadra`, `Esporte`, `Usuario`). Componentes em PascalCase.
- Telas que usam token, `localStorage`, geolocalização ou interação ficam como **Client Components** (`"use client"`).
- **Toda chamada à API passa por `services/`**. Nenhum `fetch` direto dentro de componente.
- `api.ts` deve:
  - pegar a base de `process.env.NEXT_PUBLIC_API_URL`;
  - incluir o `Authorization` quando houver token;
  - lançar um erro tipado (`ApiError` com `status`, `mensagem`, `campos`) quando `!res.ok`;
  - tratar respostas **204** sem corpo;
  - no 401, disparar o logout.
  - Para **upload** (`multipart/form-data`), **não defina o `Content-Type` à mão**; envie um `FormData` com o campo `arquivo`.
- **Autenticação:** guardar `token` e `usuario` no `localStorage` (como o API.md sugere para web), via AuthContext. O token vale 24 h (`expiraEmSegundos`). A proteção de rota é feita no `(logado)/layout.tsx`, no cliente: sem token, `router.replace('/login')`.
- **Imagens** vêm do servidor da API (`/uploads/...`) ou de URLs externas. Use `<img>` simples, ou configure `images.remotePatterns` no `next.config.ts` (avise antes de mexer).
- Estados de **carregando**, **vazio** e **erro** em toda tela que busca dados.
- Acessibilidade básica: `label` em todo input, `alt` em imagens, botões com texto claro.

## 6. Telas e rotas da API

| Tela | Rotas | Observações |
|---|---|---|
| **Login** | `POST /api/auth/login` | Salvar token + usuário; tratar 401 (senha errada) e 429 |
| **Criar Conta** | `GET /api/esportes`, `POST /api/auth/registro` | Obrigatórios: nome, e-mail, senha (8+ caracteres, com letra e número). Opcionais: telefone, `nivelHabilidade` (1 iniciante a 5 avançado), bio, foto, `esportesFavoritosIds`. Resposta 201 igual à do login, então já entra logado. Exibir aviso de privacidade (LGPD) |
| **Home** | `GET /api/partidas/sugestoes?latitude=&longitude=&limite=5`, `GET /api/usuarios/me/partidas` | Pedir a geolocalização do navegador (se negada, chamar sem lat/long). Mostrar sugestões e "meus próximos eventos" |
| **Ver Eventos** | `GET /api/partidas` com filtros (`esporteId`, `texto`, `tipoLocal`, `dataInicio`, `dataFim`, `latitude`/`longitude`/`raioKm`, `pagina`, `tamanho`) | Resposta paginada (`conteudo`, `pagina`, `ultima`...): fazer "carregar mais". Card com esporte, título, local, data, vagas (`vagasRestantes` pode não vir), `situacao` e `distanciaKm` |
| **Detalhe do evento** | `GET /api/partidas/{id}`, `GET /api/partidas/{id}/participantes`, `POST`/`DELETE /api/partidas/{id}/participacao`, `DELETE /api/partidas/{id}` | Botão Participar/Sair conforme `usuarioParticipa`. O criador não sai: ele cancela. Editar/Cancelar só para quem criou |
| **Criar / Editar Evento** | `GET /api/locais`, `POST /api/locais`, `GET /api/quadras?localId=`, `POST /api/uploads/imagens`, `POST`/`PUT /api/partidas` | Fluxo: escolher ou cadastrar o local → se for numa quadra, escolher a quadra (aí o esporte vem dela) → senão, escolher o esporte → título, descrição, capa, início/fim, `maxJogadores` (2 a 100, opcional). Validar duração de 15 min a 12 h. 409 = quadra ocupada no horário |
| **Conta / Perfil** | `GET`/`PUT /api/usuarios/me`, `PUT /api/usuarios/me/senha`, `GET /api/usuarios/me/partidas`, `GET /api/usuarios/me/dados`, `POST /api/usuarios/me/exclusao` | No PUT, campo `null` é apagado: **envie sempre todos os campos atuais**. A troca de senha devolve um **token novo** (substituir o antigo). LGPD: botão "Baixar meus dados" (salvar o JSON) e "Excluir conta" (pede a senha, confirmação dupla, depois logout). Também tem Sair |
| **Perfil público** | `GET /api/usuarios/{id}` | Abrir ao clicar num participante |
| **SportIA (chatbot)** | `POST /api/chatbot/mensagem` `{ pergunta }` → `{ resposta }` | Botão flutuante nas telas logadas, que abre um painel de chat. Pergunta de até 1000 caracteres. Timeout de cerca de 45 s (`AbortController`). Indicador "digitando..." |

Upload de imagem (perfil e capa): `POST /api/uploads/imagens` com JPG/PNG/WEBP de até 5 MB → usar a `url` retornada em `fotoUrl` ou `imagemUrl`. Validar tipo e tamanho no front antes de enviar.

## 7. Visual

- Seguir a identidade do app mobile do grupo. Se eu enviar prints ou o Figma, eles têm prioridade.
- Até lá: visual esportivo e limpo, com cor primária vibrante definida no `@theme` do `globals.css` (variáveis como `--color-primary`), cantos arredondados e cards.
- **Site responsivo** (Tailwind `sm:`/`md:`/`lg:`): o conteúdo ocupa a largura da tela até `max-w-6xl`, centralizado, com `px-4`. Listas de eventos em **grade de cards** (1 coluna no celular, 2–3 no computador). Formulários numa largura confortável (`max-w-xl`/`max-w-2xl`).
- **Cabeçalho de navegação no topo** (`BarraNavegacao`), fixo, com o logo e os links Home, Eventos, Criar evento e Perfil + Sair. No celular é o mesmo cabeçalho, com os links num menu ☰.
- Login e Criar Conta: painel da marca ao lado do formulário no computador; empilhados no celular.
- Logo em texto: **sportfy.** (minúsculo, com ponto). Slogan: "Pratique, Conecte e Divirta-se".

## 8. Forma de trabalhar

1. **Uma tela ou funcionalidade por vez**, nesta ordem:
   1. setup + `api.ts` + tipos
   2. AuthContext + Login + Criar Conta
   3. layout logado + BarraNavegacao
   4. Ver Eventos + Detalhe
   5. Criar Evento
   6. Home
   7. Perfil
   8. SportIA
2. Antes de codar uma tela, confira as rotas no `API.md` e mostre em poucas linhas o que vai fazer.
3. Depois de cada etapa, rode `npm run lint` e `npm run build` e corrija os erros. Teste no navegador em `npm run dev`.
4. **Commits pequenos, com mensagem em português** (ex.: `Cria tela de login integrada à API`). Não faça `push` sem eu pedir.
5. Nunca coloque segredos ou o `.env.local` no repositório.
6. Se o `API.md` e este arquivo divergirem, **o `API.md` vence**. Me avise da diferença.
