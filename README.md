# 🔒 Módulo Cibersegurança & Qualidade — Sportfy

**Responsável Técnico**: Fabiano[cite: 5]  
**Tecnologias**: OWASP ZAP, JWT, Bcrypt, LGPD Compliance, Helmet.js[cite: 3, 5]

---

## 🎯 Objetivo do Módulo

Assegurar a proteção dos dados sensíveis dos usuários, garantir a autenticação segura no app e realizar a análise contínua de vulnerabilidades para aplicação do conceito *Security by Design*[cite: 3, 5].

---

## 🛡️ Diretrizes e Implementações de Segurança

* **Criptografia de Senhas**: Utilização do algoritmo `bcrypt` com salt para armazenamento seguro de credenciais no banco de dados[cite: 3, 5].
* **Autenticação Stateless**: Emissão de Tokens `JWT` com tempo de expiração curto para validação de rotas protegidas[cite: 3, 5].
* **Proteção contra OWASP Top 10**: Prevenção contra SQL Injection, XSS e sanitização rigorosa de entradas de formulário na API[cite: 3, 5].
* **Conformidade LGPD**: Tratamento adequado e transparente dos dados cadastrais e de localização dos usuários[cite: 3].
* **Análise de Vulnerabilidades**: Execução de testes de estresse, varredura de falhas e conteinerização isolada com Docker[cite: 3, 5].