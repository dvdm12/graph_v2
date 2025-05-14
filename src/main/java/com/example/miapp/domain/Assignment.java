package com.example.miapp.domain;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.example.miapp.domain.conflict.ConflictEdge;
import com.example.miapp.domain.conflict.ConflictType;
import com.example.miapp.exception.AssignmentConflictException;
import com.example.miapp.exception.BlockedSlotConflictException;
import com.example.miapp.exception.DomainException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Representa una asignación de clase con todos los datos necesarios.
 * Versión con referencias directas a entidades relacionadas.
 */
@JsonDeserialize(builder = Assignment.Builder.class)
public class Assignment {
    private static final Logger logger = LoggerFactory.getLogger(Assignment.class);
    
    // Identificador único
    private final int id;
    private final LocalDate assignmentDate;
    
    // Entidades relacionadas
    private final Professor professor;
    private final Room room;
    private final Subject subject; // Opcional: podemos añadir referencia a Subject si es necesario
    
    // Información del grupo
    private final int groupId;
    private final String groupName;
    
    // Información del horario
    private final String day;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final String sessionType;    // "D"=Diurno, "N"=Nocturno
    
    // Información adicional
    private final int enrolledStudents;

    private Assignment(Builder b) {
        // Identificador único
        this.id = b.id;
        this.assignmentDate = Objects.requireNonNull(b.assignmentDate, 
                                                    "La fecha de asignación no puede ser null");
        
        // Entidades relacionadas
        this.professor = Objects.requireNonNull(b.professor, "El profesor no puede ser null");
        this.room = Objects.requireNonNull(b.room, "El aula no puede ser null");
        this.subject = b.subject; // El subject es opcional en esta implementación
        
        // Información del grupo
        this.groupId = b.groupId;
        this.groupName = Objects.requireNonNull(b.groupName, "El nombre del grupo no puede ser null");
        
        // Información del horario
        this.day = Objects.requireNonNull(b.day, "El día no puede ser null");
        this.startTime = Objects.requireNonNull(b.startTime, "La hora de inicio no puede ser null");
        this.endTime = Objects.requireNonNull(b.endTime, "La hora de fin no puede ser null");
        this.sessionType = Objects.requireNonNull(b.sessionType, "El tipo de sesión no puede ser null");
        
        // Información adicional
        this.enrolledStudents = b.enrolledStudents;
        
        validateAssignment();
        
        if (logger.isDebugEnabled()) {
            logger.debug("Assignment created: id={}, professor={}, room={}, day={}, time={}-{}",
                        id, professor.getName(), room.getName(), day, startTime, endTime);
        }
    }

    /**
     * Valida que los datos de la asignación sean coherentes.
     * Solo realiza validaciones estructurales, no de conflictos.
     * @throws DomainException si los datos son incoherentes
     */
    private void validateAssignment() {
        // Validación estructural: el tiempo debe ser coherente
        if (!endTime.isAfter(startTime)) {
            throw new DomainException("endTime debe ser posterior a startTime");
        }
        
        // Las siguientes validaciones se han convertido en métodos de detección de conflictos
        // en lugar de lanzar excepciones durante la creación del objeto
    }

    /**
     * Verifica si el aula tiene capacidad suficiente para los estudiantes inscritos.
     * 
     * @return true si hay capacidad suficiente, false en caso contrario
     */
    public boolean hasRoomCapacity() {
        return room.hasCapacityFor(enrolledStudents);
    }
    
    /**
     * Verifica si el aula es compatible con los requisitos de la materia.
     * 
     * @return true si es compatible, false en caso contrario
     */
    public boolean hasRoomCompatibility() {
        return subject == null || room.isCompatibleWithLabRequirement(subject.requiresLab());
    }

    /**
     * Verifica si esta asignación tiene conflicto con alguna franja bloqueada del profesor.
     * 
     * @return true si hay conflicto, false en caso contrario
     */
    public boolean hasBlockedSlotConflict() {
        return hasBlockedSlotConflict(false);
    }
    
    /**
     * Verifica si esta asignación tiene conflicto con alguna franja bloqueada del profesor.
     * 
     * @param throwExceptionOnConflict si es true, lanza una excepción en caso de conflicto
     * @return true si hay conflicto, false en caso contrario
     * @throws BlockedSlotConflictException si hay conflicto y throwExceptionOnConflict es true
     */
    public boolean hasBlockedSlotConflict(boolean throwExceptionOnConflict) {
        boolean hasConflict = professor.hasBlockedSlotConflict(this.day, this.startTime, this.endTime);
        
        if (hasConflict && throwExceptionOnConflict) {
            throw new BlockedSlotConflictException(
                String.format("La asignación id=%d tiene conflicto con una franja bloqueada del profesor %s en %s de %s a %s",
                        id, professor.getName(), day, startTime, endTime),
                professor.getId(),
                day,
                startTime.toString(),
                endTime.toString());
        }
        
        return hasConflict;
    }
    
