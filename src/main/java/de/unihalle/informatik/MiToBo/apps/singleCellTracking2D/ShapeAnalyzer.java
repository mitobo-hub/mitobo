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

public class ShapeAnalyzer extends ReportGenerator
{
	@Parameter(label = "label image", required = true, direction = Parameter.Direction.IN, supplemental = false, description = "label image", dataIOOrder = 0)
	private transient MTBImage labelImg;	// labeled input image stack, corresponding objects in different frames must be labeled with the same value
	
	private int sizeT;			// number of frames
	private int sizeX;			// width of the frames
	private int sizeY;			// height of the frames
		
	Hashtable<Integer, Vector<Double>> areas;
	Hashtable<Integer, Vector<Double>> perimeters;
	Hashtable<Integer, Vector<Double>> circularities;
	Hashtable<Integer, Vector<Double>> eccentricities;
	Hashtable<Integer, Vector<Double>> lengths;
	Hashtable<Integer, Vector<Double>> widths;
	
	/**
	 * constructor
	 * 
	 * @param labelImg input label image
	 */
	public ShapeAnalyzer(MTBImage labelImg) throws ALDOperatorException
	{
		this.labelImg = labelImg;
	}
	
	
	@Override
	protected void operate() throws ALDOperatorException, ALDProcessingDAGException
	{
		nf.setMaximumFractionDigits(4);
		nf.setMinimumFractionDigits(4);
		nf.setGroupingUsed(false);
		
		this.sizeT = labelImg.getSizeT();
		this.sizeX = labelImg.getSizeX();
		this.sizeY = labelImg.getSizeY();
		
		// point measures
		getShapeFeatures();
		
		Hashtable<Integer, Vector<Double>> areaFractions = getAreaFractions(areas);
		Hashtable<Integer, Vector<Double>> overlaps = getOverlaps();
		overlaps = trim(overlaps, minTrackLength - 1);
		
		// single object statistics
		Hashtable<Integer, Vector<Double>> meanAreas = getObjectsMeans(areas);
		Hashtable<Integer, Vector<Double>> meanOverlaps = getObjectsMeans(overlaps);
		Hashtable<Integer, Vector<Double>> meanPerimeters = getObjectsMeans(perimeters);
		Hashtable<Integer, Vector<Double>> meanCircularities = getObjectsMeans(circularities);
		Hashtable<Integer, Vector<Double>> meanEccentricities = getObjectsMeans(eccentricities);
		Hashtable<Integer, Vector<Double>> meanLengths = getObjectsMeans(lengths);
		Hashtable<Integer, Vector<Double>> meanWidths = getObjectsMeans(widths);
		
		// population statistics
		double averageMeanArea = getAverageMean(meanAreas);
		double averageMeanOverlap = getAverageMean(meanOverlaps);
		double averageMeanPerimeter = getAverageMean(meanPerimeters);
		double averageMeanCircularity = getAverageMean(meanCircularities);
		double averageMeanEccentricity = getAverageMean(meanEccentricities);
		double averageMeanLength = getAverageMean(meanLengths);
		double averageMeanWidth = getAverageMean(meanWidths);

		// generate report
		// heading and parameter values
		StringBuffer buffer = new StringBuffer("Shape Report \n\n");
		buffer.append(getSettings());
		buffer.append("\n\n");
		
		// population statistics
		buffer.append("population statistics\n\n");
		buffer.append("objects analyzed: " + areas.size() + "\n");
		buffer.append("average mean area:\t" + numberToString(averageMeanArea) + "\t" + unitSpace + "^2\n");
		buffer.append("average mean overlap between frames:\t" + numberToString(averageMeanOverlap) + "\n");
		buffer.append("average mean perimeter:\t" + numberToString(averageMeanPerimeter) + "\t" + unitSpace + "\n");
		buffer.append("average mean length:\t" + numberToString(averageMeanLength) + "\t" + unitSpace + "\n");
		buffer.append("average mean width:\t" + numberToString(averageMeanWidth) + "\t" + unitSpace + "\n");
		buffer.append("average mean circularity:\t" + numberToString(averageMeanCircularity) + "\n");
		buffer.append("average mean eccentricity:\t" + numberToString(averageMeanEccentricity) + "\n");
		buffer.append("\n\n");
		
		// single object statistics
		buffer.append("single object statistics\n\n");
		buffer.append(hashtableToString(meanAreas, "mean areas (" + unitSpace + "^2)"));
		buffer.append(hashtableToString(meanOverlaps, "mean overlaps"));
		buffer.append(hashtableToString(meanPerimeters, "mean perimeters (" + unitSpace + ")"));
		buffer.append(hashtableToString(meanLengths, "mean lengths (" + unitSpace + ")"));
		buffer.append(hashtableToString(meanWidths, "mean widths (" + unitSpace + ")"));
		buffer.append(hashtableToString(meanCircularities, "mean circularities"));
		buffer.append(hashtableToString(meanEccentricities, "mean eccentricities"));
		buffer.append("\n\n");
		
		// point measures
		buffer.append("point measures\n\n");
		buffer.append(hashtableToString(areas, "areas (" + unitSpace + "^2)"));
		buffer.append(hashtableToString(areaFractions, "area fractions"));
		buffer.append(hashtableToString(overlaps, "overlaps"));
		buffer.append(hashtableToString(perimeters, "perimeters (" + unitSpace + ")"));
		buffer.append(hashtableToString(lengths, "lengths (" + unitSpace + ")"));
		buffer.append(hashtableToString(widths, "widths (" + unitSpace + ")"));
		buffer.append(hashtableToString(circularities, "circularities"));
		buffer.append(hashtableToString(eccentricities, "eccentricities"));
		
		buffer.append(getExcluded());
		
		this.report = buffer.toString();
	}
	
	
//	public String makeReport()
//	{
//		// point measures
//		getShapeFeatures();
////		areas = trim(areas, minTrackLength);
////		perimeters = trim(perimeters, minTrackLength);
////		circularities = trim(circularities, minTrackLength);
////		eccentricities = trim(eccentricities, minTrackLength);
////		lengths = trim(lengths, minTrackLength);
////		widths = trim(widths, minTrackLength);
//		
//		Hashtable<Integer, Vector<Double>> areaFractions = getAreaFractions(areas);
//		Hashtable<Integer, Vector<Double>> overlaps = getOverlaps();
//		overlaps = trim(overlaps, minTrackLength - 1);
//		
//		// single object statistics
//		Hashtable<Integer, Vector<Double>> meanAreas = getObjectsMeans(areas);
//		Hashtable<Integer, Vector<Double>> meanOverlaps = getObjectsMeans(overlaps);
//		Hashtable<Integer, Vector<Double>> meanPerimeters = getObjectsMeans(perimeters);
//		Hashtable<Integer, Vector<Double>> meanCircularities = getObjectsMeans(circularities);
//		Hashtable<Integer, Vector<Double>> meanEccentricities = getObjectsMeans(eccentricities);
//		Hashtable<Integer, Vector<Double>> meanLengths = getObjectsMeans(lengths);
//		Hashtable<Integer, Vector<Double>> meanWidths = getObjectsMeans(widths);
//		
//		// population statistics
//		double averageMeanArea = getAverageMean(meanAreas);
//		double averageMeanOverlap = getAverageMean(meanOverlaps);
//		double averageMeanPerimeter = getAverageMean(meanPerimeters);
//		double averageMeanCircularity = getAverageMean(meanCircularities);
//		double averageMeanEccentricity = getAverageMean(meanEccentricities);
//		double averageMeanLength = getAverageMean(meanLengths);
//		double averageMeanWidth = getAverageMean(meanWidths);
//
//		// generate report
//		// heading and parameter values
//		StringBuffer report = new StringBuffer("Shape Report \n\n");
//		report.append(getSettings());
//		report.append("\n\n");
//		
//		// population statistics
//		report.append("population statistics\n\n");
//		report.append("objects analyzed: " + areas.size() + "\n");
//		report.append("average mean area:\t" + numberToString(averageMeanArea) + "\t" + unitXY + "^2\n");
//		report.append("average mean overlap between frames:\t" + numberToString(averageMeanOverlap) + "\n");
//		report.append("average mean perimeter:\t" + numberToString(averageMeanPerimeter) + "\t" + unitXY + "\n");
//		report.append("average mean length:\t" + numberToString(averageMeanLength) + "\t" + unitXY + "\n");
//		report.append("average mean width:\t" + numberToString(averageMeanWidth) + "\t" + unitXY + "\n");
//		report.append("average mean circularity:\t" + numberToString(averageMeanCircularity) + "\n");
//		report.append("average mean eccentricity:\t" + numberToString(averageMeanEccentricity) + "\n");
//		report.append("\n\n");
//		
//		// single object statistics
//		report.append("single object statistics\n\n");
//		report.append(hashtableToString(meanAreas, "mean areas (" + unitXY + "^2)"));
//		report.append(hashtableToString(meanOverlaps, "mean overlaps"));
//		report.append(hashtableToString(meanPerimeters, "mean perimeters (" + unitXY + ")"));
//		report.append(hashtableToString(meanLengths, "mean lengths (" + unitXY + ")"));
//		report.append(hashtableToString(meanWidths, "mean widths (" + unitXY + ")"));
//		report.append(hashtableToString(meanCircularities, "mean circularities"));
//		report.append(hashtableToString(meanEccentricities, "mean eccentricities"));
//		report.append("\n\n");
//		
//		// point measures
//		report.append("point measures\n\n");
//		report.append(hashtableToString(areas, "areas (" + unitXY + "^2)"));
//		report.append(hashtableToString(areaFractions, "area fractions"));
//		report.append(hashtableToString(overlaps, "overlaps"));
//		report.append(hashtableToString(perimeters, "perimeters (" + unitXY + ")"));
//		report.append(hashtableToString(lengths, "lengths (" + unitXY + ")"));
//		report.append(hashtableToString(widths, "widths (" + unitXY + ")"));
//		report.append(hashtableToString(circularities, "circularities"));
//		report.append(hashtableToString(eccentricities, "eccentricities"));
//		
//		report.append(getExcluded());
//		
//		return report.toString();
//	}
	
	
	/**
	 * calculate several shape features in one method
	 */
	private void getShapeFeatures()
	{
		double factor = deltaX * deltaY;
		
		areas = new Hashtable<Integer, Vector<Double>>();
		perimeters = new Hashtable<Integer, Vector<Double>>();
		circularities = new Hashtable<Integer, Vector<Double>>();
		eccentricities = new Hashtable<Integer, Vector<Double>>();
		lengths = new Hashtable<Integer, Vector<Double>>();
		widths = new Hashtable<Integer, Vector<Double>>();
		
		for(int i = 0; i < sizeT; i++)	
		{
			MTBImage frame = labelImg.getImagePart(0, 0, 0, i, 0, sizeX, sizeY, 1, 1, 1);
			MTBRegion2DSet currentRegions = LabelAreasToRegions.getRegions(frame, bgLabel);
				
			for(int j = 0; j < currentRegions.size(); j++)
			{
				MTBRegion2D cr = currentRegions.elementAt(j);
				int cid = cr.getID();
				
				if(!exclude.contains(cid))
				{
					// area
					double a = cr.getArea() * factor;
					
					if(areas.containsKey(cid))
					{
						areas.get(cid).add(a);
					}
					else
					{
						Vector<Double> values = new Vector<Double>();
						values.add(a);
						areas.put(cid, values);
					}
					
					// perimeter
					double p = 0;
					
					try
					{
						p = cr.getContour().getContourLength() * deltaX;
					} 
					catch(ALDOperatorException e)
					{
						e.printStackTrace();
					} 
					catch(ALDProcessingDAGException e)
					{
						e.printStackTrace();
					}
					
					if(perimeters.containsKey(cid))
					{
						perimeters.get(cid).add(p);
					}
					else
					{
						Vector<Double> values = new Vector<Double>();
						values.add(p);
						perimeters.put(cid, values);
					}
					
					// circularity
					double c = 0;
					
					try
					{
						c = cr.getCircularity();
					} 
					catch(ALDOperatorException e)
					{
						e.printStackTrace();
					} 
					catch(ALDProcessingDAGException e)
					{
						e.printStackTrace();
					}
					
					if(circularities.containsKey(cid))
					{
						circularities.get(cid).add(c);
					}
					else
					{
						Vector<Double> values = new Vector<Double>();
						values.add(c);
						circularities.put(cid, values);
					}
					
					// eccentricity
					double e = 0;
					
					e = cr.getEccentricity();
					
					if(eccentricities.containsKey(cid))
					{
						eccentricities.get(cid).add(e);
					}
					else
					{
						Vector<Double> values = new Vector<Double>();
						values.add(e);
						eccentricities.put(cid, values);
					}
					
					// length
					double l = 0;
					
					l = cr.getMajorAxisLength() * deltaX;
					
					if(lengths.containsKey(cid))
					{
						lengths.get(cid).add(l);
					}
					else
					{
						Vector<Double> values = new Vector<Double>();
						values.add(l);
						lengths.put(cid, values);
					}
					
					// avg. width
//					double w = a / l;
					double w = cr.getMinorAxisLength() * deltaX;
					
					if(widths.containsKey(cid))
					{
						widths.get(cid).add(w);
					}
					else
					{
						Vector<Double> values = new Vector<Double>();
						values.add(w);
						widths.put(cid, values);
					}
				}
			}
		}
	}
	
//	/**
//	 * 
//	 * @return key: areas of the objects in the distinct frames <br/>
//	 * key: object-id, value: list of areas
//	 */
//	private Hashtable<Integer, Vector<Double>> getAreas()
//	{
//		double factor = deltaX * deltaY;
//		
//		Hashtable<Integer, Vector<Double>> areas = new Hashtable<Integer, Vector<Double>>();
//		
//		for(int i = 0; i < sizeT; i++)	
//		{
//			MTBImage frame = labelImg.getImagePart(0, 0, 0, i, 0, sizeX, sizeY, 1, 1, 1);
//			MTBRegion2DSet currentRegions = LabelAreasToRegions.getRegions(frame, bgLabel);
//				
//			for(int j = 0; j < currentRegions.size(); j++)
//			{
//				MTBRegion2D cr = currentRegions.elementAt(j);
//				int cid = cr.getID();
//				
//				if(!exclude.contains(cid))
//				{
//					double a = cr.getArea() * factor;
//					
//					if(areas.containsKey(cid))
//					{
//						areas.get(cid).add(a);
//					}
//					else
//					{
//						Vector<Double> values = new Vector<Double>();
//						values.add(a);
//						areas.put(cid, values);
//					}
//				}
//			}
//		}
//		
//		return areas;
//	}
	
	
	/**
	 * 
	 * @param areas areas of the objects in the distinct frames
	 * @return key: areas fractions of the objects from one frame to the next <br/>
	 * key: object-id, value: list of areas fractions
	 */
	private Hashtable<Integer, Vector<Double>> getAreaFractions(Hashtable<Integer, Vector<Double>> areas)
	{
		Hashtable<Integer, Vector<Double>> areaFractions = new Hashtable<Integer, Vector<Double>>();
		
		Enumeration<Integer> keys = areas.keys();
		
		while(keys.hasMoreElements())
		{
			int k = keys.nextElement();
			Vector<Double> d = areas.get(k);
			Vector<Double> v = new Vector<Double>();
			
			for(int i = 1; i < d.size(); i++)
			{
				v.add((d.elementAt(i)  / d.elementAt(i-1)));
			}
			
			areaFractions.put(k, v);
		}
		
		return areaFractions;
	}
	
	
	/**
	 * 
	 * @return key: overlap of the object pixels in the distinct frames <br/>
	 * key: object-id, value: list of overlaps
	 */
	private Hashtable<Integer, Vector<Double>> getOverlaps()
	{
		Hashtable<Integer, Vector<Double>> overlaps = new Hashtable<Integer, Vector<Double>>();
		
		for(int i = 0; i < sizeT-1; i++)	
		{
			MTBImage currFrame = labelImg.getImagePart(0, 0, 0, i, 0, sizeX, sizeY, 1, 1, 1);
			MTBImage nextFrame = labelImg.getImagePart(0, 0, 0, i+1, 0, sizeX, sizeY, 1, 1, 1);
			
			MTBRegion2DSet currRegions = LabelAreasToRegions.getRegions(currFrame, bgLabel);
			MTBRegion2DSet nextRegions = LabelAreasToRegions.getRegions(nextFrame, bgLabel);
				
			for(int j = 0; j < currRegions.size(); j++)
			{
				MTBRegion2D cr = currRegions.elementAt(j);
				int cid = cr.getID();
				
				if(!exclude.contains(cid))
				{
					Vector<Point2D.Double> currPoints = cr.getPoints();
					boolean found = false;
					
					double ol = 0;
					
					for(int k = 0; k < nextRegions.size(); k++)
					{
						MTBRegion2D nr = nextRegions.elementAt(k);
						int nid = nr.getID();
						
						if(nid == cid)
						{
							Vector<Point2D.Double> nextPoints = nr.getPoints();
							
							for(int l = 0; l < currPoints.size(); l++)
							{
								Point2D.Double cp = currPoints.elementAt(l);
								
								for(int m = 0; m < nextPoints.size(); m++)
								{
									Point2D.Double np = nextPoints.elementAt(m);
									
									if(np.equals(cp))
									{
										ol++;
										
										break;
									}	
									
								}
							}
							
							ol /= currPoints.size();
							
							found = true;
							
							break;
						}
					}
					
					if(!found)	// current region is not present in the next frame
					{
						//ol = -1;
						continue;
					}
					
					if(overlaps.containsKey(cid))
					{
						overlaps.get(cid).add(ol);
					}
					else
					{
						Vector<Double> values = new Vector<Double>();
						values.add(ol);
						overlaps.put(cid, values);
					}
				}

			}
		}
		
		return overlaps;
	}

	
	
//	/**
//	 * calculates perimeters of the objects<br/>
//	 * assumes the pixels to be square, i.e. deltaX == deltaY
//	 * 
//	 * @return perimeter of each object in every frame <br/>
//	 * key: object-id, value: list of perimeters
//	 */
//	private Hashtable<Integer, Vector<Double>> getPerimeters()
//	{
//		Hashtable<Integer, Vector<Double>> perimeters = new Hashtable<Integer, Vector<Double>>();
//		
//		for(int i = 0; i < sizeT; i++)	
//		{
//			MTBImage frame = labelImg.getImagePart(0, 0, 0, i, 0, sizeX, sizeY, 1, 1, 1);
//			MTBRegion2DSet currentRegions = LabelAreasToRegions.getRegions(frame, bgLabel);
//				
//			for(int j = 0; j < currentRegions.size(); j++)
//			{
//				MTBRegion2D cr = currentRegions.elementAt(j);
//				int cid = cr.getID();
//				
//				if(!exclude.contains(cid))
//				{
//					double p = 0;
//					
//					try
//					{
//						p = cr.getContour().getContourLength() * deltaX;
//					} 
//					catch(ALDOperatorException e)
//					{
//						e.printStackTrace();
//					} 
//					catch(ALDProcessingDAGException e)
//					{
//						e.printStackTrace();
//					}
//					
//					if(perimeters.containsKey(cid))
//					{
//						perimeters.get(cid).add(p);
//					}
//					else
//					{
//						Vector<Double> values = new Vector<Double>();
//						values.add(p);
//						perimeters.put(cid, values);
//					}
//				}
//			}
//		}
//		
//		
//		return perimeters;
//	}
	
	
//	/**
//	 * 
//	 * @return circularity of each object in every frame <br/>
//	 * key: object-id, value: list of circularities
//	 */
//	private Hashtable<Integer, Vector<Double>> getCircularities()
//	{
//		Hashtable<Integer, Vector<Double>> circularities = new Hashtable<Integer, Vector<Double>>();
//		
//		for(int i = 0; i < sizeT; i++)	
//		{
//			MTBImage frame = labelImg.getImagePart(0, 0, 0, i, 0, sizeX, sizeY, 1, 1, 1);
//			MTBRegion2DSet currentRegions = LabelAreasToRegions.getRegions(frame, bgLabel);
//				
//			for(int j = 0; j < currentRegions.size(); j++)
//			{
//				MTBRegion2D cr = currentRegions.elementAt(j);
//				int cid = cr.getID();
//				
//				if(!exclude.contains(cid))
//				{
//					double c = 0;
//					
//					try
//					{
//						c = cr.getCircularity();
//					} 
//					catch(ALDOperatorException e)
//					{
//						e.printStackTrace();
//					} 
//					catch(ALDProcessingDAGException e)
//					{
//						e.printStackTrace();
//					}
//					
//					if(circularities.containsKey(cid))
//					{
//						circularities.get(cid).add(c);
//					}
//					else
//					{
//						Vector<Double> values = new Vector<Double>();
//						values.add(c);
//						circularities.put(cid, values);
//					}
//				}
//			}
//		}
//		
//		
//		return circularities;
//	}
	
	
//	/**
//	 * 
//	 * @return eccentricity of each object in every frame <br/>
//	 * key: object-id, value: list of eccentricities
//	 */
//	private Hashtable<Integer, Vector<Double>> getEccentricities()
//	{
//		Hashtable<Integer, Vector<Double>> eccentricities = new Hashtable<Integer, Vector<Double>>();
//		
//		for(int i = 0; i < sizeT; i++)	
//		{
//			MTBImage frame = labelImg.getImagePart(0, 0, 0, i, 0, sizeX, sizeY, 1, 1, 1);
//			MTBRegion2DSet currentRegions = LabelAreasToRegions.getRegions(frame, bgLabel);
//				
//			for(int j = 0; j < currentRegions.size(); j++)
//			{
//				MTBRegion2D cr = currentRegions.elementAt(j);
//				int cid = cr.getID();
//				
//				if(!exclude.contains(cid))
//				{
//					double e = 0;
//					
//					e = cr.getEccentricity();
//					
//					if(eccentricities.containsKey(cid))
//					{
//						eccentricities.get(cid).add(e);
//					}
//					else
//					{
//						Vector<Double> values = new Vector<Double>();
//						values.add(e);
//						eccentricities.put(cid, values);
//					}
//				}
//			}
//		}
//		
//		return eccentricities;
//	}
	
	
//	/**
//	 * calculates the lengths of the major axes of the best fitting ellipses for each object<br/>
//	 *  assumes the pixels to be square, i.e. deltaX == deltaY
//	 *   
//	 * @return length of major axis of each object in every frame <br/>
//	 * key: object-id, value: list of lengths
//	 */
//	private Hashtable<Integer, Vector<Double>> getLengths()
//	{
//		Hashtable<Integer, Vector<Double>> lengths = new Hashtable<Integer, Vector<Double>>();
//		
//		for(int i = 0; i < sizeT; i++)	
//		{
//			MTBImage frame = labelImg.getImagePart(0, 0, 0, i, 0, sizeX, sizeY, 1, 1, 1);
//			MTBRegion2DSet currentRegions = LabelAreasToRegions.getRegions(frame, bgLabel);
//				
//			for(int j = 0; j < currentRegions.size(); j++)
//			{
//				MTBRegion2D cr = currentRegions.elementAt(j);
//				int cid = cr.getID();
//				
//				if(!exclude.contains(cid))
//				{
//					double l = 0;
//					
//					l = cr.getMajorAxisLength() * deltaX;
//					
//					if(lengths.containsKey(cid))
//					{
//						lengths.get(cid).add(l);
//					}
//					else
//					{
//						Vector<Double> values = new Vector<Double>();
//						values.add(l);
//						lengths.put(cid, values);
//					}
//				}
//			}
//		}
//		
//		
//		return lengths;
//	}
//	
///**
// * 
// * @param areas
// * @param lengths
// * @return average width of each object in every frame <br/>
//	 * key: object-id, value: list of widths
// */
//	private Hashtable<Integer, Vector<Double>> getAvgWidths(Hashtable<Integer, Vector<Double>> areas, Hashtable<Integer, Vector<Double>> lengths)
//	{
//		Hashtable<Integer, Vector<Double>> widths = new Hashtable<Integer, Vector<Double>>();
//		
//		Enumeration<Integer> keys = areas.keys();
//		
//		while(keys.hasMoreElements())
//		{
//			int k = keys.nextElement();
//			
//			Vector<Double> a = areas.get(k);
//			Vector<Double> l = lengths.get(k);
//			
//			Vector<Double> w = new Vector<Double>();
//			
//			for(int i = 0; i < a.size(); i++)
//			{
//				w.add(a.elementAt(i) / l.elementAt(i));
//			}
//			
//			widths.put(k, w);
//		}
//		
//		return widths;
//	}
	
	
//	public void setExcluded(Vector<Integer> exclude)
//	{
//		this.exclude = exclude;
//	}
}
