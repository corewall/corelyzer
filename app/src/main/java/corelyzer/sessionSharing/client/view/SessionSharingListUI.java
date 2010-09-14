/******************************************************************************
 *
 * CoreWall / Corelyzer - An Initial Core Description Tool
 * Copyright (C) 2008 Julian Yu-Chung Chen
 * Electronic Visualization Laboratory, University of Illinois at Chicago
 *
 * This software is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either Version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License along
 * with this software; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * Questions or comments about CoreWall should be directed to
 * cavern@evl.uic.edu
 *
 *****************************************************************************/
package corelyzer.sessionSharing.client.view;

import java.awt.Component;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import corelyzer.data.CRPreferences;
import corelyzer.helper.URLRetrieval;
import corelyzer.io.StateLoader;
import corelyzer.sessionSharing.client.controller.SharingClient;
import corelyzer.sessionSharing.client.model.SessionSharingTableModel;
import corelyzer.sessionSharing.common.SharingServerResponse;
import corelyzer.ui.CorelyzerApp;
import corelyzer.util.StringUtility;
import corelyzer.util.TableSorter;

public class SessionSharingListUI extends JDialog {
	// For JTable JButtons
	class ButtonColumn extends AbstractCellEditor implements TableCellRenderer, TableCellEditor, ActionListener {
		/**
		 * 
		 */
		private static final long serialVersionUID = -8166621055439435456L;
		JTable table;
		JButton renderButton;
		JButton editButton;
		String text;

		public ButtonColumn(final JTable table, final int column) {
			super();
			this.table = table;
			renderButton = new JButton();

			editButton = new JButton();
			editButton.setFocusPainted(false);
			editButton.addActionListener(this);

			TableColumnModel columnModel = table.getColumnModel();
			columnModel.getColumn(column).setCellRenderer(this);
			columnModel.getColumn(column).setCellEditor(this);
		}

		public void actionPerformed(final ActionEvent e) {
			fireEditingStopped();

			// fixme
			System.out.println(e.getActionCommand() + " : " + table.getSelectedRow());
		}

		public Object getCellEditorValue() {
			return text;
		}

		public Component getTableCellEditorComponent(final JTable table, final Object value, final boolean isSelected, final int row, final int column) {
			text = value == null ? "" : value.toString();
			editButton.setText(text);
			return editButton;
		}

		public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row,
				final int column) {
			if (hasFocus) {
				renderButton.setForeground(table.getForeground());
				renderButton.setBackground(UIManager.getColor("Button.background"));
			} else {
				renderButton.setForeground(table.getForeground());
				renderButton.setBackground(UIManager.getColor("Button.background"));
			}

			/*
			 * else if (isSelected) {
			 * renderButton.setForeground(table.getSelectionForeground());
			 * renderButton.setBackground(table.getSelectionBackground()); }
			 */

			renderButton.setText(value == null ? "" : value.toString());
			return renderButton;
		}
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 5260843463948071252L;

	public static void main(final String[] args) {
		SessionSharingListUI dialog = new SessionSharingListUI();
		dialog.pack();
		dialog.setVisible(true);
		System.exit(0);
	}

	private JPanel contentPane;
	private JButton buttonOK;
	private JTable sessionListTable;
	private JScrollPane sessionScrollPane;

	private SessionSharingTableModel sessionListTableModel;
	private final SessionSharingListUI view;

	private String serverAddress;

	private int serverPort;

