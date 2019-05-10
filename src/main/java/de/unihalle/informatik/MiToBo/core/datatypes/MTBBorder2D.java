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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Vector;

import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.operator.ALDData;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.exceptions.MTBDatatypeException;
import de.unihalle.informatik.MiToBo.io.images.ImageWriterMTB;

/**
 * Datatype to represent borders of components.
 * <p>
 * The border of a component is basically equal to its contours, however,
 * the pixels are not arranged in a special ordering. In particular they are
 * not ordered clockwise or counter-clockwise. Due to this the extraction is 
 * faster than using classical contour extraction algorithms. 
 * 
 * @author moeller
 */
@ALDParametrizedClass
public class MTBBorder2D extends ALDData implements Cloneable {

	/**
	 * Kind of neighborhood to be applied to border pixels.
	 * 
	 * @author moeller
	 */
	public static enum BorderConnectivity {
		/**
		 * Border pixels are connected by 4-neighborhood.
		 */
		CONNECTED_4,
		/**
		 * Border pixels are connected by 8-neighborhood.
		 */
		CONNECTED_8
	}
	
	/**
	 * Set of border pixels.
	 */
	@ALDClassParameter(label="Point list", changeValueHook="hookPointsUpdated")
	protected Vector<Point2D.Double> points;

	/**
	 * Set of inner borders if available.
	 */
	@ALDClassParameter(label="Set of inner borders")
	protected Vector<MTBBorder2D> inner;

	/**
	 * The number of border points.
	 */
	protected int pointNum;
	
	/**
	 * Connectivity of border pixels.
	 * <p>
	 * If nothing is specified, 8-connectivity is assumed.
	 */
	@ALDClassParameter(label="Connectivity")
	protected BorderConnectivity connectivity = BorderConnectivity.CONNECTED_8;

	/**
	 * Standard constructor. Creates an empty 2D border object.
	 */
	public MTBBorder2D() {
		this.points = new Vector<Point2D.Double>();
		this.inner = new Vector<MTBBorder2D>();
		this.pointNum = 0;
		this.connectivity = BorderConnectivity.CONNECTED_8;
	}

	/**
	 * Constructor to create a 2D border object from a 2D point vector.
	 * @param _points		Vector with 2D points.
	 * @param bc				Type of connectivity within point list.
	 */
	public MTBBorder2D(Vector<Point2D.Double> _points, BorderConnectivity bc) {
		this.points = _points;
		this.inner = new Vector<MTBBorder2D>();
		this.pointNum = _points.size();
		this.connectivity = bc;
	}

	@Override
	public MTBBorder2D clone() {
		Vector<Point2D.Double> clonedPoints = new Vector<Point2D.Double>();
		for (Point2D.Double p: this.points)
			clonedPoints.add((Point2D.Double)(p.clone()));
		MTBBorder2D tmpBorder = new MTBBorder2D(clonedPoints, this.connectivity);
		tmpBorder.pointNum = this.pointNum;
		Vector<MTBBorder2D> clonedInnerBorders = new Vector<MTBBorder2D>();
		for (MTBBorder2D ib: this.inner) {
			MTBBorder2D clonedBorder = ib.clone();
			clonedInnerBorders.add(clonedBorder);
		}
		try {
	    tmpBorder.setInner(clonedInnerBorders);
    } catch (MTBDatatypeException e) {
    	System.err.println("[MTBBorder2D] Cloning border failed...");
    	e.printStackTrace();
    	return null;
    }
		return tmpBorder;
	}

	/**
	 * Get all points belonging to the border object.
	 * @return Vector with all 2D points of the border.
	 */
	public Vector<Point2D.Double> getPoints() {
		return this.points;
	}

	/**
	 * Get a specific 2D point belonging to the border object.
	 * @param index 	Index of requested point.
	 * @return 2D point at specific index.
	 */
	public Point2D.Double getPointAt(int index) {
		return this.points.elementAt(index);
	}

	/**
	 * Get the number of points of the border object.
	 * @return Number of border points.
	 */
	public int getPointNum() {
		return this.pointNum;
	}

	/**
	 * Query the connectivity of pixels within border.
	 * @return	Connectivity, either 4- or 8-neighborhood.
	 */
	public BorderConnectivity getConnectivity() {
		return this.connectivity;
	}
	
	/**
	 * Set the connectivity.
	 * @param bc	Connectivity of border.
	 */
	public void setConnectivity(BorderConnectivity bc) {
		this.connectivity = bc;
	}

	/**
	 * Add a 2D point to the existing border.
	 * @param x		x-coordinate of the new point.
	 * @param y  y-coordinate of the new point.
	 */
	public void addPixel(int x, int y) {
		this.pointNum++;
		this.points.addElement(new Point2D.Double(x, y));
	}

	/**
	 * Set the inner borders of the border object.
	 * @param ic	Vector with inner borders.
	 * @throws MTBDatatypeException Thrown in case of failure.
	 */
	@SuppressWarnings("unused")
  public void setInner(Vector<MTBBorder2D> ic) throws MTBDatatypeException {
		this.inner = ic;
	}

