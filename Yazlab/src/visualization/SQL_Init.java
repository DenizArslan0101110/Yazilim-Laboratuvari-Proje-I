package visualization;

import java.sql.*;

// this class initializes the database itself
// and most its components in case the user does not have it locally

public class SQL_Init
{
    public static void Initialize()
    {
        String url = "jdbc:mysql://localhost:3306/muhendislik_fakulte?serverTimezone=UTC&useSSL=false";
        String user = "root";
        String pass = "yazlab1"; // root şifresini yazlab1 yap öteki türlü giremezsin

        System.out.println("Dbg SQL: Creating Database muhendislik_fakulte...");
        try (Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/?serverTimezone=UTC&useSSL=false",
                user, pass);
             Statement st = conn.createStatement())
        {
            st.executeUpdate("CREATE DATABASE IF NOT EXISTS muhendislik_fakulte");
        }
        catch (Exception e)
        {
            System.out.println("Dbg SQL: Database muhendislik_fakulte could not be added.");
        }

        try
        {
            Class.forName("com.mysql.cj.jdbc.Driver"); // optional on modern JDKs
            try (Connection conn = DriverManager.getConnection(url, user, pass))
            {
                try (Statement st = conn.createStatement())
                {
                    System.out.println("Dbg SQL: Creating table bolum_koordinator...");
                    st.executeUpdate("CREATE TABLE IF NOT EXISTS bolum_koordinator (" +
                            "isim VARCHAR(128)," +
                            "eposta VARCHAR(256) PRIMARY KEY," +
                            "sifre VARCHAR(64)," +
                            "bolum VARCHAR(128)," +
                            "is_admin BOOLEAN)");
                }
                catch (Exception e)
                {
                    System.out.println("Dbg SQL: Table bolum_koordinator could not be added.");
                    e.printStackTrace();
                }
                try (Statement st = conn.createStatement())
                {
                    System.out.println("Dbg SQL: Creating table derslik...");
                    st.executeUpdate("CREATE TABLE IF NOT EXISTS derslik (" +
                            "isim VARCHAR(128)," +
                            "kod VARCHAR(64) PRIMARY KEY," +
                            "enine_sira INT," +
                            "boyuna_sira INT," +
                            "sira_koltuk INT," +
                            "toplam_kapasite INT," +
                            "bolum VARCHAR(128))");
                }
                catch (Exception e)
                {
                    System.out.println("Dbg SQL: Table derslik could not be added.");
                    e.printStackTrace();
                }
                try (Statement st = conn.createStatement())
                {
                    System.out.println("Dbg SQL: Creating table ders...");
                    st.executeUpdate("CREATE TABLE IF NOT EXISTS ders (" +
                            "sinif INT," +
                            "isim VARCHAR(128)," +
                            "kod VARCHAR(64) PRIMARY KEY," +
                            "hoca VARCHAR(128))");
                }
                catch (Exception e)
                {
                    System.out.println("Dbg SQL: Table ders could not be added.");
                    e.printStackTrace();
                }
                try (Statement st = conn.createStatement())
                {
                    System.out.println("Dbg SQL: Creating table ogrenci...");
                    st.executeUpdate("CREATE TABLE IF NOT EXISTS ogrenci (" +
                            "isim VARCHAR(128)," +
                            "numara VARCHAR(64) PRIMARY KEY," +
                            "sinif INT)");
                }
                catch (Exception e)
                {
                    System.out.println("Dbg SQL: Table ogrenci could not be added.");
                    e.printStackTrace();
                }
                try (Statement st = conn.createStatement())
                {
                    System.out.println("Dbg SQL: Creating table ogrenci_ders...");
                    st.executeUpdate("CREATE TABLE IF NOT EXISTS ogrenci_ders (" +
                            "ogrenci_numara VARCHAR(64)," +
                            "ders_kod VARCHAR(64)," +
                            "FOREIGN KEY (ogrenci_numara) REFERENCES ogrenci(numara)," +
                            "FOREIGN KEY (ders_kod) REFERENCES ders(kod)," +
                            "PRIMARY KEY (ogrenci_numara, ders_kod))");

                }
                catch (Exception e)
                {
                    System.out.println("Dbg SQL: Table ogrenci_ders could not be added.");
                    e.printStackTrace();
                }
                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO bolum_koordinator (isim, eposta, sifre, bolum, is_admin) VALUES (?, ?, ?, ?, ?)"))
                {

                    ps.setString(1, "admin");
                    ps.setString(2, "admin@admin.com");
                    ps.setString(3, "admin");
                    ps.setString(4, "hepsi");
                    ps.setBoolean(5, true);
                    System.out.println("Dbg SQL: Adding user admin...");
                    ps.executeUpdate();
                }
                catch (Exception e)
                {
                    System.out.println("Dbg SQL: Admin already exists.");
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
