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


import java.lang.Math;

import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;

/** Implements the (standard) length energy using forward differences
 *   for 2D images and a multi phase leve lset function.
 * Length is approximated using forward differences of the heaviside function
 */


@ALDParametrizedClass
public class MTBLengthEnergyFW2DMPNonPDE extends MTBLengthEnergyFW2DNonPDE {
	private final double SQRT_2 = Math.sqrt(2.0);

	/**
	 * constructor
	 * 
	 * @param mu weight of the length term
	 */
	public MTBLengthEnergyFW2DMPNonPDE(double mu) {
		super( mu);
		this.name = new String( "Length energy (forward differences, 2D, multi phase)");
	}

	/**
	 * constructor
	 */
	public MTBLengthEnergyFW2DMPNonPDE() {
		this.name = new String( "Length energy (forward differences, 2D, multi phase)");
	}

	/**
	 * calculate the gradient magnitude of the foreward difference of the heavyside function
	 * corresponding to the level set for one pixel
     * 
	 * 
	 * @param a	heaviside functtion at (x,y,z)
	 * @param b heaviside functtion at (x+1,y,z)
	 * @param c heaviside functtion at (x,y+1,z)
	 * @return gradient magnitude
	 * 
	 */
	// TODO: check whether correct for multiphase
	protected double gradMagHeaviside(double a, double b, double c) {
        int diff1 = ( a == b ) ? 0 : 1;
        int diff2 = ( a == c ) ? 0 : 1;

        int sum =  diff1+diff2;
        if ( sum == 2 ) 
			return scaleFactor * SQRT_2;
		else
			return scaleFactor * sum;
	}

}
