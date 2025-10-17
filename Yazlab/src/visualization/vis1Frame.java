package visualization;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class vis1Frame extends JFrame
{
    // Login flag
    private static int did_login_succeed;

    // Global Bileşenler
    JLabel welcomeLabel;
    JLabel logoLabel;
    JTextField usernameField;
    JPasswordField passwordField;
    JButton loginButton;
    JButton togglePasswordButton;
    JButton themeButton;
    JLabel userLabel;
    JLabel passLabel;

    // Tema ve İkon Durumu
    static public boolean isDarkMode = true;
    private boolean isPasswordVisible = false;

    // Timer (Debouncing)
    private final Timer resizeTimer;

    private ImageIcon iconOpen;
    private ImageIcon iconClosed;

    public vis1Frame()
    {
        // FRAME AYARLARI
        this.setTitle("Kocaeli Üniversitesi Giriş Ekranı");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(true);
        this.setSize(1280, 720);
        this.setLayout(null);

        // TIMER
        resizeTimer = new Timer(100, e -> updateComponentBounds());
        resizeTimer.setRepeats(false);

        // BİLEŞEN OLUŞTURMA
        welcomeLabel = new JLabel("HOŞGELDİNİZ", SwingConstants.CENTER);
        logoLabel = new JLabel();
        userLabel = new JLabel("Kullanıcı Adı:");
        passLabel = new JLabel("Parola:");
        usernameField = new JTextField();
        passwordField = new JPasswordField();
        passwordField.setEchoChar('*');
        loginButton = new JButton("Giriş Yap");
        togglePasswordButton = new JButton();
        themeButton = new JButton();

        // FRAME'E EKLEME
        this.add(welcomeLabel);
        this.add(logoLabel);
        this.add(userLabel);
        this.add(usernameField);
        this.add(passLabel);
        this.add(passwordField);
        this.add(loginButton);
        this.add(togglePasswordButton);
        this.add(themeButton);

        // DİNAMİK BOYUTLANDIRMA
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeTimer.restart();
            }
        });

        applyTheme();
        updateComponentBounds();
        updateToggleButtonIcon();

        // LOGIN BUTON
        loginButton.addActionListener(e -> {
            String email = usernameField.getText();
            String pass = new String(passwordField.getPassword());

            did_login_succeed = CheckAccInfo(email, pass);

            if(did_login_succeed == 3) // admin
            {
                vis2Frame newScreen = new vis2Frame();
                newScreen.setDarkMode(isDarkMode);
                newScreen.setVisible(true);
                vis1Frame.this.dispose();
            }
            else if(did_login_succeed == 2) // koordinatör
            {
                vis2Frame newScreen = new vis2Frame();
                newScreen.setDarkMode(isDarkMode);
                newScreen.setVisible(true);
                vis1Frame.this.dispose();
            }
            else if(did_login_succeed == 1) // şifre yanlış
            {
                JOptionPane.showMessageDialog(vis1Frame.this,
                        "E-posta ile parola uyuşmuyor.",
                        "Giriş Başarısız", JOptionPane.ERROR_MESSAGE);
            }
            else if(did_login_succeed == 0) // eposta yanlış
            {
                JOptionPane.showMessageDialog(vis1Frame.this,
                        "Girilen e-posta bulunamadı.",
                        "Giriş Başarısız", JOptionPane.ERROR_MESSAGE);
            }
            else
            {
                JOptionPane.showMessageDialog(vis1Frame.this,
                        "Bilinmeyen hata.",
                        "??????", JOptionPane.ERROR_MESSAGE);
            }
        });

        // TOGGLE PASSWORD BUTON
        togglePasswordButton.addActionListener(e -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                passwordField.setEchoChar((char) 0);
            } else {
                passwordField.setEchoChar('*');
            }
            updateToggleButtonIcon();
        });

        // THEME BUTON
        themeButton.addActionListener(e -> {
            isDarkMode = !isDarkMode;
            applyTheme();
        });

        // ENTER TUŞLARI
        usernameField.addActionListener(e -> passwordField.requestFocusInWindow());
        passwordField.addActionListener(e -> loginButton.doClick());


        // GÖSTER
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

    public void setDarkMode(boolean isDark)
    {
        isDarkMode = isDark;
        applyTheme();
        updateToggleButtonIcon();
    }

    private void updateToggleButtonIcon() {

        int iconSize = (int)(togglePasswordButton.getHeight() * 0.8);
        if (iconSize < 10) iconSize = 10;
        if (iconSize == 0) return;

        try {
            java.net.URL openIconUrl = getClass().getResource("/assets/gozuacik.png");
            java.net.URL closedIconUrl = getClass().getResource("/assets/gozukapali.png");

            if (openIconUrl != null && closedIconUrl != null) {
                ImageIcon originalOpenIcon = new ImageIcon(openIconUrl);
                ImageIcon originalClosedIcon = new ImageIcon(closedIconUrl);

                iconOpen = new ImageIcon(originalOpenIcon.getImage()
                        .getScaledInstance(iconSize, iconSize, Image.SCALE_SMOOTH));
                iconClosed = new ImageIcon(originalClosedIcon.getImage()
                        .getScaledInstance(iconSize, iconSize, Image.SCALE_SMOOTH));

                togglePasswordButton.setText("");
                if (isPasswordVisible) {
                    togglePasswordButton.setIcon(iconClosed);
                } else {
                    togglePasswordButton.setIcon(iconOpen);
                }
            } else {
                throw new Exception("İkon dosyaları bulunamadı.");
            }
        } catch (Exception ex) {
            togglePasswordButton.setIcon(null);
            if (isPasswordVisible) {
                togglePasswordButton.setText("Gizle");
            } else {
                togglePasswordButton.setText("Göster");
            }
        }
    }

    private void applyTheme()
    {
        Color background, foreground, fieldBg, fieldFg, buttonBg;

        if (isDarkMode) {
            background = new Color(0, 0, 0);
            foreground = Color.WHITE;
            fieldBg = Color.WHITE;
            fieldFg = Color.BLACK;
            buttonBg = new Color(30, 30, 30);
            themeButton.setText("L");
        }
        else
        {
            background = Color.WHITE;
            foreground = Color.BLACK;
            fieldBg = Color.WHITE;
            fieldFg = Color.BLACK;
            buttonBg = new Color(230, 230, 230);
            themeButton.setText("D");
        }

        this.getContentPane().setBackground(background);
        welcomeLabel.setForeground(foreground);
        userLabel.setForeground(foreground);
        passLabel.setForeground(foreground);

        usernameField.setBackground(fieldBg);
        usernameField.setForeground(fieldFg);
        passwordField.setBackground(fieldBg);
        passwordField.setForeground(fieldFg);

        loginButton.setBackground(new Color(60, 150, 255));
        loginButton.setForeground(Color.WHITE);

        togglePasswordButton.setBackground(buttonBg);
        togglePasswordButton.setForeground(foreground);

        if (isDarkMode) {
            themeButton.setBackground(new Color(50, 50, 50));
        } else {
            themeButton.setBackground(new Color(200, 200, 200));
        }
        themeButton.setForeground(foreground);
    }

    void updateComponentBounds()
    {
        int w = this.getContentPane().getWidth();
        int h = this.getContentPane().getHeight();

        float aw = (float) w / 1280;
        float ah = (float) h / 720;

        // FONT BOYUTLARI
        int fontSize = (int)(20 * Math.min(aw, ah));
        if (fontSize < 14) fontSize = 14;

        Font mainFont = new Font("Arial", Font.BOLD, fontSize);
        Font welcomeFont = new Font("Arial", Font.BOLD, fontSize * 3);
        Font labelFont = new Font("Arial", Font.BOLD, (int)(fontSize * 0.7));

        welcomeLabel.setFont(welcomeFont);
        userLabel.setFont(labelFont);
        passLabel.setFont(labelFont);
        usernameField.setFont(mainFont);
        passwordField.setFont(mainFont);
        loginButton.setFont(mainFont);
        themeButton.setFont(mainFont.deriveFont((float)fontSize * 0.9f));

        // KONUMLAR VE BOYUTLAR
        welcomeLabel.setBounds((int)(390 * aw), (int)(50 * ah), (int)(500 * aw), (int)(100 * ah));

        // --- LOGO EKLENDİ ---
        logoLabel.setBounds((int)(540 * aw), (int)(160 * ah), (int)(200 * aw), (int)(150 * ah));
        try {
            java.net.URL logoURL = getClass().getResource("/assets/Kouyenilogo.png");
            if (logoURL != null) {
                ImageIcon originalLogo = new ImageIcon(logoURL);
                Image scaledLogo = originalLogo.getImage().getScaledInstance((int)(200 * aw), (int)(150 * ah), Image.SCALE_SMOOTH);
                logoLabel.setIcon(new ImageIcon(scaledLogo));
            }
        } catch (Exception e) {
            logoLabel.setIcon(null);
        }
        // --- LOGO BİTTİ ---

        // --- LOGONUN ALTINDAKİ BİLEŞENLERİN KONUMLARI GÜNCELLENDİ ---
        userLabel.setBounds((int)(486 * aw), (int)(330 * ah), (int)(320 * aw), (int)(30 * ah));
        usernameField.setBounds((int)(486 * aw), (int)(360 * ah), (int)(320 * aw), (int)(30 * ah));

        passLabel.setBounds((int)(486 * aw), (int)(400 * ah), (int)(320 * aw), (int)(30 * ah));
        passwordField.setBounds((int)(486 * aw), (int)(430 * ah), (int)(320 * aw), (int)(30 * ah));

        togglePasswordButton.setBounds((int)(816 * aw), (int)(430 * ah), (int)(100 * aw), (int)(30 * ah));

        loginButton.setBounds((int)(540 * aw), (int)(490 * ah), (int)(200 * aw), (int)(40 * ah));
        // --- GÜNCELLEME BİTTİ ---

        themeButton.setBounds((int)(1205 * aw), (int)(20 * ah), (int)(55 * aw), (int)(55 * ah));

        this.revalidate();
        this.repaint();
    }

    public static int CheckAccInfo(String email, String password)
    {
        String url = "jdbc:mysql://localhost:3306/muhendislik_fakulte?serverTimezone=UTC&useSSL=false";
        String user = "root";
        String pass = "yazlab1";

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
                        if (isAdmin)
                        {
                            return 3; // Admin
                        }
                        return 2; // Koordinatör
                    }
                    else
                    {
                        return 1; // Şifre yanlış
                    }
                }
                else
                {
                    return 0; // E-posta bulunamadı
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return -1; // DB Hatası
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(vis1Frame::new);
    }
}