package org.stowers.microscopy.ij1plugins;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import de.erichseifert.gral.plots.XYPlot;
import de.erichseifert.gral.ui.DrawablePanel;
import de.erichseifert.gral.data.DataSeries;
import de.erichseifert.gral.data.DataTable;
import de.erichseifert.gral.ui.InteractivePanel;
import ij.measure.ResultsTable;

/**
 * Created by cjw on 7/7/16.
 */
public class ColumnPlotter implements ActionListener {


    protected JFrame frame;
    protected JPanel panel;
    protected JPanel mainPane;
    protected JLabel banner;
    protected JComboBox<String> colList;
    protected InteractivePanel dp;
    String[] headers;
    ResultsTable table;

    public ColumnPlotter(ResultsTable table) {

        this.table = table;
        headers = table.getHeadings();
        mainPane = new JPanel(new BorderLayout());
        panel = new JPanel();
        colList = new JComboBox<>(headers);
        colList.addActionListener(this);

        panel.add(colList);
        panel.setSize(120, 50);
        makePlot(3);
        mainPane.add(panel, BorderLayout.WEST);
        //mainPane.add(dp);
        mainPane.setBorder(BorderFactory.createEmptyBorder(25,25,25,25));

        frame = new JFrame();
        frame.setContentPane(mainPane);
        frame.setSize(800,600);
        frame.pack();
        frame.setVisible(true);

    }

    protected void makePlot(int colIndex) {

        if (dp != null) {
            dp.setVisible(false);
            mainPane.remove(dp);
        }
        dp = plotPanel(colIndex);
        System.out.println("--" + table.getColumnHeading(colIndex));
        mainPane.add(dp, BorderLayout.CENTER);
        dp.setVisible(true);

    }

    protected InteractivePanel plotPanel(int colIndex) {

        double data[] = table.getColumnAsDoubles(colIndex);
        String title = table.getColumnHeading(colIndex);
        DataTable dtable = new DataTable(Double.class, Double.class);

        for (int i = 0; i < data.length; i++) {
            dtable.add((double)i, data[i]);
        }

        XYPlot plot = new XYPlot(dtable);
        plot.getTitle().setText(title);
        InteractivePanel dpanel = new InteractivePanel(plot);
        dpanel.setSize(300,200);
        dpanel.setVisible(true);
        return dpanel;

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JComboBox<String> cb = (JComboBox<String>)e.getSource();
        System.out.println(cb.getSelectedIndex() + "  " + cb.getSelectedItem());
        int index = cb.getSelectedIndex() + 0;
        if (index > 0) {
            makePlot(index - 1);
        }
    }


}
