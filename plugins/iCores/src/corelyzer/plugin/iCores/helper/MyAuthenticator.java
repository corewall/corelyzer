package corelyzer.plugin.iCores.helper;

import corelyzer.ui.AuthDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.Authenticator;
import java.net.PasswordAuthentication;

// Helper class to access URLs which require authentication
public class MyAuthenticator extends Authenticator implements ActionListener {
    private boolean authentication_cancelled = false;

    public MyAuthenticator() {
        super();    
    }

    protected PasswordAuthentication getPasswordAuthentication() {
        AuthDialog authDialog = new AuthDialog(getRequestingPrompt());

        authDialog.setUsername("");
        authDialog.setPassword("");
        authDialog.setCancelActionListener(this);

        Dimension scrnsize = Toolkit.getDefaultToolkit().getScreenSize();
        int loc_x = scrnsize.width / 2 - (authDialog.getSize().width / 2);
        int loc_y = scrnsize.height / 2 - (authDialog.getSize().height / 2);
        authDialog.setLocation(loc_x, loc_y);

        authDialog.pack();
        authDialog.setVisible(true);

        if (!authentication_cancelled) {
            return (new PasswordAuthentication(authDialog.getUsername(),
                    authDialog.getPassword()));
        } else {
            return null;
        }
    }

    public void actionPerformed(ActionEvent event) {
        // let only authDialog's cancelBtn register for actionEvent,
        // so only when cancel button clicked will trigger the following
        // actions
        JButton btn = (JButton) event.getSource();

        if (btn.getText().toLowerCase().contains("cancel")) {
            authentication_cancelled = true;
        }
    }

}