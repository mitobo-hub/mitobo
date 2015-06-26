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

package de.unihalle.informatik.MiToBo.apps.neurites2D;

import java.awt.geom.*;
import java.util.*;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.MiToBo.core.datatypes.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageRGB;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.datatypes.neurites.MTBNeurite2DProfile;
import de.unihalle.informatik.MiToBo.core.datatypes.neurites.MTBNeurite2DProfileSet;
import de.unihalle.informatik.MiToBo.core.datatypes.neurites.MTBNeurite2DSet;
import de.unihalle.informatik.MiToBo.core.operator.*;

/**
 * The Neurite Molecular Profile Extractor 2D application offers advanced
 * functionality to extract molecular intensity profiles of multichannel
 * fluorescence images along detected neurite regions. The profiles are stored
 * in a vector of profile sets. Each set includes the profiles for one neurite
 * region over all protein stains (image channels).
 * 
 * @author Birgit Moeller
 * @author Danny Misiak
 */
@ALDAOperator(genericExecutionMode = ALDAOperator.ExecutionMode.NONE, level = Level.STANDARD, allowBatchMode = false)
public class NeuriteMolProfExtractor2D extends MTBOperator {

		// define some colors useful for result visualization
		private transient static int color_blue = ((0 & 0xff) << 16)
		    + ((0 & 0xff) << 8) + (255 & 0xff);
		private transient static int color_red = ((255 & 0xff) << 16)
		    + ((0 & 0xff) << 8) + (0 & 0xff);

		// --- input parameters ---

		private transient int width;
		private transient int height;
		private transient int sizeC;

		@Parameter(label = "Molecule Image", required = true, direction = Parameter.Direction.IN, description = "Multichannel image with fluorescence labeled molecules.", mode = ExpertMode.STANDARD, dataIOOrder = 0)
		private transient MTBImage moleculeImage = null;

		@Parameter(label = "Profile Molecule Channels", direction = Parameter.Direction.IN, required = true, description = "Channels of molecules for profile extraction.", mode = ExpertMode.STANDARD, dataIOOrder = 1)
		private Integer[] moleculeChannels = { new Integer(1), new Integer(2),
		    new Integer(3) };

		@Parameter(label = "Neurite Set", required = true, direction = Parameter.Direction.IN, description = "Set of 2D neurites.", mode = ExpertMode.STANDARD, dataIOOrder = 2)
		private transient MTBNeurite2DSet neuriteSet = null;

		// --- supplemental parameters ---

		// --- outout parameters ---

		@Parameter(label = "Profile Vector", required = false, direction = Parameter.Direction.OUT, description = "Result vector of protein profile set.")
		private transient Vector<MTBNeurite2DProfileSet> profileVector = null;

		/**
		 * Standard constructor
		 */
		public NeuriteMolProfExtractor2D() throws ALDOperatorException {
				// nothing to do here
		}

		/**
		 * Constructor to create a new neurite profile extraction operator.
		 * 
		 * @param _moleculeImage
		 *          multichannel fluorescence image with stained molecules
		 * @param _moleculeChannels
		 *          channles including molecule stains
		 * @param _neuriteSet
		 *          set of MTBNeurite2D objects
		 * @param _molecules
		 * @param _verbose
		 *          flag for standard console outputs
		 * @throws ALDOperatorException
		 */
		public NeuriteMolProfExtractor2D(MTBImage _moleculeImage,
		    Integer[] _moleculeChannels, MTBNeurite2DSet _neuriteSet, Boolean _verbose)
		    throws ALDOperatorException {
				this.moleculeImage = _moleculeImage;
				this.moleculeChannels = _moleculeChannels;
				this.neuriteSet = _neuriteSet;
				this.verbose = _verbose;
		}

