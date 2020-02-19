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

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;

import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.Alida.operator.ALDData;
import de.unihalle.informatik.MiToBo_xml.MTBXMLPoint3DType;
import de.unihalle.informatik.MiToBo_xml.MTBXMLPoint3DVectorType;
import de.unihalle.informatik.MiToBo_xml.MTBXMLRegion3DType;

/**
 * Class to create a 3D region object. The center of mass and the volume of the
 * 3D region can be calculated as well as other 3D region properties.
 * 
 * 
 * @author glass
 */
@ALDParametrizedClass
public class MTBRegion3D extends ALDData {
		/**
		 * Points belonging to the region
		 */
		@ALDClassParameter(label="List of points", 
				changeValueHook="hookPointsUpdated") 
		private Vector<MTBPoint3D> points;
		/**
		 * The volume of the region (number of points included).
		 */
		private int volume;
		/**
		 * The centroid of the region (not normalised by volume!)
		 */
		private MTBPoint3D centroid;
		/**
		 * ID of the region (uniqueness is not guaranteed nor checked!)
		 */
		private int id;
		
		/**
		 * factors needed for compactness calculation
		 */
		private final double compactFactor = 0.2309008;	// = 3^(5/3)/(5*(4*pi)^(2/3))
		private final double five_thirds = 1.6666667;

		/**
		 * Construct a new MTBRegion3D object.
		 */
		public MTBRegion3D()
		{
			this.points = new Vector<MTBPoint3D>();
			this.volume = 0;
			this.centroid = new MTBPoint3D();
			this.id = 0;
		}

		/**
		 * Construct a new MTBRegion3D object from the given points.
		 * 
		 * @param p
		 *          vector of region points
		 */
		public MTBRegion3D(Vector<MTBPoint3D> p)
		{
			this.points = p;
			this.volume = p.size();
			this.centroid = calcCentre(p);
			this.id = 0;
		}
		
		
		/**
		 * Construct a new MTBRegion3D object from the given points and set its id
		 * 
		 * @param p
		 * @param id
		 */
		public MTBRegion3D(Vector<MTBPoint3D> p, int id)
		{
			this.points = p;
			this.volume = p.size();
			this.centroid = calcCentre(p);
			this.id = id;
		}

		/**
		 * Construct a new MTBRegion3D from a 3D-region in xml-representation.
		 * @param xmlregion3D 3D-region in xml-representation
		 */
		public MTBRegion3D(MTBXMLRegion3DType xmlregion3D) {
			this.id = xmlregion3D.getId();
			this.volume = xmlregion3D.getVolume();
			this.centroid = new MTBPoint3D(xmlregion3D.getCentroid());
			
			MTBXMLPoint3DType[] pts = xmlregion3D.getPoints().getPointArray();
			this.points = new Vector<MTBPoint3D>(pts.length);
			
			for (int i = 0; i < pts.length; i++) {
				this.points.add(new MTBPoint3D(pts[i]));
			}
		}
		
		public MTBXMLRegion3DType toXMLType() {
			MTBXMLRegion3DType xmlregion3D = MTBXMLRegion3DType.Factory.newInstance();
			
			xmlregion3D.setId(this.id);
			xmlregion3D.setCentroid(this.centroid.toXMLType());
			xmlregion3D.setVolume(this.volume);
			
			MTBXMLPoint3DType[] pts = new MTBXMLPoint3DType[this.points.size()];
			
			for (int i = 0; i < this.points.size(); i++) {
				pts[i] = this.points.get(i).toXMLType();
			}
			
			MTBXMLPoint3DVectorType ptsVec = MTBXMLPoint3DVectorType.Factory.newInstance();
			ptsVec.setPointArray(pts);
			
			xmlregion3D.setPoints(ptsVec);
			
			return xmlregion3D;
		}
		
		/**
		 * Set the ID of the region. Uniqueness is not guaranteed nor checked!
		 * 
		 * @param id
		 *          region ID
		 */
		public void setID(int id) {
				this.id = id;
		}

		/**
		 * Return the ID of the region. Uniqueness is publicnot guaranteed nor
		 * checked!
		 * 
		 * @return Region ID
		 */
		public int getID() {
				return this.id;
		}

		/**
		 * Return the points which are included in the 3D region.
		 * 
		 * @return Region points.
		 */
		public Vector<MTBPoint3D> getPoints() {
				return points;
		}

