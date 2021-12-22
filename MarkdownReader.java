/* 
MarkdownReader, by Paulo Raposo.
*/

// commonmark Markdown library by Atlassian:
// https://github.com/atlassian/commonmark-java
// https://www.javadoc.io/doc/org.commonmark/commonmark/latest/index.html
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
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.ImageIcon;
import javax.imageio.ImageIO;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
// import java.io.FileWriter;




/*
TODO:
 Add drag-n-drop functionality to load HTML/MD. See this for drag-n-drop in Swing: https://stackoverflow.com/questions/811248/how-can-i-use-drag-and-drop-in-swing-to-get-file-path
*/


public class MarkdownReader {


    static String appName        = "MarkdownReader";
    static JFXPanel jfxPanel     = new JFXPanel(); // Scrollable JCompenent
    static String MDText         = "*Please choose an .md file...*";
    static String HTMLTranspiled = null;
    static String mdPath         = null;
    static Path mdParentPath     = null;
    static File mostRecentDir    = null;
    static ImageVisitor theImgVisitor;
    static Double zoomLevel       = 1.0;
    

    // With thanks to OscarRyz:
    // https://stackoverflow.com/questions/326390/how-do-i-create-a-java-string-from-the-contents-of-a-file
    private static String readFile(String filepath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader (filepath));
        String line = null;
        StringBuilder stringBuilder = new StringBuilder();
        String ls = System.getProperty("line.separator");
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

    private static String md_to_html(String mdText){
        // Set up a commonmark Markdown parser, and use it to transpile to HTML.
        Parser parser = Parser.builder().build();
        Node document = parser.parse(mdText);
        // Use a Commonmark Visitor class to visit all img nodes and change their file paths to absolute URIs.
        theImgVisitor = new ImageVisitor(mdParentPath);
        document.accept(theImgVisitor);
        // After image path adjustments, render the HTML.
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        return String.valueOf(renderer.render(document));
    }

    private static void makeHTML(File mdFile){

        // Determine file path information from given .md file, needed for later use in adjusting HTML image tags.
        String mdFileAbsPath = mdFile.getAbsolutePath();
        Path mdPath = Paths.get(mdFileAbsPath);
        mdParentPath = mdPath.getParent();
        String mdFileName = mdPath.getFileName().toString();

        try {
            // Read the MD file to a string.
            String mdText = readFile(mdFileAbsPath);
            // Transpile MD to HTML to show in the webview component.
            HTMLTranspiled = md_to_html(mdText);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void createAndShowGUI() {

        JFrame frame = new JFrame(appName);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setPreferredSize(new Dimension(540, 700));
        frame.setIconImage(new ImageIcon("resources/MarkdownReaderIcon.png").getImage());

        Container pane = frame.getContentPane();
        pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setPreferredSize(new Dimension(520, 86));
        buttonPanel.setMaximumSize(new Dimension(1000, 86));

        Integer buttonHeight = 74;

        JButton openFileButton = new JButton();
        openFileButton.setPreferredSize(new Dimension(200, buttonHeight));
        openFileButton.setToolTipText("Load a Markdown file, transpile it to an HTML file in the same directory, and view it here.");
        try {
            java.awt.Image img = ImageIO.read(MarkdownReader.class.getResource("resources/MDtoHTML.png"));    
            openFileButton.setIcon(new ImageIcon(img));
        } catch (Exception e) {
            e.printStackTrace();
        }        
        openFileButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent arg0) {
                //Create a file chooser
                JFileChooser fc = new JFileChooser();
                fc.setPreferredSize(new Dimension(560,600));
                // Filter for markdown files.
                FileNameExtensionFilter filter = new FileNameExtensionFilter("Markdown (\".md\" or \".MD\")", "md", "MD");
                // Open in the same directory as the last time, if there was a last time.
                if (mostRecentDir != null){
                    fc.setCurrentDirectory(mostRecentDir);
                }
                fc.setFileFilter(filter);
                int returnVal = fc.showOpenDialog(frame);
                if(returnVal == JFileChooser.APPROVE_OPTION) {
                    File chosenFile = fc.getSelectedFile();
                    // Set directory to have the next file chooser open to, if there is one.
                    mostRecentDir = new File(chosenFile.getParent());
                    // updateWebViewContentsWithFile(chosenFile);
                    mdPath = chosenFile.getAbsolutePath();
                    makeHTML(chosenFile);
                    drawFXComponents();
                }
            }
        });
        buttonPanel.add(openFileButton);

