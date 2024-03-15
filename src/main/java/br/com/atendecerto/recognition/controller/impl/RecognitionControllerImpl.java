package br.com.atendecerto.recognition.controller.impl;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.com.atendecerto.recognition.controller.RecognitionController;
import br.com.atendecerto.recognition.dto.RecognitionResponse;
import br.com.atendecerto.recognition.service.RecognitionService;

@RestController
@Controller
@RequestMapping(value = "/recognition")
public class RecognitionControllerImpl implements RecognitionController {

    private final RecognitionService RecognitionService;

    public RecognitionControllerImpl(RecognitionService RecognitionService) {
        this.RecognitionService = RecognitionService;
    }

    @PostMapping(value = "/salvar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> salva(@RequestPart(value = "foto") MultipartFile foto) throws Exception {
    	RecognitionService.salvar(foto);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping(value = "/buscar", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RecognitionResponse> busca(@RequestPart(value = "foto") MultipartFile foto) throws Exception {
        return new ResponseEntity<>(RecognitionService.buscar(foto), HttpStatus.OK);
    }
    
    @PostMapping(value = "/comparar", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RecognitionResponse> compara(@RequestPart(value = "foto") MultipartFile foto,@RequestPart(value = "id") String id) throws Exception {
        return new ResponseEntity<>(RecognitionService.comparar(foto, id), HttpStatus.OK);
    }
}
