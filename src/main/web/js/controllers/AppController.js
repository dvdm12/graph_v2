/**
 * Controlador principal de la aplicación
 * Coordina todos los componentes y maneja el flujo de la aplicación
 */
class AppController {
    /**
     * Constructor del AppController
     * @param {Object} config - Configuración opcional
     * @param {boolean} config.loadExampleOnStart - Si debe cargar datos de ejemplo al inicio
     * @param {boolean} config.enableDebugMode - Si debe habilitar modo de depuración
     * @param {Object} config.elementSelectors - Selectores de elementos DOM
     */
    constructor(config = {}) {
        // Configuración
        this.config = {
            loadExampleOnStart: config.loadExampleOnStart || false,
            enableDebugMode: config.enableDebugMode || false,
            elementSelectors: {
                graphContainer: '#graphContainer',
                sidePanel: '.side-panel',
                dropZone: '#dropZone',
                uploadButton: '#uploadJSON',
                fileInput: '#jsonFile',
                clearButton: '#clearGraph',
                ...config.elementSelectors
            }
        };
        
        // Crear el emisor de eventos para la comunicación entre componentes
        this.eventEmitter = new EventEmitter();
        
        // Registrar eventos para depuración si está habilitado
        if (this.config.enableDebugMode) {
            this.setupDebugEvents();
        }
        
        // Inicializar modelo de datos
        this.graphData = new GraphData();
        
        // Inicializar vistas
        this.graphView = new GraphView(
            document.querySelector(this.config.elementSelectors.graphContainer),
            this.eventEmitter
        );
        
        this.sidePanel = new SidePanelView(
            document.querySelector(this.config.elementSelectors.sidePanel)
        );
        
        // Inicializar controladores
        this.fileController = new FileController(
            this.eventEmitter,
            document.querySelector(this.config.elementSelectors.dropZone),
            document.querySelector(this.config.elementSelectors.uploadButton),
            document.querySelector(this.config.elementSelectors.fileInput)
        );
        
        this.selectionController = new SelectionController(
            this.graphData,
            this.graphView,
            this.sidePanel,
            this.eventEmitter
        );
        
        // Estado de la aplicación
        this.isDataLoaded = false;
        this.lastLoadedFile = null;
        
        // Configurar eventos
        this.setupEventListeners();
        
        // Cargar datos de ejemplo si está configurado
        if (this.config.loadExampleOnStart) {
            this.loadExampleData();
        }
    }
    
    /**
     * Configura escuchas de eventos
     */
    setupEventListeners() {
        // Evento de carga de archivo
        this.eventEmitter.on('file-loaded', (event) => {
            this.loadGraphData(event.data);
            this.lastLoadedFile = event.file ? event.file.name : 'datos de ejemplo';
        });
        
        // Evento de error en la carga de archivo
        this.eventEmitter.on('file-error', (event) => {
            this.showError(event.message);
        });
        
        // Botón para limpiar el grafo
        document.querySelector(this.config.elementSelectors.clearButton)
            .addEventListener('click', () => {
                this.clearGraph();
            });
        
        // Escuchar cambios de tamaño de ventana para actualizar dimensiones
        window.addEventListener('resize', () => {
            if (this.isDataLoaded) {
                this.graphView.handleResize();
            }
        });
        
        // Eventos de teclado para navegación
        document.addEventListener('keydown', (e) => {
            this.handleKeyboardEvent(e);
        });
    }
    
    /**
     * Configura eventos para depuración
     */
    setupDebugEvents() {
        const events = [
            'file-loading', 'file-loaded', 'file-error',
            'node-clicked', 'node-selected', 'node-deselected',
            'background-clicked', 'graph-loaded', 'graph-cleared'
        ];
        
        // Registrar listener para cada evento
        events.forEach(eventName => {
            this.eventEmitter.on(eventName, (data) => {
                console.debug(`[Debug] Evento '${eventName}':`, data);
            });
        });
    }
    
