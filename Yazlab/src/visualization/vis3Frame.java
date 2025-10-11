package visualization;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;

public class vis3Frame extends JFrame {

    // GÖRSEL BİLEŞENLER
    JButton themeButton;
    JButton derslikEkleButton;
    JButton geriButton;
    JPanel derslikListPanel;
    JScrollPane scrollPane;

    // Derslik listesi
    ArrayList<Derslik> derslikler = new ArrayList<>();

    // Tema Durumu
    boolean isDarkMode;

    // Timer
    final Timer resizeTimer;

    // REFERANS BOYUTLAR
    static final int DEFAULT_WIDTH = 1280;
    static final int DEFAULT_HEIGHT = 720;

    public vis3Frame(boolean isDarkMode) {

        // FRAME AYARLARI
        this.setTitle("Derslik Yönetimi");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(true);
        this.setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        this.setLayout(null);
        this.isDarkMode = isDarkMode;
        this.setLocationRelativeTo(null);

        // TIMER
        resizeTimer = new Timer(100, e -> updateComponentBounds());
        resizeTimer.setRepeats(false);

        // BİLEŞEN OLUŞTURMA
        themeButton = new JButton(isDarkMode ? "L" : "D");
        derslikEkleButton = new JButton("Derslik Ekle");
        geriButton = new JButton("Geri");

        // SCROLL PANE İÇİN PANEL
        derslikListPanel = new JPanel();
        derslikListPanel.setLayout(new BoxLayout(derslikListPanel, BoxLayout.Y_AXIS));

        scrollPane = new JScrollPane(derslikListPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        this.add(themeButton);
        this.add(derslikEkleButton);
        this.add(geriButton);
        this.add(scrollPane);

        // DİNAMİK BOYUTLANDIRMA
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeTimer.restart();
            }
        });

        applyTheme();
        updateComponentBounds();

        // THEME BUTON
        themeButton.addActionListener(e -> {
            this.isDarkMode = !isDarkMode;
            applyTheme();
            // Derslik panel temalarını da güncelle
            for (Component comp : derslikListPanel.getComponents()) {
                if (comp instanceof JPanel) {
                    derslikPanelUpdateTheme((JPanel) comp);
                }
            }
        });

        // DERSLİK EKLE BUTON
        derslikEkleButton.addActionListener(e -> {
            showDerslikEkleDialog();
        });

        // GERİ BUTON
        geriButton.addActionListener(e -> {
            vis2Frame newScreen = new vis2Frame();
            newScreen.setDarkMode(this.isDarkMode);
            newScreen.setVisible(true);
            this.dispose();
        });

        // GÖSTER
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

    void showDerslikEkleDialog() {
        JDialog dialog = new JDialog(this, "Derslik Ekle", true);
        dialog.setSize(450, 450);
        dialog.setLayout(null);
        dialog.setLocationRelativeTo(this);

        // Bileşenler
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

        JLabel siraKapasitesiLabel = new JLabel("Sıra Kapasitesi:");
        JTextField siraKapasitesiField = new JTextField();

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

        kaydetButton.setBounds(120, 320, 100, 35);
        iptalButton.setBounds(240, 320, 100, 35);

        // Dialog'a ekle
        dialog.add(bolumAdiLabel);
        dialog.add(bolumAdiField);
        dialog.add(derslikKoduLabel);
        dialog.add(derslikKoduField);
        dialog.add(isimLabel);
        dialog.add(isimField);
        dialog.add(siraSayisiYatayLabel);
        dialog.add(siraSayisiYatayField);
        dialog.add(siraSayisiDikeyLabel);
        dialog.add(siraSayisiDikeyField);
        dialog.add(siraKapasitesiLabel);
        dialog.add(siraKapasitesiField);
        dialog.add(toplamKapasiteLabel);
        dialog.add(kaydetButton);
        dialog.add(iptalButton);

        // Tema uygula
        Color background;
        Color foreground;
        Color buttonBg;

        if (isDarkMode) {
            background = Color.BLACK;
            foreground = Color.WHITE;
            buttonBg = new Color(30, 30, 30);
        } else {
            background = Color.WHITE;
            foreground = Color.BLACK;
            buttonBg = new Color(230, 230, 230);
        }
        Color fieldBg = Color.WHITE;
        Color fieldFg = Color.BLACK;

        // Alan renkleri
        dialog.getContentPane().setBackground(background);
        bolumAdiLabel.setForeground(foreground);
        derslikKoduLabel.setForeground(foreground);
        isimLabel.setForeground(foreground);
        siraSayisiYatayLabel.setForeground(foreground);
        siraSayisiDikeyLabel.setForeground(foreground);
        siraKapasitesiLabel.setForeground(foreground);
        toplamKapasiteLabel.setForeground(foreground);

        bolumAdiField.setBackground(fieldBg);
        bolumAdiField.setForeground(fieldFg);
        derslikKoduField.setBackground(fieldBg);
        derslikKoduField.setForeground(fieldFg);
        isimField.setBackground(fieldBg);
        isimField.setForeground(fieldFg);
        siraSayisiYatayField.setBackground(fieldBg);
        siraSayisiYatayField.setForeground(fieldFg);
        siraSayisiDikeyField.setBackground(fieldBg);
        siraSayisiDikeyField.setForeground(fieldFg);
        siraKapasitesiField.setBackground(fieldBg);
        siraKapasitesiField.setForeground(fieldFg);

        // Buton renkleri
        kaydetButton.setBackground(new Color(60, 150, 255));
        kaydetButton.setForeground(Color.WHITE);
        iptalButton.setBackground(buttonBg);
        iptalButton.setForeground(foreground);

        // Toplam kapasite hesaplama (dinamik)
        Runnable hesaplaKapasite = () -> {
            try {
                int yatay = Integer.parseInt(siraSayisiYatayField.getText().trim());
                int dikey = Integer.parseInt(siraSayisiDikeyField.getText().trim());
                int siraKap = Integer.parseInt(siraKapasitesiField.getText().trim());
                int toplam = yatay * dikey * siraKap;
                toplamKapasiteLabel.setText("Toplam Kapasite: " + toplam);
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

        // KAYDET BUTON
        kaydetButton.addActionListener(e -> {
            String bolumAdi = bolumAdiField.getText().trim();
            String derslikKodu = derslikKoduField.getText().trim();
            String isim = isimField.getText().trim();
            String yatayStr = siraSayisiYatayField.getText().trim();
            String dikeyStr = siraSayisiDikeyField.getText().trim();
            String siraKapStr = siraKapasitesiField.getText().trim();

            if (bolumAdi.isEmpty() || derslikKodu.isEmpty() || isim.isEmpty() || yatayStr.isEmpty() || dikeyStr.isEmpty() || siraKapStr.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Lütfen tüm alanları doldurun!", "Hata", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                int yatay = Integer.parseInt(yatayStr);
                int dikey = Integer.parseInt(dikeyStr);
                int siraKap = Integer.parseInt(siraKapStr);

                if (yatay <= 0 || dikey <= 0 || siraKap <= 0) {
                    JOptionPane.showMessageDialog(dialog, "Tüm değerler pozitif olmalıdır!", "Hata", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                int toplamKapasite = yatay * dikey * siraKap;

                // DERSLİĞİ EKLE
                Derslik yeniDerslik = new Derslik(bolumAdi, derslikKodu, isim, yatay, dikey, siraKap, toplamKapasite);
                derslikler.add(yeniDerslik);
                derslikEkleScrollPane(yeniDerslik);

                JOptionPane.showMessageDialog(dialog,
                        "Derslik başarıyla eklendi!\nBölüm: " + bolumAdi + "\nDerslik Kodu: " + derslikKodu + "\nİsim: " + isim + "\nToplam Kapasite: " + toplamKapasite,
                        "Başarılı", JOptionPane.INFORMATION_MESSAGE);

                dialog.dispose();

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Tüm değerler geçerli sayı olmalıdır!", "Hata", JOptionPane.ERROR_MESSAGE);
            }
        });

        // İPTAL BUTON
        iptalButton.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    void derslikEkleScrollPane(Derslik derslik) {
        JPanel derslikPanel = new JPanel();
        derslikPanel.setLayout(new BorderLayout());
        derslikPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        derslikPanel.setPreferredSize(new Dimension(0, 80));

        String bilgi = String.format("<html>Bölüm: %s | Derslik Kodu: %s<br/>İsim: %s | Kapasite: %d (%dx%d sıra, her sırada %d kişi)</html>",
                derslik.bolumAdi, derslik.derslikKodu, derslik.isim, derslik.toplamKapasite,
                derslik.siraSayisiYatay, derslik.siraSayisiDikey, derslik.siraKapasitesi);

        JLabel bilgiLabel = new JLabel(bilgi);
        bilgiLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        bilgiLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // Buton paneli
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 10));

        JButton gorselButton = new JButton("Görüntüle");
        JButton silButton = new JButton("Sil");

        gorselButton.setPreferredSize(new Dimension(90, 40));
        silButton.setPreferredSize(new Dimension(70, 40));

        gorselButton.addActionListener(e -> {
            showDerslikGorsel(derslik);
        });

        silButton.addActionListener(e -> {
            derslikler.remove(derslik);
            derslikListPanel.remove(derslikPanel);
            derslikListPanel.revalidate();
            derslikListPanel.repaint();
        });

        buttonPanel.add(gorselButton);
        buttonPanel.add(silButton);

        derslikPanel.add(bilgiLabel, BorderLayout.CENTER);
        derslikPanel.add(buttonPanel, BorderLayout.EAST);

        // Tema uygula
        applyDerslikPanelTheme(derslikPanel, bilgiLabel, buttonPanel, gorselButton, silButton);

        derslikListPanel.add(derslikPanel);
        derslikListPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        derslikListPanel.revalidate();
        derslikListPanel.repaint();
    }

    // Tema değişiminde derslik panellerini güncellemek için eklenen metot
    void derslikPanelUpdateTheme(JPanel parentPanel) {
        if (!(parentPanel.getLayout() instanceof BorderLayout)) return;

        Component center = ((BorderLayout)parentPanel.getLayout()).getLayoutComponent(BorderLayout.CENTER);
        Component east = ((BorderLayout)parentPanel.getLayout()).getLayoutComponent(BorderLayout.EAST);

        if (center instanceof JLabel && east instanceof JPanel) {
            JLabel label = (JLabel) center;
            JPanel buttonPanel = (JPanel) east;

            // Butonları bul
            JButton btn1 = null, btn2 = null;
            if (buttonPanel.getComponentCount() >= 2) {
                btn1 = (JButton) buttonPanel.getComponent(0);
                btn2 = (JButton) buttonPanel.getComponent(1);
            }

            if (btn1 != null && btn2 != null) {
                // Temayı yeniden uygula
                applyDerslikPanelTheme(parentPanel, label, buttonPanel, btn1, btn2);
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

                int panelW = getWidth();
                int panelH = getHeight();

                // Tek bir "hücrenin" (bir masa + bir boşluk) kaplayacağı maksimum alanı hesaplıyoruz.
                double hucreGenisligi = panelW * 0.9 / derslik.siraSayisiYatay;
                double hucreYuksekligi = panelH * 0.9 / derslik.siraSayisiDikey;

                // Hücrenin bir kısmını masaya, bir kısmını boşluğa ayırıyoruz.
                int masaW = (int)(hucreGenisligi * 0.8);
                int boslukX = (int)(hucreGenisligi * 0.2);
                int masaH = (int)(hucreYuksekligi * 0.8);
                int boslukY = (int)(hucreYuksekligi * 0.2);

                // Minimum boyut kontrolü
                if (masaW < 5) masaW = 5;
                if (masaH < 5) masaH = 5;

                int toplamW = (derslik.siraSayisiYatay * masaW) + ((derslik.siraSayisiYatay - 1) * boslukX);
                int toplamH = (derslik.siraSayisiDikey * masaH) + ((derslik.siraSayisiDikey - 1) * boslukY);

                int startX = (panelW - toplamW) / 2;
                int startY = (panelH - toplamH) / 2;

                // Yazı boyutunu dinamik hale getir
                int fontSize = Math.max(8, Math.min(masaW / 4, masaH / 3));
                g.setFont(new Font("Arial", Font.BOLD, fontSize));

                // Masaları çiz
                for (int i = 0; i < derslik.siraSayisiDikey; i++) {
                    for (int j = 0; j < derslik.siraSayisiYatay; j++) {
                        int x = startX + (j * (masaW + boslukX));
                        int y = startY + (i * (masaH + boslukY));

                        // Masa rengi
                        g.setColor(new Color(150, 75, 0));
                        g.fillRect(x, y, masaW, masaH);

                        // Çerçeve ve yazı rengi
                        if(isDarkMode){
                            g.setColor(Color.BLACK);
                        } else {
                            g.setColor(Color.WHITE);
                        }
                        g.drawRect(x, y, masaW, masaH);

                        // Kapasite yazısı
                        String kapasite = String.valueOf(derslik.siraKapasitesi);
                        FontMetrics fm = g.getFontMetrics();
                        int textW = fm.stringWidth(kapasite);
                        int textH = fm.getAscent();

                        g.drawString(kapasite, x + (masaW - textW) / 2, y + (masaH + textH) / 2);
                    }
                }
            }
        };

        // Panel arka planını temaya göre ayarla
        if(isDarkMode){
            masaPanel.setBackground(Color.BLACK);
        } else {
            masaPanel.setBackground(Color.WHITE);
        }

        // Kapatma butonu
        JButton kapat = new JButton("Kapat");
        kapat.addActionListener(e -> gorselFrame.dispose());

        gorselFrame.add(masaPanel, BorderLayout.CENTER);
        gorselFrame.add(kapat, BorderLayout.SOUTH);

        gorselFrame.setVisible(true);
    }

    void applyDerslikPanelTheme(JPanel panel, JLabel label, JPanel buttonPanel, JButton btn1, JButton btn2) {
        Color panelBg;
        Color foreground;
        Color buttonBg1;
        Color buttonBg2;

        if (isDarkMode) {
            panelBg = new Color(30, 30, 30);
            foreground = Color.WHITE;
            buttonBg1 = new Color(50, 100, 150);
            buttonBg2 = new Color(180, 50, 50);
        } else {
            panelBg = new Color(240, 240, 240);
            foreground = Color.BLACK;
            buttonBg1 = new Color(100, 150, 255);
            buttonBg2 = new Color(255, 100, 100);
        }

        panel.setBackground(panelBg);
        panel.setBorder(BorderFactory.createLineBorder(foreground, 1));
        label.setForeground(foreground);
        buttonPanel.setBackground(panelBg);

        btn1.setBackground(buttonBg1);
        btn1.setForeground(Color.WHITE);

        btn2.setBackground(buttonBg2);
        btn2.setForeground(Color.WHITE);
    }

    void applyTheme() {
        Color background, foreground, buttonBg, themeBg;

        if (isDarkMode) {
            background = Color.BLACK;
            foreground = Color.WHITE;
            buttonBg = new Color(30, 30, 30);
            themeBg = new Color(50, 50, 50);
            themeButton.setText("L");
        } else {
            background = Color.WHITE;
            foreground = Color.BLACK;
            buttonBg = new Color(230, 230, 230);
            themeBg = new Color(200, 200, 200);
            themeButton.setText("D");
        }

        this.getContentPane().setBackground(background);

        themeButton.setBackground(themeBg);
        themeButton.setForeground(foreground);
        themeButton.setBorder(null);

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

    void updateComponentBounds() {
        int w = this.getContentPane().getWidth();
        int h = this.getContentPane().getHeight();

        float aw = (float) w / DEFAULT_WIDTH;
        float ah = (float) h / DEFAULT_HEIGHT;

        int fontSize = (int)(20 * Math.min(aw, ah));
        if (fontSize < 14) fontSize = 14;

        Font mainFont = new Font("Arial", Font.BOLD, fontSize);

        themeButton.setFont(mainFont);
        derslikEkleButton.setFont(mainFont);
        geriButton.setFont(mainFont);

        themeButton.setBounds((int)(1220 * aw), (int)(20 * ah), (int)(55 * aw), (int)(55 * ah));
        derslikEkleButton.setBounds((int)(20 * aw), (int)(20 * ah), (int)(280 * aw), (int)(45 * ah));
        geriButton.setBounds((int)(20 * aw), (int)(80 * ah), (int)(280 * aw), (int)(45 * ah));
        scrollPane.setBounds((int)(320 * aw), (int)(20 * ah), (int)(940 * aw), (int)(680 * ah));

        this.revalidate();
        this.repaint();
    }

    class Derslik {
        String bolumAdi;
        String derslikKodu;
        String isim;
        int siraSayisiYatay;
        int siraSayisiDikey;
        int siraKapasitesi;
        int toplamKapasite;

        Derslik(String bolumAdi, String derslikKodu, String isim, int yatay, int dikey, int siraKap, int toplam) {
            this.bolumAdi = bolumAdi;
            this.derslikKodu = derslikKodu;
            this.isim = isim;
            this.siraSayisiYatay = yatay;
            this.siraSayisiDikey = dikey;
            this.siraKapasitesi = siraKap;
            this.toplamKapasite = toplam;
        }
    }
}