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
/* Deprecated: this class is replaced by CRAnnotationWindow.java
 *              Except, session client plugin is still referencing to this.
 * */

package corelyzer.ui.annotation.freeform;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
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

import corelyzer.data.ChatGroup;
import corelyzer.data.MarkerType;
import corelyzer.graphics.SceneGraph;
import corelyzer.plugin.CorelyzerPluginEvent;
import corelyzer.ui.AttachmentURLDialog;
import corelyzer.ui.CorelyzerApp;
import corelyzer.ui.annotation.AbstractAnnotationDialog;
import corelyzer.ui.annotation.AnnotationUtils;

public class AnnotationWindow extends AbstractAnnotationDialog implements ActionListener {

	/** Class used to insert an attachment link into the HTML editor */
	public class AttachmentAction extends StyledEditorKit.StyledTextAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = 7536223500739554201L;

		public AttachmentAction() {
			super("Attachment");
		}

		public void actionPerformed(final ActionEvent ae) {
			JEditorPane myeditor = getEditor(ae);
			if (myeditor == null) {
				// no editor in focus...just ignore this for now
				return;
			}

			HTMLEditorKit kit = (HTMLEditorKit) myeditor.getEditorKit();
			HTMLDocument doc = (HTMLDocument) myeditor.getDocument();

			AttachmentURLDialog dialog = new AttachmentURLDialog();
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
					JOptionPane.showMessageDialog(AnnotationWindow.this, "Attachment Not Added", "ERROR", JOptionPane.ERROR_MESSAGE);
					e.printStackTrace();
				}
			} else {
				System.out.println("-- [INFO] Nothing from URL input, ignore.");
			}
		}
	}

	public class BRAction extends StyledEditorKit.StyledTextAction {

		/**
		 * 
		 */
		private static final long serialVersionUID = 2397955547417757177L;

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

	/** Class used to insert an image into the HTML editor */
	public class ImageAction extends StyledEditorKit.StyledTextAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = -469994435648591566L;

		public ImageAction() {
			super("InsertIMG");
		}

		public void actionPerformed(final ActionEvent ae) {
			JEditorPane myeditor = getEditor(ae);
			if (myeditor == null) {
				// no editor in focus...just ignore this for now
				return;
			}

			HTMLEditorKit kit = (HTMLEditorKit) myeditor.getEditorKit();
			HTMLDocument doc = (HTMLDocument) myeditor.getDocument();

			AttachmentURLDialog dialog = new AttachmentURLDialog();
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
					JOptionPane.showMessageDialog(AnnotationWindow.this, "Image Not Loaded", "ERROR", JOptionPane.ERROR_MESSAGE);
					e.printStackTrace();
				}
			}

		}
	}

	/** inner class borrowed from Oreilly's Java Swing 2e Ch23 examples */
	public class TagAction extends HTMLEditorKit.HTMLTextAction {
		/**
		 * 
		 */
		private static final long serialVersionUID = -1133555737412551316L;
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
	private static final long serialVersionUID = 360393137745819081L;

	public static void main(final String[] args) {
		AnnotationWindow win = new AnnotationWindow("Annotation");
		try {
			if (args.length > 0) {
				win.setURL(new URL(args[0]));
			}
		} catch (Exception e) {
			System.err.println("-- [INFO] Exception in testing main()");
		}
		win.setVisible(true);
	}

	JEditorPane preview;
	JEditorPane discussion;

	JScrollPane discussionPane;
	HTMLEditorKit htmlKit;
	JPanel windowPanel;
	boolean new_annotation = true;
	boolean write_local = true;
	JButton submitbtn;

	JButton closebtn;

	JButton showbtn;

	JButton refreshbtn;

	JButton browserBtn;
	JButton deleteBtn;

	JComboBox groups;
	// int trackId, sectionId, markerId;
	String datasetName, sectionName;
	/** Annotation's X position */
	float x_pos, y_pos;

	URL url;

	String scratchFileName;

	boolean withGroups = true;

	int group = ChatGroup.UNDEFINED;

	int type = MarkerType.CORE_DEFAULT_MARKER;

	public AnnotationWindow() {
		super();

		setupUI();
		updateInputMap();
		this.setSize(700, 650);
		this.setLocation(300, 100);
		setResizable(true);

		windowPanel.registerKeyboardAction(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				// onCancel();
				setVisible(false);
				markerId = -1;
			}
		}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
	}

	/**
	 * Constructor takes a string and creates a title using the incoming string
	 * and the user name
	 * 
	 * @param title
	 *            The title of the AnnotationWindow
	 */
	public AnnotationWindow(final String title) {
		this();

		String user = System.getProperty("user.name");
		setTitle(title + " by " + user);
	}

	/** Performs user interface actions for closing, submitting, etc. */
	public void actionPerformed(final ActionEvent e) {
		if (e.getSource() instanceof JComboBox) {
			JComboBox cb = (JComboBox) e.getSource();

			// String groupName = (String) cb.getSelectedItem();
			group = cb.getSelectedIndex();

			// System.out.println("Annotation group is set to: " +
			// groupName + "[" + this.group + "]");

		} else if (e.getSource() instanceof JButton) {
			JButton b = (JButton) e.getSource();

			if (b.getText().equals("Close")) {
				setVisible(false);
				markerId = -1;
			} else if (b.getText().equals("Refresh")) {
				refresh();
			} else if (b.getText().equals("Show")) {
				System.out.println("Show HTML source:");
				System.out.println("---------------------------------------");
				System.out.println(getInputEntryHTML(group, group));
				System.out.println("---------------------------------------");
			} else if (b.getText().equals("Open in Browser")) {
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
			} else {
				System.out.println("Button " + b.getText() + " is pressed");
			}
		} else {
			System.err.println("Unknown action target!" + e);
		}
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
		// todo
	}

	protected JToolBar createToolBar() {
		JToolBar bar = new JToolBar();

		// Add cut/copy/paste buttons.
		bar.add(preview.getActionMap().get(DefaultEditorKit.cutAction)).setText("");
		bar.add(preview.getActionMap().get(DefaultEditorKit.copyAction)).setText("");
		bar.add(preview.getActionMap().get(DefaultEditorKit.pasteAction)).setText("");

		// add some text styles
		bar.addSeparator();

		bar.add(preview.getActionMap().get("font-bold")).setText("");
		bar.add(preview.getActionMap().get("font-italic")).setText("");
		bar.add(preview.getActionMap().get("font-underline")).setText("");

		// add some html buttons
		bar.addSeparator();
		bar.add(preview.getActionMap().get("InsertHR")).setText("");
		bar.add(preview.getActionMap().get("InsertBR")).setText("");
		bar.add(preview.getActionMap().get("InsertIMG")).setText("");
		bar.add(preview.getActionMap().get("anchor-link")).setText("");

		// add attachment link button
		bar.addSeparator();
		bar.add(preview.getActionMap().get("Attachment")).setText("");

		return bar;
	}

	public void disableGroups() {
		withGroups = false;
	}

	/** Group attribute to support ANDRILL annotation grouping requirement */
	public void enableGroups() {
		withGroups = true;
	}

	public float getAnnotationXPosition() {
		return x_pos;
	}

	public float getAnnotationYPosition() {
		return y_pos;
	}

	protected JTextComponent getEditor() {
		return discussion;
	}

	public int getGroup() {
		return group;
	}

	public String getGroupString() {
		return ChatGroup.getGroupName(group);
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

	@Override
	public int getMarkerId() {
		return markerId;
	}

	@Override
	public int getSectionId() {
		return sectionId;
	}

	@Override
	public int getTrackId() {
		return trackId;
	}

	/**
	 * Get the current URL loaded
	 * 
	 * @return url string
	 * */
	public String getURL() {
		return url.toString();
	}

	public void goToBottom() {
		JScrollBar js = discussionPane.getVerticalScrollBar();
		js.setValue(js.getMaximum());
	}

	// Return current 'writeLocalCopy' status
	public boolean isWriteLocalCopy() {
		return write_local;
	}

	protected void makeActions() {
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
		a = new AnnotationWindow.BRAction();
		a.putValue(Action.SMALL_ICON, new ImageIcon("resources/icons/break.gif"));
		preview.getActionMap().put("InsertBR", a);
		// a.putValue(Action.NAME, "BR");

		// link
		a = new AnnotationWindow.TagAction(HTML.Tag.A, "URL", HTML.Attribute.HREF);
		a.putValue(Action.SMALL_ICON, new ImageIcon("resources/icons/link.gif"));
		preview.getActionMap().put("anchor-link", a);
		// a.putValue(Action.NAME, "Anchor Link");

		// image
		a = new AnnotationWindow.ImageAction();
		a.putValue(Action.SMALL_ICON, new ImageIcon("resources/icons/picture.gif"));
		preview.getActionMap().put("InsertIMG", a);
		// a.putValue(Action.NAME, "Image");

		// Attachments
		a = new AnnotationWindow.AttachmentAction();
		a.putValue(Action.SMALL_ICON, new ImageIcon("resources/icons/open.gif"));
		preview.getActionMap().put("Attachment", a);
		// a.putValue(Action.NAME, "Image");

	}

	public void onSave() {
		if (new_annotation) {
			// create an annotation in native scene graph
			markerId = SceneGraph.createCoreSectionMarker(trackId, sectionId, group, type, x_pos, y_pos);

			if (markerId == -1) {
				return;
			}

			// Create annotation on the region specified
			float posx = (upperLeftPoint[0] + lowerRightPoint[0]) / 2;
			float posy = (upperLeftPoint[1] + lowerRightPoint[1]) / 2;

			SceneGraph.setCoreSectionMarkerType(trackId, sectionId, markerId, MarkerType.CORE_OUTLINE_MARKER);

			SceneGraph.setCoreSectionMarkerVertex(trackId, sectionId, markerId, posx, posy, upperLeftPoint[0], upperLeftPoint[1], lowerRightPoint[0],
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

		browserBtn.setEnabled(true);
		deleteBtn.setEnabled(true);

		// freeform annotation submission
		// submitFreeFormAnnotation();

		CorelyzerApp.getApp().updateGLWindows();
	}

	public void refresh() {

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

	// Access methods of the annotation's X position
	public void setAnnotationXPosition(final float p) {
		x_pos = p;
	}

	// Access methods of the annotation's Y position
	public void setAnnotationYPosition(final float p) {
		y_pos = p;
	}

	public void setCoreName(final String aName) {
		// TODO

	}

	/** Indicate that we are editing an existing annotation */
	public void setEditExistingMode() {
		new_annotation = false;
	}

	/** Indicate that we are editing a new annotation */
	public void setEditNewMode() {
		new_annotation = true;
		discussion.setText("");
		preview.setText("");
	}

	/**
	 * Set the local file name of the annotation
	 * 
	 * @param f
	 *            Annotation filename
	 * */
	public void setFileName(final String f) {
		scratchFileName = f;
	}

	// ------------------------------------------------------------------------

	public void setFileString(final String aFile) {
		scratchFileName = aFile;
	}

	public void setGroup(final int g) {
		group = g;

		groups.setSelectedIndex(g);

		/*
		 * if(!new_annotation) { this.groups.setEnabled(false); } else {
		 * this.groups.setEnabled(true); }
		 */
	}

	// Set which annotation marker is being edited.
	@Override
	public void setMarkerId(final int id) {
		markerId = id;
	}

	/* Input numbers are accumlate depth in cm */
	@Override
	public void setRange(float ulX, float ulY, float lrX, float lrY) {
		float scaleX = SceneGraph.getCanvasDPIX(0) / 2.54f;
		float scaleY = SceneGraph.getCanvasDPIY(0) / 2.54f;

		// Track & CoreSection offsets in scene coordinates
		float trackX = SceneGraph.getTrackXPos(trackId);
		float trackY = SceneGraph.getTrackYPos(trackId);
		float sectionX = SceneGraph.getSectionXPos(trackId, sectionId);
		float sectionY = SceneGraph.getSectionYPos(trackId, sectionId);

		float xOffset = (trackX + sectionX) / scaleX;
		float yOffset = (trackY + sectionY) / scaleY;

		ulX -= xOffset;
		ulY -= yOffset;
		lrX -= xOffset;
		lrY -= yOffset;

		// Convert marked rect back to scene coord
		upperLeftPoint[0] = ulX * scaleX;
		upperLeftPoint[1] = ulY * scaleY;
		lowerRightPoint[0] = lrX * scaleX;
		lowerRightPoint[1] = lrY * scaleY;
	}

	/*
	 * private void copyFile(String src, String dst) { try { // Create channel
	 * on the source FileChannel srcChannel = new
	 * FileInputStream(src).getChannel();
	 * 
	 * // Create channel on the destination FileChannel dstChannel = new
	 * FileOutputStream(dst).getChannel();
	 * 
	 * // Copy file contents from source to destination
	 * dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
	 * 
	 * // Close the channels srcChannel.close(); dstChannel.close(); } catch
	 * (IOException e) { System.err.println("--- Exception in copying files: " +
	 * src + ", " + dst); JOptionPane.showMessageDialog(this, "Copy file " + src
	 * + "failed"); } }
	 */

	// Set which section along the given track the annotation is being
	// associated with.
	@Override
	public void setSectionId(final int id) {
		sectionId = id;
	}

	// Set which track the annotation is being associated with
	@Override
	public void setTrackId(final int id) {
		trackId = id;
	}

	public void setTrackName(final String aName) {
		// TODO

	}

	// TODO annotation generalization
	public void setType(final int t) {
		type = t;
	}

	void setupUI() {
		// SpringLayout l = new SpringLayout();
		BorderLayout l = new BorderLayout();
		getContentPane().setLayout(l);

		JPanel p = new JPanel(new BorderLayout());
		windowPanel = p;

		htmlKit = new HTMLEditorKit();
		htmlKit.getStyleSheet().addRule("body { font-family: sans-serif; margin-right: 20%;" + "margin-left: 20%; }");

		// Discussion thread panel
		discussion = new JEditorPane();
		discussion.setPreferredSize(new Dimension(570, 390));
		discussion.setEditorKit(htmlKit);
		discussion.setEditable(false);
		discussion.setText("");

		JScrollPane sp_p = new JScrollPane(discussion);
		sp_p.setBorder(BorderFactory.createTitledBorder("Discussion"));
		sp_p.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		p.add(sp_p, BorderLayout.CENTER);
		discussionPane = sp_p;

		// input panel
		JPanel editPane = new JPanel(new BorderLayout());
		preview = new JEditorPane();
		preview.setPreferredSize(new Dimension(570, 100));
		preview.setEditorKit(htmlKit);
		preview.setEditable(true);
		sp_p = new JScrollPane(preview);
		sp_p.setBorder(BorderFactory.createTitledBorder("Create new entry here: "));
		editPane.add(sp_p, BorderLayout.CENTER);

		// editor's helper action buttons
		makeActions();
		editPane.add(createToolBar(), BorderLayout.NORTH);
		p.add(editPane, BorderLayout.SOUTH);

		// position parent panel
		getContentPane().add(p, BorderLayout.CENTER);

		// action panel
		p = new JPanel();
		p.setPreferredSize(new Dimension(585, 50));
		p.setLayout(new FlowLayout());

		submitbtn = new JButton("Submit");
		closebtn = new JButton("Close");
		refreshbtn = new JButton("Refresh");
		deleteBtn = new JButton("Delete");
		browserBtn = new JButton("Open in Browser");
		browserBtn.setEnabled(false);

		submitbtn.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				onSave();
			}
		});

		deleteBtn.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				onDelete();
			}
		});

		closebtn.addActionListener(this);
		refreshbtn.addActionListener(this);
		browserBtn.addActionListener(this);

		p.add(submitbtn);
		p.add(refreshbtn);
		p.add(deleteBtn);
		p.add(closebtn);

		if (withGroups) {
			Vector<String> groupTypes = new Vector<String>();

			for (int i = 0; i < ChatGroup.getNumberOfGroups(); i++) {
				groupTypes.add(ChatGroup.getGroupName(i));
			}

			groups = new JComboBox(groupTypes);
			groups.addActionListener(this);
			p.add(groups);
		}

		p.add(browserBtn);

		getContentPane().add(p, BorderLayout.SOUTH);

		try {
			setAlwaysOnTop(true);
		} catch (SecurityException e) {
			System.out.println("-- [WARN] Could not set " + "AnnotationWindow always top " + e);
		}
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

	/**
	 * Overrides the setVisible method to properly incorporate the current time
	 * in the title
	 */
	@Override
	public void setVisible(final boolean v) {
		String user = System.getProperty("user.name");

		Date today = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy 'at' hh:mm:ss z");
		String now = formatter.format(today);

		String title = "Annotation by " + user + "@\"" + now + "\"";
		title = title + ": " + trackId + ", " + sectionId;

		setTitle(title);

		try {
			discussion.setPage(url);
			// JEditorPane p = (JEditorPane) this.getEditor();

			// #ifdef DEBUG
			// System.out.println("MyURL: " + this.url);
			// System.out.println("ContentType: " + p.getContentType());
			// #endif
		} catch (IOException ex) {
			System.err.println("IOException in loading annotation url! " + url);
		}

		if ((url != null) && !url.toString().equals("")) {
			browserBtn.setEnabled(true);
		}

		JScrollBar js = discussionPane.getVerticalScrollBar();
		js.setValue(js.getMaximum());

		discussion.updateUI();
		preview.updateUI();
		super.setVisible(v);

		if (new_annotation) {
			deleteBtn.setEnabled(false);
		}

		preview.requestFocus();
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
