/******************************************************************************
 *
 * CoreWall / Corelyzer - An Initial Core Description Tool
 * Copyright (C) 2004, 2005 Arun Gangadhar Gudur Rao, Julian Yu-Chung Chen
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

package corelyzer.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;

/** A class created to choose a color and return the selected value. */
public class ColorChooser extends JDialog implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4584664426340964570L;
	JColorChooser tcc;
	JButton okbtn;
	JComponent target;
	ActionListener returnListener;

	public ColorChooser(final JDialog parent) {
		super(parent);

		this.setSize(400, 600);
		this.setLayout(new BorderLayout());
		this.setLocationRelativeTo(parent);

		tcc = new JColorChooser(Color.black);
		tcc.setBorder(BorderFactory.createTitledBorder("Choose a color"));
		this.add(tcc, BorderLayout.CENTER);

		okbtn = new JButton("Close");
		okbtn.addActionListener(this);
		this.add(okbtn, BorderLayout.PAGE_END);
	}

	public ColorChooser(final JFrame parent) {
		super(parent);

		this.setSize(400, 600);
		this.setLayout(new BorderLayout());
		this.setLocationRelativeTo(parent);

		tcc = new JColorChooser(Color.black);
		tcc.setBorder(BorderFactory.createTitledBorder("Choose a color"));
		this.add(tcc, BorderLayout.CENTER);

		okbtn = new JButton("Close");
		okbtn.addActionListener(this);
		this.add(okbtn, BorderLayout.PAGE_END);
	}

	public void actionPerformed(final ActionEvent e) {
		if (target != null) {
			target.setBackground(tcc.getColor());
			if (returnListener != null) {
				System.out.println("Sending action event to return listener");
				returnListener.actionPerformed(new ActionEvent(this, 0, ""));
			} else {
				System.out.println("Return Listener is Null!!!");
			}
		}

		dispose();
	}

	public void addReturnActionListener(final ActionListener al) {
		returnListener = al;
	}

	public Color getColor() {
		return tcc.getColor();
	}

	public void setColor(final Color c) {
		this.tcc.setColor(c);
	}

	public void setTarget(final JComponent t) {
		target = t;
	}

}
