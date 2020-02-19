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

package de.unihalle.informatik.MiToBo.segmentation.levelset.nonPDE;


import java.util.Vector;
import java.util.Hashtable;

import de.unihalle.informatik.Alida.operator.ALDData;
import de.unihalle.informatik.MiToBo.core.datatypes.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;

/**
 * A class to hold a level set function where only the membership to a phase is required and represented.
 *
 * Background pixels gets phase BG_PHASE, object phases start with BG_PHASE+1.
 * The implementation asserts that all phases > BG_PHASE are indeed object phases.
 * Value INVALID_PHASE is reserved for invalid pixels.
 *
 */

public class MTBLevelsetMembership extends ALDData
{
	/** Level set function value to represent the background phase
	 */
	static public final short BG_PHASE = 1; // phase for backgroud

	/** Level set function value to represent invalid pixels
	 */
	static public final short INVALID_PHASE = 0; // invalid pixels

	/** Maximal number of phases which may be represented including background phase.
	 */
	static public final int MAX_ALLOWED_NUM_PHASES = Short.MAX_VALUE;
	private short[][][] phi;
	private int sumSizeObject; // sum over all object phases
	private int[] sizePhases; // number of pixels per phase starting from 1 for bg
							  // i.e. length of array is numPhases+1
	private final int sizeX;
	private final int sizeY;
	private final int sizeZ;
	private int numPhases;  // number of phases including background


	/**
	 * Constructs a two phase level set function which is initialized with
	 * a circle or sphere of radius 0.5*(sizeX+sizeY+sizeZ)/3.
	 * 
	 * @param sizeX
	 * @param sizeY
	 * @param sizeZ
	 */
	public MTBLevelsetMembership(int sizeX, int sizeY, int sizeZ)
	{
			this.sizeX = sizeX;
			this.sizeY = sizeY;
			this.sizeZ = sizeZ;
			this.phi = new short[sizeZ][sizeY][sizeX];
			this.numPhases = 2;	
			this.sizePhases = new int[numPhases+1];
			
			this.initLevelset( null);
	}
	
	/**
	 * Constructs a two phase level set function which is initialiazed with
	 * a circle or sphere of radius 0.5*(sizeX+sizeY+sizeZ)/3.
	 * <p>
	 * If invalidImg is non null, than pixels <> zero in invalidImg are invalid
	 * 
	 * @param sizeX
	 * @param sizeY
	 * @param sizeZ
	 * @param invalidImg
	 */
	public MTBLevelsetMembership(int sizeX, int sizeY, int sizeZ, MTBImage invalidImg)
	{
			this.sizeX = sizeX;
			this.sizeY = sizeY;
			this.sizeZ = sizeZ;
			this.phi = new short[sizeZ][sizeY][sizeX];
			this.numPhases = 2;	
			this.sizePhases = new int[numPhases+1];
			
			this.initLevelset( invalidImg);
	}
	
	/**
	 * Creates a two phase level set function from an image via thresholding.
	 * All pixels with intensities greater equal the threshold constitute the object phase,
	 * other pixels the background.
	 * <p>
	 * If invalidImg is non null, than pixels <> zero in invalidImg are invalid
	 * 
	 * @param sizeX
	 * @param sizeY
	 * @param sizeZ
	 * @param img Images tp be thresholded
	 * @param threshold All pixels with intensities greater equal are object pixels
	 * @param invalidImg
	 */
	public MTBLevelsetMembership(int sizeX, int sizeY, int sizeZ, MTBImage img, int threshold, MTBImage invalidImg)
	{
			this(sizeX, sizeY, sizeZ);
			this.initLevelset(img, threshold, invalidImg);	
	}
	
	/**
	 * Creates a copy of <code>phi</code>.
	 * @param phi levelset function to copy
	 */

	public MTBLevelsetMembership(MTBLevelsetMembership phi)
	{
			this(phi.getSizeX(), phi.getSizeY(), phi.getSizeZ());
			this.numPhases = phi.getNumPhases();
			this.sizePhases = new int[numPhases+1];
			for ( short p = 1 ; p <= numPhases ; p++ ) 
				this.sizePhases[p] = phi.getSizePhase( p);
			this.sumSizeObject = phi.getPixelInside();

			for(int z = 0; z < this.sizeZ; z++)
				for(int y = 0; y < this.sizeY; y++)
					for(int x = 0; x < this.sizeX; x++)
						this.phi[z][y][x] = phi.getPhase(x, y, z);
	}
	
