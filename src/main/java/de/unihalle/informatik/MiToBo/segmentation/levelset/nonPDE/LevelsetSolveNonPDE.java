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
 * $Rev$
 * $Date$
 * $Author$
 * 
 */

package de.unihalle.informatik.MiToBo.segmentation.levelset.nonPDE;


import java.util.*;
import java.io.*;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.*;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.ExecutionMode;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;

import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.operator.*;
import de.unihalle.informatik.MiToBo.tools.system.UserTime;

/**
 * Implements a level set solver which may optionally perserve the topology 
 *       using non PDE optimization.
 * See: Song, B. and Chan, T.: A fast algorithm for level set based optimization,
 *      UCLA Cam Report, 2002
 * <p>
 * Optionally a invalid image may be supplied which defines 
 *    pixels with nonzero value as invalid and not considered for segmentation.
 * <p>
 * The resulting image is a short image, where background and invalid pixels have zeros
 *    and the obejct compontens/phases values starting from one.
 * <p>
 * NOTE: if verbose is turned on runtime will typically be  increase considerably
 * due to outputting (and consequently computing) the complete energy.
 * <p>
 * Currently only 2D images are supported.
 *
 * @author Stefan Posch, partially bases on code by Markus Glass
 *
 */

// TODO: check for correct handling of invalidImg (e.g. blockChange)

@ALDAOperator(genericExecutionMode=ExecutionMode.NONE)
public class LevelsetSolveNonPDE extends MTBOperator {

	@Parameter( label= "phi", required = true, direction = Parameter.Direction.IN,
                description = "Initial levelset function")
	private MTBLevelsetMembership phi;

@Parameter( label= "invalidImage", required = false, direction = Parameter.Direction.IN,
                description = "optional image of invalid pixels (pixels <> 0 are invalid)")
	private MTBImage invalidImage;

	@Parameter( label= "resultImage", direction = Parameter.Direction.OUT,
                description = "Result image")
	private MTBImageShort resultImage;

	@Parameter( label= "energy", required = true, direction = Parameter.Direction.IN,
                description = "Energy to use for optimization")
	private MTBGenericEnergyNonPDE energy;

	@Parameter( label= "maxIterations", required = false, direction = Parameter.Direction.IN,
                description = "Maximal number of iterations")
	private Integer maxIterations = 100;

	@Parameter( label= "preserveTopology", required = true, direction = Parameter.Direction.IN,
                description = "Topology preserving mode?")
	private Boolean preserveTopology;

	@Parameter( label= "debug", direction = Parameter.Direction.IN, supplemental = true,
                description = "Additional dubugging informaton")
	private Integer debug = 0;

	@Parameter( label= "spacingIntermediate", direction = Parameter.Direction.IN, supplemental = true,
                description = "spacing of intermediate of levelset function returned; 0 = none?")
	private Integer spacingIntermediate = 0;

	@Parameter( label= "intermediateLS", direction = Parameter.Direction.IN, supplemental = true,
                description = "intermediate of levelset functions")
	private Vector<MTBImageShort> intermediateLS;

	@Parameter( label= "numIterations", direction = Parameter.Direction.IN, supplemental = true,
                description = "Number of iterations performed")
	private Integer numIterations;

    /** Phases the current pixel is (topologically) allowed to change to.
	 * This vector is recycled for efficiency.
	 */
    private Vector<Short> potentialNewPhases = new Vector<Short>();

    /** Non redudant phases in the neighborhood of the current pixel. 
	 * This vector is recycled for efficiency.
	 */
    private Vector<Short> nbPhasesNonredundant = new Vector<Short>();

    /** Phases of the 8-neighbors of the current pixel.
	 * This array is recycled for efficiency.
	 */
    private short[] nbPhases = new short[8]; 
       
    /**
     * Constructor. 
         
     * @param enery Energy
     * @param ls initialed level set function of type membership, i.e. LevelsetMembership
     * @param maxIter maximal number of iterations
     * @param spacingIntermediate spacing of intermediate of level set function returned; 0 = none
     * @param invalidImg optional image of invalid pixels (pixels <> 0 are invalid)
     * @param preserveTopology 
     * @param verbose output if requested
     * @param debug bit mask for debugging output 
     */

