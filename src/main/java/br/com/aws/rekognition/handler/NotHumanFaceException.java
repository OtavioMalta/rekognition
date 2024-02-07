package br.com.aws.rekognition.handler;

import br.com.aws.rekognition.enums.InternalTypeErrorCodesEnum;

public class NotHumanFaceException extends ErrorCodeException{
	
	private static final long serialVersionUID = 1L;
	
    public NotHumanFaceException() {
        super(InternalTypeErrorCodesEnum.E401);
    }

    public NotHumanFaceException(String message) {
        super(InternalTypeErrorCodesEnum.E401, message);
    }
}