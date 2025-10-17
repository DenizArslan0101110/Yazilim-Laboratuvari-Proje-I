package visualization;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.sql.*;
import java.util.ArrayList;

public class vis32Frame extends JFrame {

    String url = "jdbc:mysql://localhost:3306/muhendislik_fakulte?serverTimezone=UTC&useSSL=false";
    String user = "root";
    String pass = "yazlab1";


    final Timer resizeTimer;

    JLabel titleLabel;
    JButton themeButton;
    JButton geriButton;
    JTextField searchField;
    JScrollPane scrollPane;
    JPanel ogrenciListPanel; // Düzeltme: Öğrenci listesi için panel adı daha uygun yapıldı (dersListPanel yerine)

    ArrayList<Ogrenci> tumOgrenciler = new ArrayList<>();
    boolean isDarkMode;

    static final int DEFAULT_WIDTH = 1280;
    static final int DEFAULT_HEIGHT = 720;

    public vis32Frame(boolean isDarkMode) {
        this.isDarkMode = isDarkMode;

        this.setTitle("Öğrenci Listesi");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(true);
        this.setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        this.setLayout(null);
        this.setLocationRelativeTo(null);

        resizeTimer = new Timer(100, e -> updateComponentBounds());
        resizeTimer.setRepeats(false);

        titleLabel = new JLabel("Öğrenci Listesi", SwingConstants.CENTER);
        themeButton = new JButton(isDarkMode ? "L" : "D");
        geriButton = new JButton("Geri");
        searchField = new JTextField("Öğrenci adı veya numarası ile ara...");

        ogrenciListPanel = new JPanel(); // Düzeltme: Yeni panel adı kullanıldı
        ogrenciListPanel.setLayout(new BoxLayout(ogrenciListPanel, BoxLayout.Y_AXIS));

        scrollPane = new JScrollPane(ogrenciListPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        this.add(titleLabel);
        this.add(themeButton);
        this.add(geriButton);
        this.add(searchField);
        this.add(scrollPane);


        geriButton.addActionListener(e -> {


                 vis2Frame newScreen = new vis2Frame();
                newScreen.setDarkMode(this.isDarkMode);
                 newScreen.setVisible(true);
                this.dispose();

        });

        themeButton.addActionListener(e -> {
            this.isDarkMode = !this.isDarkMode;
            applyTheme();
        });

        searchField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (searchField.getText().equals("Öğrenci adı veya numarası ile ara...")) {
                    searchField.setText("");
                }
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (searchField.getText().trim().isEmpty()) {
                    searchField.setText("Öğrenci adı veya numarası ile ara...");
                }
            }
        });

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { filterOgrenciler(); }
            @Override
            public void removeUpdate(DocumentEvent e) { filterOgrenciler(); }
            @Override
            public void changedUpdate(DocumentEvent e) { filterOgrenciler(); }
        });

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) { resizeTimer.restart(); }
        });

        loadOgrencilerFromDB();
        updateComponentBounds();
        applyTheme();
        this.setVisible(true);
    }

    private void loadOgrencilerFromDB() {
        System.out.println("Veritabanından öğrenciler yükleniyor...");

        tumOgrenciler.clear();
        String sql = "SELECT isim, numara, sinif FROM ogrenci ORDER BY sinif, numara";

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Ogrenci ogrenci = new Ogrenci(
                        rs.getString("isim"),
                        rs.getString("numara"),
                        rs.getInt("sinif")
                );
                tumOgrenciler.add(ogrenci);
            }
            System.out.println("Toplam " + tumOgrenciler.size() + " öğrenci yüklendi.");

        } catch (SQLException e) {
            System.err.println("SQL Hatası: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Öğrenciler veritabanından çekilemedi!", "Veritabanı Hatası", JOptionPane.ERROR_MESSAGE);
        }

        populateOgrenciList(tumOgrenciler);
    }

    private void filterOgrenciler() {
        String searchText = searchField.getText().toLowerCase().trim();

        if (searchText.isEmpty() || searchText.equals("öğrenci adı veya numarası ile ara...")) {
            populateOgrenciList(tumOgrenciler);
            return;
        }

        ArrayList<Ogrenci> filtrelenmisListe = new ArrayList<>();
        for (Ogrenci ogrenci : tumOgrenciler) {
            // İsim veya numara ile filtrele
            if (ogrenci.isim.toLowerCase().contains(searchText) || ogrenci.numara.toLowerCase().contains(searchText)) {
                filtrelenmisListe.add(ogrenci);
            }
        }

        populateOgrenciList(filtrelenmisListe);
    }

    private void populateOgrenciList(ArrayList<Ogrenci> ogrenciler) {
        ogrenciListPanel.removeAll();

        if (ogrenciler.isEmpty()) {
            JLabel emptyLabel = new JLabel("Öğrenci bulunamadı", SwingConstants.CENTER);
            emptyLabel.setFont(new Font("Arial", Font.ITALIC, 16));
            ogrenciListPanel.add(emptyLabel);
        } else {
            for (Ogrenci ogrenci : ogrenciler) {
                JPanel ogrenciPanel = new JPanel(new BorderLayout(10, 0));
                ogrenciPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70)); // Yükseklik 70 yapıldı
                ogrenciPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

                JLabel ogrenciLabel = new JLabel(String.format("<html><b>%s</b> (%s)</html>",
                        ogrenci.isim, ogrenci.numara));
                ogrenciLabel.setFont(new Font("Arial", Font.BOLD, 16));

                JLabel detayLabel = new JLabel(String.format("Sınıf: %d", ogrenci.sinif));
                detayLabel.setFont(new Font("Arial", Font.PLAIN, 14));
                detayLabel.setHorizontalAlignment(SwingConstants.RIGHT);

                ogrenciPanel.add(ogrenciLabel, BorderLayout.CENTER);
                ogrenciPanel.add(detayLabel, BorderLayout.EAST);

                ogrenciListPanel.add(ogrenciPanel);
                ogrenciListPanel.add(Box.createRigidArea(new Dimension(0, 5)));
            }
        }

        ogrenciListPanel.revalidate();
        ogrenciListPanel.repaint();
        applyThemeToListItems();
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
        ogrenciListPanel.setBackground(background); // Düzeltme: ogrenciListPanel adı kullanıldı

        applyThemeToListItems();
    }

    private void applyThemeToListItems() {
        Color panelBg = isDarkMode ? new Color(30, 30, 30) : new Color(248, 248, 248);
        Color textFg = isDarkMode ? Color.WHITE : Color.BLACK;

        for (Component comp : ogrenciListPanel.getComponents()) {
            if (comp instanceof JPanel) {
                JPanel ogrenciPanel = (JPanel) comp;
                ogrenciPanel.setBackground(panelBg);
                for (Component innerComp : ogrenciPanel.getComponents()) {
                    if (innerComp instanceof JLabel) {
                        ((JLabel) innerComp).setForeground(textFg);
                    }
                }
            } else if (comp instanceof JLabel) {
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


    static class Ogrenci {
        String isim;
        int sinif;
        String numara;

        Ogrenci(String isim, String numara, int sinif) {
            this.isim = isim;
            this.numara = numara;
            this.sinif = sinif;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new vis32Frame(true);
        });
    }
}