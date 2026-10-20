package br.senai.sportfy.controller;

import java.util.concurrent.TimeUnit;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import br.senai.sportfy.service.ImagemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Upload de imagens (foto de perfil, capa de evento).
 * O app envia o arquivo, recebe a "url" e coloca essa url em fotoUrl ou imagemUrl.
 */
@RestController
@Tag(name = "Imagens", description = "Upload de fotos de perfil e capas de evento")
public class ImagemController {

    private final ImagemService imagemService;

    public ImagemController(ImagemService imagemService) {
        this.imagemService = imagemService;
    }

    public record ImagemResponse(String url, String nome, String tipo, long tamanhoBytes) {
    }

    @PostMapping(path = "/api/uploads/imagens", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Envia uma imagem (JPG, PNG ou WEBP, até 5 MB) no campo 'arquivo' e devolve a url para usar "
            + "em fotoUrl (perfil) ou imagemUrl (evento)")
    public ResponseEntity<ImagemResponse> enviar(@RequestPart("arquivo") MultipartFile arquivo) {
        ImagemService.ImagemSalva salva = imagemService.salvar(arquivo);
        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/uploads/{nome}").buildAndExpand(salva.nome()).toUriString();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ImagemResponse(url, salva.nome(), salva.tipo().mediaType().toString(), salva.tamanhoBytes()));
    }

    @GetMapping("/uploads/{nome}")
    @Operation(summary = "Mostra uma imagem enviada (público)")
    public ResponseEntity<Resource> ver(@PathVariable String nome) {
        Resource imagem = imagemService.carregar(nome);
        return ResponseEntity.ok()
                .contentType(imagemService.tipoDoNome(nome))
                // o nome é aleatório e nunca muda: o app e o navegador podem guardar em cache
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic().immutable())
                .body(imagem);
    }
}
