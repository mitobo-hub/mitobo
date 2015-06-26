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

package de.unihalle.informatik.MiToBo.tracking.multitarget.algo;

import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.FirstOrderMoment;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.interfaces.Copyable;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.interfaces.DataAssociation;

/**
 * Interface for multi-target prediction filters
 * @author Oliver Gress
 *
 * @param <S> class type of observations' discrete variables
 * @param <T> class type of states' discrete variables 
 */
@ALDMetaInfo(export=ExportPolicy.ALLOWED)
public interface MultiTargetPredictionFilter<T extends Copyable<?>> 
			extends FirstOrderMoment<T>, Copyable<MultiTargetPredictionFilter<T>> {
	
	/**
	 * Prediction step method
	 */
	void predict();
	
	/**
	 * Update step method given a multi-target observation
	 * @param observation 
	 */
	void update(T observation, DataAssociation association);

}
