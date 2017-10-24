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

package de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes;

import java.awt.geom.*;
import java.util.Vector;

import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.*;
import de.unihalle.informatik.MiToBo.core.datatypes.defines.MTBConstants;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.segmentation.basics.*;
import de.unihalle.informatik.MiToBo_xml.MTBXMLPolygon2DType;
import de.unihalle.informatik.MiToBo_xml.MTBXMLSnakeType;

/**
 * Active Contour (Snake) datatype.
 * <p>
 * The snake is implemented with its control points and a large variety of
 * convenience methods for using the snakes, e.g., methods to plot the snake to
 * an image. A snake can be open or closed. In the latter case it also has an
 * orientation, i.e., the snake points are ordered clockwise or
 * counter-clockwise.
 * <p>
 * The snake interior is always located left of the snake segments. In case of a
 * closed snake in counter-clockwise ordering the area enclosed by the snake
 * polygon is defined as its interior. In case of counter-clockwise ordering the
 * enclosed region is the exterior part of the domain or the background,
 * respectively.
 * <p>
 * The snake datatype implements the {@link MTBSegmentationInterface}, i.e.,
 * allows for querying if certain pixels belong to foreground/interior or
 * background/exterior. Note that the interior or foreground class is always
 * labeled with 1, the background or exterior region always with 0.
 * <p>
 * A pixel or position in the domain of the snake can be visible or not. The
 * visibility is stored in an internal visibility map. Note that this map can
 * be altered from external and does not need to have the same size as the 
 * snakes domain. 
 * 
 * @author misiak, moeller
 */
@ALDDerivedClass
public class MTBSnake extends MTBPolygon2D implements MTBSegmentationInterface {

		/**
		 * Scale factor of the snake for normalization.
		 */
		private double scaleFactor = 1.0;

		/**
		 * Marker for label of enclosed area.
		 */
		private int labelInside = 1;

		/**
		 * 2D map marking the region enclosed by the snake.
		 * <p>
		 * Note that the enclosed region contains always values of 1, independent of
		 * the order of the snake. The getClass()-methods take care of returning
		 * correct class labels.
		 */
		protected int[][] classmap = null;

		/**
		 * Minimal x coordinate of class map.
		 */
		protected int mapMinX = 0;

		/**
		 * Maximal x coordinate of class map.
		 */
		protected int mapMaxX = 0;

		/**
		 * Minimal y coordinate of class map.
		 */
		protected int mapMinY = 0;

		/**
		 * Maximal y coordinate of class map.
		 */
		protected int mapMaxY = 0;

		/**
		 * Width of class map.
		 */
		protected int mapWidth = 0;

		/**
		 * Height of class map.
		 */
		protected int mapHeight = 0;

		/**
		 * Visibility map for hiding individual pixels.
		 */
		protected boolean[][] visiblemap = null;

		/**
		 * Minimal x coordinate of visibility map.
		 */
		protected int visibleMapMinX = 0;

		/**
		 * Maximal x coordinate of visibility map.
		 */
		protected int visibleMapMaxX = 0;

		/**
		 * Minimal y coordinate of visibility map.
		 */
		protected int visibleMapMinY = 0;

		/**
		 * Maximal y coordinate of visibility map.
		 */
		protected int visibleMapMaxY = 0;

		/**
		 * Width of visibility map.
		 */
		protected int visibilityMapWidth = 0;

		/**
		 * Height of visibility map.
		 */
		protected int visibilityMapHeight = 0;

		/**
		 * Standard constructor to create a new and empty snake object.
		 */
		public MTBSnake() {
			this.points = new Vector<Point2D.Double>();
			this.isClosed = false;
		}

		/**
		 * Constructor to create a new Snake object.
		 * 
		 * @param spoints
		 *          vector of 2D snake points as control points for the snake
		 * @param cycle
		 *          true if snake forms a closed polygon
		 */
		public MTBSnake(Vector<MTBSnakePoint2D> spoints, boolean cycle) {
				this.isClosed = cycle;
				this.points = new Vector<Point2D.Double>(spoints.size());
				for (int i = 0; i < spoints.size(); i++) {
						this.points.addElement(spoints.elementAt(i));
				}
				// init the class/visibility map
				if (!this.points.isEmpty()) {
						this.calcSnakeDomain();
						this.updateSnakeSegmentation();
				} else {
						System.err.println("MTBSnake: empty snake initialized!");
				}
		}

		/**
		 * Constructor to create a new Snake object.
		 * 
		 * @param spoints
		 *          vector of 2D snake points as control points for the snake
		 * @param cycle
		 *          true if snake forms a closed polygon
		 * @param scale
		 *          scaling factor
		 * @param isScaled
		 *          true if snake is already scaled
		 */
		public MTBSnake(Vector<MTBSnakePoint2D> spoints, boolean cycle, double scale,
		    boolean isScaled) {
				this.points = new Vector<Point2D.Double>(spoints.size());
				for (int i = 0; i < spoints.size(); i++) {
						this.points.addElement(spoints.elementAt(i));
				}
				this.isClosed = cycle;
				this.scaleFactor = scale;
				if (!this.points.isEmpty()) {
						if (!isScaled)
								this.normalize(scale);
						// intialize segmentation domain with snake bounding box
						this.calcSnakeDomain();
						// init the class/visibility map
						this.updateSnakeSegmentation();
				} else {
						System.err.println("MTBSnake: empty snake initialized!");
				}
		}

		/**
		 * Convert object to XML representation.
		 * <p>
		 * Copy the information of this object into the corresponding xml
		 * element <code>xmlSnake</code>. If <code>xmlSnake</code> is null, a new
		 * object is created, otherwise the passed object filled.
		 * 
		 * @param xmlSnake 	Object to be filled.
		 * @return Filled or newly created XML object.
		 */
		public MTBXMLSnakeType toXMLType(MTBXMLSnakeType xmlSnake) {
			MTBXMLSnakeType rsnake = xmlSnake;
			if (rsnake == null)
				rsnake = MTBXMLSnakeType.Factory.newInstance();

			rsnake = (MTBXMLSnakeType)super.toXMLType(rsnake);
			rsnake.setScaleFactor(this.getScaleFactor());
			return rsnake;
		}
		
		/**
		 * Get the scaling factor of the snake.
		 * 
		 * @return Scaling factor.
		 */
		public double getScaleFactor() {
				return this.scaleFactor;
		}

