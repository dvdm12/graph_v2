package com.example.miapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Exporta la estructura de conflictos a JSON usando datos de ConflictGraphLoader,
 * con logging exhaustivo para depuración.
 */
public class GraphExporter {
    private static final Logger logger = LoggerFactory.getLogger(GraphExporter.class);

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
        logger.info("==== exportToJson START ====");
        long startTime = System.currentTimeMillis();
        ObjectNode root = mapper.createObjectNode();
        ArrayNode nodesArray = mapper.createArrayNode();
        ArrayNode edgesArray = mapper.createArrayNode();

        // Nodos
        logger.debug("Exportando {} nodos", loader.getAssignments().size());
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
            logger.trace("Nodo agregado: id={}", a.getId());
        }
        root.set("nodes", nodesArray);

        // Aristas
        logger.debug("Exportando {} aristas", loader.getEdgeConflicts().size());
        for (Map.Entry<String, List<ConflictEdge>> entry : loader.getEdgeConflicts().entrySet()) {
            String key = entry.getKey();
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
                logger.trace("Arista {}-{} tipo agregado: {}", id1, id2, ce.getType());
            }
            edge.set("conflicts", conflicts);
            edgesArray.add(edge);
            logger.trace("Arista procesada: {}", key);
        }
        root.set("edges", edgesArray);

        mapper.writerWithDefaultPrettyPrinter()
              .writeValue(new File(filePath), root);
        long endTime = System.currentTimeMillis();
        logger.info("==== exportToJson END: {} ms ====", (endTime - startTime));
    }
}
