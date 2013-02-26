/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.process;

import fr.ird.osmose.Cell;
import fr.ird.osmose.School;
import fr.ird.osmose.Simulation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author pverley
 */
public class PredationProcess extends AbstractProcess {

    private float[][] predPreySizesMax, predPreySizesMin;
    private float[] predationRate;
    
    public PredationProcess(int replica) {
        super(replica);
    }

    @Override
    public void init() {
        predPreySizesMax = getOsmose().predPreySizesMaxMatrix;
        predPreySizesMin = getOsmose().predPreySizesMinMatrix;
        predationRate = getOsmose().predationRateMatrix;
    }

    @Override
    public void run() {
        for (Cell cell : getGrid().getCells()) {
            List<School> schools = getPopulation().getSchools(cell);
            Collections.shuffle(schools);
            int ns = schools.size();
            double[] preyedBiomass = new double[ns];
            if (!(cell.isLand() || schools.isEmpty())) {
                double[] nDeadPredation = new double[ns];
                // Compute predation
                for (int ipred = 0; ipred < ns; ipred++) {
                    School predator = schools.get(ipred);
                    double[] preyUpon = computePredation(predator, School.INISTEP_BIOMASS, 1);
                    for (int iprey = 0; iprey < ns; iprey++) {
                        if (iprey < ns) {
                            School prey = schools.get(iprey);
                            nDeadPredation[iprey] += prey.biom2abd(preyUpon[iprey]);
                            prey.setNdeadPredation(prey.getNdeadPredation() + prey.biom2abd(preyUpon[iprey]));
                        }
                    }
                    preyedBiomass[ipred] = sum(preyUpon);
                }
                // Apply predation mortality
                for (int is = 0; is < ns; is++) {
                    School school = schools.get(is);
                    school.setNdeadPredation(0);
                    double biomassToPredate = school.getBiomass() * getPredationRate(school, 1);
                    school.predSuccessRate = computePredSuccessRate(biomassToPredate, preyedBiomass[is]);
                    school.setAbundance(school.getAbundance() - nDeadPredation[is]);
                    if (school.getAbundance() < 1.d) {
                        school.setAbundance(0.d);
                    }
                }
            }
        }

    }

    /**
     * Returns the matrix of predation for a given predator.
     *
     * @param predator
     * @param instantaneous, whether we should consider the instantaneous
     * biomass of the schools or the biomass at the beginning of the time step.
     * @param subdt, one by default
     * @return the matrix of predation
     */
    public double[] computePredation(School predator, boolean instantaneous, int subdt) {

        Cell cell = predator.getCell();
        List<School> schools = getPopulation().getSchools(predator.getCell());
        int nFish = schools.size();
        double[] preyUpon = new double[schools.size() + getOsmose().getNumberLTLGroups()];
        // egg do not predate
        if (predator.getAgeDt() == 0) {
            return preyUpon;
        }
        // find the preys
        int[] indexPreys = findPreys(predator);

        // Compute accessible biomass
        // 1. from preys
        double[] accessibleBiomass = new double[indexPreys.length];
        for (int i = 0; i < indexPreys.length; i++) {
            School prey = schools.get(indexPreys[i]);
            accessibleBiomass[i] = (instantaneous)
                    ? getAccessibility(predator, prey) * prey.getInstantaneousBiomass()
                    : getAccessibility(predator, prey) * prey.getBiomass();
        }
        double biomAccessibleTot = sum(accessibleBiomass);
        // 2. from plankton
        float[] percentPlankton = getPercentPlankton(predator);
        for (int i = 0; i < getOsmose().getNumberLTLGroups(); i++) {
            float tempAccess = getOsmose().accessibilityMatrix[getOsmose().getNumberSpecies() + i][0][predator.getSpeciesIndex()][predator.getAccessibilityStage()];
            biomAccessibleTot += percentPlankton[i] * tempAccess * getSimulation().getPlankton(i).getAccessibleBiomass(cell);
        }

        // Compute the potential biomass that predators could prey upon
        double biomassToPredate = instantaneous
                ? getPredationRate(predator, subdt) * predator.getInstantaneousBiomass()
                : getPredationRate(predator, subdt) * predator.getBiomass();
        /*
         * phv 20121219 - this is just a way to stick to what is done in
         * Osmose version SCHOOL2012 and previous version.
         * Tbe biomassToPredate variable of the predator is update on the fly.
         * Should check how it is done in version WS2009 and make sure that it
         * is equivalent to what is done here. It might have some consequences
         * for School.predSuccessRate which influences growth and starvation.
         * phv 20130222 deleted variable school.biomassToPredate
         */
        if (Simulation.VERSION.equals(Simulation.Version.SCHOOL2012_BIOM) || Simulation.VERSION.equals(Simulation.Version.SCHOOL2012_PROD)) {
            //predator.biomassToPredate = biomassToPredate;
        }

        // Distribute the predation over the preys
        if (biomAccessibleTot != 0) {
            // There is less prey available than the predator can
            // potentially prey upon. Predator will feed upon the total
            // accessible biomass
            if (biomAccessibleTot <= biomassToPredate) {
                biomassToPredate = biomAccessibleTot;
            }

            // Assess the loss for the preys caused by this predator
            // Assess the gain for the predator from preys
            for (int i = 0; i < indexPreys.length; i++) {
                double ratio = accessibleBiomass[i] / biomAccessibleTot;
                preyUpon[indexPreys[i]] = ratio * biomassToPredate;
            }
            // Assess the gain for the predator from plankton
            // Assess the loss for the plankton caused by the predator
            for (int i = 0; i < getOsmose().getNumberLTLGroups(); i++) {
                float tempAccess = getOsmose().accessibilityMatrix[getOsmose().getNumberSpecies() + i][0][predator.getSpeciesIndex()][predator.getAccessibilityStage()];
                double ratio = percentPlankton[i] * tempAccess * getSimulation().getPlankton(i).getAccessibleBiomass(cell) / biomAccessibleTot;
                preyUpon[nFish + i] = ratio * biomassToPredate;
            }

        } else {
            // Case 2: there is no prey available
            // No loss !
        }
        return preyUpon;
    }

