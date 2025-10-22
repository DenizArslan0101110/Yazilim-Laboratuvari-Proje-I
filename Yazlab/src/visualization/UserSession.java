package visualization;

public class UserSession
{
    private static String currentUserEmail = null;
    private static boolean isAdmin = false;
    private static long loginTime = 0;

    private UserSession() {}

    public static void setCurrentUser(String email, boolean admin)
    {
        currentUserEmail = email;
        isAdmin = admin;
        loginTime = System.currentTimeMillis();

        System.out.println("Oturum açıldı: " + email + " (Admin: " + admin + ")");
    }

    public static String getCurrentUserEmail()
    {
        return currentUserEmail;
    }

    public static boolean isAdmin()
    {
        return isAdmin;
    }

    public static boolean isLoggedIn()
    {
        return currentUserEmail != null;
    }

    public static long getSessionDuration()
    {
        if (loginTime == 0) return 0;
        return System.currentTimeMillis() - loginTime;
    }

    public static void clearSession()
    {
        System.out.println("Oturum kapatıldı: " + currentUserEmail);
        currentUserEmail = null;
        isAdmin = false;
        loginTime = 0;
    }

    public static void printSessionInfo()
    {
        System.out.println("=== OTURUM BİLGİLERİ ===");
        System.out.println("E-posta: " + currentUserEmail);
        System.out.println("Admin: " + isAdmin);
        System.out.println("Giriş Zamanı: " + new java.util.Date(loginTime));
        System.out.println("Oturum Süresi: " + (getSessionDuration() / 1000) + " saniye");
        System.out.println("========================");
    }
}