// single most fucked up part of the entire project, everything is in a package
// don't ask why, don't ask how, ask if it works and they shall say yes
package visualization;

import visualization.vis1Frame;
import javax.swing.*;

public class Main
{
    public static void main(String[] args)
    {
        // Database yoksa oluşturur içindeki tablolar ve admin ile beraber
        SQL_Init.Initialize();

        // vis1frame diye başlıyor
        SwingUtilities.invokeLater(vis1Frame::new);
    }
}