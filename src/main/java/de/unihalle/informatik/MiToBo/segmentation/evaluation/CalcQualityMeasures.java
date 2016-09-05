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

import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;

/**
 * Helper class for calculating precision and recall values.
 * 
 * @author moeller
 */
public class CalcQualityMeasures {

	/**
	 * Calculates the recall value from given true positives and false negatives.
	 * <p>
	 * Formular:
	 * @latex.block %preamble{\\usepackage{amssymb}}
         *    \\begin{equation}
         *      recall = \\frac{TP}{TP + FN}
         *    \\end{equation}
	 * 
	 * @param tp	True positive count.
	 * @param fn	False negative count.
	 * @return	Calculated recall value.
	 * @throws ALDOperatorException				Thrown in case of failure.
	 * @throws ALDProcessingDAGException	Thrown in case of failure.
	 */
	public static double calcRecall(int tp, int fn)
		throws ALDOperatorException, ALDProcessingDAGException {
		RecallCalc calcOp = new RecallCalc(new Integer(tp), new Integer(fn));
		calcOp.runOp(true);
		return calcOp.getRecall().doubleValue();
	}
	
	/**
	 * Calculates the precision from given true and false positives.
	 * <p>
	 * Formular:
	 * @latex.block %preamble{\\usepackage{amssymb}}
         *    \\begin{equation}
         *      recall = \\frac{TP}{TP + FP}
         *    \\end{equation}
	 * 
	 * @param tp	True positive count.
	 * @param fp	False positive count.
	 * @return	Calculated precision.
	 * @throws ALDOperatorException				Thrown in case of failure.
	 * @throws ALDProcessingDAGException	Thrown in case of failure.
	 */
	public static double calcPrecision(int tp, int fp)
		throws ALDOperatorException, ALDProcessingDAGException {
		PrecisionCalc calcOp = new PrecisionCalc(new Integer(tp), new Integer(fp));
		calcOp.runOp(true);
		return calcOp.getPrecision().doubleValue();
	}	

	/**
	 * Internal operator for recall calculation.
	 * 
	 * @author moeller
	 */
	private static class RecallCalc extends MTBOperator {

		/**
		 * True positives.
		 */
		@Parameter( label= "truePositives", required = true, 
				direction = Direction.IN, description = "True positive count.")
		private Integer tp = null;

		/**
		 * False negatives.
		 */
		@Parameter( label= "falseNegatives", required = true, 
				direction = Direction.IN, description = "False negative count.")
		private Integer fn = null;
		
		/**
		 * Recall value.
		 */
		@Parameter( label= "recallValue", required = true, 
				direction = Direction.OUT, description = "Calculated recall value.")
		private Double recall = null;

		/**
		 * Default constructor.
		 * @throws ALDOperatorException	Thrown in case of failure.
		 */
		@SuppressWarnings("unused")
    protected RecallCalc() throws ALDOperatorException {
	    super();
    }

		/**
		 * Constructor with inputs.
		 * 
		 * @param TP	True positive count.
		 * @param FN	False negative count.
		 * @throws ALDOperatorException	Thrown in case of failure.
		 */
		protected RecallCalc(Integer TP, Integer FN) throws ALDOperatorException {
			this.fn = FN;
			this.tp = TP;
		}
		
		@Override
    protected void operate() {
	    double truep = this.tp.doubleValue();
	    double falsen = this.fn.doubleValue();
	    this.recall = new Double(truep / (truep + falsen) );
    }
		
		/**
		 * Returns calculated recall.
		 * @return Calculated recall value.
		 */
		protected Double getRecall() {
			return this.recall;
		}
		
	}
	
	/**
	 * Internal operator for precision calculation.
	 * 
	 * @author moeller
	 */
	private static class PrecisionCalc extends MTBOperator {

		/**
		 * True positives.
		 */
		@Parameter( label= "truePositives", required = true, 
				direction = Direction.IN, description = "True positive count.")
		private Integer tp = null;

		/**
		 * False negatives.
		 */
		@Parameter( label= "falsePositives", required = true, 
				direction = Direction.IN, description = "False positive count.")
		private Integer fp = null;
		
		/**
		 * Precision value.
		 */
		@Parameter( label= "precisionValue", required = true, 
				direction = Direction.OUT, description = "Calculated precision value.")
		private Double precision = null;

		/**
		 * Default constructor.
		 * @throws ALDOperatorException	Thrown in case of failure.
		 */
		@SuppressWarnings("unused")
    protected PrecisionCalc() throws ALDOperatorException {
	    super();
    }

		/**
		 * Constructor with inputs.
		 * 
		 * @param TP	True positive count.
		 * @param FP	False positive count.
		 * @throws ALDOperatorException	Thrown in case of failure.
		 */
		protected PrecisionCalc(Integer TP, Integer FP) 
			throws ALDOperatorException {
			this.fp = FP;
			this.tp = TP;
		}
		
		@Override
    protected void operate() {
	    double truep = this.tp.doubleValue();
	    double falsep = this.fp.doubleValue();
	    this.precision = new Double(truep / (truep + falsep) );
    }
		
		/**
		 * Returns calculated precision.
		 * @return Calculated precision value.
		 */
		protected Double getPrecision() {
			return this.precision;
		}
		
	}
}
