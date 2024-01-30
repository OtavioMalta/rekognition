package br.com.aws.facialrecognition.handler;

import br.com.aws.facialrecognition.enums.InternalTypeErrorCodesEnum;

public class FacesNotMatchException extends ErrorCodeException{

	private static final long serialVersionUID = 1L;

    public FacesNotMatchException() {
        super(InternalTypeErrorCodesEnum.E404);
    }

    public FacesNotMatchException(String message) {
        super(InternalTypeErrorCodesEnum.E404, message);
    }
}