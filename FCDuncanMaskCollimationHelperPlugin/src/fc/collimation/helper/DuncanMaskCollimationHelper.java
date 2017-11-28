package fc.collimation.helper;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;

import de.wonderplanets.firecapture.plugin.CamInfo;
import de.wonderplanets.firecapture.plugin.IFilter;
import de.wonderplanets.firecapture.plugin.IFilterListener;


public class DuncanMaskCollimationHelper implements IFilter 
{

	static JFrame j = null;
	static JFrame jImage = null;
	static double[] Direction;
	static int nWorkingImgWidth=0;
	static int nWorkingImgHeight=0;
	static byte[] OrigImg;
	static byte[] CurWorkImg1;
	static byte[] CurWorkImg2;
	static int nDownScaleFactor = 1;
	static long[][] ScoreMatrix;
	static long[] MaxMatrix;
	static long[] results; 

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
	public void imageSizeChanged() 
	{
		// TODO Auto-generated method stub
		Direction = null;
		OrigImg =null;
		CurWorkImg1=null;
		CurWorkImg2=null;
		nDownScaleFactor = 1;
		ScoreMatrix =null;
		MaxMatrix = null;
		results = null; 

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
		if (jImage!=null)
		{
			jImage.setVisible(false);
			jImage.dispose();
		}
	}

	@Override
	public boolean capture() {
		// TODO Auto-generated method stub
		return false;
	}

	void DuncanMaskObjectDetect(byte[] bytePixels, int nWidth, int nHeight, int DownScaleFactor, int nRadiusMin, int nRadiusMax, int NumOfMatches)
	{
		if (results==null) results = new long[NumOfMatches*4];

		int nWorkingTotalImgLen = bytePixels.length/nDownScaleFactor;
		if (Direction==null) Direction = new double[nWorkingTotalImgLen];
		if (OrigImg==null) OrigImg = new byte[bytePixels.length];
		if (CurWorkImg1==null) CurWorkImg1 = new byte[nWorkingTotalImgLen];
		if (CurWorkImg2==null) CurWorkImg2 = new byte[nWorkingTotalImgLen];
		nWorkingImgWidth = nWidth/nDownScaleFactor;
		nWorkingImgHeight = nHeight/nDownScaleFactor;
		int nWorkingRMin = nRadiusMin/nDownScaleFactor;
		int nWorkingRMax = Math.max(nWorkingRMin+1,nRadiusMax/nDownScaleFactor);
		if (ScoreMatrix==null) ScoreMatrix = new long[nWorkingRMax-nWorkingRMin][nWorkingTotalImgLen];
		if (MaxMatrix==null) MaxMatrix = new long[nWorkingRMax-nWorkingRMin];

		for (int x=0;x<nWidth;x++)
		{
			for (int y=0;y<nHeight;y++)
			{
				OrigImg[y*nWidth+x] = ((x/2%nRadiusMin==0 && x/2/nRadiusMin==1) || (x/2%nRadiusMax==0 && x/2/nRadiusMax==1) || (y/2%nRadiusMax==0 && y/2/nRadiusMax==1) ||(y/2%nRadiusMin==0 && y/2/nRadiusMin==1))?(byte)255: bytePixels[y*nWidth+x];
				CurWorkImg1[(y/nDownScaleFactor*nWorkingImgWidth)+(x/nDownScaleFactor)] = bytePixels[y*nWidth+x];
			}
		}

		sobel sobelObject = new sobel();
		sobelObject.init(CurWorkImg1,nWorkingImgWidth,nWorkingImgHeight,Direction,CurWorkImg2);
		sobelObject.process();

		nonMaxSuppression nonMaxSuppressionObject = new nonMaxSuppression(); 
		nonMaxSuppressionObject.init(CurWorkImg2,Direction,nWorkingImgWidth,nWorkingImgHeight, CurWorkImg1);
		nonMaxSuppressionObject.process2();

		hystThresh hystThreshObject = new hystThresh();
		hystThreshObject.init(CurWorkImg1,nWorkingImgWidth,nWorkingImgHeight, (byte)10,(byte)20,CurWorkImg2);
		hystThreshObject.process();

		for (int n=0;n<results.length;n++) results[n] = 0;

		circleHough circleHoughObject = new circleHough();
		circleHoughObject.init(CurWorkImg2,nWorkingImgWidth,nWorkingImgHeight, nWorkingRMin, nWorkingRMax, NumOfMatches,costable,sintable,ScoreMatrix,MaxMatrix, results);
		circleHoughObject.process();

	}

