/**
 * Clase que maneja la visualización del panel lateral con información detallada
 */
class SidePanelView {
    /**
     * Constructor de SidePanelView
     * @param {HTMLElement} container - Contenedor DOM del panel lateral
     */
    constructor(container) {
        this.container = container;
        this.nodeInfoContainer = container.querySelector("#nodeInfo") || document.createElement('div');
        this.conflictListContainer = container.querySelector("#conflictList") || document.createElement('div');
        
        // Estado
        this.selectedNode = null;
        this.conflicts = [];
        
        // Inicializar
        this.init();
    }
    
    /**
     * Inicializa el panel lateral
     */
    init() {
        // Verificar que los contenedores existan
        if (!this.container.contains(this.nodeInfoContainer)) {
            this.nodeInfoContainer = document.createElement('div');
            this.nodeInfoContainer.id = 'nodeInfo';
            this.container.appendChild(this.nodeInfoContainer);
        }
        
        if (!this.container.contains(this.conflictListContainer)) {
            this.conflictListContainer = document.createElement('div');
            this.conflictListContainer.id = 'conflictList';
            this.container.appendChild(this.conflictListContainer);
        }
        
        // Mensaje inicial
        this.clear();
    }
    
    /**
     * Muestra la información detallada de un nodo
     * @param {Object} node - Nodo seleccionado
     */
    showNodeInfo(node) {
        if (!node) {
            this.nodeInfoContainer.innerHTML = "<p>Selecciona un nodo para ver detalles</p>";
            this.selectedNode = null;
            return;
        }
        
        // Guardar referencia al nodo seleccionado
        this.selectedNode = node;
        
        // Obtener color para la cabecera según el profesor
        const headerColor = this.getNodeColor(node);
        const textColor = this.isColorDark(headerColor) ? "white" : "black";
        
        // Generar HTML para la información del nodo
        let html = `
            <div class="node-details">
                <div class="node-details-header" style="background: ${headerColor}; color: ${textColor};">
                    <h3 style="margin: 0;">${node.groupName || ""}</h3>
                    <div>${node.id || ""} - ${node.day || ""}</div>
                </div>
                <div class="node-details-content">
                    <p><strong>Profesor:</strong> ${node.professorName || "No asignado"}</p>
                    <p><strong>Aula:</strong> ${node.roomName || "No asignada"}</p>
                    <p><strong>Horario:</strong> ${this.formatTimeRange(node.startTime, node.endTime)}</p>
                    <p><strong>Estudiantes:</strong> ${node.enrolledStudents || "N/A"}</p>
                    <p><strong>Tipo de sesión:</strong> ${this.formatSessionType(node.sessionType)}</p>
                    ${node.requiresLab ? '<p><strong>Requiere laboratorio:</strong> Sí</p>' : ''}
                    ${this.renderBlockedSlots(node)}
                </div>
            </div>
        `;
        
        this.nodeInfoContainer.innerHTML = html;
    }
    
    /**
     * Renderiza la información de franjas bloqueadas
     * @param {Object} node - Nodo con franjas bloqueadas
     * @returns {string} HTML con la información de franjas bloqueadas
     */
    renderBlockedSlots(node) {
        if (!node.blockedSlots || node.blockedSlots.length === 0) {
            return '';
        }
        
        let html = `<div class="blocked-slots">
            <p><strong>Franjas bloqueadas (${node.blockedSlots.length}):</strong></p>
            <ul class="blocked-slot-list">`;
        
        // Ordenar franjas por día y hora
        const sortedSlots = [...node.blockedSlots].sort((a, b) => {
            if (a.day !== b.day) return a.day.localeCompare(b.day);
            return this.timeToMinutes(a.startTime) - this.timeToMinutes(b.startTime);
        });
        
        // Mostrar las primeras 3 franjas
        sortedSlots.slice(0, 3).forEach(slot => {
            const isConflicting = this.isBlockedSlotConflicting(slot, node);
            html += `<li ${isConflicting ? 'class="conflicting"' : ''}>
                ${slot.day}: ${slot.startTime}-${slot.endTime}
                ${isConflicting ? ' <span class="conflict-badge">Conflicto</span>' : ''}
            </li>`;
        });
        
        // Si hay más franjas, mostrar indicador
        if (sortedSlots.length > 3) {
            html += `<li>... y ${sortedSlots.length - 3} más</li>`;
        }
        
        html += `</ul></div>`;
        return html;
    }
    