		/**
		 * Override java.lang.Object.clone() to create and return a copy of this
		 * object.
		 * 
		 * @throws ALDProcessingDAGException
		 * @throws ALDOperatorException
		 */
		@Override
		public MTBSnake clone() {
				try {
						SnakeCloner sc = new SnakeCloner(this);
						sc.runOp(true);
						MTBSnake clone = sc.getOutputSnake();
						return clone;
				} catch (ALDOperatorException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
				} catch (ALDProcessingDAGException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
				}
				return null;
		}

		/**
		 * Overwrite Polygon2D method to set all points of the snake from the
		 * specified point vector object. Intern a vector of SnakePoint2D object is
		 * created. The old position of every point within the snake is set to -1 by
		 * default.
		 */
		@Override
		public void setPoints(Vector<Point2D.Double> ps) {
			this.points = new Vector<Point2D.Double>(ps.size());
			for (int i = 0; i < ps.size(); i++) {
				MTBSnakePoint2D tmpPoint = new MTBSnakePoint2D(ps.elementAt(i));
				this.points.addElement(tmpPoint);
			}
			// update segmentation data
			this.calcSnakeDomain();
			this.updateSnakeSegmentation();
		}

		/**
		 * Set all points of the snake from the specified SnakePoint2D vector object.
		 * The old position of every point within the snake is kept.
		 */
		public void setSnakePoints(Vector<MTBSnakePoint2D> ps) {
			this.points = new Vector<Point2D.Double>(ps.size());
			for (int i = 0; i < ps.size(); i++) {
				this.points.addElement(ps.elementAt(i));
			}
			// update segmentation data
			this.calcSnakeDomain();
			this.updateSnakeSegmentation();
		}

		/**
		 * Overwrites Polygon2D method. Appends a new point to the end of the snake.
		 * Old position of the point is set to -1 by default.
		 * 
		 * @param x
		 *          Point x coordinate.
		 * @param y
		 *          Point y coordinate.
		 */
		@Override
		public void addPoint(double x, double y) {
			MTBSnakePoint2D p = new MTBSnakePoint2D(x, y);
			this.points.add(p);
			// update segmentation data
			this.calcSnakeDomain();
			this.updateSnakeSegmentation();
		}

		/**
		 * Get snake points as vector of SnakePoint2D objects.
		 * 
		 * @return Vector with 2D snake points.
		 */
		@SuppressWarnings("unchecked")
		public Vector<MTBSnakePoint2D> getSnakePoints() {
				return (Vector<MTBSnakePoint2D>) this.points.clone();
		}

	/**
	 * Normalizes the snake coordinates.
	 * <p>
	 * All snake points are scaled by the given scale factor, i.e. divided by
	 * the given scale. A factor larger than 1.0 shrinks the snake, a factor 
	 * smaller than 1.0 expands it, and the default factor of 1.0 does not change 
	 * anything. The factor is stored internally for later de-normalization of 
	 * the snake.
	 * <p>
	 * Note that the scaling does not affect the snake's domain nor its 
	 * visibility maps.
	 * 
	 * @param scale	Scale factor, i.e. divisor, for normalization.
	 */
	public void normalize(double scale) {
		this.scaleFactor = scale;
		for (Point2D.Double p : this.points) {
			p.x = p.x / scale;
			p.y = p.y / scale;
		}
	}

		/**
		 * Denormalizes the snake coordinates.
		 * <p>
		 * The internally stored scale factor is used for the denormalization and
		 * subsequently set to 1.0.
		 */
		public void denormalize() {
				for (Point2D.Double p : this.points) {
						p.x = p.x * this.scaleFactor;
						p.y = p.y * this.scaleFactor;
				}
				this.scaleFactor = 1.0;
		}

		/**
		 * Get approximation of partial derivative in x-direction at point with given
		 * index.
		 * <p>
		 * For calculating partial derivatives point differences are used, i.e.
		 * forward differences. Note, that the partial derivative for the last point
		 * of a non-closed snake is always zero!
		 * 
		 * @param position
		 *          Index of snake point.
		 * @return Value of discrete approximation of x-derivative.
		 */
		public double getPartialDiffX(int position) {
				if (position == this.points.size() - 1 && !this.isClosed)
						return 0;
				if (position == this.points.size() - 1)
						return this.points.get(0).x - this.points.get(position).x;
				return this.points.get(position + 1).x - this.points.get(position).x;
		}

		/**
		 * Get approximation of partial derivative in y-direction at point with given
		 * index.
		 * <p>
		 * For calculating partial derivatives point differences are used, i.e.
		 * forward differences. Note, that the partial derivative for the last point
		 * of a non-closed snake is always zero!
		 * 
		 * @param position
		 *          Index of snake point.
		 * @return Value of discrete approximation of y-derivative.
		 */
		public double getPartialDiffY(int position) {
				if (position == this.points.size() - 1 && !this.isClosed)
						return 0;
				if (position == this.points.size() - 1)
						return this.points.get(0).y - this.points.get(position).y;
				return this.points.get(position + 1).y - this.points.get(position).y;
		}

		/**
		 * Get approximation of 2nd order partial derivative in x-direction.
		 * <p>
		 * For calculating partial derivatives point differences are used, i.e.
		 * forward differences. Note, that the partial derivatives for the first and
		 * last point of a non-closed snake are always zero!
		 * 
		 * @param position
		 *          Index of snake point.
		 * @return Value of discrete approximation of 2nd order x-derivative.
		 */
		public double getSndPartialDiffX(int position) {
				double prev_x, this_x, next_x;

				// get x-coordinate of current snake point
				this_x = this.points.get(position).x;

				// if snake is not closed, derivatives at the ends are not well-defined
				if (!this.isClosed) {

						// begin of snake
						if (position == 0)
								prev_x = this_x;
						else
								prev_x = this.points.get(position - 1).x;

						// end of snake
						if (position == this.points.size() - 1)
								next_x = this_x;
						else
								next_x = this.points.get(position + 1).x;
				}
				// if snake is closed, begin == end
				else {
						if (position == this.points.size() - 1)
								next_x = this.points.get(0).x;
						else
								next_x = this.points.get(position + 1).x;
						if (position == 0)
								prev_x = this.points.get(this.points.size() - 1).x;
						else
								prev_x = this.points.get(position - 1).x;
				}
				return next_x - 2 * this_x + prev_x;
		}

