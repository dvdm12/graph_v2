package com.example.miapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Exporta la estructura de conflictos a JSON usando datos de ConflictGraphLoader.
 */
public class GraphExporter {
    private final ConflictGraphLoader loader;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    public GraphExporter(ConflictGraphLoader loader) {
        this.loader = loader;
    }

    /**
     * Genera un JSON con nodos y aristas basados en los conflictos detectados.
     * @param filePath ruta de salida del JSON
     * @throws IOException si hay error de escritura
     */
    public void exportToJson(String filePath) throws IOException {
        ObjectNode root = mapper.createObjectNode();
        ArrayNode nodesArray = mapper.createArrayNode();
        ArrayNode edgesArray = mapper.createArrayNode();

        // Nodos
        for (Assignment a : loader.getAssignments()) {
            ObjectNode node = mapper.createObjectNode();
            node.put("id", a.getId());
            node.put("assignmentDate", a.getAssignmentDate().toString());
            node.put("day", a.getDay());
            node.put("startTime", a.getStartTime().toString());
            node.put("endTime", a.getEndTime().toString());
            node.put("professorId", a.getProfessorId());
            node.put("professorName", a.getProfessorName());
            node.put("roomId", a.getRoomId());
            node.put("roomName", a.getRoomName());
            node.put("subjectGroupId", a.getSubjectGroupId());
            node.put("subjectCode", a.getSubjectCode());
            node.put("subjectName", a.getSubjectName());
            node.put("groupName", a.getGroupName());
            node.put("sessionType", a.getSessionType());
            node.put("requiresLab", a.isRequiresLab());
            node.put("enrolledStudents", a.getEnrolledStudents());
            nodesArray.add(node);
        }
        root.set("nodes", nodesArray);

        // Aristas
        for (Map.Entry<String, List<ConflictEdge>> entry : loader.getEdgeConflicts().entrySet()) {
            String key = entry.getKey(); // "id1-id2"
            String[] parts = key.split("-");
            int id1 = Integer.parseInt(parts[0]);
            int id2 = Integer.parseInt(parts[1]);
            ObjectNode edge = mapper.createObjectNode();
            ArrayNode between = mapper.createArrayNode();
            between.add(id1);
            between.add(id2);
            edge.set("between", between);
            ArrayNode conflicts = mapper.createArrayNode();
            for (ConflictEdge ce : entry.getValue()) {
                conflicts.add(ce.getType());
            }
            edge.set("conflicts", conflicts);
            edgesArray.add(edge);
        }
        root.set("edges", edgesArray);

        mapper.writerWithDefaultPrettyPrinter()
              .writeValue(new File(filePath), root);
    }
}
