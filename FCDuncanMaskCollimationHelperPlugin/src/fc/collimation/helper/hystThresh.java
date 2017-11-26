package fc.collimation.helper;

public class hystThresh 
{

	byte[] input;
	byte[] output;
	//int progress;
	int width;
	int height;
	static byte lower;
	static byte upper;
	int nHystStackSize = 0;
	int nMaxLen ;


	public void init(byte[] inputIn, int widthIn, int heightIn, byte lowerIn, byte upperIn, byte[] output_in) {
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
				byte value = (input[y*width+x]);
				if (value >= upper) 
				{
					input[y*width+x] = (byte) 255;//0xffffffff;
					hystConnect(x, y);
				}
			}
		}

		for (int n=0;n<input.length;n++)
		{
			if (input[n] == (byte)255)//0xffffffff)
			{
				output[n] = (byte) 255;//0xffffffff;
			}
			else
			{
				output[n] = (byte)0;//0xff000000;
			}
		}
	}
	private void hystConnect(int x, int y) 
	{
		if (nHystStackSize > nMaxLen) return;
		nHystStackSize++;
		
		byte value = 0;
		for (int x1=x-1;x1<=x+1;x1++) 
		{
			for (int y1=y-1;y1<=y+1;y1++) 
			{
				if ((x1 < width) & (y1 < height) & (x1 >= 0) & (y1 >= 0) & (x1 != x) & (y1 != y)) 
				{
					value = (input[y1*width+x1]);//  & 0xff;
					if (value != (byte)255) 
					{
						if (value >= lower) 
						{
							input[y1*width+x1] = (byte) 255;
							hystConnect(x1, y1);
						} 
						else 
						{
							input[y1*width+x1] = (byte)0;//0xff000000;
						}
					}
				}
			}
		}

	}	
}
