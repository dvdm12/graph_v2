package com.example.miapp;

import com.example.miapp.domain.*;
import com.example.miapp.persistence.DataManagerPersistence;
import com.example.miapp.repository.DataManager;
import com.example.miapp.service.ConflictGraphLoader;
import com.example.miapp.service.GraphExporter;
import com.example.miapp.util.AssignmentBuilder;
import com.example.miapp.exception.DomainException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.logging.Logger;

public class App {
    private static final Logger logger = Logger.getLogger(App.class.getName());
    private static final String DATA_FILE = "data.json";
    private static final String GRAPH_FILE = "graph.json";

    /**
     * Punto de entrada de la aplicación.
     * Esta versión genera asignaciones manualmente, crea un archivo data.json,
     * y produce un grafo de conflictos como salida.
     */
    public static void main(String[] args) {
        logger.info("Iniciando generación manual de asignaciones y detección de conflictos");
        
        try {
            // Obtener instancia del DataManager
            DataManager dataManager = DataManager.getInstance();
            
            // Generar asignaciones manualmente
            generateManualAssignments();
            
            // Guardar asignaciones en formato JSON
            DataManagerPersistence persistence = new DataManagerPersistence();
            persistence.saveToNestedJson(DATA_FILE);
            logger.info("Asignaciones guardadas en: " + DATA_FILE);
            
            // Mostrar resumen de datos generados
            System.out.printf(
                "Datos generados: %d profesores, %d materias, %d aulas, %d asignaciones.%n",
                dataManager.getAllProfessors().size(),
                dataManager.getAllSubjects().size(),
                dataManager.getAllRooms().size(),
                dataManager.getAllAssignments().size()
            );
            
            // Generar grafo de conflictos
            logger.info("Generando grafo de conflictos...");
            ConflictGraphLoader loader = new ConflictGraphLoader();
            loader.loadAllAssignments();
            
            // Exportar grafo a JSON
            GraphExporter exporter = new GraphExporter(loader);
            exporter.exportToJson(GRAPH_FILE);
            
            System.out.println("Grafo de conflictos creado en: " + GRAPH_FILE);
            logger.info("Exportación del grafo completada");
            
            // Opcionalmente, mostrar estadísticas de conflictos
            int totalConflicts = loader.getTotalConflictsCount();
            System.out.println("Total de conflictos detectados: " + totalConflicts);
            
        } catch (IOException | DomainException e) {
            String error = "Error en el proceso: " + e.getMessage();
            logger.severe(error);
            System.err.println("ERROR: " + error);
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Genera asignaciones manualmente para demostrar distintos tipos de conflictos.
     */
    private static void generateManualAssignments() {
        logger.info("Generando asignaciones manualmente...");
        
        DataManager dataManager = DataManager.getInstance();
        LocalDate today = LocalDate.now();
        
        // Crear profesores
        Professor prof1 = new Professor(1, "David Mantilla", "Facultad de Ingeniería", "dmantilla@universidad.edu");
        Professor prof2 = new Professor(2, "María Fernández", "Facultad de Ingeniería", "mfernandez@universidad.edu");
        
        // Crear materias
        Subject subj1 = new Subject("INGREQ", "Ingeniería de Requisitos", "Curso de ingeniería de requisitos", 3, false);
        Subject subj2 = new Subject("BASDAT", "Bases de Datos", "Fundamentos de bases de datos", 4, true);
        Subject subj3 = new Subject("PROINT", "Proyecto Integrador", "Proyecto final integrador", 4, false);
        
        // Asignar materias a profesores
        prof1.assignSubject(subj1);
        prof1.assignSubject(subj3);
        prof2.assignSubject(subj2);
        prof2.assignSubject(subj3);
        
        // Crear aulas
        Room room1 = new Room(1, "A101", 40, false);
        Room room2 = new Room(2, "A102", 35, false);
        Room room3 = new Room(3, "LAB101", 25, true);
        
        // Crear franja bloqueada para prof1
        prof1.addBlockedSlot("Monday", LocalTime.of(8, 0), LocalTime.of(10, 0));
        
        // Registrar entidades en el DataManager
        dataManager.addProfessor(prof1);
        dataManager.addProfessor(prof2);
        dataManager.addSubject(subj1);
        dataManager.addSubject(subj2);
        dataManager.addSubject(subj3);
        dataManager.addRoom(room1);
        dataManager.addRoom(room2);
        dataManager.addRoom(room3);
        
        try {
            // Crear asignaciones con AssignmentBuilder (incluye validación completa)
            
            // Asignación 1: Normal, sin conflictos
            Assignment a1 = AssignmentBuilder.createAssignment(
                1, prof1, subj1, room1, "Tuesday", 
                LocalTime.of(8, 0), LocalTime.of(10, 0),
                101, "Grupo A", "D", 30, today
            );
            
            // Asignación 2: Conflicto con asignación 1 (mismo profesor, aula y hora)
            Assignment a2 = AssignmentBuilder.createAssignment(
                2, prof1, subj1, room1, "Tuesday", 
                LocalTime.of(8, 0), LocalTime.of(10, 0),
                102, "Grupo B", "D", 25, today
            );
            
            // Asignación 3: Conflicto con las anteriores (misma aula y hora)
            Assignment a3 = AssignmentBuilder.createAssignment(
                3, prof2, subj3, room1, "Tuesday", 
                LocalTime.of(8, 0), LocalTime.of(10, 0),
                103, "Grupo C", "D", 28, today
            );
            
            // Asignación 4: Conflicto de sobrecarga (mismo profesor, franjas pesadas consecutivas)
            Assignment a4 = AssignmentBuilder.createAssignment(
                4, prof1, subj3, room2, "Monday", 
                LocalTime.of(16, 0), LocalTime.of(18, 0),
                104, "Grupo D", "D", 20, today
            );
            
            // Asignación 5: Conflicto de sobrecarga con la 4 (franja consecutiva pesada)
            Assignment a5 = AssignmentBuilder.createAssignment(
                5, prof1, subj1, room1, "Monday", 
                LocalTime.of(18, 0), LocalTime.of(20, 0),
                105, "Grupo E", "N", 30, today
            );
            
            // Asignación 6: CON CORRECCIÓN - Materia que requiere laboratorio en un aula de laboratorio
            Assignment a6 = AssignmentBuilder.createAssignment(
                6, prof2, subj2, room3, "Wednesday", // Ahora usando room3 (LAB101) que sí es laboratorio
                LocalTime.of(14, 0), LocalTime.of(16, 0),
                106, "Grupo F", "D", 23, today
            );
            
            // Asignación 7: Sin conflictos (usa aula de lab correctamente)
            Assignment a7 = AssignmentBuilder.createAssignment(
                7, prof2, subj2, room3, "Thursday", 
                LocalTime.of(14, 0), LocalTime.of(16, 0),
                107, "Grupo G", "D", 22, today
            );
            
            // Registrar asignaciones en el DataManager
            dataManager.addAssignment(a1);
            dataManager.addAssignment(a2);
            dataManager.addAssignment(a3);
            dataManager.addAssignment(a4);
            dataManager.addAssignment(a5);
            dataManager.addAssignment(a6);
            dataManager.addAssignment(a7);
            
            logger.info("Todas las asignaciones creadas y registradas con éxito");
            
        } catch (DomainException e) {
            // Esta excepción no debería ocurrir en nuestra configuración controlada
            // pero la capturamos para depuración
            logger.severe("Error generando asignaciones: " + e.getMessage());
            throw e; // Relanzo para que el programa principal la capture
        }
    }
}