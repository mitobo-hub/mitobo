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

import java.util.List;

import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;

/**
 * Interface for objects of different cell types.
 * @author Benjamin Schwede
 */
public interface CellInterface {
	public boolean equals(Object obj);
	public String toString();	
	
	public void setCellFeatures(CellFeatures features);
	public void setArea(int area);
	public void setConvexArea(int convexArea);
	public void setPerimeterPx(int perimeterPx);
	public void setConvexPerimeterPx(int convexPerimeterPx);
	public void setCellImage(MTBImageByte cellImage);
	public void setConvexCellImage(MTBImageByte cellImage);
	
	public int getId();
	public CellCoordinate getCenter();
	public List<CellCoordinate> getPerimeter();
	public List<CellGraphEdge> getEdge();
	public int getArea();
	public int getConvexArea();
	public int getPerimeterPx();
	public int getConvexPerimeterPx();
	public CellFeatures getCellFeatures();
	public double getPerimeterLength();
	public MTBImageByte getCellImage();
	public MTBImageByte getConvexCellImage();
}
