package com.example.miapp;

import com.example.miapp.domain.*;
import com.example.miapp.repository.DataManager;
import com.example.miapp.service.ConflictGraphLoader;
import com.example.miapp.service.GraphExporter;
import com.example.miapp.persistence.DataManagerPersistence;
import com.example.miapp.exception.DomainException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Clase principal que contiene el punto de entrada a la aplicación.
 * Modificada para crear datos de ejemplo directamente con objetos del dominio
 * y opcionalmente cargar desde un archivo data.json existente.
 */
public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);
    
    // Nombres de archivo específicos
    private static final String DATA_JSON = "data.json";
    private static final String GRAPH_JSON = "graph.json";
    
    /**
     * Punto de entrada principal de la aplicación.
     * 
     * Uso:
     * 1. Sin argumentos: crea datos de ejemplo y genera graph.json
     * 2. Con argumento "load": carga desde data.json existente
     * 
     * Argumentos opcionales:
     * args[0]: "create" para crear datos de ejemplo, "load" para cargar desde archivo
     * args[1]: Ruta al archivo data.json (por defecto: "data.json" en el directorio actual)
     * args[2]: Ruta para el archivo graph.json de salida (por defecto: "graph.json" en el directorio actual)
     */
    public static void main(String[] args) {
        logger.info("Iniciando aplicación...");
        
        // Determinar modo de operación
        String mode = args.length > 0 ? args[0] : "create";
        String dataFile = args.length > 1 ? args[1] : DATA_JSON;
        String graphFile = args.length > 2 ? args[2] : GRAPH_JSON;
        
        try {
            // Crear instancia de DataManagerPersistence
            DataManagerPersistence persistence = new DataManagerPersistence();
            
            if ("load".equalsIgnoreCase(mode)) {
                // Verificar si existe el archivo data.json
                if (!Files.exists(Paths.get(dataFile))) {
                    logger.error("No se encontró el archivo {}. Por favor asegúrese de que el archivo existe.", dataFile);
                    System.err.println("ERROR: No se encontró el archivo " + dataFile);
                    return;
                }
                
                logger.info("Cargando datos desde: {}", dataFile);
                System.out.println("Cargando datos desde: " + dataFile);
                
                // Cargar datos existentes desde data.json
                persistence.loadFromNestedJson(dataFile);
            } else {
                // Crear datos de ejemplo directamente con objetos del dominio
                logger.info("Creando datos de ejemplo en memoria...");
                System.out.println("Creando datos de ejemplo en memoria...");
                
                createSampleData();
                
                // Guardar los datos creados en data.json
                logger.info("Guardando datos en: {}", dataFile);
                System.out.println("Guardando datos en: " + dataFile);
                persistence.saveToNestedJson(dataFile);
            }
            
            // Verificar que se cargaron correctamente los datos
            DataManager dataManager = DataManager.getInstance();
            int professorCount = dataManager.getAllProfessors().size();
            int subjectCount = dataManager.getAllSubjects().size(); 
            int roomCount = dataManager.getAllRooms().size();
            int assignmentCount = dataManager.getAllAssignments().size();
            
            logger.info("Datos disponibles: {} profesores, {} materias, {} aulas, {} asignaciones",
                       professorCount, subjectCount, roomCount, assignmentCount);
            
            System.out.println("Datos disponibles:");
            System.out.println("- Profesores: " + professorCount);
            System.out.println("- Materias: " + subjectCount);
            System.out.println("- Aulas: " + roomCount);
            System.out.println("- Asignaciones: " + assignmentCount);
            
            // Procesar y exportar el grafo de conflictos a graph.json
            exportConflictGraph(graphFile);
            
            logger.info("Proceso completado correctamente.");
            System.out.println("\nProceso completado correctamente.");
            System.out.println("El archivo de conflictos ha sido generado en: " + graphFile);
            
        } catch (Exception e) {
            logger.error("Error en la ejecución de la aplicación: {}", e.getMessage(), e);
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
 * Crea un conjunto de datos de ejemplo directamente con objetos del dominio.
 * Esta versión crea asignaciones directamente sin usar AssignmentBuilder para permitir
 * la creación de asignaciones con conflictos para pruebas.
 */
private static void createSampleData() {
    DataManager dataManager = DataManager.getInstance();
    
    // Limpiar datos previos
    dataManager.clearAll();
    
    // Crear materias
    Subject calculus = new Subject("MAT101", "Cálculo I", "Cálculo diferencial e integral", 4, false);
    Subject algebra = new Subject("MAT102", "Álgebra Lineal", "Matrices y sistemas lineales", 3, false);
    Subject statistics = new Subject("MAT103", "Estadística", "Estadística descriptiva e inferencial", 3, false);
    Subject programming = new Subject("CS101", "Programación Básica", "Introducción a la programación", 4, true);
    Subject databases = new Subject("CS102", "Bases de Datos", "Fundamentos de bases de datos", 4, false);
    Subject networks = new Subject("CS103", "Redes", "Fundamentos de redes", 3, true);
    
    dataManager.addSubject(calculus);
    dataManager.addSubject(algebra);
    dataManager.addSubject(statistics);
    dataManager.addSubject(programming);
    dataManager.addSubject(databases);
    dataManager.addSubject(networks);
    
    // Crear aulas
    Room lab101 = new Room(1, "LAB101", 30, true);
    Room lab102 = new Room(2, "LAB102", 25, true);
    Room lab103 = new Room(3, "LAB103", 35, true);
    Room room101 = new Room(4, "A101", 40, false);
    Room room102 = new Room(5, "A102", 35, false);
    Room room103 = new Room(6, "A103", 50, false);
    
    dataManager.addRoom(lab101);
    dataManager.addRoom(lab102);
    dataManager.addRoom(lab103);
    dataManager.addRoom(room101);
    dataManager.addRoom(room102);
    dataManager.addRoom(room103);
    
    // Crear profesores
    Professor profJuan = new Professor(1, "Dr. Juan Pérez", "Informática", "jperez@universidad.edu");
    profJuan.assignSubject(programming);
    profJuan.assignSubject(databases);
    profJuan.addBlockedSlot("Monday", LocalTime.of(9, 0), LocalTime.of(11, 0));
    profJuan.addBlockedSlot("Wednesday", LocalTime.of(14, 0), LocalTime.of(16, 0));
    
    Professor profMaria = new Professor(2, "Dra. María Rodríguez", "Matemáticas", "mrodriguez@universidad.edu");
    profMaria.assignSubject(calculus);
    profMaria.assignSubject(algebra);
    profMaria.assignSubject(statistics);
    profMaria.addBlockedSlot("Tuesday", LocalTime.of(12, 0), LocalTime.of(14, 0));
    
    Professor profCarlos = new Professor(3, "Dr. Carlos López", "Ciencias", "clopez@universidad.edu");
    profCarlos.assignSubject(programming);
    profCarlos.assignSubject(networks);
    profCarlos.addBlockedSlot("Thursday", LocalTime.of(15, 0), LocalTime.of(18, 0));
    
    dataManager.addProfessor(profJuan);
    dataManager.addProfessor(profMaria);
    dataManager.addProfessor(profCarlos);
    
    // Definir fecha común de asignaciones
    LocalDate assignmentDate = LocalDate.of(2025, 5, 13);
    
    // Lista para ir acumulando asignaciones creadas y manejar errores individualmente
    List<Assignment> createdAssignments = new ArrayList<>();
    
    // Crear asignaciones directamente (sin usar AssignmentBuilder para permitir conflictos)
    try {
        // Asignación #1 - con conflicto de profesor-materia y franja bloqueada
        try {
            Assignment a1 = new Assignment.Builder()
                .id(1)
                .assignmentDate(assignmentDate)
                .professor(profJuan)
                .subject(calculus)
                .room(lab101)
                .day("Monday")
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0))  // Franja válida: 8-12
                .groupId(101)
                .groupName("Grupo A")
                .sessionType("D")
                .enrolledStudents(25)
                .build();
            
            dataManager.addAssignment(a1);
            createdAssignments.add(a1);
            System.out.println("Creada asignación #1 con éxito");
        } catch (Exception e) {
            System.err.println("Error al crear asignación #1: " + e.getMessage());
        }
        
        // Asignación #2 - conflicto con asignación #1 (mismo grupo, profesor)
        try {
            Assignment a2 = new Assignment.Builder()
                .id(2)
                .assignmentDate(assignmentDate)
                .professor(profJuan)
                .subject(databases)
                .room(room101)
                .day("Monday")
                .startTime(LocalTime.of(10, 0))  // Cambiado para estar dentro de una franja válida
                .endTime(LocalTime.of(12, 0))    // Franja válida: 8-12
                .groupId(101)
                .groupName("Grupo A")
                .sessionType("D")
                .enrolledStudents(30)
                .build();
            
            dataManager.addAssignment(a2);
            createdAssignments.add(a2);
            System.out.println("Creada asignación #2 con éxito");
        } catch (Exception e) {
            System.err.println("Error al crear asignación #2: " + e.getMessage());
        }
        
        // Asignación #3 - conflicto con franja bloqueada del profesor
        try {
            Assignment a3 = new Assignment.Builder()
                .id(3)
                .assignmentDate(assignmentDate)
                .professor(profJuan)
                .subject(programming)
                .room(lab101)
                .day("Wednesday")
                .startTime(LocalTime.of(14, 0))  // Franja válida: 14-16
                .endTime(LocalTime.of(16, 0))
                .groupId(102)
                .groupName("Grupo B")
                .sessionType("D")
                .enrolledStudents(28)
                .build();
            
            dataManager.addAssignment(a3);
            createdAssignments.add(a3);
            System.out.println("Creada asignación #3 con éxito");
        } catch (Exception e) {
            System.err.println("Error al crear asignación #3: " + e.getMessage());
        }
        
        // Asignación #4 - conflicto con franja bloqueada y con asignación #1 (misma aula)
        try {
            Assignment a4 = new Assignment.Builder()
                .id(4)
                .assignmentDate(assignmentDate)
                .professor(profJuan)
                .subject(programming)
                .room(lab101)
                .day("Monday")
                .startTime(LocalTime.of(8, 0))   // Franja válida: 8-12
                .endTime(LocalTime.of(10, 0))
                .groupId(103)
                .groupName("Grupo C")
                .sessionType("D")
                .enrolledStudents(22)
                .build();
            
            dataManager.addAssignment(a4);
            createdAssignments.add(a4);
            System.out.println("Creada asignación #4 con éxito");
        } catch (Exception e) {
            System.err.println("Error al crear asignación #4: " + e.getMessage());
        }
        
        // Asignación #5 - conflicto de aula incompatible con requisitos de materia
        try {
            Assignment a5 = new Assignment.Builder()
                .id(5)
                .assignmentDate(assignmentDate)
                .professor(profMaria)
                .subject(networks)
                .room(room101)
                .day("Tuesday")
                .startTime(LocalTime.of(14, 0))  // Franja válida: 14-16
                .endTime(LocalTime.of(16, 0))
                .groupId(201)
                .groupName("Grupo D")
                .sessionType("D")
                .enrolledStudents(35)
                .build();
            
            dataManager.addAssignment(a5);
            createdAssignments.add(a5);
            System.out.println("Creada asignación #5 con éxito");
        } catch (Exception e) {
            System.err.println("Error al crear asignación #5: " + e.getMessage());
        }
        
        // Asignación #6 - sin conflictos
        try {
            Assignment a6 = new Assignment.Builder()
                .id(6)
                .assignmentDate(assignmentDate)
                .professor(profMaria)
                .subject(algebra)
                .room(room102)
                .day("Tuesday")
                .startTime(LocalTime.of(14, 0))  // Franja válida: 14-16
                .endTime(LocalTime.of(16, 0))
                .groupId(202)
                .groupName("Grupo E")
                .sessionType("D")
                .enrolledStudents(30)
                .build();
            
            dataManager.addAssignment(a6);
            createdAssignments.add(a6);
            System.out.println("Creada asignación #6 con éxito");
        } catch (Exception e) {
            System.err.println("Error al crear asignación #6: " + e.getMessage());
        }
        
        // Asignación #7 - conflicto con asignación #6 (mismo profesor, solapamiento)
        try {
            Assignment a7 = new Assignment.Builder()
                .id(7)
                .assignmentDate(assignmentDate)
                .professor(profMaria)
                .subject(statistics)
                .room(room103)
                .day("Tuesday")
                .startTime(LocalTime.of(16, 0))  // Franja válida: 16-18
                .endTime(LocalTime.of(18, 0))
                .groupId(203)
                .groupName("Grupo F")
                .sessionType("D")
                .enrolledStudents(40)
                .build();
            
            dataManager.addAssignment(a7);
            createdAssignments.add(a7);
            System.out.println("Creada asignación #7 con éxito");
        } catch (Exception e) {
            System.err.println("Error al crear asignación #7: " + e.getMessage());
        }
        
        // Asignación #8 - conflicto con asignación #6 (misma aula)
        try {
            Assignment a8 = new Assignment.Builder()
                .id(8)
                .assignmentDate(assignmentDate)
                .professor(profCarlos)
                .subject(statistics)
                .room(room102)
                .day("Tuesday")
                .startTime(LocalTime.of(14, 0))  // Franja válida: 14-16
                .endTime(LocalTime.of(16, 0))
                .groupId(204)
                .groupName("Grupo G")
                .sessionType("D")
                .enrolledStudents(30)
                .build();
            
            dataManager.addAssignment(a8);
            createdAssignments.add(a8);
            System.out.println("Creada asignación #8 con éxito");
        } catch (Exception e) {
            System.err.println("Error al crear asignación #8: " + e.getMessage());
        }
        
        // Asignación #9 - conflicto con franja bloqueada
        try {
            Assignment a9 = new Assignment.Builder()
                .id(9)
                .assignmentDate(assignmentDate)
                .professor(profCarlos)
                .subject(programming)
                .room(lab102)
                .day("Thursday")
                .startTime(LocalTime.of(16, 0))  // Franja válida: 16-18
                .endTime(LocalTime.of(18, 0))
                .groupId(301)
                .groupName("Grupo H")
                .sessionType("D")
                .enrolledStudents(20)
                .build();
            
            dataManager.addAssignment(a9);
            createdAssignments.add(a9);
            System.out.println("Creada asignación #9 con éxito");
        } catch (Exception e) {
            System.err.println("Error al crear asignación #9: " + e.getMessage());
        }
        
        // Asignación #10 - materia sin autorización
        try {
            Assignment a10 = new Assignment.Builder()
                .id(10)
                .assignmentDate(assignmentDate)
                .professor(profCarlos)
                .subject(calculus)
                .room(lab103)
                .day("Monday")
                .startTime(LocalTime.of(14, 0))  // Franja válida: 14-16
                .endTime(LocalTime.of(16, 0))
                .groupId(302)
                .groupName("Grupo I")
                .sessionType("D")
                .enrolledStudents(30)
                .build();
            
            dataManager.addAssignment(a10);
            createdAssignments.add(a10);
            System.out.println("Creada asignación #10 con éxito");
        } catch (Exception e) {
            System.err.println("Error al crear asignación #10: " + e.getMessage());
        }
        
        // Asignación #11 - conflicto con asignación #7 (mismo grupo)
        try {
            Assignment a11 = new Assignment.Builder()
                .id(11)
                .assignmentDate(assignmentDate)
                .professor(profCarlos)
                .subject(statistics)
                .room(lab102)
                .day("Tuesday")
                .startTime(LocalTime.of(16, 0))  // Franja válida: 16-18
                .endTime(LocalTime.of(18, 0))
                .groupId(203)
                .groupName("Grupo F")
                .sessionType("D")
                .enrolledStudents(20)
                .build();
            
            dataManager.addAssignment(a11);
            createdAssignments.add(a11);
            System.out.println("Creada asignación #11 con éxito");
        } catch (Exception e) {
            System.err.println("Error al crear asignación #11: " + e.getMessage());
        }
        
        // Asignación #12 - conflictos múltiples
        try {
            Assignment a12 = new Assignment.Builder()
                .id(12)
                .assignmentDate(assignmentDate)
                .professor(profJuan)
                .subject(programming)
                .room(lab101)
                .day("Monday")
                .startTime(LocalTime.of(10, 0))  // Franja válida: 8-12
                .endTime(LocalTime.of(12, 0))
                .groupId(104)
                .groupName("Grupo J")
                .sessionType("N")
                .enrolledStudents(25)
                .build();
            
            dataManager.addAssignment(a12);
            createdAssignments.add(a12);
            System.out.println("Creada asignación #12 con éxito");
        } catch (Exception e) {
            System.err.println("Error al crear asignación #12: " + e.getMessage());
        }
        
        logger.info("Creadas {} asignaciones de prueba con éxito", createdAssignments.size());
        System.out.println("Creadas " + createdAssignments.size() + " asignaciones de prueba con éxito");
        
    } catch (Exception e) {
        logger.error("Error general al crear asignaciones: {}", e.getMessage(), e);
        System.err.println("ERROR general al crear asignaciones: " + e.getMessage());
    }
}
    
    /**
     * Procesa las asignaciones y exporta el grafo de conflictos.
     * 
     * @param outputFile Ruta del archivo de salida para el grafo de conflictos
     */
    private static void exportConflictGraph(String outputFile) {
        logger.info("Procesando asignaciones para detectar conflictos...");
        System.out.println("\nProcesando asignaciones para detectar conflictos...");
        
        // Crear el gestor de grafo de conflictos
        ConflictGraphLoader graphLoader = new ConflictGraphLoader();
        
        // Cargar todas las asignaciones del DataManager
        graphLoader.loadAllAssignments();
        
        // Crear el exportador de grafos
        GraphExporter exporter = new GraphExporter(graphLoader);
        
        try {
            // Exportar el grafo a graph.json
            exporter.exportToJson(outputFile);
            
            logger.info("Grafo de conflictos exportado a: {}", outputFile);
            
            // Mostrar en consola un resumen de los conflictos
            int totalAssignments = graphLoader.getAllAssignments().size();
            int totalConflicts = graphLoader.getTotalConflictsCount();
            int conflictFreeAssignments = graphLoader.getConflictFreeAssignments().size();
            
            logger.info("======= RESUMEN DE CONFLICTOS =======");
            logger.info("Total de asignaciones: {}", totalAssignments);
            logger.info("Total de conflictos detectados: {}", totalConflicts);
            logger.info("Asignaciones sin conflictos: {}", conflictFreeAssignments);
            
            System.out.println("\n======= RESUMEN DE CONFLICTOS =======");
            System.out.println("Total de asignaciones: " + totalAssignments);
            System.out.println("Total de conflictos detectados: " + totalConflicts);
            System.out.println("Asignaciones sin conflictos: " + conflictFreeAssignments);
            
            // Mostrar estadísticas por tipo de conflicto
            System.out.println("\n--- Distribución por tipo de conflicto ---");
            logger.info("--- Distribución por tipo de conflicto ---");
            
            graphLoader.getConflictStatistics().forEach((type, count) -> {
                logger.info("  {} - {}", type.getLabel(), count);
                System.out.println("  " + type.getLabel() + " - " + count);
            });
            
        } catch (IOException e) {
            logger.error("Error al exportar el grafo de conflictos: {}", e.getMessage(), e);
            System.err.println("ERROR al exportar el grafo de conflictos: " + e.getMessage());
        }
    }
}