package sruu.agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.*;
import sruu.utils.OrganizationManager;

public class MedicalCoordinatorAgent extends Agent {

    private final Map<String, Hospital> hospitals = new HashMap<>();
    
    @Override
    protected void setup() {
        System.out.println("[MEDICAL_COORDINATOR] Started.");
        
        // Initialize hospitals
        initializeHospitals();
        
        // Register with DF using AGR model (Agent-Groupe-Rôle)
        try {
            DFAgentDescription dfd = OrganizationManager.createAgentDescription(
                getAID(), 
                OrganizationManager.ROLE_MEDICAL_COORDINATOR, 
                OrganizationManager.GROUP_COORDINATION
            );
            DFService.register(this, dfd);
            System.out.println("[MEDICAL_COORDINATOR] Registered with role: " + 
                OrganizationManager.ROLE_MEDICAL_COORDINATOR + " in group: " + OrganizationManager.GROUP_COORDINATION);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        
        // Add behavior
        addBehaviour(new MedicalRequestListener());
    }
    
    private void initializeHospitals() {
        hospitals.put("Hospital_A", new Hospital("Hospital_A", 50, 50, 20));
        hospitals.put("Hospital_B", new Hospital("Hospital_B", 25, 75, 15));
        hospitals.put("Hospital_C", new Hospital("Hospital_C", 75, 25, 25));
        
        System.out.println("[MEDICAL_COORDINATOR] Hospitals initialized: " + hospitals.size());
    }
    
    private class MedicalRequestListener extends CyclicBehaviour {
        private final MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);

        @Override
        public void action() {
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String content = msg.getContent();
                if (content != null && content.startsWith("MEDICAL_DISPATCH:")) {
                    handleMedicalDispatch(content);
                } else if (content != null && content.startsWith("PATIENT_DELIVERED:")) {
                    handlePatientDelivery(content);
                }
            } else {
                block(1000);
            }
        }
    }
    
    private void handleMedicalDispatch(String content) {
        try {
            String[] parts = content.split(":");
            if (parts.length >= 5) {
                String incidentId = parts[1];
                int x = Integer.parseInt(parts[2]);
                int y = Integer.parseInt(parts[3]);
                String ambulanceName = parts[4];
                
                // Find nearest available hospital
                Hospital selected = findNearestAvailableHospital(x, y);
                
                if (selected != null) {
                    // Assign hospital to ambulance
                    ACLMessage response = new ACLMessage(ACLMessage.INFORM);
                    response.addReceiver(new AID(ambulanceName, AID.ISLOCALNAME));
                    response.setContent("HOSPITAL_ASSIGNED:" + selected.name + ":" + selected.x + ":" + selected.y);
                    response.setOntology("EmergencyOntology");
                    send(response);
                    
                    System.out.println("[MEDICAL_COORDINATOR] Assigned " + selected.name + " to " + ambulanceName);
                } else {
                    System.out.println("[MEDICAL_COORDINATOR] No available hospitals for incident " + incidentId);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void handlePatientDelivery(String content) {
        try {
            String[] parts = content.split(":");
            if (parts.length >= 2) {
                String hospitalName = parts[1];
                Hospital hospital = hospitals.get(hospitalName);
                
                if (hospital != null) {
                    hospital.availableBeds--;
                    System.out.println("[MEDICAL_COORDINATOR] Patient delivered to " + hospitalName + 
                                     ". Beds available: " + hospital.availableBeds);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private Hospital findNearestAvailableHospital(int x, int y) {
        Hospital nearest = null;
        double minDistance = Double.MAX_VALUE;
        
        for (Hospital hospital : hospitals.values()) {
            if (hospital.availableBeds > 0) {
                double distance = calculateDistance(x, y, hospital.x, hospital.y);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = hospital;
                }
            }
        }
        
        return nearest;
    }
    
    private double calculateDistance(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }
    
    private static class Hospital {
        String name;
        int x, y;
        int totalBeds;
        int availableBeds;
        
        Hospital(String name, int x, int y, int totalBeds) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.totalBeds = totalBeds;
            this.availableBeds = totalBeds;
        }
    }
}
