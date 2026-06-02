package controller;

import model.auth.*;
import view.ForgotPasswordView;
import javafx.stage.Stage;

public class ForgotPasswordController {
    public ForgotPasswordController(ForgotPasswordView view, Stage stage, String email) {
        view.getConfirmButton().setOnAction(e -> {
            String codeSaisi = view.getCodeInput();
            String nouveauMdp = view.getNewPasswordInput();

            // 1. Vérifier le code
            if (UserService.verifyToken(email, codeSaisi)) {
                // 2. Mettre à jour avec le nouveau mot de passe haché
                String hashedMdp = PasswordHasher.hash(nouveauMdp);
                UserService.resetPassword(email, hashedMdp);
                System.out.println("Mot de passe mis à jour avec succès !");
            } else {
                System.out.println("Code incorrect.");
            }
        });
    }
}