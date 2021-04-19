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

import java.lang.reflect.Field;

import de.biomedical_imaging.ij.steger.LinesUtil;

/**
 * Line datatype.
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
public class Line {

	/** number of points. */
	int num;

	/** row coordinates of the line points. */
	float[] row;

	/** column coordinates of the line points. */
	float[] col;

	/** angle of normal (measured from the row axis). */
	float[] angle;

	/** response of line point (second derivative). */
	float[] response;

	/** width to the left of the line. */
	float[] width_l;

	/** width to the right of the line. */
	float[] width_r;

	/** asymmetry of the line point. */
	float[] asymmetry;

	/** intensity of the line point. */
	float[] intensity;

	/** contour class (e.g., closed, no_junc) */
	private LinesUtil.contour_class cont_class;

	/** The id counter. */
	static int idCounter = 0;

	/** The id. */
	private int id;

	/** The frame. */
	private int frame;

	/**
	 * Instantiates a new line.
	 */
	public Line() {
		// TODO Auto-generated constructor stub
		assignID();
		
		if(id == 115)
		{
			System.out.println();
		}

	}

	/**
	 * Instantiates a new line.
	 *
	 * @param x
	 *            the x
	 * @param y
	 *            the y
	 */
	public Line(float[] x, float[] y) {
		assignID();
		col = x;
		row = y;
		num = x.length;
		
		if(id == 115)
		{
			System.out.println();
		}
	}

	/**
	 * Gets the contour class.
	 *
	 * @return the contour class
	 */
	public LinesUtil.contour_class getContourClass() {

		return cont_class;
	}

	/**
	 * Sets the contour class.
	 *
	 * @param cont_class
	 *            the new contour class
	 */
	public void setContourClass(LinesUtil.contour_class cont_class) {
		this.cont_class = cont_class;
	}

	/**
	 * Sets the frame.
	 *
	 * @param frame
	 *            the new frame
	 */
	public void setFrame(int frame) {
		this.frame = frame;
	}

	/**
	 * Gets the frame.
	 *
	 * @return the frame index where the line was detected
	 */
	public int getFrame() {
		return frame;
	}

	/**
	 * Gets the x coordinates.
	 *
	 * @return x coordinates of the line points
	 */
	public float[] getXCoordinates() {
		return col;
	}

	/**
	 * Gets the y coordinates.
	 *
	 * @return y coordinates of the line points
	 */
	public float[] getYCoordinates() {
		return row;
	}

	/**
	 * Gets the response.
	 *
	 * @return response of line point (second derivative)
	 */
	public float[] getResponse() {
		return response;
	}

	/**
	 * Gets the intensity.
	 *
	 * @return intensity of the line points
	 */
	public float[] getIntensity() {
		return intensity;
	}

	/**
	 * Gets the angle.
	 *
	 * @return angle of normal (measured from the y-axis)
	 */
	public float[] getAngle() {
		return angle;
	}

	/**
	 * Gets the asymmetry.
	 *
	 * @return asymmetry of the line point
	 */
	public float[] getAsymmetry() {
		return asymmetry;
	}

	/**
	 * Gets the line width L.
	 *
	 * @return width to the left of the line
	 */
	public float[] getLineWidthL() {
		return width_l;
	}

	/**
	 * Gets the line width R.
	 *
	 * @return width to the right of the line
	 */
	public float[] getLineWidthR() {
		return width_r;
	}

	/**
	 * Gets the number.
	 *
	 * @return Return the number of points
	 */
	public int getNumber() {
		return num;
	}

	/**
	 * Gets the id.
	 *
	 * @return unique ID of the line
	 */
	public int getID() {
		return id;
	}

	/**
	 * Gets the line class.
	 *
	 * @return the line class
	 */
	public LinesUtil.contour_class getLineClass() {
		return cont_class;
	}

	/**
	 * Gets the start ord end position.
	 *
	 * @param x
	 *            the x
	 * @param y
	 *            the y
	 * @return the start ord end position
	 */
	public int getStartOrdEndPosition(float x, float y) {
		double distStart = Math.sqrt(Math.pow(col[0] - x, 2) + Math.pow(row[0] - y, 2));
		double distEnd = Math.sqrt(Math.pow(col[(this.num - 1)] - x, 2) + Math.pow(row[(this.num - 1)] - y, 2));
		return (distStart < distEnd ? 0 : this.num - 1);
	}

	/**
	 * Estimate length.
	 *
	 * @return the estimated length of the line
	 */
	public double estimateLength() {
		double length = 0;
		for (int i = 1; i < num; i++) {
			length += Math.sqrt(Math.pow(col[i] - col[i - 1], 2) + Math.pow(row[i] - row[i - 1], 2));
		}
		return length;
	}

	/**
	 * Assign ID.
	 */
	private synchronized void assignID() {
		this.id = idCounter;
		idCounter++;
	}

	/**
	 * Reset counter.
	 */
	static void resetCounter() {
		idCounter = 0;
	}
	

// new functions
	
	@Override
	public String toString()
	{
		return "L" + id + "(" + col[0] + "/" + row[0] + " -> " + col[col.length-1] + "/" + row[row.length - 1] + ")";
	}

	public void castLine(de.biomedical_imaging.ij.steger.Line line)//, int sync_counter)
	{
		col = line.getXCoordinates();
		row = line.getYCoordinates();
		num = line.getNumber();
		angle = line.getAngle();
		response = line.getResponse();
		width_l = line.getLineWidthL();
		width_r = line.getLineWidthR();
		intensity = line.getIntensity();
		asymmetry = line.getAsymmetry();
		cont_class = line.getContourClass();
		id = line.getID();
		
		//use Java Reflections for accessing static attributes for synchronized id-counter from de.biomedical_imaging.ij.steger.Line
		//this may reduce performance
		try {
			Field idCounterField = de.biomedical_imaging.ij.steger.Line.class.getDeclaredField("idCounter");
			idCounterField.setAccessible(true);
			
			idCounter = (int) idCounterField.getInt(line); //not direct castable
		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//idCounter = sync_counter; //not castable
		frame = line.getFrame();
	}
	
	public synchronized void initCounter(int cnt)
	{
		idCounter = cnt;
	}
	
	public void setXCoordinates(float[] x)
	{
		col = x;
	}
	public void setYCoordinates(float[] y)
	{
		row = y;
	}
	
	public void setAngle(float[] angle)
	{
		this.angle = angle;
	}
	
	public void setWidthL(float[] widthL)
	{
		this.width_l = widthL;
	}
	public void setWidthR(float[] widthR)
	{
		this.width_r = widthR;
	}
	
	public void setNum(int num)
	{
		this.num = num;
	}
	
	public void setID(int id)
	{
		this.id = id;
	}
}
