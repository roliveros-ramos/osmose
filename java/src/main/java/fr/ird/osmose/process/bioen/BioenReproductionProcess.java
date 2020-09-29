/* 
 * 
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (C) IRD (Institut de Recherche pour le Développement) 2009-2020
 * 
 * Osmose is a computer program whose purpose is to simulate fish
 * populations and their interactions with their biotic and abiotic environment.
 * OSMOSE is a spatial, multispecies and individual-based model which assumes
 * size-based opportunistic predation based on spatio-temporal co-occurrence
 * and size adequacy between a predator and its prey. It represents fish
 * individuals grouped into schools, which are characterized by their size,
 * weight, age, taxonomy and geographical location, and which undergo major
 * processes of fish life cycle (growth, explicit predation, additional and
 * starvation mortalities, reproduction and migration) and fishing mortalities
 * (Shin and Cury 2001, 2004).
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * Laure VELEZ (laure.velez@ird.fr)
 * Nicolas Barrier (nicolas.barrier@ird.fr)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation (version 3 of the License). Full description
 * is provided on the LICENSE file.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * 
 */

package fr.ird.osmose.process.bioen;

import fr.ird.osmose.process.*;
import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import java.util.HashMap;

import java.util.List;

/**
 * This class controls the reproduction process in the simulated domain. The
 * user defines the spawning season (a CSV file per Species) either annual or
 * interannual, the percentage of female in the population (sex ratio) and the
 * number of eggs per gramme of mature female (beta) for every species. Osmose
 * estimates the spawning stock biomass (SSB) and calculates the number of eggs
 * to be released in the system at every time steps.<br />
 * During the spin-up of the simulation (duration of spin-up either set by the
 * user or set by default to the lifespan of the longest-lived species) Osmose
 * prevents species collapse by artificially setting the SSB to a predefined
 * level (user defined, this parameter could/should be calibrated) in order to
 * guarantee egg release.
 */
public class BioenReproductionProcess extends ReproductionProcess {

    public BioenReproductionProcess(int rank) {
        super(rank);
    }

    @Override
    public void init() {
        super.init();
    }

    @Override
    public void run() {
        
        // spawning stock biomass per species
        HashMap<Integer, Double> SSB = new HashMap();
        for (int i : getConfiguration().getFocalIndex()) {
            SSB.put(i, 0.);
        }

        // loop over all the schools to compute 
        for (School school : getSchoolSet().getSchools()) {

            int i = school.getSpeciesIndex();
            if (school.isMature()) {
                double value = SSB.get(i) + school.getInstantaneousBiomass();
                SSB.put(i, value);
            }
            // increment age
            school.incrementAge();
        }

        // loop over the species to lay cohort at age class 0
        for (int i : getConfiguration().getFocalIndex()) {

            // Recover the species object and all the schools of the given species
            Species species = getSpecies(i);
            List<School> schoolset = getSchoolSet().getSchools(species);
            WeightedRandomDraft weight_rand = new WeightedRandomDraft();
            
            // compute nomber of eggs to be released
            double season = getSeason(getSimulation().getIndexTimeSimu(), species);

            if (getSimulation().getIndexTimeSimu() < this.getYearSeading() && SSB.get(i) == 0.) {
                // seeding process for collapsed species
                // if seeding biomass is 0 (no mature indivials, release eggs in the
                // old fashioned way.
                SSB.put(i, this.getSeedingBiomass(i));
                double nEgg = this.getSexRatio(i) * this.getBeta(i) * season * SSB.get(i) * 1000000;

                // in this case, weight_rand is never used.
                this.create_reproduction_schools(i, nEgg, true, weight_rand);

            } else {

                double negg_tot = 0.d;

                // if the seeding biomass is not null, loop over sexually mature schools
                for (School school : schoolset) {

                    if (!school.isMature()) {
                        // if school is not mature, no reproduction
                        continue;
                    }

                    // recovers the weight of the gonad that is lost for reproduction.
                    // in this case, the gonad weight times the season variable.
                    float wEgg = school.getGonadWeight() * (float) season;
                  
                    // the wEgg content is removed from the gonad
                    school.incrementGonadWeight(-wEgg);
                    
                    // the number of eggs is equal to the total gonad weight that is gone
                    // divided by the egg weight.
                    // barrier.n: change in conversion from tone to gram
                    // since EggWeight is in g.
                    double nEgg = wEgg * this.getSexRatio(i) / species.getEggWeight() * 1000000 * school.getInstantaneousAbundance();
                    negg_tot += nEgg;
                    weight_rand.add(nEgg, school);
                }  // end of loop over the school that belong to species i    

                this.create_reproduction_schools(i, negg_tot, false, weight_rand);
                
            }  // end of SSB statement
        }  // end of species loop
    }

    private void create_reproduction_schools(int i, double nEgg, boolean init_genotype, WeightedRandomDraft rand_draft) {
        // nschool increases with time to avoid flooding the simulation with too many schools since the beginning
        //nSchool = Math.min(getConfiguration().getNSchool(i), nSchool * (getSimulation().getIndexTimeSimu() + 1) / (getConfiguration().getNStepYear() * 10));

        // if the number of eggs is 0, nothing is done
        if (nEgg == 0.d) {
            return;
        }

        // lay age class zero
        int nSchool = getConfiguration().getNSchool(i);
        Species species = getSpecies(i);

        // do nothing, zero school
        if (nEgg < nSchool) {

            School school0 = new School(species, nEgg);
            school0.instance_genotype(this.getRank());
            if (init_genotype) {
                school0.getGenotype().init_genotype();
            } else {
                School parent_a = (School) rand_draft.next();
                School parent_b = (School) rand_draft.next();
                school0.getGenotype().transmit_genotype(parent_a.getGenotype(), parent_b.getGenotype());
            }
            getSchoolSet().add(school0);
        } else if (nEgg >= nSchool) {

            for (int s = 0; s < nSchool; s++) {
                School school0 = new School(species, nEgg / nSchool);
                school0.instance_genotype(this.getRank());
                if (init_genotype) {
                    school0.getGenotype().init_genotype();
                } else {
                    School parent_a = (School) rand_draft.next();
                    School parent_b = (School) rand_draft.next();
                    school0.getGenotype().transmit_genotype(parent_a.getGenotype(), parent_b.getGenotype());
                }
                getSchoolSet().add(school0);
            }

        } // end of test on nEgg
    }  // end of method

}  // end of class
