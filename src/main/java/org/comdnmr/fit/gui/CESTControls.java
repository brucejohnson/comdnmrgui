/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.comdnmr.fit.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.comdnmr.fit.calc.CESTFit;
import org.comdnmr.fit.calc.ExperimentData;
import org.comdnmr.fit.calc.ParValueInterface;
import org.comdnmr.fit.calc.PlotEquation;
import org.comdnmr.fit.calc.ResidueInfo;
import org.comdnmr.fit.calc.ResidueProperties;
import static org.comdnmr.fit.gui.CESTControls.PARS.KEX;
import static org.comdnmr.fit.gui.CESTControls.PARS.PB;
import static org.comdnmr.fit.gui.CESTControls.PARS.DELTAA0;
import static org.comdnmr.fit.gui.CESTControls.PARS.DELTAB0;
import static org.comdnmr.fit.gui.CESTControls.PARS.R1A;
import static org.comdnmr.fit.gui.CESTControls.PARS.R1B;
import static org.comdnmr.fit.gui.CESTControls.PARS.R2A;
import static org.comdnmr.fit.gui.CESTControls.PARS.R2B;
import static org.comdnmr.fit.gui.CESTControls.PARS.B1FIELD;
import static org.comdnmr.fit.gui.CESTControls.PARS.TEX;
import org.comdnmr.fit.calc.CalcCEST;

/**
 *
 * @author Martha Beckwith
 */
public class CESTControls implements EquationControls {

    @FXML
    ChoiceBox<String> equationSelector;
    ChoiceBox<String> stateSelector;

    String[] parNames = {"Kex", "Pb", "deltaA0", "deltaB0", "R1A", "R1B", "R2A", "R2B", "B1field", "Tex"};

    enum PARS implements ParControls {
        KEX("Kex", 0.0, 1000.0, 100.0, 150.0),
        PB("Pb", 0.0, 1.0, 0.1, 0.1),
        DELTAA0("deltaA0", 0.0, 3000.0, 100.0, 2700.0),
        DELTAB0("deltaB0", -3000.0, 0.0, 100.0, -1250.0),
        R1A("R1A", 0.0, 10.0, 1.0, 2.5),
        R1B("R1B", 0.0, 10.0, 1.0, 2.5),
        R2A("R2A", 0.0, 200.0, 50.0, 15.0),
        R2B("R2B", 0.0, 200.0, 50.0, 120.0),
        B1FIELD("B1field", 0.0, 200.0, 50.0, 20.0 * 2 * Math.PI),
        TEX("Tex", 0.0, 1.0, 0.1, 0.3);

        String name;
        Slider slider;
        Label label;
        Label valueText;

        PARS(String name, double min, double max, double major, double value) {
            this.name = name;
            slider = new Slider(min, max, value);
            slider.setShowTickLabels(true);
            slider.setShowTickMarks(true);
            slider.setMajorTickUnit(major);
            label = new Label(name);
            label.setPrefWidth(60.0);
            valueText = new Label();
            valueText.setPrefWidth(60);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void addTo(HBox hBox) {
            hBox.getChildren().addAll(label, slider, valueText);
            HBox.setHgrow(slider, Priority.ALWAYS);
        }

        @Override
        public Slider getSlider() {
            return slider;
        }

        @Override
        public void disabled(boolean state) {
            slider.setDisable(state);
        }

        @Override
        public void setValue(double value) {
            slider.setValue(value);
            valueText.setText(String.format("%.1f", value));
        }

        @Override
        public void setText() {
            double value = slider.getValue();
            valueText.setText(String.format("%.1f", value));
        }

        @Override
        public double getValue() {
            return slider.getValue();
        }

    }

    boolean updatingTable = false;
    PyController controller;

    @Override
    public VBox makeControls(PyController controller) {
        this.controller = controller;
        equationSelector = new ChoiceBox<>();
        equationSelector.getItems().addAll(CESTFit.getEquationNames());
        equationSelector.setValue(CESTFit.getEquationNames().get(0));
        stateSelector = new ChoiceBox<>();
        stateSelector.getItems().addAll("0:0:0", "1:0:0");
        stateSelector.setValue("0:0:0");
        VBox vBox = new VBox();
        HBox hBox1 = new HBox();
        HBox.setHgrow(hBox1, Priority.ALWAYS);
        vBox.setFillWidth(true);
        hBox1.getChildren().addAll(equationSelector, stateSelector);
        vBox.getChildren().add(hBox1);

        int i = 0;

        for (ParControls control : PARS.values()) {
            HBox hBox = new HBox();
            HBox.setHgrow(hBox, Priority.ALWAYS);
            control.addTo(hBox);

            control.getSlider().valueProperty().addListener(e -> {
                simSliderAction(control.getName());
            });
            vBox.getChildren().add(hBox);
        }

        if (controller.simulate == true) {
            equationAction();
        }

        equationSelector.valueProperty().addListener(e -> {
            equationAction();
        });
        stateSelector.valueProperty().addListener(e -> {
            stateAction();
        });
        return vBox;
    }

