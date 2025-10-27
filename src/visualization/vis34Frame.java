package visualization;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.sql.*; // Veritabanı işlemleri için eklendi

public class vis34Frame extends JFrame {

    // Veritabanı bilgileri (Auth.java'dan alındı)
    private static final String DB_URL = "jdbc:mysql://localhost:3306/muhendislik_fakulte?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "yazlab1";

    // Tema Durumu
    boolean isDarkMode;

    // REFERANS BOYUTLAR
    static final int DEFAULT_WIDTH = 1280;
    static final int DEFAULT_HEIGHT = 720;

    // Dinamik boyutlandırma için timer (vis32Frame'deki gibi)
    final Timer resizeTimer;

    // Global Bileşenler
    JLabel titleLabel;
    JButton geriButton;
    JButton themeButton;
    JButton ekleButton; // Yeni kullanıcı ekle
    JButton silButton;  // Seçili kullanıcıyı sil
    JTable userTable;
    DefaultTableModel model;
    JScrollPane scrollPane;

    public vis34Frame(boolean isDark) {
        this.isDarkMode = isDark;

        this.setTitle("Kullanıcı Listesi");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        this.setLayout(null);
        this.setLocationRelativeTo(null);

        // Timer (vis32Frame'deki gibi)
        resizeTimer = new Timer(100, e -> updateComponentBounds());
        resizeTimer.setRepeats(false);

        initComponents(); // Bileşenleri oluştur
        addListeners();   // Butonlara işlevsellik ekle
        loadUserData();   // Verileri tabloya yükle

        applyTheme();     // Temayı uygula
        updateComponentBounds(); // Boyutları ayarla

        // Dinamik boyutlandırma için listener (vis32Frame'deki gibi)
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeTimer.restart();
            }
        });
    }

    private void initComponents() {
        titleLabel = new JLabel("Kullanıcı Listesi", SwingConstants.CENTER);
        geriButton = new JButton("Geri"); // vis32Frame'deki gibi
        themeButton = new JButton();
        ekleButton = new JButton("Yeni Kullanıcı Ekle");
        silButton = new JButton("Seçili Kullanıcıyı Sil");

        // JTable (Tablo) - Bölüm sütunu eklendi
        String[] columnNames = {"E-posta", "Admin Yetkisi", "Bölüm"};
        model = new DefaultTableModel(columnNames, 0) {
            // Tablonun hücrelerinin düzenlenmesini engelle
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        userTable = new JTable(model);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userTable.setRowHeight(30);

        scrollPane = new JScrollPane(userTable);

        // Frame'e ekle
        this.add(titleLabel);
        this.add(geriButton);
        this.add(themeButton);
        this.add(ekleButton);
        this.add(silButton);
        this.add(scrollPane);
    }

    private void addListeners() {
        // GERİ BUTONU (vis32Frame'deki gibi)
        geriButton.addActionListener(e -> {
            vis2Frame adminPanel = new vis2Frame();
            adminPanel.setDarkMode(this.isDarkMode);
            adminPanel.setVisible(true);
            this.dispose();
        });

        // TEMA BUTONU (vis32Frame'deki gibi)
        themeButton.addActionListener(e -> {
            isDarkMode = !isDarkMode;
            applyTheme();
        });

        // YENİ KULLANICI EKLE BUTONU
        ekleButton.addActionListener(e -> {
            showKoordinatorEkleDialog();
        });

        // SİL BUTONU
        silButton.addActionListener(e -> {
            int selectedRow = userTable.getSelectedRow();

            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this,
                        "Lütfen silmek için tablodan bir kullanıcı seçin.",
                        "Uyarı", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // E-postayı al (İlk sütun, index 0)
            String email = (String) model.getValueAt(selectedRow, 0);

            // Silme onayı
            int confirm = JOptionPane.showConfirmDialog(this,
                    "'" + email + "' e-postalı kullanıcıyı silmek istediğinize emin misiniz?",
                    "Silme Onayı",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                // Auth sınıfındaki deleteKoordinator metodunu kullan
                boolean success = Auth.deleteKoordinator(email);

                if (success) {
                    // Veritabanı işlemi başarılıysa, tablodan da kaldır
                    model.removeRow(selectedRow);
                    JOptionPane.showMessageDialog(this,
                            "Kullanıcı başarıyla silindi.",
                            "Başarılı", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Kullanıcı silinirken bir hata oluştu.",
                            "Hata", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    /**
     * Veritabanından kullanıcıları çeker ve tabloyu doldurur
     */
    private void loadUserData() {
        model.setRowCount(0); // Tabloyu temizle
        String sql = "SELECT eposta, is_admin, bolum FROM bolum_koordinator ORDER BY eposta";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String email = rs.getString("eposta");
                boolean isAdmin = rs.getBoolean("is_admin");
                String bolum = rs.getString("bolum");

                // Bölüm adını düzenle
                String bolumDisplay = (bolum != null && !bolum.isEmpty()) ?
                        capitalizeFirstLetter(bolum) : "-";

                // Tabloya ekle
                model.addRow(new Object[]{email, isAdmin ? "Evet" : "Hayır", bolumDisplay});
            }

        } catch (SQLException e) {
            System.err.println("Kullanıcıları yüklerken SQL Hatası: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Kullanıcılar veritabanından çekilemedi!", "Veritabanı Hatası", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * İlk harfi büyük yapar
     */
    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Yeni koordinatör eklemek için diyalog penceresi açar
     * (vis2Frame'deki diyalog ile aynı mantıkta, Auth sınıfını kullanır)
     */
    private void showKoordinatorEkleDialog()
    {
        JDialog dialog = new JDialog(this, "Yeni Koordinatör Ekle", true);
        dialog.setSize(450, 380); // Yükseklik artırıldı
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(null);

        // Diyalog için tema renkleri
        Color bg, fg;
        if(isDarkMode) {
            bg = new Color(30, 30, 30);
            fg = Color.WHITE;
        } else {
            bg = Color.WHITE;
            fg = Color.BLACK;
        }
        dialog.getContentPane().setBackground(bg);

        // Başlık
        JLabel baslikLabel = new JLabel("Yeni Koordinatör Bilgileri", SwingConstants.CENTER);
        baslikLabel.setFont(new Font("Arial", Font.BOLD, 18));
        baslikLabel.setForeground(fg);
        baslikLabel.setBounds(50, 20, 350, 30);
        dialog.add(baslikLabel);

        // E-posta
        JLabel emailLabel = new JLabel("E-posta:");
        emailLabel.setForeground(fg);
        emailLabel.setBounds(50, 70, 100, 25);
        dialog.add(emailLabel);

        JTextField emailField = new JTextField();
        emailField.setBounds(50, 95, 350, 30);
        dialog.add(emailField);

        // Şifre
        JLabel sifreLabel = new JLabel("Şifre:");
        sifreLabel.setForeground(fg);
        sifreLabel.setBounds(50, 135, 100, 25);
        dialog.add(sifreLabel);

        JPasswordField sifreField = new JPasswordField();
        sifreField.setBounds(50, 160, 350, 30);
        dialog.add(sifreField);

        // Bölüm Seçimi - YENİ EKLENEN KISIM
        JLabel bolumLabel = new JLabel("Bölüm:");
        bolumLabel.setForeground(fg);
        bolumLabel.setBounds(50, 200, 100, 25);
        dialog.add(bolumLabel);

        String[] bolumler = {"bilgisayar", "elektrik", "makine", "insaat", "endustri"};
        JComboBox<String> bolumComboBox = new JComboBox<>(bolumler);
        bolumComboBox.setBounds(50, 225, 350, 30);
        if(isDarkMode) {
            bolumComboBox.setBackground(new Color(40, 40, 40));
            bolumComboBox.setForeground(Color.WHITE);
        }
        dialog.add(bolumComboBox);
        // YENİ EKLENEN KISIM BİTTİ

        // Admin Checkbox
        JCheckBox adminCheckBox = new JCheckBox("Admin Yetkisi Ver");
        adminCheckBox.setForeground(fg);
        adminCheckBox.setBackground(bg);
        adminCheckBox.setBounds(50, 265, 200, 25);
        dialog.add(adminCheckBox);

        // Butonlar
        JButton kaydetButton = new JButton("Kaydet");
        kaydetButton.setBounds(90, 305, 120, 35);
        kaydetButton.setBackground(new Color(60, 150, 255));
        kaydetButton.setForeground(Color.WHITE);
        dialog.add(kaydetButton);

        JButton iptalButton = new JButton("İptal");
        iptalButton.setBounds(240, 305, 120, 35);
        iptalButton.setBackground(new Color(200, 60, 60));
        iptalButton.setForeground(Color.WHITE);
        dialog.add(iptalButton);

        // --- ENTER TUŞU İŞLEVSELLİĞİ ---

        // E-posta alanındayken Enter'a basılınca şifre alanına atla
        emailField.addActionListener(e -> {
            sifreField.requestFocusInWindow();
        });

        // Şifre alanındayken Enter'a basılınca kaydet butonunu tıkla
        sifreField.addActionListener(e -> {
            kaydetButton.doClick();
        });

        // --- ENTER TUŞU İŞLEVSELLİĞİ BİTTİ ---

        // KAYDET İŞLEMİ - BÖLÜM BİLGİSİ EKLENEREK GÜNCELLENDİ
        kaydetButton.addActionListener(e -> {
            String email = emailField.getText().trim();
            String sifre = new String(sifreField.getPassword());
            String bolum = (String) bolumComboBox.getSelectedItem();
            boolean isAdmin = adminCheckBox.isSelected();

            if(email.isEmpty() || sifre.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Lütfen tüm alanları doldurun!", "Uyarı", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if(!email.contains("@")) {
                JOptionPane.showMessageDialog(dialog, "Geçerli bir e-posta adresi girin!", "Uyarı", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Veritabanına bölüm bilgisiyle birlikte ekle
            boolean success = addKoordinatorWithBolum(email, sifre, bolum, isAdmin);

            if(success) {
                JOptionPane.showMessageDialog(dialog, "Koordinatör başarıyla eklendi.", "Başarılı", JOptionPane.INFORMATION_MESSAGE);
                loadUserData(); // Tabloyu yenile
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog, "Koordinatör eklenirken bir hata oluştu!", "Hata", JOptionPane.ERROR_MESSAGE);
            }
        });

        // İPTAL
        iptalButton.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    /**
     * Koordinatör ekleme metodu - Bölüm bilgisi dahil
     */
    private boolean addKoordinatorWithBolum(String email, String sifre, String bolum, boolean isAdmin) {
        String sql = "INSERT INTO bolum_koordinator (eposta, sifre, bolum, is_admin) VALUES (?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, email);
            pst.setString(2, sifre); // Şifre düz metin olarak kaydediliyor (Auth sınıfı gibi)
            pst.setString(3, bolum);
            pst.setBoolean(4, isAdmin);

            int result = pst.executeUpdate();
            return result > 0;

        } catch (SQLException e) {
            System.err.println("Koordinatör eklenirken hata: " + e.getMessage());
            return false;
        }
    }

    private void applyTheme() {
        Color background, foreground, buttonBg, tableBg, tableFg, tableHeaderBg, tableHeaderFg;
        Color themeButtonBg, selectionBg, selectionFg;

        Font tableHeaderFont = userTable.getTableHeader().getFont();
        userTable.getTableHeader().setFont(tableHeaderFont.deriveFont(Font.BOLD));

        if (isDarkMode) {
            background = new Color(18, 18, 18);
            foreground = Color.WHITE;
            buttonBg = new Color(30, 30, 30);
            tableBg = new Color(40, 40, 40);
            tableFg = Color.WHITE;
            tableHeaderBg = new Color(50, 50, 50);
            tableHeaderFg = Color.WHITE;

            themeButton.setText("L");
            themeButtonBg = new Color(50, 50, 50);
            selectionBg = new Color(0, 100, 200);
            selectionFg = Color.WHITE;

        } else {
            background = Color.WHITE;
            foreground = Color.BLACK;
            buttonBg = new Color(230, 230, 230);
            tableBg = Color.WHITE;
            tableFg = Color.BLACK;
            tableHeaderBg = new Color(220, 220, 220);
            tableHeaderFg = Color.BLACK;

            themeButton.setText("D");
            themeButtonBg = new Color(200, 200, 200);
            selectionBg = new Color(180, 210, 255);
            selectionFg = Color.BLACK;
        }

        this.getContentPane().setBackground(background);
        titleLabel.setForeground(foreground);

        // Butonlar
        geriButton.setBackground(buttonBg);
        geriButton.setForeground(foreground);
        geriButton.setBorder(BorderFactory.createLineBorder(foreground, 1));
        geriButton.setFocusPainted(false);

        ekleButton.setBackground(new Color(60, 150, 255)); // Ekle butonu mavi
        ekleButton.setForeground(Color.WHITE);
        ekleButton.setBorder(BorderFactory.createLineBorder(new Color(100, 180, 255), 1));
        ekleButton.setFocusPainted(false);

        silButton.setBackground(new Color(200, 60, 60)); // Sil butonu kırmızı
        silButton.setForeground(Color.WHITE);
        silButton.setBorder(BorderFactory.createLineBorder(new Color(255, 100, 100), 1));
        silButton.setFocusPainted(false);

        themeButton.setBackground(themeButtonBg);
        themeButton.setForeground(foreground);
        themeButton.setBorder(BorderFactory.createLineBorder(foreground, 1));
        themeButton.setFocusPainted(false);

        // JTable Tema
        userTable.setBackground(tableBg);
        userTable.setForeground(tableFg);
        userTable.setGridColor(foreground);
        userTable.setSelectionBackground(selectionBg);
        userTable.setSelectionForeground(selectionFg);

        JTableHeader header = userTable.getTableHeader();
        header.setBackground(tableHeaderBg);
        header.setForeground(tableHeaderFg);

        // ScrollPane
        scrollPane.getViewport().setBackground(tableBg);
        scrollPane.setBorder(BorderFactory.createLineBorder(foreground, 1));
    }

    void updateComponentBounds() {
        int w = this.getContentPane().getWidth();
        int h = this.getContentPane().getHeight();
        float aw = (float) w / DEFAULT_WIDTH;
        float ah = (float) h / DEFAULT_HEIGHT;

        // Fontlar
        int fontSize = (int) (16 * Math.min(aw, ah));
        int titleFontSize = (int) (24 * Math.min(aw, ah));
        Font buttonFont = new Font("Arial", Font.BOLD, fontSize);
        Font titleFont = new Font("Arial", Font.BOLD, titleFontSize);
        Font tableFont = new Font("Arial", Font.PLAIN, (int)(fontSize * 0.9));
        Font tableHeaderFont = new Font("Arial", Font.BOLD, fontSize);

        titleLabel.setFont(titleFont);
        geriButton.setFont(buttonFont);
        ekleButton.setFont(buttonFont);
        silButton.setFont(buttonFont);
        themeButton.setFont(buttonFont);
        userTable.setFont(tableFont);
        userTable.getTableHeader().setFont(tableHeaderFont);

        // Konumlar ve Boyutlar
        geriButton.setBounds((int) (20 * aw), (int) (20 * ah), (int) (150 * aw), (int) (45 * ah));
        themeButton.setBounds((int) (1205 * aw), (int) (20 * ah), (int) (55 * aw), (int) (55 * ah));
        titleLabel.setBounds((int) (200 * aw), (int) (20 * ah), (int) (880 * aw), (int) (45 * ah));

        // Tablo
        scrollPane.setBounds((int) (20 * aw), (int) (85 * ah), (int) (1240 * aw), (int) (550 * ah));

        // Ekle ve Sil Butonları (Tablonun altında)
        int bottomButtonY = (int) (650 * ah);
        int bottomButtonHeight = (int) (45 * ah);
        int bottomButtonWidth = (int) (610 * aw); // İki buton yanyana

        ekleButton.setBounds((int) (20 * aw), bottomButtonY, bottomButtonWidth, bottomButtonHeight);
        silButton.setBounds((int) (650 * aw), bottomButtonY, bottomButtonWidth, bottomButtonHeight);

        this.revalidate();
        this.repaint();
    }
}