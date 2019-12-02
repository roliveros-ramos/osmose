/* 
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * Laure VELEZ (laure.velez@ird.fr)
 * Nicolas Barrier (nicolas.barrier@ird.fr)
 * 
 * This software is a computer program whose purpose is to simulate fish
 * populations and their interactions with their biotic and abiotic environment.
 * OSMOSE is a spatial, multispecies and individual-based model which assumes
 * size-based opportunistic predation based on spatio-temporal co-occurrence
 * and size adequacy between a predator and its prey. It represents fish
 * individuals grouped into schools, which are characterized by their size,
 * weight, age, taxonomy and geographical location, and which undergo major
 * processes of fish life cycle (growth, explicit predation, natural and
 * starvation mortalities, reproduction and migration) and fishing mortalities
 * (Shin and Cury 2001, 2004).
 * 
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". 
 * 
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability. 
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or 
 * data to be ensured and,  more generally, to use and operate it in the 
 * same conditions as regards security. 
 * 
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 */
package fr.ird.osmose.output;

import fr.ird.osmose.output.distribution.AbstractDistribution;
import fr.ird.osmose.output.distribution.AgeDistribution;
import fr.ird.osmose.output.distribution.SizeDistribution;
import fr.ird.osmose.output.distribution.TLDistribution;
import fr.ird.osmose.util.io.IOTools;
import fr.ird.osmose.util.SimulationLinker;
import java.util.ArrayList;
import java.util.List;
import fr.ird.osmose.output.spatial.SpatialAbundanceOutput;
import fr.ird.osmose.output.spatial.SpatialBiomassOutput;
import fr.ird.osmose.output.spatial.SpatialYieldOutput;
import fr.ird.osmose.output.spatial.SpatialYieldNOutput;
import fr.ird.osmose.output.spatial.SpatialTLOutput;
import fr.ird.osmose.output.spatial.SpatialSizeOutput;
import fr.ird.osmose.process.mortality.MortalityCause;

/**
 *
 * @author pverley
 */
public class OutputManager extends SimulationLinker {

    // List of the indicators
    final private List<IOutput> outputs;
    /**
     * Object that is able to take a snapshot of the set of schools and write it
     * in a NetCDF file. Osmose will be able to restart on such a file.
     */
    final private SchoolSetSnapshot snapshot;
    /**
     * Record frequency for writing restart files, in number of time step.
     */
    private int restartFrequency;
    /**
     * Whether the restart files should be written or not
     */
    private boolean writeRestart;
    /**
     * Number of years before writing restart files.
     */
    private int spinupRestart;
    /**
     * Whether first age class is discarded or not from output.
     */
    private boolean cutoff;
    /**
     * Threshold age (year) for age class zero. This parameter allows to discard
     * schools younger that this threshold in the calculation of the indicators
     * when parameter <i>output.cutoff.enabled</i> is set to {@code true}.
     * Parameter <i>output.cutoff.age.sp#</i>
     */
    private float[] cutoffAge;

    public OutputManager(int rank) {
        super(rank);
        outputs = new ArrayList();
        snapshot = new SchoolSetSnapshot(rank);
    }

    public void init() {

        int rank = getRank();
        /*
         * Delete existing outputs from previous simulation
         */
        if (!getSimulation().isRestart()) {
            // Delete previous simulation of the same name
            String pattern = getConfiguration().getString("output.file.prefix") + "*_Simu" + rank + "*";
            IOTools.deleteRecursively(getConfiguration().getOutputPathname(), pattern);
        }

        AbstractDistribution sizeDistrib = new SizeDistribution();
        sizeDistrib.init();
        AbstractDistribution ageDistrib = new AgeDistribution();
        ageDistrib.init();
        AbstractDistribution tl_distrib = new TLDistribution();
        tl_distrib.init();

        cutoff = getConfiguration().getBoolean("output.cutoff.enabled");
        cutoffAge = new float[getNSpecies()];
        if (cutoff) {
            for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
                cutoffAge[iSpec] = getConfiguration().getFloat("output.cutoff.age.sp" + iSpec);
            }
        }

