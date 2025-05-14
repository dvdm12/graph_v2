package com.example.miapp.service;

import com.example.miapp.domain.Assignment;
import com.example.miapp.domain.Professor;
import com.example.miapp.domain.conflict.ConflictEdge;
import com.example.miapp.domain.conflict.ConflictType;
import com.example.miapp.repository.DataManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Servicio dedicado a la detección y gestión del grafo de conflictos entre asignaciones.
 * Refactorizado para trabajar con el nuevo modelo de dominio y el DataManager singleton.
 */
public class ConflictGraphLoader {
    private static final Logger logger = LoggerFactory.getLogger(ConflictGraphLoader.class);

    // Gestores de datos y servicios
    private final DataManager dataManager;
    private final ConflictDetector conflictDetector;

    // Colecciones de datos y conflictos (thread-safe)
    private final Map<String, List<Assignment>> assignmentsByDay = new ConcurrentHashMap<>();
    private final Map<String, List<ConflictEdge>> edgeConflicts = new ConcurrentHashMap<>();
    private final Set<Assignment> conflictFreeAssignments = ConcurrentHashMap.newKeySet();

    // Locks para operaciones de lectura/escritura
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();

    /**
     * Construye un nuevo gestor de grafo de conflictos.
     */
    public ConflictGraphLoader() {
        this.dataManager = DataManager.getInstance();
        this.conflictDetector = new ConflictDetector();
        logger.info("ConflictGraphLoader inicializado");
    }

    /**
     * Carga todas las asignaciones desde el DataManager y construye el grafo de conflictos.
     */
    public void loadAllAssignments() {
        logger.info("==== loadAllAssignments START ====");
        Instant start = Instant.now();
        
        clear(); // Limpia los datos existentes
        
        // Obtener todas las asignaciones del DataManager
        List<Assignment> assignments = dataManager.getAllAssignments();
        logger.info("Cargando {} asignaciones desde DataManager", assignments.size());
        
        // Procesar cada asignación
        for (Assignment assignment : assignments) {
            addAssignment(assignment);
        }
        
        Instant end = Instant.now();
        logger.info("==== loadAllAssignments END: {} ms, {} conflictos detectados ====", 
                    Duration.between(start, end).toMillis(), edgeConflicts.size());
    }

    /**
     * Obtiene todas las asignaciones cargadas en el grafo.
     * 
     * @return Lista con todas las asignaciones (nunca null, puede estar vacía)
     */
    public List<Assignment> getAllAssignments() {
        readLock.lock();
        try {
            List<Assignment> result = new ArrayList<>();
            for (List<Assignment> dayAssignments : assignmentsByDay.values()) {
                result.addAll(dayAssignments);
            }
            logger.debug("getAllAssignments called, returning {} items", result.size());
            return result;
        } finally {
            readLock.unlock();
        }
    }
    
    /**
     * Genera y carga asignaciones aleatorias para pruebas.
     * 
     * @param count Número de asignaciones a generar
     * @param startDate Fecha de inicio
     */
    public void loadRandomAssignments(int count, LocalDate startDate) {
        logger.info("==== loadRandomAssignments START: {} asignaciones ====", count);
        Instant start = Instant.now();
        
        clear(); // Limpia los datos existentes
        
        // Generar asignaciones aleatorias
        List<Assignment> generatedAssignments = 
            dataManager.generateRandomAssignments(count, startDate);
        
        logger.info("Generadas {} asignaciones aleatorias", generatedAssignments.size());
        
        // Procesar cada asignación generada
        for (Assignment assignment : generatedAssignments) {
            addAssignment(assignment);
        }
        
        Instant end = Instant.now();
        logger.info("==== loadRandomAssignments END: {} ms, {} conflictos detectados ====", 
                    Duration.between(start, end).toMillis(), edgeConflicts.size());
    }
    
