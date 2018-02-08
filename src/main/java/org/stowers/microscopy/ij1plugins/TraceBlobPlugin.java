package org.stowers.microscopy.ij1plugins;

/**
 * Created by cjw on 10/26/16.
 */


import ij.ImageListener;

import ij.IJ;
import ij.WindowManager;
import ij.gui.*;
import ij.measure.ResultsTable;
import ij.text.TextPanel;
import ij.text.TextWindow;
import net.imagej.ImageJ;
import net.imagej.patcher.LegacyInjector;
import org.jfree.chart.*;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.data.general.DatasetChangeEvent;
import org.jfree.data.general.DatasetChangeListener;
import org.jfree.data.general.DatasetUtils;
//import org.jfree.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleEdge;
import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import ij.ImagePlus;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;

import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.data.xy.DefaultXYDataset;
import static ij.WindowManager.getNonImageTitles;

import org.stowers.microscopy.ij1plugins.tableutils.CellTrajectory;
import org.stowers.microscopy.ij1plugins.tableutils.MotherIdTable;
import org.stowers.microscopy.ij1plugins.tableutils.ResultsUtils;

@Plugin(type = Command.class, name = "Lineage Explorer",  menuPath="Plugins>Stowers>Chris>LineageExplorer")
public class TraceBlobPlugin implements Previewable, Command,
                                        ActionListener, MouseListener, ij.ImageListener,
                                        DatasetChangeListener, ChartMouseListener,
                                        ChangeListener {


    static {
        LegacyInjector.preinit();
    }

    @Parameter
    ImagePlus imp = null;

//    @Parameter
//    ResultsTable resultsTable;

    ImageWindow window;
    ImageCanvas canvas;
    WindowManager wm;
    long mouseDownTime;

    PointRoi roi = null;

    JFrame frame;
    JPanel toppanel;
    JPanel mainPane;
    JPanel plotPane;
    JLabel label;
    JComboBox<String> nonImage;
    JButton ok;
    JButton reset;

    ChartPanel chartpanel;
    JFreeChart chart;

    JSlider slider;

    String[] frameTitles;
    String[] imageTitles;

    String pickedTitle;
    TextWindow resultsFrame = null;

    double[][] resultsData;
    double[][] framemark;
    double[][] currentmark;
    int currentId = -1;

    DefaultXYDataset dataset;

    MotherIdTable mt;

    @Override
    public void run() {
        window = imp.getWindow();
        imp.addImageListener(this);
        canvas = window.getCanvas();
        canvas.addMouseListener(this);
//        String t = PickNonImagePlugin.getNonImageFrameTitle();

        frameTitles = WindowManager.getNonImageTitles();
        createDialog();
//        WindowManager.getImageTitles();
    }


    private void createDialog() {
        frame = new JFrame("Select Windows");
        mainPane = new JPanel(new BorderLayout());
        toppanel = new JPanel();
        label = new JLabel("Pick a Window:");
        nonImage = new JComboBox<>();
        ok = new JButton("OK");
        ok.addActionListener(this);
        JPanel lower = new JPanel();
        lower.setPreferredSize(new Dimension(120, 300));
        JPanel west = new JPanel();


        reset = new JButton("Reset");
        reset.addActionListener(this);

        for (int i = 0; i < frameTitles.length; i++) {
            nonImage.addItem(frameTitles[i]);
        }

//        frame.setLayout(new BorderLayout());
        toppanel.setLayout(new GridLayout(4,1));
        toppanel.setPreferredSize(new Dimension(120,300));

        toppanel.add(label);
        toppanel.add(nonImage);
        toppanel.add(ok);
        toppanel.add(reset);

        west.add(toppanel);
        west.add(lower);
        mainPane.add(west, BorderLayout.WEST);
        plotPane = new JPanel();
        plotPane.setLayout(new BoxLayout(plotPane, BoxLayout.Y_AXIS));
        makeplot();
        slider = new JSlider(JSlider.HORIZONTAL, 1, imp.getNFrames(), imp.getCurrentSlice());
        slider.addChangeListener(this);
        plotPane.add(slider);
        mainPane.add(plotPane, BorderLayout.CENTER);
//        frame.add(mainPane);
        frame.setContentPane(mainPane);
        frame.setSize(1000, 600);
        frame.pack();
        frame.setVisible(true);
    }

    private void makeplot() {

        int nf = imp.getNFrames();
        double[][] temp = new double[2][nf];
        for (int i = 0; i < nf; i++) {
            temp[0][i] = i;
            temp[1][i] = 0.;
        }
        dataset = new DefaultXYDataset();
        dataset.addChangeListener(this);
        dataset.addSeries("Data", temp);

        int currentFrame = imp.getCurrentSlice();

        framemark = new double[2][2];
        framemark[0][0] = currentFrame;
        framemark[0][1] = currentFrame;
        framemark[1][0] = 0;
        framemark[1][1] = .2*imp.getProcessor().getStatistics().max;
        dataset.addSeries("mark", framemark);

//        currentmark = new double[2][2];
//        currentmark[0][0] = currentFrame;
//        currentmark[0][1] = currentFrame;
//        currentmark[1][0] = 0;
//        currentmark[1][1] = .2*imp.getProcessor().getStatistics().median;
//        dataset.addSeries("currentmark", currentmark);

        chart = ChartFactory.createXYLineChart("Intensity vs Time", "Time", "Intensity", dataset);
        XYPlot plot = (XYPlot)chart.getXYPlot();
        plot.getRenderer().setSeriesPaint(0, Color.darkGray);
        plot.getRenderer().setSeriesPaint(1, Color.darkGray);
        plot.getRenderer().setSeriesPaint(2, Color.darkGray);
        plot.getRenderer().setSeriesVisibleInLegend(0, false);
        plot.getRenderer().setSeriesVisibleInLegend(1, false);
        plot.getRenderer().setSeriesVisibleInLegend(2, false);

        chart.getXYPlot().setDomainCrosshairVisible(true);

//        chart.getXYPlot().setRangeCrosshairVisible(true);
        chartpanel = new ChartPanel(chart);
        chartpanel.addChartMouseListener(this);

        chartpanel.setVisible(true);
        plotPane.add(chartpanel, BorderLayout.CENTER);

    }

    private void resetPlot() {
        dataset = null;
        chartpanel.setVisible(false);
        mainPane.remove(chartpanel);
        makeplot();
    }
    //This is the action for pressing the ok button on the JFrame
    //It should load the data from the selected table into memory
    @Override
    public void actionPerformed(ActionEvent e) {

        System.out.println("-->" + e.getSource().toString());
        if (e.getSource() == ok) {
            String title = (String) nonImage.getSelectedItem();
            pickedTitle = title;
            setResults(title);
        }

        if (e.getSource() == reset) {
            System.out.println("!!!!! Resetting !!!!");
            resetPlot();
        }
    }
    @Override
    public void stateChanged(ChangeEvent e) {
        if (e.getSource() == slider) {
//            imp.setSlice(imp.getCurrentSlice() + 1);
//            System.out.println(slider.getValue());
            imp.setSlice(slider.getValue());
        }
    }

    public void setResults(String title) {

        resultsFrame = (TextWindow)WindowManager.getWindow(title);
        TextPanel panel = resultsFrame.getTextPanel();
        String[] headings = ResultsUtils.getHeaders(panel);

        for (int i = 0; i < headings.length; i++) {
            System.out.println(headings[i]);
        }

        mt = new MotherIdTable(panel);


    }


    public String getPickedTitle() {
        return pickedTitle;
    }

    @Override
    public void mouseClicked(MouseEvent e) {

        System.out.println("Yes");
        long timeDiff = System.currentTimeMillis() - mouseDownTime;

        if (timeDiff < 500) {
            System.out.println("Hey a double click");
            handleDoubleClick(e);
        }
        mouseDownTime = System.currentTimeMillis();
    }

    protected void handleDoubleClick(MouseEvent e) {
        double mx = canvas.getCursorLoc().getX();
        double my = canvas.getCursorLoc().getY();

        int f =imp.getCurrentSlice() - 1;
        System.out.println("######" + mx + " " + my + " " + f);
        CellTrajectory ct = mt.getTrajFromPoint((int)mx, (int)my, f);

        if (ct.getId() < 0) {
            System.out.println("No cell within 5 px");
            return;
        }

        java.util.List<Float> gz = ct.getIntensity();
        java.util.List<Integer> fz = ct.getFrames();

        System.out.println("######    " + mx + " " + my + " " + f);

        double[][] plotdata = new double[2][gz.size()];
        for (int i = 0; i < gz.size(); i++)  {
            float g = gz.get(i);
            int ft = fz.get(i);
            plotdata[0][i] = ft;
            plotdata[1][i] = g;
//            System.out.println(ft + " " + g);
        }

        dataset.addSeries(Integer.toString(ct.getId()), plotdata);
        int currentFrame = imp.getCurrentSlice();

        framemark[0][0] = currentFrame;
        framemark[0][1] = currentFrame;
        currentId = ct.getId();
        makeRoi(ct.getId(), currentFrame - 1);

    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }



    @Override
    public void preview() {

    }

    @Override
    public void cancel() {

    }

    @Override
    public void imageOpened(ImagePlus ximp) {

    }

    @Override
    public void imageClosed(ImagePlus ximp) {

    }

    @Override
    public void imageUpdated(ImagePlus ximp) {
        System.out.println("Slice: " + ximp.getCurrentSlice());
        int currentFrame = ximp.getCurrentSlice();
//        currentmark[0][0] = currentFrame;
//        currentmark[0][1] = currentFrame;
//        currentmark[1][0] = 0;
//        currentmark[1][1] = .25*imp.getProcessor().getStatistics().max;
//        dataset.removeSeries("currentmark");
//        dataset.addSeries("currentmark", currentmark);
        if((currentId >= 0) && roi != null) {
            int[] xy = cellPositionFromClick(currentId, currentFrame - 1);
            roi.setLocation(xy[0], xy[1]);
        }
        imp.setRoi((Roi)roi);
        chart.getXYPlot().setDomainCrosshairValue(currentFrame);
        System.out.println("Image Updated");
    }

    @Override
    public void datasetChanged(DatasetChangeEvent datasetChangeEvent) {

    }

    @Override
    public void chartMouseMoved(ChartMouseEvent chartMouseEvent) {


    }

    @Override
    public void chartMouseClicked(ChartMouseEvent chartMouseEvent) {

        Rectangle2D dataArea = chartpanel.getScreenDataArea();

        int mouseX = chartMouseEvent.getTrigger().getX();
        int mouseY = chartMouseEvent.getTrigger().getY();
        XYPlot xyplot = chartMouseEvent.getChart().getXYPlot();
        ValueAxis xAxis = xyplot.getDomainAxis();
        ValueAxis yAxis = xyplot.getRangeAxis();
        double x = xAxis.java2DToValue(mouseX, dataArea, RectangleEdge.BOTTOM);
        double y = yAxis.java2DToValue(mouseY, dataArea, RectangleEdge.LEFT);
        int ns = dataset.getSeriesCount();

        double cy = 0;
        int mi = 0;
        for (int i = 0; i < ns; i++) {
            double yy = DatasetUtils.findYValue(dataset, i, x);
            System.out.println(i + " " + x + " " + yy);
            if (Math.abs(yy - y) < Math.abs(cy - y)) {
                cy = yy;
                mi = i;
            }
        }

        int id;
        String sid = (String)dataset.getSeriesKey(mi);

        if (ResultsUtils.isNumeric(sid)) {
            System.out.println(dataset.getSeriesKey(mi) + "  " + mi + " " + x);
            id = Integer.parseInt(sid);
            currentId = id;
//            id = mi;

            imp.setSlice((int)x);
            makeRoi(id, (int)x);


        }
    }

    protected void makeRoi(int id, int frame) {
        int[] roixy = cellPositionFromClick(id, frame);

        int ox = roixy[0];
        int oy = roixy[1];
        roi = new PointRoi(ox, oy);
        roi.setSize(10);
        imp.setRoi((Roi) roi);
        //        imp.createNewRoi(ox, oy);
        imp.repaintWindow();
    }
    protected int[] cellPositionFromClick(int id, int frame) {

        CellTrajectory ct = mt.getTrajFromId(id);

        int x = ct.getX(frame);
        int y = ct.getY(frame);

        int[] xy = new int[] {x, y};
        return xy;
    }


    public static void main(String args[]) {

        final ImageJ imagej = net.imagej.Main.launch(args);
        String xls = "/Volumes/projects/jru/public/Baumann/10102016 baumann TL 4/Analysis/X1/MotherIdTable.xls";
        String image = "/Volumes/projects/jru/public/Baumann/10102016 baumann TL 4/Analysis/Colony1-RBS-Binned-C1-SUM_ConcatenatedStacks-1.tif";
//        ij.io.Opener opener = new ij.io.Opener();
//        opener.open(xls);
//        opener.open(image);
//
//        imagej.command().run(TraceBlobPlugin.class, true);

    }



}
