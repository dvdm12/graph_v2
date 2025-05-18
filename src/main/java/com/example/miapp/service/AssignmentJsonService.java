package com.example.miapp.service;

import com.example.miapp.domain.*;
import com.example.miapp.domain.conflict.ConflictType;
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
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * Servicio especializado en la gestión de archivos JSON para asignaciones.
 * Proporciona funcionalidades para cargar, manipular y exportar datos 
 * de asignaciones y sus conflictos.
 */
public class AssignmentJsonService {
    private static final Logger logger = LoggerFactory.getLogger(AssignmentJsonService.class);
    
    private final DataManager dataManager;
    private final ObjectMapper objectMapper;
    private final ConflictGraphLoader graphLoader;
    private final GraphExporter graphExporter;
    
    /**
     * Constructor que inicializa las dependencias necesarias.
     */
    public AssignmentJsonService() {
        dataManager = DataManager.getInstance();
        
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        graphLoader = new ConflictGraphLoader();
        graphExporter = new GraphExporter(graphLoader);
        
        logger.info("AssignmentJsonService inicializado");
    }
    
    /**
     * Carga asignaciones desde un archivo JSON y las añade al DataManager.
     * Si una asignación ya existe con el mismo ID, se sobrescribe.
     * 
     * @param filePath Ruta del archivo JSON a cargar
     * @return Número de asignaciones cargadas correctamente
     * @throws IOException Si hay errores de lectura o formato del archivo
     * @throws DomainException Si hay errores en la validación de dominio
     */
    public int loadFromJson(String filePath) throws IOException, DomainException {
        logger.info("Iniciando carga de asignaciones desde JSON: {}", filePath);
        
        // Verificar que el archivo existe
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IOException("El archivo no existe: " + filePath);
        }
        
        // Leer el JSON
        ObjectNode rootNode = (ObjectNode) objectMapper.readTree(new File(filePath));
        
        // Verificar que tenga la estructura esperada
        if (!rootNode.has("assignments")) {
            throw new IOException("Formato JSON inválido: no contiene nodo 'assignments'");
        }
        
        ArrayNode assignmentsArray = (ArrayNode) rootNode.get("assignments");
        
        // Contador de asignaciones cargadas correctamente
        int successCount = 0;
        List<String> errors = new ArrayList<>();
        
        // Procesar cada asignación
        for (JsonNode assignmentNode : assignmentsArray) {
            try {
                Assignment assignment = parseAssignment(assignmentNode);
                
                // Verificar si ya existe una asignación con este ID
                int id = assignment.getId();
                if (dataManager.getAssignment(id) != null) {
                    dataManager.removeAssignment(id); // Eliminar la existente
                    logger.info("Reemplazando asignación existente con ID: {}", id);
                }
                
                // Añadir al DataManager
                dataManager.addAssignment(assignment);
                successCount++;
                
                logger.debug("Asignación cargada correctamente: ID={}", assignment.getId());
                
            } catch (Exception e) {
                // Registrar error pero continuar con la siguiente asignación
                String errorMsg = "Error al procesar asignación: " + e.getMessage();
                logger.error(errorMsg, e);
                errors.add(errorMsg);
            }
        }
        
        logger.info("Carga completada: {} de {} asignaciones cargadas correctamente",
                  successCount, assignmentsArray.size());
        
        // Si hubo errores pero se cargaron algunas asignaciones, lanzar excepción con detalles
        if (!errors.isEmpty() && successCount > 0) {
            String warningMsg = String.format(
                "Se cargaron %d de %d asignaciones. Errores: %s",
                successCount, assignmentsArray.size(), String.join("; ", errors));
            logger.warn(warningMsg);
        }
        
        // Si no se cargó ninguna asignación y hubo errores, lanzar excepción
        if (successCount == 0 && !errors.isEmpty()) {
            throw new DomainException("No se pudo cargar ninguna asignación: " + 
                                     String.join("; ", errors));
        }
        
