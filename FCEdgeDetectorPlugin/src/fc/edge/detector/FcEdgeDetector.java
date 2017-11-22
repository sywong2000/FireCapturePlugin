package fc.edge.detector;

import de.wonderplanets.firecapture.plugin.*;
import java.awt.Rectangle;
import java.util.Properties;
import javax.swing.JButton;

public class FcEdgeDetector
    implements IFilter
{

    public FcEdgeDetector()
    {
        nGradientRange = 0;
    }

    public String getName()
    {
        return "FcEdgeDetector";
    }

    public String getDescription()
    {
        return "Sobel Edge Detector Plugin by Stpehen Wong";
    }

    public String getMaxValueLabel()
    {
        return null;
    }

    public String getCurrentValueLabel()
    {
        return null;
    }

    public String getStringUsage(int percent)
    {
        return (new StringBuilder("Intensity: ")).append(percent).append("%").toString();
    }

    public boolean useValueFields()
    {
        return false;
    }

    public boolean useSlider()
    {
        return true;
    }

    public String getMaxValue()
    {
        return null;
    }

    public String getCurrentValue()
    {
        return null;
    }

    public void sliderValueChanged(int value)
    {
        nGradientRange = value;
    }

    public int getInitialSliderValue()
    {
        return 50;
    }

    public void imageSizeChanged()
    {
    }

    public void filterChanged(String s, String s1)
    {
    }

    public void activated()
    {
    }

    public void release()
    {
    }

    public boolean capture()
    {
        return false;
    }

    public void computeMono(byte bytePixels[], Rectangle imageSize, CamInfo info)
    {
        int x = imageSize.width;
        int y = imageSize.height;
        int bytePix[] = new int[bytePixels.length];
        int nMaxGrad = -1;
        for(int i = 1; i < x - 1; i++)
        {
            for(int j = 1; j < y - 1; j++)
            {
                int val00 = bytePixels[(j - 1) * x + (i - 1)];
                int val01 = bytePixels[j * x + (i - 1)];
                int val02 = bytePixels[(j + 1) * x + (i - 1)];
                int val10 = bytePixels[(j - 1) * x + i];
                int val11 = bytePixels[j * x + i];
                int val12 = bytePixels[(j + 1) * x + i];
                int val20 = bytePixels[(j - 1) * x + (i + 1)];
                int val21 = bytePixels[j * x + (i + 1)];
                int val22 = bytePixels[(j + 1) * x + (i + 1)];
                int gx = -1 * val00 + 0 * val01 + 1 * val02 + (-2 * val10 + 0 * val11 + 2 * val12) + (-1 * val20 + 0 * val21 + 1 * val22);
                int gy = -1 * val00 + -2 * val01 + -1 * val02 + (0 * val10 + 0 * val11 + 0 * val12) + (1 * val20 + 2 * val21 + 1 * val22);
                double gval = Math.sqrt(gx * gx + gy * gy);
                nMaxGrad = (double)nMaxGrad <= gval ? (int)gval : nMaxGrad;
                bytePix[j * x + i] = (int)gval;
            }

        }

        for(int n = 0; n < bytePix.length; n++)
            bytePixels[n] = (byte)Math.min(255, (((bytePix[n] * 255) / nMaxGrad) * nGradientRange) / 10);

    }

    public void computeColor(int rgbPixels[], Rectangle imageSize, CamInfo info)
    {
        int x = imageSize.width;
        int y = imageSize.height;
        int bytePix[] = new int[rgbPixels.length];
        int nMaxGrad = -1;
        for(int i = 1; i < x - 1; i++)
        {
            for(int j = 1; j < y - 1; j++)
            {
                int val00 = getGrayScale(rgbPixels[(j - 1) * x + (i - 1)]);
                int val01 = getGrayScale(rgbPixels[j * x + (i - 1)]);
                int val02 = getGrayScale(rgbPixels[(j + 1) * x + (i - 1)]);
                int val10 = getGrayScale(rgbPixels[(j - 1) * x + i]);
                int val11 = getGrayScale(rgbPixels[j * x + i]);
                int val12 = getGrayScale(rgbPixels[(j + 1) * x + i]);
                int val20 = getGrayScale(rgbPixels[(j - 1) * x + (i + 1)]);
                int val21 = getGrayScale(rgbPixels[j * x + (i + 1)]);
                int val22 = getGrayScale(rgbPixels[(j + 1) * x + (i + 1)]);
                int gx = -1 * val00 + 0 * val01 + 1 * val02 + (-2 * val10 + 0 * val11 + 2 * val12) + (-1 * val20 + 0 * val21 + 1 * val22);
                int gy = -1 * val00 + -2 * val01 + -1 * val02 + (0 * val10 + 0 * val11 + 0 * val12) + (1 * val20 + 2 * val21 + 1 * val22);
                int gval = (int)Math.sqrt(gx * gx + gy * gy);
                bytePix[j * x + i] = gval;
                nMaxGrad = nMaxGrad <= gval ? gval : nMaxGrad;
            }

        }

        for(int n = 0; n < bytePix.length; n++)
        {
            int gval = (((bytePix[n] * 255) / nMaxGrad) * nGradientRange) / 10;
            gval = 0xff000000 | gval << 16 | gval << 8 | gval;
            rgbPixels[n] = gval;
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

    public void captureStoped()
    {
    }

    public void captureStarted()
    {
    }

    public boolean isNullFilter()
    {
        return false;
    }

    public boolean processEarly()
    {
        return false;
    }

    public boolean supportsColor()
    {
        return true;
    }

    public boolean supportsMono()
    {
        return true;
    }

    public void registerFilterListener(IFilterListener ifilterlistener)
    {
    }

    public JButton getButton()
    {
        return null;
    }

    public String getInterfaceVersion()
    {
        return "1.1";
    }

    public String getFilenameAppendix()
    {
        return null;
    }

    public void appendToLogfile(Properties properties1)
    {
    }

    private int nGradientRange;
}
