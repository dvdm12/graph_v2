package com.example.miapp.domain;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.miapp.exception.DomainException;

/**
 * Representa una materia académica.
 */
public class Subject {
    private static final Logger logger = LoggerFactory.getLogger(Subject.class);
    private static final int MIN_CREDITS = 1;
    private static final int MAX_CREDITS = 10;
    
    private final String code;
    private String name;
    private String description;
    private int credits;
    private boolean requiresLab; // Indica si la materia requiere laboratorio
    
    /**
     * Constructor de la materia.
     * 
     * @param code Código único de la materia
     * @param name Nombre de la materia
     * @param description Descripción de la materia
     * @param credits Número de créditos
     * @param requiresLab Indica si requiere laboratorio
     * @throws NullPointerException si code, name o description son null
     * @throws DomainException si el código no es válido o los créditos están fuera de rango
     */
    public Subject(String code, String name, String description, int credits, boolean requiresLab) {
        this.code = validateCode(code);
        this.name = Objects.requireNonNull(name, "El nombre no puede ser null");
        this.description = Objects.requireNonNull(description, "La descripción no puede ser null");
        this.credits = validateCredits(credits);
        this.requiresLab = requiresLab;
        
        if (logger.isDebugEnabled()) {
            logger.debug("Materia creada: código={}, nombre={}, créditos={}, requiereLab={}", 
                       code, name, credits, requiresLab);
        }
    }
    
    /**
     * Valida el código de la materia.
     * 
     * @param code Código a validar
     * @return Código validado
     * @throws NullPointerException si code es null
     * @throws DomainException si el código no cumple con el formato esperado
     */
    private String validateCode(String code) {
        Objects.requireNonNull(code, "El código no puede ser null");
        
        // Verificar que el código tenga un formato válido (alfanumérico, sin espacios)
        if (!code.matches("^[A-Z0-9]{2,10}$")) {
            throw new DomainException("El código de materia debe ser alfanumérico, mayúsculas, sin espacios y entre 2 y 10 caracteres: " + code);
        }
        
        return code;
    }
    
    /**
     * Valida el número de créditos.
     * 
     * @param credits Créditos a validar
     * @return Créditos validados
     * @throws DomainException si los créditos están fuera del rango permitido
     */
    private int validateCredits(int credits) {
        if (credits < MIN_CREDITS || credits > MAX_CREDITS) {
            throw new DomainException(
                String.format("El número de créditos debe estar entre %d y %d: %d", 
                        MIN_CREDITS, MAX_CREDITS, credits));
        }
        
        return credits;
    }
    
    // Getters y setters
    
    /**
     * Obtiene el código de la materia.
     * 
     * @return Código único de la materia
     */
    public String getCode() {
        return code;
    }
    
    /**
     * Obtiene el nombre de la materia.
     * 
     * @return Nombre de la materia
     */
    public String getName() {
        return name;
    }
    
    /**
     * Establece el nombre de la materia.
     * 
     * @param name Nuevo nombre
     * @throws NullPointerException si name es null
     */
    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "El nombre no puede ser null");
    }
    
    /**
     * Obtiene la descripción de la materia.
     * 
     * @return Descripción de la materia
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Establece la descripción de la materia.
     * 
     * @param description Nueva descripción
     * @throws NullPointerException si description es null
     */
    public void setDescription(String description) {
        this.description = Objects.requireNonNull(description, "La descripción no puede ser null");
    }
    
    /**
     * Obtiene el número de créditos de la materia.
     * 
     * @return Número de créditos
     */
    public int getCredits() {
        return credits;
    }
    
    /**
     * Establece el número de créditos de la materia.
     * 
     * @param credits Nuevo número de créditos
     * @throws DomainException si los créditos están fuera del rango permitido
     */
    public void setCredits(int credits) {
        this.credits = validateCredits(credits);
    }
    
    /**
     * Indica si la materia requiere laboratorio.
     * 
     * @return true si requiere laboratorio, false en caso contrario
     */
    public boolean requiresLab() {
        return requiresLab;
    }
    
    /**
     * Establece si la materia requiere laboratorio.
     * 
     * @param requiresLab Nuevo valor
     */
    public void setRequiresLab(boolean requiresLab) {
        this.requiresLab = requiresLab;
    }
    
    /**
     * Verifica si esta materia es compatible con el aula proporcionada.
     * 
     * @param room Aula a verificar
     * @return true si es compatible, false en caso contrario
     * @throws NullPointerException si room es null
     */
    public boolean isCompatibleWith(Room room) {
        Objects.requireNonNull(room, "El aula no puede ser null");
        return !requiresLab || room.isLab();
    }
    
    @Override
    public String toString() {
        return String.format("%s - %s (créditos: %d, lab: %b)", code, name, credits, requiresLab);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Subject subject = (Subject) obj;
        return Objects.equals(code, subject.code);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(code);
    }
}