        return successCount;
    }
    
    /**
     * Procesa un nodo JSON para convertirlo en un objeto Assignment.
     * 
     * @param assignmentNode Nodo JSON con datos de la asignación
     * @return Objeto Assignment construido a partir del JSON
     * @throws DomainException Si hay errores en la validación o faltan datos requeridos
     */
    private Assignment parseAssignment(JsonNode assignmentNode) throws DomainException {
        try {
            // Extraer datos básicos de la asignación
            int id = assignmentNode.get("id").asInt();
            String day = assignmentNode.get("day").asText();
            LocalTime startTime = LocalTime.parse(assignmentNode.get("startTime").asText());
            LocalTime endTime = LocalTime.parse(assignmentNode.get("endTime").asText());
            
            // Datos de grupo y estudiantes
            int groupId = assignmentNode.get("groupId").asInt();
            String groupName = assignmentNode.get("groupName").asText();
            String sessionType = assignmentNode.get("sessionType").asText();
            int enrolledStudents = assignmentNode.get("enrolledStudents").asInt();
            
            // Fecha de asignación (usar hoy si no está presente)
            LocalDate assignmentDate;
            if (assignmentNode.has("assignmentDate")) {
                assignmentDate = LocalDate.parse(assignmentNode.get("assignmentDate").asText());
            } else {
                assignmentDate = LocalDate.now();
            }
            
            // Procesar profesor
            JsonNode professorNode = assignmentNode.get("professor");
            int professorId = professorNode.get("id").asInt();
            Professor professor = dataManager.getProfessor(professorId);
            
            if (professor == null) {
                // Si el profesor no existe en el DataManager, crearlo
                String profName = professorNode.get("name").asText();
                String profDept = professorNode.get("department").asText();
                String profEmail = professorNode.get("email").asText();
                
                professor = new Professor(professorId, profName, profDept, profEmail);
                
                // Procesar materias del profesor
                if (professorNode.has("subjects") && professorNode.get("subjects").isArray()) {
                    for (JsonNode subjectNode : professorNode.get("subjects")) {
                        String code = subjectNode.get("code").asText();
                        Subject subject = dataManager.getSubject(code);
                        
                        if (subject != null) {
                            professor.assignSubject(subject);
                        } else {
                            // Si la materia no existe, crearla
                            String name = subjectNode.get("name").asText();
                            String description = subjectNode.has("description") ? 
                                               subjectNode.get("description").asText() : "";
                            int credits = subjectNode.has("credits") ? 
                                        subjectNode.get("credits").asInt() : 1;
                            boolean requiresLab = subjectNode.has("requiresLab") ? 
                                                subjectNode.get("requiresLab").asBoolean() : false;
                            
                            subject = new Subject(code, name, description, credits, requiresLab);
                            dataManager.addSubject(subject);
                            professor.assignSubject(subject);
                        }
                    }
                }
                
                // Procesar franjas bloqueadas
                if (professorNode.has("blockedSlots") && professorNode.get("blockedSlots").isArray()) {
                    for (JsonNode slotNode : professorNode.get("blockedSlots")) {
                        String slotDay = slotNode.get("day").asText();
                        LocalTime slotStart = LocalTime.parse(slotNode.get("startTime").asText());
                        LocalTime slotEnd = LocalTime.parse(slotNode.get("endTime").asText());
                        
                        BlockedSlot slot = new BlockedSlot.Builder()
                            .day(slotDay)
                            .startTime(slotStart)
                            .endTime(slotEnd)
                            .build();
                            
                        professor.addBlockedSlot(slot);
                    }
                }
                
                dataManager.addProfessor(professor);
            }
            
            // Procesar aula
            JsonNode roomNode = assignmentNode.get("room");
            int roomId = roomNode.get("id").asInt();
            Room room = dataManager.getRoom(roomId);
            
            if (room == null) {
                // Si el aula no existe en el DataManager, crearla
                String roomName = roomNode.get("name").asText();
                int capacity = roomNode.get("capacity").asInt();
                boolean isLab = roomNode.get("isLab").asBoolean();
                
                room = new Room(roomId, roomName, capacity, isLab);
                dataManager.addRoom(room);
            }
            
            // Procesar materia (opcional)
            Subject subject = null;
            if (assignmentNode.has("subject") && !assignmentNode.get("subject").isNull()) {
                JsonNode subjectNode = assignmentNode.get("subject");
                String code = subjectNode.get("code").asText();
                subject = dataManager.getSubject(code);
                
                if (subject == null) {
                    // Si la materia no existe, crearla
                    String name = subjectNode.get("name").asText();
                    String description = subjectNode.has("description") ? 
                                       subjectNode.get("description").asText() : "";
                    int credits = subjectNode.has("credits") ? 
                                subjectNode.get("credits").asInt() : 1;
                    boolean requiresLab = subjectNode.has("requiresLab") ? 
                                        subjectNode.get("requiresLab").asBoolean() : false;
                    
                    subject = new Subject(code, name, description, credits, requiresLab);
                    dataManager.addSubject(subject);
                }
            }
            
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
            
            return builder.build();
            
        } catch (Exception e) {
            String errorMsg = "Error procesando nodo JSON de asignación: " + e.getMessage();
            logger.error(errorMsg, e);
            throw new DomainException(errorMsg, e);
        }
    }
    
    /**
     * Guarda todas las asignaciones actuales en un archivo JSON.
     * Si el archivo existe, actualiza su contenido manteniendo la estructura.
     * 
     * @param filePath Ruta del archivo donde guardar las asignaciones
     * @throws IOException Si hay errores de escritura
     */
    public void saveToJson(String filePath) throws IOException {
        logger.info("Guardando asignaciones en JSON: {}", filePath);
        
        // Preparar directorio si no existe
        Path path = Paths.get(filePath);
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        
        // Obtener las asignaciones actuales
        List<Assignment> assignments = dataManager.getAllAssignments();
        
        // Verificar si el archivo ya existe para preservar otros datos
        ObjectNode rootNode;
        if (Files.exists(path)) {
            try {
                rootNode = (ObjectNode) objectMapper.readTree(new File(filePath));
                logger.debug("Archivo JSON existente encontrado, actualizando contenido");
            } catch (Exception e) {
                logger.warn("Error leyendo JSON existente, creando nuevo: {}", e.getMessage());
                rootNode = objectMapper.createObjectNode();
            }
        } else {
            rootNode = objectMapper.createObjectNode();
        }
        
        // Crear array de asignaciones
        ArrayNode assignmentsArray = objectMapper.createArrayNode();
        
        // Añadir cada asignación al array
        for (Assignment assignment : assignments) {
            ObjectNode assignNode = convertAssignmentToJson(assignment);
            assignmentsArray.add(assignNode);
        }
        
        // Añadir o reemplazar el nodo de asignaciones
        rootNode.set("assignments", assignmentsArray);
        
        // Escribir en archivo
        objectMapper.writerWithDefaultPrettyPrinter()
                   .writeValue(new File(filePath), rootNode);
        
        logger.info("Guardadas {} asignaciones en {}", assignments.size(), filePath);
    }
    
    /**
     * Convierte una asignación a su representación JSON completa.
     * 
     * @param assignment Asignación a convertir
     * @return Nodo JSON con la representación completa de la asignación
     */
    private ObjectNode convertAssignmentToJson(Assignment assignment) {
        ObjectNode assignNode = objectMapper.createObjectNode();
        
        // Datos básicos
        assignNode.put("id", assignment.getId());
        assignNode.put("assignmentDate", assignment.getAssignmentDate().toString());
        assignNode.put("day", assignment.getDay());
        assignNode.put("startTime", assignment.getStartTime().toString());
        assignNode.put("endTime", assignment.getEndTime().toString());
        assignNode.put("groupId", assignment.getGroupId());
        assignNode.put("groupName", assignment.getGroupName());
        assignNode.put("sessionType", assignment.getSessionType());
        assignNode.put("enrolledStudents", assignment.getEnrolledStudents());
        
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
        
        assignNode.set("professor", professorNode);
        
        // Aula anidada
        ObjectNode roomNode = objectMapper.createObjectNode();
        Room room = assignment.getRoom();
        roomNode.put("id", room.getId());
        roomNode.put("name", room.getName());
        roomNode.put("capacity", room.getCapacity());
        roomNode.put("isLab", room.isLab());
        assignNode.set("room", roomNode);
        
        // Materia anidada
        if (assignment.getSubject() != null) {
            ObjectNode subjectNode = objectMapper.createObjectNode();
            Subject subject = assignment.getSubject();
            subjectNode.put("code", subject.getCode());
            subjectNode.put("name", subject.getName());
            subjectNode.put("description", subject.getDescription());
            subjectNode.put("credits", subject.getCredits());
            subjectNode.put("requiresLab", subject.requiresLab());
            assignNode.set("subject", subjectNode);
        }
        
        return assignNode;
    }
    
    /**
     * Añade una nueva asignación al archivo JSON existente.
     * Si el archivo no existe, lo crea con la asignación.
     * 
     * @param assignment Asignación a añadir
     * @param filePath Ruta del archivo donde añadir la asignación
     * @throws IOException Si hay errores de lectura/escritura
     */
    public void addAssignmentToJson(Assignment assignment, String filePath) throws IOException {
        logger.info("Añadiendo asignación ID={} a JSON: {}", assignment.getId(), filePath);
        
        // Preparar directorio si no existe
        Path path = Paths.get(filePath);
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        
        // Verificar si el archivo ya existe
        ObjectNode rootNode;
        ArrayNode assignmentsArray;
        
        if (Files.exists(path)) {
            try {
                rootNode = (ObjectNode) objectMapper.readTree(new File(filePath));
                
                // Obtener o crear array de asignaciones
                if (rootNode.has("assignments") && rootNode.get("assignments").isArray()) {
                    assignmentsArray = (ArrayNode) rootNode.get("assignments");
                } else {
                    assignmentsArray = objectMapper.createArrayNode();
                    rootNode.set("assignments", assignmentsArray);
                }
                
                logger.debug("Archivo JSON existente encontrado, añadiendo asignación");
                
            } catch (Exception e) {
                logger.warn("Error leyendo JSON existente, creando nuevo: {}", e.getMessage());
                rootNode = objectMapper.createObjectNode();
                assignmentsArray = objectMapper.createArrayNode();
                rootNode.set("assignments", assignmentsArray);
            }
        } else {
            rootNode = objectMapper.createObjectNode();
            assignmentsArray = objectMapper.createArrayNode();
            rootNode.set("assignments", assignmentsArray);
        }
        
        // Convertir asignación a JSON
        ObjectNode assignNode = convertAssignmentToJson(assignment);
        
        // Verificar si ya existe una asignación con el mismo ID para actualizarla
        boolean updated = false;
        for (int i = 0; i < assignmentsArray.size(); i++) {
            JsonNode node = assignmentsArray.get(i);
            if (node.has("id") && node.get("id").asInt() == assignment.getId()) {
                assignmentsArray.set(i, assignNode);
                updated = true;
                logger.debug("Actualizada asignación existente con ID={}", assignment.getId());
                break;
            }
        }
        
        // Si no se actualizó, añadir como nueva
        if (!updated) {
            assignmentsArray.add(assignNode);
            logger.debug("Añadida nueva asignación con ID={}", assignment.getId());
        }
        
        // Escribir en archivo
        objectMapper.writerWithDefaultPrettyPrinter()
                   .writeValue(new File(filePath), rootNode);
        
        logger.info("Asignación guardada en JSON correctamente");
    }
    
    /**
 * Analiza las asignaciones del archivo JSON para detectar conflictos
 * y genera un archivo graph.json con el grafo de conflictos.
 * Además, crea una copia en una ubicación absoluta específica.
 * 
 * @param inputJsonPath Ruta del archivo data.json
 * @param outputGraphPath Ruta donde generar el archivo graph.json
 * @return Número de conflictos detectados
 * @throws IOException Si hay errores de lectura/escritura
 * @throws DomainException Si hay errores procesando los datos
 */
