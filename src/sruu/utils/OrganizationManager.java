package sruu.utils;

import jade.domain.FIPAAgentManagement.*;
import java.util.Iterator;

/**
 * Gestionnaire d'organisation selon le modèle AGR (Agent • Groupe • Rôle)
 * Implémente les concepts théoriques de l'organisation multi-agents
 */
public class OrganizationManager {
    
    // Groupes d'organisation
    public static final String GROUP_INFRASTRUCTURE = "Infrastructure";
    public static final String GROUP_COORDINATION = "Coordination";
    public static final String GROUP_RESPONSE = "Response";
    public static final String GROUP_SENSORS = "Sensors";
    
    // Rôles dans les groupes
    public static final String ROLE_LOGGER = "Logger";
    public static final String ROLE_DISPATCHER = "Dispatcher";
    public static final String ROLE_TRAFFIC_CONTROLLER = "TrafficController";
    public static final String ROLE_MEDICAL_COORDINATOR = "MedicalCoordinator";
    public static final String ROLE_GUI = "GUI";
    
    // Rôles des unités de réponse
    public static final String ROLE_AMBULANCE = "Ambulance";
    public static final String ROLE_FIRE_TRUCK = "FireTruck";
    public static final String ROLE_POLICE = "Police";
    public static final String ROLE_BCU = "BiohazardContainmentUnit";
    
    // Rôles des capteurs
    public static final String ROLE_SENSOR = "Sensor";
    
    // Capacités (Services) selon la théorie
    public static final String SERVICE_MEDICAL = "MEDICAL";
    public static final String SERVICE_FIRE = "FIRE";
    public static final String SERVICE_RESCUE = "RESCUE";
    public static final String SERVICE_CROWD_CONTROL = "CROWD_CONTROL";
    public static final String SERVICE_PERIMETER = "PERIMETER";
    public static final String SERVICE_BIOHAZARD_CONTAINMENT = "BIOHAZARD_CONTAINMENT";
    
    /**
     * Crée une description de service pour l'enregistrement DF
     * Implémentation du modèle AGR avec Agent-Groupe-Rôle
     */
    public static ServiceDescription createServiceDescription(String serviceType, String agentName) {
        ServiceDescription sd = new ServiceDescription();
        sd.setType(serviceType); // Le type de service (ex: "Ambulance", "FireTruck")
        sd.setName(agentName); // Le nom de l'agent
        return sd;
    }
    
    /**
     * Crée une description d'agent complète avec rôle et groupe
     */
    public static DFAgentDescription createAgentDescription(jade.core.AID aid, String role, String group) {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(aid);
        dfd.addServices(createServiceDescription(role, group));
        return dfd;
    }
    
    /**
     * Ajoute des capacités spécifiques à une unité de réponse
     * Selon la théorie des capacités distribuées
     */
    public static void addCapabilities(ServiceDescription sd, String... capabilities) {
        for (String capability : capabilities) {
            sd.addProperties(new Property(capability, "true"));
        }
    }
    
    /**
     * Vérifie si un agent a une capacité spécifique
     */
    public static boolean hasCapability(DFAgentDescription agentDesc, String capability) {
        Iterator servicesIt = agentDesc.getAllServices();
        while (servicesIt.hasNext()) {
            ServiceDescription sd = (ServiceDescription) servicesIt.next();
            Iterator propsIt = sd.getAllProperties();
            while (propsIt.hasNext()) {
                Property prop = (Property) propsIt.next();
                if (prop.getName().equals(capability)) {
                    return true;
                }
            }
        }
        return false;
    }
}
