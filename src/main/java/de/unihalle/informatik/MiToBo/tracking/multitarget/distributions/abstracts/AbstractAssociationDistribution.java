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

/* 
 * Most recent change(s):
 * 
 * $Rev: 5288 $
 * $Date: 2012-03-29 10:27:02 +0200 (Thu, 29 Mar 2012) $
 * $Author: gress $
 * 
 */

package de.unihalle.informatik.MiToBo.tracking.multitarget.distributions.abstracts;

import java.io.OutputStream;
import java.util.Random;

import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.LogProbabilityDensityFunction;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.SamplingDistribution;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.abstracts.AbstractMultiState;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.interfaces.Copyable;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.interfaces.DataAssociation;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.interfaces.DataAssociationFactory;

/**
 * Abstract class for association distributions that allow sampling 
 * of the association variables for a set of observations in a multi-target tracking framework.
 * @author Oliver Gress
 *
 * @param <S> Type of discrete variables in the multi target observation
 * @param <T> Type of discrete variables in the multi target state
 */
@ALDMetaInfo(export=ExportPolicy.ALLOWED)
public abstract class AbstractAssociationDistribution<S extends Copyable<?>,T extends Copyable<?>> implements
		SamplingDistribution<DataAssociation> {

	/** random generator for sampling */
	protected Random rand;
	
	/** observation/measurement likelihood P(z_m^t | c_m, Z^{1:t-1}, c_{1:m-1}*/
	protected AbstractMultiObservationDistributionIndep<S, T> obsdistrib;
	
	/** spatial clutter distribution */
	protected LogProbabilityDensityFunction clutterdistrib;
	
	/** spatial distribution of observations from newborn targets */
	protected LogProbabilityDensityFunction newborndistrib;
	
	/** observations */
	protected AbstractMultiState<S> Z;
	
	/** factory for the data association object */
	protected DataAssociationFactory assocfactory;
	
	/** 
	 *  storage for the log likelihood values of the observations for possible associations 
	 *  to avoid recomputation
	 */
	protected double[][] log_pzc;
	
	/** number of observations */
	protected int M;
	
	/** number of targets */
	protected int N;
	
	/**
	 * Constructor
	 * @param rand
	 * @param Z vector of observations
	 * @param observationDistrib observation likelihood that can be evaluated for each observation independently
	 * @param spatialClutterDistrib
	 * @param assocFactory
	 */
	public AbstractAssociationDistribution(Random rand, 
												AbstractMultiState<S> Z,
												AbstractMultiObservationDistributionIndep<S, T> observationDistrib,
												LogProbabilityDensityFunction spatialClutterDistrib,
												LogProbabilityDensityFunction spatialNewbornDistrib,
												DataAssociationFactory assocFactory) {
		
		this.rand = rand;
		this.Z = Z;
		this.obsdistrib = observationDistrib;
		this.clutterdistrib = spatialClutterDistrib;
		this.newborndistrib = spatialNewbornDistrib;
		this.assocfactory = assocFactory;
		
		this.M = this.Z.getNumberOfStates();
		this.N = this.obsdistrib.getCondition().getNumberOfStates();
		
		// create array to hold likelihood values for each observation to be associated to each target as well as clutter and a newborn target
		this.log_pzc = new double[this.M][this.N + 2];
		
		// fill array with likelihood values
		for (int m = 0; m < this.M; m++) {
			
			// likelihood value of observation m associated as clutter
			this.log_pzc[m][0] = this.clutterdistrib.log_p(Z.getStateContinuous(m));
			
			// likelihood values of observation m associated to target n
			for (int n = 1; n <= this.N; n++) {
				this.log_pzc[m][n] = this.obsdistrib.log_p(Z, m, n-1);
			}
			
			this.log_pzc[m][this.N+1] = this.newborndistrib.log_p(Z.getStateContinuous(m));
		}
		
	}
	
	public void setNewObservations(AbstractMultiState<S> Z,
							AbstractMultiObservationDistributionIndep<S, T> observationDistrib) {
		this.Z = Z;
		this.obsdistrib = observationDistrib;	
		
		this.M = this.Z.getNumberOfStates();
		this.N = this.obsdistrib.getCondition().getNumberOfStates();
		
		// create array to hold likelihood values for each observation to be associated to each target as well as clutter and a newborn target
		this.log_pzc = new double[this.M][this.N + 2];
		
		// fill array with likelihood values
		for (int m = 0; m < this.M; m++) {
			
			// likelihood value of observation m associated as clutter
			this.log_pzc[m][0] = this.clutterdistrib.log_p(Z.getStateContinuous(m));
			
			// likelihood values of observation m associated to target n
			for (int n = 1; n <= this.N; n++) {
				this.log_pzc[m][n] = this.obsdistrib.log_p(Z, m, n-1);
			}
			
			this.log_pzc[m][this.N+1] = this.newborndistrib.log_p(Z.getStateContinuous(m));
		}
	}
	
	
	@Override
	public abstract DataAssociation drawSample();
	
	public abstract DataAssociation drawSampleDebug(DataAssociation groundtruth, OutputStream ostream);
}
