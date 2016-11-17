import java.util.ArrayList;

public class DialCache 
{
	private ArrayList<ArrayList<Integer>> m_arr;	
	private int m_largestIndex;	
	
	public DialCache()
	{
		m_arr = new ArrayList<ArrayList<Integer>>(65536);	
		m_largestIndex = 0;			
	}
	
	/**
	 * Adds a spel with its affinity to the cache
	 * @param spelIndex Integer indicating the spel's index within the volume
	 * @param affinity 16-bit unsigned short (if we could do that) representing mapped affinity from [0,1]
	 */
	public void Push(int spelIndex, int affinity)
	{
		//Get list of spels that also have this affinity to o
		ArrayList<Integer> list = m_arr.get(0xFFFF & affinity);

		if(list == null)
			list = new ArrayList<Integer>();
		
		list.add(spelIndex);
		
		if(spelIndex > m_largestIndex)
			m_largestIndex = spelIndex;
	}
	
	public int Pop()
	{
		ArrayList<Integer> list = m_arr.get(m_largestIndex);
		
		int result = list.remove(0);
		
		if(result == m_largestIndex && list.size() == 0)
			UpdateLargestIndex();
		
		return result;
	}
	
	private void UpdateLargestIndex()
	{
		for(int i = 0; i >= 0; i--)
		{
			ArrayList<Integer> list = m_arr.get(i);
			
			if(list != null && list.size() != 0)
			{
				m_largestIndex = i;
				return;
			}				
		}		
		
		m_largestIndex = 0;
	}
}
