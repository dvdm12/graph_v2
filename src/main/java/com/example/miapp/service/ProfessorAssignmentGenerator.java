package com.example.miapp.service;

import com.example.miapp.domain.*;
import com.example.miapp.exception.DomainException;
import com.example.miapp.repository.DataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Servicio para generar asignaciones a partir de profesores y materias.
 * Implementa procesamiento paralelo para mejorar el rendimiento.
 */
public class ProfessorAssignmentGenerator {
    private static final Logger logger = LoggerFactory.getLogger(ProfessorAssignmentGenerator.class);
    
    // Constantes para la generación
    private static final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
    private static final LocalTime[][] TIME_SLOTS = {
        {LocalTime.of(7, 0), LocalTime.of(9, 0)},
        {LocalTime.of(9, 0), LocalTime.of(11, 0)},
        {LocalTime.of(11, 0), LocalTime.of(13, 0)},
        {LocalTime.of(14, 0), LocalTime.of(16, 0)},
        {LocalTime.of(16, 0), LocalTime.of(18, 0)},
        {LocalTime.of(18, 0), LocalTime.of(20, 0)}
    };
    
    // Configuración para ejecución paralela
    private static final int MIN_ASSIGNMENTS_FOR_PARALLEL = 100; // Umbral para usar paralelismo
    private static final int BATCH_SIZE = 50; // Tamaño de lote para procesamiento paralelo
    
    private final DataManager dataManager;
    private final Random random;
    private final long seed;
    private final ExecutorService executor;
    
    /**
     * Constructor con semilla específica para los generadores aleatorios.
     * Permite reproducibilidad en las pruebas.
     * 
     * @param seed Semilla para el generador aleatorio
     */
    public ProfessorAssignmentGenerator(long seed) {
        this.dataManager = DataManager.getInstance();
        this.seed = seed;
        this.random = new Random(seed);
        
        // Crear un ExecutorService con un número óptimo de hilos
        int processors = Runtime.getRuntime().availableProcessors();
        this.executor = Executors.newWorkStealingPool(processors);
        
        logger.info("ProfessorAssignmentGenerator inicializado con semilla: {} y {} hilos", 
                   seed, processors);
    }
    
    /**
     * Constructor por defecto que utiliza una semilla aleatoria.
     */
    public ProfessorAssignmentGenerator() {
        this(ThreadLocalRandom.current().nextLong());
    }
    
    /**
     * Genera asignaciones aleatorias basadas en los profesores y materias disponibles.
     * 
     * @param startDate Fecha de inicio para las asignaciones
     * @param count Número de asignaciones a generar
     * @return Lista de asignaciones generadas
     */
    public List<Assignment> generateAssignments(LocalDate startDate, int count) {
        logger.info("Generando {} asignaciones a partir de fecha: {}", count, startDate);
        
        // Decidir si usar procesamiento paralelo basado en el número de asignaciones
        if (count >= MIN_ASSIGNMENTS_FOR_PARALLEL) {
            logger.info("Usando generación paralela para {} asignaciones", count);
            return generateAssignmentsParallel(startDate, count);
        } else {
            logger.info("Usando generación secuencial para {} asignaciones", count);
            return generateAssignmentsSequential(startDate, count);
        }
    }
    
    /**
     * Implementación secuencial original para generar asignaciones.
     */
    private List<Assignment> generateAssignmentsSequential(LocalDate startDate, int count) {
        List<Assignment> assignments = new ArrayList<>();
        List<Professor> professors = dataManager.getAllProfessors();
        List<Room> rooms = dataManager.getAllRooms();
        
        // Verificar que hay entidades para generar asignaciones
        if (professors.isEmpty()) {
            logger.warn("No hay profesores disponibles para generar asignaciones");
            return Collections.emptyList();
        }
        
        if (rooms.isEmpty()) {
            logger.warn("No hay aulas disponibles para generar asignaciones");
            return Collections.emptyList();
        }
        
        // Contador de asignaciones generadas
        int generated = 0;
        int attempts = 0;
        int maxAttempts = count * 3; // Límite de intentos para evitar bucles infinitos
        
        while (generated < count && attempts < maxAttempts) {
            attempts++;
            
            Optional<Assignment> result = tryGenerateAssignment(
                professors, rooms, startDate, generated);
            
            if (result.isPresent()) {
                assignments.add(result.get());
                generated++;
            }
        }
        
        if (attempts >= maxAttempts && generated < count) {
            logger.warn("Generación interrumpida tras {} intentos. Solo se generaron {} de {} asignaciones",
                       attempts, generated, count);
        }
        
        logger.info("Generación secuencial completada: {} asignaciones generadas", assignments.size());
        return assignments;
    }
    
