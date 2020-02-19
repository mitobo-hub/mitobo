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

package de.unihalle.informatik.MiToBo.segmentation.levelset.nonPDE;


import java.lang.Math;

import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;

/** Implements the (standard) length energy using forward differences
 *   for 3D images and a two phase levelset function.
 */

@ALDParametrizedClass
public class MTBLengthEnergyFW3D2PNonPDE extends MTBLengthEnergyFW3DNonPDE {
	private final double SQRT_2 = Math.sqrt(2.0);
	private final double SQRT_3 = Math.sqrt(3.0);

	/**
	 * constructor
	 * 
	 * @param mu weight of the length term
	 */
	public MTBLengthEnergyFW3D2PNonPDE(double mu) {
		super( mu);
		this.name = new String( "Length energy (forward differences, 3D, multi phase)");
	}

	/**
	 * constructor
	 * 
	 */
	public MTBLengthEnergyFW3D2PNonPDE() {
		this.name = new String( "Length energy (forward differences, 3D, multi phase)");
	}

	// TODO: this is probably NOT correct !!!
	/**
	 * calculate the gradient magnitude of the forward difference of the heaviside function
	 * corresponding to the level set for one voxel
     * 
	 * 
	 * @param a	heaviside functtion at (x,y,z)
	 * @param b heaviside functtion at (x+1,y,z)
	 * @param c heaviside functtion at (x,y+1,z)
	 * @param d heaviside functtion at (x,y,z+1)
	 * @return
	 */
	protected double gradMagHeaviside(double a, double b, double c, double d) {
        int diff1 = ( a == b ) ? 0 : 1;
        int diff2 = ( a == c ) ? 0 : 1;
        int diff3 = ( a == d ) ? 0 : 1;

        int sum =  diff1+diff2+diff3;
        if ( sum == 3 ) 
			return scaleFactor * SQRT_3;
        else if ( sum == 2 ) 
			return scaleFactor * SQRT_2;
	else
		return scaleFactor * sum;
	}

}
