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

package de.unihalle.informatik.MiToBo.segmentation.contours.extraction;

import java.awt.Polygon;
import java.awt.geom.Point2D;
import java.util.Vector;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBContour2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBContour2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBBorder2D.BorderConnectivity;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.exceptions.MTBDatatypeException;
import de.unihalle.informatik.MiToBo.core.operator.*;
import de.unihalle.informatik.MiToBo.segmentation.regions.labeling.*;

/**
 * Class to segment contours from a binary image using the contour following
 * algorithm. The contours are only segmented from a set of given image regions
 * in this binary image. If no image regions exist you should use one of the
 * LabelComponent methods from the package
 * de.unihalle.informatik.MiToBo.segmentation.regions.labeling.
 * <p>
 * Outer contours as well as inner contours (length greater than a given value)
 * are segmented.
 * 
 * 
 * @author Danny Misiak
 */
@ALDAOperator(genericExecutionMode = ALDAOperator.ExecutionMode.ALL, level = Level.APPLICATION)
public class ContourOnLabeledComponents extends MTBOperator {

		/**
		 * The possible type of contours for segmentation.
		 * 
		 * @author misiak
		 */
		public static enum ContourType {
				/**
				 * Only outer contours will be segmented.
				 */
				OUTER_CONTOUR,
				/**
				 * Only inner contours will be segmented.
				 */
				INNER_CONTOUR,
				/**
				 * Outer as well as inner contours will be segmented.
				 */
				OUT_IN_CONTOUR,
		}

		/**
		 * Image width.
		 */
		private transient int width;
		/**
		 * Image height.
		 */
		private transient int height;
		/**
		 * Minimum number of pixels a inner contour must have (length of the contour).
		 * No inner contours with length below this value will be segmented.
		 */
		@Parameter(label = "Inner Contour Length Min", required = true, direction = Parameter.Direction.IN, description = "Minimum of inner contour length.")
		private int innerContourLengthMin = 0;
		/**
		 * Special type of contours to calculate. Type can be inner, outer and both.
		 */
		@Parameter(label = "Contour Type", required = true, direction = Parameter.Direction.IN, description = "Contour type.")
		private ContourType contourType = ContourType.OUT_IN_CONTOUR;
		/**
		 * The binary input image where the contours should be calculated from.
		 */
		@Parameter(label = "Input Image", required = false, direction = Parameter.Direction.IN, description = "Input image.")
		private transient MTBImageByte inputImage = null;

		/**
		 * The input regions where the contours should be calculated from.
		 */
		@Parameter(label = "Input Regions", required = true, direction = Parameter.Direction.IN, description = "Input regions.")
		private transient MTBRegion2DSet inputRegions = null;
		/**
		 * * The binary image with the calculated contours and the contours as
		 * possible outputs from the operator.
		 */
		@Parameter(label = "Result Image", required = true, direction = Parameter.Direction.OUT, description = "Result image with contours.")
		private transient MTBImageByte resultImage = null;
		/**
		 * * The calculated contours, every contour can include several inner
		 * contours.
		 */
		@Parameter(label = "Result Contours", required = true, direction = Parameter.Direction.OUT, description = "Resulting contour set.")
		private transient MTBContour2DSet resultContours = null;

		/**
		 * Standard constructor.
		 */
		public ContourOnLabeledComponents() throws ALDOperatorException {
				// nothing to do here
		}

		/**
		 * Constructor. A new operator object is initialized.
		 * 
		 * @param inImg
		 *          input image to work on
		 * @param inRegions
		 *          regions to get the contours from
		 * @param type
		 *          type of the resulting contour regions to get the contours from
		 * @param length
		 *          minimum number of pixels that a inner region must have (length of
		 *          the inner contour) to calculate the inner contour
		 * @throws ALDOperatorException
		 */
		public ContourOnLabeledComponents(MTBImageByte inImg,
		    MTBRegion2DSet inRegions, ContourType type, int length)
		    throws ALDOperatorException {
				this.inputImage = inImg;
				this.inputRegions = inRegions;
				this.contourType = type;
				this.innerContourLengthMin = length;

		}

