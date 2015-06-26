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

package de.unihalle.informatik.MiToBo.segmentation.snakes.energies;

import static org.junit.Assert.*;

import java.awt.geom.Point2D;
import java.util.Vector;

import org.junit.Before;
import org.junit.Test;

import Jama.Matrix;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBPolygon2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSet_SnakeEnergyDerivable;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSnake;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSnakePoint2D;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.SnakeOptimizerSingle.EnergyNormalizationMode;

/**
 * JUnit test class for {@link MTBSnakeEnergyCD_CVRegionFit}.
 * <p>
 * Please note that this test class is restricted to testing the energy on 
 * single-channel images although the energy itself is capable of also 
 * handling multi-channel images.
 * 
 * @author moeller
 */
public class TestMTBSnakeEnergyCD_CVRegionFit {

	private static final String IDS = "[TestMTBSnakeEnergyCD_CVRegionFit]";
	
	private static final double accuracy = 10e-5;
	
	private MTBSnake testSnake;
	
	private MTBImageByte testImg;
	
	/**
	 * Fixture.
	 */
	@Before
	public void initTestClass() {
		Vector<MTBSnakePoint2D> spoints = new Vector<MTBSnakePoint2D>();
		spoints.add(new MTBSnakePoint2D(1.0, 1.0));
		spoints.add(new MTBSnakePoint2D(2.0, 1.0));
		spoints.add(new MTBSnakePoint2D(3.0, 1.0));
		spoints.add(new MTBSnakePoint2D(4.0, 1.0));
		spoints.add(new MTBSnakePoint2D(5.0, 1.0));
		spoints.add(new MTBSnakePoint2D(5.0, 2.0));
		spoints.add(new MTBSnakePoint2D(5.0, 3.0));
		spoints.add(new MTBSnakePoint2D(5.0, 4.0));
		spoints.add(new MTBSnakePoint2D(5.0, 5.0));
		spoints.add(new MTBSnakePoint2D(4.0, 5.0));
		spoints.add(new MTBSnakePoint2D(3.0, 5.0));
		spoints.add(new MTBSnakePoint2D(2.0, 5.0));
		spoints.add(new MTBSnakePoint2D(1.0, 5.0));
		spoints.add(new MTBSnakePoint2D(1.0, 4.0));
		spoints.add(new MTBSnakePoint2D(1.0, 3.0));
		spoints.add(new MTBSnakePoint2D(1.0, 2.0));
		this.testSnake = new MTBSnake(spoints, true);
		
		this.testImg = (MTBImageByte)MTBImage.createMTBImage(
				6, 6, 1, 1, 1, MTBImageType.MTB_BYTE);
		for (int y=0; y<4; ++y) {
			for (int x=0; x<4; ++x) {
				this.testImg.putValueInt(x, y, 0);
			}
			for (int x=4; x<6; ++x) {
				this.testImg.putValueInt(x, y, 255);
			}
		}
		for (int y=4; y<6; ++y) {
			for (int x=0; x<6; ++x) {
				this.testImg.putValueInt(x, y, 255);
			}
		}
	}
	
