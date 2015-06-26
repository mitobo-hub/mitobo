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

package de.unihalle.informatik.MiToBo.segmentation.basics;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.segmentation.basics.MTBSegmentation2D;
import de.unihalle.informatik.MiToBo.segmentation.basics.MTBSegmentation3D;
import de.unihalle.informatik.MiToBo.segmentation.basics.MTBSegmentationInterface;
import de.unihalle.informatik.MiToBo.segmentation.basics.MTBSegmentationInterface.SegmentationDimension;

/**
 * JUnit test class for {@link SegmentationInitializer}.
 * 
 * @author moeller
 */
public class TestSegmentationInitializer {

	private static final String IDS = "[SegmentationInitializer]";
	
	/**
	 * Dummy 2D image for tests.
	 */
	private MTBImage dummyImage2D;
	
	/**
	 * Dummy 3D image for tests.
	 */
	private MTBImage dummyImage3D;

	/**
	 * Object to test.
	 */
	private SegmentationInitializer testObject;
	
	/**
	 * Fixture.
	 */
	@Before
	public void initTestClass() {
		// instantiate dummy images
		this.dummyImage2D = 
				MTBImage.createMTBImage(10, 10, 1, 1, 1, MTBImageType.MTB_BYTE);
		this.dummyImage2D.fillBlack();
		for (int i=3;i<6;++i)
			for (int j=1;j<3;++j)
				this.dummyImage2D.putValueInt(i, j, 1);
		for (int i=8;i<10;++i)
			for (int j=8;j<10;++j)
				this.dummyImage2D.putValueInt(i, j, 2);
		for (int i=2;i<4;++i)
			for (int j=7;j<9;++j)
				this.dummyImage2D.putValueInt(i, j, 10);
		
		this.dummyImage3D = 
				MTBImage.createMTBImage(10, 10, 10, 1, 1, MTBImageType.MTB_BYTE);
		this.dummyImage3D.fillBlack();
		for (int z=0;z<2;++z)
			for (int y=3;y<6;++y)
				for (int x=1;x<3;++x)
					this.dummyImage3D.putValueInt(x,y,z,1);
		for (int z=8;z<10;++z)
			for (int y=8;y<10;++y)
				for (int x=8;x<10;++x)
					this.dummyImage3D.putValueInt(x,y,z,1);
		for (int z=4;z<6;++z)
			for (int y=2;y<4;++y)
				for (int x=7;x<9;++x)
					this.dummyImage3D.putValueInt(x,y,z,3);
		
		boolean thrown = false;
		try {
	    this.testObject = new SegmentationInitializer();
    } catch (ALDOperatorException e) {
    	thrown = true;
    }
		assertFalse(IDS + " init of initializer failed!", thrown);
	}
	
