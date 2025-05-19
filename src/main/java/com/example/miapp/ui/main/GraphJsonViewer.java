package com.example.miapp.ui.main;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.plaf.basic.BasicButtonUI;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Interfaz para visualizar el contenido de graph.json y abrir index.html en Firefox.
 */
public class GraphJsonViewer extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(GraphJsonViewer.class.getName());

    // Paleta de colores moderna
    private static final Color PRIMARY_COLOR = new Color(63, 81, 181);   // Indigo
    private static final Color ACCENT_COLOR = new Color(255, 167, 38);    // Amber
    private static final Color BACKGROUND_COLOR = new Color(243, 243, 243); // Light Gray
    private static final Color TEXT_COLOR_PRIMARY = new Color(33, 33, 33);   // Dark Gray
    private static final Color TEXT_COLOR_SECONDARY = new Color(117, 117, 117); // Medium Gray
    private static final Color BORDER_COLOR = new Color(204, 204, 204); // Light Gray Border

    // Componentes UI
    private JTextArea jsonTextArea;
    private JButton openBrowserButton;
    private JButton refreshButton;
    private JLabel statusLabel;

    // Ruta absoluta al archivo graph.json
    private final String graphJsonPath = "C:\\graph_project\\graph_two\\graph\\src\\main\\java\\com\\example\\miapp\\ui\\main\\graph_data\\graph.json";

    /**
     * Constructor de la interfaz.
     */
    public GraphJsonViewer() {
        super("Visualizador de Grafo de Conflictos");
        setLookAndFeel();
        initializeUI();
        loadJsonContent();
    }

    /**
     * Establece un Look and Feel moderno para la aplicación.
     */
    private void setLookAndFeel() {
        try {
            // Usar el Look and Feel del sistema para asegurar consistencia
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            logger.log(Level.WARNING, "No se pudo establecer el Look & Feel", e);
        }
    }

    /**
     * Inicializa los componentes de la interfaz de usuario.
     */
    private void initializeUI() {
        // Configuración básica de la ventana
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1000, 800); // Un tamaño ligeramente mayor
        setLocationRelativeTo(null);

        // Panel principal con BorderLayout y márgenes amplios
        JPanel mainPanel = new JPanel(new BorderLayout(20, 20));
        mainPanel.setBorder(new EmptyBorder(30, 30, 30, 30));
        mainPanel.setBackground(BACKGROUND_COLOR);

        // Título más prominente con un margen inferior
        JLabel titleLabel = new JLabel("Visualizador de Grafo de Conflictos", JLabel.CENTER);
        titleLabel.setFont(new Font("Roboto", Font.BOLD, 26)); // Fuente moderna y más grande
        titleLabel.setForeground(TEXT_COLOR_PRIMARY);
        titleLabel.setBorder(new EmptyBorder(0, 0, 30, 0));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Área de texto para mostrar el JSON con barra de desplazamiento
        jsonTextArea = new JTextArea();
        jsonTextArea.setEditable(false);
        jsonTextArea.setFont(new Font("Consolas", Font.PLAIN, 16)); // Fuente monoespaciada popular para código
        jsonTextArea.setBackground(Color.WHITE);
        jsonTextArea.setForeground(TEXT_COLOR_PRIMARY);
        jsonTextArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                new EmptyBorder(15, 15, 15, 15)));

        JScrollPane scrollPane = new JScrollPane(jsonTextArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setBorder(BorderFactory.createEmptyBorder()); // Sin borde alrededor del scrollPane
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Panel inferior
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(BACKGROUND_COLOR);
        
        // Etiqueta de estado
        statusLabel = new JLabel("Listo");
        statusLabel.setFont(new Font("Roboto", Font.ITALIC, 14));
        statusLabel.setForeground(TEXT_COLOR_SECONDARY);
        statusLabel.setBorder(new EmptyBorder(5, 0, 0, 0));
        bottomPanel.add(statusLabel, BorderLayout.WEST);
        
        // Panel de botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 15)); // Más espacio
        buttonPanel.setBackground(BACKGROUND_COLOR);

        // Botón para refrescar el contenido JSON
        refreshButton = createStyledButton("Refrescar JSON", ACCENT_COLOR); // Color de acento
        refreshButton.addActionListener(e -> {
            statusLabel.setText("Recargando JSON...");
            SwingUtilities.invokeLater(() -> {
                loadJsonContent();
                statusLabel.setText("JSON recargado a las " + 
                        java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
            });
        });

        // Botón para abrir index.html en Firefox
        openBrowserButton = createStyledButton("Abrir en Navegador", PRIMARY_COLOR); // Color primario
        openBrowserButton.addActionListener(e -> {
            statusLabel.setText("Abriendo en navegador...");
            SwingUtilities.invokeLater(() -> {
                openIndexHtmlInFirefox();
            });
        });

        buttonPanel.add(refreshButton);
        buttonPanel.add(openBrowserButton);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);
        
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        // Agregar el panel principal a la ventana
        setContentPane(mainPanel);
    }

    /**
     * Crea un botón con un estilo visual mejorado (Material Design like).
     * @param text Texto del botón.
     * @param backgroundColor Color de fondo del botón.
     * @return El botón estilizado.
     */
    private JButton createStyledButton(String text, Color backgroundColor) {
        JButton button = new JButton(text);
        button.setBackground(backgroundColor);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Roboto", Font.BOLD, 16)); // Fuente moderna
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(12, 25, 12, 25));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);  // Importante para mantener el color de fondo
        button.setContentAreaFilled(false);  // Personalizar la apariencia
        
        // Crear un UI personalizado
        button.setUI(new BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Dibujar el fondo del botón
                JButton b = (JButton) c;
                if (b.getModel().isPressed()) {
                    g2.setColor(backgroundColor.darker());
                } else {
                    g2.setColor(backgroundColor);
                }
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 10, 10);
                
                // Dibujar el texto
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                int textWidth = fm.stringWidth(b.getText());
                int textHeight = fm.getHeight();
                int x = (c.getWidth() - textWidth) / 2;
                int y = (c.getHeight() + textHeight) / 2 - fm.getDescent();
                g2.drawString(b.getText(), x, y);
                
                g2.dispose();
            }
        });

        // Efecto hover sutil
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(backgroundColor.darker());
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(backgroundColor);
            }
        });
        return button;
    }

    /**
     * Carga el contenido del archivo graph.json y lo muestra en el área de texto.
     * Esta versión usa la ruta absoluta para garantizar que se cargue el archivo actualizado.
     */
    private void loadJsonContent() {
        try {
            // Usar la ruta absoluta directamente para asegurar que se cargue el archivo actualizado
            File file = new File(graphJsonPath);
            
            if (!file.exists()) {
                throw new FileNotFoundException("No se pudo encontrar el archivo graph.json en: " + graphJsonPath);
            }
            
            logger.info("Cargando graph.json desde: " + graphJsonPath);
            
            // Leer el contenido del archivo usando Files para asegurar que se lea la versión más reciente
            String jsonContent = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            
            // Formatear el JSON para mejor visualización
            String formattedJson = formatJson(jsonContent);

            // Mostrar en el área de texto
            jsonTextArea.setText(formattedJson);
            // Posicionar al inicio del documento
            jsonTextArea.setCaretPosition(0);

            logger.info("Archivo graph.json cargado correctamente desde: " + file.getAbsolutePath());
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error al cargar el archivo graph.json", e);
            jsonTextArea.setText("Error al cargar el archivo graph.json:\n" + e.getMessage());

            // Mostrar mensaje de error con un diseño más agradable
            JOptionPane.showMessageDialog(
                    this,
                    "<html><body style='width: 350px; font-family: \"Roboto\", sans-serif;'>" +
                            "<h4 style='color: #D32F2F;'>Error al cargar el archivo JSON:</h4>" +
                            "<p>" + e.getMessage() + "</p></body></html>",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    /**
     * Abre el archivo index.html en el navegador Firefox.
     */
    private void openIndexHtmlInFirefox() {
        try {
            // Buscar index.html en la raíz del proyecto
            Path indexPath = findIndexHtmlFile();

            if (indexPath == null) {
                throw new FileNotFoundException("No se pudo encontrar el archivo index.html");
            }

            // Comando para abrir Firefox con el archivo HTML
            String[] command;

            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                // Windows
                command = new String[]{
                        "cmd", "/c", "start", "\"\"", "firefox", indexPath.toAbsolutePath().toString()
                };
            } else if (os.contains("mac")) {
                // macOS
                command = new String[]{
                        "open", "-a", "Firefox", indexPath.toAbsolutePath().toString()
                };
            } else if (os.contains("nix") || os.contains("nux")) {
                // Linux y otros Unix
                command = new String[]{"firefox", indexPath.toAbsolutePath().toString()};
            } else {
                // Otro sistema operativo
                throw new UnsupportedOperationException("El sistema operativo no es compatible con la apertura automática del navegador.");
            }

            // Ejecutar el comando
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            // Iniciar el proceso y capturar su referencia (aunque no la usemos después)
            processBuilder.start();

            // Registrar que se ha abierto el navegador
            logger.info("Abriendo index.html en Firefox: " + indexPath.toAbsolutePath());
            statusLabel.setText("Visualizador abierto en Firefox");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error al abrir index.html en Firefox", e);
            statusLabel.setText("Error al abrir el navegador");

            // Mostrar mensaje de error con un diseño más agradable
            JOptionPane.showMessageDialog(
                    this,
                    "<html><body style='width: 400px; font-family: \"Roboto\", sans-serif;'>" +
                            "<h4 style='color: #D32F2F;'>Error al abrir el visualizador en el navegador:</h4>" +
                            "<p>" + e.getMessage() + "</p>" +
                            "<p style='color: " + TEXT_COLOR_SECONDARY.toString().substring(14, 23) + ";'>Asegúrese de que Firefox esté instalado y que index.html exista en la ubicación correcta.</p>" +
                            "</body></html>",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    /**
     * Busca el archivo index.html en varias ubicaciones posibles.
     */
    private Path findIndexHtmlFile() {
        // Posibles ubicaciones para el archivo
        Path[] possiblePaths = {
                Paths.get("index.html"),
                Paths.get("src/main/resources/index.html"),
                Paths.get("resources/index.html"),
                Paths.get("src/index.html"),
                Paths.get("src/main/webapp/index.html"),
                Paths.get("webapp/index.html"),
                Paths.get("web/index.html")
        };

        // Verificar cada ubicación
        for (Path path : possiblePaths) {
            if (Files.exists(path)) {
                logger.info("Archivo index.html encontrado en: " + path.toAbsolutePath());
                return path;
            }
        }
        return null;
    }

    /**
     * Formatea una cadena JSON para que sea más legible.
     */
    private String formatJson(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);

            // Parsear y formatear el JSON
            JsonNode jsonNode = mapper.readTree(json);
            return mapper.writeValueAsString(jsonNode);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error al formatear JSON", e);
            // Si hay un error de formato, devolver el JSON original
            return json;
        }
    }

    /**
     * Método principal para probar la interfaz.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GraphJsonViewer viewer = new GraphJsonViewer();
            viewer.setVisible(true);
        });
    }
}