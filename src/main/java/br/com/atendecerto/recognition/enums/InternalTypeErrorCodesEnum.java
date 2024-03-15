package br.com.atendecerto.recognition.enums;

public enum InternalTypeErrorCodesEnum {

    E401("Rosto não humano. Envie outra foto."),
    E402("Rosto não cadastrado, tente novamente."),
    E403("Múltiplos rostos na foto. Selecione uma foto onde tenha apenas 1 rosto."),
    E404("Os rostos não coincidem."),
    E405("O ID não foi encontrado.");

    private final String message;

    InternalTypeErrorCodesEnum(String message) {
        this.message = message;
    }

    public String getValue() {
        return this.name();
    }

    public String getMessage() {
        return message;
    }


    @Override
    public String toString() {
        return String.format("Error code: %s = %s.", getMessage());
    }
}