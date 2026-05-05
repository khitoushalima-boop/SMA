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

public class AmbulanceAgent extends Agent {

    // === Variables d'état ===
    private int x, y;
    private int baseX, baseY;
    private String state = "IDLE";
    private String currentIncidentId = null;
    private int ticksOnSite = 0;

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

        System.out.println("[AMBULANCE] " + getLocalName() + " started at (" + x + "," + y + ")");

        // Enregistrement DF
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("MEDICAL");
            dfd.setName(getAID());
            dfd.addServices(sd);
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        // Comportements
        addBehaviour(new CFPListenerBehaviour());
        addBehaviour(new MovementBehaviour(this, 1000));
        addBehaviour(new PositionRequestListener());
    }

    // === Comportement d'écoute des CFP ===
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
            if (!"IDLE".equals(state)) {
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
                        "MEDICAL",
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
                case "EN_ROUTE":  moveToward(targetX, targetY, "ON_SITE");   break;
                case "ON_SITE":   treatPatient();                              break;
                case "RETURNING": moveToward(baseX, baseY, "IDLE");           break;
            }
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
        if ("ON_SITE".equals(newState)) {
            notifyDispatcher("ARRIVED:" + currentIncidentId);
            System.out.println("[AMBULANCE] " + getLocalName() + " arrived on site.");
        } else if ("IDLE".equals(newState)) {
            System.out.println("[AMBULANCE] " + getLocalName() + " returned to base.");
        }
    }

    private void treatPatient() {
        ticksOnSite++;
        if (ticksOnSite >= 1) {
            System.out.println("[AMBULANCE] " + getLocalName() + " treating patient at (" + x + "," + y + ")");
            state = "RETURNING";
            notifyDispatcher("RESOLVED:" + currentIncidentId);
            notifyLogger("TREATMENT_DONE:" + getLocalName() + ";" + currentIncidentId);
            currentIncidentId = null;
            ticksOnSite = 0;
        }
    }

    // === Comportement d'écoute des demandes de position ===
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
                        getLocalName(), "MEDICAL", x, y, "IDLE", "").serialize());
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
                    System.out.println("[AMBULANCE] " + getLocalName() + " assigned to " + incident.getId());
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
