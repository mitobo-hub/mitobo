package de.unihalle.informatik.MiToBo.core.datatypes;

import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;

/**
 * class representing a 2D flat (, i.e. without different gray values) structuring element<br/>
 * for the use with morphological operations<br/>
 * values are stored in a rectangular 2D array (rows: 1st dimension, columns: 2nd dimension)<br/>
 * whereas onValue represents set locations and 0 represents unset locations
 * 
 * @author glass
 *
 */
@ALDParametrizedClass
public class MTBStructuringElement
{
	@ALDClassParameter(label="mask")
	private int[][] mask;
	
	//private int sizeX;
	//private int sizeY;
	
	@ALDClassParameter(label="centerX")
	private int centerX;
	
	@ALDClassParameter(label="centerY")
	private int centerY;
	
	private final static int onValue = 1;
	
	
	/**
	 * creates structuring element from given 2D-array
	 * 
	 * @param mask
	 * @param centerX
	 * @param centerY
	 */
	public MTBStructuringElement(int[][] mask, int centerX, int centerY)
	{
		int sizeX = mask[0].length;
		int sizeY = mask.length;
		
		// if given center coordinates exceed mask boundaries, center is set to the center of the given array
		if(centerX >= 0 && centerX < sizeX && centerY >= 0 && centerY < sizeY)
		{
			this.centerX = centerX;
			this.centerY = centerY;
		}
		else
		{
			this.centerX = sizeX / 2;
			this.centerY = sizeY / 2;
		}
		
		this.mask = mask;
	}
	
	
	/**
	 * creates structuring element from given 2D-array, center is assumed to be at the center of
	 * the array
	 * 
	 * @param mask
	 */
	public MTBStructuringElement(int[][] mask)
	{
		this(mask, mask[0].length/2, mask.length/2);
	}
	
	
	/**
	 * create quadratic 3x3 structuring element
	 */
	public MTBStructuringElement()
	{	
		this.mask = new int[][]{{onValue,onValue,onValue}, {onValue,onValue,onValue}, {onValue,onValue,onValue}};
		this.centerX = 1;
		this.centerY = 1;
//		this(new int[3][3]);
	}
	
	
	/**
	 * 
	 * @param x
	 * @param y
	 * @return value at (x,y)
	 */
	public int getValue(int x, int y)
	{
		return mask[y][x];
	}
	
	
	/**
	 * 
	 * @return size in x-direction (width)
	 */
	public int getSizeX()
	{
		return mask[0].length;
	}
	
	
	/**
	 * 
	 * @return size in y-direction (height)
	 */
	public int getSizeY()
	{
		return mask.length;
	}
	
	
	/**
	 * 
	 * @return x-coordinate of center
	 */
	public int getCenterX()
	{
		return centerX;
	}
	
	
	/**
	 * 
	 * @return y-coordinate of center
	 */
	public int getCenterY()
	{
		return centerY;
	}
	
	
	/**
	 * 
	 * @param size 
	 * @return quadratic shaped structuring element
	 */
	public static MTBStructuringElement createQuadraticElement(int size)
	{
		int[][] m = new int[size][size];
		int center = size / 2;
		
		for(int y = 0; y < size; y++)
		{	
			for(int x = 0; x < size; x++)
			{
				m[y][x] = onValue;
			}
		}
		
		
		return new MTBStructuringElement(m, center,center);
	}
	
	
	/**
	 * 
	 * @param sizeX
	 * @param sizeY
	 * @return rectangular shaped structuring element
	 */
	public static MTBStructuringElement createRectangularElement(int sizeX, int sizeY)
	{
		int[][] m = new int[sizeY][sizeX];
		int centerX = sizeX / 2;
		int centerY = sizeY / 2;
		
		for(int x = 0; x < sizeX; x++)
		{	
			for(int y = 0; y < sizeY; y++)
			{
				m[y][x] = onValue;
			}
		}
		
		return new MTBStructuringElement(m, centerX, centerY);
	}
	
	
	/**
	 * 
	 * @param size
	 * @return circular shaped structuring element
	 */
	public static MTBStructuringElement createCircularElement(int size)
	{
		int radius = size / 2;
		int center = radius;
		float radiusSqr = radius * radius;
		
		int[][] m = new int[size][size];
		
		for(int y = 0; y < size; y++)
		{	
			for(int x = 0; x < size; x++)
			{
				float xVal = (float)((x - center) * (x - center));
				float yVal = (float)((y - center) * (y - center));
				
				if((xVal + yVal) <= radiusSqr)
				{
					m[y][x] = onValue;
				}
				else
				{
					m[y][x] = 0;
				}
			}
		}
		
		
		return new MTBStructuringElement(m, (int)center,(int)center);
	}
	
	
	/**
	 * 
	 * @param length
	 * @param value
	 * @return  line shaped structuring element, horizontally (in x-direction) oriented
	 */
	public static MTBStructuringElement createHorizontalLineElement(int length)
	{
		int[][] m = new int[1][length];
		int center = length / 2;
			
		for(int x = 0; x < length; x++)
		{
			m[0][x] = onValue;
		}
		
		
		return new MTBStructuringElement(m, center,0);
	}
	
	
	/**
	 * 
	 * @param length
	 * @return line shaped structuring element, vertically (in y-direction) oriented
	 */
	public static MTBStructuringElement createVerticalLineElement(int length)
	{
		int[][] m = new int[length][1];
		int center = length / 2;
			
		for(int y = 0; y < length; y++)
		{
			m[y][0] = onValue;
		}
		
		
		return new MTBStructuringElement(m, 0, center);
	}
	
	
	/**
	 * inverts the values of the structuring element
	 * 
	 * @return inverted structuring element
	 */
	public MTBStructuringElement invert()
	{	
		int sizeX = mask[0].length;
		int sizeY = mask.length;
		
		int[][] inv = new int[sizeY][sizeX];
		
		for(int y = 0; y < sizeY; y++)
		{
			for(int x = 0; x < sizeX; x++)
			{
				if(mask[y][x] == onValue)
				{
					inv[y][x] = 0;
				}
				else
				{
					inv[y][x] = onValue;
				}
			}
		}
		
		return new MTBStructuringElement(inv, this.centerX, this.centerY);
	}
	
	
	/**
	 * reflects the structuring element around its center
	 * 
	 * @return around its center reflected structuring element
	 */
	public MTBStructuringElement reflect()
	{
		int sizeX = mask[0].length;
		int sizeY = mask.length;
		
		int[][] ref = new int[sizeY][sizeX];
		
		int newCenterX = (sizeX - centerX) - 1;
		int newCenterY = (sizeY - centerY) - 1;
		
		for(int y = 0; y < sizeY; y++)
		{
			for(int x = 0; x < sizeX; x++)
			{
				if(mask[y][x] == onValue)
				{
					ref[(sizeY - y) - 1][(sizeX - x) -1] = onValue;
				}
			}
		}
		
		return new MTBStructuringElement(ref, newCenterX, newCenterY);
	}
}
