package de.unihalle.informatik.MiToBo.apps.minirhizotron.segmentation;

public class Node
{
	private int predecessor; 
	private double x;
	private double y;
	private double diameter;
	private double nx;
	private double ny;
	
	// constructors
	public Node()
	{
		super();
	}
	
	public Node(double x, double y)
	{
		this.predecessor = -1;
		this.x = x;
		this.y = y;
		this.diameter = 0.0;
	}
	
	public Node(int predecessor, double x, double y)
	{
		this.predecessor = predecessor;
		this.x = x;
		this.y = y;
		this.diameter = 0.0;
	}
	
	public Node(int predecessor, double x, double y, double diameter)
	{
		this.predecessor = predecessor;
		this.x = x;
		this.y = y;
		this.diameter = diameter;
	}
	
	// getter	
	public int getPredecessor()
	{
		return this.predecessor;
	}
	
	public double getX()
	{
		return this.x;
	}
	
	public double getY()
	{
		return this.y;
	}
	
	public double getDiameter()
	{
		return this.diameter;
	}
	
	public double getNx()
	{
		return this.nx;
	}
	
	public double getNy()
	{
		return this.ny;
	}
	
	// setter
	public void setPredecessor(int predecessor)
	{
		this.predecessor = predecessor;
	}
	
	public void setX(double x)
	{
		this.x = x;
	}
	
	public void setY(double y)
	{
		this.y = y;
	}
	
	public void setDiameter(double diameter)
	{
		this.diameter = diameter;
	}
	
	public void setNx(double nx)
	{
		this.nx = nx;
	}
	
	public void setNy(double ny)
	{
		this.ny = ny;
	}
}
