package com.example.miapp.domain;

import com.example.miapp.exception.DomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.*;

/**
 * Define las franjas horarias válidas para asignaciones académicas y proporciona
 * métodos para validar horarios según reglas institucionales.
 * 
 * Implementa restricciones realistas de horarios académicos:
 * - Franjas horarias específicas por día
 * - Regla de sobrecarga para evitar franjas consecutivas pesadas
 */
public class TimeSlot {
    private static final Logger logger = LoggerFactory.getLogger(TimeSlot.class);
    
    /**
     * Mapa que define las franjas horarias válidas para cada día de la semana.
     * Inmutable después de la inicialización.
     */
    private static final Map<DayOfWeek, List<TimeRange>> VALID_TIME_SLOTS;
    
    /**
     * Mapa para convertir entre nombres de días en español/inglés y DayOfWeek
     */
    private static final Map<String, DayOfWeek> DAY_NAME_MAP;
    
    // Inicialización estática de las franjas horarias válidas
    static {
        // Inicializar mapa de conversión de nombres de días
        Map<String, DayOfWeek> dayMap = new HashMap<>();
        // Nombres en inglés
        dayMap.put("MONDAY", DayOfWeek.MONDAY);
        dayMap.put("TUESDAY", DayOfWeek.TUESDAY);
        dayMap.put("WEDNESDAY", DayOfWeek.WEDNESDAY);
        dayMap.put("THURSDAY", DayOfWeek.THURSDAY);
        dayMap.put("FRIDAY", DayOfWeek.FRIDAY);
        dayMap.put("SATURDAY", DayOfWeek.SATURDAY);
        dayMap.put("SUNDAY", DayOfWeek.SUNDAY);
        // Nombres en español
        dayMap.put("LUNES", DayOfWeek.MONDAY);
        dayMap.put("MARTES", DayOfWeek.TUESDAY);
        dayMap.put("MIÉRCOLES", DayOfWeek.WEDNESDAY);
        dayMap.put("MIERCOLES", DayOfWeek.WEDNESDAY);
        dayMap.put("JUEVES", DayOfWeek.THURSDAY);
        dayMap.put("VIERNES", DayOfWeek.FRIDAY);
        dayMap.put("SÁBADO", DayOfWeek.SATURDAY);
        dayMap.put("SABADO", DayOfWeek.SATURDAY);
        dayMap.put("DOMINGO", DayOfWeek.SUNDAY);
        
        DAY_NAME_MAP = Collections.unmodifiableMap(dayMap);
        
        // Inicializar franjas horarias válidas
        Map<DayOfWeek, List<TimeRange>> slots = new EnumMap<>(DayOfWeek.class);
        
        // Franjas para Lunes a Viernes (días laborables)
        List<TimeRange> weekdaySlots = Arrays.asList(
            new TimeRange(LocalTime.of(8, 0), LocalTime.of(12, 0)),
            new TimeRange(LocalTime.of(14, 0), LocalTime.of(16, 0)),
            new TimeRange(LocalTime.of(16, 0), LocalTime.of(18, 0)),
            new TimeRange(LocalTime.of(18, 0), LocalTime.of(22, 0))
        );
        
        // Asignar las mismas franjas para todos los días laborables
        slots.put(DayOfWeek.MONDAY, weekdaySlots);
        slots.put(DayOfWeek.TUESDAY, weekdaySlots);
        slots.put(DayOfWeek.WEDNESDAY, weekdaySlots);
        slots.put(DayOfWeek.THURSDAY, weekdaySlots);
        slots.put(DayOfWeek.FRIDAY, weekdaySlots);
        
        // Franja para Sábado
        slots.put(DayOfWeek.SATURDAY, Collections.singletonList(
            new TimeRange(LocalTime.of(14, 0), LocalTime.of(17, 0))
        ));
        
        // Domingo no tiene franjas válidas (no se permiten clases)
        slots.put(DayOfWeek.SUNDAY, Collections.emptyList());
        
        VALID_TIME_SLOTS = Collections.unmodifiableMap(slots);
        
        logger.info("TimeSlot inicializado con {} días configurados", VALID_TIME_SLOTS.size());
    }
    
    /**
     * Convierte un nombre de día en formato String a un objeto DayOfWeek.
     * Acepta nombres en inglés o español, ignorando mayúsculas/minúsculas.
     * 
     * @param dayName Nombre del día (ej: "Monday", "Lunes")
     * @return Objeto DayOfWeek correspondiente
     * @throws DomainException si el nombre del día no es reconocido
     */
    public static DayOfWeek parseDayOfWeek(String dayName) {
        if (dayName == null || dayName.trim().isEmpty()) {
            throw new DomainException("El nombre del día no puede ser nulo o vacío");
        }
        
        DayOfWeek day = DAY_NAME_MAP.get(dayName.toUpperCase());
        if (day == null) {
            throw new DomainException("Nombre de día no reconocido: " + dayName);
        }
        
        return day;
    }
    
