package sruu.agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import sruu.ontology.Incident;
import sruu.ontology.IncidentType;
import sruu.utils.OrganizationManager;
import java.util.Random;

public class SensorAgent extends Agent {

    private int x, y;
    private String zoneName;
    private Random random = new Random();
    private int incidentCounter = 0;
    private boolean shutdownRequested = false;
    private static final int MAX_INCIDENTS = 5; // Limit for proper termination

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length >= 2) {
            x = Integer.parseInt((String) args[0]);
            y = Integer.parseInt((String) args[1]);
            // Use agent name to determine zone
            String agentName = getLocalName();
            if (agentName.contains("ZoneA")) zoneName = "Zone_A";
            else if (agentName.contains("ZoneB")) zoneName = "Zone_B";
            else if (agentName.contains("ZoneC")) zoneName = "Zone_C";
            else if (agentName.contains("ZoneD")) zoneName = "Zone_D";
            else zoneName = "Zone_A";
        } else {
            x = 50; y = 50; zoneName = "Zone_A";
        }

        System.out.println("[SENSOR] " + getLocalName() + " started at (" + x + "," + y + ")");

        // Register with DF using AGR model (Agent-Groupe-Rôle)
        try {
            DFAgentDescription dfd = OrganizationManager.createAgentDescription(
                getAID(), 
                OrganizationManager.ROLE_SENSOR, 
                OrganizationManager.GROUP_SENSORS
            );
            DFService.register(this, dfd);
            System.out.println("[SENSOR] " + getLocalName() + " registered with role: " + 
                OrganizationManager.ROLE_SENSOR + " in group: " + OrganizationManager.GROUP_SENSORS);
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        // Add behavior for incident generation
        addBehaviour(new IncidentGeneratorBehaviour(this, 8000));
        
        // Add behavior for coordination signals (shutdown)
        addBehaviour(new CoordinationListenerBehaviour());
        
        // Send initial position update to GUI
        sendUpdateToGUI();
    }

    private class IncidentGeneratorBehaviour extends TickerBehaviour {
        IncidentGeneratorBehaviour(Agent a, long period) { 
            super(a, period); 
        }

        @Override
        protected void onTick() {
            if (shutdownRequested) {
                myAgent.doDelete();
                return;
            }
            
            if (shouldGenerateIncident()) {
                generateIncident();
            }
            
            // Check if we should self-terminate
            if (incidentCounter >= MAX_INCIDENTS) {
                System.out.println("[SENSOR] " + getLocalName() + " reached max incidents (" + MAX_INCIDENTS + "), terminating...");
                myAgent.doDelete();
            }
        }
    }

    private boolean shouldGenerateIncident() {
        // Random probability of incident generation (30% chance)
        return random.nextDouble() < 0.3;
    }

    private void generateIncident() {
        try {
            // Random incident type
            IncidentType[] types = {IncidentType.FIRE, IncidentType.MEDICAL, IncidentType.STRUCTURAL_COLLAPSE};
            IncidentType type = types[random.nextInt(types.length)];
            
            // Random severity (1-10)
            int severity = random.nextInt(10) + 1;
            
            // Create incident
            String incidentId = "INC-" + Integer.toHexString(random.nextInt(0xFFFFFF)).toUpperCase();
            Incident incident = new Incident(incidentId, type, severity, x, y);
            
            // Send to Dispatcher
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(new AID("Dispatcher", AID.ISLOCALNAME));
            msg.setContent("INCIDENT:" + incident.serialize());
            msg.setOntology("EmergencyOntology");
            send(msg);
            
            // Send to Logger
            ACLMessage logMsg = new ACLMessage(ACLMessage.INFORM);
            logMsg.addReceiver(new AID("Logger", AID.ISLOCALNAME));
            logMsg.setContent("INCIDENT_REPORT:" + incidentId + ":" + type + ":" + severity + ":" + x + ":" + y + ":OPEN");
            logMsg.setOntology("EmergencyOntology");
            send(logMsg);
            
            // Send incident to GUI
            sendIncidentToGUI(incident);
            
            System.out.println("[SENSOR] " + getLocalName() + " reported: " + incident);
            incidentCounter++;
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void sendIncidentToGUI(Incident incident) {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(new AID("GUI", AID.ISLOCALNAME));
        msg.setContent("INCIDENT:" + incident.getId());
        send(msg);
    }
    
    private void sendUpdateToGUI() {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(new AID("GUI", AID.ISLOCALNAME));
        msg.setContent("AGENT_UPDATE:Sensor:" + getLocalName() + ":" + x + ":" + y + ":ACTIVE");
        send(msg);
    }
    
    /**
     * Behaviour pour écouter les signaux de coordination (shutdown)
     * Implémentation du mécanisme de coordination selon la théorie
     */
    private class CoordinationListenerBehaviour extends jade.core.behaviours.CyclicBehaviour {
        private final MessageTemplate mt = MessageTemplate.and(
            MessageTemplate.MatchPerformative(ACLMessage.INFORM),
            MessageTemplate.MatchContent("SIMULATION_COMPLETE")
        );

        @Override
        public void action() {
            ACLMessage msg = receive(mt);
            if (msg != null) {
                System.out.println("[SENSOR] " + getLocalName() + " received shutdown signal from " + msg.getSender().getLocalName());
                shutdownRequested = true;
                myAgent.removeBehaviour(this);
            } else {
                block(1000);
            }
        }
    }

    @Override
    protected void takeDown() {
        System.out.println("[SENSOR] " + getLocalName() + " terminating. Generated " + incidentCounter + " incidents.");
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }
}
