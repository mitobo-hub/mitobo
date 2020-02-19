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

package de.unihalle.informatik.MiToBo.core.datatypes;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.io.File;
import java.util.Vector;

import de.unihalle.informatik.Alida.dataio.provider.swing.components.ALDTableWindow.TableModelDelimiter;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBNeuriteSkelGraphNode.MTBSkeletonNodeType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageRGB;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.gui.MTBTableModel;
import de.unihalle.informatik.MiToBo.morphology.DistanceTransform;
import de.unihalle.informatik.MiToBo.morphology.DistanceTransform.DistanceMetric;
import de.unihalle.informatik.MiToBo.morphology.DistanceTransform.ForegroundColor;

/**
 * <pre>
 * The class implements a neurite skeleton graph (NSG), based on the skeleton
 * of a neurite region. The NSG is directed, in the way that exact one START
 * node (mostly the point near the neuron cell body area) exists, which can
 * have one or more output edges and no input edges. By the way, there are some
 * other nodes from type BRANCH or END inside the graph.
 * 
 * BRANCH nodes: have one input edge and two or more out edges
 * END nodes: have exact one input edge and no output edges 
 * 
 * The NSG is build by skeleton traversing, beginning at one
 * given node. The direction is implicit corrected, so that at every time there
 * is a direct "flow" from the START node to all END nodes. So the START node is
 * connected with every END node via exact one path.
 * 
 * If the START nodes is changed, the directness of the graph is automatically
 * corrected, and the old node becomes automatically a BRANCH or a END node.
 * 
 * The edges hold the pixels of the skeleton between the nodes.
 * It is possible to get all paths from the START node to all END nodes,
 * get the longest path in the whole graph, and several other operations,
 * needed for a NSG.
 * 
 * The graph is always free of circles. If an circle exists in the underlying
 * skeleton, the circle is disconnected in two paths
 * (due to the approach of building the graph).
 * 
 * </pre>
 * 
 * @see MTBGraph
 * @see MTBGraphEdge
 * @see MTBNeuriteSkelGraphNode
 * 
 * 
 * @author Danny Misiak
 * 
 */
public class MTBNeuriteSkelGraph extends MTBGraph {

		/**
		 * Skeleton image size in x-direction.
		 */
		private int width;
		/**
		 * Skeleton image size in y-direction.
		 */
		private int height;

		/**
		 * Region ID of the corresponding neurite region to this neurite skeleton
		 * graph.
		 */
		private int regionID;

		// define some colors
		/**
		 * The color of the skeleton in the binary image. The background is set to the
		 * opposite color, for example the skeleton is 0, then the background is set
		 * to 255 and vice versa.
		 */
		private int skeleton_color = 0;
		private int background_color = 255;
		/**
		 * Color of pre-detected branch points.
		 */
		private final int color_skelBranch = 100;
		/**
		 * Color of pre-detected special points, for example a 2x2 skeleton region.
		 */
		private final int color_skelSpecial = 90;
		/**
		 * Color of graph edge points.
		 */
		private final int color_graphEdge = 150;
		/**
		 * Color of graph node points.
		 */
		private final int color_graphNode = 60;
		/**
		 * Color of visited graph edge points.
		 */
		private final int color_skelVisitE = 20;
		/**
		 * Color of visited graph branch points.
		 */
		private final int color_skelVisitB = 10;
		/**
		 * X- and Y-coordinate differences of the 8-way neighbors.
		 */
		private final int[][] delta = { { 0, -1 }, { 1, -1 }, { 1, 0 }, { 1, 1 },
		    { 0, 1 }, { -1, 1 }, { -1, 0 }, { -1, -1 } };

		private int maxSpineLength = 0;

		/**
		 * Standard constructor. Creates an empty directed MTBNeuriteSkelGraph.
		 * Maximum spine length is set to 0.
		 */
		public MTBNeuriteSkelGraph() {
				super(true);
				this.regionID = -1;
				this.maxSpineLength = 0;
		}

		/**
		 * Constructor that creates an empty directed MTBNeuriteSkelGraph with defined
		 * maximum spine length.
		 * 
		 * @param _maxSpineLength
		 */
		public MTBNeuriteSkelGraph(int _maxSpineLength) {
				super(true);
				this.regionID = -1;
				this.maxSpineLength = _maxSpineLength;
		}

		/**
		 * Get skeleton graph image size in x direction.
		 */
		public int getWidth() {
				return this.width;
		}

		/**
		 * Get skeleton graph image size in y direction.
		 */
		public int getHeight() {
				return this.height;
		}

		/**
		 * Constructor to create an directed MTBNeuriteSkelGraph with the given
		 * MTBSkeletonGraphNodes and MTBGraphEdges.
		 * 
		 * @param skelNodes
		 *          vector of MTBNeuriteSkelGraphNodes
		 * @param skelEdges
		 *          vector of MTBGraphEdges
		 * @throws MTBNeuriteSkelGraphException
		 */
		public MTBNeuriteSkelGraph(
		    Vector<MTBNeuriteSkelGraphNode<Point2D.Double>> skelNodes,
		    Vector<MTBGraphEdge> skelEdges, int _maxSpineLength, int _width,
		    int _heigth) throws MTBNeuriteSkelGraphException {
				this.nodes = new Vector<MTBGraphNode<?>>(skelNodes.size());
				for (int i = 0; i < skelNodes.size(); i++) {
						this.nodes.addElement(skelNodes.elementAt(i));
				}
				this.regionID = -1;
				this.edges = skelEdges;
				this.directed = true;
				this.numberOfNodes = this.nodes.size();
				this.numberOfEdges = this.edges.size();
				this.maxSpineLength = _maxSpineLength;
				this.width = _width;
				this.height = _heigth;
				// test for existing START node
				if (getStartNode() == null) {
						throw new MTBNeuriteSkelGraphException(
						    ">>>>>>> Exactly one node of type MTBSkeletonNodeType.START must exist!");
				}
				// correct directness of the neurite skeleton graph, according to the
				// definition
				correctDirectness(getStartNode());
		}

		@SuppressWarnings("unchecked")
		@Override
		public MTBNeuriteSkelGraph clone() {
				Vector<MTBNeuriteSkelGraphNode<Point2D.Double>> tmpNodes = new Vector<MTBNeuriteSkelGraphNode<Point2D.Double>>(
				    this.nodes.size());
				for (int i = 0; i < this.nodes.size(); i++) {
						tmpNodes.addElement((MTBNeuriteSkelGraphNode<Point2D.Double>) this.nodes
						    .elementAt(i));
				}
				MTBNeuriteSkelGraph graph = null;
				try {
						graph = new MTBNeuriteSkelGraph(tmpNodes,
						    (Vector<MTBGraphEdge>) this.edges, this.maxSpineLength, this.width,
						    this.height);
				} catch (MTBNeuriteSkelGraphException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
				}
				return graph;
		};

