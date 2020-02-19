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
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Vector;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.operator.ALDData;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBNeuriteSkelGraph;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageRGB;
import de.unihalle.informatik.MiToBo.segmentation.regions.labeling.LabelComponentsSequential;

/**
 * Class for neurite objects. An neurite consists of its neurite skeleton graph
 * (NSG), at least two feature lines, which are to borderlines to separate the
 * soma from the neurite shaft and the neurite shaft from the growth cone(s).
 * And there is a maximum spine (filopodia-like protrusions) length value that
 * indicates, which paths of the neurite are spines.
 * 
 * TODO updated comments about measurements
 * 
 * @see MTBNeuriteSkelGraph
 * 
 * @author Danny Misiak
 * 
 */

public class MTBNeurite2D extends ALDData {

		/**
		 * Neurite skeleton Graph (NSG) of the current neurite.
		 */
		private MTBNeuriteSkelGraph neuriteGraph;
		/**
		 * Features of the single neurite branches. Each branch has at least one
		 * feature at the starting point of the neurite and one feature to separate
		 * the neurite shaft from the growth cone.
		 */
		private Vector<Vector<Point2D.Double>> featurePoints;
		/**
		 * Borderlines that separate the neurite from the soma and additionally
		 * separate the neurite from its growth cone.
		 */
		private Vector<Vector<Line2D.Double>> featureLines;
		/**
		 * Neurite corresponding region as MTBRegion2D object.
		 */
		private MTBRegion2D neuriteRegion;
		/**
		 * Neurite corresponding growth cone regions as MTBRegion2D objects.
		 */
		private MTBRegion2DSet growthCones;
		/**
		 * Neurite corresponding neurite shaft region as MTBRegion2D object.
		 */
		private MTBRegion2D neuriteShaft;
		/**
		 * Given maximum lengths for definition of spines. If an end point of the
		 * MTBNeuriteSkelGraph has an incoming edge smaller than the given
		 * spineLength, the path is labeled as spine.
		 */
		private int maxSpineLength;
		/**
		 * Lengths of the single branches of a neurite shaft.
		 */
		private int[] neuriteShaftLengths;

		/**
		 * Average widths of the whole neurite, the neurite shaft and the growth cone.
		 */
		private Vector<Vector<Double>> neuriteWidths;

		/**
		 * Constructor to generate a new neurite object.
		 * 
		 * @param _neuriteGraph
		 *          NSG of the current neurite
		 * @param _featurePoints
		 *          vector of feature points
		 * @param _featureLines
		 *          vector of feature lines
		 * @param _neuriteRegion
		 *          2D region of the neurite
		 * @param _maxSpineLength
		 *          maximum spine (filopodia-like protrusions) length
		 * @param _neuriteShaftLengths
		 *          array of neurite branch lengths
		 * @param _neuriteWidths
		 *          average widths of neurite branches, shafts and growth cones for
		 *          the current neurite object
		 */
		public MTBNeurite2D(MTBNeuriteSkelGraph _neuriteGraph,
		    Vector<Vector<Point2D.Double>> _featurePoints,
		    Vector<Vector<Line2D.Double>> _featureLines, MTBRegion2D _neuriteRegion,
		    int _maxSpineLength, int[] _neuriteShaftLengths,
		    Vector<Vector<Double>> _neuriteWidths) {

				this.neuriteGraph = _neuriteGraph;
				this.featurePoints = _featurePoints;
				this.featureLines = _featureLines;
				this.neuriteRegion = _neuriteRegion;
				this.maxSpineLength = _maxSpineLength;
				this.neuriteShaftLengths = _neuriteShaftLengths;
				this.neuriteWidths = _neuriteWidths;
				/*
				 * Update the neurite shaft and growth cone regions, depending on the
				 * current regions and features.
				 */
				updateRegions();
		}

		/**
		 * Get neurite corresponding skeleton graph.
		 * 
		 * @return Neurite skeleton graph.
		 */
		public MTBNeuriteSkelGraph getNeuriteGraph() {
				return this.neuriteGraph;
		}

		/**
		 * Get neurite corresponding feature points of borders between soma and
		 * neurite shafts and neurite shafts and growth cones.
		 * 
		 * @return Feature points.
		 */
		public Vector<Vector<Point2D.Double>> getFeaturePoints() {
				return this.featurePoints;
		}

		/**
		 * Get first feature of neurite corresponding feature points from borders
		 * between soma and neurite shafts.
		 * 
		 * @return First feature points.
		 */
		public Point2D.Double getfirstFeatureAt(int index) {
				return this.featurePoints.elementAt(index).elementAt(0);
		}

