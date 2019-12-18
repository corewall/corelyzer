package corelyzer.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.jtechlabs.ui.widget.directorychooser.JDirectoryChooser;

import net.miginfocom.swing.MigLayout;

import corelyzer.data.CRPreferences;
import corelyzer.graphics.SceneGraph;
import corelyzer.util.FileUtility;

public class CRPreferencesDialog extends JDialog implements ChangeListener, WindowListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2332149565974224470L;

	public static void main(final String[] args) {
		CRPreferencesDialog dialog = new CRPreferencesDialog(null);
		dialog.pack();
		dialog.setSize(450, 700);
		dialog.setVisible(true);
	}

	private JPanel contentPane;
	private JButton buttonOK;
	private JButton buttonCancel;
	private JButton helpButton;
	private JTabbedPane stageTab;
	private JCheckBox lockCoreSectionImage;
	private JCheckBox autoCheckVersion;
	private JButton canvasBackgroundColorButton;
	private JCheckBox check_gridEnabled;
	private JComboBox<String> field_gridType;
	private JButton grid_colorbtn;
	private JFormattedTextField field_gridSpace;
	private JFormattedTextField field_gridThickness;
	private JButton imgBtn;
	private JTextField field_imgblock;
	private JButton downBtn;
	private JTextField field_download;
	private JButton tmpBtn;
	private JTextField field_tmpdir;
	private JPanel displayPanel;
	private JCheckBox autoZoomCheckBox;
	private JRadioButton horiDepthRadioButton;
	private JRadioButton vertDepthRadioButton;
	private JTextField serverAddressTextField;
	private JTextField serverPortTextField;
	private JCheckBox showOriginAxisCheckbox;
	private JCheckBox showCoreSectionLabelCheckbox;
	private JCheckBox canvasAlwaysAtBelowCheckBox;
	private JCheckBox depthScrollCheckbox;
	private JTextField disPrefixTextField;
	private JButton disPrefixSelectButton;
	private JTextField dictDefFileTextField;

	private JButton dictDefFileSelectButton;

	public static boolean MAC_OS_X = System.getProperty("os.name").toLowerCase().startsWith("mac os x");
	// data
	// Stage Code
	int number_of_stages = 4;
	final int SETUP_DESC = 0;
	final int SETUP_DIRS = 1;
	final int SETUP_DISPLAY = 2;

	final int SETUP_UI = 3;
	CRPreferences prefs;

	boolean glchanged;
	// Display Panel
	JButton displayBtn;

	DisplayConfig dpcfg;
	// grid
	Color grid_color;

	Color bg_color;

	public CRPreferencesDialog(final JFrame parent) {
		super(parent);

		// init data
		this.prefs = new CRPreferences();
		glchanged = false;

		$$$setupUI$$$();

		setTitle("Preferences");
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
			public void windowClosing(final WindowEvent e) {
				onCancel();
			}
		});

		contentPane.registerKeyboardAction(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				onCancel();
			}
		}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

		helpButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				onHelp();
			}
		});

		imgBtn.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				onChooseDir(field_imgblock);
			}
		});

		downBtn.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				onChooseDir(field_download);
			}
		});
		
		tmpBtn.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				onChooseDir(field_tmpdir);
			}
		});

		check_gridEnabled.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				onCheckGrid();
			}
		});

		grid_colorbtn.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				onGridColor();
			}
		});

		canvasBackgroundColorButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				onBgColor();
			}
		});

		autoZoomCheckBox.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				prefs.setAutoZoom(autoZoomCheckBox.isSelected());
			}
		});

		horiDepthRadioButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				switchDepthOrientation();
			}
		});

		vertDepthRadioButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				switchDepthOrientation();
			}
		});

		showOriginAxisCheckbox.addChangeListener(new ChangeListener() {
			public void stateChanged(final ChangeEvent event) {
				JCheckBox cb = (JCheckBox) event.getSource();
				boolean b = cb.isSelected();

				CorelyzerApp app = CorelyzerApp.getApp();
				if (app != null) {
					SceneGraph.setShowOrigin(b);
					app.updateGLWindows();
				}
			}
		});

		showCoreSectionLabelCheckbox.addChangeListener(new ChangeListener() {
			public void stateChanged(final ChangeEvent event) {
				JCheckBox cb = (JCheckBox) event.getSource();
				boolean b = cb.isSelected();

				CorelyzerApp app = CorelyzerApp.getApp();
				if (app != null) {
					SceneGraph.setShowSectionText(b);
					app.updateGLWindows();
				}
			}
		});

		disPrefixSelectButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				onSelectDISPrefix();
			}
		});

		dictDefFileSelectButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				onSelectDictDefinitionFile();
			}
		});
	}

	public CRPreferencesDialog(final JFrame f, final CRPreferences p) {
		this(f);

		this.prefs = new CRPreferences();
		glchanged = false;
		setPreferences(p);
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
		panel2.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
		panel1.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		buttonCancel = new JButton();
		buttonCancel.setText("Cancel");
		panel2.add(buttonCancel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		buttonOK = new JButton();
		buttonOK.setText("OK");
		panel2.add(buttonOK, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		helpButton = new JButton();
		helpButton.setText("Help");
		panel2.add(helpButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JPanel panel3 = new JPanel();
		panel3.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
		contentPane
				.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
						| GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
						0, false));
		stageTab = new JTabbedPane();
		panel3.add(stageTab, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200,
				200), null, 0, false));
		
		// Main Directories tab panel (I <3 MigLayout, especially compared to the surrounding GridLayout chaos).
		final JPanel dirPanel = new JPanel();
		dirPanel.setLayout(new MigLayout());
		dirPanel.setBorder(BorderFactory.createTitledBorder(""));
		
		final JLabel imgLabel = new JLabel("Image Cache: ");
		imgBtn = new JButton("Select...");
		field_imgblock = new JTextField();
		field_imgblock.setEnabled(false);
		
		final JLabel downLabel = new JLabel("Downloads: ");
		downBtn = new JButton("Select...");
		field_download = new JTextField();
		field_download.setEnabled(false);
		
		final JLabel tmpLabel = new JLabel("Temporary Files: ");
		tmpBtn = new JButton("Select...");
		field_tmpdir = new JTextField();
		field_tmpdir.setEnabled(false);
		
		dirPanel.add(imgLabel);
		dirPanel.add(imgBtn, "wrap");
		dirPanel.add(field_imgblock, "span, growx, wrap");
		
		dirPanel.add(downLabel, "gaptop 20px");
		dirPanel.add(downBtn, "wrap");
		dirPanel.add(field_download, "span, growx, wrap");

		dirPanel.add(tmpLabel, "gaptop 20px");
		dirPanel.add(tmpBtn, "wrap");
		dirPanel.add(field_tmpdir, "span, growx, wrap");

	
		// Display tab
		displayPanel.setBorder(BorderFactory.createTitledBorder(""));
		final JPanel panel7 = new JPanel();
		panel7.setLayout(new GridLayoutManager(12, 1, new Insets(0, 0, 0, 0), -1, -1));

		panel7.setBorder(BorderFactory.createTitledBorder(""));
		lockCoreSectionImage = new JCheckBox();
		lockCoreSectionImage.setText("Lock depth of core section images after loading");
		lockCoreSectionImage.setToolTipText("Cores loaded while this option is enabled cannot be moved up or down stratigraphically");
		panel7.add(lockCoreSectionImage, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		
		JLabel tooltipInfoText = new JLabel("Mouse over text for descriptions.");
		tooltipInfoText.setFont( tooltipInfoText.getFont().deriveFont( 11.0f ));
		panel7.add( tooltipInfoText, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		
		autoCheckVersion = new JCheckBox();
		autoCheckVersion.setSelected(true);
		autoCheckVersion.setText("Automatically check for updates on startup");
		panel7.add(autoCheckVersion, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JPanel panel8 = new JPanel();
		panel8.setLayout(new GridLayoutManager(4, 3, new Insets(0, 0, 0, 0), -1, -1));
		panel7.add(panel8, new GridConstraints(11, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		panel8.setBorder(BorderFactory.createTitledBorder("Canvas Grid"));
		check_gridEnabled = new JCheckBox();
		check_gridEnabled.setText("Show Grid");
		panel8.add(check_gridEnabled, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final Spacer spacer5 = new Spacer();
		panel8.add(spacer5, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
		final JLabel label3 = new JLabel();
		label3.setText("Grid Type: ");
		panel8.add(label3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JLabel label4 = new JLabel();
		label4.setText("Grid Space: ");
		panel8.add(label4, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		field_gridType = new JComboBox<String>();
		final DefaultComboBoxModel<String> defaultComboBoxModel1 = new DefaultComboBoxModel<String>();
		defaultComboBoxModel1.addElement("Basic Cross");
		defaultComboBoxModel1.addElement("Horizontal Lines");
		defaultComboBoxModel1.addElement("Vertical Lines");
		defaultComboBoxModel1.addElement("Points");
		defaultComboBoxModel1.addElement("Cross Points");
		field_gridType.setModel(defaultComboBoxModel1);
		panel8.add(field_gridType, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		field_gridSpace = new JFormattedTextField();
		field_gridSpace.setHorizontalAlignment(11);
		field_gridSpace.setText("10.0");
		panel8.add(field_gridSpace, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		final JLabel label5 = new JLabel();
		label5.setText("cm");
		panel8.add(label5, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JLabel label6 = new JLabel();
		label6.setText("Thickness: ");
		panel8.add(label6, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		field_gridThickness = new JFormattedTextField();
		field_gridThickness.setHorizontalAlignment(11);
		field_gridThickness.setText("1");
		panel8.add(field_gridThickness, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		grid_colorbtn = new JButton();
		grid_colorbtn.setText("Color");
		panel8.add(grid_colorbtn, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JPanel panel9 = new JPanel();
		panel9.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
		panel7.add(panel9, new GridConstraints(10, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		canvasBackgroundColorButton = new JButton();
		canvasBackgroundColorButton.setText("Canvas background color");
		panel9.add(canvasBackgroundColorButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(98, 29),
				null, 0, false));
		final Spacer spacer6 = new Spacer();
		panel9.add(spacer6, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
		final Spacer spacer7 = new Spacer();
		panel9.add(spacer7, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
				GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(98, 14), null, 0, false));
		autoZoomCheckBox = new JCheckBox();
		autoZoomCheckBox.setSelected(true);
		autoZoomCheckBox.setText("Double-clicking section name zooms to that section");
		autoZoomCheckBox.setToolTipText("Double-clicking section name (in the session window) shows the entire section at the " +
					"highest zoom level that can accommodate it.");
		panel7.add(autoZoomCheckBox, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JPanel panel10 = new JPanel();
		panel10.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));

		panel7.add(panel10, new GridConstraints(9, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		horiDepthRadioButton = new JRadioButton();
		horiDepthRadioButton.setSelected(true);
		horiDepthRadioButton.setText("Horizontal Depth Mode");
		panel10.add(horiDepthRadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final Spacer spacer8 = new Spacer();
		panel10.add(spacer8, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
		vertDepthRadioButton = new JRadioButton();
		vertDepthRadioButton.setText("Vertical Depth Mode");
		panel10.add(vertDepthRadioButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		showOriginAxisCheckbox = new JCheckBox();
		showOriginAxisCheckbox.setSelected(true);
		showOriginAxisCheckbox.setText("Show crosshair at origin");
		panel7.add(showOriginAxisCheckbox, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		showCoreSectionLabelCheckbox = new JCheckBox();
		showCoreSectionLabelCheckbox.setSelected(true);
		showCoreSectionLabelCheckbox.setText("Show labels for sections with data but no image");
		showCoreSectionLabelCheckbox.setToolTipText("Show section labels for dummy sections (those with plotted data but no image)");
		panel7.add(showCoreSectionLabelCheckbox, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		canvasAlwaysAtBelowCheckBox = new JCheckBox();
		canvasAlwaysAtBelowCheckBox.setSelected(true);
		canvasAlwaysAtBelowCheckBox.setText("Canvas always draws below external application windows");
		canvasAlwaysAtBelowCheckBox.setToolTipText("Windows of non-Corelyzer applications will draw on top of the visualization canvas. " +
				"Disabling this option is recommended.");
		panel7.add(canvasAlwaysAtBelowCheckBox, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		depthScrollCheckbox = new JCheckBox();
		depthScrollCheckbox.setText("Scroll wheel/gesture zooms in Vertical Depth Mode");
		depthScrollCheckbox.setToolTipText("In Vertical Depth Mode, scroll wheel or trackpad gesture will zoom when this option is enabled. " +
				"If disabled, it will pan along the depth axis.");
		panel7.add(depthScrollCheckbox, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JPanel panel11 = new JPanel();
		panel11.setLayout(new GridLayoutManager(2, 4, new Insets(0, 0, 0, 0), -1, -1));
		final JLabel label7 = new JLabel();
		label7.setText("Sharing Server: ");
		panel11.add(label7, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final Spacer spacer9 = new Spacer();
		panel11.add(spacer9, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
				GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
		serverAddressTextField = new JTextField();
		serverAddressTextField.setHorizontalAlignment(11);
		serverAddressTextField.setText("127.0.0.1");
		panel11.add(serverAddressTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(145, 28), null, 0, false));
		final JLabel label8 = new JLabel();
		label8.setText("Port: ");
		panel11.add(label8, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(37, 16), null, 0, false));
		serverPortTextField = new JTextField();
		serverPortTextField.setHorizontalAlignment(11);
		serverPortTextField.setText("16688");
		panel11.add(serverPortTextField, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(79, 28), null, 0, false));
		final JPanel panel12 = new JPanel();
		panel12.setLayout(new GridLayoutManager(3, 3, new Insets(0, 0, 0, 0), -1, -1));
		final JLabel label9 = new JLabel();
		label9.setText("Local path prefix: ");
		panel12.add(label9, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
		GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final Spacer spacer10 = new Spacer();
		panel12.add(spacer10, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
		GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
		disPrefixTextField = new JTextField();
		disPrefixTextField.setEditable(false);
		disPrefixTextField.setHorizontalAlignment(11);
		disPrefixTextField.setText("");
		panel12.add(disPrefixTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
		GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		disPrefixSelectButton = new JButton();
		disPrefixSelectButton.setText("Select...");
		panel12.add(disPrefixSelectButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
		GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JLabel label10 = new JLabel();
		label10.setText("Dictionary definition: ");
		panel12.add(label10, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
		GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		dictDefFileTextField = new JTextField();
		dictDefFileTextField.setEditable(false);
		dictDefFileTextField.setHorizontalAlignment(11);
		panel12.add(dictDefFileTextField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
		GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		dictDefFileSelectButton = new JButton();
		dictDefFileSelectButton.setText("Select...");
		panel12.add(dictDefFileSelectButton, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
		GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		ButtonGroup buttonGroup;
		buttonGroup = new ButtonGroup();
		buttonGroup.add(horiDepthRadioButton);
		buttonGroup.add(vertDepthRadioButton);

		stageTab.addTab("User Interface", panel7);
		stageTab.addTab("Displays", displayPanel);
		stageTab.addTab("Directories", dirPanel);		
		stageTab.addTab("Session Sharing", panel11);
		stageTab.addTab("DIS", panel12);
	}

	

	private JPanel create_display_panel() {
		JPanel p = new JPanel(new BorderLayout());
		p.setName("Display");

		dpcfg = new DisplayConfig();
		this.dpcfg.setPreferences(prefs);
		p.add(dpcfg.dlg.getContentPane(), BorderLayout.CENTER);

		return p;
	}

	private void createUIComponents() {
		// color chooser
		grid_color = new Color(200, 200, 200);

		// create display panel
		this.displayPanel = this.create_display_panel();
	}

	public CRPreferences getPreferences() {
		return prefs;
	}

	private void onBgColor() {
		final ColorChooser chooser = new ColorChooser(this);
		chooser.setVisible(true);
		chooser.setTarget(this.canvasBackgroundColorButton);
		chooser.setColor(this.bg_color);
		chooser.addReturnActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				bg_color = chooser.getColor();

				CorelyzerApp app = CorelyzerApp.getApp();
				if (app != null) {
					float r = bg_color.getRed() / 255.0f;
					float g = bg_color.getGreen() / 255.0f;
					float b = bg_color.getBlue() / 255.0f;

					SceneGraph.setBackgroundColor(r, g, b);

					app.updateGLWindows();
				}
			}
		});

		Point loc = this.getLocation();
		Dimension dim = this.getSize();
		chooser.setLocation(loc.x + dim.width + 10, loc.y);
	}

	private void onCancel() {
		this.prefs = null;
		this.setVisible(false);

		dispose();
	}

	private void onCheckGrid() {
		if (check_gridEnabled.isSelected()) {
			field_gridType.setEnabled(true);
			field_gridSpace.setEnabled(true);
			field_gridThickness.setEnabled(true);
			grid_colorbtn.setEnabled(true);
		} else {
			field_gridType.setEnabled(false);
			field_gridSpace.setEnabled(false);
			field_gridThickness.setEnabled(false);
			grid_colorbtn.setEnabled(false);
		}
	}

	private void onGridColor() {
		final ColorChooser chooser = new ColorChooser(this);
		chooser.setVisible(true);
		chooser.setTarget(this.grid_colorbtn);
		chooser.setColor(this.bg_color);
		chooser.addReturnActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				grid_color = chooser.getColor();
			}
		});

		Point loc = this.getLocation();
		Dimension dim = this.getSize();
		chooser.setLocation(loc.x + dim.width + 10, loc.y);
	}

	private void onHelp() {
		String url = null;
		try {
			url = "http://csdco.umn.edu/resources/software/corelyzer";
			URI uri = new URI(url);
			java.awt.Desktop.getDesktop().browse(uri);
		} catch (IOException ex) {
			System.err.println("IOException trying to browse to " + url + " from About Dialog");
		} catch (URISyntaxException urie) {
			System.err.println("URI Syntax Exception parsing " + url + ":" + urie.getMessage());
		}
	}

	private File chooseDirectory(File currentDir) {
		File chosenDir = null;
		JFileChooser fc = new JFileChooser(currentDir);
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			chosenDir = fc.getSelectedFile();
		}
		return chosenDir;
	}
	
	// called on Directory tab "Browse..." buttons: set field text when user selects new path
	private void onChooseDir(JTextField pathField) {
		final String path = pathField.getText();
		File f = new File(path);
		File newDir = chooseDirectory(f);
		if (newDir != null) {
			String abspath = newDir.getAbsolutePath();
			if (abspath.equals(path)) {
				glchanged = true;
			}
			pathField.setText(abspath);
		}
	}

	private void onOK() {
		this.saveAndApplySetups();
		this.setVisible(false);
	}

	private void onSelectDictDefinitionFile() {
		String f = FileUtility.selectASingleFile(this, "Select the annotation definition file", null, FileUtility.LOAD);

		if (f != null) {
			this.dictDefFileTextField.setText(f);
		}
	}

	private void onSelectDISPrefix() {
		File f = new File(this.disPrefixTextField.getText());

		if (!f.exists()) {
			f = new File(System.getProperty("user.home"));
		}

		if ((f = JDirectoryChooser.showDialog(this, f)) != null) {
			String abspath = f.getAbsolutePath();

			this.disPrefixTextField.setText(abspath);
		}
	}

	public void positionCentricTo(final JFrame parent) {
		Point pLoc;
		Dimension pDim;
		if (parent != null) {
			pLoc = parent.getLocation();
			pDim = parent.getSize();

		} else {
			pLoc = new Point(0, 0);
			pDim = Toolkit.getDefaultToolkit().getScreenSize();
		}

		Dimension mDim = this.getSize();
		this.setLocation(pLoc.x + pDim.width / 2 - mDim.width / 2, pLoc.y + pDim.height / 2 - mDim.height / 2);
	}
	
	private String makeDirectory(String path) {
		String newPath = null;
		final String sp = System.getProperty("file.separator");
		if (!path.equals("")) {
			newPath = path;
			if (!newPath.endsWith(sp))
				newPath += sp;
			
			File dir = new File(newPath);
			if (!dir.exists())
				dir.mkdir();
		}
		return newPath;
	}

	private void saveAndApplySetups() {
		this.prefs.isInited = true;
		// collect info in the UIs
		// Dirs
		this.prefs.texBlock_Directory = makeDirectory(field_imgblock.getText());
		this.prefs.download_Directory = makeDirectory(field_download.getText());
		this.prefs.tmp_Directory = makeDirectory(field_tmpdir.getText());

		// Display
		if (dpcfg.getPreferences(prefs)) {
			glchanged = true;
		}

		// UIs
		this.prefs.lockCoreSectionImage = lockCoreSectionImage.isSelected();
		this.prefs.setAutoCheckVersion(this.autoCheckVersion.isSelected());

		// Grids
		this.prefs.grid_show = check_gridEnabled.isSelected();
		this.prefs.grid_type = field_gridType.getSelectedIndex() < 0 ? 0 : field_gridType.getSelectedIndex();
		this.prefs.grid_size = Float.parseFloat(field_gridSpace.getValue().toString());
		this.prefs.grid_thickness = Integer.parseInt(field_gridThickness.getValue().toString());
		this.prefs.grid_r = grid_color.getRed() / 255.0f;
		this.prefs.grid_g = grid_color.getGreen() / 255.0f;
		this.prefs.grid_b = grid_color.getBlue() / 255.0f;

		this.prefs.save();

		CorelyzerApp app = CorelyzerApp.getApp();
		SceneGraph.setTexBlockDirectory(this.prefs.texBlock_Directory);

		if (app == null) {
			System.out.println("CRPreferencesSave: Null app pointer!");
			return;
		}

		// app.preferences = null;
		app.preferences = this.prefs;

		// could add runtime restart GL stuff here
		if (!app.isGLInited) {
			app.destroyGLWindows();
			app.setPreferences(prefs);
			app.createGLWindows();
			app.getMainFrame().setVisible(true);
			app.getToolFrame().setVisible(true);
		} else {
			if (glchanged) {
				Object[] options = { "Cancel", "Restart" };

				String mesg = "You might need to restart Corelyzer to make " + "display configuration take effect.";
				int n = JOptionPane.showOptionDialog(this, mesg, "Restart Corelyzer?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options,
						options[1]);

				if (n == 1) {
					System.out.println("---> [INFO] Restart from Preferences.");
					app.quit();
					return;
				}
			} else {
				System.out.println("---> Should be fine to keep running");
				// gl stuff not changed. do nothing here
			}
		}

		// Grid stuff should be after any possible gl restart
		SceneGraph.lock();
		if (check_gridEnabled.isSelected()) {
			SceneGraph.enableCanvasGrid(true);
			SceneGraph.setCanvasGridColor(this.prefs.grid_r, this.prefs.grid_g, this.prefs.grid_b);
			SceneGraph.setCanvasGridSize(this.prefs.grid_size);
			SceneGraph.setCanvasGridThickness(this.prefs.grid_thickness);
			SceneGraph.setCanvasGridType(this.prefs.grid_type);
		} else {
			SceneGraph.enableCanvasGrid(false);
		}
		SceneGraph.unlock();
		app.updateGLWindows();

		// sharing server address and port number
		prefs.setProperty("sessionSharing.serverAddress", this.serverAddressTextField.getText());
		prefs.setProperty("sessionSharing.serverPort", this.serverPortTextField.getText());

		// show origin coord and section label
		prefs.setProperty("ui.showOrigin", String.valueOf(this.showOriginAxisCheckbox.isSelected()));
		prefs.setProperty("ui.showSectionLabel", String.valueOf(this.showCoreSectionLabelCheckbox.isSelected()));

		// if the canvas always at the bottom
		prefs.setProperty("ui.canvas.alwaysBelow", String.valueOf(this.canvasAlwaysAtBelowCheckBox.isSelected()));

		prefs.setProperty("ui.verticalDepthScroll", String.valueOf(this.depthScrollCheckbox.isSelected()));

		prefs.setProperty("dis.dictDefinitionFile", this.dictDefFileTextField.getText());
		prefs.setProperty("dis.prefix", this.disPrefixTextField.getText());

		// If only UI config is modified, then the program can keep going
		// if Directory & DisplayConfig modified, restart will be needed
	}

	public void setPreferences(final CRPreferences p) {
		if (p == null) {
			return;
		}

		// Notice! We are making a duplication here!
		// So the preferences will not effect current preferences in the
		// main CorelyzerApp
		prefs = new CRPreferences(p);
		// prefs = p;

		// Update Description Panel
		File f_cwd = new File(".");
		String cwd = "";

		try {
			cwd = f_cwd.getCanonicalPath();
		} catch (IOException e) {
			System.err.println("This should not happen.");
		}

		// Update Directory Panel
		this.field_imgblock.setText(prefs.texBlock_Directory);
		this.field_download.setText(prefs.download_Directory);
		this.field_tmpdir.setText(prefs.tmp_Directory);

		// Update Display Panel
		dpcfg.setPreferences(prefs);

		// Update UI Panel
		this.lockCoreSectionImage.setSelected(prefs.lockCoreSectionImage);
		this.autoCheckVersion.setSelected(prefs.getAutoCheckVersion());

		// Update Grid
		this.check_gridEnabled.setSelected(prefs.grid_show);
		this.field_gridType.setSelectedIndex(prefs.grid_type);
		this.field_gridSpace.setValue(prefs.grid_size);
		this.field_gridThickness.setValue(prefs.grid_thickness);
		this.grid_color = new Color((int) (prefs.grid_r * 255.0f), (int) (prefs.grid_g * 255.0f), (int) (prefs.grid_b * 255.0f));
		this.grid_colorbtn.setBackground(this.grid_color);

		if (prefs.grid_show) {
			this.field_gridType.setEnabled(true);
			this.field_gridSpace.setEnabled(true);
			this.field_gridThickness.setEnabled(true);
			this.grid_colorbtn.setEnabled(true);
		}

		// autoZoom
		this.autoZoomCheckBox.setSelected(prefs.isAutoZoom());

		// depth orientation
		boolean depthOrientation = SceneGraph.getDepthOrientation();

		if (depthOrientation) {
			this.horiDepthRadioButton.setSelected(true);
		} else {
			this.vertDepthRadioButton.setSelected(true);
		}

		// sharing server address and port number
		String srvAddress, srvPort;

		if (prefs.getProperty("sessionSharing.serverAddress") == null || prefs.getProperty("sessionSharing.serverAddress").equals("")) {
			srvAddress = "127.0.0.1";
		} else {
			srvAddress = prefs.getProperty("sessionSharing.serverAddress");
		}

		if (prefs.getProperty("sessionSharing.serverPort") == null || prefs.getProperty("sessionSharing.serverPort").equals("")) {
			srvPort = "16688";
		} else {
			srvPort = prefs.getProperty("sessionSharing.serverPort");
		}

		this.serverAddressTextField.setText(srvAddress);
		this.serverPortTextField.setText(srvPort);

		boolean showOrigin = Boolean.parseBoolean(prefs.getProperty("ui.showOrigin"));
		boolean showSectionLabel = Boolean.parseBoolean(prefs.getProperty("ui.showSectionLabel"));

		this.showOriginAxisCheckbox.setSelected(showOrigin);
		this.showCoreSectionLabelCheckbox.setSelected(showSectionLabel);

		boolean isCanvasAlwaysAtBottom = Boolean.parseBoolean(prefs.getProperty("ui.canvas.alwaysBelow"));
		this.canvasAlwaysAtBelowCheckBox.setSelected(isCanvasAlwaysAtBottom);

		boolean isVerticalDepthScroll = Boolean.parseBoolean(prefs.getProperty("ui.verticalDepthScroll"));
		this.depthScrollCheckbox.setSelected(isVerticalDepthScroll);

		String dictDefFile = prefs.getProperty("dis.dictDefinitionFile");
		if (dictDefFile == null || dictDefFile.equals("")) {
			String sp = System.getProperty("file.separator");
			dictDefFile = cwd + sp + "resources" + sp + "annotations" + sp + "defaultDictionary.plist";
		}
		this.dictDefFileTextField.setText(dictDefFile);

		String prefDISPrefix = prefs.getProperty("dis.prefix");
		if (prefDISPrefix == null || prefDISPrefix.equals("")) {
			prefDISPrefix = System.getProperty("user.home");
		}
		this.disPrefixTextField.setText(prefDISPrefix);
	}

	public void start(final int step, final CRPreferences p) {
		this.setPreferences(p);
		stageTab.setSelectedIndex(step);
	}

	public void stateChanged(final ChangeEvent e) {
	}

	private void switchDepthOrientation() {
		boolean isHorizontal = horiDepthRadioButton.isSelected();

		CorelyzerApp app = CorelyzerApp.getApp();
		if (app != null) {
			SceneGraph.setDepthOrientation(isHorizontal);
			app.updateGLWindows();
		}
	}

	public void windowActivated(final WindowEvent windowEvent) {
	}

	public void windowClosed(final WindowEvent e) {
	}

	/**
	 * Sends the results to the CorelyzerApp object and saves the settings for
	 * the future.
	 */
	public void windowClosing(final WindowEvent e) {
		System.out.println("CRPreferencesDialog Window Closed");
		this.saveAndApplySetups();
	}

	public void windowDeactivated(final WindowEvent windowEvent) {
	}

	public void windowDeiconified(final WindowEvent windowEvent) {
	}

	public void windowIconified(final WindowEvent windowEvent) {
	}

	public void windowOpened(final WindowEvent e) {
	}
}