    void equationAction() {
        updatingTable = true;
        String equationName = equationSelector.getValue().toString();
        if (equationName == "") {
            equationName = equationSelector.getItems().get(0);
        }
        //System.out.println("eqnAction eqnName = " + equationName);
        ResidueInfo resInfo = controller.currentResInfo;
        if (resInfo != null) {
            updatingTable = true;
            String state = stateSelector.getValue();
            List<ParValueInterface> parValues = resInfo.getParValues(equationName, state);
            controller.updateTableWithPars(parValues);
            updateSliders(parValues, equationName);
            updatingTable = false;
        }
        switch (equationName) {
            case "CESTR1RHOPERTURBATION":
                KEX.disabled(false);
                PB.disabled(false);
                DELTAA0.disabled(false);
                DELTAB0.disabled(false);
                R1A.disabled(false);
                R1B.disabled(true);
                R2A.disabled(false);
                R2B.disabled(false);
                B1FIELD.disabled(false);
                TEX.disabled(false);
                break;
            case "CESTR1RHON":
                KEX.disabled(false);
                PB.disabled(false);
                DELTAA0.disabled(false);
                DELTAB0.disabled(false);
                R1A.disabled(false);
                R1B.disabled(true);
                R2A.disabled(false);
                R2B.disabled(true);
                B1FIELD.disabled(false);
                TEX.disabled(false);
                break;
            case "CESTR1RHOBALDWINKAY":
                KEX.disabled(false);
                PB.disabled(false);
                DELTAA0.disabled(false);
                DELTAB0.disabled(false);
                R1A.disabled(false);
                R1B.disabled(true);
                R2A.disabled(false);
                R2B.disabled(false);
                B1FIELD.disabled(false);
                TEX.disabled(false);
                break;
            case "CESTR1RHOSD":
                KEX.disabled(false);
                PB.disabled(false);
                DELTAA0.disabled(false);
                DELTAB0.disabled(false);
                R1A.disabled(false);
                R1B.disabled(true);
                R2A.disabled(false);
                R2B.disabled(false);
                B1FIELD.disabled(false);
                TEX.disabled(false);
                break;
            case "CESTR1RHOEXACT1":
                KEX.disabled(false);
                PB.disabled(false);
                DELTAA0.disabled(false);
                DELTAB0.disabled(false);
                R1A.disabled(false);
                R1B.disabled(true);
                R2A.disabled(false);
                R2B.disabled(false);
                B1FIELD.disabled(false);
                TEX.disabled(false);
                break;
            case "CESTEXACT0":
                KEX.disabled(false);
                PB.disabled(false);
                DELTAA0.disabled(false);
                DELTAB0.disabled(false);
                R1A.disabled(false);
                R1B.disabled(false);
                R2A.disabled(false);
                R2B.disabled(false);
                B1FIELD.disabled(false);
                TEX.disabled(false);
                break;
            case "CESTEXACT1":
                KEX.disabled(false);
                PB.disabled(false);
                DELTAA0.disabled(false);
                DELTAB0.disabled(false);
                R1A.disabled(false);
                R1B.disabled(true);
                R2A.disabled(false);
                R2B.disabled(false);
                B1FIELD.disabled(false);
                TEX.disabled(false);
                break;
            case "CESTEXACT2":
                KEX.disabled(false);
                PB.disabled(false);
                DELTAA0.disabled(false);
                DELTAB0.disabled(false);
                R1A.disabled(false);
                R1B.disabled(false);
                R2A.disabled(false);
                R2B.disabled(true);
                B1FIELD.disabled(false);
                TEX.disabled(false);
                break;
            default:
                return;
        }
        simSliderAction(equationName);
        updatingTable = false;
    }

    void stateAction() {
        ResidueInfo resInfo = controller.currentResInfo;
        if (resInfo != null) {
            String state = stateSelector.getValue();
            if (state != null) {
                String equationName = equationSelector.getValue();
                List<ParValueInterface> parValues = resInfo.getParValues(equationName, state);
                controller.updateTableWithPars(parValues);
                updateSliders(parValues, equationName);
            }
        }

    }

