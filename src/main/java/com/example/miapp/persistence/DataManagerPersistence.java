package com.example.miapp.persistence;

import com.example.miapp.domain.*;
import com.example.miapp.exception.DomainException;
import com.example.miapp.repository.DataManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Clase encargada de la persistencia del DataManager.
 * Proporciona métodos para guardar y cargar el estado completo
 * del sistema a/desde archivos JSON.
 */
public class DataManagerPersistence {
    private static final Logger logger = LoggerFactory.getLogger(DataManagerPersistence.class);

    private final ObjectMapper mapper;
    private final DataManager dataManager;

    /**
     * Constructor que inicializa el ObjectMapper con los módulos necesarios.
     */
    public DataManagerPersistence() {
        this.dataManager = DataManager.getInstance();
        this.mapper = new ObjectMapper();
        
        // Configurar mapper para manejar tipos de fecha/hora de Java 8
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        logger.info("DataManagerPersistence inicializado");
    }

    /**
     * Guarda el estado completo del DataManager en un archivo JSON.
     * 
     * @param filePath Ruta del archivo donde se guardará el estado
     * @throws IOException Si hay error en la escritura del archivo
     * @throws NullPointerException Si filePath es null
     */
    public void saveToJson(String filePath) throws IOException {
        Objects.requireNonNull(filePath, "La ruta del archivo no puede ser null");
        
        logger.info("Iniciando exportación del estado a JSON: {}", filePath);
        
        // Crear el directorio si no existe
        Path path = Paths.get(filePath);
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
            logger.debug("Directorio creado: {}", parent);
        }
        
        // Crear el nodo raíz que contendrá todas las colecciones
        ObjectNode rootNode = mapper.createObjectNode();
        
        // Serializar profesores
        rootNode.set("professors", serializeProfessors());
        
        // Serializar materias
        rootNode.set("subjects", serializeSubjects());
        
        // Serializar aulas
        rootNode.set("rooms", serializeRooms());
        
        // Serializar asignaciones
        rootNode.set("assignments", serializeAssignments());
        
