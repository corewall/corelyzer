package corelyzer.ui;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListModel;
import javax.swing.event.ListDataListener;

import net.miginfocom.swing.MigLayout;

import corelyzer.data.lists.CRDefaultListModel;

class CheckBoxListModel implements ListModel<JCheckBox> {
	Vector<JCheckBox> sessions;

	public CheckBoxListModel(final CRDefaultListModel m) {
		if (m == null) {
			return;
		}

		sessions = new Vector<JCheckBox>();

		for (int i = 0; i < m.getSize(); i++) {
			String sessionName = m.getElementAt(i).toString();
			sessions.add(new JCheckBox(sessionName, true));
		}
	}

	public void addListDataListener(final ListDataListener listDataListener) {
		// do nothing
	}

	public JCheckBox getElementAt(final int i) {
		if (sessions == null) {
			return null;
		} else {
			return sessions.elementAt(i);
		}
	}

	public int getSize() {
		if (sessions == null) {
			return 0;
		} else {
			return sessions.size();
		}
	}

	public void removeListDataListener(final ListDataListener listDataListener) {
		// do nothing
	}
}

public class SessionsSelectDialog extends JDialog {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5845300587940135499L;

	public static void main(final String[] args) {
		SessionsSelectDialog dialog = new SessionsSelectDialog(null);
		dialog.pack();
		dialog.setVisible(true);
		System.exit(0);
	}

	private JPanel contentPane;
	private JButton buttonOK;
	private JButton buttonCancel;
	private JScrollPane checkListScrollPane;
	private CheckBoxList sessList;

	private boolean isCancelled = false;

	public SessionsSelectDialog(final Frame parent) {
		super(parent, "Save Sessions");

		// $$$setupUI$$$();
		setupUI();
		setContentPane(contentPane);
		setModal(true);
		getRootPane().setDefaultButton(buttonOK);

		buttonOK.addActionListener(new ActionListener() {

			public void actionPerformed(final ActionEvent e) {
				onOK();
			}
		});

		buttonCancel.addActionListener(new ActionListener() {

			public void actionPerformed(final ActionEvent e) {
				onCancel();
			}
		});

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(final WindowEvent e) {
				onCancel();
			}
		});

		contentPane.registerKeyboardAction(new ActionListener() {

			public void actionPerformed(final ActionEvent e) {
				onCancel();
			}
		}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
	}

	private void setupUI() {
		createUIComponents();
		contentPane = new JPanel(); 
		contentPane.setLayout(new MigLayout("insets 10, wrap", "[grow]", "[][grow][center]"));
		contentPane.add(new JLabel("Select sessions to save."));
		checkListScrollPane = new JScrollPane();
		contentPane.add(checkListScrollPane, "grow");
		checkListScrollPane.setViewportView(sessList);
		buttonOK = new JButton("OK");
		buttonCancel = new JButton("Cancel");
		contentPane.add(buttonOK, "split 2, align center");
		contentPane.add(buttonCancel);
	}

	private void createUIComponents() {
		CorelyzerApp app = CorelyzerApp.getApp();

		if (app == null) {
			return;
		}

		sessList = new CheckBoxList();
		sessList.setModel(new CheckBoxListModel(app.getSessionListModel()));
	}

	public boolean[] getSelectedIndices() {
		if (this.isCancelled) {
			return null;
		}

		boolean hasSelection = false;
		if (sessList != null) {
			CheckBoxList l = (CheckBoxList) sessList;
			boolean[] ret = new boolean[l.getModel().getSize()];

			for (int i = 0; i < ret.length; i++) {
				JCheckBox cb = (JCheckBox) l.getModel().getElementAt(i);
				ret[i] = cb.isSelected();
				if (ret[i])
					hasSelection = true;
			}

			if (hasSelection)
				return ret;
		}
		return null;
	}
	
	// returns text of first selected checkbox
	public String getSelectedIndexName() {
		String result = null;
		if ( !this.isCancelled && sessList != null )
		{
			CheckBoxList l = (CheckBoxList) sessList;

			for (int i = 0; i < l.getModel().getSize(); i++) {
				JCheckBox cb = (JCheckBox) l.getModel().getElementAt(i);
				if ( cb.isSelected() )
				{
					result = cb.getText();
					break;
				}
			}
		}	

		return result;
	}

	private void onCancel() {
		this.isCancelled = true;
		dispose();
	}

	private void onOK() {
		this.isCancelled = false;
		dispose();
	}
}