    /**
     * Verifica si el profesor está autorizado para impartir la materia de esta asignación.
     * 
     * @return true si el profesor está autorizado, false en caso contrario
     */
    public boolean hasProfessorSubjectAuthorization() {
        // Si no hay materia asignada, no hay conflicto
        return subject == null || professor.hasSubject(subject.getCode());
    }
    
    /**
     * Verifica si el profesor está autorizado para impartir la materia y lanza una excepción si no lo está.
     * 
     * @param throwExceptionOnConflict si es true, lanza una excepción en caso de conflicto
     * @return true si hay conflicto, false en caso contrario
     * @throws AssignmentConflictException si el profesor no está autorizado y throwExceptionOnConflict es true
     */
    public boolean hasProfessorSubjectAuthorization(boolean throwExceptionOnConflict) {
        boolean hasAuthorization = hasProfessorSubjectAuthorization();
        
        if (!hasAuthorization && throwExceptionOnConflict) {
            throw new AssignmentConflictException(
                String.format("El profesor %s (id=%d) no está autorizado para impartir la materia %s",
                            professor.getName(), professor.getId(), subject.getCode()),
                id,
                id,  // Auto-conflicto
                "PROFESSOR_SUBJECT_MISMATCH");
        }
        
        return hasAuthorization;
    }

    /**
     * Determina si esta asignación se solapa temporalmente con otra.
     * @param other La otra asignación para verificar solapamiento
     * @return true si hay solapamiento temporal, false en caso contrario
     * @throws NullPointerException si other es null
     */
    public boolean overlapsTimeWith(Assignment other) {
        Objects.requireNonNull(other, "La asignación a comparar no puede ser null");
        
        if (!this.day.equals(other.day)) {
            return false;
        }
        
        boolean overlaps = !this.endTime.isBefore(other.startTime) && 
                          !this.startTime.isAfter(other.endTime);
        
        if (overlaps && logger.isTraceEnabled()) {
            logger.trace("Time overlap between id={} ({}-{}) and id={} ({}-{})",
                       this.id, this.startTime, this.endTime,
                       other.id, other.startTime, other.endTime);
        }
        
        return overlaps;
    }

    /**
     * Calcula los conflictos entre esta asignación y otra.
     * @param other La otra asignación a comparar
     * @return Mapa de tipos de conflicto y sus aristas correspondientes
     * @throws NullPointerException si other es null
     */
    public Map<String, List<ConflictEdge>> conflictsWith(Assignment other) {
        // Validar parámetro
        Objects.requireNonNull(other, "La asignación a comparar no puede ser null");
        
        // Verificar si son la misma asignación
        if (this.id == other.id) {
            if (logger.isTraceEnabled()) {
                logger.trace("No checking conflicts with self (same ID: {})", this.id);
            }
            
            // Comprobar auto-conflictos para la misma asignación
            Map<String, List<ConflictEdge>> selfConflicts = new HashMap<>();
            
            // Verificar autorización profesor-materia (auto-conflicto)
            if (!hasProfessorSubjectAuthorization()) {
                selfConflicts
                    .computeIfAbsent("professorSubject", k -> new java.util.ArrayList<>())
                    .add(new ConflictEdge(ConflictType.PROFESSOR_SUBJECT_MISMATCH));
                    
                if (logger.isDebugEnabled()) {
                    logger.debug("Professor-Subject mismatch detected for assignment id={}: Professor id={} not authorized for subject {}",
                               this.id, professor.getId(), subject != null ? subject.getCode() : "null");
                }
            }
            
            // Verificar capacidad del aula (auto-conflicto)
            if (!hasRoomCapacity()) {
                selfConflicts
                    .computeIfAbsent("roomCapacity", k -> new java.util.ArrayList<>())
                    .add(new ConflictEdge(ConflictType.ROOM_CAPACITY));
                    
                if (logger.isDebugEnabled()) {
                    logger.debug("Room capacity exceeded for assignment id={}: required={}, available={}",
                               this.id, this.enrolledStudents, this.room.getCapacity());
                }
            }
            
            // Verificar compatibilidad de aula (auto-conflicto)
            if (!hasRoomCompatibility()) {
                selfConflicts
                    .computeIfAbsent("roomCompatibility", k -> new java.util.ArrayList<>())
                    .add(new ConflictEdge(ConflictType.ROOM_COMPATIBILITY));
                    
                if (logger.isDebugEnabled()) {
                    logger.debug("Room incompatibility detected for assignment id={}: room={}, requiresLab={}",
                               this.id, this.room.getName(), this.subject != null ? this.subject.requiresLab() : "null");
                }
            }
            
            return selfConflicts;
        }
        
        // Si no hay solapamiento temporal, no hay conflicto
        if (!overlapsTimeWith(other)) {
            if (logger.isTraceEnabled()) {
                logger.trace("No time overlap between id={} and id={}, no conflicts", 
                           this.id, other.id);
            }
            return new HashMap<>();
        }
        
        // Mapa para almacenar los conflictos encontrados
        Map<String, List<ConflictEdge>> conflicts = new HashMap<>();
        
        // Verificar cada tipo de conflicto
        checkProfessorConflict(other, conflicts);
        checkRoomConflict(other, conflicts);
        checkGroupConflict(other, conflicts);
        checkSessionTypeConflict(other, conflicts);
        
        // Registrar resultado
        if (conflicts.isEmpty()) {
            if (logger.isTraceEnabled()) {
                logger.trace("No specific conflicts found between id={} and id={} despite time overlap", 
                           this.id, other.id);
            }
        } else if (logger.isDebugEnabled()) {
            logger.debug("Found {} conflict types between id={} and id={}: {}", 
                       conflicts.size(), this.id, other.id, 
                       String.join(", ", conflicts.keySet()));
        }
        
        return conflicts;
    }
    
