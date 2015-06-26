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

package de.unihalle.informatik.MiToBo.tools.images;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageShort;
import de.unihalle.informatik.MiToBo.tools.image.ImageContrastReducer;
import de.unihalle.informatik.MiToBo.tools.image.ImageContrastReducer.ResultValueMode;
import de.unihalle.informatik.MiToBo.tools.image.ImageContrastReducer.TargetContrast;

/**
 * JUnit test class for {@link ImageContrastReducer}.
 * 
 * @author moeller
 */
public class TestImageContrastReducer {

	/**
	 * Dummy byte image for tests.
	 */
	protected MTBImageByte dummyImageByte;
	
	/**
	 * Dummy short image for tests.
	 */
	protected MTBImageShort dummyImageShort;

	/**
	 * Test operator.
	 */
	protected ImageContrastReducer contrastOp;
	
	/**
	 * Fixture.
	 */
	@Before
	public void initTestClass() {
		// create test images
		this.dummyImageByte = (MTBImageByte)MTBImage.createMTBImage(
			256, 1, 1, 1, 1, MTBImageType.MTB_BYTE);
		for (int x=0;x<256;++x)
			this.dummyImageByte.putValueInt(x, 0, x);
		this.dummyImageShort = (MTBImageShort)MTBImage.createMTBImage(
			65536, 1, 1, 1, 1, MTBImageType.MTB_SHORT);
		for (int x=0;x<65536;++x)
			this.dummyImageShort.putValueInt(x, 0, x);
		// init operator
		try {
	    this.contrastOp = new ImageContrastReducer();
    } catch (ALDOperatorException e) {
	    e.printStackTrace();
    }
	}
	
	/**
	 * Test if contrast reduction is ok.
	 */
	@Test
	public void testContrastReduction() {
		// test byte image
		this.testByteImage();
		// test short image
		this.testShortImage();
	}
	
