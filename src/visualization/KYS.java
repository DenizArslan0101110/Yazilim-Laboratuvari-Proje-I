package visualization;

// Gerekli importlar
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.JOptionPane;
import java.time.format.DateTimeFormatter;
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

    // --- DÜZELTME 2: GÜNLÜK SINAV SÜRESİ ARTIRILDI ---
    // 480 (8 saat) 39 sınav için yetersizdi. 540 (9 saat) 6 sınava izin verir (6*7=42 slot).
    static final int DAY_LENGTH = 540;

    // --- Arayüzden Gelen Değerler ---
    static LocalDate restrict_starting_date;
    static LocalDate restrict_ending_date;
    static ArrayList<LocalDate> restrict_banned_days = new ArrayList<>();
    static int restrict_type_of_exam;
    static int restrict_break_duration;
    static String restrict_department_name;
    static boolean restrict_multiple_exams_allowed = false;
    static float restrict_classroom_fill_percentage = 0.5f; // Bu değer MikeRebuildMyKids içinde artık kullanılmayacak
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

    // Sınavın odalarını belirler (VE exam_student_count_list'i doldurur)
    static ArrayList<String> ReturnExamRoomsList(Exam_Schedule exam)
    {
        int expendable_people = 0;
        ArrayList<String> room_names = new ArrayList<>();

        // DÜZELTME 3: PDF için öğrenci dağılımını tutacak haritayı temizle
        exam.exam_student_count_list.clear();

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

        // Odaları kapasiteye göre sırala (Büyükten küçüğe)
        rooms_list.sort((r1, r2) -> Integer.compare(r2.room_effective_capacity, r1.room_effective_capacity));

        for (Room_Info room : rooms_list) {
            if (expendable_people <= 0) break;

            if (room.room_effective_capacity > 0 && expendable_people > 0)
            {
                if (room.room_effective_capacity >= expendable_people) {
                    room_names.add(room.room_name);
                    // DÜZELTME 3: Dağılım haritasına ekle
                    exam.exam_student_count_list.put(room.room_name, expendable_people);
                    System.out.println(" -> " + room.room_name + " (" + expendable_people + " kişi)");
                    expendable_people = 0;
                    break;
                } else {
                    room_names.add(room.room_name);
                    // DÜZELTME 3: Dağılım haritasına ekle
                    exam.exam_student_count_list.put(room.room_name, room.room_effective_capacity);
                    System.out.println(" -> " + room.room_name + " (" + room.room_effective_capacity + " kişi)");
                    expendable_people -= room.room_effective_capacity;
                }
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

                // Exam_Info nesnesini oluştur
                Exam_Info new_exam_info = new Exam_Info(
                        ilgili_ders.course_teacher, ilgili_ders.course_name, ilgili_ders.course_id,
                        exam_schedule.date, exam_schedule.starting_point, ilgili_ders.course_exam_length, odalar);

                // DÜZELTME 3: Öğrenci dağılım haritasını da Exam_Info'ya kopyala
                new_exam_info.exam_student_count_list = exam_schedule.exam_student_count_list;

                exams_list.add(new_exam_info);
                System.out.println(" + Excel'e Eklendi: " + ilgili_ders.course_name);
            } else {
                System.err.println("Hata: " + exam_schedule.exam_course_id + " kodlu ders, ana ders listesinde bulunamadı!");
            }
        }
        System.out.println("Debug: Excel listesi oluşturuldu. Toplam sınav: " + exams_list.size());
    }

    // --- Excel Başlık Metodları ---
    static String GetExamTypeExcelTitle() {
        return switch (restrict_type_of_exam) {
            case 0-> "Vize";
            case 1 -> "Final";
            case 2 -> "Butunleme";
            default -> "TANIMSIZ";
        };
    }
    static String GetDepartmentExcelTitle() {
        return switch (restrict_department_name) {
            case "bilgisayar" -> "Bilgisayar";
            case "elektrik" -> "Elektronik_Haberlesme";
            case "kimya" -> "Kimya";
            case "jeofizik" -> "Jeofizik";
            case "yazilim" -> "Yazilim";
            default -> restrict_department_name;
        };
    }
    static String GetYearExcelTitle() {
        if (restrict_starting_date == null) return "TANIMSIZ_AKADEMIK_YIL";
        int school_start_year;
        MonthDay august_first = MonthDay.of(8,1);

        if(MonthDay.from(restrict_starting_date).isAfter(august_first)) {
            school_start_year = restrict_starting_date.getYear();
            return school_start_year + "-" + (school_start_year + 1) + "_Guz";
        } else {
            school_start_year = restrict_starting_date.getYear() - 1;
            return school_start_year + "-" + (school_start_year + 1) + "_Bahar";
        }
    }
    static String ReturnExcelTitle() {
        // Dosya adında boşluk olmaması için _ kullanıldı
        return "KOU_" + GetYearExcelTitle() + "_" + GetDepartmentExcelTitle() + "_Muh_" + GetExamTypeExcelTitle() + "_Programi";
    }

    // Sınavları Excel için sıralar
    static void RankExamsBasedOnTimesForExcel()
    {
        if (exams_list.isEmpty()) return;
        for (Exam_Info exam : exams_list) { exam.excel_ranking = 1; }
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

        // Başlık Satırı
        Row headerRow = sheet.createRow(0); Cell titleCell = headerRow.createCell(0);
        String excelTitle = ReturnExcelTitle().replace("_", " ");
        titleCell.setCellValue(excelTitle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));
        CellStyle titleStyle = workbook.createCellStyle(); Font titleFont = workbook.createFont();
        titleFont.setFontName("Arial"); titleFont.setFontHeightInPoints((short)16); titleFont.setBold(true);
        titleStyle.setFont(titleFont); titleStyle.setAlignment(HorizontalAlignment.CENTER); titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        titleCell.setCellStyle(titleStyle); headerRow.setHeightInPoints(30);

        // Sütun Başlıkları
        Row columnHeaderRow = sheet.createRow(1); String[] columns = {"Tarih", "Saat", "Ders Adı", "Öğretim Üyesi", "Sınıflar"};
        CellStyle columnHeaderStyle = workbook.createCellStyle(); Font columnHeaderFont = workbook.createFont();
        columnHeaderFont.setBold(true); columnHeaderStyle.setFont(columnHeaderFont); columnHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
        for (int i = 0; i < columns.length; i++) { Cell cell = columnHeaderRow.createCell(i); cell.setCellValue(columns[i]); cell.setCellStyle(columnHeaderStyle); }

        // Veri Satırları
        LocalTime baseTime = LocalTime.of(9, 0);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        CellStyle dataCellStyle = workbook.createCellStyle(); dataCellStyle.setVerticalAlignment(VerticalAlignment.CENTER); dataCellStyle.setWrapText(true);
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

        // Sütun Genişlikleri
        for (int i = 0; i < columns.length; i++) {
            if (i==4) sheet.setColumnWidth(i, 8000);
            else if (i==2 || i==3) sheet.setColumnWidth(i, 10000);
            else sheet.autoSizeColumn(i);
        }

        // Dosyaya Yazma
        try {
            String fileName = ReturnExcelTitle() + ".xlsx";
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
        for (Room_Info room : rooms_list) { room.room_exams_list.clear(); }

        for (Exam_Schedule busy_exam : busy_room.room_exams_list) {
            int remaining_students = busy_exam.seats_filled;
            System.out.println(" Dağıtılıyor: " + busy_exam.exam_course_id + " (" + remaining_students + " öğrenci)");

            // Odaları kapasiteye göre sırala (Büyükten küçüğe)
            rooms_list.sort((r1, r2) -> Integer.compare(r2.room_effective_capacity, r1.room_effective_capacity));

            for (Room_Info room : rooms_list) {
                if (remaining_students <= 0) break;

                if (IsRoomAvailable(room, busy_exam.date, busy_exam.starting_point, busy_exam.length)) {

                    int students_for_this_room = Math.min(remaining_students, room.room_effective_capacity);

                    Exam_Schedule room_exam = new Exam_Schedule(busy_exam.date, busy_exam.starting_point, busy_exam.length, students_for_this_room, busy_exam.exam_course_id);
                    room.room_exams_list.add(room_exam);
                    remaining_students -= students_for_this_room;
                    System.out.println("   -> " + room.room_name + " eklendi (" + students_for_this_room + " kişi)");
                }
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
                if (!(newEnd <= existingStart || newStart >= existingEnd)) {
                    return false;
                }
            }
        }
        return true;
    }

    // busy_room üzerinde ana programı oluşturur
    static void MakeExamProgramOnBusyRoom()
    {
        busy_room.room_exams_list.clear();
        courses_list_neo.clear();

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

        // --- Planlama Döngüsü ---
        while (!courses_list_neo.isEmpty()) {
            Course_Info course_to_schedule = findMostCrowdedCourse(courses_list_neo);
            if (course_to_schedule == null) break;

            Exam_Schedule potential_exam = FindBestSlotForCourse(busy_room, course_to_schedule);

            if (potential_exam != null) {
                if (AddCourseExamToTheRoomSafe(busy_room, potential_exam)) {
                    courses_list_neo.remove(course_to_schedule);
                    System.out.println(" + Planlandı: " + potential_exam.exam_course_id + " -> " + potential_exam.date + " @ " + potential_exam.starting_point);
                } else {
                    System.err.println(" !! Hata: " + potential_exam.exam_course_id + " için bulunan zaman eklenemedi! Çakışma olabilir.");
                    courses_list_neo.remove(course_to_schedule); // Hatalı dersi çıkar
                }
            } else {
                System.err.println(" !! Uyarı: " + course_to_schedule.course_id + " (" + course_to_schedule.course_total_takers + " kişi) dersi için uygun zaman bulunamadı. Bu ders program dışı kalacak.");
                courses_list_neo.remove(course_to_schedule); // Bu dersi atla
            }
        }
        System.out.println("Debug: Busy_room üzerinde sınav programı oluşturma tamamlandı. Toplam sınav: " + busy_room.room_exams_list.size());
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

        for (LocalDate date : restrict_allowed_days) {
            int startTime = 0;
            while (startTime + examDurationWithBreak <= DAY_LENGTH) {
                boolean isSlotAvailable = true;
                for (Exam_Schedule existing_exam : room.room_exams_list) {
                    if (existing_exam.date.equals(date)) {
                        int existingStart = existing_exam.starting_point;
                        int existingEnd = existingStart + existing_exam.length + restrict_break_duration;
                        int newStart = startTime;
                        int newEnd = newStart + examDurationWithBreak;
                        if (!(newEnd <= existingStart || newStart >= existingEnd)) {
                            isSlotAvailable = false;
                            startTime = existingEnd;
                            break;
                        }
                    }
                }
                if (isSlotAvailable) {
                    return new Exam_Schedule(date, startTime, course.course_exam_length, course.course_total_takers, course.course_id);
                }
                if(isSlotAvailable) {
                    startTime++;
                }
            }
        }
        return null; // Hiç uygun slot bulunamadı
    }

    // Sınavı odaya güvenle ekler
    static boolean AddCourseExamToTheRoomSafe(Room_Info room, Exam_Schedule exam) {
        if (!IsRoomAvailable(room, exam.date, exam.starting_point, exam.length)) {
            System.err.println("Hata: " + room.room_name + " odasında " + exam.date + " @ " + exam.starting_point + " zamanı zaten dolu! (" + exam.exam_course_id + ")");
            return false;
        }
        room.room_exams_list.add(exam);
        return true;
    }

    // Veritabanından derslikleri ve dersleri yükler
    static void MikeRebuildMyKids()
    {
        // Önce izin verilen günleri hesapla
        ExtractAllowedDays();

        rooms_list.clear(); courses_list.clear();

        // Derslikleri Yükle (KAPASİTE HATASI DÜZELTİLDİ)
        String sql_rooms = "SELECT * FROM derslik";
        try (Connection connection = DriverManager.getConnection(url, user, pass); Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql_rooms)) {
            while (rs.next()) {
                String kod = rs.getString("kod");
                String isim = rs.getString("isim");
                int toplam_kapasite = rs.getInt("toplam_kapasite");

                // --- DÜZELTME 1: Kapasite %100 kullanılıyor ---
                // restrict_classroom_fill_percentage çarpanı kaldırıldı.
                int effective_capacity = toplam_kapasite;
                // --- DÜZELTME SONU ---

                Room_Info temp_room = new Room_Info(kod, isim, effective_capacity);
                rooms_list.add(temp_room);
            }
            System.out.println("Debug DB: " + rooms_list.size() + " adet derslik yüklendi (Kapasite %100).");
        } catch (Exception e) { System.err.println("Hata: Derslik verileri yüklenemedi!"); e.printStackTrace(); }

        // Dersleri Yükle
        String sql_courses = "SELECT * FROM ders";
        try (Connection connection = DriverManager.getConnection(url, user, pass); Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql_courses)) {
            while (rs.next()) {
                int sinif = rs.getInt("sinif"); String isim = rs.getString("isim"); String kod = rs.getString("kod"); String hoca = rs.getString("hoca");
                int exam_duration = restrict_specific_durations.getOrDefault(kod, restrict_default_exam_duration);
                Course_Info temp_course = new Course_Info(kod, isim, hoca, sinif, exam_duration, 0); courses_list.add(temp_course);
            } System.out.println("Debug DB: " + courses_list.size() + " adet ders yüklendi.");
        } catch (Exception e) { System.err.println("Hata: Ders verileri yüklenemedi!"); e.printStackTrace(); }

        // Derslerin Öğrenci Sayılarını Yükle
        for (Course_Info course : courses_list) {
            // DÜZELTME 4: Hatalı tablo adı (ders_kayit) düzeltildi
            String sql_count = "SELECT COUNT(*) AS student_count FROM ogrenci_ders WHERE ders_kod = ?";
            try (Connection connection = DriverManager.getConnection(url, user, pass); PreparedStatement ps = connection.prepareStatement(sql_count)) {
                ps.setString(1, course.course_id);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) course.course_total_takers = rs.getInt("student_count"); }
            } catch (Exception e) { System.err.println("Hata: " + course.course_id + " dersi için öğrenci sayısı alınamadı!"); e.printStackTrace(); }
        } System.out.println("Debug DB: Derslerin öğrenci sayıları güncellendi.");
    }

    // --- İç İçe Sınıflar ---
    public static class Course_Info {
        String course_id; String course_name; String course_teacher;
        int course_type; int course_exam_length; int course_total_takers;
        Course_Info(String id, String name, String teacher, int type, int length, int takers) {
            this.course_id = id; this.course_name = name; this.course_teacher = teacher;
            this.course_type = type; this.course_exam_length = length; this.course_total_takers = takers;
        }
        Course_Info(){ this.course_id = "undefined"; }
    }
    public static class Room_Info {
        String room_id; String room_name; int room_effective_capacity;
        ArrayList<Exam_Schedule> room_exams_list = new ArrayList<>();
        Room_Info(String id, String name, int capacity) {
            this(id, name, capacity, new ArrayList<>());
        }
        Room_Info(String id, String name, int capacity, ArrayList<Exam_Schedule> exams) {
            this.room_id = id; this.room_name = name; this.room_effective_capacity = capacity; this.room_exams_list = exams;
        }
        Room_Info(){ this.room_id = "undefined"; }
    }

    // DÜZELTME 3: PDF Dağıtımı için Map Eklendi
    public static class Exam_Info {
        String exam_course_teacher; String exam_course_name; String exam_course_id;
        LocalDate exam_date; int exam_time; int exam_duration;
        int excel_ranking = 1;
        ArrayList<String> exam_roomlist;

        // YENİ EKLENDİ (PDF için öğrenci dağılımını tutar)
        Map<String, Integer> exam_student_count_list = new HashMap<>();

        Exam_Info(String teacher, String name, String id, LocalDate date, int time, int duration, ArrayList<String> rooms) {
            this.exam_course_teacher = teacher; this.exam_course_name = name; this.exam_course_id = id;
            this.exam_date = date; this.exam_time = time; this.exam_duration = duration;
            this.exam_roomlist = rooms;
        }
        Exam_Info(String teacher, String name, String id, LocalDate date, int time, int duration) {
            this(teacher, name, id, date, time, duration, new ArrayList<>());
        }
        Exam_Info(){}
        @Override public String toString() { return exam_course_name + " @ " + exam_date + " " + LocalTime.of(9,0).plusMinutes(exam_time); }
    }

    // DÜZELTME 3: PDF Dağıtımı için Map Eklendi
    public static class Exam_Schedule {
        LocalDate date; int seats_filled; int starting_point; int length;
        String exam_course_id;

        // YENİ EKLENDİ (PDF için öğrenci dağılımını tutar)
        Map<String, Integer> exam_student_count_list = new HashMap<>();

        Exam_Schedule(LocalDate date, int start, int len, int seats, String courseId) {
            this.date = date; this.starting_point = start; this.length = len;
            this.seats_filled = seats; this.exam_course_id = courseId;
        }
        Exam_Schedule(){ this.exam_course_id = "undefined"; }
    }
}