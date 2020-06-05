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
 * processes of fish life cycle (growth, explicit predation, additional and
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
package fr.ird.osmose.util;

import fr.ird.osmose.Configuration;

/** 
 * Class that manages the initialisation of year parameters.
 * 
 * Years can be provided as a list of years or as an initial and final year.
 * 
 * Years that exceed the number of simulated years are discarded.
 *
 * @author Nicolas
 */
public class YearParameters extends OsmoseLinker {

    /** Prefix of the parameters. */
    private final String prefix;
    /** Prefix of the parameters. */
    private final String suffix;
    private int[] years;

    public YearParameters(String prefix, String suffix) {
        this.prefix = prefix;
        this.suffix = suffix;
        this.init();
    }

    public void init() {
        
        String key;
        int ymax, ymin;
        int[] tempYears;

        Configuration conf = this.getConfiguration();

        int nseason = getConfiguration().getNStepYear();
        int nyear = (int) Math.ceil(this.getConfiguration().getNStep() / (float) nseason);
        System.out.println("Nyear = " + nyear);

        key = String.format("%s.years.%s", this.prefix, this.suffix);
        if (!conf.isNull(key)) {
            tempYears = conf.getArrayInt(key);    
        } else {           
            key = String.format("%s.initialYear.%s", this.prefix, this.suffix);
            if (!conf.isNull(key)) {
                ymin = conf.getInt(key);
            } else {
                ymin = 0;
            }

            key = String.format("%s.lastYear.%s", this.prefix, this.suffix);
            if (!conf.isNull(key)) {
                ymax = conf.getInt(key);
            } else {
                ymax = nyear;
            }

            int nyears = ymax - ymin;
            tempYears = new int[nyears];
            int cpt = 0;
            for (int y = ymin; y < ymax; y++) {
                tempYears[cpt] = y;
                cpt++;
            }
        }

        // Get rid off years that are beyond number of simulated years.
        int goodyear = 0;
        for (int y : tempYears) {
            if (y < nyear) {
                goodyear++;
            }
        }

        years = new int[goodyear];
        int cpt = 0;
        for (int y : tempYears) {
            if (y < nyear) {
                years[cpt] = y;
                cpt++;
            }
        }
    }

    public int[] getYears() {
        return this.years;
    }

}
