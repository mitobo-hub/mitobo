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

import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;


/** Implements the (standard) length energy using forward differences for 2D images.
 * This is an abstract class, gradMagHeaviside still needs to be implemented. 
 */

@ALDParametrizedClass
public abstract class MTBLengthEnergyFW2DNonPDE extends MTBLengthEnergyFWNonPDE {

	/**
	 * constructor
	 */
	public MTBLengthEnergyFW2DNonPDE() {
		this.name = new String( "Length energy (forward differences, 2D)");
	}

	/**
	 * constructor
	 * 
	 * @param mu
	 */
	public MTBLengthEnergyFW2DNonPDE(double mu) {
		super( mu);
		this.name = new String( "Length energy (forward differences, 2D)");
	}

	@Override
	public double deltaE(int x, int y, int z, short newPhase, MTBLevelsetMembership phi) {
		if ( (debug & FLAG_LENGTH) != 0 ) System.out.println( "deltaE @(" + x + "," + y + ")");

		int sizeX = phi.getSizeX();
		int sizeY = phi.getSizeY();
		double delta = 0.0;

		short oldPhase = phi.getPhase( x,y); // old phase at x,y
		short phaseCurrent; // for each pixel considered: its phase 
		short phaseRight; // for each pixel considered: phase of its right neighbor
		short phaseBelow; // for each pixel considered: phase of its below neighbor

		// consider current pixel (x,y)
		phaseRight = ( (x+1) < sizeX) ? phi.getPhase( x+1,y) : MTBLevelsetMembership.BG_PHASE;
		phaseBelow = ( (y+1) < sizeY) ? phi.getPhase( x,y+1) : MTBLevelsetMembership.BG_PHASE;

		delta -= gradMagHeaviside( oldPhase, phaseRight, phaseBelow);
		delta += gradMagHeaviside( newPhase, phaseRight, phaseBelow);

		if ( (debug & FLAG_LENGTH) != 0 ) 
			System.out.println( "   current, old: " + gradMagHeaviside( oldPhase, phaseRight, phaseBelow) +
                                    ", new: " + gradMagHeaviside( newPhase, phaseRight, phaseBelow));

		// now its above neighbor
		phaseCurrent = ( (y-1) >= 0 ) ? phi.getPhase( x,y-1) : MTBLevelsetMembership.BG_PHASE;
		phaseRight = ( (x+1) < sizeX && (y-1) >= 0 ) ? phi.getPhase( x+1,y-1) : MTBLevelsetMembership.BG_PHASE;

		delta -= gradMagHeaviside( phaseCurrent, phaseRight, oldPhase);
		delta += gradMagHeaviside( phaseCurrent, phaseRight, newPhase);

		if ( (debug & FLAG_LENGTH) != 0 ) 
			System.out.println( "   above, old: " + gradMagHeaviside( phaseCurrent, phaseRight, oldPhase) +
                                    ", new: " + gradMagHeaviside( phaseCurrent, phaseRight, newPhase));

		// finally its left neighbor
		phaseCurrent = ( (x-1) >= 0 ) ? phi.getPhase( x-1,y) : MTBLevelsetMembership.BG_PHASE;
		phaseBelow = ( (x-1) >= 0  && (y+1) < sizeY ) ? phi.getPhase( x-1,y+1) : MTBLevelsetMembership.BG_PHASE;

		delta -= gradMagHeaviside( phaseCurrent, oldPhase, phaseBelow);
		delta += gradMagHeaviside( phaseCurrent, newPhase, phaseBelow);

		if ( (debug & FLAG_LENGTH) != 0 ) 
			System.out.println( "   left, old: " + gradMagHeaviside( phaseCurrent, oldPhase, phaseBelow) +
                                    ", new: " + gradMagHeaviside( phaseCurrent, newPhase, phaseBelow));


		if ( (debug & FLAG_LENGTH) != 0 ) System.out.println( " ---> delta  " + delta);
		return mu * delta;
	}

    /**
     * calculate the gradient magnitude of the foreward difference of the heavyside function
     * corresponding to the level set for one pixel
     * 
     * 
     * @param a heaviside functtion at (x,y,z)
     * @param b heaviside functtion at (x+1,y,z)
     * @param c heaviside functtion at (x,y+1,z)
     * @return
     */
    // TODO: check whether correct for multiphase
    protected abstract double gradMagHeaviside(double a, double b, double c); 

    @Override
    public double E(MTBLevelsetMembership phi)
    {
		double sum = 0.0;
			
		// remind; we are in 2D
		for(int y = 0; y < phi.getSizeY(); y++) {
			for(int x = 0; x < phi.getSizeX(); x++) {
					sum += E( x, y, phi);
			}
		}
		return sum;
	}
	
	/** Get energy term a pixel (x,y) for <code>phi</code>. 2D only
	 * @param x
	 * @param y
	 * @param phi
	 * 
	 * @return energy
	*/

	public double E( int x, int y, MTBLevelsetMembership phi) {
		int sizeX = phi.getSizeX();
		int sizeY = phi.getSizeY();

		short phaseCurrent = phi.getPhase( x,y); // old phase at x,y
		short phaseRight = ( (x+1) < sizeX) ? phi.getPhase( x+1,y) : MTBLevelsetMembership.BG_PHASE;
		short phaseBelow = ( (y+1) < sizeY) ? phi.getPhase( x,y+1) : MTBLevelsetMembership.BG_PHASE;

		return mu * gradMagHeaviside( phaseCurrent, phaseRight, phaseBelow);
    }

}
