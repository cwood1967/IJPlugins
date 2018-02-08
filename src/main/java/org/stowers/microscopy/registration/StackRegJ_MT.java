package org.stowers.microscopy.registration;

import ij.*;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.*;
import jguis.*;
import jalgs.*;
import java.util.*;
import ij.text.*;
import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath="Plugins>Stowers>Chris>StackRegJ_MultiThreaded")
public class StackRegJ_MT implements Command, Previewable {
	//this plugin is a highly modified version of the StackReg plugin available from http://bigwww.epfl.ch/thevenaz/stackreg/
	//this version will output the transformation for the selected slice and channel of a hyperstack
	//the alignment outputs a translation trajectory for further alignments
	int slices,channels,frames,targetFrame,targetChannel,targetSlice,slices2,channels2,frames2;

	public void run () {
		final ImagePlus imp = WindowManager.getCurrentImage();
		GenericDialog gd = new GenericDialog("StackRegJ");
		final String[] transformationItem = {
			"Translation",
			"Rigid Body",
			"Scaled Rotation",
			"Affine"
		};
		gd.addChoice("Transformation:", transformationItem, "Rigid Body");
		gd.addCheckbox("Output_Trans_Trajectory",true);
		//gd.addCheckbox("Align_Secondary_Image",false);
		gd.showDialog();
		if (gd.wasCanceled()) {return;}
		final int transformation = gd.getNextChoiceIndex();
		boolean outtrans=gd.getNextBoolean();
		//boolean secondary=gd.getNextBoolean();
		final int width = imp.getWidth();
		final int height = imp.getHeight();
		slices=imp.getNSlices();
		channels=imp.getNChannels();
		frames=imp.getNFrames();
		targetFrame = imp.getFrame();
		targetChannel = imp.getChannel();
		targetSlice=imp.getSlice();
		/*ImagePlus simp=null;
		if(secondary){
			ImagePlus[] images=jutils.selectImages(false,1,new String[]{"Secondary_Image"});
			if(images!=null){
				simp=images[0];
				slices2=simp.getNSlices();
				channels2=simp.getNChannels();
				frames2=simp.getNFrames();
			}
		}*/
		if(frames==1){
			frames=slices;
			slices=1;
			targetFrame=targetSlice;
			targetSlice=1;
			frames2=slices2;
			slices2=1;
		}
		/*if(secondary && (frames2!=frames)){
			IJ.showMessage("Number of frames in images doesn't match, ignoring secondary");
			simp=null;
		}*/
		double[][] globalTransform = {
			{1.0, 0.0, 0.0},
			{0.0, 1.0, 0.0},
			{0.0, 0.0, 1.0}
		};
		double[][] anchorPoints = null;
		switch (transformation) {
			case 0: {
				anchorPoints = new double[1][3];
				anchorPoints[0][0] = (double)(width / 2);
				anchorPoints[0][1] = (double)(height / 2);
				anchorPoints[0][2] = 1.0;
				break;
			}
			case 1: {
				anchorPoints = new double[3][3];
				anchorPoints[0][0] = (double)(width / 2);
				anchorPoints[0][1] = (double)(height / 2);
				anchorPoints[0][2] = 1.0;
				anchorPoints[1][0] = (double)(width / 2);
				anchorPoints[1][1] = (double)(height / 4);
				anchorPoints[1][2] = 1.0;
				anchorPoints[2][0] = (double)(width / 2);
				anchorPoints[2][1] = (double)((3 * height) / 4);
				anchorPoints[2][2] = 1.0;
				break;
			}
			case 2: {
				anchorPoints = new double[2][3];
				anchorPoints[0][0] = (double)(width / 4);
				anchorPoints[0][1] = (double)(height / 2);
				anchorPoints[0][2] = 1.0;
				anchorPoints[1][0] = (double)((3 * width) / 4);
				anchorPoints[1][1] = (double)(height / 2);
				anchorPoints[1][2] = 1.0;
				break;
			}
			case 3: {
				anchorPoints = new double[3][3];
				anchorPoints[0][0] = (double)(width / 2);
				anchorPoints[0][1] = (double)(height / 4);
				anchorPoints[0][2] = 1.0;
				anchorPoints[1][0] = (double)(width / 4);
				anchorPoints[1][1] = (double)((3 * height) / 4);
				anchorPoints[1][2] = 1.0;
				anchorPoints[2][0] = (double)((3 * width) / 4);
				anchorPoints[2][1] = (double)((3 * height) / 4);
				anchorPoints[2][2] = 1.0;
				break;
			}
			default: {
				IJ.error("Unexpected transformation");
				return;
			}
		}
		//ImagePlus target = new ImagePlus("StackRegTarget",getImpProcessor(imp,targetChannel,targetSlice,targetFrame));
		float[][] trans=new float[3][frames];
		//target.show();
		double[][][] transforms=new double[frames][][];

		//try finding the local transforms first: the old getLocalTransform had be done in the same order as the transformations to get target swaps to work right
		//the new version of getLocalTransform circumvents this
		double[][][] localtransforms=new double[frames][][];

		long t1 = System.currentTimeMillis();

		List<RegThread> threads = new ArrayList<>();
		for(int f= (targetFrame - 1); f>0; f--) {
			//localtransforms[f-1]=getLocalTransform(target,imp,width,height,transformation,f);
            threads.add(new RegThread(imp, transformation, f, f +1 , targetChannel, targetSlice));

			//localtransforms[f-1]=RegUtils.getLocalTransform(imp,width,height,transformation,
            //        targetChannel, targetSlice, f,f+1);
			//IJ.showStatus("Frame "+f+" Registered");
		}

		for (int f=(targetFrame+1); f<=frames; f++) {
			//localtransforms[f-1]=getLocalTransform(target,imp,width,height,transformation,f);
            threads.add(new RegThread(imp, transformation, f, f - 1 , targetChannel, targetSlice));
			//localtransforms[f-1]=RegUtils.getLocalTransform(imp,width,height,transformation,
            //        targetChannel, targetSlice, f,f-1);
			//IJ.showStatus("Frame "+f+" Registered");
		}

		threads.parallelStream()
                .forEach(s -> s.calcLocalTransform(localtransforms));

		//and then accumulate the global transformations
		for (int f = (targetFrame - 1); f>0; f--) {
			double[][] rescued=clone_multidim_array(globalTransform);
			//here multiply the global transformation by the recent local transform to add all previous transformations
			for (int i = 0; (i < 3); i++) {
				for (int j = 0; (j < 3); j++) {
					globalTransform[i][j] = 0.0;
					for (int k = 0; (k < 3); k++) {
						globalTransform[i][j] += localtransforms[f-1][i][k] * rescued[k][j];
					}
				}
			}
			transforms[f-1]=clone_multidim_array(globalTransform);
			//if(!transformFrame(imp, width, height,transformation, transforms[f-1], anchorPoints, f)) return;
			float[] trans2=get_translation(globalTransform,width,height);
			trans[0][f-1]=trans2[0]; trans[1][f-1]=trans2[1]; trans[2][f-1]=trans2[2];
		}
		if ((1 < targetFrame) && (targetFrame < frames)) {
			//reset the global transformation for the upper portion of the stack
			globalTransform[0][0] = 1.0; globalTransform[0][1] = 0.0; globalTransform[0][2] = 0.0;
			globalTransform[1][0] = 0.0; globalTransform[1][1] = 1.0; globalTransform[1][2] = 0.0;
			globalTransform[2][0] = 0.0; globalTransform[2][1] = 0.0; globalTransform[2][2] = 1.0;
			//target.getProcessor().copyBits(getImpProcessor(imp,targetChannel,targetSlice,targetFrame), 0, 0, Blitter.COPY);
		}
		transforms[targetFrame-1]=clone_multidim_array(globalTransform);
		for (int f=(targetFrame+1); f<=frames; f++) {
			double[][] rescued=clone_multidim_array(globalTransform);
			//here multiply the global transformation by the recent local transform to add all previous transformations
			for (int i = 0; (i < 3); i++) {
				for (int j = 0; (j < 3); j++) {
					globalTransform[i][j] = 0.0;
					for (int k = 0; (k < 3); k++) {
						globalTransform[i][j] += localtransforms[f-1][i][k] * rescued[k][j];
					}
				}
			}
			transforms[f-1]=clone_multidim_array(globalTransform);
			//if(!transformFrame(imp, width, height,transformation, transforms[f-1], anchorPoints, f)) return;
			float[] trans2=get_translation(globalTransform,width,height);
			trans[0][f-1]=trans2[0]; trans[1][f-1]=trans2[1]; trans[2][f-1]=trans2[2];
		}

        System.out.println("The time it " + (System.currentTimeMillis() - t1));

		if(outtrans){
			new PlotWindow4("Translation Trajectory","x","y",trans[0],trans[1]).draw();
			new PlotWindow4("Angle Trajectory","frame","angle (radians)",trans[2]).draw();
		}
		//now output the transformation matrices (linearized) to a table
		String titles="frame\t(0,0)\t(0,1)\t(0,2)\t(1,0)\t(1,1)\t(1,2)\t(2,0)\t(2,1)\t(2,2)";
		TextWindow tw=new TextWindow("Global Transformations",titles,"",400,200);
		for(int i=0;i<frames;i++){
			String linear=table_tools.print_double_array(transforms[i][0])+"\t"+table_tools.print_double_array(transforms[i][1])+"\t"+table_tools.print_double_array(transforms[i][2]);
			tw.append(""+(i+1)+"\t"+linear);
		}
		//now transform the image
		for(int f=1;f<=frames;f++){
			if(!transformFrame(imp, width, height,transformation, transforms[f-1], anchorPoints, f)) return;
			IJ.showStatus("Frame "+f+" Aligned");
		}


		imp.updateAndDraw();

	}

