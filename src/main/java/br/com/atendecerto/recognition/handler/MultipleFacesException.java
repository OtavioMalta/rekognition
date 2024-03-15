package br.com.atendecerto.recognition.handler;

import br.com.atendecerto.recognition.enums.InternalTypeErrorCodesEnum;

public class MultipleFacesException extends ErrorCodeException{

	private static final long serialVersionUID = 1L;

    public MultipleFacesException() {
        super(InternalTypeErrorCodesEnum.E403);
    }

    public MultipleFacesException(String message) {
        super(InternalTypeErrorCodesEnum.E403, message);
    }
}