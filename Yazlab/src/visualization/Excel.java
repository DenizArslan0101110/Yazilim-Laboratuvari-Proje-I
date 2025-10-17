package visualization;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;

public class Excel
{
    public static void ReadExcelForCoursesAndSendToDatabase(String filepath, String bolum)
    {
        ArrayList<Ders_EX> db_icin_dersler = new ArrayList<>();
        Ders_EX temp_ders_buffer = new Ders_EX();

        String url = "jdbc:mysql://localhost:3306/muhendislik_fakulte?serverTimezone=UTC&useSSL=false";
        String user = "root";
        String pass = "yazlab1";

        try (FileInputStream fis = new FileInputStream(filepath))
        {
            int current_row = 0;
            int current_cell = 0;
            int sinif_at_this_point = 0;

            Workbook readWorkbook = new XSSFWorkbook(fis);
            Sheet sheet = readWorkbook.getSheetAt(0);
            Row row = sheet.getRow(current_row);
            Cell cell = row.getCell(current_cell);

            for(; row!=null ;current_row = current_row + 1)
            {
                row = sheet.getRow(current_row);
                for(current_cell=0; ;current_cell++)
                {

                    try {cell = row.getCell(current_cell);}
                    catch (NullPointerException _){}
                    if(cell==null || cell.getCellType()==CellType.BLANK) break;
                    String cell_string = String.valueOf(cell);
                    if(cell_string.contains("1.")){current_row++;sinif_at_this_point=1;break;}
                    if(cell_string.contains("2.")){current_row++;sinif_at_this_point=2;break;}
                    if(cell_string.contains("3.")){current_row++;sinif_at_this_point=3;break;}
                    if(cell_string.contains("4.")){current_row++;sinif_at_this_point=4;break;}
                    if(cell_string.contains("DERS")){current_row++;sinif_at_this_point=0;break;}
                    if(current_cell == 0)
                    {
                        temp_ders_buffer.kod = String.valueOf(cell);
                    }
                    else if(current_cell == 1)
                    {
                        temp_ders_buffer.isim = String.valueOf(cell);
                    }
                    else if(current_cell == 2)
                    {
                        temp_ders_buffer.hoca = String.valueOf(cell);
                    }

                }
                db_icin_dersler.add(new Ders_EX(temp_ders_buffer.isim, temp_ders_buffer.kod, temp_ders_buffer.hoca, sinif_at_this_point));
            }
            try
            {
                Class.forName("com.mysql.cj.jdbc.Driver"); // optional on modern JDKs
                try (Connection conn = DriverManager.getConnection(url, user, pass))
                {
                    String sql = "INSERT INTO ders (sinif, isim, kod, hoca) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(sql))
                    {
                        for (Ders_EX ders_info : db_icin_dersler)
                        {
                            try
                            {
                                ps.setInt(1, ders_info.sinif);
                                ps.setString(2, ders_info.isim);
                                ps.setString(3, ders_info.kod);
                                ps.setString(4, ders_info.hoca);
                                ps.executeUpdate();
                            }
                            catch (SQLIntegrityConstraintViolationException _) {}
                            catch (Exception e)
                            {
                                System.out.println("Dbg SQL: Some sort of error happened (yeah I have no idea too)");
                                e.printStackTrace();
                            }
                        }
                        System.out.println("Dbg SQL: Courses were inserted to the table.");
                    }
                    catch (Exception e) {e.printStackTrace();}
                }
                catch (Exception e) {e.printStackTrace();}
            }
            catch (Exception e) {e.printStackTrace();}
            readWorkbook.close();
        }
        catch (IOException e) {e.printStackTrace();}


    }

