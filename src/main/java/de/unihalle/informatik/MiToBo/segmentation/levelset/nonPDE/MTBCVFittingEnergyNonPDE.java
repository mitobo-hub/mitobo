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


import java.io.PrintStream;

import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.Alida.annotations.ALDClassParameter;

/**
 * A class to implement the fitting term of the Chan-Vese energy for
 * level sets using a non PDE approach to optimization.
 * The level set function may be two or multi phase.
 *
 * @author Stefan Posch, based on code by Markus Glass
 */

@ALDParametrizedClass
@ALDDerivedClass
public class MTBCVFittingEnergyNonPDE extends MTBGenericEnergyNonPDE
{
	/** Lambda for foreground 
	 */
	@ALDClassParameter(label="Lambda fg",dataIOOrder=1)
	private double lambdaFg;   

	/** Lambda for background 
	 */
	@ALDClassParameter(label="Lambda bg",dataIOOrder=2)
	private double lambdaBg;   

	/** Image to be segmented
	 */
	@ALDClassParameter(label="Input image",dataIOOrder=3)
	private MTBImage img;

	/** number of phases including background
	 */
	private int numPhases = -1;
	
	 /** An array of lambda values to be set in this energy object during initialization,
	  * i.e. the <code>init()</code> method. This is an alternative to supply lambdas
	  * besides setting the members <code>lambdaFg</code> and <code>lambdaBg</code>:
	  * <b>
	 *  The first value (with index 0) is taken as the lambda for the background phase, the subsequent
	 *  values for the foreground phases. If the array is longer as the number of phases superfluous
	 *  values are ignored. If it is shorter then the number of phases <code>lambdaFg</code> and/or
	 *  <code>lambdaBg</code> are used for the foreground resp. background phase.
	 *  <br>
	 *  This array may be <code>null</code>, which is handled equivalent to a size of zero.
	 */
	private double lambdaArray[];   

	/** array to hold lambdas of phases, starting with MTBLevelsetMembership.BG_PHASE 
	 */
	private double lambdaForOptimization[];  
	
	/** array to hold means of phases, starting with MTBLevelsetMembership.BG_PHASE 
	 */
	private double c[];  

	/**
	 * Construct an energy object for the level set function  <code>phi</code> realizing the fitting term of the Chan-Vese energy.
	 */
	public MTBCVFittingEnergyNonPDE() {
		this( 1.0, 1.0);
	}

	/**
	 * Construct an energy object realizing the fitting term of the Chan-Vese energy.
     * The weight for the background is lambdaBg and for all for all object phases an identically lambdaFg. 
	 * 
	 * @param lambdaBg	Weight for background phase
	 * @param lambdaFg	Weight for foreground phase
	 */
	public MTBCVFittingEnergyNonPDE( double lambdaBg, double lambdaFg) {
		this( lambdaBg, lambdaFg, null);		
	}
	
	/**
	 * Construct an energy object realizing the fitting term of the Chan-Vese energy.
     * The weight for the background is lambdaBg and for all for all object phases an identically lambdaFg. 
	 * 
	 * @param lambdaBg	Weight for background phase
	 * @param lambdaFg	Weight for foreground phase
	 */
	public MTBCVFittingEnergyNonPDE( double lambdaBg, double lambdaFg, double lambdaArray[]) {
		this.name = new String( "MTBCVFittingEnergyNonPDE");

        this.lambdaFg = lambdaFg;		
        this.lambdaBg = lambdaBg;		
        this.lambdaArray = lambdaArray;   
	}
	
	/**
	 * Construct an energy object with the initial level set function <code>phi</code>
	 * realizing the fitting term of the Chan-Vese energy to segment <code>img</code>.
     * The weight for the background is lambdaBg and for all for all object phases an identically lambdaFg. 
	 * 
	 * @param img	Image to be segmented
	 * @param phi	Level set function to construct the energy object for
	 * @param lambdaBg	Weight for background phase
	 * @param lambdaFg	Weight for foreground phase
	 */
	public MTBCVFittingEnergyNonPDE( MTBImage img, MTBLevelsetMembership phi, double lambdaBg, double lambdaFg) {
		this( lambdaBg, lambdaFg);
		this.init( img, phi);
	}
	
