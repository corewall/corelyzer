package corelyzer.ui.tie;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import net.miginfocom.swing.MigLayout;

import corelyzer.graphics.SceneGraph;

public class SectionTieDialog extends JDialog {
    private JPanel contentPane;
    private JLabel srcLabel, destLabel;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField fromDesc, toDesc;
    public boolean confirmed = false;

    public static void main(final String[] args) {
		SectionTieDialog dialog = new SectionTieDialog(null, -1);
		dialog.pack();
		dialog.setVisible(true);
        dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}

    public SectionTieDialog(JFrame parent, int tieId) {
        super(parent);
        setupUI();
        setupLabels(tieId);
    }
    
    private void setupLabels(int tieId) {
        if (tieId != -1) {
            String srcSecId = SceneGraph.getSectionTieSourceSectionName(tieId);
            String destSecId = SceneGraph.getSectionTieDestinationSectionName(tieId);
            float[] srcPos = SceneGraph.getSectionTieSourcePosition(tieId);
            float[] destPos = SceneGraph.getSectionTieDestinationPosition(tieId);
            int sx = Math.round(srcPos[0] / SceneGraph.getCanvasDPIX(0) * 2.54f);
            int dx = Math.round(destPos[0] / SceneGraph.getCanvasDPIX(0) * 2.54f);
            srcLabel.setText(srcSecId + " " + sx + "cm");
            destLabel.setText(destSecId + " " + dx + "cm");
            pack();
        }
    }

    public void setDescs(String fromDesc, String toDesc) {
        this.fromDesc.setText(fromDesc);
        this.toDesc.setText(toDesc);
        pack();
    }

    public String getFromDesc() { return fromDesc.getText(); }
    public String getToDesc() { return toDesc.getText(); }

    private void setupUI() {
        setTitle("Edit Section Tie");
        contentPane = new JPanel();
        setContentPane(contentPane);

        contentPane.setLayout(new MigLayout("wrap", "[grow]", "[grow]"));

        srcLabel = new JLabel("[source core ID]");
        contentPane.add(srcLabel);
        fromDesc = new JTextField();
        contentPane.add(fromDesc, "grow");

        destLabel = new JLabel("[dest core ID]");
        contentPane.add(destLabel);
        toDesc = new JTextField();
        contentPane.add(toDesc, "grow");

        buttonOK = new JButton("OK");
        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("OK");
                onOK();
            }
        });
        buttonCancel = new JButton("Cancel");
        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // System.out.println("Cancel");
                onCancel();
            }
        });
        contentPane.add(buttonCancel, "split 2");
        contentPane.add(buttonOK);
        getRootPane().setDefaultButton(buttonOK);

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
