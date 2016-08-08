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

package de.unihalle.informatik.MiToBo.segmentation.levelset.PDE.datatypes;

import java.util.Iterator;
import java.util.ArrayList;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.operator.ALDOperator;
import de.unihalle.informatik.Alida.operator.ALDOperator.HidingMode;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageRGB;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageShort;
import de.unihalle.informatik.MiToBo.segmentation.basics.MTBSegmentationInterface;
import de.unihalle.informatik.MiToBo.segmentation.levelset.core.MTBLevelsetFunctionDerivable;
import de.unihalle.informatik.MiToBo.visualization.colormappings.ArrayToColorMapImage;

import java.util.HashMap;

import de.unihalle.informatik.MiToBo.core.datatypes.MTBPoint3D;

/**
 * Class representing a 3-dimensional level set function.
 * <p> 
 * The representation includes two classes with labels 0 and 1. Label 0 refers 
 * to the background including all pixels in the level set function's domain 
 * where the value of the function is smaller than zero. Class 1, i.e. the 
 * foreground phase of the level set function, is linked to all pixels in the 
 * domain with function values larger than zero.
 * <p>
 * The levelset function can be transformed into a signed-distance function.
 * 
 * @author Martin Scharm
 * @author Michael Schneider
 * @author Birgit Moeller
 */
