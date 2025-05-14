package com.example.miapp.util;

import com.example.miapp.domain.*;
import com.example.miapp.exception.AssignmentConflictException;
import com.example.miapp.exception.BlockedSlotConflictException;
import com.example.miapp.exception.DomainException;
import com.example.miapp.exception.IncompatibleRoomException;
import com.example.miapp.exception.RoomCapacityExceededException;
import com.example.miapp.repository.DataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;


/**
 * Clase utilitaria para facilitar la construcción de asignaciones.
 * 
 * Esta clase proporciona métodos de alto nivel que utilizan internamente 
 * el Builder de Assignment, pero añadiendo validaciones adicionales, 
 * resolución de entidades y funcionalidades avanzadas.
 * 
 * Mientras Assignment.Builder se centra en la construcción básica,
 * AssignmentBuilder implementa la lógica de negocio para crear
 * asignaciones válidas según las reglas del dominio.
 */
public class AssignmentBuilder {
    private static final Logger logger = LoggerFactory.getLogger(AssignmentBuilder.class);
    
    // DataManager para resolver referencias si es necesario
    private static final DataManager dataManager = DataManager.getInstance();
    
    /**
     * Crea una asignación completa con las entidades proporcionadas.
     * 
     * @param id ID de la asignación
     * @param professor Profesor que impartirá la clase
     * @param subject Materia a impartir
     * @param room Aula donde se impartirá
     * @param day Día de la semana
     * @param startTime Hora de inicio
     * @param endTime Hora de fin
     * @param groupId ID del grupo
     * @param groupName Nombre del grupo
     * @param sessionType Tipo de sesión (D=Diurno, N=Nocturno)
     * @param enrolledStudents Número de estudiantes inscritos
     * @param assignmentDate Fecha de la asignación
     * @return Asignación creada
     * @throws IllegalArgumentException si hay incompatibilidades entre los parámetros
     */
    public static Assignment createAssignment(
            int id, 
            Professor professor, 
            Subject subject,
            Room room,
            String day,
            LocalTime startTime,
            LocalTime endTime,
            int groupId,
            String groupName,
            String sessionType,
            int enrolledStudents,
            LocalDate assignmentDate) {
        
        // Validar compatibilidad de requisitos de laboratorio
        validateLabRequirements(subject, room);
        
        // Validar capacidad del aula
        validateRoomCapacity(room, enrolledStudents);
        
        // Validar que no haya conflicto con franjas bloqueadas del profesor
        validateNoBlockedSlotConflict(professor, day, startTime, endTime);
        
        // Crear la asignación
        Assignment assignment = new Assignment.Builder()
            .id(id)
            .assignmentDate(assignmentDate)
            .professor(professor)
            .subject(subject)
            .room(room)
            .day(day)
            .startTime(startTime)
            .endTime(endTime)
            .groupId(groupId)
            .groupName(groupName)
            .sessionType(sessionType)
            .enrolledStudents(enrolledStudents)
            .build();
        
        logger.debug("Asignación creada con éxito: id={}, profesor={}, materia={}, aula={}, día={}",
                   id, professor.getName(), subject.getCode(), room.getName(), day);
        
        return assignment;
    }
    