	public float[] get_translation(double[][] globalTransform,int width,int height){
		//here we get the translation from the globalTransform
		//anchor points are at the center of the image
		double[][] anchorPoints = new double[1][3];
		anchorPoints[0][0] =0.5*(double)width;
		anchorPoints[0][1] =0.5*(double)height;
		anchorPoints[0][2] = 1.0;
		//source points will hold the transformation of the anchor points
		double[][] sourcePoints = new double[1][3];
		//matrix multiplication
		for (int i = 0; (i < 3); i++) {
			sourcePoints[0][i] = 0.0;
			for (int j = 0; (j < 3); j++) {
				sourcePoints[0][i] += globalTransform[i][j]* anchorPoints[0][j];
			}
		}
		//the translation is the difference between source and anchor points
		float[] trans={(float)(sourcePoints[0][0]-anchorPoints[0][0]),(float)(sourcePoints[0][1]-anchorPoints[0][1]),0.0f};
		//transform another set of anchor points to get the rotation
		double[][] anchorPoints2 = new double[1][3];
		anchorPoints2[0][0] =0.5*(double)width+1.0f;
		anchorPoints2[0][1] =0.5*(double)height;
		anchorPoints2[0][2] = 1.0;
		double[][] sourcePoints2=new double[1][3];
		//matrix multiplication
		for (int i = 0; (i < 3); i++) {
			sourcePoints2[0][i] = 0.0;
			for (int j = 0; (j < 3); j++) {
				sourcePoints2[0][i] += globalTransform[i][j]* anchorPoints2[0][j];
			}
		}
		//calculate the normalized vector between sourcepoints2 and sourcepoints
		float xvec=(float)(sourcePoints2[0][0]-sourcePoints[0][0]);
		float yvec=(float)(sourcePoints2[0][1]-sourcePoints[0][1]);
		float len=(float)Math.sqrt(xvec*xvec+yvec*yvec);
		float angle=(float)Math.atan2(yvec/len,xvec/len);
		trans[2]=angle;
		//return the difference between the source (transformed) and anchor points
		return trans;
	}



