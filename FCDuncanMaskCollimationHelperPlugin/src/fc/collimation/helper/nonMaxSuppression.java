package fc.collimation.helper;

public class nonMaxSuppression 
{
	byte[] magnitude;
	double[] direction;
	byte[] output;
	//float[] template={-1,0,1,-2,0,2,-1,0,1};;
	//int progress;
	//int templateSize=3;
	int width;
	int height;
	final double roottwo = Math.sqrt(2);
	final double pi_over_8 = Math.PI/8;
	final double pi_over_2 = Math.PI / 2;


	public void init(byte[] magnitudeIn, double[] directionIn, int widthIn, int heightIn, byte[] output_in) 
	{
		width=widthIn;
		height=heightIn;
		output = output_in;
		magnitude=magnitudeIn; 
		direction=directionIn;
	}

	public void process()
	{
		// https://rosettacode.org/wiki/Canny_edge_detector#J
		for(int y=0;y<height;y++) 
		{
			for(int x=0;x<width;x++) 
			{
				int nCenter = y*width + x;
				byte mC = magnitude[nCenter]; 
				int nN = nCenter - width;
				int nS = nCenter + width;
				int nW =  nCenter-1; // should be n_c -1 not +1
				int nE = nCenter+1;
				int nNW = nCenter - width-1;
				int nNE = nCenter - width+1;
				int nSW = nCenter + width-1;
				int nSE = nCenter + width+1;
				
				float ndir = (float)((direction[y*width+x] + Math.PI)%Math.PI)/Math.PI * 8;
//				if (((ndir <=1 || ndir >7) && mC > magnitude[nE] && mC > magnitude[nW]) ||
//						((ndir >1 || ndir <=3) && mC > magnitude[nNW] && mC > magnitude[nSE]) ||
			}
		}
	}


	public void process2() 
	{
		for(int x=0;x<width;x++) 
		{
			for(int y=0;y<height;y++) 
			{
				if ((magnitude[y*width+x]) > 0) 
				{
					double angle = direction[y*width+x];
					int Mint = magnitude[y*width+x]&0xff;

					// angle wants to be the normal so add pi/2
					angle += pi_over_2;

					int x1 = (int)Math.ceil((Riven.cos((float) (angle + pi_over_8)) * roottwo) - 0.5);
					int y1 = (int)Math.ceil((-Riven.sin((float) (angle + pi_over_8)) * roottwo) - 0.5);
					int x2 = (int)Math.ceil((Riven.cos((float) (angle - pi_over_8)) * roottwo) - 0.5);
					int y2 = (int)Math.ceil((-Riven.sin((float) (angle - pi_over_8)) * roottwo) - 0.5);

					double M1 = (magnitude[(y+y1)*width+(x+x1)]&0xff + magnitude[(y+y2)*width+(x+x2)]&0xff)/2;

					angle += Math.PI;

					x1 = (int)Math.ceil((Riven.cos((float) (angle + pi_over_8)) * roottwo) - 0.5);
					y1 = (int)Math.ceil((-Riven.sin((float) (angle + pi_over_8)) * roottwo) - 0.5);
					x2 = (int)Math.ceil((Riven.cos((float) (angle - pi_over_8)) * roottwo) - 0.5);
					y2 = (int)Math.ceil((-Riven.sin((float) (angle - pi_over_8)) * roottwo) - 0.5);

					double M2 = (magnitude[(y+y1)*width+(x+x1)]&0xff + magnitude[(y+y2)*width+(x+x2)]&0xff)/2;

					if ((Mint > M1) && (Mint >= M2)) 
					{
						// 00000000000000000000000011001000 (the int) Mint int value = (200)
						// 00000000000000001100100000000000 (the int) Mint << 8
						// 00000000110010000000000000000000 (the int) Mint << 8
						// 11111111000000000000000000000000 (the int) 0xff000000
						// 11111111110010001100100011001000 (the int) after | int value = -3618616
						// 11111111000000000000000000000000

						output[y*width+x] = (byte) 255;//0xff000000 | (Mint << 16 | Mint << 8 | Mint);
					}
					else 
					{
						output[y*width+x] = (byte)0;//0xff000000;
					}
				} 
				else 
					output[y*width+x] = (byte)0;//0xff000000;
			}
		}

		//return output;
	}
}
