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

import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;

import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBBorder2D.BorderConnectivity;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.io.images.ImageWriterMTB;
import de.unihalle.informatik.MiToBo.segmentation.contours.extraction.BordersOnLabeledComponents;
import de.unihalle.informatik.MiToBo.segmentation.contours.extraction.ContourOnLabeledComponents;
import de.unihalle.informatik.MiToBo.segmentation.contours.extraction.BordersOnLabeledComponents.BorderType;
import de.unihalle.informatik.MiToBo.segmentation.contours.extraction.ContourOnLabeledComponents.ContourType;
import de.unihalle.informatik.MiToBo.topology.MTBTopologicalNumber2DN8;
import de.unihalle.informatik.MiToBo.topology.MTBTopologicalNumber.Point3D;
import de.unihalle.informatik.MiToBo_xml.MTBXMLPoint2DDoubleType;
import de.unihalle.informatik.MiToBo_xml.MTBXMLPointVectorType;
import de.unihalle.informatik.MiToBo_xml.MTBXMLRegion2DType;

/**
 * Class to implement a 2D region object. the region is stored in a vector of 2D
 * double points with its x- and y-coordinate. The area, center of mass,
 * moments, and other properties of the region can be calculated. For further
 * details watch the implemented methods.
 * 
 * @author Danny Misiak
 */
@ALDParametrizedClass
public class MTBRegion2D implements MTBRegionInterface {

		/**
		 * Id of the region (uniqueness is not guaranteed nor checked!).
		 */
		@ALDClassParameter(label="ID")
		private int id;

		/**
		 * Pixels belonging to region.
		 */
		@ALDClassParameter(label="List of points", 
			changeValueHook="hookPointsUpdated" )
		Vector<Point2D.Double> points;

		/**
		 * Center of mass in x (just sum, not normalized by area!).
		 */
		private float com_x = 0;

		/**
		 * Center of mass in y (just sum, not normalized by area!).
		 */
		private float com_y = 0;

		/**
		 * Size of region in pixels.
		 */
		private int area = 0;

		/**
		 * Construct a new empty MTBRegion2D object.
		 */
		public MTBRegion2D() {
				this.points = new Vector<Point2D.Double>();
				this.com_x = 0.0f;
				this.com_y = 0.0f;
				this.area = 0;
		}

		/**
		 * Construct a new MTBRegion2D from the given points.
		 * 
		 * @param inPoints 	Vector of region points.
		 */
		public MTBRegion2D(Vector<Point2D.Double> inPoints) {
			this.points = new Vector<Point2D.Double>();
			for (Point2D.Double p: inPoints) {				
				this.points.addElement(p);
			}
			this.hookPointsUpdated();
		}

		/**
		 * Construct a new MTBRegion2D from a region object that was constructed from
		 * a xml-representation. Only used for reading region sets (
		 * {@link MTBRegion2DSet}) from file.
		 * 
		 * @param xmlregion2D
		 *          object that represents the region read from xml
		 */
		MTBRegion2D(MTBXMLRegion2DType xmlregion2D) {

			this.id = xmlregion2D.getId();

			MTBXMLPoint2DDoubleType[] ptTypes = 
				xmlregion2D.getPoints().getPointArray();
			this.points = new Vector<Point2D.Double>(ptTypes.length);

			for (int i = 0; i < ptTypes.length; i++) {
				this.points.add(
					new Point2D.Double(ptTypes[i].getX(), ptTypes[i].getY()));
			}
			// make sure that area and other internal variables are properly set
			this.hookPointsUpdated();
		}

