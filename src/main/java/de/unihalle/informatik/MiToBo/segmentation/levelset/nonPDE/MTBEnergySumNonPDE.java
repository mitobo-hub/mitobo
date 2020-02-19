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


/**
 * A generic class to implement sums of energies for level sets within
 * a nonPDE optimization approach.
 *
 * @author Stefan Posch
 */


import java.io.PrintStream;
import java.util.Vector;

import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;

@ALDDerivedClass
@ALDParametrizedClass
public class MTBEnergySumNonPDE extends MTBGenericEnergyNonPDE
{
	@ALDClassParameter(label="Energies")
	private Vector<MTBGenericEnergyNonPDE>	energies;

	/**
	 * Construct an energy object for a level set function realizing the sum of energies
	 * in the vector <code>energies</code>.
	 * The name of the energy is as supplied as parameter-
	 * 
	 * @param name The name of this compound energy
	 * @param energies A vector comprising the energies to be summed in this energy
	 * 
	 */
	public MTBEnergySumNonPDE(String name, Vector<MTBGenericEnergyNonPDE> energies) {
		this.energies = energies;
		this.name = name;
	}

	/**
	 * Construct an energy object for a level set function realizing the sum of energies.
	 * This constructor is mainly intended for use within generic operator execution.
	 * The name should be set properly after instantiation.
	 * 
	 */
	public MTBEnergySumNonPDE() {
		name = "MTBEnergySumNonPDE";
	}
	
	@Override
	public MTBGenericEnergyNonPDE init( MTBImage img, MTBLevelsetMembership phi) {
		for ( int i =0 ; i < energies.size() ; i++ ) {
			energies.setElementAt( energies.elementAt(i).init( img, phi), i);
		}
		return this;
	}

	@Override
	public double deltaE(int x , int y , int z , MTBLevelsetMembership phi) {
		double sum = 0.0;
		for ( int e = 0 ; e < energies.size() ; e++ )
			sum += energies.elementAt(e).deltaE( x, y, z, phi);
		return sum;
	}
	
	@Override
	public double deltaE(int x , int y , int z , short newPhase, MTBLevelsetMembership phi) {
		double sum = 0.0;
		for ( int e = 0 ; e < energies.size() ; e++ )
			sum += energies.elementAt(e).deltaE( x, y, z, newPhase, phi);
		return sum;
	}
	
	@Override
	public double E(MTBLevelsetMembership phi) {
		double sum = 0.0;
		for ( int e = 0 ; e < energies.size() ; e++ )
			sum += energies.elementAt(e).E( phi);
		return sum;
	}
	
	@Override
	public void updateParams(int x, int y, int z, MTBLevelsetMembership phi) {
		for ( int e = 0 ; e < energies.size() ; e++ )
			energies.elementAt(e).updateParams(x, y, z, phi);
	}

	@Override
	public void updateParams(int x, int y, int z, short newPhase, MTBLevelsetMembership phi) {
		for ( int e = 0 ; e < energies.size() ; e++ )
			energies.elementAt(e).updateParams(x, y, z, newPhase, phi);
	}

	@Override
	public void estimateParams( MTBLevelsetMembership phi) {
		for ( int e = 0 ; e < energies.size() ; e++ )
			energies.elementAt(e).estimateParams( phi);
	}

	@Override
	public void setDebug( int debug) {
		this.debug = debug;
		for ( int e = 0 ; e < energies.size() ; e++ )
			energies.elementAt(e).setDebug( debug);
	}

	@Override
	public  String 	toString() {
		String str = new String( name + " composed of\n");
		for ( int e = 0 ; e < energies.size()-1 ; e++ )
			str += energies.elementAt(e).toString() + "\n";

		str += energies.elementAt(energies.size()-1).toString();

        return str;

    }

	@Override
	public void print( MTBLevelsetMembership phi, PrintStream out, String indent) {
		out.println( name + " sub energies: ");

		String newIndent = getNewIndent( indent);
		for ( int e = 0 ; e < energies.size() ; e++ )
			energies.elementAt(e).print( phi, out, newIndent);
	}


}
