package controller;

import javafx.stage.Stage;
import view.RegisterView;
import app.Main;
import model.auth.User;
import model.auth.UserService;

import java.time.LocalDate;

public class RegisterController {

    private final RegisterView view;
    private final Stage stage;

    public RegisterController(RegisterView view, Stage stage) {
        this.view = view;
        this.stage = stage;
        
        this.view.setController(this);
        initActions();
    }

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
            String houseType, int floor, double lat, double lng,
            String role, int householdSize, boolean hasPets, 
            String medicalNeeds, String emergencyContact
    ) {
        // 1. Sécurité : Vérifier si l'utilisateur existe déjà
        if (UserService.findByEmail(email) != null) {
            System.out.println("[RegisterController] Erreur : Cet email est déjà utilisé !");
            return false;
        }

        // 2. Création de l'objet User via les setters privés
        User newUser = new User();
        newUser.setFirstName(firstName);
        newUser.setLastName(lastName);
        newUser.setBirthDate(birthDate);
        newUser.setEmail(email);
        newUser.setPhone(phone);
        
        // Note : Idéalement à hacher plus tard (ex: BCrypt), stocké brut pour le moment selon ton besoin
        newUser.setPasswordHash(password); 

        newUser.setAddress(address);
        newUser.setCity(city);
        newUser.setCountry(country);
        
        newUser.setHouseType(houseType);
        newUser.setFloor(floor);
        
        newUser.setGpsLat(lat);
        newUser.setGpsLng(lng);
        
        newUser.setRole(role);
        newUser.setEmergencyContact(emergencyContact);
        
        // Attributs spécifiques au Citizen
        newUser.setHouseholdSize(householdSize);
        newUser.setHasPets(hasPets);
        newUser.setMedicalNeeds(medicalNeeds);
        
        // Statut de mobilité par défaut basé sur l'âge ou les besoins médicaux (ajustable)
        newUser.setMobilityStatus("normal"); 

        // 3. Écriture physique dans le fichier JSON
        try {
            UserService.addUser(newUser);
            System.out.println("[RegisterController] Succès : Utilisateur enregistré dans le JSON.");
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

    public boolean validatePasswordLength(String password) {
        return password != null && password.length() >= 8;
    }

    public boolean validatePasswordUpper(String password) {
        return password != null && password.chars().anyMatch(Character::isUpperCase);
    }

    public boolean validatePasswordDigit(String password) {
        return password != null && password.chars().anyMatch(Character::isDigit);
    }

    public boolean passwordsMatch(String password, String confirmPassword) {
        if (password == null || password.isEmpty()) return false;
        return password.equals(confirmPassword);
    }

    public boolean canRegister(String password, String confirmPassword) {
        return validatePasswordLength(password)
                && validatePasswordUpper(password)
                && validatePasswordDigit(password)
                && passwordsMatch(password, confirmPassword);
    }
}