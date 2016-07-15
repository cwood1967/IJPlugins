package org.stowers.microscopy.ij1plugins;

/**
 * Created by cjw on 7/12/16.
 */
public class Histogram {

    int[] counts;
    double[] bins;
    double[] data;

    int nbins;
    double dataMin;
    double dataMax;
    double startx;
    double endx;

    public Histogram(double[] data, int xnbins) {
        this.data = data;
        this.nbins = xnbins;
        findDataMinMax();
        makeBins();
        counts = new int[nbins];
        System.out.println(dataMin + " " + dataMax);
        doHist();
    }

    protected int[] doHist() {

        for (int i = 0; i < data.length; i++) {

            double x = data[i];
            int k = (int)(nbins*(x - startx)/(endx - startx));
            try {
                counts[k]++;
            }
            catch (Exception e) {
                System.out.println(x + " " + k + " " + startx + " " + endx);
            }
//            System.out.println(x + " " + k);
        }

        return counts;
    }

    public double[] getBins() {
        return bins;
    }

    public double binMean(int interval) {

        int i1 = interval;
        int i2 = interval + 1;

        double md = (bins[i2] + bins[i1])/2.;
        System.out.println(interval + " " + md);
        return md;
    }

    protected void findDataMinMax() {

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (int i = 0; i < data.length; i++) {

            double x = data[i];
            if (x > max) {
                max = x;
            }
            if ( x < min) {
                    min = x;
            }
        }

        dataMin = min;
        dataMax = max;
        startx = dataMin - .01*(dataMax - dataMin);
        endx = dataMax + .01*(dataMax - dataMin);;
    }

    protected void makeBins() {

        double d = (endx - startx)/nbins;
        bins = new double[nbins + 1];

        for (int i = 0; i < bins.length; i++) {
            bins[i] = startx + i*d;
        }
    }

    public double getStartX() {
        return startx;
    }

    public double getEndX() {
        return endx;
    }

    public double getNBins() {
        return nbins;
    }
}
