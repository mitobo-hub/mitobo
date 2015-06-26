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
 * $Rev: 4399 $
 * $Date: 2011-10-17 14:11:39 +0200 (Mo, 17 Okt 2011) $
 * $Author: moeller $
 * 
 */

package de.unihalle.informatik.MiToBo.math.images;

import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;

/**
 * Image arithmetics class for MTBImage objects. The arithmetic operations are sub-divided into operations
 * regarding two images (ADD, SUB, MULT, DIV, MIN, MAX, AND, OR) and operations regarding one image and one constant
 * (ADD_CONST, MULT_CONST, POW_CONST, INV).
 * 
 * Operations with one input image result in images of the same type!!
 * Operations with two input images result in an image of type of the higher input image precision!!
 * If two input images do not have the same size the resulting image is null.
 * Each operation is elementwise.
 * 
 * @author gress
 *
 */
@ALDMetaInfo(export=ExportPolicy.ALLOWED)
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,level=Level.STANDARD)
public class ImageArithmetics extends MTBOperator {
	
	@Parameter( label= "First input image", required = true, direction = Parameter.Direction.IN, 
            dataIOOrder=1, mode=ExpertMode.STANDARD, description = "Input image 1")
	private MTBImage inImg1 = null;
	
	@Parameter( label= "Second input image", required = false, direction = Parameter.Direction.IN, 
            dataIOOrder=3, mode=ExpertMode.STANDARD, description = "Input image 2 (if required by operation)")
	private MTBImage inImg2 = null;
	
	@Parameter( label= "Result image", required = true, direction = Parameter.Direction.OUT, 
            dataIOOrder=1, mode=ExpertMode.STANDARD, description = "Resulting image")
	private MTBImage resultImg = null;
	
	@Parameter( label= "Arithmetic operation", required = true, direction = Parameter.Direction.IN, 
            dataIOOrder=2, mode=ExpertMode.STANDARD, description = "Arithmetic operation on images")
	private ArithOp arithmeticOp = null;
	
	@Parameter( label= "Input constant", required = false, direction = Parameter.Direction.IN, 
            dataIOOrder=4, mode=ExpertMode.STANDARD, description = "Input constant (if required by operation)")
	private Double inConst = null;

	// ordinal of first "one image, one constant"-operation in ArithOp enum
	private final int firstOneImageOneConstOrdinal = 9;
	
	// ordinal of first "one image only"-operation in ArithOp enum
	private final int firstOneImageOnlyOrdinal = 12;
	
	/**
	 * Arithmetic operations: <br/>
	 * ADD, SUB, MULT, DIV, MIN, MAX, AND, OR, ABS_DIFF require two input images,  <br/>
	 * ADD_CONST, MULT_CONST, POW_CONST require one input image and a constant <br/>
	 * INV, ABS require one input image only
	 * @author Oliver Gress
	 *
	 */
	public enum ArithOp {
		ADD, SUB, MULT, DIV, MIN, MAX, AND, OR, ABS_DIFF, ADD_CONST, MULT_CONST, POW_CONST, 
		INV, ABS
	}
	
	/**
	 * Create empty ImageArithmetics object. Remember to set parameters manually.
	 */
	public ImageArithmetics() throws ALDOperatorException {
	}
	
	/**
	 * Constructor for arithmetic operations considering two images.
	 * @param op arithmetic operation
	 * @param inputImage1 first input image
	 * @param inputImage2 second input image
	 */
	protected ImageArithmetics(ArithOp op, MTBImage inputImage1, MTBImage inputImage2) throws ALDOperatorException {
		this.setArithmeticOperation(op);
		this.setInImg1(inputImage1);
		this.setInImg2(inputImage2);
	}
	
	/**
	 * Constructor for arithmetic operations considering one image and a constant.
	 * @param op arithmetic operation
	 * @param inputImage input image
	 * @param inputConstant input constant
	 */
	protected ImageArithmetics(ArithOp op, MTBImage inputImage, double inputConstant) throws ALDOperatorException {
		this.setArithmeticOperation(op);
		this.setInImg1(inputImage);
		this.setInConst(inputConstant);
	}
	
