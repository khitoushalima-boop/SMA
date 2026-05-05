package sruu.agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.*;

public class TrafficControllerAgent extends Agent {

    private final Map<String, Corridor> activeCorridors = new HashMap<>();
    
    @Override
    protected void setup() {
        System.out.println("[TRAFFIC_CONTROLLER] Started.");
        
        // Register with DF
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("TRAFFIC_CONTROLLER");
            dfd.setName(getAID());
            dfd.addServices(sd);
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        
        // Add behavior for corridor requests
        addBehaviour(new CorridorRequestListener());
    }
    
    private class CorridorRequestListener extends CyclicBehaviour {
        private final MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                MessageTemplate.MatchContent("CORRIDOR:*"));

        @Override
        public void action() {
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String content = msg.getContent();
                if (content != null && content.startsWith("CORRIDOR:")) {
                    handleCorridorRequest(content);
                }
            } else {
                block(1000);
            }
        }
    }
    
    private void handleCorridorRequest(String content) {
        try {
            String[] parts = content.split(":");
            if (parts.length >= 4) {
                String incidentId = parts[1];
                int x = Integer.parseInt(parts[2]);
                int y = Integer.parseInt(parts[3]);
                
                // Open corridor
                Corridor corridor = new Corridor(incidentId, x, y);
                activeCorridors.put(incidentId, corridor);
                
                // Broadcast corridor opening
                broadcastCorridorStatus(incidentId, "OPEN", x, y);
                
                // Schedule corridor closure
                addBehaviour(new CorridorClosureBehaviour(incidentId, 30000)); // 30 seconds
                
                System.out.println("[TRAFFIC_CONTROLLER] Corridor " + incidentId + " opened at (" + x + "," + y + ")");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void broadcastCorridorStatus(String incidentId, String status, int x, int y) {
        try {
            // Find all emergency units
            DFAgentDescription dfd = new DFAgentDescription();
            DFAgentDescription[] result = DFService.search(this, dfd);
            
            for (DFAgentDescription dfad : result) {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(dfad.getName());
                msg.setContent("CORRIDOR_" + status + ":" + incidentId + ":" + x + ":" + y);
                msg.setOntology("EmergencyOntology");
                send(msg);
            }
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }
    
    private class CorridorClosureBehaviour extends WakerBehaviour {
        private final String incidentId;
        
        CorridorClosureBehaviour(String incidentId, long timeout) {
            super(null, timeout);
            this.incidentId = incidentId;
        }
        
        @Override
        protected void onWake() {
            Corridor corridor = activeCorridors.get(incidentId);
            if (corridor != null) {
                // Close corridor
                broadcastCorridorStatus(incidentId, "CLOSED", corridor.x, corridor.y);
                activeCorridors.remove(incidentId);
                
                System.out.println("[TRAFFIC_CONTROLLER] Corridor " + incidentId + " closed");
            }
        }
    }
    
    private static class Corridor {
        String incidentId;
        int x, y;
        
        Corridor(String incidentId, int x, int y) {
            this.incidentId = incidentId;
            this.x = x;
            this.y = y;
        }
    }
    
    @Override
    protected void takeDown() {
        System.out.println("[TRAFFIC_CONTROLLER] Terminating. Active corridors: " + activeCorridors.size());
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }
}