    public void simSliderAction(String label) {
        if (updatingTable) {
            return;
        }
//        System.out.println("simSlider");
        String equationName = equationSelector.getValue().toString();
//        if (equationName.equals("CPMGSLOW") && label.equals("Rex")) {
//            return;
//        }
        double kex = KEX.getValue();
        double pb = PB.getValue();
        double deltaA0 = DELTAA0.getValue();
        double deltaB0 = DELTAB0.getValue();
        double R1a = R1A.getValue();
        double R1b = R1B.getValue();
        double R2a = R2A.getValue();
        double R2b = R2B.getValue();
        double B1field = B1FIELD.getValue();
        double Tex = TEX.getValue();
        KEX.setText();
        PB.setText();
        DELTAA0.setText();
        DELTAB0.setText();
        R1A.setText();
        R1B.setText();
        R2A.setText();
        R2B.setText();
        B1FIELD.setText();
        TEX.setText();
        double[][] pars;
        switch (equationName) {
            case "CESTR1RHOPERTURBATION":
                pars = new double[2][8];
                pars[0][0] = kex;
                pars[0][1] = pb;
                pars[0][2] = deltaA0;
                pars[0][3] = deltaB0;
                pars[0][4] = R1a;
                pars[0][5] = R1b;
                pars[0][6] = R2a;
                pars[0][7] = R2b;
                pars[1][0] = B1field;
                pars[1][1] = Tex;
                break;
            case "CESTR1RHON":
                pars = new double[2][8];
                pars[0][0] = kex;
                pars[0][1] = pb;
                pars[0][2] = deltaA0;
                pars[0][3] = deltaB0;
                pars[0][4] = R1a;
                pars[0][5] = R1b;
                pars[0][6] = R2a;
                pars[0][7] = R2b;
                pars[1][0] = B1field;
                pars[1][1] = Tex;
                break;
            case "CESTR1RHOSD":
                pars = new double[2][8];
                pars[0][0] = kex;
                pars[0][1] = pb;
                pars[0][2] = deltaA0;
                pars[0][3] = deltaB0;
                pars[0][4] = R1a;
                pars[0][5] = R1b;
                pars[0][6] = R2a;
                pars[0][7] = R2b;
                pars[1][0] = B1field;
                pars[1][1] = Tex;
                break;
            case "CESTR1RHOBALDWINKAY":
                pars = new double[2][8];
                pars[0][0] = kex;
                pars[0][1] = pb;
                pars[0][2] = deltaA0;
                pars[0][3] = deltaB0;
                pars[0][4] = R1a;
                pars[0][5] = R1b;
                pars[0][6] = R2a;
                pars[0][7] = R2b;
                pars[1][0] = B1field;
                pars[1][1] = Tex;
                break;
            case "CESTR1RHOEXACT1":
                pars = new double[2][8];
                pars[0][0] = kex;
                pars[0][1] = pb;
                pars[0][2] = deltaA0;
                pars[0][3] = deltaB0;
                pars[0][4] = R1a;
                pars[0][5] = R1b;
                pars[0][6] = R2a;
                pars[0][7] = R2b;
                pars[1][0] = B1field;
                pars[1][1] = Tex;
                break;
            case "CESTEXACT0":
                pars = new double[2][8];
                pars[0][0] = kex;
                pars[0][1] = pb;
                pars[0][2] = deltaA0;
                pars[0][3] = deltaB0;
                pars[0][4] = R1a;
                pars[0][5] = R1b;
                pars[0][6] = R2a;
                pars[0][7] = R2b;
                pars[1][0] = B1field;
                pars[1][1] = Tex;
                break;
            case "CESTEXACT1":
                pars = new double[2][8];
                pars[0][0] = kex;
                pars[0][1] = pb;
                pars[0][2] = deltaA0;
                pars[0][3] = deltaB0;
                pars[0][4] = R1a;
                pars[0][5] = R1b;
                pars[0][6] = R2a;
                pars[0][7] = R2b;
                pars[1][0] = B1field;
                pars[1][1] = Tex;
                break;
            case "CESTEXACT2":
                pars = new double[2][8];
                pars[0][0] = kex;
                pars[0][1] = pb;
                pars[0][2] = deltaA0;
                pars[0][3] = deltaB0;
                pars[0][4] = R1a;
                pars[0][5] = R1b;
                pars[0][6] = R2a;
                pars[0][7] = R2b;
                pars[1][0] = B1field;
                pars[1][1] = Tex;
                break;
            default:
                return;
        }
//        if (equationName.equals("CPMGSLOW")) {
//            int[] map = {0, 1, 2, 3};
//            rEx = CPMGEquation.CPMGSLOW.getRex(pars, map);
//            REX.setValue(rEx);
//        }

//        double[] errs = new double[pars.length];
//        int nFields = field2 > (defaultField + 10) ? 2 : 1; // addTo 10.0 to make sure slider set near to bottom gives 1 field
//        double[] fields = new double[nFields];
//        fields[0] = 1.0;
//        if (nFields > 1) {
//            fields[1] = field2 / defaultField;
//        }
        updateEquations();

        //controller.updateChartEquations(equationName, pars, errs, fields);
    }