		/**
		 * Constructor. A new operator object is initialized. No labeled image is
		 * give, so for each region the binary image is calculated for contour
		 * following.
		 * 
		 * @param inRegions
		 *          regions to get the contours from
		 * @param type
		 *          type of the resulting contour regions to get the contours from
		 * @param length
		 *          minimum number of pixels that a inner region must have (length of
		 *          the inner contour) to calculate the inner contour
		 * @throws ALDOperatorException
		 */
		public ContourOnLabeledComponents(MTBRegion2DSet inRegions, ContourType type,
		    int length) throws ALDOperatorException {
				this.inputRegions = inRegions;
				this.contourType = type;
				this.innerContourLengthMin = length;
				this.inputImage = null;

		}

		/**
		 * Get reference to the current input image.
		 * 
		 * @return Input image to work on.
		 */
		public MTBImageByte getInputImage() {
				return this.inputImage;
		}

		/**
		 * Set reference to the current input image.
		 */
		public void setInputImage(MTBImageByte inImg) {
				this.inputImage = inImg;
		}

		/**
		 * Get reference to the input regions.
		 * 
		 * @return Region2D vector including all regions.
		 */
		public MTBRegion2DSet getInputRegions() {
				return this.inputRegions;
		}

		/**
		 * Set reference to the the input regions.
		 */
		public void setInputRegions(MTBRegion2DSet inRegions) {
				this.inputRegions = inRegions;
		}

		/**
		 * Get reference to the current contour type.
		 * 
		 * @return The used contour type.
		 */
		public ContourType getContourType() {
				return this.contourType;
		}

		/**
		 * Set reference to the contour type.
		 */
		public void setContourType(ContourType type) {
				this.contourType = type;
		}

		/**
		 * Get the minimum number of pixels in the inner contour (length of the
		 * contour). No inner contours with a length below this value will be
		 * segmented.
		 * 
		 * @return Minimum value of pixels for a inner region.
		 */
		public int getInnerContourLengthMin() {
				return innerContourLengthMin;
		}

		/**
		 * Set the minimum number of pixels in the inner contour (length of the
		 * contour). No inner contours with a length below this value will be
		 * segmented.
		 */
		public void setInnerContourLengthMin(int length) {
				this.innerContourLengthMin = length;
		}

		/**
		 * Get reference to the binary image with the calculated contours.
		 * 
		 * @return Binary image with contours
		 */
		public MTBImageByte getResultImage() {
				return this.resultImage;
		}

		/**
		 * Get reference to the calculated contours.
		 * 
		 * @return Inner- and outer-contours of the regions.
		 */
		public MTBContour2DSet getResultContours() {
				return resultContours;
		}

		/**
		 * Get image width.
		 */
		public int getWidth() {
				return width;
		}

		/**
		 * Set image width.
		 */
		public void setWidth(int w) {
				this.width = w;
		}

		/**
		 * Get image height.
		 */
		public int getHeight() {
				return height;
		}

		/**
		 * Set image height.
		 */
		public void setHeight(int h) {
				this.height = h;
		}

		/**
		 * This method does the actual work.
		 */
		@Override
		protected void operate() throws ALDOperatorException,
		    ALDProcessingDAGException {
				if (this.inputImage == null) {

						this.width = (int) Math.round(this.inputRegions.getXmax()) + 1;
						this.height = (int) Math.round(this.inputRegions.getYmax()) + 1;
				} else {
						this.width = this.inputImage.getSizeX();
						this.height = this.inputImage.getSizeY();
				}

				// calculate the contours and a result image where the contours are drawn in
				this.resultImage = calcContours();
				this.resultImage.setTitle("Contour-Result");

		}

		/**
		 * Get all inner- and outer-contours for the given regions.
		 * 
		 * @return Image including all contours that should be segmented.
		 * @throws ALDOperatorException
		 */
		protected MTBImageByte calcContours() throws ALDOperatorException,
		    ALDProcessingDAGException {
				this.resultContours = null;
				/*
				 * Get the contours of the given regions.
				 */
				switch (getContourType()) {
				case OUTER_CONTOUR:
						this.resultContours = getOuterContours(this.inputRegions, this.inputImage);
						break;

				case INNER_CONTOUR:
						this.resultContours = getOuterContours(this.inputRegions, this.inputImage);
						this.resultContours = addInnerContours();
						break;

				case OUT_IN_CONTOUR:
						this.resultContours = getOuterContours(this.inputRegions, this.inputImage);
						addInnerContours();
						break;
				}
				return (toMTBImageByte());
		}

