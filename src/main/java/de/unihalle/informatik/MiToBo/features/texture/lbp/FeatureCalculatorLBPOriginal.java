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

package de.unihalle.informatik.MiToBo.features.texture.lbp;

import org.apache.commons.lang3.ArrayUtils;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageInt;
import de.unihalle.informatik.MiToBo.features.FeatureCalculator;

/**
 * Operator for extracting Local Binary Patterns (LBPs).
 * <p>
 * This operator is based on the original publication of LBPs:
 * <p>
 * Ojala, Pietikaeinen, Harwood, <i>"A Comparative Study of Texture 
 * Measures with Classification based on Feature Distributions"</i>,<br>
 * Pattern Recognition, Vol. 29, No. 1, pp. 51-59, 1996
 * <p>
 * In addition to the original method here optionally not only the 8
 * direct neighbors of a pixel are considered for local LBP code generation, 
 * but also the next 16 or 24 surrounding pixels (corresponding to R=1, 
 * R=2 and R=3):
 * <br><br>
 *  3 3 3 3 3 3 3<br> 
 *  3 2 2 2 2 2 3<br>
 *  3 2 1 1 1 2 3<br>
 *  3 2 1 1 1 2 3<br>
 *  3 2 1 1 1 2 3<br>
 *  3 2 2 2 2 2 3<br>
 *  3 3 3 3 3 3 3<br>
 * <p>
 * All three calculated LBP codes are each binned in a corresponding histogram
 * which is calculated over the whole image. All three histograms are then 
 * concatenated and returned as result. According to the chosen neighborhood 
 * context the dimensionality of this result varies naturally.
 * <p>
 * Note that this class is not optimized for efficiency!
 * 
 * @author Alexander Weiss
 * @author Birgit Moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL, 
	level=Level.STANDARD, allowBatchMode=false)
@ALDDerivedClass
public class FeatureCalculatorLBPOriginal extends FeatureCalculator
{
	/**
	 * Kind of neighborhood to be considered for code calculation.
	 */
	public static enum NeighborhoodSelection {
		/**
		 * Only R = 1, i.e. 8 neighhbors. 
		 */
		NB_1,
		/**
		 * Only R = 2, i.e. 16 neighhbors. 
		 */
		NB_2,
		/**
		 * Only R = 3, i.e. 24 neighhbors. 
		 */
		NB_3,
		/**
		 * R = 1 and R = 2 combined. 
		 */
		NB_12,
		/**
		 * R = 1 and R = 3 combined. 
		 */
		NB_13,
		/**
		 * R = 2 and R = 3 combined. 
		 */
		NB_23,
		/**
		 * R = 1, R = 2 and R = 3 combined. 
		 */
		NB_123
	}
	
	/**
	 * Neighborhood(s) to be considered.
	 */
	@Parameter(label = "Neighborhood", direction = Parameter.Direction.IN, 
		description = "Neighborhood(s) to consider.")
	protected NeighborhoodSelection nbMode = NeighborhoodSelection.NB_123;
	
	/**
	 * Show additional result image.
	 */
	@Parameter(label = "Show Code Stack?", 
		direction = Parameter.Direction.IN, supplemental = true, 
		description = "Enable/disable display of code images in a stack.")
	protected boolean showCodeStack = false;

	/**
	 * Additional code stack.
	 */
	@Parameter(label = "Result Code Stack", 
		direction = Parameter.Direction.OUT, supplemental = true, 
		description = "Stack with LBP code images.")
	protected MTBImageInt codeStack;

	/*
	 * Local helpers.
	 */
	
	/**
	 * Channel index of code image for R = 1.
	 */
	private int indexR1 = -1;
	/**
	 * Channel index of code image for R = 2.
	 */
	private int indexR2 = -1;
	/**
	 * Channel index of code image for R = 3.
	 */
	private int indexR3 = -1;
	
	/**
	 * Default constructor.
	 * @throws ALDOperatorException Thrown in case of failure.
	 */
	public FeatureCalculatorLBPOriginal() throws ALDOperatorException {
		// nothing to do here
	}
	
	@Override
  public void operate() {
		
		this.codeStack = null;
		
		int[] resultHistos = this.extractFeatures();
		double[] features = new double[resultHistos.length];
		for(int i=0; i<resultHistos.length; i++) {
			features[i] = resultHistos[i];
		}
		this.resultObj = new FeatureCalculatorLBPResult(features);
  }

	@Override
	protected FeatureCalculatorLBPResult getResultDataObjectInvalid(int dim) {
		double[] nanResult = new double[dim];
		for (int i=0; i<dim; ++i)
			nanResult[i] = Double.NaN;
		return new FeatureCalculatorLBPResult(nanResult);
	}

	/**
	 * Method to extract code histogram from the given image.
	 * @return Histogram of LBP codes.
	 */
	protected int[] extractFeatures() {
		
		int[] H1 = null;
		int[] H2 = null;
		int[] H3 = null;
		
		int codeNum = 0;
		if (   this.nbMode == NeighborhoodSelection.NB_1 
				|| this.nbMode == NeighborhoodSelection.NB_12
				|| this.nbMode == NeighborhoodSelection.NB_13
				|| this.nbMode == NeighborhoodSelection.NB_123) {
			H1 = new int[8];
			this.indexR1 = codeNum;
			++codeNum;
		}
		if (   this.nbMode == NeighborhoodSelection.NB_2 
				|| this.nbMode == NeighborhoodSelection.NB_12
				|| this.nbMode == NeighborhoodSelection.NB_23
				|| this.nbMode == NeighborhoodSelection.NB_123) {
			H2 = new int[16];
			this.indexR2 = codeNum;
			++codeNum;
		}
		if (   this.nbMode == NeighborhoodSelection.NB_3 
				|| this.nbMode == NeighborhoodSelection.NB_13
				|| this.nbMode == NeighborhoodSelection.NB_23
				|| this.nbMode == NeighborhoodSelection.NB_123) {
			H3 = new int[16];
			this.indexR3 = codeNum;
			++codeNum;
		}
		
		if (codeNum == 0)
			return null;
		
		if (this.showCodeStack)
			this.codeStack = (MTBImageInt)MTBImage.createMTBImage(
				this.inImg.getSizeX(),this.inImg.getSizeY(), codeNum, 1, 1, 
					MTBImageType.MTB_INT);
		
		//H1: r=1, Neighbors=8
		//H2: r=2, Neighbors=16
		//H3: r=3, Neighbors=24
		for(int x=0; x < this.inImg.getSizeX(); x++) {
			for (int y=0; y < this.inImg.getSizeY(); y++) {
				int center = this.inImg.getValueInt(x, y);
				if (   x > 1 && y > 1 
						&& x < this.inImg.getSizeX() - 1	
						&& y < this.inImg.getSizeY() - 1) {
					if (H1 != null) {
						int index8 = getLPB8(x, y, center);
						H1[index8]++;
					}
					if (   x > 2 && y > 2 
							&& x < this.inImg.getSizeX() - 2	
							&& y < this.inImg.getSizeY() - 2) {
						if (H2 != null) {
							int index16 = getLPB16(x, y, center);
							H2[index16]++;
						}
						if (   x > 3 && y > 3 
								&& x < this.inImg.getSizeX() - 3	
								&& y < this.inImg.getSizeY() - 3) {
							if (H3 != null) {
								int index24 = getLPB24(x, y, center);
								H3[index24]++;
							}
						}
					}
				}
			}
		}
		int[] features = null;
		if (H1 != null)
			features = ArrayUtils.addAll(H1, null);
		if (H2 != null)
			features = ArrayUtils.addAll(features, H2);
		if (H3 != null)
			features = ArrayUtils.addAll(features, H3);
		return features;
	}
	
	/**
	 * Extract codes for 8-pixel neighborhood with r=1. 
	 * @param x	      x-coordinate of center pixel to consider.
	 * @param y       y-coordinate of center pixel to consider.
	 * @param center	Intensity of center pixel.
	 * @return	Index of histogram bin of resulting LBP code, 
	 * 					-1 in case of error.
	 */
	private int getLPB8(int x, int y, double center) {
		int index = -1;
		int pattern = 0;
		if(center >= this.inImg.getValueDouble(x+1, y  )) pattern += 1; 
		if(center >= this.inImg.getValueDouble(x+1, y-1)) pattern += 2; 
		if(center >= this.inImg.getValueDouble(x  , y-1)) pattern += 4; 
		if(center >= this.inImg.getValueDouble(x-1, y-1)) pattern += 8; 
		if(center >= this.inImg.getValueDouble(x-1, y  )) pattern += 16; 
		if(center >= this.inImg.getValueDouble(x-1, y+1)) pattern += 32; 
		if(center >= this.inImg.getValueDouble(x  , y+1)) pattern += 64; 
		if(center >= this.inImg.getValueDouble(x+1, y+1)) pattern += 128; 

		if (this.codeStack != null)
			this.codeStack.putValueInt(x, y, this.indexR1, 0, 0, pattern);
		
		if(pattern < 32) 
			return 0; 
		if(pattern < 64) 
			return 1; 
		if(pattern < 96) 
			return 2; 
		if(pattern < 128) 
			return 3; 
		if(pattern < 160) 
			return 4; 
		if(pattern < 192) 
			return 5; 
		if(pattern < 224) 
			return 6; 
		if(pattern < 256) 
			return 7; 
		return index;
	}
	
	/**
	 * Extract codes for 16-pixel neighborhood with r=2. 
	 * @param x	      x-coordinate of center pixel to consider.
	 * @param y       y-coordinate of center pixel to consider.
	 * @param center	Intensity of center pixel.
	 * @return	Index of histogram bin of resulting LBP code, 
	 * 					-1 in case of error.
	 */
	private int getLPB16(int x, int y, double center) {
		int index = -1;
		int pattern = 0;
		if(center >= this.inImg.getValueDouble(x+2, y  )) pattern += 1; 
		if(center >= this.inImg.getValueDouble(x+2, y-1)) pattern += 2; 
		if(center >= this.inImg.getValueDouble(x+2, y-2)) pattern += 4; 
		if(center >= this.inImg.getValueDouble(x+1, y-2)) pattern += 8; 
		if(center >= this.inImg.getValueDouble(x  , y-2)) pattern += 16; 
		if(center >= this.inImg.getValueDouble(x-1, y-2)) pattern += 32; 
		if(center >= this.inImg.getValueDouble(x-2, y-2)) pattern += 64; 
		if(center >= this.inImg.getValueDouble(x-2, y-1)) pattern += 128; 
		if(center >= this.inImg.getValueDouble(x-2, y  )) pattern += 256;
		if(center >= this.inImg.getValueDouble(x-2, y+1)) pattern += 512;
		if(center >= this.inImg.getValueDouble(x-2, y+2)) pattern += 1024;
		if(center >= this.inImg.getValueDouble(x-1, y+2)) pattern += 2048;
		if(center >= this.inImg.getValueDouble(x  , y+2)) pattern += 4096;
		if(center >= this.inImg.getValueDouble(x+1, y+2)) pattern += 8192;
		if(center >= this.inImg.getValueDouble(x+2, y+2)) pattern += 16384;
		if(center >= this.inImg.getValueDouble(x+2, y+1)) pattern += 32768;
		
		if (this.codeStack != null)
			this.codeStack.putValueInt(x, y, this.indexR2, 0, 0, pattern);

		if(pattern < 4096) 
			return 0;
		if(pattern < 8192) 
			return 1; 
		if(pattern < 12288) 
			return 2;
		if(pattern < 16384) 
			return 3; 
		if(pattern < 20480) 
			return 4;
		if(pattern < 24576) 
			return 5;
		if(pattern < 28672) 
			return 6;
		if(pattern < 32768) 
			return 7;
		if(pattern < 36864) 
			return 8;
		if(pattern < 40960) 
			return 9; 
		if(pattern < 45056) 
			return 10; 
		if(pattern < 49152) 
			return 11; 
		if(pattern < 53248)
			return 12;
		if(pattern < 57344) 
			return 13; 
		if(pattern < 61440)
			return 14;
		if(pattern < 65536)
			return 15;
		
		return index;
	}

	/**
	 * Extract codes for 24-pixel neighborhood with r=3. 
	 * @param x	      x-coordinate of center pixel to consider.
	 * @param y       y-coordinate of center pixel to consider.
	 * @param center	Intensity of center pixel.
	 * @return	Index of histogram bin of resulting LBP code, 
	 * 					-1 in case of error.
	 */
	private int getLPB24(int x, int y, double center) {
		
		int index = -1;
		int pattern = 0;
		
		if(center >= this.inImg.getValueDouble(x+3,   y)) pattern += 1; 
		if(center >= this.inImg.getValueDouble(x+3, y-1)) pattern += 2; 
		if(center >= this.inImg.getValueDouble(x+3, y-2)) pattern += 4; 
		if(center >= this.inImg.getValueDouble(x+3, y-3)) pattern += 8; 
		if(center >= this.inImg.getValueDouble(x+2, y-3)) pattern += 16; 
		if(center >= this.inImg.getValueDouble(x+1, y-3)) pattern += 32; 
		if(center >= this.inImg.getValueDouble(x  , y-3)) pattern += 64; 
		if(center >= this.inImg.getValueDouble(x-1, y-3)) pattern += 128; 
		if(center >= this.inImg.getValueDouble(x-2, y-3)) pattern += 256;
		if(center >= this.inImg.getValueDouble(x-3, y-3)) pattern += 512;
		if(center >= this.inImg.getValueDouble(x-3, y-2)) pattern += 1024;
		if(center >= this.inImg.getValueDouble(x-3, y-1)) pattern += 2048;
		if(center >= this.inImg.getValueDouble(x-3, y  )) pattern += 4096;
		if(center >= this.inImg.getValueDouble(x-3, y+1)) pattern += 8192;
		if(center >= this.inImg.getValueDouble(x-3, y+2)) pattern += 16384;
		if(center >= this.inImg.getValueDouble(x-3, y+3)) pattern += 32768;
		if(center >= this.inImg.getValueDouble(x-2, y+3)) pattern += 65536; 
		if(center >= this.inImg.getValueDouble(x-1, y+3)) pattern += 131072; 
		if(center >= this.inImg.getValueDouble(x  , y+3)) pattern += 262144; 
		if(center >= this.inImg.getValueDouble(x+1, y+3)) pattern += 524288; 
		if(center >= this.inImg.getValueDouble(x+2, y+3)) pattern += 1048576; 
		if(center >= this.inImg.getValueDouble(x+3, y+3)) pattern += 2097152; 
		if(center >= this.inImg.getValueDouble(x+3, y+2)) pattern += 4194304; 
		if(center >= this.inImg.getValueDouble(x+3, y+1)) pattern += 8388608; 

		if (this.codeStack != null)
			this.codeStack.putValueInt(x, y, this.indexR3, 0, 0, pattern);

		if(pattern < 1048576) 
			return 0;
		if(pattern < 2097152) 
			return 1;
		if(pattern < 3145728) 
			return 2;
		if(pattern < 4194304) 
			return 3;
		if(pattern < 5242880) 
			return 4;
		if(pattern < 6291456) 
			return 5;
		if(pattern < 7340032) 
			return 6;
		if(pattern < 8388608) 
			return 7;
		if(pattern < 9437184) 
			return 8;
		if(pattern < 10485760) 
			return 9;
		if(pattern < 11534336) 
			return 10;
		if(pattern < 12582912) 
			return 11;
		if(pattern < 13631488) 
			return 12;
		if(pattern < 14680064) 
			return 13;
		if(pattern < 15728640) 
			return 14;
		if(pattern < 16777216) 
			return 15;
		
		return index;		
	}
}
