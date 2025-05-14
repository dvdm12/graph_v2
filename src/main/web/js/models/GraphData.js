/**
 * Clase que representa los datos del grafo de conflictos académicos.
 * Encapsula la estructura de nodos (clases) y enlaces (conflictos).
 */
class GraphData {
    /**
     * Constructor de la clase GraphData
     */
    constructor() {
        this.nodes = [];
        this.links = [];
        this.nodeMap = new Map(); // Mapa para acceso rápido a nodos por ID
    }

    /**
     * Carga datos desde un objeto JSON y los convierte al formato interno
     * @param {Object} jsonData - Datos JSON con nodos y aristas
     * @returns {boolean} true si la carga fue exitosa
     * @throws {Error} Si hay un error en el procesamiento de los datos
     */
    loadFromJSON(jsonData) {
        try {
            // Reiniciar datos
            this.clear();
            
            // Validar estructura básica del JSON
            if (!jsonData || typeof jsonData !== 'object') {
                throw new Error('Los datos JSON no son válidos');
            }
            
            // Procesar nodos
            if (!jsonData.nodes || !Array.isArray(jsonData.nodes)) {
                throw new Error('El JSON no contiene un array de nodos válido');
            }
            this.processNodes(jsonData.nodes);
            
            // Procesar aristas/conflictos
            if (!jsonData.edges || !Array.isArray(jsonData.edges)) {
                throw new Error('El JSON no contiene un array de aristas válido');
            }
            this.processEdges(jsonData.edges);
            
            return true;
        } catch (error) {
            console.error("Error al procesar JSON:", error);
            throw new Error(`Error al procesar los datos: ${error.message}`);
        }
    }

    /**
     * Procesa los nodos del JSON y los convierte a formato interno
     * @param {Array} nodes - Array de nodos del JSON
     */
    processNodes(nodes) {
        this.nodes = nodes.map(nodeData => {
            // Asegurar que el nodo tenga un ID
            if (nodeData.id === undefined || nodeData.id === null) {
                throw new Error('Un nodo no tiene ID definido');
            }
            
            // Asegurar que el ID sea string
            const id = String(nodeData.id);
            
            // Crear objeto de nodo con propiedades estandarizadas
            const node = {
                id: id,
                assignmentDate: nodeData.assignmentDate || "",
                day: nodeData.day || "",
                startTime: nodeData.startTime || "",
                endTime: nodeData.endTime || "",
                professorId: nodeData.professorId || 0,
                professorName: nodeData.professorName || "",
                roomId: nodeData.roomId || 0,
                roomName: nodeData.roomName || "",
                groupId: nodeData.groupId || 0,
                groupName: nodeData.groupName || "",
                sessionType: nodeData.sessionType || "",
                requiresLab: Boolean(nodeData.requiresLab),
                enrolledStudents: Number(nodeData.enrolledStudents) || 0,
                blockedSlots: Array.isArray(nodeData.blockedSlots) ? 
                    [...nodeData.blockedSlots] : []
            };
            
            // Añadir al mapa para acceso rápido
            this.nodeMap.set(id, node);
            
            return node;
        });
    }

    /**
     * Procesa las aristas/conflictos del JSON y los convierte a formato interno
     * @param {Array} edges - Array de aristas del JSON
     */
    processEdges(edges) {
        // Mapa para contar y agrupar conflictos entre los mismos nodos
        const conflictCountMap = new Map();
        
        edges.forEach(edge => {
            // Validar estructura básica de la arista
            if (!edge.between || !Array.isArray(edge.between) || edge.between.length < 2) {
                console.warn('Arista con estructura inválida:', edge);
                return;
            }
            
            const sourceId = String(edge.between[0]);
            const targetId = String(edge.between[1]);
            
            // Verificar que los nodos existan
            if (!this.nodeMap.has(sourceId) || !this.nodeMap.has(targetId)) {
                console.warn(`Conflicto con nodos inexistentes: ${sourceId}-${targetId}`);
                return;
            }
            
            // Determinar si es una arista cíclica (auto-bucle)
            const isSelfLoop = sourceId === targetId;
            
            // Crear clave única para este par de nodos
            // Para aristas cíclicas, usar una clave especial para evitar mezclar con otras aristas
            const edgeKey = isSelfLoop ? 
                `self-${sourceId}` : 
                (sourceId <= targetId ? `${sourceId}-${targetId}` : `${targetId}-${sourceId}`);
            
            // Validar array de conflictos
            const conflicts = Array.isArray(edge.conflicts) ? edge.conflicts : [];
            
            // Agrupar conflictos entre los mismos nodos
            if (conflictCountMap.has(edgeKey)) {
                const entry = conflictCountMap.get(edgeKey);
                entry.count += conflicts.length;
                entry.types = [...entry.types, ...conflicts];
            } else {
                conflictCountMap.set(edgeKey, {
                    source: sourceId,
                    target: targetId,
                    count: conflicts.length,
                    types: [...conflicts],
                    isSelfLoop: isSelfLoop
                });
            }
        });
        
        // Convertir el mapa a un array de enlaces
        this.links = Array.from(conflictCountMap.values()).map(link => {
            // Convertir IDs a objetos de nodo completos
            return {
                source: this.nodeMap.get(link.source),
                target: this.nodeMap.get(link.target),
                count: link.count,
                types: link.types,
                isSelfLoop: link.isSelfLoop
            };
        });
    }

    /**
     * Obtiene un nodo por su ID
     * @param {string|number} id - ID del nodo a buscar
     * @returns {Object|null} Nodo encontrado o null
     */
    getNodeById(id) {
        const strId = String(id);
        return this.nodeMap.get(strId) || null;
    }

