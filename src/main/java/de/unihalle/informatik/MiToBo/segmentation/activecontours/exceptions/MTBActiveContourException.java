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

package de.unihalle.informatik.MiToBo.segmentation.activecontours.exceptions;

import de.unihalle.informatik.MiToBo.core.exceptions.MTBException;

/**
 * MiToBo exception thrown in context of segmentation with active contours.
 * 
 * @author moeller
 */
public class MTBActiveContourException extends MTBException {

	/**
	 * Possible exception types.
	 */
	public static enum ExceptionType {
		/**
		 * Something could not be initialized properly.
		 */
		INITIALIZATION_ERROR,
		/**
		 * Something could not be updated properly.
		 */
		UPDATE_ERROR,
		/**
		 * An unspecified error.
		 */
		UNSPECIFIED_ERROR
	}

	/**
	 * Type of exception.
	 */
	protected ExceptionType type = ExceptionType.UNSPECIFIED_ERROR;
	
	/**
	 * Default constructor.
	 * @param t	Type of exception.
	 * @param c	User-specific exception comment.
	 */
	public MTBActiveContourException(ExceptionType t, String c) {
		this.comment= c;
		this.type= t;
	}

	@Override
  public String getIdentString() {
		return "MTBActiveContourException";
	}
}
