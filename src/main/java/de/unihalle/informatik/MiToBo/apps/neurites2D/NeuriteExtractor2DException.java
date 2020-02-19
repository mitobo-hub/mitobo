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

package de.unihalle.informatik.MiToBo.apps.neurites2D;

/**
 * Exception handling for 2D neurite extraction.
 * 
 * @see NeuriteExtractor2D
 * 
 * 
 * @author misiak
 */
public class NeuriteExtractor2DException extends Exception {

		private String error;

		public NeuriteExtractor2DException() {
				super();
		}

		public NeuriteExtractor2DException(String msg) {
				super("Neurite Extractor 2D Exception!!!\n" + msg);
				this.error = "Neurite Extractor 2D Exception!!!\n" + msg;
		}

		public String getError() {
				return this.error;
		}

}
