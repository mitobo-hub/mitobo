package de.unihalle.informatik.MiToBo.core.datatypes;

import java.util.Vector;

import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.Alida.operator.ALDData;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;

/**
 * 
 * @author glass
 *
 */
@ALDParametrizedClass
public class MTBSurface3DSet extends ALDData
{
	/** 
	 * Minimal x coordinate of the domain of this region set.
	 */
	@ALDClassParameter(label = "Minimal x in domain.")
	private double xMin;

	/** 
	 * Minimal y coordinate of the domain of this region set.
	 */
	@ALDClassParameter(label = "Minimal y in domain")
	private double yMin;
	
	/** 
	 * Minimal z coordinate of the domain of this region set.
	 */
	@ALDClassParameter(label = "Minimal z in domain")
	private double zMin;
	
	/** 
	 * Maximal x coordinate of the domain of this region set.
	 */
	@ALDClassParameter(label = "Maximal x in domain")
	private double xMax;

	/** 
	 * Maximal y coordinate of the domain of this region set.
	 */
	@ALDClassParameter(label = "Maximal y in domain")
	private double yMax;

	/** 
	 * Maximal z coordinate of the domain of this region set.
	 */
	@ALDClassParameter(label = "Maximal z in domain")
	private double zMax;
	
	/** 
	 * The set of surfaces represented as a Vector.
	 */
	@ALDClassParameter(label = "List of surfaces")
	Vector<MTBSurface3D> surfaceSet;

	/** Construct an empty set of regions with given extent of domain
	 */
	public MTBSurface3DSet(double xMin, double yMin, double zMin, double xMax, double yMax, double zMax)
	{
		this.xMin = xMin;
		this.yMin = yMin;
		this.zMin = zMin;
		this.xMax = xMax;
		this.yMax = yMax;
		this.zMax = zMax;
		
		surfaceSet = new Vector<MTBSurface3D>();

		setProperty( "xMin", xMin);
		setProperty( "yMin", yMin);
		setProperty( "zMin", zMin);
		setProperty( "xMax", xMax);
		setProperty( "yMax", yMax);
		setProperty( "zMax", zMax);
	}
	
	
	public double getXmin() {
		return this.xMin;
	}
	public double getXmax() {
		return this.xMax;
	}
	public double getYmin() {
		return this.yMin;
	}
	public double getYmax() {
		return this.yMax;
	}
	public double getZmin() {
		return this.zMin;
	}
	public double getZmax() {
		return this.zMax;
	}
	
	/** 
	 * Get the number of regions of this region set
	 * 
	 * @return number of regions
	 */
	public int size()
	{
		return surfaceSet.size();
	}

	/**
	 * Remove all regions from this set
	 */
	public void clear()
	{
		this.surfaceSet.clear();
	}
	
	/** Get a region by index
	 *
	 * @return i-th region
	 */
	public MTBSurface3D get(int i)
	{
		return this.surfaceSet.get(i);
	}
	
	/** Get a region by index
	 *
	 * @return i-th region
	 */
	public MTBSurface3D elementAt(int i)
	{
		if(i < 0 || i >= size())
		{
			throw new ArrayIndexOutOfBoundsException();
		}	

		return surfaceSet.elementAt( i);
	}

	
	/** 
	 * Append a region (at the end) to the set of regions
	 * 
	 * @param 	region	region to add
	 */
	public boolean add(MTBSurface3D surface)
	{
		return surfaceSet.add(surface);
	}

	
	/** 
	 * Set the i-th region from the set
	 * 
	 * @param 	i	index of region to remove
	 */
	public void setElementAt(MTBSurface3D region, int i)
	{
		surfaceSet.setElementAt(region, i);
	}

	
	/** 
	 * Delete the i-th region from the set
	 * 
	 * @param 	i	index of region to remove
	 */
	public void removeElementAt(int i)
	{
		surfaceSet.removeElementAt(i);
	}
	
	
	/**
	 * draw surfaces into an image with the surface ids as labels
	 * 
	 * @return surface label image
	 */
	public MTBImage getSurfaceImage()
	{
		MTBImage surfaceImg = MTBImage.createMTBImage((int)(xMax + 1), (int)(yMax + 1), (int)(zMax + 1), 1, 1, MTBImage.MTBImageType.MTB_SHORT);
		
		for(int i = 0; i < surfaceSet.size(); i++)
		{
			Vector<MTBPoint3D> points = surfaceSet.elementAt(i).getPoints();
			int label = surfaceSet.elementAt(i).getID();
			
			for(int j = 0; j < points.size(); j++)
			{
				MTBPoint3D p = points.elementAt(j);
				surfaceImg.putValueInt((int)p.x, (int)p.y, (int)p.z, label);
			}
		}
		
		return surfaceImg;
	}
}
