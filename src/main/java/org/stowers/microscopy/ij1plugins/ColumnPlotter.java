package org.stowers.microscopy.ij1plugins;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;


import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.*;


import org.stowers.microscopy.utils.Histogram;

import ij.measure.ResultsTable;
import org.stowers.microscopy.utils.HistogramPlot;

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
        makeHistogram(headers[1]);
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


    protected void makeHistogram(String iheader) {

        if ( chartpanel != null) {
            chartpanel.setVisible(false);
            mainPane.remove(chartpanel);
            System.out.println("Cleanup");
        } else {
            System.out.println("Null");
        }

        int colIndex = table.getColumnIndex(iheader);
        DefaultXYDataset dataset = new DefaultXYDataset();
        double[] data = table.getColumnAsDoubles(colIndex);

        if (data == null) {
            return;
        }
        Histogram h = new Histogram(data, 20);
//        double[] hd = h.doHist();

        double[][] s = h.getHistogramArray(false);

        HistogramPlot p = new HistogramPlot(h);

        String xlabel = iheader; //headers[colIndex + nlabel];
        chartpanel = p.makeChart("Histogram", xlabel, "Counts");

        mainPane.add(chartpanel);
        chartpanel.setVisible(true);

    }


    @Override
    public void actionPerformed(ActionEvent e) {
        JComboBox<String> cb = (JComboBox<String>)e.getSource();
        System.out.println(cb.getSelectedIndex() + "  " + cb.getSelectedItem());
        int index = cb.getSelectedIndex() + 0;
        String iheader = (String)cb.getSelectedItem();
        if (index >= nlabel) {
            makeHistogram(iheader);
        }


    }


}
