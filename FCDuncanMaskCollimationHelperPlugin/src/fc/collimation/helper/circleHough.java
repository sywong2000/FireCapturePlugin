package fc.collimation.helper;

public class circleHough 
{
	byte[] input;
	//int[] output;
	//float[] template={-1,0,1,-2,0,2,-1,0,1};;
	//double progress;
	int width;
	int height;
	long[][] acc;
	static int accSize=-1;
	long[] results;
	int r_min;
	int r_max;
	long[] maxtable;
	static double[] costable = null;
	static double[] sintable = null;

	public void init(byte[] inputIn, int widthIn, int heightIn, int radius_min, int radius_max, int NumOfMatches, double[] cos_t_in, double[] sin_t_in, long[][] acc_in, long[] max_t_in, long[] results_in) 
	{
		r_min = radius_min;
		r_max = radius_max;
		width=widthIn;
		height=heightIn;
		input=inputIn;
		results = results_in;

		acc = acc_in;//new int[r_max-r_min+1][width*height];

		accSize=NumOfMatches;
		
		if (results==null || results.length!=(accSize*4))
		{
			results = new long[accSize*4];
		}

		maxtable = max_t_in;//new int[r_max-r_min+1];

		costable = cos_t_in;
		sintable = sin_t_in;
	}

	// hough transform for lines (polar), returns the accumulator array

	public void process() 
	{

		// for polar we need accumulator of 180degress * the longest length in the image
		//int rmax = (int)Math.sqrt(width*width + height*height);
		//acc = new int[width * height];

		//		for(int x=0;x<width;x++) 
		//		{
		//			for(int y=0;y<height;y++) 
		//			{
		//				acc[x*width+y] =0 ;
		//			}
		//		}

		int x0, y0;
		//int max=0;

		for(int x=0;x<width;x++) 
		{
			for(int y=0;y<height;y++) 
			{
				if ((input[y*width+x])== (byte)255) 
				{
					// speed up the processing by matching 24 points (i.e. 360 degress /24 = 15) 
					for (int theta=0; theta<360; theta++) 
					{
						//t = (theta * 3.14159265) / 180;
						for (int rd = 0;rd<(r_max-r_min);rd++)
						{
							int radius = r_min+rd;
							x0 = (int)Math.round(x - (radius * costable[theta]));
							y0 = (int)Math.round(y - (radius * sintable[theta]));
							if(x0 < width && x0 > 0 && y0 < height && y0 > 0) 
							{
								acc[rd][x0 + (y0 * width)] += 1;
								maxtable[rd]= (acc[rd][x0 + (y0 * width)]>maxtable[rd])?acc[rd][x0 + (y0 * width)]:maxtable[rd];
							}
						}
					}
				}
			}
		}

		// now normalise to 255 and put in format for a pixel array


		// Find max acc value
		//		for(int x=0;x<width;x++) 
		//		{
		//			for(int y=0;y<height;y++) 
		//			{
		//				if (acc[x + (y * width)] > max) 
		//				{
		//					max = acc[x + (y * width)];
		//				}
		//			}
		//		}

		//System.out.println("Max :" + max);
		// Normalise all the values

//		byte value;
//
//		for (int rd=0;rd<(r_max-r_min);rd++)
//		{
//			for(int n=0;n<acc[rd].length;n++) 
//			{
//				value = (byte)(((double)acc[rd][n]/(double)maxtable[rd])*255.0);
//				acc[rd][n] = value;//0xff000000 | (value << 16 | value << 8 | value);
//			}
//		}

		findMaxima();
	}

	private void findMaxima()
	{
		
		for (int n=0;n<results.length;n++) results[n]=0;
		
		for (int rd=0;rd<(r_max-r_min);rd++)
		{
			for(int n=0;n<acc[rd].length;n++) 
			{
				long value = acc[rd][n];// & 0xff;
				// if its higher than lowest value add it and then sort
				if (value > results[(accSize-1)*4]) 
				{
					long x = n % width;
					long y = n / width;
					long radius = rd+r_min;

					// add to bottom of array
					results[(accSize-1)*4] = value;
					results[(accSize-1)*4+1] = x;
					results[(accSize-1)*4+2] = y;
					results[(accSize-1)*4+3] = radius;

					// shift up until its in right place
					int i = (accSize-2)*4;
					while ((i >= 0) && (results[i+4] > results[i])) 
					{
						for(int j=0; j<4; j++) 
						{
							long temp = results[i+j];
							results[i+j] = results[i+j+4];
							results[i+j+4] = temp;
						}
						i = i - 4;
						if (i < 0) break;
					}
				}
			}
		}

		//		for(int i=accSize-1; i>=0; i--)
		//		{
		//			drawCircle(results[i*4], results[i*4+1], results[i*4+2],results[i*4+3]);
		//		}

		//return output;
	}

