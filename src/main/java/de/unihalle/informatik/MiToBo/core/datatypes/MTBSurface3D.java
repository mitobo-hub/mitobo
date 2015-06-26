package de.unihalle.informatik.MiToBo.core.datatypes;

import java.util.Vector;

import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;

/**
 * class representing the surface area of a 3D-object<br/>
 * points are not ordered!
 * 
 * @author glass
 *
 */
@ALDParametrizedClass
public class MTBSurface3D
{
	/**
	 * Points belonging to the region
	 */
	@ALDClassParameter(label = "list of points") 
	private Vector<MTBPoint3D> points;
	
	/**
	 * ID of the region (uniqueness is not guaranteed nor checked!)
	 */
	private int id;
	
	
	/**
	 * 
	 * @param points
	 */
	public MTBSurface3D(Vector<MTBPoint3D> points)
	{
		this.points = points;
		this.id = 0;
	}
	
	
	/**
	 * 
	 * @param points
	 * @param id
	 */
	public MTBSurface3D(Vector<MTBPoint3D> points, int id)
	{
		this.points = points;
		this.id = id;
	}
	
	/**
	 * Append a 3D point to the region. The point is added at the end of the point
	 * list.
	 * 
	 * @param p
	 *          point to add
	 */
	public void addPoint(MTBPoint3D p)
	{
		points.addElement(p);	
	}
	
	
	/**
	 * Set the ID of the region. Uniqueness is not guaranteed nor checked!
	 * 
	 * @param id region ID
	 */
	public void setID(int id)
	{
		this.id = id;
	}

	/**
	 * Return the ID of the region. Uniqueness is publicnot guaranteed nor
	 * checked!
	 * 
	 * @return Region ID
	 */
	public int getID()
	{
		return this.id;
	}

	
	/**
	 * set points of this surface
	 * 
	 * @param points
	 */
	public void setPoints(Vector<MTBPoint3D> points)
	{
		this.points = points;
	}
	
	
	/**
	 * Return the points which are included in the 3D region.
	 * 
	 * @return Region points.
	 */
	public Vector<MTBPoint3D> getPoints()
	{
		return points;
	}
	
	
	/**
	 * 
	 * @return area (number of voxels)
	 */
	public double getArea()
	{
		return this.points.size();
	}
}
