package br.com.aws.rekognition.controller.impl;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.com.aws.rekognition.controller.RekognitionController;
import br.com.aws.rekognition.dto.RekognitionResponse;
import br.com.aws.rekognition.service.RekognitionService;

@RestController
@Controller
@RequestMapping(value = "/rekognition")
public class RekognitionControllerImpl implements RekognitionController {

    private final RekognitionService RekognitionService;

    public RekognitionControllerImpl(RekognitionService RekognitionService) {
        this.RekognitionService = RekognitionService;
    }

    @PostMapping(value = "/salvar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> salva(@RequestPart(value = "foto") MultipartFile foto) throws Exception {
    	RekognitionService.salvar(foto);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping(value = "/buscar", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RekognitionResponse> busca(@RequestPart(value = "foto") MultipartFile foto) throws Exception {
        return new ResponseEntity<>(RekognitionService.buscar(foto), HttpStatus.OK);
    }
    
    @PostMapping(value = "/comparar", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RekognitionResponse> compara(@RequestPart(value = "foto") MultipartFile foto,@RequestPart(value = "id") String id) throws Exception {
        return new ResponseEntity<>(RekognitionService.comparar(foto, id), HttpStatus.OK);
    }
}
