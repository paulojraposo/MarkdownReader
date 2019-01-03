

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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;


/*
TODO:
* Add "Delete HTML and Exit" button to GUI to delete the HTML produced from the markdown.
* Add drag-n-drop functionality to load HTML/MD.
*/

/*
See this for drag-n-drop in Swing: https://stackoverflow.com/questions/811248/how-can-i-use-drag-and-drop-in-swing-to-get-file-path
*/


public class MarkdownReader {

    static JFXPanel jfxPanel = new JFXPanel(); // Scrollable JCompenent
    static String MDText = "*Please choose an .md file...* :)";
    static String htmlPath = null;


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

    private static void deleteFile(String filepath){

    }

    private static String md_to_html(String mdText){
        // Set up a commonmark Markdown parser, and use it to transpile to HTML.
        Parser parser = Parser.builder().build();
        Node document = parser.parse(mdText);
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        return String.valueOf( renderer.render(document) );
    }

    private static void makeHTMLFile(File mdFile){

        String mdFileAbsPath = mdFile.getAbsolutePath();

        // Define path to HTML file in same directory.
        Path mdPath = Paths.get(mdFileAbsPath);
        Path mdParentPath = mdPath.getParent();
        String mdFileName = mdPath.getFileName().toString();
        String[] nameParts = mdFileName.split("[.]");
        String mdFileBaseName = nameParts[0];
        String htmlFileName = mdFileBaseName + ".html";
        // Set static variable htmlPath so it's accessible outside of this method.
        htmlPath = mdParentPath.resolve(htmlFileName).toString();

        try {
            // Read the MD file to a string.
            String mdText = readFile(mdFileAbsPath);
            // Create HTML file in same folder as the MD file.
            BufferedWriter writer = new BufferedWriter(new FileWriter(htmlPath));
            // Transpile MD -> HTML.
            String htmlText = md_to_html(mdText);
            writer.write(htmlText);
            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void updateWebViewContentsWithFile(File aFile){
        
        String filePath = aFile.getAbsolutePath();

        try {
            // Read file to string, and set MDText to that string.
            MDText = readFile(filePath);
        } catch (Exception e) {
            MDText = "Something didn't work correctly.";
            e.printStackTrace();
        }

        drawFXComponents();
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
                    File chosenFile = fc.getSelectedFile();
                    // updateWebViewContentsWithFile(chosenFile);
                    makeHTMLFile(chosenFile);
                    drawFXComponents();
                }
            }
        });
        buttonPanel.add(openFileButton);
        frame.add(buttonPanel);

        // Add the JFXPanel to the Swing app, and initialize
        // it in its own JFX thread.
        frame.add(jfxPanel);
        drawFXComponents();

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    private static void drawFXComponents(){
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                initFX(jfxPanel);
            }
        });
    }

    private static void initFX(JFXPanel theJFXPanel){
        
        WebView webView = new WebView();

        // Set the CSS
        // Tony Vu's answer helped: https://stackoverflow.com/questions/1480398/java-reading-a-file-from-current-directory
        String cssPath = String.valueOf(MarkdownReader.class.getResource("style.css"));
        webView.getEngine().setUserStyleSheetLocation(cssPath);

        // Load different things depending on whether an HTML
        // file has been created.
        if (htmlPath == null){
            // First time this is run, there's no value to htmlPath.
            // So we display the simple instruction message MDText
            // is initialized to.
            String outHTML = md_to_html(MDText);
            webView.getEngine().loadContent(outHTML);
        } else {
            // Convert HTML filepath to URI for WebView.
            File f = new File(htmlPath);
            webView.getEngine().load(f.toURI().toString());
        }       

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