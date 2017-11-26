package fc.collimation.helper;

public class gaussianFilter  
{
	byte[] input;
	byte[] output;
	float[] template;
	double sigma;
	int templateSize;
	int width;
	int height;


	public void init(byte[] original, byte[] output_in, int sigmaIn, int tempSize, int widthIn, int heightIn) 
	{
		if((tempSize%2)==0) templateSize=tempSize-1;
		sigma=(double)sigmaIn;
		templateSize=tempSize;
		width=widthIn;
		height=heightIn;
		input = original;
		output = output_in;
		template = new float[templateSize*templateSize];
	}

	public void generateTemplate() 
	{
		float center=(templateSize-1)/2;

		float total=0;

		for(int x = 0; x < templateSize; x++) 
		{
			for(int y = 0; y < templateSize; y++) 
			{
				template[x*templateSize+y] = (float)(1/(float)(2*Math.PI*sigma*sigma))*(float)Math.exp((float)(-((x-center)*(x-center)+(y-center)*(y-center))/(2*sigma*sigma)));
				total+=template[x*templateSize+y];
			}
		}
		for(int x = 0; x < templateSize; x++) 
		{
			for(int y = 0; y < templateSize; y++) 
			{
				template[x*templateSize+y] = template[x*templateSize+y]/total;
			}
		}
	}
	public void process() 
	{
		float sum;
		int w_smaller = (width-(templateSize-1));
		int h_smaller = (height-(templateSize-1));
		byte outputsmaller[] = new byte[w_smaller*h_smaller];

		for(int x=(templateSize-1)/2; x<width-(templateSize+1)/2;x++) 
		{
			for(int y=(templateSize-1)/2; y<height-(templateSize+1)/2;y++)
			{
				sum=0;
				for(int x1=0;x1<templateSize;x1++) 
				{
					for(int y1=0;y1<templateSize;y1++) 
					{
						int x2 = (x-(templateSize-1)/2+x1);
						int y2 = (y-(templateSize-1)/2+y1);
						float value = (input[y2*width+x2]) * (template[y1*templateSize+x1]);
						sum += value;
					}
				}
				outputsmaller[(y-(templateSize-1)/2)*(width-(templateSize-1))+(x-(templateSize-1)/2)] = (byte)Math.min(255, sum);
			}
		}

		int xs=0; 
		for (int x=0;x<width;x++)
		{
			if (x<w_smaller) xs++;

			int ys=0;
			for (int y=0;y<height;y++)
			{
				if (y<h_smaller) ys++;
				try
				{
					output[y*width +x] = outputsmaller[ys*w_smaller +xs];
				}catch (Exception e)
				{

				}
			}
		}
	}
}