        // Escribir el JSON en el archivo
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(filePath), rootNode);
        
        logger.info("Exportación completada: {} profesores, {} materias, {} aulas, {} asignaciones",
                  dataManager.getAllProfessors().size(),
                  dataManager.getAllSubjects().size(),
                  dataManager.getAllRooms().size(),
                  dataManager.getAllAssignments().size());
    }
    
    /**
     * Serializa la colección de profesores a un array JSON.
     */
    private ArrayNode serializeProfessors() {
        ArrayNode professorsArray = mapper.createArrayNode();
        
        for (Professor professor : dataManager.getAllProfessors()) {
            ObjectNode profNode = mapper.createObjectNode();
            
            // Datos básicos del profesor
            profNode.put("id", professor.getId());
            profNode.put("name", professor.getName());
            profNode.put("department", professor.getDepartment());
            profNode.put("email", professor.getEmail());
            
            // Materias asignadas (solo códigos)
            ArrayNode subjectsArray = mapper.createArrayNode();
            for (Subject subject : professor.getSubjects()) {
                subjectsArray.add(subject.getCode());
            }
            profNode.set("subjects", subjectsArray);
            
            // Franjas bloqueadas
            ArrayNode blockedSlotsArray = mapper.createArrayNode();
            for (BlockedSlot slot : professor.getBlockedSlots()) {
                ObjectNode slotNode = mapper.createObjectNode();
                slotNode.put("day", slot.getDay());
                slotNode.put("startTime", slot.getStartTime().toString());
                slotNode.put("endTime", slot.getEndTime().toString());
                blockedSlotsArray.add(slotNode);
            }
            profNode.set("blockedSlots", blockedSlotsArray);
            
            professorsArray.add(profNode);
            logger.trace("Profesor serializado: id={}", professor.getId());
        }
        
        logger.debug("Serializados {} profesores", professorsArray.size());
        return professorsArray;
    }
    
    /**
     * Serializa la colección de materias a un array JSON.
     */
    private ArrayNode serializeSubjects() {
        ArrayNode subjectsArray = mapper.createArrayNode();
        
        for (Subject subject : dataManager.getAllSubjects()) {
            ObjectNode subjectNode = mapper.createObjectNode();
            
            // Datos de la materia
            subjectNode.put("code", subject.getCode());
            subjectNode.put("name", subject.getName());
            subjectNode.put("description", subject.getDescription());
            subjectNode.put("credits", subject.getCredits());
            subjectNode.put("requiresLab", subject.requiresLab());
            
            subjectsArray.add(subjectNode);
            logger.trace("Materia serializada: código={}", subject.getCode());
        }
        
        logger.debug("Serializadas {} materias", subjectsArray.size());
        return subjectsArray;
    }
    
    /**
     * Serializa la colección de aulas a un array JSON.
     */
    private ArrayNode serializeRooms() {
        ArrayNode roomsArray = mapper.createArrayNode();
        
        for (Room room : dataManager.getAllRooms()) {
            ObjectNode roomNode = mapper.createObjectNode();
            
            // Datos del aula
            roomNode.put("id", room.getId());
            roomNode.put("name", room.getName());
            roomNode.put("capacity", room.getCapacity());
            roomNode.put("isLab", room.isLab());
            
            roomsArray.add(roomNode);
            logger.trace("Aula serializada: id={}", room.getId());
        }
        
        logger.debug("Serializadas {} aulas", roomsArray.size());
        return roomsArray;
    }
    
    /**
     * Serializa la colección de asignaciones a un array JSON.
     */
    private ArrayNode serializeAssignments() {
        ArrayNode assignmentsArray = mapper.createArrayNode();
        
        for (Assignment assignment : dataManager.getAllAssignments()) {
            ObjectNode assignNode = mapper.createObjectNode();
            
            // Datos básicos de la asignación
            assignNode.put("id", assignment.getId());
            assignNode.put("assignmentDate", assignment.getAssignmentDate().toString());
            assignNode.put("day", assignment.getDay());
            assignNode.put("startTime", assignment.getStartTime().toString());
            assignNode.put("endTime", assignment.getEndTime().toString());
            
            // Referencias
            assignNode.put("professorId", assignment.getProfessorId());
            assignNode.put("roomId", assignment.getRoomId());
            if (assignment.getSubject() != null) {
                assignNode.put("subjectCode", assignment.getSubject().getCode());
            }
            
            // Información del grupo
            assignNode.put("groupId", assignment.getGroupId());
            assignNode.put("groupName", assignment.getGroupName());
            
            // Información adicional
            assignNode.put("sessionType", assignment.getSessionType());
            assignNode.put("enrolledStudents", assignment.getEnrolledStudents());
            
            assignmentsArray.add(assignNode);
            logger.trace("Asignación serializada: id={}", assignment.getId());
        }
        
        logger.debug("Serializadas {} asignaciones", assignmentsArray.size());
        return assignmentsArray;
    }
    
    /**
     * Carga el estado completo del DataManager desde un archivo JSON.
     * Reemplaza todo el estado actual.
     * 
     * @param filePath Ruta del archivo a cargar
     * @throws IOException Si hay error en la lectura del archivo
     * @throws DomainException Si hay error en la validación de los datos cargados
     * @throws NullPointerException Si filePath es null
     */
    public void loadFromJson(String filePath) throws IOException {
        Objects.requireNonNull(filePath, "La ruta del archivo no puede ser null");
        
        logger.info("Iniciando carga del estado desde JSON: {}", filePath);
        
        // Verificar que el archivo existe
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IOException("El archivo no existe: " + filePath);
        }
        
        // Leer el JSON
        ObjectNode rootNode = (ObjectNode) mapper.readTree(new File(filePath));
        
        // Limpiar el estado actual
        dataManager.clearAll();
        
        try {
            // Cargar materias primero (ya que profesores y asignaciones dependen de ellas)
            loadSubjects(rootNode);
            
            // Cargar aulas (asignaciones dependen de ellas)
            loadRooms(rootNode);
            
            // Cargar profesores (asignaciones dependen de ellos)
            loadProfessors(rootNode);
            
            // Finalmente, cargar asignaciones
            loadAssignments(rootNode);
            
            logger.info("Carga completada: {} profesores, {} materias, {} aulas, {} asignaciones",
                      dataManager.getAllProfessors().size(),
                      dataManager.getAllSubjects().size(),
                      dataManager.getAllRooms().size(),
                      dataManager.getAllAssignments().size());
        } catch (Exception e) {
            // En caso de error, limpiar el estado para evitar inconsistencias
            dataManager.clearAll();
            
            // Relanzar la excepción con contexto
            throw new DomainException("Error al cargar los datos desde JSON: " + e.getMessage(), e);
        }
    }
    
    /**
     * Carga las materias desde el nodo JSON.
     */
    private void loadSubjects(ObjectNode rootNode) {
        if (!rootNode.has("subjects")) {
            logger.warn("El JSON no contiene la colección 'subjects'");
            return;
        }
        
        ArrayNode subjectsArray = (ArrayNode) rootNode.get("subjects");
        
        for (int i = 0; i < subjectsArray.size(); i++) {
            ObjectNode subjectNode = (ObjectNode) subjectsArray.get(i);
            
            String code = subjectNode.get("code").asText();
            String name = subjectNode.get("name").asText();
            String description = subjectNode.get("description").asText();
            int credits = subjectNode.get("credits").asInt();
            boolean requiresLab = subjectNode.get("requiresLab").asBoolean();
            
            Subject subject = new Subject(code, name, description, credits, requiresLab);
            dataManager.addSubject(subject);
            
            logger.trace("Materia cargada: código={}", code);
        }
        
        logger.debug("Cargadas {} materias", subjectsArray.size());
    }
    
    /**
     * Carga las aulas desde el nodo JSON.
     */
    private void loadRooms(ObjectNode rootNode) {
        if (!rootNode.has("rooms")) {
            logger.warn("El JSON no contiene la colección 'rooms'");
            return;
        }
        
        ArrayNode roomsArray = (ArrayNode) rootNode.get("rooms");
        
        for (int i = 0; i < roomsArray.size(); i++) {
            ObjectNode roomNode = (ObjectNode) roomsArray.get(i);
            
            int id = roomNode.get("id").asInt();
            String name = roomNode.get("name").asText();
            int capacity = roomNode.get("capacity").asInt();
            boolean isLab = roomNode.get("isLab").asBoolean();
            
            Room room = new Room(id, name, capacity, isLab);
            dataManager.addRoom(room);
            
            logger.trace("Aula cargada: id={}", id);
        }
        
        logger.debug("Cargadas {} aulas", roomsArray.size());
    }
    
    /**
     * Carga los profesores desde el nodo JSON.
     */
    private void loadProfessors(ObjectNode rootNode) {
        if (!rootNode.has("professors")) {
            logger.warn("El JSON no contiene la colección 'professors'");
            return;
        }
        
        ArrayNode professorsArray = (ArrayNode) rootNode.get("professors");
        
        for (int i = 0; i < professorsArray.size(); i++) {
            ObjectNode profNode = (ObjectNode) professorsArray.get(i);
            
            int id = profNode.get("id").asInt();
            String name = profNode.get("name").asText();
            String department = profNode.get("department").asText();
            String email = profNode.get("email").asText();
            
            Professor professor = new Professor(id, name, department, email);
            
            // Asignar materias
            if (profNode.has("subjects")) {
                ArrayNode subjectsArray = (ArrayNode) profNode.get("subjects");
                for (int j = 0; j < subjectsArray.size(); j++) {
                    String subjectCode = subjectsArray.get(j).asText();
                    Subject subject = dataManager.getSubject(subjectCode);
                    if (subject != null) {
                        professor.assignSubject(subject);
                    } else {
                        logger.warn("Materia no encontrada: {}", subjectCode);
                    }
                }
            }
            
            // Añadir franjas bloqueadas
            if (profNode.has("blockedSlots")) {
                ArrayNode slotsArray = (ArrayNode) profNode.get("blockedSlots");
                for (int j = 0; j < slotsArray.size(); j++) {
                    ObjectNode slotNode = (ObjectNode) slotsArray.get(j);
                    
                    String day = slotNode.get("day").asText();
                    LocalTime startTime = LocalTime.parse(slotNode.get("startTime").asText());
                    LocalTime endTime = LocalTime.parse(slotNode.get("endTime").asText());
                    
                    professor.addBlockedSlot(day, startTime, endTime);
                }
            }
            
            dataManager.addProfessor(professor);
            logger.trace("Profesor cargado: id={}", id);
        }
        
        logger.debug("Cargados {} profesores", professorsArray.size());
    }
    
    /**
     * Carga las asignaciones desde el nodo JSON.
     */
    private void loadAssignments(ObjectNode rootNode) {
        if (!rootNode.has("assignments")) {
            logger.warn("El JSON no contiene la colección 'assignments'");
            return;
        }
        
        ArrayNode assignmentsArray = (ArrayNode) rootNode.get("assignments");
        List<Assignment> assignments = new ArrayList<>();
        
        for (int i = 0; i < assignmentsArray.size(); i++) {
            ObjectNode assignNode = (ObjectNode) assignmentsArray.get(i);
            
            try {
                int id = assignNode.get("id").asInt();
                LocalDate assignmentDate = LocalDate.parse(assignNode.get("assignmentDate").asText());
                String day = assignNode.get("day").asText();
                LocalTime startTime = LocalTime.parse(assignNode.get("startTime").asText());
                LocalTime endTime = LocalTime.parse(assignNode.get("endTime").asText());
                
                int professorId = assignNode.get("professorId").asInt();
                int roomId = assignNode.get("roomId").asInt();
                
                Professor professor = dataManager.getProfessor(professorId);
                if (professor == null) {
                    throw new DomainException("Profesor no encontrado: " + professorId);
                }
                
                Room room = dataManager.getRoom(roomId);
                if (room == null) {
                    throw new DomainException("Aula no encontrada: " + roomId);
                }
                
                Subject subject = null;
                if (assignNode.has("subjectCode") && !assignNode.get("subjectCode").isNull()) {
                    String subjectCode = assignNode.get("subjectCode").asText();
                    subject = dataManager.getSubject(subjectCode);
                    if (subject == null) {
                        throw new DomainException("Materia no encontrada: " + subjectCode);
                    }
                }
                
                int groupId = assignNode.get("groupId").asInt();
                String groupName = assignNode.get("groupName").asText();
                String sessionType = assignNode.get("sessionType").asText();
                int enrolledStudents = assignNode.get("enrolledStudents").asInt();
                
                // Crear la asignación
                Assignment.Builder builder = new Assignment.Builder()
                    .id(id)
                    .assignmentDate(assignmentDate)
                    .professor(professor)
                    .room(room)
                    .subject(subject)
                    .day(day)
                    .startTime(startTime)
                    .endTime(endTime)
                    .groupId(groupId)
                    .groupName(groupName)
                    .sessionType(sessionType)
                    .enrolledStudents(enrolledStudents);
                
                Assignment assignment = builder.build();
                assignments.add(assignment);
                
                logger.trace("Asignación cargada: id={}", id);
            } catch (Exception e) {
                // Loguear y continuar con la siguiente asignación
                logger.error("Error al cargar la asignación #{}: {}", i, e.getMessage(), e);
            }
        }
        
        // Añadir todas las asignaciones cargadas correctamente
        for (Assignment assignment : assignments) {
            dataManager.addAssignment(assignment);
        }
        
        logger.debug("Cargadas {} de {} asignaciones", assignments.size(), assignmentsArray.size());
    }

    /**
 * Guarda las asignaciones en formato JSON anidado.
 * 
 * @param filePath Ruta del archivo donde se guardará
 * @throws IOException Si hay error en la escritura del archivo
 */
