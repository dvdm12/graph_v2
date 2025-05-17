package com.example.miapp.ui;

import com.example.miapp.domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Interfaz para visualizar asignaciones en formato de tabla de datos avanzada.
 * Permite ordenación, filtrado, exportación y visualización detallada.
 */
public class AssignmentDataTableViewer extends JDialog {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(AssignmentDataTableViewer.class.getName());
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    
    // Asignaciones a visualizar
    private final List<Assignment> assignments;
    
    // Componentes de la interfaz
    private JTable dataTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;
    private JTextArea detailsTextArea;
    
    // Datos para el detalle
    private ObjectMapper objectMapper;
    
    /**
     * Constructor.
     * 
     * @param parent Ventana padre
     * @param assignments Lista de asignaciones a visualizar
     */
    public AssignmentDataTableViewer(Frame parent, List<Assignment> assignments) {
        super(parent, "Visualizador de Asignaciones", true);
        this.assignments = new ArrayList<>(assignments);
        
        // Inicializar mapper para detalles JSON
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        // Configurar ventana
        setSize(1200, 700);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        
        // Inicializar componentes
        initializeUI();
        
        // Cargar datos
        loadDataToTable();
        
        logger.info("Visualizador de asignaciones iniciado con " + assignments.size() + " registros");
    }
    

    
    
/**
 * 1) Inicializa la UI en el orden correcto:
 *    - Primero la tabla (y con ella el sorter)
 *    - Luego el panel superior de filtros
 *    - Luego el panel de detalles
 */
private void initializeUI() {
    // Panel principal
    JPanel mainPanel = new JPanel(new BorderLayout(10,10));
    mainPanel.setBorder(new EmptyBorder(10,10,10,10));

    // (A) Creamos la tabla y su sorter
    JPanel centerPanel = createTablePanel();

    // (B) Ahora el panel de filtro/búsqueda
    JPanel topPanel = createTopPanel();

    // (C) El panel de detalles
    JPanel bottomPanel = createDetailsPanel();

    // SplitPane y ensamblaje final
    JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, centerPanel, bottomPanel);
    split.setResizeWeight(0.7);
    mainPanel.add(topPanel,   BorderLayout.NORTH);
    mainPanel.add(split,      BorderLayout.CENTER);

    setContentPane(mainPanel);
}

    /**
 * Crea el panel superior sólo con los botones de acción:
 * Exportar a CSV, Imprimir y Cerrar.
 */
