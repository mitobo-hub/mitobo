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

import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;

/** Implements the (standard) length energy using length approximation
 *  due to Kolmogorov/Boykov for two phases.
 *  Does NOT take invalid pixels into account
 * This is an abstract class, neighborhood coordinates and weights have to
 * be still defined.
 * For notation see Danek, Matula: Graph cuts and approximation pf the Eucleidean metric
 * on anisotropic grids, VISAPP 2010, 68-73.
 */

@ALDParametrizedClass
public abstract class MTBLengthEnergyKB2D2PNonPDE extends MTBLengthEnergyKBNonPDE {

	/** x coordinate of displacement vector defining neighbor hood
	 */
	int[] e_x;

	/** x coordinate of displacement vector defining neighbor hood
	 */
	int[] e_y;
	
	/** weight for neighbors
	 */
	double[] w;
	
	/**
	 * constructor
	 */
	public MTBLengthEnergyKB2D2PNonPDE() {
		this(0.0);
	}

	/**
	 * constructor
	 * 
	 * @param mu
	 */
	public MTBLengthEnergyKB2D2PNonPDE(double mu) {
		super( mu);
		this.name = new String( "Length energy (Kolmogorov/Boykov, 2D)");
	}

	@Override
	public double deltaE(int x, int y, int z, short newPhase, MTBLevelsetMembership phi) {
		if ( (debug & FLAG_LENGTH) != 0 ) System.out.println( "deltaE @(" + x + "," + y + ")");
		System.out.println( "deltaE: (" + x + "," + y + ")  phase = " + phi.getPhase(x, y));
		
		int sizeX = phi.getSizeX();
		int sizeY = phi.getSizeY();
		double delta = 0.0;

		//if (  x - e_x[k] >= 0 &&  x - e_x[k] < sizeX &&
			//	y - e_y[k] >= 0 && y - e_y[k] < sizeY ) {
			//System.out.println( "  " + phi.getPhase(sizeX, sizeY))
		//}


		for ( int k = 0 ; k < e_x.length ; k++ ) {
			short neighborphase;
	
			if (  x + e_x[k] >= 0 &&  x + e_x[k] < sizeX &&
					y + e_y[k] >= 0 && y + e_y[k] < sizeY ) {
				neighborphase =  phi.getPhase(x + e_x[k], y + e_y[k]);
			} else {
				neighborphase = MTBLevelsetMembership.BG_PHASE;
			}
		

			if ( phi.getPhase( x, y) == neighborphase ) {
				delta += w[k];
			} else {				
				delta -= w[k];
			}	
			System.out.println( "    e_x = " + e_x[k] + "  e_y = "+ e_y[k] + 
					"     phase = " + neighborphase + " --> " + delta);

		}
		
		for ( int k = 0 ; k < e_x.length ; k++ ) {
			short neighborphase;
			
			if (  x - e_x[k] >= 0 &&  x - e_x[k] < sizeX &&
					y - e_y[k] >= 0 && y - e_y[k] < sizeY ) {
				neighborphase =  phi.getPhase(x - e_x[k], y - e_y[k]);
			} else {
				neighborphase = MTBLevelsetMembership.BG_PHASE;
			}
			
			if ( phi.getPhase( x, y) == neighborphase ) {
				delta += w[k];
			} else {				
				delta -= w[k];
			}
			System.out.println( "    e_x = " + e_x[k] + "  e_y = "+ e_y[k] + 
					"     phase = " + neighborphase + " --> " + delta);

		}
		
		return mu * delta;
	}

    @Override
    public double E(MTBLevelsetMembership phi)
    {		
    	int sizeX = phi.getSizeX();
    	int sizeY = phi.getSizeY();

    	double sum = 0.0;
    	System.out.println( "XXX " + name + " " + e_x);

    	// remind; we are in 2D
    	for(int y = 0; y < phi.getSizeY(); y++) {
    		for(int x = 0; x < phi.getSizeX(); x++) {
    			if ( phi.getPhase(x, y) != MTBLevelsetMembership.BG_PHASE ) {
    				for ( int k = 0 ; k < e_x.length ; k++ ) {
    					short neighborphase;
    					int nx = x + e_x[k]; int ny = y + e_y[k];
    					if (  nx >= 0 &&  nx < sizeX &&
    							ny >= 0 && ny < sizeY ) {
    						neighborphase =  phi.getPhase( nx, ny);
    					} else {
    						neighborphase = MTBLevelsetMembership.BG_PHASE;
    					}

    					if ( neighborphase == MTBLevelsetMembership.BG_PHASE) {
							sum += w[k];
						} else {
							sum -= w[k];
						}		
					}
					
					for ( int k = 0 ; k < e_x.length ; k++ ) {
						short neighborphase;
    					int nx = x - e_x[k]; int ny = y - e_y[k];
    					if (  nx >= 0 &&  nx < sizeX &&
    							ny >= 0 && ny < sizeY ) {
    						neighborphase =  phi.getPhase( nx, ny);
    					} else {
    						neighborphase = MTBLevelsetMembership.BG_PHASE;
    					}

						if ( neighborphase == MTBLevelsetMembership.BG_PHASE) {
							sum += w[k];
						} else {
							sum -= w[k];
						}		
					}
				}
			}
		}
		return sum;
	}
	
}
