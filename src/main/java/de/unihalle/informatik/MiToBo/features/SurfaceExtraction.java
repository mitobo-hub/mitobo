package de.unihalle.informatik.MiToBo.features;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBPoint3D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBSurface3D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBSurface3DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;

/**
 * Operator for extracting the surface areas of 3D-objects.<br/>
 * Currently, it is not checked if objects contain holes, so voxels bordering holes are included into the surface!<br/>
 * 
 * @author glass
 *
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.SWING, level=Level.STANDARD)
public class SurfaceExtraction extends MTBOperator
{
	@Parameter(label = "label image", required = true, direction = Parameter.Direction.IN, supplemental = false, description = "input label image")
	private transient MTBImage labelImg = null;
	
	@Parameter(label = "are objects 26-connected", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "are objects 26-connected (6-connected otherwise)")
	private Boolean objects26Connected = false;
	
	@Parameter(label = "surfaces", required = true, direction = Parameter.Direction.OUT, supplemental = false, description = "surfaces of the given labeled objects")
	MTBSurface3DSet surfaces = null;
	
	private Double bg_value = 0.0;
	
	private int sizeX;
	private int sizeY;
	private int sizeZ;
	
	public SurfaceExtraction() throws ALDOperatorException
	{
		
	}
	
	
	public SurfaceExtraction(MTBImage labelImg, boolean objects26Connected) throws ALDOperatorException
	{
		this.labelImg = labelImg;
		this.objects26Connected = objects26Connected;
	}

	
	@Override
	protected void operate() throws ALDOperatorException, ALDProcessingDAGException
	{
		sizeX = labelImg.getSizeX();
		sizeY = labelImg.getSizeY();
		sizeZ = labelImg.getSizeZ();
		
		surfaces = new MTBSurface3DSet(0, 0, 0, sizeX-1, sizeY-1, sizeZ-1);
		Hashtable<Integer, Vector<MTBPoint3D>> pointList = new Hashtable<Integer, Vector<MTBPoint3D>>(); 
		
		for(int z = 0; z < sizeZ; z++)
		{
			for(int y = 0; y < sizeY; y++)
			{
				for(int x = 0; x < sizeX; x++)
				{
					int label = labelImg.getValueInt(x, y, z);
					
					if(label != bg_value)
					{
						boolean isSurfacePoint;
						
						if(objects26Connected)
						{
							isSurfacePoint = hasForeign6Neighbor(x, y, z, label);
						}
						else
						{
							isSurfacePoint = hasForeign26Neighbor(x, y, z, label);
						}
						
						if(isSurfacePoint)
						{
							if(pointList.containsKey(label))
							{
								pointList.get(label).add(new MTBPoint3D(x, y, z));
							}
							else
							{
								Vector<MTBPoint3D> v = new Vector<MTBPoint3D>();
								v.add(new MTBPoint3D(x, y, z));
								pointList.put(label, v);
							}
						}
					}		
				}
			}
		}
		
		Enumeration<Integer> enumKeys = pointList.keys();
		
		while(enumKeys.hasMoreElements())
		{
			int label = enumKeys.nextElement();
			MTBSurface3D s = new MTBSurface3D(pointList.get(label), label);
			surfaces.add(s);
		}
		
		if(verbose)
		{
			surfaces.getSurfaceImage().show();
		}
	}

	
	private boolean hasForeign6Neighbor(int x, int y, int z, int label)
	{
		for(int i = -1; i <= 1; i++)
		{
			if(x + i < 0 || x + i > sizeX-1 || labelImg.getValueInt(x + i, y, z) != label)
			{
				return true;
			}
			
			if(y + i < 0 || y + i > sizeY-1 || labelImg.getValueInt(x, y + i, z) != label)
			{
				return true;
			}
			
			if(z + i < 0 || z + i > sizeZ-1 || labelImg.getValueInt(x, y, z + i) != label)
			{
				return true;
			}
		}
		
		return false;
	}
	
	
	private boolean hasForeign26Neighbor(int x, int y, int z, int label)
	{
		for(int k = -1; k <= 1; k++)
		{
			for(int j = -1; j <= 1; j++)
			{
				for(int i = -1; i <= 1; i++)
				{
					
					if(x+i < 0 || x+i > sizeX -1 || y+j < 0 || y+j > sizeY - 1 || z+k < 0 || z+k > sizeZ - 1 || labelImg.getValueInt(x + i, y + j, z + k) != label)
					{
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
	
	/**
	 * 
	 * @return the extracted surfaces
	 */
	public MTBSurface3DSet getSurfaces()
	{
		return this.surfaces;
	}
}
