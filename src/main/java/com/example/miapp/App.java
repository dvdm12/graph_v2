package com.example.miapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

/**
 * Punto de entrada de la aplicación con rutas de entrada/salida fijas.
 */
public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        String inputPath = "data.json";
        String outputPath = "graph.json";

        Path inputFile = Paths.get(inputPath);
        if (!Files.exists(inputFile) || !Files.isReadable(inputFile)) {
            logger.error("No se puede leer el archivo de entrada: {}", inputPath);
            System.exit(1);
        }

        try {
            ConflictGraphLoader loader = new ConflictGraphLoader();
            loader.loadData(inputPath);

            List<Assignment> all = loader.getAssignments();
            logger.info("Total de asignaciones cargadas: {}", all.size());

            Set<Assignment> free = loader.getConflictFreeAssignments();
            logger.info("Asignaciones sin conflicto: {}", free.size());

            GraphExporter exporter = new GraphExporter(loader);
            exporter.exportToJson(outputPath);
            logger.info("JSON de grafo exportado a {}", outputPath);

        } catch (IOException e) {
            logger.error("Error E/S al procesar {}: {}", inputPath, e.getMessage());
            System.exit(2);
        } catch (Exception e) {
            logger.error("Error inesperado", e);
            System.exit(3);
        }
    }
}
