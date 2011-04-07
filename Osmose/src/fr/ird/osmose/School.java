package fr.ird.osmose;

/********************************************************************************
 * <p>Titre : School class</p>
 *
 * <p>Description : Basic unit of Osmose model - represents a super-individual </p>
 *
 * <p>Copyright : Copyright (c) may 2009</p>
 *
 * <p>Society : IRD, France </p>
 *
 * @author Yunne Shin, Morgane Travers
 * @version 2.1
 ******************************************************************************** 
 */
import java.util.*;

public class School {

    /*
     * ********
     * * Logs *
     * ********
     * 2011/04/07 phv
     * Deleted variabled posi & posj and replaced them by a cell variable which
     * indicates the cell where the school is located.
     * Encapsulated most of the variables.
     * Renamed void distribute() into randomDeal()
     * Recoded public void randomWalk(). Calling Grid.getNeighborCells
     * since Cell.neighbors[] has been deleted.
     * ***
     */
///////////////////////////////
// Declaration of the variables
///////////////////////////////

    /*
     * The Cell where is located the School
     */
    private Cell cell;
    /*
     * Cohort of the School
     */
    private Cohort cohort;
    /*
     * Number of the serie
     */
    int numSerie;
    /*
     * Index of the cell (i,j)
     * phv: do not understand yet. @see Simulation.distributeSpeciesIni()
     */
    int indexij;
    /*
     * Tag for outOfZone schools
     */
    private boolean outOfZoneSchool;
    /*
     * length of the individuals in the school in centimeters
     */
    private float length;
    /*
     * weight of individual of the school in grams
     */
    private float weight;
    /*
     * Trophic level per age
     */
    private float[] trophicLevel;
    /*
     * Number of individuals in the school
     */
    private long abundance;
    /*
     * Biomass of the school in tons
     */
    private double biomass;
    /*
     * Predation success rate
     */
    private float predSuccessRate;
    /*
     * whether the school will disappear at next time step
     */
    private boolean disappears;
    /*
     * Available biomass [ton] of the school for predation by other schools
     */
    private double biomassToPredate;
    /*
     * Maximum prey size [cm]
     */
    float critPreySizeMax;
    /*
     * Minimum prey size [cm]
     */
    float critPreySizeMin;
    /*
     * Whether the school is catchable for fishing
     */
    private boolean catchable;
    /*
     * Correspond to feeding length-stage
     */
    private int feedingStage;
    /*
     * Correspond to the age-stage used for accessibilities between species
     */
    private int accessibilityStage;
    /*
     * 
     */
    int dietOutputStage;
    private float[][] dietTemp;
    private float sumDiet;

//////////////
// Constructor
//////////////
    /**
     * Create a new school.
     * @param cohort of the school
     * @param abundance, number of individuals in the school
     * @param length [cm] of the individual
     * @param weight [g] of the individual
     */
    public School(Cohort cohort, long abundance, float length, float weight) {
        this.cohort = cohort;
        this.numSerie = cohort.numSerie;
        this.abundance = abundance;
        this.length = length;
        this.weight = weight;
        this.biomass = ((double) abundance) * weight / 1000000.;
        disappears = false;
        catchable = true;

        // initialisation TLs
        trophicLevel = new float[cohort.species.nbCohorts];    // table of the TL of this school at each time step
        for (int t = 0; t < cohort.species.nbCohorts; t++) {
            trophicLevel[t] = 0f;
        }
        if (cohort.ageNbDt == 0) {
            trophicLevel[cohort.ageNbDt] = cohort.species.TLeggs;   // initialisation of the previous age because predation is based on TL at the previous time step
        }
        if (cohort.ageNbDt != 0) {
            trophicLevel[cohort.ageNbDt - 1] = 3f;   // initialisation of the previous age because predation is based on TL at the previous time step
        }
        // initialisation of stage
        updateFeedingStage(cohort.species.sizeFeeding, cohort.species.nbFeedingStages);
        updateAccessStage(cohort.species.ageStagesTab, cohort.species.nbAccessStages);
        updateDietOutputStage(cohort.species.dietStagesTab, cohort.species.nbDietStages);

        this.outOfZoneSchool = false;

        dietTemp = new float[cohort.species.simulation.species.length + cohort.species.simulation.couple.nbPlankton][];
        for (int i = 0; i < cohort.species.simulation.species.length; i++) {
            dietTemp[i] = new float[cohort.species.simulation.species[i].nbDietStages];
            for (int s = 0; s < cohort.species.simulation.species[i].nbDietStages; s++) {
                dietTemp[i][s] = 0;
            }
        }
        for (int i = cohort.species.simulation.species.length; i < cohort.species.simulation.species.length + cohort.species.simulation.couple.nbPlankton; i++) {
            dietTemp[i] = new float[1];
            dietTemp[i][0] = 0;
        }
        sumDiet = 0;

    }

////////////////////////////
// Definition of the methods
////////////////////////////
    /**
     * Gets the current location of the school
     * @return the cell where is located the school
     */
    public Cell getCell() {
        return cell;
    }

