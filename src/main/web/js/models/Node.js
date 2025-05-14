/**
 * Clase que representa un nodo académico (clase/asignatura)
 */
class Node {
    /**
     * Constructor de Node
     * @param {Object} nodeData - Datos del nodo
     */
    constructor(nodeData = {}) {
        // Datos de identificación
        this.id = String(nodeData.id || '');
        this.assignmentDate = nodeData.assignmentDate || '';
        
        // Datos temporales
        this.day = nodeData.day || '';
        this.startTime = nodeData.startTime || '';
        this.endTime = nodeData.endTime || '';
        
        // Datos del profesor
        this.professorId = nodeData.professorId || 0;
        this.professorName = nodeData.professorName || '';
        
        // Datos del aula
        this.roomId = nodeData.roomId || 0;
        this.roomName = nodeData.roomName || '';
        
        // Datos del grupo
        this.groupId = nodeData.groupId || 0;
        this.groupName = nodeData.groupName || '';
        
        // Características de la sesión
        this.sessionType = nodeData.sessionType || '';
        this.requiresLab = Boolean(nodeData.requiresLab);
        this.enrolledStudents = Number(nodeData.enrolledStudents) || 0;
        
        // Franjas bloqueadas
        this.blockedSlots = Array.isArray(nodeData.blockedSlots) ? 
            [...nodeData.blockedSlots] : [];
        
        // Propiedades para la visualización en D3.js
        this.x = 0;            // Posición X actual
        this.y = 0;            // Posición Y actual
        this.vx = 0;           // Velocidad X (para simulación)
        this.vy = 0;           // Velocidad Y (para simulación)
        this.fx = null;        // Posición X fija (para arrastre)
        this.fy = null;        // Posición Y fija (para arrastre)
        this.selected = false; // Estado de selección
    }
    
    /**
     * Comprueba si la clase actual tiene conflicto con sus franjas bloqueadas
     * @returns {Array} Lista de conflictos con franjas bloqueadas
     */
    getBlockedSlotConflicts() {
        const conflicts = [];
        
        // Verificar si hay franjas bloqueadas para este día
        const slotsForToday = this.blockedSlots.filter(slot => 
            slot.day === this.day
        );
        
        if (slotsForToday.length === 0) {
            return conflicts;
        }
        
        // Comprobar solapamiento con cada franja bloqueada
        slotsForToday.forEach(slot => {
            const slotStart = this.timeToMinutes(slot.startTime);
            const slotEnd = this.timeToMinutes(slot.endTime);
            const classStart = this.timeToMinutes(this.startTime);
            const classEnd = this.timeToMinutes(this.endTime);
            
            // Verificar si hay solapamiento
            if (slotStart < classEnd && slotEnd > classStart) {
                conflicts.push({
                    type: "Conflicto con franja bloqueada del profesor",
                    day: slot.day,
                    blockedTime: `${slot.startTime}-${slot.endTime}`,
                    classTime: `${this.startTime}-${this.endTime}`
                });
            }
        });
        
        return conflicts;
    }
    
    /**
     * Verifica si este nodo tiene conflicto con otro nodo
     * @param {Node} otherNode - Otro nodo para verificar
     * @returns {Array} Tipos de conflictos detectados
     */
    detectConflictsWith(otherNode) {
        const conflicts = [];
        
        // Verificar si es el mismo nodo (auto-conflicto)
        if (this.id === otherNode.id) {
            // Verificar conflictos con franjas bloqueadas
            const blockedConflicts = this.getBlockedSlotConflicts();
            if (blockedConflicts.length > 0) {
                conflicts.push('Conflicto con franja bloqueada del profesor');
            }
            return conflicts;
        }
        
        // Verificar solapamiento de horarios
        if (this.day !== otherNode.day || !this.hasTimeOverlap(otherNode)) {
            return conflicts; // Sin solapamiento, no hay conflicto
        }
        
        // Verificar si es el mismo profesor
        if (this.professorId && otherNode.professorId && 
            this.professorId === otherNode.professorId) {
            conflicts.push('Solapamiento de horarios (mismo profesor)');
        }
        
        // Verificar si es la misma sala
        if (this.roomId && otherNode.roomId && 
            this.roomId === otherNode.roomId) {
            conflicts.push('Solapamiento de horarios (misma sala)');
        }
        
        // Verificar si es el mismo grupo
        if (this.groupId && otherNode.groupId && 
            this.groupId === otherNode.groupId) {
            conflicts.push('Solapamiento de horarios (mismo grupo)');
        }
        
        // Si hay solapamiento pero no detectamos conflictos específicos,
        // añadir conflicto general de jornada
        if (conflicts.length === 0) {
            conflicts.push('Misma jornada y horario');
        }
        
        return conflicts;
    }
    