		/**
		 * Construct an object that represents this region by xml. Only used for
		 * writing region sets ({@link MTBRegion2DSet}) to file.
		 */
		public MTBXMLRegion2DType toXMLType() {
				MTBXMLRegion2DType xmlregion2D = MTBXMLRegion2DType.Factory.newInstance();

				xmlregion2D.setArea(this.area);
				xmlregion2D.setId(this.id);
				xmlregion2D.setComX(this.getCenterOfMass_X());
				xmlregion2D.setComY(this.getCenterOfMass_Y());

				MTBXMLPoint2DDoubleType[] ptTypes = new MTBXMLPoint2DDoubleType[this.points
				    .size()];

				for (int i = 0; i < this.points.size(); i++) {
						ptTypes[i] = MTBXMLPoint2DDoubleType.Factory.newInstance();
						ptTypes[i].setX(this.points.get(i).x);
						ptTypes[i].setY(this.points.get(i).y);
				}

				MTBXMLPointVectorType pts = MTBXMLPointVectorType.Factory.newInstance();
				pts.setPointArray(ptTypes);

				xmlregion2D.setPoints(pts);

				return xmlregion2D;
		}

		/**
		 * Override java.lang.Object.clone() to create and return a copy of this
		 * object.
		 */
		@Override
		public MTBRegion2D clone() {
				MTBRegion2D tmpRegion = new MTBRegion2D(this.points);
				return tmpRegion;
		}

		/**
		 * Get a Region2D copy of this object.
		 * 
		 * @return Copy of this Region2D object.
		 */
		public MTBRegion2D getRegion() {
				return this.clone();
		}

		/**
		 * Return all points of the region.
		 * 
		 * @return Region points.
		 */
		public Vector<Point2D.Double> getPoints() {
				return this.points;
		}

		/**
		 * Return the area of the region in pixels.
		 * 
		 * @return Pixel area of the region.
		 */
		public int getArea() {
				return this.area;
		}

		/**
		 * Return x-coordinate of the center of mass.
		 * 
		 * @return Center of mass coordinate in x-dimension.
		 */
		public float getCenterOfMass_X() {
				return this.com_x / this.area;
		}

		/**
		 * Return y-coordinate of the center of mass.
		 * 
		 * @return Center of mass coordinate in y-dimension.
		 */
		public float getCenterOfMass_Y() {
				return this.com_y / this.area;
		}

		/**
		 * Extracts the extreme coordinates of the region in each dimension.
		 * 
		 * @return Array with extreme values: [xmin, ymin, xmax, ymax]
		 */
		public double[] getMinMaxCoordinates() {
				double extremes[] = new double[4];
				extremes[0] = Double.MAX_VALUE;
				extremes[1] = Double.MAX_VALUE;
				extremes[2] = Double.MIN_VALUE;
				extremes[3] = Double.MIN_VALUE;
				for (Point2D.Double p : this.points) {
						if (p.x < extremes[0])
								extremes[0] = p.x;
						if (p.x > extremes[2])
								extremes[2] = p.x;
						if (p.y < extremes[1])
								extremes[1] = p.y;
						if (p.y > extremes[3])
								extremes[3] = p.y;
				}
				return extremes;
		}

		/**
		 * Return the ID of the region. Uniqueness is not guaranteed nor checked!
		 * 
		 * @return Region ID.
		 */
		public int getID() {
				return this.id;
		}

		/**
		 * Append a point to the region from the given coordinates. The point is added
		 * at the end of the point vector.
		 * 
		 * @param x
		 *          coordinate in x-dimension
		 * @param y
		 *          coordinate in y-dimension
		 */
		public void addPixel(int x, int y) {
				this.area++;
				this.com_x = this.com_x + x;
				this.com_y = this.com_y + y;
				this.points.addElement(new Point2D.Double(x, y));
		}

		/**
		 * Append a point to the region from the given point object. The point is
		 * added at the end of the point vector.
		 * 
		 * @param p
		 *          point to add
		 */
		public void addPixel(Point2D.Double p) {
				this.area++;
				this.com_x += p.getX();
				this.com_y += p.getY();
				this.points.addElement(p);
		}

		/**
		 * Set the ID of the region. Uniqueness is not guaranteed nor checked!
		 * 
		 * @param _id
		 *          Region ID
		 */
		public void setID(int _id) {
				this.id = _id;
		}