		/**
		 * Custom validation of some input parameters.
		 */
		@Override
		public void validateCustom() throws ALDOperatorException {

				boolean error = false;
				for (int i = 0; i < moleculeChannels.length; i++) {
						if (moleculeChannels[i] < 1
						    || moleculeChannels[i] > moleculeImage.getSizeC()) {
								error = true;
						}
				}
				if (error) {
						throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
						    "\n>>>>>>> NeuriteMolProfExtractor2D: validation failed!"
						        + "\nMolecule channels must be in range [1, #ImageChannels].");
				}
		}

		/**
		 * Get the input image with the fluorescence labeled molecules.
		 * 
		 * @return Gray value molecule image.
		 */
		public MTBImage getMoelculeImg() {
				return this.moleculeImage;
		}

		/**
		 * Get molecule channels, used for extraction.
		 */
		public Integer[] getMoleculeChannles() {
				return moleculeChannels;
		}

		/**
		 * Get the input set of neurites.
		 * 
		 * @return Neurite set.
		 */
		public MTBNeurite2DSet getNeuriteSet() {
				return this.neuriteSet;
		}

		/**
		 * Get the image size in x-direction.
		 * 
		 * @return Image size in x.
		 */
		public int getImageSizeX() {
				return this.width;
		}

		/**
		 * Get the image size in y-direction.
		 * 
		 * @return Image size in y.
		 */
		public int getImageSizeY() {
				return this.height;
		}

		/**
		 * Get the resulting set of neurite profiles.
		 * 
		 * @return Neurite profiles.
		 */
		public Vector<MTBNeurite2DProfileSet> getProfiles() {
				return this.profileVector;
		}

		/**
		 * This method does the actual work.
		 */
		@Override
		protected void operate() throws ALDOperatorException {
				this.width = this.moleculeImage.getSizeX();
				this.height = this.moleculeImage.getSizeY();
				this.sizeC = this.moleculeImage.getSizeC();
				this.profileVector = calcProfile();
		}

		private Vector<MTBNeurite2DProfileSet> calcProfile() {

				Vector<MTBNeurite2DProfileSet> profileVector = new Vector<MTBNeurite2DProfileSet>(
				    this.sizeC);

				for (int i = 0; i < this.neuriteSet.size(); i++) {

						MTBNeuriteSkelGraph pathTree = this.neuriteSet.getElementAt(i)
						    .getNeuriteGraph();

						Vector<Vector<Point2D.Double>> featPoints = this.neuriteSet.getElementAt(
						    i).getFeaturePoints();
						Vector<Point2D.Double> secFeatPoints = new Vector<Point2D.Double>(
						    featPoints.size());
						for (int j = 0; j < featPoints.size(); j++) {
								secFeatPoints.addElement(featPoints.elementAt(j).elementAt(1));
						}

						Vector<Vector<Point2D.Double>> pathes = pathTree.getAllPaths(false);

						for (int j = 0; j < pathes.size(); j++) {
								MTBNeurite2DProfileSet profileSet = new MTBNeurite2DProfileSet();
								Vector<Point2D.Double> longPath = pathes.elementAt(j);

								/*
								 * Check, that exactly one growth cone feature is in the path for
								 * profiling.
								 */
								int count = 0;
								for (int k = 0; k < secFeatPoints.size(); k++) {
										if (longPath.contains(secFeatPoints.elementAt(k))) {
												count++;
										}
								}

								// TODO handle count > 1
								if (count != 1) {
										continue;
								}

								Vector<Point2D.Double> skelPoints = new Vector<Point2D.Double>();
								pathTree.getAllPixels(skelPoints);
								// calculate profile for path
								int[][] countMatrix = new int[this.height][this.width];
								// corresponding region points
								Vector<Point2D.Double> regPoints = this.neuriteSet.getElementAt(i)
								    .getNeuriteRegion().getPoints();
								// initialize vector for profile points
								Vector<Vector<Point2D.Double>> profPoints = new Vector<Vector<Point2D.Double>>();
								for (int k = 0; k < skelPoints.size(); ++k) {
										profPoints.add(new Vector<Point2D.Double>());
								}
								// iterate over all region points
								for (int k = 0; k < regPoints.size(); ++k) {
										int x = (int) regPoints.elementAt(k).x;
										int y = (int) regPoints.elementAt(k).y;
										double maxDist = 4 * this.width * this.width + 4 * this.height
										    * this.height;
										Vector<Integer> pIDs = new Vector<Integer>();
										int pID = 0;
										// search for closest point in skeleton...
										for (int l = 0; l < skelPoints.size(); ++l) {
												int sx = (int) skelPoints.elementAt(l).x;
												int sy = (int) skelPoints.elementAt(l).y;
												double dist = (x - sx) * (x - sx) + (y - sy) * (y - sy);
												if (dist < maxDist) {
														pID = l;
														maxDist = dist;
												}
										} // end of for-loop: search all skeleton points for closest one
										// ... and remember number of close neighbors for that point
										countMatrix[y][x]++;
										pIDs.add(pID);
										// search for additional skeleton points with same distance
										for (int l = 0; l < skelPoints.size(); ++l) {

												int sx = (int) skelPoints.elementAt(l).x;
												int sy = (int) skelPoints.elementAt(l).y;

												if ((x - sx) * (x - sx) + (y - sy) * (y - sy) == maxDist
												    && pID != l) {
														pIDs.add(l);
														countMatrix[y][x]++;
												}
										}
										// add region point to profile, i.e. to all close skeleton points
										for (int l = 0; l < pIDs.size(); ++l) {
												profPoints.elementAt(pIDs.elementAt(l)).add(
												    new Point2D.Double(x, y));
										}
								} // end of for-loop: all region points

								// eliminate skeleton points not belonging to longest path
								for (int k = 0; k < skelPoints.size(); ++k) {

										int x = (int) skelPoints.elementAt(k).x;
										int y = (int) skelPoints.elementAt(k).y;

										boolean bingo = false;
										for (int l = 0; !bingo && l < longPath.size(); ++l) {
												int px = (int) longPath.elementAt(l).x;
												int py = (int) longPath.elementAt(l).y;

												if (px == x && py == y) {
														bingo = true;
												}
										}
										/**
										 * @todo Birgit: Handling of spine points... for the moment their are
										 *       ignored!
										 */
										if (!bingo) {
												// point does not belong to longest path, but still may be a spine
												// point...
												boolean isSpinePoint = false;
												// for (int l=0; !isSpinePoint && l<spinePoints.size(); ++l) {
												// int spx= (int)spinePoints.elementAt(l).x;
												// int spy= (int)spinePoints.elementAt(l).y;
												//
												// if (x==spx && y==spy) {
												// isSpinePoint= true;
												// }
												// }
												if (!isSpinePoint) {
														profPoints.elementAt(k).clear();
												} else {
														// it is a spine point, so we distribute its pixels...
														Vector<Point2D.Double> profilePixels = profPoints.elementAt(k);
														for (int l = 0; l < profilePixels.size(); ++l) {

																// point to be redistributed
																int ppx = (int) profilePixels.elementAt(l).x;
																int ppy = (int) profilePixels.elementAt(l).y;

																double maxDist = this.width * this.width * this.height
																    * this.height;
																int pID = 0;

																// search for closest point in path
																for (int m = 0; m < longPath.size(); ++m) {

																		int lpx = (int) longPath.elementAt(m).x;
																		int lpy = (int) longPath.elementAt(m).y;

																		double dist = (ppx - lpx) * (ppx - lpx) + (ppy - lpy)
																		    * (ppy - lpy);
																		if (dist < maxDist) {
																				pID = m;
																				maxDist = dist;
																		}
																}
																// closest point is point with id "m" in longest path
																Vector<Integer> pIDs = new Vector<Integer>();
																pIDs.add(pID);

																// search for additional path points with same distance
																for (int m = 0; m < longPath.size(); ++m) {

																		int lpx = (int) longPath.elementAt(m).x;
																		int lpy = (int) longPath.elementAt(m).y;

																		if ((ppx - lpx) * (ppx - lpx) + (ppy - lpy) * (ppy - lpy) == maxDist
																		    && pID != m) {
																				pIDs.add(m);
																				countMatrix[ppy][ppx]++;
																		}
																}
																// add additional points to profile (m is index in longest
																// path!)

																for (int m = 0; m < pIDs.size(); ++m) {

																		int longPathID = pIDs.elementAt(m);

																		int lpx = (int) longPath.elementAt(longPathID).x;
																		int lpy = (int) longPath.elementAt(longPathID).y;

																		// find corresponding ID in profile list
																		for (int n = 0; n < skelPoints.size(); ++n) {

																				int skx = (int) skelPoints.elementAt(n).x;
																				int sky = (int) skelPoints.elementAt(n).y;

																				if (skx == lpx && sky == lpy) {
																						profPoints.elementAt(n).add(new Point2D.Double(ppx, ppy));
																						n = skelPoints.size();
																				}
																		}
																}
														}
														profPoints.elementAt(k).clear();
												}
										}
								}

								// sort indices in profile according to longest path
								Vector<Integer> idList = new Vector<Integer>();
								for (int k = 0; k < longPath.size(); ++k) {
										Point2D.Double lp = longPath.elementAt(k);
										boolean matchFound = false;
										for (int l = 0; !matchFound && l < skelPoints.size(); ++l) {
												Point2D.Double sp = skelPoints.elementAt(l);
												if ((int) lp.x == (int) sp.x && (int) lp.y == (int) sp.y) {
														idList.add(l);
														matchFound = true;
												}
										}
										if (!matchFound) {
												System.out.println("No corresponding point found: " + lp.x + " , "
												    + lp.y);
										}
								}
								MTBImage voronoiImg = MTBImage.createMTBImage(this.width, this.height,
								    1, 1, 1, MTBImageType.MTB_RGB);
								voronoiImg.fillWhite();
								// save some additional information:
								// for all points in skeleton, mark pixels belonging to the individual
								// points
								for (int k = 0; k < skelPoints.size(); ++k) {
										Random rand = new Random();
										int color = rand.nextInt();
										Vector<Point2D.Double> ps = profPoints.elementAt(k);
										for (int l = 0; l < ps.size(); ++l) {
												int x = (int) ps.elementAt(l).x;
												int y = (int) ps.elementAt(l).y;
												voronoiImg.putValueInt(x, y, color);
										}
								}
								// mark the skeleton
								for (int k = 0; k < skelPoints.size(); ++k) {
										Point2D.Double p = skelPoints.elementAt(k);
										voronoiImg.putValueInt((int) p.x, (int) p.y, color_red);
								}
								// mark longest path
								for (int k = 0; k < longPath.size(); ++k) {
										Point2D.Double p = longPath.elementAt(k);
										voronoiImg.putValueInt((int) p.x, (int) p.y, color_blue);
								}

								// // mark spines
								// for (int k = 0; k < spinePoints.size(); ++k) {
								// Point2D.Double p = spinePoints.elementAt(k);
								// voronoiImg.putValueInt((int) p.x, (int) p.y, color_green);
								// }

								/*
								 * Calculate the final profile from original image.
								 */
								for (int c = 0; c < moleculeChannels.length; c++) {
										// profile data along the neurite
										double[] profile = new double[idList.size()];
										for (int k = 0; k < idList.size(); ++k) {
												/*
												 * Calculate profile data.
												 */
												// get point from correct profile entry
												Vector<Point2D.Double> points = profPoints.elementAt(idList
												    .elementAt(k));

												double intensity = 0.0;
												for (int l = 0; l < points.size(); ++l) {
														int x = (int) points.elementAt(l).x;
														int y = (int) points.elementAt(l).y;
														// doppelbelegungen anteilig verreichnet aus countMatrix
														intensity = intensity
														    + this.moleculeImage.getValueDouble(x, y, 0, 0,
														        (moleculeChannels[c] - 1)) * 1.0
														    / (double) countMatrix[y][x];
												}
												// intensity is normalized by the number of used pixels
												// for the current skeleton point
												profile[k] = intensity / points.size();
										}
										/*
										 * Calculate neurite width.
										 */
										MTBNeurite2DProfile tmpProf = new MTBNeurite2DProfile(this.neuriteSet
										    .getElementAt(i), profile, profPoints, (MTBImageRGB) voronoiImg);
										profileSet.add(tmpProf);
								}
								profileVector.add(profileSet);
						}
				}
				return profileVector;
		}
}
