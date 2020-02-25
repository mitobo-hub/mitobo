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

package de.unihalle.informatik.MiToBo.apps.minirhizotron.datatypes;

import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBTreeNodeData;

/**
 * Node data object for {@link MTBRootTree}. 
 * <p>
 * This class is intended to store data for representing plant 
 * roots in images. Its main purpose is the data exchange between
 * rhizoTrak and MiToBo.
 * 
 * @author Birgit Moeller
 */
@ALDDerivedClass
public class MTBRootTreeNodeData extends MTBTreeNodeData {

	/**
	 * y-position of the node in 2D image space.
	 */
	protected double xPos;

	/**
	 * y-position of the node in 2D image space.
	 */
	protected double yPos;
	
	/**
	 * Used for drawing the estimated diameter during Ridge Detection.
	 */
	protected double nx, ny;

	/**
	 * Layer where the node is located.
	 */
	protected int layer;

	/**	
	 *  Radius of the node
	 */
	protected double radius;

	/**
	 * Status associated with the node.
	 */
	protected byte status;

	/**
	 * List of connectors the corresponding treeline is attached to.class
	 * <p>
	 * Note: usually a treeline should only be member of one connector,
	 * only in case of conflicts there could be more.
	 */
	protected long[] connectorIDs;

	/**
	 * Default constructor, all members are initialized with zero.
	 */
	public MTBRootTreeNodeData() {

	}

	/**
	 * Constructor with positional data.
	 * @param x		x-position of the node.
	 * @param y		y-position of the node.
	 */
	public MTBRootTreeNodeData(double x, double y) {
		this.xPos = x;
		this.yPos = y;
	}

	/**
	 * Clone function.
	 */
	public MTBRootTreeNodeData clone() {
		MTBRootTreeNodeData nData = new MTBRootTreeNodeData(this.xPos, this.yPos);
		nData.layer = this.layer;
		nData.radius = this.radius;
		nData.status = this.status;
		nData.connectorIDs = this.connectorIDs.clone();
		return nData;
	}

	/**
	 * Get x-position of the node.
	 * @return	Position in x.
	 */
	public double getXPos() {
		return this.xPos;
	}

	/**
	 * Get y-position of the node.
	 * @return	Position in y.
	 */
	public double getYPos() {
		return this.yPos;
	}
	
	public double getNx() {
		return this.nx;
	}
	
	public double getNy() {
		return this.ny;
	}

	/**
	 * Get the layer where the node is located.
	 * @return	Layer.
	 */	
	public int getLayer() {
		return this.layer;
	}

	/**
	 * Get the radius of the node.
	 * @return	Node radius.
	 */
	public double getRadius() {
		return this.radius;
	}

	/**
	 * Get the status of the node (segment).
	 * @return	Status.
	 */
	public byte getStatus() {
		return this.status;
	}

	/**
	 * Get connector IDs.
	 * @return List of connectors.
	 */
	public long[] getConnectorIDs() {
		return this.connectorIDs;
	}

	/**
	 * Set x-position of the node.
	 * @param x		x-position.
	 */
	public void setXPos(double x) {
		this.xPos = x;
	}

	/**
	 * Set y-position of the node.
	 * @param y		y-position.
	 */
	public void setYPos(double y) {
		this.yPos = y;
	}
	
	public void setNx(double nx) {
		this.nx = nx;
	}
	
	public void setNy(double ny) {
		this.ny = ny;
	}

	/**
	 * Set the layer where the node is located.
	 * @param l		Layer.
	 */	
	public void setLayer(int l) {
		this.layer = l;
	}

	/**
	 * Set the radius of the node.
	 * @param	r		Node radius.
	 */
	public void setRadius(double r) {
		this.radius = r;
	}

	/**
	 * Set the status of the node (segment).
	 * @param s		Status.
	 */
	public void setStatus(byte s) {
		this.status = s;
	}

	/**
	 * Set connector IDs.
	 * @param cs	List of connectors.
	 */
	public void setConnectorIDs(long[] cs) {
		this.connectorIDs = cs;
	}

	@Override
	public void printData() {
		System.out.println("[layer " + this.layer + "] " + this.xPos + " , " + this.yPos
			+ " / r = " + this.radius + " / status = " + this.status);
	}
}