    /**
     * Verifica si hay conflicto con otra asignación y lanza una excepción si se encuentra alguno.
     * @param other La otra asignación para comparar
     * @throws AssignmentConflictException si se encuentra algún conflicto
     */
    public void verifyNoConflictWith(Assignment other) {
        Map<String, List<ConflictEdge>> conflicts = conflictsWith(other);
        
        if (!conflicts.isEmpty()) {
            // Obtener el primer tipo de conflicto encontrado
            String conflictType = conflicts.keySet().iterator().next();
            List<ConflictEdge> edges = conflicts.get(conflictType);

            //Obtener el tipo especifico de conflicto de la primera arista
            String conflictLabel = edges.isEmpty() 
            ? conflictType : edges.get(0).getType().getLabel();
            
            throw new AssignmentConflictException(
                String.format("Conflicto detectado entre asignaciones id=%d e id=%d: %s",
                            this.id, other.id, conflictLabel),
                this.id,
                other.id,
                conflictType);
        }
    }
    
    /**
     * Verifica si hay conflicto de profesor entre esta asignación y otra.
     * 
     * @param other La otra asignación para comparar
     * @param conflicts Mapa donde se añadirá el conflicto si existe
     */
    private void checkProfessorConflict(Assignment other, Map<String, List<ConflictEdge>> conflicts) {
        if (this.professor.getId() == other.professor.getId()) {
            conflicts
                .computeIfAbsent("professor", k -> new java.util.ArrayList<>())
                .add(new ConflictEdge(ConflictType.PROFESSOR));
                
            if (logger.isDebugEnabled()) {
                logger.debug("Professor conflict detected: id={} and id={} have same professor (id={})",
                           this.id, other.id, this.professor.getId());
            }
        }
    }
    
    /**
     * Verifica si hay conflicto de sala entre esta asignación y otra.
     * 
     * @param other La otra asignación para comparar
     * @param conflicts Mapa donde se añadirá el conflicto si existe
     */
    private void checkRoomConflict(Assignment other, Map<String, List<ConflictEdge>> conflicts) {
        if (this.room.getId() == other.room.getId()) {
            conflicts
                .computeIfAbsent("room", k -> new java.util.ArrayList<>())
                .add(new ConflictEdge(ConflictType.ROOM));
                
            if (logger.isDebugEnabled()) {
                logger.debug("Room conflict detected: id={} and id={} have same room (id={})",
                           this.id, other.id, this.room.getId());
            }
        }
    }
    
    /**
     * Verifica si hay conflicto de grupo entre esta asignación y otra.
     * 
     * @param other La otra asignación para comparar
     * @param conflicts Mapa donde se añadirá el conflicto si existe
     */
    private void checkGroupConflict(Assignment other, Map<String, List<ConflictEdge>> conflicts) {
        if (this.groupId == other.groupId) {
            conflicts
                .computeIfAbsent("group", k -> new java.util.ArrayList<>())
                .add(new ConflictEdge(ConflictType.GROUP));
                
            if (logger.isDebugEnabled()) {
                logger.debug("Group conflict detected: id={} and id={} have same group (id={})",
                           this.id, other.id, this.groupId);
            }
        }
    }
    
