package com.example.miapp.ui;

import com.example.miapp.domain.*;
import com.example.miapp.exception.DomainException;
import com.example.miapp.repository.DataManager;
import com.example.miapp.util.AssignmentBuilder;
import com.example.miapp.service.AssignmentJsonService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Interfaz gráfica para la gestión de asignaciones académicas (Assignment).
 * Permite crear y gestionar asignaciones con profesores existentes, visualizar 
 * la representación JSON en tiempo real, así como importar/exportar data.json
 * y generar graph.json con análisis de conflictos.
 */
public class AssignmentManagerUI extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(AssignmentManagerUI.class.getName());
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // Gestor de datos y servicios
    private final DataManager dataManager;
    private final ObjectMapper objectMapper;
    private final AssignmentJsonService jsonService;
    private final AtomicInteger idCounter;

    // Lista de asignaciones creadas y estado de edición
    private final List<Assignment> createdAssignments = new ArrayList<>();
    private boolean jsonModified = false;
    
    // Componentes de la interfaz
    private JComboBox<Professor> professorCombo;
    private JComboBox<Subject> subjectCombo;
    private JComboBox<Room> roomCombo;
    private JComboBox<String> dayCombo;
    private JComboBox<GroupInfo> groupCombo;
    private List<GroupInfo> groupOptions = new ArrayList<>();
    private JSpinner startTimeSpinner;
    private JSpinner endTimeSpinner;
    private JComboBox<String> sessionTypeCombo;
    private JSpinner enrolledStudentsSpinner;
    private JTextArea jsonTextArea;
    private JTable assignmentsTable;
    private DefaultTableModel tableModel;
    private JButton createButton;
    private JButton removeButton;
    private JButton clearButton;
    private JComboBox<TimeSlot.TimeRange> validRangesCombo;
    private JButton viewProfessorSubjectsButton;
    private JButton validateConflictsButton;

    /**
     * Clase auxiliar para almacenar información de grupo
     */
    private static class GroupInfo {
        private final int id;
        private final String name;
        
        public GroupInfo(int id, String name) {
            this.id = id;
            this.name = name;
        }
        
        public int getId() { return id; }
        public String getName() { return name; }
        
        @Override
        public String toString() {
            return name + " (ID: " + id + ")";
        }
    }
    
    /**
     * Constructor de la interfaz de gestión de asignaciones.
     */
    public AssignmentManagerUI() {
        super("Gestor de Asignaciones Académicas");
        
        // Inicialización de componentes centrales
        dataManager = DataManager.getInstance();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        jsonService = new AssignmentJsonService();

        // Calcular el siguiente ID disponible
        int nextId = dataManager.getAllAssignments().stream()
                .mapToInt(Assignment::getId)
                .max()
                .orElse(0) + 1;
        idCounter = new AtomicInteger(nextId);

        // Configuración de la ventana
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 800);
        setLocationRelativeTo(null);

        // Inicialización de la interfaz
        initializeUI();
        
        logger.info("Interfaz de gestión de asignaciones iniciada. Próximo ID: " + nextId);
    }

    /**
     * Inicializa todos los componentes de la interfaz.
     */
    private void initializeUI() {
        // Panel principal con división
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setDividerLocation(700);
        
        // Panel izquierdo (panel de importación, formulario y tabla)
        JPanel leftPanel = new JPanel(new BorderLayout());
        
        // Panel de formulario y tabla en layout vertical
        JSplitPane formTableSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        formTableSplitPane.setDividerLocation(400);
        formTableSplitPane.setTopComponent(createFormPanel());
        formTableSplitPane.setBottomComponent(createTablePanel());
        
        // Añadir panel de importación/exportación en la parte superior
        leftPanel.add(createImportExportPanel(), BorderLayout.NORTH);
        leftPanel.add(formTableSplitPane, BorderLayout.CENTER);
        
        // Panel derecho (JSON)
        JPanel rightPanel = createJsonPanel();
        
        // Añadir paneles al split principal
        mainSplitPane.setLeftComponent(leftPanel);
        mainSplitPane.setRightComponent(rightPanel);
        
        // Añadir a la ventana
        getContentPane().add(mainSplitPane);
        
        // Configurar eventos
        setupEventListeners();
        
        // Configurar menú contextual para el área JSON
        setupJsonContextMenu();
        
        // Inicializar JSON vacío
        updateJsonView();
    }

    /**
     * Método para generar opciones de grupos
     */
    private void generateGroupOptions() {
        groupOptions.clear();
        
        // Generar 15 grupos predefinidos con letras (A-O)
        for (int i = 0; i < 15; i++) {
            int id = 100 + i;
            String name = "Grupo " + (char)('A' + i);
            groupOptions.add(new GroupInfo(id, name));
        }
        
        // Generar 10 grupos numéricos
        for (int i = 1; i <= 10; i++) {
            int id = 200 + i;
            String name = "Grupo " + i;
            groupOptions.add(new GroupInfo(id, name));
        }
        
        // Actualizar ComboBox
        if (groupCombo != null) {
            groupCombo.removeAllItems();
            for (GroupInfo group : groupOptions) {
                groupCombo.addItem(group);
            }
        }
    }

    private JPanel createImportExportPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(new CompoundBorder(
                new EmptyBorder(5, 10, 5, 10),
                new TitledBorder(BorderFactory.createLineBorder(new Color(46, 139, 87), 2), 
                        "Operaciones de Archivos")
        ));
        panel.setBackground(new Color(240, 255, 240));
        panel.setLayout(new FlowLayout(FlowLayout.LEFT));
    
        // Botones existentes
        JButton importButton = createStyledButton("Importar data.json", new Color(46, 139, 87));
        JButton exportButton = createStyledButton("Exportar a data.json", new Color(46, 139, 87));
        JButton generateGraphButton = createStyledButton("Generar graph.json", new Color(70, 130, 180));
    
        // Nuevo botón para visualizar en tabla
        JButton viewDataTableButton = createStyledButton("Ver Asignaciones en Tabla", new Color(148, 0, 211));
    
        // Añadir botones al panel
        panel.add(importButton);
        panel.add(exportButton);
        panel.add(generateGraphButton);
        panel.add(viewDataTableButton);
    
        // Configurar eventos
        importButton.addActionListener(e -> importDataJson());
        exportButton.addActionListener(e -> exportCurrentJson());
        generateGraphButton.addActionListener(e -> generateGraphJson());
        viewDataTableButton.addActionListener(e -> openDataTableView());
    
        return panel;
    }

    /**
     * Abre una nueva ventana con una visualización tabular de las asignaciones en formato DataTable.
     */
    private void openDataTableView() {
        try {
            // Si no hay asignaciones, mostrar mensaje
            if (createdAssignments.isEmpty()) {
                showInfo("No hay asignaciones para visualizar.\nImporte datos o cree asignaciones primero.");
                return;
            }

            // Crear y mostrar la nueva interfaz
            SwingUtilities.invokeLater(() -> {
                AssignmentDataTableViewer viewer = new AssignmentDataTableViewer(this, createdAssignments);
                viewer.setVisible(true);
            });
            
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error al abrir visualización de tabla: " + ex.getMessage(), ex);
            showError("Error al abrir visualización de tabla:\n" + ex.getMessage());
        }
    }

    /**
     * Crea el panel de formulario para la entrada de datos.
     */
    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new CompoundBorder(
                new EmptyBorder(10, 10, 10, 10),
                new TitledBorder(BorderFactory.createLineBorder(new Color(70, 130, 180), 2), 
                        "Creación de Asignación")
        ));
        panel.setBackground(new Color(240, 248, 255));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        int row = 0;

        // Inicializar componentes del formulario
        professorCombo = new JComboBox<>(dataManager.getAllProfessors().toArray(new Professor[0]));
        professorCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, 
                                                         boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Professor) {
                    Professor prof = (Professor) value;
                    setText(prof.getId() + " - " + prof.getName());
                }
                return this;
            }
        });

        subjectCombo = new JComboBox<>(dataManager.getAllSubjects().toArray(new Subject[0]));
        subjectCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, 
                                                         boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Subject) {
                    Subject subj = (Subject) value;
                    setText(subj.getCode() + " - " + subj.getName());
                }
                return this;
            }
        });

        roomCombo = new JComboBox<>(dataManager.getAllRooms().toArray(new Room[0]));
        roomCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, 
                                                         boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Room) {
                    Room room = (Room) value;
                    setText(room.getId() + " - " + room.getName() + 
                           (room.isLab() ? " (Lab)" : "") + 
                           " [Cap: " + room.getCapacity() + "]");
                }
                return this;
            }
        });

        dayCombo = new JComboBox<>(new String[]{"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"});
        
        validRangesCombo = new JComboBox<>();
        
        // Crear y configurar el combo de grupos
        generateGroupOptions();
        groupCombo = new JComboBox<>();
        for (GroupInfo group : groupOptions) {
            groupCombo.addItem(group);
        }
        
        updateValidTimeRanges();
        
        // Spinners para horas
        startTimeSpinner = createTimeSpinner(8, 0);
        endTimeSpinner = createTimeSpinner(10, 0);
        
        sessionTypeCombo = new JComboBox<>(new String[]{"D", "N"});
        enrolledStudentsSpinner = new JSpinner(new SpinnerNumberModel(20, 1, 100, 1));

        // Botones de acción
        createButton = createStyledButton("Crear Asignación", new Color(70, 130, 180));
        clearButton = createStyledButton("Limpiar Formulario", new Color(180, 180, 180));
        
        // Crear un panel para contener el combo de profesor y el botón
        JPanel professorPanel = new JPanel(new BorderLayout(5, 0));
        professorPanel.setBackground(panel.getBackground());
        professorPanel.add(professorCombo, BorderLayout.CENTER);

        // Crear botón de información
        viewProfessorSubjectsButton = new JButton("?");
        viewProfessorSubjectsButton.setToolTipText("Ver materias habilitadas");
        viewProfessorSubjectsButton.setBackground(new Color(70, 130, 180));
        viewProfessorSubjectsButton.setForeground(Color.WHITE);
        viewProfessorSubjectsButton.setFont(viewProfessorSubjectsButton.getFont().deriveFont(Font.BOLD));
        viewProfessorSubjectsButton.setFocusPainted(false);
        viewProfessorSubjectsButton.setPreferredSize(new Dimension(25, 25));
        viewProfessorSubjectsButton.addActionListener(e -> showProfessorSubjects());
        professorPanel.add(viewProfessorSubjectsButton, BorderLayout.EAST);
        
        // Añadir componentes al panel
        addFormField(panel, gbc, row++, "Profesor:", professorPanel);
        addFormField(panel, gbc, row++, "Materia:", subjectCombo);
        addFormField(panel, gbc, row++, "Aula:", roomCombo);
        addFormField(panel, gbc, row++, "Día:", dayCombo);
        addFormField(panel, gbc, row++, "Franjas Válidas:", validRangesCombo);
        addFormField(panel, gbc, row++, "Hora Inicio:", startTimeSpinner);
        addFormField(panel, gbc, row++, "Hora Fin:", endTimeSpinner);
        addFormField(panel, gbc, row++, "Grupo:", groupCombo);
        addFormField(panel, gbc, row++, "Tipo Sesión:", sessionTypeCombo);
        addFormField(panel, gbc, row++, "Estudiantes:", enrolledStudentsSpinner);

        // Panel de botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(new Color(240, 248, 255));
        buttonPanel.add(createButton);
        buttonPanel.add(clearButton);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(buttonPanel, gbc);

        return panel;
    }

    /**
     * Crea el panel de tabla para mostrar las asignaciones creadas.
     */
    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new CompoundBorder(
                new EmptyBorder(10, 10, 10, 10),
                new TitledBorder(BorderFactory.createLineBorder(new Color(70, 130, 180), 2), 
                        "Asignaciones Creadas")
        ));
        panel.setBackground(new Color(240, 248, 255));

        // Modelo de la tabla
        String[] columnNames = {"ID", "Profesor", "Materia", "Aula", "Día", "Horario", "Grupo", "Estudiantes"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Hacer la tabla no editable
            }
        };
        
        assignmentsTable = new JTable(tableModel);
        assignmentsTable.setFillsViewportHeight(true);
        assignmentsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Personalizar la apariencia de la tabla
        assignmentsTable.setRowHeight(25);
        assignmentsTable.getTableHeader().setBackground(new Color(70, 130, 180));
        assignmentsTable.getTableHeader().setForeground(Color.WHITE);
        assignmentsTable.getTableHeader().setFont(assignmentsTable.getTableHeader().getFont().deriveFont(Font.BOLD));
        
        // Alternar colores de fila
        assignmentsTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
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

        // Añadir tabla a scroll pane
        JScrollPane scrollPane = new JScrollPane(assignmentsTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Panel de botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(new Color(240, 248, 255));
        removeButton = createStyledButton("Eliminar Seleccionada", new Color(220, 20, 60));
        buttonPanel.add(removeButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Crea el panel JSON para visualizar y editar la representación JSON de las asignaciones.
     */
    private JPanel createJsonPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new CompoundBorder(
                new EmptyBorder(10, 10, 10, 10),
                new TitledBorder(BorderFactory.createLineBorder(new Color(46, 139, 87), 2), 
                        "Representación JSON")
        ));
        panel.setBackground(new Color(240, 255, 240));

        // Hacer que el área de texto sea editable
        jsonTextArea = new JTextArea();
        jsonTextArea.setEditable(true);
        jsonTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        jsonTextArea.setBackground(new Color(250, 255, 250));
        
        JScrollPane scrollPane = new JScrollPane(jsonTextArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Panel de botones para operaciones JSON
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(new Color(240, 255, 240));
        
        // Botón para formatear el JSON (pretty-print)
        JButton formatButton = createStyledButton("Formatear JSON", new Color(70, 130, 180));
        formatButton.addActionListener(e -> formatJson());
        
        // Botón para validar el JSON
        JButton validateButton = createStyledButton("Validar JSON", new Color(70, 130, 180));
        validateButton.addActionListener(e -> validateJson());
        
        // Botón para aplicar cambios manuales
        JButton applyButton = createStyledButton("Aplicar Cambios", new Color(46, 139, 87));
        applyButton.addActionListener(e -> applyJsonChanges());
        
        // Botón para validar conflictos
        validateConflictsButton = createStyledButton("Validar Conflictos", new Color(148, 0, 211));
        validateConflictsButton.addActionListener(e -> validateConflicts());
        validateConflictsButton.setEnabled(!createdAssignments.isEmpty());
        
        buttonPanel.add(formatButton);
        buttonPanel.add(validateButton);
        buttonPanel.add(applyButton);
        buttonPanel.add(validateConflictsButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Configura todos los eventos y listeners de la interfaz.
     */
    private void setupEventListeners() {
        // Actualizar franjas válidas cuando cambia el día
        dayCombo.addActionListener(e -> updateValidTimeRanges());
        
        // Actualizar horas cuando cambia la franja válida seleccionada
        validRangesCombo.addActionListener(e -> {
            TimeSlot.TimeRange selectedRange = (TimeSlot.TimeRange) validRangesCombo.getSelectedItem();
            if (selectedRange != null) {
                startTimeSpinner.setValue(timeToDate(selectedRange.getStart()));
                endTimeSpinner.setValue(timeToDate(selectedRange.getEnd()));
            }
        });
        
        // Verificar compatibilidad cuando cambia la materia o el aula
        subjectCombo.addActionListener(e -> checkCompatibility());
        roomCombo.addActionListener(e -> checkCompatibility());
        
        // Verificar autorizaciones cuando cambia el profesor o la materia
        professorCombo.addActionListener(e -> checkAuthorization());
        subjectCombo.addActionListener(e -> checkAuthorization());
        
        // Acción para crear una asignación
        createButton.addActionListener(e -> createAssignment());
        
        // Acción para limpiar el formulario
        clearButton.addActionListener(e -> clearForm());
        
        // Acción para eliminar una asignación
        removeButton.addActionListener(e -> removeSelectedAssignment());
        
        // Listener para la selección en la tabla
        assignmentsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                removeButton.setEnabled(assignmentsTable.getSelectedRow() != -1);
            }
        });
        
        // Inicialmente deshabilitar el botón de eliminar
        removeButton.setEnabled(false);
        
        // Agregar listener para detectar cambios en el área JSON
        jsonTextArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                jsonModified = true;
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                jsonModified = true;
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                jsonModified = true;
            }
        });
    }

    /**
     * Configura un menú contextual para el área de texto JSON.
     */
    private void setupJsonContextMenu() {
        JPopupMenu popup = new JPopupMenu();
        
        JMenuItem cutItem = new JMenuItem("Cortar");
        cutItem.addActionListener(e -> jsonTextArea.cut());
        
        JMenuItem copyItem = new JMenuItem("Copiar");
        copyItem.addActionListener(e -> jsonTextArea.copy());
        
        JMenuItem pasteItem = new JMenuItem("Pegar");
        pasteItem.addActionListener(e -> jsonTextArea.paste());
        
        JMenuItem formatItem = new JMenuItem("Formatear JSON");
        formatItem.addActionListener(e -> formatJson());
        
        JMenuItem validateItem = new JMenuItem("Validar JSON");
        validateItem.addActionListener(e -> validateJson());
        
        popup.add(cutItem);
        popup.add(copyItem);
        popup.add(pasteItem);
        popup.addSeparator();
        popup.add(formatItem);
        popup.add(validateItem);
        
        jsonTextArea.setComponentPopupMenu(popup);
    }

    /**
     * Muestra un diálogo con las materias habilitadas para el profesor seleccionado.
     */
    private void showProfessorSubjects() {
        Professor professor = (Professor) professorCombo.getSelectedItem();
        if (professor == null) {
            showInfo("No hay profesor seleccionado.");
            return;
        }
        
        List<Subject> subjects = professor.getSubjects();
        if (subjects.isEmpty()) {
            showInfo("El profesor " + professor.getName() + " no tiene materias asignadas.");
            return;
        }
        
        // Crear la tabla de materias
        String[] columnNames = {"Código", "Nombre", "Créditos", "Requiere Lab"};
        Object[][] data = new Object[subjects.size()][4];
        
        for (int i = 0; i < subjects.size(); i++) {
            Subject subject = subjects.get(i);
            data[i][0] = subject.getCode();
            data[i][1] = subject.getName();
            data[i][2] = subject.getCredits();
            data[i][3] = subject.requiresLab() ? "Sí" : "No";
        }
        
        JTable subjectsTable = new JTable(data, columnNames);
        subjectsTable.setDefaultEditor(Object.class, null); // Hacer la tabla no editable
        subjectsTable.setRowHeight(25);
        
        // Personalizar la apariencia
        subjectsTable.getTableHeader().setBackground(new Color(70, 130, 180));
        subjectsTable.getTableHeader().setForeground(Color.WHITE);
        subjectsTable.getTableHeader().setFont(subjectsTable.getTableHeader().getFont().deriveFont(Font.BOLD));
        
        // Alternar colores de fila
        subjectsTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
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
        
        // Crear el panel con la tabla
        JScrollPane scrollPane = new JScrollPane(subjectsTable);
        scrollPane.setPreferredSize(new Dimension(500, 300));
        
        // Mostrar el diálogo
        JOptionPane.showMessageDialog(
            this,
            scrollPane,
            "Materias habilitadas para " + professor.getName(),
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    /**
     * Valida y muestra los conflictos entre las asignaciones existentes.
     */
    private void validateConflicts() {
        if (createdAssignments.isEmpty()) {
            showInfo("No hay asignaciones para validar conflictos.");
            return;
        }
        
        try {
            // Usamos un SwingWorker para no bloquear la UI durante el análisis
            SwingWorker<List<String>, Void> worker = new SwingWorker<>() {
                @Override
                protected List<String> doInBackground() throws Exception {
                    List<String> conflicts = new ArrayList<>();
                    
                    // Para cada par de asignaciones, verificar conflictos
                    for (int i = 0; i < createdAssignments.size(); i++) {
                        Assignment a1 = createdAssignments.get(i);
                        
                        // 1. Verificar conflictos con sí misma (auto-conflictos)
                        // Verificar profesor-materia
                        if (a1.getSubject() != null && !a1.hasProfessorSubjectAuthorization()) {
                            conflicts.add(String.format(
                                "Asignación #%d: El profesor %s no está autorizado para impartir %s",
                                a1.getId(), a1.getProfessorName(), a1.getSubject().getCode()));
                        }
                        
                        // Verificar franja bloqueada
                        if (a1.hasBlockedSlotConflict()) {
                            conflicts.add(String.format(
                                "Asignación #%d: El profesor %s tiene una franja bloqueada en %s de %s a %s",
                                a1.getId(), a1.getProfessorName(), a1.getDay(), 
                                a1.getStartTime(), a1.getEndTime()));
                        }
                        
                        // Verificar compatibilidad aula-materia
                        if (!a1.hasRoomCompatibility()) {
                            conflicts.add(String.format(
                                "Asignación #%d: El aula %s no es compatible con la materia %s",
                                a1.getId(), a1.getRoomName(),
                                a1.getId(), a1.getRoomName(), 
                                a1.getSubject() != null ? a1.getSubject().getCode() : "N/A"));
                        }
                        
                        // Verificar capacidad del aula
                        if (!a1.hasRoomCapacity()) {
                            conflicts.add(String.format(
                                "Asignación #%d: El aula %s (capacidad: %d) no tiene capacidad suficiente para %d estudiantes",
                                a1.getId(), a1.getRoomName(), a1.getRoom().getCapacity(), 
                                a1.getEnrolledStudents()));
                        }
                        
                        // 2. Verificar conflictos con otras asignaciones
                        for (int j = i + 1; j < createdAssignments.size(); j++) {
                            Assignment a2 = createdAssignments.get(j);
                            
                            // Solo verificar si hay solapamiento temporal
                            if (a1.getDay().equals(a2.getDay()) && a1.overlapsTimeWith(a2)) {
                                // Verificar conflicto de profesor
                                if (a1.getProfessorId() == a2.getProfessorId()) {
                                    conflicts.add(String.format(
                                        "Asignaciones #%d y #%d: El profesor %s tiene clase a la misma hora",
                                        a1.getId(), a2.getId(), a1.getProfessorName()));
                                }
                                
                                // Verificar conflicto de aula
                                if (a1.getRoomId() == a2.getRoomId()) {
                                    conflicts.add(String.format(
                                        "Asignaciones #%d y #%d: El aula %s está asignada a la misma hora",
                                        a1.getId(), a2.getId(), a1.getRoomName()));
                                }
                                
                                // Verificar conflicto de grupo
                                if (a1.getGroupId() == a2.getGroupId()) {
                                    conflicts.add(String.format(
                                        "Asignaciones #%d y #%d: El grupo %s tiene clases a la misma hora",
                                        a1.getId(), a2.getId(), a1.getGroupName()));
                                }
                                
                                // Verificar sobrecarga de profesor
                                if (AssignmentBuilder.hasWorkloadConflict(a1, a2)) {
                                    conflicts.add(String.format(
                                        "Asignaciones #%d y #%d: El profesor %s tiene sobrecarga horaria",
                                        a1.getId(), a2.getId(), a1.getProfessorName()));
                                }
                            }
                        }
                    }
                    
                    return conflicts;
                }
                
                @Override
                protected void done() {
                    try {
                        List<String> conflicts = get();
                        
                        if (conflicts.isEmpty()) {
                            showInfo("No se detectaron conflictos entre las asignaciones.");
                        } else {
                            // Crear un mensaje con todos los conflictos encontrados
                            StringBuilder message = new StringBuilder();
                            message.append("Se detectaron los siguientes conflictos:\n\n");
                            
                            for (int i = 0; i < conflicts.size(); i++) {
                                message.append(i + 1).append(". ").append(conflicts.get(i)).append("\n");
                            }
                            
                            // Mostrar en un diálogo con scroll si hay muchos conflictos
                            JTextArea textArea = new JTextArea(message.toString());
                            textArea.setEditable(false);
                            textArea.setLineWrap(true);
                            textArea.setWrapStyleWord(true);
                            
                            JScrollPane scrollPane = new JScrollPane(textArea);
                            scrollPane.setPreferredSize(new Dimension(600, 400));
                            
                            JOptionPane.showMessageDialog(
                                AssignmentManagerUI.this,
                                scrollPane,
                                "Conflictos Detectados",
                                JOptionPane.WARNING_MESSAGE
                            );
                        }
                        
                    } catch (Exception ex) {
                        logger.log(Level.SEVERE, "Error al validar conflictos: " + ex.getMessage(), ex);
                        showError("Error al validar conflictos: " + ex.getMessage());
                    }
                }
            };
            
            // Mostrar diálogo de progreso mientras se analizan los conflictos
            ProgressDialog progressDialog = new ProgressDialog(this, "Analizando conflictos...");
            worker.addPropertyChangeListener(evt -> {
                if (worker.isDone()) {
                    progressDialog.dispose();
                }
            });
            
            worker.execute();
            progressDialog.setVisible(true);
            
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error al iniciar validación: " + ex.getMessage(), ex);
            showError("Error al iniciar validación: " + ex.getMessage());
        }
    }

    /**
     * Método para crear una nueva asignación.
     */
    private void createAssignment() {
        try {
            // Obtener valores del formulario
            Professor professor = (Professor) professorCombo.getSelectedItem();
            Subject subject = (Subject) subjectCombo.getSelectedItem();
            Room room = (Room) roomCombo.getSelectedItem();
            String day = (String) dayCombo.getSelectedItem();
            LocalTime startTime = dateToTime(startTimeSpinner.getValue());
            LocalTime endTime = dateToTime(endTimeSpinner.getValue());
            
            // Validar que los campos obligatorios no sean nulos
            if (professor == null || subject == null || room == null || day == null) {
                showError("Debe seleccionar profesor, materia, aula y día.");
                return;
            }
            
            // Obtener grupo seleccionado
            GroupInfo selectedGroup = (GroupInfo) groupCombo.getSelectedItem();
            if (selectedGroup == null) {
                showError("Debe seleccionar un grupo.");
                return;
            }
            int groupId = selectedGroup.getId();
            String groupName = selectedGroup.getName();
            
            String sessionType = (String) sessionTypeCombo.getSelectedItem();
            int enrolledStudents = (Integer) enrolledStudentsSpinner.getValue();
            
            // Validar el rango de tiempo usando TimeSlot
            if (!TimeSlot.isValidTimeRange(day, startTime, endTime)) {
                showError("El horario seleccionado no es válido para el día " + day + 
                         ".\nPor favor seleccione una franja válida.");
                return;
            }
            
            // Verificar compatibilidad de aula y materia
            if (subject.requiresLab() && !room.isLab()) {
                showError("La materia " + subject.getCode() + " requiere un laboratorio.\n" +
                         "El aula seleccionada no es un laboratorio.");
                return;
            }
            
            // Verificar capacidad del aula
            if (enrolledStudents > room.getCapacity()) {
                showError("El aula seleccionada tiene capacidad para " + room.getCapacity() + 
                         " estudiantes.\nLa cantidad ingresada (" + enrolledStudents + 
                         ") excede la capacidad.");
                return;
            }
            
            // Verificar autorización del profesor
            if (!professor.hasSubject(subject.getCode())) {
                showError("El profesor " + professor.getName() + 
                         " no está autorizado para impartir la materia " + 
                         subject.getCode() + ".");
                return;
            }
            
            // Verificar conflicto con franjas bloqueadas del profesor
            if (professor.hasBlockedSlotConflict(day, startTime, endTime)) {
                showError("El profesor " + professor.getName() + 
                         " tiene una franja bloqueada que se solapa con el horario seleccionado.");
                return;
            }
            
            // Obtener ID para la nueva asignación
            int id = idCounter.getAndIncrement();
            
            // Crear la asignación usando AssignmentBuilder
            Assignment assignment = AssignmentBuilder.createAssignment(
                id,
                professor,
                subject,
                room,
                day,
                startTime,
                endTime,
                groupId,
                groupName,
                sessionType,
                enrolledStudents,
                LocalDate.now()
            );
            
            // Añadir la asignación al DataManager y a la lista local
            dataManager.addAssignment(assignment);
            createdAssignments.add(assignment);
            
            // Actualizar la tabla y la vista JSON
            addAssignmentToTable(assignment);
            updateJsonView();
            
            // Habilitar botón de validación de conflictos
            validateConflictsButton.setEnabled(true);
            
            // Mostrar mensaje de éxito
            showInfo("Asignación creada correctamente con ID: " + id);
            
            // Limpiar formulario
            clearForm();
            
            logger.info("Asignación creada: ID=" + id + ", Profesor=" + professor.getName() + 
                      ", Materia=" + subject.getCode() + ", Día=" + day + 
                      ", Horario=" + startTime + "-" + endTime);
            
        } catch (DomainException ex) {
            // Capturar y mostrar excepciones del dominio
            logger.log(Level.WARNING, "Error al crear asignación: " + ex.getMessage(), ex);
            showError("Error al crear asignación: " + ex.getMessage());
        } catch (Exception ex) {
            // Capturar otras excepciones inesperadas
            logger.log(Level.SEVERE, "Error inesperado: " + ex.getMessage(), ex);
            showError("Error inesperado: " + ex.getMessage());
        }
    }

    /**
     * Elimina la asignación seleccionada en la tabla.
     */
    private void removeSelectedAssignment() {
        int selectedRow = assignmentsTable.getSelectedRow();
        if (selectedRow >= 0) {
            int id = (Integer) tableModel.getValueAt(selectedRow, 0);
            
            // Buscar la asignación por ID
            Assignment toRemove = null;
            for (Assignment a : createdAssignments) {
                if (a.getId() == id) {
                    toRemove = a;
                    break;
                }
            }
            
            // Eliminar de la lista y del DataManager
            if (toRemove != null) {
                createdAssignments.remove(toRemove);
                dataManager.removeAssignment(id);
                
                // Actualizar tabla y JSON
                tableModel.removeRow(selectedRow);
                updateJsonView();
                
                // Actualizar estado del botón de validación
                validateConflictsButton.setEnabled(!createdAssignments.isEmpty());
                
                logger.info("Asignación eliminada: ID=" + id);
                showInfo("Asignación eliminada correctamente.");
            }
        }
    }

    /**
     * Añade una asignación a la tabla.
     */
    private void addAssignmentToTable(Assignment a) {
        tableModel.addRow(new Object[]{
            a.getId(),
            a.getProfessorName(),
            a.getSubject() != null ? a.getSubject().getCode() : "N/A",
            a.getRoomName(),
            a.getDay(),
            a.getStartTime().format(TIME_FORMATTER) + "-" + a.getEndTime().format(TIME_FORMATTER),
            a.getGroupName() + " (" + a.getGroupId() + ")",
            a.getEnrolledStudents()
        });
    }

    /**
     * Actualiza el área de texto JSON con la representación actual.
     */
    private void updateJsonView() {
        try {
            // Si el JSON ya ha sido modificado manualmente, no lo sobrescribimos
            if (jsonModified) {
                return;
            }
            
            // Crear el objeto raíz
            ObjectNode rootNode = objectMapper.createObjectNode();
            
            // Crear un array de asignaciones
            ArrayNode assignmentsArray = objectMapper.createArrayNode();
            
            // Añadir cada asignación al array
            for (Assignment assignment : createdAssignments) {
                ObjectNode assignmentNode = objectMapper.createObjectNode();
                
                // Datos básicos
                assignmentNode.put("id", assignment.getId());
                assignmentNode.put("assignmentDate", assignment.getAssignmentDate().toString());
                assignmentNode.put("day", assignment.getDay());
                assignmentNode.put("startTime", assignment.getStartTime().toString());
                assignmentNode.put("endTime", assignment.getEndTime().toString());
                assignmentNode.put("groupId", assignment.getGroupId());
                assignmentNode.put("groupName", assignment.getGroupName());
                assignmentNode.put("sessionType", assignment.getSessionType());
                assignmentNode.put("enrolledStudents", assignment.getEnrolledStudents());
                
                // Profesor anidado
                ObjectNode professorNode = objectMapper.createObjectNode();
                Professor professor = assignment.getProfessor();
                professorNode.put("id", professor.getId());
                professorNode.put("name", professor.getName());
                professorNode.put("department", professor.getDepartment());
                professorNode.put("email", professor.getEmail());
                
                // Materias del profesor
                ArrayNode subjectsArray = objectMapper.createArrayNode();
                for (Subject subject : professor.getSubjects()) {
                    ObjectNode subjectNode = objectMapper.createObjectNode();
                    subjectNode.put("code", subject.getCode());
                    subjectNode.put("name", subject.getName());
                    subjectNode.put("description", subject.getDescription());
                    subjectNode.put("credits", subject.getCredits());
                    subjectNode.put("requiresLab", subject.requiresLab());
                    subjectsArray.add(subjectNode);
                }
                professorNode.set("subjects", subjectsArray);
                
                // Franjas bloqueadas del profesor
               ArrayNode blockedSlotsArray = objectMapper.createArrayNode();
               for (BlockedSlot slot : professor.getBlockedSlots()) {
                   ObjectNode slotNode = objectMapper.createObjectNode();
                   slotNode.put("day", slot.getDay());
                   slotNode.put("startTime", slot.getStartTime().toString());
                   slotNode.put("endTime", slot.getEndTime().toString());
                   blockedSlotsArray.add(slotNode);
               }
               professorNode.set("blockedSlots", blockedSlotsArray);
               
               assignmentNode.set("professor", professorNode);
               
               // Aula anidada
               ObjectNode roomNode = objectMapper.createObjectNode();
               Room room = assignment.getRoom();
               roomNode.put("id", room.getId());
               roomNode.put("name", room.getName());
               roomNode.put("capacity", room.getCapacity());
               roomNode.put("isLab", room.isLab());
               assignmentNode.set("room", roomNode);
               
               // Materia anidada
               if (assignment.getSubject() != null) {
                   ObjectNode subjectNode = objectMapper.createObjectNode();
                   Subject subject = assignment.getSubject();
                   subjectNode.put("code", subject.getCode());
                   subjectNode.put("name", subject.getName());
                   subjectNode.put("description", subject.getDescription());
                   subjectNode.put("credits", subject.getCredits());
                   subjectNode.put("requiresLab", subject.requiresLab());
                   assignmentNode.set("subject", subjectNode);
               }
               
               // Añadir al array
               assignmentsArray.add(assignmentNode);
           }
           
           // Añadir el array al objeto raíz
           rootNode.set("assignments", assignmentsArray);
           
           // Convertir a formato JSON bonito
           String jsonString = objectMapper.writerWithDefaultPrettyPrinter()
                   .writeValueAsString(rootNode);
           
           // Actualizar el área de texto preservando la posición de scroll
           int scrollPosition = jsonTextArea.getCaretPosition();
           jsonTextArea.setText(jsonString);
           
           // Intentar restaurar la posición si estaba dentro del rango válido
           if (scrollPosition >= 0 && scrollPosition < jsonTextArea.getText().length()) {
               jsonTextArea.setCaretPosition(scrollPosition);
           }
           
           // Resetear el flag ya que acabamos de actualizarlo programáticamente
           jsonModified = false;
           
       } catch (Exception ex) {
           logger.log(Level.SEVERE, "Error al generar JSON: " + ex.getMessage(), ex);
           jsonTextArea.setText("Error al generar JSON: " + ex.getMessage());
       }
   }

   /**
    * Limpia el formulario para una nueva entrada.
    */
   private void clearForm() {
       // Resetear combos
       if (professorCombo.getItemCount() > 0) professorCombo.setSelectedIndex(0);
       if (subjectCombo.getItemCount() > 0) subjectCombo.setSelectedIndex(0);
       if (roomCombo.getItemCount() > 0) roomCombo.setSelectedIndex(0);
       if (dayCombo.getItemCount() > 0) dayCombo.setSelectedIndex(0);
       if (groupCombo.getItemCount() > 0) groupCombo.setSelectedIndex(0);
       if (sessionTypeCombo.getItemCount() > 0) sessionTypeCombo.setSelectedIndex(0);
       
       // Resetear spinners
       startTimeSpinner.setValue(timeToDate(LocalTime.of(8, 0)));
       endTimeSpinner.setValue(timeToDate(LocalTime.of(10, 0)));
       enrolledStudentsSpinner.setValue(20);
       
       // Actualizar franjas válidas
       updateValidTimeRanges();
   }

   /**
    * Actualiza el combo de franjas horarias válidas según el día seleccionado.
    */
   private void updateValidTimeRanges() {
       validRangesCombo.removeAllItems();
       
       String selectedDay = (String) dayCombo.getSelectedItem();
       if (selectedDay != null) {
           try {
               List<TimeSlot.TimeRange> validRanges = TimeSlot.getValidTimeSlots(
                       TimeSlot.parseDayOfWeek(selectedDay));
               
               for (TimeSlot.TimeRange range : validRanges) {
                   validRangesCombo.addItem(range);
               }
               
               if (validRangesCombo.getItemCount() > 0) {
                   validRangesCombo.setSelectedIndex(0);
               }
           } catch (Exception ex) {
               logger.log(Level.WARNING, "Error al obtener franjas válidas: " + ex.getMessage(), ex);
           }
       }
   }

   /**
    * Verifica la compatibilidad entre la materia y el aula seleccionadas.
    */
   private void checkCompatibility() {
       Subject subject = (Subject) subjectCombo.getSelectedItem();
       Room room = (Room) roomCombo.getSelectedItem();
       
       if (subject != null && room != null) {
           if (subject.requiresLab() && !room.isLab()) {
               // Mostrar advertencia visual
               roomCombo.setBackground(new Color(255, 200, 200));
               roomCombo.setToolTipText("Esta materia requiere laboratorio");
           } else {
               // Restablecer apariencia normal
               roomCombo.setBackground(Color.WHITE);
               roomCombo.setToolTipText(null);
           }
       }
   }

   /**
   * Verifica si el profesor está autorizado para impartir la materia seleccionada.
   */
  private void checkAuthorization() {
      Professor professor = (Professor) professorCombo.getSelectedItem();
      Subject subject = (Subject) subjectCombo.getSelectedItem();
      
      if (professor != null && subject != null) {
          boolean isAuthorized = professor.hasSubject(subject.getCode());
          
          if (!isAuthorized) {
              // Mostrar advertencia visual
              subjectCombo.setBackground(new Color(255, 200, 200));
              subjectCombo.setToolTipText("El profesor no está autorizado para esta materia");
          } else {
              // Restablecer apariencia normal
              subjectCombo.setBackground(Color.WHITE);
              subjectCombo.setToolTipText(null);
          }
      }
  }

   /**
    * Convierte un objeto Date en LocalTime para usar con el modelo de dominio.
    */
   private LocalTime dateToTime(Object value) {
       if (value instanceof java.util.Date) {
           java.util.Date date = (java.util.Date) value;
           return java.time.LocalDateTime.ofInstant(
                   date.toInstant(), java.time.ZoneId.systemDefault()).toLocalTime();
       }
       return LocalTime.now(); // Valor por defecto
   }

   /**
    * Convierte un LocalTime en Date para usar con los spinners.
    */
   private java.util.Date timeToDate(LocalTime time) {
       return java.util.Date.from(
               time.atDate(java.time.LocalDate.now())
                   .atZone(java.time.ZoneId.systemDefault())
                   .toInstant());
   }

   /**
    * Crea un spinner para selección de horas.
    */
   private JSpinner createTimeSpinner(int hour, int minute) {
       JSpinner spinner = new JSpinner(new SpinnerDateModel());
       JSpinner.DateEditor editor = new JSpinner.DateEditor(spinner, "HH:mm");
       spinner.setEditor(editor);
       
       // Configurar valor inicial
       spinner.setValue(timeToDate(LocalTime.of(hour, minute)));
       
       return spinner;
   }

   /**
    * Crea un botón estilizado.
    */
   private JButton createStyledButton(String text, Color background) {
       JButton button = new JButton(text);
       button.setBackground(background);
       button.setForeground(Color.WHITE);
       button.setFont(button.getFont().deriveFont(Font.BOLD));
       button.setFocusPainted(false);
       button.setBorderPainted(false);
       
       // Efecto hover
       button.addMouseListener(new MouseAdapter() {
           @Override
           public void mouseEntered(MouseEvent evt) {
               button.setBackground(background.brighter());
           }
           
           @Override
           public void mouseExited(MouseEvent evt) {
               button.setBackground(background);
           }
       });
       
       return button;
   }

   /**
    * Añade un campo etiquetado al formulario.
    */
   private void addFormField(JPanel panel, GridBagConstraints gbc, int row, 
                            String label, Component component) {
       gbc.gridx = 0;
       gbc.gridy = row;
       gbc.gridwidth = 1;
       gbc.fill = GridBagConstraints.NONE;
       gbc.anchor = GridBagConstraints.EAST;
       JLabel lbl = new JLabel(label);
       lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
       panel.add(lbl, gbc);
       
       gbc.gridx = 1;
       gbc.fill = GridBagConstraints.HORIZONTAL;
       gbc.anchor = GridBagConstraints.WEST;
       panel.add(component, gbc);
   }

   /**
    * Muestra un mensaje de error.
    */
   private void showError(String message) {
       JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
   }

   /**
    * Muestra un mensaje informativo.
    */
   private void showInfo(String message) {
       JOptionPane.showMessageDialog(this, message, "Información", JOptionPane.INFORMATION_MESSAGE);
   }

   /**
    * Importa datos desde un archivo data.json usando AssignmentJsonService.
    */
   private void importDataJson() {
       JFileChooser fileChooser = new JFileChooser();
       fileChooser.setDialogTitle("Seleccionar archivo data.json");
       fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos JSON", "json"));
       
       int result = fileChooser.showOpenDialog(this);
       if (result == JFileChooser.APPROVE_OPTION) {
           File selectedFile = fileChooser.getSelectedFile();
           String filePath = selectedFile.getAbsolutePath();
           
           try {
               // Verificar que el archivo existe y es accesible
               if (!selectedFile.exists() || !selectedFile.canRead()) {
                   showError("No se puede acceder al archivo seleccionado.");
                   return;
               }
               
               // Verificar que el archivo no esté vacío
               if (selectedFile.length() == 0) {
                   showError("El archivo seleccionado está vacío.");
                   return;
               }
               
               // Mostrar diálogo de progreso
               SwingWorker<ImportResult, Void> worker = new SwingWorker<>() {
                   @Override
                   protected ImportResult doInBackground() throws Exception {
                       ImportResult result = new ImportResult();
                       
                       try {
                           // Usar AssignmentJsonService para cargar los datos
                           result.count = jsonService.loadFromJson(filePath);
                           
                           // Leer el contenido del archivo JSON para mostrarlo en el editor
                           result.jsonContent = Files.readString(selectedFile.toPath());
                           
                           return result;
                       } catch (Exception ex) {
                           result.error = ex;
                           return result;
                       }
                   }
                   
                   @Override
                   protected void done() {
                       try {
                           // Obtener el resultado
                           ImportResult result = get();
                           
                           if (result.error != null) {
                               throw result.error;
                           }
                           
                           // Mostrar en el área de texto para posible edición manual
                           jsonTextArea.setText(result.jsonContent);
                           
                           // Actualizar la UI con los datos cargados
                           refreshDataFromDataManager();
                           
                           // Mostrar mensaje con la información correcta
                           showInfo("Datos importados correctamente desde:\n" + filePath + 
                                   "\n\nSe cargaron " + result.count + " asignaciones.");
                           
                           logger.info("Datos importados desde: " + filePath + 
                                      " - Se cargaron " + result.count + " asignaciones");
                           
                           // Resetear el flag de modificación ya que acabamos de cargar
                           jsonModified = false;
                           
                       } catch (Exception ex) {
                           Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                           logger.log(Level.SEVERE, "Error al importar datos: " + cause.getMessage(), cause);
                           showError("Error al importar datos:\n" + cause.getMessage());
                           
                           // Limpiar el dataManager si hubo un error en la carga
                           try {
                               dataManager.clearAll();
                               refreshDataFromDataManager();
                           } catch (Exception e) {
                               // Ignorar errores en la limpieza
                           }
                       }
                   }
               };
               
               // Mostrar diálogo de progreso mientras se carga
               ProgressDialog progressDialog = new ProgressDialog(this, "Importando datos...");
               worker.addPropertyChangeListener(evt -> {
                   if (worker.isDone()) {
                       progressDialog.dispose();
                   }
               });
               
               worker.execute();
               progressDialog.setVisible(true);
               
           } catch (Exception ex) {
               logger.log(Level.SEVERE, "Error al importar datos: " + ex.getMessage(), ex);
               showError("Error al importar datos:\n" + ex.getMessage());
           }
       }
   }

   /**
    * Clase para encapsular los resultados de la importación.
    */
   private static class ImportResult {
       public int count;
       public String jsonContent;
       public Exception error;
   }

   /**
    * Exporta el JSON actual a un archivo usando AssignmentJsonService.
    */
   private void exportCurrentJson() {
       JFileChooser fileChooser = new JFileChooser();
       fileChooser.setDialogTitle("Guardar JSON");
       fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos JSON", "json"));
       fileChooser.setSelectedFile(new File("data.json"));
       
       int result = fileChooser.showSaveDialog(this);
       if (result == JFileChooser.APPROVE_OPTION) {
           File selectedFile = fileChooser.getSelectedFile();
           
           // Asegurar que el archivo tenga extensión .json
           String filePath = selectedFile.getAbsolutePath();
           if (!filePath.toLowerCase().endsWith(".json")) {
               filePath += ".json";
           }
           
           try {
               // Si se ha editado manualmente el JSON, usamos esa versión
               if (jsonModified) {
                   // Guardar la versión modificada manualmente
                   Files.writeString(Path.of(filePath), jsonTextArea.getText());
               } else {
                   // Usar AssignmentJsonService para guardar las asignaciones actuales
                   jsonService.saveToJson(filePath);
               }
               
               showInfo("JSON guardado correctamente en:\n" + filePath);
               logger.info("JSON exportado a: " + filePath);
               
           } catch (Exception ex) {
               logger.log(Level.SEVERE, "Error al exportar JSON: " + ex.getMessage(), ex);
               showError("Error al exportar JSON:\n" + ex.getMessage());
           }
       }
   }

   /**
    * Genera un archivo graph.json usando AssignmentJsonService.
    */
   private void generateGraphJson() {
       if (createdAssignments.isEmpty()) {
           showError("No hay asignaciones para analizar.\nImporte datos o cree asignaciones primero.");
           return;
       }

       File targetFile = chooseTargetFile();
       if (targetFile == null) return;

       if (!confirmOverwriteIfNeeded(targetFile)) return;

       if (!ensureDirectoryExists(targetFile.getParentFile())) return;

       final String finalPath = targetFile.getAbsolutePath();
       File inputFile = prepareInputJson();
       if (inputFile == null) return;

       runGenerateGraphWorker(inputFile, finalPath);
   }

   /** Abre un JFileChooser para seleccionar dónde guardar graph.json. */
   private File chooseTargetFile() {
       JFileChooser chooser = new JFileChooser();
       chooser.setDialogTitle("Guardar archivo graph.json");
       chooser.setFileFilter(new FileNameExtensionFilter("Archivos JSON", "json"));
       chooser.setSelectedFile(new File("graph.json"));

       if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
           return null;
       }
       File sel = chooser.getSelectedFile();
       String path = sel.getAbsolutePath();
       if (!path.toLowerCase().endsWith(".json")) {
           sel = new File(path + ".json");
       }
       return sel;
   }

   /** Si el archivo existe, pregunta si sobrescribir. */
   private boolean confirmOverwriteIfNeeded(File f) {
       if (!f.exists()) return true;
       int opt = JOptionPane.showConfirmDialog(this,
           "El archivo ya existe. ¿Desea sobrescribirlo?",
           "Confirmar sobrescritura", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
       return opt == JOptionPane.YES_OPTION;
   }

   /** Asegura que el directorio exista (o lo crea). */
   private boolean ensureDirectoryExists(File dir) {
       if (dir == null || dir.exists()) return true;
       try {
           if (!dir.mkdirs()) {
               showError("No se pudo crear el directorio: " + dir);
               return false;
           }
           return true;
       } catch (SecurityException ex) {
           showError("Sin permisos para crear directorio: " + dir);
           return false;
       }
   }

   /** Prepara el JSON de entrada: usa edición manual o salva el estado actual. */
   private File prepareInputJson() {
       try {
           File temp = File.createTempFile("graph_input_", ".json");
           if (jsonModified) {
               Files.writeString(temp.toPath(), jsonTextArea.getText());
           } else {
               jsonService.saveToJson(temp.getAbsolutePath());
           }
           return temp;
       } catch (IOException ex) {
           showError("Error al crear archivo temporal de entrada:\n" + ex.getMessage());
           return null;
       }
   }

   /** Lanza el SwingWorker y, al terminar, muestra el mensaje final. */
   private void runGenerateGraphWorker(File inputFile, String outputPath) {
       SwingWorker<Integer, Void> worker = new SwingWorker<>() {
           @Override
           protected Integer doInBackground() throws Exception {
               return jsonService.generateConflictGraph(
                   inputFile.getAbsolutePath(), outputPath);
           }
           @Override
           protected void done() {
               cleanupTemp(inputFile);
               try {
                   int conflicts = get();
                   verifyOutputFile(outputPath);
                   JOptionPane.showMessageDialog(AssignmentManagerUI.this,
                       "Grafo generado con éxito en:\n" + outputPath +
                       "\nConflictos detectados: " + conflicts +
                       "\n\nAccede a la pestaña Conflictos para visualizar el grafo.",
                       "Operación completada",
                       JOptionPane.INFORMATION_MESSAGE);
               } catch (Exception ex) {
                   handleWorkerError(ex, outputPath);
               }
           }
       };

       ProgressDialog pd = new ProgressDialog(this, "Generando grafo de conflictos...");
       worker.addPropertyChangeListener(evt -> {
           if ("state".equals(evt.getPropertyName()) &&
               SwingWorker.StateValue.DONE == evt.getNewValue()) {
               pd.dispose();
           }
       });
       worker.execute();
       pd.setVisible(true);
   }

   /** Elimina el archivo temporal. */
   private void cleanupTemp(File temp) {
       try {
           if (temp != null && temp.exists()) temp.delete();
       } catch (Exception ignored) { }
   }

   /** Verifica que el archivo de salida exista y no esté vacío. */
   private void verifyOutputFile(String path) throws Exception {
       File out = new File(path);
       if (!out.exists() || out.length() == 0) {
           throw new Exception("El archivo de salida no se generó correctamente.");
       }
   }

   /** Maneja errores al finalizar el SwingWorker. */
   private void handleWorkerError(Exception ex, String outputPath) {
       String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
       logger.log(Level.SEVERE, "Error al generar grafo: " + msg, ex);
       showError("Error al generar grafo de conflictos:\n" + msg);
       // intentar borrar salida vacía
       try {
           File out = new File(outputPath);
           if (out.exists() && out.length() == 0) out.delete();
       } catch (Exception ignored) { }
   }

   /**
    * Actualiza la UI con los datos cargados en el DataManager.
    */
   private void refreshDataFromDataManager() {
       // Limpiar datos actuales
       createdAssignments.clear();
       tableModel.setRowCount(0);
       
       // Cargar datos desde DataManager
       List<Assignment> assignments = dataManager.getAllAssignments();
       for (Assignment assignment : assignments) {
           createdAssignments.add(assignment);
           addAssignmentToTable(assignment);
       }
       
       // Actualizar la vista JSON
       jsonModified = false;  // Resetear flag para permitir actualización
       updateJsonView();
       
       // Actualizar estado del botón de validación
       validateConflictsButton.setEnabled(!createdAssignments.isEmpty());
       
       // Actualizar contador de IDs para futuras asignaciones
       if (!assignments.isEmpty()) {
           int maxId = assignments.stream()
                   .mapToInt(Assignment::getId)
                   .max()
                   .orElse(0);
           idCounter.set(maxId + 1);
       }
   }

   /**
    * Formatea el texto JSON para hacerlo más legible (pretty-print).
    */
   private void formatJson() {
       try {
           // Leer el JSON actual como objeto
           String currentJson = jsonTextArea.getText();
           if (currentJson.trim().isEmpty()) {
               showInfo("No hay contenido JSON para formatear.");
               return;
           }
           
           // Intentar parsear el JSON para verificar que es válido
           ObjectNode rootNode = (ObjectNode) objectMapper.readTree(currentJson);
           
           // Formatear y mostrar
           String formattedJson = objectMapper.writerWithDefaultPrettyPrinter()
                   .writeValueAsString(rootNode);
           jsonTextArea.setText(formattedJson);
           
           showInfo("JSON formateado correctamente.");
           
       } catch (Exception ex) {
           logger.log(Level.WARNING, "Error al formatear JSON: " + ex.getMessage(), ex);
           showError("Error al formatear JSON: " + ex.getMessage() + 
                    "\nVerifique que el JSON sea válido.");
       }
   }

   /**
    * Valida la sintaxis del JSON sin aplicar cambios.
    */
   private void validateJson() {
       try {
           String currentJson = jsonTextArea.getText();
           if (currentJson.trim().isEmpty()) {
               showInfo("No hay contenido JSON para validar.");
               return;
           }
           
           // Intentar parsear el JSON para verificar que es válido
           objectMapper.readTree(currentJson);
           
           showInfo("El JSON es sintácticamente válido.");
           
       } catch (Exception ex) {
           logger.log(Level.WARNING, "Error al validar JSON: " + ex.getMessage(), ex);
           
           // Mostrar mensaje con ubicación del error
           String errorMsg = ex.getMessage();
           int lineNum = -1;
           int colNum = -1;
           
           // Intentar extraer línea y columna del error
           if (errorMsg.contains("line:")) {
               try {
                   String lineInfo = errorMsg.substring(errorMsg.indexOf("line:"));
                   lineNum = Integer.parseInt(lineInfo.substring(5, lineInfo.indexOf(',')));
                   colNum = Integer.parseInt(lineInfo.substring(lineInfo.indexOf("column:") + 7, 
                                           lineInfo.indexOf(')')));
               } catch (Exception e) {
                   // Ignorar errores al extraer línea/columna
               }
           }
           
           if (lineNum > 0 && colNum > 0) {
               showError("Error de sintaxis JSON en línea " + lineNum + ", columna " + colNum + 
                        ":\n" + errorMsg);
               
               // Intentar posicionar el cursor en la ubicación del error
               try {
                   int offset = getPositionInDocument(lineNum, colNum);
                   jsonTextArea.setCaretPosition(offset);
                   jsonTextArea.requestFocus();
               } catch (Exception e) {
                   // Ignorar errores al posicionar el cursor
               }
           } else {
               showError("Error de sintaxis JSON:\n" + errorMsg);
           }
       }
   }

   /**
    * Calcula la posición en el documento basado en línea y columna.
    */
   private int getPositionInDocument(int line, int column) {
       try {
           String text = jsonTextArea.getText();
           String[] lines = text.split("\n", -1);
           
           int position = 0;
           for (int i = 0; i < line - 1 && i < lines.length; i++) {
               position += lines[i].length() + 1; // +1 por el \n
           }
           
           return position + Math.min(column - 1, lines[line - 1].length());
       } catch (Exception e) {
           return 0;
       }
   }

   /**
    * Aplica los cambios realizados manualmente en el JSON.
    */
   private void applyJsonChanges() {
       try {
           // Leer el JSON modificado
           String modifiedJson = jsonTextArea.getText();
           if (modifiedJson.trim().isEmpty()) {
               showError("El JSON está vacío. No hay cambios que aplicar.");
               return;
           }
           
           // Confirmar antes de proceder
           int option = JOptionPane.showConfirmDialog(this,
                   "¿Está seguro de que desea aplicar los cambios manuales al JSON?\n" +
                   "Esto reemplazará todas las asignaciones actuales.",
                   "Confirmar Cambios",
                   JOptionPane.YES_NO_OPTION,
                   JOptionPane.WARNING_MESSAGE);
                   
           if (option != JOptionPane.YES_OPTION) {
               return;
           }
           
           // Guardar el JSON en un archivo temporal
           File tempFile = File.createTempFile("manual_edit", ".json");
           try {
               Files.writeString(tempFile.toPath(), modifiedJson);
               
               // Usar AssignmentJsonService para cargar desde el archivo temporal
               int loadedCount = jsonService.loadFromJson(tempFile.getAbsolutePath());
               
               // Actualizar la UI con los datos cargados
               refreshDataFromDataManager();
               
               // Resetear el flag de modificación
               jsonModified = false;
               
               showInfo("Cambios aplicados correctamente. " +
                      "Se han cargado " + loadedCount + " asignaciones.");
               
           } finally {
               // Asegurar que se elimine el archivo temporal
               tempFile.delete();
           }
           
       } catch (Exception ex) {
           logger.log(Level.SEVERE, "Error al aplicar cambios JSON: " + ex.getMessage(), ex);
           showError("Error al aplicar cambios JSON:\n" + ex.getMessage() + 
                    "\nVerifique que el JSON tenga el formato correcto.");
       }
   }

   /**
    * Clase para mostrar un diálogo de progreso durante operaciones largas.
    */
   private static class ProgressDialog extends JDialog {
       public ProgressDialog(Frame owner, String message) {
           super(owner, "Procesando", true);
           
           JProgressBar progressBar = new JProgressBar();
           progressBar.setIndeterminate(true);
           
           JLabel messageLabel = new JLabel(message);
           messageLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
           
           setLayout(new BorderLayout());
           add(messageLabel, BorderLayout.NORTH);
           add(progressBar, BorderLayout.CENTER);
           
           setSize(300, 100);
           setLocationRelativeTo(owner);
           setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
       }
   }

   /**
    * Punto de entrada para probar la interfaz.
    */
   public static void main(String[] args) {
       SwingUtilities.invokeLater(() -> {
           try {
               // Intentar usar el Look & Feel del sistema
               UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
           } catch (Exception e) {
               // Ignorar errores y continuar con el Look & Feel por defecto
               logger.log(Level.WARNING, "No se pudo establecer el Look & Feel del sistema", e);
           }
           
           // Crear y mostrar la interfaz
           AssignmentManagerUI ui = new AssignmentManagerUI();
           ui.setVisible(true);
       });
   }
}