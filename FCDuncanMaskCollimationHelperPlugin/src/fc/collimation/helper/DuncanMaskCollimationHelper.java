package fc.collimation.helper;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;

import de.wonderplanets.firecapture.plugin.CamInfo;
import de.wonderplanets.firecapture.plugin.IFilter;
import de.wonderplanets.firecapture.plugin.IFilterListener;


public class DuncanMaskCollimationHelper implements IFilter 
{

	static JFrame j = null;
	static double[] Direction;
	static int nWorkingImgWidth=0;
	static int nWorkingImgHeight=0;
	static byte[] OrigImg;
	static byte[] CurWorkImg1;
	static byte[] CurWorkImg2;
	static int nDownScaleFactor = 1;
	static byte[][] ResultMatrix;
	static byte[] MaxMatrix;
	static int[] results; 

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


	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "Collimation Helper (In use with a Duncan Mask)";
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return "Collimation Helper with Duncan Mask by Stephen Wong";
	}

	@Override
	public String getMaxValueLabel() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getCurrentValueLabel() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getStringUsage(int percent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean useValueFields() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean useSlider() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getMaxValue() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getCurrentValue() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void sliderValueChanged(int value) {
		// TODO Auto-generated method stub

	}

	@Override
	public int getInitialSliderValue() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void imageSizeChanged() {
		// TODO Auto-generated method stub

	}

	@Override
	public void filterChanged(String prevFilter, String filter) {
		// TODO Auto-generated method stub

	}

	@Override
	public void activated() {
		// TODO Auto-generated method stub

	}

	@Override
	public void release() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean capture() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void computeMono(byte[] bytePixels, Rectangle imageSize, CamInfo info) 
	{
		// we can further downsample the input image for faster performance
		// suggest downsample to 800 * 800 or even smaller values
		try
		{
			int nTargetWorkingImgDimension = 20000000;

			int radius_min=100;
			int radius_max=180;

			int nWorkingRMin = radius_min;
			int nWorkingRMax = radius_max;

			int numofmatches=3;


			if (imageSize.width!=(nWorkingImgWidth*nDownScaleFactor) || imageSize.height!=(nWorkingImgHeight*nDownScaleFactor) )
			{
				nDownScaleFactor = Math.max(1,Math.min(imageSize.width, imageSize.height)/nTargetWorkingImgDimension);
				int nWorkingTotalImgLen = bytePixels.length/nDownScaleFactor; 
				Direction = new double[nWorkingTotalImgLen];
				OrigImg = new byte[bytePixels.length];
				CurWorkImg1 = new byte[nWorkingTotalImgLen];
				CurWorkImg2 = new byte[nWorkingTotalImgLen];
				nWorkingImgWidth = imageSize.width/nDownScaleFactor;
				nWorkingImgHeight = imageSize.height/nDownScaleFactor;
				nWorkingRMin = radius_min/nDownScaleFactor;
				nWorkingRMax = Math.max(nWorkingRMin+1,radius_max/nDownScaleFactor);
				ResultMatrix = new byte[nWorkingRMax-nWorkingRMin][nWorkingTotalImgLen];
				MaxMatrix = new byte[nWorkingRMax-nWorkingRMin];
			}


			for (int x=0;x<imageSize.width;x++)
			{
				for (int y=0;y<imageSize.height;y++)
				{
					//OrigImg[y*imageSize.width+x] = bytePixels[y*imageSize.width+x];
					CurWorkImg1[(y/nDownScaleFactor*imageSize.width)+(x/nDownScaleFactor)] = bytePixels[y*imageSize.width+x];
				}
			}


			sobel sobelObject = new sobel();
			sobelObject.init(CurWorkImg1,nWorkingImgWidth,nWorkingImgHeight,Direction,CurWorkImg2);
			sobelObject.process();


			nonMaxSuppression nonMaxSuppressionObject = new nonMaxSuppression(); 
			nonMaxSuppressionObject.init(CurWorkImg2,Direction,nWorkingImgWidth,nWorkingImgHeight, CurWorkImg1);
			nonMaxSuppressionObject.process();

						
			hystThresh hystThreshObject = new hystThresh();
			hystThreshObject.init(CurWorkImg1,nWorkingImgWidth,nWorkingImgHeight, (byte)25,(byte)50,CurWorkImg2);
			hystThreshObject.process();
			
			for (int n=0;n<bytePixels.length;n++)
			{
				OrigImg[n] = CurWorkImg2[n];
			}

//
//			circleHough circleHoughObject = new circleHough();
//			circleHoughObject.init(CurWorkImg2,nWorkingImgWidth,nWorkingImgHeight, nWorkingRMin, nWorkingRMax, numofmatches,costable,sintable,ResultMatrix,MaxMatrix);
//			results = circleHoughObject.process();
//			// the result from circleHoughObject
//			// [0] = score 
//			// [1] = x coordinate 
//			// [2] = y coordinate
//			// [3] = radius
//
//			for(int i=numofmatches-1; i>=0; i--)
//			{
//				drawCircle(results[i*4], results[i*4+1]*nDownScaleFactor, results[i*4+2]*nDownScaleFactor,results[i*4+3]*nDownScaleFactor,imageSize.width,imageSize.height, OrigImg);
//			}
		}
		catch (Exception e)
		{

			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			String sStackTrace = sw.toString();

			JLabel label = null; 
			if (j==null)
			{
				j = new JFrame();
				label = new JLabel();
				j.getContentPane().add("exMessage", label);
			}

			label=(JLabel) j.getContentPane().getComponent(0);
			label.setText("<html>Exception:"+e.toString()+"StackTrace="+sStackTrace+"</html>");
			j.setSize(1020, 420);
			label.setSize(new Dimension(1000,400));
			j.setVisible(true);
		}

		for (int n=0;n<bytePixels.length;n++)
		{
			bytePixels[n] = (byte)Math.min(255, OrigImg[n]);
		}


		//OverlayImage = createImage(new MemoryImageSource(width, height, overlayImage(orig), 0, width));

		//		int rmax = (int)Math.sqrt(imageSize.width*imageSize.width + imageSize.height*imageSize.height);
		//		int acc[] = new int[imageSize.width * imageSize.height];
		//		acc=circleHoughObject.getAcc();

		//HoughAccImage = createImage(new MemoryImageSource(width, height, acc, 0, width)).getScaledInstance(256, 256, Image.SCALE_SMOOTH);
		//LinesImage = createImage(new MemoryImageSource(width, height, orig, 0, width));


	}

	private void drawCircle(int pix, int xCenter, int yCenter,int radius, int width, int height, byte[] output) 
	{
		//pix = 250;
		byte nPixVal = (byte)250;
		int x, y, r2;
		r2 = radius * radius;

		if ((((yCenter + radius) * width)+xCenter) < output.length) output[((yCenter + radius) * width)+xCenter] = nPixVal;// setPixel(pix, xCenter, yCenter + radius);
		if (((yCenter - radius) * width)+xCenter>=0) output[((yCenter - radius) * width)+xCenter] = nPixVal;//setPixel(pix, xCenter, yCenter - radius);
		if ((yCenter * width)+(xCenter + radius)< output.length) output[(yCenter * width)+(xCenter + radius)] = nPixVal;// setPixel(pix, xCenter + radius, yCenter);
		if ((yCenter * width)+(xCenter - radius)>=0) output[(yCenter * width)+(xCenter - radius)] = nPixVal;//setPixel(pix, xCenter - radius, yCenter);

		y = radius;
		x = 1;
		y = (int) (Math.sqrt(r2 - 1) + 0.5);

		while (x < y) 
		{
			if (((yCenter + y) * width)+(xCenter + x)<output.length) output[((yCenter + y) * width)+(xCenter + x)] = nPixVal;//setPixel(pix, xCenter + x, yCenter + y);
			if (((yCenter - y) * width)+(xCenter + x)>=0) output[((yCenter - y) * width)+(xCenter + x)] = nPixVal;//setPixel(pix, xCenter + x, yCenter - y);
			if (((yCenter + y) * width)+(xCenter - x)<output.length) output[((yCenter + y) * width)+(xCenter - x)] = nPixVal;//setPixel(pix, xCenter - x, yCenter + y);
			if (((yCenter - y) * width)+(xCenter - x)>=0) output[((yCenter - y) * width)+(xCenter - x)] = nPixVal;//setPixel(pix, xCenter - x, yCenter - y);
			if (((yCenter + x) * width)+(xCenter + y)<output.length) output[((yCenter + x) * width)+(xCenter + y)] = nPixVal;//setPixel(pix, xCenter + y, yCenter + x);
			if (((yCenter - x) * width)+(xCenter + y)>=0) output[((yCenter - x) * width)+(xCenter + y)] = nPixVal;//setPixel(pix, xCenter + y, yCenter - x);
			if (((yCenter + x) * width)+(xCenter - y)<output.length) output[((yCenter + x) * width)+(xCenter - y)] = nPixVal;//setPixel(pix, xCenter - y, yCenter + x);
			if (((yCenter - x) * width)+(xCenter - y)>=0) output[((yCenter - x) * width)+(xCenter - y)] = nPixVal;//setPixel(pix, xCenter - y, yCenter - x);
			x += 1;
			y = (int) (Math.sqrt(r2 - x*x) + 0.5);
		}

		if (x == y) 
		{
			if (((yCenter + y) * width)+(xCenter + x)<output.length) output[((yCenter + y) * width)+(xCenter + x)] = nPixVal;//setPixel(pix, xCenter + x, yCenter + y);
			if (((yCenter - y) * width)+(xCenter + x)>=0) output[((yCenter - y) * width)+(xCenter + x)] = nPixVal;//setPixel(pix, xCenter + x, yCenter - y);
			if (((yCenter + y) * width)+(xCenter - x)<output.length) output[((yCenter + y) * width)+(xCenter - x)] = nPixVal;//setPixel(pix, xCenter - x, yCenter + y);
			if (((yCenter - y) * width)+(xCenter - x)>=0) output[((yCenter - y) * width)+(xCenter - x)] = nPixVal;//setPixel(pix, xCenter - x, yCenter - y);
		}
	}


	@Override
	public void computeColor(int[] rgbPixels, Rectangle imageSize, CamInfo info) {
		// TODO Auto-generated method stub

	}

	@Override
	public void captureStoped() {
		// TODO Auto-generated method stub

	}

	@Override
	public void captureStarted() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isNullFilter() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean processEarly() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsColor() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsMono() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void registerFilterListener(IFilterListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public JButton getButton() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getInterfaceVersion() {
		// TODO Auto-generated method stub
		return "1.1";
	}

	@Override
	public String getFilenameAppendix() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void appendToLogfile(Properties properties) {
		// TODO Auto-generated method stub

	}

}