		/**
		 * Adding the possible inner contours to the outer contour of a region.
		 * 
		 * @return Vector including all inner contours of a outer contour.
		 */
		protected MTBContour2DSet addInnerContours() throws ALDOperatorException,
		    ALDProcessingDAGException {
				/*
				 * all inner contours of all given regions with a length greater than
				 * innerContourLengthMin
				 */
				MTBContour2DSet allInnerContours = new MTBContour2DSet(0, 0,
				    this.width - 1, this.height - 1);
				// calculate inner contours for every outer contour
				for (int i = 0; i < this.resultContours.size(); i++) {
						/*
						 * creating a vector with all points of the current outer region and
						 * creating a byte array with all pixels of the outer region (without the
						 * inner regions)
						 */
						Vector<Point2D.Double> RegionPoints = this.inputRegions.elementAt(i)
						    .getPoints();
						byte[] reg_pixs = new byte[width * height];
						for (int j = 0; j < RegionPoints.size(); j++) {
								Point2D.Double p = RegionPoints.elementAt(j);
								int pos = (int) Math.round(p.getY()) * width
								    + (int) Math.round(p.getX());
								reg_pixs[pos] = (byte) 255;
						}
						int[] xpoints = new int[this.resultContours.elementAt(i).getPointNum()];
						int[] ypoints = new int[this.resultContours.elementAt(i).getPointNum()];
						byte[] inner_pixs = new byte[width * height];
						/*
						 * creating a polygon from the pixels of the current contour from the
						 * current region and creating a byte array with all pixels of the inner
						 * region and outer region (for getting a filled region)
						 */
						Vector<Point2D.Double> OutContourPoints = this.resultContours
						    .elementAt(i).getPoints();
						for (int j = 0; j < OutContourPoints.size(); j++) {
								xpoints[j] = (int) Math.round(OutContourPoints.elementAt(j).getX());
								ypoints[j] = (int) Math.round(OutContourPoints.elementAt(j).getY());
								// add the polygon pixels to the filled region
								int pos = ypoints[j] * width + xpoints[j];
								inner_pixs[pos] = (byte) 255;
						}

						Polygon polygon = new Polygon(xpoints, ypoints, OutContourPoints.size());
						// add the pixels inside the polygon to the filled region
						for (int y = 0; y < height; ++y) {
								for (int x = 0; x < width; ++x) {
										int pos = y * width + x;
										if (polygon.contains(x, y)) {
												inner_pixs[pos] = (byte) 255;
										}
								}
						}
						/*
						 * difference between the filled region and the outer region to get the
						 * inner regions and calculate the contours of the inner regions
						 */
						byte[] differencePixs = new byte[width * height];
						for (int y = 0; y < height; ++y) {
								for (int x = 0; x < width; ++x) {
										int pos = y * width + x;
										int tmp = (reg_pixs[pos] & 0xff) + (inner_pixs[pos] & 0xff);
										// use only the pixels, which exist in the filled region, but not in
										// the outer region
										if (tmp == 255) {
												differencePixs[pos] = (byte) 255;
										}
								}
						}
						// creating the image with the results of the difference
						MTBImageByte innerRegionImage = (MTBImageByte) MTBImage.createMTBImage(
						    this.width, this.height, 1, 1, 1, MTBImageType.MTB_BYTE);
						innerRegionImage.fillBlack();
						// calculate the inner contours as regions
						byte[] innerRegionImagePix = new byte[width * height];
						for (int y = 1; y < height - 1; y++) {
								for (int x = 1; x < width - 1; x++) {
										int pos = y * width + x;
										int p0 = differencePixs[y * width + x] & 0xff;
										int p1 = (differencePixs[(y - 1) * width + x] & 0xff);
										int p2 = (differencePixs[y * width + (x + 1)] & 0xff);
										int p3 = (differencePixs[(y + 1) * width + x] & 0xff);
										int p4 = (differencePixs[y * width + (x - 1)] & 0xff);
										int sum = (p1 + p2 + p3 + p4);
										if ((sum > 0) && (sum < 1020) && (p0 == 0)) {
												innerRegionImagePix[pos] = (byte) 255;
												innerRegionImage.putValueInt(x, y, 255);
										} else {
												innerRegionImagePix[pos] = (byte) 0;
												innerRegionImage.putValueInt(x, y, 0);
										}
								}
						}
						// label the inner contour "regions" to get the pixels of the contour
						LabelComponentsSequential lcs = new LabelComponentsSequential(
						    innerRegionImage, true);
						lcs.runOp(null);
						MTBRegion2DSet tmpInnerRegions = lcs.getResultingRegions();
						// copy the points of the regions to the inner contour vector
						for (int j = 0; j < tmpInnerRegions.size(); j++) {
								// use only the inner contours with a length greater
								// innerContourLengthMin
								if (tmpInnerRegions.elementAt(j).getArea() >= innerContourLengthMin) {
									try {
										MTBContour2D tmpCont = new MTBContour2D(tmpInnerRegions.elementAt(j)
										    .getPoints());
										tmpCont.setConnectivity(BorderConnectivity.CONNECTED_8);
										// add current inner contour to all inner contours of the region
										MTBRegion2DSet tmpRegs = new MTBRegion2DSet(0, 0, this.width - 1,
										    this.height - 1);
										tmpRegs.add(new MTBRegion2D(tmpCont.getPoints()));
										/*
										 * labeling of the inner contour to get pixels sorted
										 * counter-clockwise
										 */
										MTBImageByte tmpImage = tmpCont.toMTBImageByte(null, this.width,
										    this.height);
										ContourOnLabeledComponents clc = new ContourOnLabeledComponents(
										    tmpImage, tmpRegs, ContourType.OUTER_CONTOUR, 1);
										clc.runOp(null);
										MTBContour2DSet myContours = clc.getResultContours();
										allInnerContours.add(myContours.elementAt(0));
										// add inner contours to specific outer contour
										this.resultContours.elementAt(i).addInner(myContours.elementAt(0));
									}
									catch (MTBDatatypeException ex) {
										System.err.println("[ContourOnLabeledComponents] " +
											"something went wrong adding inner contour... skipping");
	                	continue;
									}
								}
						}
				}
				return allInnerContours;
		}

