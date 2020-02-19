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

package de.unihalle.informatik.MiToBo.core.datatypes.images;

import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;


/**
 * Class for windowing a MTBImage.
 * Creates a hyperrectangle window over a MTBImage to access sectors of the image (volume) easily.
 * Translation of the window is supported.
 * @author gress
 *
 */
public class MTBImageWindow implements MTBImageManipulator {

	/** size of the window in x-dimension */
	protected int m_sizeX;
	
	/** size of the window in y-dimension */
	protected int m_sizeY;

	/** size of the window in z-dimension */
	protected int m_sizeZ;
	
	/** size of the window in t-dimension */
	protected int m_sizeT;

	/** size of the window in c-dimension */
	protected int m_sizeC;
	
	/** source image on which the window is applied */
	protected MTBImageManipulator m_impulator;

	/** current x-position of the window in the source image */
	protected int m_posX;

	/** current y-position of the window in the source image */
	protected int m_posY;

	/** current z-position of the window in the source image */
	protected int m_posZ;

	/** current t-position of the window in the source image */
	protected int m_posT;

	/** current c-position of the window in the source image */
	protected int m_posC;
	
	/** padding mode for nonexistent values (outside the underlying image), see static finals */
	protected BoundaryPadding m_padMode;
	
	/**
	 * Padding of image: Method of how to obtain pixel values outside the image domain.
	 * - PADDING_ZERO: Values outside the image domain are assumed to be zero.
	 * - PADDING_BORDER: Values outside the image domain correspond to value of nearest pixel in the image domain. 
	 * - PADDING_MIRROR: Values of the image are mirrored outside of the image domain along the image border.
	 * - PADDING_PERIODIC: Values are repeated, i.e. the image is assumed to be periodical with period equal to the image dimensions (as assumed for DFT) 
	 * 
	 * @author Oliver Gress
	 *
	 */
	public enum BoundaryPadding {
		PADDING_ZERO, PADDING_BORDER, PADDING_MIRROR, PADDING_PERIODIC
	}
	
	/**
	 * Create a new window on a given source MTBImageManipulator (i.e. MTBImage or MTBImageWindow).
	 * @param sizeX size of the window in x-dimension
	 * @param sizeY size of the window in y-dimension
	 * @param sizeZ size of the window in z-dimension
	 * @param sizeT size of the window in t-dimension
	 * @param sizeC size of the window in c-dimension
	 * @param sourceImage source image on which the window is applied
	 * @param paddingMode
	 */
	public MTBImageWindow(int sizeX, int sizeY, int sizeZ, int sizeT, int sizeC, MTBImageManipulator sourceImage, BoundaryPadding paddingMode) {
		m_impulator = sourceImage;
		
		m_sizeX = sizeX;
		m_sizeY = sizeY;
		m_sizeZ = sizeZ;
		m_sizeT = sizeT;
		m_sizeC = sizeC;
		
		m_posX = 0;
		m_posY = 0;
		m_posZ = 0;
		m_posT = 0;
		m_posC = 0;
		
		m_padMode = paddingMode;
	}
	
	/**
	 * Set a new source for the window.
	 * The window's position and size stay as they are.
	 * @param sourceImage source image on which the window is applied
	 */
	public void setSource(MTBImageManipulator sourceImage) {
		m_impulator = sourceImage;
	}
	
	public MTBImage createImageFromWindow() {
		MTBImage newImg = MTBImage.createMTBImage(m_sizeX, m_sizeY, m_sizeZ, m_sizeT, m_sizeC, this.getType());
		
		
		for (int c = 0; c < m_sizeC; c++) {
			for (int t = 0; t < m_sizeT; t++) {
				for (int z = 0; z < m_sizeZ; z++) {
					for (int y = 0; y < m_sizeY; y++) {
						for (int x = 0; x < m_sizeX; x++) {
							newImg.putValueDouble(x, y, z, t, c, this.getValueDouble(x, y, z, t, c));
						}
					}
				}
			}
		}
		
		newImg.setTitle(this.getTitle());
		
		newImg.setStepsizeX(this.getStepsizeX());
		newImg.setStepsizeY(this.getStepsizeY());
		newImg.setStepsizeZ(this.getStepsizeZ());
		newImg.setStepsizeT(this.getStepsizeT());
		
		newImg.setUnitX(this.getUnitX());
		newImg.setUnitY(this.getUnitY());
		newImg.setUnitZ(this.getUnitZ());
		newImg.setUnitT(this.getUnitT());
		
		return newImg;
	}

