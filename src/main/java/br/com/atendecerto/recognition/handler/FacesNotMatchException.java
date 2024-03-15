package br.com.atendecerto.recognition.handler;

import br.com.atendecerto.recognition.enums.InternalTypeErrorCodesEnum;

public class FacesNotMatchException extends ErrorCodeException{

	private static final long serialVersionUID = 1L;

    public FacesNotMatchException() {
        super(InternalTypeErrorCodesEnum.E404);
    }

    public FacesNotMatchException(String message) {
        super(InternalTypeErrorCodesEnum.E404, message);
    }
}