	/**
	 * Constructor for given size which is initialized from a list of 2D regions for a 2D level set function.
	 * If multiphase is true, then each region yields on phase thus the number of regions determines the
	 * the number of phases.
     * If multiphase is false, exactly one object phase is populated from all regions.
	 * <p>
	 * If invalidImg is non null, than pixels <> zero in invalidImg are invalid
	 * 
	 * @param sizeX
	 * @param sizeY
	 * @param regions	Vector of regions to initialize from
	 * @param invalidImg  pixels which are non zero are assumed invalid
	 * @param multiphase  if true create one phase for each region, otherwise two phases
	 */
	public MTBLevelsetMembership( int sizeX, int sizeY, Vector<MTBRegion2D> regions, MTBImage invalidImg, boolean multiphase)
	{
			this.sizeX = sizeX;
			this.sizeY = sizeY;
			this.sizeZ = 1;
			this.phi = new short[sizeZ][sizeY][sizeX];

			if ( multiphase ) {
				this.numPhases = regions.size()+1;
				if ( this.numPhases > MAX_ALLOWED_NUM_PHASES ) {
					throw new ArrayIndexOutOfBoundsException("MTBLevelsetMembership: trying to create a MTBLevelsetMembership with " + this.numPhases + " phases");
				}
			} else {
				this.numPhases = 2;
			} 

			this.sizePhases = new int[numPhases+1];
			for ( short p = 1 ; p <= numPhases ; p++ ) 
				this.sizePhases[p] = 0;
			this.sumSizeObject = 0;

			for(int y = 0; y < this.sizeY; y++)
				for(int x = 0; x < this.sizeX; x++)
					if ( isValid( invalidImg, x, y, 0) ) {
						this.phi[0][y][x] = BG_PHASE;
						sizePhases[BG_PHASE]++;
					} else {
						this.phi[0][y][x] = INVALID_PHASE;
					}
					

			// init the level set function and fill it from the regions
			for (int r = 0; r < regions.size(); ++r) {
				short phase = (short)(r+BG_PHASE+1);
				Vector<java.awt.geom.Point2D.Double> points = regions.elementAt(r).getPoints();
				for ( int i = 0 ; i < points.size() ; i++ ) {
					// set point to phase
					int x = (int)points.elementAt(i).getX();
					int y = (int)points.elementAt(i).getY();

					if ( this.phi[0][y][x] != INVALID_PHASE ) {
						if ( multiphase ) {
							this.phi[0][y][x] = phase;
							sizePhases[(short)(phase)]++;
						} else {
							this.phi[0][y][x] = BG_PHASE+1;
							sizePhases[BG_PHASE+1]++;
						} 

						this.sumSizeObject++;
						sizePhases[BG_PHASE]--;
					}
				}
			}
	}
	