        JButton refreshButton = new JButton();
        Integer refreshButtonWidth = buttonHeight;
        refreshButton.setPreferredSize(new Dimension(refreshButtonWidth, buttonHeight));
        refreshButton.setToolTipText("Re-transpile, save, and load the HTML.");
        try {
            // java.awt.Image img = ImageIO.read(MarkdownReader.class.getResource("resources/icons8_refresh.png"));
            java.awt.Image img = ImageIO.read(MarkdownReader.class.getResource("resources/oxygen-refresh.png"));
            refreshButton.setIcon(new ImageIcon(img));
        } catch (Exception e) {
            e.printStackTrace();
        }
        refreshButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent arg0) {
                // Reload and transpile file, redraw FX.
                if (mdPath != null){
                    File mdFile = new File(mdPath);
                    makeHTML(mdFile);
                    drawFXComponents();
                }
            }
        });
        buttonPanel.add(refreshButton);

        JButton zoomInButton = new JButton();
        Integer zoomInButtonWidth = buttonHeight;
        zoomInButton.setPreferredSize(new Dimension(zoomInButtonWidth, buttonHeight));
        zoomInButton.setToolTipText("Zoom in by 10%");
        try {
            java.awt.Image img = ImageIO.read(MarkdownReader.class.getResource("resources/oxygen-zoom-in.png"));
            zoomInButton.setIcon(new ImageIcon(img));
        } catch (Exception e) {
            e.printStackTrace();
        }
        zoomInButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent arg0) {
                // Adjust zoomLevel and redraw Reload and transpile file, redraw FX.
                zoomLevel = zoomLevel + 0.1;
                drawFXComponents();
            }
        });
        buttonPanel.add(zoomInButton);

        JButton zoomResetButton = new JButton();
        Integer zoomResetButtonWidth = buttonHeight;
        zoomResetButton.setPreferredSize(new Dimension(zoomResetButtonWidth, buttonHeight));
        zoomResetButton.setToolTipText("Reset zoom to 100%");
        try {
            java.awt.Image img = ImageIO.read(MarkdownReader.class.getResource("resources/oxygen-zoom-reset.png"));
            zoomResetButton.setIcon(new ImageIcon(img));
        } catch (Exception e) {
            e.printStackTrace();
        }
        zoomResetButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent arg0) {
                // Adjust zoomLevel and redraw Reload and transpile file, redraw FX.
                zoomLevel = 1.0;
                drawFXComponents();
            }
        });
        buttonPanel.add(zoomResetButton);

        JButton zoomOutButton = new JButton();
        Integer zoomOutButtonWidth = buttonHeight;
        zoomOutButton.setPreferredSize(new Dimension(zoomOutButtonWidth, buttonHeight));
        zoomOutButton.setToolTipText("Zoom in by 10%");
        try {
            java.awt.Image img = ImageIO.read(MarkdownReader.class.getResource("resources/oxygen-zoom-out.png"));
            zoomOutButton.setIcon(new ImageIcon(img));
        } catch (Exception e) {
            e.printStackTrace();
        }
        zoomOutButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent arg0) {
                // Adjust zoomLevel and redraw Reload and transpile file, redraw FX.
                zoomLevel = zoomLevel - 0.1;
                drawFXComponents();
            }
        });
        buttonPanel.add(zoomOutButton);

        frame.add(buttonPanel);

        // Add the JFXPanel to the Swing app, and initialize it in its own JFX thread.
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
        String cssPath = String.valueOf(MarkdownReader.class.getResource("resources/style.css"));
        webView.getEngine().setUserStyleSheetLocation(cssPath);

        // Load different things depending on whether an HTML file has been created.
        if (HTMLTranspiled == null){
            // First time this is run, there's no value to HTMLTranspiled.
            // So we display the simple instruction message MDText is initialized to.
            String outHTML = md_to_html(MDText);
            webView.getEngine().loadContent(outHTML);
        } else {
            // Load the value of HTMLTranpiled.
            webView.getEngine().loadContent(HTMLTranspiled);
            // System.out.println(HTMLTranspiled);
        }
        // Set the scene to be drawn as the WebView object.
        Scene scene = new Scene(webView);
        theJFXPanel.setScene(scene);

        // Zoom
        webView.setZoom(zoomLevel);
    };

    public static void main(String[] args) {
        
        // Turn on font anti aliasing - from https://batsov.com/articles/2010/02/26/enable-aa-in-swing/.
        System.setProperty("awt.useSystemAAFontSettings","on");
        System.setProperty("swing.aatext", "true");

        setLAF();
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                createAndShowGUI();
            }
        });

        // If the program was started with a file specified as an
        // argument on the command line, load it. At present, we
        // assume it's a full, absolute path to valid Markdown.
        if (args.length == 1){
            // String workingDir = System.getProperty("user.dir");
            mdPath = args[0];
            File givenFile = new File(mdPath);
            makeHTML(givenFile);
            drawFXComponents();
        
        }
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


class ImageVisitor extends AbstractVisitor {
    
    private static Path parentPath;

    public ImageVisitor(Path aPath){
        parentPath = aPath;
    }

    @Override
    public void visit(org.commonmark.node.Image img) {
        // Update the paths to images such that they are absolute by the adding parent directory path,
        // and make it a URI for webview.
        String destination = parentPath.resolve(img.getDestination()).toString();
        File destFile = new File(destination);
        img.setDestination(destFile.toURI().toString());
    }

}