    public LevelsetSolveNonPDE( MTBGenericEnergyNonPDE energy, MTBLevelsetMembership phi, int maxIter, int spacingIntermediate, MTBImage invalidImg, boolean preserveTopology) 
		throws ALDOperatorException {

        this.energy = energy;
        this.phi = phi;
        this.maxIterations = maxIter;
        this.preserveTopology = preserveTopology;
        this.invalidImage = invalidImg;
        this.spacingIntermediate = spacingIntermediate;
    }

    private boolean verbose = false;
	private static final int FLAG_SOLVER = 1; // flag vor solver
	private static final int FLAG_SOLVER2 = 2; // flag vor solver

    /**
     * Constructor
     *
     */
    public LevelsetSolveNonPDE() throws ALDOperatorException {
    }


    /**
     * This function does the actual work, i.e. optimization of the energy.
	 * NOTE: Currently only 2D level set functions are supported
	 * NOTE: if debug & FLAG_SOLVER, then for debugging purposes the complete energy
	 * is computed in each iteration, which is expensive
     */
    
	@Override
    protected void operate() throws ALDOperatorException {
		if ( phi.getSizeZ() != 1 )
			throw new ALDOperatorException( OperatorExceptionType.OPERATE_FAILED, "LevelsetSolveNonPDE.operate() failed: currently only 2D level set functions are suported");
		
		if ( solve()  ) {
            // set the resulting image 
        	
            resultImage = phi.getMTBImageLabel();

        } else {
			throw new ALDOperatorException( OperatorExceptionType.OPERATE_FAILED, "LevelsetSolveNonPDE.operate() failed: Result image is 'null'");
        }
    }

    // this does the actual work
	// according to Song/Chan and modifications as in the Diploma thesis of Markus Glass
    private boolean solve() throws ALDOperatorException {
		energy.setDebug( debug);
        verbose = getVerbose();

		// images for intermediate results
        intermediateLS = null;
		if ( spacingIntermediate > 0 )
        	intermediateLS = new Vector<MTBImageShort>(); 

        // if spacingIntermediate > 0 then return also initial level set function
        if ( spacingIntermediate > 0 ) {
             intermediateLS.add( phi.getMTBImageLabel()); 
        }

		// more variables
        int iter = 0;
        int numFlipedPixel;
        double E = energy.E( phi);
        double oldE;
		UserTime gt = new UserTime();

		// verbose output
        if ( verbose ) {
            System.out.println( "LevelsetSolveNonPDE: initialized");
            System.out.println( phi);
            System.out.println( energy);
            System.out.println();
            System.out.println ( "Initial energy of phi: " + E);
            System.out.println();
            System.out.println( "LevelsetSolveNonPDE: Start optimization\n");
        }

        // now do the iterations
        do {
            iter++;
            numFlipedPixel = 0;
            oldE = E;

            // TODO: add support for 3D images (topology, getPotentialNewPhases)

			LevelsetIterator itr;
			if ( preserveTopology )
				itr = new LevelsetIteratorContourPoints();
			else
				itr = new LevelsetIteratorScan();
		
			while ( itr.hasNext() ) {
				CoordInt3D coord = itr.next();
				int x = coord.x;
				int y = coord.y;
				int z = coord.z;

                potentialNewPhases = getPotentialNewPhases2D( preserveTopology, x, y);

                // for all phases the pixel is allowed to change to 
                if ( potentialNewPhases.size() > 0 ) {
					//System.out.println( "flippable (" + x + "," + y + "|" + phi.getPhase( x,y) + 
                    //                 ") potentialNewPhases: " + potentialNewPhases);

                    // find energetically best phase
                    double bestDeltaE = Float.POSITIVE_INFINITY;
                    short    bestNewPhase = 0;
                    for ( int i = 0 ; i < potentialNewPhases.size() ; i++ ) {
                        //System.out.println( "newPhase: " + (short)potentialNewPhases.elementAt(i));
                        double delta = energy.deltaE( x, y, z, (short)potentialNewPhases.elementAt(i), phi);
				      //System.out.println( "       deltaE for new phase " + (short)potentialNewPhases.elementAt(i) + ": " + delta);
                        if ( delta < bestDeltaE ) {
                            bestDeltaE = delta;
                            bestNewPhase = potentialNewPhases.elementAt(i);
                        }
                    }

                    // does best phase decrease the energy, than flip
                    if (  bestDeltaE < 0 ) {
						if ( (debug & FLAG_SOLVER2) != 0 ) {
                        	System.out.println( "    change phase @ (" + x + "," + y + ") to: " 
														+ bestNewPhase + " delta: "  
														+ String.format("%12.10e",bestDeltaE));
						}
		                  
                        E += bestDeltaE;
                        numFlipedPixel++;
                        energy.updateParams( x, y, 0, bestNewPhase, phi);
                        phi.changePhase( x, y, 0, bestNewPhase);
                    }
                }
            }
		
			// add intermediate results if asked for
            if ( spacingIntermediate > 0 &&  (iter % spacingIntermediate) == 0 ) {
                 intermediateLS.add( phi.getMTBImageLabel()); 
            }

            if ( verbose ) {
                System.out.println( "======================");
                System.out.println( "Iteration:" + iter + "  flipped " + numFlipedPixel + " pixel");
                System.out.println( "Energy: " + String.format("%12.10e",oldE) + " --> " + 
                                    String.format("%12.10e", E));
				if ( (debug & FLAG_SOLVER) != 0 ) {
					double computedE = energy.E( phi);
                	System.out.println ( "  [control E " + 
			    					String.format("%12.10e",computedE) +", diff = " +  
									String.format("%12.10e",E-computedE) + "]");
				}

                System.out.println();
                System.out.println( phi);
            }


        } while ( numFlipedPixel != 0 && iter < maxIterations );

		if ( verbose ) {
            System.out.println( "======================");
			double t = gt.getElapsedTime();
            System.out.println( gt.getOperation() + ": " + t + " s" + " for " + iter + " iterations");
            System.out.println( "");

            System.out.println( "LevelsetSolveNonPDE: End of optimization");
            System.out.println();
			System.out.println( phi);
            System.out.println( "    " + energy);
		}

		numIterations = iter;
        // return intermediate results 
        if ( spacingIntermediate > 0 )
            // add final result if not already done
            if ( (iter % spacingIntermediate) != 0 )
                 intermediateLS.add( phi.getMTBImageLabel());

        return true;
    }

