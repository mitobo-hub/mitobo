package de.unihalle.informatik.MiToBo.core.datatypes;

import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.Alida.operator.ALDData;
import loci.formats.tiff.PhotoInterp;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@ALDParametrizedClass
public class MTBPoint2DSet extends ALDData implements Cloneable, Iterable<Point2D>
{
	
	@ALDClassParameter(label="List of points")
	List<Point2D> point2DList;
	
	public MTBPoint2DSet()
	{
		this.point2DList = new ArrayList<Point2D>();
	}
	
	public MTBPoint2DSet(List<Point2D> point2DList)
	{
		this.point2DList = point2DList;
	}
	
	@Override
	public Iterator<Point2D> iterator()
	{
		return point2DList.iterator();
	}
	
	@Override
	public MTBPoint2DSet clone()
	{
		MTBPoint2DSet newSet = new MTBPoint2DSet();
		for(Point2D point2D: this.point2DList) 
		{
			newSet.point2DList.add((Point2D) point2D.clone());
		}
		
		return newSet;
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder("MTBPoint2DSet - set of " + this.point2DList.size() + " points:\n");
		
		for(int i = 0; i < this.point2DList.size(); i++)
		{
			Point2D point = this.point2DList.get(i);
			sb.append("[" + point.getX() + ", " + point.getY() + "]");
			if(i != this.point2DList.size() - 1) sb.append(", ");
		}
		
		return sb.toString();
	}
	
	public int size()
	{
		return this.point2DList.size();
	}
	
	public boolean add(Point2D point2D)
	{
		return this.point2DList.add(point2D);
	}
	
	public boolean remove(Point2D point2D)
	{
		return this.point2DList.remove(point2D);
	}
	
	
}
