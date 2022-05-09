package corelyzer.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;


import net.miginfocom.swing.MigLayout;

public class SectionTieDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField fromDesc, toDesc;
    public boolean confirmed = false;

    public static void main(final String[] args) {
		SectionTieDialog dialog = new SectionTieDialog(null);
		dialog.pack();
		dialog.setVisible(true);
        dialog.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

    public SectionTieDialog(JFrame parent) {
        super(parent);
        setupUI();
    }

    public void setTieData(String fromDesc, String toDesc) {
        this.fromDesc.setText(fromDesc);
        this.toDesc.setText(toDesc);
    }

    public String getFromDesc() { return fromDesc.getText(); }
    public String getToDesc() { return toDesc.getText(); }

    private void setupUI() {
        setTitle("Edit Section Tie");
        contentPane = new JPanel();
        setContentPane(contentPane);

        contentPane.setLayout(new MigLayout("wrap 2", "[][grow]", ""));

        contentPane.add(new JLabel("From description"));
        fromDesc = new JTextField();
        contentPane.add(fromDesc, "grow");

        contentPane.add(new JLabel("To description"));
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
        contentPane.add(buttonCancel);
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