public int generateConflictGraph(String inputJsonPath, String outputGraphPath) 
        throws IOException, DomainException {
    logger.info("Iniciando generación de grafo de conflictos");
    logger.info("Archivo origen: {}", inputJsonPath);
    logger.info("Archivo destino principal: {}", outputGraphPath);
    
    // Primero carga todas las asignaciones del archivo
    int numLoaded = loadFromJson(inputJsonPath);
    
    if (numLoaded == 0) {
        throw new DomainException("No se encontraron asignaciones para analizar");
    }
    
    // Cargar las asignaciones en el grafo de conflictos
    graphLoader.clear();
    graphLoader.loadAllAssignments();
    
    // Obtener estadísticas
    int totalConflicts = graphLoader.getTotalConflictsCount();
    Map<ConflictType, Integer> stats = graphLoader.getConflictStatistics();
    
    // Mostrar estadísticas
    logger.info("Análisis completado: {} conflictos detectados", totalConflicts);
    for (Map.Entry<ConflictType, Integer> entry : stats.entrySet()) {
        logger.info("  - {}: {}", entry.getKey().getLabel(), entry.getValue());
    }
    
    // Exportar el grafo a JSON en la ubicación seleccionada por el usuario
    graphExporter.exportToJson(outputGraphPath);
    logger.info("Grafo de conflictos exportado correctamente a: {}", outputGraphPath);
    
    // NUEVA FUNCIONALIDAD: Crear copia en directorio predefinido (ruta absoluta)
    try {
        // Usar la ruta absoluta específica
        File backupDir = new File("C:\\graph_project\\graph_two\\graph\\src\\main\\java\\com\\example\\miapp\\ui\\main\\graph_data");
        
        // Crear el directorio si no existe
        if (!backupDir.exists()) {
            boolean created = backupDir.mkdirs();
            if (!created) {
                logger.error("No se pudo crear el directorio de backup: {}", backupDir.getAbsolutePath());
                return totalConflicts; // Continuar con el flujo normal
            }
        }
        
        // Generar nombre para la copia (solo una versión sin timestamp)
        File backupFile = new File(backupDir, "graph.json");
        
        // Copiar el archivo generado
        Files.copy(Paths.get(outputGraphPath), backupFile.toPath(), 
                  StandardCopyOption.REPLACE_EXISTING);
        logger.info("Copia de seguridad creada en: {}", backupFile.getAbsolutePath());
        
    } catch (Exception e) {
        // No interrumpir el flujo principal si la copia falla
        logger.error("Error al crear copia de seguridad del grafo: {}", e.getMessage(), e);
        // No lanzar la excepción para no interrumpir el flujo normal si solo falló la copia
    }
    
    return totalConflicts;
}
    
    /**
     * Elimina una asignación del archivo JSON.
     * 
     * @param assignmentId ID de la asignación a eliminar
     * @param filePath Ruta del archivo JSON
     * @return true si se eliminó la asignación, false si no se encontró
     * @throws IOException Si hay errores de lectura/escritura
     */
    public boolean removeAssignmentFromJson(int assignmentId, String filePath) throws IOException {
        logger.info("Eliminando asignación ID={} de JSON: {}", assignmentId, filePath);
        
        // Verificar si el archivo existe
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            logger.warn("El archivo no existe: {}", filePath);
            return false;
        }
        
        // Leer el JSON
        ObjectNode rootNode = (ObjectNode) objectMapper.readTree(new File(filePath));
        
        // Verificar estructura
        if (!rootNode.has("assignments") || !rootNode.get("assignments").isArray()) {
            logger.warn("Estructura JSON inválida en {}", filePath);
            return false;
        }
        
        ArrayNode assignmentsArray = (ArrayNode) rootNode.get("assignments");
        
        // Buscar y eliminar la asignación
        boolean found = false;
        for (int i = 0; i < assignmentsArray.size(); i++) {
            JsonNode node = assignmentsArray.get(i);
            if (node.has("id") && node.get("id").asInt() == assignmentId) {
                assignmentsArray.remove(i);
                found = true;
                logger.debug("Asignación con ID={} encontrada y eliminada", assignmentId);
                break;
            }
        }
        
        // Si no se encontró, retornar false
        if (!found) {
            logger.warn("No se encontró asignación con ID={}", assignmentId);
            return false;
        }
        
        // Escribir cambios
        objectMapper.writerWithDefaultPrettyPrinter()
                   .writeValue(new File(filePath), rootNode);
        
        logger.info("Asignación eliminada de JSON correctamente");
        return true;
    }
    
    /**
     * Busca una asignación específica en el archivo JSON.
     * 
     * @param assignmentId ID de la asignación a buscar
     * @param filePath Ruta del archivo JSON
     * @return Optional con la asignación si se encuentra, vacío si no
     * @throws IOException Si hay errores de lectura
     */
    public Optional<Assignment> findAssignmentInJson(int assignmentId, String filePath) throws IOException {
        logger.info("Buscando asignación ID={} en JSON: {}", assignmentId, filePath);
        
        // Verificar si el archivo existe
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            logger.warn("El archivo no existe: {}", filePath);
            return Optional.empty();
        }
        
        // Leer el JSON
        ObjectNode rootNode = (ObjectNode) objectMapper.readTree(new File(filePath));
        
        // Verificar estructura
        if (!rootNode.has("assignments") || !rootNode.get("assignments").isArray()) {
            logger.warn("Estructura JSON inválida en {}", filePath);
            return Optional.empty();
        }
        
        ArrayNode assignmentsArray = (ArrayNode) rootNode.get("assignments");
        
        // Buscar la asignación
        for (JsonNode node : assignmentsArray) {
            if (node.has("id") && node.get("id").asInt() == assignmentId) {
                logger.debug("Asignación con ID={} encontrada", assignmentId);
                try {
                    Assignment assignment = parseAssignment(node);
                    return Optional.of(assignment);
                } catch (Exception e) {
                    logger.error("Error procesando asignación encontrada: {}", e.getMessage(), e);
                    return Optional.empty();
                }
            }
        }
        
        logger.debug("No se encontró asignación con ID={}", assignmentId);
        return Optional.empty();
    }
}