	private void testShortImage() {
		int shortImgSize = this.dummyImageShort.getSizeX();
		boolean exceptionThrown = false;
		try {
			this.contrastOp.setInImg(this.dummyImageShort);

			/* short to 8-bit */
			this.contrastOp.setTargetContrast(TargetContrast.BIT_8);
			this.contrastOp.setResultValueSelectionMode(ResultValueMode.ZERO_TO_N);
			this.contrastOp.runOp();
			MTBImage result = this.contrastOp.getResultImg();
			assertTrue("[TestImageContrastReducer] Short -> Byte: " 
				+ "result image has wrong type!",
					result.getType().equals(MTBImageType.MTB_SHORT));
			for (int x=0; x<shortImgSize; ++x) {
				int targetValue = x/256;
				assertTrue("[TestImageContrastReducer] Short -> Byte: " 
					+ "error at position " + x + ", expected " + targetValue 
						+ ", got " + result.getValueInt(x, 0) + "!",
							result.getValueInt(x, 0) == targetValue);
			}
			this.contrastOp.setResultValueSelectionMode(ResultValueMode.INTERVAL_MIN);
			this.contrastOp.runOp();
			result = this.contrastOp.getResultImg();
			assertTrue("[TestImageContrastReducer] Short -> 8-bit (I_MIN): " 
				+ "result image has wrong type!",
					result.getType().equals(MTBImageType.MTB_SHORT));
			for (int x=0; x<shortImgSize; ++x) {
				int targetValue = x/256 * 256;
				assertTrue("[TestImageContrastReducer] Short -> 8-bit (I_MIN): " 
					+ "error at position " + x + ", expected " + targetValue 
						+ ", got " + result.getValueInt(x, 0) + "!",
							result.getValueInt(x, 0) == targetValue);
			}
			this.contrastOp.setResultValueSelectionMode(ResultValueMode.INTERVAL_MAX);
			this.contrastOp.runOp();
			result = this.contrastOp.getResultImg();
			assertTrue("[TestImageContrastReducer] Short -> 8-bit (I_MAX): " 
				+ "result image has wrong type!",
					result.getType().equals(MTBImageType.MTB_SHORT));
			for (int x=0; x<shortImgSize; ++x) {
				int targetValue = (x/256 + 1) * 256 - 1;
				assertTrue("[TestImageContrastReducer] Short -> 8-bit (I_MAX): " 
					+ "error at position " + x + ", expected " + targetValue 
						+ ", got " + result.getValueInt(x, 0) + "!",
							result.getValueInt(x, 0) == targetValue);
			}
			this.contrastOp.setResultValueSelectionMode(
				ResultValueMode.INTERVAL_CENTER);
			this.contrastOp.runOp();
			result = this.contrastOp.getResultImg();
			assertTrue("[TestImageContrastReducer] Short -> 8-bit (I_CENTER): " 
					+ "result image has wrong type!",
					result.getType().equals(MTBImageType.MTB_SHORT));
			for (int x=0; x<shortImgSize; ++x) {
				int targetValue = (x/256) * 256 + 128;
				assertTrue("[TestImageContrastReducer] Short -> 8-bit (I_CENTER): " 
					+ "error at position " + x + ", expected " + targetValue 
						+ ", got " + result.getValueInt(x, 0) + "!",
							result.getValueInt(x, 0) == targetValue);
			}

			/* short to 6-bit */
			this.contrastOp.setTargetContrast(TargetContrast.BIT_6);
			this.contrastOp.setResultValueSelectionMode(ResultValueMode.ZERO_TO_N);
			this.contrastOp.runOp();
			result = this.contrastOp.getResultImg();
			assertTrue("[TestImageContrastReducer] Short -> 6-bit (ZERO_TO_N): " 
				+ "result image has wrong type!",
					result.getType().equals(MTBImageType.MTB_SHORT));
			for (int x=0; x<shortImgSize; ++x) {
				int targetValue = x/1024;
				assertTrue("[TestImageContrastReducer] Short -> 6-bit (ZERO_TO_N): " 
					+ "error at position " + x + ", expected " + targetValue 
						+ ", got " + result.getValueInt(x, 0) + "!",
							result.getValueInt(x, 0) == targetValue);
			}
			this.contrastOp.setResultValueSelectionMode(ResultValueMode.INTERVAL_MIN);
			this.contrastOp.runOp();
			result = this.contrastOp.getResultImg();
			assertTrue("[TestImageContrastReducer] Short -> 6-bit (I_MIN): " 
				+ "result image has wrong type!",
					result.getType().equals(MTBImageType.MTB_SHORT));
			for (int x=0; x<shortImgSize; ++x) {
				int targetValue = x/1024 * 1024;
				assertTrue("[TestImageContrastReducer] Short -> 6-bit (I_MIN): " 
					+ "error at position " + x + ", expected " + targetValue 
						+ ", got " + result.getValueInt(x, 0) + "!",
							result.getValueInt(x, 0) == targetValue);
			}
			this.contrastOp.setResultValueSelectionMode(ResultValueMode.INTERVAL_MAX);
			this.contrastOp.runOp();
			result = this.contrastOp.getResultImg();
			assertTrue("[TestImageContrastReducer] Short -> 6-bit (I_MAX): " 
				+ "result image has wrong type!",
					result.getType().equals(MTBImageType.MTB_SHORT));
			for (int x=0; x<shortImgSize; ++x) {
				int targetValue = (x/1024 + 1) * 1024 - 1;
				assertTrue("[TestImageContrastReducer] Short -> 6-bit (I_MAX): " 
					+ "error at position " + x + ", expected " + targetValue 
						+ ", got " + result.getValueInt(x, 0) + "!",
							result.getValueInt(x, 0) == targetValue);
			}
			this.contrastOp.setResultValueSelectionMode(
				ResultValueMode.INTERVAL_CENTER);
			this.contrastOp.runOp();
			result = this.contrastOp.getResultImg();
			assertTrue("[TestImageContrastReducer] Short -> 6-bit (I_CENTER): " 
				+ "result image has wrong type!",
					result.getType().equals(MTBImageType.MTB_SHORT));
			for (int x=0; x<shortImgSize; ++x) {
				int targetValue = (x/1024) * 1024 + 512;
				assertTrue("[TestImageContrastReducer] Short -> 6-bit (I_CENTER): " 
					+ "error at position " + x + ", expected " + targetValue 
						+ ", got " + result.getValueInt(x, 0) + "!",
							result.getValueInt(x, 0) == targetValue);
			}

			/* short to 4-bit */
			this.contrastOp.setTargetContrast(TargetContrast.BIT_4);
			this.contrastOp.setResultValueSelectionMode(ResultValueMode.ZERO_TO_N);
			this.contrastOp.runOp();
			result = this.contrastOp.getResultImg();
			assertTrue("[TestImageContrastReducer] Short -> 4-bit (ZERO_TO_N): " 
				+ "result image has wrong type!",
					result.getType().equals(MTBImageType.MTB_SHORT));
			for (int x=0; x<shortImgSize; ++x) {
				int targetValue = x/4096;
				assertTrue("[TestImageContrastReducer] Short -> 4-bit (ZERO_TO_N): " 
					+ "error at position " + x + ", expected " + targetValue 
						+ ", got " + result.getValueInt(x, 0) + "!",
							result.getValueInt(x, 0) == targetValue);
			}
			this.contrastOp.setResultValueSelectionMode(ResultValueMode.INTERVAL_MIN);
			this.contrastOp.runOp();
			result = this.contrastOp.getResultImg();
			assertTrue("[TestImageContrastReducer] Short -> 4-bit (I_MIN): " 
				+ "result image has wrong type!",
					result.getType().equals(MTBImageType.MTB_SHORT));
			for (int x=0; x<shortImgSize; ++x) {
				int targetValue = x/4096 * 4096;
				assertTrue("[TestImageContrastReducer] Short -> 4-bit (I_MIN): " 
					+ "error at position " + x + ", expected " + targetValue 
						+ ", got " + result.getValueInt(x, 0) + "!",
							result.getValueInt(x, 0) == targetValue);
			}
			this.contrastOp.setResultValueSelectionMode(ResultValueMode.INTERVAL_MAX);
			this.contrastOp.runOp();
			result = this.contrastOp.getResultImg();
			assertTrue("[TestImageContrastReducer] Short -> 4-bit (I_MAX): " 
				+ "result image has wrong type!",
					result.getType().equals(MTBImageType.MTB_SHORT));
			for (int x=0; x<shortImgSize; ++x) {
				int targetValue = (x/4096 + 1) * 4096 - 1;
				assertTrue("[TestImageContrastReducer] Short -> 4-bit (I_MAX): " 
					+ "error at position " + x + ", expected " + targetValue 
						+ ", got " + result.getValueInt(x, 0) + "!",
							result.getValueInt(x, 0) == targetValue);
			}
			this.contrastOp.setResultValueSelectionMode(
				ResultValueMode.INTERVAL_CENTER);
			this.contrastOp.runOp();
			result = this.contrastOp.getResultImg();
			assertTrue("[TestImageContrastReducer] Short -> 4-bit (I_CENTER): " 
				+ "result image has wrong type!",
					result.getType().equals(MTBImageType.MTB_SHORT));
			for (int x=0; x<shortImgSize; ++x) {
				int targetValue = (x/4096) * 4096 + 2048;
				assertTrue("[TestImageContrastReducer] Short -> 4-bit (I_CENTER): " 
					+ "error at position " + x + ", expected " + targetValue 
						+ ", got " + result.getValueInt(x, 0) + "!",
							result.getValueInt(x, 0) == targetValue);
			}

			/* short to 2-bit */
			this.contrastOp.setTargetContrast(TargetContrast.BIT_2);
			this.contrastOp.setResultValueSelectionMode(ResultValueMode.ZERO_TO_N);
			this.contrastOp.runOp();
			result = this.contrastOp.getResultImg();
			assertTrue("[TestImageContrastReducer] Short -> 2-bit (ZERO_TO_N): " 
				+ "result image has wrong type!",
					result.getType().equals(MTBImageType.MTB_SHORT));
			for (int x=0; x<shortImgSize; ++x) {
				int targetValue = x/16384;
				assertTrue("[TestImageContrastReducer] Short -> 2-bit (ZERO_TO_N): " 
					+ "error at position " + x + ", expected " + targetValue 
						+ ", got " + result.getValueInt(x, 0) + "!",
							result.getValueInt(x, 0) == targetValue);
			}
			this.contrastOp.setResultValueSelectionMode(ResultValueMode.INTERVAL_MIN);
			this.contrastOp.runOp();
			result = this.contrastOp.getResultImg();
			assertTrue("[TestImageContrastReducer] Short -> 2-bit (I_MIN): " 
				+ "result image has wrong type!",
					result.getType().equals(MTBImageType.MTB_SHORT));
			for (int x=0; x<shortImgSize; ++x) {
				int targetValue = x/16384 * 16384;
				assertTrue("[TestImageContrastReducer] Short -> 2-bit (I_MIN): " 
					+ "error at position " + x + ", expected " + targetValue 
						+ ", got " + result.getValueInt(x, 0) + "!",
							result.getValueInt(x, 0) == targetValue);
			}
			this.contrastOp.setResultValueSelectionMode(ResultValueMode.INTERVAL_MAX);
			this.contrastOp.runOp();
			result = this.contrastOp.getResultImg();
			assertTrue("[TestImageContrastReducer] Short -> 2-bit (I_MAX): " 
				+ "result image has wrong type!",
					result.getType().equals(MTBImageType.MTB_SHORT));
			for (int x=0; x<shortImgSize; ++x) {
				int targetValue = (x/16384 + 1) * 16384 - 1;
				assertTrue("[TestImageContrastReducer] Short -> 2-bit (I_MAX): " 
					+ "error at position " + x + ", expected " + targetValue 
						+ ", got " + result.getValueInt(x, 0) + "!",
						result.getValueInt(x, 0) == targetValue);
			}
			this.contrastOp.setResultValueSelectionMode(
				ResultValueMode.INTERVAL_CENTER);
			this.contrastOp.runOp();
			result = this.contrastOp.getResultImg();
			assertTrue("[TestImageContrastReducer] Short -> 2-bit (I_CENTER): " 
				+ "result image has wrong type!",
					result.getType().equals(MTBImageType.MTB_SHORT));
			for (int x=0; x<shortImgSize; ++x) {
				int targetValue = (x/16384) * 16384 + 8192;
				assertTrue("[TestImageContrastReducer] Short -> 2-bit (I_CENTER): " 
					+ "error at position " + x + ", expected " + targetValue 
						+ ", got " + result.getValueInt(x, 0) + "!",
							result.getValueInt(x, 0) == targetValue);
			}
		} catch (Exception e) {
			exceptionThrown = true;
		}
		assertFalse("Operator has thrown an exception!", exceptionThrown);
	}