    /**
     * Randomly choose one cell out the list of cells.
     * @param cells, the list of cells for the random deal.
     */
    public void randomDeal(List<Cell> cells) {

        // indexij to test the probability of presence
        indexij = (int) Math.round((cells.size() - 1) * Math.random());
        cell = cells.get(indexij);
    }

    /**
     * Function called when update of schools present in cells after having
     * removed all elements from vectPresentSchools in all cells
     */
    public void communicatePosition() {
        cell.add(this);
    }

    /**
     * Randomly move the school in one of the neighbor cells (including the
     * current cell).
     */
    public void randomWalk() {

        /* Create a list of the accessible cells
         * => neighbor cells that are not in land + current cell
         */
        List<Cell> accessibleCells = new ArrayList();
        Iterator<Cell> neighbors = getGrid().getNeighborCells(cell).iterator();
        while (neighbors.hasNext()) {
            Cell neighbor = neighbors.next();
            if (!neighbor.isLand()) {
                accessibleCells.add(neighbor);
            }
        }
        accessibleCells.add(cell);

        /* Randomly choose the new cell */
        randomDeal(accessibleCells);
    }

    private Grid getGrid() {
        return Osmose.getInstance().getGrid();
    }

    public void updateAccessStage(float[] ageStages, int nbAccessStages) {
        accessibilityStage = 0;
        for (int i = 1; i < nbAccessStages; i++) {
            if (getCohort().ageNbDt >= ageStages[i - 1]) {
                accessibilityStage++;
            }
        }
    }

    public void updateFeedingStage(float[] sizeStages, int nbStages) {
        feedingStage = 0;
        for (int i = 1; i < nbStages; i++) {
            if (getLength() >= sizeStages[i - 1]) {
                feedingStage++;
            }
        }
    }

    public void updateDietOutputStage(float[] thresholdTab, int nbStages) {
        dietOutputStage = 0;

        if (getCohort().species.simulation.dietMetric.equalsIgnoreCase("size")) {
            for (int i = 1; i < nbStages; i++) {
                if (getLength() >= thresholdTab[i - 1]) {
                    dietOutputStage++;
                }
            }
        } else if (getCohort().species.simulation.dietMetric.equalsIgnoreCase("age")) {
            for (int i = 1; i < nbStages; i++) {
                int tempAge = Math.round(thresholdTab[i - 1] * getCohort().species.simulation.nbDt);
                if (getLength() >= tempAge) {
                    dietOutputStage++;
                }
            }
        }
    }