	/**
	 * Get title of underlying image
	 * @return
	 */
	public String getTitle() {
		return m_impulator.getTitle();
	}
	
	/**
	 * Get underlying data type
	 * @return data type ID
	 */
	public MTBImageType getType() {
		return m_impulator.getType();
	}
	
	/**
	 * Get window size in x-dimension
	 */
	public int getSizeX() {
		return m_sizeX;
	}
	
	/**
	 * Get window size in y-dimension
	 */
	public int getSizeY() {
		return m_sizeY;
	}
	
	/**
	 * Get window size in z-dimension
	 */
	public int getSizeZ() {
		return m_sizeZ;
	}

	/**
	 * Get window size in t-dimension
	 */
	public int getSizeT() {
		return m_sizeT;
	}

	/**
	 * Get window size in c-dimension
	 */
	public int getSizeC() {
		return m_sizeC;
	}

	/**
	 * Get the value of the 5D image at coordinate (x,y,z,t,c) as a Double
	 * @param x x-coordinate
	 * @param y y-coordinate
	 * @param z z-coordinate
	 * @param t t-coordinate
	 * @param c c-coordinate
	 * @return voxel value
	 */
	public double getValueDouble(int x, int y, int z, int t, int c) {
		int xp = x+m_posX;
		int yp = y+m_posY;
		int zp = z+m_posZ;
		int tp = t+m_posT;
		int cp = t+m_posC;
		
		if (xp < 0 || xp >= m_impulator.getSizeX()
				|| yp < 0 || yp >= m_impulator.getSizeY()
				|| zp < 0 || zp >= m_impulator.getSizeZ()
				|| tp < 0 || tp >= m_impulator.getSizeT()
				|| cp < 0 || cp >= m_impulator.getSizeC()) {
		// position is outside of underlying image	
			
			if (m_padMode == BoundaryPadding.PADDING_BORDER) {
			// border padding: use value of the nearest border pixel
				
				if (xp < 0)
					xp = 0;
				if (xp >= m_impulator.getSizeX())
					xp = m_impulator.getSizeX()-1;
				if (yp < 0)
					yp = 0;
				if (yp >= m_impulator.getSizeY())
					yp = m_impulator.getSizeY()-1;				
				if (zp < 0)
					zp = 0;
				if (zp >= m_impulator.getSizeZ())
					zp = m_impulator.getSizeZ()-1;				
				if (tp < 0)
					tp = 0;
				if (tp >= m_impulator.getSizeT())
					tp = m_impulator.getSizeT()-1;				
				if (cp < 0)
					cp = 0;
				if (cp >= m_impulator.getSizeC())
					cp = m_impulator.getSizeC()-1;	
				
				return m_impulator.getValueDouble(xp, yp, zp, tp, cp);
			}
			else if (m_padMode == BoundaryPadding.PADDING_MIRROR) {
			// border padding: use value of mirrored value at the border
				
				if (xp < 0)
					xp = -xp;
				if (xp >= m_impulator.getSizeX()) {
					xp = m_impulator.getSizeX() - (xp - m_impulator.getSizeX()) - 2;
					if (xp < 0)
						xp = m_impulator.getSizeX()-1;
				}
				if (yp < 0)
					yp = -yp;
				if (yp >= m_impulator.getSizeY()) {
					yp = m_impulator.getSizeY() - (yp - m_impulator.getSizeY()) - 2;
					if (yp < 0)
						yp = m_impulator.getSizeY()-1;
				}
				if (zp < 0)
					zp = -zp;
				if (zp >= m_impulator.getSizeZ()) {
					zp = m_impulator.getSizeZ() - (zp - m_impulator.getSizeZ()) - 2;
					if (zp < 0)
						zp = m_impulator.getSizeZ()-1;
				}
				if (tp < 0)
					tp = -tp;
				if (tp >= m_impulator.getSizeT()) {
					tp = m_impulator.getSizeT() - (tp - m_impulator.getSizeT()) - 2;
					if (tp < 0)
						tp = m_impulator.getSizeT()-1;		
				}
				if (cp < 0)
					cp = -cp;
				if (cp >= m_impulator.getSizeC()) {
					cp = m_impulator.getSizeC() - (cp - m_impulator.getSizeC()) - 2;	
					if (cp < 0)
						cp = m_impulator.getSizeC()-1;
				}
				
				
				
				return m_impulator.getValueDouble(xp, yp, zp, tp, cp);
			}
			else if (m_padMode == BoundaryPadding.PADDING_PERIODIC) {
				
				if (xp < 0)
					xp += Math.ceil(-xp/(double)m_impulator.getSizeX());
				else if (xp >= m_impulator.getSizeX()) 
					xp = xp % m_impulator.getSizeX();
				
				if (yp < 0)
					yp += Math.ceil(-yp/(double)m_impulator.getSizeY());
				else if (yp >= m_impulator.getSizeY()) 
					yp = yp % m_impulator.getSizeY();
				
				if (zp < 0)
					zp += Math.ceil(-zp/(double)m_impulator.getSizeZ());
				else if (zp >= m_impulator.getSizeZ()) 
					zp = zp % m_impulator.getSizeZ();
				
				if (tp < 0)
					tp += Math.ceil(-tp/(double)m_impulator.getSizeT());
				else if (tp >= m_impulator.getSizeT()) 
					tp = tp % m_impulator.getSizeT();
				
				if (cp < 0)
					cp += Math.ceil(-cp/(double)m_impulator.getSizeC());
				else if (cp >= m_impulator.getSizeC()) 
					cp = cp % m_impulator.getSizeC();
			
				
				return m_impulator.getValueDouble(xp, yp, zp, tp, cp);
			}
			else {
			// Zero padding	
				
				return 0.0;
			}
		}
		else {
		// position is inside the underlying image 
			
			return m_impulator.getValueDouble(xp, yp, zp, tp, cp);
		}
	}

