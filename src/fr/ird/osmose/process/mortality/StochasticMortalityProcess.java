/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.process.mortality;

import fr.ird.osmose.process.AbstractProcess;
import fr.ird.osmose.Cell;
import fr.ird.osmose.IAggregation;
import fr.ird.osmose.School;
import fr.ird.osmose.Swarm;
import fr.ird.osmose.background.BackgroundSchool;
import fr.ird.osmose.background.BackgroundSpecies;
import fr.ird.osmose.process.bioen.BioenStarvationMortality;
import fr.ird.osmose.process.bioen.BioenPredationMortality;
import fr.ird.osmose.util.XSRandom;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Mortality processes compete stochastically.
 * <ul>
 * <li>It is assumed that every cause compete with each other.</li>
 * <li>Stochasticity and competition within predation process.</li>
 * <li>Asynchronous updating of school biomass (it means biomass are updated on
 * the fly).</li>
 * </ul>
 */
public class StochasticMortalityProcess extends AbstractProcess {

    private boolean newfisheries = false;

    /*
     * Random generator
     */
    private static Random random;
    /*
     * Subdivise the main time step in smaller time steps for applying
     * mortality. Should only be 1 so far, still problems to fix.
     */
    private int subdt;
    /*
     * Private instance of the additional mortality
     */
    private AdditionalMortality additionalMortality;
    /*
     * Private instance of the fishing mortality
     */
    private FishingMortality fishingMortality;
    /*
     * Private instance of the predation mortality
     */
    private PredationMortality predationMortality;

    /* barrier.n: fisheries mortality */
    private FisheriesMortality fisheriesMortality;

    /* barrier.n: bioenergetic starvation mortality */
    private BioenStarvationMortality starvationMortality;

    /* PhV, bioenergetic foraging mortality */
    private ForagingMortality foragingMortality;

    /**
     * The set of plankton swarms
     */
    private HashMap<Integer, List<Swarm>> swarmSet;

    /**
     * The set of background species schools. Structure is (cell index, list of
     * swarms)
     */
    private HashMap<Integer, List<BackgroundSchool>> bkgSet;

    public StochasticMortalityProcess(int rank) {
        super(rank);
    }