		/**
		 * Get all nodes of the MTBNeuriteSkelGraph as MTBNeuriteSkelGraphNodes.
		 * 
		 * @return Vector of MTBNeuriteSkelGraphNode.
		 */
		@SuppressWarnings("unchecked")
		public Vector<MTBNeuriteSkelGraphNode<Point2D.Double>> getSkeletonGraphNodes() {
				Vector<MTBNeuriteSkelGraphNode<Point2D.Double>> theNodes = new Vector<MTBNeuriteSkelGraphNode<Point2D.Double>>(
				    this.nodes.size());
				for (int i = 0; i < this.nodes.size(); i++) {
						theNodes.addElement((MTBNeuriteSkelGraphNode<Point2D.Double>) this.nodes
						    .elementAt(i));
				}
				return theNodes;
		}

		/**
		 * Get the start node of the MTBNeuriteSkelGraph.
		 * 
		 * @return MTBNeuriteSkelGraphNode of type START.
		 */
		@SuppressWarnings("unchecked")
		public MTBNeuriteSkelGraphNode<Point2D.Double> getStartNode() {
				// check all nodes
				for (int i = 0; i < this.nodes.size(); i++) {
						MTBNeuriteSkelGraphNode<Point2D.Double> n = (MTBNeuriteSkelGraphNode<Point2D.Double>) this.nodes
						    .elementAt(i);
						// search for node of type START
						if (n.getNodeType() == MTBSkeletonNodeType.START)
								return n;
				}
				return null;
		}

		/**
		 * Set a new start node. For the skeleton graph only one start node can exist!
		 * The node type for the old start node automatically becomes a node of type
		 * BRANCH or END, depending on the number of output edges. If no output edges
		 * exist, the nodes becomes a END node, if some output edges exit, the node
		 * becomes a BRANCH node. Direction correction of the edges is automatically
		 * applied too.
		 * 
		 * @param node
		 *          new start node
		 */
		public void setStartNode(MTBNeuriteSkelGraphNode<Point2D.Double> node) {
				MTBNeuriteSkelGraphNode<Point2D.Double> tmpNode = getStartNode();
				// check if node is currently the start node
				if (!(node.getData().equals(tmpNode.getData()))) {
						// correct graph directness
						correctDirectness(node);
						if (tmpNode.getOutEdges().size() > 0) {
								// old node becomes BRANCH if there are some output edges
								tmpNode.setNodeType(MTBSkeletonNodeType.BRANCH);
						} else {
								// old node becomes END if there are no output edges
								tmpNode.setNodeType(MTBSkeletonNodeType.END);
						}
						// set new START node
						node.setNodeType(MTBSkeletonNodeType.START);
				}
		}

		/**
		 * Get all branch nodes of the MTBNeuriteSkelGraph.
		 * 
		 * @return Vector of MTBNeuriteSkelGraphNode of type BRANCH.
		 */
		@SuppressWarnings("unchecked")
		public Vector<MTBNeuriteSkelGraphNode<Point2D.Double>> getBranchNodes() {
				Vector<MTBNeuriteSkelGraphNode<Point2D.Double>> branchNodes = new Vector<MTBNeuriteSkelGraphNode<Point2D.Double>>();
				// check all nodes
				for (int i = 0; i < this.nodes.size(); i++) {
						MTBNeuriteSkelGraphNode<Point2D.Double> n = (MTBNeuriteSkelGraphNode<Point2D.Double>) this.nodes
						    .elementAt(i);
						// search for node of type BRANCH
						if (n.getNodeType() == MTBSkeletonNodeType.BRANCH) {
								branchNodes.addElement(n);
						}
				}
				return branchNodes;
		}

		/**
		 * Get all end nodes of the MTBNeuriteSkelGraph.
		 * 
		 * @return Vector of MTBNeuriteSkelGraphNode of type END.
		 */
		@SuppressWarnings("unchecked")
		public Vector<MTBNeuriteSkelGraphNode<Point2D.Double>> getEndNodes() {
				Vector<MTBNeuriteSkelGraphNode<Point2D.Double>> endNodes = new Vector<MTBNeuriteSkelGraphNode<Point2D.Double>>();
				// check all nodes
				for (int i = 0; i < this.nodes.size(); i++) {
						MTBNeuriteSkelGraphNode<Point2D.Double> n = (MTBNeuriteSkelGraphNode<Point2D.Double>) nodes
						    .elementAt(i);
						// search for node of type END
						if (n.getNodeType() == MTBSkeletonNodeType.END) {
								endNodes.addElement(n);
						}
				}
				return endNodes;
		}

		/**
		 * Set the region id. This id indirectly links the neurite skeleton graph to a
		 * neurite region, where the skeleton is created from. So in another
		 * application the skeleton can be linked to a region and vice versa.
		 */
		public void setRegionID(int id) {
				this.regionID = id;
		}

		/**
		 * Get the region id. This id indirectly links the neurite skeleton graph to a
		 * neurite region, where the skeleton is created from. So in another
		 * application the skeleton can be linked to a region and vice versa.
		 */
		public int getRegionID() {
				return this.regionID;
		}

		/**
		 * <pre>
		 * Build the skeleton graph from a given binary skeleton image and a given
		 * starting point to build up the graph.
		 * 
		 * 
		 * Build definition:
		 * 
		 * The graph is build from a given starting point (mostly a end point of the
		 * skeleton, near the neuron cell body). All other points are marked
		 * automatically by building up the graph.
		 * Branch points are marked as BRANCH nodes, end points are marked as END
		 * nodes and edge points are added to the edge data structure.
		 * 
		 * BRANCH nodes: have one input edge and two or more out edges
		 * END nodes: have exact one input edge and no output edges 
		 * 
		 * By building up the graph, 4-way neighbors are visited first, diagonal
		 * neighbors are visited in the second step.
		 * 
		 * The graph is always free of circles. If an circle exists in the underlying
		 * skeleton, the circle is disconnected in two paths
		 * (due to the approach of building the graph).
		 * 
		 * </pre>
		 * 
		 * @param x
		 *          starting point coordinate in x-direction
		 * @param y
		 *          starting point coordinate in y-direction
		 * @param w
		 *          width of the skeleton image
		 * @param h
		 *          height of the skeleton image
		 * @param skeletonImg
		 *          the binary skeleton image
		 * @param skelColor
		 *          color of the skeleton, the background is set to the opposite
		 *          color, for example the skeleton is 0, then the background is set
		 *          to 255 and vice versa
		 * @return true if graph was successfully build
		 */
		public boolean buildSkeletonGraph(int x, int y, int w, int h,
		    MTBImageByte skeletonImg, int skelColor) {

				// set the color of the skeleton
				this.skeleton_color = skelColor;
				// set background color
				if (this.skeleton_color == 255) {
						this.background_color = 0;
				} else if (this.skeleton_color == 0) {
						this.background_color = 255;
				}

				if (skeletonImg.getValueInt(x, y) == skeleton_color) {
						// make a copy of the binary skeleton image
						MTBImageByte skelImg = (MTBImageByte) skeletonImg.duplicate();

						this.width = w;
						this.height = h;
						// mark the junction pixels in the skeleton
						skelImg = this.markBranchPoints(skelImg);
						// add first node as an end node because of the build definition
						MTBNeuriteSkelGraphNode<Point2D.Double> endNode = new MTBNeuriteSkelGraphNode<Point2D.Double>(
						    new Point2D.Double(x, y), MTBSkeletonNodeType.START);
						this.addNode(endNode);
						skelImg.putValueInt(x, y, color_graphNode);
						// build the graph from the given point
						return (build(x, y, skelImg, endNode));
				} else {
						return false;
				}
		}

