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


**Projet de fin d'année — ING1 GI4 · CY Tech · 2025–2026**  
Matière : Agents et Graphes &nbsp;·&nbsp; Tutrice : D. Zaouche &nbsp;·&nbsp; Jury : S. Hawari

</div>


<div align="center">

<img width="100%" src="https://capsule-render.vercel.app/api?type=waving&height=200&color=0:001B44,35:003B8F,70:006DFF,100:00B7FF&text=FloodRoute&fontColor=FFFFFF&fontSize=80&fontAlignY=42&desc=Simulation%20d'inondation%20avec%20agents%20et%20graphes&descAlignY=62&descSize=20"/>



<img src="src/main/resources/images/header.svg" alt="FloodRoute Banner" width="100%"/>

<br/>

> _Anticiper. Alerter. Protéger._

<br/>

| Formation | Groupe | Matière | Année |
|:---------:|:------:|:-------:|:-----:|
| ING1 | GI4 | Agents et Graphes | 2025–2026 |

**CY Tech &nbsp;·&nbsp; Tutrice : D. Zaouche &nbsp;·&nbsp; Jury : S. Hawari**

</div>

---

<br/>

## 📌 Présentation du projet

> FloodRoute est une application de simulation d'inondation pensée pour représenter une situation de crise dans une ville.

La ville est modélisée sous forme de **graphe**. Les sommets représentent des lieux importants (quartiers, refuges, hôpitaux). Les arêtes représentent les routes reliant ces lieux. Selon l'évolution de l'inondation, une route peut rester praticable, devenir inondée ou être bloquée.

L'application propose plusieurs interfaces selon le profil de l'utilisateur : citoyen, secours ou administrateur. Elle prend également en compte les personnes à mobilité réduite afin de proposer une évacuation adaptée.

<br/>

<div align="center">

| Élément | Description |
|:-------:|-------------|
| **Sommets** | Lieux importants de la carte |
| **Arêtes** | Routes reliant les différents lieux |
| **Agents** | Utilisateurs présents dans la simulation |
| **Zones** | Espaces touchés ou surveillés |
| **Alertes** | Informations liées aux dangers |
| **Itinéraires** | Chemins proposés vers des zones plus sûres |

</div>

---

<br/>

## 🎯 Objectif du projet

L'objectif de FloodRoute est de fournir une application claire et interactive permettant de :

- **Visualiser** une ville sous forme de graphe et suivre l'état des routes
- **Simuler** l'évolution d'une inondation en temps réel
- **Alerter** les utilisateurs selon le niveau de danger
- **Orienter** les citoyens vers des refuges via des itinéraires sécurisés
- **Coordonner** les secours et permettre à l'administrateur de superviser le système

<br/>

<div align="center">

| 🗺️ Visualiser | 🔔 Alerter | 🤝 Coordonner |
|:-------------:|:---------:|:-------------:|
| Carte, graphe, zones et routes | Notifications, dangers et suivi | Citoyens, secours et administrateurs |

</div>

---

<br/>

## 👥 Profils utilisateurs

<div align="center">

| Profil | Description | Accès |
|--------|-------------|-------|
| 🧑‍💼 **Administrateur** | Lance la simulation, gère le graphe, supervise le système | Complet |
| 🚒 **Secours** | Visualise les zones critiques, suit les agents, coordonne les missions | Opérationnel |
| 🧍 **Citoyen** | Consulte la carte, reçoit les alertes, calcule son itinéraire | Lecture + navigation |
| ♿ **PMR** | Mêmes droits que le citoyen avec un itinéraire adapté aux contraintes de mobilité | Navigation adaptée |

</div>

---

<br/>

## ⚙️ Fonctionnalités principales

<table>
<tr>
<td width="50%" valign="top">

### 🔐 Authentification
- Connexion et création de compte
- Gestion des profils utilisateurs
- Réinitialisation du mot de passe
- Validation sécurisée

</td>
<td width="50%" valign="top">

### 🗺️ Carte et graphe
- Affichage d'une carte interactive (Leaflet)
- Représentation en graphe
- Gestion des sommets et des arêtes
- Visualisation de l'état des routes

</td>
</tr>
<tr>
<td width="50%" valign="top">

### 🌊 Simulation
- Lancement et contrôle de la simulation
- Propagation progressive de l'inondation
- Mise à jour dynamique des zones
- Détection du niveau d'eau (capteurs)

</td>
<td width="50%" valign="top">

### 🔔 Alertes
- Création et diffusion d'alertes
- Notification des utilisateurs en temps réel
- Signalement des dangers
- Gestion des types et niveaux d'alertes

