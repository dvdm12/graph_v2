package com.example.miapp;

import org.jgrapht.graph.DefaultEdge;
import java.util.Objects;

/**
 * Representa un tipo de conflicto entre dos asignaciones.
 * Al usar un grafo no dirigido, no necesita flag bidireccional.
 */
public class ConflictEdge extends DefaultEdge {
    private final String type;

    public ConflictEdge(String type) {
        this.type = Objects.requireNonNull(type, "El tipo de conflicto no puede ser null");
    }

    /**
     * Retorna la descripción del conflicto.
     */
    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ConflictEdge)) return false;
        ConflictEdge other = (ConflictEdge) obj;
        return Objects.equals(this.type, other.type);
    }
}