	/**
	 * Get the value of the 5D image at coordinate (x,y,z,t,c) as an Integer
	 * @param x x-coordinate
	 * @param y y-coordinate
	 * @param z z-coordinate
	 * @param t t-coordinate
	 * @param c c-coordinate
	 * @return voxel value
	 */
	public int getValueInt(int x, int y, int z, int t, int c) {
		int xp = x+m_posX;
		int yp = y+m_posY;
		int zp = z+m_posZ;
		int tp = t+m_posT;
		int cp = t+m_posC;
		
		if (xp < 0 || xp >= m_impulator.getSizeX()
				|| yp < 0 || yp >= m_impulator.getSizeY()
				|| zp < 0 || zp >= m_impulator.getSizeZ()
				|| tp < 0 || tp >= m_impulator.getSizeT()
				|| cp < 0 || cp >= m_impulator.getSizeC()) {
			
			if (m_padMode == BoundaryPadding.PADDING_BORDER) {
				if (xp < 0)
					xp = 0;
				if (xp >= m_impulator.getSizeX())
					xp = m_impulator.getSizeX()-1;
				if (yp < 0)
					yp = 0;
				if (yp >= m_impulator.getSizeY())
					yp = m_impulator.getSizeY()-1;				
				if (zp < 0)
					zp = 0;
				if (zp >= m_impulator.getSizeZ())
					zp = m_impulator.getSizeZ()-1;				
				if (tp < 0)
					tp = 0;
				if (tp >= m_impulator.getSizeT())
					tp = m_impulator.getSizeT()-1;				
				if (cp < 0)
					cp = 0;
				if (cp >= m_impulator.getSizeC())
					cp = m_impulator.getSizeC()-1;	
				
				return m_impulator.getValueInt(xp, yp, zp, tp, cp);
			}
			else if (m_padMode == BoundaryPadding.PADDING_MIRROR) {
				// border padding: use value of mirrored at the border
					
				if (xp < 0)
					xp = -xp;
				if (xp >= m_impulator.getSizeX()) {
					xp = m_impulator.getSizeX() - (xp - m_impulator.getSizeX()) - 2;
					if (xp < 0)
						xp = m_impulator.getSizeX()-1;
				}
				if (yp < 0)
					yp = -yp;
				if (yp >= m_impulator.getSizeY()) {
					yp = m_impulator.getSizeY() - (yp - m_impulator.getSizeY()) - 2;
					if (yp < 0)
						yp = m_impulator.getSizeY()-1;
				}
				if (zp < 0)
					zp = -zp;
				if (zp >= m_impulator.getSizeZ()) {
					zp = m_impulator.getSizeZ() - (zp - m_impulator.getSizeZ()) - 2;
					if (zp < 0)
						zp = m_impulator.getSizeZ()-1;
				}
				if (tp < 0)
					tp = -tp;
				if (tp >= m_impulator.getSizeT()) {
					tp = m_impulator.getSizeT() - (tp - m_impulator.getSizeT()) - 2;
					if (tp < 0)
						tp = m_impulator.getSizeT()-1;		
				}
				if (cp < 0)
					cp = -cp;
				if (cp >= m_impulator.getSizeC()) {
					cp = m_impulator.getSizeC() - (cp - m_impulator.getSizeC()) - 2;	
					if (cp < 0)
						cp = m_impulator.getSizeC()-1;
				}
					
				return m_impulator.getValueInt(xp, yp, zp, tp, cp);
			}
			else if (m_padMode == BoundaryPadding.PADDING_PERIODIC) {
				
				if (xp < 0)
					xp += Math.ceil(-xp/(double)m_impulator.getSizeX());
				else if (xp >= m_impulator.getSizeX()) 
					xp = xp % m_impulator.getSizeX();
				
				if (yp < 0)
					yp += Math.ceil(-yp/(double)m_impulator.getSizeY());
				else if (yp >= m_impulator.getSizeY()) 
					yp = yp % m_impulator.getSizeY();
				
				if (zp < 0)
					zp += Math.ceil(-zp/(double)m_impulator.getSizeZ());
				else if (zp >= m_impulator.getSizeZ()) 
					zp = zp % m_impulator.getSizeZ();
				
				if (tp < 0)
					tp += Math.ceil(-tp/(double)m_impulator.getSizeT());
				else if (tp >= m_impulator.getSizeT()) 
					tp = tp % m_impulator.getSizeT();
				
				if (cp < 0)
					cp += Math.ceil(-cp/(double)m_impulator.getSizeC());
				else if (cp >= m_impulator.getSizeC()) 
					cp = cp % m_impulator.getSizeC();
			
				
				return m_impulator.getValueInt(xp, yp, zp, tp, cp);
			}
			else {
				return 0;
			}
			
		}
		else {
			return m_impulator.getValueInt(xp, yp, zp, tp, cp);
		}
	}

