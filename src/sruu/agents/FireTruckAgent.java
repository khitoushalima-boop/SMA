package sruu.agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import sruu.ontology.*;
import sruu.utils.UtilityCalculator;
import sruu.utils.OrganizationManager;

import java.util.Random;
import java.util.Iterator;

public class FireTruckAgent extends Agent {

    // === Variables d'etat ===
    private int x, y;
    private int baseX, baseY;
    private String state = "IDLE";
    private String currentIncidentId = null;
    private int ticksOnSite = 0;
    private int waterLevel = 100;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length >= 2) {
            x = baseX = Integer.parseInt((String) args[0]);
            y = baseY = Integer.parseInt((String) args[1]);
        } else {
            x = baseX = 50;
            y = baseY = 50;
        }

        System.out.println("[FIRETRUCK] " + getLocalName() + " started at (" + x + "," + y + ")");

        // Register with DF using AGR model (Agent-Groupe-Rôle)
        try {
            DFAgentDescription dfd = OrganizationManager.createAgentDescription(
                getAID(), 
                OrganizationManager.ROLE_FIRE_TRUCK, 
                OrganizationManager.GROUP_RESPONSE
            );
            
            // Add specific service type that matches Dispatcher search
            Iterator servicesIt = dfd.getAllServices();
            if (servicesIt.hasNext()) {
                ServiceDescription sd = (ServiceDescription) servicesIt.next();
                // Use service type "FireTruck" to match Dispatcher search
                sd.setType("FireTruck");
                sd.setName(getLocalName());
            }
            
            DFService.register(this, dfd);
            System.out.println("[FIRETRUCK] Registered with role: " + 
                OrganizationManager.ROLE_FIRE_TRUCK + " in group: " + OrganizationManager.GROUP_RESPONSE);
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        // Comportements
        addBehaviour(new CFPListenerBehaviour());
        addBehaviour(new MovementBehaviour(this, 1000));
        addBehaviour(new PositionRequestListener());
    }

    // === Comportement d'ecoute des CFP ===
    private class CFPListenerBehaviour extends CyclicBehaviour {
        private final MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.CFP),
                MessageTemplate.MatchOntology("EmergencyOntology"));

        @Override
        public void action() {
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String content = msg.getContent();
                if (content != null && content.startsWith("CFP:")) {
                    handleCFP(msg, content);
                }
            } else {
                block(1000);
            }
        }

        private void handleCFP(ACLMessage cfp, String content) {
            if (!"IDLE".equals(state) || waterLevel <= 20) {
                ACLMessage refuse = cfp.createReply();
                refuse.setPerformative(ACLMessage.REFUSE);
                refuse.setContent("REFUSE:Busy or Low Water");
                myAgent.send(refuse);
                return;
            }

            try {
                Incident incident = Incident.deserialize(content.substring(4));
                
                // Calculer l'utilité pour le FireTruck
                double utility = 8.0; // Utilité de base pour les incendies
                double distance = Math.abs(x - incident.getX()) + Math.abs(y - incident.getY());
                double estimatedTime = distance / 10.0; // Temps estimé
                double cost = estimatedTime * 15; // Coût basé sur le temps
                
                // Créer une proposition avec les valeurs numériques correctes
                String proposalData = getLocalName() + ";" + 
                                    incident.getId() + ";" + 
                                    utility + ";" + 
                                    estimatedTime + ";" + 
                                    cost;

                ACLMessage propose = cfp.createReply();
                propose.setPerformative(ACLMessage.PROPOSE);
                propose.setContent("PROPOSE:" + proposalData);
                myAgent.send(propose);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // === Comportement de mouvement ===
    private class MovementBehaviour extends TickerBehaviour {
        MovementBehaviour(Agent a, long period) { super(a, period); }

        @Override
        protected void onTick() {
            switch (state) {
                case "EN_ROUTE":  moveToward(targetX, targetY, "ACTIVE"); break;
                case "ACTIVE":    doWork();                      break;
                case "RETURNING": moveToward(baseX, baseY, "IDLE");     break;
            }
            
            // Send update to GUI after movement
            sendUpdateToGUI();
        }
    }

    private int targetX, targetY;

    private void moveToward(int tx, int ty, String nextState) {
        if (x < tx) x++; else if (x > tx) x--;
        if (y < ty) y++; else if (y > ty) y--;

        sendPositionUpdate();

        if (x == tx && y == ty) {
            state = nextState;
            onArrival(nextState);
            sendPositionUpdate();
        }
    }

    private void onArrival(String newState) {
        if ("ACTIVE".equals(newState)) {
            System.out.println("[FIRETRUCK] " + getLocalName() + " arrived on site.");
        } else if ("IDLE".equals(newState)) {
            System.out.println("[FIRETRUCK] " + getLocalName() + " returned to base.");
        }
    }

    private void doWork() {
        ticksOnSite++;
        waterLevel -= 15; 
        System.out.println("[FIRETRUCK] " + getLocalName() + " working. Water=" + waterLevel + " ticks=" + ticksOnSite);

        if (waterLevel <= 0) {
            System.out.println("[FIRETRUCK] " + getLocalName() + " WATER EXHAUSTED — sending ABORT!");
            notifyDispatcher("ABORT:" + currentIncidentId);
            notifyLogger("ABORT:" + currentIncidentId + ";" + getLocalName() + ";WATER_EXHAUSTED");
            state = "RETURNING";
            currentIncidentId = null;
            ticksOnSite = 0;
            waterLevel = 0;
        } else if (ticksOnSite >= 1) {
            notifyDispatcher("RESOLVED:" + currentIncidentId);
            notifyLogger("FIRE_EXTINGUISHED:" + getLocalName() + ";" + currentIncidentId);
            state = "RETURNING";
            currentIncidentId = null;
            ticksOnSite = 0;
        }
    }

    
    private class PositionRequestListener extends CyclicBehaviour {
        private final MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                MessageTemplate.MatchContent("GET_POSITION"));

        @Override
        public void action() {
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent("POSITION_RESPONSE:" + new UnitProposal(
                        getLocalName(), "FIRE", x, y, "IDLE", "").serialize());
                reply.setOntology("EmergencyOntology");
                myAgent.send(reply);
            } else {
                block(1000);
            }
        }
    }

    // === Gestion des ACCEPT/REJECT ===
    private void handleContractNetResponse(ACLMessage msg) {
        String content = msg.getContent();
        if (content != null) {
            if (content.startsWith("ACCEPT:")) {
                try {
                    Incident incident = Incident.deserialize(content.substring(7));
                    currentIncidentId = incident.getId();
                    targetX = incident.getX();
                    targetY = incident.getY();
                    state = "EN_ROUTE";
                    System.out.println("[FIRETRUCK] " + getLocalName() + " assigned to " + incident.getId());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // === Utilitaires ===
    private void sendPositionUpdate() {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(new AID("GUI", AID.ISLOCALNAME));
        msg.setContent("AGENT_UPDATE:" + getLocalName() + ":" + x + ":" + y + ":" + state);
        msg.setOntology("EmergencyOntology");
        send(msg);
    }
    
    private void sendUpdateToGUI() {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(new AID("GUI", AID.ISLOCALNAME));
        msg.setContent("AGENT_UPDATE:FireTruck:" + getLocalName() + ":" + x + ":" + y + ":" + state);
        send(msg);
    }

    private void notifyDispatcher(String message) {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(new AID("Dispatcher", AID.ISLOCALNAME));
        msg.setContent(message);
        msg.setOntology("EmergencyOntology");
        send(msg);
    }

    private void notifyLogger(String message) {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(new AID("Logger", AID.ISLOCALNAME));
        msg.setContent(message);
        msg.setOntology("EmergencyOntology");
        send(msg);
    }
}
