package rosswilhite.mustrainer;


/**
 * Created by Ross Wilhite on 3/10/2017.
 */

public class RossDSP {

    public static void computeSecondOrderLowPassParameters( float SampleRate, float fc, double a[], double b[] )
    {
        double a0;
        double w0 = 2 * Math.PI * fc/SampleRate;
        double cosw0 = Math.cos(w0);
        double sinw0 = Math.sin(w0);
        //float alpha = sinw0/2;
        double alpha = sinw0/2 * Math.sqrt(2);

        a0   = 1 + alpha;
        a[0] = (-2*cosw0) / a0;
        a[1] = (1 - alpha) / a0;
        b[0] = ((1-cosw0)/2) / a0;
        b[1] = ( 1-cosw0) / a0;
        b[2] = b[0];
    }
    public static double processSecondOrderFilter( double x, double mem[], double a[], double b[] )
    {
        double ret = b[0] * x + b[1] * mem[0] + b[2] * mem[1]
                - a[0] * mem[2] - a[1] * mem[3] ;

        mem[1] = mem[0];
        mem[0] = x;
        mem[3] = mem[2];
        mem[2] = ret;

        return ret;
    }
}
