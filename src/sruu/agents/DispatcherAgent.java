package sruu.agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import sruu.ontology.Incident;
import sruu.ontology.IncidentType;
import sruu.ontology.UnitProposal;
import sruu.utils.UtilityCalculator;

import java.util.*;

public class DispatcherAgent extends Agent {

    private final Map<String, Incident> activeIncidents = new HashMap<>();
    private final Map<String, String> incidentAssignments = new HashMap<>();
    
    @Override
    protected void setup() {
        System.out.println("[DISPATCHER] Started.");
        addBehaviour(new IncidentListenerBehaviour());
        addBehaviour(new AbortListenerBehaviour());
        addBehaviour(new UnitStatusListenerBehaviour());
    }

    private class IncidentListenerBehaviour extends CyclicBehaviour {
        private final MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);

        @Override
        public void action() {
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String content = msg.getContent();
                System.out.println("[DISPATCHER] DEBUG: Received INFORM message from " + msg.getSender().getLocalName() + ": " + content);
                System.out.println("[DISPATCHER] DEBUG: Performative=" + ACLMessage.getPerformative(msg.getPerformative()) + ", Ontology=" + msg.getOntology());
                if (content != null && content.startsWith("INCIDENT:")) {
                    System.out.println("[DISPATCHER] DEBUG: Processing incident...");
                    Incident inc = Incident.deserialize(content.substring(9));
                    activeIncidents.put(inc.getId(), inc);
                    System.out.println("[DISPATCHER] Received incident: " + inc);

                    notifyLogger("INCIDENT_RECEIVED:" + inc.serialize());
                    requestTrafficCorridor(inc);
                    myAgent.addBehaviour(new ContractNetInitiatorBehaviour(inc));
                } else {
                    System.out.println("[DISPATCHER] DEBUG: Message does not start with INCIDENT:");
                }
            } else {
                block();
            }
        }
    }

    private class ContractNetInitiatorBehaviour extends Behaviour {
        private final Incident incident;
        private final List<UnitProposal> proposals = new ArrayList<>();
        private final String conversationId;
        private int expectedResponders = 0;
        private int receivedResponses = 0;
        private int step = 0;
        private long cfpTime;

        ContractNetInitiatorBehaviour(Incident incident) {
            this.incident = incident;
            this.conversationId = "cfp-" + incident.getId();
        }

        @Override
        public void action() {
            switch (step) {
                case 0: sendCFP();    break;
                case 1: collectProposals(); break;
                case 2: selectAndAssign(); break;
            }
        }

        private void sendCFP() {
            AID[] units = findEligibleUnits(incident.getType());
            if (units.length == 0) {
                System.out.println("[DISPATCHER] No eligible units found for " + incident.getId());
                notifyLogger("NO_UNITS:" + incident.serialize());
                step = 3;
                return;
            }
            expectedResponders = units.length;
            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
            for (AID unit : units) cfp.addReceiver(unit);
            cfp.setConversationId(conversationId);
            cfp.setOntology("EmergencyOntology");
            cfp.setContent("CFP:" + incident.serialize());
            cfp.setReplyByDate(new Date(System.currentTimeMillis() + 5000));
            myAgent.send(cfp);
            cfpTime = System.currentTimeMillis();
            System.out.println("[DISPATCHER] CFP sent for " + incident.getId() + " to " + units.length + " units.");
            step = 1;
        }

        private void collectProposals() {
            if (System.currentTimeMillis() - cfpTime > 5500) {
                System.out.println("[DISPATCHER] CFP timeout, got " + proposals.size() + " proposals.");
                step = 2;
                return;
            }
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchConversationId(conversationId),
                    MessageTemplate.or(
                            MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
                            MessageTemplate.MatchPerformative(ACLMessage.REFUSE)));
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                receivedResponses++;
                if (msg.getPerformative() == ACLMessage.PROPOSE) {
                    UnitProposal proposal = UnitProposal.deserialize(msg.getContent().substring(8));
                    double score = UtilityCalculator.compute(proposal, incident);
                    proposal.setUtilityScore(score);
                    proposals.add(proposal);
                    System.out.println("[DISPATCHER] Received proposal: " + proposal);
                }
                if (receivedResponses >= expectedResponders) {
                    step = 2;
                }
            } else {
                block(500);
            }
        }

        private void selectAndAssign() {
            if (proposals.isEmpty()) {
                System.out.println("[DISPATCHER] No proposals for incident " + incident.getId());
                notifyLogger("UNRESOLVED:" + incident.serialize());
                step = 3;
                return;
            }
            proposals.sort((a, b) -> Double.compare(b.getUtilityScore(), a.getUtilityScore()));
            UnitProposal winner = proposals.get(0);

            ACLMessage accept = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
            accept.addReceiver(new AID(winner.getUnitName(), AID.ISLOCALNAME));
            accept.setConversationId(conversationId);
            accept.setOntology("EmergencyOntology");
            accept.setContent("ACCEPT:" + incident.serialize());
            myAgent.send(accept);

            for (int i = 1; i < proposals.size(); i++) {
                ACLMessage reject = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                reject.addReceiver(new AID(proposals.get(i).getUnitName(), AID.ISLOCALNAME));
                reject.setConversationId(conversationId);
                reject.setOntology("EmergencyOntology");
                reject.setContent("REJECT:" + incident.getId());
                myAgent.send(reject);
            }

            incidentAssignments.put(incident.getId(), winner.getUnitName());
            incident.setStatus("ASSIGNED");
            System.out.println("[DISPATCHER] Assigned " + incident.getId() + " to " + winner.getUnitName()
                    + " (score=" + String.format("%.3f", winner.getUtilityScore()) + ")");
            notifyLogger("ASSIGNED:" + incident.getId() + ";" + winner.getUnitName()
                    + ";" + winner.getUtilityScore());

            if (incident.getType() == IncidentType.MEDICAL) {
                notifyMedicalCoordinator(incident, winner.getUnitName());
            }

            step = 3;
        }

        @Override
        public boolean done() { return step == 3; }
    }

    private class AbortListenerBehaviour extends CyclicBehaviour {
        private final MessageTemplate mt = MessageTemplate.or(
                MessageTemplate.MatchPerformative(ACLMessage.REFUSE),
                MessageTemplate.and(
                        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                        MessageTemplate.MatchContent("RESOLVED:*")));

        @Override
        public void action() {
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String content = msg.getContent();
                if (content.startsWith("ABORT:")) {
                    String incidentId = content.substring(6);
                    System.out.println("[DISPATCHER] ABORT received for " + incidentId);
                    incidentAssignments.remove(incidentId);
                    Incident inc = activeIncidents.get(incidentId);
                    if (inc != null) {
                        inc.setStatus("OPEN");
                        myAgent.addBehaviour(new ContractNetInitiatorBehaviour(inc));
                    }
                } else if (content.startsWith("RESOLVED:")) {
                    String incidentId = content.substring(9);
                    System.out.println("[DISPATCHER] RESOLVED received for " + incidentId);
                    Incident inc = activeIncidents.get(incidentId);
                    if (inc != null) {
                        inc.setStatus("RESOLVED");
                        notifyLogger("RESOLVED:" + incidentId);
                    }
                }
            } else {
                block();
            }
        }
    }

    private class UnitStatusListenerBehaviour extends CyclicBehaviour {
        private final MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.MatchContent("STATUS:*"));

        @Override
        public void action() {
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String content = msg.getContent();
                System.out.println("[DISPATCHER] Status update from " + msg.getSender().getLocalName() + ": " + content);
            } else {
                block();
            }
        }
    }

    private AID[] findEligibleUnits(IncidentType type) {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            String[] capabilities = getCapabilitiesFor(type);
            
            for (String cap : capabilities) {
                sd.setType(cap);
                dfd.addServices(sd);
            }
            
            DFAgentDescription[] result = DFService.search(this, dfd);
            AID[] agents = new AID[result.length];
            for (int i = 0; i < result.length; i++) {
                agents[i] = result[i].getName();
            }
            return agents;
        } catch (FIPAException fe) {
            fe.printStackTrace();
            return new AID[0];
        }
    }

    private String[] getCapabilitiesFor(IncidentType type) {
        switch (type) {
            case FIRE: return new String[]{"FIRE", "RESCUE"};
            case MEDICAL: return new String[]{"MEDICAL"};
            case STRUCTURAL_COLLAPSE: return new String[]{"RESCUE", "CROWD_CONTROL", "PERIMETER"};
            case BIOHAZARD: return new String[]{"BIOHAZARD"};
            case CRYOGENIC_LEAK: return new String[]{"CRYOGENIC"};
            default: return new String[0];
        }
    }

    private void requestTrafficCorridor(Incident inc) {
        ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
        request.addReceiver(new AID("TrafficController", AID.ISLOCALNAME));
        request.setContent("CORRIDOR:" + inc.getId() + ":" + inc.getX() + ":" + inc.getY());
        request.setOntology("EmergencyOntology");
        send(request);
    }

    private void notifyMedicalCoordinator(Incident inc, String ambulanceName) {
        ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
        request.addReceiver(new AID("MedicalCoordinator", AID.ISLOCALNAME));
        request.setContent("MEDICAL_DISPATCH:" + inc.getId() + ":" + inc.getX() + ":" + inc.getY() + ":" + ambulanceName);
        request.setOntology("EmergencyOntology");
        send(request);
    }

    private void notifyLogger(String message) {
        ACLMessage log = new ACLMessage(ACLMessage.INFORM);
        log.addReceiver(new AID("Logger", AID.ISLOCALNAME));
        log.setContent(message);
        log.setOntology("EmergencyOntology");
        send(log);
    }
}
