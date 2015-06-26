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


/** Class to compute topological numbers for the 2D case.
 * This classes define the 8-neighborhood
 */

public class MTBTopologicalNumber2DN8 extends MTBTopologicalNumber2D {
	/** Construct a class for this neighborhood definition
	  */
	public MTBTopologicalNumber2DN8() {
		// dist == 1.5 gives 8-neighborhood
		initNeighbors( 1.5f);
	}

	@Override
	protected void computeN() {
		// nothing to be done here, as X == N_8_1
	}
	
	/** Test
     */
	public static void main(String [] args) {
		MTBTopologicalNumber2DN8 t = new MTBTopologicalNumber2DN8();
		t.print();
	}
}