		/**
		 * Internal method to create the graph from the skeleton image. Does the
		 * actual work to build up the neurite skeleton graph.
		 * 
		 * @param xx
		 *          point coordinate in x-direction
		 * @param yy
		 *          point coordinate in y-direction
		 * @param skelImg
		 *          the binary skeleton image
		 * @param parentNode
		 *          parent node of the current point
		 * @return true if graph was successfully build
		 */
		private boolean build(int xx, int yy, MTBImageByte skelImg,
		    MTBNeuriteSkelGraphNode<Point2D.Double> parentNode) {
				// set coordinates of given starting point to build up
				int x = xx;
				int y = yy;
				// set parent node
				MTBNeuriteSkelGraphNode<Point2D.Double> parent = parentNode;
				// initialize edge data
				Vector<Point2D.Double> edgeData = new Vector<Point2D.Double>();
				// add point to edge data if it is an edge point
				if (skelImg.getValueDouble(x, y) == color_graphEdge) {
						edgeData.addElement(new Point2D.Double(x, y));
				}
				// point count for the number of neighbors
				int pCount = 0;
				do {
						// search next pixel
						pCount = 0;
						for (int dx = -1; dx <= 1; dx++) {
								for (int dy = -1; dy <= 1; ++dy) {
										int nx = x + dx;
										int ny = y + dy;
										if (nx < 0 || nx >= this.width)
												continue;
										if (ny < 0 || ny >= this.height)
												continue;
										if (dx == 0 && dy == 0)
												continue;
										// edge pixel or branch point found
										if (skelImg.getValueInt(nx, ny) == skeleton_color
										    || skelImg.getValueInt(nx, ny) == color_skelBranch
										    || skelImg.getValueInt(nx, ny) == color_skelSpecial) {
												pCount++;
										}
								}
						}
						// no expansion, so an end node is reached
						if (pCount == 0) {
								skelImg.putValueInt(x, y, color_graphNode);
								// remove last added edge data because its an end node
								if (edgeData.size() > 0) {
										edgeData.removeElementAt((edgeData.size() - 1));
								}
								MTBNeuriteSkelGraphNode<Point2D.Double> newEnd = new MTBNeuriteSkelGraphNode<Point2D.Double>(
								    new Point2D.Double(x, y), MTBSkeletonNodeType.END);
								MTBGraphEdge newEdge = new MTBGraphEdge(parent, newEnd, edgeData);
								this.addNode(newEnd);
								if (edgeData.size() > 0) {
										this.addEdge(newEdge);
								}
								return true;
						}
						// simple case: unique expansion
						if (pCount == 1) {
								for (int dx = -1; dx <= 1; dx++) {
										for (int dy = -1; dy <= 1; ++dy) {
												int nx = x + dx;
												int ny = y + dy;
												if (nx < 0 || nx >= this.width)
														continue;
												if (ny < 0 || ny >= this.height)
														continue;
												if (dx == 0 && dy == 0)
														continue;
												// edge pixel found
												if (skelImg.getValueInt(nx, ny) == skeleton_color) {
														skelImg.putValueInt(nx, ny, color_graphEdge);
														edgeData.addElement(new Point2D.Double(nx, ny));
														dx = 2;
														dy = 2;
														x = nx;
														y = ny;
												}
												// special point found
												if (skelImg.getValueInt(nx, ny) == color_skelSpecial) {

														MTBNeuriteSkelGraphNode<Point2D.Double> newBranch = new MTBNeuriteSkelGraphNode<Point2D.Double>(
														    new Point2D.Double(nx, ny), MTBSkeletonNodeType.BRANCH);
														MTBGraphEdge newEdge = new MTBGraphEdge(parent, newBranch,
														    edgeData);
														this.addNode(newBranch);
														if (edgeData.size() > 0) {
																this.addEdge(newEdge);
														}
														dx = 2;
														dy = 2;
														x = nx;
														y = ny;
														this.build(nx, ny, skelImg, newBranch);
														pCount = -1;
												}
												// branch point found
												if (skelImg.getValueInt(nx, ny) == color_skelBranch) {
														skelImg.putValueInt(nx, ny, color_graphNode);
														MTBNeuriteSkelGraphNode<Point2D.Double> newBranch = new MTBNeuriteSkelGraphNode<Point2D.Double>(
														    new Point2D.Double(nx, ny), MTBSkeletonNodeType.BRANCH);
														MTBGraphEdge newEdge = new MTBGraphEdge(parent, newBranch,
														    edgeData);
														this.addNode(newBranch);
														if (edgeData.size() > 0) {
																this.addEdge(newEdge);
														}
														dx = 2;
														dy = 2;
														x = nx;
														y = ny;
														this.build(nx, ny, skelImg, newBranch);
														pCount = -1;
												}
										}
								}
						}
						//
						// special case: unique expansion but with an branch in the neighborhood
						//
						// |_|_|_|x|_|_|
						// |_|_|_|x|_|_|
						// |_|_|_|x|_|_|
						// |_|_|x|b|x|_|
						// |_|_|c|_|_|_|
						// | | |v| | | |
						//
						// x: skeleton, b: branch, c: current skeleton point, v: visited
						//
						// TODO update comment
						/*
						 * If normal point is in 4-way neighborhood ... if normal skeleton point
						 * is in diagonal neighborhood and branch point is in the 4-way
						 * neighborhood...if two skeleton points are found...
						 */
						if (pCount == 2 && (skelImg.getValueInt(x, y) != color_graphNode)) {
								// search for normal skeleton points in 4-way neighborhood
								Vector<Point2D.Double> visited = new Vector<Point2D.Double>();
								for (int i = 0; i < 8; i += 2) {
										// edge pixel found
										if (skelImg.getValueInt(x + delta[i][0], y + delta[i][1]) == skeleton_color) {
												skelImg.putValueDouble(x + delta[i][0], y + delta[i][1],
												    color_skelVisitE);
												visited.addElement(new Point2D.Double(x + delta[i][0], y
												    + delta[i][1]));
										}
								}
								// unique expansion
								if (visited.size() == 1) {
										skelImg.putValueInt((int) Math.round(visited.elementAt(0).x),
										    (int) Math.round(visited.elementAt(0).y), color_graphEdge);
										edgeData.addElement(visited.elementAt(0));

										x = (int) Math.round(visited.elementAt(0).x);
										y = (int) Math.round(visited.elementAt(0).y);
										pCount = pCount - 1;
								} else {
										for (int i = 0; i < visited.size(); i++) {
												skelImg.putValueInt((int) Math.round(visited.elementAt(i).x),
												    (int) Math.round(visited.elementAt(i).y), skeleton_color);
										}
										visited = new Vector<Point2D.Double>();
								}
						}
				} while (pCount == 1);
				// complex case: multiple path expansion
				if (pCount > 1) {
						// vector of visited neighbors
						Vector<Point2D.Double> visited = new Vector<Point2D.Double>();
						boolean special = false;

						//
						// ---------------------------------------------
						// Check for special skeleton points.
						// ---------------------------------------------
						//

						// TODO handle branch points near special points

						if (skelImg.getValueInt(x, y) == color_graphEdge) {
								for (int i = 0; i < 8; i++) {
										if (skelImg.getValueInt(x + delta[i][0], y + delta[i][1]) == color_skelSpecial) {
												special = true;
												MTBNeuriteSkelGraphNode<Point2D.Double> newBranch = new MTBNeuriteSkelGraphNode<Point2D.Double>(
												    new Point2D.Double(x + delta[i][0], y + delta[i][1]),
												    MTBSkeletonNodeType.BRANCH);
												// add node and edge data to the skeleton graph
												MTBGraphEdge newEdge = new MTBGraphEdge(parent, newBranch, edgeData);
												this.addNode(newBranch);
												if (edgeData.size() > 0) {
														this.addEdge(newEdge);
												}
												this.build(x + delta[i][0], y + delta[i][1], skelImg, newBranch);
												i = 8;
										}
								}
						}
						if (skelImg.getValueInt(x, y) == color_skelSpecial) {
								special = true;
								skelImg.putValueInt(x, y, color_graphNode);
								for (int i = 0; i < 8; i++) {
										if (skelImg.getValueInt(x + delta[i][0], y + delta[i][1]) == skeleton_color) {
												skelImg.putValueDouble(x + delta[i][0], y + delta[i][1],
												    color_skelVisitE);
												visited.addElement(new Point2D.Double(x + delta[i][0], y
												    + delta[i][1]));
										}
								}
								for (int i = 0; i < visited.size(); i++) {
										Point2D.Double vis = visited.elementAt(i);
										// mark as edge point
										if (skelImg.getValueInt((int) Math.round(vis.x),
										    (int) Math.round(vis.y)) == color_skelVisitE) {
												skelImg.putValueInt((int) Math.round(vis.x),
												    (int) Math.round(vis.y), skeleton_color);
												this.build(x, y, skelImg, parent);
										}
								}
						}

						//
						// ---------------------------------------------
						// Check for normal skeleton points.
						// ---------------------------------------------
						//
						if (!special) {
								//
								// ---------------------------------------------
								// First check neighbors in 4-way neighborhood.
								// ---------------------------------------------
								//
								visited = new Vector<Point2D.Double>();
								for (int i = 0; i < 8; i += 2) {
										// edge pixel found
										if (skelImg.getValueInt(x + delta[i][0], y + delta[i][1]) == skeleton_color) {
												skelImg.putValueDouble(x + delta[i][0], y + delta[i][1],
												    color_skelVisitE);
												visited.addElement(new Point2D.Double(x + delta[i][0], y
												    + delta[i][1]));
										}
										// branch point found
										if (skelImg.getValueInt(x + delta[i][0], y + delta[i][1]) == color_skelBranch) {
												skelImg.putValueDouble(x + delta[i][0], y + delta[i][1],
												    color_skelVisitB);
												visited.addElement(new Point2D.Double(x + delta[i][0], y
												    + delta[i][1]));
										}

								}
								// expansion in 4-way neighborhood
								for (int i = 0; i < visited.size(); i++) {
										Point2D.Double vis = visited.elementAt(i);

										// mark as edge point
										if (skelImg.getValueInt((int) Math.round(vis.x),
										    (int) Math.round(vis.y)) == color_skelVisitE) {
												skelImg.putValueInt((int) Math.round(vis.x),
												    (int) Math.round(vis.y), color_graphEdge);
												this.build((int) Math.round(vis.x), (int) Math.round(vis.y),
												    skelImg, parent);

										}
										// mark as branch point
										if (skelImg.getValueInt((int) Math.round(vis.x),
										    (int) Math.round(vis.y)) == color_skelVisitB) {
												// create new branch node
												skelImg.putValueInt((int) Math.round(vis.x),
												    (int) Math.round(vis.y), color_graphNode);
												MTBNeuriteSkelGraphNode<Point2D.Double> newBranch = new MTBNeuriteSkelGraphNode<Point2D.Double>(
												    new Point2D.Double((int) Math.round(vis.x),
												        (int) Math.round(vis.y)), MTBSkeletonNodeType.BRANCH);
												// add node and edge data to the skeleton graph
												MTBGraphEdge newEdge = new MTBGraphEdge(parent, newBranch, edgeData);
												this.addNode(newBranch);
												if (edgeData.size() > 0) {
														this.addEdge(newEdge);
												}
												this.build((int) Math.round(vis.x), (int) Math.round(vis.y),
												    skelImg, newBranch);
										}
								}
								// vector of visited neighbors
								visited = new Vector<Point2D.Double>();
								//
								// ---------------------------------------------
								// Now check neighbors in diagonal neighborhood.
								// ---------------------------------------------
								//
								for (int i = 1; i < 8; i += 2) {
										// edge pixel found
										if (skelImg.getValueInt(x + delta[i][0], y + delta[i][1]) == skeleton_color) {
												skelImg.putValueDouble(x + delta[i][0], y + delta[i][1],
												    color_skelVisitE);
												visited.addElement(new Point2D.Double(x + delta[i][0], y
												    + delta[i][1]));
										}
										// branch point found
										if (skelImg.getValueInt(x + delta[i][0], y + delta[i][1]) == color_skelBranch) {
												skelImg.putValueDouble(x + delta[i][0], y + delta[i][1],
												    color_skelVisitB);
												visited.addElement(new Point2D.Double(x + delta[i][0], y
												    + delta[i][1]));
										}
								}
								// expansion in diagonal neighborhood
								for (int i = 0; i < visited.size(); i++) {
										Point2D.Double vis = visited.elementAt(i);
										// mark as edge point
										if (skelImg.getValueInt((int) Math.round(vis.x),
										    (int) Math.round(vis.y)) == color_skelVisitE) {
												skelImg.putValueInt((int) Math.round(vis.x),
												    (int) Math.round(vis.y), color_graphEdge);
												this.build((int) Math.round(vis.x), (int) Math.round(vis.y),
												    skelImg, parent);

										}
										// mark as branch point
										if (skelImg.getValueInt((int) Math.round(vis.x),
										    (int) Math.round(vis.y)) == color_skelVisitB) {
												skelImg.putValueInt((int) Math.round(vis.x),
												    (int) Math.round(vis.y), color_graphNode);
												// create new branch bode
												MTBNeuriteSkelGraphNode<Point2D.Double> newBranch = new MTBNeuriteSkelGraphNode<Point2D.Double>(
												    new Point2D.Double((int) Math.round(vis.x),
												        (int) Math.round(vis.y)), MTBSkeletonNodeType.BRANCH);
												MTBGraphEdge newEdge = new MTBGraphEdge(parent, newBranch, edgeData);
												this.addNode(newBranch);
												if (edgeData.size() > 0) {
														this.addEdge(newEdge);
												}
												this.build((int) Math.round(vis.x), (int) Math.round(vis.y),
												    skelImg, newBranch);
										}
								}
						}
				}
				return true;
		}

