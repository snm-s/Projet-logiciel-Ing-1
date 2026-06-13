package controller.AuthPage;

import java.time.LocalDate;

import app.Main;
import javafx.stage.Stage;
import model.agent.Agent;
import model.agent.Citizen;
import model.agent.HouseType;
import model.agent.PMRAgent;
import model.agent.RescueAgent;
import model.auth.UserService;
import model.graph.Node;
import view.RegisterView;

public class RegisterController {

    private final RegisterView view;
    private final Stage stage;

    /**
     * Constructs a new RegisterController.
     * @param view the view.
     * @param stage the stage.
     */
    public RegisterController(RegisterView view, Stage stage) {
        this.view = view;
        this.stage = stage;
        
        this.view.setController(this);
        initActions();
    }

    /**
     * Initializes actions.
     */
    private void initActions() {
        view.getBackButton().setOnAction(e -> {
            System.out.println("[MVC] Clic retour (Register)");
            Main.showWelcomeView();
        });
    }

    /**
     * Crée et enregistre un nouvel utilisateur dans le fichier JSON.
     * Vérifie d'abord si l'email est unique.
     */
    public boolean handleUserRegistration(
            String firstName, String lastName, LocalDate birthDate,
            String email, String phone, String password,
            String address, String city, String country,
            String houseTypeString, int floor, double lat, double lng,
            String role, int householdSize, boolean hasPets, 
            String medicalNeeds, String emergencyContact,
            boolean isPmr
    ) {

        // 1. Sécurité : Vérifier si l'utilisateur existe déjà
        if (UserService.findByEmail(email) != null) {
            System.out.println("[RegisterController] Erreur : Cet email est déjà utilisé !");
            return false;
        }

        // 2. Création de l'objet via polymorphisme
        Agent newAgent;

        String roleKey = role.equalsIgnoreCase("Citoyen") ? "citizen" : "rescue";
    
    
        


        if ("rescue".equalsIgnoreCase(roleKey)) {
            RescueAgent rescue = new RescueAgent(0, firstName, lastName, null);
            rescue.setState(model.enums.RescueState.DISPONIBLE);
            newAgent = rescue;
        } 
        else if ("citizen".equalsIgnoreCase(roleKey)) {
            if (isPmr) {
                PMRAgent pmr = new PMRAgent(0, firstName, lastName, null);
                pmr.setState(model.enums.CitizenState.CALM);
                newAgent = pmr;
            } else {
                Citizen citizen = new Citizen(0, firstName, lastName, null);
                citizen.calculateMobilityStatus(birthDate);
                citizen.setState(model.enums.CitizenState.CALM);
                newAgent = citizen;
            }
        } else {
            System.out.println("[RegisterController] Erreur : Rôle inconnu.");
            return false;
        }

        // 3. Remplissage des données communes à tous les agents
        newAgent.setBirthDate(birthDate);
        newAgent.setEmail(email);
        newAgent.setPhone(phone);
        newAgent.setPasswordHash(model.auth.PasswordHasher.hash(password));
        newAgent.setAddress(address);
        newAgent.setCity(city);
        newAgent.setCountry(country);
        newAgent.setPosition(new Node(lat, lng));


        // 4. Remplissage des données spécifiques aux citoyens
        if (newAgent instanceof Citizen) {
            Citizen cit = (Citizen) newAgent;
            String houseTypeKey = houseTypeString.equalsIgnoreCase("Appartement") ? "APARTMENT" : "HOUSE";

            try {
                // Conversion protégée
                HouseType houseType = HouseType.valueOf(houseTypeKey.toUpperCase());
                cit.setHouseType(houseType);
            } catch (Exception e) {
                System.err.println("[RegisterController] Type de maison invalide : " + houseTypeKey);
                return false; 
            }
            
            cit.setFloor(floor);
            cit.setHouseholdSize(householdSize);
            cit.setHasPets(hasPets);
            cit.setMedicalNeeds(medicalNeeds);
            cit.setEmergencyContact(emergencyContact);
        }

        // 5. Écriture physique dans le fichier JSON
        try {
            UserService.addAgent(newAgent);
            System.out.println("[RegisterController] Succès : Agent " + role + " enregistré.");
            return true;
        } catch (Exception e) {
            System.out.println("[RegisterController] Erreur critique lors de l'écriture JSON.");
            e.printStackTrace();
            return false;
        }
    }

    // ==========================================
    // LOGIQUE DE VALIDATION DES MOTS DE PASSE
    // ==========================================

    /**
     * Validates password length.
     * @param password the password.
     * @return the boolean result.
     */
    public boolean validatePasswordLength(String password) {
        return password != null && password.length() >= 8;
    }

    /**
     * Validates password upper.
     * @param password the password.
     * @return the boolean result.
     */
    public boolean validatePasswordUpper(String password) {
        return password != null && password.chars().anyMatch(Character::isUpperCase);
    }

    /**
     * Validates password lower.
     * @param password the password.
     * @return the boolean result.
     */
    public boolean validatePasswordLower(String password) {
        return password != null && password.chars().anyMatch(Character::isLowerCase);
    }

    /**
     * Validates password digit.
     * @param password the password.
     * @return the boolean result.
     */
    public boolean validatePasswordDigit(String password) {
        return password != null && password.chars().anyMatch(Character::isDigit);
    }

    /**
     * Performs match.
     * @param password the password.
     * @param confirmPassword the confirmPassword.
     * @return the boolean result.
     */
    public boolean passwordsMatch(String password, String confirmPassword) {
        if (password == null || password.isEmpty()) return false;
        return password.equals(confirmPassword);
    }

    /**
     * Performs register.
     * @param password the password.
     * @param confirmPassword the confirmPassword.
     * @return the boolean result.
     */
    public boolean canRegister(String password, String confirmPassword) {
        return validatePasswordLength(password)
                && validatePasswordUpper(password)
                && validatePasswordLower(password)
                && validatePasswordDigit(password)
                && passwordsMatch(password, confirmPassword);
    }
    
}
