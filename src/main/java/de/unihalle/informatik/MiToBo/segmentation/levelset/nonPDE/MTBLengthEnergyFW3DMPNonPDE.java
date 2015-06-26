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

package de.unihalle.informatik.MiToBo.segmentation.levelset.nonPDE;

import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;

/** Implements the (standard) length energy using forward differences
 *   for 3D images and a multi phase level set function.
 */

@ALDParametrizedClass
@ALDDerivedClass
public class MTBLengthEnergyFW3DMPNonPDE extends MTBLengthEnergyFW3DNonPDE {

	/**
	 * constructor
	 * 
	 * @param mu weight terhm of the length energy
	 */
	public MTBLengthEnergyFW3DMPNonPDE(double mu) {
		super( mu);
		this.name = new String( "Length energy (forward differences, 3D, multi phase)");
	}
	
	/**
	 * constructor
	 */
	public MTBLengthEnergyFW3DMPNonPDE() {
		this.name = new String( "Length energy (forward differences, 3D, multi phase)");
	}

	/**
	 * Calculate the gradient magnitude of the foreard difference of the heavyside function
	 * corresponding to the level set for one voxel
     * 
	 * 
	 * @param a	heaviside functtion at (x,y,z)
	 * @param b heaviside functtion at (x+1,y,z)
	 * @param c heaviside functtion at (x,y+1,z)
	 * @param d heaviside functtion at (x,y,z+1)
	 * 
	 * @return gradient magnitude
	 */
	protected double gradMagHeaviside(double a, double b, double c, double d) {
		System.err.println( "MTBLengthEnergyFW3DMPNonPDE: gradMagHeaviside not yet implemented");
		System.exit( 1);
		return 0.0;
	}

}
