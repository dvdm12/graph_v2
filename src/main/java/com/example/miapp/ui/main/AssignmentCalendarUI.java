package com.example.miapp.ui.main;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import java.awt.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Interfaz gráfica del calendario de asignaciones.
 * Muestra las asignaciones sin conflictos organizadas por día.
 */
public class AssignmentCalendarUI extends JFrame {
    private static final Logger logger = Logger.getLogger(AssignmentCalendarUI.class.getName());
    private static final Color BUTTON_BLUE = new Color(66, 133, 244);
    private static final Color BUTTON_SELECTED = new Color(25, 103, 210);
    
    // Modelo de la tabla
    private final DefaultTableModel tableModel;
    
    // Componentes UI
    private JTable assignmentsTable;
    private JLabel dayLabel;
    private JPanel daysPanel;
    
    // Gestor de asignaciones (lógica de negocio)
    private final AssignmentManager assignmentManager;
    
    /**
     * Constructor del calendario de asignaciones
     */
    public AssignmentCalendarUI() {
        super("Calendario de Asignaciones");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);
        
        // Inicializar gestor de asignaciones
        assignmentManager = new AssignmentManager();
        
        // Inicializar el modelo de tabla
        String[] columns = {"ID", "Grupo", "Materia", "Profesor", "Aula", "Hora Inicio", "Hora Fin", "Tipo", "Estudiantes"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        // Configurar la interfaz de usuario
        initUI();
    }
    
    /**
     * Inicializa la interfaz de usuario
     */
    private void initUI() {
        try {
            // Panel principal
            JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
            mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
            
            // Título
            JLabel titleLabel = new JLabel("Calendario de Asignaciones (Sin Conflictos)", JLabel.CENTER);
            titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
            titleLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
            
            // Panel de días (botones de día)
            daysPanel = createDaysPanel();
            
            // Etiqueta para mostrar el día seleccionado
            dayLabel = new JLabel("Seleccione un día", JLabel.CENTER);
            dayLabel.setFont(new Font("Arial", Font.BOLD, 16));
            dayLabel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));
            dayLabel.setBorder(new EmptyBorder(5, 0, 10, 0));
            
            // Tabla de asignaciones
            assignmentsTable = new JTable(tableModel);
            assignmentsTable.setRowHeight(25);
            assignmentsTable.getTableHeader().setReorderingAllowed(false);
            assignmentsTable.setGridColor(Color.LIGHT_GRAY);
            assignmentsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            assignmentsTable.setFillsViewportHeight(true);
            
            // Configurar colores alternados para las filas
            assignmentsTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    
                    if (!isSelected) {
                        comp.setBackground(row % 2 == 0 ? new Color(240, 245, 255) : Color.WHITE);
                    }
                    
