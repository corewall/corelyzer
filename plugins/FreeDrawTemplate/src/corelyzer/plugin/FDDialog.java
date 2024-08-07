/**
 *      FreeDraw Notes

        // create a free draw area
        SceneGraph.lock();
        int fdid = SceneGraph.createFreeDrawRectangle(
            getPluginID(), 0.0f, 0.0f, 0.1f, 0.1f);
        SceneGraph.unlock();

        SceneGraph.markFreeDrawScaleIndependent( fdid, true );

        // destroy free draw area
        SceneGraph.lock();
        SceneGraph.destroyFreeDrawRectangle( cd.getFreeDrawID() );
        SceneGraph.unlock();

        // position the freedraw object
        SceneGraph.repositionFreeDrawRectangle( cd.getFreeDrawID(), x, y);
 *
 */

package corelyzer.plugin;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import corelyzer.helper.SceneGraph;
import corelyzer.ui.CorelyzerApp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;

public class FDDialog extends JFrame {
    private JPanel contentPane;
    private JButton buttonAdd;
    private JButton buttonRemove;
    private JTextField ulXCoordTextField;
    private JTextField widthTextField;
    private JTextField inputTextField;
    private JTextField ulYCoordTextField;
    private JTextField heightTextField;
    private JList freeDrawList;

    private int pluginID;
    private Vector<FreeDrawEntry> freeDrawVector;

    public FDDialog(int pID) {
        pluginID = pID;
        freeDrawVector = new Vector<FreeDrawEntry>();

        this.setTitle("FreeDraw Template");
        setContentPane(contentPane);
        // setModal(true);
        getRootPane().setDefaultButton(buttonAdd);

        buttonAdd.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onAdd();
            }
        });

        buttonRemove.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onRemove();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void onAdd() {
        String title = inputTextField.getText();
        float x = Float.parseFloat(ulXCoordTextField.getText());
        float y = Float.parseFloat(ulYCoordTextField.getText());
        float w = Float.parseFloat(widthTextField.getText());
        float h = Float.parseFloat(heightTextField.getText());

        System.out.println("---> Add a new FreeDraw: " + title + " at ("
                + x + ", " + y + ") with (" + w + ", " + h + ")");

        // TODO Create a free draw area
        SceneGraph.lock();
        // not attach to any track or section
        // int fdId = SceneGraph.createFreeDrawRectangle(pluginID, x, y, w, h);
        
        // attach to track. FIXME trackID 0
        //int fdId = SceneGraph.createFreeDrawRectangleForTrack(pluginID, 0, x, y, w, h);
        // int fdId = SceneGraph.createFreeDrawRectangleForTrack(pluginID, 0, x, y, w, h);
        // int fdId = SceneGraph.createFreeDrawRectangleForSection(pluginID, 0, 0, y, h);
        int fdId = SceneGraph.createFreeDrawRectangle(pluginID, x, y, w, h);
        SceneGraph.markFreeDrawScaleIndependent(fdId, true);
        SceneGraph.unlock();

        freeDrawVector.add(new FreeDrawEntry(fdId, title, x, y, w, h));
        freeDrawList.setListData(this.freeDrawVector);

        CorelyzerApp app = CorelyzerApp.getApp();
        app.updateGLWindows();
    }

    private void onRemove() {
        int selectedIndex = this.freeDrawList.getSelectedIndex();
        int fdId = this.freeDrawVector.elementAt(selectedIndex).freeDrawID;

        System.out.println("---> Remove current select FreeDraw: selectedIdx: "
                + selectedIndex + ", fdId: " + fdId);

        // TODO Destroy free draw area
        SceneGraph.lock();
        SceneGraph.destroyFreeDrawRectangle(fdId);
        SceneGraph.unlock();

        this.freeDrawVector.removeElementAt(selectedIndex);
        this.freeDrawList.setListData(this.freeDrawVector);

        CorelyzerApp app = CorelyzerApp.getApp();
        app.updateGLWindows();
    }

    private void onCancel() {
        dispose();
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(3, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1, true, false));
        panel1.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonAdd = new JButton();
        buttonAdd.setText("Add");
        panel2.add(buttonAdd, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonRemove = new JButton();
        buttonRemove.setText("Remove");
        panel2.add(buttonRemove, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("FreeDraw Template (Numbers in meters)");
        panel3.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(panel4, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Upper Left X(depth) and Y");
        panel4.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("width and height");
        panel4.add(label3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        ulXCoordTextField = new JTextField();
        ulXCoordTextField.setText("0.0");
        panel4.add(ulXCoordTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        widthTextField = new JTextField();
        widthTextField.setText("1.0");
        panel4.add(widthTextField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        ulYCoordTextField = new JTextField();
        ulYCoordTextField.setText("0.0");
        panel4.add(ulYCoordTextField, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        heightTextField = new JTextField();
        heightTextField.setText("1.0");
        panel4.add(heightTextField, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(panel5, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Input Text");
        panel5.add(label4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        inputTextField = new JTextField();
        inputTextField.setText("SomeTextHere");
        panel5.add(inputTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel6, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel6.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        freeDrawList = new JList();
        final DefaultListModel defaultListModel1 = new DefaultListModel();
        freeDrawList.setModel(defaultListModel1);
        scrollPane1.setViewportView(freeDrawList);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

    /*
    public static void main(String[] args) {
        FDDialog dialog = new FDDialog();
        dialog.pack();
        dialog.setVisible(true);
        // System.exit(0);
    }
    */

    // FreeDrawEntry to keep track of freedraw properties

    class FreeDrawEntry {
        int freeDrawID;
        String title;
        float x, y, w, h;

        public FreeDrawEntry() {
            super();
            freeDrawID = -1;
            title = "Default empty title";
            x = 0.0f;
            y = 0.0f;
            w = 1.0f;
            h = 1.0f;
        }

        public FreeDrawEntry(int fdId, String s,
                             float f1, float f2, float f3, float f4) {
            freeDrawID = fdId;
            title = s;
            x = f1;
            y = f2;
            w = f3;
            h = f4;
        }

        public String toString() {
            return "ID[" + freeDrawID + "]: \"" + title + "\"";
        }
    }
}
