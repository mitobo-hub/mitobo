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

package de.unihalle.informatik.MiToBo.visualization.drawing;

import java.awt.geom.Point2D;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;


/**
 * Implementation of the Bresenham line drawing algorithm according to <br\>
 * http://en.wikipedia.org/wiki/Bresenham%27s_line_algorithm
 * 
 * @author glass
 *
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL, level=Level.APPLICATION)
public class DrawLine extends MTBOperator
{
	@Parameter(label = "inImg", required = true, direction = Parameter.Direction.IN, supplemental = false, description = "input image", dataIOOrder = 0)
	private transient MTBImage inImg = null;
	
	@Parameter(label = "start", required = true, direction = Parameter.Direction.IN, supplemental = false, description = "starting point of the line", dataIOOrder = 1)
	private Point2D.Double start = null;
	
	@Parameter(label = "end", required = true, direction = Parameter.Direction.IN, supplemental = false, description = "end point of the line", dataIOOrder = 2)
	private Point2D.Double end = null;
	
	@Parameter(label = "color", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "color value to draw the line in", dataIOOrder = 3)
	private Double color = 255.0;
	
	public DrawLine() throws ALDOperatorException
	{
		
	}
	
	
	public DrawLine(MTBImage inImg, Point2D.Double start, Point2D.Double end) throws ALDOperatorException
	{
		this.inImg = inImg;
		this.start = start;
		this.end = end;
	}


	@Override
	public void operate() throws ALDOperatorException, ALDProcessingDAGException
	{
		line();
		
		inImg.updateAndRepaintWindow();
	}
	
	
	
	private void line()
	{
		int x0 = (int)start.x;
		int y0 = (int)start.y;
		int x1 = (int)end.x;
		int y1 = (int)end.y;
		
		// check if line end points don't exceed the image boundaries
		int sizeX = inImg.getSizeX();
		int sizeY = inImg.getSizeY();
		
		if(x0 < 0)
		{
			x0 = 0;
		}
		if(x0 >= sizeX)
		{
			x0 = sizeX - 1;
		}
		
		if(y0 < 0)
		{
			y0 = 0;
		}
		if(y0 >= sizeY)
		{
			y0 = sizeY - 1;
		}
		
		if(x1 < 0)
		{
			x1 = 0;
		}
		if(x1 >= sizeX)
		{
			x1 = sizeX - 1;
		}
		
		if(y1 < 0)
		{
			y1 = 0;
		}
		if(y1 >= sizeY)
		{
			y1 = sizeY - 1;
		}
		
		int dx = Math.abs(x1 - x0);
		int dy = -Math.abs(y1 - y0);
		int sx = x0 < x1 ? 1 : -1;
		int sy = y0 < y1 ? 1 : -1; 
		int err = dx + dy, e2; 
		 
		for(;;)
		{
			inImg.putValueDouble(x0, y0, color);
		    
			if(x0 == x1 && y0 == y1)
		    {
		    	break;
		    }
		    	
		    e2 = 2 * err;
		   
		    if(e2 > dy)
		    {
		    	err += dy; 
		    	x0 += sx;
		    }
		    
		    if(e2 < dx)
		    {
		    	err += dx; 
		    	y0 += sy;
		    } 
		}
	}
	
	
	
	public void setColor(double c)
	{
		this.color = c;
	}
	
}
