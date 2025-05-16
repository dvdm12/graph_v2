package com.example.miapp.ui;

import com.example.miapp.domain.*;
import com.example.miapp.exception.DomainException;
import com.example.miapp.repository.DataManager;
import com.example.miapp.util.AssignmentBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
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
 * Permite crear y gestionar asignaciones con profesores existentes y visualizar 
 * la representación JSON en tiempo real.
 */
public class AssignmentManagerUI extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(AssignmentManagerUI.class.getName());
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // Gestor de datos
    private final DataManager dataManager;
    private final ObjectMapper objectMapper;
    private final AtomicInteger idCounter;

    // Lista de asignaciones creadas
    private final List<Assignment> createdAssignments = new ArrayList<>();
    
    // Componentes de la interfaz
    private JComboBox<Professor> professorCombo;
    private JComboBox<Subject> subjectCombo;
    private JComboBox<Room> roomCombo;
    private JComboBox<String> dayCombo;
    private JSpinner startTimeSpinner;
    private JSpinner endTimeSpinner;
    private JTextField groupIdField;
    private JTextField groupNameField;
    private JComboBox<String> sessionTypeCombo;
    private JSpinner enrolledStudentsSpinner;
    private JTextArea jsonTextArea;
    private JTable assignmentsTable;
    private DefaultTableModel tableModel;
    private JButton createButton;
    private JButton removeButton;
    private JButton clearButton;
    private JComboBox<TimeSlot.TimeRange> validRangesCombo;

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
        
        // Panel izquierdo (formulario y tabla)
        JSplitPane leftPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        leftPanel.setDividerLocation(400);
        
        // Formulario de creación
        leftPanel.setTopComponent(createFormPanel());
        
        // Tabla de asignaciones
        leftPanel.setBottomComponent(createTablePanel());
        
        // Panel derecho (JSON)
        JPanel rightPanel = createJsonPanel();
        
        // Añadir paneles al split principal
        mainSplitPane.setLeftComponent(leftPanel);
        mainSplitPane.setRightComponent(rightPanel);
        
        // Añadir a la ventana
        getContentPane().add(mainSplitPane);
        
        // Configurar eventos
        setupEventListeners();
        
        // Inicializar JSON vacío
        updateJsonView();
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
        updateValidTimeRanges();
        
        // Spinners para horas
        startTimeSpinner = createTimeSpinner(8, 0);
        endTimeSpinner = createTimeSpinner(10, 0);
        
        groupIdField = new JTextField(10);
        groupNameField = new JTextField(15);
        sessionTypeCombo = new JComboBox<>(new String[]{"D", "N"});
        enrolledStudentsSpinner = new JSpinner(new SpinnerNumberModel(20, 1, 100, 1));

        // Botones de acción
        createButton = createStyledButton("Crear Asignación", new Color(70, 130, 180));
        clearButton = createStyledButton("Limpiar Formulario", new Color(180, 180, 180));
        
        // Añadir componentes al panel
        addFormField(panel, gbc, row++, "Profesor:", professorCombo);
        addFormField(panel, gbc, row++, "Materia:", subjectCombo);
        addFormField(panel, gbc, row++, "Aula:", roomCombo);
        addFormField(panel, gbc, row++, "Día:", dayCombo);
        addFormField(panel, gbc, row++, "Franjas Válidas:", validRangesCombo);
        addFormField(panel, gbc, row++, "Hora Inicio:", startTimeSpinner);
        addFormField(panel, gbc, row++, "Hora Fin:", endTimeSpinner);
        addFormField(panel, gbc, row++, "ID Grupo:", groupIdField);
        addFormField(panel, gbc, row++, "Nombre Grupo:", groupNameField);
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
     * Crea el panel JSON para visualizar la representación JSON de las asignaciones.
     */
    private JPanel createJsonPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new CompoundBorder(
                new EmptyBorder(10, 10, 10, 10),
                new TitledBorder(BorderFactory.createLineBorder(new Color(46, 139, 87), 2), 
                        "Representación JSON")
        ));
        panel.setBackground(new Color(240, 255, 240));

        jsonTextArea = new JTextArea();
        jsonTextArea.setEditable(false);
        jsonTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        jsonTextArea.setBackground(new Color(250, 255, 250));
        
        JScrollPane scrollPane = new JScrollPane(jsonTextArea);
        panel.add(scrollPane, BorderLayout.CENTER);

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
            
            // Validar y convertir campos numéricos
            int groupId;
            try {
                groupId = Integer.parseInt(groupIdField.getText().trim());
            } catch (NumberFormatException ex) {
                showError("El ID de grupo debe ser un número válido.");
                return;
            }
            
            String groupName = groupNameField.getText().trim();
            if (groupName.isEmpty()) {
                showError("El nombre del grupo no puede estar vacío.");
                return;
            }
            
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
            
            // Actualizar el área de texto
            jsonTextArea.setText(jsonString);
            
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
        if (sessionTypeCombo.getItemCount() > 0) sessionTypeCombo.setSelectedIndex(0);
        
        // Resetear campos de texto
        groupIdField.setText("");
        groupNameField.setText("");
        
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
       button.addMouseListener(new java.awt.event.MouseAdapter() {
           @Override
           public void mouseEntered(java.awt.event.MouseEvent evt) {
               button.setBackground(background.brighter());
           }
           
           @Override
           public void mouseExited(java.awt.event.MouseEvent evt) {
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