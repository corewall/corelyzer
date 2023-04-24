/******************************************************************************
 *
 * CoreWall / Corelyzer - An Initial Core Description Tool
 * Copyright (C) 2007 Julian Yu-Chung Chen
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
package corelyzer.ui.annotation.freeform;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import corelyzer.data.ChatGroup;
import corelyzer.data.MarkerType;
import corelyzer.graphics.SceneGraph;
import corelyzer.plugin.CorelyzerPluginEvent;
import corelyzer.ui.AttachmentURLDialog;
import corelyzer.ui.CorelyzerApp;
import corelyzer.ui.annotation.AbstractAnnotationDialog;
import corelyzer.ui.annotation.AnnotationUtils;

public class CRAnnotationWindow extends AbstractAnnotationDialog {
	/**
	 * Class used to insert an attachment link into the HTML editor
	 */
	public class AttachmentAction extends StyledEditorKit.StyledTextAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = 4303886419785967511L;

		public AttachmentAction() {
			super("Attachment");
		}

		public void actionPerformed(final ActionEvent ae) {
			preview.requestFocus(); // should prevent null getEditor() result
			JEditorPane myeditor = getEditor(ae);
			if (myeditor == null) {
				// no editor in focus...just ignore this for now
				return;
			}

			HTMLEditorKit kit = (HTMLEditorKit) myeditor.getEditorKit();
			HTMLDocument doc = (HTMLDocument) myeditor.getDocument();

			Object parentDialog = this.getValue("parent_dialog");
			AttachmentURLDialog dialog = new AttachmentURLDialog((java.awt.Dialog)parentDialog);
			dialog.setLocationRelativeTo(preview);
			dialog.setIsImageLoader(false);
			dialog.setPreferences(CorelyzerApp.getApp().preferences());
			dialog.pack();
			dialog.setVisible(true);

			String value = dialog.getURLString();
			// System.out.println("-- [DEBUG] Selected file URL: " + value);

			if ((value != null) && !value.equals("")) {
				// System.out.println("-- [DEBUG] Selected file: " + value);

				try {
					// String iconURLString =(new File(".")).getAbsolutePath() +
					// System.getProperty("file.separator")
					// + "resources/icons/attachment.gif";
					String filename = new File(value).getName();
					String htmlCode = "<br><a href=\"" + value + "\">" + "[" + filename + "]</a><br>";

					kit.insertHTML(doc, myeditor.getCaretPosition(), htmlCode, 0, 0, HTML.Tag.A);
				} catch (Exception e) {
					JOptionPane.showMessageDialog(CRAnnotationWindow.this, "Attachment Not Added", "ERROR", JOptionPane.ERROR_MESSAGE);
					e.printStackTrace();
				}
			} else {
				System.out.println("-- [INFO] Nothing from URL input, ignore.");
			}
		}
	}

	// ----------------------------
	public class BRAction extends StyledEditorKit.StyledTextAction {

		/**
		 * 
		 */
		private static final long serialVersionUID = 2152756739489457451L;

		public BRAction() {
			super("InsertBR");
		}

		public void actionPerformed(final ActionEvent e) {
			insertBreak(e);
		}

		private void insertBreak(final ActionEvent e) {
			JEditorPane editor = this.getEditor(e);

			if (editor == null) {
				return;
			}

			HTMLEditorKit kit = (HTMLEditorKit) editor.getEditorKit();
			HTMLDocument doc = (HTMLDocument) editor.getDocument();
			int caretPos = editor.getCaretPosition();

			try {
				kit.insertHTML(doc, caretPos, "<BR>", 0, 0, HTML.Tag.BR);
			} catch (IOException ioe) {
				System.err.println("IOException in inserting <BR>");
			} catch (BadLocationException ble) {
				System.err.println("BadLocationException in inserting <BR>");
			}

			editor.setCaretPosition(caretPos + 1);
		}
	}

	/**
	 * Class used to insert an image into the HTML editor
	 */
	public class ImageAction extends StyledEditorKit.StyledTextAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = -5988515917804316138L;

		public ImageAction() {
			super("InsertIMG");
		}

		public void actionPerformed(final ActionEvent ae) {
			preview.requestFocus(); // should prevent null getEditor() result
			JEditorPane myeditor = getEditor(ae);
			if (myeditor == null) {
				// no editor in focus...just ignore this for now
				return;
			}

			HTMLEditorKit kit = (HTMLEditorKit) myeditor.getEditorKit();
			HTMLDocument doc = (HTMLDocument) myeditor.getDocument();

			Object parentDialog = this.getValue("parent_dialog");
			AttachmentURLDialog dialog = new AttachmentURLDialog((java.awt.Dialog)parentDialog);
			dialog.setLocationRelativeTo(preview);
			dialog.setIsImageLoader(true);
			dialog.setPreferences(CorelyzerApp.getApp().preferences());
			dialog.pack();
			dialog.setVisible(true);

			String value = dialog.getURLString();
			// System.out.println("-- [DEBUG] Selected file URL: " + value);

			if ((value != null) && !value.equals("")) {
				try {
					String htmlCode = "<br><img src=\"" + value + "\">";
					kit.insertHTML(doc, myeditor.getCaretPosition(), htmlCode, 0, 0, HTML.Tag.IMG);
				} catch (Exception e) {
					JOptionPane.showMessageDialog(CRAnnotationWindow.this, "Image Not Loaded", "ERROR", JOptionPane.ERROR_MESSAGE);
					e.printStackTrace();
				}
			}

		}
	}

	/**
	 * inner class borrowed from Oreilly's Java Swing 2e Ch23 examples
	 */
	public class TagAction extends HTMLEditorKit.HTMLTextAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = 2039697036902820218L;
		private final HTML.Tag tag;
		private final HTML.Attribute tagAttr;
		private final String tagName;

		public TagAction(final HTML.Tag t, final String s, final HTML.Attribute a) {
			super(s);
			tag = t;
			tagName = s;
			tagAttr = a;
		}

		public void actionPerformed(final ActionEvent e) {
			JEditorPane myeditor = getEditor(e);

			if (myeditor != null) {
				String value = JOptionPane.showInputDialog(this, "Enter " + tagName + ":");

				if ((value != null) && !value.equals("")) {
					StyledEditorKit kit = getStyledEditorKit(myeditor);
					MutableAttributeSet attr = kit.getInputAttributes();
					boolean anchor = attr.isDefined(tag);
					if (anchor) {
						attr.removeAttribute(tag);
					} else {
						SimpleAttributeSet as = new SimpleAttributeSet();
						as.addAttribute(tagAttr, value);
						attr.addAttribute(tag, as);
					}
					setCharacterAttributes(myeditor, attr, false);
				}
			}
		}
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 5351116115299567983L;

	public static void main(final String[] args) {
		CRAnnotationWindow dialog = new CRAnnotationWindow();
		dialog.pack();
		dialog.setVisible(true);
		System.exit(0);
	}

	private JPanel contentPane;
	private JButton buttonOK;
	private JButton buttonCancel;
	private JEditorPane discussion;
	private JButton refreshButton;
	private JButton openInBrowserButton;

	private JComboBox<String> groups;
	private JEditorPane preview;
	private JButton deleteButton;

	private JToolBar editToolbar;

	private JScrollPane discussionScrollPane;

	HTMLEditorKit htmlKit;
	boolean new_annotation = true;

	boolean write_local = true;
	String datasetName, sectionName;
	// Annotation's X position
	float x_pos, y_pos;

	URL url;

	String scratchFileName;

	boolean withGroups = true;

	int group = ChatGroup.UNDEFINED;

	int type = MarkerType.CORE_DEFAULT_MARKER;

	{
		// GUI initializer generated by IntelliJ IDEA GUI Designer
		// >>> IMPORTANT!! <<<
		// DO NOT EDIT OR ADD ANY CODE HERE!
		$$$setupUI$$$();
	}

	public CRAnnotationWindow() {
		setupUI();
		setContentPane(contentPane);
		setModal(true);
		getRootPane().setDefaultButton(buttonOK);

		buttonOK.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				onSave();
			}
		});

		buttonCancel.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				onCancel();
			}
		});

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(final WindowEvent e) {
				onCancel();
			}
		});

		contentPane.registerKeyboardAction(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				onCancel();
			}
		}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

		deleteButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				onDelete();
			}
		});

		refreshButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				onRefresh();
			}
		});
		openInBrowserButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				onOpenInBrowser();
			}
		});
	}

	public CRAnnotationWindow(final String title) {
		this();

		String user = System.getProperty("user.name");
		setTitle(title + " by " + user);
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
		contentPane = new JPanel();
		contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
		final JPanel panel1 = new JPanel();
		panel1.setLayout(new GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
		contentPane.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
		final Spacer spacer1 = new Spacer();
		panel1.add(spacer1, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
		final JPanel panel2 = new JPanel();
		panel2.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
		panel1.add(panel2, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		buttonOK = new JButton();
		buttonOK.setText("Save");
		panel2.add(buttonOK, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		buttonCancel = new JButton();
		buttonCancel.setText("Cancel");
		panel2.add(buttonCancel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		deleteButton = new JButton();
		deleteButton.setText("Delete");
		panel2.add(deleteButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		openInBrowserButton = new JButton();
		openInBrowserButton.setText("Open in Browser");
		panel1.add(openInBrowserButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		refreshButton = new JButton();
		refreshButton.setText("Refresh");
		panel1.add(refreshButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JPanel panel3 = new JPanel();
		panel3.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
		contentPane
				.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
						| GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null,
						0, false));
		final JPanel panel4 = new JPanel();
		panel4.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
		panel3.add(panel4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null,
				new Dimension(24, 349), null, 0, false));
		panel4.setBorder(BorderFactory.createTitledBorder("Discussion"));
		discussionScrollPane = new JScrollPane();
		discussionScrollPane.setHorizontalScrollBarPolicy(31);
		discussionScrollPane.setVerticalScrollBarPolicy(22);
		panel4.add(discussionScrollPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK
						| GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(629, 370), null, 0, false));
		discussion = new JEditorPane();
		discussion.setEditable(false);
		discussionScrollPane.setViewportView(discussion);
		final JPanel panel5 = new JPanel();
		panel5.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
		panel3.add(panel5, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		panel5.setBorder(BorderFactory.createTitledBorder("Input"));
		final JPanel panel6 = new JPanel();
		panel6.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
		panel5.add(panel6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		final JLabel label1 = new JLabel();
		label1.setText("Group: ");
		panel6.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
				GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		groups = new JComboBox<String>();
		final DefaultComboBoxModel<String> defaultComboBoxModel1 = new DefaultComboBoxModel<String>();
		defaultComboBoxModel1.addElement("UNDEFINED");
		defaultComboBoxModel1.addElement("SEDIMENTOLOGY");
		defaultComboBoxModel1.addElement("GEOPHYSICS");
		defaultComboBoxModel1.addElement("BIOCHEMISTRY");
		defaultComboBoxModel1.addElement("OPERATIONAL");
		defaultComboBoxModel1.addElement("EDUCATIONAL");
		defaultComboBoxModel1.addElement("LITHOLOGY");
		defaultComboBoxModel1.addElement("PETROLOGY");
		defaultComboBoxModel1.addElement("CLAST");
		defaultComboBoxModel1.addElement("SAMPLE");
		defaultComboBoxModel1.addElement("DIS");
		groups.setModel(defaultComboBoxModel1);
		panel6.add(groups, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW,
				GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		editToolbar = new JToolBar();
		panel6.add(editToolbar, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
				GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(-1, 20), null, 0, false));
		final JScrollPane scrollPane1 = new JScrollPane();
		scrollPane1.setHorizontalScrollBarPolicy(31);
		scrollPane1.setVerticalScrollBarPolicy(22);
		panel5.add(scrollPane1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0,
				false));
		preview = new JEditorPane();
		scrollPane1.setViewportView(preview);
	}

	/* Append input HTML text to discussion thread */
	private void appendAnnotation(final int origGroup, final int newGroup) {
		String groupMods = "";
		if (origGroup != newGroup) {
			groupMods = "<br>\nGroup type changed from " + ChatGroup.getGroupName(origGroup) + " to " + ChatGroup.getGroupName(newGroup) + "<br>\n";
		}

		String text = discussion.getText();
		int bodyend = text.lastIndexOf("</body>");
		String before, end;
		before = text.substring(0, bodyend);
		end = text.substring(bodyend, text.length());

		String colors[] = { "#ccccff", "#ffffff" };
		int lastbgcolor = text.lastIndexOf("bgcolor=");
		int color = 1;
		if (lastbgcolor != -1) {
			if (text.charAt(lastbgcolor + 10) == 'f') {
				color = 0;
			}
		}

		Date now = new Date(System.currentTimeMillis());
		SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyy 'at' hh:mm:ss z");
		String time = format.format(now);

		String header = "<b>" + "On " + time + " " + System.getProperty("user.name") + " wrote: </b><br>";

		String ls = System.getProperty("line.separator");
		String newtext = preview.getText();
		// System.out.println("appendAnnotation0: " + ls + newtext);

		// fix bug of tagaction html
		int st, ed;
		st = 0;
		ed = newtext.indexOf("<body>") + 6;
		newtext = newtext
				.replace(newtext.substring(st, ed), "<table bgcolor=\"" + colors[color] + "\" width=\"100%\" border=\"0\">" + ls + "<tr><td>" + header);
		st = newtext.indexOf("</body>");
		ed = newtext.indexOf("</html>") + 7;
		newtext = newtext.replace(newtext.substring(st, ed), groupMods + "</td></tr></table><hr>");

		// System.out.println("appendAnnotation1: " + ls + newtext);

		text = before + newtext + end;
		text = text.replaceAll("<p style=\"margin-top: 0\">", "<p style=\"margin-left: 5%; " + "margin-right: 10%; " + "font-family: sans-serif;\">");
		discussion.setText(text);

		if (write_local) {
			// System.out.println("-- [INFO] Write to local!");
			try {
				File f = new File(scratchFileName);
				FileWriter fw = new FileWriter(f);
				fw.write(discussion.getText(), 0, discussion.getText().length());
				fw.close();
			} catch (Exception ee) {
				System.out.println("Failed to write out html to disk:");
				ee.printStackTrace();
			}

			// Call scenegraph to set marker's URL & local filepath
			String u;

			try {
				u = new File(scratchFileName).toURI().toURL().toString();
			} catch (MalformedURLException e) {
				u = "file:////" + scratchFileName;
			}

			try {
				setURL(new URL(u));
			} catch (MalformedURLException e) {
				System.err.println("--> MalformedURL in AnnotationWindow.");
			} catch (IOException e) {
				System.err.println("--> IOException in AnnotationWindow");
				e.printStackTrace();
			}

			SceneGraph.setCoreSectionMarkerURL(trackId, sectionId, markerId, u);
			SceneGraph.setCoreSectionMarkerLocal(trackId, sectionId, markerId, scratchFileName);

			// System.out.println("Annotation local:" + scratchFileName);
			// System.out.println("Annotation URL:  " + u);
		} else {
			System.out.println("TODO!!! Need to handle non-local, delegate to plugin");

			// TODO #handleNewEntryWithoutLocalSave();
			// TODO Needs to be filled in proper markinfo,
			//
			// String newenry = this.getInputEntryHTML();
			// Without local or URL, re-click the marker in GLContext could
			// be a problem
		}

		// Update GL context
		CorelyzerApp.getApp().updateGLWindows();

		preview.requestFocus();
		repaint();
	}

	@Override
	public void collectViewInfo() {
	}

	private void createToolBar() {
		// Add cut/copy/paste buttons.
		editToolbar.add(preview.getActionMap().get(DefaultEditorKit.cutAction)).setText("");
		editToolbar.add(preview.getActionMap().get(DefaultEditorKit.copyAction)).setText("");
		editToolbar.add(preview.getActionMap().get(DefaultEditorKit.pasteAction)).setText("");

		// add some text styles
		editToolbar.addSeparator();

		editToolbar.add(preview.getActionMap().get("font-bold")).setText("");
		editToolbar.add(preview.getActionMap().get("font-italic")).setText("");
		editToolbar.add(preview.getActionMap().get("font-underline")).setText("");

		// add some html buttons
		editToolbar.addSeparator();
		editToolbar.add(preview.getActionMap().get("InsertHR")).setText("");
		editToolbar.add(preview.getActionMap().get("InsertBR")).setText("");
		editToolbar.add(preview.getActionMap().get("InsertIMG")).setText("");
		editToolbar.add(preview.getActionMap().get("anchor-link")).setText("");

		// add attachment link button
		editToolbar.addSeparator();
		editToolbar.add(preview.getActionMap().get("Attachment")).setText("");
	}

	protected JTextComponent getEditor() {
		return discussion;
	}

	// Get the HTML text block of input entry
	public String getInputEntryHTML(final int origGroup, final int newGroup) {
		String ls = System.getProperty("line.separator");

		String groupMods = "";
		if (origGroup != newGroup) {
			groupMods = "<br>\nGroup type changed from " + ChatGroup.getGroupName(origGroup) + " to " + ChatGroup.getGroupName(newGroup) + "<br>\n";
		}

		// who wrote this
		Date now = new Date(System.currentTimeMillis());
		SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyy 'at' hh:mm:ss z");
		String time = format.format(now);

		String header = "<b>" + "On " + time + " " + System.getProperty("user.name") + " wrote: </b><br>";

		String newtext = preview.getText();
		newtext = newtext.replace("<html>" + ls + "  <head>" + ls + ls + "  </head>" + ls + "  <body>" + ls, "\n<table bgcolor=\"#ffffff\""
				+ " width=\"100%\" border=\"0\">" + ls + "<tr><td>" + header);
		newtext = newtext.replace("  </body>" + ls + "</html>", groupMods + "</td></tr></table><hr>\n");

		return newtext;
	}

	/**
	 * Get the current URL loaded
	 * 
	 * @return url string
	 */
	public String getURL() {
		return url.toString();
	}

	public void goToBottom() {
		JScrollBar js = discussionScrollPane.getVerticalScrollBar();
		js.setValue(js.getMaximum());
	}

	private void makeActions() {
		Action a;

		a = preview.getActionMap().get(DefaultEditorKit.cutAction);
		a.putValue(Action.SMALL_ICON, new ImageIcon("resources/icons/cut.gif"));
		// a.putValue(Action.NAME, "Cut");

		a = preview.getActionMap().get(DefaultEditorKit.copyAction);
		a.putValue(Action.SMALL_ICON, new ImageIcon("resources/icons/copy.gif"));
		// a.putValue(Action.NAME, "Copy");

		a = preview.getActionMap().get(DefaultEditorKit.pasteAction);
		a.putValue(Action.SMALL_ICON, new ImageIcon("resources/icons/paste.gif"));
		// a.putValue(Action.NAME, "Paste");

		// bold
		a = preview.getActionMap().get("font-bold");
		a.putValue(Action.SMALL_ICON, new ImageIcon("resources/icons/bold.gif"));
		// a.putValue(Action.NAME, "Bold");

		// italic
		a = preview.getActionMap().get("font-italic");
		a.putValue(Action.SMALL_ICON, new ImageIcon("resources/icons/italic.gif"));
		// a.putValue(Action.NAME, "Italic");

		// underline
		a = preview.getActionMap().get("font-underline");
		a.putValue(Action.SMALL_ICON, new ImageIcon("resources/icons/underline.gif"));
		// a.putValue(Action.NAME, "Underline");

		// <hr>
		a = preview.getActionMap().get("InsertHR");
		a.putValue(Action.SMALL_ICON, new ImageIcon("resources/icons/hr.gif"));
		// a.putValue(Action.NAME, "HR");

		// create an "Insert <br>" action
		// a = new HTMLEditorKit.InsertHTMLTextAction("InsertBR", "<br>",
		// HTML.Tag.BODY, HTML.Tag.BR);
		a = new BRAction();
		a.putValue(Action.SMALL_ICON, new ImageIcon("resources/icons/break.gif"));
		preview.getActionMap().put("InsertBR", a);
		// a.putValue(Action.NAME, "BR");

		// link
		a = new TagAction(HTML.Tag.A, "URL", HTML.Attribute.HREF);
		a.putValue(Action.SMALL_ICON, new ImageIcon("resources/icons/link.gif"));
		preview.getActionMap().put("anchor-link", a);
		// a.putValue(Action.NAME, "Anchor Link");

		// image
		a = new ImageAction();
		a.putValue(Action.SMALL_ICON, new ImageIcon("resources/icons/picture.gif"));
		a.putValue("parent_dialog", this);
		preview.getActionMap().put("InsertIMG", a);
		// a.putValue(Action.NAME, "Image");

		// Attachments
		a = new AttachmentAction();
		a.putValue("parent_dialog", this);
		a.putValue(Action.SMALL_ICON, new ImageIcon("resources/icons/open.gif"));
		preview.getActionMap().put("Attachment", a);
		// a.putValue(Action.NAME, "Image");

	}

	private void onCancel() {
		dispose();
	}

	private void onOpenInBrowser() {
		String url = getURL();
		// System.out.println(
		// "-- [INFO] Open annotation in system browser " + url);

		if ((url != null) && !url.equals("")) {

			String app;
			try {
				if (System.getProperty("os.name").toLowerCase().contains("windows")) {
					app = "cmd.exe /c explorer " + url;
					Runtime.getRuntime().exec(app);
				} else {
					app = "open";
					String[] cmd = { app, url };
					Runtime.getRuntime().exec(cmd);
				}

				setVisible(false);
			} catch (IOException ex) {
				JOptionPane.showMessageDialog(this, ex);
				System.err.println("IOException in opening annotation " + "file with system browser");
			}
		}
	}

	private void onRefresh() {
		try {
			URL u = discussion.getPage();
			Document doc = discussion.getDocument();
			doc.putProperty(Document.StreamDescriptionProperty, null);
			repaint();
			discussion.setPage(u);
			repaint();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public void onSave() {
		// todo: save to a plist instead
		if (new_annotation) {
			// create an annotation in native scene graph
			markerId = SceneGraph.createCoreSectionMarker(trackId, sectionId, group, type, x_pos, -y_pos / 2);

			if (markerId == -1) {
				return;
			}

			// Create annotation on the region specified
			float posx = (upperLeftPoint[0] + lowerRightPoint[0]) / 2;
			float posy = (upperLeftPoint[1] + lowerRightPoint[1]) / 2;

			SceneGraph.setCoreSectionMarkerType(trackId, sectionId, markerId, MarkerType.CORE_OUTLINE_MARKER);

			SceneGraph.setCoreSectionMarkerVertex(trackId, sectionId, markerId, posx, -posy / 2, upperLeftPoint[0], upperLeftPoint[1], lowerRightPoint[0],
					lowerRightPoint[1]);

			SceneGraph.lock();
			SceneGraph.setCoreSectionMarkerGroup(trackId, sectionId, markerId, group);
			SceneGraph.unlock();

			String desc = "";
			/*
			 * desc = desc + "" + this.trackId + "\t" + this.sectionId + "\t" +
			 * this.group + "\t" + this.type + "\t" + (x_pos /
			 * corelyzer.helper.SceneGraph.getCanvasDPIX(0))+ "\t" + (y_pos /
			 * corelyzer.helper.SceneGraph.getCanvasDPIY(0));
			 */
			// just use point type marker for now
			desc = desc + "" + trackId + "\t" + sectionId + "\t" + group + "\t" + MarkerType.CORE_POINT_MARKER + "\t" + x_pos / SceneGraph.getCanvasDPIX(0)
					+ "\t" + y_pos / SceneGraph.getCanvasDPIY(0);

			CorelyzerApp.getApp().getPluginManager().broadcastEventToPlugins(CorelyzerPluginEvent.NEW_ANNOTATION, desc);

			scratchFileName = AnnotationUtils.generateFilename(trackId, sectionId, markerId);
			// System.out.println("-- [DEBUG] Generated scratchFileName: "
			// + scratchFileName);

			new_annotation = false;
		} else {
			// markerId = corelyzer.helper.SceneGraph.accessPickedMarker();
		}

		// Update group type
		int origGroupId;
		SceneGraph.lock();
		{
			origGroupId = SceneGraph.getCoreSectionMarkerGroup(trackId, sectionId, markerId);

			if (origGroupId != group) {
				SceneGraph.setCoreSectionMarkerGroup(trackId, sectionId, markerId, group);
			}
		}
		SceneGraph.unlock();

		String entry = getInputEntryHTML(origGroupId, group);

		// send out entry to plugins
		String desc = "" + trackId + "\t" + sectionId + "\t" + markerId + "\t" + entry;
		CorelyzerApp.getApp().getPluginManager().broadcastEventToPlugins(CorelyzerPluginEvent.NEW_ANNOTATION_ENTRY, desc);

		if (write_local) {
			// discussion thread style annotation submission
			appendAnnotation(origGroupId, group);
		}

		// Updates
		preview.setText("");
		preview.requestFocus();

		openInBrowserButton.setEnabled(true);
		deleteButton.setEnabled(true);

		// freeform annotation submission
		// submitFreeFormAnnotation();

		CorelyzerApp.getApp().updateGLWindows();
	}

	/**
	 * Indicate that we are editing an existing annotation
	 */
	public void setEditExistingMode() {
		new_annotation = false;
	}

	/**
	 * Indicate that we are editing a new annotation
	 */
	public void setEditNewMode() {
		new_annotation = true;
		discussion.setText("");
		preview.setText("");
	}

	public void setGroup(final int g) {
		group = g;

		groups.setSelectedIndex(g);

		/*
		 * if(!new_annotation) { this.groups.setEnabled(false); } else {
		 * this.groups.setEnabled(true); }
		 */
	}

	// ------------------------------------------------------------------------

	public void setType(final int t) {
		type = t;
	}

	private void setupUI() {
		setTitle("Annotation Window");

		// For Preview editorPane
		htmlKit = new HTMLEditorKit();
		htmlKit.getStyleSheet().addRule("body { font-family: sans-serif; margin-right: 20%;" + "margin-left: 20%; }");
		preview.setEditorKit(htmlKit);

		// For Discussion editorPane
		discussion.setEditorKit(htmlKit);
		discussion.setEditable(false);
		discussion.setText("");

		// For editing toolbar
		makeActions();
		createToolBar();
		updateInputMap();

	}

	// Load up a given URL if it is valid
	public boolean setURL(final URL u) throws IOException {
		if (u.getProtocol().equalsIgnoreCase("file")) {
			scratchFileName = u.getFile();
		} else {
			scratchFileName = AnnotationUtils.generateFilename(trackId, sectionId, markerId);
		}

		url = u;

		Document doc = discussion.getDocument();
		doc.putProperty(Document.StreamDescriptionProperty, null);
		repaint();
		discussion.setPage(u);
		repaint();
		return true;
	}

	// Set whether write to local copy discussion thread, default true
	// Used to when submit button is clicked. See method appendAnnotation()
	public void setWriteLocalCopy(final boolean b) {
		write_local = b;
	}

	private void updateInputMap() {
		InputMap map = getEditor().getInputMap();
		int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

		KeyStroke bold = KeyStroke.getKeyStroke(KeyEvent.VK_B, mask, false);
		KeyStroke italic = KeyStroke.getKeyStroke(KeyEvent.VK_I, mask, false);
		KeyStroke under = KeyStroke.getKeyStroke(KeyEvent.VK_U, mask, false);
		KeyStroke br = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false);

		map.put(bold, "font-bold");
		map.put(italic, "font-italic");
		map.put(under, "font-underline");

		map.put(br, "InsertBR");
	}

}
