package com.example.miapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.miapp.domain.Assignment;
import com.example.miapp.domain.BlockedSlot;
import com.example.miapp.domain.Professor;
import com.example.miapp.domain.Subject;

import com.example.miapp.exception.AssignmentConflictException;
import com.example.miapp.exception.BlockedSlotConflictException;
import com.example.miapp.exception.DomainException;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Servicio dedicado a la detección de conflictos entre asignaciones.
 * Implementa la lógica de detección de solapamientos temporales y con franjas bloqueadas.
 * Refactorizado para mejorar rendimiento y flexibilidad.
 */
public class ConflictDetector {
    private static final Logger logger = LoggerFactory.getLogger(ConflictDetector.class);
    
    // Caché de resultados para mejorar rendimiento
    private final ConcurrentMap<CacheKey, Boolean> overlapCache = new ConcurrentHashMap<>();
    
    // Factor de tolerancia para solapamientos (en minutos)
    private final int overlapToleranceMinutes;
    
    /**
     * Constructor con parámetros por defecto.
     */
    public ConflictDetector() {
        this(0);
    }
    
    /**
     * Constructor con parámetros configurables.
     * 
     * @param overlapToleranceMinutes Minutos de tolerancia para considerar solapamiento (0 = exacto)
     */
    public ConflictDetector(int overlapToleranceMinutes) {
        this.overlapToleranceMinutes = Math.max(0, overlapToleranceMinutes);
        
        logger.info("ConflictDetector inicializado con tolerancia de {} minutos", 
                 this.overlapToleranceMinutes);
    }
    
    /**
     * Verifica si la asignación tiene conflicto con sus propias franjas bloqueadas.
     * 
     * @param assignment Asignación a verificar
     * @return true si hay al menos un conflicto con una franja bloqueada, false en caso contrario
     * @throws NullPointerException si assignment es null
     */
    public boolean checkBlockedSlotConflicts(Assignment assignment) {
        Objects.requireNonNull(assignment, "La asignación no puede ser null");
        
        Professor professor = assignment.getProfessor();
        if (professor == null) {
            logger.warn("La asignación id={} no tiene profesor asignado", assignment.getId());
            return false;
        }
        
        List<BlockedSlot> blockedSlots = professor.getBlockedSlots();
        if (blockedSlots.isEmpty()) {
            // Optimización para caso frecuente: sin franjas bloqueadas
            return false;
        }
        
        // Datos de la asignación para comparar
        String day = assignment.getDay();
        LocalTime startTime = assignment.getStartTime();
        LocalTime endTime = assignment.getEndTime();
        
        for (BlockedSlot slot : blockedSlots) {
            if (slot.getDay().equals(day) && 
                timeOverlaps(startTime, endTime, slot.getStartTime(), slot.getEndTime())) {
                
                if (logger.isDebugEnabled()) {
                    logger.debug("Assignment {} conflicts with blocked slot on {} from {} to {}", 
                               assignment.getId(), slot.getDay(), 
                               slot.getStartTime(), slot.getEndTime());
                }
                return true;
            }
        }
        
        // No se encontraron conflictos
        if (logger.isTraceEnabled()) {
            logger.trace("No blocked slot conflicts for assignment {}", assignment.getId());
        }
        return false;
    }
    
    /**
     * Verifica si la asignación tiene conflicto con sus propias franjas bloqueadas y lanza una excepción si existe.
     * 
     * @param assignment Asignación a verificar
     * @throws NullPointerException si assignment es null
     * @throws BlockedSlotConflictException si hay conflicto con una franja bloqueada
     */
    public void verifyNoBlockedSlotConflicts(Assignment assignment) {
        Objects.requireNonNull(assignment, "La asignación no puede ser null");
        
        Professor professor = assignment.getProfessor();
        if (professor == null) {
            logger.warn("La asignación id={} no tiene profesor asignado", assignment.getId());
            return;
        }
        
        List<BlockedSlot> conflictingSlots = getConflictingBlockedSlots(assignment);
        
        if (!conflictingSlots.isEmpty()) {
            BlockedSlot firstConflict = conflictingSlots.get(0);
            
            throw new BlockedSlotConflictException(
                String.format("La asignación id=%d tiene conflicto con una franja bloqueada del profesor %s en %s de %s a %s",
                        assignment.getId(), professor.getName(), firstConflict.getDay(),
                        firstConflict.getStartTime(), firstConflict.getEndTime()),
                professor.getId(),
                firstConflict.getDay(),
                firstConflict.getStartTime().toString(),
                firstConflict.getEndTime().toString());
        }
    }
    
