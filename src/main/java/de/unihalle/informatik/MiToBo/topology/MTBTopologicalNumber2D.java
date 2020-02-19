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

package de.unihalle.informatik.MiToBo.topology;

import java.util.ArrayList;

/** An abstract class to compute topological numbers for the 2D case.
 *
 * The pixels in the neighborhood are sorted as follows:
 * First the four 4-neighbors, then the four 8- but not 4-neighbors.
 */


abstract public class MTBTopologicalNumber2D extends MTBTopologicalNumber {

	public MTBTopologicalNumber2D () {
		coordinatesNeighbors = new Point3D[] {
				new Point3D(0,1,2),
				new Point3D(0,2,1),
				new Point3D(0,1,0),
				new Point3D(0,0,1),
				new Point3D(0,2,2),
				new Point3D(0,2,0),
				new Point3D(0,0,0),
				new Point3D(0,0,2)
		};
	}

	/** This initializes all relevant arrays containing information on neighbors
      * where <code>maxDist</code> defines the neighborhood, e.g. <code>maxDist</code> == 1 defines 4-neighbors.
	  * Theses arrays are <code>coordinatesNeighbors</code>, <code>offsetsNeighbors</code>,
	  * and <code>coordinatesNeighborNeighbors</code>.
      *
      * @param dist All pixels within the 3x3 window around the current pixel
      *             with euclidean distance not larger then <code>dist</code> are considered neighbors of 
      *             the current pixel.
	  */
	@SuppressWarnings("unchecked")
	protected void initNeighbors() {
		offsetsNeighbors = new Point3D[8];
		coordinatesNeighborNeighbors = new ArrayList[8];
		offsetsNeighborNeighbors = new ArrayList[8];
		coordinatesNeighborNeighborsByCoord = new ArrayList[3][3][3];

		for ( int n=0 ; n < coordinatesNeighbors.length ; n++ ) {
			int z = coordinatesNeighbors[n].getZ();
			int y = coordinatesNeighbors[n].getY();
			int x = coordinatesNeighbors[n].getX();

			offsetsNeighbors[n] = new Point3D( 0, y-1,x-1);
			coordinatesNeighborNeighbors[n] = new ArrayList<Point3D>();
			offsetsNeighborNeighbors[n] = new ArrayList<Point3D>();

			coordinatesNeighborNeighborsByCoord[z][y][x] = new ArrayList<Point3D>();

			for ( int deltaY = -1 ; deltaY <= 1 ; deltaY++ ) {
				for ( int deltaX = -1 ; deltaX <= 1 ; deltaX++ ) {
					
					if ( 
						// not allowed, i.e. no neighbors :
						// delta (0,0) which is the pixel to compute the neighbors for
						! ( deltaX == 0 && deltaY == 0 ) &&

						// neighbors outside a 3x3 window around the current pixel
						(deltaX+coordinatesNeighbors[n].getX() >= 0) && (deltaX+coordinatesNeighbors[n].getX() <= 2 ) &&
						(deltaY+coordinatesNeighbors[n].getY() >= 0) && (deltaY+coordinatesNeighbors[n].getY() <= 2 ) &&

						// the current pixel located in the center of the 3x3 window at (1,1)
						! ( (deltaX+coordinatesNeighbors[n].getX() == 1) && (deltaY+coordinatesNeighbors[n].getY() == 1 ) ) &&

						// neighbors with euclidean distance greater dist
						(Math.sqrt( deltaX*deltaX + deltaY*deltaY) <= this.maxDist) ) {

						Point3D tmp = new Point3D (0, deltaY+coordinatesNeighbors[n].getY(), deltaX+coordinatesNeighbors[n].getX());
						coordinatesNeighborNeighbors[n].add( tmp);
						coordinatesNeighborNeighborsByCoord[z][y][x].add( tmp);
						offsetsNeighborNeighbors[n].add( new Point3D (0, deltaY, deltaX));
					}
				}
			}
		}
	}

	@Override
	public boolean topoNumberIsOne( int [][][] nbPhases3D, int p) {

		// build the set X
		for ( int y = 0 ; y <= 2 ; y++) {
			for ( int x = 0 ; x <= 2 ; x++) {
				if ( nbPhases3D[0][y][x] == p )
					X[0][y][x] = true;
				else
					X[0][y][x] = false;
			}
		}

		if ( debug ) {
			System.out.println( "TopologicalNumber2D::topoNumberIsOne for phase " + p +
				" using " + this.getClass().getName() +
				" (" + this.hashCode() + ")");
			System.out.println( "  X");
			for ( int y = 0 ; y <= 2 ; y++) {
				for ( int x = 0 ; x <= 2 ; x++) {
					System.out.print( "   " + X[0][y][x]);
				}
				System.out.println();
			}
		}


		// build N_n_k
		computeN();
		if ( debug ) {
			System.out.println( "  N_n_k");
			for ( int y = 0 ; y <= 2 ; y++) {
				for ( int x = 0 ; x <= 2 ; x++) {
					System.out.print( "   " + X[0][y][x]);
				}
				System.out.println();
			}
		}

		// compute connected components in N_n_k
		boolean res = hasOneCC();
		if ( debug ) {
			System.out.println( "  after hasOneCC");
			for ( int y = 0 ; y <= 2 ; y++) {
				for ( int x = 0 ; x <= 2 ; x++) {
					System.out.print( "   " + X[0][y][x]);
				}
				System.out.println();
			}
            System.out.println( "  returns " + res);
		}

		return res;
	}

}
