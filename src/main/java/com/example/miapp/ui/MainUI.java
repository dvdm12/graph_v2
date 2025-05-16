package com.example.miapp.ui;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import com.example.miapp.domain.Subject;
import com.example.miapp.domain.BlockedSlot;
import com.example.miapp.domain.Professor;
import com.example.miapp.domain.Assignment;
import com.example.miapp.repository.DataManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;


/**
 * Ventana principal avanzada con menú, barra de herramientas,
 * panel lateral y CardLayout para mostrar diferentes interfaces.
 * Incluye visor de grafo de conflictos y gestor de asignaciones.
 */
public class MainUI extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(MainUI.class.getName());

    private final JPanel sidePanel;
    private final CardLayout cardLayout;
    private final JPanel mainPanel;
    private final DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    // Cached panels
    private JPanel professorPanel;
    private JPanel assignmentPanel;

    public MainUI() {
        super("Gestión Académica");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        logger.info("Iniciando construcción de MainUI");

        setJMenuBar(createMenuBar());
        add(createToolBar(), BorderLayout.NORTH);

        sidePanel = createSidePanel();
        add(sidePanel, BorderLayout.WEST);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        
        // Panel de inicio
        JPanel homePanel = new JPanel(new BorderLayout());
        homePanel.setBackground(new Color(240, 248, 255));
        homePanel.add(centeredLabel("Bienvenido al sistema de gestión académica"), BorderLayout.CENTER);
        
        // Añadir paneles al CardLayout
        mainPanel.add(homePanel, "home");
        mainPanel.add(new ConflictGraphUI(), "conflicts");
        mainPanel.add(new SubjectViewerUI(), "subjects");
        
        // Inicialmente estos paneles no se crean hasta que se necesiten
        professorPanel = null;
        assignmentPanel = null;

        add(mainPanel, BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);
        updateStatus("Listo a las " + LocalTime.now().format(dtFmt));
        cardLayout.show(mainPanel, "home");
        
        logger.info("MainUI construido completamente");
        setVisible(true);
    }

    private JMenuBar createMenuBar() {
        logger.fine("Creando barra de menú");
        JMenuBar menuBar = new JMenuBar();
        
        JMenu mArchivo = new JMenu("Archivo");
        JMenuItem miSalir = new JMenuItem("Salir");
        miSalir.addActionListener(e -> System.exit(0));
        mArchivo.add(miSalir);

        JMenu mProfes = new JMenu("Profesores");
        JMenuItem miGestor = new JMenuItem("Gestor Avanzado");
        miGestor.addActionListener(e -> {
            logger.info("Menú Profesores > Gestor Avanzado seleccionado");
            showProfessorManager();
        });
        mProfes.add(miGestor);

        JMenu mConf = new JMenu("Conflictos");
        JMenuItem miGrafo = new JMenuItem("Visualizar Grafo");
        miGrafo.addActionListener(e -> {
            logger.info("Menú Conflictos > Visualizar Grafo seleccionado");
            cardLayout.show(mainPanel, "conflicts");
            updateStatus("Visualización de grafo de conflictos activada");
        });
        mConf.add(miGrafo);

        JMenu mAsig = new JMenu("Asignaturas");
        JMenuItem miList = new JMenuItem("Listar Asignaturas");
        miList.addActionListener(e -> {
            logger.info("Menú Asignaturas > Listar Asignaturas seleccionado");
            cardLayout.show(mainPanel, "subjects");
            updateStatus("Lista de asignaturas activada");
        });
        mAsig.add(miList);
        
        // Nuevo menú para Asignaciones
        JMenu mAssignments = new JMenu("Asignaciones");
        JMenuItem miAssignmentManager = new JMenuItem("Gestor de Asignaciones");
        miAssignmentManager.addActionListener(e -> {
            logger.info("Menú Asignaciones > Gestor de Asignaciones seleccionado");
            showAssignmentManager();
        });
        mAssignments.add(miAssignmentManager);

        menuBar.add(mArchivo);
        menuBar.add(mProfes);
        menuBar.add(mConf);
        menuBar.add(mAsig);
        menuBar.add(mAssignments);
        
        logger.fine("Barra de menú creada");
        return menuBar;
    }

    private JToolBar createToolBar() {
        logger.fine("Creando barra de herramientas");
        JToolBar tb = new JToolBar();
        tb.setFloatable(false); // Evitar que se pueda desprender
        
        JButton btnHome = createToolButton("Inicio", e -> {
            logger.info("Botón Inicio presionado");
            cardLayout.show(mainPanel, "home");
            updateStatus("Pantalla de inicio");
        });
        
        JButton btnProf = createToolButton("Profesores", e -> {
            logger.info("Botón Profesores presionado");
            showProfessorManager();
        });
        
        JButton btnConf = createToolButton("Conflictos", e -> {
            logger.info("Botón Conflictos presionado");
            cardLayout.show(mainPanel, "conflicts");
            updateStatus("Visualización de grafo de conflictos activada");
        });
        
        JButton btnAsig = createToolButton("Asignaturas", e -> {
            logger.info("Botón Asignaturas presionado");
            cardLayout.show(mainPanel, "subjects");
            updateStatus("Lista de asignaturas activada");
        });
        
        // Nuevo botón para Asignaciones
        JButton btnAssignments = createToolButton("Asignaciones", e -> {
            logger.info("Botón Asignaciones presionado");
            showAssignmentManager();
        });
        
        tb.add(btnHome);
        tb.add(btnProf);
        tb.add(btnConf);
        tb.add(btnAsig);
        tb.add(btnAssignments);
        
        logger.fine("Barra de herramientas creada");
        return tb;
    }
    
    /**
     * Crea un botón estilizado para la barra de herramientas
     */
    private JButton createToolButton(String text, java.awt.event.ActionListener listener) {
        JButton btn = new JButton(text);
        btn.setFocusable(false);
        btn.setBackground(new Color(240, 240, 240));
        btn.setFont(btn.getFont().deriveFont(Font.BOLD));
        btn.addActionListener(listener);
        return btn;
    }

    private JPanel createSidePanel() {
        logger.fine("Creando panel lateral");
        JPanel p = new JPanel(new GridLayout(0,1,5,5));
        p.setPreferredSize(new Dimension(180,0));
        p.setBorder(BorderFactory.createTitledBorder("Navegación"));
        
        JButton b1 = new JButton("Inicio");
        b1.addActionListener(e -> {
            logger.info("Panel lateral: Botón Inicio presionado");
            cardLayout.show(mainPanel, "home");
            updateStatus("Pantalla de inicio");
        });
        
        JButton b2 = new JButton("Profesores");
        b2.addActionListener(e -> {
            logger.info("Panel lateral: Botón Profesores presionado");
            showProfessorManager();
        });
        
        JButton b3 = new JButton("Conflictos");
        b3.addActionListener(e -> {
            logger.info("Panel lateral: Botón Conflictos presionado");
            cardLayout.show(mainPanel, "conflicts");
            updateStatus("Visualización de grafo de conflictos activada");
        });
        
        JButton b4 = new JButton("Asignaturas");
        b4.addActionListener(e -> {
            logger.info("Panel lateral: Botón Asignaturas presionado");
            cardLayout.show(mainPanel, "subjects");
            updateStatus("Lista de asignaturas activada");
        });
        
        // Nuevo botón para Asignaciones
        JButton b5 = new JButton("Asignaciones");
        b5.addActionListener(e -> {
            logger.info("Panel lateral: Botón Asignaciones presionado");
            showAssignmentManager();
        });
        
        p.add(b1);
        p.add(b2);
        p.add(b3);
        p.add(b4);
        p.add(b5);
        
        logger.fine("Panel lateral creado");
        return p;
    }

    private JPanel createStatusBar() {
        logger.fine("Creando barra de estado");
        JPanel status = new JPanel(new BorderLayout());
        JLabel lbl = new JLabel("");
        lbl.setBorder(BorderFactory.createEtchedBorder());
        status.add(lbl, BorderLayout.WEST);
        return status;
    }

    private void updateStatus(String msg) {
        ((JLabel)((JPanel)getContentPane().getComponent(3)).getComponent(0)).setText(" " + msg);
        logger.log(Level.INFO, msg);
    }

    private JLabel centeredLabel(String text) {
        JLabel lbl = new JLabel(text, SwingConstants.CENTER);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 16f));
        return lbl;
    }

    /**
 * Muestra el gestor de profesores en el panel principal
 */
