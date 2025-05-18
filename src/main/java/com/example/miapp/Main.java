package com.example.miapp;

import com.example.miapp.persistence.DataManagerPersistence;
import com.example.miapp.repository.DataManager;
import com.example.miapp.service.ConflictGraphLoader;
import com.example.miapp.service.GraphExporter;
import com.example.miapp.exception.DomainException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.logging.Logger;

public class Main {
    private static final Logger logger = Logger.getLogger(App.class.getName());
    private static final String DEFAULT_DATA_FILE = "data.json";
    private static final String DEFAULT_GRAPH_FILE = "graph.json";

    /**
     * Punto de entrada de la aplicación.
     * Uso: java -jar app.jar [<ruta_data.json> <ruta_graph.json>]
     * - Entrada: archivo JSON con asignaciones (por defecto: data.json).
     * - Salida: archivo JSON para grafo de conflictos (por defecto: graph.json).
     */
    public static void main(String[] args) {
        // Procesar argumentos de línea de comandos
        String dataFile = args.length >= 1 ? args[0] : DEFAULT_DATA_FILE;
        String graphFile = args.length == 2 ? args[1] : DEFAULT_GRAPH_FILE;

        logger.info("Archivo de entrada: " + dataFile);
        logger.info("Archivo de salida: " + graphFile);

        // Verificar que el archivo de entrada existe
        Path dataPath = Paths.get(dataFile);
        if (!Files.exists(dataPath) || !Files.isRegularFile(dataPath)) {
            String msg = "No se encontró o no es un archivo válido: " + dataFile;
            logger.severe(msg);
            System.err.println("ERROR: " + msg);
            System.exit(1);
        }

        try {
            // Cargar datos desde JSON
            DataManagerPersistence persistence = new DataManagerPersistence();
            persistence.loadFromNestedJson(dataFile);

            // Mostrar resumen de datos
            DataManager dataManager = DataManager.getInstance();
            System.out.printf(
                "Datos cargados: %d profesores, %d materias, %d aulas, %d asignaciones.%n",
                dataManager.getAllProfessors().size(),
                dataManager.getAllSubjects().size(),
                dataManager.getAllRooms().size(),
                dataManager.getAllAssignments().size()
            );

            // Generar grafo de conflictos
            logger.info("Generando grafo de conflictos...");
            ConflictGraphLoader loader = new ConflictGraphLoader();
            loader.loadAllAssignments();

            // Preparar directorio de salida si es necesario
            Path graphPath = Paths.get(graphFile);
            if (graphPath.getParent() != null && !Files.exists(graphPath.getParent())) {
                Files.createDirectories(graphPath.getParent());
            }

            // Exportar grafo a JSON
            GraphExporter exporter = new GraphExporter(loader);
            exporter.exportToJson(graphFile);

            // Mostrar estadísticas de conflictos
            int totalConflicts = loader.getTotalConflictsCount();
            System.out.println("Grafo de conflictos creado en: " + graphFile);
            System.out.println("Total de conflictos detectados: " + totalConflicts);
            
            // Opcional: mostrar estadísticas más detalladas
            System.out.println("\nEstadísticas de conflictos por tipo:");
            loader.getConflictStatistics().forEach((type, count) -> 
                System.out.printf("- %s: %d%n", type.getLabel(), count)
            );
            
            logger.info("Análisis de conflictos completado con éxito.");

        } catch (IOException | DomainException e) {
            String error = "Error al procesar archivos: " + e.getMessage();
            logger.severe(error);
            System.err.println("ERROR: " + error);
            e.printStackTrace();
            System.exit(1);
        }
    }
}