    /**
 * Implementación paralela para generar asignaciones.
 * Divide el trabajo en lotes y los procesa en paralelo.
 */
private List<Assignment> generateAssignmentsParallel(LocalDate startDate, int count) {
    // Obtener y validar los recursos necesarios
    ResourceBundle resources = validateAndGetResources();
    if (resources == null) {
        return Collections.emptyList();
    }
    
    // Configuración de lotes
    BatchConfig batchConfig = createBatchConfig(count);
    logger.debug("Dividiendo {} asignaciones en {} lotes de tamaño ~{}", 
               count, batchConfig.batches, BATCH_SIZE);
    
    // Crear y ejecutar tareas en paralelo
    List<CompletableFuture<List<Assignment>>> futures = createBatchTasks(
        startDate, batchConfig, resources);
    
    // Combinar resultados
    return combineResults(futures);
}

/**
 * Clase auxiliar para mantener la configuración de lotes.
 */
private static class BatchConfig {
    final int batches;
    final AtomicInteger counter;
    
    BatchConfig(int batches) {
        this.batches = batches;
        this.counter = new AtomicInteger(0);
    }
    
    int getAndIncrementId() {
        return counter.incrementAndGet();
    }
}

/**
 * Clase auxiliar para agrupar los recursos necesarios para la generación.
 */
private static class ResourceBundle {
    final List<Professor> professors;
    final List<Room> rooms;
    
