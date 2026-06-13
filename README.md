<div align="center">

```
███████╗██╗      ██████╗  ██████╗ ██████╗ ██████╗  ██████╗ ██╗   ██╗████████╗███████╗
██╔════╝██║     ██╔═══██╗██╔═══██╗██╔══██╗██╔══██╗██╔═══██╗██║   ██║╚══██╔══╝██╔════╝
█████╗  ██║     ██║   ██║██║   ██║██║  ██║██████╔╝██║   ██║██║   ██║   ██║   █████╗  
██╔══╝  ██║     ██║   ██║██║   ██║██║  ██║██╔══██╗██║   ██║██║   ██║   ██║   ██╔══╝  
██║     ███████╗╚██████╔╝╚██████╔╝██████╔╝██║  ██║╚██████╔╝╚██████╔╝   ██║   ███████╗
╚═╝     ╚══════╝ ╚═════╝  ╚═════╝ ╚═════╝ ╚═╝  ╚═╝ ╚═════╝  ╚═════╝   ╚═╝   ╚══════╝
```

**Projet de fin d'année — ING1 GI4 · CY Tech · 2025–2026**  
Matière : Agents et Graphes &nbsp;·&nbsp; Tutrice : D. Zaouche &nbsp;·&nbsp; Jury : S. Hawari

<br/>

