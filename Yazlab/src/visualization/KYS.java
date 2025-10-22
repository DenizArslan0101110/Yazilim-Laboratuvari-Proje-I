package visualization;

// Gerekli importlar
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.JOptionPane;         // <-- EKLENMİŞTİ
import java.time.format.DateTimeFormatter; // <-- EKLENMİŞTİ
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class KYS
{
    static String url = "jdbc:mysql://localhost:3306/muhendislik_fakulte?allowPublicKeyRetrieval=true&serverTimezone=UTC&useSSL=false";
    static String user = "root";
    static String pass = "yazlab1";

    static final int DAY_LENGTH = 480;

    // --- Arayüzden Gelen Değerler ---
    static LocalDate restrict_starting_date;
    static LocalDate restrict_ending_date;
    static ArrayList<LocalDate> restrict_banned_days;
    static int restrict_type_of_exam;
    static int restrict_break_duration;
    static String restrict_department_name;
    static boolean restrict_multiple_exams_allowed = false;
    static float restrict_classroom_fill_percentage = 0.5f;
    static int restrict_default_exam_duration = 75;
    static ArrayList<String> restrict_banned_courses = new ArrayList<>();
    static Map<String, Integer> restrict_specific_durations = new HashMap<>();

    // --- Hesaplanan Değerler ---
    static ArrayList<LocalDate> restrict_allowed_days = new ArrayList<>();

    // --- Ana Veri Yapıları ---
    static ArrayList<Exam_Info> exams_list = new ArrayList<>();
    static ArrayList<Room_Info> rooms_list = new ArrayList<>();
    static ArrayList<Course_Info> courses_list = new ArrayList<>();
    static ArrayList<Course_Info> courses_list_neo = new ArrayList<>();
    static Room_Info busy_room = new Room_Info("BUSY", "Sanal Çakışma Odası", Integer.MAX_VALUE);

    // İzin verilen günleri hesaplar
    static ArrayList<LocalDate> ExtractAllowedDays()
    {
        restrict_allowed_days.clear();
        if (restrict_starting_date == null || restrict_ending_date == null || restrict_banned_days == null) {
            System.err.println("Hata: ExtractAllowedDays çağrılırken tarihler veya yasaklı günler null!");
            return restrict_allowed_days;
        }
        LocalDate test_date = restrict_starting_date;
        while (!test_date.isAfter(restrict_ending_date)) {
            if (!restrict_banned_days.contains(test_date)) {
                restrict_allowed_days.add(test_date);
            }
            test_date = test_date.plusDays(1);
        }
        System.out.println("Hesaplanan İzinli Gün Sayısı: " + restrict_allowed_days.size());
        return restrict_allowed_days;
    }

    // Sınavın odalarını belirler
    static ArrayList<String> ReturnExamRoomsList(Exam_Schedule exam)
    {
        int expendable_people = 0;
        ArrayList<String> room_names = new ArrayList<>();
        for (Course_Info course : courses_list) {
            if (Objects.equals(course.course_id, exam.exam_course_id)) {
                expendable_people = course.course_total_takers;
                break;
            }
        }
        if (expendable_people == 0) {
            System.err.println("Uyarı: " + exam.exam_course_id + " dersini alan öğrenci sayısı bulunamadı veya 0!");
            return room_names;
        }
        System.out.println("Debug: " + expendable_people + " öğrenci " + exam.exam_course_id + " sınavına yerleştirilecek.");
        for (Room_Info room : rooms_list) {
            if (room.room_effective_capacity >= expendable_people) {
                room_names.add(room.room_name);
                System.out.println(" -> " + room.room_name + " (" + expendable_people + " kişi)");
                expendable_people = 0;
                break;
            } else if (expendable_people > 0) {
                room_names.add(room.room_name);
                System.out.println(" -> " + room.room_name + " (" + room.room_effective_capacity + " kişi)");
                expendable_people -= room.room_effective_capacity;
            }
        }
        if (expendable_people > 0) System.err.println("Uyarı: Derslik kapasitesi yetersiz! " + exam.exam_course_id + " dersi için " + expendable_people + " öğrenci açıkta kaldı!");
        return room_names;
    }

    // Excel listesini doldurur
    static void FillOutExamsListForExcel()
    {
        exams_list.clear();
        System.out.println("Debug: Excel için sınav listesi oluşturuluyor. Busy_room sınav sayısı: " + busy_room.room_exams_list.size());
        for (Exam_Schedule exam_schedule : busy_room.room_exams_list) {
            Course_Info ilgili_ders = null;
            for (Course_Info course : courses_list) {
                if (course.course_id.equals(exam_schedule.exam_course_id)) {
                    ilgili_ders = course;
                    break;
                }
            }
            if (ilgili_ders != null) {
                ArrayList<String> odalar = ReturnExamRoomsList(exam_schedule);
                exams_list.add(new Exam_Info(
                        ilgili_ders.course_teacher, ilgili_ders.course_name, ilgili_ders.course_id,
                        exam_schedule.date, exam_schedule.starting_point, ilgili_ders.course_exam_length, odalar));
                System.out.println(" + Excel'e Eklendi: " + ilgili_ders.course_name);
            } else {
                System.err.println("Hata: " + exam_schedule.exam_course_id + " kodlu ders, ana ders listesinde bulunamadı!");
            }
        }
        System.out.println("Debug: Excel listesi oluşturuldu. Toplam sınav: " + exams_list.size());
    }

    // --- Excel Başlık Metodları ---
    static String GetExamTypeExcelTitle() { /* ... önceki kod ... */
        return switch (restrict_type_of_exam)
        {
            case 0-> "Vize";         // Kod 0 ise Vize
            case 1 -> "Final";       // Kod 1 ise Final
            case 2 -> "Bütünleme";   // Kod 2 ise Büt
            default -> "TANIMSIZ";
        };
    }
    static String GetDepartmentExcelTitle() { /* ... önceki kod ... */
        return switch (restrict_department_name)
        {
            case "bilgisayar" -> "Bilgisayar";
            case "elektrik" -> "Elektronik ve Haberleşme";
            case "kimya" -> "Kimya";
            case "jeofizik" -> "Jeofizik";
            case "yazilim" -> "Yazilim";
            default -> restrict_department_name; // Eşleşmezse ham veriyi döndür
        };
    }
    static String GetYearExcelTitle() { /* ... önceki kod ... */
        if (restrict_starting_date == null) return "TANIMSIZ AKADEMİK YIL";

        int school_start_year;
        MonthDay august_first = MonthDay.of(8,1); // Ağustos ayı dönemi belirler

        // Eğer sınav tarihi Ağustos'tan sonraysa Güz dönemidir
        if(MonthDay.from(restrict_starting_date).isAfter(august_first))
        {
            school_start_year = restrict_starting_date.getYear();
            return school_start_year + "-" + (school_start_year + 1) + " Güz"; // Format: 2025-2026 Güz
        }
        // Değilse Bahar dönemidir
        else
        {
            school_start_year = restrict_starting_date.getYear() - 1;
            return school_start_year + "-" + (school_start_year + 1) + " Bahar"; // Format: 2024-2025 Bahar
        }
    }
    static String ReturnExcelTitle() { /* ... önceki kod ... */
        return "KOU " + GetYearExcelTitle() + " " + GetDepartmentExcelTitle() + " Müh. " + GetExamTypeExcelTitle() + " Programı";
    }

    // Sınavları Excel için sıralar
    static void RankExamsBasedOnTimesForExcel()
    {
        if (exams_list.isEmpty()) return;
        for (Exam_Info exam : exams_list) { exam.excel_ranking = 1; } // Rankları sıfırla
        for (int i = 0; i < exams_list.size(); i++) {
            for (int j = 0; j < exams_list.size(); j++) {
                if (i == j) continue;
                Exam_Info exam1 = exams_list.get(i);
                Exam_Info exam2 = exams_list.get(j);
                if (exam1.exam_date.isAfter(exam2.exam_date) ||
                        (exam1.exam_date.equals(exam2.exam_date) && exam1.exam_time > exam2.exam_time)) {
                    exam1.excel_ranking++;
                }
            }
        }
        System.out.println("Debug: Excel rankları hesaplandı.");
    }

    // Excel dosyasını oluşturur ve yazar
    static void PrintProgramToExcel()
    {
        FillOutExamsListForExcel();
        RankExamsBasedOnTimesForExcel();

        if (exams_list.isEmpty()) {
            System.err.println("Excel'e yazdırılacak sınav bulunamadı!");
            JOptionPane.showMessageDialog(null, "Sınav programı oluşturulamadı veya boş.", "Uyarı", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sınav Programı");

        // Başlık Satırı (Aynı kaldı)
        Row headerRow = sheet.createRow(0); Cell titleCell = headerRow.createCell(0);
        String excelTitle = ReturnExcelTitle(); titleCell.setCellValue(excelTitle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));
        CellStyle titleStyle = workbook.createCellStyle(); Font titleFont = workbook.createFont();
        titleFont.setFontName("Arial"); titleFont.setFontHeightInPoints((short)16); titleFont.setBold(true);
        titleStyle.setFont(titleFont); titleStyle.setAlignment(HorizontalAlignment.CENTER); titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        titleCell.setCellStyle(titleStyle); headerRow.setHeightInPoints(30);

        // Sütun Başlıkları (Aynı kaldı)
        Row columnHeaderRow = sheet.createRow(1); String[] columns = {"Tarih", "Saat", "Ders Adı", "Öğretim Üyesi", "Sınıflar"};
        CellStyle columnHeaderStyle = workbook.createCellStyle(); Font columnHeaderFont = workbook.createFont();
        columnHeaderFont.setBold(true); columnHeaderStyle.setFont(columnHeaderFont); columnHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
        for (int i = 0; i < columns.length; i++) { Cell cell = columnHeaderRow.createCell(i); cell.setCellValue(columns[i]); cell.setCellStyle(columnHeaderStyle); }

        // Veri Satırları (Formatlamalar dahil aynı kaldı)
        LocalTime baseTime = LocalTime.of(9, 0);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        CellStyle dataCellStyle = workbook.createCellStyle(); dataCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        CellStyle dateCellStyle = workbook.createCellStyle(); dateCellStyle.cloneStyleFrom(dataCellStyle); dateCellStyle.setAlignment(HorizontalAlignment.CENTER);
        CellStyle timeCellStyle = workbook.createCellStyle(); timeCellStyle.cloneStyleFrom(dataCellStyle); timeCellStyle.setAlignment(HorizontalAlignment.CENTER);

        System.out.println("Dbg Xls: Excel satırları yazdırılıyor. Toplam: " + exams_list.size());
        for (int rank = 1; rank <= exams_list.size(); rank++) {
            for (Exam_Info exam : exams_list) {
                if (exam.excel_ranking == rank) {
                    Row dataRow = sheet.createRow(rank + 1);
                    Cell dateCell = dataRow.createCell(0); dateCell.setCellValue(exam.exam_date.format(dateFormatter)); dateCell.setCellStyle(dateCellStyle);
                    Cell timeCell = dataRow.createCell(1); LocalTime examStartTime = baseTime.plusMinutes(exam.exam_time); timeCell.setCellValue(examStartTime.format(timeFormatter)); timeCell.setCellStyle(timeCellStyle);
                    Cell courseNameCell = dataRow.createCell(2); courseNameCell.setCellValue(exam.exam_course_name); courseNameCell.setCellStyle(dataCellStyle);
                    Cell teacherCell = dataRow.createCell(3); teacherCell.setCellValue(exam.exam_course_teacher); teacherCell.setCellStyle(dataCellStyle);
                    Cell roomsCell = dataRow.createCell(4); roomsCell.setCellValue(String.join(", ", exam.exam_roomlist)); roomsCell.setCellStyle(dataCellStyle);
                    System.out.println("  -> Yazıldı (Rank " + rank + "): " + exam.exam_course_name);
                    break;
                }
            }
        }

        // Sütun Genişlikleri (Aynı kaldı)
        for (int i = 0; i < columns.length; i++) { if (i==4) { sheet.setColumnWidth(i, 6000); } else { sheet.autoSizeColumn(i); } }

        // Dosyaya Yazma (Aynı kaldı, sadece mesaj eklendi)
        try {
            String fileName = "KOU_" + GetYearExcelTitle().replace(" ", "_") + "_" + GetExamTypeExcelTitle() + "_Programi.xlsx";
            FileOutputStream fileOut = new FileOutputStream(fileName);
            workbook.write(fileOut); workbook.close(); fileOut.close();
            System.out.println("Excel dosyası başarıyla oluşturuldu: " + fileName);
            JOptionPane.showMessageDialog(null, "Sınav programı başarıyla oluşturuldu!\nDosya Adı: " + fileName, "Başarılı", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Excel dosyası yazılırken hata oluştu: " + e.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Sınavları odalara dağıtır
    static void MakeTheDamnProgramAlready()
    {
        MakeExamProgramOnBusyRoom();
        if (busy_room.room_exams_list.isEmpty() || rooms_list.isEmpty()) {
            System.err.println("Hata: Sınavlar veya odalar yüklenemediği için dağıtım yapılamıyor.");
            return;
        }
        System.out.println("Debug: Sınavlar odalara dağıtılıyor...");
        for (Room_Info room : rooms_list) { room.room_exams_list.clear(); } // Odaları temizle

        for (Exam_Schedule busy_exam : busy_room.room_exams_list) {
            int remaining_students = busy_exam.seats_filled;
            System.out.println(" Dağıtılıyor: " + busy_exam.exam_course_id + " (" + remaining_students + " öğrenci)");
            for (Room_Info room : rooms_list) {
                if (remaining_students > 0 && IsRoomAvailable(room, busy_exam.date, busy_exam.starting_point, busy_exam.length)) {
                    int students_for_this_room = Math.min(remaining_students, room.room_effective_capacity);
                    Exam_Schedule room_exam = new Exam_Schedule(busy_exam.date, busy_exam.starting_point, busy_exam.length, students_for_this_room, busy_exam.exam_course_id);
                    room.room_exams_list.add(room_exam);
                    remaining_students -= students_for_this_room;
                    System.out.println("   -> " + room.room_name + " eklendi (" + students_for_this_room + " kişi)");
                }
                if (remaining_students <= 0) break;
            }
            if (remaining_students > 0) System.err.println("   !! Kapasite yetersiz! " + busy_exam.exam_course_id + " için " + remaining_students + " öğrenci açıkta kaldı!");
        }
        System.out.println("Debug: Sınavların odalara dağıtımı tamamlandı.");
    }

    // Oda uygun mu kontrolü
    static boolean IsRoomAvailable(Room_Info room, LocalDate date, int startTime, int duration) {
        for (Exam_Schedule existing_exam : room.room_exams_list) {
            if (existing_exam.date.equals(date)) {
                int existingStart = existing_exam.starting_point;
                int existingEnd = existingStart + existing_exam.length + restrict_break_duration;
                int newStart = startTime;
                int newEnd = newStart + duration + restrict_break_duration;
                if (!(newEnd <= existingStart || newStart >= existingEnd)) { // Çakışma varsa
                    return false;
                }
            }
        }
        return true; // Uygun
    }

    // busy_room üzerinde ana programı oluşturur
    static void MakeExamProgramOnBusyRoom()
    {
        busy_room.room_exams_list.clear();
        courses_list_neo = new ArrayList<>(); // Planlanacak ders listesi
        if (courses_list == null || courses_list.isEmpty()) {
            System.err.println("Hata: Ana ders listesi (courses_list) boş veya null!");
            return;
        }
        // Yasaklı olmayan dersleri neo listesine ekle
        for (Course_Info course : courses_list) {
            if (restrict_banned_courses == null || !restrict_banned_courses.contains(course.course_id)) {
                courses_list_neo.add(course);
            } else {
                System.out.println("Debug: Yasaklı ders planlamaya dahil edilmiyor: " + course.course_name);
            }
        }
        System.out.println("Debug: Planlanacak ders sayısı: " + courses_list_neo.size());
        if (courses_list_neo.isEmpty()) {
            System.out.println("Uyarı: Planlanacak hiç ders bulunamadı.");
            return;
        }
        // Planlama döngüsü
        while (!courses_list_neo.isEmpty()) {
            if (!ScheduleAnExam(busy_room)) { // Bir sınav planlamayı dene
                if (!courses_list_neo.isEmpty()) { // Eğer ders bitmediyse ama planlanamadıysa
                    System.err.println("Hata: " + courses_list_neo.get(0).course_name + " dersi için uygun zaman bulunamadı! Programlama durduruldu.");
                    break; // Döngüyü kır
                }
            }
        }
        System.out.println("Debug: Busy_room üzerinde sınav programı oluşturma tamamlandı. Toplam sınav: " + busy_room.room_exams_list.size());
    }

    // Sıradaki dersi planlamaya çalışır
    static boolean ScheduleAnExam(Room_Info room) // Sadece busy_room için
    {
        if (courses_list_neo.isEmpty()) return false; // Ders kalmadıysa

        Course_Info course_to_schedule = findMostCrowdedCourse(courses_list_neo); // En kalabalık dersi seç
        if (course_to_schedule == null) return false;

        Exam_Schedule potential_exam = FindBestSlotForCourse(room, course_to_schedule); // Uygun yer bul

        if (potential_exam != null) { // Yer bulunduysa
            if (AddCourseExamToTheRoomSafe(room, potential_exam)) { // Odaya ekle
                courses_list_neo.remove(course_to_schedule); // Listeden çıkar
                System.out.println(" + Planlandı: " + potential_exam.exam_course_id + " -> " + potential_exam.date + " @ " + potential_exam.starting_point);
                return true; // Başarılı
            } else { // Eklenemediyse (hata)
                System.err.println(" !! Hata: " + potential_exam.exam_course_id + " için bulunan zaman eklenemedi! Çakışma olabilir.");
                return false; // Başarısız
            }
        } else { // Yer bulunamadıysa
            System.err.println(" !! Uyarı: " + course_to_schedule.course_id + " dersi için uygun zaman bulunamadı.");
            courses_list_neo.remove(course_to_schedule); // Bu dersi atla
            return true; // Diğer dersleri denemek için true dön
        }
    }

    // En kalabalık dersi bulur
    static Course_Info findMostCrowdedCourse(ArrayList<Course_Info> courseList) {
        if (courseList == null || courseList.isEmpty()) return null;
        Course_Info mostCrowded = courseList.get(0);
        for (int i = 1; i < courseList.size(); i++) {
            if (courseList.get(i).course_total_takers > mostCrowded.course_total_takers) {
                mostCrowded = courseList.get(i);
            }
        }
        return mostCrowded;
    }

    // Bir ders için ilk uygun zamanı bulur
    static Exam_Schedule FindBestSlotForCourse(Room_Info room, Course_Info course) {
        if (restrict_allowed_days == null || restrict_allowed_days.isEmpty()) {
            System.err.println("Hata: İzin verilen gün listesi boş!");
            return null;
        }
        int examDurationWithBreak = course.course_exam_length + restrict_break_duration;

        for (LocalDate date : restrict_allowed_days) { // İzinli günleri dolaş
            int startTime = 0; // Günün başından başla
            while (startTime + examDurationWithBreak <= DAY_LENGTH) { // Gün sonuna kadar slot ara
                boolean isSlotAvailable = true;
                // O günkü mevcut sınavlarla çakışma var mı?
                for (Exam_Schedule existing_exam : room.room_exams_list) {
                    if (existing_exam.date.equals(date)) {
                        int existingStart = existing_exam.starting_point;
                        int existingEnd = existingStart + existing_exam.length + restrict_break_duration;
                        int newStart = startTime;
                        int newEnd = newStart + examDurationWithBreak;
                        if (!(newEnd <= existingStart || newStart >= existingEnd)) { // Çakışma varsa
                            isSlotAvailable = false;
                            startTime = existingEnd; // Aramaya çakışan sınavın bitiminden devam et
                            break; // İç döngüden çık
                        }
                    }
                }
                if (isSlotAvailable) { // Uygun slot bulunduysa
                    return new Exam_Schedule(date, startTime, course.course_exam_length, course.course_total_takers, course.course_id);
                }
                // Çakışma yoksa veya çakışma bulunduysa startTime güncellendi, döngü devam eder.
                // Güvenlik: Sonsuz döngü ihtimaline karşı küçük bir artış (normalde gerekmemeli)
                // if (!isSlotAvailable) startTime++;
            }
        }
        return null; // Hiçbir günde uygun slot bulunamadı
    }

    // Kullanılmıyor olabilir
    static int FindTheNextExamWorthyHoleInTheSchedule(Room_Info room, LocalDate date, int hole_size) { /* ... önceki kod ... */
        int busy_time = DAY_LENGTH - ReturnFreeTimeInMinutesForRoomForGivenDay(room, date);
        if(DAY_LENGTH - busy_time >= hole_size) // Eşitlik de kabul edilmeli
        {
            return busy_time;
        }
        return -1; // Uygun yer yoksa -1 döner
    }

    // Kullanılmıyor olabilir
    static int FindTheMostAbundantCourseType() { /* ... önceki kod ... */
        int[] typeCounts = new int[5]; // 0'dan 4'e kadar indexler
        for(Course_Info course : courses_list_neo)
        {
            if (course.course_type >= 0 && course.course_type <= 4) {
                typeCounts[course.course_type]++;
            }
        }

        int maxCount = -1;
        int mostAbundantType = -1;
        for (int i=0; i < typeCounts.length; i++) {
            if (typeCounts[i] > maxCount) {
                maxCount = typeCounts[i];
                mostAbundantType = i;
            }
        }
        // Eğer hiç ders yoksa veya hepsi geçersizse 0 dönebilir, dikkat!
        System.out.println("Debug: En yoğun sınıf türü: " + mostAbundantType + " (" + maxCount + " adet)");
        return mostAbundantType;
    }

    // Kullanılmıyor olabilir
    static boolean HasThisExamShowedUpBefore(ArrayList<Exam_Schedule> exam_list, Exam_Schedule single_exam) { /* ... önceki kod ... */
        for (Exam_Schedule exam : exam_list)
        {
            if (single_exam.exam_course_id.equals(exam.exam_course_id))
            {
                return true;
            }
        }
        return false;
    }

    // Bir odanın belirli bir gündeki boş zamanını döndürür
    static int ReturnFreeTimeInMinutesForRoomForGivenDay(Room_Info room, LocalDate date) { /* ... önceki kod ... */
        int total_busy_time = 0;
        for(Exam_Schedule existing_exam : room.room_exams_list)
        {
            if(existing_exam.date.equals(date)) // Sadece o günkü sınavları topla
            {
                total_busy_time += (existing_exam.length + restrict_break_duration);
            }
        }
        return DAY_LENGTH - total_busy_time; // Toplam günden meşgul zamanı çıkar
    }

    // Sınavı odaya güvenle ekler
    static boolean AddCourseExamToTheRoomSafe(Room_Info room, Exam_Schedule exam) { /* ... önceki kod ... */
        if (!IsRoomAvailable(room, exam.date, exam.starting_point, exam.length)) {
            System.err.println("Hata: " + room.room_name + " odasında " + exam.date + " @ " + exam.starting_point + " zamanı zaten dolu! (" + exam.exam_course_id + ")");
            return false; // Oda uygun değilse ekleme
        }

        // Oda uygunsa ekle
        room.room_exams_list.add(exam);
        return true;
    }

    // Veritabanından derslikleri ve dersleri yükler
    static void MikeRebuildMyKids()
    {
        rooms_list.clear(); courses_list.clear(); // Listeleri temizle

        // Derslikleri Yükle (Aynı kaldı)
        String sql_rooms = "SELECT * FROM derslik";
        try (Connection connection = DriverManager.getConnection(url, user, pass); Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql_rooms)) {
            while (rs.next()) {
                String kod = rs.getString("kod"); String isim = rs.getString("isim"); int toplam_kapasite = rs.getInt("toplam_kapasite");
                Room_Info temp_room = new Room_Info(kod, isim, toplam_kapasite); rooms_list.add(temp_room);
            } System.out.println("Debug DB: " + rooms_list.size() + " adet derslik yüklendi.");
        } catch (Exception e) { System.err.println("Hata: Derslik verileri yüklenemedi!"); e.printStackTrace(); }

        // Dersleri Yükle (Süre ataması güncellendi)
        String sql_courses = "SELECT * FROM ders";
        try (Connection connection = DriverManager.getConnection(url, user, pass); Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql_courses)) {
            while (rs.next()) {
                int sinif = rs.getInt("sinif"); String isim = rs.getString("isim"); String kod = rs.getString("kod"); String hoca = rs.getString("hoca");
                // Dersin süresini özel listeden veya varsayılandan al
                int exam_duration = restrict_specific_durations.getOrDefault(kod, restrict_default_exam_duration);
                Course_Info temp_course = new Course_Info(kod, isim, hoca, sinif, exam_duration, 0); courses_list.add(temp_course);
            } System.out.println("Debug DB: " + courses_list.size() + " adet ders yüklendi.");
        } catch (Exception e) { System.err.println("Hata: Ders verileri yüklenemedi!"); e.printStackTrace(); }

        // Derslerin Öğrenci Sayılarını Yükle (Aynı kaldı)
        for (Course_Info course : courses_list) {
            String sql_count = "SELECT COUNT(*) AS student_count FROM ogrenci_ders WHERE ders_kod = ?";
            try (Connection connection = DriverManager.getConnection(url, user, pass); PreparedStatement ps = connection.prepareStatement(sql_count)) {
                ps.setString(1, course.course_id);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) course.course_total_takers = rs.getInt("student_count"); }
            } catch (Exception e) { System.err.println("Hata: " + course.course_id + " dersi için öğrenci sayısı alınamadı!"); e.printStackTrace(); }
        } System.out.println("Debug DB: Derslerin öğrenci sayıları güncellendi.");
    }

    // --- İç İçe Sınıflar ---
    public static class Course_Info { /* ... önceki kod ... */
        String course_id;
        String course_name;
        String course_teacher;
        int course_type; // Sınıf (1, 2, 3, 4) veya Seçmeli (0)
        int course_exam_length; // Dakika cinsinden
        int course_total_takers; // Sınava giren toplam öğrenci

        Course_Info(String id, String name, String teacher, int type, int length, int takers)
        {
            this.course_id = id; this.course_name = name; this.course_teacher = teacher;
            this.course_type = type; this.course_exam_length = length; this.course_total_takers = takers;
        }
        Course_Info(){ this.course_id = "undefined"; } // Boş constructor
    }
    public static class Room_Info { /* ... önceki kod ... */
        String room_id;
        String room_name;
        int room_effective_capacity; // Kullanılabilir kapasite
        ArrayList<Exam_Schedule> room_exams_list = new ArrayList<>(); // Bu odadaki sınavlar

        Room_Info(String id, String name, int capacity, ArrayList<Exam_Schedule> exams)
        {
            this.room_id = id; this.room_name = name;
            this.room_effective_capacity = capacity; this.room_exams_list = exams;
        }
        Room_Info(String id, String name, int capacity) // Sınavsız constructor
        {
            this(id, name, capacity, new ArrayList<>());
        }
        Room_Info(){ this.room_id = "undefined"; } // Boş constructor
    }
    public static class Exam_Info { /* ... önceki kod ... */
        String exam_course_teacher;
        String exam_course_name;
        String exam_course_id;
        LocalDate exam_date;
        int exam_time; // Günün başlangıcından itibaren geçen dakika (0 = 09:00)
        int exam_duration; // Dakika cinsinden
        int excel_ranking = 1; // Excel'de sıralama için
        ArrayList<String> exam_roomlist; // Sınavın yapıldığı odaların isimleri

        Exam_Info(String teacher, String name, String id, LocalDate date, int time, int duration, ArrayList<String> rooms)
        {
            this.exam_course_teacher = teacher; this.exam_course_name = name; this.exam_course_id = id;
            this.exam_date = date; this.exam_time = time; this.exam_duration = duration;
            this.exam_roomlist = rooms;
        }
        Exam_Info(){}

        // Kolay yazdırmak için toString metodu (opsiyonel)
        @Override
        public String toString() { return exam_course_name + " @ " + exam_date + " " + LocalTime.of(9,0).plusMinutes(exam_time); }
    }
    public static class Exam_Schedule { /* ... önceki kod ... */
        LocalDate date;
        int seats_filled;   // Bu odada bu sınava giren kişi sayısı
        int starting_point; // Günün başlangıcından itibaren dakika (0 = 09:00)
        int length;         // Sınavın süresi (dakika)
        String exam_course_id; // Hangi dersin sınavı olduğu

        Exam_Schedule(LocalDate date, int start, int len, int seats, String courseId)
        {
            this.date = date; this.starting_point = start; this.length = len;
            this.seats_filled = seats; this.exam_course_id = courseId;
        }
        Exam_Schedule(){ this.exam_course_id = "undefined"; }
    }
}