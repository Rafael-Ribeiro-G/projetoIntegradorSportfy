package br.senai.sportfy.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import br.senai.sportfy.config.SportfyProperties;
import br.senai.sportfy.exception.RecursoNaoEncontradoException;
import br.senai.sportfy.exception.RequisicaoInvalidaException;

/**
 * Guarda as imagens enviadas pelo app (foto de perfil, capa de evento) numa pasta do servidor
 * e devolve o nome do arquivo. Aceita só JPG, PNG e WEBP de verdade (confere os primeiros bytes,
 * não o nome do arquivo) e gera um nome aleatório, para ninguém sobrescrever arquivo de outra pessoa.
 */
@Service
public class ImagemService {

    private static final Logger log = LoggerFactory.getLogger(ImagemService.class);

    public static final long TAMANHO_MAXIMO_BYTES = 5L * 1024 * 1024;
    /** uuid.jpg | uuid.png | uuid.webp — nada de barras ou "..". */
    private static final Pattern NOME_VALIDO = Pattern.compile("^[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}\\.(jpg|png|webp)$");

    public enum TipoImagem {
        JPG("jpg", MediaType.IMAGE_JPEG),
        PNG("png", MediaType.IMAGE_PNG),
        WEBP("webp", MediaType.parseMediaType("image/webp"));

        final String extensao;
        final MediaType mediaType;

        TipoImagem(String extensao, MediaType mediaType) {
            this.extensao = extensao;
            this.mediaType = mediaType;
        }

        public MediaType mediaType() {
            return mediaType;
        }

        static TipoImagem daExtensao(String ext) {
            return Arrays.stream(values()).filter(t -> t.extensao.equals(ext)).findFirst().orElseThrow();
        }
    }

    public record ImagemSalva(String nome, TipoImagem tipo, long tamanhoBytes) {
    }

    private final Path pasta;

    public ImagemService(SportfyProperties properties) {
        String configurada = properties.uploads() == null ? null : properties.uploads().pasta();
        this.pasta = Path.of(configurada == null || configurada.isBlank() ? "uploads" : configurada)
                .toAbsolutePath().normalize();
    }

    public ImagemSalva salvar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new RequisicaoInvalidaException("Envie uma imagem no campo 'arquivo'.");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO_BYTES) {
            throw new RequisicaoInvalidaException("A imagem deve ter no máximo 5 MB.");
        }
        TipoImagem tipo = detectarTipo(arquivo);
        String nome = UUID.randomUUID() + "." + tipo.extensao;
        try {
            Files.createDirectories(pasta);
            try (InputStream in = arquivo.getInputStream()) {
                Files.copy(in, pasta.resolve(nome), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("Não foi possível salvar a imagem em {}", pasta, e);
            throw new IllegalStateException("Falha ao salvar a imagem", e);
        }
        return new ImagemSalva(nome, tipo, arquivo.getSize());
    }

    public Resource carregar(String nome) {
        if (nome == null || !NOME_VALIDO.matcher(nome).matches()) {
            throw new RecursoNaoEncontradoException("Imagem não encontrada.");
        }
        Path arquivo = pasta.resolve(nome).normalize();
        if (!arquivo.startsWith(pasta) || !Files.isRegularFile(arquivo)) {
            throw new RecursoNaoEncontradoException("Imagem não encontrada.");
        }
        return new PathResource(arquivo);
    }

    public MediaType tipoDoNome(String nome) {
        return TipoImagem.daExtensao(nome.substring(nome.lastIndexOf('.') + 1)).mediaType();
    }

    /** Confere a "assinatura" do arquivo (magic bytes). */
    static TipoImagem detectarTipo(MultipartFile arquivo) {
        byte[] b = new byte[12];
        int lidos;
        try (InputStream in = arquivo.getInputStream()) {
            lidos = in.readNBytes(b, 0, b.length);
        } catch (IOException e) {
            throw new RequisicaoInvalidaException("Não foi possível ler a imagem enviada.");
        }
        if (lidos >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return TipoImagem.JPG;
        }
        if (lidos >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return TipoImagem.PNG;
        }
        if (lidos >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return TipoImagem.WEBP;
        }
        throw new RequisicaoInvalidaException("Formato não aceito: envie uma imagem JPG, PNG ou WEBP.");
    }
}
