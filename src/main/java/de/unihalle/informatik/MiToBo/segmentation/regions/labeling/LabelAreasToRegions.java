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

package de.unihalle.informatik.MiToBo.segmentation.regions.labeling;

import java.awt.geom.Point2D;
import java.util.Vector;

import de.unihalle.informatik.MiToBo.core.datatypes.MTBPoint3D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion3D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion3DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;

public class LabelAreasToRegions
{
	/**
	 * 
	 * @param frame frame from which the regions should be extracted
	 * @param bgLabel label assigned to the background
	 * @return region set of all regions except the background region
	 */
	public static MTBRegion2DSet getRegions(MTBImage frame, int bgLabel)
	{
		int sizeX = frame.getSizeX();
		int sizeY = frame.getSizeY();
		
		MTBRegion2DSet regions = new MTBRegion2DSet(0, 0, sizeX, sizeY);
		Vector<Integer> labels = new Vector<Integer>();
		
		for(int y = 0; y < sizeY; y++)
		{
			for(int x = 0; x < sizeX; x++)
			{
				int lbl = frame.getValueInt(x, y);
				
				if(lbl != bgLabel)
				{
					if(labels.contains(lbl))	// region with current label was already found
					{
						for(int i = 0; i < regions.size(); i++)
						{
							MTBRegion2D r = regions.elementAt(i);
							
							if(r.getID() == lbl)
							{
								r.addPixel(new Point2D.Double(x,y));
							}
						}
					}
					else	// new region
					{
						MTBRegion2D r = new MTBRegion2D();
						r.setID(lbl);
						r.addPixel(new Point2D.Double(x,y));
						
						regions.add(r);
						labels.add(lbl);
					}
				}
			}
		}
		
		return regions;
	}
	
	
	/**
	 * 
	 * @param frame
	 * @return region set of all regions (possibly including the background region)
	 */
	public static MTBRegion2DSet getRegions(MTBImage frame)
	{
		int sizeX = frame.getSizeX();
		int sizeY = frame.getSizeY();
		
		MTBRegion2DSet regions = new MTBRegion2DSet(0, 0, sizeX, sizeY);
		Vector<Integer> labels = new Vector<Integer>();
		
		for(int y = 0; y < sizeY; y++)
		{
			for(int x = 0; x < sizeX; x++)
			{
				int lbl = frame.getValueInt(x, y);
				
				if(labels.contains(lbl))	// region with current label already found
				{
					for(int i = 0; i < regions.size(); i++)
					{
						MTBRegion2D r = regions.elementAt(i);
						
						if(r.getID() == lbl)
						{
							r.addPixel(new Point2D.Double(x,y));
						}
					}
				}
				else	// new region
				{
					MTBRegion2D r = new MTBRegion2D();
					r.setID(lbl);
					r.addPixel(new Point2D.Double(x,y));
					
					regions.add(r);
					labels.add(lbl);
				}	
			}
		}
		
		return regions;
	}
	
	
	/**
	 * 
	 * @param frame
	 * @param bgLabel label assigned to the background
	 * @return background region
	 */
	public static MTBRegion2D getBackground(MTBImage frame, int bgLabel)
	{
		int sizeX = frame.getSizeX();
		int sizeY = frame.getSizeY();
		
		MTBRegion2D background = new MTBRegion2D();
		background.setID(bgLabel);
		
		for(int y = 0; y < sizeY; y++)
		{
			for(int x = 0; x < sizeX; x++)
			{
				int lbl = frame.getValueInt(x, y);
				
				if(lbl == bgLabel)
				{
					background.addPixel(x, y);
				}
			}
		}
		
		return background;
	}
	
	
	public static MTBRegion3DSet getRegions3D(MTBImage frame, int bgLabel)
	{
		int sizeX = frame.getSizeX();
		int sizeY = frame.getSizeY();
		int sizeZ = frame.getSizeZ();
		
		MTBRegion3DSet regions = new MTBRegion3DSet(0, 0, 0, sizeX-1, sizeY-1, sizeZ-1);
		Vector<Integer> labels = new Vector<Integer>();
		
		for(int z = 0; z < sizeZ; z++)
		{
			for(int y = 0; y < sizeY; y++)
			{
				for(int x = 0; x < sizeX; x++)
				{
					int lbl = frame.getValueInt(x, y, z);
					
					if(lbl != bgLabel)
					{
						if(labels.contains(lbl))	// region with current label was already found
						{
							for(int i = 0; i < regions.size(); i++)
							{
								MTBRegion3D r = regions.elementAt(i);
								
								if(r.getID() == lbl)
								{
									r.addPoint(new MTBPoint3D(x,y,z));
								}
							}
						}
						else	// new region
						{
							MTBRegion3D r = new MTBRegion3D();
							r.setID(lbl);
							r.addPoint(new MTBPoint3D(x,y,z));
							
							regions.add(r);
							labels.add(lbl);
						}
					}
				}
			}
		}
		
		
		
		return regions;
	}
}
