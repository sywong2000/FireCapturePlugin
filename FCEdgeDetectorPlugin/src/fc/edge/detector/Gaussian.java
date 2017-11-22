package fc.edge.detector;


public class Gaussian
{

    public Gaussian()
    {
    }

    public static int[][][] BlurRGB(int raw[][][], int rad, double intens)
    {
        int height = raw.length;
        int width = raw[0].length;
        double intensSquared2 = 2D * intens * intens;
        double invIntensSqrPi = 1.0D / (SQRT2PI * intens);
        double norm = 0.0D;
        double mask[] = new double[2 * rad + 1];
        int outRGB[][][] = new int[height - 2 * rad][width - 2 * rad][3];
        for(int x = -rad; x < rad + 1; x++)
        {
            double exp = Math.exp(-((double)(x * x) / intensSquared2));
            mask[x + rad] = invIntensSqrPi * exp;
            norm += mask[x + rad];
        }

        for(int r = rad; r < height - rad; r++)
        {
            for(int c = rad; c < width - rad; c++)
            {
                double sum[] = new double[3];
                for(int mr = -rad; mr < rad + 1; mr++)
                {
                    for(int chan = 0; chan < 3; chan++)
                        sum[chan] += mask[mr + rad] * (double)raw[r][c + mr][chan];

                }

                for(int chan = 0; chan < 3; chan++)
                {
                    sum[chan] /= norm;
                    outRGB[r - rad][c - rad][chan] = (int)Math.round(sum[chan]);
                }

            }

        }

        for(int r = rad; r < height - rad; r++)
        {
            for(int c = rad; c < width - rad; c++)
            {
                double sum[] = new double[3];
                for(int mr = -rad; mr < rad + 1; mr++)
                {
                    for(int chan = 0; chan < 3; chan++)
                        sum[chan] += mask[mr + rad] * (double)raw[r + mr][c][chan];

                }

                for(int chan = 0; chan < 3; chan++)
                {
                    sum[chan] /= norm;
                    outRGB[r - rad][c - rad][chan] = (int)Math.round(sum[chan]);
                }

            }

        }

        return outRGB;
    }

    public static int[][] BlurGS(int raw[][], int rad, double intens)
    {
        int height = raw.length;
        int width = raw[0].length;
        double norm = 0.0D;
        double intensSquared2 = 2D * intens * intens;
        double invIntensSqrPi = 1.0D / (SQRT2PI * intens);
        double mask[] = new double[2 * rad + 1];
        int outGS[][] = new int[height][width];
        for(int x = -rad; x < rad + 1; x++)
        {
            double exp = Math.exp(-((double)(x * x) / intensSquared2));
            mask[x + rad] = invIntensSqrPi * exp;
            norm += mask[x + rad];
        }

        for(int r = rad; r < height - rad; r++)
        {
            for(int c = rad; c < width - rad; c++)
            {
                double sum = 0.0D;
                for(int mr = -rad; mr < rad + 1; mr++)
                    sum += mask[mr + rad] * (double)raw[r][c + mr];

                sum /= norm;
                outGS[r - rad][c - rad] = (int)Math.round(sum);
            }

        }

        for(int r = rad; r < height - rad; r++)
        {
            for(int c = rad; c < width - rad; c++)
            {
                double sum = 0.0D;
                for(int mr = -rad; mr < rad + 1; mr++)
                    sum += mask[mr + rad] * (double)raw[r + mr][c];

                sum /= norm;
                outGS[r - rad][c - rad] = (int)Math.round(sum);
            }

        }

        return outGS;
    }

    private static final double SQRT2PI = Math.sqrt(6.2831853071795862D);

}
