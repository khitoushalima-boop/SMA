package sruu.utils;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.*;

/**
 * Framework BDI (Beliefs-Desires-Intentions) pour agents intelligents
 * Implémentation du modèle psychologique de référence en IA distribuée
 */
public abstract class BDIAgent extends Agent {
    
    // État mental de l'agent selon le modèle BDI
    protected Set<String> beliefs = new HashSet<>();      // Croyances
    protected List<String> desires = new ArrayList<>();   // Désirs/Buts
    protected String currentIntention = null;              // Intention courante
    
    // État opérationnel
    protected String status = "IDLE";
    protected int x, y; // Position
    protected String currentIncident = null;
    
    @Override
    protected void setup() {
        initializeBeliefs();
        addBehaviour(new BDIReasoningCycle());
        registerWithDF();
    }
    
    /**
     * Initialise les croyances de base de l'agent
     */
    protected abstract void initializeBeliefs();
    
    /**
     * Enregistre l'agent avec le DF selon le modèle AGR
     */
    protected abstract void registerWithDF();
    
    /**
     * Cycle de raisonnement BDI : Percevoir → Délibérer → Agir
     */
    private class BDIReasoningCycle extends CyclicBehaviour {
        @Override
        public void action() {
            // 1. Percevoir l'environnement (mise à jour des croyances)
            perceiveEnvironment();
            
            // 2. Délibérer (choisir une intention)
            deliberate();
            
            // 3. Agir (exécuter l'intention)
            act();
            
            block(1000); // Cycle de raisonnement chaque seconde
        }
    }
    
    /**
     * Percevoir l'environnement et mettre à jour les croyances
     * Bt+1 = brf(Bt, perception)
     */
    protected void perceiveEnvironment() {
        // À implémenter par les agents concrets
        // Ex: détecter batterie faible, position, incidents, etc.
    }
    
    /**
     * Délibération : choix d'une intention parmi les désirs
     * I = filter(B, D) - sélection de l'intention optimale
     */
    protected void deliberate() {
        // Filtrer les désirs selon les croyances actuelles
        List<String> validOptions = new ArrayList<>();
        
        for (String desire : desires) {
            if (isIntentionValid(desire)) {
                validOptions.add(desire);
            }
        }
        
        // Choisir la meilleure intention selon utilité
        if (!validOptions.isEmpty()) {
            currentIntention = selectBestIntention(validOptions);
            System.out.println("[BDI] " + getLocalName() + " selected intention: " + currentIntention);
        }
    }
    
    /**
     * Vérifie si une intention est valide compte tenu des croyances
     */
    protected abstract boolean isIntentionValid(String intention);
    
    /**
     * Sélectionne la meilleure intention basée sur l'utilité
     * U(g) = P(g|B) × Valeur(g) - Coût(g)
     */
    protected String selectBestIntention(List<String> options) {
        String bestIntention = null;
        double bestUtility = Double.NEGATIVE_INFINITY;
        
        for (String option : options) {
            double utility = calculateUtility(option);
            if (utility > bestUtility) {
                bestUtility = utility;
                bestIntention = option;
            }
        }
        
        return bestIntention;
    }
    
    /**
     * Calcule l'utilité d'une intention
     * Formule : U(g) = P(g|B) × Valeur(g) - Coût(g)
     */
    protected abstract double calculateUtility(String intention);
    
    /**
     * Exécute l'intention choisie
     */
    protected abstract void act();
    
    /**
     * Ajoute une croyance
     */
    protected void addBelief(String belief) {
        beliefs.add(belief);
    }
    
    /**
     * Retire une croyance
     */
    protected void removeBelief(String belief) {
        beliefs.remove(belief);
    }
    
    /**
     * Ajoute un désir
     */
    protected void addDesire(String desire) {
        if (!desires.contains(desire)) {
            desires.add(desire);
        }
    }
    
    /**
     * Met à jour l'état de l'agent
     */
    protected void updateStatus(String newStatus) {
        status = newStatus;
        System.out.println("[BDI] " + getLocalName() + " status: " + status);
    }
    
    /**
     * Gère les messages de coordination (shutdown)
     */
    protected class CoordinationListener extends CyclicBehaviour {
        private final MessageTemplate mt = MessageTemplate.and(
            MessageTemplate.MatchPerformative(ACLMessage.INFORM),
            MessageTemplate.MatchContent("SIMULATION_COMPLETE")
        );

        @Override
        public void action() {
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                System.out.println("[BDI] " + getLocalName() + " received shutdown signal");
                myAgent.doDelete();
            } else {
                block(1000);
            }
        }
    }
}
