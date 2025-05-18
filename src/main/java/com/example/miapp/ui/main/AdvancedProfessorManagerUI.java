package com.example.miapp.ui.main;

import com.example.miapp.domain.BlockedSlot;
import com.example.miapp.domain.Professor;
import com.example.miapp.domain.Subject;
import com.example.miapp.domain.TimeSlot;
import com.example.miapp.exception.DomainException;
import com.example.miapp.repository.DataManager;
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
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Interfaz avanzada unificada para creación y gestión de Profesores.
 * Diseñada para funcionar como un panel integrado dentro de un CardLayout en MainUI.
 * Incorpora logging, control de ID y selección de franjas permitidas.
 */
public class AdvancedProfessorManagerUI extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(AdvancedProfessorManagerUI.class.getName());

    private final DataManager dataManager;
    private final ObjectMapper objectMapper;
    private final AtomicInteger idCounter;

    // Form fields
    private final JTextField nameField;
    private final JTextField deptField;
    private final JTextField emailField;
    private final JComboBox<Subject> subj1, subj2, subj3;
    private final JComboBox<String> dayCombo;
    private final JComboBox<TimeSlot.TimeRange> rangeCombo;
    private final JSpinner startSpinner, endSpinner;
    private final JButton addSlotBtn, removeSlotBtn, createBtn, clearBtn;

    // Draft blocked slots
    private final List<BlockedSlotInfo> draftBlockedSlots = new ArrayList<>();
    private final DefaultTableModel slotsTableModel;
    private final JTable slotsTable;

    // Professors model
    private final List<Professor> createdProfessors = new ArrayList<>();
    private final DefaultTableModel profTableModel;

    // JSON preview
    private final JTextArea jsonArea;
    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Constructor principal. Inicializa el panel y todos sus componentes.
     */
    public AdvancedProfessorManagerUI() {
        super(new BorderLayout());
        dataManager = DataManager.getInstance();

        // Inicializar ID en el siguiente disponible
        int nextId = dataManager.getAllProfessors().stream()
            .mapToInt(Professor::getId)
            .max()
            .orElse(0) + 1;
        idCounter = new AtomicInteger(nextId);

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        // Campos básicos
        nameField = new JTextField(20);
        deptField = new JTextField("Facultad de Ingeniería", 20);
        deptField.setEditable(false);
        deptField.setBackground(new Color(240, 240, 240));
        emailField = new JTextField(20);
        emailField.setToolTipText("Debe terminar con @eam.edu.co");
        
        // Campos de selección
        List<Subject> subjects = dataManager.getAllSubjects();
        subj1 = new JComboBox<>(subjects.toArray(new Subject[0]));
        subj2 = new JComboBox<>(subjects.toArray(new Subject[0]));
        subj3 = new JComboBox<>(subjects.toArray(new Subject[0]));
        dayCombo = new JComboBox<>(new String[]{"Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"});
        rangeCombo = new JComboBox<>();
        startSpinner = createTimeSpinner();
        endSpinner = createTimeSpinner();

        // Botones
        addSlotBtn = createStyledButton("Añadir Franja");
        removeSlotBtn = createStyledButton("Eliminar Franja");
        createBtn = createStyledButton("Crear Profesor");
        clearBtn = createStyledButton("Limpiar");

        // Tablas
        slotsTableModel = new DefaultTableModel(new Object[]{"Día", "Inicio", "Fin"}, 0);
        slotsTable = new JTable(slotsTableModel);
        profTableModel = new DefaultTableModel(new Object[]{"ID","Nombre","Dept","Email","Materias","Franjas"}, 0);

        // JSON
        jsonArea = new JTextArea();
        jsonArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        jsonArea.setEditable(false);

        initUI();
        populateRangeCombo();
        setupListeners();
        loadExistingProfessors();
        
        logger.info("Interfaz inicializada con ID inicial: " + nextId);
    }

    /**
     * Carga los profesores existentes desde el DataManager.
     */
    private void loadExistingProfessors() {
        List<Professor> existing = dataManager.getAllProfessors();
        createdProfessors.addAll(existing);
        refreshProfTable();
    }

    /**
     * Inicializa la interfaz de usuario agregando todos los componentes.
     */
    private void initUI() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildFormPanel(), buildRightPanel());
        split.setDividerLocation(480);
        add(split, BorderLayout.CENTER);
        updateJson();
    }

    /**
     * Construye el panel del formulario para la creación de profesores.
     */
    private JPanel buildFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(230, 240, 255));
        panel.setBorder(new CompoundBorder(
            new EmptyBorder(10,10,10,10),
            new TitledBorder(BorderFactory.createLineBorder(new Color(100, 150, 200), 2), "Formulario de Profesor")
        ));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6,6,6,6);
        gc.anchor = GridBagConstraints.WEST;
        int y = 0;

        addField(panel, gc, y++, "Nombre:", nameField);
        addField(panel, gc, y++, "Departamento:", deptField);
        addField(panel, gc, y++, "Email:", emailField);
        addField(panel, gc, y++, "Materia 1:", subj1);
        addField(panel, gc, y++, "Materia 2:", subj2);
        addField(panel, gc, y++, "Materia 3:", subj3);

        addField(panel, gc, y++, "Día:", dayCombo);
        addField(panel, gc, y++, "Rango Permitido:", rangeCombo);
        addField(panel, gc, y++, "Hora Inicio:", startSpinner);
        addField(panel, gc, y++, "Hora Fin:", endSpinner);

        gc.gridx = 0; gc.gridy = y; panel.add(addSlotBtn, gc);
        gc.gridx = 1; panel.add(removeSlotBtn, gc);
        y++;

        JScrollPane scroll = new JScrollPane(slotsTable);
        scroll.setPreferredSize(new Dimension(420, 140));
        gc.gridx = 0; gc.gridy = y; gc.gridwidth = 2; panel.add(scroll, gc);
        gc.gridwidth = 1; y++;

        JPanel bp = new JPanel(); bp.setBackground(new Color(200, 220, 240));
        bp.add(createBtn); bp.add(clearBtn);
        gc.gridx = 0; gc.gridy = y; gc.gridwidth = 2; panel.add(bp, gc);

        return panel;
    }

    /**
     * Método auxiliar para añadir un campo con etiqueta al formulario.
     */
    private void addField(JPanel panel, GridBagConstraints gc, int row, String label, Component comp) {
        gc.gridx = 0; gc.gridy = row; gc.anchor = GridBagConstraints.EAST;
        JLabel lbl = new JLabel(label);
        lbl.setForeground(new Color(30,30,60));
        panel.add(lbl, gc);
        gc.gridx = 1; gc.anchor = GridBagConstraints.WEST;
        panel.add(comp, gc);
    }

    /**
     * Construye el panel derecho con las tablas y visualización JSON.
     */
    private JPanel buildRightPanel() {
        JPanel panel = new JPanel(new BorderLayout(5,5));
        panel.setBorder(new CompoundBorder(
            new EmptyBorder(10,10,10,10),
            new TitledBorder(BorderFactory.createLineBorder(new Color(100,200,150), 2), "Profesores & JSON")
        ));
        JTable pt = new JTable(profTableModel);
        pt.setFillsViewportHeight(true);
        JSplitPane vs = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(pt), new JScrollPane(jsonArea));
        vs.setDividerLocation(300);
        panel.add(vs, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Configura los listeners para todos los componentes interactivos.
     */
    private void setupListeners() {
        dayCombo.addActionListener(e -> { populateRangeCombo(); updateJson(); });
        rangeCombo.addActionListener(e -> {
            TimeSlot.TimeRange r = (TimeSlot.TimeRange) rangeCombo.getSelectedItem();
            if (r != null) {
                LocalDate today = LocalDate.now();
                startSpinner.setValue(Date.from(today.atTime(r.getStart()).atZone(ZoneId.systemDefault()).toInstant()));
                endSpinner.setValue(Date.from(today.atTime(r.getEnd()).atZone(ZoneId.systemDefault()).toInstant()));
            }
        });
        addSlotBtn.addActionListener(e -> addSlot());
        removeSlotBtn.addActionListener(e -> removeSlot());
        createBtn.addActionListener(e -> createProfessor());
        clearBtn.addActionListener(e -> clearForm());

        DocumentListener dl = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateJson(); }
            public void removeUpdate(DocumentEvent e) { updateJson(); }
            public void changedUpdate(DocumentEvent e) { updateJson(); }
        };
        nameField.getDocument().addDocumentListener(dl);
        emailField.getDocument().addDocumentListener(dl);
    }

    /**
     * Rellena el combobox de rangos horarios según el día seleccionado.
     */
    private void populateRangeCombo() {
        rangeCombo.removeAllItems();
        String day = (String) dayCombo.getSelectedItem();
        List<TimeSlot.TimeRange> ranges = TimeSlot.getValidTimeSlots(TimeSlot.parseDayOfWeek(day));
        for (TimeSlot.TimeRange r : ranges) rangeCombo.addItem(r);
    }

    /**
 * Añade una franja horaria bloqueada a la lista y la tabla.
 * Con validación para evitar solapamientos con franjas existentes.
 */