		/**
		 * Method to correct the direction of every edge inside the skeleton graph
		 * from a given root node. The graph is directed like a tree. One root node
		 * and several branch and end nodes. Every end node can be visit via one
		 * directed path from the start (root) node.
		 */
		@SuppressWarnings("unchecked")
		private void correctDirectness(
		    MTBNeuriteSkelGraphNode<Point2D.Double> rootNode) {
				// only do work is graph is directed
				if (this.directed) {
						Vector<MTBGraphEdge> edges = rootNode.getInEdges();
						if (edges != null) {
								for (int i = 0; i < edges.size(); i++) {
										MTBGraphEdge e = edges.elementAt(i);
										// flip source and target node of the edge
										MTBNeuriteSkelGraphNode<Point2D.Double> tmpNode = (MTBNeuriteSkelGraphNode<Point2D.Double>) e
										    .getSrcNode();
										e.setSrcNode((MTBNeuriteSkelGraphNode<Point2D.Double>) e.getTgtNode());
										e.setTgtNode(tmpNode);
										// reverse edge data
										Vector<Point2D.Double> pixels = (Vector<Point2D.Double>) e.getData();
										Vector<Point2D.Double> reSortedPixels = new Vector<Point2D.Double>(
										    pixels.size());

										for (int j = (pixels.size() - 1); j >= 0; j--) {
												reSortedPixels.addElement(pixels.elementAt(j));
										}
										e.setData(reSortedPixels);
										// correct directness recursively
										correctDirectness((MTBNeuriteSkelGraphNode<Point2D.Double>) e
										    .getTgtNode());
										// change the in and out edges of the source and the target node
										e.getSrcNode().removeInEdge(e);
										e.getSrcNode().addOutEdge(e);
										e.getTgtNode().removeOutEdge(e);
										e.getTgtNode().addInEdge(e);
								}
						}
				}
		}

