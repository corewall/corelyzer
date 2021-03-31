package corelyzer.ui;

import java.awt.EventQueue;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import java.io.File;

public class SwingSafeDirectoryChooser {
    private static int retval;
    public static File selectedDir;
    public static int chooseFile(File dir, JFrame parent, String title) {
        try {
            EventQueue.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    JFileChooser chooser = new JFileChooser(dir);
                    chooser.setDialogTitle(title);
                    chooser.resetChoosableFileFilters();
                    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    chooser.setMultiSelectionEnabled(false);
                    retval = chooser.showOpenDialog(parent);
                    if (retval == JFileChooser.APPROVE_OPTION) {
                        selectedDir = chooser.getSelectedFile();
                    }
                }
            });
        } catch (Exception e) {
            System.err.println(e.getLocalizedMessage());
        }
        return retval;
    }
}

