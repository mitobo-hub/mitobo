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

/** Implements the (standard) length energy using forward differences for 3D images.
 *  This is an abstract class, gradMagHeaviside still needs to be implemented.
 */

@ALDParametrizedClass
public abstract class MTBLengthEnergyFW3DNonPDE extends MTBLengthEnergyFWNonPDE {

	/**
	 * constructor
	 * 
	 * @param mu weight of the length term
	 */
	public MTBLengthEnergyFW3DNonPDE(double mu) {
		super( mu);
		this.name = new String( "Length energy (forward differences, 3D, multi phase)");
	}

	/**
	 * constructor
	 */
	public MTBLengthEnergyFW3DNonPDE() {
		this.name = new String( "Length energy (forward differences, 3D, multi phase)");
	}

	@Override
	public double deltaE(int x, int y, int z, short newPhase, MTBLevelsetMembership phi) {
		int sizeX = phi.getSizeX();
		int sizeY = phi.getSizeY();
		int sizeZ = phi.getSizeZ();
	
		// code from Markus Glass (ChanVese3D); not thoroughly checked
		if ( (debug & FLAG_LENGTH) != 0 ) System.out.println( "deltaE @(" + x + "," + y + "," + z +")");
	
		// collect required neighbors (if available)
		double[] values = new double[13];
		double[] values2 = new double[13];
		
		short oldPhase = phi.getPhase(x, y, z);
		values[0] = oldPhase;
		values2[0] = newPhase;
		
		if(x < sizeX - 1) {
			values[1] = phi.getPhase(x+1, y, z);
			values2[1] = values[1];
		} else {
			values[1] = MTBLevelsetMembership.BG_PHASE;
			values2[1] = MTBLevelsetMembership.BG_PHASE;
		}
		
		if(y < sizeY -1) {
			values[2] = phi.getPhase(x, y+1, z);
			values2[2] = values[2];
		} else {
			values[2] = MTBLevelsetMembership.BG_PHASE;
			values2[2] = MTBLevelsetMembership.BG_PHASE;
		}
		
		if(z < sizeZ - 1) {
			values[3] = phi.getPhase(x, y, z+1);
			values2[3] = values[3];
		} else {
			values[3] = MTBLevelsetMembership.BG_PHASE;
			values2[3] = MTBLevelsetMembership.BG_PHASE;
		}
		
		if(x > 0) {
			values[4] = phi.getPhase(x-1, y, z);
			values2[4] = values[4];
			
			if(y < sizeY - 1) {
				values[5] = phi.getPhase(x-1, y+1, z);
				values2[5] = values[5];
			} else {
				values[5] = MTBLevelsetMembership.BG_PHASE;
				values2[5] = MTBLevelsetMembership.BG_PHASE;
			}
			
			if(z < sizeZ - 1) {
				values[6] = phi.getPhase(x-1, y, z+1);
				values2[6] = values[6];
			} else {
				values[6] = MTBLevelsetMembership.BG_PHASE;
				values2[6] = MTBLevelsetMembership.BG_PHASE;
			}
			
		} else {
			values[4] = values[0];
			values[5] = values[0];
			values[6] = values[0];
			values2[4] = values2[0];
			values2[5] = values2[0];
			values2[6] = values2[0];
		}
		
		if(y > 0) {
			values[7] = phi.getPhase(x, y-1, z);
			values2[7] = values[7];
			
			if(x < sizeX - 1) {
				values[8] = phi.getPhase(x+1, y-1, z);
				values2[8] = values[8];
			} else {
				values[8] = MTBLevelsetMembership.BG_PHASE;
				values2[8] = MTBLevelsetMembership.BG_PHASE;
			}
			
			if(z < sizeZ - 1) {
				values[9] = phi.getPhase(x, y-1, z+1);
				values2[9] = values[9];
			} else {
				values[9] = MTBLevelsetMembership.BG_PHASE;
				values2[9] = MTBLevelsetMembership.BG_PHASE;
			}
			
		} else {
			values[7] = values[0];
			values[8] = values[0];
			values[9] = values[0];
			values2[7] = values2[0];
			values2[8] = values2[0];
			values2[9] = values2[0];
			
		}
		
		if(z > 0) {
			values[10] = phi.getPhase(x, y, z-1);
			values2[10] = values[10];
			
			if(x < sizeX - 1) {
				values[11] = phi.getPhase(x+1, y, z-1);
				values2[11] = values[11];
			} else {
				values[11] = MTBLevelsetMembership.BG_PHASE;
				values2[11] = MTBLevelsetMembership.BG_PHASE;
			}
			
			if(y < sizeY - 1) {
				values[12] = phi.getPhase(x, y+1, z-1);
				values2[12] = values[12];
			} else {
				values[12] = MTBLevelsetMembership.BG_PHASE;
				values2[12] = MTBLevelsetMembership.BG_PHASE;
			}
			
		} else {
			values[10] = values[0];
			values[11] = values[0];
			values[12] = values[0];
			values2[10] = values2[0];
			values2[11] = values2[0];
			values2[12] = values2[0];
		}
	
		double before = 0;
		double after = 0;
		
		before += gradMagHeaviside(values[0], values[1], values[2], values[3]);
		before += gradMagHeaviside(values[4], values[0], values[5], values[6]);
		before += gradMagHeaviside(values[7], values[8], values[0], values[9]);
		before += gradMagHeaviside(values[10], values[11], values[12], values[0]);
	
		after += gradMagHeaviside(values2[0], values2[1], values2[2], values2[3]);
		after += gradMagHeaviside(values2[4], values2[0], values2[5], values2[6]);
		after += gradMagHeaviside(values2[7], values2[8], values2[0], values2[9]);
		after += gradMagHeaviside(values2[10], values2[11], values2[12], values2[0]);
		
		if ( (debug & FLAG_LENGTH) != 0 ) System.out.println( " ---> delta  " + (mu * (after - before)));
	
		return mu * (after - before);
	}

