/**
 * Controlador para manejo de carga de archivos
 * Gestiona eventos de arrastrar y soltar, y selección de archivos
 */
class FileController {
    /**
     * Constructor del FileController
     * @param {EventEmitter} eventEmitter - Instancia de EventEmitter para comunicación
     * @param {HTMLElement} dropZone - Elemento DOM para la zona de arrastrar y soltar
     * @param {HTMLElement} uploadButton - Botón para cargar archivos
     * @param {HTMLElement} fileInput - Input de tipo file para seleccionar archivos
     */
    constructor(eventEmitter, dropZone, uploadButton, fileInput) {
        // Referencias a elementos DOM y eventEmitter
        this.eventEmitter = eventEmitter;
        this.dropZone = dropZone;
        this.uploadButton = uploadButton;
        this.fileInput = fileInput;
        
        // Verificar que los elementos DOM existen
        if (!this.dropZone) {
            console.error("Error: dropZone no encontrado");
            return;
        }
        if (!this.uploadButton) {
            console.error("Error: uploadButton no encontrado");
            return;
        }
        if (!this.fileInput) {
            console.error("Error: fileInput no encontrado");
            return;
        }
        
        // Estado
        this.isLoading = false;
        this.lastFileLoaded = null;
        
        // Configuración
        this.config = {
            allowedTypes: ['application/json'],
            allowedExtensions: ['.json'],
            maxFileSizeMB: 10, // Tamaño máximo en MB
            validateStructure: true // Validar estructura del JSON
        };
        
        // Inicializar eventos
        this.setupEvents();
        
        // Mensaje inicial en consola
        console.info("FileController inicializado");
    }
    