		/**
		 * Join a region with this region. New point objects are created for the new
		 * region.
		 * 
		 * @param reg
		 *          region to join
		 * @return New region (union of the old region and the join region).
		 */
		public MTBRegion2D join(MTBRegion2D reg) {
				int x, y;
				boolean exists;
				MTBRegion2D r = new MTBRegion2D();
				r.id = this.id;
				// add pixels of this region
				for (int i = 0; i < this.points.size(); i++) {
						r.addPixel((int) this.points.get(i).x, (int) this.points.get(i).y);
				}
				// add pixels of region 'reg' if they do not already exist in this region
				for (int j = 0; j < reg.points.size(); j++) {
						exists = false;
						x = (int) reg.points.get(j).x;
						y = (int) reg.points.get(j).y;
						for (int i = 0; i < this.points.size(); i++) {
								if ((int) this.points.get(i).x == x && (int) this.points.get(i).y == y) {
										exists = true;
										break;
								}
						}
						if (!exists) {
								r.addPixel(x, y);
						}
				}
				return r;
		}

		/**
		 * Test if a point is inside the region or not.
		 * 
		 * @param p
		 *          point that maybe lies in the region
		 * @return True if point p is inside, false if not.
		 */
		public boolean contains(Point2D.Double p) {
				for (int i = 0; i < this.points.size(); i++) {
						Point2D.Double r = this.points.elementAt(i);
						if (p.equals(r)) {
								return true;
						}
				}
				return false;
		}
		
		@Override
		public boolean equals( Object obj) {
			if ( ! MTBRegion2D.class.isAssignableFrom(obj.getClass())) {
				return super.equals(obj);
			}
			
			MTBRegion2D region = (MTBRegion2D)obj;
			if ( this.area != region.area)
				return false;
			
			Iterator<Point2D.Double> iter = this.getPoints().iterator();
			Iterator<Point2D.Double> iterBack = region.getPoints().iterator();
			while ( iter.hasNext()) {
				Point2D.Double point = iter.next();
				Point2D.Double pointBack = iterBack.next();

				if ( !point.equals(pointBack))
					return false;
			}
			return true;
		}

		/**
		 * Method to save the region on a binary image with specific width and height.
		 * Color value of the region is set to 255.
		 * 
		 * @param file
		 *          path where the file should be saved, can be null if image should
		 *          not be stored at disk
		 * @param width
		 *          width of the binary image
		 * @param height
		 *          height of the binary image
		 * @return 2D region in a binary image of type MTBImageByte.
		 * @throws ALDProcessingDAGException
		 * @throws ALDOperatorException
		 * 
		 * @throws ALDOperatorException
		 * @throws ALDProcessingDAGException
		 */
		public MTBImageByte toMTBImageByte(String file, int width, int height)
		    throws ALDOperatorException, ALDProcessingDAGException {
				return (this.toMTBImageByte(file, width, height, 255));
		}

		/**
		 * Method to save the region on a binary image with specific width, height and
		 * color.
		 * 
		 * @param file
		 *          path where the file should be saved, can be null if image should
		 *          not be stored at disk
		 * @param width
		 *          width of the binary image
		 * @param height
		 *          height of the binary image
		 * @param color
		 *          region color
		 * @return 2D region in a binary image of type MTBImageByte.
		 * 
		 * @throws ALDOperatorException
		 * @throws ALDProcessingDAGException
		 */
		public MTBImageByte toMTBImageByte(String file, int width, int height,
		    int color) throws ALDOperatorException, ALDProcessingDAGException {
				MTBImageByte imageToDraw = (MTBImageByte) MTBImage.createMTBImage(width,
				    height, 1, 1, 1, MTBImageType.MTB_BYTE);
				for (int i = 0; i < this.points.size(); i++) {
						Point2D.Double p = this.points.elementAt(i);
						imageToDraw.putValueDouble((int) Math.round(p.getX()), (int) Math.round(p
						    .getY()), color);
				}
				// save the region in a binary image
				if (file != null) {
						ImageWriterMTB writer = new ImageWriterMTB(imageToDraw, file);
						writer.setOverwrite(false);
						writer.runOp(null);
				}
				return imageToDraw;
		}

