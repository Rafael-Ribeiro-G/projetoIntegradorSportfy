# ⚙️ Módulo Backend & API REST — Sportfy

**Responsável Técnico**: Danilo[cite: 5]  
**Tecnologias**: Node.js / Python (FastAPI / Express), JWT, REST API, Docker[cite: 3, 5]

---

## 🎯 Objetivo do Módulo

Servir como a camada central de inteligência e regra de negócio do Sportfy, disponibilizando endpoints seguros para que a aplicação mobile possa cadastrar usuários, autenticar sessões, criar e listar partidas esportivas[cite: 3, 5].

---

## 🚀 Endpoints Principais

### 🔑 Autenticação e Usuários
* **`POST /api/auth/register`**: Cadastro de novos usuários[cite: 5].
* **`POST /api/auth/login`**: Autenticação e geração de Token JWT[cite: 3, 5].
* **`GET /api/users/profile`**: Consulta e edição de dados do perfil (bio, localização e esportes favoritos)[cite: 3, 5].

### ⚽ Gerenciamento de Eventos
* **`GET /api/events`**: Listagem e filtragem de eventos esportivos na região[cite: 3, 5].
* **`POST /api/events`**: Criação de novo evento esportivo (upload de imagem, data, local e esporte)[cite: 3, 5].
* **`GET /api/events/:id`**: Detalhes de um evento específico e lista de participantes confirmados[cite: 3, 5].

---

## 🛠️ Como Executar Este Módulo Localmente

```bash
# 1. Acesse o diretório do backend
cd backend

# 2. Instale as dependências
npm install

# 3. Configure as variáveis de ambiente
cp .env.example .env

# 4. Execute a API em modo de desenvolvimento
npm run dev