	/**
	 * Add an inner border to the existing border object.
	 * @param ic	New inner 2D border.
	 * @throws MTBDatatypeException Thrown in case of failure.
	 */
	@SuppressWarnings("unused")
  public void addInner(MTBBorder2D ic) throws MTBDatatypeException {
		this.inner.addElement(ic);
	}

	/**
	 * Get a specific inner border.
	 * @param index		Index of desired inner border.
	 * @return Inner border with given index.
	 */
	public MTBBorder2D getInner(int index) {
		return this.inner.elementAt(index);
	}

	/**
	 * Get all inner borders from the border object.
	 * @return All inner borders.
	 */
	public Vector<MTBBorder2D> getAllInnerBorders() {
		return this.inner;
	}

	/**
	 * Get the number of inner borders included.
	 * @return The number of inner borders.
	 */
	public int countInner() {
		return this.inner.size();
	}

	/**
	 * Draws a border with all inner borders to a binary image with given size.
	 * 
	 * @param file 			Path where the file should be saved, can be null.
	 * @param width			Width of the binary image.
	 * @param height		Height of the binary image.
	 * @return Image with borders.
	 * 
	 * @throws ALDOperatorException				Thrown in case of failure.
	 * @throws ALDProcessingDAGException	Thrown in case of failure.
	 */
	public MTBImageByte toMTBImageByte(String file, int width, int height)
			throws ALDOperatorException, ALDProcessingDAGException {
		MTBImageByte imageToDraw = (MTBImageByte) MTBImage.createMTBImage(width,
				height, 1, 1, 1, MTBImageType.MTB_BYTE);
		for (int i = 0; i < this.points.size(); ++i) {

			Point2D.Double p = this.points.elementAt(i);
			imageToDraw.putValueDouble((int) Math.round(p.getX()), (int) Math.round(p
					.getY()), 255);
		}
		int end = this.inner.size();
		for (int j = 0; j < end; j++) {
			MTBBorder2D c = this.inner.elementAt(j);
			Vector<Point2D.Double> in_points = c.getPoints();
			for (int k = 0; k < in_points.size(); k++) {
				Point2D.Double p = in_points.elementAt(k);
				imageToDraw.putValueDouble((int) Math.round(p.getX()), (int) Math
						.round(p.getY()), 255);
			}
		}
		if (file != null) {

			ImageWriterMTB writer = new ImageWriterMTB(imageToDraw, file);
			writer.setOverwrite(false);
			writer.runOp(null);

		}
		return imageToDraw;
	}

	/**
	 * Draws border with all inner borders to a given image.
	 * 
	 * @param file		Path where the file should be saved, can be null.
	 * @param image		Image to where the borders should be drawn.
	 * @return Image with borders.
	 * 
	 * @throws ALDOperatorException				Thrown in case of failure.
	 * @throws ALDProcessingDAGException	Thrown in case of failure.
	 */
	public MTBImage toMTBImage(String file, MTBImage image)
			throws ALDOperatorException, ALDProcessingDAGException {
		MTBImage imageToDraw = image.duplicate();
		for (int i = 0; i < this.points.size(); ++i) {
			Point2D.Double p = this.points.elementAt(i);
			imageToDraw.putValueDouble((int) Math.round(p.getX()), (int) Math.round(p
					.getY()), image.getTypeMax());
		}
		int end = this.inner.size();
		for (int j = 0; j < end; j++) {
			MTBBorder2D c = this.inner.elementAt(j);
			Vector<Point2D.Double> in_points = c.getPoints();
			for (int k = 0; k < in_points.size(); k++) {
				Point2D.Double p = in_points.elementAt(k);
				imageToDraw.putValueDouble((int) Math.round(p.getX()), (int) Math
						.round(p.getY()), image.getTypeMax());
			}
		}
		if (file != null) {
			ImageWriterMTB writer = new ImageWriterMTB(imageToDraw, file);
			writer.setOverwrite(false);
			writer.runOp(null);
		}
		return imageToDraw;
	}

	/**
	 * Calculates the axes-parallel bounding box of the contour.
	 * <p>
	 * The function extracts the coordinates of the upper left and lower right
	 * corner of the bounding box of the border. Note that the there is at least
	 * one point of the border lying on each side of the bounding box, i.e. the
	 * border not just touches the box, but lies on it.
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
	 * Read a 2D border object from an ASCII file in xSV format.
	 * <p>
	 * The file is expected to contain a single point in each row, first
	 * the x-coordinate and then the y-coordinate. Both coordinates should
	 * be separated by the delimiter character, e.g. ',' or ' '.  
	 * 
	 * @param file			File name from where to read the points.
	 * @param delim			Delimiter in the file.
	 * @param skipLines	Number of header lines to skip.
	 * @return Border object.
	 */
	public static MTBBorder2D readBorderFromASCIIFile(
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
			return new MTBBorder2D(pointVec, BorderConnectivity.CONNECTED_8);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
	}


	/**
	 * Function to update object state after setting new point list.
	 * <p>
	 * It is assumed that point list is up-to-date before calling the hook.
	 */
	protected void hookPointsUpdated() {
		if (this.points != null) {
			this.pointNum = this.points.size();
		}
		else {
			this.pointNum = 0;
		}
	}

}
