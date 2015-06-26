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

import Jama.Matrix;
import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.MiToBo.core.datatypes.defines.MTBConstants;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSnake;
import de.unihalle.informatik.MiToBo.segmentation.snakes.energies.paramAdapt.*;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.*;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.SnakeOptimizerSingle.EnergyNormalizationMode;

/**
 * Internal length penalty energy of the pioneering paper of Kass et al.
 * <p>
 * The energy is based on first order derivatives of a snake C and  
 * is defined as follows:
 * {@latex.ilb %preamble{\\usepackage{amssymb}}
 *      \\begin{equation*}
 *      	E(C) = 0.5 \\cdot \\int_0^1 \\alpha(s) \\cdot |C_s(s)|^2 ds 
 *      \\end{equation*}}
 * Note that in contrast to the original work this implementation allows for 
 * more flexible weighting parameters alpha, i.e. the weighting factors may be 
 * set for each snake point individually.
 * <p>
 * In addition, the weighting parameters can dynamically be adjusted by
 * providing the energy with an update object, see
 * {@link MTBSnakeEnergyCD_KassLength_ParamAdapt}.
 * <p>
 * Paper: Kass, Witkin and Terzopoulos, <i>Snakes: Active Contour Models</i>,
 * International Journal of Computer Vision, pp. 321-331, 1988.
 * 
 * @author moeller
 */
