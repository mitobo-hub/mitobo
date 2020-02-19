/*
 * This file is part of MiToBo, the Microscope Image Analysis Toolbox.
 *
 * Copyright (C) 2010
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

package de.unihalle.informatik.MiToBo.core.datatypes.neurites;

import java.awt.Color;
import java.util.Vector;

import de.unihalle.informatik.Alida.operator.ALDData;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageRGB;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;

/**
 * Class to represent a set of MTBNeurite2D objects.
 * 
 * @see MTBNeurite2D
 * 
 * @author Danny Misiak
 * 
 */
public class MTBNeurite2DSet extends ALDData {

		/**
		 * The current set of neurites.
		 */
		private Vector<MTBNeurite2D> neuriteSet;

		/**
		 * Constructor to create a new set of neurites.
		 */
		public MTBNeurite2DSet() {
				this.neuriteSet = new Vector<MTBNeurite2D>();
		}

		/**
		 * Appends the specified element to the end of this list.
		 * 
		 * @param neurite
		 *          new neurite to add
		 */
		public void add(MTBNeurite2D neurite) {
				this.neuriteSet.addElement(neurite);
		}

		/**
		 * Inserts the specified element at the specified position in this list.
		 * Shifts the element currently at that position (if any) and any subsequent
		 * elements to the right (adds one to their indices).
		 * 
		 * @param index
		 *          index for the new element
		 * @param neurite
		 *          new neurite to set at the specified index
		 */
		public void addElementAt(int index, MTBNeurite2D neurite) {
				this.neuriteSet.add(index, neurite);
		}

		/**
		 * Returns the element at the specified position in this list.
		 * 
		 * @param index
		 *          index of the element to get
		 * @return Element at specified index.
		 */
		public MTBNeurite2D getElementAt(int index) {
				return (this.neuriteSet.get(index));
		}

		/**
		 * Replaces the element at the specified position in this list with the
		 * specified element.
		 * 
		 * @param index
		 *          position to set the neurite
		 * @param neurite
		 *          neurite to set
		 */
		public void setElementAt(int index, MTBNeurite2D neurite) {
				this.neuriteSet.set(index, neurite);
		}

		/**
		 * Returns true if this collection contains no elements.
		 * 
		 * @return True if list is empty.
		 */
		public boolean isEmpty() {
				return (this.neuriteSet.isEmpty());
		}

		/**
		 * Removes the element at the specified position in this list. Shifts any
		 * subsequent elements to the left (subtracts one from their indices). Returns
		 * the element that was removed from the list.
		 * 
		 * @param index
		 *          index of the element to remove
		 * @return Element at specified index.
		 */
		public MTBNeurite2D removeElementAt(int index) {
				return (this.neuriteSet.remove(index));
		}

		/**
		 * Returns the number of elements in this list.
		 * 
		 * @return Number of elements.
		 */
		public int size() {
				return (this.neuriteSet.size());
		}

		/**
		 * Visualize the neurites skeleton graphs and regions. Edge points colored in
		 * bright green, END nodes colored in red, BRANCH nodes colored in blue and
		 * START nodes colored in green. The features (border lines) are colored in
		 * yellow. Regions can be shown in special colors via an 3-element int array.
		 * 
		 * @param regionColor
		 *          neurite region color in 3-element RGB array red[0], green[1],
		 *          blue[2], if null no regions are drawn
		 * 
		 * @return stack of neurites of the current neurite set.
		 */
		public MTBImage getNeuriteStack(Color regionColor) {

				int width = this.getElementAt(0).getNeuriteGraph().getWidth();
				int height = this.getElementAt(0).getNeuriteGraph().getHeight();

				MTBImage neuriteStack = MTBImage.createMTBImage(width, height, 1, 1,
				    this.neuriteSet.size(), MTBImageType.MTB_RGB);
				neuriteStack.setTitle("NeuriteSet-NeuriteStack");
				for (int i = 0; i < this.neuriteSet.size(); i++) {
						MTBImageRGB tmpImg = this.neuriteSet.elementAt(i).toImage(regionColor);
						neuriteStack.setImagePart(tmpImg, 0, 0, 0, 0, i);
				}
				return (neuriteStack);
		}

		/**
		 * Visualize the neurites skeleton graphs and regions. Edge points colored in
		 * bright green, END nodes colored in red, BRANCH nodes colored in blue and
		 * START nodes colored in green. The features (border lines) are colored in
		 * yellow. Regions can be shown in special colors via an 3-element int array.
		 * 
		 * @param regionColor
		 *          neurite region color in 3-element RGB array red[0], green[1],
		 *          blue[2], if null no regions are drawn
		 * 
		 * @return image of neurites of the current neurite set.
		 */
		public MTBImageRGB getNeuriteImage(Color regionColor) {

				int width = 500;
				int height = 500;

				if (this.size() > 0) {
						width = this.getElementAt(0).getNeuriteGraph().getWidth();
						height = this.getElementAt(0).getNeuriteGraph().getHeight();
				}

				MTBImageRGB neuriteImage = (MTBImageRGB) MTBImage.createMTBImage(width,
				    height, 1, 1, 1, MTBImageType.MTB_RGB);
				neuriteImage.fillWhite();
				neuriteImage.setTitle("Detected Neurites");
				for (int i = 0; i < this.neuriteSet.size(); i++) {
						neuriteImage = this.neuriteSet.elementAt(i).toImage(neuriteImage,
						    regionColor);

				}
				return (neuriteImage);
		}
}
