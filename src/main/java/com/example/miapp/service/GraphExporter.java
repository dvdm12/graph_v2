package com.example.miapp.service;

import com.example.miapp.domain.Assignment;
import com.example.miapp.domain.conflict.ConflictEdge;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servicio para exportar la estructura de conflictos a formato JSON.
 * Refactorizado para trabajar con el nuevo modelo de dominio y manejar grafos grandes.
 * Modificado para usar un formato de nodo idéntico a data.json
 */
public class GraphExporter {
    private static final Logger logger = LoggerFactory.getLogger(GraphExporter.class);
    private static final int DEFAULT_BATCH_SIZE = 1000; // Tamaño de lote por defecto

    private final ConflictGraphLoader graphLoader;
    private final ObjectMapper mapper;

    /**
     * Constructor que utiliza el ConflictGraphLoader para obtener datos de conflictos.
     * 
     * @param graphLoader Gestor de grafo de conflictos
     * @throws NullPointerException si graphLoader es null
     */
    public GraphExporter(ConflictGraphLoader graphLoader) {
        this.graphLoader = Objects.requireNonNull(graphLoader, "El graphLoader no puede ser null");
        this.mapper = new ObjectMapper().findAndRegisterModules();
        logger.info("GraphExporter inicializado");
    }

    /**
     * Genera un JSON con nodos y aristas basados en los conflictos detectados.
     * 
     * @param filePath Ruta de salida del JSON
     * @throws IOException Si hay error de escritura
     * @throws IllegalArgumentException Si la ruta es inválida o no es accesible
     */
    public void exportToJson(String filePath) throws IOException {
        logger.info("==== exportToJson START ====");
        Instant start = Instant.now();
        
        // Validar ruta de archivo
        validateFilePath(filePath);
        
        // Obtener número de asignaciones para decidir método de exportación
        List<Assignment> assignments = graphLoader.getAllAssignments();
        int assignmentCount = assignments.size();
        
        logger.info("Preparando exportación de {} asignaciones y sus conflictos", assignmentCount);
        
        // Si hay muchas asignaciones, usar el método de streaming
        if (assignmentCount > 10000) {
            logger.info("Detectado conjunto de datos grande, usando exportación por streaming");
            exportToJsonStreaming(filePath);
        } else {
            // Para conjuntos pequeños o medianos, usar el método en memoria
            ObjectNode rootNode = mapper.createObjectNode();
            
            // Añadir array de nodos (asignaciones)
            rootNode.set("nodes", createNodesArray(assignments));
            
            // Añadir array de aristas (conflictos)
            rootNode.set("edges", createEdgesArray());
            
            // Escribir en fichero
            mapper.writerWithDefaultPrettyPrinter()
                  .writeValue(new File(filePath), rootNode);
            
            logger.info("Exportado grafo con {} nodos y {} aristas", 
                       assignmentCount, graphLoader.getEdgeConflicts().size());
        }
        
        Instant end = Instant.now();
        logger.info("==== exportToJson END: {} ms ====", Duration.between(start, end).toMillis());
    }
    
    /**
     * Exporta el grafo usando streaming JSON para reducir uso de memoria.
     * Útil para conjuntos de datos grandes.
     * 
     * @param filePath Ruta del archivo
     * @throws IOException Si hay error de escritura
     */
    public void exportToJsonStreaming(String filePath) throws IOException {
        logger.info("==== exportToJsonStreaming START ====");
        Instant start = Instant.now();
        
        validateFilePath(filePath);
        
        try (JsonGenerator generator = mapper.getFactory().createGenerator(
                new File(filePath), JsonEncoding.UTF8)) {
            
            generator.useDefaultPrettyPrinter();
            
            // Iniciar documento JSON
            generator.writeStartObject();
            
            // Escribir nodos
            generator.writeFieldName("nodes");
            generator.writeStartArray();
            
            int nodeCount = 0;
            for (Assignment a : graphLoader.getAllAssignments()) {
                // Escribir cada nodo sin mantenerlos todos en memoria
                writeNodeToGenerator(generator, a);
                nodeCount++;
                
                // Logging periódico para mostrar progreso
                if (nodeCount % 1000 == 0) {
                    logger.debug("Procesados {} nodos...", nodeCount);
                }
            }
            
            generator.writeEndArray();
            logger.info("Exportados {} nodos mediante streaming", nodeCount);
            
            // Escribir aristas
            generator.writeFieldName("edges");
            generator.writeStartArray();
            
            Map<String, List<ConflictEdge>> edgeConflicts = graphLoader.getEdgeConflicts();
            int edgeCount = 0;
            for (Map.Entry<String, List<ConflictEdge>> entry : edgeConflicts.entrySet()) {
                // Escribir cada arista sin mantenerlas todas en memoria
                writeEdgeToGenerator(generator, entry.getKey(), entry.getValue());
                edgeCount++;
                
                // Logging periódico para mostrar progreso
                if (edgeCount % 1000 == 0) {
                    logger.debug("Procesadas {} aristas...", edgeCount);
                }
            }
            
            generator.writeEndArray();
            logger.info("Exportadas {} aristas mediante streaming", edgeCount);
            
            // Finalizar documento JSON
            generator.writeEndObject();
        }
        
        Instant end = Instant.now();
        logger.info("==== exportToJsonStreaming END: {} ms ====", Duration.between(start, end).toMillis());
    }
    
