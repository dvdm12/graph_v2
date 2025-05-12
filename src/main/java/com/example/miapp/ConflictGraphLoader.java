package com.example.miapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Gestiona las asignaciones y sus conflictos de manera thread-safe,
 * optimizado con indexación por día para reducir comparaciones,
 * y logging exhaustivo para depuración detallada.
 */
public class ConflictGraphLoader {
    private static final Logger logger = LoggerFactory.getLogger(ConflictGraphLoader.class);

    private final List<Assignment> assignments = new ArrayList<>();
    private final Map<String, List<Assignment>> assignmentsByDay = new HashMap<>();
    private final Map<String, List<ConflictEdge>> edgeConflicts = new LinkedHashMap<>();
    private final Set<Assignment> conflictFreeAssignments = new LinkedHashSet<>();
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final Lock lock = new ReentrantLock();

    /**
     * Carga datos desde un archivo JSON y construye la estructura de conflictos.
     * @param jsonPath ruta al fichero JSON con asignaciones
     * @throws IOException si ocurre un error de lectura
     */
    public void loadData(String jsonPath) throws IOException {
        logger.info("==== loadData START ====");
        Instant start = Instant.now();
        logger.info("Cargando datos desde: {}", jsonPath);
        JsonNode root = mapper.readTree(new File(jsonPath));
        JsonNode array = root.path("assignments");
        if (!array.isArray()) {
            logger.error("ERROR: El JSON no contiene un array 'assignments'");
            throw new IllegalArgumentException("El JSON debe contener un array 'assignments'.");
        }
        logger.info("Se encontraron {} registros en 'assignments'", array.size());
        for (JsonNode node : array) {
            Assignment a = mapper.treeToValue(node, Assignment.class);
            addAssignment(a);
        }
        Instant end = Instant.now();
        logger.info("==== loadData END: {} ms ====", Duration.between(start, end).toMillis());
        logger.info("Resumen: total assignments={}, conflict-free={}, conflicted-pairs={}",
                    assignments.size(), conflictFreeAssignments.size(), edgeConflicts.size());
    }

    /**
     * Añade una asignación y detecta conflictos con asignaciones previas.
     * @param a nueva asignación
     */
    public void addAssignment(Assignment a) {
        logger.trace("--> addAssignment START id={}", a.getId());
        Instant start = Instant.now();
        lock.lock();
        try {
            assignments.add(a);
            logger.debug("Added assignment id={} to list", a.getId());
            List<Assignment> sameDay = assignmentsByDay
                .computeIfAbsent(a.getDay(), k -> new ArrayList<>());
            logger.debug("Assignments on {} before adding: {}", a.getDay(), sameDay.size());

            boolean hasConflict = false;
            for (Assignment existing : sameDay) {
                logger.trace("Checking overlap between id={} and id={}", existing.getId(), a.getId());
                if (!overlaps(existing, a)) {
                    logger.trace("No overlap for pair {}-{}", existing.getId(), a.getId());
                    continue;
                }
                logger.debug("Overlap detected for pair {}-{}", existing.getId(), a.getId());
                Map<String, List<ConflictEdge>> conflicts = existing.conflictsWith(a);
                logger.trace("conflictsWith returned keys {}", conflicts.keySet());
                if (!conflicts.isEmpty()) {
                    String key = keyFor(existing.getId(), a.getId());
                    edgeConflicts.computeIfAbsent(key, k -> new ArrayList<>());
                    conflicts.forEach((type, edges) -> {
                        edges.forEach(edge -> {
                            edgeConflicts.get(key).add(edge);
                            logger.debug("Added edge type '{}' for key {}", edge.getType(), key);
                        });
                    });
                    hasConflict = true;
                }
            }
            if (!hasConflict) {
                conflictFreeAssignments.add(a);
                logger.debug("Assignment id={} marked as conflict-free", a.getId());
            }
            sameDay.add(a);
            logger.debug("Assignments on {} after adding: {}", a.getDay(), sameDay.size());
        } finally {
            lock.unlock();
            Instant end = Instant.now();
            logger.trace("<-- addAssignment END id={} ({} ms)", a.getId(), Duration.between(start, end).toMillis());
        }
    }

    private String keyFor(int id1, int id2) {
        return id1 < id2 ? id1 + "-" + id2 : id2 + "-" + id1;
    }

    private boolean overlaps(Assignment a1, Assignment a2) {
        boolean dayEq = a1.getDay().equals(a2.getDay());
        boolean timeOverlap = !a1.getEndTime().isBefore(a2.getStartTime())
                               && !a1.getStartTime().isAfter(a2.getEndTime());
        logger.trace("overlaps check for {}-{}: dayEq={}, timeOverlap={}",
                     a1.getId(), a2.getId(), dayEq, timeOverlap);
        return dayEq && timeOverlap;
    }

    public List<Assignment> getAssignments() {
        logger.debug("getAssignments called, returning {} items", assignments.size());
        return Collections.unmodifiableList(assignments);
    }

    public Set<Assignment> getConflictFreeAssignments() {
        logger.debug("getConflictFreeAssignments called, returning {} items", conflictFreeAssignments.size());
        return Collections.unmodifiableSet(conflictFreeAssignments);
    }

    public Map<String, List<ConflictEdge>> getEdgeConflicts() {
        logger.debug("getEdgeConflicts called, returning {} items", edgeConflicts.size());
        return Collections.unmodifiableMap(edgeConflicts);
    }
}
