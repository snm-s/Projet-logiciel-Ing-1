package controller;

import model.agent.Agent;
import model.auth.PasswordHasher;
import model.auth.UserService;
import javafx.stage.Stage;
import view.LoginView;
import app.Main; 

public class LoginController {

    private final LoginView view;
    private final Stage stage;

    public LoginController(LoginView view, Stage stage) {
        this.view = view;
        this.stage = stage;

        // Liaison des écouteurs sur la vue
        initActions();
    }

    private void initActions() {
        // Action du bouton retour -> Accueil
        view.getBackButton().setOnAction(e -> {
            System.out.println("[MVC] Clic retour : Redirection via Main.showWelcomeView()");
            Main.showWelcomeView();
        });

        // Action du lien "Créer un compte" -> Page Inscription
        view.getRegisterLink().setOnAction(e -> {
            System.out.println("[MVC] Clic inscription : Redirection via Main.showRegisterView()");
            Main.showRegisterView();
        });

        // ==========================================
        // CONNEXION À L'ACTION DE CONNEXION (AJOUT)
        // ==========================================
        view.getLoginButton().setOnAction(e -> {
            String email = view.getEmailInput();
            String password = view.getPasswordInput();

            // Petite validation de surface pour éviter de lire le JSON inutilement
            if (email == null || email.trim().isEmpty() || password == null || password.isEmpty()) {
                view.displayErrorMessage("Veuillez remplir tous les champs.");
                return;
            }

            // Tentative d'authentification
            Agent authenticatedUser = login(email, password);

            if (authenticatedUser != null) {
                System.out.println("[LoginController] Connexion réussie pour : " + authenticatedUser.getFirstName());
                view.displayErrorMessage(""); // On efface les anciennes erreurs éventuelles
                
                // Redirection vers ton interface principale / tableau de bord (Dashboard)
                // Remplace cette ligne par l'appel de ta vue principale (ex: Main.showDashboardView();)
                Main.showWelcomeView(); 
            } else {
                System.out.println("[LoginController] Échec de la connexion (Email inconnu ou mot de passe faux)");
                view.displayErrorMessage("Email ou mot de passe incorrect.");
            }
        });
    }

    /**
     * Authentifie un utilisateur en fonction de son email et de son mot de passe.
     * @param email L'email saisi par l'utilisateur
     * @param password Le mot de passe en clair saisi par l'utilisateur
     * @return L'objet User si l'authentification réussit, null sinon.
     */
    public Agent login(String email, String password) {
        // 1. Recherche de l'utilisateur par son email via le service
        Agent user = UserService.findByEmail(email);

        // Si aucun utilisateur n'est trouvé, on s'arrête là
        if (user == null) {
            return null;
        }

        // 2. Hachage du mot de passe saisi pour comparaison
        String hashedInput = PasswordHasher.hash(password);

        // 3. Vérification de la correspondance des empreintes de mots de passe
        try {
            if (hashedInput != null && hashedInput.equals(user.getPasswordHash())) {
                return user; // Authentification réussie
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null; // Mot de passe incorrect
    }
}