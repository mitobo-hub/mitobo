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



/** Class to compute topological numbers for the 2D case.
 * This classes define the 4-neighborhood
 */

public class MTBTopologicalNumber2DN4 extends MTBTopologicalNumber2D {
	
	/** Construct a class for this neighborhood definitions
	 */
	public MTBTopologicalNumber2DN4() {
		dimension = 2;
		sizeNeighborhood = 4;
		// dist == 1 gives 4-neighborhood
		maxDist = 1.0f;
		initNeighbors();
	}

	@Override
	protected void computeN() {
		// visit each diagonal pixels in this neighborhood
		for ( int i = 4 ; i < 8 ; i ++ ) {
			if ( X[coordinatesNeighbors[i].getZ()][coordinatesNeighbors[i].getY()][coordinatesNeighbors[i].getX()] ) {
				// if this diagonal pixel is true try to find a 4-neighbor
				// that is also true - otherwise this pixel is not element of X
				boolean found = false;
				for ( int n = 0; n < coordinatesNeighborNeighbors[i].size() ; n++ ) {
					if ( X[(coordinatesNeighborNeighbors[i].get(n)).getZ()]
                          [(coordinatesNeighborNeighbors[i].get(n)).getY()] 
                          [(coordinatesNeighborNeighbors[i].get(n)).getX()] ) {

						found = true;
						break;
					}
				}

				if ( ! found ) 
					X[coordinatesNeighbors[i].getZ()][coordinatesNeighbors[i].getY()][coordinatesNeighbors[i].getX()] = false;
			}
		}
	}
	
	/** Test
	  */
	public static void main(String [] args) {
		MTBTopologicalNumber2DN4 t = new MTBTopologicalNumber2DN4();
		t.print();
	}
}
