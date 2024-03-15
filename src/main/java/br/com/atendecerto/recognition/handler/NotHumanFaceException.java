package br.com.atendecerto.recognition.handler;

import br.com.atendecerto.recognition.enums.InternalTypeErrorCodesEnum;

public class NotHumanFaceException extends ErrorCodeException{
	
	private static final long serialVersionUID = 1L;
	
    public NotHumanFaceException() {
        super(InternalTypeErrorCodesEnum.E401);
    }

    public NotHumanFaceException(String message) {
        super(InternalTypeErrorCodesEnum.E401, message);
    }
}