    /**
     * Verifica si hay conflicto de tipo de sesión entre esta asignación y otra.
     * 
     * @param other La otra asignación para comparar
     * @param conflicts Mapa donde se añadirá el conflicto si existe
     */
    private void checkSessionTypeConflict(Assignment other, Map<String, List<ConflictEdge>> conflicts) {
        if (this.sessionType.equals(other.sessionType)) {
            conflicts
                .computeIfAbsent("sessionType", k -> new java.util.ArrayList<>())
                .add(new ConflictEdge(ConflictType.SESSION_TYPE));
                
            if (logger.isDebugEnabled()) {
                logger.debug("Session type conflict detected: id={} and id={} have same session type ({})",
                           this.id, other.id, this.sessionType);
            }
        }
    }

    /**
     * Crea un builder pre-configurado con todos los valores actuales.
     * Facilita la creación de copias modificadas.
     * 
     * @return Un Builder pre-configurado con los valores de esta asignación
     */
    public Builder toBuilder() {
        return new Builder()
            .id(this.id)
            .assignmentDate(this.assignmentDate)
            .professor(this.professor)
            .room(this.room)
            .subject(this.subject)
            .groupId(this.groupId)
            .groupName(this.groupName)
            .day(this.day)
            .startTime(this.startTime)
            .endTime(this.endTime)
            .sessionType(this.sessionType)
            .enrolledStudents(this.enrolledStudents);
    }

    // ===== Getters =====
    
    // Identificador único
    public int getId() { return id; }
    public LocalDate getAssignmentDate() { return assignmentDate; }
    
    // Entidades relacionadas
    public Professor getProfessor() { return professor; }
    public Room getRoom() { return room; }
    public Subject getSubject() { return subject; } // Puede ser null
    
    // Información del grupo
    public int getGroupId() { return groupId; }
    public String getGroupName() { return groupName; }
    
    // Información del horario
    public String getDay() { return day; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public String getSessionType() { return sessionType; }
    
    // Información adicional
    public int getEnrolledStudents() { return enrolledStudents; }
    
    // Métodos de conveniencia para mantener compatibilidad con código existente
    public int getProfessorId() { return professor.getId(); }
    public String getProfessorName() { return professor.getName(); }
    public int getRoomId() { return room.getId(); }
    public String getRoomName() { return room.getName(); }
    public boolean isRequiresLab() { return subject != null && subject.requiresLab(); }
    
    @Override
    public String toString() {
        return String.format("Assignment{id=%d, professor=%s, room=%s, subject=%s, day=%s, time=%s-%s, group=%s}",
                           id, professor.getName(), room.getName(), 
                           subject != null ? subject.getCode() : "N/A",
                           day, startTime, endTime, groupName);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Assignment other = (Assignment) obj;
        return id == other.id;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Builder para Assignment.
     */
    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        // Identificador único
        private int id;
        private LocalDate assignmentDate;
        
        // Entidades relacionadas
        private Professor professor;
        private Room room;
        private Subject subject;
        
        // Información del grupo
        private int groupId;
        private String groupName;
        
        // Información del horario
        private String day;
        private LocalTime startTime;
        private LocalTime endTime;
        private String sessionType;
        
        // Información adicional
        private int enrolledStudents;

        // Identificador único
        public Builder id(int id) { this.id = id; return this; }
        public Builder assignmentDate(LocalDate date) { this.assignmentDate = date; return this; }
        
        // Entidades relacionadas
        public Builder professor(Professor professor) { this.professor = professor; return this; }
        public Builder room(Room room) { this.room = room; return this; }
        public Builder subject(Subject subject) { this.subject = subject; return this; }
        
        // Información del grupo
        public Builder groupId(int id) { this.groupId = id; return this; }
        public Builder groupName(String name) { this.groupName = name; return this; }
        
        // Información del horario
        public Builder day(String day) { this.day = day; return this; }
        public Builder startTime(LocalTime time) { this.startTime = time; return this; }
        public Builder endTime(LocalTime time) { this.endTime = time; return this; }
        public Builder sessionType(String type) { this.sessionType = type; return this; }
        
        // Información adicional
        public Builder enrolledStudents(int count) { this.enrolledStudents = count; return this; }

        // Métodos para mantener compatibilidad con código existente
        public Builder professorId(int id) {
            return this;
        }
        
        public Builder professorName(String name) {
            return this;
        }
        
        public Builder roomId(int id) {
            return this;
        }
        
        public Builder roomName(String name) {
            return this;
        }
        
        public Builder requiresLab(boolean requires) {
            return this;
        }

        public Assignment build() {
            return new Assignment(this);
        }
    }
}