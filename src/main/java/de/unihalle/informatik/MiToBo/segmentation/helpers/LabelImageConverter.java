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

package de.unihalle.informatik.MiToBo.segmentation.helpers;

import java.util.HashMap;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.ExecutionMode;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageRGB;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageShort;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;

/**
 * Converts (pseudo-) colored RGB labels to unique gray-scale labels.
 * 
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ExecutionMode.ALL, level=Level.STANDARD)
public class LabelImageConverter extends MTBOperator {

	/**
	 * Colored label image.
	 */
	@Parameter( label= "Input Color Label Image", required = true, 
			direction = Direction.IN,	description = "Colored label image.")
	private MTBImageRGB labelImageColor = null;

	/**
	 * Output gray-scale image.
	 */
	@Parameter( label= "Output gray-scale image", 
			direction = Direction.OUT, description = "Gray-scale label image.")
	private MTBImageShort labelImageGray = null;

	/* 
	 * local helpers
	 */
	
	/**
	 * Input image width.
	 */
	private int width;
	
	/**
	 * Input image height.
	 */
	private int height;
	
	/**
	 * Default constructor.
	 * 
	 * @throws ALDOperatorException
	 */
	public LabelImageConverter() throws ALDOperatorException {
	  super();
  }

	/**
	 * Default constructor with parameters.
	 *
	 * @param colorMask		Colored input label image.
	 * @return Gray-scale label image.
	 * @throws ALDOperatorException
	 */
	public LabelImageConverter(MTBImageRGB colorMask) 
		throws ALDOperatorException {
		this.labelImageColor = colorMask;
  }
	
	/**
	 * Returns result label image.
	 */
	public MTBImageShort getResultLabelImage() {
		return this.labelImageGray;
	}

	@Override
	protected void operate() {
		
		// convert RGB to gray-scale with unique labels
    this.width = this.labelImageColor.getSizeX();
    this.height = this.labelImageColor.getSizeY();
    this.labelImageGray = 
    	(MTBImageShort)MTBImage.createMTBImage(this.width, this.height, 
    																				1, 1, 1, MTBImageType.MTB_SHORT);
    
    // hash map matching colors to grayscale labels
    HashMap<Integer, Integer> colorMap =  new HashMap<Integer, Integer>();
    int labelCount = 1;
    for (int h= 0; h<this.height; ++h) {
    	for (int w=0; w<this.width; ++w) {
    		if (this.labelImageColor.getValueInt(w, h) == 0)
    			this.labelImageGray.putValueInt(w, h, 0);
    		else {
    			Integer color = 
    				new Integer(this.labelImageColor.getValueInt(w, h));
    			Integer gray;
    			if (colorMap.containsKey(color)) {
    				gray = colorMap.get(color);
    			}
    			else {
    				gray = new Integer(labelCount);
    				colorMap.put(color, gray);
    				++labelCount;
    			}
    			this.labelImageGray.putValueInt(w, h, gray.intValue());
    		}
    	}
    }
	}
}