public void saveToNestedJson(String filePath) throws IOException {
    Objects.requireNonNull(filePath, "La ruta del archivo no puede ser null");
    
    logger.info("Iniciando exportación a formato JSON anidado: {}", filePath);
    
    // Crear el directorio si no existe
    Path path = Paths.get(filePath);
    Path parent = path.getParent();
    if (parent != null && !Files.exists(parent)) {
        Files.createDirectories(parent);
    }
    
    // Crear el nodo raíz
    ObjectNode rootNode = mapper.createObjectNode();
    
    // Crear array de asignaciones anidadas
    ArrayNode assignmentsArray = mapper.createArrayNode();
    
    // Para cada asignación, crear un nodo con datos anidados
    for (Assignment assignment : dataManager.getAllAssignments()) {
        ObjectNode assignmentNode = mapper.createObjectNode();
        
        // Datos básicos de asignación
        assignmentNode.put("id", assignment.getId());
        assignmentNode.put("assignmentDate", assignment.getAssignmentDate().toString());
        assignmentNode.put("day", assignment.getDay());
        assignmentNode.put("startTime", assignment.getStartTime().toString());
        assignmentNode.put("endTime", assignment.getEndTime().toString());
        assignmentNode.put("groupId", assignment.getGroupId());
        assignmentNode.put("groupName", assignment.getGroupName());
        assignmentNode.put("sessionType", assignment.getSessionType());
        assignmentNode.put("enrolledStudents", assignment.getEnrolledStudents());
        
        // Añadir objeto profesor completo
        Professor professor = assignment.getProfessor();
        assignmentNode.set("professor", createProfessorObjectNode(professor));
        
        // Añadir objeto aula completo
        Room room = assignment.getRoom();
        assignmentNode.set("room", createRoomObjectNode(room));
        
        // Añadir objeto materia completo si existe
        Subject subject = assignment.getSubject();
        if (subject != null) {
            assignmentNode.set("subject", createSubjectObjectNode(subject));
        }
        
        // Añadir la asignación al array
        assignmentsArray.add(assignmentNode);
    }
    
    // Añadir array de asignaciones al nodo raíz
    rootNode.set("assignments", assignmentsArray);
    
    // Escribir el JSON en el archivo
    mapper.writerWithDefaultPrettyPrinter().writeValue(new File(filePath), rootNode);
    
    logger.info("Exportación en formato anidado completada: {} asignaciones", 
              assignmentsArray.size());
}

