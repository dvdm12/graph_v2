/**
 * Clase que representa un conflicto entre nodos académicos
 */
class Conflict {
    /**
     * Constructor de Conflict
     * @param {Object} sourceNode - Nodo origen
     * @param {Object} targetNode - Nodo destino
     * @param {Array} types - Tipos de conflicto
     * @param {boolean} isSelfLoop - Si es un auto-conflicto (opcional)
     */
    constructor(sourceNode, targetNode, types = [], isSelfLoop = null) {
        this.source = sourceNode;
        this.target = targetNode;
        this.types = Array.isArray(types) ? [...types] : [];
        this.count = this.types.length;
        
        // Determinar si es un auto-conflicto
        this.isSelfLoop = isSelfLoop !== null ? 
            isSelfLoop : 
            (sourceNode && targetNode && sourceNode.id === targetNode.id);
    }
    
    /**
     * Añade un nuevo tipo de conflicto si no existe ya
     * @param {string} type - Tipo de conflicto a añadir
     * @returns {Conflict} This para encadenamiento
     */
    addType(type) {
        if (type && !this.types.includes(type)) {
            this.types.push(type);
            this.count = this.types.length;
        }
        return this;
    }
    
    /**
     * Añade múltiples tipos de conflicto de una vez
     * @param {Array} newTypes - Array de tipos de conflicto a añadir
     * @returns {Conflict} This para encadenamiento
     */
    addTypes(newTypes) {
        if (Array.isArray(newTypes)) {
            newTypes.forEach(type => {
                if (type && !this.types.includes(type)) {
                    this.types.push(type);
                }
            });
            this.count = this.types.length;
        }
        return this;
    }
    
    /**
     * Verifica si este conflicto es del tipo especificado
     * @param {string} typePattern - Patrón a buscar en los tipos
     * @returns {boolean} true si coincide con algún tipo
     */
    isOfType(typePattern) {
        if (!typePattern) return false;
        
        return this.types.some(type => 
            type.toLowerCase().includes(typePattern.toLowerCase())
        );
    }
    
    /**
     * Obtiene el tipo de conflicto principal según prioridad
     * @returns {string} Tipo principal de conflicto
     */
    getPrimaryType() {
        const priorityOrder = [
            "franja bloqueada",
            "mismo profesor",
            "misma sala",
            "mismo grupo",
            "jornada",
            "horario"
        ];
        
        // Buscar el primer tipo que coincida con las prioridades
        for (const priority of priorityOrder) {
            const matchingType = this.types.find(type => 
                type.toLowerCase().includes(priority.toLowerCase())
            );
            
            if (matchingType) {
                return matchingType;
            }
        }
        
        // Si no hay coincidencias, devolver el primer tipo o valor por defecto
        return this.types[0] || "Conflicto desconocido";
    }
    
    /**
     * Obtiene el color asociado al tipo de conflicto principal
     * @returns {string} Código de color en formato hexadecimal
     */
    getColor() {
        const primaryType = this.getPrimaryType().toLowerCase();
        
        if (primaryType.includes("franja bloqueada"))
            return "#9c27b0"; // Púrpura
        if (primaryType.includes("mismo profesor"))
            return "#e53935"; // Rojo
        if (primaryType.includes("misma sala"))
            return "#ffb300"; // Ámbar
        if (primaryType.includes("mismo grupo"))
            return "#43a047"; // Verde
        if (primaryType.includes("jornada") || primaryType.includes("horario"))
            return "#26a69a"; // Verde azulado
        
        return "#9e9e9e"; // Gris por defecto
    }
    
    /**
     * Obtiene una descripción breve del conflicto
     * @returns {string} Descripción del conflicto
     */
    getDescription() {
        if (this.isSelfLoop) {
            return `Auto-conflicto: ${this.getPrimaryType()}`;
        }
        
        const sourceName = this.source ? (this.source.groupName || this.source.id) : "?";
        const targetName = this.target ? (this.target.groupName || this.target.id) : "?";
        
        return `Conflicto entre ${sourceName} y ${targetName}: ${this.getPrimaryType()}`;
    }
    
    /**
     * Obtiene información detallada sobre el conflicto
     * @returns {Object} Objeto con información detallada
     */
    getDetailedInfo() {
        // Información básica
        const info = {
            primaryType: this.getPrimaryType(),
            color: this.getColor(),
            count: this.count,
            types: [...this.types],
            isSelfLoop: this.isSelfLoop
        };
        
        // Añadir información de los nodos
        if (this.source) {
            info.source = {
                id: this.source.id,
                name: this.source.groupName || "",
                professor: this.source.professorName || "",
                room: this.source.roomName || "",
                schedule: `${this.source.day || ""} ${this.source.startTime || ""}-${this.source.endTime || ""}`
            };
        }
        
        if (this.target && !this.isSelfLoop) {
            info.target = {
                id: this.target.id,
                name: this.target.groupName || "",
                professor: this.target.professorName || "",
                room: this.target.roomName || "",
                schedule: `${this.target.day || ""} ${this.target.startTime || ""}-${this.target.endTime || ""}`
            };
        }
        
        // Añadir información sobre franjas bloqueadas para auto-conflictos
        if (this.isSelfLoop && this.source && this.source.blockedSlots) {
            info.blockedSlots = this.source.blockedSlots
                .filter(slot => slot.day === this.source.day)
                .map(slot => ({
                    day: slot.day,
                    time: `${slot.startTime}-${slot.endTime}`
                }));
        }
        
        return info;
    }
    