    public void predation() {

        Coupling myCouple = getCohort().species.simulation.couple;

        float[] percentPlankton;
        percentPlankton = new float[myCouple.nbPlankton];

        biomassToPredate = getBiomass() * getCohort().species.predationRate / (float) getCohort().species.simulation.nbDt;
        double biomassToPredateIni = biomassToPredate;

        critPreySizeMax = getLength() / getCohort().species.predPreySizesMax[feedingStage];
        critPreySizeMin = getLength() / getCohort().species.predPreySizesMin[feedingStage];

        //************ Calculation of available fish******************

        int indexMax = 0;
        while ((indexMax < (cell.size() - 1))
                && (critPreySizeMax >= ((School) cell.get(indexMax)).getLength())) {
            indexMax++;
        }
        //indexMax is the index of vectBP which corresponds to the sup limit of the possible prey sizes
        //for this school, vectPresentSchools(indexMax) is not a possible prey school.

        int indexMin = 0;
        while ((indexMin < (cell.size() - 1))
                && (critPreySizeMin > ((School) cell.get(indexMin)).getLength())) {
            indexMin++;
        }
        //indexMin is the index of vectBP which corresponds to the inf limit of the possible prey sizes
        //for this school, vectPresentSchools(indexMin) IS a possible prey school.

        //summation of the accessible biom of school prey, for distributing predation mortality
        //cf third version further in the program, 21 january 2002
        double biomAccessibleTot = 0;
        for (int k = indexMin; k < indexMax; k++) {

            float tempAccess;
            School preySchool = (School) cell.get(k);
            tempAccess = getCohort().species.simulation.osmose.accessibilityMatrix[preySchool.getCohort().species.number - 1][preySchool.accessibilityStage][getCohort().species.number - 1][accessibilityStage];
            biomAccessibleTot += ((School) cell.get(k)).getBiomass() * tempAccess;
        }
        //************ Calculation of available plankton ***************
        for (int i = 0; i < myCouple.nbPlankton; i++) {
            if ((critPreySizeMin > myCouple.planktonList[i].sizeMax) || (critPreySizeMax < myCouple.planktonList[i].sizeMin)) {
                percentPlankton[i] = 0.0f;
            } else {
                float tempAccess;
                tempAccess = getCohort().species.simulation.osmose.accessibilityMatrix[getCohort().species.simulation.nbSpecies + i][0][getCohort().species.number - 1][accessibilityStage];
                percentPlankton[i] = myCouple.planktonList[i].calculPercent(critPreySizeMin, critPreySizeMax);
                biomAccessibleTot += percentPlankton[i] * tempAccess * myCouple.planktonList[i].accessibleBiomass[cell.getI()][cell.getJ()];
            }
        }


        int[] tabIndices = new int[indexMax - indexMin];

        Vector vectIndices = new Vector();
        for (int i = indexMin; i < indexMax; i++) {
            vectIndices.addElement(new Integer(i));
        }
        int z = 0;
        while (z < tabIndices.length) {
            int rand = (int) (Math.round((vectIndices.size() - 1) * Math.random()));
            tabIndices[z] = ((Integer) vectIndices.elementAt(rand)).intValue();
            vectIndices.removeElementAt(rand);
            z++;
        }

        /*-------code pour predation with inaccessible biom, of 10%
        calculated in simulation.step for each school before each phase of predation
        not quarter to quarter but directly according to options 21 january 2002*/

        if (biomAccessibleTot != 0) // if there is food
        {
            if (biomAccessibleTot <= biomassToPredate) // = NOT ENOUGH PREY --------------------------------
            {
                biomassToPredate -= biomAccessibleTot;
                // ***********Predation of fish*********************
                for (int k = 0; k < tabIndices.length; k++) {
                    School mySchoolk = (School) cell.get(tabIndices[k]);
                    float tempAccess = getCohort().species.simulation.osmose.accessibilityMatrix[mySchoolk.getCohort().species.number - 1][mySchoolk.accessibilityStage][getCohort().species.number - 1][accessibilityStage];

                    float TLprey;
                    if ((mySchoolk.getCohort().ageNbDt == 0) || (mySchoolk.getCohort().ageNbDt == 1)) // prey are eggs or were eggs at the previous time step
                    {
                        TLprey = getCohort().species.TLeggs;
                    } else {
                        TLprey = mySchoolk.getTrophicLevel()[mySchoolk.getCohort().ageNbDt - 1];
                    }

                    getTrophicLevel()[cohort.ageNbDt] += TLprey * (float) (mySchoolk.getBiomass() * tempAccess / biomAccessibleTot);

                    if ((getCohort().species.simulation.dietsOutput) && (getCohort().species.simulation.t >= getCohort().species.simulation.osmose.timeSeriesStart)) {
                        dietTemp[mySchoolk.cohort.species.number - 1][mySchoolk.dietOutputStage] += mySchoolk.getBiomass() * tempAccess;
                    }
                    //
                    mySchoolk.cohort.nbDeadPp += Math.round((mySchoolk.getBiomass() * tempAccess * 1000000.) / mySchoolk.getWeight());
                    mySchoolk.setBiomass(mySchoolk.getBiomass() - mySchoolk.getBiomass() * tempAccess);
                    mySchoolk.setAbundance(mySchoolk.getAbundance() - Math.round((mySchoolk.getBiomass() * tempAccess * 1000000.) / mySchoolk.getWeight()));

                    if (mySchoolk.getAbundance() <= 0) {
                        mySchoolk.setAbundance(0);
                        mySchoolk.setBiomass(0);
                        mySchoolk.tagForRemoval();
                    }

                }
                //**********Predation on plankton**************
                for (int i = 0; i < getCohort().species.simulation.couple.nbPlankton; i++) {
                    if (percentPlankton[i] != 0.0f) {
                        float tempAccess;
                        tempAccess = getCohort().species.simulation.osmose.accessibilityMatrix[getCohort().species.simulation.nbSpecies + i][0][getCohort().species.number - 1][accessibilityStage];

                        double tempBiomEaten = percentPlankton[i] * tempAccess * myCouple.planktonList[i].accessibleBiomass[cell.getI()][cell.getJ()];

                        myCouple.planktonList[i].accessibleBiomass[cell.getI()][cell.getJ()] -= tempBiomEaten;
                        myCouple.planktonList[i].biomass[cell.getI()][cell.getJ()] -= tempBiomEaten;
                        getTrophicLevel()[cohort.ageNbDt] += myCouple.planktonList[i].trophicLevel * (float) (tempBiomEaten / biomAccessibleTot);
                        if ((getCohort().species.simulation.dietsOutput) && (getCohort().species.simulation.t >= getCohort().species.simulation.osmose.timeSeriesStart)) {
                            dietTemp[cohort.species.simulation.species.length + i][0] += tempBiomEaten;
                        }
                    }
                }
            } else // = ENOUGH PREY --------------------------------------------------------------------
            {
                // *********** Predation on fish ******************
                for (int k = 0; k < tabIndices.length; k++) {
                    School mySchoolk = (School) cell.get(tabIndices[k]);
                    float tempAccess = getCohort().species.simulation.osmose.accessibilityMatrix[mySchoolk.getCohort().species.number - 1][mySchoolk.accessibilityStage][getCohort().species.number - 1][accessibilityStage];

                    float TLprey;
                    if ((mySchoolk.getCohort().ageNbDt == 0) || (mySchoolk.getCohort().ageNbDt == 1)) // prey are eggs or were eggs at the previous time step
                    {
                        TLprey = getCohort().species.TLeggs;
                    } else {
                        TLprey = mySchoolk.getTrophicLevel()[mySchoolk.getCohort().ageNbDt - 1];
                    }

                    getTrophicLevel()[cohort.ageNbDt] += TLprey * (float) (mySchoolk.getBiomass() * tempAccess / biomAccessibleTot);// TL of the prey at the previous time step because all schools hav'nt predate yet

                    if ((getCohort().species.simulation.dietsOutput) && (getCohort().species.simulation.t >= getCohort().species.simulation.osmose.timeSeriesStart)) {
                        dietTemp[mySchoolk.cohort.species.number - 1][mySchoolk.dietOutputStage] += mySchoolk.getBiomass() * tempAccess * biomassToPredate / biomAccessibleTot;
                    }

                    mySchoolk.cohort.nbDeadPp += Math.round((mySchoolk.getBiomass() * tempAccess
                            * biomassToPredate * 1000000.) / (mySchoolk.getWeight() * biomAccessibleTot));
                    mySchoolk.setAbundance(mySchoolk.getAbundance() - Math.round((mySchoolk.getBiomass() * tempAccess * biomassToPredate * 1000000.) / (mySchoolk.getWeight() * biomAccessibleTot)));
                    mySchoolk.setBiomass(mySchoolk.getBiomass() - mySchoolk.getBiomass() * tempAccess * biomassToPredate / biomAccessibleTot);

                    if (mySchoolk.getAbundance() <= 0) {
                        mySchoolk.setAbundance(0);
                        mySchoolk.setBiomass(0);
                        mySchoolk.tagForRemoval();
                    }

                }
                //************ Predation on plankton ***************
                for (int i = 0; i < getCohort().species.simulation.couple.nbPlankton; i++) {
                    if (percentPlankton[i] != 0.0f) {
                        float tempAccess;
                        tempAccess = getCohort().species.simulation.osmose.accessibilityMatrix[getCohort().species.simulation.nbSpecies + i][0][getCohort().species.number - 1][accessibilityStage];

                        double tempBiomEaten = percentPlankton[i] * tempAccess * myCouple.planktonList[i].accessibleBiomass[cell.getI()][cell.getJ()] * biomassToPredate / biomAccessibleTot;

                        myCouple.planktonList[i].accessibleBiomass[cell.getI()][cell.getJ()] -= tempBiomEaten;
                        myCouple.planktonList[i].biomass[cell.getI()][cell.getJ()] -= tempBiomEaten;
                        getTrophicLevel()[cohort.ageNbDt] += myCouple.planktonList[i].trophicLevel * (float) (tempBiomEaten / biomassToPredate);

                        if ((getCohort().species.simulation.dietsOutput) && (getCohort().species.simulation.t >= getCohort().species.simulation.osmose.timeSeriesStart)) {
                            dietTemp[cohort.species.simulation.species.length + i][0] += tempBiomEaten;
                        }
                    }
                }

                biomassToPredate = 0;

            }//end else (case of enough prey)

            getTrophicLevel()[cohort.ageNbDt] += 1;
        } else // biomAccessibleTot = 0, i.e. No food
        {
            getTrophicLevel()[getCohort().ageNbDt] = getTrophicLevel()[getCohort().ageNbDt - 1];     // no food available, so current TL = previous TL
        }
        predSuccessRate = (float) (1 - biomassToPredate / biomassToPredateIni);     // = predSuccessRate for the current time step
        if ((getCohort().species.simulation.dietsOutput) && (getCohort().species.simulation.t >= getCohort().species.simulation.osmose.timeSeriesStart)) {
            for (int i = 0; i < getCohort().species.simulation.species.length; i++) {
                for (int s = 0; s < getCohort().species.simulation.species[i].nbDietStages; s++) {
                    sumDiet += dietTemp[i][s];
                    cohort.species.simulation.predatorsPressureMatrix[cohort.species.number - 1][dietOutputStage][i][s] += dietTemp[i][s];
                }
            }
            for (int i = getCohort().species.simulation.species.length; i < getCohort().species.simulation.species.length + getCohort().species.simulation.couple.nbPlankton; i++) {
                sumDiet += dietTemp[i][0];
                cohort.species.simulation.predatorsPressureMatrix[cohort.species.number - 1][dietOutputStage][i][0] += dietTemp[i][0];
            }

            if (sumDiet != 0) {
                cohort.species.simulation.nbStomachs[cohort.species.number - 1][dietOutputStage] += getAbundance();
                for (int i = 0; i < getCohort().species.simulation.species.length; i++) {
                    for (int s = 0; s < getCohort().species.simulation.species[i].nbDietStages; s++) {
                        cohort.species.simulation.dietsMatrix[cohort.species.number - 1][dietOutputStage][i][s] += getAbundance() * dietTemp[i][s] / sumDiet;
                    }
                }
                for (int i = getCohort().species.simulation.species.length; i < getCohort().species.simulation.species.length + getCohort().species.simulation.couple.nbPlankton; i++) {
                    cohort.species.simulation.dietsMatrix[cohort.species.number - 1][dietOutputStage][i][0] += getAbundance() * dietTemp[i][0] / sumDiet;
                }
                sumDiet = 0;
            }
            for (int i = 0; i < getCohort().species.simulation.species.length; i++) {
                for (int s = 0; s < getCohort().species.simulation.species[i].nbDietStages; s++) {
                    dietTemp[i][s] = 0;
                }
            }
            for (int i = getCohort().species.simulation.species.length; i < getCohort().species.simulation.species.length + getCohort().species.simulation.couple.nbPlankton; i++) {
                dietTemp[i][0] = 0;
            }

        }
    }

