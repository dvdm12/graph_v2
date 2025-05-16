package com.example.miapp.repository;

import com.example.miapp.domain.*;
import com.example.miapp.domain.TimeSlot.TimeRange;
import com.example.miapp.exception.DomainException;
import com.example.miapp.persistence.DataManagerPersistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
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
         initializeDefaultProfessors();
    }

    private void initializeDefaultProfessors() {
    logger.info("Comenzando inicialización de 20 profesores por defecto");

    // 1) Nombres colombianos
    List<String> names = List.of(
        "David Mantilla Aviles",
        "María Fernanda López Gómez",
        "Juan Camilo Pérez Torres",
        "Catalina Rodríguez Martínez",
        "Andrés Felipe Ramírez Ruiz",
        "Laura Isabel Gómez Silva",
        "Sebastián Hernández Castro",
        "Valentina Sánchez Moreno",
        "Diego Alejandro Vargas Díaz",
        "Paula Andrea Mendoza Rojas",
        "Carlos Eduardo Morales Ibañez",
        "Natalia Jiménez Herrera",
        "Miguel Ángel Romero Gil",
        "Daniela Patricia Ortiz León",
        "Santiago Castro Villamil",
        "Alejandra Ruiz Pacheco",
        "José Luis Fernández Vargas",
        "Camila Álvarez Suárez",
        "Andrés Mauricio Silva Rincón",
        "María Paula Torres Escobar"
    );

    // 2) Materias disponibles
    List<Subject> subjectList = new ArrayList<>(subjects.values());

    // 3) Días válidos (solo de lunes a viernes)
    List<DayOfWeek> days = List.of(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY
    );

    for (int i = 0; i < names.size(); i++) {
        String fullName = names.get(i);
        // Generar email: minúsculas, sin tildes, espacios → puntos
        String email = fullName.toLowerCase(Locale.ROOT)
            .replace("á","a").replace("é","e")
            .replace("í","i").replace("ó","o")
            .replace("ú","u").replace("ñ","n")
            .replaceAll("\\s+", "\\.") + "@universidad.edu";

        // Crear profesor (usa el constructor que autogenera el ID)
        Professor prof = new Professor(fullName, "Facultad de Ingeniería", email);

        // Asignar 3 materias rotativas
        int base = i * 3;
        Subject s1 = subjectList.get( base      % subjectList.size());
        Subject s2 = subjectList.get((base + 1) % subjectList.size());
        Subject s3 = subjectList.get((base + 2) % subjectList.size());
        prof.assignSubject(s1);
        prof.assignSubject(s2);
        prof.assignSubject(s3);

        // Seleccionar un día y una franja válida
        DayOfWeek dow = days.get(i % days.size());
        List<TimeRange> ranges = TimeSlot.getValidTimeSlots(dow);
        TimeRange range = ranges.get(i % ranges.size());  // rotar entre las franjas
        String dayString = dow.getDisplayName(TextStyle.FULL, Locale.ENGLISH);  // "Monday", …

        // Añadir el bloque respetando los TimeSlots del dominio
        prof.addBlockedSlot(dayString, range.getStart(), range.getEnd());

        // Registrar y guardar
        addProfessor(prof);
        logger.debug(
            "  Profesor creado: id={}, nombre='{}', materias=[{},{},{}], bloque={} {}-{}",
            prof.getId(),
            fullName,
            s1.getCode(), s2.getCode(), s3.getCode(),
            dayString,
            range.getStart(), range.getEnd()
        );
    }

    logger.info("Finalizada inicialización de {} profesores por defecto", professors.size());
}
    
    private void initializeDefaultSubjects() {
    // Ingeniería de Software y áreas relacionadas
    addSubject(new Subject("ADMBD"   , "Administración de Bases de Datos", "", 3, false));
    addSubject(new Subject("ALGLIN"  , "Álgebra Lineal"                , "", 3, false));
    addSubject(new Subject("ANLALG"  , "Análisis Algorítmico"          , "", 3, false));
    addSubject(new Subject("ANLNUM"  , "Análisis Numérico"             , "", 3, false));
    addSubject(new Subject("ARCSW"   , "Arquitecturas de Software"     , "", 3, false));
    addSubject(new Subject("CONAPPE" , "Construcción de APP Empresariales", "", 3, false));
    addSubject(new Subject("CONAPPM" , "Construcción de APP Móviles"    , "", 3, false));
    addSubject(new Subject("CECAL"   , "Control Estadístico de Calidad", "", 3, false));
    addSubject(new Subject("CONPRO"  , "Control de Procesos"           , "", 3, false));
    addSubject(new Subject("CULINV"  , "Cultura Investigativa"         , "", 2, false));
    addSubject(new Subject("CATPAZ"  , "Cátedra de la Paz - 3 Grupos Opción de Grado", "", 2, false));

    // Diseño y desarrollo
    addSubject(new Subject("DSNMEC"  , "Diseño Mecánico"               , "", 3, false));
    addSubject(new Subject("DSNALG"  , "Diseño de Algoritmos"          , "", 3, false));
    addSubject(new Subject("DSNBD"   , "Diseño de Bases de Datos"      , "", 3, false));
    addSubject(new Subject("DSNSW"   , "Diseño de Software"            , "", 3, false));
    addSubject(new Subject("DSNMEC2" , "Diseño Mecatrónico II"         , "", 3, false));

    // Desarrollo y procesos
    addSubject(new Subject("DEVSW"   , "Desarrollo de Software"        , "", 4, false));
    addSubject(new Subject("DEVEMP"  , "Desarrollo en Equipo"          , "", 2, false));
    addSubject(new Subject("PROINT"  , "Proyecto Integrador"           , "", 4, false));

    // Electivas
    addSubject(new Subject("ELCIPY"  , "Electiva I - Python"           , "", 2, false));
    addSubject(new Subject("ELCISST" , "Electiva I - Seguridad y Salud en el Trabajo", "", 2, false));
    addSubject(new Subject("ELCIIT"  , "Electiva II - Inglés Técnico"  , "", 2, false));
    addSubject(new Subject("ELCIET"  , "Electiva III - Ética Profesional", "", 2, false));
    addSubject(new Subject("ELCIIE"  , "Electiva IV - Innovación y Emprendimiento", "", 2, false));
    addSubject(new Subject("ELCVGP"  , "Electiva V - Gestión de Proyectos", "", 2, false));
    addSubject(new Subject("ELCVDK"  , "Electiva VI - Contenedores Docker", "", 2, false));
    addSubject(new Subject("ELCVLM"  , "Electiva VI - Lean Manufacturing", "", 2, false));

    // Fundamentos y matemáticas
    addSubject(new Subject("ESTPROB" , "Estadística y Probabilidad"    , "", 3, false));
    addSubject(new Subject("ESTINF"  , "Estadística Inferencial"       , "", 3, false));
    addSubject(new Subject("MATDIS"  , "Matemáticas Discretas"         , "", 3, false));
    addSubject(new Subject("MATI"    , "Matemáticas I"                 , "", 3, false));
    addSubject(new Subject("MATII"   , "Matemáticas II"                , "", 3, false));
    addSubject(new Subject("MATIII"  , "Matemáticas III"               , "", 3, false));
    addSubject(new Subject("MATIV"   , "Matemáticas IV"                , "", 3, false));
    addSubject(new Subject("MATV"    , "Matemáticas V"                 , "", 3, false));

    // Ciencias básicas
    addSubject(new Subject("FISIA"   , "Física I"                      , "", 4, true));
    addSubject(new Subject("FISII"   , "Física II"                     , "", 4, true));
    addSubject(new Subject("ELEFUN"  , "Fundamentos de Electricidad"   , "", 3, false));

    // Ingeniería y proyectos
    addSubject(new Subject("GPSW"    , "Gestión de Proyectos de Software", "", 3, false));
    addSubject(new Subject("INGREQ"  , "Ingeniería de Requisitos"      , "", 3, false));
    addSubject(new Subject("IO"      , "Investigación de Operaciones"  , "", 3, false));
    addSubject(new Subject("MNDTEC"  , "Modelos de Negocio Tecnológicos", "", 2, false));
    addSubject(new Subject("ORBARC"  , "Organización y Arquitectura de Computadores", "", 3, false));
    addSubject(new Subject("PARPRO"  , "Paradigmas de Programación"    , "", 3, false));
    addSubject(new Subject("PLCPRO"  , "Planificación y Control de la Producción", "", 3, false));
    addSubject(new Subject("PRCADM"  , "Procesos Administrativos"       , "", 3, false));
    addSubject(new Subject("PRCIND"  , "Procesos Industriales"         , "", 3, false));
    addSubject(new Subject("PRCIND2" , "Procesos Industriales II"      , "", 3, false));
    addSubject(new Subject("PRODAT"  , "Producción I"                  , "", 3, false));

    // Redes y sistemas
    addSubject(new Subject("PROWEB"  , "Programación Web"              , "", 3, true));
    addSubject(new Subject("REDDAT"  , "Redes de Computadores"         , "", 3, true));
    addSubject(new Subject("SEGINFO" , "Seguridad Informática"         , "", 3, true));
    addSubject(new Subject("SIMUL"   , "Simulación"                    , "", 3, false));
    addSubject(new Subject("SISDIN"  , "Sistemas Dinámicos"            , "", 3, false));
    addSubject(new Subject("SISDIS"  , "Sistemas Distribuidos"         , "", 3, true));
    addSubject(new Subject("SISOP"   , "Sistemas Operativos"           , "", 4, false));
    addSubject(new Subject("SISCST"  , "Sistemas y Análisis de Costos" , "", 3, false));

    // Automatización y robótica
    addSubject(new Subject("ROBIIO"  , "Robótica II"                   , "", 3, true));
    addSubject(new Subject("VISART"  , "Visión Artificial"             , "", 3, true));
    addSubject(new Subject("TOPGEO"  , "Topología y Geometría"         , "", 3, false));
    addSubject(new Subject("TDS"     , "Tratamiento de Señales"       , "", 3, false));
    addSubject(new Subject("TVAMOV"  , "Tiempos y Movimientos"        , "", 3, false));

    // Ética y formación integral
    addSubject(new Subject("ETBL02"  , "Ética BLearning - 2 Grupos"    , "", 2, false));

    logger.debug("Inicializadas {} materias por defecto", subjects.size());
}




    
    /**
 * Inicializa la colección de aulas por defecto.
 */
