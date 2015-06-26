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

import java.util.Vector;

import org.junit.Before;
import org.junit.Test;

import Jama.Matrix;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBPolygon2DSet;
import de.unihalle.informatik.MiToBo.segmentation.activecontours.exceptions.MTBSnakeException;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSet_SnakeEnergyDerivable;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSnake;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSnakePoint2D;
import de.unihalle.informatik.MiToBo.segmentation.snakes.energies.paramAdapt.MTBSnakeEnergyCD_KassLength_ParamAdaptNone;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.SnakeOptimizerSingle.EnergyNormalizationMode;

/**
 * JUnit test class for {@link MTBSnakeEnergyCD_KassLength}.
 * 
 * @author moeller
 */
public class TestMTBSnakeEnergyCD_KassLength {

	private static final String IDS = "[TestMTBSnakeEnergyCD_KassLength]";
	
	private static final double accuracy = 10e-10;
	
	private MTBSnake testSnake;
	
	/**
	 * Fixture.
	 */
	@Before
	public void initTestClass() {
		Vector<MTBSnakePoint2D> spoints = new Vector<MTBSnakePoint2D>();
		spoints.add(new MTBSnakePoint2D(2.0, 1.0));
		spoints.add(new MTBSnakePoint2D(3.0, 1.0));
		spoints.add(new MTBSnakePoint2D(4.0, 2.0));
		spoints.add(new MTBSnakePoint2D(4.0, 3.0));
		spoints.add(new MTBSnakePoint2D(3.0, 4.0));
		spoints.add(new MTBSnakePoint2D(2.0, 4.0));
		spoints.add(new MTBSnakePoint2D(1.0, 3.0));
		spoints.add(new MTBSnakePoint2D(1.0, 2.0));
		this.testSnake = new MTBSnake(spoints, true);
	}
	
	/**
	 * Test methods.
	 */
	@Test
	public void testMTBSnakeEnergyCD_KassLength() {
		MTBSnakeEnergyCD_KassLength energy = new MTBSnakeEnergyCD_KassLength(2.0, 
				new MTBSnakeEnergyCD_KassLength_ParamAdaptNone());
		assertTrue(IDS + " initial alpha should be 2.0, it's " 
			+ energy.getInitAlpha(), Math.abs(energy.getInitAlpha() - 2.0)<accuracy);
		
		// init energy
		boolean exceptionThrown = false;
    try {
  		MockSnakeOptimizerSingleVarCalc opt = new MockSnakeOptimizerSingleVarCalc();
    	MTBPolygon2DSet inSnakes = new MTBPolygon2DSet();
    	inSnakes.add(this.testSnake);
    	opt.setInitialSnakes(inSnakes);
    	opt.setNormalizationMode(EnergyNormalizationMode.NORM_NONE);
    	MTBSet_SnakeEnergyDerivable eSet = new MTBSet_SnakeEnergyDerivable();
    	eSet.addEnergy(energy);
    	opt.setEnergySet(eSet);
    	try {
	      opt.initOptimizer();
      } catch (MTBSnakeException e) {
      	exceptionThrown = true;
      }
    	double snakeEnergy = energy.calcEnergy(opt);
    	assertTrue(IDS + " length energy should be 12.0, is " + snakeEnergy,
    		Math.abs(snakeEnergy - 12.0) < accuracy);
    	Matrix A = energy.getDerivative_MatrixPart(opt);
    	assertTrue(IDS + " expecting A having 16 rows...", 
    		A.getRowDimension() == 16);
    	assertTrue(IDS + " expecting A having 16 columns...", 
      	A.getColumnDimension() == 16);
    	
    	/*
    	 * upper left block
    	 */
    	
    	// first row
   		assertTrue(IDS + " entry ( 0, 0 ) should be 4.0...", 
   			Math.abs(A.get(0, 0) - 4.0) < accuracy); 
   		assertTrue(IDS + " entry ( 0, 1 ) should be -2.0...", 
   			Math.abs(A.get(0, 1) - -2.0) < accuracy);
  		assertTrue(IDS + " entry ( 0, 7 ) should be -2.0, it's "	+ A.get(0, 7) 
  			+ "...", Math.abs(A.get(0, 7) - -2.0) < accuracy); 
   		// rows 1 to 6
    	for (int row = 1; row < 7; ++row) {
    		assertTrue(IDS + " entry (" +row+ "," +(row-1)+ ") should be -2.0...", 
    			Math.abs(A.get(row, row-1) - -2.0) < accuracy); 
    		assertTrue(IDS + " entry (" +row+ "," +(row)+ ") should be 4.0...", 
     			Math.abs(A.get(row, row) - 4.0) < accuracy); 
    		assertTrue(IDS + " entry (" +row+ "," +(row+1)+ ") should be -2.0...", 
     			Math.abs(A.get(row, row+1) - -2.0) < accuracy); 
    	}
    	// last row of upper block
  		assertTrue(IDS + " entry ( 7, 0 ) should be -2.0...", 
   			Math.abs(A.get(7, 0) - -2.0) < accuracy); 
   		assertTrue(IDS + " entry ( 7, 6 ) should be -2.0...", 
     			Math.abs(A.get(7, 6) - -2.0) < accuracy);
   		assertTrue(IDS + " entry ( 7, 7 ) should be 4.0...", 
   			Math.abs(A.get(7, 7) - 4.0) < accuracy); 
   		
    	/*
    	 * lower right block
    	 */
    	
    	// first row (row 8)
   		assertTrue(IDS + " entry ( 8, 8 ) should be 4.0...", 
   			Math.abs(A.get(8, 8) - 4.0) < accuracy); 
   		assertTrue(IDS + " entry ( 8, 9 ) should be -2.0...", 
   			Math.abs(A.get(8, 9) - -2.0) < accuracy);
  		assertTrue(IDS + " entry ( 8, 15 ) should be -2.0, it's "	+ A.get(8, 15) 
  			+ "...", Math.abs(A.get(8, 15) - -2.0) < accuracy); 
   		// rows 9 to 14
    	for (int row = 9; row < 15; ++row) {
    		assertTrue(IDS + " entry (" +row+ "," +(row-1)+ ") should be -2.0...", 
    			Math.abs(A.get(row, row-1) - -2.0) < accuracy); 
    		assertTrue(IDS + " entry (" +row+ "," +(row)+ ") should be 4.0...", 
     			Math.abs(A.get(row, row) - 4.0) < accuracy); 
    		assertTrue(IDS + " entry (" +row+ "," +(row+1)+ ") should be -2.0...", 
     			Math.abs(A.get(row, row+1) - -2.0) < accuracy); 
    	}
    	// last row of lower block
  		assertTrue(IDS + " entry ( 15, 8 ) should be -2.0...", 
   			Math.abs(A.get(15, 8) - -2.0) < accuracy); 
   		assertTrue(IDS + " entry ( 15, 14 ) should be -2.0...", 
     			Math.abs(A.get(15, 14) - -2.0) < accuracy);
   		assertTrue(IDS + " entry ( 15, 15 ) should be 4.0...", 
   			Math.abs(A.get(15, 15) - 4.0) < accuracy); 

    } catch (ALDOperatorException e) {
    	exceptionThrown = true;
    }
    assertFalse(IDS + " got an exception during testing the energy!!!",
    	exceptionThrown);
	}
}