	/**
	 * Constructor for given size which is initialized from a MTBRegion2DSet for a 2D level set function.
	 * If multiphase is true, each region yields on phase thus the number of regions determines the
	 * the number of phases.
	 * If multiphase is true, then each region yields on phase thus the number of regions determines the
	 * the number of phases.
     * If mutliphase is false, exactly one object phase is populated from all regions.
	 *
	 * If invalidImg is non null, than pixels <> zero in invalidImg are invalid
	 * 
	 * @param sizeX
	 * @param sizeY
	 * @param regions	Region2D set to initialize from
	 * @param invalidImg  pixels which are non zero are assumed invalid
	 * @param multiphase  if true create one phase for each region, otherwise two phases
	 */
	public MTBLevelsetMembership( int sizeX, int sizeY, MTBRegion2DSet regions, MTBImage invalidImg, boolean multiphase)
	{
			this.sizeX = sizeX;
			this.sizeY = sizeY;
			this.sizeZ = 1;
			this.phi = new short[sizeZ][sizeY][sizeX];

			if ( multiphase ) {
				this.numPhases = regions.size()+1;
				if ( this.numPhases > MAX_ALLOWED_NUM_PHASES ) {
					throw new ArrayIndexOutOfBoundsException("MTBLevelsetMembership: trying to create a MTBLevelsetMembership with " + this.numPhases + " phases");
				}
			} else {
				this.numPhases = 2;
			} 

			this.sizePhases = new int[numPhases+1];
			for ( short p = 1 ; p <= numPhases ; p++ ) 
				this.sizePhases[p] = 0;
			this.sumSizeObject = 0;

			for(int y = 0; y < this.sizeY; y++)
				for(int x = 0; x < this.sizeX; x++)
					if ( isValid( invalidImg, x, y, 0) ) {
						this.phi[0][y][x] = BG_PHASE;
						sizePhases[BG_PHASE]++;
					} else {
						this.phi[0][y][x] = INVALID_PHASE;
					}
					

			// init the level set function and fill it from the regions
			for (int r = 0; r < regions.size(); ++r) {
				short phase = (short)(r+BG_PHASE+1);
				Vector<java.awt.geom.Point2D.Double> points = regions.elementAt(r).getPoints();
				for ( int i = 0 ; i < points.size() ; i++ ) {
					// set point to phase
					int x = (int)points.elementAt(i).getX();
					int y = (int)points.elementAt(i).getY();

					if ( this.phi[0][y][x] != INVALID_PHASE ) {
						if ( multiphase ) {
							this.phi[0][y][x] = phase;
							sizePhases[(short)(phase)]++;
						} else {
							this.phi[0][y][x] = BG_PHASE+1;
							sizePhases[BG_PHASE+1]++;
						} 

						this.sumSizeObject++;
						sizePhases[BG_PHASE]--;
					}
				}
			}
	}
	
	/**
	 * Constructor for a two phase level set function from a label image.
	 * Label zero is interpreted as background, everything else as foreground, e.g. the only object phase. 
	 *
	 * If invalidImg is non null, than pixels <> zero in invalidImg are invalid
	 * 
	 * @param initLabelImg  Label image for initialization
	 * @param invalidImg  Image indicating invalid pixels with values <> 0
	 */
	public MTBLevelsetMembership( MTBImage initLabelImg, MTBImage invalidImg) {
		this.sizeX = initLabelImg.getSizeX();
		this.sizeY = initLabelImg.getSizeY();
		this.sizeZ = initLabelImg.getSizeZ();

		initFromLabels( initLabelImg, invalidImg, false);
	}

	/**
	 * Constructor for a level set function from a label image.
	 * if multiphase is true, then a multi phase level set function is created,
	 * where labels ared interpreted as phases (a label of zero indeicated background).
	 * Otherwise a two phase level set function is created,
	 * where all non zero labels are interpreted as foreground.
	 * <p>
	 * If invalidImg is non null, than pixels <> zero in invalidImg are invalid
	 * 
	 * @param initLabelImg  Label image for initialization
	 * @param invalidImg  Image indicating invalid pixels with values <> 0
	 */
	public MTBLevelsetMembership( MTBImage initLabelImg, MTBImage invalidImg, boolean multiphase) {
		this.sizeX = initLabelImg.getSizeX();
		this.sizeY = initLabelImg.getSizeY();
		this.sizeZ = initLabelImg.getSizeZ();

		initFromLabels( initLabelImg, invalidImg, multiphase);
	}

