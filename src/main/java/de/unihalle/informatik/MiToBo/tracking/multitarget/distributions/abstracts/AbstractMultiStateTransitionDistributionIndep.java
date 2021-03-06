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
package de.unihalle.informatik.MiToBo.tracking.multitarget.distributions.abstracts;

import de.unihalle.informatik.MiToBo.math.distributions.interfaces.IndependentSamplingDistribution;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.abstracts.AbstractMultiState;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.abstracts.AbstractMultiStateFactory;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.interfaces.Copyable;

/**
 * Abstract class of multi-target state transition distribution with independent targets.
 * @author Oliver Gress
 *
 * @param <T>
 */
public abstract class AbstractMultiStateTransitionDistributionIndep<T extends Copyable<?>> extends AbstractMultiStateTransitionDistribution<T> 
														implements IndependentSamplingDistribution<AbstractMultiState<T>> {

	public AbstractMultiStateTransitionDistributionIndep(AbstractMultiState<T> X,
			AbstractMultiStateFactory<T> factoryX) {
		super(X, factoryX);
		
	}

	@Override
	public abstract AbstractMultiState<T> drawSample(int i, AbstractMultiState<T> X);

	@Override
	public abstract AbstractMultiState<T> drawSample();
}
