package model.auth;

import java.security.MessageDigest;

public class PasswordHasher {

    /**
     * Performs hash.
     * @param password the password.
     * @return the String.
     */
    public static String hash(String password) {

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            byte[] bytes = md.digest(password.getBytes());

            StringBuilder sb = new StringBuilder();

            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Performs verify.
     * @param password the password.
     * @param hash the hash.
     * @return the boolean result.
     */
    public static boolean verify(String password, String hash) {
        return hash(password).equals(hash);
    }
}