                    return comp;
                }
            });
            
            // Panel superior con título e información
            JPanel topPanel = new JPanel(new BorderLayout());
            topPanel.add(titleLabel, BorderLayout.NORTH);
            topPanel.add(dayLabel, BorderLayout.CENTER);
            
            // Añadir componentes al panel principal
            mainPanel.add(topPanel, BorderLayout.NORTH);
            mainPanel.add(daysPanel, BorderLayout.WEST);
            mainPanel.add(new JScrollPane(assignmentsTable), BorderLayout.CENTER);
            
            // Botón para cerrar
            JButton closeButton = new JButton("Cerrar");
            closeButton.addActionListener(e -> dispose());
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.add(closeButton);
            mainPanel.add(buttonPanel, BorderLayout.SOUTH);
            
            // Establecer el panel principal como contenido
            setContentPane(mainPanel);
            
            // Mostrar lunes por defecto
            showAssignmentsForDay("Monday");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error al inicializar la interfaz", e);
        }
    }
    
    /**
     * Crea el panel de botones para los días de la semana
     */
    private JPanel createDaysPanel() {
        JPanel panel = new JPanel(new GridLayout(6, 1, 5, 10));
        panel.setBorder(new EmptyBorder(0, 0, 0, 10));
        panel.setPreferredSize(new Dimension(180, 0));
        
        // Crear un botón por cada día (excepto domingo)
        for (String day : assignmentManager.getDays()) {
            String dayName = assignmentManager.getDayName(day);
            int count = assignmentManager.getAssignmentCountForDay(day);
            
            // Crear botón con nombre del día y número de asignaciones
            JButton dayButton = new JButton("<html><center>" + dayName + "<br><small>(" + count + " asignaciones)</small></center></html>");
            dayButton.setBackground(BUTTON_BLUE);
            dayButton.setForeground(Color.WHITE);
            dayButton.setBorderPainted(false);
            dayButton.setFocusPainted(false);
            dayButton.setOpaque(true);
            dayButton.setPreferredSize(new Dimension(150, 60));
            dayButton.setName(day); // Guardar el día en el nombre del botón
            
            // Asegurar que el botón mantenga su color
            dayButton.setContentAreaFilled(false);
            dayButton.setOpaque(true);
            
            // Efecto hover
            dayButton.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    if (dayButton.getBackground() != BUTTON_SELECTED) {
                        dayButton.setBackground(new Color(100, 149, 237));
                    }
                }
                
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    if (dayButton.getBackground() != BUTTON_SELECTED) {
                        dayButton.setBackground(BUTTON_BLUE);
                    }
                }
            });
            
            // Acción al hacer clic
            dayButton.addActionListener(e -> {
                // Restablecer color de todos los botones
                for (Component c : panel.getComponents()) {
                    if (c instanceof JButton) {
                        c.setBackground(BUTTON_BLUE);
                    }
                }
                
                // Establecer color para botón seleccionado
                dayButton.setBackground(BUTTON_SELECTED);
                
                // Mostrar asignaciones para el día
                showAssignmentsForDay(day);
            });
            
            panel.add(dayButton);
        }
        
        return panel;
    }
    
    /**
     * Muestra las asignaciones para un día específico
     */
    private void showAssignmentsForDay(String day) {
        try {
            // Actualizar etiqueta del día
            dayLabel.setText("Asignaciones para el " + assignmentManager.getDayName(day));
            
            // Limpiar tabla
            tableModel.setRowCount(0);
            
            // Obtener asignaciones para este día
            List<AssignmentManager.AssignmentInfo> assignments = assignmentManager.getAssignmentsForDay(day);
            
            logger.info("Mostrando " + assignments.size() + " asignaciones para " + day);
            
            // Añadir cada asignación a la tabla
            for (AssignmentManager.AssignmentInfo info : assignments) {
                tableModel.addRow(new Object[]{
                    info.id,
                    info.groupName,
                    info.subjectName + " (" + info.subjectCode + ")",
                    info.professorName,
                    info.roomName + (info.isLab ? " (Lab)" : ""),
                    info.startTime,
                    info.endTime,
                    "D".equals(info.sessionType) ? "Diurno" : "Nocturno",
                    info.enrolledStudents
                });
            }
            
            // Ajustar anchos de columna
            adjustColumnWidths();
            
            // Seleccionar primera fila si hay asignaciones
            if (tableModel.getRowCount() > 0) {
                assignmentsTable.setRowSelectionInterval(0, 0);
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error al mostrar asignaciones para " + day, e);
        }
    }
    
    /**
     * Ajusta el ancho de las columnas según su contenido
     */
    private void adjustColumnWidths() {
        for (int column = 0; column < assignmentsTable.getColumnCount(); column++) {
            int width = 50; // ancho mínimo
            
            // Obtener ancho del encabezado
            int headerWidth = assignmentsTable.getTableHeader().getDefaultRenderer()
                .getTableCellRendererComponent(assignmentsTable, assignmentsTable.getColumnModel().getColumn(column).getHeaderValue(), false, false, 0, column)
                .getPreferredSize().width + 10;
            
            width = Math.max(width, headerWidth);
            
            // Obtener ancho máximo de las celdas
            for (int row = 0; row < assignmentsTable.getRowCount(); row++) {
                TableCellRenderer renderer = assignmentsTable.getCellRenderer(row, column);
                Component comp = assignmentsTable.prepareRenderer(renderer, row, column);
                width = Math.max(width, comp.getPreferredSize().width + 10);
            }
            
            // Establecer el ancho de la columna
            assignmentsTable.getColumnModel().getColumn(column).setPreferredWidth(width);
        }
    }
    
    /**
     * Abre la interfaz del calendario
     */
    public static void showCalendar() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            SwingUtilities.invokeLater(() -> {
                AssignmentCalendarUI calendar = new AssignmentCalendarUI();
                calendar.setVisible(true);
            });
        } catch (Exception e) {
            Logger.getLogger(AssignmentCalendarUI.class.getName())
                  .log(Level.SEVERE, "Error al mostrar el calendario", e);
        }
    }
    
    /**
     * Método principal para pruebas
     */
    public static void main(String[] args) {
        showCalendar();
    }
}