		/**
		 * Get approximation of 2nd order partial derivative in y-direction.
		 * <p>
		 * For calculating partial derivatives point differences are used, i.e.
		 * forward differences. Note, that the partial derivatives for the first and
		 * last point of a non-closed snake are always zero!
		 * 
		 * @param position
		 *          Index of snake point.
		 * @return Value of discrete approximation of 2nd order y-derivative.
		 */
		public double getSndPartialDiffY(int position) {
				double prev_y, this_y, next_y;

				// get x-coordinate of current snake point
				this_y = this.points.get(position).y;

				// if snake is not closed, derivatives at the ends are not well-defined
				if (!this.isClosed) {

						// begin of snake
						if (position == 0)
								prev_y = this_y;
						else
								prev_y = this.points.get(position - 1).y;

						// end of snake
						if (position == this.points.size() - 1)
								next_y = this_y;
						else
								next_y = this.points.get(position + 1).y;
				}
				// if snake is closed, begin == end
				else {
						if (position == this.points.size() - 1)
								next_y = this.points.get(0).y;
						else
								next_y = this.points.get(position + 1).y;
						if (position == 0)
								prev_y = this.points.get(this.points.size() - 1).y;
						else
								prev_y = this.points.get(position - 1).y;
				}
				return next_y - 2 * this_y + prev_y;
		}

		/**
		 * Returns the center of mass of the snake in x-direction.
		 */
		public double getCOMx() {
				double comx = 0;
				for (Point2D.Double p : this.points)
						comx += p.x;
				return comx / this.points.size();
		}

		/**
		 * Returns the center of mass of the snake in y-direction.
		 */
		public double getCOMy() {
				double comy = 0;
				for (Point2D.Double p : this.points)
						comy += p.y;
				return comy / this.points.size();
		}

		/**
		 * Draw snake into an image.
		 * 
		 * @param img
		 *          Image where to draw the polygon into.
		 */
		@Override
		public void drawPolygon(MTBImage img, int color) {
				if (this.scaleFactor == 1.0) {
						super.drawPolygon(img, color);
						return;
				}
				// // define the polygon color: red
				// int color = ((255 & 0xff) << 16) + ((0 & 0xff) << 8) + (0 & 0xff);
				// get number of points
				int np = this.points.size();
				// draw segments
				for (int i = 0; i < np - 1; i++) {
						int x1 = (int) Math.round(this.points.get(i).x * this.scaleFactor);
						int y1 = (int) Math.round(this.points.get(i).y * this.scaleFactor);
						int x2 = (int) Math.round(this.points.get(i + 1).x * this.scaleFactor);
						int y2 = (int) Math.round(this.points.get(i + 1).y * this.scaleFactor);
						img.drawLine2D(x1, y1, x2, y2, color);
				}
				if (this.isClosed) {
						// last segment N-1 -> 0
						int x1 = (int) Math.round(this.points.get(np - 1).x * this.scaleFactor);
						int y1 = (int) Math.round(this.points.get(np - 1).y * this.scaleFactor);
						int x2 = (int) Math.round(this.points.get(0).x * this.scaleFactor);
						int y2 = (int) Math.round(this.points.get(0).y * this.scaleFactor);
						img.drawLine2D(x1, y1, x2, y2, color);
				}
		}

		/**
		 * Draw snake points into an image (as crosses).
		 * 
		 * @param img
		 *          Image where to draw the polygon points into.
		 * @param color
		 *          Color to be used.
		 * @param mode
		 *          Shape of points to be applied.
		 */
		@Override
		public void drawPolygonPoints(MTBImage img, int color, int mode) {
				if (this.scaleFactor == 1.0) {
						super.drawPolygonPoints(img, color, mode);
						return;
				}
				int np = this.points.size();
				// draw points, but consider scaling
				int x = 0, y = 0;
				for (int i = 0; i < np; i++) {
						x = (int) Math.round(this.points.get(i).x * this.scaleFactor);
						y = (int) Math.round(this.points.get(i).y * this.scaleFactor);
						img.drawPoint2D(x, y, 0, color, mode);
				}
		}

		/**
		 * Generates binary mask for inside part of the snake.
		 * <p>
		 * The area enclosed by the snake will be painted in white. Note that the
		 * coordinates of the mask start with (0,0).
		 * 
		 * @param w
		 * @param h
		 * @return Binary mask, 0= outside / 1=inside
		 * @throws ALDOperatorException
		 */
		@Override
		public int[][] getBinaryMask(int w, int h) {
				return this.getBinaryMask(w, h, 0, 0);
		}

		/**
		 * Generates binary mask for inside part of the snake.
		 * <p>
		 * The area enclosed by the snake will be painted in white.
		 * 
		 * @param w
		 *          Width of the mask.
		 * @param h
		 *          Height of the mask.
		 * @param xoff
		 *          Offset in x-direction.
		 * @param yoff
		 *          Offset in y-direction.
		 * @return Binary mask, 1=enclosed region
		 * @throws ALDOperatorException
		 */
		private int[][] getBinaryMask(int w, int h, int xoff, int yoff) {
				try {
						SnakeHelperOperators helper = new SnakeHelperOperators();
						SnakeHelperOperators.MaskMaker mm = helper.new MaskMaker(this, xoff,
						    yoff, w, h);
						mm.runOp(true);
						return mm.getResultMask();
				} catch (ALDOperatorException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
				} catch (ALDProcessingDAGException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
				}
				return null;
		}

		@Override
		public void makeSimple() {
			// save the old points
			Vector<Point2D.Double> oldPoints = this.points;
			// make polygon simple, replaces snake point list
			super.makeSimple();
			Vector<MTBSnakePoint2D> newPoints = new Vector<MTBSnakePoint2D>();
			for(Point2D.Double p : this.points)
				newPoints.add(new MTBSnakePoint2D(p.x, p.y));
			// recover old points
			this.points = oldPoints;

			/*
			 * Update snake point IDs so that all former existing 
			 * points get the same ID as before.
			 */
			this.updateIDs(newPoints);
			// segmentation is possibly no longer up-to-date
			this.calcSnakeDomain();
			this.updateSnakeSegmentation();
		}