	/**
	 * calculate the gradient magnitude of the foreward difference of the heavyside function
	 * corresponding to the level set for one voxel
     * 
	 * 
	 * @param a	heaviside functtion at (x,y,z)
	 * @param b heaviside functtion at (x+1,y,z)
	 * @param c heaviside functtion at (x,y+1,z)
	 * @param d heaviside functtion at (x,y,z+1)
	 * 
	 * @return weight of the length term
	 */
	protected abstract double gradMagHeaviside(double a, double b, double c, double d);

	@Override
	public double E(MTBLevelsetMembership phi) {
		double sum = 0.0;

		for(int z = 0; z < phi.getSizeZ(); z++) {
			for(int y = 0; y < phi.getSizeY(); y++) {
				for(int x = 0; x < phi.getSizeX(); x++) {
					sum += E( x, y, z, phi);
				}
			}
		}
		return this.mu*sum;
	}
	
	/** Get energy term a voxel (x,y,z) for phi. 
	 * @param x
	 * @param y
	 * @param z
	 * @param phi
	 * 
	 * @return energy
	*/

	public double E( int x, int y, int z, MTBLevelsetMembership phi) {
		int sizeX = phi.getSizeX();
		int sizeY = phi.getSizeY();
		int sizeZ = phi.getSizeZ();

		short phaseCurrent = phi.getPhase( x,y); // old phase at x,y
		short phaseRight = ( (x+1) < sizeX) ? phi.getPhase( x+1,y,z) : MTBLevelsetMembership.BG_PHASE;
		short phaseBelow = ( (y+1) < sizeY) ? phi.getPhase( x,y+1,z) : MTBLevelsetMembership.BG_PHASE;
		short phaseNextslice = ( (z+1) < sizeZ) ? phi.getPhase( x,y, z+1) : MTBLevelsetMembership.BG_PHASE;

		return mu * gradMagHeaviside( phaseCurrent, phaseRight, phaseBelow, phaseNextslice);
    }

}
