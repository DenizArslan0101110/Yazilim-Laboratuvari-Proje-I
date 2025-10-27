package visualization;

import java.sql.*;

public class Auth
{
    private static final String DB_URL = "jdbc:mysql://localhost:3306/muhendislik_fakulte?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "yazlab1";

    /**
     * Kullanıcı bilgilerini kontrol eder
     * @return 3: Admin, 2: Koordinatör, 1: Şifre yanlış, 0: E-posta bulunamadı, -1: Veritabanı hatası
     */
    public static int CheckAccInfo(String email, String password)
    {
        if (email == null || email.trim().isEmpty() || password == null || password.isEmpty()) {
            return 0;
        }

        String sql = "SELECT sifre, is_admin FROM bolum_koordinator WHERE eposta = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql))
        {
            ps.setString(1, email.trim());

            try (ResultSet rs = ps.executeQuery())
            {
                if (rs.next())
                {
                    String dbPassword = rs.getString("sifre");
                    boolean isAdmin = rs.getBoolean("is_admin");

                    if (dbPassword != null && dbPassword.equals(password))
                    {
                        if (isAdmin) {
                            System.out.println("Admin girişi başarılı: " + email);
                            return 3;
                        }
                        System.out.println("Koordinatör girişi başarılı: " + email);
                        return 2;
                    }
                    else {
                        System.out.println("Şifre yanlış: " + email);
                        return 1;
                    }
                }
                else {
                    System.out.println("E-posta bulunamadı: " + email);
                    return 0;
                }
            }
        }
        catch (SQLException e)
        {
            System.err.println("Veritabanı hatası: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Yeni koordinatör ekler (Sadece admin)
     * @return 1: Başarılı, -1: Hata, -2: E-posta zaten kayıtlı
     */
    public static int addKoordinator(String email, String sifre, boolean isAdmin)
    {
        if(isEmailExists(email)) {
            System.out.println("E-posta zaten kayıtlı: " + email);
            return -2;
        }

        String sql = "INSERT INTO bolum_koordinator (eposta, sifre, is_admin) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql))
        {
            ps.setString(1, email);
            ps.setString(2, sifre);
            ps.setBoolean(3, isAdmin);

            int rowsAffected = ps.executeUpdate();

            if(rowsAffected > 0) {
                System.out.println("Koordinatör başarıyla eklendi: " + email + " (Admin: " + isAdmin + ")");
                return 1;
            }
            return -1;
        }
        catch (SQLException e)
        {
            System.err.println("Koordinatör ekleme hatası: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * E-posta kontrolü
     */
    private static boolean isEmailExists(String email)
    {
        String sql = "SELECT COUNT(*) FROM bolum_koordinator WHERE eposta = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql))
        {
            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery())
            {
                if(rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        catch (SQLException e)
        {
            System.err.println("E-posta kontrol hatası: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Koordinatör siler (Sadece admin)
     *
     * @param email Silinecek koordinatör e-postası
     * @return true: Başarılı, false: Başarısız
     */
    public static boolean deleteKoordinator(String email)
    {
        String sql = "DELETE FROM bolum_koordinator WHERE eposta = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql))
        {
            ps.setString(1, email);
            int rowsAffected = ps.executeUpdate();

            if(rowsAffected > 0) {
                System.out.println("Koordinatör silindi: " + email);
                return true;
            }
            return false;
        }
        catch (SQLException e)
        {
            System.err.println("Koordinatör silme hatası: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Veritabanı bağlantı testi
     */
    public static boolean testConnection()
    {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD))
        {
            System.out.println("Veritabanı bağlantısı başarılı!");
            return true;
        }
        catch (SQLException e)
        {
            System.err.println("Veritabanı bağlantı hatası: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}