    /**
     * Obtiene todos los conflictos relacionados con un nodo
     * @param {string|number} nodeId - ID del nodo
     * @returns {Array} Lista de conflictos
     */
    getConflictsForNode(nodeId) {
        const strId = String(nodeId);
        return this.links.filter(link => 
            link.source.id === strId || link.target.id === strId
        );
    }

    /**
     * Obtiene los auto-conflictos de un nodo
     * @param {string|number} nodeId - ID del nodo
     * @returns {Array} Lista de auto-conflictos
     */
    getSelfConflictsForNode(nodeId) {
        const strId = String(nodeId);
        return this.links.filter(link => 
            link.source.id === strId && 
            link.target.id === strId &&
            link.isSelfLoop === true
        );
    }

    /**
     * Comprueba si hay un conflicto entre dos nodos
     * @param {string|number} nodeId1 - ID del primer nodo
     * @param {string|number} nodeId2 - ID del segundo nodo
     * @returns {Object|null} Objeto de conflicto o null si no existe
     */
    getConflictBetween(nodeId1, nodeId2) {
        const id1 = String(nodeId1);
        const id2 = String(nodeId2);
        
        // Si son el mismo nodo, buscar auto-conflictos
        if (id1 === id2) {
            return this.links.find(link => 
                link.source.id === id1 && 
                link.target.id === id1 &&
                link.isSelfLoop === true
            ) || null;
        }
        
        // Buscar conflictos entre dos nodos diferentes
        return this.links.find(link => 
            (link.source.id === id1 && link.target.id === id2) || 
            (link.source.id === id2 && link.target.id === id1)
        ) || null;
    }

    /**
     * Verifica si un nodo tiene conflictos con franjas bloqueadas
     * @param {string|number} nodeId - ID del nodo a verificar
     * @returns {boolean} true si hay conflictos con franjas bloqueadas
     */
    hasBlockedSlotConflicts(nodeId) {
        const node = this.getNodeById(nodeId);
        if (!node || !Array.isArray(node.blockedSlots) || node.blockedSlots.length === 0) {
            return false;
        }
        
        // Verificar si hay franjas bloqueadas para este día
        const slotsForToday = node.blockedSlots.filter(slot => 
            slot.day === node.day
        );
        
        if (slotsForToday.length === 0) {
            return false;
        }
        
        // Comprobar solapamiento con cada franja bloqueada
        return slotsForToday.some(slot => {
            const slotStart = this.timeToMinutes(slot.startTime);
            const slotEnd = this.timeToMinutes(slot.endTime);
            const classStart = this.timeToMinutes(node.startTime);
            const classEnd = this.timeToMinutes(node.endTime);
            
            // Verificar si hay solapamiento
            return (slotStart < classEnd && slotEnd > classStart);
        });
    }
    
    /**
     * Obtiene las franjas bloqueadas que tienen conflicto con un nodo
     * @param {string|number} nodeId - ID del nodo a verificar
     * @returns {Array} Lista de franjas bloqueadas con conflicto
     */
    getConflictingBlockedSlots(nodeId) {
        const node = this.getNodeById(nodeId);
        if (!node || !Array.isArray(node.blockedSlots) || node.blockedSlots.length === 0) {
            return [];
        }
        
        // Obtener las franjas bloqueadas para el día de la clase
        const slotsForToday = node.blockedSlots.filter(slot => 
            slot.day === node.day
        );
        
        if (slotsForToday.length === 0) {
            return [];
        }
        
        // Devolver las franjas que tienen solapamiento
        return slotsForToday.filter(slot => {
            const slotStart = this.timeToMinutes(slot.startTime);
            const slotEnd = this.timeToMinutes(slot.endTime);
            const classStart = this.timeToMinutes(node.startTime);
            const classEnd = this.timeToMinutes(node.endTime);
            
            // Verificar si hay solapamiento
            return (slotStart < classEnd && slotEnd > classStart);
        });
    }
    
    /**
     * Convierte hora en formato HH:MM a minutos totales
     * @param {string} time - Hora en formato HH:MM
     * @returns {number} Minutos totales
     */
    timeToMinutes(time) {
        if (!time) return 0;
        
        const [hours, minutes] = time.split(':').map(Number);
        return (hours || 0) * 60 + (minutes || 0);
    }

    /**
     * Limpia todos los datos del grafo
     */
    clear() {
        this.nodes = [];
        this.links = [];
        this.nodeMap.clear();
    }
    
    /**
     * Obtiene estadísticas básicas sobre el grafo
     * @returns {Object} Objeto con estadísticas
     */
    getStats() {
        const stats = {
            nodeCount: this.nodes.length,
            linkCount: this.links.length,
            conflictCount: this.links.reduce((sum, link) => sum + link.count, 0),
            selfLoopCount: this.links.filter(link => link.isSelfLoop).length,
            professorCount: new Set(this.nodes.map(n => n.professorId).filter(id => id > 0)).size,
            roomCount: new Set(this.nodes.map(n => n.roomId).filter(id => id > 0)).size,
            groupCount: new Set(this.nodes.map(n => n.groupId).filter(id => id > 0)).size,
            blockedSlotsCount: this.nodes.reduce((sum, node) => 
                sum + (Array.isArray(node.blockedSlots) ? node.blockedSlots.length : 0), 0),
            conflictTypes: {}
        };
        
        // Contar tipos de conflictos
        this.links.forEach(link => {
            link.types.forEach(type => {
                stats.conflictTypes[type] = (stats.conflictTypes[type] || 0) + 1;
            });
        });
        
        return stats;
    }
}

// Si estamos utilizando módulos ES
export default GraphData;