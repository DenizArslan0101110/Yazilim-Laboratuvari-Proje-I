import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.*;
import java.util.Scanner;

public class Main
{
    // good old main
    public static void main(String[] args)
    {
        // excel here
        String filePath = "example.xlsx";

        // Create workbook and sheet
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

        // Read back the file
        try (FileInputStream fis = new FileInputStream(filePath))
        {
            Workbook readWorkbook = new XSSFWorkbook(fis);
            Sheet readSheet = readWorkbook.getSheetAt(0);
            Row readRow = readSheet.getRow(0);
            Cell readCell = readRow.getCell(0);
            System.out.println("Read from Excel: " + readCell.getStringCellValue());
            readWorkbook.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        // Database yoksa oluşturur içindeki tablolar ve admin ile beraber
        SQL_Init.Initialize();

        // Hesap bilgilerini kontrol eder
        Auth.CheckAccInfo();

        // will be deleted once frontend makes it here
        Scanner klavye = new Scanner(System.in);
        System.out.println("Derslik bilgilerini giriniz:\n(isim, kod, enine, boyuna, koltuk, kapasite, bolum)");
        String isim = klavye.nextLine();
        String kod = klavye.nextLine();
        int enine_sira = klavye.nextInt();
        int boyuna_sira = klavye.nextInt();
        int sira_koltuk = klavye.nextInt();
        int toplam_kapasite = klavye.nextInt();
        klavye.nextLine();
        String bolum = klavye.nextLine();

        // derslik bilgisi gönderir
        Room.SendToDatabaseOneClassroom(isim, kod, enine_sira, boyuna_sira, sira_koltuk, toplam_kapasite, bolum);
    }
}