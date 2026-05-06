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
import sruu.utils.BDIAgent;

import java.util.Random;
import java.util.Iterator;

public class AmbulanceAgent extends BDIAgent {

    // === Variables d'état ===
    private int x, y;
    private int baseX, baseY;
    private String state = "IDLE";
    private String currentIncidentId = null;
    private int ticksOnSite = 0;
    
    // === Movement Behaviour (defined before use) ===
    private class MovementBehaviour extends TickerBehaviour {
        public MovementBehaviour(Agent a, long period) {
            super(a, period);
        }
        
        @Override
        protected void onTick() {
            // Send position update every second
            sendPositionUpdate();
            
            // If we have a target, move towards it
            if (currentIncidentId != null && "EN_ROUTE".equals(state)) {
                // Move towards incident location (simplified)
                if (targetX > 0 && targetY > 0) {
                    moveToward(targetX, targetY, "ON_SITE");
                }
            } else if ("RETURNING".equals(state)) {
                // Move back to base
                moveToward(baseX, baseY, "IDLE");
            }
            
            // Send update to GUI after movement
            sendUpdateToGUI();
        }
    }

    @Override
    protected void initializeBeliefs() {
        Object[] args = getArguments();
        if (args != null && args.length >= 2) {
            x = baseX = Integer.parseInt((String) args[0]);
            y = baseY = Integer.parseInt((String) args[1]);
        } else {
            x = baseX = 50;
            y = baseY = 50;
        }
        
        // Croyances initiales selon le modèle BDI
        addBelief("POSITION:" + x + "," + y);
        addBelief("STATUS:IDLE");
        addBelief("FUEL_FULL");
        addBelief("EQUIPMENT_READY");
        
        // Désirs initiaux
        addDesire("RESPOND_TO_MEDICAL");
        addDesire("RETURN_TO_BASE");
        addDesire("MAINTENANCE");
        
        // État initial: pas de mission en attente
        currentIntention = null;
        
        System.out.println("[BDI-AMBULANCE] " + getLocalName() + " started at (" + x + "," + y + ")");
    }
    
