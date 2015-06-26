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
 * $Rev: 5460 $
 * $Date: 2012-04-17 16:42:16 +0200 (Di, 17 Apr 2012) $
 * $Author: glass $
 * 
 */

package de.unihalle.informatik.MiToBo.apps.singleCellTracking2D;

import java.awt.geom.Point2D;
import java.util.Random;
import java.util.Vector;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;

public class TrackVisualizer
{
	private transient MTBImage inImg;
	Vector<Trajectory2D> trajectories;
	int sizeX;
	int sizeY;
	int sizeT;
	
	
	/**
	 * 
	 * @param inImg
	 * @param trajectories
	 */
	public TrackVisualizer(MTBImage inImg, Vector<Trajectory2D> trajectories)
	{
		this.inImg = inImg;
		this.trajectories = trajectories;
		
		this.sizeX = inImg.getSizeX();
		this.sizeY = inImg.getSizeY();
		this.sizeT = inImg.getSizeT();
	}
	
	
	/**
	 * 
	 * @param pseudocolor
	 * @param center
	 * @return
	 * @throws ALDOperatorException
	 */
	public MTBImage create2DTrajectoryImage(boolean pseudocolor, boolean center) throws ALDOperatorException
	{
		MTBImage trajectoryImg;
		
		if(pseudocolor)
		{
			trajectoryImg = MTBImage.createMTBImage(sizeX, sizeY, 1, 1, 1, MTBImage.MTBImageType.MTB_RGB);
		}
		else
		{
			trajectoryImg = MTBImage.createMTBImage(sizeX, sizeY, 1, 1, 1, MTBImage.MTBImageType.MTB_BYTE);
		}
		
		trajectoryImg.fillBlack();
		trajectoryImg.setTitle("trajectories");
		
		int centerX = sizeX / 2;
		int centerY = sizeY / 2;
		int tx = 0;
		int ty = 0;
		
		Random r = new Random();
		
		for(int i = 0; i < trajectories.size(); i++)
		{
			Trajectory2D trajectory = trajectories.elementAt(i);
			
			int color = trajectory.getID();
			
			if(pseudocolor)
			{
				color = r.nextInt((int)Math.pow(2, 32));
			}
			
			Vector<Point2D.Double> points = trajectory.getPoints();
			
			for(int j = 1; j < points.size(); j++)
			{
				Point2D.Double start = points.elementAt(j-1);
				Point2D.Double end  = points.elementAt(j);
				
				if(center)
				{
					if(j == 1)	// calculate translation vector
					{
						tx = centerX - (int)start.x;
						ty = centerY - (int)start.y;
					}
					
					trajectoryImg.drawLine2D((int)start.x + tx, (int)start.y + ty, (int)end.x + tx, (int)end.y + ty, color);
				}
				else
				{
					trajectoryImg.drawLine2D((int)start.x, (int)start.y, (int)end.x, (int)end.y, color);
				}
				
			}
		}
		
		
		return trajectoryImg;
	}
	
	
	/**
	 * 
	 * @param pseudocolor
	 * @return
	 * @throws ALDOperatorException
	 */
	public MTBImage create3DTrajectoryImage(boolean pseudocolor) throws ALDOperatorException
	{
		MTBImage trajectoryImg;
		
		if(pseudocolor)
		{
			trajectoryImg = MTBImage.createMTBImage(sizeX, sizeY, sizeT, 1, 1, MTBImage.MTBImageType.MTB_RGB);
		}
		else
		{
			trajectoryImg = MTBImage.createMTBImage(sizeX, sizeY, sizeT, 1, 1, MTBImage.MTBImageType.MTB_BYTE);
		}
		
		trajectoryImg.fillBlack();
		trajectoryImg.setTitle("trajectories");
		
		Random r = new Random();
		
		for(int i = 0; i < trajectories.size(); i++)
		{
			Trajectory2D trajectory = trajectories.elementAt(i);
			
			int color = trajectory.getID();
			
			if(pseudocolor)
			{
				color = r.nextInt((int)Math.pow(2, 32));
			}
			
			Vector<Point2D.Double> points = trajectory.getPoints();
			int startFrame = trajectory.getStartFrame();
			
			for(int j = 0; j < points.size(); j++)
			{
				Point2D.Double p = points.elementAt(j);
				
				trajectoryImg.putValueInt((int)p.x, (int)p.y, startFrame + j, color);
			}
		}
		
		
		return trajectoryImg;
	}
	
	
	public MTBImage createProgressionImage(boolean pseudocolor) throws ALDOperatorException
	{
		MTBImage trajectoryImg;
		
		if(pseudocolor)
		{
			trajectoryImg = MTBImage.createMTBImage(sizeX, sizeY, 1, sizeT, 1, MTBImage.MTBImageType.MTB_RGB);
		}
		else
		{
			trajectoryImg = MTBImage.createMTBImage(sizeX, sizeY, 1, sizeT, 1, MTBImage.MTBImageType.MTB_BYTE);
		}
		
		trajectoryImg.fillBlack();
		trajectoryImg.setTitle("trajectories");
		
		Random r = new Random();
		
		for(int i = 0; i < trajectories.size(); i++)
		{
			Trajectory2D trajectory = trajectories.elementAt(i);
			
			int color = trajectory.getID();
			
			if(pseudocolor)
			{
				color = r.nextInt((int)Math.pow(2, 32));
			}
			
			Vector<Point2D.Double> points = trajectory.getPoints();
			int startFrame = trajectory.getStartFrame();
			
			for(int j = 0; j < points.size(); j++)
			{
				MTBImage currSlice = trajectoryImg.getSlice(0, startFrame + j, 0);
				
				//TODO: draw point in starting frame
				
				for(int k = 1; k <= j; k++)
				{
					Point2D.Double start = points.elementAt(k-1);
					Point2D.Double end  = points.elementAt(k);
					
					currSlice.drawLine2D((int)start.x, (int)start.y, (int)end.x, (int)end.y, color);
				}
				
				trajectoryImg.setCurrentSliceIndex(startFrame + j);
				trajectoryImg.setCurrentSlice(currSlice);
			}
		}
		
		
		return trajectoryImg;
	}
	
	
	public MTBImage createOverlayImage(boolean pseudocolor) throws ALDOperatorException
	{
		MTBImage trajectoryImg;
		
		if(pseudocolor)
		{
			trajectoryImg = inImg.duplicate().convertType(MTBImageType.MTB_RGB, false);
		}
		else
		{
			trajectoryImg = inImg.duplicate().convertType(MTBImageType.MTB_BYTE, false);
		}
		
		trajectoryImg.setTitle("trajectories");
		
		Random r = new Random();
		
		for(int i = 0; i < trajectories.size(); i++)
		{
			Trajectory2D trajectory = trajectories.elementAt(i);
			
			int color = trajectory.getID();
			
			if(pseudocolor)
			{
				color = r.nextInt((int)Math.pow(2, 32));
			}
			
			Vector<Point2D.Double> points = trajectory.getPoints();
			int startFrame = trajectory.getStartFrame();
			
			for(int j = 0; j < points.size(); j++)
			{
				MTBImage currSlice = trajectoryImg.getSlice(0, startFrame + j, 0);
				
				//TODO: draw point in starting frame
				for(int k = 1; k <= j; k++)
				{
					Point2D.Double start = points.elementAt(k-1);
					Point2D.Double end  = points.elementAt(k);
					currSlice.drawLine2D((int)start.x, (int)start.y, (int)end.x, (int)end.y, color);
				}
				
				trajectoryImg.setCurrentSliceIndex(startFrame + j);
				trajectoryImg.setCurrentSlice(currSlice);
			}
		}
		
		
		return trajectoryImg;
	}
}
