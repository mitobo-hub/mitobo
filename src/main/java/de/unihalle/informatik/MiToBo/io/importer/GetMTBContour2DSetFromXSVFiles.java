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

package de.unihalle.informatik.MiToBo.io.importer;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.ExecutionMode;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.datatypes.ALDDirectoryString;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBContour2DSet;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;

/**
 * Read a {@link MTBContour2DSet} from a list of ASCII xSV files.
 * <p>
 * The files are expected to all share the same format and contain a single 
 * point in each row, first the x-coordinate and then the y-coordinate. 
 * Both coordinates should be separated by the delimiter character, 
 * e.g., ',' or ' '.
 * <p>
 * Header lines in each file maybe skipped using the {@link #skipLines}
 * argument. If it is zero no line is skipped.
 *   
 * @author Birgit Moeller
 */
@ALDAOperator(genericExecutionMode=ExecutionMode.ALL)
public class GetMTBContour2DSetFromXSVFiles extends MTBOperator {
	
	/**
	 * Input directory from where to read the xSV files.
	 * <p>
	 * We assume that there are no other files in the directory than xSV
	 * files containing contours.
	 */
	@Parameter( label= "inputDir", required = true,
		direction=Direction.IN, dataIOOrder=0, mode=ExpertMode.STANDARD,
		description = "Directory with input files.")
	private ALDDirectoryString inputDir = null;

	/**
	 * Delimiter in xSV files.
	 */
	@Parameter( label= "delim", required = true,
		direction=Direction.IN, dataIOOrder=1, mode=ExpertMode.STANDARD,
		description = "Delimiter in xSV format.")
	private String delim = " ";	
	
	/**
	 * Skip header lines.
	 * <p>
	 * Number of header lines to be skipped, if zero, nothing is skipped.
	 */
	@Parameter( label= "skipLines", required = true,
		direction=Direction.IN, dataIOOrder=2, mode=ExpertMode.STANDARD,
		description = "Number of header lines to skip.")
	private int skipLines = 0;	

	/**
	 * Resulting contour set.
	 */
	@Parameter( label= "resultSet",  
		direction=Direction.OUT, dataIOOrder=0, mode=ExpertMode.STANDARD,
		description = "Result contour set.")
	private MTBContour2DSet resultSet;

	/**
	 * Default constructor.
	 * @throws ALDOperatorException	Thrown in case of failure.
	 */
	public GetMTBContour2DSetFromXSVFiles() throws ALDOperatorException {
		// nothing happens here
	}
	
	@Override
	protected void operate() {
		this.resultSet = MTBContour2DSet.readContoursFromASCIIFiles(
				this.inputDir.getDirectoryName(), this.delim, this.skipLines);
	}
}
