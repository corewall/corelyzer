package corelyzer.plugin;

import java.awt.Component;
import java.awt.Frame;
import java.io.File;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedList;
import java.util.Vector;
import java.util.jar.Attributes;

import javax.swing.JPopupMenu;
import javax.swing.WindowConstants;

import corelyzer.ui.CorelyzerApp;

/**
 * This class helps to manage plugins. Internally handles to plugin objects,
 * class names and menu item labels are held here.
 */
public class CorelyzerPluginManager {

	// --------------------------------------------------------------
	// private LinkedList incomingPluginEvents;
	private Vector<CorelyzerPlugin> pluginObject;
	private Vector<String> pluginClassNames;
	private Vector<String> pluginMenuNames;
	private final Vector<LinkedList> eventRegistry;
	private final String[] startupArgs;

	// --------------------------------------------------------------
	/**
	 * Requires and array of class names that are plugins, which need to be
	 * loaded up.
	 */
	public CorelyzerPluginManager(final String[] pluginNames) {

		// create the event registry system
		eventRegistry = new Vector<LinkedList>();
		for (int i = 0; i < CorelyzerPluginEvent.NUMBER_OF_EVENTS; i++) {
			eventRegistry.add(new LinkedList());
		}

		startupArgs = pluginNames;
	}

	// --------------------------------------------------------------
	private void acquirePluginMenuNames() {
		System.out.println("---> [DEBUG] pluginClassNames size: " + pluginClassNames.size());

		for (int k = 0; k < pluginObject.size(); k++) {
			// for(int k=0; k<pluginMenuNames.size(); ++k) {
			CorelyzerPlugin p = pluginObject.elementAt(k);
			if (p.getMenuName().equals("Unknown Plugin")) {
				System.out.println("---> [DEBUG] Unknown Plugin atIdx: " + k);
				System.out.println("P is: " + p);

				pluginMenuNames.add(pluginClassNames.elementAt(k));
			} else {
				pluginMenuNames.add(p.getMenuName());
			}
		}

	}

	// --------------------------------------------------------------
	/**
	 * Let's a Plugin add to the popup menu. This call is made at every right
	 * click on a corelyzer.ui.CorelyzerGLCanvas. A plugin can query the
	 * corelyzer.helper.SceneGraph about what is being picked or not.
	 */
	public void addPluginPopupSubMenus(final JPopupMenu jpm) {

		for (int i = 0; i < pluginObject.size(); i++) {
			pluginObject.elementAt(i).addToPopupMenu(jpm);
		}

	}

	// --------------------------------------------------------------
	/**
	 * Relays a message to a single plugin, if it registered for the event of
	 * the given type.
	 */
	public void broadcastEventToPlugin(final int pluginId, final int type, final String desc) {
		if (type < 0 || type >= CorelyzerPluginEvent.NUMBER_OF_EVENTS) {
			return;
		}

		CorelyzerPlugin p;
		LinkedList l = eventRegistry.elementAt(type);
		Object[] v = l.toArray();

		for (int i = 0; i < v.length; i++) {
			// System.out.println("Calling Process Event");
			p = (CorelyzerPlugin) v[i];
			if (p.pluginId == pluginId) {
				p.processEvent(new CorelyzerPluginEvent(type, desc));
				return;
			}
		}
	}

	// --------------------------------------------------------------
	/**
	 * Blasts a given Corelyzer event to all plugins that have registered for
	 * the event of the given type
	 */
	public void broadcastEventToPlugins(final int type, final String desc) {
		// System.out.println("Broadcasting event of type " + type );
		if (type < 0 || type >= CorelyzerPluginEvent.NUMBER_OF_EVENTS) {
			return;
		}

		CorelyzerPluginEvent e = new CorelyzerPluginEvent(type, desc);
		LinkedList l = eventRegistry.elementAt(type);
		Object[] v = l.toArray();

		// System.out.println("# plugins registered for event: " + v.size());

		for (int i = 0; i < v.length; i++) {
			// System.out.println("Calling Process Event");
			CorelyzerPlugin p;
			p = (CorelyzerPlugin) v[i];
			p.processEvent(e);
		}
	}

	// --------------------------------------------------------------
	/** deiconify all the plugin GUIs. */
	public void deiconifyAllPlugins() {
		for (int k = 0; k < pluginObject.size(); k++) {
			CorelyzerPlugin p = pluginObject.elementAt(k);
			// check this frame is iconified, then set to normal and deiconified
			if (p.getFrame().getState() == Frame.ICONIFIED) {
				p.getFrame().setExtendedState(Frame.NORMAL);
				p.getFrame().setVisible(true);
			}
		}

	}

	// --------------------------------------------------------------
	/**
	 * Returns the handle to a plugin object, given a valid id. Otherwise it
	 * returns null.
	 */
	public CorelyzerPlugin getPlugin(final int pluginId) {
		// System.out.println("\t\tPLUGIN MANAGER getPlugin CALLED\n");
		if (pluginId < 0 || pluginId > pluginObject.size() - 1) {
			return null;
		}
		return pluginObject.elementAt(pluginId);
	}

	// --------------------------------------------------------------
	/** Hide all the plugin GUIs. */
	public void hideAllPlugins() {
		for (int k = 0; k < pluginObject.size(); k++) {
			CorelyzerPlugin p = pluginObject.elementAt(k);
			p.getFrame().setVisible(false);
		}
	}

