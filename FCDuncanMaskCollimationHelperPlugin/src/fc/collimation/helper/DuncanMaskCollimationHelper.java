package fc.collimation.helper;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
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
	static int nWorkingRMin;
	static int nWorkingRMax;

	static byte[] OrigImg;
	static byte[] CurWorkImg1;
	static byte[] CurWorkImg2;
	static int nDownScaleFactor = 1;
	static int[][] ScoreMatrix;
	static int[] colortable = null;
	//static long[] MaxMatrix;
	static int[] results; 

	static double[] costable = null;
	static double[] sintable = null;
	static int nHystStackSize = 0;
	static int nHystMaxLen = 0;
	static int nTargetWorkingImgDimension = 400;

	static int RadiusMinValue=10;
	static int RadiusMaxValue=20;
	static final int nRedColor = 0xffff0000;
	static final int nGreenColor = 0xff00ff00;
	static final int nYellowColor = 0xffffff00;


	static int nObjectRadius = 100;


	static
	{
		costable = new double[360];
		sintable = new double[360];

		for (int t =0;t<360;t++)
		{
			costable[t] = Math.cos(t);
			sintable[t] = Math.sin(t);
		}
		
		colortable = new int[100];
		for (int t=0;t<100;t++)
		{
			colortable[t] = numberToColorHsl((float) ((float)t/100.0),(float)0.5,(float)1.0);
		}
	}

	final double roottwo = Math.sqrt(2);
	final double pi_over_8 = Math.PI/8;
	final double pi_over_2 = Math.PI / 2;


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
	public String getStringUsage(int percent) 
	{
		// TODO Auto-generated method stub
		return (new StringBuilder("Detect object Radius: ")).append(percent*5).append(" pixels.").toString();
	}

	@Override
	public boolean useValueFields() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean useSlider() {
		// TODO Auto-generated method stub
		return true;
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
	public void sliderValueChanged(int value) 
	{
		// TODO Auto-generated method stub
		nObjectRadius = Math.max(1,value) * 5;

		RadiusMinValue= (int) (nObjectRadius * 0.9);
		RadiusMaxValue= (int) (nObjectRadius * 1.1);

		nWorkingRMin = RadiusMinValue/nDownScaleFactor;
		nWorkingRMax = Math.max(nWorkingRMin+1,RadiusMaxValue/nDownScaleFactor);
	}

	@Override
	public int getInitialSliderValue() {
		// TODO Auto-generated method stub
		return 10;
	}

	@Override
	public void imageSizeChanged() 
	{
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

			int numofmatches=3;

			int nWorkingTotalImgLen = bytePixels.length;
			if (imageSize.width!=(nWorkingImgWidth*nDownScaleFactor) || imageSize.height!=(nWorkingImgHeight*nDownScaleFactor) )
			{
				nDownScaleFactor = Math.max(1,Math.min(imageSize.width, imageSize.height)/nTargetWorkingImgDimension);
				nWorkingTotalImgLen = bytePixels.length/nDownScaleFactor;

				Direction = new double[nWorkingTotalImgLen];
				OrigImg = new byte[bytePixels.length];
				CurWorkImg1 = new byte[nWorkingTotalImgLen];
				CurWorkImg2 = new byte[nWorkingTotalImgLen];
				nWorkingImgWidth = imageSize.width/nDownScaleFactor;
				nWorkingImgHeight = imageSize.height/nDownScaleFactor;
				nWorkingRMin = RadiusMinValue/nDownScaleFactor;
				nWorkingRMax = Math.max(nWorkingRMin+1,RadiusMaxValue/nDownScaleFactor);
				results = new int[numofmatches*4];
			}



			ScoreMatrix = new int[nWorkingRMax-nWorkingRMin][nWorkingTotalImgLen];
			//MaxMatrix = new long[nWorkingRMax-nWorkingRMin];

			for (int x=0;x<imageSize.width;x++)
			{
				for (int y=0;y<imageSize.height;y++)
				{
					OrigImg[y*imageSize.width+x] = ((x/2%RadiusMinValue==0 && x/2/RadiusMinValue==1) || (x/2%RadiusMaxValue==0 && x/2/RadiusMaxValue==1) || (y/2%RadiusMaxValue==0 && y/2/RadiusMaxValue==1) ||(y/2%RadiusMinValue==0 && y/2/RadiusMinValue==1))?(byte)255: bytePixels[y*imageSize.width+x];
					CurWorkImg1[(y/nDownScaleFactor*nWorkingImgWidth)+(x/nDownScaleFactor)] = bytePixels[y*imageSize.width+x];
				}
			}

			//			gaussianFilter gaussianObject = new gaussianFilter();
			//			gaussianObject.init(CurWorkImg1, CurWorkImg2, 3,5,nWorkingImgWidth, nWorkingImgHeight);
			//			gaussianObject.generateTemplate();
			//			gaussianObject.process();



			//			sobel sobelObject = new sobel();
			//			sobelObject.init(CurWorkImg1,nWorkingImgWidth,nWorkingImgHeight,Direction,CurWorkImg2);
			SobelProcess(CurWorkImg1,nWorkingImgWidth,nWorkingImgHeight,Direction,CurWorkImg2);

			//			nonMaxSuppression nonMaxSuppressionObject = new nonMaxSuppression(); 
			//			nonMaxSuppressionObject.init(CurWorkImg2,Direction,nWorkingImgWidth,nWorkingImgHeight, CurWorkImg1);
			//			nonMaxSuppressionObject.process2();
			NonMaxSuppressionProcess(CurWorkImg2,Direction,nWorkingImgWidth,nWorkingImgHeight, CurWorkImg1);

			byte[] nMap = new byte[CurWorkImg1.length];
			nHystStackSize = 0;
			nHystMaxLen = (int) (2*Math.PI * Math.min(nWorkingImgWidth,nWorkingImgHeight)/2);

			//			hystThresh hystThreshObject = new hystThresh();
			//			hystThreshObject.init(CurWorkImg1,nWorkingImgWidth,nWorkingImgHeight, (byte)10,(byte)20,CurWorkImg2);
			//			hystThreshObject.process();
			hystThreshProcess(CurWorkImg1,nWorkingImgWidth,nWorkingImgHeight, (byte)10,(byte)20,CurWorkImg2);

			for (int n=0;n<CurWorkImg2.length;n++)
			{
				nMap[n] = CurWorkImg2[n];
			}


			//			for (int n=0;n<results.length;n++) results[n] = 0;
			//
			//			circleHough circleHoughObject = new circleHough();
			//			circleHoughObject.init(CurWorkImg2,nWorkingImgWidth,nWorkingImgHeight, nWorkingRMin, nWorkingRMax, numofmatches,costable,sintable,ScoreMatrix,MaxMatrix, results);
			//			circleHoughObject.process();

			HoughProcess(CurWorkImg2,nWorkingImgWidth,nWorkingImgHeight, nWorkingRMin, nWorkingRMax, numofmatches,ScoreMatrix);

			//			int nAccTotal = 0;
			//			for (int n=0;n<ScoreMatrix.length;n++)
			//			{
			//				for (int m=0;m<ScoreMatrix[n].length;m++)
			//				{
			//					nAccTotal +=ScoreMatrix[n][m];
			//				}
			//			}


			//			long nMax = MaxMatrix[0];
			//			byte[] nMap = new byte[ScoreMatrix[0].length];
			//			
			//			for (int n=0;n<ScoreMatrix[0].length;n++)
			//			{
			//				nMap[n] = (byte) (ScoreMatrix[0][n]/nMax * 255);
			//			}




			Image img = getImageFromArray(nMap,nWorkingImgWidth, nWorkingImgHeight);

			// the result from circleHoughObject
			// [0] = score 
			// [1] = x coordinate 
			// [2] = y coordinate
			// [3] = radius

			//			for (int n=0;n<bytePixels.length;n++)
			//			{
			//				OrigImg[n] = CurWorkImg1[n];
			//			}


			int r_x,r_y, x, y;
			r_x = results[5]*nDownScaleFactor;
			x = results[9]*nDownScaleFactor;
			r_y = results[6]*nDownScaleFactor;
			y = results[10]*nDownScaleFactor;
			int d1 = (int) Math.sqrt((r_x-x)*(r_x-x) + (r_y-y)*(r_y-y));

			r_x = results[9]*nDownScaleFactor;
			x = results[13]*nDownScaleFactor;
			r_y = results[10]*nDownScaleFactor;
			y = results[14]*nDownScaleFactor;

			int d2 = (int) Math.sqrt((r_x-x)*(r_x-x) + (r_y-y)*(r_y-y));

			r_x = results[13]*nDownScaleFactor;
			x = results[5]*nDownScaleFactor;
			r_y = results[14]*nDownScaleFactor;
			y = results[6]*nDownScaleFactor;

			int d3 = (int) Math.sqrt((r_x-x)*(r_x-x) + (r_y-y)*(r_y-y));

			String sResults = "<html>";
			sResults +=String.format("<br>nDownScaleFactor=%d, nWorkingImgWidth=%d, nWorkingImgHeight=%d<br>", nDownScaleFactor, nWorkingImgWidth, nWorkingImgHeight);
			for(int i=numofmatches-1; i>=0; i--)
			{
				drawCircle((byte)255, (int)results[i*4+1]*nDownScaleFactor, (int)results[i*4+2]*nDownScaleFactor,(int)results[i*4+3]*nDownScaleFactor,imageSize.width,imageSize.height, bytePixels);
				sResults += String.format("Circle %d at (%d,%d) with radius %d. ",i,results[i*4+1],results[i*4+2],results[i*4+3]);
			}
			sResults += String.format("<br>d1=%d,d2=%d,d3=%d.",d1,d2,d3);
			sResults +=String.format("<br>radius_min=%d, radius_max=%d", RadiusMinValue, RadiusMaxValue);
			sResults +="</html>";

			drawDottedCircle(0, imageSize.width/2, imageSize.height/2,RadiusMinValue,imageSize.width,imageSize.height, bytePixels);
			drawDottedCircle(0, imageSize.width/2, imageSize.height/2,RadiusMaxValue,imageSize.width,imageSize.height, bytePixels);

			ShowImageWindowFrame(sResults, img);

			//			if (j==null)
			//			{
			//				j = new JFrame();
			//				label = new JLabel();
			//				j.getContentPane().add("exMessage", label);
			//			}
			//
			//			label = (JLabel)(j.getContentPane().getComponent(0));
			//			label.setText(sResults);
			//
			//			j.setSize(500, 420);
			//			j.setVisible(true);
		}
		catch (Exception e)
		{

			//			StringWriter sw = new StringWriter();
			//			PrintWriter pw = new PrintWriter(sw);
			//			e.printStackTrace(pw);
			//			String sStackTrace = sw.toString();
			//
			//			JLabel label = null; 
			//			if (j==null)
			//			{
			//				j = new JFrame();
			//				label = new JLabel();
			//				j.getContentPane().add("exMessage", label);
			//			}
			//
			//			label=(JLabel) j.getContentPane().getComponent(0);
			//			label.setText("<html>Exception:"+e.toString()+"<br>StackTrace="+sStackTrace.substring(4000).replace("\r","<br>").replace("\n","<br>")+"</html>");
			//			j.setSize(1200, 420);
			//			label.setSize(new Dimension(1200,400));
			//			j.setVisible(true);
		}

		//		for (int n=0;n<bytePixels.length;n++)
		//		{
		//			bytePixels[n] = (byte)Math.min(255, OrigImg[n]);
		//		}


		//OverlayImage = createImage(new MemoryImageSource(width, height, overlayImage(orig), 0, width));

		//		int rmax = (int)Math.sqrt(imageSize.width*imageSize.width + imageSize.height*imageSize.height);
		//		int acc[] = new int[imageSize.width * imageSize.height];
		//		acc=circleHoughObject.getAcc();

		//HoughAccImage = createImage(new MemoryImageSource(width, height, acc, 0, width)).getScaledInstance(256, 256, Image.SCALE_SMOOTH);
		//LinesImage = createImage(new MemoryImageSource(width, height, orig, 0, width));


	}


	@Override
	public void computeColor(int[] rgbPixels, Rectangle imageSize, CamInfo info) 
	{
		try
		{
			int numofmatches=3;
			byte[] bytePixels = new byte[rgbPixels.length];

			int nWorkingTotalImgLen = bytePixels.length;
			if (imageSize.width!=(nWorkingImgWidth*nDownScaleFactor) || imageSize.height!=(nWorkingImgHeight*nDownScaleFactor) )
			{
				nDownScaleFactor = Math.max(1,Math.min(imageSize.width, imageSize.height)/nTargetWorkingImgDimension);
				nWorkingTotalImgLen = bytePixels.length/nDownScaleFactor;

				Direction = new double[nWorkingTotalImgLen];
				OrigImg = new byte[bytePixels.length];
				CurWorkImg1 = new byte[nWorkingTotalImgLen];
				CurWorkImg2 = new byte[nWorkingTotalImgLen];
				nWorkingImgWidth = imageSize.width/nDownScaleFactor;
				nWorkingImgHeight = imageSize.height/nDownScaleFactor;
				nWorkingRMin = RadiusMinValue/nDownScaleFactor;
				nWorkingRMax = Math.max(nWorkingRMin+1,RadiusMaxValue/nDownScaleFactor);
				results = new int[numofmatches*4];
			}



			ScoreMatrix = new int[nWorkingRMax-nWorkingRMin][nWorkingTotalImgLen];
			//MaxMatrix = new long[nWorkingRMax-nWorkingRMin];

			for (int n=0;n<rgbPixels.length;n++)
			{
				bytePixels[n] = (byte) getGrayScale(rgbPixels[n]);
			}

			for (int x=0;x<imageSize.width;x++)
			{
				for (int y=0;y<imageSize.height;y++)
				{
					OrigImg[y*imageSize.width+x] = ((x/2%RadiusMinValue==0 && x/2/RadiusMinValue==1) || (x/2%RadiusMaxValue==0 && x/2/RadiusMaxValue==1) || (y/2%RadiusMaxValue==0 && y/2/RadiusMaxValue==1) ||(y/2%RadiusMinValue==0 && y/2/RadiusMinValue==1))?(byte)255: bytePixels[y*imageSize.width+x];
					CurWorkImg1[(y/nDownScaleFactor*nWorkingImgWidth)+(x/nDownScaleFactor)] = bytePixels[y*imageSize.width+x];
				}
			}

			SobelProcess(CurWorkImg1,nWorkingImgWidth,nWorkingImgHeight,Direction,CurWorkImg2);
			NonMaxSuppressionProcess(CurWorkImg2,Direction,nWorkingImgWidth,nWorkingImgHeight, CurWorkImg1);

			byte[] nMap = new byte[CurWorkImg1.length];
			nHystStackSize = 0;
			nHystMaxLen = (int) (2*Math.PI * Math.min(nWorkingImgWidth,nWorkingImgHeight)/2);

			hystThreshProcess(CurWorkImg1,nWorkingImgWidth,nWorkingImgHeight, (byte)10,(byte)20,CurWorkImg2);

			for (int n=0;n<CurWorkImg2.length;n++)
			{
				nMap[n] = CurWorkImg2[n];
			}
			HoughProcess(CurWorkImg2,nWorkingImgWidth,nWorkingImgHeight, nWorkingRMin, nWorkingRMax, numofmatches,ScoreMatrix);
			Image img = getImageFromArray(nMap,nWorkingImgWidth, nWorkingImgHeight);

			int p0_x,p0_y, p1_x, p1_y, p2_x,p2_y, pT_x, pT_y;

			p0_x = results[1];//*nDownScaleFactor;
			p0_y = results[2];//*nDownScaleFactor;

			p1_x = results[5];//*nDownScaleFactor;
			p1_y = results[6];//*nDownScaleFactor;

			p2_x = results[9];//*nDownScaleFactor;			
			p2_y = results[10];//*nDownScaleFactor;

			int d1 = (int) Math.sqrt((p0_x-p1_x)*(p0_x-p1_x) + (p0_y-p1_y)*(p0_y-p1_y));
			int d2 = (int) Math.sqrt((p1_x-p2_x)*(p1_x-p2_x) + (p1_y-p2_y)*(p1_y-p2_y));
			int d3 = (int) Math.sqrt((p2_x-p0_x)*(p2_x-p0_x) + (p2_y-p0_y)*(p2_y-p0_y));

			int p3a_x=0, p3a_y=0, p3b_x=0, p3b_y=0;

			int[] p3points = computeTipPoint(p0_x,p0_y,p1_x,p1_y, d1);
			
			p3a_x = p3points[0];
			p3a_y = p3points[1];
			p3b_x = p3points[2];
			p3b_y = p3points[3];

			int dp3a_p2 = (int) Math.sqrt((p2_x-p3a_x)*(p2_x-p3a_x) + (p2_y-p3a_y)*(p2_y-p3a_y));
			int dp3b_p2 = (int) Math.sqrt((p2_x-p3b_x)*(p2_x-p3b_x) + (p2_y-p3b_y)*(p2_y-p3b_y));

			int dp = (dp3a_p2<dp3b_p2)?dp3a_p2:dp3b_p2;
			pT_x = (dp3a_p2<dp3b_p2)?p3a_x:p3b_x;
			pT_y = (dp3a_p2<dp3b_p2)?p3a_y:p3b_y;
			
			int dDiff = (int)(100-(double)dp/(double)d1 * 100.0);
			int nColor = colortable[dDiff];// (dDiff<10.0)?nGreenColor:nRedColor;

			//			drawLine(p0_x,p0_y,p1_x, p1_y, imageSize.width, imageSize.height, nGreenColor,  rgbPixels);
			//			drawLine(p1_x,p1_y,pT_x, pT_y, imageSize.width, imageSize.height, nRedColor,  rgbPixels);
			//			drawLine(pT_x,pT_y,p0_x, p0_y, imageSize.width, imageSize.height, nRedColor,  rgbPixels);
			
			//drawDottedLine(p0_x,p0_y,p1_x, p1_y, nWorkingImgWidth, nWorkingImgHeight, nYellowColor,  img);
			
			drawDottedLine(p1_x,p1_y,p2_x, p2_y, nWorkingImgWidth, nWorkingImgHeight, nYellowColor,  img);
			drawDottedLine(p2_x,p2_y,p0_x, p0_y, nWorkingImgWidth, nWorkingImgHeight, nYellowColor,  img);

			drawLine(p0_x,p0_y,p1_x, p1_y, nWorkingImgWidth, nWorkingImgHeight, nColor,  img);
			drawLine(p1_x,p1_y,pT_x, pT_y, nWorkingImgWidth, nWorkingImgHeight, nColor,  img);
			drawLine(pT_x,pT_y,p0_x, p0_y, nWorkingImgWidth, nWorkingImgHeight, nColor,  img);
			
			drawCross(p0_x,p0_y,5, nColor,  img);
			drawCross(p1_x,p1_y,5, nColor,  img);
			drawCross(p2_x,p2_y,5, nColor,  img);

			String sResults = "<html>";
			sResults +=String.format("<br>nDownScaleFactor=%d, nWorkingImgWidth=%d, nWorkingImgHeight=%d<br>", nDownScaleFactor, nWorkingImgWidth, nWorkingImgHeight);

			for(int i=numofmatches-1; i>=0; i--)
			{
				//drawCircle(nRedColor, (int)results[i*4+1]*nDownScaleFactor, (int)results[i*4+2]*nDownScaleFactor,(int)results[i*4+3]*nDownScaleFactor,imageSize.width,imageSize.height, rgbPixels);
				drawCircle(nColor, (int)results[i*4+1], (int)results[i*4+2],(int)results[i*4+3],nWorkingImgWidth,nWorkingImgHeight, img);

				sResults += String.format("Circle %d at (%d,%d) with radius %d. ",i,results[i*4+1],results[i*4+2],results[i*4+3]);
			}

			sResults += String.format("<br>d1=%d,d2=%d,d3=%d. dp=%d. dDiff=%#.4f",d1,d2,d3, dp, (double)dDiff);
			sResults +=String.format("<br>radius_min=%d, radius_max=%d", RadiusMinValue, RadiusMaxValue);
			//sResults +=String.format("<br>p3a_x=%d, p3a_y=%d, p3b_x=%d, p3b_y=%d", p3a_x, p3a_y, p3b_x, p3a_y);
			sResults +="</html>";

			drawDottedCircle(nYellowColor, imageSize.width/2, imageSize.height/2,RadiusMinValue,imageSize.width,imageSize.height, rgbPixels);
			drawDottedCircle(nYellowColor, imageSize.width/2, imageSize.height/2,RadiusMaxValue,imageSize.width,imageSize.height, rgbPixels);

			ShowImageWindowFrame(sResults, img);

			//			if (j==null)
			//			{
			//				j = new JFrame();
			//				label = new JLabel();
			//				j.getContentPane().add("exMessage", label);
			//			}
			//
			//			label = (JLabel)(j.getContentPane().getComponent(0));
			//			label.setText(sResults);
			//
			//			j.setSize(500, 420);
			//			j.setVisible(true);
		}
		catch (Exception e)
		{

			//			StringWriter sw = new StringWriter();
			//			PrintWriter pw = new PrintWriter(sw);
			//			e.printStackTrace(pw);
			//			String sStackTrace = sw.toString();
			//
			//			JLabel label = null; 
			//			if (j==null)
			//			{
			//				j = new JFrame();
			//				label = new JLabel();
			//				j.getContentPane().add("exMessage", label);
			//			}
			//
			//			label=(JLabel) j.getContentPane().getComponent(0);
			//			label.setText("<html>Exception:"+e.toString()+"StackTrace="+sStackTrace.replace("\r","<br>").replace("\n","<br>")+"</html>");
			//			j.setSize(1020, 420);
			//			label.setSize(new Dimension(1000,400));
			//			j.setVisible(true);
		}
	}

	private static int[] computeTipPoint(int p0_x, int p0_y, int p1_x, int p1_y, double length)
	{
		int pOut0_x, pOut0_y, pOut1_x, pOut1_y;
		double dx = p1_x - p0_x;
		double dy = p1_y - p0_y;
		double dirX = dx / length;
		double dirY = dy / length;
		double height = Math.sqrt(3)/2 * length;
		double cx = p0_x + dx * 0.5;
		double cy = p0_y + dy * 0.5;
		double pDirX = -dirY;
		double pDirY = dirX;

		pOut0_x = (int) (cx + height * pDirX);
		pOut0_y = (int) (cy + height * pDirY);
		pOut1_x = (int) (cx - height * pDirX);
		pOut1_y = (int) (cy - height * pDirY);

		return new int[] {pOut0_x, pOut0_y, pOut1_x, pOut1_y};
	}

	//	private static void computeTipPoint(Point2D p0, Point2D p1, double length,  Point2D pOut1, Point2D pOut2)
	//	{
	//		double dx = p1.getX() - p0.getX();
	//		double dy = p1.getY() - p0.getY();
	//		//double length = Math.sqrt(dx*dx+dy*dy);
	//		double dirX = dx / length;
	//		double dirY = dy / length;
	//		double height = Math.sqrt(3)/2 * length;
	//		double cx = p0.getX() + dx * 0.5;
	//		double cy = p0.getY() + dy * 0.5;
	//		double pDirX = -dirY;
	//		double pDirY = dirX;
	//		double rx = 0;
	//		double ry = 0;
	//		rx = cx + height * pDirX;
	//		ry = cy + height * pDirY;
	//		pOut1 = new Point2D.Double(rx, ry);
	//		rx = cx - height * pDirX;
	//		ry = cy - height * pDirY;
	//		pOut2 = new Point2D.Double(rx, ry);
	//	}

	private void ShowImageWindowFrame(String sText, Image img)
	{
		if (jImage==null)
		{
			jImage = new JFrame();
			JLabel lbImage = new JLabel();
			JLabel lbTxt = new JLabel();
			Container p = jImage.getContentPane();
			p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
			lbImage.setAlignmentX(Component.LEFT_ALIGNMENT);
			p.add(lbImage,0);
			lbTxt.setAlignmentX(Component.LEFT_ALIGNMENT);
			p.add(lbTxt,1);
		}


		JLabel lbImage = (JLabel) jImage.getContentPane().getComponent(0);
		JLabel lbTxt = (JLabel) jImage.getContentPane().getComponent(1);
		lbTxt.setText(sText);
		//lbTxt.setPreferredSize(new Dimension(500,200));
		//lbTxt.setLocation(5,nWorkingImgHeight+5);
		//lbTxt.setBorder(BorderFactory.createLineBorder(Color.BLUE, 5));
		lbImage.setIcon(new ImageIcon(img));
		lbImage.setSize(nWorkingImgWidth,nWorkingImgHeight);
		//lbImage.setLocation(0,30);
		//lbImage.setVisible(false);
		jImage.setSize(nWorkingImgWidth, (int)(nWorkingImgHeight*1.4));
		jImage.setVisible(true);

	}

	private void SobelProcess(byte[] input,int width ,int height ,double[] direction,byte[] output)
	{
		int x=width;
		int y=height;
		byte nMax = 0;
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
	}

	private void NonMaxSuppressionProcess(byte[] magnitude,double[] direction,int width,int height, byte[] output)
	{
		for(int x=0;x<width;x++) 
		{
			for(int y=0;y<height;y++) 
			{
				if ((magnitude[y*width+x]) > 0) 
				{
					double angle = direction[y*width+x];
					byte Mint = magnitude[y*width+x];

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

					if ((Mint > (byte) M1) && (Mint >= (byte) M2)) 
					{
						// 00000000000000000000000011001000 (the int) Mint int value = (200)
						// 00000000000000001100100000000000 (the int) Mint << 8
						// 00000000110010000000000000000000 (the int) Mint << 8
						// 11111111000000000000000000000000 (the int) 0xff000000
						// 11111111110010001100100011001000 (the int) after | int value = -3618616
						// 11111111000000000000000000000000

						output[y*width+x] = Mint;//0xff000000 | (Mint << 16 | Mint << 8 | Mint);
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
	}

	void hystThreshProcess(byte input[],int width,int height, byte lower,byte upper ,byte[] output)
	{
		for(int x=0;x<width;x++) 
		{
			for(int y=0;y<height;y++) 
			{
				byte value = (input[y*width+x]);
				if (value >= upper) 
				{
					input[y*width+x] = (byte) 255;//0xffffffff;
					hystConnect(input, x, y, width, height, lower);
				}
			}
		}

		for (int n=0;n<input.length;n++)
		{
			if (input[n] == (byte)255)//0xffffffff)
			{
				output[n] = (byte) 255;//0xffffffff;
			}
			else
			{
				output[n] = (byte)0;//0xff000000;
			}
		}

	}

	private void hystConnect(byte[] input, int x, int y, int width, int height, byte lower) 
	{

		if (nHystStackSize > nHystMaxLen) return;
		nHystStackSize++;

		byte value = 0;
		for (int x1=x-1;x1<=x+1;x1++) 
		{
			for (int y1=y-1;y1<=y+1;y1++) 
			{
				if ((x1 < width) & (y1 < height) & (x1 >= 0) & (y1 >= 0) & (x1 != x) & (y1 != y)) 
				{
					value = (input[y1*width+x1]);//  & 0xff;
					if (value != (byte)255) 
					{
						if (value >= lower) 
						{
							input[y1*width+x1] = (byte) 255;
							hystConnect(input, x1, y1, width, height, lower);
						} 
						else 
						{
							input[y1*width+x1] = (byte)0;//0xff000000;
						}
					}
				}
			}
		}
	}


	void HoughProcess(byte[] input, int width, int height, int r_min, int r_max, int NumOfMatches, int[][] acc)
	{
		Arrays.fill(results, 0);

		for (int n=0;n<acc.length;n++)
		{
			Arrays.fill(acc[n], 0);
		}
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
							//if(x0 < width && x0 > 0 && y0 < height && y0 > 0)
							if (x0 + (y0 * width)>0 && x0 + (y0 * width)<acc[rd].length)
							{
								acc[rd][x0 + (y0 * width)] ++;
								//								maxtable[rd]= (acc[rd][x0 + (y0 * width)]>maxtable[rd])?acc[rd][x0 + (y0 * width)]:maxtable[rd];
							}
						}
					}
				}
			}
		}

		// find Maxima


		for (int rd=0;rd<(r_max-r_min);rd++)
		{
			for(int n=0;n<acc[rd].length;n++) 
			{
				int value = acc[rd][n];// & 0xff;
				// if its higher than lowest value add it and then sort
				int x = n % width;
				int y = n / width;
				int radius = rd+r_min;

				int r_maxscore = results[(NumOfMatches-1)*4];

				if (value > r_maxscore) 
				{
					// avoid overlapped results
					// check the circle center to give it a certain threshold
					boolean bTooNear = false;
					for (int i=0;i<NumOfMatches;i++)
					{
						int r_x = results[(NumOfMatches-1-i)*4+1];
						int r_y = results[(NumOfMatches-1-i)*4+2];
						int d = (int) Math.sqrt((r_x-x)*(r_x-x) + (r_y-y)*(r_y-y));
						if (d < 2*radius)
						{
							bTooNear = true;
							break;
						}
					}

					if (!bTooNear)
					{

						// add to bottom of array
						results[(NumOfMatches-1)*4] = value;
						results[(NumOfMatches-1)*4+1] = x;
						results[(NumOfMatches-1)*4+2] = y;
						results[(NumOfMatches-1)*4+3] = radius;

						// shift up until its in right place
						int i = (NumOfMatches-2)*4;
						while ((i >= 0) && (results[i+4] > results[i])) 
						{
							for(int j=0; j<4; j++) 
							{
								int temp = results[i+j];
								results[i+j] = results[i+j+4];
								results[i+j+4] = temp;
							}
							i = i - 4;
							if (i < 0) break;
						}
					}
				}
			}
		}

	}

	public Image getImageFromArray(byte[] pixels, int width, int height) 
	{
		// rgb = grey << 16 | grey << 8 | grey
		//		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		//		image.getRaster().setDataElements(0, 0, width, height, pixels);
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		int[] rgbPixels = new int[pixels.length];

		for (int n=0;n<pixels.length;n++)
		{
			rgbPixels[n] = pixels[n]<< 16 | pixels[n] << 8 |  pixels[n];
		}

		image.getRaster().setDataElements(0, 0, width, height, rgbPixels);
		return image;
	}


	private void drawCircle(byte nPixVal, int xCenter, int yCenter,int radius, int width, int height, byte[] output) 
	{
		//pix = 250;
		int x, y, r2;
		r2 = radius * radius;

		// this is exact S
		if ((((yCenter + radius) * width)+xCenter) < output.length) output[((yCenter + radius) * width)+xCenter] = nPixVal;
		if ((((yCenter + radius-1) * width)+xCenter) < output.length) output[((yCenter + radius-1) * width)+xCenter] = nPixVal;

		// this is exact W
		if (((yCenter - radius) * width)+xCenter>=0) output[((yCenter - radius) * width)+xCenter] = nPixVal;
		if (((yCenter - radius+1) * width)+xCenter>=0) output[((yCenter - radius+1) * width)+xCenter] = nPixVal;

		// this is exact E
		if ((yCenter * width)+(xCenter + radius)< output.length) output[(yCenter * width)+(xCenter + radius)] = nPixVal;
		if ((yCenter * width)+(xCenter + radius-1)< output.length) output[(yCenter * width)+(xCenter + radius-1)] = nPixVal;

		// this is exact N
		if ((yCenter * width)+(xCenter - radius)>=0) output[(yCenter * width)+(xCenter - radius)] = nPixVal;
		if ((yCenter * width)+(xCenter - radius+1)>=0) output[(yCenter * width)+(xCenter - radius+1)] = nPixVal;


		//y = radius;

		x = 1;
		y = (int) (Math.sqrt(r2 - 1) + 0.5);
		// start drawing at 90 degree (top side)

		while (x < y) 
		{
			// this starts from S side to SE
			if (((yCenter + y) * width)+(xCenter + x)<output.length) output[((yCenter + y) * width)+(xCenter + x)] = nPixVal;
			if (((yCenter + y-1) * width)+(xCenter + x)<output.length) output[((yCenter + y-1) * width)+(xCenter + x)] = nPixVal;

			// this starts from N side to NE
			if (((yCenter - y) * width)+(xCenter + x)>=0) output[((yCenter - y) * width)+(xCenter + x)] = nPixVal;
			if (((yCenter - y+1) * width)+(xCenter + x)>=0) output[((yCenter - y+1) * width)+(xCenter + x)] = nPixVal;

			// this starts from S side to SW
			if (((yCenter + y) * width)+(xCenter - x)<output.length) output[((yCenter + y) * width)+(xCenter - x)] = nPixVal;
			if (((yCenter + y-1) * width)+(xCenter - x)<output.length) output[((yCenter + y-1) * width)+(xCenter - x)] = nPixVal;

			// this starts from N side to NW
			if (((yCenter - y) * width)+(xCenter - x)>=0) output[((yCenter - y) * width)+(xCenter - x)] = nPixVal;
			if (((yCenter - y+1) * width)+(xCenter - x)>=0) output[((yCenter - y+1) * width)+(xCenter - x)] = nPixVal;

			// this starts from E side to SE
			if (((yCenter + x) * width)+(xCenter + y)<output.length) output[((yCenter + x) * width)+(xCenter + y)] = nPixVal;
			if (((yCenter + x) * width)+(xCenter + y-1)<output.length) output[((yCenter + x) * width)+(xCenter + y-1)] = nPixVal;

			// this starts from E side to NE			
			if (((yCenter - x) * width)+(xCenter + y)>=0) output[((yCenter - x) * width)+(xCenter + y)] = nPixVal;
			if (((yCenter - x) * width)+(xCenter + y-1)>=0) output[((yCenter - x) * width)+(xCenter + y-1)] = nPixVal;

			// this starts from W side to SW
			if (((yCenter + x) * width)+(xCenter - y)<output.length) output[((yCenter + x) * width)+(xCenter - y)] = nPixVal;
			if (((yCenter + x) * width)+(xCenter - y+1)<output.length) output[((yCenter + x) * width)+(xCenter - y+1)] = nPixVal;

			// this starts from W side to NW
			if (((yCenter - x) * width)+(xCenter - y)>=0) output[((yCenter - x) * width)+(xCenter - y)] = nPixVal;
			if (((yCenter - x) * width)+(xCenter - y+1)>=0) output[((yCenter - x) * width)+(xCenter - y+1)] = nPixVal;

			x++;
			y = (int) (Math.sqrt(r2 - x*x) + 0.5);
		}

		if (x == y) 
		{
			// this is exact SE
			if (((yCenter + y) * width)+(xCenter + x)<output.length) output[((yCenter + y) * width)+(xCenter + x)] = nPixVal;
			if (((yCenter + y-1) * width)+(xCenter + x-1)<output.length) output[((yCenter + y-1) * width)+(xCenter + x-1)] = nPixVal;

			// this is exact NE
			if (((yCenter - y) * width)+(xCenter + x)>=0) output[((yCenter - y) * width)+(xCenter + x)] = nPixVal;
			if (((yCenter - y-1) * width)+(xCenter + x-1)>=0) output[((yCenter - y-1) * width)+(xCenter + x-1)] = nPixVal;

			// this is exact SW 
			if (((yCenter + y) * width)+(xCenter - x)<output.length) output[((yCenter + y) * width)+(xCenter - x)] = nPixVal;
			if (((yCenter + y-1) * width)+(xCenter - x+1)<output.length) output[((yCenter + y-1) * width)+(xCenter - x+1)] = nPixVal;

			// this is exact NW
			if (((yCenter - y) * width)+(xCenter - x)>=0) output[((yCenter - y) * width)+(xCenter - x)] = nPixVal;
			if (((yCenter - y-1) * width)+(xCenter - x+1)>=0) output[((yCenter - y-1) * width)+(xCenter - x+1)] = nPixVal;
		}
	}


	private void drawDottedCircle(int nPixValColor1, int xCenter, int yCenter,int radius, int width, int height, int[] output) 
	{
		//pix = 250;
		int x, y, r2;
		r2 = radius * radius;

		// this is exact S
		if ((((yCenter + radius) * width)+xCenter) < output.length) output[((yCenter + radius) * width)+xCenter] = nPixValColor1;
		if ((((yCenter + radius-1) * width)+xCenter) < output.length) output[((yCenter + radius-1) * width)+xCenter] = nPixValColor1;

		// this is exact W
		if (((yCenter - radius) * width)+xCenter>=0) output[((yCenter - radius) * width)+xCenter] = nPixValColor1;
		if (((yCenter - radius+1) * width)+xCenter>=0) output[((yCenter - radius+1) * width)+xCenter] = nPixValColor1;

		// this is exact E
		if ((yCenter * width)+(xCenter + radius)< output.length) output[(yCenter * width)+(xCenter + radius)] = nPixValColor1;
		if ((yCenter * width)+(xCenter + radius-1)< output.length) output[(yCenter * width)+(xCenter + radius-1)] = nPixValColor1;

		// this is exact N
		if ((yCenter * width)+(xCenter - radius)>=0) output[(yCenter * width)+(xCenter - radius)] = nPixValColor1;
		if ((yCenter * width)+(xCenter - radius+1)>=0) output[(yCenter * width)+(xCenter - radius+1)] = nPixValColor1;


		//y = radius;

		x = 1;
		y = (int) (Math.sqrt(r2 - 1) + 0.5);
		// start drawing at 90 degree (top side)

		while (x < y) 
		{
			// this starts from S side to SE
			if (((yCenter + y) * width)+(xCenter + x)<output.length) output[((yCenter + y) * width)+(xCenter + x)] = nPixValColor1;
			if (((yCenter + y-1) * width)+(xCenter + x)<output.length) output[((yCenter + y-1) * width)+(xCenter + x)] = nPixValColor1;

			// this starts from N side to NE
			if (((yCenter - y) * width)+(xCenter + x)>=0) output[((yCenter - y) * width)+(xCenter + x)] = nPixValColor1;
			if (((yCenter - y+1) * width)+(xCenter + x)>=0) output[((yCenter - y+1) * width)+(xCenter + x)] = nPixValColor1;

			// this starts from S side to SW
			if (((yCenter + y) * width)+(xCenter - x)<output.length) output[((yCenter + y) * width)+(xCenter - x)] = nPixValColor1;
			if (((yCenter + y-1) * width)+(xCenter - x)<output.length) output[((yCenter + y-1) * width)+(xCenter - x)] = nPixValColor1;

			// this starts from N side to NW
			if (((yCenter - y) * width)+(xCenter - x)>=0) output[((yCenter - y) * width)+(xCenter - x)] = nPixValColor1;
			if (((yCenter - y+1) * width)+(xCenter - x)>=0) output[((yCenter - y+1) * width)+(xCenter - x)] = nPixValColor1;

			// this starts from E side to SE
			if (((yCenter + x) * width)+(xCenter + y)<output.length) output[((yCenter + x) * width)+(xCenter + y)] = nPixValColor1;
			if (((yCenter + x) * width)+(xCenter + y-1)<output.length) output[((yCenter + x) * width)+(xCenter + y-1)] = nPixValColor1;

			// this starts from E side to NE			
			if (((yCenter - x) * width)+(xCenter + y)>=0) output[((yCenter - x) * width)+(xCenter + y)] = nPixValColor1;
			if (((yCenter - x) * width)+(xCenter + y-1)>=0) output[((yCenter - x) * width)+(xCenter + y-1)] = nPixValColor1;

			// this starts from W side to SW
			if (((yCenter + x) * width)+(xCenter - y)<output.length) output[((yCenter + x) * width)+(xCenter - y)] = nPixValColor1;
			if (((yCenter + x) * width)+(xCenter - y+1)<output.length) output[((yCenter + x) * width)+(xCenter - y+1)] = nPixValColor1;

			// this starts from W side to NW
			if (((yCenter - x) * width)+(xCenter - y)>=0) output[((yCenter - x) * width)+(xCenter - y)] = nPixValColor1;
			if (((yCenter - x) * width)+(xCenter - y+1)>=0) output[((yCenter - x) * width)+(xCenter - y+1)] = nPixValColor1;

			x+=6;
			y = (int) (Math.sqrt(r2 - x*x) + 0.5);
		}

		if (x == y) 
		{
			// this is exact SE
			if (((yCenter + y) * width)+(xCenter + x)<output.length) output[((yCenter + y) * width)+(xCenter + x)] = nPixValColor1;
			if (((yCenter + y-1) * width)+(xCenter + x-1)<output.length) output[((yCenter + y-1) * width)+(xCenter + x-1)] = nPixValColor1;

			// this is exact NE
			if (((yCenter - y) * width)+(xCenter + x)>=0) output[((yCenter - y) * width)+(xCenter + x)] = nPixValColor1;
			if (((yCenter - y-1) * width)+(xCenter + x-1)>=0) output[((yCenter - y-1) * width)+(xCenter + x-1)] = nPixValColor1;

			// this is exact SW 
			if (((yCenter + y) * width)+(xCenter - x)<output.length) output[((yCenter + y) * width)+(xCenter - x)] = nPixValColor1;
			if (((yCenter + y-1) * width)+(xCenter - x+1)<output.length) output[((yCenter + y-1) * width)+(xCenter - x+1)] =  nPixValColor1;

			// this is exact NW
			if (((yCenter - y) * width)+(xCenter - x)>=0) output[((yCenter - y) * width)+(xCenter - x)] = nPixValColor1;
			if (((yCenter - y-1) * width)+(xCenter - x+1)>=0) output[((yCenter - y-1) * width)+(xCenter - x+1)] = nPixValColor1;
		}
	}


	private void drawDottedCircle(int pix, int xCenter, int yCenter,int radius, int width, int height, byte[] output) 
	{
		//pix = 250;
		byte nPixVal = (byte)250;
		int x, y, r2;
		r2 = radius * radius;

		// this is exact S
		if ((((yCenter + radius) * width)+xCenter) < output.length) output[((yCenter + radius) * width)+xCenter] = nPixVal;
		if ((((yCenter + radius-1) * width)+xCenter) < output.length) output[((yCenter + radius-1) * width)+xCenter] = nPixVal;

		// this is exact W
		if (((yCenter - radius) * width)+xCenter>=0) output[((yCenter - radius) * width)+xCenter] = nPixVal;
		if (((yCenter - radius+1) * width)+xCenter>=0) output[((yCenter - radius+1) * width)+xCenter] = nPixVal;

		// this is exact E
		if ((yCenter * width)+(xCenter + radius)< output.length) output[(yCenter * width)+(xCenter + radius)] = nPixVal;
		if ((yCenter * width)+(xCenter + radius-1)< output.length) output[(yCenter * width)+(xCenter + radius-1)] = nPixVal;

		// this is exact N
		if ((yCenter * width)+(xCenter - radius)>=0) output[(yCenter * width)+(xCenter - radius)] = nPixVal;
		if ((yCenter * width)+(xCenter - radius+1)>=0) output[(yCenter * width)+(xCenter - radius+1)] = nPixVal;


		//y = radius;

		x = 1;
		y = (int) (Math.sqrt(r2 - 1) + 0.5);
		// start drawing at 90 degree (top side)

		while (x < y) 
		{
			// this starts from S side to SE
			if (((yCenter + y) * width)+(xCenter + x)<output.length) output[((yCenter + y) * width)+(xCenter + x)] = nPixVal;
			if (((yCenter + y-1) * width)+(xCenter + x)<output.length) output[((yCenter + y-1) * width)+(xCenter + x)] = nPixVal;

			// this starts from N side to NE
			if (((yCenter - y) * width)+(xCenter + x)>=0) output[((yCenter - y) * width)+(xCenter + x)] = nPixVal;
			if (((yCenter - y+1) * width)+(xCenter + x)>=0) output[((yCenter - y+1) * width)+(xCenter + x)] = nPixVal;

			// this starts from S side to SW
			if (((yCenter + y) * width)+(xCenter - x)<output.length) output[((yCenter + y) * width)+(xCenter - x)] = nPixVal;
			if (((yCenter + y-1) * width)+(xCenter - x)<output.length) output[((yCenter + y-1) * width)+(xCenter - x)] = nPixVal;

			// this starts from N side to NW
			if (((yCenter - y) * width)+(xCenter - x)>=0) output[((yCenter - y) * width)+(xCenter - x)] = nPixVal;
			if (((yCenter - y+1) * width)+(xCenter - x)>=0) output[((yCenter - y+1) * width)+(xCenter - x)] = nPixVal;

			// this starts from E side to SE
			if (((yCenter + x) * width)+(xCenter + y)<output.length) output[((yCenter + x) * width)+(xCenter + y)] = nPixVal;
			if (((yCenter + x) * width)+(xCenter + y-1)<output.length) output[((yCenter + x) * width)+(xCenter + y-1)] = nPixVal;

			// this starts from E side to NE			
			if (((yCenter - x) * width)+(xCenter + y)>=0) output[((yCenter - x) * width)+(xCenter + y)] = nPixVal;
			if (((yCenter - x) * width)+(xCenter + y-1)>=0) output[((yCenter - x) * width)+(xCenter + y-1)] = nPixVal;

			// this starts from W side to SW
			if (((yCenter + x) * width)+(xCenter - y)<output.length) output[((yCenter + x) * width)+(xCenter - y)] = nPixVal;
			if (((yCenter + x) * width)+(xCenter - y+1)<output.length) output[((yCenter + x) * width)+(xCenter - y+1)] = nPixVal;

			// this starts from W side to NW
			if (((yCenter - x) * width)+(xCenter - y)>=0) output[((yCenter - x) * width)+(xCenter - y)] = nPixVal;
			if (((yCenter - x) * width)+(xCenter - y+1)>=0) output[((yCenter - x) * width)+(xCenter - y+1)] = nPixVal;

			x+=6;
			y = (int) (Math.sqrt(r2 - x*x) + 0.5);
		}

		if (x == y) 
		{
			// this is exact SE
			if (((yCenter + y) * width)+(xCenter + x)<output.length) output[((yCenter + y) * width)+(xCenter + x)] = nPixVal;
			if (((yCenter + y-1) * width)+(xCenter + x-1)<output.length) output[((yCenter + y-1) * width)+(xCenter + x-1)] = nPixVal;

			// this is exact NE
			if (((yCenter - y) * width)+(xCenter + x)>=0) output[((yCenter - y) * width)+(xCenter + x)] = nPixVal;
			if (((yCenter - y-1) * width)+(xCenter + x-1)>=0) output[((yCenter - y-1) * width)+(xCenter + x-1)] = nPixVal;

			// this is exact SW 
			if (((yCenter + y) * width)+(xCenter - x)<output.length) output[((yCenter + y) * width)+(xCenter - x)] = nPixVal;
			if (((yCenter + y-1) * width)+(xCenter - x+1)<output.length) output[((yCenter + y-1) * width)+(xCenter - x+1)] = nPixVal;

			// this is exact NW
			if (((yCenter - y) * width)+(xCenter - x)>=0) output[((yCenter - y) * width)+(xCenter - x)] = nPixVal;
			if (((yCenter - y-1) * width)+(xCenter - x+1)>=0) output[((yCenter - y-1) * width)+(xCenter - x+1)] = nPixVal;
		}
	}

	private void drawCircle(int nPixVal, int xCenter, int yCenter,int radius, int width, int height, int[] output) 
	{
		//pix = 250;
		//		int nPixVal = 0xffff0000;
		int x, y, r2;
		r2 = radius * radius;

		// this is exact S
		if ((((yCenter + radius) * width)+xCenter) < output.length) output[((yCenter + radius) * width)+xCenter] = nPixVal;
		if ((((yCenter + radius-1) * width)+xCenter) < output.length) output[((yCenter + radius-1) * width)+xCenter] = nPixVal;

		// this is exact W
		if (((yCenter - radius) * width)+xCenter>=0) output[((yCenter - radius) * width)+xCenter] = nPixVal;
		if (((yCenter - radius+1) * width)+xCenter>=0) output[((yCenter - radius+1) * width)+xCenter] = nPixVal;

		// this is exact E
		if ((yCenter * width)+(xCenter + radius)< output.length) output[(yCenter * width)+(xCenter + radius)] = nPixVal;
		if ((yCenter * width)+(xCenter + radius-1)< output.length) output[(yCenter * width)+(xCenter + radius-1)] = nPixVal;

		// this is exact N
		if ((yCenter * width)+(xCenter - radius)>=0) output[(yCenter * width)+(xCenter - radius)] = nPixVal;
		if ((yCenter * width)+(xCenter - radius+1)>=0) output[(yCenter * width)+(xCenter - radius+1)] = nPixVal;


		//y = radius;

		x = 1;
		y = (int) (Math.sqrt(r2 - 1) + 0.5);
		// start drawing at 90 degree (top side)

		while (x < y) 
		{
			// this starts from S side to SE
			if (((yCenter + y) * width)+(xCenter + x)<output.length) output[((yCenter + y) * width)+(xCenter + x)] = nPixVal;
			if (((yCenter + y-1) * width)+(xCenter + x)<output.length) output[((yCenter + y-1) * width)+(xCenter + x)] = nPixVal;

			// this starts from N side to NE
			if (((yCenter - y) * width)+(xCenter + x)>=0) output[((yCenter - y) * width)+(xCenter + x)] = nPixVal;
			if (((yCenter - y+1) * width)+(xCenter + x)>=0) output[((yCenter - y+1) * width)+(xCenter + x)] = nPixVal;

			// this starts from S side to SW
			if (((yCenter + y) * width)+(xCenter - x)<output.length) output[((yCenter + y) * width)+(xCenter - x)] = nPixVal;
			if (((yCenter + y-1) * width)+(xCenter - x)<output.length) output[((yCenter + y-1) * width)+(xCenter - x)] = nPixVal;

			// this starts from N side to NW
			if (((yCenter - y) * width)+(xCenter - x)>=0) output[((yCenter - y) * width)+(xCenter - x)] = nPixVal;
			if (((yCenter - y+1) * width)+(xCenter - x)>=0) output[((yCenter - y+1) * width)+(xCenter - x)] = nPixVal;

			// this starts from E side to SE
			if (((yCenter + x) * width)+(xCenter + y)<output.length) output[((yCenter + x) * width)+(xCenter + y)] = nPixVal;
			if (((yCenter + x) * width)+(xCenter + y-1)<output.length) output[((yCenter + x) * width)+(xCenter + y-1)] = nPixVal;

			// this starts from E side to NE			
			if (((yCenter - x) * width)+(xCenter + y)>=0) output[((yCenter - x) * width)+(xCenter + y)] = nPixVal;
			if (((yCenter - x) * width)+(xCenter + y-1)>=0) output[((yCenter - x) * width)+(xCenter + y-1)] = nPixVal;

			// this starts from W side to SW
			if (((yCenter + x) * width)+(xCenter - y)<output.length) output[((yCenter + x) * width)+(xCenter - y)] = nPixVal;
			if (((yCenter + x) * width)+(xCenter - y+1)<output.length) output[((yCenter + x) * width)+(xCenter - y+1)] = nPixVal;

			// this starts from W side to NW
			if (((yCenter - x) * width)+(xCenter - y)>=0) output[((yCenter - x) * width)+(xCenter - y)] = nPixVal;
			if (((yCenter - x) * width)+(xCenter - y+1)>=0) output[((yCenter - x) * width)+(xCenter - y+1)] = nPixVal;

			x++;
			y = (int) (Math.sqrt(r2 - x*x) + 0.5);
		}

		if (x == y) 
		{
			// this is exact SE
			if (((yCenter + y) * width)+(xCenter + x)<output.length) output[((yCenter + y) * width)+(xCenter + x)] = nPixVal;
			if (((yCenter + y-1) * width)+(xCenter + x-1)<output.length) output[((yCenter + y-1) * width)+(xCenter + x-1)] = nPixVal;

			// this is exact NE
			if (((yCenter - y) * width)+(xCenter + x)>=0) output[((yCenter - y) * width)+(xCenter + x)] = nPixVal;
			if (((yCenter - y-1) * width)+(xCenter + x-1)>=0) output[((yCenter - y-1) * width)+(xCenter + x-1)] = nPixVal;

			// this is exact SW 
			if (((yCenter + y) * width)+(xCenter - x)<output.length) output[((yCenter + y) * width)+(xCenter - x)] = nPixVal;
			if (((yCenter + y-1) * width)+(xCenter - x+1)<output.length) output[((yCenter + y-1) * width)+(xCenter - x+1)] = nPixVal;

			// this is exact NW
			if (((yCenter - y) * width)+(xCenter - x)>=0) output[((yCenter - y) * width)+(xCenter - x)] = nPixVal;
			if (((yCenter - y-1) * width)+(xCenter - x+1)>=0) output[((yCenter - y-1) * width)+(xCenter - x+1)] = nPixVal;
		}
	}



	private void drawCircle(int nPixVal, int xCenter, int yCenter,int radius, int width, int height, Image img) 
	{
		//pix = 250;
		//		int nPixVal = 0xffff0000;

		int x, y, r2;
		r2 = radius * radius;
		BufferedImage bimg = (BufferedImage) img;

		// this is exact S
		if (yCenter + radius < height) bimg.setRGB(xCenter, yCenter + radius, nPixVal);
		if (yCenter + radius-1 < height) bimg.setRGB(xCenter, yCenter + radius-1, nPixVal);

		// this is exact W
		if (xCenter - radius>=0) bimg.setRGB(xCenter - radius, yCenter, nPixVal);
		if (xCenter - radius+1>=0) bimg.setRGB(xCenter - radius+1, yCenter, nPixVal);

		// this is exact E
		if (xCenter + radius<width) bimg.setRGB(xCenter + radius, yCenter, nPixVal);
		if (xCenter + radius-1<width) bimg.setRGB(xCenter + radius-1, yCenter, nPixVal);

		// this is exact N
		if (yCenter - radius>=0) bimg.setRGB(xCenter, yCenter - radius, nPixVal);
		if (yCenter - radius+1>=0) bimg.setRGB(xCenter, yCenter - radius+1, nPixVal);

		//y = radius;

		x = 1;
		y = (int) (Math.sqrt(r2 - 1) + 0.5);
		// start drawing at 90 degree (top side)

		while (x < y) 
		{
			// this starts from S side to SE
			if (yCenter + y< height && xCenter + x < width) bimg.setRGB(xCenter + x, yCenter + y, nPixVal);
			if (yCenter + y-1< height && xCenter + x < width) bimg.setRGB(xCenter + x, yCenter + y-1, nPixVal);

			// this starts from N side to NE
			if (yCenter - y >=0 && xCenter + x < width) bimg.setRGB(xCenter + x, yCenter - y, nPixVal);
			if (yCenter - y+1 >=0 && xCenter + x < width) bimg.setRGB(xCenter + x, yCenter - y+1, nPixVal);

			// this starts from S side to SW
			if (yCenter + y < height && xCenter - x >= 0) bimg.setRGB(xCenter - x, yCenter + y, nPixVal);
			if (yCenter + y-1 < height && xCenter - x >= 0) bimg.setRGB(xCenter - x, yCenter + y-1, nPixVal);

			// this starts from N side to NW
			if (yCenter - y >= 0 && xCenter - x >= 0) bimg.setRGB(xCenter - x, yCenter - y, nPixVal);
			if (yCenter - y+1 >= 0 && xCenter - x >= 0) bimg.setRGB(xCenter - x, yCenter - y+1, nPixVal);

			// this starts from E side to SE
			if (yCenter + x < height && xCenter +y < width) bimg.setRGB(xCenter + y, yCenter + x, nPixVal);
			if (yCenter + x < height && xCenter +y-1 < width) bimg.setRGB(xCenter + y-1, yCenter + x, nPixVal);

			// this starts from E side to NE			
			if (yCenter - x >=0 && xCenter +y < width) bimg.setRGB(xCenter + y, yCenter - x, nPixVal);
			if (yCenter - x >=0 && xCenter +y-1 < width) bimg.setRGB(xCenter + y-1, yCenter - x, nPixVal);

			// this starts from W side to SW
			if (yCenter + x < height && xCenter -y >=0) bimg.setRGB(xCenter - y, yCenter + x, nPixVal);
			if (yCenter + x < height && xCenter -y+1 >=0) bimg.setRGB(xCenter - y+1, yCenter + x, nPixVal);

			// this starts from W side to NW
			if (yCenter - x >=0 && xCenter -y >=0) bimg.setRGB(xCenter - y, yCenter - x, nPixVal);
			if (yCenter - x >=0 && xCenter -y+1 >=0) bimg.setRGB(xCenter - y+1, yCenter - x, nPixVal);

			x++;
			y = (int) (Math.sqrt(r2 - x*x) + 0.5);
		}

		if (x == y) 
		{
			// this is exact SE
			if (yCenter + y < height && xCenter + x < width) bimg.setRGB(xCenter + x, yCenter + y, nPixVal);
			if (yCenter + y-1 < height && xCenter + x < width) bimg.setRGB(xCenter + x, yCenter + y-1, nPixVal);

			// this is exact NE
			if (yCenter - y >=0 && xCenter + x < width) bimg.setRGB(xCenter + x, yCenter - y, nPixVal);
			if (yCenter - y-1 >=0 && xCenter + x-1 < width) bimg.setRGB(xCenter + x-1, yCenter - y-1, nPixVal);

			// this is exact SW 
			if (yCenter + y < height && xCenter - x >=0) bimg.setRGB(xCenter - x, yCenter + y, nPixVal);
			if (yCenter + y-1 < height && xCenter - x+1 >=0) bimg.setRGB(xCenter - x+1, yCenter + y-1, nPixVal);

			// this is exact NW
			if (yCenter - y >=0&& xCenter - x >=0) bimg.setRGB(xCenter - x, yCenter - y, nPixVal);
			if (yCenter - y-1 >=0&& xCenter - x+1 >=0) bimg.setRGB(xCenter - x+1, yCenter - y-1, nPixVal);
		}
	}



	public void drawLine(int x,int y,int x2, int y2, int width, int height, int color,  int[] output) 
	{
		int w = x2 - x ;
		int h = y2 - y ;
		int dx1 = 0, dy1 = 0, dx2 = 0, dy2 = 0 ;
		if (w<0) dx1 = -1 ; else if (w>0) dx1 = 1 ;
		if (h<0) dy1 = -1 ; else if (h>0) dy1 = 1 ;
		if (w<0) dx2 = -1 ; else if (w>0) dx2 = 1 ;
		int longest = Math.abs(w) ;
		int shortest = Math.abs(h) ;
		if (!(longest>shortest)) 
		{
			longest = Math.abs(h) ;
			shortest = Math.abs(w) ;
			if (h<0) dy2 = -1 ; else if (h>0) dy2 = 1 ;
			dx2 = 0 ;            
		}
		int numerator = longest >> 1 ;

		for (int i=0;i<=longest;i++) 
		{
			output[y*width+x] = color;
			numerator += shortest ;
			if (!(numerator<longest)) 
			{
				numerator -= longest ;
				x += dx1 ;
				y += dy1 ;
			}
			else 
			{
				x += dx2 ;
				y += dy2 ;
			}
		}
	}


	public void drawLine(int x,int y,int x2, int y2, int width, int height, int color,  Image img) 
	{
		BufferedImage bimg = (BufferedImage) img;
		int w = x2 - x ;
		int h = y2 - y ;
		int dx1 = 0, dy1 = 0, dx2 = 0, dy2 = 0 ;
		if (w<0) dx1 = -1 ; else if (w>0) dx1 = 1 ;
		if (h<0) dy1 = -1 ; else if (h>0) dy1 = 1 ;
		if (w<0) dx2 = -1 ; else if (w>0) dx2 = 1 ;
		int longest = Math.abs(w) ;
		int shortest = Math.abs(h) ;
		if (!(longest>shortest)) 
		{
			longest = Math.abs(h) ;
			shortest = Math.abs(w) ;
			if (h<0) dy2 = -1 ; else if (h>0) dy2 = 1 ;
			dx2 = 0 ;            
		}
		int numerator = longest >> 1 ;

		for (int i=0;i<=longest;i++) 
		{
			bimg.setRGB(x, y, color);
			numerator += shortest ;
			if (!(numerator<longest)) 
			{
				numerator -= longest ;
				x += dx1 ;
				y += dy1 ;
			}
			else 
			{
				x += dx2 ;
				y += dy2 ;
			}
		}
	}

	private void drawCross(int x,int y, int nSize, int color,  Image img)
	{
		for (int ny=y-nSize;ny<y+nSize;ny++)
		{
			((BufferedImage) img).setRGB(x,ny,color);
			((BufferedImage) img).setRGB(x+1,ny,color);
		}
		
		for (int nx=x-nSize;nx<x+nSize;nx++)
		{
			((BufferedImage) img).setRGB(nx,y,color);
			((BufferedImage) img).setRGB(nx,y+1,color);
		}
	}

	public void drawDottedLine(int x,int y,int x2, int y2, int width, int height, int color,  int[] output) 
	{
		int w = x2 - x ;
		int h = y2 - y ;
		int dx1 = 0, dy1 = 0, dx2 = 0, dy2 = 0 ;
		if (w<0) dx1 = -1 ; else if (w>0) dx1 = 1 ;
		if (h<0) dy1 = -1 ; else if (h>0) dy1 = 1 ;
		if (w<0) dx2 = -1 ; else if (w>0) dx2 = 1 ;
		int longest = Math.abs(w) ;
		int shortest = Math.abs(h) ;
		if (!(longest>shortest)) 
		{
			longest = Math.abs(h) ;
			shortest = Math.abs(w) ;
			if (h<0) dy2 = -1 ; else if (h>0) dy2 = 1 ;
			dx2 = 0 ;            
		}
		int numerator = longest >> 1 ;

		for (int i=0;i<=longest;i++) 
		{
			output[y*width+x] = color;
			numerator += shortest ;
			if (!(numerator<longest)) 
			{
				numerator -= longest ;
				x += dx1 ;
				y += dy1 ;
			}
			else 
			{
				x += dx2 ;
				y += dy2 ;
			}
		}
	}

	
	
	public void drawDottedLine(int x,int y,int x2, int y2, int width, int height, int color,  Image img) 
	{
		int w = x2 - x ;
		int h = y2 - y ;
		
		int dx1 = 0, dy1 = 0, dx2 = 0, dy2 = 0 ;
		
		if (w<0) dx1 = -1 ; else if (w>0) dx1 = 1 ;
		if (h<0) dy1 = -1 ; else if (h>0) dy1 = 1 ;
		if (w<0) dx2 = -1 ; else if (w>0) dx2 = 1 ;
		
		int longest = Math.abs(w) ;
		int shortest = Math.abs(h) ;
		if (!(longest>shortest)) 
		{
			longest = Math.abs(h) ;
			shortest = Math.abs(w) ;
			if (h<0) dy2 = -1 ; else if (h>0) dy2 = 1 ;
			dx2 = 0 ;            
		}
		int numerator = longest >> 1 ;

		for (int i=0;i<=longest;i++) 
		{
			((BufferedImage) img).setRGB(x,y,color);
			numerator += shortest ;
			if (!(numerator<longest)) 
			{
				numerator -= longest ;
				x += dx1 ;
				y += dy1 ;
			}
			else 
			{
				x += dx2 ;
				y += dy2 ;
			}
		}
	}

	

	private int getGrayScale(int rgb)
	{
		int r = rgb >> 16 & 0xff;
			int g = rgb >> 8 & 0xff;
		int b = rgb & 0xff;
		int gray = (int)(0.21260000000000001D * (double)r + 0.71519999999999995D * (double)g + 0.0722D * (double)b);
		return gray;
	}

	
	
