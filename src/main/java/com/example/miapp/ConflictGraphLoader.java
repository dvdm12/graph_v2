package com.example.miapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Gestiona las asignaciones y sus conflictos de manera thread-safe,
 * optimizado con indexación por día para reducir comparaciones,
 * usando estructuras internas para múltiples conflictos.
 */
public class ConflictGraphLoader {
    private final List<Assignment> assignments = new ArrayList<>();
    private final Map<String, List<ConflictEdge>> edgeConflicts = new LinkedHashMap<>();
    private final Map<String, List<Assignment>> assignmentsByDay = new HashMap<>();
    private final Set<Assignment> conflictFreeAssignments = new LinkedHashSet<>();
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final Lock lock = new ReentrantLock();

    /**
     * Carga datos desde un archivo JSON y construye la estructura de conflictos.
     * @param jsonPath ruta al fichero JSON con asignaciones
     * @throws IOException si ocurre un error de lectura
     */
    public void loadData(String jsonPath) throws IOException {
        JsonNode root = mapper.readTree(new File(jsonPath));
        for (JsonNode node : root.get("assignments")) {
            Assignment a = mapper.treeToValue(node, Assignment.class);
            addAssignment(a);
        }
    }

    /**
     * Añade una asignación y detecta conflictos con asignaciones previas.
     * @param a nueva asignación
     */
    public void addAssignment(Assignment a) {
        lock.lock();
        try {
            assignments.add(a);
            List<Assignment> sameDay = assignmentsByDay
                .computeIfAbsent(a.getDay(), k -> new ArrayList<>());
            boolean hasConflict = false;
            for (Assignment existing : sameDay) {
                if (!overlaps(existing, a)) continue;
                Map<String, List<ConflictEdge>> conflicts = existing.conflictsWith(a);
                if (!conflicts.isEmpty()) {
                    String key = keyFor(existing.getId(), a.getId());
                    edgeConflicts.computeIfAbsent(key, k -> new ArrayList<>());
                    for (List<ConflictEdge> edges : conflicts.values()) {
                        edgeConflicts.get(key).addAll(edges);
                    }
                    hasConflict = true;
                }
            }
            if (!hasConflict) {
                conflictFreeAssignments.add(a);
            }
            sameDay.add(a);
        } finally {
            lock.unlock();
        }
    }

    private String keyFor(int id1, int id2) {
        return id1 < id2 ? id1 + "-" + id2 : id2 + "-" + id1;
    }

    private boolean overlaps(Assignment a1, Assignment a2) {
        return a1.getDay().equals(a2.getDay())
            && !a1.getEndTime().isBefore(a2.getStartTime())
            && !a1.getStartTime().isAfter(a2.getEndTime());
    }

    public List<Assignment> getAssignments() {
        return Collections.unmodifiableList(assignments);
    }

    public Set<Assignment> getConflictFreeAssignments() {
        return Collections.unmodifiableSet(conflictFreeAssignments);
    }

    public Map<String, List<ConflictEdge>> getEdgeConflicts() {
        return Collections.unmodifiableMap(edgeConflicts);
    }
}
