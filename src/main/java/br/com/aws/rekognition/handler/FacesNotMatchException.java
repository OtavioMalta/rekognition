package br.com.aws.rekognition.handler;

import br.com.aws.rekognition.enums.InternalTypeErrorCodesEnum;

public class FacesNotMatchException extends ErrorCodeException{

	private static final long serialVersionUID = 1L;

    public FacesNotMatchException() {
        super(InternalTypeErrorCodesEnum.E404);
    }

    public FacesNotMatchException(String message) {
        super(InternalTypeErrorCodesEnum.E404, message);
    }
}