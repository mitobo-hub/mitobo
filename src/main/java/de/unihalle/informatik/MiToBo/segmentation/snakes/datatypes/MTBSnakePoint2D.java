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

/* 
 * Most recent change(s):
 * 
 * $Rev$
 * $Date$
 * $Author$
 * 
 */

package de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes;

import java.awt.geom.Point2D;

/**
 * <pre>
 * 
 * Class to implement 2D snake points with a special structure:
 * 
 * (snake points means control points of the snake, on which the snake energy
 *  is calculated and optimized)
 *  
 * - every snake point has an old index (position) within the snake, if the point does not
 *   exists before, the old index is set to -1 by default
 * - every snake point has a Point2D.Double object for point coordinates
 * 
 * </pre>
 * 
 * 
 * @author misiak
 */
public class MTBSnakePoint2D extends Point2D.Double {

  private static final long serialVersionUID = 1L;
  /**
   * Old index (position) of the point within the snake.
   */
  protected int oldId;

  /**
   * Standard constructor to create a new SnakePoint2D
   */
  public MTBSnakePoint2D() {
    this.oldId = -1;
  }

  /**
   * Constructor to create a new SnakePoint2D with x- and y-coordinates for the
   * point. The old index of the point within the snake is set to -1 by default.
   * 
   * @param _x
   *          x-coordinate of the snake point
   * @param _y
   *          y-coordinate of the snake point
   */
  public MTBSnakePoint2D(double _x, double _y) {
    this.oldId = -1;
    this.x = _x;
    this.y = _y;
  }

  /**
   * Constructor to create a new SnakePoint2D from the specified Point2D.Double
   * object. The old index of the point within the snake is set to -1 by
   * default.
   * 
   * @param p
   *          Point2D.Double object with coordinates for new 2D snake point
   */
  public MTBSnakePoint2D(Point2D.Double p) {
    this.oldId = -1;
    this.x = p.x;
    this.y = p.y;
  }

  /**
   * Set old index of current snake point within the snake.
   * 
   * @param _oldId
   *          old index of the point point in the previous snake
   */
  public void setOldId(int _oldId) {
    this.oldId = _oldId;
  }

  /**
   * Overwrites Java Point2D method. Sets the location of this SnakePoint2D to
   * the specified double coordinates. The old index of the snake point is kept.
   */
  @Override
  public void setLocation(double _x, double _y) {
    setLocation(_x, _y, this.oldId);
  }

  /**
   * Overwrites Java Point2D method. Sets the location of this SnakePoint2D to
   * the same coordinates as the specified Point2D object. The old index of the
   * snake point is kept.
   */
  @Override
  public void setLocation(Point2D p) {
    setLocation(new Point2D.Double(p.getX(), p.getY()), this.oldId);
  }

  /**
   * Sets the location of this SnakePoint2D to the same coordinates as the
   * specified Point2D.Double object. The old index of the snake point is given
   * by the oldId value.
   * 
   * @param p
   *          2D point with new coordinates for the current 2D snake point
   * @param _oldId
   *          old index of the point within the snake
   */
  public void setLocation(Point2D.Double p, int _oldId) {
    this.x = p.x;
    this.y = p.y;
    this.oldId = _oldId;
  }

  /**
   * Sets the location of this SnakePoint2D to the specified double coordinates.
   * The old index of the snake point is given by the oldId value.
   * 
   * @param _x
   *          new x-coordinate of the current 2D snake point
   * @param _y
   *          new y-coordinate of the current 2D snake point
   * @param _oldId
   *          old index of the point within the snake
   */
  public void setLocation(double _x, double _y, int _oldId) {
    this.x = _x;
    this.y = _y;
    this.oldId = _oldId;
  }

  /**
   * Get previous index of snake point from the previous snake.
   * 
   * @return Current snake point index.
   */
  public int getOldId() {
    return this.oldId;
  }

  /**
   * Override java.lang.Object.clone() to create and return a copy of this
   * object.
   */
  @Override
  public MTBSnakePoint2D clone() {
    MTBSnakePoint2D tmpSnakePoint2D = new MTBSnakePoint2D(this.x, this.y);
    tmpSnakePoint2D.setOldId(this.oldId);
    return tmpSnakePoint2D;
  }
}
