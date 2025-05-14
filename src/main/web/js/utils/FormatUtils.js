/**
 * Clase de utilidades para formateo de texto y datos
 */
class FormatUtils {
    /**
     * Trunca un texto si excede la longitud máxima
     * @param {string} text - Texto a truncar
     * @param {number} maxLength - Longitud máxima
     * @returns {string} Texto truncado con elipsis si es necesario
     */
    static truncateText(text, maxLength) {
        if (!text) return "";
        return text.length > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
    
    /**
     * Formatea una hora en formato 24h a formato más legible
     * @param {string} time - Hora en formato HH:MM
     * @returns {string} Hora formateada
     */
    static formatTime(time) {
        if (!time) return "";
        
        // Si ya tiene formato correcto, retornarlo
        if (/^\d{1,2}:\d{2}$/.test(time)) {
            return time;
        }
        
        // Intentar parsear el tiempo
        try {
            const [hours, minutes] = time.split(':').map(Number);
            return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}`;
        } catch (e) {
            return time;
        }
    }
    
    /**
     * Formatea un intervalo de tiempo
     * @param {string} startTime - Hora de inicio
     * @param {string} endTime - Hora de fin
     * @returns {string} Intervalo formateado
     */
    static formatTimeRange(startTime, endTime) {
        const start = this.formatTime(startTime);
        const end = this.formatTime(endTime);
        
        if (!start && !end) return "";
        if (!start) return end;
        if (!end) return start;
        
        return `${start} - ${end}`;
    }
    
    /**
     * Formatea el día de la semana
     * @param {string} day - Día en cualquier formato
     * @returns {string} Día formateado
     */
    static formatDay(day) {
        if (!day) return "";
        
        // Normalizar día a minúsculas
        const normalizedDay = day.toLowerCase();
        
        // Mapeo de días en diferentes formatos
        const dayMap = {
            'lu': 'Lunes',
            'lun': 'Lunes',
            'lunes': 'Lunes',
            'monday': 'Lunes',
            'mon': 'Lunes',
            
            'ma': 'Martes',
            'mar': 'Martes',
            'martes': 'Martes',
            'tuesday': 'Martes',
            'tue': 'Martes',
            
            'mi': 'Miércoles',
            'mie': 'Miércoles',
            'miercoles': 'Miércoles',
            'miércoles': 'Miércoles',
            'wednesday': 'Miércoles',
            'wed': 'Miércoles',
            
            'ju': 'Jueves',
            'jue': 'Jueves',
            'jueves': 'Jueves',
            'thursday': 'Jueves',
            'thu': 'Jueves',
            
            'vi': 'Viernes',
            'vie': 'Viernes',
            'viernes': 'Viernes',
            'friday': 'Viernes',
            'fri': 'Viernes',
            
            'sa': 'Sábado',
            'sab': 'Sábado',
            'sabado': 'Sábado',
            'sábado': 'Sábado',
            'saturday': 'Sábado',
            'sat': 'Sábado',
            
            'do': 'Domingo',
            'dom': 'Domingo',
            'domingo': 'Domingo',
            'sunday': 'Domingo',
            'sun': 'Domingo'
        };
        
        return dayMap[normalizedDay] || day;
    }
    
    /**
     * Formatea un tipo de conflicto para mejor legibilidad
     * @param {string} type - Tipo de conflicto
     * @returns {string} Tipo formateado
     */
    static formatConflictType(type) {
        if (!type) return "";
        
        // Capitalizar primera letra
        return type.charAt(0).toUpperCase() + type.slice(1);
    }
    
    /**
     * Formatea una fecha en formato ISO a formato local
     * @param {string} isoDate - Fecha en formato ISO
     * @returns {string} Fecha formateada
     */
    static formatDate(isoDate) {
        if (!isoDate) return "";
        
        try {
            const date = new Date(isoDate);
            return date.toLocaleDateString('es-ES', {
                year: 'numeric',
                month: 'long',
                day: 'numeric'
            });
        } catch (e) {
            return isoDate;
        }
    }
    
    /**
     * Convierte el primer carácter de cada palabra a mayúscula
     * @param {string} text - Texto a convertir
     * @returns {string} Texto con capitalización
     */
    static toTitleCase(text) {
        if (!text) return "";
        return text.replace(
            /\w\S*/g,
            txt => txt.charAt(0).toUpperCase() + txt.substring(1).toLowerCase()
        );
    }
    
    /**
     * Formatea un tipo de sesión a un formato más legible
     * @param {string} sessionType - Tipo de sesión (D, T, P, etc.)
     * @returns {string} Descripción del tipo de sesión
     */
    static formatSessionType(sessionType) {
        if (!sessionType) return "N/A";
        
        const typeMap = {
            'D': 'Docencia',
            'T': 'Teoría',
            'P': 'Práctica',
            'L': 'Laboratorio',
            'S': 'Seminario',
            'E': 'Evaluación',
            'O': 'Otra'
        };
        
        return typeMap[sessionType.toUpperCase()] || sessionType;
    }
}

// Si estamos utilizando módulos ES
export default FormatUtils;