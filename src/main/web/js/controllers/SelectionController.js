/**
 * Controlador para gestionar la selección de nodos
 * Coordina la comunicación entre el modelo de datos y las vistas
 */
class SelectionController {
    /**
     * Constructor del SelectionController
     * @param {GraphData} graphData - Modelo de datos del grafo
     * @param {GraphView} graphView - Vista del grafo
     * @param {SidePanelView} sidePanel - Vista del panel lateral
     * @param {EventEmitter} eventEmitter - Emisor de eventos para comunicación
     */
    constructor(graphData, graphView, sidePanel, eventEmitter) {
        // Referencias a componentes
        this.graphData = graphData;
        this.graphView = graphView;
        this.sidePanel = sidePanel;
        this.eventEmitter = eventEmitter;
        
        // Estado
        this.selectedNode = null;
        this.selectedNodeId = null;
        this.selectionHistory = []; // Historial de selecciones para navegación
        
        // Configuración
        this.config = {
            maxHistoryLength: 10, // Número máximo de selecciones en historial
            autoZoomOnSelect: true, // Zoom automático al seleccionar
            highlightRelatedNodes: true // Resaltar nodos relacionados al seleccionar
        };
        
        // Inicializar
        this.setupEvents();
    }
    
    /**
     * Configura los eventos relacionados con la selección
     */
    setupEvents() {
        // Cuando se hace clic en un nodo
        this.eventEmitter.on('node-clicked', (nodeId) => {
            this.selectNode(nodeId);
        });
        
        // Cuando se hace clic en el fondo (deseleccionar)
        this.eventEmitter.on('background-clicked', () => {
            this.clearSelection();
        });
        
        // Cuando se carga un nuevo grafo (limpiar selección)
        this.eventEmitter.on('graph-loaded', () => {
            this.clearSelection();
            this.selectionHistory = [];
        });
        
        // Cuando se borra el grafo (limpiar todo)
        this.eventEmitter.on('graph-cleared', () => {
            this.clearSelection();
            this.selectionHistory = [];
        });
        
        // Opcionales: eventos para navegación por historial
        this.eventEmitter.on('selection-back', () => {
            this.navigateSelectionHistory(-1);
        });
        
        this.eventEmitter.on('selection-forward', () => {
            this.navigateSelectionHistory(1);
        });
    }
    
    /**
     * Selecciona un nodo por su ID
     * @param {string|number} nodeId - ID del nodo a seleccionar
     * @param {boolean} addToHistory - Si se debe añadir al historial de selección
     * @returns {boolean} true si la selección fue exitosa
     */
    selectNode(nodeId, addToHistory = true) {
        // Convertir a string para consistencia
        const strNodeId = String(nodeId);
        
        // Si es el mismo nodo, no hacer nada
        if (this.selectedNodeId === strNodeId) {
            return true;
        }
        
        // Obtener el nodo del modelo de datos
        const node = this.graphData.getNodeById(strNodeId);
        
        if (!node) {
            console.warn(`Nodo con ID ${strNodeId} no encontrado`);
            return false;
        }
        
        // Guardar selección anterior para historial
        const previousNodeId = this.selectedNodeId;
        
        // Actualizar estado
        this.selectedNode = node;
        this.selectedNodeId = strNodeId;
        
        // Actualizar historial de selección
        if (addToHistory && previousNodeId) {
            this.addToSelectionHistory(previousNodeId);
        }
        
        // Actualizar vista del grafo
        this.graphView.highlightNode(strNodeId);
        
        // Zoom al nodo seleccionado si está configurado
        if (this.config.autoZoomOnSelect) {
            this.graphView.zoomToNode(strNodeId);
        }
        
        // Actualizar panel lateral
        this.updateSidePanel();
        
        // Emitir evento de selección para otros componentes
        this.eventEmitter.emit('node-selected', {
            nodeId: strNodeId,
            node: node
        });
        
        return true;
    }
    
    /**
     * Limpia la selección actual
     */
    clearSelection() {
        if (!this.selectedNode) {
            return;
        }
        
        // Guardar selección anterior para historial
        const previousNodeId = this.selectedNodeId;
        
        // Resetear vista del grafo
        this.graphView.resetHighlights();
        
        // Limpiar panel lateral
        this.sidePanel.clear();
        
        // Limpiar selección
        this.selectedNode = null;
        this.selectedNodeId = null;
        
        // Emitir evento de deselección
        this.eventEmitter.emit('node-deselected', {
            previousNodeId: previousNodeId
        });
    }
    
