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

package de.unihalle.informatik.MiToBo.core.exceptions;

/**
 * MiToBo exception type related to problems with its image data types.
 * 
 * @author moeller
 */
public class MTBImageException extends MTBException {

	/**
	 * Default constructor.
	 * @param c		Exception message.
	 */
	public MTBImageException(String c) {
		this.comment = c;
	}
	
	@Override
  protected String getExceptionID() {
		return "MTBImageException";
	}

	@Override
  public String getIdentString() {
	  return null;
  }
}