	/**
	 * Initialize the energy object. 
	 * <p>
	 * NOTE: The image provided as argument to this
	 * method is only associated with the energy object, if not already set!!
	 * This rational behind this is to allow the energy to be supplied generically with
	 * the input image and while it is still possible to set an image
	 * deviating from this default.
	 * 
	 * @param img	Image to be segmented
	 * @param phi	Level set function to construct the energy object for
	 */
	@Override
	public MTBGenericEnergyNonPDE init( MTBImage img, MTBLevelsetMembership phi) {
		this.img = img;
	
		this.numPhases = phi.getNumPhases();

        this.lambdaForOptimization = new double[this.numPhases+1];
        if ( lambdaArray != null && lambdaArray.length > 0)
    		this.lambdaForOptimization[MTBLevelsetMembership.BG_PHASE] = this.lambdaArray[0];
        else
        	this.lambdaForOptimization[MTBLevelsetMembership.BG_PHASE] = this.lambdaBg;
        
        int idx = 1; // used to index into lambdaArray
        for ( short p = (MTBLevelsetMembership.BG_PHASE+1) ; p <= numPhases ; p++ ) {
            if ( lambdaArray != null && lambdaArray.length > idx)
            	this.lambdaForOptimization[p] = lambdaArray[idx];
            else
            	this.lambdaForOptimization[p] = lambdaFg;
            idx++;
        }

        this.c = new double[this.numPhases+1];

		this.estimateParams(phi);
		
		return this;
	}

    // TODO: maybe we just shod call deltaE with newPhase given to avoid redundance,
    //        or leave it for efficiency ??
	
	@Override
	public double deltaE(int x , int y , int z , MTBLevelsetMembership phi)
	{
		short BG_PHASE = MTBLevelsetMembership.BG_PHASE; // abbreviation
		double picVal = this.img.getValueDouble(x, y, z);
		double in = (picVal - this.c[BG_PHASE]);
		double out = (picVal - this.c[BG_PHASE+1]);
		
		double m = phi.getPixelInside();
		double n = phi.getPixelOutside();
		
		if( phi.getPhase(x, y, z) > BG_PHASE) 
			// change from object phase to background
			return( lambdaForOptimization[BG_PHASE] * (out * out) * (n / (n + 1)) - lambdaForOptimization[BG_PHASE+1] * (in * in) * (m / (m - 1)));
		else 
			// change from background  to object phase 
			return(  lambdaForOptimization[BG_PHASE+1] * (in * in) * (m / (m + 1)) - lambdaForOptimization[BG_PHASE] * (out * out) * (n / (n - 1)));
	}
	
	@Override
	public double deltaE(int x , int y , int z , short newPhase, MTBLevelsetMembership phi)
	{
		short oldPhase = phi.getPhase( x, y, z);

		double picVal = this.img.getValueDouble(x, y, z);

		double diffNew = (picVal - this.c[newPhase]);
		double diffOld = (picVal - this.c[oldPhase]);

		double lambdaNew = lambdaForOptimization[ newPhase];
		double lambdaOld = lambdaForOptimization[ oldPhase];
		
            double sizeNew =  phi.getSizePhase( newPhase);
            double sizeOld =  phi.getSizePhase( oldPhase);

		// DEBUGSystem.out.println( "MTBCVFittingEnergyNonPDE.deltaE " + oldPhase + "->" + newPhase + "@(" + x + "," + y + "," + z + ")" +
                   //" diffOld: " + diffOld + " diffNew: " + diffNew +
                   //" sizeOld: " + sizeOld + " sizeNew: " + sizeNew +
                   //" lambdaOld: " + lambdaOld + " lambdaNew: " + lambdaNew);

		if ( sizeOld > 1 ) 
			return lambdaNew * (diffNew * diffNew) * (sizeNew / (sizeNew + 1)) - 
			   	   lambdaOld * (diffOld * diffOld) * (sizeOld / (sizeOld - 1)) ;
		else
			return lambdaNew * (diffNew * diffNew) * (sizeNew / (sizeNew + 1)) - 
				   lambdaOld * (diffOld * diffOld);
	}
	
