package fc.collimation.helper;

public class hystThresh 
{

	int[] input;
	int[] output;
	//int progress;
	int width;
	int height;
	static int lower;
	static int upper;
	int nHystStackSize = 0;
	int nMaxLen ;


	public void init(int[] inputIn, int widthIn, int heightIn, int lowerIn, int upperIn, int[] output_in) {
		width=widthIn;
		height=heightIn;
		//input = new int[width*height];
		output = output_in;
		input=inputIn;
		lower=lowerIn;
		upper=upperIn;
		// nMaxLen = (int)Math.sqrt(width*width + height*height);
		// since the function to be used to detect circle,
		// we can assume the max length of the connected dots are 2pi*r  
		nMaxLen = (int) (2*Math.PI * Math.min(width,height)/2);
	}
	public void process() 
	{
		for(int x=0;x<width;x++) 
		{
			for(int y=0;y<height;y++) 
			{
				int value = (input[y*width+x]) & 0xff;
				if (value >= upper) 
				{
					input[y*width+x] = 0xffffffff;
					hystConnect(x, y);
				}
			}
		}

		for (int n=0;n<input.length;n++)
		{
			if (input[n] == 0xffffffff)
			{
				output[n] = 0xffffffff;
			}
			else
			{
				output[n] = 0xff000000;
			}
		}
	}
	private void hystConnect(int x, int y) 
	{
		if (nHystStackSize > nMaxLen) return;
		nHystStackSize++;
		
		int value = 0;
		for (int x1=x-1;x1<=x+1;x1++) 
		{
			for (int y1=y-1;y1<=y+1;y1++) 
			{
				if ((x1 < width) & (y1 < height) & (x1 >= 0) & (y1 >= 0) & (x1 != x) & (y1 != y)) 
				{
					value = (input[y1*width+x1])  & 0xff;
					if (value != 255) 
					{
						if (value >= lower) 
						{
							input[y1*width+x1] = 0xffffffff;
							hystConnect(x1, y1);
						} 
						else 
						{
							input[y1*width+x1] = 0xff000000;
						}
					}
				}
			}
		}

	}	
}
