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

package de.unihalle.informatik.MiToBo.apps.singleCellTracking2D;

import java.awt.geom.Point2D;
import java.util.Vector;

import de.unihalle.informatik.Alida.operator.ALDData;

public class Trajectory2D extends ALDData
{
	private int id;
	private int startFrame;
	private int parentID = -1;	// id of parent (-1 if not set)
	private Vector<Point2D.Double> points;
	
	
	public Trajectory2D(int id, int startFrame, Vector<Point2D.Double> points)
	{
		this.id = id;
		this.startFrame = startFrame;
		this.points = points;
	}
	
	
	public Trajectory2D(int id, int startFrame)
	{
		this(id, startFrame, new Vector<Point2D.Double>());
	}
	
	
	public Trajectory2D(int id)
	{
		this(id, 0, new Vector<Point2D.Double>());
	}
	
	
	public int getID()
	{
		return id;
	}


	public void setID(int id)
	{
		this.id = id;
	}

	public int getStartFrame()
	{
		return startFrame;
	}


	public void setStartFrame(int startFrame)
	{
		this.startFrame = startFrame;
	}

	
	public int getParentID()
	{
		return this.parentID;
	}
	
	
	public void setParentID(int pid)
	{
		this.parentID = pid;
	}

	
	public Vector<Point2D.Double> getPoints()
	{
		return points;
	}


	public void setPoints(Vector<Point2D.Double> points)
	{
		this.points = points;
	}
	
	public void addPoint(Point2D.Double p)
	{
		this.points.add(p);
	}
	
	
	/**
	 * 
	 * @return short summary of the trajectory consisting of its id, start frame, end frame and the id (if available) of its
	 * parent, separated by spaces
	 */
	public String getSummary()
	{
		return new String(id + " " + startFrame + " " + (startFrame + points.size() - 1) + " " + parentID);
	}
}
