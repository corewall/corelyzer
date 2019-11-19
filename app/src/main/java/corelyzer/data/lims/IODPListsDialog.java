package corelyzer.data.lims;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import corelyzer.data.Session;
import corelyzer.data.TrackSceneNode;
import corelyzer.data.coregraph.CoreGraph;
import corelyzer.graphics.SceneGraph;
import corelyzer.helper.URLRetrieval;
import corelyzer.io.CRDepthValueDataLoader;
import corelyzer.remoteControl.server.controller.ControlServerApplication;
import corelyzer.ui.CorelyzerApp;
import corelyzer.util.FileUtility;

public class IODPListsDialog extends JDialog {
	/**
	 * 
	 */
	private static final long serialVersionUID = 9027302391810333011L;

	public static void main(final String[] args) {
		IODPListsDialog dialog = new IODPListsDialog(null);
		dialog.pack();

		dialog.setLocationRelativeTo(null);
		dialog.setVisibleTab(0);

		// for quick tests
		/*
		 * String[] sectionLists =
		 * {"/Users/julian/Documents/Corelyzer/CRTest/199_1217/chronos-199_1217.dat"
		 * }; dialog.loadLIMSTables(sectionLists); dialog.loadAAffineTable(
		 * "/Users/julian/Documents/Corelyzer/CRTest/199_1217/199-1217.1.affine.table"
		 * );
		 */
		// dialog.loadASpliceTable("/Users/julian/Documents/Corelyzer/CRTest/199_1217/199-1217.1.splice.table",
		// "199");

		// tests
		// LIMSImageryDirectory dir = LIMSImageryDirectory.getDirectory();
		/*
		 * String[] cores = dir.getCoresInAHoleBelowDepth("199", "1217", "A",
		 * 40.0f); for (String core : cores) { System.out.println("Below " +
		 * 40.0f + " core: " + core); }
		 */

		// String coreType = dir.getCoreType("199", "1217", "A", "17");
		// System.out.println("[TEST] Core type is: " + coreType);

		dialog.setVisible(true);
	}

	private JPanel contentPane;
	private JButton buttonOK;
	private JTabbedPane listTabPane;
	private JTable sectionListTable;
	private JTable affineTable;
	private JTable spliceTable;
	private JButton resetButton;
	private JButton openButton;
	private JButton applyButton;
	private JComboBox<String> srvPrefixComboBox;
	// private JComboBox toolComboBox;
	private JComboBox<String> siteComboBox;
	private JTable dataURLTable;
	private JPanel loggingDBPane;
	private JLabel dataFilesLabel;
	// private JLabel toolLabel;
	// private JLabel holeLabel;
	private JLabel serviceLabel;
	private JComboBox<String> legComboBox;
	// private JComboBox holeComboBox;
	private JLabel siteLabel;
	private JLabel legLabel;
	private JComboBox<String> chronosServiceComboBox;
	private JComboBox<String> siteField;
	private JTextField filterTextField;
	private JTable resultTable;
	private JLabel searchLabel;

	private JLabel imageSiteLabel;

