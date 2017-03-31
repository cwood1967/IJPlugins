package org.stowers.microscopy.ij1plugins;

/**
 * Created by cjw on 3/13/17.
 */
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.ArrayList;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

import ij.*;
import ij.gui.MessageDialog;
import ij.gui.Roi;
import ij.io.FileInfo;
import ij.measure.ResultsTable;
import ij.process.ImageProcessor;
import net.imagej.*;
import org.jfree.ui.tabbedui.VerticalLayout;
import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, name = "Click for Roi",  menuPath="Plugins>Stowers>Chris>Pixels To Text")
public class PixelsToText implements Command, Previewable, ActionListener, TableModelListener {

    @Parameter
    ImagePlus imp;

//    @Parameter(label="Put in results table?", callback = "resultsChanged")
//    boolean inResults;
//
//    @Parameter(required=false, label="Select Output Directory", style="directory")
//    File outputDir;

    String strDir;

    Vector<Float> allPixels;
    Vector<String>  colNames;
    Vector<Vector<Float>> vectPix;
    float[] pixels;
    Path outputFile = null;

    JTable jtable = null;
    JFrame frame;
    JPanel mainPane;
    JButton runButton;
    JScrollPane scrollPane;

    DefaultTableModel tableModel;
    @Override
    public void run() {

        allPixels = new Vector<Float>();
        vectPix = new Vector();
        vectPix.add(allPixels);
        colNames = new Vector();
        String name = imp.getTitle();

        pixels = (float[])(imp.getProcessor().convertToFloatProcessor().getPixels());
        colNames.add("Value");
//        allPixels.add(45.67f);
        tableModel = new DefaultTableModel(vectPix, colNames);
        createDialog();
    }

    private void addPixelsToTable() {
        float[] outPixels;

        int n1 = tableModel.getRowCount();
        if (n1 == 1) {
            tableModel.removeRow(0);
        }
        if (imp.getRoi() != null) {
            Point[] points = imp.getRoi().getContainedPoints();
            outPixels = new float[points.length];
            int i = 0;
            for (Point p : points) {
                int x = (int) p.getX();
                int y = (int) p.getY();
                int index = imp.getWidth() * y + x;
//                allPixels.add(pixels[index]);
                Vector<Float> f = new Vector<>();
                f.add(pixels[index]);
                tableModel.addRow(f);

                outPixels[i] = pixels[index];
                i++;
            }
        } else {
            outPixels = pixels;
        }
        int n3 = tableModel.getRowCount();
        int n2 = allPixels.size();
        System.out.println(n3 + " " + n2);

//        tableModel.fireTableRowsInserted(n1, n2-1);

    }


    private void createDialog() {

        tableModel.addTableModelListener(this);
        frame = new JFrame("Pixels to Text");
        mainPane = new JPanel(new VerticalLayout());
        runButton = new JButton("Run");
        runButton.addActionListener(this);
        mainPane.add(runButton);
        mainPane.setSize(100,1000);

        jtable = new JTable();

        jtable.setModel(tableModel);

        jtable.setVisible(true);
        jtable.setPreferredScrollableViewportSize(new Dimension(80,800));
        jtable.setFillsViewportHeight(true);

        scrollPane = new JScrollPane(jtable);
        mainPane.add(scrollPane);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
//        frame.add(scrollPane);
        frame.setContentPane(mainPane);
        frame.setSize(120,1000);
//        frame.pack();
        frame.setVisible(true);

    }
//    protected void resultsChanged() {
//
//        if (inResults == true) {
//            //strDir = outputDir.getAbsolutePath();
//            outputDir = null;
//        }
//        else {
//            outputDir = new File(strDir);
//        }
//    }

    private void writeResults(float[] outPixels) {

        ResultsTable table = new ResultsTable();
        
        for (int i = 0; i < outPixels.length; i++) {
            table.incrementCounter();
            float pix = outPixels[i];
            String pout = String.format("%10.2f\n" , pix);
            table.addValue("Value", pix);
        }
        table.show("Pixel Values");
    }

    private void writePixels(Path outputFile, float[] outPixels) {

        Charset charset = Charset.forName("US-ASCII");
        try {
            BufferedWriter writer = Files.newBufferedWriter(outputFile, charset);

            String pout;
            for (int i = 0; i < outPixels.length; i++) {
                float pix = outPixels[i];
                pout = String.format("%10.2f\n" , pix);
                writer.write(pout);
            }

            writer.close();
        }
        catch (IOException e) {
            IJ.log("Can't create the output file");
        }
    }
    @Override
    public void preview() {

    }

    @Override
    public void cancel() {

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("Adding");
        addPixelsToTable();
        jtable.repaint();

    }

    @Override
    public void tableChanged(TableModelEvent e) {
//        System.out.println("Table changed");
//        System.out.println(allPixels.size());

//        jtable.invalidate();
        //allPixels.set(0, allPixels.get(allPixels.size() -1));

//        jtable.validate();
//        jtable.updateUI();
//        jtable.repaint();
//        System.out.println(jtable.getSize());
//        runButton.setText(Float.toString(allPixels.get(allPixels.size() - 1)));
    }
}

