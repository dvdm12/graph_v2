package com.example.miapp.repository;

import com.example.miapp.domain.*;
import com.example.miapp.exception.DomainException;
import com.example.miapp.persistence.DataManagerPersistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Gestor centralizado de datos en memoria con patrón Singleton.
 * Mantiene las colecciones de todas las entidades del sistema durante el tiempo de ejecución.
 */
public class DataManager {
    private static final Logger logger = LoggerFactory.getLogger(DataManager.class);
    
    // Instancia única del Singleton
    private static DataManager instance;
    
    // Colecciones de entidades (thread-safe)
    private final Map<Integer, Professor> professors = new ConcurrentHashMap<>();
    private final Map<String, Subject> subjects = new ConcurrentHashMap<>();
    private final Map<Integer, Room> rooms = new ConcurrentHashMap<>();
    private final Map<Integer, Assignment> assignments = new ConcurrentHashMap<>();
    
    /**
     * Constructor privado para el patrón Singleton.
     */
    private DataManager() {
        initializeDefaultData();
        logger.info("DataManager inicializado con datos por defecto");
    }
    
    /**
     * Obtiene la instancia única del DataManager.
     */
    public static synchronized DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
    }
    
    /**
     * Inicializa el sistema con algunos datos predeterminados.
     */
    private void initializeDefaultData() {
        // Inicializar algunas materias predefinidas
        initializeDefaultSubjects();
        
        // Inicializar algunas aulas predefinidas
        initializeDefaultRooms();
        
        // Podríamos inicializar profesores, etc. si fuera necesario
    }
    
    /**
     * Inicializa algunas materias predefinidas.
     */
    private void initializeDefaultSubjects() {
        // Matemáticas
        addSubject(new Subject("MAT101", "Cálculo I", "Introducción al cálculo diferencial e integral", 4, false));
        addSubject(new Subject("MAT102", "Álgebra Lineal", "Vectores, matrices y sistemas lineales", 3, false));
        addSubject(new Subject("MAT201", "Ecuaciones Diferenciales", "EDOs y sistemas diferenciales", 4, false));
        
        // Informática
        addSubject(new Subject("CS101", "Programación Básica", "Fundamentos de programación", 4, true));
        addSubject(new Subject("CS102", "Estructuras de Datos", "Algoritmos y estructuras de datos", 4, true));
        addSubject(new Subject("CS103", "Algoritmos", "Análisis y diseño de algoritmos", 3, false));
        addSubject(new Subject("CS104", "Bases de Datos", "Diseño y gestión de bases de datos", 4, true));
        addSubject(new Subject("CS105", "Redes", "Fundamentos de redes de computadoras", 3, true));
        
        // Física
        addSubject(new Subject("PHY101", "Física General", "Mecánica clásica y termodinámica", 4, true));
        addSubject(new Subject("PHY102", "Electricidad y Magnetismo", "Fundamentos de electromagnetismo", 4, true));
    }
    
    /**
     * Inicializa algunas aulas predefinidas.
     */
    private void initializeDefaultRooms() {
        // Aulas normales
        addRoom(new Room("A101", 40, false));
        addRoom(new Room("A102", 35, false));
        addRoom(new Room("A103", 30, false));
        addRoom(new Room("A104", 45, false));
        addRoom(new Room("A105", 50, false));
        
        // Laboratorios
        addRoom(new Room("LAB101", 25, true));
        addRoom(new Room("LAB102", 30, true));
        addRoom(new Room("LAB103", 20, true));
        addRoom(new Room("LAB104", 35, true));
        addRoom(new Room("LAB105", 40, true));
    }
    
    // ===== Métodos para gestionar profesores =====
    
    /**
     * Añade un profesor al sistema.
     */
    public void addProfessor(Professor professor) {
        Objects.requireNonNull(professor, "El profesor no puede ser null");
        professors.put(professor.getId(), professor);
        logger.debug("Profesor añadido: id={}, nombre={}", professor.getId(), professor.getName());
    }
    
    /**
     * Elimina un profesor del sistema.
     */
    public boolean removeProfessor(int id) {
        Professor removed = professors.remove(id);
        if (removed != null) {
            logger.debug("Profesor eliminado: id={}, nombre={}", id, removed.getName());
            return true;
        }
        return false;
    }
    
    /**
     * Obtiene un profesor por su ID.
     */
    public Professor getProfessor(int id) {
        return professors.get(id);
    }
    
    /**
     * Obtiene todos los profesores.
     */
    public List<Professor> getAllProfessors() {
        return new ArrayList<>(professors.values());
    }
    
    // ===== Métodos para gestionar materias =====
    
    /**
     * Añade una materia al sistema.
     */
    public void addSubject(Subject subject) {
        Objects.requireNonNull(subject, "La materia no puede ser null");
        subjects.put(subject.getCode(), subject);
        logger.debug("Materia añadida: código={}, nombre={}", subject.getCode(), subject.getName());
    }
    
    /**
     * Elimina una materia del sistema.
     */
    public boolean removeSubject(String code) {
        Subject removed = subjects.remove(code);
        if (removed != null) {
            logger.debug("Materia eliminada: código={}, nombre={}", code, removed.getName());
            return true;
        }
        return false;
    }
    
    /**
     * Obtiene una materia por su código.
     */
    public Subject getSubject(String code) {
        return subjects.get(code);
    }
    
    /**
     * Obtiene todas las materias.
     */
    public List<Subject> getAllSubjects() {
        return new ArrayList<>(subjects.values());
    }
    
    // ===== Métodos para gestionar aulas =====
    
    /**
     * Añade un aula al sistema.
     */
    public void addRoom(Room room) {
        Objects.requireNonNull(room, "El aula no puede ser null");
        rooms.put(room.getId(), room);
        logger.debug("Aula añadida: id={}, nombre={}, esLab={}", 
                   room.getId(), room.getName(), room.isLab());
    }
    
    /**
     * Elimina un aula del sistema.
     */
    public boolean removeRoom(int id) {
        Room removed = rooms.remove(id);
        if (removed != null) {
            logger.debug("Aula eliminada: id={}, nombre={}", id, removed.getName());
            return true;
        }
        return false;
    }
    
    /**
     * Obtiene un aula por su ID.
     */
    public Room getRoom(int id) {
        return rooms.get(id);
    }
    
    /**
     * Obtiene todas las aulas.
     */
    public List<Room> getAllRooms() {
        return new ArrayList<>(rooms.values());
    }
    
    /**
     * Obtiene todas las aulas que son laboratorios.
     */
    public List<Room> getAllLabs() {
        return rooms.values().stream()
            .filter(Room::isLab)
            .collect(Collectors.toList());
    }
    
    /**
     * Obtiene todas las aulas que no son laboratorios.
     */
    public List<Room> getAllNonLabs() {
        return rooms.values().stream()
            .filter(room -> !room.isLab())
            .collect(Collectors.toList());
    }
    
    // ===== Métodos para gestionar asignaciones =====
    
    /**
     * Añade una asignación al sistema.
     */
    public void addAssignment(Assignment assignment) {
        Objects.requireNonNull(assignment, "La asignación no puede ser null");
        assignments.put(assignment.getId(), assignment);
        logger.debug("Asignación añadida: id={}, profesor={}, aula={}, día={}", 
                   assignment.getId(), assignment.getProfessorName(), 
                   assignment.getRoomName(), assignment.getDay());
    }
    
    /**
     * Elimina una asignación del sistema.
     */
    public boolean removeAssignment(int id) {
        Assignment removed = assignments.remove(id);
        if (removed != null) {
            logger.debug("Asignación eliminada: id={}", id);
            return true;
        }
        return false;
    }
    
    /**
     * Obtiene una asignación por su ID.
     */
    public Assignment getAssignment(int id) {
        return assignments.get(id);
    }
    
    /**
     * Obtiene todas las asignaciones.
     */
    public List<Assignment> getAllAssignments() {
        return new ArrayList<>(assignments.values());
    }
    
    /**
     * Obtiene las asignaciones de un profesor específico.
     */
    public List<Assignment> getAssignmentsByProfessor(int professorId) {
        return assignments.values().stream()
            .filter(a -> a.getProfessorId() == professorId)
            .collect(Collectors.toList());
    }
    
    /**
     * Obtiene las asignaciones de un aula específica.
     */
    public List<Assignment> getAssignmentsByRoom(int roomId) {
        return assignments.values().stream()
            .filter(a -> a.getRoomId() == roomId)
            .collect(Collectors.toList());
    }
    
    /**
     * Obtiene las asignaciones de un día específico.
     */
    public List<Assignment> getAssignmentsByDay(String day) {
        return assignments.values().stream()
            .filter(a -> a.getDay().equals(day))
            .collect(Collectors.toList());
    }
    
    /**
     * Asigna una materia a un profesor.
     */
    public void assignSubjectToProfessor(int professorId, String subjectCode) {
        Professor professor = professors.get(professorId);
        Subject subject = subjects.get(subjectCode);
        
        if (professor != null && subject != null) {
            professor.assignSubject(subject);
            logger.debug("Materia {} asignada a profesor {}", subjectCode, professorId);
        } else {
            logger.warn("No se pudo asignar materia. Profesor o materia no encontrados: professorId={}, subjectCode={}", 
                       professorId, subjectCode);
        }
    }
    
    /**
     * Genera asignaciones aleatorias para pruebas.
     * @param count Número de asignaciones a generar
     * @param startDate Fecha de inicio
     * @return Lista de asignaciones generadas
     */
    public List<Assignment> generateRandomAssignments(int count, LocalDate startDate) {
        List<Assignment> result = new ArrayList<>();
        
        // Obtener entidades disponibles
        List<Professor> availableProfessors = getAllProfessors();
        List<Subject> availableSubjects = getAllSubjects();
        List<Room> availableRooms = getAllRooms();
        
        // Verificar que hay entidades suficientes
        if (availableProfessors.isEmpty() || availableSubjects.isEmpty() || availableRooms.isEmpty()) {
            logger.warn("No hay suficientes entidades para generar asignaciones");
            return result;
        }
        
        // Días y horas disponibles
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
        LocalTime[][] timeSlots = {
            {LocalTime.of(7, 0), LocalTime.of(9, 0)},
            {LocalTime.of(9, 0), LocalTime.of(11, 0)},
            {LocalTime.of(11, 0), LocalTime.of(13, 0)},
            {LocalTime.of(14, 0), LocalTime.of(16, 0)},
            {LocalTime.of(16, 0), LocalTime.of(18, 0)}
        };
        
        Random random = new Random();
        
        for (int i = 0; i < count; i++) {
            // Seleccionar entidades aleatorias
            Professor professor = availableProfessors.get(random.nextInt(availableProfessors.size()));
            Subject subject = availableSubjects.get(random.nextInt(availableSubjects.size()));
            
            // Seleccionar un aula adecuada (laboratorio si la materia lo requiere)
            List<Room> compatibleRooms = subject.requiresLab() ? 
                getAllLabs() : getAllRooms();
            
            if (compatibleRooms.isEmpty()) {
                logger.warn("No hay aulas compatibles para la materia {}", subject.getCode());
                continue;
            }
            
            Room room = compatibleRooms.get(random.nextInt(compatibleRooms.size()));
            
            // Seleccionar día y hora aleatorios
            String day = days[random.nextInt(days.length)];
            int timeSlotIndex = random.nextInt(timeSlots.length);
            
            // Generar ID único para la asignación
            int assignmentId = assignments.size() + 1;
            
            // Crear la asignación
            Assignment assignment = new Assignment.Builder()
                .id(assignmentId)
                .assignmentDate(startDate)
                .professor(professor)
                .room(room)
                .subject(subject)
                .groupId(assignmentId)  // Usar el ID como ID de grupo por simplicidad
                .groupName("Grupo " + (char)('A' + (assignmentId % 26)))
                .day(day)
                .startTime(timeSlots[timeSlotIndex][0])
                .endTime(timeSlots[timeSlotIndex][1])
                .sessionType(random.nextBoolean() ? "D" : "N")
                .enrolledStudents(20 + random.nextInt(20))  // Entre 20-39 estudiantes
                .build();
            
            result.add(assignment);
            addAssignment(assignment);
            
            logger.debug("Asignación generada: id={}, profesor={}, materia={}, aula={}, día={}",
                       assignment.getId(), professor.getName(), subject.getCode(), 
                       room.getName(), day);
        }
        
        return result;
    }
    
    /**
     * Limpia todos los datos del sistema.
     */
    public void clearAll() {
        professors.clear();
        subjects.clear();
        rooms.clear();
        assignments.clear();
        logger.info("Todos los datos han sido eliminados");
        
        // Reinicializar datos por defecto
        initializeDefaultData();
    }

    // Estos métodos deberían añadirse a la clase DataManager.java

    /**
    * Guarda el estado completo del DataManager en un archivo JSON.
    * 
    * @param filePath Ruta del archivo donde se guardará el estado
    * @throws IOException Si hay error en la escritura del archivo
    */
    public void saveToJson(String filePath) throws IOException {
        DataManagerPersistence persistence = new DataManagerPersistence();
        persistence.saveToJson(filePath);
        logger.info("Estado guardado en: {}", filePath);
    }

    /**
    * Carga el estado completo del DataManager desde un archivo JSON.
    * Reemplaza todo el estado actual.
    * 
    * @param filePath Ruta del archivo a cargar
    * @throws IOException Si hay error en la lectura del archivo
    * @throws DomainException Si hay error en la validación de los datos cargados
    */
    public void loadFromJson(String filePath) throws IOException {
        DataManagerPersistence persistence = new DataManagerPersistence();
        persistence.loadFromJson(filePath);
        logger.info("Estado cargado desde: {}", filePath);
    }
}