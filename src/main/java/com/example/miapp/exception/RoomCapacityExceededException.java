package com.example.miapp.exception;

/**
 * Excepción lanzada cuando se intenta asignar más estudiantes de los
 * que puede albergar un aula.
 */
public class RoomCapacityExceededException extends DomainException {
    
    private final int roomId;
    private final int roomCapacity;
    private final int assignedStudents;
    
    /**
     * Crea una nueva excepción de capacidad de aula excedida.
     * 
     * @param message Mensaje descriptivo del error
     * @param roomId ID del aula involucrada
     * @param roomCapacity Capacidad máxima del aula
     * @param assignedStudents Número de estudiantes que se intentaron asignar
     */
    public RoomCapacityExceededException(String message, int roomId, int roomCapacity, int assignedStudents) {
        super(message);
        this.roomId = roomId;
        this.roomCapacity = roomCapacity;
        this.assignedStudents = assignedStudents;
    }
    
    /**
     * Obtiene el ID del aula involucrada.
     */
    public int getRoomId() {
        return roomId;
    }
    
    /**
     * Obtiene la capacidad máxima del aula.
     */
    public int getRoomCapacity() {
        return roomCapacity;
    }
    
    /**
     * Obtiene el número de estudiantes que se intentaron asignar.
     */
    public int getAssignedStudents() {
        return assignedStudents;
    }
}