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

package de.unihalle.informatik.MiToBo.segmentation.activecontours.exceptions;

/**
 * MiToBo exception thrown inside snake segmentation framework.
 * 
 * @author moeller
 */
public class MTBSnakeException extends MTBActiveContourException {

	/**
	 * Default constructor.
	 * @param t	Type of exception.
	 * @param c	User-specific exception comment.
	 */
	public MTBSnakeException(ExceptionType t, String c) {
		super(t,c);
	}

	@Override
  public String getIdentString() {
		return "MTBSnakeException";
	}
}
