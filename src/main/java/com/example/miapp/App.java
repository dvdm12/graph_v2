package com.example.miapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Punto de entrada de la aplicación con logging exhaustivo.
 */
public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        logger.info("==== App START ====");
        String inputPath = "data.json";
        String outputPath = "graph.json";
        logger.debug("Input path: {} | Output path: {}", inputPath, outputPath);

        Path inputFile = Paths.get(inputPath);
        if (!Files.exists(inputFile) || !Files.isReadable(inputFile)) {
            logger.error("No se puede leer el archivo de entrada: {}", inputPath);
            System.exit(1);
        }

        try {
            logger.info("Inicializando ConflictGraphLoader");
            ConflictGraphLoader loader = new ConflictGraphLoader();

            logger.info("Cargando datos desde {}", inputPath);
            loader.loadData(inputPath);
            logger.info("Datos cargados: {} asignaciones", loader.getAssignments().size());

            logger.info("Generando exportación de grafo");
            GraphExporter exporter = new GraphExporter(loader);
            exporter.exportToJson(outputPath);
            logger.info("JSON de grafo exportado exitosamente a {}", outputPath);

            logger.info("==== App END ====");
        } catch (IOException e) {
            logger.error("Error E/S al procesar {}: {}", inputPath, e.getMessage());
            System.exit(2);
        } catch (Exception e) {
            logger.error("Error inesperado en la aplicación", e);
            System.exit(3);
        }
    }
}
