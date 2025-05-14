/**
 * Clase que maneja la visualización del grafo utilizando D3.js
 */
class GraphView {
    /**
     * Constructor de GraphView
     * @param {HTMLElement} container - Contenedor DOM para la visualización
     * @param {EventEmitter} eventEmitter - Instancia de EventEmitter para comunicación
     */
    constructor(container, eventEmitter) {
        // Referencias base
        this.container = container;
        this.eventEmitter = eventEmitter;
        
        // Elementos D3
        this.svg = null;
        this.g = null; // Grupo principal para zoom y pan
        
        // Elementos del grafo
        this.nodeElements = null;
        this.linkElements = null;
        this.selfLoopElements = null;
        this.linkLabelElements = null;
        
        // Simulación física
        this.simulation = null;
        
        // Estado
        this.selectedNodeId = null;
        this.graphData = { nodes: [], links: [] };
        
        // Dimensiones
        this.width = this.container.clientWidth;
        this.height = this.container.clientHeight;
        
        // Configuración
        this.config = {
            nodeRadius: 35,
            selfLoopRadius: 25,
            selfLoopOffset: 70,
            linkStrengthNormal: -800,
            linkDistance: 200,
            collisionRadius: 70,
            minLinkWidth: 2,
            maxLinkWidth: 6,
            minZoom: 0.1,
            maxZoom: 4
        };
        
        // Inicializar
        this.init();
    }
    
    /**
     * Inicializa la visualización SVG y configuraciones
     */
    init() {
        // Crear SVG
        this.svg = d3.select(this.container).append("svg")
            .attr("width", "100%")
            .attr("height", "100%")
            .attr("class", "graph-svg");
        
        // Configurar zoom
        const zoom = d3.zoom()
            .scaleExtent([this.config.minZoom, this.config.maxZoom])
            .on("zoom", (event) => {
                this.g.attr("transform", event.transform);
            });
        
        this.svg.call(zoom);
        
        // Crear grupo principal
        this.g = this.svg.append("g");
        
        // Manejar clic en el fondo para deseleccionar
        this.svg.on("click", (event) => {
            // Solo deseleccionar si se hizo clic en el SVG, no en un nodo
            if (event.target === this.svg.node()) {
                this.eventEmitter.emit('background-clicked');
            }
        });
        
        // Escucha eventos de redimensionamiento
        window.addEventListener('resize', this.handleResize.bind(this));
    }
    
    /**
     * Maneja el evento de redimensionamiento de la ventana
     */
    handleResize() {
        this.width = this.container.clientWidth;
        this.height = this.container.clientHeight;
        
        // Actualizar posición central si hay una simulación activa
        if (this.simulation) {
            this.simulation.force("center", d3.forceCenter(this.width / 2, this.height / 2));
            this.simulation.alpha(0.3).restart();
        }
    }
    
    /**
     * Renderiza el grafo con los datos proporcionados
     * @param {Object} graphData - Datos del grafo con nodos y enlaces
     */
    render(graphData) {
        // Guardar datos
        this.graphData = graphData;
        
        // Limpiar visualización anterior
        this.g.selectAll("*").remove();
        
        // Si no hay datos, salir
        if (!graphData || !graphData.nodes || graphData.nodes.length === 0) {
            return;
        }
        
        // Separar enlaces normales y auto-bucles
        const normalLinks = graphData.links.filter(link => !link.isSelfLoop);
        const selfLoopLinks = graphData.links.filter(link => link.isSelfLoop);
        
        // Crear elementos del grafo en orden de capas (de atrás hacia adelante)
        this.createLinks(normalLinks);
        this.createLinkLabels(normalLinks);
        this.createSelfLoops(selfLoopLinks);
        this.createNodes(graphData.nodes);
        
        // Configurar simulación física
        this.setupSimulation(graphData);
    }
    
    /**
     * Crea los elementos de enlace (líneas entre nodos)
     * @param {Array} links - Enlaces normales (no auto-bucles)
     */
    createLinks(links) {
        // Grupo para enlaces
        const linksGroup = this.g.append("g")
            .attr("class", "links");
        
        // Crear líneas para los enlaces
        this.linkElements = linksGroup.selectAll("line")
            .data(links)
            .enter()
            .append("line")
            .attr("class", "link")
            .attr("stroke", link => this.getConflictColor(link))
            .attr("stroke-width", link => this.calculateLinkWidth(link.count))
            .attr("opacity", 0.8)
            // Línea punteada para múltiples conflictos
            .attr("stroke-dasharray", link => link.count > 1 ? "5,5" : null);
    }
    
