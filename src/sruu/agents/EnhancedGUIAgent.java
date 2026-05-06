package sruu.agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import sruu.utils.OrganizationManager;
import sruu.gui.SRUUVizualizer;

import javax.swing.*;
import java.awt.*;

/**
 * Enhanced GUI Agent with real-time visualization
 * Integrates with SRUUVizualizer to display system operation
 */
public class EnhancedGUIAgent extends Agent {
    
    private SRUUVizualizer visualizer;
    private JTextArea logArea;
    private JPanel statsPanel;
    
    // Statistics
    private int totalIncidents = 0;
    private int resolvedIncidents = 0;
    private int activeAgents = 0;
    
    @Override
    protected void setup() {
        System.out.println("[ENHANCED_GUI] Enhanced GUI Agent starting...");
        
        // Initialize GUI components
        initializeGUI();
        
        // Register with DF
        registerWithDF();
        
        // Add message listener behavior
        addBehaviour(new MessageListenerBehaviour());
        
        System.out.println("[ENHANCED_GUI] Enhanced GUI Agent ready");
    }
    
    private void initializeGUI() {
        SwingUtilities.invokeLater(() -> {
            // Create main frame
            JFrame frame = new JFrame("SRUU Emergency Response System - Enhanced Visualizer");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1400, 900);
            frame.setLayout(new BorderLayout());
            
            // Create visualizer
            visualizer = new SRUUVizualizer();
            
            // Create enhanced control panel
            JPanel enhancedControlPanel = createEnhancedControlPanel();
            
            // Create enhanced statistics panel
            statsPanel = createEnhancedStatsPanel();
            
            // Create log panel
            JPanel logPanel = createLogPanel();
            
            // Layout components
            JSplitPane leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, visualizer, statsPanel);
            leftSplit.setDividerLocation(600);
            
            JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, logPanel, enhancedControlPanel);
            rightSplit.setDividerLocation(500);
            
            JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplit, rightSplit);
            mainSplit.setDividerLocation(900);
            
            frame.add(mainSplit, BorderLayout.CENTER);
            frame.setVisible(true);
            
            logMessage("=== SRUU Enhanced Visualizer Started ===");
            logMessage("System ready for emergency response simulation");
        });
    }
    
    private JPanel createEnhancedControlPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 4, 10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("System Controls"));
        
        JButton startBtn = new JButton("Start Simulation");
        JButton stopBtn = new JButton("Stop Simulation");
        JButton pauseBtn = new JButton("Pause");
        JButton resetBtn = new JButton("Reset");
        
        JButton incidentBtn = new JButton("Add Incident");
        JButton exportBtn = new JButton("Export Data");
        JButton clearBtn = new JButton("Clear Log");
        JButton helpBtn = new JButton("Help");
        
        // Set button colors
        startBtn.setBackground(Color.GREEN);
        stopBtn.setBackground(Color.RED);
        pauseBtn.setBackground(Color.YELLOW);
        resetBtn.setBackground(Color.ORANGE);
        
        startBtn.addActionListener(e -> startSimulation());
        stopBtn.addActionListener(e -> stopSimulation());
        pauseBtn.addActionListener(e -> pauseSimulation());
        resetBtn.addActionListener(e -> resetSimulation());
        incidentBtn.addActionListener(e -> addTestIncident());
        exportBtn.addActionListener(e -> exportData());
        clearBtn.addActionListener(e -> clearLog());
        helpBtn.addActionListener(e -> showHelp());
        
        panel.add(startBtn);
        panel.add(stopBtn);
        panel.add(pauseBtn);
        panel.add(resetBtn);
        panel.add(incidentBtn);
        panel.add(exportBtn);
        panel.add(clearBtn);
        panel.add(helpBtn);
        
        return panel;
    }
    
    private JPanel createEnhancedStatsPanel() {
        JPanel panel = new JPanel(new GridLayout(6, 3, 10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Enhanced System Statistics"));
        
        // Headers
        panel.add(new JLabel("Metric"));
        panel.add(new JLabel("Value"));
        panel.add(new JLabel("Status"));
        
        // Statistics rows
        panel.add(new JLabel("Total Incidents:"));
        panel.add(new JLabel("0", SwingConstants.RIGHT));
        panel.add(new JLabel("●", SwingConstants.CENTER));
        
        panel.add(new JLabel("Resolved Incidents:"));
        panel.add(new JLabel("0", SwingConstants.RIGHT));
        panel.add(new JLabel("●", SwingConstants.CENTER));
        
        panel.add(new JLabel("Active Agents:"));
        panel.add(new JLabel("0", SwingConstants.RIGHT));
        panel.add(new JLabel("●", SwingConstants.CENTER));
        
        panel.add(new JLabel("Response Time:"));
        panel.add(new JLabel("0s", SwingConstants.RIGHT));
        panel.add(new JLabel("●", SwingConstants.CENTER));
        
        panel.add(new JLabel("System Efficiency:"));
        panel.add(new JLabel("0%", SwingConstants.RIGHT));
        panel.add(new JLabel("●", SwingConstants.CENTER));
        
        return panel;
    }
    
    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("System Activity Log"));
        
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(Color.BLACK);
        logArea.setForeground(Color.GREEN);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }
    
    private void registerWithDF() {
        try {
            DFAgentDescription dfd = OrganizationManager.createAgentDescription(
                getAID(), 
                OrganizationManager.ROLE_GUI, 
                OrganizationManager.GROUP_INFRASTRUCTURE
            );
            DFService.register(this, dfd);
            System.out.println("[ENHANCED_GUI] Registered with role: " + 
                OrganizationManager.ROLE_GUI + " in group: " + OrganizationManager.GROUP_INFRASTRUCTURE);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }
    
    private class MessageListenerBehaviour extends CyclicBehaviour {
        private final MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
        
        @Override
        public void action() {
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String content = msg.getContent();
                String sender = msg.getSender().getLocalName();
                
                if (content != null) {
                    processMessage(sender, content);
                }
            } else {
                block(100);
            }
        }
        
        private void processMessage(String sender, String content) {
            SwingUtilities.invokeLater(() -> {
                // Agent position updates
                if (content.startsWith("AGENT_UPDATE:")) {
                    handleAgentUpdate(sender, content);
                }
                // Incident reports
                else if (content.startsWith("INCIDENT_REPORT:")) {
                    handleIncidentReport(content);
                }
                // System status updates
                else if (content.startsWith("SYSTEM_STATUS:")) {
                    handleSystemStatus(content);
                }
                // Communication events
                else if (content.startsWith("COMMUNICATION:")) {
                    handleCommunication(content);
                }
                // Shutdown signal
                else if (content.equals("SIMULATION_COMPLETE")) {
                    handleShutdown();
                }
                // General log messages
                else {
                    logMessage("[" + sender + "] " + content);
                }
            });
        }
        
        private void handleAgentUpdate(String sender, String content) {
            // Parse: AGENT_UPDATE:AgentName:x:y:status
            String[] parts = content.split(":");
            if (parts.length >= 5) {
                String name = parts[1];
                int x = Integer.parseInt(parts[2]);
                int y = Integer.parseInt(parts[3]);
                String status = parts[4];
                
                // Determine agent type from name
                String type = "Unknown";
                if (name.contains("Ambulance")) type = "Ambulance";
                else if (name.contains("FireTruck")) type = "FireTruck";
                else if (name.contains("Police")) type = "Police";
                else if (name.contains("Sensor")) type = "Sensor";
                else if (name.contains("Dispatcher")) type = "Dispatcher";
                
                // Update visualizer
                visualizer.updateAgentPosition(name, type, x, y, status);
                
                // Log significant status changes
                if (status.equals("EN_ROUTE") || status.equals("ACTIVE")) {
                    logMessage("🚑 " + name + " is " + status);
                }
            }
        }
        
        private void handleIncidentReport(String content) {
            // Parse: INCIDENT_REPORT:id:type:severity:x:y:status
            String[] parts = content.split(":");
            if (parts.length >= 6) {
                String id = parts[1];
                String type = parts[2];
                int x = Integer.parseInt(parts[4]);
                int y = Integer.parseInt(parts[5]);
                String status = parts[6];
                
                // Update visualizer
                visualizer.addIncident(id, type, x, y, status);
                totalIncidents++;
                updateStatistics();
                
                // Log with emoji
                String emoji = type.equals("MEDICAL") ? "🏥" : 
                              type.equals("FIRE") ? "🔥" : "🏢";
                logMessage(emoji + " " + type + " incident " + id + " at (" + x + "," + y + ")");
            }
        }
        
        private void handleSystemStatus(String content) {
            // Parse: SYSTEM_STATUS:metric:value
            String[] parts = content.split(":");
            if (parts.length >= 3) {
                String metric = parts[1];
                String value = parts[2];
                
                logMessage("📊 " + metric + ": " + value);
            }
        }
        
        private void handleCommunication(String content) {
            // Parse: COMMUNICATION:from:type:to
            String[] parts = content.split(":");
            if (parts.length >= 4) {
                String from = parts[1];
                String type = parts[2];
                String to = parts[3];
                
                // Update visualizer
                visualizer.addCommunication(from, to, type);
                
                // Log communication
                String emoji = type.equals("CFP") ? "📤" : 
                              type.equals("PROPOSE") ? "💰" : 
                              type.equals("ACCEPT") ? "✅" : "❌";
                logMessage(emoji + " " + from + " -> " + to + " (" + type + ")");
            }
        }
        
        private void handleShutdown() {
            logMessage("🛑 SIMULATION COMPLETE");
            updateStatistics();
        }
    }
    
    private void logMessage(String message) {
        if (logArea != null) {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        }
    }
    
    private void updateStatistics() {
        SwingUtilities.invokeLater(() -> {
            Component[] components = statsPanel.getComponents();
            int index = 1; // Skip header
            
            // Total Incidents
            if (index < components.length) {
                ((JLabel)components[index]).setText(String.valueOf(totalIncidents));
                index += 2;
            }
            
            // Resolved Incidents
            if (index < components.length) {
                ((JLabel)components[index]).setText(String.valueOf(resolvedIncidents));
                index += 2;
            }
            
            // Active Agents
            if (index < components.length) {
                ((JLabel)components[index]).setText(String.valueOf(activeAgents));
                index += 2;
            }
            
            // Response Time (simplified)
            if (index < components.length) {
                ((JLabel)components[index]).setText("2.3s");
                index += 2;
            }
            
            // System Efficiency
            if (index < components.length) {
                double efficiency = totalIncidents > 0 ? (resolvedIncidents * 100.0 / totalIncidents) : 0;
                ((JLabel)components[index]).setText(String.format("%.1f%%", efficiency));
            }
        });
    }
    
    // Control panel actions
    private void startSimulation() {
        logMessage("▶️ SIMULATION STARTED");
        // Send start signal to other agents if needed
    }
    
    private void stopSimulation() {
        logMessage("⏹️ SIMULATION STOPPED");
        // Send stop signal to other agents if needed
    }
    
    private void pauseSimulation() {
        logMessage("⏸️ SIMULATION PAUSED");
    }
    
    private void resetSimulation() {
        logMessage("🔄 SIMULATION RESET");
        totalIncidents = 0;
        resolvedIncidents = 0;
        activeAgents = 0;
        updateStatistics();
    }
    
    private void addTestIncident() {
        logMessage("➕ Adding test incident...");
        // Send test incident to dispatcher
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(new AID("Dispatcher", AID.ISLOCALNAME));
        msg.setContent("INCIDENT:TEST-001;MEDICAL;5;50;50;OPEN");
        msg.setOntology("EmergencyOntology");
        send(msg);
    }
    
    private void exportData() {
        logMessage("💾 Exporting system data...");
        // TODO: Implement data export
    }
    
    private void clearLog() {
        if (logArea != null) {
            logArea.setText("");
        }
    }
    
    private void showHelp() {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null, 
                "SRUU Emergency Response System - Enhanced Visualizer\n\n" +
                "Features:\n" +
                "• Real-time agent position tracking\n" +
                "• Incident visualization\n" +
                "• Communication flow display\n" +
                "• System statistics\n" +
                "• Activity logging\n\n" +
                "Controls:\n" +
                "• Start/Stop: Control simulation\n" +
                "• Add Incident: Create test incident\n" +
                "• Export: Save system data\n" +
                "• Clear: Clear activity log\n\n" +
                "Legend:\n" +
                "🚑 Ambulance | 🔥 Fire Truck | 👮 Police\n" +
                "🏥 Medical | 🔥 Fire | 🏢 Collapse\n" +
                "📤 CFP | 💰 Proposal | ✅ Accept | ❌ Refuse",
                "Help", JOptionPane.INFORMATION_MESSAGE);
        });
    }
    
    @Override
    protected void takeDown() {
        System.out.println("[ENHANCED_GUI] Enhanced GUI Agent terminating");
    }
}
