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

package de.unihalle.informatik.MiToBo.apps.cellMorphology.paceQuant.datatypes;

/**
 * Junction datatype.
 * <p>
 * This class has been inspired by and in parts been copied from the original 
 * Ridge Detection plugin by Carsten Steger (and Thorsten Wagner).
 * <p>
 * Related publication:
 * <ul>
 * <li> C. Steger, <i>An unbiased detector of curvilinear structures</i>, in  
 * IEEE Transactions on Pattern Analysis and Machine Intelligence, 
 * 20(2), pp.113–125, 1998
 * </ul>
 * <p>
 * For more details take a look at ...
 * <ul>
 * <li> the website of the corresponding ImageJ plugin: 
 * 			<a href="http://imagej.net/Ridge_Detection">
 * 			         http://imagej.net/Ridge_Detection</a>
 * <li> the Github page with the source code: 
 * 			<a href="https://github.com/thorstenwagner/ij-ridgedetection">
 *               https://github.com/thorstenwagner/ij-ridgedetection</a>
 * </ul>
 *
 * @author Benjamin Schwede
 */
public class Junction implements Comparable<Junction> {

	/** Index of line that is already processed. */
	int cont1;

	/** Index of line tnat runs into cont1. */
	int cont2;

	/** Index of the junction point in cont1. */
	int pos;

	/** x-(row-)coordinate of the junction point. */
	float x;

	/** y-(col-)coordinate of the junction point. */
	float y;

	/** line that is already processed. */
	Line lineCont1;

	/** line that runs into idCont1. */
	Line lineCont2;

	/** True if this junction sits on a start/end of at least one line. */
	boolean isNonTerminal = false;

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Junction o) {
		return (((this.cont1 - o.cont1) != 0) ? this.cont1 - o.cont1 : this.pos - o.pos);
	}

	/**
	 * Gets the x.
	 *
	 * @return x-coordinate of the junction point
	 */
	public float getX() {
		return x;
	}

	/**
	 * Gets the y.
	 *
	 * @return y-coordinate of the junction point
	 */
	public float getY() {
		return y;
	}

	/**
	 * Gets the line 1.
	 *
	 * @return The line that is already processed
	 */
	public Line getLine1() {
		return lineCont1;
	}

	/**
	 * Gets the line 2.
	 *
	 * @return The line that runs into line1
	 */
	public Line getLine2() {
		return lineCont2;
	}

	/**
	 * Checks if is non terminal.
	 *
	 * @return True iff this junction point does not sit on either line's start/end
	 */
	public boolean isNonTerminal() {
		return isNonTerminal;
	}
	
//new functions
	public void castJunction(de.biomedical_imaging.ij.steger.Junction junction)//, int sync_counter)
	{
		cont1 = -1; //not castable
		cont2 = -1; //not castable
		pos = -1; //not castable
		x = junction.getX();
		y = junction.getY();
		lineCont1 = new Line();
		lineCont1.castLine(junction.getLine1());//, sync_counter);
		lineCont2 = new Line();
		lineCont2.castLine(junction.getLine2());//, sync_counter);
		isNonTerminal = junction.isNonTerminal();
	}
	
	public void setLine1(Line line)
	{
		lineCont1 = line;
	}
	
	public void setLine2(Line line)
	{
		lineCont2 = line;
	}
	
	public void setX(float x) {
		this.x = x;
	}

	public void setY(float y) {
		this.y = y;
	}
	
	public void replaceLine(int lineId, Line newLine)
	{
		if(lineCont1.getID() == lineId)
		{
			lineCont1 = newLine;
		}
		if(lineCont2.getID() == lineId)
		{
			lineCont2 = newLine;
		}
	}

}