    /**
     * Exporta el grafo por lotes para manejar conjuntos de datos grandes.
     * 
     * @param filePath Ruta base para los archivos (se añadirá un sufijo por lote)
     * @throws IOException Si hay error de escritura
     */
    public void exportToJsonInBatches(String filePath) throws IOException {
        exportToJsonInBatches(filePath, DEFAULT_BATCH_SIZE);
    }
    
    /**
     * Exporta el grafo por lotes para manejar conjuntos de datos grandes.
     * 
     * @param filePath Ruta base para los archivos (se añadirá un sufijo por lote)
     * @param batchSize Tamaño de cada lote
     * @throws IOException Si hay error de escritura
     */
    public void exportToJsonInBatches(String filePath, int batchSize) throws IOException {
        logger.info("==== exportToJsonInBatches START ====");
        Instant start = Instant.now();
        
        List<Assignment> allAssignments = graphLoader.getAllAssignments();
        int totalAssignments = allAssignments.size();
        int batches = (int) Math.ceil((double) totalAssignments / batchSize);
        
        logger.info("Exportando {} asignaciones en {} lotes de tamaño {}", 
                  totalAssignments, batches, batchSize);
        
        for (int i = 0; i < batches; i++) {
            int fromIndex = i * batchSize;
            int toIndex = Math.min(fromIndex + batchSize, totalAssignments);
            List<Assignment> batchAssignments = allAssignments.subList(fromIndex, toIndex);
            
            String batchFilePath = filePath + "_batch" + (i + 1) + ".json";
            exportBatchToJson(batchFilePath, batchAssignments);
            
            logger.debug("Lote {} exportado: {} asignaciones", i + 1, batchAssignments.size());
        }
        
        Instant end = Instant.now();
        logger.info("==== exportToJsonInBatches END: {} ms ====", Duration.between(start, end).toMillis());
    }
    
    /**
     * Exporta un lote específico de asignaciones a JSON.
     * 
     * @param filePath Ruta del archivo
     * @param batchAssignments Lista de asignaciones del lote
     * @throws IOException Si hay error de escritura
     */
    private void exportBatchToJson(String filePath, List<Assignment> batchAssignments) throws IOException {
        validateFilePath(filePath);
        
        ObjectNode rootNode = mapper.createObjectNode();
        
        // Añadir array de nodos (asignaciones del lote)
        rootNode.set("nodes", createNodesArray(batchAssignments));
        
        // Recopilar IDs de asignaciones en este lote
        Set<Integer> batchAssignmentIds = batchAssignments.stream()
            .map(Assignment::getId)
            .collect(Collectors.toSet());
        
        // Añadir solo las aristas relacionadas con asignaciones del lote
        rootNode.set("edges", createEdgesArrayForBatch(batchAssignmentIds));
        
        // Escribir en fichero
        mapper.writerWithDefaultPrettyPrinter()
              .writeValue(new File(filePath), rootNode);
    }
    
