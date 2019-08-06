package org.stowers.microscopy.segmentation;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.ImageWindow;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath="Plugins>Stowers>Chris>Z Correlation")
public class ZCorrelation implements Command, Previewable {

    @Parameter
    ImagePlus imp;

    ImagePlus cimp = null;

    int sizeT;
    int sizeZ;

    double[] kernel;
    @Override
    public void run() {
        sizeT = imp.getNFrames();
        sizeZ = imp.getNSlices();
        KernelTanh kern = new KernelTanh(2., .9, .85, sizeZ);
        kernel = kern.calcTanh();
        cimp = zcorrelate();
        cimp.show();

    }

    @Override
    public void preview() {

    }

    @Override
    public void cancel() {

    }

    public ImagePlus zcorrelate() {

        ImageStack stack = imp.getStack();
        ImageStack corr_stack = new ImageStack(imp.getWidth(), imp.getHeight(), sizeT);
        IJ.showStatus("Calculating best Z");
        double[] zs = SegUtils.stackEdgeZScore(stack);

        IJ.showStatus("Correlating Z");
        for (int k = 0; k < sizeT; k++) {
            //imp.setT(k + 1);
            IJ.showProgress(k, sizeT);
            int nx = stack.getWidth();
            int ny = stack.getHeight();

            int slice = imp.getStackIndex(1, 1, k + 1) - 1;
            int bestZ = calcBestZ(zs, slice, sizeZ);

            int kstart = bestZ - kernel.length/2;
            if (kstart < 0) {
                kstart = 0;
            }
            int kstop = kstart + kernel.length;
            if (kstop > sizeZ) {
                kstop = sizeZ;
                kstart = kstop - kernel.length;
            }
            ImageProcessor cip = new FloatProcessor(imp.getWidth(), imp.getHeight());
            System.out.println(k + " " + slice + " " + bestZ);
            double[] dpix = new double[imp.getWidth()*imp.getHeight()];
            float[] pix = new float[imp.getWidth()*imp.getHeight()];

            double pmax = -1.e20;
            double pmin = 1e20;
            for (int j = 0; j < ny; j++) {
                for (int i = 0; i < nx; i++) {
                    //System.out.println(i + " " + j + " " + slice);

                    float[] zp = stack.getVoxels(i, j, slice, 1, 1, sizeZ, null);
                    double sum = 0;

                    int ki = 0; // for index of kernel
                    //index for index of z profile
                    for (int zi = kstart; zi < kstop; zi++) {
                        //do correlation here
                        // only need to do it for best
                        if (zi >= zp.length) {
                            break;
                        }
                        if ((zi >= 0) & (zi < kstop)) {
                            float p = zp[zi];
                            sum += p*kernel[ki];
                            ki++;
                        }

                        dpix[j*nx + i] = sum;

                    }
                    if (sum > pmax) {
                        pmax = sum;
                    } else if (sum < pmin) {
                        pmin = sum;
                    }
                }
            }

            double px = -1e30;
            double pm = 1e30;
            for (int ii = 0; ii < dpix.length; ii++) {
                double p = 2.0*(dpix[ii] - pmin)/(pmax - pmin) - 1.0;
                if (p < pm) {
                    pm = p;
                } else if (p > px) {
                    px = p;
                }
                pix[ii] = (float)p;
            }
            System.out.println(k + " " + px + " " + pm + " " + pmin + " " + pmax);
            corr_stack.setPixels(pix, k + 1);
        }

        ImagePlus cimp = new ImagePlus();
        cimp.setTitle("ZCORR_" + imp.getTitle());
        cimp.setStack(corr_stack);
        return cimp;
    }

    private int calcBestZ(double[] zScores, int z0, int d) {

        int best = 0;
        double minv = 9e19;
        for (int i = 0; i < d; i++) {
            double v = zScores[z0 + i];
            if ( v < minv) {
                minv = v;
                best = i;
            }
        }

        return best;
    }
}