		/**
		 * Method to save a region with in a given MTBImage image. Color value of the
		 * region is set so image.getTypeMax().
		 * 
		 * @param file
		 *          path where the file should be saved, can be null if image should
		 *          not be stored at disk
		 * @param image
		 *          image where the region should be drawn in
		 * @return 2D region in a image of type MTBImage.
		 * 
		 * @throws ALDProcessingDAGException
		 * @throws ALDOperatorException
		 */
		public MTBImage toMTBImage(String file, MTBImage image)
		    throws ALDOperatorException, ALDProcessingDAGException {
				MTBImage imageToDraw = image.duplicate();
				for (int i = 0; i < this.points.size(); i++) {
						Point2D.Double p = this.points.elementAt(i);
						imageToDraw.putValueDouble((int) Math.round(p.getX()), (int) Math.round(p
						    .getY()), image.getTypeMax());
				}
				// save the region in the given image on the disk
				if (file != null) {
						ImageWriterMTB writer = new ImageWriterMTB(imageToDraw, file);
						writer.setOverwrite(false);
						writer.runOp(null);
				}
				return imageToDraw;
		}

		/**
		 * Method to get the contour object from the current 2D region. Outer and
		 * inner contours will be returned from the current region object. The image
		 * size, including the region, is calculated by the minimum and maximum x- and
		 * y-coordinate of the bounding box of the region.
		 * 
		 * @return Inner and outer contours of the given region.
		 * 
		 * @throws ALDOperatorException
		 */
		public MTBContour2D getContour() throws ALDOperatorException,
		    ALDProcessingDAGException {
				/*
				 * Calculate the bounding box to get the maximum x and y value of the
				 * region.
				 */
				double[] boundingBox = this.getBoundingBox();
				int imageSizeX = (int) Math.round(boundingBox[2] + 1);
				int imageSizeY = (int) Math.round(boundingBox[3] + 1);
				
				MTBRegion2DSet regions = new MTBRegion2DSet(0, 0, imageSizeX, imageSizeY);
				regions.add(this);
				ContourOnLabeledComponents clc = new ContourOnLabeledComponents(regions,
				    ContourType.OUT_IN_CONTOUR, 1);
				clc.runOp(null);
				return clc.getResultContours().elementAt(0);
		}

		/**
		 * Method to get the border object from the current 2D region, for example to
		 * visualize the regions. Contrary to contours the borders extracted by this
		 * operator contain <i>unordered</i> sets of pixels, but it is faster than
		 * extracting the contours. Outer and inner borders will be returned from the
		 * current region object. The image size, including the region, is calculated
		 * by the minimum and maximum x- and y-coordinate of the bounding box of the
		 * region.
		 * 
		 * @see BordersOnLabeledComponents
		 * 
		 * @return Inner and outer borders of the given region.
		 * 
		 * @throws ALDOperatorException
		 */
		public MTBBorder2D getBorder() throws ALDOperatorException,
		    ALDProcessingDAGException {
				/*
				 * Calculate the bounding box to get the maximum x and y value of the
				 * region.
				 */
				double[] boundingBox = this.getBoundingBox();
				int imageSizeX = (int) Math.round(boundingBox[2] + 1);
				int imageSizeY = (int) Math.round(boundingBox[3] + 1);
				
				MTBRegion2DSet regions = new MTBRegion2DSet(0, 0, imageSizeX, imageSizeY);
				regions.add(this);
				BordersOnLabeledComponents blc = new BordersOnLabeledComponents(null,
					regions, BorderConnectivity.CONNECTED_8, BorderType.OUT_IN_BORDERS, 1);
				blc.runOp(null);				
				return blc.getResultBorders().elementAt(0);
		}