    /**
     * Calcula el ancho de una línea según el número de conflictos
     * @param {number} count - Número de conflictos
     * @returns {number} Ancho de línea en píxeles
     */
    calculateLinkWidth(count) {
        // Escalar entre min y max según el número de conflictos
        // Con un mínimo de 1 conflicto y un escalado logarítmico para muchos conflictos
        return Math.max(
            this.config.minLinkWidth,
            Math.min(
                this.config.maxLinkWidth,
                this.config.minLinkWidth + Math.log2(count) * 1.5
            )
        );
    }
    
    /**
     * Crea etiquetas para los enlaces (contadores de conflictos)
     * @param {Array} links - Enlaces normales
     */
    createLinkLabels(links) {
        // Grupo para etiquetas
        const labelsGroup = this.g.append("g")
            .attr("class", "link-labels");
        
        // Crear contenedores de etiquetas
        this.linkLabelElements = labelsGroup.selectAll("g")
            .data(links)
            .enter()
            .append("g")
            .attr("class", "link-label");
        
        // Añadir fondo para el contador (mejora visibilidad)
        this.linkLabelElements.append("circle")
            .attr("r", 14)
            .attr("fill", "white")
            .attr("stroke", d => this.getConflictColor(d))
            .attr("stroke-width", 2);
        
        // Añadir texto del contador
        this.linkLabelElements.append("text")
            .attr("text-anchor", "middle")
            .attr("dy", 5)  // Ajuste para centrar verticalmente
            .attr("font-size", "12px")
            .attr("font-weight", "bold")
            .attr("fill", d => this.getConflictColor(d))
            .text(d => d.count);
    }
    
    /**
     * Crea auto-bucles (conflictos de un nodo consigo mismo)
     * @param {Array} selfLoops - Enlaces auto-bucle
     */
    createSelfLoops(selfLoops) {
        // Grupo para auto-bucles
        const selfLoopsGroup = this.g.append("g")
            .attr("class", "self-loops");
        
        // Crear contenedores para auto-bucles
        this.selfLoopElements = selfLoopsGroup.selectAll("g")
            .data(selfLoops)
            .enter()
            .append("g")
            .attr("class", "self-loop");
        
        // Añadir círculo para el bucle
        this.selfLoopElements.append("circle")
            .attr("class", "self-loop-circle")
            .attr("r", this.config.selfLoopRadius)
            .attr("cy", this.config.selfLoopOffset)
            .attr("stroke", d => this.getConflictColor(d))
            .attr("stroke-width", d => this.calculateLinkWidth(d.count))
            .attr("fill", "none")
            .attr("stroke-dasharray", d => d.count > 1 ? "5,5" : null);
        
        // Añadir un fondo blanco para el contador
        this.selfLoopElements.append("circle")
            .attr("r", 14)
            .attr("cy", this.config.selfLoopOffset)
            .attr("fill", "white")
            .attr("stroke", d => this.getConflictColor(d))
            .attr("stroke-width", 2);
        
        // Añadir texto del contador
        this.selfLoopElements.append("text")
            .attr("text-anchor", "middle")
            .attr("y", this.config.selfLoopOffset)
            .attr("dy", 5)  // Ajuste para centrar verticalmente
            .attr("font-size", "12px")
            .attr("font-weight", "bold")
            .attr("fill", d => this.getConflictColor(d))
            .text(d => d.count);
    }
    
    /**
     * Crea los nodos del grafo
     * @param {Array} nodes - Nodos a visualizar
     */
    createNodes(nodes) {
        // Grupo para nodos
        const nodesGroup = this.g.append("g")
            .attr("class", "nodes");
        
        // Crear contenedores para nodos
        this.nodeElements = nodesGroup.selectAll("g")
            .data(nodes)
            .enter()
            .append("g")
            .attr("class", "node")
            .call(d3.drag()
                .on("start", this.dragstarted.bind(this))
                .on("drag", this.dragged.bind(this))
                .on("end", this.dragended.bind(this)))
            .on("click", (event, d) => {
                event.stopPropagation();
                this.handleNodeClick(event, d);
            });
        
        // Añadir círculo principal
        this.nodeElements.append("circle")
            .attr("class", "node-circle")
            .attr("r", this.config.nodeRadius)
            .attr("fill", this.getNodeColor.bind(this))
            .attr("stroke", "white")
            .attr("stroke-width", 2);
        
        // Añadir código/nombre de grupo en el centro
        this.nodeElements.append("text")
            .attr("class", "node-label")
            .attr("dy", 5)
            .attr("text-anchor", "middle")
            .attr("fill", "white")
            .text(d => d.groupName || d.id);
        
        // Añadir panel de información debajo del nodo
        const nodeInfoPanels = this.nodeElements.append("g")
            .attr("class", "node-info")
            .attr("transform", "translate(0, 45)");
        
        // Fondo del panel de información
        nodeInfoPanels.append("foreignObject")
            .attr("width", 120)
            .attr("height", 65)
            .attr("x", -60)
            .attr("y", 0)
            .html(d => `
                <div class="info-panel">
                    <div class="info-header">${this.truncateText(d.groupName, 15)}</div>
                    <div class="info-text">Profesor: ${this.truncateText(d.professorName, 18)}</div>
                    <div class="info-text">Hora: ${d.startTime || ""}-${d.endTime || ""}</div>
                </div>
            `);
    }
    
