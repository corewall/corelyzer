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

package corelyzer.data;

/**
 * Static enumeration tags for length units
 * <p/>
 * Units: CM, M, MM, INCH, FOOT, YARD
 */

public class UnitLength {
	public static final int CM = 0;
	public static final int M = 1;
	public static final int MM = 2;
	public static final int INCH = 3;
	public static final int FOOT = 4;
	public static final int YARD = 5;
	
	public static float getUnitScale(int depthUnit) {
		float unitScale = 0.0f;
		switch ( depthUnit ) {
			case UnitLength.CM:
				unitScale = 0.01f;
				break;
				
			case UnitLength.M:
				unitScale = 1.0f;
				break;
				
			case UnitLength.FOOT:
				unitScale = 0.3048f;
				break;
				
			case UnitLength.INCH:
				unitScale = 0.0254f;
				break;
				
			case UnitLength.MM:
				unitScale = 0.001f;
				break;
				
			case UnitLength.YARD:
				unitScale = 0.9144f;
				break;
				
			default:
				unitScale = 1.0f;
		}

		return unitScale;
	}
}
