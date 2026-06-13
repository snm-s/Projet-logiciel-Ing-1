# Projet-logiciel-Ing-1

<div align="center">

<!-- LOGO / TITRE -->

```
███████╗██╗      ██████╗  ██████╗ ██████╗ ██████╗  ██████╗ ██╗   ██╗████████╗███████╗
██╔════╝██║     ██╔═══██╗██╔═══██╗██╔══██╗██╔══██╗██╔═══██╗██║   ██║╚══██╔══╝██╔════╝
█████╗  ██║     ██║   ██║██║   ██║██║  ██║██████╔╝██║   ██║██║   ██║   ██║   █████╗  
██╔══╝  ██║     ██║   ██║██║   ██║██║  ██║██╔══██╗██║   ██║██║   ██║   ██║   ██╔══╝  
██║     ███████╗╚██████╔╝╚██████╔╝██████╔╝██║  ██║╚██████╔╝╚██████╔╝   ██║   ███████╗
╚═╝     ╚══════╝ ╚═════╝  ╚═════╝ ╚═════╝ ╚═╝  ╚═╝ ╚═════╝  ╚═════╝   ╚═╝   ╚══════╝
```

# FloodRoute

**Simulation d'inondation avec agents et graphes — Application JavaFX**

<br/>

[![Java](https://img.shields.io/badge/Java-21-007396?style=for-the-badge&logo=java&logoColor=white)](https://www.java.com)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-0078D7?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjfx.io)
[![Maven](https://img.shields.io/badge/Maven-3.9-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)](https://maven.apache.org)
[![License](https://img.shields.io/badge/Licence-Académique-4A90E2?style=for-the-badge)](.)
[![Status](https://img.shields.io/badge/Statut-Soutenu-22c55e?style=for-the-badge)](.)

<br/>

> _Anticiper. Alerter. Protéger._

<br/>

**Projet de fin d'année — ING1 GI4 · CY Tech · 2025–2026**  
Matière : Agents et Graphes · Tutrice : D. Zaouche · Jury : S. Hawari

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

L'application distingue quatre profils, chacun disposant d'une interface et de droits spécifiques :

| Profil | Description | Accès |
|--------|-------------|-------|
| 🧑‍💼 **Administrateur** | Gère la simulation, configure le graphe, déclenche les alertes | Complet |
| 🚒 **Secours** | Consulte les zones, reçoit les missions, met à jour les statuts | Opérationnel |
| 🧍 **Citoyen** | Consulte la carte, reçoit les alertes, calcule son itinéraire | Lecture + navigation |
| ♿ **PMR** | Même droits que le citoyen, mais avec calcul d'itinéraire adapté | Lecture + navigation adaptée |

<br/>

---

<br/>

## 🛠️ Technologies utilisées

```
┌─────────────────────────────────────────────────────────┐
│  Langage         Java 21                                │
│  Interface       JavaFX 21 + FXML                       │
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
│       │   ├── view/                   # Vues construites en Java (WelcomeView, LoginView…)
│       │   └── model/                  # Modèles de données (User, Alert, Refuge…)
│       │
│       └── resources/
│           ├── data/                   # Données du graphe (villes, routes)
│           ├── images/                 # Assets graphiques
│           └── css/                    # Feuilles de style JavaFX
│
├── pom.xml                             # Configuration Maven
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

Chaque agent est une entité autonome dotée d'un **état**, d'une **position** et d'un **comportement** :

```java
// Exemple de hiérarchie d'agents
Agent (abstraite)
├── Citoyen        → se déplace, reçoit des alertes, cherche un refuge
├── PMR            → contraintes de mobilité supplémentaires
├── Secours        → intervient sur les zones critiques, prioritaire
└── Administrateur → contrôle la simulation, gère le graphe
```

### Algorithme de calcul d'itinéraire

L'itinéraire sécurisé est calculé via une adaptation de l'algorithme de **Dijkstra** qui exclut dynamiquement les arêtes inondées ou bloquées :

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

### Compiler le projet

```bash
mvn clean compile
```

### Lancer l'application

```bash
mvn javafx:run
```

### Générer le JAR exécutable

```bash
mvn clean package
java -jar target/FloodRoute-1.0.jar
```

> **Note :** Si JavaFX n'est pas inclus dans votre JDK, ajoutez les options `--module-path` et `--add-modules` correspondantes à votre installation.

<br/>

---

<br/>

## 👨‍💻 Organisation de l'équipe

| Membre | Rôle principal |
|--------|----------------|
| **Martial Mouttalapane** | Architecture logicielle, moteur de simulation, algorithmes |
| **Sanem Sayed** | Modèle d'agents, logique métier, gestion des profils |
| **Hajar Achour** | Interface graphique JavaFX, design des vues, CSS |
| **Bouchra Zamoum** | Modélisation du graphe, structure des données, tests |
| **Jenistar Makoudjou** | Système d'alertes, statistiques, intégration finale |

<br/>

> _Tutrice : **D. Zaouche** — Jury : **S. Hawari**_

<br/>

---

<br/>

## ⚠️ Difficultés rencontrées

- **Synchronisation de la simulation** — gérer la propagation de l'inondation en temps réel sans bloquer le thread JavaFX
- **Mise à jour dynamique du graphe** — recalculer les itinéraires à chaque changement d'état des routes sans recréer le graphe entier
- **Gestion des conflits d'accès concurrents** — plusieurs agents interagissant simultanément avec les mêmes nœuds
- **Différenciation des interfaces par profil** — construire des vues flexibles sans dupliquer le code de navigation
- **Modélisation réaliste des contraintes PMR** — pondérer correctement les arêtes selon le type d'agent

<br/>

---

<br/>

## ✅ Solutions apportées

- Utilisation du **JavaFX Application Thread** avec `Platform.runLater()` pour toutes les mises à jour visuelles depuis les threads de simulation
- Implémentation d'un **observateur sur le graphe** (pattern Observer) pour déclencher le recalcul d'itinéraires uniquement en cas de changement d'état
- Synchronisation des accès aux structures partagées via des **verrous explicites** (`ReentrantLock`)
- Mise en place d'un **système de routing centralisé** dans `Main.java` pour gérer les transitions entre vues de manière unifiée
- Ajout d'un **attribut de type d'agent** dans l'algorithme de Dijkstra pour filtrer les arêtes inaccessibles aux PMR

<br/>

---

<br/>

## 📈 Résultats obtenus

- ✔️ Simulation d'inondation fonctionnelle avec propagation progressive
- ✔️ Calcul d'itinéraire sécurisé opérationnel pour tous les profils
- ✔️ Interface graphique complète et cohérente (welcome, connexion, inscription, carte, alertes, statistiques)
- ✔️ Gestion des quatre profils utilisateurs avec droits différenciés
- ✔️ Système d'alertes en temps réel intégré
- ✔️ Architecture modulaire facilitant l'extension future

<br/>

---

<br/>

## 🔭 Perspectives d'amélioration

- **Import de carte réelle** — intégrer les données OpenStreetMap pour simuler sur une vraie ville
- **Persistance des données** — remplacer les fichiers JSON par une base de données embarquée (SQLite)
- **Simulation multi-scénarios** — permettre de sauvegarder et rejouer des scénarios d'inondation
- **Mode réseau** — permettre à plusieurs utilisateurs de se connecter simultanément à une même simulation
- **Apprentissage des agents** — introduire des comportements adaptatifs via des règles ou du renforcement simple
- **Export de rapports** — générer un rapport PDF de la simulation (zones touchées, agents mobilisés, temps de réponse)

<br/>

---

<br/>

## 📝 Conclusion

FloodRoute illustre comment les concepts de **graphes** et de **systèmes multi-agents** peuvent être appliqués à une problématique concrète de gestion de crise. Au-delà de l'aspect académique, le projet démontre qu'une modélisation formelle rigoureuse peut servir de socle à une application interactive et utilisable.

Le travail en équipe et les contraintes techniques rencontrées ont permis d'approfondir notre maîtrise de Java, de JavaFX et des algorithmes de graphes, tout en nous confrontant aux exigences d'un développement logiciel structuré.

<br/>

---

<br/>

<div align="center">

**FloodRoute** · ING1 GI4 · CY Tech · 2025–2026

_Anticiper. Alerter. Protéger._

</div>
