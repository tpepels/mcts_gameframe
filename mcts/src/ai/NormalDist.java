package ai;

public class NormalDist {

    public static double getNormalValue(double mean, double stddev, double x) {
        return (1./(stddev * Math.sqrt(2*  Math.PI))) * Math.exp(-(Math.pow(x - mean, 2)/(2*Math.pow(stddev,2))));
    }
}
