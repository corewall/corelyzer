package corelyzer.plugin.andrill;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DiscussionThreadWindow extends JFrame implements ActionListener {

    JEditorPane   discussion;
    JEditorPane   preview;
    HTMLEditorKit htmlKit;
    String        htmlfilename;
    JScrollPane   discussionPane;

    public DiscussionThreadWindow(String htmlpage) {
        super();
        htmlfilename = htmlpage;
        JPanel htmlpanel = new JPanel();
        htmlpanel.setLayout( new BorderLayout() );
        htmlKit = new HTMLEditorKit();
        htmlKit.getStyleSheet().addRule(
            "body { font-family: sans-serif; margin-right: 20%;" +
            "margin-left: 20%; }" );

        String text = new String("");
        try {
            File f = new File(htmlpage);
            if( f.exists() ) {
                BufferedReader br = new BufferedReader( new FileReader(f));
                String htmlcode = new String("");
                String line;
                while( (line = br.readLine()) != null) {
                    htmlcode = htmlcode + line;
                }
                br.close();
                text = htmlcode;
            }
        } catch( Exception e) {
            // do nothing
        }

        discussion = new JEditorPane();
        discussion.setPreferredSize( new Dimension(585,390));
        discussion.setEditorKit(htmlKit);
        discussion.setEditable(false);
        discussion.setBorder(BorderFactory.createTitledBorder("Discussion"));
        discussion.setText(text);
        JScrollPane sp_p = new JScrollPane(discussion);
        sp_p.setVerticalScrollBarPolicy( 
            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
        htmlpanel.add( sp_p, BorderLayout.CENTER );
        discussionPane = sp_p;

        preview = new JEditorPane();
        preview.setPreferredSize( new Dimension(585,100));
        preview.setEditorKit(htmlKit);
        preview.setEditable(true);
        preview.setBorder(
            BorderFactory.createTitledBorder("Create new entry here:"));
        sp_p = new JScrollPane(preview);
        htmlpanel.add( sp_p, BorderLayout.SOUTH);
        
        add( htmlpanel, BorderLayout.CENTER );
        
        JButton btn = new JButton("SUBMIT");
        btn.addActionListener(this);
        add( btn, BorderLayout.SOUTH );

        setSize(600,550);
        setVisible(true);

        updateInputMap();
    }

    public static void main(String[] args) {
        DiscussionThreadWindow dtw = new DiscussionThreadWindow(args[0]);
        dtw.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    private void updateInputMap() {
        InputMap map = getEditor().getInputMap();
        int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

        KeyStroke bold   = KeyStroke.getKeyStroke(KeyEvent.VK_B, mask, false);
        KeyStroke italic = KeyStroke.getKeyStroke(KeyEvent.VK_I, mask, false);
        KeyStroke under  = KeyStroke.getKeyStroke(KeyEvent.VK_U, mask, false);
        KeyStroke br     = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false);

        map.put(bold,   "font-bold");
        map.put(italic, "font-italic");
        map.put(under,  "font-underline");

//        map.put(br,     "InsertBR");
    }

    public JEditorPane getEditor() {
        return this.preview; 
    }

    public void actionPerformed(ActionEvent e) {
        String text = discussion.getText();
        int bodyend = text.lastIndexOf("</body>");
        String before, end;
        before = text.substring(0,bodyend);
        end = text.substring(bodyend, text.length() );

        String colors[] = { "#ccccff" , "#ffffff" };
        int lastbgcolor = text.lastIndexOf("bgcolor=");
        int color = 1;
        if( lastbgcolor != -1) {
            if( text.charAt( lastbgcolor + 10 ) == 'f')
                color = 0;
        }

        Date now = new Date(System.currentTimeMillis());
        SimpleDateFormat format = new SimpleDateFormat(
            "MM/dd/yyy 'at' hh:mm:ss z");
        String time = format.format(now);

        String header = new String( "<b>" + "On " + time + " " +
                                    System.getProperty("user.name") +
                                    " wrote: </b><br>");
                                     
                                    
        String newtext = preview.getText();
        newtext = newtext.replace("<html>\n  <head>\n\n  </head>\n  <body>\n",
                                  "<table bgcolor=\"" + colors[color] +
                                  "\" width=\"100%\" border=\"0\">\n" +
                                  "<tr><td>" + header);
        newtext = newtext.replace("  </body>\n</html>", 
                                  "</td></tr></table><hr>");
        //System.out.println(newtext + "\n=======================");

        text = before + newtext + end;
        text = text.replaceAll("<p style=\"margin-top: 0\">",
                               "<p style=\"margin-left: 5%; " +
                               "margin-right: 10%; " +
                               "font-family: sans-serif;\">");


        discussion.setText(text);

        //System.out.println(text + "\n==========================");

        preview.setText("");

        try {
            File f = new File(htmlfilename);
            FileWriter fw = new FileWriter(f);
            fw.write( discussion.getText(), 0, discussion.getText().length() );
            fw.close();
        } catch (Exception ee) {
            System.out.println("Failed to write out html to disk:");
            ee.printStackTrace();
        }
        
        JScrollBar js = discussionPane.getVerticalScrollBar();
        js.setValue( js.getMaximum() );
        
        preview.requestFocus();
        repaint();
    }

    //--------------------------------------------------------------------------

    public class BRAction extends StyledEditorKit.StyledTextAction {

        public BRAction () {
            super("InsertBR");
        }

        public void actionPerformed(ActionEvent e) {
            insertBreak(e);
        }

        private void insertBreak(ActionEvent e) {
            JEditorPane   editor = this.getEditor(e);

            if(editor == null) return;

            HTMLEditorKit kit    = (HTMLEditorKit) editor.getEditorKit();
            HTMLDocument  doc    = (HTMLDocument)  editor.getDocument();
            int caretPos = editor.getCaretPosition();

            try {
                kit.insertHTML(doc, caretPos, "<BR>", 0, 0, HTML.Tag.BR);
            } catch (IOException ioe) {
                System.err.println("IOException in inserting <BR>");
            } catch (BadLocationException ble) {
                System.err.println("BadLocationException in inserting <BR>");
            }

            editor.setCaretPosition(caretPos+1);
        }
    }

}
