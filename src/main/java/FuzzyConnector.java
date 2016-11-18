import ij.ImagePlus;

public class FuzzyConnector 
{
	ImagePlus m_img;
	int m_width;
	int m_height;
	int m_depth;
	
	DialCache m_dial;
	float[] m_conScene;
	int[] m_seeds;
	float m_avg;
	float m_sigma;
	
	private float gaussian(float val)
	{
		return 0;
	}
	
	private float affinity(int c, int d)
	{
		return 0;
	}
	
	private boolean adjacency(int c, int d)
	{
		return false;
	}
	
	public float[] run(ImagePlus image, Point3D[] seeds, float threshold)
	{
		return null;
	}
}
