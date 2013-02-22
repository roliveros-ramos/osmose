/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.output;

import fr.ird.osmose.School;

/**
 *
 * @author pverley
 */
public class AbundanceTotIndicator extends AbstractIndicator {

    private double[] abundance;

    @Override
    public void initStep() {
        // Nothing to do
    }

    @Override
    public void reset() {
        abundance = new double[getNSpecies()];
    }

    @Override
    public void update() {

        for (School school : getPopulation().getAliveSchools()) {
            if (getOsmose().isIncludeClassZero()) {
                abundance[school.getSpeciesIndex()] += school.getAbundance();
            }
        }
    }

    @Override
    public boolean isEnabled() {
        return getOsmose().isIncludeClassZero() && !getOsmose().isCalibrationOutput();
    }

    @Override
    public void write(float time) {

        double nsteps = getOsmose().savingDtMatrix;
        for (int i = 0; i < abundance.length; i++) {
            abundance[i] /= nsteps;
        }
        writeVariable(time, abundance);
    }

    @Override
    String getFilename() {
        StringBuilder filename = new StringBuilder(getOsmose().outputPrefix);
        filename.append("_abundance-total_Simu");
        filename.append(getSimulation().getReplica());
        filename.append(".csv");
        return filename.toString();
    }

    @Override
    String getDescription() {
        return "Mean abundance (number of fish), including first ages specified in input (typically in calibration file)";
    }
    
    @Override
    String[] getHeaders() {
        String[] species = new String[getNSpecies()];
        for (int i = 0; i < species.length; i++) {
            species[i] = getSimulation().getSpecies(i).getName();
        }
        return species;
    }
}
