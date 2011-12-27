package corelyzer.plugins.expeditionmanager.util;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.prefs.Preferences;

import corelyzer.plugins.expeditionmanager.ExpeditionManagerPlugin;
import corelyzer.ui.AuthDialog;

/**
 * Extract MyAuthenticator from URLRetrieval for general use. Updated to use a
 * stored username and password.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class URLAuthenticator extends Authenticator {

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        // check for a saved value first
        Preferences prefs = Preferences.userNodeForPackage(getClass());
        String savedUser = prefs.get("username", "ANDRILL");
        String savedPass = prefs.get("password", "SMS-office");

        // use the saved values first
        if ((savedUser != null) && (savedPass != null)) {
            return new PasswordAuthentication(savedUser, savedPass
                    .toCharArray());
        } else {
            return null;
            // return promptUser();
        }
    }

    /**
     * Prompt the user for the username and password.
     * 
     * @return the PasswordAuthentication
     */
    private PasswordAuthentication promptUser() {
        final AuthDialog authDialog = new AuthDialog(ExpeditionManagerPlugin
                .getDefault().getFrame(), "Enter your login information for "
                + getRequestingHost());

        // figure out where to place the dialog
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = screenSize.width / 2 - (authDialog.getSize().width / 2);
        int y = screenSize.height / 2 - (authDialog.getSize().height / 2);
        authDialog.setLocation(x, y);

        // layout and show the dialog
        authDialog.pack();
        authDialog.setVisible(true);

        // get our username and password
        String username = authDialog.getUsername();
        char[] password = authDialog.getPassword();
        if ((username == null) || username.equals("")) {
            return null; // cancelled
        } else {
            Preferences prefs = Preferences.userNodeForPackage(getClass());
            prefs.put("username", username);
            prefs.put("password", new String(password));
            return new PasswordAuthentication(username, password);
        }
    }
}