    /**
     * Obtiene el nombre localizado de un día de la semana.
     * 
     * @param day DayOfWeek a convertir
     * @param locale Locale para la localización (null para usar el predeterminado)
     * @return Nombre del día en el idioma especificado
     */
    public static String getDayName(DayOfWeek day, Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return day.getDisplayName(TextStyle.FULL, locale);
    }
    
    /**
     * Verifica si un rango de tiempo está dentro de las franjas válidas para un día específico.
     * 
     * @param dayOfWeek Día de la semana
     * @param startTime Hora de inicio
     * @param endTime Hora de fin
     * @return true si el rango está completamente dentro de una franja válida, false en caso contrario
     */
    public static boolean isValidTimeRange(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        Objects.requireNonNull(dayOfWeek, "El día no puede ser nulo");
        Objects.requireNonNull(startTime, "La hora de inicio no puede ser nula");
        Objects.requireNonNull(endTime, "La hora de fin no puede ser nula");
        
        // Verificar que el rango sea coherente
        if (!endTime.isAfter(startTime)) {
            throw new DomainException("La hora de fin debe ser posterior a la hora de inicio");
        }
        
        List<TimeRange> validRanges = VALID_TIME_SLOTS.getOrDefault(dayOfWeek, Collections.emptyList());
        
        // Si no hay franjas válidas para este día, el rango no es válido
        if (validRanges.isEmpty()) {
            logger.debug("No hay franjas válidas definidas para {}", dayOfWeek);
            return false;
        }
        
        // Verificar si el rango propuesto está completamente contenido en alguna franja válida
        boolean isValid = validRanges.stream()
            .anyMatch(range -> range.contains(startTime, endTime));
            
        if (!isValid && logger.isDebugEnabled()) {
            logger.debug("Rango inválido {}-{} para {}. Franjas válidas: {}", 
                       startTime, endTime, dayOfWeek, validRanges);
        }
        
        return isValid;
    }
    
    /**
     * Sobrecarga que acepta el nombre del día como String.
     * 
     * @param dayName Nombre del día (ej: "Monday", "Lunes")
     * @param startTime Hora de inicio
     * @param endTime Hora de fin
     * @return true si el rango está completamente dentro de una franja válida, false en caso contrario
     */
    public static boolean isValidTimeRange(String dayName, LocalTime startTime, LocalTime endTime) {
        DayOfWeek dayOfWeek = parseDayOfWeek(dayName);
        return isValidTimeRange(dayOfWeek, startTime, endTime);
    }
    
    /**
     * Verifica si una asignación en la franja de 4-6pm impediría otra asignación en la franja 6-8pm
     * o viceversa, para evitar sobrecarga horaria del profesor.
     * 
     * @param existingStartTime Hora de inicio de una asignación existente
     * @param existingEndTime Hora de fin de una asignación existente
     * @param newStartTime Hora de inicio de la nueva asignación propuesta
     * @param newEndTime Hora de fin de la nueva asignación propuesta
     * @return true si hay un conflicto de sobrecarga horaria, false en caso contrario
     */
    public static boolean hasWorkloadConflict(
            LocalTime existingStartTime, LocalTime existingEndTime,
            LocalTime newStartTime, LocalTime newEndTime) {
        
        // Definir las franjas críticas
        LocalTime afternoonStart = LocalTime.of(16, 0);
        LocalTime afternoonEnd = LocalTime.of(18, 0);
        LocalTime eveningStart = LocalTime.of(18, 0);
        LocalTime eveningEnd = LocalTime.of(20, 0);
        
        // Verificar si la asignación existente está en la franja de 4-6pm
        boolean existingInAfternoon = existingStartTime.equals(afternoonStart) && 
                                    existingEndTime.equals(afternoonEnd);
        
        // Verificar si la nueva asignación está en la franja de 6-8pm
        boolean newInEvening = newStartTime.equals(eveningStart) && 
                              (newEndTime.equals(eveningEnd) || newEndTime.isAfter(eveningEnd));
        
        // También verificar el caso inverso
        boolean newInAfternoon = newStartTime.equals(afternoonStart) && 
                                newEndTime.equals(afternoonEnd);
        boolean existingInEvening = existingStartTime.equals(eveningStart) && 
                                   (existingEndTime.equals(eveningEnd) || 
                                    existingEndTime.isAfter(eveningEnd));
        
        // Hay conflicto si una asignación está en 4-6pm y la otra en 6-8pm
        boolean hasConflict = (existingInAfternoon && newInEvening) || 
                             (newInAfternoon && existingInEvening);
                             
        if (hasConflict && logger.isDebugEnabled()) {
            logger.debug("Detectado conflicto de sobrecarga: franja {}-{} seguida de {}-{}", 
                       existingStartTime, existingEndTime, newStartTime, newEndTime);
        }
        
        return hasConflict;
    }
    