        if (getConfiguration().getBoolean("output.abundance.netcdf.enabled")) {
            outputs.add(new AbundanceOutput_Netcdf(rank));
        }

        if (getConfiguration().getBoolean("output.biomass.netcdf.enabled")) {
            outputs.add(new BiomassOutput_Netcdf(rank));
        }

        if (getConfiguration().getBoolean("output.yield.biomass.netcdf.enabled")) {
            outputs.add(new YieldOutput_Netcdf(rank));
        }

        if (getConfiguration().getBoolean("output.yieldN.netcdf.enabled")) {
            outputs.add(new YieldNOutput_Netcdf(rank));
        }

        if (getConfiguration().getBoolean("output.biomass.bysize.netcdf.enabled")) {
            outputs.add(new BiomassDistribOutput_Netcdf(rank, sizeDistrib));
        }

        if (getConfiguration().getBoolean("output.biomass.bytl.netcdf.enabled")) {
            outputs.add(new BiomassDistribOutput_Netcdf(rank, tl_distrib));
        }

        if (getConfiguration().getBoolean("output.biomass.byage.netcdf.enabled")) {
            outputs.add(new BiomassDistribOutput_Netcdf(rank, ageDistrib));
        }

        if (getConfiguration().getBoolean("output.abundance.bysize.netcdf.enabled")) {
            outputs.add(new AbundanceDistribOutput_Netcdf(rank, sizeDistrib));
        }

        if (getConfiguration().getBoolean("output.abundance.byage.netcdf.enabled")) {
            outputs.add(new AbundanceDistribOutput_Netcdf(rank, ageDistrib));
        }

        if (getConfiguration().getBoolean("output.diet.pressure.netcdf.enabled")) {
            outputs.add(new BiomassDietStageOutput_Netcdf(rank));
        }

        if (getConfiguration().getBoolean("output.diet.composition.netcdf.enabled")) {
            getSimulation().requestPreyRecord();
            outputs.add(new DietOutput_Netcdf(rank));
        }

        if (getConfiguration().getBoolean("output.mortality.perSpecies.byage.netcdf.enabled")) {
            for (int i = 0; i < getNSpecies(); i++) {
                outputs.add(new MortalitySpeciesOutput_Netcdf(rank, getSpecies(i), ageDistrib));
            }
        }

        if (getConfiguration().getBoolean("output.diet.composition.byage.netcdf.enabled")) {
            getSimulation().requestPreyRecord();
            for (int i = 0; i < getNSpecies(); i++) {
                outputs.add(new DietDistribOutput_Netcdf(rank, getSpecies(i), ageDistrib));
            }
        }

        if (getConfiguration().getBoolean("output.diet.composition.bysize.netcdf.enabled")) {
            getSimulation().requestPreyRecord();
            for (int i = 0; i < getNSpecies(); i++) {
                outputs.add(new DietDistribOutput_Netcdf(rank, getSpecies(i), sizeDistrib));
            }
        }

        if (getConfiguration().getBoolean("output.yield.bySize.netcdf.enabled")) {
            outputs.add(new YieldDistribOutput_Netcdf(rank, sizeDistrib));
        }

        if (getConfiguration().getBoolean("output.yield.byage.netcdf.enabled")) {
            outputs.add(new YieldDistribOutput_Netcdf(rank, ageDistrib));
        }

        if (getConfiguration().getBoolean("output.yieldN.bySize.netcdf.enabled")) {
            outputs.add(new YieldNDistribOutput_Netcdf(rank, sizeDistrib));
        }

        if (getConfiguration().getBoolean("output.yieldN.byage.netcdf.enabled")) {
            outputs.add(new YieldNDistribOutput_Netcdf(rank, ageDistrib));
        }