		// /**
		// * TODO UNTESTED!!!
		// *
		// * @param regions
		// * @param imgW
		// * @param imgH
		// * @return
		// */
		// public static MTBImageRGB makeLabelImageRGB(Vector<MTBRegion2D> regions,
		// int imgW, int imgH) {
		// MTBImageRGB img = (MTBImageRGB) MTBImage.createMTBImage(imgW, imgH, 1, 1,
		// 1, MTBImageType.MTB_RGB);
		// // TreeSet<Integer> usedColors = new TreeSet<Integer>();
		//
		// Random randGen = new Random();
		// for (MTBRegion2D r : regions) {
		// // // get random color
		// // do {
		// // gray= randGen.nextInt(256*256);
		// // if (gray==0) gray= gray+1;
		// // } while (usedColors.contains(new Integer(gray)));
		// // usedColors.add(new Integer(gray));
		// // get random color
		// int red = randGen.nextInt(256);
		// int green = randGen.nextInt(256);
		// int blue = randGen.nextInt(256);
		// for (Point2D.Double p : r.getPoints())
		// img.putValue((int) p.x, (int) p.y, red, green, blue);
		// }
		// return img;
		// }

		/*
		 * >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
		 * 
		 * Calculation of moments p,q for the MTBREgion2D.
		 * 
		 * W. Burger, M. J. Burge: "Digitale Bildverarbeitung" Springer-Verlag, 2005
		 * www.imagingbook.com
		 */

		/**
		 * Calculate moment of the region of order p,q.
		 * <p>
		 * {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}}
		 * \\begin{equation*} 	
		 *    m_{p,q}(R) = \\sum_{(x,y) \\in R} x^p \\cdot y^q
		 * \\end{equation*}}
		 * 
		 * @param p
		 *          order of x-component
		 * @param q
		 *          oder of y-component
		 * @return Moment as double value.
		 */
		@SuppressWarnings("javadoc")
    public double getMoment(int p, int q) {
				double Mpq = 0.0;
				for (int i = 0; i < this.area; i++) {
						Point2D.Double pt = this.points.elementAt(i);
						Mpq += Math.pow(pt.x, p) * Math.pow(pt.y, q);
				}
				return Mpq;
		}

		/**
		 * Calculate central moment of the region of order p,q.
		 * <p>
		 * {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}}
		 * \\begin{eqnarray*} 	
		 * \\mu_{p,q}(R)&=&\\sum_{(x,y)\\in R} (x-\\mu_x)^p\\cdot (y-\\mu_y)^q \\\\
		 * \\mu_x &=& \\frac{1}{|R|} \\sum_{(x,y) \\in R} x \\\\
		 * \\mu_y &=& \\frac{1}{|R|} \\sum_{(x,y) \\in R} y
		 * \\end{eqnarray*}}
		 * 
		 * @param p
		 *          order of x-component
		 * @param q
		 *          oder of y-component
		 * @return Central moment as double value.
		 */
		@SuppressWarnings("javadoc")
    public double getCentralMoment(int p, int q) {
				double m00 = this.getMoment(0, 0);
				double xCtr = this.getMoment(1, 0) / m00;
				double yCtr = this.getMoment(0, 1) / m00;
				double cMpq = 0.0;
				for (int i = 0; i < this.area; i++) {
						Point2D.Double pt = this.points.elementAt(i);
						cMpq += Math.pow(pt.x - xCtr, p) * Math.pow(pt.y - yCtr, q);
				}
				return cMpq;
		}

		/**
		 * Calculate normalized central moment of the region of order p,q.
		 * <p>
		 * {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}}
		 * \\begin{eqnarray*} 	
		 * \\mu_{p,q}(R)&=& 
		 * 		\\frac{1}{m_{0,0}(R)^{\\frac{p+q+2}{2}}} \\cdot \\mu_{p,q}(R)
		 * \\end{eqnarray*}}
		 * 
		 * @param p
		 *          order of x-component
		 * @param q
		 *          oder of y-component
		 * @return Normalized central moment as double value.
		 */
		@SuppressWarnings("javadoc")
    public double getNormalCentralMoment(int p, int q) {
				double m00 = this.getMoment(0, 0);
				double norm = Math.pow(m00, (double) (p + q + 2) / 2);
				return this.getCentralMoment(p, q) / norm;
		}

