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

package de.unihalle.informatik.MiToBo.apps.singleCellTracking2D;

import java.awt.geom.Point2D;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;


/**
 * 
 * @author glass
 *
 */
public abstract class ReportGenerator extends MTBOperator
{
	@Parameter(label = "\u0394 x", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "pixel length in x-direction", dataIOOrder = 5)
	protected double deltaX = 1;	// pixel length in x-direction
	
	@Parameter(label = "\u0394 y", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "pixel length in y-direction", dataIOOrder = 6)
	protected double deltaY = 1;	// pixel length in y direction
	
	@Parameter(label = "\u0394 z", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "pixel length in z-direction", dataIOOrder = 7)
	protected double deltaZ = 1;	// pixel length in y direction
	
	@Parameter(label = "\u0394 t", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "time between consecutive frames", dataIOOrder = 8)
	protected double deltaT = 1;	// time between consecutive frames
	
	@Parameter(label = "unit of space", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "unit of space", dataIOOrder = 9)
	protected String unitSpace = "micron";	// unit of space
	
	@Parameter(label = "unit of time", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "unit of time", dataIOOrder = 10)
	protected String unitTime = "min";	// unit of time
	
	@Parameter(label = "unit of angles", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "unit of angles", dataIOOrder = 11)
	protected String unitAngle = "degree";	// unit of angles
	
	@Parameter(label = "minimum track length", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "minimum length a track must have to be considered", dataIOOrder = 12)
	protected int minTrackLength = 1;	// minimum length a track must have to be considered
	
	@Parameter(label = "report", required = true, direction = Parameter.Direction.OUT, supplemental = false, description = "resulting report")
	protected String report = "";
	
	protected NumberFormat nf = NumberFormat.getInstance();
	
	protected final String naString = "NA";	// String indicating that no data are available
	
	protected Vector<Integer> exclude = new Vector<Integer>();	// contains region ids that are to be excluded from analysis
	
	protected int bgLabel = 0;			// label value for the background
	
	
	public ReportGenerator() throws ALDOperatorException
	{
		
	}
	
	
	/**
	 * 
	 * @return report
	 */
//	public abstract String makeReport();
	
	/**
	 * 
	 * @param values hash table containing object id-value list pairs
	 * @return means of the values for the distinct objects
	 */
	protected Hashtable<Integer, Vector<Double>> getObjectsMeans(Hashtable<Integer, Vector<Double>> values)
	{
		Hashtable<Integer, Vector<Double>> means = new Hashtable<Integer, Vector<Double>>();
		
		Enumeration<Integer> keys = values.keys();
		
		while(keys.hasMoreElements())
		{
			int k = keys.nextElement();
			Vector<Double> v = values.get(k);
			Vector<Double> m = new Vector<Double>();
			
			double sum = 0;
			int n = v.size();
			
			for(int i = 0; i < n; i++)
			{
				sum += v.elementAt(i);
			}
			
			m.add(sum / n);
			
			means.put(k, m);
		}
		
		return means;
	}
	
	
	/**
	 * 
	 * @param values hash table containing object id-value list pairs
	 * @return standard deviations of the values for the distinct objects
	 */
	protected Hashtable<Integer, Vector<Double>> getObjectsStdDevs(Hashtable<Integer, Vector<Double>> values)
	{
		Hashtable<Integer, Vector<Double>> means = getObjectsMeans(values);
		Hashtable<Integer, Vector<Double>> stdDevs = new Hashtable<Integer, Vector<Double>>();
		
		Enumeration<Integer> keys = values.keys();
		
		while(keys.hasMoreElements())
		{
			int k = keys.nextElement();
			Vector<Double> v = values.get(k);
			Vector<Double> m = means.get(k);
			Vector<Double> sd = new Vector<Double>();
			
			double sum = 0;
			int n = v.size();
			
			for(int i = 0; i < n; i++)
			{
				sum += (v.elementAt(i) - m.elementAt(0)) * (v.elementAt(i) - m.elementAt(0));
			}
			
			if(n > 1)
			{
				sd.add(Math.sqrt(sum / (n-1)));
			}
			else
			{
				sd.add(0.0);
			}
			
			
			stdDevs.put(k, sd);
		}
		
		return stdDevs;
	}

	
	/**
	 * 
	 * @param values hash table containing object id-value list pairs
	 * @param minLength minimum length a track must have to be kept
	 * @return trimmed hash table containing only those id-value lists that have at least minTrackLength entries
	 */
	protected Hashtable<Integer, Vector<Double>> trim(Hashtable<Integer, Vector<Double>> values, int minLength)
	{
		Hashtable<Integer, Vector<Double>> trimmedValues = new Hashtable<Integer, Vector<Double>>();
		
		Enumeration<Integer> keys = values.keys();
		
		while(keys.hasMoreElements())
		{
			int k = keys.nextElement();
			Vector<Double> v = values.get(k);
			
			if(v.size() >= minLength)
			{
				trimmedValues.put(k, v);
			}
			
		}
		return trimmedValues;
	}
	
