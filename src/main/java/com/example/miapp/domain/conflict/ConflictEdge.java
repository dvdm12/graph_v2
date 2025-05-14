package com.example.miapp.domain.conflict;

import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.Objects;

/**
 * Representa un conflicto entre dos asignaciones con un tipo semántico.
 * Extiende DefaultEdge para su uso en grafos de JGraphT, con serialización
 * y métodos de igualdad mejorados.
 */
public class ConflictEdge extends DefaultEdge {
    @Serial
    private static final long serialVersionUID = 2L; // Incrementado para la versión refactorizada
    private static final Logger logger = LoggerFactory.getLogger(ConflictEdge.class);
    
    private transient ConflictType type; // Transient para controlar la serialización

    /**
     * Crea una arista de conflicto con un tipo específico.
     * @param type tipo de conflicto
     * @throws NullPointerException si el tipo es null
     */
    public ConflictEdge(ConflictType type) {
        this.type = Objects.requireNonNull(type, "El tipo de conflicto no puede ser null");
        if (logger.isDebugEnabled()) {
            logger.debug("Creado conflicto de tipo: {}", type.getLabel());
        }
    }

    /**
     * Obtiene el tipo de conflicto.
     * @return tipo de conflicto
     */
    public ConflictType getType() {
        return type;
    }
    
    /**
     * Obtiene la descripción legible del conflicto.
     * @return descripción del conflicto
     */
    public String getDescription() {
        return type.getLabel();
    }
    
    /**
     * Obtiene la categoría del conflicto.
     * @return categoría del conflicto
     */
    public ConflictType.Category getCategory() {
        return type.getCategory();
    }
    
    /**
     * Determina si este conflicto es del tipo especificado.
     * @param conflictType tipo a comprobar
     * @return true si coincide, false en caso contrario
     * @throws NullPointerException si conflictType es null
     */
    public boolean isType(ConflictType conflictType) {
        Objects.requireNonNull(conflictType, "El tipo de conflicto a comprobar no puede ser null");
        return this.type == conflictType;
    }
    
    /**
     * Determina si este conflicto es de la categoría especificada.
     * @param category categoría a comprobar
     * @return true si el conflicto pertenece a la categoría, false en caso contrario
     * @throws NullPointerException si category es null
     */
    public boolean isInCategory(ConflictType.Category category) {
        Objects.requireNonNull(category, "La categoría no puede ser null");
        return type.isInCategory(category);
    }
    
    /**
     * Determina si este conflicto está relacionado con el profesor.
     * @return true si es un conflicto de profesor, false en caso contrario
     */
    public boolean isProfessorRelated() {
        return type.isProfessorRelated();
    }
    
    /**
     * Determina si este conflicto está relacionado con recursos.
     * @return true si es un conflicto de recursos, false en caso contrario
     */
    public boolean isResourceRelated() {
        return type.isResourceRelated();
    }
    
    /**
     * Determina si este conflicto está relacionado con estudiantes.
     * @return true si es un conflicto relacionado con estudiantes, false en caso contrario
     */
    public boolean isStudentRelated() {
        return type.isStudentRelated();
    }
    
    /**
     * Determina si este conflicto está relacionado con horarios.
     * @return true si es un conflicto de horarios, false en caso contrario
     */
    public boolean isScheduleRelated() {
        return type.isScheduleRelated();
    }

    /**
     * Versión adaptada para mostrar información relevante del conflicto.
     * @return representación en cadena del conflicto
     */
    @Override
    public String toString() {
        return String.format("Conflicto[tipo=%s, categoría=%s, origen=%s, destino=%s]", 
                           type, 
                           type.getCategory(),
                           getSource(),
                           getTarget());
    }

    /**
     * Calcula el código hash basado en el tipo de conflicto y los vértices conectados.
     * Implementación compatible con DefaultEdge.
     * @return código hash
     */
    @Override
    public int hashCode() {
        return Objects.hash(
            type, 
            getSource(), 
            getTarget()
        );
    }

    /**
     * Compara si este conflicto es igual a otro objeto.
     * Dos conflictos son iguales si tienen el mismo tipo y conectan los mismos vértices.
     * @param obj objeto a comparar
     * @return true si son iguales, false en caso contrario
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ConflictEdge)) return false;
        if (!super.equals(obj)) return false; // Incluye la comparación de DefaultEdge
        
        ConflictEdge other = (ConflictEdge) obj;
        return type == other.type;
    }
    
    /**
     * Serialización personalizada para manejar correctamente el campo transient.
     * @param out flujo de salida
     * @throws IOException si ocurre un error de E/S
     */
    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        // Escribir el nombre del enum como String para evitar problemas de serialización
        out.writeUTF(type.name());
    }
    
    /**
     * Deserialización personalizada para reconstruir el campo transient.
     * @param in flujo de entrada
     * @throws IOException si ocurre un error de E/S
     * @throws ClassNotFoundException si no se puede encontrar la clase
     */
    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        // Reconstruir el enum a partir del nombre
        String typeName = in.readUTF();
        try {
            this.type = ConflictType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            logger.error("Error al deserializar ConflictType: {}", typeName, e);
            // Usar un valor por defecto para evitar NPE
            this.type = ConflictType.PROFESSOR; 
        }
    }
}