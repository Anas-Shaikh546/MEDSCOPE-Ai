package com.medscope.interpretation.client;

/**
 * Thrown when communication with the AI service fails or returns an error.
 */
public class AiServiceException extends RuntimeException {

    public AiServiceException(String message) {
        super(message);
    }

    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
