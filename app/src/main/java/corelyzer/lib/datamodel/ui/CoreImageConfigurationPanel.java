package corelyzer.lib.datamodel.ui;

import javax.swing.JPanel;

import corelyzer.lib.datamodel.CoreImageDirectory;

/**
 * An abstract class for panels that contribute to a CoreImageConfiguration.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public abstract class CoreImageConfigurationPanel extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3868551357307778993L;
	protected CoreImageDirectory directory = null;

	/**
	 * Create a new CoreImageConfigurationPanel.
	 */
	public CoreImageConfigurationPanel() {
		initComponents();
		layoutComponents();
	}

	/**
	 * Initialize the components...
	 */
	protected abstract void initComponents();

	/**
	 * Layout the components...
	 */
	protected abstract void layoutComponents();

	/**
	 * Save the component values to the CoreImageConfiguration.
	 */
	protected abstract void saveComponenets();

	/**
	 * Sets the core image directory.
	 * 
	 * @param directory
	 *            the core image directory.
	 */
	public void setDirectory(final CoreImageDirectory directory) {
		this.directory = directory;
		updateComponents();
	}

	/**
	 * Update the component values from the CoreImageConfiguration.
	 */
	protected abstract void updateComponents();
}