		/**
		 * Calculate the outer contours of the given regions in a image. Method from
		 * the book: W. Burger, M.J. Burge,
		 * "Digitale Bildverarbeitung. Eine Einfuehrung mit Java und ImageJ.", page
		 * 211
		 * 
		 * @param theRegions
		 *          Regions to get contours from.
		 * @param theImage
		 *          Binary image of the regions to get the contours from.
		 * @return Vector of contours from the given regions.
		 */
		protected MTBContour2DSet getOuterContours(MTBRegion2DSet theRegions,
		    MTBImageByte theImage) {

				MTBContour2DSet tempCont = new MTBContour2DSet(0, 0, this.width - 1,
				    this.height - 1);

				for (int i = 0; i < theRegions.size(); ++i) {
						MTBRegion2D r = theRegions.elementAt(i);
						Vector<Point2D.Double> points = r.getPoints();
						MTBContour2D c = new MTBContour2D();
						c.setConnectivity(BorderConnectivity.CONNECTED_8);

						if (points.size() > 0) {

								if (this.inputImage == null) {

										/*
										 * Calculate the bounding box to get the maximum x and y value of the
										 * region.
										 */
										double[] boundingBox = r.getBoundingBox();
										int imageSizeX = (int) Math.round(boundingBox[2] + 1);
										int imageSizeY = (int) Math.round(boundingBox[3] + 1);
										/*
										 * Calculate outer and inner contours of the region.
										 */
										theImage = (MTBImageByte) MTBImage.createMTBImage(imageSizeX,
										    imageSizeY, 1, 1, 1, MTBImageType.MTB_BYTE);
										int minX = Integer.MAX_VALUE;
										int minY = Integer.MAX_VALUE;
										for (int j = 0; j < points.size(); j++) {
												Point2D.Double p = points.elementAt(j);
												int x = (int) Math.round(p.x);
												int y = (int) Math.round(p.y);
												theImage.putValueInt(x, y, 255);
												if (y < minY) {
														minY = y;
														minX = x;
												}
												if (y == minY) {
														if (x < minX) {
																minX = x;
														}
												}
										}
										c = traceContour(minX, minY, theImage);
								} else {
										Point2D.Double p = points.elementAt(0);
										int px = (int) Math.round((p.getX()));
										int py = (int) Math.round((p.getY()));
										c = traceContour(px, py, theImage);
								}

						} else {
								c = new MTBContour2D();
								c.setConnectivity(BorderConnectivity.CONNECTED_8);
						}
						tempCont.add(c);
				}
				return (tempCont);
		}

