package visualization;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.util.List;

public class vis2Frame extends JFrame
{
    // Veritabanı Bilgileri (Excel sınıfı için)
    String url = "jdbc:mysql://localhost:3306/muhendislik_fakulte?allowPublicKeyRetrieval=true&serverTimezone=UTC&useSSL=false";
    String user = "root";
    String pass = "yazlab1";

    // Global Bileşenler
    JLabel titleLabel;
    JButton cikisButton;
    JButton derslikButton;
    JButton dersListesiButton;
    JButton ogrenciListesiButton;
    JButton sinavProgramiButton;
    JButton kullaniciListesiButton;
    JButton themeButton;

    // Dosya Yükleme Alanları
    JPanel dersListesiDropPanel;
    JPanel ogrenciListesiDropPanel;
    JLabel dersListesiDropLabel;
    JLabel ogrenciListesiDropLabel;
    JButton dersListesiSecButton;
    JButton ogrenciListesiSecButton;

    // Dosya yolları
    static String dersListesiDosyaYolu = null;
    static String ogrenciListesiDosyaYolu = null;

    // Tema Durumu
    boolean isDarkMode = true;

    // Timer (Donma Çözümü)
    private final Timer resizeTimer;
    private static final int RESIZE_DELAY_MS = 100;

    // REFERANS BOYUTLAR
    static final int DEFAULT_WIDTH = 1280;
    static final int DEFAULT_HEIGHT = 720;