	public SessionSharingListUI() {
		$$$setupUI$$$();
		setContentPane(contentPane);
		setModal(true);
		getRootPane().setDefaultButton(buttonOK);

		buttonOK.addActionListener(new ActionListener() {

			public void actionPerformed(final ActionEvent e) {
				onOK();
			}
		});

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(final WindowEvent e) {
				onOK();
			}
		});

		contentPane.registerKeyboardAction(new ActionListener() {

			public void actionPerformed(final ActionEvent e) {
				onOK();
			}
		}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

		decorateTable();
		view = this;

		// Fetch setup preferences if available
		CorelyzerApp app = CorelyzerApp.getApp();

		if (app == null) {
			setServerAddress("127.0.0.1");
			setServerPort(16688);
		} else {
			CRPreferences prefs = app.preferences();

			String srvAddr;
			int srvPort;

			if ((prefs.getProperty("sessionSharing.serverAddress") == null) || prefs.getProperty("sessionSharing.serverAddress").equals("")) {
				srvAddr = "127.0.0.1";
			} else {
				srvAddr = prefs.getProperty("sessionSharing.serverAddress");
			}

			if ((prefs.getProperty("sessionSharing.serverPort") == null) || prefs.getProperty("sessionSharing.serverPort").equals("")) {
				srvPort = 16688;
			} else {
				srvPort = Integer.parseInt(prefs.getProperty("sessionSharing.serverPort"));
			}

			setServerAddress(srvAddr);
			setServerPort(srvPort);
		}
	}

	/**
	 * @noinspection ALL
	 */
	public JComponent $$$getRootComponent$$$() {
		return contentPane;
	}

	/**
	 * Method generated by IntelliJ IDEA GUI Designer >>> IMPORTANT!! <<< DO NOT
	 * edit this method OR call it in your code!
	 * 
	 * @noinspection ALL
	 */
	private void $$$setupUI$$$() {
		createUIComponents();
		contentPane = new JPanel();
		contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
		final JPanel panel1 = new JPanel();
		panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
		contentPane.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
		final Spacer spacer1 = new Spacer();
		panel1.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
		final JPanel panel2 = new JPanel();
		panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
		panel1.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		buttonOK = new JButton();
		buttonOK.setText("Close");
		panel2.add(buttonOK, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JPanel panel3 = new JPanel();
		panel3.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
		contentPane
				.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
						| GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
						0, false));
		sessionScrollPane = new JScrollPane();
		panel3.add(sessionScrollPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK
						| GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
		sessionScrollPane.setBorder(BorderFactory.createTitledBorder("Sessions"));
		sessionScrollPane.setViewportView(sessionListTable);
	}

	private void createUIComponents() {
		sessionListTable = new JTable() {
			private static final long serialVersionUID = 1L;

			@Override
			public Class getColumnClass(final int column) {
				return getValueAt(0, column).getClass();
			}
		};
	}

	private void decorateTable() {
		sessionListTable.setShowVerticalLines(true);
		sessionListTable.getTableHeader().setReorderingAllowed(true);
		sessionListTable.addMouseListener(new MouseListener() {

			public void mouseClicked(final MouseEvent event) {
				Point p = event.getPoint();

				TableSorter sorter = (TableSorter) sessionListTable.getModel();
				int row = sorter.modelIndex(sessionListTable.rowAtPoint(p));
				int column = sessionListTable.columnAtPoint(p);

				if (event.getClickCount() == 2) {
					String description = (String) sessionListTableModel.getValueAt(row, 2);

					ProjectDescriptionDialog dialog = new ProjectDescriptionDialog(view);
					dialog.setDescription(description);
					dialog.pack();
					dialog.setSize(800, 600);
					dialog.setLocationRelativeTo(view);
					dialog.setVisible(true);
				} else if (event.getClickCount() == 1) {
					String projectName = (String) sessionListTableModel.getValueAt(row, 0);
					String authorName = (String) sessionListTableModel.getValueAt(row, 1);

					if (column == 4) { // Subscribe
						doSubscribe(authorName, projectName);
					} else if (column == 5) { // Download
						doDownload(authorName, projectName);
					}
				}
			}

			public void mouseEntered(final MouseEvent event) {
			}

			public void mouseExited(final MouseEvent event) {
			}

			public void mousePressed(final MouseEvent event) {
			}

			public void mouseReleased(final MouseEvent event) {
			}
		});

		sessionListTableModel = new SessionSharingTableModel();
		TableSorter sorter = new TableSorter();
		sorter.setTableModel(sessionListTableModel);
		sorter.setTableHeader(sessionListTable.getTableHeader());
		sessionListTable.setModel(sorter);

		// pre-process header labels
		for (int i = 0; i < sessionListTableModel.getIndexKeyMapping().length; i++) {
			String header = sessionListTableModel.getIndexKeyMapping()[i];
			header = StringUtility.capitalizeHeadingCharacter(header);

			sessionListTable.getColumnModel().getColumn(i).setHeaderValue(header);
		}

		// Button cell render
		new ButtonColumn(sessionListTable, 4);
		new ButtonColumn(sessionListTable, 5);
	}

	private void doDownload(final String authorName, final String projectName) {
		String mesg = "Download '" + projectName + "' by '" + authorName + "'?";
		int res = JOptionPane.showConfirmDialog(view, mesg);

		if (res == JOptionPane.YES_OPTION) {
			SharingClient client = new SharingClient(this);
			client.setConnectOperation(getServerAddress(), getServerPort());
			byte srvRes = client.execute();
			if (srvRes != SharingServerResponse.CONNECTED) {
				mesg = "Cannot connect to server \n'" + getServerAddress() + "'";
				JOptionPane.showMessageDialog(this, mesg);
				return;
			}

			client.setDownloadOperation(authorName, projectName);
			srvRes = client.execute();

			if (srvRes != SharingServerResponse.DOWNLOAD_SUCCESS) {
				mesg = "Download failed";
				JOptionPane.showMessageDialog(this, mesg);
				return;
			}

			// Get cmlURL, download cml and do something with the file.
			String cmlUrl = client.getCmlURL();
			if ((cmlUrl == null) || cmlUrl.equals("")) {
				JOptionPane.showMessageDialog(view, "Invalid CML URL!");
				return;
			}
			cmlUrl = cmlUrl.replaceAll(" ", "%20");

			CorelyzerApp app = CorelyzerApp.getApp();
			if (app != null) {
				CRPreferences prefs = app.preferences();
				final String sp = System.getProperty("file.separator");

				// cml download dir
				File cmlDir = new File(prefs.download_Directory + sp + "CML");
				if (!cmlDir.exists() || !cmlDir.isDirectory()) {
					if (!cmlDir.mkdir()) {
						JOptionPane.showMessageDialog(view, "Cannot create local cml directory! " + "Download Failed");
						return;
					}
				}

				final String localPath = cmlDir.getAbsolutePath() + sp + projectName + ".cml";

				try {
					boolean isDownloaded = URLRetrieval.retrieveLocalCopy(cmlUrl, localPath);

					if (isDownloaded) {
						final StateLoader loader = new StateLoader();

						Runnable loading = new Runnable() {

							public void run() {
								boolean isLoaded = loader.loadState(localPath);

								if (!isLoaded) {
									JOptionPane.showMessageDialog(view, "Loading session error! '" + localPath + "'");
								}
							}
						};
						new Thread(loading).start();

						Vector<String> hst = prefs.getSessionHistory();
						if (!hst.contains(localPath)) {
							prefs.getSessionHistory().add(localPath);
						}
						app.getController().refreshSessionHistoryMenu();

						view.dispose();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				JOptionPane.showMessageDialog(view, "CML URL: '" + cmlUrl + "'");
			}
		}
	}

	private void doSubscribe(final String authorName, final String projectName) {
		String mesg = "Subscribe '" + projectName + "' by '" + authorName + "'?";
		int res = JOptionPane.showConfirmDialog(view, mesg);

		if (res == JOptionPane.YES_OPTION) {
			SharingClient client = new SharingClient(this);
			client.setConnectOperation(getServerAddress(), getServerPort());
			byte srvRes = client.execute();
			if (srvRes != SharingServerResponse.CONNECTED) {
				mesg = "Cannot connect to server \n'" + getServerAddress() + "'";
				JOptionPane.showMessageDialog(this, mesg);
				return;
			}

			client.setSubscribeOperation(authorName, projectName);
			srvRes = client.execute();

			if (srvRes != SharingServerResponse.SUBSCRIBE_SUCCESS) {
				mesg = "Subscribe failed";
				JOptionPane.showMessageDialog(this, mesg);
				return;
			}

			// get feedUrl and do something with the feedUrl
			String feedUrl = client.getFeedURL();
			if ((feedUrl == null) || feedUrl.equals("")) {
				JOptionPane.showMessageDialog(view, "Invalid CML URL!");
				return;
			}
			feedUrl = feedUrl.replaceAll(" ", "%20");

			CorelyzerApp app = CorelyzerApp.getApp();
			if (app != null) {
				setVisible(false);

				StringUtility.setClipboard(feedUrl);
				app.getController().onSubscribe();
			} else {
				JOptionPane.showMessageDialog(view, "Feed URL: '" + feedUrl + "'");
			}

			dispose();
		}
	}

	public String getServerAddress() {
		return serverAddress;
	}

	public int getServerPort() {
		return serverPort;
	}

	private void onOK() {
		dispose();
	}

	public void setServerAddress(final String serverAddress) {
		this.serverAddress = serverAddress;
	}

	public void setServerPort(final int serverPort) {
		this.serverPort = serverPort;
	}

	public void setSessions(final Vector<Hashtable<String, String>> sessions) {
		sessionListTableModel.setSessions(sessions);
		sessionListTable.updateUI();
	}
}