    private double sum(double[] array) {
        double sum = 0.d;
        for (int i = 0; i < array.length; i++) {
            sum += array[i];
        }
        return sum;
    }

    /**
     *
     * @param cell
     * @param instantaneous
     * @param subdt
     * @return
     */
    public double[][] computePredationMatrix(Cell cell, boolean instantaneous, int subdt) {

        List<School> schools = getPopulation().getSchools(cell);
        double[][] preyUpon = new double[schools.size() + getOsmose().getNumberLTLGroups()][schools.size() + getOsmose().getNumberLTLGroups()];
        // Loop over the schools of the cell
        for (School school : schools) {
            school.setNdeadPredation(0);
        }
        for (int iPred = 0; iPred < schools.size(); iPred++) {
            preyUpon[iPred] = computePredation(schools.get(iPred), instantaneous, subdt);
        }
        return preyUpon;
    }

    /**
     * Compute the rate of predation success.
     *
     * @param biomassToPredate, the max biomass [ton] that a school can prey.
     * @param preyedBiomass, the biomass [ton] effectively preyed.
     * @return
     */
    public float computePredSuccessRate(double biomassToPredate, double preyedBiomass) {

        // Compute the predation success rate
        return Math.min((float) (preyedBiomass / biomassToPredate), 1.f);
    }

    private float[] getPercentPlankton(School predator) {
        float[] percentPlankton = new float[getOsmose().getNumberLTLGroups()];
        int iPred = predator.getSpeciesIndex();
        float preySizeMax = predator.getLength() / predPreySizesMax[iPred][predator.getFeedingStage()];
        float preySizeMin = predator.getLength() / predPreySizesMin[iPred][predator.getFeedingStage()];
        for (int i = 0; i < getOsmose().getNumberLTLGroups(); i++) {
            if ((preySizeMin > getSimulation().getPlankton(i).getSizeMax()) || (preySizeMax < getSimulation().getPlankton(i).getSizeMin())) {
                percentPlankton[i] = 0.0f;
            } else {
                percentPlankton[i] = getSimulation().getPlankton(i).calculPercent(preySizeMin, preySizeMax);
            }
        }
        return percentPlankton;
    }

    /**
     * Returns a list of preys for a given predator.
     *
     * @param predator
     * @return the list of preys for this predator
     */
    private int[] findPreys(School predator) {

        int iPred = predator.getSpeciesIndex();
        List<School> schoolsInCell = getPopulation().getSchools(predator.getCell());
        //schoolsInCell.remove(predator);
        float preySizeMax = predator.getLength() / predPreySizesMax[iPred][predator.getFeedingStage()];
        float preySizeMin = predator.getLength() / predPreySizesMin[iPred][predator.getFeedingStage()];
        List<Integer> indexPreys = new ArrayList();
        for (int iPrey = 0; iPrey < schoolsInCell.size(); iPrey++) {
            School prey = schoolsInCell.get(iPrey);
            if (prey.equals(predator)) {
                continue;
            }
            if (prey.getLength() >= preySizeMin && prey.getLength() < preySizeMax) {
                indexPreys.add(iPrey);
            }
        }
        int[] index = new int[indexPreys.size()];
        for (int iPrey = 0; iPrey < indexPreys.size(); iPrey++) {
            index[iPrey] = indexPreys.get(iPrey);
        }
        return index;
    }

    /**
     * Gets the maximum predation rate of a predator per time step
     *
     * @param predator
     * @param subdt
     * @return
     */
    public double getPredationRate(School predator, int subdt) {
        return predationRate[predator.getSpeciesIndex()] / (double) (getOsmose().getNumberTimeStepsPerYear() * subdt);
    }

    /*
     * Get the accessible biomass that predator can feed on prey
     */
    private double getAccessibility(School predator, School prey) {
        return getOsmose().accessibilityMatrix[prey.getSpeciesIndex()][prey.getAccessibilityStage()][predator.getSpeciesIndex()][predator.getAccessibilityStage()];
    }
}
