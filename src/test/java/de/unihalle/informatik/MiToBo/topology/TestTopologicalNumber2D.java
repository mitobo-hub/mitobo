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

import static org.junit.Assert.*;

import java.util.Iterator;
import org.junit.Before;
import org.junit.Test;

import de.unihalle.informatik.MiToBo.topology.MTBTopologicalNumber.Point3D;

/**
 * JUnit test class for {@link MTBTopologicalNumber2D}.
 * 
 * @author posch
 */
public class TestTopologicalNumber2D {

	/**
	 * Fixture.
	 */
	@Before
	public void initTestClass() {
		// nothing to do here
	}

	@Test
	public void testNeighbors() {

		MTBTopologicalNumber2D topo = new MTBTopologicalNumber2DN4();
		// test neighborhood, -1 encode current pixel, 1 neighbor, 0 non neighborhood
		int[][][] neighborhoodCorrect = new int[][][]{
				{{0,0,0},
			      {0,0,0},
				  {0,0,0}},
				{{0,1,0},
				 {1,-1,1},
				 {0,1,0}},
				{{0,0,0},
				 {0,0,0},
				 {0,0,0}}
		};
		
		testNeightbors( topo, 4, neighborhoodCorrect);

		topo = new MTBTopologicalNumber2DN8();
		// test neighborhood, -1 encode current pixel, 1 neighbor, 0 non neighborhood
		neighborhoodCorrect = new int[][][]{
				{{0,0,0},
				      {0,0,0},
					  {0,0,0}},
					{{1,1,1},
					 {1,-1,1},
					 {1,1,1}},
					{{0,0,0},
					 {0,0,0},
					 {0,0,0}}
			};
		testNeightbors( topo,8, neighborhoodCorrect);
	}

	@Test
	public void testNeighborNeighbors() {

		MTBTopologicalNumber2D topo = new MTBTopologicalNumber2DN4();
		Iterator<Point3D>iter = topo.iterator();
		while ( iter.hasNext() ) {
			Point3D pt = iter.next();		
			testNeighborneighborsCoordinates( pt, topo, 1.0f);			
		}

		topo = new MTBTopologicalNumber2DN8();
		iter = topo.iterator();
		while ( iter.hasNext() ) {
			Point3D pt = iter.next();		
			testNeighborneighborsCoordinates( pt, topo, 1.5f);			
		}
	}


	/**
	 * @param topo
	 * @param size
	 * @param neighborhoodCorrect
	 */
	private void testNeightbors( MTBTopologicalNumber2D topo, int size, int[][][] neighborhoodCorrect) {
		assertTrue( (topo.getClass().getCanonicalName() + " sizeNeighborhood incorrect "), 
				size == topo.getSizeNeighborhood());

		int[][][] neighborhoodComputed = new int[3][3][3];
		for ( int k=0; k < 3 ; k++) 
			for ( int i = 0 ; i < 3 ; i++)
				for ( int j=0 ; j<3 ; j++ )
					neighborhoodComputed[k][i][j] = 0;

		neighborhoodComputed[1][1][1] = -1;
		Iterator<Point3D> iter = topo.iteratorOffsets();
		while ( iter.hasNext() ) {
			Point3D pt = iter.next();
			neighborhoodComputed[1][pt.getY()+1][pt.getX()+1] = 1;
		}

		assertArrayEquals( (topo.getClass().getName() + ": error (testing iteratorOffsets): "),
				neighborhoodComputed, neighborhoodCorrect);

		neighborhoodComputed = new int[3][3][3];

		neighborhoodComputed[1][1][1] = -1;
		iter = topo.iterator();
		while ( iter.hasNext() ) {
			Point3D pt = iter.next();
			neighborhoodComputed[1][pt.getY()][pt.getX()] = 1;
		}

			assertArrayEquals( (topo.getClass().getName() + ": error (testing iterator): "),  
					neighborhoodComputed, neighborhoodCorrect);
	}

	/**
	 * @param neighbor
	 * @param neighborneighborsCoordinates
	 * @param dist
	 */
	private void testNeighborneighborsCoordinates( Point3D neighbor, MTBTopologicalNumber2D topo, float dist) {
		Point3D neighborCentered = new Point3D( 0, neighbor.getY()+1, neighbor.getX()+1);

		int[][][] neighborhoodComputed = new int[5][5][5];

		for ( int k=0; k < 5 ; k++) 
			for ( int i = 0; i < 5 ; i++)
				for ( int j = 0 ; j < 5 ; j++ )
					neighborhoodComputed[k][i][j] = 0;
		neighborhoodComputed[neighborCentered.getZ()][neighborCentered.getY()][neighborCentered.getX()] = -1;

		for ( Point3D nn : topo.getCoordinatesNeighborNeighbors(neighbor.getZ(), neighbor.getY(), neighbor.getX())) {
			neighborhoodComputed[1+nn.getZ()][1+nn.getY()][1+nn.getX()] = 1;
		}

		int[][][] neighborhoodCorrect = new int[5][5][5];
		for ( int k=0; k < 5 ; k++) 
			for ( int i = 0; i < 5 ; i++)
				for ( int j = 0 ; j < 5 ; j++ )
					neighborhoodCorrect[k][i][j] = 0;
		neighborhoodCorrect[neighborCentered.getZ()][neighborCentered.getY()][neighborCentered.getX()] = -1;

		for ( int k=0; k < 5 ; k++) 
			for ( int i = 0; i < 5 ; i++) {
				for ( int j = 0 ; j < 5 ; j++ ) {
					Point3D pt = new Point3D(k, i, j);
					if ( pt.dist( neighborCentered) <= dist &&
							k >= 1 & k <= 3 && i >= 1 &&  i <=3 && j >=1 && j <=3 &&
							(k != 2 || i != 2 || j != 2 ) &&
							(k != neighborCentered.getZ() || i != neighborCentered.getY() || j != neighborCentered.getX() ))
						neighborhoodCorrect[k][i][j] = 1;
				}
			}

		if ( topo.dimension == 2 ) {
			assertArrayEquals( (topo.getClass().getSimpleName() + ": error (testing getCoordinatesNeighborNeighbors for neigbor " +
-                                       neighbor.getZ() + "," + neighbor.getY() + "," + neighbor.getX() + ")"),  
					neighborhoodComputed[2], neighborhoodCorrect[2]);

		} else {
		for ( int i = 0; i< 5 ; i++ ) 
			assertArrayEquals( (topo.getClass().getSimpleName() + ": error (testing getCoordinatesNeighborNeighbors for neigbor " +
-                                       neighbor.getZ() + "," + neighbor.getY() + "," + neighbor.getX() + ")"),  
					neighborhoodComputed, neighborhoodCorrect);
		}
	}

}