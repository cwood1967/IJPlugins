package org.stowers.microscopy.segmentation;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath="Plugins>Chris>Z Correlation")
public class ZCorrelation implements Command, Previewable {

    @Parameter
    ImagePlus imp;

    int sizeT;
    int sizeZ;

    double[] kernel;
    @Override
    public void run() {
        sizeT = imp.getNFrames();
        sizeZ = imp.getNSlices();
        KernelTanh kern = new KernelTanh(2., .9, .85, sizeZ);
        kernel = kern.calcTanh();
        zcorrelate();
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
        double[] zs = SegUtils.stackEdgeZScore(stack);

        for (int k = 0; k < sizeT; k++) {
            //imp.setT(k + 1);
            int nx = stack.getWidth();
            int ny = stack.getHeight();

            int slice = imp.getStackIndex(1, 1, k + 1) - 1;
            int bestZ = calcBestZ(zs, slice, sizeZ);

            int kstart = bestZ - kernel.length/2;
            int kstop = kstart + kernel.length;

            ImageProcessor cip = new FloatProcessor(imp.getWidth(), imp.getHeight());
            System.out.println(k + " " + slice + " " + bestZ);
            float[] pix = new float[imp.getWidth()*imp.getHeight()];
            int pindex = 0;
            for (int j = 0; j < ny; j++) {
                for (int i = 0; i < nx; i++) {
                    //System.out.println(i + " " + j + " " + slice);

                    float[] zp = stack.getVoxels(i, j, slice, 1, 1, sizeZ, null);
                    double sum = 0;
                    int index = 0;
                    int ki = kstart;
                    for (float p :zp) {
                        //do correlation here
                        // only need to do it for best
                        if ((ki >= 0) & (ki < kstop)) {
                            sum += p*kernel[index];
                        }
                        index++;
                        ki++;
                        pix[j*nx + i] = (float)sum;
                        pindex++;

                    }
                }
            }
            corr_stack.setPixels(pix, k + 1);

        }

        ImagePlus cimp = new ImagePlus();
        cimp.setStack(corr_stack);
        cimp.show();
        return null;
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
