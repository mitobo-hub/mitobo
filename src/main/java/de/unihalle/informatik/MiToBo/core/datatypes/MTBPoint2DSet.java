package de.unihalle.informatik.MiToBo.core.datatypes;

import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.Alida.operator.ALDData;

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
	
	public boolean addAll(MTBPoint2DSet point2DSet)
	{
		if(null == point2DSet) return false;
		return this.point2DList.addAll(point2DSet.point2DList);
	}
	
	public boolean remove(Point2D point2D)
	{
		return this.point2DList.remove(point2D);
	}
	
	public Point2D remove(int i)
	{		
		if (i < 0 || i >= size()) throw new ArrayIndexOutOfBoundsException();
		return this.point2DList.remove(i);
	}
	
	public Point2D get(int i)
	{
		if (i < 0 || i >= size()) throw new ArrayIndexOutOfBoundsException();
		return this.point2DList.get(i);
	}
	
	
	/**
	 * Algorithm idea: 
	 * 		- Urs Ramer, "An iterative procedure for the polygonal approximation of plane curves", 
	 * 			Computer Graphics and Image Processing, 1(3), 244–256 (1972) doi:10.1016/S0146-664X(72)80017-0
	 * 		- David Douglas & Thomas Peucker, "Algorithms for the reduction of the number of points required to represent a digitized line 
	 * 			or its caricature", The Canadian Cartographer 10(2), 112–122 (1973) doi:10.3138/FM57-6770-U75U-7727
	 * Implementation according to: https://www.namekdev.net/2014/06/iterative-version-of-ramer-douglas-peucker-line-simplification-algorithm/
	 * 
	 * @param startIndex	Index in line of the current starting point
	 * @param lastIndex		Index in line of the current ending point
	 * @param epsilon		Distance dimension. Keep the point if the distance is greater than epsilon. 
	 * 						The greater the epsilon the coarser the approximation.
	 * @return List of points after applying the algorithm
	 */
	public MTBPoint2DSet applyRamerDouglasPeuckerAlgorithm(int startIndex, int lastIndex, double epsilon)
	{
		MTBPoint2DSet result = new MTBPoint2DSet();
		Point2D startPoint = this.get(startIndex);
		Point2D endPoint = this.get(lastIndex);
		
		double dmax = 0.0;
		int index = startIndex;
		
		for(int i = index; i < lastIndex; i++)
		{
			Point2D point = this.get(i);
			double d = calculatePointLineDistance(point.getX(), point.getY(), startPoint.getX(), startPoint.getY(), endPoint.getX(), endPoint.getY());
			
			if(d > dmax)
			{
				index = i;
				dmax = d;
			}
		}
		
		if(dmax > epsilon)
		{
			MTBPoint2DSet res1 = applyRamerDouglasPeuckerAlgorithm(startIndex, index, epsilon);
			MTBPoint2DSet res2 = applyRamerDouglasPeuckerAlgorithm(index, lastIndex, epsilon);
			
			res1.remove(res1.size() - 1);
			result.addAll(res1);
			result.addAll(res2);
		}
		else
		{
			result.add(startPoint);
			result.add(endPoint);
		}
		
		return result;
	}
	
	/**
	 * Applies the Ramer-Douglas-Peucker algorithm with the first and last point of the point list
	 * @param epsilon	Distance dimension. Keep the point if the distance is greater than epsilon.
	 *                     The greater the epsilon the coarser the approximation.
	 * @return List of points after applying the algorithm
	 */
	public MTBPoint2DSet applyRamerDouglasPeuckerAlgorithm(double epsilon)
	{
		return this.applyRamerDouglasPeuckerAlgorithm(0, this.size() - 1, epsilon);
	}
	
	/**
	 * Calculate the distance between a point and a line
	 *
	 * @param px 	x-coordinate of current point
	 * @param py 	y-coordinate of current point
	 * @param sx 	x-coordinate of start point
	 * @param sy 	y-coordinate of start point
	 * @param lx	x-coordinate of end point
	 * @param ly	y-coordinate of end point
	 * @return Distance between current point and line between start and end point
	 */
	private double calculatePointLineDistance(double px, double py, double sx, double sy, double lx, double ly)
	{
		// start and end point are the same
		if(sx == lx && sy == ly) return Math.sqrt((px - sx) * (px - sx) + (py - sy) * (py - sy));
		
		double n = Math.abs((lx - sx) * (sy - py) - (sx - px) * (ly - sy));
		double d = Math.sqrt((lx - sx) * (lx - sx) + (ly - sy) * (ly - sy));
		
		return n / d;
	}
	
	
}
