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

package de.unihalle.informatik.MiToBo.color;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.MiToBo.color.conversion.HSIToRGBPixelConverter;

/**
 * JUnit test class for {@link HSIToRGBPixelConverter}.
 * 
 * @author Birgit Moeller
 */
public class TestHSIToRGBConverter {

	/**
	 * Accuracy for calculations.
	 */
	private static final double accuracy = 1.0e-10;
	
	/**
	 * Object to test.
	 */
	private HSIToRGBPixelConverter testObj;
	
	/**
	 * Fixture.
	 */
	@Before
	public void initTestClass() {
		boolean thrown = false;
		try {
	    this.testObj = new HSIToRGBPixelConverter();
    } catch (ALDOperatorException e) {
    	thrown = true;
    }
		assertFalse("[TestHSIToRGBConverter] init failed, caught an exception...!",
			thrown);
	}

	/**
	 * Test conversion of HSI colors to RGB values.
	 */
	@Test
	public void testColorConversion() {

		double[] hsi = new double[3];
		double[] rgb = new double[3];
		
		// color black
		hsi[0] = 0.0; hsi[1] = 0.0; hsi[2] = 0.0;
		rgb[0] = 0.0; rgb[1] = 0.0; rgb[2] = 0.0;
		this.colorCheck("black", hsi, rgb);

		// color white
		hsi[0] = 0.0; hsi[1] = 0.0; hsi[2] = 1.0;
		rgb[0] = 1.0; rgb[1] = 1.0; rgb[2] = 1.0;
		this.colorCheck("white", hsi, rgb);

		// color red
		hsi[0] = 0.0; hsi[1] = 1.0; hsi[2] = 1.0/3.0;
		rgb[0] = 1.0; rgb[1] = 0.0; rgb[2] = 0.0;
		this.colorCheck("red", hsi, rgb);

		// color green
		hsi[0] = 0.5*2.0/3.0; hsi[1] = 1.0; hsi[2] = 1.0/3.0;
		rgb[0] = 0.0; rgb[1] = 1.0; rgb[2] = 0.0;
		this.colorCheck("green", hsi, rgb);

		// color blue
		hsi[0] = 0.5*4.0/3.0; hsi[1] = 1.0; hsi[2] = 1.0/3.0;
		rgb[0] = 0.0; rgb[1] = 0.0; rgb[2] = 1.0;
		this.colorCheck("blue", hsi, rgb);
		
		// color cyan
		hsi[0] = 0.5; hsi[1] = 1.0; hsi[2] = 2.0/3.0;
		rgb[0] = 0.0; rgb[1] = 1.0; rgb[2] = 1.0;
		this.colorCheck("cyan", hsi, rgb);

		// color yellow
		hsi[0] = 0.5/3.0; hsi[1] = 1.0; hsi[2] = 2.0/3.0;
		rgb[0] = 1.0; rgb[1] = 1.0; rgb[2] = 0.0;
		this.colorCheck("yellow", hsi, rgb);

		// color magenta
		hsi[0] = 0.5*5.0/3.0; hsi[1] = 1.0; hsi[2] = 2.0/3.0;
		rgb[0] = 1.0; rgb[1] = 0.0; rgb[2] = 1.0;
		this.colorCheck("magenta", hsi, rgb);
		
		// color red with reduced intensity
		hsi[0] = 0.0; hsi[1] = 1.0; hsi[2] = 1.0/6.0;
		rgb[0] = 0.5; rgb[1] = 0.0; rgb[2] = 0.0;
		this.colorCheck("intensity-reduced red", hsi, rgb);

		// color green with reduced intensity
		hsi[0] = 0.5*2.0/3.0; hsi[1] = 1.0; hsi[2] = 1.0/6.0;
		rgb[0] = 0.0; rgb[1] = 0.5; rgb[2] = 0.0;
		this.colorCheck("intensity-reduced green", hsi, rgb);

		// color blue with reduced intensity
		hsi[0] = 0.5*4.0/3.0; hsi[1] = 1.0; hsi[2] = 1.0/6.0;
		rgb[0] = 0.0; rgb[1] = 0.0; rgb[2] = 0.5;
		this.colorCheck("intensity-reduced blue", hsi, rgb);
		
		// color cyan with reduced intensity
		hsi[0] = 0.5; hsi[1] = 1.0; hsi[2] = 1.0/3.0;
		rgb[0] = 0.0; rgb[1] = 0.5; rgb[2] = 0.5;
		this.colorCheck("intensity-reduced cyan", hsi, rgb);

		// color yellow with reduced intensity
		hsi[0] = 0.5/3.0; hsi[1] = 1.0; hsi[2] = 1.0/3.0;
		rgb[0] = 0.5; rgb[1] = 0.5; rgb[2] = 0.0;
		this.colorCheck("intensity-reduced yellow", hsi, rgb);

		// color magenta with reduced intensity
		hsi[0] = 0.5*5.0/3.0; hsi[1] = 1.0; hsi[2] = 1.0/3.0;
		rgb[0] = 0.5; rgb[1] = 0.0; rgb[2] = 0.5;
		this.colorCheck("intensity-reduced magenta", hsi, rgb);

		// color red with reduced saturation
		hsi[0] = 0; hsi[1] = 1.0/4.0; hsi[2] = 2.0/3.0;
		rgb[0] = 1; rgb[1] = 0.5; rgb[2] = 0.5;
		this.colorCheck("saturation-reduced red", hsi, rgb);
	}
	
	/**
	 * Checks an expected color value against conversion result.
	 * @param color		String identifier of color.
	 * @param hsi			HSI input values.
	 * @param rgb			Expected RGB output values.
	 */
	private void colorCheck(String color, double[] hsi, double[] rgb) {
		boolean thrown = false;
		double[] result;
		// validity checks
		assertTrue("[TestHSIToRGBConverter] R value is out of [0,1]!", 
			rgb[0] >= 0 && rgb[0] <= 1.0);
		assertTrue("[TestHSIToRGBConverter] G value is out of [0,1]!", 
			rgb[1] >= 0 && rgb[1] <= 1.0);
		assertTrue("[TestHSIToRGBConverter] B value is out of [0,1]!", 
			rgb[2] >= 0 && rgb[2] <= 1.0);
		try {
			this.testObj.setHSIInput(hsi);
			this.testObj.runOp();
			result = this.testObj.getResultRGB();
			assertTrue("[TestHSIToRGBConverter] " + color + " "
					+ "[" +rgb[0]+ " / " +rgb[1]+ " / " + rgb[2] + "] expected,\n "
					+ "got ["	+result[0]+ " / " +result[1]+ " / " + result[2] + "]", 
					   Math.abs(result[0]-rgb[0]) < accuracy
					&& Math.abs(result[1]-rgb[1]) < accuracy
					&& Math.abs(result[2]-rgb[2]) < accuracy);
		} catch (Exception e) {
			thrown = true;
		}
		assertFalse("[TestHSIToRGBConverter] caught an exception on conversion...!",
				thrown);
	}
}