package visualization;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;

public class vis33Frame extends JFrame {

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

    // Yasaklı günler
    JLabel yasakliGunlerLabel;
    JPanel yasakliGunlerPanel;
    JScrollPane yasakliScroll;
    ArrayList<JCheckBox> gunCheckBoxes;

    // Mola süresi
    JLabel molaLabel;
    JSpinner molaSpinner;

    boolean isDarkMode;

    static final int DEFAULT_WIDTH = 1280;
    static final int DEFAULT_HEIGHT = 720;

    // Resimdeki değişkenler
    static final int DAY_LENGTH = 480; // dakika cinsinden
    static LocalDate restrict_starting_date;
    static LocalDate restrict_ending_date;
    static ArrayList<LocalDate> restrict_banned_days;
    static int restrict_type_of_exam; // 0: Vize, 1: Final, 2: Büt
    static int restrict_break_duration;
    static boolean restrict_multiple_exams_allowed;
    static float restrict_classroom_fill_percentage;
    static ArrayList<LocalDate> restrict_allowed_days;

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

        // Sınav türü
        sinavTuruLabel = new JLabel("Sınav Türü:");
        sinavTuruCombo = new JComboBox<>(new String[]{"Vize", "Final", "Büt"});
        sinavTuruCombo.setFont(new Font("Arial", Font.PLAIN, 14));

        // Başlangıç tarihi
        baslangicLabel = new JLabel("Sınav Haftası Başlangıç:");
        baslangicField = new JTextField("2025-01-13");
        baslangicField.setFont(new Font("Arial", Font.PLAIN, 14));

        // Bitiş tarihi
        bitisLabel = new JLabel("Sınav Haftası Bitiş:");
        bitisField = new JTextField("2025-01-19");
        bitisField.setFont(new Font("Arial", Font.PLAIN, 14));

        // Format açıklaması
        tarihFormatLabel = new JLabel("(Format: YYYY-MM-DD)");
        tarihFormatLabel.setFont(new Font("Arial", Font.ITALIC, 11));

        // Yasaklı günler
        yasakliGunlerLabel = new JLabel("Sınava Dahil Olmayan Günler:");
        yasakliGunlerPanel = new JPanel();
        yasakliGunlerPanel.setLayout(new BoxLayout(yasakliGunlerPanel, BoxLayout.Y_AXIS));

        gunCheckBoxes = new ArrayList<>();
        String[] gunler = {"Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar"};
        for (String gun : gunler) {
            JCheckBox cb = new JCheckBox(gun);
            cb.setFont(new Font("Arial", Font.PLAIN, 13));
            cb.setFocusPainted(false);
            gunCheckBoxes.add(cb);
            yasakliGunlerPanel.add(cb);
            yasakliGunlerPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        }

        yasakliScroll = new JScrollPane(yasakliGunlerPanel);
        yasakliScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        yasakliScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Mola süresi
        molaLabel = new JLabel("Sınavlar Arası Mola (dakika):");
        SpinnerNumberModel molaModel = new SpinnerNumberModel(15, 0, 120, 5);
        molaSpinner = new JSpinner(molaModel);
        molaSpinner.setFont(new Font("Arial", Font.PLAIN, 14));

