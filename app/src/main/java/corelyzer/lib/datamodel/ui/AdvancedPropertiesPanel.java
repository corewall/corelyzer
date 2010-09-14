package corelyzer.lib.datamodel.ui;

import javax.swing.JLabel;
import javax.swing.JTextField;

import org.jdesktop.layout.GroupLayout;
import org.jdesktop.layout.LayoutStyle;

import corelyzer.lib.datamodel.CoreImageConfiguration;

/**
 * A panel to collect our advanced panel.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class AdvancedPropertiesPanel extends CoreImageConfigurationPanel {
	private static final long serialVersionUID = 5890575462056642423L;

	// our components
	private JLabel baseDirLabel;
	private JTextField baseDirText;
	private JLabel baseURLLabel;
	private JTextField baseURLText;
	private JLabel trackLabel;
	private JTextField trackText;

	@Override
	protected void initComponents() {
		trackLabel = new JLabel();
		baseURLLabel = new JLabel();
		baseDirLabel = new JLabel();
		trackText = new JTextField();
		baseURLText = new JTextField();
		baseDirText = new JTextField();

		trackLabel.setText("Track Name:");

		baseURLLabel.setText("Base URL:");

		baseDirLabel.setText("Base Directory:");
	}

	@Override
	protected void layoutComponents() {
		GroupLayout layout = new GroupLayout(this);
		setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.LEADING).add(
				layout.createSequentialGroup()
						.add(20, 20, 20)
						.add(layout.createParallelGroup(GroupLayout.TRAILING).add(baseURLLabel).add(trackLabel).add(baseDirLabel))
						.addPreferredGap(LayoutStyle.RELATED)
						.add(layout.createParallelGroup(GroupLayout.LEADING).add(baseURLText, GroupLayout.DEFAULT_SIZE, 255, Short.MAX_VALUE)
								.add(trackText, GroupLayout.DEFAULT_SIZE, 255, Short.MAX_VALUE)
								.add(baseDirText, GroupLayout.DEFAULT_SIZE, 255, Short.MAX_VALUE)).addContainerGap()));
		layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.LEADING).add(
				layout.createSequentialGroup()
						.addContainerGap()
						.add(layout.createParallelGroup(GroupLayout.BASELINE).add(trackLabel)
								.add(trackText, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(LayoutStyle.RELATED)
						.add(layout.createParallelGroup(GroupLayout.BASELINE).add(baseURLLabel)
								.add(baseURLText, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(LayoutStyle.RELATED)
						.add(layout.createParallelGroup(GroupLayout.BASELINE).add(baseDirLabel)
								.add(baseDirText, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
						.addContainerGap(48, Short.MAX_VALUE)));
	}

	@Override
	protected void saveComponenets() {
		if (directory == null) {
			return;
		}

		// save our advanced properties
		CoreImageConfiguration config = directory.getConfig();
		config.setTrack(trackText.getText());
		config.setBaseDir(baseDirText.getText());
		config.setBaseURL(baseURLText.getText());
	}

	@Override
	protected void updateComponents() {
		if (directory == null) {
			return;
		}

		// update fields from our config
		CoreImageConfiguration config = directory.getConfig();

		if (config.get(CoreImageConfiguration.KEY_TRACK) != null) {
			trackText.setText(config.getTrack());
		} else {
			trackText.setText("");
		}

		if (config.get(CoreImageConfiguration.KEY_BASE_URL) != null) {
			baseURLText.setText(config.getBaseURL());
		} else {
			baseURLText.setText("");
		}

		if (config.get(CoreImageConfiguration.KEY_BASE_DIR) != null) {
			baseDirText.setText(config.getBaseDir());
		} else {
			baseDirText.setText("");
		}
	}
}
