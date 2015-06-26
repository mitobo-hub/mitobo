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
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.segmentation.regions.labeling.LabelAreasToRegions;

public class IntensityAnalyzer extends ReportGenerator
{
	@Parameter(label = "(fluorescence) intensity image", required = true, direction = Parameter.Direction.IN, supplemental = false, description = "(fluorescence) intensity image", dataIOOrder = 0)
	private transient MTBImage intensityImg = null;
	
	@Parameter(label = "label image", required = true, direction = Parameter.Direction.IN, supplemental = false, description = "label image", dataIOOrder = 0)
	private transient MTBImage labelImg;
	
	private int sizeX;
	private int sizeY;
	private int sizeT;
	
	
	public IntensityAnalyzer(MTBImage intensityImg, MTBImage labelImg) throws ALDOperatorException
	{

		this.intensityImg = intensityImg;
		this.labelImg = labelImg;
	}
	
	
	@Override
	public void operate() throws ALDOperatorException, ALDProcessingDAGException
	{
		nf.setMaximumFractionDigits(4);
		nf.setMinimumFractionDigits(4);
		nf.setGroupingUsed(false);
		
		sizeX = intensityImg.getSizeX();
		sizeY = intensityImg.getSizeY();
		sizeT = intensityImg.getSizeT();
		
		// point measures
		Hashtable<Integer, Vector<Double>> intMeans = getIntensityMeans();
		Hashtable<Integer, Vector<Double>> intSDs = getIntensityStdDevs(intMeans);
//		intMeans = trim(intMeans, minTrackLength);
//		intSDs = trim(intSDs, minTrackLength);
		Hashtable<Integer, Vector<Double>> bgMeans = getMeanBackgroundIntensity();
		Hashtable<Integer, Vector<Double>> bgSDs = getStdDevBackgroundIntensity(bgMeans);
		
		// single object statistics
		Hashtable<Integer, Vector<Double>> intObjectMeans = getObjectsMeans(intMeans);
		Hashtable<Integer, Vector<Double>> intBGMeans = getObjectsMeans(bgMeans);
		
		// population statistics
		double averageMeanIntensity = getAverageMean(intObjectMeans);
		
		// generate report
		// heading and parameter values
		StringBuffer buffer = new StringBuffer("Intensity Report \n\n");
		buffer.append(getSettings());
		buffer.append("\n\n");
		
		// population statistics
		buffer.append("population statistics\n\n");
		buffer.append("objects analyzed: " + intObjectMeans.size() + "\n");
		buffer.append("average mean object intensity\t" + numberToString(averageMeanIntensity) + "\n");
		buffer.append("\n\n");
		
		// single object statistics
		buffer.append("single object statistics\n\n");
		buffer.append(hashtableToString(intObjectMeans, "intensity means per object"));
		buffer.append(hashtableToString(intBGMeans, "mean intensity of background"));
		buffer.append("\n\n");
		
		// point measures
		buffer.append("point measures\n\n");
		buffer.append(hashtableToString(intMeans, "object intensity means"));
		buffer.append(hashtableToString(bgMeans, "background intensity means"));
		buffer.append(hashtableToString(intSDs, "object intensity standard deviations"));
		buffer.append(hashtableToString(bgSDs, "background intensity standard deviation"));
		
		buffer.append(getExcluded());
		
		this.report = buffer.toString();
	}
	
//	public String makeReport()
//	{
//		// point measures
//		Hashtable<Integer, Vector<Double>> intMeans = getIntensityMeans();
//		Hashtable<Integer, Vector<Double>> intSDs = getIntensityStdDevs(intMeans);
////		intMeans = trim(intMeans, minTrackLength);
////		intSDs = trim(intSDs, minTrackLength);
//		Hashtable<Integer, Vector<Double>> bgMeans = getMeanBackgroundIntensity();
//		Hashtable<Integer, Vector<Double>> bgSDs = getStdDevBackgroundIntensity(bgMeans);
//		
//		// single object statistics
//		Hashtable<Integer, Vector<Double>> intObjectMeans = getObjectsMeans(intMeans);
//		Hashtable<Integer, Vector<Double>> intBGMeans = getObjectsMeans(bgMeans);
//		
//		// population statistics
//		double averageMeanIntensity = getAverageMean(intObjectMeans);
//		
//		// generate report
//		// heading and parameter values
//		StringBuffer report = new StringBuffer("Intensity Report \n\n");
//		report.append(getSettings());
//		report.append("\n\n");
//		
//		// population statistics
//		report.append("population statistics\n\n");
//		report.append("objects analyzed: " + intObjectMeans.size() + "\n");
//		report.append("average mean object intensity\t" + numberToString(averageMeanIntensity) + "\n");
//		report.append("\n\n");
//		
//		// single object statistics
//		report.append("single object statistics\n\n");
//		report.append(hashtableToString(intObjectMeans, "intensity means per object"));
//		report.append(hashtableToString(intBGMeans, "mean intensity of background"));
//		report.append("\n\n");
//		
//		// point measures
//		report.append("point measures\n\n");
//		report.append(hashtableToString(intMeans, "object intensity means"));
//		report.append(hashtableToString(bgMeans, "background intensity means"));
//		report.append(hashtableToString(intSDs, "object intensity standard deviations"));
//		report.append(hashtableToString(bgSDs, "background intensity standard deviation"));
//		
//		report.append(getExcluded());
//		
//		return report.toString();
//	}

	
	/**
	 * 
	 * @param intensityImg
	 * @return mean intensities of each object in every frame <br/>
	 * key: object-id, value: list of mean intensities
	 */
	private Hashtable<Integer, Vector<Double>> getIntensityMeans()
	{
		Hashtable<Integer, Vector<Double>> intensityMeans = new Hashtable<Integer, Vector<Double>>();
		
		// check if dimensions of intensity and label image are identical
		if(intensityImg.getSizeT() == sizeT && intensityImg.getSizeX() == sizeX && intensityImg.getSizeY() == sizeY)
		{
			for(int t = 0; t < sizeT; t++)	
			{
				MTBImage labelFrame = labelImg.getImagePart(0, 0, 0, t, 0, sizeX, sizeY, 1, 1, 1);
				MTBRegion2DSet currentRegions = LabelAreasToRegions.getRegions(labelFrame, bgLabel);
				
				MTBImage intensityFrame = intensityImg.getImagePart(0, 0, 0, t, 0, sizeX, sizeY, 1, 1, 1);
					
				for(int j = 0; j < currentRegions.size(); j++)
				{
					MTBRegion2D cr = currentRegions.elementAt(j);
					int cid = cr.getID();
					
					if(!exclude.contains(cid))
					{
						Vector<Point2D.Double> points = cr.getPoints();
						
						double mean = 0;
						int n = points.size();
						
						for(int k = 0; k < n; k++)
						{
							Point2D.Double p = points.elementAt(k);
							
							mean += intensityFrame.getValueDouble((int)p.x, (int)p.y);
						}
						
						mean /= n;
						
						if(intensityMeans.containsKey(cid))
						{
							intensityMeans.get(cid).add(mean);
						}
						else
						{
							Vector<Double> intMeans = new Vector<Double>();
							intMeans.add(mean);
							intensityMeans.put(cid, intMeans);
						}
					}
					
				}
			}
		}
		
		return intensityMeans;
	}
	
	
	/**
	 * 
	 * @param intensityMeans mean intensities of each object in every frame
	 * @return standard deviation of intensities of each object in every frame <br/>
	 * key: object-id, value: list of standard deviations
	 */
	private Hashtable<Integer, Vector<Double>> getIntensityStdDevs(Hashtable<Integer, Vector<Double>> intensityMeans)
	{
		Hashtable<Integer, Vector<Double>> intensitySDs = new Hashtable<Integer, Vector<Double>>();
		
		Hashtable<Integer, Integer> counter = new Hashtable<Integer, Integer>();	// used to count available frames for the region means as they are not mapped to real time points
		
		Enumeration<Integer> keys = intensityMeans.keys();
		
		while(keys.hasMoreElements())
		{
			counter.put(keys.nextElement(), -1);
		}
		
		// check if dimensions of intensity and label image are identical
		if(intensityImg.getSizeT() == sizeT && intensityImg.getSizeX() == sizeX && intensityImg.getSizeY() == sizeY)
		{
			for(int t = 0; t < sizeT; t++)	
			{
				MTBImage labelFrame = labelImg.getImagePart(0, 0, 0, t, 0, sizeX, sizeY, 1, 1, 1);
				MTBRegion2DSet currentRegions = LabelAreasToRegions.getRegions(labelFrame, bgLabel);
				
				MTBImage intensityFrame = intensityImg.getImagePart(0, 0, 0, t, 0, sizeX, sizeY, 1, 1, 1);
					
				for(int j = 0; j < currentRegions.size(); j++)
				{
					MTBRegion2D cr = currentRegions.elementAt(j);
					int cid = cr.getID();
					
					if(!exclude.contains(cid))
					{
						Vector<Point2D.Double> points = cr.getPoints();
						
						counter.put(cid, counter.get(cid) + 1);
						double mean = intensityMeans.get(cid).elementAt(counter.get(cid));
						
						double sd = 0;
						int n = points.size();
						
						for(int k = 0; k < n; k++)
						{
							Point2D.Double p = points.elementAt(k);
							
							sd += (intensityFrame.getValueDouble((int)p.x, (int)p.y) - mean) * (intensityFrame.getValueDouble((int)p.x, (int)p.y) - mean);
						}
						
						if(n > 1)
						{
							sd = Math.sqrt(sd / (n - 1));
						}
						else
						{
							sd = 0;
						}
						
						//System.out.println("frame: " + t + ", object: " + cid + ", mean: " + mean + ", sd: " + sd);
						
						if(intensitySDs.containsKey(cid))
						{
							intensitySDs.get(cid).add(sd);
						}
						else
						{
							Vector<Double> intMeans = new Vector<Double>();
							intMeans.add(sd);
							intensitySDs.put(cid, intMeans);
						}
					}
					
				}
			}
		}
		
		return intensitySDs;
	}
	
	
	/**
	 * 
	 * @return mean background intensity for every frame<br/>
	 * key: object-id (= background label), value: list of mean intensities
	 */
	private Hashtable<Integer, Vector<Double>> getMeanBackgroundIntensity()
	{
		Hashtable<Integer, Vector<Double>> meanIntensity = new Hashtable<Integer, Vector<Double>>();
		
		meanIntensity.put(bgLabel, new Vector<Double>());
		
		// check if dimensions of intensity and label image are identical
		if(intensityImg.getSizeT() == sizeT && intensityImg.getSizeX() == sizeX && intensityImg.getSizeY() == sizeY)
		{
			for(int t = 0; t < sizeT; t++)	
			{
				MTBImage labelFrame = labelImg.getImagePart(0, 0, 0, t, 0, sizeX, sizeY, 1, 1, 1);
				
				MTBImage intensityFrame = intensityImg.getImagePart(0, 0, 0, t, 0, sizeX, sizeY, 1, 1, 1);
				
				MTBRegion2D background = LabelAreasToRegions.getBackground(labelFrame, bgLabel);
				
				Vector<Point2D.Double> points = background.getPoints();
				
				double mean = 0;
				int n = points.size();
				
				for(int k = 0; k < n; k++)
				{
					Point2D.Double p = points.elementAt(k);
					
					mean += intensityFrame.getValueDouble((int)p.x, (int)p.y);
				}
				
				mean /= n;
				
				meanIntensity.get(bgLabel).add(mean);
			}
		}
		
		return meanIntensity;
	}
	
	
	/**
	 * 
	 * @param meanBGIntensity mean intensities of background in every frame
	 * @return standard deviation of background intensity in every frame<br/>
	 * key: object-id (= background label), value: list of standard deviations
	 */
	private Hashtable<Integer, Vector<Double>> getStdDevBackgroundIntensity(Hashtable<Integer, Vector<Double>> meanBGIntensity)
	{
		Hashtable<Integer, Vector<Double>> sdIntensity = new Hashtable<Integer, Vector<Double>>();
		
		sdIntensity.put(bgLabel, new Vector<Double>());
		
		// check if dimensions of intensity and label image are identical
		if(intensityImg.getSizeT() == sizeT && intensityImg.getSizeX() == sizeX && intensityImg.getSizeY() == sizeY)
		{
			for(int t = 0; t < sizeT; t++)	
			{
				MTBImage labelFrame = labelImg.getImagePart(0, 0, 0, t, 0, sizeX, sizeY, 1, 1, 1);
				
				MTBImage intensityFrame = intensityImg.getImagePart(0, 0, 0, t, 0, sizeX, sizeY, 1, 1, 1);
				
				MTBRegion2D background = LabelAreasToRegions.getBackground(labelFrame, bgLabel);
				
				Vector<Point2D.Double> points = background.getPoints();
				
				double mean = meanBGIntensity.get(bgLabel).elementAt(t);
				double sd = 0;
				int n = points.size();
				
				for(int k = 0; k < n; k++)
				{
					Point2D.Double p = points.elementAt(k);
					
					sd += (intensityFrame.getValueDouble((int)p.x, (int)p.y) - mean) * (intensityFrame.getValueDouble((int)p.x, (int)p.y) - mean);
				}
				
				sd = Math.sqrt(sd / (n - 1));
				
				sdIntensity.get(bgLabel).add(sd);
			}
		}
		
		return sdIntensity;
	}
	
}