/**
 * Crea un nodo JSON para un profesor con todas sus propiedades anidadas.
 */
private ObjectNode createProfessorObjectNode(Professor professor) {
    ObjectNode profNode = mapper.createObjectNode();
    
    // Datos básicos del profesor
    profNode.put("id", professor.getId());
    profNode.put("name", professor.getName());
    profNode.put("department", professor.getDepartment());
    profNode.put("email", professor.getEmail());
    
    // Materias asignadas como objetos completos
    ArrayNode subjectsArray = mapper.createArrayNode();
    for (Subject subject : professor.getSubjects()) {
        subjectsArray.add(createSubjectObjectNode(subject));
    }
    profNode.set("subjects", subjectsArray);
    
    // Franjas bloqueadas
    ArrayNode blockedSlotsArray = mapper.createArrayNode();
    for (BlockedSlot slot : professor.getBlockedSlots()) {
        ObjectNode slotNode = mapper.createObjectNode();
        slotNode.put("day", slot.getDay());
        slotNode.put("startTime", slot.getStartTime().toString());
        slotNode.put("endTime", slot.getEndTime().toString());
        blockedSlotsArray.add(slotNode);
    }
    profNode.set("blockedSlots", blockedSlotsArray);
    
    return profNode;
}

/**
 * Crea un nodo JSON para una materia.
 */
