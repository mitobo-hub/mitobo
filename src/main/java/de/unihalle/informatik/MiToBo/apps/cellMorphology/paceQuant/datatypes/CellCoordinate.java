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
 * Class for handling subpixel coordinates in images.
 * @author Benjamin Schwede
 */
public class CellCoordinate{
	private float x,y;
	
	public CellCoordinate(float x, float y)
	{
		this.x = x;
		this.y = y;
	}
	
	public CellCoordinate(double x, double y)
	{
		this.x = (float)x;
		this.y = (float)y;
	}

	public float getX()
	{
		return x;
	}
	public float getY()
	{
		return y;
	}
	public boolean compare(CellCoordinate c)
	{
		return (x == c.getX() && y == c.getY());
	}
	public double distance(CellCoordinate c)
	{
		return Math.sqrt(Math.pow(x-c.getX(), 2) + Math.pow(y-c.getY(), 2));
	}
	
	@Override
	public String toString()
	{
		return "(" + x + ", " + y + ")";
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(o instanceof CellCoordinate)
		{
			CellCoordinate c = (CellCoordinate)o;
			if(c.getX() == x && c.getY() == y)
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