    public void surviveP() {
        if (predSuccessRate <= getCohort().species.criticalPredSuccess) {
            long nbDead;
            double mortalityRate;

            mortalityRate = -(cohort.species.starvMaxRate * predSuccessRate) / getCohort().species.criticalPredSuccess + getCohort().species.starvMaxRate;
            if (mortalityRate < 0) {
                mortalityRate = 0;
                System.out.print("starvation bug ");
            }
            nbDead = (long) (Math.round((double) getAbundance()) * (1 - Math.exp(-mortalityRate / (float) getCohort().species.simulation.nbDt)));

            setAbundance(getAbundance() - nbDead);

            this.cohort.nbDeadSs += nbDead;
            if (getAbundance() <= 0) {
                tagForRemoval();
                setAbundance(0);
            }
        }
    }

    public void growth(float minDelta, float maxDelta, float c, float bPower) {
        float previousW = (float) (c * Math.pow(getLength(), bPower));
        //calculation of lengths according to predation efficiency
        if (predSuccessRate >= getCohort().species.criticalPredSuccess) {
            setLength(getLength() + minDelta + (maxDelta - minDelta) * ((predSuccessRate - getCohort().species.criticalPredSuccess) / (1 - getCohort().species.criticalPredSuccess)));
            setWeight((float) (c * Math.pow(getLength(), bPower)));
        }
        setBiomass(((double) getAbundance()) * getWeight() / 1000000.);

        //		updateTL(previousW,weight);
        updateTLbis();
    }

