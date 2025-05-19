package com.example.miapp.ui.main;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileWriter;
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
            // Buscar el archivo graph.json en la ubicación específica
            String baseDir = System.getProperty("user.dir");
            logger.info("Directorio base de la aplicación: " + baseDir);
            
            String[] possiblePaths = {
                // Rutas relativas al directorio de trabajo actual
                "src/main/java/com/example/miapp/ui/main/graph_data/graph.json",
                "com/example/miapp/ui/main/graph_data/graph.json",
                // Rutas relativas al classpath
                "/com/example/miapp/ui/main/graph_data/graph.json",
                // Ruta absoluta (se usa para depuración)
                "C:/graph_project/graph_two/graph/src/main/java/com/example/miapp/ui/main/graph_data/graph.json",
                // Rutas de respaldo
                "graph_data/graph.json",
                "graph.json"
            };
            
            String jsonContent = null;
            String foundPath = null;
            
            // Intentar cargar el archivo desde las rutas especificadas
            for (String path : possiblePaths) {
                File file = new File(path);
                logger.info("Buscando en: " + file.getAbsolutePath());
                
                if (file.exists()) {
                    jsonContent = new String(Files.readAllBytes(Paths.get(path)));
                    foundPath = file.getAbsolutePath();
                    logger.info("¡Archivo encontrado!: " + foundPath);
                    break;
                }
            }
            
            // Creación manual del archivo graph.json si no se encuentra
            if (jsonContent == null) {
                logger.warning("No se pudo encontrar el archivo graph.json. Intentando crear uno en la ubicación actual...");
                
                // Copiar el JSON proporcionado a un archivo local como respaldo
                String jsonFromGraph = "{ \"nodes\" : [ { \"id\" : 3, \"assignmentDate\" : \"2025-05-18\", \"day\" : \"Thursday\", \"startTime\" : \"08:00\", \"endTime\" : \"10:00\", \"groupId\" : 103, \"groupName\" : \"Grupo D\", \"sessionType\" : \"N\", \"enrolledStudents\" : 33, \"professor\" : { \"id\" : 4, \"name\" : \"Catalina Rodríguez Martínez\", \"department\" : \"Facultad de Ingeniería\", \"email\" : \"catalina.rodriguez.martinez@eam.edu\", \"subjects\" : [ { \"code\" : \"DSNMEC\", \"name\" : \"Diseño Mecánico\", \"description\" : \"\", \"credits\" : 3, \"requiresLab\" : false } ], \"blockedSlots\" : [ { \"day\" : \"Thursday\", \"startTime\" : \"18:00\", \"endTime\" : \"22:00\" } ] }, \"room\" : { \"id\" : 4, \"name\" : \"A104\", \"capacity\" : 45, \"isLab\" : false }, \"subject\" : { \"code\" : \"DSNMEC\", \"name\" : \"Diseño Mecánico\", \"description\" : \"\", \"credits\" : 3, \"requiresLab\" : false } }, { \"id\" : 9, \"assignmentDate\" : \"2025-05-18\", \"day\" : \"Thursday\", \"startTime\" : \"10:00\", \"endTime\" : \"12:00\", \"groupId\" : 109, \"groupName\" : \"Grupo J\", \"sessionType\" : \"N\", \"enrolledStudents\" : 34, \"professor\" : { \"id\" : 10, \"name\" : \"Paula Andrea Mendoza Rojas\", \"department\" : \"Facultad de Ingeniería\", \"email\" : \"paula.andrea.mendoza.rojas@eam.edu\", \"subjects\" : [ { \"code\" : \"PLCPRO\", \"name\" : \"Planificación y Control de la Producción\", \"description\" : \"\", \"credits\" : 3, \"requiresLab\" : false } ], \"blockedSlots\" : [ { \"day\" : \"Friday\", \"startTime\" : \"14:00\", \"endTime\" : \"16:00\" } ] }, \"room\" : { \"id\" : 10, \"name\" : \"LAB105\", \"capacity\" : 40, \"isLab\" : true }, \"subject\" : { \"code\" : \"PLCPRO\", \"name\" : \"Planificación y Control de la Producción\", \"description\" : \"\", \"credits\" : 3, \"requiresLab\" : false } }, { \"id\" : 2, \"assignmentDate\" : \"2025-05-18\", \"day\" : \"Wednesday\", \"startTime\" : \"08:00\", \"endTime\" : \"10:00\", \"groupId\" : 102, \"groupName\" : \"Grupo C\", \"sessionType\" : \"D\", \"enrolledStudents\" : 7, \"professor\" : { \"id\" : 3, \"name\" : \"Juan Camilo Pérez Torres\", \"department\" : \"Facultad de Ingeniería\", \"email\" : \"juan.camilo.perez.torres@eam.edu\", \"subjects\" : [ { \"code\" : \"ROBIIO\", \"name\" : \"Robótica II\", \"description\" : \"\", \"credits\" : 3, \"requiresLab\" : true } ], \"blockedSlots\" : [ { \"day\" : \"Wednesday\", \"startTime\" : \"16:00\", \"endTime\" : \"18:00\" } ] }, \"room\" : { \"id\" : 8, \"name\" : \"LAB103\", \"capacity\" : 20, \"isLab\" : true }, \"subject\" : { \"code\" : \"ROBIIO\", \"name\" : \"Robótica II\", \"description\" : \"\", \"credits\" : 3, \"requiresLab\" : true } }, { \"id\" : 8, \"assignmentDate\" : \"2025-05-18\", \"day\" : \"Wednesday\", \"startTime\" : \"10:00\", \"endTime\" : \"12:00\", \"groupId\" : 108, \"groupName\" : \"Grupo I\", \"sessionType\" : \"D\", \"enrolledStudents\" : 28, \"professor\" : { \"id\" : 9, \"name\" : \"Diego Alejandro Vargas Díaz\", \"department\" : \"Facultad de Ingeniería\", \"email\" : \"diego.alejandro.vargas.diaz@eam.edu\", \"subjects\" : [ { \"code\" : \"CONAPPM\", \"name\" : \"Construcción de APP Móviles\", \"description\" : \"\", \"credits\" : 3, \"requiresLab\" : false } ], \"blockedSlots\" : [ { \"day\" : \"Thursday\", \"startTime\" : \"08:00\", \"endTime\" : \"12:00\" } ] }, \"room\" : { \"id\" : 9, \"name\" : \"LAB104\", \"capacity\" : 35, \"isLab\" : true }, \"subject\" : { \"code\" : \"CONAPPM\", \"name\" : \"Construcción de APP Móviles\", \"description\" : \"\", \"credits\" : 3, \"requiresLab\" : false } }, { \"id\" : 1, \"assignmentDate\" : \"2025-05-18\", \"day\" : \"Tuesday\", \"startTime\" : \"08:00\", \"endTime\" : \"10:00\", \"groupId\" : 101, \"groupName\" : \"Grupo B\", \"sessionType\" : \"N\", \"enrolledStudents\" : 21, \"professor\" : { \"id\" : 2, \"name\" : \"María Fernanda López Gómez\", \"department\" : \"Facultad de Ingeniería\", \"email\" : \"maria.fernanda.lopez.gomez@eam.edu\", \"subjects\" : [ { \"code\" : \"TOPGEO\", \"name\" : \"Topología y Geometría\", \"description\" : \"\", \"credits\" : 3, \"requiresLab\" : false } ], \"blockedSlots\" : [ { \"day\" : \"Tuesday\", \"startTime\" : \"14:00\", \"endTime\" : \"16:00\" } ] }, \"room\" : { \"id\" : 2, \"name\" : \"A102\", \"capacity\" : 35, \"isLab\" : false }, \"subject\" : { \"code\" : \"TOPGEO\", \"name\" : \"Topología y Geometría\", \"description\" : \"\", \"credits\" : 3, \"requiresLab\" : false } } ], \"edges\" : [ { \"between\" : [ 4, 10 ], \"conflicts\" : [ \"Misma jornada y horario\" ] }, { \"between\" : [ 3, 9 ], \"conflicts\" : [ \"Misma jornada y horario\" ] }, { \"between\" : [ 5, 6 ], \"conflicts\" : [ \"Misma jornada y horario\" ] } ] }";
                
                try {
                    // Escribir el JSON en un archivo local
                    File localGraphJson = new File("graph.json");
                    try (FileWriter writer = new FileWriter(localGraphJson)) {
                        writer.write(jsonFromGraph);
                    }
                    
                    logger.info("Archivo graph.json creado en: " + localGraphJson.getAbsolutePath());
                    jsonContent = jsonFromGraph;
                    foundPath = localGraphJson.getAbsolutePath();
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error al crear archivo graph.json local", e);
                }
            }
            
            if (jsonContent == null) {
                logger.severe("No se pudo encontrar ni crear el archivo graph.json");
                return;
            }
            
            logger.info("Archivo graph.json cargado desde: " + foundPath);
            
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