		/**
		 * Trace contour, starting at (xS, yS) in direction dS.
		 * 
		 * @param xS
		 *          starting x-coordinate
		 * @param yS
		 *          starting y-coordinate
		 * @param theImage
		 *          the binary image to validate if pixel is black or white (get the
		 *          label)
		 * @return Contour of the region.
		 */
		protected MTBContour2D traceContour(int xS, int yS, MTBImageByte theImage) {
				MTBContour2D cont = new MTBContour2D();
				cont.setConnectivity(BorderConnectivity.CONNECTED_8);
				int xT, yT; // T = successor of starting point (xS,yS)
				int xP, yP; // P = previous contour point
				int xC, yC; // C = current contour point
				Point2D.Double n = new Point2D.Double(xS, yS);
				int dir = findNextPoint(n, 0, theImage);
				cont.addPixel((int) Math.round(n.getX()), (int) Math.round(n.getY()));
				xP = xS;
				yP = yS;
				xC = xT = (int) n.getX();
				yC = yT = (int) n.getY();
				boolean done = (xS == xT && yS == yT); // true if isolated pixel
				while (!done) {
						n = new Point2D.Double(xC, yC);
						int dSearch = (dir + 6) % 8;
						dir = findNextPoint(n, dSearch, theImage);
						xP = xC;
						yP = yC;
						xC = (int) Math.round(n.getX());
						yC = (int) Math.round(n.getY());
						// are we back at the starting position?
						done = (xP == xS && yP == yS && xC == xT && yC == yT);
						if (!done) {
								cont.addPixel((int) n.getX(), (int) n.getY());
						}
				}
				return cont;
		}

		/**
		 * Find direction to next pixel in specific direction from the last pixel.
		 * 
		 * @param Xc
		 *          starting point
		 * @param dir
		 *          direction of last point
		 * @param theImage
		 *          the binary image to validate if pixel is black or white (get the
		 *          label)
		 * @return Next direction.
		 */
		protected int findNextPoint(Point2D.Double Xc, int dir, MTBImageByte theImage) {
				// starts at node Xc in direction dir
				// returns the final tracing direction
				// and modifies node Xc
				// direction table for getting new coordinates from the current coordinates
				final int[][] delta = { { 1, 0 }, { 1, 1 }, { 0, 1 }, { -1, 1 }, { -1, 0 },
				    { -1, -1 }, { 0, -1 }, { 1, -1 } };
				for (int i = 0; i < 7; i++) {
						int x = (int) Math.round(Xc.getX() + delta[dir][0]);
						int y = (int) Math.round(Xc.getY() + delta[dir][1]);
						// validate coordinates
						if (y < 0)
								dir = (dir + 1) % 8;
						else if (x < 0)
								dir = (dir + 1) % 8;
						else if (y >= theImage.getSizeY())
								dir = (dir + 1) % 8;
						else if (x >= theImage.getSizeX())
								dir = (dir + 1) % 8;
						else if (theImage.getValueInt(x, y) == 0) {
								dir = (dir + 1) % 8;
						} else {
								Xc.setLocation(x, y);
								break;
						}
				}
				return dir;
		}

		/**
		 * Method to save all contours from the regions on a binary image with
		 * specific width and height.
		 * 
		 * @return 2D inner- and outer-contours in a binary image of type
		 *         MTBImageByte.
		 */
		protected MTBImageByte toMTBImageByte() {
				MTBImageByte imageToDraw = (MTBImageByte) MTBImage.createMTBImage(width,
				    height, 1, 1, 1, MTBImageType.MTB_BYTE);
				for (int i = 0; i < this.resultContours.size(); ++i) {
						MTBContour2D out = this.resultContours.elementAt(i);
						Vector<Point2D.Double> out_points = out.getPoints();
						int color = 0;
						while (color < 10) {
								color = (int) (Math.random() * 255.0);
						}

						for (int j = 0; j < out_points.size(); ++j) {
								Point2D.Double p = out_points.elementAt(j);
								imageToDraw.putValueDouble((int) Math.round(p.getX()), (int) Math
								    .round(p.getY()), color);
						}
						int end = out.countInner();
						for (int j = 0; j < end; j++) {
								MTBContour2D c = out.getInner(j);
								Vector<Point2D.Double> in_points = c.getPoints();
								for (int k = 0; k < in_points.size(); k++) {
										Point2D.Double p = in_points.elementAt(k);
										imageToDraw.putValueDouble((int) Math.round(p.getX()), (int) Math
										    .round(p.getY()), color);
								}
						}
				}
				return imageToDraw;
		}
}
