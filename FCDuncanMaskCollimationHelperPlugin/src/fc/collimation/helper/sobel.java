package fc.collimation.helper;

public class sobel 
{
	byte[] input;
	byte[] output;
	float[] template={-1,0,1,-2,0,2,-1,0,1};;
	//int progress;
	int templateSize=3;
	int width;
	int height;
	double[] direction;
	static byte[] bytePix=null;

	public void init(byte[] original, int widthIn, int heightIn, double[] direction_in, byte[] output_in) 
	{
		width=widthIn;
		height=heightIn;
		//input = new int[width*height];
		//output = new int[width*height];
		//direction = new double[width*height];
		input=original;
		output=output_in;
		direction = direction_in;
		if (bytePix==null || bytePix.length!=original.length)
		{
			bytePix = new byte[original.length];
		}
	}
	
	public void process()
	{
		int x=width;
		int y=height;
		byte nMax = 0;

		// i is the x coordinate, 1 to width (x)
		// j is the y coordinate, 1 to height (y)
		
		// n=j*W + i
		
		//int[] bytePix = new int[input.length];
		
		for(int i=1;i<x-1;i++)
		{
			for(int j=1;j<y-1;j++)
			{
				byte val00 = input[((j-1)*x)+(i-1)];//&0xFF;// image.getRGB(i-1,j-1); // top left
				byte val01 = input[(j*x)+(i-1)];//&0xFF; //image.getRGB(i-1,j); //left
				byte val02 = input[((j+1)*x)+(i-1)];//&0xFF;//image.getRGB(i-1,j+1); //bottom left

				byte val10 = input[((j-1)*x)+i];//&0xFF; //image.getRGB(i,j-1); // top
				byte val11 = input[j*x+i];//&0xFF;//image.getRGB(i,j); // center
				byte val12 = input[((j+1)*x)+i];//&0xFF;// image.getRGB(i,j+1); // bottom

				byte val20 = input[((j-1)*x)+(i+1)];//&0xFF; //image.getRGB(i+1,j-1); //top right
				byte val21 = input[(j*x)+(i+1)];//&0xFF; //image.getRGB(i+1,j); // right
				byte val22 = input[((j+1)*x)+(i+1)];//&0xFF; //image.getRGB(i+1,j+1); //bottom right
				
				// GX is +1, 0, -1	GY is +1, +2, +1
				// GX is +2, 0, -2	GY is  0,  0,  0
				// GX is +1, 0, -1	GY is -1, -2, -1

				//int gx=(((-1*val00)+(0*val01)+(1*val02))+((-2*val10)+(0*val11)+(2*val12))+((-1*val20)+(0*val21)+(1*val22)));
				//int gy=(((-1*val00)+(-2*val01)+(-1*val02))+((0*val10)+(0*val11)+(0*val12))+((1*val20)+(2*val21)+(1*val22)));
				
				int gx=(((1*val00)+(2*val01)+(1*val02))+((0*val10)+(0*val11)+(0*val12))+((-1*val20)+(-2*val21)+(-1*val22)));
				int gy=(((1*val00)+(0*val01)+(-1*val02))+((2*val10)+(0*val11)+(-2*val12))+((1*val20)+(0*val21)+(-1*val22)));
				

				byte gval= (byte)Math.min(255,(Math.sqrt((gx*gx)+(gy*gy))));
				direction[j*x + i] = Icecore.atan2(gy,gx);
							
				nMax = nMax>gval?nMax:gval;
				output[j*x + i] = gval;
			}
		}
		
//		for (int n=0;n<bytePix.length;n++)
//		{
//			output[n] = bytePix[n];//(byte)Math.min(255,(bytePix[n]));// * 255/nMax
//		}
		//return output;
	}
	
//	public int[] process() 
//	{
//		float[] GY = new float[width*height];
//		float[] GX = new float[width*height];
//		
//		int[] total = new int[width*height];
//		//progress=0;
//		int sum=0;
//		int max=0;
//
//		for(int x=(templateSize-1)/2; x<width-(templateSize+1)/2;x++) 
//		{
//			//progress++;
//			for(int y=(templateSize-1)/2; y<height-(templateSize+1)/2;y++) 
//			{
//				sum=0;
//
//				for(int x1=0;x1<templateSize;x1++) 
//				{
//					for(int y1=0;y1<templateSize;y1++) 
//					{
//						int x2 = (x-(templateSize-1)/2+x1);
//						int y2 = (y-(templateSize-1)/2+y1);
//						float value = (input[y2*width+x2] & 0xff) * (template[y1*templateSize+x1]);
//						sum += value;
//					}
//				}
//				
//				GY[y*width+x] = sum;
//				
//				for(int x1=0;x1<templateSize;x1++) 
//				{
//					for(int y1=0;y1<templateSize;y1++) 
//					{
//						int x2 = (x-(templateSize-1)/2+x1);
//						int y2 = (y-(templateSize-1)/2+y1);
//						float value = (input[y2*width+x2] & 0xff) * (template[x1*templateSize+y1]);
//						sum += value;
//					}
//				}
//				GX[y*width+x] = sum;
//
//			}
//		}
//		
//		for(int x=0; x<width;x++) 
//		{
//			for(int y=0; y<height;y++) 
//			{
//				
//				total[y*width+x]=(int)Math.sqrt(GX[y*width+x]*GX[y*width+x]+GY[y*width+x]*GY[y*width+x]);
//				direction[y*width+x] = Math.atan2(GX[y*width+x],GY[y*width+x]);
//				
//				if(max<total[y*width+x])
//					max=total[y*width+x];
//			}
//		}
//		
//		float ratio=(float)max/255;
//		for(int x=0; x<width;x++) 
//		{
//			for(int y=0; y<height;y++) 
//			{
//				sum=(int)(total[y*width+x]/ratio);
//				output[y*width+x] = 0xff000000 | ((int)sum << 16 | (int)sum << 8 | (int)sum);
//			}
//		}
//		//progress=width;
//		
//		return output;
//	}

}