    /**
     * Valida que la ruta del archivo sea válida y accesible.
     * 
     * @param filePath Ruta a validar
     * @throws IllegalArgumentException Si la ruta es inválida o no es accesible
     * @throws NullPointerException Si filePath es null
     */
    private void validateFilePath(String filePath) {
        Objects.requireNonNull(filePath, "La ruta del archivo no puede ser null");
        
        if (filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("La ruta del archivo no puede estar vacía");
        }
        
        Path path = Paths.get(filePath);
        Path parent = path.getParent();
        
        // Verificar que el directorio exista o se pueda crear
        if (parent != null && !Files.exists(parent)) {
            try {
                Files.createDirectories(parent);
                logger.debug("Directorio creado: {}", parent);
            } catch (IOException e) {
                throw new IllegalArgumentException(
                    "No se puede crear el directorio para el archivo: " + e.getMessage(), e);
            }
        }
        
        // Verificar si se puede escribir
        if (Files.exists(path) && !Files.isWritable(path)) {
            throw new IllegalArgumentException("El archivo existe pero no se puede escribir: " + filePath);
        }
    }
    
    /**
     * Crea el array de nodos JSON a partir de las asignaciones proporcionadas.
     * 
     * @param assignments Lista de asignaciones a procesar
     * @return ArrayNode con los nodos JSON
     */
    private ArrayNode createNodesArray(List<Assignment> assignments) {
        ArrayNode nodesArray = mapper.createArrayNode();
        
        logger.debug("Exportando {} nodos", assignments.size());
        
        for (Assignment a : assignments) {
            ObjectNode node = createNodeObject(a);
            nodesArray.add(node);
            logger.trace("Nodo agregado: id={}", a.getId());
        }
        
        return nodesArray;
    }
    
    /**
     * Crea el objeto JSON para un nodo (asignación) con formato idéntico a data.json.
     * 
     * @param a Asignación a convertir en nodo
     * @return ObjectNode representando la asignación
     */
    private ObjectNode createNodeObject(Assignment a) {
        ObjectNode node = mapper.createObjectNode();
        
        // Datos básicos de la asignación (exactamente como en data.json)
        node.put("id", a.getId());
        node.put("assignmentDate", a.getAssignmentDate().toString());
        node.put("day", a.getDay());
        node.put("startTime", a.getStartTime().toString());
        node.put("endTime", a.getEndTime().toString());
        node.put("groupId", a.getGroupId());
        node.put("groupName", a.getGroupName());
        node.put("sessionType", a.getSessionType());
        node.put("enrolledStudents", a.getEnrolledStudents());
        
        // Información del profesor con sus franjas bloqueadas
        ObjectNode professorNode = mapper.createObjectNode();
        professorNode.put("id", a.getProfessorId());
        professorNode.put("name", a.getProfessorName());
        professorNode.put("department", a.getProfessor().getDepartment());
        professorNode.put("email", a.getProfessor().getEmail());
        
        // Materias asignadas al profesor
        ArrayNode professorSubjectsArray = mapper.createArrayNode();
        a.getProfessor().getSubjects().forEach(subject -> {
            ObjectNode subjectNode = mapper.createObjectNode();
            subjectNode.put("code", subject.getCode());
            subjectNode.put("name", subject.getName());
            subjectNode.put("description", subject.getDescription());
            subjectNode.put("credits", subject.getCredits());
            subjectNode.put("requiresLab", subject.requiresLab());
            professorSubjectsArray.add(subjectNode);
        });
        professorNode.set("subjects", professorSubjectsArray);
        
        // Franjas bloqueadas del profesor
        ArrayNode blockedSlotsArray = mapper.createArrayNode();
        a.getProfessor().getBlockedSlots().forEach(slot -> {
            ObjectNode slotNode = mapper.createObjectNode();
            slotNode.put("day", slot.getDay());
            slotNode.put("startTime", slot.getStartTime().toString());
            slotNode.put("endTime", slot.getEndTime().toString());
            blockedSlotsArray.add(slotNode);
        });
        professorNode.set("blockedSlots", blockedSlotsArray);
        
        node.set("professor", professorNode);
        
        // Información del aula
        ObjectNode roomNode = mapper.createObjectNode();
        roomNode.put("id", a.getRoomId());
        roomNode.put("name", a.getRoomName());
        roomNode.put("capacity", a.getRoom().getCapacity());
        roomNode.put("isLab", a.getRoom().isLab());
        node.set("room", roomNode);
        
        // Información de la materia
        if (a.getSubject() != null) {
            ObjectNode subjectNode = mapper.createObjectNode();
            subjectNode.put("code", a.getSubject().getCode());
            subjectNode.put("name", a.getSubject().getName());
            subjectNode.put("description", a.getSubject().getDescription());
            subjectNode.put("credits", a.getSubject().getCredits());
            subjectNode.put("requiresLab", a.getSubject().requiresLab());
            node.set("subject", subjectNode);
        }
        
        return node;
    }
    