	public IODPListsDialog(final JFrame f) {
		super(f);

		$$$setupUI$$$();
		setTitle("IODP tables");
		initTables();

		setContentPane(contentPane);
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
				onCancel();
			}
		});

		contentPane.registerKeyboardAction(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				onCancel();
			}
		}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

		resetButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent actionEvent) {
				onReset();
			}
		});

		openButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent actionEvent) {
				onOpen();
			}
		});

		applyButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent actionEvent) {
				onApply();
			}
		});

		listTabPane.addChangeListener(new ChangeListener() {
			public void stateChanged(final ChangeEvent changeEvent) {
				onTabChange();
			}
		});

		srvPrefixComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent actionEvent) {
				resetButton.setEnabled(false);
				srvPrefixComboBox.setEnabled(false);

				legComboBox.setEnabled(false);
				siteComboBox.setEnabled(false);
				// holeComboBox.setEnabled(false);
				// toolComboBox.setEnabled(false);

				serviceLabel.setEnabled(false);
				serviceLabel.setText("Refreshing...");
				legLabel.setEnabled(false);
				legLabel.setText("Refreshing...");
				siteLabel.setEnabled(false);
				siteLabel.setText("Refreshing...");
				// holeLabel.setEnabled(false);
				// holeLabel.setText("Refreshing...");
				// toolLabel.setEnabled(false);
				// toolLabel.setText("Refreshing...");

				Thread refreshComboBox = new Thread() {
					@Override
					public void run() {
						onRefresh();
					}
				};

				refreshComboBox.start();
			}
		});

		/*
		 * siteComboBox.addActionListener(new ActionListener() { public void
		 * actionPerformed(ActionEvent actionEvent) { doQuery(); } });
		 * toolComboBox.addActionListener(new ActionListener() { public void
		 * actionPerformed(ActionEvent actionEvent) { doQuery(); } });
		 */

		legComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent actionEvent) {
				if ((legComboBox.getSelectedItem() == null) || legComboBox.getSelectedItem().toString().equals("")) {
					return;
				}

				siteComboBox.setEnabled(false);
				siteLabel.setEnabled(false);
				siteLabel.setText("Refreshing...");

				Thread refreshSiteComboBox = new Thread() {
					@Override
					public void run() {
						onRefreshSiteComboBox();
					}
				};

				refreshSiteComboBox.start();
			}
		});

		resultTable.addMouseListener(new MouseAdapter() {
			//
		});

		chronosServiceComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent action) {
				siteField.setEnabled(false);
				imageSiteLabel.setEnabled(false);
				imageSiteLabel.setText("Refreshing...");

				Thread refreshSiteComboBox = new Thread() {
					@Override
					public void run() {
						onRefreshSiteField();
					}
				};

				refreshSiteComboBox.start();
			}
		});
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
		panel1.setLayout(new GridLayoutManager(1, 5, new Insets(0, 0, 0, 0), -1, -1));
		contentPane.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
		final Spacer spacer1 = new Spacer();
		panel1.add(spacer1, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
		final JPanel panel2 = new JPanel();
		panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
		panel1.add(panel2, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		buttonOK = new JButton();
		buttonOK.setText("Close");
		panel2.add(buttonOK, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		resetButton = new JButton();
		resetButton.setText("Reset...");
		panel1.add(resetButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		openButton = new JButton();
		openButton.setText("Open...");
		panel1.add(openButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		applyButton = new JButton();
		applyButton.setEnabled(false);
		applyButton.setText("Load selected sections");
		panel1.add(applyButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JPanel panel3 = new JPanel();
		panel3.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
		contentPane
				.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
						| GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
						0, false));
		listTabPane = new JTabbedPane();
		panel3.add(listTabPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200,
				200), null, 0, false));
		final JPanel panel4 = new JPanel();
		panel4.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
		listTabPane.addTab("Image list from file", panel4);
		final JScrollPane scrollPane1 = new JScrollPane();
		panel4.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0,
				false));
		scrollPane1.setViewportView(sectionListTable);
		final JPanel panel5 = new JPanel();
		panel5.setLayout(new GridLayoutManager(4, 2, new Insets(0, 0, 0, 0), -1, -1));
		listTabPane.addTab("Image list from service", panel5);
		final JLabel label1 = new JLabel();
		label1.setText("Service provider: ");
		panel5.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		chronosServiceComboBox = new JComboBox<String>();
		final DefaultComboBoxModel<String> defaultComboBoxModel1 = new DefaultComboBoxModel<String>();
		defaultComboBoxModel1.addElement("http://services.chronos.org/");
		defaultComboBoxModel1.addElement("http://webserv.iodp.tamu.edu:8080/");
		defaultComboBoxModel1.addElement("http://webserv.ship.iodp.tamu.edu:8080/");
		chronosServiceComboBox.setModel(defaultComboBoxModel1);
		panel5.add(chronosServiceComboBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		imageSiteLabel = new JLabel();
		imageSiteLabel.setDisabledIcon(new ImageIcon(getClass().getResource("/corelyzer/ui/resources/indicator.gif")));
		imageSiteLabel.setText("Site: ");
		panel5.add(imageSiteLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JLabel label2 = new JLabel();
		label2.setText("Depth filter (m): ");
		panel5.add(label2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		filterTextField = new JTextField();
		filterTextField.setHorizontalAlignment(10);
		filterTextField.setText("0.0");
		filterTextField.setToolTipText("Range example: 0-10");
		panel5.add(filterTextField, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		final JScrollPane scrollPane2 = new JScrollPane();
		panel5.add(scrollPane2, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0,
				false));
		scrollPane2.setViewportView(resultTable);
		searchLabel = new JLabel();
		searchLabel.setDisabledIcon(new ImageIcon(getClass().getResource("/corelyzer/ui/resources/indicator.gif")));
		searchLabel.setText("Search results: ");
		panel5.add(searchLabel, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		siteField = new JComboBox<String>();
		siteField.setEditable(true);
		final DefaultComboBoxModel<String> defaultComboBoxModel2 = new DefaultComboBoxModel<String>();
		siteField.setModel(defaultComboBoxModel2);
		panel5.add(siteField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		loggingDBPane = new JPanel();
		loggingDBPane.setLayout(new GridLayoutManager(4, 2, new Insets(0, 0, 0, 0), -1, -1));
		listTabPane.addTab("Logging DB", loggingDBPane);
		serviceLabel = new JLabel();
		serviceLabel.setDisabledIcon(new ImageIcon(getClass().getResource("/corelyzer/ui/resources/indicator.gif")));
		serviceLabel.setEnabled(true);
		serviceLabel.setHorizontalAlignment(10);
		serviceLabel.setText("Service provider: ");
		loggingDBPane.add(serviceLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
				GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(120, -1), new Dimension(120, -1), new Dimension(120, -1), 0,
				false));
		srvPrefixComboBox = new JComboBox<String>();
		srvPrefixComboBox.setEditable(false);
		final DefaultComboBoxModel<String> defaultComboBoxModel3 = new DefaultComboBoxModel<String>();
		defaultComboBoxModel3.addElement("http://brg.ldeo.columbia.edu/services/");
		defaultComboBoxModel3.addElement("http://brg.ship.iodp.tamu.edu/services/");
		srvPrefixComboBox.setModel(defaultComboBoxModel3);
		loggingDBPane.add(srvPrefixComboBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JScrollPane scrollPane3 = new JScrollPane();
		loggingDBPane.add(scrollPane3, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK
						| GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
		scrollPane3.setViewportView(dataURLTable);
		dataFilesLabel = new JLabel();
		dataFilesLabel.setDisabledIcon(new ImageIcon(getClass().getResource("/corelyzer/ui/resources/indicator.gif")));
		dataFilesLabel.setEnabled(true);
		dataFilesLabel.setText("Data files: ");
		loggingDBPane.add(dataFilesLabel, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
				GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(100, -1), new Dimension(100, -1), new Dimension(100, -1), 0,
				false));
		legLabel = new JLabel();
		legLabel.setDisabledIcon(new ImageIcon(getClass().getResource("/corelyzer/ui/resources/indicator.gif")));
		legLabel.setText("Leg:");
		legLabel.setToolTipText("For example: 199, 200");
		loggingDBPane.add(legLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		legComboBox = new JComboBox<String>();
		legComboBox.setEditable(true);
		legComboBox.setToolTipText("For example: 199, 200");
		loggingDBPane.add(legComboBox, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		siteLabel = new JLabel();
		siteLabel.setDisabledIcon(new ImageIcon(getClass().getResource("/corelyzer/ui/resources/indicator.gif")));
		siteLabel.setText("Site: ");
		siteLabel.setToolTipText("For example: 1218, U1313");
		loggingDBPane.add(siteLabel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		siteComboBox = new JComboBox<String>();
		siteComboBox.setEditable(true);
		siteComboBox.setToolTipText("For example: 1218, U1313");
		loggingDBPane.add(siteComboBox, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JPanel panel6 = new JPanel();
		panel6.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
		panel6.setEnabled(false);
		listTabPane.addTab("Affine table", panel6);
		final JScrollPane scrollPane4 = new JScrollPane();
		panel6.add(scrollPane4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0,
				false));
		scrollPane4.setViewportView(affineTable);
		final JPanel panel7 = new JPanel();
		panel7.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
		panel7.setEnabled(false);
		listTabPane.addTab("Splice table", panel7);
		final JScrollPane scrollPane5 = new JScrollPane();
		panel7.add(scrollPane5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0,
				false));
		scrollPane5.setViewportView(spliceTable);
	}

	private void createUIComponents() {
		sectionListTable = new JTable() {
			private static final long serialVersionUID = 1L;

			@Override
			public Component prepareRenderer(final TableCellRenderer renderer, final int Index_row, final int Index_col) {

				Component comp = super.prepareRenderer(renderer, Index_row, Index_col);

				// even index, selected or not selected
				if (!isCellSelected(Index_row, Index_col)) {
					if (Index_row % 2 == 0) {
						comp.setBackground(new Color(239, 242, 255));
					} else {
						comp.setBackground(Color.white);
					}
				}

				return comp;
			}
		};

		resultTable = new JTable() {
			private static final long serialVersionUID = 1L;

			@Override
			public Component prepareRenderer(final TableCellRenderer renderer, final int Index_row, final int Index_col) {

				Component comp = super.prepareRenderer(renderer, Index_row, Index_col);

				// even index, selected or not selected
				if (!isCellSelected(Index_row, Index_col)) {
					if (Index_row % 2 == 0) {
						comp.setBackground(new Color(239, 242, 255));
					} else {
						comp.setBackground(Color.white);
					}
				}

				return comp;
			}
		};

		dataURLTable = new JTable() {
			private static final long serialVersionUID = 1L;

			@Override
			public Component prepareRenderer(final TableCellRenderer renderer, final int Index_row, final int Index_col) {

				Component comp = super.prepareRenderer(renderer, Index_row, Index_col);

				// even index, selected or not selected
				if (!isCellSelected(Index_row, Index_col)) {
					if (Index_row % 2 == 0) {
						comp.setBackground(new Color(239, 242, 255));
					} else {
						comp.setBackground(Color.white);
					}
				}

				return comp;
			}
		};

		affineTable = new JTable() {
			private static final long serialVersionUID = 1L;

			@Override
			public Component prepareRenderer(final TableCellRenderer renderer, final int Index_row, final int Index_col) {

				Component comp = super.prepareRenderer(renderer, Index_row, Index_col);

				// even index, selected or not selected
				if (!isCellSelected(Index_row, Index_col)) {
					if (Index_row % 2 == 0) {
						comp.setBackground(new Color(239, 242, 255));
					} else {
						comp.setBackground(Color.white);
					}
				}

				return comp;
			}
		};

		spliceTable = new JTable() {
			private static final long serialVersionUID = 1L;

			@Override
			public Component prepareRenderer(final TableCellRenderer renderer, final int Index_row, final int Index_col) {

				Component comp = super.prepareRenderer(renderer, Index_row, Index_col);

				// even index, selected or not selected
				if (!isCellSelected(Index_row, Index_col)) {
					if (Index_row % 2 == 0) {
						comp.setBackground(new Color(239, 242, 255));
					} else {
						comp.setBackground(Color.white);
					}
				}

				return comp;
			}
		};
	}

	private void doChronosQuery() {
		// do Chronos query
		String serviceQueryURL = chronosServiceComboBox.getSelectedItem().toString();

		// Use service prefix + service path to query whether the service is
		// really available.
		if (serviceQueryURL.toLowerCase().endsWith(".iodp.tamu.edu:8080/")) {
			serviceQueryURL += "resteasy-lims-webservices/re";
		}

		if (!isServiceReachable(serviceQueryURL)) {
			JOptionPane.showMessageDialog(this, "Service unavailable.");
			return;
		}

		if (siteField.getSelectedItem().toString().equals("")) {
			String mesg = "Please input \'Site\' for query.";
			JOptionPane.showMessageDialog(this, mesg);

			return;
		}

		resetButton.setEnabled(false);

		searchLabel.setEnabled(false);
		searchLabel.setText("Refreshing...");

		Thread doQuery = new Thread() {
			@Override
			public void run() {
				onChronosQuery();
			}
		};

		doQuery.start();
	}

	private void doLoggingDBQuery() {
		// ignore if service unreachable
		if (!isServiceReachable(srvPrefixComboBox.getSelectedItem().toString())) {
			return;
		}

		if (legComboBox.getSelectedItem().toString().equals("") && siteComboBox.getSelectedItem().toString().equals("")) {
			String mesg = "Both 'Leg' and 'Site' input parameters are empty.\n" + "Please input or select at least one of them.";
			JOptionPane.showMessageDialog(this, mesg);

			return;
		}

		resetButton.setEnabled(false);
		dataFilesLabel.setEnabled(false);
		dataFilesLabel.setText("Refreshing...");

		Thread doQuery = new Thread() {
			@Override
			public void run() {
				onQuery();
			}
		};

		doQuery.start();
	}

	private void fillInChronosQueryTable(final String line, final float[] filterDepthRange, final ResultTableModel model) {
		String[] toks = line.split("\t");

		if (toks.length >= 10) { // 10: depth(mbsf)
			float depth = Float.parseFloat(toks[10]);

			if ((depth >= filterDepthRange[0]) && (depth <= filterDepthRange[1])) {
				model.addRow(line);

				// Load queries into LIMSImageryDir
				LIMSImageryDirectory dir = LIMSImageryDirectory.getDirectory();
				dir.addChronosTokensToSectionTable(toks);

				return;
			} else {
				return;
			}
		}

		model.addRow(line);
	}

	private String getRealSiteWithWildCard(final String site) {
		String realSite = site;

		String queryString = srvPrefixComboBox.getSelectedItem().toString() + "LogHole/?site=" + site;

		DOMParser parser = new DOMParser();

		try {
			parser.setFeature("http://apache.org/xml/features/dom/" + "include-ignorable-whitespace", false);

			parser.parse(queryString);
			Document doc = parser.getDocument();
			Element e = doc.getDocumentElement(); // brg:LogHoles

			NodeList logHoleList = e.getChildNodes();
			for (int i = 0; i < logHoleList.getLength(); i++) {
				if (!(logHoleList.item(i) instanceof Element)) {
					continue;
				}

				Element logHoleElement = (Element) logHoleList.item(i);

				NodeList propertyList = logHoleElement.getChildNodes();
				for (int j = 0; j < propertyList.getLength(); j++) {
					if (!(propertyList.item(j) instanceof Element)) {
						continue;
					}

					Element property = (Element) propertyList.item(j);
					if (property.getTagName().equalsIgnoreCase("site")) {
						realSite = property.getTextContent();
					}
				}
			}

		} catch (SAXException e) {
			e.printStackTrace();

			return site;
		} catch (IOException e) {
			e.printStackTrace();

			return site;
		}

		return realSite;
	}

	private void initAffineTable() {
		// Affine table
		// - model
		AffineTableModel aModel = new AffineTableModel();
		affineTable.setModel(aModel);

		for (int i = 0; i < aModel.getColumnCount(); i++) {
			String header = aModel.getColumnName(i);

			affineTable.getColumnModel().getColumn(i).setHeaderValue(header);
		}
		affineTable.updateUI();
	}

	private void initLogDBTable() {
		LogDBQueryResultTableModel model = new LogDBQueryResultTableModel();
		// TableSorter sorter = new TableSorter(model);
		// sorter.setTableHeader(this.sectionListTable.getTableHeader());

		dataURLTable.setModel(model); // !sorter

		// table decorations
		for (int i = 0; i < model.getColumnCount(); i++) {
			String header = model.getColumnName(i);

			dataURLTable.getColumnModel().getColumn(i).setHeaderValue(header);
		}

		dataURLTable.addMouseListener(new MouseListener() {
			public void mouseClicked(final MouseEvent mouseEvent) {
			}

			public void mouseEntered(final MouseEvent mouseEvent) {
			}

			public void mouseExited(final MouseEvent mouseEvent) {
			}

			public void mousePressed(final MouseEvent mouseEvent) {
			}

			public void mouseReleased(final MouseEvent mouseEvent) {
				boolean shouldBeEnabled = (dataURLTable.getSelectedRowCount() > 0)
						&& !((String) dataURLTable.getModel().getValueAt(0, 0)).startsWith("Nothing");

				applyButton.setEnabled(shouldBeEnabled);
			}
		});

		dataURLTable.updateUI();
	}

	private void initResultTable() {
		// Section list
		// - model
		ResultTableModel model = new ResultTableModel();
		// TableSorter sorter = new TableSorter(model);
		// sorter.setTableHeader(this.sectionListTable.getTableHeader());

		resultTable.setModel(model); // !sorter

		// table decorations
		for (int i = 0; i < model.getColumnCount(); i++) {
			String header = model.getColumnName(i);

			resultTable.getColumnModel().getColumn(i).setHeaderValue(header);
		}

		resultTable.updateUI();
	}

	private void initSectionTable() {
		// Section list
		// - model
		SectionListTableModel model = new SectionListTableModel();
		// TableSorter sorter = new TableSorter(model);
		// sorter.setTableHeader(this.sectionListTable.getTableHeader());

		sectionListTable.setModel(model); // !sorter

		// table decorations
		for (int i = 0; i < model.getColumnCount(); i++) {
			String header = model.getColumnName(i);

			sectionListTable.getColumnModel().getColumn(i).setHeaderValue(header);
		}

		sectionListTable.updateUI();
	}

	private void initSpliceTable() {
		// Splice table
		// - model
		SpliceTableModel sModel = new SpliceTableModel();
		spliceTable.setModel(sModel);

		for (int i = 0; i < sModel.getColumnCount(); i++) {
			String header = sModel.getColumnName(i);
			spliceTable.getColumnModel().getColumn(i).setHeaderValue(header);
		}
		spliceTable.updateUI();

		// enable apply buttons if data already populated in LIMSDir
		if (sectionListTable.getModel().getRowCount() > 0) {
			if (affineTable.getModel().getRowCount() > 0) {
				applyButton.setEnabled(true);
			}

			if (spliceTable.getModel().getRowCount() > 0) {
				applyButton.setEnabled(true);
			}
		}
	}

	private void initTables() {
		initSectionTable();
		sectionListTable.addMouseListener(new SectionListTableMouseListener(sectionListTable, applyButton));

		initResultTable();
		resultTable.addMouseListener(new SectionListTableMouseListener(resultTable, applyButton));

		initAffineTable();
		initSpliceTable();
		initLogDBTable();
	}

	// Input like: http://brg.ldeo.columbia.edu/services/
	// http://brg.ship.iodp.tamu.edu/services/
	private boolean isServiceReachable(final String serviceURLString) {
		try {
			URL serviceURL = new URL(serviceURLString);
			URLConnection connection = serviceURL.openConnection();
			connection.setConnectTimeout(30000);
			connection.connect();

			InputStreamReader isr = new InputStreamReader(connection.getInputStream());
			isr.close();
		} catch (MalformedURLException e) {
			e.printStackTrace();

			return false;
		} catch (IOException e) {
			e.printStackTrace();

			return false;
		} catch (Exception e) {
			e.printStackTrace();

			return false;
		}

		return true;
	}

	public void loadAAffineTable(final String aFile) {
		File f = new File(aFile);

		if (f.exists()) {
			LIMSImageryDirectory dir = LIMSImageryDirectory.getDirectory();
			if (dir == null) {
				return;
			}

			if (FileUtility.getExtension(f).equalsIgnoreCase("xml")) {
				boolean isLoaded = dir.loadXMLAffineTable(f);
				if (!isLoaded) {
					JOptionPane.showMessageDialog(this, "Error in loading '" + f + "'");
				}
			} else {
				dir.loadPlainTextAffineTable(f);
			}
		}
	}

	public void loadAAffineTable(Window parent) {
		if (parent == null) {
			parent = this;
		}

		String aFile = FileUtility.selectASingleFile(parent, "Select an affine table", null, FileUtility.LOAD);

		if (aFile != null) {
			this.loadAAffineTable(aFile);
		}
	}

	public void loadASpliceTable(final String aFile) {
		File f = new File(aFile);

		if (f.exists()) {
			LIMSImageryDirectory dir = LIMSImageryDirectory.getDirectory();
			if (dir == null) {
				return;
			}

			if (FileUtility.getExtension(f).equalsIgnoreCase("xml")) {
				boolean isLoaded = dir.loadXMLSpliceTableFile(f);
				if (!isLoaded) {
					JOptionPane.showMessageDialog(this, "Error in loading '" + f + "'");
				}
			} else { // plain text format doesn't have leg column in it
				String leg = JOptionPane.showInputDialog(this, "What is the leg number?");

				if (leg != null) {
					dir.loadPlainTextSpliceTableFile(f, leg);
				}
			}
		}
	}

	// just for plain text file with additional leg parameter
	public void loadASpliceTable(final String aFile, final String leg) {
		File f = new File(aFile);

		if (f.exists()) {
			LIMSImageryDirectory dir = LIMSImageryDirectory.getDirectory();
			if (dir == null) {
				return;
			}

			dir.loadPlainTextSpliceTableFile(f, leg);
		}
	}

	public void loadASpliceTable(Window parent) {
		if (parent == null) {
			parent = this;
		}

		String aFile = FileUtility.selectASingleFile(parent, "Select a splice table", null, FileUtility.LOAD);

		if (aFile != null) {
			this.loadASpliceTable(aFile);
		}
	}

	private void loadFMSFile(final String local, final String url, final int trackId) {
		File inputFile = new File(local);

		try {
			// Convert GIF to JPG format
			BufferedImage inImage = ImageIO.read(inputFile);
			String outFilename = local.substring(0, local.length() - 4) + ".jpg";
			File outFile = new File(outFilename);
			ImageIO.write(inImage, "jpg", outFile);

			// Load into current track
			CorelyzerApp app = CorelyzerApp.getApp();
			if (app != null) {
				int secId = app.loadImage(outFile, url);

				if (secId != -1) {
					SceneGraph.setSectionOrientation(trackId, secId, SceneGraph.PORTRAIT);

					float dpi = 72.0f;
					float xpos = 0.0f;
					float ypos = 0.0f;

					// Guess the image's starting depth and length
					// 162-984B_p2_476_496.jpg or 162-984B_p2_D_476_496.jpg
					String[] toks = outFile.getName().split("_");

					try {
						float dpix = SceneGraph.getCanvasDPIX(0);
						int lastTokIdx = toks.length - 1;

						int dotPos = toks[lastTokIdx].trim().indexOf(".");
						String endDepthStr = toks[lastTokIdx].trim().substring(0, dotPos);

						float startDepth = Float.parseFloat(toks[lastTokIdx - 1].trim());
						float endDepth = Float.parseFloat(endDepthStr);
						float length = (endDepth - startDepth) * 100.0f / 2.54f;

						int imageId = SceneGraph.getImageIdForSection(trackId, secId);
						float imageWidth = SceneGraph.getImageHeight(imageId);

						dpi = imageWidth / length;
						xpos = startDepth * 100.0f / 2.54f * dpix;
					} catch (NumberFormatException e) {
						System.err.println("Invalid FMS filename convention.");
						e.printStackTrace();
					} catch (ArrayIndexOutOfBoundsException e) {
						System.err.println("Index out of bounds");
						e.printStackTrace();
					}

					SceneGraph.setSectionDPI(trackId, secId, dpi, dpi);
					SceneGraph.positionSection(trackId, secId, xpos, ypos);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// with no UI
	public void loadLIMSTables(final String[] tableFileStrs) {
		if (tableFileStrs != null) {
			LIMSImageryDirectory dir = LIMSImageryDirectory.getDirectory();
			dir.loadImagesTableFiles(this, tableFileStrs);

			initSectionTable(); // workaround for # of column changes
		}
	}

	// with UI
	public void loadLIMSTables(Window parent) {
		if (parent == null) {
			parent = this;
		}

		String[] tableFileStrs = FileUtility.selectMultipleFiles(parent, "Select image listing files", null);

		if (tableFileStrs != null) {
			loadLIMSTables(tableFileStrs);
		}
	}

	private void loadSelectedFiles() {
		final int[] selectedRows = dataURLTable.getSelectedRows();

		CorelyzerApp app = CorelyzerApp.getApp();
		if (app == null) {
			return;
		}

		final JProgressBar progress = app.getProgressUI();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				progress.setMaximum(selectedRows.length);
				progress.setValue(0);
			}
		});

		boolean notWarnYet = true;
		for (int i = 0; i < selectedRows.length; i++) {
			int rowIndex = selectedRows[i];
			final int p = i;

			String leg = dataURLTable.getModel().getValueAt(rowIndex, 0).toString();
			String site = dataURLTable.getModel().getValueAt(rowIndex, 1).toString();
			String tool = dataURLTable.getModel().getValueAt(rowIndex, 2).toString();
			// String measurement =
			// this.dataURLTable.getModel().getValueAt(rowIndex, 3).toString();
			String filename = dataURLTable.getModel().getValueAt(rowIndex, 4).toString();
			final String fileURL = dataURLTable.getModel().getValueAt(rowIndex, 5).toString();
			String passStr = dataURLTable.getModel().getValueAt(rowIndex, LogDBQueryResultTableModel.PASS_INDEX).toString();

			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					progress.setValue(p);
					progress.setString("Loading " + fileURL);
				}
			});

			try {
				URL url = new URL(fileURL);

				String prefix = app.preferences().getLocalRepositoryPath();
				String sessionName = leg + "_" + site;
				String fileName = new File(url.getFile()).getName();
				String sp = System.getProperty("file.separator");
				String dir = fileName.toLowerCase().endsWith(".gif") ? "images" : "datasets";
				String local = prefix + sp + sessionName + sp + dir + sp + fileName;

				// Create parent directory if necessary
				FileUtility.createDirsIfNecessary(new File(local).getParentFile());

				String extension = FileUtility.getExtension(new File(local));
				if (!extension.equalsIgnoreCase("dat") && !extension.equalsIgnoreCase("tsv") && !extension.equalsIgnoreCase("csv")
						&& !extension.equalsIgnoreCase("gif")) {
					JOptionPane.showMessageDialog(this, "File '" + fileURL + "' is not recognized.");
					continue;
				}

				boolean isDownloaded;
				try {
					isDownloaded = URLRetrieval.retrieveLocalCopy(fileURL, local);
				} catch (IOException e) {
					isDownloaded = false;
					e.printStackTrace();
				}

				if (isDownloaded) {
					// Create and select required session and track
					CoreGraph cg = CoreGraph.getInstance();
					Session s = cg.getSession(sessionName);
					if (s == null) {
						s = new Session(sessionName);
						cg.addSession(s);
					}

					// Create its own track
					String trackName;

					if (extension.equalsIgnoreCase("gif")) { // FMS
						String[] myPassStrings = filename.split(" ");
						if (myPassStrings.length > 2) {
							trackName = passStr + " " + myPassStrings[2];
						} else {
							trackName = tool + "_" + passStr;
						}
					} else {
						trackName = filename + "_Graph";
					}

					int nativeTrackId = app.getController().createTrack(trackName);
					TrackSceneNode t = s.getTrackSceneNodeWithTrackId(nativeTrackId);
					if (t == null) {
						continue;
					}

					// check file extension
					if (extension.equalsIgnoreCase("dat") || extension.equalsIgnoreCase("tsv") || extension.equalsIgnoreCase("csv")) {
						boolean knownFormat = preCheckFileFormat(local);
						if (knownFormat) {
							CRDepthValueDataLoader loader = new CRDepthValueDataLoader(new File(local), 4, false, "\t");
							loader.load();

							// automatically create graphs
						} else {
							String mesg = "The selected file URL downloaded in\n'" + local + "'\n is in unknown format.";

							JOptionPane.showMessageDialog(this, mesg);
						}
					} else if (extension.equalsIgnoreCase("gif")) {
						if (notWarnYet) {
							String mesg = "Will attempt to load the FMS image with\n" + "depth and length information from file's name.\n"
									+ "It may not be accurate.\n" + "You can fine-tune parameters in images' properties";
							JOptionPane.showMessageDialog(this, mesg);
							notWarnYet = false;
						}

						loadFMSFile(local, fileURL, t.getId());
					} else {
						JOptionPane.showMessageDialog(this, "File '" + fileURL + "' is not recognized.");
					}
				} else {
					JOptionPane.showMessageDialog(this, "Cannot download file from URL '" + fileURL + "'");
				}
			} catch (MalformedURLException e) {
				System.err.println("Invalid URL: '" + fileURL + "'");
				e.printStackTrace();
			}
		}

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				progress.setValue(selectedRows.length);
				progress.setString("All selected loaded");
			}
		});
	}

	private String massageLIMSOutputToChronosForm(final String line) {
		String[] toks = line.split(",");

		// w: working half, ignore for now
		// a: archive half
		if (toks[7].equalsIgnoreCase("w")) {
			return null;
		}

		String exp = toks[1].trim();

		float topDepth = Float.parseFloat(toks[10].trim());
		float bottomDepth = Float.parseFloat(toks[11].trim());
		float length = bottomDepth - topDepth;

		String asim_id = toks[21].trim();

		// webserv.XXX -> for query
		// web.XXX -> for download images from asim
		String servicePrefix = chronosServiceComboBox.getSelectedItem().toString().replace("webserv", "web");
		String url = servicePrefix + "resteasy-asman/re?service=getFile&catalogName=EXP" + exp + "&recordId=" + asim_id;

		String newLine = toks[1] + "\t" + toks[2] + "\t" + toks[3] + "\t" + toks[4] + "\t" + toks[6] + "\t0.0\tS\t" + toks[5] + "\t" + length + "\t" + length
				+ "\t" + topDepth + "\tJPG\t0.0\t" + url;

		return newLine;
	}

	private void onApply() {
		switch (listTabPane.getSelectedIndex()) {
			case 0:
				onApplySectionList(sectionListTable);
				break;

			case 3:
				onApplyAffineTable();
				break;

			case 4:
				onApplySpliceTable();
				break;

			case 2:
				onApplyLoggingDB();
				break;

			case 1:
				onApplySectionList(resultTable);
				break;

			default:
		}
	}

	private void onApplyAffineTable() {
		int sel = JOptionPane.showConfirmDialog(this, "Do you want to apply affine table?");

		if (sel == JOptionPane.YES_OPTION) {
			LIMSImageryDirectory dir = LIMSImageryDirectory.getDirectory();
			ControlServerApplication app = ControlServerApplication.getControlServer();

			IODPOperationController.applyAffineTable(this, dir, app);
			// this.dispose();
		}
	}

	// load select rows in url table
	private void onApplyLoggingDB() {
		if (dataURLTable.getSelectedRowCount() <= 0) {
			return;
		}

		Thread loadThread = new Thread() {
			@Override
			public void run() {
				loadSelectedFiles();
			}
		};

		loadThread.start();
	}

	private void onApplySectionList(final JTable aTable) {
		int sel = JOptionPane.showConfirmDialog(this, "Do you want to load selected sections?");

		if (sel == JOptionPane.YES_OPTION) {
			LIMSImageryDirectory dir = LIMSImageryDirectory.getDirectory();
			ControlServerApplication app = ControlServerApplication.getControlServer();

			int[] selectedRows;
			sel = JOptionPane.showConfirmDialog(this, "Ignore CC(Core Catcher) sections?");
			if (sel == JOptionPane.YES_OPTION) {
				Vector<Integer> filtered = new Vector<Integer>();
				TableModel model = aTable.getModel();

				for (int row : aTable.getSelectedRows()) {
					String section = (String) model.getValueAt(row, LIMSImageryDirectory.SECTION_INDEX);
					if (!section.trim().toLowerCase().equals("cc")) {
						filtered.add(row);
					}
				}

				selectedRows = new int[filtered.size()];
				for (int i = 0; i < filtered.size(); i++) {
					selectedRows[i] = filtered.elementAt(i);
				}
			} else {
				selectedRows = aTable.getSelectedRows();
			}

			IODPOperationController.applySelectedSections(this, dir, app, aTable.getModel(), selectedRows);
		}
	}

	private void onApplySpliceTable() {
		LIMSImageryDirectory dir = LIMSImageryDirectory.getDirectory();

		String defaultTrackName = dir.getSpliceTableCell(0, 0) + "_" + dir.getSpliceTableCell(0, 1) + "-spliceTrack";

		String spliceTrackName = JOptionPane.showInputDialog(this, "Please input a splice track name", defaultTrackName);

		if (spliceTrackName != null) {
			String src;
			if (affineTable.getModel().getRowCount() != 0) { // Use affine
																// table
				IODPOperationController.setApplyAffineTableToOriginalSections(true);
				IODPOperationController.setAffineShiftSource(IODPOperationController.ShiftSource.FROM_AFFINE);

				src = "affine table";
			} else { // Use splice table differential
				IODPOperationController.setApplyAffineTableToOriginalSections(true);
				IODPOperationController.setAffineShiftSource(IODPOperationController.ShiftSource.FROM_SPLICE);

				src = "splice table";
			}

			System.out.println("- Use " + src + " in IODPListsDialog#onApplySpliceTable()");

			ControlServerApplication app = ControlServerApplication.getControlServer();

			IODPOperationController.applySpliceTable(this, dir, app, spliceTrackName);
			// this.dispose();
		}
	}

	private void onCancel() {
		dispose();
	}

	private void onChronosQuery() {
		// run the query
		final ResultTableModel model = (ResultTableModel) resultTable.getModel();
		model.clear();

		String query = null;

		int serviceIndex = chronosServiceComboBox.getSelectedIndex();
		switch (serviceIndex) {
			case 0: // Chronos Janus
				query = "http://services.chronos.org/xqe/public" + "/iodp.janus.core-images?callback=displayNexus&" + "site="
						+ siteField.getSelectedItem().toString()
						// + "&hole=" + holeField.getText()
						+ "&serializeAs=tsv"
						// + "&filter=curated_length,ImageURL"
						+ "&noHeader=true";
				break;

			case 1: // on shore lims service query
			case 2: // on ship lims service query
				query = chronosServiceComboBox.getSelectedItem().toString()
						+ "resteasy-lims-webservices/re?service=getSciDataFile&returnType=default&qaqc=false&site=" + siteField.getSelectedItem().toString()
						+ "&analysis=LSIMG";

				break;
		}

		if (query == null) {
			JOptionPane.showMessageDialog(this, "Unsupported service option");
			return;
		}

		System.out.println("- Query is: " + query);

		String filterString = filterTextField.getText();
		float[] filterDepthRange = parseDepthRange(filterString);

		URL remote;
		URLConnection uc;
		InputStreamReader isr;
		BufferedReader br;

		try {
			remote = new URL(query);
			uc = remote.openConnection();
			isr = new InputStreamReader(uc.getInputStream());
			br = new BufferedReader(isr);

			int count = 0;
			String line;
			while ((line = br.readLine()) != null) {

				switch (serviceIndex) {
					case 0: // chronos
						fillInChronosQueryTable(line, filterDepthRange, model);
						break;

					case 1: // on shore lims (csv)
					case 2: // on ship lims (csv)
						if (count > 3) {
							String newLine = massageLIMSOutputToChronosForm(line);
							if (newLine != null) {
								fillInChronosQueryTable(newLine, filterDepthRange, model);
							}
						}

						break;
				}

				count++;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				resultTable.updateUI();

				resetButton.setEnabled(true);
				searchLabel.setEnabled(true);
				searchLabel.setText("Search results: ");
			}
		});
	}

	private void onOK() {
		dispose();
	}

	private void onOpen() {
		JTable targetTable = null;

		switch (listTabPane.getSelectedIndex()) {
			case 0:
				this.loadLIMSTables(this);
				targetTable = sectionListTable;

				break;

			case 3:
				this.loadAAffineTable(this);
				targetTable = affineTable;

				if (targetTable.getModel().getRowCount() > 0) {
					applyButton.setEnabled(true);
				}

				break;

			case 4:
				this.loadASpliceTable(this);
				targetTable = spliceTable;

				if (targetTable.getModel().getRowCount() > 0) {
					applyButton.setEnabled(true);
				}

				break;

			default:
		}

		if (targetTable != null) {
			targetTable.updateUI();
		}
	}

	private synchronized void onQuery() {
		String leg = legComboBox.getSelectedItem().toString();
		String site = siteComboBox.getSelectedItem().toString();
		// String hole = this.holeComboBox.getSelectedItem().toString();
		// String tool = this.toolComboBox.getSelectedItem().toString();
		String hole = "";
		String tool = "";

		// Query SITE from LogHole first and use the result to do the real
		// query?
		String realSite = getRealSiteWithWildCard(site);
		if ((realSite != null) && !realSite.equals("")) {
			site = realSite;
		}

		String queryURL = srvPrefixComboBox.getSelectedItem().toString() + "LogFile/?leg=" + leg + "&site=" + site; // +
																													// "&hole="
																													// +
																													// hole
																													// +
																													// "&tool="
																													// +
																													// tool;

		if (!isServiceReachable(queryURL)) {
			JOptionPane.showMessageDialog(this, "Selected service '" + srvPrefixComboBox.getSelectedItem() + "'\nis not reachable.");

			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					resetButton.setEnabled(true);
					applyButton.setEnabled(false);

					dataFilesLabel.setEnabled(true);
					dataFilesLabel.setText("Data files: ");
				}
			});
			return;
		}

		final LogDBQueryResultTableModel model = (LogDBQueryResultTableModel) dataURLTable.getModel();
		model.clear();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				dataURLTable.updateUI();
			}
		});

		DOMParser parser = new DOMParser();
		try {
			parser.setFeature("http://apache.org/xml/features/dom/" + "include-ignorable-whitespace", false);

			parser.parse(queryURL);
			Document doc = parser.getDocument();
			Element e = doc.getDocumentElement(); // brg:Logfiles

			NodeList logFileList = e.getChildNodes();
			for (int i = 0; i < logFileList.getLength(); i++) {
				if (!(logFileList.item(i) instanceof Element)) {
					continue;
				}

				Element logFileElement = (Element) logFileList.item(i);
				// String leg = null;
				String measurement = null;
				String filename = null;
				String fileUrlStr = null;
				String passStr = null;

				NodeList metaList = logFileElement.getChildNodes();
				for (int j = 0; j < metaList.getLength(); j++) {
					if (!(metaList.item(j) instanceof Element)) {
						continue;
					}

					Element metaElement = (Element) metaList.item(j);

					String tagName = metaElement.getTagName();
					if (tagName.equalsIgnoreCase("leg")) {
						NodeList nL = metaElement.getChildNodes();
						leg = nL.item(0).getNodeValue();
					} else if (tagName.equalsIgnoreCase("measurement")) {
						NodeList nL = metaElement.getChildNodes();
						measurement = nL.item(0).getNodeValue();
					} else if (tagName.equalsIgnoreCase("fileurl")) {
						NodeList nL = metaElement.getChildNodes();
						fileUrlStr = nL.item(0).getNodeValue();
					} else if (tagName.equalsIgnoreCase("filename")) {
						NodeList nL = metaElement.getChildNodes();
						filename = nL.item(0).getNodeValue();
					} else if (tagName.equalsIgnoreCase("tool")) {
						NodeList nL = metaElement.getChildNodes();
						tool = nL.item(0).getNodeValue();
					} else if (tagName.equalsIgnoreCase("hole")) {
						NodeList nL = metaElement.getChildNodes();
						hole = nL.item(0).getNodeValue();
					} else if (tagName.equalsIgnoreCase("pass")) {
						NodeList nL = metaElement.getChildNodes();
						if (nL.getLength() > 0) {
							passStr = nL.item(0).getNodeValue();
						} else {
							passStr = "";
						}
					}
				}

				// create and packaging into table model
				String[] row = { leg, hole, tool, measurement, filename, fileUrlStr, passStr };
				model.addRow(row);
			}
		} catch (SAXNotRecognizedException e) {
			e.printStackTrace();
		} catch (SAXNotSupportedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}

		// notify nothing found
		if (model.getRowCount() == 0) {
			String[] row = { "Nothing ", "found!" };
			model.addRow(row);
		}

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				dataURLTable.updateUI();

				resetButton.setEnabled(true);
				dataFilesLabel.setEnabled(true);
				dataFilesLabel.setText("Data files: ");
			}
		});
	}

	// Refresh logging db input UIs
	private void onRefresh() {
		// Test if the service is reachable...
		if (!isServiceReachable(srvPrefixComboBox.getSelectedItem().toString())) {
			showServiceInvalid();

			return;
		}

		try {
			// parse schema and update combobox models
			final String legSchemaURL = srvPrefixComboBox.getSelectedItem().toString() + "vocab/Leg.xsd";
			final String siteSchemaURL = srvPrefixComboBox.getSelectedItem().toString() + "vocab/Site.xsd";
			// final String holeSchemaURL =
			// this.srvPrefixComboBox.getSelectedItem().toString() +
			// "vocab/Hole.xsd";
			// final String toolSchemaURL =
			// this.srvPrefixComboBox.getSelectedItem().toString() +
			// "vocab/Tool.xsd";

			final DefaultComboBoxModel<String> legListModel = updateComboBoxModel(legSchemaURL, "leg");
			final DefaultComboBoxModel<String> siteListModel = updateComboBoxModel(siteSchemaURL, "site");
			// final DefaultComboBoxModel holeListModel =
			// updateComboBoxModel(holeSchemaURL, "hole");
			// final DefaultComboBoxModel toolListModel =
			// updateComboBoxModel(toolSchemaURL, "tool");

			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					legComboBox.setModel(legListModel);
					siteComboBox.setModel(siteListModel);
					// holeComboBox.setModel(holeListModel);
					// toolComboBox.setModel(toolListModel);

					resetButton.setEnabled(true);
					srvPrefixComboBox.setEnabled(true);

					legComboBox.setEnabled(true);
					siteComboBox.setEnabled(true);
					// holeComboBox.setEnabled(true);
					// toolComboBox.setEnabled(true);

					serviceLabel.setEnabled(true);
					serviceLabel.setText("Service provider: ");

					legLabel.setEnabled(true);
					legLabel.setText("Leg: ");
					siteLabel.setEnabled(true);
					siteLabel.setText("Site: ");
					// holeLabel.setEnabled(true);
					// holeLabel.setText("Hole: ");
					// toolLabel.setEnabled(true); toolLabel.setText("Tool: ");
				}
			});
		} catch (Exception e) {
			showServiceInvalid();
		}
	}

	private void onRefreshSiteComboBox() {
		// Query available site with given LEG(EXP) info
		String inputLeg = legComboBox.getSelectedItem().toString();
		final DefaultComboBoxModel<String> siteListModel = updateSiteModelWithInputLeg(inputLeg);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				siteComboBox.setModel(siteListModel);

				siteComboBox.setEnabled(true);
				siteLabel.setEnabled(true);
				siteLabel.setText("Site: ");
			}
		});
	}

	private void onRefreshSiteField() {
		// Query available site info using select image list service
		final DefaultComboBoxModel<String> siteFieldsModel = updateSiteFieldModelWithService();

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				siteField.setModel(siteFieldsModel);

				siteField.setEnabled(true);
				imageSiteLabel.setEnabled(true);
				imageSiteLabel.setText("Site: ");
			}
		});
	}

	private void onReset() {
		LIMSImageryDirectory dir = LIMSImageryDirectory.getDirectory();
		if (dir == null) {
			return;
		}

		JTable targetTable = null;
		switch (listTabPane.getSelectedIndex()) {
			case 0: // section list
				if (sectionListTable.getModel().getRowCount() == 0) {
					return;
				}

				dir.clearSectionList();
				targetTable = sectionListTable;

				break;

			case 3: // affine table
				if (affineTable.getModel().getRowCount() == 0) {
					return;
				}

				dir.clearAffineTable();
				targetTable = affineTable;

				break;

			case 4: // splice table
				if (spliceTable.getModel().getRowCount() == 0) {
					return;
				}

				dir.clearSpliceTable();
				targetTable = spliceTable;

				break;

			case 2: // logging db
				doLoggingDBQuery();

				break;

			case 1: // chronos via janus
				doChronosQuery();
				break;
		}

		applyButton.setEnabled(false);
		if (targetTable != null) {
			targetTable.updateUI();
		}
	}

	private void onTabChange() {
		switch (listTabPane.getSelectedIndex()) {
			case 0:
				applyButton.setText("Load selected sections");
				resetButton.setText("Reset...");

				if ((sectionListTable.getModel().getRowCount() <= 0) || (sectionListTable.getSelectedRowCount() <= 0)) {
					applyButton.setEnabled(false);
				} else {
					applyButton.setEnabled(true);
				}

				sectionListTable.updateUI();

				break;

			case 3:
				applyButton.setText("Apply affine table");
				resetButton.setText("Reset...");

				if (affineTable.getModel().getRowCount() <= 0) {
					applyButton.setEnabled(false);
				} else {
					applyButton.setEnabled(true);
				}

				affineTable.updateUI();

				break;

			case 4:
				applyButton.setText("Apply splice table");
				resetButton.setText("Reset...");

				if (spliceTable.getModel().getRowCount() <= 0) {
					applyButton.setEnabled(false);
				} else {
					applyButton.setEnabled(true);
				}

				spliceTable.updateUI();

				break;

			case 2: // Logging database
				applyButton.setText("Load selected data files");
				resetButton.setText("Search");
				getRootPane().setDefaultButton(resetButton);

				if (dataURLTable.getSelectedRowCount() <= 0) {
					applyButton.setEnabled(false);
				} else {
					applyButton.setEnabled(true);
				}

				// refresh srv records
				resetButton.setEnabled(false);
				srvPrefixComboBox.setEnabled(false);

				legComboBox.setEnabled(false);
				siteComboBox.setEnabled(false);
				// holeComboBox.setEnabled(false);
				// toolComboBox.setEnabled(false);

				Thread refreshComboBox = new Thread() {
					@Override
					public void run() {
						onRefresh();
					}
				};

				refreshComboBox.start();

				break;

			case 1: // Chronos
				applyButton.setText("Load selected image files");
				resetButton.setText("Search");
				getRootPane().setDefaultButton(resetButton);

				if (resultTable.getSelectedRowCount() <= 0) {
					applyButton.setEnabled(false);
				} else {
					applyButton.setEnabled(true);
				}

				break;

			default:
		}
	}

	private float[] parseDepthRange(final String filterString) {
		float[] range = new float[2];
		String[] toks = filterString.split("-");

		if (toks.length >= 2) {
			try {
				range[0] = Float.parseFloat(toks[0].trim());
			} catch (NumberFormatException e) {
				range[0] = Float.MIN_VALUE;
			}

			try {
				range[1] = Float.parseFloat(toks[1].trim());
			} catch (NumberFormatException e) {
				range[1] = Float.MAX_VALUE;
			}
		} else {
			try {
				range[0] = Float.parseFloat(toks[0].trim());
			} catch (NumberFormatException e) {
				range[0] = Float.MIN_VALUE;
			}

			range[1] = Float.MAX_VALUE;
		}

		// swap
		if (range[0] > range[1]) {
			float tmp = range[1];
			range[1] = range[0];
			range[0] = tmp;
		}

		return range;
	}

	// Check the file header see if it's in depth-value pairs per row.
	private boolean preCheckFileFormat(final String local) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(local));

			// skip first 4 lines of headers
			for (int i = 0; i < 4; i++) {
				br.readLine();
			}

			String labelLine = br.readLine();
			br.close();
			String[] labels = labelLine.split("\t");
			if (labels.length != 0) {
				if (labels[0].trim().equalsIgnoreCase("depth")) {
					return true;
				} else {
					return false;
				}
			} else {
				return false;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public void setVisibleTab(final int idx) {
		listTabPane.setSelectedIndex(idx);
	}

	private void showServiceInvalid() {
		JOptionPane.showMessageDialog(this, "Selected service '" + srvPrefixComboBox.getSelectedItem() + "'\nis not reachable.");

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				resetButton.setEnabled(true);
				srvPrefixComboBox.setEnabled(true);

				serviceLabel.setEnabled(true);
				serviceLabel.setText("Service provider: ");
				legLabel.setEnabled(true);
				legLabel.setText("Leg: ");
				siteLabel.setEnabled(true);
				siteLabel.setText("Site: ");

				// siteComboBox.setEnabled(true);
				// toolComboBox.setEnabled(true);

				// holeLabel.setEnabled(true);
				// holeLabel.setText("Site: ");
				// toolLabel.setEnabled(true);
				// toolLabel.setText("Tool: ");
			}
		});
	}

	private DefaultComboBoxModel<String> updateComboBoxModel(final String schemaURL, final String labelName) throws Exception {
		DefaultComboBoxModel<String> model = new DefaultComboBoxModel<String>();

		DOMParser parser = new DOMParser();

		parser.setFeature("http://apache.org/xml/features/dom/" + "include-ignorable-whitespace", false);
		parser.parse(schemaURL);

		Document doc = parser.getDocument();
		Element e = doc.getDocumentElement(); // xs:schema

		NodeList simpleTypeList = e.getChildNodes();
		for (int i = 0; i < simpleTypeList.getLength(); i++) {
			if (!(simpleTypeList.item(i) instanceof Element)) {
				continue;
			}

			Element simpleTypeElement = (Element) simpleTypeList.item(i);
			String name = simpleTypeElement.getAttribute("name");

			if (name.equalsIgnoreCase(labelName)) {
				NodeList restrictList = simpleTypeElement.getChildNodes();

				for (int j = 0; j < restrictList.getLength(); j++) {
					if (!(restrictList.item(j) instanceof Element)) {
						continue;
					}

					Element restrictElement = (Element) restrictList.item(j);
					NodeList enumList = restrictElement.getChildNodes();

					for (int k = 0; k < enumList.getLength(); k++) {
						if (!(enumList.item(k) instanceof Element)) {
							continue;
						}

						Element enumElement = (Element) enumList.item(k);
						String value = enumElement.getAttribute("value");

						model.addElement(value);
					}
				}
			}
		}

		return model;
	}

	private DefaultComboBoxModel<String> updateSiteFieldModelWithService() {
		DefaultComboBoxModel<String> model = new DefaultComboBoxModel<String>();
		final String query = chronosServiceComboBox.getSelectedItem() + "resteasy-lims-webservices/re?service=getSciDataFile&qaqc=false&analysis=LATLONG";

		if (isServiceReachable(query)) { // except using chronos
			// http://webserv.ship.iodp.tamu.edu:8080/resteasy-lims-webservices/re?service=getSciDataFile&qaqc=false&analysis=LATLONG
			System.out.println("- Available sites query is: " + query);

			URL remote;
			URLConnection uc;
			InputStreamReader isr;
			BufferedReader br;

			try {
				remote = new URL(query);
				uc = remote.openConnection();
				isr = new InputStreamReader(uc.getInputStream());
				br = new BufferedReader(isr);

				int count = 0;
				String line;
				HashMap<String, String> map = new HashMap<String, String>();

				while ((line = br.readLine()) != null) {
					if (count > 3) { // skip 1st 4 lines
						String[] toks = line.split(",");
						map.put(toks[2].trim(), toks[2].trim());
					}

					count++;
				}

				// populate combobox list model
				for (Map.Entry<String, String> e : map.entrySet()) {
					model.addElement(e.getKey());
				}

				br.close();
				isr.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			// It's chronos
		}

		return model;
	}

	/**
	 * Example: http://brg.ldeo.columbia.edu/services/LogHole/?exp=304
	 */
	private DefaultComboBoxModel<String> updateSiteModelWithInputLeg(final String inputLeg) {
		final String queryURL = srvPrefixComboBox.getSelectedItem().toString() + "LogHole/?exp=" + inputLeg;

		DefaultComboBoxModel<String> model = new DefaultComboBoxModel<String>();

		DOMParser parser = new DOMParser();
		try {
			parser.setFeature("http://apache.org/xml/features/dom/" + "include-ignorable-whitespace", false);
			parser.parse(queryURL);

			Document doc = parser.getDocument();
			Element e = doc.getDocumentElement();

			NodeList logHoleList = e.getChildNodes();
			for (int i = 0; i < logHoleList.getLength(); i++) {
				if (!(logHoleList.item(i) instanceof Element)) {
					continue;
				}

				Element logHoleElement = (Element) logHoleList.item(i);
				NodeList propertyList = logHoleElement.getChildNodes();

				for (int j = 0; j < propertyList.getLength(); j++) {
					if (!(propertyList.item(j) instanceof Element)) {
						continue;
					}

					Element property = (Element) propertyList.item(j);
					String tagName = property.getTagName();

					if (tagName.equalsIgnoreCase("site")) {
						boolean hasTheElement = model.getIndexOf(property.getTextContent()) >= 0;

						if (!hasTheElement) {
							model.addElement(property.getTextContent());
						}
					}
				}
			}
		} catch (SAXNotRecognizedException e) {
			e.printStackTrace();
		} catch (SAXNotSupportedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}

		return model;
	}
}