    /**
     * Maneja eventos de teclado
     * @param {KeyboardEvent} e - Evento de teclado
     */
    handleKeyboardEvent(e) {
        // Solo procesar si hay datos cargados
        if (!this.isDataLoaded) return;
        
        switch (e.key) {
            case 'Escape':
                // Limpiar selección
                this.selectionController.clearSelection();
                break;
                
            case 'c':
                // Centrar grafo
                if (!e.ctrlKey && !e.metaKey) { // Evitar interferir con Ctrl+C
                    this.graphView.centerGraph();
                }
                break;
                
            case 'ArrowLeft':
                // Navegar atrás en el historial de selección
                if (e.altKey) {
                    e.preventDefault();
                    this.eventEmitter.emit('selection-back');
                }
                break;
                
            case 'ArrowRight':
                // Navegar adelante en el historial de selección
                if (e.altKey) {
                    e.preventDefault();
                    this.eventEmitter.emit('selection-forward');
                }
                break;
                
            case 'n':
                // Seleccionar siguiente nodo con conflicto
                if (e.altKey) {
                    e.preventDefault();
                    this.selectionController.selectNextConflictingNode();
                }
                break;
        }
    }
    
    /**
     * Carga los datos del grafo a partir de un objeto JSON
     * @param {Object} jsonData - Datos JSON con nodos y aristas
     */
    loadGraphData(jsonData) {
        try {
            // Cargar datos en el modelo
            this.graphData.loadFromJSON(jsonData);
            
            // Renderizar grafo
            this.graphView.render(this.graphData);
            
            // Ocultar zona de arrastrar y soltar
            const dropZone = document.querySelector(this.config.elementSelectors.dropZone);
            if (dropZone) {
                dropZone.style.display = 'none';
            }
            
            // Actualizar estado
            this.isDataLoaded = true;
            
            // Emitir evento de carga exitosa
            this.eventEmitter.emit('graph-loaded', {
                stats: this.graphData.getStats()
            });
            
            // Mostrar estadísticas
            this.showStats();
            
        } catch (error) {
            console.error("Error al cargar datos del grafo:", error);
            this.showError(`Error al cargar datos del grafo: ${error.message}`);
        }
    }
    
    /**
     * Limpia el grafo actual
     */
    clearGraph() {
        if (!this.isDataLoaded) {
            return;
        }
        
        if (confirm("¿Estás seguro de que deseas borrar el grafo?")) {
            // Limpiar datos
            this.graphData.clear();
            
            // Actualizar vista
            this.graphView.render(this.graphData);
            
            // Limpiar panel lateral
            this.sidePanel.clear();
            
            // Mostrar zona de arrastrar y soltar
            const dropZone = document.querySelector(this.config.elementSelectors.dropZone);
            if (dropZone) {
                dropZone.style.display = 'flex';
            }
            
            // Actualizar estado
            this.isDataLoaded = false;
            this.lastLoadedFile = null;
            
            // Emitir evento
            this.eventEmitter.emit('graph-cleared');
        }
    }
    
    /**
     * Muestra un mensaje de error
     * @param {string} message - Mensaje de error
     */
    showError(message) {
        alert(message);
        console.error(message);
    }
    
    /**
     * Muestra las estadísticas del grafo
     */
    showStats() {
        const stats = this.graphData.getStats();
        
        console.info('Estadísticas del grafo:', stats);
        
        if (this.config.enableDebugMode) {
            const statsText = `
                Grafo cargado de: ${this.lastLoadedFile || 'desconocido'}
                Nodos: ${stats.nodeCount}
                Enlaces: ${stats.linkCount}
                Conflictos: ${stats.conflictCount}
                Auto-bucles: ${stats.selfLoopCount}
                Profesores: ${stats.professorCount}
                Aulas: ${stats.roomCount}
                Grupos: ${stats.groupCount}
            `;
            
            console.debug(statsText);
        }
    }
    