    double[][] getPars(String equationName) {
        double kex = KEX.getValue();
        double pb = PB.getValue();
        double deltaA0 = DELTAA0.getValue();
        double deltaB0 = DELTAB0.getValue();
        double R1a = R1A.getValue();
        double R1b = R1B.getValue();
        double R2a = R2A.getValue();
        double R2b = R2B.getValue();
        double B1field = B1FIELD.getValue();
        double Tex = TEX.getValue();
        double[][] pars;
        switch (equationName) {
            case "CESTR1RHOPERTURBATION":
                pars = new double[2][8];
                pars[0][0] = kex;
                pars[0][1] = pb;
                pars[0][2] = deltaA0;
                pars[0][3] = deltaB0;
                pars[0][4] = R1a;
                pars[0][5] = R1b;
                pars[0][6] = R2a;
                pars[0][7] = R2b;
                pars[1][0] = B1field;
                pars[1][1] = Tex;
                break;
            case "CESTR1RHON":
                pars = new double[2][8];
                pars[0][0] = kex;
                pars[0][1] = pb;
                pars[0][2] = deltaA0;
                pars[0][3] = deltaB0;
                pars[0][4] = R1a;
                pars[0][5] = R1b;
                pars[0][6] = R2a;
                pars[0][7] = R2b;
                pars[1][0] = B1field;
                pars[1][1] = Tex;
                break;
            case "CESTR1RHOSD":
                pars = new double[2][8];
                pars[0][0] = kex;
                pars[0][1] = pb;
                pars[0][2] = deltaA0;
                pars[0][3] = deltaB0;
                pars[0][4] = R1a;
                pars[0][5] = R1b;
                pars[0][6] = R2a;
                pars[0][7] = R2b;
                pars[1][0] = B1field;
                pars[1][1] = Tex;
                break;
            case "CESTR1RHOBALDWINKAY":
                pars = new double[2][8];
                pars[0][0] = kex;
                pars[0][1] = pb;
                pars[0][2] = deltaA0;
                pars[0][3] = deltaB0;
                pars[0][4] = R1a;
                pars[0][5] = R1b;
                pars[0][6] = R2a;
                pars[0][7] = R2b;
                pars[1][0] = B1field;
                pars[1][1] = Tex;
                break;
            case "CESTR1RHOEXACT1":
                pars = new double[2][8];
                pars[0][0] = kex;
                pars[0][1] = pb;
                pars[0][2] = deltaA0;
                pars[0][3] = deltaB0;
                pars[0][4] = R1a;
                pars[0][5] = R1b;
                pars[0][6] = R2a;
                pars[0][7] = R2b;
                pars[1][0] = B1field;
                pars[1][1] = Tex;
                break;
            case "CESTEXACT0":
                pars = new double[2][8];
                pars[0][0] = kex;
                pars[0][1] = pb;
                pars[0][2] = deltaA0;
                pars[0][3] = deltaB0;
                pars[0][4] = R1a;
                pars[0][5] = R1b;
                pars[0][6] = R2a;
                pars[0][7] = R2b;
                pars[1][0] = B1field;
                pars[1][1] = Tex;
                break;
            case "CESTEXACT1":
                pars = new double[2][8];
                pars[0][0] = kex;
                pars[0][1] = pb;
                pars[0][2] = deltaA0;
                pars[0][3] = deltaB0;
                pars[0][4] = R1a;
                pars[0][5] = R1b;
                pars[0][6] = R2a;
                pars[0][7] = R2b;
                pars[1][0] = B1field;
                pars[1][1] = Tex;
                break;
            case "CESTEXACT2":
                pars = new double[2][8];
                pars[0][0] = kex;
                pars[0][1] = pb;
                pars[0][2] = deltaA0;
                pars[0][3] = deltaB0;
                pars[0][4] = R1a;
                pars[0][5] = R1b;
                pars[0][6] = R2a;
                pars[0][7] = R2b;
                pars[1][0] = B1field;
                pars[1][1] = Tex;
                break;
            default:
                pars = null;
        }
        return pars;

    }

