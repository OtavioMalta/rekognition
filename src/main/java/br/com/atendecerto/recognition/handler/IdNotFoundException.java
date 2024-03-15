package br.com.atendecerto.recognition.handler;

import br.com.atendecerto.recognition.enums.InternalTypeErrorCodesEnum;

public class IdNotFoundException extends ErrorCodeException{

	private static final long serialVersionUID = 1L;

	public IdNotFoundException() {
        super(InternalTypeErrorCodesEnum.E405);
    }

    public IdNotFoundException(String message) {
        super(InternalTypeErrorCodesEnum.E405, message);
    }
}