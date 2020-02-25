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

package de.unihalle.informatik.MiToBo.apps.nuclei2D;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.operator.*;

/**
 *  Class provides routines for separating conglomerates of cell nuclei in 2D.
 * 
 * @author moeller, posch
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,
	level=Level.APPLICATION,
	shortDescription="Routines for separating conglomerates of cell nuclei in 2D.")
public class NucleusSeparator2D extends MTBOperator {

	/**
	 * Display mode of the result image.
	 * 
	 * @author moeller
	 */
	public static enum ResultImageMode {
		/**
		 * Gray-scale label image as output.
		 */
		LABELS,
		/**
		 * Binary mask as output.
		 */
		BINARY
	}
	
	/**
	 * Input image to be processed.
	 */
	@Parameter( label= "Greyscale Input Image", required = true, 
			direction = Parameter.Direction.IN, dataIOOrder= -21, 
			mode= ExpertMode.STANDARD, 
      description = "Greyscale input image to be analyzed.")	
	private transient MTBImage inputImg= null;

	/**
	 * Binary nuclei image to be processed.
	 */
	@Parameter(label= "Label Input image", required = true, 
			mode= ExpertMode.STANDARD,  dataIOOrder= -20,
			direction= Direction.IN, description = "Label input image.")
	private transient MTBImage labelImg = null;

	/**
	 * Result label image of separated nuclei.
	 */
	@Parameter( label= "Result image", direction= Direction.OUT, 
      description = "Label image of separated nuclei.")
	private transient MTBImageShort resultImg = null;

	/**
	 * Operation mode of the operator.
	 */
	@Parameter( label="Operator Mode", required=true, mode=ExpertMode.STANDARD,
			direction= Direction.IN, description = "Operator separation mode.")
	private NucleusSeparator2DAlgos nucSepOp = new NucleusSeparator2DBeamCut();

	/**
	 * Mode of how to display result image.
	 */
	@Parameter( label= "Result image display mode", 
			required = false, mode= ExpertMode.STANDARD, 
			direction= Direction.IN, supplemental = true,
			description = "Mode how result image is displayed.")
	private ResultImageMode resultDisplayMode = ResultImageMode.LABELS;	

	/**
	 * Empty constructor.
	 * @throws ALDOperatorException
	 */
	public NucleusSeparator2D() throws ALDOperatorException {
		// nothing to do here
	}

	/**
	 * Default constructor.
	 * 
	 * @param label							Label image to be processed.
	 * @param mode							Operator mode to be applied.
	 * @throws ALDOperatorException
	 */
	public NucleusSeparator2D(MTBImage label, NucleusSeparator2DAlgos op) 
		throws ALDOperatorException {
		this.labelImg = label;
		this.nucSepOp = op;
	}

	/**
	 * Specify label input image.
	 */
	public void setInputLabelImage(MTBImage img) {
		this.labelImg = img;
	}

	/**
	 * Specify input gray-scale image.
	 */
	public void setInputGrayScaleImage(MTBImage img) {
		this.inputImg = img;
	}

	/**
	 * Returns result image, i.e. the segmentation mask.
	 * @return Returns the result label image.
	 */
	public MTBImageShort getResultImage() {
		return this.resultImg;
	}

	@Override
	protected void operate() throws ALDOperatorException,
	ALDProcessingDAGException {

		// reset detector in case it is run multiple times
		this.resultImg = null;
		this.nucSepOp.setInputLabelImage(this.labelImg);
		this.nucSepOp.setInputGrayScaleImage(this.inputImg);
		this.nucSepOp.runOp(false);
		this.prepareResultImage(this.nucSepOp.getResultImg());
	}
	
	/**
	 * Prepare visualization of result.
	 */
	private void prepareResultImage(MTBImageShort labelImage) {
		
		switch (this.resultDisplayMode)
		{
		case LABELS:
			this.resultImg = labelImage;
			break;
		case BINARY:
			int width = labelImage.getSizeX();
			int height = labelImage.getSizeY();
			this.resultImg = (MTBImageShort)MTBImage.createMTBImage(
					width, height, 1, 1, 1,	MTBImage.MTBImageType.MTB_SHORT);
			this.resultImg.fillBlack();
			for (int y = 0; y < height; ++y) {
				for (int x = 0; x < width; ++x) {
					if (labelImage.getValueInt(x, y) > 0) {
						this.resultImg.putValueInt(x, y, Short.MAX_VALUE);
					}
				}
			}
			break;
		}
		this.resultImg.setTitle("Separation result for image \"" +
				labelImage.getTitle() + "\"...");
	} 
	
	@Override
	public String getDocumentation() {
		return "<ul><li>\n" + 
				"<p>this operator provides methods to split-up nuclei regions which have been merged during segmentation</p>\n" + 
				"</li><li>\n" + 
				"<p>it supports different modes discussed in detail below</p>\n" + 
				"</li><li>\n" + 
				"<p>output of the operator is a binary or label images of detected (separated) nuclei regions</p>\n" + 
				"</li></ul>\n" + 
				"<h2>Usage:</h2>\n" + 
				"<h3>Required parameters:</h3>\n" + 
				"\n" + 
				"<ul><li>\n" + 
				"<p><tt>Greyscale Input Image</tt> \n" + 
				"<ul><li>\n" + 
				"<p>the single-channel gray-scale image to be analyzed</p>\n" + 
				"</li><li>\n" + 
				"<p>if the image contains multiple channels, only the first one is processed</p>\n" + 
				"</li></ul>\n" + 
				"</p>\n" + 
				"</li><li>\n" + 
				"<p><tt> Label Input Image</tt>\n" + 
				"<ul><li>\n" + 
				"<p>label image encoding the result of a prior nucleus detection with merged nuclei</p>\n" + 
				"</li></ul>\n" + 
				"</p>\n" + 
				"</li><li>\n" + 
				"<p><tt>Operator mode</tt>\n" + 
				"<ul><li>\n" + 
				"<p>operator to be used for region separation, currently supported are <a href=\"stml:de.unihalle.informatik.MiToBo.apps.nuclei2D.NucleusSeparator2DBeamCut\">Beam-Cut</a> and <a href=\"stml:de.unihalle.informatik.MiToBo.apps.nuclei2D.NucleusSeparator2DPeakSearch\">Peak-Search</a> separation</p>\n" + 
				"</li></ul>\n" + 
				"</p>\n" + 
				"</li></ul>\n" + 
				"<h3>Supplemental parameters:</h3>\n" + 
				"\n" + 
				"<ul><li>\n" + 
				"<p><tt>Result image display mode</tt> \n" + 
				"<ul><li>\n" + 
				"<p>allows switch between binary and gray-scale (labeled) result images</p>\n" + 
				"</li></ul>\n" + 
				"</p>\n" + 
				"</li><li>\n" + 
				"<p><tt> Verbose</tt>\n" + 
				"<ul><li>\n" + 
				"<p>enables/disables display of additional console output</p>\n" + 
				"</li></ul>\n" + 
				"</p>\n" + 
				"</li></ul>";
	}
}