        if (getConfiguration().getBoolean("output.meanSize.byAge.netcdf.enabled")) {
            outputs.add(new MeanSizeDistribOutput_Netcdf(rank, ageDistrib));
        }

        if (getConfiguration().getBoolean("output.diet.pressure.netcdf.enabled")) {
            getSimulation().requestPreyRecord();
            outputs.add(new PredatorPressureOutput_Netcdf(rank));
        }

        /*
         * Instantiate indicators
         */
        if (getConfiguration().getBoolean("output.spatialabundance.enabled")) {
            outputs.add(new SpatialAbundanceOutput(rank));
        }

        if (getConfiguration().getBoolean("output.spatialbiomass.enabled")) {
            outputs.add(new SpatialBiomassOutput(rank));
        }

        if (getConfiguration().getBoolean("output.spatialsize.enabled")) {
            outputs.add(new SpatialSizeOutput(rank));
        }

        if (getConfiguration().getBoolean("output.spatialyield.enabled")) {
            outputs.add(new SpatialYieldOutput(rank));
        }

        if (getConfiguration().getBoolean("output.spatialyieldN.enabled")) {
            outputs.add(new SpatialYieldNOutput(rank));
        }

        if (getConfiguration().getBoolean("output.spatialtl.enabled")) {
            outputs.add(new SpatialTLOutput(rank));
        }

