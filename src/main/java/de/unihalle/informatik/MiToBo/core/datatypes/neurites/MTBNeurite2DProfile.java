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

import java.awt.geom.Point2D;
import java.util.*;
import ij.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageRGB;

/**
 * Container class for storing neurite profile data and associated additional
 * information for a single neurite region.
 * <p>
 * Objects of this type are usually generated by applying methods from class
 * {@link NeuriteMolProfExtractor2D} to neuron images. An object of this class is
 * empty per default (apart from the associated neurite region), so all data has
 * to calculated externally and then be passed to the object using its
 * set-routines.
 * 
 * 
 * @author moeller
 * @author Danny Misiak
 */
public class MTBNeurite2DProfile {

		private MTBNeurite2D neurite;

		/**
		 * Profile data along longest skeleton path.
		 */
		private double[] profile;

		/**
		 * Points of the profile (neurite region) belonging to the longest path.
		 */
		private Vector<Vector<Point2D.Double>> profilePoints;

		/**
		 * Image filled with voronoi tesselation data during profile calculation if
		 * non-null.
		 * <p>
		 * <b>Note:</b> Memory needs to be allocated externally, please use the
		 * {@link #setVoronoiImg(ImagePlus) setVoronoiImg()}-routine for passing a
		 * suitable image to the object.
		 */
		private MTBImageRGB voronoiImg;

		public MTBNeurite2DProfile() {
				this.neurite = null;
				this.profile = null;
				this.profilePoints = new Vector<Vector<Point2D.Double>>();
				this.voronoiImg = null;
		}

		/**
		 * Constructor to create a new MTBNeuriteProfile object from the given
		 * MTBRegion2D.
		 * 
		 * @param region
		 *          Neurite region the profile is calculated for.
		 */
		public MTBNeurite2DProfile(MTBNeurite2D _neurite, double[] _profile,
		    Vector<Vector<Point2D.Double>> _profilePoints, MTBImageRGB _voronoiImg) {
				this.neurite = _neurite;
				this.profile = _profile;
				this.profilePoints = _profilePoints;
				this.voronoiImg = _voronoiImg;

		}

		public MTBNeurite2D getNeurite() {
				return neurite;
		}

		public void setNeurite(MTBNeurite2D neurite) {
				this.neurite = neurite;
		}

		/**
		 * Get profile data of the neurite region.
		 * <p>
		 * <b>Attention:</b> May be null!
		 * 
		 * @return Profile data of neurite.
		 */
		public double[] getProfile() {
				return this.profile;
		}

		/**
		 * Pass profile data to the profile.
		 * 
		 * @param profileData
		 *          Data of the profile as calculated by profile extractor.
		 */
		public void setProfile(double[] profileData) {
				this.profile = profileData;
		}

		public int getProfileSize() {
				return this.profile.length;
		}

		/**
		 * Get voronoi tesselation image.
		 * <p>
		 * <b>Attention:</b> May be null!
		 * 
		 * @return Voronoi tesselation image.
		 */
		public MTBImageRGB getVoronoiImg() {
				return voronoiImg;
		}

		/**
		 * Specify an image to be filled with voronoi data.
		 * <p>
		 * <b>Attention:</b> Correct size is mandatory!
		 * 
		 * @param voronoiImg
		 *          Allocated image to be filled with voronoi data.
		 */
		public void setVoronoiImg(MTBImageRGB voronoiImg) {
				this.voronoiImg = voronoiImg;
		}

		public Vector<Vector<Point2D.Double>> getProfilePoints() {
				return profilePoints;
		}

		/**
		 * Set the points of the neurite region where the profiles are calculated
		 * from.
		 * 
		 * @param profPoints
		 *          points in the neurite region for profile calculating
		 * 
		 */
		public void setProfilePoints(Vector<Vector<Point2D.Double>> profPoints) {
				this.profilePoints = profPoints;
		}

		// /**
		// * Saves the profile data to the given file.
		// * <p>
		// * File format: <Num>\t<x-coord>\t<y-coord>\t<prof-val>
		// *
		// * @param file
		// * output file (txt format)
		// * @return true if save successful
		// */
		// public boolean saveProfileData(String file) {
		//
		// // if we have no profile, do nothing
		// if (this.profileData == null || this.skelGraph == null)
		// return false;
		//
		// try {
		// PrintStream pStream = new PrintStream(file);
		// for (int k = 0; k < this.profileData.length; ++k) {
		// Point2D.Double pathPoint =
		// this.skelGraph.getLongestPath(true).elementAt(k);
		// int px = (int) pathPoint.x;
		// int py = (int) pathPoint.y;
		// pStream.println(k + "\t" + px + "\t" + py + "\t" + this.profileData[k]);
		// }
		// pStream.close();
		// } catch (FileNotFoundException e) {
		// System.err
		// .println("Error: Could not open file " + file + " for writing!");
		// }
		// return true;
		// }
		//
		//		
}