		/**
		 * Update method for snake point ID's after make snake simple. Method is
		 * necessary for jni_makePolySimple().
		 * 
		 * @param newPoints
		 *          points after using jni_makePolySimple but without the former ID's
		 *          for all points.
		 */
		private void updateIDs(Vector<MTBSnakePoint2D> newPoints) {
				for (int i = 0; i < newPoints.size(); i++) {
						for (int j = 0; j < this.points.size(); j++) {
								double dx = Math.abs(newPoints.elementAt(i).getX()
								    - this.points.elementAt(j).getX());
								double dy = Math.abs(newPoints.elementAt(i).getY()
								    - this.points.elementAt(j).getY());
								if (dx < MTBConstants.epsilon && dy < MTBConstants.epsilon) {
										MTBSnakePoint2D oldPoint = (MTBSnakePoint2D) this.points.elementAt(j);
										newPoints.elementAt(i).setOldId(oldPoint.getOldId());
										break;
								}
						}
				}
				this.points = new Vector<Point2D.Double>();
				for (int i = 0; i < newPoints.size(); i++) {
						this.points.addElement(newPoints.elementAt(i));
				}
		}

		/**
		 * Overwrite reversePolypoints method of Polygon2D. Changes the ordering of
		 * the snake points.
		 * <p>
		 * If the snake points are ordered clockwise afterwards they are ordered
		 * counter-clockwise and vice versa. The old id of a snake point is kept.
		 */
		@Override
		public void reversePolypoints() {
				Vector<MTBSnakePoint2D> npoints = new Vector<MTBSnakePoint2D>();
				for (int i = this.points.size() - 1; i >= 0; --i) {
						npoints.add((MTBSnakePoint2D) this.points.get(i));
				}
				this.points = new Vector<Point2D.Double>(this.points.size());
				for (int i = 0; i < npoints.size(); i++) {
						this.points.addElement(npoints.elementAt(i));
				}
				// labels of the segmentation are inverted
				this.labelInside = ((this.labelInside == 0) ? 1 : 0);
		}

		/**
		 * <pre>
		 * 
		 * Method to re-sample the line segments of the snake in a range of a given
		 * segment length. The range is calculated from the given length of a segment
		 * by calculation: 
		 * - minimum = segment length * 0.5
		 * - maximum = segment length * 1.5
		 * 
		 * If a line segment (p,q) is too small, the point q is removed from the list.
		 * Then the new line segment (p,r) is observed, where r is the successor of q.
		 * 
		 * If a line segment (p,q) is too large, new points will be added.
		 * The number of new points is calculated by the possible number of points
		 * (depending on the given segment length) that fit into the line segment
		 * between p and q.
		 * 
		 * The old index of the snake point position in the previous snake is kept for
		 * all existing snake points. For new snake points the old position index is
		 * set to -1 by default.
		 * 
		 * </pre>
		 */

		@Override
		public void resample(double segLength) {
				double segmentLength = segLength / this.scaleFactor;
				// minimum allowed distance between two points
				double min = 0.5 * segmentLength;
				// maximum allowed distance between two points
				double max = 1.5 * segmentLength;
				// observe every line segment in the polygon
				for (int i = 0; i < this.points.size() - 1; i++) {
						// x distance between the current observed two points of the line segment
						double distX = (-1)
						    * (this.points.elementAt(i).x - this.points.elementAt(i + 1).x);
						// y distance between the current observed two points of the line segment
						double distY = (-1)
						    * (this.points.elementAt(i).y - this.points.elementAt(i + 1).y);
						// total segment length between the two points
						// double dist = Math.sqrt((Math.pow(distX, 2)) + (Math.pow(distY, 2)));
						double dist = Math.sqrt(distX * distX + distY * distY);
						/*
						 * Segment length between points is under allowed minimum distance.
						 */
						if (dist <= min) {
								// remove point
								this.points.removeElementAt(i + 1);
								// decrease i to observe new line segment from first observed point to
								// the successor of the removed point
								i--;
						}
						/*
						 * Segment length between points is over allowed maximum distance.
						 */
						if (dist > max) {
								// max number of points which can be included in the current segment,
								// depending on the given segment length
								int elementCount = (int) Math.floor(dist / segmentLength);
								// insert new points
								for (int j = 0; j < elementCount - 1; j++) {
										Point2D.Double p = new Point2D.Double();
										p.x = this.points.elementAt(i).x + (distX / elementCount);
										p.y = this.points.elementAt(i).y + (distY / elementCount);
										this.points.insertElementAt(new MTBSnakePoint2D(p), i + 1);
										i++;
								}
						}
				}
				/*
				 * If polygon is closed, observe the line segment from the last point to the
				 * first point of the polygon.
				 */
				if (this.isClosed) {
						// x distance between the current observed two points of the line segment
						double distX = (-1)
						    * (this.points.elementAt(this.points.size() - 1).x - this.points
						        .elementAt(0).x);
						// y distance between the current observed two points of the line segment
						double distY = (-1)
						    * (this.points.elementAt(this.points.size() - 1).y - this.points
						        .elementAt(0).y);
						// total segment length between the two points
						// double dist = Math.sqrt((Math.pow(distX, 2)) + (Math.pow(distY, 2)));
						double dist = Math.sqrt(distX * distX + distY * distY);
						/*
						 * Segment length between points is under allowed minimum distance. Remove
						 * the last point.
						 */
						if (dist <= min) {
								this.points.removeElementAt(this.points.size() - 1);
						}
						/*
						 * Segment length between points is over allowed maximum distance. Add new
						 * points between the two points, where the number of points to add
						 * depends on the given segment length.
						 */
						if (dist > max) {
								// max number of points which can be included in the current segment,
								// depending on the given segment length
								int elementCount = (int) Math.floor(dist / segmentLength);
								for (int j = 0; j < elementCount - 1; j++) {
										Point2D.Double p = new Point2D.Double();
										p.x = this.points.elementAt(this.points.size() - 1).x
										    + (distX / elementCount);
										p.y = this.points.elementAt(this.points.size() - 1).y
										    + (distY / elementCount);
										this.points.add(new MTBSnakePoint2D(p));
								}
						}
				}
				// segmentation is possibly no longer up-to-date
				this.calcSnakeDomain();
				this.updateSnakeSegmentation();
		}