	/*------------------------------------------------------------------*/

	public void setHyperstackSlice(ImagePlus imp,int channel,int slice,int frame){
		//imp.setSlice((channel-1)+(slice-1)*channels+(frame-1)*slices*channels+1);
		imp.setPosition((channel-1)+(slice-1)*channels+(frame-1)*slices*channels+1);
		//int[] pos=imp.convertIndexToPosition((channel-1)+(slice-1)*channels+(frame-1)*slices*channels+1);
		//imp.setPositionWithoutUpdate(pos[0],pos[1],pos[2]);
	}



	public void setImpProcessor(final ImagePlus imp,ImagePlus source,int channel,int slice,int frame){
		source.getStack().deleteLastSlice();
		int index=(channel-1)+(slice-1)*channels+(frame-1)*slices*channels+1;
		switch(imp.getType()){
			case ImagePlus.GRAY8: {
				source.getProcessor().setMinAndMax(0.0,255.0);
				imp.getStack().setPixels(source.getProcessor().convertToByte(false).getPixels(),index);
				break;
			}
			case ImagePlus.GRAY16: {
				source.getProcessor().setMinAndMax(0.0,65535.0);
				imp.getStack().setPixels(source.getProcessor().convertToShort(false).getPixels(),index);
				break;
			}
			case ImagePlus.GRAY32: {
				imp.getStack().setPixels(source.getProcessor().getPixels(),index);
				break;
			}
			default: {
				IJ.error("Unexpected image type");
			}
		}
	}


