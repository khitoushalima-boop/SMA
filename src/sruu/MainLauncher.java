package sruu;

import jade.core.Runtime;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

/**
 * SRUU Main Launcher.
 * Starts the JADE platform and all agents.
 * 
 * Usage: java sruu.MainLauncher
 * Requires jade.jar on classpath.
 */
public class MainLauncher {

    public static void main(String[] args) throws Exception {
        Runtime rt = Runtime.instance();
        rt.setCloseVM(true);

        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.MAIN, "true");
        profile.setParameter(Profile.GUI, "true"); // set to "false" to disable JADE GUI

        AgentContainer container = rt.createMainContainer(profile);

        System.out.println("=================================================");
        System.out.println("  SRUU — Système de Réponse aux Urgences Urbaines");
        System.out.println("  JADE Multi-Agent System Starting...");
        System.out.println("=================================================");

        // ── Infrastructure Agents ─────────────────────────
        startAgent(container, "GUI",              "sruu.agents.SimpleGUIAgent",                   null);
        startAgent(container, "Logger",           "sruu.agents.LoggerAgent",                      null);
        startAgent(container, "Dispatcher",       "sruu.agents.DispatcherAgent",                  null);
        startAgent(container, "TrafficController","sruu.agents.TrafficControllerAgent",            null);
        startAgent(container, "MedicalCoordinator","sruu.agents.MedicalCoordinatorAgent",          null);

        Thread.sleep(500); // let infrastructure agents register

        // ── Field Units ───────────────────────────────────
        // Ambulances (x, y starting positions)
        startAgent(container, "Ambulance1",       "sruu.agents.AmbulanceAgent",  new Object[]{"10","20"});
        startAgent(container, "Ambulance2",       "sruu.agents.AmbulanceAgent",  new Object[]{"70","80"});

        // Fire Trucks
        startAgent(container, "FireTruck1",       "sruu.agents.FireTruckAgent",  new Object[]{"30","10"});
        startAgent(container, "FireTruck2",       "sruu.agents.FireTruckAgent",  new Object[]{"60","60"});

        // Police Units
        startAgent(container, "Police1",          "sruu.agents.PoliceAgent",     new Object[]{"20","50"});
        startAgent(container, "Police2",          "sruu.agents.PoliceAgent",     new Object[]{"80","40"});

        // Biohazard Containment Units - REMOVED (class doesn't exist)
        // startAgent(container, "BCU1",             "sruu.agents.BiohazardContainmentUnitAgent", new Object[]{"50","90"});

        Thread.sleep(500); // let units register with DF

        // ── Sensor Agents ─────────────────────────────────
        // Sensors are placed at fixed locations representing incident-prone zones
        startAgent(container, "Sensor_ZoneA",     "sruu.agents.SensorAgent",     new Object[]{"15","25"});
        startAgent(container, "Sensor_ZoneB",     "sruu.agents.SensorAgent",     new Object[]{"75","60"});
        startAgent(container, "Sensor_ZoneC",     "sruu.agents.SensorAgent",     new Object[]{"45","45"});
        startAgent(container, "Sensor_ZoneD",     "sruu.agents.SensorAgent",     new Object[]{"90","20"});

        System.out.println("=================================================");
        System.out.println("  All agents started. Simulation running...");
        System.out.println("  Log file: sruu_log.txt");
        System.out.println("  Waiting for simulation completion signal...");
        System.out.println("=================================================");
        
        // Wait for LoggerAgent to send shutdown signal (coordination mechanism)
        waitForSimulationCompletion();
        
        // Graceful shutdown of JADE container
        System.out.println("[LAUNCHER] Shutting down JADE platform...");
        container.kill();
        System.out.println("[LAUNCHER] Simulation completed successfully.");
        System.exit(0);
    }
    
    private static void waitForSimulationCompletion() {
        // Wait for maximum 4 minutes for simulation to complete
        long maxWaitTime = 240000; // 4 minutes
        long startTime = System.currentTimeMillis();
        
        while (System.currentTimeMillis() - startTime < maxWaitTime) {
            try {
                Thread.sleep(1000);
                
                // Check if LoggerAgent has generated the final report
                // This is a coordination mechanism - we wait for the report file to be updated
                java.io.File reportFile = new java.io.File("sruu_final_report.txt");
                if (reportFile.exists() && reportFile.length() > 0) {
                    // Give a moment for agents to receive shutdown signal
                    Thread.sleep(2000);
                    break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private static void startAgent(AgentContainer container, String name, String className, Object[] args) {
        try {
            AgentController ac = container.createNewAgent(name, className, args);
            ac.start();
            System.out.println("[LAUNCHER] Started: " + name + " (" + className + ")");
        } catch (StaleProxyException e) {
            System.err.println("[LAUNCHER] Failed to start " + name + ": " + e.getMessage());
        }
    }
}
