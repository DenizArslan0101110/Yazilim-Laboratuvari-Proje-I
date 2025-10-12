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

    //login için flag
    private static int did_login_succeed;

    // Global Bileşenler
    JLabel welcomeLabel;
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

    // Konumlandırma Gecikmesi için Timer (Debouncing)
    private final Timer resizeTimer;
    private static final int RESIZE_DELAY_MS = 100; // 100 milisaniye gecikme

    // İkon Yolları (Lütfen kendi yollarınızı doğrulayın)
    private final String ICON_PATH_OPEN = "/assets/gozuacik.png";
    private final String ICON_PATH_CLOSED = "/assets/gozukapali.png";

    // Sabitler
    static final double FORM_START_X_PERCENT = 0.38;
    static final double FIELD_WIDTH_PERCENT = 0.25;
    static final double TOGGLE_BUTTON_WIDTH_PERCENT = 0.08;
    static final int BASE_HORIZONTAL_GAP = 10;
    static final int THEME_BUTTON_SIZE = 40;
    static final int BASE_MARGIN = 20;

    // TEMEL BOYUTLAR (Referans)
    static final int BASE_WIDTH = 1280;
    static final int BASE_HEIGHT = 720;
    static final int BASE_VERTICAL_GAP = 50;
    static final int BASE_FIELD_HEIGHT = 30;
    static final int BASE_FONT_SIZE = 20;

    private ImageIcon iconOpen;
    private ImageIcon iconClosed;


    public vis1Frame()
    {

        // FRAME AYAR
        this.setTitle("Kocaeli Üniversitesi Giriş Ekranı");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(true);
        this.setSize(BASE_WIDTH, BASE_HEIGHT);
        this.setLayout(null);

        // TIMER TANIMLAMASI (Donma Çözümü) BUNU ELLERSEN DONUYO MS LAZIM ÇAKIŞIYO
        resizeTimer = new Timer(RESIZE_DELAY_MS, e -> {
            updateComponentBounds();
        });
        resizeTimer.setRepeats(false); // Sadece bir kez çalıştır

        // --- BİLEŞENLERİN OLUŞTURULMASI ---
        welcomeLabel = new JLabel("HOŞ GELDİNİZ", SwingConstants.CENTER);
        userLabel = new JLabel("Kullanıcı Adı:");
        passLabel = new JLabel("Parola:");
        usernameField = new JTextField();
        passwordField = new JPasswordField();
        passwordField.setEchoChar('*');
        loginButton = new JButton("Giriş Yap");

        togglePasswordButton = new JButton();
        togglePasswordButton.setText("");

        themeButton = new JButton();
        themeButton.setText(isDarkMode ? "L" : "D");

        // FRAME'E EKLEME
        this.add(welcomeLabel);
        this.add(userLabel);
        this.add(usernameField);
        this.add(passLabel);
        this.add(passwordField);
        this.add(loginButton);
        this.add(togglePasswordButton);
        this.add(themeButton);

        // DİNAMİK BOYUTLANDIRMA VE KONUMLANDIRMA
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // Her boyut değiştiğinde Timer'ı yeniden başlatıyo
                resizeTimer.restart();
            }
        });

        applyTheme();
        updateComponentBounds();
        updateToggleButtonIcon();

        //  BUTON İŞLEYİCİLER   LOGİN BUTON BURDA
        loginButton.addActionListener(e -> {
            String email = usernameField.getText();
            String pass = new String(passwordField.getPassword());

            did_login_succeed = CheckAccInfo(email, pass);

            if(did_login_succeed == 3) // admin girişi başarılı
            {
                vis2Frame newScreen = new vis2Frame();
                newScreen.setDarkMode(isDarkMode);
                newScreen.setVisible(true);
                vis1Frame.this.dispose();

            }
            else if(did_login_succeed == 2) // koordinatör girişi başarılı
            {
                vis2Frame newScreen = new vis2Frame();
                newScreen.setDarkMode(isDarkMode);
                newScreen.setVisible(true);
                vis1Frame.this.dispose();
            }
            else if(did_login_succeed == 1) // eposta doğru şifre yanlış
            {
                JOptionPane.showMessageDialog
                        (vis1Frame.this,
                        "E-posta ile parola uyuşmuyor.",
                        "Giriş Başarısız", JOptionPane.ERROR_MESSAGE);
            }
            else if(did_login_succeed == 0) // eposta yanlış
            {
                JOptionPane.showMessageDialog
                        (vis1Frame.this,
                        "Girilen e-posta bulunamadı.",
                        "Giriş Başarısız", JOptionPane.ERROR_MESSAGE);
            }
            else
            {
                JOptionPane.showMessageDialog
                        (vis1Frame.this,
                        "Bilinmeyen hata.",
                        "??????", JOptionPane.ERROR_MESSAGE);
            }
        });

        togglePasswordButton.addActionListener(e -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible)
            {
                passwordField.setEchoChar((char) 0);
            }
            else
            {
                passwordField.setEchoChar('*');
            }
            updateToggleButtonIcon();
        });

        themeButton.addActionListener(e -> {
            isDarkMode = !isDarkMode;
            applyTheme();
        });

        // GÖSTERME
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

    // Tema bilgisini vis2Frame'den almak için metot
    public void setDarkMode(boolean isDark)
    {
        isDarkMode = isDark;
        applyTheme();
        updateToggleButtonIcon();
    }

    // YARDIMCI METOTLAR

    private void updateToggleButtonIcon()
    {
        int buttonHeight = togglePasswordButton.getHeight();
        int iconSize = (int)(buttonHeight * 0.8);
        if (iconSize < 10) iconSize = 10;

        try
        {
            iconOpen = new ImageIcon
            (new ImageIcon(ICON_PATH_OPEN).getImage().getScaledInstance(iconSize, iconSize, Image.SCALE_SMOOTH));
            System.out.println(iconSize + "\n");
            iconClosed = new ImageIcon
            (new ImageIcon(ICON_PATH_CLOSED).getImage().getScaledInstance(iconSize, iconSize, Image.SCALE_SMOOTH));

            togglePasswordButton.setText("");
            if (isPasswordVisible)
            {
                togglePasswordButton.setIcon(iconClosed);
            }
            else
            {
                togglePasswordButton.setIcon(iconOpen);
            }

        }
        catch (Exception ex)
        {
            togglePasswordButton.setText(isPasswordVisible ? "GİZLE" : "GÖSTER");
            togglePasswordButton.setIcon(null);
        }
    }


    private void applyTheme() {
        Color background, foreground, fieldBg, fieldFg, buttonBg;

        // vis2Frame ile senkronize renkler
        if (isDarkMode) {
            background = Color.BLACK;
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

        themeButton.setBackground(isDarkMode ? new Color(50, 50, 50) : new Color(200, 200, 200));
        themeButton.setForeground(foreground);
    }

    // PENCERE BOYUT HESAPLAYICI (Dinamik Konumlandırma)
    void updateComponentBounds()
    {
        int frameWidth = this.getContentPane().getWidth();
        int frameHeight = this.getContentPane().getHeight();

        double widthScale = (double) frameWidth / BASE_WIDTH;
        double heightScale = (double) frameHeight / BASE_HEIGHT;
        double finalScale = Math.min(widthScale, heightScale);

        int newGap = (int) (BASE_VERTICAL_GAP * heightScale);
        int newHeight = (int) (BASE_FIELD_HEIGHT * heightScale);
        int newFontSize = (int) (BASE_FONT_SIZE * finalScale);
        int horizontalGap = (int) (BASE_HORIZONTAL_GAP * widthScale);
        int themeBtnSize = (int) (THEME_BUTTON_SIZE * finalScale);

        if (newHeight < 28) newHeight = 28;
        if (newGap < 15) newGap = 15;
        if (newFontSize < 14) newFontSize = 14;

        // FONT UYGULAMA
        Font newFont = new Font("Arial", Font.BOLD, newFontSize);
        int welcomeFontSize = (int) (newFontSize * 3.0);
        if (welcomeFontSize < 40) welcomeFontSize = 40;
        Font welcomeFont = new Font("Arial", Font.BOLD, welcomeFontSize);
        welcomeLabel.setFont(welcomeFont);

        int labelFontSize = (int) (newFontSize * 0.70);
        if (labelFontSize < 12) labelFontSize = 12;
        Font labelFont = new Font("Arial", Font.BOLD, labelFontSize);
        userLabel.setFont(labelFont);
        passLabel.setFont(labelFont);

        usernameField.setFont(newFont);
        passwordField.setFont(newFont);
        loginButton.setFont(newFont);
        themeButton.setFont(newFont.deriveFont((float)newFontSize * 0.5f));

        // KONUM HESAPLAMALARI
        int fieldWidth = (int) (frameWidth * FIELD_WIDTH_PERCENT);
        int startX = (int) (frameWidth * FORM_START_X_PERCENT);
        int labelWidth = fieldWidth;
        int toggleButtonWidth = (int) (frameWidth * TOGGLE_BUTTON_WIDTH_PERCENT);

        // Dikey Merkezleme Hesaplamaları
        int centerPointY = frameHeight / 2;
        int titleHeight = (int) (newHeight * 4);
        int titleGap = newGap;

        int formTotalHeight = (5 * newHeight) + (2 * newGap) + titleHeight + titleGap;
        int startY = centerPointY - (formTotalHeight / 2);

        int welcomeY = startY;
        int userLabelY = welcomeY + titleHeight + titleGap;
        int usernameFieldY = userLabelY + newHeight;
        int passLabelY = usernameFieldY + newHeight + newGap;
        int passwordFieldY = passLabelY + newHeight;
        int loginY = passwordFieldY + newHeight + newGap;

        // SetBounds UYGULAMA
        int welcomeWidth = (int) (frameWidth * 0.50);
        int welcomeX = (frameWidth / 2) - (welcomeWidth / 2);
        welcomeLabel.setBounds(welcomeX, welcomeY, welcomeWidth, titleHeight);

        userLabel.setBounds(startX, userLabelY, labelWidth, newHeight);
        usernameField.setBounds(startX, usernameFieldY, fieldWidth, newHeight);

        passLabel.setBounds(startX, passLabelY, labelWidth, newHeight);
        passwordField.setBounds(startX, passwordFieldY, fieldWidth, newHeight);

        // TOGGLE BUTONU
        int toggleX = startX + fieldWidth + horizontalGap;
        togglePasswordButton.setBounds(toggleX, passwordFieldY, toggleButtonWidth, newHeight);

        int loginButtonWidth = (int) (frameWidth * 0.15);
        int loginButtonX = (frameWidth / 2) - (loginButtonWidth / 2);
        loginButton.setBounds(loginButtonX, loginY, loginButtonWidth, newHeight + 10);

        // TEMA BUTONU KONUMLANDIRMA (SAĞ ÜST KÖŞE)
        int themeX = frameWidth - (int)(BASE_MARGIN * widthScale) - themeBtnSize;
        int themeY = (int)(BASE_MARGIN * heightScale);
        themeButton.setBounds(themeX, themeY, themeBtnSize, themeBtnSize);

        this.revalidate();
        this.repaint();
    }

    // Hesap bilgilerinin kontrolü için fonksiyon
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
                        System.out.println("Giriş başarılı, sisteme yönlendiriliyorsunuz.");
                        if (isAdmin)
                        {
                            System.out.println("Hoşgeldiniz, admin!");
                            return 3;
                        }
                        return 2;
                    }
                    else
                    {
                        System.out.println("Belirtilen E-posta ve şifre uyuşmuyor.");
                        return 1;
                    }
                }
                else
                {
                    System.out.println("Belirtilen E-posta sisteme kayıtlı değil.");
                    return 0;
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return -1;
        }
    }
}