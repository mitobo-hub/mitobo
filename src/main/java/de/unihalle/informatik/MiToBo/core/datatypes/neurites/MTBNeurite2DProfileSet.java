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

package de.unihalle.informatik.MiToBo.core.datatypes.neurites;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.LinkedList;

import de.unihalle.informatik.Alida.operator.ALDData;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageRGB;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;

/**
 * A set of neurite profiles organized as linked list. Each profile is of type
 * MTBNeuriteProfile.
 * 
 * @see MTBNeurite2DProfile
 * 
 * 
 * @author Danny Misiak
 */
public class MTBNeurite2DProfileSet extends ALDData {
		/**
		 * The current list of the neurite profiles.
		 */
		private LinkedList<MTBNeurite2DProfile> neuriteProfiles;

		/**
		 * Constructor to create a new set of neurite profiles.
		 */
		public MTBNeurite2DProfileSet() {
				this.neuriteProfiles = new LinkedList<MTBNeurite2DProfile>();
		}

		/**
		 * Appends the specified element to the end of this list.
		 * 
		 * @param profile
		 *          new profile to add
		 */
		public void add(MTBNeurite2DProfile profile) {
				this.neuriteProfiles.add(profile);
		}

		/**
		 * Inserts the specified element at the specified position in this list.
		 * Shifts the element currently at that position (if any) and any subsequent
		 * elements to the right (adds one to their indices).
		 * 
		 * @param index
		 *          index for the new element
		 * @param profile
		 *          new profile to set at the specified index
		 */
		public void addElementAt(int index, MTBNeurite2DProfile profile) {
				this.neuriteProfiles.add(index, profile);
		}

		/**
		 * Inserts the specified element at the beginning of this list.
		 * 
		 * @param profile
		 *          profile to add at first position
		 */
		public void addFirst(MTBNeurite2DProfile profile) {
				this.neuriteProfiles.addFirst(profile);
		}

		/**
		 * Appends the specified element to the end of this list.
		 * 
		 * @param profile
		 *          to add at last position
		 */
		public void addLast(MTBNeurite2DProfile profile) {
				this.neuriteProfiles.addLast(profile);
		}

		/**
		 * Returns the element at the specified position in this list.
		 * 
		 * @param index
		 *          index of the element to get
		 * @return Element at specified index.
		 */
		public MTBNeurite2DProfile getElementAt(int index) {
				return (this.neuriteProfiles.get(index));
		}

		/**
		 * Replaces the element at the specified position in this list with the
		 * specified element.
		 * 
		 * @param index
		 *          position to set the profile
		 * @param profile
		 *          profile to set
		 */
		public void setElementAt(int index, MTBNeurite2DProfile profile) {
				this.neuriteProfiles.set(index, profile);
		}

		/**
		 * Returns the first element in this list.
		 * 
		 * @return First element of the list.
		 */
		public MTBNeurite2DProfile getFirst() {
				return (this.neuriteProfiles.getFirst());
		}

		/**
		 * Returns the last element in this list.
		 * 
		 * @return Last element of the list
		 */
		public MTBNeurite2DProfile getLast() {
				return (this.neuriteProfiles.getLast());
		}

		/**
		 * Returns true if this collection contains no elements.
		 * 
		 * @return True if list is empty.
		 */
		public boolean isEmpty() {
				return (this.neuriteProfiles.isEmpty());
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
		public MTBNeurite2DProfile removeElementAt(int index) {
				return (this.neuriteProfiles.remove(index));
		}

		/**
		 * Removes and returns the first element from this list.
		 * 
		 * @return Removed first element.
		 */
		public MTBNeurite2DProfile removeFirst() {
				return (this.neuriteProfiles.removeFirst());
		}

		/**
		 * Removes and returns the last element from this list.
		 * 
		 * @return Removed last element.
		 */
		public MTBNeurite2DProfile removeLast() {
				return (this.neuriteProfiles.removeLast());
		}

		/**
		 * Returns the number of elements in this list.
		 * 
		 * @return Number of elements.
		 */
		public int size() {
				return (this.neuriteProfiles.size());
		}

		/**
		 * Saves the given profile data list with N profiles to the given file.
		 * <p>
		 * Each profile is written to a single column. Each column will have a
		 * different number of entries according to the different lengths of the
		 * profiles.
		 * <p>
		 * File format: <prof-val-1.1>\t<prof-val-2.1>\t...\t<prof-val-N.1> ...
		 * <prof-val-1.M>\t<prof-val-2.M>\t...\t<prof-val-N.M>
		 * 
		 * @param profs
		 *          List of neurite profiles.
		 * @param file
		 *          Output file (txt format).
		 * @return True if saving is successful.
		 */
		public boolean saveProfileSet(String file) {

				// get length of longest profile
				int maxProfLength = 0;
				for (int i = 0; i < this.neuriteProfiles.size(); ++i) {
						if (this.neuriteProfiles.get(i).getProfileSize() > maxProfLength)
								maxProfLength = this.neuriteProfiles.get(i).getProfileSize();
				}

				// System.out.println(profile.length + ", " + longPath.size() + ", " +
				// skelPoints.s);
				try {
						// open the output stream
						PrintStream pStream = new PrintStream(file);

						// iterate over all profile entries
						for (int j = 0; j < maxProfLength; ++j) {
								for (int k = 0; k < this.neuriteProfiles.size(); ++k) {

										// get current profile
										MTBNeurite2DProfile cp = this.neuriteProfiles.get(k);

										// if we have no profile or no data, do nothing
										if (cp.getProfile() == null || cp.getProfileSize() <= j) {
												pStream.print("\t");
										} else {
												// write the data
												pStream.print(cp.getProfile()[j]);
												if (k != this.neuriteProfiles.size() - 1)
														pStream.print("\t");
										}
								}
								pStream.println();
						}
						System.out.println("done writing to " + file);
						pStream.close();
				} catch (FileNotFoundException e) {
						System.err
						    .println("Error: Could not open file " + file + " for writing!");
						return false;
				}
				return true;
		}

		/**
		 * Get stack of voronoi tesselation images. Each voronoi tesselation of a
		 * neurite is saved in the stack.
		 * 
		 * @return Voronoi tesselations for each neurite of the current profile set.
		 */
		public MTBImage getVoronoiStack() {
				MTBImageRGB voronoiImg = this.getElementAt(0).getVoronoiImg();
				int width = voronoiImg.getSizeX();
				int height = voronoiImg.getSizeY();
				MTBImage voronoiStack = MTBImage.createMTBImage(width, height, 1, 1,
				    this.neuriteProfiles.size(), MTBImageType.MTB_RGB);
				voronoiStack.setTitle("NeuriteProfileSet-VoronoiStack");
				for (int i = 0; i < this.neuriteProfiles.size(); i++) {
						MTBImageRGB tmpImg = this.getElementAt(i).getVoronoiImg();
						voronoiStack.setImagePart(tmpImg, 0, 0, 0, 0, i);
				}
				return (voronoiStack);
		}
}
