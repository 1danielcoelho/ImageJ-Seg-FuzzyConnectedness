class Point3D
{
	int x;
	int y;
	int z;
	
	Point3D(int x, int y, int z)
	{
		this.x = x; this.y = y; this.z = z;
	}
	
	@Override
	public String toString() 
	{
		return "Point3D [x=" + x + ", y=" + y + ", z=" + z + "]";
	}
}