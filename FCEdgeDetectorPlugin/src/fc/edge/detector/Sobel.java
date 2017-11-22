package fc.edge.detector;


public class Sobel
{

    public Sobel()
    {
    }

    public static int[][] Horizontal(int raw[][])
    {
        int out[][] = null;
        int height = raw.length;
        int width = raw[0].length;
        if(height > 2 && width > 2)
        {
            out = new int[height - 2][width - 2];
            for(int r = 1; r < height - 1; r++)
            {
                for(int c = 1; c < width - 1; c++)
                {
                    int sum = 0;
                    for(int kr = -1; kr < 2; kr++)
                    {
                        for(int kc = -1; kc < 2; kc++)
                            sum += MASK_H[kr + 1][kc + 1] * raw[r + kr][c + kc];

                    }

                    out[r - 1][c - 1] = sum;
                }

            }

        }
        return out;
    }

    public static int[][] Vertical(int raw[][])
    {
        int out[][] = null;
        int height = raw.length;
        int width = raw[0].length;
        if(height > 2 || width > 2)
        {
            out = new int[height - 2][width - 2];
            for(int r = 1; r < height - 1; r++)
            {
                for(int c = 1; c < width - 1; c++)
                {
                    int sum = 0;
                    for(int kr = -1; kr < 2; kr++)
                    {
                        for(int kc = -1; kc < 2; kc++)
                            sum += MASK_V[kr + 1][kc + 1] * raw[r + kr][c + kc];

                    }

                    out[r - 1][c - 1] = sum;
                }

            }

        }
        return out;
    }

    private static final int MASK_H[][] = {
        {
            -1, 0, 1
        }, {
            -2, 0, 2
        }, {
            -1, 0, 1
        }
    };
    private static final int MASK_V[][] = {
        {
            -1, -2, -1
        }, new int[3], {
            1, 2, 1
        }
    };

}
