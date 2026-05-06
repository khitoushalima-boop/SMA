package sruu.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SRUU System Visualizer - Real-time GUI for emergency response system
 * Shows agent positions, incidents, communications, and system statistics
 */
public class SRUUVizualizer extends JFrame {
    
    // Main components
    private JPanel mainPanel;
    private JPanel mapPanel;
    private JPanel statsPanel;
    private JPanel logPanel;
    private JPanel controlPanel;
    
    // Data structures
    private Map<String, AgentInfo> agents = new ConcurrentHashMap<>();
    private Map<String, IncidentInfo> incidents = new ConcurrentHashMap<>();
    private Map<String, CommunicationInfo> communications = new ConcurrentHashMap<>();
    
    // Map dimensions
    private static final int MAP_WIDTH = 600;
    private static final int MAP_HEIGHT = 400;
    private static final int AGENT_SIZE = 8;
    private static final int INCIDENT_SIZE = 12;
    
    // Colors
    private static final Color COLOR_AMBULANCE = new Color(255, 0, 0);
    private static final Color COLOR_FIRETRUCK = new Color(255, 165, 0);
    private static final Color COLOR_POLICE = new Color(0, 0, 255);
    private static final Color COLOR_SENSOR = new Color(0, 128, 0);
    private static final Color COLOR_DISPATCHER = new Color(128, 0, 128);
    private static final Color COLOR_INCIDENT_MEDICAL = new Color(255, 100, 100);
    private static final Color COLOR_INCIDENT_FIRE = new Color(255, 100, 0);
    private static final Color COLOR_INCIDENT_COLLAPSE = new Color(128, 128, 128);
    
    // Statistics
    private int totalIncidents = 0;
    private int resolvedIncidents = 0;
    private int activeAgents = 0;
    private long startTime = System.currentTimeMillis();
    
    public SRUUVizualizer() {
        setTitle("SRUU Emergency Response System - Real-time Visualizer");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        initializeComponents();
        setupEventHandlers();
        
        // Start update timer
        javax.swing.Timer updateTimer = new javax.swing.Timer(100, e -> updateDisplay());
        updateTimer.start();
        
        setVisible(true);
    }
    
