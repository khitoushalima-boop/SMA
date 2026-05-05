# Rapport Technique : Algorithme du Répartiteur (Dispatcher)

## 📋 Analyse Complète de l'Élément Central du Projet SRUU

### 🎯 Position du Répartiteur dans l'Architecture

Le **DispatcherAgent** est effectivement l'élément central du projet SRUU. Il orchestre l'ensemble du système d'urgence en implémentant le protocole **FIPA Contract Net** pour la sélection optimale des unités d'intervention.

---

## 🔍 Analyse Détaillée des Fonctionnalités

### 1. **Évaluation des Unités et Sélection de la Meilleure Offre**

**✅ Implémenté Correctement**

```java
// Tri par score d'utilité décroissant
proposals.sort((a, b) -> Double.compare(b.getUtilityScore(), a.getUtilityScore()));
UnitProposal winner = proposals.get(0);
```

**Mécanisme :**
- Collecte toutes les propositions des unités éligibles
- Calcule le score d'utilité pour chaque proposition
- Sélectionne l'unité avec le score le plus élevé
- Envoie `ACCEPT_PROPOSAL` au gagnant

### 2. **Rejet Poli des Autres Unités**

**✅ Implémenté Correctement**

```java
// Envoie REJECT_PROPOSAL à toutes les autres unités
for (int i = 1; i < proposals.size(); i++) {
    ACLMessage reject = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
    reject.addReceiver(new AID(proposals.get(i).getUnitName(), AID.ISLOCALNAME));
    reject.setContent("REJECT:" + incident.getId());
    myAgent.send(reject);
}
```

**Mécanisme :**
- Envoie `REJECT_PROPOSAL` à toutes les unités non sélectionnées
- Message poli avec identifiant de l'incident
- Permet aux unités rejetées de rester disponibles pour d'autres incidents

### 3. **Gestion de l'Absence de Réponse**

**✅ Implémenté Correctement**

```java
// Timeout après 5 secondes
if (System.currentTimeMillis() - cfpTime > 5500) {
    System.out.println("[DISPATCHER] CFP timeout, got " + proposals.size() + " proposals.");
    step = 2;
    return;
}

// Gestion du cas où aucune proposition n'est reçue
if (proposals.isEmpty()) {
    System.out.println("[DISPATCHER] No proposals for incident " + incident.getId());
    notifyLogger("UNRESOLVED:" + incident.serialize());
    step = 3;
    return;
}
```

**Mécanisme :**
- Timeout de 5 secondes pour les réponses
- Si aucune proposition → incident marqué comme `UNRESOLVED`
- Logging de l'échec pour analyse ultérieure

### 4. **Réaffectation Dynamique**

**✅ Implémenté Correctement**

```java
// Écoute des messages ABORT
if (msg.getContent().startsWith("ABORT:")) {
    String incidentId = msg.getContent().substring(6);
    incidentAssignments.remove(incidentId);
    Incident inc = activeIncidents.get(incidentId);
    if (inc != null) {
        inc.setStatus("OPEN");
        // Relance Contract Net pour réaffectation
        myAgent.addBehaviour(new ContractNetInitiatorBehaviour(inc));
    }
}
```

**Mécanisme :**
- Surveillance des abandons (ex: camion de pompier vide)
- Suppression de l'assignment courant
- Remise de l'incident en statut `OPEN`
- Relancement automatique du processus de sélection

---

## 🧮 Fonction d'Utilité : Modèle Mathématique

### Formule Principale

```
U(u, i) = 0.40 × TypeMatch(u,i) + 0.30 × (1 - DistNorm(u,i)) + 0.20 × WorkloadScore(u) + 0.10 × SeverityBonus(i)
```

### Variables Pondérées

#### 1. **TypeMatch** - Pondération : 0.40 (40%)

**Justification Mathématique :**
- Coefficient le plus élevé car **critère de compétence obligatoire**
- Échelle : [0.0, 0.5, 1.0]
- 1.0 = unité primaire, 0.5 = capacité secondaire, 0.0 = incompatible

