package corelyzer.helper;

/** Reference Apple Bonjour Java Client Example */

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.Collator;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.apple.dnssd.BrowseListener;
import com.apple.dnssd.DNSSD;
import com.apple.dnssd.DNSSDException;
import com.apple.dnssd.DNSSDRegistration;
import com.apple.dnssd.DNSSDService;
import com.apple.dnssd.RegisterListener;
import com.apple.dnssd.ResolveListener;
import com.apple.dnssd.TXTRecord;

import corelyzer.ui.CorelyzerApp;

public class BonjourManager extends DefaultListModel<Object> implements ResolveListener, BrowseListener, RegisterListener, Runnable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -451279396607271617L;
	protected static Collator sCollator;

	static // Initialize our static variables
	{
		sCollator = Collator.getInstance();
		sCollator.setStrength(Collator.PRIMARY);
	}

	public static void main(final String[] args) {
		System.out.println("Happy happy Bonjour Manager!");
		new BonjourManager();
	}

	// Bonjour
	DNSSDService browser;
	DNSSDService resolver;

	DNSSDRegistration registration;
	String serviceName = "_corelyzer._tcp";

	int servicePort = 17799;

	private final Vector<BrowserListElem> addCache;
	private final Vector<String> removeCache;

	JMenu rootMenu;

	BonjourManager bonjourManager;

	public BonjourManager() {
		addCache = new Vector<BrowserListElem>();
		removeCache = new Vector<String>();

		initBonjour();

		bonjourManager = this;
	}

	public BonjourManager(final JMenu r) {
		this();
		rootMenu = r;
	}

	protected void addInSortOrder(final Object obj) {
		int i;
		for (i = 0; i < this.size(); i++) {
			if (sCollator.compare(obj.toString(), this.getElementAt(i).toString()) < 0) {
				break;
			}
		}

		this.add(i, obj);
	}

	protected int findMatching(final String match) {
		for (int i = 0; i < this.size(); i++) {
			if (match.equals(this.getElementAt(i).toString())) {
				return i;
			}
		}

		return -1;
	}

	private void initBonjour() {
		// Init Bonjour register and browser

		try {
			System.out.println("-- [INFO] Init Bonjour register for " + serviceName);
			registration = DNSSD.register("", serviceName, servicePort, this);

			System.out.println("-- [INFO] Init Bonjour browser for " + serviceName);

			browser = DNSSD.browse(serviceName, this);
		} catch (DNSSDException e) {
			System.err.println("-- [EXCEPTION] In init bonjour browser" + e);
		}
	}

	public void operationFailed(final DNSSDService dnssdService, final int i) {
		System.err.println("-- [INFO] operationFailed.");
	}

	public void run() {
		while (removeCache.size() > 0) {
			String serviceName = (String) removeCache.remove(removeCache.size() - 1);
			int matchInd = this.findMatching(serviceName);
			// probably doesn't handle near-duplicates well.

			if (matchInd != -1) {
				this.removeElementAt(matchInd);

				if (rootMenu != null) {
					System.out.println("-- [INFO] Remove " + matchInd + " menuitem");
					rootMenu.remove(matchInd);
				}
			}
		}

		while (addCache.size() > 0) {
			final BrowserListElem elem = (BrowserListElem) addCache.remove(addCache.size() - 1);

			if (-1 == this.findMatching(elem.fServiceName)) {
				// probably doesn't handle near-duplicates well.
				this.addInSortOrder(elem);

				if (rootMenu != null) {
					System.out.println("-- [INFO] Add " + elem + " menuitem.");
					JMenuItem aFriend = new JMenuItem(elem.toString());
					aFriend.addActionListener(new ActionListener() {

						public void actionPerformed(final ActionEvent e) {
							// Try to resolve discovered service
							try {
								DNSSD.resolve(0, elem.fInt, elem.fServiceName, elem.fType, elem.fDomain, new SwingResolveListener(bonjourManager));
							} catch (DNSSDException ex) {
								System.err.println("-- [INFO] Exception " + "in DNSSD resolve");
							}

							String message = "Friend " + elem + " in " + elem.fType + ", " + elem.fDomain + ", " + elem.fInt + ", " + elem.fServiceName + ", "
									+ elem.fType;

							JOptionPane.showMessageDialog(CorelyzerApp.getApp().getMainFrame(), message);
						}
					});
					rootMenu.add(aFriend);
				}
			}
		}
	}

	protected void scheduleOnEventThread() {
		try {
			SwingUtilities.invokeAndWait(this);
			// SwingUtilities.invokeLater(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// -- Implement BrowserListener interface

	public void serviceFound(final DNSSDService browser, final int flags, final int ifIndex, final String serviceName, final String regType, final String domain) {
		System.out.println("-- [INFO] Found [" + serviceName + "] in [" + regType + "][" + domain + "]");

		addCache.add(new BrowserListElem(serviceName, domain, regType, ifIndex));

		if ((flags & DNSSD.MORE_COMING) == 0) {
			this.scheduleOnEventThread();
		}
	}

	public void serviceLost(final DNSSDService browser, final int flags, final int ifIndex, final String serviceName, final String regType, final String domain) {
		removeCache.add(serviceName);

		if ((flags & DNSSD.MORE_COMING) == 0) {
			this.scheduleOnEventThread();
		}
	}

	// -- Implement RegisterListener interface

	public void serviceRegistered(final DNSSDRegistration dnssdRegistration, final int flags, final String serviceName, final String regType,
			final String domain) {
		System.out.println("-- [INFO] Great! service [" + serviceName + "] registered");
	}

	public void serviceResolved(final DNSSDService resolver, final int flags, final int ifIndex, final String fullName, final String hostName, final int port,
			final TXTRecord txtRecord) {
		System.out.println("-- [INFO] Resolved: " + fullName + ", " + hostName + ", " + port + ", " + txtRecord);
	}

	public void setRootMenu(final JMenu r) {
		rootMenu = r;
	}
}

class BrowserListElem {
	public String fServiceName, fDomain, fType;
	public int fInt;

	public BrowserListElem(final String serviceName, final String domain, final String type, final int ifIndex) {
		fServiceName = serviceName;
		fDomain = domain;
		fType = type;
		fInt = ifIndex;
	}

	@Override
	public String toString() {
		return fServiceName;
	}
}