    /**
     * Crea una asignación a partir de IDs de entidades, obteniendo las referencias del DataManager.
     * 
     * @param id ID de la asignación
     * @param professorId ID del profesor
     * @param subjectCode Código de la materia
     * @param roomId ID del aula
     * @param day Día de la semana
     * @param startTime Hora de inicio
     * @param endTime Hora de fin
     * @param groupId ID del grupo
     * @param groupName Nombre del grupo
     * @param sessionType Tipo de sesión
     * @param enrolledStudents Número de estudiantes inscritos
     * @param assignmentDate Fecha de la asignación
     * @return Asignación creada
     * @throws IllegalArgumentException si alguna entidad no existe o hay incompatibilidades
     */
    public static Assignment createAssignmentFromIds(
            int id, 
            int professorId, 
            String subjectCode,
            int roomId,
            String day,
            LocalTime startTime,
            LocalTime endTime,
            int groupId,
            String groupName,
            String sessionType,
            int enrolledStudents,
            LocalDate assignmentDate) {
        
        // Obtener las entidades del DataManager
       // Obtener las entidades del DataManager
    Professor professor = dataManager.getProfessor(professorId);
    if (professor == null) {
        throw new DomainException("No existe un profesor con ID: " + professorId);
    }

    Subject subject = dataManager.getSubject(subjectCode);
    if (subject == null) {
        throw new DomainException("No existe una materia con código: " + subjectCode);
    }

    Room room = dataManager.getRoom(roomId);
    if (room == null) {
        throw new DomainException("No existe un aula con ID: " + roomId);
    }

    // Verificar que el profesor tenga asignada la materia
    boolean hasSubject = professor.getSubjects().stream()
        .anyMatch(s -> s.getCode().equals(subjectCode));

    if (!hasSubject) {
        throw new AssignmentConflictException(
            "El profesor " + professor.getName() + " no tiene asignada la materia " + subjectCode,
            0, // No hay asignación existente todavía
            0, // No hay segunda asignación implicada
            "PROFESSOR_SUBJECT_MISMATCH");
    }
        
        // Crear la asignación con las entidades obtenidas
        return createAssignment(
            id, professor, subject, room, day, startTime, endTime,
            groupId, groupName, sessionType, enrolledStudents, assignmentDate
        );
    }
    
    /**
     * Crea una copia de una asignación existente con posibles modificaciones.
     * 
     * @param original Asignación original a copiar
     * @param newId Nuevo ID para la copia (puede ser el mismo que el original)
     * @param modifyBuilder Función para modificar el builder antes de construir
     * @return Nueva asignación con las modificaciones aplicadas
     */
    public static Assignment copyAssignmentWithModifications(
            Assignment original, 
            int newId,
            java.util.function.Consumer<Assignment.Builder> modifyBuilder) {
        
        Objects.requireNonNull(original, "La asignación original no puede ser null");
        Objects.requireNonNull(modifyBuilder, "La función de modificación no puede ser null");
        
        // Crear un builder pre-configurado con los valores de la asignación original
        Assignment.Builder builder = original.toBuilder();
        
        // Aplicar el nuevo ID
        builder.id(newId);
        
        // Aplicar las modificaciones personalizadas
        modifyBuilder.accept(builder);
        
        // Construir la nueva asignación
        Assignment result = builder.build();
        
        logger.debug("Asignación copiada con modificaciones: original.id={}, nueva.id={}",
                   original.getId(), result.getId());
        
        return result;
    }
    
    /**
     * Crea una copia de una asignación con un nuevo día y horario.
     * 
     * @param original Asignación original a copiar
     * @param newId Nuevo ID para la copia
     * @param newDay Nuevo día para la copia
     * @param newStartTime Nueva hora de inicio
     * @param newEndTime Nueva hora de fin
     * @return Nueva asignación con el día y horario modificados
     */
    public static Assignment copyWithNewTimeSlot(
            Assignment original,
            int newId,
            String newDay,
            LocalTime newStartTime,
            LocalTime newEndTime) {
        
        // Verificar que no haya conflicto con franjas bloqueadas del profesor
        validateNoBlockedSlotConflict(original.getProfessor(), newDay, newStartTime, newEndTime);
        
        return copyAssignmentWithModifications(original, newId, builder -> {
            builder.day(newDay)
                   .startTime(newStartTime)
                   .endTime(newEndTime);
        });
    }
    
