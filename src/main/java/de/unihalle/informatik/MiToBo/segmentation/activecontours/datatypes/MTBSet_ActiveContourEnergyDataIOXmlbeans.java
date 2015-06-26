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
import de.unihalle.informatik.Alida.dataio.ALDDataIOManagerXmlbeans;
import de.unihalle.informatik.Alida.dataio.provider.xmlbeans.ALDStandardizedDataIOXmlbeans;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOManagerException;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException.ALDDataIOProviderExceptionType;
import de.unihalle.informatik.Alida_xml.ALDXMLAnyType;
import de.unihalle.informatik.Alida_xml.ALDXMLObjectType;
import de.unihalle.informatik.MiToBo.segmentation.levelset.core.MTBSet_LevelEnergyDerivable;
import de.unihalle.informatik.MiToBo.segmentation.levelset.core.energies.derivable.MTBLevelsetEnergyDerivable;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSet_SnakeEnergyComputable;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSet_SnakeEnergyDerivable;
import de.unihalle.informatik.MiToBo.segmentation.snakes.energies.MTBSnakeEnergyComputable;
import de.unihalle.informatik.MiToBo.segmentation.snakes.energies.MTBSnakeEnergyDerivable;
import de.unihalle.informatik.MiToBo_xml.MTBXMLSetWeightedEnergyType;

import java.lang.reflect.Field;
import java.util.*;

import org.apache.xmlbeans.XmlObject;

/**
 * Class for loading/saving sets of PDE energies for command line interfaces.
 * 
 * @author posch
 */

@ALDDataIOProvider(priority=10)
public class MTBSet_ActiveContourEnergyDataIOXmlbeans extends ALDStandardizedDataIOXmlbeans {
	@Override
	public Collection<Class<?>> providedClasses() {
		LinkedList<Class<?>> classes = new LinkedList<Class<?>>();

		classes.add(MTBSet_SnakeEnergyDerivable.class);
	  classes.add(MTBSet_SnakeEnergyComputable.class);
		classes.add(MTBSet_LevelEnergyDerivable.class);
		return classes;
	}