		/**
		 * Method to shift the whole snake outward (positive value) ore inward
		 * (negative value) to its normal vector from every line segment.
		 * <p>
		 * ATTENTION!!! After shifting a snake, it can happen that the snake is not
		 * simple !!!
		 * 
		 * @param shift
		 *          positive ore negative value to shift the snake
		 * @param imageSizeX
		 *          width of the image to test whether the coordinates of the shifted
		 *          snake are valid
		 * @param imageSizeY
		 *          height of the image to test whether the coordinates of the shifted
		 *          snake are valid
		 */
		@Override
		public void shift(double shift, int imageSizeX, int imageSizeY) {
				double shiftValue = (shift / this.scaleFactor);
				boolean signChanged = false;
				/*
				 * Change sign of shift value if snake is not counter clockwise, else on a
				 * positive shift value the snake is shifted inward instead of outward and
				 * on a negative value the snake is outward instead of inward. If the snake
				 * is not simple, an exception is thrown by the
				 * jni_isCounterClockwiseOriented() method. The snake must be simple for
				 * shifting!!!
				 */
//						if (!this.jni_isCounterClockwiseOriented()) {
					if (!this.isOrderedCounterClockwise()) {
								shiftValue = shiftValue * (-1.0);
								signChanged = true;
						}
				// vector of new shifted points
				Vector<Point2D.Double> newPoints = new Vector<Point2D.Double>();
				// x-coordinate of current point
				double x = 0.0;
				// y-coordinate of current point
				double y = 0.0;
				// x-coordinate of antecessor of current point
				double vor_x = 0.0;
				// y-coordinate of antecessor of current point
				double vor_y = 0.0;
				// x-coordinate of successor of current point
				double nach_x = 0.0;
				// y-coordinate of successor of current point
				double nach_y = 0.0;
				if (this.isClosed) { // shift on a closed snake
						for (int i = 0; i < this.points.size(); i++) {
								x = 0.0;
								y = 0.0;
								vor_x = 0.0;
								vor_y = 0.0;
								nach_x = 0.0;
								nach_y = 0.0;
								if (i == 0) {
										vor_x = this.points.elementAt(this.points.size() - 1).x;
										vor_y = this.points.elementAt(this.points.size() - 1).y;
								} else {
										vor_x = this.points.elementAt(i - 1).x;
										vor_y = this.points.elementAt(i - 1).y;
								}
								if (i == this.points.size() - 1) {
										nach_x = this.points.elementAt(0).x;
										nach_y = this.points.elementAt(0).y;
								} else {
										nach_x = this.points.elementAt(i + 1).x;
										nach_y = this.points.elementAt(i + 1).y;
								}
								x = this.points.elementAt(i).x;
								y = this.points.elementAt(i).y;
								// assign normal vector to the left side
								double dx1 = 0.0;
								double dy1 = 0.0;
								double dx2 = 0.0;
								double dy2 = 0.0;
								dx1 = (x - vor_x);
								dy1 = (y - vor_y);
								dx2 = (nach_x - x);
								dy2 = (nach_y - y);
								// assign vector to the right side
								Point2D.Double n1 = new Point2D.Double(dy1, (-1) * dx1);
								Point2D.Double n2 = new Point2D.Double(dy2, (-1) * dx2);
								n1 = standardization(n1);
								n2 = standardization(n2);
								double Nv_x = (n1.x + n2.x) / 2;
								double Nv_y = (n1.y + n2.y) / 2;
								Point2D.Double NV = new Point2D.Double(Nv_x, Nv_y);
								// normalize vector
								NV = standardization(NV);
								// shift the point on the given distance
								NV.x = shiftValue * NV.x;
								NV.y = shiftValue * NV.y;
								// set new point
								double newX = (x + NV.x);
								double newY = (y + NV.y);
								// test if new x and y coordinate are valid coordinate in the image
								if (newX < MTBConstants.epsilon)
										newX = 0.0;
								if (newY < MTBConstants.epsilon)
										newY = 0.0;
								if (newX >= imageSizeX)
										newX = imageSizeX - 1;
								if (newY >= imageSizeY)
										newY = imageSizeY - 1;
								// assign new snake point
								MTBSnakePoint2D oldSnakePoint = (MTBSnakePoint2D) this.points
								    .elementAt(i);
								MTBSnakePoint2D newSnakePoint = new MTBSnakePoint2D(newX, newY);
								// set old snake point index to previous index of the point within the
								// snake
								newSnakePoint.setOldId(oldSnakePoint.getOldId());
								// add new point to the new list of snake points
								newPoints.add(i, newSnakePoint);
						}
				} else { // shift on a not closed snake
						for (int i = 0; i < this.points.size(); i++) {
								x = 0.0;
								y = 0.0;
								vor_x = 0.0;
								vor_y = 0.0;
								nach_x = 0.0;
								nach_y = 0.0;
								if (i == 0) {
										vor_x = this.points.elementAt(i + 1).x;
										vor_y = this.points.elementAt(i + 1).y;
								} else {
										vor_x = this.points.elementAt(i - 1).x;
										vor_y = this.points.elementAt(i - 1).y;
								}
								if (i == this.points.size() - 1) {
										nach_x = vor_x;
										nach_y = vor_y;
								} else {
										nach_x = this.points.elementAt(i + 1).x;
										nach_y = this.points.elementAt(i + 1).y;
								}
								x = this.points.elementAt(i).x;
								y = this.points.elementAt(i).y;
								// assign normal vector to the left side
								double dx1 = 0.0;
								double dy1 = 0.0;
								double dx2 = 0.0;
								double dy2 = 0.0;
								// assign vector to the right side
								dx1 = (x - vor_x);
								dy1 = (y - vor_y);
								dx2 = (nach_x - x);
								dy2 = (nach_y - y);
								Point2D.Double n1 = new Point2D.Double(dy1, (-1) * dx1);
								Point2D.Double n2 = new Point2D.Double(dy2, (-1) * dx2);
								n1 = standardization(n1);
								n2 = standardization(n2);
								double Nv_x = (n1.x + n2.x) / 2;
								double Nv_y = (n1.y + n2.y) / 2;
								Point2D.Double NV = new Point2D.Double(Nv_x, Nv_y);
								if (i == 0)
										NV.setLocation(n2.x, n2.y);
								if (i == this.points.size() - 1)
										NV.setLocation(n1.x, n1.y);
								// normalize vector
								NV = standardization(NV);
								// shift the point on the given distance
								NV.x = shiftValue * NV.x;
								NV.y = shiftValue * NV.y;
								// set new point
								double newX = (x + NV.x);
								double newY = (y + NV.y);
								// test if new x and y coordinate are valid coordinate in the image
								if (newX < MTBConstants.epsilon)
										newX = 0.0;
								if (newY < MTBConstants.epsilon)
										newY = 0.0;
								if (newX >= imageSizeX)
										newX = imageSizeX - 1;
								if (newY >= imageSizeY)
										newY = imageSizeY - 1;
								// assign new snake point
								MTBSnakePoint2D oldSnakePoint = (MTBSnakePoint2D) this.points
								    .elementAt(i);
								MTBSnakePoint2D newSnakePoint = new MTBSnakePoint2D(newX, newY);
								// set old snake point index to previous index of the point within the
								// snake
								newSnakePoint.setOldId(oldSnakePoint.getOldId());
								// add new point to the new list of snake points
								newPoints.add(i, newSnakePoint);
						}
				}

				this.points = new Vector<Point2D.Double>(newPoints.size());
				for (int i = 0; i < newPoints.size(); i++) {
						this.points.addElement(newPoints.elementAt(i));
				}
				// segmentation is possibly no longer up-to-date
				this.calcSnakeDomain();
				this.updateSnakeSegmentation();
				if (shiftValue >= 0) {
						if (signChanged == false) {
								System.out.println("   --> do shift outward: " + Math.abs(shift)
								    + " pixels");
						} else {
								System.out.println("   --> do shift inward: " + Math.abs(shift)
								    + " pixels");
						}
				}
				if (shiftValue < MTBConstants.epsilon) {
						if (signChanged == false) {
								System.out.println("   --> do shift inward: " + Math.abs(shift)
								    + " pixels");
						} else {
								System.out.println("   --> do shift outward: " + Math.abs(shift)
								    + " pixels");
						}
				}
		}