    /**
     * Crea el array de aristas JSON a partir de los conflictos.
     * 
     * @return ArrayNode con las aristas JSON
     */
    private ArrayNode createEdgesArray() {
        ArrayNode edgesArray = mapper.createArrayNode();
        Map<String, List<ConflictEdge>> edgeConflicts = graphLoader.getEdgeConflicts();
        
        logger.debug("Exportando {} aristas", edgeConflicts.size());
        
        for (Map.Entry<String, List<ConflictEdge>> entry : edgeConflicts.entrySet()) {
            String key = entry.getKey();
            List<ConflictEdge> conflicts = entry.getValue();
            
            ObjectNode edgeNode = createEdgeObject(key, conflicts);
            edgesArray.add(edgeNode);
            logger.trace("Arista procesada: {}", key);
        }
        
        return edgesArray;
    }
    
    /**
     * Crea el array de aristas JSON solo para las asignaciones del lote especificado.
     * 
     * @param batchAssignmentIds Conjunto de IDs de asignaciones del lote
     * @return ArrayNode con las aristas JSON filtradas
     */
    private ArrayNode createEdgesArrayForBatch(Set<Integer> batchAssignmentIds) {
        ArrayNode edgesArray = mapper.createArrayNode();
        Map<String, List<ConflictEdge>> edgeConflicts = graphLoader.getEdgeConflicts();
        
        int totalEdges = 0;
        int filteredEdges = 0;
        
        for (Map.Entry<String, List<ConflictEdge>> entry : edgeConflicts.entrySet()) {
            totalEdges++;
            String key = entry.getKey();
            List<ConflictEdge> conflicts = entry.getValue();
            
            // Obtener IDs de las asignaciones involucradas
            String[] parts = key.split("-");
            int id1 = Integer.parseInt(parts[0]);
            int id2 = Integer.parseInt(parts[1]);
            
            // Solo incluir aristas donde al menos una asignación está en el lote
            if (batchAssignmentIds.contains(id1) || batchAssignmentIds.contains(id2)) {
                ObjectNode edgeNode = createEdgeObject(key, conflicts);
                edgesArray.add(edgeNode);
                filteredEdges++;
                logger.trace("Arista procesada para lote: {}", key);
            }
        }
        
        logger.debug("Filtradas {} de {} aristas para el lote", filteredEdges, totalEdges);
        return edgesArray;
    }
    
    /**
     * Crea el objeto JSON para una arista (conflicto).
     * 
     * @param key Clave que identifica las asignaciones involucradas
     * @param conflicts Lista de conflictos entre esas asignaciones
     * @return ObjectNode representando la arista
     */
    private ObjectNode createEdgeObject(String key, List<ConflictEdge> conflicts) {
        // Obtener IDs de las asignaciones involucradas
        String[] parts = key.split("-");
        int id1 = Integer.parseInt(parts[0]);
        int id2 = Integer.parseInt(parts[1]);
        
        // Crear el objeto de arista
        ObjectNode edgeNode = mapper.createObjectNode();
        
        // Añadir los IDs de las asignaciones en "between"
        ArrayNode betweenNode = mapper.createArrayNode();
        betweenNode.add(id1);
        betweenNode.add(id2);
        edgeNode.set("between", betweenNode);
        
        // Añadir los tipos de conflictos en "conflicts"
        ArrayNode conflictsNode = mapper.createArrayNode();
        for (ConflictEdge conflict : conflicts) {
            // Usar el label (descripción) del tipo de conflicto
            conflictsNode.add(conflict.getType().getLabel());
        }
        edgeNode.set("conflicts", conflictsNode);
        
        return edgeNode;
    }
    
