package br.com.aws.facialrecognition.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FaceAuthenticationResponse {

    private String userName;
    private BigDecimal confidencePercentage;
    private BigDecimal similarityPercentage;

}