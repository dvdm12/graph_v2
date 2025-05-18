package com.example.miapp.ui.main;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mxgraph.layout.mxFastOrganicLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxStylesheet;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.plaf.FontUIResource;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * Panel que muestra un grafo importado desde graph.json,
 * con diseño avanzado, toolbar y leyenda.
 */
public class GraphViewerPanel extends JPanel {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JTextArea jsonTextArea = new JTextArea();
    private mxGraphComponent graphComponent;
    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final JPanel legendPanel = new JPanel();
    private final JToolBar toolbar = new JToolBar();
    private  mxGraph mx = new mxGraph();

    // Iconos para la barra de herramientas
    private final Icon loadIcon = UIManager.getIcon("FileView.fileIcon");
    private final Icon zoomInIcon = UIManager.getIcon("OptionPane.zoomIcon");
    private final Icon zoomOutIcon = UIManager.getIcon("OptionPane.errorIcon");
    private final JButton loadBtn = new JButton("Importar", loadIcon);
    private final JButton zoomIn = new JButton("+", zoomInIcon);
    private final JButton zoomOut = new JButton("–", zoomOutIcon);
    private JPanel visualPanel;

    public GraphViewerPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        initializeToolbar();
        initializeTabs();
        add(toolbar, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);

        // Aplica el Look and Feel FlatLaf
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (UnsupportedLookAndFeelException ex) {
            System.err.println("Failed to initialize LaF");
        }
        // Establecer la fuente predeterminada para toda la aplicación
        Font interFont = new Font("Inter", Font.PLAIN, 12);
        UIManager.put("Label.font", new FontUIResource(interFont));
        UIManager.put("Button.font", new FontUIResource(interFont));
        UIManager.put("TitledBorder.font", new FontUIResource(interFont));
        jsonTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

    }

    private void initializeToolbar() {
        toolbar.setFloatable(false);
        loadBtn.addActionListener(e -> onLoadJson());
        zoomIn.addActionListener(e -> {
            if (graphComponent != null) {
                graphComponent.zoomIn();
            }
        });
        zoomOut.addActionListener(e -> {
            if (graphComponent != null) {
                graphComponent.zoomOut();
            }
        });
        toolbar.add(loadBtn);
        toolbar.addSeparator();
        toolbar.add(zoomIn);
        toolbar.add(zoomOut);

    }

    private void initializeTabs() {
        jsonTextArea.setEditable(false);
        tabbedPane.addTab("JSON", new JScrollPane(jsonTextArea));

        visualPanel = new JPanel(new BorderLayout());
        visualPanel.add(new JLabel("Importa un JSON para visualizar", SwingConstants.CENTER), BorderLayout.CENTER);
        legendPanel.setLayout(new BoxLayout(legendPanel, BoxLayout.Y_AXIS));
        legendPanel.setBorder(BorderFactory.createTitledBorder("Leyenda"));
        visualPanel.add(legendPanel, BorderLayout.EAST);
        tabbedPane.addTab("Visual", visualPanel);
    }

    private JPanel createLegendEntry(Color color, String text) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JLabel icon = new JLabel("  ");
        icon.setOpaque(true);
        icon.setBackground(color);
        icon.setPreferredSize(new Dimension(16, 16));
        p.add(icon);
        p.add(new JLabel(text));
        return p;
    }

    private void updateLegend(mxGraph graph) {
        legendPanel.removeAll();
        legendPanel.add(createLegendEntry(Color.decode("#E8F8F5"), "Nodo: Asignación"));
        legendPanel.add(Box.createVerticalStrut(5));
        legendPanel.add(createLegendEntry(Color.decode("#FDEDEC"), "Arista roja: Conflictos"));
        legendPanel.add(Box.createVerticalStrut(5));
        legendPanel.add(new JLabel("Tamaño de arista ∝ nº conflictos"));

        // Crear un mapa para almacenar los grupos y sus colores
        Map<String, Color> groupColors = new HashMap<>();
        for (Object cell : graph.getChildVertices(graph.getDefaultParent())) {
            if (cell instanceof mxCell) {
                mxCell vertex = (mxCell) cell;
                String[] parts = vertex.getValue().toString().split(":");
                if (parts.length > 1) {
                    String groupName = parts[1].trim().split("\\(")[0].trim();
                    if (!groupColors.containsKey(groupName)) {
                        Color groupColor = new Color((int) (Math.random() * 0xFFFFFF));
                        groupColors.put(groupName, groupColor);
                    }
                    legendPanel.add(Box.createVerticalStrut(5));
                    legendPanel.add(createLegendEntry(groupColors.get(groupName), "Grupo: " + groupName));
                }
            }
        }
        legendPanel.revalidate();
        legendPanel.repaint();
    }

    private void onLoadJson() {
        // Abre el directorio "data" en el mismo nivel que la clase.
        File dataDir = new File(GraphViewerPanel.class.getResource("").getFile() + "data");
        if (!dataDir.exists() || !dataDir.isDirectory()) {
            JOptionPane.showMessageDialog(this,
                    "El directorio 'data' no existe o no es un directorio válido.\n" +
                    "Por favor, asegúrate de que el directorio 'data' se encuentra en el mismo nivel que el paquete com.example.miapp.ui.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser(dataDir); // Establece el directorio de inicio
        chooser.setDialogTitle("Selecciona graph.json");
        FileNameExtensionFilter filter = new FileNameExtensionFilter("JSON Files", "json");
        chooser.setFileFilter(filter);

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        try {
            JsonNode root = objectMapper.readTree(file);
            jsonTextArea.setText(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root));
            jsonTextArea.setCaretPosition(0);
            showGraph(root);
            tabbedPane.setSelectedIndex(1);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error al parsear JSON:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showGraph(JsonNode root) {
        JsonNode nodes = root.path("nodes"), edges = root.path("edges");
        if (!nodes.isArray() || !edges.isArray()) {
            JOptionPane.showMessageDialog(this, "El JSON debe tener arrays 'nodes' y 'edges'.",
                    "Formato inválido", JOptionPane.WARNING_MESSAGE);
            return;
        }

        mx = new mxGraph();
        applyStyles(mx.getStylesheet());
        Object parent = mx.getDefaultParent();
        mx.getModel().beginUpdate();

        Map<Integer, mxCell> vMap = new HashMap<>();
        try {
            for (JsonNode n : nodes) {
                int id = n.path("id").asInt();
                String groupName = n.path("groupName").asText() + " (" + n.path("groupId").asInt() + ")";
                String label = id + ": " + groupName;
                mxCell cell = (mxCell) mx.insertVertex(parent, null, label,
                        0, 0, 180, 60, "NODE");
                vMap.put(id, cell);
            }
            for (JsonNode e : edges) {
                int[] between = {e.path("between").get(0).asInt(), e.path("between").get(1).asInt()};
                mxCell src = vMap.get(between[0]), dst = vMap.get(between[1]);
                if (src != null && dst != null) {
                    JsonNode confs = e.path("conflicts");
                    int cnt = confs.isArray() ? confs.size() : 0;
                    String label = cnt > 0
                            ? StreamSupport.stream(confs.spliterator(), false)
                            .map(JsonNode::asText)
                            .collect(Collectors.joining(", "))
                            : "";
                    mx.insertEdge(parent, null, label, src, dst, "EDGE;strokeWidth=" + (1 + cnt));
                }
            }
        } finally {
            mx.getModel().endUpdate();
        }

        mxFastOrganicLayout layout = new mxFastOrganicLayout(mx);
        layout.setForceConstant(100);
        layout.execute(parent);

        graphComponent = new mxGraphComponent(mx);
        graphComponent.setConnectable(false);
        graphComponent.getGraph().setAllowDanglingEdges(false);
        graphComponent.getGraphControl().addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Object cell = graphComponent.getCellAt(e.getX(), e.getY());
                if (cell instanceof mxCell) {
                    String tooltip = getTooltipText((mxCell) cell, root);
                    graphComponent.setToolTipText(tooltip);
                } else {
                    graphComponent.setToolTipText(null);
                }
            }
        });

        visualPanel.removeAll();
        visualPanel.add(graphComponent, BorderLayout.CENTER);
        visualPanel.add(legendPanel, BorderLayout.EAST);
        visualPanel.revalidate();
        visualPanel.repaint();
        updateLegend(mx);
    }

    private String getTooltipText(mxCell cell, JsonNode root) {
        if (cell.isVertex()) {
            int id = Integer.parseInt(cell.getValue().toString().split(":")[0].trim());
            JsonNode node = null;
            for (JsonNode n : root.path("nodes")) {
                if (n.path("id").asInt() == id) {
                    node = n;
                    break;
                }
            }
            if (node != null) {
                String groupName = node.path("groupName").asText();
                int groupId = node.path("groupId").asInt();
                return "<html><b>Nodo:</b> " + id + "<br><b>Grupo:</b> " + groupName + "<br><b>ID Grupo:</b> " + groupId + "</html>";
            }
        } else if (cell.isEdge()) {
            JsonNode edge = null;
             for (JsonNode e : root.path("edges")) {
                int[] between = { Integer.parseInt(cell.getSource().getId()), Integer.parseInt(cell.getTarget().getId())};
                JsonNode between0 = e.path("between").get(0);
                JsonNode between1 = e.path("between").get(1);
                if (between0 != null && between1 != null &&
                    ((between0.asInt() == between[0] && between1.asInt() == between[1]) ||
                    (between0.asInt() == between[1] && between1.asInt() == between[0])))
                    {
                    edge = e;
                    break;
                }
            }
            if (edge != null) {
                JsonNode conflicts = edge.path("conflicts");
                int conflictCount = conflicts.isArray() ? conflicts.size() : 0;
                return "<html><b>Arista</b><br><b>Conflictos:</b> " + conflictCount + "<br><b>Detalles:</b> " + (conflictCount > 0 ? conflicts.toString() : "Ninguno") + "</html>";
            }
        }
        return "Información no disponible";
    }

    private void applyStyles(mxStylesheet style) {
        var nodeStyle = style.getDefaultVertexStyle();
        nodeStyle.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_RECTANGLE);
        nodeStyle.put(mxConstants.STYLE_ROUNDED, true);
        nodeStyle.put(mxConstants.STYLE_FILLCOLOR, "#E8F8F5");
        nodeStyle.put(mxConstants.STYLE_FONTCOLOR, "#0B5345");
        nodeStyle.put(mxConstants.STYLE_FONTSIZE, 14);
        nodeStyle.put(mxConstants.STYLE_SHADOW, true);
        nodeStyle.put(mxConstants.STYLE_STROKECOLOR, "#D4EDDA");

        var edgeStyle = style.getDefaultEdgeStyle();
        edgeStyle.put(mxConstants.STYLE_STROKECOLOR, "#E74C3C");
        edgeStyle.put(mxConstants.STYLE_FONTCOLOR, "#C0392B");
        edgeStyle.put(mxConstants.STYLE_ENDARROW, mxConstants.ARROW_CLASSIC);
        edgeStyle.put(mxConstants.STYLE_ROUNDED, true);
        edgeStyle.put(mxConstants.STYLE_LABEL_BACKGROUNDCOLOR, "#FFFFFF");
        edgeStyle.put(mxConstants.STYLE_FONTSIZE, 12);

        style.putCellStyle("NODE", nodeStyle);
        style.putCellStyle("EDGE", edgeStyle);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new FlatLightLaf());
            } catch (UnsupportedLookAndFeelException ex) {
                Logger.getLogger(GraphViewerPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
            JFrame f = new JFrame("Graph Viewer");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.getContentPane().add(new GraphViewerPanel());
            f.setSize(1000, 700);
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}

