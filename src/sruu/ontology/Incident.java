package sruu.ontology;

import java.io.Serializable;

public class Incident implements Serializable {
    private String id;
    private IncidentType type;
    private int severity; // 1-10
    private int x;
    private int y;
    private long timestamp;
    private String status; // OPEN, ASSIGNED, RESOLVED, ABORTED

    public Incident(String id, IncidentType type, int severity, int x, int y) {
        this.id = id;
        this.type = type;
        this.severity = severity;
        this.x = x;
        this.y = y;
        this.timestamp = System.currentTimeMillis();
        this.status = "OPEN";
    }

    // Getters and setters
    public String getId() { return id; }
    public IncidentType getType() { return type; }
    public int getSeverity() { return severity; }
    public int getX() { return x; }
    public int getY() { return y; }
    public long getTimestamp() { return timestamp; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return String.format("Incident[id=%s, type=%s, severity=%d, pos=(%d,%d), status=%s]",
                id, type, severity, x, y, status);
    }

    public String serialize() {
        return id + ";" + type + ";" + severity + ";" + x + ";" + y + ";" + status;
    }

    public static Incident deserialize(String data) {
        String[] parts = data.split(";");
        Incident inc = new Incident(parts[0], IncidentType.valueOf(parts[1]),
                Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4]));
        inc.setStatus(parts[5]);
        return inc;
    }
}