    /**
     * Verifica si el profesor está autorizado para impartir la materia de la asignación.
     * 
     * @param assignment Asignación a verificar
     * @return true si hay conflicto (profesor no autorizado), false en caso contrario
     * @throws NullPointerException si assignment es null
     */
    public boolean checkProfessorSubjectMismatch(Assignment assignment) {
        Objects.requireNonNull(assignment, "La asignación no puede ser null");
        
        Professor professor = assignment.getProfessor();
        Subject subject = assignment.getSubject();
        
        if (professor == null || subject == null) {
            // Si no hay profesor o materia, no hay conflicto
            return false;
        }
        
        boolean hasAuthorization = professor.hasSubject(subject.getCode());
        boolean hasConflict = !hasAuthorization;
        
        if (hasConflict && logger.isDebugEnabled()) {
            logger.debug("Assignment {} has professor-subject mismatch: Professor {} is not authorized to teach {}",
                       assignment.getId(), professor.getName(), subject.getCode());
        }
        
        return hasConflict;
    }
    
    /**
     * Verifica si el profesor está autorizado para impartir la materia y lanza una excepción si no lo está.
     * 
     * @param assignment Asignación a verificar
     * @throws NullPointerException si assignment es null
     * @throws AssignmentConflictException si el profesor no está autorizado para impartir la materia
     */
    public void verifyProfessorSubjectAuthorization(Assignment assignment) {
        Objects.requireNonNull(assignment, "La asignación no puede ser null");
        
        Professor professor = assignment.getProfessor();
        Subject subject = assignment.getSubject();
        
        if (professor == null || subject == null) {
            return;
        }
        
        if (!professor.hasSubject(subject.getCode())) {
            throw new AssignmentConflictException(
                String.format("El profesor %s (id=%d) no está autorizado para impartir la materia %s",
                        professor.getName(), professor.getId(), subject.getCode()),
                assignment.getId(),
                assignment.getId(), // Auto-conflicto
                "PROFESSOR_SUBJECT_MISMATCH");
        }
    }
    
    /**
     * Verifica si el aula tiene capacidad suficiente para los estudiantes matriculados.
     * 
     * @param assignment Asignación a verificar
     * @return true si hay conflicto (capacidad insuficiente), false en caso contrario
     * @throws NullPointerException si assignment es null
     */
    public boolean checkRoomCapacityConflict(Assignment assignment) {
        Objects.requireNonNull(assignment, "La asignación no puede ser null");
        
        boolean hasCapacity = assignment.hasRoomCapacity();
        boolean hasConflict = !hasCapacity;
        
        if (hasConflict && logger.isDebugEnabled()) {
            logger.debug("Assignment {} has room capacity conflict: required={}, available={}",
                       assignment.getId(), assignment.getEnrolledStudents(), 
                       assignment.getRoom().getCapacity());
        }
        
        return hasConflict;
    }
    
    /**
     * Verifica si el aula es compatible con los requisitos de la materia.
     * 
     * @param assignment Asignación a verificar
     * @return true si hay conflicto (aula incompatible), false en caso contrario
     * @throws NullPointerException si assignment es null
     */
    public boolean checkRoomCompatibilityConflict(Assignment assignment) {
        Objects.requireNonNull(assignment, "La asignación no puede ser null");
        
        boolean isCompatible = assignment.hasRoomCompatibility();
        boolean hasConflict = !isCompatible;
        
        if (hasConflict && logger.isDebugEnabled()) {
            logger.debug("Assignment {} has room compatibility conflict: room={}, requires lab={}",
                       assignment.getId(), assignment.getRoom().getName(),
                       assignment.getSubject() != null && assignment.getSubject().requiresLab());
        }
        
        return hasConflict;
    }
    