	@Override
	public double E(MTBLevelsetMembership phi)
	{ double sum = 0.0;
		
		for(int z = 0; z < phi.getSizeZ(); z++) {
			for(int y = 0; y < phi.getSizeY(); y++) {
				for(int x = 0; x < phi.getSizeX(); x++) {
					short phase = phi.getPhase(x, y, z);
					if ( phase != MTBLevelsetMembership.INVALID_PHASE ) {
					    //double in = this.lambda[1] * (img.getValueDouble(x, y, z, 0, 0) - this.c[1]);
					    //double out = this.lambda[2] * (img.getValueDouble(x, y, z, 0, 0) - this.c[2]);
						//sum += in * in * heaviside(phi.getPhase(x, y, z)) + out * out * (1 - heaviside(phi.getPhase(x, y, z)));
						double picval = img.getValueDouble(x, y, z);
						sum += lambdaForOptimization[phase] * (picval - c[phase]) * (picval - c[phase]) ;
					}
				}
			}
		}
		
		return sum;	
	}
	
	@Override
	public void updateParams(int x, int y, int z, MTBLevelsetMembership phi) {
		short BG_PHASE = MTBLevelsetMembership.BG_PHASE; // abbreviation
		
		short currentVal = phi.getPhase(x, y, z);
		double picVal = this.img.getValueDouble(x, y, z, 0, 0);
		double m = phi.getPixelInside();
		double n = phi.getPixelOutside();
		
		if(currentVal > BG_PHASE) {	// change from object phase to background 
			this.c[BG_PHASE+1] = this.c[BG_PHASE+1] + ((this.c[BG_PHASE+1] - picVal) / (m - 1));	
			this.c[BG_PHASE] = this.c[BG_PHASE] - ((this.c[BG_PHASE] - picVal) / (n + 1));
		} else {       // change from background to object phase
			this.c[BG_PHASE] = this.c[BG_PHASE] + ((this.c[BG_PHASE] - picVal) / (n - 1));
			this.c[BG_PHASE+1] = this.c[BG_PHASE+1] + ((this.c[BG_PHASE+1] - picVal) / (m + 1));
		}
	
	}

	@Override
	public void updateParams(int x, int y, int z, short newPhase, MTBLevelsetMembership phi)
	{
            short oldPhase = phi.getPhase( x, y, z);

            double picVal = this.img.getValueDouble(x, y, z);

            double diffOld = (this.c[oldPhase] - picVal);
            double diffNew = (this.c[newPhase] - picVal);

            int sizeOld =  phi.getSizePhase( oldPhase);
            int sizeNew =  phi.getSizePhase( newPhase);

            c[oldPhase] = c[oldPhase] + diffOld/(sizeOld-1);
            c[newPhase] = c[newPhase] - diffNew/(sizeNew+1);
	}

	@Override
	protected void estimateParams(MTBLevelsetMembership phi)
	{

        for ( short p = 1 ; p <= numPhases ; p++ )
			this.c[p] = 0.0;

		for(int z = 0; z < phi.getSizeZ(); z++) {
			for(int y = 0; y < phi.getSizeY(); y++) {
				for(int x = 0; x < phi.getSizeX(); x++) {
					// this also looks at invalid pixel. but does not matter if we do not use c[0]
					this.c[ phi.getPhase( x, y, z )] += this.img.getValueDouble(x, y, z, 0, 0);
				}
			}
		}
		
        for ( short p = 1 ; p <= numPhases ; p++ )
		this.c[p] /= phi.getSizePhase(p);
	}
	
	/**
	 * @return the lambdaArray
	 */
	public double[] getLambdaArray() {
		return lambdaArray;
	}

	/**
	 * @param lambdaArray the lambdaArray to set
	 */
	public void setLambdaArray(double[] lambdaArray) {
		this.lambdaArray = lambdaArray;
	}

	@Override
	public  String 	toString() {
        String str = new String( name + ": " + numPhases + " phases\n    Lambdas: ");
        for ( short p = 1 ; p <= numPhases ; p++ )
            str = str.concat( lambdaForOptimization[p] + "\t");
        return str;
    }

	@Override
	public void print( MTBLevelsetMembership phi, PrintStream out, String indent) {
		out.println( indent + name + " energy = " + E( phi));

		String newIndent = getNewIndent( indent);
		out.print( newIndent + "Lambdas: ");
        for ( short p = 1 ; p <= numPhases ; p++ )
            out.print( indent + lambdaForOptimization[p] + "\t");
		out.println();

        out.println( newIndent + "c:");
        for ( short p = 1 ; p <= numPhases ; p++ )
            out.print( indent + c[p] + "\t");
		out.println();
	}

}
