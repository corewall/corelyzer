package corelyzer.ui;

/*************************************************
 * TripleSlider
 * Description:	Slider with two thumbs that divide the bar
 *   into three parts. Relative size of each part is
 *   the value of that part, so the three values add up to 1.
 * Author: Gene Vishnevsky  Oct. 15, 1997
 *************************************************/

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.text.DecimalFormat;

import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * This class produces a slider with 2 thumbs that has 3 values.
 */
public class TripleSlider extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3845395798942259373L;
	private float scaleValue = 1.0f;
	private ChangeListener listener = null;

	private final static int THUMB_SIZE = 14;
	private final static int BUFFER = 2;
	private final static int TEXT_HEIGHT = 18;
	private final static int TEXT_BUFFER = 3;
	private final static int DEFAULT_WIDTH = 300; // 200;
	private final static int DEFAULT_HEIGHT = 15;
	/** Array that holds colors of each of the 3 parts. */
	protected Color colors[];
	private boolean enabled = true;
	private final Dimension preferredSize_;
	/* this value depends on resizing */
	protected int pixMin_, pixMax_, width_;
	private int pix1_, pix2_; // pixel position of the thumbs
	private final double values[]; // the 3 values
	/** current font of the labels. */
	protected Font font;

	/**
	 * Constructs and initializes the slider.
	 */
	public TripleSlider() {
		values = new double[3];
		colors = new Color[3];
		preferredSize_ = new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT + TEXT_HEIGHT + TEXT_BUFFER);
		font = new Font("TimesRoman", Font.PLAIN, 12);
		pixMax_ = DEFAULT_WIDTH - THUMB_SIZE - 1;
		pixMax_ = DEFAULT_WIDTH - THUMB_SIZE - 1;
		width_ = DEFAULT_WIDTH;
		setSize(width_, DEFAULT_HEIGHT + TEXT_HEIGHT /* ó+ TEXT_BUFFER */);
		setValues(0.33333, 0.33333);
		setColor(0, Color.green);
		setColor(1, Color.gray);
		setColor(2, Color.green);
		addMouseListener(new MouseAdapter() {

			@Override
			public void mousePressed(final MouseEvent evt) {
				mouseDown(evt);
			}
		});
		addMouseMotionListener(new MouseMotionAdapter() {

			@Override
			public void mouseDragged(final MouseEvent evt) {
				mouseDrag(evt);
			}
		});
	}

	public void addChangeListener(final ChangeListener l) {
		listener = l;
	}

	/**
	 * Returns color of a part.
	 * 
	 * @return current part's color.
	 */
	public Color getColor(final int part) {
		return colors[part];
	}

	public double getIntervalEnd() {
		return (getValue(0) + getValue(1)) * getScaleValue();
	}

	public double getIntervalStart() {
		return getValue(0) * this.getScaleValue();
	}

	/**
	 * Overrides the default minimumSize() method.
	 * 
	 * @return new Dimension
	 */

	@Override
	public Dimension getMinimumSize() {
		return preferredSize_;
	}

	/**
	 * Overrides the default preferredSize() method.
	 * 
	 * @return new Dimension
	 */

	@Override
	public Dimension getPreferredSize() {
		return preferredSize_;
	}

	public float getScaleValue() {
		return scaleValue;
	}

	/**
	 * Returns value for a part.
	 * 
	 * @return value for the part.
	 */
	public double getValue(final int part) {
		return values[part];
	}

	/**
	 * Does all the recalculations related to user interaction with the slider.
	 */
	protected void HandleMouse(final int x) {
		boolean leftControl = false;
		int left = pix1_, right = pix2_;
		int xmin = THUMB_SIZE;
		int xmax = width_ - THUMB_SIZE;
		// Which thumb is closer?
		if (x < pix1_ + (pix2_ - pix1_) / 2) {
			leftControl = true;
			left = x;
		} else {
			right = x;
		}
		/* verify boundaries and reconcile */
		if (leftControl) {
			if (left < xmin) {
				left = xmin;
			} else if (left > xmax - THUMB_SIZE * 2) {
				left = xmax - THUMB_SIZE * 2;
			} else {
				if (left > right - THUMB_SIZE * 2 && right < xmax) {
					// push right
					right = left + THUMB_SIZE * 2;
				}
			}
		} else {
			// right control
			if (right > xmax) {
				right = xmax;
			} else if (right < xmin + THUMB_SIZE * 2) {
				right = xmin + THUMB_SIZE * 2;
			} else {
				if (right < left + THUMB_SIZE * 2 && left > xmin) {
					// push left
					left = right - THUMB_SIZE * 2;
				}
			}
		}
		pix1_ = left;
		pix2_ = right;
		updateValues();
		repaint();
	}

	/**
	 * This method is called when the "thumb" of the slider is dragged by the
	 * user. Must be overridden to give the slider some behavior.
	 */
	public void Motion() {
	}

	/**
	 * An internal method used to handle mouse down events.
	 */
	private void mouseDown(final MouseEvent e) {
		if (enabled) {
			HandleMouse((int) e.getPoint().getX());
			Motion();
		}
	}

	/**
	 * An internal method used to handle mouse drag events.
	 */
	private void mouseDrag(final MouseEvent e) {
		if (enabled) {
			HandleMouse((int) e.getPoint().getX());
			Motion();

			if (this.listener != null) {
				listener.stateChanged(new ChangeEvent(this));
			}
		}
	}

	/**
	 * Paints the whole slider and labels.
	 */

	@Override
	public void paint(final Graphics g) {
		int width = getSize().width;
		int height = getSize().height;
		g.setColor(Color.lightGray); // bground
		g.fillRect(0, 0, width, TEXT_HEIGHT);
		g.setColor(colors[0]);
		g.fillRect(0, TEXT_HEIGHT, pix1_ - THUMB_SIZE, height - TEXT_HEIGHT);
		g.setColor(colors[1]);
		g.fillRect(pix1_ + THUMB_SIZE, TEXT_HEIGHT, pix2_ - pix1_ - THUMB_SIZE * 2, height - TEXT_HEIGHT);
		g.setColor(colors[2]);
		g.fillRect(pix2_ + THUMB_SIZE, TEXT_HEIGHT, width_ - pix2_ - THUMB_SIZE, height - TEXT_HEIGHT);
		/* draw two thumbs */
		g.setColor(Color.lightGray);
		g.fill3DRect(pix1_ - THUMB_SIZE, TEXT_HEIGHT /* + BUFFER */, THUMB_SIZE * 2 + 1, height /*- 2 * BUFFER*/- TEXT_HEIGHT, true);
		g.fill3DRect(pix2_ - THUMB_SIZE, TEXT_HEIGHT /* + BUFFER */, THUMB_SIZE * 2 + 1, height /*- 2 * BUFFER*/- TEXT_HEIGHT, true);
		g.setColor(Color.black);
		g.drawLine(pix1_, TEXT_HEIGHT + BUFFER + 1, pix1_, height - 2 * BUFFER);
		g.drawLine(pix2_, TEXT_HEIGHT + BUFFER + 1, pix2_, height - 2 * BUFFER);
		g.setFont(font);
		// center each value in the middle
		String str = render(getValue(0) * scaleValue);
		g.drawString(str, pix1_ / 2 - getFontMetrics(font).stringWidth(str) / 2, TEXT_HEIGHT - TEXT_BUFFER);
		str = render(getValue(1) * scaleValue);
		g.drawString(str, (pix2_ - pix1_) / 2 + pix1_ - getFontMetrics(font).stringWidth(str) / 2, TEXT_HEIGHT - TEXT_BUFFER);
		str = render(getValue(2) * scaleValue);
		g.drawString(str, (width_ - pix2_) / 2 + pix2_ - getFontMetrics(font).stringWidth(str) / 2, TEXT_HEIGHT - TEXT_BUFFER);
	}

	private String render(final double value) {
		DecimalFormat myF = new DecimalFormat("###,###,###.##");
		return myF.format(value);
	}

	/**
	 * This method is called by the runtime when the slider is resized.
	 */

	@Override
	public void setBounds(final int x, final int y, final int width, final int height) {
		// setBounds() is not called.
		super.setBounds(x, y, width, height);
		width_ = width;
		pixMin_ = THUMB_SIZE;
		pixMax_ = width - THUMB_SIZE - 1;
		// recompute new thumbs pixels (for the same values)
		setValues(values[0], values[1], values[2]);
		repaint();
	}

	/**
	 * Sets color of a part.
	 */
	public void setColor(final int part, final Color color) {
		colors[part] = color;
	}

	/**
	 * Enables/disables the slider.
	 */

	@Override
	public void setEnabled(final boolean flag) {
		enabled = flag;
	}

	public void setScaleValue(final float scaleValue) {
		this.scaleValue = scaleValue;
	}

	/**
	 * Sets new values of the slider. is 1 - a - b.
	 */
	public void setValues(final double a, final double b) {
		double sum_ab = a + b;
		if (sum_ab > 1. || sum_ab < 0.) {
			/* invalid input: should throw exception */
			System.out.println("invalid input");
			return;
		}
		/* call this private method */
		setValues(a, b, 1 - sum_ab);
		repaint();
	}

	private void setValues(final double a, final double b, final double c) {
		// we know the values are valid
		values[0] = a;
		values[1] = b;
		values[2] = c;
		double total = width_ - THUMB_SIZE * 4; // sum
		pix1_ = (int) (a * total) + THUMB_SIZE;
		pix2_ = (int) (b * total) + pix1_ + THUMB_SIZE * 2;
	}

	/**
	 * Overrides the default update(Graphics) method in order not to clear
	 * screen to avoid flicker.
	 */

	@Override
	public void update(final Graphics g) {
		paint(g);
	}

	private void updateValues() {
		double total = width_ - THUMB_SIZE * 4; // sum
		int a = pix1_ - THUMB_SIZE;
		int b = pix2_ - pix1_ - THUMB_SIZE * 2;
		int c = width_ - (pix2_ + THUMB_SIZE);
		values[0] = a / total;
		values[1] = b / total;
		values[2] = c / total;
	}
}