    /**
     * Obtiene todas las franjas bloqueadas que tienen conflicto con la asignación.
     * 
     * @param assignment Asignación a verificar
     * @return Lista de franjas bloqueadas en conflicto (nunca null, puede estar vacía)
     * @throws NullPointerException si assignment es null
     */
    public List<BlockedSlot> getConflictingBlockedSlots(Assignment assignment) {
        Objects.requireNonNull(assignment, "La asignación no puede ser null");
        
        Professor professor = assignment.getProfessor();
        if (professor == null) {
            logger.warn("La asignación id={} no tiene profesor asignado", assignment.getId());
            return Collections.emptyList();
        }
        
        List<BlockedSlot> blockedSlots = professor.getBlockedSlots();
        if (blockedSlots.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Datos de la asignación para comparar
        String day = assignment.getDay();
        LocalTime startTime = assignment.getStartTime();
        LocalTime endTime = assignment.getEndTime();
        
        List<BlockedSlot> conflictingSlots = new ArrayList<>();
        
        for (BlockedSlot slot : blockedSlots) {
            if (slot.getDay().equals(day) && 
                timeOverlaps(startTime, endTime, slot.getStartTime(), slot.getEndTime())) {
                
                conflictingSlots.add(slot);
                
                if (logger.isTraceEnabled()) {
                    logger.trace("Found conflicting blocked slot for assignment {}: {} ({} - {})", 
                               assignment.getId(), slot.getDay(), 
                               slot.getStartTime(), slot.getEndTime());
                }
            }
        }
        
        if (logger.isDebugEnabled() && !conflictingSlots.isEmpty()) {
            logger.debug("Found {} conflicting blocked slots for assignment {}", 
                       conflictingSlots.size(), assignment.getId());
        }
        
        return conflictingSlots;
    }
    
    /**
     * Verifica si hay solapamiento temporal entre dos asignaciones.
     * 
     * @param a1 Primera asignación
     * @param a2 Segunda asignación
     * @return true si hay solapamiento temporal, false en caso contrario
     * @throws NullPointerException si alguna asignación es null
     */
    public boolean timeOverlaps(Assignment a1, Assignment a2) {
        Objects.requireNonNull(a1, "La primera asignación no puede ser null");
        Objects.requireNonNull(a2, "La segunda asignación no puede ser null");
        
        // Si son diferentes días, no hay solapamiento
        if (!a1.getDay().equals(a2.getDay())) {
            return false;
        }
        
        // Usar la caché para mejorar rendimiento
        CacheKey key = new CacheKey(
            a1.getId(), a2.getId(), 
            a1.getDay(), a2.getDay(),
            a1.getStartTime(), a1.getEndTime(),
            a2.getStartTime(), a2.getEndTime()
        );
        
        // Intentar obtener resultado de caché
        Boolean cachedResult = overlapCache.get(key);
        if (cachedResult != null) {
            return cachedResult;
        }
        
        // Calcular resultado
        boolean result = timeOverlaps(
            a1.getStartTime(), a1.getEndTime(), 
            a2.getStartTime(), a2.getEndTime()
        );
        
        // Guardar en caché
        overlapCache.put(key, result);
        
        if (result && logger.isTraceEnabled()) {
            logger.trace("Time overlap detected between assignments {} ({}-{}) and {} ({}-{})",
                       a1.getId(), a1.getStartTime(), a1.getEndTime(),
                       a2.getId(), a2.getStartTime(), a2.getEndTime());
        }
        
        return result;
    }
    
    /**
     * Verifica si hay solapamiento entre dos rangos de tiempo.
     * 
     * @param start1 Hora de inicio del primer rango
     * @param end1 Hora de fin del primer rango
     * @param start2 Hora de inicio del segundo rango
     * @param end2 Hora de fin del segundo rango
     * @return true si hay solapamiento, false en caso contrario
     * @throws NullPointerException si alguno de los parámetros es null
     * @throws DomainException si algún rango tiene end antes que start
     */
    public boolean timeOverlaps(LocalTime start1, LocalTime end1, 
                              LocalTime start2, LocalTime end2) {
        
        // Validar parámetros
        Objects.requireNonNull(start1, "La hora de inicio del primer rango no puede ser null");
        Objects.requireNonNull(end1, "La hora de fin del primer rango no puede ser null");
        Objects.requireNonNull(start2, "La hora de inicio del segundo rango no puede ser null");
        Objects.requireNonNull(end2, "La hora de fin del segundo rango no puede ser null");
        
        // Validar que los rangos sean coherentes
        if (!end1.isAfter(start1)) {
            throw new DomainException(
                "La hora de fin del primer rango debe ser posterior a la hora de inicio");
        }
        
        if (!end2.isAfter(start2)) {
            throw new DomainException(
                "La hora de fin del segundo rango debe ser posterior a la hora de inicio");
        }
        
        // Si hay tolerancia de solapamiento, ajustar los tiempos
        if (overlapToleranceMinutes > 0) {
            return timeOverlapsWithTolerance(start1, end1, start2, end2, overlapToleranceMinutes);
        }
        
        // Verificar solapamiento: no hay solapamiento solo si uno termina antes de que el otro empiece
        return !end1.isBefore(start2) && !start1.isAfter(end2);
    }
    
    /**
     * Verifica solapamiento con un margen de tolerancia.
     */
    private boolean timeOverlapsWithTolerance(
            LocalTime start1, LocalTime end1, 
            LocalTime start2, LocalTime end2,
            int toleranceMinutes) {
        
        // Ajustar los tiempos según la tolerancia
        LocalTime adjustedEnd1 = end1.minusMinutes(toleranceMinutes);
        LocalTime adjustedStart1 = start1.plusMinutes(toleranceMinutes);
        LocalTime adjustedEnd2 = end2.minusMinutes(toleranceMinutes);
        LocalTime adjustedStart2 = start2.plusMinutes(toleranceMinutes);
        
        // Si después de ajustar, algún rango se invierte, no hay solapamiento
        if (!adjustedEnd1.isAfter(adjustedStart1) || !adjustedEnd2.isAfter(adjustedStart2)) {
            return false;
        }
        
        // Verificar solapamiento con los tiempos ajustados
        return !adjustedEnd1.isBefore(adjustedStart2) && !adjustedStart1.isAfter(adjustedEnd2);
    }
    
    /**
     * Calcula el tiempo de solapamiento entre dos rangos temporales, en minutos.
     * Devuelve 0 si no hay solapamiento.
     * 
     * @param start1 Hora de inicio del primer rango
     * @param end1 Hora de fin del primer rango
     * @param start2 Hora de inicio del segundo rango
     * @param end2 Hora de fin del segundo rango
     * @return Minutos de solapamiento, o 0 si no hay solapamiento
     * @throws NullPointerException si alguno de los parámetros es null
     * @throws DomainException si algún rango tiene end antes que start
     */
    public int getOverlapMinutes(LocalTime start1, LocalTime end1, 
                                LocalTime start2, LocalTime end2) {
        
        // Reutilizamos la validación existente
        if (!timeOverlaps(start1, end1, start2, end2)) {
            return 0;
        }
        
        // Calcular el rango de solapamiento
        LocalTime overlapStart = start1.isAfter(start2) ? start1 : start2;
        LocalTime overlapEnd = end1.isBefore(end2) ? end1 : end2;
        
        // Calcular la diferencia en minutos
        int minutes = (int) Duration.between(overlapStart, overlapEnd).toMinutes();
        
        if (logger.isTraceEnabled()) {
            logger.trace("Overlap between {}:{} and {}:{} is {} minutes",
                       start1, end1, start2, end2, minutes);
        }
        
        return minutes;
    }
    
    /**
     * Calcula el porcentaje de solapamiento de un rango sobre otro.
     * 
     * @param rangeStart Hora de inicio del rango base
     * @param rangeEnd Hora de fin del rango base
     * @param overlapStart Hora de inicio del potencial solapamiento
     * @param overlapEnd Hora de fin del potencial solapamiento
     * @return Porcentaje de solapamiento (0-100), o 0 si no hay solapamiento
     * @throws NullPointerException si alguno de los parámetros es null
     * @throws DomainException si algún rango tiene end antes que start
     */
    public double getOverlapPercentage(LocalTime rangeStart, LocalTime rangeEnd, 
                                     LocalTime overlapStart, LocalTime overlapEnd) {
        
        // Obtener los minutos de solapamiento
        int overlapMinutes = getOverlapMinutes(rangeStart, rangeEnd, overlapStart, overlapEnd);
        if (overlapMinutes == 0) {
            return 0.0;
        }
        
        // Calcular la duración total del rango base en minutos
        long rangeDurationMinutes = Duration.between(rangeStart, rangeEnd).toMinutes();
        
        if (rangeDurationMinutes == 0) {
            return 0.0; // Evitar división por cero
        }
        
        // Calcular y devolver el porcentaje
        double percentage = (overlapMinutes * 100.0) / rangeDurationMinutes;
        
        if (logger.isTraceEnabled()) {
            logger.trace("Overlap percentage for range {}:{} with {}:{} is {:.2f}%",
                       rangeStart, rangeEnd, overlapStart, overlapEnd, percentage);
        }
        
        return percentage;
    }
    
    /**
     * Verifica si dos asignaciones son compatibles (no hay conflictos).
     * 
     * @param a1 Primera asignación
     * @param a2 Segunda asignación
     * @return true si son compatibles, false si hay algún conflicto
     * @throws NullPointerException si alguna asignación es null
     */
    public boolean areCompatible(Assignment a1, Assignment a2) {
        Objects.requireNonNull(a1, "La primera asignación no puede ser null");
        Objects.requireNonNull(a2, "La segunda asignación no puede ser null");
        
        // Si son el mismo día y hay solapamiento temporal
        if (a1.getDay().equals(a2.getDay()) && timeOverlaps(a1, a2)) {
            // Verificar conflictos específicos
            if (a1.getProfessorId() == a2.getProfessorId()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Incompatible: mismo profesor ({}) en horarios solapados", 
                               a1.getProfessorId());
                }
                return false;
            }
            
            if (a1.getRoomId() == a2.getRoomId()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Incompatible: misma aula ({}) en horarios solapados", 
                               a1.getRoomId());
                }
                return false;
            }
            
            if (a1.getGroupId() == a2.getGroupId()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Incompatible: mismo grupo ({}) en horarios solapados", 
                               a1.getGroupId());
                }
                return false;
            }
        }
        
        // Si llegamos aquí, son compatibles
        return true;
    }
    
    /**
     * Verifica que dos asignaciones sean compatibles y lanza una excepción si no lo son.
     * 
     * @param a1 Primera asignación
     * @param a2 Segunda asignación
     * @throws NullPointerException si alguna asignación es null
     * @throws AssignmentConflictException si las asignaciones tienen algún conflicto
     */
    public void verifyCompatible(Assignment a1, Assignment a2) {
        Objects.requireNonNull(a1, "La primera asignación no puede ser null");
        Objects.requireNonNull(a2, "La segunda asignación no puede ser null");
        
        // Si no son el mismo día o no hay solapamiento temporal, son compatibles
        if (!a1.getDay().equals(a2.getDay()) || !timeOverlaps(a1, a2)) {
            return;
        }
        
        // Verificar conflictos específicos
        if (a1.getProfessorId() == a2.getProfessorId()) {
            throw new AssignmentConflictException(
                String.format("Conflicto de profesor: las asignaciones id=%d e id=%d tienen el mismo profesor (id=%d) en horarios solapados",
                        a1.getId(), a2.getId(), a1.getProfessorId()),
                a1.getId(),
                a2.getId(),
                "PROFESSOR");
        }
        
        if (a1.getRoomId() == a2.getRoomId()) {
            throw new AssignmentConflictException(
                String.format("Conflicto de aula: las asignaciones id=%d e id=%d tienen la misma aula (id=%d) en horarios solapados",
                        a1.getId(), a2.getId(), a1.getRoomId()),
                a1.getId(),
                a2.getId(),
                "ROOM");
        }
        
        if (a1.getGroupId() == a2.getGroupId()) {
            throw new AssignmentConflictException(
                String.format("Conflicto de grupo: las asignaciones id=%d e id=%d tienen el mismo grupo (id=%d) en horarios solapados",
                        a1.getId(), a2.getId(), a1.getGroupId()),
                a1.getId(),
                a2.getId(),
                "GROUP");
        }
    }
    
    /**
     * Limpia la caché de resultados.
     */
    public void clearCache() {
        int size = overlapCache.size();
        overlapCache.clear();
        logger.info("Caché limpiada: {} entradas eliminadas", size);
    }
    
    /**
     * Clase para las claves de caché.
     */
    private static class CacheKey {
        private final int id1;
        private final int id2;
        private final String day1;
        private final String day2;
        private final LocalTime start1;
        private final LocalTime end1;
        private final LocalTime start2;
        private final LocalTime end2;
        
        public CacheKey(int id1, int id2, String day1, String day2,
                      LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
            this.id1 = id1;
            this.id2 = id2;
            this.day1 = day1;
            this.day2 = day2;
            this.start1 = start1;
            this.end1 = end1;
            this.start2 = start2;
            this.end2 = end2;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CacheKey cacheKey = (CacheKey) o;
            return id1 == cacheKey.id1 &&
                   id2 == cacheKey.id2 &&
                   Objects.equals(day1, cacheKey.day1) &&
                   Objects.equals(day2, cacheKey.day2) &&
                   Objects.equals(start1, cacheKey.start1) &&
                   Objects.equals(end1, cacheKey.end1) &&
                   Objects.equals(start2, cacheKey.start2) &&
                   Objects.equals(end2, cacheKey.end2);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(id1, id2, day1, day2, start1, end1, start2, end2);
        }
    }
}