    /**
     * Carga datos de ejemplo (opcional)
     */
    loadExampleData() {
        const exampleData = {
            "nodes": [
                {
                    "id": 1,
                    "assignmentDate": "2025-05-13",
                    "day": "Monday",
                    "startTime": "10:00",
                    "endTime": "12:00",
                    "professorId": 1,
                    "professorName": "Dr. Juan Pérez",
                    "roomId": 1,
                    "roomName": "LAB101",
                    "groupId": 101,
                    "groupName": "Grupo A",
                    "sessionType": "D",
                    "requiresLab": true,
                    "enrolledStudents": 25,
                    "blockedSlots": [
                        {
                            "day": "Monday",
                            "startTime": "09:00",
                            "endTime": "11:00"
                        },
                        {
                            "day": "Wednesday",
                            "startTime": "14:00",
                            "endTime": "16:00"
                        }
                    ]
                },
                {
                    "id": 2,
                    "assignmentDate": "2025-05-13",
                    "day": "Monday",
                    "startTime": "11:00",
                    "endTime": "13:00",
                    "professorId": 1,
                    "professorName": "Dr. Juan Pérez",
                    "roomId": 2,
                    "roomName": "A101",
                    "groupId": 102,
                    "groupName": "Grupo B",
                    "sessionType": "D",
                    "requiresLab": false,
                    "enrolledStudents": 30,
                    "blockedSlots": [
                        {
                            "day": "Monday",
                            "startTime": "09:00",
                            "endTime": "11:00"
                        },
                        {
                            "day": "Wednesday",
                            "startTime": "14:00",
                            "endTime": "16:00"
                        }
                    ]
                },
                {
                    "id": 3,
                    "assignmentDate": "2025-05-13",
                    "day": "Monday",
                    "startTime": "12:00",
                    "endTime": "14:00",
                    "professorId": 2,
                    "professorName": "Dra. Ana García",
                    "roomId": 1,
                    "roomName": "LAB101",
                    "groupId": 103,
                    "groupName": "Grupo C",
                    "sessionType": "L",
                    "requiresLab": true,
                    "enrolledStudents": 20,
                    "blockedSlots": []
                },
                {
                    "id": 4,
                    "assignmentDate": "2025-05-13",
                    "day": "Monday",
                    "startTime": "13:00",
                    "endTime": "15:00",
                    "professorId": 2,
                    "professorName": "Dra. Ana García",
                    "roomId": 3,
                    "roomName": "A102",
                    "groupId": 104,
                    "groupName": "Grupo D",
                    "sessionType": "T",
                    "requiresLab": false,
                    "enrolledStudents": 35,
                    "blockedSlots": []
                },
                {
                    "id": 5,
                    "assignmentDate": "2025-05-13",
                    "day": "Tuesday",
                    "startTime": "10:00",
                    "endTime": "12:00",
                    "professorId": 3,
                    "professorName": "Dr. Carlos Ramírez",
                    "roomId": 1,
                    "roomName": "LAB101",
                    "groupId": 105,
                    "groupName": "Grupo E",
                    "sessionType": "L",
                    "requiresLab": true,
                    "enrolledStudents": 15,
                    "blockedSlots": []
                }
            ],
            "edges": [
                {
                    "between": [1, 1],
                    "conflicts": ["Conflicto con franja bloqueada del profesor"]
                },
                {
                    "between": [1, 2],
                    "conflicts": ["Solapamiento de horarios (mismo profesor)", "Misma jornada y horario"]
                },
                {
                    "between": [2, 3],
                    "conflicts": ["Misma jornada y horario"]
                },
                {
                    "between": [3, 4],
                    "conflicts": ["Solapamiento de horarios (mismo profesor)", "Misma jornada y horario"]
                },
                {
                    "between": [2, 2],
                    "conflicts": ["Conflicto con franja bloqueada del profesor"]
                },
                {
                    "between": [2, 4],
                    "conflicts": ["Misma jornada y horario"]
                }
            ]
        };
        
        // Cargar los datos de ejemplo
        this.loadGraphData(exampleData);
    }
    
    /**
     * Informa sobre el estado actual de la aplicación
     * @returns {Object} Estado de la aplicación
     */
    getApplicationState() {
        return {
            isDataLoaded: this.isDataLoaded,
            lastLoadedFile: this.lastLoadedFile,
            selectedNode: this.selectionController.getSelectedNode(),
            graphStats: this.isDataLoaded ? this.graphData.getStats() : null
        };
    }
}

// Si estamos utilizando módulos ES
export default AppController;