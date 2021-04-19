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
 * Representation of detected perimeter fragments in cell graphs.
 * @author Benjamin Schwede
 */
public class CellGraphEdge {
	private double length;
	private int id;
	private CellGraphNode src, dst;
	private float[] x, y;
	private float[] angle;
	private float[] widthL;
	private float[] widthR;
	
	public CellGraphEdge(int id, double length)
	{
		this.length = length;
		this.id = id;
		x = new float[0];
		y = new float[0];
		angle = new float[0];
		widthR = new float[0];
		widthL = new float[0];
	}
	
	public void setXCoordinates(float[] x)
	{
		this.x = x;
	}
	
	public void setYCoordinates(float[] y)
	{
		this.y = y;
	}
	
	public void setAngle(float[] angle)
	{
		this.angle = angle;
	}
	
	public void setWidthR(float[] widthR)
	{
		this.widthR = widthR;
	}
	
	public void setWidthL(float[] widthL)
	{
		this.widthL = widthL;
	}
	
	public float[] getXCoordinates()
	{
		return x;
	}
	
	public float[] getYCoordinates()
	{
		return y;
	}
	
	public float[] getAngle()
	{
		return angle;
	}
	
	public float[] getWidthL()
	{
		return widthL;
	}
	
	public float[] getWidthR()
	{
		return widthR;
	}
	
	public double getLength()
	{
		return length;
	}
	public int getId()
	{
		return id;
	}
	
	public void setSrc(CellGraphNode src)
	{
		this.src = src;
	}
	public void setDst(CellGraphNode dst)
	{
		this.dst = dst;
	}

	public CellGraphNode getSrc() {
		return src;
	}

	public CellGraphNode getDst() {
		return dst;
	}

	@Override
	public String toString()
	{
		return "L" + id;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if(obj instanceof CellGraphEdge)
		{
			CellGraphEdge e = (CellGraphEdge)obj;
			if(id == e.getId() && length == e.getLength())
			{
				return true;
			}
			else
			{
				return false;
			}
		}
		else
		{
			return false;
		}
	}
}