    /**
     * Helper for constructors from a label image.
     * If multiphase is true, then a multi phase level set function is created,
     * where labels are interpreted as phases (a label of zero indicates background).
     * The set of occurring in the label image are mapped to a contiguous interval
     * of phases (starting with BG_PHASE). The label 0 is mapped to BG_PHASE
     * <p>
     * Otherwise a two phase level set function is created,
     * where all non zero labels are interpreted as foreground.
     * <p>
     * If invalidImg is non null, than pixels <> zero in invalidImg are invalid
     * 
     * @param initLabelImg  Label image for initialization
     * @param invalidImg  Image indicating invalid pixels with values <> 0
     * @param multiphase Construct multi or two phase level set function iff true or false, resp.
     */
	private void initFromLabels( MTBImage initLabelImg, MTBImage invalidImg, boolean multiphase) {

			this.phi = new short[sizeZ][sizeY][sizeX];

			this.numPhases = 2;

			// this hash table maps all occurring labels to a contiguous interval of phases
			Hashtable<Integer,Integer> labelMap = new Hashtable<Integer,Integer>();
			labelMap.put( new Integer( BG_PHASE), 1);
			this.numPhases = 1;
			// count number of phases
			if ( multiphase ) {
				for(int z = 0; z < this.sizeZ; z++) {
					for(int y = 0; y < this.sizeY; y++) {
						for(int x = 0; x < this.sizeX; x++) {
							/*if ( isValid( invalidImg, x, y, z) &&
								 initLabelImg.getValueInt( x, y, z)+1 > this.numPhases ) {
								this.numPhases = (initLabelImg.getValueInt( x, y, z) + 1);
							}
							*/
							if ( isValid( invalidImg, x, y, z) &&
								 (! labelMap.containsKey( initLabelImg.getValueInt( x, y, z) + 1)) )  {
								this.numPhases++;
								labelMap.put( initLabelImg.getValueInt( x, y, z) + 1, this.numPhases);
							}
						}
					}
				}
			}

			if ( this.numPhases == 1 ) {
				this.numPhases = 2;
			}

			if ( this.numPhases > MAX_ALLOWED_NUM_PHASES ) {
				throw new ArrayIndexOutOfBoundsException("MTBLevelsetMembership: trying to create a MTBLevelsetMembership with " + this.numPhases + " phases");
			}

			this.sizePhases = new int[numPhases+1];
			for ( short p = 1 ; p <= numPhases ; p++ ) 
				this.sizePhases[p] = 0;
			this.sumSizeObject = 0;

			for(int z = 0; z < this.sizeZ; z++) {
				for(int y = 0; y < this.sizeY; y++) {
					for(int x = 0; x < this.sizeX; x++) {
						if ( isValid( invalidImg, x, y, z) ) {
							short phase;
							if ( multiphase) {
								int i = (labelMap.get(initLabelImg.getValueInt( x,y,z) + 1));
								phase = (short)i;
							} else {
								if ( initLabelImg.getValueInt( x,y,z) == 0)
									phase = BG_PHASE;
								else
									phase = BG_PHASE+1;
							}
							
							if ( phase  == BG_PHASE ) {
								this.phi[z][y][x] = BG_PHASE;
								sizePhases[BG_PHASE]++;
							} else {
								if ( ! multiphase )
									phase = BG_PHASE+1;

								this.phi[z][y][x] = phase;
								this.sumSizeObject++;
								sizePhases[phase]++;
							} 
						} else {
							this.phi[z][y][x] = INVALID_PHASE;
						}
					}
				}
			}
	}
	
	/**
	 * Helper to initialize a two phase level set function of size as
	 * given by invalidImg with a circle/sphere
	 * of radius 0.5*(sizeX+sizeY+sizeZ)/3.
	 * <p>
	 * If invalidImg is non null, than pixels <> zero in invalidImg are invalid
	 */
	private void initLevelset( MTBImage invalidImg)
	{
			int midX = this.sizeX/2;
			int midY = this.sizeY/2;
			int midZ = this.sizeZ/2;

			double radius = 0.5*(sizeX+sizeY+sizeZ)/3;

			// we do have two phases
			sizePhases[BG_PHASE] = this.sizePhases[BG_PHASE+1] = 0;
			sumSizeObject = 0;

			for(int z = 0; z < this.sizeZ; z++)	
			{
				for(int y = 0; y < this.sizeY; y++)
				{
					for(int x = 0; x < this.sizeX; x++)
					{
						if ( isValid( invalidImg, x, y, 0) ) {
							if ( Math.sqrt( (midZ - z)*(midZ - z) + (midY - y)* (midY - y)+ (midX - x)*(midX - x) ) < radius) {
								this.phi[z][y][x] = BG_PHASE+1;
								this.sumSizeObject++;
								sizePhases[BG_PHASE+1]++;
							} else {	
								this.phi[z][y][x] = BG_PHASE;
								sizePhases[BG_PHASE]++;
							}
						} else {
							this.phi[0][y][x] = INVALID_PHASE;
						}
					}
				}
			}
	}

