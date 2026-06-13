<div align="center">

```
███████╗██╗      ██████╗  ██████╗ ██████╗ ██████╗  ██████╗ ██╗   ██╗████████╗███████╗
██╔════╝██║     ██╔═══██╗██╔═══██╗██╔══██╗██╔══██╗██╔═══██╗██║   ██║╚══██╔══╝██╔════╝
█████╗  ██║     ██║   ██║██║   ██║██║  ██║██████╔╝██║   ██║██║   ██║   ██║   █████╗  
██╔══╝  ██║     ██║   ██║██║   ██║██║  ██║██╔══██╗██║   ██║██║   ██║   ██║   ██╔══╝  
██║     ███████╗╚██████╔╝╚██████╔╝██████╔╝██║  ██║╚██████╔╝╚██████╔╝   ██║   ███████╗
╚═╝     ╚══════╝ ╚═════╝  ╚═════╝ ╚═════╝ ╚═╝  ╚═╝ ╚═════╝  ╚═════╝   ╚═╝   ╚══════╝
```

<img src="src/main/resources/images/header.svg" alt="FloodRoute Banner" width="100%"/>

<br/>

[![Java](https://img.shields.io/badge/Java-21-007396?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.java.com)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-0078D7?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjfx.io)
[![Maven](https://img.shields.io/badge/Maven-3.9-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)](https://maven.apache.org)
[![Licence](https://img.shields.io/badge/Licence-Académique-4A90E2?style=for-the-badge)](.)
[![Statut](https://img.shields.io/badge/Statut-Soutenu-22c55e?style=for-the-badge)](.)

<br/>

**Projet de fin d'année — ING1 GI4 · CY Tech · 2025–2026**  
Matière : Agents et Graphes &nbsp;·&nbsp; Tutrice : D. Zaouche &nbsp;·&nbsp; Jury : S. Hawari

</div>

---

<br/>

## 📌 Présentation du projet

**FloodRoute** est une application de bureau développée en **JavaFX** dans le cadre du projet de fin d'année de la matière *Agents et Graphes* (ING1 GI4, CY Tech, 2025–2026).

Le projet répond à une problématique concrète : comment modéliser, simuler et gérer une situation d'inondation urbaine en temps réel, en intégrant différents types d'agents (citoyens, secours, administrateurs) sur un graphe représentant le réseau routier d'une ville ?

L'application propose une interface graphique complète permettant de visualiser l'évolution d'une inondation, de calculer des itinéraires sécurisés vers des refuges et de coordonner les équipes de secours.

<br/>

---

<br/>

## 🎯 Objectif de l'application

L'objectif principal de FloodRoute est de fournir un outil de simulation réaliste permettant de :

- **Modéliser** un réseau urbain sous forme de graphe pondéré et orienté
- **Simuler** la progression d'une inondation nœud par nœud en temps réel
- **Calculer** des itinéraires sécurisés depuis n'importe quelle position vers un refuge accessible
- **Coordonner** les agents (secours, citoyens, PMR) selon leurs contraintes propres
- **Alerter** les utilisateurs en fonction de l'évolution de la situation

<br/>

---

<br/>

## ⚙️ Fonctionnalités principales

### 🗺️ Carte et graphe
- Représentation de la ville sous forme de **graphe** (nœuds = lieux, arêtes = routes)
- Trois états de route : **praticable**, **inondée**, **bloquée**
- Visualisation dynamique avec coloration en temps réel

### 🌊 Simulation d'inondation
- Propagation progressive de l'inondation sur le graphe
- Mise à jour automatique des routes accessibles
- Déclenchement d'alertes selon le niveau d'inondation

### 🧭 Calcul d'itinéraire sécurisé
- Algorithme de plus court chemin adapté à l'état du graphe
- Prise en compte des contraintes de mobilité (PMR)
- Affichage du chemin optimal vers les refuges disponibles

### 👤 Gestion des utilisateurs
- Authentification avec création de compte
- Interfaces différenciées selon le profil utilisateur
- Historique et statistiques par session

### 📊 Statistiques et alertes
- Tableau de bord en temps réel
- Système d'alertes par niveau de criticité
- Suivi des agents et des refuges occupés

<br/>

---

<br/>

## 👥 Utilisateurs de l'application

| Profil | Description | Accès |
|--------|-------------|-------|
| 🧑‍💼 **Administrateur** | Gère la simulation, configure le graphe, déclenche les alertes | Complet |
| 🚒 **Secours** | Consulte les zones, reçoit les missions, met à jour les statuts | Opérationnel |
| 🧍 **Citoyen** | Consulte la carte, reçoit les alertes, calcule son itinéraire | Lecture + navigation |
| ♿ **PMR** | Même droits que le citoyen, itinéraire adapté aux contraintes de mobilité | Lecture + navigation adaptée |

<br/>

---

<br/>

## 🛠️ Technologies utilisées

```
┌─────────────────────────────────────────────────────────┐
│  Langage         Java 21                                │
│  Interface       JavaFX 21                              │
│  Build           Apache Maven 3.9                       │
│  Algorithmes     Dijkstra, BFS, gestion d'agents        │
│  Données         JSON / fichiers texte structurés       │
│  Versionning     Git + GitHub                           │
│  IDE             IntelliJ IDEA / Eclipse                │
└─────────────────────────────────────────────────────────┘
```

<br/>

---

<br/>

## 🏗️ Architecture du projet

```
FloodRoute/
│
├── src/
│   └── main/
│       ├── java/
│       │   ├── app/                    # Point d'entrée (Main.java)
│       │   ├── agent/                  # Modèles d'agents (Citoyen, Secours, PMR, Admin)
│       │   ├── graph/                  # Modèle du graphe (Nœud, Arête, Graphe)
│       │   ├── simulation/             # Moteur de simulation d'inondation
│       │   ├── algorithm/              # Algorithmes de pathfinding (Dijkstra, BFS)
│       │   ├── controller/             # Contrôleurs JavaFX
│       │   ├── view/                   # Vues construites en Java
│       │   └── model/                  # Modèles de données (User, Alert, Refuge…)
│       │
│       └── resources/
│           ├── data/                   # Données du graphe (villes, routes)
│           ├── images/                 # Assets graphiques
│           └── css/                    # Feuilles de style JavaFX
│
├── pom.xml
└── README.md
```

<br/>

---

<br/>

## 🔬 Modélisation avec agents et graphes

### Structure du graphe

Le réseau urbain est modélisé comme un **graphe orienté pondéré** :

- **Nœuds** — lieux importants de la ville (carrefours, refuges, zones résidentielles…)
- **Arêtes** — routes reliant ces lieux, pondérées par la distance ou le temps de trajet
- **État dynamique** — chaque nœud et chaque arête possède un état (`LIBRE`, `INONDÉ`, `BLOQUÉ`) mis à jour en cours de simulation

### Modèle d'agents

```java
// Hiérarchie d'agents
Agent (abstraite)
├── Citoyen        → se déplace, reçoit des alertes, cherche un refuge
├── PMR            → contraintes de mobilité supplémentaires
├── Secours        → intervient sur les zones critiques, prioritaire
└── Administrateur → contrôle la simulation, gère le graphe
```

### Algorithme de calcul d'itinéraire

```java
// Schéma simplifié
ShortestPath.compute(graphe, source, refuge, etatCourant);
// → retourne le chemin optimal parmi les routes praticables
```

<br/>

---

<br/>

## 🚀 Installation et lancement

### Prérequis

- **Java 21** ou supérieur — [Télécharger](https://adoptium.net/)
- **Maven 3.9** ou supérieur — [Télécharger](https://maven.apache.org/download.cgi)
- **JavaFX SDK 21** — [Télécharger](https://openjfx.io/)

### Cloner le dépôt

```bash
git clone https://github.com/<votre-organisation>/FloodRoute.git
cd FloodRoute
```

### Compiler et lancer

```bash
mvn clean compile
mvn javafx:run
```

### Générer le JAR

```bash
mvn clean package
java -jar target/FloodRoute-1.0.jar
```

<br/>

---

<br/>

## 👨‍💻 Organisation de l'équipe

| Membre | Rôle principal |
|--------|----------------|
| **Martial Mouttalapane** | Architecture logicielle, moteur de simulation, algorithmes |
| **Sanem Sayed** | Modèle d'agents, logique métier, gestion des profils |
| **Hajar Achour** | Interface graphique JavaFX, design des vues |
| **Bouchra Zamoum** | Modélisation du graphe, structure des données, tests |
| **Jenistar Makoudjou** | Système d'alertes, statistiques, intégration finale |

<br/>

> Tutrice : **D. Zaouche** &nbsp;·&nbsp; Jury : **S. Hawari**

<br/>

---

<br/>

## ⚠️ Difficultés rencontrées

- **Synchronisation de la simulation** — gérer la propagation sans bloquer le thread JavaFX
- **Mise à jour dynamique du graphe** — recalculer les itinéraires sans recréer le graphe entier
- **Gestion des accès concurrents** — plusieurs agents interagissant simultanément sur les mêmes nœuds
- **Différenciation des interfaces par profil** — vues flexibles sans duplication de code
- **Modélisation des contraintes PMR** — pondération correcte des arêtes selon le type d'agent

<br/>

---

<br/>

## ✅ Solutions apportées

- Utilisation de `Platform.runLater()` pour toutes les mises à jour visuelles depuis les threads de simulation
- Pattern **Observer** sur le graphe pour déclencher le recalcul d'itinéraires uniquement en cas de changement d'état
- Synchronisation via **`ReentrantLock`** sur les structures partagées
- **Routing centralisé** dans `Main.java` pour les transitions entre vues
- Attribut de type d'agent dans Dijkstra pour filtrer les arêtes inaccessibles aux PMR

<br/>

---

<br/>

## 📈 Résultats obtenus

- ✔️ Simulation d'inondation fonctionnelle avec propagation progressive
- ✔️ Calcul d'itinéraire sécurisé opérationnel pour tous les profils
- ✔️ Interface graphique complète et cohérente
- ✔️ Gestion des quatre profils utilisateurs avec droits différenciés
- ✔️ Système d'alertes en temps réel intégré
- ✔️ Architecture modulaire facilitant l'extension future

<br/>

---

<br/>

## 🔭 Perspectives d'amélioration

- **Import de carte réelle** — intégrer les données OpenStreetMap
- **Persistance des données** — base de données embarquée (SQLite)
- **Simulation multi-scénarios** — sauvegarder et rejouer des scénarios
- **Mode réseau** — plusieurs utilisateurs connectés à une même simulation
- **Export de rapports** — générer un PDF de la simulation

<br/>

---

<br/>

## 📝 Conclusion

FloodRoute illustre comment les concepts de **graphes** et de **systèmes multi-agents** peuvent être appliqués à une problématique concrète de gestion de crise. Au-delà de l'aspect académique, le projet démontre qu'une modélisation formelle rigoureuse peut servir de socle à une application interactive et utilisable.

<br/>

---

<br/>

<div align="center">

**FloodRoute** &nbsp;·&nbsp; ING1 GI4 &nbsp;·&nbsp; CY Tech &nbsp;·&nbsp; 2025–2026

_Anticiper. Alerter. Protéger._

</div>
