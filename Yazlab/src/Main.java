import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import java.sql.*;

import java.io.*;

public class Main extends Application
{

    // sample code for javafx, no idea how it works but it opens a window with a button
    @Override
    public void start(Stage primaryStage)
    {
        Button btn = new Button("Click Me");

        btn.setOnAction(event -> { System.out.println("Button clicked!"); });

        StackPane root = new StackPane();
        root.getChildren().add(btn);

        Scene scene = new Scene(root, 300, 200);

        primaryStage.setTitle("JavaFX Example");
        primaryStage.setScene(scene);
        primaryStage.show();
    }


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

        // below is mysql sample code
        SQL_Init.Initialize();
        Auth.CheckAccInfo();

        // this probaby opens the window
        launch(args);
    }
}