package visualization;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.FileOutputStream;
import java.io.IOException; // Eklendi
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// --- PDF KÜTÜPHANESİ (iText 7) ---
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.io.font.PdfEncodings; // Eklendi
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.layout.properties.TextAlignment;

// Metin Listesi için Importlar
import com.itextpdf.layout.element.List;
import com.itextpdf.layout.element.ListItem;
// Sayfa Sonu için Import
import com.itextpdf.layout.element.AreaBreak;


public class vis4Frame extends JFrame {

    // Global Bileşenler
    JButton themeButton;
    JButton geriButton;
    JButton pdfButton;
    JList<KYS.Exam_Info> sinavListesi;
    DefaultListModel<KYS.Exam_Info> listModel;
    JScrollPane scrollPane;

    // Tema Durumu
    boolean isDarkMode;

    // Timer (Donma Çözümü)
    private final Timer resizeTimer;
    private static final int RESIZE_DELAY_MS = 100; // 100 milisaniye gecikme

    // REFERANS BOYUTLAR
    static final int DEFAULT_WIDTH = 1280;
    static final int DEFAULT_HEIGHT = 720;

    // Veritabanı Bilgileri
    String url = "jdbc:mysql://localhost:3306/muhendislik_fakulte?allowPublicKeyRetrieval=true&serverTimezone=UTC&useSSL=false";
    String user = "root";
    String pass = "yazlab1";

