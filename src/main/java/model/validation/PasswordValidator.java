package model.validation;

public class PasswordValidator {

    /**
     * Returns whether min length.
     * @param pwd the pwd.
     * @return the boolean result.
     */
    public static boolean hasMinLength(String pwd) {
        return pwd != null && pwd.length() >= 8;
    }

    /**
     * Returns whether uppercase.
     * @param pwd the pwd.
     * @return the boolean result.
     */
    public static boolean hasUppercase(String pwd) {
        return pwd != null && pwd.matches(".*[A-Z].*");
    }

    /**
     * Returns whether digit.
     * @param pwd the pwd.
     * @return the boolean result.
     */
    public static boolean hasDigit(String pwd) {
        return pwd != null && pwd.matches(".*[0-9].*");
    }

    /**
     * Returns whether valid.
     * @param pwd the pwd.
     * @return the boolean result.
     */
    public static boolean isValid(String pwd) {
        return hasMinLength(pwd)
                && hasUppercase(pwd)
                && hasDigit(pwd);
    }
}
