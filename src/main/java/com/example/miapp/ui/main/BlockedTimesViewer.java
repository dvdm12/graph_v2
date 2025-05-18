package com.example.miapp.ui.main;

import com.example.miapp.domain.BlockedSlot;
import com.example.miapp.domain.Professor;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Ventana para visualizar los horarios bloqueados de un profesor.
 * Muestra tanto una tabla detallada como una representación visual por día y hora.
 */
public class BlockedTimesViewer extends JDialog {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(BlockedTimesViewer.class.getName());
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    
    private final Professor professor;
    private JTable detailTable;
    private JPanel calendarPanel;
    
    /**
     * Constructor que inicializa la ventana con la información del profesor.
     * 
     * @param parent Ventana padre
     * @param professor Profesor cuyos horarios bloqueados se visualizarán
     */
    public BlockedTimesViewer(Frame parent, Professor professor) {
        super(parent, "Horarios Bloqueados - " + professor.getName(), true);
        this.professor = professor;
        
        initializeUI();
        populateData();
        
        // Configurar ventana
        setSize(800, 600);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        
        logger.info("Visualizador de horarios bloqueados abierto para profesor id=" + professor.getId());
    }
    
    /**
     * Inicializa la interfaz de usuario.
     */
    private void initializeUI() {
        // Panel principal dividido en dos partes
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerLocation(200);
        
        // Panel superior: tabla detallada
        JPanel topPanel = createDetailedTablePanel();
        
        // Panel inferior: visualización tipo calendario
        JPanel bottomPanel = createCalendarPanel();
        
        // Añadir paneles al split
        splitPane.setTopComponent(topPanel);
        splitPane.setBottomComponent(bottomPanel);
        
        // Añadir a la ventana
        getContentPane().add(splitPane, BorderLayout.CENTER);
        
        // Panel de botones en la parte inferior
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton closeButton = new JButton("Cerrar");
        closeButton.addActionListener(e -> dispose());
        buttonPanel.add(closeButton);
        
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    }
    
    /**
     * Crea el panel con la tabla detallada de horarios bloqueados.
     */
    private JPanel createDetailedTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Detalle de Horarios Bloqueados"));
        
