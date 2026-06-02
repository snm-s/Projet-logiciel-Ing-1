package view;

import javafx.scene.Parent;
import javafx.scene.layout.VBox;
import javafx.scene.control.*;

public class ForgotPasswordView {
    private VBox root = new VBox(10);
    private TextField codeInput = new TextField();
    private PasswordField newPasswordField = new PasswordField();
    private Button confirmButton = new Button("Réinitialiser");

    public ForgotPasswordView() {
        root.getChildren().addAll(
            new Label("Entrez le code reçu par mail :"), codeInput,
            new Label("Nouveau mot de passe :"), newPasswordField,
            confirmButton
        );
    }

    public Parent getRoot() { return root; }
    public String getCodeInput() { return codeInput.getText(); }
    public String getNewPasswordInput() { return newPasswordField.getText(); }
    public Button getConfirmButton() { return confirmButton; }
}