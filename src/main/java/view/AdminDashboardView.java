package view;

import app.Main;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.web.WebView;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        // 1. CHARGEMENT DEPUIS TON VRAI FICHIER USER.JSON
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

        Button btnSupervision = createMenuButton("🗺️  Carte de Supervision Live", true);
        Button btnInscrits = createMenuButton("👥  Liste des Inscrits", false);
        Button btnGraphe = createMenuButton("🛠️  Modifications Graphe", false);
        Button btnMoteur = createMenuButton("⏱️  Moteur & Simulation", false);

        sidebar.getChildren().addAll(btnSupervision, btnInscrits, btnGraphe, btnMoteur);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().add(spacer);

        Button btnLogout = createMenuButton("🚪  Déconnexion", false);
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
     * PARSER MAISON : Lit directement ton fichier "user.json" sans bibliothèque externe bloquante
     */
    private void chargerDonneesDepuisJSON() {
        listeInscritsGlobal = FXCollections.observableArrayList();
        String cheminFichier = "user.json"; // Place ton fichier user.json à la racine de ton projet

        try {
            File fichier = new File(cheminFichier);
            if (!fichier.exists()) {
                System.out.println("⚠️ Fichier user.json introuvable à la racine. Création d'exemples.");
                // Fallback si le fichier n'est pas encore là
                listeInscritsGlobal.add(new AgentInscrit("admin", 1, "Chef", "Admin", "admin@test.com", 48.85, 2.35, "CALME", "Aucune"));
                return;
            }

            String content = new String(Files.readAllBytes(Paths.get(cheminFichier)));

            // Regex pour découper les blocs d'objets { ... } du JSON
            Pattern objectPattern = Pattern.compile("\\{[^\\}]+\\}");
            Matcher matcher = objectPattern.matcher(content);

            while (matcher.find()) {
                String bloc = matcher.group();

                String type = extractJsonValue(bloc, "type");
                int id = Integer.parseInt(extractJsonValue(bloc, "id"));
                String firstName = extractJsonValue(bloc, "firstName");
                String lastName = extractJsonValue(bloc, "lastName");
                String email = extractJsonValue(bloc, "email");
                String state = extractJsonValue(bloc, "state");
                String destination = extractJsonValue(bloc, "destination");
                if (destination == null || destination.equals("null")) destination = "Aucune";

                // Extraction des coordonnées imbriquées dans "position"
                double lat = 48.85; // valeurs par défaut
                double lng = 2.35;
                Pattern latPattern = Pattern.compile("\"lat\"\\s*:\\s*([0-9.]+)");
                Pattern lngPattern = Pattern.compile("\"lng\"\\s*:\\s*([0-9.]+)");
                Matcher mLat = latPattern.matcher(bloc);
                Matcher mLng = lngPattern.matcher(bloc);
                if (mLat.find()) lat = Double.parseDouble(mLat.group(1));
                if (mLng.find()) lng = Double.parseDouble(mLng.group(1));

                listeInscritsGlobal.add(new AgentInscrit(type, id, firstName, lastName, email, lat, lng, state, destination));
            }

        } catch (Exception e) {
            System.err.println("Erreur lors de la lecture du JSON : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String extractJsonValue(String bloc, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"?([^\",\\}]+)\"?");
        Matcher matcher = pattern.matcher(bloc);
        if (matcher.find()) {
            return matcher.group(1).replace("\"", "").trim();
        }
        return "null";
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

        // Affichage de tes vrais agents sur la carte
        for (AgentInscrit agent : listeInscritsGlobal) {
            String color = "blue"; 
            if (agent.getType().equals("admin")) color = "purple";
            if (agent.getType().equals("rescueAgent")) color = "red"; // Rouge pour les secours/pompiers

            htmlContent.append("L.circle([").append(agent.getLat()).append(", ").append(agent.getLng()).append("], {")
                    .append("color: '").append(color).append("',")
                    .append("fillColor: '").append(color).append("',")
                    .append("fillOpacity: 0.7, radius: 150 })")
                    .append(".addTo(map).bindPopup('")
                    .append("<b>Rôle:</b> ").append(agent.getType())
                    .append("<br><b>Nom:</b> ").append(agent.getFirstName()).append(" ").append(agent.getLastName())
                    .append("<br><b>État:</b> ").append(agent.getState())
                    .append("<br><b>Destination:</b> ").append(agent.getDestination())
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

    // ==========================================
    // CLASSE MODÈLE INTERNE ADAPTÉE À TON JSON
    // ==========================================
    public static class AgentInscrit {
        private final String type, firstName, lastName, email, state, destination;
        private final int id;
        private final double lat, lng;

        public AgentInscrit(String type, int id, String firstName, String lastName, String email, double lat, double lng, String state, String destination) {
            this.type = type; this.id = id; this.firstName = firstName; this.lastName = lastName; this.email = email;
            this.lat = lat; this.lng = lng; this.state = state; this.destination = destination;
        }

        public String getType() { return type; }
        public int getId() { return id; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getEmail() { return email; }
        public double getLat() { return lat; }
        public double getLng() { return lng; }
        public String getState() { return state; }
        public String getDestination() { return destination; }
    }
}