		/**
		 * <pre>
		 * 
		 * Get all pixels of the neurite skeleton graph from the nodes and edges.
		 * Pixel collection starts at the start node of the skeleton graph.
		 * 
		 * WARNING!!!
		 * The specific order within the list depends on the edge direction between
		 * the single nodes. Order of the single edges depends on the build process.
		 * 
		 * </pre>
		 * 
		 * @param pixels
		 *          empty list to be filled
		 */
		public void getAllPixels(Vector<Point2D.Double> pixels) {
				getAllPixels(pixels, getStartNode());
		}

		/**
		 * <pre>
		 * 
		 * Get all pixels of the neurite skeleton graph from the nodes and edges.
		 * 
		 * WARNING!!!
		 * The specific order within the list depends on the edges and their direction
		 * between the single nodes.
		 * Order of the single edges depends on the build process.
		 * 
		 * </pre>
		 * 
		 * @param pixels
		 *          empty list to be filled
		 * @param tmpNode
		 *          current node for pixel collection
		 */
		@SuppressWarnings("unchecked")
		private void getAllPixels(Vector<Point2D.Double> pixels,
		    MTBNeuriteSkelGraphNode<Point2D.Double> tmpNode) {
				// add pixel data of the current node
				pixels.addElement(tmpNode.getData());
				// only visit out edges, because of the special directionality of the
				// neurite skeleton graph edges
				Vector<MTBGraphEdge> allOutEdges = tmpNode.getOutEdges();
				for (int i = 0; i < allOutEdges.size(); i++) {
						MTBGraphEdge e = allOutEdges.elementAt(i);
						Vector<Point2D.Double> eData = (Vector<Point2D.Double>) e.getData();
						for (int j = 0; j < eData.size(); j++) {
								// add pixel data of the edge
								pixels.addElement(eData.elementAt(j));
						}
						// search for next node
						getAllPixels(pixels,
						    (MTBNeuriteSkelGraphNode<Point2D.Double>) e.getTgtNode());
				}
		}

		/**
		 * Get the pixels of the longest path of the neurite skeleton graph. The
		 * pixels are ordered from the START node to the corresponding END node of the
		 * path.
		 * 
		 * @param includeSpines
		 * @return Pixel of longest path.
		 */
		public Vector<Point2D.Double> getLongestPath(boolean includeSpines) {
				Vector<Vector<Point2D.Double>> allPaths = this.getAllPaths(includeSpines);
				int max = 0;
				int index = 0;
				for (int i = 0; i < allPaths.size(); i++) {
						int length = allPaths.elementAt(i).size();
						if (length > max) {
								max = length;
								index = i;
						}
				}
				return allPaths.elementAt(index);
		}

		/**
		 * Get the pixels of the shortest path of the neurite skeleton graph. The
		 * pixels are ordered from the START node to the corresponding END node of the
		 * path.
		 * 
		 * @param includeSpines
		 * @return Pixel of shortest path.
		 */
		public Vector<Point2D.Double> getShortestPath(boolean includeSpines) {
				Vector<Vector<Point2D.Double>> allPaths = this.getAllPaths(includeSpines);
				int min = Integer.MAX_VALUE;
				int index = 0;
				for (int i = 0; i < allPaths.size(); i++) {
						int length = allPaths.elementAt(i).size();
						if (length < min) {
								min = length;
								index = i;
						}
				}
				return allPaths.elementAt(index);
		}

		/**
		 * Get the pixels of all spine paths of the neurite skeleton graph. The pixels
		 * are ordered from the START node to the corresponding END node of the path.
		 * 
		 * @return Vector of spine paths.
		 */
		@SuppressWarnings("unchecked")
		public Vector<Vector<Point2D.Double>> getSpinePaths() {
				Vector<Vector<Point2D.Double>> allPaths = this.getAllPaths(true);
				Vector<Vector<Point2D.Double>> noSpinePaths = this.getAllPaths(false);

				// all current end-points of the graph
				Vector<MTBNeuriteSkelGraphNode<Point2D.Double>> endNodes = this
				    .getEndNodes();

				Vector<Point2D.Double> spineEndPoints = new Vector<Point2D.Double>();

				for (int i = 0; i < allPaths.size(); i++) {
						if (!noSpinePaths.contains(allPaths.elementAt(i))) {
								spineEndPoints.addElement(allPaths.elementAt(i).lastElement());
						}
				}

				Vector<Vector<Point2D.Double>> spinePaths = new Vector<Vector<Point2D.Double>>();

				for (int i = 0; i < endNodes.size(); i++) {
						MTBNeuriteSkelGraphNode<Point2D.Double> e = endNodes.elementAt(i);
						if (spineEndPoints.contains(e.getData())) {
								Vector<Point2D.Double> tmpSpine = (Vector<Point2D.Double>) e
								    .getInEdges().elementAt(0).getData();
								tmpSpine.addElement(e.getData());
								spinePaths.addElement(tmpSpine);
						}
				}

				return spinePaths;
		}