		/**
		 * Calculates the orientation of the principal axis of the 
		 * {@link MTBRegion2D.}
		 * <p>
		 * The orientation is measured with regard to the horizontal x-axis.<br> 
		 * We apply the atan2 function here:
		 * 
		 * {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}}
		 * \\begin{eqnarray*} 	
		 * \\theta(R)&=& \\frac{1}{2} \\cdot 
		 * 		atan2(2.0 \\cdot \\mu_{1,1}, \\mu_{2,0} - \\mu_{0,2} )  
		 * \\end{eqnarray*}}
		 * 
		 * Thus, the orientation (angle) is given in radians in a range of 
		 * [-pi, +pi].<br>
		 * Please note that two angles with a difference of 
		 * 180 degrees (or pi) are equivalent and cannot be distinguished. 
		 * E.g., axes with orientations 0 and 180 degrees are identical.
		 * <p>
		 * For details on the calculations and some background infos take a look
		 * in Burger/Burge, <i>Digitale Bildverarbeitung</i>, 2nd edition, 
		 * Springer, 2006, section "Orientierung" on pages 224 to 226.
		 * 
		 * @return Orientation of region main axis in radians.
		 */
		@SuppressWarnings("javadoc")
    public double getOrientation() {
				// calculate different moments
				double m11 = this.getCentralMoment(1, 1);
				double m20 = this.getCentralMoment(2, 0);
				double m02 = this.getCentralMoment(0, 2);
				// calculate orientation of principal axis
				double ori = 0.5 * Math.atan2((2.0 * m11), (m20 - m02));
				return ori;
		}

		/**
		 * Calculates eccentricity of the region in the range of [0,1].
		 * <p>
		 * The eccentricity is calculated according to the following equation:
		 * {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}}
		 * \\begin{eqnarray*} 	
		 * e(R) &=& \\frac{(\\mu_{2,0} - \\mu_{0,2})^2 + 4 \\cdot \\mu_{1,1}^2}
		 *                {(\\mu_{2,0} + \\mu_{0,2})^2}
		 * \\end{eqnarray*}}
		 * <p>
		 * 1 refers to a sustained region.
		 * <p>
		 * Reference:
		 * <i>Burger/Burge, Digitale Bildverarbeitung, Springer, 2006, pp. 227</i>.
		 * 
		 * @return Eccentricity as double value.
		 */
		@SuppressWarnings("javadoc")
    public double getEccentricity() {
				// calculate different moments
				double m11 = this.getCentralMoment(1, 1);
				double m20 = this.getCentralMoment(2, 0);
				double m02 = this.getCentralMoment(0, 2);
				// calculate eccentricity
				double ecc = (Math.pow((m20 - m02), 2) + 4 * Math.pow(m11, 2))
				    / (Math.pow((m20 + m02), 2));
				return ecc;
		}

		/*
		 * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
		 */

		/**
		 * Calculates circularity of given region.
		 * <p>
		 * The circularity is calculated according to the following equation:
		 * {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}}
		 * \\begin{eqnarray*} 	
		 * c(R) &=& \\frac{4 \\cdot \\pi \\cdot A(R)}{U(R) \\cdot U(R)} \\\\
		 * A(R) &:=& \\text{area of the region}\\\\ 
		 * U(R) &:=& \\text{perimeter of the region}
		 * \\end{eqnarray*}}
		 * <p>
		 * A perfect circle yields 1, otherwise smaller values result.
		 * <p>
		 * Reference:
		 * <i>Burger/Burge, Digitale Bildverarbeitung, Springer, 2006, pp. 220</i>.
		 * 
		 * @return Circularity of the region.
		 * @throws ALDOperatorException 
		 * @throws ALDProcessingDAGException 
		 */
		@SuppressWarnings("javadoc")
    public double getCircularity() throws ALDOperatorException,
		    ALDProcessingDAGException {
				MTBContour2D contour = getContour();
				double perimeter = contour.getContourLength();
				if (perimeter != 0) {
						return (4 * Math.PI * this.area) / (perimeter * perimeter);
				}
				return 0;
		}

