package fc.collimation.helper;

public class boxFilter  
{
	byte[] input;
	byte[] output;
	int width;
	int height;


	public void init(byte[] original, byte[] output_in, int sigmaIn, int tempSize, int widthIn, int heightIn) 
	{
		width=widthIn;
		height=heightIn;
		input = original;
		output = output_in;
	}

	public void process() 
	{
		int x=width;
		int y=height;

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
				
				

				byte gval= (byte)Math.min(255,(val00+val01+val02+val10+val11+val12+val20+val21+val22)/9);
				output[j*x + i] = gval;
			}
		}
	}
}