	/**
	 * Initialize a two phase level set function by thresholding an image.
	 * All pixels with intensities greater equal the threshold constitute the object phase,
	 * other pixels the background.
	 * <p>
	 * If invalidImg is non null, than pixels <> zero in invalidImg are invalid
	 * 
	 * @param threshold
	 * @param threshold All pixels with intensities greater equal are object pixels
	 * @param invalidImg
	 */
	private void initLevelset(MTBImage img, int threshold, MTBImage invalidImg)
	{
		this.sumSizeObject = 0;
		sizePhases[BG_PHASE] = this.sizePhases[BG_PHASE+1] = 0;
			
		for(int z = 0; z < this.sizeZ; z++) {
			for(int y = 0; y < this.sizeY; y++) {
				for(int x = 0; x < this.sizeX; x++) {
					if ( isValid( invalidImg, x, y, z) ) {
						if(img.getValueInt(x, y, z, 0, 0) >= threshold) {
							this.phi[z][y][x] = BG_PHASE+1;
							this.sumSizeObject++;
							sizePhases[BG_PHASE+1]++;
						} else {
							this.phi[z][y][x] = BG_PHASE;
							sizePhases[BG_PHASE]++;
						}
					} else {
						this.phi[z][y][x] = INVALID_PHASE;
					}
				}
			}
		}
	}

	/**
	 * Change a pixel/voxel to a new phase.
	 * Note: validity of phase is NOT checked.
	 * @param x
	 * @param y
	 * @param z
	 * @param phase
	 */
	public void changePhase(int x, int y, int z, short phase)
	{
		if ( phase != this.phi[z][y][x]  ) {
			if(this.phi[z][y][x] == BG_PHASE) {
				this.sumSizeObject++;
			} else {
				this.sumSizeObject--;
			}

			sizePhases[this.phi[z][y][x]]--;
			sizePhases[phase]++;

			this.phi[z][y][x] = phase;
		}
	}
	
	/**
	 * Change a pixel to a new phase.
	 * Note: validity of phase is NOT checked.
	 * @param x
	 * @param y
	 * @param phase
	 */
	public void changePhase(int x, int y, short phase)
	{
		if ( phase != this.phi[0][y][x]  ) {
			if(this.phi[0][y][x] == BG_PHASE) {
				this.sumSizeObject++;
			} else {
				this.sumSizeObject--;
			}

			sizePhases[this.phi[0][y][x]]--;
			sizePhases[phase]++;

			this.phi[0][y][x] = phase;
		}
	}
	
	/** Return phase for a pixel/voxel
	 * @param x
	 * @param y
	 * @param z
	 */
	public short getPhase(int x, int y, int z)
	{
			return this.phi[z][y][x];	
	}

	/** Return phase for a pixel/voxel checking  limits of all coordinates.
	 * If coordinates are invalid, INVALID_PHASE is returned.
	 * @param x
	 * @param y
	 * @param z
	 * @return phase
	 */
	public short getPhaseCheckXYZ(int x, int y, int z)
	{
			if ( x >= 0 && x < sizeX &&
				 y >= 0 && y < sizeY &&
				 z >= 0 && z < sizeZ ) 
				return this.phi[z][y][x];	
			else
				return INVALID_PHASE;
	}

	/** Return phase for a pixel/voxel, z coordinate defaults to zero
	 * @param x
	 * @param y
	 */
	public short getPhase(int x, int y)
	{
			return this.phi[0][y][x];	
	}

	/** Return phase for a pixel checking  limits of all coordinates.
	 * If coordinates are invalid, INVALID_PHASE is returned.
	 * @param x
	 * @param y
	 * @return phase
	 */
	public short getPhaseCheckXY(int x, int y)
	{
			if ( x >= 0 && x < sizeX &&
				 y >= 0 && y < sizeY )
				return this.phi[0][y][x];	
			else
				return INVALID_PHASE;
	}

	/**
	 * Creates a bi-level MTBImageShort of the level set function.
	 * Background and invalid pixels get intensity 0, object pixels intensity 255.
	 *
	 * @return MTBImage
	 */
	public MTBImageShort getMTBImage()
	{
			MTBImageShort image = (MTBImageShort)MTBImage.createMTBImage(sizeX, sizeY, sizeZ, 1, 1, MTBImageType.MTB_SHORT);
		
			for(int z = 0; z < this.sizeZ; z++)
				for(int y = 0; y < this.sizeY; y++)
					for(int x = 0; x < this.sizeX; x++)
						if(this.phi[z][y][x] <= BG_PHASE) image.putValueDouble(x, y, z, 0, 0, 0);
						else image.putValueDouble(x, y, z, 0, 0, 255);
		
			return image;
	}