		/**
		 * Get the pixels of all paths of the neurite skeleton graph. The pixels are
		 * ordered from the START node to the corresponding END node of the path.
		 * 
		 * @param includeSpines
		 * @return Vector of all paths.
		 */
		@SuppressWarnings("unchecked")
		public Vector<Vector<Point2D.Double>> getAllPaths(boolean includeSpines) {

				// all current end-points of the graph
				Vector<MTBNeuriteSkelGraphNode<Point2D.Double>> endNodes = this
				    .getEndNodes();
				// vector for paths and their pixels
				Vector<Vector<Point2D.Double>> paths = new Vector<Vector<Point2D.Double>>();
				int size = endNodes.size();
				// do it for all remaining end-points in the list
				while (size > 0) {
						MTBNeuriteSkelGraphNode<Point2D.Double> tmpNode = endNodes.elementAt(0);
						// flag to traverse the path ore not
						boolean stop = false;
						// list only paths which ends NOT in spines
						if (!includeSpines) {
								// spines are such edges on end nodes that have a length lower than
								// maxSpineLength pixel
								if (tmpNode.getInEdges().elementAt(0).getCost() > this.maxSpineLength) {
										stop = false;
								} else {
										// if edge is shorter than maxSpineLength pixels it can be a spine or
										// a fragment of a growth cone use the longest fragment for growth
										// cone representation path
										MTBNeuriteSkelGraphNode<Point2D.Double> n = (MTBNeuriteSkelGraphNode<Point2D.Double>) tmpNode
										    .getInEdges().elementAt(0).getSrcNode();
										// search for node of type BRANCH
										if (n.getNodeType() == MTBSkeletonNodeType.BRANCH) {
												Vector<MTBGraphEdge> e = n.getOutEdges();
												int branchCount = 0;
												double max = 0.0;
												int indexLongEdge = 0;
												for (int j = 0; j < e.size(); j++) {
														MTBNeuriteSkelGraphNode<Point2D.Double> tNode = (MTBNeuriteSkelGraphNode<Point2D.Double>) e
														    .elementAt(j).getTgtNode();
														if (e.elementAt(j).getCost() > max) {
																max = e.elementAt(j).getCost();
																indexLongEdge = j;
														}
														if (tNode.getNodeType() == MTBSkeletonNodeType.BRANCH) {
																branchCount++;
														}
												}
												if (branchCount > 0) {
														stop = true;
												} else {
														Vector<Point2D.Double> P = new Vector<Point2D.Double>(
														    endNodes.size());
														for (int i = 0; i < endNodes.size(); i++) {
																P.addElement(endNodes.elementAt(i).data);
														}
														if (P.contains(e.elementAt(indexLongEdge).getTgtNode().data)) {

																tmpNode = (MTBNeuriteSkelGraphNode<Point2D.Double>) e
																    .elementAt(indexLongEdge).getTgtNode();
																stop = false;
														} else {
																stop = true;
														}
												}
										} else {
												stop = true;
										}
								}
						}
						// remove visited nodes from end-point list
						Point2D.Double tmpData = tmpNode.data;
						if (!(tmpData.equals(endNodes.elementAt(0).data))) {
								endNodes.removeElement(tmpNode);
								size--;
						}
						endNodes.removeElementAt(0);
						size--;
						// get paths by traversing the graph from the END nodes of the graph
						if (!stop) {
								Vector<Point2D.Double> p = new Vector<Point2D.Double>();
								this.getPath(p, tmpNode);
								// reverse pixel list, so that the path starts at the START node
								// position
								Vector<Point2D.Double> reSortedPixels = new Vector<Point2D.Double>(
								    p.size());
								for (int j = (p.size() - 1); j >= 0; j--) {
										reSortedPixels.addElement(p.elementAt(j));
								}
								// add currently found path to the path list
								paths.addElement(reSortedPixels);
						}
				}
				// return the list of the paths
				return paths;
		}

		// /**
		// * Method to prune the NSG. Pruning in this way means, that all edges from
		// an
		// * end node will be roved (also their end nodes) if they have an branch node
		// * as source node. So the NSG results in a simpler NSG.
		// *
		// * @return Pruned NSG.
		// */
		// @SuppressWarnings("unchecked")
		// public MTBNeuriteSkelGraph pruneGraph() {
		// // copy current graph for pruning
		// MTBNeuriteSkelGraph prunedGraph = this.clone();
		// // collect all end points
		// Vector<MTBNeuriteSkelGraphNode<Point2D.Double>> eNodes = prunedGraph
		// .getEndNodes();
		// // check if the source node of the end point's edge is a branch node
		// for (int i = 0; i < eNodes.size(); i++) {
		// MTBNeuriteSkelGraphNode<Point2D.Double> eN = eNodes.elementAt(i);
		// MTBGraphEdge edge = eN.getInEdges().elementAt(0);
		// MTBNeuriteSkelGraphNode<Point2D.Double> node =
		// (MTBNeuriteSkelGraphNode<Point2D.Double>) edge
		// .getSrcNode();
		// // delete edge and end point from graph if source node is a branch node
		// if (node.getNodeType() == MTBSkeletonNodeType.BRANCH) {
		// prunedGraph.removeNode(eN);
		// }
		// }
		//
		// Vector<MTBNeuriteSkelGraphNode<Point2D.Double>> bNodes = prunedGraph
		// .getBranchNodes();
		// for (int i = 0; i < bNodes.size(); i++) {
		// MTBNeuriteSkelGraphNode<Point2D.Double> bN = bNodes.elementAt(i);
		// int edgeCount = bN.getOutEdges().size();
		// if (edgeCount == 0) {
		// bN.setNodeType(MTBSkeletonNodeType.END);
		// }
		// }
		// MTBNeuriteSkelGraphNode<Point2D.Double> startNode = prunedGraph
		// .getStartNode();
		//
		// MTBImageByte graphImg = prunedGraph.toByteImage();
		// // create byte processor as basis for new binary processor
		// ByteProcessor bP = new ByteProcessor(width, height);
		// // draw neuron in black on a white background
		// for (int y = 0; y < this.height; y++) {
		// for (int x = 0; x < this.width; x++) {
		// bP.putPixel(x, y, graphImg.getValueInt(x, y));
		// }
		// }
		// // generate new binary processor
		// BinaryProcessor bbP = new BinaryProcessor(bP);
		// // create the skeleton of the neuron using the ImageJ skeletonize()
		// bbP.skeletonize();
		//
		// // transfer neuron skeleton from binary processor into the skeleton image
		// for (int y = 0; y < this.height; y++) {
		// for (int x = 0; x < this.width; x++) {
		// if (bbP.getPixel(x, y) == 0) {
		// graphImg.putValueInt(x, y, 0);
		// } else {
		// graphImg.putValueInt(x, y, 255);
		// }
		// }
		// }
		//
		// MTBNeuriteSkelGraph newPrunedGraph = new MTBNeuriteSkelGraph(
		// this.maxSpineLength);
		// newPrunedGraph.buildSkeletonGraph((int) startNode.getData().x,
		// (int) startNode.getData().y, this.width, this.height, graphImg, 0);
		//
		// // // TODO recalculate graoh without compolete new build up.
		// // // compute the new types of the remaining end nodes due to the pruning
		// // Vector<MTBNeuriteSkelGraphNode<Point2D.Double>> bNodes = prunedGraph
		// // .getBranchNodes();
		// // for (int i = 0; i < bNodes.size(); i++) {
		// // MTBNeuriteSkelGraphNode<Point2D.Double> bN = bNodes.elementAt(i);
		// // int edgeCount = bN.getOutEdges().size();
		// // if (edgeCount == 0) {
		// // bN.setNodeType(MTBSkeletonNodeType.END);
		// // } else if (edgeCount == 1) {
		// // // TODO node is now an edge pixel
		// // } else {
		// // // do nothing, node type still branch
		// // }
		// // }
		// // return pruned NSG
		// return newPrunedGraph;
		// }

