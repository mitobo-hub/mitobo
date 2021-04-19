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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageRGB;

/**
 * Stores data of recognized pavement cells.
 * @author Benjamin Schwede
 */
public class Cell implements CellInterface{
	private List<CellGraphNode> perimeterNodes = new ArrayList<>();
	private List<CellGraphEdge> edges = new ArrayList<>();
	private List<CellCoordinate> perimeter = new ArrayList<>();
	private List<Float> angle = new ArrayList<>();
	private List<Float> widthL = new ArrayList<>();
	private List<Float> widthR = new ArrayList<>();
	private CellCoordinate center = new CellCoordinate(-1,-1);
	private int id;
	private int area = -1;
	private int perimeterPx = -1;
	private int convexPerimeterPx = -1;
	private int convexArea = -1;
	private double perimeterlength = 0;
	private CellFeatures features;
	
	private MTBImageByte cellImage;
	private MTBImageByte convexCellImage;

	public Cell(int id, List<CellGraphNode> borderNodes, List<CellGraphEdge> cellEdges)
	{
		this.perimeterNodes = borderNodes;
		edges = cellEdges;
		this.id = id;
		//calc center
		center = calculateCenter();
		//calculate scope
		for(int n = 0; n < edges.size(); n++)
		{
			perimeterlength = perimeterlength + edges.get(n).getLength();
		}
		//create perimeter
		for(int n = 0; n < cellEdges.size(); n++)
		{
			if(perimeter.size() != 0)
			{
				//search next coordinate on perimeter
				CellCoordinate lastPerimeterPoint = perimeter.get(perimeter.size()-1); //== LPP
				CellCoordinate firstPerimeterPoint = perimeter.get(0);	//== FPP
				if(cellEdges.get(n).getXCoordinates().length > 0)
				{
					//check whether the next line is inserted forwards or backwards at the beginning or end of perimeter
					CellCoordinate firstElement = new CellCoordinate(cellEdges.get(n).getXCoordinates()[0], cellEdges.get(n).getYCoordinates()[0]);
					CellCoordinate lastElement = new CellCoordinate(cellEdges.get(n).getXCoordinates()[cellEdges.get(n).getXCoordinates().length-1], cellEdges.get(n).getYCoordinates()[cellEdges.get(n).getYCoordinates().length-1]);
					//calculate distances for end of perimeter
					double distLPPFirst = Math.sqrt(Math.pow(lastPerimeterPoint.getX() - firstElement.getX(), 2) + Math.pow(lastPerimeterPoint.getY() - firstElement.getY(), 2));
					double distLPPLast = Math.sqrt(Math.pow(lastPerimeterPoint.getX() - lastElement.getX(), 2) + Math.pow(lastPerimeterPoint.getY() - lastElement.getY(), 2));
					//calculate distances for beginning of perimeter
					double distFPPFirst = Math.sqrt(Math.pow(firstPerimeterPoint.getX() - firstElement.getX(), 2) + Math.pow(firstPerimeterPoint.getY() - firstElement.getY(), 2));
					double distFPPLast = Math.sqrt(Math.pow(firstPerimeterPoint.getX() - lastElement.getX(), 2) + Math.pow(firstPerimeterPoint.getY() - lastElement.getY(), 2));
					
					List<Double> distances = new ArrayList<>(Arrays.asList(distFPPFirst, distFPPLast, distLPPFirst, distLPPLast));
					int minDist = distances.indexOf(Collections.min(distances));
					
					switch(minDist)
					{
						case 0: // distFPPFirst
							//insert line in front of perimeter in orientation [0->n]
							for(int m = 0; m < cellEdges.get(n).getXCoordinates().length; m++)
							{
								CellCoordinate c = new CellCoordinate(cellEdges.get(n).getXCoordinates()[m], cellEdges.get(n).getYCoordinates()[m]);
								if(!perimeter.contains(c))
								{
									perimeter.add(0, c);
									angle.add(0, cellEdges.get(n).getAngle()[m]);
									widthL.add(0, cellEdges.get(n).getWidthL()[m]);
									widthR.add(0, cellEdges.get(n).getWidthR()[m]);
								}
							}
							break;
						case 1: // distFPPLast
							//insert line in front of perimeter in orientation [n->0]
							for(int m = cellEdges.get(n).getXCoordinates().length-1; m >= 0; m--)
							{
								CellCoordinate c = new CellCoordinate(cellEdges.get(n).getXCoordinates()[m], cellEdges.get(n).getYCoordinates()[m]);
								if(!perimeter.contains(c))
								{
									perimeter.add(0, c);
									angle.add(0, cellEdges.get(n).getAngle()[m]);
									widthL.add(0, cellEdges.get(n).getWidthL()[m]);
									widthR.add(0, cellEdges.get(n).getWidthR()[m]);
								}
							}
							break;
						case 2: // distLPPFirst
							//insert line after perimeter in orientation [0->n]
							for(int m = 0; m < cellEdges.get(n).getXCoordinates().length; m++)
							{
								CellCoordinate c = new CellCoordinate(cellEdges.get(n).getXCoordinates()[m], cellEdges.get(n).getYCoordinates()[m]);
								if(!perimeter.contains(c))
								{
									perimeter.add(c);
									angle.add(cellEdges.get(n).getAngle()[m]);
									widthL.add(cellEdges.get(n).getWidthL()[m]);
									widthR.add(cellEdges.get(n).getWidthR()[m]);
								}
							}
							break;
						case 3: // distLPPFirst
							//insert line after perimeter in orientation [n->0]
							for(int m = cellEdges.get(n).getXCoordinates().length-1; m >= 0; m--)
							{
								CellCoordinate c = new CellCoordinate(cellEdges.get(n).getXCoordinates()[m], cellEdges.get(n).getYCoordinates()[m]);
								if(!perimeter.contains(c))
								{
									perimeter.add(c);
									angle.add(cellEdges.get(n).getAngle()[m]);
									widthL.add(cellEdges.get(n).getWidthL()[m]);
									widthR.add(cellEdges.get(n).getWidthR()[m]);
								}
							}
							break;
					}
				}
			}
			else
			{
				for(int m = 0; m < cellEdges.get(n).getXCoordinates().length; m++)
				{
					CellCoordinate c = new CellCoordinate(cellEdges.get(n).getXCoordinates()[m], cellEdges.get(n).getYCoordinates()[m]);
					if(!perimeter.contains(c))
					{
						perimeter.add(c);
						angle.add(cellEdges.get(n).getAngle()[m]);
						widthL.add(cellEdges.get(n).getWidthL()[m]);
						widthR.add(cellEdges.get(n).getWidthR()[m]);
					}
				}
			}
		}
	}
	
