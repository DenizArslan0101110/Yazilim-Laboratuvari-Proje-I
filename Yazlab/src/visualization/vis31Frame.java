package visualization;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.sql.*;
import java.util.ArrayList;

public class vis31Frame extends JFrame {

    String url = "jdbc:mysql://localhost:3306/muhendislik_fakulte?serverTimezone=UTC&useSSL=false";
    String user = "root";
    String pass = "yazlab1";


    final Timer resizeTimer;

    JButton themeButton;
    JButton geriButton;
    JTextField searchField;
    JScrollPane scrollPane;
    JPanel dersListPanel;

    ArrayList<Ders> tumDersler = new ArrayList<>();
    boolean isDarkMode;

    static final int DEFAULT_WIDTH = 1280;
    static final int DEFAULT_HEIGHT = 720;

    public vis31Frame(boolean isDarkMode) {
        this.isDarkMode = isDarkMode;

        this.setTitle("Ders Listesi");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(true);
        this.setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        this.setLayout(null);
        this.setLocationRelativeTo(null);

        resizeTimer = new Timer(100, e -> updateComponentBounds());
        resizeTimer.setRepeats(false);

        themeButton = new JButton(isDarkMode ? "L" : "D");
        geriButton = new JButton("Geri");
        searchField = new JTextField("");

        dersListPanel = new JPanel();
        dersListPanel.setLayout(new BoxLayout(dersListPanel, BoxLayout.Y_AXIS));

        scrollPane = new JScrollPane(dersListPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        this.add(themeButton);
        this.add(geriButton);
        this.add(searchField);
        this.add(scrollPane);

        geriButton.addActionListener(e -> {
            try {
                vis2Frame newScreen = new vis2Frame();
                newScreen.setDarkMode(this.isDarkMode);
                newScreen.setVisible(true);
                this.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Geri butonunda hata: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        themeButton.addActionListener(e -> {
            this.isDarkMode = !this.isDarkMode;
            applyTheme();
        });

        searchField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (searchField.getText().isEmpty()) {
                    searchField.setText("");
                }
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (searchField.getText().trim().isEmpty()) {
                    searchField.setText("");
                }
            }
        });

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterDersler();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterDersler();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterDersler();
            }
        });

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeTimer.restart();
            }
        });

        updateComponentBounds();
        applyTheme();

        loadDerslerFromDB();

        this.setVisible(true);
    }

    private void loadDerslerFromDB() {
        System.out.println("Veritabanından dersler yükleniyor...");

        tumDersler.clear();

        String sql = "SELECT kod, isim, sinif, Hoca FROM ders ORDER BY sinif, kod";

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                int count = 0;
                while (rs.next()) {
                    String kod = rs.getString("kod");
                    String isim = rs.getString("isim");
                    int sinif = rs.getInt("sinif");

                    String hoca = rs.getString("Hoca");

                    Ders ders = new Ders(kod, isim, sinif, hoca);
                    tumDersler.add(ders);
                    count++;

                    System.out.println("Ders eklendi: " + kod + " - " + isim);
                }

                System.out.println("Toplam " + count + " ders yüklendi.");

            }

        } catch (SQLException e) {
            System.err.println("SQL Hatası: " + e.getMessage());
        }

        populateDersList(tumDersler);
    }

    private void filterDersler() {
        String searchText = searchField.getText().toLowerCase().trim();

        if (searchText.isEmpty()) {
            populateDersList(tumDersler);
            return;
        }

        ArrayList<Ders> filtrelenmisListe = new ArrayList<>();
        for (Ders ders : tumDersler) {
            if (ders.dersKodu.toLowerCase().contains(searchText) ||
                    ders.dersAdi.toLowerCase().contains(searchText)) {
                filtrelenmisListe.add(ders);
            }
        }

        populateDersList(filtrelenmisListe);
    }

    // <-- GÜNCELLENDİ: Buton eklendi ve Sınıf=0 düzeltildi -->
    private void populateDersList(ArrayList<Ders> dersler) {
        dersListPanel.removeAll();

        if (dersler.isEmpty()) {
            JLabel emptyLabel = new JLabel("Ders bulunamadı", SwingConstants.CENTER);
            emptyLabel.setFont(new Font("Arial", Font.ITALIC, 16));
            dersListPanel.add(emptyLabel);
        } else {
            for (Ders ders : dersler) {
                JPanel dersPanel = new JPanel(new BorderLayout(10, 0));
                dersPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80)); // Yükseklik arttırıldı
                // dersPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15)); // (Tema metoduna taşındı)

                // Sol/Orta kısım: Ders bilgileri
                JPanel infoPanel = new JPanel();
                infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
                infoPanel.setOpaque(false); // Arka planı transparan yap

                JLabel dersLabel = new JLabel(String.format("<html><b>%s</b> - %s</html>",
                        ders.dersKodu, ders.dersAdi));
                dersLabel.setFont(new Font("Arial", Font.PLAIN, 16)); // Font büyütüldü

                // <-- YENİ: Sınıf 0 ise "Seçmeli Ders" yaz
                String sinifStr = (ders.sinif == 0) ? "Seçmeli Ders" : String.valueOf(ders.sinif);

                JLabel detayLabel = new JLabel(String.format("Sınıf: %s | Hoca: %s",
                        sinifStr, ders.hoca));
                detayLabel.setFont(new Font("Arial", Font.PLAIN, 12));

                infoPanel.add(dersLabel);
                infoPanel.add(Box.createRigidArea(new Dimension(0, 5)));
                infoPanel.add(detayLabel);

                // <-- YENİ: Sağ kısım: Buton -->
                JButton ogrencileriGosterButton = new JButton("Öğrenciler");
                ogrencileriGosterButton.setMargin(new Insets(5, 10, 5, 10)); // Butona iç boşluk
                ogrencileriGosterButton.addActionListener(e -> {
                    showOgrenciListDialog(ders);
                });

                dersPanel.add(infoPanel, BorderLayout.CENTER);
                dersPanel.add(ogrencileriGosterButton, BorderLayout.EAST);

                dersListPanel.add(dersPanel);
                dersListPanel.add(Box.createRigidArea(new Dimension(0, 5)));
            }
        }

        dersListPanel.revalidate();
        dersListPanel.repaint();
        applyThemeToListItems();
    }

    // <-- YENİ METOD: Öğrencileri gösteren diyalog penceresi -->
    private void showOgrenciListDialog(Ders ders) {
        JDialog dialog = new JDialog(this, "Dersi Alan Öğrenciler: " + ders.dersKodu, true);
        dialog.setSize(500, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel mainListPanel = new JPanel();
        mainListPanel.setLayout(new BoxLayout(mainListPanel, BoxLayout.Y_AXIS));

        // Diyalog için tema renkleri
        Color dialogBg = isDarkMode ? new Color(30, 30, 30) : Color.WHITE;
        Color panelBg = isDarkMode ? new Color(40, 40, 40) : new Color(248, 248, 248);
        Color textFg = isDarkMode ? Color.WHITE : Color.BLACK;

        dialog.getContentPane().setBackground(dialogBg);
        mainListPanel.setBackground(dialogBg);

        JScrollPane scroll = new JScrollPane(mainListPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(dialogBg);

        // ÖNEMLİ: ogrenci_ders adında bir ara tablo olduğunu varsayıyorum
        String sql = "SELECT o.isim, o.numara, o.sinif FROM ogrenci o " +
                "JOIN ogrenci_ders od ON o.numara = od.ogrenci_numara " +
                "WHERE od.ders_kod = ?";

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, ders.dersKodu);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.isBeforeFirst()) { // Kayıt yoksa
                    JLabel emptyLabel = new JLabel("Bu dersi alan öğrenci bulunamadı.");
                    emptyLabel.setForeground(textFg);
                    emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
                    emptyLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
                    mainListPanel.add(emptyLabel);
                } else {
                    while (rs.next()) {
                        String isim = rs.getString("isim");
                        String numara = rs.getString("numara");
                        int sinif = rs.getInt("sinif");

                        // vis32Frame'deki gibi bir panel oluştur
                        JPanel ogrenciPanel = new JPanel(new BorderLayout(10, 0));
                        ogrenciPanel.setBackground(panelBg);
                        ogrenciPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
                        ogrenciPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

                        JLabel ogrenciLabel = new JLabel(String.format("<html><b>%s</b> (%s)</html>", isim, numara));
                        ogrenciLabel.setForeground(textFg);
                        ogrenciPanel.add(ogrenciLabel, BorderLayout.CENTER);

                        JLabel sinifLabel = new JLabel("Sınıf: " + sinif);
                        sinifLabel.setForeground(textFg);
                        ogrenciPanel.add(sinifLabel, BorderLayout.EAST);

                        mainListPanel.add(ogrenciPanel);
                        mainListPanel.add(Box.createRigidArea(new Dimension(0, 5)));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JLabel errorLabel = new JLabel("Öğrenciler yüklenirken hata oluştu: " + e.getMessage());
            errorLabel.setForeground(Color.RED);
            errorLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
            mainListPanel.add(errorLabel);
        }

        dialog.add(scroll, BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    private void applyTheme() {
        Color background, foreground, buttonBg, searchBg, searchFg;

        if (isDarkMode) {
            background = new Color(18, 18, 18);
            foreground = Color.WHITE;
            buttonBg = new Color(30, 30, 30);
            searchBg = new Color(40, 40, 40);
            searchFg = Color.WHITE;
            themeButton.setText("L");
        } else {
            background = Color.WHITE;
            foreground = Color.BLACK;
            buttonBg = new Color(230, 230, 230);
            searchBg = new Color(245, 245, 245);
            searchFg = Color.BLACK;
            themeButton.setText("D");
        }

        this.getContentPane().setBackground(background);

        geriButton.setBackground(buttonBg);
        geriButton.setForeground(foreground);
        geriButton.setBorder(BorderFactory.createLineBorder(foreground, 1));
        geriButton.setFocusPainted(false);

        themeButton.setBackground(isDarkMode ? new Color(50, 50, 50) : new Color(200, 200, 200));
        themeButton.setForeground(foreground);
        themeButton.setBorder(BorderFactory.createLineBorder(foreground, 1));
        themeButton.setFocusPainted(false);

        searchField.setBackground(searchBg);
        searchField.setForeground(searchFg);
        searchField.setCaretColor(searchFg);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(foreground, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        scrollPane.getViewport().setBackground(background);
        scrollPane.setBorder(BorderFactory.createLineBorder(foreground, 1));
        dersListPanel.setBackground(background);

        applyThemeToListItems();
    }

    // <-- GÜNCELLENDİ: Yeni panel yapısına (infoPanel + button) göre tema uygular -->
    private void applyThemeToListItems() {
        Color panelBg = isDarkMode ? new Color(30, 30, 30) : new Color(248, 248, 248);
        Color textFg = isDarkMode ? Color.WHITE : Color.BLACK;
        Color buttonBg = isDarkMode ? new Color(50, 100, 150) : new Color(100, 150, 255);
        Color borderColor = isDarkMode ? new Color(50, 50, 50) : new Color(220, 220, 220);

        for (Component comp : dersListPanel.getComponents()) {
            if (comp instanceof JPanel) { // Bu dersPanel
                JPanel dersPanel = (JPanel) comp;
                dersPanel.setBackground(panelBg);
                // Panel etrafına kenarlık ve iç boşluk ekle
                dersPanel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(borderColor, 1),
                        BorderFactory.createEmptyBorder(10, 15, 10, 15)
                ));

                // Panelin içindeki bileşenleri bul (infoPanel ve Button)
                Component centerComp = ((BorderLayout)dersPanel.getLayout()).getLayoutComponent(BorderLayout.CENTER);
                Component eastComp = ((BorderLayout)dersPanel.getLayout()).getLayoutComponent(BorderLayout.EAST);

                // 1. infoPanel'deki etiketlerin rengini ayarla
                if (centerComp instanceof JPanel) {
                    JPanel infoPanel = (JPanel) centerComp;
                    for(Component infoChild : infoPanel.getComponents()){
                        if(infoChild instanceof JLabel){
                            infoChild.setForeground(textFg);
                        }
                    }
                }

                // 2. Butonun rengini ayarla
                if (eastComp instanceof JButton) {
                    JButton btn = (JButton) eastComp;
                    btn.setBackground(buttonBg);
                    btn.setForeground(Color.WHITE);
                    btn.setFocusPainted(false);
                    btn.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                }

            } else if (comp instanceof JLabel) { // "Ders bulunamadı" etiketi
                comp.setForeground(textFg);
            }
        }
    }

    void updateComponentBounds() {
        int w = this.getContentPane().getWidth();
        int h = this.getContentPane().getHeight();
        float aw = (float) w / DEFAULT_WIDTH;
        float ah = (float) h / DEFAULT_HEIGHT;
        int fontSize = (int) (16 * Math.min(aw, ah));

        geriButton.setFont(new Font("Arial", Font.BOLD, fontSize));
        themeButton.setFont(new Font("Arial", Font.BOLD, fontSize));
        searchField.setFont(new Font("Arial", Font.PLAIN, fontSize));

        geriButton.setBounds((int) (20 * aw), (int) (20 * ah), (int) (150 * aw), (int) (45 * ah));
        themeButton.setBounds((int) (1205 * aw), (int) (20 * ah), (int) (55 * aw), (int) (55 * ah));
        searchField.setBounds((int) (20 * aw), (int) (80 * ah), (int) (1240 * aw), (int) (50 * ah));
        scrollPane.setBounds((int) (20 * aw), (int) (145 * ah), (int) (1240 * aw), (int) (540 * ah));

        this.revalidate();
        this.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new vis31Frame(false);
        });
    }

    static class Ders {
        String dersKodu;
        String dersAdi;
        int sinif;
        String hoca;

        Ders(String kod, String isim, int sinif, String hoca) {
            this.dersKodu = kod;
            this.dersAdi = isim;
            this.sinif = sinif;
            this.hoca = hoca;
        }
    }
}