    /*
	 * Return all phases a pixel may change to for the 2D case. 
	 * As a side effect member field <code>potentialNewPhases</code> is modified
	 * (efficiency).
     * <br>
     * If preserveTopology is true, then
     * find all phases in (x,y)'s 4-neighborhood to which (x,y) may be changed
     * without changing the topology 
     * (do not split phase in multiple connected compontens
     *  and do not introduce holes in a connected component)
     * <br>
     * If preserveTopology is false then return all phases from nbphases (except invalid phase)
     */

    private Vector<Short>  getPotentialNewPhases2D( boolean preserveTopology,
                                 int x, int y) {
		
        get8NeighborsPhase(x,y, nbPhases); // phases for 8 neighbors

        potentialNewPhases.clear();

        short r = phi.getPhase( x,y); // phase of (x,y)
        if ( preserveTopology ) {
			// first find all phases in the 4 neighborhood
			nbPhasesNonredundant.clear();
            for (int k = 0; k < nbPhases.length; k += 2 ) {
				if ( ! nbPhasesNonredundant.contains( nbPhases[k]) )
					nbPhasesNonredundant.add( nbPhases[k]);
			}

            if ( r == MTBLevelsetMembership.BG_PHASE )  {  // current pixel belongs to background
				for ( int i = 0 ; i < nbPhasesNonredundant.size() ; i++ ) {
					short p = (short)nbPhasesNonredundant.elementAt(i);

                    if (p != MTBLevelsetMembership.INVALID_PHASE && p != r) {
                        if ( numBlockChange( nbPhases, p) == 1) {
                            if ( ! potentialNewPhases.contains( p) )
                                potentialNewPhases.add(p);
                        }
                    }
                }
                
            } else {   // current pixel belongs not to background
				for ( int i = 0 ; i < nbPhasesNonredundant.size() ; i++ ) {
					short p = (short)nbPhasesNonredundant.elementAt(i);

                    if (p != MTBLevelsetMembership.INVALID_PHASE && p != r) {
                        if ( numBlockChange(nbPhases, r) == 1) {
                            if (p != MTBLevelsetMembership.BG_PHASE) {
                                if ( numBlockChange( nbPhases, p) == 1) {
                                    if ( ! potentialNewPhases.contains( p) )
                                        potentialNewPhases.add(p);
                                }
                            } else {
                                if ( ! potentialNewPhases.contains( p) )
                                    potentialNewPhases.add(MTBLevelsetMembership.BG_PHASE);
                            }
                        }    
                    }
                }
            }
        } else {
			// we may change phase to all other phases
			for (short p = phi.BG_PHASE; p <= phi.getNumPhases(); p++ ) {
                if ( p != r && ! potentialNewPhases.contains( p) )
                    potentialNewPhases.add( p);

            }
        }

		return potentialNewPhases;
    }

