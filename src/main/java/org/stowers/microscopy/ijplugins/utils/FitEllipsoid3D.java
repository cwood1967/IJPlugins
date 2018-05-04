package org.stowers.microscopy.ijplugins.utils;

import org.apache.commons.math3.linear.*;

public class FitEllipsoid3D {

    double[][] xyz;
    double cx;
    double cy;
    double cz;
    double[] com;
    double volume;

    public FitEllipsoid3D(double[][] xyz) {

        this.xyz = xyz;
        volume = xyz[0].length;
        calcCOM();
    }

    public void calcMoments() {

        double[][] Im = new double[3][3];

        for (int j = 0; j < 3; j++) {
            for (int k = 0; k < 3; k++) {
                if (j == k) {
                    Im[j][k] = calcMomentDiag(j);
                } else {
                    Im[j][k] = calcMoment(j, k);
                }
               // System.out.println(j + " " + k + " " + Im[j][k]);
            }
        }

        RealMatrix matrix = MatrixUtils.createRealMatrix(Im);
        EigenDecomposition eigen = new EigenDecomposition(matrix);
        RealMatrix v = eigen.getV();
        double[] ev = eigen.getRealEigenvalues();

        for (int j = 0; j < 3; j++) {
            RealVector eva = eigen.getEigenvector(j);
            for (int k = 0; k < 3; k++) {
                //System.out.print(v.getEntry(k,j) + " ");
                System.out.print(eva.getEntry(k) + " ");
            }
            System.out.println("------: " + ev[j]);
        }
        double vc = (4./3)*Math.PI*Math.sqrt(5.*ev[0]*ev[1]*ev[2]);
        System.out.println(vc + " " + volume);
        System.out.println("------" );
        double ev1 = ev[0];
    }

    private void calcCOM() {

        cx = calcAverage(xyz[0]);
        cy = calcAverage(xyz[1]);
        cz = calcAverage(xyz[2]);

        com = new double[3];
        com[0] = cx;
        com[1] = cy;
        com[2] = cz;
    }

    private double calcAverage(double[] v) {

        double sum = 0;
        for (int i = 0; i < v.length; i++) {
            sum += v[i];
        }

        return sum / v.length;
    }

    private double calcMomentDiag(int j) {

        double sum = 0;
        for (int i = 0; i < xyz[0].length; i ++) {
            double se = xyz[j][i] - com[j];
            sum += calcLengthSquared(i) - se*se;
        }

        return sum/volume;
    }

    private double calcLengthSquared(int i) {

        double ax = xyz[0][i] - cx;
        double ay = xyz[1][i] - cy;
        double az = xyz[2][i] - cz;

        return ax*ax + ay*ay + az*az;
    }

    private double calcMoment(int j, int k) {

        double[] e1 = xyz[j];
        double c1 = com[j];
        double[] e2 = xyz[k];
        double c2 = com[k];

        double sum = 0;
        for (int i = 0; i < e1.length; i++) {
            double p1 = e1[i] - c1;
            double p2 = e2[i] - c2;
            sum += p1 * p2;
        }

        return -sum/volume;
    }
}
