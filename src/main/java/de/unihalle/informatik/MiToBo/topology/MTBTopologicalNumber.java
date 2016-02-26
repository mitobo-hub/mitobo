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
 * $Author: posch $
 * 
 */

package de.unihalle.informatik.MiToBo.topology;

import java.util.*;

import de.unihalle.informatik.MiToBo.segmentation.basics.MTBSegmentationInterface;


/** An abstract class to compute topological numbers given a neighborhood.
  * <p>
  * See
  * Han, X. and Xu, C. and Prince, J.L., A topology preserving level set method for geometric deformable models},
  * PAMI, pages 755-768, 2003
  * <br>
  * for definitions and notation.
  */


public abstract class MTBTopologicalNumber {


	/** debug flag
	 */
	protected static boolean debug = false;

	/** 
	  * For each neighbor in the neighborhood 
      * this gives their coordinates in a 3x3x3 array center around the
      * current pixel (of which the neighborhood is represented).
      * The coordinates of this current pixel are (1,1,1).
      * The order of the neighbors is define in the class extending this abstract super class.
	  */
	protected Point3D coordinatesNeighbors[];

	/** In analogy to <code>coordinatesNeighbors</code> this array hold the offsets of the neighbors.
	  * Each offset may also be considered as the coordinates of a neighbor in a
	  * a 3x3x3 array center around the
      * current pixel where 
      * the coordinates of this current pixel are (0,0,0).
      * <p>
      * The order of neighbors within the array are the same as for <code>coordinatesNeighbors</code>.
	  */
	public Point3D offsetsNeighbors[];

	/** For each neighbor in the neighborhood 
	 * this vector holds coordinates of all other pixels in the neighborhood
	 * which are connected to the neighbor under consideration with regard the
	 * the neighborhood definition. (These are called neighborneighbors subsequently.)
      * The order of neighbors within the array are the same as for <code>coordinatesNeighbors</code> 
      * while the order of neighborneighbors in the vector is arbitrary.
	  *
	  */
	protected Vector<Point3D>  coordinatesNeighborNeighbors[];

	/** As <code>coordinatesNeighborNeighbors</code> this array give
	 * for each neighbor in the neighborhood coordinates of its neighbors
	 * (i.e. neighborneighbors).
	 * However the vectors of neighborneighbors are not indexed by the order of
	 * the neighbors but by the coordinate of each neighbor
     * within an  3x3x3 array, {@link coordinatesNeighbors}.
	  *
      *  The vector are define in analogy as in @see indicesNeighbors.
	  */
	protected Vector<Point3D>  coordinatesNeighborNeighborsByCoord[][][];

	/** As for the neighbors this vector is in analogy to <code>coordinatesNeighborNeighbors</code>
	 * and gives for each neighborneighbor the offset instead of coordinates.
	  */
	protected Vector<Point3D>  offsetsNeighborNeighbors[];

    /** 8- or 26 neighbors of the current pixel
     * used to determine topological numbers represented as an 3x3x3 array center around
	 * the current pixel.
	 * Memory layout is <code>nbClasses3D[z][y][x]</code>.
	 * <p>
	 * First it is assigned to the array <code>X</code> of Han et al, i.e.:
     * An entry is true if class in neighborhood is equal to the class under consideration, 
     * false otherwise.
	 * <p>
	 * Subsequently to the array N_n^k
	 * <p>
	 * And finally altered to compute number of components.
	 * <p>
     * This array is recycled for efficiency.
     */
    protected boolean[][][] X = new boolean[3][3][3];

	// ================== Methods ===============================================================0