		/**
		 * Get second feature of neurite corresponding feature points from borders
		 * between neurite shafts and growth cones.
		 * 
		 * @return Second feature points.
		 */
		public Point2D.Double getsecondFeatureAt(int index) {
				return this.featurePoints.elementAt(index).elementAt(1);
		}

		/**
		 * Get neurite corresponding feature lines of borders between soma and neurite
		 * shafts and neurite shafts and growth cones.
		 * 
		 * @return Feature points.
		 */
		public Vector<Vector<Line2D.Double>> getFeatureLines() {
				return this.featureLines;
		}

		/**
		 * Get neurite corresponding region.
		 * 
		 * @return Neurite region.
		 */
		public MTBRegion2D getNeuriteRegion() {
				return this.neuriteRegion;
		}

		/**
		 * Add data to the current neurite object.
		 * 
		 * @param p
		 *          vector of feature points to add
		 * @param f
		 *          vector of feature lines to add
		 * @param n
		 *          array of neurite branch lengths to add
		 * @param w
		 *          vector of average widths to add
		 * @return True if data was added successfully.
		 */
		public boolean addData(Vector<Vector<Point2D.Double>> p,
		    Vector<Vector<Line2D.Double>> f, int[] n, Vector<Vector<Double>> w) {
				if (p.size() == f.size() && p.size() == n.length) {
						for (int i = 0; i < p.size(); i++) {
								this.featurePoints.addElement(p.elementAt(i));
								this.featureLines.addElement(f.elementAt(i));
						}
						int[] tmpNeuriteShaftLengths = new int[this.neuriteShaftLengths.length
						    + n.length];
						for (int i = 0; i < this.neuriteShaftLengths.length; i++) {
								tmpNeuriteShaftLengths[i] = this.neuriteShaftLengths[i];
						}
						for (int i = 0; i < n.length; i++) {
								tmpNeuriteShaftLengths[this.neuriteShaftLengths.length + i] = n[i];
						}
						this.neuriteShaftLengths = new int[tmpNeuriteShaftLengths.length];
						for (int i = 0; i < tmpNeuriteShaftLengths.length; i++) {
								this.neuriteShaftLengths[i] = tmpNeuriteShaftLengths[i];
						}
						for (int i = 0; i < w.size(); i++) {
								this.neuriteWidths.addElement(w.elementAt(i));
						}
						/*
						 * Update the neurite shaft and growth cone regions, depending on the
						 * current regions and features.
						 */
						updateRegions();
						return true;
				} else {
						return false;
				}
		}

		/**
		 * Update neurite shaft and growth cone regions, due to changes of this
		 * regions by adding features or something else.
		 */
		public void updateRegions() {
				// temporary image to separate the binary neurite regions
				MTBImageByte tmpByteImg = null;
				try {
						tmpByteImg = (MTBImageByte) this.neuriteRegion.toMTBImageByte(null,
						    this.neuriteGraph.getWidth(), this.neuriteGraph.getHeight());
				} catch (ALDOperatorException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
				} catch (ALDProcessingDAGException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
				}
				// draw feature lines to separate the binary regions
				for (int i = 0; i < this.featureLines.size(); i++) {
						this.drawLine2D(this.getFeatureLines().elementAt(i).elementAt(1), this
						    .getFeaturePoints().elementAt(i).elementAt(1), tmpByteImg);
				}
				// label separated regions
				LabelComponentsSequential LCS = null;
				MTBRegion2DSet regions = null;
				try {
						LCS = new LabelComponentsSequential(tmpByteImg, true);
						LCS.runOp();
						regions = LCS.getResultingRegions();
						// LCS.getLabelImage().show();
				} catch (ALDOperatorException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
				} catch (ALDProcessingDAGException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
				}
				// separate neurite shaft from growth cones regions
				for (int i = 0; i < regions.size(); i++) {
						MTBRegion2D tmpRegion = regions.elementAt(i);
						// only neurite shaft region includes the skeleton graph star node
						Point2D.Double tmpPoint = this.neuriteGraph.getStartNode().getData();
						if (tmpRegion.contains(tmpPoint)) {
								this.neuriteShaft = tmpRegion.clone();
								regions.removeElementAt(i);
								if (i > 0) {
										i--;
								} else {
										i = 0;
								}
						} else if (tmpRegion.getArea() <= 16) {
								regions.removeElementAt(i);
								if (i > 0) {
										i--;
								} else {
										i = 0;
								}
						}
				}
				this.growthCones = regions.clone();
		}

