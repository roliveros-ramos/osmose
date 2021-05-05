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

import fr.ird.osmose.School;
import fr.ird.osmose.util.SimulationLinker;
import java.io.File;
import java.io.IOException;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayInt;
import ucar.ma2.ArrayString;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

/**
 *
 * @author pverley
 */
public class ModularSchoolSetSnapshot extends SimulationLinker implements IOutput {
    
    private Variable xVar, yVar, abVar, weightVar, specVar, gonadVar, uuidVar;
    
    private boolean includeCoords;
    private boolean includeAbundance;
    private boolean includeWeight;
    private boolean includeGonadWeight;
    private boolean includeTemp;
    private int savingFrequency;
    
    public ModularSchoolSetSnapshot(int rank) {
        super(rank);
    }

    @Override
    public void init() {
        if (getConfiguration().isNull("output.individual.freq")) {
            savingFrequency = 1;
        } else {
            savingFrequency = getConfiguration().getInt("output.individual.freq");
        }
        includeCoords = getConfiguration().getBoolean("output.individual.coords.enabled");
        includeCoords = getConfiguration().getBoolean("output.individual.coords.enabled");
        includeAbundance = getConfiguration().getBoolean("output.individual.abundance.enabled");
        includeWeight = getConfiguration().getBoolean("output.individual.weight.enabled");
        includeGonadWeight = getConfiguration().isBioenEnabled() ?  getConfiguration().getBoolean("output.individual.gonadweight.enabled") : false;
    }
    
    @Override
    public void write(float time) {
        
        int iStepSimu = getSimulation().getIndexTimeSimu();
        
        int nSchool = getSchoolSet().getSchools().size();
        if(nSchool == 0) { 
            return;
        }
        
        boolean useBioen = this.getConfiguration().isBioenEnabled();
        
        ArrayFloat.D1 lon = null;
        ArrayFloat.D1 lat = null;
        ArrayFloat.D1 gonadicWeight = null;
        ArrayFloat.D1 weight = null;
        ArrayFloat.D1 abundance = null;
        
        //int nChars = getSchoolSet().getSchools().get(0).getID().toString().length();
        
        ArrayString uuid = new ArrayString(new int[] {nSchool});
        ArrayInt.D1 species = new ArrayInt.D1(nSchool);
        
        if (useBioen && includeGonadWeight) {
            gonadicWeight = new ArrayFloat.D1(nSchool);
        }

        if (includeWeight) {
            weight = new ArrayFloat.D1(nSchool);
        }

        if (includeAbundance) {
            abundance = new ArrayFloat.D1(nSchool);
        }

        if (includeCoords) {
            lon = new ArrayFloat.D1(nSchool);
            lat = new ArrayFloat.D1(nSchool);
        }
          
        NetcdfFileWriter nc = createNCFile(iStepSimu);
                
        int s = 0;
        Index uuidIndex = uuid.getIndex();
        
        // fill up the arrays
        for (School school : getSchoolSet().getSchools()) {
            species.set(s, school.getSpeciesIndex());
            if (includeCoords) {
                lon.set(s, school.getLon());
                lat.set(s, school.getLat());
            }
            
            if(includeAbundance) { 
                abundance.set(s, (float) school.getInstantaneousAbundance());
            }
        
            if(includeWeight) { 
                weight.set(s, school.getWeight() * 1e6f);
            }

            if(includeGonadWeight) {
                gonadicWeight.set(s, school.getGonadWeight()* 1e6f);
            }
            
            uuidIndex.set(s);
            uuid.set(uuidIndex, school.getID().toString());
            
            // writes genotype for each school
            s++;
        }
        // write the arrays in the NetCDF file
        try {

            nc.write(this.specVar, species);
            nc.write(this.uuidVar, uuid);
            if (this.includeCoords) {
                nc.write(this.xVar, lon);
                nc.write(this.yVar, lat);
            }
            if (this.includeAbundance) {
                nc.write(this.abVar, abundance);
            }

            if (this.includeWeight) {
                nc.write(this.weightVar, weight);
            }

            if (useBioen && this.includeGonadWeight) {
                nc.write(this.gonadVar, gonadicWeight);
            }
            
            nc.close();
            //close(nc);
        } catch (IOException ex) {
            error("Error writing snapshot " + nc.getNetcdfFile().getLocation(), ex);
        } catch (InvalidRangeException ex) {
            error("Error writing snapshot " + nc.getNetcdfFile().getLocation(), ex);
        }
    }

