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

import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.MiToBo_xml.MTBXMLPoint3DType;

/**
 * The Point3D class defines a point representing a location in (x, y, z)
 * coordinate space. The coordinates are specified in double precision.
 * 
 * @author glass
 */
@ALDParametrizedClass
public class MTBPoint3D implements Comparable<MTBPoint3D>
{
		/**
		 * Point coordinate at x-dimension.
		 */
		@ALDClassParameter(label="x coordinate")
		public double x;
		/**
		 * Point coordinate at y-dimension.
		 */
		@ALDClassParameter(label="y coordinate")
		public double y;
		/**
		 * Point coordinate at z-dimension.
		 */
		@ALDClassParameter(label="z coordinate")
		public double z;

		/**
		 * Standardconstructor to create an new empty 3D point object.
		 */
		public MTBPoint3D() {
				this.x = 0.0;
				this.y = 0.0;
				this.z = 0.0;
		}

		/**
		 * Construct a new 3D point with the given coordinates.
		 * 
		 * @param x
		 *          coordinate for x-dimension
		 * @param y
		 *          coordinate for y-dimension
		 * @param z
		 *          coordinate for z-dimension
		 */
		public MTBPoint3D(double x, double y, double z) {
				this.x = x;
				this.y = y;
				this.z = z;
		}

		/**
		 * Construct a new 3D point from the given 3D point.
		 * 
		 * @param p
		 *          3D point
		 */
		public MTBPoint3D(MTBPoint3D p) {
				this.x = p.x;
				this.y = p.y;
				this.z = p.z;
		}
		
		/**
		 * Construct a new MTBPoint3D from a 3D-point object constructed from xml-representation.
		 * @param p 3D-point in xml-representation
		 */
		public MTBPoint3D(MTBXMLPoint3DType p) {
			this.x = p.getX();
			this.y = p.getY();
			this.z = p.getZ();
		}

		/**
		 * Generate xml-representation of this 3D-point. 
		 */
		public MTBXMLPoint3DType toXMLType() {
			MTBXMLPoint3DType p = MTBXMLPoint3DType.Factory.newInstance();
			p.setX(this.x);
			p.setY(this.y);
			p.setZ(this.z);
			return p;
		}
		
		/**
		 * Return coordinate of x-dimension.
		 * 
		 * @return x-coordinate.
		 */
		public double getX() {
				return x;
		}

		/**
		 * Return coordinate of y-dimension.
		 * 
		 * @return y-coordinate.
		 */
		public double getY() {
				return y;
		}

		/**
		 * Return coordinate of z-dimension.
		 * 
		 * @return z-coordinate.
		 */
		public double getZ() {
				return z;
		}

		/**
		 * Set coordinate of x-dimension.
		 * 
		 * @param x
		 *          x-coordinate
		 */
		public void setX(double x) {
				this.x = x;
		}

		/**
		 * Set coordinate of y-dimension.
		 * 
		 * @param y
		 *          y-coordinate
		 */
		public void setY(double y) {
				this.y = y;
		}

		/**
		 * Set coordinate of z-dimension.
		 * 
		 * @param z
		 *          z-coordinate
		 */
		public void setZ(double z) {
				this.z = z;
		}

		/**
		 * Set coordinates of current 3D point object.
		 * 
		 * @param x
		 *          coordinate of x-dimension
		 * @param y
		 *          coordinate of y-dimension
		 * @param z
		 *          coordinate of z-dimension
		 */
		public void setLocation(double x, double y, double z) {
				this.x = x;
				this.y = y;
				this.z = z;
		}

		/**
		 * Translate current coordinates of the 3D point object. Given values will be
		 * added to each coordinate.
		 * 
		 * @param x
		 *          value to ad at x-coordinate
		 * @param y
		 *          value to ad at y-coordinate
		 * @param z
		 *          value to ad at z-coordinate
		 */
		public void translate(double x, double y, double z) {
				this.x += x;
				this.y += y;
				this.z += z;
		}

		/**
		 * Compute the Euclidean distance between this 3D point object and the given
		 * 3D point object.
		 * 
		 * @param p
		 *          3D point object
		 * @return Distance between this 3D point object and the given 3D point.
		 */
		public double distance(MTBPoint3D p) {
				return (Math.sqrt((x - p.x) * (x - p.x) + (y - p.y) * (y - p.y) + (z - p.z)
				    * (z - p.z)));
		}

		/**
		 * Compute the Euclidean distance between this 3D point object and the given
		 * location.
		 * 
		 * @param px
		 *          x-coordinate of the location
		 * @param py
		 *          y-coordinate of the location
		 * @param pz
		 *          z-coordinate of the location
		 * @return Distance between this 3D point object and the given location.
		 */
		public double distance(double px, double py, double pz) {
				return (Math.sqrt((x - px) * (x - px) + (y - py) * (y - py) + (z - pz)
				    * (z - pz)));
		}

		public int compareTo(MTBPoint3D q) 
		{
				int d;

				if (z < q.getZ()) 
				{
						d = -1;
				} 
				else if (z > q.getZ())
				{
						d = 1;
				}
				else // z-coordinate equals
				{
					if (y < q.getY())
					{
						d = -1;
					} 
					else if (y < q.getY())
					{
						d = 1;
					} 
					else // y-coordinate equals, too
					{
						if (x < q.getX())
						{
							d = -1;
						}
						else if (x > q.getX())
						{
							d = 1;
						}
						else // all 3 coordinates equal, so both points are said to be equal
						{
							d = 0;
						}
					}
				}
				
				return d;
		}

		@Override
		public boolean equals(Object obj) {
				MTBPoint3D q = (MTBPoint3D) obj;
				if (q.getX() == x && q.getY() == y && q.getZ() == z) {
						return true;
				} else {
						return false;
				}
		}

    @Override
    public int hashCode() {
        return (int)x + (int)y + (int)z;
    }
}