    @Override
    protected void registerWithDF() {
        // Register with DF using AGR model (Agent-Groupe-Rôle)
        try {
            DFAgentDescription dfd = OrganizationManager.createAgentDescription(
                getAID(), 
                OrganizationManager.ROLE_AMBULANCE, 
                OrganizationManager.GROUP_RESPONSE
            );
            
            // Add specific service type that matches Dispatcher search
            Iterator servicesIt = dfd.getAllServices();
            if (servicesIt.hasNext()) {
                ServiceDescription sd = (ServiceDescription) servicesIt.next();
                // Use service type "Ambulance" to match Dispatcher search
                sd.setType("Ambulance");
                sd.setName(getLocalName());
            }
            
            DFService.register(this, dfd);
            System.out.println("[BDI-AMBULANCE] " + getLocalName() + " registered with role: " + 
                OrganizationManager.ROLE_AMBULANCE + " in group: " + OrganizationManager.GROUP_RESPONSE);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        
        // Comportements spécifiques
        addBehaviour(new CFPListenerBehaviour());
        addBehaviour(new PositionRequestListener());
        addBehaviour(new ContractNetResponseListener());
        addBehaviour(new AmbulanceCoordinationListener());
        addBehaviour(new MovementBehaviour(this, 1000)); // Movement every 1 second
    }
    
    @Override
    protected void perceiveEnvironment() {
        // Mise à jour des croyances basée sur l'environnement
        removeBelief("STATUS:" + status);
        addBelief("STATUS:" + status);
        removeBelief("POSITION:" + x + "," + y);
        addBelief("POSITION:" + x + "," + y);
        
        if (currentIncident != null) {
            addBelief("HAS_MISSION");
        } else {
            removeBelief("HAS_MISSION");
        }
    }
    
    @Override
    protected boolean isIntentionValid(String intention) {
        switch (intention) {
            case "RESPOND_TO_MEDICAL":
                return beliefs.contains("STATUS:IDLE") && !beliefs.contains("LOW_FUEL") && beliefs.contains("HAS_MISSION_REQUEST");
            case "RETURN_TO_BASE":
                return beliefs.contains("STATUS:RETURNING") || beliefs.contains("LOW_FUEL");
            case "MAINTENANCE":
                return beliefs.contains("LOW_FUEL") || beliefs.contains("EQUIPMENT_DAMAGED");
            default:
                return false;
        }
    }
    
    @Override
    protected double calculateUtility(String intention) {
        // Formule d'utilité : U(g) = P(g|B) × Valeur(g) - Coût(g)
        double probability = 0.0;
        double value = 0.0;
        double cost = 0.0;
        
        switch (intention) {
            case "RESPOND_TO_MEDICAL":
                probability = beliefs.contains("STATUS:IDLE") ? 0.9 : 0.1;
                value = 10.0; // Haute valeur pour les missions médicales
                cost = beliefs.contains("LOW_FUEL") ? 8.0 : 2.0;
                break;
            case "RETURN_TO_BASE":
                probability = beliefs.contains("LOW_FUEL") ? 0.95 : 0.3;
                value = 7.0; // Valeur modérée pour retour à la base
                cost = 3.0;
                break;
            case "MAINTENANCE":
                probability = beliefs.contains("LOW_FUEL") ? 0.9 : 0.1;
                value = 8.0; // Haute valeur pour la maintenance
                cost = 5.0;
                break;
        }
        
        return probability * value - cost;
    }
    
    @Override
    protected void act() {
        if (currentIntention == null) return;
        
        switch (currentIntention) {
            case "RESPOND_TO_MEDICAL":
                executeMedicalResponse();
                break;
            case "RETURN_TO_BASE":
                executeReturnToBase();
                break;
            case "MAINTENANCE":
                executeMaintenance();
                break;
        }
    }
    
    private void executeMedicalResponse() {
        if ("IDLE".equals(status) && currentIncidentId != null) {
            updateStatus("EN_ROUTE");
            System.out.println("[BDI-AMBULANCE] " + getLocalName() + " executing medical response");
            sendUpdateToGUI(); // Send status change to GUI
        }
        
        // Actually move towards incident
        if ("EN_ROUTE".equals(status) && currentIncidentId != null) {
            moveToward(targetX, targetY, "ON_SITE");
        }
    }
    
    private void executeReturnToBase() {
        if ("RETURNING".equals(status)) {
            moveToward(baseX, baseY, "IDLE");
        }
    }
    
    private void executeMaintenance() {
        if (beliefs.contains("LOW_FUEL")) {
            addBelief("FUEL_FULL");
            removeBelief("LOW_FUEL");
            System.out.println("[BDI-AMBULANCE] " + getLocalName() + " completed maintenance");
        }
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
            try {
                Incident incident = Incident.deserialize(content.substring(4));
                
                // Ajouter la croyance de mission en attente
                addBelief("HAS_MISSION_REQUEST");
                
                // Utiliser le raisonnement BDI pour décider de répondre
                if (!"IDLE".equals(status) || !isIntentionValid("RESPOND_TO_MEDICAL")) {
                    removeBelief("HAS_MISSION_REQUEST");
                    ACLMessage refuse = cfp.createReply();
                    refuse.setPerformative(ACLMessage.REFUSE);
                    refuse.setContent("REFUSE:Busy_or_Invalid_Intention");
                    myAgent.send(refuse);
                    return;
                }

                // Calculer l'utilité selon la théorie de la négociation
                double utility = calculateUtility("RESPOND_TO_MEDICAL");
                
                // Créer une proposition avec les valeurs numériques correctes
                String proposalData = getLocalName() + ";" + 
                                    incident.getId() + ";" + 
                                    utility + ";" + 
                                    (Math.abs(x - incident.getX()) + Math.abs(y - incident.getY())) + ";" + 
                                    (utility * 10);

                ACLMessage propose = cfp.createReply();
                propose.setPerformative(ACLMessage.PROPOSE);
                propose.setContent("PROPOSE:" + proposalData);
                myAgent.send(propose);
                
                System.out.println("[BDI-AMBULANCE] " + getLocalName() + " proposed with utility: " + utility);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // === Gestion des ACCEPT/REJECT du Contract Net ===
    private void handleContractNetResponse(ACLMessage msg) {
        String content = msg.getContent();
        if (content != null) {
            if (content.startsWith("ACCEPT:")) {
                try {
                    Incident incident = Incident.deserialize(content.substring(7));
                    currentIncidentId = incident.getId();
                    targetX = incident.getX();
                    targetY = incident.getY();
                    updateStatus("EN_ROUTE");
                    System.out.println("[BDI-AMBULANCE] " + getLocalName() + " assigned to " + incident.getId());
                    
                    // Mettre à jour les croyances BDI
                    addBelief("HAS_MISSION");
                    currentIntention = "RESPOND_TO_MEDICAL";
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private int targetX, targetY;

    private void moveToward(int tx, int ty, String nextState) {
        if (x < tx) x++; else if (x > tx) x--;
        if (y < ty) y++; else if (y > ty) y--;

        sendPositionUpdate();

        if (x == tx && y == ty) {
            updateStatus(nextState);
            onArrival(nextState);
            sendPositionUpdate();
        }
    }

    private void onArrival(String newState) {
        if ("ON_SITE".equals(newState)) {
            notifyDispatcher("ARRIVED:" + currentIncidentId);
            System.out.println("[BDI-AMBULANCE] " + getLocalName() + " arrived on site.");
        } else if ("IDLE".equals(newState)) {
            System.out.println("[BDI-AMBULANCE] " + getLocalName() + " returned to base.");
            currentIntention = null; // Réinitialiser l'intention
        }
    }

    private void treatPatient() {
        ticksOnSite++;
        if (ticksOnSite >= 1) {
            System.out.println("[BDI-AMBULANCE] " + getLocalName() + " treating patient at (" + x + "," + y + ")");
            updateStatus("RETURNING");
            notifyDispatcher("RESOLVED:" + currentIncidentId);
            notifyLogger("TREATMENT_DONE:" + getLocalName() + ";" + currentIncidentId);
            
            // Mettre à jour les croyances BDI
            removeBelief("HAS_MISSION");
            currentIncidentId = null;
            ticksOnSite = 0;
            currentIntention = "RETURN_TO_BASE";
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
                        getLocalName(), "MEDICAL", x, y, status, "").serialize());
                reply.setOntology("EmergencyOntology");
                myAgent.send(reply);
            } else {
                block(1000);
            }
        }
    }

    // === Utilitaires ===
    private void sendPositionUpdate() {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(new AID("GUI", AID.ISLOCALNAME));
        msg.setContent("AGENT_UPDATE:" + getLocalName() + ":" + x + ":" + y + ":" + status);
        msg.setOntology("EmergencyOntology");
        send(msg);
    }
    
    private void sendUpdateToGUI() {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(new AID("GUI", AID.ISLOCALNAME));
        msg.setContent("AGENT_UPDATE:Ambulance:" + getLocalName() + ":" + x + ":" + y + ":" + status);
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
    
    // Écoute des réponses Contract Net (ACCEPT/REJECT)
    private class ContractNetResponseListener extends CyclicBehaviour {
        private final MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchOntology("EmergencyOntology"),
                MessageTemplate.or(
                    MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
                    MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL)
                )
        );

        @Override
        public void action() {
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                handleContractNetResponse(msg);
            } else {
                block(1000);
            }
        }
    }
    
    // Écoute des signaux de coordination pour Ambulance
    private class AmbulanceCoordinationListener extends CyclicBehaviour {
        private final MessageTemplate mt = MessageTemplate.and(
            MessageTemplate.MatchPerformative(ACLMessage.INFORM),
            MessageTemplate.MatchContent("SIMULATION_COMPLETE")
        );

        @Override
        public void action() {
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                System.out.println("[BDI-AMBULANCE] " + getLocalName() + " received shutdown signal");
                myAgent.doDelete();
            } else {
                block(1000);
            }
        }
    }
}
