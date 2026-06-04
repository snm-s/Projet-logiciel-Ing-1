package view;

import java.io.File;
import java.io.FileReader;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import app.Main;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.web.WebView;

public class AdminDashboardView extends BorderPane {

    private final StackPane centralViewContainer;
    private ObservableList<AgentInscrit> listeInscritsGlobal;

    private VBox pageSupervision;
    private VBox pageInscrits;
    private VBox pageGraphe;
    private VBox pageMoteur;
    private WebView mapWebView;

    public AdminDashboardView() {
        this.setPrefSize(1280, 750);
        this.setStyle("-fx-background-color: #f4f7fc;");

        // 1. CHARGEMENT DEPUIS TON VRAI FICHIER USER.JSON VIA GSON
        chargerDonneesDepuisJSON();

        // ==========================================
        // 2. SIDEBAR DE NAVIGATION
        // ==========================================
        VBox sidebar = new VBox(5);
        sidebar.setPrefWidth(260);
        sidebar.setStyle("-fx-background-color: #0b1a30;");
        sidebar.setPadding(new Insets(20, 15, 20, 15));

        Label lblLogo = new Label("🛡️ PC DES SECOURS");
        lblLogo.setTextFill(Color.WHITE);
        lblLogo.setFont(Font.font("System", FontWeight.BOLD, 16));
        Label lblSub = new Label("Système de Crise Cartographique");
        lblSub.setTextFill(Color.web("#a0b2ce"));
        lblSub.setFont(Font.font("System", 11));
        VBox headerBox = new VBox(3, lblLogo, lblSub);
        headerBox.setPadding(new Insets(10, 5, 30, 5));
        sidebar.getChildren().add(headerBox);

        Button btnSupervision = createMenuButton("🗺️   Carte de Supervision Live", true);
        Button btnInscrits = createMenuButton("👥   Liste des Inscrits", false);
        Button btnGraphe = createMenuButton("🛠️   Modifications Graphe", false);
        Button btnMoteur = createMenuButton("⏱️   Moteur & Simulation", false);

        sidebar.getChildren().addAll(btnSupervision, btnInscrits, btnGraphe, btnMoteur);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS); // CORRIGÉ : C'est bien Vgrow ici !
        sidebar.getChildren().add(spacer);

        Button btnLogout = createMenuButton("🚪   Déconnexion", false);
        btnLogout.setStyle("-fx-background-color: transparent; -fx-text-fill: #e74c3c; -fx-alignment: center-left; -fx-cursor: hand;");
        sidebar.getChildren().addAll(new Separator(), btnLogout);

        this.setLeft(sidebar);

        // ==========================================
        // 3. ZONE CENTRALE
        // ==========================================
        centralViewContainer = new StackPane();
        this.setCenter(centralViewContainer);

        buildPageSupervision();
        buildPageInscrits();
        buildPageGraphe();
        buildPageMoteur();

        centralViewContainer.getChildren().add(pageSupervision);