    /**
     * Check whether there is a different/foreign phase (then <code>phase</code>) in the 4-neighborhood 
     * as represented in the 8 neighborhood <code>nbPhases</code>.
     *
     * @param nbPhases
     * @return
     */
    private boolean hasForeign4Neighbor( short phase, short[] nbPhases) {
        boolean has = false;
        for(int k = 0; k < nbPhases.length; k += 2) {
            if ( nbPhases[k] != phase && nbPhases[k] != MTBLevelsetMembership.INVALID_PHASE) {
                has = true;
                break;
            }
        }
        return has;
    }

    /**
     * Return number of connected components of different/foreign phase then <code>r</code>
	 * in the 8 neighborhood <code>nbPhases</code>,
	 * which are 4 connected to the current pixel.
     * Invalid pixels are not considered as another phase
     * This is identical to the topological number T_4(\vec x,fg) n in
     * <p>
     * Han, X. and Xu, C. and Prince, J.L., A topology preserving level set method for geometric deformable models},
     * PAMI, pages 755-768, 2003
     * 
     * @param nbPhases	Phases in the 8 neighborhood of current pixel with pahse <code>r</code>
     * @param r	Phase of current pixel
     * @return Number of components 
     */
    private int numBlockChange(short[] nbPhases, int r) {
        int chgCount = 0;
        int l = nbPhases.length;

        for(int i = 0; i < l; i++) {
            short p = nbPhases[i];
            short q = nbPhases[(i+1) % l];   // next neighbor
            short t = nbPhases[(i+l-1) % l]; // previous neighbor

            // current neighbor belongs to the same phase
            if(p == r) {
                // TODO: in noBlockChangeWithGranule entfaellt zweites Praed: q != 0
                if(q != r && q != 0) {
                    if(((i % 2) == 0) || t == r) {
                        chgCount++;
                    }
                }
            }
        }

		return chgCount;
    }

    /**
     * Compute phases of 8 neighbors of (x,y) and store these
	 * in the array <code>nb</code>.
	 * This array is reused for efficiency.
     * 
     * @param x	x coordinate
     * @param y	y coordinate
     * @param nb	array in which to store the phases.
     */
    private void get8NeighborsPhase(int x, int y, short[] nb) {
        if(x < phi.getSizeX()-1) {
            nb[0] = phi.getPhase(x+1, y);
            
            if(y < phi.getSizeY()-1) {
                nb[1] = phi.getPhase(x+1, y+1);
            } else {
                nb[1] = MTBLevelsetMembership.INVALID_PHASE;
                nb[1] = MTBLevelsetMembership.BG_PHASE;
            }
        } else {
            nb[0] = MTBLevelsetMembership.INVALID_PHASE;
            nb[0] = MTBLevelsetMembership.BG_PHASE;
            nb[1] = MTBLevelsetMembership.INVALID_PHASE;
            nb[1] = MTBLevelsetMembership.BG_PHASE;
        }
        
        if(y < phi.getSizeY()-1) {
            nb[2] = phi.getPhase(x, y+1);
            
            if(x > 0) {
                nb[3] = phi.getPhase(x-1, y+1);
            } else {
                nb[3] = MTBLevelsetMembership.INVALID_PHASE;
                nb[3] = MTBLevelsetMembership.BG_PHASE;
            }
        } else {
            nb[2] = MTBLevelsetMembership.INVALID_PHASE;
            nb[2] = MTBLevelsetMembership.BG_PHASE;
            nb[3] = MTBLevelsetMembership.INVALID_PHASE;
            nb[3] = MTBLevelsetMembership.BG_PHASE;
        }
        
        if(x > 0) {
            nb[4] = phi.getPhase(x-1, y);
            
            if(y > 0) {
                nb[5] = phi.getPhase(x-1, y-1);
            } else {
                nb[5] = MTBLevelsetMembership.INVALID_PHASE;
                nb[5] = MTBLevelsetMembership.BG_PHASE;
            }
        } else {
            nb[4] = MTBLevelsetMembership.INVALID_PHASE;
            nb[4] = MTBLevelsetMembership.BG_PHASE;
            nb[5] = MTBLevelsetMembership.INVALID_PHASE;
            nb[5] = MTBLevelsetMembership.BG_PHASE;
        }
        
        if(y > 0) {
            nb[6] = phi.getPhase(x, y-1);
            if(x < phi.getSizeX()-1) {
                nb[7] = phi.getPhase(x+1, y-1);
            } else {
                nb[7] = MTBLevelsetMembership.INVALID_PHASE;
                nb[7] = MTBLevelsetMembership.BG_PHASE;
            }
        } else {
            nb[6] = MTBLevelsetMembership.INVALID_PHASE;
            nb[6] = MTBLevelsetMembership.BG_PHASE;
            nb[7] = MTBLevelsetMembership.INVALID_PHASE;
            nb[7] = MTBLevelsetMembership.BG_PHASE;
        }
    }

