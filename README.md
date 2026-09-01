# Projeto Integrador Sportfy

# 🏆 Sportfy — Pratique & Conecte & Divirta-se

> **Projeto Integrador - 4º Semestre**  
> *Análise e Desenvolvimento de Sistemas*

----

## 📌 Sobre o Projeto

O **Sportfy** é uma plataforma *mobile* desenvolvida para solucionar um problema comum enfrentado por atletas amadores e praticantes de atividades físicas: **a dificuldade de encontrar pessoas disponíveis para organizar e praticar esportes coletivos ou individuais**.

Seja para completar a linha do futebol no final de semana, encontrar uma dupla para o vôlei de praia ou reunir uma turma para o basquete, o aplicativo conecta usuários com base em preferências esportivas e geolocalização.

---

## 📱 Principais Telas e Funcionalidades

- **Home / Descobrir**: Apresentação da plataforma e atalhos para exploração rápida de partidas.
- **Ver Eventos**: Feed interativo com sugestões e partidas criadas na região.
- **Criar Evento**: Formulação de novas partidas com upload de fotos, descrição, esporte, data e local.
- **Login e Criar Conta**: Sistema de autenticação seguro para gerenciamento de perfis.
- **Conta / Perfil do Usuário**: Exibição da foto de perfil, avaliação (estrelas), bio, localização e esportes favoritos.
- **Chatbot de Suporte**: Assistente virtual integrado para tirar dúvidas e sugerir eventos de acordo com o perfil do usuário.

---

## 🛠️ Tecnologias e Arquitetura

O projeto foi concebido seguindo as melhores práticas de engenharia de software e arquitetura em camadas:

- **Frontend Mobile**: React Native / Flutter
- **Backend & API REST**: Node.js / Python / Java
- **Banco de Dados**: PostgreSQL / MySQL (Relacional)
- **Infraestrutura**: Docker & Deploy em Nuvem
- **Inteligência Artificial**: Algoritmo de recomendação de partidas e Chatbot interativo
- **Cibersegurança**: Autenticação via JWT, criptografia de dados e análise contínua de vulnerabilidades

---

## 👥 Estrutura do Time & Responsabilidades (EAP)

A organização das frentes de trabalho segue a Estrutura Analítica do Projeto (EAP):

| Integrante | Área / Módulo | Principais Entregas |
| :--- | :--- | :--- |
| **Rafa** | Gestão de Projetos | Documentação, Cronograma, TAP e Organização do Time |
| **Fabiano** | Cibersegurança | Diretrizes de Segurança, Proteção de Dados e Análise de Falhas |
| **Danilo** | Back-End | APIs REST de Autenticação, Cadastro de Usuários e Eventos |
| **Enzo** | Banco de Dados & Nuvem | Modelagem de Tabelas, Consultas SQL e Deploy em Nuvem |
| **Sábio & Gabriel** | Front-End Mobile | Identidade Visual e Desenvolvimento das Telas (Home, Eventos, Perfil) |
| **Guilherme** | IA & Chatbot | Algoritmo de Recomendação e Chat de Suporte |

---

## 🚀 Como Executar o Projeto

### Pré-requisitos
- Node.js (v18+) ou ambiente da linguagem escolhida
- Docker e Docker Compose
- Git

### Passos para Instalação

```bash
# 1. Clone o repositório
git clone [https://github.com/seu-usuario/sportfy.git](https://github.com/seu-usuario/sportfy.git)

# 2. Acesse o diretório do projeto
cd sportfy

# 3. Suba os containers da infraestrutura (Banco de Dados e API)
docker-compose up -d

# 4. Instale as dependências do app mobile
cd mobile
npm install

# 5. Execute o aplicativo
npx react-native run-android # ou flutter run
