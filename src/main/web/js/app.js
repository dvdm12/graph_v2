/**
 * Punto de entrada de la aplicación
 * Importa los componentes necesarios y lanza la aplicación
 */

// Importar clases de utilidades
import EventEmitter from './utils/EventEmitter.js';
import ColorUtils from './utils/ColorUtils.js';
import FormatUtils from './utils/FormatUtils.js';

// Importar modelos
import GraphData from './models/GraphData.js';
import Node from './models/Node.js';
import Conflict from './models/Conflict.js';

// Importar vistas
import GraphView from './views/GraphView.js';
import SidePanelView from './views/SidePanelView.js';

// Importar controladores
import FileController from './controllers/FileController.js';
import SelectionController from './controllers/SelectionController.js';
import AppController from './controllers/AppController.js';

/**
 * Inicializa la aplicación
 * @param {Object} config - Configuración de la aplicación
 */
function initializeApp(config = {}) {
    console.info('Inicializando Visualizador de Conflictos Académicos...');
    
    // Obtener parámetros de URL
    const urlParams = new URLSearchParams(window.location.search);
    
    // Configuración predeterminada
    const appConfig = {
        loadExampleOnStart: urlParams.has('demo') && urlParams.get('demo') === 'true',
        enableDebugMode: urlParams.has('debug') && urlParams.get('debug') === 'true',
        ...config
    };
    
    try {
        // Inicializar el controlador principal
        const app = new AppController(appConfig);
        
        // Exponer la instancia de la aplicación para depuración si está en modo debug
        if (appConfig.enableDebugMode) {
            window.app = app;
            console.debug('Modo de depuración habilitado. Accede a la aplicación desde la consola usando "app".');
            
            // También exponer clases para facilitar la depuración
            window.models = { GraphData, Node, Conflict };
            window.utils = { EventEmitter, ColorUtils, FormatUtils };
        }
        
        console.info('Visualizador de Conflictos Académicos inicializado correctamente.');
        return app;
    } catch (error) {
        console.error('Error al inicializar la aplicación:', error);
        alert(`Error al inicializar la aplicación: ${error.message}`);
        return null;
    }
}

// Función para cargar la aplicación cuando el DOM está listo
function bootstrapApplication() {
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => initializeApp());
    } else {
        initializeApp();
    }
}

// Iniciar la aplicación
bootstrapApplication();

// Para entornos que no soportan módulos ES, asegurarse de que las clases estén disponibles globalmente
if (typeof window !== 'undefined' && !window.hasOwnProperty('GraphData')) {
    window.GraphData = GraphData;
    window.GraphView = GraphView;
    window.SidePanelView = SidePanelView;
    window.FileController = FileController;
    window.SelectionController = SelectionController;
    window.AppController = AppController;
    window.EventEmitter = EventEmitter;
    window.ColorUtils = ColorUtils;
    window.FormatUtils = FormatUtils;
    window.Node = Node;
    window.Conflict = Conflict;
}

// Exportar clases principales para entornos con soporte para módulos
export {
    GraphData,
    GraphView,
    SidePanelView,
    FileController,
    SelectionController,
    AppController,
    EventEmitter,
    ColorUtils,
    FormatUtils,
    Node,
    Conflict
};

// Exportar la función de inicialización para uso programático
export default initializeApp;