@ALDDerivedClass
@ALDParametrizedClass
public class MTBSnakeEnergyCD_KassLength 
	implements MTBSnakeEnergyDerivable, MTBSnakeEnergyComputable {

	/**
	 * Initial weighting factor for the length term, should be > 0.
	 */
	@ALDClassParameter(label="Initial Alpha")
	private double alphaInitial = 1.0;
	
	/**
	 * Parameter update strategy.
	 */
	@ALDClassParameter(label="Parameter Updater")
	private MTBSnakeEnergyCD_KassLength_ParamAdapt pUpdater =
																	new MTBSnakeEnergyCD_KassLength_ParamAdaptNone();

	/*
	 * Private helper variables.
	 */

	/**
	 * Mode of normalization.
	 */
	private EnergyNormalizationMode normMode = 
		EnergyNormalizationMode.NORM_BALANCED_DERIVATIVES;
	
	/**
	 * Normalization factor for scaling matrix entries. 
	 */
	private double normalizationFactor = 1.0;
	
	/**
	 * Maximal possible value of alpha as can be provided by parameter updated.
	 */
	private Double maxAlpha;
	
	/**
	 * Vector of point-specific alpha values.
	 */
	private double[] alphas;

	/**
	 * Default constructor.
	 */
	public MTBSnakeEnergyCD_KassLength() {
		// nothing to do here
	}
	
	/**
	 * Constructor with dynamic parameter updater.
	 * 
	 * @param _alpha	Weighting factor for length term.
	 * @param _pUpd		Parameter update object.
	 */
		public MTBSnakeEnergyCD_KassLength(double _alpha,
		    MTBSnakeEnergyCD_KassLength_ParamAdapt _pUpd) {
				this.alphaInitial = _alpha;
				this.pUpdater = _pUpd;
		}

	/**
	 * Scaling factor for image coordinates.
	 * <p>
	 * The factor is used to scale image coordinates between the range [0,1]
	 * and the real-world image size in pixels.
	 * Default scaling factor is 1.
	 */
	protected double scaleFactor = 1.0;

	/**
	 * Set the scaling factor.
	 * 
	 * @param s	New scaling factor.
	 */
	@Override
	public void setScaleFactor(double s) {
		this.scaleFactor = s;
	}

	/**
	 * Get scaling factor.
	 * 
	 * @return Scaling factor.
	 */
	@Override
	public double getScaleFactor() {
		return this.scaleFactor;
	}

	@Override
	public boolean initEnergy(SnakeOptimizerSingle o) {
		this.alphas = new double[1];
		this.alphas[0] = this.alphaInitial;
		
		// make sure that energy status is correct
		this.updateStatus(o);

		if (o.getNormalizationMode() != null)
			this.normMode = o.getNormalizationMode();
		// for greedy optimization normalization mode is ignored
		if (o instanceof SnakeOptimizerSingleGreedy)
			this.normMode = EnergyNormalizationMode.NORM_NONE;

		this.pUpdater.init(this);
		this.maxAlpha = this.pUpdater.getMaxAlpha();
		
		// determine probable energy ranges
		if (!this.maxAlpha.isNaN()) {
			double minE, maxE;
			switch (this.normMode)
			{	
			case NORM_BALANCED_DERIVATIVES:
				// derivatives range between [-2 * alpha, 2 * alpha]
				minE = - 2 * this.maxAlpha.doubleValue();
				maxE =   2 * this.maxAlpha.doubleValue();
				if (Math.abs(minE) > Math.abs(maxE))
					this.normalizationFactor = Math.abs(minE);
				else		
					this.normalizationFactor = Math.abs(maxE);
				if (Math.abs(this.normalizationFactor) < MTBConstants.epsilon)
					this.normalizationFactor = 1.0;
				break;
//			case NORM_ENERGY_ABSOLUTE:
//				// absolut energies range between [ 0 , alpha * N];
//				maxE = this.maxAlpha.doubleValue() * 2;
//				this.normalizationFactor = maxE;
//				break;
			case NORM_NONE:
				this.normalizationFactor = 1.0;
				break;
			}
			return true;
		}
		return false;
	}

	/**
	 * Get the initial alpha value.
	 * 
	 * @return Intial alpha.
	 */
	public double getInitAlpha() {
		return this.alphaInitial;
	}

	/**
	 * Calculates snake energy (non-normalized!).
	 */
	@Override
	public double calcEnergy(SnakeOptimizerSingle opt) {

		MTBSnake snake = opt.getCurrentSnake();

		double lEnergy = 0.0;
		int pointNum = snake.getPointNum();
		for (int i = 0; i < pointNum; ++i) {
			double x_diff = snake.getPartialDiffX(i);
			double y_diff = snake.getPartialDiffY(i);
			lEnergy += this.alphas[i] * (x_diff * x_diff + y_diff * y_diff);
		}
		double normFac = this.getNormalizationFactor(opt);
		return 0.5 * lEnergy * normFac;
	}

	/**
	 * Calculates energy at a certain position for the current snake.
	 */
	public double calcEnergy(SnakeOptimizerSingle opt, int pos) {
		MTBSnake s = opt.getCurrentSnake();
		double x_diff = s.getPartialDiffX(pos);
		double y_diff = s.getPartialDiffY(pos);
		double lener = this.alphas[pos] * (x_diff * x_diff + y_diff * y_diff);
		double normFac = this.getNormalizationFactor(opt);
		return 0.5 * lener * normFac;
	}

	/**
	 * Updates matrix A given weights for the internal energy term.
	 * <p>
	 * The upper left block and the lower right block of matrix A just dependent
	 * on the weight parameters alpha and beta for the internal snake energy
	 * terms.
	 */
	@Override
	public Matrix getDerivative_MatrixPart(SnakeOptimizerSingleVarCalc opt) {

		// get necessary data from the calling optimizer
		MTBSnake snake = opt.getCurrentSnake();
		int snakePointNum = snake.getPointNum();

		// get normalization factor
		double normFac = this.getNormalizationFactor(opt);

		if (this.alphas == null)
			return null;

		// init result matrix
		Matrix A = new Matrix(snakePointNum * 2, snakePointNum * 2);

		/* upper left block */

		// first line
		A.set(0, 0, 2 * this.alphas[0]*normFac);
		A.set(0, 1,    -this.alphas[0]*normFac); 
		A.set(0, snakePointNum - 1,  -this.alphas[0]*normFac);
		// second line
		A.set(1, 0,	    -this.alphas[1]*normFac);
		A.set(1, 1,  2 * this.alphas[1]*normFac);
		A.set(1, 2,     -this.alphas[1]*normFac);
		// inner lines
		for (int l = 2; l < snakePointNum - 2; ++l) {
			A.set(l,l-1,     -this.alphas[l]*normFac);
			A.set(l,  l,  2 * this.alphas[l]*normFac);
			A.set(l,l+1,     -this.alphas[l]*normFac);
		}
		// pre-last line
		A.set(snakePointNum - 2, snakePointNum - 3,
				   -this.alphas[snakePointNum - 2]*normFac);
		A.set(snakePointNum - 2, snakePointNum - 2, 
				2 * this.alphas[snakePointNum - 2]*normFac);
		A.set(snakePointNum - 2, snakePointNum - 1,
				   -this.alphas[snakePointNum - 2]*normFac);
		// last line
		A.set(snakePointNum - 1, 0, 
				    -this.alphas[snakePointNum - 1]*normFac);
		A.set(snakePointNum - 1, snakePointNum - 2,
				    -this.alphas[snakePointNum - 1]*normFac);
		A.set(snakePointNum - 1, snakePointNum - 1,
				 2 * this.alphas[snakePointNum - 1]*normFac);

		/* lower right block */

		// first line
		A.set(snakePointNum, snakePointNum,
				 2 * this.alphas[0]*normFac);
		A.set(snakePointNum, snakePointNum + 1,
				    -this.alphas[0]*normFac);
		A.set(snakePointNum, 2 * snakePointNum - 1,
				    -this.alphas[0]*normFac);
		// second line
		A.set(snakePointNum + 1, snakePointNum,
				    -this.alphas[1]*normFac);
		A.set(snakePointNum + 1, snakePointNum + 1,
				 2 * this.alphas[1]*normFac);
		A.set(snakePointNum + 1, snakePointNum + 2,
				    -this.alphas[1]*normFac);
		// inner lines
		for (int l = snakePointNum + 2; l < 2 * snakePointNum - 2; ++l) {
			A.set(l,l-1,   -this.alphas[l - snakePointNum]*normFac);
			A.set(l,  l,2 * this.alphas[l - snakePointNum]*normFac);
			A.set(l,l+1,   -this.alphas[l - snakePointNum]*normFac);
		}
		// pre-last line
		A.set(2 * snakePointNum - 2, 2 * snakePointNum - 3,
				    -this.alphas[snakePointNum - 2]*normFac);
		A.set(2 * snakePointNum - 2, 2 * snakePointNum - 2,
				 2 * this.alphas[snakePointNum - 2]*normFac);
		A.set(2 * snakePointNum - 2, 2 * snakePointNum - 1,
				    -this.alphas[snakePointNum - 2]*normFac);
		// last line
		A.set(2 * snakePointNum - 1, snakePointNum,
				    -this.alphas[snakePointNum - 1]*normFac);
		A.set(2 * snakePointNum - 1, 2 * snakePointNum - 2,
				    -this.alphas[snakePointNum - 1]*normFac);
		A.set(2 * snakePointNum - 1, 2 * snakePointNum - 1,
				 2 * this.alphas[snakePointNum - 1]*normFac);
		return A;
	}

	private double getNormalizationFactor(SnakeOptimizerSingle opt) {
		double normFac = 0.0;
		switch(opt.getNormalizationMode())
		{
		case NORM_BALANCED_DERIVATIVES:
			normFac = 1.0 / this.normalizationFactor;
			break;
//		case NORM_ENERGY_ABSOLUTE:
//			normFac = 
//				1.0/(this.normalizationFactor * opt.getCurrentSnake().getPointNum());
//			break;
		case NORM_NONE:
		default:
			normFac = 1.0;
			break;
		}		
		return normFac;
	}

	@Override
	public Matrix getDerivative_VectorPart(SnakeOptimizerSingleVarCalc opt) {
		return null;
	}

	@Override
	public String toString() {
		return new String("MTBSnkEner: Kass Length-Curvature Energy " 
				+ "(alpha = " + this.alphaInitial + ")");
	}

	@Override
	public void updateStatus(SnakeOptimizerSingle o) {
		// update parameter vectors
		this.alphas = this.pUpdater.alphaUpdate(o, this.alphas);
	}

	@Override
  public boolean requiresCounterClockwiseContourSorting() {
		return false;
	}
	
	@Override
  public  boolean requiresOverlapMask() {
		return false;
	}
}