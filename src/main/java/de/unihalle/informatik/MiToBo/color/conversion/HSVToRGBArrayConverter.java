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

package de.unihalle.informatik.MiToBo.color.conversion;

import java.util.LinkedList;

import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;

/**
 * <pre>
 * Class to convert a whole array of values from HSV color space to RGB.
 * <p>
 * {@link HSVToRGBPixelConverter}
 * 
 * @author Birgit Moeller
 */

public class HSVToRGBArrayConverter extends MTBOperator {
  /**
   * Array of hue values.
   */
	@Parameter( label= "Hue", required = true, dataIOOrder = 1, 
		direction= Parameter.Direction.IN, description= "Hue input values.")
  private double[] h;
  /**
   * Array of saturation values.
   */
	@Parameter( label= "Saturation", required = true, dataIOOrder = 2, 
		direction= Parameter.Direction.IN, description= "Saturation input values.")
  private double[] s;
  /**
   * Array of intensity values.
   */
	@Parameter( label= "Brightness Value", required = true, dataIOOrder = 3, 
		direction= Parameter.Direction.IN, description= "Brightness input values.")
  private double[] v;

	/**
	 * Resulting RGB values.
	 */
	@Parameter( label= "RGB values", required = true, dataIOOrder = 1, 
		direction = Parameter.Direction.OUT, description = "Result.")
  private LinkedList<int[]> rgbColor;

	/**
   * Standard constructor.
   */
  public HSVToRGBArrayConverter() throws ALDOperatorException {
  	// nothing to do here
  }

  /**
   * Constructor to create a HSVconverter object
   * 
   * @param _h	Hue values.
   * @param _s	Saturation values.
   * @param _v	Brightness values.
   */
  public HSVToRGBArrayConverter(double[] _h, double[] _s, double[] _v) 
  		throws ALDOperatorException {
    this.h = _h;
    this.s = _s;
    this.v = _v;
  }

  /**
   * Get RGB values.
   * @return	List of RGB color values.
   */
  public LinkedList<int[]> getRGBResult() {
  	return this.rgbColor;
  }
  
	@Override
  protected void operate() {
		this.toRGB();
  }
	
  /**
   * Method to convert colors from HSV color space to RGB color space.
   * <p>
   * This method falls back to methods from {@link HSVToRGBPixelConverter}.
   */
  private void toRGB() {
  	// init result list
  	int[] rgb;
  	this.rgbColor = new LinkedList<int[]>();
  	for (int i=0; i<this.h.length; ++i) {
  		rgb = HSVToRGBPixelConverter.toRGB(this.h[i], this.s[i],  this.v[i]);
  		this.rgbColor.add(rgb);
  	}
  }
}
