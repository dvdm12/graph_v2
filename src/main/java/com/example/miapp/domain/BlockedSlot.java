package com.example.miapp.domain;

import java.time.LocalTime;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.example.miapp.exception.BlockedSlotConflictException;
import com.example.miapp.exception.DomainException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Representa una franja horaria bloqueada para un profesor,
 * con día, hora de inicio y hora de fin.
 * Implementa un objeto inmutable con validación completa.
 */
@JsonDeserialize(builder = BlockedSlot.Builder.class)
public class BlockedSlot {
    private static final Logger logger = LoggerFactory.getLogger(BlockedSlot.class);
    private static final Set<String> VALID_DAYS = new HashSet<>(
        Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"));
    private static final int MIN_DURATION_MINUTES = 30; // Duración mínima de una franja en minutos
    
    private final String day;
    private final LocalTime startTime;
    private final LocalTime endTime;

    private BlockedSlot(Builder b) {
        // Validación de campos obligatorios con mensajes específicos
        this.day = validateDay(b.day);
        this.startTime = Objects.requireNonNull(b.startTime, "El campo 'startTime' no puede ser null");
        this.endTime = Objects.requireNonNull(b.endTime, "El campo 'endTime' no puede ser null");
        
        validateTimeRange();
        
        if (logger.isDebugEnabled()) {
            logger.debug("BlockedSlot creado: {} de {} a {}", day, startTime, endTime);
        }
    }
    
    /**
     * Valida que el día sea válido.
     * 
     * @param day Día a validar
     * @return Día validado
     * @throws NullPointerException si day es null
     * @throws DomainException si el día no es válido
     */
    private String validateDay(String day) {
        Objects.requireNonNull(day, "El campo 'day' no puede ser null");
        
        if (!VALID_DAYS.contains(day)) {
            throw new DomainException(String.format(
                "Día '%s' no válido. Debe ser uno de: %s", 
                day, String.join(", ", VALID_DAYS)));
        }
        
        return day;
    }
    
    /**
     * Valida que el rango de tiempo sea coherente.
     * 
     * @throws DomainException si endTime no es posterior a startTime
     * @throws DomainException si la duración es menor que la mínima permitida
     */
    private void validateTimeRange() {
        if (!endTime.isAfter(startTime)) {
            String errorMsg = String.format(
                "endTime (%s) debe ser posterior a startTime (%s)", 
                endTime, startTime);
            logger.error(errorMsg);
            throw new DomainException(errorMsg);
        }
        
        // Verificar duración mínima
        long durationMinutes = Duration.between(startTime, endTime).toMinutes();
        if (durationMinutes < MIN_DURATION_MINUTES) {
            String errorMsg = String.format(
                "La duración de la franja (%d minutos) debe ser al menos %d minutos", 
                durationMinutes, MIN_DURATION_MINUTES);
            logger.error(errorMsg);
            throw new DomainException(errorMsg);
        }
    }

    /**
     * Verifica si esta franja bloqueada se solapa con un rango de tiempo dado.
     * 
     * @param otherDay día a comparar
     * @param otherStart hora de inicio a comparar
     * @param otherEnd hora de fin a comparar
     * @return true si hay solapamiento, false en caso contrario
     * @throws NullPointerException si algún parámetro es null
     */
    public boolean overlaps(String otherDay, LocalTime otherStart, LocalTime otherEnd) {
        // Validación de parámetros
        Objects.requireNonNull(otherDay, "El día a comparar no puede ser null");
        Objects.requireNonNull(otherStart, "La hora de inicio a comparar no puede ser null");
        Objects.requireNonNull(otherEnd, "La hora de fin a comparar no puede ser null");
        
        if (!this.day.equals(otherDay)) {
            return false;
        }
        
        boolean result = !this.endTime.isBefore(otherStart) && !this.startTime.isAfter(otherEnd);
        
        if (result && logger.isTraceEnabled()) {
            logger.trace("Overlap detectado: {} ({} - {}) solapa con {} ({} - {})",
                       this.day, this.startTime, this.endTime,
                       otherDay, otherStart, otherEnd);
        }
        
        return result;
    }
    
    /**
     * Verifica si esta franja bloqueada se solapa con un rango de tiempo dado y lanza
     * una excepción si existe solapamiento.
     * 
     * @param otherDay día a comparar
     * @param otherStart hora de inicio a comparar
     * @param otherEnd hora de fin a comparar
     * @param professorId ID del profesor (para la excepción)
     * @throws NullPointerException si algún parámetro es null
     * @throws BlockedSlotConflictException si hay solapamiento
     */
    public void verifyNoOverlap(String otherDay, LocalTime otherStart, LocalTime otherEnd, int professorId) {
        // Validación de parámetros
        Objects.requireNonNull(otherDay, "El día a comparar no puede ser null");
        Objects.requireNonNull(otherStart, "La hora de inicio a comparar no puede ser null");
        Objects.requireNonNull(otherEnd, "La hora de fin a comparar no puede ser null");
        
        if (this.day.equals(otherDay) && 
            !this.endTime.isBefore(otherStart) && !this.startTime.isAfter(otherEnd)) {
            
            throw new BlockedSlotConflictException(
                String.format("La franja propuesta (%s de %s a %s) se solapa con una franja bloqueada existente (%s de %s a %s)",
                        otherDay, otherStart, otherEnd, 
                        this.day, this.startTime, this.endTime),
                professorId,
                this.day,
                this.startTime.toString(),
                this.endTime.toString());
        }
    }
    
    /**
     * Verifica si esta franja bloqueada se solapa con otra.
     * 
     * @param other otra franja bloqueada
     * @return true si hay solapamiento, false en caso contrario
     * @throws NullPointerException si other es null
     */
    public boolean overlaps(BlockedSlot other) {
        Objects.requireNonNull(other, "La franja a comparar no puede ser null");
        return overlaps(other.day, other.startTime, other.endTime);
    }
    
    /**
     * Verifica si esta franja bloqueada se solapa con otra y lanza una excepción si existe solapamiento.
     * 
     * @param other otra franja bloqueada
     * @param professorId ID del profesor (para la excepción)
     * @throws NullPointerException si other es null
     * @throws BlockedSlotConflictException si hay solapamiento
     */
    public void verifyNoOverlap(BlockedSlot other, int professorId) {
        Objects.requireNonNull(other, "La franja a comparar no puede ser null");
        verifyNoOverlap(other.day, other.startTime, other.endTime, professorId);
    }
    
    /**
     * Calcula la duración de esta franja en minutos.
     * 
     * @return Duración en minutos
     */
    public long getDurationMinutes() {
        return Duration.between(startTime, endTime).toMinutes();
    }
    
    /**
     * Crea un builder pre-configurado con todos los valores actuales.
     * Facilita la creación de copias modificadas.
     * 
     * @return Un Builder pre-configurado
     */
    public Builder toBuilder() {
        return new Builder()
            .day(this.day)
            .startTime(this.startTime)
            .endTime(this.endTime);
    }
    
    /**
     * Crea una nueva franja con día actualizado.
     * 
     * @param day el nuevo día
     * @return nueva franja con el día actualizado
     * @throws NullPointerException si day es null
     * @throws DomainException si el día no es válido
     */
    public BlockedSlot withDay(String day) {
        return toBuilder().day(day).build();
    }
    
    /**
     * Crea una nueva franja con hora de inicio actualizada.
     * 
     * @param startTime la nueva hora de inicio
     * @return nueva franja con la hora actualizada
     * @throws NullPointerException si startTime es null
     * @throws DomainException si el rango de tiempo resulta inválido
     */
    public BlockedSlot withStartTime(LocalTime startTime) {
        return toBuilder().startTime(startTime).build();
    }
    
    /**
     * Crea una nueva franja con hora de fin actualizada.
     * 
     * @param endTime la nueva hora de fin
     * @return nueva franja con la hora actualizada
     * @throws NullPointerException si endTime es null
     * @throws DomainException si el rango de tiempo resulta inválido
     */
    public BlockedSlot withEndTime(LocalTime endTime) {
        return toBuilder().endTime(endTime).build();
    }

    // Getters
    /**
     * Obtiene el día de la franja bloqueada.
     * 
     * @return Día de la semana
     */
    public String getDay() {
        return day;
    }

    /**
     * Obtiene la hora de inicio de la franja bloqueada.
     * 
     * @return Hora de inicio
     */
    public LocalTime getStartTime() {
        return startTime;
    }

    /**
     * Obtiene la hora de fin de la franja bloqueada.
     * 
     * @return Hora de fin
     */
    public LocalTime getEndTime() {
        return endTime;
    }
    
    @Override
    public String toString() {
        return String.format("BlockedSlot{day='%s', startTime=%s, endTime=%s}", 
                           day, startTime, endTime);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockedSlot that = (BlockedSlot) o;
        return Objects.equals(day, that.day) &&
               Objects.equals(startTime, that.startTime) &&
               Objects.equals(endTime, that.endTime);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(day, startTime, endTime);
    }

    /**
     * Builder para BlockedSlot.
     */
    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private String day;
        private LocalTime startTime;
        private LocalTime endTime;

        /**
         * Establece el día para la franja bloqueada.
         * 
         * @param day Día de la semana
         * @return Builder para encadenamiento
         */
        public Builder day(String day) {
            this.day = day;
            return this;
        }

        /**
         * Establece la hora de inicio para la franja bloqueada.
         * 
         * @param startTime Hora de inicio
         * @return Builder para encadenamiento
         */
        public Builder startTime(LocalTime startTime) {
            this.startTime = startTime;
            return this;
        }

        /**
         * Establece la hora de fin para la franja bloqueada.
         * 
         * @param endTime Hora de fin
         * @return Builder para encadenamiento
         */
        public Builder endTime(LocalTime endTime) {
            this.endTime = endTime;
            return this;
        }

        /**
         * Construye el objeto BlockedSlot con los valores establecidos.
         * 
         * @return Nuevo objeto BlockedSlot
         * @throws NullPointerException si algún campo obligatorio es null
         * @throws DomainException si el día no es válido o el rango de tiempo es incoherente
         */
        public BlockedSlot build() {
            return new BlockedSlot(this);
        }
    }
}