	/**
	 * Test methods.
	 */
	@Test
	public void testMTBSnakeEnergyCD_CVRegionFit() {
		MTBSnakeEnergyCD_CVRegionFit energy = new MTBSnakeEnergyCD_CVRegionFit(
			this.testImg, new double[]{0.25}, new double[]{0.75});
		
		// init energy
		boolean exceptionThrown = false;
    try {
  		MockSnakeOptimizerSingleVarCalc opt = 
  			new MockSnakeOptimizerSingleVarCalc();
    	MTBPolygon2DSet inSnakes = new MTBPolygon2DSet();
    	inSnakes.add(this.testSnake);
    	opt.setInitialSnakes(inSnakes);
    	opt.setNormalizationMode(EnergyNormalizationMode.NORM_NONE);
    	MTBSet_SnakeEnergyDerivable eSet = new MTBSet_SnakeEnergyDerivable();
    	eSet.addEnergy(energy);
    	opt.setEnergySet(eSet);
    	opt.initOptimizer();
    	energy.updateStatus(opt);
    	double snakeEnergy = energy.calcEnergy(opt);
    	
    	assertTrue(IDS + " length energy should be 285906.7969, is "+snakeEnergy,
    		Math.abs(snakeEnergy - 285906.7969) < accuracy);
    	Matrix A = energy.getDerivative_MatrixPart(opt);
    	assertTrue(IDS + " expecting A having 32 rows...", 
    		A.getRowDimension() == 32);
    	assertTrue(IDS + " expecting A having 32 columns...", 
      	A.getColumnDimension() == 32);
    	
    	/*
    	 * upper left block
    	 */
    	
   		// rows 0 to 14
    	double valBlack = -17493.24902;
    	double valWhite = -830.59277;
  		double targetVal, targetValNeg;
  		Point2D.Double p;
  		for (int row = 0; row < 14; ++row) {
    		int col = row + 16;
    		p = this.testSnake.getPoints().get(row);
    		if (this.testImg.getValueInt((int)p.x, (int)p.y) == 0) 
    			targetVal = valBlack;
    		else
    			targetVal = valWhite;
  			targetValNeg = -targetVal;
    		assertTrue(IDS + " matrix entry (" +row+ "," + col + ") " 
    			+ "should be " + targetValNeg + " , but is " +A.get(row, col)+ "...", 
    				Math.abs(A.get(row, col) - targetValNeg) < accuracy); 
    		assertTrue(IDS + " entry (" +row+ "," +(col+1)+ ") " 
     			+ "should be " + targetVal + "...", 
     				Math.abs(A.get(row, col+1) - targetVal) < accuracy); 
    	}
    	// last row of upper block
  		p = this.testSnake.getPoints().get(15);
  		if (this.testImg.getValueInt((int)p.x, (int)p.y) == 0) 
  			targetVal = valBlack;
  		else
  			targetVal = valWhite;
			targetValNeg = -targetVal;
  		assertTrue(IDS + " matrix entry (" + 15 + "," + 31 + ") " 
  			+ "should be " + targetValNeg + " , but is " + A.get(15, 31) + "...", 
  				Math.abs(A.get(15, 31) - targetValNeg) < accuracy); 
  		assertTrue(IDS + " entry (" + 15 + "," + 16 + ") " 
   			+ "should be " + targetVal + "...", 
   				Math.abs(A.get(15, 16) - targetVal) < accuracy); 
   		
    	/*
    	 * lower right block
    	 */
    	
   		// rows 16 to 30
    	for (int row = 16; row < 30; ++row) {
    		int col = row - 16;
    		p = this.testSnake.getPoints().get(row - 16);
    		if (this.testImg.getValueInt((int)p.x, (int)p.y) == 0) 
    			targetVal = valBlack;
    		else
    			targetVal = valWhite;
  			targetValNeg = -targetVal;
    		assertTrue(IDS + " matrix entry (" + row + "," + col + ") " 
    			+ "should be " + targetVal + " , but is " +A.get(row, col) + "...", 
    				Math.abs(A.get(row, col) - targetVal) < accuracy); 
    		assertTrue(IDS + " entry (" + row + "," + (col+1) + ") " 
     			+ "should be " + targetValNeg + "...", 
     				Math.abs(A.get(row, col+1) - targetValNeg) < accuracy); 
    	}
    	// last row of upper block
  		p = this.testSnake.getPoints().get(15);
  		if (this.testImg.getValueInt((int)p.x, (int)p.y) == 0) 
  			targetVal = valBlack;
  		else
  			targetVal = valWhite;
			targetValNeg = -targetVal;
  		assertTrue(IDS + " matrix entry (" + 31 + "," + 15 + ") " 
  			+ "should be " + targetVal + " , but is " +A.get(31, 15) + "...", 
  				Math.abs(A.get(31, 15) - targetVal) < accuracy); 
  		assertTrue(IDS + " entry (" + 31 + "," + 0 + ") " 
   			+ "should be " + targetValNeg + "...", 
   				Math.abs(A.get(31, 0) - targetValNeg) < accuracy); 

    } catch (Exception e) {
    	exceptionThrown = true;
    }
    assertFalse(IDS + " got an exception during testing the energy!!!",
    	exceptionThrown);
	}
}