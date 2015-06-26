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

package de.unihalle.informatik.MiToBo.core.datatypes;

import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;

/**
 * Exception handling for Polygon2D object.
 * 
 * @see de.unihalle.informatik.MiToBo.core.datatypes.MTBPolygon2D
 * 
 * 
 * @author Danny Misiak
 */
@ALDMetaInfo(export = ExportPolicy.ALLOWED)
public class MTBPolygon2DException extends Exception {
		/**
		 * Exception error message.
		 */
		private String error;

		/**
		 * Standardconstructor.
		 */
		public MTBPolygon2DException() {
				super();
		}

		/**
		 * Constructor for new MTBPolygon2DException with specific message output.
		 * 
		 * @param msg
		 *          exception message
		 */
		public MTBPolygon2DException(String msg) {
				super("Polygon2D Exception!!!\n" + msg);
				this.error = "Polygon2D Exception!!!\n" + msg;
		}

		/**
		 * Returns the complete exception message as string.
		 * 
		 * @return Exception message.
		 */
		public String getError() {
				return this.error;
		}

}
