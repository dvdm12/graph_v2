package com.example.miapp.ui.main;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Clase que maneja la lógica de carga y procesamiento de asignaciones desde el grafo de conflictos.
 * Separa la lógica de negocio de la interfaz gráfica.
 */
public class AssignmentManager {
    private static final Logger logger = Logger.getLogger(AssignmentManager.class.getName());
    
    // Días de la semana (sin domingo)
    private static final String[] DAYS = {
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    };
    
    // Mapeo de nombres de días en español
    private static final Map<String, String> DAYS_MAP = Map.of(
        "Monday", "Lunes",
        "Tuesday", "Martes",
        "Wednesday", "Miércoles",
        "Thursday", "Jueves",
        "Friday", "Viernes",
        "Saturday", "Sábado"
    );
    
    // Almacenamiento de asignaciones por día
    private final Map<String, List<AssignmentInfo>> assignmentsByDay = new HashMap<>();
    
    /**
     * Constructor que inicializa el gestor de asignaciones
     */
    public AssignmentManager() {
        // Inicializar mapa de asignaciones por día
        for (String day : DAYS) {
            assignmentsByDay.put(day, new ArrayList<>());
        }
        
        // Cargar asignaciones desde el grafo
        loadFromGraphJson();
    }
    
    /**
 * Carga asignaciones desde el archivo graph.json
 */
private void loadFromGraphJson() {
    try {
        // Ruta absoluta específica
        String specificPath = "C:/graph_project/graph_two/graph/src/main/java/com/example/miapp/ui/main/graph_data/graph.json";
        File file = new File(specificPath);
        
        logger.info("Intentando cargar graph.json desde: " + specificPath);
        
        if (!file.exists()) {
            logger.severe("No se pudo encontrar el archivo graph.json en: " + specificPath);
            logger.warning("Cargando datos de ejemplo como alternativa...");
            loadSampleData();
            return;
        }
        
        String jsonContent = new String(Files.readAllBytes(Paths.get(specificPath)));
        logger.info("Archivo graph.json cargado desde: " + specificPath);
        
        logger.info("Analizando contenido JSON...");
        // Analizar el JSON
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(jsonContent);
        
        // Depuración: Mostrar estructura
        logger.info("Estructura del JSON: " + rootNode.getNodeType());
        Set<String> fieldsInRoot = new HashSet<>();
        rootNode.fieldNames().forEachRemaining(fieldsInRoot::add);
        logger.info("Campos en el nodo raíz: " + fieldsInRoot);
        
        // Obtener nodos y aristas
        JsonNode nodesNode = rootNode.get("nodes");
        JsonNode edgesNode = rootNode.get("edges");
        
        if (nodesNode == null || !nodesNode.isArray()) {
            logger.warning("No se encontraron nodos en graph.json");
            loadSampleData();
            return;
        }
        
        logger.info("Encontrados " + nodesNode.size() + " nodos y " + 
                   (edgesNode != null ? edgesNode.size() : 0) + " aristas");
        
        // Identificar nodos con conflictos
        Set<Integer> nodesWithConflicts = new HashSet<>();
        if (edgesNode != null && edgesNode.isArray()) {
            for (JsonNode edge : edgesNode) {
                JsonNode between = edge.get("between");
                if (between != null && between.isArray()) {
                    for (JsonNode id : between) {
                        nodesWithConflicts.add(id.asInt());
                    }
                }
            }
        }
        
        logger.info("Nodos con conflictos: " + nodesWithConflicts);
        
        // Procesar nodos sin conflictos (asignaciones correctas)
        for (JsonNode node : nodesNode) {
            int id = node.get("id").asInt();
            
            // Incluir solo nodos sin conflictos
            if (!nodesWithConflicts.contains(id)) {
                String day = node.get("day").asText();
                
                // Solo procesar días válidos (Lunes a Sábado)
                if (!assignmentsByDay.containsKey(day)) {
                    continue;
                }
                
                AssignmentInfo info = new AssignmentInfo();
                
                // Información básica
                info.id = id;
                info.groupName = node.get("groupName").asText();
                info.startTime = node.get("startTime").asText();
                info.endTime = node.get("endTime").asText();
                info.sessionType = node.get("sessionType").asText();
                info.enrolledStudents = node.get("enrolledStudents").asInt();
                
                // Información de materia
                JsonNode subject = node.get("subject");
                if (subject != null) {
                    info.subjectName = subject.get("name").asText();
                    info.subjectCode = subject.get("code").asText();
                }
                
                // Información de profesor
                JsonNode professor = node.get("professor");
                if (professor != null) {
                    info.professorName = professor.get("name").asText();
                }
                
                // Información de aula
                JsonNode room = node.get("room");
                if (room != null) {
                    info.roomName = room.get("name").asText();
                    info.isLab = room.get("isLab").asBoolean();
                }
                
                // Añadir a la lista del día correspondiente
                assignmentsByDay.get(day).add(info);
            }
        }
        
        // Mostrar resumen de asignaciones cargadas
        int total = 0;
        for (String day : DAYS) {
            int count = assignmentsByDay.get(day).size();
            logger.info(day + ": " + count + " asignaciones");
            total += count;
        }
        
        logger.info("Total: " + total + " asignaciones cargadas");
        
        // Si no se encontró ninguna asignación, cargar datos de ejemplo
        if (total == 0) {
            logger.warning("No se detectaron asignaciones. Cargando datos de ejemplo...");
            loadSampleData();
        }
    } catch (Exception e) {
        logger.log(Level.SEVERE, "Error al cargar asignaciones", e);
        // En caso de error, cargar datos de ejemplo
        loadSampleData();
    }
}
    
