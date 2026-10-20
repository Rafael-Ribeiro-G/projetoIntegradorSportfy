-- =============================================================
-- Estrutura do banco do Sportfy (schema public), feita pelo Enzo.
-- CÓPIA DE REFERÊNCIA tirada do servidor em 19/10/2026 com:
--   pg_dump -d sportfy -n public --schema-only --no-owner --no-privileges
-- A fonte oficial é o banco do servidor; a API não cria nem altera tabelas.
--
-- Usada para subir o banco LOCAL (docker-compose.yml) igual ao do servidor.
-- Ajustes em relação ao dump: removidos \restrict/\unrestrict, "SET transaction_timeout"
-- (só existem no PostgreSQL 17+) e o CREATE SCHEMA public (já existe em banco novo).
-- =============================================================
--
-- PostgreSQL database dump
--

-- Dumped from database version 18.6 (Ubuntu 18.6-0ubuntu0.26.04.1)
-- Dumped by pg_dump version 18.6 (Ubuntu 18.6-0ubuntu0.26.04.1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--

--

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: avaliacoes_quadras; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.avaliacoes_quadras (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    avaliador_id uuid NOT NULL,
    quadra_id uuid NOT NULL,
    nota integer NOT NULL,
    comentario text,
    criado_em timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT avaliacoes_quadras_nota_check CHECK (((nota >= 1) AND (nota <= 5)))
);

--
-- Name: avaliacoes_usuarios; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.avaliacoes_usuarios (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    avaliador_id uuid NOT NULL,
    avaliado_id uuid NOT NULL,
    partida_id uuid,
    nota integer NOT NULL,
    comentario text,
    criado_em timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT avaliacoes_usuarios_check CHECK ((avaliador_id <> avaliado_id)),
    CONSTRAINT avaliacoes_usuarios_nota_check CHECK (((nota >= 1) AND (nota <= 5)))
);

--
-- Name: campeonato_equipes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.campeonato_equipes (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    campeonato_id uuid NOT NULL,
    nome_equipe character varying(255) NOT NULL,
    capitao_id uuid NOT NULL
);

--
-- Name: campeonatos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.campeonatos (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    nome character varying(255) NOT NULL,
    esporte_id integer NOT NULL,
    organizador_id uuid NOT NULL,
    data_inicio date NOT NULL,
    data_fim date NOT NULL,
    regras text,
    CONSTRAINT campeonatos_check CHECK ((data_fim >= data_inicio))
);

--
-- Name: esportes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.esportes (
    id integer NOT NULL,
    nome character varying(100) NOT NULL,
    quantidade_jogadores_padrao integer
);

--
-- Name: esportes_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.esportes_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: esportes_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.esportes_id_seq OWNED BY public.esportes.id;

--
-- Name: locais; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.locais (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    nome character varying(255) NOT NULL,
    endereco text NOT NULL,
    coordenadas point,
    dono_id uuid
);

--
-- Name: partida_participantes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.partida_participantes (
    partida_id uuid NOT NULL,
    usuario_id uuid NOT NULL,
    status_confirmacao character varying(50) DEFAULT 'pendente'::character varying
);

--
-- Name: partidas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.partidas (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    criador_id uuid NOT NULL,
    quadra_id uuid NOT NULL,
    data_hora_inicio timestamp with time zone NOT NULL,
    data_hora_fim timestamp with time zone NOT NULL,
    max_jogadores integer,
    status character varying(50) DEFAULT 'aberta'::character varying,
    CONSTRAINT partidas_check CHECK ((data_hora_fim > data_hora_inicio))
);

--
-- Name: quadras; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.quadras (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    local_id uuid NOT NULL,
    esporte_id integer NOT NULL,
    nome character varying(255) NOT NULL,
    preco_hora numeric(10,2)
);

--
-- Name: usuarios; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.usuarios (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    nome character varying(255) NOT NULL,
    email character varying(255) NOT NULL,
    senha_hash character varying(255) NOT NULL,
    telefone character varying(20),
    nivel_habilidade integer,
    criado_em timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT usuarios_nivel_habilidade_check CHECK (((nivel_habilidade >= 1) AND (nivel_habilidade <= 5)))
);

--
-- Name: esportes id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.esportes ALTER COLUMN id SET DEFAULT nextval('public.esportes_id_seq'::regclass);

--
-- Name: avaliacoes_quadras avaliacoes_quadras_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.avaliacoes_quadras
    ADD CONSTRAINT avaliacoes_quadras_pkey PRIMARY KEY (id);

--
-- Name: avaliacoes_usuarios avaliacoes_usuarios_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.avaliacoes_usuarios
    ADD CONSTRAINT avaliacoes_usuarios_pkey PRIMARY KEY (id);

--
-- Name: campeonato_equipes campeonato_equipes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.campeonato_equipes
    ADD CONSTRAINT campeonato_equipes_pkey PRIMARY KEY (id);

--
-- Name: campeonatos campeonatos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.campeonatos
    ADD CONSTRAINT campeonatos_pkey PRIMARY KEY (id);

--
-- Name: esportes esportes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.esportes
    ADD CONSTRAINT esportes_pkey PRIMARY KEY (id);

--
-- Name: locais locais_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.locais
    ADD CONSTRAINT locais_pkey PRIMARY KEY (id);

--
-- Name: partida_participantes partida_participantes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.partida_participantes
    ADD CONSTRAINT partida_participantes_pkey PRIMARY KEY (partida_id, usuario_id);

--
-- Name: partidas partidas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.partidas
    ADD CONSTRAINT partidas_pkey PRIMARY KEY (id);

--
-- Name: quadras quadras_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quadras
    ADD CONSTRAINT quadras_pkey PRIMARY KEY (id);

--
-- Name: usuarios usuarios_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT usuarios_email_key UNIQUE (email);

--
-- Name: usuarios usuarios_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT usuarios_pkey PRIMARY KEY (id);

--
-- Name: avaliacoes_quadras avaliacoes_quadras_avaliador_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.avaliacoes_quadras
    ADD CONSTRAINT avaliacoes_quadras_avaliador_id_fkey FOREIGN KEY (avaliador_id) REFERENCES public.usuarios(id) ON DELETE CASCADE;

--
-- Name: avaliacoes_quadras avaliacoes_quadras_quadra_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.avaliacoes_quadras
    ADD CONSTRAINT avaliacoes_quadras_quadra_id_fkey FOREIGN KEY (quadra_id) REFERENCES public.quadras(id) ON DELETE CASCADE;

--
-- Name: avaliacoes_usuarios avaliacoes_usuarios_avaliado_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.avaliacoes_usuarios
    ADD CONSTRAINT avaliacoes_usuarios_avaliado_id_fkey FOREIGN KEY (avaliado_id) REFERENCES public.usuarios(id) ON DELETE CASCADE;

--
-- Name: avaliacoes_usuarios avaliacoes_usuarios_avaliador_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.avaliacoes_usuarios
    ADD CONSTRAINT avaliacoes_usuarios_avaliador_id_fkey FOREIGN KEY (avaliador_id) REFERENCES public.usuarios(id) ON DELETE CASCADE;

--
-- Name: avaliacoes_usuarios avaliacoes_usuarios_partida_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.avaliacoes_usuarios
    ADD CONSTRAINT avaliacoes_usuarios_partida_id_fkey FOREIGN KEY (partida_id) REFERENCES public.partidas(id) ON DELETE SET NULL;

--
-- Name: campeonato_equipes campeonato_equipes_campeonato_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.campeonato_equipes
    ADD CONSTRAINT campeonato_equipes_campeonato_id_fkey FOREIGN KEY (campeonato_id) REFERENCES public.campeonatos(id) ON DELETE CASCADE;

--
-- Name: campeonato_equipes campeonato_equipes_capitao_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.campeonato_equipes
    ADD CONSTRAINT campeonato_equipes_capitao_id_fkey FOREIGN KEY (capitao_id) REFERENCES public.usuarios(id);

--
-- Name: campeonatos campeonatos_esporte_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.campeonatos
    ADD CONSTRAINT campeonatos_esporte_id_fkey FOREIGN KEY (esporte_id) REFERENCES public.esportes(id);

--
-- Name: campeonatos campeonatos_organizador_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.campeonatos
    ADD CONSTRAINT campeonatos_organizador_id_fkey FOREIGN KEY (organizador_id) REFERENCES public.usuarios(id);

--
-- Name: locais locais_dono_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.locais
    ADD CONSTRAINT locais_dono_id_fkey FOREIGN KEY (dono_id) REFERENCES public.usuarios(id) ON DELETE SET NULL;

--
-- Name: partida_participantes partida_participantes_partida_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.partida_participantes
    ADD CONSTRAINT partida_participantes_partida_id_fkey FOREIGN KEY (partida_id) REFERENCES public.partidas(id) ON DELETE CASCADE;

--
-- Name: partida_participantes partida_participantes_usuario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.partida_participantes
    ADD CONSTRAINT partida_participantes_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id) ON DELETE CASCADE;

--
-- Name: partidas partidas_criador_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.partidas
    ADD CONSTRAINT partidas_criador_id_fkey FOREIGN KEY (criador_id) REFERENCES public.usuarios(id) ON DELETE CASCADE;

--
-- Name: partidas partidas_quadra_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.partidas
    ADD CONSTRAINT partidas_quadra_id_fkey FOREIGN KEY (quadra_id) REFERENCES public.quadras(id) ON DELETE RESTRICT;

--
-- Name: quadras quadras_esporte_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quadras
    ADD CONSTRAINT quadras_esporte_id_fkey FOREIGN KEY (esporte_id) REFERENCES public.esportes(id) ON DELETE RESTRICT;

--
-- Name: quadras quadras_local_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quadras
    ADD CONSTRAINT quadras_local_id_fkey FOREIGN KEY (local_id) REFERENCES public.locais(id) ON DELETE CASCADE;

--
-- PostgreSQL database dump complete
--