		/**
		 * Get regions of the separated growth cones.
		 * 
		 * @return Growth cone regions.
		 */
		public MTBRegion2DSet getGrowthConeRegions() {
				return growthCones;
		}

		/**
		 * Get neurite shaft region (without growth cone).
		 * 
		 * @return Neurite shaft region.
		 */
		public MTBRegion2D getNeuriteShaftRegion() {
				return neuriteShaft;
		}

		/**
		 * Get area of neurite shaft region (without growth cone).
		 * 
		 * @return Neurite shaft area in pixel.
		 */
		public int getNeuriteShaftArea() {
				return neuriteShaft.getArea();
		}

		/**
		 * Get areas of growth cone regions.
		 * 
		 * @return Growth cone areas in pixel.
		 */
		public int[] getConeAreas() {
				int[] areas = new int[growthCones.size()];
				for (int i = 0; i < growthCones.size(); i++) {
						MTBRegion2D region = growthCones.elementAt(i);
						areas[i] = region.getArea();
				}
				return areas;
		}

		/**
		 * Get roundness of growth cone regions, 1 means a perfect circle.
		 * 
		 * @return Growth cone roundness.
		 */
		public double[] getConeRoundness() {
				double[] round = new double[growthCones.size()];
				for (int i = 0; i < growthCones.size(); i++) {
						MTBRegion2D region = growthCones.elementAt(i);
						try {
								round[i] = region.getCorrCircularity();
						} catch (ALDOperatorException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
						} catch (ALDProcessingDAGException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
						}
				}
				return round;
		}

		/**
		 * Get number of spines (filopodia-like protrusions) per growth cones.
		 * 
		 * @return Growth cone spine count.
		 */
		public int[] getConeSpineCount() {
				int[] spineCount = new int[growthCones.size()];
				Vector<Vector<Point2D.Double>> spines = this.neuriteGraph.getSpinePaths();
				Vector<Point2D.Double> spinePoints = new Vector<Point2D.Double>(spines
				    .size());
				for (int i = 0; i < spines.size(); i++) {
						spinePoints.addElement(spines.elementAt(i).lastElement());
				}
				for (int i = 0; i < growthCones.size(); i++) {
						MTBRegion2D region = growthCones.elementAt(i);
						for (int j = 0; j < spinePoints.size(); j++) {
								if (region.contains(spinePoints.elementAt(j))) {
										spineCount[i]++;
								}
						}
				}
				return spineCount;
		}

		/**
		 * Get length to define a neurite branch as spine (filopodia-like
		 * protrusions).
		 * 
		 * @return Spine length.
		 */
		public int getDefinedSpineLength() {
				return this.maxSpineLength;
		}

		/**
		 * Get lengths of neurite branches along the complete neurite skeleton graph.
		 * 
		 * @return Neurite branch lengths.
		 */
		public int[] getBranchLengths() {
				Vector<Vector<Point2D.Double>> paths = this.neuriteGraph.getAllPaths(false);

				int[] lengths = new int[this.featurePoints.size()];

				for (int m = 0; m < this.featurePoints.size(); m++) {
						Point2D.Double fPoint = this.getsecondFeatureAt(m);
						for (int k = 0; k < paths.size(); k++) {
								Vector<Point2D.Double> tmpPath = paths.elementAt(k);

								if (tmpPath.contains(fPoint)) {
										lengths[m] = tmpPath.size();
										k = paths.size();
								}
						}
				}
				return (lengths);
		}

		/**
		 * Get neurite shaft lengths of all branches without growth cone areas.
		 * 
		 * @return Neurite shaft lengths.
		 */
		public int[] getShaftLengths() {
				return this.neuriteShaftLengths;
		}

		/**
		 * Get length of the growth cone region along the neurite skeleton graph
		 * without neurite shaft areas.
		 * 
		 * @return Growth cone lengths.
		 */
		public int[] getConeLengths() {
				int[] gcLengths = new int[this.neuriteShaftLengths.length];
				int[] nbLengths = this.getBranchLengths();
				for (int i = 0; i < nbLengths.length; i++) {
						gcLengths[i] = (nbLengths[i] - this.neuriteShaftLengths[i]);
				}
				return gcLengths;
		}

		/**
		 * Get average widths of each neurite branch, including the average widths of
		 * the whole branches, the shafts and the growth cones.
		 * 
		 * @return Average widths.
		 */
		public Vector<Vector<Double>> getNeuriteWidths() {
				return this.neuriteWidths;
		}