private ObjectNode createSubjectObjectNode(Subject subject) {
    ObjectNode subjectNode = mapper.createObjectNode();
    
    subjectNode.put("code", subject.getCode());
    subjectNode.put("name", subject.getName());
    subjectNode.put("description", subject.getDescription());
    subjectNode.put("credits", subject.getCredits());
    subjectNode.put("requiresLab", subject.requiresLab());
    
    return subjectNode;
}

/**
 * Crea un nodo JSON para un aula.
 */
private ObjectNode createRoomObjectNode(Room room) {
    ObjectNode roomNode = mapper.createObjectNode();
    
    roomNode.put("id", room.getId());
    roomNode.put("name", room.getName());
    roomNode.put("capacity", room.getCapacity());
    roomNode.put("isLab", room.isLab());
    
    return roomNode;
}

/**
 * Carga asignaciones desde un JSON en formato anidado.
 * 
 * @param filePath Ruta del archivo a cargar
 * @throws IOException Si hay error en la lectura del archivo
 */
public void loadFromNestedJson(String filePath) throws IOException {
    Objects.requireNonNull(filePath, "La ruta del archivo no puede ser null");
    
    logger.info("Iniciando carga desde JSON anidado: {}", filePath);
    
    // Verificar que el archivo existe
    Path path = Paths.get(filePath);
    if (!Files.exists(path)) {
        throw new IOException("El archivo no existe: " + filePath);
    }
    
    // Leer el JSON
    ObjectNode rootNode = (ObjectNode) mapper.readTree(new File(filePath));
    
    try {
        // Limpiar el estado actual
        dataManager.clearAll();
        
        // Mapas para rastrear entidades ya procesadas
        Map<Integer, Professor> professorsMap = new HashMap<>();
        Map<String, Subject> subjectsMap = new HashMap<>();
        Map<Integer, Room> roomsMap = new HashMap<>();
        
        // Procesar asignaciones
        JsonNode assignmentsNode = rootNode.get("assignments");
        if (assignmentsNode != null && assignmentsNode.isArray()) {
            for (JsonNode assignmentNode : assignmentsNode) {
                processNestedAssignment(assignmentNode, professorsMap, subjectsMap, roomsMap);
            }
        }
        
        logger.info("Carga desde JSON anidado completada: {} profesores, {} materias, {} aulas, {} asignaciones",
                  professorsMap.size(), subjectsMap.size(), roomsMap.size(), dataManager.getAllAssignments().size());
                  
    } catch (Exception e) {
        // En caso de error, limpiar el estado para evitar inconsistencias
        dataManager.clearAll();
        
        // Relanzar la excepción con contexto
        throw new IOException("Error al cargar los datos desde JSON anidado: " + e.getMessage(), e);
    }
}

