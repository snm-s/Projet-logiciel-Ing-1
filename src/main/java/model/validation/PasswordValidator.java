package model.validation;

public class PasswordValidator {

    public static boolean hasMinLength(String pwd) {
        return pwd != null && pwd.length() >= 8;
    }

    public static boolean hasUppercase(String pwd) {
        return pwd != null && pwd.matches(".*[A-Z].*");
    }

    public static boolean hasDigit(String pwd) {
        return pwd != null && pwd.matches(".*[0-9].*");
    }

    public static boolean isValid(String pwd) {
        return hasMinLength(pwd)
                && hasUppercase(pwd)
                && hasDigit(pwd);
    }
}