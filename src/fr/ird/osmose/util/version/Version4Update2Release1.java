/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.util.version;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import fr.ird.osmose.util.Separator;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

/**
 *
 * @author pverley
 */
public class Version4Update2Release1 extends AbstractVersion {

    public Version4Update2Release1() {
        super(4, 2, 1, 2019, Calendar.SEPTEMBER, 19);
    }

    @Override
    void updateParameters() {
        
        updateKey("output.yield.abundance.enabled", "output.yieldN.enabled");
        updateKey("output.yield.biomass.enabled", "output.yield.enabled");

    }
}