	/**
	 * Test segmentation object generation routines.
	 */
	@Test
	public void testGetInterfaceMethods() {
		
		// check the methods
		MTBSegmentationInterface result;
		MTBSegmentation2D result2D;
		MTBSegmentation3D result3D;
		
		/* 2D non-binary result given 2D image*/
		result = this.testObject.get2DInterface(this.dummyImage2D, false);
		assertTrue(IDS + " expecting 2D segmentation object, got something else...",
				result instanceof MTBSegmentation2D);
		result2D = (MTBSegmentation2D)result;
		assertTrue(IDS + " expecting dimension 2D, got something else!",
				result2D.getDimension().equals(SegmentationDimension.dim_2));
		assertTrue(IDS + " expecting width 10, got something else!",
				result2D.getSizeX() == 10);
		assertTrue(IDS + " expecting height 10, got something else!",
				result2D.getSizeY() == 10);
		assertTrue(IDS + " expecting 4 classes, got something else!",
				result2D.getNumberOfClasses() == 4);
		assertTrue(IDS + " expecting max. label 3, got " + result2D.getMaxLabel()
				+ "!", result2D.getMaxLabel() == 3);
		// check if all pixels are visible
		for (int y=0;y<10;++y)
			for (int x=0;x<10;++x)
				assertTrue(IDS + " expecting all pixels to be visible, (" 
						+ x + "," + y + ") is not visible!", result2D.isVisible(x, y));
		// check if all pixels have weight 1.0
		for (int y=0;y<10;++y)
			for (int x=0;x<10;++x)
				assertTrue(IDS + " expecting all pixels to have weight 1.0, (" 
						+ x + "," + y + ") is has not!", result2D.getWeight(x, y) == 1.0);
		// check class assignments 
		for (int i=3;i<6;++i)
			for (int j=1;j<3;++j)
				assertTrue(IDS + " expecting class 1, got " + result2D.getClass(i,j) +
						"!", result2D.getClass(i,j) == 1);
		for (int i=8;i<10;++i)
			for (int j=8;j<10;++j)
				assertTrue(IDS + " expecting class 2, got " + result2D.getClass(i,j) +
						"!", result2D.getClass(i,j) == 2);
		for (int i=2;i<4;++i)
			for (int j=7;j<9;++j)
				assertTrue(IDS + " expecting class 3, got " + result2D.getClass(i,j) +
						"!", result2D.getClass(i,j) == 3);
		
		/* 2D binary result given 2D image */
		result = this.testObject.get2DInterface(this.dummyImage2D, true);
		assertTrue(IDS + " expecting 2D segmentation object, got something else...",
				result instanceof MTBSegmentation2D);
		result2D = (MTBSegmentation2D)result;
		assertTrue(IDS + " expecting dimension 2D, got something else!",
				result2D.getDimension().equals(SegmentationDimension.dim_2));
		assertTrue(IDS + " expecting width 10, got something else!",
				result2D.getSizeX() == 10);
		assertTrue(IDS + " expecting height 10, got something else!",
				result2D.getSizeY() == 10);
		assertTrue(IDS + " expecting 2 classes, got something else!",
				result2D.getNumberOfClasses() == 2);
		assertTrue(IDS + " expecting max. label 1, got " + result2D.getMaxLabel()
				+ "!", result2D.getMaxLabel() == 1);
		// check if all pixels are visible
		for (int y=0;y<10;++y)
			for (int x=0;x<10;++x)
				assertTrue(IDS + " expecting all pixels to be visible, (" 
						+ x + "," + y + ") is not visible!", result2D.isVisible(x, y));
		// check if all pixels have weight 1.0
		for (int y=0;y<10;++y)
			for (int x=0;x<10;++x)
				assertTrue(IDS + " expecting all pixels to have weight 1.0, (" 
						+ x + "," + y + ") is has not!", result2D.getWeight(x, y) == 1.0);
		// check class assignments 
		for (int i=3;i<6;++i)
			for (int j=1;j<3;++j)
				assertTrue(IDS + " expecting class 1, got " + result2D.getClass(i,j) +
						"!", result2D.getClass(i,j) == 1);
		for (int i=8;i<10;++i)
			for (int j=8;j<10;++j)
				assertTrue(IDS + " expecting class 1, got " + result2D.getClass(i,j) +
						"!", result2D.getClass(i,j) == 1);
		for (int i=2;i<4;++i)
			for (int j=7;j<9;++j)
				assertTrue(IDS + " expecting class 1, got " + result2D.getClass(i,j) +
						"!", result2D.getClass(i,j) == 1);

		/* 3D non-binary result given 2D image */
		result = this.testObject.get3DInterface(this.dummyImage2D, false);
		assertTrue(IDS + " expecting 3D segmentation object, got something else...",
				result instanceof MTBSegmentation3D);
		result3D = (MTBSegmentation3D)result;
		assertTrue(IDS + " expecting dimension 3D, got something else!",
				result3D.getDimension().equals(SegmentationDimension.dim_3));
		assertTrue(IDS + " expecting width 10, got something else!",
				result3D.getSizeX() == 10);
		assertTrue(IDS + " expecting height 10, got something else!",
				result3D.getSizeY() == 10);
		assertTrue(IDS + " expecting depth 1, got something else!",
				result3D.getSizeZ() == 1);
		assertTrue(IDS + " expecting 4 classes, got something else!",
				result3D.getNumberOfClasses() == 4);
		assertTrue(IDS + " expecting max. label 3, got " + result3D.getMaxLabel()
				+ "!", result3D.getMaxLabel() == 3);
		// check if all pixels are visible
		for (int z=0;z<1;++z)
			for (int y=0;y<10;++y)
				for (int x=0;x<10;++x)
					assertTrue(IDS + " expecting all pixels to be visible, (" 
						+ x + "," + y + "," + z + ") is not visible!", 
						result3D.isVisible(x, y, z));
		// check if all pixels have weight 1.0
		for (int z=0;z<1;++z)
			for (int y=0;y<10;++y)
				for (int x=0;x<10;++x)
					assertTrue(IDS + " expecting all pixels to have weight 1.0, (" 
						+ x + "," + y + "," + z + ") is has not!", 
						result3D.getWeight(x, y, z) == 1.0);
		// check class assignments 
		for (int i=3;i<6;++i)
			for (int j=1;j<3;++j)
				assertTrue(IDS + " expecting class 1, got " + result3D.getClass(i,j,0)+
						"!", result3D.getClass(i,j,0) == 1);
		for (int i=8;i<10;++i)
			for (int j=8;j<10;++j)
				assertTrue(IDS + " expecting class 2, got " + result3D.getClass(i,j,0)+
						"!", result3D.getClass(i,j,0) == 2);
		for (int i=2;i<4;++i)
			for (int j=7;j<9;++j)
				assertTrue(IDS + " expecting class 3, got " + result3D.getClass(i,j,0)+
						"!", result3D.getClass(i,j,0) == 3);

		/* 3D binary result given 2D image */
		result = this.testObject.get3DInterface(this.dummyImage2D, true);
		assertTrue(IDS + " expecting 3D segmentation object, got something else...",
				result instanceof MTBSegmentation3D);
		result3D = (MTBSegmentation3D)result;
		assertTrue(IDS + " expecting dimension 3D, got something else!",
				result3D.getDimension().equals(SegmentationDimension.dim_3));
		assertTrue(IDS + " expecting width 10, got something else!",
				result3D.getSizeX() == 10);
		assertTrue(IDS + " expecting height 10, got something else!",
				result3D.getSizeY() == 10);
		assertTrue(IDS + " expecting depth 1, got something else!",
				result3D.getSizeZ() == 1);
		assertTrue(IDS + " expecting 2 classes, got something else!",
				result3D.getNumberOfClasses() == 2);
		assertTrue(IDS + " expecting max. label 1, got " + result3D.getMaxLabel()
				+ "!", result3D.getMaxLabel() == 1);
		// check if all pixels are visible
		for (int z=0;z<1;++z)
			for (int y=0;y<10;++y)
				for (int x=0;x<10;++x)
					assertTrue(IDS + " expecting all pixels to be visible, (" 
						+ x + "," + y + "," + z + ") is not visible!", 
						result3D.isVisible(x, y, z));
		// check if all pixels have weight 1.0
		for (int z=0;z<1;++z)
			for (int y=0;y<10;++y)
				for (int x=0;x<10;++x)
					assertTrue(IDS + " expecting all pixels to have weight 1.0, (" 
						+ x + "," + y + "," + z + ") is has not!", 
						result3D.getWeight(x, y, z) == 1.0);
		// check class assignments 
		for (int i=3;i<6;++i)
			for (int j=1;j<3;++j)
				assertTrue(IDS + " expecting class 1, got " + result3D.getClass(i,j,0)+
						"!", result3D.getClass(i,j,0) == 1);
		for (int i=8;i<10;++i)
			for (int j=8;j<10;++j)
				assertTrue(IDS + " expecting class 1, got " + result3D.getClass(i,j,0)+
						"!", result3D.getClass(i,j,0) == 1);
		for (int i=2;i<4;++i)
			for (int j=7;j<9;++j)
				assertTrue(IDS + " expecting class 1, got " + result3D.getClass(i,j,0)+
						"!", result3D.getClass(i,j,0) == 1);

		/* 2D non-binary result given 3D image */
		result = this.testObject.get2DInterface(this.dummyImage3D, false);
		assertTrue(IDS + " expecting 2D segmentation object, got something else...",
				result instanceof MTBSegmentation2D);
		result2D = (MTBSegmentation2D)result;
		assertTrue(IDS + " expecting dimension 2D, got something else!",
				result2D.getDimension().equals(SegmentationDimension.dim_2));
		assertTrue(IDS + " expecting width 10, got something else!",
				result2D.getSizeX() == 10);
		assertTrue(IDS + " expecting 2 classes, got something else!",
				result2D.getNumberOfClasses() == 2);
		assertTrue(IDS + " expecting max. label 1, got " + result2D.getMaxLabel()
				+ "!", result2D.getMaxLabel() == 1);
		// check if all pixels are visible
		for (int y=0;y<10;++y)
			for (int x=0;x<10;++x)
				assertTrue(IDS + " expecting all pixels to be visible, (" 
						+ x + "," + y + ") is not visible!", result2D.isVisible(x,y,0));
		// check if all pixels have weight 1.0
		for (int y=0;y<10;++y)
			for (int x=0;x<10;++x)
				assertTrue(IDS + " expecting all pixels to have weight 1.0, (" 
						+ x + "," + y + ") is has not!", result2D.getWeight(x,y,0) == 1.0);
		// check class assignments 
		for (int y=3;y<6;++y)
			for (int x=1;x<3;++x)
				assertTrue(IDS + " expecting class 1, got " + result2D.getClass(x, y)+
						"!", result2D.getClass(x,y) == 1);
		
		/* 2D binary result given 3D image */		
		result = this.testObject.get2DInterface(this.dummyImage3D, true);
		assertTrue(IDS + " expecting 2D segmentation object, got something else...",
				result instanceof MTBSegmentation2D);
		result2D = (MTBSegmentation2D)result;
		assertTrue(IDS + " expecting dimension 2D, got something else!",
				result2D.getDimension().equals(SegmentationDimension.dim_2));
		assertTrue(IDS + " expecting width 10, got something else!",
				result2D.getSizeX() == 10);
		assertTrue(IDS + " expecting 2 classes, got something else!",
				result2D.getNumberOfClasses() == 2);
		assertTrue(IDS + " expecting max. label 1, got " + result2D.getMaxLabel()
				+ "!", result2D.getMaxLabel() == 1);
		// check if all pixels are visible
		for (int y=0;y<10;++y)
			for (int x=0;x<10;++x)
				assertTrue(IDS + " expecting all pixels to be visible, (" 
						+ x + "," + y + ") is not visible!", result2D.isVisible(x,y,0));
		// check if all pixels have weight 1.0
		for (int y=0;y<10;++y)
			for (int x=0;x<10;++x)
				assertTrue(IDS + " expecting all pixels to have weight 1.0, (" 
						+ x + "," + y + ") is has not!", result2D.getWeight(x,y,0) == 1.0);
		// check class assignments 
		for (int y=3;y<6;++y)
			for (int x=1;x<3;++x)
				assertTrue(IDS + " expecting class 1, got " + result2D.getClass(x, y)+
						"!", result2D.getClass(x,y) == 1);

		/* 3D non-binary result given 3D image */
		result = this.testObject.get3DInterface(this.dummyImage3D, false);
		assertTrue(IDS + " expecting 3D segmentation object, got something else...",
				result instanceof MTBSegmentation3D);
		result3D = (MTBSegmentation3D)result;
		assertTrue(IDS + " expecting dimension 3D, got something else!",
				result3D.getDimension().equals(SegmentationDimension.dim_3));
		assertTrue(IDS + " expecting width 10, got something else!",
				result3D.getSizeX() == 10);
		assertTrue(IDS + " expecting height 10, got something else!",
				result3D.getSizeY() == 10);
		assertTrue(IDS + " expecting depth 10, got something else!",
				result3D.getSizeZ() == 10);
		assertTrue(IDS + " expecting 3 classes, got something else!",
				result3D.getNumberOfClasses() == 3);
		assertTrue(IDS + " expecting max. label 2, got " + result3D.getMaxLabel()
				+ "!", result3D.getMaxLabel() == 2);
		// check if all pixels are visible
		for (int z=0;z<10;++z)
			for (int y=0;y<10;++y)
				for (int x=0;x<10;++x)
					assertTrue(IDS + " expecting all pixels to be visible, (" 
						+ x + "," + y + "," + z + ") is not visible!", 
						result3D.isVisible(x, y, z));
		// check if all pixels have weight 1.0
		for (int z=0;z<10;++z)
			for (int y=0;y<10;++y)
				for (int x=0;x<10;++x)
					assertTrue(IDS + " expecting all pixels to have weight 1.0, (" 
						+ x + "," + y + "," + z + ") is has not!", 
						result3D.getWeight(x, y, z) == 1.0);
		// check class assignments 
		for (int z=0;z<2;++z)
			for (int y=3;y<6;++y)
				for (int x=1;x<3;++x)
					assertTrue(IDS + " expecting class 1, got " + 
						result3D.getClass(x,y,z) + 	"!", result3D.getClass(x,y,z) == 1);
		for (int z=8;z<10;++z)
			for (int y=8;y<10;++y)
				for (int x=8;x<10;++x)
					assertTrue(IDS + " expecting class 1, got " + 
						result3D.getClass(x,y,z) + "!", result3D.getClass(x,y,z) == 1);
		for (int z=4;z<6;++z)
			for (int y=2;y<4;++y)
				for (int x=7;x<9;++x)
					assertTrue(IDS + " expecting class 2, got " + 
						result3D.getClass(x,y,z) + "!", result3D.getClass(x,y,z) == 2);

		/* 3D binary result given 3D image */
		result = this.testObject.get3DInterface(this.dummyImage3D, true);
		assertTrue(IDS + " expecting 3D segmentation object, got something else...",
				result instanceof MTBSegmentation3D);
		result3D = (MTBSegmentation3D)result;
		assertTrue(IDS + " expecting dimension 3D, got something else!",
				result3D.getDimension().equals(SegmentationDimension.dim_3));
		assertTrue(IDS + " expecting width 10, got something else!",
				result3D.getSizeX() == 10);
		assertTrue(IDS + " expecting height 10, got something else!",
				result3D.getSizeY() == 10);
		assertTrue(IDS + " expecting depth 10, got something else!",
				result3D.getSizeZ() == 10);
		assertTrue(IDS + " expecting 2 classes, got " + 
				result3D.getNumberOfClasses() + "!",
				result3D.getNumberOfClasses() == 2);
		assertTrue(IDS + " expecting max. label 1, got " + result3D.getMaxLabel()
				+ "!", result3D.getMaxLabel() == 1);
		// check if all pixels are visible
		for (int z=0;z<10;++z)
			for (int y=0;y<10;++y)
				for (int x=0;x<10;++x)
					assertTrue(IDS + " expecting all pixels to be visible, (" 
						+ x + "," + y + "," + z + ") is not visible!", 
						result3D.isVisible(x, y, z));
		// check if all pixels have weight 1.0
		for (int z=0;z<10;++z)
			for (int y=0;y<10;++y)
				for (int x=0;x<10;++x)
					assertTrue(IDS + " expecting all pixels to have weight 1.0, (" 
						+ x + "," + y + "," + z + ") is has not!", 
						result3D.getWeight(x, y, z) == 1.0);
		// check class assignments 
		for (int z=0;z<2;++z)
			for (int y=3;y<6;++y)
				for (int x=1;x<3;++x)
					assertTrue(IDS + " expecting class 1, got " + 
						result3D.getClass(x,y,z) + 	"!", result3D.getClass(x,y,z) == 1);
		for (int z=8;z<10;++z)
			for (int y=8;y<10;++y)
				for (int x=8;x<10;++x)
					assertTrue(IDS + " expecting class 1, got " + 
						result3D.getClass(x,y,z) + "!", result3D.getClass(x,y,z) == 1);
		for (int z=4;z<6;++z)
			for (int y=2;y<4;++y)
				for (int x=7;x<9;++x)
					assertTrue(IDS + " expecting class 1, got " + 
						result3D.getClass(x,y,z) + "!", result3D.getClass(x,y,z) == 1);	
	}
}