        // Barrier.n: Saving of spatial, class (age or size) structure abundance
        if (getConfiguration().getBoolean("output.spatialsizespecies.enabled")) {
            outputs.add(new SpatialSizeSpeciesOutput(rank, sizeDistrib));
        }
        if (getConfiguration().getBoolean("output.spatialagespecies.enabled")) {
            outputs.add(new SpatialSizeSpeciesOutput(rank, ageDistrib));
        }
        // Fisheries output
        if (getConfiguration().isFisheryEnabled() && getConfiguration().getBoolean("output.fishery.enabled")) {
            outputs.add(new FisheryOutput(rank));
        }
        // Biomass
        if (getConfiguration().getBoolean("output.biomass.enabled")) {
            outputs.add(new SpeciesOutput(rank, null, "biomass",
                    "Mean biomass (tons), " + (cutoff ? "excluding" : "including") + " first ages specified in input",
                    (school) -> school.getInstantaneousBiomass(),
                    true)
            );
        }
        if (getConfiguration().getBoolean("output.biomass.bysize.enabled")) {
            outputs.add(new DistribOutput(rank, "Indicators", "biomass",
                    "Distribution of fish species biomass (tonne)",
                    school -> school.getInstantaneousBiomass(),
                    sizeDistrib
            ));
        }
        if (getConfiguration().getBoolean("output.biomass.byage.enabled")) {
            outputs.add(new DistribOutput(rank, "Indicators", "biomass",
                    "Distribution of fish species biomass (tonne)",
                    school -> school.getInstantaneousBiomass(),
                    ageDistrib
            ));
        }
        // Abundance
        if (getConfiguration().getBoolean("output.abundance.enabled")) {
            outputs.add(new SpeciesOutput(rank, null, "abundance",
                    "Mean abundance (number of fish), " + (cutoff ? "excluding" : "including") + " first ages specified in input",
                    (school) -> school.getInstantaneousAbundance(),
                    true)
            );
        }
        if (getConfiguration().getBoolean("output.abundance.bysize.enabled")) {
            outputs.add(new DistribOutput(rank, "Indicators", "abundance",
                    "Distribution of fish abundance (number of fish)",
                    school -> school.getInstantaneousAbundance(),
                    sizeDistrib
            ));
        }
        if (getConfiguration().getBoolean("output.abundance.byage.enabled")) {
            outputs.add(new DistribOutput(rank, "Indicators", "abundance",
                    "Distribution of fish abundance (number of fish)",
                    school -> school.getInstantaneousAbundance(),
                    ageDistrib
            ));
        }
        if (getConfiguration().getBoolean("output.abundance.bytl.enabled")) {
            outputs.add(new DistribOutput(rank, "Indicators", "abundance",
                    "Distribution of fish abundance (number of fish)",
                    school -> school.getInstantaneousAbundance(),
                    tl_distrib
            ));
        }
        // Mortality
        if (getConfiguration().getBoolean("output.mortality.enabled")) {
            outputs.add(new MortalityOutput(rank));
        }
        if (getConfiguration().getBoolean("output.mortality.perSpecies.byage.enabled")) {
            for (int i = 0; i < getNSpecies(); i++) {
                outputs.add(new MortalitySpeciesOutput(rank, getSpecies(i), ageDistrib));
            }
        }
        // phv 20150413, it should be size distribution at the beginning of the
        // time step. To be fixed
        if (getConfiguration().getBoolean("output.mortality.perSpecies.bysize.enabled")) {
            for (int i = 0; i < getNSpecies(); i++) {
                outputs.add(new MortalitySpeciesOutput(rank, getSpecies(i), sizeDistrib));
            }
        }
        if (getConfiguration().getBoolean("output.mortality.additional.bySize.enabled")
                || getConfiguration().getBoolean("output.mortality.additional.byAge.enabled")) {
            outputs.add(new DistribOutput(rank, "Indicators", "additionalMortality",
                    "Distribution of additional mortality biomass (tonne of fish dead from unexplicited cause per time step of saving)",
                    school -> school.abd2biom(school.getNdead(MortalityCause.ADDITIONAL)),
                    sizeDistrib
            ));
        }
        if (getConfiguration().getBoolean("output.mortality.additional.byAge.enabled")) {
            outputs.add(new DistribOutput(rank, "Indicators", "additionalMortality",
                    "Distribution of additional mortality biomass (tonne of fish dead from unexplicited cause per time step of saving)",
                    school -> school.abd2biom(school.getNdead(MortalityCause.ADDITIONAL)),
                    ageDistrib
            ));
        }
        if (getConfiguration().getBoolean("output.mortality.additionalN.bySize.enabled")) {
            outputs.add(new DistribOutput(rank, "Indicators", "additionalMortalityN",
                    "Distribution of additional mortality biomass (number of fish dead from unexplicited cause per time step of saving)",
                    school -> school.getNdead(MortalityCause.ADDITIONAL),
                    sizeDistrib
            ));
        }
        if (getConfiguration().getBoolean("output.mortality.additionalN.byAge.enabled")) {
            outputs.add(new DistribOutput(rank, "Indicators", "additionalMortalityN",
                    "Distribution of additional mortality biomass (number of fish dead from unexplicited cause per time step of saving)",
                    school -> school.getNdead(MortalityCause.ADDITIONAL),
                    ageDistrib
            ));
        }
        // Yield
        if (getConfiguration().getBoolean("output.yield.biomass.enabled")) {
            outputs.add(new SpeciesOutput(rank, null, "yield",
                    "cumulative catch (tons per time step of saving). ex: if time step of saving is the year, then annual catches are saved",
                    school -> school.abd2biom(school.getNdead(MortalityCause.FISHING)),
                    true)
            );
        }
        if (getConfiguration().getBoolean("output.yieldN.enabled")) {
            outputs.add(new SpeciesOutput(rank, null, "yieldN",
                    "cumulative catch (number of fish caught per time step of saving). ex: if time step of saving is the year, then annual catches in fish numbers are saved",
                    school -> school.getNdead(MortalityCause.FISHING),
                    true)
            );
        }
        // Size
        if (getConfiguration().getBoolean("output.size.enabled")) {
            outputs.add(new WeightedSpeciesOutput(rank, "SizeIndicators", "meanSize",
                    "Mean size of fish species in cm, weighted by fish numbers, and " + (cutoff ? "excluding" : "including") + " first ages specified in input",
                    school -> school.getAge() >= cutoffAge[school.getSpeciesIndex()],
                    school -> school.getLength(),
                    school -> school.getInstantaneousAbundance()
            ));
        }
        // Size
        if (getConfiguration().getBoolean("output.weight.enabled")) {
            outputs.add(new WeightedSpeciesOutput(rank, "SizeIndicators", "meanWeight",
                    "Mean weight of fish species in kilogram, weighted by fish numbers, and " + (cutoff ? "excluding" : "including") + " first ages specified in input",
                    school -> school.getAge() >= cutoffAge[school.getSpeciesIndex()],
                    school -> 1E-3 * school.getWeight(),
                    school -> school.getInstantaneousAbundance()
            ));
        }