	/**
	 * Creates a MTBImageShort of the level set function.
	 *	Background and invalid pixels get value 0, the object phases
	 *	get values starting with 1
	 * @return MTBImageShort
	 */
	public MTBImageShort getMTBImageLabel()
	{
			MTBImageShort image = (MTBImageShort)MTBImage.createMTBImage(sizeX, sizeY, sizeZ, 1, 1, MTBImageType.MTB_SHORT );
		
			for(int z = 0; z < this.sizeZ; z++)
				for(int y = 0; y < this.sizeY; y++)
					for(int x = 0; x < this.sizeX; x++)
						if(this.phi[z][y][x] <= 1) image.putValueDouble(x, y, z, 0, 0, 0);
						else image.putValueDouble(x, y, z, 0, 0, phi[z][y][x]-1);
		
			return image;
	}

//	//TODO
//	// this should maybe go into level set membership
//	// however: a MTBLevelsetMembership is not capable of holding gradients, as it
//	//	has only short values
//	// probably this should return a MTBImage and not a MTBLevelsetMembership
//	/**
//	 * calculates gradient level set function
//	 */
//	public MTBLevelsetMembership getGradient()
//	{
//			MTBLevelsetMembership phi = new MTBLevelsetMembership(this);
//			
//			for(int z = 0; z < this.sizeZ; z++)
//			{	
//				for(int y = 0; y < this.sizeY; y++)
//				{
//					for(int x = 0; x < this.sizeX; x++)
//					{
//						int distX = 0;
//						int distY = 0;
//						int distZ = 0;
//						if(z > 0 && z < this.sizeZ - 1) distZ = this.phi[z + 1][y][x] - this.phi[z - 1][y][x];
//						if(y > 0 && y < this.sizeY - 1) distY = this.phi[z][y + 1][x] - this.phi[z][y - 1][x];
//						if(x > 0 && x < this.sizeX - 1) distX = this.phi[z][y][x + 1] - this.phi[z][y][x - 1];
//						phi.putPhase(x, y, z, (short)(Math.sqrt((double)(distX * distX + distY * distY + distZ * distZ)) + 1.0));
//					}
//				}
//			}
//			return phi;
//	}
	
	/** 
	 * Return number of pixels of all object phases.
	 */
	public int getPixelInside()
	{
			return this.sumSizeObject;
	}
	
	/** 
	 * Return number of pixels of background phase.
	 */
	public int getPixelOutside()
	{
			return this.sizePhases[BG_PHASE];
	}
	
	/** 
	 * Return X size of the level set function.
	 */
	public int getSizeX()
	{
			return this.sizeX;
	}	
	
	/** 
	 * Return Y size of the level set function.
	 */
	public int getSizeY()
	{
			return this.sizeY;
	}	
	
	/** 
	 * Return Z size of the level set function.
	 */
	public int getSizeZ()
	{
			return this.sizeZ;
	}

	/**
	 * Return the size, i.e. number of pixel/voxels of a phase.
	 */
	public int getSizePhase( short phase)
	{
			return this.sizePhases[phase];
	}

	/**
	 * Return .e. number phases.
	 */
	public int getNumPhases()
	{
			return this.numPhases;
	}

	/**
	 * Return a string representation.
	 */
	public  String 	toString() {
		String str = new String( "MTBLevelsetMembership with " + numPhases + " phases; size of phases ");
		for ( short p = 1 ; p <= numPhases ; p++ )
			str = str.concat( getSizePhase(p) + " ");
		return str;

	}
	
	/**
      * returns true, if pixel/voxel is valid. Valid pixels are define as zero pixels
      * (i.e. invalid pixels are marks with phase not equal to zero
      * 
      * @param invalidImg
      * @param x
      * @param y
      * @param z
	  */
	public  boolean isValid( MTBImage invalidImg, int x, int y, int z) {
		return (invalidImg == null) || (invalidImg.getValueInt( x,y,z) == 0);
	}
}