    /**
     * Actualiza el panel lateral con información del nodo seleccionado
     */
    updateSidePanel() {
        if (!this.selectedNode) {
            this.sidePanel.clear();
            return;
        }
        
        // Mostrar información del nodo
        this.sidePanel.showNodeInfo(this.selectedNode);
        
        // Obtener y mostrar conflictos
        const conflicts = this.graphData.getConflictsForNode(this.selectedNodeId);
        this.sidePanel.showConflicts(conflicts, this.selectedNode);
    }
    
    /**
     * Añade un nodo al historial de selección
     * @param {string} nodeId - ID del nodo
     */
    addToSelectionHistory(nodeId) {
        // Evitar duplicados consecutivos
        if (this.selectionHistory.length > 0 && 
            this.selectionHistory[this.selectionHistory.length - 1] === nodeId) {
            return;
        }
        
        // Añadir al historial
        this.selectionHistory.push(nodeId);
        
        // Limitar el tamaño del historial
        if (this.selectionHistory.length > this.config.maxHistoryLength) {
            this.selectionHistory.shift();
        }
    }
    
    /**
     * Navega por el historial de selección
     * @param {number} steps - Pasos a navegar (positivo para adelante, negativo para atrás)
     * @returns {boolean} true si la navegación fue exitosa
     */
    navigateSelectionHistory(steps) {
        if (this.selectionHistory.length === 0) {
            return false;
        }
        
        // Encontrar el índice actual
        const currentIndex = this.selectedNodeId ? 
            this.selectionHistory.lastIndexOf(this.selectedNodeId) : -1;
        
        // Calcular el nuevo índice
        const newIndex = currentIndex + steps;
        
        // Verificar límites
        if (newIndex < 0 || newIndex >= this.selectionHistory.length) {
            return false;
        }
        
        // Seleccionar el nodo del historial
        const nodeId = this.selectionHistory[newIndex];
        return this.selectNode(nodeId, false); // No añadir al historial para evitar bucles
    }
    
    /**
     * Obtiene el nodo actualmente seleccionado
     * @returns {Object|null} Nodo seleccionado o null
     */
    getSelectedNode() {
        return this.selectedNode;
    }
    
    /**
     * Verifica si hay algún nodo seleccionado
     * @returns {boolean} true si hay un nodo seleccionado
     */
    hasSelection() {
        return this.selectedNode !== null;
    }
    
    /**
     * Obtiene todos los conflictos del nodo seleccionado
     * @returns {Array} Lista de conflictos o array vacío
     */
    getSelectedNodeConflicts() {
        if (!this.selectedNodeId) {
            return [];
        }
        
        return this.graphData.getConflictsForNode(this.selectedNodeId);
    }
    
    /**
     * Obtiene nodos relacionados con el nodo seleccionado
     * @returns {Array} Lista de nodos relacionados o array vacío
     */
    getRelatedNodes() {
        if (!this.selectedNodeId) {
            return [];
        }
        
        const conflicts = this.graphData.getConflictsForNode(this.selectedNodeId);
        const relatedNodesMap = new Map();
        
        conflicts.forEach(conflict => {
            // Evitar auto-bucles
            if (conflict.isSelfLoop) {
                return;
            }
            
            // Añadir el nodo del otro extremo
            const otherNode = conflict.source.id === this.selectedNodeId ? 
                conflict.target : conflict.source;
            
            relatedNodesMap.set(otherNode.id, otherNode);
        });
        
        return Array.from(relatedNodesMap.values());
    }
    
    /**
     * Selecciona el siguiente nodo con conflicto
     * @returns {boolean} true si se seleccionó un nuevo nodo
     */
    selectNextConflictingNode() {
        if (!this.selectedNodeId) {
            return false;
        }
        
        const relatedNodes = this.getRelatedNodes();
        if (relatedNodes.length === 0) {
            return false;
        }
        
        // Seleccionar el primer nodo relacionado
        return this.selectNode(relatedNodes[0].id);
    }
}

// Si estamos utilizando módulos ES
export default SelectionController;