	public boolean transformFrame(ImagePlus imp,int width,int height,int transformation,double[][] globalTransform,double[][] anchorPoints,int f){
		//this just uses the globalTransform to transform the indicated frame
		double[][] sourcePoints=null;
		TurboRegJ_ trj=null;
		switch (transformation) {
			case 0: {
				//transform a new set of source and anchor points
				sourcePoints = new double[1][3];
				for (int i = 0; (i < 3); i++) {
					sourcePoints[0][i] = 0.0;
					for (int j = 0; (j < 3); j++) {
						sourcePoints[0][i] += globalTransform[i][j]* anchorPoints[0][j];
					}
				}
				//and transform the entire hyperstack frame according to those points
				for(int i=1;i<=slices;i++){
					for(int j=1;j<=channels;j++){
						trj=RegUtils.gettrj();
						trj.setSourcePoints(new double[][]{{sourcePoints[0][0],sourcePoints[0][1]}});
						trj.setTargetPoints(new double[][]{{width/2,height/2}});
						ImagePlus source2=new ImagePlus("StackRegSource",RegUtils.getImpProcessor(imp,j,i,f));
						ImagePlus transformed=trj.transformImage(source2,width,height,TurboRegJ_.TRANSLATION);
						if(transformed==null) return false;
						setImpProcessor(imp,transformed,j,i,f);
					}
				}
				break;
			}
			case 1: {
				sourcePoints = new double[3][3];
				for (int i = 0; (i < 3); i++) {
					sourcePoints[0][i] = 0.0;
					sourcePoints[1][i] = 0.0;
					sourcePoints[2][i] = 0.0;
					for (int j = 0; (j < 3); j++) {
						sourcePoints[0][i] += globalTransform[i][j]* anchorPoints[0][j];
						sourcePoints[1][i] += globalTransform[i][j]* anchorPoints[1][j];
						sourcePoints[2][i] += globalTransform[i][j]* anchorPoints[2][j];
					}
				}
				
				for(int i=1;i<=slices;i++){
					for(int j=1;j<=channels;j++){
						trj=RegUtils.gettrj();
						trj.setSourcePoints(new double[][]{{sourcePoints[0][0],sourcePoints[0][1]},{sourcePoints[1][0],sourcePoints[1][1]},{sourcePoints[2][0],sourcePoints[2][1]}});
						trj.setTargetPoints(new double[][]{{width/2,height/2},{width/2,height/4},{width/2,3*height/4}});
						ImagePlus source2=new ImagePlus("StackRegSource",RegUtils.getImpProcessor(imp,j,i,f));
						ImagePlus transformed=trj.transformImage(source2,width,height,TurboRegJ_.RIGID_BODY);
						if(transformed==null) return false;
						setImpProcessor(imp,transformed,j,i,f);
					}
				}
				break;
			}
			case 2: {
				sourcePoints = new double[2][3];
				for (int i = 0; (i < 3); i++) {
					sourcePoints[0][i] = 0.0;
					sourcePoints[1][i] = 0.0;
					for (int j = 0; (j < 3); j++) {
						sourcePoints[0][i] += globalTransform[i][j]* anchorPoints[0][j];
						sourcePoints[1][i] += globalTransform[i][j]* anchorPoints[1][j];
					}
				}
				for(int i=1;i<=slices;i++){
					for(int j=1;j<=channels;j++){
						trj=RegUtils.gettrj();
						trj.setSourcePoints(new double[][]{{sourcePoints[0][0],sourcePoints[0][1]},{sourcePoints[1][0],sourcePoints[1][1]}});
						trj.setTargetPoints(new double[][]{{width/4,height/2},{3*width/4,height/2}});
						ImagePlus source2=new ImagePlus("StackRegSource",RegUtils.getImpProcessor(imp,j,i,f));
						ImagePlus transformed=trj.transformImage(source2,width,height,TurboRegJ_.SCALED_ROTATION);
						if(transformed==null) return false;
						setImpProcessor(imp,transformed,j,i,f);
					}
				}
				break;
			}
			case 3: {
				sourcePoints = new double[3][3];
				for (int i = 0; (i < 3); i++) {
					sourcePoints[0][i] = 0.0;
					sourcePoints[1][i] = 0.0;
					sourcePoints[2][i] = 0.0;
					for (int j = 0; (j < 3); j++) {
						sourcePoints[0][i] += globalTransform[i][j]* anchorPoints[0][j];
						sourcePoints[1][i] += globalTransform[i][j]* anchorPoints[1][j];
						sourcePoints[2][i] += globalTransform[i][j]* anchorPoints[2][j];
					}
				}
				for(int i=1;i<=slices;i++){
					for(int j=1;j<=channels;j++){
						trj=RegUtils.gettrj();
						trj.setSourcePoints(new double[][]{{sourcePoints[0][0],sourcePoints[0][1]},{sourcePoints[1][0],sourcePoints[1][1]},{sourcePoints[2][0],sourcePoints[2][1]}});
						trj.setTargetPoints(new double[][]{{width/2,height/4},{width/4,3*height/4},{3*width/4,3*height/4}});
						ImagePlus source2=new ImagePlus("StackRegSource",RegUtils.getImpProcessor(imp,j,i,f));
						ImagePlus transformed=trj.transformImage(source2,width,height,TurboRegJ_.AFFINE);
						if(transformed==null) return false;
						setImpProcessor(imp,transformed,j,i,f);
					}
				}
				break;
			}
		}
		return true;
	}




    public double[][] clone_multidim_array(double[][] arr){
        double[][] temp=new double[arr.length][];
        for(int i=0;i<arr.length;i++){
            temp[i]=arr[i].clone();
        }
        return temp;
    }



    @Override
    public void preview() {

    }

    @Override
    public void cancel() {

    }
}