    /**
     * Limpia todas las colecciones de datos y conflictos.
     */
    public void clear() {
        writeLock.lock();
        try {
            assignmentsByDay.clear();
            edgeConflicts.clear();
            conflictFreeAssignments.clear();
            logger.debug("Colecciones de datos y conflictos limpiadas");
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Añade una asignación al grafo, detectando todos sus posibles conflictos.
     * 
     * @param assignment Asignación a añadir
     * @throws NullPointerException si assignment es null
     */
    public void addAssignment(Assignment assignment) {
        Objects.requireNonNull(assignment, "La asignación no puede ser null");
        
        logger.trace("--> addAssignment START id={}", assignment.getId());
        Instant start = Instant.now();

        writeLock.lock();
        try {
            // Variable para controlar si hay algún conflicto
            boolean hasAnyConflict = false;

            // 1. Verificar conflictos con franjas bloqueadas del profesor
            Professor professor = assignment.getProfessor();
            if (assignment.hasBlockedSlotConflict()) {
                recordSelfConflict(assignment, ConflictType.PROFESSOR_BLOCKED);
                hasAnyConflict = true;
                logger.debug("Assignment id={} has conflict with blocked slots of professor id={}", 
                           assignment.getId(), professor.getId());
            }
            
            // 2. Verificar si el profesor está autorizado para impartir la materia
            if (!assignment.hasProfessorSubjectAuthorization()) {
                recordSelfConflict(assignment, ConflictType.PROFESSOR_SUBJECT_MISMATCH);
                hasAnyConflict = true;
                logger.debug("Assignment id={} has professor-subject mismatch: Professor id={} is not authorized for subject {}",
                          assignment.getId(), professor.getId(), 
                          assignment.getSubject() != null ? assignment.getSubject().getCode() : "null");
            }
            
            // 3. Verificar si el aula tiene capacidad suficiente
            if (!assignment.hasRoomCapacity()) {
                recordSelfConflict(assignment, ConflictType.ROOM_CAPACITY);
                hasAnyConflict = true;
                logger.debug("Assignment id={} exceeds room capacity: required={}, available={}",
                          assignment.getId(), assignment.getEnrolledStudents(), 
                          assignment.getRoom().getCapacity());
            }
            
            // 4. Verificar si el aula es compatible con la materia
            if (!assignment.hasRoomCompatibility()) {
                recordSelfConflict(assignment, ConflictType.ROOM_COMPATIBILITY);
                hasAnyConflict = true;
                logger.debug("Assignment id={} has room compatibility conflict: room={}, requires lab={}",
                          assignment.getId(), assignment.getRoom().getName(),
                          assignment.getSubject() != null ? assignment.getSubject().requiresLab() : false);
            }

            // 5. Verificar conflictos con otras asignaciones del mismo día
            boolean hasAssignmentConflicts = checkConflictsWithExistingAssignments(assignment);
            if (hasAssignmentConflicts) {
                hasAnyConflict = true;
                logger.debug("Assignment id={} has conflicts with other assignments", 
                           assignment.getId());
            }
            
            // 6. Añadir a la colección por día para futuras comprobaciones
            addToAssignmentsByDay(assignment);
            
            // 7. Solo agregar a conflictFreeAssignments si no tiene ningún tipo de conflicto
            if (!hasAnyConflict) {
                conflictFreeAssignments.add(assignment);
                logger.debug("Assignment id={} is conflict-free", assignment.getId());
            } else {
                logger.debug("Assignment id={} has conflicts and won't be added to conflict-free set", 
                           assignment.getId());
            }
            
        } finally {
            writeLock.unlock();
            logger.trace("<-- addAssignment END id={} ({} ms)", 
                         assignment.getId(), 
                         Duration.between(start, Instant.now()).toMillis());
        }
    }
    
    /**
     * Registra un conflicto de la asignación consigo misma (ej: franja bloqueada).
     */
    private void recordSelfConflict(Assignment assignment, ConflictType conflictType) {
        String key = assignment.getId() + "-" + assignment.getId();
        edgeConflicts
            .computeIfAbsent(key, k -> Collections.synchronizedList(new ArrayList<>()))
            .add(new ConflictEdge(conflictType));
        // Eliminar de la lista de asignaciones sin conflictos
        conflictFreeAssignments.remove(assignment);
        logger.debug("Recorded self-conflict for assignment id={}: {}", 
                   assignment.getId(), conflictType.getLabel());
    }
    
    /**
     * Verifica conflictos con asignaciones existentes del mismo día.
     * @return true si se encontró algún conflicto
     */
    private boolean checkConflictsWithExistingAssignments(Assignment assignment) {
        List<Assignment> sameDay = assignmentsByDay.getOrDefault(
            assignment.getDay(), Collections.emptyList());
        
        boolean foundConflict = false;
        logger.trace("Checking conflicts for assignment id={} with {} existing assignments on {}",
                  assignment.getId(), sameDay.size(), assignment.getDay());
        
        for (Assignment existing : sameDay) {
            // Evitar comparar con asignaciones de ID mayor (ya comparadas o la misma)
            if (existing.getId() >= assignment.getId()) {
                logger.trace("Skipping comparison with id={} (ID >= current)", existing.getId());
                continue;
            }
            
            // Verificar si hay solapamiento de tiempo
            boolean overlaps = conflictDetector.timeOverlaps(existing, assignment);
            if (!overlaps) {
                logger.trace("No time overlap between assignments {}-{}", 
                          existing.getId(), assignment.getId());
                continue;
            }
            
            logger.debug("Time overlap detected between assignments {}-{}", 
                       existing.getId(), assignment.getId());
            
            // Verificar conflictos específicos
            Map<String, List<ConflictEdge>> conflicts = existing.conflictsWith(assignment);
            if (!conflicts.isEmpty()) {
                recordConflicts(existing, assignment, conflicts);
                foundConflict = true;
                logger.debug("Recorded {} conflict types between assignments {}-{}", 
                           conflicts.size(), existing.getId(), assignment.getId());
            } else {
                logger.trace("No specific conflicts found despite time overlap between {}-{}", 
                          existing.getId(), assignment.getId());
            }
        }
        
        return foundConflict;
    }
    
    /**
     * Registra conflictos entre dos asignaciones.
     */
    private void recordConflicts(Assignment a1, Assignment a2, 
                               Map<String, List<ConflictEdge>> conflicts) {
        String key = a1.getId() + "-" + a2.getId();
        
        // Registrar cada tipo de conflicto
        conflicts.forEach((type, edges) -> {
            logger.debug("Recording conflict of type '{}' between assignments {}-{}", 
                       type, a1.getId(), a2.getId());
            
            edgeConflicts
                .computeIfAbsent(key, k -> Collections.synchronizedList(new ArrayList<>()))
                .addAll(edges);
        });
        
        // Eliminar ambas asignaciones de la lista de asignaciones sin conflictos
        conflictFreeAssignments.remove(a1);
        conflictFreeAssignments.remove(a2);
        
        logger.debug("Conflict detected between assignments {} and {} with {} conflict types", 
                   a1.getId(), a2.getId(), conflicts.size());
    }
    
    /**
     * Añade una asignación a la colección indexada por día.
     */
    private void addToAssignmentsByDay(Assignment assignment) {
        assignmentsByDay
            .computeIfAbsent(assignment.getDay(), k -> Collections.synchronizedList(new ArrayList<>()))
            .add(assignment);
        logger.debug("Assignment id={} added to {} day collection", 
                   assignment.getId(), assignment.getDay());
    }

    /**
     * Elimina una asignación y todos sus conflictos asociados.
     * 
     * @param assignment Asignación a eliminar
     * @return true si se eliminó correctamente, false si no existía
     */
    public boolean removeAssignment(Assignment assignment) {
        Objects.requireNonNull(assignment, "La asignación no puede ser null");
        
        logger.debug("Removing assignment id={}", assignment.getId());
        writeLock.lock();
        try {
            // 1. Eliminar de la colección por día
            List<Assignment> dayAssignments = assignmentsByDay.get(assignment.getDay());
            if (dayAssignments != null) {
                boolean removed = dayAssignments.remove(assignment);
                if (!removed) {
                    logger.debug("Assignment id={} not found in day collection", assignment.getId());
                    return false;
                }
            } else {
                logger.debug("No assignments found for day {}", assignment.getDay());
                return false;
            }
            
            // 2. Eliminar de conflictFreeAssignments
            conflictFreeAssignments.remove(assignment);
            
            // 3. Eliminar todos los conflictos relacionados
            int assignmentId = assignment.getId();
            List<String> keysToRemove = new ArrayList<>();
            
            for (String key : edgeConflicts.keySet()) {
                String[] parts = key.split("-");
                int id1 = Integer.parseInt(parts[0]);
                int id2 = Integer.parseInt(parts[1]);
                
                if (id1 == assignmentId || id2 == assignmentId) {
                    keysToRemove.add(key);
                }
            }
            
            for (String key : keysToRemove) {
                edgeConflicts.remove(key);
                logger.debug("Removed conflict key {}", key);
            }
            
            logger.debug("Assignment id={} and {} related conflicts removed", 
                       assignment.getId(), keysToRemove.size());
            
            return true;
        } finally {
            writeLock.unlock();
        }
    }
    
    /**
     * Obtiene todas las asignaciones del día especificado.
     * 
     * @param day Día a consultar
     * @return Lista de asignaciones (nunca null, puede estar vacía)
     */
    public List<Assignment> getAssignmentsByDay(String day) {
        Objects.requireNonNull(day, "El día no puede ser null");
        
        readLock.lock();
        try {
            List<Assignment> result = assignmentsByDay.getOrDefault(day, Collections.emptyList());
            return new ArrayList<>(result); // Copia defensiva
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Obtiene el conjunto de asignaciones sin conflictos.
     */
    public Set<Assignment> getConflictFreeAssignments() {
        readLock.lock();
        try {
            logger.debug("getConflictFreeAssignments called, returning {} items", 
                       conflictFreeAssignments.size());
            return new HashSet<>(conflictFreeAssignments); // Copia defensiva
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Obtiene el mapa de conflictos entre asignaciones.
     */
    public Map<String, List<ConflictEdge>> getEdgeConflicts() {
        readLock.lock();
        try {
            logger.debug("getEdgeConflicts called, returning {} items", 
                       edgeConflicts.size());
            
            // Crear una copia defensiva del mapa y sus listas
            Map<String, List<ConflictEdge>> copy = new HashMap<>();
            edgeConflicts.forEach((key, value) -> 
                copy.put(key, new ArrayList<>(value))
            );
            
            return copy;
        } finally {
            readLock.unlock();
        }
    }
    
    /**
     * Obtiene la lista de conflictos para una asignación específica.
     * 
     * @param assignmentId ID de la asignación
     * @return Mapa de IDs de asignaciones conflictivas y sus conflictos
     */
    public Map<Integer, List<ConflictEdge>> getConflictsForAssignment(int assignmentId) {
        readLock.lock();
        try {
            Map<Integer, List<ConflictEdge>> result = new HashMap<>();
            
            // Buscar conflictos donde esta asignación participa
            for (Map.Entry<String, List<ConflictEdge>> entry : edgeConflicts.entrySet()) {
                String[] parts = entry.getKey().split("-");
                int id1 = Integer.parseInt(parts[0]);
                int id2 = Integer.parseInt(parts[1]);
                
                if (id1 == assignmentId) {
                    // Esta asignación es la primera en el par
                    result.put(id2, new ArrayList<>(entry.getValue()));
                } else if (id2 == assignmentId) {
                    // Esta asignación es la segunda en el par
                    result.put(id1, new ArrayList<>(entry.getValue()));
                }
            }
            
            return result;
        } finally {
            readLock.unlock();
        }
    }
    
    /**
     * Obtiene el número total de conflictos detectados.
     */
    public int getTotalConflictsCount() {
        readLock.lock();
        try {
            int count = 0;
            for (List<ConflictEdge> edges : edgeConflicts.values()) {
                count += edges.size();
            }
            return count;
        } finally {
            readLock.unlock();
        }
    }
    
    /**
     * Analiza los conflictos por tipo y retorna estadísticas.
     * 
     * @return Mapa con el recuento de cada tipo de conflicto
     */
    public Map<ConflictType, Integer> getConflictStatistics() {
        readLock.lock();
        try {
            Map<ConflictType, Integer> stats = new EnumMap<>(ConflictType.class);
            
            // Inicializar contador para cada tipo
            for (ConflictType type : ConflictType.values()) {
                stats.put(type, 0);
            }
            
            // Contar ocurrencias de cada tipo
            for (List<ConflictEdge> edges : edgeConflicts.values()) {
                for (ConflictEdge edge : edges) {
                    ConflictType type = edge.getType();
                    stats.put(type, stats.get(type) + 1);
                }
            }
            
            return stats;
        } finally {
            readLock.unlock();
        }
    }
}