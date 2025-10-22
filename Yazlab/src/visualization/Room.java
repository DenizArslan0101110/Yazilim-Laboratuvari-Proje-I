package visualization;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class Room
{
    public static void SendToDatabaseOneClassroom(String isim, String kod, int enine_sira, int boyuna_sira, int sira_koltuk, int toplam_kapasite, String bolum)
    {
        String url = "jdbc:mysql://localhost:3306/muhendislik_fakulte?serverTimezone=UTC&useSSL=false";
        String user = "root";
        String pass = "yazlab1";

        try
        {
            Class.forName("com.mysql.cj.jdbc.Driver"); // optional on modern JDKs
            try (Connection conn = DriverManager.getConnection(url, user, pass))
            {
                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO derslik (isim, kod, enine_sira, boyuna_sira, sira_koltuk, toplam_kapasite, bolum) VALUES (?, ?, ?, ?, ?, ?, ?)"))
                {

                    ps.setString(1, isim);
                    ps.setString(2, kod);
                    ps.setInt(3, enine_sira);
                    ps.setInt(4, boyuna_sira);
                    ps.setInt(5, sira_koltuk);
                    ps.setInt(6, toplam_kapasite);
                    ps.setString(7, bolum);
                    System.out.println("Dbg SQL: Adding new derslik...");
                    ps.executeUpdate();
                }
                catch (Exception e)
                {
                    System.out.println("Dbg SQL: derslik already exists.");
                    e.printStackTrace();
                }
            }
        }
        catch (Exception e)
        {
            System.out.println("An unexpected error occured.");
            e.printStackTrace();
        }
    }
}
