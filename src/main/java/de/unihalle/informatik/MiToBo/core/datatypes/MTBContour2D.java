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

package de.unihalle.informatik.MiToBo.core.datatypes;

import ij.ImagePlus;
import ij.gui.NewImage;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.Polygon;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Hashtable;
import java.util.Vector;

import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.datatypes.interfaces.MTBDataExportableToImageJROI;
import de.unihalle.informatik.MiToBo.core.exceptions.MTBDatatypeException;
import de.unihalle.informatik.MiToBo.core.exceptions.MTBDatatypeException.DatatypeExceptionType;
import de.unihalle.informatik.MiToBo.math.arrays.filter.GaussFilterDouble1D;
import de.unihalle.informatik.MiToBo.segmentation.regions.labeling.LabelComponentsSequential;

/**
 * Class to create 2D contour objects with a vector of points, belonging to the
 * contour. Every contour can include a set of inner contours.
 * <p>
 * Contrary to the super class of 2D borders the points belonging to a 
 * contour have a certain ordering.
 * 
 * @author misiak
 * 
 */
@ALDDerivedClass
@ALDParametrizedClass
public class MTBContour2D extends MTBBorder2D 
	implements MTBDataExportableToImageJROI {

		/**
		 * Standard constructor. Creates an empty 2D contour object.
		 */
		public MTBContour2D() {
//				this.points = new Vector<Point2D.Double>();
//				this.inner = new Vector<MTBBorder2D>();
//				this.pointNum = 0;
			super();
		}

		/**
		 * Constructor to create a 2D contour object from a 2D point vector.
		 * 
		 * @param _points
		 *          vector with 2D points
		 */
		public MTBContour2D(Vector<Point2D.Double> _points) {
			super();
			this.points = _points;
//			this.inner = new Vector<MTBContour2D>();
			this.pointNum = _points.size();
		}

		/**
		 * Override java.lang.Object.clone() to create and return a copy of this
		 * object.
		 */
		@Override
		public MTBContour2D clone() {
			Vector<Point2D.Double> clonedPoints = new Vector<Point2D.Double>();
			for (Point2D.Double p: this.points)
				clonedPoints.add((Point2D.Double)(p.clone()));
			MTBContour2D tmpContour = new MTBContour2D(clonedPoints);
			tmpContour.pointNum = this.pointNum;
			Vector<MTBBorder2D> clonedInner = new Vector<MTBBorder2D>();
			for (MTBBorder2D ib: this.inner) {
				MTBContour2D clonedBorder = (MTBContour2D)ib.clone();
				clonedInner.add(clonedBorder);
			}
			try {
	      tmpContour.setInner(clonedInner);
      } catch (MTBDatatypeException e) {
       	System.err.println("[MTBContour2D] Cloning contour failed...");
      	e.printStackTrace();
      	return null;
      }
			return tmpContour;
		}

		/**
		 * Get a Contour2D copy of this object.
		 * 
		 * @return Copy of this Contour2D object.
		 */
		public MTBContour2D getContour() {
				return this.clone();
		}

		/**
		 * Get all points belonging to the contour object.
		 * 
		 * @return Vector with all 2D points of the contour.
		 */
//		public Vector<Point2D.Double> getPoints() {
//				return this.points;
//		}

		/**
		 * Get a specific 2D point belonging to the contour object.
		 * 
		 * @return 2D point at specific index.
		 */
//		public Point2D.Double getPointAt(int index) {
//				return this.points.elementAt(index);
//		}

		/**
		 * Calculates chaincode from the outer contour. The contour points must be
		 * stored in consecutive order and the contour must be closed!
		 * 
		 * @return Chaincode of outer contour.
		 */
		public int[] getChaincode() {
				Hashtable<Point2D.Double, Integer> codeTable = new Hashtable<Point2D.Double, Integer>();

				codeTable.put(new Point2D.Double(1, 0), new Integer(0));
				codeTable.put(new Point2D.Double(1, 1), new Integer(1));
				codeTable.put(new Point2D.Double(0, 1), new Integer(2));
				codeTable.put(new Point2D.Double(-1, 1), new Integer(3));
				codeTable.put(new Point2D.Double(-1, 0), new Integer(4));
				codeTable.put(new Point2D.Double(-1, -1), new Integer(5));
				codeTable.put(new Point2D.Double(0, -1), new Integer(6));
				codeTable.put(new Point2D.Double(1, -1), new Integer(7));

				int n = this.points.size();
				int[] code = new int[n];

				int dx;
				int dy;

				for (int i = 1; i < n; i++) {
						dx = (int) (this.points.elementAt(i).x - this.points.elementAt(i - 1).x);
						dy = (int) (this.points.elementAt(i).y - this.points.elementAt(i - 1).y);

						if (codeTable.containsKey(new Point2D.Double(dx, dy))) {
							code[i - 1]= codeTable.get(new Point2D.Double(dx, dy)).intValue();
						}
				}

				dx = (int) (this.points.elementAt(0).x - this.points.elementAt(n - 1).x);
				dy = (int) (this.points.elementAt(0).y - this.points.elementAt(n - 1).y);

				if (codeTable.containsKey(new Point2D.Double(dx, dy))) {
						code[n - 1] = codeTable.get(new Point2D.Double(dx, dy)).intValue();
				}

				return code;
		}

		/**
		 * Calculates the length of the outer contour using it's chaincode.
		 * 
		 * @return Length of the contour.
		 */
		public double getContourLength() {
				double sqrt_2 = Math.sqrt(2);
				double l = 0;
				int[] ccode = getChaincode();

				for (int i = 0; i < ccode.length; i++) {
						if (ccode[i] % 2 == 0) // vertical and horizontal points have distance 1
						{
								l += 1;
						} else // diagonal points have distance sqrt(2)
						{
								l += sqrt_2;
						}
				}
				return l;
		}

		/**
		 * Get the number of points of the contour object.
		 * 
		 * @return Number of contour points.
		 */
//		public int getPointNum() {
//				return this.pointNum;
//		}

		/**
		 * Add a 2D point to the existing contour.
		 * 
		 * @param x
		 *          x-coordinate of the new point
		 * @param y
		 *          y-coordinate of the new point
		 */
//		public void addPixel(int x, int y) {
//				this.pointNum++;
//				this.points.addElement(new Point2D.Double(x, y));
//		}

		/**
		 * Set the inner contours of the contour object.
		 * <p>
		 * Note that an error occurs if the objects are not of type 
		 * {@link MTBContour2D}.
		 * 
		 * @param C		Vector with inner contours belonging to the contour object.
		 */
		@Override
    public void setInner(Vector<MTBBorder2D> C) throws MTBDatatypeException {
			for (MTBBorder2D c: C)
				if (!c.getClass().equals(MTBContour2D.class))
					throw new MTBDatatypeException(DatatypeExceptionType.WRONG_DADATYPE,
							"[MTBContour2D] Inner contour is not of type MTBContour2D!");
			this.inner = C;
		}

		/**
		 * Add a inner contour to the existing contour object.
		 * 
		 * @param C
		 *          the new 2D inner contour
		 */
    @Override
		@SuppressWarnings("unused")
    public void addInner(MTBBorder2D C) throws MTBDatatypeException {
				this.inner.addElement(C);
		}
    
		/**
		 * Get the number of inner contours including in the contour object.
		 * 
		 * @return The number of inner contours.
		 */
		public int getInnerCount() {
				return this.inner.size();
		}

		/**
		 * Get a specific inner contour from the contour object.
		 * 
		 * @param index
		 *          specific index of the inner contour to get from the inner contour
		 *          vector
		 * @return Inner contour at the specific index as 2D contour object.
		 */
		@Override
    public MTBContour2D getInner(int index) {
				return (MTBContour2D)this.inner.elementAt(index);
		}

		/**
		 * Get all inner contours from the contour object.
		 * 
		 * @return Copy of vector with all inner contours.
		 */
		public Vector<MTBContour2D> getAllInner() {
			Vector<MTBContour2D> conts = new Vector<MTBContour2D>();
			for (MTBBorder2D c: this.inner)
				conts.add((MTBContour2D)c);
			return conts;
		}

		/**
		 * Method to get the included region in a 2D contour as Region2D object.
		 * 
		 * @param imageSizeX
		 *          width of image that includes the region
		 * @param imageSizeY
		 *          height of image that includes the region
		 * @return 2D region included in the 2D contour.
		 * @throws ALDOperatorException Thrown in case of failure.
		 * @throws ALDProcessingDAGException Thrown in case of failure.
		 */
		// ToDo use contains method from Polygon2D if method was tuned up
		public MTBRegion2D getRegion(int imageSizeX, int imageSizeY)
		    throws ALDOperatorException, ALDProcessingDAGException {
				/*
				 * Calculate whole region which is included in the outer contour.
				 */
				int[] xps = new int[this.points.size()];
				int[] yps = new int[this.points.size()];
				int n = 0;
				for (Point2D.Double p : this.points) {
						xps[n] = (int) (p.x + 0.5);
						yps[n] = (int) (p.y + 0.5);
						n++;
				}
				// create awt polygon
				Polygon awtPoly = new Polygon(xps, yps, this.points.size());
				// background black, polygon white
				ImagePlus img = NewImage.createByteImage("", imageSizeX, imageSizeY, 1,
				    NewImage.FILL_BLACK);
				ImageProcessor ip = img.getProcessor();
				ip.setColor(Color.WHITE);
				// fill polygon with Java-AWT filling algorithm
				ip.fillPolygon(awtPoly);
				// create MTBImage object as result image
				MTBImageByte fullRegion = (MTBImageByte) MTBImage.createMTBImage(img);
				for (int i = 0; i < this.points.size(); i++) {
						Point2D.Double p = this.points.elementAt(i);
						fullRegion.putValueInt((int) Math.round(p.x), (int) Math.round(p.y), 255);
				}
				/*
				 * If an inner contour exists, get all the pixels in this regions (the hole)
				 * and subtract these pixels from the region of the outer contour to get the
				 * hole back.
				 */
				for (int i = 0; i < this.inner.size(); i++) {
						MTBContour2D tmpCont = (MTBContour2D)this.inner.elementAt(i);
						// create Java-AWT polygon from current inner contour pixels
						int[] tmpXPS = new int[tmpCont.pointNum];
						int[] tmpYPS = new int[tmpCont.pointNum];
						int tmpN = 0;
						for (Point2D.Double p : tmpCont.points) {
								tmpXPS[tmpN] = (int) (p.x + 0.5);
								tmpYPS[tmpN] = (int) (p.y + 0.5);
								tmpN++;
						}
						Polygon tmpPoly = new Polygon(tmpXPS, tmpYPS, tmpCont.pointNum);
						// background white, polygon black
						ImagePlus img2 = NewImage.createByteImage("", imageSizeX, imageSizeY, 1,
						    NewImage.FILL_BLACK);
						ImageProcessor ip2 = img2.getProcessor();
						ip2.setColor(Color.WHITE);
						ip2.fillPolygon(tmpPoly);
						for (int y = 0; y < imageSizeY; ++y) {
								for (int x = 0; x < imageSizeX; ++x) {
										if (ip2.getPixel(x, y) == 255)
												fullRegion.putValueInt(x, y, 0);
								}
						}
						for (int j = 0; j < tmpCont.pointNum; j++) {
								Point2D.Double p = tmpCont.points.elementAt(j);
								fullRegion.putValueInt((int) Math.round(p.x), (int) Math.round(p.y),
								    255);
						}
				}
				/*
				 * Create Region2D object from binary MTBImage. If some inner contours
				 * exist, more then one region is returned. The outer Region is the first
				 * one in the vector, than the inner regions follow.
				 */
				LabelComponentsSequential LCS = new LabelComponentsSequential(fullRegion,
				    true);
				LCS.runOp(null);
				MTBRegion2DSet resultRegion = LCS.getResultingRegions();
				System.out.println(resultRegion.size());
				return resultRegion.elementAt(0);
		}

		/* (non-Javadoc)
		 * @see de.unihalle.informatik.MiToBo.core.datatypes.interfaces.MTBDataExportableToImageJROI#convertToImageJRoi()
		 */
		@Override
		public PolygonRoi[] convertToImageJRoi() {

			int[] xPoints = new int[this.getPointNum()];
			int[] yPoints = new int[this.getPointNum()];
			Vector<Point2D.Double> pts = this.getPoints();
			int i = 0;
			for (Point2D.Double p : pts) {
				xPoints[i] = (int) (p.x);
				yPoints[i] = (int) (p.y);
				++i;
			}
			PolygonRoi p = 
				new PolygonRoi(xPoints, yPoints, pts.size(), Roi.POLYGON);
			return new PolygonRoi[]{p};
		}
		
		/**
		 * Method to save a contour with all inner contours on a binary image with
		 * specific width and height.
		 * 
		 * @param file
		 *          path where the file should be saved, can be null
		 * @param width
		 *          width of the binary image
		 * @param height
		 *          height of the binary image
		 * @return 2D inner- and outer-contours in a binary image of type
		 *         MTBImageByte.
		 * 
		 * @throws ALDOperatorException
		 * @throws ALDProcessingDAGException
		 */
//		public MTBImageByte toMTBImageByte(String file, int width, int height)
//		    throws ALDOperatorException, ALDProcessingDAGException {
//				MTBImageByte imageToDraw = (MTBImageByte) MTBImage.createMTBImage(width,
//				    height, 1, 1, 1, MTBImageType.MTB_BYTE);
//				for (int i = 0; i < this.points.size(); ++i) {
//
//						Point2D.Double p = this.points.elementAt(i);
//						imageToDraw.putValueDouble((int) Math.round(p.getX()), (int) Math.round(p
//						    .getY()), 255);
//				}
//				int end = this.inner.size();
//				for (int j = 0; j < end; j++) {
//						MTBContour2D c = this.inner.elementAt(j);
//						Vector<Point2D.Double> in_points = c.getPoints();
//						for (int k = 0; k < in_points.size(); k++) {
//								Point2D.Double p = in_points.elementAt(k);
//								imageToDraw.putValueDouble((int) Math.round(p.getX()), (int) Math
//								    .round(p.getY()), 255);
//						}
//				}
//				if (file != null) {
//
//						ImageWriterMTB writer = new ImageWriterMTB(imageToDraw, file);
//						writer.setOverwrite(false);
//						writer.runOp(null);
//
//				}
//				return imageToDraw;
//		}

		/**
		 * Method to save a contour with all inner contours in a given MTBImage image.
		 * 
		 * @param file
		 *          path where the file should be saved, can be null
		 * @param image
		 *          image where the contours should be drawn in
		 * @return 2D inner- and outer-contours in a binary image of type
		 *         MTBImageByte.
		 * 
		 * @throws ALDOperatorException
		 * @throws ALDProcessingDAGException
		 */
//		public MTBImage toMTBImage(String file, MTBImage image)
//		    throws ALDOperatorException, ALDProcessingDAGException {
//				MTBImage imageToDraw = image.duplicate();
//				for (int i = 0; i < this.points.size(); ++i) {
//						Point2D.Double p = this.points.elementAt(i);
//						imageToDraw.putValueDouble((int) Math.round(p.getX()), (int) Math.round(p
//						    .getY()), image.getTypeMax());
//				}
//				int end = this.inner.size();
//				for (int j = 0; j < end; j++) {
//						MTBContour2D c = this.inner.elementAt(j);
//						Vector<Point2D.Double> in_points = c.getPoints();
//						for (int k = 0; k < in_points.size(); k++) {
//								Point2D.Double p = in_points.elementAt(k);
//								imageToDraw.putValueDouble((int) Math.round(p.getX()), (int) Math
//								    .round(p.getY()), image.getTypeMax());
//						}
//				}
//				if (file != null) {
//						ImageWriterMTB writer = new ImageWriterMTB(imageToDraw, file);
//						writer.setOverwrite(false);
//						writer.runOp(null);
//				}
//				return imageToDraw;
//		}

		/**
		 * Calculates the axes-parallel bounding box of the contour.
		 * <p>
		 * The function extracts the coordinates of the upper left and lower right
		 * corner of the bounding box of the contour. Note that the there is at least
		 * one point of the contour lying on each side of the bounding box, i.e. the
		 * contour not just touches the box, but lies on it.
		 * <p>
		 * The result array contains the corner coordinates in the following order:
		 * [xmin, ymin, xmax, ymax]
		 * 
		 * @return Coordinates of upper left and lower right corners.
		 */
//		public double[] getBoundingBox() {
//				double minX = Double.MAX_VALUE;
//				double maxX = 0;
//				double minY = Double.MAX_VALUE;
//				double maxY = 0;
//
//				for (Point2D.Double p : this.points) {
//						if (p.x > maxX)
//								maxX = p.x;
//						if (p.x < minX)
//								minX = p.x;
//						if (p.y > maxY)
//								maxY = p.y;
//						if (p.y < minY)
//								minY = p.y;
//				}
//				double[] bbox = new double[4];
//				bbox[0] = minX;
//				bbox[1] = minY;
//				bbox[2] = maxX;
//				bbox[3] = maxY;
//				return bbox;
//		}
		
		/**
		 * Smoothes the contour by convolving x and y coordinates with a 
		 * Gaussian kernel. 
		 * <p>
		 * Note that the outer contour and all inner contours are smoothed.
		 * 
		 * @param gaussSigma	Standard deviation of the Gaussian kernel.
		 * @return	Smoothed contour.
		 * @throws MTBDatatypeException 			Thrown in case of failure.
		 * @throws ALDProcessingDAGException 	Thrown in case of failure.
		 * @throws ALDOperatorException 			Thrown in case of failure.
		 */
		public MTBContour2D smoothContour(double gaussSigma) throws 
			MTBDatatypeException, ALDOperatorException, ALDProcessingDAGException {
			
			Vector<Point2D.Double> smoothedPoints = 
					smoothPointVector(this.points, gaussSigma);
			MTBContour2D nc = new MTBContour2D(smoothedPoints);
			
			MTBContour2D ic;
			for (int i=0; i<this.inner.size(); ++i) {
				ic = new MTBContour2D(
						smoothPointVector(this.inner.elementAt(i).points, gaussSigma));
				nc.addInner(ic);
			}
			return nc;
		}

		/**
		 * Draw a contour into an image.
		 * 
		 * @param img			Image where to draw the contour into.
		 * @param color 	Color in which to draw the contour.
		 */
	  public void drawContour(MTBImage img, Color color) {

	    // get number of points
	    int np = this.points.size();

	    if (np == 0)
	      return;

	    // draw segments
	    int c = ((color.getRed() & 0xff) << 16) 
	    		+ ((color.getGreen() & 0xff) << 8) 
	    		+ (color.getBlue() & 0xff);
	    for (int i = 0; i < np - 1; i++) {
	      int x1 = (int) Math.round(this.points.get(i).x);
	      int y1 = (int) Math.round(this.points.get(i).y);
	      int x2 = (int) Math.round(this.points.get(i + 1).x);
	      int y2 = (int) Math.round(this.points.get(i + 1).y);
	      img.drawLine2D(x1, y1, x2, y2, c);
	    }
	    // last segment N-1 -> 0
	    int x1 = (int) Math.round(this.points.get(np - 1).x);
	    int y1 = (int) Math.round(this.points.get(np - 1).y);
	    int x2 = (int) Math.round(this.points.get(0).x);
	    int y2 = (int) Math.round(this.points.get(0).y);
	    img.drawLine2D(x1, y1, x2, y2, c);
	  }
	  
		/**
		 * Read a 2D contour object from an ASCII file in xSV format.
		 * <p>
		 * The file is expected to contain a single point in each row, first
		 * the x-coordinate and then the y-coordinate. Both coordinates should
		 * be separated by the delimiter character.  
		 * 
		 * @param file			File name from where to read the points.
		 * @param delim			Delimiter in the file.
		 * @param skipLines	Number of header lines to skip.
		 * @return Contour object.
		 */
		public static MTBContour2D readContourFromASCIIFile(
				String file, String delim, int skipLines) {
			
			try {
				BufferedReader bf = new BufferedReader(new FileReader(new File(file)));
				
				Vector<Point2D.Double> pointVec = new Vector<>();
				
				// skip first lines
				int sl = 0;
				String line;
				while (sl < skipLines) {
					line = bf.readLine();
					++sl;
				}
				line = bf.readLine();
				
				String[] coords = null;
				double x, y;
				while (line != null) {
					coords = line.split(delim);
					x = Double.parseDouble(coords[0]);
					y = Double.parseDouble(coords[1]);
					pointVec.add(new Point2D.Double(x, y));
					line = bf.readLine();
				}
				bf.close();
				return new MTBContour2D(pointVec);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
			
		}

		/**
		 * Convolves a list of 2D points with a Gaussian kernel.
		 * <p>
		 * x and y coordinates are each convolved with a Gaussian kernel.
		 * For convolution an operator of type {@link GaussFilterDouble1D}
		 * is applied. Take a look at its documentation for more details.
		 * <p>
		 * Note that we assume here that the list of points is periodic, e.g.,
		 * represents a closed contour.
		 * 
		 * @param pList		List of points.
		 * @param sigma		Standard deviation of Gaussian kernel.
		 * @return	Result of convolution.
		 * @throws ALDOperatorException				Thrown in case of failure. 
		 * @throws ALDProcessingDAGException 	Thrown in case of failure.
		 */
		private static Vector<Point2D.Double> smoothPointVector(
				Vector<Point2D.Double> pList, double sigma) 
						throws ALDOperatorException, ALDProcessingDAGException {

			int pointNum = pList.size();
			double[] xcoords = new double[pointNum];
			double[] ycoords = new double[pointNum];

			for (int i=0; i<pointNum; ++i) {
				xcoords[i] = pList.elementAt(i).x;
				ycoords[i] = pList.elementAt(i).y;
			}
			
			GaussFilterDouble1D gaussFilter = new GaussFilterDouble1D();
			gaussFilter.setSigma(sigma);
			gaussFilter.setDataIsPeriodic(true);
			gaussFilter.setInputArray(xcoords);
			gaussFilter.runOp();
			double[] nxcoords = gaussFilter.getResultArray();
			gaussFilter.setInputArray(ycoords);
			gaussFilter.runOp();
			double[] nycoords = gaussFilter.getResultArray();
			
			Vector<Point2D.Double> npList = new Vector<>();
			for (int i=0; i<pointNum; ++i) {
				npList.add(new Point2D.Double(nxcoords[i], nycoords[i]));
			}
			return npList;
		}
}
