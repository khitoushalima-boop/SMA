package sruu.ontology;

import jade.content.Concept;
import jade.content.AgentAction;
import jade.content.onto.*;
import jade.content.schema.*;
import jade.core.AID;

/**
 * ============================================================
 * EMERGENCY ONTOLOGY — Système de Réponse aux Urgences Urbaines (SRUU)
 * ============================================================
 * 
 * Ontologie de Domaine complète pour formaliser les messages ACL
 * entre tous les agents du système multi-agents.
 * 
 * Utilise SLCodec + ContentManager pour sérialisation/désérialisation.
 */

public class EmergencyOntology extends Ontology {

    public static final String ONTOLOGY_NAME = "Emergency-Ontology";
    private static Ontology instance = new EmergencyOntology();

    public static Ontology getInstance() { return instance; }

    private EmergencyOntology() {
        super(ONTOLOGY_NAME, BasicOntology.getInstance());

        try {
            // ========================================================
            // CONCEPTS
            // ========================================================
            
            ConceptSchema incidentSchema = new ConceptSchema("Incident");
            incidentSchema.add("id", (PrimitiveSchema) getSchema(BasicOntology.STRING));
            incidentSchema.add("type", (PrimitiveSchema) getSchema(BasicOntology.STRING));
            incidentSchema.add("severity", (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            incidentSchema.add("x", (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            incidentSchema.add("y", (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            incidentSchema.add("status", (PrimitiveSchema) getSchema(BasicOntology.STRING));
            add(incidentSchema, Incident.class);

            ConceptSchema proposalSchema = new ConceptSchema("UnitProposal");
            proposalSchema.add("unitName", (PrimitiveSchema) getSchema(BasicOntology.STRING));
            proposalSchema.add("unitAID", (ConceptSchema) getSchema(BasicOntology.AID));
            proposalSchema.add("capability", (PrimitiveSchema) getSchema(BasicOntology.STRING));
            proposalSchema.add("x", (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            proposalSchema.add("y", (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            proposalSchema.add("resourceLevel", (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            proposalSchema.add("estimatedTime", (PrimitiveSchema) getSchema(BasicOntology.FLOAT));
            proposalSchema.add("incidentId", (PrimitiveSchema) getSchema(BasicOntology.STRING));
            proposalSchema.add("utilityScore", (PrimitiveSchema) getSchema(BasicOntology.FLOAT));
            add(proposalSchema, UnitProposal.class);

            ConceptSchema statusSchema = new ConceptSchema("UnitStatus");
            statusSchema.add("unitName", (PrimitiveSchema) getSchema(BasicOntology.STRING));
            statusSchema.add("unitAID", (ConceptSchema) getSchema(BasicOntology.AID));
            statusSchema.add("x", (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            statusSchema.add("y", (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            statusSchema.add("state", (PrimitiveSchema) getSchema(BasicOntology.STRING));
            statusSchema.add("currentIncidentId", (PrimitiveSchema) getSchema(BasicOntology.STRING));
            statusSchema.add("resourceLevel", (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            add(statusSchema, UnitStatus.class);

            ConceptSchema positionSchema = new ConceptSchema("Position");
            positionSchema.add("x", (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            positionSchema.add("y", (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            add(positionSchema, Position.class);

            ConceptSchema hospitalSchema = new ConceptSchema("Hospital");
            hospitalSchema.add("name", (PrimitiveSchema) getSchema(BasicOntology.STRING));
            hospitalSchema.add("x", (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            hospitalSchema.add("y", (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            hospitalSchema.add("totalBeds", (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            hospitalSchema.add("availableBeds", (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            add(hospitalSchema, Hospital.class);

            // ========================================================
            // AGENT ACTIONS
            // ========================================================

            AgentActionSchema reportSchema = new AgentActionSchema("ReportIncident");
            reportSchema.add("incident", (ConceptSchema) getSchema("Incident"));
            add(reportSchema, ReportIncident.class);

            AgentActionSchema cfpSchema = new AgentActionSchema("CallForProposal");
            cfpSchema.add("incident", (ConceptSchema) getSchema("Incident"));
            add(cfpSchema, CallForProposal.class);

            AgentActionSchema proposeSchema = new AgentActionSchema("ProposeService");
            proposeSchema.add("proposal", (ConceptSchema) getSchema("UnitProposal"));
            add(proposeSchema, ProposeService.class);

            AgentActionSchema assignSchema = new AgentActionSchema("AssignMission");
            assignSchema.add("incident", (ConceptSchema) getSchema("Incident"));
            assignSchema.add("assignedUnit", (ConceptSchema) getSchema(BasicOntology.AID));
            add(assignSchema, AssignMission.class);

            AgentActionSchema rejectSchema = new AgentActionSchema("RejectProposal");
            rejectSchema.add("incident", (ConceptSchema) getSchema("Incident"));
            rejectSchema.add("reason", (PrimitiveSchema) getSchema(BasicOntology.STRING));
            add(rejectSchema, RejectProposal.class);

            AgentActionSchema abortSchema = new AgentActionSchema("AbortMission");
            abortSchema.add("incidentId", (PrimitiveSchema) getSchema(BasicOntology.STRING));
            abortSchema.add("reason", (PrimitiveSchema) getSchema(BasicOntology.STRING));
            abortSchema.add("unitName", (PrimitiveSchema) getSchema(BasicOntology.STRING));
            add(abortSchema, AbortMission.class);

            AgentActionSchema completeSchema = new AgentActionSchema("MissionCompleted");
            completeSchema.add("incidentId", (PrimitiveSchema) getSchema(BasicOntology.STRING));
            completeSchema.add("unitName", (PrimitiveSchema) getSchema(BasicOntology.STRING));
            completeSchema.add("ticksTaken", (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            add(completeSchema, MissionCompleted.class);

            AgentActionSchema arrivedSchema = new AgentActionSchema("UnitArrived");
            arrivedSchema.add("incidentId", (PrimitiveSchema) getSchema(BasicOntology.STRING));
            arrivedSchema.add("unitName", (PrimitiveSchema) getSchema(BasicOntology.STRING));
            arrivedSchema.add("timestamp", (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            add(arrivedSchema, UnitArrived.class);

            AgentActionSchema reqPosSchema = new AgentActionSchema("RequestPosition");
            reqPosSchema.add("unitName", (PrimitiveSchema) getSchema(BasicOntology.STRING));
            add(reqPosSchema, RequestPosition.class);

            AgentActionSchema reportPosSchema = new AgentActionSchema("ReportPosition");
            reportPosSchema.add("status", (ConceptSchema) getSchema("UnitStatus"));
            add(reportPosSchema, ReportPosition.class);

            AgentActionSchema reqCorridorSchema = new AgentActionSchema("RequestCorridor");
            reqCorridorSchema.add("incidentId", (PrimitiveSchema) getSchema(BasicOntology.STRING));
            reqCorridorSchema.add("x", (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            reqCorridorSchema.add("y", (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            add(reqCorridorSchema, RequestCorridor.class);

            AgentActionSchema corridorStatusSchema = new AgentActionSchema("CorridorStatus");
            corridorStatusSchema.add("incidentId", (PrimitiveSchema) getSchema(BasicOntology.STRING));
            corridorStatusSchema.add("x", (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            corridorStatusSchema.add("y", (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            corridorStatusSchema.add("open", (PrimitiveSchema) getSchema(BasicOntology.BOOLEAN));
            corridorStatusSchema.add("expiryTime", (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            add(corridorStatusSchema, CorridorStatus.class);

            AgentActionSchema reqHospSchema = new AgentActionSchema("RequestHospital");
            reqHospSchema.add("ambulanceName", (PrimitiveSchema) getSchema(BasicOntology.STRING));
            reqHospSchema.add("ambulanceAID", (ConceptSchema) getSchema(BasicOntology.AID));
            reqHospSchema.add("ambulanceX", (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            reqHospSchema.add("ambulanceY", (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            reqHospSchema.add("incidentId", (PrimitiveSchema) getSchema(BasicOntology.STRING));
            add(reqHospSchema, RequestHospital.class);

            AgentActionSchema hospAssignSchema = new AgentActionSchema("HospitalAssignment");
            hospAssignSchema.add("hospitalName", (PrimitiveSchema) getSchema(BasicOntology.STRING));
            hospAssignSchema.add("hospitalX", (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            hospAssignSchema.add("hospitalY", (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            hospAssignSchema.add("availableBeds", (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            hospAssignSchema.add("incidentId", (PrimitiveSchema) getSchema(BasicOntology.STRING));
            add(hospAssignSchema, HospitalAssignment.class);

            AgentActionSchema hospFailSchema = new AgentActionSchema("HospitalAssignmentFailure");
            hospFailSchema.add("reason", (PrimitiveSchema) getSchema(BasicOntology.STRING));
            hospFailSchema.add("incidentId", (PrimitiveSchema) getSchema(BasicOntology.STRING));
            add(hospFailSchema, HospitalAssignmentFailure.class);

            AgentActionSchema patientDelSchema = new AgentActionSchema("PatientDelivered");
            patientDelSchema.add("hospitalName", (PrimitiveSchema) getSchema(BasicOntology.STRING));
            patientDelSchema.add("ambulanceName", (PrimitiveSchema) getSchema(BasicOntology.STRING));
            patientDelSchema.add("incidentId", (PrimitiveSchema) getSchema(BasicOntology.STRING));
            add(patientDelSchema, PatientDelivered.class);

            AgentActionSchema satAlertSchema = new AgentActionSchema("HospitalSaturationAlert");
            satAlertSchema.add("totalAvailableBeds", (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            satAlertSchema.add("threshold", (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            satAlertSchema.add("timestamp", (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            add(satAlertSchema, HospitalSaturationAlert.class);

            AgentActionSchema perimeterSchema = new AgentActionSchema("PerimeterCleared");
            perimeterSchema.add("incidentId", (PrimitiveSchema) getSchema(BasicOntology.STRING));
            perimeterSchema.add("unitName", (PrimitiveSchema) getSchema(BasicOntology.STRING));
            perimeterSchema.add("locationX", (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            perimeterSchema.add("locationY", (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            add(perimeterSchema, PerimeterCleared.class);

            AgentActionSchema logSchema = new AgentActionSchema("LogEvent");
            logSchema.add("eventType", (PrimitiveSchema) getSchema(BasicOntology.STRING));
            logSchema.add("agentName", (PrimitiveSchema) getSchema(BasicOntology.STRING));
            logSchema.add("incidentId", (PrimitiveSchema) getSchema(BasicOntology.STRING));
            logSchema.add("details", (PrimitiveSchema) getSchema(BasicOntology.STRING));
            logSchema.add("timestamp", (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            add(logSchema, LogEvent.class);

            AgentActionSchema guiSchema = new AgentActionSchema("GUIUpdate");
            guiSchema.add("updateType", (PrimitiveSchema) getSchema(BasicOntology.STRING));
            guiSchema.add("agentName", (PrimitiveSchema) getSchema(BasicOntology.STRING));
            guiSchema.add("x", (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            guiSchema.add("y", (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            guiSchema.add("state", (PrimitiveSchema) getSchema(BasicOntology.STRING));
            guiSchema.add("data", (PrimitiveSchema) getSchema(BasicOntology.STRING));
            add(guiSchema, GUIUpdate.class);

        } catch (OntologyException e) {
            e.printStackTrace();
        }
    }

    // ============================================================
    // CONCEPT CLASSES
    // ============================================================

    public static class Incident implements Concept {
        public static final String TYPE_FIRE = "FIRE";
        public static final String TYPE_MEDICAL = "MEDICAL";
        public static final String TYPE_STRUCTURAL_COLLAPSE = "STRUCTURAL_COLLAPSE";

        private String id;
        private String type;
        private int severity;
        private int x;
        private int y;
        private String status;

        public Incident() {}

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public int getSeverity() { return severity; }
        public void setSeverity(int severity) { this.severity = severity; }
        public int getX() { return x; }
        public void setX(int x) { this.x = x; }
        public int getY() { return y; }
        public void setY(int y) { this.y = y; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        @Override
        public String toString() {
            return String.format("Incident[%s, type=%s, severity=%d, pos=(%d,%d), status=%s]",
                    id, type, severity, x, y, status);
        }
    }

    public static class UnitProposal implements Concept {
        private String unitName;
        private AID unitAID;
        private String capability;
        private int x;
        private int y;
        private int resourceLevel;
        private float estimatedTime;
        private String incidentId;
        private float utilityScore;

        public UnitProposal() {}

        public String getUnitName() { return unitName; }
        public void setUnitName(String unitName) { this.unitName = unitName; }
        public AID getUnitAID() { return unitAID; }
        public void setUnitAID(AID unitAID) { this.unitAID = unitAID; }
        public String getCapability() { return capability; }
        public void setCapability(String capability) { this.capability = capability; }
        public int getX() { return x; }
        public void setX(int x) { this.x = x; }
        public int getY() { return y; }
        public void setY(int y) { this.y = y; }
        public int getResourceLevel() { return resourceLevel; }
        public void setResourceLevel(int resourceLevel) { this.resourceLevel = resourceLevel; }
        public float getEstimatedTime() { return estimatedTime; }
        public void setEstimatedTime(float estimatedTime) { this.estimatedTime = estimatedTime; }
        public String getIncidentId() { return incidentId; }
        public void setIncidentId(String incidentId) { this.incidentId = incidentId; }
        public float getUtilityScore() { return utilityScore; }
        public void setUtilityScore(float utilityScore) { this.utilityScore = utilityScore; }

        @Override
        public String toString() {
            return String.format("UnitProposal[%s, cap=%s, pos=(%d,%d), res=%d, eta=%.1f, score=%.3f]",
                    unitName, capability, x, y, resourceLevel, estimatedTime, utilityScore);
        }
    }

    public static class UnitStatus implements Concept {
        private String unitName;
        private AID unitAID;
        private int x;
        private int y;
        private String state;
        private String currentIncidentId;
        private int resourceLevel;

        public UnitStatus() {}

        public String getUnitName() { return unitName; }
        public void setUnitName(String unitName) { this.unitName = unitName; }
        public AID getUnitAID() { return unitAID; }
        public void setUnitAID(AID unitAID) { this.unitAID = unitAID; }
        public int getX() { return x; }
        public void setX(int x) { this.x = x; }
        public int getY() { return y; }
        public void setY(int y) { this.y = y; }
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        public String getCurrentIncidentId() { return currentIncidentId; }
        public void setCurrentIncidentId(String currentIncidentId) { this.currentIncidentId = currentIncidentId; }
        public int getResourceLevel() { return resourceLevel; }
        public void setResourceLevel(int resourceLevel) { this.resourceLevel = resourceLevel; }

        @Override
        public String toString() {
            return String.format("UnitStatus[%s, pos=(%d,%d), state=%s, incident=%s, res=%d]",
                    unitName, x, y, state, currentIncidentId, resourceLevel);
        }
    }

    public static class Position implements Concept {
        private int x;
        private int y;

        public Position() {}
        public Position(int x, int y) { this.x = x; this.y = y; }

        public int getX() { return x; }
        public void setX(int x) { this.x = x; }
        public int getY() { return y; }
        public void setY(int y) { this.y = y; }

        public double distanceTo(int otherX, int otherY) {
            return Math.sqrt(Math.pow(x - otherX, 2) + Math.pow(y - otherY, 2));
        }

        @Override
        public String toString() { return String.format("(%d,%d)", x, y); }
    }

    public static class Hospital implements Concept {
        private String name;
        private int x;
        private int y;
        private int totalBeds;
        private int availableBeds;

        public Hospital() {}
        public Hospital(String name, int x, int y, int totalBeds) {
            this.name = name; this.x = x; this.y = y;
            this.totalBeds = totalBeds; this.availableBeds = totalBeds;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getX() { return x; }
        public void setX(int x) { this.x = x; }
        public int getY() { return y; }
        public void setY(int y) { this.y = y; }
        public int getTotalBeds() { return totalBeds; }
        public void setTotalBeds(int totalBeds) { this.totalBeds = totalBeds; }
        public int getAvailableBeds() { return availableBeds; }
        public void setAvailableBeds(int availableBeds) { this.availableBeds = availableBeds; }

        public boolean hasAvailableBeds() { return availableBeds > 0; }
        public double distanceTo(int otherX, int otherY) {
            return Math.sqrt(Math.pow(x - otherX, 2) + Math.pow(y - otherY, 2));
        }

        @Override
        public String toString() {
            return String.format("Hospital[%s at (%d,%d), beds=%d/%d]",
                    name, x, y, availableBeds, totalBeds);
        }
    }

    // ============================================================
    // AGENT ACTION CLASSES
    // ============================================================

    public static class ReportIncident implements AgentAction {
        private Incident incident;
        public ReportIncident() {}
        public Incident getIncident() { return incident; }
        public void setIncident(Incident incident) { this.incident = incident; }
    }

    public static class CallForProposal implements AgentAction {
        private Incident incident;
        public CallForProposal() {}
        public Incident getIncident() { return incident; }
        public void setIncident(Incident incident) { this.incident = incident; }
    }

    public static class ProposeService implements AgentAction {
        private UnitProposal proposal;
        public ProposeService() {}
        public UnitProposal getProposal() { return proposal; }
        public void setProposal(UnitProposal proposal) { this.proposal = proposal; }
    }

    public static class AssignMission implements AgentAction {
        private Incident incident;
        private AID assignedUnit;
        public AssignMission() {}
        public Incident getIncident() { return incident; }
        public void setIncident(Incident incident) { this.incident = incident; }
        public AID getAssignedUnit() { return assignedUnit; }
        public void setAssignedUnit(AID assignedUnit) { this.assignedUnit = assignedUnit; }
    }

    public static class RejectProposal implements AgentAction {
        private Incident incident;
        private String reason;
        public RejectProposal() {}
        public Incident getIncident() { return incident; }
        public void setIncident(Incident incident) { this.incident = incident; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class AbortMission implements AgentAction {
        public static final String REASON_WATER_EXHAUSTED = "WATER_EXHAUSTED";
        public static final String REASON_EQUIPMENT_FAILURE = "EQUIPMENT_FAILURE";
        private String incidentId;
        private String reason;
        private String unitName;
        public AbortMission() {}
        public String getIncidentId() { return incidentId; }
        public void setIncidentId(String incidentId) { this.incidentId = incidentId; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public String getUnitName() { return unitName; }
        public void setUnitName(String unitName) { this.unitName = unitName; }
    }

    public static class MissionCompleted implements AgentAction {
        private String incidentId;
        private String unitName;
        private int ticksTaken;
        public MissionCompleted() {}
        public String getIncidentId() { return incidentId; }
        public void setIncidentId(String incidentId) { this.incidentId = incidentId; }
        public String getUnitName() { return unitName; }
        public void setUnitName(String unitName) { this.unitName = unitName; }
        public int getTicksTaken() { return ticksTaken; }
        public void setTicksTaken(int ticksTaken) { this.ticksTaken = ticksTaken; }
    }

    public static class UnitArrived implements AgentAction {
        private String incidentId;
        private String unitName;
        private long timestamp;
        public UnitArrived() {}
        public String getIncidentId() { return incidentId; }
        public void setIncidentId(String incidentId) { this.incidentId = incidentId; }
        public String getUnitName() { return unitName; }
        public void setUnitName(String unitName) { this.unitName = unitName; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }

    public static class RequestPosition implements AgentAction {
        private String unitName;
        public RequestPosition() {}
        public String getUnitName() { return unitName; }
        public void setUnitName(String unitName) { this.unitName = unitName; }
    }

    public static class ReportPosition implements AgentAction {
        private UnitStatus status;
        public ReportPosition() {}
        public UnitStatus getStatus() { return status; }
        public void setStatus(UnitStatus status) { this.status = status; }
    }

    public static class RequestCorridor implements AgentAction {
        private String incidentId;
        private int x;
        private int y;
        public RequestCorridor() {}
        public String getIncidentId() { return incidentId; }
        public void setIncidentId(String incidentId) { this.incidentId = incidentId; }
        public int getX() { return x; }
        public void setX(int x) { this.x = x; }
        public int getY() { return y; }
        public void setY(int y) { this.y = y; }
    }

    public static class CorridorStatus implements AgentAction {
        private String incidentId;
        private int x;
        private int y;
        private boolean open;
        private long expiryTime;
        public CorridorStatus() {}
        public String getIncidentId() { return incidentId; }
        public void setIncidentId(String incidentId) { this.incidentId = incidentId; }
        public int getX() { return x; }
        public void setX(int x) { this.x = x; }
        public int getY() { return y; }
        public void setY(int y) { this.y = y; }
        public boolean isOpen() { return open; }
        public void setOpen(boolean open) { this.open = open; }
        public long getExpiryTime() { return expiryTime; }
        public void setExpiryTime(long expiryTime) { this.expiryTime = expiryTime; }
    }

    public static class RequestHospital implements AgentAction {
        private String ambulanceName;
        private AID ambulanceAID;
        private int ambulanceX;
        private int ambulanceY;
        private String incidentId;
        public RequestHospital() {}
        public String getAmbulanceName() { return ambulanceName; }
        public void setAmbulanceName(String ambulanceName) { this.ambulanceName = ambulanceName; }
        public AID getAmbulanceAID() { return ambulanceAID; }
        public void setAmbulanceAID(AID ambulanceAID) { this.ambulanceAID = ambulanceAID; }
        public int getAmbulanceX() { return ambulanceX; }
        public void setAmbulanceX(int ambulanceX) { this.ambulanceX = ambulanceX; }
        public int getAmbulanceY() { return ambulanceY; }
        public void setAmbulanceY(int ambulanceY) { this.ambulanceY = ambulanceY; }
        public String getIncidentId() { return incidentId; }
        public void setIncidentId(String incidentId) { this.incidentId = incidentId; }
    }

    public static class HospitalAssignment implements AgentAction {
        private String hospitalName;
        private int hospitalX;
        private int hospitalY;
        private int availableBeds;
        private String incidentId;
        public HospitalAssignment() {}
        public String getHospitalName() { return hospitalName; }
        public void setHospitalName(String hospitalName) { this.hospitalName = hospitalName; }
        public int getHospitalX() { return hospitalX; }
        public void setHospitalX(int hospitalX) { this.hospitalX = hospitalX; }
        public int getHospitalY() { return hospitalY; }
        public void setHospitalY(int hospitalY) { this.hospitalY = hospitalY; }
        public int getAvailableBeds() { return availableBeds; }
        public void setAvailableBeds(int availableBeds) { this.availableBeds = availableBeds; }
        public String getIncidentId() { return incidentId; }
        public void setIncidentId(String incidentId) { this.incidentId = incidentId; }
    }

    public static class HospitalAssignmentFailure implements AgentAction {
        private String reason;
        private String incidentId;
        public HospitalAssignmentFailure() {}
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public String getIncidentId() { return incidentId; }
        public void setIncidentId(String incidentId) { this.incidentId = incidentId; }
    }

    public static class PatientDelivered implements AgentAction {
        private String hospitalName;
        private String ambulanceName;
        private String incidentId;
        public PatientDelivered() {}
        public String getHospitalName() { return hospitalName; }
        public void setHospitalName(String hospitalName) { this.hospitalName = hospitalName; }
        public String getAmbulanceName() { return ambulanceName; }
        public void setAmbulanceName(String ambulanceName) { this.ambulanceName = ambulanceName; }
        public String getIncidentId() { return incidentId; }
        public void setIncidentId(String incidentId) { this.incidentId = incidentId; }
    }

    public static class HospitalSaturationAlert implements AgentAction {
        private int totalAvailableBeds;
        private int threshold;
        private long timestamp;
        public HospitalSaturationAlert() {}
        public int getTotalAvailableBeds() { return totalAvailableBeds; }
        public void setTotalAvailableBeds(int totalAvailableBeds) { this.totalAvailableBeds = totalAvailableBeds; }
        public int getThreshold() { return threshold; }
        public void setThreshold(int threshold) { this.threshold = threshold; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }

    public static class PerimeterCleared implements AgentAction {
        private String incidentId;
        private String unitName;
        private int locationX;
        private int locationY;
        public PerimeterCleared() {}
        public String getIncidentId() { return incidentId; }
        public void setIncidentId(String incidentId) { this.incidentId = incidentId; }
        public String getUnitName() { return unitName; }
        public void setUnitName(String unitName) { this.unitName = unitName; }
        public int getLocationX() { return locationX; }
        public void setLocationX(int locationX) { this.locationX = locationX; }
        public int getLocationY() { return locationY; }
        public void setLocationY(int locationY) { this.locationY = locationY; }
    }

    public static class LogEvent implements AgentAction {
        private String eventType;
        private String agentName;
        private String incidentId;
        private String details;
        private long timestamp;
        public LogEvent() {}
        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }
        public String getAgentName() { return agentName; }
        public void setAgentName(String agentName) { this.agentName = agentName; }
        public String getIncidentId() { return incidentId; }
        public void setIncidentId(String incidentId) { this.incidentId = incidentId; }
        public String getDetails() { return details; }
        public void setDetails(String details) { this.details = details; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }

    public static class GUIUpdate implements AgentAction {
        private String updateType;
        private String agentName;
        private int x;
        private int y;
        private String state;
        private String data;
        public GUIUpdate() {}
        public String getUpdateType() { return updateType; }
        public void setUpdateType(String updateType) { this.updateType = updateType; }
        public String getAgentName() { return agentName; }
        public void setAgentName(String agentName) { this.agentName = agentName; }
        public int getX() { return x; }
        public void setX(int x) { this.x = x; }
        public int getY() { return y; }
        public void setY(int y) { this.y = y; }
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        public String getData() { return data; }
        public void setData(String data) { this.data = data; }
    }
}