		/**
		 * Return the volume of the 3D region (number of points within the region).
		 * 
		 * @return Region volume.
		 */
		public double getVolume() {
				return volume;
		}

		/**
		 * Return the center of the region as 3D point.
		 * 
		 * @return Center of region.
		 */
		public MTBPoint3D getCentre() {
				MTBPoint3D c = new MTBPoint3D(centroid);
				c.setX(c.getX() / this.volume);
				c.setY(c.getY() / this.volume);
				c.setZ(c.getZ() / this.volume);
				return c;
		}

		/**
		 * Return the x-coordinate of the center of the region.
		 * 
		 * @return Coordinate of the center in x-dimension.
		 */
		public double getCentreX() {
				return (centroid.getX() / volume);
		}

		/**
		 * Return the y-coordinate of the center of the region.
		 * 
		 * @return Coordinate of the center in y-dimension.
		 */
		public double getCentreY() {
				return (centroid.getY() / volume);
		}

		/**
		 * Return the z-coordinate of the center of the region.
		 * 
		 * @return Coordinate of the center in z-dimension.
		 */
		public double getCentreZ() {
				return (centroid.getZ() / volume);
		}

		/**
		 * Append a 3D point to the region. The point is added at the end of the point
		 * list.
		 * 
		 * @param p
		 *          point to add
		 */
		public void addPoint(MTBPoint3D p) {
				points.addElement(p);
				volume++;
				centroid.translate(p.getX(), p.getY(), p.getZ());
		}

		/**
		 * Append a point to the region from the given coordinates. The point is added
		 * at the end of the point list.
		 * 
		 * @param px
		 *          coordinate in x-dimension
		 * @param py
		 *          coordinate in y-dimension
		 * @param pz
		 *          coordinate in z-dimension
		 */
		public void addPoint(double px, double py, double pz) {
				points.addElement(new MTBPoint3D(px, py, pz));
				volume++;
				centroid.translate(px, py, pz);
		}

		/**
		 * Calculate the centroid (not yet normalized by volume) of a given vector of
		 * points.
		 * 
		 * @param p
		 *          vector of Point3D
		 * @return The centroid (not yet normalised by volume).
		 */
		public MTBPoint3D calcCentre(Vector<MTBPoint3D> p) {
				MTBPoint3D c = new MTBPoint3D();
				for (int n = 0; n < p.size(); n++) {
						c.translate(p.elementAt(n).getX(), p.elementAt(n).getY(), p.elementAt(n)
						    .getZ());
				}
				return c;
		}

		
		/**
		 * 
		 * @param p
		 * @param q
		 * @param r
		 * @return moment of order p, q, r of the region
		 */
		public double getMoment(int p, int q, int r)
		{
			double m = 0;
			
			for(int i = 0; i < this.points.size(); i++)
			{
				MTBPoint3D point = points.elementAt(i);
				m += Math.pow(point.x, p) * Math.pow(point.y, q) * Math.pow(point.z, r);
			}
			
			return m;
		}
		
		
		/**
		 * 
		 * @param p
		 * @param q
		 * @param r
		 * @return central moment of order p, q, r of the region (invariant under translation)
		 */
		public double getCentralMoment(int p, int q, int r)
		{
			double cm = 0;
			
			double volume = getMoment(0,0,0);
			double mx = getMoment(1,0,0) / volume;
			double my = getMoment(0,1,0) / volume;
			double mz = getMoment(0,0,1) / volume;
			
			for(int i = 0; i < this.points.size(); i++)
			{
				MTBPoint3D point = points.elementAt(i);
				cm += Math.pow(point.x - mx, p) * Math.pow(point.y - my, q) * Math.pow(point.z - mz, r);
			}
			
			return cm;
		}
		
		
		/**
		 * calculates compactness value according to <br/>
		 * Carlos Martinez-Ortiz. "2D and 3D Shape Descriptors". PhD thesis. 2010.<br/>
		 * compactness measures the deviation of an object's shape from a sphere<br/>
		 * its values range between (0,1], wheras 1 indicates a sphere
		 * 
		 * @return compactness
		 */
		public double getCompactness()
		{
			double cm000 = getCentralMoment(0, 0, 0);
			double cm200 = getCentralMoment(2, 0, 0);
			double cm020 = getCentralMoment(2, 0, 0);
			double cm002 = getCentralMoment(2, 0, 0);
			
			return compactFactor * (Math.pow(cm000, five_thirds)) / (cm200 + cm020 + cm002);
		}
		
