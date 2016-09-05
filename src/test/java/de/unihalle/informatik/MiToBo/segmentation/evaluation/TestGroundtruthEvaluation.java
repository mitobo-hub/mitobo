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

package de.unihalle.informatik.MiToBo.segmentation.evaluation;

import static org.junit.Assert.*;

import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;

import de.unihalle.informatik.Alida.exceptions.ALDException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.gui.MTBTableModel;
import de.unihalle.informatik.MiToBo.io.images.ImageReaderMTB;
import de.unihalle.informatik.MiToBo.segmentation.helpers.LabelImageConverter;

/**
 * Test class for operator 
 * {@link de.unihalle.informatik.MiToBo.segmentation.evaluation.GroundtruthEvaluation}.
 * 
 * @author moeller
 */
public class TestGroundtruthEvaluation {

	/*
	 * Test image and segmentation results are taken from
	 * 
	 * /vol/daten/microscope/cellAnalysis_MIAAB_11/data_ICPR_10/test_24h_5.tif
	 * 
	 * (local versions are stored as
	 *   ./src/test/resources/images/testSegmentationGT.tif
	 *   ./src/test/resources/images/testSegmentationAuto.tif)
	 */
	
	/**
	 * Groundtruth label image in color.
	 */
	private MTBImageRGB groundtruthSegColor;
	
	/**
	 * Groundtruth label image in grayscale.
	 */
	private MTBImageShort groundtruthSegGray;

	/**
	 * Grayscale label image of automatic segmentation result.
	 */
	private MTBImageShort autoSeg;

	/*
	 * groundtruth data
	 */
	
	/**
	 * Set of labels to be found in groundtruth label image.
	 */
	private TreeSet<Integer> groundtruthLabels;

	/**
	 * Set of labels to be found in segmentation label image.
	 */
	private TreeSet<Integer> segmentationLabels;

	/**
	 * Sizes of groundtruth regions.
	 */
//	private int [] groundtruthRegionSizes = new int[]{
//			45147, 55954, 18616, 12412, 13128, 6850,
//			62638, 12722, 39461, 8765, 35162, 41940,
//			34719, 39577, 22360, 41748, 41462, 33316,
//			4236, 29046, 34376, 38422, 14460, 7620};

	/**
	 * Sizes of segmented regions.
	 */
//	private int [] segmentationRegionSizes = new int[]{
//		10475, 10735, 28854, 44915, 56750, 3774,
//		39548, 17945, 41314,	9572, 30666, 31011,
//		29965, 49570, 33620, 21613, 48744, 12932,
//		32375, 16954, 29399, 34797, 18739, 4253};
	
	/**
	 * TPs for each groundtruth region.
	 */
	private int [] groundtruthTP = new int[]{
			41584, 52205, 8383, 12249, 10475, 3474, 
			38240, 9440, 33653, 5929, 26589, 28895, 
			24804, 31330, 18314, 40809, 36346, 30043, 
			4236, 16892, 28605, 33402, 14340, 4191};

	/**
	 * FNs for each groundtruth region.
	 */
	private int [] groundtruthFN = new int[]{
			3563, 3749, 10233, 163, 2653, 3376, 
			24398, 3282, 5808, 2836, 8573, 13045,
			9915, 8247, 4046, 939, 5116, 3273, 
			0, 12154, 5771, 5020, 120, 3429};

	/**
	 * FPs for each groundtruth region.
	 */
	private int [] groundtruthFP = new int[]{
			3331, 4545, 2352, 16605, 0, 300, 
			1308, 132, 7661, 12016, 4422, 1771, 
			5161, 2290, 3299, 8761, 12398, 2332, 
			8696, 62, 794, 1395, 4399, 62};

	/**
	 * Recalls for each groundtruth region.
	 */
	private double [] groundtruthRecalls = new double[]{
			0.9210800, 0.9329985, 0.4503116, 0.9868675, 0.7979129, 0.5071533,
			0.6104920, 0.7420217, 0.8528167, 0.6764404, 0.7561857, 0.6889604,
			0.7144215, 0.7916214, 0.8190519, 0.9775079, 0.8766099, 0.9017589,
			1.0000000, 0.5815603, 0.8321212, 0.8693457, 0.9917012, 0.5500000};

	/**
	 * Precisions for each groundtruth region.
	 */
	private double [] groundtruthPrecisions = new double[]{
			 0.9258377, 0.9199119, 0.7809036, 0.4245165, 1.0000000, 0.9205087,
			 0.9669263, 0.9862098, 0.8145665, 0.3303984, 0.8574054, 0.9422487,
			 0.8277657, 0.9318858, 0.8473604, 0.8232600, 0.7456507, 0.9279691,
			 0.3275595, 0.9963430, 0.9729923, 0.9599103, 0.7652489, 0.9854221};

