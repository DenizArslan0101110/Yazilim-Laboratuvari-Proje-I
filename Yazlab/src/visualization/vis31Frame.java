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

    private void populateDersList(ArrayList<Ders> dersler) {
        dersListPanel.removeAll();

        if (dersler.isEmpty()) {
            JLabel emptyLabel = new JLabel("Ders bulunamadı", SwingConstants.CENTER);
            emptyLabel.setFont(new Font("Arial", Font.ITALIC, 16));
            dersListPanel.add(emptyLabel);
        } else {
            for (Ders ders : dersler) {
                JPanel dersPanel = new JPanel(new BorderLayout(10, 0));
                dersPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
                dersPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

                JLabel dersLabel = new JLabel(String.format("<html><b>%s</b> - %s</html>",
                        ders.dersKodu, ders.dersAdi));
                dersLabel.setFont(new Font("Arial", Font.PLAIN, 14));

                JLabel detayLabel = new JLabel(String.format("<html>Sınıf: %d<br>Hoca: %s</html>",
                        ders.sinif, ders.hoca));
                detayLabel.setFont(new Font("Arial", Font.PLAIN, 12));
                detayLabel.setHorizontalAlignment(SwingConstants.RIGHT);

                dersPanel.add(dersLabel, BorderLayout.CENTER);
                dersPanel.add(detayLabel, BorderLayout.EAST);

                dersListPanel.add(dersPanel);
                dersListPanel.add(Box.createRigidArea(new Dimension(0, 5)));
            }
        }

        dersListPanel.revalidate();
        dersListPanel.repaint();
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
        dersListPanel.setBackground(background);

        applyThemeToListItems();
    }

    private void applyThemeToListItems() {
        Color panelBg = isDarkMode ? new Color(30, 30, 30) : new Color(248, 248, 248);
        Color textFg = isDarkMode ? Color.WHITE : Color.BLACK;

        for (Component comp : dersListPanel.getComponents()) {
            if (comp instanceof JPanel) {
                JPanel dersPanel = (JPanel) comp;
                dersPanel.setBackground(panelBg);
                for (Component innerComp : dersPanel.getComponents()) {
                    if (innerComp instanceof JLabel) {
                        innerComp.setForeground(textFg);
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