    public static void ReadExcelForStudentsAndSendToDatabase(String filepath)
    {
        ArrayList<Ogrenci_EX> db_icin_ogrenciler = new ArrayList<>();
        Ogrenci_EX temp_ogrenci_buffer = new Ogrenci_EX();

        String url = "jdbc:mysql://localhost:3306/muhendislik_fakulte?serverTimezone=UTC&useSSL=false";
        String user = "root";
        String pass = "yazlab1";

        // this one reads the student list
        try (FileInputStream fis = new FileInputStream(filepath))
        {
            int current_row = 1;
            int current_cell = 0;

            Workbook readWorkbook = new XSSFWorkbook(fis);
            Sheet sheet = readWorkbook.getSheetAt(0);
            Row row = sheet.getRow(current_row);
            Cell cell = row.getCell(current_cell);

            for(; row!=null ;current_row = current_row + 1)
            {
                row = sheet.getRow(current_row);
                for(current_cell=0; ;current_cell++)
                {
                    try {cell = row.getCell(current_cell);}
                    catch (NullPointerException _){}
                    if(cell==null || cell.getCellType()==CellType.BLANK) break;
                    if(current_cell == 0)
                    {
                        temp_ogrenci_buffer.numara = String.valueOf(cell);
                    }
                    else if(current_cell == 1)
                    {
                        temp_ogrenci_buffer.isim = String.valueOf(cell);
                    }
                    else if(current_cell == 2)
                    {
                        String temp_string = String.valueOf(cell);
                        temp_ogrenci_buffer.sinif = Character.getNumericValue(temp_string.charAt(0));
                    }
                    else if(current_cell == 3)
                    {
                        temp_ogrenci_buffer.ders = String.valueOf(cell);
                    }

                }
                db_icin_ogrenciler.add(new Ogrenci_EX(temp_ogrenci_buffer.numara, temp_ogrenci_buffer.isim, temp_ogrenci_buffer.sinif, temp_ogrenci_buffer.ders));

            }
            try
            {
                Class.forName("com.mysql.cj.jdbc.Driver"); // optional on modern JDKs
                try (Connection conn = DriverManager.getConnection(url, user, pass))
                {
                    String sql = "INSERT INTO ogrenci (isim, numara, sinif) VALUES (?, ?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(sql))
                    {
                        for (Ogrenci_EX ogr_ders_info : db_icin_ogrenciler)
                        {
                            try
                            {
                                ps.setString(1, ogr_ders_info.isim);
                                ps.setString(2, ogr_ders_info.numara);
                                ps.setInt(3, ogr_ders_info.sinif);
                                ps.executeUpdate();
                            }
                            catch (SQLIntegrityConstraintViolationException _) {}
                            catch (Exception e)
                            {
                                System.out.println("Dbg SQL: Some sort of error happened (yeah I have no idea too)");
                                e.printStackTrace();
                            }
                        }
                        System.out.println("Dbg SQL: Student list inserted to table.");
                    }
                    catch (Exception e) {e.printStackTrace();}
                }
                catch (Exception e) {e.printStackTrace();}
            }
            catch (Exception e) {e.printStackTrace();}
            try
            {
                Class.forName("com.mysql.cj.jdbc.Driver"); // optional on modern JDKs
                try (Connection conn = DriverManager.getConnection(url, user, pass))
                {
                    String sql = "INSERT INTO ogrenci_ders (ogrenci_numara, ders_kod) VALUES (?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(sql))
                    {
                        for (Ogrenci_EX ogr_ders_info : db_icin_ogrenciler)
                        {
                            try
                            {
                                ps.setString(1, ogr_ders_info.numara);
                                ps.setString(2, ogr_ders_info.ders);
                                ps.executeUpdate();
                            }
                            catch (SQLIntegrityConstraintViolationException _) {}
                            catch (Exception e)
                            {
                                System.out.println("Dbg SQL: Some sort of error happened (yeah I have no idea too)");
                                e.printStackTrace();
                            }
                        }
                        System.out.println("Dbg SQL: Student-Course list inserted to table.");
                    }
                    catch (Exception e) {e.printStackTrace();}
                }
                catch (Exception e) {e.printStackTrace();}
            }
            catch (Exception e) {e.printStackTrace();}




            //Row readRow = readSheet.getRow(0);
            //Cell readCell = readRow.getCell(0);
            //System.out.println("Read from Excel: " + readCell.getStringCellValue());
            readWorkbook.close();
        }
        catch (IOException e) {e.printStackTrace();}
    }
}

        /*  // Future reference, we will need this trust me
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sheet1");


            // Create row and cell
        Row row = sheet.createRow(0);
        Cell cell = row.createCell(0);
        cell.setCellValue("Hello, POI!");

            // Write to file
        try (FileOutputStream fos = new FileOutputStream(filePath))
        {
            workbook.write(fos);
            workbook.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

         */