    public vis4Frame(boolean isDarkMode) {
        this.isDarkMode = isDarkMode;

        this.setTitle("Oluşturulan Sınav Programı");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(true);
        this.setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        this.setLayout(null);
        this.setLocationRelativeTo(null);

        resizeTimer = new Timer(RESIZE_DELAY_MS, e -> updateComponentBounds());
        resizeTimer.setRepeats(false);

        // Bileşen Oluşturma
        themeButton = new JButton(isDarkMode ? "L" : "D");
        geriButton = new JButton("Geri");
        pdfButton = new JButton("Seçili Sınavın Oturma Planını PDF Yap");

        // JList ve Modeli
        listModel = new DefaultListModel<>();
        sinavListesi = new JList<>(listModel);
        sinavListesi.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Özel JList Hücre Çizimi
        sinavListesi.setCellRenderer(new ExamListRenderer());

        scrollPane = new JScrollPane(sinavListesi);

        // Frame'e Ekleme
        this.add(themeButton);
        this.add(geriButton);
        this.add(pdfButton);
        this.add(scrollPane);

        // Dinamik Boyutlandırma
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeTimer.restart();
            }
        });

        applyTheme();
        updateComponentBounds();

        // KYS'den sınav listesini yükle
        loadExamList();

        // --- BUTON İŞLEYİCİLER ---
        geriButton.addActionListener(e -> {
            // vis33Frame'e geri dön
            vis33Frame newScreen = new vis33Frame(this.isDarkMode);
            newScreen.setVisible(true);
            this.dispose();
        });

        themeButton.addActionListener(e -> {
            this.isDarkMode = !this.isDarkMode;
            applyTheme();
        });

        pdfButton.addActionListener(e -> {
            KYS.Exam_Info selectedExam = sinavListesi.getSelectedValue();
            if (selectedExam != null && selectedExam.exam_course_id != null) {
                generatePdfForExam(selectedExam);
            } else {
                JOptionPane.showMessageDialog(this, "Lütfen PDF'ini oluşturmak için listeden geçerli bir sınav seçin.", "Uyarı", JOptionPane.WARNING_MESSAGE);
            }
        });

        this.setVisible(true);
    }

    // KYS'deki sınav listesini JList'e yükler
    private void loadExamList() {
        listModel.clear();
        if (KYS.exams_list == null || KYS.exams_list.isEmpty()) {
            listModel.addElement(new KYS.Exam_Info("", "", "", LocalDate.now(), 0, 0, new ArrayList<>()){
                @Override public String toString(){ return "Oluşturulmuş sınav programı bulunamadı."; }
            });
            pdfButton.setEnabled(false);
        } else {
            for (KYS.Exam_Info exam : KYS.exams_list) {
                listModel.addElement(exam);
            }
            pdfButton.setEnabled(true);
        }
    }

    // (GÜNCELLENDİ) Seçilen sınav için PDF oluşturur (Font Düzeltmesi)
    private void generatePdfForExam(KYS.Exam_Info exam) {
        String pdfFileName = exam.exam_course_id + "_" + exam.exam_date + "_OturmaPlani.pdf";

        try (PdfWriter writer = new PdfWriter(pdfFileName);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf))
        {
            System.out.println("PDF Oluşturuluyor: " + exam.exam_course_name);

            // --- TÜRKÇE KARAKTER DÜZELTMESİ ---
            String FONT_PATH = "C:/Windows/Fonts/arial.ttf";
            PdfFont titleFont = PdfFontFactory.createFont(FONT_PATH, PdfEncodings.IDENTITY_H, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
            PdfFont regularFont = PdfFontFactory.createFont(FONT_PATH, PdfEncodings.IDENTITY_H, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
            // --- DÜZELTME SONU ---


            // --- Ana Başlık (İlk Sayfada) ---
            document.add(new Paragraph("SINAV OTURMA PLANI")
                    .setFont(titleFont).setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Ders: " + exam.exam_course_name + " (" + exam.exam_course_id + ")")
                    .setFont(regularFont).setFontSize(12));
            document.add(new Paragraph("Tarih: " + exam.exam_date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                    .setFont(regularFont).setFontSize(12));
            document.add(new Paragraph("Saat: " + LocalTime.of(9, 0).plusMinutes(exam.exam_time).format(DateTimeFormatter.ofPattern("HH:mm")))
                    .setFont(regularFont).setFontSize(12));
            document.add(new Paragraph("Derslik(ler): " + String.join(", ", exam.exam_roomlist))
                    .setFont(regularFont).setFontSize(12));
            document.add(new Paragraph(" "));

            // KYS'den öğrenci dağılımını al
            Map<String, ArrayList<String>> ogrenciDagilimi = getStudentDistribution(exam);

            // Her derslik için 2 sayfa oluştur (Görsel + Metin)
            for (String derslikKodu : exam.exam_roomlist) {
                Derslik ilgiliDerslik = findDerslikByKod(derslikKodu);

                if (ilgiliDerslik == null) {
                    document.add(new Paragraph("UYARI: '" + derslikKodu + "' kodlu derslik veritabanında bulunamadı!")
                            .setFont(regularFont).setFontSize(11).setFontColor(ColorConstants.RED));
                    continue;
                }

                // Öğrencileri al
                ArrayList<String> derslikOgrencileri = ogrenciDagilimi.getOrDefault(derslikKodu, new ArrayList<>());
                if (derslikOgrencileri.isEmpty()) {
                    System.out.println("UYARI: " + derslikKodu + " için öğrenci listesi boş!");
                }

                // Efektif kapasite bilgisini al
                String kapasiteBilgisi = String.format("Bu Derslikteki Toplam Öğrenci: %d / %d",
                        derslikOgrencileri.size(), ilgiliDerslik.toplamKapasite);

                // --- Sayfa 1: GÖRSEL PLAN ---
                if (!derslikKodu.equals(exam.exam_roomlist.get(0))) {
                    document.add(new AreaBreak()); // Sayfa sonu
                }
                PdfCanvas canvas = new PdfCanvas(pdf.getLastPage());
                Rectangle pageSize = pdf.getDefaultPageSize();
                document.add(new Paragraph("Derslik: " + derslikKodu + " (Görsel Oturma Planı)")
                        .setFont(titleFont).setFontSize(16)
                        .setTextAlignment(TextAlignment.CENTER));

                // Görsel Çizim Metotları
                drawEmptySeatingPlan(canvas, pageSize, ilgiliDerslik);
                placeStudentsOnVisualPlan(canvas, pageSize, ilgiliDerslik, derslikOgrencileri, titleFont);
                document.add(new Paragraph(kapasiteBilgisi)
                        .setFont(regularFont).setFontSize(10)
                        .setFixedPosition(pageSize.getLeft() + 72, pageSize.getBottom() + 72, pageSize.getWidth() - 144) // Sayfanın altı
                        .setTextAlignment(TextAlignment.CENTER));


                // --- Sayfa 2: METİN LİSTESİ ---
                document.add(new AreaBreak()); // Yeni sayfaya geç
                document.add(new Paragraph("Derslik: " + derslikKodu + " (Metin Oturma Listesi)")
                        .setFont(titleFont).setFontSize(16)
                        .setTextAlignment(TextAlignment.CENTER));
                document.add(new Paragraph(
                        String.format("Derslik Düzeni: %d Sıra x %d Sütun (Fiziksel Koltuk/Sıra: %d)",
                                ilgiliDerslik.siraSayisiDikey,
                                ilgiliDerslik.siraSayisiYatay,
                                ilgiliDerslik.siraKapasitesi // Fiziksel kapasiteyi göster
                        ))
                        .setFont(regularFont).setFontSize(10)
                        .setTextAlignment(TextAlignment.CENTER));
                document.add(new Paragraph(kapasiteBilgisi)
                        .setFont(regularFont).setFontSize(10)
                        .setTextAlignment(TextAlignment.CENTER));
                document.add(new Paragraph(" ")); // Boşluk
                addStudentListToPdf(document, ilgiliDerslik, derslikOgrencileri, regularFont);
            }

            document.close();
            JOptionPane.showMessageDialog(this,
                    "PDF başarıyla oluşturuldu!\nDosya: " + pdfFileName,
                    "Başarılı",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "PDF oluşturulurken hata: " + e.getMessage(),
                    "Hata",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * KYS'den sınav için öğrenci dağılımını alır
     */
    private Map<String, ArrayList<String>> getStudentDistribution(KYS.Exam_Info exam) {
        Map<String, ArrayList<String>> distribution = new HashMap<>();
        if (exam.exam_student_count_list == null || exam.exam_student_count_list.isEmpty()) {
            System.out.println("UYARI: " + exam.exam_course_id + " için KYS'de öğrenci dağılımı bulunamadı!");
            return distribution;
        }
        ArrayList<String> tumOgrenciler = getStudentsFromDatabase(exam.exam_course_id);
        int ogrenciIndex = 0;
        for (Map.Entry<String, Integer> entry : exam.exam_student_count_list.entrySet()) {
            String derslikKodu = entry.getKey();
            int ogrenciSayisi = entry.getValue(); // Bu KYS'den gelen efektif öğrenci sayısıdır
            ArrayList<String> derslikOgrencileri = new ArrayList<>();
            for (int i = 0; i < ogrenciSayisi && ogrenciIndex < tumOgrenciler.size(); i++) {
                derslikOgrencileri.add(tumOgrenciler.get(ogrenciIndex));
                ogrenciIndex++;
            }
            distribution.put(derslikKodu, derslikOgrencileri);
            System.out.println(" -> " + derslikKodu + " için " + derslikOgrencileri.size() + " öğrenci PDF'e eklendi");
        }
        return distribution;
    }

    /**
     * Veritabanından dersi alan öğrencileri çeker
     */
    private ArrayList<String> getStudentsFromDatabase(String courseId) {
        ArrayList<String> students = new ArrayList<>();
        String sql = "SELECT o.numara, o.isim " +
                "FROM ogrenci o " +
                "JOIN ogrenci_ders od ON o.numara = od.ogrenci_numara " +
                "WHERE od.ders_kod = ? " +
                "ORDER BY o.numara";
        try (Connection conn = DriverManager.getConnection(url, user, pass);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String ogrNo = rs.getString("numara");
                    String ad = rs.getString("isim");
                    students.add(ogrNo + " - " + ad);
                }
            }
        } catch (SQLException e) {
            System.err.println("Öğrenci listesi alınırken SQL hatası: " + e.getMessage());
        }
        System.out.println("Dbg PDF: " + courseId + " dersi için " + students.size() + " öğrenci bulundu.");
        return students;
    }

    /**
     * PDF üzerine boş oturma planını çizer (YENİ KURALA GÖRE GÜNCELLENDİ)
     */
    private void drawEmptySeatingPlan(PdfCanvas canvas, Rectangle pageSize, Derslik derslik) throws Exception {
        float panelW = pageSize.getWidth() * 0.9f;
        float panelH = pageSize.getHeight() * 0.6f;
        float pageMarginX = pageSize.getWidth() * 0.05f;
        float pageMarginY = pageSize.getHeight() * 0.25f;
        double hucreGenisligi = panelW / derslik.siraSayisiYatay;
        double hucreYuksekligi = panelH / derslik.siraSayisiDikey;
        float masaW = (float)(hucreGenisligi * 0.8);
        float boslukX = (float)(hucreGenisligi * 0.2);
        float masaH = (float)(hucreYuksekligi * 0.4);
        float boslukY = (float)(hucreYuksekligi * 0.6);
        if (masaW < 10) masaW = 10; if (masaH < 5) masaH = 5;
        if (boslukX < 2) boslukX = 2; if (boslukY < 10) boslukY = 10;
        float toplamW = (derslik.siraSayisiYatay * masaW) + ((derslik.siraSayisiYatay - 1) * boslukX);
        float toplamH = (derslik.siraSayisiDikey * (masaH + boslukY));
        float startX = pageMarginX + (panelW - toplamW) / 2;
        float startY = pageSize.getHeight() - pageMarginY - (panelH - toplamH) / 2 - masaH;

        // Fiziksel kapasite (örn: 4)
        int siraKapasitesi = derslik.getSiraKapasitesi();

        // --- YENİ KURAL ---
        // Hangi koltukların kullanılacağını belirle
        ArrayList<Integer> kullanilacakKoltuklar = new ArrayList<>();
        if (siraKapasitesi <= 2) {
            kullanilacakKoltuklar.add(0); // Sadece ilk koltuk
        } else {
            kullanilacakKoltuklar.add(0); // İlk koltuk
            kullanilacakKoltuklar.add(siraKapasitesi - 1); // Son koltuk
        }
        // --- KURAL SONU ---

        canvas.saveState();
        canvas.setLineWidth(0.5f);

        for (int i = 0; i < derslik.siraSayisiDikey; i++) {
            for (int j = 0; j < derslik.siraSayisiYatay; j++) {
                float masaX = startX + (j * (masaW + boslukX));
                float masaY = startY - (i * (masaH + boslukY));

                // 1. Masa çerçevesi
                canvas.setStrokeColor(ColorConstants.LIGHT_GRAY);
                canvas.roundRectangle(masaX, masaY, masaW, masaH, 3);
                canvas.stroke();

                // 2. Masa bölmesi
                if (siraKapasitesi > 1) {
                    for (int k = 1; k < siraKapasitesi; k++) {
                        float bolmeX = masaX + (masaW * k / siraKapasitesi);
                        canvas.moveTo(bolmeX, masaY).lineTo(bolmeX, masaY + masaH).stroke();
                    }
                }

                // 3. Koltukları Çiz
                float koltukW = (float)(masaW * (0.8 / siraKapasitesi));
                float koltukH = koltukW;
                float koltukOffsetY = masaH + (boslukY * 0.1f);
                canvas.setLineWidth(1.5f);

                for (int k = 0; k < siraKapasitesi; k++) {
                    int bolmeBaslangicX = (int)(masaX + (masaW * k / siraKapasitesi));
                    int bolmeGenisligi = (int)(masaW / siraKapasitesi);
                    int koltukX = bolmeBaslangicX + (bolmeGenisligi - (int)koltukW) / 2;

                    // Koltuk çerçevesi
                    // Kullanılmayacak koltukları daha soluk çiz
                    if (kullanilacakKoltuklar.contains(k)) {
                        canvas.setStrokeColor(ColorConstants.DARK_GRAY); // Aktif koltuk
                    } else {
                        canvas.setStrokeColor(ColorConstants.LIGHT_GRAY); // Pasif koltuk
                    }

                    canvas.rectangle(koltukX, masaY - koltukOffsetY, koltukW, koltukH);
                    canvas.stroke();
                }
                canvas.setLineWidth(0.5f);
            }
        }
        canvas.restoreState();
    }

    /**
     * (YENİ KURALA GÖRE GÜNCELLENDİ) Öğrencileri görsel plana yerleştirir.
     */
    private void placeStudentsOnVisualPlan(PdfCanvas canvas, Rectangle pageSize, Derslik derslik, ArrayList<String> ogrenciler, PdfFont font) throws Exception {
        if (ogrenciler == null || ogrenciler.isEmpty()) return;

        // Boyut hesaplamaları (drawEmptySeatingPlan ile aynı)
        float panelW = pageSize.getWidth() * 0.9f;
        float panelH = pageSize.getHeight() * 0.6f;
        float pageMarginX = pageSize.getWidth() * 0.05f;
        float pageMarginY = pageSize.getHeight() * 0.25f;
        double hucreGenisligi = panelW / derslik.siraSayisiYatay;
        double hucreYuksekligi = panelH / derslik.siraSayisiDikey;
        float masaW = (float)(hucreGenisligi * 0.8);
        float boslukX = (float)(hucreGenisligi * 0.2);
        float masaH = (float)(hucreYuksekligi * 0.4);
        float boslukY = (float)(hucreYuksekligi * 0.6);
        if (masaW < 10) masaW = 10; if (masaH < 5) masaH = 5;
        if (boslukX < 2) boslukX = 2; if (boslukY < 10) boslukY = 10;
        float toplamW = (derslik.siraSayisiYatay * masaW) + ((derslik.siraSayisiYatay - 1) * boslukX);
        float toplamH = (derslik.siraSayisiDikey * (masaH + boslukY));
        float startX = pageMarginX + (panelW - toplamW) / 2;
        float startY = pageSize.getHeight() - pageMarginY - (panelH - toplamH) / 2 - masaH;

        // Fiziksel kapasite (örn: 4)
        int siraKapasitesi = derslik.getSiraKapasitesi();
        int ogrenciIndex = 0;

        // --- YENİ KURAL ---
        // Hangi koltuklara öğrenci yerleştirileceğini belirle
        ArrayList<Integer> yerlesimIndexleri = new ArrayList<>();
        if (siraKapasitesi <= 2) {
            yerlesimIndexleri.add(0); // Sadece ilk koltuk
        } else {
            yerlesimIndexleri.add(0); // İlk koltuk
            yerlesimIndexleri.add(siraKapasitesi - 1); // Son koltuk
        }
        // --- KURAL SONU ---

        canvas.saveState();

        float FONT_SIZE = 7;
        if (siraKapasitesi >= 4) { FONT_SIZE = 5; }

        for (int i = 0; i < derslik.siraSayisiDikey && ogrenciIndex < ogrenciler.size(); i++) {
            for (int j = 0; j < derslik.siraSayisiYatay && ogrenciIndex < ogrenciler.size(); j++) {
                float masaX = startX + (j * (masaW + boslukX));
                float masaY = startY - (i * (masaH + boslukY));

                float koltukW = (float)(masaW * (0.8 / siraKapasitesi));
                float koltukH = koltukW;
                float koltukOffsetY = masaH + (boslukY * 0.1f);

                // KURAL: 'k' indeksi yerine 'yerlesimIndexleri' listesini döngüye al
                for (int k : yerlesimIndexleri) {

                    if (ogrenciIndex >= ogrenciler.size()) break; // Öğrenci bittiyse bu masayı atla

                    int bolmeBaslangicX = (int)(masaX + (masaW * k / siraKapasitesi));
                    int bolmeGenisligi = (int)(masaW / siraKapasitesi);
                    int koltukX = bolmeBaslangicX + (bolmeGenisligi - (int)koltukW) / 2;
                    float koltukY = masaY - koltukOffsetY;

                    // Dolu koltuğu boya
                    canvas.setFillColor(new DeviceRgb(255, 100, 100)); // Kırmızı
                    canvas.rectangle(koltukX, koltukY, koltukW, koltukH);
                    canvas.fillStroke();

                    // Öğrenci bilgisini masanın içine yazdır
                    String ogrenciInfo = ogrenciler.get(ogrenciIndex);
                    int maxLen = (int)(masaW / siraKapasitesi / (FONT_SIZE * 0.5));
                    if (maxLen < 10) maxLen = 10;
                    if(ogrenciInfo.length() > maxLen) ogrenciInfo = ogrenciInfo.substring(0, maxLen-1) + ".";

                    canvas.beginText();
                    canvas.setFontAndSize(font, FONT_SIZE);
                    canvas.setFillColor(ColorConstants.BLACK);

                    // Yazıyı masanın ilgili bölesine ortala
                    float textX = bolmeBaslangicX + (bolmeGenisligi / 2);
                    float textY = masaY + masaH / 2 - (FONT_SIZE / 2);
                    float textWidth = font.getWidth(ogrenciInfo, FONT_SIZE);
                    float textStartX = textX - (textWidth / 2);
                    if (textStartX < bolmeBaslangicX) textStartX = bolmeBaslangicX;
                    if (textStartX + textWidth > bolmeBaslangicX + bolmeGenisligi) textStartX = bolmeBaslangicX;
                    canvas.moveText(textStartX, textY);
                    canvas.showText(ogrenciInfo);
                    canvas.endText();

                    ogrenciIndex++; // Sadece yerleştirilen öğrenci için arttır
                }
            }
        }
        canvas.restoreState();
    }


    /**
     * (YENİ KURALA GÖRE GÜNCELLENDİ) Öğrenci oturma düzenini PDF'e metin listesi olarak ekler.
     */
    private void addStudentListToPdf(Document document, Derslik derslik, ArrayList<String> ogrenciler, PdfFont font) {
        if (ogrenciler == null || ogrenciler.isEmpty()) {
            document.add(new Paragraph("Bu derslik için atanmış öğrenci bulunmamaktadır.")
                    .setFont(font).setFontSize(10).setFontColor(ColorConstants.RED));
            return;
        }

        List pdfList = new List()
                .setFont(font)
                .setFontSize(9)
                .setSymbolIndent(10)
                .setListSymbol("• ");

        // Fiziksel kapasite (örn: 4)
        int siraKapasitesi = derslik.getSiraKapasitesi();
        int ogrenciIndex = 0;

        // --- YENİ KURAL ---
        // Hangi pozisyonların kullanılacağını ve isimlerini belirle
        ArrayList<Integer> yerlesimIndexleri = new ArrayList<>();
        Map<Integer, String> pozisyonIsimleri = new HashMap<>();

        if (siraKapasitesi <= 2) {
            yerlesimIndexleri.add(0); // Sadece ilk koltuk
            // Sira kapasitesi 1 veya 2 ise özel bir pozisyon ismi yazmaya gerek yok
            pozisyonIsimleri.put(0, "");
        } else {
            yerlesimIndexleri.add(0); // İlk koltuk
            pozisyonIsimleri.put(0, " (Sol Kenar)");
            yerlesimIndexleri.add(siraKapasitesi - 1); // Son koltuk
            pozisyonIsimleri.put(siraKapasitesi - 1, " (Sağ Kenar)");
        }
        // --- KURAL SONU ---


        // Dersliğin düzenine göre döngü kur
        for (int i = 0; i < derslik.siraSayisiDikey; i++) { // Dikey sıra
            for (int j = 0; j < derslik.siraSayisiYatay; j++) { // Yatay sütun

                if (ogrenciIndex >= ogrenciler.size()) break;

                String siraKonum = String.format("Sıra %d, Sütun %d", (i + 1), (j + 1));

                // KURAL: 'k' indeksi yerine 'yerlesimIndexleri' listesini döngüye al
                for (int k : yerlesimIndexleri) {
                    if (ogrenciIndex < ogrenciler.size()) {
                        String pozisyon = pozisyonIsimleri.get(k);
                        String yazi = String.format("%s%s: %s",
                                siraKonum, pozisyon, ogrenciler.get(ogrenciIndex));
                        pdfList.add(new ListItem(yazi));
                        ogrenciIndex++;
                    } else {
                        break;
                    }
                }
            }
            if (ogrenciIndex >= ogrenciler.size()) break;
        }

        document.add(pdfList);
    }


    // Derslik koduna göre Derslik nesnesini bulur
    private Derslik findDerslikByKod(String kod) {
        String sql = "SELECT * FROM derslik WHERE kod = ?";
        try (Connection conn = DriverManager.getConnection(url, user, pass);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, kod);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Derslik(
                            rs.getString("bolum"),
                            rs.getString("kod"),
                            rs.getString("isim"),
                            rs.getInt("enine_sira"),
                            rs.getInt("boyuna_sira"),
                            rs.getInt("sira_koltuk"), // Fiziksel kapasite
                            rs.getInt("toplam_kapasite") // Efektif kapasite
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.err.println("UYARI: '" + kod + "' kodlu derslik veritabannda bulunamadı!");
        return null;
    }

    // Tema uygulama
    private void applyTheme() {
        Color background, foreground, buttonBg, listBg, listSelBg, listSelFg, themeButtonBg;
        if (isDarkMode) {
            background = Color.BLACK; foreground = Color.WHITE;
            buttonBg = new Color(30, 30, 30); listBg = new Color(25, 25, 25);
            listSelBg = new Color(0, 80, 150); listSelFg = Color.WHITE;
            themeButtonBg = new Color(50, 50, 50); themeButton.setText("L");
        } else {
            background = Color.WHITE; foreground = Color.BLACK;
            buttonBg = new Color(230, 230, 230); listBg = Color.WHITE;
            listSelBg = new Color(180, 210, 255); listSelFg = Color.BLACK;
            themeButtonBg = new Color(200, 200, 200); themeButton.setText("D");
        }
        this.getContentPane().setBackground(background);
        geriButton.setBackground(buttonBg);
        geriButton.setForeground(foreground);
        geriButton.setBorder(BorderFactory.createLineBorder(foreground, 1));
        themeButton.setBackground(themeButtonBg);
        themeButton.setForeground(foreground);
        themeButton.setBorder(BorderFactory.createLineBorder(foreground, 1));
        pdfButton.setBackground(new Color(40, 167, 69));
        pdfButton.setForeground(Color.WHITE);
        pdfButton.setBorder(BorderFactory.createLineBorder(foreground, 1));
        scrollPane.getViewport().setBackground(listBg);
        scrollPane.setBorder(BorderFactory.createLineBorder(foreground, 2));
        sinavListesi.setBackground(listBg);
        sinavListesi.setForeground(foreground);
        sinavListesi.setSelectionBackground(listSelBg);
        sinavListesi.setSelectionForeground(listSelFg);
    }

    void updateComponentBounds() {
        int w = this.getContentPane().getWidth();
        int h = this.getContentPane().getHeight();
        float aw = (float) w / DEFAULT_WIDTH;
        float ah = (float) h / DEFAULT_HEIGHT;
        int fontSize = (int)(18 * Math.min(aw, ah));
        geriButton.setFont(new Font("Arial", Font.BOLD, fontSize));
        themeButton.setFont(new Font("Arial", Font.BOLD, fontSize));
        pdfButton.setFont(new Font("Arial", Font.BOLD, fontSize));
        sinavListesi.setFont(new Font("Arial", Font.PLAIN, (int)(fontSize * 0.9)));
        geriButton.setBounds((int)(20 * aw), (int)(20 * ah), (int)(150 * aw), (int)(45 * ah));
        themeButton.setBounds((int)(1205 * aw), (int)(20 * ah), (int)(55 * aw), (int)(55 * ah));
        scrollPane.setBounds((int)(20 * aw), (int)(80 * ah), (int)(1240 * aw), (int)(550 * ah));
        pdfButton.setBounds((int)(440 * aw), (int)(640 * ah), (int)(400 * aw), (int)(50 * ah));
        this.revalidate();
        this.repaint();
    }

    // JList'teki her bir sınavın daha güzel görünmesini sağlar
    // JList'teki her bir sınavın daha güzel görünmesini sağlar
    static class ExamListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof KYS.Exam_Info) {
                KYS.Exam_Info exam = (KYS.Exam_Info) value;
                if (exam.exam_course_id != null && !exam.exam_course_id.isEmpty()) {
                    // --- DÜZELTME BURADA ---
                    String text = String.format("<html><b>%s (%s)</b><br>" +
                                    "Tarih: %s | Saat: %s | Süre: %d dk<br>" + // '+' eklendi
                                    "Hoca: %s | Odalar: %s</html>",           // Kapanış etiketi düzeltildi, fazla tırnak kaldırıldı
                            exam.exam_course_name, exam.exam_course_id,
                            exam.exam_date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                            LocalTime.of(9, 0).plusMinutes(exam.exam_time).format(DateTimeFormatter.ofPattern("HH:mm")),
                            exam.exam_duration,
                            exam.exam_course_teacher,
                            String.join(", ", exam.exam_roomlist));
                    // --- DÜZELTME SONU ---
                    label.setText(text);
                } else {
                    label.setText(value.toString());
                }
                label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            }
            return label;
        }
    }

    // Derslik sınıfı (vis3Frame'den kopyalandı)
    static class Derslik {
        String bolumAdi, derslikKodu, isim;
        int siraSayisiYatay, siraSayisiDikey;
        int siraKapasitesi; // Fiziksel kapasite (örn: 4)
        int toplamKapasite; // Efektif kapasite (örn: 200)

        Derslik(String ba, String dk, String i, int yatay, int dikey, int siraKap, int toplam) {
            this.bolumAdi = ba;
            this.derslikKodu = dk;
            this.isim = i;
            this.siraSayisiYatay = yatay;
            this.siraSayisiDikey = dikey;
            this.siraKapasitesi = siraKap; // Fiziksel (örn: 4)
            this.toplamKapasite = toplam; // Efektif (örn: 200)
        }

        // Bu metot FİZİKSEL kapasiteyi döndürür (örn: 4)
        public int getSiraKapasitesi() {
            return this.siraKapasitesi;
        }
    }
}