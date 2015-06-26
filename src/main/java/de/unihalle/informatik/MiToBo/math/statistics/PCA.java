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

package de.unihalle.informatik.MiToBo.math.statistics;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.operator.events.ALDOperatorExecutionProgressEvent;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.MiToBo.core.datatypes.defines.MTBConstants;
import de.unihalle.informatik.MiToBo.core.operator.*;
import Jama.EigenvalueDecomposition;
import Jama.Matrix;

/**
 * This class implements the Karhunen-Loeve transformation, also known as PCA. 
 * <p>
 * Given a data matrix A where each column contains a data vector, first the 
 * covariance matrix of the data, i.e., {@latex.inline $A\\cdot A^T$}, is 
 * calculated. Then the eigenvalues and eigenvectors of this matrix are 
 * computed according to 
 * {@latex.ilb %preamble{\\usepackage{amssymb, amsmath}}
 *      \\begin{equation}
 *        A \\cdot A^T \\cdot \\vec{v} = \\lambda \\cdot \\vec{v}
 *       \\end{equation}}
 * A subset of the eigenvectors corresponding to the largest 
 * eigenvalues is then selected according to the given dimension reduction mode.
 * These are finally used to form the basis of a new vector space with reduced 
 * dimensionality. The (mean-free) input data is projected into this space for 
 * dimension reduction and yields the result of the operator.
 * <p>
 * In case that the dimensionality of the data is larger than the available 
 * number of samples, i.e., the input data matrix has more row than columns,
 * the calculations are simplified by using the matrix
 * {@latex.inline $A^T\\cdot A$} instead of the covariance matrix which is 
 * larger in this case. For the eigenvectors 
 * and values of this matrix the following equation holds:
 * {@latex.ilb %preamble{\\usepackage{amssymb, amsmath}}
 *      \\begin{equation}     	
 *        A^T \\cdot A \\cdot \\vec{w} = \\lambda \\cdot \\vec{w}
 *        \\;\\; \\Longleftrightarrow \\;\\;
 *        A \\cdot (A^T  \\cdot A ) \\cdot \\vec{w} = 
 *	          \\lambda \\cdot A \\cdot \\vec{w}
 *        \\;\\; \\Longleftrightarrow \\;\\;
 *        (A \\cdot A^T ) \\cdot A  \\cdot \\vec{w} = 
 *	          \\lambda \\cdot A \\cdot \\vec{w}
 *       \\end{equation}}
 * In detail, the eigenvectors of this matrix, denoted by
 * {@latex.inline $\\vec{w}$} in the above equation,
 * can be used to calculate the eigenvectors 
 * {@latex.inline $\\vec{v} = A \\cdot \\vec{w}$} of the covariance matrix 
 * without need for explicitly solving the problem for the larger matrix.
 * 
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,
	level=Level.STANDARD, allowBatchMode=false)
public class PCA extends MTBOperator {

	/**
	 * Available modes for determining the sub-space dimensionality.
	 */
	public static enum ReductionMode {
		/**
		 * Just pick a predefined number of components.
		 */
		NUMBER_COMPONENTS,
		/**
		 * Select components containing more than given the percentage of variance.
		 */
		PERCENTAGE_VARIANCE
	}
	
	/**
	 * Input data with each column containing a data vector.
	 */
	@Parameter( label= "Dataset", required = true, dataIOOrder = -1, 
		direction = Parameter.Direction.IN, description = "Dataset.")
	private double[][] dataset = null;

	/**
	 * Flag for indicating if input data is already mean-free.
	 */
	@Parameter( label= "Is data mean-free?", required = true,
		direction = Parameter.Direction.IN, dataIOOrder = 1,
		mode = ExpertMode.ADVANCED,
		description = "Set to true, if data is already mean-free.")
	private boolean isMeanFree = false;
	
	/**
	 * Mode for dimension reduction, i.e., how to determine the sub-space 
	 * dimensionality.
	 */
	@Parameter( label= "Reduction Mode", required = true, dataIOOrder = 2,
		direction = Parameter.Direction.IN, description = "Mode.")
	private ReductionMode mode = ReductionMode.PERCENTAGE_VARIANCE;
	
	/**
	 * Number of sub-space components in mode 
	 * {@link ReductionMode.NUMBER_COMPONENTS}.
	 */
	@Parameter( label= "Number of Components", required = true,
		direction = Parameter.Direction.IN, dataIOOrder = 3,
		description = "Number of components, i.e., sub-space dimensionality.")
	private int componentNum = 0;
	
	/**
	 * Variance fraction for automatic dimension selection in mode
	 * {@link ReductionMode.PERCENTAGE_VARIANCE}.
	 */
	@Parameter( label= "Variance fraction", required = true,
		direction = Parameter.Direction.IN, dataIOOrder = 4,
		description = "Percentage of data variance to be contained in sub-space.")
	private double percentageVar = 100.0;

	/**
	 * Resulting data set with each column containing a data vector.
	 */
	@Parameter( label= "Result Dataset", required = true,
			direction = Parameter.Direction.OUT, description = "Result dataset.")
	private transient double[][] resultData = null;

	/*
	 * Some local helper variables.
	 */
	
	/**
	 * Dimensionality of the input data.
	 */
	protected int dataDim;
	/**
	 * Number of data samples in input data.
	 */
	protected int sampleCount;
	/**
	 * Average vector of input dataset.
	 */
	protected transient double[] mean;
	/**
	 * Normalized, i.e., mean-free, dataset.
	 */
	protected transient double meanfreeData[][];
	/**
	 * Normalized, i.e., mean-free, data matrix.
	 */
	protected transient Matrix meanfreeDataMatrix;
	/**
	 * Covariance matrix calculated from mean-free data.
	 * <p>
	 * The scaling by the number of samples is omitted here as this is just a
	 * constant factor in eigenvalue and -vector calculations. 
	 */
	protected transient Matrix C;
	/**
	 * Set of computed eigenvalues.
	 * <p>
	 * Note that the values are in ascending order.
	 */
	protected transient double[] eigenVals = null;
	/**
	 * Matrix of eigenvectors, each column containing a vector.
	 * <p>
	 * The vectors are sorted according to their eigenvalues, i.e., the vector
	 * corresponding to the largest eigenvalue can be found in the last column.
	 */
	protected transient Matrix eigenVects;
	/**
	 * Dimensionality of the sub-space as either specified by the user or 
	 * automatically determined based on the percentage of variance.
	 */
	protected transient int subDim;
	/**
	 * The final transformation matrix to be used for dimension reduction.
	 * <p>
	 * This matrix is already transposed, i.e., each row contains a sub-space
	 * basis vector and the number of rows is equal to the dimension of the
	 * sub-space.
	 */
	protected transient Matrix P_t;
	
	/**
	 * Default constructor.
	 * @throws ALDOperatorException
	 */
	public PCA() throws ALDOperatorException {
		// nothing to do here
	}		

	/**
	 * Specify an input dataset.
	 * @param ds	Dataset to process.
	 */
	public void setDataset(double[][] ds) {
		this.dataset = ds;
	}
	
	/**
	 * Set flag to indicate if data is already mean-free.
	 * @param b	If true, the input data is assumed to be mean-free already.
	 */
	public void setMeanFreeData(boolean b) {
		this.isMeanFree = b;
	}
	
	/**
	 * Specify the mode for selecting the sub-space dimensionality.
	 * @param rm	Mode for dimension reduction.
	 */
	public void setReductionMode(ReductionMode rm) {
		this.mode = rm;
	}
	
	/**
	 * Number of sub-space components if reduction mode is NUMBER_COMPONENTS.
	 * @param compNum	Number of components, i.e., eigenvectors, to use.
	 */
	public void setNumberOfComponents(int compNum) {
		this.componentNum = compNum;
	}
	
	/**
	 * Fraction of variance to be represented in the sub-space if the 
	 * reduction mode is PERCENTAGE_VARIANCE.
	 * @param p	Fraction of variance to represent in sub-space.
	 */
	public void setPercentageOfVariance(double p) {
		this.percentageVar = p;
	}
	
	/**
	 * Get the transformed dataset.
	 * @return	Resulting dataset.
	 */
	public double[][] getResultData() {
		return this.resultData;
	}
	
	/**
	 * Get calculated eigenvalues in ascending order.
	 * @return	Set of eigenvalues, null if calculations are not yet completed.
	 */
	public double[] getEigenvalues() {
		return this.eigenVals;
	}
	
	/**
	 * Get calculated eigenvectors, one vector per column, in ascending order.
	 * @return	Set of eigenvectors, null if calculations are not yet completed.
	 */
	public double[][] getEigenvects() {
		if (this.eigenVects == null)
			return null;
		int vecNum = this.eigenVects.getColumnDimension();
		int vecDim = this.eigenVects.getRowDimension();
		double[][] vects = new double[vecDim][vecNum];
		for (int v= 0; v<vecNum; ++v) {
			for (int d=0; d<vecDim; ++d)
				vects[d][v] = this.eigenVects.get(d, v);
		}
		return vects;
	}

	/**
	 * This method does the actual work.
	 */
	@Override
	protected void operate() {
		
		if (this.verbose.booleanValue())
			System.out.println("[PCA] running...");
		
		// reset some variables
		this.eigenVals = null;

		// determine number of samples and sample dimension
		if (this.verbose.booleanValue())
			System.out.print("\t Examining dataset...");
    fireOperatorExecutionProgressEvent(
     	new ALDOperatorExecutionProgressEvent(this,
     		" examining dataset..."));
		this.examineDataset();
		if (this.verbose.booleanValue())
			System.out.println("done, found " 
				+ this.sampleCount + " samples with dimension " + this.dataDim + ".");
    fireOperatorExecutionProgressEvent(
     	new ALDOperatorExecutionProgressEvent(this,
     		" found " + this.sampleCount + " samples with dimension " 
     			+ this.dataDim ));

		// calculate mean-free data
		if (this.verbose.booleanValue())
			System.out.println("\t Calculating mean-free data...");
    fireOperatorExecutionProgressEvent(
     	new ALDOperatorExecutionProgressEvent(this,
     		" calculating mean-free data..."));
		this.calculateMeanFreeData();
		
		// covariance matrix and eigenvalues/-vectors
		if (this.verbose.booleanValue())
			System.out.println("\t Extracting principal components...");
    fireOperatorExecutionProgressEvent(
     	new ALDOperatorExecutionProgressEvent(this,
     		" extracting principal components..."));
		this.calculateCovarianceMatrixAndEigenstuff();
		
		// determine subspace dimension
		if (this.verbose.booleanValue())
			System.out.print("\t Determining subspace dimension...");
    fireOperatorExecutionProgressEvent(
     	new ALDOperatorExecutionProgressEvent(this,
     		" determining subspace dimension..."));
		this.determineSubspaceDimension();
		if (this.verbose.booleanValue())
			System.out.println("it's " + this.subDim + ".");
    fireOperatorExecutionProgressEvent(
     	new ALDOperatorExecutionProgressEvent(this,
     		" subspace dimension is " + this.subDim));
		
		// do the actual dimension reduction
		if (this.verbose.booleanValue())
			System.out.print("\t Performing dimension reduction...");
    fireOperatorExecutionProgressEvent(
     	new ALDOperatorExecutionProgressEvent(this,
     		" performing dimension reduction..."));
		this.doDimensionReduction();		
		if (this.verbose.booleanValue())
			System.out.println("done.");

		if (this.verbose.booleanValue())
			System.out.println("\t PCA completed!");
    fireOperatorExecutionProgressEvent(
     	new ALDOperatorExecutionProgressEvent(this, " PCA completed!"));
	}
	
	/**
	 * Extracts number of samples and their dimension from dataset.
	 */
	protected void examineDataset() {
		this.dataDim = this.dataset.length;
		this.sampleCount = this.dataset[0].length;
	}
	
	/**
	 * Computes the average data vector and makes data mean-free.
	 */
	protected void calculateMeanFreeData() {
		
		// initialize the mean vector
		this.mean = new double[this.dataDim];
		for (int d=0; d<this.dataDim; ++d)
			this.mean[d]= 0.0;
		
		// calculate average vector of input data (if not mean-free)
		if (!this.isMeanFree) {
			for (int d=0; d<this.dataDim; ++d) {
				for (int s=0; s<this.sampleCount; ++s) {
					this.mean[d] += this.dataset[d][s];
				}
				this.mean[d] /= this.sampleCount;
			}
		}
		// generate mean-free data
		this.meanfreeData = new double[this.dataDim][this.sampleCount];
		for (int d=0; d<this.dataDim; ++d) {
			for (int s=0; s<this.sampleCount; ++s) {
				this.meanfreeData[d][s] = this.dataset[d][s] - this.mean[d];
			}
		}
	}
	
	/**
	 * Calculates covariance matrix and eigenvalues and -vectors.
	 */
	protected void calculateCovarianceMatrixAndEigenstuff() {
		// init data matrix A and calculate covariance matrix
		this.meanfreeDataMatrix = new Matrix(this.meanfreeData);
		Matrix A = this.meanfreeDataMatrix;
		Matrix A_t = A.transpose();

		this.C = null;
		if (this.sampleCount > this.dataDim) {
			this.C = A.times(A_t); 
		}
		else {
			this.C = A_t.times(A);
		}

		// extract eigenvalues and eigenvectors of C
		EigenvalueDecomposition eigenOp = new EigenvalueDecomposition(this.C);
		this.eigenVals = eigenOp.getRealEigenvalues();
		if (this.sampleCount > this.dataDim) {
			this.eigenVects = eigenOp.getV();
		}
		else {
			// if sample count smaller than dimension, re-transform eigenvectors
			this.eigenVects = new Matrix(this.dataDim, this.sampleCount);
			for (int s=0; s<this.sampleCount; ++s) {
				Matrix eigenVec = new Matrix(this.sampleCount, 1);
				for (int d=0; d<this.sampleCount; ++d) {
					eigenVec.set(d, 0, eigenOp.getV().get(d,s));
				}
				Matrix transformedEigenVec = A.times(eigenVec);
				// normalize to unit length
				double sum = 0;
				for (int d=0; d<this.dataDim; ++d)
					sum += transformedEigenVec.get(d, 0) * transformedEigenVec.get(d, 0);
				// safety check: if sum is to small, i.e., vector almost zero,
				// set vector explicit to zero
				if (sum < MTBConstants.epsilon) {
					for (int d=0; d<this.dataDim; ++d)
						this.eigenVects.set(d, s, 0);
				}
				else {
					for (int d=0; d<this.dataDim; ++d)
						this.eigenVects.set(d, s, 
								transformedEigenVec.get(d, 0) * 1.0 / Math.sqrt(sum) );
				}
			}
		}
	}
	
	/**
	 * Determines desired sub-space dimensionality according to selected mode.
	 */
	protected void determineSubspaceDimension() {
		this.subDim = this.dataDim;
		switch(this.mode)
		{
		case NUMBER_COMPONENTS:
			this.subDim = this.componentNum;
			if (this.subDim > this.eigenVals.length)
				this.subDim = this.eigenVals.length;
			break;
		case PERCENTAGE_VARIANCE:
			double varSum = 0.0;
			for (int d=0; d<this.eigenVals.length; ++d) 
				varSum += this.eigenVals[d];
			double sum= 0.0;
			for (int d= this.eigenVals.length-1; d>=0; --d) { 
				sum += this.eigenVals[d];
				if (sum/varSum >= this.percentageVar) {
					this.subDim = this.eigenVals.length - d;
					break;
				}
			}
			break;
		}
	}
	
	/**
	 * Does the actual dimension reduction by data projection into sub-space.
	 */
	protected void doDimensionReduction() {
		// create projection matrix
		this.P_t = new Matrix(this.subDim, this.dataDim);
		if (this.sampleCount > this.dataDim) {
			for (int s=this.dataDim-1; s>=this.dataDim-this.subDim; --s) {
				for (int d=0; d<this.dataDim; ++d)
					this.P_t.set(this.dataDim - s - 1, d, this.eigenVects.get(d, s));
			}
		}
		else {
			for (int s=this.sampleCount-1; s>=this.sampleCount-this.subDim; --s) {
				for (int d=0; d<this.dataDim; ++d) {
					this.P_t.set(this.sampleCount - s - 1, d, this.eigenVects.get(d, s));
				}
			}
		}
		
		// compute projection and result data
		Matrix projectedData = this.P_t.times(this.meanfreeDataMatrix);
		this.resultData = new double[this.subDim][this.sampleCount];
		for (int s=0; s<this.subDim; ++s) {
			for (int d=0; d<this.sampleCount; ++d) {
				this.resultData[s][d] = projectedData.get(s, d);
			}
		}
	}
}

