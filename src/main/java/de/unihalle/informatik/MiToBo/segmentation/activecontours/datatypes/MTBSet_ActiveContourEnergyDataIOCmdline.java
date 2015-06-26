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

package de.unihalle.informatik.MiToBo.segmentation.activecontours.datatypes;

import de.unihalle.informatik.Alida.annotations.ALDDataIOProvider;
import de.unihalle.informatik.Alida.dataio.ALDDataIOManagerCmdline;
import de.unihalle.informatik.Alida.dataio.provider.cmdline.ALDParametrizedClassDataIOCmdline;
import de.unihalle.informatik.Alida.dataio.provider.cmdline.ALDStandardizedDataIOCmdline;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOManagerException;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException.ALDDataIOProviderExceptionType;
import de.unihalle.informatik.Alida.helpers.ALDParser;
import de.unihalle.informatik.MiToBo.segmentation.levelset.core.MTBSet_LevelEnergyDerivable;
import de.unihalle.informatik.MiToBo.segmentation.levelset.core.energies.derivable.MTBLevelsetEnergyDerivable;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSet_SnakeEnergyComputable;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSet_SnakeEnergyDerivable;
import de.unihalle.informatik.MiToBo.segmentation.snakes.energies.MTBSnakeEnergyComputable;
import de.unihalle.informatik.MiToBo.segmentation.snakes.energies.MTBSnakeEnergyDerivable;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Class for loading/saving sets of PDE energies for command line interfaces.
 * 
 * @author posch
 */

@ALDDataIOProvider(priority=10)
public class MTBSet_ActiveContourEnergyDataIOCmdline extends ALDStandardizedDataIOCmdline {
	/**
	 * Debugging output
	 */
	private boolean debug = false;

	/**
	 * Used as a substitute for the same field in MTBSet_???EnergyDerivable
	 * (problems with generics, generically read a Vector!!)
	 */
	Vector<Double> weights = null;
	
	/**
	 * Fields of a EnergySet we parse
	 */
	HashSet<String> energySetFields = new HashSet<String>();
	
	/** 
	 * used to work around generics
	 */
	HashMap<String, Field> fieldMap = new HashMap<String, Field>();

	
	/**
	 * Default constructor
	 */
	public MTBSet_ActiveContourEnergyDataIOCmdline() {
		this.energySetFields.add( "energies");
		this.energySetFields.add( "weights");

		// this is to work around generics
		// field to be used to read a Vector of Double
		for (Field auxField : this.getClass().getDeclaredFields()) {
			this.fieldMap.put( auxField.getName(), auxField);
		}
	}

	@Override
	public Collection<Class<?>> providedClasses() {
		LinkedList<Class<?>> classes = new LinkedList<Class<?>>();

		classes.add(MTBSet_SnakeEnergyDerivable.class);
	  classes.add(MTBSet_SnakeEnergyComputable.class);
		classes.add(MTBSet_LevelEnergyDerivable.class);
		return classes;
	}


	@SuppressWarnings("unchecked")
	@Override
	public Object parse(Field field, Class<?> cl, String valueString) 
			throws ALDDataIOProviderException, ALDDataIOManagerException {
		if ( this.debug ) {
			System.out.println("MTBSet_ActiveContourEnergyDataIOCmdline::parse valueString <" +
					valueString + ">");
		}
		String pairStr;
		if ( valueString.charAt(0) != '{' ||
				(pairStr = ALDParser.parseBracket( valueString, '}')) == null ) {
			throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.SYNTAX_ERROR,
					"MTBSet_ActiveContourEnergyDataIOCmdline::readData cannot find matching {} in <" + 
					valueString + ">");
		}
		
		Vector<MTBActiveContourEnergy> energies = null;