    public void updateTL(float previousW, float W) {
        float previousTL, newTL;
        if (getCohort().ageNbDt != 0) {
            previousTL = getTrophicLevel()[getCohort().ageNbDt - 1];
        } else {
            previousTL = getCohort().species.TLeggs;
        }

        newTL = ((previousW * previousTL) + ((W - previousW) * getTrophicLevel()[getCohort().ageNbDt])) / (W);   // weighting of new TL according to increase of weight dut to prey ingestion
        getTrophicLevel()[getCohort().ageNbDt] = newTL;
    }

    public void updateTLbis() {
        float[] TLproie = new float[4];
        float[] deltaW = new float[4];
        if (getCohort().ageNbDt == 0) {
            deltaW[0] = 0;
            deltaW[1] = 0;
            deltaW[2] = 0;
            deltaW[3] = predSuccessRate;
            TLproie[0] = 0;
            TLproie[1] = 0;
            TLproie[2] = 0;
            TLproie[3] = getTrophicLevel()[getCohort().ageNbDt];
        } else {
            if (getCohort().ageNbDt == 1) {
                deltaW[0] = 0;
                deltaW[1] = 0;
                deltaW[2] = deltaW[3];
                deltaW[3] = predSuccessRate;
                TLproie[0] = 0;
                TLproie[1] = 0;
                TLproie[2] = TLproie[3];
                TLproie[3] = getTrophicLevel()[getCohort().ageNbDt];
            } else {
                if (getCohort().ageNbDt == 2) {
                    deltaW[0] = 0;
                    deltaW[1] = deltaW[2];
                    deltaW[2] = deltaW[3];
                    deltaW[3] = predSuccessRate;
                    TLproie[0] = 0;
                    TLproie[1] = TLproie[2];
                    TLproie[2] = TLproie[3];
                    TLproie[3] = getTrophicLevel()[getCohort().ageNbDt];
                } else {
                    deltaW[0] = deltaW[1];
                    deltaW[1] = deltaW[2];
                    deltaW[2] = deltaW[3];
                    deltaW[3] = predSuccessRate;
                    TLproie[0] = TLproie[1];
                    TLproie[1] = TLproie[2];
                    TLproie[2] = TLproie[3];
                    TLproie[3] = getTrophicLevel()[getCohort().ageNbDt];
                }
            }
        }
        if ((deltaW[3] + deltaW[2] + deltaW[1] + deltaW[0]) != 0) {
            getTrophicLevel()[getCohort().ageNbDt] = (deltaW[3] * TLproie[3] + deltaW[2] * TLproie[2] + deltaW[1] * TLproie[1] + deltaW[0] * TLproie[0]) / (deltaW[3] + deltaW[2] + deltaW[1] + deltaW[0]);
        } else {
            if (getCohort().ageNbDt != 0) {
                getTrophicLevel()[getCohort().ageNbDt] = getTrophicLevel()[getCohort().ageNbDt - 1];
            } else {
                getTrophicLevel()[getCohort().ageNbDt] = getCohort().species.TLeggs;
            }
        }
    }