        if (getConfiguration().getBoolean("output.size.catch.enabled")) {
            outputs.add(new WeightedSpeciesOutput(rank, "SizeIndicators", "meanSizeCatch",
                    "Mean size of fish species in cm, weighted by fish numbers in the catches, and including first ages specified in input.",
                    school -> school.getLength(),
                    school -> school.getNdead(MortalityCause.FISHING)
            ));
        }
        if (getConfiguration().getBoolean("output.yieldN.bySize.enabled")) {
            outputs.add(new DistribOutput(rank, "Indicators", "yieldN",
                    "Distribution of cumulative catch (number of fish per time step of saving)",
                    school -> school.getNdead(MortalityCause.FISHING),
                    sizeDistrib
            ));
        }
        if (getConfiguration().getBoolean("output.yield.bySize.enabled")) {
            outputs.add(new DistribOutput(rank, "Indicators", "yield",
                    "Distribution of cumulative catch (tonne per time step of saving)",
                    school -> school.abd2biom(school.getNdead(MortalityCause.FISHING)),
                    sizeDistrib
            ));
        }
        if (getConfiguration().getBoolean("output.meanSize.byAge.enabled")) {
            outputs.add(new WeightedDistribOutput(
                    rank, "Indicators", "meanSize",
                    "Mean size of fish (centimeter)",
                    school -> school.getInstantaneousAbundance() * school.getLength(),
                    school -> school.getInstantaneousAbundance(),
                    ageDistrib
            ));
        }
        // Age
        if (getConfiguration().getBoolean("output.yieldN.byAge.enabled")) {
            outputs.add(new DistribOutput(rank, "Indicators", "yieldN",
                    "Distribution of cumulative catch (number of fish per time step of saving)",
                    school -> school.getNdead(MortalityCause.FISHING),
                    ageDistrib
            ));
        }
        if (getConfiguration().getBoolean("output.yield.byAge.enabled")) {
            outputs.add(new DistribOutput(rank, "Indicators", "yield",
                    "Distribution of cumulative catch (tonne per time step of saving)",
                    school -> school.abd2biom(school.getNdead(MortalityCause.FISHING)),
                    ageDistrib
            ));
        }
        // TL
        if (getConfiguration().getBoolean("output.tl.enabled")) {
            getSimulation().requestPreyRecord();
            outputs.add(new WeightedSpeciesOutput(rank, "Trophic", "meanTL",
                    "Mean Trophic Level of fish species, weighted by fish biomass, and " + (cutoff ? "excluding" : "including") + " first ages specified in input",
                    school -> school.getAge() >= cutoffAge[school.getSpeciesIndex()],
                    school -> school.getTrophicLevel(),
                    school -> school.getInstantaneousBiomass()
            ));
        }
        if (getConfiguration().getBoolean("output.tl.catch.enabled")) {
            getSimulation().requestPreyRecord();
            outputs.add(new WeightedSpeciesOutput(rank, "Trophic", "meanTLCatch",
                    "Mean Trophic Level of fish species, weighted by fish catch, and including first ages specified in input",
                    school -> school.getTrophicLevel(),
                    school -> school.abd2biom(school.getNdead(MortalityCause.FISHING))
            ));
        }
        if (getConfiguration().getBoolean("output.biomass.bytl.enabled")) {
            outputs.add(new DistribOutput(rank, "Indicators", "biomass",
                    "Distribution of fish biomass (tonne)",
                    school -> school.getInstantaneousBiomass(),
                    tl_distrib
            ));
        }
        if (getConfiguration().getBoolean("output.meanTL.bySize.enabled")) {
            getSimulation().requestPreyRecord();
            outputs.add(new WeightedDistribOutput(
                    rank, "Trophic", "meanTL",
                    "Mean trophic level of fish species",
                    school -> school.getInstantaneousBiomass() * school.getTrophicLevel(),
                    school -> school.getInstantaneousBiomass(),
                    sizeDistrib
            ));
        }
        if (getConfiguration().getBoolean("output.meanTL.byAge.enabled")) {
            getSimulation().requestPreyRecord();
            outputs.add(new WeightedDistribOutput(
                    rank, "Trophic", "meanTL",
                    "Mean trophic level of fish species",
                    school -> school.getInstantaneousBiomass() * school.getTrophicLevel(),
                    school -> school.getInstantaneousBiomass(),
                    ageDistrib
            ));
        }
        // Predation
        if (getConfiguration().getBoolean("output.diet.composition.enabled")) {
            getSimulation().requestPreyRecord();
            outputs.add(new DietOutput(rank));
        }
        if (getConfiguration().getBoolean("output.diet.composition.byage.enabled")) {
            getSimulation().requestPreyRecord();
            for (int i = 0; i < getNSpecies(); i++) {
                outputs.add(new DietDistribOutput(rank, getSpecies(i), ageDistrib));
            }
        }
        if (getConfiguration().getBoolean("output.diet.composition.bysize.enabled")) {
            getSimulation().requestPreyRecord();
            for (int i = 0; i < getNSpecies(); i++) {
                outputs.add(new DietDistribOutput(rank, getSpecies(i), sizeDistrib));
            }
        }
        if (getConfiguration().getBoolean("output.diet.pressure.enabled")) {
            getSimulation().requestPreyRecord();
            outputs.add(new PredatorPressureOutput(rank));
        }
        if (getConfiguration().getBoolean("output.diet.pressure.enabled")) {
            outputs.add(new BiomassDietStageOutput(rank));
        }
        if (getConfiguration().getBoolean("output.diet.pressure.byage.enabled")) {
            getSimulation().requestPreyRecord();
            for (int i = 0; i < getNSpecies(); i++) {
                outputs.add(new PredatorPressureDistribOutput(rank, getSpecies(i), ageDistrib));
            }
        }
        if (getConfiguration().getBoolean("output.diet.pressure.bysize.enabled")) {
            getSimulation().requestPreyRecord();
            for (int i = 0; i < getNSpecies(); i++) {
                outputs.add(new PredatorPressureDistribOutput(rank, getSpecies(i), sizeDistrib));
            }
        }
        if (getConfiguration().getBoolean("output.diet.success.enabled")) {
            outputs.add(new WeightedSpeciesOutput(rank, "Trophic", "predationSuccess",
                    "Predation success rate per species",
                    school -> school.getPredSuccessRate(),
                    nschool -> 1.d
            ));
        }
        // Spatialized
        if (getConfiguration().getBoolean("output.spatial.enabled")) {
            outputs.add(new SpatialOutput(rank));
        }
        if (getConfiguration().getBoolean("output.spatial.ltl.enabled")) {
            getSimulation().requestPreyRecord();
            outputs.add(new ResourceOutput(rank));
        }

