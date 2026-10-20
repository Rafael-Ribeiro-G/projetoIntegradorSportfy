package br.senai.sportfy.integracao;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Cadastro, login, proteção de rotas, perfil e LGPD. */
class AuthIntegracaoTest extends IntegracaoBase {

    @Test
    void registro_devolveTokenQueDaAcessoAoPerfil() throws Exception {
        JsonNode registro = registrar("Ana");
        String email = registro.get("usuario").get("email").asText();

        mvc.perform(get("/api/usuarios/me").header("Authorization", bearer(tokenDe(registro))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.nivelHabilidade").value(3))
                .andExpect(jsonPath("$.criadoEm", notNullValue()))
                // a senha (nem o hash) nunca sai na resposta
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
    }

    @Test
    void login_comEmailEmMaiusculas_funciona() throws Exception {
        JsonNode registro = registrar("Bia");
        String email = registro.get("usuario").get("email").asText();
        login(email.toUpperCase(), SENHA_PADRAO);
    }

    @Test
    void registro_emailDuplicado_devolve409() throws Exception {
        String email = "dup-" + unico() + "@teste.com";
        ObjectNode body = novoRegistro("Carlos", email);
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void registro_dadosInvalidos_devolve400ComOsCampos() throws Exception {
        ObjectNode body = json.createObjectNode();
        body.put("nome", "");
        body.put("email", "nao-e-email");
        body.put("senha", "123");
        body.put("nivelHabilidade", 9);

        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("email")))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("senha")))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("nivelHabilidade")));
    }

    @Test
    void registro_semAceiteDeTermos_funciona() throws Exception {
        // o cadastro não pede mais aceite de termos: só nome, e-mail e senha são obrigatórios
        ObjectNode body = json.createObjectNode();
        body.put("nome", "Sem Termos");
        body.put("email", "sem-termos-" + unico() + "@teste.com");
        body.put("senha", SENHA_PADRAO);
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario.nivelHabilidade").doesNotExist());
    }

    @Test
    void registro_comHtmlNoNome_ehBloqueado() throws Exception {
        ObjectNode body = novoRegistro("<script>alert(1)</script>", "xss-" + unico() + "@teste.com");
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("nome"));
    }

    @Test
    void jsonMalformado_devolve400() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{ isso nao e json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void rotaProtegida_semToken_devolve401EmJson() throws Exception {
        mvc.perform(get("/api/usuarios/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void rotaProtegida_comTokenInvalido_devolve401() throws Exception {
        mvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer token.falso.qualquer"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_senhaErrada_bloqueiaAposTresTentativas() throws Exception {
        JsonNode registro = registrar("Duda");
        String email = registro.get("usuario").get("email").asText();
        ObjectNode body = json.createObjectNode();
        body.put("email", email);
        body.put("senha", "SenhaErrada1");

        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(body)))
                    .andExpect(status().isUnauthorized());
        }
        // mesmo com a senha certa, fica bloqueado
        body.put("senha", SENHA_PADRAO);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void rotasDeAdminNaoExistemMais() throws Exception {
        String token = tokenDe(registrar("Edu"));
        mvc.perform(get("/api/admin/usuarios").header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void esportes_listagemPublica() throws Exception {
        mvc.perform(get("/api/esportes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nome", hasItem("Futebol")));
    }

    @Test
    void perfilPublico_naoExpoeEmailNemTelefone() throws Exception {
        JsonNode outro = registrar("Fabi");
        String token = tokenDe(registrar("Gabi"));
        String idOutro = outro.get("usuario").get("id").asText();

        mvc.perform(get("/api/usuarios/" + idOutro).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Fabi"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.telefone").doesNotExist());
    }

    @Test
    void atualizarPerfil_salvaNomeTelefoneENivel() throws Exception {
        String token = tokenDe(registrar("Hugo"));
        ObjectNode body = json.createObjectNode();
        body.put("nome", "Hugo Silva");
        body.put("telefone", "(11) 98888-7777");
        body.put("nivelHabilidade", 5);

        mvc.perform(put("/api/usuarios/me").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Hugo Silva"))
                .andExpect(jsonPath("$.telefone").value("(11) 98888-7777"))
                .andExpect(jsonPath("$.nivelHabilidade").value(5));
        mvc.perform(get("/api/usuarios/me").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.nivelHabilidade").value(5));
    }

    @Test
    void trocarSenha_devolveTokenNovoEInvalidaOAntigo() throws Exception {
        String tokenAntigo = tokenDe(registrar("Joca"));
        ObjectNode body = json.createObjectNode();
        body.put("senhaAtual", SENHA_PADRAO);
        body.put("novaSenha", "NovaSenha999");

        String resposta = mvc.perform(put("/api/usuarios/me/senha").header("Authorization", bearer(tokenAntigo))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String tokenNovo = json.readTree(resposta).get("token").asText();

        mvc.perform(get("/api/usuarios/me").header("Authorization", bearer(tokenAntigo)))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/usuarios/me").header("Authorization", bearer(tokenNovo)))
                .andExpect(status().isOk());
    }

    @Test
    void trocarSenha_comSenhaAtualErrada_devolve401() throws Exception {
        String token = tokenDe(registrar("Kiko"));
        ObjectNode body = json.createObjectNode();
        body.put("senhaAtual", "Errada12345");
        body.put("novaSenha", "NovaSenha999");

        mvc.perform(put("/api/usuarios/me/senha").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void excluirConta_apagaEImpedeNovoLogin() throws Exception {
        JsonNode registro = registrar("Iara");
        String email = registro.get("usuario").get("email").asText();
        String token = tokenDe(registro);

        ObjectNode body = json.createObjectNode();
        body.put("senha", SENHA_PADRAO);
        mvc.perform(post("/api/usuarios/me/exclusao").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isNoContent());

        // token antigo deixa de funcionar
        mvc.perform(get("/api/usuarios/me").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());

        ObjectNode loginBody = json.createObjectNode();
        loginBody.put("email", email);
        loginBody.put("senha", SENHA_PADRAO);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(loginBody)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void respostas_incluemHeadersDeSeguranca() throws Exception {
        mvc.perform(get("/api/esportes"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Content-Security-Policy", not("")));
    }
}