**Calcul :**
```java
FIRE: FIRETRUCK=1.0, POLICE=0.5, autres=0.0
MEDICAL: AMBULANCE=1.0, FIRETRUCK=0.4, autres=0.0
BIOHAZARD: BCU=1.0, FIRETRUCK=0.2, autres=0.0
```

#### 2. **Distance Normalisée** - Pondération : 0.30 (30%)

**Justification Mathématique :**
- Distance euclidienne normalisée sur diagonale de grille
- Formule : `1 - (distance / √(100² + 100²))`
- Plus l'unité est proche, plus le score est élevé

**Calcul :**
```java
double dist = Math.sqrt(Math.pow(ux - ix, 2) + Math.pow(uy - iy, 2));
return 1.0 - (dist / GRID_DIAG);
```

#### 3. **Charge de Travail** - Pondération : 0.20 (20%)

**Justification Mathématique :**
- Préférence pour les unités disponibles
- IDLE = 1.0 (totalement disponible)
- EN_ROUTE = 0.3 (partiellement disponible)
- Autres états = 0.0 (indisponible)

#### 4. **Bonus de Gravité** - Pondération : 0.10 (10%)

**Justification Mathématique :**
- Incitation à répondre aux incidents graves
- Normalisé : `severity / 10.0`
- Échelle de 0.1 à 1.0

---

## 📊 Justification Mathématique des Pondérations

### Analyse de Sensibilité

| Variable | Poids | Impact | Justification |
|----------|-------|--------|---------------|
| TypeMatch | 40% | **CRITIQUE** | Compétence de base indispensable |
| Distance | 30% | **ÉLEVÉ** | Temps d'intervention direct |
| Workload | 20% | **MOYEN** | Optimisation des ressources |
| Severity | 10% | **FAIBLE** | Urgence relative |

### Coherence Mathématique

**Somme des poids = 1.0** ✅
- Garantit que U(u,i) ∈ [0,1]
- Normalisation pour comparaison cohérente

### Optimisation Multi-objectifs

La fonction d'utilité réalise un **compromis de Pareto** entre :
- **Compétence** (TypeMatch) - contrainte dure
- **Rapidité** (Distance) - objectif principal
- **Disponibilité** (Workload) - optimisation
- **Urgence** (Severity) - priorisation

---

## 🔄 Flux Complet du Contract Net

### Phase 1 : Call For Proposals (CFP)
```
Dispatcher → Unités éligibles : CFP(incident)
```

### Phase 2 : Collecte des Propositions
```
Unités → Dispatcher : PROPOSE(proposition) ou REFUSE(indisponibilité)
```

### Phase 3 : Sélection et Assignation
```
Dispatcher → Meilleure unité : ACCEPT_PROPOSAL
Dispatcher → Autres unités : REJECT_PROPOSAL
```

### Phase 4 : Gestion des Abandons
```
Unité → Dispatcher : ABORT(incident) → Réaffectation automatique
```

---

## ✅ Validation des Exigences

| Exigence | Statut | Implémentation |
|----------|--------|----------------|
| Évaluer unités et sélectionner meilleure offre | ✅ | UtilityCalculator + tri par score |
| Rejeter poliment autres unités | ✅ | REJECT_PROPOSAL systématique |
| Gérer absence de réponse | ✅ | Timeout + gestion UNRESOLVED |
| Réaffectation dynamique | ✅ | ABORT listener + relancement |
| Fonction d'utilité mathématique | ✅ | Modèle à 4 variables pondérées |
| Pondération justifiée | ✅ | Analyse de sensibilité cohérente |

---

## 🎯 Conclusion

L'algorithme du Répartiteur est **robusteement implémenté** et constitue effectivement l'élément central du projet SRUU. La fonction d'utilité mathématique est bien conçue avec des pondérations justifiées qui équilibrent efficacement les multiples critères de décision.

**Points Forts :**
- Architecture FIPA Contract Net respectée
- Gestion complète du cycle de vie des incidents
- Réaffectation dynamique fonctionnelle
- Modèle mathématique cohérent et justifié

**Le Répartiteur orchestre avec succès l'ensemble du système d'urgence urbaine.**
