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

/* 
 * Most recent change(s):
 * 
 * $Rev$
 * $Date$
 * $Author$
 * 
 */

package de.unihalle.informatik.MiToBo.tools.image;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;

@ALDMetaInfo(export=ExportPolicy.ALLOWED)
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,level=Level.STANDARD)
public class ImageDimensionReducer extends MTBOperator {
	

	@Parameter( label= "reduceX", required = true, direction = Parameter.Direction.IN, 
			 mode=ExpertMode.STANDARD, dataIOOrder=3,
	                        description = "Set true reduction in/along x-dimension")
	private Boolean reduceX = false;

	@Parameter( label= "reduceY", required = true, direction = Parameter.Direction.IN, 
			 mode=ExpertMode.STANDARD, dataIOOrder=4,
	                        description = "Set true reduction in/along y-dimension")
	private Boolean reduceY = false;
	
	@Parameter( label= "reduceZ", required = true, direction = Parameter.Direction.IN, 
			 mode=ExpertMode.STANDARD, dataIOOrder=5,
	                        description = "Set true reduction in/along z-dimension")
	private Boolean reduceZ = false;

	@Parameter( label= "reduceT", required = true, direction = Parameter.Direction.IN, 
			 mode=ExpertMode.STANDARD, dataIOOrder=6,
	                        description = "Set true reduction in/along t-dimension")
	private Boolean reduceT = false;
	
	@Parameter( label= "reduceC", required = true, direction = Parameter.Direction.IN, 
			 mode=ExpertMode.STANDARD, dataIOOrder=7,
	                        description = "Set true reduction in/along c-dimension")
	private Boolean reduceC = false;


	@Parameter( label= "reducerMethod", required = true, direction = Parameter.Direction.IN, 
			 mode=ExpertMode.STANDARD, dataIOOrder=2,
	                        description = "Reduction method along the specified axes")
	private ReducerMethod reducerMethod = null;

	
	@Parameter( label= "inImg", required = true, direction = Parameter.Direction.IN, 
			 mode=ExpertMode.STANDARD, dataIOOrder=1,
	                        description = "Input image")
	private MTBImage inImg = null;

	@Parameter( label= "resultImg", required = true, direction = Parameter.Direction.OUT, 
			 mode=ExpertMode.STANDARD, dataIOOrder=1,
	                        description = "Result image")
	                        
	private MTBImage resultImg = null;

	/**
	 * Methods for dimension reduction. A dimension can be reduced (eliminated from the image) by the following methods:
	 * MEAN:            compute the mean value along the specified dimension(s)
	 * MIN:             find the minimum value along the specified dimension(s)
	 * MAX:             find the maximum value along the specified dimension(s)
	 * PROJECTION:      compute the projection along the specified dimension(s)
	 * NORM_PROJECTION: compute the normalized projection along the specified dimension(s) (resulting image values sum up to 1 -> discrete pdf)
	 * 
	 * @author gress
	 *
	 */
	public enum ReducerMethod {
		MEAN, MIN, MAX, PROJECTION, NORM_PROJECTION 
	}
	
	
	/**
	 * Constructor 
	 */
	public ImageDimensionReducer() throws ALDOperatorException {
		
	}
	
	/**
	 * Constructor
	 * @param img input image
	 * @param reduceX flag for x-dimension reduction
	 * @param reduceY flag for y-dimension reduction
	 * @param reduceZ flag for z-dimension reduction
	 * @param reduceT flag for t-dimension reduction
	 * @param reduceC flag for c-dimension reduction
	 * @param method reduction method
	 */
	public ImageDimensionReducer(MTBImage img,
									boolean reduceX,
									boolean reduceY,
									boolean reduceZ,
									boolean reduceT,
									boolean reduceC,
									ReducerMethod method) throws ALDOperatorException {
		
		this.setInImg(img);
		this.setReduceX(reduceX);
		this.setReduceY(reduceY);
		this.setReduceZ(reduceZ);
		this.setReduceT(reduceT);
		this.setReduceC(reduceC);
		this.setReducerMethod(method);
	}
	

