package fc.PrintingApplication.TP4;

import fc.Math.Vec3f;

public class Plane
{
	public Vec3f m_Point;
	public Vec3f m_Normal;
	
	public Plane(Vec3f point, Vec3f normal)
	{
		m_Point = new Vec3f(point.x, point.y, point.z);
		m_Normal = normal;
    }
	
	public float distanceToPoint(Vec3f p)
	{
		return 0;
    }
}