private void initializeDefaultRooms() {
    // Aulas normales existentes
    addRoom(new Room("A101", 40, false));
    addRoom(new Room("A102", 35, false));
    addRoom(new Room("A103", 30, false));
    addRoom(new Room("A104", 45, false));
    addRoom(new Room("A105", 50, false));
    
    // Laboratorios existentes
    addRoom(new Room("LAB101", 25, true));
    addRoom(new Room("LAB102", 30, true));
    addRoom(new Room("LAB103", 20, true));
    addRoom(new Room("LAB104", 35, true));
    addRoom(new Room("LAB105", 40, true));

    // 10 aulas normales nuevas
    addRoom(new Room("A106", 28, false));
    addRoom(new Room("A107", 32, false));
    addRoom(new Room("A108", 38, false));
    addRoom(new Room("A109", 42, false));
    addRoom(new Room("A110", 48, false));
    addRoom(new Room("A111", 36, false));
    addRoom(new Room("A112", 31, false));
    addRoom(new Room("A113", 29, false));
    addRoom(new Room("A114", 44, false));
    addRoom(new Room("A115", 52, false));

    // 10 laboratorios nuevos
    addRoom(new Room("LAB106", 22, true));
    addRoom(new Room("LAB107", 27, true));
    addRoom(new Room("LAB108", 18, true));
    addRoom(new Room("LAB109", 33, true));
    addRoom(new Room("LAB110", 37, true));
    addRoom(new Room("LAB111", 24, true));
    addRoom(new Room("LAB112", 26, true));
    addRoom(new Room("LAB113", 21, true));
    addRoom(new Room("LAB114", 39, true));
    addRoom(new Room("LAB115", 43, true));
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