        // Modelo de tabla
        String[] columnNames = {"Día", "Hora Inicio", "Hora Fin", "Duración"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        detailTable = new JTable(model);
        detailTable.setRowHeight(25);
        detailTable.getTableHeader().setBackground(new Color(70, 130, 180));
        detailTable.getTableHeader().setForeground(Color.WHITE);
        detailTable.getTableHeader().setFont(detailTable.getTableHeader().getFont().deriveFont(Font.BOLD));
        
        // Alternar colores de fila
        detailTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, 
                                                         boolean isSelected, boolean hasFocus, 
                                                         int row, int column) {
                Component component = super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                
                if (!isSelected) {
                    component.setBackground(row % 2 == 0 ? new Color(240, 248, 255) : Color.WHITE);
                }
                
                return component;
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(detailTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Crea el panel con la visualización tipo calendario de los horarios bloqueados.
     */
    private JPanel createCalendarPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Visualización de Horarios Bloqueados"));
        
        // Panel para la cuadrícula de horarios
        calendarPanel = new JPanel(new GridBagLayout());
        calendarPanel.setBackground(Color.WHITE);
        
        JScrollPane scrollPane = new JScrollPane(calendarPanel);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Llena los datos del profesor en los componentes de la UI.
     */
    private void populateData() {
        // Obtener los horarios bloqueados
        List<BlockedSlot> blockedSlots = professor.getBlockedSlots();
        
        // Si no hay horarios bloqueados, mostrar mensaje
        if (blockedSlots.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "El profesor " + professor.getName() + " no tiene horarios bloqueados.",
                "Sin Horarios Bloqueados", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // Llenar la tabla de detalles
        DefaultTableModel model = (DefaultTableModel) detailTable.getModel();
        model.setRowCount(0); // Limpiar filas existentes
        
        for (BlockedSlot slot : blockedSlots) {
            String day = slot.getDay();
            String startTime = slot.getStartTime().format(TIME_FORMATTER);
            String endTime = slot.getEndTime().format(TIME_FORMATTER);
            String duration = slot.getDurationMinutes() + " minutos";
            
            model.addRow(new Object[]{day, startTime, endTime, duration});
        }
        
        // Crear visualización de calendario
        createCalendarView(blockedSlots);
    }
    
    /**
     * Crea la visualización tipo calendario con los horarios bloqueados.
     * 
     * @param blockedSlots Lista de horarios bloqueados
     */
    private void createCalendarView(List<BlockedSlot> blockedSlots) {
        calendarPanel.removeAll();
        
        // Definir días y horarios a mostrar
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        
        // Horas de 7:00 a 22:00 en intervalos de 1 hora
        String[] hours = new String[16];
        for (int i = 0; i < hours.length; i++) {
            hours[i] = String.format("%02d:00", i + 7);
        }
        
        // Configurar GridBagConstraints
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(1, 1, 1, 1);
        
        // Agrupar los horarios bloqueados por día
        Map<String, List<BlockedSlot>> slotsByDay = new HashMap<>();
        for (BlockedSlot slot : blockedSlots) {
            slotsByDay.computeIfAbsent(slot.getDay(), k -> new ArrayList<>()).add(slot);
        }
        
        // Añadir etiqueta vacía en la esquina superior izquierda
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel cornerLabel = new JLabel("Hora / Día");
        cornerLabel.setFont(cornerLabel.getFont().deriveFont(Font.BOLD));
        cornerLabel.setBackground(new Color(220, 220, 220));
        cornerLabel.setOpaque(true);
        cornerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        cornerLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        calendarPanel.add(cornerLabel, gbc);
        
        // Añadir etiquetas de días en la primera fila
        for (int i = 0; i < days.length; i++) {
            gbc.gridx = i + 1;
            gbc.gridy = 0;
            
            JLabel dayLabel = new JLabel(days[i]);
            dayLabel.setFont(dayLabel.getFont().deriveFont(Font.BOLD));
            dayLabel.setBackground(new Color(70, 130, 180));
            dayLabel.setForeground(Color.WHITE);
            dayLabel.setOpaque(true);
            dayLabel.setHorizontalAlignment(SwingConstants.CENTER);
            dayLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            calendarPanel.add(dayLabel, gbc);
        }
        
        // Añadir etiquetas de horas en la primera columna
        for (int i = 0; i < hours.length; i++) {
            gbc.gridx = 0;
            gbc.gridy = i + 1;
            
            JLabel hourLabel = new JLabel(hours[i]);
            hourLabel.setFont(hourLabel.getFont().deriveFont(Font.BOLD));
            hourLabel.setBackground(new Color(220, 220, 220));
            hourLabel.setOpaque(true);
            hourLabel.setHorizontalAlignment(SwingConstants.CENTER);
            hourLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            calendarPanel.add(hourLabel, gbc);
        }
        
        // Añadir celdas del calendario
        for (int dayIndex = 0; dayIndex < days.length; dayIndex++) {
            String day = days[dayIndex];
            
            for (int hourIndex = 0; hourIndex < hours.length; hourIndex++) {
                gbc.gridx = dayIndex + 1;
                gbc.gridy = hourIndex + 1;
                
                // Verificar si esta celda está bloqueada
                boolean isBlocked = false;
                BlockedSlot matchingSlot = null;
                
                // Obtener los horarios para este día
                List<BlockedSlot> daySlots = slotsByDay.get(day);
                if (daySlots != null) {
                    // Hora actual en formato 24h (7 + hourIndex)
                    int currentHour = 7 + hourIndex;
                    
                    // Verificar cada horario bloqueado
                    for (BlockedSlot slot : daySlots) {
                        int startHour = slot.getStartTime().getHour();
                        int endHour = slot.getEndTime().getHour();
                        
                        // Si la hora actual está en el rango (inclusive), está bloqueada
                        if (currentHour >= startHour && currentHour < endHour) {
                            isBlocked = true;
                            matchingSlot = slot;
                            break;
                        }
                        
                        // Casos especiales para horas exactas con minutos
                        if (currentHour == endHour && slot.getEndTime().getMinute() > 0) {
                            isBlocked = true;
                            matchingSlot = slot;
                            break;
                        }
                    }
                }
                
                // Crear la celda con el color apropiado
                JPanel cell = new JPanel();
                if (isBlocked) {
                    cell.setBackground(new Color(255, 200, 200)); // Rojo claro para bloqueado
                    cell.setBorder(BorderFactory.createLineBorder(new Color(180, 0, 0)));
                    
                    // Añadir tooltip con información detallada
                    if (matchingSlot != null) {
                        cell.setToolTipText(
                            matchingSlot.getDay() + ": " + 
                            matchingSlot.getStartTime().format(TIME_FORMATTER) + " - " + 
                            matchingSlot.getEndTime().format(TIME_FORMATTER)
                        );
                    }
                } else {
                    cell.setBackground(new Color(240, 255, 240)); // Verde claro para disponible
                    cell.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                }
                
                cell.setPreferredSize(new Dimension(30, 30));
                calendarPanel.add(cell, gbc);
            }
        }
        
        // Actualizar el panel
        calendarPanel.revalidate();
        calendarPanel.repaint();
    }
}