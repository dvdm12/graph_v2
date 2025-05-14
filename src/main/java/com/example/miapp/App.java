package com.example.miapp;

import com.example.miapp.repository.DataManager;
import com.example.miapp.service.ConflictGraphLoader;
import com.example.miapp.service.GraphExporter;
import com.example.miapp.persistence.DataManagerPersistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Clase principal que contiene el punto de entrada a la aplicación.
 * Modificada para cargar datos desde un archivo data.json existente
 * y generar el archivo graph.json con los conflictos detectados.
 */
public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);
    
    // Nombres de archivo específicos (se pueden cambiar aquí si es necesario)
    private static final String DATA_JSON = "data.json";
    private static final String GRAPH_JSON = "graph.json";
    
    /**
     * Punto de entrada principal de la aplicación.
     * 
     * Uso:
     * 1. Asegúrese de que existe un archivo data.json en el directorio de trabajo
     * 2. El programa cargará los datos y generará el archivo graph.json con los conflictos
     * 
     * Argumentos opcionales:
     * args[0]: Ruta al archivo data.json (por defecto: "data.json" en el directorio actual)
     * args[1]: Ruta para el archivo graph.json de salida (por defecto: "graph.json" en el directorio actual)
     */
    public static void main(String[] args) {
        logger.info("Iniciando aplicación...");
        
        // Procesar argumentos si se proporcionan
        String dataFile = args.length > 0 ? args[0] : DATA_JSON;
        String graphFile = args.length > 1 ? args[1] : GRAPH_JSON;
        
        try {
            // Verificar si existe el archivo data.json
            if (!Files.exists(Paths.get(dataFile))) {
                logger.error("No se encontró el archivo {}. Por favor asegúrese de que el archivo existe.", dataFile);
                System.err.println("ERROR: No se encontró el archivo " + dataFile);
                return;
            }
            
            logger.info("Cargando datos desde: {}", dataFile);
            System.out.println("Cargando datos desde: " + dataFile);
            
            // Crear instancia de DataManagerPersistence para cargar el formato anidado
            DataManagerPersistence persistence = new DataManagerPersistence();
            
            // Cargar datos existentes desde data.json
            persistence.loadFromNestedJson(dataFile);
            
            // Verificar que se cargaron correctamente los datos
            DataManager dataManager = DataManager.getInstance();
            int professorCount = dataManager.getAllProfessors().size();
            int subjectCount = dataManager.getAllSubjects().size(); 
            int roomCount = dataManager.getAllRooms().size();
            int assignmentCount = dataManager.getAllAssignments().size();
            
            logger.info("Datos cargados correctamente: {} profesores, {} materias, {} aulas, {} asignaciones",
                       professorCount, subjectCount, roomCount, assignmentCount);
            
            System.out.println("Datos cargados correctamente:");
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