	// --------------------------------------------------------------
	/** Iconify all the plugin GUIs. */
	public void iconifyAllPlugins() {
		for (int k = 0; k < pluginObject.size(); k++) {
			CorelyzerPlugin p = pluginObject.elementAt(k);
			// check this frame is visible, then iconified
			if (p.getFrame().isVisible()) {
				p.getFrame().setExtendedState(Frame.ICONIFIED);
				p.getFrame().setVisible(false);
			}
		}

	}

	private void loadPlugin(final String pluginName) {
		File file = new File("../plugins/" + pluginName + ".jar");
		if (!file.exists()) {
			file = new File("plugins/" + pluginName + ".jar");
		}

		if (file.exists()) {
			try {
				URL jarfile = new URL("jar", "", "file:" + file.getAbsolutePath() + "!/");

				JarURLConnection uc = (JarURLConnection) jarfile.openConnection();
				Attributes attr = uc.getMainAttributes();
				String mainClass = attr != null ? attr.getValue(Attributes.Name.MAIN_CLASS) : null;
				if (mainClass == null) {
					mainClass = file.getName().substring(0, file.getName().lastIndexOf('.'));
				}

				System.out.println("---> [INFO] Loading mainClass [" + mainClass + "] from plugin jar: " + jarfile);

				URLClassLoader cl = URLClassLoader.newInstance(new URL[] { jarfile });

				Class c = cl.loadClass(mainClass);

				CorelyzerPlugin p = (CorelyzerPlugin) c.newInstance();
				p.pluginId = pluginObject.size();
				pluginObject.add(p);

				if (p.init(CorelyzerApp.getApp().getMainFrame())) {
					pluginClassNames.add(pluginName);
					p.getFrame().setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

					CorelyzerApp app = CorelyzerApp.getApp();
					if (app != null && app.getPluginUIIndex() == -1 && p.isWantToReplaceMainFrame()) {

						// find the index
						for (int i = 0; i < this.startupArgs.length; i++) {
							if (pluginName.equals(startupArgs[i])) {
								System.out.println("---> [INFO] Setting " + "MainFrame replacement plugin index " + i);

								app.setPluginUIIndex(i);
								app.setUsePluginUI(true);

								// move menuar too
								p.getFrame().setJMenuBar(app.getMenuBar());

								break;
							}
						}
					}
				} else {
					System.err.println("---> [DEBUG] Plugin " + p + " initialize failed!");
					pluginObject.remove(p);
				}
			} catch (Exception e) {
				System.err.println("---> [EXCEPTION] Plugin Manager Init Exception: " + pluginName);

				e.printStackTrace();
			}
		}
	}

	// --------------------------------------------------------------
	/**
	 * Allows a plugin to register for a given type of event.
	 */
	public void registerPluginForEventType(final int pluginId, final int eventType) {
		System.out.println("Plugin " + pluginId + " registering for event " + eventType);
		if (eventType < 0 || eventType >= CorelyzerPluginEvent.NUMBER_OF_EVENTS) {
			System.out.println("Event # too big or small 0 to " + CorelyzerPluginEvent.NUMBER_OF_EVENTS);
			return;
		}

		if (pluginId < 0 || pluginId > pluginObject.size() - 1) {
			System.out.println("Plugin id too big or small 0 to " + (pluginObject.size() - 1));
			return;
		}

		System.out.println("SUCCESS");
		eventRegistry.elementAt(eventType).add(getPlugin(pluginId));
	}

	// --------------------------------------------------------------
	/** Show the GUI of a given plugin. */
	public void showPlugin(final int k) {
		CorelyzerPlugin p = pluginObject.elementAt(k);

		CorelyzerApp app = CorelyzerApp.getApp();
		Component owner = null;
		if (app != null) {
			owner = app.getMainFrame();
		}
		p.getFrame().setLocationRelativeTo(owner);

		p.getFrame().setVisible(true);
	}

	// --------------------------------------------------------------
	/**
	 * Notifies all plugins to close by calling the fini method of all plugins.
	 * WARNING!!! SHOULD NOT BE USED BY PLUGINS
	 */
	public void Shutdown() {

		System.out.println("CALLING FINI(s)");
		for (int i = 0; i < pluginObject.size(); i++) {
			try {
				pluginObject.elementAt(i).fini();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// --------------------------------------------------------------
	public void startup() {
		pluginObject = new Vector<CorelyzerPlugin>();
		pluginClassNames = new Vector<String>();
		pluginMenuNames = new Vector<String>();
		// incomingPluginEvents = new LinkedList();

		startupAllPlugins(startupArgs);
		acquirePluginMenuNames();
		hideAllPlugins();

		CorelyzerApp.getApp().createPluginMenuItems(pluginMenuNames);
	}

	// --------------------------------------------------------------
	private void startupAllPlugins(final String[] pluginNames) {
		System.out.println("---> [INFO] Starting up " + pluginNames.length + " plugins");
		for (String pluginName : pluginNames) {
			loadPlugin(pluginName);
		}
	}

	// --------------------------------------------------------------
	/**
	 * Allows a polugin to UN-register for a given type of event.
	 */
	public void unregisterPluginForEventType(final int pluginId, final int eventType) {
		if (eventType < 0 || eventType >= CorelyzerPluginEvent.NUMBER_OF_EVENTS) {
			System.out.println("Event # too big or small 0 to " + CorelyzerPluginEvent.NUMBER_OF_EVENTS);
			return;
		}

		if (pluginId < 0 || pluginId > pluginObject.size() - 1) {
			System.out.println("Plugin id too big or small 0 to " + (pluginObject.size() - 1));
			return;
		}

		System.out.println("SUCCESS");
		eventRegistry.elementAt(eventType).remove(getPlugin(pluginId));

	}
}
