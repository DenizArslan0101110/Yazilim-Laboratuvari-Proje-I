package visualization;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;

public class vis33Frame extends JFrame {

    // --- Veritabanı bağlantısı ---
    String url = "jdbc:mysql://localhost:3306/muhendislik_fakulte?serverTimezone=UTC&useSSL=false";
    String user = "root";
    String pass = "yazlab1";

    final Timer resizeTimer;

    JButton themeButton;
    JButton geriButton;
    JButton olusturButton;

    // Sınav türü seçimi
    JLabel sinavTuruLabel;
    JComboBox<String> sinavTuruCombo;

    // Tarih girdileri
    JLabel baslangicLabel;
    JTextField baslangicField;
    JLabel bitisLabel;
    JTextField bitisField;
    JLabel tarihFormatLabel;

    // --- Yeni Bölümler ---
    JLabel sinavGunleriLabel;
    JPanel tarihSecimPanel;
    JScrollPane tarihSecimScroll;
    LinkedHashMap<LocalDate, JCheckBox> tarihCheckBoxes;

    JLabel istemeyenDerslerLabel;
    JPanel istemeyenDerslerPanel;
    JScrollPane istemeyenDerslerScroll;
    ArrayList<JCheckBox> dersCheckBoxes;
    JButton tumunuSecButton;
    JButton secimiKaldirButton;

    JLabel ozelSureLabel;
    JPanel ozelSurePanel;
    JScrollPane ozelSureScroll;
    LinkedHashMap<String, JSpinner> sureSpinnerMap;
    // --- Yeni Bölümler Sonu ---

    // Mola süresi
    JLabel molaLabel;
    JSpinner molaSpinner;

    // Varsayılan Sınav Süresi
    JLabel sinavSuresiLabel;
    JSpinner sinavSuresiSpinner;

    boolean isDarkMode;

    static final int DEFAULT_WIDTH = 1280;
    static final int DEFAULT_HEIGHT = 720;

    // --- KYS Değişkenleri (Güncellendi) ---
    static final int DAY_LENGTH = 480;
    static LocalDate restrict_starting_date;
    static LocalDate restrict_ending_date;
    static ArrayList<LocalDate> restrict_banned_days;
    static int restrict_type_of_exam;
    static int restrict_break_duration;
    static String restrict_department_name;
    static boolean restrict_multiple_exams_allowed;
    static float restrict_classroom_fill_percentage;
    static ArrayList<LocalDate> restrict_allowed_days;
    static int restrict_default_exam_duration;
    static ArrayList<String> restrict_banned_courses;
    static Map<String, Integer> restrict_specific_durations;
    // --- KYS Değişkenleri Sonu ---

