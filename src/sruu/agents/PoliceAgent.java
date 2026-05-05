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

import java.util.Random;

public class PoliceAgent extends Agent {

    // === Variables d'etat ===
    private int x, y;
    private int baseX, baseY;
    private String state = "PATROLLING";
    private String currentIncidentId = null;
    private int ticksSecuring = 0;
    private int patrolDirection = 1;

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

        System.out.println("[POLICE] " + getLocalName() + " started at (" + x + "," + y + ")");

        // Enregistrement DF
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            ServiceDescription sd1 = new ServiceDescription();
            sd1.setType("CROWD_CONTROL");
            ServiceDescription sd2 = new ServiceDescription();
            sd2.setType("PERIMETER");
            ServiceDescription sd3 = new ServiceDescription();
            sd3.setType("RESCUE");
            dfd.setName(getAID());
            dfd.addServices(sd1);
            dfd.addServices(sd2);
            dfd.addServices(sd3);
            DFService.register(this, dfd);
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
            if (!"PATROLLING".equals(state)) {
                ACLMessage refuse = cfp.createReply();
                refuse.setPerformative(ACLMessage.REFUSE);
                refuse.setContent("REFUSE:Busy");
                myAgent.send(refuse);
                return;
            }

            try {
                Incident incident = Incident.deserialize(content.substring(4));
                UnitProposal proposal = new UnitProposal(
                        getLocalName(),
                        "POLICE",
                        x, y,
                        "IDLE", // status
                        ""
                );

                ACLMessage propose = cfp.createReply();
                propose.setPerformative(ACLMessage.PROPOSE);
                propose.setContent("PROPOSE:" + proposal.serialize());
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
                case "PATROLLING": patrol();                         break;
                case "EN_ROUTE":   moveToward(targetX, targetY, "SECURING");   break;
                case "SECURING":   securePerimeter();               break;
                case "RETURNING":  moveToward(baseX, baseY, "PATROLLING");        break;
            }
        }
    }

    private int targetX, targetY;

    private void patrol() {
        // Patrouille en cercle autour de la base
        int patrolRadius = 15;
        double angle = (System.currentTimeMillis() / 2000.0) % (2 * Math.PI);
        int newX = baseX + (int)(Math.cos(angle) * patrolRadius);
        int newY = baseY + (int)(Math.sin(angle) * patrolRadius);
        
        // Limiter aux bornes
        x = Math.max(0, Math.min(99, newX));
        y = Math.max(0, Math.min(99, newY));
        
        System.out.println("[POLICE] " + getLocalName() + " patrolling at (" + x + "," + y + ")");
        sendPositionUpdate();
    }

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
        if ("SECURING".equals(newState)) {
            System.out.println("[POLICE] " + getLocalName() + " arrived on site.");
        } else if ("PATROLLING".equals(newState)) {
            System.out.println("[POLICE] " + getLocalName() + " returned to patrol.");
        }
    }

    private void securePerimeter() {
        ticksSecuring++;
        if (ticksSecuring >= 1) {
            System.out.println("[POLICE] " + getLocalName() + " perimeter secured for " + currentIncidentId);
            
            state = "RETURNING";
            notifyDispatcher("RESOLVED:" + currentIncidentId);
            notifyLogger("PERIMETER_SECURED:" + getLocalName() + ";" + currentIncidentId);
            currentIncidentId = null;
            ticksSecuring = 0;
        }
    }

    // === Comportement d'ecoute des demandes de position ===
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
                        getLocalName(), "POLICE", x, y, "IDLE", "").serialize());
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
                    System.out.println("[POLICE] " + getLocalName() + " assigned to " + incident.getId());
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