		/**
		 * Method to draw and save the snake in a binary image.
		 * 
		 * @param file
		 *          file to save the image with the snake on the disk. File can be
		 *          null, then no file is stored on disk
		 * @param w
		 *          image width
		 * @param h
		 *          image height
		 * @return MTBImageByte including the snake as white contour line on a black
		 *         background.
		 * @throws ALDOperatorException
		 * @throws ALDProcessingDAGException
		 */
		public MTBImageByte toMTBImageByte(String file, int w, int h)
		    throws ALDOperatorException, ALDProcessingDAGException {
				SnakeHelperOperators helper = new SnakeHelperOperators();
				SnakeHelperOperators.ImageMaker im = helper.new ImageMaker(this, 1, w, h,
				    file, null);
				im.runOp(true);
				return (MTBImageByte) im.getResultImage();
		}

		/**
		 * Method to draw and save the snake in a given MTBImage.
		 * 
		 * @param file
		 *          file to save the image with the snake on the disk. File can be
		 *          null, then no file is stored on disk
		 * @param img
		 *          image where the snake should be drawn in
		 * @return MTBImage from type of the given image, including the snake as white
		 *         contour line.
		 * @throws ALDOperatorException
		 * @throws ALDProcessingDAGException
		 */
		public MTBImage toMTBImage(String file, MTBImage img)
		    throws ALDOperatorException, ALDProcessingDAGException {
				SnakeHelperOperators helper = new SnakeHelperOperators();
				SnakeHelperOperators.ImageMaker im = helper.new ImageMaker(this, 2, -1, -1,
				    file, img);
				im.runOp(true);
				return im.getResultImage();
		}

		/**
		 * Prints points of snake to standard output stream.
		 */
		public void printPoints() {
				System.out.println("Snake points: ");
				int counter = 0;
				for (Point2D.Double p : this.points) {
						MTBSnakePoint2D pp = (MTBSnakePoint2D) p;
						System.out.println("ID= " + counter + ": x= " + pp.x + " , y= " + pp.y
						    + ", oldId= " + pp.getOldId());
						counter++;
				}
				if (this.isClosed)
						System.out.println("(Snake is closed.)");
		}

		/**
		 * Operator class to construct new snake object from a given one.
		 * 
		 * @author moeller
		 */
		private class SnakeCloner extends MTBOperator {

			@Parameter(label = "inputSnake", required = true, 
				direction = Direction.IN, description = "Input snake to clone.")
			private MTBSnake inputSnake = null;

			@Parameter(label = "outputSnake", required = true, 
				direction = Direction.OUT, description = "Cloned snake object.")
			private MTBSnake outputSnake = null;

			/**
			 * Default constructor.
			 * 
			 * @param input	Input snake.
			 * @throws ALDOperatorException
			 */
			SnakeCloner(MTBSnake input) throws ALDOperatorException {
				this.inputSnake = input;
			}

			/**
			 * Get cloned snake object.
			 */
			public MTBSnake getOutputSnake() {
				return this.outputSnake;
			}

			@Override
			@SuppressWarnings("synthetic-access")
			protected void operate() {
				Vector<MTBSnakePoint2D> pointsClone = new Vector<MTBSnakePoint2D>();
				for (int i = 0; i < this.inputSnake.points.size(); i++) {
					MTBSnakePoint2D oldPoint = ((MTBSnakePoint2D) this.inputSnake.points
							.elementAt(i)).clone();
					pointsClone.addElement(oldPoint);
				}
				MTBSnake snakeClone = new MTBSnake(pointsClone, this.inputSnake.isClosed,
						this.inputSnake.scaleFactor, true);
				if (this.inputSnake.classmap != null)
					snakeClone.classmap = this.inputSnake.classmap.clone();
				if (this.inputSnake.visiblemap != null)
					snakeClone.visiblemap = this.inputSnake.visiblemap.clone();
				snakeClone.mapHeight = this.inputSnake.mapHeight;
				snakeClone.mapWidth = this.inputSnake.mapWidth;
				snakeClone.mapMaxX = this.inputSnake.mapMaxX;
				snakeClone.mapMinX = this.inputSnake.mapMinX;
				snakeClone.mapMaxY = this.inputSnake.mapMaxY;
				snakeClone.mapMinY = this.inputSnake.mapMinY;
				snakeClone.visibilityMapHeight = this.inputSnake.visibilityMapHeight;
				snakeClone.visibilityMapWidth = this.inputSnake.visibilityMapWidth;
				snakeClone.visibleMapMaxX = this.inputSnake.visibleMapMaxX;
				snakeClone.visibleMapMinX = this.inputSnake.visibleMapMinX;
				snakeClone.visibleMapMaxY = this.inputSnake.visibleMapMaxY;
				snakeClone.visibleMapMinY = this.inputSnake.visibleMapMinY;
				this.outputSnake = snakeClone;
			}
		}

