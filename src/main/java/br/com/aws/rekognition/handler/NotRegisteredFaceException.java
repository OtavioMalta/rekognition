package br.com.aws.rekognition.handler;

import br.com.aws.rekognition.enums.InternalTypeErrorCodesEnum;

public class NotRegisteredFaceException extends ErrorCodeException{

	private static final long serialVersionUID = 1L;
	
    public NotRegisteredFaceException() {
        super(InternalTypeErrorCodesEnum.E402);
    }

    public NotRegisteredFaceException(String message) {
        super(InternalTypeErrorCodesEnum.E402, message);
    }
}