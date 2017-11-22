package fc.collimation.helper;

public class nonMaxSuppression 
{
	int[] magnitude;
	double[] direction;
	int[] output;
	//float[] template={-1,0,1,-2,0,2,-1,0,1};;
	//int progress;
	//int templateSize=3;
	int width;
	int height;
	final double roottwo = Math.sqrt(2);
	final double pi_over_8 = Math.PI/8;
	final double pi_over_2 = Math.PI / 2;


	public void init(int[] magnitudeIn, double[] directionIn, int widthIn, int heightIn, int[] output_in) 
	{
		width=widthIn;
		height=heightIn;
		//magnitude = new int[width*height];
		//direction = new double[width*height];
		//output = new int[width*height];
		output = output_in;
		magnitude=magnitudeIn; 
		direction=directionIn;
	}
	
	public void process() 
	{
		
		for(int x=0;x<width;x++) 
		{
			for(int y=0;y<height;y++) 
			{
				if ((magnitude[y*width+x]&0xff) > 0) 
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
						output[y*width+x] = 0xff000000 | (Mint << 16 | Mint << 8 | Mint);
					}
					else 
					{
						output[y*width+x] = 0xff000000;
					}
				} 
				else 
					output[y*width+x] = 0xff000000;
			}
		}

		//return output;
	}
}