		/**
		 * Get average widths of each complete neurite branch.
		 * 
		 * @return Average branch widths.
		 */
		public double[] getAvgNeuriteWidths() {
				double[] widths = new double[this.neuriteWidths.size()];
				for (int i = 0; i < widths.length; i++) {
						widths[i] = this.neuriteWidths.elementAt(i).elementAt(0);
				}
				return widths;
		}

		/**
		 * Get average widths of each neurite shaft.
		 * 
		 * @return Average shaft widths.
		 */
		public double[] getAvgShaftWidths() {
				double[] widths = new double[this.neuriteWidths.size()];
				for (int i = 0; i < widths.length; i++) {
						widths[i] = this.neuriteWidths.elementAt(i).elementAt(1);
				}
				return widths;
		}

		/**
		 * Get average widths of each neurite growth cone.
		 * 
		 * @return Average growth cone widths.
		 */
		public double[] getAvgConeWidths() {
				double[] widths = new double[this.neuriteWidths.size()];
				for (int i = 0; i < widths.length; i++) {
						widths[i] = this.neuriteWidths.elementAt(i).elementAt(2);
				}
				return widths;
		}

		/**
		 * Get number of neurite branches.
		 */
		public int getBrancheCount() {
				return (this.neuriteGraph.getBranchNodes().size());
		}

		/**
		 * Get number of spines (filopodia-like protrusions).
		 */
		public int getSpineCount() {
				return (this.neuriteGraph.getSpinePaths().size());
		}

		/**
		 * Get number of end points.
		 */
		public int getEndCount() {
				return (this.neuriteGraph.getEndNodes().size());
		}

		/**
		 * Visualize the neurites skeleton graph as RGB image. Edge points colored in
		 * bright green, END nodes colored in red, BRANCH nodes colored in blue and
		 * START nodes colored in green. The features (border lines) are colored in
		 * yellow.
		 * 
		 * @param regionColor
		 *          neurite region color in 3-element RGB array red[0], green[1],
		 *          blue[2], if null no regions are drawn
		 * 
		 * @return RGB labeled neurite image.
		 */
		public MTBImageRGB toImage(Color regionColor) {
				return toImage(null, regionColor);
		}

		/**
		 * Visualize the neurites skeleton graph in the given RGB image. Edge points
		 * colored in bright green, END nodes colored in red, BRANCH nodes colored in
		 * blue and START nodes colored in green. The features (border lines) are
		 * colored in yellow. Regions can be shown in special colors via an 3-element
		 * int array.
		 * 
		 * @param rgbImage
		 *          image to draw neurite into
		 * @param regionColor
		 *          neurite region color in 3-element RGB array red[0], green[1],
		 *          blue[2], if null no regions are drawn
		 * 
		 * @return RGB labeled neurite image.
		 */

		public MTBImageRGB toImage(MTBImageRGB rgbImage, Color regionColor) {
				if (rgbImage == null) {
						rgbImage = this.neuriteGraph.toImage();
				} else {
						rgbImage = this.neuriteGraph.toImage(rgbImage);
				}

				// int color_orange = ((255 & 0xff) << 16) + ((85 & 0xff) << 8) + (0 &
				// 0xff);
				// int color_own = ((0 & 0xff) << 16) + ((0 & 0xff) << 8) + (0 & 0xff);
				int color_yellow = ((255 & 0xff) << 16) + ((255 & 0xff) << 8) + (0 & 0xff);

				for (int i = 0; i < this.featureLines.size(); i++) {
						Line2D.Double line = featureLines.elementAt(i).elementAt(0);
						rgbImage.drawLine2D((int) Math.round(line.x1), (int) Math.round(line.y1),
						    (int) Math.round(line.x2), (int) Math.round(line.y2), color_yellow);

						line = featureLines.elementAt(i).elementAt(1);
						rgbImage.drawLine2D((int) Math.round(line.x1), (int) Math.round(line.y1),
						    (int) Math.round(line.x2), (int) Math.round(line.y2), color_yellow);

						// rgbImage.drawPoint2D((int)this.featurePoints.elementAt(i).elementAt(0).x,(int)this.featurePoints.elementAt(i).elementAt(0).y,0,((0
						// & 0xff) << 16) + ((0 & 0xff) << 8) + (0 & 0xff),0);

				}
				if (regionColor != null) {
						Vector<Point2D.Double> points = this.neuriteRegion.getPoints();
						for (int i = 0; i < points.size(); i++) {
								Point2D.Double p = points.elementAt(i);
								int x = (int) Math.round(p.x);
								int y = (int) Math.round(p.y);
								int[] col = rgbImage.getValue(x, y);
								int sum = col[0] + col[1] + col[2];

								if (sum == 765 || sum == 0) {
										rgbImage.putValue(x, y, regionColor.getRed(), regionColor.getGreen(),
										    regionColor.getBlue());
								}
						}
				}
				return rgbImage;
		}

