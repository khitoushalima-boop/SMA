package sruu.ontology;

import java.io.Serializable;
import java.util.Arrays;

public class UnitProposal implements Serializable {
    private String unitName;
    private String unitType;  // AMBULANCE, FIRETRUCK, POLICE, BCU
    private int x;
    private int y;
    private String status;
    private double utilityScore;
    private String incidentId;
    private String additionalInfo;

    public UnitProposal(String unitName, String unitType, int x, int y, String status, String incidentId) {
        this.unitName = unitName;
        this.unitType = unitType;
        this.x = x;
        this.y = y;
        this.status = status;
        this.incidentId = incidentId;
    }
    
    public UnitProposal(String unitName, String unitType, int x, int y, String status, String incidentId, double utility, double estimatedTime, double cost, String additionalInfo) {
        this.unitName = unitName;
        this.unitType = unitType;
        this.x = x;
        this.y = y;
        this.status = status;
        this.incidentId = incidentId;
        this.utilityScore = utility;
        this.additionalInfo = additionalInfo;
    }

    public String getUnitName() { return unitName; }
    public String getUnitType() { return unitType; }
    public int getX() { return x; }
    public int getY() { return y; }
    public String getStatus() { return status; }
    public double getUtilityScore() { return utilityScore; }
    public void setUtilityScore(double score) { this.utilityScore = score; }
    public String getIncidentId() { return incidentId; }
    public String getAdditionalInfo() { return additionalInfo; }
    public void setAdditionalInfo(String info) { this.additionalInfo = info; }

    public String serialize() {
        return unitName + ";" + unitType + ";" + x + ";" + y + ";" + status + ";" + incidentId;
    }

    public static UnitProposal deserialize(String data) {
        String[] parts = data.split(";");
        
        // Safety check for array bounds
        if (parts.length < 5) {
            System.err.println("[ERROR] Invalid proposal data (expected 5 parts, got " + parts.length + "): " + data);
            return null;
        }
        
        try {
            String unitId = parts[0];
            String unitType = parts[1];
            double utility = Double.parseDouble(parts[2]);
            double estimatedTime = Double.parseDouble(parts[3]);
            double cost = Double.parseDouble(parts[4]);
            
            // parts[4] is the last valid index (0-4)
            String additionalInfo = parts.length > 5 ? parts[5] : "";
            
            return new UnitProposal(unitId, unitType, 0, 0, "IDLE", "", utility, estimatedTime, cost, additionalInfo);
        } catch (NumberFormatException e) {
            System.err.println("[ERROR] Failed to parse numeric values in proposal: " + data);
            System.err.println("[ERROR] Parts: " + Arrays.toString(parts));
            return null;
        }
    }

    @Override
    public String toString() {
        return String.format("Proposal[unit=%s, type=%s, pos=(%d,%d), status=%s, score=%.2f]",
                unitName, unitType, x, y, status, utilityScore);
    }
}
