import java.sql.*;
import java.util.Scanner;

// this class checks if a login attempt succeeds or not

public class Auth
{
    public static void CheckAccInfo()
    {
        String url = "jdbc:mysql://localhost:3306/muhendislik_fakulte?serverTimezone=UTC&useSSL=false";
        String user = "root";
        String pass = "yazlab1";

        // bunları kullanıcının inputu ile değiştir, fonksiyona parametre olarak verebilirsin
        Scanner temp = new Scanner(System.in);
        System.out.println("E-posta ve şifre giriniz: ");
        String email = temp.nextLine();
        String password = temp.nextLine();

        String sql = "SELECT sifre, is_admin FROM bolum_koordinator WHERE eposta = ?";

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             PreparedStatement ps = conn.prepareStatement(sql))
        {
            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery())
            {
                if (rs.next())
                {
                    String dbPassword = rs.getString("sifre");
                    boolean isAdmin = rs.getBoolean("is_admin");

                    if (dbPassword.equals(password))
                    {
                        System.out.println("Giriş başarılı, sisteme yönlendiriliyorsunuz.");
                        if (isAdmin)
                        {
                            System.out.println("Hoşgeldiniz, admin!");
                        }
                    }
                    else
                    {
                        System.out.println("Belirtilen E-posta ve şifre uyuşmuyor.");
                    }
                }
                else
                {
                    System.out.println("Belirtilen E-posta sisteme kayıtlı değil.");
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
