package com.example.miapp.service;

import com.example.miapp.domain.*;
import com.example.miapp.exception.DomainException;
import com.example.miapp.repository.DataManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Generador automático de asignaciones que deliberadamente crea conflictos.
 * Genera exactamente el número de asignaciones solicitado.
 */
public class AutoAssignmentGenerator {
    private static final Logger logger = Logger.getLogger(AutoAssignmentGenerator.class.getName());
    
    private final DataManager dataManager;
    private final ObjectMapper objectMapper;
    private final Random random;
    private final long seed;
    
    // Constantes para la generación
    private static final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
    private static final String[] SESSION_TYPES = {"D", "N"};
    
    /**
     * Constructor que inicializa el generador con una semilla aleatoria.
     */
    public AutoAssignmentGenerator() {
        this(System.currentTimeMillis());
    }
    
    /**
     * Constructor que permite especificar una semilla para la generación.
     * 
     * @param seed Semilla para el generador aleatorio
     */
    public AutoAssignmentGenerator(long seed) {
        this.dataManager = DataManager.getInstance();
        this.seed = seed;
        this.random = new Random(seed);
        
        this.objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        logger.info("AutoAssignmentGenerator inicializado con semilla: " + seed);
    }
    
    /**
     * Genera un número específico de asignaciones con conflictos deliberados.
     * 
     * @param count Número exacto de asignaciones a generar
     * @return Representación JSON de las asignaciones generadas
     * @throws IllegalArgumentException si count es negativo o mayor que 1000
     * @throws IllegalStateException si no hay suficientes datos para generar asignaciones
     */
    public String generateAssignments(int count) {
        if (count < 1) {
            throw new IllegalArgumentException("El número de asignaciones debe ser al menos 1");
        }
        
        if (count > 1000) {
            throw new IllegalArgumentException("Se supera el límite máximo de 1000 asignaciones");
        }
        
        // Verificar que haya datos suficientes
        List<Professor> professors = dataManager.getAllProfessors();
        List<Room> rooms = dataManager.getAllRooms();
        List<Subject> subjects = dataManager.getAllSubjects();
        
        if (professors.isEmpty()) {
            throw new IllegalStateException("No hay profesores disponibles");
        }
        
        if (rooms.isEmpty()) {
            throw new IllegalStateException("No hay aulas disponibles");
        }
        
        if (subjects.isEmpty()) {
            throw new IllegalStateException("No hay materias disponibles");
        }
        
        // Lista para almacenar las asignaciones generadas
        List<Assignment> generatedAssignments = new ArrayList<>(count);
        
        // Generar asignaciones individuales para garantizar el número exacto
        for (int i = 0; i < count; i++) {
            Assignment assignment = generateConflictingAssignment(i + 1, professors, rooms, subjects);
            if (assignment != null) {
                generatedAssignments.add(assignment);
                logger.fine("Generada asignación #" + (i + 1));
            }
        }
        
        // Verificar que hemos generado exactamente el número solicitado
        if (generatedAssignments.size() != count) {
            logger.warning("Se generaron " + generatedAssignments.size() + 
                         " asignaciones en lugar de " + count + " solicitadas");
        }
        
        logger.info("Generación completada: " + generatedAssignments.size() + " asignaciones generadas");
        
        // Convertir las asignaciones a formato JSON
        return convertAssignmentsToJson(generatedAssignments);
    }
    
    /**
     * Genera una única asignación con potencial de conflicto.
     * 
     * @param id ID para la asignación
     * @param professors Lista de profesores disponibles
     * @param rooms Lista de aulas disponibles
     * @param subjects Lista de materias disponibles
     * @return Una nueva asignación o null si no se pudo generar
     */
    private Assignment generateConflictingAssignment(int id, 
                                                 List<Professor> professors,
                                                 List<Room> rooms,
                                                 List<Subject> subjects) {
        try {
            // Elegir profesor y materia
            Professor professor = professors.get(id % professors.size());
            
            // Obtener materias asignadas al profesor
            List<Subject> professorSubjects = professor.getSubjects();
            if (professorSubjects.isEmpty()) {
                // Si el profesor no tiene materias, asignar cualquiera
                Subject randomSubject = subjects.get(random.nextInt(subjects.size()));
                professor.assignSubject(randomSubject);
                professorSubjects = professor.getSubjects();
            }
            
            Subject subject = professorSubjects.get(id % professorSubjects.size());
            
            // Elegir aula
            Room room;
            if (subject.requiresLab()) {
                // Si la materia requiere laboratorio, buscar uno
                List<Room> labs = rooms.stream()
                    .filter(Room::isLab)
                    .collect(Collectors.toList());
                
                if (labs.isEmpty()) {
                    // Si no hay laboratorios, usar cualquier aula (generará conflicto)
                    room = rooms.get(id % rooms.size());
                } else {
                    room = labs.get(id % labs.size());
                }
            } else {
                room = rooms.get(id % rooms.size());
            }
            
            // Calcular horario y día
            String day = DAYS[id % DAYS.length];
            
            // Para crear conflictos, usamos horarios fijos según el índice
            LocalTime startTime, endTime;
            int timePattern = (id / DAYS.length) % 4; // 4 patrones de tiempo diferentes
            
            switch (timePattern) {
                case 0:
                    startTime = LocalTime.of(8, 0);
                    endTime = LocalTime.of(10, 0);
                    break;
                case 1:
                    startTime = LocalTime.of(10, 0);
                    endTime = LocalTime.of(12, 0);
                    break;
                case 2:
                    startTime = LocalTime.of(14, 0);
                    endTime = LocalTime.of(16, 0);
                    break;
                default:
                    startTime = LocalTime.of(16, 0);
                    endTime = LocalTime.of(18, 0);
                    break;
            }
            
            // Datos del grupo
            int groupId = 100 + id;
            String groupName = "Grupo " + (char)('A' + (id % 26));
            String sessionType = SESSION_TYPES[id % SESSION_TYPES.length];
            
            // Estudiantes (potencialmente excediendo capacidad del aula)
            int enrolledStudents = Math.min(100, room.getCapacity() + (id % 30) - 15);
            
            // Crear la asignación
            return new Assignment.Builder()
                    .id(id)
                    .assignmentDate(LocalDate.now())
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
                    
        } catch (DomainException e) {
            logger.log(Level.WARNING, "Error al generar asignación #" + id + ": " + e.getMessage(), e);
            
            // En caso de error, crear una asignación simple que no cause excepción
            try {
                Professor professor = professors.get(0);
                Subject subject = subjects.get(0);
                Room room = rooms.get(0);
                
                return new Assignment.Builder()
                        .id(id)
                        .assignmentDate(LocalDate.now())
                        .professor(professor)
                        .subject(subject)
                        .room(room)
                        .day("Monday")
                        .startTime(LocalTime.of(8, 0))
                        .endTime(LocalTime.of(10, 0))
                        .groupId(id)
                        .groupName("Grupo Fallback " + id)
                        .sessionType("D")
                        .enrolledStudents(20)
                        .build();
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Error crítico al crear asignación de respaldo: " + ex.getMessage(), ex);
                return null;
            }
        }
    }
    