    /**
     * Crea una copia de una asignación con un nuevo aula.
     * 
     * @param original Asignación original a copiar
     * @param newId Nuevo ID para la copia
     * @param newRoom Nueva aula para la copia
     * @return Nueva asignación con el aula modificada
     */
    public static Assignment copyWithNewRoom(
            Assignment original,
            int newId,
            Room newRoom) {
        
        // Validar compatibilidad con requisitos de laboratorio
        if (original.getSubject() != null) {
            validateLabRequirements(original.getSubject(), newRoom);
        }
        
        // Validar capacidad del aula
        validateRoomCapacity(newRoom, original.getEnrolledStudents());
        
        return copyAssignmentWithModifications(original, newId, builder -> {
            builder.room(newRoom);
        });
    }
    
    /**
     * Crea una copia de una asignación con un nuevo profesor.
     * 
     * @param original Asignación original a copiar
     * @param newId Nuevo ID para la copia
     * @param newProfessor Nuevo profesor para la copia
     * @return Nueva asignación con el profesor modificado
     * @throws IllegalArgumentException si el profesor no tiene asignada la materia
     */
    public static Assignment copyWithNewProfessor(
            Assignment original,
            int newId,
            Professor newProfessor) {
        
        // Verificar que el profesor tenga asignada la materia si hay materia
        if (original.getSubject() != null) {
            boolean hasSubject = newProfessor.getSubjects().stream()
                .anyMatch(s -> s.getCode().equals(original.getSubject().getCode()));
    
            if (!hasSubject) {
                throw new AssignmentConflictException(
                    "El profesor " + newProfessor.getName() + 
                    " no tiene asignada la materia " + original.getSubject().getCode(),
                    original.getId(),
                0, // No hay segunda asignación implicada
                "PROFESSOR_SUBJECT_MISMATCH"); // Nuevo tipo de conflicto
            }
        }
        
        // Verificar que no haya conflicto con franjas bloqueadas del profesor
        validateNoBlockedSlotConflict(
            newProfessor, original.getDay(), original.getStartTime(), original.getEndTime());
        
        return copyAssignmentWithModifications(original, newId, builder -> {
            builder.professor(newProfessor);
        });
    }
    
    // ===== Métodos de validación privados =====
    
    /**
    * Valida la compatibilidad entre la materia y el aula respecto a requisitos de laboratorio.
    * @throws IncompatibleRoomException si la materia requiere laboratorio pero el aula no lo es
    */
    private static void validateLabRequirements(Subject subject, Room room) {
        if (subject.requiresLab() && !room.isLab()) {
            throw new IncompatibleRoomException(
                "La materia " + subject.getCode() + " requiere laboratorio, pero el aula " + 
                room.getName() + " no es un laboratorio",
                subject.getCode(),
                room.getId());
        }
    }
    
    /**
    * Valida que el aula tenga capacidad suficiente para los estudiantes.
    * @throws RoomCapacityExceededException si el número de estudiantes supera la capacidad
    */
    private static void validateRoomCapacity(Room room, int enrolledStudents) {
        if (enrolledStudents > room.getCapacity()) {
            throw new RoomCapacityExceededException(
                "El aula " + room.getName() + " tiene capacidad para " + room.getCapacity() + 
                " estudiantes, pero se intentan asignar " + enrolledStudents,
                room.getId(),
                room.getCapacity(),
                enrolledStudents);
        }
    }
    
    /**
    * Valida que no haya conflicto con franjas bloqueadas del profesor.
    * @throws BlockedSlotConflictException si hay conflicto con una franja bloqueada
    */
    private static void validateNoBlockedSlotConflict(
        Professor professor, String day, LocalTime startTime, LocalTime endTime) {
    
        if (professor.hasBlockedSlotConflict(day, startTime, endTime)) {
            throw new BlockedSlotConflictException(
                "El profesor " + professor.getName() + " tiene una franja bloqueada que " +
                "se solapa con el horario propuesto: " + day + " de " + startTime + " a " + endTime,
                professor.getId(),
                day,
                startTime.toString(),
                endTime.toString());
        }
    }
}