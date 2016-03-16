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

package de.unihalle.informatik.MiToBo.core.operator;

import de.unihalle.informatik.Alida.version.ALDVersionProvider;
import de.unihalle.informatik.Alida.version.ALDVersionProviderFactory;

import java.io.*;

/**  
 * Helper class for global `MiToBo` operator configuration.
 * <p>
 * Note, this class is used by all `MiToBo` operators and implemented as
 * singleton.
 * 
 * @author moeller
 */
class MTBOperatorConfigTools {

	/**
	 * The object instance.
	 */
	private static MTBOperatorConfigTools confObj = 
			new MTBOperatorConfigTools();
	
	/**
	 * Version provider class to be used in the current session.
	 */
	private String providerClass;

	/**
	 * Access object for port hash.
	 */
	private MTBPortHashAccess portHashAccess;
	
	/**
	 * Hidden constructor, never used outside of the class.
	 */
	private MTBOperatorConfigTools() {
		this.portHashAccess = new MTBPortHashAccess();		
		// test if release file exists, if so, set version provider to file
		try { 
			String revFile = MTBVersionProviderReleaseFile.getRevisionFile();
			InputStream is= 
				MTBOperatorConfigTools.class.getResourceAsStream("/" + revFile);
			BufferedReader br= new BufferedReader(new InputStreamReader(is));
			String vLine= br.readLine();
			if (vLine == null) {
				br.close();
				throw new Exception();
			}
			this.providerClass = "de.unihalle.informatik.MiToBo.core." 
					+ "operator.MTBVersionProviderReleaseFile";
			br.close();
		} catch (Exception e) {
			this.providerClass =
				"de.unihalle.informatik.Alida.version.ALDVersionProviderDummy";
		}
	}
	
	/**
	 * Single access point for singleton functionality.
	 * @return	Reference to the singleton instance.
	 */
	public static MTBOperatorConfigTools getInstance() {
		return confObj;
	}

	/**
	 * Returns access object for port hash.
	 * @return Port hash access object.
	 */
	protected MTBPortHashAccess getPortHashAccessObject() {
		return this.portHashAccess;
	}
	
	/**
	 * Returns version provider object.
	 * @return Version provider for the current session.
	 */
	protected ALDVersionProvider getVersionProvider() {
	  if (!ALDVersionProviderFactory.isClassNameSpecified())
	  	return ALDVersionProviderFactory.getProviderInstance(
	  			this.providerClass);
	  return ALDVersionProviderFactory.getProviderInstance();
  }
}

