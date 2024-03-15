package br.com.atendecerto.recognition.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecognitionResponse {
    private String idUsuario;
    private BigDecimal confianca;
    private BigDecimal similaridade;

}