    ResourceBundle(List<Professor> professors, List<Room> rooms) {
        this.professors = professors;
        this.rooms = rooms;
    }
}

/**
 * Valida que existan los recursos necesarios y los devuelve agrupados.
 */
private ResourceBundle validateAndGetResources() {
    List<Professor> professors = dataManager.getAllProfessors();
    List<Room> rooms = dataManager.getAllRooms();
    
    // Verificar que hay entidades para generar asignaciones
    if (professors.isEmpty()) {
        logger.warn("No hay profesores disponibles para generar asignaciones");
        return null;
    }
    
    if (rooms.isEmpty()) {
        logger.warn("No hay aulas disponibles para generar asignaciones");
        return null;
    }
    
    return new ResourceBundle(professors, rooms);
}

/**
 * Crea la configuración de lotes basada en el número total de asignaciones.
 */
private BatchConfig createBatchConfig(int count) {
    // Calcular número de lotes redondeando hacia arriba
    int batches = (count + BATCH_SIZE - 1) / BATCH_SIZE;
    return new BatchConfig(batches);
}

/**
 * Crea y lanza las tareas en paralelo para generar asignaciones por lotes.
 */
private List<CompletableFuture<List<Assignment>>> createBatchTasks(
        LocalDate startDate, BatchConfig batchConfig, ResourceBundle resources) {
    
    List<CompletableFuture<List<Assignment>>> futures = new ArrayList<>();
    
    for (int i = 0; i < batchConfig.batches; i++) {
        // Calcular tamaño del lote actual (el último puede ser más pequeño)
        int batchNumber = i;
        int totalCount = batchConfig.batches * BATCH_SIZE;
        int batchCount = Math.min(BATCH_SIZE, totalCount - (i * BATCH_SIZE));
        
        // Crear una copia del generador aleatorio con una semilla derivada para este lote
        Random batchRandom = new Random(seed + i);
        
        // Crear y ejecutar la tarea para este lote
        CompletableFuture<List<Assignment>> batchFuture = CompletableFuture.supplyAsync(
            () -> generateBatch(startDate, batchNumber, batchCount, batchRandom, 
                                resources, batchConfig),
            executor);
        
        futures.add(batchFuture);
    }
    
    return futures;
}

/**
 * Genera un lote de asignaciones como parte del procesamiento paralelo.
 */
private List<Assignment> generateBatch(
        LocalDate startDate, int batchNumber, int batchCount, Random batchRandom,
        ResourceBundle resources, BatchConfig batchConfig) {
    
    List<Assignment> batchAssignments = new ArrayList<>();
    int batchGenerated = 0;
    int batchAttempts = 0;
    int maxBatchAttempts = batchCount * 5; // Más intentos para evitar bloqueos
    
    while (batchGenerated < batchCount && batchAttempts < maxBatchAttempts) {
        batchAttempts++;
        
        // Intentar crear una asignación
        Optional<Assignment> result = tryCreateAssignment(
            startDate, batchRandom, resources, batchConfig, batchGenerated);
        
        if (result.isPresent()) {
            Assignment assignment = result.get();
            batchAssignments.add(assignment);
            batchGenerated++;
            
            logger.trace("Lote {}: Generada asignación #{}: id={}, profesor={}, aula={}",
                       batchNumber + 1, batchGenerated, assignment.getId(), 
                       assignment.getProfessorName(), assignment.getRoomName());
        }
    }
    
    logger.debug("Lote {} completado: {} asignaciones generadas en {} intentos", 
               batchNumber + 1, batchGenerated, batchAttempts);
    
    return batchAssignments;
}

/**
 * Intenta crear una asignación aleatoria como parte de un lote.
 */
private Optional<Assignment> tryCreateAssignment(
        LocalDate startDate, Random random, ResourceBundle resources, 
        BatchConfig batchConfig, int batchIndex) {
    
    // Seleccionar un profesor aleatorio
    Professor professor = resources.professors.get(random.nextInt(resources.professors.size()));
    
    // Verificar que el profesor tenga materias asignadas
    List<Subject> professorSubjects = professor.getSubjects();
    if (professorSubjects.isEmpty()) {
        return Optional.empty();
    }
    
    // Seleccionar una materia aleatoria de las asignadas al profesor
    Subject subject = professorSubjects.get(random.nextInt(professorSubjects.size()));
    
    // Seleccionar una sala compatible con los requisitos de la materia
    List<Room> compatibleRooms = resources.rooms.stream()
        .filter(room -> room.isCompatibleWithLabRequirement(subject.requiresLab()))
        .collect(Collectors.toList());
    
    if (compatibleRooms.isEmpty()) {
        return Optional.empty();
    }
    
    Room room = compatibleRooms.get(random.nextInt(compatibleRooms.size()));
    
    // Seleccionar día y hora aleatorios
    String day = DAYS[random.nextInt(DAYS.length)];
    int timeSlotIndex = random.nextInt(TIME_SLOTS.length);
    LocalTime startTime = TIME_SLOTS[timeSlotIndex][0];
    LocalTime endTime = TIME_SLOTS[timeSlotIndex][1];
    
    // Verificar que el profesor no tenga conflicto con una franja bloqueada
    if (professor.hasBlockedSlotConflict(day, startTime, endTime)) {
        return Optional.empty();
    }
    
    // Generar ID único para la asignación (de manera segura con el contador atómico)
    int assignmentId = batchConfig.getAndIncrementId();
    
    // Generar un número aleatorio de estudiantes dentro del límite de capacidad
    int maxStudents = room.getCapacity();
    int minStudents = Math.min(10, maxStudents); // Al menos 10 estudiantes si es posible
    int enrolledStudents = minStudents + random.nextInt(maxStudents - minStudents + 1);
    
    // Crear la asignación
    Assignment assignment = new Assignment.Builder()
        .id(assignmentId)
        .assignmentDate(startDate)
        .professor(professor)
        .room(room)
        .subject(subject)
        .groupId(assignmentId) // Usar el ID como ID de grupo también
        .groupName("Grupo " + (char)('A' + (batchIndex % 26))) // A, B, C, ...
        .day(day)
        .startTime(startTime)
        .endTime(endTime)
        .sessionType(random.nextBoolean() ? "D" : "N") // D o N aleatorio
        .enrolledStudents(enrolledStudents)
        .build();
    
    return Optional.of(assignment);
}

/**
 * Combina los resultados de todos los lotes en una única lista ordenada.
 */
private List<Assignment> combineResults(List<CompletableFuture<List<Assignment>>> futures) {
    try {
        List<Assignment> allAssignments = futures.stream()
            .map(CompletableFuture::join)
            .flatMap(List::stream)
            .collect(Collectors.toList());
        
        logger.info("Generación paralela completada: {} asignaciones generadas", allAssignments.size());
        
        // Ordenar por ID para mantener consistencia
        allAssignments.sort(Comparator.comparingInt(Assignment::getId));
        
        return allAssignments;
    } catch (CompletionException e) {
        Throwable cause = e.getCause();
        if (cause instanceof RuntimeException) {
            throw (RuntimeException) cause;
        } else {
            throw new DomainException("Error en la generación paralela: " + cause.getMessage(), cause);
        }
    }
}
    
    
    /**
     * Intenta generar una asignación aleatoria.
     * 
     * @param professors Lista de profesores disponibles
     * @param rooms Lista de aulas disponibles
     * @param startDate Fecha de inicio
     * @param index Índice para el nombre del grupo
     * @return Optional con la asignación generada o vacío si no fue posible
     */
    private Optional<Assignment> tryGenerateAssignment(
            List<Professor> professors, List<Room> rooms, LocalDate startDate, int index) {
        
        // Seleccionar un profesor aleatorio
        Professor professor = professors.get(random.nextInt(professors.size()));
        
        // Verificar que el profesor tenga materias asignadas
        List<Subject> professorSubjects = professor.getSubjects();
        if (professorSubjects.isEmpty()) {
            logger.debug("Profesor id={} no tiene materias asignadas, saltando", professor.getId());
            return Optional.empty();
        }
        
        // Seleccionar una materia aleatoria de las asignadas al profesor
        Subject subject = professorSubjects.get(random.nextInt(professorSubjects.size()));
        
        // Seleccionar una sala compatible con los requisitos de la materia
        List<Room> compatibleRooms = rooms.stream()
            .filter(room -> room.isCompatibleWithLabRequirement(subject.requiresLab()))
            .collect(Collectors.toList());
        
        if (compatibleRooms.isEmpty()) {
            logger.debug("No hay aulas compatibles para la materia {}, saltando", subject.getCode());
            return Optional.empty();
        }
        
        Room room = compatibleRooms.get(random.nextInt(compatibleRooms.size()));
        
        // Seleccionar día y hora aleatorios
        String day = DAYS[random.nextInt(DAYS.length)];
        int timeSlotIndex = random.nextInt(TIME_SLOTS.length);
        LocalTime startTime = TIME_SLOTS[timeSlotIndex][0];
        LocalTime endTime = TIME_SLOTS[timeSlotIndex][1];
        
        // Verificar que el profesor no tenga conflicto con una franja bloqueada
        if (professor.hasBlockedSlotConflict(day, startTime, endTime)) {
            logger.debug("Profesor id={} tiene conflicto en {} {}-{}, saltando", 
                       professor.getId(), day, startTime, endTime);
            return Optional.empty();
        }
        
        // Generar ID único para la asignación
        int assignmentId = dataManager.getAllAssignments().size() + index + 1;
        
        // Generar un número aleatorio de estudiantes dentro del límite de capacidad
        int maxStudents = room.getCapacity();
        int minStudents = Math.min(10, maxStudents); // Al menos 10 estudiantes si es posible
        int enrolledStudents = minStudents + random.nextInt(maxStudents - minStudents + 1);
        
        // Crear la asignación
        Assignment assignment = new Assignment.Builder()
            .id(assignmentId)
            .assignmentDate(startDate)
            .professor(professor)
            .room(room)
            .subject(subject)
            .groupId(assignmentId) // Usar el ID como ID de grupo también
            .groupName("Grupo " + (char)('A' + (index % 26))) // A, B, C, ...
            .day(day)
            .startTime(startTime)
            .endTime(endTime)
            .sessionType(random.nextBoolean() ? "D" : "N") // D o N aleatorio
            .enrolledStudents(enrolledStudents)
            .build();
        
        logger.debug("Generada asignación #{}: id={}, profesor={}, materia={}, aula={}, día={}, hora={}-{}",
                   index + 1, assignmentId, professor.getName(), subject.getCode(), 
                   room.getName(), day, startTime, endTime);
        
        return Optional.of(assignment);
    }
    
