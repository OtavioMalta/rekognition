package br.com.atendecerto.recognition.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import br.com.atendecerto.recognition.dto.RecognitionResponse;

@Tag(name = "Autenticação")
public interface RecognitionController {
	
	@Operation(summary = "Comparação de face")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Sucesso"),
            @ApiResponse(responseCode = "400", description = "Erro na consulta", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno", content = @Content)
    })
    ResponseEntity<RecognitionResponse> compara(@Parameter(description = "Face a ser comparada") MultipartFile photo, 
    		@Parameter(description = "Id") String id) throws Exception;

    @Operation(summary = "Busca por face")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Sucesso"),
            @ApiResponse(responseCode = "400", description = "Erro na consulta", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno", content = @Content)
    })
    ResponseEntity<RecognitionResponse> busca(@Parameter(description = "Face a ser buscada") MultipartFile photo) throws Exception;

    @Operation(summary = "Salva uma nova face")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Sucesso"),
            @ApiResponse(responseCode = "400", description = "Erro na consulta", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno", content = @Content)
    })
    ResponseEntity<String> salva(@Parameter(description = "Face a ser salva") MultipartFile photo) throws Exception;
}
