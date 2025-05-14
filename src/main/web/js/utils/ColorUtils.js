/**
 * Clase de utilidades para manejar colores en la aplicación
 */
class ColorUtils {
    /**
     * Obtiene el color para un nodo basado en su profesorId
     * @param {Object} node - Objeto de nodo
     * @returns {string} Código de color en formato hexadecimal
     */
    static getNodeColor(node) {
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
     * @param {Object} conflict - Objeto de conflicto
     * @returns {string} Código de color en formato hexadecimal
     */
    static getConflictColor(conflict) {
        if (!conflict) return "#9e9e9e"; // Gris por defecto
        
        const types = conflict.types || [];
        
        // Priorizar tipos de conflicto
        if (types.some(type => type.toLowerCase().includes("mismo profesor")))
            return "#e53935"; // Rojo
        if (types.some(type => type.toLowerCase().includes("misma sala")))
            return "#ffb300"; // Amarillo
        if (types.some(type => type.toLowerCase().includes("jornada") || 
                               type.toLowerCase().includes("horario")))
            return "#43a047"; // Verde
        if (types.some(type => type.toLowerCase().includes("bloqueada")))
            return "#9c27b0"; // Púrpura
        
        return "#9e9e9e"; // Gris para tipos desconocidos
    }
    
    /**
     * Obtiene una versión más clara del color para hover o estados activos
     * @param {string} hexColor - Color en formato hexadecimal
     * @param {number} percent - Porcentaje de luminosidad a añadir (0-100)
     * @returns {string} Nuevo color con luminosidad ajustada
     */
    static lightenColor(hexColor, percent = 20) {
        // Convertir hex a RGB
        let r = parseInt(hexColor.slice(1, 3), 16);
        let g = parseInt(hexColor.slice(3, 5), 16);
        let b = parseInt(hexColor.slice(5, 7), 16);
        
        // Aumentar cada componente según el porcentaje
        r = Math.min(255, r + Math.floor(percent / 100 * (255 - r)));
        g = Math.min(255, g + Math.floor(percent / 100 * (255 - g)));
        b = Math.min(255, b + Math.floor(percent / 100 * (255 - b)));
        
        // Convertir de nuevo a hex
        return `#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}`;
    }
    
    /**
     * Determina si un color es oscuro para elegir texto blanco o negro
     * @param {string} hexColor - Color en formato hexadecimal
     * @returns {boolean} true si el color es oscuro
     */
    static isColorDark(hexColor) {
        const r = parseInt(hexColor.slice(1, 3), 16);
        const g = parseInt(hexColor.slice(3, 5), 16);
        const b = parseInt(hexColor.slice(5, 7), 16);
        
        // Fórmula para calcular brillo percibido
        const brightness = (r * 299 + g * 587 + b * 114) / 1000;
        return brightness < 128;
    }
    
    /**
     * Obtiene el color de texto apropiado (blanco o negro) según el fondo
     * @param {string} backgroundColor - Color de fondo en formato hexadecimal
     * @returns {string} Color de texto (#ffffff o #000000)
     */
    static getTextColorForBackground(backgroundColor) {
        return this.isColorDark(backgroundColor) ? "#ffffff" : "#000000";
    }
}

// Si estamos utilizando módulos ES
export default ColorUtils;