	private int getGrayScale(int rgb)
	{
		int r = rgb >> 16 & 0xff;
		int g = rgb >> 8 & 0xff;
		int b = rgb & 0xff;
		int gray = (int)(0.21260000000000001D * (double)r + 0.71519999999999995D * (double)g + 0.0722D * (double)b);
		return gray;
	}


	@Override
	public void computeMono(byte[] bytePixels, Rectangle imageSize, CamInfo info) 
	{

		// we can further downsample the input image for faster performance
		// suggest downsample to 800 * 800 or even smaller values

		int radius_min=80;
		int radius_max=140;
		int NumOfMatches=3;
		int nTargetWorkingImgDimension = 300;
		int nDownScaleFactor = imageSize.width/nTargetWorkingImgDimension;

		DuncanMaskObjectDetect(bytePixels, imageSize.width, imageSize.height, nDownScaleFactor, radius_min, radius_max,NumOfMatches);


		byte[] nMap = new byte[CurWorkImg1.length];
		for (int n=0;n<CurWorkImg1.length;n++)
		{
			nMap[n] = CurWorkImg1[n];
		}

		for(int i=NumOfMatches-1; i>=0; i--)
		{
			drawCircle((int)results[i*4], (int)results[i*4+1]*nDownScaleFactor, (int)results[i*4+2]*nDownScaleFactor,(int)results[i*4+3]*nDownScaleFactor,imageSize.width,imageSize.height, bytePixels);
		}

		//Image img = getImageFromArray(nMap,nWorkingImgWidth, nWorkingImgHeight);

		// the result from circleHoughObject
		// [0] = score 
		// [1] = x coordinate 
		// [2] = y coordinate
		// [3] = radius

		//			for (int n=0;n<bytePixels.length;n++)
		//			{
		//				OrigImg[n] = CurWorkImg1[n];
		//			}

		//			String sResults = "<html>";


		//			sResults +="</html>";
		//
		//			if (jImage==null)
		//			{
		//				jImage = new JFrame();
		//				JLabel label = new JLabel();
		//				jImage.getContentPane().add(label);
		//				jImage.setSize(nWorkingImgWidth, nWorkingImgHeight);
		//				
		//			}

		//			JLabel label = (JLabel) jImage.getContentPane().getComponent(0);
		//			label.setIcon(new ImageIcon(img));
		//			label.setSize(nWorkingImgWidth,nWorkingImgHeight);
		//			if (!jImage.isVisible())
		//			{
		//				jImage.setVisible(true);
		//			}

//		for (int n=0;n<bytePixels.length;n++)
//		{
//			bytePixels[n] = (byte)Math.min(255, OrigImg[n]);
//		}
	}

	public Image getImageFromArray(byte[] pixels, int width, int height) 
	{
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		image.getRaster().setDataElements(0, 0, width, height, pixels);
		return image;
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

	private void drawCircle(int pix, int xCenter, int yCenter,int radius, int width, int height, int[] output) 
	{
		//pix = 250;
		int nPixVal = 0xff000000;
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
	public void computeColor(int[] rgbPixels, Rectangle imageSize, CamInfo info) 
	{
		int radius_min=80;
		int radius_max=140;
		int NumOfMatches=3;
		int nTargetWorkingImgDimension = 300;
		int nDownScaleFactor = imageSize.width/nTargetWorkingImgDimension;

		byte[] bytePixels = new byte[imageSize.width* imageSize.height];
		for (int n =0;n<rgbPixels.length;n++)
		{
			bytePixels[n] = (byte) Math.min(255, getGrayScale(rgbPixels[n]));
		}
		DuncanMaskObjectDetect(bytePixels, imageSize.width, imageSize.height, nDownScaleFactor, radius_min, radius_max,NumOfMatches);


		byte[] nMap = new byte[CurWorkImg1.length];
		for (int n=0;n<CurWorkImg1.length;n++)
		{
			nMap[n] = CurWorkImg1[n];
		}

		for(int i=NumOfMatches-1; i>=0; i--)
		{
			drawCircle((int)results[i*4], (int)results[i*4+1]*nDownScaleFactor, (int)results[i*4+2]*nDownScaleFactor,(int)results[i*4+3]*nDownScaleFactor,imageSize.width,imageSize.height, rgbPixels);
		}
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
		return true;
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
