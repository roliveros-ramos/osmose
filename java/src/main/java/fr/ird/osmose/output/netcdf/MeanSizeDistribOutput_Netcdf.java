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

package fr.ird.osmose.output.netcdf;

import fr.ird.osmose.School;
import fr.ird.osmose.output.distribution.AbstractDistribution;
import java.io.File;

/**
 *
 * @author pverley
 */
public class MeanSizeDistribOutput_Netcdf extends AbstractMeanDistribOutput_Netcdf {

    public MeanSizeDistribOutput_Netcdf(int rank, AbstractDistribution distrib) {
        super(rank, distrib);
    }

    @Override
    String getFilename() {
        StringBuilder filename = this.initFileName();
        filename.append(getType().toString());
        filename.append("Indicators");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_meanSizeDistribBy");
        filename.append(getType().toString());
        filename.append("_Simu");
        filename.append(getRank());
        filename.append(".nc.part");
        return filename.toString();
    }

    @Override
    String getDescription() {
        StringBuilder description = new StringBuilder();
        description.append("Mean size of fish (centimeter) by ");
        description.append(getType().getDescription());
        description.append(". For class i, the mean size in [i,i+1[ is reported.");
        return description.toString();
    }

    @Override
    public void initStep() {
        // nothing to do
    }

    @Override
    public void update() {
        for (School school : getSchoolSet().getAliveSchools()) {
            int iSpec = school.getSpeciesIndex();
            int iClass = getClass(school);
            if (iClass >= 0) {
                values[iSpec][iClass] += school.getInstantaneousAbundance() * school.getLength();
                denominator[iSpec][iClass] += school.getInstantaneousAbundance();
            }
        }
    }

    @Override
    String getUnits() {
        return("cm"); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    String getVarname() {
        return("size");
    }
}