    /**
     * Genera franjas bloqueadas aleatorias para un profesor.
     * 
     * @param professor Profesor para el que generar franjas
     * @param count Número de franjas a generar
     */
    public void generateRandomBlockedSlots(Professor professor, int count) {
        logger.info("Generando {} franjas bloqueadas para profesor id={}", 
                  count, professor.getId());
        
        int generated = 0;
        int attempts = 0;
        int maxAttempts = count * 3;
        
        while (generated < count && attempts < maxAttempts) {
            attempts++;
            
            // Día aleatorio
            String day = DAYS[random.nextInt(DAYS.length)];
            
            // Horario aleatorio
            int startHour = 7 + random.nextInt(12); // Entre 7 y 18
            int duration = 1 + random.nextInt(3);   // Entre 1 y 3 horas
            
            LocalTime startTime = LocalTime.of(startHour, 0);
            LocalTime endTime = startTime.plusHours(duration);
            
            // Comprobar si la franja ya existe
            boolean exists = false;
            for (BlockedSlot existingSlot : professor.getBlockedSlots()) {
                if (existingSlot.getDay().equals(day) && 
                    existingSlot.getStartTime().equals(startTime) && 
                    existingSlot.getEndTime().equals(endTime)) {
                    exists = true;
                    break;
                }
            }
            
            if (exists) {
                logger.debug("Franja ya existe: {} {}-{}, saltando", day, startTime, endTime);
                continue;
            }
            
            // Crear franja bloqueada
            BlockedSlot slot = new BlockedSlot.Builder()
                .day(day)
                .startTime(startTime)
                .endTime(endTime)
                .build();
            
            professor.addBlockedSlot(slot);
            generated++;
            
            logger.debug("Generada franja bloqueada #{} para profesor id={}: {} {}-{}",
                       generated, professor.getId(), day, startTime, endTime);
        }
        
        if (attempts >= maxAttempts && generated < count) {
            logger.warn("Generación de franjas interrumpida tras {} intentos. Solo se generaron {} de {}",
                       attempts, generated, count);
        }
        
        logger.info("Generación completada: {} franjas bloqueadas para profesor id={}",
                  generated, professor.getId());
    }
    
