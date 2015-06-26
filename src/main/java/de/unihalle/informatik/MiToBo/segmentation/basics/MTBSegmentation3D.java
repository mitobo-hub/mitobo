/*
 * This file is part of MiToBo, the Microscope Image Analysis Toolbox.
 *
 * Copyright (C) 2011
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

package de.unihalle.informatik.MiToBo.segmentation.basics;

import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;

/**
 * Class for representing 3D segmentations of image data.
 *
 * @author Birgit Moeller
 */
public class MTBSegmentation3D implements  MTBSegmentationInterface {

	/**
	 * Number of represented regions.
	 */
	protected int numRegions;
	
	/**
	 * Maximal label appearing in the label list.
	 */
	protected int maxLabel;
	
	/**
	 * Height of segmentation domain.
	 */
	int height = 0;

	/**
	 * Width of segmentation domain.
	 */
	int width = 0;
	
	/**
	 * Depth of segmentation domain.
	 */
	int depth = 0;
	
	/**
	 * 2D map with class labels.
	 */
	protected int [][][] classmap;
	
	/**
	 * Visibility map for hiding individual pixels.
	 */
	protected boolean [][][] visiblemap;

	/**
	 * Map of weights for the segmentation.
	 */
	protected double [][][] weightmap;

	/**
	 * Constructor.
	 * 
	 * @param w						Width of segmentation domain.
	 * @param h						Height of segmentation domain.
	 * @param d						Depth of segmentation domain.
	 * @param numClasses	Number of segmented classes.
	 * @param cmap				Map of class labels.
	 * @param vmap				Visibility map.
	 * @param wmap				Map of pixels weights.
	 */
	public MTBSegmentation3D (int w, int h, int d, int numClasses,
										int[][][] cmap, boolean[][][] vmap, double[][][] wmap) {
		this.width = w;
		this.height = h;
		this.depth = d;
		this.numRegions = numClasses;
		this.classmap = cmap;
		this.weightmap = wmap;
		for (int z=0;z<this.depth;++z)
			for (int y=0;y<this.height;++y)
				for (int x=0;x<this.width;++x)
					this.maxLabel= 
						( cmap[z][y][x]>this.maxLabel ? cmap[z][y][x] : this.maxLabel ); 
		this.visiblemap = vmap;
	}
	
	/**
	 * Returns the dimension of the given membership.
	 */
	@Override
  public SegmentationDimension getDimension() {
		return SegmentationDimension.dim_3;
	}
	
	/**
	 * Returns the number of classes represented in the membership.
	 */
	@Override
  public int getNumberOfClasses() {
		return this.numRegions;
	}
	
	/**
	 * Returns the maximal label used in the membership representation.
	 * <p>
	 * Note that the maximal label is not required to coincide with the 
	 * number of classes represented by the membership function.
	 * Labels should always be larger or equal to zero.
	 */
	@Override
  public int getMaxLabel() {
		return this.maxLabel;
	}
	
	@Override
  public boolean isVisible(int x, int y) {
		return this.isVisible(x,y,0);
	}

	@Override
  public boolean isVisible(int x, int y, int z) {
		return this.visiblemap[z][y][x];		
	}
	
	@Override
  public int getClass(int x, int y) {
		if (this.isVisible(x, y))
			return this.getClass(x,y,0);
		return -1;
	}
	
	@Override
  public int getClass(int x, int y, int z) {
		return this.classmap[z][y][x];
	}
	
	@Override
  public void setClass(int x, int y, int c) {
		this.setClass(x,y,0,c);
	}
	
	@Override
  public void setClass(int x, int y, int z, int c) {
		this.classmap[z][y][x] = c;
	}

	@Override
  public void setVisible(int x, int y, int z) {
		this.visiblemap[z][y][x] = true;
  }

	@Override
  public void setVisible(int x, int y) {
		this.setVisible(x,y,0);
  }

	@Override
  public void setInvisible(int x, int y, int z) {
		this.visiblemap[z][y][x] = false;
	}

	@Override
  public void setInvisible(int x, int y) {
		this.setInvisible(x, y, 0);
  }

	/**
	 * Set the valid mask from external.
	 */
	public void setVisibilityMask(boolean [][][] mask) {
		this.visiblemap = mask.clone();
	}

	@Override
  public int getSizeX() {
		return this.width;
  }

	@Override
  public int getSizeY() {
		return this.height;
  }

	@Override
  public int getSizeZ() {
		return this.depth;
  }

	@Override
  public double getWeight(int x, int y) {
	  return this.getWeight(x, y, 0);
  }

	@Override
  public double getWeight(int x, int y, int z) {
	  return this.weightmap[z][y][x];
  }

	@Override
  public void setWeight(int x, int y, double c) {
		this.setWeight(x, y, 0, c);
	}

	@Override
  public void setWeight(int x, int y, int z, double c) {
		this.weightmap[z][y][x] = c;
	}

    @Override
    public MTBImage getMask(int class_)
    {
        MTBImage mask = MTBImage.createMTBImage(getSizeX(),getSizeY(),getSizeZ(),1,1, MTBImage.MTBImageType.MTB_BYTE);

        for (int z = 0; z < getSizeZ(); z++)
        {
            for (int y = 0; y < getSizeY(); y++)
            {
                for (int x = 0; x < getSizeX(); x++)
                {
                    if(getClass(x,y,z) == class_)
                        mask.putValueInt(x,y,z,255);
                    else
                        mask.putValueInt(x,y,z,0);
                }
            }
        }

        return mask;
    }
}
