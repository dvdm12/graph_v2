package com.example.miapp.domain;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.miapp.exception.DomainException;
import com.example.miapp.exception.IncompatibleRoomException;
import com.example.miapp.exception.RoomCapacityExceededException;

/**
 * Representa un aula o espacio físico donde se imparten clases.
 */
public class Room {
    private static final Logger logger = LoggerFactory.getLogger(Room.class);
    private static final Object ID_LOCK = new Object(); // Para sincronización de nextId
    private static int nextId = 1;
    
    private final int id;
    private String name;
    private int capacity;  // Capacidad máxima de estudiantes
    private boolean isLab; // Indica si el aula es un laboratorio
    
    /**
     * Constructor para crear un aula nueva con ID automático.
     * 
     * @param name Nombre del aula
     * @param capacity Capacidad máxima de estudiantes
     * @param isLab Indica si el aula es un laboratorio
     * @throws NullPointerException si name es null
     * @throws DomainException si capacity es negativa
     */
    public Room(String name, int capacity, boolean isLab) {
        synchronized (ID_LOCK) {
            this.id = nextId++;
        }
        this.name = Objects.requireNonNull(name, "El nombre no puede ser null");
        
        // Validar que la capacidad sea positiva
        if (capacity < 0) {
            throw new DomainException("La capacidad del aula no puede ser negativa: " + capacity);
        }
        
        this.capacity = capacity;
        this.isLab = isLab;
        
        if (logger.isDebugEnabled()) {
            logger.debug("Aula creada: id={}, nombre={}, capacidad={}, esLaboratorio={}", 
                      id, name, capacity, isLab);
        }
    }
    
    /**
     * Constructor para cargar un aula existente con ID específico.
     * 
     * @param id ID del aula
     * @param name Nombre del aula
     * @param capacity Capacidad máxima de estudiantes
     * @param isLab Indica si el aula es un laboratorio
     * @throws NullPointerException si name es null
     * @throws DomainException si capacity es negativa
     */
    public Room(int id, String name, int capacity, boolean isLab) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "El nombre no puede ser null");
        
        // Validar que la capacidad sea positiva
        if (capacity < 0) {
            throw new DomainException("La capacidad del aula no puede ser negativa: " + capacity);
        }
        
        this.capacity = capacity;
        this.isLab = isLab;
        
        // Actualizar nextId si es necesario
        synchronized (ID_LOCK) {
            if (id >= nextId) {
                nextId = id + 1;
            }
        }
        
        if (logger.isDebugEnabled()) {
            logger.debug("Aula cargada: id={}, nombre={}, capacidad={}, esLaboratorio={}", 
                      id, name, capacity, isLab);
        }
    }
    
    /**
     * Verifica si esta aula tiene suficiente capacidad para un número de estudiantes.
     * 
     * @param studentCount número de estudiantes a verificar
     * @return true si hay suficiente capacidad, false en caso contrario
     */
    public boolean hasCapacityFor(int studentCount) {
        boolean hasCapacity = studentCount <= capacity;
        
        if (!hasCapacity && logger.isDebugEnabled()) {
            logger.debug("Aula id={} no tiene capacidad suficiente: requerido={}, disponible={}", 
                       id, studentCount, capacity);
        }
        
        return hasCapacity;
    }
    
    /**
     * Verifica si esta aula tiene suficiente capacidad para un número de estudiantes y lanza
     * una excepción si no es así.
     * 
     * @param studentCount número de estudiantes a verificar
     * @throws RoomCapacityExceededException si la capacidad del aula es insuficiente
     */
    public void verifyCapacityFor(int studentCount) {
        if (!hasCapacityFor(studentCount)) {
            throw new RoomCapacityExceededException(
                String.format("El aula %s (id=%d) no tiene capacidad suficiente: requerido=%d, disponible=%d",
                            name, id, studentCount, capacity),
                id,
                capacity,
                studentCount);
        }
    }
    
    /**
     * Verifica si esta aula es compatible con el requisito de laboratorio.
     * 
     * @param requiresLab indica si se requiere laboratorio
     * @return true si es compatible, false en caso contrario
     */
    public boolean isCompatibleWithLabRequirement(boolean requiresLab) {
        // Si la asignatura requiere laboratorio, el aula debe ser un laboratorio
        // Si la asignatura no requiere laboratorio, cualquier aula es válida
        boolean isCompatible = !requiresLab || this.isLab;
        
        if (!isCompatible && logger.isDebugEnabled()) {
            logger.debug("Aula id={} no cumple con requisito de laboratorio", id);
        }
        
        return isCompatible;
    }
    
    /**
     * Verifica si esta aula es compatible con el requisito de laboratorio de una materia y
     * lanza una excepción si no es así.
     * 
     * @param subject La materia con requisitos a verificar
     * @throws IncompatibleRoomException si el aula no es compatible con los requisitos
     * @throws NullPointerException si subject es null
     */
    public void verifyCompatibilityWith(Subject subject) {
        Objects.requireNonNull(subject, "La materia no puede ser null");
        
        boolean requiresLab = subject.requiresLab();
        if (!isCompatibleWithLabRequirement(requiresLab)) {
            throw new IncompatibleRoomException(
                String.format("El aula %s (id=%d) no es compatible con los requisitos de laboratorio de la materia %s: requiresLab=%b, isLab=%b",
                            name, id, subject.getName(), requiresLab, isLab),
                subject.getCode(),
                id);
        }
    }
    
    // Getters y setters
    public int getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "El nombre no puede ser null");
    }
    
    public int getCapacity() {
        return capacity;
    }
    
    public void setCapacity(int capacity) {
        if (capacity < 0) {
            throw new DomainException("La capacidad del aula no puede ser negativa: " + capacity);
        }
        this.capacity = capacity;
    }
    
    public boolean isLab() {
        return isLab;
    }
    
    public void setLab(boolean isLab) {
        this.isLab = isLab;
    }
    
    @Override
    public String toString() {
        return String.format("Room{id=%d, name='%s', capacity=%d, isLab=%b}", 
                           id, name, capacity, isLab);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Room room = (Room) obj;
        return id == room.id;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}