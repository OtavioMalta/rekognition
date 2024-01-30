package br.com.aws.facialrecognition.handler;

import br.com.aws.facialrecognition.enums.InternalTypeErrorCodesEnum;

public class IdNotFoundException extends ErrorCodeException{

	private static final long serialVersionUID = 1L;

	public IdNotFoundException() {
        super(InternalTypeErrorCodesEnum.E405);
    }

    public IdNotFoundException(String message) {
        super(InternalTypeErrorCodesEnum.E405, message);
    }
}