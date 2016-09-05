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

package de.unihalle.informatik.MiToBo.segmentation.evaluation.measures;

import java.util.HashMap;
import java.util.TreeSet;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageShort;
import de.unihalle.informatik.MiToBo.segmentation.evaluation.CalcQualityMeasures;
import de.unihalle.informatik.MiToBo.segmentation.evaluation.MTBGroundtruthEvaluationData;

/**
 * Class for evaluating recalls, precisions, F1-scores and Jaccard indices.
 *
 * @author moeller
 */
public class Measure_RecallPrecisionF extends EvaluationMeasure {

	/**
	 * Default constructor.
	 *
	 * @throws ALDOperatorException Thrown in case of failure.
	 */
	public Measure_RecallPrecisionF() throws ALDOperatorException {
		// nothing to do here
	}

	/**
	 * Default constructor with parameters.
	 *
	 * @param _segLabels		Label image of segmentation result.
	 * @param _gtLabels			Label image of groundtruth segmentation.
	 * @param _labelsetSG		Set of labels in segmentation image.
	 * @param _labelsetGT 	Set of labels in groundtruth image.
	 * @param _gtIDs 				Match list of set entries to groundtruth labels.
	 * @param _sgIDs 				Match list of set entries to segmentation labels.
	 * @param _sizesGT 			Region sizes in groundtruth image indexed by label.
	 * @param _sizesSG 			Region sizes in segmentation image indexed by label.
	 * @param _minRegionCount 	Minimal number of regions in both images.
	 * @param _maxRegionCount 	Maximal number of regions in both images.
	 * @param _scoreMatrix 			Score matrix for label matching.
	 * @param _matchingMatrix 	Label matching matrix (based on score matrix).
	 * @throws ALDOperatorException	Thrown in case of failure.
	 */
	public Measure_RecallPrecisionF(
			MTBImageShort _segLabels,	MTBImageShort _gtLabels,
			TreeSet<Integer> _labelsetSG, TreeSet<Integer> _labelsetGT,
			int [] _gtIDs, int [] _sgIDs,
			HashMap<Integer, Integer> _sizesGT, HashMap<Integer, Integer> _sizesSG,
			int _minRegionCount, int _maxRegionCount,
			double[][] _scoreMatrix, byte[][] _matchingMatrix)
					throws ALDOperatorException {
		super(_segLabels, _gtLabels);
		this.labelsetGT = _labelsetGT;
		this.labelsetSG = _labelsetSG;
		this.gtIDs = _gtIDs;
		this.sgIDs = _sgIDs;
		this.sizesGT = _sizesGT;
		this.sizesSG = _sizesSG;
		this.minRegionCount = _minRegionCount;
		this.maxRegionCount = _maxRegionCount;
		this.scoreMatrix = _scoreMatrix;
		this.matchingMatrix = _matchingMatrix;
	}

	@Override
	protected void doEvaluation() 
			throws ALDOperatorException, ALDProcessingDAGException {

		// allocate data structures
		HashMap<Integer, Double> mapTruePositives =
				new HashMap<Integer, Double>();
		HashMap<Integer, Double> mapFalsePositives =
				new HashMap<Integer, Double>();
		HashMap<Integer, Double> mapFalseNegatives =
				new HashMap<Integer, Double>();
		HashMap<Integer, Double> mapRecalls =
				new HashMap<Integer, Double>();
		HashMap<Integer, Double> mapPrecisions =
				new HashMap<Integer, Double>();
		HashMap<Integer, Double> mapFScores =
				new HashMap<Integer, Double>();
		HashMap<Integer, Double> mapJaccardIDs =
				new HashMap<Integer, Double>();
		HashMap<Integer, Integer> mapAssignments =
				new HashMap<Integer, Integer>();

		// run over all groundtruth region labels
		int entry = 0;
		for (entry= 0; entry<this.labelsetGT.size(); ++entry) {
			
			// get corresponding groundtruth label
			Integer gtLab = new Integer(this.gtIDs[entry]);
			// get label of matched segmented region
			int segEntry = -1;
			for (int c=0;c<this.maxRegionCount;++c) {
				if (this.matchingMatrix[entry][c] == 1) {
					segEntry = c;
					break;
				}
			}
			if (segEntry >= this.sgIDs.length) {
				if (this.verbose.booleanValue())
					System.out.println("Warning: GT-Region " + gtLab + " not matched!");
				mapTruePositives.put(gtLab, new Double(0));
				// FP = SegRegionSize - TP
				mapFalsePositives.put(gtLab, new Double(0));
				// FN = GtRegionSize - TP
				int fn = this.sizesGT.get(gtLab).intValue();
				mapFalseNegatives.put(gtLab, new Double(fn));
				// recall of element
				mapRecalls.put(gtLab, new Double(0));
				mapPrecisions.put(gtLab, new Double(0));
				mapFScores.put(gtLab, new Double(0));
				mapJaccardIDs.put(gtLab, new Double(0));
				mapAssignments.put(gtLab, new Integer(-1));

				if(this.sgIDs.length == 0)
				{
					mapTruePositives.put(gtLab, new Double(0));
					// FP = SegRegionSize - TP
					mapFalsePositives.put(gtLab, new Double(0));
					// FN = GtRegionSize - TP
					mapFalseNegatives.put(gtLab, new Double(fn));
					// recall of element
					mapRecalls.put(gtLab, new Double(0));
					mapPrecisions.put(gtLab, new Double(1));
					mapFScores.put(gtLab, new Double(0));
					mapJaccardIDs.put(gtLab, new Double(0));
					mapAssignments.put(gtLab, new Integer(-1));
				}
			}
			else {
				Integer sgLab = new Integer(this.sgIDs[segEntry]);
				// fill result data objects:
				// TP = overlap
				int tp = (int)this.scoreMatrix[entry][segEntry];
				mapTruePositives.put(gtLab, new Double(tp));
				// FP = SegRegionSize - TP
				int fp = this.sizesSG.get(sgLab).intValue() - tp;
				mapFalsePositives.put(gtLab, new Double(fp));
				// FN = GtRegionSize - TP
				int fn = this.sizesGT.get(gtLab).intValue() - tp;
				mapFalseNegatives.put(gtLab, new Double(fn));
				// recall of element
				double recall = CalcQualityMeasures.calcRecall(tp, fn);
				//				sumRecall += recall;
				mapRecalls.put(gtLab, new Double(recall));
				// precision of element
				double precision = CalcQualityMeasures.calcPrecision(tp, fp);
				mapPrecisions.put(gtLab, new Double(precision));
				// f-score of element
				double fScore = 2.0 * ((recall * precision) / (recall + precision));
				mapFScores.put(gtLab, new Double(fScore));
				// Jaccard index
				double jIndex = (double)tp / (double)(tp + fn + fp);
				mapJaccardIDs.put(gtLab, new Double(jIndex));
				// assignment of element
				mapAssignments.put(gtLab, sgLab);
			}
		}

		// summarize result data
		HashMap< String, HashMap<Integer, Double> > resultMap =
				new HashMap<String, HashMap<Integer,Double>>();
		resultMap.put("FN", mapFalseNegatives);
		resultMap.put("FP", mapFalsePositives);
		resultMap.put("TP", mapTruePositives);
		resultMap.put("Recall", mapRecalls);
		resultMap.put("Precision", mapPrecisions);
		resultMap.put("F1-Score", mapFScores);
		resultMap.put("Jaccard", mapJaccardIDs);
		this.evalData = new MTBGroundtruthEvaluationData(resultMap);
	}
}