    double[][] getPars(String equationName, Map<String, ParValueInterface> parValues) {
        double[][] pars;
        switch (equationName) {
            case "CESTR1RHOPERTURBATION":
                pars = new double[2][8];
                pars[0][0] = parValues.get("kex").getValue();
                pars[0][1] = parValues.get("pb").getValue();
                pars[0][2] = parValues.get("deltaA0").getValue();
                pars[0][3] = parValues.get("deltaB0").getValue();
                pars[0][4] = parValues.get("R1a").getValue();
                pars[0][5] = parValues.get("R1b").getValue();
                pars[0][6] = parValues.get("R2a").getValue();
                pars[0][7] = parValues.get("R2b").getValue();
                pars[1][0] = parValues.get("B1field").getValue();
                pars[1][1] = parValues.get("Tex").getValue();
                break;
            case "CESTR1RHON":
                pars = new double[2][8];
                pars[0][0] = parValues.get("kex").getValue();
                pars[0][1] = parValues.get("pb").getValue();
                pars[0][2] = parValues.get("deltaA0").getValue();
                pars[0][3] = parValues.get("deltaB0").getValue();
                pars[0][4] = parValues.get("R1a").getValue();
                pars[0][5] = parValues.get("R1b").getValue();
                pars[0][6] = parValues.get("R2a").getValue();
                pars[0][7] = parValues.get("R2b").getValue();
                pars[1][0] = parValues.get("B1field").getValue();
                pars[1][1] = parValues.get("Tex").getValue();
                break;
            case "CESTR1RHOBALDWINKAY":
                pars = new double[2][8];
                pars[0][0] = parValues.get("kex").getValue();
                pars[0][1] = parValues.get("pb").getValue();
                pars[0][2] = parValues.get("deltaA0").getValue();
                pars[0][3] = parValues.get("deltaB0").getValue();
                pars[0][4] = parValues.get("R1a").getValue();
                pars[0][5] = parValues.get("R1b").getValue();
                pars[0][6] = parValues.get("R2a").getValue();
                pars[0][7] = parValues.get("R2b").getValue();
                pars[1][0] = parValues.get("B1field").getValue();
                pars[1][1] = parValues.get("Tex").getValue();
                break;
            case "CESTR1RHOSD":
                pars = new double[2][8];
                pars[0][0] = parValues.get("kex").getValue();
                pars[0][1] = parValues.get("pb").getValue();
                pars[0][2] = parValues.get("deltaA0").getValue();
                pars[0][3] = parValues.get("deltaB0").getValue();
                pars[0][4] = parValues.get("R1a").getValue();
                pars[0][5] = parValues.get("R1b").getValue();
                pars[0][6] = parValues.get("R2a").getValue();
                pars[0][7] = parValues.get("R2b").getValue();
                pars[1][0] = parValues.get("B1field").getValue();
                pars[1][1] = parValues.get("Tex").getValue();
                break;
            case "CESTR1RHOEXACT1":
                pars = new double[2][8];
                pars[0][0] = parValues.get("kex").getValue();
                pars[0][1] = parValues.get("pb").getValue();
                pars[0][2] = parValues.get("deltaA0").getValue();
                pars[0][3] = parValues.get("deltaB0").getValue();
                pars[0][4] = parValues.get("R1a").getValue();
                pars[0][5] = parValues.get("R1b").getValue();
                pars[0][6] = parValues.get("R2a").getValue();
                pars[0][7] = parValues.get("R2b").getValue();
                pars[1][0] = parValues.get("B1field").getValue();
                pars[1][1] = parValues.get("Tex").getValue();
                break;
            case "CESTEXACT0":
                pars = new double[2][8];
                pars[0][0] = parValues.get("kex").getValue();
                pars[0][1] = parValues.get("pb").getValue();
                pars[0][2] = parValues.get("deltaA0").getValue();
                pars[0][3] = parValues.get("deltaB0").getValue();
                pars[0][4] = parValues.get("R1a").getValue();
                pars[0][5] = parValues.get("R1b").getValue();
                pars[0][6] = parValues.get("R2a").getValue();
                pars[0][7] = parValues.get("R2b").getValue();
                pars[1][0] = parValues.get("B1field").getValue();
                pars[1][1] = parValues.get("Tex").getValue();
                break;
            case "CESTEXACT1":
                pars = new double[2][8];
                pars[0][0] = parValues.get("kex").getValue();
                pars[0][1] = parValues.get("pb").getValue();
                pars[0][2] = parValues.get("deltaA0").getValue();
                pars[0][3] = parValues.get("deltaB0").getValue();
                pars[0][4] = parValues.get("R1a").getValue();
                pars[0][5] = parValues.get("R1b").getValue();
                pars[0][6] = parValues.get("R2a").getValue();
                pars[0][7] = parValues.get("R2b").getValue();
                pars[1][0] = parValues.get("B1field").getValue();
                pars[1][1] = parValues.get("Tex").getValue();
                break;
            case "CESTEXACT2":
                pars = new double[2][8];
                pars[0][0] = parValues.get("kex").getValue();
                pars[0][1] = parValues.get("pb").getValue();
                pars[0][2] = parValues.get("deltaA0").getValue();
                pars[0][3] = parValues.get("deltaB0").getValue();
                pars[0][4] = parValues.get("R1a").getValue();
                pars[0][5] = parValues.get("R1b").getValue();
                pars[0][6] = parValues.get("R2a").getValue();
                pars[0][7] = parValues.get("R2b").getValue();
                pars[1][0] = parValues.get("B1field").getValue();
                pars[1][1] = parValues.get("Tex").getValue();
                break;
            default:
                pars = null;
        }
        return pars;

    }
    
