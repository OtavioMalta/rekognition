package br.com.aws.rekognition.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RekognitionResponse {
    private String idUsuario;
    private BigDecimal confianca;
    private BigDecimal similaridade;

}