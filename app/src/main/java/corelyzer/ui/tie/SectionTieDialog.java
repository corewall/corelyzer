package corelyzer.ui.tie;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.*;

import java.text.DecimalFormat;

import net.miginfocom.swing.MigLayout;
import corelyzer.data.CoreSectionTieType;
import corelyzer.graphics.SceneGraph;

public class SectionTieDialog extends JDialog {
    private JPanel contentPane;
    private JRadioButton visualType, dataType, spliceType;
    private JLabel aLabel, bLabel;
    private JButton buttonConfirm;
    private JButton buttonCancel;
    private JTextArea aDesc, bDesc;
    public boolean confirmed = false;

    public static void main(final String[] args) {
		SectionTieDialog dialog = new SectionTieDialog(null, -1, false);
		dialog.pack();
		dialog.setVisible(true);
        dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}

    // create: if true, dialog is for just-created tie with buttons "Delete"/"Create",
    // otherwise for editing tie with buttons "Close"/"Save".
    public SectionTieDialog(JFrame parent, int tieId, boolean create) {
        super(parent);
        setupUI(create);
        setupLabels(tieId);
        setTieType(CoreSectionTieType.fromInt(SceneGraph.getSectionTieType(tieId)));
    }
    
    private void setupLabels(int tieId) {
        if (tieId != -1) {
            String aSecId = SceneGraph.getSectionTieASectionName(tieId);
            String bSecId = SceneGraph.getSectionTieBSectionName(tieId);
            float[] aPos = SceneGraph.getSectionTieAPosition(tieId);
            float[] bPos = SceneGraph.getSectionTieBPosition(tieId);
            DecimalFormat df = new DecimalFormat("#.#");
            float ax = aPos[0] / SceneGraph.getCanvasDPIX(0) * 2.54f;
            float bx = bPos[0] / SceneGraph.getCanvasDPIX(0) * 2.54f;
            aLabel.setText("Z: " + aSecId + " " + df.format(ax) + "cm");
            aDesc.setText(SceneGraph.getSectionTieADescription(tieId));
            bLabel.setText("Z': " + bSecId + " " + df.format(bx) + "cm");
            bDesc.setText(SceneGraph.getSectionTieBDescription(tieId));
            pack();
        }
    }

    private void setTieType(CoreSectionTieType type) {
        if (type == CoreSectionTieType.VISUAL) {
            visualType.setSelected(true);
        } else if (type == CoreSectionTieType.DATA) {
            dataType.setSelected(true);
        } else if (type == CoreSectionTieType.SPLICE) {
            spliceType.setSelected(true);
        }
    }

    public CoreSectionTieType getTieType() {
        if (visualType.isSelected()) {
            return CoreSectionTieType.VISUAL;
        } else if (dataType.isSelected()) {
            return CoreSectionTieType.DATA;
        } else { // spliceType.isSelected()
            return CoreSectionTieType.SPLICE;
        }
    }
    public String getADesc() { return aDesc.getText(); }
    public String getBDesc() { return bDesc.getText(); }

    private void setupUI(boolean create) {
        final String titleStr = create ? "Create Section Tie" : "Edit Section Tie";
        setTitle(titleStr);
        contentPane = new JPanel();
        setContentPane(contentPane);

        contentPane.setLayout(new MigLayout("wrap, insets 10", "[grow]", "[]10[][grow][][grow][]"));

        JPanel tieTypePanel = new JPanel();
        tieTypePanel.setLayout(new MigLayout("insets 0", "[]10[][][]", ""));
        visualType = new JRadioButton("Visual");
        dataType = new JRadioButton("Data");
        spliceType = new JRadioButton("Splice");
        
        tieTypePanel.add(new JLabel("Tie Type: "));
        tieTypePanel.add(visualType);
        tieTypePanel.add(dataType);
        tieTypePanel.add(spliceType);

        ButtonGroup typeButtonGroup = new ButtonGroup();
        typeButtonGroup.add(visualType);
        typeButtonGroup.add(dataType);
        typeButtonGroup.add(spliceType);

        visualType.setSelected(true);
        contentPane.add(tieTypePanel);

        aLabel = new JLabel("[A core ID]");
        contentPane.add(aLabel);
        aDesc = new JTextArea();
        JScrollPane aScrollPane = new JScrollPane();
        aScrollPane.setViewportView(aDesc);
        contentPane.add(aScrollPane, "grow, hmin 80, wmin 300");

        bLabel = new JLabel("[B core ID]");
        contentPane.add(bLabel, "gapy 10");
        bDesc = new JTextArea();
        JScrollPane bScrollPane = new JScrollPane();
        bScrollPane.setViewportView(bDesc);
        contentPane.add(bScrollPane, "grow, hmin 80, wmin 300");

        final String confirmText = create ? "Create" : "Save";
        buttonConfirm = new JButton(confirmText);
        buttonConfirm.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });
        final String cancelText = create ? "Delete" : "Close";
        buttonCancel = new JButton(cancelText);
        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });
        contentPane.add(buttonCancel, "split 2, gapy 10, align right");
        contentPane.add(buttonConfirm);
        getRootPane().setDefaultButton(buttonConfirm);

        // close dialog on Escape key, thx StackOverflow
        // https://stackoverflow.com/questions/642925/swing-how-do-i-close-a-dialog-when-the-esc-key-is-pressed
        getRootPane().registerKeyboardAction(e -> { dispose(); }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        pack();
    }

    private void onOK() {
        confirmed = true;
        setVisible(false);
    }

    private void onCancel() {
		dispose();
	}
}