	@Override
	protected void operate() throws ALDOperatorException {
		
		
		MTBImage resultImg = this.reduce();
		
		if (resultImg != null) {
			
			this.setResultImg(resultImg);
		}
		else {
			throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, "ImageDimensionReducer.operate() failed: Result image is 'null'");
		}
	}
	

	/**
	 * Method which implements the reduction
	 * @return
	 * @throws ALDOperatorException
	 */
	private MTBImage reduce() throws ALDOperatorException {
		MTBImage img = this.getInImg();
		
		int isizeX = img.getSizeX();
		int isizeY = img.getSizeY();
		int isizeZ = img.getSizeZ();
		int isizeT = img.getSizeT();
		int isizeC = img.getSizeC();

		IntObject c = new IntObject(), t = new IntObject(), z = new IntObject(), y = new IntObject(), x = new IntObject();
		
		int sizeX, sizeY, sizeZ, sizeT, sizeC;
		IntObject xx, yy, zz, tt, cc;
		
		int reducedDimSize = 1;
		
		if (this.getReduceX()) {
			reducedDimSize *= isizeX;
			sizeX = 1;
			xx = new IntObject();
		}
		else {
			sizeX = isizeX;
			xx = x;
		}
		
		if (this.getReduceY()) {
			reducedDimSize *= isizeY;
			sizeY = 1;
			yy = new IntObject();
		}
		else {
			sizeY = isizeY;
			yy = y;
		}
		
		if (this.getReduceZ()) {
			reducedDimSize *= isizeZ;
			sizeZ = 1;
			zz = new IntObject();
		}
		else {
			sizeZ = isizeZ;
			zz = z;
		}
		
		if (this.getReduceT()) {
			reducedDimSize *= isizeT;
			sizeT = 1;
			tt = new IntObject();
		}
		else {
			sizeT = isizeT;
			tt = t;
		}
		
		if (this.getReduceC()) {
			reducedDimSize *= isizeC;
			sizeC = 1;
			cc = new IntObject();
		}
		else {
			sizeC = isizeC;
			cc = c;
		}
		
		MTBImage result = null;
		
		if (this.getReducerMethod() == ReducerMethod.MIN
				|| this.getReducerMethod() == ReducerMethod.MAX) {
			
			result = MTBImage.createMTBImage(sizeX, sizeY, sizeZ, sizeT, sizeC, img.getType());
			
		}
		else {	
			result = MTBImage.createMTBImage(sizeX, sizeY, sizeZ, sizeT, sizeC, MTBImageType.MTB_DOUBLE);
			result.fillBlack();
		}
		
		if (this.getReducerMethod() == ReducerMethod.MIN) {
			
			for (t.i = 0; t.i < isizeT; t.i++) {
				for (z.i = 0; z.i < isizeZ; z.i++) {
					for (c.i = 0; c.i < isizeC; c.i++) {
						for (y.i = 0; y.i < isizeY; y.i++) {
							for (x.i = 0; x.i < isizeX; x.i++) {
								
								if (x.i == xx.i && y.i == yy.i && z.i == zz.i && t.i == tt.i && c.i == cc.i) {
									result.putValueDouble(xx.i, yy.i, zz.i, tt.i, cc.i, img.getValueDouble(x.i, y.i, z.i, t.i, c.i));
								}
								else {
									if (result.getValueDouble(xx.i, yy.i, zz.i, tt.i, cc.i) > img.getValueDouble(x.i, y.i, z.i, t.i, c.i)) {
										result.putValueDouble(xx.i, yy.i, zz.i, tt.i, cc.i, img.getValueDouble(x.i, y.i, z.i, t.i, c.i));
									}
								}
							}
						}
					}
				}
			}
			
		}
		else if (this.getReducerMethod() == ReducerMethod.MAX) {
			
			for (t.i = 0; t.i < isizeT; t.i++) {
				for (z.i = 0; z.i < isizeZ; z.i++) {
					for (c.i = 0; c.i < isizeC; c.i++) {
						for (y.i = 0; y.i < isizeY; y.i++) {
							for (x.i = 0; x.i < isizeX; x.i++) {
								
								if (x.i == xx.i && y.i == yy.i && z.i == zz.i && t.i == tt.i && c.i == cc.i) {
									result.putValueDouble(xx.i, yy.i, zz.i, tt.i, cc.i, img.getValueDouble(x.i, y.i, z.i, t.i, c.i));
								}
								else {
									if (result.getValueDouble(xx.i, yy.i, zz.i, tt.i, cc.i) < img.getValueDouble(x.i, y.i, z.i, t.i, c.i)) {
										result.putValueDouble(xx.i, yy.i, zz.i, tt.i, cc.i, img.getValueDouble(x.i, y.i, z.i, t.i, c.i));
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
								
								result.putValueDouble(xx.i, yy.i, zz.i, tt.i, cc.i, result.getValueDouble(xx.i, yy.i, zz.i, tt.i, cc.i) + img.getValueDouble(x.i, y.i, z.i, t.i, c.i));
			
							}
						}
					}
				}
			}
	
			if (this.getReducerMethod() == ReducerMethod.MEAN) {
				// compute mean
				int ss = result.getSizeStack();
				for (int i = 0; i < ss; i++) {
					result.setCurrentSliceIndex(i);
					for (int iy = 0; iy < sizeY; iy++) {
						for (int ix = 0; ix < sizeX; ix++) {
							result.putValueDouble(ix, iy, result.getValueDouble(ix, iy)/(double)reducedDimSize);
						}
					}
				}
				result.setCurrentSliceIndex(0);
			}
			else if (this.getReducerMethod() == ReducerMethod.NORM_PROJECTION) {
				
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
							result.putValueDouble(ix, iy, result.getValueDouble(ix, iy)/n);
						}
					}
				}
				
				result.setCurrentSliceIndex(0);
			}
		}
		
		return result;
	}
	
	/** Get value of Parameter argument reduceZ.
	  * @return value of reduceZ
	  */
	public Boolean getReduceZ() {
		return this.reduceZ;
	}
	
	/** Set value of Parameter argument reduceZ.
	  * @param value New value for reduceZ
	  */
	public void setReduceZ( boolean value ) {
		 this.reduceZ = value;
	}

	/** Get value of Parameter argument reduceC.
	  * @return value of reduceC
	  */
	public Boolean getReduceC() {
		return this.reduceC;
	}
	
	/** Set value of Parameter argument reduceC.
	  * @param value New value for reduceC
	  */
	public void setReduceC( boolean value ) {
		 this.reduceC = value;
	}

	/** Get value of Parameter argument reduceY.
	  * @return value of reduceY
	  */
	public Boolean getReduceY() {
		return this.reduceY;
	}
	
	/** Set value of Parameter argument reduceY.
	  * @param value New value for reduceY
	  */
	public void setReduceY( boolean value ) {
		 this.reduceY = value;
	}

	/** Get value of Parameter argument reduceX.
	  * @return value of reduceX
	  */
	public Boolean getReduceX() {
		return this.reduceX;
	}
	
	/** Set value of Parameter argument reduceX.
	  * @param value New value for reduceX
	  */
	public void setReduceX( boolean value ) {
		 this.reduceX = value;
	}

	/** Get value of Parameter argument reduceT.
	  * @return value of reduceT
	  */
	public Boolean getReduceT() {
		return this.reduceT;
	}
	
	/** Set value of Parameter argument reduceT.
	  * @param value New value for reduceT
	  */
	protected void setReduceT( boolean value ) {
		 this.reduceT = value;
	}

	/** Get value of Parameter argument reducerMethod.
	  * @return value of reducerMethod
	  */
	public ReducerMethod getReducerMethod() {
		return this.reducerMethod;
	}
	
	/** Set value of Parameter argument reducerMethod.
	  * @param value New value for reducerMethod
	  */
	public void setReducerMethod( ReducerMethod value ) {
		 this.reducerMethod = value;
	}

	/** Get value of Input argument inImg.
	  * @return value of inImg
	  */
	public MTBImage getInImg() throws ALDOperatorException {
		return this.inImg;
	}
	
	/** Set value of Input argument inImg.
	  * @param value New value for inImg
	  */
	public void setInImg( MTBImage img ) {
		 this.inImg = img;
	}

	/** Get value of Output argument resultImg.
	  * @return value of resultImg
	  */
	public MTBImage getResultImg() {
		return this.resultImg;
	}
	
	/** Set value of Output argument resultImg.
	  * @param value New value for resultImg
	  */
	protected void setResultImg( MTBImage img ) {
		 this.resultImg = img;
	}

	
	private class IntObject {
		
		public int i;
		
		private IntObject() {
			i = 0;
		}
	}
	
	
}