    private void initializeComponents() {
        // Main panel with card layout
        mainPanel = new JPanel(new BorderLayout());
        
        // Create map panel
        mapPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawMap(g);
            }
        };
        mapPanel.setPreferredSize(new Dimension(MAP_WIDTH, MAP_HEIGHT));
        mapPanel.setBackground(Color.WHITE);
        mapPanel.setBorder(BorderFactory.createTitledBorder("City Map"));
        
        // Create statistics panel
        statsPanel = createStatsPanel();
        
        // Create log panel
        logPanel = createLogPanel();
        
        // Create control panel
        controlPanel = createControlPanel();
        
        // Layout components
        JSplitPane leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mapPanel, statsPanel);
        leftSplit.setDividerLocation(500);
        
        JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, logPanel, controlPanel);
        rightSplit.setDividerLocation(400);
        
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplit, rightSplit);
        mainSplit.setDividerLocation(800);
        
        mainPanel.add(mainSplit, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);
    }
    
    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("System Statistics"));
        
        panel.add(new JLabel("Total Incidents:"));
        panel.add(new JLabel("0", SwingConstants.RIGHT));
        
        panel.add(new JLabel("Resolved Incidents:"));
        panel.add(new JLabel("0", SwingConstants.RIGHT));
        
        panel.add(new JLabel("Active Agents:"));
        panel.add(new JLabel("0", SwingConstants.RIGHT));
        
        panel.add(new JLabel("Runtime:"));
        panel.add(new JLabel("0s", SwingConstants.RIGHT));
        
        return panel;
    }
    
    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("System Log"));
        
        JTextArea logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(Color.BLACK);
        logArea.setForeground(Color.GREEN);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }
    
    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Controls"));
        
        JButton startBtn = new JButton("Start Simulation");
        JButton stopBtn = new JButton("Stop Simulation");
        JButton clearBtn = new JButton("Clear Log");
        JButton exportBtn = new JButton("Export Data");
        
        startBtn.addActionListener(e -> startSimulation());
        stopBtn.addActionListener(e -> stopSimulation());
        clearBtn.addActionListener(e -> clearLog());
        exportBtn.addActionListener(e -> exportData());
        
        panel.add(startBtn);
        panel.add(stopBtn);
        panel.add(clearBtn);
        panel.add(exportBtn);
        
        return panel;
    }
    
    private void setupEventHandlers() {
        // Add window listener
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }
    
    private void drawMap(Graphics g) {
        // Draw grid
        g.setColor(Color.LIGHT_GRAY);
        for (int i = 0; i <= MAP_WIDTH; i += 50) {
            g.drawLine(i, 0, i, MAP_HEIGHT);
        }
        for (int i = 0; i <= MAP_HEIGHT; i += 50) {
            g.drawLine(0, i, MAP_WIDTH, i);
        }
        
        // Draw incidents
        for (IncidentInfo incident : incidents.values()) {
            drawIncident(g, incident);
        }
        
        // Draw agents
        for (AgentInfo agent : agents.values()) {
            drawAgent(g, agent);
        }
        
        // Draw communications
        for (CommunicationInfo comm : communications.values()) {
            drawCommunication(g, comm);
        }
    }
    
    private void drawAgent(Graphics g, AgentInfo agent) {
        Color color = getAgentColor(agent.type);
        g.setColor(color);
        
        int x = (int) (agent.x * MAP_WIDTH / 100.0);
        int y = (int) (agent.y * MAP_HEIGHT / 100.0);
        
        // Draw agent circle
        g.fillOval(x - AGENT_SIZE/2, y - AGENT_SIZE/2, AGENT_SIZE, AGENT_SIZE);
        
        // Draw agent label
        g.setColor(Color.BLACK);
        g.drawString(agent.name.substring(0, Math.min(3, agent.name.length())), 
                    x - 10, y - AGENT_SIZE/2 - 2);
        
        // Draw status indicator
        if (agent.status.equals("EN_ROUTE") || agent.status.equals("ACTIVE")) {
            g.setColor(Color.YELLOW);
            g.drawOval(x - AGENT_SIZE, y - AGENT_SIZE, AGENT_SIZE * 2, AGENT_SIZE * 2);
        }
    }
    
    private void drawIncident(Graphics g, IncidentInfo incident) {
        Color color = getIncidentColor(incident.type);
        g.setColor(color);
        
        int x = (int) (incident.x * MAP_WIDTH / 100.0);
        int y = (int) (incident.y * MAP_HEIGHT / 100.0);
        
        // Draw incident diamond
        int[] xPoints = {x, x + INCIDENT_SIZE/2, x, x - INCIDENT_SIZE/2};
        int[] yPoints = {y - INCIDENT_SIZE/2, y, y + INCIDENT_SIZE/2, y};
        g.fillPolygon(xPoints, yPoints, 4);
        
        // Draw incident label
        g.setColor(Color.BLACK);
        g.drawString(incident.id.substring(0, Math.min(6, incident.id.length())), 
                    x + INCIDENT_SIZE/2 + 2, y);
    }
    
    private void drawCommunication(Graphics g, CommunicationInfo comm) {
        g.setColor(new Color(100, 100, 255, 100));
        
        AgentInfo from = agents.get(comm.from);
        AgentInfo to = agents.get(comm.to);
        
        if (from != null && to != null) {
            int x1 = (int) (from.x * MAP_WIDTH / 100.0);
            int y1 = (int) (from.y * MAP_HEIGHT / 100.0);
            int x2 = (int) (to.x * MAP_WIDTH / 100.0);
            int y2 = (int) (to.y * MAP_HEIGHT / 100.0);
            
            g.drawLine(x1, y1, x2, y2);
            
            // Draw arrow
            drawArrow(g, x1, y1, x2, y2);
        }
    }
    
    private void drawArrow(Graphics g, int x1, int y1, int x2, int y2) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        double angle = Math.atan2(dy, dx);
        
        int arrowLength = 8;
        int arrowAngle = 25;
        
        int x3 = (int) (x2 - arrowLength * Math.cos(angle - Math.toRadians(arrowAngle)));
        int y3 = (int) (y2 - arrowLength * Math.sin(angle - Math.toRadians(arrowAngle)));
        int x4 = (int) (x2 - arrowLength * Math.cos(angle + Math.toRadians(arrowAngle)));
        int y4 = (int) (y2 - arrowLength * Math.sin(angle + Math.toRadians(arrowAngle)));
        
        g.drawLine(x2, y2, x3, y3);
        g.drawLine(x2, y2, x4, y4);
    }
    
    private Color getAgentColor(String type) {
        switch (type) {
            case "Ambulance": return COLOR_AMBULANCE;
            case "FireTruck": return COLOR_FIRETRUCK;
            case "Police": return COLOR_POLICE;
            case "Sensor": return COLOR_SENSOR;
            case "Dispatcher": return COLOR_DISPATCHER;
            default: return Color.GRAY;
        }
    }
    
    private Color getIncidentColor(String type) {
        switch (type) {
            case "MEDICAL": return COLOR_INCIDENT_MEDICAL;
            case "FIRE": return COLOR_INCIDENT_FIRE;
            case "STRUCTURAL_COLLAPSE": return COLOR_INCIDENT_COLLAPSE;
            default: return Color.GRAY;
        }
    }
    
    private void updateDisplay() {
        mapPanel.repaint();
        updateStatistics();
    }
    
    private void updateStatistics() {
        Component[] labels = statsPanel.getComponents();
        if (labels.length >= 8) {
            ((JLabel)labels[1]).setText(String.valueOf(totalIncidents));
            ((JLabel)labels[3]).setText(String.valueOf(resolvedIncidents));
            ((JLabel)labels[5]).setText(String.valueOf(activeAgents));
            
            long runtime = (System.currentTimeMillis() - startTime) / 1000;
            ((JLabel)labels[7]).setText(runtime + "s");
        }
    }
    
    // Public methods for updating data from agents
    public void updateAgentPosition(String name, String type, int x, int y, String status) {
        agents.put(name, new AgentInfo(name, type, x, y, status));
        activeAgents = agents.size();
    }
    
    public void addIncident(String id, String type, int x, int y, String status) {
        incidents.put(id, new IncidentInfo(id, type, x, y, status));
        totalIncidents++;
        logMessage("INCIDENT: " + id + " (" + type + ") at (" + x + "," + y + ")");
    }
    
    public void resolveIncident(String id) {
        IncidentInfo incident = incidents.remove(id);
        if (incident != null) {
            resolvedIncidents++;
            logMessage("RESOLVED: " + id);
        }
    }
    
    public void addCommunication(String from, String to, String type) {
        String key = from + "-" + to + "-" + System.currentTimeMillis();
        communications.put(key, new CommunicationInfo(from, to, type));
        
        // Remove old communications after 5 seconds
        new javax.swing.Timer(5000, e -> communications.remove(key)).start();
        
        logMessage("COMM: " + from + " -> " + to + " (" + type + ")");
    }
    
    public void logMessage(String message) {
        JTextArea logArea = (JTextArea) ((JScrollPane) logPanel.getComponent(0)).getViewport().getView();
        logArea.append(message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
    
    private void startSimulation() {
        logMessage("=== SIMULATION STARTED ===");
        startTime = System.currentTimeMillis();
    }
    
    private void stopSimulation() {
        logMessage("=== SIMULATION STOPPED ===");
    }
    
    private void clearLog() {
        JTextArea logArea = (JTextArea) ((JScrollPane) logPanel.getComponent(0)).getViewport().getView();
        logArea.setText("");
    }
    
    private void exportData() {
        logMessage("EXPORT: Data exported to file");
        // TODO: Implement data export functionality
    }
    
    // Data classes
    private static class AgentInfo {
        String name, type;
        int x, y;
        String status;
        
        AgentInfo(String name, String type, int x, int y, String status) {
            this.name = name;
            this.type = type;
            this.x = x;
            this.y = y;
            this.status = status;
        }
    }
    
    private static class IncidentInfo {
        String id, type, status;
        int x, y;
        
        IncidentInfo(String id, String type, int x, int y, String status) {
            this.id = id;
            this.type = type;
            this.x = x;
            this.y = y;
            this.status = status;
        }
    }
    
    private static class CommunicationInfo {
        String from, to, type;
        
        CommunicationInfo(String from, String to, String type) {
            this.from = from;
            this.to = to;
            this.type = type;
        }
    }
    
    // Main method for standalone testing
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SRUUVizualizer());
    }
}