	@Override
	public Object readData(Field field, Class<?> cl,
			ALDXMLObjectType aldXmlObject, Object object)
			throws ALDDataIOProviderException, ALDDataIOManagerException {
		if (cl == null)
			throw new ALDDataIOProviderException(ALDDataIOProviderExceptionType.OBJECT_TYPE_ERROR, 
					"MTBSet_ActiveContourEnergyDataIOXmlbeans::readData cl == null");
		
		if ( aldXmlObject == null || aldXmlObject.isNil())
			return null;
		
		if ( ! (aldXmlObject instanceof ALDXMLAnyType)) {
			throw new ALDDataIOProviderException(ALDDataIOProviderExceptionType.SYNTAX_ERROR, 
					"MTBSet_ActiveContourEnergyDataIOXmlbeans::readData wrong xml object, is not of type ALDXMLAnyType");
		}
		
		MTBXMLSetWeightedEnergyType setWE;
		try {
			 setWE = (MTBXMLSetWeightedEnergyType) ((ALDXMLAnyType)aldXmlObject).getValue();
		} catch (Exception e) {
			throw new ALDDataIOProviderException(ALDDataIOProviderExceptionType.SYNTAX_ERROR, 
					"MTBSet_ActiveContourEnergyDataIOXmlbeans::readData wrong xml object does not " +
					"hold an xml object of type MTBXMLSetWeightedEnergyType");
		}

		// first we instantiate the object required and add the energies as read from xml
		// this is distinct for snakes and level sets
		
		MTBSet_ActiveContourEnergy energySet = null;

		try {
			if (cl.equals(MTBSet_SnakeEnergyDerivable.class) && 
					aldXmlObject.getClassName().equals(
						MTBSet_SnakeEnergyDerivable.class.getName())) {
				energySet = new MTBSet_SnakeEnergyDerivable();
			} else if (cl.equals(MTBSet_SnakeEnergyComputable.class) && 
					aldXmlObject.getClassName().equals(
						MTBSet_SnakeEnergyComputable.class.getName())) {
				energySet = new MTBSet_SnakeEnergyComputable();
			} else if (cl.equals(MTBSet_LevelEnergyDerivable.class) &&
					((ALDXMLAnyType)aldXmlObject).getClassName().equals(
						MTBSet_LevelEnergyDerivable.class.getName()) ) {
				energySet = new MTBSet_LevelEnergyDerivable();
			} else {
				throw new ALDDataIOProviderException(ALDDataIOProviderExceptionType.OBJECT_TYPE_ERROR, 
						"MTBSet_ActiveContourEnergyDataIOXmlbeans::readData cannot read object of type " +
								cl.getName() + ">" +
								" from <" + aldXmlObject.getClassName() + ">\n");
			}
		} catch (Exception e) {
			throw new ALDDataIOProviderException(ALDDataIOProviderExceptionType.SYNTAX_ERROR, 
					"MTBSet_ActiveContourEnergyDataIOXmlbeans::readData wrong xml object of type <" +
							((ALDXMLAnyType)aldXmlObject).getValue().getClass().getName() +
							"> to read an object of class <" + cl.getName() + ">");

		}
		
		// now we  add the energies as read from xml
		// this is distinct for snakes and level sets
		
		Vector<MTBActiveContourEnergy> energies = 
				new Vector<MTBActiveContourEnergy>();

		for ( int i = 0 ; i < setWE.getEnergiesArray().length ; i++) {
			try {
				MTBActiveContourEnergy energy = null;

				if (cl.equals(MTBSet_SnakeEnergyDerivable.class) ) {
					energy = 
						(MTBSnakeEnergyDerivable)ALDDataIOManagerXmlbeans.getInstance().
							readData(null, MTBSnakeEnergyDerivable.class, 
								(ALDXMLObjectType)setWE.getEnergiesArray(i));
				} else if (cl.equals(MTBSet_SnakeEnergyComputable.class) ) {
					energy = 
						(MTBSnakeEnergyComputable)ALDDataIOManagerXmlbeans.getInstance().
							readData(null, MTBSnakeEnergyComputable.class, 
								(ALDXMLObjectType)setWE.getEnergiesArray(i));
				} else if (cl.equals(MTBSet_LevelEnergyDerivable.class) ) {
					energy = 
						(MTBLevelsetEnergyDerivable)ALDDataIOManagerXmlbeans.getInstance().
							readData(null, MTBLevelsetEnergyDerivable.class, 
								(ALDXMLObjectType)setWE.getEnergiesArray(i));
				}
				energies.add( energy);
			} catch (ALDDataIOManagerException e) {
				throw new ALDDataIOManagerException( e.getType(),
						"MTBSet_ActiveContourEnergyDataIOXmlbeans::readData Error: " +
								"cannot read single active contour energy from <" + 
								setWE.getEnergiesArray(i) + ">\n" + e.getMessage());
			} catch (ALDDataIOProviderException e) {
				throw new ALDDataIOProviderException( e.getType(),
						"MTBSet_ActiveContourEnergyDataIOXmlbeans::readData Error: " +
								"cannot read single active contour energy from <" + 
								setWE.getEnergiesArray(i) + ">\n" + e.getMessage());
			} catch (Exception e) {
				throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.UNSPECIFIED_ERROR,
						"MTBSet_ActiveContourEnergyDataIOXmlbeans::readData Error: " +
								"cannot read single active contour energy from <" + 
								setWE.getEnergiesArray(i) + ">\n" + e.getMessage());
			}
		}
		
		energySet.setEnergyList(energies);
		
		// now we add the weights, uniformly for both types of active contours
		Vector<Double> weights = new Vector<Double>();
		for ( int i = 0 ; i < setWE.getWeightsArray().length ; i++) {
			weights.add(new Double(setWE.getWeightsArray()[i]));		
		}
		energySet.setWeights( weights);	
		
		return energySet;
	}

	@Override
	public ALDXMLObjectType writeData(Object obj)
			throws ALDDataIOManagerException, ALDDataIOProviderException {
		ALDXMLAnyType aldXmlObject = ALDXMLAnyType.Factory.newInstance();
		aldXmlObject.setClassName(obj.getClass().getName());
		
		MTBSet_ActiveContourEnergy setACED = (MTBSet_ActiveContourEnergy)obj;
		
		MTBXMLSetWeightedEnergyType setWE = MTBXMLSetWeightedEnergyType.Factory.newInstance();
		
		for ( Double weight : setACED.getWeights()) {
			setWE.addWeights(weight.doubleValue());
		}
		
		XmlObject[] xmlEnergies = new XmlObject[setACED.getGenericEnergyList().size()];
		for ( int i = 0 ; i < setACED.getGenericEnergyList().size() ; i++) {
			xmlEnergies[i] = ALDDataIOManagerXmlbeans.getInstance().writeData(setACED.getEnergy(i));
		}
		setWE.setEnergiesArray(xmlEnergies);
		
		aldXmlObject.setValue(setWE);
		return aldXmlObject;
	}
}
