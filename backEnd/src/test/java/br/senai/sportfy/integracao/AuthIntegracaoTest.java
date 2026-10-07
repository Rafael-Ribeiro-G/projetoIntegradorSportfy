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

/** Cadastro, login, proteção de rotas, permissões e LGPD. */
class AuthIntegracaoTest extends IntegracaoBase {

    @Test
    void registro_devolveTokenQueDaAcessoAoPerfil() throws Exception {
        JsonNode registro = registrar("Ana");
        String email = registro.get("usuario").get("email").asText();

        mvc.perform(get("/api/usuarios/me").header("Authorization", bearer(tokenDe(registro))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.perfil").value("USER"))
                .andExpect(jsonPath("$.consentimentoLgpdEm", notNullValue()))
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
        body.put("aceiteTermos", false);

        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo", hasItem("email")))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("senha")))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("aceiteTermos")));
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
    void usuarioComum_naoAcessaRotasDeAdmin() throws Exception {
        String token = tokenDe(registrar("Edu"));
        mvc.perform(get("/api/admin/usuarios").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        mvc.perform(post("/api/esportes").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Xadrez\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_criadoNaSubida_acessaRotasDeAdmin() throws Exception {
        String token = login(ADMIN_EMAIL, ADMIN_SENHA);
        mvc.perform(get("/api/admin/usuarios").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo", notNullValue()));
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
        long idOutro = outro.get("usuario").get("id").asLong();

        mvc.perform(get("/api/usuarios/" + idOutro).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Fabi"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.telefone").doesNotExist());
    }

    @Test
    void atualizarPerfil_salvaEsportesFavoritos() throws Exception {
        String token = tokenDe(registrar("Hugo"));
        ObjectNode body = json.createObjectNode();
        body.put("nome", "Hugo Silva");
        body.put("cidade", "Santo André");
        body.put("nivel", "INTERMEDIARIO");
        body.put("latitude", -23.66);
        body.put("longitude", -46.53);
        body.putArray("esportesFavoritosIds").add(1).add(4);

        mvc.perform(put("/api/usuarios/me").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Hugo Silva"))
                .andExpect(jsonPath("$.esportesFavoritos.length()").value(2));
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
    void excluirConta_anonimizaEImpedeNovoLogin() throws Exception {
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
