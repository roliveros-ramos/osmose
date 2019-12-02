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

import fr.ird.osmose.Cell;
import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import fr.ird.osmose.process.mortality.MortalityCause;
import java.io.File;
import java.util.List;

/**
 *
 * @author nbarrier
 */
public class RegionalOutputsYield extends RegionalOutputsBiomass {
    
    public RegionalOutputsYield(int rank, Species species) {
        super(rank, "yield", species);
    }

    @Override
    String getDescription() {
        String output = "Mean yield biomass integrated over pre-defined regional domains";
        return output;
    }

    @Override
    public void update() {
        // Loop over all the regions
        for (int idom = 0; idom < this.getNRegions(); idom++) {

            // index of the points that belong to the region
            int[] iind = this.getIDom(idom);
            int[] jind = this.getJDom(idom);
            int npoints = iind.length;

            // loop over all the points within the current region
            for (int ipoint = 0; ipoint < npoints; ipoint++) {
                int i = iind[ipoint];
                int j = jind[ipoint];
                Cell cell = this.getGrid().getCell(i, j);
                // extraction of the schools that belong to the current cell
                List<School> listSchool = getSchoolSet().getSchools(cell);
                if (null != listSchool) {
                    for (School school : listSchool) {
                        // integration of the biomass that belong to the proper index
                        if (school.getSpeciesIndex() == this.getSpecies().getIndex()) { 
                            values[idom] += school.abd2biom(school.getNdead(MortalityCause.FISHING));
                        } // end if species test
                    }  // end of school loop
                }  // end if if null
            }  // end of domain point loop
        }  // end of domain loop
    }  // end of method
}