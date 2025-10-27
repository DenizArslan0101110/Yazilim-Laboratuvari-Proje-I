package visualization;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.sql.*;
import java.util.ArrayList;

public class vis3Frame extends JFrame
{
    // Veritabanı Bilgileri
    String url = "jdbc:mysql://localhost:3306/muhendislik_fakulte?allowPublicKeyRetrieval=true&serverTimezone=UTC&useSSL=false";
    String user = "root";
    String pass = "yazlab1";

    // Global Bileşenler
    JButton themeButton;
    JButton derslikEkleButton;
    JButton geriButton;
    JPanel derslikListPanel;
    JScrollPane scrollPane;

    // Derslik listesi
    ArrayList<Derslik> derslikler = new ArrayList<>();

    // Tema Durumu
    boolean isDarkMode;

    // Timer (Donma Çözümü)
    final Timer resizeTimer;

    // REFERANS BOYUTLAR
    static final int DEFAULT_WIDTH = 1280;
    static final int DEFAULT_HEIGHT = 720;

    public vis3Frame(boolean isDarkMode)
    {
        this.isDarkMode = isDarkMode;

        this.setTitle("Derslik Yönetimi");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(true);
        this.setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        this.setLayout(null);
        this.setLocationRelativeTo(null);

        // TIMER
        resizeTimer = new Timer(100, e -> updateComponentBounds());
        resizeTimer.setRepeats(false);

        // BİLEŞEN OLUŞTURMA
        themeButton = new JButton(isDarkMode ? "L" : "D");
        derslikEkleButton = new JButton("Derslik Ekle");
        geriButton = new JButton("Geri");

        derslikListPanel = new JPanel();
        derslikListPanel.setLayout(new BoxLayout(derslikListPanel, BoxLayout.Y_AXIS));

        scrollPane = new JScrollPane(derslikListPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        this.add(themeButton);
        this.add(derslikEkleButton);
        this.add(geriButton);
        this.add(scrollPane);

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeTimer.restart();
            }
        });

        applyTheme();
        updateComponentBounds();

        loadDersliklerFromDatabase(); // DB'den yükle

        // --- BUTON İŞLEYİCİLER ---
        themeButton.addActionListener(e -> {
            this.isDarkMode = !this.isDarkMode;
            applyTheme();
            // Tema değiştiğinde tüm derslik panellerini güncelle
            for (Component comp : derslikListPanel.getComponents()) {
                if (comp instanceof JPanel) {
                    derslikPanelUpdateTheme((JPanel) comp);
                }
            }
        });

        derslikEkleButton.addActionListener(e -> {
            showDerslikEkleDialog();
        });

        geriButton.addActionListener(e -> {
            // vis2Frame'e geri dön
            vis2Frame newScreen = new vis2Frame();
            newScreen.setDarkMode(this.isDarkMode);
            newScreen.setVisible(true);
            this.dispose();
        });


        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

    // --- METOTLAR ---

    void loadDersliklerFromDatabase()
    {
        derslikler.clear();
        derslikListPanel.removeAll(); // Listeyi temizle

        String sql = "SELECT * FROM derslik ORDER BY kod";

        try (Connection connection = DriverManager.getConnection(url, user, pass);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql))
        {
            while (resultSet.next())
            {
                Derslik derslik = new Derslik(
                        resultSet.getString("bolum"),
                        resultSet.getString("kod"),
                        resultSet.getString("isim"),
                        resultSet.getInt("enine_sira"),
                        resultSet.getInt("boyuna_sira"),
                        resultSet.getInt("sira_koltuk"),
                        resultSet.getInt("toplam_kapasite")
                );
                derslikler.add(derslik);
                derslikEkleScrollPane(derslik);
            }
            System.out.println("Dbg SQL: Derslikler başarıyla yüklendi. Toplam: " + derslikler.size());
            derslikListPanel.revalidate();
            derslikListPanel.repaint();
        }
        catch (SQLException e)
        {
            System.err.println("Dbg SQL: Derslikler yüklenemedi: " + e.getMessage());
        }
    }

    // (GÜNCELLENDİ)
    void showDerslikEkleDialog() {
        JDialog dialog = new JDialog(this, "Derslik Ekle", true);
        dialog.setSize(450, 450);
        dialog.setLayout(null);
        dialog.setLocationRelativeTo(this);

        // --- BİLEŞENLER ---
        JLabel bolumAdiLabel = new JLabel("Bölüm Adı:");
        JTextField bolumAdiField = new JTextField();
        JLabel derslikKoduLabel = new JLabel("Derslik Kodu:");
        JTextField derslikKoduField = new JTextField();
        JLabel isimLabel = new JLabel("Derslik İsmi:");
        JTextField isimField = new JTextField();
        JLabel siraSayisiYatayLabel = new JLabel("Sıra Sayısı (Yatay):");
        JTextField siraSayisiYatayField = new JTextField();
        JLabel siraSayisiDikeyLabel = new JLabel("Sıra Sayısı (Dikey):");
        JTextField siraSayisiDikeyField = new JTextField();
        JLabel siraKapasitesiLabel = new JLabel("Sıra Kapasitesi (Fiziksel):");
        JTextField siraKapasitesiField = new JTextField("2");
        JLabel toplamKapasiteLabel = new JLabel("Toplam Kapasite: 0");
        toplamKapasiteLabel.setFont(new Font("Arial", Font.BOLD, 14));
        JButton kaydetButton = new JButton("Kaydet");
        JButton iptalButton = new JButton("İptal");

        // Konumlandırma
        bolumAdiLabel.setBounds(30, 20, 150, 30);
        bolumAdiField.setBounds(200, 20, 220, 30);
        derslikKoduLabel.setBounds(30, 60, 150, 30);
        derslikKoduField.setBounds(200, 60, 220, 30);
        isimLabel.setBounds(30, 100, 150, 30);
        isimField.setBounds(200, 100, 220, 30);
        siraSayisiYatayLabel.setBounds(30, 140, 150, 30);
        siraSayisiYatayField.setBounds(200, 140, 220, 30);
        siraSayisiDikeyLabel.setBounds(30, 180, 150, 30);
        siraSayisiDikeyField.setBounds(200, 180, 220, 30);
        siraKapasitesiLabel.setBounds(30, 220, 150, 30);
        siraKapasitesiField.setBounds(200, 220, 220, 30);
        toplamKapasiteLabel.setBounds(30, 260, 390, 30);
        kaydetButton.setBounds(120, 310, 100, 35);
        iptalButton.setBounds(240, 310, 100, 35);

        // Dialog'a ekle
        dialog.add(bolumAdiLabel); dialog.add(bolumAdiField);
        dialog.add(derslikKoduLabel); dialog.add(derslikKoduField);
        dialog.add(isimLabel); dialog.add(isimField);
        dialog.add(siraSayisiYatayLabel); dialog.add(siraSayisiYatayField);
        dialog.add(siraSayisiDikeyLabel); dialog.add(siraSayisiDikeyField);
        dialog.add(siraKapasitesiLabel); dialog.add(siraKapasitesiField);
        dialog.add(toplamKapasiteLabel);
        dialog.add(kaydetButton); dialog.add(iptalButton);

        // Tema uygula
        Color background, foreground, buttonBg, fieldBg, fieldFg;
        if (isDarkMode) {
            background = Color.BLACK; foreground = Color.WHITE; buttonBg = new Color(30, 30, 30);
        } else {
            background = Color.WHITE; foreground = Color.BLACK; buttonBg = new Color(230, 230, 230);
        }
        fieldBg = Color.WHITE; fieldFg = Color.BLACK;
        dialog.getContentPane().setBackground(background);
        bolumAdiLabel.setForeground(foreground); derslikKoduLabel.setForeground(foreground);
        isimLabel.setForeground(foreground); siraSayisiYatayLabel.setForeground(foreground);
        siraSayisiDikeyLabel.setForeground(foreground);
        siraKapasitesiLabel.setForeground(foreground);
        toplamKapasiteLabel.setForeground(foreground);
        bolumAdiField.setBackground(fieldBg); bolumAdiField.setForeground(fieldFg);
        derslikKoduField.setBackground(fieldBg); derslikKoduField.setForeground(fieldFg);
        isimField.setBackground(fieldBg); isimField.setForeground(fieldFg);
        siraSayisiYatayField.setBackground(fieldBg); siraSayisiYatayField.setForeground(fieldFg);
        siraSayisiDikeyField.setBackground(fieldBg); siraSayisiDikeyField.setForeground(fieldFg);
        siraKapasitesiField.setBackground(fieldBg); siraKapasitesiField.setForeground(fieldFg);
        kaydetButton.setBackground(new Color(60, 150, 255)); kaydetButton.setForeground(Color.WHITE);
        iptalButton.setBackground(buttonBg); iptalButton.setForeground(foreground);

        // KAPASİTE HESAPLAMA (Dinamik) (YENİ KURALA GÖRE GÜNCELLENDİ)
        Runnable hesaplaKapasite = () -> {
            try {
                int yatay = Integer.parseInt(siraSayisiYatayField.getText().trim());
                int dikey = Integer.parseInt(siraSayisiDikeyField.getText().trim());
                int siraKap = Integer.parseInt(siraKapasitesiField.getText().trim());

                // --- YENİ KURAL ---
                // Sırada 2 veya daha az koltuk varsa 1 kişi oturur.
                // Sırada 2'den fazla koltuk varsa 2 kişi (kenarlara) oturur.
                int efektifKoltukSayisi = (siraKap <= 2) ? 1 : 2;
                int toplam = yatay * dikey * efektifKoltukSayisi;
                // --- KURAL SONU ---

                toplamKapasiteLabel.setText("Toplam (Efektif) Kapasite: " + toplam);
            } catch (NumberFormatException ex) {
                toplamKapasiteLabel.setText("Toplam Kapasite: 0");
            }
        };

        siraSayisiYatayField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) { hesaplaKapasite.run(); }
        });
        siraSayisiDikeyField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) { hesaplaKapasite.run(); }
        });
        siraKapasitesiField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) { hesaplaKapasite.run(); }
        });
        // İlk açılışta da hesapla
        hesaplaKapasite.run();


        // KAYDET BUTON (YENİ KURALA GÖRE GÜNCELLENDİ)
        kaydetButton.addActionListener(e -> {
            String bolumAdi = bolumAdiField.getText().trim();
            String derslikKodu = derslikKoduField.getText().trim();
            String isim = isimField.getText().trim();
            String yatayStr = siraSayisiYatayField.getText().trim();
            String dikeyStr = siraSayisiDikeyField.getText().trim();
            String siraKapStr = siraKapasitesiField.getText().trim(); // Fiziksel kapasite

            if (bolumAdi.isEmpty() || derslikKodu.isEmpty() || isim.isEmpty() || yatayStr.isEmpty() || dikeyStr.isEmpty() || siraKapStr.isEmpty())
            {
                JOptionPane.showMessageDialog(dialog, "Lütfen tüm alanları doldurun!", "Hata", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try
            {
                int yatay = Integer.parseInt(yatayStr);
                int dikey = Integer.parseInt(dikeyStr);
                int siraKap = Integer.parseInt(siraKapStr); // Fiziksel kapasite (Örn: 4)

                if (yatay <= 0 || dikey <= 0 || siraKap <= 0)
                {
                    JOptionPane.showMessageDialog(dialog, "Sıra sayıları ve kapasite pozitif olmalıdır!", "Hata", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // --- YENİ KURAL ---
                int efektifKoltukSayisi = (siraKap <= 2) ? 1 : 2;
                int toplamKapasite = yatay * dikey * efektifKoltukSayisi; // Bu, DB'ye kaydedilecek efektif kapasite (Örn: 200)
                // --- KURAL SONU ---

                // --- VERİTABANI KAYIT ---
                try (Connection conn = DriverManager.getConnection(url, user, pass);
                     PreparedStatement ps = conn.prepareStatement("INSERT INTO derslik (isim, kod, enine_sira, boyuna_sira, sira_koltuk, toplam_kapasite, bolum) VALUES (?, ?, ?, ?, ?, ?, ?)"))
                {
                    ps.setString(1, isim);
                    ps.setString(2, derslikKodu);
                    ps.setInt(3, yatay);
                    ps.setInt(4, dikey);
                    ps.setInt(5, siraKap); // DB'ye fiziksel kapasiteyi (sira_koltuk) kaydet (Örn: 4)
                    ps.setInt(6, toplamKapasite); // DB'ye efektif kapasiteyi (toplam_kapasite) kaydet (Örn: 200)
                    ps.setString(7, bolumAdi);

                    System.out.println("Dbg SQL: Adding new classroom...");
                    ps.executeUpdate();

                    Derslik yeniDerslik = new Derslik(bolumAdi, derslikKodu, isim, yatay, dikey, siraKap, toplamKapasite);
                    derslikler.add(yeniDerslik);
                    derslikEkleScrollPane(yeniDerslik);

                    JOptionPane.showMessageDialog(dialog,
                            "Derslik başarıyla eklendi!\nEfektif Kapasite: " + toplamKapasite,
                            "Başarılı", JOptionPane.INFORMATION_MESSAGE);

                    dialog.dispose();
                }
                catch (SQLException exc)
                {
                    JOptionPane.showMessageDialog(dialog, "Veritabanı hatası! Kod daha önce kullanılmış olabilir.", "Hata", JOptionPane.ERROR_MESSAGE);
                    exc.printStackTrace();
                }
            }
            catch (NumberFormatException ex)
            {
                JOptionPane.showMessageDialog(dialog, "Sıra sayıları ve kapasite geçerli sayı olmalıdır!", "Hata", JOptionPane.ERROR_MESSAGE);
            }
        });

        iptalButton.addActionListener(e -> dialog.dispose());
        dialog.setVisible(true);
    }

    // --- DÜZENLEME DİYALOG PENCERESİ --- (YENİ KURALA GÖRE GÜNCELLENDİ)
    void showDerslikDuzenleDialog(Derslik derslik, JPanel derslikPanel, JLabel bilgiLabel) {
        JDialog dialog = new JDialog(this, "Derslik Düzenle", true);
        dialog.setSize(450, 450);
        dialog.setLayout(null);
        dialog.setLocationRelativeTo(this);

        // --- Bileşenler ---
        JLabel bolumAdiLabel = new JLabel("Bölüm Adı:");
        JTextField bolumAdiField = new JTextField(derslik.bolumAdi);
        JLabel derslikKoduLabel = new JLabel("Derslik Kodu:");
        JTextField derslikKoduField = new JTextField(derslik.derslikKodu);
        derslikKoduField.setEditable(false);
        derslikKoduField.setBackground(Color.LIGHT_GRAY);
        JLabel isimLabel = new JLabel("Derslik İsmi:");
        JTextField isimField = new JTextField(derslik.isim);
        JLabel siraSayisiYatayLabel = new JLabel("Sıra Sayısı (Yatay):");
        JTextField siraSayisiYatayField = new JTextField(String.valueOf(derslik.siraSayisiYatay));
        JLabel siraSayisiDikeyLabel = new JLabel("Sıra Sayısı (Dikey):");
        JTextField siraSayisiDikeyField = new JTextField(String.valueOf(derslik.siraSayisiDikey));
        JLabel siraKapasitesiLabel = new JLabel("Sıra Kapasitesi (Fiziksel):");
        JTextField siraKapasitesiField = new JTextField(String.valueOf(derslik.siraKapasitesi));
        JLabel toplamKapasiteLabel = new JLabel("Toplam Kapasite: " + derslik.toplamKapasite);
        toplamKapasiteLabel.setFont(new Font("Arial", Font.BOLD, 14));
        JButton kaydetButton = new JButton("Kaydet");
        JButton iptalButton = new JButton("İptal");

        // --- Konumlandırma (setBounds) ---
        bolumAdiLabel.setBounds(30, 20, 150, 30);
        bolumAdiField.setBounds(200, 20, 220, 30);
        derslikKoduLabel.setBounds(30, 60, 150, 30);
        derslikKoduField.setBounds(200, 60, 220, 30);
        isimLabel.setBounds(30, 100, 150, 30);
        isimField.setBounds(200, 100, 220, 30);
        siraSayisiYatayLabel.setBounds(30, 140, 150, 30);
        siraSayisiYatayField.setBounds(200, 140, 220, 30);
        siraSayisiDikeyLabel.setBounds(30, 180, 150, 30);
        siraSayisiDikeyField.setBounds(200, 180, 220, 30);
        siraKapasitesiLabel.setBounds(30, 220, 150, 30);
        siraKapasitesiField.setBounds(200, 220, 220, 30);
        toplamKapasiteLabel.setBounds(30, 260, 390, 30);
        kaydetButton.setBounds(120, 310, 100, 35);
        iptalButton.setBounds(240, 310, 100, 35);

        // --- Diyaloğa Ekleme ---
        dialog.add(bolumAdiLabel); dialog.add(bolumAdiField);
        dialog.add(derslikKoduLabel); dialog.add(derslikKoduField);
        dialog.add(isimLabel); dialog.add(isimField);
        dialog.add(siraSayisiYatayLabel); dialog.add(siraSayisiYatayField);
        dialog.add(siraSayisiDikeyLabel); dialog.add(siraSayisiDikeyField);
        dialog.add(siraKapasitesiLabel); dialog.add(siraKapasitesiField);
        dialog.add(toplamKapasiteLabel);
        dialog.add(kaydetButton); dialog.add(iptalButton);

        // --- Tema ---
        Color background, foreground, buttonBg, fieldBg, fieldFg;
        if (isDarkMode) {
            background = Color.BLACK; foreground = Color.WHITE; buttonBg = new Color(30, 30, 30);
        } else {
            background = Color.WHITE; foreground = Color.BLACK; buttonBg = new Color(230, 230, 230);
        }
        fieldBg = Color.WHITE; fieldFg = Color.BLACK;
        dialog.getContentPane().setBackground(background);
        bolumAdiLabel.setForeground(foreground); derslikKoduLabel.setForeground(foreground);
        isimLabel.setForeground(foreground); siraSayisiYatayLabel.setForeground(foreground);
        siraSayisiDikeyLabel.setForeground(foreground);
        siraKapasitesiLabel.setForeground(foreground);
        toplamKapasiteLabel.setForeground(foreground);
        bolumAdiField.setBackground(fieldBg); bolumAdiField.setForeground(fieldFg);
        isimField.setBackground(fieldBg); isimField.setForeground(fieldFg);
        siraSayisiYatayField.setBackground(fieldBg); siraSayisiYatayField.setForeground(fieldFg);
        siraSayisiDikeyField.setBackground(fieldBg); siraSayisiDikeyField.setForeground(fieldFg);
        siraKapasitesiField.setBackground(fieldBg); siraKapasitesiField.setForeground(fieldFg);
        kaydetButton.setBackground(new Color(60, 150, 255)); kaydetButton.setForeground(Color.WHITE);
        iptalButton.setBackground(buttonBg); iptalButton.setForeground(foreground);

        // --- İşlevsellik (Kapasite Hesaplama) (YENİ KURALA GÖRE GÜNCELLENDİ) ---
        Runnable hesaplaKapasite = () -> {
            try {
                int yatay = Integer.parseInt(siraSayisiYatayField.getText().trim());
                int dikey = Integer.parseInt(siraSayisiDikeyField.getText().trim());
                int siraKap = Integer.parseInt(siraKapasitesiField.getText().trim());

                // --- YENİ KURAL ---
                int efektifKoltukSayisi = (siraKap <= 2) ? 1 : 2;
                int toplam = yatay * dikey * efektifKoltukSayisi;
                // --- KURAL SONU ---

                toplamKapasiteLabel.setText("Toplam (Efektif) Kapasite: " + toplam);
            } catch (NumberFormatException ex) {
                toplamKapasiteLabel.setText("Toplam Kapasite: 0");
            }
        };

        siraSayisiYatayField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) { hesaplaKapasite.run(); }
        });
        siraSayisiDikeyField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) { hesaplaKapasite.run(); }
        });
        siraKapasitesiField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) { hesaplaKapasite.run(); }
        });

        // --- Kaydet Butonu (UPDATE) (YENİ KURALA GÖRE GÜNCELLENDİ) ---
        kaydetButton.addActionListener(e -> {
            String bolumAdi = bolumAdiField.getText().trim();
            String isim = isimField.getText().trim();
            String yatayStr = siraSayisiYatayField.getText().trim();
            String dikeyStr = siraSayisiDikeyField.getText().trim();
            String siraKapStr = siraKapasitesiField.getText().trim(); // Fiziksel

            if (bolumAdi.isEmpty() || isim.isEmpty() || yatayStr.isEmpty() || dikeyStr.isEmpty() || siraKapStr.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Lütfen tüm alanları doldurun!", "Hata", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                int yatay = Integer.parseInt(yatayStr);
                int dikey = Integer.parseInt(dikeyStr);
                int siraKap = Integer.parseInt(siraKapStr); // Fiziksel (Örn: 4)

                if (yatay <= 0 || dikey <= 0 || siraKap <= 0) {
                    JOptionPane.showMessageDialog(dialog, "Sıra sayıları ve kapasite pozitif olmalıdır!", "Hata", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // --- YENİ KURAL ---
                int efektifKoltukSayisi = (siraKap <= 2) ? 1 : 2;
                int toplamKapasite = yatay * dikey * efektifKoltukSayisi; // Efektif (Örn: 200)
                // --- KURAL SONU ---

                String sql = "UPDATE derslik SET isim = ?, enine_sira = ?, boyuna_sira = ?, sira_koltuk = ?, toplam_kapasite = ?, bolum = ? WHERE kod = ?";

                try (Connection conn = DriverManager.getConnection(url, user, pass);
                     PreparedStatement ps = conn.prepareStatement(sql)) {

                    ps.setString(1, isim);
                    ps.setInt(2, yatay);
                    ps.setInt(3, dikey);
                    ps.setInt(4, siraKap); // Fiziksel kapasite (sira_koltuk)
                    ps.setInt(5, toplamKapasite); // Efektif kapasite (toplam_kapasite)
                    ps.setString(6, bolumAdi);
                    ps.setString(7, derslik.derslikKodu);

                    System.out.println("Dbg SQL: Updating classroom...");
                    ps.executeUpdate();

                    // Arayüzü ve lokal listeyi güncelle
                    derslik.bolumAdi = bolumAdi;
                    derslik.isim = isim;
                    derslik.siraSayisiYatay = yatay;
                    derslik.siraSayisiDikey = dikey;
                    derslik.siraKapasitesi = siraKap;
                    derslik.toplamKapasite = toplamKapasite;

                    String yeniBilgi = String.format("<html><b>Bölüm: %s | Kod: %s | İsim: %s</b><br>Efektif Kapasite: %d (%dx%d sıra, %d koltuk/sıra)</html>",
                            derslik.bolumAdi, derslik.derslikKodu, derslik.isim, derslik.toplamKapasite,
                            derslik.siraSayisiYatay, derslik.siraSayisiDikey, derslik.siraKapasitesi); // Bilgi label'ını da güncelledim
                    bilgiLabel.setText(yeniBilgi);

                    derslikPanel.revalidate();
                    derslikPanel.repaint();

                    JOptionPane.showMessageDialog(dialog, "Derslik başarıyla güncellendi!", "Başarılı", JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose();

                } catch (SQLException exc) {
                    System.err.println("Dbg SQL: Classroom could not be updated: " + exc.getMessage());
                    JOptionPane.showMessageDialog(dialog, "Veritabanı güncellenirken hata oluştu!", "Hata", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Sıra sayıları ve kapasite geçerli sayı olmalıdır!", "Hata", JOptionPane.ERROR_MESSAGE);
            }
        });

        iptalButton.addActionListener(e -> dialog.dispose());
        dialog.setVisible(true);
    }


    void derslikEkleScrollPane(Derslik derslik) {
        JPanel derslikPanel = new JPanel();
        derslikPanel.setLayout(new BorderLayout());
        derslikPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        derslikPanel.setPreferredSize(new Dimension(0, 80));

        // Bilgi metnini yeni mantığa göre güncelledim
        String bilgi = String.format("<html><b>Bölüm: %s | Kod: %s | İsim: %s</b><br>Efektif Kapasite: %d (%dx%d sıra, %d koltuk/sıra)</html>",
                derslik.bolumAdi, derslik.derslikKodu, derslik.isim, derslik.toplamKapasite,
                derslik.siraSayisiYatay, derslik.siraSayisiDikey, derslik.siraKapasitesi);

        JLabel bilgiLabel = new JLabel(bilgi);
        bilgiLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        bilgiLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 10));
        JButton gorselButton = new JButton("Görüntüle");
        JButton duzenleButton = new JButton("Düzenle");
        JButton silButton = new JButton("Sil");
        gorselButton.setPreferredSize(new Dimension(90, 40));
        duzenleButton.setPreferredSize(new Dimension(90, 40));
        silButton.setPreferredSize(new Dimension(70, 40));

        gorselButton.addActionListener(e -> {
            showDerslikGorsel(derslik);
        });

        duzenleButton.addActionListener(e -> {
            showDerslikDuzenleDialog(derslik, derslikPanel, bilgiLabel);
        });

        silButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "'" + derslik.isim + "' (" + derslik.derslikKodu + ") dersliğini silmek istediğinize emin misiniz?",
                    "Silme Onayı",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;
            String sql = "DELETE FROM derslik WHERE kod = ?";
            try (Connection conn = DriverManager.getConnection(url, user, pass);
                 PreparedStatement stmt = conn.prepareStatement(sql))
            {
                stmt.setString(1, derslik.derslikKodu);
                stmt.executeUpdate();
                derslikler.remove(derslik);
                derslikListPanel.remove(derslikPanel);
                derslikListPanel.revalidate();
                derslikListPanel.repaint();
                System.out.println("Dbg SQL: Classroom deleted successfully.");
            }
            catch (SQLException except)
            {
                System.err.println("Dbg SQL: Classroom could not be deleted: " + except.getMessage());
            }
        });

        buttonPanel.add(gorselButton);
        buttonPanel.add(duzenleButton);
        buttonPanel.add(silButton);
        derslikPanel.add(bilgiLabel, BorderLayout.CENTER);
        derslikPanel.add(buttonPanel, BorderLayout.EAST);
        applyDerslikPanelTheme(derslikPanel, bilgiLabel, buttonPanel, gorselButton, duzenleButton, silButton);
        derslikListPanel.add(derslikPanel);
        derslikListPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        derslikListPanel.revalidate();
        derslikListPanel.repaint();
    }

    void derslikPanelUpdateTheme(JPanel parentPanel)
    {
        if (!(parentPanel.getLayout() instanceof BorderLayout)) return;
        Component center = ((BorderLayout)parentPanel.getLayout()).getLayoutComponent(BorderLayout.CENTER);
        Component east = ((BorderLayout)parentPanel.getLayout()).getLayoutComponent(BorderLayout.EAST);
        if (center instanceof JLabel && east instanceof JPanel)
        {
            JLabel label = (JLabel) center;
            JPanel buttonPanel = (JPanel) east;
            JButton btn1 = null, btn2 = null, btn3 = null;
            if (buttonPanel.getComponentCount() >= 3)
            {
                btn1 = (JButton) buttonPanel.getComponent(0); // Görüntüle
                btn2 = (JButton) buttonPanel.getComponent(1); // Düzenle
                btn3 = (JButton) buttonPanel.getComponent(2); // Sil
            }
            if (btn1 != null && btn2 != null && btn3 != null)
            {
                applyDerslikPanelTheme(parentPanel, label, buttonPanel, btn1, btn2, btn3);
            }
        }
    }

    void showDerslikGorsel(Derslik derslik) {
        JFrame gorselFrame = new JFrame(derslik.isim + " - Görünüm");
        gorselFrame.setSize(900, 700);
        gorselFrame.setLayout(new BorderLayout());
        gorselFrame.setLocationRelativeTo(this);

        JPanel masaPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Stroke originalStroke = g2d.getStroke();
                Stroke thickerStroke = new BasicStroke(3); // Kalın çizgi (Oturaklar)
                Stroke thinnerStroke = new BasicStroke(1); // İnce çizgi (Masalar)
                int panelW = getWidth(); int panelH = getHeight();
                double panelPaddingY = panelH * 0.05; double panelPaddingX = panelW * 0.05;
                double cizimAlaniW = panelW - (panelPaddingX * 2); double cizimAlaniH = panelH - (panelPaddingY * 2);
                double hucreGenisligi = cizimAlaniW / derslik.siraSayisiYatay;
                double hucreYuksekligi = cizimAlaniH / derslik.siraSayisiDikey;
                int masaW = (int)(hucreGenisligi * 0.9); int masaH = (int)(masaW * 0.4);
                int boslukX = (int)(hucreGenisligi * 0.1); int boslukY = (int)(hucreYuksekligi) - masaH;
                if (masaW < 10) masaW = 10; if (masaH < 5) masaH = 5;
                if (boslukX < 2) boslukX = 2; if (boslukY < 10) boslukY = 10;
                int toplamW = (derslik.siraSayisiYatay * masaW) + ((derslik.siraSayisiYatay - 1) * boslukX);
                int toplamH = (derslik.siraSayisiDikey * (masaH + boslukY));
                int startX = (panelW - toplamW) / 2; int startY = (panelH - toplamH) / 2;

                // Fiziksel kapasiteyi al
                int siraKapasitesi = derslik.getSiraKapasitesi();

                for (int i = 0; i < derslik.siraSayisiDikey; i++) {
                    for (int j = 0; j < derslik.siraSayisiYatay; j++) {
                        int masaX = startX + (j * (masaW + boslukX));
                        int masaY = startY + (i * (masaH + boslukY));

                        // 1. Masa çerçevesi
                        g2d.setStroke(thinnerStroke);
                        g2d.setColor(isDarkMode ? Color.WHITE : Color.GRAY);
                        g2d.drawRoundRect(masaX, masaY, masaW, masaH, 5, 5);

                        // Masayı böl
                        if (siraKapasitesi > 1) {
                            for (int k = 1; k < siraKapasitesi; k++) {
                                int bolmeX = masaX + (masaW * k / siraKapasitesi);
                                g2d.drawLine(bolmeX, masaY, bolmeX, masaY + masaH);
                            }
                        }

                        // 2. Oturakları Çiz
                        g2d.setStroke(thickerStroke);
                        int koltukOffsetY = masaH + (int)(boslukY * 0.1);
                        int koltukW = (int)(masaW * (0.8 / siraKapasitesi));
                        int koltukH = koltukW;

                        // --- YENİ KURALA GÖRE OTURAKLARI ÇİZ ---
                        Color redEdge = new Color(255, 100, 100);
                        Color blueMiddle = new Color(0, 120, 215);
                        Color grayDisabled = new Color(100, 100, 100);

                        // Hangi koltukların çizileceğini belirle
                        ArrayList<Integer> cizilecekKoltuklar = new ArrayList<>();
                        if (siraKapasitesi <= 2) {
                            cizilecekKoltuklar.add(0); // Sadece ilk koltuk
                        } else {
                            cizilecekKoltuklar.add(0); // İlk koltuk
                            cizilecekKoltuklar.add(siraKapasitesi - 1); // Son koltuk
                        }

                        for (int k = 0; k < siraKapasitesi; k++) {
                            int bolmeBaslangicX = masaX + (masaW * k / siraKapasitesi);
                            int bolmeGenisligi = (masaW / siraKapasitesi);
                            int koltukX = bolmeBaslangicX + (bolmeGenisligi - koltukW) / 2;

                            Color seatFillColor;

                            // Bu koltuk kullanılacak mı?
                            if (cizilecekKoltuklar.contains(k)) {
                                // Evet. Rengini belirle (Kırmızı/Mavi mantığı)
                                if (k == 0 || k == siraKapasitesi - 1) {
                                    seatFillColor = redEdge;
                                } else {
                                    seatFillColor = blueMiddle; // (Bu senaryo <=2 kuralıyla anlamsız kaldı ama zararı yok)
                                }
                            } else {
                                // Hayır. Koltuğu gri (pasif) çiz
                                seatFillColor = grayDisabled;
                            }

                            g2d.setColor(seatFillColor);
                            g2d.fillRect(koltukX, masaY + koltukOffsetY, koltukW, koltukH);
                            g2d.setColor(isDarkMode ? Color.WHITE : Color.BLACK);
                            g2d.drawRect(koltukX, masaY + koltukOffsetY, koltukW, koltukH);
                        }
                    }
                }
                g2d.setStroke(originalStroke);
            }
        };

        masaPanel.setBackground(isDarkMode ? Color.BLACK : Color.WHITE);
        JButton kapat = new JButton("Kapat");
        kapat.addActionListener(e -> gorselFrame.dispose());
        gorselFrame.add(masaPanel, BorderLayout.CENTER);
        gorselFrame.add(kapat, BorderLayout.SOUTH);
        gorselFrame.setVisible(true);
    }

    // 3 butonu da alacak şekilde güncellendi
    void applyDerslikPanelTheme(JPanel panel, JLabel label, JPanel buttonPanel, JButton btnGorsel, JButton btnDuzenle, JButton btnSil)
    {
        Color panelBg; Color foreground;
        Color buttonBgGorsel; Color buttonBgDuzenle; Color buttonBgSil;
        if (isDarkMode) {
            panelBg = new Color(30, 30, 30); foreground = Color.WHITE;
            buttonBgGorsel = new Color(50, 100, 150); buttonBgDuzenle = new Color(50, 150, 50);
            buttonBgSil = new Color(180, 50, 50);
        } else {
            panelBg = new Color(240, 240, 240); foreground = Color.BLACK;
            buttonBgGorsel = new Color(100, 150, 255); buttonBgDuzenle = new Color(100, 200, 100);
            buttonBgSil = new Color(255, 100, 100);
        }
        panel.setBackground(panelBg);
        panel.setBorder(BorderFactory.createLineBorder(foreground, 1));
        label.setForeground(foreground);
        buttonPanel.setBackground(panelBg);
        btnGorsel.setBackground(buttonBgGorsel); btnGorsel.setForeground(Color.WHITE);
        btnDuzenle.setBackground(buttonBgDuzenle); btnDuzenle.setForeground(Color.WHITE);
        btnSil.setBackground(buttonBgSil); btnSil.setForeground(Color.WHITE);
    }

    public void setDarkMode(boolean isDark)
    {
        this.isDarkMode = isDark;
        applyTheme();
    }

    void applyTheme()
    {
        Color background, foreground, buttonBg, themeBg;
        if (isDarkMode) {
            background = Color.BLACK; foreground = Color.WHITE;
            buttonBg = new Color(30, 30, 30); themeBg = new Color(50, 50, 50);
            themeButton.setText("L");
        } else {
            background = Color.WHITE; foreground = Color.BLACK;
            buttonBg = new Color(230, 230, 230); themeBg = new Color(200, 200, 200);
            themeButton.setText("D");
        }
        this.getContentPane().setBackground(background);
        themeButton.setBackground(themeBg);
        themeButton.setForeground(foreground); themeButton.setBorder(null);
        derslikEkleButton.setBackground(buttonBg);
        derslikEkleButton.setForeground(foreground);
        derslikEkleButton.setBorder(BorderFactory.createLineBorder(foreground, 1));
        geriButton.setBackground(buttonBg);
        geriButton.setForeground(foreground);
        geriButton.setBorder(BorderFactory.createLineBorder(foreground, 1));
        scrollPane.getViewport().setBackground(background);
        scrollPane.setBorder(BorderFactory.createLineBorder(foreground, 2));
        derslikListPanel.setBackground(background);
    }

    void updateComponentBounds()
    {
        int w = this.getContentPane().getWidth(); int h = this.getContentPane().getHeight();
        float aw = (float) w / DEFAULT_WIDTH; float ah = (float) h / DEFAULT_HEIGHT;
        int fontSize = (int)(20 * Math.min(aw, ah));
        if (fontSize < 14) fontSize = 14;
        Font mainFont = new Font("Arial", Font.BOLD, fontSize);
        themeButton.setFont(mainFont);
        derslikEkleButton.setFont(mainFont);
        geriButton.setFont(mainFont);
        themeButton.setBounds((int)(1220 * aw), (int)(20 * ah), (int)(55 * aw), (int)(55 * ah));
        derslikEkleButton.setBounds((int)(20 * aw), (int)(80 * ah), (int)(280 * aw), (int)(45 * ah));
        geriButton.setBounds((int) (20 * aw), (int) (20 * ah), (int) (150 * aw), (int) (45 * ah));
        scrollPane.setBounds((int)(320 * aw), (int)(20 * ah), (int)(890 * aw), (int)(680 * ah));
        this.revalidate();
        this.repaint();
    }

    static class Derslik
    {
        String bolumAdi; String derslikKodu; String isim;
        int siraSayisiYatay; int siraSayisiDikey;
        int siraKapasitesi; // Fiziksel kapasite (örn: 4)
        int toplamKapasite; // Efektif kapasite (örn: 200)

        Derslik(String bolumAdi, String derslikKodu, String isim, int yatay, int dikey, int siraKap, int toplam)
        {
            this.bolumAdi = bolumAdi;
            this.derslikKodu = derslikKodu;
            this.isim = isim;
            this.siraSayisiYatay = yatay;
            this.siraSayisiDikey = dikey;
            this.siraKapasitesi = siraKap;
            this.toplamKapasite = toplam;
        }

        // Bu metot FİZİKSEL kapasiteyi döndürür (örn: 4)
        public int getSiraKapasitesi() {
            return this.siraKapasitesi;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new vis3Frame(true);
        });
    }
}