        // Debugging outputs
        boolean NO_WARNING = false;
        if (getConfiguration().getBoolean("output.ssb.enabled", NO_WARNING)) {
            outputs.add(new SpeciesOutput(rank, null, "SSB",
                    "Spawning Stock Biomass (tonne)",
                    school -> school.getSpecies().isSexuallyMature(school) ? school.getInstantaneousBiomass() : 0.d,
                    false)
            );
        }
        if (getConfiguration().getBoolean("output.nschool.enabled", NO_WARNING)) {
            outputs.add(new NSchoolOutput(rank));
        }
        if (getConfiguration().getBoolean("output.nschool.byage.enabled", NO_WARNING)) {
            outputs.add(new NSchoolDistribOutput(rank, ageDistrib));
        }

        if (getConfiguration().getBoolean("output.nschool.bysize.enabled", NO_WARNING)) {
            outputs.add(new NSchoolDistribOutput(rank, sizeDistrib));
        }

        if (getConfiguration().getBoolean("output.ndeadschool.enabled", NO_WARNING)) {
            outputs.add(new NDeadSchoolOutput(rank));
        }

        if (getConfiguration().getBoolean("output.ndeadschool.byage.enabled", NO_WARNING)) {
            outputs.add(new NDeadSchoolDistribOutput(rank, ageDistrib));
        }

