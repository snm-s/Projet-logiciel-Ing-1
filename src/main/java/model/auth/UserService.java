package model.auth;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import model.auth.User;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;

public class UserService {

    private static final Path FILE = Paths.get("src/main/java/ressources/data/users.json");
    private static final Gson gson = new Gson();

    private static Type type = new TypeToken<List<User>>() {}.getType();

    public static List<User> loadUsers() {
        try {
            if (!Files.exists(FILE)) {
                return new ArrayList<>();
            }

            Reader reader = Files.newBufferedReader(FILE);
            List<User> users = gson.fromJson(reader, type);
            reader.close();

            return users != null ? users : new ArrayList<>();

        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static void saveUsers(List<User> users) {
        try {
            Files.createDirectories(FILE.getParent());

            Writer writer = Files.newBufferedWriter(FILE);
            gson.toJson(users, writer);
            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addUser(User user) {
        List<User> users = loadUsers();
        users.add(user);
        saveUsers(users);
    }

    public static User findByEmail(String email) {
        return loadUsers()
                .stream()
                // CORRECTION ICI : u.email remplacé par u.getEmail() pour respecter l'accès privé
                .filter(u -> u.getEmail() != null && u.getEmail().equalsIgnoreCase(email))
                .findFirst()
                .orElse(null);
    }
}