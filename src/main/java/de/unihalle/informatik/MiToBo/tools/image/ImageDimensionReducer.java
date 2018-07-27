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

package de.unihalle.informatik.MiToBo.tools.image;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;

/**
 * Operator to reduce image dimension by projecting  multi-dimensional 
 * images along selected dimensions.
 * <p>
 * This operator can for example be applied to generate a maximum 
 * projection of a z-stack, or to project images row- or columnwise.
 * 
 * @author gress
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,
	level=Level.STANDARD)
public class ImageDimensionReducer extends MTBOperator {
	
	/**
	 * Methods for dimension reduction/projection. 
	 */
	public static enum ReducerMethod {
		/**
		 * Computes the mean value along the specified dimension(s).
		 */
		MEAN, 
		/**
		 * Finds the minimum value along the specified dimension(s).
		 */
		MIN, 
		/**
		 * Finds the maximum value along the specified dimension(s).
		 */
		MAX, 
		/**
		 * Computes the sum along the specified dimension(s).
		 */
		SUM, 
		/**
		 * Computes the normalized sum along the specified dimension(s).
		 * <p>
		 * The resulting image values sum up to 1, i.e. refer to a discrete
		 * probability density function.
		 */
		NORM_SUM 
	}
	
	/**
	 * Multi-dimensional input image or input stack.
	 */
	@Parameter( label= "Input image", required = true, dataIOOrder = -1,
		direction = Parameter.Direction.IN, mode=ExpertMode.STANDARD,
		description = "Input image to reduce in dimension.")
	private MTBImage inImg = null;

	/**
	 * Projection mode.
	 */
	@Parameter( label= "Reduction Mode", required = true,
		direction = Parameter.Direction.IN,	mode=ExpertMode.STANDARD, 
		dataIOOrder=0, description = "Reduction mode.")
	private ReducerMethod projMode = ReducerMethod.MAX;

	/**
	 * Enables/disables projection along x-dimension.
	 */
	@Parameter( label= "Project along x", required = true, dataIOOrder = 1,
		direction = Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
		description = "Select for projection along x-dimension.")
	private boolean projectAlongX = false;

	/**
	 * Enables/disables projection along y-dimension.
	 */
	@Parameter( label= "Project along y", required = true, dataIOOrder = 2,
		direction = Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
		description = "Select for projection along y-dimension.")
	private boolean projectAlongY = false;
	
	/**
	 * Enables/disables projection along z-dimension.
	 */
	@Parameter( label= "Project along z", required = true, dataIOOrder = 3,
		direction = Parameter.Direction.IN, mode=ExpertMode.STANDARD,
		description = "Select for projection along z-dimension.")
	private boolean projectAlongZ = false;

	/**
	 * Enables/disables projection along t-dimension.
	 */
	@Parameter( label= "Project along t", required = true, dataIOOrder = 4,
		direction = Parameter.Direction.IN, mode=ExpertMode.STANDARD,
		description = "Select for projection along t-dimension.")
	private boolean projectAlongT = false;
	
	/**
	 * Enables/disables projection along c-dimension.
	 */
	@Parameter( label= "Project along c", required = true, dataIOOrder = 5,
		direction = Parameter.Direction.IN, mode=ExpertMode.STANDARD,
		description = "Select for projection along c-dimension.")
	private boolean projectAlongC = false;

	/**
	 * Result image (stack).
	 */
	@Parameter( label= "Result image", dataIOOrder = 0,
		direction = Parameter.Direction.OUT, mode=ExpertMode.STANDARD, 
		description = "Resulting dimension-reduced image.")
	private MTBImage resultImg = null;

	/**
	 * Constructor. 
	 * @throws ALDOperatorException Thrown in case of failure.
	 */
	public ImageDimensionReducer() throws ALDOperatorException {
		// nothing to do here
	}
	
	/**
	 * Constructor with arguments.
	 * @param img 		Input image.
	 * @param reduceX Flag for x-dimension reduction.
	 * @param reduceY Flag for y-dimension reduction.
	 * @param reduceZ Flag for z-dimension reduction.
	 * @param reduceT Flag for t-dimension reduction.
	 * @param reduceC Flag for c-dimension reduction.
	 * @param method 	Reduction method.
	 * @throws ALDOperatorException Thrown in case of failure.
	 */
	public ImageDimensionReducer(MTBImage img, boolean reduceX,
		boolean reduceY, boolean reduceZ,	boolean reduceT,
			boolean reduceC, ReducerMethod method) 
		throws ALDOperatorException {
		
		this.setInImg(img);
		this.setReduceX(reduceX);
		this.setReduceY(reduceY);
		this.setReduceZ(reduceZ);
		this.setReduceT(reduceT);
		this.setReduceC(reduceC);
		this.setReductionMode(method);
	}
	
	@Override
	protected void operate() throws ALDOperatorException {
		
		this.resultImg = this.reduce();
		
		if (this.resultImg == null) {
			throw new ALDOperatorException(
				OperatorExceptionType.OPERATE_FAILED, 
					"ImageDimensionReducer.operate() failed: " 
						+ "Result image is 'null'");
		}
	}
	

	/**
	 * Method which implements the dimension reduction.
	 * @return	Dimension-reduced image.
	 * @throws ALDOperatorException	Thrown in case of failure.
	 */
	private MTBImage reduce() throws ALDOperatorException {
		
		MTBImage img = this.inImg;
		
		int isizeX = img.getSizeX();
		int isizeY = img.getSizeY();
		int isizeZ = img.getSizeZ();
		int isizeT = img.getSizeT();
		int isizeC = img.getSizeC();

		IntObject c = new IntObject(), t = new IntObject(), 
				z = new IntObject(), y = new IntObject(), x = new IntObject();
		
		int sizeX, sizeY, sizeZ, sizeT, sizeC;
		IntObject xx, yy, zz, tt, cc;
		
		int reducedDimSize = 1;
		
		if (this.projectAlongX) {
			reducedDimSize *= isizeX;
			sizeX = 1;
			xx = new IntObject();
		}
		else {
			sizeX = isizeX;
			xx = x;
		}
		
		if (this.projectAlongY) {
			reducedDimSize *= isizeY;
			sizeY = 1;
			yy = new IntObject();
		}
		else {
			sizeY = isizeY;
			yy = y;
		}
		
		if (this.projectAlongZ) {
			reducedDimSize *= isizeZ;
			sizeZ = 1;
			zz = new IntObject();
		}
		else {
			sizeZ = isizeZ;
			zz = z;
		}
		
		if (this.projectAlongT) {
			reducedDimSize *= isizeT;
			sizeT = 1;
			tt = new IntObject();
		}
		else {
			sizeT = isizeT;
			tt = t;
		}
		
		if (this.projectAlongC) {
			reducedDimSize *= isizeC;
			sizeC = 1;
			cc = new IntObject();
		}
		else {
			sizeC = isizeC;
			cc = c;
		}
		
		MTBImage result = null;
		
		if (   this.projMode == ReducerMethod.MIN
				|| this.projMode == ReducerMethod.MAX) {
			
			result = MTBImage.createMTBImage(sizeX, sizeY, sizeZ, sizeT, sizeC, 
					img.getType());
			
		}
		else {	
			result = MTBImage.createMTBImage(sizeX, sizeY, sizeZ, sizeT, sizeC, 
					MTBImageType.MTB_DOUBLE);
			result.fillBlack();
		}
		
		if (this.projMode == ReducerMethod.MIN) {
			
			for (t.i = 0; t.i < isizeT; t.i++) {
				for (z.i = 0; z.i < isizeZ; z.i++) {
					for (c.i = 0; c.i < isizeC; c.i++) {
						for (y.i = 0; y.i < isizeY; y.i++) {
							for (x.i = 0; x.i < isizeX; x.i++) {
								
								if (   x.i == xx.i && y.i == yy.i && z.i == zz.i 
										&& t.i == tt.i && c.i == cc.i) {
									result.putValueDouble(xx.i, yy.i, zz.i, tt.i, cc.i, 
											img.getValueDouble(x.i, y.i, z.i, t.i, c.i));
								}
								else {
									if (result.getValueDouble(xx.i, yy.i, zz.i, tt.i, cc.i) 
											> img.getValueDouble(x.i, y.i, z.i, t.i, c.i)) {
										result.putValueDouble(xx.i, yy.i, zz.i, tt.i, cc.i, 
												img.getValueDouble(x.i, y.i, z.i, t.i, c.i));
									}
								}
							}
						}
					}
				}
			}
			
		}
		else if (this.projMode == ReducerMethod.MAX) {
			
			for (t.i = 0; t.i < isizeT; t.i++) {
				for (z.i = 0; z.i < isizeZ; z.i++) {
					for (c.i = 0; c.i < isizeC; c.i++) {
						for (y.i = 0; y.i < isizeY; y.i++) {
							for (x.i = 0; x.i < isizeX; x.i++) {
								
								if (   x.i == xx.i && y.i == yy.i && z.i == zz.i 
										&& t.i == tt.i && c.i == cc.i) {
									result.putValueDouble(xx.i, yy.i, zz.i, tt.i, cc.i, 
											img.getValueDouble(x.i, y.i, z.i, t.i, c.i));
								}
								else {
									if (result.getValueDouble(xx.i, yy.i, zz.i, tt.i, cc.i) 
											< img.getValueDouble(x.i, y.i, z.i, t.i, c.i)) {
										result.putValueDouble(xx.i, yy.i, zz.i, tt.i, cc.i, 
												img.getValueDouble(x.i, y.i, z.i, t.i, c.i));
									}
								}
							}
						}
					}
				}
			}
			
		}
		else {
			// sum values
			for (t.i = 0; t.i < isizeT; t.i++) {
				for (z.i = 0; z.i < isizeZ; z.i++) {
					for (c.i = 0; c.i < isizeC; c.i++) {
						for (y.i = 0; y.i < isizeY; y.i++) {
							for (x.i = 0; x.i < isizeX; x.i++) {
								
								result.putValueDouble(xx.i, yy.i, zz.i, tt.i, cc.i, 
									result.getValueDouble(xx.i, yy.i, zz.i, tt.i, cc.i) 
										+ img.getValueDouble(x.i, y.i, z.i, t.i, c.i));
			
							}
						}
					}
				}
			}
	
			if (this.projMode == ReducerMethod.MEAN) {
				// compute mean
				int ss = result.getSizeStack();
				for (int i = 0; i < ss; i++) {
					result.setCurrentSliceIndex(i);
					for (int iy = 0; iy < sizeY; iy++) {
						for (int ix = 0; ix < sizeX; ix++) {
							result.putValueDouble(ix, iy, 
								result.getValueDouble(ix, iy)/reducedDimSize);
						}
					}
				}
				result.setCurrentSliceIndex(0);
			}
			else if (this.projMode == ReducerMethod.NORM_SUM) {
				
				// compute normalizer
				int ss = result.getSizeStack();
				double n = 0.0;
				for (int i = 0; i < ss; i++) {
					result.setCurrentSliceIndex(i);
					for (int iy = 0; iy < sizeY; iy++) {
						for (int ix = 0; ix < sizeX; ix++) {
							n += result.getValueDouble(ix, iy);
						}
					}
				}
				
				// normalize
				for (int i = 0; i < ss; i++) {
					result.setCurrentSliceIndex(i);
					for (int iy = 0; iy < sizeY; iy++) {
						for (int ix = 0; ix < sizeX; ix++) {
							result.putValueDouble(ix, iy, 
								result.getValueDouble(ix, iy)/n);
						}
					}
				}
				
				result.setCurrentSliceIndex(0);
			}
		}
		
		return result;
	}
	
	/** 
	 * Enable/disable projection along z-dimension.
	 * @param value 	If true, projection along z is enabled.
	 */
	public void setReduceZ( boolean value ) {
		 this.projectAlongZ = value;
	}

	/** 
	 * Enable/disable projection along c-dimension.
	 * @param value 	If true, projection along c is enabled.
	 */
	public void setReduceC( boolean value ) {
		 this.projectAlongC = value;
	}

	/** 
	 * Enable/disable projection along y-dimension.
	 * @param value 	If true, projection along y is enabled.
	 */
	public void setReduceY( boolean value ) {
		 this.projectAlongY = value;
	}

	/** 
	 * Enable/disable projection along x-dimension.
	 * @param value 	If true, projection along x is enabled.
	 */
	public void setReduceX( boolean value ) {
		 this.projectAlongX = value;
	}

	/** 
	 * Enable/disable projection along t-dimension.
	 * @param value 	If true, projection along t is enabled.
	 */
	protected void setReduceT( boolean value ) {
		 this.projectAlongT = value;
	}

	/** 
	 * Specify projection mode.
	 * @param value 	Mode to apply.
	 */
	public void setReductionMode( ReducerMethod value ) {
		 this.projMode = value;
	}

	/** 
	 * Set input image.
	 * @param img 	Image to process.
	 */
	public void setInImg( MTBImage img ) {
		 this.inImg = img;
	}

	/** 
	 * Get result image.
	 * @return Result image.
	 */
	public MTBImage getResultImg() {
		return this.resultImg;
	}
	
	/**
	 * Internal helper class for iterating over an image dimension.
	 */
	private class IntObject {
		
		/**
		 * Internal position counter.
		 */
		public int i;
		
		/**
		 * Constructor.
		 */
		private IntObject() {
			this.i = 0;
		}
	}
	
	
}