    public double[] sliderGuess(String equationName, int[][] map) {
        double kex = KEX.getValue();
        double pb = PB.getValue();
        double deltaA0 = DELTAA0.getValue();
        double deltaB0 = DELTAB0.getValue();
        double R1a = R1A.getValue();
        double R1b = R1B.getValue();
        double R2a = R2A.getValue();
        double R2b = R2B.getValue();
        double B1field = B1FIELD.getValue();
        double Tex = TEX.getValue();
        int nPars = CalcCEST.getNPars(map);
        double[] guesses = new double[nPars];
        switch (equationName) {
            case "CESTR1RHOPERTURBATION":
                for (int id = 0; id < map.length; id++) {
                    guesses[map[id][0]] = kex; //kex
                    guesses[map[id][1]] = pb; //pb
                    guesses[map[id][2]] = deltaA0; //deltaA
                    guesses[map[id][3]] = deltaB0; //deltaB
                    guesses[map[id][4]] = R1a; //R1A
                    guesses[map[id][5]] = R1a; //R1B
                    guesses[map[id][6]] = R2a; //R2A
                    guesses[map[id][7]] = R2b; //R2B
                    guesses[map[id][8]] = B1field; //B1field
                    guesses[map[id][9]] = Tex; //Tex
                }
                break;
            case "CESTR1RHON":
                for (int id = 0; id < map.length; id++) {
                    guesses[map[id][0]] = kex; //kex
                    guesses[map[id][1]] = pb; //pb
                    guesses[map[id][2]] = deltaA0; //deltaA
                    guesses[map[id][3]] = deltaB0; //deltaB
                    guesses[map[id][4]] = R1a; //R1A
                    guesses[map[id][5]] = R1a; //R1B
                    guesses[map[id][6]] = R2a; //R2A
                    guesses[map[id][7]] = R2a; //R2B
                    guesses[map[id][8]] = B1field; //B1field
                    guesses[map[id][9]] = Tex; //Tex
                }
                break;
            case "CESTR1RHOSD":
                for (int id = 0; id < map.length; id++) {
                    guesses[map[id][0]] = kex; //kex
                    guesses[map[id][1]] = pb; //pb
                    guesses[map[id][2]] = deltaA0; //deltaA
                    guesses[map[id][3]] = deltaB0; //deltaB
                    guesses[map[id][4]] = R1a; //R1A
                    guesses[map[id][5]] = R1a; //R1B
                    guesses[map[id][6]] = R2a; //R2A
                    guesses[map[id][7]] = R2b; //R2B
                    guesses[map[id][8]] = B1field; //B1field
                    guesses[map[id][9]] = Tex; //Tex
                }
                break;
            case "CESTR1RHOBALDWINKAY":
                for (int id = 0; id < map.length; id++) {
                    guesses[map[id][0]] = kex; //kex
                    guesses[map[id][1]] = pb; //pb
                    guesses[map[id][2]] = deltaA0; //deltaA
                    guesses[map[id][3]] = deltaB0; //deltaB
                    guesses[map[id][4]] = R1a; //R1A
                    guesses[map[id][5]] = R1a; //R1B
                    guesses[map[id][6]] = R2a; //R2A
                    guesses[map[id][7]] = R2b; //R2B
                    guesses[map[id][8]] = B1field; //B1field
                    guesses[map[id][9]] = Tex; //Tex
                }
                break;
            case "CESTR1RHOEXACT1":
                for (int id = 0; id < map.length; id++) {
                    guesses[map[id][0]] = kex; //kex
                    guesses[map[id][1]] = pb; //pb
                    guesses[map[id][2]] = deltaA0; //deltaA
                    guesses[map[id][3]] = deltaB0; //deltaB
                    guesses[map[id][4]] = R1a; //R1A
                    guesses[map[id][5]] = R1a; //R1B
                    guesses[map[id][6]] = R2a; //R2A
                    guesses[map[id][7]] = R2b; //R2B
                    guesses[map[id][8]] = B1field; //B1field
                    guesses[map[id][9]] = Tex; //Tex
                }
                break;
            case "CESTEXACT0":
                for (int id = 0; id < map.length; id++) {
                    guesses[map[id][0]] = kex; //kex
                    guesses[map[id][1]] = pb; //pb
                    guesses[map[id][2]] = deltaA0; //deltaA
                    guesses[map[id][3]] = deltaB0; //deltaB
                    guesses[map[id][4]] = R1a; //R1A
                    guesses[map[id][5]] = R1b; //R1B
                    guesses[map[id][6]] = R2a; //R2A
                    guesses[map[id][7]] = R2b; //R2B
                    guesses[map[id][8]] = B1field; //B1field
                    guesses[map[id][9]] = Tex; //Tex
                }
                break;
            case "CESTEXACT1":
                for (int id = 0; id < map.length; id++) {
                    guesses[map[id][0]] = kex; //kex
                    guesses[map[id][1]] = pb; //pb
                    guesses[map[id][2]] = deltaA0; //deltaA
                    guesses[map[id][3]] = deltaB0; //deltaB
                    guesses[map[id][4]] = R1a; //R1A
                    guesses[map[id][5]] = R1a; //R1B
                    guesses[map[id][6]] = R2a; //R2A
                    guesses[map[id][7]] = R2b; //R2B
                    guesses[map[id][8]] = B1field; //B1field
                    guesses[map[id][9]] = Tex; //Tex
                }
                break;
            case "CESTEXACT2":
                for (int id = 0; id < map.length; id++) {
                    guesses[map[id][0]] = kex; //kex
                    guesses[map[id][1]] = pb; //pb
                    guesses[map[id][2]] = deltaA0; //deltaA
                    guesses[map[id][3]] = deltaB0; //deltaB
                    guesses[map[id][4]] = R1a; //R1A
                    guesses[map[id][5]] = R1b; //R1B
                    guesses[map[id][6]] = R2a; //R2A
                    guesses[map[id][7]] = R2a; //R2B
                    guesses[map[id][8]] = B1field; //B1field
                    guesses[map[id][9]] = Tex; //Tex
                }
                break;
        }
//        for(int i=0; i<guesses.length; i++){
//            System.out.println("slider guess: " + i + " " + guesses[i]);
//        }
        
        return guesses;

    }