</td>
</tr>
<tr>
<td width="50%" valign="top">

### 🧭 Itinéraires
- Calcul de trajet sécurisé (Dijkstra)
- Recherche du refuge le plus proche
- Évitement des routes bloquées ou inondées
- Adaptation aux contraintes PMR

</td>
<td width="50%" valign="top">

### 📊 Statistiques
- Tableau de bord par profil
- Suivi des agents et des refuges
- Historique des alertes et missions
- Vue globale de l'état du système

</td>
</tr>
</table>

---

<br/>

## 🔬 Modélisation avec agents et graphes

### Structure du graphe

```
Ville
│
├── Sommets
│   ├── Quartiers
│   ├── Refuges
│   ├── Hôpitaux
│   └── Points de passage
│
├── Arêtes
│   ├── Routes praticables
│   ├── Routes inondées
│   └── Routes bloquées
│
└── Agents
    ├── Citoyens
    ├── Personnes à mobilité réduite (PMR)
    ├── Secours
    └── Administrateurs
```

### Modèle d'agents

```java
// Hiérarchie d'agents
Agent (abstraite)
├── Citoyen        → se déplace, reçoit des alertes, cherche un refuge
├── PMRAgent       → contraintes de mobilité supplémentaires
├── RescueAgent    → intervient sur les zones critiques, prioritaire
└── AdminAgent     → contrôle la simulation, gère le graphe
```

### Algorithme d'évacuation

```java
// Calcul d'itinéraire sécurisé — extrait simplifié
EvacuationRouter.compute(graph, source, refuge, currentState);
// → retourne le chemin optimal parmi les routes praticables
```

<div align="center">

<br/>

| État | Description |
|:----:|-------------|
| ✅ **Praticable** | La route peut être empruntée normalement |
| 🌊 **Inondée** | La route devient dangereuse |
| 🚫 **Bloquée** | La route n'est plus utilisable |

</div>

---

<br/>

## 🏗️ Architecture du projet

```
Projet-logiciel-Ing-1/
│
├── data/
│   ├── users.json
│   └── zones.json
│
├── src/main/
│   ├── java/
│   │   ├── app/                        # Point d'entrée (Main.java)
│   │   ├── cli/                        # Version console (ConsoleApp.java)
│   │   ├── controller/                 # Contrôleurs JavaFX
│   │   │   ├── AdminPage/
│   │   │   ├── AuthPage/
│   │   │   ├── CitizenPage/
│   │   │   ├── RescuePage/
│   │   │   └── MapController.java
│   │   ├── model/
│   │   │   ├── agent/                  # Agents (Citoyen, PMR, Secours, Admin)
│   │   │   ├── alert/                  # Système d'alertes
│   │   │   ├── algorithms/             # Calcul d'itinéraires (Dijkstra)
│   │   │   ├── auth/                   # Authentification et mots de passe
│   │   │   ├── graph/                  # Graphe, nœuds, arêtes, routes
│   │   │   ├── observer/               # Pattern Observer
│   │   │   ├── sensor/                 # Capteurs de niveau d'eau
│   │   │   ├── simulation/             # Moteur de simulation
│   │   │   ├── statistics/             # Statistiques
│   │   │   └── strategy/               # Stratégies de comportement des agents
│   │   ├── service/                    # Services géocodage et adresse
│   │   └── view/                       # Toutes les vues JavaFX
│   │       ├── components/             # Header, Sidebar, GraphMap
│   │       ├── WelcomeView.java
│   │       ├── LoginView.java
│   │       ├── MapView.java
│   │       ├── SimulationView.java
│   │       └── ...
│   │
│   └── resources/
│       ├── css/                        # Feuilles de style JavaFX
│       ├── images/                     # Assets graphiques
│       ├── map/                        # Leaflet (carte interactive)
│       │   ├── leaflet.css
│       │   ├── leaflet.js
│       │   └── map.html
│       └── lyon_routes.json
│
├── pom.xml
└── README.md
```

<div align="center">

<br/>

| Package | Rôle |
|---------|------|
| `app` | Point d'entrée principal |
| `controller` | Relie les vues JavaFX au modèle |
| `model.agent` | Types d'agents et comportements |
| `model.alert` | Alertes et notifications |
| `model.algorithms` | Calcul des chemins d'évacuation |
| `model.auth` | Authentification et gestion des comptes |
| `model.graph` | Graphe, sommets, arêtes et routes |
| `model.simulation` | Logique de simulation de l'inondation |
| `model.strategy` | Stratégies de déplacement des agents |
| `service` | Géocodage et adresses |
| `view` | Interfaces JavaFX |

</div>

---

<br/>

## 🛠️ Technologies utilisées