    public vis2Frame()
    {
        this.setTitle("Dinamik Sınav Takvimi Oluşturma Sistemi");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(true);
        this.setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        this.setLayout(null);
        this.setLocationRelativeTo(null);

        // TIMER TANIMLAMASI (Donma Çözümü)
        resizeTimer = new Timer(RESIZE_DELAY_MS, e -> updateComponentBounds());
        resizeTimer.setRepeats(false);

        // Bileşen oluşturma
        // Başlık, giriş yapan kullanıcıya göre (Admin/Koordinatör) ayarlanır
        if(UserSession.isAdmin()) {
            titleLabel = new JLabel("Admin Paneli", SwingConstants.CENTER);
        } else {
            titleLabel = new JLabel("Bölüm Koordinatörü", SwingConstants.CENTER);
        }

        cikisButton = new JButton("Çıkış Yap");
        derslikButton = new JButton("Derslik Girişi");
        dersListesiButton = new JButton("Ders Listesi");
        ogrenciListesiButton = new JButton("Öğrenci Listesi");
        sinavProgramiButton = new JButton("Sınav Programı Oluşturma");
        themeButton = new JButton();
        themeButton.setText(isDarkMode ? "L" : "D");

        // Kullanıcı Listesi Butonu (Sadece Admin görebilir)
        kullaniciListesiButton = new JButton("Kullanıcı Listesi");
        kullaniciListesiButton.setVisible(UserSession.isAdmin());

        // Dosya yükleme panelleri oluştur
        createFileDropPanels();

        // Frame'e ekleme
        this.add(titleLabel);
        this.add(cikisButton);
        this.add(derslikButton);
        this.add(dersListesiButton);
        this.add(ogrenciListesiButton);
        this.add(sinavProgramiButton);
        this.add(kullaniciListesiButton);
        this.add(themeButton);
        this.add(dersListesiDropPanel);
        this.add(ogrenciListesiDropPanel);

        // Dinamik boyutlandırma (TIMER KULLANILARAK GÜNCELLENDİ)
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeTimer.restart(); // Donmayı önlemek için timer'ı yeniden başlat
            }
        });

        applyTheme();
        updateComponentBounds();

        // --- BUTON İŞLEYİCİLER ---

        // ÇIKIŞ BUTON
        cikisButton.addActionListener(e -> {
            UserSession.clearSession(); // Oturumu temizle
            vis1Frame newScreen = new vis1Frame();
            newScreen.setDarkMode(this.isDarkMode);
            newScreen.setVisible(true);
            this.dispose();
        });

        // THEME BUTON
        themeButton.addActionListener(e -> {
            isDarkMode = !isDarkMode;
            applyTheme();
            // updateComponentBounds(); // Tema renkleri boyutu etkilemediği için buna gerek yok
        });

        // DERSLİK BUTONU (vis3Frame'e geçiş)
        derslikButton.addActionListener(e -> {
            vis3Frame newScreen = new vis3Frame(this.isDarkMode);
            newScreen.setVisible(true);
            this.dispose();
        });

        // DERS LİSTESİ BUTONU (vis31Frame'e geçiş)
        dersListesiButton.addActionListener(e -> {
            vis31Frame newScreen = new vis31Frame(this.isDarkMode);
            newScreen.setVisible(true);
            this.dispose();
        });

        // ÖĞRENCİ LİSTESİ BUTONU (vis32Frame'e geçiş)
        ogrenciListesiButton.addActionListener(e -> {
            vis32Frame newScreen = new vis32Frame(this.isDarkMode);
            newScreen.setVisible(true);
            this.dispose();
        });

        // SINAV PROGRAMI BUTONU (vis33Frame'e geçiş)
        sinavProgramiButton.addActionListener(e -> {
            vis33Frame newScreen = new vis33Frame(this.isDarkMode);
            newScreen.setVisible(true);
            this.dispose();
        });

        // KULLANICI LİSTESİ BUTONU (vis34Frame'e geçiş)
        kullaniciListesiButton.addActionListener(e -> {
            vis34Frame userListScreen = new vis34Frame(this.isDarkMode);
            userListScreen.setVisible(true);
            this.dispose();
        });
    }

    private void createFileDropPanels()
    {
        // DERS LİSTESİ PANEL
        dersListesiDropPanel = new JPanel();
        dersListesiDropPanel.setLayout(new BorderLayout());
        dersListesiDropPanel.setBorder(BorderFactory.createDashedBorder(Color.GRAY, 2, 5, 5, true));

        JPanel dersContentPanel = new JPanel();
        dersContentPanel.setLayout(new BoxLayout(dersContentPanel, BoxLayout.Y_AXIS));
        dersContentPanel.setOpaque(false);

        JLabel dersBaslikLabel = new JLabel("Ders Listesi Yükle");
        dersBaslikLabel.setFont(new Font("Arial", Font.BOLD, 18));
        dersBaslikLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        dersListesiDropLabel = new JLabel("Dosyayı buraya sürükleyin veya");
        dersListesiDropLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        dersListesiDropLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        dersListesiSecButton = new JButton("Dosya Seç");
        dersListesiSecButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        dersListesiSecButton.setPreferredSize(new Dimension(150, 35));
        dersListesiSecButton.setMaximumSize(new Dimension(150, 35));

        dersContentPanel.add(Box.createVerticalGlue());
        dersContentPanel.add(dersBaslikLabel);
        dersContentPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        dersContentPanel.add(dersListesiDropLabel);
        dersContentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        dersContentPanel.add(dersListesiSecButton);
        dersContentPanel.add(Box.createVerticalGlue());

        dersListesiDropPanel.add(dersContentPanel, BorderLayout.CENTER);

        // ÖĞRENCİ LİSTESİ PANEL
        ogrenciListesiDropPanel = new JPanel();
        ogrenciListesiDropPanel.setLayout(new BorderLayout());
        ogrenciListesiDropPanel.setBorder(BorderFactory.createDashedBorder(Color.GRAY, 2, 5, 5, true));

        JPanel ogrenciContentPanel = new JPanel();
        ogrenciContentPanel.setLayout(new BoxLayout(ogrenciContentPanel, BoxLayout.Y_AXIS));
        ogrenciContentPanel.setOpaque(false);

        JLabel ogrenciBaslikLabel = new JLabel("Öğrenci Listesi Yükle");
        ogrenciBaslikLabel.setFont(new Font("Arial", Font.BOLD, 18));
        ogrenciBaslikLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        ogrenciListesiDropLabel = new JLabel("Dosyayı buraya sürükleyin veya");
        ogrenciListesiDropLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        ogrenciListesiDropLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        ogrenciListesiSecButton = new JButton("Dosya Seç");
        ogrenciListesiSecButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        ogrenciListesiSecButton.setPreferredSize(new Dimension(150, 35));
        ogrenciListesiSecButton.setMaximumSize(new Dimension(150, 35));

        ogrenciContentPanel.add(Box.createVerticalGlue());
        ogrenciContentPanel.add(ogrenciBaslikLabel);
        ogrenciContentPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        ogrenciContentPanel.add(ogrenciListesiDropLabel);
        ogrenciContentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        ogrenciContentPanel.add(ogrenciListesiSecButton);
        ogrenciContentPanel.add(Box.createVerticalGlue());

        ogrenciListesiDropPanel.add(ogrenciContentPanel, BorderLayout.CENTER);

        // DRAG & DROP İŞLEMLERİ
        setupDragAndDrop(dersListesiDropPanel, true);
        setupDragAndDrop(ogrenciListesiDropPanel, false);

        // DOSYA SEÇ BUTONLARI
        dersListesiSecButton.addActionListener(e -> selectFile(true));
        ogrenciListesiSecButton.addActionListener(e -> selectFile(false));
    }

    private void setupDragAndDrop(JPanel panel, boolean isDersList)
    {
        new DropTarget(panel, new DropTargetListener()
        {
            @Override
            public void dragEnter(DropTargetDragEvent dtde) {
                panel.setBackground(isDarkMode ? new Color(40, 40, 40) : new Color(220, 220, 220));
            }
            @Override
            public void dragOver(DropTargetDragEvent dtde) {}
            @Override
            public void dropActionChanged(DropTargetDragEvent dtde) {}
            @Override
            public void dragExit(DropTargetEvent dte) {
                panel.setBackground(isDarkMode ? new Color(20, 20, 20) : new Color(245, 245, 245));
            }
            @Override
            public void drop(DropTargetDropEvent dtde)
            {
                try
                {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    @SuppressWarnings("unchecked")
                    List<File> droppedFiles = (List<File>) dtde.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);

                    if (!droppedFiles.isEmpty())
                    {
                        File file = droppedFiles.get(0);
                        String fileName = file.getName().toLowerCase();

                        if (fileName.endsWith(".xls") || fileName.endsWith(".xlsx"))
                        {
                            handleFileSelection(file, isDersList);
                        }
                        else
                        {
                            JOptionPane.showMessageDialog(vis2Frame.this,
                                    "Lütfen sadece Excel dosyası (.xls veya .xlsx) seçin!",
                                    "Hatalı Dosya Tipi", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                    dtde.dropComplete(true);
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                    dtde.dropComplete(false);
                }
                panel.setBackground(isDarkMode ? new Color(20, 20, 20) : new Color(245, 245, 245));
            }
        });
    }

    private void selectFile(boolean isDersList)
    {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(isDersList ? "Ders Listesi Seç" : "Öğrenci Listesi Seç");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter()
        {
            @Override
            public boolean accept(File f)
            {
                if (f.isDirectory()) return true;
                String name = f.getName().toLowerCase();
                return name.endsWith(".xls") || name.endsWith(".xlsx");
            }

            @Override
            public String getDescription() {
                return "Excel Dosyaları (*.xls, *.xlsx)";
            }
        });

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            handleFileSelection(selectedFile, isDersList);
        }
    }

    private void handleFileSelection(File file, boolean isDersList)
    {
        if (isDersList)
        {
            dersListesiDropLabel.setText("Dosya işleniyor...");
            dersListesiDropLabel.setForeground(new Color(200, 150, 0));
            dersListesiDosyaYolu = file.getAbsolutePath();
            SwingWorker<Void, Void> worker = new SwingWorker<>()
            {
                @Override
                protected Void doInBackground()
                {
                    Excel.ReadExcelForCoursesAndSendToDatabase(vis2Frame.dersListesiDosyaYolu, "bilgisayar");
                    return null;
                }

                @Override
                protected void done()
                {
                    dersListesiDropLabel.setText("Dosya Yüklendi: " + file.getName());
                    dersListesiDropLabel.setForeground(new Color(0, 150, 0));
                }
            };
            worker.execute();
        }
        else
        {
            ogrenciListesiDropLabel.setText("Dosya işleniyor...");
            ogrenciListesiDropLabel.setForeground(new Color(200, 150, 0));
            ogrenciListesiDosyaYolu = file.getAbsolutePath();
            SwingWorker<Void, Void> worker = new SwingWorker<>()
            {
                @Override
                protected Void doInBackground()
                {
                    Excel.ReadExcelForStudentsAndSendToDatabase(vis2Frame.ogrenciListesiDosyaYolu);
                    return null;
                }

                @Override
                protected void done()
                {
                    ogrenciListesiDropLabel.setText("Dosya Yüklendi: " + file.getName());
                    ogrenciListesiDropLabel.setForeground(new Color(0, 150, 0));
                }
            };
            worker.execute();
        }
    }

    public void setDarkMode(boolean isDark)
    {
        this.isDarkMode = isDark;
        applyTheme();
    }

    private void applyTheme()
    {
        Color background, foreground, buttonBg, buttonFg, dropPanelBg;

        if (isDarkMode)
        {
            background = Color.BLACK;
            foreground = Color.WHITE;
            buttonBg = new Color(30, 30, 30);
            buttonFg = Color.WHITE;
            dropPanelBg = new Color(20, 20, 20);
            themeButton.setText("L");
        }
        else
        {
            background = Color.WHITE;
            foreground = Color.BLACK;
            buttonBg = new Color(230, 230, 230);
            buttonFg = Color.BLACK;
            dropPanelBg = new Color(245, 245, 245);
            themeButton.setText("D");
        }

        this.getContentPane().setBackground(background);
        titleLabel.setForeground(foreground);

        JButton[] buttons = {cikisButton, derslikButton, dersListesiButton, ogrenciListesiButton, sinavProgramiButton, kullaniciListesiButton};
        for (JButton btn : buttons)
        {
            btn.setBackground(buttonBg);
            btn.setForeground(buttonFg);
            btn.setBorder(BorderFactory.createLineBorder(foreground, 1));
        }

        themeButton.setBackground(isDarkMode ? new Color(50, 50, 50) : new Color(200, 200, 200));
        themeButton.setForeground(foreground);
        themeButton.setBorder(null);

        // Dosya yükleme panelleri tema
        dersListesiDropPanel.setBackground(dropPanelBg);
        ogrenciListesiDropPanel.setBackground(dropPanelBg);
        dersListesiDropPanel.setBorder(BorderFactory.createDashedBorder(foreground, 2, 5, 5, true));
        ogrenciListesiDropPanel.setBorder(BorderFactory.createDashedBorder(foreground, 2, 5, 5, true));

        // Panellerin içindeki bileşenlerin rengini güncelle
        updateDropPanelColors(dersListesiDropPanel, foreground);
        updateDropPanelColors(ogrenciListesiDropPanel, foreground);
    }

    private void updateDropPanelColors(JPanel panel, Color foreground)
    {
        for (Component comp : panel.getComponents())
        {
            if (comp instanceof JPanel)
            {
                JPanel innerPanel = (JPanel) comp;
                innerPanel.setBackground(panel.getBackground());
                for (Component innerComp : innerPanel.getComponents())
                {
                    if (innerComp instanceof JLabel)
                    {
                        // Drop Label'ın rengi "Yüklendi" durumunda değişeceği için
                        // Sadece "Yüklendi" metni içermiyorsa rengini değiştir
                        if (!((JLabel) innerComp).getText().contains("Yüklendi")) {
                            ((JLabel) innerComp).setForeground(foreground);
                        }
                    }
                    else if (innerComp instanceof JButton)
                    {
                        JButton btn = (JButton) innerComp;
                        btn.setBackground(new Color(60, 150, 255));
                        btn.setForeground(Color.WHITE);
                    }
                }
            }
        }
    }

    void updateComponentBounds()
    {
        int w = this.getContentPane().getWidth();
        int h = this.getContentPane().getHeight();

        float aw = (float) w / DEFAULT_WIDTH;
        float ah = (float) h / DEFAULT_HEIGHT;

        // FONT BOYUTLARI
        int fontSize = (int)(24 * Math.min(aw, ah));
        if (fontSize < 14) fontSize = 14;

        Font titleFont = new Font("Arial", Font.BOLD, (int)(fontSize * 1.5));
        Font buttonFont = new Font("Arial", Font.BOLD, fontSize);

        titleLabel.setFont(titleFont);

        JButton[] buttons = {cikisButton, derslikButton, dersListesiButton, ogrenciListesiButton, sinavProgramiButton, kullaniciListesiButton, themeButton};
        for (JButton btn : buttons)
        {
            btn.setFont(buttonFont);
        }
        themeButton.setFont(buttonFont.deriveFont((float)fontSize * 0.7f));

        // KONUMLAR VE BOYUTLAR (Sidebar butonları)
        cikisButton.setBounds((int)(20 * aw), (int)(20 * ah), (int)(280 * aw), (int)(45 * ah));
        derslikButton.setBounds((int)(20 * aw), (int)(100 * ah), (int)(280 * aw), (int)(45 * ah));
        dersListesiButton.setBounds((int)(20 * aw), (int)(160 * ah), (int)(280 * aw), (int)(45 * ah));
        ogrenciListesiButton.setBounds((int)(20 * aw), (int)(220 * ah), (int)(280 * aw), (int)(45 * ah));
        sinavProgramiButton.setBounds((int)(20 * aw), (int)(280 * ah), (int)(280 * aw), (int)(45 * ah));

        // Admin butonu
        if(UserSession.isAdmin()) {
            kullaniciListesiButton.setBounds((int)(20 * aw), (int)(340 * ah), (int)(280 * aw), (int)(45 * ah));
        }

        // BAŞLIK (Ana alan)
        titleLabel.setBounds((int)(320 * aw), (int)(50 * ah), (int)(940 * aw), (int)(68 * ah));

        // TEMA BUTONU (Sağ üst)
        themeButton.setBounds((int)(1205 * aw), (int)(20 * ah), (int)(55 * aw), (int)(55 * ah));

        // DOSYA YÜKLEME PANELLERİ (Yan yana yerleştirilmiş)
        int dropPanelWidth = (int)(440 * aw);
        int dropPanelHeight = (int)(550 * ah);
        int dropPanelY = (int)(150 * ah);

        dersListesiDropPanel.setBounds((int)(330 * aw), dropPanelY, dropPanelWidth, dropPanelHeight);
        ogrenciListesiDropPanel.setBounds((int)(790 * aw), dropPanelY, dropPanelWidth, dropPanelHeight);

        this.revalidate();
        this.repaint();
    }
}