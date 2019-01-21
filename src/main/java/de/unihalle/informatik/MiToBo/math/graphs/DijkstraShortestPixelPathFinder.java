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

package de.unihalle.informatik.MiToBo.math.graphs;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;

import java.awt.geom.Point2D;
import java.util.Vector;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedWeightedPseudograph;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.*;

/**
 * Search shortest path between two pixels in an image using Dijkstra.
 * <p>
 * The image is interpreted as a graph with the pixels being the nodes and
 * edges referring to 8-neighborhood relations. Each edge gets as weight the 
 * intensity value of the target pixel. Optionally also a power of the 
 * intensity value or its exponential function value can be used as weight.
 * The algorithm seeks to minimize path cost, i.e., is looking for path along
 * dark pixels.
 * <p>
 * The path is found applying the Dijkstra graph search algorithm as 
 * implemented in the JGraphT library released under GPL by Barak Naveh and 
 * contributors. For details refer to the webpage of the library, 
 * <a href="https://jgrapht.org/">https://jgrapht.org/</a>, and to the API 
 * documentation of the 
 * <a href="https://jgrapht.org/javadoc/org/jgrapht/alg/shortestpath/DijkstraShortestPath.html">
 * Dijkstra class</a>.
 * 
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,
		level=Level.APPLICATION)
public class DijkstraShortestPixelPathFinder extends MTBOperator {

	/**
	 * Modes how to set the weights.
	 */
	public static enum WeightModel {
		/**
		 * Edge weight is equal to intensity of edge target pixel.
		 */
		INTENSITY,
		/**
		 * Edge weight is equal to squared intensity of edge target pixel.
		 */
		INTENSITY_SQUARE,
		/**
		 * Edge weight is equal to cubic intensity of edge target pixel.
		 */
		INTENSITY_CUBIC,
		/**
		 * Edge weight is equal to exponential fuunction value of intensity 
		 * of edge target pixel.
		 */
		INTENSITY_EXPONENTIAL
	}
	
	/**
	 * Input gray-scale image.
	 * <p>
	 * By default, intensity values are directly interpreted as costs, i.e.,
	 * the algorithm is searching for paths along dark pixels. If this is not
	 * intended, select option. {@link #invertPixelValues} to invert values.
	 */
	@Parameter( label= "Input Image", required = true, dataIOOrder = -10,
		direction = Parameter.Direction.IN, description = "Input image.")
	protected transient MTBImage inImg = null;

	/**
	 * If true image values are inverted, i.e., the algorithm seeks to find a 
	 * path along bright pixels.
	 */
	@Parameter( label= "Invert Pixel Values?", required = true, dataIOOrder = -1,
		direction = Parameter.Direction.IN, 
		description = "Invert pixel values, i.e., favor bright pixels.")
	protected boolean invertPixelValues = false;

	/**
	 * Model for the edge weights.
	 */
	@Parameter( label= "Weight Model", required = true, dataIOOrder = 0,
		direction = Parameter.Direction.IN, description = "Edge weight model.")
	protected WeightModel weightModel = WeightModel.INTENSITY;

	/**
	 * Start pixel of the path.
	 */
	@Parameter( label= "Start Pixel", required = true, dataIOOrder = 1,
		direction = Parameter.Direction.IN, description = "Start pixel.")
	protected Point2D.Double startPoint = new Point2D.Double(1,	35);

	/**
	 * End pixel of the path.
	 */
	@Parameter( label= "End Pixel", required = true, dataIOOrder = 2,
		direction = Parameter.Direction.IN, description = "Result image.")
	protected Point2D.Double endPoint = new Point2D.Double(55, 73);

	/**
	 * Threshold for nodes to be included, nodes with an intensity above 
	 * the threshold are removed from the graph.
	 * <p>
	 * By default the threshold is Double.MAX_VALUE and all nodes are considered. 
	 * Omitting nodes (and corresponding edges) has a positive effect on runtime.
	 */
	@Parameter( label= "Node Threshold", required = true, dataIOOrder = 3,
		direction = Parameter.Direction.IN, 
		description = "Only intensities below threshold (or above if "
				+ "inverted mode is active) are considered.")
	protected double nodeThreshold = Double.MAX_VALUE;

	/**
	 * Result image with path overlay, start pixel in green, end pixel in blue.
	 */
	@Parameter( label= "Result image", direction = Parameter.Direction.OUT, 
		dataIOOrder = 1, description = "Result image with path overlay.")
	protected transient MTBImageRGB resultImg = null;

	/**
	 * Result path represented as point list.
	 */
	@Parameter( label= "Result path", direction = Parameter.Direction.OUT, 
		dataIOOrder = 2, description = "Result path represented as point list.")
	protected Vector<Point2D.Double> resultPath;
	
	/**
	 * Costs of the result path.
	 */
	@Parameter( label= "Result costs", direction = Parameter.Direction.OUT, 
		dataIOOrder = 3, description = "Costs of result path.")
	protected double resultCosts;
	
	// some local helper variables
	
	/**
	 * Maximum intensity value in working image, i.e. after potential inversion
	 * of image intensity values.
	 */
	private double workImgMaxVal;
	
	/**
	 * Default constructor.
	 * @throws ALDOperatorException Thrown in case of failure.
	 */
	public DijkstraShortestPixelPathFinder() throws ALDOperatorException {
		// nothing to do here
	}		

	/**
	 * Set input image to process.
	 * @param img Input image to process.
	 */
	public void setInputImage(MTBImage img) {
		this.inImg = img;
	}
	
	/**
	 * Enable/disable pixel values invertion.
	 * @param b	If true, pixel intensities are inverted.
	 */
	public void setInvertPixelValues(boolean b) {
		this.invertPixelValues = b;
	}
	
	/**
	 * Select model for choosing edge weights.
	 * @param w		Model to apply.
	 */
	public void setWeightModel(WeightModel w) {
		this.weightModel = w;
	}
	
	/**
	 * Specify start pixel of path.
	 * @param sp	Start pixel.
	 */
	public void setStartPixel(Point2D.Double sp) {
		this.startPoint = sp;
	}
	
	/**
	 * Specify end pixel of path.
	 * @param ep	End pixel.
	 */
	public void setEndPixel(Point2D.Double ep) {
		this.endPoint = ep;
	}

	/**
	 * Set threshold for filtering nodes.
	 * @param t	Threshold to apply.
	 */
	public void setNodeThreshold(double t) {
		this.nodeThreshold = t;
	}
	
	/**
	 * Returns the result image with path overlay.
	 * @return Overlay image.
	 */
	public MTBImageRGB getResultImage() {
		return this.resultImg;
	}

	/**
	 * Returns the extracted shortest path as point list.
	 * @return	Result path with minimal cost, null if none was found.
	 */
	public Vector<Point2D.Double> getResultPath() {
		return this.resultPath;
	}
	
	/**
	 * Returns the costs of the shortest path. 
	 * @return	Costs of optimal path, NaN in case that no path was found.
	 */
	public double getResultCosts() {
		return this.resultCosts;
	}
	
	/* (non-Javadoc)
	 * @see de.unihalle.informatik.Alida.operator.ALDOperator#operate()
	 */
	@Override
	protected void operate() {

		// some local variables
		int width = this.inImg.getSizeX();
		int height = this.inImg.getSizeY();

		// create work image, from now on always small intensity values are good!
		MTBImage workImage = this.inImg.duplicate();
		double maxVal = workImage.getMinMaxDouble()[1], newval;
		this.workImgMaxVal = 0;
		
		double workThresh;
		if (this.invertPixelValues) {
			for (int y=0; y<height; ++y) {
				for (int x=0; x<width; ++x) {
					newval = maxVal - workImage.getValueDouble(x, y);
					workImage.putValueDouble(x, y, newval);
					if (newval > this.workImgMaxVal) {
						this.workImgMaxVal = newval;
					}
				}
			}
			workThresh = maxVal - this.nodeThreshold;
		}
		else {
			workThresh = this.nodeThreshold;
			this.workImgMaxVal = maxVal;
		}
		
		// graph object
		DirectedWeightedPseudograph<GraphNode, DefaultEdge> swg = 
				new DirectedWeightedPseudograph<>(DefaultEdge.class);
		
		// array to store the nodes, some entries might be null
		GraphNode[][] nodes = new GraphNode[height][width];

		// construct graph, include only nodes with values below threshold
		for (int y=0; y<height; ++y) {
			for (int x=0; x<width; ++x) {
				if (workImage.getValueDouble(x, y) <= workThresh) {
					nodes[y][x] = new GraphNode(x,y); 
					swg.addVertex(nodes[y][x]);
				}
				else
					nodes[y][x] = null;
			}
		}
		
		// add edges
		DefaultEdge e;
		double val_1, val_2;
		for (int y=0; y<height; ++y) {
			for (int x=0; x<width; ++x) {
				
				// absolute value of node too small, skip it
				if (nodes[y][x] == null)
					continue;
				
				val_1 = workImage.getValueDouble(x, y);
				if (y+1 < height) {
					if (x-1 >= 0) {
						if (nodes[y+1][x-1] != null) {
							val_2 = workImage.getValueDouble(x-1, y+1);
							e = swg.addEdge(nodes[y][x], nodes[y+1][x-1]);
							swg.setEdgeWeight(e, this.getWeight(val_1, val_2));
							
							e = swg.addEdge(nodes[y+1][x-1], nodes[y][x]);
							swg.setEdgeWeight(e, this.getWeight(val_2, val_1));
						}
					}
					if (nodes[y+1][x] != null) {
						val_2 = workImage.getValueDouble(x, y+1);
						e = swg.addEdge(nodes[y][x], nodes[y+1][x]);						
						swg.setEdgeWeight(e, this.getWeight(val_1, val_2));
						
						e = swg.addEdge(nodes[y+1][x], nodes[y][x]);	
						swg.setEdgeWeight(e, this.getWeight(val_2, val_1));
					}
					if (x+1 < width) {
						if (nodes[y+1][x+1] != null) {
							val_2 = workImage.getValueDouble(x+1, y+1);
							e = swg.addEdge(nodes[y][x], nodes[y+1][x+1]);						
							swg.setEdgeWeight(e, this.getWeight(val_1, val_2));
							
							e = swg.addEdge(nodes[y+1][x+1], nodes[y][x]);						
							swg.setEdgeWeight(e, this.getWeight(val_2, val_1));
						}
					}
				}
				if (x+1 < width) {
					if (nodes[y][x+1] != null) {
						val_2 = workImage.getValueDouble(x+1, y);
						e = swg.addEdge(nodes[y][x], nodes[y][x+1]);
						swg.setEdgeWeight(e, this.getWeight(val_1, val_2));
						
						e = swg.addEdge(nodes[y][x+1], nodes[y][x]);
						swg.setEdgeWeight(e, this.getWeight(val_2, val_1));
					}
				}
			}
		}
		
		// init result image
		this.resultImg = (MTBImageRGB) MTBImage.createMTBImage(
				width, height, 1, 1, 1, MTBImageType.MTB_RGB);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				this.resultImg.putValueR(x, y, this.inImg.getValueInt(x, y));
				this.resultImg.putValueG(x, y, this.inImg.getValueInt(x, y));
				this.resultImg.putValueB(x, y, this.inImg.getValueInt(x, y));
			}
		}

		// extract path (no radius is applied)
		DijkstraShortestPath<GraphNode, DefaultEdge> dsp = 
				new DijkstraShortestPath<>(swg, Double.POSITIVE_INFINITY);
		GraphPath<GraphNode, DefaultEdge> gp = dsp.getPath(
			nodes[(int)this.startPoint.y][(int)this.startPoint.x], 
				nodes[(int)this.endPoint.y][(int)this.endPoint.x]);

		if (gp != null) {
			this.resultPath = new Vector<>();
			for (GraphNode n : gp.getVertexList()) {
				this.resultPath.add(new Point2D.Double(n.xc, n.yc));
			}
			this.resultCosts = dsp.getPathWeight(
				nodes[(int)this.startPoint.y][(int)this.startPoint.x], 
					nodes[(int)this.endPoint.y][(int)this.endPoint.x]);
		}
		else {
			this.resultPath = null;
			this.resultCosts = Double.NaN;
		}
		
		// overlay path to output image
		if (gp != null) {
			for (GraphNode n : gp.getVertexList()) {
				int nxc = n.xc;
				int nyc = n.yc;
				this.resultImg.putValueR(nxc, nyc, 255);
				this.resultImg.putValueG(nxc, nyc, 0);
				this.resultImg.putValueB(nxc, nyc, 0);
			}
			// start point in green
			int sx = gp.getVertexList().get(0).xc;
			int sy = gp.getVertexList().get(0).yc;
			for (int dy = -1; dy<=1; ++dy) {
				for (int dx = -1; dx<=1; ++dx) {
					if (dx != 0 && dy != 0)
						continue;
					if (sx+dx >= 0 && sx+dx < width && sy+dy >= 0 && sy+dy < height) {
						this.resultImg.putValueR(sx+dx, sy+dy, 0);
						this.resultImg.putValueG(sx+dx, sy+dy, 255);
						this.resultImg.putValueB(sx+dx, sy+dy, 0);
					}
				}
			}
			// end point in blue
			int length = gp.getVertexList().size();
			int ex = gp.getVertexList().get(length-1).xc;
			int ey = gp.getVertexList().get(length-1).yc;
			for (int dy = -1; dy<=1; ++dy) {
				for (int dx = -1; dx<=1; ++dx) {
					if (dx != 0 && dy != 0)
						continue;
					if (ex+dx >= 0 && ex+dx < width && ey+dy >= 0 && ey+dy < height) {
						this.resultImg.putValueR(ex+dx, ey+dy, 0);
						this.resultImg.putValueG(ex+dx, ey+dy, 0);
						this.resultImg.putValueB(ex+dx, ey+dy, 255);
					}
				}
			}
		}
	}
	
	/**
	 * Return edge weight for edge between given nodes.
	 * <p>
	 * We assume here that small intensity values of the target node are good,
	 * i.e., the edge towards the pixel should ge a small weight.
	 * 
	 * @param v1	Source node of edge.
	 * @param v2	Target node of edge.
	 * @return	Weight for edge according to selected weight model.
	 */
	private double getWeight(@SuppressWarnings("unused") double v1, double v2) {
		
		double weight = v2; 
		
		switch(this.weightModel)
		{
		case INTENSITY:
			return weight;
		case INTENSITY_SQUARE:
			return weight * weight;
		case INTENSITY_CUBIC:
			return weight * weight * weight;
		case INTENSITY_EXPONENTIAL:
			return Math.exp(weight);
		default:
			return weight;
		}
	}
	
	/**
	 * Local class to represent the nodes of the graph.
	 */
	protected class GraphNode {

		/**
		 * x-coordinate associated with the node.
		 */
		public int xc;
		
		/**
		 * y-coordinate associated with the node.
		 */
		public int yc;
		
		/**
		 * Default constructor.
		 * @param x		Coordinate in x-direction.
		 * @param y		Coordinate in y-direction.
		 */
		public GraphNode(int x, int y) {
			this.xc = x;
			this.yc = y;
		}
		
	}
}
