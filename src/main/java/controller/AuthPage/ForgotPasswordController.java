package controller.AuthPage;

import app.Main;
import model.auth.*;
import view.ForgotPasswordView;
import javafx.stage.Stage;

public class ForgotPasswordController {

    private final ForgotPasswordView view;
    private final Stage stage;
    private final String email;

    public ForgotPasswordController(ForgotPasswordView view, Stage stage, String email) {
        this.view = view;
        this.stage = stage;
        this.email = email;

        initActions();
    }

    private void initActions() {
        // Action du bouton retour
        view.getBackButton().setOnAction(e -> {
            System.out.println("[MVC] Clic retour (ForgotPassword)");
            Main.showLoginView();
        });

        // Action du bouton réinitialiser
        view.getConfirmButton().setOnAction(e -> handlePasswordReset());
    }

    private void handlePasswordReset() {
        System.out.println("[DEBUG] Bouton réinitialiser cliqué !");
        
        String codeSaisi = view.getCodeInput();
        String nouveauMdp = view.getNewPasswordInput();
        
        System.out.println("[DEBUG] Code saisi : " + codeSaisi);
        System.out.println("[DEBUG] Mdp saisi : " + nouveauMdp);

        if (!validatePasswordStrength(nouveauMdp)) {
            System.out.println("[DEBUG] Validation MDP échouée.");
            view.displayErrorMessage("Mot de passe invalide : min 8 caractères, 1 majuscule et 1 chiffre.");
            return;
        }

        System.out.println("[DEBUG] Tentative de vérification token via UserService...");
        if (UserService.verifyToken(email, codeSaisi)) {
            System.out.println("[DEBUG] Token valide ! Mise à jour en cours...");
            String hashedMdp = PasswordHasher.hash(nouveauMdp);
            UserService.resetPassword(email, hashedMdp);
            Main.showLoginView();
        } else {
            System.out.println("[DEBUG] Token invalide.");
            view.displayErrorMessage("Code de vérification incorrect.");
        }
    }


    private boolean validatePasswordStrength(String password) {
        return password != null 
            && password.length() >= 8 
            && password.chars().anyMatch(Character::isUpperCase) 
            && password.chars().anyMatch(Character::isDigit);
    }
}