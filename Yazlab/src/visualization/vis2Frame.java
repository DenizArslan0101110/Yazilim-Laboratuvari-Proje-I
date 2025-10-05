package visualization;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class vis2Frame extends JFrame {

    // Global Bileşenler
    JLabel titleLabel;
    JButton cikisButton;
    JButton derslikButton;
    JButton dersListesiYuklemeButton;
    JButton ogrenciListesiYuklemeButton;
    JButton dersListesiButton;
    JButton ogrenciListesiButton;
    JButton sinavProgramiButton;
    JButton themeButton;

    // Tema Durumu
    private boolean isDarkMode = true;

    // TEMEL BOYUTLAR (vis1Frame ile aynı kalmalı)
    static final int BASE_WIDTH = 1280;
    static final int BASE_HEIGHT = 720;
    static final int BASE_FONT_SIZE = 24;

    // YENİ DİZAYN SABİTLERİ
    static final double SIDEBAR_WIDTH_PERCENT = 0.25;
    static final int BUTTON_HEIGHT = 45;
    static final int BUTTON_V_GAP = 15;
    static final int BUTTON_MARGIN = 20;
    static final int TITLE_TOP_MARGIN = 50;
    static final int THEME_BUTTON_SIZE = 40;

    public vis2Frame() {
        this.setTitle("Dinamik Sınav Takvimi Oluşturma Sistemi");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(true);
        this.setSize(BASE_WIDTH, BASE_HEIGHT);
        this.setLayout(null);

        // Bileşen oluştuma
        titleLabel = new JLabel("Bölüm Koordinatörü", SwingConstants.CENTER);

        cikisButton = new JButton("Çıkış Yap");
        derslikButton = new JButton("Derslik Girişi");
        dersListesiYuklemeButton = new JButton("Ders Listesi Yükleme");
        ogrenciListesiYuklemeButton = new JButton("Öğrenci Listesi Yükleme");
        dersListesiButton = new JButton("Ders Listesi");
        ogrenciListesiButton = new JButton("Öğrenci Listesi");
        sinavProgramiButton = new JButton("Sınav Programı Oluşturma");

        themeButton = new JButton();
        themeButton.setText(isDarkMode ? "L" : "D");

        // --- FRAME'E EKLEME ---
        this.add(titleLabel);
        this.add(cikisButton);
        this.add(derslikButton);
        this.add(dersListesiYuklemeButton);
        this.add(ogrenciListesiYuklemeButton);
        this.add(dersListesiButton);
        this.add(ogrenciListesiButton);
        this.add(sinavProgramiButton);
        this.add(themeButton);

        // Dinamik boyutlandırma
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateComponentBounds();
            }
        });

        applyTheme();
        updateComponentBounds();  // aşşağıdaki boyut oluşturucu burdan çağrılıyo elleme

        // -BUTON
        cikisButton.addActionListener(e -> {
            vis1Frame newScreen = new vis1Frame();
            newScreen.setDarkMode(this.isDarkMode);
            newScreen.setVisible(true);
            this.dispose();
        });

        // BUTON
        themeButton.addActionListener(e -> {
            isDarkMode = !isDarkMode;
            applyTheme();
            updateComponentBounds();
        });
    }

    // TEMA BİLGİSİ ALIYO
    public void setDarkMode(boolean isDark) {
        this.isDarkMode = isDark;
        applyTheme();
    }


    // YARDIMCI METOTLAR
    private void applyTheme() {
        Color background, foreground, buttonBg, buttonFg;

        if (isDarkMode) {
            background = Color.BLACK;
            foreground = Color.WHITE;
            buttonBg = new Color(30, 30, 30);
            buttonFg = Color.WHITE;
            themeButton.setText("L");
        } else {
            background = Color.WHITE;
            foreground = Color.BLACK;
            buttonBg = new Color(230, 230, 230);
            buttonFg = Color.BLACK;
            themeButton.setText("D");
        }

        this.getContentPane().setBackground(background);
        titleLabel.setForeground(foreground);

        JButton[] buttons = {cikisButton, derslikButton, dersListesiYuklemeButton, ogrenciListesiYuklemeButton, dersListesiButton, ogrenciListesiButton, sinavProgramiButton};
        for (JButton btn : buttons) {
            btn.setBackground(buttonBg);
            btn.setForeground(buttonFg);
            btn.setBorder(BorderFactory.createLineBorder(foreground, 1));
        }

        themeButton.setBackground(isDarkMode ? new Color(50, 50, 50) : new Color(200, 200, 200));
        themeButton.setForeground(foreground);
        themeButton.setBorder(null);
    }


    void updateComponentBounds() {
        int frameWidth = this.getContentPane().getWidth();
        int frameHeight = this.getContentPane().getHeight();

        double widthScale = (double) frameWidth / BASE_WIDTH;
        double heightScale = (double) frameHeight / BASE_HEIGHT;
        double finalScale = Math.min(widthScale, heightScale);


        int newBtnHeight = (int) (BUTTON_HEIGHT * heightScale);
        int newBtnGap = (int) (BUTTON_V_GAP * heightScale);
        int newMargin = (int) (BUTTON_MARGIN * widthScale);
        int themeBtnSize = (int) (THEME_BUTTON_SIZE * finalScale);

        // Font Ayarları
        int newFontSize = (int) (BASE_FONT_SIZE * finalScale);
        if (newFontSize < 14) newFontSize = 14;

        int titleFontSize = (int) (newFontSize * 1.5);
        if (titleFontSize < 30) titleFontSize = 30;
        Font titleFont = new Font("Arial", Font.BOLD, titleFontSize);
        titleLabel.setFont(titleFont);

        Font buttonFont = new Font("Arial", Font.BOLD, newFontSize);
        JButton[] buttons = {cikisButton, derslikButton, dersListesiYuklemeButton, ogrenciListesiYuklemeButton, dersListesiButton, ogrenciListesiButton, sinavProgramiButton, themeButton};
        for (JButton btn : buttons) {
            btn.setFont(buttonFont);
        }
        themeButton.setFont(buttonFont.deriveFont((float)newFontSize * 0.7f));


        int themeX = frameWidth - newMargin - themeBtnSize;
        int themeY = newMargin;
        themeButton.setBounds(themeX, themeY, themeBtnSize, themeBtnSize);



        int sidebarWidth = (int) (frameWidth * SIDEBAR_WIDTH_PERCENT);
        int buttonWidth = sidebarWidth - (2 * newMargin);

        int currentY = newMargin;


        cikisButton.setBounds(newMargin, currentY, buttonWidth, newBtnHeight);
        currentY += newBtnHeight + newBtnGap * 2;


        derslikButton.setBounds(newMargin, currentY, buttonWidth, newBtnHeight);
        currentY += newBtnHeight + newBtnGap;
        dersListesiYuklemeButton.setBounds(newMargin, currentY, buttonWidth, newBtnHeight);
        currentY += newBtnHeight + newBtnGap;
        ogrenciListesiYuklemeButton.setBounds(newMargin, currentY, buttonWidth, newBtnHeight);
        currentY += newBtnHeight + newBtnGap * 2;


        dersListesiButton.setBounds(newMargin, currentY, buttonWidth, newBtnHeight);
        currentY += newBtnHeight + newBtnGap;
        ogrenciListesiButton.setBounds(newMargin, currentY, buttonWidth, newBtnHeight);
        currentY += newBtnHeight + newBtnGap;
        sinavProgramiButton.setBounds(newMargin, currentY, buttonWidth, newBtnHeight);


        int mainAreaX = sidebarWidth;
        int mainAreaWidth = frameWidth - sidebarWidth;

        int titleHeight = (int) (newBtnHeight * 1.5);
        int titleY = (int) (TITLE_TOP_MARGIN * heightScale);

        titleLabel.setBounds(mainAreaX, titleY, mainAreaWidth, titleHeight);

        this.revalidate();
        this.repaint();
    }
}