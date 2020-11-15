public class Utilities
{
    /**
     * Returns the log base b of x.
     * Works Cited: My CS310 project
     */
    public static double logBase(double b, double x)
    {
        assert b > 1.0;
        // change of base formula
        return Math.log(x) / Math.log(b);
    }
}