    /**
     * Convierte el conflicto a un formato compatible con D3.js
     * @returns {Object} Objeto de enlace para D3.js
     */
    toD3Link() {
        return {
            source: this.source,
            target: this.target,
            types: this.types,
            count: this.count,
            isSelfLoop: this.isSelfLoop,
            color: this.getColor(),
            primaryType: this.getPrimaryType()
        };
    }
    
    /**
     * Verifica si este conflicto es idéntico a otro
     * @param {Conflict} otherConflict - Conflicto a comparar
     * @returns {boolean} true si los conflictos son idénticos
     */
    equals(otherConflict) {
        if (!otherConflict) return false;
        
        // Verificar si los nodos son los mismos (en cualquier orden para conflictos no cíclicos)
        const sameNodes = this.isSelfLoop ? 
            (this.source.id === otherConflict.source.id && otherConflict.isSelfLoop) : 
            ((this.source.id === otherConflict.source.id && this.target.id === otherConflict.target.id) || 
             (this.source.id === otherConflict.target.id && this.target.id === otherConflict.source.id));
        
        if (!sameNodes) return false;
        
        // Verificar si tienen los mismos tipos
        if (this.count !== otherConflict.count) return false;
        
        return this.types.every(type => otherConflict.types.includes(type));
    }
    
    /**
     * Crea una copia independiente de este conflicto
     * @returns {Conflict} Copia del conflicto
     */
    clone() {
        return new Conflict(
            this.source,
            this.target,
            [...this.types],
            this.isSelfLoop
        );
    }
    
    /**
     * Crea un conflicto a partir de dos nodos
     * @param {Object} node1 - Primer nodo
     * @param {Object} node2 - Segundo nodo
     * @returns {Conflict|null} Conflicto creado o null si no hay conflicto
     * @static
     */
    static fromNodes(node1, node2) {
        if (!node1 || !node2) return null;
        
        // Detectar tipos de conflicto
        let types = [];
        
        // Auto-conflicto (mismo nodo)
        if (node1.id === node2.id) {
            // Verificar conflictos con franjas bloqueadas
            if (node1.blockedSlots && node1.blockedSlots.length > 0) {
                const blockedSlots = node1.blockedSlots.filter(slot => 
                    slot.day === node1.day
                );
                
                for (const slot of blockedSlots) {
                    const slotStart = Conflict.timeToMinutes(slot.startTime);
                    const slotEnd = Conflict.timeToMinutes(slot.endTime);
                    const classStart = Conflict.timeToMinutes(node1.startTime);
                    const classEnd = Conflict.timeToMinutes(node1.endTime);
                    
                    if (slotStart < classEnd && slotEnd > classStart) {
                        types.push("Conflicto con franja bloqueada del profesor");
                        break;
                    }
                }
            }
            
            // Si no hay conflictos, retornar null
            if (types.length === 0) return null;
            
            return new Conflict(node1, node2, types, true);
        }
        
        // Conflicto entre nodos diferentes
        // Verificar solapamiento de horarios
        if (node1.day !== node2.day) return null;
        
        const node1Start = Conflict.timeToMinutes(node1.startTime);
        const node1End = Conflict.timeToMinutes(node1.endTime);
        const node2Start = Conflict.timeToMinutes(node2.startTime);
        const node2End = Conflict.timeToMinutes(node2.endTime);
        
        if (node1Start >= node2End || node1End <= node2Start) return null;
        
        // Detectar tipos de conflicto
        if (node1.professorId && node2.professorId && 
            node1.professorId === node2.professorId) {
            types.push("Solapamiento de horarios (mismo profesor)");
        }
        
        if (node1.roomId && node2.roomId && 
            node1.roomId === node2.roomId) {
            types.push("Solapamiento de horarios (misma sala)");
        }
        
        if (node1.groupId && node2.groupId && 
            node1.groupId === node2.groupId) {
            types.push("Solapamiento de horarios (mismo grupo)");
        }
        
        // Si hay solapamiento pero no detectamos conflictos específicos,
        // añadir conflicto general de jornada
        if (types.length === 0) {
            types.push("Misma jornada y horario");
        }
        
        return new Conflict(node1, node2, types, false);
    }
    
    /**
     * Convierte hora en formato HH:MM a minutos totales
     * @param {string} time - Hora en formato HH:MM
     * @returns {number} Minutos totales
     * @static
     */
    static timeToMinutes(time) {
        if (!time) return 0;
        
        const parts = time.split(':');
        if (parts.length < 2) return 0;
        
        const hours = parseInt(parts[0], 10) || 0;
        const minutes = parseInt(parts[1], 10) || 0;
        
        return hours * 60 + minutes;
    }
}

// Si estamos utilizando módulos ES
export default Conflict;