	/**
	 * Expected matching matrix.
	 */
	private byte [][] matchingMatrix = new byte[24][24];
	
	/**
	 * Operator to test.
	 */
	private GroundtruthEvaluation compareOp = null;
	
	/**
	 * Fixture.
	 */
	@Before
	public void initTestClass() {
		
		// read both label images
		try {
	    ImageReaderMTB reader = 
	    	new ImageReaderMTB("target/test-classes/images/testSegmentationGT.tif");
	    reader.runOp(true);
	    this.groundtruthSegColor = (MTBImageRGB)reader.getResultMTBImage();
	    reader = 
	    	new ImageReaderMTB("target/test-classes/images/testSegmentationAuto.tif");
	    reader.runOp(true);
	    this.autoSeg = 
	    	(MTBImageShort)(reader.getResultMTBImage()).convertType(
	    																				MTBImageType.MTB_SHORT, true);
    } catch (Exception e) {
	    e.printStackTrace();
    }
    
		// convert RGB to gray-scale with unique labels
    try {
	    LabelImageConverter convOp = 
	    	new LabelImageConverter(this.groundtruthSegColor);
	    convOp.runOp(true);
	    this.groundtruthSegGray = convOp.getResultLabelImage();
    } catch (ALDException e1) {
	    e1.printStackTrace();
    }

    // groundtruth labels, background with label zero is ignored!
    this.groundtruthLabels = new TreeSet<Integer>();
    this.segmentationLabels = new TreeSet<Integer>();
    for (int i=1; i<=24; ++i) {
    	this.groundtruthLabels.add(new Integer(i));
    	this.segmentationLabels.add(new Integer(i*10));
    }
    
    // matching matrix
    for (int r=0;r<24;++r)
    	for (int c=0;c<24;++c)
    		this.matchingMatrix[r][c] = 0;
    this.matchingMatrix[0][3] = 1;
    this.matchingMatrix[1][4] = 1;
    this.matchingMatrix[2][1] = 1;
    this.matchingMatrix[3][2] = 1;
    this.matchingMatrix[4][0] = 1;
    this.matchingMatrix[5][5] = 1;
    this.matchingMatrix[6][6] = 1;
    this.matchingMatrix[7][9] = 1;
    this.matchingMatrix[8][8] = 1;
    this.matchingMatrix[9][7] = 1;
    this.matchingMatrix[10][11] = 1;
    this.matchingMatrix[11][10] = 1;
    this.matchingMatrix[12][12] = 1;
    this.matchingMatrix[13][14] = 1;
    this.matchingMatrix[14][15] = 1;
    this.matchingMatrix[15][13] = 1;
    this.matchingMatrix[16][16] = 1;
    this.matchingMatrix[17][18] = 1;
    this.matchingMatrix[18][17] = 1;
    this.matchingMatrix[19][19] = 1;
    this.matchingMatrix[20][20] = 1;
    this.matchingMatrix[21][21] = 1;
    this.matchingMatrix[22][22] = 1;
    this.matchingMatrix[23][23] = 1;
    
    // init operator
  	try {
	    this.compareOp = 
	    	new GroundtruthEvaluation(this.autoSeg, this.groundtruthSegGray, 
	    			new Boolean(false));
    } catch (ALDOperatorException e) {
	    e.printStackTrace();
    }
	}

	/**
	 * Test pairwise region matching and extraction of quality measures.
	 * 
	 * @throws ALDOperatorException				Thrown in case of failure.
	 * @throws ALDProcessingDAGException	Thrown in case of failure.
	 */
	@Test
	public void testDoEvaluation() 
		throws ALDOperatorException, ALDProcessingDAGException {
		
		// run the operator
		this.compareOp.runOp(true);

		// check matching matrix
		for (int r=0;r<24;++r) {
			for (int c=0;c<24;++c) {
				assertTrue(   this.matchingMatrix[r][c] 
                   == this.compareOp.matchingMatrix[r][c]);
			}
		}	

		// check the returned data object 
		MTBTableModel resultTable = this.compareOp.getResultTable();
		for (int i=1;i<24;++i) {
			assertTrue(this.groundtruthFN[i-1]
			     == ((Double)resultTable.getValueAt(i-1, 13)).intValue());
			assertTrue(this.groundtruthFP[i-1]
			     == ((Double)resultTable.getValueAt(i-1, 12)).intValue());
			assertTrue(this.groundtruthTP[i-1]
			     == ((Double)resultTable.getValueAt(i-1, 11)).intValue());
			assertTrue(  Math.abs(this.groundtruthRecalls[i-1] 
					       - ((Double)resultTable.getValueAt(i-1, 9)).doubleValue()) 
					< 0.000001);
			assertTrue(  Math.abs(this.groundtruthPrecisions[i-1] 
								- ((Double)resultTable.getValueAt(i-1, 10)).doubleValue()) 
			    < 0.000001);
		}
	}
}
