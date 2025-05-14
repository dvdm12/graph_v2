package com.example.miapp.exception;

/**
 * Excepción base para todas las excepciones específicas del dominio.
 * Proporciona un punto común de captura para excepciones relacionadas con
 * la lógica de negocio del sistema.
 */
public class DomainException extends RuntimeException {
    
    /**
     * Crea una nueva excepción del dominio con el mensaje especificado.
     * 
     * @param message Mensaje descriptivo del error
     */
    public DomainException(String message) {
        super(message);
    }
    
    /**
     * Crea una nueva excepción del dominio con el mensaje y causa especificados.
     * 
     * @param message Mensaje descriptivo del error
     * @param cause Excepción que causó este error
     */
    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}