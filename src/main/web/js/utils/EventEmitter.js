/**
 * Clase que implementa el patrón Observador (publish/subscribe)
 * Permite la comunicación desacoplada entre componentes
 */
class EventEmitter {
    /**
     * Constructor del EventEmitter
     */
    constructor() {
        this.events = {};
    }
    
    /**
     * Registra un listener para un evento específico
     * @param {string} event - Nombre del evento
     * @param {Function} listener - Función a ejecutar cuando ocurra el evento
     * @returns {EventEmitter} Instancia actual para encadenamiento
     */
    on(event, listener) {
        if (!this.events[event]) {
            this.events[event] = [];
        }
        
        this.events[event].push(listener);
        return this;
    }
    
    /**
     * Registra un listener que se ejecutará una sola vez
     * @param {string} event - Nombre del evento
     * @param {Function} listener - Función a ejecutar cuando ocurra el evento
     * @returns {EventEmitter} Instancia actual para encadenamiento
     */
    once(event, listener) {
        const onceWrapper = (...args) => {
            listener(...args);
            this.off(event, onceWrapper);
        };
        
        // Guardar referencia al listener original para permitir su eliminación
        onceWrapper.originalListener = listener;
        
        return this.on(event, onceWrapper);
    }
    
    /**
     * Emite un evento con los argumentos proporcionados
     * @param {string} event - Nombre del evento
     * @param {...any} args - Argumentos para pasar a los listeners
     * @returns {boolean} true si el evento tenía listeners registrados
     */
    emit(event, ...args) {
        if (!this.events[event]) {
            return false;
        }
        
        const listeners = [...this.events[event]];
        listeners.forEach(listener => {
            try {
                listener(...args);
            } catch (error) {
                console.error(`Error en listener de evento ${event}:`, error);
            }
        });
        
        return true;
    }
    
    /**
     * Elimina un listener específico de un evento
     * @param {string} event - Nombre del evento
     * @param {Function} listenerToRemove - Listener a eliminar
     * @returns {EventEmitter} Instancia actual para encadenamiento
     */
    off(event, listenerToRemove) {
        if (!this.events[event]) {
            return this;
        }
        
        this.events[event] = this.events[event].filter(listener => {
            // Comprobar el listener directo o el originalListener para eventos .once()
            return listener !== listenerToRemove && 
                  listener.originalListener !== listenerToRemove;
        });
        
        // Eliminar array vacío para liberar memoria
        if (this.events[event].length === 0) {
            delete this.events[event];
        }
        
        return this;
    }
    
    /**
     * Elimina todos los listeners de un evento específico o de todos los eventos
     * @param {string} [event] - Nombre del evento (opcional)
     * @returns {EventEmitter} Instancia actual para encadenamiento
     */
    removeAllListeners(event) {
        if (event) {
            // Eliminar listeners de un evento específico
            delete this.events[event];
        } else {
            // Eliminar todos los listeners
            this.events = {};
        }
        
        return this;
    }
    
    /**
     * Obtiene una lista de los listeners registrados para un evento
     * @param {string} event - Nombre del evento
     * @returns {Function[]} Array de funciones listener
     */
    listeners(event) {
        return this.events[event] || [];
    }
    
    /**
     * Obtiene el número de listeners registrados para un evento
     * @param {string} event - Nombre del evento
     * @returns {number} Número de listeners
     */
    listenerCount(event) {
        return this.listeners(event).length;
    }
}

// Si estamos utilizando módulos ES
export default EventEmitter;