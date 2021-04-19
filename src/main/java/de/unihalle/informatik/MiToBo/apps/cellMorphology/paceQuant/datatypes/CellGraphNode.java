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

import java.util.ArrayList;
import java.util.List;


/**
 * Representation of junctions between detected perimeter fragments 
 * in cell graphs.
 * @author Benjamin Schwede
 */
public class CellGraphNode {
	private CellCoordinate coordinate;
	private int id;
	private List<Integer> edgeIds = new ArrayList<>(); //contains also edges with not set end-junction
	
	public CellGraphNode(int id, Junction j,  CellCoordinate c)
	{
		this.id = id;
		coordinate = c; //new Coordinates(Math.round(j.getX()), Math.round(j.getY()));
		edgeIds.add(j.getLine1().getID());
		edgeIds.add(j.getLine2().getID());
	}
	
	public CellGraphNode(int id, CellCoordinate c)
	{
		this.id = id;
		coordinate = c; //new Coordinates(Math.round(j.getX()), Math.round(j.getY()));
	}
	
	public CellGraphNode() {
		// TODO Auto-generated constructor stub
	}

	public void addEdgeId(Line l)
	{
		edgeIds.add(l.getID());
	}
	
	public void addEdgeId(int id)
	{
		edgeIds.add(id);
	}
	
	public boolean removeEdge(int id)
	{
		return edgeIds.remove((Object)id);
	}
	
	public boolean removeEdges(List<Integer> ids)
	{
		return edgeIds.removeAll(ids);
	}
	
	public CellCoordinate getCoordinate()
	{
		return coordinate;
	}
	
	public int getId()
	{
		return id;
	}
	public List<Integer> getEdgeIds()
	{
		return edgeIds;
	}
	
	@Override
	public String toString()
	{
		return "N" + id + coordinate.toString();
	}
}
