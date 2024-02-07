package br.com.aws.rekognition.handler;

import br.com.aws.rekognition.enums.InternalTypeErrorCodesEnum;

public class IdNotFoundException extends ErrorCodeException{

	private static final long serialVersionUID = 1L;

	public IdNotFoundException() {
        super(InternalTypeErrorCodesEnum.E405);
    }

    public IdNotFoundException(String message) {
        super(InternalTypeErrorCodesEnum.E405, message);
    }
}