	/** Check if topological for the class <code>c</code> is one in the
	  * neighborhood <code>nbClassess3D</code>. Specifically, all pixels in 
	  * <code>nbClasses3D</code> with equal values to <code>c</code>
	  * are considered as elements of X, the rest as not in X.
	  * <p>
	  * <code>nbClassess3D</code> is a 3D volume of size 3x3x3
	  * where the current pixel is located in the center, i.e. with coordinates (1,1,1).
	  * Memory layout is <code>nbClasses3D[z][y][x]</code>.
	  * The values give the classes or phases to which the pixels belong to.
	  *<p>
	  * Neighborhood definitions are define in derived sub classes.
	  * If this is a 2D neighborhood, only the <code>z=0</code> slice of 
	  * <code>nbClasses3D</code> will be considered and accessed.
      */
	abstract public boolean topoNumberIsOne( int [][][] nbClasses3D, int c);

	/** Check if topological for the class <code>c</code> is one in the
	 * pixel with coordinate <code>(x,y,z)</code> in the segmentation object
	 * <code>segmentation</code>.
	  *  Specifically, all pixels in 
	  * neighborhood with equal values to <code>c</code>
	  * are considered as elements of X, the rest as not in X.
	  *<p>
	  * Neighborhood definitions are define in derived sub classes.
	  * <p>
	  * If this segmentation is 2D a 2D topology is to be used
	  * <p>
	  * TODO: conceive how to handle invalid pixels
     */
	 public boolean topoNumberIsOne( MTBSegmentationInterface segmentation, 
			int x, int y, int z, int c) {
		 int[][][] nbClasses3D = new int[3][3][3];
		 
		 if ( segmentation.getSizeZ() == 1) {
			 for ( int deltaZ = -1 ; deltaZ <= 1 ; deltaZ++) {
				 for ( int deltaX = -1 ; deltaX <= 1 ; deltaX++) {
					 nbClasses3D[0][deltaZ+1][deltaX+1] =
							 segmentation.getClass(x+deltaX, y+deltaZ, 0);
				 }
			 }
		 } else {
			 for ( int deltaz = -1 ; deltaz <= 1 ; deltaz++) {
				 for ( int deltaZ = -1 ; deltaZ <= 1 ; deltaZ++) {
					 for ( int deltaX = -1 ; deltaX <= 1 ; deltaX++) {
						 nbClasses3D[deltaz+1][deltaZ+1][deltaX+1] =
								 segmentation.getClass(x+deltaX, y+deltaZ, z+deltaz);
					 }
				 }
			 }
		 }
         if(!topoNumberIsOne(nbClasses3D,c))
         {
//                 for ( int deltaY = -1 ; deltaY <= 1 ; deltaY++) {
//                     for ( int deltaX = -1 ; deltaX <= 1 ; deltaX++) {
//                         System.out.print(nbClasses3D[0][deltaY+1][deltaX+1] + " | ");
//                     }
//                     System.out.println();
//                 }
             return false;
         }
         else
             return true;
		// return topoNumberIsOne(nbClasses3D, c);
	 }

	/** Derive N_n^k from X, see Han et al.
	 * This method works destructive on X (for efficiency),
     * that is upon return X contains N_n^k, which is a subset of X.
	 * n and k are define in derived sub classes.
	 */
	abstract protected void computeN();

	/** Checks if the neighborhood represented in X has exactly one connected
      * component of entries set to true using the neighborhood definition as
      * as represented in the neighborhood arrays of the class.
	 */
	protected boolean hasOneCC() {
		// find first pixel of a connected component
		boolean foundFirstCC = false;
		int n;
		for ( n=0; n < coordinatesNeighbors.length ; n++) {
			if ( X[coordinatesNeighbors[n].z][coordinatesNeighbors[n].y][coordinatesNeighbors[n].x] ) {
				foundFirstCC = true;
				visitCC( coordinatesNeighbors[n].z, coordinatesNeighbors[n].y, coordinatesNeighbors[n].x);
				break;
			}
		}

		// check if exactly one component
		if ( foundFirstCC ) {
			for ( ; n < coordinatesNeighbors.length ; n++) {
				if ( X[coordinatesNeighbors[n].z][coordinatesNeighbors[n].y][coordinatesNeighbors[n].x] )
					return false; // we found a second component
			}
		} else {
			return false; // zero components
		}

		return true;
	}