	@Override
	public void validateCustom() throws ALDOperatorException {
		
		boolean valid = false;
		

			if (this.getArithmeticOperation().ordinal() < this.firstOneImageOneConstOrdinal) {
				// operations with two images
				valid = (this.getInImg1() != null && this.getInImg2() != null && this.getInImg1().equalSize(this.getInImg2()));
				
				if (!valid)
					throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED, "Operation needs two images of equal size.");
			}
			else if (this.getArithmeticOperation().ordinal() < this.firstOneImageOnlyOrdinal){
				// operations with one image and one constant
				valid = (this.getInImg1() != null && this.getInConst() != null);
				
				if (!valid)
					throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED, "Operation needs an image and a constant.");
			}
	}
	
	

	@Override
	protected void operate() throws ALDOperatorException {
		MTBImage resultImg = null;
		
		ArithOp op = this.getArithmeticOperation();
		
		
		if (op.ordinal() < firstOneImageOneConstOrdinal) {
			// Operations with two images	
			
			MTBImage inImg1 = this.getInImg1();
			MTBImage inImg2 = this.getInImg2();
		
			if (op == ArithOp.ADD)
				resultImg = this.add(inImg1, inImg2);
			else if (op == ArithOp.SUB)
				resultImg = this.sub(inImg1, inImg2);				
			else if (op == ArithOp.MULT)
				resultImg = this.mult(inImg1, inImg2);
			else if (op == ArithOp.DIV)
				resultImg = this.div(inImg1, inImg2);
			else if (op == ArithOp.MIN)
				resultImg = this.min(inImg1, inImg2);
			else if (op == ArithOp.MAX)
				resultImg = this.max(inImg1, inImg2);
			else if (op == ArithOp.AND)
				resultImg = this.and(inImg1, inImg2);
			else if (op == ArithOp.OR)
				resultImg = this.or(inImg1, inImg2);
			else if (op == ArithOp.ABS_DIFF)
				resultImg = this.abs_diff(inImg1, inImg2);
				
			
			if (resultImg != null) {
				this.setResultImg(resultImg);
			}		
		}
		else {
		// Operations with one image and one constant	
			
			MTBImage inImg = this.getInImg1();
			double inConst = this.getInConst();
		
			if (op == ArithOp.ADD_CONST)
				resultImg = this.add(inImg, inConst);
			else if (op == ArithOp.MULT_CONST)
				resultImg = this.mult(inImg, inConst);
			else if (op == ArithOp.POW_CONST)
				resultImg = this.pow(inImg, inConst);
			else if (op == ArithOp.INV)
				resultImg = this.inv(inImg);
			else if (op == ArithOp.ABS)
				resultImg = this.abs(inImg);
			
			if (resultImg != null) {
				this.setResultImg(resultImg);
			}
		}			
				
		if (resultImg == null) 
			throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, "MTBImageArithmetics.ImageArithmetics.operate() failed: Result image is 'null'");
	}
	
	
	protected MTBImage getInImg1() {
		return this.inImg1;
	}
	
	protected void setInImg1(MTBImage inImg1) {
		this.inImg1 = inImg1;
	}
		
	protected MTBImage getInImg2() {
		return this.inImg2;
	}
	
	protected void setInImg2(MTBImage inImg2) {
		this.inImg2 = inImg2;
	}		
	
	protected Double getInConst() {
		return this.inConst;
	}
	
	protected void setInConst(double inConst) {
		this.inConst = inConst;
	}			
	
	protected ArithOp getArithmeticOperation() {
		return this.arithmeticOp;
	}
	
	protected void setArithmeticOperation(ArithOp op) {
		this.arithmeticOp = op;
	}	
	
	protected MTBImage getResultImg() {
		return this.resultImg;
	}
	
	protected void setResultImg(MTBImage img) {
		this.resultImg = img;
	}		
	
	/**
	 * Add a constant to each image value
	 * @param img input image
	 * @param constant
	 * @return
	 */
	protected MTBImage add(MTBImage img, double constant) {
		int sizeStack = img.getSizeStack();
		int sizeY = img.getSizeY();
		int sizeX = img.getSizeX();
		
		int idx = img.getCurrentSliceIndex();
		
		MTBImage newImg = MTBImage.createMTBImage(sizeX, sizeY, img.getSizeZ(), img.getSizeT(), img.getSizeC(), img.getType());

		newImg.setTitle(img.getTitle());
		newImg.setStepsizeX(img.getStepsizeX());
		newImg.setStepsizeY(img.getStepsizeY());
		newImg.setStepsizeZ(img.getStepsizeZ());
		newImg.setStepsizeT(img.getStepsizeT());
		newImg.setUnitX(img.getUnitX());
		newImg.setUnitY(img.getUnitY());
		newImg.setUnitZ(img.getUnitZ());
		newImg.setUnitT(img.getUnitT());
		
		for (int i = 0; i < sizeStack; i++) {
			img.setCurrentSliceIndex(i);
			newImg.setCurrentSliceIndex(i);
			
			for (int y = 0; y < sizeY; y++) {
				for (int x = 0; x < sizeX; x++) {
					newImg.putValueDouble(x, y, img.getValueDouble(x,y) + constant);
				}
			}
		}
		
		// restore actual slice index
		img.setCurrentSliceIndex(idx);
		newImg.setCurrentSliceIndex(0);
		
		return newImg;
	}
	
	/**
	 * Multiply each image value by a constant
	 * @param img input image
	 * @param constant
	 * @return
	 */
	protected MTBImage mult(MTBImage img, double constant) {
		int sizeStack = img.getSizeStack();
		int sizeY = img.getSizeY();
		int sizeX = img.getSizeX();
		
		int idx = img.getCurrentSliceIndex();
		
		MTBImage newImg = MTBImage.createMTBImage(sizeX, sizeY, img.getSizeZ(), img.getSizeT(), img.getSizeC(), img.getType());

		newImg.setTitle(img.getTitle());
		newImg.setStepsizeX(img.getStepsizeX());
		newImg.setStepsizeY(img.getStepsizeY());
		newImg.setStepsizeZ(img.getStepsizeZ());
		newImg.setStepsizeT(img.getStepsizeT());
		newImg.setUnitX(img.getUnitX());
		newImg.setUnitY(img.getUnitY());
		newImg.setUnitZ(img.getUnitZ());
		newImg.setUnitT(img.getUnitT());
		
		for (int i = 0; i < sizeStack; i++) {
			img.setCurrentSliceIndex(i);
			newImg.setCurrentSliceIndex(i);
			
			for (int y = 0; y < sizeY; y++) {
				for (int x = 0; x < sizeX; x++) {
					newImg.putValueDouble(x, y, img.getValueDouble(x,y) * constant);
				}
			}
		}
		
		// restore actual slice index
		img.setCurrentSliceIndex(idx);
		newImg.setCurrentSliceIndex(0);
		
		return newImg;
	}	
	
	/**
	 * Raise image values to the power of 'constant'
	 * @param img
	 * @param constant
	 * @return
	 */
	protected MTBImage pow(MTBImage img, double constant) {
		int sizeStack = img.getSizeStack();
		int sizeY = img.getSizeY();
		int sizeX = img.getSizeX();
		
		int idx = img.getCurrentSliceIndex();
		
		MTBImage newImg = MTBImage.createMTBImage(sizeX, sizeY, img.getSizeZ(), img.getSizeT(), img.getSizeC(), img.getType());

		newImg.setTitle(img.getTitle());
		newImg.setStepsizeX(img.getStepsizeX());
		newImg.setStepsizeY(img.getStepsizeY());
		newImg.setStepsizeZ(img.getStepsizeZ());
		newImg.setStepsizeT(img.getStepsizeT());
		newImg.setUnitX(img.getUnitX());
		newImg.setUnitY(img.getUnitY());
		newImg.setUnitZ(img.getUnitZ());
		newImg.setUnitT(img.getUnitT());
		
		for (int i = 0; i < sizeStack; i++) {
			img.setCurrentSliceIndex(i);
			newImg.setCurrentSliceIndex(i);
			
			for (int y = 0; y < sizeY; y++) {
				for (int x = 0; x < sizeX; x++) {
					newImg.putValueDouble(x, y, Math.pow(img.getValueDouble(x,y), constant));
				}
			}
		}
		
		// restore actual slice index
		img.setCurrentSliceIndex(idx);
		newImg.setCurrentSliceIndex(0);
		
		return newImg;
	}		
	
	/**
	 * Inverts the image.
	 * @param img	Input image.
	 * @return	Result image.
	 */
	protected MTBImage inv(MTBImage img) {
		int sizeStack = img.getSizeStack();
		int sizeY = img.getSizeY();
		int sizeX = img.getSizeX();
		
		int idx = img.getCurrentSliceIndex();
		
		MTBImage newImg = MTBImage.createMTBImage(sizeX, sizeY, img.getSizeZ(), img.getSizeT(), img.getSizeC(), img.getType());

		newImg.setTitle(img.getTitle());
		newImg.setStepsizeX(img.getStepsizeX());
		newImg.setStepsizeY(img.getStepsizeY());
		newImg.setStepsizeZ(img.getStepsizeZ());
		newImg.setStepsizeT(img.getStepsizeT());
		newImg.setUnitX(img.getUnitX());
		newImg.setUnitY(img.getUnitY());
		newImg.setUnitZ(img.getUnitZ());
		newImg.setUnitT(img.getUnitT());
		
		for (int i = 0; i < sizeStack; i++) {
			img.setCurrentSliceIndex(i);
			newImg.setCurrentSliceIndex(i);
			
			for (int y = 0; y < sizeY; y++) {
				for (int x = 0; x < sizeX; x++) {
					newImg.putValueDouble(x, y, 
							newImg.getTypeMax() - img.getValueDouble(x, y));
				}
			}
		}
		
		// restore actual slice index
		img.setCurrentSliceIndex(idx);
		newImg.setCurrentSliceIndex(0);
		
		return newImg;
	}	
	
	/**
	 * Absolute values of the image elements.
	 * @param img	Input image.
	 * @return	Result image.
	 */
	protected MTBImage abs(MTBImage img) {
		int sizeStack = img.getSizeStack();
		int sizeY = img.getSizeY();
		int sizeX = img.getSizeX();
		
		int idx = img.getCurrentSliceIndex();
		
		MTBImage newImg = MTBImage.createMTBImage(sizeX, sizeY, img.getSizeZ(), img.getSizeT(), img.getSizeC(), img.getType());

		newImg.setTitle(MTBImage.getTitleRunning(img.getTitle()));
		newImg.setStepsizeX(img.getStepsizeX());
		newImg.setStepsizeY(img.getStepsizeY());
		newImg.setStepsizeZ(img.getStepsizeZ());
		newImg.setStepsizeT(img.getStepsizeT());
		newImg.setUnitX(img.getUnitX());
		newImg.setUnitY(img.getUnitY());
		newImg.setUnitZ(img.getUnitZ());
		newImg.setUnitT(img.getUnitT());
		
		for (int i = 0; i < sizeStack; i++) {
			img.setCurrentSliceIndex(i);
			newImg.setCurrentSliceIndex(i);
			
			for (int y = 0; y < sizeY; y++) {
				for (int x = 0; x < sizeX; x++) {
					newImg.putValueDouble(x, y, 
							Math.abs(img.getValueDouble(x, y)));
				}
			}
		}
		
		// restore actual slice index
		img.setCurrentSliceIndex(idx);
		newImg.setCurrentSliceIndex(0);
		
		return newImg;
	}

	/**
	 * Add the elements of two images
	 */
	protected MTBImage add(MTBImage img1, MTBImage img2) {
		int sizeStack = img1.getSizeStack();
		int sizeY = img1.getSizeY();
		int sizeX = img1.getSizeX();
		
		int idx1 = img1.getCurrentSliceIndex();
		int idx2 = img2.getCurrentSliceIndex();
		
		MTBImage newImg = MTBImage.createMTBImage(sizeX, sizeY, img1.getSizeZ(), img1.getSizeT(), img1.getSizeC(), 
				(img1.getType().ordinal() >= img2.getType().ordinal() ? img1.getType() : img2.getType()));

		newImg.setTitle(img1.getTitle());
		newImg.setStepsizeX(img1.getStepsizeX());
		newImg.setStepsizeY(img1.getStepsizeY());
		newImg.setStepsizeZ(img1.getStepsizeZ());
		newImg.setStepsizeT(img1.getStepsizeT());
		newImg.setUnitX(img1.getUnitX());
		newImg.setUnitY(img1.getUnitY());
		newImg.setUnitZ(img1.getUnitZ());
		newImg.setUnitT(img1.getUnitT());
		
		for (int i = 0; i < sizeStack; i++) {
			img1.setCurrentSliceIndex(i);
			img2.setCurrentSliceIndex(i);
			newImg.setCurrentSliceIndex(i);
			
			for (int y = 0; y < sizeY; y++) {
				for (int x = 0; x < sizeX; x++) {
					newImg.putValueDouble(x, y, img1.getValueDouble(x,y) + img2.getValueDouble(x,y));
				}
			}
		}
		
		// restore actual slice index
		img1.setCurrentSliceIndex(idx1);
		img2.setCurrentSliceIndex(idx2);
		newImg.setCurrentSliceIndex(0);
		
		return newImg;
	}
	
	/**
	 * Subtract the elements of two images (img1 - img2)
	 */
	protected MTBImage sub(MTBImage img1, MTBImage img2) {
		int sizeStack = img1.getSizeStack();
		int sizeY = img1.getSizeY();
		int sizeX = img1.getSizeX();
		
		int idx1 = img1.getCurrentSliceIndex();
		int idx2 = img2.getCurrentSliceIndex();
		
		MTBImage newImg = MTBImage.createMTBImage(sizeX, sizeY, img1.getSizeZ(), img1.getSizeT(), img1.getSizeC(), 
									(img1.getType().ordinal() >= img2.getType().ordinal() ? img1.getType() : img2.getType()));
		
		newImg.setTitle(img1.getTitle());
		newImg.setStepsizeX(img1.getStepsizeX());
		newImg.setStepsizeY(img1.getStepsizeY());
		newImg.setStepsizeZ(img1.getStepsizeZ());
		newImg.setStepsizeT(img1.getStepsizeT());
		newImg.setUnitX(img1.getUnitX());
		newImg.setUnitY(img1.getUnitY());
		newImg.setUnitZ(img1.getUnitZ());
		newImg.setUnitT(img1.getUnitT());

		
		for (int i = 0; i < sizeStack; i++) {
			img1.setCurrentSliceIndex(i);
			img2.setCurrentSliceIndex(i);
			newImg.setCurrentSliceIndex(i);
			
			for (int y = 0; y < sizeY; y++) {
				for (int x = 0; x < sizeX; x++) {
					newImg.putValueDouble(x, y, img1.getValueDouble(x,y) - img2.getValueDouble(x,y));
				}
			}
		}
		
		// restore actual slice index
		img1.setCurrentSliceIndex(idx1);
		img2.setCurrentSliceIndex(idx2);
		newImg.setCurrentSliceIndex(0);
		
		return newImg;
	}
	
	/**
	 * Multiply the elements of two images
	 */
	protected MTBImage mult(MTBImage img1, MTBImage img2) {
		int sizeStack = img1.getSizeStack();
		int sizeY = img1.getSizeY();
		int sizeX = img1.getSizeX();
		
		int idx1 = img1.getCurrentSliceIndex();
		int idx2 = img2.getCurrentSliceIndex();
		
		MTBImage newImg = MTBImage.createMTBImage(sizeX, sizeY, img1.getSizeZ(), img1.getSizeT(), img1.getSizeC(), 
				(img1.getType().ordinal() >= img2.getType().ordinal() ? img1.getType() : img2.getType()));

		newImg.setTitle(img1.getTitle());
		newImg.setStepsizeX(img1.getStepsizeX());
		newImg.setStepsizeY(img1.getStepsizeY());
		newImg.setStepsizeZ(img1.getStepsizeZ());
		newImg.setStepsizeT(img1.getStepsizeT());
		newImg.setUnitX(img1.getUnitX());
		newImg.setUnitY(img1.getUnitY());
		newImg.setUnitZ(img1.getUnitZ());
		newImg.setUnitT(img1.getUnitT());
		
		for (int i = 0; i < sizeStack; i++) {
			img1.setCurrentSliceIndex(i);
			img2.setCurrentSliceIndex(i);
			newImg.setCurrentSliceIndex(i);
			
			for (int y = 0; y < sizeY; y++) {
				for (int x = 0; x < sizeX; x++) {
					newImg.putValueDouble(x, y, img1.getValueDouble(x,y) * img2.getValueDouble(x,y));
				}
			}
		}
		
		// restore actual slice index
		img1.setCurrentSliceIndex(idx1);
		img2.setCurrentSliceIndex(idx2);
		newImg.setCurrentSliceIndex(0);
		
		return newImg;
	}
	
	/**
	 * Divide the elements of two images (img1/img2)
	 */
	protected MTBImage div(MTBImage img1, MTBImage img2) {
		int sizeStack = img1.getSizeStack();
		int sizeY = img1.getSizeY();
		int sizeX = img1.getSizeX();
		
		int idx1 = img1.getCurrentSliceIndex();
		int idx2 = img2.getCurrentSliceIndex();
		
		MTBImage newImg = MTBImage.createMTBImage(sizeX, sizeY, img1.getSizeZ(), img1.getSizeT(), img1.getSizeC(), 
				(img1.getType().ordinal() >= img2.getType().ordinal() ? img1.getType() : img2.getType()));

		newImg.setTitle(img1.getTitle());
		newImg.setStepsizeX(img1.getStepsizeX());
		newImg.setStepsizeY(img1.getStepsizeY());
		newImg.setStepsizeZ(img1.getStepsizeZ());
		newImg.setStepsizeT(img1.getStepsizeT());
		newImg.setUnitX(img1.getUnitX());
		newImg.setUnitY(img1.getUnitY());
		newImg.setUnitZ(img1.getUnitZ());
		newImg.setUnitT(img1.getUnitT());
		
		for (int i = 0; i < sizeStack; i++) {
			img1.setCurrentSliceIndex(i);
			img2.setCurrentSliceIndex(i);
			newImg.setCurrentSliceIndex(i);
			
			for (int y = 0; y < sizeY; y++) {
				for (int x = 0; x < sizeX; x++) {
					newImg.putValueDouble(x, y, img1.getValueDouble(x,y) / img2.getValueDouble(x,y));
				}
			}
		}
		
		// restore actual slice index
		img1.setCurrentSliceIndex(idx1);
		img2.setCurrentSliceIndex(idx2);
		newImg.setCurrentSliceIndex(0);
		
		return newImg;
	}
	
	/**
	 * Pixelwise minimum
	 */
	protected MTBImage min(MTBImage img1, MTBImage img2) {
		int sizeStack = img1.getSizeStack();
		int sizeY = img1.getSizeY();
		int sizeX = img1.getSizeX();
		
		int idx1 = img1.getCurrentSliceIndex();
		int idx2 = img2.getCurrentSliceIndex();
		
		double v1, v2;
		
		
		MTBImage newImg = MTBImage.createMTBImage(sizeX, sizeY, img1.getSizeZ(), img1.getSizeT(), img1.getSizeC(), 
				(img1.getType().ordinal() >= img2.getType().ordinal() ? img1.getType() : img2.getType()));

		newImg.setTitle(img1.getTitle());
		newImg.setStepsizeX(img1.getStepsizeX());
		newImg.setStepsizeY(img1.getStepsizeY());
		newImg.setStepsizeZ(img1.getStepsizeZ());
		newImg.setStepsizeT(img1.getStepsizeT());
		newImg.setUnitX(img1.getUnitX());
		newImg.setUnitY(img1.getUnitY());
		newImg.setUnitZ(img1.getUnitZ());
		newImg.setUnitT(img1.getUnitT());
		
		for (int i = 0; i < sizeStack; i++) {
			img1.setCurrentSliceIndex(i);
			img2.setCurrentSliceIndex(i);
			newImg.setCurrentSliceIndex(i);
			
			for (int y = 0; y < sizeY; y++) {
				for (int x = 0; x < sizeX; x++) {
					v1 = img1.getValueDouble(x,y);
					v2 = img2.getValueDouble(x,y);
					newImg.putValueDouble(x, y, v1 <= v2 ? v1 : v2);
				}
			}
		}
		
		// restore actual slice index
		img1.setCurrentSliceIndex(idx1);
		img2.setCurrentSliceIndex(idx2);
		newImg.setCurrentSliceIndex(0);
		
		return newImg;
	}
	
	/**
	 * Pixelwise maximum
	 */
	protected MTBImage max(MTBImage img1, MTBImage img2) {
		int sizeStack = img1.getSizeStack();
		int sizeY = img1.getSizeY();
		int sizeX = img1.getSizeX();
		
		int idx1 = img1.getCurrentSliceIndex();
		int idx2 = img2.getCurrentSliceIndex();
		
		double v1, v2;
		
		
		MTBImage newImg = MTBImage.createMTBImage(sizeX, sizeY, img1.getSizeZ(), img1.getSizeT(), img1.getSizeC(), 
				(img1.getType().ordinal() >= img2.getType().ordinal() ? img1.getType() : img2.getType()));

		newImg.setTitle(img1.getTitle());
		newImg.setStepsizeX(img1.getStepsizeX());
		newImg.setStepsizeY(img1.getStepsizeY());
		newImg.setStepsizeZ(img1.getStepsizeZ());
		newImg.setStepsizeT(img1.getStepsizeT());
		newImg.setUnitX(img1.getUnitX());
		newImg.setUnitY(img1.getUnitY());
		newImg.setUnitZ(img1.getUnitZ());
		newImg.setUnitT(img1.getUnitT());
		
		for (int i = 0; i < sizeStack; i++) {
			img1.setCurrentSliceIndex(i);
			img2.setCurrentSliceIndex(i);
			newImg.setCurrentSliceIndex(i);
			
			for (int y = 0; y < sizeY; y++) {
				for (int x = 0; x < sizeX; x++) {
					v1 = img1.getValueDouble(x,y);
					v2 = img2.getValueDouble(x,y);
					newImg.putValueDouble(x, y, v1 >= v2 ? v1 : v2);
				}
			}
		}
		
		// restore actual slice index
		img1.setCurrentSliceIndex(idx1);
		img2.setCurrentSliceIndex(idx2);
		newImg.setCurrentSliceIndex(0);
		
		return newImg;
	}

	/**
	 * Bitwise logical AND of two images.
	 * @param img1	First image.
	 * @param img2	Second image.
	 * @return Result image.
	 */
	protected MTBImage and(MTBImage img1, MTBImage img2) {
		int sizeStack = img1.getSizeStack();
		int sizeY = img1.getSizeY();
		int sizeX = img1.getSizeX();
		
		int idx1 = img1.getCurrentSliceIndex();
		int idx2 = img2.getCurrentSliceIndex();
		
		MTBImage newImg = MTBImage.createMTBImage(sizeX, sizeY, img1.getSizeZ(), 
				img1.getSizeT(), img1.getSizeC(), 
				(img1.getType().ordinal() >= img2.getType().ordinal() ? img1.getType() : img2.getType()));

		newImg.setTitle(img1.getTitle());
		newImg.setStepsizeX(img1.getStepsizeX());
		newImg.setStepsizeY(img1.getStepsizeY());
		newImg.setStepsizeZ(img1.getStepsizeZ());
		newImg.setStepsizeT(img1.getStepsizeT());
		newImg.setUnitX(img1.getUnitX());
		newImg.setUnitY(img1.getUnitY());
		newImg.setUnitZ(img1.getUnitZ());
		newImg.setUnitT(img1.getUnitT());
		
		for (int i = 0; i < sizeStack; i++) {
			img1.setCurrentSliceIndex(i);
			img2.setCurrentSliceIndex(i);
			newImg.setCurrentSliceIndex(i);
			
			for (int y = 0; y < sizeY; y++) {
				for (int x = 0; x < sizeX; x++) {
					newImg.putValueDouble(x, y, 
							(img1.getValueDouble(x,y) > 0 && img2.getValueDouble(x,y) > 0)?
									newImg.getTypeMax() : newImg.getTypeMin());
				}
			}
		}
		
		// restore actual slice index
		img1.setCurrentSliceIndex(idx1);
		img2.setCurrentSliceIndex(idx2);
		newImg.setCurrentSliceIndex(0);
		
		return newImg;
	}
	

	/**
	 * Bitwise OR of the elements of two images
	 * @param img1	First image.
	 * @param img2	Second image.
	 * @return	Result image.
	 */
	protected MTBImage or(MTBImage img1, MTBImage img2) {
		int sizeStack = img1.getSizeStack();
		int sizeY = img1.getSizeY();
		int sizeX = img1.getSizeX();
		
		int idx1 = img1.getCurrentSliceIndex();
		int idx2 = img2.getCurrentSliceIndex();
		
		MTBImage newImg = MTBImage.createMTBImage(sizeX, sizeY, img1.getSizeZ(), img1.getSizeT(), img1.getSizeC(), 
				(img1.getType().ordinal() >= img2.getType().ordinal() ? img1.getType() : img2.getType()));

		newImg.setTitle(img1.getTitle());
		newImg.setStepsizeX(img1.getStepsizeX());
		newImg.setStepsizeY(img1.getStepsizeY());
		newImg.setStepsizeZ(img1.getStepsizeZ());
		newImg.setStepsizeT(img1.getStepsizeT());
		newImg.setUnitX(img1.getUnitX());
		newImg.setUnitY(img1.getUnitY());
		newImg.setUnitZ(img1.getUnitZ());
		newImg.setUnitT(img1.getUnitT());
		
		for (int i = 0; i < sizeStack; i++) {
			img1.setCurrentSliceIndex(i);
			img2.setCurrentSliceIndex(i);
			newImg.setCurrentSliceIndex(i);
			
			for (int y = 0; y < sizeY; y++) {
				for (int x = 0; x < sizeX; x++) {
					newImg.putValueDouble(x, y, 
							(img1.getValueDouble(x,y) > 0 || img2.getValueDouble(x,y) > 0)?
									newImg.getTypeMax() : newImg.getTypeMin());
				}
			}
		}
		
		// restore actual slice index
		img1.setCurrentSliceIndex(idx1);
		img2.setCurrentSliceIndex(idx2);
		newImg.setCurrentSliceIndex(0);
		
		return newImg;
	}
	
	/**
	 * Absolute difference of the elements of two images
	 */
	protected MTBImage abs_diff(MTBImage img1, MTBImage img2) {
		int sizeStack = img1.getSizeStack();
		int sizeY = img1.getSizeY();
		int sizeX = img1.getSizeX();
		
		int idx1 = img1.getCurrentSliceIndex();
		int idx2 = img2.getCurrentSliceIndex();
		
		MTBImage newImg = MTBImage.createMTBImage(sizeX, sizeY, img1.getSizeZ(), img1.getSizeT(), img1.getSizeC(), 
				(img1.getType().ordinal() >= img2.getType().ordinal() ? img1.getType() : img2.getType()));

		newImg.setTitle(MTBImage.getTitleRunning(img1.getTitle()));
		newImg.setStepsizeX(img1.getStepsizeX());
		newImg.setStepsizeY(img1.getStepsizeY());
		newImg.setStepsizeZ(img1.getStepsizeZ());
		newImg.setStepsizeT(img1.getStepsizeT());
		newImg.setUnitX(img1.getUnitX());
		newImg.setUnitY(img1.getUnitY());
		newImg.setUnitZ(img1.getUnitZ());
		newImg.setUnitT(img1.getUnitT());
		
		for (int i = 0; i < sizeStack; i++) {
			img1.setCurrentSliceIndex(i);
			img2.setCurrentSliceIndex(i);
			newImg.setCurrentSliceIndex(i);
			
			for (int y = 0; y < sizeY; y++) {
				for (int x = 0; x < sizeX; x++) {
					newImg.putValueDouble(x, y, Math.abs(img1.getValueDouble(x,y) - img2.getValueDouble(x,y)));
				}
			}
		}
		
		// restore actual slice index
		img1.setCurrentSliceIndex(idx1);
		img2.setCurrentSliceIndex(idx2);
		newImg.setCurrentSliceIndex(0);
		
		return newImg;
	}

}