	/**
	 * Set the value of the 5D image at coordinate (x,y,z,t,c) using a Double
	 * @param x x-coordinate
	 * @param y y-coordinate
	 * @param z z-coordinate
	 * @param t t-coordinate
	 * @param c c-coordinate
	 * @param value to set the voxel to 
	 */	
	public void putValueDouble(int x, int y, int z, int t, int c, double value) {
		if (x+m_posX >= 0 && x+m_posX < m_impulator.getSizeX()
				&& y+m_posY >= 0 && y+m_posY < m_impulator.getSizeY()
				&& z+m_posZ >= 0 && z+m_posZ < m_impulator.getSizeZ()
				&& t+m_posT >= 0 && t+m_posT < m_impulator.getSizeT()
				&& c+m_posC >= 0 && c+m_posC < m_impulator.getSizeC()) {
			
			m_impulator.putValueDouble(x+m_posX, y+m_posY, z+m_posZ, t+m_posT, c+m_posC, value);
		}
	}

	/**
	 * Set the value of the 5D image at coordinate (x,y,z,t,c) using an Integer
	 * @param x x-coordinate
	 * @param y y-coordinate
	 * @param z z-coordinate
	 * @param t t-coordinate
	 * @param c c-coordinate
	 * @param value to set the voxel to 
	 */	
	public void putValueInt(int x, int y, int z, int t, int c, int value) {
		if (x+m_posX >= 0 && x+m_posX < m_impulator.getSizeX()
				&& y+m_posY >= 0 && y+m_posY < m_impulator.getSizeY()
				&& z+m_posZ >= 0 && z+m_posZ < m_impulator.getSizeZ()
				&& t+m_posT >= 0 && t+m_posT < m_impulator.getSizeT()
				&& c+m_posC >= 0 && c+m_posC < m_impulator.getSizeC()) {
			
			m_impulator.putValueInt(x+m_posX, y+m_posY, z+m_posZ, t+m_posT, c+m_posC, value);
		}
	}
	
	/**
	 * Set the window's position in the source image.
	 * Set the position of the (upper left) window coordinate (0,0,0,0,0)
	 * to the specified position in the source image.
	 * @param x x-coordinate in source image
	 * @param y y-coordinate in source image
	 * @param z z-coordinate in source image
	 * @param t t-coordinate in source image
	 * @param c c-coordinate in source image
	 */
	public void setWindowPosition(int x, int y, int z, int t, int c) {

		m_posX = x;
		m_posY = y;	
		m_posZ = z;
		m_posT = t;
		m_posC = c;
	}
	
