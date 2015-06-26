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

package de.unihalle.informatik.MiToBo.core.datatypes;

import java.awt.geom.*;

import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;
import de.unihalle.informatik.MiToBo.core.datatypes.defines.MTBConstants;

/**
 * Enhanced 2D line segments.
 * <p>
 * This class adds some useful functions to {@link java.awt.geom.Line2D.Double},
 * mostly concerning geometrical calculations like intersections, orientations,
 * scalar products and distances.
 * 
 * @see java.awt.geom.Line2D.Double
 * 
 * @author moeller
 */
@ALDMetaInfo(export=ExportPolicy.ALLOWED)
public class MTBLineSegment2D extends Line2D.Double {

		/**
		 * Constructor.
		 * 
		 * @param X1	x-coordinate of first point.
		 * @param Y1	y-coordinate of first point.
		 * @param X2	x-coordinate of second point.
		 * @param Y2	y-coordinate of second point.
		 */
		public MTBLineSegment2D(double X1, double Y1, double X2, double Y2) {
				super(X1, Y1, X2, Y2);
		}

		/**
		 * Calculates the scalar product of the given segment to this one.
		 * <p>
		 * The segments are interpreted as vectors, directed from the first point
		 * of the segment to the second.
		 * 
		 * @param ls	Input line segment.
		 * @return Value of scalar product.
		 */
		public double scalprod(MTBLineSegment2D ls) {
				double v1_x = this.x2 - this.x1;
				double v1_y = this.y2 - this.y1;
				double v2_x = ls.x2 - ls.x1;
				double v2_y = ls.y2 - ls.y1;
				return v1_x * v2_x + v1_y * v2_y;
		}

		/**
		 * Calculates the Euclidean norm of the segment.
		 * 
		 * @return Length of the segment.
		 */
		public double getNorm() {
				return Math.sqrt((this.x2 - this.x1) * (this.x2 - this.x1)
				    + (this.y2 - this.y1) * (this.y2 - this.y1));
		}

		/**
		 * Checks if a given point is part of the segment.
		 * 
		 * @param px	x-coordinate of the point.
		 * @param py  y-coordinate of the point.
		 * @return True if point lies on the segment.
		 */
		public boolean containsPoint(double px, double py) {
				double lambda = -1;
				if (Math.abs(this.x2 - this.x1) > MTBConstants.epsilon) {
						lambda = (px - this.x1) / (this.x2 - this.x1);
						if (lambda >= 0
						    && lambda <= 1
						    && Math.abs(this.y1 + lambda * (this.y2 - this.y1) - py) < 
						    																				MTBConstants.epsilon)
								return true;
				}
				if (Math.abs(this.y2 - this.y1) > MTBConstants.epsilon) {
						lambda = (py - this.y1) / (this.y2 - this.y1);
						if (lambda >= 0
						    && lambda <= 1
						    && Math.abs(this.x1 + lambda * (this.x2 - this.x1) - px) < 
						    																				MTBConstants.epsilon)
								return true;
				}
				return false;
		}

		@Deprecated
		public double getPointDist(double x, double y) {

				double a_x = x2 - x1;
				double a_y = y2 - y1;

				// System.out.println("ax= " + a_x + " , ay= " + a_y);

				double b_x = x - x1;
				double b_y = y - y1;

				// System.out.println("bx= " + b_x + " , by= " + b_y);

				double scalprod = a_x * b_x + a_y * b_y;
				double norm_a = a_x * a_x + a_y * a_y;

				// System.out.println("scal= " + scalprod + " , norma= " + norm_a);

				double v_x = 1.0 / norm_a * scalprod * a_x;
				double v_y = 1.0 / norm_a * scalprod * a_y;

				// System.out.println("vx= " + v_x + " , vy= " + v_y);

				double w_x = v_x - b_x;
				double w_y = v_y - b_y;

				// System.out.println("wx= " + w_x + " , wy= " + w_y);

				double norm_w = Math.sqrt(w_x * w_x + w_y * w_y);
				// System.out.println("normw= " + norm_w);
				return norm_w;
		}

		/**
		 * Get orientation of point relative to segment.
		 * <p>
		 * If return value is positive, point lies left of the segment, if it is
		 * negative, the point is located on the right. If the return value is 
		 * zero, the point is located on the segment or at least on the line to 
		 * which the segment belongs to.
		 * 
		 * @param px	x-coordinate of the point.
		 * @param py  y-coordinate of the point.
		 * @return Orientation of point with regard to segment.
		 */
		@Deprecated
		public double getOrientation(double px, double py) {

				MTBLineSegment2D pseg = new MTBLineSegment2D(this.x1, this.y1, px, py);

				double scalProd = this.scalprod(pseg);
				double normSquare = this.getNorm() * this.getNorm();
				double lambda = scalProd / normSquare;

				double inter_x = this.x1 + lambda * (this.x2 - this.x1);
				double inter_y = this.y1 + lambda * (this.y2 - this.y1);

				double gamma = 0;
				if (Math.abs(px - inter_x) > MTBConstants.epsilon)
						gamma = (this.y1 - this.y2) / (px - inter_x);
				else if (Math.abs(py - inter_y) > MTBConstants.epsilon)
						gamma = (this.x2 - this.x1) / (py - inter_y);
				return gamma;
		}

		/**
		 * Calculates the point of intersection between the segments.
		 * 
		 * @param ls	Line segment to be checked.
		 * @return True, if both segments intersect.
		 */
		public Point2D.Double getIntersection(MTBLineSegment2D ls) {

				double sx_this = this.x1;
				double sy_this = this.y1;
				double sx_that = ls.x1;
				double sy_that = ls.y1;

				double vx_this = this.x2 - this.x1;
				double vy_this = this.y2 - this.y1;
				double vx_that = ls.x2 - ls.x1;
				double vy_that = ls.y2 - ls.y1;

				double denom = 0, counter = 0;
				if (Math.abs(vy_this) > MTBConstants.epsilon) {
						counter = sx_this - sx_that + 
											((sy_that - sy_this) * vx_this) / (vy_this);
						denom = vx_that - (vy_that * vx_this) / vy_this;
				} else {
						counter = sy_this - sy_that + 
											((sx_that - sx_this) * vy_this) / (vx_this);
						denom = vy_that - (vx_that * vy_this) / vx_this;
				}
				double gamma = counter / denom;
				double i_x = sx_that + gamma * vx_that;
				double i_y = sy_that + gamma * vy_that;
				return new Point2D.Double(i_x, i_y);
		}
}