		MTBSet_ActiveContourEnergy energySet = null;
		if (   cl == MTBSet_SnakeEnergyDerivable.class  
				|| cl == MTBSet_LevelEnergyDerivable.class
				|| cl == MTBSet_SnakeEnergyComputable.class) {
			
			if ( cl == MTBSet_SnakeEnergyDerivable.class ) {
				energySet = new MTBSet_SnakeEnergyDerivable();
			} 
			else if ( cl == MTBSet_SnakeEnergyComputable.class ) {
				energySet = new MTBSet_SnakeEnergyComputable();								
			}
			else {
				energySet = new MTBSet_LevelEnergyDerivable();				
			}

			if ( ! pairStr.trim().equals("") ) {
				// there is anything to parse
				HashMap<String,String> nameValuePairs = ALDParser.parseNameValuePairs( pairStr.trim());

				for ( String name : nameValuePairs.keySet() )  {
					if ( this.energySetFields.contains(name)) {
						if ( name.equalsIgnoreCase("energies") ) {
							// read the energies; do it explicitly analogous to ALDCollectionDataIOCmdline
							// but cannot use this generic provider due to type inference problem
							String auxValueString = nameValuePairs.get( name);
							if ( this.debug ) {
								System.out.println( "MTBSet_ActiveContourEnergyDataIOCmdline::parse " +
										"parsing energies using <" + auxValueString + ">");
							}

							auxValueString = auxValueString.trim();
							if ( auxValueString.length() < 2 || auxValueString.charAt(0) != '[' || auxValueString.charAt(auxValueString.length()-1) != ']' ) {
								throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.SYNTAX_ERROR,
										"MTBSet_ActiveContourEnergyDataIOCmdline::parse cannot read active contour energies from >" + 
												auxValueString + ">\n   missing matching []");
							}

							// loop over all energies
							energies = new Vector<MTBActiveContourEnergy>();
							for ( String aux2ValueString : ALDParser.split( auxValueString.substring( 
									1, auxValueString.length()-1), ',') ) {
								if ( this.debug ) 
									System.out.println("MTBSet_ActiveContourEnergyDataIOCmdline::parse " + 
											"one energy from <" + aux2ValueString + ">");
								try {
									MTBActiveContourEnergy energy;

									if  (cl == MTBSet_SnakeEnergyDerivable.class) {
										energy = 
												(MTBSnakeEnergyDerivable) ALDDataIOManagerCmdline.getInstance().
												readData( null, MTBSnakeEnergyDerivable.class, aux2ValueString);
									} else if  (cl == MTBSet_SnakeEnergyComputable.class) {
											energy = 
													(MTBSnakeEnergyComputable) ALDDataIOManagerCmdline.getInstance().
													readData( null, MTBSnakeEnergyComputable.class, aux2ValueString);
									} else {
										energy = 
												(MTBLevelsetEnergyDerivable) ALDDataIOManagerCmdline.getInstance().
												readData( null, MTBLevelsetEnergyDerivable.class, aux2ValueString);

									}
									energies.add(energy);
								} catch (ALDDataIOManagerException e) {
									throw new ALDDataIOManagerException( e.getType(),
											"MTBSet_ActiveContourEnergyDataIOCmdline::parse Error: " +
													"cannot read single active contour energy from <" + 
													aux2ValueString + ">\n" + e.getMessage());
						}

							}
						} else {
							// must be weights now
							if ( this.debug ) {
								System.out.println( "MTBSet_ActiveContourEnergyDataIOCmdline::parse parsing weights");
							}
							
							Field f = this.fieldMap.get( name);
							if ( f == null) {
								throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.SYNTAX_ERROR,
										"MTBSet_ActiveContourEnergyDataIOCmdline::parse cannot find field for "+
										name);
							}
							
							try {
								this.weights = (Vector<Double>) ALDDataIOManagerCmdline.getInstance().readData( 
										f, null, nameValuePairs.get( name));
							} catch (ALDDataIOManagerException e1) {
									throw new ALDDataIOManagerException( e1.getType(),
											"MTBSet_ActiveContourEnergyDataIOCmdline::parse Error: cannot weights " +
													"of class <" +
										f.getType().getCanonicalName() + ">\n    from <" + 
										nameValuePairs.get( name) + ">\n" + e1.getMessage());
							}

						}
					} else {
						StringBuffer msg = new StringBuffer("   existing parameters:");
						for ( String key : this.energySetFields ) 
							msg.append( "         " + key);

						throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.OBJECT_TYPE_ERROR,
								"MTBSet_ActiveContourEnergyDataIOCmdline::parse " + cl.getName() +
								" does not contain an annotated member "+ name +
										new String( msg));
					}
				}
			} else {
				return null;
			}
			
			if ( energies != null && this.weights != null ) {
				if ( energies.size() != this.weights.size() ) {
					throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.SYNTAX_ERROR,
					 "MTBSet_ActiveContourEnergyDataIOCmdline::parse lenght of energies (" + 
							energies.size() + ") and weights (" + this.weights.size() + ") differs");
				}

				energySet.setEnergyList(energies);
				energySet.setWeights(this.weights);
				if ( this.debug) {
					for ( int i = 0 ; i < energySet.getWeights().size() ; i++) {
						System.out.println( "MTBSet_ActiveContourEnergyDataIOCmdline::parse read " + 
								energySet.getEnergy(i) + " with weight " + energySet.getWeight(i));
					}
				}
				return energySet;
			}
			if ( energies == null )
				throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.SYNTAX_ERROR,
						"MTBSet_ActiveContourEnergyDataIOCmdline::parse energies need to be given"); 
			if ( this.weights == null )
				throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.SYNTAX_ERROR,
						"MTBSet_ActiveContourEnergyDataIOCmdline::parse weights need to be given"); 
			return null;

		}
		throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.UNSPECIFIED_ERROR,
				"MTBSet_ActiveContourEnergyDataIOCmdline::parse unsupported class <" + 
				cl.getName() + ">");
	}


	@Override
	public String formatAsString(Object obj) throws ALDDataIOProviderException, ALDDataIOManagerException {
		ALDParametrizedClassDataIOCmdline provider = new ALDParametrizedClassDataIOCmdline();
		return provider.formatAsString(obj);
	}
}