    /**
     * Configura los eventos para la carga de archivos
     */
    setupEvents() {
        // Verificar que los elementos existen antes de configurar eventos
        if (!this.dropZone || !this.uploadButton || !this.fileInput) {
            console.error("No se pueden configurar eventos: faltan elementos DOM");
            return;
        }
        
        // Prevenir comportamiento por defecto en eventos de arrastrar
        ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
            this.dropZone.addEventListener(eventName, (e) => {
                console.log(`Evento ${eventName} detectado`);
                this.preventDefaults(e);
            }, false);
            
            document.body.addEventListener(eventName, this.preventDefaults.bind(this), false);
        });
        
        // Resaltar la zona al arrastrar sobre ella
        ['dragenter', 'dragover'].forEach(eventName => {
            this.dropZone.addEventListener(eventName, (e) => {
                console.log("Resaltando dropZone");
                this.highlight(e);
            }, false);
        });
        
        // Quitar resaltado al salir o soltar
        ['dragleave', 'drop'].forEach(eventName => {
            this.dropZone.addEventListener(eventName, (e) => {
                console.log("Quitando resaltado de dropZone");
                this.unhighlight(e);
            }, false);
        });
        
        // Manejar evento de soltar archivo
        this.dropZone.addEventListener('drop', (e) => {
            console.log("Archivo soltado en dropZone");
            this.handleDrop(e);
        }, false);
        
        // Clic en la zona de soltar también activa el input
        this.dropZone.addEventListener('click', () => {
            console.log("Clic en dropZone");
            if (!this.isLoading) {
                console.log("Activando fileInput desde dropZone");
                this.fileInput.click();
            }
        });
        
        // Botón de cargar archivo
        this.uploadButton.addEventListener('click', (e) => {
            console.log("Clic en botón de carga");
            if (!this.isLoading) {
                e.preventDefault(); // Prevenir comportamiento predeterminado del botón
                console.log("Activando fileInput desde botón");
                this.fileInput.click();
            }
        });
        
        // Cambio en el input de archivo
        this.fileInput.addEventListener('change', (e) => {
            console.log("Cambio detectado en fileInput, archivos:", e.target.files);
            this.handleFileInputChange(e);
        });
        
        console.log("Eventos configurados correctamente");
    }
    
    /**
     * Previene eventos por defecto para arrastrar y soltar
     * @param {Event} e - Evento del DOM
     */
    preventDefaults(e) {
        e.preventDefault();
        e.stopPropagation();
    }
    
    /**
     * Resalta la zona de soltar
     * @param {Event} e - Evento del DOM
     */
    highlight(e) {
        this.dropZone.classList.add('highlight');
        // Añadir mensaje visual para feedback
        const dragText = this.dropZone.querySelector('h2');
        if (dragText) {
            dragText.textContent = 'Suelta para cargar';
        }
    }
    
    /**
     * Quita el resaltado de la zona de soltar
     * @param {Event} e - Evento del DOM
     */
    unhighlight(e) {
        this.dropZone.classList.remove('highlight');
        // Restaurar mensaje original
        const dragText = this.dropZone.querySelector('h2');
        if (dragText) {
            dragText.textContent = 'Arrastra un archivo JSON aquí';
        }
    }
    
    /**
     * Maneja el evento de soltar archivos
     * @param {DragEvent} e - Evento de soltar
     */
    handleDrop(e) {
        console.log("Procesando evento drop");
        const dt = e.dataTransfer;
        
        if (!dt) {
            console.error("Error: dataTransfer no disponible en el evento drop");
            return;
        }
        
        const files = dt.files;
        console.log("Archivos detectados:", files.length);
        
        if (files.length && !this.isLoading) {
            this.handleFiles(files);
        } else if (this.isLoading) {
            console.warn("No se pueden procesar archivos: carga en progreso");
            this.showTemporaryMessage("Espera a que termine la carga actual");
        } else {
            console.warn("No se detectaron archivos en el evento drop");
            this.showTemporaryMessage("No se detectaron archivos. Intenta de nuevo.");
        }
    }
    
    /**
     * Muestra un mensaje temporal en la zona de drop
     * @param {string} message - Mensaje a mostrar
     * @param {number} duration - Duración en ms (por defecto 3000)
     */
    showTemporaryMessage(message, duration = 3000) {
        const originalContent = this.dropZone.innerHTML;
        
        this.dropZone.innerHTML = `
            <div class="temp-message">
                <p>${message}</p>
            </div>
        `;
        
        setTimeout(() => {
            this.dropZone.innerHTML = originalContent;
        }, duration);
    }
    
    /**
     * Maneja el cambio en el input de archivo
     * @param {Event} e - Evento de cambio
     */
    handleFileInputChange(e) {
        console.log("Manejando cambio en input de archivo");
        if (e.target.files.length && !this.isLoading) {
            console.log("Procesando archivos seleccionados:", e.target.files);
            this.handleFiles(e.target.files);
            // Limpiar el input para permitir seleccionar el mismo archivo de nuevo
            this.fileInput.value = '';
        } else if (this.isLoading) {
            console.warn("No se pueden procesar archivos: carga en progreso");
            // No es necesario mostrar mensaje aquí ya que no es una acción visible
        } else {
            console.warn("No se seleccionaron archivos");
        }
    }
    
    /**
     * Procesa los archivos seleccionados
     * @param {FileList} files - Lista de archivos
     */
    handleFiles(files) {
        console.log("Procesando archivos", files);
        const file = files[0]; // Por ahora solo procesamos un archivo
        
        console.log("Archivo a procesar:", {
            nombre: file.name,
            tipo: file.type,
            tamaño: `${(file.size / 1024).toFixed(2)} KB`
        });
        
        // Verificar tamaño del archivo
        if (file.size > this.config.maxFileSizeMB * 1024 * 1024) {
            const errorMsg = `El archivo excede el tamaño máximo de ${this.config.maxFileSizeMB}MB.`;
            console.error(errorMsg);
            this.handleError(errorMsg);
            return;
        }
        
        // Verificar tipo de archivo
        if (!this.isValidFile(file)) {
            const errorMsg = 'Formato de archivo no válido. Por favor, selecciona un archivo JSON.';
            console.error(errorMsg, file.type);
            this.handleError(errorMsg);
            return;
        }
        
        // Procesar el archivo
        this.loadFile(file);
    }
    
    /**
     * Verifica si el archivo es válido
     * @param {File} file - Archivo a verificar
     * @returns {boolean} true si el archivo es válido
     */
    isValidFile(file) {
        console.log("Validando archivo:", file.name);
        
        // Verificar por tipo MIME
        const validMime = this.config.allowedTypes.includes(file.type);
        console.log(`Tipo MIME: ${file.type}, válido: ${validMime}`);
        
        // Si el tipo MIME no coincide, verificar por extensión
        const fileName = file.name.toLowerCase();
        const validExt = this.config.allowedExtensions.some(ext => fileName.endsWith(ext));
        console.log(`Verificación por extensión: ${validExt}`);
        
        return validMime || validExt;
    }
    
    /**
     * Carga y procesa el archivo
     * @param {File} file - Archivo a cargar
     */
    loadFile(file) {
        // Actualizar estado
        this.isLoading = true;
        this.lastFileLoaded = file.name;
        
        console.log(`Iniciando carga de archivo: ${file.name}`);
        
        // Emitir evento de inicio de carga
        this.eventEmitter.emit('file-loading', { 
            file: file,
            name: file.name,
            size: file.size,
            type: file.type
        });
        
        // Mostrar indicador de carga
        this.updateLoadingUI(true, file.name);
        
        const reader = new FileReader();
        
        reader.onload = (e) => {
            console.log("Lectura del archivo completada");
            try {
                console.log("Parseando JSON...");
                const jsonData = JSON.parse(e.target.result);
                console.log("JSON parseado correctamente");
                
                // Validación de la estructura del JSON
                if (this.config.validateStructure && !this.isValidJsonStructure(jsonData)) {
                    throw new Error('El archivo JSON no tiene la estructura esperada (nodes y edges).');
                }
                
                console.log("Estructura JSON validada");
                
                // Emitir evento de éxito con los datos
                this.eventEmitter.emit('file-loaded', { 
                    file: file,
                    data: jsonData
                });
                
                console.log("Evento file-loaded emitido");
                
                // Actualizar UI para mostrar éxito
                this.updateLoadingUI(false);
                
            } catch (error) {
                console.error("Error al procesar el archivo JSON:", error);
                this.handleError(`Error al procesar el JSON: ${error.message}`);
            }
            
            // Actualizar estado
            this.isLoading = false;
        };
        
        reader.onerror = (error) => {
            console.error("Error al leer el archivo:", error);
            this.handleError('Error al leer el archivo.');
            this.isLoading = false;
        };
        
        // Leer el archivo como texto
        try {
            console.log("Iniciando lectura del archivo como texto");
            reader.readAsText(file);
        } catch (error) {
            console.error("Error al iniciar la lectura del archivo:", error);
            this.handleError(`Error al leer el archivo: ${error.message}`);
            this.isLoading = false;
        }
    }
    
    /**
     * Maneja errores durante la carga de archivos
     * @param {string} message - Mensaje de error
     */
    handleError(message) {
        console.error(`Error en FileController: ${message}`);
        
        // Actualizar UI para mostrar error
        this.updateLoadingUI(false, null, message);
        
        // Emitir evento de error
        this.eventEmitter.emit('file-error', { 
            message: message
        });
        
        // Actualizar estado
        this.isLoading = false;
    }
    
    /**
     * Actualiza la UI durante la carga
     * @param {boolean} isLoading - Si está cargando
     * @param {string} fileName - Nombre del archivo (opcional)
     * @param {string} errorMessage - Mensaje de error (opcional)
     */
    updateLoadingUI(isLoading, fileName = null, errorMessage = null) {
        console.log("Actualizando UI:", { isLoading, fileName, errorMessage });
        
        if (!this.dropZone) {
            console.error("No se puede actualizar UI: dropZone no disponible");
            return;
        }
        
        // Eliminar todas las clases de estado anteriores
        this.dropZone.classList.remove('loading', 'error', 'success');
        
        if (isLoading) {
            this.dropZone.innerHTML = `
                <div class="loading-indicator">
                    <div class="spinner"></div>
                    <p>Cargando ${fileName || 'archivo'}...</p>
                </div>
            `;
            this.dropZone.classList.add('loading');
        } else if (errorMessage) {
            this.dropZone.innerHTML = `
                <div class="error-message">
                    <p><strong>Error:</strong> ${errorMessage}</p>
                    <p>Arrastra un archivo JSON aquí<br>o haz clic para seleccionar</p>
                </div>
            `;
            this.dropZone.classList.add('error');
            
            // Auto-limpiar el error después de unos segundos
            setTimeout(() => {
                if (this.dropZone.classList.contains('error')) {
                    this.dropZone.innerHTML = `
                        <h2>Arrastra un archivo JSON aquí</h2>
                        <p>O haz clic en "Cargar JSON"</p>
                    `;
                    this.dropZone.classList.remove('error');
                }
            }, 5000);
        } else {
            // Estado normal o éxito
            this.dropZone.innerHTML = `
                <h2>Arrastra un archivo JSON aquí</h2>
                <p>O haz clic en "Cargar JSON"</p>
            `;
            
            // Brevemente mostrar éxito si venimos de una carga
            if (this.lastFileLoaded) {
                this.dropZone.classList.add('success');
                setTimeout(() => {
                    this.dropZone.classList.remove('success');
                }, 2000);
            }
        }
    }
    
    /**
     * Valida la estructura básica del JSON
     * @param {Object} jsonData - Datos JSON a validar
     * @returns {boolean} true si la estructura es válida
     */
    isValidJsonStructure(jsonData) {
        console.log("Validando estructura JSON...");
        
        // Verificación básica: el JSON debe ser un objeto
        if (!jsonData || typeof jsonData !== 'object') {
            console.error("JSON no es un objeto válido");
            return false;
        }
        
        // Verificar que tenga al menos una de las propiedades esperadas
        const hasNodes = Array.isArray(jsonData.nodes);
        const hasEdges = Array.isArray(jsonData.edges);
        
        console.log(`JSON contiene: { nodes: ${hasNodes}, edges: ${hasEdges} }`);
        
        if (!hasNodes && !hasEdges) {
            console.error("JSON no contiene arrays 'nodes' ni 'edges'");
            return false;
        }
        
        // Si hay nodos, verificar que tengan la estructura esperada
        if (hasNodes && jsonData.nodes.length > 0) {
            // Tomar el primer nodo como muestra
            const sampleNode = jsonData.nodes[0];
            
            // Un nodo válido debe tener al menos un ID
            if (!('id' in sampleNode)) {
                console.error("Los nodos no tienen propiedad 'id'");
                return false;
            }
            
            console.log("Estructura de nodos validada");
        }
        
        // Si hay aristas, verificar que tengan la estructura esperada
        if (hasEdges && jsonData.edges.length > 0) {
            // Tomar la primera arista como muestra
            const sampleEdge = jsonData.edges[0];
            
            // Una arista válida debe tener la propiedad "between" con al menos 2 IDs
            if (!Array.isArray(sampleEdge.between) || sampleEdge.between.length < 2) {
                console.error("Las aristas no tienen la propiedad 'between' correcta");
                return false;
            }
            
            console.log("Estructura de aristas validada");
        }
        
        console.log("Estructura JSON válida");
        return true;
    }
    
    /**
     * Carga datos de ejemplo (para demostración)
     * @param {Object} exampleData - Datos de ejemplo
     */
    loadExampleData(exampleData) {
        console.log("Cargando datos de ejemplo");
        
        if (this.isLoading) {
            console.warn("No se pueden cargar datos de ejemplo: carga en progreso");
            return;
        }
        
        // Validar primero los datos de ejemplo
        if (!exampleData || !this.isValidJsonStructure(exampleData)) {
            console.error("Los datos de ejemplo no tienen una estructura válida");
            this.handleError('Los datos de ejemplo no tienen una estructura válida.');
            return;
        }
        
        // Emitir evento con los datos de ejemplo
        this.eventEmitter.emit('file-loaded', { 
            file: null,
            data: exampleData,
            isExample: true
        });
        
        this.lastFileLoaded = "datos_ejemplo.json";
        console.log("Datos de ejemplo cargados correctamente");
    }
}

// Si estamos utilizando módulos ES
export default FileController;