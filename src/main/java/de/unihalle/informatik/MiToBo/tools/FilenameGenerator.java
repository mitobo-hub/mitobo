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

/*
 * This class uses the Bio-Formats and LOCI-commons packages/libraries (see the two licenses at the top)
 * as well as source code from the LOCI-plugins package (see third license from the top)
 */

package de.unihalle.informatik.MiToBo.tools;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.ExecutionMode;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.datatypes.ALDFileString;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.helpers.ALDFilePathManipulator;
import de.unihalle.informatik.Alida.operator.ALDOperator;

/**
 * Utility operator to generate a filename from a given filename.
 * With the default setting of <code>replacementPattern</code> the filename extension (if existing) including the dot is
 * replaced by the .
 * <p>
 * In general, if a custom  <code>replacementPattern</code> is specified, all occurrences
 * of this pattern are replaced using it as a Java <code>Pattern</code> for a Java <code>Matcher</code>
 * invoking the method {@link java.util.regex.Matcher#replaceAll(String)} with <code>replacementString</code>
 * as argument.
 * 
 * @author Stefan Posch
 *
 */
@ALDAOperator(genericExecutionMode=ExecutionMode.SWING,level=Level.STANDARD)
public class FilenameGenerator extends ALDOperator {

	@Parameter( label= "Filename", required = true, direction = Direction.INOUT,
			mode = ExpertMode.STANDARD, dataIOOrder = 1,
			description = "Filename")
	protected ALDFileString fileName = null;

	@Parameter( label= "Replacement string", required = true, direction = Direction.IN,
			mode = ExpertMode.STANDARD, dataIOOrder = 2,
			description = "Replacment string, default replacing the extension including dot")
	protected String replacementString = null;

	@Parameter( label= "Replacement pattern", required = false, direction = Direction.IN,
			mode = ExpertMode.STANDARD, dataIOOrder = 3,
			description = "Pattern to be replaced (all occurences), default is extention incuding dot")
	protected String replacementPattern = "\\.[^." + System.getProperty("file.separator") + "/]*$";

	@Parameter( label= "FilenameOut", required = true, direction = Direction.OUT,
			mode = ExpertMode.STANDARD, dataIOOrder = 4,
			description = "Filename with differnt extension")
	protected ALDFileString fileNameOut = null;


	public FilenameGenerator() throws ALDOperatorException {
	}

	@Override
	protected void operate() {

		Pattern p = Pattern.compile(replacementPattern);
		Matcher m = p.matcher(fileName.getFileName());

		fileNameOut = new ALDFileString( m.replaceAll(replacementString));
	}
}
