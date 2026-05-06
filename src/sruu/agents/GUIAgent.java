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

public class GUIAgent extends Agent {
    
    private Map<String, AgentInfo> agents = new HashMap<>();
    private int totalIncidents = 0;
    
    protected void setup() {
        System.out.println("[GUI] GUI Agent starting...");
        
        // Register with DF
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setType("GUI");
            sd.setName("GUI-Monitor");
            dfd.addServices(sd);
            DFService.register(this, dfd);
            System.out.println("[GUI] Registered with role: GUI in group: Infrastructure");
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        
        // Listen for agent updates
        addBehaviour(new CyclicBehaviour() {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    String content = msg.getContent();
                    if (content.startsWith("AGENT_UPDATE:")) {
                        parseAgentUpdate(content);
                    } else if (content.startsWith("INCIDENT:")) {
                        totalIncidents++;
                    }
                }
                block();
            }
        });
        
        // Refresh display every 2 seconds
        addBehaviour(new TickerBehaviour(this, 2000) {
            protected void onTick() {
                displayDashboard();
            }
        });
    }
    
    private void parseAgentUpdate(String data) {
        // Format: AGENT_UPDATE:TYPE:NAME:X:Y:STATUS
        String[] parts = data.split(":");
        if (parts.length >= 6) {
            String type = parts[1];
            String name = parts[2];
            int x = Integer.parseInt(parts[3]);
            int y = Integer.parseInt(parts[4]);
            String status = parts[5];
            agents.put(name, new AgentInfo(type, name, x, y, status));
        }
    }
    
    private void displayDashboard() {
        // Clear screen
        System.out.print("\033[H\033[2J");
        System.out.flush();
        
        System.out.println("======================================================================");
        System.out.println("  SRUU - Emergency Response System");
        System.out.println("======================================================================");
        System.out.println();
        
        // ===== ORGANIZATION STRUCTURE =====
        System.out.println("+------------------+------------------+------------------+");
        System.out.println("|     GROUP        |      ROLE        |     AGENTS       |");
        System.out.println("+------------------+------------------+------------------+");
        
        // Group: Coordination
        System.out.println("| Coordination     | Dispatcher       | Dispatcher       |");
        
        // Group: Infrastructure
        System.out.println("| Infrastructure   | GUI              | GUI              |");
        System.out.println("| Infrastructure   | Logger           | Logger           |");
        System.out.println("| Infrastructure   | MedicalCoord     | MedicalCoord     |");
        System.out.println("| Infrastructure   | TrafficController| TrafficController|");
        
        // Group: Response
        for (AgentInfo info : agents.values()) {
            if (info.type.equals("Ambulance")) {
                System.out.printf("| Response         | Ambulance        | %-16s |\n", info.name);
            }
        }
        for (AgentInfo info : agents.values()) {
            if (info.type.equals("FireTruck")) {
                System.out.printf("| Response         | FireTruck        | %-16s |\n", info.name);
            }
        }
        for (AgentInfo info : agents.values()) {
            if (info.type.equals("Police")) {
                System.out.printf("| Response         | Police           | %-16s |\n", info.name);
            }
        }
        
        // Group: Sensors
        for (AgentInfo info : agents.values()) {
            if (info.type.equals("Sensor")) {
                System.out.printf("| Sensors          | Sensor           | %-16s |\n", info.name);
            }
        }
        
        System.out.println("+------------------+------------------+------------------+");
        System.out.println();
        
        // ===== AGENT STATUS =====
        System.out.println("----------------------------------------------------------------------");
        System.out.println("AGENT STATUS");
        System.out.println("----------------------------------------------------------------------");
        
        if (agents.isEmpty()) {
            System.out.println("  No agents reporting yet...");
        } else {
            for (AgentInfo info : agents.values()) {
                System.out.printf("  %-15s | Position: (%3d,%3d) | Status: %-12s\n",
                                  info.name, info.x, info.y, info.status);
            }
        }
        
        System.out.println();
        System.out.println("----------------------------------------------------------------------");
        System.out.println("STATISTICS");
        System.out.println("----------------------------------------------------------------------");
        System.out.printf("  Total Incidents Received: %d\n", totalIncidents);
        System.out.printf("  Active Agents: %d\n", agents.size());
        
        System.out.println();
        System.out.println("======================================================================");
        System.out.println("  AGR Model: Agent - Group - Role");
        System.out.println("  BDI: Beliefs -> Desires -> Intentions");
        System.out.println("  CNP: CFP -> Propose -> Accept/Reject");
        System.out.println("======================================================================");
    }
    
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
}
