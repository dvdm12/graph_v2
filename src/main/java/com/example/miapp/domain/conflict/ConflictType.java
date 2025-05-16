package com.example.miapp.domain.conflict;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Enumera los distintos tipos de conflicto posibles entre asignaciones.
 * Proporciona métodos de utilidad para búsqueda y categorización.
 */
public enum ConflictType {
    /**
     * Conflicto con una franja horaria bloqueada del profesor.
     */
    PROFESSOR_BLOCKED("Conflicto con franja bloqueada del profesor", Category.PROFESSOR),
    
    /**
     * Conflicto por asignar el mismo profesor a dos clases solapadas.
     */
    PROFESSOR("Solapamiento de horarios (mismo profesor)", Category.PROFESSOR),
    
    /**
     * Conflicto por asignar la misma sala a dos clases solapadas.
     */
    ROOM("Solapamiento de horarios (misma sala)", Category.RESOURCE),
    
    /**
     * Conflicto por asignar el mismo grupo a dos clases solapadas.
     */
    GROUP("Solapamiento de horarios (mismo grupo)", Category.STUDENT),
    
    /**
     * Conflicto por programar clases en la misma jornada y horario.
     */
    SESSION_TYPE("Misma jornada y horario", Category.SCHEDULE),
    
    /**
     * Conflicto por asignar a un profesor una materia para la que no está autorizado.
     */
    PROFESSOR_SUBJECT_MISMATCH("Profesor no autorizado para impartir esta materia", Category.PROFESSOR),
    
    /**
     * Conflicto por asignar un aula con capacidad insuficiente.
     */
    ROOM_CAPACITY("Capacidad de aula insuficiente", Category.RESOURCE),
    
    /**
     * Conflicto por asignar un aula incompatible con los requisitos de la materia.
     */
    ROOM_COMPATIBILITY("Aula incompatible con requisitos de la materia", Category.RESOURCE),
    
    /**
     * Conflicto por sobrecarga horaria del profesor (franjas consecutivas pesadas).
     * Se produce cuando un profesor tiene una clase en la franja de 16:00-18:00
     * y otra en la franja de 18:00-20:00 del mismo día.
     */
    PROFESSOR_WORKLOAD("Sobrecarga horaria del profesor (franjas consecutivas pesadas)", Category.PROFESSOR),
    
    /**
     * Conflicto por programar una clase fuera de las franjas horarias válidas.
     */
    INVALID_TIME_SLOT("Horario fuera de las franjas válidas permitidas", Category.SCHEDULE);

    // Caché para búsquedas eficientes por etiqueta
    private static final Map<String, ConflictType> LABEL_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(ConflictType::getLabel, Function.identity()));
            
    // Caché de valores por categoría para evitar cálculos repetidos
    private static final Map<Category, Set<ConflictType>> BY_CATEGORY = Arrays.stream(values())
            .collect(Collectors.groupingBy(
                ConflictType::getCategory,
                Collectors.toSet()
            ));

    private final String label;
    private final Category category;

    /**
     * Categorías de conflictos para facilitar agrupación y análisis.
     */
    public enum Category {
        /**
         * Conflictos relacionados con profesores.
         */
        PROFESSOR("Profesor"),
        
        /**
         * Conflictos relacionados con recursos físicos.
         */
        RESOURCE("Recurso"),
        
        /**
         * Conflictos que afectan a estudiantes.
         */
        STUDENT("Estudiante"),
        
        /**
         * Conflictos de programación horaria.
         */
        SCHEDULE("Horario");
        
        private final String description;
        
        Category(String description) {
            this.description = description;
        }
        
        /**
         * Retorna la descripción legible de la categoría.
         * @return descripción de la categoría
         */
        public String getDescription() {
            return description;
        }
        
        /**
         * Obtiene todos los tipos de conflicto en esta categoría.
         * @return conjunto inmutable de tipos de conflicto
         */
        public Set<ConflictType> getConflictTypes() {
            return BY_CATEGORY.getOrDefault(this, EnumSet.noneOf(ConflictType.class));
        }
        
        @Override
        public String toString() {
            return description;
        }
    }

    /**
     * Constructor de ConflictType.
     * @param label descripción legible del conflicto
     * @param category categoría del conflicto
     */
    ConflictType(String label, Category category) {
        this.label = label;
        this.category = category;
    }

    /**
     * Retorna la descripción legible del conflicto.
     * @return descripción del conflicto
     */
    public String getLabel() {
        return label;
    }
    
    /**
     * Retorna la categoría del conflicto.
     * @return categoría del conflicto
     */
    public Category getCategory() {
        return category;
    }
    
    /**
     * Determina si este tipo de conflicto pertenece a la categoría especificada.
     * @param category categoría a comprobar
     * @return true si pertenece, false en caso contrario
     * @throws NullPointerException si category es null
     */
    public boolean isInCategory(Category category) {
        if (category == null) {
            throw new NullPointerException("La categoría no puede ser null");
        }
        return this.category == category;
    }
    
    /**
     * Busca un tipo de conflicto por su descripción.
     * @param label descripción a buscar
     * @return Optional con el tipo de conflicto si se encuentra, vacío en caso contrario
     * @throws NullPointerException si label es null
     */
    public static Optional<ConflictType> findByLabel(String label) {
        if (label == null) {
            throw new NullPointerException("La etiqueta no puede ser null");
        }
        return Optional.ofNullable(LABEL_MAP.get(label));
    }
    
    /**
     * Busca un tipo de conflicto por su descripción, lanzando excepción si no existe.
     * @param label descripción a buscar
     * @return el tipo de conflicto encontrado
     * @throws IllegalArgumentException si no se encuentra ningún conflicto con esa etiqueta
     * @throws NullPointerException si label es null
     */
    public static ConflictType getByLabel(String label) {
        return findByLabel(label)
            .orElseThrow(() -> new IllegalArgumentException(
                "No se encontró ningún tipo de conflicto con la etiqueta: " + label));
    }
    
    /**
     * Obtiene todos los tipos de conflicto de una categoría específica.
     * @param category categoría a filtrar
     * @return conjunto de tipos de esa categoría
     * @throws NullPointerException si category es null
     */
    public static Set<ConflictType> getByCategory(Category category) {
        if (category == null) {
            throw new NullPointerException("La categoría no puede ser null");
        }
        return category.getConflictTypes();
    }
    
    /**
     * Comprueba si un tipo de conflicto está relacionado con profesores.
     * @return true si es un conflicto relacionado con profesores
     */
    public boolean isProfessorRelated() {
        return category == Category.PROFESSOR;
    }
    
    /**
     * Comprueba si un tipo de conflicto está relacionado con recursos.
     * @return true si es un conflicto relacionado con recursos
     */
    public boolean isResourceRelated() {
        return category == Category.RESOURCE;
    }
    
    /**
     * Comprueba si un tipo de conflicto está relacionado con estudiantes.
     * @return true si es un conflicto relacionado con estudiantes
     */
    public boolean isStudentRelated() {
        return category == Category.STUDENT;
    }
    
    /**
     * Comprueba si un tipo de conflicto está relacionado con horarios.
     * @return true si es un conflicto relacionado con horarios
     */
    public boolean isScheduleRelated() {
        return category == Category.SCHEDULE;
    }

    @Override
    public String toString() {
        return label;
    }
}