	// helper for debugging purposes.
    private void printNbPhases( short[] nbPhases) {
        System.out.println( nbPhases[0] +
                    " " + nbPhases[1] +
                    " " + nbPhases[2] +
                    " " + nbPhases[3] +
                    " " + nbPhases[4] +
                    " " + nbPhases[5] +
                    " " + nbPhases[6] +
                    " " + nbPhases[7]);
	}

	// ===============================================================================
	/** Abstract class for iterators of pixels/voxels of the level set function.
	 */
	abstract private class LevelsetIterator implements Iterator<CoordInt3D> {
		public void remove() {
			System.err.println( "LevelsetIterator: cannot remove elements");
		}

    }

    /** Iterator for all pixels/voxels of the level set function in scanline fashion
	 * excluding invalid pixels/voxels.
	 */
	private class LevelsetIteratorScan extends LevelsetIterator implements Iterator<CoordInt3D> {
		int x;
		int y;
		int z;

		LevelsetIteratorScan() {
			x = phi.getSizeX()-1;
			y = phi.getSizeY()-1;
			z = -1;
			next();
		}

		@Override
		public boolean hasNext() {
			return  z < phi.getSizeZ();
		}

		@Override
		public CoordInt3D next() {
			if ( z == phi.getSizeZ() ) 
				throw new java.util.NoSuchElementException( "LevelsetIteratorScan");

			CoordInt3D coord = new CoordInt3D( x, y, z);
			do {
				x++;
				if ( x == phi.getSizeX() ) {
					x=0;
					y++;
					if ( y == phi.getSizeY() ) {
						y=0;
						z++;
					}
				}
			} while ( z < phi.getSizeZ() && phi.getPhase( x, y, z) == phi.INVALID_PHASE );
	
			return coord;
		}
	}

    /** Iterator for all contour pixels/voxels of the level set function
	 * excluding invalid pixels/voxels.
	 * Note: it is decided at the time of the instantiation of an iterator
	 * whether a pixel/voxel is a contour pixels/voxels, i.e. according to the current state
	 * of the level set function,
	 * The 4 neighborhood is used, e.g. a pixel/voxel is consider a contour pixel, if
	 * it has a 4 neighbor with a different phase.
	 * Note: currently only for 2D level set functions.
	 */
	private class LevelsetIteratorContourPoints extends LevelsetIterator implements Iterator<CoordInt3D> {
		int n;
		Vector<CoordInt3D> ctrPoints;
        short[] nbPhases;