<div align="center">

| Technologie | Usage |
|:-----------:|-------|
| **Java 21** | Langage principal |
| **JavaFX 21** | Interface graphique |
| **Apache Maven 3.9** | Gestion du projet et des dépendances |
| **Leaflet.js** | Carte interactive intégrée via WebView |
| **JSON** | Stockage des données (users, zones) |
| **CSS** | Style de l'interface JavaFX |
| **Git / GitHub** | Versionnement et collaboration |

</div>

---

<br/>

## 🚀 Installation et lancement

### Prérequis

- **Java 21** — [Télécharger](https://adoptium.net/)
- **Maven 3.9** — [Télécharger](https://maven.apache.org/download.cgi)
- **JavaFX SDK 21** — [Télécharger](https://openjfx.io/)

### Cloner le dépôt

```bash
git clone https://github.com/<votre-organisation>/FloodRoute.git
cd FloodRoute
```

### Lancer sur macOS

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH=$JAVA_HOME/bin:$PATH
mvn javafx:run
```

### Lancer sur Windows / Linux

```bash
mvn javafx:run
```

### Générer le JAR

```bash
mvn clean package
java -jar target/FloodRoute-1.0.jar
```

---

<br/>

## 👨‍💻 Équipe projet

<div align="center">

| Membre | Rôle |
|--------|------|
| **Martial Mouttalapane** | Architecture logicielle, moteur de simulation, algorithmes |
| **Sanem Sayed** | Modèle d'agents, logique métier, gestion des profils |
| **Hajar Achour** | Interface graphique JavaFX, design des vues |
| **Bouchra Zamoum** | Modélisation du graphe, structure des données, tests |
| **Jenistar Makoudjou** | Système d'alertes, statistiques, intégration finale |

<br/>

| Rôle | Personne |
|------|----------|
| **Tutrice** | D. Zaouche |
| **Jury** | S. Hawari |

</div>

---

<br/>

## ⚠️ Difficultés rencontrées

- **Synchronisation de la simulation** — gérer la propagation sans bloquer le thread JavaFX
- **Mise à jour dynamique du graphe** — recalculer les itinéraires sans recréer le graphe entier
- **Gestion des accès concurrents** — plusieurs agents interagissant simultanément sur les mêmes nœuds
- **Différenciation des interfaces par profil** — vues flexibles sans duplication de code
- **Modélisation des contraintes PMR** — pondération correcte des arêtes selon le type d'agent

---

<br/>

## ✅ Solutions apportées

- `Platform.runLater()` pour toutes les mises à jour visuelles depuis les threads de simulation
- Pattern **Observer** sur le graphe pour déclencher le recalcul uniquement en cas de changement d'état
- Synchronisation via **`ReentrantLock`** sur les structures partagées
- **Routing centralisé** dans `Main.java` pour les transitions entre vues
- Attribut de type d'agent dans l'algorithme pour filtrer les arêtes inaccessibles aux PMR

---

<br/>

## 📈 Résultats obtenus

- ✔️ Simulation d'inondation fonctionnelle avec propagation progressive
- ✔️ Calcul d'itinéraire sécurisé opérationnel pour tous les profils
- ✔️ Interface graphique complète et cohérente sur toutes les vues
- ✔️ Gestion des quatre profils avec droits différenciés
- ✔️ Système d'alertes en temps réel intégré
- ✔️ Architecture modulaire et extensible

---

<br/>

## 🔭 Perspectives d'amélioration

- **Import de carte réelle** — intégrer OpenStreetMap pour simuler sur une vraie ville
- **Persistance** — remplacer le JSON par une base SQLite embarquée
- **Multi-scénarios** — sauvegarder et rejouer des scénarios d'inondation
- **Mode réseau** — plusieurs utilisateurs connectés à une même simulation
- **Export PDF** — générer un rapport complet de la simulation

---

<br/>

## 📝 Conclusion

FloodRoute illustre comment les concepts de **graphes** et de **systèmes multi-agents** peuvent être appliqués à une problématique concrète de gestion de crise. Le projet démontre qu'une modélisation formelle rigoureuse peut servir de socle à une application interactive et utilisable, tout en confrontant l'équipe aux exigences réelles d'un développement logiciel structuré.

<br/>

<div align="center">

<img width="100%" src="https://capsule-render.vercel.app/api?type=waving&height=140&section=footer&color=0:00B7FF,50:006DFF,100:001B44"/>

**FloodRoute &nbsp;·&nbsp; ING1 GI4 &nbsp;·&nbsp; CY Tech &nbsp;·&nbsp; 2025–2026**

_Anticiper. Alerter. Protéger._

</div>