    /**
     * Genera datos completos para pruebas, incluyendo profesores, materias, aulas y asignaciones.
     * Utiliza procesamiento paralelo para mejorar el rendimiento.
     * 
     * @param professorCount Número de profesores a generar
     * @param assignmentCount Número de asignaciones a generar
     * @param startDate Fecha de inicio para las asignaciones
     */
    public void generateCompleteTestData(int professorCount, int assignmentCount, LocalDate startDate) {
        logger.info("Generando datos completos de prueba: {} profesores, {} asignaciones",
                  professorCount, assignmentCount);
        
        // 1. Verificar que hay materias disponibles
        List<Subject> subjects = dataManager.getAllSubjects();
        if (subjects.isEmpty()) {
            logger.warn("No hay materias disponibles para generar datos de prueba");
            return;
        }
        
        // 2. Generar profesores y asignarles materias en paralelo si hay muchos profesores
        generateProfessorsParallel(professorCount, subjects);
        
        // 3. Generar asignaciones
        List<Assignment> generatedAssignments = generateAssignments(startDate, assignmentCount);
        
        // 4. Añadir las asignaciones al DataManager
        for (Assignment assignment : generatedAssignments) {
            dataManager.addAssignment(assignment);
        }
        
        logger.info("Generación de datos de prueba completada: {} profesores, {} asignaciones",
                  dataManager.getAllProfessors().size(), dataManager.getAllAssignments().size());
    }
    