    @Override
    public void updateStates(List<int[]> allStates) {
        StringBuilder sBuilder = new StringBuilder();
        stateSelector.setDisable(true);
        stateSelector.getItems().clear();
        for (int[] state : allStates) {
            sBuilder.setLength(0);
            for (int i = 1; i < state.length; i++) {
                if (sBuilder.length() > 0) {
                    sBuilder.append(':');
                }
                sBuilder.append(state[i]);
            }
            stateSelector.getItems().add(sBuilder.toString());
        }
        stateSelector.setValue(stateSelector.getItems().get(0));
        stateSelector.setDisable(false);

    }

    @Override
    public void updateSliders(List<ParValueInterface> parValues, String equationName) {
        updatingTable = true;
        for (ParValueInterface parValue : parValues) {
            String parName = parValue.getName();
            ParControls control = PARS.valueOf(parName.toUpperCase());
            if (control != null) {
                control.setValue(parValue.getValue());
            }
        }
        ResidueProperties resProps = controller.currentResProps;
//        if (resProps != null) {
//            double[] fields = resProps.getFields();
//            int iField = Integer.parseInt(stateSelector.getValue().substring(0, 1));
//            FIELD2.setValue(fields[iField]);
//            FIELD2.setText();
//        }
        equationSelector.setValue(equationName);

        updatingTable = false;
    }

    @Override
    public String getEquation() {
        return equationSelector.getValue();
    }
    