		/**
		 * Converts a contour into a snake.
		 * 
		 * @param contour
		 *          Incoming contour.
		 * @return Snake object.
		 * @throws ALDOperatorException
		 * @throws ALDProcessingDAGException
		 */
		public static MTBSnake convertContourToSnake(MTBContour2D contour)
				throws ALDOperatorException, ALDProcessingDAGException {

			SnakeHelperOperators helper = new SnakeHelperOperators();
			SnakeHelperOperators.ContourConverter cconv = 
					helper.new ContourConverter(contour);
			cconv.runOp(true);
			return cconv.getResultSnake();
		}

		/**
		 * Converts a region into a snake.
		 * 
		 * @param region
		 *          Incoming region.
		 * @return Resulting snake object.
		 * @throws ALDOperatorException
		 * @throws ALDProcessingDAGException
		 */
		public static MTBSnake convertRegionToSnake(MTBRegion2D region)
		    throws ALDOperatorException, ALDProcessingDAGException {

			SnakeHelperOperators helper = new SnakeHelperOperators();
			SnakeHelperOperators.RegionConverter rconv = 
					helper.new RegionConverter(
							region);
			rconv.runOp(true);
			return rconv.getResultSnake();
		}

		/**
		 * Converts the regions of a label/binary image into a set of snakes.
		 * 
		 * @param image
		 *          Incoming image.
		 * @return Resulting snake objects.
		 * @throws ALDOperatorException
		 * @throws ALDProcessingDAGException
		 */
		public static MTBPolygon2DSet convertRegionsToSnakes(MTBImage image)
		    throws ALDOperatorException, ALDProcessingDAGException {

			SnakeHelperOperators helper = new SnakeHelperOperators();
			SnakeHelperOperators.RegionImageConverter riconv = 
				helper.new RegionImageConverter(image);
			riconv.runOp(true);
			return riconv.getResultSnakes();
		}

		/**
		 * Specify the domain of the underlying segmentation.
		 * <p>
		 * Class information is only available for pixels lying in the domain.
		 * 
		 * @param xmin	Minimal coordinate in x-direction.
		 * @param xmax	Maximal coordinate in x-direction.
		 * @param ymin	Minimal coordinate in y-direction.
		 * @param ymax	Maximal coordinate in y-direction.
		 */
		public void setSegmentationDomain(int xmin, int xmax, int ymin, int ymax) {
				this.mapMinY = ymin;
				this.mapMaxY = ymax;
				this.mapMinX = xmin;
				this.mapMaxX = xmax;
				this.mapHeight = (this.mapMaxY - this.mapMinY) + 1;
				this.mapWidth = (this.mapMaxX - this.mapMinX) + 1;
		}

		/**
		 * Calculates snake bounding box (in non-normalized coordinates!).
		 */
		private void calcSnakeDomain() {
				this.mapMinX = Integer.MAX_VALUE;
				this.mapMaxX = 0;
				this.mapMinY = Integer.MAX_VALUE;
				this.mapMaxY = 0;
				// get bounding box of the snake
				for (Point2D.Double p : this.points) {
						if (p.x * this.scaleFactor > this.mapMaxX)
								this.mapMaxX = (int) (p.x * this.scaleFactor + 0.5);
						if (p.x * this.scaleFactor < this.mapMinX)
								this.mapMinX = (int) (p.x * this.scaleFactor);
						if (p.y * this.scaleFactor > this.mapMaxY)
								this.mapMaxY = (int) (p.y * this.scaleFactor + 0.5);
						if (p.y * this.scaleFactor < this.mapMinY)
								this.mapMinY = (int) (p.y * this.scaleFactor);
				}
				// add 5 pixel border
				this.mapMinX -= 5;
				this.mapMaxX += 5;
				this.mapMinY -= 5;
				this.mapMaxY += 5;
				// calculate width and height
				this.mapWidth = (this.mapMaxX - this.mapMinX) + 1;
				this.mapHeight = (this.mapMaxY - this.mapMinY) + 1;
		}

		/**
		 * Convenience function for updating the segmentation.
		 */
		private void updateSnakeSegmentation() {
//				try {
//						this.labelInside = (this.jni_isCounterClockwiseOriented() ? 1 : 0);
//				} catch (MTBPolygon2DException e) {
//						System.err.println("MTBSnake: cannot determine snake orientation! "
//						    + " => Assuming counter-clockwise sorting!");
//				}
			this.labelInside = (this.isOrderedCounterClockwise() ? 1 : 0);
				// update the masks, set all pixels visible per default
				int[][] binMap = this.getBinaryMask(this.mapWidth, this.mapHeight,
				    this.mapMinX, this.mapMinY);
				this.classmap = new int[this.mapHeight][this.mapWidth];
				this.visiblemap = new boolean[this.mapHeight][this.mapWidth];
				this.visibilityMapHeight = this.mapHeight;
				this.visibilityMapWidth = this.mapWidth;
				this.visibleMapMinX = this.mapMinX;
				this.visibleMapMinY = this.mapMinY;
				this.visibleMapMaxX = this.mapMaxX;
				this.visibleMapMaxY = this.mapMaxY;
				for (int y = 0; y < this.mapHeight; ++y) {
						for (int x = 0; x < this.mapWidth; ++x) {
								this.visiblemap[y][x] = true;
								this.classmap[y][x] = binMap[y][x];
						}
				}
		}

		// /**
		// * Convert the current segmentation given by the snake to an image.
		// */
		// public MTBImageByte segmentationToImage() {
		// MTBImageByte im =
		// (MTBImageByte)MTBImage.createMTBImage(this.mapWidth, this.mapHeight,
		// 1, 1, 1, MTBImage.MTBImageType.MTB_BYTE);
		// for (int y=0;y<this.mapHeight;++y) {
		// for (int x=0;x<this.mapWidth;++x) {
		// im.putValueInt(x, y, this.getClass(x, y));
		// }
		// }
		// return im;
		// }

		/*
		 * Functions for implementing interface 'MTBSegmentationInterface'.
		 */

		/**
		 * Returns the dimension of the given membership.
		 */
		@Override
		public SegmentationDimension getDimension() {
				return SegmentationDimension.dim_2;
		}

