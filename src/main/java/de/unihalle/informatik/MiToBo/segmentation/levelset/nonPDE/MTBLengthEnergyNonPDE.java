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

import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;

/** An class for energies for a nonPDF level set approach
 * based on different length energies of the phases boundaries.
 * In principle this class should be abstract but may not in order to facilitate  generic
 * execution.
 * <p>
 * TODO: each create function returning a suitable energy instance has to call the
 * method setScaleFactor()!!
 */

@ALDDerivedClass
@ALDParametrizedClass
public class MTBLengthEnergyNonPDE extends MTBGenericEnergyNonPDE
{
	/** weight of the length term
	 */
	@ALDClassParameter(label="Weight of the length term")
	protected double mu = 1.0; 

	/** do normalization?
     * The dimension of the largest direction in the original image (thus level set function)
     * is scale to 1.
     * <p>
     * Still experimental
	 */
	protected boolean doNormalization = false;

	/** Scaling factor for normalization
     * <p>
     * Still experimental
	 */
	protected double scaleFactor = 1.0;

	/** Bit mask for length energies for debugging purposes.
     * The value has to be coordinated with other bit masks.
     */
	protected final int FLAG_LENGTH = 4; // flag for length term

	/**
	 * Constructor
	 * 
	 */
	public MTBLengthEnergyNonPDE() {
		this( 1.0);
	}

	/**
	 * Constructor
	 * 
	 * @param mu	weight of the length term
	 */
	public MTBLengthEnergyNonPDE(double mu) {
		this.name = new String( "Length energy");
		this.mu = mu;
	}

	@Override
	public  String 	toString() {
        return name + ": mu = " + mu + " scaleFactor = " + scaleFactor;
    }
	
	@Override
	public MTBGenericEnergyNonPDE init( MTBImage img, MTBLevelsetMembership phi) {	
		return MTBLengthEnergyFWNonPDE.createEnergy( phi, this.mu);
	}

    /** set the scale factor if normalization is requested.
     * 
     * @param   phi Levelset function
     */
	protected void setScaleFactor( MTBLevelsetMembership phi) {
		if ( doNormalization )  {
			int maxSize = Math.max( Math.max( phi.getSizeX(), phi.getSizeY()), phi.getSizeZ());
			scaleFactor = 1/(double)maxSize;
		} else {
			scaleFactor = 1.0;
		}
	}

	@Override
	public double deltaE(int x, int y, int z, short newPhase,
			MTBLevelsetMembership phi) {
		System.err.println( "WARNING MTBLengthEnergyNonPDE::deltaE should never be called");
		return 0;
	}

	@Override
	public double E(MTBLevelsetMembership phi) {
		System.err.println( "WARNING MTBLengthEnergyNonPDE::E should never be called");
		return 0;
	}
    
}
