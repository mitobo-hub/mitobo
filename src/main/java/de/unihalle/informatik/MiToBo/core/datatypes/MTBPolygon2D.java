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

import java.awt.Polygon;
import java.awt.geom.*;
import java.util.*;

import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.Alida.operator.ALDData;
import de.unihalle.informatik.MiToBo.core.datatypes.defines.MTBConstants;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.datatypes.interfaces.MTBDataExportableToImageJROI;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSnake;

/**
 * Polygon datatype with double precision.
 * <p>
 * This class re-implements the class {@link java.awt.Polygon}.
 * The main reason for this is the awt-polygons are based on integer 
 * points. However, in MiToBo we require double precision in snake 
 * calculations.
 * <p>
 * Each polygon can be of complex or simple nature (i.e. does or does 
 * not intersect with itself), can be open or closed and is sorted 
 * either counter-clockwise or clockwise. Note that boundary points 
 * belongs to the polygon per definition.
 * <p>
 * The point of the polygon must have positive values for x- and 
 * y-coordinates. Negative coordinates can afford an undesirable 
 * behavior of some methods.
 * <p>
 * Note that some of the methods to be found here rely on the 
 * GeoLib library. The library is accessed via the wrapper class
 * {@link MTBPolygon2D_GeoPolygonsWrapper}.
 * 
 * @see java.awt.Polygon
 * @see MTBPolygon2D_GeoPolygonsWrapper
 * 
 * @author moeller, misiak
 */
