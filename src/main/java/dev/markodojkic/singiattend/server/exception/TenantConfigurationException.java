package dev.markodojkic.singiattend.server.exception;

public class TenantConfigurationException extends RuntimeException {
    public TenantConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}