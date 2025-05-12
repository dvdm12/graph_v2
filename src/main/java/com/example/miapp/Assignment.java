package com.example.miapp;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Representa una asignación de clase con todos los datos necesarios para detección de conflictos.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(builder = Assignment.Builder.class)
public class Assignment {
    private static final Logger logger = LoggerFactory.getLogger(Assignment.class);

    private int id;
    private LocalDate assignmentDate;
    private int professorId;
    private String professorName;
    private int subjectGroupId;
    private String subjectCode;
    private String subjectName;
    private String groupName;
    private int roomId;
    private String roomName;
    private String day;
    private LocalTime startTime;
    private LocalTime endTime;
    private String sessionType;    // "D"=Diurno, "N"=Nocturno
    private boolean requiresLab;
    private int enrolledStudents;

    private Assignment(Builder b) {
        this.id = b.id;
        this.assignmentDate = b.assignmentDate;
        this.professorId = b.professorId;
        this.professorName = b.professorName;
        this.subjectGroupId = b.subjectGroupId;
        this.subjectCode = b.subjectCode;
        this.subjectName = b.subjectName;
        this.groupName = b.groupName;
        this.roomId = b.roomId;
        this.roomName = b.roomName;
        this.day = b.day;
        this.startTime = b.startTime;
        this.endTime = b.endTime;
        this.sessionType = b.sessionType;
        this.requiresLab = b.requiresLab;
        this.enrolledStudents = b.enrolledStudents;
        validateAssignment();
    }

    private void validateAssignment() {
        logger.debug("Validando Assignment id={}", id);
        Objects.requireNonNull(assignmentDate, "La fecha de asignación no puede ser null");
        Objects.requireNonNull(professorName, "El nombre del profesor no puede ser null");
        Objects.requireNonNull(startTime, "La hora de inicio no puede ser null");
        Objects.requireNonNull(endTime, "La hora de fin no puede ser null");
        if (endTime.isBefore(startTime)) {
            logger.error("Validación fallida en Assignment id={}: endTime {} es antes de startTime {}", id, endTime, startTime);
            throw new IllegalArgumentException("La hora de fin debe ser posterior a la de inicio");
        }
        logger.debug("Assignment id={} validado correctamente", id);
    }

    /**
     * Determina los conflictos con otra asignación.
     */
    public Map<String, List<ConflictEdge>> conflictsWith(Assignment other) {
        logger.debug("Calculando conflictos entre id={} y id={}", id, other.id);
        Map<String, List<ConflictEdge>> conflicts = new HashMap<>();
        if (!this.day.equals(other.day) || !overlaps(other)) {
            logger.debug("No hay solapamiento o distinto día entre id={} y id={}", id, other.id);
            return conflicts;
        }
        // Conflicto por profesor
        if (this.professorId == other.professorId) {
            logger.debug("Conflicto de profesor entre id={} y id={}", id, other.id);
            conflicts.computeIfAbsent("professor", k -> new ArrayList<>())
                     .add(new ConflictEdge("Solapamiento de horarios (mismo profesor)"));
        }
        // Conflicto por sala
        if (this.roomId == other.roomId) {
            logger.debug("Conflicto de sala entre id={} y id={}", id, other.id);
            conflicts.computeIfAbsent("room", k -> new ArrayList<>())
                     .add(new ConflictEdge("Solapamiento de horarios (misma sala)"));
        }
        // Conflicto por grupo
        if (this.subjectGroupId == other.subjectGroupId) {
            logger.debug("Conflicto de grupo entre id={} y id={}", id, other.id);
            conflicts.computeIfAbsent("group", k -> new ArrayList<>())
                     .add(new ConflictEdge("Solapamiento de horarios (mismo grupo)"));
        }
        // Conflicto por jornada
        if (this.sessionType.equals(other.sessionType)) {
            logger.debug("Conflicto de jornada entre id={} y id={}", id, other.id);
            conflicts.computeIfAbsent("sessionType", k -> new ArrayList<>())
                     .add(new ConflictEdge("Misma jornada y horario"));
        }
        logger.debug("Conflictos encontrados: {} para par id={} y id={}", conflicts.keySet(), id, other.id);
        return conflicts;
    }

    private boolean overlaps(Assignment other) {
        return !this.endTime.isBefore(other.startTime)
            && !this.startTime.isAfter(other.endTime);
    }

    // Getters
    public int getId() { return id; }
    public LocalDate getAssignmentDate() { return assignmentDate; }
    public int getProfessorId() { return professorId; }
    public String getProfessorName() { return professorName; }
    public int getSubjectGroupId() { return subjectGroupId; }
    public String getSubjectCode() { return subjectCode; }
    public String getSubjectName() { return subjectName; }
    public String getGroupName() { return groupName; }
    public int getRoomId() { return roomId; }
    public String getRoomName() { return roomName; }
    public String getDay() { return day; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public String getSessionType() { return sessionType; }
    public boolean isRequiresLab() { return requiresLab; }
    public int getEnrolledStudents() { return enrolledStudents; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Assignment)) return false;
        Assignment that = (Assignment) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Builder para Jackson y construcción de Assignment.
     */
    @JsonPOJOBuilder(withPrefix = "")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Builder {
        private int id;
        private LocalDate assignmentDate;
        private int professorId;
        private String professorName;
        private int subjectGroupId;
        private String subjectCode;
        private String subjectName;
        private String groupName;
        private int roomId;
        private String roomName;
        private String day;
        private LocalTime startTime;
        private LocalTime endTime;
        private String sessionType;
        private boolean requiresLab;
        private int enrolledStudents;

        public Builder id(int id) { this.id = id; return this; }
        public Builder assignmentDate(LocalDate d) { this.assignmentDate = d; return this; }
        public Builder professorId(int pid) { this.professorId = pid; return this; }
        public Builder professorName(String name) { this.professorName = name; return this; }
        public Builder subjectGroupId(int sgid) { this.subjectGroupId = sgid; return this; }
        public Builder subjectCode(String code) { this.subjectCode = code; return this; }
        public Builder subjectName(String name) { this.subjectName = name; return this; }
        public Builder groupName(String name) { this.groupName = name; return this; }
        public Builder roomId(int rid) { this.roomId = rid; return this; }
        public Builder roomName(String name) { this.roomName = name; return this; }
        public Builder day(String day) { this.day = day; return this; }
        public Builder startTime(LocalTime start) { this.startTime = start; return this; }
        public Builder endTime(LocalTime end) { this.endTime = end; return this; }
        public Builder sessionType(String type) { this.sessionType = type; return this; }
        public Builder requiresLab(boolean req) { this.requiresLab = req; return this; }
        public Builder enrolledStudents(int count) { this.enrolledStudents = count; return this; }
        public Assignment build() { return new Assignment(this); }
    }
}