private void showProfessorManager() {
    try {
        logger.info("=== Mostrando Gestor de Profesores ===");
        
        // Inicializar el panel si es la primera vez
        if (professorPanel == null) {
            logger.info("Creando panel de profesores por primera vez");
            
            // Crear una nueva instancia del gestor (ahora es un JPanel directamente)
            professorPanel = new AdvancedProfessorManagerUI();
            
            // Añadirlo al CardLayout
            mainPanel.add(professorPanel, "professors");
            
            logger.info("Panel de profesores creado y añadido al CardLayout");
        } else {
            logger.info("Usando panel de profesores existente");
        }
        
        // Mostrar el panel en el CardLayout
        cardLayout.show(mainPanel, "professors");
        
        // Actualizar estado
        updateStatus("Gestor de Profesores activado");
        
        logger.info("=== Gestor de Profesores mostrado con éxito ===");
    } catch (Exception e) {
        logger.log(Level.SEVERE, "Error al mostrar el Gestor de Profesores", e);
        JOptionPane.showMessageDialog(
            this,
            "Error al mostrar el Gestor de Profesores: " + e.getMessage(),
            "Error",
            JOptionPane.ERROR_MESSAGE
        );
    }
}
    
    /**
     * Muestra el gestor de asignaciones en el panel principal
     */
    private void showAssignmentManager() {
        try {
            logger.info("=== Mostrando Gestor de Asignaciones ===");
            
            // Inicializar el panel si es la primera vez
            if (assignmentPanel == null) {
                logger.info("Creando panel de asignaciones por primera vez");
                
                // Crear una nueva instancia del gestor
                AssignmentManagerUI mgr = new AssignmentManagerUI();
                
                // Obtener su contenido
                JPanel content = (JPanel) mgr.getContentPane();
                
                // Asignar el panel
                assignmentPanel = content;
                
                // Añadirlo al CardLayout
                mainPanel.add(assignmentPanel, "assignments");
                
                logger.info("Panel de asignaciones creado y añadido al CardLayout");
            } else {
                logger.info("Usando panel de asignaciones existente");
            }
            
            // Mostrar el panel en el CardLayout
            cardLayout.show(mainPanel, "assignments");
            
            // Actualizar estado
            updateStatus("Gestor de Asignaciones activado");
            
            logger.info("=== Gestor de Asignaciones mostrado con éxito ===");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error al mostrar el Gestor de Asignaciones", e);
            JOptionPane.showMessageDialog(
                this,
                "Error al mostrar el Gestor de Asignaciones: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // Panel para visualizar grafo de conflictos en JSON
    private class ConflictGraphUI extends JPanel {
        private final JTextArea area;
        private final ObjectMapper mapper;
        public ConflictGraphUI() {
            super(new BorderLayout());
            mapper = new ObjectMapper().registerModule(new JavaTimeModule());
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            area = new JTextArea(); area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12)); area.setEditable(false);
            add(new JScrollPane(area), BorderLayout.CENTER);
            loadConflicts();
        }
        private void loadConflicts() {
            try {
                ObjectNode root = mapper.createObjectNode();
                ArrayNode arr = mapper.createArrayNode();
                List<Professor> profs = DataManager.getInstance().getAllProfessors();
                for (int i=0; i<profs.size(); i++) {
                    for (int j=i+1; j<profs.size(); j++) {
                        Professor p1 = profs.get(i), p2 = profs.get(j);
                        for (BlockedSlot bs1 : p1.getBlockedSlots()) {
                            for (BlockedSlot bs2 : p2.getBlockedSlots()) {
                                if (bs1.getDay().equals(bs2.getDay()) &&
                                    bs1.getStartTime().isBefore(bs2.getEndTime()) &&
                                    bs2.getStartTime().isBefore(bs1.getEndTime())) {
                                    ObjectNode edge = mapper.createObjectNode();
                                    edge.put("professor1", p1.getName());
                                    edge.put("professor2", p2.getName());
                                    edge.put("day", bs1.getDay());
                                    edge.put("overlapStart", bs1.getStartTime().plusNanos(0).toString());
                                    edge.put("overlapEnd", bs1.getEndTime().isBefore(bs2.getEndTime()) ? bs1.getEndTime().toString() : bs2.getEndTime().toString());
                                    arr.add(edge);
                                }
                            }
                        }
                    }
                }
                
                // Añadir también los conflictos entre asignaciones
                List<Assignment> assignments = DataManager.getInstance().getAllAssignments();
                for (int i=0; i<assignments.size(); i++) {
                    for (int j=i+1; j<assignments.size(); j++) {
                        Assignment a1 = assignments.get(i);
                        Assignment a2 = assignments.get(j);
                        
                        // Verificar si hay solapamiento de tiempo
                        if (a1.getDay().equals(a2.getDay()) && 
                            a1.overlapsTimeWith(a2)) {
                            
                            // Verificar diferentes tipos de conflictos
                            if (a1.getProfessorId() == a2.getProfessorId()) {
                                ObjectNode edge = mapper.createObjectNode();
                                edge.put("type", "PROFESSOR");
                                edge.put("assignment1", a1.getId());
                                edge.put("assignment2", a2.getId());
                                edge.put("description", "Mismo profesor en horarios solapados");
                                arr.add(edge);
                            }
                            
                            if (a1.getRoomId() == a2.getRoomId()) {
                                ObjectNode edge = mapper.createObjectNode();
                                edge.put("type", "ROOM");
                                edge.put("assignment1", a1.getId());
                                edge.put("assignment2", a2.getId());
                                edge.put("description", "Misma aula en horarios solapados");
                                arr.add(edge);
                            }
                            
                            if (a1.getGroupId() == a2.getGroupId()) {
                                ObjectNode edge = mapper.createObjectNode();
                                edge.put("type", "GROUP");
                                edge.put("assignment1", a1.getId());
                                edge.put("assignment2", a2.getId());
                                edge.put("description", "Mismo grupo en horarios solapados");
                                arr.add(edge);
                            }
                        }
                    }
                }
                
                root.set("conflictGraph", arr);
                area.setText(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root));
            } catch (Exception e) {
                area.setText("Error cargando conflictos: " + e.getMessage());
                logger.log(Level.WARNING, "Error al cargar conflictos", e);
            }
        }
    }

    // Panel para listar asignaturas
    private class SubjectViewerUI extends JPanel {
        public SubjectViewerUI() {
            super(new BorderLayout());
            DefaultListModel<String> model = new DefaultListModel<>();
            for (Subject s : DataManager.getInstance().getAllSubjects()) {
                model.addElement(s.getCode() + " - " + s.getName());
            }
            JList<String> list = new JList<>(model);
            add(new JScrollPane(list), BorderLayout.CENTER);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { 
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); 
                logger.info("Look & Feel del sistema configurado");
            } catch (Exception e) {
                logger.log(Level.WARNING, "No se pudo establecer el Look & Feel del sistema", e);
            }
            
            logger.info("========== INICIANDO APLICACIÓN ==========");
            new MainUI();
        });
    }
}