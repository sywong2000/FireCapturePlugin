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
		
		for (int n=0;n<magnitude.length;n++)
		{
			if (magnitude[n]< (byte) 180)
			{
				output[n] = (byte)0;
			}
			else
			{
				output[n] = magnitude[n];
			}
		}
	}

	public void process3()
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
				
				// 
				// 0 deg is 0
				// 180 deg is Math.PI
				// direction + PI to fall into below ranges. Direction is atan2(y,x)
				// E is between 7PI/8 to 9PI/8 : 7-9
				// NE is between 9PI/8 to 11PI/8: 9-11
				// N is between 11PI/8 to 13PI/8: 11-13
				// NW is between 13PI/8 to 15PI/8: 13-15
				// W is between 15PI/8 to 16PI/8 and 0PI/8 to 1PI/8: 15-16 & 0-1
				// SW is between PI/8 to 3PI/8: 1-3
				// S is between 3PI/8 to 5PI/8: 3-5
				// SE is between 5PI/8 to 7PI/8: 5-7
				
				float dir = (float) ((direction[y*width+x] + Math.PI)/Math.PI)*8;
				
				
				try 
				{
				if ((((dir >7 && dir <=9)||(dir >15 || dir <=1)) && mC > magnitude[nE] && mC > magnitude[nW]) || // E or W
						(((dir >9 && dir <=11)||(dir >1 && dir <=3)) && mC > magnitude[nNE] && mC > magnitude[nSW]) || // NE or SW
		                (((dir > 11 && dir <= 13)||(dir > 3 && dir <= 5)) && mC > magnitude[nN] && mC > magnitude[nS]) || // N or S
		                (((dir > 13 && dir <= 15)||(dir > 5 && dir <= 7)) && mC > magnitude[nNW] && mC > magnitude[nSE]))   // NW or SE
				{
					output[nCenter] = mC;
				}
				else
				{
					output[nCenter] = (byte)0;
				}
				}
				catch (Exception e)
				{
					output[nCenter] = (byte)0;	
				}
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