/*BEGIN_MITOBO_ONLINE_HELP

Principal component analysis and Karhunen-Loeve transformation of data.
 
<ul>
<li><p><b>input:</b>
<ul>
<li><p><i>Dataset</i>:<br> data to process, each column of the matrix should 
	contain a data vector</p></li>
<li><p><i>Is data mean-free?</i>:<br> flag to indicate if data is already mean-free
	which results in skipping the mean value calculations and might save
	some time</p></li>	
<li><p><i>Reduction Mode</i>:<br> for dimension reduction a subset of principal
	components is selected, the mode specifies how this is done
	<ul>
	<li>NUMBER_COMPONENTS:<br> a fixed number of components is chosen<br>
		(see also input parameter <i>Number of Components</i>)
	<li>PERCENTAGE_VARIANCE:<br> a certain percentage of data variance is preserved<br>
		(see also input parameter <i>Variance fraction</i>)
	</ul></p></li>
<li><p><i>Number of Components</i>:<br> dimension of data subspace, i.e., 
	number of principal components used for dimension-reduction</p></li>
<li><p><i>Variance fraction</i>:<br> amount of data variance to be represented 
	in the subspace, e.g., 90% or 95%</p></li>
</ul>

<li><p><b>output:</b>
<ul>
<li><p><i>Result Dataset</i>: dimension-reduced data set, each column contains
	a data vector
</ul>

</ul>

END_MITOBO_ONLINE_HELP*/
