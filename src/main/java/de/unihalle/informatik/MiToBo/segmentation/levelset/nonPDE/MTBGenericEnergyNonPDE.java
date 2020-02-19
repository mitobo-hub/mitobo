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

import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;

import java.io.PrintStream;

/** The base class for energies intended for a nonPDE level set approach.
 * When extending this base class it is necessary to override at least the
 * constructor (defining the name of the energy) as well as
 * the abstract methods <code>E</code> and <code>toString</code>.
 * Likewise the abstract version of the 
 * method <code>deltaE</code> 
 * which explicitly refers to the new phase the pixel should change to has to be implemented.
 * The second version of <code>deltaE</code>
 * makes sense in a strict way only for two phase approaches and an appropriate
 * default implementation is supplied.
 * <p>
 * In case the derived energy class relies on parameters depending on the
 * current state of the level set function associated with the energy,
 * then also the version of <code>updateParams</code>  has to be implemented
 * which explicitly refers to the new phase the pixel should change to.
 * The second version of <code>updateParams</code> has an appropriate default implementation.
 * Each implementation of  <code>updateParams</code> is assume to change/update the parameters of the
 * energy object to reflect an intended change of the level set function.
 * If the energy relies on parameters it is also highly recommended to implement the method
 * <code>estimateParams</code>. This method is expected to set the parameters to the 
 * current state of the level set function.
 *<p>
 * The rational behind this requirements is as follows.
 * In almost all cases an instance of an extending class is associated
 * with an instance of <code>MTBLevelsetMembership</code>.
 * As these two separate objects (of class <code>MTBGenericEnergyNonPDE</code> and <code>MTBLevelsetMembership</code>)
 * interact some caution has to be taken into account.
 * This is the case if the energy object relies on parameters
 * depending on the current state of the level set function associated with the energy.
 * Examples for such parameters are the mean intensities of phases/regions
 * as used in the fitting term of the Chan-Vese-Energy.
 * In this case changing of the level set function and setting of the parameters of the energy object
 * need to be synchronized.
 * The concept used in the solver {@link LevelsetSolveNonPDE}
 * is as follows: First the parameters of the energy object are updated calling <code>updateParams</code> where the
 * the level set function supplied as argument is still in the old state
 * and the anticipated change of the level set function is given via arguments, too.
 * Subsequently the energy object may rely on the fact that this change of the level set function
 * is conducted immediately to again have both energy function and 
 * level set function in compatible states.
 * <p>
 * In order for an extending energy class to be used as a parameter of
 * an operator intended to be used for generic execution, 
 * e.g. {@link de.unihalle.informatik.MiToBo.segmentation.levelset.nonPDE.ui.LevelsetSegmentationNonPDE}
 * the following has to be taken into account.
 * As in this case the energy object is initialized using the standard constructor 
 * the implementation may not rely on initialization of any member variables
 * conducted in other constructors. Particularly initialization depending on the 
 * image of initial level set function are not feasible within a constructor.
 * However, the energy object may rely on its <code>init</code> method being called
 * prior to actual use of the energy. This method assumes that all class parameters are properly set.
 * <p>
 * If the neighborhood of pixels is used to define the energy then
 * it is recommended to implement a 2D and 3D version of the energy  * in separate classes.
 * This is the case e.g. if derivatives are used. Examples are length energies
 * {@link MTBLengthEnergyNonPDE} and {@link CurvatureEnergy}.
 */

@ALDParametrizedClass
public abstract class MTBGenericEnergyNonPDE {

	/** Name of the energy.
	 */
	protected String name= null; // name of the energy

	/** Bit mask for debug output on <code>System.out</code>.
	 */
	protected int debug; 

	/**
	 * Initialization routine which is called once before the energy is actually used.
	 * <p>
	 * This method assumes, that all class parameters of the energy are already set
	 * and takes care of initializations which depend on the image to be segmented
	 * and/or the initial level set function supplied as argument.
	 * It may also be the case that depending on the image and/or the initial level set
	 * function a new energy object is created, e.g. if a specialized energy according to 
	 * dimensionality of the image of the type of the level set function (multi phase)
	 * is required.
	 * <p>
	 * If no initialization is required the routine should just return the unmodified energy object.
	 * 
	 * @param img  the image to be segmented
	 * @param phi  the level set function associated with this energy object in its initial state
	 *
	 * @return	The modified or a new energy if initialization was successful, otherwise null.
	 */
	public MTBGenericEnergyNonPDE init( MTBImage img, MTBLevelsetMembership phi) {
		return this;
	}

