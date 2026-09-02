# 🗄️ Módulo Banco de Dados & Nuvem — Sportfy

**Responsável Técnico**: Enzo[cite: 5]  
**Tecnologias**: PostgreSQL / MySQL, Docker, Docker Compose, Serviços de Nuvem[cite: 3, 5]

---

## 🎯 Objetivo do Módulo

Garantir a modelagem relacional, persistência, integridade e alta disponibilidade dos dados do aplicativo Sportfy, além de orquestrar os ambientes de execução através de containers Docker e hospedagem em nuvem[cite: 3, 5].

---

## 📊 Estrutura Relacional (Principais Tabelas)

* **`usuarios`**: `id`, `nome`, `email`, `senha_hash`, `bio`, `localizacao`, `nota_avaliacao`[cite: 3, 5].
* **`esportes`**: `id`, `nome` (Futebol, Vôlei, Basquete, etc.), `icone`[cite: 3].
* **`eventos`**: `id`, `titulo`, `descricao`, `data_hora`, `localizacao`, `criador_id`, `esporte_id`[cite: 3, 5].
* **`participantes_evento`**: `evento_id`, `usuario_id`, `status_confirmacao`[cite: 3, 5].

---

## 🛠️ Como Executar Este Módulo Localmente

```bash
# 1. Acesse o diretório do banco de dados
cd database

# 2. Suba o container do banco de dados via Docker Compose
docker-compose up -d database

# 3. Executar as migrations / scripts de população inicial
npm run migrate # ou docker exec -i <container_id> psql -U usuario -d sportfy < init.sql