    private NetcdfFileWriter createNCFile(int iStepSimu) {

        NetcdfFileWriter nc = null;
        File file = null;
        
        /*
         * Create NetCDF file
         */
        try {
            File path = new File(getConfiguration().getOutputPathname());
            file = new File(path, getFilename(iStepSimu));
            file.getParentFile().mkdirs();
            nc = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4, file.getAbsolutePath());
        } catch (IOException ex) {
            error("Could not create snapshot file " + file.getAbsolutePath(), ex);
        }
        /*
         * Create dimensions
         */
        nc.addDimension(null, "nschool", getSchoolSet().getSchools().size());
        
        /*
         * Add variables
         */
        specVar = nc.addVariable(null, "species", DataType.INT, "nschool");
        specVar.addAttribute(new Attribute("description", "index of the species"));
        
        uuidVar = nc.addVariable(null, "uuid", DataType.STRING, "nschool");
        

        if (this.includeCoords) {
            xVar = nc.addVariable(null, "lon", DataType.FLOAT, "nschool");
            xVar.addAttribute(new Attribute("units", "scalar"));
            xVar.addAttribute(new Attribute("description", "longitude of the school"));

            yVar = nc.addVariable(null, "lat", DataType.FLOAT, "nschool");
            yVar.addAttribute(new Attribute("units", "scalar"));
            yVar.addAttribute(new Attribute("description", "latitude index of the school"));
        }

        if (this.includeAbundance) {
            abVar = nc.addVariable(null, "abundance", DataType.DOUBLE, "nschool");
            abVar.addAttribute(new Attribute("units", "scalar"));
            abVar.addAttribute(new Attribute("description", "number of fish in the school"));
        }
        
        if (this.includeWeight) {
            weightVar = nc.addVariable(null, "weight", DataType.FLOAT, "nschool");
            weightVar.addAttribute(new Attribute("units", "g"));
            weightVar.addAttribute(new Attribute("description", "weight of the fish in the school in gram"));
        }
        
        if (this.getConfiguration().isBioenEnabled() && this.includeGonadWeight) {
            gonadVar = nc.addVariable(null, "gonadWeight", DataType.FLOAT, "nschool");
            gonadVar.addAttribute(new Attribute("units", "g"));
            gonadVar.addAttribute(new Attribute("description", "gonadic weight of the fish in the school in gram"));
        }

        /*
         * Add global attributes
         */
        nc.addGroupAttribute(null, new Attribute("step", String.valueOf(iStepSimu)));
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < getConfiguration().getNSpecies(); i++) {
            str.append(i);
            str.append("=");
            str.append(getSpecies(i).getName());
            str.append(" ");
        }
        
        nc.addGroupAttribute(null, new Attribute("species", str.toString()));
        
        try {
            /*
             * Validates the structure of the NetCDF file.
             */
            nc.create();

        } catch (IOException ex) {
            error("Could not create snapshot file " + nc.getNetcdfFile().getLocation(), ex);
        }
        return nc;
    }

    String getFilename(int iStepSimu) {
        StringBuilder filename = new StringBuilder("individual");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_modular_snapshot_step");
        filename.append(iStepSimu);
        filename.append(".nc.");
        filename.append(getRank());
        return filename.toString();
    }

    @Override
    public void initStep() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void reset() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void update() {
        // TODO Auto-generated method stub
        
    }


    @Override
    public boolean isTimeToWrite(int iStepSimu) {
        return ((iStepSimu + 1) % this.savingFrequency == 0);
    }

    @Override
    public void close() {

    }
}
