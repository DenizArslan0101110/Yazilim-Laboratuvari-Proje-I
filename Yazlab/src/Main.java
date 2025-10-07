import visualization.vis1Frame;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import javax.swing.*;
import java.io.*;
import java.util.Scanner;

public class Main
{
    // good old main
    public static void main(String[] args)
    {


        // Database yoksa oluşturur içindeki tablolar ve admin ile beraber
        SQL_Init.Initialize();

        // vis1frame diye başlıyor
        SwingUtilities.invokeLater(vis1Frame::new);
/*
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
*/
        // derslik bilgisi gönderir
        //Room.SendToDatabaseOneClassroom(isim, kod, enine_sira, boyuna_sira, sira_koltuk, toplam_kapasite, bolum);

        Excel.ReadExcelForCoursesAndSendToDatabase("C:\\Users\\deniz\\IdeaProjects\\Yazlab\\src\\Files\\Ders Listesi.xlsx", "bilgisayar");
        Excel.ReadExcelForStudentsAndSendToDatabase("C:\\Users\\deniz\\IdeaProjects\\Yazlab\\src\\Files\\ogrenci_listesi.xlsx");

    }
}