/**
 * Procesa una asignación en formato anidado, extrayendo y registrando sus entidades.
 */
private void processNestedAssignment(JsonNode assignmentNode, 
                                   Map<Integer, Professor> professorsMap,
                                   Map<String, Subject> subjectsMap,
                                   Map<Integer, Room> roomsMap) throws IOException {
    
    // Procesar profesor
    JsonNode professorNode = assignmentNode.get("professor");
    int professorId = professorNode.get("id").asInt();
    
    Professor professor;
    if (professorsMap.containsKey(professorId)) {
        professor = professorsMap.get(professorId);
    } else {
        professor = processProfessor(professorNode, subjectsMap);
        professorsMap.put(professorId, professor);
        dataManager.addProfessor(professor);
    }
    
    // Procesar aula
    JsonNode roomNode = assignmentNode.get("room");
    int roomId = roomNode.get("id").asInt();
    
    Room room;
    if (roomsMap.containsKey(roomId)) {
        room = roomsMap.get(roomId);
    } else {
        room = processRoom(roomNode);
        roomsMap.put(roomId, room);
        dataManager.addRoom(room);
    }
    
    // Procesar materia
    JsonNode subjectNode = assignmentNode.get("subject");
    Subject subject = null;
    
    if (subjectNode != null) {
        String subjectCode = subjectNode.get("code").asText();
        
        if (subjectsMap.containsKey(subjectCode)) {
            subject = subjectsMap.get(subjectCode);
        } else {
            subject = processSubject(subjectNode);
            subjectsMap.put(subject.getCode(), subject);
            dataManager.addSubject(subject);
        }
    }
    
    // Construir la asignación
    try {
        Assignment.Builder builder = new Assignment.Builder()
            .id(assignmentNode.get("id").asInt())
            .assignmentDate(LocalDate.parse(assignmentNode.get("assignmentDate").asText()))
            .professor(professor)
            .room(room)
            .subject(subject)
            .day(assignmentNode.get("day").asText())
            .startTime(LocalTime.parse(assignmentNode.get("startTime").asText()))
            .endTime(LocalTime.parse(assignmentNode.get("endTime").asText()))
            .groupId(assignmentNode.get("groupId").asInt())
            .groupName(assignmentNode.get("groupName").asText())
            .sessionType(assignmentNode.get("sessionType").asText())
            .enrolledStudents(assignmentNode.get("enrolledStudents").asInt());
            
        Assignment assignment = builder.build();
        dataManager.addAssignment(assignment);
        
    } catch (Exception e) {
        logger.error("Error al construir asignación: {}", e.getMessage());
        throw new IOException("Error en formato de asignación: " + e.getMessage(), e);
    }
}