		LevelsetIteratorContourPoints() {
			n = 0;
			ctrPoints = new Vector<CoordInt3D>();
        	nbPhases = new short[8]; 

			if ( phi.getSizeZ() != 1 ) 
				throw new ArrayIndexOutOfBoundsException("LevelsetIteratorContourPoints: not yet implemented for 3D");
				
            int z = 0; 
            for ( int y = 0 ; y < phi.getSizeY() ; y++ ) {
                for ( int x = 0 ; x < phi.getSizeX() ; x++ ) {
                    if ( phi.getPhase( x,y) != phi.INVALID_PHASE ) {
                        // valid pixel
                        get8NeighborsPhase(x,y, nbPhases); // phases for 8 neighbors

                        // is this pixel contour pixel (if topology preserving is required)
                        if ( hasForeign4Neighbor( phi.getPhase( x,y), nbPhases) ) {
                            ctrPoints.add( new CoordInt3D( x, y, z));
                    	}
                    }
                }
            }

		}

		@Override
		public boolean hasNext() {
			return  n < ctrPoints.size();
		}

		@Override
		public CoordInt3D next() {
			if ( n == ctrPoints.size() ) 
				throw new java.util.NoSuchElementException( "LevelsetIteratorScan");

			n++;

			return ctrPoints.elementAt( n-1);
		}

    }

	/** Just to hold 3D coordinate and nothing else
	 */
	private class CoordInt3D {
		int x,y,z;

		CoordInt3D( int x, int y, int z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}

	/** Get value of maxIterations.
	  * Explanation: Maximal number of iterations.
	  * @return value of maxIterations
	  */
	public java.lang.Integer getMaxIterations(){
		return maxIterations;
	}
	
	/** Set value of maxIterations.
	  * Explanation: Maximal number of iterations.
	  * @param value New value of maxIterations
	  */
	public void setMaxIterations( java.lang.Integer value){
		this.maxIterations = value;
	}
	
	/** Get value of preserveTopology.
	  * Explanation: Topology preserving mode?.
	  * @return value of preserveTopology
	  */
	public java.lang.Boolean getPreserveTopology(){
		return preserveTopology;
	}
	
	/** Set value of preserveTopology.
	  * Explanation: Topology preserving mode?.
	  * @param value New value of preserveTopology
	  */
	public void setPreserveTopology( java.lang.Boolean value){
		this.preserveTopology = value;
	}
	
	/** Get value of energy.
	  * Explanation: Energy to use for optimization.
	  * @return value of energy
	  */
	public de.unihalle.informatik.MiToBo.segmentation.levelset.nonPDE.MTBGenericEnergyNonPDE getEnergy(){
		return energy;
	}
	
	/** Get value of invalidImage.
	  * Explanation: optional image of invalid pixels (pixels <> 0 are invalid).
	  * @return value of invalidImage
	  */
	public de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage getInvalidImage(){
		return invalidImage;
	}
	
	/** Get value of phi.
	  * Explanation: Initial levelset function.
	  * @return value of phi
	  */
	public de.unihalle.informatik.MiToBo.segmentation.levelset.nonPDE.MTBLevelsetMembership getPhi(){
		return phi;
	}
	
	/** Get value of resultImage.
	  * Explanation: Result image.
	  * @return value of resultImage
	  */
	public de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageShort getResultImage(){
		return resultImage;
	}
	
	/** Get value of intermediateLS.
	  * Explanation: Additional dubugging informaton.
	  * @return value of intermediateLS
	  */
	public Vector<MTBImageShort> getIntermediateLS(){
		return intermediateLS;
	}
	
	/** Get value of debug.
	  * Explanation: Additional dubugging informaton.
	  * @return value of debug
	  */
	public java.lang.Integer getDebug(){
		return debug;
	}
	
	/** Set value of debug.
	  * Explanation: Additional dubugging informaton.
	  * @param value New value of debug
	  */
	public void setDebug( java.lang.Integer value){
		this.debug = value;
	}
	
    /** Get value of numIterations
      * Explanation: Number of iterations performed
      * @return value of numIterations
      */
    public Integer getNumIterations(){
        return numIterations;
    }

	/** Get value of spacingIntermediate.
	  * Explanation: spacing of intermediate of levelset function returned; 0 = none?.
	  * @return value of spacingIntermediate
	  */
	public java.lang.Integer getSpacingIntermediate(){
		return spacingIntermediate;
	}
	
	/** Set value of spacingIntermediate.
	  * Explanation: spacing of intermediate of levelset function returned; 0 = none?.
	  * @param value New value of spacingIntermediate
	  */
	public void setSpacingIntermediate( java.lang.Integer value){
		this.spacingIntermediate = value;
	}	
}