    public void rankSize(float[] tabSizes, float sizeMax) // for size spectrum output
    {
        int indexMax = tabSizes.length - 1;
        if (getLength() <= sizeMax) {
            while (getLength() < tabSizes[indexMax]) {
                indexMax--;
            }

            //cohort.species.simulation.spectrumAbd[indexMax] += this.abundance;
            //cohort.species.simulation.spectrumBiom[indexMax] += this.biomass;

            //MORGANE 07-2004
            // Size spectrum per species
            cohort.species.simulation.spectrumSpeciesAbd[cohort.species.number - 1][indexMax] += this.getAbundance();
        }
    }

    public void rankTL(float[] tabTL) // for TL distribution output
    {
        if ((getTrophicLevel()[getCohort().ageNbDt] >= 1) && (getBiomass() != 0)) {
            int indexMax = tabTL.length - 1;
            while ((getTrophicLevel()[getCohort().ageNbDt] <= tabTL[indexMax]) && (indexMax > 0)) {
                indexMax--;
            }

            if (getCohort().ageNbDt < getCohort().species.supAgeOfClass0) {
                cohort.species.simulation.distribTL[cohort.species.number - 1][0][indexMax] += this.getBiomass();   // inferior to ageSupAgeClass0
            } else {
                cohort.species.simulation.distribTL[cohort.species.number - 1][1][indexMax] += this.getBiomass();   // superior to ageSupAgeClass0
            }

        }
    }

