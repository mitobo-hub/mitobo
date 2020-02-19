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


import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;

/** Base class of length energies using forward differences.
 *  This is an abstract class, gradMagHeaviside still needs to be implemented.
 */

@ALDDerivedClass
@ALDParametrizedClass
public class MTBLengthEnergyFWNonPDE extends MTBLengthEnergyNonPDE {

	/**
	 * constructor
	 * 
	 * @param mu weight of the length term
	 */
	public MTBLengthEnergyFWNonPDE(double mu) {
		super( mu);
		this.name = new String( "Length energy (forward differences)");
	}

	/**
	 * constructor
	 */
	public MTBLengthEnergyFWNonPDE() {
		this( 1.0);
	}

	@Override
	public MTBGenericEnergyNonPDE init( MTBImage img, MTBLevelsetMembership phi) {	
		return MTBLengthEnergyFWNonPDE.createEnergy( phi, this.mu);
	}


	/**
	 * Create a proper length energy asked for by the level set function.
	 * If <code>sizeZ = 1</code> than 2D, otherwise 3D .
	 * If the number of phases in <code>phi</code> is 2 than two phase, otherwise multi phase.
	 *<br>
	 * In the future more types, e.g. various methods to approximate length
	 * may be available
	 *
	 * @param	mu weight of length term
	 * @param	phi Level set function associated with this energy object
	 * 
	 * @return	length energy
	 */
	public static MTBLengthEnergyNonPDE createEnergy(MTBLevelsetMembership phi, double mu) {

		MTBLengthEnergyNonPDE res;
		if ( phi.getNumPhases() > 2 ) {
			if ( phi.getSizeZ() != 1 ) {
				res = new MTBLengthEnergyFW3DMPNonPDE( mu);
			} else {
				res = new MTBLengthEnergyFW2DMPNonPDE( mu);
			}
		} else {
			if ( phi.getSizeZ() != 1 ) {
				res = new MTBLengthEnergyFW3D2PNonPDE( mu);
			} else {
				res = new MTBLengthEnergyFW2D2PNonPDE( mu);
			}
		}

		res.setScaleFactor( phi);
		return res;
	}
	@Override
	public double deltaE(int x, int y, int z, short newPhase,
			MTBLevelsetMembership phi) {
		System.err.println( "WARNING MTBLengthEnergyFWNonPDE::deltaE should never be called");
		return 0;
	}

	@Override
	public double E(MTBLevelsetMembership phi) {
		System.err.println( "WARNING MTBLengthEnergyFWNonPDE::E should never be called");
		return 0;
	}
    

	

}
