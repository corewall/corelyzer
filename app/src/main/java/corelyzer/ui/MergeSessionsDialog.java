package corelyzer.ui;

import java.awt.Frame;
import javax.swing.*;
import java.awt.event.*;
import java.util.Vector;

import net.miginfocom.swing.MigLayout;

import corelyzer.data.Session;

public class MergeSessionsDialog extends JDialog implements ItemListener {
	private JPanel contentPane;
	private JButton buttonOK, buttonCancel;
	private JScrollPane sessionListScrollPane;
	private CheckBoxList sessionList;
    private JComboBox<String> destSession;
    private Vector<Session> sessions;
    public boolean confirmed = false;

    public MergeSessionsDialog(final Frame parent, Vector<Session> sessions) {
        super(parent, "Merge Sessions");
        this.sessions = sessions;
        setupUI();
        setupListeners();
    }

    private void setupUI() {
        contentPane = new JPanel();
        setContentPane(contentPane);
		setModal(true);
		getRootPane().setDefaultButton(buttonOK);
        
        contentPane.setLayout(new MigLayout("insets 10, wrap", "[grow]", "[][grow][]15[center]"));
		contentPane.add(new JLabel("Select sessions to merge."));
        
        sessionList = new CheckBoxList();
        sessionListScrollPane = new JScrollPane();
        sessionListScrollPane.setViewportView(sessionList);
        contentPane.add(sessionListScrollPane, "grow");
        populateSessionList();

        destSession = new JComboBox<String>();
        DefaultComboBoxModel<String> cbModel = new DefaultComboBoxModel<String>();
        cbModel.addElement("Select 2+ sessions");
        destSession.setModel(cbModel);
        contentPane.add(new JLabel("Merge into: "), "aligny center, split 2");
        contentPane.add(destSession, "gapleft 0, grow, aligny bot");
        
        buttonOK = new JButton("Merge Sessions");
        buttonOK.setEnabled(false);
        buttonCancel = new JButton("Cancel");
        contentPane.add(buttonCancel, "split 2, align center");
        contentPane.add(buttonOK);

        this.pack();
    }

    private void setupListeners() {
		buttonOK.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
                confirmed = true;
                dispose();
            }
		});

		buttonCancel.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) { onCancel(); }
		});

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(final WindowEvent e) { onCancel(); }
		});

		contentPane.registerKeyboardAction(new ActionListener() {
			public void actionPerformed(final ActionEvent e) { onCancel(); }
		}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void populateSessionList() {
        DefaultListModel<JCheckBox> model = new DefaultListModel<JCheckBox>();
        for (Session s : this.sessions) {
            JCheckBox cb = new JCheckBox(s.getName());
            cb.addItemListener(this);
            model.addElement(cb);
        }
        sessionList.setModel(model);
    }

    // When session is checked, update destSession combo box.
    // Must use ItemListener (vs ActionListener) to detect changes in CheckBoxList
    // because its checkboxes' states are set with setSelected().
    public void itemStateChanged(ItemEvent e) {
        Vector<String> selectedSessions = new Vector<String>();
        for (int idx : sessionList.getCheckedIndices()) {
            final String sessionName = sessionList.getModel().getElementAt(idx).getText();
            selectedSessions.add(sessionName);
        }
        
        boolean enableOK = true;
        DefaultComboBoxModel<String> cbModel = new DefaultComboBoxModel<String>();
        if (selectedSessions.size() < 2) {
            cbModel.addElement("Select 2+ sessions");
            enableOK = false;
        } else {
            for (String sessionName : selectedSessions) {
                cbModel.addElement(sessionName);
            }
        }
        destSession.setModel(cbModel);
        buttonOK.setEnabled(enableOK);
    }

    public Vector<Session> getSessionsToMerge() {
        Vector<Session> sessionsToMerge = new Vector<Session>();
        final int destSessionIdx = destSession.getSelectedIndex();
        for (int idx = 0; idx < sessions.size(); idx++) {
            if (idx == destSessionIdx) { continue; }
            sessionsToMerge.add(sessions.get(idx));
        }
        return sessionsToMerge;
    }

    public Session getDestinationSession() {
        return sessions.get(destSession.getSelectedIndex());
    }

    private void onCancel() { dispose(); }
}
