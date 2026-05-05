package sruu.agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class LoggerAgent extends Agent {

    private PrintWriter logWriter;
    private PrintWriter reportWriter;
    private final Map<String, Long> incidentStartTimes = new HashMap<>();
    private final Map<String, String> incidentStatuses = new HashMap<>();
    private final List<String> unresolvedIncidents = new ArrayList<>();
    private final List<String> abortedIncidents = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");

    @Override
    protected void setup() {
        System.out.println("[LOGGER] Starting Logger Agent...");
        
        try {
            logWriter = new PrintWriter(new FileWriter("sruu_log.txt", true));
            reportWriter = new PrintWriter(new FileWriter("sruu_final_report.txt"));
            
            logWriter.println("SYSTEM_START:" + new Date());
            System.out.println("[LOGGER] Log files initialized");
        } catch (IOException e) {
            e.printStackTrace();
        }

        addBehaviour(new LogListenerBehaviour());
        addBehaviour(new ReportGeneratorBehaviour(this, 180000));
    }

    private class LogListenerBehaviour extends CyclicBehaviour {
        private final MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);

        @Override
        public void action() {
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String content = msg.getContent();
                String sender = msg.getSender().getLocalName();
                processLogEntry(content, sender);
            } else {
                block(100);
            }
        }
    }

    private void processLogEntry(String content, String sender) {
        String timestamp = dateFormat.format(new Date());
        String logEntry = timestamp + " | " + content;
        
        logWriter.println(logEntry);
        logWriter.flush();
        
        // Process specific event types
        if (content.startsWith("INCIDENT_RECEIVED:")) {
            String incidentId = content.split(":")[1];
            incidentStartTimes.put(incidentId, System.currentTimeMillis());
            incidentStatuses.put(incidentId, "RECEIVED");
        } else if (content.startsWith("ASSIGNED:")) {
            String[] parts = content.split(":");
            if (parts.length >= 3) {
                String incidentId = parts[1];
                incidentStatuses.put(incidentId, "ASSIGNED");
            }
        } else if (content.startsWith("RESOLVED:")) {
            String incidentId = content.split(":")[1];
            incidentStatuses.put(incidentId, "RESOLVED");
            calculateResponseTime(incidentId);
        } else if (content.startsWith("UNRESOLVED:")) {
            String incidentId = content.split(":")[1];
            unresolvedIncidents.add(incidentId);
            incidentStatuses.put(incidentId, "UNRESOLVED");
        } else if (content.startsWith("ABORT:")) {
            String[] parts = content.split(":");
            if (parts.length >= 3) {
                String incidentId = parts[1];
                abortedIncidents.add(incidentId);
                incidentStatuses.put(incidentId, "ABORTED");
            }
        }
    }

    private void calculateResponseTime(String incidentId) {
        Long startTime = incidentStartTimes.get(incidentId);
        if (startTime != null) {
            long responseTime = System.currentTimeMillis() - startTime;
            logWriter.println("RESPONSE_TIME:" + incidentId + ":" + responseTime + "ms");
        }
    }

    private class ReportGeneratorBehaviour extends TickerBehaviour {
        private boolean reportGenerated = false;

        ReportGeneratorBehaviour(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            if (!reportGenerated) {
                generateFinalReport();
                reportGenerated = true;
            }
        }
    }

    private void generateFinalReport() {
        try {
            reportWriter.println("===============================================");
            reportWriter.println("SRUU SYSTEM - FINAL REPORT");
            reportWriter.println("Generated: " + new Date());
            reportWriter.println("===============================================");
            reportWriter.println();

            // System Statistics
            reportWriter.println("SYSTEM STATISTICS:");
            reportWriter.println("- Total incidents processed: " + incidentStatuses.size());
            reportWriter.println("- Resolved incidents: " + countIncidentsByStatus("RESOLVED"));
            reportWriter.println("- Unresolved incidents: " + unresolvedIncidents.size());
            reportWriter.println("- Aborted incidents: " + abortedIncidents.size());
            
            // Calculate average response time
            long totalResponseTime = 0;
            int responseCount = 0;
            for (Map.Entry<String, Long> entry : incidentStartTimes.entrySet()) {
                if (incidentStatuses.get(entry.getKey()).equals("RESOLVED")) {
                    // This would need actual response time tracking
                    responseCount++;
                }
            }
            
            if (responseCount > 0) {
                reportWriter.println("- Average response time: " + (totalResponseTime / responseCount) + "ms");
            }
            
            reportWriter.println();
            
            // Unresolved Incidents
            if (!unresolvedIncidents.isEmpty()) {
                reportWriter.println("UNRESOLVED INCIDENTS:");
                for (String incidentId : unresolvedIncidents) {
                    reportWriter.println("- " + incidentId);
                }
                reportWriter.println();
            }
            
            // Aborted Incidents
            if (!abortedIncidents.isEmpty()) {
                reportWriter.println("ABORTED INCIDENTS:");
                for (String incidentId : abortedIncidents) {
                    reportWriter.println("- " + incidentId);
                }
                reportWriter.println();
            }
            
            reportWriter.println("===============================================");
            reportWriter.println("END OF REPORT");
            reportWriter.println("===============================================");
            
            reportWriter.flush();
            System.out.println("[LOGGER] Final report generated");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int countIncidentsByStatus(String status) {
        int count = 0;
        for (String incidentStatus : incidentStatuses.values()) {
            if (status.equals(incidentStatus)) {
                count++;
            }
        }
        return count;
    }

    @Override
    protected void takeDown() {
        if (logWriter != null) {
            logWriter.println("SYSTEM_SHUTDOWN:" + new Date());
            logWriter.close();
        }
        if (reportWriter != null) {
            reportWriter.close();
        }
        System.out.println("[LOGGER] Logger Agent terminated");
    }
}
