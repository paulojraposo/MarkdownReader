

// commonmark Markdown library by Atlassian:
// https://github.com/atlassian/commonmark-java
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.embed.swing.JFXPanel;
import javafx.application.Platform;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import java.net.URL;
import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
// import java.nio.file.*;

/*
TODO:
* Implement a pure Java parsing of the markdown, eliminating the need for mistletoe.
* Add "Delete HTML and Exit" button to GUI to delete the HTML produced from the markdown.
* Add drag-n-drop functionality to load HTML/MD.
* Add file chooser functionality to load HTML/MD.
*/

/*
Strategy so far is to use mistletoe to make an html file *in the same folder as the .md*, view it
with this app, then delete the file on close.

Maybe better: get this Java app to make a system call to mistletoe, which produces the HTML to
stout, which I can read into this Java app and render from there. That way it's a pure Java app,
just depends on mistletoe being installed and on the system PATH. That way no HTML file is written
to disk or stored.

See this for drag-n-drop in Swing: https://stackoverflow.com/questions/811248/how-can-i-use-drag-and-drop-in-swing-to-get-file-path
*/


public class MarkdownReader {

    static JFXPanel jfxPanel = new JFXPanel(); // Scrollable JCompenent
    static String MDText = "*Please choose an .md file...* :)";


    // With thanks to OscarRyz:
    // https://stackoverflow.com/questions/326390/how-do-i-create-a-java-string-from-the-contents-of-a-file
    private static String readFile(String filepath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader (filepath));
        String         line = null;
        StringBuilder  stringBuilder = new StringBuilder();
        String         ls = System.getProperty("line.separator");
    
        try {
            while((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append(ls);
            }
    
            return stringBuilder.toString();
        } finally {
            reader.close();
        }
    }

    private static void updateWebViewContentsWithFile(File aFile){

        // Read file to string, and set MDText to that string.
        String filePath = aFile.getAbsolutePath();

        try {
            MDText =  readFile(filePath);
        } catch (Exception e) {
            //TODO: handle exception
            MDText = "Something didn't work correctly.";
        }

        // Then we just recall initFX().
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                initFX(jfxPanel);
            }
        });

    }

    private static void createAndShowGUI() {

        JFrame frame = new JFrame("Markdown Reader");
        Container pane;
        pane = frame.getContentPane();
        pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setPreferredSize(new Dimension(520, 700));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setPreferredSize(new Dimension(520, 60));
        JButton openFileButton = new JButton("Open MD File...");
        openFileButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent arg0) {
                //Create a file chooser
                JFileChooser fc = new JFileChooser();
                int returnVal = fc.showOpenDialog(jfxPanel);
                if(returnVal == JFileChooser.APPROVE_OPTION) {
                    System.out.println("You chose to open this file: " +
                         fc.getSelectedFile().getName());
                    File chosenFile = fc.getSelectedFile();
                    updateWebViewContentsWithFile(chosenFile);
                }
            }
        });
        buttonPanel.add(openFileButton);
        frame.add(buttonPanel);

        // Add the JFXPanel to the Swing app, and initialize
        // it in its own JFX thread.
        frame.add(jfxPanel);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                initFX(jfxPanel);
            }
        });

        //Display the window.
        frame.pack();
        frame.setVisible(true);

    }

    private static void initFX(JFXPanel theJFXPanel){

        System.out.println("initFX was called.");
        
        WebView webView = new WebView();

        // Set the CSS
        // Tony Vu's answer helped: https://stackoverflow.com/questions/1480398/java-reading-a-file-from-current-directory
        String cssPath = String.valueOf(MarkdownReader.class.getResource("style.css"));
        webView.getEngine().setUserStyleSheetLocation(cssPath);

        // Set up a commonmark Markdown parser, and use it to transpile to HTML.
        Parser parser = Parser.builder().build();
        Node document = parser.parse(MDText);
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        String outHTML = String.valueOf( renderer.render(document) );
        webView.getEngine().loadContent( outHTML );

        // Set the scene to be drawn as the WebView object.
        Scene scene = new Scene(webView);
        theJFXPanel.setScene(scene);

    };

    public static void main(String[] args) {

        // System.out.println("Running...");
        setLAF();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                createAndShowGUI();
            }
        });
    }

    private static void setLAF(){
        // With thanks to BenjaminLinus,
        // https://stackoverflow.com/questions/4617615/how-to-set-nimbus-look-and-feel-in-main.
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // If Nimbus is not available, fall back to cross-platform
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}