        // Oluştur butonu
        olusturButton = new JButton("Sınav Oluştur");
        olusturButton.setFont(new Font("Arial", Font.BOLD, 16));
        olusturButton.setFocusPainted(false);
        olusturButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        olusturButton.addActionListener(e -> {
            if (validateInputs()) {
                try {
                    // Static değişkenleri doldur
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    restrict_starting_date = LocalDate.parse(baslangicField.getText(), formatter);
                    restrict_ending_date = LocalDate.parse(bitisField.getText(), formatter);

                    // Sınav türünü integer'a çevir
                    String sinavTuru = (String) sinavTuruCombo.getSelectedItem();
                    restrict_type_of_exam = sinavTuru.equals("Vize") ? 0 : (sinavTuru.equals("Final") ? 1 : 2);

                    // Mola süresini al
                    restrict_break_duration = (Integer) molaSpinner.getValue();

                    // Yasaklı günleri LocalDate olarak al
                    restrict_banned_days = new ArrayList<>();
                    ArrayList<Integer> secilenGunIndeksleri = new ArrayList<>();
                    for (int i = 0; i < gunCheckBoxes.size(); i++) {
                        if (gunCheckBoxes.get(i).isSelected()) {
                            secilenGunIndeksleri.add(i + 1); // 1=Pazartesi, 7=Pazar
                        }
                    }

                    // Başlangıç ve bitiş arasındaki tüm günleri kontrol et
                    LocalDate current = restrict_starting_date;
                    while (!current.isAfter(restrict_ending_date)) {
                        int dayOfWeek = current.getDayOfWeek().getValue(); // 1=Pazartesi, 7=Pazar
                        if (secilenGunIndeksleri.contains(dayOfWeek)) {
                            restrict_banned_days.add(current);
                        }
                        current = current.plusDays(1);
                    }

                    // İzin verilen günleri hesapla
                    restrict_allowed_days = new ArrayList<>();
                    current = restrict_starting_date;
                    while (!current.isAfter(restrict_ending_date)) {
                        if (!restrict_banned_days.contains(current)) {
                            restrict_allowed_days.add(current);
                        }
                        current = current.plusDays(1);
                    }

                    // Varsayılan değerler
                    restrict_multiple_exams_allowed = false;
                    restrict_classroom_fill_percentage = 1.0f;

                    // Bilgi mesajı
                    String message = "Kısıtlamalar Ayarlandı:\n\n";
                    message += "Tür: " + sinavTuru + " (kod: " + restrict_type_of_exam + ")\n";
                    message += "Başlangıç: " + restrict_starting_date + "\n";
                    message += "Bitiş: " + restrict_ending_date + "\n";
                    message += "Mola Süresi: " + restrict_break_duration + " dakika\n";
                    message += "Yasaklı Gün Sayısı: " + restrict_banned_days.size() + "\n";
                    message += "İzinli Gün Sayısı: " + restrict_allowed_days.size() + "\n";
                    message += "Gün Uzunluğu: " + DAY_LENGTH + " dakika";

                    JOptionPane.showMessageDialog(this,
                            message,
                            "Kısıtlamalar Uygulandı",
                            JOptionPane.INFORMATION_MESSAGE);

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this,
                            "Kısıtlamalar ayarlanırken hata: " + ex.getMessage(),
                            "Hata",
                            JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            }
        });

        this.add(themeButton);
        this.add(geriButton);
        this.add(sinavTuruLabel);
        this.add(sinavTuruCombo);
        this.add(baslangicLabel);
        this.add(baslangicField);
        this.add(bitisLabel);
        this.add(bitisField);
        this.add(tarihFormatLabel);
        this.add(yasakliGunlerLabel);
        this.add(yasakliScroll);
        this.add(molaLabel);
        this.add(molaSpinner);
        this.add(olusturButton);

        // Action Listeners
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

        // Component resize listener
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeTimer.restart();
            }
        });

        // İlk boyutlandırma ve tema
        updateComponentBounds();
        applyTheme();

        this.setVisible(true);
    }

    private boolean validateInputs() {
        // Tarih formatı kontrolü
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        try {
            LocalDate baslangic = LocalDate.parse(baslangicField.getText(), formatter);
            LocalDate bitis = LocalDate.parse(bitisField.getText(), formatter);

            if (bitis.isBefore(baslangic)) {
                JOptionPane.showMessageDialog(this,
                        "Bitiş tarihi başlangıç tarihinden önce olamaz!",
                        "Hatalı Tarih",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }

            return true;

        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this,
                    "Lütfen tarihleri YYYY-MM-DD formatında girin!\nÖrnek: 2025-01-13",
                    "Hatalı Format",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
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

        // Label'lar
        sinavTuruLabel.setForeground(foreground);
        baslangicLabel.setForeground(foreground);
        bitisLabel.setForeground(foreground);
        tarihFormatLabel.setForeground(isDarkMode ? new Color(150, 150, 150) : new Color(100, 100, 100));
        yasakliGunlerLabel.setForeground(foreground);
        molaLabel.setForeground(foreground);

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

        // CheckBox'lar
        yasakliGunlerPanel.setBackground(background);
        for (JCheckBox cb : gunCheckBoxes) {
            cb.setBackground(background);
            cb.setForeground(foreground);
        }

        yasakliScroll.getViewport().setBackground(background);
    }

    void updateComponentBounds() {
        int w = this.getContentPane().getWidth();
        int h = this.getContentPane().getHeight();
        float aw = (float) w / DEFAULT_WIDTH;
        float ah = (float) h / DEFAULT_HEIGHT;
        int fontSize = (int) (16 * Math.min(aw, ah));
        int labelFontSize = (int) (14 * Math.min(aw, ah));

        geriButton.setFont(new Font("Arial", Font.BOLD, fontSize));
        themeButton.setFont(new Font("Arial", Font.BOLD, fontSize));

        sinavTuruLabel.setFont(new Font("Arial", Font.BOLD, labelFontSize));
        baslangicLabel.setFont(new Font("Arial", Font.BOLD, labelFontSize));
        bitisLabel.setFont(new Font("Arial", Font.BOLD, labelFontSize));
        yasakliGunlerLabel.setFont(new Font("Arial", Font.BOLD, labelFontSize));
        molaLabel.setFont(new Font("Arial", Font.BOLD, labelFontSize));

        // Pozisyonlar
        geriButton.setBounds((int) (20 * aw), (int) (20 * ah), (int) (150 * aw), (int) (45 * ah));
        themeButton.setBounds((int) (1205 * aw), (int) (20 * ah), (int) (55 * aw), (int) (55 * ah));

        // Satır 1: Sınav Türü
        sinavTuruLabel.setBounds((int)(50 * aw), (int)(100 * ah), (int)(250 * aw), (int)(30 * ah));
        sinavTuruCombo.setBounds((int)(300 * aw), (int)(100 * ah), (int)(350 * aw), (int)(35 * ah));

        // Satır 2: Başlangıç Tarihi
        baslangicLabel.setBounds((int)(50 * aw), (int)(100 * ah) + (int)(70 * ah), (int)(250 * aw), (int)(30 * ah));
        baslangicField.setBounds((int)(300 * aw), (int)(100 * ah) + (int)(70 * ah), (int)(350 * aw), (int)(35 * ah));

        // Satır 3: Bitiş Tarihi
        bitisLabel.setBounds((int)(50 * aw), (int)(100 * ah) + (int)(70 * ah) * 2, (int)(250 * aw), (int)(30 * ah));
        bitisField.setBounds((int)(300 * aw), (int)(100 * ah) + (int)(70 * ah) * 2, (int)(350 * aw), (int)(35 * ah));

        // Satır 3 Ek: Tarih Formatı Uyarısı
        tarihFormatLabel.setBounds((int)(300 * aw), (int)(100 * ah) + (int)(70 * ah) * 2 + (int)(40 * ah), (int)(350 * aw), (int)(20 * ah));

        // Satır 4: Mola Süresi
        molaLabel.setBounds((int)(50 * aw), (int)(100 * ah) + (int)(70 * ah) * 3, (int)(250 * aw), (int)(30 * ah));
        molaSpinner.setBounds((int)(300 * aw), (int)(100 * ah) + (int)(70 * ah) * 3, (int)(150 * aw), (int)(35 * ah));

        // Sağ kolon - Yasaklı günler
        yasakliGunlerLabel.setBounds((int)(700 * aw), (int)(100 * ah), (int)(400 * aw), (int)(30 * ah));
        yasakliScroll.setBounds((int)(700 * aw), (int)(100 * ah) + (int)(40 * ah), (int)(300 * aw), (int)(300 * ah));

        // Oluştur butonu
        olusturButton.setBounds((int)(450 * aw), (int)(600 * ah), (int)(380 * aw), (int)(60 * ah));
        this.revalidate();
        this.repaint();
    }
}