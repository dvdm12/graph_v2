package com.example.miapp.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.time.LocalTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.miapp.exception.AssignmentConflictException;
import com.example.miapp.exception.BlockedSlotConflictException;
import com.example.miapp.exception.DomainException;

/**
 * Representa a un profesor con sus datos, materias asignadas y franjas bloqueadas.
 */
public class Professor {
    private static final Logger logger = LoggerFactory.getLogger(Professor.class);
    private static final Object ID_LOCK = new Object(); // Para sincronización de nextId
    private static int nextId = 1;
    
    private final int id;
    private String name;
    private String department;
    private String email;
    private final List<Subject> subjects;
    private final List<BlockedSlot> blockedSlots; // Nueva colección de franjas bloqueadas
    
    /**
     * Constructor para crear un nuevo profesor con ID automático.
     * 
     * @param name Nombre del profesor
     * @param department Departamento al que pertenece
     * @param email Correo electrónico de contacto
     * @throws NullPointerException si algún parámetro es null
     * @throws DomainException si el email no tiene un formato válido
     */
    public Professor(String name, String department, String email) {
        synchronized (ID_LOCK) {
            this.id = nextId++;
        }
        this.name = Objects.requireNonNull(name, "El nombre no puede ser null");
        this.department = Objects.requireNonNull(department, "El departamento no puede ser null");
        this.email = validateEmail(email);
        this.subjects = new ArrayList<>();
        this.blockedSlots = new ArrayList<>();
        
        if (logger.isDebugEnabled()) {
            logger.debug("Profesor creado: id={}, nombre={}", id, name);
        }
    }
    