    /**
     * Convierte una lista de asignaciones a formato JSON.
     * 
     * @param assignments Lista de asignaciones
     * @return Cadena en formato JSON
     */
    private String convertAssignmentsToJson(List<Assignment> assignments) {
        try {
            // Crear el objeto raíz
            ObjectNode rootNode = objectMapper.createObjectNode();
            
            // Crear array de asignaciones
            ArrayNode assignmentsArray = objectMapper.createArrayNode();
            
            // Añadir cada asignación al array
            for (Assignment assignment : assignments) {
                ObjectNode assignmentNode = objectMapper.createObjectNode();
                
                // Datos básicos
                assignmentNode.put("id", assignment.getId());
                assignmentNode.put("assignmentDate", assignment.getAssignmentDate().toString());
                assignmentNode.put("day", assignment.getDay());
                assignmentNode.put("startTime", assignment.getStartTime().toString());
                assignmentNode.put("endTime", assignment.getEndTime().toString());
                assignmentNode.put("groupId", assignment.getGroupId());
                assignmentNode.put("groupName", assignment.getGroupName());
                assignmentNode.put("sessionType", assignment.getSessionType());
                assignmentNode.put("enrolledStudents", assignment.getEnrolledStudents());
                
                // Profesor anidado
                ObjectNode professorNode = objectMapper.createObjectNode();
                Professor professor = assignment.getProfessor();
                professorNode.put("id", professor.getId());
                professorNode.put("name", professor.getName());
                professorNode.put("department", professor.getDepartment());
                professorNode.put("email", professor.getEmail());
                
                // Materias del profesor
                ArrayNode subjectsArray = objectMapper.createArrayNode();
                for (Subject subject : professor.getSubjects()) {
                    ObjectNode subjectNode = objectMapper.createObjectNode();
                    subjectNode.put("code", subject.getCode());
                    subjectNode.put("name", subject.getName());
                    subjectNode.put("description", subject.getDescription());
                    subjectNode.put("credits", subject.getCredits());
                    subjectNode.put("requiresLab", subject.requiresLab());
                    subjectsArray.add(subjectNode);
                }
                professorNode.set("subjects", subjectsArray);
                
                // Franjas bloqueadas del profesor
                ArrayNode blockedSlotsArray = objectMapper.createArrayNode();
                for (BlockedSlot slot : professor.getBlockedSlots()) {
                    ObjectNode slotNode = objectMapper.createObjectNode();
                    slotNode.put("day", slot.getDay());
                    slotNode.put("startTime", slot.getStartTime().toString());
                    slotNode.put("endTime", slot.getEndTime().toString());
                    blockedSlotsArray.add(slotNode);
                }
                professorNode.set("blockedSlots", blockedSlotsArray);
                
                assignmentNode.set("professor", professorNode);
                
                // Aula anidada
                ObjectNode roomNode = objectMapper.createObjectNode();
                Room room = assignment.getRoom();
                roomNode.put("id", room.getId());
                roomNode.put("name", room.getName());
                roomNode.put("capacity", room.getCapacity());
                roomNode.put("isLab", room.isLab());
                assignmentNode.set("room", roomNode);
                
                // Materia anidada
                if (assignment.getSubject() != null) {
                    ObjectNode subjectNode = objectMapper.createObjectNode();
                    Subject subject = assignment.getSubject();
                    subjectNode.put("code", subject.getCode());
                    subjectNode.put("name", subject.getName());
                    subjectNode.put("description", subject.getDescription());
                    subjectNode.put("credits", subject.getCredits());
                    subjectNode.put("requiresLab", subject.requiresLab());
                    assignmentNode.set("subject", subjectNode);
                }
                
                // Añadir al array
                assignmentsArray.add(assignmentNode);
            }
            
            // Añadir el array al objeto raíz
            rootNode.set("assignments", assignmentsArray);
            
            // Convertir a cadena JSON formateada
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error al convertir asignaciones a JSON: " + e.getMessage(), e);
            return "{ \"error\": \"" + e.getMessage() + "\" }";
        }
    }
    
    /**
     * Obtiene la semilla utilizada por este generador.
     * 
     * @return Semilla del generador aleatorio
     */
    public long getSeed() {
        return seed;
    }
}