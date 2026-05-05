package sruu.utils;

import sruu.ontology.Incident;
import sruu.ontology.IncidentType;
import sruu.ontology.UnitProposal;

/**
 * Utility Function for the Dispatcher Agent.
 *
 * U(u, i) = w1 * TypeMatch(u,i) + w2 * (1 - NormDist(u,i)) + w3 * (1 - WorkloadFactor(u)) + w4 * SeverityBonus(i)
 *
 * Weights:
 *   w1 = 0.40 (type compatibility is most critical)
 *   w2 = 0.30 (proximity)
 *   w3 = 0.20 (availability / low workload)
 *   w4 = 0.10 (severity urgency bonus)
 *
 * All components in [0,1], so U in [0,1].
 */
public class UtilityCalculator {

    private static final double W1_TYPE   = 0.40;
    private static final double W2_DIST   = 0.30;
    private static final double W3_LOAD   = 0.20;
    private static final double W4_SEV    = 0.10;
    private static final double GRID_DIAG = Math.sqrt(100 * 100 + 100 * 100); // max possible dist

    public static double compute(UnitProposal proposal, Incident incident) {
        double typeMatch   = typeMatchScore(proposal.getUnitType(), incident.getType());
        double distScore   = distanceScore(proposal.getX(), proposal.getY(), incident.getX(), incident.getY());
        double loadScore   = workloadScore(proposal.getStatus());
        double sevBonus    = severityBonus(incident.getSeverity());

        return W1_TYPE * typeMatch
             + W2_DIST * distScore
             + W3_LOAD * loadScore
             + W4_SEV  * sevBonus;
    }

    /** 1.0 = perfect match, 0.5 = secondary capability, 0.0 = incompatible */
    private static double typeMatchScore(String unitType, IncidentType incidentType) {
        switch (incidentType) {
            case FIRE:
                if ("FIRETRUCK".equals(unitType)) return 1.0;
                if ("POLICE".equals(unitType))    return 0.5;
                return 0.0;
            case MEDICAL:
                if ("AMBULANCE".equals(unitType)) return 1.0;
                if ("FIRETRUCK".equals(unitType)) return 0.4;
                return 0.0;
            case STRUCTURAL_COLLAPSE:
                if ("FIRETRUCK".equals(unitType)) return 1.0;
                if ("POLICE".equals(unitType))    return 0.6;
                if ("AMBULANCE".equals(unitType)) return 0.3;
                return 0.0;
            case BIOHAZARD:
            case CRYOGENIC_LEAK:
                if ("BCU".equals(unitType))       return 1.0;
                if ("FIRETRUCK".equals(unitType)) return 0.2;
                return 0.0;
            default:
                return 0.0;
        }
    }

    /** Normalized inverse distance score: closer = higher score */
    private static double distanceScore(int ux, int uy, int ix, int iy) {
        double dist = Math.sqrt(Math.pow(ux - ix, 2) + Math.pow(uy - iy, 2));
        return 1.0 - (dist / GRID_DIAG);
    }

    /** IDLE = fully available, EN_ROUTE = partially, others = 0 */
    private static double workloadScore(String status) {
        switch (status) {
            case "IDLE":     return 1.0;
            case "EN_ROUTE": return 0.3;
            default:         return 0.0;
        }
    }

    /** Higher severity = higher urgency bonus (normalized to [0,1]) */
    private static double severityBonus(int severity) {
        return severity / 10.0;
    }

    public static double euclideanDistance(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }
}
