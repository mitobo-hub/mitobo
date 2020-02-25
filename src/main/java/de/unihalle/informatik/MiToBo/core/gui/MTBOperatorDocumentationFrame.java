/*
 * This file is part of Alida, a Java library for 
 * Advanced Library for Integrated Development of Data Analysis Applications.
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
 * Fore more information on Alida, visit
 *
 *    http://www.informatik.uni-halle.de/alida/
 *
 */

package de.unihalle.informatik.MiToBo.core.gui;

import de.unihalle.informatik.Alida.gui.ALDOperatorDocumentationFrame;

/**
 * Frame to show documentation for operators and tools.
 * 
 * @author Birgit Moeller
 */
public class MTBOperatorDocumentationFrame 
		extends ALDOperatorDocumentationFrame {

	/**
	 * Name of associated operator.
	 */
	protected String opName = null;
	
	/**
	 * Package of associated operator/tool.
	 */
	protected String opPackage = null;
	
	/**
	 * Constructor with title parameter.
	 * @param title			Window title.
	 * @param operator	Associated operator.
	 * @param pLevel		Level of interaction the providers should obey to.
	 */
	public MTBOperatorDocumentationFrame(String name, String pack, 
			String docText) {

		// init the frame
		super(name, pack, docText);
	}
	
	@Override
	protected String getAPIText() {
		return "Class API at mitobo.informatik.uni-halle.de";
	}
	
	@Override
	protected String getAPIURL() {
		return "https://mitobo.informatik.uni-halle.de/api/";
	}
}