/**
 * Convert a number to a color using hsl, with range definition.
 * Example: if min/max are 0/1, and i is 0.75, the color is closer to green.
 * Example: if min/max are 0.5/1, and i is 0.75, the color is in the middle between red and green.
 * @param i (floating point, range 0 to 1)
 * param min (floating point, range 0 to 1, all i at and below this is red)
 * param max (floating point, range 0 to 1, all i at and above this is green)
 */

	private static int numberToColorHsl (float i, float RangeMin, float RangeMax) 
	{
	    float ratio = i;
	    if (RangeMin> 0 || RangeMax < 1) 
	    {
	        if (i < RangeMin) 
	        {
	            ratio = 0;
	        } else if (i > RangeMax) 
	        {
	            ratio = 1;
	        } else 
	        {
	            float range = RangeMax - RangeMin;
	            ratio = (i-RangeMin) / range;
	        }
	    }

	    int nG = (int) (ratio * 255);
	    int nR = (int) ((1-ratio) * 255);
	    int nB = 0;
	    
	    return (255 << 24) | (nR << 16) | (nG << 8) | nB;
	    
	    // as the function expects a value between 0 and 1, and red = 0¢X and green = 120¢X
	    // we convert the input to the appropriate hue value
	    //float hue = (float) (ratio * 1.2 / 3.60);
	    //if (minMaxFactor!=1) hue /= minMaxFactor;
	    //console.log(hue);

	    // we convert hsl to rgb (saturation 100%, lightness 50%)
	    //return HslToRgb(hue, 1, (float) 0.5);
	    // we format to css value and return
	    //return 'rgb(' + rgb[0] + ',' + rgb[1] + ',' + rgb[2] + ')'; 
	}
	
    public static int HslToRgb(float h, float s, float l) 
    { 
        int r, g, b; 
        if (h == 0) 
        { 
            // gray values 
            r = g = b = (byte) (l * 255); 
        }
        else 
        { 
            double v1, v2; 
            double hue = (double) h / 360; 
 
            v2 = (l < 0.5) ?(l * (1 + s)) : ((l + s) - (l * s)); 
            v1 = 2 * l - v2; 
 
            r = (byte) (255 * Hue_2_RGB(v1, v2, hue + (1.0 / 3))); 
            g = (byte) (255 * Hue_2_RGB(v1, v2, hue)); 
            b = (byte) (255 * Hue_2_RGB(v1, v2, hue - (1.0 / 3))); 
        } 
        return (255 << 24) | (r << 16) | (g << 8) | b; 
    } 
    
    private static double Hue_2_RGB(double v1, double v2, double vH) 
    { 
        if (vH < 0) 
            vH += 1; 
        if (vH > 1) 
            vH -= 1; 
        if ((6 * vH) < 1) 
            return (v1 + (v2 - v1) * 6 * vH); 
        if ((2 * vH) < 1) 
            return v2; 
        if ((3 * vH) < 2) 
            return (v1 + (v2 - v1) * ((2.0 / 3) - vH) * 6); 
        return v1; 
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
