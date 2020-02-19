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

package de.unihalle.informatik.MiToBo.core.helpers;

import de.unihalle.informatik.Alida.helpers.ALDEnvironmentConfig;

/**
 * This class extends the super class with routines to access ImageJ
 * properties.
 * <p>
 * Every MiToBo operator and also every class can define properties. To ensure 
 * a certain structure of the properties and avoid chaos in property names 
 * the properties should commit to the following convention:
 * 
 *  mitobo.operatorname.property
 * 
 * The corresponding environment variable will then be 
 * 
 * MITOBO_OPERATORNAME_PROPERTY
 * 
 * following common Unix/Linux conventions. 
 * <p>
 * 
 * @author moeller
 *
 */
public class MTBEnvironmentConfig extends ALDEnvironmentConfig {

	/**
	 * This function reads out the value of a given ImageJ preference.
	 * 
	 * @param plugin	Name of plugin, ignored if null.
	 * @param envVariable	Name of the preference.
	 * @return	value of property, null if not existing
	 */
	public static String getImageJPropValue(String plugin, String envVariable) {
		String prefix= null;
		if (plugin == null)
			prefix= "mitobo";
		else
			prefix= "mitobo." + plugin;
		String envPropNameTmp= prefix + "." + envVariable;
		String envPropName= envPropNameTmp.toLowerCase();
		return ij.Prefs.get(envPropName,null);		
	}
	
	/**
	 * This functions sets a given property to the specified value.
	 * <p>
	 * The properties are saved to ~/.imagej/Prefs.txt when the ImageJ gui is
	 * properly closed (not killed!) by the user. Note that already defined 
	 * property are simply overwritten with new values if the method is called 
	 * on existing properties.  
	 * 
	 * @param plugin	Name of Mitobo plugin, ignored if null.
	 * @param envVar	Name of property.
	 * @param val		Value of property.
	 */
	public static void setImageJPref(String plugin, String envVar, String val) {
		String prefix= null;
		if (plugin == null)
			prefix= "mitobo";
		else
			prefix= "mitobo." + plugin;

		String envPropNameTmp= prefix + "." + envVar;
		String envPropName= envPropNameTmp.toLowerCase();
		ij.Prefs.set(envPropName, val);
	}
	
	/**
	 * This function reads out the value of a given environment property.
	 * <p>
	 * Here the default prefix "mitobo" is assumed. It is combined with operator 
	 * and environment variable name in this order.
	 * 
	 * @param _operator			Name of the operator.
	 * @param _envVariable	Name of the property.
	 * @return	Value of property, NULL if not existing.
	 */
	public static String getConfigValue(String _operator, String _envVariable) {
		return getConfigValue("mitobo", _operator, _envVariable);
	}
	
	/**
	 * This function reads out the value of a given JVM property.
	 * <p>
	 * Default prefix is "mitobo".
	 * 
	 * @param _operator			Name of operator, ignored if null.
	 * @param _envVariable	Name of the property.
	 * @return	Value of property, NULL if not existing.
	 */
	public static String getJVMPropValue(String _operator, String _envVariable){
		return getJVMPropValue("mitobo", _operator, _envVariable);
	}
	/**
	 * This function reads out the value of a given environment variable.
	 * <p>
	 * Default prefix is "mitobo".
	 * 
	 * @param _operator			Name of operator, ignored if null.
	 * @param _envVariable	Name of the variable.
	 * @return	Value of property, NULL if not existing.
	 */
	public static String getEnvVarValue(String _operator, String _envVariable) {
		String operator = (_operator == null || _operator.isEmpty()) ? 
																											"" : _operator + "_";
		String envVariable = (_envVariable == null || _envVariable.isEmpty()) ? 
																											"" : _envVariable;
		String envVarNameTmp= "mitobo_" + operator + envVariable;
		String envVarName= envVarNameTmp.toUpperCase();		
		return System.getenv(envVarName); 
	}

}
