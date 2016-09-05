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

import java.awt.Point;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.datatypes.ALDFileString;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageRGB;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageShort;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.gui.MTBTableModel;
import de.unihalle.informatik.MiToBo.math.optimization.MatchingBipartite_HungarianAlgorithm;
import de.unihalle.informatik.MiToBo.math.optimization.MatchingBipartite_HungarianAlgorithm.ScoreInterpretation;
import de.unihalle.informatik.MiToBo.segmentation.evaluation.measures.*;
import de.unihalle.informatik.MiToBo.segmentation.helpers.LabelImageConverter;

/**
 * Implementation of segmentation evaluation measures.
 * <p>
 * The operator takes a segmentation image and a ground truth image. 
 * It evaluates both segmentations based on region and/or contour information
 * <p>
 * Both images have to be region images and have to have the same size.
 * 
 * @author Birgit Moeller
 * @author Felix Knispel
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,
        level=Level.APPLICATION)
public class GroundtruthEvaluation extends MTBOperator {

	/**
	 * Groundtruth labeling.
	 */
	@Parameter( label= "Groundtruth Label Image", required = true,
		direction=Parameter.Direction.IN, dataIOOrder = 0,
		description = "Groundtruth label image.")
	protected transient MTBImage gtLabelsOrig = null;

	/**
	 * Label image of segmentation result.
	 */
	@Parameter( label= "Segmentation Label Image", required = true,
		direction=Parameter.Direction.IN, dataIOOrder = 1,
		description = "Segmentation result label image.")
	protected transient MTBImage segLabelsOrig = null;

	/**
	 * Table with result data.
	 */
	@Parameter(label="Result data table",   direction=Parameter.Direction.OUT,
		supplemental = true, description = "Result data table")
	protected MTBTableModel resultTable = null;

	/**
	 * Info string to structure GUI.
	 */
	@Parameter(label= "Measure info string", required = true, dataIOOrder = 2,
		direction=Parameter.Direction.IN, description = "Info string.",	info= true)
	protected String measureInfo = "Measures to calculate:";

	/**
	 * Check if you want to compute Recall and Precision
	 */
	@Parameter( label= "Recalls, Precisions, F1-Scores and Jaccard Indices",
		required = true, dataIOOrder = 3, direction=Parameter.Direction.IN,
		description = 
			"Calculate recalls, precisions, F1-scores and Jaccard indices.")
	private boolean GTC = true;

	/**
	 * Check if you want to compute Pratt's figure of merit
	 */
	@Parameter( label= "Pratt's Figure of Merit", required = true,
		dataIOOrder = 4, direction=Parameter.Direction.IN,
		description = "Calculate Pratt's figure of merit.")
	private boolean PRA = false;

	/**
	 * Check if you want to compute the Hausdorff-Distance
	 */
	@Parameter( label= "Hausdorff Distance", required = true, dataIOOrder = 5,
		direction=Parameter.Direction.IN,
		description = "Calculate Hausdorff distance.")
	private boolean HAU = false;

	/**
	 * Check if you want to compute the Detection Errors
	 */
	@Parameter( label= "Detection Errors", required = true, dataIOOrder = 6,
		direction=Parameter.Direction.IN, 
		description = "Calculate Detection Errors.")
	private boolean DE = false;

	/**
	 * Check if you want to compute Odet's criteria
	 */
	@Parameter( label= "Odet's criteria", required = true, dataIOOrder = 7,
		direction=Parameter.Direction.IN,
		description = "Calculate Odet's criteria.")
	private boolean Odet = false;

	/**
	 * For Odet's criteria only
	 */
	@Parameter(label= "Odet's criteria exponent", required = true,
		dataIOOrder = 8, direction = Parameter.Direction.IN,
		description = "Exponent for calculating Odet's criterias.")
	protected int n = 1;

	/**
	 * Flag to invert ground-truth image prior to evaluation.
	 */
	@Parameter( label= "Invert groundtruth image", required = false,
		direction=Parameter.Direction.IN, dataIOOrder = 1,
		description = "Apply inversion to groundtruth image.")
	private Boolean invertGroundtrouth = new Boolean(false);

	/**
	 * Flag to invert input image.
	 */
	@Parameter( label= "Invert input image", required = false,
		direction=Parameter.Direction.IN, dataIOOrder = 2,
		description = "Apply inversion to input image.")
	private Boolean invertInputImage = new Boolean(false);

	/**
	 * Flag to omit region assignment.
	 */
	@Parameter( label= "Automatic region assignment off", required = false,
		direction=Parameter.Direction.IN, dataIOOrder = 3,
		description = "No automatic region assignment.")
	private Boolean omitAssignment = new Boolean(false);

	/**
	 * Optional file where to save the result table.
	 */
	@Parameter(label = "(Optional) result file", required = false,
		direction = Parameter.Direction.IN, dataIOOrder = 4)
	private ALDFileString resultFile = null;

	/**
	 * Width of label images.
	 */
	protected int width;

	/**
	 * Height of label images.
	 */
	protected int height;

	/**
	 * Preprocessed groundtruth label image with short pixel type.
	 */
	protected MTBImageShort gtLabelImage;

	/**
	 * Preprocessed segmentation label image with short pixel type.
	 */
	protected MTBImageShort segLabelImage;

	/**
	 * Set of available groundtruth labels.
	 */
	protected TreeSet<Integer> labelSetGT = null;

	/**
	 * Set of available segmentation labels.
	 */
	protected TreeSet<Integer> labelSetSG = null;

	/**
	 * List of groundtruth region labels.
	 */
	private ArrayList<Integer> labelListGT;
	
	/**
	 * List of labels found in segmentation.
	 */
	private ArrayList<Integer> labelListSG;
	
	/**
	 * Match list of set entries to original groundtruth labels.
	 */
	protected int [] gtIDs;

	/**
	 * Match list of set entries to original segmentation labels.
	 */
	protected int [] sgIDs;

	/**
	 * Array of sizes of groundtruth regions.
	 */
	protected HashMap<Integer, Integer> sizesGT = null;

	/**
	 * Array of sizes of segmented regions.
	 */
	protected HashMap<Integer, Integer> sizesSG	= null;
	/**
	 * Minimum number of regions in groundtruth or segmented region set.
	 */
	protected int minRegionCount = 0;

	/**
	 * Maximum number of regions in groundtruth or segmented region set.
	 */
	protected int maxRegionCount = 0;

	/**
	 * Score matrix with pairwise overlaps.
	 */
	protected double [][] scoreMatrix = null;

	/**
	 * Matching matrix.
	 */
	protected byte [][] matchingMatrix = null;


	/**
	 * Default constructor.
	 * @throws ALDOperatorException	Thrown in case of failure.
	 */
	public GroundtruthEvaluation() throws ALDOperatorException {
		// nothing to do here
	}

	/**
	 * Default constructor with parameters.
	 *
	 * @param _segLabels		Label image of segmentation result.
	 * @param _gtLabels			Label image of groundtruth segmentation.
	 * @param noAssignment	Flag, set true if label images imply assignment.
	 * @throws ALDOperatorException Thrown in case of failure.
	 */
	public GroundtruthEvaluation(MTBImage _segLabels,	MTBImage _gtLabels,
			Boolean noAssignment) throws ALDOperatorException {
		this.gtLabelsOrig = _gtLabels;
		this.segLabelsOrig = _segLabels;
		this.omitAssignment = noAssignment;
	}

	/**
	 * Get evaluation results.
	 * @return	Table with evaluation results.
	 */
	public MTBTableModel getResultTable() {
		return this.resultTable;
	}

	@Override
	protected void operate() throws ALDOperatorException {
		try {
			// initialize some member variables
			this.width = this.segLabelsOrig.getSizeX();
			this.height = this.segLabelsOrig.getSizeY();

			// convert images to short images if necessary
			this.convertLabelImages();

			// extract statistical data about regions
			this.extractLabelsSizesOverlaps();

			// if not disabled, extract region mappings GT <---> Seg
			if (this.omitAssignment.booleanValue()) {
				// labels induce an assignment, i.e. identical labels coincide
				this.matchingMatrix = new byte[this.maxRegionCount][this.maxRegionCount];
				for (int r=0;r<this.maxRegionCount;++r)
					for (int c=0;c<this.maxRegionCount;++c)
						this.matchingMatrix[r][c] = 0;
				// fill diagonal with ones, but consider unmatched regions
				for (int d=0; d<this.minRegionCount;++d)
					this.matchingMatrix[d][d] = 1;
			}
			// if labels do not induce a matching, calculate one
			else {
				// apply Hungarian algorithm to score matrix
				MatchingBipartite_HungarianAlgorithm matcher =
						new MatchingBipartite_HungarianAlgorithm(this.scoreMatrix,
								ScoreInterpretation.MAXIMUM_IS_BEST);
				matcher.runOp(false);
				this.matchingMatrix = matcher.getMatching();
			}

			// do the actual evaluation
			HashMap<String,HashMap<Integer,Double> > evalData = this.doEvaluation();
			if (evalData == null) {
				System.out.println("No EvalData!");
				return;
			}

			// fill result table
			this.fillResultTable(evalData);
			if (this.resultFile != null)
				this.resultTable.saveTable(new File(this.resultFile.getFileName()));
		} catch (ALDProcessingDAGException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Converts input images to short pixel format and optionally inverts them.
	 * @throws ALDOperatorException				Thrown in case of failure.
	 * @throws ALDProcessingDAGException	Thrown in case of failure.
	 */
	protected void convertLabelImages()
			throws ALDOperatorException, ALDProcessingDAGException {
		
		// convert input images to short data type if necessary
		this.segLabelImage = (MTBImageShort)this.segLabelsOrig.convertType(
				MTBImageType.MTB_SHORT, true);
		
		if (this.gtLabelsOrig.getType() == MTBImageType.MTB_RGB) {
			// convert groundtruth colors to gray-scale
			LabelImageConverter convOp =
					new LabelImageConverter((MTBImageRGB)this.gtLabelsOrig);
			convOp.runOp(false);
			this.gtLabelImage = convOp.getResultLabelImage();
		}
		else {
			this.gtLabelImage = (MTBImageShort)this.gtLabelsOrig.convertType(
					MTBImageType.MTB_SHORT, true);
		}

		// optionally invert groundtruth
		if (this.invertGroundtrouth.booleanValue()) {
			MTBImageShort tmpImg = (MTBImageShort)this.gtLabelImage.duplicate();
			for (int y=0;y<tmpImg.getSizeY();++y)
				for (int x=0;x<tmpImg.getSizeX();++x)
					tmpImg.putValueInt(x, y,(tmpImg.getValueInt(x, y) == 0) ? 65535 : 0);
			this.gtLabelImage = tmpImg;
		}

		// optionally invert input
		if (this.invertInputImage.booleanValue()) {
			MTBImageShort tmpImg = (MTBImageShort)this.segLabelImage.duplicate();
			for (int y=0;y<tmpImg.getSizeY();++y)
				for (int x=0;x<tmpImg.getSizeX();++x)
					tmpImg.putValueInt(x, y,(tmpImg.getValueInt(x, y) == 0) ? 65535 : 0);
			this.segLabelImage = tmpImg;
		}
	}

	/**
	 * Extracts evaluation data in terms of overlaps and missed pixels.
	 */
	protected void extractLabelsSizesOverlaps() {

		this.labelSetGT = new TreeSet<Integer>();
		this.labelSetSG = new TreeSet<Integer>();

		// hashtable: GT-label -> { (SG-label, count) }
		Hashtable<Integer, Hashtable<Integer, Integer> > overlapHash =
				new Hashtable<Integer, Hashtable<Integer,Integer>>();
		this.sizesGT = new HashMap<Integer, Integer>();
		this.sizesSG = new HashMap<Integer, Integer>();
		for (int y=0;y<this.height;++y) {
			for (int x=0;x<this.width;++x) {
				Integer gtLabel = new Integer(this.gtLabelImage.getValueInt(x, y));
				Integer sgLabel = new Integer(this.segLabelImage.getValueInt(x, y));
				this.labelSetGT.add(gtLabel);
				this.labelSetSG.add(sgLabel);
				// calc region sizes of groundtruth regions
				if (this.sizesGT.containsKey(gtLabel))
					this.sizesGT.put(gtLabel,
							new Integer(this.sizesGT.get(gtLabel).intValue()+1));
				else
					this.sizesGT.put(gtLabel, new Integer(1));
				// calc region sizes of segmented regions
				if (this.sizesSG.containsKey(sgLabel))
					this.sizesSG.put(sgLabel,
							new Integer(this.sizesSG.get(sgLabel).intValue()+1));
				else
					this.sizesSG.put(sgLabel, new Integer(1));
				// check if gt label has been seen before
				if (overlapHash.containsKey(gtLabel)) {
					// fetch list of overlapping seg regions already found
					Hashtable<Integer,Integer> segHash = overlapHash.get(gtLabel);
					// seg label present? -> increment pixel count
					if (segHash.containsKey(sgLabel))
						segHash.put(sgLabel,new Integer(segHash.get(sgLabel).intValue()+1));
					// seg label not present? -> insert new element
					else
						segHash.put(sgLabel, new Integer(1));
				}
				// gt label was not seen before
				else {
					Hashtable<Integer, Integer> tmpSegHash =
							new Hashtable<Integer, Integer>();
					tmpSegHash.put(sgLabel, new Integer(1));
					overlapHash.put(gtLabel, tmpSegHash);
				}
			}
		}

		// regions with label zero are assumed as background, remove them
		if (this.labelSetGT.contains(new Integer(0)))
			this.labelSetGT.remove(new Integer(0));
		if (this.labelSetSG.contains(new Integer(0)))
			this.labelSetSG.remove(new Integer(0));
		
		// copy data to arrays
		this.labelListGT= new ArrayList<>();
		Iterator<Integer> treeIt = this.labelSetGT.iterator();
		while(treeIt.hasNext())
			this.labelListGT.add(treeIt.next());
		this.labelListSG= new ArrayList<>();
		treeIt = this.labelSetSG.iterator();
		while(treeIt.hasNext())
			this.labelListSG.add(treeIt.next());

		// determine size of score matrix
		this.maxRegionCount = ( this.labelSetGT.size() > this.labelSetSG.size() ?
				this.labelSetGT.size() : this.labelSetSG.size() );
		this.minRegionCount = ( this.labelSetGT.size() < this.labelSetSG.size() ?
				this.labelSetGT.size() : this.labelSetSG.size() );

		// generate array with references (set-index-1) <-> label
		this.gtIDs = new int[this.labelSetGT.size()];
		this.sgIDs = new int[this.labelSetSG.size()];
		int entry = 0;
		for (Integer item : this.labelSetGT)
		{
			this.gtIDs[entry] = item.intValue();
			++entry;
		}
		entry = 0;
		for (Integer item : this.labelSetSG)
		{
			this.sgIDs[entry] = item.intValue();
			++entry;
		}


		// fill the matrix, row/column IDs refer to set IDs in treesets
		this.scoreMatrix = new double[this.maxRegionCount][this.maxRegionCount];
		for (int r=0;r<this.maxRegionCount;++r)
			for (int c=0;c<this.maxRegionCount;++c)
				this.scoreMatrix[r][c] = 0;
		int entryGT=-1, entrySG= -1;
		for (Integer key: this.labelSetGT) {
			// get hashtable of overlapping segmented regions
			++entryGT;
			Hashtable<Integer,Integer> overlappingSegRegions = overlapHash.get(key);
			// iterate over all entries
			Set<Integer> regKeys = overlappingSegRegions.keySet();
			for (Integer rkey: regKeys) {
				if (rkey.intValue() == 0)
					continue;
				// determine entry index of key in labelset
				for (entrySG = 0; entrySG < this.maxRegionCount; ++entrySG)
					if(this.sgIDs[entrySG] == rkey.intValue())
						break;
				if (entrySG < this.maxRegionCount) {
					this.scoreMatrix[entryGT][entrySG] =
							overlappingSegRegions.get(rkey).doubleValue();
				}
			}
		}
	}

	/**
	 * Calculates evaluation measures.
	 * @return	Evaluation measures.
	 * @throws ALDOperatorException				Thrown in case of failure.
	 * @throws ALDProcessingDAGException	Thrown in case of failure.
	 */
	protected HashMap< String, HashMap<Integer, Double> > doEvaluation()
			throws ALDOperatorException, ALDProcessingDAGException {

		HashMap< String, HashMap<Integer, Double> > evalData =
				new HashMap<String, HashMap<Integer,Double>>();

		PreprocessLabelImages preprocessor = new PreprocessLabelImages(
			this.gtLabelImage, this.segLabelImage, 
				this.labelListSG, this.labelListGT);
		preprocessor.runOp();

		ArrayList<ArrayList<Point>> segContours= preprocessor.getSegContours();
		ArrayList<ArrayList<Point>> gtContours= preprocessor.getGtContours();

		if (this.HAU) {
			Measure_HausdorffDistance hausdorff = new Measure_HausdorffDistance(
					this.segLabelImage, this.gtLabelImage, segContours, gtContours,
					this.labelListSG, this.labelListGT, this.matchingMatrix);
			hausdorff.runOp();
			HashMap< String, HashMap<Integer, Double> > resultData =
					hausdorff.getResult().getResultData();
			Set<String> keys = resultData.keySet();
			for (String entry: keys) {
				evalData.put(entry, resultData.get(entry));
			}
		}
		
		if (this.DE) {

			Measure_DetectionErrors detection = new Measure_DetectionErrors(
					this.segLabelImage, this.gtLabelImage, segContours, gtContours,
					this.labelListSG, this.labelListGT, this.matchingMatrix);
			detection.runOp();
			HashMap< String, HashMap<Integer, Double> > resultData =
					detection.getResult().getResultData();
			Set<String> keys = resultData.keySet();
			for (String entry: keys) {
				evalData.put(entry, resultData.get(entry));
			}
		}
		
		if (this.PRA) {

			Measure_PrattsFigureOfMerit pratt = new Measure_PrattsFigureOfMerit(
					this.segLabelImage, this.gtLabelImage,	segContours, gtContours,
					this.labelListSG, this.labelListGT, this.matchingMatrix);
			pratt.runOp();
			HashMap< String, HashMap<Integer, Double> > resultData =
					pratt.getResult().getResultData();
			Set<String> keys = resultData.keySet();
			for (String entry: keys) {
				evalData.put(entry, resultData.get(entry));
			}
		}
		
		if (this.Odet) {
			Measure_OdetsCriteria odet = new Measure_OdetsCriteria(
					this.segLabelImage, this.gtLabelImage, segContours, gtContours,
					this.labelListSG, this.labelListGT, this.matchingMatrix, this.n);
			odet.runOp();
			HashMap< String, HashMap<Integer, Double> > resultData =
					odet.getResult().getResultData();
			Set<String> keys = resultData.keySet();
			for (String entry: keys) {
				evalData.put(entry, resultData.get(entry));
			}
		}
		
		if (this.GTC) {
			Measure_RecallPrecisionF rpf = new Measure_RecallPrecisionF(
					this.segLabelImage, this.gtLabelImage,
					this.labelSetSG, this.labelSetGT, this.gtIDs, this.sgIDs,
					this.sizesGT, this.sizesSG,
					this.minRegionCount, this.maxRegionCount,
					this.scoreMatrix, this.matchingMatrix);
			rpf.runOp();
			HashMap< String, HashMap<Integer, Double> > resultData =
					rpf.getResult().getResultData();
			Set<String> keys = resultData.keySet();
			for (String entry: keys) {
				evalData.put(entry, resultData.get(entry));
			}
		}
		return evalData;
	}

	/**
	 * Method to fill the result table.
	 * @param data	Evaluation data to fill into the table.
	 */
	protected void fillResultTable(
			HashMap<String,HashMap<Integer,Double> > data) {

		// setup the table
		this.createResultTable();

		// iterate over groundtruth regions and extract corresp. segmented region
		int line = 0;
		for (int j=0; j<this.labelListGT.size(); ++j) {
			int segID = 0;
			for (int i = 0; i < this.labelListSG.size(); i++) {
				if (this.matchingMatrix[i][j] == 1) {
					segID = i;
					break;
				}
			}
			this.resultTable.setValueAt(this.labelListGT.get(j),line, 0);
			this.resultTable.setValueAt(this.labelListSG.get(segID), line, 1);
			++line;
		}

		// iterate over all measures and fill table column-wise
		line = 0;
		for (Integer gtID : this.labelListGT) {
			// Hausdorff measure
			if (data.get("Hausdorff") != null)
				this.resultTable.setValueAt(data.get("Hausdorff").get(gtID),
						line, 2);

			// Detection Errors
			if (data.get("ODE") != null)
				this.resultTable.setValueAt(data.get("ODE").get(gtID),
						line, 3);
			if (data.get("UDE") != null)
				this.resultTable.setValueAt(data.get("UDE").get(gtID),
						line, 4);
			if (data.get("LE") != null)
				this.resultTable.setValueAt(data.get("LE").get(gtID),
						line, 5);

			// Pratt's Figure of Merit
			if (data.get("Pratt") != null)
				this.resultTable.setValueAt(data.get("Pratt").get(gtID),
						line, 6);

			// Odet's Criteria
			if (data.get("OCO") != null)
				this.resultTable.setValueAt(data.get("OCO").get(gtID),
						line, 7);
			if (data.get("OCU") != null)
				this.resultTable.setValueAt(data.get("OCU").get(gtID),
						line, 8);

			// Recall, Precision and F-Measure
			if (data.get("Recall") != null)
				this.resultTable.setValueAt(data.get("Recall").get(gtID),
						line, 9);
			if (data.get("Precision") != null)
				this.resultTable.setValueAt(data.get("Precision").get(gtID),
						line, 10);
			if (data.get("TP") != null)
				this.resultTable.setValueAt(data.get("TP").get(gtID),
						line, 11);
			if (data.get("FP") != null)
				this.resultTable.setValueAt(data.get("FP").get(gtID),
						line, 12);
			if (data.get("FN") != null)
				this.resultTable.setValueAt(data.get("FN").get(gtID),
						line, 13);
			if (data.get("F1-Score") != null)
				this.resultTable.setValueAt(data.get("F1-Score").get(gtID),
						line, 14);
			if (data.get("Jaccard") != null)
				this.resultTable.setValueAt(data.get("Jaccard").get(gtID),
						line, 15);

			// next line
			++line;
		}
	}

	/**
	 * This method prepares the result table for the solution.
	 *
	 * POST:
	 * > result table of the necessary size and with a header
	 */
	protected void createResultTable() {
		// count how many criteria are selected
		int col = 2;
		if (this.HAU) col++;
		if (this.PRA) col++;
		if (this.DE) col+=3;
		if (this.Odet) col+=2;
		if (this.GTC) col += 7;

		Vector<String> header = new Vector<String>();
		header.add("GT-ID");
		header.add("Seg-ID");

		header.add("HAU");

		header.add("ODE");
		header.add("UDE");
		header.add("LE");
		header.add("PRA");

		header.add("OCO");
		header.add("OCU");

		header.add("Recall");
		header.add("Precision");
		header.add("TP");
		header.add("FP");
		header.add("FN");
		header.add("F1-Score");
		header.add("Jaccard-Index");

		this.resultTable = new MTBTableModel(this.labelListGT.size(), col, header);
	}
}
