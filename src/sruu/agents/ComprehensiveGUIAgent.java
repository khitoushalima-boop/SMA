package sruu.agents;

import jade.core.Agent;
import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;

import java.util.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Comprehensive GUI Agent - Full System Display
 * Shows agents, incidents, statistics, BDI, CNP, and AGR organization
 */
public class ComprehensiveGUIAgent extends Agent {
    
    private Map<String, AgentInfo> agents = new HashMap<>();
    private List<IncidentInfo> activeIncidents = new ArrayList<>();
    private List<ContractNetInfo> contractNetHistory = new ArrayList<>();
    private int totalIncidents = 0;
    private int resolvedIncidents = 0;
    private long startTime;
    private String lastCFPIncident = null;
    private String lastProposal = null;
    private String lastAssignment = null;
    
    protected void setup() {
        startTime = System.currentTimeMillis();
        System.out.println("[COMPREHENSIVE_GUI] Comprehensive GUI Agent starting...");
        
        // Register with DF
        registerWithDF();
        
        // Main behaviour to receive updates
        addBehaviour(new CyclicBehaviour() {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    processMessage(msg);
                }
                block();
            }
        });
        
        // Display refresh behaviour (every second)
        addBehaviour(new TickerBehaviour(this, 1000) {
            protected void onTick() {
                displayComprehensiveDashboard();
            }
        });
    }
    
    private void registerWithDF() {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setType("GUI");
            sd.setName("Comprehensive-Dashboard");
            dfd.addServices(sd);
            DFService.register(this, dfd);
            System.out.println("[COMPREHENSIVE_GUI] Registered with role: GUI in group: Infrastructure");
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }
    
    private void processMessage(ACLMessage msg) {
        String content = msg.getContent();
        String sender = msg.getSender().getLocalName();
        
        // Agent position updates
        if (content.startsWith("AGENT_UPDATE:")) {
            parseAgentUpdate(content);
        }
        // Incident reports
        else if (content.startsWith("INCIDENT:")) {
            parseIncident(content);
        }
        // Incident assigned
        else if (content.contains("Assigned")) {
            parseAssignment(content);
        }
        // Incident resolved
        else if (content.contains("RESOLVED")) {
            resolvedIncidents++;
            // Remove from active incidents
            activeIncidents.removeIf(inc -> content.contains(inc.id));
        }
        // Contract Net events
        else if (content.startsWith("CFP sent") || content.contains("proposed with utility") || 
                   content.contains("Assigned")) {
            parseContractNetEvent(content);
        }
        // Shutdown signal
        else if (content.equals("SIMULATION_COMPLETE")) {
            System.out.println("[COMPREHENSIVE_GUI] Simulation complete, shutting down...");
            doDelete();
        }
    }
    
    private void parseAgentUpdate(String data) {
        // Format: AGENT_UPDATE:<type>:<name>:<x>:<y>:<status>
        String[] parts = data.split(":");
        if (parts.length >= 6) {
            String type = parts[1];
            String name = parts[2];
            int x = Integer.parseInt(parts[3]);
            int y = Integer.parseInt(parts[4]);
            String status = parts[5];
            
            // Determine agent type from name
            String agentType = "Unknown";
            if (name.contains("Ambulance")) agentType = "Ambulance";
            else if (name.contains("FireTruck")) agentType = "FireTruck";
            else if (name.contains("Police")) agentType = "Police";
            else if (name.contains("Sensor")) agentType = "Sensor";
            else if (name.contains("Dispatcher")) agentType = "Dispatcher";
            
            agents.put(name, new AgentInfo(agentType, name, x, y, status));
        }
    }
    
    private void parseIncident(String data) {
        // Format: INCIDENT:<id>:<type>:<severity>:<x>:<y>
        String[] parts = data.split(":");
        if (parts.length >= 6) {
            String id = parts[1];
            String type = parts[2];
            int severity = Integer.parseInt(parts[3]);
            int x = Integer.parseInt(parts[4]);
            int y = Integer.parseInt(parts[5]);
            
            activeIncidents.add(new IncidentInfo(id, type, severity, x, y, null));
            totalIncidents++;
        }
    }
    
    private void parseAssignment(String data) {
        // Extract incident ID and assigned unit
        String[] parts = data.split(" ");
        if (parts.length >= 3) {
            String incidentId = parts[1];
            String assignedTo = parts[3];
            
            for (IncidentInfo incident : activeIncidents) {
                if (incident.id.equals(incidentId)) {
                    incident.assignedTo = assignedTo;
                    break;
                }
            }
        }
    }
    
    private void parseContractNetEvent(String data) {
        if (data.contains("CFP sent")) {
            String[] parts = data.split(" ");
            if (parts.length >= 4) {
                lastCFPIncident = parts[3];
            }
        } else if (data.contains("proposed with utility")) {
            String[] parts = data.split(" ");
            if (parts.length >= 4) {
                lastProposal = parts[1];
            }
        } else if (data.contains("Assigned")) {
            String[] parts = data.split(" ");
            if (parts.length >= 3) {
                lastAssignment = parts[3];
            }
        }
        
        try {
            double scoreValue = Double.parseDouble(lastProposal);
            contractNetHistory.add(new ContractNetInfo(lastCFPIncident, lastAssignment, scoreValue));
        } catch (NumberFormatException e) {
            contractNetHistory.add(new ContractNetInfo(lastCFPIncident, lastAssignment, 0.0));
        }
    }
    
    private void displayComprehensiveDashboard() {
        // Clear console
        clearConsole();
        
        long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
        
        // ========== HEADER ==========
        displayHeader(elapsedSeconds);
        
        // ========== STATISTICS ==========
        displayStatistics();
        
        // ========== RESPONSE UNITS ==========
        displayResponseUnits();
        
        // ========== INFRASTRUCTURE AGENTS ==========
        displayInfrastructureAgents();
        
        // ========== ACTIVE INCIDENTS ==========
        displayActiveIncidents();
        
        // ========== BDI REASONING STATUS ==========
        displayBDIStatus();
        
        // ========== CONTRACT NET STATUS ==========
        displayContractNetStatus();
        
        // ========== AGR ORGANIZATION ==========
        displayAGROrganization();
        
        System.out.println("\n" + "═".repeat(70));
    }
    
    private void displayHeader(long elapsedSeconds) {
        System.out.println("╔════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                    🚑 SRUU - Système de Réponse aux Urgences Urbaines 🚑                    ║");
        System.out.println("║                           Emergency Response System                           ║");
        System.out.println("╠════════════════════════════════════════════════════════════════════╣");
        System.out.println("║ Time: " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + 
                           "                    Elapsed: " + elapsedSeconds + " sec                    ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════════╝");
    }
    
    private void displayStatistics() {
        System.out.println("\n📊 STATISTIQUES / STATISTICS:");
        System.out.println("   ┌─────────────────────────────────────────────────┐");
        System.out.printf("   │ Total Incidents (incidents totaux)    : %-30d │\n", totalIncidents);
        System.out.printf("   │ Resolved (résolus)                    : %-30d │\n", resolvedIncidents);
        System.out.printf("   │ Active (actifs)                       : %-30d │\n", activeIncidents.size());
        System.out.printf("   │ Efficiency (efficacité)               : %-30d%% │\n", 
                          totalIncidents > 0 ? (resolvedIncidents * 100 / totalIncidents) : 0);
        System.out.println("   └─────────────────────────────────────────────────┘");
    }
    
    private void displayResponseUnits() {
        System.out.println("\n🚨 UNITÉS D'INTERVENTION / RESPONSE UNITS:");
        System.out.println("   ┌─────────────────────────────────────────────────┐");
        
        // Police
        System.out.println("   │ 🚓 POLICE (Patrouille)                                   │");
        for (AgentInfo info : agents.values()) {
            if (info.type.equals("Police")) {
                String icon = info.status.equals("PATROLLING") ? "🟢" : "🔴";
                System.out.printf("   │    %s %-20s  Position: (%3d,%3d)  Status: %-10s │\n",
                                  icon, info.name, info.x, info.y, info.status);
            }
        }
        
        // Ambulances
        System.out.println("   │                                                           │");
        System.out.println("   │ 🚑 AMBULANCES (Réponse médicale)                         │");
        for (AgentInfo info : agents.values()) {
            if (info.type.equals("Ambulance")) {
                String icon = info.status.equals("EN_ROUTE") ? "🚑" : 
                              (info.status.equals("IDLE") ? "💚" : "⚠️");
                System.out.printf("   │    %s %-20s  Position: (%3d,%3d)  Status: %-10s │\n",
                                  icon, info.name, info.x, info.y, info.status);
            }
        }
        
        // FireTrucks
        System.out.println("   │                                                           │");
        System.out.println("   │ 🚒 FIRE TRUCKS (Lutte contre l'incendie)                 │");
        for (AgentInfo info : agents.values()) {
            if (info.type.equals("FireTruck")) {
                String icon = info.status.equals("EN_ROUTE") ? "🚒" : 
                              (info.status.equals("IDLE") ? "💚" : "⚠️");
                System.out.printf("   │    %s %-20s  Position: (%3d,%3d)  Status: %-10s │\n",
                                  icon, info.name, info.x, info.y, info.status);
            }
        }
        System.out.println("   └─────────────────────────────────────────────────┘");
    }
    
    private void displayInfrastructureAgents() {
        System.out.println("\n🏢 AGENTS D'INFRASTRUCTURE / INFRASTRUCTURE AGENTS:");
        System.out.println("   ┌─────────────────────────────────────────────────┐");
        System.out.println("   │   🎯 Dispatcher     : Coordination des interventions   │");
        System.out.println("   │   📝 Logger         : Enregistrement des événements    │");
        System.out.println("   │   🏥 Medical Coord  : Gestion des hôpitaux             │");
        System.out.println("   │   🚦 Traffic Ctrl   : Gestion du trafic                │");
        System.out.println("   │   🖥️ GUI            : Tableau de bord complet           │");
        System.out.println("   └─────────────────────────────────────────────────┘");
    }
    
    private void displayActiveIncidents() {
        if (!activeIncidents.isEmpty()) {
            System.out.println("\n🆘 INCIDENTS ACTIFS / ACTIVE INCIDENTS:");
            System.out.println("   ┌─────────────────────────────────────────────────┐");
            for (IncidentInfo inc : activeIncidents) {
                String icon;
                switch (inc.type) {
                    case "MEDICAL": icon = "🏥"; break;
                    case "FIRE": icon = "🔥"; break;
                    case "STRUCTURAL_COLLAPSE": icon = "🏚️"; break;
                    default: icon = "⚠️";
                }
                String severityIcon = inc.severity > 7 ? "🔴" : (inc.severity > 4 ? "🟠" : "🟡");
                System.out.printf("   │ %s %-12s %s Niv.%d  Position:(%3d,%3d)  → %-15s │\n",
                                  icon, inc.type, severityIcon, inc.severity, inc.x, inc.y,
                                  inc.assignedTo != null ? inc.assignedTo : "En attente");
            }
            System.out.println("   └─────────────────────────────────────────────────┘");
        }
    }
    
    private void displayBDIStatus() {
        System.out.println("\n🧠 ÉTAT DU RAISONNEMENT BDI / BDI REASONING STATUS:");
        System.out.println("   ┌─────────────────────────────────────────────────┐");
        
        // Show BDI status for each agent type
        Map<String, Integer> statusCounts = new HashMap<>();
        for (AgentInfo info : agents.values()) {
            statusCounts.put(info.status, statusCounts.getOrDefault(info.status, 0) + 1);
        }
        
        System.out.println("   │   Beliefs (Croyances)    : Position, État, Mission   │");
        System.out.println("   │   Desires (Désirs)       : Répondre, Retour     │");
        System.out.println("   │   Intentions (Intentions): En route, Patrouille     │");
        System.out.println("   └─────────────────────────────────────────────────┘");
        
        // Show status breakdown
        System.out.println("   │   Status Distribution:                             │");
        for (Map.Entry<String, Integer> entry : statusCounts.entrySet()) {
            String icon = entry.getKey().equals("IDLE") ? "💚" : 
                         entry.getKey().equals("EN_ROUTE") ? "🚑" :
                         entry.getKey().equals("PATROLLING") ? "🟢" : "⚠️";
            System.out.printf("   │    %s %-15s: %d agents                        │\n",
                                  icon, entry.getKey(), entry.getValue());
        }
        System.out.println("   └─────────────────────────────────────────────────┘");
    }
    
    private void displayContractNetStatus() {
        System.out.println("\n📡 PROTOCOLE CONTRACT NET (CNP) - Dernière négociation:");
        System.out.println("   ┌─────────────────────────────────────────────────┐");
        System.out.println("   │   1. Dispatcher envoie CFP (Appel à propositions)         │");
        System.out.println("   │   2. Unités calculent leur utilité (Utility)            │");
        System.out.println("   │   3. Unités envoient leurs propositions (Proposals)     │");
        System.out.println("   │   4. Dispatcher choisit la meilleure offre ✓            │");
        System.out.println("   │   5. Unité sélectionnée exécute la mission              │");
        System.out.println("   └─────────────────────────────────────────────────┘");
        
        // Show last CNP event if available
        if (!contractNetHistory.isEmpty()) {
            ContractNetInfo lastCNP = contractNetHistory.get(contractNetHistory.size() - 1);
            if (lastCNP != null) {
                System.out.println("   │   Dernière transaction:                               │");
                System.out.printf("   │   Incident: %s → Unité: %s (Score: %.3f)         │\n",
                                  lastCNP.incidentId, lastCNP.assignedUnit, lastCNP.score);
            }
        }
        System.out.println("   └─────────────────────────────────────────────────┘");
    }
    
    private void displayAGROrganization() {
        System.out.println("\n🏢 ORGANISATION AGR (Agent-Groupe-Rôle):");
        System.out.println("   ┌─────────────────────────────────────────────────┐");
        System.out.println("   │   Groupe: Coordination   → Rôle: Dispatcher             │");
        System.out.println("   │   Groupe: Response       → Rôles: Ambulance, FireTruck  │");
        System.out.println("   │   Groupe: Sensors        → Rôle: Sensor                 │");
        System.out.println("   │   Groupe: Infrastructure → Rôles: GUI, Logger, Medical, Traffic │");
        System.out.println("   └─────────────────────────────────────────────────┘");
    }
    
    private void clearConsole() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
    
    // Inner classes
    private class AgentInfo {
        String type, name, status;
        int x, y;
        
        AgentInfo(String type, String name, int x, int y, String status) {
            this.type = type;
            this.name = name;
            this.x = x;
            this.y = y;
            this.status = status;
        }
    }
    
    private class IncidentInfo {
        String id, type, assignedTo;
        int severity, x, y;
        
        IncidentInfo(String id, String type, int severity, int x, int y, String assignedTo) {
            this.id = id;
            this.type = type;
            this.severity = severity;
            this.x = x;
            this.y = y;
            this.assignedTo = assignedTo;
        }
    }
    
    private class ContractNetInfo {
        String incidentId, assignedUnit;
        double score;
        
        ContractNetInfo(String incidentId, String assignedUnit, double score) {
            this.incidentId = incidentId;
            this.assignedUnit = assignedUnit;
            this.score = score;
        }
    }
}