        btnSupervision.setOnAction(e -> { showPage(pageSupervision); updateActiveTabs(btnSupervision, btnInscrits, btnGraphe, btnMoteur); rafraichirCarte(); });
        btnInscrits.setOnAction(e -> { showPage(pageInscrits); updateActiveTabs(btnInscrits, btnSupervision, btnGraphe, btnMoteur); });
        btnGraphe.setOnAction(e -> { showPage(pageGraphe); updateActiveTabs(btnGraphe, btnSupervision, btnInscrits, btnMoteur); });
        btnMoteur.setOnAction(e -> { showPage(pageMoteur); updateActiveTabs(btnMoteur, btnSupervision, btnInscrits, btnGraphe); });
        btnLogout.setOnAction(e -> {
            Main.currentUser = null;
            Main.showWelcomeView();
        });
    }

    /**
     * CHARGEMENT ROBUSTE : Utilise Gson pour mapper directement ton fichier user.json
     */
    private void chargerDonneesDepuisJSON() {
        listeInscritsGlobal = FXCollections.observableArrayList();
        String cheminFichier = "user.json";

        try {
            File fichier = new File(cheminFichier);
            if (!fichier.exists()) {
                System.out.println("⚠️ Fichier user.json introuvable. Création de données par défaut.");
                listeInscritsGlobal.add(new AgentInscrit("admin", 1, "Chef", "Admin", "admin@test.com", "CALME", null, new Position(48.85, 2.35)));
                return;
            }

            Gson gson = new Gson();
            FileReader reader = new FileReader(fichier);
            
            // On convertit le tableau JSON directement en Liste d'objets Java
            List<AgentInscrit> listeJson = gson.fromJson(reader, new TypeToken<List<AgentInscrit>>(){}.getType());
            reader.close();

            if (listeJson != null) {
                listeInscritsGlobal.addAll(listeJson);
                System.out.println("✅ " + listeInscritsGlobal.size() + " utilisateurs chargés avec succès depuis le JSON !");
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur critique lors de la lecture du JSON via GSON : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void buildPageSupervision() {
        pageSupervision = new VBox(15);
        pageSupervision.setPadding(new Insets(25));

        Label title = new Label("Supervision Globale - Carte Réelle (user.json)");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));
        title.setTextFill(Color.web("#0b1a30"));

        mapWebView = new WebView();
        VBox.setVgrow(mapWebView, Priority.ALWAYS);

        rafraichirCarte();

        pageSupervision.getChildren().addAll(title, mapWebView);
    }

    private void rafraichirCarte() {
        StringBuilder htmlContent = new StringBuilder();
        htmlContent.append("<html><head>")
                .append("<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>")
                .append("<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>")
                .append("<style>html, body, #map { height: 100%; margin: 0; padding: 0; }</style>")
                .append("</head><body><div id='map'></div><script>")
                .append("var map = L.map('map').setView([48.85, 2.35], 12);")
                .append("L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19 }).addTo(map);");

        // Points d'intérêts d'infrastructure fixes
        htmlContent.append("L.marker([48.8584, 2.3499]).addTo(map).bindPopup('<b>🏢 CASERNE DE POMPIERS CENTRALE</b>');");
        htmlContent.append("L.marker([48.8462, 2.3427]).addTo(map).bindPopup('<b>🏠 REFUGE DE CRISE PRINCIPAL</b>');");

        // Affichage dynamique des vrais agents chargés du fichier JSON
        for (AgentInscrit agent : listeInscritsGlobal) {
            if (agent.getPosition() == null) continue; // Sécurité si un agent n'a pas de coordonnées

            String color = "blue"; 
            if ("admin".equals(agent.getType())) color = "purple";
            if ("rescueAgent".equals(agent.getType())) color = "red"; 

            String dest = agent.getDestination() != null ? agent.getDestination() : "Aucune";

            // Nettoyage des chaînes pour éviter les crashs de chaînes de caractères en JavaScript
            String prenom = agent.getFirstName().replace("'", "\\'");
            String nom = agent.getLastName().replace("'", "\\'");

            htmlContent.append("L.circle([").append(agent.getPosition().getLat()).append(", ").append(agent.getPosition().getLng()).append("], {")
                    .append("color: '").append(color).append("',")
                    .append("fillColor: '").append(color).append("',")
                    .append("fillOpacity: 0.7, radius: 250 })")
                    .append(".addTo(map).bindPopup('")
                    .append("<b>Rôle:</b> ").append(agent.getType())
                    .append("<br><b>Nom:</b> ").append(prenom).append(" ").append(nom)
                    .append("<br><b>État:</b> ").append(agent.getState())
                    .append("<br><b>Destination:</b> ").append(dest)
                    .append("');");
        }

        htmlContent.append("</script></body></html>");
        mapWebView.getEngine().loadContent(htmlContent.toString());
    }

    private void buildPageInscrits() {
        pageInscrits = new VBox(15);
        pageInscrits.setPadding(new Insets(25));

        Label title = new Label("Base de Données — Registre Matricule (user.json)");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));

        TableView<AgentInscrit> table = new TableView<>();
        table.setItems(listeInscritsGlobal);

        TableColumn<AgentInscrit, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        
        TableColumn<AgentInscrit, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));

        TableColumn<AgentInscrit, String> colFirstName = new TableColumn<>("Prénom");
        colFirstName.setCellValueFactory(new PropertyValueFactory<>("firstName"));

        TableColumn<AgentInscrit, String> colLastName = new TableColumn<>("Nom");
        colLastName.setCellValueFactory(new PropertyValueFactory<>("lastName"));

        TableColumn<AgentInscrit, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        TableColumn<AgentInscrit, String> colState = new TableColumn<>("État");
        colState.setCellValueFactory(new PropertyValueFactory<>("state"));

        table.getColumns().addAll(colId, colType, colFirstName, colLastName, colEmail, colState);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        Button btnDelete = new Button("🗑️ Supprimer de la Base de Données");
        btnDelete.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
        btnDelete.setOnAction(e -> {
            AgentInscrit selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                listeInscritsGlobal.remove(selected);
            }
        });

        pageInscrits.getChildren().addAll(title, table, btnDelete);
    }

    private void buildPageGraphe() {
        pageGraphe = new VBox(20); pageGraphe.setPadding(new Insets(25));
        pageGraphe.getChildren().add(new Label("🛠️ Configuration du Graphe — Gestion des goulots d'accès (Pénalité de 2 cycles)"));
    }

    private void buildPageMoteur() {
        pageMoteur = new VBox(20); pageMoteur.setPadding(new Insets(25));
        pageMoteur.getChildren().add(new Label("⏱️ Moteur de Temps — Exécution de la simulation Pas à Pas"));
    }

    private Button createMenuButton(String text, boolean isActive) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(44);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(0, 15, 0, 15));
        setButtonStyle(btn, isActive);
        return btn;
    }

    private void setButtonStyle(Button btn, boolean isActive) {
        if (isActive) {
            btn.setStyle("-fx-background-color: #0b5cbf; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: bold;");
        } else {
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #a0b2ce; -fx-background-radius: 6; -fx-cursor: hand;");
        }
    }

    private void showPage(VBox page) {
        centralViewContainer.getChildren().clear();
        centralViewContainer.getChildren().add(page);
    }

    private void updateActiveTabs(Button active, Button... others) {
        setButtonStyle(active, true);
        for (Button b : others) setButtonStyle(b, false);
    }

    // ========================================================
    // CLASSES MODÈLES ADAPTÉES STRICTEMENT À TON FICHIER JSON
    // ========================================================
    public static class AgentInscrit {
        private String type;
        private int id;
        private String firstName;
        private String lastName;
        private String email;
        private String state;
        private String destination;
        private Position position; // Objet imbriqué comme dans ton fichier JSON

        public AgentInscrit(String type, int id, String firstName, String lastName, String email, String state, String destination, Position position) {
            this.type = type; this.id = id; this.firstName = firstName; this.lastName = lastName;
            this.email = email; this.state = state; this.destination = destination; this.position = position;
        }

        public String getType() { return type; }
        public int getId() { return id; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getEmail() { return email; }
        public String getState() { return state; }
        public String getDestination() { return destination; }
        public Position getPosition() { return position; }
    }

    public static class Position {
        private double lat;
        private double lng;

        public Position(double lat, double lng) {
            this.lat = lat;
            this.lng = lng;
        }

        public double getLat() { return lat; }
        public double getLng() { return lng; }
    }
}