    /**
     * Encuentra la franja válida que mejor se ajusta a un rango de tiempo deseado.
     * Útil para sugerir alternativas cuando un rango propuesto no es válido.
     * 
     * @param dayOfWeek Día de la semana
     * @param preferredStartTime Hora de inicio preferida
     * @param preferredEndTime Hora de fin preferida
     * @return Optional con la franja más cercana, o vacío si no hay franjas válidas
     */
    public static Optional<TimeRange> findClosestValidTimeSlot(
            DayOfWeek dayOfWeek, LocalTime preferredStartTime, LocalTime preferredEndTime) {
        
        Objects.requireNonNull(dayOfWeek, "El día no puede ser nulo");
        Objects.requireNonNull(preferredStartTime, "La hora de inicio no puede ser nula");
        Objects.requireNonNull(preferredEndTime, "La hora de fin no puede ser nula");
        
        List<TimeRange> validRanges = VALID_TIME_SLOTS.getOrDefault(dayOfWeek, Collections.emptyList());
        if (validRanges.isEmpty()) {
            return Optional.empty();
        }
        
        // Duración deseada en minutos
        long preferredDuration = java.time.Duration.between(preferredStartTime, preferredEndTime).toMinutes();
        
        // Encontrar la franja que mejor se adapta
        return validRanges.stream()
            // Filtrar franjas con duración suficiente
            .filter(range -> range.getDurationMinutes() >= preferredDuration)
            // Ordenar por cercanía a la hora preferida
            .min((r1, r2) -> {
                long dist1 = r1.distanceMinutes(preferredStartTime);
                long dist2 = r2.distanceMinutes(preferredStartTime);
                return Long.compare(dist1, dist2);
            });
    }
    
    /**
     * Obtiene todas las franjas horarias válidas para un día específico.
     * 
     * @param dayOfWeek Día de la semana
     * @return Lista inmutable de franjas válidas (vacía si no hay franjas para ese día)
     */
    public static List<TimeRange> getValidTimeSlots(DayOfWeek dayOfWeek) {
        Objects.requireNonNull(dayOfWeek, "El día no puede ser nulo");
        List<TimeRange> slots = VALID_TIME_SLOTS.getOrDefault(dayOfWeek, Collections.emptyList());
        return Collections.unmodifiableList(slots);
    }
    
    /**
     * Clase inmutable que representa un rango de tiempo.
     */
    public static class TimeRange {
        private final LocalTime start;
        private final LocalTime end;
        
        /**
         * Crea un nuevo rango de tiempo.
         * 
         * @param start Hora de inicio
         * @param end Hora de fin
         * @throws IllegalArgumentException si end no es posterior a start
         */
        public TimeRange(LocalTime start, LocalTime end) {
            this.start = Objects.requireNonNull(start, "La hora de inicio no puede ser nula");
            this.end = Objects.requireNonNull(end, "La hora de fin no puede ser nula");
            
            if (!end.isAfter(start)) {
                throw new IllegalArgumentException("La hora de fin debe ser posterior a la hora de inicio");
            }
        }
        
        /**
         * Verifica si un rango de tiempo está completamente contenido en esta franja.
         * 
         * @param otherStart Hora de inicio del otro rango
         * @param otherEnd Hora de fin del otro rango
         * @return true si el otro rango está completamente contenido en este, false en caso contrario
         */
        public boolean contains(LocalTime otherStart, LocalTime otherEnd) {
            return !otherStart.isBefore(this.start) && !otherEnd.isAfter(this.end);
        }
        
        /**
         * Calcula la distancia en minutos entre este rango y una hora específica.
         * 
         * @param time Hora a comparar
         * @return Distancia en minutos (0 si la hora está dentro del rango)
         */
        public long distanceMinutes(LocalTime time) {
            if (!time.isBefore(this.start) && !time.isAfter(this.end)) {
                return 0; // Está dentro del rango
            }
            
            if (time.isBefore(this.start)) {
                return java.time.Duration.between(time, this.start).toMinutes();
            } else {
                return java.time.Duration.between(this.end, time).toMinutes();
            }
        }
        
        /**
         * Calcula la duración de este rango en minutos.
         * 
         * @return Duración en minutos
         */
        public long getDurationMinutes() {
            return java.time.Duration.between(start, end).toMinutes();
        }
        
        /**
         * Obtiene la hora de inicio del rango.
         * 
         * @return Hora de inicio
         */
        public LocalTime getStart() { 
            return start; 
        }
        
        /**
         * Obtiene la hora de fin del rango.
         * 
         * @return Hora de fin
         */
        public LocalTime getEnd() { 
            return end; 
        }
        
        @Override
        public String toString() {
            return start.toString() + " - " + end.toString();
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TimeRange)) return false;
            TimeRange other = (TimeRange) o;
            return Objects.equals(start, other.start) && 
                   Objects.equals(end, other.end);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(start, end);
        }
    }
    
    // Constructor privado para evitar instanciación
    private TimeSlot() {
        throw new UnsupportedOperationException("Esta clase no debe ser instanciada");
    }
}