package controller.AuthPage;

import java.util.UUID;
 
import app.Main;
import javafx.stage.Stage;
import model.agent.Agent;
import model.agent.RescueTeam;
import model.auth.UserService;
import view.LoginView; 

public class LoginController {

    private final LoginView view;
    private final Stage stage;

    public LoginController(LoginView view, Stage stage) {
        this.view = view;
        this.stage = stage;
        initActions();
    }

    private void initActions() {
        view.getBackButton().setOnAction(e -> Main.showWelcomeView());
        view.getRegisterLink().setOnAction(e -> Main.showRegisterView());

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
                
                // 🔥 SAUVEGARDE DE LA SESSION
                Main.currentUser = authenticatedUser; 
                
                String role = "citizen"; 
                String className = authenticatedUser.getClass().getSimpleName().toLowerCase();
                if (className.contains("admin")) {
                    role = "admin";
                } else if (className.contains("rescue") || authenticatedUser instanceof RescueTeam) {
                    role = "rescue";
                } else {
                    role = "citizen";
                }

                Main.showDashboardView(role); 
                
            } else {
                view.displayErrorMessage("Email ou mot de passe incorrect.");
            }
        });

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
        // 🔥 On passe directement 'password' en clair à ton UserService
        return UserService.authenticate(email, password);
    }
}