    public List<String> getParNames() {
        String equationName = equationSelector.getValue().toString();
        List<String> parNames1 = new ArrayList<>();
        switch (equationName) {
            case "CESTR1RHOPERTURBATION":
                parNames1.add("Kex");
                parNames1.add("Pb");
                parNames1.add("deltaA0");
                parNames1.add("deltaB0");
                parNames1.add("R1A");
                parNames1.add("R2A");
                parNames1.add("R2B");
                break;
            case "CESTR1RHON":
                parNames1.add("Kex");
                parNames1.add("Pb");
                parNames1.add("deltaA0");
                parNames1.add("deltaB0");
                parNames1.add("R1A");
                parNames1.add("R2A");
                break;
            case "CESTR1RHOBALDWINKAY":
                parNames1.add("Kex");
                parNames1.add("Pb");
                parNames1.add("deltaA0");
                parNames1.add("deltaB0");
                parNames1.add("R1A");
                parNames1.add("R2A");
                parNames1.add("R2B");
                break;
            case "CESTR1RHOSD":
                parNames1.add("Kex");
                parNames1.add("Pb");
                parNames1.add("deltaA0");
                parNames1.add("deltaB0");
                parNames1.add("R1A");
                parNames1.add("R2A");
                parNames1.add("R2B");
                break;
            case "CESTR1RHOEXACT1":
                parNames1.add("Kex");
                parNames1.add("Pb");
                parNames1.add("deltaA0");
                parNames1.add("deltaB0");
                parNames1.add("R1A");
                parNames1.add("R2A");
                parNames1.add("R2B");
                break;
            case "CESTEXACT0":
                parNames1.add("Kex");
                parNames1.add("Pb");
                parNames1.add("deltaA0");
                parNames1.add("deltaB0");
                parNames1.add("R1A");
                parNames1.add("R1B");
                parNames1.add("R2A");
                parNames1.add("R2B");
                break;
            case "CESTEXACT1":
                parNames1.add("Kex");
                parNames1.add("Pb");
                parNames1.add("deltaA0");
                parNames1.add("deltaB0");
                parNames1.add("R1A");
                parNames1.add("R2A");
                parNames1.add("R2B");
                break;
            case "CESTEXACT2":
                parNames1.add("Kex");
                parNames1.add("Pb");
                parNames1.add("deltaA0");
                parNames1.add("deltaB0");
                parNames1.add("R1A");
                parNames1.add("R1B");
                parNames1.add("R2A");
                break;
        }
        return parNames1;
    }

    void updateEquations() {
//        System.out.println("CEST Controls updateEqns called.");
        ResidueInfo resInfo = controller.currentResInfo;
        ResidueProperties resProps = controller.currentResProps;
        List<PlotEquation> equations = new ArrayList<>();
        double[] pars;
        double[] extras1;
        String equationName = equationSelector.getValue();
        Optional<ExperimentData> optionalData = Optional.empty();
        if (resInfo != null) {
            if (resProps != null) {
                optionalData = resProps.getExperimentData().stream().findFirst();
                if (optionalData.isPresent() && optionalData.get().getExtras().size() > 0) {
                    ExperimentData expData = optionalData.get();
                    pars = getPars(equationName)[0];
                    List<Double> dataExtras = expData.getExtras();
                    double[] errs = new double[pars.length];
                    double[] extras = new double[3];
                    for (int j = 0; j < dataExtras.size()/2; j++) {
                        extras[0] = 1.0;
                        extras[1] = dataExtras.get(2*j);
                        extras[2] = dataExtras.get(2*j+1);
//                        System.out.println("resInfo Res Num = " + resInfo.getResNum());
//                        System.out.println("extras size = " + expData.getExtras().size());
                        //System.out.println("expData extras size = " + expData.getExtras().size()+ " extra[1] = " + extras[1]);
                        PlotEquation plotEquation = new PlotEquation(equationName, pars, errs, extras);
                        //equationCopy.setExtra(extras);

                        equations.add(plotEquation);
                    }
                } else {
                    pars = getPars(equationName)[0];
                    double[] errs = new double[pars.length];
                    double[] extras = new double[1];
                    extras[0] = 1.0;
                    PlotEquation plotEquation = new PlotEquation(equationName, pars, errs, extras);
                    //equationCopy.setExtra(extras);
                    //System.out.println("expData extras size = " + expData.getExtras().size()+ " extra[0] = " + extras[0]);
                    equations.add(plotEquation);
                }
            }

        } else {
            pars = getPars(equationName)[0];
            extras1 = getPars(equationName)[1];
            double[] errs = new double[pars.length];
            double[] extras = new double[3];
            extras[0] = 1.0;
            extras[1] = extras1[0]; //17.0 * 2 * Math.PI;
            extras[2] = extras1[1]; //0.3;
            //System.out.println("updateEquations got called without resProps; extras length = "+extras.length);
            PlotEquation plotEquation = new PlotEquation(equationName, pars, errs, extras);
            equations.add(plotEquation);
        }

        controller.showEquations(equations);
    }
}
