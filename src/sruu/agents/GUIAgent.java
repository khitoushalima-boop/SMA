package sruu.agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class GUIAgent extends Agent {
    
    @Override
    protected void setup() {
        System.out.println("[GUI] GUI Agent starting...");
        
        // Register with DF
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("GUI");
            dfd.setName(getAID());
            dfd.addServices(sd);
            DFService.register(this, dfd);
            System.out.println("[GUI] Registered with DF service");
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        
        // Add behavior to receive messages
        addBehaviour(new MessageListener());
    }
    
    private class MessageListener extends CyclicBehaviour {
        private final MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);

        @Override
        public void action() {
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String content = msg.getContent();
                String sender = msg.getSender().getLocalName();
                System.out.println("[GUI] Received from " + sender + ": " + content);
            } else {
                block(100);
            }
        }
    }
    
    @Override
    protected void takeDown() {
        System.out.println("[GUI] GUI Agent terminating...");
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }
}