		/**
		 * Calculates corrected circularity of given region.
		 * <p>
		 * The circularity is calculated according to the following equation:
		 * {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}}
		 * \\begin{eqnarray*} 	
		 * c(R) &=& \\frac{4 \\cdot \\pi \\cdot A(R)}{U(R) \\cdot U(R)} \\\\
		 * A(R) &:=& \\text{area of the region}\\\\ 
		 * U(R) &:=& \\text{perimeter of the region}
		 * \\end{eqnarray*}}
		 * <p>
		 * A perfect circle yields 1, otherwise smaller values result.
		 * <p> 
		 * Compared to the {@link #getCircularity()} method here a corrected version
		 * of the region's perimeter is used as suggested for small regions in 
		 * <i>Burger/Burge, Digitale Bildverarbeitung, Springer, 2006, pp. 219</i>.
		 * 
		 * @return Corrected circularity of the region.
		 * @throws ALDProcessingDAGException
		 * @throws ALDOperatorException
		 */
		@SuppressWarnings("javadoc")
    public double getCorrCircularity() throws ALDOperatorException,
		    ALDProcessingDAGException {
				MTBContour2D contour = getContour();
				double perimeter = (0.95 * contour.getContourLength());
				if (perimeter != 0) 
						return (4 * Math.PI * this.area) / (perimeter * perimeter);
				return 0;
		}

		/**
		 * Calculates the axes-parallel bounding box of the region.
		 * <p>
		 * The function extracts the coordinates of the upper left and lower right
		 * corner of the bounding box of the region. Note that the there is at least
		 * one point of the region lying on each side of the bounding box, i.e. the
		 * region not just touches the box, but lies on it.
		 * <p>
		 * The result array contains the corner coordinates in the following order:
		 * [xmin, ymin, xmax, ymax]
		 * 
		 * @return Coordinates of upper left and lower right corners.
		 */
		public double[] getBoundingBox() {
				double minX = Double.MAX_VALUE;
				double maxX = 0;
				double minY = Double.MAX_VALUE;
				double maxY = 0;

				for (Point2D.Double p : this.points) {
						if (p.x > maxX)
								maxX = p.x;
						if (p.x < minX)
								minX = p.x;
						if (p.y > maxY)
								maxY = p.y;
						if (p.y < minY)
								minY = p.y;
				}
				double[] bbox = new double[4];
				bbox[0] = minX;
				bbox[1] = minY;
				bbox[2] = maxX;
				bbox[3] = maxY;
				return bbox;
		}
		
		
		/**
		 * Calculates length of the major axis of the ellipse best fitting.
		 * <p>
		 * The method is based on using moments of the region:
		 * {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}}
		 * \\begin{eqnarray*} 	
		 * a(R) &=& 2 \\cdot \\sqrt{
		 * 	\\frac{2 \\cdot (\\mu_{2,0} + \\mu_{0,2} + 
		 * 					\\sqrt{ (\\mu_{2,0} - \\mu_{0,2})^2 + 4 \\cdot \\mu_{1,1}^2 })}
		 *        {\\mu_{0,0}}}
		 * \\end{eqnarray*}}
		 * <p>
		 * Reference:
		 * <i>R. Prokop/A. Reeves, A Survey of Moment-Based Techniques for 
		 * Unoccluded Object Representation and Recognition, Graphical Models and 
		 * Image Processing (CVGIP), pp. 438-460, 1992</i>.
		 * 
		 * @return length of major axis of best fitting ellipse
		 */
		@SuppressWarnings("javadoc")
    public double getMajorAxisLength()
		{
			double u00 = this.getCentralMoment(0, 0);
			double u11 = this.getCentralMoment(1, 1);
			double u02 = this.getCentralMoment(0, 2);
			double u20 = this.getCentralMoment(2, 0);
			
			return 2 * Math.sqrt(2 * (u20 + u02 + Math.sqrt(4 * u11 * u11 + (u20 - u02) * (u20 - u02))) / u00);
		}
		
