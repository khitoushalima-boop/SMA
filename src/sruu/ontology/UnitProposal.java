package sruu.ontology;

import java.io.Serializable;

public class UnitProposal implements Serializable {
    private String unitName;
    private String unitType;  // AMBULANCE, FIRETRUCK, POLICE, BCU
    private int x;
    private int y;
    private String status;
    private double utilityScore;
    private String incidentId;

    public UnitProposal(String unitName, String unitType, int x, int y, String status, String incidentId) {
        this.unitName = unitName;
        this.unitType = unitType;
        this.x = x;
        this.y = y;
        this.status = status;
        this.incidentId = incidentId;
    }

    public String getUnitName() { return unitName; }
    public String getUnitType() { return unitType; }
    public int getX() { return x; }
    public int getY() { return y; }
    public String getStatus() { return status; }
    public double getUtilityScore() { return utilityScore; }
    public void setUtilityScore(double score) { this.utilityScore = score; }
    public String getIncidentId() { return incidentId; }

    public String serialize() {
        return unitName + ";" + unitType + ";" + x + ";" + y + ";" + status + ";" + incidentId;
    }

    public static UnitProposal deserialize(String data) {
        String[] parts = data.split(";");
        return new UnitProposal(parts[0], parts[1], Integer.parseInt(parts[2]),
                Integer.parseInt(parts[3]), parts[4], parts[5]);
    }

    @Override
    public String toString() {
        return String.format("Proposal[unit=%s, type=%s, pos=(%d,%d), status=%s, score=%.2f]",
                unitName, unitType, x, y, status, utilityScore);
    }
}