    @Override
    public void init() {

        // Possibility to use a seed in the definition of mortality algorithm
        String key = "stochastic.mortality.seed";
        if (getConfiguration().canFind(key)) {
            random = new XSRandom(getConfiguration().getLong(key));
        } else {
            random = new XSRandom(System.nanoTime());
        }

        if (getConfiguration().canFind("fisheries.new.activate")) {
            newfisheries = getConfiguration().getBoolean("fisheries.new.activate");
        }

        additionalMortality = new AdditionalMortality(getRank());
        additionalMortality.init();

        // If not use of bioen, use the traditional predation mort.
        // class. If bioen, use the dedicated class.
        if (!getConfiguration().useBioen()) {
            predationMortality = new PredationMortality(getRank());
            predationMortality.init();
        } else {
            try {
                predationMortality = new BioenPredationMortality(getRank());
                predationMortality.init();
            } catch (IOException ex) {
                Logger.getLogger(StochasticMortalityProcess.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        // Subdt 
        if (!getConfiguration().isNull("mortality.subdt")) {
            subdt = getConfiguration().getInt("mortality.subdt");
        } else {
            subdt = 10;
            warning("Did not find parameter 'mortality.subdt' for stochastic mortality algorithm. Osmose set it to {0}.", subdt);
        }

        // barrier.n: initialisation of fisheries mortality if 
        // the new fisheries are activated
        if (newfisheries) {
            fisheriesMortality = new FisheriesMortality(getRank(), subdt);
            fisheriesMortality.init();
        } else {
            fishingMortality = new FishingMortality(getRank());
            fishingMortality.init();
        }

        // Create a new swarm set, empty at the moment
        swarmSet = new HashMap();

        // Create a new bkg swarm, emty for the moment
        // Structure 
        bkgSet = new HashMap();

        // barrier.n: init the bioenergetic module
        if (this.getConfiguration().useBioen()) {
            // starvation mortality
            starvationMortality = new BioenStarvationMortality(getRank());
            starvationMortality.init();
            // foraging mortality
            foragingMortality = new ForagingMortality(getRank());
            foragingMortality.init();
        }

    }

    @Override
    public void run() {

        // Update fishing process (for MPAs)
        if (!newfisheries) {
            fishingMortality.setMPA();
        }

        // Assess accessibility for this time step
        for (Cell cell : getGrid().getOceanCells()) {
            List<School> schools = getSchoolSet().getSchools(cell);
            if (null == schools) {
                continue;
            }
            // Create the list of preys by gathering the schools and the plankton group
            List<IAggregation> preys = new ArrayList();
            preys.addAll(schools);
            preys.addAll(getSwarms(cell));

            // recovers the list of schools for background species and
            // for the current cell. add this to the list of preys 
            // for the current cell
            preys.addAll(this.getBackgroundSchool(cell));

            // NOTE: at this stage, swarm and bkg biomass is not initialized but 
            // it does not matter: only size is used to define access.
            // Loop over focal schools, which are viewed here as predators.
            // Consider predation over plankton, focal species and background species.
            for (School school : schools) {
                school.setAccessibility(predationMortality.getAccessibility(school, preys));
                school.setPredSuccessRate(0);
                if (school.getAgeDt() == 0) {
                    // Egg loss, not accessible to predation process
                    double D = additionalMortality.getRate(school);
                    double nDead = school.getInstantaneousAbundance() * (1.d - Math.exp(-D));
                    school.incrementNdead(MortalityCause.ADDITIONAL, nDead);
                    school.retainEgg();
                }
            }

            // Loop over background species, which are this time predators.
            for (BackgroundSchool bkg : this.getBackgroundSchool(cell)) {
                bkg.setAccessibility(predationMortality.getAccessibility(bkg, preys));
                bkg.setPredSuccessRate(0);
            }

        } // end of cell loop

        // Update swarms biomass
        int iStepSimu = getSimulation().getIndexTimeSimu();
        for (List<Swarm> swarms : swarmSet.values()) {    // basically a loop over the cells
            for (Swarm swarm : swarms) {    // basically a loop over the LTL classes
                int iltl = swarm.getLTLIndex();
                double accessibleBiom = getConfiguration().getPlankton(iltl).getAccessibility(iStepSimu)
                        * getForcing().getBiomass(iltl, swarm.getCell());
                swarm.setBiomass(accessibleBiom);
            }
        }

        // Update background species biomass
        int istep = getSimulation().getIndexTimeSimu();
        for (List<BackgroundSchool> bkgCell : bkgSet.values()) {    // basically a loop over the cells
            for (BackgroundSchool bkgTmp : bkgCell) {    // basically a loop over all the background species + class
                bkgTmp.setStep(istep);
                bkgTmp.init();
            }
        }

        int[] ncellBatch = dispatchCells();
        int nbatch = ncellBatch.length;
        for (int idt = 0; idt < subdt; idt++) {
            if (!newfisheries) {
                fishingMortality.assessFishableBiomass();
            }
            CountDownLatch doneSignal = new CountDownLatch(nbatch);
            int iStart = 0, iEnd = 0;
            for (int ibatch = 0; ibatch < nbatch; ibatch++) {
                iEnd += ncellBatch[ibatch];
                new Thread(new MortalityWorker(iStart, iEnd, doneSignal)).start();
                iStart += ncellBatch[ibatch];
            }
            try {
                doneSignal.await();
            } catch (InterruptedException ex) {
                error("Multithread mortality process terminated unexpectedly.", ex);
            }
        }
    }

    /**
     * Stochastic mortality algorithm > It is assumed that every cause compete
     * with each other. > Stochasticity and competition within predation
     * process. > Asynchronous updating of school biomass (it means biomass are
     * updated on the fly).
     */
    private void computeMortality(int subdt, Cell cell) {

        List<School> schools = getSchoolSet().getSchools(cell);
        if (null == schools) {
            return;
        }
        int ns = schools.size();

        // Create the list of preys by gathering the schools and the plankton group
        List<IAggregation> preys = new ArrayList();
        preys.addAll(schools);
        for (School prey : schools) {
            // Release some eggs for current subdt (initial abundance / subdt)
            if (prey.getAgeDt() == 0) {
                prey.releaseEgg(subdt);
            }
        }

        preys.addAll(getSwarms(cell));

        // Recover the list of background schools for the current cell
        List<BackgroundSchool> bkgSchool = this.getBackgroundSchool(cell);
        int nBkg = bkgSchool.size();

        // barrier.n: adding background species to the list of possible preys.
        preys.addAll(bkgSchool);

        // preys contains focal species + LTL + bkg species
        // Arrays for loop over schools are initialised with nfocal + nbackgroud
        Integer[] seqPred = new Integer[ns + nBkg];
        for (int i = 0; i < ns + nBkg; i++) {
            seqPred[i] = i;
        }

        Integer[] seqFish = Arrays.copyOf(seqPred, ns + nBkg);
        Integer[] seqNat = Arrays.copyOf(seqPred, ns + nBkg);
        Integer[] seqStarv = Arrays.copyOf(seqPred, ns + nBkg);
        Integer[] seqFor = Arrays.copyOf(seqPred, ns + nBkg);
        MortalityCause[] mortalityCauses = MortalityCause.values();

        // Initialisation of list of predators, which contains both
        // background species and focal species.
        // pred contains focal + bkg species
        ArrayList<IAggregation> listPred = new ArrayList<>();
        listPred.addAll(schools);
        listPred.addAll(bkgSchool);

        shuffleArray(seqPred);
        shuffleArray(seqFish);
        shuffleArray(seqNat);
        shuffleArray(seqStarv);
        shuffleArray(seqFor);

        boolean keepRecord = getSimulation().isPreyRecord();
        for (int i = 0; i < ns + nBkg; i++) {               // loop over all the school (focal and bkg) as predators.
            shuffleArray(mortalityCauses);
            for (MortalityCause cause : mortalityCauses) {   // random loop over all the mortality causes
                School school;
                double nDead = 0;
                switch (cause) {

                    // barrier.n: adding the 
                    case FORAGING:
                        if ((seqFor[i] >= ns) || (!getConfiguration().useBioen())) {
                            // foraging mortality for bion module and focal species only
                            break;
                        }
                        school = schools.get(seqFor[i]);
                        // foraging mortality rate at current sub time step                      
                        double Mo = foragingMortality.getRate(school) / subdt;
                        // during the egg stage, there is no foraging, and then no foraging mortality                     
                        if (school.getAgeDt() > 0) {
                        if (Mo > 0.d) {
                            nDead = school.getInstantaneousAbundance() * (1.d - Math.exp(-Mo));
                            school.incrementNdead(MortalityCause.FORAGING, nDead);
                        }
                        }
                        break;
                    case PREDATION:
                        // Predation mortality
                        IAggregation predator = listPred.get(seqPred[i]);   // recover one predator (background or focal species)
                        // compute predation from predator to all the possible preys
                        // preyUpon is the total biomass easten by predator
                        double[] preyUpon = predationMortality.computePredation(predator, preys, predator.getAccessibility(), subdt);
                        for (int ipr = 0; ipr < preys.size(); ipr++) {
                            if (preyUpon[ipr] > 0) {
                                // Loop over all the preys. If they are eaten by the predator,
                                // the biomass of the prey is updted
                                IAggregation prey = preys.get(ipr);
                                nDead = prey.biom2abd(preyUpon[ipr]);   // total biomass that has been eaten
                                prey.incrementNdead(MortalityCause.PREDATION, nDead);
                                predator.preyedUpon(prey.getSpeciesIndex(), prey.getTrophicLevel(), prey.getAge(), prey.getLength(), preyUpon[ipr], keepRecord);
                            }
                        }
                        break;
                    case STARVATION:

                        if (seqStarv[i] >= ns) {
                            break;   // if background school, nothing is done
                        }

                        school = schools.get(seqStarv[i]);
                        nDead = 0.d;
                        if (!this.getConfiguration().useBioen()) {
                            // Starvation mortality when no use of bioen module.                           
                            double M = school.getStarvationRate() / subdt;
                            nDead = school.getInstantaneousAbundance() * (1.d - Math.exp(-M));
                        } else if (school.getAgeDt() > 0) {
                            // computation of the starvation mortality
                            // which is updated directly from the BioenMortality class.
                            // computes starv.mortality only for species greater than 0 years old
                            nDead = starvationMortality.computeStarvation(school, subdt);
                        }
                        if (nDead > 0.d) {
                            school.incrementNdead(MortalityCause.STARVATION, nDead);
                        }

                        break;
                    case ADDITIONAL:
                        if (seqNat[i] >= ns) {
                            break;    // if background school, nothing is done
                        }
                        // Additional mortality
                        school = schools.get(seqNat[i]);
                        // Egg mortality is handled separately and beforehand, 
                        // assuming that the egg loss is not available to predation
                        // and thus these mortality causes should not compete
                        if (school.getAgeDt() > 0) {
                            double D = additionalMortality.getRate(school) / subdt;
                            nDead = school.getInstantaneousAbundance() * (1.d - Math.exp(-D));
                            school.incrementNdead(MortalityCause.ADDITIONAL, nDead);
                        }
                        break;
                    case FISHING:

                        // Possibility to fish background species?????
                        if (seqFish[i] >= ns) {
                            break;    // if background school, nothing is done
                        }
                        // recovers the current school
                        school = schools.get(seqFish[i]);

                        // Fishing mortality: if new fisheries are activated.
                        if (newfisheries) {
                            // If the new fisheries are activated, we compute the mortality rate 
                            // it returns nothing
                            this.fisheriesMortality.getRate(school);

                        } else {

                            // Fishing Mortality
                            switch (fishingMortality.getType(school.getSpeciesIndex())) {
                                case RATE:
                                    double F = fishingMortality.getRate(school) / subdt;
                                    nDead = school.getInstantaneousAbundance() * (1.d - Math.exp(-F));
                                    break;
                                case CATCHES:
                                    nDead = school.biom2abd(fishingMortality.getCatches(school) / subdt);
                                    break;
                            }
                            school.incrementNdead(MortalityCause.FISHING, nDead);
                            break;
                        }
                }  // end of switch (cause
            }   // end of mort cause loop
        }   // end of school loop species loop
    }    // end of function

    private List<Swarm> getSwarms(Cell cell) {
        if (!swarmSet.containsKey(cell.getIndex())) {
            List<Swarm> swarms = new ArrayList();
            for (int iLTL = 0; iLTL < getConfiguration().getNPlankton(); iLTL++) {
                swarms.add(new Swarm(getConfiguration().getPlankton(iLTL), cell));
            }
            swarmSet.put(cell.getIndex(), swarms);
        }
        return swarmSet.get(cell.getIndex());
    }

    /**
     * Shuffles an input array.
     *
     * @param <T> Input array
     */
    public static <T> void shuffleArray(T[] a) {
        // Shuffle array
        for (int i = a.length; i > 1; i--) {
            T tmp = a[i - 1];
            int j = random.nextInt(i);
            a[i - 1] = a[j];
            a[j] = tmp;
        }
    }

    /**
     * Split the ocean cells in batches that will run on concurrent threads.
     * Distribute them evenly considering number of schools per batches of ocean
     * cells.
     *
     * @return integer array, the number of ocean cells for every batch
     */
    private int[] dispatchCells() {

        // number of school in a batch
        int nschoolBatch = 0;
        // number of procs available for multithreading
        int ncpu = Math.max(1, getConfiguration().getNCpu() / getConfiguration().getNSimulation());
        // number of schools to be handled by each proc
        int nschoolPerCPU = getSchoolSet().getSchools().size() / ncpu;
        // array of number of cells in every batch
        int[] ncellBatch = new int[ncpu];
        // index of current batch [0, ncpu - 1]
        int ibatch = 0;
        for (Cell cell : getGrid().getOceanCells()) {
            // number of schools in current cell
            List<School> schools = getSchoolSet().getSchools(cell);
            int nschoolCell = (null == schools) ? 0 : schools.size();
            // check whether the batch reaches expected number of schools
            if (nschoolBatch + nschoolCell > nschoolPerCPU) {
                // current batch reached expected number of schools
                // check whether the batch with or without current cell is
                // closer to average number of schools per CPU
                if (nschoolBatch + nschoolCell - nschoolPerCPU > nschoolPerCPU - nschoolBatch) {
                    // batch without current cell is closer to nschoolPerCPU, so
                    // schools of current cell go to next batch.
                    // current cell counts as 1st cell of next batch
                    ncellBatch[Math.min(ibatch + 1, ncpu - 1)] += 1;
                    // schools of current cell go to next batch
                    nschoolBatch = nschoolCell;
                } else {
                    // current cell is attached to current batch
                    // set final number of ocean cells in current batch
                    ncellBatch[ibatch] += 1;
                    nschoolBatch = 0;
                }
                // increment batch index
                ibatch = Math.min(ibatch + 1, ncpu - 1);
            } else {
                // current batch not full yet
                // increment number of schools in current batch
                nschoolBatch += nschoolCell;
                ncellBatch[ibatch] += 1;
            }
        }

        //        debug("Dispatch Ocean Cells over CPUs");
        //        debug("  Total number of schools " + getSchoolSet().getSchools().size());
        //        debug("  Average number of schools per CPU " + nschoolPerCPU);
        //        int iStart = 0, iEnd = 0;
        //        List<Cell> cells = getGrid().getOceanCells();
        //        int ntot = 0;
        //        for (ibatch = 0; ibatch < ncpu; ibatch++) {
        //            iEnd += ncellBatch[ibatch];
        //            int n = 0;
        //            for (int i = iStart; i < iEnd; i++) {
        //                List<School> schools = getSchoolSet().getSchools(cells.get(i));
        //                n += (null != schools) ? schools.size() : 0;
        //            }
        //            ntot += n;
        //            iStart += ncellBatch[ibatch];
        //            debug("  CPU" + ibatch + ", number of ocean cells "+ ncellBatch[ibatch] + ", number of schools " + n);
        //        }
        //        assert iEnd == cells.size();
        //        assert ntot == getSchoolSet().getSchools().size();
        return ncellBatch;
    }

    /**
     * Implementation of the Fork/Join algorithm for splitting the set of cells
     * in several subsets.
     */
    private class MortalityWorker implements Runnable {

        private final int iStart, iEnd;
        /**
         * The {@link java.util.concurrent.CountDownLatch} that will wait for
         * this {@link Simulation} to complete before decrementing the count of
         * the latch.
         */
        private final CountDownLatch doneSignal;

        /**
         * Creates a new {@code ForkStep} that will handle a subset of cells.
         *
         * @param iStart, index of the first cell of the subset
         * @param iEnd , index of the last cell of the subset
         * @param doneSignal, the CountDownLatch object
         */
        MortalityWorker(int iStart, int iEnd, CountDownLatch doneSignal) {
            this.iStart = iStart;
            this.iEnd = iEnd;
            this.doneSignal = doneSignal;
        }

        /**
         * Loop over the subset of cells and apply the
         * {@link fr.ird.osmose.process.mortality.StochasticMortalityProcess#computeMortality(int, fr.ird.osmose.Cell)}
         * function.
         */
        @Override
        public void run() {
            try {
                List<Cell> cells = getGrid().getOceanCells();
                for (int iCell = iStart; iCell < iEnd; iCell++) {
                    computeMortality(subdt, cells.get(iCell));
                }
            } finally {
                doneSignal.countDown();
            }
        }
    }

    /**
     * Recovers the list of background schools for the current cell. If the
     * current cell does not contain any background school yet, they are added.
     * This is the same as for the getSwarms method.
     *
     * @param cell
     * @return
     */
    private List<BackgroundSchool> getBackgroundSchool(Cell cell) {

        if (!bkgSet.containsKey(cell.getIndex())) {
            // If the cell does not contain any background school
            // initialisation of a list of cells.
            List<BackgroundSchool> output = new ArrayList<>();

            // Loop over all the background species
            for (int iBkg = 0; iBkg < getConfiguration().getNBkgSpecies(); iBkg++) {

                BackgroundSpecies bkgSpec = getConfiguration().getBkgSpecies(iBkg);

                // Loop over all the classes of the background species.
                for (int iClass = 0; iClass < bkgSpec.getTimeSeries().getNClass(); iClass++) {

                    // Init a background school of species bkgSpec and of class iClass
                    BackgroundSchool BkgSchTmp = new BackgroundSchool(bkgSpec, iClass);
                    // Move the bkg school to cell (set x and y)
                    BkgSchTmp.moveToCell(cell);
                    // add to output
                    output.add(BkgSchTmp);

                }   // end of iClass loop
            }   // end of bkg loop

            // add the list to the hash map
            bkgSet.put(cell.getIndex(), output);

        }   // end of contains test

        return bkgSet.get(cell.getIndex());

    }   // end of function

}
