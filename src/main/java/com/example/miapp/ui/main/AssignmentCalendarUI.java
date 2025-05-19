package com.example.miapp.ui.main;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
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
    private static final Color COLOR_PRIMARIO = new Color(63, 81, 181);   // Un azul más moderno (Indigo 500 de Material Design)
    private static final Color COLOR_ACCENT = new Color(255, 167, 38);   // Un naranja llamativo (Amber 500 de Material Design)
    private static final Color COLOR_TEXTO_PRIMARIO = Color.DARK_GRAY;
    private static final Color COLOR_FONDO_FILA_PAR = new Color(242, 242, 242); // Gris claro para filas pares
    private static final Font FUENTE_TITULO = new Font("Roboto", Font.BOLD, 22);
    private static final Font FUENTE_SUBTITULO = new Font("Roboto", Font.BOLD, 16);
    private static final Font FUENTE_TEXTO = new Font("Roboto", Font.PLAIN, 14);
    private static final EmptyBorder ESPACIO_MEDIANO = new EmptyBorder(10, 10, 10, 10);
    private static final EmptyBorder ESPACIO_GRANDE = new EmptyBorder(15, 15, 15, 15);
    private static final Dimension DIMENSION_BOTON_DIA = new Dimension(160, 50);

    // Modelo de la tabla
    private final DefaultTableModel tableModel;

    // Componentes UI
    private JTable assignmentsTable;
    private JLabel dayLabel;
    private JPanel daysPanel;

    // Gestor de asignaciones (lógica de negocio)
    private final AssignmentManager assignmentManager;
    private JButton selectedDayButton; // Para mantener el botón de día seleccionado

    /**
     * Constructor del calendario de asignaciones
     */
    public AssignmentCalendarUI() {
        super("Calendario de Asignaciones");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1150, 750); // Un poco más grande
        setLocationRelativeTo(null);

        // Inicializar gestor de asignaciones
        assignmentManager = new AssignmentManager();

        // Inicializar el modelo de tabla
        String[] columns = {"ID", "Grupo", "Materia", "Profesor", "Aula", "Inicio", "Fin", "Tipo", "Estudiantes"};
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
            mainPanel.setBorder(ESPACIO_GRANDE);
            mainPanel.setBackground(Color.WHITE); // Fondo blanco

            // Título
            JLabel titleLabel = new JLabel("Calendario de Asignaciones", JLabel.CENTER);
            titleLabel.setFont(FUENTE_TITULO);
            titleLabel.setForeground(COLOR_PRIMARIO);
            titleLabel.setBorder(new EmptyBorder(0, 0, 20, 0)); // Más espacio debajo del título

            // Panel de días (botones de día)
            daysPanel = createDaysPanel();
            daysPanel.setBackground(Color.WHITE);

            // Etiqueta para mostrar el día seleccionado
            dayLabel = new JLabel("Seleccione un día", JLabel.CENTER);
            dayLabel.setFont(FUENTE_SUBTITULO);
            dayLabel.setForeground(COLOR_TEXTO_PRIMARIO);
            dayLabel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
            dayLabel.setBorder(ESPACIO_MEDIANO);

            // Tabla de asignaciones
            assignmentsTable = new JTable(tableModel);
            assignmentsTable.setRowHeight(30); // Filas un poco más altas
            assignmentsTable.getTableHeader().setReorderingAllowed(false);
            assignmentsTable.setGridColor(new Color(224, 224, 224)); // Gris más suave para las líneas de la tabla
            assignmentsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            assignmentsTable.setFillsViewportHeight(true);
            assignmentsTable.setFont(FUENTE_TEXTO);
            assignmentsTable.getTableHeader().setFont(FUENTE_SUBTITULO);

            // Renderizador de celdas personalizado para colores alternados
            assignmentsTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    if (!isSelected) {
                        comp.setBackground(row % 2 == 0 ? COLOR_FONDO_FILA_PAR : Color.WHITE);
                    } else {
                        comp.setBackground(COLOR_ACCENT.brighter()); // Color de selección más suave
                        comp.setForeground(Color.WHITE);
                    }
                    comp.setForeground(COLOR_TEXTO_PRIMARIO);
                    return comp;
                }
            });

            // Panel superior con título e información
            JPanel topPanel = new JPanel(new BorderLayout());
            topPanel.setBackground(Color.WHITE);
            topPanel.add(titleLabel, BorderLayout.NORTH);
            topPanel.add(dayLabel, BorderLayout.CENTER);

            // Añadir componentes al panel principal
            mainPanel.add(topPanel, BorderLayout.NORTH);
            mainPanel.add(daysPanel, BorderLayout.WEST);
            mainPanel.add(new JScrollPane(assignmentsTable), BorderLayout.CENTER);

            // Botón para cerrar
            JButton closeButton = new JButton("Cerrar");
            closeButton.setForeground(Color.WHITE);
            closeButton.setBackground(COLOR_PRIMARIO);
            closeButton.setFont(FUENTE_TEXTO);
            closeButton.setFocusPainted(false);
            closeButton.setBorder(new EmptyBorder(8, 15, 8, 15));
            closeButton.addActionListener(e -> dispose());
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.setBackground(Color.WHITE);
            buttonPanel.add(closeButton);
            mainPanel.add(buttonPanel, BorderLayout.SOUTH);

            // Establecer el panel principal como contenido
            setContentPane(mainPanel);

            // Mostrar lunes por defecto y seleccionar el botón
            showAssignmentsForDay("Monday");
            for (Component c : daysPanel.getComponents()) {
                if (c instanceof JButton && c.getName().equals("Monday")) {
                    selectedDayButton = (JButton) c;
                    selectedDayButton.setBackground(COLOR_ACCENT);
                    break;
                }
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error al inicializar la interfaz", e);
        }
    }

    /**
     * Crea el panel de botones para los días de la semana
     */
    private JPanel createDaysPanel() {
        JPanel panel = new JPanel(new GridLayout(7, 1, 5, 10)); // Incluimos el espacio para "Domingo" aunque no haya botón
        panel.setBorder(new EmptyBorder(0, 0, 0, 15)); // Más espacio a la derecha
        panel.setPreferredSize(new Dimension(180, 0));
        panel.setBackground(Color.WHITE);

        // Crear un botón por cada día
        for (String day : assignmentManager.getDays()) {
            String dayName = assignmentManager.getDayName(day);
            int count = assignmentManager.getAssignmentCountForDay(day);

            // Crear botón con nombre del día y número de asignaciones
            JButton dayButton = new JButton("<html><center>" + dayName + "<br><small>(" + count + " asignaciones)</small></center></html>");
            dayButton.setBackground(COLOR_PRIMARIO);
            dayButton.setForeground(Color.WHITE);
            dayButton.setBorderPainted(false);
            dayButton.setFocusPainted(false);
            dayButton.setOpaque(true);
            dayButton.setPreferredSize(DIMENSION_BOTON_DIA);
            dayButton.setName(day); // Guardar el día en el nombre del botón
            dayButton.setFont(FUENTE_TEXTO);

            // Efecto hover
            dayButton.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    if (dayButton != selectedDayButton) {
                        dayButton.setBackground(COLOR_PRIMARIO.brighter());
                    }
                }

                public void mouseExited(java.awt.event.MouseEvent evt) {
                    if (dayButton != selectedDayButton) {
                        dayButton.setBackground(COLOR_PRIMARIO);
                    }
                }
            });

            // Acción al hacer clic
            dayButton.addActionListener(e -> {
                // Deseleccionar el botón previamente seleccionado
                if (selectedDayButton != null) {
                    selectedDayButton.setBackground(COLOR_PRIMARIO);
                }
                // Seleccionar el botón actual
                selectedDayButton = (JButton) e.getSource();
                selectedDayButton.setBackground(COLOR_ACCENT);

                // Mostrar asignaciones para el día
                showAssignmentsForDay(dayButton.getName());
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
            int width = 80; // Ancho mínimo un poco mayor

            // Obtener ancho del encabezado
            TableCellRenderer headerRenderer = assignmentsTable.getTableHeader().getDefaultRenderer();
            Component headerComp = headerRenderer.getTableCellRendererComponent(
                    assignmentsTable, assignmentsTable.getColumnModel().getColumn(column).getHeaderValue(), false, false, 0, column);
            width = Math.max(width, headerComp.getPreferredSize().width + 15); // Añadir un poco de padding

            // Obtener ancho máximo de las celdas
            for (int row = 0; row < assignmentsTable.getRowCount(); row++) {
                TableCellRenderer cellRenderer = assignmentsTable.getCellRenderer(row, column);
                Component cellComp = assignmentsTable.prepareRenderer(cellRenderer, row, column);
                width = Math.max(width, cellComp.getPreferredSize().width + 15); // Añadir un poco de padding
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