	private void testByteImage() {
		boolean exceptionThrown = false;
		try {
			this.contrastOp.setInImg(this.dummyImageByte);
			this.contrastOp.setTargetContrast(TargetContrast.BIT_8);
			this.contrastOp.runOp();
			MTBImage result = this.contrastOp.getResultImg();
			assertTrue("[TestImageContrastReducer] Byte -> Byte: " 
					+ "result image has wrong type!",
					result.getType().equals(MTBImageType.MTB_BYTE));
			for (int x=0; x<256; ++x)
				assertTrue("[TestImageContrastReducer] Byte -> Byte: " 
						+ "error at position " + x + ", expected " + x 
						+ ", got " + result.getValueInt(x, 0) + "!",
						result.getValueInt(x, 0) == x);

			/* byte to 6-bit */
			this.contrastOp.setTargetContrast(TargetContrast.BIT_6);
			this.contrastOp.setResultValueSelectionMode(ResultValueMode.ZERO_TO_N);
			this.contrastOp.runOp();
			result = this.contrastOp.getResultImg();
			assertTrue("[TestImageContrastReducer] Byte -> 6-bit (ZERO_TO_N): " 
					+ "result image has wrong type!",
					result.getType().equals(MTBImageType.MTB_BYTE));
			for (int x=0; x<256; ++x) {
				int targetValue = x/4;
				assertTrue("[TestImageContrastReducer] Byte -> 6-bit (ZERO_TO_N): " 
						+ "error at position " + x + ", expected " + targetValue 
						+ ", got " + result.getValueInt(x, 0) + "!",
						result.getValueInt(x, 0) == targetValue);
			}
			this.contrastOp.setResultValueSelectionMode(ResultValueMode.INTERVAL_MIN);
			this.contrastOp.runOp();
			result = this.contrastOp.getResultImg();
			assertTrue("[TestImageContrastReducer] Byte -> 6-bit (I_MIN): " 
					+ "result image has wrong type!",
					result.getType().equals(MTBImageType.MTB_BYTE));
			for (int x=0; x<256; ++x) {
				int targetValue = x/4 * 4;
				assertTrue("[TestImageContrastReducer] Byte -> 6-bit (I_MIN): " 
						+ "error at position " + x + ", expected " + targetValue 
						+ ", got " + result.getValueInt(x, 0) + "!",
						result.getValueInt(x, 0) == targetValue);
			}
			this.contrastOp.setResultValueSelectionMode(ResultValueMode.INTERVAL_MAX);
			this.contrastOp.runOp();
			result = this.contrastOp.getResultImg();
			assertTrue("[TestImageContrastReducer] Byte -> 6-bit (I_MAX): " 
					+ "result image has wrong type!",
					result.getType().equals(MTBImageType.MTB_BYTE));
			for (int x=0; x<256; ++x) {
				int targetValue = (x/4 + 1) * 4 - 1;
				assertTrue("[TestImageContrastReducer] Byte -> 6-bit (I_MAX): " 
						+ "error at position " + x + ", expected " + targetValue 
						+ ", got " + result.getValueInt(x, 0) + "!",
						result.getValueInt(x, 0) == targetValue);
			}
			this.contrastOp.setResultValueSelectionMode(ResultValueMode.INTERVAL_CENTER);
			this.contrastOp.runOp();
			result = this.contrastOp.getResultImg();
			assertTrue("[TestImageContrastReducer] Byte -> 6-bit (I_CENTER): " 
					+ "result image has wrong type!",
					result.getType().equals(MTBImageType.MTB_BYTE));
			for (int x=0; x<256; ++x) {
				int targetValue = (x/4) * 4 + 2;
				assertTrue("[TestImageContrastReducer] Byte -> 6-bit (I_CENTER): " 
						+ "error at position " + x + ", expected " + targetValue 
						+ ", got " + result.getValueInt(x, 0) + "!",
						result.getValueInt(x, 0) == targetValue);
			}

			/* byte to 4-bit */
			this.contrastOp.setTargetContrast(TargetContrast.BIT_4);
			this.contrastOp.setResultValueSelectionMode(ResultValueMode.ZERO_TO_N);
			this.contrastOp.runOp();
			result = this.contrastOp.getResultImg();
			assertTrue("[TestImageContrastReducer] Byte -> 4-bit (ZERO_TO_N): " 
					+ "result image has wrong type!",
					result.getType().equals(MTBImageType.MTB_BYTE));
			for (int x=0; x<256; ++x) {
				int targetValue = x/16;
				assertTrue("[TestImageContrastReducer] Byte -> 4-bit (ZERO_TO_N): " 
						+ "error at position " + x + ", expected " + targetValue 
						+ ", got " + result.getValueInt(x, 0) + "!",
						result.getValueInt(x, 0) == targetValue);
			}
			this.contrastOp.setResultValueSelectionMode(ResultValueMode.INTERVAL_MIN);
			this.contrastOp.runOp();
			result = this.contrastOp.getResultImg();
			assertTrue("[TestImageContrastReducer] Byte -> 4-bit (I_MIN): " 
					+ "result image has wrong type!",
					result.getType().equals(MTBImageType.MTB_BYTE));
			for (int x=0; x<256; ++x) {
				int targetValue = x/16 * 16;
				assertTrue("[TestImageContrastReducer] Byte -> 4-bit (I_MIN): " 
						+ "error at position " + x + ", expected " + targetValue 
						+ ", got " + result.getValueInt(x, 0) + "!",
						result.getValueInt(x, 0) == targetValue);
			}
			this.contrastOp.setResultValueSelectionMode(ResultValueMode.INTERVAL_MAX);
			this.contrastOp.runOp();
			result = this.contrastOp.getResultImg();
			assertTrue("[TestImageContrastReducer] Byte -> 4-bit (I_MAX): " 
					+ "result image has wrong type!",
					result.getType().equals(MTBImageType.MTB_BYTE));
			for (int x=0; x<256; ++x) {
				int targetValue = (x/16 + 1) * 16 - 1;
				assertTrue("[TestImageContrastReducer] Byte -> 4-bit (I_MAX): " 
						+ "error at position " + x + ", expected " + targetValue 
						+ ", got " + result.getValueInt(x, 0) + "!",
						result.getValueInt(x, 0) == targetValue);
			}
			this.contrastOp.setResultValueSelectionMode(ResultValueMode.INTERVAL_CENTER);
			this.contrastOp.runOp();
			result = this.contrastOp.getResultImg();
			assertTrue("[TestImageContrastReducer] Byte -> 4-bit (I_CENTER): " 
					+ "result image has wrong type!",
					result.getType().equals(MTBImageType.MTB_BYTE));
			for (int x=0; x<256; ++x) {
				int targetValue = (x/16) * 16 + 8;
				assertTrue("[TestImageContrastReducer] Byte -> 4-bit (I_CENTER): " 
						+ "error at position " + x + ", expected " + targetValue 
						+ ", got " + result.getValueInt(x, 0) + "!",
						result.getValueInt(x, 0) == targetValue);
			}

			/* byte to 2-bit */
			this.contrastOp.setTargetContrast(TargetContrast.BIT_2);
			this.contrastOp.setResultValueSelectionMode(ResultValueMode.ZERO_TO_N);
			this.contrastOp.runOp();
			result = this.contrastOp.getResultImg();
			assertTrue("[TestImageContrastReducer] Byte -> 2-bit (ZERO_TO_N): " 
					+ "result image has wrong type!",
					result.getType().equals(MTBImageType.MTB_BYTE));
			for (int x=0; x<256; ++x) {
				int targetValue = x/64;
				assertTrue("[TestImageContrastReducer] Byte -> 2-bit (ZERO_TO_N): " 
						+ "error at position " + x + ", expected " + targetValue 
						+ ", got " + result.getValueInt(x, 0) + "!",
						result.getValueInt(x, 0) == targetValue);
			}
			this.contrastOp.setResultValueSelectionMode(ResultValueMode.INTERVAL_MIN);
			this.contrastOp.runOp();
			result = this.contrastOp.getResultImg();
			assertTrue("[TestImageContrastReducer] Byte -> 2-bit (I_MIN): " 
					+ "result image has wrong type!",
					result.getType().equals(MTBImageType.MTB_BYTE));
			for (int x=0; x<256; ++x) {
				int targetValue = x/64 * 64;
				assertTrue("[TestImageContrastReducer] Byte -> 2-bit (I_MIN): " 
						+ "error at position " + x + ", expected " + targetValue 
						+ ", got " + result.getValueInt(x, 0) + "!",
						result.getValueInt(x, 0) == targetValue);
			}
			this.contrastOp.setResultValueSelectionMode(ResultValueMode.INTERVAL_MAX);
			this.contrastOp.runOp();
			result = this.contrastOp.getResultImg();
			assertTrue("[TestImageContrastReducer] Byte -> 2-bit (I_MAX): " 
					+ "result image has wrong type!",
					result.getType().equals(MTBImageType.MTB_BYTE));
			for (int x=0; x<256; ++x) {
				int targetValue = (x/64 + 1) * 64 - 1;
				assertTrue("[TestImageContrastReducer] Byte -> 2-bit (I_MAX): " 
						+ "error at position " + x + ", expected " + targetValue 
						+ ", got " + result.getValueInt(x, 0) + "!",
						result.getValueInt(x, 0) == targetValue);
			}
			this.contrastOp.setResultValueSelectionMode(ResultValueMode.INTERVAL_CENTER);
			this.contrastOp.runOp();
			result = this.contrastOp.getResultImg();
			assertTrue("[TestImageContrastReducer] Byte -> 2-bit (I_CENTER): " 
					+ "result image has wrong type!",
					result.getType().equals(MTBImageType.MTB_BYTE));
			for (int x=0; x<256; ++x) {
				int targetValue = (x/64) * 64 + 32;
				assertTrue("[TestImageContrastReducer] Byte -> 2-bit (I_CENTER): " 
						+ "error at position " + x + ", expected " + targetValue 
						+ ", got " + result.getValueInt(x, 0) + "!",
						result.getValueInt(x, 0) == targetValue);
			}
		} catch (Exception e) {
			exceptionThrown = true;
		}
		assertFalse("Operator has thrown an exception!", exceptionThrown);
	}
}