private void addSlot() {
    String day = (String) dayCombo.getSelectedItem();
    Date d1 = (Date) startSpinner.getValue();
    Date d2 = (Date) endSpinner.getValue();
    LocalTime st = Instant.ofEpochMilli(d1.getTime()).atZone(ZoneId.systemDefault()).toLocalTime();
    LocalTime en = Instant.ofEpochMilli(d2.getTime()).atZone(ZoneId.systemDefault()).toLocalTime();
    
    // Validar que sea un rango válido según las franjas permitidas
    if (!TimeSlot.isValidTimeRange(day, st, en)) {
        logger.warning("Franja inválida: " + day + " " + st + "-" + en);
        showError("Rango inválido según franjas permitidas.");
        return;
    }
    
    // Validar que la hora de fin sea posterior a la de inicio
    if (!en.isAfter(st)) {
        logger.warning("Hora fin <= inicio: " + st + "-" + en);
        showError("La hora final debe ser posterior al inicio.");
        return;
    }
    
    // NUEVA VALIDACIÓN: Comprobar solapamiento con franjas ya añadidas
    for (BlockedSlotInfo existing : draftBlockedSlots) {
        if (existing.day.equals(day) && 
            timeRangesOverlap(existing.start, existing.end, st, en)) {
            
            logger.warning("Solapamiento detectado: " + 
                existing.day + " " + existing.start + "-" + existing.end + 
                " solapa con " + day + " " + st + "-" + en);
                
            showError("Ya existe una franja bloqueada que se solapa con el horario seleccionado.");
            return;
        }
    }
    
    // Si pasa todas las validaciones, añadir la franja
    draftBlockedSlots.add(new BlockedSlotInfo(day, st, en));
    slotsTableModel.addRow(new Object[]{day, st.format(timeFmt), en.format(timeFmt)});
    logger.info("Franja añadida: " + day + " " + st + "-" + en);
    updateJson();
}