	/**
	 * 
	 * @param meanValues
	 * @return average of the mean values
	 */
	protected double getAverageMean(Hashtable<Integer, Vector<Double>> meanValues)
	{
		double sum = 0;
		double n = 0;
		
		Enumeration<Integer> keys = meanValues.keys();
		
		while(keys.hasMoreElements())
		{
			int k = keys.nextElement();
			Vector<Double> v = meanValues.get(k);
			
			sum += v.elementAt(0);
			n++;
		}
		
		return (sum / n);
	}
	
	
	/**
	 * 
	 * @param meanValues
	 * @return average of the standard deviations
	 */
	protected double getAverageStdDev(Hashtable<Integer, Vector<Double>> meanValues)
	{
		double mean = getAverageMean(meanValues);
		
		double sum = 0;
		double n = 0;
		
		Enumeration<Integer> keys = meanValues.keys();
		
		while(keys.hasMoreElements())
		{
			int k = keys.nextElement();
			Vector<Double> v = meanValues.get(k);
			
			sum += (v.elementAt(0) - mean) * (v.elementAt(0) - mean);
			n++;
		}
		
		if(n > 1)
		{
			return Math.sqrt(sum / (n - 1));
		}
		else
		{
			return 0.0;
		}
	}
	
	
	/**
	 * 
	 * @param table
	 * @param caption heading for the data
	 * @return string representation of the input hash table
	 */
	protected String hashtableToString(Hashtable<Integer, Vector<Double>> table, String caption)
	{
		StringBuffer buffer = new StringBuffer(caption + ":\n\n");
		
		Enumeration<Integer> enumeration = table.keys();
		
		while(enumeration.hasMoreElements())
		{
			int key = enumeration.nextElement();
			buffer.append("object " + key + ":\t" + vectorToString(table.get(key)) + "\n");
		}
		
		buffer.append("\n\n");
		
		return buffer.toString();
	}
	
	
	/**
	 * 
	 * @param table	data to print
	 * @param caption heading for the data
	 * @param offsets number of time points where no data are available for the respective objects	
	 * @return string representation of the input hash table
	 */
	protected String hashtableToString(Hashtable<Integer, Vector<Double>> table, String caption, Hashtable<Integer, Integer> offsets)
	{
		StringBuffer buffer = new StringBuffer(caption + ":\n\n");
		
		Enumeration<Integer> enumeration = table.keys();
		
		while(enumeration.hasMoreElements())
		{
			int key = enumeration.nextElement();
			int offset = offsets.get(key);
			buffer.append("object " + key + ":\t" + vectorToString(offset, table.get(key)) + "\n");
		}
		
		buffer.append("\n\n");
		
		return buffer.toString();
	}
	
	
	/**
	 * 
	 * @param table
	 * @param caption heading for the data
	 * @return string representation of the input hash table
	 */
	protected String pointHashtableToString(Hashtable<Integer, Vector<Point2D.Double>> table, String caption)
	{
		StringBuffer buffer = new StringBuffer(caption + ":\n\n");
		
		Enumeration<Integer> enumeration = table.keys();
		
		while(enumeration.hasMoreElements())
		{
			int key = enumeration.nextElement();
			buffer.append("object " + key + ":\t" + pointVectorToString(table.get(key)) + "\n");
		}
		
		buffer.append("\n\n");
		
		return buffer.toString();
	}
	
	
	/**
	 * 
	 * @param table
	 * @param caption heading for the data
	 * @return string representation of the input hash table
	 */
	protected String pointHashtableToStringX(Hashtable<Integer, Vector<Point2D.Double>> table, String caption)
	{
		StringBuffer buffer = new StringBuffer(caption + ":\n\n");
		
		Enumeration<Integer> enumeration = table.keys();
		
		while(enumeration.hasMoreElements())
		{
			int key = enumeration.nextElement();
			buffer.append("object " + key + ":\t" + pointVectorToStringX(table.get(key)) + "\n");
		}
		
		buffer.append("\n\n");
		
		return buffer.toString();
	}
	
	
	/**
	 * 
	 * @param table
	 * @param caption heading for the data
	 * @return string representation of the input hash table
	 */
	protected String pointHashtableToStringY(Hashtable<Integer, Vector<Point2D.Double>> table, String caption)
	{
		StringBuffer buffer = new StringBuffer(caption + ":\n\n");
		
		Enumeration<Integer> enumeration = table.keys();
		
		while(enumeration.hasMoreElements())
		{
			int key = enumeration.nextElement();
			buffer.append("object " + key + ":\t" + pointVectorToStringY(table.get(key)) + "\n");
		}
		
		buffer.append("\n\n");
		
		return buffer.toString();
	}
	
	
	/**
	 * 
	 * @param values
	 * @return string representation of the input vector
	 */
	protected StringBuffer vectorToString(Vector<Double> values)
	{
		StringBuffer buffer = new StringBuffer();
		int n = values.size();
		
		for(int i = 0; i < n - 1; i++)
		{
			buffer.append(nf.format(values.elementAt(i)) + "\t");
		}
		
		if(n > 0)
		{
			buffer.append(nf.format(values.elementAt(n - 1)));
		}
		
		
		return buffer;
	}
	
	
	/**
	 * 
	 * @param offset
	 * @param values
	 * @return string representation of the input vector
	 */
	protected StringBuffer vectorToString(int offset, Vector<Double> values)
	{
		StringBuffer buffer = new StringBuffer();
		int n = values.size();
		
		for(int i = 0; i < offset; i++)
		{
			buffer.append(naString + "\t");
		}
		
		for(int i = 0; i < n - 1; i++)
		{
			buffer.append(nf.format(values.elementAt(i)) + "\t");
		}
		
		if(n > 0)
		{
			buffer.append(nf.format(values.elementAt(n - 1)));
		}
		
		
		return buffer;
	}
	
	
	/**
	 * 
	 * @param values
	 * @return string representation of the input vector
	 */
	protected StringBuffer pointVectorToString(Vector<Point2D.Double> values)
	{
		StringBuffer buffer = new StringBuffer();
		
		for(int i = 0; i < values.size() - 1; i++)
		{
			//buffer.append(values.elementAt(i).toString() + "\t");
			buffer.append("(" + nf.format(values.elementAt(i).x) + "; " + nf.format(values.elementAt(i).y) + ")\t");
		}
		//buffer.append(values.elementAt(values.size() - 1));
		buffer.append("(" + nf.format(values.elementAt(values.size() - 1).x) + "; " + nf.format(values.elementAt(values.size() - 1).y) + ")");
		
		return buffer;
	}
	