private JPanel createTopPanel() {
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
    panel.setBorder(new EmptyBorder(0, 0, 10, 0));

    Color blueBg = new Color(70, 130, 180);

    JButton exportBtn = new JButton("Exportar a CSV");
    JButton printBtn  = new JButton("Imprimir");
    JButton closeBtn  = new JButton("Cerrar");

    for (JButton b : Arrays.asList(exportBtn, printBtn, closeBtn)) {
        b.setBackground(blueBg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        panel.add(b);
    }

    exportBtn.addActionListener(e -> exportToCsv());
    printBtn.addActionListener(e -> printTable());
    closeBtn.addActionListener(e -> dispose());

    return panel;
}


    
    /**
     * Crea el panel central con la tabla de datos.
     */
    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Crear modelo de tabla
        String[] columnNames = {
            "ID", "Fecha", "Día", "Hora Inicio", "Hora Fin", 
            "Profesor", "ID Profesor", "Departamento",
            "Materia", "Créditos", "¿Lab?",
            "Aula", "Capacidad", "¿Es Lab?",
            "Grupo", "Tipo Sesión", "Estudiantes"
        };
        
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Tabla no editable
            }
            
            @Override
            public Class<?> getColumnClass(int column) {
                switch (column) {
                    case 0: case 6: case 9: case 16: 
                        return Integer.class; // Columnas numéricas
                    case 10: case 13: 
                        return Boolean.class; // Columnas booleanas
                    default: 
                        return String.class;
                }
            }
        };
        
        // Crear tabla
        dataTable = new JTable(tableModel);
        sorter = new TableRowSorter<>(tableModel);
        dataTable.setRowSorter(sorter);
        
        // Configurar apariencia de la tabla
        configureTableAppearance();
        
        // Añadir listener de selección para mostrar detalles
        dataTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showDetailsForSelectedRow();
            }
        });
        
        // Añadir tabla a un scrollpane
        JScrollPane scrollPane = new JScrollPane(dataTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
 * Configura la apariencia visual de la tabla, incluyendo estilos de cabecera,
 * colores alternos de filas y renderizadores específicos.
 */
private void configureTableAppearance() {
    // Colores base
    final Color blueBg  = new Color(70, 130, 180);
    final Color whiteFg = Color.WHITE;

    // 1) RENDERER PERSONALIZADO PARA LA CABECERA
    DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
    headerRenderer.setHorizontalAlignment(JLabel.CENTER);
    headerRenderer.setOpaque(true);
    headerRenderer.setBackground(blueBg);
    headerRenderer.setForeground(whiteFg);
    headerRenderer.setFont(headerRenderer.getFont().deriveFont(Font.BOLD));

    // Aplicamos el renderer al header completo
    JTableHeader header = dataTable.getTableHeader();
    header.setDefaultRenderer(headerRenderer);
    header.setReorderingAllowed(false);  // opcional: evita mover columnas
    header.setResizingAllowed(true);

    // 2) ALTURA DE FILAS
    dataTable.setRowHeight(24);

    // 3) RENDERER GENÉRICO PARA ALTERNAR COLOR DE FILAS
    dataTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                c.setBackground(row % 2 == 0
                        ? new Color(240, 248, 255)
                        : Color.WHITE);
            }
            return c;
        }
    });

    // 4) RENDERER PARA BOOLEANOS
    dataTable.setDefaultRenderer(Boolean.class, new DefaultTableCellRenderer() {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            JCheckBox checkBox = new JCheckBox();
            checkBox.setSelected(Boolean.TRUE.equals(value));
            checkBox.setHorizontalAlignment(JLabel.CENTER);
            if (isSelected) {
                checkBox.setBackground(table.getSelectionBackground());
                checkBox.setForeground(table.getSelectionForeground());
            } else {
                checkBox.setBackground(row % 2 == 0
                        ? new Color(240, 248, 255)
                        : Color.WHITE);
                checkBox.setForeground(table.getForeground());
            }
            return checkBox;
        }
    });

    // 5) AJUSTE DE ANCHOS PREFERIDOS
    int[] preferredWidths = {40, 80, 60, 60, 60, 100, 40, 80, 100, 40, 40, 80, 50, 40, 60, 40, 50};
    TableColumnModel columnModel = dataTable.getColumnModel();
    for (int i = 0; i < preferredWidths.length; i++) {
        columnModel.getColumn(i).setPreferredWidth(preferredWidths[i]);
    }

    // 6) ORDENACIÓN Y SELECCIÓN
    dataTable.setAutoCreateRowSorter(true);
    dataTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    dataTable.setColumnSelectionAllowed(false);
    dataTable.setRowSelectionAllowed(true);
}

    
    /**
     * Crea el panel inferior para mostrar detalles de la asignación seleccionada.
     */
    private JPanel createDetailsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Detalles de Asignación"));
        
        // Área de texto para detalles
        detailsTextArea = new JTextArea();
        detailsTextArea.setEditable(false);
        detailsTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        detailsTextArea.setBackground(new Color(250, 250, 250));
        
        // Añadir a scroll pane
        JScrollPane scrollPane = new JScrollPane(detailsTextArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Carga las asignaciones en la tabla.
     */
    private void loadDataToTable() {
        // Limpiar tabla
        tableModel.setRowCount(0);
        
        // Añadir cada asignación como una fila
        for (Assignment assignment : assignments) {
            Professor professor = assignment.getProfessor();
            Room room = assignment.getRoom();
            Subject subject = assignment.getSubject();
            
            Object[] rowData = {
                // Datos básicos de asignación
                assignment.getId(),
                assignment.getAssignmentDate().toString(),
                assignment.getDay(),
                assignment.getStartTime().format(TIME_FORMATTER),
                assignment.getEndTime().format(TIME_FORMATTER),
                
                // Datos de profesor
                professor.getName(),
                professor.getId(),
                professor.getDepartment(),
                
                // Datos de materia
                subject != null ? subject.getCode() + " - " + subject.getName() : "N/A",
                subject != null ? subject.getCredits() : 0,
                subject != null ? subject.requiresLab() : false,
                
                // Datos de aula
                room.getName(),
                room.getCapacity(),
                room.isLab(),
                
                // Datos de grupo
                assignment.getGroupName() + " (" + assignment.getGroupId() + ")",
                assignment.getSessionType(),
                assignment.getEnrolledStudents()
            };
            
            tableModel.addRow(rowData);
        }
        
        // Actualizar contador de filas
        setTitle("Visualizador de Asignaciones - " + assignments.size() + " registros");
    }
    

    
    /**
     * Muestra los detalles de la asignación seleccionada.
     */
    private void showDetailsForSelectedRow() {
        int selectedRow = dataTable.getSelectedRow();
        
        if (selectedRow >= 0) {
            // Convertir índice de vista a índice de modelo (por el filtro)
            int modelRow = dataTable.convertRowIndexToModel(selectedRow);
            
            // Obtener ID de la asignación
            int assignmentId = (Integer) tableModel.getValueAt(modelRow, 0);
            
            // Buscar la asignación correspondiente
            Assignment selectedAssignment = null;
            for (Assignment a : assignments) {
                if (a.getId() == assignmentId) {
                    selectedAssignment = a;
                    break;
                }
            }
            
            if (selectedAssignment != null) {
                try {
                    // Convertir a JSON formateado para visualización
                    String json = objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(selectedAssignment);
                    
                    // Mostrar en el área de detalles
                    detailsTextArea.setText(json);
                    detailsTextArea.setCaretPosition(0);
                    
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, "Error al generar detalles: " + ex.getMessage(), ex);
                    detailsTextArea.setText("Error al generar detalles: " + ex.getMessage());
                }
            }
        } else {
            // Si no hay selección, limpiar detalles
            detailsTextArea.setText("");
        }
    }
    
    /**
     * Exporta la tabla a un archivo CSV.
     */
    private void exportToCsv() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this,
                    "No hay datos para exportar",
                    "Exportación CSV",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Exportar a CSV");
        fileChooser.setSelectedFile(new File("asignaciones.csv"));
        
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            String path = file.getAbsolutePath();
            if (!path.toLowerCase().endsWith(".csv")) {
                path += ".csv";
                file = new File(path);
            }
            
            try (FileWriter writer = new FileWriter(file)) {
                // Primero las cabeceras
                for (int i = 0; i < tableModel.getColumnCount(); i++) {
                    writer.append('"').append(tableModel.getColumnName(i)).append('"');
                    if (i < tableModel.getColumnCount() - 1) {
                        writer.append(',');
                    }
                }
                writer.append('\n');
                
                // Luego todas las filas visibles (respetando filtros)
                for (int i = 0; i < dataTable.getRowCount(); i++) {
                    for (int j = 0; j < tableModel.getColumnCount(); j++) {
                        // Convertir índice de vista a modelo
                        int modelRow = dataTable.convertRowIndexToModel(i);
                        Object value = tableModel.getValueAt(modelRow, j);
                        
                        if (value != null) {
                            String cellValue = value.toString();
                            // Escapar comillas y caracteres especiales
                            if (cellValue.contains("\"") || cellValue.contains(",")) {
                                cellValue = cellValue.replace("\"", "\"\"");
                                writer.append('"').append(cellValue).append('"');
                            } else {
                                writer.append(cellValue);
                            }
                        }
                        
                        if (j < tableModel.getColumnCount() - 1) {
                            writer.append(',');
                        }
                    }
                    writer.append('\n');
                }
                
                JOptionPane.showMessageDialog(this,
                        "Datos exportados correctamente a:\n" + path,
                        "Exportación Exitosa",
                        JOptionPane.INFORMATION_MESSAGE);
                
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Error al exportar CSV: " + ex.getMessage(), ex);
                JOptionPane.showMessageDialog(this,
                        "Error al exportar datos: " + ex.getMessage(),
                        "Error de Exportación",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * Imprime la tabla de datos.
     */
    private void printTable() {
        try {
            if (!dataTable.print()) {
                JOptionPane.showMessageDialog(this,
                        "La impresión fue cancelada por el usuario",
                        "Impresión Cancelada",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error al imprimir tabla: " + ex.getMessage(), ex);
            JOptionPane.showMessageDialog(this,
                    "Error al imprimir: " + ex.getMessage(),
                    "Error de Impresión",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}