    /**
     * Verifica si hay solapamiento de horarios con otro nodo
     * @param {Node} otherNode - Otro nodo para verificar
     * @returns {boolean} true si hay solapamiento
     */
    hasTimeOverlap(otherNode) {
        if (!otherNode || !this.startTime || !this.endTime || 
            !otherNode.startTime || !otherNode.endTime) {
            return false;
        }
        
        // Convertir horas a minutos para comparación
        const thisStart = this.timeToMinutes(this.startTime);
        const thisEnd = this.timeToMinutes(this.endTime);
        const otherStart = this.timeToMinutes(otherNode.startTime);
        const otherEnd = this.timeToMinutes(otherNode.endTime);
        
        // Verificar solapamiento (los intervalos se solapan si el inicio de uno 
        // es anterior al fin del otro y el fin de uno es posterior al inicio del otro)
        return (thisStart < otherEnd && thisEnd > otherStart);
    }
    
    /**
     * Convierte hora en formato HH:MM a minutos totales
     * @param {string} time - Hora en formato HH:MM
     * @returns {number} Minutos totales
     */
    timeToMinutes(time) {
        if (!time) return 0;
        
        const parts = time.split(':');
        if (parts.length < 2) return 0;
        
        const hours = parseInt(parts[0], 10) || 0;
        const minutes = parseInt(parts[1], 10) || 0;
        
        return hours * 60 + minutes;
    }
    
    /**
     * Obtiene la duración de la clase en minutos
     * @returns {number} Duración en minutos
     */
    getDuration() {
        const startMinutes = this.timeToMinutes(this.startTime);
        const endMinutes = this.timeToMinutes(this.endTime);
        return Math.max(0, endMinutes - startMinutes);
    }
    
    /**
     * Comprueba si esta clase requiere laboratorio
     * @returns {boolean} true si requiere laboratorio
     */
    needsLab() {
        return this.requiresLab || this.sessionType === 'L';
    }
    
    /**
     * Obtiene un objeto con información resumida del nodo
     * @returns {Object} Información resumida
     */
    getSummary() {
        return {
            id: this.id,
            name: this.groupName,
            professor: this.professorName,
            schedule: `${this.day} ${this.startTime}-${this.endTime}`
        };
    }
    
    /**
     * Obtiene un objeto con información detallada del nodo
     * @returns {Object} Información detallada
     */
    getDetailedInfo() {
        return {
            id: this.id,
            title: this.groupName,
            professor: this.professorName,
            room: this.roomName,
            schedule: `${this.day} ${this.startTime}-${this.endTime}`,
            duration: `${this.getDuration()} min`,
            students: this.enrolledStudents,
            sessionType: this.formatSessionType(),
            requiresLab: this.requiresLab ? "Sí" : "No",
            blockedSlots: this.blockedSlots.length
        };
    }
    
    /**
     * Obtiene una representación de texto del nodo
     * @returns {string} Descripción del nodo
     */
    toString() {
        return `${this.groupName || ""} (${this.professorName || ""})`;
    }
    
    /**
     * Formatea el tipo de sesión a un formato más legible
     * @returns {string} Tipo de sesión formateado
     */
    formatSessionType() {
        const typeMap = {
            'D': 'Docencia',
            'T': 'Teoría',
            'P': 'Práctica',
            'L': 'Laboratorio',
            'S': 'Seminario',
            'E': 'Evaluación',
            'O': 'Otra'
        };
        
        return typeMap[this.sessionType] || this.sessionType || "N/A";
    }
    
    /**
     * Crea una copia independiente de este nodo
     * @returns {Node} Copia del nodo
     */
    clone() {
        return new Node({
            id: this.id,
            assignmentDate: this.assignmentDate,
            day: this.day,
            startTime: this.startTime,
            endTime: this.endTime,
            professorId: this.professorId,
            professorName: this.professorName,
            roomId: this.roomId,
            roomName: this.roomName,
            groupId: this.groupId,
            groupName: this.groupName,
            sessionType: this.sessionType,
            requiresLab: this.requiresLab,
            enrolledStudents: this.enrolledStudents,
            blockedSlots: this.blockedSlots.map(slot => ({...slot}))
        });
    }
}

// Si estamos utilizando módulos ES
export default Node;