		/**
		 * Calculates length of the minor axis of the ellipse best fitting.
		 * <p>
		 * The method is based on using moments of the region:
		 * {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}}
		 * \\begin{eqnarray*} 	
		 * b(R) &=& 2 \\cdot \\sqrt{
		 * 	\\frac{2 \\cdot (\\mu_{2,0} + \\mu_{0,2} - 
		 * 					\\sqrt{ (\\mu_{2,0} - \\mu_{0,2})^2 + 4 \\cdot \\mu_{1,1}^2 })}
		 *        {\\mu_{0,0}}}
		 * \\end{eqnarray*}}
		 * <p>
		 * Reference:
		 * <i>R. Prokop/A. Reeves, A Survey of Moment-Based Techniques for 
		 * Unoccluded Object Representation and Recognition, Graphical Models and 
		 * Image Processing (CVGIP), pp. 438-460, 1992</i>.
		 * 
		 * @return Length of minor axis of ellipse best fitting the region.
		 */
		@SuppressWarnings("javadoc")
    public double getMinorAxisLength()
		{
			double u00 = this.getCentralMoment(0, 0);
			double u11 = this.getCentralMoment(1, 1);
			double u02 = this.getCentralMoment(0, 2);
			double u20 = this.getCentralMoment(2, 0);
			return 2 * Math.sqrt(2 * (u20 + u02 - 
					Math.sqrt(4 * u11 * u11 + (u20 - u02) * (u20 - u02) ) ) / u00);
		}

		/**
		 * Create a random connected 2D region of size <code>maxArea</code> in a hypothetical image of
		 * a random size between <code>1</code> and <code>maxArea</code>.
		 * 
		 * @param xSize
		 * @param ySize
		 * @param maxArea
		 * @return
		 */
		public static MTBRegion2D createRandomRegion2D(int xSize, int ySize, int maxArea, Random rndGen) {
			MTBRegion2D region2D = new MTBRegion2D();
			int area = rndGen.nextInt(maxArea+1) + 1;
			int x = rndGen.nextInt(xSize);
			int y = rndGen.nextInt(ySize);
			
			MTBRegion2D.growRandomRegion2D( x, y, xSize, ySize, area, region2D, rndGen);
			return region2D;
		}

		/**
		 * Grow a 2D region adding the pixel specified and potentially grow further
		 * 
		 * @param x x coordinates of pixel to add
		 * @param y y coordinate of pixel to add
		 * @param area design area of region, however: resulting region may be smaller 
		 * @param region2D current partial region to grow
		 * @param rndGen random number generator
		 */
		private static void growRandomRegion2D(int x, int y, int xSize, int ySize, int area, MTBRegion2D region2D,
				Random rndGen) {
			region2D.addPixel(x, y);
			
			MTBTopologicalNumber2DN8 nb = new MTBTopologicalNumber2DN8();
			Iterator<Point3D>  offsets = nb.iteratorOffsets();
			while ( offsets.hasNext()) {
				if ( region2D.getArea() >= area )
					return;

				Point3D offset = offsets.next();
				int nextX = x + offset.getX();
				int nextY = y + offset.getY();
				
				Point2D.Double nextPoint = new Point2D.Double(nextX, nextY);
				if ( nextX >= 0 && nextX < xSize && nextY >= 0 && nextY < ySize &&
						! region2D.contains(nextPoint)) {
					// accept a new point with probability 0.8
					if ( rndGen.nextFloat() <= 0.80 )
						MTBRegion2D.growRandomRegion2D(nextX, nextY, xSize, ySize, area, region2D, rndGen);
				}
			}
		}

		@Override
    public String toString() {
			int size = 0;
			if (this.points != null)
				size = this.points.size();
			return new String("Region[" + this.id + "] : " + size + " points.");
		}
		
		/**
		 * Function to update object state after setting new point list.
		 * <p>
		 * It is assumed that point list is up-to-date before calling the hook.
		 */
		protected void hookPointsUpdated() {
			this.com_x = 0;
			this.com_y = 0;
			if (this.points != null) {
				this.area = this.points.size();
				for (int i = 0; i < this.points.size(); ++i) {
					this.com_x += this.points.elementAt(i).x;
					this.com_y += this.points.elementAt(i).y;
				}
			}
			else {
				this.area = 0;
			}
		}
}