public class MTBLevelsetFunctionPDE extends MTBLevelsetFunctionDerivable 
        implements Cloneable {

	/**
	 * Constant used in SDF calculations.
	 */
	public static final double MAX_VALUE = Integer.MAX_VALUE / 2;

	/**
	 * The level set function.
	 */
	protected double[][][] phi;
	/**
	 * Visibility array.
	 */
	protected boolean[][][] visible;
	/**
	 * Flag to indicate if function is signed or not.
	 */
//	private boolean signed;
	/**
	 * Height of the input image and levelset function.
	 */
	protected int height;
	/**
	 * Width of the input image and levelset function.
	 */
	protected int width;
	/**
	 * Depth of the input image and levelset function.
	 */
	protected int depth;
	/**
	 * Safety distance along image borders.
	 * <p>
	 * Pixels closer to border than this distance are ignored in calculations.
	 */
	protected int borderDist = 2;
	/**
	 * Pixels in narrow-band.
	 * <p>
	 * The array is updated on transforming the function into a SDF.
	 */
	protected ArrayList<MTBPoint3D> narrow;
	/**
	 * Map of precursors on contour for each pixel in narrow-band.
	 * <p>
	 * The map is updated on transforming the function into a SDF.
	 */
	protected HashMap<MTBPoint3D,MTBPoint3D> predecessors = 
		new HashMap<MTBPoint3D, MTBPoint3D>();
	
	/* local helpers to save memory */
	
	/**
	 * 2D array of first two levelset dimension used for visualization
	 */
	private double[][] helperArray;

	
	private boolean DEFAULT_FALL_BACK;

	/**
	 * Create a new levelset function.
	 * <p>
	 * The default handle for calculations of derivatives along borders is set
	 * to fall back to other methods to calculate the derivatives.
	 *
	 * @param w 	Width of function.
	 * @param h		Height of function.
	 * @param d		Depth of function.
	 */
	public MTBLevelsetFunctionPDE(int w, int h, int d) {
		this.DEFAULT_FALL_BACK = true;
		this.width = w;
		this.height = h;
		this.depth = d;
		this.narrow = null;
		//		this.signed = false;
		this.phi = new double[w][h][d];
		this.visible = new boolean[w][h][d];
		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {
				for (int z = 0; z < d; z++) {
					this.visible[x][y][z] = true;
				}
			}
		} 
	}

	/**
	 * Constructor to init empty function with safety border.
	 * @param w				Width of levelset function (x-size).
	 * @param h				Height of levelset function (y-size).
	 * @param d				Depth of levelset function (z-size).
	 * @param border	Width of border to ignore in calculations.
	 */
	public MTBLevelsetFunctionPDE(int w, int h, int d, int border) {
		this(w, h, d);
		this.borderDist = border;
	}

	/**
	 * create a new levelsetfunction, setting a default handle for derivations
	 * on borders
	 *
	 * @param w length of function
	 * @param h height of function
	 * @param d depth of function
	 * @param default_fallback default strategy for border, true -> fall back,
	 * false -> cancel with NaN
	 */
	public MTBLevelsetFunctionPDE(int w, int h, int d, boolean default_fallback) {
		this.width = w;
		this.height = h;
		this.depth = d;
		this.DEFAULT_FALL_BACK = default_fallback;
		this.narrow = null;
//		this.signed = false;
		this.phi = new double[this.width][this.height][this.depth];
		this.visible = new boolean[this.width][this.height][this.depth];
		for (int x = 0; x < this.width; x++) {
			for (int y = 0; y < this.height; y++) {
				for (int z = 0; z < this.depth; z++) {
					this.visible[x][y][z] = true;
				}
			}
		}
	}

	/**
	 * create a new levelsetfunction, setting a default handle for derivations
	 * on borders
	 *
	 * @param width length of function
	 * @param height height of function
	 * @param depth depth of function
	 * false -> cancel with NaN
	 */
	public MTBLevelsetFunctionPDE(boolean nb_diag, int w, int h, int d) {
		this.DEFAULT_FALL_BACK = true;
		this.width = w;
		this.height = h;
		this.depth = d;
		this.narrow = null;
//		this.signed = false;
		this.phi = new double[this.width][this.height][this.depth];
		this.visible = new boolean[this.width][this.height][this.depth];
		for (int x = 0; x < this.width; x++) {
			for (int y = 0; y < this.height; y++) {
				for (int z = 0; z < this.depth; z++) {
					this.visible[x][y][z] = true;
				}
			}
		}
	}

	/**
	 * create a new levelsetfunction, setting a default handle for derivations
	 * on borders
	 *
	 * @param width length of function
	 * @param height height of function
	 * @param depth depth of function
	 * @param default_fallback default strategy for border, true -> fall back,
	 * false -> cancel with NaN
	 */
	public MTBLevelsetFunctionPDE(boolean nb_diag, int w, int h, int d, boolean default_fallback) {
		this.DEFAULT_FALL_BACK = default_fallback;
		this.width = w;
		this.height = h;
		this.depth = d;
		this.narrow = null;
//		this.signed = false;
		this.phi = new double[this.width][this.height][this.depth];
		this.visible = new boolean[this.width][this.height][this.depth];
		for (int x = 0; x < this.width; x++) {
			for (int y = 0; y < this.height; y++) {
				for (int z = 0; z < this.depth; z++) {
					this.visible[x][y][z] = true;
				}
			}
		}
	}

	/**
	 * Constructor to build a level set function from a segmentation object.
	 * <p>
	 * The level set function is initialized with value -1 for the background
	 * and +1 for the foreground. Note that the size of the given segmentation
	 * is allowed to differ from the target size of the function. Positions being
	 * undefined in the segmentation are assigned to the background class.
	 *  
	 * @param w			Target width of the level set function domain.
	 * @param h			Target height of the level set function domain.
	 * @param d			Target depth of the level set function domain.
	 * @param seg		Initial segmentation.
	 * @param default_fallback
	 */
	public MTBLevelsetFunctionPDE(int w, int h, int d,
			MTBSegmentationInterface seg,	boolean default_fallback) {
		
		this.width = w;
		this.height = h;
		this.depth = d;
		
		this.DEFAULT_FALL_BACK = default_fallback;
		this.narrow = null;
//		this.signed = false;
		this.phi = new double[w][h][d];
		this.visible = new boolean[w][h][d];
		for (int x = 0; x < w; ++x) {
			for (int y = 0; y < h; ++y) {
				for (int z = 0; z < d; ++z) {
					this.visible[x][y][z] = true;
				}
			}
		}
		for (int x = 0; x < w; ++x) {
			for (int y = 0; y < h; ++y) {
				for (int z = 0; z < d; ++z) {
					if (x < seg.getSizeX() && y < seg.getSizeY() && z < seg.getSizeZ())
						this.phi[x][y][z] = seg.getClass(x, y, z) == 0 ? -1 : 1;
					else
						this.phi[x][y][z] = -1;
				}
			}
		}
	}
	
	/**
	 * Constructor to build a level set function from a segmentation object.
	 * <p>
	 * The level set function is initialized with value -1 for the background
	 * and +1 for the foreground. Note that the size of the given segmentation
	 * is allowed to differ from the target size of the function. Positions being
	 * undefined in the segmentation are assigned to the background class.
	 *  
	 * @param w				Target width of the level set function domain.
	 * @param h				Target height of the level set function domain.
	 * @param d				Target depth of the level set function domain.
	 * @param seg			Initial segmentation.
	 * @param default_fallback
	 * @param border	Width of border to be ignored.
	 */
	public MTBLevelsetFunctionPDE(int w, int h, int d,
			MTBSegmentationInterface seg,	boolean default_fallback, int border) {
		this(w, h, d, seg, default_fallback);
		this.borderDist = border;
	}

	/**
	 * Set visibility of positions.
	 * @param mask		Visibility mask.
	 * @return	True, if successful, otherwise false.
	 */
	public boolean setVisible(boolean[][][] mask) {
		if (mask.length != this.width) {
			return false;
		}
		if (mask[0].length != this.height) {
			return false;
		}
		if (mask[0][0].length != this.depth) {
			return false;
		}
		for (int x = 0; x < this.width; ++x) {
			for (int y = 0; y < this.height; ++y) {
				for (int z = 0; z < this.depth; ++z) {
					this.visible[x][y][z] = mask[x][y][z];
				}
			}
		}
		return true;
	}

	/**
	 * get the value of this function at the position of the predecessor of ( x, y, z )
	 *
	 * @param x position x in matrix
	 * @param y position y in matrix
	 * @param z position z in matrix
	 *
	 * @return value at position ( x, y, z )
	 */
	@Override
  public double get(int x, int y, int z) {
		//  without predecessor:     return this.phi[x][y][z];
		// get predecessor of (x,y,z)
		//System.out.println("punkt: " + x + " " + y + " " + z);
		return this.phi[x][y][z];
	}

	/**
	 * gets the value of the nearest contourpoint to point (x,y,z)
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public MTBPoint3D getPredecessorOnContour(int x, int y, int z)
	{
		MTBPoint3D pred = this.predecessors.get(new MTBPoint3D(x,y,z));
		return  pred;
	}

	/**
	 * assign a value to the matrix at position ( x, y, z )
	 *
	 * @param x position x in matrix
	 * @param y position y in matrix
	 * @param z position z in matrix
	 * @param value value to save an this position
	 */
	public void set(int x, int y, int z, double value) {
		this.phi[x][y][z] = value;
//		this.signed = false;
	}

	/**
	 * Get width of level set function.
	 * @return	Width of level set function.
	 */
	@Override
  public int getSizeX() {
		return this.width;
	}

	/**
	 * Get height of level set function.
	 * @return Height of level set function.
	 */
	@Override
  public int getSizeY() {
		return this.height;
	}

	/**
	 * Get depth of level set function.
	 * @return Depth of level set function.
	 */
	@Override
  public int getSizeZ() {
		return this.depth;
	}

	/**
	 * retrieve x-derivation at nearest contourpoint to ( x, y, z ), calculated by forward euler method
	 *
	 * @param x position x in matrix
	 * @param y position y in matrix
	 * @param z position z in matrix
	 *
	 * @return derivation at nearest contourpoint to ( x, y, z )
	 */
	@Override
  public double getDerivativeX(int x, int y, int z) {
		return forwardDifferencing_X(x, y, z, this.DEFAULT_FALL_BACK);
	}

	/**
	 * retrieve y-derivation at nearest contourpoint to  ( x, y, z ), calculated by forward euler method
	 *
	 * @param x position x in matrix
	 * @param y position y in matrix
	 * @param z position z in matrix
	 *
	 * @return derivation at nearest contourpoint to ( x, y, z )
	 */
	@Override
  public double getDerivativeY(int x, int y, int z) {
		return forwardDifferencing_Y(x, y, z, this.DEFAULT_FALL_BACK);
	}

	/**
	 * retrieve z-derivation at nearest contourpoint to ( x, y, z ), calculated by forward euler method
	 *
	 * @param x position x in matrix
	 * @param y position y in matrix
	 * @param z position z in matrix
	 *
	 * @return derivation at nearest contourpoint to ( x, y, z )
	 */
	@Override
  public double getDerivativeZ(int x, int y, int z) {
		return forwardDifferencing_Z(x, y, z, this.DEFAULT_FALL_BACK);
	}

	/**
	 * retrieve x-derivation at nearest contourpoint to ( x, y, z ), calculated by forward euler method,
	 * explicit define fallback-strategy
	 *
	 * @param x position x in matrix
	 * @param y position y in matrix
	 * @param z position z in matrix
	 * @param fallback fall back to other methods at border?
	 *
	 * @return derivation at nearest contourpoint to ( x, y, z )
	 */
	public double forwardDifferencing_X(int x, int y, int z, boolean fallback) {

		MTBPoint3D pred = this.predecessors.get(new MTBPoint3D(x,y,z));

		if (pred.getX() + 1 >= this.phi.length || !valid((int)pred.getX() + 1, (int)pred.getY(), (int)pred.getZ())) {
			if (fallback && pred.getX() > 0) {
				return backwardDifferencing_X((int)pred.getX(), (int)pred.getY(), (int)pred.getZ(), false);
			}
			return Double.NaN;
		}
		if (!valid((int)pred.getX(), (int)pred.getY(), (int)pred.getZ())) {
			return Double.NaN;
		}

		return this.phi[(int)pred.getX() + 1][(int)pred.getY()][(int)pred.getZ()] - this.phi[(int)pred.getX()][(int)pred.getY()][(int)pred.getZ()];
	}

	/**
	 * retrieve y-derivation at nearest contourpoint to ( x, y, z ), calculated by forward euler method,
	 * explicit define fallback-strategy
	 *
	 * @param x position x in matrix
	 * @param y position y in matrix
	 * @param z position z in matrix
	 * @param fallback fall back to other methods at border?
	 *
	 * @return derivation at nearest contourpoint to ( x, y, z )
	 */
	public double forwardDifferencing_Y(int x, int y, int z, boolean fallback) {

		MTBPoint3D pred = this.predecessors.get(new MTBPoint3D(x,y,z));

		if (pred.getY() + 1 >= this.phi[0].length || !valid((int)pred.getX(), (int)pred.getY() + 1, (int)pred.getZ())) {
			if (fallback && (int)pred.getY() > 0) {
				return backwardDifferencing_Y((int)pred.getX(), (int)pred.getY(), (int)pred.getZ(), false);
			}
			return Double.NaN;
		}
		if (!valid((int)pred.getX(), (int)pred.getY(), (int)pred.getZ())) {
			return Double.NaN;
		}

		return this.phi[(int)pred.getX()][(int)pred.getY() + 1][(int)pred.getZ()] - this.phi[(int)pred.getX()][(int)pred.getY()][(int)pred.getZ()];
	}

	/**
	 * retrieve z-derivation at nearest contourpoint to ( x, y, z ), calculated by forward euler method,
	 * explicit define fallback-strategy
	 *
	 * @param x position x in matrix
	 * @param y position y in matrix
	 * @param z position z in matrix
	 * @param fallback fall back to other methods at border?
	 *
	 * @return derivation at nearest contourpoint to ( x, y, z )
	 */
	public double forwardDifferencing_Z(int x, int y, int z, boolean fallback) {

		MTBPoint3D pred = this.predecessors.get(new MTBPoint3D(x,y,z));

		if (pred.getZ() + 1 >= this.phi[0][0].length || !valid((int)pred.getX(), (int)pred.getY(), (int)pred.getZ() + 1)) {
			if (fallback && (int)pred.getZ() > 0) {
				return backwardDifferencing_Z((int)pred.getX(), (int)pred.getY(), (int)pred.getZ(), false);
			}
			return Double.NaN;
		}
		if (!valid((int)pred.getX(), (int)pred.getY(), (int)pred.getZ())) {
			return Double.NaN;
		}

		return this.phi[(int)pred.getX()][(int)pred.getY()][(int)pred.getZ() + 1] - this.phi[(int)pred.getX()][(int)pred.getY()][(int)pred.getZ()];
	}

	/**
	 * retrieve x-derivation at nearest contourpoint to ( x, y, z ), calculated by backward euler method
	 *
	 * @param x position x in matrix
	 * @param y position y in matrix
	 * @param z position z in matrix
	 *
	 * @return derivation at nearest contourpoint to ( x, y, z )
	 */
	public double backwardDifferencing_X(int x, int y, int z) {
		return backwardDifferencing_X(x, y, z, this.DEFAULT_FALL_BACK);
	}

	/**
	 * retrieve x-derivation at nearest contourpoint to ( x, y, z ), calculated by backward euler method
	 *
	 * @param x position x in matrix
	 * @param y position y in matrix
	 * @param z position z in matrix
	 *
	 * @return derivation at nearest contourpoint to ( x, y, z )
	 */
	public double backwardDifferencing_Y(int x, int y, int z) {
		return backwardDifferencing_Y(x, y, z, this.DEFAULT_FALL_BACK);
	}

	/**
	 * retrieve x-derivation at nearest contourpoint to ( x, y, z ), calculated by backward euler method
	 *
	 * @param x position x in matrix
	 * @param y position y in matrix
	 * @param z position z in matrix
	 *
	 * @return derivation at nearest contourpoint to ( x, y, z )
	 */
	public double backwardDifferencing_Z(int x, int y, int z) {
		return backwardDifferencing_Z(x, y, z, this.DEFAULT_FALL_BACK);
	}

	/**
	 * retrieve x-derivation at nearest contourpoint to ( x, y, z ), calculated by backward euler
	 * method, explicit define fallback-strategy
	 *
	 * @param x position x in matrix
	 * @param y position y in matrix
	 * @param z position z in matrix
	 * @param fallback fall back to other methods at border?
	 *
	 * @return derivation at nearest contourpoint to ( x, y, z )
	 */
	public double backwardDifferencing_X(int x, int y, int z, boolean fallback) {

		MTBPoint3D pred = this.predecessors.get(new MTBPoint3D(x,y,z));

		if ((int)pred.getX() <= 0 || !valid((int)pred.getX() - 1, (int)pred.getY(), (int)pred.getZ())) {
			if (fallback && x < this.phi.length) {
				return forwardDifferencing_X((int)pred.getX(), (int)pred.getY(), (int)pred.getZ(), false);
			}
			return Double.NaN;
		}
		if (!valid((int)pred.getX(), (int)pred.getY(), (int)pred.getZ())) {
			return Double.NaN;
		}

		return this.phi[(int)pred.getX()][(int)pred.getY()][(int)pred.getZ()] - this.phi[(int)pred.getX() - 1][(int)pred.getY()][(int)pred.getZ()];
	}

	/**
	 * retrieve y-derivation at nearest contourpoint to ( x, y, z ), calculated by backward euler
	 * method, explicit define fallback-strategy
	 *
	 * @param x position x in matrix
	 * @param y position y in matrix
	 * @param z position z in matrix
	 * @param fallback fall back to other methods at border?
	 *
	 * @return derivation at nearest contourpoint to ( x, y, z )
	 */
	public double backwardDifferencing_Y(int x, int y, int z, boolean fallback) {

		MTBPoint3D pred = this.predecessors.get(new MTBPoint3D(x,y,z));

		if (pred.getY() <= 0 || !valid((int)pred.getX(), (int)pred.getY() - 1, (int)pred.getZ())) {
			if (fallback && (int)pred.getY() < this.phi[0].length) {
				return forwardDifferencing_Y((int)pred.getX(), (int)pred.getY(), (int)pred.getZ(), false);
			}
			return Double.NaN;
		}
		if (!valid((int)pred.getX(), (int)pred.getY(), (int)pred.getZ())) {
			return Double.NaN;
		}

		return this.phi[(int)pred.getX()][(int)pred.getY()][(int)pred.getZ()] - this.phi[(int)pred.getX()][(int)pred.getY() - 1][(int)pred.getZ()];
	}

	/**
	 * retrieve z-derivation at nearest contourpoint to ( x, y, z ), calculated by backward euler
	 * method, explicit define fallback-strategy
	 *
	 * @param x position x in matrix
	 * @param y position y in matrix
	 * @param z position z in matrix
	 * @param fallback fall back to other methods at border?
	 *
	 * @return derivation at nearest contourpoint to ( x, y, z )
	 */
	public double backwardDifferencing_Z(int x, int y, int z, boolean fallback) {

		MTBPoint3D pred = this.predecessors.get(new MTBPoint3D(x,y,z));

		if (pred.getZ() <= 0 || !valid((int)pred.getX(), (int)pred.getY(), (int)pred.getZ() - 1)) {
			if (fallback && pred.getZ() < this.phi[0][0].length) {
				return forwardDifferencing_Z((int)pred.getX(), (int)pred.getY(), (int)pred.getZ(), false);
			}
			return Double.NaN;
		}
		if (!valid((int)pred.getX(), (int)pred.getY(), (int)pred.getZ())) {
			return Double.NaN;
		}

		return this.phi[(int)pred.getX()][(int)pred.getY()][(int)pred.getZ()] - this.phi[(int)pred.getX()][(int)pred.getY()][(int)pred.getZ() - 1];
	}

	/**
	 * computes the the derivation in x direction 2 times at nearest contourpoint to (x,y,z)
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	@Override
  public double getDerivativeXX(int x, int y, int z)
	{
		MTBPoint3D pred = this.predecessors.get(new MTBPoint3D(x,y,z));
		double phixx =  this.phi[(int)pred.getX() - 1][(int)pred.getY()][(int)pred.getZ()] - 2 * this.phi[(int)pred.getX()][(int)pred.getY()][(int)pred.getZ()] + this.phi[(int)pred.getX() + 1][(int)pred.getY()][(int)pred.getZ()];
		return  phixx;
	}

	/**
	 * computes the the derivation in y direction 2 times at nearest contourpoint to (x,y,z)
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	@Override
  public double getDerivativeYY(int x, int y, int z)
	{
		MTBPoint3D pred = this.predecessors.get(new MTBPoint3D(x,y,z));
		double phiyy =  this.phi[(int)pred.getX()][(int)pred.getY() - 1][(int)pred.getZ()] - 2 * this.phi[(int)pred.getX()][(int)pred.getY()][(int)pred.getZ()] + this.phi[(int)pred.getX()][(int)pred.getY() - 1][(int)pred.getZ()];
		return  phiyy;
	}

	/**
	 * computes the the derivation in z direction 2 times at nearest contourpoint to (x,y,z)
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	@Override
  public double getDerivativeZZ(int x, int y, int z)
	{
		MTBPoint3D pred = this.predecessors.get(new MTBPoint3D(x,y,z));
		double phizz =  this.phi[(int)pred.getX()][(int)pred.getY()][(int)pred.getZ() - 1] - 2 * this.phi[(int)pred.getX()][(int)pred.getY()][(int)pred.getZ()] + this.phi[(int)pred.getX()][(int)pred.getY()][(int)pred.getZ() + 1];
		return  phizz;
	}

	/**
	 * computes the the derivation in x and y direction at nearest contourpoint to (x,y,z)
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	@Override
  public double getDerivativeXY(int x, int y, int z)
	{
		MTBPoint3D pred = this.predecessors.get(new MTBPoint3D(x,y,z));
		double phixy = (this.phi[(int)pred.getX() - 1][(int)pred.getY() - 1][(int)pred.getZ()] + this.phi[(int)pred.getX() + 1][(int)pred.getY() + 1][(int)pred.getZ()] - this.phi[(int)pred.getX() + 1][(int)pred.getY() - 1][(int)pred.getZ()] - this.phi[(int)pred.getX() - 1][(int)pred.getY() + 1][(int)pred.getZ()])/4.0;
		return  phixy;
	}

	/**
	 * computes the the derivation in x and z direction at nearest contourpoint to (x,y,z)
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	@Override
  public double getDerivativeXZ(int x, int y, int z)
	{
		MTBPoint3D pred = this.predecessors.get(new MTBPoint3D(x,y,z));
		double phixz = (this.phi[(int)pred.getX() - 1][(int)pred.getY()][(int)pred.getZ() - 1] + this.phi[(int)pred.getX() + 1][(int)pred.getY()][(int)pred.getZ() + 1] - this.phi[(int)pred.getX() + 1][(int)pred.getY()][(int)pred.getZ() - 1] - this.phi[(int)pred.getX() - 1][(int)pred.getY()][(int)pred.getZ() + 1])/4.0;
		return  phixz;
	}

	/**
	 * computes the the derivation in y and z direction at nearest contourpoint to (x,y,z)
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	@Override
  public double getDerivativeYZ(int x, int y, int z)
	{
		MTBPoint3D pred = this.predecessors.get(new MTBPoint3D(x,y,z));
		double phiyz = (this.phi[(int)pred.getX()][(int)pred.getY() - 1][(int)pred.getZ() - 1] + this.phi[(int)pred.getX()][(int)pred.getY() + 1][(int)pred.getZ() + 1] - this.phi[(int)pred.getX()][(int)pred.getY() + 1][(int)pred.getZ() - 1] - this.phi[(int)pred.getX()][(int)pred.getY() - 1][(int)pred.getZ() + 1])/4.0;
		return  phiyz;
	}

	/**
	 * compute the curvature at at nearest contourpoint to (x,y,z)
	 * if x or y are not in the boundaries it returns 0
	 * if z is not in the boundaries it returns the 2d cuvature
	 * @param x x coordinate
	 * @param y y coordinate
	 * @param z z coordinate
	 *
	 * @return curvature at (x, y, z)
	 */
	@Override
  public double getCurvature(int x, int y, int z) {

		double result = 0;

		boolean xok = valid(x - 1, y, z) && valid(x + 1, y, z) && valid(x, y, z);
		boolean yok = valid(x, y - 1, z) && valid(x, y + 1, z) && valid(x, y, z);
		boolean zok = valid(x, y, z - 1) && valid(x, y, z + 1) && valid(x, y, z);

		if (!xok || !yok) {
			return 0;
		}

		// (phixx * phiy2 + phiyy * phix2 - 2 phixy * phiy * phix) / (phix2 + phiy2)^(3/2)
		double phix = forwardDifferencing_X(x, y, z, true);
		double phix2 = phix * phix;
		double phiy = forwardDifferencing_Y(x, y, z, true);
		double phiy2 = phiy * phiy;
		double phiz = 0;
		double  phiz2 = 0;

		// border treatment is missing
		double phixx = getDerivativeXX(x, y, z); //phi.get(x - 1, y, z) - 2 * phi.get(x, y, z) + phi.get(x + 1, y, z);
		double phiyy = getDerivativeYY(x, y, z);//phi.get(x, y - 1, z) - 2 * phi.get(x, y, z) + phi.get(x, y + 1, z);
		double phizz = 0;

		double phixy = getDerivativeXY(x, y, z);// (phi.get(x - 1, y - 1, z) + phi.get(x + 1, y + 1, z) - phi.get(x + 1, y - 1, z) - phi.get(x - 1, y + 1, z))/4.0;
		double  phixz = 0;
		double phiyz = 0;

		if(zok)
		{
			phiz = forwardDifferencing_Z(x,y,z,true);
			phiz2 = phiz * phiz;

			phizz = getDerivativeZZ(x, y, z);

			phixz = getDerivativeXZ(x, y, z);
			phiyz = getDerivativeYZ(x, y, z);

			result = (phix2*phiyy - 2*phix*phiy*phixy + phiy2*phixx + phix2*phizz - 2*phix*phiz*phixz + phiz2*phixx + phiy2*phizz - 2*phiy*phiz*phiyz + phiz2*phiyy)
					/
					(Math.sqrt((phix2 + phiy2 + phiz2) * (phix2 + phiy2 + phiz2) * (phix2 + phiy2 + phiz2)));
		}
		else
		{
			result = (phixx * phiy2 + phiyy * phix2 - 2 * phixy * phiy * phix) / Math.sqrt((phix2 + phiy2) * (phix2 + phiy2) * (phix2 + phiy2));
		}


		return result;
	}

	/**
	 * is this pixel valid?
	 *
	 * @param x x coordinate
	 * @param y y coordinate
	 * @param z z coordinate
	 *
	 * @return true if visible and not MAX_VALUE, else false
	 */
	@Override
  public boolean valid(int x, int y, int z) {
		if (x < 0 || y < 0 || z < 0 || x >= this.phi.length || y >= this.phi[x].length || z >= this.phi[x][y].length) {
			return false;
		}
		if (!this.visible[x][y][z]) {
			return false;
		}
		if (Math.abs(this.phi[x][y][z]) >= MAX_VALUE) {
			return false;
		}
		return true;
	}

	/**
	 * is this pixel visible?
	 *
	 * @param x x coordinate
	 * @param y y coordinate
	 * @param z z coordinate
	 *
	 * @return true if visible, else false
	 */
	@Override
	public boolean isVisible(int x, int y, int z) {
		return this.visible[x][y][z];
	}

	/**
	 * exists valid signed distance
	 *
	 * @return true if signed distance is valid, otherwise false
	 */