	/** Visit (x,y,z): reset the corresponding entry in X to false
      * and recursively visit all neighbors of (x,y,z) in X.
	  */
	private void visitCC( int z, int y, int x) {
		X[z][y][x] = false;
		for ( Point3D indices : coordinatesNeighborNeighborsByCoord[z][y][x] ) {
			if ( X[indices.z] [indices.y] [indices.x] ) {
				visitCC( indices.z, indices.y, indices.x);
			}
		}
	}


	/** Print information of this class to stdout
	 */
	public void print() {
		System.out.println( "Details of " + this.getClass().getName() +
			" (" + this.hashCode() + ")");
        for ( int i = 0 ; i < coordinatesNeighborNeighbors.length ; i++ ) {
            System.out.println( "pixel " + i + " @(" + coordinatesNeighbors[i].z + "," + 
                                coordinatesNeighbors[i].y + "," + coordinatesNeighbors[i].x + ")" +
								" offset:  @(" + offsetsNeighbors[i].z + "," +
                                offsetsNeighbors[i].y + "," + offsetsNeighbors[i].x + ")" );
            for ( int n = 0; n < coordinatesNeighborNeighbors[i].size() ; n++ ) {
				Point3D indices = coordinatesNeighborNeighbors[i].elementAt(n);
                System.out.print( " neighbor: " + n + " @(" + 
                    indices.z + "," + indices.y +  "," + indices.x + ")");
            }
            System.out.println();

            for ( int n = 0; n < offsetsNeighborNeighbors[i].size() ; n++ ) {
            	Point3D indices = offsetsNeighborNeighbors[i].elementAt(n);
                System.out.print( " offset: " + n + " @(" + 
                    indices.z + "," + indices.y  + "," + indices.x + ")");
            }
            System.out.println();
        }

	}

	/** Returns a iterator for the coordinatesNeighbors in the order as defined
      */
	public Iterator<Point3D> iterator() {
		return new PixelIndexIterator();
	}

	/** Returns a iterator for the pixelOffsets in the order as defined
      */
	public Iterator<Point3D> iteratorOffsets() {
		return new PixelOffsetIterator();
	}

	private class PixelIndexIterator implements Iterator<Point3D> {
		int n;

		@Override
        public void remove() {
            System.err.println( "PixelIndexIterator: cannot remove elements");
			//TODO throw exception
        }

		public PixelIndexIterator() {
			n = 0;
		}

		@Override
		public boolean hasNext() {
			return n < coordinatesNeighbors.length;
		}

		@Override
		public Point3D next() {
			n++;
			return coordinatesNeighbors[n-1];
		}
	}

	public class PixelOffsetIterator implements Iterator<Point3D> {
		int n;

		@Override
        public void remove() {
            System.err.println( "PixelOffsetIterator: cannot remove elements");
			//TODO throw exception
        }

		public PixelOffsetIterator() {
			n = 0;
		}

		@Override
		public boolean hasNext() {
			return n < offsetsNeighbors.length;
		}

		@Override
		public Point3D next() {
			n++;
			return offsetsNeighbors[n-1];
		}
	}
	
	/**
	 * This class is used as a light weight 3D point implementation
	 * 
	 * @author posch
	 *
	 */
	public class Point3D {
		int x;
		int y;
		int z;
		
		public Point3D( int z, int y, int x) {
			this.z = z;
			this.y = y;
			this.x = x;
		}
		
		public Point3D() {
		}

		/**
		 * @return the x
		 */
		public int getX() {
			return x;
		}

		/**
		 * @param x the x to set
		 */
		public void setX(int x) {
			this.x = x;
		}

		/**
		 * @return the y
		 */
		public int getY() {
			return y;
		}

		/**
		 * @param y the y to set
		 */
		public void setY(int y) {
			this.y = y;
		}

		/**
		 * @return the z
		 */
		public int getZ() {
			return z;
		}

		/**
		 * @param z the z to set
		 */
		public void setZ(int z) {
			this.z = z;
		}
		
	}
}
