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
import java.util.Random;

public class SensorAgent extends Agent {

    private int x, y;
    private String zoneName;
    private Random random = new Random();
    private int incidentCounter = 0;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length >= 2) {
            x = Integer.parseInt((String) args[0]);
            y = Integer.parseInt((String) args[1]);
            zoneName = "Zone_" + (char)('A' + Integer.parseInt((String) args[2]) - 1);
        } else {
            x = 50; y = 50; zoneName = "Zone_A";
        }

        System.out.println("[SENSOR] " + getLocalName() + " started at (" + x + "," + y + ")");

        // Register with DF
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("SENSOR");
            dfd.setName(getAID());
            dfd.addServices(sd);
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        // Add behavior for incident generation
        addBehaviour(new IncidentGeneratorBehaviour(this, 8000));
    }

    private class IncidentGeneratorBehaviour extends TickerBehaviour {
        IncidentGeneratorBehaviour(Agent a, long period) { 
            super(a, period); 
        }

        @Override
        protected void onTick() {
            if (shouldGenerateIncident()) {
                generateIncident();
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
            
            // Send to GUI
            ACLMessage guiMsg = new ACLMessage(ACLMessage.INFORM);
            guiMsg.addReceiver(new AID("GUI", AID.ISLOCALNAME));
            guiMsg.setContent("INCIDENT_REPORT:" + incidentId + ":" + type + ":" + severity + ":" + x + ":" + y + ":OPEN");
            guiMsg.setOntology("EmergencyOntology");
            send(guiMsg);
            
            System.out.println("[SENSOR] " + getLocalName() + " reported: " + incident);
            incidentCounter++;
            
        } catch (Exception e) {
            e.printStackTrace();
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