	/**
	 * 
	 * @param values
	 * @return string representation of the x-coordinates of the input vector
	 */
	protected StringBuffer pointVectorToStringX(Vector<Point2D.Double> values)
	{
		StringBuffer buffer = new StringBuffer();
		
		for(int i = 0; i < values.size() - 1; i++)
		{
			buffer.append(nf.format(values.elementAt(i).x) + "\t");
		}

		buffer.append(nf.format(values.elementAt(values.size() - 1).x));
		
		return buffer;
	}
	
	/**
	 * 
	 * @param values
	 * @return string representation of the y-coordinates of the input vector
	 */
	protected StringBuffer pointVectorToStringY(Vector<Point2D.Double> values)
	{
		StringBuffer buffer = new StringBuffer();
		
		for(int i = 0; i < values.size() - 1; i++)
		{
			buffer.append(nf.format(values.elementAt(i).y) + "\t");
		}

		buffer.append(nf.format(values.elementAt(values.size() - 1).y));
		
		return buffer;
	}
	
	
	/**
	 * 
	 * @param num input number
	 * @return formatted String representation of the input number
	 */
	protected String numberToString(double num)
	{	
		return nf.format(num);
	}
	
	
	/**
	 * 
	 * @return String containing information about parameter settings used
	 */
	protected String getSettings()
	{
		DecimalFormatSymbols dfs = new DecimalFormatSymbols();
		
		StringBuffer sb = new StringBuffer();
		sb.append("settings:\n\n");
		sb.append("pixel length in x-direction:\t" + nf.format(deltaX) + "\t" + unitSpace +"\n");
		sb.append("pixel length in y-direction:\t" + nf.format(deltaY) + "\t" + unitSpace + "\n");
		sb.append("time between frames:\t" + nf.format(deltaT) + "\t" + unitTime + "\n");
		sb.append("minimum track length:\t" + minTrackLength + "\n");
		sb.append("decimal separator:\t" + dfs.getDecimalSeparator() + "\n");
		
		return sb.toString();
	}
	
	
	protected String getExcluded()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("excluded objects: \n\n");
		
		if(exclude.size() > 0)
		{
			for(int i = 0; i < exclude.size()-1; i++)
			{
				sb.append(exclude.elementAt(i) + ", ");
			}
			
			sb.append(exclude.elementAt(exclude.size()-1));
		}
		
		return sb.toString();
	}
	
	/**
	 * set the time between two frames
	 * @param dt
	 */
	public void setDeltaT(double dt)
	{
		this.deltaT = dt;
	}
	
	
	/**
	 * set pixel length in x-direction
	 * @param dx
	 */
	public void setDeltaX(double dx)
	{
		this.deltaX = dx;
	}
	
	
	/**
	 * set pixel length in y-direction
	 * @param dy
	 */
	public void setDeltaY(double dy)
	{
		this.deltaY = dy;
	}
	
	
	/**
	 * set unit of time
	 * @param unitT
	 */
	public void setUnitTime(String unitT)
	{
		this.unitTime = unitT;
	}
	
	
	/**
	 * set unit of space
	 * @param unitXY
	 */
	public void setUnitSpace(String unitXY)
	{
		this.unitSpace = unitXY;
	}
	
	
	/**
	 * set unit of angles
	 * @param unitAngle
	 */
	public void setUnitAngle(String unitAngle)
	{
		this.unitAngle = unitAngle;
	}
	
	
	/**
	 * set minimum track length
	 * @param minTrackLength
	 */
	public void setMinTrackLength(int minTrackLength)
	{
		this.minTrackLength = minTrackLength;
	}
	
	public void setExcluded(Vector<Integer> exclude)
	{
		this.exclude = exclude;
	}
	
	
	public String getReport()
	{
		return this.report;
	}
}