/**
 * Verifica si dos rangos de tiempo se solapan.
 * 
 * @param start1 Hora de inicio del primer rango
 * @param end1 Hora de fin del primer rango
 * @param start2 Hora de inicio del segundo rango
 * @param end2 Hora de fin del segundo rango
 * @return true si hay solapamiento, false en caso contrario
 */
private boolean timeRangesOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
    return !end1.isBefore(start2) && !start1.isAfter(end2);
}

    /**
     * Elimina la franja horaria bloqueada seleccionada.
     */
    private void removeSlot() {
        int sel = slotsTable.getSelectedRow();
        if (sel >= 0) {
            BlockedSlotInfo info = draftBlockedSlots.remove(sel);
            slotsTableModel.removeRow(sel);
            logger.info("Franja eliminada: " + info.day + " " + info.start + "-" + info.end);
            updateJson();
        } else {
            showError("Seleccione una franja a eliminar.");
        }
    }

    /**
     * Crea un nuevo profesor con los datos del formulario.
     */
    private void createProfessor() {
        // Validación de campos básicos
        if (nameField.getText().isBlank() || emailField.getText().isBlank()) {
            logger.warning("Datos básicos incompletos.");
            showError("Complete nombre y email.");
            return;
        }
        
        // Validar formato de email
        String email = emailField.getText().trim();
        if (!email.endsWith("@eam.edu.co")) {
            logger.warning("Email inválido: " + email);
            showError("El email debe terminar con @eam.edu.co");
            return;
        }
        
        // Validación de materias
        Set<Subject> set = new HashSet<>();
        for (JComboBox<Subject> cb : List.of(subj1, subj2, subj3)) {
            Subject s = (Subject) cb.getSelectedItem();
            if (s != null) set.add(s);
        }
        if (set.isEmpty()) {
            logger.warning("No se seleccionaron materias.");
            showError("Seleccione al menos una materia.");
            return;
        }
        
        // Creación del profesor
        int id = idCounter.get();
        Professor p = new Professor(id, nameField.getText().trim(), deptField.getText(), email);
        set.forEach(p::assignSubject);
        draftBlockedSlots.forEach(info -> p.addBlockedSlot(
            new BlockedSlot.Builder()
                .day(info.day)
                .startTime(info.start)
                .endTime(info.end)
                .build()
        ));
        
        try {
            dataManager.addProfessor(p);
            createdProfessors.add(p);
            idCounter.incrementAndGet();
            logger.info("Profesor creado: ID=" + p.getId());
            JOptionPane.showMessageDialog(this, "Profesor creado: " + p.getName(), "Éxito", JOptionPane.INFORMATION_MESSAGE);
            refreshProfTable();
            clearForm();
        } catch (DomainException ex) {
            logger.log(Level.SEVERE, "Error creando profesor: " + ex.getMessage(), ex);
            showError(ex.getMessage());
        }
    }

    /**
     * Actualiza la tabla de profesores con la lista actual.
     */
    private void refreshProfTable() {
        profTableModel.setRowCount(0);
        for (Professor p : createdProfessors) {
            String subs = p.getSubjects().stream().map(Subject::getCode).collect(Collectors.joining(","));
            String slots = p.getBlockedSlots().stream()
                .map(bs->bs.getDay()+"["+bs.getStartTime().format(timeFmt)+"-"+bs.getEndTime().format(timeFmt)+"]")
                .collect(Collectors.joining(","));
            profTableModel.addRow(new Object[]{p.getId(), p.getName(), p.getDepartment(), p.getEmail(), subs, slots});
        }
    }

    /**
     * Limpia el formulario para una nueva entrada.
     */
    private void clearForm() {
        nameField.setText(""); 
        emailField.setText("");
        subj1.setSelectedIndex(0); 
        subj2.setSelectedIndex(0); 
        subj3.setSelectedIndex(0);
        draftBlockedSlots.clear(); 
        slotsTableModel.setRowCount(0);
        updateJson();
    }

    /**
     * Actualiza la representación JSON según el estado actual.
     */
    private void updateJson() {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            ArrayNode arr = objectMapper.createArrayNode();
            ObjectNode draft = objectMapper.createObjectNode();
            draft.put("id", idCounter.get());
            draft.put("name", nameField.getText());
            draft.put("department", deptField.getText());
            draft.put("email", emailField.getText());
            ArrayNode sj = objectMapper.createArrayNode();
            new HashSet<>(List.of((Subject)subj1.getSelectedItem(),(Subject)subj2.getSelectedItem(),(Subject)subj3.getSelectedItem()))
                .stream().filter(Objects::nonNull).forEach(s->{
                    ObjectNode so=objectMapper.createObjectNode();
                    so.put("code",s.getCode()); so.put("name",s.getName()); so.put("credits",s.getCredits()); so.put("requiresLab",s.requiresLab());
                    sj.add(so);
                });
            draft.set("subjects",sj);
            ArrayNode bs=objectMapper.createArrayNode();
            draftBlockedSlots.forEach(info->{
                ObjectNode bn=objectMapper.createObjectNode();
                bn.put("day",info.day); bn.put("startTime",info.start.toString()); bn.put("endTime",info.end.toString());
                bs.add(bn);
            });
            draft.set("blockedSlots",bs);
            arr.add(draft);
            createdProfessors.forEach(p->arr.add(objectMapper.valueToTree(p)));
            root.set("professors",arr);
            jsonArea.setText(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error serializando JSON: " + e.getMessage(), e);
            jsonArea.setText("Error JSON: " + e.getMessage());
        }
    }

    /**
     * Muestra un mensaje de error.
     */
    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Crea un spinner para selección de hora.
     */
    private JSpinner createTimeSpinner() {
        JSpinner s = new JSpinner(new SpinnerDateModel());
        s.setEditor(new JSpinner.DateEditor(s, "HH:mm"));
        return s;
    }

    /**
     * Crea un botón con estilo predefinido.
     */
    private JButton createStyledButton(String text) {
        JButton b = new JButton(text);
        b.setFont(b.getFont().deriveFont(Font.BOLD, 13f));
        b.setBackground(new Color(100, 150, 200));
        b.setForeground(Color.BLUE);
        return b;
    }

    /**
     * Clase interna para almacenar información temporal de franjas bloqueadas.
     */
    private class BlockedSlotInfo {
        final String day;
        final LocalTime start;
        final LocalTime end;
        BlockedSlotInfo(String d, LocalTime s, LocalTime e) {
            day = d;
            start = s;
            end = e;
        }
    }

    /**
     * Método para crear una instancia independiente para pruebas.
     */
    public static void createStandaloneInstance() {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch(Exception ignored) {}
            
            JFrame frame = new JFrame("Gestor Avanzado de Profesores");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1200, 800);
            frame.add(new AdvancedProfessorManagerUI());
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    /**
     * Punto de entrada para pruebas independientes.
     */
    public static void main(String[] args) {
        createStandaloneInstance();
    }
}