    /**
     * Comprueba si una franja bloqueada tiene conflicto con un nodo
     * @param {Object} slot - Franja bloqueada
     * @param {Object} node - Nodo a comprobar
     * @returns {boolean} true si hay conflicto
     */
    isBlockedSlotConflicting(slot, node) {
        if (slot.day !== node.day) return false;
        
        const slotStart = this.timeToMinutes(slot.startTime);
        const slotEnd = this.timeToMinutes(slot.endTime);
        const nodeStart = this.timeToMinutes(node.startTime);
        const nodeEnd = this.timeToMinutes(node.endTime);
        
        return (slotStart < nodeEnd && slotEnd > nodeStart);
    }
    
    /**
     * Muestra la lista de conflictos relacionados con un nodo
     * @param {Array} conflicts - Lista de conflictos
     * @param {Object} selectedNode - Nodo seleccionado actualmente
     */
    showConflicts(conflicts, selectedNode) {
        // Guardar referencias
        this.conflicts = conflicts || [];
        
        if (!selectedNode || !conflicts || conflicts.length === 0) {
            this.conflictListContainer.innerHTML = "<p>No hay conflictos para este nodo.</p>";
            return;
        }
        
        // Cabecera con conteo de conflictos
        let html = `
            <div class="conflict-count">
                ${conflicts.length} conflicto${conflicts.length !== 1 ? 's' : ''} detectado${conflicts.length !== 1 ? 's' : ''}
            </div>
        `;
        
        // Agrupar conflictos por tipo principal
        const groupedConflicts = this.groupConflictsByType(conflicts, selectedNode);
        
        // Generar lista de conflictos
        Object.entries(groupedConflicts).forEach(([type, typeConflicts]) => {
            const firstConflict = typeConflicts[0];
            const conflictColor = this.getConflictColor(firstConflict);
            const textColor = this.isColorDark(conflictColor) ? "white" : "black";
            const count = typeConflicts.length;
            
            // Determinar si son auto-conflictos
            const isSelfLoop = firstConflict.source.id === firstConflict.target.id;
            
            html += `
                <div class="conflict-item">
                    <div class="conflict-header" style="background: ${conflictColor}; color: ${textColor};">
                        <strong>${type}</strong>
                        <div class="conflict-badge" style="background: white; color: ${conflictColor};">
                            ${count}
                        </div>
                    </div>
                    <div class="conflict-content">
                        ${isSelfLoop ? 
                          this.renderSelfConflictDetails(firstConflict, typeConflicts) : 
                          this.renderConflictDetails(typeConflicts, selectedNode)}
                    </div>
                </div>
            `;
        });
        
        this.conflictListContainer.innerHTML = html;
    }
    
    /**
     * Agrupa conflictos por su tipo principal
     * @param {Array} conflicts - Lista de conflictos
     * @param {Object} selectedNode - Nodo seleccionado
     * @returns {Object} Conflictos agrupados por tipo
     */
    groupConflictsByType(conflicts, selectedNode) {
        const grouped = {};
        
        conflicts.forEach(conflict => {
            // Determinar el tipo principal del conflicto
            let mainType = this.getConflictMainType(conflict);
            
            // Inicializar el grupo si no existe
            if (!grouped[mainType]) {
                grouped[mainType] = [];
            }
            
            // Añadir el conflicto al grupo
            grouped[mainType].push(conflict);
        });
        
        return grouped;
    }
    