    /**
     * @return the outOfZoneSchool
     */
    public boolean isOutOfZoneSchool() {
        return outOfZoneSchool;
    }

    /**
     * @param outOfZoneSchool the outOfZoneSchool to set
     */
    public void setOutOfZoneSchool(boolean outOfZoneSchool) {
        this.outOfZoneSchool = outOfZoneSchool;
    }

    /**
     * @return the length
     */
    public float getLength() {
        return length;
    }

    /**
     * @param length the length to set
     */
    public void setLength(float length) {
        this.length = length;
    }

    /**
     * @return the weight
     */
    public float getWeight() {
        return weight;
    }

    /**
     * @param weight the weight to set
     */
    public void setWeight(float weight) {
        this.weight = weight;
    }

    /**
     * @return the trophicLevel
     */
    public float[] getTrophicLevel() {
        return trophicLevel;
    }

    /**
     * @param trophicLevel the trophicLevel to set
     */
    public void setTrophicLevel(float[] trophicLevel) {
        this.trophicLevel = trophicLevel;
    }

    /**
     * @return the abundance
     */
    public long getAbundance() {
        return abundance;
    }

    /**
     * @param abundance the abundance to set
     */
    public void setAbundance(long abundance) {
        this.abundance = abundance;
    }

    /**
     * @return the biomass
     */
    public double getBiomass() {
        return biomass;
    }

    /**
     * @param biomass the biomass to set
     */
    public void setBiomass(double biomass) {
        this.biomass = biomass;
    }

    /**
     * @return the cohort
     */
    public Cohort getCohort() {
        return cohort;
    }

    public void setCohort(Cohort cohort) {
        this.cohort = cohort;
    }

    /**
     * @return whether the shcool will disappear at next time step
     */
    public boolean willDisappear() {
        return disappears;
    }

    /**
     * Tag the school as about to disappear
     */
    public void tagForRemoval() {
        disappears = true;
    }

    /**
     * @return whether the school is catchable for fishing
     */
    public boolean isCatchable() {
        return catchable;
    }

    /**
     * @param sets whether the school is catchable for fishing
     */
    public void setCatchable(boolean catchable) {
        this.catchable = catchable;
    }
}