	/**
	 * Calculate differences of energy if the pixel/voxel (x,y,z) is changed
	 * from its current phase in the level set function <code>phi</code> supplied
	 * from current phase to <code>newPhase</code>.
	 * 
	 * @param x		
	 * @param y	
	 * @param z	
	 * @param newPhase		new phase to change the pixel/voxel to
	 * @param phi  level set function assumed to be associated with this energy object
	 * 
	 * @return		difference of energy
	 */
	public abstract double deltaE(int x, int y, int z, short newPhase, MTBLevelsetMembership phi);

	/**
	 * Calculate differences of energy if the pixel/voxel (x,y,z) changes its phase in
	 * the levelest function <code>phi</code> supplied as argument and assumed to be
	 * associated with this energy object.
	 * <p>
	 * This version makes sense only for two phase level set function. In the
	 * multi phase case any object phase is changed to the background, while a
	 * background pixel/voxel is changed to the first object phase.
	 *
	 * @param x
	 * @param y
	 * @param z
	 * @param phi  level set function assumed to be associated with this energy object
	 * 
	 * @return		difference of energy
	 */
	public double deltaE(int x, int y, int z, MTBLevelsetMembership phi) {
		if ( phi.getPhase( x, y, z) == MTBLevelsetMembership.BG_PHASE ) {
			return deltaE( x, y, z, (short)(MTBLevelsetMembership.BG_PHASE+1), phi);
		} else {
			return deltaE( x, y, z, MTBLevelsetMembership.BG_PHASE, phi);
		}
	}

	/**
	 * Return the complete energy for the level set function <code>phi</code>.
	 * <br>
	 *  Note: this may be an expensive operation.
	 *
	 * @param phi	level set function assumed to be associated with this energy object
	 * 
	 * @return		 energy
	 */
	public abstract double E(MTBLevelsetMembership phi);

	/** Update parameters (if any) of the energy object which depend on 
	 * the associated  level set function.
	 * <p>
	 *  This method is called to update the parameters if is is intended to change the
	 *  phase of a pixel/voxel to <code>newPhase</code>.
	 *  <p>
	 *  Note: The level set function <code> phi</code> is still in the old state and is
	 *  assumed to be to updated subsequently.
	 *
	 * @param x
	 * @param y
	 * @param z
	 * @param newPhase		new phase to change pixel/voxel to
	 * @param phi            assumed to be associated with this energy object
	 */
	public void updateParams(int x, int y, int z, short newPhase, MTBLevelsetMembership phi) {
	};

	/** Update parameters (if any) of the energy object which depend on 
	 * the associated level set function.
	 * <p>
	 *  This method is called to update the parameters if is is intended to change a pixel/voxel to the other state,
	 *  assuming a two phase level set function.
	 *  For a multi phase level set function flipping of phases is realized
	 *  in {@link MTBGenericEnergyNonPDE#deltaE}.
	 * <p>
	 *  Note: The level set function <code> phi</code> is still in the old state and is
	 *  assumed to be to updated subsequently.
	 *
	 * @param x
	 * @param y
	 * @param z
	 * @param phi assumed to be associated with this energy object
	 */
	public void updateParams(int x, int y, int z, MTBLevelsetMembership phi)
	{
		if ( phi.getPhase( x, y, z) == MTBLevelsetMembership.BG_PHASE ) {
			updateParams( x, y, z, (short)(MTBLevelsetMembership.BG_PHASE+1), phi);
		} else {
			updateParams( x, y, z, MTBLevelsetMembership.BG_PHASE, phi);
		}
	}

	/**
	 * Estimate internal parameters (if any) for the level set function <code>phi</code>
	 * assumed to be associated with this energy object.
	 *
	 * @param phi
	 */
	protected void estimateParams(MTBLevelsetMembership phi) {
	}

	/**
	 * Print this energy object - including the value of the energy for 
	 * <code>phi</code> assumed to be associated with this energy object. 
	 * Each line output is prefixed with the string <code>indent</code>.
	 * 
	 *  <b>Note:</b> this may be an expensive operation.
	 *
	 * @param phi	level set function assumed to be associated with this energy object
	 * @param out	stream to print to
	 * @param indent	indentation string
	 */
	public void print( MTBLevelsetMembership phi, PrintStream out, String indent) {
		out.println( indent + name + " energy = " + E( phi));
	}

	/**
	 * Return an ascii representation identifying the energy and internal parameters,
	 * but not the state.
	 */
	public abstract String toString();

	/**
	 * Set debug bit mask
	 *
	 * @param debug	bit mask
	 */
	public void setDebug( int debug) {
		this.debug = debug;
	}

	/**
	 * Return a new indentation string. If old indent was empty, just leave is,
	 * otherwise add four spaces.
	 *
	 * @param indent	Indentation asked for
	 */
	protected String getNewIndent( String indent) {
		return indent == null  ? new String ("") : indent + "    ";
	}
}
