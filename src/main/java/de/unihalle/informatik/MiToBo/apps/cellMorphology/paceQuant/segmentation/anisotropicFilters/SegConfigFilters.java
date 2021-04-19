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

package de.unihalle.informatik.MiToBo.apps.cellMorphology.paceQuant.segmentation.anisotropicFilters;

import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.MiToBo.apps.cellMorphology.paceQuant.PaCeQuant.GapCloseMode;
import de.unihalle.informatik.MiToBo.apps.cellMorphology.paceQuant.PaCeQuant.SpineLengthDefine;
import de.unihalle.informatik.MiToBo.filters.linear.GaussFilter.SigmaInterpretation;

/**
 * Configurator object for pavement cell detection based on vesselness filters.
 * 
 * @author Birgit Moeller
 */
@ALDParametrizedClass
public class SegConfigFilters {

	/**
	 * Flag to enable/disable parallel execution.
	 */
	@ALDClassParameter(label = "Run in parallel?", 
			dataIOOrder = -1)
	private boolean runParallel = false;

	/**
	 * Mode for closing gaps.
	 */
	@ALDClassParameter(label = "Heuristic for Gap Closing", 
			dataIOOrder = 0)
	private GapCloseMode gapMode = GapCloseMode.WATERSHED;

	/**
	 * Maximal distance of gaps in naive mode to be closed.
	 */
	@ALDClassParameter(label = "  - End-point distance for naive heuristic",
			dataIOOrder = 1)
	private int naiveGapThreshold = 20;

	/**
	 * Gaussian smoothing configuration.
	 */
	@ALDClassParameter(label = "Gaussian Sigma Interpretation",
			dataIOOrder = 10)
	private SigmaInterpretation sigmaMeaning = SigmaInterpretation.PHYSICALSIZE; 

	/**
	 * Niblack threshold.
	 */
	@ALDClassParameter(label = "Niblack threshold", 
			dataIOOrder = 11)
	private double niblackVarianceThresh = 4.0; 

	/**
	 * Spine length interpretation.
	 */
	@ALDClassParameter(label = "Spine Length Interpretation",
			dataIOOrder = 12)
	private SpineLengthDefine spineLengthDefine = SpineLengthDefine.ABSOLUTE; 

	/**
	 * Maximal length of admissible spines.
	 */
	@ALDClassParameter(label = "Maximal spine length",
			dataIOOrder = 13)
	private double spineLengthMax = 40; 
	
	/**
	 * Flag to enable/disable check for branch points in spines.
	 */
	@ALDClassParameter(label = "Allow Branch Points in Spines?", 
			dataIOOrder = 14)
	private boolean allowBranchPointsInSpines = false;

	/**
	 * Default constructor.
	 */
	public SegConfigFilters() {
	}
	
	/**
	 * Check if parallelization should be used.
	 * @return	If true, parallelization is desired.
	 */
	public boolean runParallelMode() {
		return this.runParallel;
	}	
	
	/**
	 * Get current gap mode selection
	 * @return	Selected gap mode.
	 */
	public GapCloseMode getGapMode() {
		return this.gapMode;
	}
	
	/**
	 * Get threshold for closing gaps in naive mode.
	 * @return	Maximal value for gaps to be closed.
	 */
	public int getNaiveGapThreshold() {
		return this.naiveGapThreshold;
	}
	
	/**
	 * Get interpretation for sigma values.
	 * @return	Interpretation for sigma values for Gaussian mask.
	 */
	public SigmaInterpretation getSigmaMeaning() {
		return this.sigmaMeaning;
	}
	
	/**
	 * Get variance threshold for Niblack thresholding.
	 * @return	Selected variance threshold.
	 */
	public double getNiblackVarianceThreshold() {
		return this.niblackVarianceThresh;
	}
	
	/**
	 * Get interpretation of spine length parameter.
	 * @return	Either absolute or relative.
	 */
	public SpineLengthDefine getSpineLengthInterpretation() {
		return this.spineLengthDefine;
	}
	
	/**
	 * Maximal length of admissible spines.
	 * @return	Maximal length for spines.
	 */
	public double getMaxSpineLength() {
		return this.spineLengthMax;
	}
	
	/**
	 * Request how to deal with branch points in spines.
	 * @return If true, branch points in spines are tolerated.
	 */
	public boolean isAllowedBranchPointsInSpines() {
		return this.allowBranchPointsInSpines;
	}
}
