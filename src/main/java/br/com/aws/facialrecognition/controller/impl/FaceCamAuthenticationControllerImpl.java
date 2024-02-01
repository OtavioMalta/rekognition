package br.com.aws.facialrecognition.controller.impl;

import br.com.aws.facialrecognition.controller.IFaceAuthenticationController;
import br.com.aws.facialrecognition.controller.IFaceCamAuthenticationController;
import br.com.aws.facialrecognition.dto.FaceAuthenticationResponse;
import br.com.aws.facialrecognition.service.FaceAuthenticationService;
import br.com.aws.facialrecognition.service.FaceCamAuthenticationService;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value = "/cam")
public class FaceCamAuthenticationControllerImpl implements IFaceCamAuthenticationController {

    private final FaceCamAuthenticationService faceCamAuthenticationService;

    public FaceCamAuthenticationControllerImpl(FaceCamAuthenticationService faceCamAuthenticationService) {
        this.faceCamAuthenticationService = faceCamAuthenticationService;
    }

    @PostMapping(value = "/opencam", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<String> openCam(String id) throws Exception {
		faceCamAuthenticationService.main(id);
		
		return null;
	}
}