	/**
	 * Get the current window position
	 * @return window position ([0]=x-position, [1]=y-position, [2]=z-position, [3]=t-position, [4]=c-position) 
	 */
	public int[] getWindowPosition() {
		int[] pos = new int[5];
		
		pos[0] = m_posX;
		pos[1] = m_posY;
		pos[2] = m_posZ;
		pos[3] = m_posT;
		pos[4] = m_posC;
		
		return pos;
	}
	
	/**
	 * Increment the window's position by 1 in the x-dimension
	 */
	public void incrPositionX() {
		m_posX++;
	}
	
	/**
	 * Increment the window's position by 'increment' in the x-dimension
	 * @param increment
	 */
	public void incrPositionX(int increment) {
		m_posX += increment;
	}	
	
	/**
	 * Increment the window's position by 1 in the y-dimension
	 */
	public void incrPositionY() {
		m_posY++;
	}
	
	/**
	 * Increment the window's position by 'increment' in the y-dimension
	 * @param increment
	 */
	public void incrPositionY(int increment) {
		m_posY += increment;
	}
	
	/**
	 * Increment the window's position by 1 in the z-dimension
	 */
	public void incrPositionZ() {
		m_posZ++;
	}
	
	/**
	 * Increment the window's position by 'increment' in the z-dimension
	 * @param increment
	 */
	public void incrPositionZ(int increment) {
		m_posZ += increment;
	}
	
	/**
	 * Increment the window's position by 1 in the t-dimension
	 */
	public void incrPositionT() {
		m_posT++;
	}
	
	/**
	 * Increment the window's position by 'increment' in the t-dimension
	 * @param increment
	 */
	public void incrPositionT(int increment) {
		m_posT += increment;
	}
	
	/**
	 * Increment the window's position by 1 in the c-dimension
	 */
	public void incrPositionC() {
		m_posC++;
	}
	
	/**
	 * Increment the window's position by 'increment' in the c-dimension
	 * @param increment
	 */
	public void incrPositionC(int increment) {
		m_posC += increment;
	}
	
	/**
	 * Set the window's position to 'x' in the x-dimension
	 * @param x
	 */
	public void setPositionX(int x) {
		m_posX = x;
	}	
	
	/**
	 * Set the window's position to 'y' in the y-dimension
	 * @param y
	 */
	public void setPositionY(int y) {
		m_posY = y;
	}	
	
	/**
	 * Set the window's position to 'z' in the z-dimension
	 * @param z
	 */
	public void setPositionZ(int z) {
		m_posZ = z;
	}	
	
	/**
	 * Set the window's position to 't' in the t-dimension
	 * @param t
	 */
	public void setPositionT(int t) {
		m_posT = t;
	}	
	
	/**
	 * Set the window's position to 'c' in the c-dimension
	 * @param c
	 */
	public void setPositionC(int c) {
		m_posC = c;
	}
	
	/**
	 * Get the physical size of a voxel (stepsize) in x-dimension 
	 * @return physical voxel size in x-dimension
	 */
	public double getStepsizeX() {
		return m_impulator.getStepsizeX();
	}

	/**
	 * Get the physical size of a voxel (stepsize) in y-dimension 
	 * @return physical voxel size in y-dimension
	 */
	public double getStepsizeY() {
		return m_impulator.getStepsizeY();
	}

	/**
	 * Get the physical size of a voxel (stepsize) in z-dimension 
	 * @return physical voxel size in z-dimension
	 */
	public double getStepsizeZ() {
		return m_impulator.getStepsizeZ();
	}

	/**
	 * Get the stepsize in t-dimension (timestep)
	 * @return time stepsize
	 */
	public double getStepsizeT() {
		return m_impulator.getStepsizeT();
	}
	
	/**
	 * Get the unit of the x-dimension
	 * @return String of x-dimension's unit
	 */
	public String getUnitX() {
		return m_impulator.getUnitX();
	}
	
	/**
	 * Get the unit of the y-dimension
	 * @return String of y-dimension's unit
	 */
	public String getUnitY() {
		return m_impulator.getUnitY();
	}	
	
	/**
	 * Get the unit of the z-dimension
	 * @return String of z-dimension's unit
	 */
	public String getUnitZ() {
		return m_impulator.getUnitZ();
	}	
	
	/**
	 * Get the unit of the t-dimension
	 * @return String of t-dimension's unit
	 */
	public String getUnitT() {
		return m_impulator.getUnitT();
	}
}