    /**
     * Obtiene el tipo principal de un conflicto
     * @param {Object} conflict - Conflicto
     * @returns {string} Tipo principal
     */
    getConflictMainType(conflict) {
        if (!conflict || !Array.isArray(conflict.types) || conflict.types.length === 0) {
            return "Conflicto desconocido";
        }
        
        // Orden de prioridad para tipos de conflicto
        const priorityTypes = [
            "franja bloqueada",
            "mismo profesor",
            "misma sala",
            "mismo grupo",
            "jornada",
            "horario"
        ];
        
        // Buscar el primer tipo que coincida con las prioridades
        for (const priority of priorityTypes) {
            const matchingType = conflict.types.find(type => 
                type.toLowerCase().includes(priority.toLowerCase())
            );
            
            if (matchingType) {
                return matchingType;
            }
        }
        
        // Si no hay coincidencias, devolver el primer tipo
        return conflict.types[0];
    }
    
    /**
     * Renderiza los detalles de auto-conflictos
     * @param {Object} conflict - Conflicto 
     * @param {Array} allConflicts - Todos los conflictos del mismo tipo
     * @returns {string} HTML con detalles del auto-conflicto
     */
    renderSelfConflictDetails(conflict, allConflicts) {
        // Obtener todas las franjas bloqueadas que tienen conflicto
        const node = conflict.source;
        const conflictingSlots = node.blockedSlots.filter(slot => 
            this.isBlockedSlotConflicting(slot, node)
        );
        
        let html = `
            <p><strong>Auto-conflicto</strong></p>
            <p><strong>Horario de clase:</strong> ${this.formatTimeRange(node.startTime, node.endTime)}</p>
        `;
        
        if (conflictingSlots.length > 0) {
            html += `<p><strong>Franjas bloqueadas en conflicto:</strong></p>
            <ul>`;
            
            conflictingSlots.forEach(slot => {
                html += `<li>${slot.day}: ${slot.startTime}-${slot.endTime}</li>`;
            });
            
            html += `</ul>`;
        }
        
        return html;
    }
    
    /**
     * Renderiza los detalles de conflictos normales
     * @param {Array} conflicts - Conflictos del mismo tipo
     * @param {Object} selectedNode - Nodo seleccionado
     * @returns {string} HTML con detalles de los conflictos
     */
    renderConflictDetails(conflicts, selectedNode) {
        // Agrupar por nodo "del otro lado"
        const otherNodes = new Map();
        
        conflicts.forEach(conflict => {
            const otherNode = conflict.source.id === selectedNode.id ? 
                conflict.target : conflict.source;
            
            if (!otherNodes.has(otherNode.id)) {
                otherNodes.set(otherNode.id, {
                    node: otherNode,
                    count: 1,
                    types: [...conflict.types]
                });
            } else {
                const entry = otherNodes.get(otherNode.id);
                entry.count++;
                
                // Añadir tipos únicos
                conflict.types.forEach(type => {
                    if (!entry.types.includes(type)) {
                        entry.types.push(type);
                    }
                });
            }
        });
        
        // Generar HTML para cada nodo en conflicto
        let html = '';
        
        if (otherNodes.size === 1) {
            // Un solo nodo en conflicto, mostrar detalles completos
            const [nodeInfo] = otherNodes.values();
            const otherNode = nodeInfo.node;
            
            html += `
                <p><strong>${otherNode.groupName || "Grupo"}</strong></p>
                <p><strong>Profesor:</strong> ${otherNode.professorName || "No asignado"}</p>
                <p><strong>Aula:</strong> ${otherNode.roomName || "No asignada"}</p>
                <p><strong>Horario:</strong> ${this.formatTimeRange(otherNode.startTime, otherNode.endTime)}</p>
                ${nodeInfo.types.length > 1 ? `
                <p><strong>Tipos de conflicto:</strong></p>
                <ul>
                    ${nodeInfo.types.map(type => `<li>${type}</li>`).join('')}
                </ul>
                ` : ''}
            `;
        } else {
            // Múltiples nodos, mostrar lista resumida
            html += `<p><strong>${otherNodes.size} grupos en conflicto:</strong></p>
            <ul>`;
            
            otherNodes.forEach(nodeInfo => {
                const otherNode = nodeInfo.node;
                html += `
                    <li>
                        <strong>${otherNode.groupName || "Grupo"}</strong>: 
                        ${otherNode.professorName || "N/A"}, 
                        ${this.formatTimeRange(otherNode.startTime, otherNode.endTime)}
                        ${nodeInfo.count > 1 ? ` (${nodeInfo.count} conflictos)` : ''}
                    </li>
                `;
            });
            
            html += `</ul>`;
        }
        
        return html;
    }
    