    /**
     * Carga datos de ejemplo para asegurar que siempre haya algo que mostrar
     */
    private void loadSampleData() {
        logger.info("Cargando datos de ejemplo...");
        
        // Limpiar datos existentes
        for (String day : DAYS) {
            assignmentsByDay.get(day).clear();
        }
        
        // Datos de ejemplo para cada día
        createSampleAssignment("Monday", 101, "Grupo A", "Matemáticas I", "MATI", 
                "Juan Pérez", "A101", false, "08:00", "10:00", "D", 25);
        createSampleAssignment("Monday", 102, "Grupo B", "Programación Web", "PROWEB", 
                "María López", "LAB101", true, "10:00", "12:00", "D", 20);
                
        createSampleAssignment("Tuesday", 201, "Grupo C", "Bases de Datos", "BBDD", 
                "Carlos Rodríguez", "LAB102", true, "08:00", "10:00", "D", 30);
        createSampleAssignment("Tuesday", 202, "Grupo D", "Algoritmos", "ALGO", 
                "Ana Martínez", "A102", false, "10:00", "12:00", "D", 28);
                
        createSampleAssignment("Wednesday", 301, "Grupo E", "Inteligencia Artificial", "IA", 
                "Pedro Sánchez", "LAB103", true, "08:00", "10:00", "D", 22);
        createSampleAssignment("Wednesday", 302, "Grupo F", "Sistemas Operativos", "SO", 
                "Laura González", "A103", false, "10:00", "12:00", "D", 26);
                
        createSampleAssignment("Thursday", 401, "Grupo G", "Redes", "REDES", 
                "Diego Fernández", "LAB104", true, "08:00", "10:00", "D", 24);
        createSampleAssignment("Thursday", 402, "Grupo H", "Ingeniería de Software", "IS", 
                "Carmen Díaz", "A104", false, "10:00", "12:00", "D", 27);
                
        createSampleAssignment("Friday", 501, "Grupo I", "Arquitectura de Computadores", "ARQUI", 
                "Roberto Morales", "LAB105", true, "08:00", "10:00", "D", 18);
        createSampleAssignment("Friday", 502, "Grupo J", "Seguridad Informática", "SEGINFO", 
                "Sofía Torres", "A105", false, "10:00", "12:00", "D", 23);
                
        createSampleAssignment("Saturday", 601, "Grupo K", "Diseño de Interfaces", "IU", 
                "Miguel Ruiz", "LAB106", true, "08:00", "10:00", "N", 15);
        createSampleAssignment("Saturday", 602, "Grupo L", "Proyecto Final", "PFINAL", 
                "Julia Vega", "A106", false, "10:00", "12:00", "N", 12);
        
        // Mostrar resumen de datos de ejemplo
        int total = 0;
        for (String day : DAYS) {
            int count = assignmentsByDay.get(day).size();
            logger.info(day + " (ejemplo): " + count + " asignaciones");
            total += count;
        }
        
        logger.info("Total datos de ejemplo: " + total + " asignaciones");
    }
    
    /**
     * Crea una asignación de ejemplo
     */
    private void createSampleAssignment(String day, int id, String groupName, String subjectName, 
                                      String subjectCode, String professorName, String roomName, 
                                      boolean isLab, String startTime, String endTime, 
                                      String sessionType, int enrolledStudents) {
        AssignmentInfo info = new AssignmentInfo();
        info.id = id;
        info.groupName = groupName;
        info.subjectName = subjectName;
        info.subjectCode = subjectCode;
        info.professorName = professorName;
        info.roomName = roomName;
        info.isLab = isLab;
        info.startTime = startTime;
        info.endTime = endTime;
        info.sessionType = sessionType;
        info.enrolledStudents = enrolledStudents;
        
        assignmentsByDay.get(day).add(info);
    }
    
    /**
     * Obtiene las asignaciones para un día específico
     */
    public List<AssignmentInfo> getAssignmentsForDay(String day) {
        return assignmentsByDay.getOrDefault(day, Collections.emptyList());
    }
    
    /**
     * Obtiene todos los días disponibles (Lunes a Sábado)
     */
    public String[] getDays() {
        return DAYS;
    }
    
    /**
     * Obtiene el nombre en español de un día
     */
    public String getDayName(String day) {
        return DAYS_MAP.getOrDefault(day, day);
    }
    
    /**
     * Obtiene el número de asignaciones para un día
     */
    public int getAssignmentCountForDay(String day) {
        return assignmentsByDay.getOrDefault(day, Collections.emptyList()).size();
    }
    
    /**
     * Clase para almacenar la información de una asignación
     */
    public static class AssignmentInfo {
        public int id;
        public String groupName;
        public String subjectName;
        public String subjectCode;
        public String professorName;
        public String roomName;
        public boolean isLab;
        public String startTime;
        public String endTime;
        public String sessionType;
        public int enrolledStudents;
    }
}