    public vis33Frame(boolean isDarkMode) {
        this.isDarkMode = isDarkMode;

        this.setTitle("Sınav Kısıtlamaları");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(true);
        this.setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        this.setLayout(null);
        this.setLocationRelativeTo(null);

        resizeTimer = new Timer(100, e -> updateComponentBounds());
        resizeTimer.setRepeats(false);

        // Componentleri oluştur
        themeButton = new JButton(isDarkMode ? "L" : "D");
        geriButton = new JButton("Geri");

        sinavTuruLabel = new JLabel("Sınav Türü:");
        sinavTuruCombo = new JComboBox<>(new String[]{"Vize", "Final", "Büt"});
        sinavTuruCombo.setFont(new Font("Arial", Font.PLAIN, 14));

        baslangicLabel = new JLabel("Başlangıç Tarihi:");
        baslangicField = new JTextField("2025-01-13");
        baslangicField.setFont(new Font("Arial", Font.PLAIN, 14));

        bitisLabel = new JLabel("Bitiş Tarihi:");
        bitisField = new JTextField("2025-01-19");
        bitisField.setFont(new Font("Arial", Font.PLAIN, 14));

        tarihFormatLabel = new JLabel("(Format: YYYY-MM-DD)");
        tarihFormatLabel.setFont(new Font("Arial", Font.ITALIC, 11));

        // --- Yeni Bileşenlerin Oluşturulması ---
        sinavGunleriLabel = new JLabel("Sınav Günlerini Seçin:");
        tarihSecimPanel = new JPanel();
        tarihSecimPanel.setLayout(new BoxLayout(tarihSecimPanel, BoxLayout.Y_AXIS));
        tarihCheckBoxes = new LinkedHashMap<>();
        tarihSecimScroll = new JScrollPane(tarihSecimPanel);
        tarihSecimScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        tarihSecimScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        istemeyenDerslerLabel = new JLabel("Sınava Dahil Edilmeyecekler:");
        istemeyenDerslerPanel = new JPanel();
        istemeyenDerslerPanel.setLayout(new BoxLayout(istemeyenDerslerPanel, BoxLayout.Y_AXIS));
        dersCheckBoxes = new ArrayList<>();
        istemeyenDerslerScroll = new JScrollPane(istemeyenDerslerPanel);
        istemeyenDerslerScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        istemeyenDerslerScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        tumunuSecButton = new JButton("Tümünü Seç");
        secimiKaldirButton = new JButton("Seçimi Kaldır");

        ozelSureLabel = new JLabel("Özel Sınav Süreleri (dk):");
        ozelSurePanel = new JPanel();
        ozelSurePanel.setLayout(new BoxLayout(ozelSurePanel, BoxLayout.Y_AXIS));
        sureSpinnerMap = new LinkedHashMap<>();
        ozelSureScroll = new JScrollPane(ozelSurePanel);
        ozelSureScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        ozelSureScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        // --- Yeni Bileşenler Sonu ---

        molaLabel = new JLabel("Sınavlar Arası Mola (dakika):");
        SpinnerNumberModel molaModel = new SpinnerNumberModel(15, 0, 120, 5);
        molaSpinner = new JSpinner(molaModel);
        molaSpinner.setFont(new Font("Arial", Font.PLAIN, 14));

        sinavSuresiLabel = new JLabel("Varsayılan Süre (dk):");
        sinavSuresiSpinner = new JSpinner(new SpinnerNumberModel(75, 30, 240, 5));
        sinavSuresiSpinner.setFont(new Font("Arial", Font.PLAIN, 14));

        olusturButton = new JButton("Sınav Oluştur");
        olusturButton.setFont(new Font("Arial", Font.BOLD, 16));
        olusturButton.setFocusPainted(false);
        olusturButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        olusturButton.addActionListener(e -> {
            if (validateInputs()) {
                collectAndSendDataToKYS();
            }
        });


        // --- Frame'e Ekleme ---
        this.add(themeButton);
        this.add(geriButton);
        this.add(sinavTuruLabel);
        this.add(sinavTuruCombo);
        this.add(baslangicLabel);
        this.add(baslangicField);
        this.add(bitisLabel);
        this.add(bitisField);
        this.add(tarihFormatLabel);
        this.add(molaLabel);
        this.add(molaSpinner);
        this.add(olusturButton);
        this.add(sinavSuresiLabel);
        this.add(sinavSuresiSpinner);
        this.add(sinavGunleriLabel);
        this.add(tarihSecimScroll);
        this.add(istemeyenDerslerLabel);
        this.add(istemeyenDerslerScroll);
        this.add(tumunuSecButton);
        this.add(secimiKaldirButton);
        this.add(ozelSureLabel);
        this.add(ozelSureScroll);
        // --- Ekleme Sonu ---


        // --- Action Listeners ---
        geriButton.addActionListener(e -> {
            try {
                vis2Frame newScreen = new vis2Frame();
                newScreen.setDarkMode(this.isDarkMode);
                newScreen.setVisible(true);
                this.dispose();
            } catch (Exception ex) {
                handleException("Geri butonu hatası", ex);
                ex.printStackTrace();
            }
        });

        themeButton.addActionListener(e -> {
            this.isDarkMode = !this.isDarkMode;
            applyTheme();
        });

        DocumentListener tarihListener = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updateTarihSecimListesi(); }
            @Override public void removeUpdate(DocumentEvent e) { updateTarihSecimListesi(); }
            @Override public void changedUpdate(DocumentEvent e) { updateTarihSecimListesi(); }
        };
        baslangicField.getDocument().addDocumentListener(tarihListener);
        bitisField.getDocument().addDocumentListener(tarihListener);

        sinavSuresiSpinner.addChangeListener(e -> updateOzelSureSpinners());

        tumunuSecButton.addActionListener(e -> setAllDersCheckBoxes(true));
        secimiKaldirButton.addActionListener(e -> setAllDersCheckBoxes(false));


        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeTimer.restart();
            }
        });

        // --- İlk yüklemeler ---
        loadDersler();
        updateTarihSecimListesi();
        // updateOzelSurePanelVisibility(); // YENİ: Başlangıçta hepsi görünür olduğu için buna gerek yok.

        // İlk boyutlandırma ve tema
        updateComponentBounds();
        applyTheme();

        this.setVisible(true);
    }

    private boolean validateInputs() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        try {
            LocalDate b = LocalDate.parse(baslangicField.getText(), formatter);
            LocalDate f = LocalDate.parse(bitisField.getText(), formatter);
            if (f.isBefore(b)) { showError("Bitiş tarihi başlangıç tarihinden önce olamaz!"); return false; }
            if (tarihCheckBoxes.values().stream().noneMatch(JCheckBox::isSelected)) { showError("Lütfen sınav yapmak için en az bir gün seçin!"); return false; }
            return true;
        } catch (DateTimeParseException e) { showError("Lütfen tarihleri YYYY-MM-DD formatında girin!"); return false; }
    }


    private void applyTheme() {
        Color background, foreground, buttonBg, inputBg, inputFg;

        if (isDarkMode) {
            background = new Color(18, 18, 18);
            foreground = Color.WHITE;
            buttonBg = new Color(30, 30, 30);
            inputBg = new Color(40, 40, 40);
            inputFg = Color.WHITE;
            themeButton.setText("L");
        } else {
            background = Color.WHITE;
            foreground = Color.BLACK;
            buttonBg = new Color(230, 230, 230);
            inputBg = new Color(245, 245, 245);
            inputFg = Color.BLACK;
            themeButton.setText("D");
        }

        this.getContentPane().setBackground(background);

        // Butonlar
        geriButton.setBackground(buttonBg);
        geriButton.setForeground(foreground);
        geriButton.setBorder(BorderFactory.createLineBorder(foreground, 1));
        geriButton.setFocusPainted(false);

        themeButton.setBackground(isDarkMode ? new Color(50, 50, 50) : new Color(200, 200, 200));
        themeButton.setForeground(foreground);
        themeButton.setBorder(BorderFactory.createLineBorder(foreground, 1));
        themeButton.setFocusPainted(false);

        olusturButton.setBackground(new Color(40, 167, 69));
        olusturButton.setForeground(Color.WHITE);
        olusturButton.setBorder(BorderFactory.createLineBorder(new Color(34, 139, 58), 2));

        tumunuSecButton.setBackground(buttonBg);
        tumunuSecButton.setForeground(foreground);
        tumunuSecButton.setBorder(BorderFactory.createLineBorder(foreground, 1));
        tumunuSecButton.setFocusPainted(false);
        secimiKaldirButton.setBackground(buttonBg);
        secimiKaldirButton.setForeground(foreground);
        secimiKaldirButton.setBorder(BorderFactory.createLineBorder(foreground, 1));
        secimiKaldirButton.setFocusPainted(false);

        // Label'lar
        sinavTuruLabel.setForeground(foreground);
        baslangicLabel.setForeground(foreground);
        bitisLabel.setForeground(foreground);
        tarihFormatLabel.setForeground(isDarkMode ? new Color(150, 150, 150) : new Color(100, 100, 100));
        molaLabel.setForeground(foreground);
        sinavSuresiLabel.setForeground(foreground);
        sinavGunleriLabel.setForeground(foreground);
        istemeyenDerslerLabel.setForeground(foreground);
        ozelSureLabel.setForeground(foreground);

        // Input alanları
        sinavTuruCombo.setBackground(inputBg);
        sinavTuruCombo.setForeground(inputFg);

        baslangicField.setBackground(inputBg);
        baslangicField.setForeground(inputFg);
        baslangicField.setCaretColor(inputFg);

        bitisField.setBackground(inputBg);
        bitisField.setForeground(inputFg);
        bitisField.setCaretColor(inputFg);

        molaSpinner.getEditor().getComponent(0).setBackground(inputBg);
        molaSpinner.getEditor().getComponent(0).setForeground(inputFg);

        sinavSuresiSpinner.getEditor().getComponent(0).setBackground(inputBg);
        sinavSuresiSpinner.getEditor().getComponent(0).setForeground(inputFg);

        // --- Panel Temaları ---
        Color brdCol = isDarkMode ? new Color(50,50,50) : new Color(200,200,200);

        tarihSecimPanel.setBackground(background);
        for (JCheckBox cb : tarihCheckBoxes.values()) { cb.setBackground(background); cb.setForeground(foreground); }
        tarihSecimScroll.getViewport().setBackground(background);
        tarihSecimScroll.setBorder(BorderFactory.createLineBorder(brdCol));

        istemeyenDerslerPanel.setBackground(background);
        for (JCheckBox cb : dersCheckBoxes) { cb.setBackground(background); cb.setForeground(foreground); }
        istemeyenDerslerScroll.getViewport().setBackground(background);
        istemeyenDerslerScroll.setBorder(BorderFactory.createLineBorder(brdCol));

        ozelSurePanel.setBackground(background);
        for(Component comp : ozelSurePanel.getComponents()) {
            if (comp instanceof JPanel row) {
                row.setBackground(background);
                for(Component child : row.getComponents()) {
                    if (child instanceof JLabel label) label.setForeground(foreground);
                    else if (child instanceof JSpinner spinner) {
                        spinner.getEditor().getComponent(0).setBackground(inputBg);
                        spinner.getEditor().getComponent(0).setForeground(inputFg);
                    }
                }
            }
        }
        ozelSureScroll.getViewport().setBackground(background);
        ozelSureScroll.setBorder(BorderFactory.createLineBorder(brdCol));
        // --- Panel Temaları Sonu ---
    }


    void updateComponentBounds() {
        int w = this.getContentPane().getWidth();
        int h = this.getContentPane().getHeight();
        float aw = (float) w / DEFAULT_WIDTH;
        float ah = (float) h / DEFAULT_HEIGHT;
        int fontSize = (int) (16 * Math.min(aw, ah));
        int labelFontSize = (int) (14 * Math.min(aw, ah));
        int smallFontSize = (int) (12 * Math.min(aw, ah));
        int checkBoxFontSize = (int) (13 * Math.min(aw, ah));

        geriButton.setFont(new Font("Arial", Font.BOLD, fontSize));
        themeButton.setFont(new Font("Arial", Font.BOLD, fontSize));

        sinavTuruLabel.setFont(new Font("Arial", Font.BOLD, labelFontSize));
        baslangicLabel.setFont(new Font("Arial", Font.BOLD, labelFontSize));
        bitisLabel.setFont(new Font("Arial", Font.BOLD, labelFontSize));
        molaLabel.setFont(new Font("Arial", Font.BOLD, labelFontSize));

        sinavSuresiLabel.setFont(new Font("Arial", Font.BOLD, labelFontSize));
        sinavGunleriLabel.setFont(new Font("Arial", Font.BOLD, labelFontSize));
        istemeyenDerslerLabel.setFont(new Font("Arial", Font.BOLD, labelFontSize));
        ozelSureLabel.setFont(new Font("Arial", Font.BOLD, labelFontSize));

        tumunuSecButton.setFont(new Font("Arial", Font.PLAIN, smallFontSize));
        secimiKaldirButton.setFont(new Font("Arial", Font.PLAIN, smallFontSize));
        tarihFormatLabel.setFont(new Font("Arial", Font.ITALIC, smallFontSize));

        sinavTuruCombo.setFont(new Font("Arial", Font.PLAIN, fontSize));
        baslangicField.setFont(new Font("Arial", Font.PLAIN, fontSize));
        bitisField.setFont(new Font("Arial", Font.PLAIN, fontSize));
        molaSpinner.setFont(new Font("Arial", Font.PLAIN, fontSize));
        sinavSuresiSpinner.setFont(new Font("Arial", Font.PLAIN, fontSize));

        // Liste fontları
        for (JCheckBox cb : tarihCheckBoxes.values()) cb.setFont(new Font("Arial", Font.PLAIN, checkBoxFontSize));
        for (JCheckBox cb : dersCheckBoxes) cb.setFont(new Font("Arial", Font.PLAIN, checkBoxFontSize - 1));
        for(Component comp : ozelSurePanel.getComponents()) {
            if (comp instanceof JPanel row) {
                for(Component child : row.getComponents()) {
                    if (child instanceof JLabel label) label.setFont(new Font("Arial", Font.PLAIN, checkBoxFontSize - 1));
                    else if (child instanceof JSpinner spinner) spinner.setFont(new Font("Arial", Font.PLAIN, checkBoxFontSize - 1));
                }
            }
        }

        // --- Pozisyonlar ---
        geriButton.setBounds((int) (20 * aw), (int) (20 * ah), (int) (150 * aw), (int) (45 * ah));
        themeButton.setBounds((int) (1205 * aw), (int) (20 * ah), (int) (55 * aw), (int) (55 * ah));

        // --- SOL KOLON ---
        sinavTuruLabel.setBounds((int)(50 * aw), (int)(100 * ah), (int)(250 * aw), (int)(30 * ah));
        sinavTuruCombo.setBounds((int)(300 * aw), (int)(100 * ah), (int)(350 * aw), (int)(35 * ah));

        baslangicLabel.setBounds((int)(50 * aw), (int)(100 * ah) + (int)(70 * ah), (int)(250 * aw), (int)(30 * ah));
        baslangicField.setBounds((int)(300 * aw), (int)(100 * ah) + (int)(70 * ah), (int)(350 * aw), (int)(35 * ah));

        bitisLabel.setBounds((int)(50 * aw), (int)(100 * ah) + (int)(70 * ah) * 2, (int)(250 * aw), (int)(30 * ah));
        bitisField.setBounds((int)(300 * aw), (int)(100 * ah) + (int)(70 * ah) * 2, (int)(350 * aw), (int)(35 * ah));

        tarihFormatLabel.setBounds((int)(300 * aw), (int)(100 * ah) + (int)(70 * ah) * 2 + (int)(40 * ah), (int)(350 * aw), (int)(20 * ah));

        molaLabel.setBounds((int)(50 * aw), (int)(100 * ah) + (int)(70 * ah) * 3 + (int)(20 * ah), (int)(250 * aw), (int)(30 * ah));
        molaSpinner.setBounds((int)(300 * aw), (int)(100 * ah) + (int)(70 * ah) * 3 + (int)(20 * ah), (int)(150 * aw), (int)(35 * ah));

        sinavSuresiLabel.setBounds((int)(50 * aw), (int)(100 * ah) + (int)(70 * ah) * 4 + (int)(20 * ah), (int)(250 * aw), (int)(30 * ah));
        sinavSuresiSpinner.setBounds((int)(300 * aw), (int)(100 * ah) + (int)(70 * ah) * 4 + (int)(20 * ah), (int)(150 * aw), (int)(35 * ah));

        // --- SAĞ KOLON ---
        int sagKolonX = (int)(700 * aw);
        int sagKolonGenislik = (int)(450 * aw);

        sinavGunleriLabel.setBounds(sagKolonX, (int)(100 * ah), sagKolonGenislik, (int)(30 * ah));
        tarihSecimScroll.setBounds(sagKolonX, (int)(140 * ah), sagKolonGenislik, (int)(180 * ah));

        istemeyenDerslerLabel.setBounds(sagKolonX, (int)(330 * ah), sagKolonGenislik, (int)(30 * ah));
        istemeyenDerslerScroll.setBounds(sagKolonX, (int)(370 * ah), sagKolonGenislik, (int)(150 * ah));
        tumunuSecButton.setBounds(sagKolonX, (int)(525 * ah), (sagKolonGenislik / 2) - 5, (int)(30 * ah));
        secimiKaldirButton.setBounds(sagKolonX + (sagKolonGenislik / 2) + 5, (int)(525 * ah), (sagKolonGenislik / 2) - 5, (int)(30 * ah));

        ozelSureLabel.setBounds(sagKolonX, (int)(565 * ah), sagKolonGenislik, (int)(30 * ah));
        ozelSureScroll.setBounds(sagKolonX, (int)(600 * ah), sagKolonGenislik, (int)(100 * ah));

        olusturButton.setBounds((int)(450 * aw), (int)(600 * ah), (int)(200 * aw), (int)(60 * ah));

        this.revalidate();
        this.repaint();
    }


    // --- Yardımcı Metodlar ---

    // *** YENİ METOD ***
    /**
     * "İstenmeyen Dersler" listesindeki seçimlere göre
     * "Özel Sınav Süreleri" panelindeki satırların görünürlüğünü günceller.
     */
    private void updateOzelSurePanelVisibility() {
        // Özel süre panelindeki tüm bileşenleri (satırları) al
        Component[] sureRows = ozelSurePanel.getComponents();

        // İki listenin boyutunun eşleştiğinden emin ol
        if (sureRows.length != dersCheckBoxes.size()) {
            System.err.println("Hata: Ders listesi (" + dersCheckBoxes.size() +
                    ") ile süre listesi (" + sureRows.length +
                    ") senkronize değil!");
            return;
        }

        // Listeleri döngüye al
        for (int i = 0; i < dersCheckBoxes.size(); i++) {
            JCheckBox cb = dersCheckBoxes.get(i);
            Component row = sureRows[i];

            // Eğer checkbox SEÇİLİ ise (ders istenmiyorsa)
            if (cb.isSelected()) {
                row.setVisible(false); // Süre satırını gizle
            } else {
                row.setVisible(true); // Süre satırını göster
            }
        }

        // Panel düzenini yenile
        ozelSurePanel.revalidate();
        ozelSurePanel.repaint();
        // Kaydırma çubuğunu da yenilemek iyi bir fikir
        ozelSureScroll.revalidate();
        ozelSureScroll.repaint();
    }
    // *** YENİ METOD SONU ***


    /**
     * Toplu CheckBox seçimi (*** GÜNCELLENDİ ***)
     */
    private void setAllDersCheckBoxes(boolean selected) {
        for(JCheckBox cb : dersCheckBoxes) {
            cb.setSelected(selected);
        }
        // *** YENİ EKLENDİ ***
        // Toplu seçim sonrası süre panelini de güncelle
        updateOzelSurePanelVisibility();
        // *** YENİ EKLENDİ SONU ***
    }

    // KYS'ye veri gönderme
    private void collectAndSendDataToKYS() {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            restrict_starting_date = LocalDate.parse(baslangicField.getText(), formatter);
            restrict_ending_date = LocalDate.parse(bitisField.getText(), formatter);

            String sinavTuru = (String) sinavTuruCombo.getSelectedItem();
            restrict_type_of_exam = sinavTuru.equals("Vize") ? 0 : (sinavTuru.equals("Final") ? 1 : 2);

            restrict_break_duration = (Integer) molaSpinner.getValue();
            restrict_default_exam_duration = (Integer) sinavSuresiSpinner.getValue();

            // İzinli ve yasaklı günleri al
            restrict_allowed_days = new ArrayList<>();
            restrict_banned_days = new ArrayList<>();
            for (Map.Entry<LocalDate, JCheckBox> entry : tarihCheckBoxes.entrySet()) {
                if (entry.getValue().isSelected()) restrict_allowed_days.add(entry.getKey());
                else restrict_banned_days.add(entry.getKey());
            }

            // Yasaklı dersleri al
            restrict_banned_courses = new ArrayList<>();
            for (JCheckBox cb : dersCheckBoxes) {
                if (cb.isSelected()) restrict_banned_courses.add(cb.getName());
            }

            // Özel süreleri al
            restrict_specific_durations = new LinkedHashMap<>();
            int defaultDuration = restrict_default_exam_duration;
            for (Map.Entry<String, JSpinner> entry : sureSpinnerMap.entrySet()) {
                // *** YENİ KONTROL ***
                // Sadece "istenmeyen" olarak İŞARETLENMEMİŞ dersleri dikkate al
                JCheckBox ilgiliCheckBox = dersCheckBoxes.stream()
                        .filter(cb -> cb.getName().equals(entry.getKey()))
                        .findFirst().orElse(null);

                // Eğer checkbox bulunursa VE seçili DEĞİLSE (yani sınava dahilse)
                if (ilgiliCheckBox != null && !ilgiliCheckBox.isSelected()) {
                    int spinnerValue = (Integer) entry.getValue().getValue();
                    if (spinnerValue != defaultDuration) {
                        restrict_specific_durations.put(entry.getKey(), spinnerValue);
                    }
                }
                // *** KONTROL SONU ***
            }

            // Diğer KYS değişkenleri
            restrict_multiple_exams_allowed = false;
            restrict_classroom_fill_percentage = 0.5f;
            restrict_department_name = "bilgisayar";

            // Bilgi mesajı
            String message = String.format(
                    "Kısıtlamalar Ayarlandı:\n\n" +
                            "Tür: %s\nTarih Aralığı: %s / %s\n" +
                            "Varsayılan Süre: %d dk\nÖzel Süre Ayarlanan Ders: %d adet\n" +
                            "Mola Süresi: %d dk\nİzinli Gün Sayısı: %d\n" +
                            "Yasaklı Ders Sayısı: %d",
                    sinavTuru, restrict_starting_date, restrict_ending_date,
                    restrict_default_exam_duration, restrict_specific_durations.size(),
                    restrict_break_duration, restrict_allowed_days.size(),
                    restrict_banned_courses.size()
            );
            JOptionPane.showMessageDialog(this, message, "Kısıtlamalar Uygulandı", JOptionPane.INFORMATION_MESSAGE);

            // KYS Sınıfını güncelle
            KYS.restrict_starting_date = restrict_starting_date;
            KYS.restrict_ending_date = restrict_ending_date;
            KYS.restrict_banned_days = restrict_banned_days;
            KYS.restrict_allowed_days = restrict_allowed_days;
            KYS.restrict_type_of_exam = restrict_type_of_exam;
            KYS.restrict_break_duration = restrict_break_duration;
            KYS.restrict_department_name = restrict_department_name;
            KYS.restrict_default_exam_duration = restrict_default_exam_duration;
            KYS.restrict_banned_courses = restrict_banned_courses;
            KYS.restrict_specific_durations = restrict_specific_durations;

            KYS.MikeRebuildMyKids();
            KYS.MakeTheDamnProgramAlready();
            KYS.PrintProgramToExcel();

        } catch (Exception ex) {
            handleException("Kısıtlamalar ayarlanırken veya KYS çalıştırılırken hata", ex);
        }
    }

    /**
     * Dersleri DB'den yükle (*** GÜNCELLENDİ ***)
     */
    private void loadDersler() {
        istemeyenDerslerPanel.removeAll(); ozelSurePanel.removeAll();
        dersCheckBoxes.clear(); sureSpinnerMap.clear();
        String sql = "SELECT kod, isim FROM ders ORDER BY isim";
        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            int defaultDuration = (Integer) sinavSuresiSpinner.getValue();
            while (rs.next()) {
                String kod = rs.getString("kod"); String isim = rs.getString("isim"); String dersText = isim + " (" + kod + ")";

                // İstenmeyen Dersler CheckBox
                JCheckBox cb = new JCheckBox(dersText); cb.setName(kod); cb.setFocusPainted(false);

                // *** YENİ EKLENDİ ***
                // Checkbox'a tıklandığında süre panelini güncelleyen listener
                cb.addActionListener(e -> updateOzelSurePanelVisibility());
                // *** YENİ EKLENDİ SONU ***

                istemeyenDerslerPanel.add(cb); dersCheckBoxes.add(cb);

                // Özel Süre Ayarı Paneli
                JPanel sureRowPanel = new JPanel(new BorderLayout(5, 0)); sureRowPanel.setOpaque(false); sureRowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
                JLabel dersLabel = new JLabel(dersText);
                JSpinner spinner = new JSpinner(new SpinnerNumberModel(defaultDuration, 30, 240, 5)); spinner.setPreferredSize(new Dimension(70, 25));
                sureRowPanel.add(dersLabel, BorderLayout.CENTER); sureRowPanel.add(spinner, BorderLayout.EAST);
                ozelSurePanel.add(sureRowPanel); sureSpinnerMap.put(kod, spinner);
            }
        } catch (Exception e) { handleException("Dersler yüklenirken hata", e); }

        istemeyenDerslerPanel.revalidate(); istemeyenDerslerPanel.repaint();
        ozelSurePanel.revalidate(); ozelSurePanel.repaint();
    }

    // Özel süre spinnerlarını güncelle
    private void updateOzelSureSpinners() {
        int newDefault = (Integer) sinavSuresiSpinner.getValue();
        for(JSpinner spinner : sureSpinnerMap.values()) { spinner.setValue(newDefault); }
    }

    // Tarih aralığına göre gün seçimi listesini güncelle
    private void updateTarihSecimListesi() {
        tarihSecimPanel.removeAll(); tarihCheckBoxes.clear();
        DateTimeFormatter inputFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter outputFmt = DateTimeFormatter.ofPattern("dd MMMM yyyy, EEEE", new Locale("tr"));
        try {
            LocalDate baslangic = LocalDate.parse(baslangicField.getText(), inputFmt);
            LocalDate bitis = LocalDate.parse(bitisField.getText(), inputFmt);
            if (bitis.isBefore(baslangic)) { tarihSecimPanel.add(new JLabel("Bitiş tarihi geçersiz.")); }
            else {
                for (LocalDate date = baslangic; !date.isAfter(bitis); date = date.plusDays(1)) {
                    JCheckBox cb = new JCheckBox(date.format(outputFmt)); cb.setSelected(true); cb.setFocusPainted(false);
                    tarihSecimPanel.add(cb); tarihSecimPanel.add(Box.createRigidArea(new Dimension(0, 5)));
                    tarihCheckBoxes.put(date, cb);
                }
            }
        } catch (DateTimeParseException e) { tarihSecimPanel.add(new JLabel("Tarih formatı geçersiz. (YYYY-MM-DD)")); }

        applyTheme();
        updateComponentBounds();

        tarihSecimPanel.revalidate(); tarihSecimPanel.repaint();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Hata", JOptionPane.ERROR_MESSAGE);
    }

    private void handleException(String context, Exception ex) {
        ex.printStackTrace();
        showError(context + ": " + ex.getMessage());
    }
}