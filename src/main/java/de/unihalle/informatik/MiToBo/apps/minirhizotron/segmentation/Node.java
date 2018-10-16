package de.unihalle.informatik.MiToBo.apps.minirhizotron.segmentation;

public class Node
{
	private int predecessor; 
	private double x;
	private double y;
	private double radius;
	
	// constructors
	public Node()
	{
		super();
	}
	
	public Node(double x, double y)
	{
		this.predecessor = 0;
		this.x = x;
		this.y = y;
		this.radius = 0.0;
	}
	
	public Node(int predecessor, double x, double y)
	{
		this.predecessor = predecessor;
		this.x = x;
		this.y = y;
		this.radius = 0.0;
	}
	
	public Node(int predecessor, double x, double y, double radius)
	{
		this.predecessor = predecessor;
		this.x = x;
		this.y = y;
		this.radius = radius;
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
	
	public double getRadius()
	{
		return this.radius;
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
	
	public void setRadius(double radius)
	{
		this.radius = radius;
	}
}
