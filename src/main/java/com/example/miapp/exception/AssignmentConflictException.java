package com.example.miapp.exception;

/**
 * Excepción lanzada cuando se detecta un conflicto entre asignaciones.
 * Puede representar conflictos de profesor, aula, grupo, etc.
 */
public class AssignmentConflictException extends DomainException {
    
    private final int assignmentId1;
    private final int assignmentId2;
    private final String conflictType;
    
    /**
     * Crea una nueva excepción de conflicto entre asignaciones.
     * 
     * @param message Mensaje descriptivo del error
     * @param assignmentId1 ID de la primera asignación involucrada
     * @param assignmentId2 ID de la segunda asignación involucrada
     * @param conflictType Tipo de conflicto detectado
     */
    public AssignmentConflictException(String message, int assignmentId1, int assignmentId2, String conflictType) {
        super(message);
        this.assignmentId1 = assignmentId1;
        this.assignmentId2 = assignmentId2;
        this.conflictType = conflictType;
    }
    
    /**
     * Obtiene el ID de la primera asignación involucrada en el conflicto.
     */
    public int getAssignmentId1() {
        return assignmentId1;
    }
    
    /**
     * Obtiene el ID de la segunda asignación involucrada en el conflicto.
     */
    public int getAssignmentId2() {
        return assignmentId2;
    }
    
    /**
     * Obtiene el tipo de conflicto detectado.
     */
    public String getConflictType() {
        return conflictType;
    }
}