        if (getConfiguration().getBoolean("output.ndeadschool.bysize.enabled", NO_WARNING)) {
            outputs.add(new NDeadSchoolDistribOutput(rank, sizeDistrib));
        }

        if (getConfiguration().isBioenEnabled()) {

            if (getConfiguration().getBoolean("output.bioen.maturesize.enabled", NO_WARNING)) {
                outputs.add(new WeightedSpeciesOutput(rank, "Bioen", "sizeMature",
                        "Size at maturity (centimeter)",
                        school -> school.isMature(),
                        school -> school.getSizeMat(),
                        school -> school.getInstantaneousAbundance()
                ));
            }

            if (getConfiguration().getBoolean("output.bioen.matureage.enabled", NO_WARNING)) {
                outputs.add(new WeightedSpeciesOutput(rank, "Bioen", "ageMature",
                        "Age at maturity (year)",
                        school -> school.isMature(),
                        school -> school.getAgeMat(),
                        school -> school.getInstantaneousAbundance()
                ));
            }

            if (getConfiguration().getBoolean("output.bioen.ingest.enabled", NO_WARNING)) {
                outputs.add(new WeightedSpeciesOutput(rank, "Bioen", "ingestion",
                        "Ingestion rate (grams.grams^-alpha)",
                        school -> school.getEGross() / school.getInstantaneousAbundance() * 1e6f / (Math.pow(school.getWeight() * 1e6f, school.getAlphaBioen())),
                        nshool -> 1.d
                ));
            }

            if (getConfiguration().getBoolean("output.bioen.maint.enabled", NO_WARNING)) {
                outputs.add(new WeightedSpeciesOutput(rank, "Bioen", "maintenance",
                        "Maintenance rate (grams.grams^-alpha)",
                        school -> school.getEMaint() / school.getInstantaneousAbundance() * 1e6f / (Math.pow(school.getWeight() * 1e6f, school.getAlphaBioen())),
                        nshool -> 1.d
                ));
            }

            if (getConfiguration().getBoolean("output.bioen.growthpot.enabled", NO_WARNING)) {
                outputs.add(new WeightedSpeciesOutput(rank, "Bioen", "potentialGrowthRate",
                        "Potential net growth rate (grams.grams^-alpha) (grams net usable per gram of predator)",
                        school -> school.getENet() / school.getInstantaneousAbundance() * 1e6f / (Math.pow(school.getWeight() * 1e6f, school.getAlphaBioen())),
                        nshool -> 1.d
                ));
            }

            if (getConfiguration().getBoolean("output.bioen.sizeInf.enabled", NO_WARNING)) {
                outputs.add(new BioenSizeInfOutput(rank));
            }

            if (getConfiguration().getBoolean("output.bioen.kappa.enabled", NO_WARNING)) {
                outputs.add(new WeightedSpeciesOutput(rank, "Bioen", "kappa",
                        "Kappa (rate [0-1])",
                        school -> school.getKappa(),
                        school -> school.getInstantaneousAbundance()
                ));
            }
        }