    /**
     * Configura la simulación física para posicionar los nodos
     * @param {Object} graphData - Datos del grafo
     */
    setupSimulation(graphData) {
        // Detener simulación anterior si existe
        if (this.simulation) {
            this.simulation.stop();
        }
        
        // Crear nueva simulación
        this.simulation = d3.forceSimulation(graphData.nodes)
            .force("link", d3.forceLink(graphData.links)
                .id(d => d.id)
                .distance(this.config.linkDistance))
            .force("charge", d3.forceManyBody().strength(this.config.linkStrengthNormal))
            .force("center", d3.forceCenter(this.width / 2, this.height / 2))
            .force("collision", d3.forceCollide().radius(this.config.collisionRadius)); // Evitar solapamiento
        
        // Actualizar posiciones en cada tick
        this.simulation.on("tick", this.updatePositions.bind(this));
    }
    
    /**
     * Actualiza las posiciones de todos los elementos en cada tick de la simulación
     */
    updatePositions() {
        // Actualizar enlaces
        if (this.linkElements) {
            this.linkElements
                .attr("x1", d => d.source.x)
                .attr("y1", d => d.source.y)
                .attr("x2", d => d.target.x)
                .attr("y2", d => d.target.y);
        }
        
        // Actualizar etiquetas de enlaces
        if (this.linkLabelElements) {
            this.linkLabelElements.attr("transform", d => {
                // Calcular punto medio entre nodos
                const x = (d.source.x + d.target.x) / 2;
                const y = (d.source.y + d.target.y) / 2;
                
                // Añadir un pequeño desplazamiento para evitar que las etiquetas
                // se superpongan exactamente con la línea
                const dx = d.target.x - d.source.x;
                const dy = d.target.y - d.source.y;
                const len = Math.sqrt(dx * dx + dy * dy);
                
                // Solo desplazar si los nodos no están en la misma posición
                if (len > 0) {
                    // Desplazamiento perpendicular a la línea
                    const offsetX = -dy * 10 / len;
                    const offsetY = dx * 10 / len;
                    
                    return `translate(${x + offsetX},${y + offsetY})`;
                }
                
                return `translate(${x},${y})`;
            });
        }
        
        // Actualizar auto-bucles
        if (this.selfLoopElements) {
            this.selfLoopElements.attr("transform", d => 
                `translate(${d.source.x},${d.source.y})`
            );
        }
        
        // Actualizar nodos
        if (this.nodeElements) {
            this.nodeElements.attr("transform", d => 
                `translate(${d.x},${d.y})`
            );
        }
    }
    
    /**
     * Evento de inicio de arrastre
     * @param {Event} event - Evento D3
     * @param {Object} d - Nodo
     */
    dragstarted(event, d) {
        if (!event.active) this.simulation.alphaTarget(0.3).restart();
        d.fx = d.x;
        d.fy = d.y;
    }
    
    /**
     * Evento de arrastre
     * @param {Event} event - Evento D3
     * @param {Object} d - Nodo
     */
    dragged(event, d) {
        d.fx = event.x;
        d.fy = event.y;
    }
    
    /**
     * Evento de fin de arrastre
     * @param {Event} event - Evento D3
     * @param {Object} d - Nodo
     */
    dragended(event, d) {
        if (!event.active) this.simulation.alphaTarget(0);
        d.fx = null;
        d.fy = null;
    }
    
    /**
     * Maneja el clic en un nodo
     * @param {Event} event - Evento del navegador
     * @param {Object} d - Nodo seleccionado
     */
    handleNodeClick(event, d) {
        // Emitir evento de nodo seleccionado
        this.eventEmitter.emit('node-clicked', d.id);
        
        // Actualizar estado interno
        this.selectedNodeId = d.id;
        
        // Aplicar resaltado visual
        this.highlightNode(d.id);
        
        // Prevenir propagación para evitar deselección
        event.stopPropagation();
    }
    