    /**
     * Constructor para cargar un profesor existente con ID específico.
     * 
     * @param id ID del profesor
     * @param name Nombre del profesor
     * @param department Departamento al que pertenece
     * @param email Correo electrónico de contacto
     * @throws NullPointerException si algún parámetro es null
     * @throws DomainException si el email no tiene un formato válido
     */
    public Professor(int id, String name, String department, String email) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "El nombre no puede ser null");
        this.department = Objects.requireNonNull(department, "El departamento no puede ser null");
        this.email = validateEmail(email);
        this.subjects = new ArrayList<>();
        this.blockedSlots = new ArrayList<>();
        
        // Actualizar nextId si es necesario
        synchronized (ID_LOCK) {
            if (id >= nextId) {
                nextId = id + 1;
            }
        }
        
        if (logger.isDebugEnabled()) {
            logger.debug("Profesor cargado: id={}, nombre={}", id, name);
        }
    }
    
    /**
     * Valida el formato del email.
     * 
     * @param email Email a validar
     * @return Email validado
     * @throws NullPointerException si email es null
     * @throws DomainException si el email no tiene un formato válido
     */
    private String validateEmail(String email) {
        Objects.requireNonNull(email, "El email no puede ser null");
        
        // Verificar formato básico de email usando una expresión regular simple
        if (!email.matches("^[^@]+@[^@]+\\.[^@]+$")) {
            throw new DomainException("El email no tiene un formato válido: " + email);
        }
        
        return email;
    }
    
    /**
     * Asigna una materia a este profesor.
     * 
     * @param subject Materia a asignar
     * @throws NullPointerException si subject es null
     */
    public void assignSubject(Subject subject) {
        Objects.requireNonNull(subject, "La materia no puede ser null");
        
        if (!subjects.contains(subject)) {
            subjects.add(subject);
            
            if (logger.isDebugEnabled()) {
                logger.debug("Materia {} asignada a profesor id={}", 
                           subject.getCode(), this.id);
            }
        }
    }
    
    /**
     * Comprueba si el profesor tiene asignada una materia específica.
     * 
     * @param subjectCode Código de la materia a verificar
     * @return true si tiene asignada la materia, false en caso contrario
     */
    public boolean hasSubject(String subjectCode) {
        return subjects.stream()
            .anyMatch(s -> s.getCode().equals(subjectCode));
    }
    
    /**
     * Verifica que el profesor tenga asignada una materia y lanza una excepción si no.
     * 
     * @param subjectCode Código de la materia a verificar
     * @throws AssignmentConflictException si el profesor no tiene asignada la materia
     */
    public void verifyHasSubject(String subjectCode) {
        if (!hasSubject(subjectCode)) {
            throw new AssignmentConflictException(
                String.format("El profesor %s (id=%d) no tiene asignada la materia %s",
                        name, id, subjectCode),
                0, // No hay asignación asociada
                0, // No hay segunda asignación
                "PROFESSOR_SUBJECT_MISMATCH");
        }
    }
    
    /**
     * Desasigna una materia de este profesor.
     * 
     * @param subject Materia a desasignar
     * @return true si se eliminó, false si no estaba asignada
     * @throws NullPointerException si subject es null
     */
    public boolean removeSubject(Subject subject) {
        Objects.requireNonNull(subject, "La materia no puede ser null");
        
        boolean removed = subjects.remove(subject);
        
        if (removed && logger.isDebugEnabled()) {
            logger.debug("Materia {} desasignada de profesor id={}", 
                       subject.getCode(), this.id);
        }
        
        return removed;
    }
    
    /**
     * Añade una franja bloqueada para este profesor.
     * 
     * @param blockedSlot Franja a añadir
     * @throws NullPointerException si blockedSlot es null
     */
    public void addBlockedSlot(BlockedSlot blockedSlot) {
        Objects.requireNonNull(blockedSlot, "La franja bloqueada no puede ser null");
        
        blockedSlots.add(blockedSlot);
        
        if (logger.isDebugEnabled()) {
            logger.debug("Franja bloqueada añadida: {} de {} a {} para profesor id={}",
                       blockedSlot.getDay(), blockedSlot.getStartTime(),
                       blockedSlot.getEndTime(), this.id);
        }
    }
    
    /**
     * Añade una franja bloqueada construida a partir de los parámetros.
     * 
     * @param day Día de la semana
     * @param startTime Hora de inicio
     * @param endTime Hora de fin
     * @throws NullPointerException si algún parámetro es null
     */
    public void addBlockedSlot(String day, LocalTime startTime, LocalTime endTime) {
        BlockedSlot slot = new BlockedSlot.Builder()
            .day(day)
            .startTime(startTime)
            .endTime(endTime)
            .build();
            
        addBlockedSlot(slot);
    }
    
    /**
     * Elimina una franja bloqueada.
     * 
     * @param blockedSlot Franja a eliminar
     * @return true si se eliminó, false si no estaba presente
     * @throws NullPointerException si blockedSlot es null
     */
    public boolean removeBlockedSlot(BlockedSlot blockedSlot) {
        Objects.requireNonNull(blockedSlot, "La franja bloqueada no puede ser null");
        
        boolean removed = blockedSlots.remove(blockedSlot);
        
        if (removed && logger.isDebugEnabled()) {
            logger.debug("Franja bloqueada eliminada: {} de {} a {} para profesor id={}",
                       blockedSlot.getDay(), blockedSlot.getStartTime(),
                       blockedSlot.getEndTime(), this.id);
        }
        
        return removed;
    }
    
    /**
     * Elimina todas las franjas bloqueadas.
     */
    public void clearBlockedSlots() {
        int count = blockedSlots.size();
        blockedSlots.clear();
        
        if (logger.isDebugEnabled()) {
            logger.debug("Eliminadas {} franjas bloqueadas para profesor id={}", count, this.id);
        }
    }
    
    /**
     * Verifica si este profesor tiene conflicto con una asignación propuesta.
     * 
     * @param day Día de la semana
     * @param startTime Hora de inicio
     * @param endTime Hora de fin
     * @return true si hay alguna franja bloqueada que se solape, false en caso contrario
     * @throws NullPointerException si algún parámetro es null
     */
    public boolean hasBlockedSlotConflict(String day, LocalTime startTime, LocalTime endTime) {
        Objects.requireNonNull(day, "El día no puede ser null");
        Objects.requireNonNull(startTime, "La hora de inicio no puede ser null");
        Objects.requireNonNull(endTime, "La hora de fin no puede ser null");
        
        for (BlockedSlot slot : blockedSlots) {
            if (slot.overlaps(day, startTime, endTime)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Profesor id={} tiene conflicto en {} de {} a {}",
                               this.id, day, startTime, endTime);
                }
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Verifica si este profesor tiene conflicto con una asignación propuesta y lanza
     * una excepción si existe un conflicto.
     * 
     * @param day Día de la semana
     * @param startTime Hora de inicio
     * @param endTime Hora de fin
     * @throws NullPointerException si algún parámetro es null
     * @throws BlockedSlotConflictException si hay alguna franja bloqueada que se solape
     */
    public void verifyNoBlockedSlotConflict(String day, LocalTime startTime, LocalTime endTime) {
        Objects.requireNonNull(day, "El día no puede ser null");
        Objects.requireNonNull(startTime, "La hora de inicio no puede ser null");
        Objects.requireNonNull(endTime, "La hora de fin no puede ser null");
        
        for (BlockedSlot slot : blockedSlots) {
            if (slot.overlaps(day, startTime, endTime)) {
                throw new BlockedSlotConflictException(
                    String.format("El profesor %s (id=%d) tiene una franja bloqueada en %s de %s a %s que se solapa con el horario propuesto",
                            name, id, slot.getDay(), slot.getStartTime(), slot.getEndTime()),
                    id,
                    slot.getDay(),
                    slot.getStartTime().toString(),
                    slot.getEndTime().toString());
            }
        }
    }
    
    /**
     * Obtiene todas las franjas bloqueadas que tienen conflicto con el horario propuesto.
     * 
     * @param day Día de la semana
     * @param startTime Hora de inicio
     * @param endTime Hora de fin
     * @return Lista de franjas bloqueadas que se solapan (nunca null, puede estar vacía)
     * @throws NullPointerException si algún parámetro es null
     */
    public List<BlockedSlot> getConflictingBlockedSlots(String day, LocalTime startTime, LocalTime endTime) {
        Objects.requireNonNull(day, "El día no puede ser null");
        Objects.requireNonNull(startTime, "La hora de inicio no puede ser null");
        Objects.requireNonNull(endTime, "La hora de fin no puede ser null");
        
        List<BlockedSlot> conflictingSlots = new ArrayList<>();
        
        for (BlockedSlot slot : blockedSlots) {
            if (slot.overlaps(day, startTime, endTime)) {
                conflictingSlots.add(slot);
            }
        }
        
        return conflictingSlots;
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
    
    public String getDepartment() {
        return department;
    }
    
    public void setDepartment(String department) {
        this.department = Objects.requireNonNull(department, "El departamento no puede ser null");
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = validateEmail(email);
    }
    
    /**
     * Obtiene las materias asignadas (vista inmutable).
     */
    public List<Subject> getSubjects() {
        return Collections.unmodifiableList(subjects);
    }
    
    /**
     * Obtiene las franjas bloqueadas (vista inmutable).
     */
    public List<BlockedSlot> getBlockedSlots() {
        return Collections.unmodifiableList(blockedSlots);
    }
    
    @Override
    public String toString() {
        return String.format("Profesor{id=%d, nombre='%s', departamento='%s', email='%s', materias=%d, franjasBloquedas=%d}",
                           id, name, department, email, subjects.size(), blockedSlots.size());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Professor professor = (Professor) obj;
        return id == professor.id;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}