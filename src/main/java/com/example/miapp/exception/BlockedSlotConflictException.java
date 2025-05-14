package com.example.miapp.exception;

/**
 * Excepción lanzada cuando una asignación entra en conflicto
 * con una franja bloqueada de un profesor.
 */
public class BlockedSlotConflictException extends DomainException {
    
    private final int professorId;
    private final String day;
    private final String startTime;
    private final String endTime;
    
    /**
     * Crea una nueva excepción de conflicto con franja bloqueada.
     * 
     * @param message Mensaje descriptivo del error
     * @param professorId ID del profesor involucrado
     * @param day Día de la franja bloqueada
     * @param startTime Hora de inicio de la franja bloqueada
     * @param endTime Hora de fin de la franja bloqueada
     */
    public BlockedSlotConflictException(String message, int professorId, 
                                     String day, String startTime, String endTime) {
        super(message);
        this.professorId = professorId;
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
    }
    
    /**
     * Obtiene el ID del profesor involucrado.
     */
    public int getProfessorId() {
        return professorId;
    }
    
    /**
     * Obtiene el día de la franja bloqueada.
     */
    public String getDay() {
        return day;
    }
    
    /**
     * Obtiene la hora de inicio de la franja bloqueada.
     */
    public String getStartTime() {
        return startTime;
    }
    
    /**
     * Obtiene la hora de fin de la franja bloqueada.
     */
    public String getEndTime() {
        return endTime;
    }
}