//	public boolean signDistance() {
//		return this.signed;
//	}

	/**
	 * Transform function into signed distance function.
	 * <p>
	 * The transformation can be restricted to a narrow-band around the zero 
	 * level by setting the width parameter to a value larger or equal to one.
	 *
	 * @param b 	Width of narrow-band around zero level.
	 */
	public void signDistance(double b) {
		
		// safety-check for band-width value
		double bandwidth = b;
		double maximum = MAX_VALUE;
		if (bandwidth < 1) {
			bandwidth = maximum;
		}
		
		// initialize new narrow band
		this.narrow = new ArrayList<MTBPoint3D>();

		// lists of inner and outer pixels
		MTBLevelsetFunctionPDE.SortedList<MTBPoint3D> out = 
			new MTBLevelsetFunctionPDE.SortedList<MTBPoint3D>();
		MTBLevelsetFunctionPDE.SortedList<MTBPoint3D> in = 
			new MTBLevelsetFunctionPDE.SortedList<MTBPoint3D>();
		// map of precursors of each pixel in the narrow band
		this.predecessors = new HashMap<MTBPoint3D, MTBPoint3D>();

		// reset phi to binary values, extract zero level
		for (int x = 0; x < this.phi.length; x++) {
			for (int y = 0; y < this.phi[x].length; y++) {
				for (int z = 0; z < this.phi[x][y].length; z++) {
					if (this.nearZero(x, y, z)) {
						if (this.phi[x][y][z] > 0) {
							this.phi[x][y][z] = 0.5 + sgnSum(x, y, z);
							in.offer(new MTBPoint3D(x, y, z), Math.abs(this.phi[x][y][z]));
							this.narrow.add(new MTBPoint3D(x, y, z));
							// the precursor of a zero-level point is the point itself
							this.predecessors.put(
								new MTBPoint3D(x, y, z), new MTBPoint3D(x, y, z));
						} else {
							this.phi[x][y][z] = -0.5 + sgnSum(x, y, z);
							out.offer(new MTBPoint3D(x, y, z), Math.abs(this.phi[x][y][z]));
							this.narrow.add(new MTBPoint3D(x, y, z));
							// the precursor of a zero-level point is the point itself
							this.predecessors.put(
								new MTBPoint3D(x, y, z), new MTBPoint3D(x, y, z));
						}
					} else if (this.phi[x][y][z] > 0) {
						this.phi[x][y][z] = maximum;
					} else {
						this.phi[x][y][z] = -maximum;
					}
				}
			}
		}
		// perform sign expansion in inner and outer direction
		this.signedExpandation(in, bandwidth, 1);
		this.signedExpandation(out, bandwidth, -1);
	}

