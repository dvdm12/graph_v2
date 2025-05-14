package com.example.miapp.exception;

/**
 * Excepción lanzada cuando se intenta asignar una materia a un aula incompatible,
 * por ejemplo, una materia que requiere laboratorio a un aula normal.
 */
public class IncompatibleRoomException extends DomainException {
    
    private final String subjectCode;
    private final int roomId;
    
    /**
     * Crea una nueva excepción de incompatibilidad de aula.
     * 
     * @param message Mensaje descriptivo del error
     * @param subjectCode Código de la materia involucrada
     * @param roomId ID del aula incompatible
     */
    public IncompatibleRoomException(String message, String subjectCode, int roomId) {
        super(message);
        this.subjectCode = subjectCode;
        this.roomId = roomId;
    }
    
    /**
     * Obtiene el código de la materia involucrada.
     */
    public String getSubjectCode() {
        return subjectCode;
    }
    
    /**
     * Obtiene el ID del aula incompatible.
     */
    public int getRoomId() {
        return roomId;
    }
}