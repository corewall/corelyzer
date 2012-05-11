package corelyzer.ui;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicDirectoryModel;
import javax.swing.plaf.metal.MetalFileChooserUI;
import javax.swing.plaf.basic.BasicFileChooserUI;

// 1/31/2012 brg: JFileChooser doesn't respect numeric values when sorting filenames, which
// makes loading images a pain. Furthermore, there's no trivial way to change the sort order!
// Found and adapted this rather convoluted solution for Windows. (Using Quaqua on Mac.)

public class LoadImageChooser extends JFileChooser {
	static final long serialVersionUID = 3616241660235093043L;

	// needed to use the ui that uses the sorting model
	public LoadImageChooser()
	{
		super();
		setUI(new LoadImageChooserUI(this));
	}

	// class that does the actual sorting
	public static class LoadImageDirectoryModel extends BasicDirectoryModel {
		public LoadImageDirectoryModel(JFileChooser jfc) {
			super(jfc);
		}

		// @Override
		public void sort(Vector<? extends File> v) {
			// override sort to use my own sorting method
			Collections.sort(v, new AlphanumComparator.FileAlphanumComparator());
		}
	}

	// need to extend MetalFileChooserUI as BasicFileChooserUI results in empty frame
	private static class LoadImageChooserUI extends MetalFileChooserUI {
		private LoadImageDirectoryModel _dir_model;

		public LoadImageChooserUI(JFileChooser jfc) {
			super(jfc);
		}

		@Override
		public void createModel() {
			_dir_model = new LoadImageDirectoryModel(this.getFileChooser());
		}

		@Override
		public BasicDirectoryModel getModel() {
			return _dir_model;
		}
	}

	public static void main(String[] args) {
		// brg 1/31/2012: Make the file chooser look Windows-y rather than ugly "metal" Java GUI look
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}

		LoadImageChooser lic = new LoadImageChooser();
		LoadImageDirectoryModel bdm = new LoadImageDirectoryModel(lic);

		try {
			final Field model = BasicFileChooserUI.class.getDeclaredField("model");
			if (model != null)
			{
				model.setAccessible(true);
				model.set(lic.getUI(), bdm);
			}
		} catch (NoSuchFieldException nsfe) {} catch (IllegalAccessException iae) {}


		JFrame win = new JFrame("Sorting Chooser SSCCE");
		win.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		win.add(lic);
		win.pack();

		win.setVisible(true);
	}
}
