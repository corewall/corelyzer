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
package corelyzer.util;

import java.awt.Component;
import java.awt.HeadlessException;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import corelyzer.ui.CorelyzerApp;

public class CROptionPane extends JOptionPane {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5270576367287023301L;
	static JFrame mainFrame;

	static {
		mainFrame = CorelyzerApp.getApp().getMainFrame();
	}

	public static int showOptionDialog(final Component parentComponent, final Object message, final String title, final int optionType, final int messageType,
			final Icon icon, final Object[] options, final Object initialValue) throws HeadlessException {
		mainFrame.setAlwaysOnTop(false);

		int res = JOptionPane.showOptionDialog(parentComponent, message, title, optionType, messageType, icon, options, initialValue);

		mainFrame.setAlwaysOnTop(true);
		return res;
	}
}
