package org.stowers.microscopy.ij1plugins;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

//import de.erichseifert.gral.plots.XYPlot;
//import de.erichseifert.gral.ui.DrawablePanel;
//import de.erichseifert.gral.data.DataSeries;
//import de.erichseifert.gral.data.DataTable;
//import de.erichseifert.gral.ui.InteractivePanel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.*;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

import ij.measure.ResultsTable;

/**
 * Created by cjw on 7/7/16.
 */
public class ColumnPlotter implements ActionListener {


//    protected ApplicationFrame frame;
    protected JFrame frame;
    protected ChartPanel chartpanel;
    protected JPanel panel;
    protected JPanel mainPane;
    protected JLabel banner;
    protected JFreeChart chart;
    protected JComboBox<String> colList;
    private int nlabel;
//    protected InteractivePanel dp;
    String[] headers;
    ResultsTable table;

    public ColumnPlotter(ResultsTable table) {


        this.table = table;
        headers = table.getHeadings();
        mainPane = new JPanel(new BorderLayout());
        String label = table.getLabel(0);
        if (label == null) {
            System.out.println("No label");
            nlabel = 0;
        }
        else {
            nlabel = 1;
        }
        JPanel panel = new JPanel();
        colList = new JComboBox<>(headers);
        colList.addActionListener(this);

        panel.add(colList);
        JButton pltButton = new JButton();
        panel.add(pltButton);
//        makePlot(3);
//        chartpanel = new ChartPanel(chart);

        panel.setSize(120, 50);
        makeHistogram(3);
        mainPane.add(panel, BorderLayout.WEST);
        //mainPane.add(dp);
        mainPane.setBorder(BorderFactory.createEmptyBorder(25,25,25,25));
        frame = new JFrame("Plots");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setContentPane(mainPane);
        frame.setSize(800,600);
        frame.pack();
        frame.setVisible(true);

    }

    protected void makeHistogram(int colIndex) {

        if ( chartpanel != null) {
            chartpanel.setVisible(false);
            mainPane.remove(chartpanel);
            System.out.println("Cleanup");
        } else {
            System.out.println("Null");
        }

        DefaultXYDataset dataset = new DefaultXYDataset();
        double[] data = table.getColumnAsDoubles(colIndex);
        Histogram h = new Histogram(data, 20);
        int[] hd = h.doHist();

        double[][] s = new double[2][hd.length];
        for (int i = 0; i < hd.length; i++) {
            double hx = h.binMean(i);
            s[0][i] = hx;
            s[1][i] = hd[i];
//            series.add(new XYDataItem(hx, (double)hd[i]));

        }

        double barWidth = 0.8*(h.getEndX() - h.getStartX())/h.getNBins();
        dataset.addSeries("Hist", s);
        XYBarDataset xybar = new XYBarDataset(dataset, barWidth);
        String xlabel = headers[colIndex + nlabel];
        chart = ChartFactory.createXYBarChart("Hist", xlabel, false, "Counts",  xybar);

        XYPlot plot = (XYPlot) chart.getPlot();

        //CategoryAxis axis =  plot.getDomainAxis();
        //axis.setTickMarkOutsideLength(3.0f);

        XYBarRenderer renderer = (XYBarRenderer) plot.getRenderer();
        renderer.setBarPainter(new StandardXYBarPainter());
        renderer.setSeriesPaint(0, Color.orange);
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.lightGray);
        plot.setRangeGridlinePaint(Color.lightGray);
        plot.setOutlineVisible(false);
        plot.setDomainAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
        plot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);

        chartpanel = new ChartPanel(chart);
        mainPane.add(chartpanel);
        chartpanel.setVisible(true);

    }

    protected void makePlot(int colIndex) {

        if ( chartpanel != null) {
            chartpanel.setVisible(false);
            mainPane.remove(chartpanel);
            System.out.println("Cleanup");
        } else {
            System.out.println("Null");
        }
        XYSeries dataset = new XYSeries(headers[colIndex + 1]);
        XYSeriesCollection c = new XYSeriesCollection();
        double[] data = table.getColumnAsDoubles(colIndex);
        Histogram h = new Histogram(data, 10);
        int[] hd = h.doHist();
        for (int i = 0; i < hd.length; i++) {
            dataset.add(i, hd[i]);
        }

        c.addSeries(dataset);



        chart = ChartFactory.createScatterPlot("Title", "x" + (colIndex + 1),
                headers[colIndex + 1], c);

        chartpanel = new ChartPanel(chart);
        mainPane.add(chartpanel);
        chartpanel.setVisible(true);

//        if (dp != null) {
//            dp.setVisible(false);
//            mainPane.remove(dp);
//        }
//        dp = plotPanel(colIndex);
//        System.out.println("--" + table.getColumnHeading(colIndex));
//        mainPane.add(dp, BorderLayout.CENTER);
//        dp.setVisible(true);

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JComboBox<String> cb = (JComboBox<String>)e.getSource();
        System.out.println(cb.getSelectedIndex() + "  " + cb.getSelectedItem());
        int index = cb.getSelectedIndex() + 0;
        if (index >= nlabel) {
            makeHistogram(index - nlabel);
        }
    }


}
