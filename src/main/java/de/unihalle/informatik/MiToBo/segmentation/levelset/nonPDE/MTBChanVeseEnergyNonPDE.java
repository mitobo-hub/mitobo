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
import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;

/**
 * A class to implement the Chan-Vese energy for nonPDE level sets.
 *
 * @author Stefan Posch
 *
 */

@ALDDerivedClass
@ALDParametrizedClass
public class MTBChanVeseEnergyNonPDE extends MTBGenericEnergyNonPDE
{
	
	private MTBCVFittingEnergyNonPDE fittingEnergy = null;
	private MTBLengthEnergyNonPDE    lengthEnergy = null;
	private MTBSizeEnergyNonPDE      sizeEnergy = null;
	
	/** Lambda for foreground 
	 */
	@ALDClassParameter(label="Lambda fg",dataIOOrder=1)
	private double lambdaFg;   

	/** Lambda for background 
	 */
	@ALDClassParameter(label="Lambda bg",dataIOOrder=2)
	private double lambdaBg;  
	
	/** weight of the length term
	 */
	@ALDClassParameter(label="mu", dataIOOrder=3)
	private double mu; 
 
	/** weight of the size term
	 */
	@ALDClassParameter(label="nu", dataIOOrder=4)
	private double nu; 
 
	/** Image to be segmented
	 */
	@ALDClassParameter(label="Input image",dataIOOrder=3)
	private MTBImage img;

	
	/**
	 * Construct an energy object realizing the Chan-Vese energy. 
	 * The lambda for the background is lambdaBg and for all for all object phases an identical lambdaFg. 
	 * Mu and nu for length and size term.
	 * 
	 * @param lambdaBg
	 * @param lambdaFg
	 * @param mu
	 * @param nu
	 */
	public MTBChanVeseEnergyNonPDE( double lambdaBg, double lambdaFg, double mu, double nu)
	{
		this.name = new String( "Chan-Vese energy");
		this.lambdaFg = lambdaFg;
		this.lambdaBg = lambdaBg;
		this.mu = mu;
		this.nu = nu;
		
		fittingEnergy = new MTBCVFittingEnergyNonPDE( lambdaBg, lambdaFg);
		lengthEnergy = new MTBLengthEnergyNonPDE( mu);
		sizeEnergy = new MTBSizeEnergyNonPDE( nu);
	}
	
	/**
	 * Construct an energy object realizing the Chan-Vese energy. 
	 * The level set function may be two or multi phase.
	 * The lambda for the background is lambdaBg and for all for all object phases an identical lambdaFg. 
	 * Mu and nu for length and size term.
	 * 
	 * @param img	Image to be segmented
	 * @param phi	Level set function to construct the energy object for
	 * @param lambdaBg
	 * @param lambdaFg
	 * @param mu
	 * @param nu
	 */
	public MTBChanVeseEnergyNonPDE( MTBImage img, MTBLevelsetMembership phi, double lambdaBg, double lambdaFg, double mu, double nu)
	{
		this( lambdaBg, lambdaFg, mu, nu);
		this.img = img;
		this.init( img, phi);
	}
	
	/**
	 * Construct an energy object for the level set function realizing the Chan-Vese energy. 
	 */
	public MTBChanVeseEnergyNonPDE()
	{
		this.name = new String( "Chan-Vese energy");
	}
	
	/**
	 * Initialize the energy object. 
	 * <p>
	 * NOTE: The image provided as argument to this
	 * method is only associated with the energy object, if not already set!!
	 * This rational behind this is to allow the energy to be supplied generically with
	 * the input image and while it is still possible to set an image
	 * deviating from this default (e.g. to apply the energy to the gradient image)
	 * 
	 * @param img	Image to be segmented
	 * @param phi	Level set function to construct the energy object for
	 */
	@Override
	public MTBGenericEnergyNonPDE init( MTBImage img, MTBLevelsetMembership phi) {
		if ( this.img == null ) {
			this.img = img;
		}

		this.fittingEnergy = new MTBCVFittingEnergyNonPDE( lambdaBg, lambdaFg);
		this.fittingEnergy = (MTBCVFittingEnergyNonPDE) this.fittingEnergy.init( this.img, phi);

		this.lengthEnergy = new MTBLengthEnergyNonPDE( mu);
		this.lengthEnergy = (MTBLengthEnergyNonPDE) this.lengthEnergy.init( this.img, phi);

		this.sizeEnergy = new MTBSizeEnergyNonPDE( nu);
		this.sizeEnergy = (MTBSizeEnergyNonPDE) this.sizeEnergy.init( this.img, phi);

		return this;
	}

    // TODO: maybe we just should call deltaE with newPhase given to avoid redundancy,
    //        or leave it for efficiency ??
	@Override
	public double deltaE(int x , int y , int z , MTBLevelsetMembership phi) {
		return fittingEnergy.deltaE( x, y, z, phi) +
			    lengthEnergy.deltaE( x, y, z, phi) +
			    sizeEnergy.deltaE( x, y, z, phi);
	}
	
	@Override
	public double deltaE(int x , int y , int z , short newPhase, MTBLevelsetMembership phi)
	{
		return fittingEnergy.deltaE( x, y, z, newPhase, phi) +
				 lengthEnergy.deltaE( x, y, z, newPhase, phi) +
				 sizeEnergy.deltaE( x, y, z, newPhase, phi);
	}
	
	@Override
	public double E(MTBLevelsetMembership phi) {
		return fittingEnergy.E( phi) +
				 lengthEnergy.E( phi) +
				 sizeEnergy.E( phi);
	}
	
	@Override
	public void updateParams(int x, int y, int z, MTBLevelsetMembership phi) {
            fittingEnergy.updateParams(x, y, z, phi);
            lengthEnergy.updateParams(x, y, z, phi);
            sizeEnergy.updateParams(x, y, z, phi);
	}

	@Override
	public void updateParams(int x, int y, int z, short newPhase, MTBLevelsetMembership phi) {
        fittingEnergy.updateParams(x, y, z, newPhase, phi);
        lengthEnergy.updateParams(x, y, z, newPhase, phi);
        sizeEnergy.updateParams(x, y, z, newPhase, phi);
	}

	@Override
	public void estimateParams(MTBLevelsetMembership phi) {
		fittingEnergy.estimateParams(phi);
		lengthEnergy.estimateParams(phi);
		sizeEnergy.estimateParams(phi);
	}
	
	@Override
	public void setDebug( int debug) {
		this.debug = debug;
		if ( fittingEnergy != null )fittingEnergy.setDebug( debug);
		if ( lengthEnergy != null ) lengthEnergy.setDebug( debug);
		if ( sizeEnergy != null ) sizeEnergy.setDebug( debug);
	}

	@Override
	public  String 	toString() {
        return name + " composed of\n" +
			   (fittingEnergy != null ? (fittingEnergy.toString() + "\n") : "" ) +
               (lengthEnergy != null  ? (lengthEnergy.toString() + "\n") : "" ) +
               (sizeEnergy != null ? sizeEnergy.toString() : "" );

    }

	@Override
	public void print( MTBLevelsetMembership phi, PrintStream out, String indent) {
		out.println( "Chan-Vese energy");
			
		String newIndent = getNewIndent( indent);
        fittingEnergy.print( phi, out, newIndent);
        lengthEnergy.print( phi, out, newIndent);
        sizeEnergy.print( phi, out, newIndent);
			
	}

}
