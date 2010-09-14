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
package corelyzer.data.graph;

import java.util.Comparator;

public class Tuple {
	private static final class DepthComparator implements Comparator<Tuple> {

		public int compare(final Tuple t1, final Tuple t2) {
			if (t1.depth < t2.depth) {
				return -1;
			} else if (t1.depth > t2.depth) {
				return 1;
			} else {
				return 0;
			}
		}
	}

	private static final class ValueComparator implements Comparator<Tuple> {

		public int compare(final Tuple t1, final Tuple t2) {
			if (t1.value < t2.value) {
				return -1;
			} else if (t1.value > t2.value) {
				return 1;
			} else {
				return 0;
			}
		}
	}

	public static Comparator<Tuple> BY_DEPTH = new DepthComparator();
	public static Comparator<Tuple> BY_VALUE = new ValueComparator();

	private float depth;

	private float value;

	public Tuple() {
		super();

		depth = 0.0f;
		value = 0.0f;
	}

	public Tuple(final float aDepth, final float aValue) {
		this();

		depth = aDepth;
		value = aValue;
	}

	public float getDepth() {
		return depth;
	}

	public float getValue() {
		return value;
	}

	@Override
	public String toString() {
		return depth + ", " + value;
	}
}