    /**
     * Resalta un nodo y sus conexiones
     * @param {string} nodeId - ID del nodo a resaltar
     */
    highlightNode(nodeId) {
        // Deseleccionar nodo anterior
        this.resetHighlights();
        
        // Resaltar nodo seleccionado
        this.nodeElements.filter(n => n.id === nodeId)
            .select(".node-circle")
            .attr("stroke", "#ffc107")
            .attr("stroke-width", 4);
        
        // Resaltar enlaces conectados
        this.linkElements
            .style("opacity", l => (l.source.id === nodeId || l.target.id === nodeId) ? 1 : 0.2)
            .style("stroke-width", l => {
                const baseWidth = this.calculateLinkWidth(l.count);
                return (l.source.id === nodeId || l.target.id === nodeId) ? 
                    baseWidth * 1.5 : baseWidth * 0.5;
            });
            
        // Resaltar etiquetas de enlaces
        this.linkLabelElements
            .style("opacity", l => (l.source.id === nodeId || l.target.id === nodeId) ? 1 : 0.2);
        
        // Resaltar auto-bucles
        if (this.selfLoopElements) {
            this.selfLoopElements
                .style("opacity", l => (l.source.id === nodeId) ? 1 : 0.2)
                .select(".self-loop-circle")
                .style("stroke-width", l => {
                    const baseWidth = this.calculateLinkWidth(l.count);
                    return (l.source.id === nodeId) ? baseWidth * 1.5 : baseWidth * 0.5;
                });
        }
    }
    
    /**
     * Restablece todos los resaltados visuales
     */
    resetHighlights() {
        // Restablecer nodos
        this.nodeElements.select(".node-circle")
            .attr("stroke", "white")
            .attr("stroke-width", 2);
        
        // Restablecer enlaces
        this.linkElements
            .style("opacity", 0.8)
            .style("stroke-width", d => this.calculateLinkWidth(d.count));
        
        // Restablecer etiquetas
        this.linkLabelElements
            .style("opacity", 1);
        
        // Restablecer auto-bucles
        if (this.selfLoopElements) {
            this.selfLoopElements
                .style("opacity", 1)
                .select(".self-loop-circle")
                .style("stroke-width", d => this.calculateLinkWidth(d.count));
        }
        
        // Limpiar estado
        this.selectedNodeId = null;
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
     * @param {Object} link - Enlace/conflicto
     * @returns {string} Color en formato hexadecimal
     */
    getConflictColor(link) {
        if (!link || !Array.isArray(link.types) || link.types.length === 0) {
            return "#9e9e9e"; // Gris por defecto
        }
        
        // Definir prioridad de colores según tipo
        if (link.types.some(type => type.toLowerCase().includes("franja bloqueada")))
            return "#9c27b0"; // Púrpura
        if (link.types.some(type => type.toLowerCase().includes("mismo profesor")))
            return "#e53935"; // Rojo
        if (link.types.some(type => type.toLowerCase().includes("misma sala")))
            return "#ffb300"; // Amarillo
        if (link.types.some(type => type.toLowerCase().includes("mismo grupo")))
            return "#43a047"; // Verde
        if (link.types.some(type => type.toLowerCase().includes("jornada") || type.toLowerCase().includes("horario")))
            return "#26a69a"; // Verde azulado
        
        return "#9e9e9e"; // Gris para tipos desconocidos
    }
    
    /**
     * Trunca un texto si es demasiado largo
     * @param {string} text - Texto a truncar
     * @param {number} maxLength - Longitud máxima
     * @returns {string} Texto truncado
     */
    truncateText(text, maxLength) {
        if (!text) return "";
        return text.length > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
    
    /**
     * Centra el grafo en la vista
     */
    centerGraph() {
        const svgRect = this.svg.node().getBoundingClientRect();
        const zoom = d3.zoom().scaleExtent([0.1, 4]);
        
        this.svg.transition().duration(750).call(
            zoom.transform, 
            d3.zoomIdentity
                .translate(svgRect.width / 2, svgRect.height / 2)
                .scale(0.9)
        );
    }
    
    /**
     * Hace zoom sobre un nodo específico
     * @param {string} nodeId - ID del nodo
     */
    zoomToNode(nodeId) {
        const node = this.graphData.nodes.find(n => n.id === nodeId);
        if (!node || !node.x || !node.y) return;
        
        const zoom = d3.zoom().scaleExtent([0.1, 4]);
        
        this.svg.transition().duration(750).call(
            zoom.transform, 
            d3.zoomIdentity
                .translate(this.width / 2, this.height / 2)
                .scale(1.5)
                .translate(-node.x, -node.y)
        );
    }
}

// Si estamos utilizando módulos ES
export default GraphView;