package br.com.aws.facialrecognition.controller.impl;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.aws.facialrecognition.controller.IFaceCamAuthenticationController;
import br.com.aws.facialrecognition.service.FaceCamAuthenticationService;

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