	private void drawCellContur()
	{
		MTBImageRGB img = (MTBImageRGB)MTBImage.createMTBImage(1024, 1024, 1, 1, 1, MTBImage.MTBImageType.MTB_RGB);
		img.fillBlack();
		img.setTitle("C" + this.id);
		img.close();
		for(int n = 0; n < perimeter.size(); n++)
		{
			img.putValueInt((int)perimeter.get(n).getX(), (int)perimeter.get(n).getY(), 255);
		}
		
		img.show();
	}
	
	private CellCoordinate calculateCenter()
	{
		float x = 0, y = 0;
		int cnt = 0;
		for(int n = 0; n < perimeterNodes.size(); n++)
		{
			x = x + perimeterNodes.get(n).getCoordinate().getX();
			y = y + perimeterNodes.get(n).getCoordinate().getY();
		}
		cnt = cnt + perimeterNodes.size();
		for(int n = 0; n < edges.size(); n++)
		{
			for(int m = 0; m < edges.get(n).getXCoordinates().length; m++)
			{
				x = x + edges.get(n).getXCoordinates()[m];
				y = y + edges.get(n).getYCoordinates()[m];
			}
			cnt = cnt + edges.get(n).getXCoordinates().length;
		}
		return new CellCoordinate(x/cnt, y/cnt);
	}
	
	public int getId()
	{
		return id;
	}
	
	public CellCoordinate getCenter()
	{
		return center;
	}
	
	public List<CellCoordinate> getPerimeter()
	{
		return perimeter;
	}
	public List<Float> getAngle()
	{
		return angle;
	}
	public List<Float> getWidthR()
	{
		return widthR;
	}
	public List<Float> getWidthL()
	{
		return widthL;
	}
	
	public List<CellGraphEdge> getEdge()
	{
		return edges;
	}
	