		/**
		 * Returns the number of classes represented in the membership.
		 */
		@Override
		public int getNumberOfClasses() {
				return 2;
		}

		/**
		 * Returns the maximal label used in the membership representation.
		 */
		@Override
		public int getMaxLabel() {
				return 1;
		}

		@Override
		public boolean isVisible(int x, int y, int z) {
				return this.isVisible(x, y);
		}

		/** 
		 * Get visibility of a certain pixel position.
		 * <p>
		 * Note that pixels outside the domain of the visibility map are 
		 * always assumed to be visible.
		 * 
		 * @param x		x-coordinate of position under consideration.
		 * @param y		y-coordinate of position under consideration.
		 * @return True, if pixel is visible.
		 */
		@Override
		public boolean isVisible(int x, int y) {
			if (   x < this.visibleMapMinX || x > this.visibleMapMaxX 
					|| y < this.visibleMapMinY || y > this.visibleMapMaxY)
				return true;
			return this.visiblemap[y-this.visibleMapMinY][x-this.visibleMapMinX];
		}

		@Override
		public int getClass(int x, int y, int z) {
				return this.getClass(x, y);
		}

		@Override
		public int getClass(int x, int y) {
			// pixel out of map area, must be outside
			if (   x < this.mapMinX || x > this.mapMaxX 
					|| y < this.mapMinY || y > this.mapMaxY)
				return (this.labelInside == 0 ? 1 : 0);
			return (this.classmap[y - this.mapMinY][x - this.mapMinX] == 1 ? 
				this.labelInside : (1 - this.labelInside));
		}

		@Override
		@Deprecated
		public void setClass(int x, int y, int z, int c) {
				System.err.println("MTBSnake::setClass() - attention, does nothing...!");
				return;
		}

		@Override
		@Deprecated
		public void setClass(int x, int y, int c) {
				System.err.println("MTBSnake::setClass() - attention, does nothing...!");
				return;
		}

		@Override
		public void setVisible(int x, int y) {
			if (   x < this.visibleMapMinX || x > this.visibleMapMaxX 
					|| y < this.visibleMapMinY || y > this.visibleMapMaxY)
				return;
			this.visiblemap[y-this.visibleMapMinY][x-this.visibleMapMinX] = true;
		}

		@Override
		public void setVisible(int x, int y, int z) {
				this.setVisible(x, y);
		}

		@Override
		public void setInvisible(int x, int y) {
			if (   x < this.visibleMapMinX || x > this.visibleMapMaxX 
					|| y < this.visibleMapMinY || y > this.visibleMapMaxY)
				return;
			this.visiblemap[y-this.visibleMapMinY][x-this.visibleMapMinX] = false;
		}

		@Override
		public void setInvisible(int x, int y, int z) {
				this.setInvisible(x, y);
		}

		/**
		 * Set the valid mask from external mask data.
		 * <p>
		 * Note that the parameter object is cloned for internal representation!
		 */
		public void setVisibilityMask(boolean[][] mask, int mMinX, int mMinY,
				int mMaxX, int mMaxY) {
			this.visiblemap = mask.clone();
			this.visibleMapMinX = mMinX;
			this.visibleMapMinY = mMinY;
			this.visibleMapMaxX = mMaxX;
			this.visibleMapMaxY = mMaxY;
			this.visibilityMapHeight = 
				(this.visibleMapMaxY - this.visibleMapMinY) + 1;
			this.visibilityMapWidth = 
				(this.visibleMapMaxX - this.visibleMapMinX) + 1;
		}

		@Override
		public int getSizeX() {
				return this.mapWidth;
		}

		@Override
		public int getSizeY() {
				return this.mapHeight;
		}

		@Override
		public int getSizeZ() {
				return 1;
		}

		/**
		 * Returns minimal x coordinate represented in class map.
		 */
		public int getDomainXMinCoordinate() {
				return this.mapMinX;
		}

		/**
		 * Returns maximal x coordinate represented in class map.
		 */
		public int getDomainXMaxCoordinate() {
				return this.mapMaxX;
		}

		/**
		 * Returns minimal y coordinate represented in class map.
		 */
		public int getDomainYMinCoordinate() {
				return this.mapMinY;
		}

		/**
		 * Returns maximal y coordinate represented in visibility map.
		 */
		public int getDomainYMaxCoordinate() {
				return this.mapMaxY;
		}

		/**
		 * Returns minimal x coordinate represented in visibility map.
		 */
		public int getVisibilityMapXMinCoordinate() {
				return this.visibleMapMinX;
		}

		/**
		 * Returns maximal x coordinate represented in visibility map.
		 */
		public int getVisibilityMapXMaxCoordinate() {
				return this.visibleMapMaxX;
		}

		/**
		 * Returns minimal y coordinate represented in visibility map.
		 */
		public int getVisibilityMapYMinCoordinate() {
				return this.visibleMapMinY;
		}

		/**
		 * Returns maximal y coordinate represented in visibility map.
		 */
		public int getVisibilityMapYMaxCoordinate() {
				return this.visibleMapMaxX;
		}
		
		/**
		 * Returns width of the visibility map.
		 * @return	Width of map.
		 */
		public int getVisibilityMapWidth() {
			return this.visibilityMapWidth;
		}

		/**
		 * Returns height of the visibility map.
		 * @return	Height of map.
		 */
		public int getVisibilityMapHeight() {
			return this.visibilityMapHeight;
		}

		@Override
		public double getWeight(int x, int y) {
				return 1.0;
		}

		@Override
		public double getWeight(int x, int y, int z) {
				return 1.0;
		}

		@Override
		public void setWeight(int x, int y, double c) {
				// nothing to do here
		}

		@Override
		public void setWeight(int x, int y, int z, double c) {
				// nothing to do here
		}

    @Override
    public MTBImage getMask(int class_) {
    	MTBImage mask = MTBImage.createMTBImage(getSizeX(), getSizeY(), 
    		getSizeZ(),1,1, MTBImage.MTBImageType.MTB_BYTE);

    	for (int z = 0; z < getSizeZ(); z++) {
    		for (int y = 0; y < getSizeY(); y++) {
    			for (int x = 0; x < getSizeX(); x++) {
    				if(getClass(x + this.mapMinX, y + this.mapMinY, z) == class_)
    					mask.putValueInt(x,y,z,255);
    				else
    					mask.putValueInt(x,y,z,0);
    			}
    		}
    	}
    	return mask;
    }
}