@ALDParametrizedClass
public class MTBPolygon2D extends ALDData 
	implements Cloneable, MTBDataExportableToImageJROI {

  /**
   * List of polygon points.
   */
	@ALDClassParameter(label="List of points")
  protected Vector<Point2D.Double> points;

  /**
   * Indicates if the polygon is closed or not.
   */
	@ALDClassParameter(label="Polygon is closed")
  protected boolean isClosed;

  /**
   * Default constructor.
   */
  public MTBPolygon2D() {
    this.points = new Vector<Point2D.Double>();
  }

  /**
   * Construct polygon from coordinate arrays.
   * 
   * @param xp
   *          x coordinates of points.
   * @param yp
   *          y coordinates of points.
   * @param closed
   *          Should be true if polygon is closed.
   */
  public MTBPolygon2D(double[] xp, double[] yp, boolean closed) {
    for (int i = 0; i < xp.length; ++i) {
      this.points.add(new Point2D.Double(xp[i], yp[i]));
    }
    this.isClosed = closed;
  }

  /**
   * Construct from point list.
   * 
   * @param ps
   *          List with points.
   * @param closed
   *          Should be true if polygon is closed.
   */
  public MTBPolygon2D(Vector<Point2D.Double> ps, boolean closed) {
    this.points = ps;
    this.isClosed = closed;
  }

  @Override
  public boolean equals( Object obj) {
		if ( ! MTBPolygon2D.class.isAssignableFrom(obj.getClass())) {
			return super.equals(obj);
		}
		
		MTBPolygon2D polygon = (MTBPolygon2D) obj;
		if ( polygon.points.size() != this.points.size())
			return false;
		
		for ( int p = 0 ; p < this.points.size() ; p++) {
			Point2D.Double thisPoint = this.points.get(p);
			Point2D.Double otherPoint = polygon.points.get(p);
			
			if ( ! thisPoint.equals(otherPoint))
				return false;
		}
		return true;
  }
  /**
   * Set polygon closed.
   */
  public void setClosed() {
    this.isClosed = true;
  }

  /**
   * Set polygon opened (not closed).
   */
  public void setOpen() {
    this.isClosed = false;
  }

  /**
   * Returns true if the polygon forms a closed polygon.
   * @return True if polygon is closed.
   */
  public boolean isClosed() {
    return this.isClosed;
  }

  /**
   * Set all points of the polygon from the specified point vector object.
   * 
   * @param ps
   *          vector with new points of the polygon
   */
  public void setPoints(Vector<Point2D.Double> ps) {
    this.points = ps;
  }

  /**
   * Get polygon points.
   * 
   * @return Vector with points.
   */
  public Vector<Point2D.Double> getPoints() {
    return this.points;
  }

  /**
   * Get the number of points from the polygon.
   * 
   * @return Number of included points.
   */
  public int getPointNum() {
    if (this.points == null)
      return 0;
    return this.points.size();
  }

  @Override
  public MTBPolygon2D clone() {
  	Vector<Point2D.Double> newPoints = new Vector<Point2D.Double>();
  	for (Point2D.Double p : this.points) {
  		newPoints.add(new Point2D.Double(p.x,p.y));
  	}
    MTBPolygon2D tmpPoly = new MTBPolygon2D(newPoints, this.isClosed);
    return tmpPoly;
  }

  /**
   * Get a Polygon2D copy of this object.
   * 
   * @return Copy of this Polygon2D object.
   */
  public MTBPolygon2D getPolygon() {
    return this.clone();
  }

  /**
   * Appends a new point to the end of the polygon.
   * 
   * @param x
   *          Point x coordinate.
   * @param y
   *          Point y coordinate.
   */
  public void addPoint(double x, double y) {
    this.points.add(new Point2D.Double(x, y));
  }

		/**
		 * Calculates the signed area of simple (!) polygons.
		 * <p>
		 * The area is calculated according to the formulas found at
		 * <p>
		 * http://www.faqs.org/faqs/graphics/algorithms-faq/,
		 * <p>
		 * item 2.01. If the polygon points are ordered counter-clockwise the area
		 * will be larger than zero, otherwise smaller.
		 * <p>
		 * Note: if the polygon is complex, the result is undetermined!
		 * 
		 * TODO Replace with JNI CGAL function.
		 * 
		 * @return Signed area of the polygon.
		 */
  public double getSignedArea() {
    double x_i, y_i, x_n, y_n, area = 0;
    for (int i = 0; i <= this.points.size() - 2; ++i) {
      x_i = this.points.get(i).x;
      y_i = this.points.get(i).y;
      x_n = this.points.get(i + 1).x;
      y_n = this.points.get(i + 1).y;
      area += x_i * y_n - y_i * x_n;
    }
    x_i = this.points.get(this.points.size() - 1).x;
    y_i = this.points.get(this.points.size() - 1).y;
    x_n = this.points.get(0).x;
    y_n = this.points.get(0).y;
    area += x_i * y_n - y_i * x_n;
    return area / 2.0;
  }

  /**
   * Get the length of the polygon.
   * @return Length of polygon, i.e. sum of lengths of all segments.
   */
  public double getLength() {
  	double length = 0;
    for (int i = 0; i <= this.points.size() - 2; ++i) {
    	length += Math.sqrt((this.points.get(i+1).x - this.points.get(i).x)*
    	(this.points.get(i+1).x - this.points.get(i).x) +
    	(this.points.get(i+1).y - this.points.get(i).y)*
    	(this.points.get(i+1).y - this.points.get(i).y));
    }
  	length += Math.sqrt((this.points.get(0).x - this.points.get(this.points.size() - 1).x)*
      	(this.points.get(0).x - this.points.get(this.points.size() - 1).x) +
      	(this.points.get(0).y - this.points.get(this.points.size() - 1).y)*
      	(this.points.get(0).y - this.points.get(this.points.size() - 1).y));
  	return length;
  }
  
  /**
   * Calculates the axes-parallel bounding box of the snake.
   * <p>
   * The function extracts the coordinates of the upper left and lower right
   * corner of the bounding box of the snake. Note that the there is at least
   * one point of the snake lying on each side of the bounding box, i.e. the
   * snake not just touches the box, but lies on it.
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
   * Generates binary mask for inside part of the polygon. Negative coordinates
   * of the polygon should be avoid.
   * 
   * @param w Image width.
   * @param h Image height.
   * @return Binary mask of size height times width, 0= outside / 1=inside.
   */
  public int[][] getBinaryMask(int w, int h) {
    ImagePlus img = NewImage.createByteImage("", w, h, 1, NewImage.FILL_WHITE);
    ImageProcessor ip = img.getProcessor();
    int[] xps = new int[this.getPointNum()];
    int[] yps = new int[this.getPointNum()];
    int n = 0;
    for (Point2D.Double p : this.points) {
      xps[n] = (int) (p.x + 0.5);
      yps[n] = (int) (p.y + 0.5);
      n++;
    }    
    Polygon awtPoly = new Polygon(xps, yps, this.getPointNum());
    // background white, polygon black
    ip.fillPolygon(awtPoly);
    // transfer region information to result mask
    int[][] mask = new int[h][w];
    for (int y = 0; y < h; ++y)
      for (int x = 0; x < w; ++x)
        if (ip.getPixel(x, y) == 0)
          mask[y][x] = 1;
    // safety check: add potentially missing contour pixels
    // (ImageJ uses scan-line polygon filling, but sometimes apparently
    //  misses some of the contour pixels themselves which we want to be
    //  part of the polygon region...)
    for (int i=0;i<n; ++i) {
    	// safety check if pixel is inside mask
    	if (yps[i] >= 0 && yps[i] < h && xps[i] >= 0 && xps[i] < w)
    		mask[yps[i]][xps[i]] = 1;
    }
    return mask;
  }

		/**
		 * Checks if (simple!) polygon points are sorted counter-clockwise.
		 * <p>
		 * Implemented according to item 2.07 at
		 * <p>
		 * http://www.faqs.org/faqs/graphics/algorithms-faq/.
		 * <p>
		 * Note that if the polygon is complex the result maybe wrong.
		 * 
		 * @return True, if points are ordered counter-clockwise.
		 */
//  @Deprecated
//  public boolean isSortedCounterClockwise() {
//    return (this.getSignedArea() > 0);
//  }

  /**
   * Check if points are ordered clockwise.
   * @return	True, if points are ordered clockwise.
   */
  public boolean isOrderedClockwise() {
  	return MTBPolygon2D_GeoPolygonsWrapper.isClockwiseOriented(this);
  }

  /**
   * Check if points are ordered counter-clockwise.
   * @return	True, if points are ordered counter-clockwise.
   */
  public boolean isOrderedCounterClockwise() {
  	return MTBPolygon2D_GeoPolygonsWrapper.isCounterClockwiseOriented(this);
  }
  
  /**
   * Check if polygon is simple, i.e. has no self-overlaps.
   * @return	True, if polygon is simple.
   */
  public boolean isSimple() {
  	return MTBPolygon2D_GeoPolygonsWrapper.isSimple(this);
  }
  
  /**
   * Check if polygon is convex.
   * @return	True, if polygon is convex.
   */
  public boolean isConvex() {
  	return MTBPolygon2D_GeoPolygonsWrapper.isConvex(this);
  }
  
  /**
   * Checks if given point lies inside of the polygon.
   * <p>
   * Note that the polygons boundary does not belong to the interior.
   * 
   * @param x		x-coordinate of the point to test.
   * @param y		y-coordinate of the point to test.
   * @return	True, if the point lies in the polygon's interior.
   */
  public boolean containsPoint(double x, double y) {
  	return MTBPolygon2D_GeoPolygonsWrapper.containsPoint(this, x, y);
  }
  
  /**
   * Determines if a point lies inside a polygon or on its boundary.
   * <p>
   * The method indirectly relies on CGAL functions.
   * <p>
   * Method is not very fast. The Polygon must be a simple polygon.
   * <p>
   * For fast method to check whether a couple of points lies inside or outside
   * the polygon, use the binary mask (see getBinaryMask) an check pixel values
   * for 0 or 1.
   * 
   * @param x
   *          x coordinate of point to check.
   * @param y
   *          y coordinate of point to check.
   * @return True, if point lies on the boundary or inside of the polygon.
   */
//  public boolean jni_containsPoint(double x, double y)
//      throws MTBPolygon2DException {
//    if (!this.jni_isSimple()) {
//      throw new MTBPolygon2DException(
//          "> Polygon is not simple! Cannot test if point contains in polygon! <");
//    }
//    double[] xs = new double[this.points.size()];
//    double[] ys = new double[this.points.size()];
//    for (int i = 0; i < this.points.size(); i++) {
//      xs[i] = this.points.get(i).x;
//      ys[i] = this.points.get(i).y;
//    }
//    double[] pt = new double[2];
//    pt[0] = x;
//    pt[1] = y;
//
//    // get sorting of polygon: 1 = clockwise, -1 = counter-clockwise
//    int sorting = (this.polyHelper.cgal_isClockwiseOriented(xs, ys)) ? 1 : -1;
//
//    // get orientation of point w.r.t. polygon
//    int orient = this.polyHelper.cgal_orientation(xs, ys, pt);
//
//    // now check for all possible constellations
//    switch (orient) {
//      case 0:
//        return true;
//      case 1: // positive side
//        if (sorting == -1)
//          return true;
//        return false;
//      case -1: // counter-clockwise ordering
//        if (sorting == 1)
//          return true;
//        return false;
//    }
//    return false;
//  }

  /**
   * JNI method for eliminating loops in polygons.
   * <p>
   * ATTENTION!!! The returned polygon is CLOCKWISE ORIENTED!!!
   */
//  public void jni_makePolySimple() {
//    double[] xs = new double[this.points.size()];
//    double[] ys = new double[this.points.size()];
//    for (int i = 0; i < this.points.size(); i++) {
//      xs[i] = this.points.get(i).x;
//      ys[i] = this.points.get(i).y;
//    }
//    double[] simplePoly = this.polyHelper.cgal_makePolySimple(xs, ys);
//    this.points = new Vector<Point2D.Double>();
//    int size = simplePoly.length / 2;
//    // search for double occurrence of points
//    boolean find;
//    for (int i = 0; i < size - 1; i++) {
//      find = false;
//      for (int j = i + 1; j < size; j++) {
//        double dx = Math.abs(simplePoly[i] - simplePoly[j]);
//        double dy = Math.abs(simplePoly[i + size] - simplePoly[j + size]);
//        if (dx < MTBConstants.epsilon && dy < MTBConstants.epsilon) {
//          find = true;
//          break;
//        }
//      }
//      // use only points with single occurrence to make the poly simple
//      if (find == false) {
//        this.points.addElement(new Point2D.Double(simplePoly[i], simplePoly[i
//            + size]));
//      }
//    }
//  }

  /**
   * Changes the ordering of the polygon points.
   * <p>
   * If the polygon points are ordered clockwise afterwards they are ordered
   * counter-clockwise and vice versa.
   */
  public void reversePolypoints() {
    Vector<Point2D.Double> npoints = new Vector<Point2D.Double>();
    for (int i = this.points.size() - 1; i >= 0; --i) {
      npoints.add(this.points.get(i));
    }
    this.points = npoints;
  }

  /**
   * Determines if a point lies inside a polygon or on its boundary.
   * <p>
   * Use this method only for single point testing. If a couple of points should
   * be tested, use the getBinaryMask method to get the region inside the
   * polygon and test the points whether they are inside (value = 1) or outside
   * (value = 0) the polygon.
   * 
   * @param px
   *          x coordinate of point to check
   * @param py
   *          y coordinate of point to check
   * @param w
   *          width of the image where the polygon is included
   * @param h
   *          height of the image where the polygon is included
   * @return True, if point lies on the boundary or inside of the polygon.
   */
  public boolean contains(double px, double py, int w, int h) {
    int[][] mask = this.getBinaryMask(w, h);
    // test whether the point is inside the polygon region or not
    if (mask[(int) Math.round(px)][(int) Math.round(py)] == 1) {
      return true;
    }
		return false;
  }

  /**
   * Check if a given polygon is simple.
   * <p>
   * The algorithm works brute-force by simply checking each pair of polygon
   * segments for intersections.
   * 
   * @return True if no intersections are found.
   */
//  @Deprecated
//  public boolean isSimple() {
//
//    int npoints = this.points.size();
//
//    // search for the start point: the top-left point of the polygon
//    // (top-left = minimal distance to origin (0,0))
//    int startID = -1;
//    double dist, minDist = Double.MAX_VALUE;
//    for (int i = 0; i < npoints; ++i) {
//      dist = this.points.get(i).x * this.points.get(i).x + this.points.get(i).y
//          * this.points.get(i).y;
//      if (dist < minDist) {
//        minDist = dist;
//        startID = i;
//      }
//    }
//
//    int segStart = startID;
//    int segEnd = -1;
//    if (startID == npoints)
//      segEnd = 0;
//    else
//      segEnd = segStart + 1;
//
//    int firstSegment = segStart;
//
//    // traverse the polygon and search for intersections
//    int direction = 1; // +1 -> forward traversal
//
//    boolean ready = false;
//    while (!ready) {
//
//      int firstToCheck = segEnd + 1;
//      if (firstToCheck == npoints)
//        firstToCheck = 0;
//      int lastToCheck = segStart - 1;
//      if (lastToCheck == -1)
//        lastToCheck = npoints - 1;
//
//      MTBLineSegment2D s = new MTBLineSegment2D(this.points.get(segStart).x,
//          this.points.get(segStart).y, this.points.get(segEnd).x, this.points
//              .get(segEnd).y);
//
//      for (int checkStart = firstToCheck; checkStart != lastToCheck;) {
//        int checkEnd = checkStart + direction;
//        if (checkEnd == npoints)
//          checkEnd = 0;
//        MTBLineSegment2D t = new MTBLineSegment2D(this.points.get(checkStart).x,
//            this.points.get(checkStart).y, this.points.get(checkEnd).x,
//            this.points.get(checkEnd).y);
//        if (s.intersectsLine(t)) {
//          return false;
//        }
//        checkStart = checkStart + direction;
//        if (checkStart == npoints)
//          checkStart = 0;
//      }
//      segStart += direction;
//      segEnd = segStart + direction;
//
//      // next segment, or all done?
//      if (segStart == firstSegment) {
//        ready = true;
//        break;
//      }
//      if (segStart == npoints) {
//        segStart = 0;
//        segEnd = 1;
//      }
//      if (segEnd == npoints) {
//        segEnd = 0;
//      }
//    }
//    return true;
//  }

	/**
	 * Makes the polygon simple, i.e. removes self-overlaps.
	 */
	public void makeSimple() {
		this.points = 
			MTBPolygon2D_GeoPolygonsWrapper.makePolySimple(this).getPoints();
		this.isClosed = true;
	}

  /**
   * Simplifies the polygon.
   * <p>
   * Given a polygon with points sorted counter-clockwise any self-intersection
   * is eliminated. Basically, at first all intersections are calculated and
   * then inserted into the point list, which results in a new pointlist
   * including all polygon points and the intersections in between.<br>
   * In a second step the complete pointlist is traversed counter-clockwise.
   * Non-intersection points are directly added to the simplified polygon. If an
   * intersection point is found, its second counter-part in the list is
   * searched. Then the neighbor of that point with lies left of the current
   * segment is added to the simple polygon and traversal continues in the
   * direction of the recently added point.<br>
   * The whole procedure stops when the starting point is reached again.
   * <p>
   * Complexity of the algorithm:<br>
   * Searching for all intersection points requires to check each segment
   * against each other, yielding a complexity of O(N*N). During pointlist
   * traversal each point of the list is considered maximal once, however, the
   * list is traversed two times in two different directions to make sure that
   * we run in the correct direction on the polygon. Thus, traversal needs
   * O(2*|pointlist|).<br>
   * Altogether this results in O(N*N + N), i.e. quadratic complexity.
   * <p>
   * IMPORTANT! It is strongly required that the polygon is sorted
   * counter-clockwise!
   * 
   * @return True, if the simplification procedure was successful.
   */
  @Deprecated
  public boolean simplify() {

    // number of polygon points
    int npoints = this.points.size();

    // three segments never intersect!
    if (npoints < 4)
      return true;

    // TODO segment suchen, dass dem Ursprung am nächsten ist...

    // search for the start point: the top-left point of the polygon
    // (top-left = minimal distance to origin (0,0)
    int startID = -1;
    double dist, minDist = Double.MAX_VALUE;
    for (int i = 0; i < npoints; ++i) {
      dist = this.points.get(i).x * this.points.get(i).x + this.points.get(i).y
          * this.points.get(i).y;
      if (dist < minDist) {
        minDist = dist;
        startID = i;
      }
    }
    if (startID == -1)
      return true;
    double start_x = this.points.get(startID).x;
    double start_y = this.points.get(startID).y;

    // init intersection polygon
    Vector<Point2D.Double> polyWithIntersections = new Vector<Point2D.Double>();

    // for each segment, calculate all intersections and generate a new
    // polygon that includes those intersections
    MTBLineSegment2D s, t;
    Iterator<Double> iterator;
    SortedMap<Double, Point2D.Double> intersections;

    // check all segments pairwise for intersections
    double distance = 0;
    int startPointID = -1;
    for (int i = 0; i < npoints; ++i) {
      s = new MTBLineSegment2D(this.points.get(i).x, this.points.get(i).y,
          this.points.get((i + 1) % npoints).x, this.points.get((i + 1)
              % npoints).y);

      // init data structure for intersection points;
      // points are automatically sorted by distance to segment source
      intersections = new TreeMap<Double, Point2D.Double>();
      for (int j = 0; j < npoints; ++j) {
        // if the segment t has contact with segment s, skip it
        if (j == i || (j + 1) % npoints == i || j == (i + 1) % npoints)
          continue;
        t = new MTBLineSegment2D(this.points.get(j).x, this.points.get(j).y,
            this.points.get((j + 1) % npoints).x, this.points.get((j + 1)
                % npoints).y);
        if (s.intersectsLine(t)) {
          Point2D.Double p = s.getIntersection(t);
          distance = p.distance(new Point2D.Double(s.x1, s.y1));
          intersections.put(new Double(distance), new IntersectionPoint2D(p.x,
              p.y));
        }
      }

      // add segment source to new polygon
      polyWithIntersections.add(new Point2D.Double(s.x1, s.y1));

      // check, if the point is the starting point
      if (s.x1 == start_x && s.y1 == start_y)
        startPointID = polyWithIntersections.size() - 1;

      // append all intersection points
      iterator = intersections.keySet().iterator();
      while (iterator.hasNext()) {
        Object key = iterator.next();
        polyWithIntersections.add(intersections.get(key));
      }
    }

    /*
     * Polygon traversal: Now the polygon point list is traversed and
     * self-intersections are eliminated. As we cannot be sure that we start in
     * a correctly ordered section of the polygon, the idea is to traverse the
     * polygon points twice: once given the original sorting, and another time
     * after the point list was reversed. Afterwards both resulting polygons are
     * compared with regard to their length, and the longest one is the final
     * simple result.
     */

    int pnum = polyWithIntersections.size();

    // first traversal
    Vector<Point2D.Double> simplePoly_1 = this.traversePolygonPointList(
        polyWithIntersections, startPointID);
    if (simplePoly_1 == null)
      return false;

    // revert the list
    Vector<Point2D.Double> invPolyWithIntersections = new Vector<Point2D.Double>();
    for (int i = pnum - 1; i >= 0; --i)
      invPolyWithIntersections.add(polyWithIntersections.get(i));

    // second traversal
    Vector<Point2D.Double> simplePoly_2 = this.traversePolygonPointList(
        invPolyWithIntersections, pnum - 1 - startPointID);
    if (simplePoly_2 == null)
      return false;

    // if (simplePoly_1.size() > simplePoly_2.size())
    System.out.println("Punkte vorher: " + npoints);
    System.out.println("Poly 1: " + simplePoly_1.size());
    System.out.println("Poly 2: " + simplePoly_2.size());

    if (simplePoly_1.size() < simplePoly_2.size()) {
      if (simplePoly_1.size() > npoints / 2)
        this.points = simplePoly_1;
      else if (simplePoly_2.size() > npoints / 2)
        this.points = simplePoly_2;
    } else {
      if (simplePoly_2.size() > npoints / 2)
        this.points = simplePoly_2;
      else if (simplePoly_1.size() > npoints / 2)
        this.points = simplePoly_1;
    }
    System.out.println("Punkte nachher: " + this.points.size());
    return true;
  }

  /**
   * Helper function for simplify().
   * <p>
   * This function traverses a given point list in counter-clockwise ordering
   * and eliminates all intersection points.
   * 
   * @param pointlist
   *          List of points to traverse.
   * @param startPointID
   *          ID of point where to start.
   * @return Simplified point list (in counter-clockwise order).
   */
  @SuppressWarnings("unchecked")
  private Vector<Point2D.Double> traversePolygonPointList(
      Vector<Point2D.Double> pointlist, int startPointID) {
    // init result data structure
    Vector<Point2D.Double> simplePoly = new Vector<Point2D.Double>();

    // number of points in the list
    int pnum = pointlist.size();

    // add first point to the simplified polygon
    simplePoly.add(pointlist.get(startPointID));

    // next point to examine
    int currentPointID = (startPointID + 1) % pnum;

    // current direction of traversal
    int direction = 1;

    // as long as we do not reach the starting point again, continue
    int iterations = 0;
    while (currentPointID != startPointID && iterations < pnum + 10) {

      iterations++;

      // get the current point
      Point2D.Double currentPoint = pointlist.get(currentPointID);

      // if the point is not an intersection point, simply add it to the polygon
      if (!(currentPoint instanceof IntersectionPoint2D)) {
        simplePoly.add(currentPoint);
        currentPointID += direction;
      }
      // otherwise, eliminate intersection
      else {
        // cast the intersection point
        IntersectionPoint2D intersectionPoint = (IntersectionPoint2D) currentPoint;

        // search for the second appearance of this point in the polygon
        int sndAppearID = -1;
        for (int i = 0; i < pointlist.size(); ++i) {
          if (i == currentPointID)
            continue;
          if (intersectionPoint.equals(pointlist.get(i))) {
            sndAppearID = i;
            break;
          }
        }
        // error handling, if no point was found...
        if (sndAppearID == -1) {
          System.err.println("simplify() - Error!"
              + "Intersection only one time present!?");
          return null;
        }

        // get the preceder of the second intersection point...
        if (sndAppearID == 0) {
          System.err.println("simplify() - Error!"
              + "Intersection only one time present!?");
          return null;
        }
        Point2D.Double neighbor = pointlist.get(sndAppearID - 1);

        // ... and check if it is lying left of our current point
        int currentPointNum = simplePoly.size();
        MTBLineSegment2D currentSegment = new MTBLineSegment2D(simplePoly
            .get(currentPointNum - 1).x, simplePoly.get(currentPointNum - 1).y,
            currentPoint.x, currentPoint.y);
        double orientation = currentSegment.getOrientation(neighbor.x,
            neighbor.y);

        // neighbor point lies right of segment
        // -> continue with the neighbor on the other side
        if (orientation > 0) {
          currentPointID = sndAppearID + 1;
          direction = 1;
        }
        // neighbor point lies left of segment
        // -> continue with the neighbor itself
        else {
          currentPointID = sndAppearID - 1;
          direction = -1;
        }
      }
      if (currentPointID == -1)
        currentPointID = pnum - 1;
      if (currentPointID == pnum)
        currentPointID = 0;
    }
    if (iterations >= pnum + 10)
      return (Vector<Point2D.Double>) pointlist.clone();
    return simplePoly;
  }

  /**
   * <pre>
   * 
   * Method to re-sample the line segments of the polygon in a range of a given
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
   * </pre>
   * @param segmentLength Target length of segments in resampling.
   */
  public void resample(double segmentLength) {
    // number of points in the current polygon
    int numPoints = this.points.size();
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
      double dist = Math.sqrt((Math.pow(distX, 2)) + (Math.pow(distY, 2)));
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
          this.points.insertElementAt(p, i + 1);
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
      double dist = Math.sqrt((Math.pow(distX, 2)) + (Math.pow(distY, 2)));
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
          this.points.addElement(p);
        }
      }
    }
    // console output
    System.out.println("   --> do resampling: number of points changed from "
        + numPoints + " to " + this.points.size());
  }

  /**
   * Method to shift the whole polygon outward (positive value) ore inward
   * (negative value) to its normal vector from every line segment.
   * <p>
   * ATTENTION!!! After shifting a polygon, it can happen that the polygon is
   * not simple !!!
   * 
   * @param shiftValue
   *          positive ore negative value to shift the polygon
   * @param imageSizeX
   *          width of the image to test whether the coordinates of the shifted
   *          polygon are valid
   * @param imageSizeY
   *          height of the image to test whether the coordinates of the shifted
   *          polygon are valid
   */
  public void shift(double shiftValue, int imageSizeX, int imageSizeY) {
    boolean signChanged = false;
    /*
     * Change sign of shift value if polygon is not counter clockwise, else on a
     * positive shift value the polygon is shifted inward instead of outward and
     * on a negative value the polygon is outward instead of inward. If the
     * polygon is not simple, an exception is thrown by the
     * jni_isCounterClockwiseOriented() method. The polygon must be simple for
     * shifting.
     */
//    try {
//      if (!this.jni_isCounterClockwiseOriented()) {
    	if (!this.isOrderedCounterClockwise()) {
        shiftValue = shiftValue * (-1.0);
        signChanged = true;
      }
//    } catch (MTBPolygon2DException e) {
//      System.out.println(">>> Error shift Polygon2D: ");
//      e.printStackTrace();
//      return;
//    }
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
    if (this.isClosed) { // shift on a closed polygon
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
        // add new point to the new list of polygon points
        newPoints.add(i, new Point2D.Double(newX, newY));
      }
    } else { // shift on a not closed polygon
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
        newPoints.add(i, new Point2D.Double(newX, newY));
      }
    }
    // assign new polygon points
    this.points = newPoints;
    // console output
    if (shiftValue >= 0) {
      if (signChanged == false) {
        System.out.println("   --> do shift outward: " + Math.abs(shiftValue)
            + " pixels");
      } else {
        System.out.println("   --> do shift inward: " + Math.abs(shiftValue)
            + " pixels");
      }
    }
    if (shiftValue < MTBConstants.epsilon) {
      if (signChanged == false) {
        System.out.println("   --> do shift inward: " + Math.abs(shiftValue)
            + " pixels");
      } else {
        System.out.println("   --> do shift outward: " + Math.abs(shiftValue)
            + " pixels");
      }
    }
  }

  public Point2D.Double standardization(Point2D.Double p) {
    double abs = Math.sqrt(((Math.pow(p.getX(), 2)) + (Math.pow(p.getY(), 2))));
    if (abs < MTBConstants.epsilon) {
      return (new Point2D.Double(p.x, p.y));
    }
    double x = ((p.x) / abs);
    double y = ((p.y) / abs);
    return (new Point2D.Double(x, y));
  }

  /**
   * Draw a polygon into an image (in red color).
   * 
   * @param img
   *          Image where to draw the polygon into.
   */
  public void drawPolygon(MTBImage img) {
    // define the polygon color: red
    int red = ((255 & 0xff) << 16) + ((0 & 0xff) << 8) + (0 & 0xff);
    this.drawPolygon(img, red);
  }

  /**
   * Draw a polygon into an image.
   * 
   * @param img			Image where to draw the polygon into.
   * @param color 	Color in which to draw the polygon.
   */
  public void drawPolygon(MTBImage img, int color) {

    // // define the polygon color: red
    // int color = ((255 & 0xff) << 16) + ((0 & 0xff) << 8) + (0 & 0xff);

    // get number of points
    int np = this.points.size();

    if (np == 0)
      return;

    // draw segments
    for (int i = 0; i < np - 1; i++) {
      int x1 = (int) Math.round(this.points.get(i).x);
      int y1 = (int) Math.round(this.points.get(i).y);
      int x2 = (int) Math.round(this.points.get(i + 1).x);
      int y2 = (int) Math.round(this.points.get(i + 1).y);
      img.drawLine2D(x1, y1, x2, y2, color);
    }
    if (this.isClosed) {
      // last segment N-1 -> 0
      int x1 = (int) Math.round(this.points.get(np - 1).x);
      int y1 = (int) Math.round(this.points.get(np - 1).y);
      int x2 = (int) Math.round(this.points.get(0).x);
      int y2 = (int) Math.round(this.points.get(0).y);
      img.drawLine2D(x1, y1, x2, y2, color);
    }
  }

  /**
   * Draw polygon points into an image (in red color and as crosses).
   * @param img	Image where to draw the polygon points into.
   */
  public void drawPolygonPoints(MTBImage img) {
    // define the polygon color: red
    int red = ((255 & 0xff) << 16) + ((0 & 0xff) << 8) + (0 & 0xff);
    this.drawPolygonPoints(img, red, 2);
  }

  /**
   * Draw polygon points into an image.
   * @param img 	Image where to draw the polygon points into.
   * @param color	Color to be used.
   * @param mode	Shape to be drawn.
   */
  public void drawPolygonPoints(MTBImage img, int color, int mode) {
    // get number of points
    int np = this.points.size();
    if (np == 0)
      return;

    // draw points
    int x=0,y=0;
    for (int i = 0; i < np; i++) {
      x = (int) Math.round(this.points.get(i).x);
      y = (int) Math.round(this.points.get(i).y);
      img.drawPoint2D(x, y, 0, color, mode);
    }
  }

  /* (non-Javadoc)
   * @see de.unihalle.informatik.MiToBo.core.datatypes.interfaces.MTBDataExportableToImageJROI#convertToRoi()
   */
  @Override
	public PolygonRoi[] convertToImageJRoi() {
		int[] xPoints = new int[this.getPointNum()];
		int[] yPoints = new int[this.getPointNum()];
		Vector<Point2D.Double> pts = this.getPoints();
		int i = 0;
		double scaleFactor = 1.0;
		if (this.getClass() == MTBSnake.class) {
			scaleFactor = ((MTBSnake)this).getScaleFactor();
		}
		for (Point2D.Double p : pts) {
			xPoints[i] = (int) (p.x * scaleFactor);
			yPoints[i] = (int) (p.y * scaleFactor);
			++i;
		}
		PolygonRoi p = 
			new PolygonRoi(xPoints, yPoints, pts.size(), Roi.POLYGON);
		return new PolygonRoi[]{p}; 
	}
  
  /**
   * Helper class for function simplify().
   * <p>
   * This class allows to differentiate between common 2D points in Java and
   * polygon intersection points, while keeping them in a single list.
   * 
   * @author moeller
   */
  private class IntersectionPoint2D extends Point2D.Double {

    /**
     * Default constructor.
     * 
     * @param xx
     *          x-coordinate of the point.
     * @param yy
     *          y-coordinate of the point.
     */
    public IntersectionPoint2D(double xx, double yy) {
      super(xx, yy);
    }

    /**
     * Checks if two intersection points are equal.
     * <p>
     * According to numerical inaccuracies during intersection calculation,
     * intersection points referring to the same intersection may have small
     * differences in their coordinates. Hence, they are assumed to be equal, if
     * these differences are sufficiently small.
     * 
     * @see java.awt.geom.Point2D#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object ptc) {
      // if we do not have an intersection point, equality is impossible
      if (!(ptc instanceof IntersectionPoint2D))
        return false;
      IntersectionPoint2D pointToTest = (IntersectionPoint2D) ptc;
      return (Math.abs(this.x - pointToTest.x) < 0.0001 && Math.abs(this.y
          - pointToTest.y) < 0.0001);
    }
  }
}
