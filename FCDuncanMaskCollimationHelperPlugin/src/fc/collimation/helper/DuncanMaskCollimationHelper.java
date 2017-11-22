package fc.collimation.helper;
import java.awt.Rectangle;
import java.util.Properties;
import javax.swing.JButton;

import de.wonderplanets.firecapture.plugin.CamInfo;
import de.wonderplanets.firecapture.plugin.IFilter;
import de.wonderplanets.firecapture.plugin.IFilterListener;


public class DuncanMaskCollimationHelper implements IFilter 
{

	static double[] CurDirection;
	static int nCurWidth=0;
	static int nCurHeight=0;
	static int[] CurOrig;
	static int[] CurOutput;
	static int[] CurAcc;

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
		// suggest downsample to 1024 * 1024
		
		
		
		if (imageSize.width!=nCurWidth || imageSize.height!=nCurHeight )
		{
			CurDirection = new double[bytePixels.length];
			CurOrig = new int[bytePixels.length];
			CurAcc = new int[bytePixels.length];
			CurOutput = new int[bytePixels.length];
			nCurWidth = imageSize.width;
			nCurHeight = imageSize.height;
		}
		
		int lines=1;
		int radius=30;

		for (int n=0;n<bytePixels.length;n++)
		{
			CurOrig[n] = bytePixels[n]&0xFF;
		}

		sobel sobelObject = new sobel();
		sobelObject.init(CurOrig,imageSize.width,imageSize.height,CurDirection,CurOutput);
		sobelObject.process2();
		
		//CurDirection=sobelObject.getDirection();

		nonMaxSuppression nonMaxSuppressionObject = new nonMaxSuppression(); 
		nonMaxSuppressionObject.init(CurOutput,CurDirection,imageSize.width,imageSize.height, CurOrig);
		nonMaxSuppressionObject.process();

		hystThresh hystThreshObject = new hystThresh();
		hystThreshObject.init(CurOrig,imageSize.width,imageSize.height, 25,50,CurOutput);
		hystThreshObject.process();

		circleHough circleHoughObject = new circleHough();
		circleHoughObject.init(CurOutput,imageSize.width,imageSize.height, radius, CurOrig, CurAcc, lines);

		circleHoughObject.process();

		for (int n=0;n<bytePixels.length;n++)
		{
			bytePixels[n] = (byte)Math.min(255, CurOrig[n]);
		}

		
		//OverlayImage = createImage(new MemoryImageSource(width, height, overlayImage(orig), 0, width));

		//		int rmax = (int)Math.sqrt(imageSize.width*imageSize.width + imageSize.height*imageSize.height);
		//		int acc[] = new int[imageSize.width * imageSize.height];
		//		acc=circleHoughObject.getAcc();

		//HoughAccImage = createImage(new MemoryImageSource(width, height, acc, 0, width)).getScaledInstance(256, 256, Image.SCALE_SMOOTH);
		//LinesImage = createImage(new MemoryImageSource(width, height, orig, 0, width));


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
