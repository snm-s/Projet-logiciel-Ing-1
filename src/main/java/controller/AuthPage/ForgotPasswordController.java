package controller.AuthPage;

import app.Main;
import javafx.stage.Stage;
import model.auth.PasswordHasher;
import model.auth.UserService;
import view.ForgotPasswordView;

/**
 * ForgotPasswordController
 *
 * This controller manages the password reset workflow.
 *
 * Responsibilities:
 * - Handles navigation back to the login page.
 * - Retrieves the verification code entered by the user.
 * - Validates the strength of the new password.
 * - Verifies the reset token associated with the user's email.
 * - Hashes the new password before storing it.
 * - Updates the user's credentials through UserService.
 *
 * Security design choices:
 * - Passwords are never stored in plain text.
 * - Verification codes must match the generated token.
 * - Password strength requirements are enforced before reset.
 */
public class ForgotPasswordController {

    /** View associated with the password reset screen. */
    private final ForgotPasswordView view;

    /** Current JavaFX stage. */
    private final Stage stage;

    /** Email address of the account requesting a password reset. */
    private final String email;

    /**
     * Constructor.
     *
     * @param view  Forgot password view
     * @param stage Current application stage
     * @param email Email address of the user resetting the password
     */
    public ForgotPasswordController(ForgotPasswordView view, Stage stage, String email) {
        this.view = view;
        this.stage = stage;
        this.email = email;

        initActions();
    }

    /**
     * Initializes all button actions associated with the view.
     *
     * Actions:
     * - Back button: returns to the login screen.
     * - Confirm button: starts the password reset process.
     */
    private void initActions() {

        // Navigate back to the login page.
        view.getBackButton().setOnAction(e -> {
            System.out.println("[MVC] Back button clicked (Forgot Password)");
            Main.showLoginView();
        });

        // Trigger password reset.
        view.getConfirmButton().setOnAction(e -> handlePasswordReset());
    }

    /**
     * Handles the complete password reset process.
     *
     * Workflow:
     * 1. Retrieve user inputs.
     * 2. Validate password strength.
     * 3. Verify the reset token.
     * 4. Hash the new password.
     * 5. Update the password in the user service.
     * 6. Redirect the user back to the login page.
     *
     * If any validation fails, an error message is displayed.
     */
    private void handlePasswordReset() {

        System.out.println("[DEBUG] Reset password button clicked.");

        // Retrieve user inputs from the view.
        String enteredCode = view.getCodeInput();
        String newPassword = view.getNewPasswordInput();

        System.out.println("[DEBUG] Verification code entered: " + enteredCode);
        System.out.println("[DEBUG] Password entered: " + newPassword);

        /*
         * Password validation occurs before token verification.
         * This prevents unnecessary verification requests when
         * the password already violates security constraints.
         */
        if (!validatePasswordStrength(newPassword)) {

            System.out.println("[DEBUG] Password validation failed.");

            view.displayErrorMessage(
                    "Invalid password: minimum 8 characters, "
                    + "at least 1 uppercase letter and 1 digit."
            );

            return;
        }

        System.out.println("[DEBUG] Verifying reset token through UserService...");

        /*
         * The verification token confirms that the user has access
         * to the email address associated with the account.
         */
        if (UserService.verifyToken(email, enteredCode)) {

            System.out.println("[DEBUG] Token verified successfully.");

            /*
             * Passwords are hashed before persistence.
             * Storing plain-text passwords is avoided for security reasons.
             */
            String hashedPassword = PasswordHasher.hash(newPassword);

            UserService.resetPassword(email, hashedPassword);

            /*
             * Once the password has been updated successfully,
             * redirect the user to the login screen.
             */
            Main.showLoginView();

        } else {

            System.out.println("[DEBUG] Invalid verification token.");

            view.displayErrorMessage(
                    "Incorrect verification code."
            );
        }
    }

    /**
     * Validates password strength according to the application's policy.
     *
     * Current requirements:
     * - Password must not be null.
     * - Minimum length: 8 characters.
     * - At least one uppercase letter.
     * - At least one numeric digit.
     *
     * These rules provide a basic level of protection against
     * weak passwords while remaining user-friendly.
     *
     * @param password Password entered by the user.
     * @return true if the password satisfies all requirements.
     */
    private boolean validatePasswordStrength(String password) {

        return password != null
                && password.length() >= 8
                && password.chars().anyMatch(Character::isUpperCase)
                && password.chars().anyMatch(Character::isDigit);
    }
}