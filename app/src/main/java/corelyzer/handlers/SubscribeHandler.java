package corelyzer.handlers;

/**
 * ***************************************************************************
 * <p/>
 * CoreWall / Corelyzer - An Initial Core Description Tool Copyright (C) 2008
 * Julian Yu-Chung Chen Electronic Visualization Laboratory, University of
 * Illinois at Chicago
 * <p/>
 * This software is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either Version 2.1 of the License, or (at your
 * option) any later version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU Lesser Public License along with
 * this software; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 * <p/>
 * Questions or comments about CoreWall should be directed to cavern@evl.uic.edu
 * <p/>
 * ***************************************************************************
 */
public interface SubscribeHandler {
	public void onSubscribe();

	public void onSubscribe(String url);
}