    /**
     * Limpia el panel lateral
     */
    clear() {
        this.nodeInfoContainer.innerHTML = "<p>Selecciona un nodo para ver detalles</p>";
        this.conflictListContainer.innerHTML = "<p>Selecciona un nodo para ver sus conflictos</p>";
        this.selectedNode = null;
        this.conflicts = [];
    }
    
    /**
     * Formatea un rango de tiempo
     * @param {string} startTime - Hora de inicio
     * @param {string} endTime - Hora de fin
     * @returns {string} Rango formateado
     */
    formatTimeRange(startTime, endTime) {
        if (!startTime && !endTime) return "No definido";
        if (!startTime) return endTime;
        if (!endTime) return startTime;
        
        return `${startTime} - ${endTime}`;
    }
    
    /**
     * Formatea un tipo de sesión para mejor legibilidad
     * @param {string} sessionType - Tipo de sesión (D, T, P, etc.)
     * @returns {string} Descripción del tipo de sesión
     */
    formatSessionType(sessionType) {
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
    
    /**
     * Obtiene el color para un nodo basado en su profesorId
     * @param {Object} node - Nodo
     * @returns {string} Color en formato hexadecimal
     */
    getNodeColor(node) {
        if (node && node.professorId) {
            const colors = [
                "#4285f4", // Azul Google
                "#34a853", // Verde Google
                "#fbbc04", // Amarillo Google
                "#ea4335", // Rojo Google
                "#9c27b0", // Púrpura
                "#00acc1", // Cian
                "#ff7043", // Naranja
                "#5c6bc0"  // Índigo
            ];
            return colors[(node.professorId - 1) % colors.length];
        }
        return "#42a5f5"; // Azul por defecto
    }
    
    /**
     * Obtiene el color para un conflicto basado en su tipo
     * @param {Object} conflict - Conflicto
     * @returns {string} Color en formato hexadecimal
     */
    getConflictColor(conflict) {
        if (!conflict || !Array.isArray(conflict.types) || conflict.types.length === 0) {
            return "#9e9e9e"; // Gris por defecto
        }
        
        // Obtener el tipo principal
        const mainType = this.getConflictMainType(conflict).toLowerCase();
        
        // Mapeo de colores según tipo
        if (mainType.includes("franja bloqueada"))
            return "#9c27b0"; // Púrpura
        if (mainType.includes("mismo profesor"))
            return "#e53935"; // Rojo
        if (mainType.includes("misma sala"))
            return "#ffb300"; // Amarillo
        if (mainType.includes("mismo grupo"))
            return "#43a047"; // Verde
        if (mainType.includes("jornada") || mainType.includes("horario"))
            return "#26a69a"; // Verde azulado
        
        return "#9e9e9e"; // Gris para tipos desconocidos
    }
    
    /**
     * Determina si un color es oscuro para elegir texto blanco o negro
     * @param {string} hexColor - Color en formato hexadecimal
     * @returns {boolean} true si el color es oscuro
     */
    isColorDark(hexColor) {
        const r = parseInt(hexColor.slice(1, 3), 16);
        const g = parseInt(hexColor.slice(3, 5), 16);
        const b = parseInt(hexColor.slice(5, 7), 16);
        
        // Fórmula para calcular brillo percibido
        const brightness = (r * 299 + g * 587 + b * 114) / 1000;
        return brightness < 128;
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
}

// Si estamos utilizando módulos ES
export default SidePanelView;