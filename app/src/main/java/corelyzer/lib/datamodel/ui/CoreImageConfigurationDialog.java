package corelyzer.lib.datamodel.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.jdesktop.layout.GroupLayout;
import org.jdesktop.layout.LayoutStyle;

import corelyzer.lib.datamodel.CoreImageDirectory;

/**
 * A dialog for configuring a directory for the directory viewer.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class CoreImageConfigurationDialog extends JDialog {
	private static final long serialVersionUID = -8324218076140608828L;

	private static File currentDirectory = null;

	// our fields
	private CoreImageDirectory directory;

	// our components
	private JButton browseButton;
	private JButton cancelButton;
	private JLabel directoryLabel;
	private JTextField directoryText;
	private JButton okButton;
	private JTabbedPane tabbedPane;

	// our panels
	private DepthPatternPanel depthPatternPanel;
	private ImagePropertiesPanel imagePanel;
	private AdvancedPropertiesPanel advancedPanel;

	/**
	 * Create a new CoreImageConfigurationDialog.
	 * 
	 * @param parent
	 *            the parent.
	 */
	public CoreImageConfigurationDialog(final JFrame parent) {
		super(parent, true);
		initComponents();
	}

	/**
	 * Create a new CoreImageConfigurationDialog.
	 * 
	 * @param parent
	 *            the parent.
	 * @param directory
	 *            the directory.
	 */
	public CoreImageConfigurationDialog(final JFrame parent, final File directory) {
		super(parent, true);
		initComponents();
		directoryText.setText(directory.getAbsolutePath());
	}

	/**
	 * Get the configured core image directory or null if the dialog was
	 * canceled.
	 * 
	 * @return the core image directory or null.
	 */
	public CoreImageDirectory getCoreImageDirectory() {
		return directory;
	}

	private void initComponents() {
		// create our depth pattern panel
		depthPatternPanel = new DepthPatternPanel();

		// create our image properties panel
		imagePanel = new ImagePropertiesPanel();

		// create our advanced properties panel
		advancedPanel = new AdvancedPropertiesPanel();

		// create our components
		directoryLabel = new JLabel();
		directoryText = new JTextField();
		browseButton = new JButton();
		tabbedPane = new JTabbedPane();
		okButton = new JButton();
		cancelButton = new JButton();

		// add our tabs
		tabbedPane.addTab("Depth", depthPatternPanel);
		tabbedPane.addTab("Image", imagePanel);
		tabbedPane.addTab("Advanced", advancedPanel);

		// configure our dialog
		setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		setTitle("Configure Directory");
		setModal(true);
		setResizable(false);

		// set up our directory label
		directoryLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		directoryLabel.setText("Directory:");

		// listen for changes to the directory text field
		directoryText.getDocument().addDocumentListener(new DocumentListener() {

			public void changedUpdate(final DocumentEvent e) {
				onDirectoryTextChange();
			}

			public void insertUpdate(final DocumentEvent e) {
				onDirectoryTextChange();
			}

			public void removeUpdate(final DocumentEvent e) {
				onDirectoryTextChange();
			}
		});

		// create a browse button
		browseButton.setText("Browse...");
		browseButton.addActionListener(new ActionListener() {

			public void actionPerformed(final ActionEvent evt) {
				onBrowse(evt);
			}
		});

		// create an OK button
		okButton.setText("OK");
		okButton.addActionListener(new ActionListener() {

			public void actionPerformed(final ActionEvent evt) {
				onOK(evt);
			}
		});
		okButton.setEnabled(false);

		// create a cancel button
		cancelButton.setText("Cancel");
		cancelButton.addActionListener(new ActionListener() {

			public void actionPerformed(final ActionEvent evt) {
				onCancel(evt);
			}
		});

		// layout our dialog
		GroupLayout layout = new GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(layout
				.createParallelGroup(GroupLayout.LEADING)
				.add(layout.createSequentialGroup().add(directoryLabel, GroupLayout.PREFERRED_SIZE, 72, GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(LayoutStyle.RELATED).add(directoryText, GroupLayout.DEFAULT_SIZE, 245, Short.MAX_VALUE)
						.addPreferredGap(LayoutStyle.RELATED).add(browseButton))
				.add(GroupLayout.TRAILING,
						layout.createSequentialGroup().addContainerGap(222, Short.MAX_VALUE).add(okButton).addPreferredGap(LayoutStyle.RELATED)
								.add(cancelButton).add(42, 42, 42)).add(tabbedPane, GroupLayout.DEFAULT_SIZE, 420, Short.MAX_VALUE));
		layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.LEADING).add(
				layout.createSequentialGroup()
						.add(layout.createParallelGroup(GroupLayout.BASELINE).add(browseButton).add(directoryLabel)
								.add(directoryText, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(LayoutStyle.RELATED).add(tabbedPane, GroupLayout.PREFERRED_SIZE, 190, GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(LayoutStyle.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.add(layout.createParallelGroup(GroupLayout.BASELINE).add(okButton).add(cancelButton))));
		pack();
	}

	// show our file chooser
	private void onBrowse(final ActionEvent evt) {
		// create our file chooser
		JFileChooser directoryChooser;
		if (currentDirectory == null) {
			directoryChooser = new JFileChooser();
		} else {
			directoryChooser = new JFileChooser(currentDirectory);
		}
		directoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		if (directoryChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			directoryText.setText(directoryChooser.getSelectedFile().getAbsolutePath());
			currentDirectory = directory.getDirectory().getParentFile();
		}
	}

	// null our fields
	private void onCancel(final ActionEvent evt) {
		directory = null;
		dispose();
	}

	private void onDirectoryTextChange() {
		// figure out if we got a directory
		File d = new File(directoryText.getText());
		if (d.exists() && d.isDirectory()) {
			directory = new CoreImageDirectory(new File(directoryText.getText()));
		} else {
			directory = null;
		}

		// update our panels
		depthPatternPanel.setDirectory(directory);
		imagePanel.setDirectory(directory);
		advancedPanel.setDirectory(directory);
		okButton.setEnabled(directory != null);
	}

	// tell our panels to save themselves and then dispose
	private void onOK(final ActionEvent evt) {
		if (directory != null) {
			depthPatternPanel.saveComponenets();
			imagePanel.saveComponenets();
			advancedPanel.saveComponenets();
			directory.saveConfig();
		}
		dispose();
	}
}
