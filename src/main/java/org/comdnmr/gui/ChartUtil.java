/*
 * CoMD/NMR Software : A Program for Analyzing NMR Dynamics Data
 * Copyright (C) 2018-2019 Bruce A Johnson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.comdnmr.gui;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Alert;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Transform;
import org.comdnmr.eqnfit.CurveFit;
import org.comdnmr.data.DataIO;
import org.comdnmr.data.ExperimentData;
import org.comdnmr.eqnfit.PlotEquation;
import org.comdnmr.data.ResidueInfo;
import org.comdnmr.data.ResidueProperties;
import org.comdnmr.data.ResidueData;
import org.nmrfx.chart.DataSeries;
import org.nmrfx.chart.XYEValue;
import org.nmrfx.chart.XYValue;

public class ChartUtil {

    public static int minRes = 0;
    public static int maxRes = 100;
    public static Map<String, ResidueProperties> residueProperties = new HashMap<>();

    public static Node findNode(Scene scene, String target) {
        Parent parent = scene.getRoot();
        return checkChildren(target, parent);

    }

    static Node checkChildren(String target, Parent parent) {
        ObservableList<Node> children = parent.getChildrenUnmodifiable();
        for (Node child : children) {
            if ((child.getId() != null) && child.getId().equals(target)) {
                return child;
            } else if (child instanceof Parent) {
                Node test = checkChildren(target, (Parent) child);
                if (test != null) {
                    return test;
                }
            }
        }
        return null;
    }

    public static int getNMaps() {
        return residueProperties.size();
    }

    public static ObservableList<XYChart.Series<Double, Double>> makeChartSeries(double[] xValues, double[] yValues) {
        ObservableList<XYChart.Series<Double, Double>> data = FXCollections.observableArrayList();
        XYChart.Series<Double, Double> series = new Series<>();
        for (int i = 0; i < xValues.length; i++) {
            XYChart.Data dataPoint = new XYChart.Data(xValues[i], yValues[i]);
            series.getData().add(dataPoint);
        }
        data.add(series);
        return data;
    }

    public static ObservableList<XYChart.Series<Integer, Double>> makeChartSeries(int[] xValues, double[] yValues) {
        ObservableList<XYChart.Series<Integer, Double>> data = FXCollections.observableArrayList();
        XYChart.Series<Integer, Double> series = new Series<>();
        for (int i = 0; i < xValues.length; i++) {
            XYChart.Data dataPoint = new XYChart.Data(xValues[i], yValues[i]);
            series.getData().add(dataPoint);
        }
        series.setNode(new Sphere(50.0));
        data.add(series);
        return data;
    }

    public static ArrayList<SecondaryStructure> loadSS(String fileName) throws IOException {
        Path path = Paths.get("", fileName);
        ArrayList<SecondaryStructure> secondaryStructures = new ArrayList<>();
        try (Stream<String> lines = Files.lines(path)) {
            lines.forEach(line -> addSS(secondaryStructures, line));
        }
        return secondaryStructures;
    }

    private static void addSS(ArrayList<SecondaryStructure> ss, String line) {
        String[] fields = line.split(" ");
        Integer start = Integer.parseInt(fields[0]);
        Integer end = Integer.parseInt(fields[1]);
        String type = fields[2];
        String label = "";
        if (fields.length > 3) {
            label = fields[3];

        }
        int mode = 0;
        if (type.equals("h")) {
            mode = 0;
        } else if (type.equals("s")) {
            mode = 1;
        }
        ss.add(new SecondaryStructure(start, end, mode, label));
    }

    public static ObservableList<XYChart.Series<Integer, Double>> loadChartData(String fileName) throws IOException {
        ObservableList<XYChart.Series<Integer, Double>> data = FXCollections.observableArrayList();
        Series<Integer, Double> series1 = new Series<>();
        Path path = Paths.get("", fileName);
        series1.setName(path.getFileName().toString());
        try (Stream<String> lines = Files.lines(path)) {
            lines.forEach(line -> doLine(series1, line, 1.0));
        }
        data.addAll(series1);
        return data;
    }

    static void doLine(Series<Integer, Double> series, String line, double scale) {
        String[] fields = line.split(" ");
        Integer x = Integer.parseInt(fields[0]);
        Double y = Double.parseDouble(fields[1]) * scale;
        ErrorExtraValues extra = new ErrorExtraValues(25, -0.2, 0.2);
        XYChart.Data xyData = new XYChart.Data(x, y, extra);
//        xyData.setNode(new Sphere(50.0));
        series.getData().add(xyData);
    }

    public static void print(Node node) {
        Set<Printer> printers = Printer.getAllPrinters();
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null) {
            Node oldClip = node.getClip();
            List<Transform> oldTransforms = new ArrayList<>(node.getTransforms());

            Printer printer = job.getPrinter();

            PageLayout pageLayout = printer.createPageLayout(Paper.NA_LETTER, PageOrientation.LANDSCAPE, Printer.MarginType.DEFAULT);
            job.showPageSetupDialog(node.getScene().getWindow());
            double scaleX = pageLayout.getPrintableWidth() / node.getBoundsInParent().getWidth();
            double scaleY = pageLayout.getPrintableHeight() / node.getBoundsInParent().getHeight();
            //node.getTransforms().add(new Scale(scaleX, scaleY));

            boolean doPrint = job.showPrintDialog(node.getScene().getWindow());
            if (doPrint) {
                boolean success = job.printPage(node);
                if (success) {
                    job.endJob();
                }
            }
            node.getTransforms().clear();
            node.getTransforms().addAll(oldTransforms);
            node.setClip(oldClip);
        }
    }

    public static ObservableList<XYChart.Series<Double, Double>> loadCPMGData(String fileName, String[] residues) throws IOException {
        ObservableList<XYChart.Series<Double, Double>> data = FXCollections.observableArrayList();
        Path path = Paths.get(fileName);
        Series<Double, Double> series = null;
        boolean inData = false;
        double[] xValues = null;
        double[] yValues = null;
        try (BufferedReader fileReader = Files.newBufferedReader(path)) {
            String line = fileReader.readLine();
            double field = Double.parseDouble(line.trim());
            line = fileReader.readLine();
            double time = Double.parseDouble(line.trim());
            while (true) {
                line = fileReader.readLine();
                if (line == null) {
                    break;
                }
                String sline = line.trim();
                String[] sfields = sline.split("\t");
                if (line.startsWith("#")) {
                } else if (line.startsWith("vCPMG")) {
                    xValues = new double[sfields.length - 2];
                    for (int i = 2; i < sfields.length; i++) {
                        xValues[i - 2] = Double.parseDouble(sfields[i]);
                    }
                } else if (Arrays.stream(residues).anyMatch(e -> e.equals(sfields[0]))) {
                    series = new Series<>();
                    data.add(series);

                    series.setName(sfields[0]);
                    double ref = Double.parseDouble(sfields[1]);
                    yValues = new double[sfields.length - 2];
                    for (int i = 2; i < sfields.length; i++) {
                        yValues[i - 2] = -Math.log(Double.parseDouble(sfields[i]) / ref) / time;
                        //-math.log(v/ref)/tCPMG
                    }
                    for (int i = 0; i < xValues.length; i++) {
                        if (series != null) {
                            double x = xValues[i];
                            double y = yValues[i];
                            XYChart.Data dataPoint = new XYChart.Data(x, y);
                            series.getData().add(dataPoint);
                        }

                    }
                }
            }
        }
        return data;
    }

    public static List<DataSeries> getMapData(String seriesName, String expName, String[] residues) {
        List<DataSeries> data = new ArrayList<>();
        for (String resNum : residues) {
            data.add(getMapData(seriesName, expName, resNum));
        }
        return data;
    }

    public static DataSeries getMapData(String seriesName, String expName, String resNum) {
        ResidueProperties resProps = residueProperties.get(seriesName);
        ExperimentData expData = resProps.getExperimentData(expName);
        DataSeries series = new DataSeries();
        series.setName(expName + ":" + resNum);
        ResidueData resData = expData.getResidueData(resNum);
        if (resData != null) {
            double[][] xValues = resData.getXValues();
            double[] yValues = resData.getYValues();
            int nValues = yValues.length;
            double[] errValues = resData.getErrValues();
            for (int i = 0; i < nValues; i++) {
                double x = xValues[0][i];
                double y = yValues[i];
                double err = errValues[i];
                XYValue dataPoint = new XYEValue(x, y, err);
                dataPoint.setExtraValue(resData.getDataValues().get(i));
                series.add(dataPoint);
            }
        }
        return series;
    }

    public static ArrayList<GUIPlotEquation> getEquations(ExperimentData expData, String seriesName, String[] residues, String equationName, String state, double field) {
        //System.out.println(" series name is " + seriesName);
        ArrayList<GUIPlotEquation> equations = new ArrayList<>();
        for (String resNum : residues) {
            GUIPlotEquation equation = getEquation(expData, seriesName, resNum, equationName, state, field);
            if (equation != null) {
                equations.add(equation);
            }
        }
        return equations;
    }

    public static GUIPlotEquation getEquation(ExperimentData expData, String seriesName, String resNum, String equationName, String state, double field) {
        ResidueProperties residueProps = residueProperties.get(seriesName);
        ResidueInfo resInfo = residueProps.getResidueInfo(resNum);
        GUIPlotEquation equationCopy = null;
        String expType = expData.getExpMode();
//            ExperimentData expData = residueProps.getExperimentData("cest"); // fixme
        Optional<ExperimentData> optionalData = Optional.empty();
        optionalData = residueProps.getExperimentData().stream().findFirst();
        if (resInfo != null) {
            final String useEquationName;
            if (equationName.equals("best")) {
                useEquationName = resInfo.getBestEquationName();
            } else {
                useEquationName = equationName;
            }

            CurveFit curveSet = resInfo.getCurveSet(useEquationName, state); // fixme
            if (curveSet != null) {
                PlotEquation equation = curveSet.getEquation();
                if (optionalData.isPresent() && optionalData.get().getExtras().size() > 0) {
//                        ExperimentData expData = optionalData.get();
                    double[] pars = curveSet.getEquation().getPars(); //pars = getPars(equationName);
                    double[] errs = curveSet.getEquation().getErrs(); //double[] errs = new double[pars.length];
                    double[] extras = new double[3];
//                        for (int j = 0; j < expData.getExtras().size() / 2; j++) {
//                        extras[0] = field;
//                        extras[1] = expData.getExtras().get(j);
//                        //System.out.println("expData extras size = " + expData.getExtras().size()+ " extra[1] = " + extras[1]);
//                        PlotEquation equationCopy = equation.clone();
//                        equationCopy.setExtra(extras);
                    extras[0] = field;
                    extras[1] = expData.getExtras().get(0);
                    extras[2] = expData.getExtras().get(1);
                    equationCopy = new GUIPlotEquation(expType, useEquationName, pars, errs, extras);
                } else {
                    double[] extras = new double[1];
                    extras[0] = expData.getNucleusField();
                    equationCopy = new GUIPlotEquation(expType, equation);
                    equationCopy.setExtra(extras);
                    //System.out.println("expData extras size = " + expData.getExtras().size()+ " extra[0] = " + extras[0]);

                }

            }
        }

        return equationCopy;
    }

    public static ResidueInfo getResInfo(String seriesName, String residue) {
        ResidueProperties residueProps = residueProperties.get(seriesName);
        ResidueInfo resInfo = residueProps.getResidueInfo(residue);
        return resInfo;
    }

    public static ObservableList<DataSeries> getParMapData(String mapName, String eqnName, String state, String parName) {
        ResidueProperties residueProps = residueProperties.get(mapName);
        ObservableList<DataSeries> data = FXCollections.observableArrayList();

        DataSeries series = new DataSeries();
        series.setName(mapName + '|' + eqnName + "|" + state + "|" + parName);
        data.add(series);
        minRes = Integer.MAX_VALUE;
        maxRes = Integer.MIN_VALUE;
        Collection<ExperimentData> expDataSets = residueProps.getExperimentData();
        for (ExperimentData expData : expDataSets) {
            for (String resNumS : expData.getResidues()) {
                int resNum = Integer.parseInt(resNumS);
                minRes = Math.min(resNum, minRes);
                maxRes = Math.max(resNum, maxRes);
            }
        }

        List<ResidueInfo> resValues = residueProps.getResidueValues();

        for (ResidueInfo resInfo : resValues) {
            String useEquName = eqnName;
            if (resInfo == null) {
                continue;
            }

            if (eqnName.equals("best")) {
                useEquName = resInfo.getBestEquationName();
            }
            int resNum = resInfo.getResNum();
            double x = resNum;
            Double errUp = null;
            Double y = resInfo.getParValue(useEquName, state, parName);
            if (y == null) {
                continue;
            }
            errUp = resInfo.getParValue(useEquName, state, parName + ".sd");
            Double errLow = errUp;
            if (errUp == null) {
                errUp = 0.0;
                errLow = 0.0;
            }
            // fixme  this all needs to be replaced with logarithmic axis
            double yOrig = y;
            double errUpOrig = errUp;
            if (false && parName.equals("Kex")) {
                if (y > 1.0) {
                    y = Math.log10(y);
                    errUp = Math.log10(yOrig + errUp) - y;
                    if ((yOrig - errLow) > 1.0) {
                        errLow = y - Math.log10(yOrig - errLow);
                    } else {
                        errLow = y - Math.log10(1.0);
                    }
                } else {
                    y = 0.0;
                    errUp = 0.0;
                    errLow = 0.0;
                }
            }
            ErrorExtraValues extra;
            if (errUp != null) {
                extra = new ErrorExtraValues(25, -errLow, errUp);
            } else {
                extra = new ErrorExtraValues(25, 0.0, 0.0);
            }

            XYEValue dataPoint = new XYEValue(x, y, errUp);
            series.getData().add(dataPoint);
        }
        return data;
    }

    public static void loadParameters(String fileName) {
        ResidueChart reschartNode = PyController.mainController.getActiveChart();
        if (reschartNode == null) {
            reschartNode = PyController.mainController.addChart();

        }
        File file = new File(fileName);
        if (!file.exists()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Parameter file error");
            alert.setContentText("File " + fileName + " not found");
            alert.showAndWait();
            return;
        }
        ResidueProperties resProp = null;
        try {
            resProp = DataIO.loadYAMLFile(fileName);
        } catch (IOException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Parameter file error");
            alert.setContentText("Error reading file\n" + ex.getLocalizedMessage());
            alert.showAndWait();
            return;
        }
        residueProperties.put(resProp.getName(), resProp);
        String parName = "Kex";
        if (resProp.getExpMode().equals("exp")) {
            parName = "R";
        } else if (resProp.getExpMode().equals("noe")) {
            parName = "NOE";
        }
        ObservableList<DataSeries> data = ChartUtil.getParMapData(resProp.getName(), "best", "0:0:0", parName);
        PyController.mainController.currentResProps = resProp;
        PyController.mainController.makeAxisMenu();
        PyController.mainController.setYAxisType(resProp.getName(), "best", "0:0:0", parName);
        reschartNode.setResProps(resProp);
        PyController.mainController.setControls();
    }

}