    /**
     * Escribe un nodo directamente al generador JSON con formato idéntico a data.json.
     * 
     * @param generator Generador JSON
     * @param a Asignación a escribir
     * @throws IOException Si hay error de escritura
     */
    private void writeNodeToGenerator(JsonGenerator generator, Assignment a) throws IOException {
        generator.writeStartObject();
        
        // Datos básicos de la asignación
        generator.writeNumberField("id", a.getId());
        generator.writeStringField("assignmentDate", a.getAssignmentDate().toString());
        generator.writeStringField("day", a.getDay());
        generator.writeStringField("startTime", a.getStartTime().toString());
        generator.writeStringField("endTime", a.getEndTime().toString());
        generator.writeNumberField("groupId", a.getGroupId());
        generator.writeStringField("groupName", a.getGroupName());
        generator.writeStringField("sessionType", a.getSessionType());
        generator.writeNumberField("enrolledStudents", a.getEnrolledStudents());
        
        // Información del profesor
        generator.writeObjectFieldStart("professor");
        generator.writeNumberField("id", a.getProfessorId());
        generator.writeStringField("name", a.getProfessorName());
        generator.writeStringField("department", a.getProfessor().getDepartment());
        generator.writeStringField("email", a.getProfessor().getEmail());
        
        // Materias asignadas al profesor
        generator.writeArrayFieldStart("subjects");
        for (var subject : a.getProfessor().getSubjects()) {
            generator.writeStartObject();
            generator.writeStringField("code", subject.getCode());
            generator.writeStringField("name", subject.getName());
            generator.writeStringField("description", subject.getDescription());
            generator.writeNumberField("credits", subject.getCredits());
            generator.writeBooleanField("requiresLab", subject.requiresLab());
            generator.writeEndObject();
        }
        generator.writeEndArray();
        
        // Franjas bloqueadas del profesor
        generator.writeArrayFieldStart("blockedSlots");
        for (var slot : a.getProfessor().getBlockedSlots()) {
            generator.writeStartObject();
            generator.writeStringField("day", slot.getDay());
            generator.writeStringField("startTime", slot.getStartTime().toString());
            generator.writeStringField("endTime", slot.getEndTime().toString());
            generator.writeEndObject();
        }
        generator.writeEndArray();
        generator.writeEndObject(); // Fin información profesor
        
        // Información del aula
        generator.writeObjectFieldStart("room");
        generator.writeNumberField("id", a.getRoomId());
        generator.writeStringField("name", a.getRoomName());
        generator.writeNumberField("capacity", a.getRoom().getCapacity());
        generator.writeBooleanField("isLab", a.getRoom().isLab());
        generator.writeEndObject(); // Fin información aula
        
        // Información de la materia
        if (a.getSubject() != null) {
            generator.writeObjectFieldStart("subject");
            generator.writeStringField("code", a.getSubject().getCode());
            generator.writeStringField("name", a.getSubject().getName());
            generator.writeStringField("description", a.getSubject().getDescription());
            generator.writeNumberField("credits", a.getSubject().getCredits());
            generator.writeBooleanField("requiresLab", a.getSubject().requiresLab());
            generator.writeEndObject(); // Fin información materia
        }
        
        generator.writeEndObject(); // Fin del nodo
    }
    
    /**
     * Escribe una arista directamente al generador JSON.
     * 
     * @param generator Generador JSON
     * @param key Clave que identifica las asignaciones involucradas
     * @param conflicts Lista de conflictos entre esas asignaciones
     * @throws IOException Si hay error de escritura
     */
    private void writeEdgeToGenerator(JsonGenerator generator, String key, List<ConflictEdge> conflicts) throws IOException {
        // Obtener IDs de las asignaciones involucradas
        String[] parts = key.split("-");
        int id1 = Integer.parseInt(parts[0]);
        int id2 = Integer.parseInt(parts[1]);
        
        generator.writeStartObject();
        
        // Añadir los IDs de las asignaciones en "between"
        generator.writeArrayFieldStart("between");
        generator.writeNumber(id1);
        generator.writeNumber(id2);
        generator.writeEndArray();
        
        // Añadir los tipos de conflictos en "conflicts"
        generator.writeArrayFieldStart("conflicts");
        for (ConflictEdge conflict : conflicts) {
            generator.writeString(conflict.getType().getLabel());
        }
        generator.writeEndArray();
        
        generator.writeEndObject();
    }
    
    /**
     * Genera una versión de exportación en memoria (sin escribir a archivo).
     * Útil para pruebas o visualización directa.
     * 
     * @return String con el JSON generado
     * @throws IOException Si hay error en la serialización
     */
    public String generateJsonString() throws IOException {
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.set("nodes", createNodesArray(graphLoader.getAllAssignments()));
        rootNode.set("edges", createEdgesArray());
        
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
    }
}