		/**
		 * Draws a 2D line into the neuron image.
		 * <p>
		 * This function implements the Bresenham algorithm. Code was 'stolen' from
		 * Wikipedia, {@link http://de.wikipedia.org/wiki/Bresenham-Algorithmus}, and
		 * then translated into Java (German comments where kept).
		 * 
		 * @param xstart
		 *          x-coordinate of start point.
		 * @param ystart
		 *          y-coordinate of start point.
		 * @param xend
		 *          x-coordinate of end point.
		 * @param yend
		 *          y-coordinate of end point.
		 * @param value
		 *          Color/gray-scale value of the polygon.
		 */
		public MTBImageByte drawLine2D(Line2D.Double line, Point2D.Double point,
		    MTBImageByte tmpNeuronImg) {

				final int[][] delta = { { 0, -1 }, { 1, -1 }, { 1, 0 }, { 1, 1 }, { 0, 1 },
				    { -1, 1 }, { -1, 0 }, { -1, -1 } };

				int xstart = (int) Math.round(line.x1);
				int ystart = (int) Math.round(line.y1);
				int xend = (int) Math.round(line.x2);
				int yend = (int) Math.round(line.y2);

				int x, y, t, dx, dy, incx, incy, pdx, pdy, ddx, ddy, es, el, err;

				/* Entfernung in beiden Dimensionen berechnen */
				dx = xend - xstart;
				dy = yend - ystart;

				/* Vorzeichen des Inkrements bestimmen */
				incx = (int) Math.signum(dx);
				incy = (int) Math.signum(dy);
				if (dx < 0)
						dx = -dx;
				if (dy < 0)
						dy = -dy;

				/* feststellen, welche Entfernung größer ist */
				if (dx > dy) {
						/* x ist schnelle Richtung */
						pdx = incx;
						pdy = 0; /* pd. ist Parallelschritt */
						// int[] col = { 170, 170, 170 };
						// debugImage = tmpNeurite.toImage(debugImage, col);
						ddx = incx;
						ddy = incy; /* dd. ist Diagonalschritt */
						es = dy;
						el = dx; /* Fehlerschritte schnell, langsam */
				} else {
						/* y ist schnelle Richtung */
						pdx = 0;
						pdy = incy; /* pd. ist Parallelschritt */
						ddx = incx;
						ddy = incy; /* dd. ist Diagonalschritt */
						es = dx;
						el = dy; /* Fehlerschritte schnell, langsam */
				}

				/* Initialisierungen vor Schleifenbeginn */
				x = xstart;
				y = ystart;
				err = el / 2;
				int bgColor = 0;
				// int fgColor = 255;

				if (x >= 0 && x < this.neuriteGraph.getWidth() && y >= 0
				    && y < this.neuriteGraph.getHeight()) {
						for (int j = 0; j < 8; j++) {
								tmpNeuronImg.putValueInt(x + delta[j][0], y + delta[j][1], bgColor);
						}
						tmpNeuronImg.putValueInt(x, y, bgColor);
				}

				/* Pixel berechnen */
				for (t = 0; t < el; ++t) /* t zaehlt die Pixel, el ist auch Anzahl */
				{
						/* Aktualisierung Fehlerterm */
						err -= es;
						if (err < 0) {
								/* Fehlerterm wieder positiv (>=0) machen */
								err += el;
								/* Schritt in langsame Richtung, Diagonalschritt */
								x += ddx;
								y += ddy;
						} else {
								/* Schritt in schnelle Richtung, Parallelschritt */
								x += pdx;
								y += pdy;
						}
						if (x >= 0 && x < this.neuriteGraph.getWidth() && y >= 0
						    && y < this.neuriteGraph.getHeight()) {
								for (int j = 0; j < 8; j++) {
										tmpNeuronImg.putValueInt(x + delta[j][0], y + delta[j][1], bgColor);
								}
								tmpNeuronImg.putValueInt(x, y, bgColor);
						}
				}
				return tmpNeuronImg;
		}
}
