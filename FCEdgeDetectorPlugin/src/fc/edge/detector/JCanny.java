package fc.edge.detector;

public class JCanny
{

    public JCanny()
    {
    }

    public static byte[] CannyEdges(byte img[], int numberDeviations, double fract, int width, int height)
    {
        int raw[][] = null;
        int blurred[][] = null;
        byte edges[] = null;
        numDev = numberDeviations;
        tFract = fract;
        if(img != null && numberDeviations > 0 && fract > 0.0D)
        {
            //raw = ImgIO.GSArray(img, width, height);
            blurred = Gaussian.BlurGS(raw, 4, 1.75D);
            gx = Sobel.Horizontal(blurred);
            gy = Sobel.Vertical(blurred);
            Magnitude();
            Direction();
            Suppression();
            //edges = ImgIO.GSImg(Hysteresis());
        }
        return edges;
    }

    private static void Magnitude()
    {
        double sum = 0.0D;
        double var = 0.0D;
        int height = gx.length;
        int width = gx[0].length;
        double pixelTotal = height * width;
        mag = new double[height][width];
        for(int r = 0; r < height; r++)
        {
            for(int c = 0; c < width; c++)
            {
                mag[r][c] = Math.sqrt(gx[r][c] * gx[r][c] + gy[r][c] * gy[r][c]);
                sum += mag[r][c];
            }

        }

        mean = (int)Math.round(sum / pixelTotal);
        for(int r = 0; r < height; r++)
        {
            for(int c = 0; c < width; c++)
            {
                double diff = mag[r][c] - (double)mean;
                var += diff * diff;
            }

        }

        stDev = (int)Math.sqrt(var / pixelTotal);
    }

    private static void Direction()
    {
        int height = gx.length;
        int width = gx[0].length;
        double piRad = 57.295779513082323D;
        dir = new int[height][width];
        for(int r = 0; r < height; r++)
        {
            for(int c = 0; c < width; c++)
            {
                double angle = Math.atan2(gy[r][c], gx[r][c]) * piRad;
                if(angle < 0.0D)
                    angle += 360D;
                if(angle <= 22.5D || angle >= 157.5D && angle <= 202.5D || angle >= 337.5D)
                    dir[r][c] = 0;
                else
                if(angle >= 22.5D && angle <= 67.5D || angle >= 202.5D && angle <= 247.5D)
                    dir[r][c] = 45;
                else
                if(angle >= 67.5D && angle <= 112.5D || angle >= 247.5D && angle <= 292.5D)
                    dir[r][c] = 90;
                else
                    dir[r][c] = 135;
            }

        }

    }

    private static void Suppression()
    {
        int height = mag.length - 1;
        int width = mag[0].length - 1;
        for(int r = 1; r < height; r++)
        {
            for(int c = 1; c < width; c++)
            {
                double magnitude = mag[r][c];
                switch(dir[r][c])
                {
                default:
                    break;

                case 0: // '\0'
                    if(magnitude < mag[r][c - 1] && magnitude < mag[r][c + 1])
                        mag[r - 1][c - 1] = 0.0D;
                    break;

                case 45: // '-'
                    if(magnitude < mag[r - 1][c + 1] && magnitude < mag[r + 1][c - 1])
                        mag[r - 1][c - 1] = 0.0D;
                    break;

                case 90: // 'Z'
                    if(magnitude < mag[r - 1][c] && magnitude < mag[r + 1][c])
                        mag[r - 1][c - 1] = 0.0D;
                    break;

                case 135: 
                    if(magnitude < mag[r - 1][c - 1] && magnitude < mag[r + 1][c + 1])
                        mag[r - 1][c - 1] = 0.0D;
                    break;
                }
            }

        }

    }

    private static int[][] Hysteresis()
    {
        int height = mag.length - 1;
        int width = mag[0].length - 1;
        int bin[][] = new int[height - 1][width - 1];
        tHi = mean + numDev * stDev;
        tLo = tHi * tFract;
        for(int r = 1; r < height; r++)
        {
            for(int c = 1; c < width; c++)
            {
                double magnitude = mag[r][c];
                if(magnitude >= tHi)
                    bin[r - 1][c - 1] = 255;
                else
                if(magnitude < tLo)
                {
                    bin[r - 1][c - 1] = 0;
                } else
                {
                    boolean connected = false;
                    for(int nr = -1; nr < 2; nr++)
                    {
                        for(int nc = -1; nc < 2; nc++)
                            if(mag[r + nr][c + nc] >= tHi)
                                connected = true;

                    }

                    bin[r - 1][c - 1] = connected ? 255 : 0;
                }
            }

        }

        return bin;
    }

    private static final int GAUSSIAN_RADIUS = 4;
    private static final double GAUSSIAN_INTENSITY = 1.75D;
    private static int stDev;
    private static int mean;
    private static int numDev;
    private static double tHi;
    private static double tLo;
    private static double tFract;
    private static int dir[][];
    private static int gx[][];
    private static int gy[][];
    private static double mag[][];
}