[![Java](https://img.shields.io/badge/Java-21-007396?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.java.com)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-0078D7?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjfx.io)
[![Maven](https://img.shields.io/badge/Maven-3.9-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)](https://maven.apache.org)
[![Licence](https://img.shields.io/badge/Licence-Académique-4A90E2?style=for-the-badge)](.)
[![Statut](https://img.shields.io/badge/Statut-Soutenu-22c55e?style=for-the-badge)](.)

</div>

<img width="100%" src="https://capsule-render.vercel.app/api?type=waving&height=200&color=0:001B44,35:003B8F,70:006DFF,100:00B7FF&text=FloodRoute&fontColor=FFFFFF&fontSize=80&fontAlignY=42&desc=Simulation%20d'inondation%20avec%20agents%20et%20graphes&descAlignY=62&descSize=20"/>

---

## 👥 Équipe projet

<div align="center">

| Martial Mouttalapane | Sanem Sayed | Hajar Achour | Bouchra Zamoum | Jenistar Makoudjou |
|:--------------------:|:-----------:|:------------:|:--------------:|:------------------:|

| Tutrice | Jury |
|:-------:|:----:|
| D. Zaouche | S. Hawari |

</div>

---

## 📌 Présentation

FloodRoute est une application de simulation d'inondation représentant une situation de crise dans une ville modélisée sous forme de **graphe**.

- Les **sommets** représentent des lieux importants (quartiers, refuges, hôpitaux).
- Les **arêtes** représentent les routes reliant ces lieux.
- Selon l'évolution de l'inondation, une route peut rester praticable, devenir inondée ou être bloquée.

L'application propose plusieurs interfaces selon le profil de l'utilisateur (citoyen, secours, administrateur) et prend en compte les personnes à mobilité réduite (PMR) pour proposer une évacuation adaptée.

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

## 🎯 Objectifs

- **Visualiser** une ville sous forme de graphe et suivre l'état des routes
- **Simuler** l'évolution d'une inondation en temps réel
- **Alerter** les utilisateurs selon le niveau de danger
- **Orienter** les citoyens vers des refuges via des itinéraires sécurisés
- **Coordonner** les secours et permettre à l'administrateur de superviser le système

---

## 👤 Profils utilisateurs

<div align="center">

| Profil | Description | Accès |
|--------|-------------|-------|
| 🧑‍💼 **Administrateur** | Lance la simulation, gère le graphe, supervise le système | Complet |
| 🚒 **Secours** | Visualise les zones critiques, suit les agents, coordonne les missions | Opérationnel |
| 🧍 **Citoyen** | Consulte la carte, reçoit les alertes, calcule son itinéraire | Lecture + navigation |
| ♿ **PMR** | Mêmes droits que le citoyen avec un itinéraire adapté | Navigation adaptée |

</div>

---

## ⚙️ Fonctionnalités

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

## 🔬 Modélisation agents & graphes

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
│   ├── Routes praticables  ✅
│   ├── Routes inondées     🌊
│   └── Routes bloquées     🚫
│
└── Agents
    ├── Citoyens
    ├── Personnes à mobilité réduite (PMR)
    ├── Secours
    └── Administrateurs
```

### Hiérarchie d'agents

```java
Agent (abstraite)
├── Citoyen      → se déplace, reçoit des alertes, cherche un refuge
├── PMRAgent     → contraintes de mobilité supplémentaires
├── RescueAgent  → intervient sur les zones critiques, prioritaire
└── AdminAgent   → contrôle la simulation, gère le graphe
```

### Algorithme d'évacuation

```java
// Calcul d'itinéraire sécurisé
EvacuationRouter.compute(graph, source, refuge, currentState);
// → retourne le chemin optimal parmi les routes praticables
```

---

## 🗂️ Structure du projet

```
Projet-logiciel-Ing-1/
│
├── data/
│   ├── users.json
│   └── zones.json
│
├── src/main/
│   ├── java/
│   │   ├── app/
│   │   ├── cli/
│   │   ├── controller/
│   │   │   ├── AdminPage/
│   │   │   ├── AuthPage/
│   │   │   ├── CitizenPage/
│   │   │   ├── RescuePage/
│   │   │   └── MapController.java
│   │   ├── model/
│   │   │   ├── agent/
│   │   │   ├── alert/
│   │   │   ├── algorithms/
│   │   │   ├── auth/
│   │   │   ├── enums/
│   │   │   ├── graph/
│   │   │   ├── observer/
│   │   │   ├── persistence/
│   │   │   ├── sensor/
│   │   │   ├── simulation/
│   │   │   ├── statistics/
│   │   │   ├── strategy/
│   │   │   ├── validation/
│   │   │   └── zone/
│   │   ├── service/
│   │   └── view/
│   │       ├── components/
│   │       ├── AdminAlertsView.java
│   │       ├── AdminDashboardView.java
│   │       ├── CitizenDashboardView.java
│   │       ├── LoginView.java
│   │       ├── MapView.java
│   │       ├── RescueDashboardView.java
│   │       ├── SimulationView.java
│   │       └── WelcomeView.java
│   └── resources/
│       ├── css/
│       ├── images/
│       ├── map/
│       └── lyon_routes.json
│
├── pom.xml
└── README.md
```

<div align="center">

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

## 🛠️ Technologies

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

## 🚀 Installation

### Prérequis

- **Java 21** — [Télécharger](https://adoptium.net/)
- **Maven 3.9** — [Télécharger](https://maven.apache.org/download.cgi)
- **JavaFX SDK 21** — [Télécharger](https://openjfx.io/)

### Cloner le dépôt

```bash
git clone https://github.com/<votre-organisation>/FloodRoute.git
cd FloodRoute
```

### Lancer l'application

```bash
# macOS
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH=$JAVA_HOME/bin:$PATH
mvn javafx:run

# Windows / Linux
mvn javafx:run
```

### Générer le JAR

```bash
mvn clean package
java -jar target/FloodRoute-1.0.jar
```

---

## 📝 Conclusion

FloodRoute illustre comment les concepts de **graphes** et de **systèmes multi-agents** peuvent être appliqués à une problématique concrète de gestion de crise. Le projet démontre qu'une modélisation formelle rigoureuse peut servir de socle à une application interactive et utilisable, tout en confrontant l'équipe aux exigences réelles d'un développement logiciel structuré.

<div align="center">

<br/>

<img width="100%" src="https://capsule-render.vercel.app/api?type=waving&height=140&section=footer&color=0:00B7FF,50:006DFF,100:001B44"/>

**FloodRoute · ING1 GI4 · CY Tech · 2025–2026**

_Anticiper. Alerter. Protéger._

</div>
