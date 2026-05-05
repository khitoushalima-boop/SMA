# SRUU — Système de Réponse aux Urgences Urbaines
## Implémentation JADE | M1 Génie Logiciel

---

## 📁 Structure du projet

```
SRUU/
├── src/
│   └── sruu/
│       ├── MainLauncher.java              ← Point d'entrée
│       ├── agents/
│       │   ├── SensorAgent.java           ← Pôle 1 : Capteur
│       │   ├── LoggerAgent.java           ← Pôle 1 : Enregistreur
│       │   ├── DispatcherAgent.java       ← Pôle 2 : Répartiteur (Contract Net)
│       │   ├── TrafficControllerAgent.java← Pôle 2 : Contrôleur de trafic
│       │   ├── MedicalCoordinatorAgent.java← Pôle 2 : Coordinateur médical
│       │   ├── AmbulanceAgent.java        ← Pôle 3 : Ambulance (FSM)
│       │   ├── FireTruckAgent.java        ← Pôle 3 : Camion de pompiers (FSM+ABORT)
│       │   ├── PoliceAgent.java           ← Pôle 3 : Police (FSM+Patrol)
│       │   └── BiohazardContainmentUnitAgent.java ← BCU (FSM)
│       ├── ontology/
│       │   ├── IncidentType.java          ← Enum des types d'incidents
│       │   ├── Incident.java              ← Objet incident sérialisable
│       │   └── UnitProposal.java          ← Objet proposition (Contract Net)
│       └── utils/
│           └── UtilityCalculator.java     ← Fonction d'utilité du Répartiteur
├── lib/
│   └── jade.jar                           ← À placer ici !
├── build.sh
└── README.md
```

---

## ⚙️ Prérequis

- **Java 8+**
- **JADE** : téléchargez `jade.jar` depuis https://jade.tilab.com/
  - Placez `jade.jar` dans le dossier `lib/`

---

## 🚀 Compilation & Lancement

### Linux / macOS
```bash
chmod +x build.sh
./build.sh
```

### Windows
```bat
mkdir out
javac -cp lib\jade.jar -sourcepath src -d out src\sruu\MainLauncher.java src\sruu\agents\*.java src\sruu\ontology\*.java src\sruu\utils\*.java
java -cp out;lib\jade.jar sruu.MainLauncher
```

### Manuellement (Linux)
```bash
mkdir -p out
javac -cp lib/jade.jar -sourcepath src -d out \
  src/sruu/ontology/*.java \
  src/sruu/utils/*.java \
  src/sruu/agents/*.java \
  src/sruu/MainLauncher.java

java -cp out:lib/jade.jar sruu.MainLauncher
```

---

## 🤖 Agents du système (9 types)

| Agent | Rôle | Capacités DF |
|-------|------|-------------|
| `SensorAgent` | Détecte et signale les incidents | — |
| `DispatcherAgent` | Coordinateur central (Contract Net) | — |
| `AmbulanceAgent` | Soins médicaux | `MEDICAL` |
| `FireTruckAgent` | Incendies et sauvetage | `FIRE`, `RESCUE` |
| `PoliceAgent` | Périmètre et foule | `CROWD_CONTROL`, `PERIMETER` |
| `MedicalCoordinatorAgent` | Courtier hôpital-ambulance | — |
| `TrafficControllerAgent` | Corridors d'urgence | — |
| `BiohazardContainmentUnitAgent` | Incidents BIOHAZARD/CRYOGENIC | `BIOHAZARD_CONTAINMENT` |
| `LoggerAgent` | Audit passif + rapport final | — |

---

## 📐 Fonction d'Utilité (Répartiteur)

```
U(u, i) = 0.40 × TypeMatch(u,i)
         + 0.30 × (1 - DistNorm(u,i))
         + 0.20 × WorkloadScore(u)
         + 0.10 × SeverityBonus(i)
```

- **TypeMatch** : 1.0 = unité primaire, 0.5 = capacité secondaire, 0.0 = incompatible
- **DistNorm** : distance euclidienne normalisée sur diagonale de la grille (0→100)
- **WorkloadScore** : 1.0 (IDLE), 0.3 (EN_ROUTE), 0.0 (autres)
- **SeverityBonus** : gravité / 10

---

## 📊 Scénarios de démonstration

### Scénario 1 — Deux incidents simultanés (FIRE + MEDICAL)
Les capteurs `Sensor_ZoneA` et `Sensor_ZoneB` déclenchent des incidents.
Le Dispatcher lance deux Contract Net en parallèle.

### Scénario 2 — Conflit de ressources
`FireTruck1` est assigné à un FIRE.
Un second FIRE arrive → `FireTruck2` est sélectionné automatiquement.
Si FireTruck1 manque d'eau → ABORT → le Dispatcher réassigne.

### Scénario 3 — Rapport Logger
Après 3 minutes, `LoggerAgent` génère `sruu_log.txt` avec :
- Tous les événements horodatés
- Temps de réponse moyen
- Incidents non résolus / abandonnés

---

## 📜 Protocoles FIPA utilisés

- **FIPA Contract Net** : CFP → PROPOSE/REFUSE → ACCEPT/REJECT
- **Performatives** : INFORM, REQUEST, CFP, PROPOSE, ACCEPT_PROPOSAL, REJECT_PROPOSAL, REFUSE, FAILURE
- **Ontologie** : `EmergencyOntology` sur tous les messages ACL
- **DF** : toutes les unités s'enregistrent, le Dispatcher fait des recherches DF (pas de hard-coding)
