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
import sruu.utils.OrganizationManager;

import java.util.*;

public class DispatcherAgent extends Agent {

    private final Map<String, Incident> activeIncidents = new HashMap<>();
    private final Map<String, String> incidentAssignments = new HashMap<>();
    
    @Override
    protected void setup() {
        System.out.println("[DISPATCHER] Started.");
        
        // Register with DF using AGR model (Agent-Groupe-Rôle)
        try {
            DFAgentDescription dfd = OrganizationManager.createAgentDescription(
                getAID(), 
                OrganizationManager.ROLE_DISPATCHER, 
                OrganizationManager.GROUP_COORDINATION
            );
            DFService.register(this, dfd);
            System.out.println("[DISPATCHER] Registered with role: " + 
                OrganizationManager.ROLE_DISPATCHER + " in group: " + OrganizationManager.GROUP_COORDINATION);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        
        addBehaviour(new IncidentListenerBehaviour());
        addBehaviour(new AbortListenerBehaviour());
        addBehaviour(new UnitStatusListenerBehaviour());
        addBehaviour(new CoordinationListenerBehaviour());
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
            
            // Send communication event to GUI
            sendCommunicationEvent("Dispatcher", "CFP", "All Units");
            
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
                    
                    // Send communication event to GUI
                    sendCommunicationEvent(msg.getSender().getLocalName(), "PROPOSE", "Dispatcher");
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
            
            // Send communication event to GUI
            sendCommunicationEvent("Dispatcher", "ACCEPT", winner.getUnitName());

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
    
    /**
     * Behaviour pour écouter les signaux de coordination (shutdown)
     * Implémentation du mécanisme de coordination selon la théorie
     */
    private class CoordinationListenerBehaviour extends CyclicBehaviour {
        private final MessageTemplate mt = MessageTemplate.and(
            MessageTemplate.MatchPerformative(ACLMessage.INFORM),
            MessageTemplate.MatchContent("SIMULATION_COMPLETE")
        );

        @Override
        public void action() {
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                System.out.println("[DISPATCHER] Received shutdown signal from " + msg.getSender().getLocalName());
                myAgent.doDelete();
            } else {
                block(1000);
            }
        }
    }

    private AID[] findEligibleUnits(IncidentType type) {
        try {
            List<AID> eligibleUnits = new ArrayList<>();
            
            // Determine required unit type based on incident type
            String requiredServiceType;
            switch (type) {
                case MEDICAL:
                    requiredServiceType = "Ambulance";
                    break;
                case FIRE:
                case STRUCTURAL_COLLAPSE:
                    requiredServiceType = "FireTruck";
                    break;
                default:
                    requiredServiceType = "Police";
                    break;
            }
            
            // Search for units with required service type
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType(requiredServiceType);
            template.addServices(sd);
            
            DFAgentDescription[] result = DFService.search(this, template);
            for (DFAgentDescription agentDesc : result) {
                AID agentAID = agentDesc.getName();
                if (!eligibleUnits.contains(agentAID)) {
                    eligibleUnits.add(agentAID);
                    System.out.println("[DISPATCHER] Found eligible unit: " + agentAID.getLocalName() + 
                        " for incident type: " + type);
                }
            }
            
            return eligibleUnits.toArray(new AID[0]);
        } catch (FIPAException fe) {
            fe.printStackTrace();
            return new AID[0];
        }
    }
    
    private void sendCommunicationEvent(String from, String type, String to) {
        try {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(new AID("GUI", AID.ISLOCALNAME));
            msg.setContent("COMMUNICATION:" + from + ":" + type + ":" + to);
            msg.setOntology("EmergencyOntology");
            send(msg);
        } catch (Exception e) {
            // Ignore GUI communication errors
        }
    }

    private String[] getCapabilitiesFor(IncidentType type) {
        switch (type) {
            case FIRE: return new String[]{
                OrganizationManager.SERVICE_FIRE, 
                OrganizationManager.SERVICE_RESCUE
            };
            case MEDICAL: return new String[]{OrganizationManager.SERVICE_MEDICAL};
            case STRUCTURAL_COLLAPSE: return new String[]{
                OrganizationManager.SERVICE_RESCUE, 
                OrganizationManager.SERVICE_CROWD_CONTROL, 
                OrganizationManager.SERVICE_PERIMETER
            };
            case BIOHAZARD: return new String[]{OrganizationManager.SERVICE_BIOHAZARD_CONTAINMENT};
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