		/**
		 * Internal method to traverse the path and return the pixel list.
		 */
		@SuppressWarnings("unchecked")
		private void getPath(Vector<Point2D.Double> p,
		    MTBNeuriteSkelGraphNode<Point2D.Double> n) {
				p.addElement(n.getData());
				if (n.getInEdges().size() > 0) {
						MTBGraphEdge e = n.getInEdges().elementAt(0);
						Vector<Point2D.Double> data = (Vector<Point2D.Double>) e.getData();
						for (int i = (data.size() - 1); i >= 0; i--) {
								p.addElement(data.elementAt(i));
						}
						getPath(p, (MTBNeuriteSkelGraphNode<Point2D.Double>) e.getSrcNode());
				}
		}

		/**
		 * Mark branch/special points as first step before building up the whole
		 * skeleton graph.
		 * 
		 * @param skelImg
		 *          binary skeleton image
		 * @return Skeleton image with labeled branch/special points.
		 */
		private MTBImageByte markBranchPoints(MTBImageByte skelImg) {
				Vector<Point> branchPoints = new Vector<Point>();
				for (int y = 0; y < this.height; y++) {
						for (int x = 0; x < this.width; x++) {
								if (skelImg.getValueInt(x, y) == skeleton_color) {
										/*
										 * Search for branch points. A branch point exists, if there are two
										 * or more switches between a background pixel and a skeleton pixel in
										 * a 8-way neighborhood.
										 */
										int gapCount = 0;
										int oldVal = 0;
										for (int i = 0; i < 8; i++) {
												int nx = x + delta[i][0];
												int ny = y + delta[i][1];
												if (nx < 0 || nx >= this.width)
														continue;
												if (ny < 0 || ny >= this.height)
														continue;
												int value = skelImg.getValueInt(nx, ny);
												if (value == this.skeleton_color) {
														oldVal = 1;
												}
												if (value == this.background_color) {
														if (oldVal == 1) {
																gapCount++;
														}
														oldVal = 0;
												}
										}
										int nx = x + delta[0][0];
										int ny = y + delta[0][1];

										int value = skelImg.getValueInt(nx, ny);

										if (value == this.skeleton_color) {
												oldVal = 1;
										}
										if (value == this.background_color) {
												if (oldVal == 1) {
														gapCount++;

												}
												oldVal = 0;
										}
										if (gapCount > 2) {
												branchPoints.addElement(new Point(x, y));
										}
										/*
										 * Search for special structure of a 2x2 square.
										 */
										int count = 0;

										for (int i = 2; i < 5; i++) {
												nx = x + delta[i][0];
												ny = y + delta[i][1];
												if (nx < 0 || nx >= this.width)
														continue;
												if (ny < 0 || ny >= this.height)
														continue;
												value = skelImg.getValueInt(nx, ny);
												if (value == this.skeleton_color) {
														count++;
												}
										}
										if (count == 3) {
												skelImg.putValueInt(x, y, color_skelSpecial);
										}

								}
						}
				}
				for (int i = 0; i < branchPoints.size(); i++) {
						Point p = branchPoints.elementAt(i);
						skelImg.putValueInt(p.x, p.y, color_skelBranch);
				}
				return skelImg;
		}

		/**
		 * Visualize the skeleton graph as RGB image. Edge points colored in bright
		 * green, END nodes colored in red, BRANCH nodes colored in blue and START
		 * nodes colored in green.
		 * 
		 * @return RGB labeled graph image.
		 */
		public MTBImageRGB toImage() {
				MTBImageRGB graphImage = (MTBImageRGB) MTBImage.createMTBImage(this.width,
				    this.height, 1, 1, 1, MTBImageType.MTB_RGB);
				// fill image white
				graphImage.fillWhite();
				return (toImage(graphImage));
		}

		/**
		 * Draw the skeleton graph in the given RGB image. Edge points colored in
		 * bright green, END nodes colored in red, BRANCH nodes colored in blue and
		 * START nodes colored in green.
		 * 
		 * @return Graph drawn in the given RGB image.
		 */
		@SuppressWarnings("unchecked")
		public MTBImageRGB toImage(MTBImageRGB rgbImage) {

				/*
				 * Draw data (points) of graph edges into the graph image, colored in bright
				 * green values.
				 */
				for (int i = 0; i < this.numberOfEdges; i++) {
						MTBGraphEdge e = this.edges.elementAt(i);
						Vector<Point2D.Double> edgeData = (Vector<Point2D.Double>) e.edgeData;
						int[] color = { 0, 200, 0 };
						MTBNeuriteSkelGraphNode<Point2D.Double> eNode = (MTBNeuriteSkelGraphNode<Double>) e
						    .getTgtNode();
						for (int j = 0; j < edgeData.size(); j++) {
								Point2D.Double data = edgeData.elementAt(j);
								rgbImage.putValue((int) Math.round(data.x), (int) Math.round(data.y),
								    color[0], color[1], color[2]);
						}
				}

				/*
				 * Draw data (points) of spines into the graph image, colored in red values.
				 */
				Vector<Vector<Point2D.Double>> spines = this.getSpinePaths();
				for (int i = 0; i < spines.size(); i++) {
						Vector<Point2D.Double> tmpSpine = spines.elementAt(i);
						for (int j = 0; j < tmpSpine.size(); j++) {
								Point2D.Double spinePoint = tmpSpine.elementAt(j);
								rgbImage.putValue((int) Math.round(spinePoint.x),
								    (int) Math.round(spinePoint.y), 200, 0, 0);
						}
				}

				/*
				 * Draw graph nodes into the graph image. The color of an node depends on
				 * its type. START node = green; END node = red; BRANCH node = blue. Nodes
				 * are drawn at the end to overlay with path pixels.
				 */
				for (int i = 0; i < this.numberOfNodes; i++) {
						MTBNeuriteSkelGraphNode<Point2D.Double> n = (MTBNeuriteSkelGraphNode<Point2D.Double>) this.nodes
						    .elementAt(i);
						switch (n.getNodeType()) {
						case START:
								rgbImage.putValue((int) Math.round(n.data.x),
								    (int) Math.round(n.data.y), 0, 255, 0);
								break;
						case END:
								rgbImage.putValue((int) Math.round(n.data.x),
								    (int) Math.round(n.data.y), 255, 0, 0);
								break;
						case BRANCH:
								rgbImage.putValue((int) Math.round(n.data.x),
								    (int) Math.round(n.data.y), 0, 0, 255);
								break;
						}
				}
				return rgbImage;
		}

