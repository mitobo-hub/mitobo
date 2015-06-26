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
package de.unihalle.informatik.MiToBo.math;

import java.util.Random;

import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;
import de.unihalle.informatik.MiToBo.math.distributions.impl.GaussianDistribution;

import Jama.Matrix;

/**
 * 
 * A linear transform with additive Gaussian noise.
 * 
 * @author Oliver Gress
 *
 */
@ALDMetaInfo(export=ExportPolicy.ALLOWED)
public class LinearTransformGaussNoise {
	
	protected Matrix trafo, noiseCov;
	protected Random rand;
	protected GaussianDistribution gaussnoise;
	
	public LinearTransformGaussNoise(Matrix trafoMatrix, Matrix noiseCovariance, Random rand) {
		this.trafo = trafoMatrix;
		this.noiseCov = noiseCovariance;
		this.rand = rand;
	}
	
	public Matrix getTransformMatrix() {
		return this.trafo;
	}
	
	public Matrix getNoiseCovariance() {
		return this.noiseCov;
	}
	
	/** 
	 * Transform the (column) vector <code>x</code> and 
	 * add noise sampled from the Gaussian noise distribution.
	 */
	public Matrix transform(Matrix x) {
		
		if (this.gaussnoise == null) {
			this.gaussnoise = new GaussianDistribution(new Matrix(this.noiseCov.getRowDimension(),1),
															this.noiseCov, this.rand);
		}
		
		Matrix x_ = this.trafo.times(x);
		x_ = x_.plusEquals(this.gaussnoise.drawSample());
		
		return x_;
	}
	
	/**
	 * Transform the Gaussian distribution <code>gaussian</code> with this transform.
	 */
	public GaussianDistribution transform(GaussianDistribution gaussian) {
		
		Matrix x = this.trafo.times(gaussian.getMean());
		Matrix P = this.trafo.times(gaussian.getCovariance().times(this.trafo.transpose()));
		P.plusEquals(this.noiseCov);
		
		return new GaussianDistribution(x,P);
	}

}