/**
 * Procesa un nodo de profesor.
 */
private Professor processProfessor(JsonNode professorNode, Map<String, Subject> subjectsMap) throws IOException {
    int id = professorNode.get("id").asInt();
    String name = professorNode.get("name").asText();
    String department = professorNode.get("department").asText();
    String email = professorNode.get("email").asText();
    
    Professor professor = new Professor(id, name, department, email);
    
    // Procesar materias del profesor
    if (professorNode.has("subjects") && professorNode.get("subjects").isArray()) {
        for (JsonNode subjectNode : professorNode.get("subjects")) {
            String subjectCode = subjectNode.get("code").asText();
            
            Subject subject;
            if (subjectsMap.containsKey(subjectCode)) {
                subject = subjectsMap.get(subjectCode);
            } else {
                subject = processSubject(subjectNode);
                subjectsMap.put(subject.getCode(), subject);
                dataManager.addSubject(subject);
            }
            
            professor.assignSubject(subject);
        }
    }
    
    // Procesar franjas bloqueadas
    if (professorNode.has("blockedSlots") && professorNode.get("blockedSlots").isArray()) {
        for (JsonNode slotNode : professorNode.get("blockedSlots")) {
            String day = slotNode.get("day").asText();
            LocalTime startTime = LocalTime.parse(slotNode.get("startTime").asText());
            LocalTime endTime = LocalTime.parse(slotNode.get("endTime").asText());
            
            BlockedSlot slot = new BlockedSlot.Builder()
                .day(day)
                .startTime(startTime)
                .endTime(endTime)
                .build();
                
            professor.addBlockedSlot(slot);
        }
    }
    
    return professor;
}

/**
 * Procesa un nodo de materia.
 */
private Subject processSubject(JsonNode subjectNode) {
    String code = subjectNode.get("code").asText();
    String name = subjectNode.get("name").asText();
    String description = subjectNode.get("description").asText();
    int credits = subjectNode.get("credits").asInt();
    boolean requiresLab = subjectNode.get("requiresLab").asBoolean();
    
    return new Subject(code, name, description, credits, requiresLab);
}

/**
 * Procesa un nodo de aula.
 */
private Room processRoom(JsonNode roomNode) {
    int id = roomNode.get("id").asInt();
    String name = roomNode.get("name").asText();
    int capacity = roomNode.get("capacity").asInt();
    boolean isLab = roomNode.get("isLab").asBoolean();
    
    return new Room(id, name, capacity, isLab);
}
}