//	/**
//	 * Checks if a one point is close to the zero level.
//	 *
//	 * @param x x coord of point.
//	 * @param y y coord of point.
//	 * @param z z coord of point.
//	 * @return True, if point is close to zero-level, false otherwise.
//	 */
//	public boolean nearZero(int x, int y, int z) {
//		if (   x > 0 
//				&& sgn(this.phi[x][y][z]) != sgn(this.phi[x - 1][y][z])) {
//			return true;
//		}
//		if (   x < this.phi.length - 1 
//				&& sgn(this.phi[x][y][z]) != sgn(this.phi[x + 1][y][z])) {
//			return true;
//		}
//		if (   y > 0 
//				&& sgn(this.phi[x][y][z]) != sgn(this.phi[x][y - 1][z])) {
//			return true;
//		}
//		if (   y < this.phi[0].length - 1 
//				&& sgn(this.phi[x][y][z]) != sgn(this.phi[x][y + 1][z])) {
//			return true;
//		}
//		if (   z > 0 
//				&& sgn(this.phi[x][y][z]) != sgn(this.phi[x][y][z - 1])) {
//			return true;
//		}
//		if (   z < this.phi[0][0].length - 1 
//				&& sgn(this.phi[x][y][z]) != sgn(this.phi[x][y][z + 1])) {
//			return true;
//		}
//		return false;
//	}

	/**
	 * Expand signed distances around zero-level.
	 * <p>
	 * Helper function for method {@link signDistance}.
	 *
	 * @param slist 		Dynamic sorted list of points to expand.
	 * @param maximum 	Maximal distance value, just for narrow-band.
	 * @param toAdd 		Value to add to next level value, usually 1.
	 */
	private void signedExpandation(
			MTBLevelsetFunctionPDE.SortedList<MTBPoint3D> slist, double maximum, 
			double toAdd) {
		
		MTBPoint3D p;
		while (slist.getSize() > 0) {
			
			// process next point
			p = slist.poll();
			double val = 
				this.phi[(int)p.getX()][(int)p.getY()][(int)p.getZ()] + toAdd;
			double v = Math.abs(val);
			
			// if value lies out of band-width, skip pixel
			if (v > maximum) {
				continue;
			}

			/* propagate distances to neighbors */
			
			// point on the left
			if (     p.getX() > 0 
					&&	(Math.abs(this.phi[(int)p.getX() - 1]
							                   [(int)p.getY()]
							                   [(int)p.getZ()]) > v)) {
				this.phi[(int)p.getX() - 1][(int)p.getY()][(int)p.getZ()] = val;
				slist.offer(new MTBPoint3D(p.getX() - 1, p.getY(), p.getZ()), v);
				if (   p.getX()-1 >= this.borderDist 
						&& p.getY()   >= this.borderDist 
						&& p.getX()-1 < this.phi.length - this.borderDist
						&& p.getY()   < this.phi[0].length - this.borderDist) {
					this.narrow.add(new MTBPoint3D(p.getX() - 1, p.getY(), p.getZ()));
				}
				// propagate precursor
				this.predecessors.put(new MTBPoint3D(p.getX() - 1, p.getY(), p.getZ()),
					this.predecessors.get(new MTBPoint3D(p.getX(), p.getY(), p.getZ())));
			}

			// point at the top
			if (    p.getY() > 0 
					&& (Math.abs(this.phi[(int)p.getX()]
							                  [(int)p.getY() - 1]
							                  [(int)p.getZ()]) > v)) {
				this.phi[(int)p.getX()][(int)p.getY() - 1][(int)p.getZ()] = val;
				slist.offer(new MTBPoint3D(p.getX(), p.getY() - 1, p.getZ()), v);
				if (   p.getX()   >= this.borderDist 
						&& p.getY()-1 >= this.borderDist 
						&& p.getX()   < this.phi.length - this.borderDist
						&& p.getY()-1 < this.phi[0].length - this.borderDist) {
					this.narrow.add(new MTBPoint3D(p.getX(), p.getY() - 1, p.getZ()));
				}
				// propagate precursor
				this.predecessors.put(new MTBPoint3D(p.getX(), p.getY() - 1, p.getZ()),
					this.predecessors.get(new MTBPoint3D(p.getX(), p.getY(), p.getZ())));
			}

			// point on the right
			if (    p.getX() < this.phi.length - 1 
					&& (Math.abs(this.phi[(int)p.getX() + 1]
							                  [(int)p.getY()]
							                  [(int)p.getZ()]) > v)) {
				this.phi[(int)p.getX() + 1][(int)p.getY()][(int)p.getZ()] = val;
				slist.offer(new MTBPoint3D(p.getX() + 1, p.getY(), p.getZ()), v);
				if (   p.getX()+1 >= this.borderDist 
						&& p.getY()   >= this.borderDist 
						&& p.getX()+1 < this.phi.length - this.borderDist
						&& p.getY()   < this.phi[0].length - this.borderDist) {
					this.narrow.add(new MTBPoint3D(p.getX() + 1, p.getY(), p.getZ()));
				}
				// propagate precursor
				this.predecessors.put(new MTBPoint3D(p.getX() + 1, p.getY(), p.getZ()),
					this.predecessors.get(new MTBPoint3D(p.getX(), p.getY(), p.getZ())));
			}

			// point at the bottom
			if (    p.getY() < this.phi[0].length - 1 
					&& (Math.abs(this.phi[(int)p.getX()]
							                  [(int)p.getY() + 1]
							                  [(int)p.getZ()]) > v)) {
				this.phi[(int)p.getX()][(int)p.getY() + 1][(int)p.getZ()] = val;
				slist.offer(new MTBPoint3D(p.getX(), p.getY() + 1, p.getZ()), v);
				if (   p.getX()   >= this.borderDist 
						&& p.getY()+1 >= this.borderDist 
						&& p.getX()   < this.phi.length - this.borderDist
						&& p.getY()+1 < this.phi[0].length - this.borderDist) {
					this.narrow.add(new MTBPoint3D(p.getX(), p.getY() + 1, p.getZ()));
				}
				// propagate precursor
				this.predecessors.put(new MTBPoint3D(p.getX(), p.getY() + 1, p.getZ()),
					this.predecessors.get(new MTBPoint3D(p.getX(), p.getY(), p.getZ())));
			}

			/* additional points in 3D - untested so far!!! */
			
			//check fronter point
			if (p.getZ() > 0 && (Math.abs(this.phi[(int)p.getX()][(int)p.getY()][(int)p.getZ() - 1]) > v)) {
				this.phi[(int)p.getX()][(int)p.getY()][(int)p.getZ() - 1] = val;
				slist.offer(new MTBPoint3D(p.getX(), p.getY(), p.getZ() - 1), v);
				if (p.getZ() > this.borderDist)
				{
					this.narrow.add(new MTBPoint3D(p.getX(), p.getY(), p.getZ() - 1));
				}
				this.predecessors.put(new MTBPoint3D(p.getX(), p.getY(), p.getZ() - 1),
						this.predecessors.get(new MTBPoint3D(p.getX(), p.getY(), p.getZ())));
			}

			//check back
			if (p.getZ() < this.phi[0][0].length - 1 && (Math.abs(this.phi[(int)p.getX()][(int)p.getY()][(int)p.getZ() + 1]) > v)) {
				this.phi[(int)p.getX()][(int)p.getY()][(int)p.getZ() + 1] = val;
				slist.offer(new MTBPoint3D(p.getX(), p.getY(), p.getZ() + 1), v);
				if (p.getZ() > this.phi[0][0].length - this.borderDist + 1)
				{
					this.narrow.add(new MTBPoint3D(p.getX(), p.getY(), p.getZ() + 1));
				}
				this.predecessors.put(new MTBPoint3D(p.getX(), p.getY(), p.getZ() + 1),
						this.predecessors.get(new MTBPoint3D(p.getX(), p.getY(), p.getZ())));
			}
		}
	}

	/**
	 * Computes scaled sign in neighborhood.
	 *
	 * @param x x coord of point.
	 * @param y y coord of point.
	 * @param z z coord of point.
	 *
	 * @return Scaled sign in neighborhood.
	 */
	private double sgnSum(int x, int y, int z) {
		double sum = -2 * sgn(this.phi[x][y][z]);
		int cnt = 1;
		for (int i = -1; i < 2; i++) {
			for (int j = -1; j < 2; j++) {
				for (int k = -1; k < 2; k++) {
					if (   x + i >= 0 
							&& y + j >= 0 
							&& z + k >= 0 
							&& x + i < this.phi.length 
							&& y + j < this.phi[x].length 
							&& z + k < this.phi[x][y].length 
							&& this.visible[x + i][y + j][z + k]) {
						sum += sgn(this.phi[x + i][y + j][z + k]);
						cnt++;
					}
				}
			}
		}
		// scale result to [-0.5,0.5]
		// ATTENTION: never change the sign while generating narrow band!
		return sum / (2.01 * cnt);
	}

	/**
	 * generate a clone of this object
	 *
	 * @return the copy
	 */
	@Override
	public MTBLevelsetFunctionPDE clone() {
		try {
			MTBLevelsetFunctionPDE gecloned = (MTBLevelsetFunctionPDE) super.clone();

			gecloned.phi = new double[this.phi.length][this.phi[0].length][this.phi[0][0].length];
			for (int x = 0; x < this.phi.length; x++) {
				for (int y = 0; y < this.phi[x].length; y++) {
					for (int z = 0; z < this.phi[x][y].length; z++) {
						gecloned.phi[x][y][z] = this.phi[x][y][z];
					}
				}
			}
//			gecloned.signed = this.signed;
			//gecloned.narrow = (ArrayList<MTBPoint3D>) ((List) narrow).clone ();
			gecloned.narrow = new ArrayList<MTBPoint3D>(this.narrow);
			gecloned.predecessors = (HashMap<MTBPoint3D, MTBPoint3D>) this.predecessors.clone();
			return gecloned;
		} catch (CloneNotSupportedException cnse) {
			return null;
		}
	}

	/**
	 * copy this object to another one
	 */
	public void copyTo(MTBLevelsetFunctionPDE two) {
		two.phi = new double[this.phi.length][this.phi[0].length][this.phi[0][0].length];
		two.visible = new boolean[this.visible.length][this.visible[0].length][this.visible[0][0].length];
		for (int x = 0; x < this.phi.length; x++) {
			for (int y = 0; y < this.phi[x].length; y++) {
				for (int z = 0; z < this.phi[x][y].length; z++) {
					two.phi[x][y][z] = this.phi[x][y][z];
					two.visible[x][y][z] = this.visible[x][y][z];
				}
			}
		}
//		two.signed = this.signed;
		two.DEFAULT_FALL_BACK = this.DEFAULT_FALL_BACK;
		two.narrow = new ArrayList<MTBPoint3D>();
		if (this.narrow != null) {
			for (int i = 0; i < this.narrow.size(); i++) {
				two.narrow.add(new MTBPoint3D(this.narrow.get(i)));
			}
		}
		two.predecessors = (HashMap<MTBPoint3D, MTBPoint3D>) this.predecessors.clone();
	}

	/**
	 * are these two LevelSets equal?
	 *
	 * @param ls2d LevelSet to check
	 *
	 * @return true if this functions equals the other, false otherwise
	 */
	public boolean equals(MTBLevelsetFunctionPDE ls2d) {
		if (ls2d.phi.length != this.phi.length) {
			return false;
		}
		if (ls2d.phi[0].length != this.phi[0].length) {
			return false;
		}
		if (ls2d.phi[0][0].length != this.phi[0][0].length) {
			return false;
		}

		for (int x = 0; x < this.phi.length; x++) {
			for (int y = 0; y < this.phi[x].length; y++) {
				for (int z = 0; z < this.phi[x][y].length; z++) {
					if (ls2d.phi[x][y][z] != this.phi[x][y][z]) {
						return false;
					}
				}
			}
		}

		if (!this.narrow.equals(ls2d.narrow)) {
			return false;
		}

		return true;
	}

	/**
	 * get an iterator through narrow band
	 *
	 * @return iterator through narrow band
	 */
	public Iterator<MTBPoint3D> getNarrowIterator() {
		return this.narrow.iterator();
	}

	/**
	 * Get a binary mask of the represented segmentation.
	 * @return Binary segmentation mask.
	 */
	public MTBImageByte getBinaryMask() {
		MTBImageByte image = (MTBImageByte) MTBImage.createMTBImage(
			this.width, this.height, this.depth, 1, 1, MTBImageType.MTB_BYTE);
		for (int x = 0; x < this.width; ++x) {
			for (int y = 0; y < this.height; ++y) {
				for (int z = 0; z < this.depth; ++z) {
					if (this.phi[x][y][z] > 0) {
						image.putValueInt(x, y, z, 0, 0, 255);
					} else {
						image.putValueInt(x, y, z, 0, 0, 0);
					}
				}
			}
		}
		image.setTitle("PDE level set function visualization - binary mask");
		return image;
	}

	/**
	 * Get visualization of current level set function in red-blue color map.
	 * <p>
	 * This function only considers the first dimension in z.
	 * @return Image showing level set function.
	 */
	public MTBImageRGB getPhiColorImage2D(MTBImageRGB targetImg) {

		// find minimal and maximal values	
		double maxVal = Double.MIN_VALUE;
		double minVal = Double.MAX_VALUE;

		for (int x = 0; x < this.width; ++x) {
			for (int y = 0; y < this.height; ++y) {
				if (this.phi[x][y][0] < minVal && this.phi[x][y][0] != -MAX_VALUE) {
					minVal = this.phi[x][y][0];
				}
				if (this.phi[x][y][0] > maxVal && this.phi[x][y][0] != MAX_VALUE) {
					maxVal = this.phi[x][y][0];
				}
			}
		}

		// allocate memory for array, but only once
		if (this.helperArray == null)
			this.helperArray = new double[this.height][this.width];
		
		// fill 2D array
		for (int y = 0; y < this.height; ++y) {
			for (int x = 0; x < this.width; ++x) {
				if (this.phi[x][y][0] > 0)
					this.helperArray[y][x] = 
						(this.phi[x][y][0] > maxVal) ? maxVal : this.phi[x][y][0];
				else
					this.helperArray[y][x] = 
						(this.phi[x][y][0] < minVal) ? minVal : this.phi[x][y][0];
			}
		}
		
		ArrayToColorMapImage arrVis;
    try {
  		// disable history logging temporarily
  		int oldHistoryConstructionMode = ALDOperator.getConstructionMode();
  		ALDOperator.setConstructionMode(1);

	    arrVis = new ArrayToColorMapImage(this.helperArray);
	    arrVis.setTargetImage(targetImg);
			arrVis.runOp(HidingMode.HIDDEN);

			// ensable history logging again
			ALDOperator.setConstructionMode(oldHistoryConstructionMode);
			return arrVis.getResultImage();
    } catch (ALDOperatorException e) {
    	return null;
    } catch (ALDProcessingDAGException e) {
    	return null;
    }
	}

	/**
	 * Get visualization of current level set function.
	 * @return Image showing level set function.
	 */
	public MTBImageShort getPhiImage() {
		MTBImageShort phiImage = (MTBImageShort)MTBImage.createMTBImage(
			this.width, this.height, this.depth, 1, 1, MTBImageType.MTB_SHORT);

		double maxVal = Double.MIN_VALUE;
//		double minVal = Double.MAX_VALUE;
//
		for (int x = 0; x < this.width; ++x) {
			for (int y = 0; y < this.height; ++y) {
				for (int z = 0; z < this.depth; ++z) {
//					if (phi[x][y][z] < minVal && phi[x][y][z] != -MAX_VALUE) {
//						minVal = phi[x][y][z];
//					}
					if (   Math.abs(this.phi[x][y][z]) > maxVal 
							&& this.phi[x][y][z] != MAX_VALUE) {
						maxVal = Math.abs(this.phi[x][y][z]);
					}
					//                    System.out.println("maxVal: " + maxVal);
				}
			}
		}

		//        if(minVal < 0)
		//        {
		//            maxVal += Math.abs(minVal);
		//           // minVal = 0;
		//        }
		//        System.out.println("minVal: " + minVal + " ,maxVal: " + maxVal);

		for (int x = 0; x < this.width; ++x) {
			for (int y = 0; y < this.height; ++y) {
				for (int z = 0; z < this.depth; ++z) {
//					if (Math.abs(this.phi[x][y][z]) == MAX_VALUE) {
//						phiImage.putValueDouble(x, y, z, 0, 0, 255);
//					} else {
//						if (this.phi[x][y][z] < 0) {
//							phiImage.putValueDouble(x, y, z, 0, 0, ((this.phi[x][y][z]) / minVal) * 255);
//						}
//						if (this.phi[x][y][z] >= 0) {
//							phiImage.putValueDouble(x, y, z, 0, 0, ((this.phi[x][y][z]) / maxVal) * 255);
//						}
					phiImage.putValueDouble(x, y, z, 0, 0, 
						Math.abs(this.phi[x][y][z])/maxVal*phiImage.getTypeMax());
				}
			}
		}
		return phiImage;
	}

	public double getForegroundSum() {
		double ForegroundSum = 0;

		for (int x = 0; x < this.getSizeX(); x++) {
			for (int y = 0; y < this.getSizeY(); y++) {
				for (int z = 0; z < this.getSizeZ(); z++) {
					if (this.get(x, y, z) <= 0) {
						ForegroundSum++;
					}
				}
			}
		}
		return ForegroundSum;
	}

	@Override
	public MTBSegmentationInterface.SegmentationDimension getDimension() {
		return MTBSegmentationInterface.SegmentationDimension.dim_3;
	}

	@Override
	public int getNumberOfClasses() {
		return 2;
	}

	@Override
	public int getMaxLabel() {
		return 1;
	}

	@Override
	public boolean isVisible(int x, int y) {
		return this.isVisible(x, y, 0);
	}

	@Override
	public void setVisible(int x, int y) {
		this.setVisible(x, y, 0);
	}

	@Override
	public void setVisible(int x, int y, int z) {
		this.visible[x][y][z] = true;
	}

	@Override
	public void setInvisible(int x, int y) {
		this.setInvisible(x, y, 0);
	}

	@Override
	public void setInvisible(int x, int y, int z) {
		this.visible[x][y][z] = false;
	}

	@Override
	public int getClass(int x, int y) {
		return this.getClass(x, y, 0);
	}

	@Override
	public int getClass(int x, int y, int z) {
		if (this.get(x, y, z) > 0) {
			return 1;
		}
		return 0;
	}

	@Override
	@Deprecated
	public void setClass(int x, int y, int c) {
		System.err.println("MTBLevelSegmentation::setClass() - without function!");
	}

	@Override
	@Deprecated
	public void setClass(int x, int y, int z, int c) {
		System.err.println("MTBLevelSegmentation::setClass() - without function!");
	}

	/**
	 * A genric class representing a sorted list
	 *
	 * @author martin scharm
	 * @version 1.0
	 * @param <EType> type of the element
	 */
	protected class SortedList<EType> {

		private SortedListElement<EType> head, tail;
		private int size;

		/**
		 * create empty SortetList
		 */
		public SortedList() {
			this.head = null;
			this.tail = null;
			this.size = 0;
		}

		/**
		 * count number of elements
		 *
		 * @return number of elements
		 */
		public int getSize() {
			return this.size;
		}

		/**
		 * add an element to List
		 *
		 * @param element element to add
		 * @param priority priority of this element
		 *
		 * @return was adding successfull?
		 */
		public boolean offer(EType element, double priority) {
			if (this.size == 0) {
				this.head = new MTBLevelsetFunctionPDE.SortedListElement<EType>(element, priority);
				this.tail = this.head;
				this.size++;
				return true;
			}

			MTBLevelsetFunctionPDE.SortedListElement<EType> neu = new MTBLevelsetFunctionPDE.SortedListElement<EType>(element, priority);
			double p = priority;
			if (p < this.head.priority) {
				neu.next = this.head;
				this.head.prev = neu;
				this.head = neu;
				this.size++;
				return true;
			}

			if (p >= this.tail.priority) {
				neu.prev = this.tail;
				this.tail.next = neu;
				this.tail = neu;
				this.size++;
				return true;
			}

			MTBLevelsetFunctionPDE.SortedListElement<EType> akt = this.tail.prev;
			while (p < akt.priority) {
				akt = akt.prev;
			}

			akt.next.prev = neu;
			neu.next = akt.next;
			neu.prev = akt;
			akt.next = neu;
			this.size++;
			return true;
		}

		/**
		 * retrieve element with highest priority
		 *
		 * @return element with highest priority in list
		 */
		public EType poll() {
			if (this.size == 0) {
				return null;
			}

			EType r = this.head.value;
			this.head = this.head.next;
			if (this.head != null) {
				this.head.prev = null;
			} else {
				this.tail = null;
			}

			this.size--;

			return r;
		}
	}

	/**
	 * internal representation of elements
	 */
	protected class SortedListElement<EType> {

		public EType value;
		public double priority;
		public SortedListElement<EType> next, prev;

		public SortedListElement(EType _value, double _priority) {
			this.value = _value;
			this.priority = _priority;
			this.next = null;
			this.prev = null;
		}
	}

	@Override
	public double getWeight(int x, int y) {
		return 1;
	}

	@Override
	public double getWeight(int x, int y, int z) {
		return 1;
	}

	@Override
	public void setWeight(int x, int y, double c) {
		// TODO Auto-generated method stub
	}

	@Override
	public void setWeight(int x, int y, int z, double c) {
		// TODO Auto-generated method stub
	}

	@Override
  public MTBImage getMask(int class_)
	{
		MTBImage mask = MTBImage.createMTBImage(getSizeX(),getSizeY(),getSizeZ(),1,1,MTBImageType.MTB_BYTE);

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
