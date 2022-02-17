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



import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Set;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.MiToBo.color.conversion.HSVToRGBArrayConverter;
import de.unihalle.informatik.MiToBo.color.conversion.HSVToRGBPixelConverter;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBVectorField2D;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.*;
import de.unihalle.informatik.MiToBo.fields.FieldOperations2D;
import de.unihalle.informatik.MiToBo.fields.FieldOperations2D.FieldOperation;
import de.unihalle.informatik.MiToBo.fields.GradientFieldCalculator2D;
import de.unihalle.informatik.MiToBo.fields.GradientFieldCalculator2D.GradientMode;

/**
 * This class implements morphological dilation on 2D binary/grayscale images.
 * <p>
 * If the given image only contains two pixel values it is interpreted as 
 * binary image. In the resulting image the background pixels will be set 
 * to the smaller value, while the foreground pixels will be set to the 
 * larger ones.
 * <p> 
 * The structuring element is a square matrix of size 'masksize' x 'masksize', 
 * with reference pixel in the center of the matrix.
 *
 * Attention: if masksize is even, errors may result due 
 *            to lack of operator symmetry
 *
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,
		level=Level.APPLICATION)
public class FibrilToolComplete extends MTBOperator {

	@Parameter( label= "Input Image", required = true, dataIOOrder = 0,
			direction = Parameter.Direction.IN, description = "Input image")
	private transient MTBImageByte inImg = null;

	@Parameter( label= "Window Size", required = true, dataIOOrder = 0,
			direction = Parameter.Direction.IN, description = "Window Size")
	private int winSize = 15;

	@Parameter( label= "Anisotropic threshold", required = true, dataIOOrder = 0,
			direction = Parameter.Direction.IN, description = "Anisotropic threshold")
	private double anisoThresh = 0.01;

	@Parameter( label= "Result Image", required = true,
			direction = Parameter.Direction.OUT, description = "Result image")
	private transient MTBImageDouble resultImgScore = null;

	@Parameter( label= "Result Image", required = true,
			direction = Parameter.Direction.OUT, description = "Result image")
	private transient MTBImageRGB resultImgDirection = null;

//	@Parameter( label= "verbose", required = false)
//	@MTBArgumentAnnotation( type = MTBArgumentAnnotation.ALDArgumentType.SUPPLEMENTAL, 
//	                        explanation = "Verbose flag")
//	private Boolean verbose = false;

	/**
	 * Default constructor.
	 *  @throws ALDOperatorException
	 */
	public FibrilToolComplete() throws ALDOperatorException {
		// nothing to do here
	}

	/** Get value of inImg.
	  * Explanation: Input image.
	  * @return value of inImg
	  */
	public MTBImage getInputImage(){
		return this.inImg;
	}

	/**
	 * This method does the actual work. 
	 * @throws ALDOperatorException 
	 * @throws ALDProcessingDAGException 
	 */
	@Override
	protected void operate() throws ALDOperatorException, ALDProcessingDAGException {

		this.resultImgDirection = (MTBImageRGB)this.inImg.convertType(MTBImageType.MTB_RGB,true);
		this.resultImgScore = (MTBImageDouble)this.inImg.convertType(MTBImageType.MTB_DOUBLE,true);

		// calculate derivatives
		GradientFieldCalculator2D gfc = new GradientFieldCalculator2D(this.inImg,
				GradientMode.PARTIAL_DIFF);
		gfc.runOp();
		MTBVectorField2D diffs = gfc.getVectorField();
		MTBImageDouble diffsImg = gfc.getVectorFieldImage();

		FieldOperations2D fo = new FieldOperations2D(diffs, diffsImg,
				FieldOperation.MAGNITUDE_IMAGE);
		fo.runOp();
		MTBImage magImage = fo.getResultImage();

		MTBImageDouble nXX = (MTBImageDouble)this.resultImgScore.duplicate();
		MTBImageDouble nYY = (MTBImageDouble)this.resultImgScore.duplicate();
		MTBImageDouble nXY = (MTBImageDouble)this.resultImgScore.duplicate();
		for (int y=0;y<diffsImg.getSizeY(); ++y) {
			for (int x=0;x<diffsImg.getSizeX(); ++x) {
				double norm = magImage.getValueDouble(x,y);
				if (norm < 2)
					norm = 255;
				double vx = diffs.getValueU(x,y)/norm;
				double vy = diffs.getValueV(x,y)/norm;
				nXX.putValueDouble(x,y,vx*vx);
				nYY.putValueDouble(x,y,vy*vy);
				nXY.putValueDouble(x,y,vx*vy);
			}
		}
		
		double[] h = new double[diffsImg.getSizeY()*diffsImg.getSizeX()];
		double[] s = new double[diffsImg.getSizeY()*diffsImg.getSizeX()];
		double[] v = new double[diffsImg.getSizeY()*diffsImg.getSizeX()];
		
		int wh = (int)(this.winSize/2.0);
		double fac = this.winSize * this.winSize;
		
		double minAngle = 100000000;
		double maxAngle = -100000000;
		
		double sxx, syy, sxy;
		for (int y=wh;y<diffsImg.getSizeY()-wh; ++y) {
			for (int x=wh;x<diffsImg.getSizeX()-wh; ++x) {
				sxx = 0; syy = 0; sxy = 0;
				for (int dy=-wh;dy<=wh;++dy) {
					for (int dx=-wh;dx<=wh;++dx) {
						sxx += nXX.getValueDouble(x+dx,y+dy);
						syy += nYY.getValueDouble(x+dx,y+dy);
						sxy += nXY.getValueDouble(x+dx,y+dy);
					}
				}
				sxx = 1.0/fac * sxx;
				syy = 1.0/fac * syy;
				sxy = 1.0/fac * sxy;
				
				//eigenvalues and eigenvector of texture tensor
				double m = (sxx + syy) / 2.0;
				double d = (sxx - syy) / 2.0;
				double v1 = m + Math.sqrt(sxy*sxy + d*d);
				double v2 = m - Math.sqrt(sxy*sxy + d*d);
				//direction
				double tn = - Math.atan((v2 - sxx) / sxy);
				//score
				double scoren = Math.abs((v1-v2) / 2.0 / m);

				//output
				double tsn=tn*180.0/Math.PI;
				if (tsn < 0) tsn = tsn+180;
				
				if (tsn > maxAngle) maxAngle = tsn;
				if (tsn < minAngle) minAngle = tsn;

				if (scoren > this.anisoThresh) {
					this.resultImgScore.putValueDouble(x,y,scoren*255);
					h[y*diffsImg.getSizeY() + x] = 2*tsn;
					s[y*diffsImg.getSizeY() + x] = scoren;
					v[y*diffsImg.getSizeY() + x] = 1.0;
				}
			}
		}
		
		System.out.println("Max = " + maxAngle + " , Min = " + minAngle);
		
		HSVToRGBArrayConverter conv = new HSVToRGBArrayConverter(h,s,v);
		conv.runOp();
		LinkedList<int[]> rgbColor = conv.getRGBResult();

		int xc=0, yc=0;
		for (int[] color: rgbColor) {
			this.resultImgDirection.putValueR(xc,yc,color[0]); 
			this.resultImgDirection.putValueG(xc,yc,color[1]); 
			this.resultImgDirection.putValueB(xc,yc,color[2]);
			++xc;
			if (xc >= diffsImg.getSizeX()) {
				xc=0;
				++yc;
			}
		}
	}
}
