package org.stowers.microscopy.segmentation;

import java.util.ArrayList;

public class KernelTanh {

    double rate;
    double size;
    double clip;
    int max_length;

    public KernelTanh(double rate, double size, double clip, int max_length) {
        this.rate = rate;
        this.size= size;
        this.clip = clip;
        this.max_length = max_length;
    }

    public double[] calcTanh() {

        double xf  = 2.5;
        double x0 =  -2.5;
        double dx = (xf - x0)/max_length;
        int n = 0;
        ArrayList<Double> tanhList = new ArrayList<>();
        while (n < max_length) {
            double x = x0 + n*dx;
            double tanh = -size*Math.tanh(rate*x);
            if (Math.abs(tanh) < clip) {
                tanhList.add(tanh);
            }
            n++;
        }

        double[] res = new double[tanhList.size()];
        int na = tanhList.size();
        for (int i = 0; i < na; i++) {
            res[i] = tanhList.get(i);
        }
        return res;
    }
}