	//	private void setPixel(int value, int xPos, int yPos) {
	//		output[(yPos * width)+xPos] = 0xff000000 | (value << 16 | value << 8 | value);
	//	}

	// draw circle at x y

	//	private void drawCircleCenter(int pix, int xCenter, int yCenter)
	//	{
	//		pix = 250;
	//		int nPixVal = 0xff000000 | (pix << 16 | pix << 8 | pix);
	//		output[(yCenter * width)+ xCenter] = nPixVal;
	//		//		output[(yCenter * width)+ xCenter+1] = nPixVal;
	//		//		output[(yCenter * width)+ xCenter-1] = nPixVal;
	//		//		
	//		//		output[((yCenter+1) * width)+ xCenter] = nPixVal;
	//		//		output[((yCenter-1) * width)+ xCenter] = nPixVal;
	//
	//	}
	//
	//	private void drawCircle(int pix, int xCenter, int yCenter,int radius) 
	//	{
	//		pix = 250;
	//		int nPixVal = 0xff000000 | (pix << 16 | pix << 8 | pix);
	//		int x, y, r2;
	//		r2 = radius * radius;
	//		output[((yCenter + radius) * width)+xCenter] = nPixVal;// setPixel(pix, xCenter, yCenter + radius);
	//		output[((yCenter - radius) * width)+xCenter] = nPixVal;//setPixel(pix, xCenter, yCenter - radius);
	//		output[(yCenter * width)+(xCenter + radius)] = nPixVal;// setPixel(pix, xCenter + radius, yCenter);
	//		output[(yCenter * width)+(xCenter - radius)] = nPixVal;//setPixel(pix, xCenter - radius, yCenter);
	//
	//		y = radius;
	//		x = 1;
	//		y = (int) (Math.sqrt(r2 - 1) + 0.5);
	//		while (x < y) 
	//		{
	//			output[((yCenter + y) * width)+(xCenter + x)] = nPixVal;//setPixel(pix, xCenter + x, yCenter + y);
	//			output[((yCenter - y) * width)+(xCenter + x)] = nPixVal;//setPixel(pix, xCenter + x, yCenter - y);
	//			output[((yCenter + y) * width)+(xCenter - x)] = nPixVal;//setPixel(pix, xCenter - x, yCenter + y);
	//			output[((yCenter - y) * width)+(xCenter - x)] = nPixVal;//setPixel(pix, xCenter - x, yCenter - y);
	//			output[((yCenter + x) * width)+(xCenter + y)] = nPixVal;//setPixel(pix, xCenter + y, yCenter + x);
	//			output[((yCenter - x) * width)+(xCenter + y)] = nPixVal;//setPixel(pix, xCenter + y, yCenter - x);
	//			output[((yCenter + x) * width)+(xCenter - y)] = nPixVal;//setPixel(pix, xCenter - y, yCenter + x);
	//			output[((yCenter - x) * width)+(xCenter - y)] = nPixVal;//setPixel(pix, xCenter - y, yCenter - x);
	//			x += 1;
	//			y = (int) (Math.sqrt(r2 - x*x) + 0.5);
	//		}
	//
	//		if (x == y) 
	//		{
	//			output[((yCenter + y) * width)+(xCenter + x)] = nPixVal;//setPixel(pix, xCenter + x, yCenter + y);
	//			output[((yCenter - y) * width)+(xCenter + x)] = nPixVal;//setPixel(pix, xCenter + x, yCenter - y);
	//			output[((yCenter + y) * width)+(xCenter - x)] = nPixVal;//setPixel(pix, xCenter - x, yCenter + y);
	//			output[((yCenter - y) * width)+(xCenter - x)] = nPixVal;//setPixel(pix, xCenter - x, yCenter - y);
	//		}
	//	}
}