        if (getConfiguration().getBoolean("output.regional.biomass.enabled")) {
            for (int i = 0; i < getNSpecies(); i++) {
                outputs.add(new RegionalOutputsBiomass(rank, "biomass", getSpecies(i)));
            }
        }

        if (getConfiguration().getBoolean("output.regional.abundance.enabled")) {
            for (int i = 0; i < getNSpecies(); i++) {
                outputs.add(new RegionalOutputsAbundance(rank, getSpecies(i)));
            }
        }

        if (getConfiguration().getBoolean("output.regional.yield.enabled")) {
            for (int i = 0; i < getNSpecies(); i++) {
                outputs.add(new RegionalOutputsYield(rank, getSpecies(i)));
            }
        }

        if (getConfiguration().getBoolean("output.regional.yieldN.enabled")) {
            for (int i = 0; i < getNSpecies(); i++) {
                outputs.add(new RegionalOutputsYieldN(rank, getSpecies(i)));
            }
        }

        // warning: simulation init is called after output init.
        //List<String> genet_keys = this.getConfiguration().findKeys("*.trait.mean");
        if (this.getConfiguration().isGeneticEnabled()) {
            if (getConfiguration().getBoolean("output.evolvingtraits.enabled")) {
                outputs.add(new VariableTraitOutput(rank));
            }
        }

        /*
         * Initialize indicators
         */
        for (IOutput indicator : outputs) {
            indicator.init();
            indicator.reset();
        }

        // Initialize the restart maker
        restartFrequency = Integer.MAX_VALUE;
        if (!getConfiguration().isNull("output.restart.recordfrequency.ndt")) {
            restartFrequency = getConfiguration().getInt("output.restart.recordfrequency.ndt");
        }

        writeRestart = true;
        if (!getConfiguration().isNull("output.restart.enabled")) {
            writeRestart = getConfiguration().getBoolean("output.restart.enabled");
        } else {
            warning("Could not find parameter 'output.restart.enabled'. Osmose assumes it is true and a NetCDF restart file will be created at the end of the simulation (or more, depending on parameters 'simulation.restart.recordfrequency.ndt' and 'simulation.restart.spinup').");
        }

        spinupRestart = 0;
        if (!getConfiguration().isNull("output.restart.spinup")) {
            spinupRestart = getConfiguration().getInt("output.restart.spinup") - 1;
        }
    }

    public void close() {
        for (IOutput indicator : outputs) {
            indicator.close();
        }
    }

    public void initStep() {
        if (getSimulation().getYear() >= getConfiguration().getInt("output.start.year")) {
            for (IOutput indicator : outputs) {
                indicator.initStep();
            }
        }
    }

    public void update(int iStepSimu) {

        // UPDATE
        if (getSimulation().getYear() >= getConfiguration().getInt("output.start.year")) {
            for (IOutput indicator : outputs) {
                indicator.update();
                // WRITE
                if (indicator.isTimeToWrite(iStepSimu)) {
                    float time = (float) (iStepSimu + 1) / getConfiguration().getNStepYear();
                    indicator.write(time);
                    indicator.reset();
                }
            }
        }
    }

    public void writeRestart(int iStepSimu) {
        // Create a restart file
        boolean isTimeToWrite = writeRestart;
        isTimeToWrite &= (getSimulation().getYear() >= spinupRestart);
        isTimeToWrite &= ((iStepSimu + 1) % restartFrequency == 0);
        isTimeToWrite |= (iStepSimu >= (getConfiguration().getNStep() - 1));

        if (isTimeToWrite) {
            snapshot.makeSnapshot(iStepSimu);
        }
    }
}