    /**
     * Genera profesores en paralelo cuando hay un número significativo a crear.
     * 
     * @param professorCount Número de profesores a generar
     * @param subjects Lista de materias disponibles
     */
    private void generateProfessorsParallel(int professorCount, List<Subject> subjects) {
        List<String> departments = List.of("Matemáticas", "Informática", "Física", "Química", "Biología");
        
        // Decidir si usar paralelismo basado en el número de profesores
        if (professorCount >= 50) {
            logger.info("Usando generación paralela para {} profesores", professorCount);
            
            // Crear profesores en paralelo
            List<Professor> professors = IntStream.range(0, professorCount)
                .parallel()
                .mapToObj(i -> {
                    // Crear un generador aleatorio específico para este profesor
                    Random localRandom = new Random(seed + i);
                    
                    String name = "Profesor " + (i + 1);
                    String department = departments.get(localRandom.nextInt(departments.size()));
                    String email = "profesor" + (i + 1) + "@universidad.edu";
                    
                    Professor professor = new Professor(name, department, email);
                    
                    // Asignar 2-4 materias aleatorias
                    int subjectCount = 2 + localRandom.nextInt(3);
                    List<Subject> availableSubjects = new ArrayList<>(subjects);
                    Collections.shuffle(availableSubjects, localRandom);
                    
                    for (int j = 0; j < Math.min(subjectCount, availableSubjects.size()); j++) {
                        professor.assignSubject(availableSubjects.get(j));
                    }
                    
                    // Generar 0-2 franjas bloqueadas
                    int blockedSlotCount = localRandom.nextInt(3);
                    if (blockedSlotCount > 0) {
                        generateRandomBlockedSlotsLocal(professor, blockedSlotCount, localRandom);
                    }
                    
                    logger.debug("Generado profesor con id temporal={}: {}, departamento={}, materias={}",
                               i + 1, name, department, professor.getSubjects().size());
                    
                    return professor;
                })
                .collect(Collectors.toList());
            
            // Añadir profesores al DataManager (esto debe hacerse secuencialmente)
            for (Professor professor : professors) {
                dataManager.addProfessor(professor);
            }
            
            logger.info("Generación paralela de profesores completada: {} profesores creados", 
                       professors.size());
        } else {
            // Generación secuencial para pocos profesores
            for (int i = 0; i < professorCount; i++) {
                String name = "Profesor " + (i + 1);
                String department = departments.get(random.nextInt(departments.size()));
                String email = "profesor" + (i + 1) + "@universidad.edu";
                
                Professor professor = new Professor(name, department, email);
                
                // Asignar 2-4 materias aleatorias a cada profesor
                int subjectCount = 2 + random.nextInt(3);
                List<Subject> availableSubjects = new ArrayList<>(subjects);
                Collections.shuffle(availableSubjects, random);
                
                for (int j = 0; j < Math.min(subjectCount, availableSubjects.size()); j++) {
                    professor.assignSubject(availableSubjects.get(j));
                }
                
                // Generar 0-2 franjas bloqueadas
                int blockedSlotCount = random.nextInt(3);
                if (blockedSlotCount > 0) {
                    generateRandomBlockedSlots(professor, blockedSlotCount);
                }
                
                dataManager.addProfessor(professor);
                logger.debug("Generado profesor id={}: {}, departamento={}, materias={}",
                           professor.getId(), name, department, professor.getSubjects().size());
            }
        }
    }
    
    /**
     * Versión local de generación de franjas bloqueadas para uso en procesamiento paralelo.
     * Evita compartir el generador aleatorio principal.
     */
    private void generateRandomBlockedSlotsLocal(Professor professor, int count, Random localRandom) {
        int generated = 0;
        int attempts = 0;
        int maxAttempts = count * 3;
        
        while (generated < count && attempts < maxAttempts) {
            attempts++;
            
            // Día aleatorio
            String day = DAYS[localRandom.nextInt(DAYS.length)];
            
            // Horario aleatorio
            int startHour = 7 + localRandom.nextInt(12); // Entre 7 y 18
            int duration = 1 + localRandom.nextInt(3);   // Entre 1 y 3 horas
            
            LocalTime startTime = LocalTime.of(startHour, 0);
            LocalTime endTime = startTime.plusHours(duration);
            
            // Comprobar si la franja ya existe
            boolean exists = false;
            for (BlockedSlot existingSlot : professor.getBlockedSlots()) {
                if (existingSlot.getDay().equals(day) && 
                    existingSlot.getStartTime().equals(startTime) && 
                    existingSlot.getEndTime().equals(endTime)) {
                    exists = true;
                    break;
                }
            }
            
            if (exists) {
                continue;
            }
            
            // Crear franja bloqueada
            BlockedSlot slot = new BlockedSlot.Builder()
                .day(day)
                .startTime(startTime)
                .endTime(endTime)
                .build();
            
            professor.addBlockedSlot(slot);
            generated++;
        }
    }
    
    /**
     * Devuelve la semilla utilizada por este generador.
     * Útil para reproducir resultados exactos.
     * 
     * @return Semilla del generador
     */
    public long getSeed() {
        return seed;
    }
    
    /**
     * Libera los recursos del generador.
     * Debe llamarse cuando ya no se necesita el generador para liberar los hilos.
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("ProfessorAssignmentGenerator shutdown completado");
    }
}