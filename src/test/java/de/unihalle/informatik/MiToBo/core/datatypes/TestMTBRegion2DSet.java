/*
 * This file is part of Alida, a Java library for 
 * Advanced Library for Integrated Development of Data Analysis Applications.
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
 * Fore more information on Alida, visit
 *
 *    http://www.informatik.uni-halle.de/alida/
 *
 */

package de.unihalle.informatik.MiToBo.core.datatypes;

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

/**
 * JUnit test class for {@link MTBRegion2DSet}.
 * 
 * @author posch
 */
public class TestMTBRegion2DSet {
	private final boolean debug = true;
	
	private final int maxNumRegions = 20;
	private final int xSize = 500;
	private final int ySize = 500;
	private final int maxArea = 200;

	/**
	 * Fixture.
	 */
	@Before
	public void initTestClass() {
		// nothing to do here
	}
	
	/**
	 * Test if remove method.
	 */
	@Test
	public void testRemove() {
		Random rndGen = new Random();
				
		MTBRegion2DSet region2DSet = new MTBRegion2DSet();
		int numRegions = rndGen.nextInt(maxNumRegions)  + 1;
		if ( debug )
			System.out.println( "   " + numRegions + " regions with maximum area " + maxArea);

		for ( int r = 0 ; r < numRegions ; r++) {
			MTBRegion2D region2D = MTBRegion2D.createRandomRegion2D(xSize, ySize, maxArea, rndGen);
			region2DSet.add( region2D);
			if ( debug ) 
				System.out.println( "created region of size " + region2D.getArea());
		}

		LinkedList<MTBRegion2D> regionsToRemove = new LinkedList<MTBRegion2D>();
		
		for ( MTBRegion2D region : region2DSet) {
			if ( region.getArea() < 0.5*maxArea ) {
				regionsToRemove.add( region);
			}
		}
		
		for ( MTBRegion2D region : regionsToRemove) {
			region2DSet.remove(region);
			if ( debug) {
				System.out.println( "Removing region with id " + region.getID() +
						" of size " + region.getArea());
			}
		}
		
		if ( debug ) {
			System.out.println( "keeping " + region2DSet.size() + " regions");
		}

		assertTrue("Regionset contains " + region2DSet.size() + " regions instead of "
				+  (numRegions-regionsToRemove.size()), 
				region2DSet.size() == (numRegions-regionsToRemove.size()));
		
	}
	
}