		/**
		 * Draw the skeleton graph into a binary image. Background is filled with
		 * white, graph is drawn in black.
		 */
		public MTBImageByte toByteImage() {
				MTBImageByte graphImage = (MTBImageByte) MTBImage.createMTBImage(
				    this.width, this.height, 1, 1, 1, MTBImageType.MTB_BYTE);
				// fill image white
				graphImage.fillWhite();

				/*
				 * Draw data (points) of graph edges into the binary graph image.
				 */
				for (int i = 0; i < this.numberOfEdges; i++) {
						MTBGraphEdge e = this.edges.elementAt(i);
						Vector<Point2D.Double> edgeData = (Vector<Point2D.Double>) e.edgeData;
						MTBNeuriteSkelGraphNode<Point2D.Double> eNode = (MTBNeuriteSkelGraphNode<Double>) e
						    .getTgtNode();
						for (int j = 0; j < edgeData.size(); j++) {
								Point2D.Double data = edgeData.elementAt(j);
								graphImage.putValueInt((int) Math.round(data.x),
								    (int) Math.round(data.y), 0);
						}
				}
				/*
				 * Draw graph nodes into the binary graph image.
				 */
				for (int i = 0; i < this.numberOfNodes; i++) {
						MTBNeuriteSkelGraphNode<Point2D.Double> n = (MTBNeuriteSkelGraphNode<Point2D.Double>) this.nodes
						    .elementAt(i);
						graphImage.putValueInt((int) Math.round(n.data.x),
						    (int) Math.round(n.data.y), 0);
				}
				return graphImage;
		}

		/**
		 * Print the neurite skeleton graph informations.
		 */
		@SuppressWarnings("unchecked")
		@Override
		public void print() {
				// print all nodes
				System.out.println(" nodes:");
				for (int i = 0; i < this.nodes.size(); i++) {
						MTBNeuriteSkelGraphNode<Point2D.Double> n = (MTBNeuriteSkelGraphNode<Point2D.Double>) this.nodes
						    .elementAt(i);
						System.out.println("  " + n.toString());
				}
				System.out.println();
				// sum of all edge costs
				double graphCosts = 0;
				// print all edges
				System.out.println(" edges: (cost > 0)");
				for (int i = 0; i < this.edges.size(); i++) {
						MTBGraphEdge e = this.edges.elementAt(i);
						System.out.println("  " + e.toString());
						graphCosts += e.cost;
				}
				System.out.println();
				// print graph: all nodes with edges
				System.out.println(" graph:");
				System.out.println("  total costs: " + graphCosts);
				for (int i = 0; i < this.nodes.size(); i++) {
						MTBNeuriteSkelGraphNode<Point2D.Double> n = (MTBNeuriteSkelGraphNode<Point2D.Double>) this.nodes
						    .elementAt(i);
						Vector<MTBGraphEdge> e = n.getAllEdges();
						System.out.print("  " + n.getData().toString() + " --> ");
						for (int j = 0; j < e.size(); j++) {
								System.out.print(e.elementAt(j).getSrcNode().getData().toString()
								    + ", " + e.elementAt(j).getTgtNode().getData().toString() + "; ");
						}
						System.out.println();
				}
				System.out.println();
		}

		/**
		 * Method to save neurite skeleton graph as SWC file.
		 * 
		 * @param fileName
		 *          filename for saving the SWC file, if null, file will not be saved
		 * @param neuronImage
		 *          if radii for each skeleton point should be calculated, a image
		 *          must be given to calculate the distance map of the neuron region
		 *          and get radius of each skeleton point, can be null
		 * @return MTBTableModel Table with SWC file data.
		 */
		public MTBTableModel toSWC(String fileName, MTBImageByte neuronImage) {
				Vector<Point2D.Double> graphPixels = new Vector<Point2D.Double>();
				Vector<Integer> parentPixels = new Vector<Integer>();
				this.depthFirstSearch(graphPixels, parentPixels, this.getStartNode(), 0);

				// calculate distance map to get radius for each skeleton point
				double[][] distanceMap = new double[0][0];
				if (neuronImage != null) {
						DistanceTransform dt = null;
						try {
								dt = new DistanceTransform(neuronImage, DistanceMetric.EUCLIDEAN,
								    ForegroundColor.FG_BLACK);
								dt.runOp();
								distanceMap = dt.getDistanceMap();
						} catch (ALDOperatorException e) {
								System.out.println(">>>>>>> extraction failed @ distance transform");
								e.printStackTrace();
						} catch (ALDProcessingDAGException e) {
								System.out.println(">>>>>>> extraction failed @ distance transform");
								e.printStackTrace();
						}
				}

				// initialize table
				Vector<String> header = new Vector<String>();
				header.add("#id");
				header.add("t");
				header.add("x");
				header.add("y");
				header.add("z");
				header.add("r");
				header.add("p");

				MTBTableModel swcTable = new MTBTableModel(0, header.size(), header);
				for (int i = 0; i < graphPixels.size(); i++) {
						int row = swcTable.getRowCount();
						// set id
						swcTable.setValueAt(i + 1, row, 0);
						// set type
						swcTable.setValueAt(2, row, 1);
						// set x-coordinate
						swcTable.setValueAt(graphPixels.elementAt(i).x, row, 2);
						// set y-coordinate
						swcTable.setValueAt(graphPixels.elementAt(i).y, row, 3);
						// set z-coordinate
						swcTable.setValueAt(0, row, 4);
						// set radius
						if (neuronImage != null) {
								swcTable.setValueAt(
								    distanceMap[(int) graphPixels.elementAt(i).y][(int) graphPixels
								        .elementAt(i).x], row, 5);
						} else {
								swcTable.setValueAt(0, row, 5);
						}
						// set parent
						if (i == 0) {
								swcTable.setValueAt(-1, row, 6);
						} else {
								swcTable.setValueAt(parentPixels.elementAt(i), row, 6);
						}
				}
				// save table if file name exists
				if (fileName != null) {
						File swcFile = new File(fileName);
						swcTable.setDelimiter(TableModelDelimiter.SPACE);
						swcTable.saveTable(swcFile);
				}
				return swcTable;
		}

		/**
		 * Depth-first-search to collect data from the neurite skeleton graph in a SWC
		 * file like format.
		 * 
		 * @param pixels
		 *          empty vector where skeleton pixels will be stored
		 * @param parents
		 *          empty vector where parents of each skeleton pixels will be stored
		 * @param startNode
		 *          start node for traversing graph and collect data
		 * @param label
		 *          label for parent of start node (root)
		 */
		private void depthFirstSearch(Vector<Point2D.Double> pixels,
		    Vector<Integer> parents,
		    MTBNeuriteSkelGraphNode<Point2D.Double> startNode, int label) {
				// add pixel data of the current node
				pixels.addElement(startNode.getData());
				if (parents.size() < 1) {
						parents.add(new Integer(label));
				} else {
						parents.add(parents.size());
				}
				// update label
				label = pixels.size();
				// only visit out edges, because of the special directionality of the
				// neurite skeleton graph edges
				Vector<MTBGraphEdge> allOutEdges = startNode.getOutEdges();
				for (int i = 0; i < allOutEdges.size(); i++) {
						MTBGraphEdge e = allOutEdges.elementAt(i);
						Vector<Point2D.Double> eData = (Vector<Point2D.Double>) e.getData();
						for (int j = 0; j < eData.size(); j++) {
								if (j == 0) {
										parents.add(label);
								} else {
										parents.add(parents.size());
								}
								// add pixel data
								pixels.addElement(eData.elementAt(j));
						}
						// step deeper into graph
						this.depthFirstSearch(pixels, parents,
						    (MTBNeuriteSkelGraphNode<Point2D.Double>) e.getTgtNode(), label);
				}
		}
}