	public int getArea()
	{
		return area;
	}
	public int getConvexArea()
	{
		return convexArea;
	}
	public int getPerimeterPx()
	{
		return perimeterPx;
	}
	public int getConvexPerimeterPx()
	{
		return convexPerimeterPx;
	}
	public double getPerimeterLength()
	{
		return perimeterlength;
	}
	public MTBImageByte getCellImage()
	{
		return cellImage;
	}
	public MTBImageByte getConvexCellImage()
	{
		return convexCellImage;
	}
	public CellFeatures getCellFeatures()
	{
		return features;
	}
	
	public void setPerimeterPx(int perimeterPx)
	{
		this.perimeterPx = perimeterPx;
	}
	public void setConvexPerimeterPx(int convexPerimeterPx)
	{
		this.convexPerimeterPx = convexPerimeterPx;
	}
	public void setArea(int area)
	{
		this.area = area;
	}
	public void setConvexArea(int convexArea)
	{
		this.convexArea = convexArea;
	}
	public void setCellImage(MTBImageByte cellImage)
	{
		this.cellImage = cellImage;
	}
	public void setConvexCellImage(MTBImageByte convexCellImage)
	{
		this.convexCellImage = convexCellImage;
	}
	public void setCellFeatures(CellFeatures features)
	{
		this.features = features;
	}
	
	public int calculateArea(MTBImageByte tempCellImage)
	{
		tempCellImage.fillWhite();
		int area = 0;
		for(int n = 0; n < perimeter.size(); n++)
		{
			tempCellImage.putValueInt((int)perimeter.get(n).getX(), (int)perimeter.get(n).getY(), 0);
		}
		area = countBlackPixel(tempCellImage)/2;
		fillBlack(tempCellImage,0,0);
		this.area = area + countWhitePixel(tempCellImage);
		return this.area;
	}
	
	private void fillBlack(MTBImageByte image, int x, int y)
	{
		if(image.getValueInt(x, y) == 255)
		{
			image.putValueInt(x, y, 0);
			if(x+1 < image.getSizeX())
			{
				fillBlack(image, x+1, y);
			}
			if(y+1 < image.getSizeY())
			{
				fillBlack(image, x, y+1);
			}
			if(x-1 > 0)
			{
				fillBlack(image, x-1, y);
			}
			if(y-1 > 0)
			{
				fillBlack(image, x, y-1);
			}
			
			if(x+1 < image.getSizeX() && y+1 < image.getSizeY())
			{
				fillBlack(image, x+1, y+1);
			}
			if(x+1 < image.getSizeX() && y-1 > 0)
			{
				fillBlack(image, x+1, y-1);
			}
			if(x-1 > 0  && y+1 < image.getSizeY())
			{
				fillBlack(image, x-1, y+1);
			}
			if(x-1 > 0  && y-1 > 0)
			{
				fillBlack(image, x-1, y-1);
			}
		}
	}
	private int countBlackPixel(MTBImageByte image)
	{
		int cnt = 0;
		for(int x = 0; x < image.getSizeX(); x++)
		{
			for(int y = 0; y < image.getSizeY(); y++)
			{
				if(image.getValueInt(x, y) == 0)
				{
					cnt++;
				}
			}
		}
		return cnt;
	}
	private int countWhitePixel(MTBImageByte image)
	{
		int cnt = 0;
		for(int x = 0; x < image.getSizeX(); x++)
		{
			for(int y = 0; y < image.getSizeY(); y++)
			{
				if(image.getValueInt(x, y) == 255)
				{
					cnt++;
				}
			}
		}
		return cnt;
	}
	
	public List<CellCoordinate> getPerimeterNodes()
	{
		List<CellCoordinate> nodes = new ArrayList<>();
		for(int n = 0; n < perimeterNodes.size(); n++)
		{
			nodes.add(perimeterNodes.get(n).getCoordinate());
		}
		return nodes;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if(obj instanceof Cell)
		{
			Cell c = (Cell)obj;
			if(center.equals(c.getCenter()) || id == c.getId())
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
	
	
	@Override
	public String toString()
	{
		return "C" + id + " scope: " + perimeterlength + " " + Arrays.toString(perimeter.toArray()) + "\n\tnodes: " + Arrays.toString(perimeterNodes.toArray()) + "\n\tedges: "+ Arrays.toString(edges.toArray());
	}
}