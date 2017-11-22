package fc.collimation.helper;

public class circleHough {
	int[] input;
	int[] output;
	//float[] template={-1,0,1,-2,0,2,-1,0,1};;
	//double progress;
	int width;
	int height;
	int[] acc;
	int accSize=30;
	int[] results;
	int r;

	static double[] costable = null;
	static double[] sintable = null;

	static
	{
		costable = new double[360];
		sintable = new double[360];

		for (int t =0;t<360;t++)
		{
			costable[t] = Math.cos(t);
			sintable[t] = Math.sin(t);
		}
	}

	public void init(int[] inputIn, int widthIn, int heightIn, int radius, int[] output_in, int[] acc_in, int lines) 
	{

		r = radius;
		width=widthIn;
		height=heightIn;
		output = output_in;
		input=inputIn;
		acc=acc_in;
		accSize=lines;	
		for (int n = 0;n<output.length;n++)
		{
			output[n] = input[n];
			acc[n] = 0;
		}
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
		int max=0;

		for(int x=0;x<width;x++) 
		{
			for(int y=0;y<height;y++) 
			{
				if ((input[y*width+x] & 0xff)== 255) 
				{
					// speed up the processing by matching each other 4 pixels 
					for (int theta=0; theta<360; theta=theta+4) 
					{
						//t = (theta * 3.14159265) / 180;
						x0 = (int)Math.round(x - r * costable[theta]);
						y0 = (int)Math.round(y - r * sintable[theta]);
						if(x0 < width && x0 > 0 && y0 < height && y0 > 0) 
						{
							acc[x0 + (y0 * width)] += 1;
							max= (acc[x0 + (y0 * width)]>max)?acc[x0 + (y0 * width)]:max;
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

		int value;
		for(int n=0;n<acc.length;n++) 
		{
			value = (int)(((double)acc[n]/(double)max)*255.0);
			acc[n] = 0xff000000 | (value << 16 | value << 8 | value);
		}

		findMaxima();
	}

	private void findMaxima()
	{
		results = new int[accSize*3];
		//int[] output = new int[width*height];

		for(int n=0;n<acc.length;n++) 
		{
			int value = acc[n] & 0xff;
			// if its higher than lowest value add it and then sort
			if (value > results[(accSize-1)*3]) 
			{
				int x = n % width;
				int y = n / width;

				// add to bottom of array
				results[(accSize-1)*3] = value;
				results[(accSize-1)*3+1] = x;
				results[(accSize-1)*3+2] = y;

				// shift up until its in right place
				int i = (accSize-2)*3;
				while ((i >= 0) && (results[i+3] > results[i])) 
				{
					for(int j=0; j<3; j++) 
					{
						int temp = results[i+j];
						results[i+j] = results[i+3+j];
						results[i+3+j] = temp;
					}
					i = i - 3;
					if (i < 0) break;
				}
			}
		}

		//double ratio=(double)(width/2)/accSize;
		//System.out.println("top "+accSize+" matches:");
		for(int i=accSize-1; i>=0; i--)
		{
			//System.out.println("value: " + results[i*3] + ", r: " + results[i*3+1] + ", theta: " + results[i*3+2]);
			drawCircle(results[i*3], results[i*3+1], results[i*3+2]);
			//drawCircleCenter(results[i*3], results[i*3+1], results[i*3+2]);
		}
		//return output;
	}

	//	private void setPixel(int value, int xPos, int yPos) {
	//		output[(yPos * width)+xPos] = 0xff000000 | (value << 16 | value << 8 | value);
	//	}

	// draw circle at x y

	private void drawCircleCenter(int pix, int xCenter, int yCenter)
	{
		pix = 250;
		int nPixVal = 0xff000000 | (pix << 16 | pix << 8 | pix);
		output[(yCenter * width)+ xCenter] = nPixVal;
		//		output[(yCenter * width)+ xCenter+1] = nPixVal;
		//		output[(yCenter * width)+ xCenter-1] = nPixVal;
		//		
		//		output[((yCenter+1) * width)+ xCenter] = nPixVal;
		//		output[((yCenter-1) * width)+ xCenter] = nPixVal;

	}

	private void drawCircle(int pix, int xCenter, int yCenter) 
	{
		pix = 250;
		int nPixVal = 0xff000000 | (pix << 16 | pix << 8 | pix);
		int x, y, r2;
		int radius = r;
		r2 = r * r;
		output[((yCenter + radius) * width)+xCenter] = nPixVal;// setPixel(pix, xCenter, yCenter + radius);
		output[((yCenter - radius) * width)+xCenter] = nPixVal;//setPixel(pix, xCenter, yCenter - radius);
		output[(yCenter * width)+(xCenter + radius)] = nPixVal;// setPixel(pix, xCenter + radius, yCenter);
		output[(yCenter * width)+(xCenter - radius)] = nPixVal;//setPixel(pix, xCenter - radius, yCenter);

		y = radius;
		x = 1;
		y = (int) (Math.sqrt(r2 - 1) + 0.5);
		while (x < y) 
		{
			output[((yCenter + y) * width)+(xCenter + x)] = nPixVal;//setPixel(pix, xCenter + x, yCenter + y);
			output[((yCenter - y) * width)+(xCenter + x)] = nPixVal;//setPixel(pix, xCenter + x, yCenter - y);
			output[((yCenter + y) * width)+(xCenter - x)] = nPixVal;//setPixel(pix, xCenter - x, yCenter + y);
			output[((yCenter - y) * width)+(xCenter - x)] = nPixVal;//setPixel(pix, xCenter - x, yCenter - y);
			output[((yCenter + x) * width)+(xCenter + y)] = nPixVal;//setPixel(pix, xCenter + y, yCenter + x);
			output[((yCenter - x) * width)+(xCenter + y)] = nPixVal;//setPixel(pix, xCenter + y, yCenter - x);
			output[((yCenter + x) * width)+(xCenter - y)] = nPixVal;//setPixel(pix, xCenter - y, yCenter + x);
			output[((yCenter - x) * width)+(xCenter - y)] = nPixVal;//setPixel(pix, xCenter - y, yCenter - x);
			x += 1;
			y = (int) (Math.sqrt(r2 - x*x) + 0.5);
		}

		if (x == y) 
		{
			output[((yCenter + y) * width)+(xCenter + x)] = nPixVal;//setPixel(pix, xCenter + x, yCenter + y);
			output[((yCenter - y) * width)+(xCenter + x)] = nPixVal;//setPixel(pix, xCenter + x, yCenter - y);
			output[((yCenter + y) * width)+(xCenter - x)] = nPixVal;//setPixel(pix, xCenter - x, yCenter + y);
			output[((yCenter - y) * width)+(xCenter - x)] = nPixVal;//setPixel(pix, xCenter - x, yCenter - y);
		}
	}
}
