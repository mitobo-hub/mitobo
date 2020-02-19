/*
 * This file is part of MiToBo, the Microscope Image Analysis Toolbox.
 *
 * Copyright (C) 2010 - @YEAR@
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Fore more information on MiToBo, visit
 *
 *    http://www.informatik.uni-halle.de/mitobo/
 *
 */

package de.unihalle.informatik.MiToBo.gui;

import ij.ImageListener;
import ij.ImagePlus;

import java.util.Vector;


/**
 * A class that synchronizes viewing of images for easier comparison. Implemented is synchronous scrolling of stacks.
 * @author Oliver Gress
 *
 */
public class SynchronizedImageWindows implements ImageListener {

	private Vector<ImagePlus> images;
	
	/**
	 * 
	 */
	public SynchronizedImageWindows() {
		this.images = new Vector<ImagePlus>(2);
		ImagePlus.addImageListener(this);
	}
	
	public void addImage(ImagePlus img) {
		if (!this.images.contains(img)) {
			this.images.add(img);
		}
	}
	
	public void removeImage(ImagePlus img) {
		this.images.remove(img);
		
	}
	
	
	@Override
	public void imageClosed(ImagePlus arg0) {
		this.removeImage(arg0);
	}

	@Override
	public void imageOpened(ImagePlus arg0) {

	}

	@Override
	public void imageUpdated(ImagePlus arg0) {
		
		
		
		int vecidx = this.images.indexOf(arg0);
		
		if (vecidx != -1) {
			
			int slice = arg0.getCurrentSlice();
			for (int i = 0; i < this.images.size(); i++) {
				if (i != vecidx && this.images.get(i) != null && this.images.get(i).getCurrentSlice() != slice) {
					this.images.get(i).setPosition(slice);

				}		
			}
		}
	}

}