		/**
		 * Calculate the expansion in z-direction of the region.
		 * 
		 * @return Expansion of the region in z-direction.
		 */
		public int getSizeZ() {
				int max = (int) points.elementAt(0).getZ();
				int min = (int) points.elementAt(0).getZ();
				for (int i = 0; i < points.size(); i++) {
						int z = (int) points.elementAt(i).getZ();
						if (z > max) {
								max = z;
						} else if (z < min) {
								min = z;
						}
				}
				return max - min + 1;
		}

		/**
		 * Calculate the expansion in y-direction of the region.
		 * 
		 * @return Expansion of the region in y-direction.
		 */
		public int getSizeY() {
				int max = (int) points.elementAt(0).getY();
				int min = (int) points.elementAt(0).getY();
				for (int i = 0; i < points.size(); i++) {
						int y = (int) points.elementAt(i).getY();
						if (y > max) {
								max = y;
						} else if (y < min) {
								min = y;
						}
				}
				return max - min + 1;
		}

		/**
		 * Calculate the expansion in x-direction of the region.
		 * 
		 * @return Expansion of the region in x-direction.
		 */
		public int getSizeX() {
				int max = (int) points.elementAt(0).getX();
				int min = (int) points.elementAt(0).getX();
				for (int i = 0; i < points.size(); i++) {
						int x = (int) points.elementAt(i).getX();
						if (x > max) {
								max = x;
						} else if (x < min) {
								min = x;
						}
				}
				return max - min + 1;
		}

		/**
		 * Write the coordinates of the included 3D points into a text file.
		 * 
		 * @param fileName
		 *          File name to store the region on disk.
		 */
		public void regionToFile(String fileName) {
				try {
						BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));

						for (int i = 0; i < this.points.size(); i++) {
								bw.write("[" + this.points.elementAt(i).getX() + ", "
								    + this.points.elementAt(i).getY() + ", "
								    + this.points.elementAt(i).getZ() + "], ");

								if (i % 5 == 0) // line break after 5 points
								{
										bw.newLine();
								}
						}
						bw.close();
				} catch (FileNotFoundException e) {
						e.printStackTrace();
				} catch (IOException e) {
						e.printStackTrace();
				}
		}
		
		
		/**
		   * Calculates the axes-parallel bounding box of the region.
		   * <p>
		   * The function extracts the coordinates of the back upper left and front lower right
		   * corner of the bounding box of the region. Note that the there is at least
		   * one point of the region lying on each side of the bounding box, i.e. the
		   * region not just touches the box, but lies on it.
		   * <p>
		   * The result array contains the corner coordinates in the following order:
		   * [xmin, ymin, zmin, xmax, ymax, zmax]
		   * 
		   * @return Coordinates of back upper left and front lower right corners.
		   */
		  public double[] getBoundingBox() 
		  {
		    double minX = Double.MAX_VALUE;
		    double maxX = 0;
		    double minY = Double.MAX_VALUE;
		    double maxY = 0;
		    double minZ = Double.MAX_VALUE;
		    double maxZ = 0;

		    for (MTBPoint3D p : this.points) 
		    {
		      if (p.x > maxX)
		      {
		    	  maxX = p.x;
		      }
		      if (p.x < minX)
		      {
		    	  minX = p.x;
		      }        
		      if (p.y > maxY)
		      {
		    	  maxY = p.y;
		      }
		      if (p.y < minY)
		      {
		    	  minY = p.y;
		      }
		      if(p.z > maxZ)
		      {
		    	  maxZ = p.z;
		      }	  
		      if(p.z < minZ)
		      {
		    	  minZ = p.z;
		      }  
		    }
		    
		    double[] bbox = new double[6];
		    bbox[0] = minX;
		    bbox[1] = minY;
		    bbox[2] = minZ;
		    bbox[3] = maxX;
		    bbox[4] = maxY;
		    bbox[5] = maxZ;
		    
		    return bbox;
		  }
		  
			/**
			 * Function to update object state after setting new point list.
			 * <p>
			 * It is assumed that point list is up-to-date before calling the hook.
			 */
			protected void hookPointsUpdated() {
				if (this.points != null) {
					this.volume = this.points.size();
					this.centroid = this.calcCentre(this.points);
				}
				else {
					this.volume = 0;
					this.centroid = null;
				}
			}
}
