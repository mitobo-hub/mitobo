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

package de.unihalle.informatik.MiToBo.core.datatypes.images;

import java.util.Vector;

import de.unihalle.informatik.Alida.operator.ALDData;


/**
 * A set of MTBImages stored in a vector. 
 * @author gress
 *
 */
public class MTBImageSet extends ALDData {

	Vector<MTBImage> images;
	
	
	public MTBImageSet() {
		this.images = new Vector<MTBImage>();
	}
	
	public MTBImageSet(MTBImage[] images) {
		this.images = new Vector<MTBImage>(images.length);
		
		for (MTBImage img : images) {
			this.images.add(img);
		}
	}
	
	
	public void add(MTBImage img) {
		this.images.add(img);
	}
	
	public int getNumOfImages() {
		return this.images.size();
	}
	
	public MTBImage get(int i) {
		return this.images.get(i);
	}
	
	public void remove(int i) {
		this.images.remove(i);
	}
	
	public MTBImage[] toArray() {
		MTBImage[] imgs = new MTBImage[this.images.size()];
		imgs = this.images.toArray(imgs);
		return imgs;
	}
	
}
