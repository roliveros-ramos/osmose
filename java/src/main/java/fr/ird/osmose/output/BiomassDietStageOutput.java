/* 
 * 
 * OSMOSE (Object-oriented Simulator of Marine Ecosystems)
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

package fr.ird.osmose.output;

import fr.ird.osmose.Cell;
import fr.ird.osmose.stage.DietOutputStage;
import fr.ird.osmose.stage.IStage;
import java.util.HashMap;

/**
 *
 * @author pverley
 */
public class BiomassDietStageOutput extends AbstractOutput {

    private int nColumns;
    /*
     * Biomass per diet stages [SPECIES][DIET_STAGES]
     */
    private HashMap<Integer, double[]> biomassStage = new HashMap();

    private IStage dietOutputStage;

    public BiomassDietStageOutput(int rank) {
        super(rank, "Trophic", "biomassPredPreyIni");
    }

    @Override
    public void init() {

        dietOutputStage = new DietOutputStage();
        dietOutputStage.init();

        nColumns = 0;
        
        // Sum-up diet stages
        for (int iSpec : getConfiguration().getPredatorIndex()) {
            nColumns += dietOutputStage.getNStage(iSpec);
        }

        nColumns += getConfiguration().getNRscSpecies();

        super.init();
    }

    @Override
    String getDescription() {
        return "\\ Biomass (tons) of preys at the beginning of the time step (before all sources of mortality - fishing, predation, starvation, others)";
    }

    @Override
    String[] getHeaders() {

        int k = 0;
        String[] headers = new String[nColumns];
        for (int iSpec : getConfiguration().getFocalIndex()) {
            String name = getSpecies(iSpec).getName();
            float[] threshold = dietOutputStage.getThresholds(iSpec);
            int nStage = dietOutputStage.getNStage(iSpec);
            for (int s = 0; s < nStage; s++) {
                if (nStage == 1) {
                    headers[k] = name;    // Name predators
                } else {
                    if (s == 0) {
                        headers[k] = name + " < " + threshold[s];    // Name predators
                    } else {
                        headers[k] = name + " >=" + threshold[s - 1];    // Name predators
                    }
                }
                k++;
            }
        }

        for (int iSpec : getConfiguration().getBackgroundIndex()) {
            String name = getBkgSpecies(iSpec).getName();
            float[] threshold = dietOutputStage.getThresholds(iSpec);
            int nStage = dietOutputStage.getNStage(iSpec);
            for (int s = 0; s < nStage; s++) {
                if (nStage == 1) {
                    headers[k] = name;    // Name predators
                } else {
                    if (s == 0) {
                        headers[k] = name + " < " + threshold[s];    // Name predators
                    } else {
                        headers[k] = name + " >=" + threshold[s - 1];    // Name predators
                    }
                }
                k++;
            }
        }


        for (int iSpec : getConfiguration().getResourceIndex()) {
            headers[k] = getConfiguration().getResourceSpecies(iSpec).getName();
            k++;
        }
        return headers;
    }

    @Override
    public void initStep() {

        getSchoolSet().getPresentSchools().forEach(school -> {
            biomassStage.get(school.getSpeciesIndex())[dietOutputStage.getStage(school)] += school.getBiomass();
        });

        this.getBkgSchoolSet().getAllSchools().forEach(school -> {
            biomassStage.get(school.getSpeciesIndex())[dietOutputStage.getStage(school)] += school.getBiomass();
        });

        for (int iRsc : getConfiguration().getResourceIndex()) {
            biomassStage.get(iRsc)[0] += getTotalBiomass(iRsc);
        }

    }

    @Override
    public void reset() {
        
        biomassStage.clear();
        
        for (int iSpec : getConfiguration().getPredatorIndex()) {
            biomassStage.put(iSpec, new double[dietOutputStage.getNStage(iSpec)]);
        }
        
        for (int i : getConfiguration().getPreyIndex()) {
            // we consider just 1 stage per resource group
            biomassStage.put(i, new double[1]);
        }
    }

    @Override
    public void update() {
        // nothing to do
    }

    @Override
    public void write(float time) {
        double[] biomass = new double[nColumns];
        double nsteps = getRecordFrequency();
        int k = 0;
        for (int iSpec : getConfiguration().getPredatorIndex()) {
            for (int s = 0; s < dietOutputStage.getNStage(iSpec); s++) {
                biomass[k] = biomassStage.get(iSpec)[s] / nsteps;
                k++;
            }
        }
        for (int j : getConfiguration().getResourceIndex()) {
            biomass[k] = biomassStage.get(j)[0] / nsteps;
            k++;
        }
        writeVariable(time, biomass);
    }

    /**
     * Gets the total biomass of the resource groups over the grid.
     *
     * @return the cumulated biomass over the domain in tonne
     */
    private double getTotalBiomass(int iRsc) {
        double biomTot = 0.d;
        for (Cell cell : getGrid().getCells()) {
            if (!cell.isLand()) {
                biomTot += getSimulation().getResourceForcing(iRsc).getBiomass(cell);
            }
        }
        return biomTot;
    }

}
