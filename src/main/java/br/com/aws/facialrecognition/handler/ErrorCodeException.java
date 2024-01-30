package br.com.aws.facialrecognition.handler;

import br.com.aws.facialrecognition.enums.InternalTypeErrorCodesEnum;

public class ErrorCodeException extends Exception{

	private static final long serialVersionUID = 1L;

    protected InternalTypeErrorCodesEnum errorcode;

    protected ErrorCodeException(InternalTypeErrorCodesEnum errorcode) {
        super(errorcode.getMessage());
        this.errorcode = errorcode;
    }

    protected ErrorCodeException(InternalTypeErrorCodesEnum errorcode, Object... args) {
        super(String.format(errorcode.getMessage(), args));
        this.errorcode = errorcode;
    }

    public InternalTypeErrorCodesEnum getErrorcode() {
        return errorcode;
    }
}