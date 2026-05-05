package sruu.gui;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GUIAgent extends Agent {

    public static final String SERVICE_TYPE = "GUI";

    // === Données de simulation ===
    private final Map<String, AgentVisual> agents = new ConcurrentHashMap<>();
    private final Map<String, IncidentVisual> incidents = new ConcurrentHashMap<>();
    private final java.util.List<LogEntry> eventLog = Collections.synchronizedList(new ArrayList<>());

    // === Composants Swing ===
    private JFrame mainFrame;
    private SimulationPanel simPanel;
    private JTable statsTable;
    private JTextArea logArea;
    private JLabel statusLabel;

    // ============================================================
    // INITIALISATION
    // ============================================================

    @Override
    protected void setup() {
        System.out.println("[GUI] Starting GUI Agent...");

        // Enregistrement DF
        registerDF();

        // Création de l'interface
        SwingUtilities.invokeLater(this::createGUI);

        // Comportement d'écoute
        addBehaviour(new GUIUpdateListener());
    }

    private void registerDF() {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setType(SERVICE_TYPE);
            dfd.addServices(sd);
            DFService.register(this, dfd);
            System.out.println("[GUI] Registered with DF service");
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    // ============================================================
    // CRÉATION DE L'INTERFACE GRAPHIQUE
    // ============================================================

    private void createGUI() {
        mainFrame = new JFrame("SRUU - Systeme de Reponse aux Urgences Urbaines");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(1200, 800);
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setAlwaysOnTop(true);

        // Panel principal
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(new Color(30, 30, 40));

        // Titre
        JLabel titleLabel = new JLabel("SRUU - Simulation Multi-Agents", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(220, 220, 220));
        titleLabel.setBorder(new EmptyBorder(10, 0, 15, 0));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Panel central
        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        centerSplit.setDividerLocation(800);

        // Panel de simulation
        simPanel = new SimulationPanel();
        simPanel.setPreferredSize(new Dimension(800, 600));
        simPanel.setBackground(new Color(20, 20, 30));
        centerSplit.setLeftComponent(new JScrollPane(simPanel));

        // Panel droit avec onglets
        JTabbedPane tabs = new JTabbedPane();
        
        // Onglet Statistiques
        statsTable = createStatsTable();
        tabs.addTab("Statistics", new JScrollPane(statsTable));
        
        // Onglet Journal
        logArea = new JTextArea();
        logArea.setFont(new Font("Courier", Font.PLAIN, 12));
        logArea.setBackground(new Color(25, 25, 35));
        logArea.setForeground(new Color(200, 200, 200));
        logArea.setEditable(false);
        tabs.addTab("Log", new JScrollPane(logArea));
        
        centerSplit.setRightComponent(tabs);
        mainPanel.add(centerSplit, BorderLayout.CENTER);

        // Barre de statut
        statusLabel = new JLabel("System Ready", JLabel.CENTER);
        statusLabel.setBorder(new EmptyBorder(5, 5, 5, 5));
        statusLabel.setBackground(new Color(40, 40, 50));
        statusLabel.setOpaque(true);
        mainPanel.add(statusLabel, BorderLayout.SOUTH);

        mainFrame.setContentPane(mainPanel);
        
        // Forcer l'affichage
        mainFrame.setVisible(true);
        mainFrame.setState(JFrame.NORMAL);
        mainFrame.toFront();
        mainFrame.repaint();
        
        System.out.println("[GUI] Interface graphique initialisee et affichee");
    }

    private JTable createStatsTable() {
        String[] columns = {"Agent", "Type", "State", "Position", "Incident"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        JTable table = new JTable(model);
        table.setFont(new Font("Arial", Font.PLAIN, 12));
        table.setRowHeight(20);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        
        return table;
    }

    // ============================================================
    // COMPORTEMENT D'ÉCOUTE
    // ============================================================

    private class GUIUpdateListener extends CyclicBehaviour {
        private final MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);

        @Override
        public void action() {
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String content = msg.getContent();
                String sender = msg.getSender().getLocalName();
                
                System.out.println("[GUI] Received from " + sender + ": " + content);
                
                // Traiter les messages
                processMessage(sender, content);
            } else {
                block(100);
            }
        }
    }

    private void processMessage(String sender, String content) {
        SwingUtilities.invokeLater(() -> {
            if (content.startsWith("AGENT_UPDATE:")) {
                String[] parts = content.split(":");
                if (parts.length >= 5) {
                    updateAgent(parts[1], 
                            Integer.parseInt(parts[2]), 
                            Integer.parseInt(parts[3]), 
                            parts[4]);
                }
            } else if (content.startsWith("INCIDENT_REPORT:")) {
                String[] parts = content.split(":");
                if (parts.length >= 6) {
                    addIncident(parts[1], parts[2], parts[3], parts[4], parts[5]);
                }
            } else if (content.startsWith("INCIDENT_UPDATE:")) {
                String[] parts = content.split(":");
                if (parts.length >= 3) {
                    updateIncident(parts[1], parts[2]);
                }
            }
            
            // Ajouter au log
            addLogEntry(sender, content);
            
            // Rafraîchir l'affichage
            refreshStatsTable();
            simPanel.repaint();
        });
    }

    private void updateAgent(String name, int x, int y, String state) {
        AgentVisual av = agents.computeIfAbsent(name, k -> new AgentVisual(name, x, y, state));
        av.x = x;
        av.y = y;
        av.state = state;
    }

    private void addIncident(String id, String type, String severity, String x, String y) {
        IncidentVisual incident = new IncidentVisual(id, type, Integer.parseInt(severity), 
                                               Integer.parseInt(x), Integer.parseInt(y));
        incidents.put(id, incident);
        statusLabel.setText("New Incident: " + id);
    }

    private void updateIncident(String id, String status) {
        IncidentVisual incident = incidents.get(id);
        if (incident != null) {
            incident.status = status;
        }
    }

    private void addLogEntry(String agent, String message) {
        LogEntry entry = new LogEntry(agent, message);
        eventLog.add(entry);
        
        logArea.append(entry.toString() + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
        
        // Limiter la taille du log
        if (eventLog.size() > 1000) {
            eventLog.remove(0);
        }
    }

    private void refreshStatsTable() {
        DefaultTableModel model = (DefaultTableModel) statsTable.getModel();
        model.setRowCount(0);
        
        for (AgentVisual av : agents.values()) {
            model.addRow(new Object[]{
                av.name,
                getAgentType(av.name),
                av.state,
                "(" + av.x + "," + av.y + ")",
                "Active"
            });
        }
    }

    private String getAgentType(String name) {
        if (name.contains("Police")) return "Police";
        if (name.contains("FireTruck")) return "Fire Truck";
        if (name.contains("Ambulance")) return "Ambulance";
        if (name.contains("BCU")) return "BCU";
        if (name.contains("Sensor")) return "Sensor";
        return "Unknown";
    }

    // ============================================================
    // CLASSES INTERNES
    // ============================================================

    private class SimulationPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            
            // Dessiner la grille
            drawGrid(g2d);
            
            // Dessiner les incidents
            for (IncidentVisual incident : incidents.values()) {
                drawIncident(g2d, incident);
            }
            
            // Dessiner les agents
            for (AgentVisual agent : agents.values()) {
                drawAgent(g2d, agent);
            }
            
            g2d.dispose();
        }
        
        private void drawGrid(Graphics2D g2d) {
            g2d.setColor(new Color(50, 50, 60));
            g2d.setStroke(new BasicStroke(1));
            
            int gridSize = 10;
            for (int i = 0; i <= getWidth(); i += gridSize) {
                g2d.drawLine(i, 0, i, getHeight());
                g2d.drawLine(0, i, getWidth(), i);
            }
        }
        
        private void drawIncident(Graphics2D g2d, IncidentVisual incident) {
            Color color = getIncidentColor(incident.type);
            g2d.setColor(color);
            g2d.fillOval(incident.x - 8, incident.y - 8, 16, 16);
            
            g2d.setColor(Color.WHITE);
            g2d.drawString(incident.id, incident.x + 10, incident.y - 10);
        }
        
        private void drawAgent(Graphics2D g2d, AgentVisual agent) {
            Color color = getAgentColor(agent.name);
            g2d.setColor(color);
            g2d.fillRect(agent.x - 4, agent.y - 4, 8, 8);
            
            g2d.setColor(Color.WHITE);
            g2d.drawString(agent.name, agent.x + 6, agent.y - 2);
        }
        
        private Color getIncidentColor(String type) {
            switch (type) {
                case "FIRE": return Color.RED;
                case "MEDICAL": return Color.GREEN;
                case "STRUCTURAL_COLLAPSE": return Color.ORANGE;
                case "BIOHAZARD": return Color.MAGENTA;
                case "CRYOGENIC_LEAK": return Color.CYAN;
                default: return Color.GRAY;
            }
        }
        
        private Color getAgentColor(String name) {
            if (name.contains("Police")) return Color.BLUE;
            if (name.contains("FireTruck")) return Color.RED;
            if (name.contains("Ambulance")) return Color.WHITE;
            if (name.contains("BCU")) return Color.YELLOW;
            if (name.contains("Sensor")) return Color.GRAY;
            return Color.LIGHT_GRAY;
        }
    }

    private static class AgentVisual {
        String name;
        int x, y;
        String state;
        
        AgentVisual(String name, int x, int y, String state) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.state = state;
        }
    }

    private static class IncidentVisual {
        String id, type, status;
        int x, y, severity;
        
        IncidentVisual(String id, String type, int severity, int x, int y) {
            this.id = id;
            this.type = type;
            this.severity = severity;
            this.x = x;
            this.y = y;
            this.status = "OPEN";
        }
    }

    private static class LogEntry {
        String agent, message;
        long timestamp;
        
        LogEntry(String agent, String message) {
            this.agent = agent;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }
        
        @Override
        public String toString() {
            return "[" + new Date(timestamp) + "] " + agent + ": " + message;
        }
    }

    @Override
    protected void takeDown() {
        SwingUtilities.invokeLater(() -> {
            if (mainFrame != null) mainFrame.dispose();
        });
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }
}
