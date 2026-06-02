package controller;

import model.agent.Agent;
import model.auth.PasswordHasher;
import model.auth.UserService;
import javafx.stage.Stage;
import view.LoginView;
import java.util.UUID;
import app.Main; 

public class LoginController {

    private final LoginView view;
    private final Stage stage;

    public LoginController(LoginView view, Stage stage) {
        this.view = view;
        this.stage = stage;
        initActions();
    }

    private void initActions() {
        // Action du bouton retour
        view.getBackButton().setOnAction(e -> Main.showWelcomeView());

        // Action du lien "Créer un compte"
        view.getRegisterLink().setOnAction(e -> Main.showRegisterView());

        // Action du bouton Connexion
        view.getLoginButton().setOnAction(e -> {
            String email = view.getEmailInput();
            String password = view.getPasswordInput();

            if (email == null || email.trim().isEmpty() || password == null || password.isEmpty()) {
                view.displayErrorMessage("Veuillez remplir tous les champs.");
                return;
            }

            Agent authenticatedUser = login(email, password);
            if (authenticatedUser != null) {
                view.displayErrorMessage("");
                Main.showWelcomeView(); 
            } else {
                view.displayErrorMessage("Email ou mot de passe incorrect.");
            }
        });

        // Action du lien Mot de passe oublié
        view.getForgotPasswordLink().setOnAction(e -> {
            String email = view.getEmailInput();
            
            if (email == null || email.trim().isEmpty()) {
                view.displayErrorMessage("Veuillez saisir votre email.");
                return;
            }
            
            if (UserService.findByEmail(email) == null) {
                view.displayErrorMessage("Aucun compte associé à cet email.");
                return;
            }
            
            String code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            UserService.setResetToken(email, code);
            
            new Thread(() -> {
                try {
                    model.auth.EmailResetService.sendResetCode(email, code);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();
            
            Main.showForgotPasswordView(email); 
        });
    }

    public Agent login(String email, String password) {
        // Le hashage est effectué ici avant l'authentification
        return UserService.authenticate(email, PasswordHasher.hash(password));
    }
}