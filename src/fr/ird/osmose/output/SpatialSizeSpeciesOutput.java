/*
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Philippe VERLEY (philippe.verley@ird.fr)
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
import fr.ird.osmose.output.distribution.AbstractDistribution;
import fr.ird.osmose.util.io.IOTools;
import fr.ird.osmose.util.SimulationLinker;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import ucar.ma2.ArrayFloat;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;

/**
 *
 * @author pverley
 */
public class SpatialSizeSpeciesOutput extends SimulationLinker implements IOutput {

    /**
     * _FillValue attribute for cells on land
     */
    private final float FILLVALUE = -99.f;
    
    /** Size/Age distribution. */
    private final AbstractDistribution distrib;
    
    private double timeOut;
    private int counter;
    
    /**
     * Object for creating/writing netCDF files.
     */
    private NetcdfFileWriteable nc;
    // spatial indicators
    private float[][][][] abundance;
    private boolean cutoffEnabled;
    /**
     * Threshold age (year) for age class zero. This parameter allows to discard
     * schools younger that this threshold in the calculation of the indicators
     * when parameter <i>output.cutoff.enabled</i> is set to {@code true}.
     * Parameter <i>output.cutoff.age.sp#</i>
     */
    private float[] cutoffAge;
    
    private int recordFrequency;

    public SpatialSizeSpeciesOutput(int rank, AbstractDistribution distrib) {
        super(rank);
        this.distrib = distrib;
    }

    private boolean includeClassZero() {
        return !cutoffEnabled;
    }

    @Override
    public void init() {
        
                
        recordFrequency = getConfiguration().getInt("output.recordfrequency.ndt");

        // cutoff for egg, larvae and juveniles
        cutoffEnabled = getConfiguration().getBoolean("output.cutoff.enabled");
        cutoffAge = new float[getNSpecies()];
        if (cutoffEnabled) {
            for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
                cutoffAge[iSpec] = getConfiguration().getFloat("output.cutoff.age.sp" + iSpec);
            }
        }

        /*
         * Create NetCDF file
         */
        try {
            nc = NetcdfFileWriteable.createNew("");
            String filename = getFilename();
            IOTools.makeDirectories(filename);
            nc.setLocation(filename);
        } catch (IOException ex) {
            Logger.getLogger(SpatialOutput.class.getName()).log(Level.SEVERE, null, ex);
        }
        /*
         * Create dimensions
         */
        Dimension speciesDim = nc.addDimension("species", getNSpecies());
        Dimension ltlDim = nc.addDimension("class", this.distrib.getNClass());
        Dimension columnsDim = nc.addDimension("nx", getGrid().get_nx());
        Dimension linesDim = nc.addDimension("ny", getGrid().get_ny());
        Dimension timeDim = nc.addUnlimitedDimension("time");
        /*
         * Add variables
         */
        nc.addVariable("time", DataType.FLOAT, new Dimension[]{timeDim});
        nc.addVariableAttribute("time", "units", "days since 0-1-1 0:0:0");
        nc.addVariableAttribute("time", "calendar", "360_day");
        nc.addVariableAttribute("time", "description", "time ellapsed, in days, since the beginning of the simulation");
      
        nc.addVariable("abundance", DataType.FLOAT, new Dimension[]{timeDim, ltlDim, speciesDim, linesDim, columnsDim});
        nc.addVariableAttribute("abundance", "units", "number of fish");
        nc.addVariableAttribute("abundance", "description", "Number of fish per species and per cell");
        nc.addVariableAttribute("abundance", "_FillValue", -99.f);
        
        nc.addVariable("class", DataType.FLOAT, new Dimension[]{ltlDim});
        /*nc.addVariableAttribute("class", "units", "number of fish");
        nc.addVariableAttribute("class", "description", "Number of fish per species and per cell");
        nc.addVariableAttribute("class", "_FillValue", -99.f);*/
        
        nc.addVariable("latitude", DataType.FLOAT, new Dimension[]{linesDim, columnsDim});
        nc.addVariableAttribute("latitude", "units", "degree");
        nc.addVariableAttribute("latitude", "description", "latitude of the center of the cell");
        
        nc.addVariable("longitude", DataType.FLOAT, new Dimension[]{linesDim, columnsDim});
        nc.addVariableAttribute("longitude", "units", "degree");
        nc.addVariableAttribute("longitude", "description", "longitude of the center of the cell");
        /*
         * Add global attributes
         */
        /*
        nc.addGlobalAttribute("dimension_step", "step=0 before predation, step=1 after predation");
        StringBuilder str = new StringBuilder();
        for (int kltl = 0; kltl < getConfiguration().getNPlankton(); kltl++) {
            str.append(kltl);
            str.append("=");
            str.append(getConfiguration().getPlankton(kltl));
            str.append(" ");
        }
        nc.addGlobalAttribute("dimension_ltl", str.toString());
        */
        
        try {
            /*
             * Validates the structure of the NetCDF file.
             */
            nc.create();
            /*
             * Writes variable longitude and latitude
             */
            ArrayFloat.D2 arrLon = new ArrayFloat.D2(getGrid().get_ny(), getGrid().get_nx());
            ArrayFloat.D2 arrLat = new ArrayFloat.D2(getGrid().get_ny(), getGrid().get_nx());
            for (Cell cell : getGrid().getCells()) {
                arrLon.set(cell.get_jgrid(), cell.get_igrid(), cell.getLon());
                arrLat.set(cell.get_jgrid(), cell.get_igrid(), cell.getLat());
            }
            
            /* Writes out the class array. */
            ArrayFloat.D1 arrClass = new ArrayFloat.D1(this.distrib.getNClass());
            for (int iclass=0; iclass< this.distrib.getNClass(); iclass++){
                arrClass.set(iclass, this.distrib.getThreshold(iclass));
            }
            
            nc.write("class", arrClass);
            nc.write("longitude", arrLon);
            nc.write("latitude", arrLat);
        } catch (IOException ex) {
            Logger.getLogger(SpatialOutput.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidRangeException ex) {
            Logger.getLogger(SpatialOutput.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void close() {
        try {
            nc.close();
            String strFilePart = nc.getLocation();
            String strFileBase = strFilePart.substring(0, strFilePart.indexOf(".part"));
            File filePart = new File(strFilePart);
            File fileBase = new File(strFileBase);
            filePart.renameTo(fileBase);
        } catch (IOException ex) {
            warning("Problem closing the NetCDF spatial output file | {0}", ex.toString());
        }
    }

    @Override
    public void initStep() {
    }

    @Override
    public void reset() {

    }

    @Override
    public void update() {
        
        int iStepSimu = getSimulation().getIndexTimeSimu();
        if (this.isTimeToReset(iStepSimu)) {
            int nSpecies = getNSpecies();
            int nx = getGrid().get_nx();
            int ny = getGrid().get_ny();
            abundance = new float[getNClass()][nSpecies][ny][nx];
            this.counter = 0;
            this.timeOut = 0;
        }

        this.timeOut += (float) (iStepSimu + 1) / getConfiguration().getNStepYear();
        this.counter += 1;
        
        // Loop over the cells
        for (Cell cell : getGrid().getCells()) {
            if (!cell.isLand()) {
                int i = cell.get_igrid();
                int j = cell.get_jgrid();
                if (null != getSchoolSet().getSchools(cell)) {
                    for (School school : getSchoolSet().getSchools(cell)) {
                        if (cutoffEnabled && school.getAge() < cutoffAge[school.getSpeciesIndex()]) {
                            //System.out.println("+++ cutoff ");
                            continue;
                        }
                        if (!school.isUnlocated()) {
                            //System.out.println("+++ unlocated ");
                            int iSpec = school.getSpeciesIndex();
                            int classSchool = this.distrib.getClass(school);
                            if (classSchool >= 0) {
                                abundance[classSchool][iSpec][j][i] += school.getInstantaneousAbundance();
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean isTimeToReset(int iStepSimu) {
        return (((iStepSimu) % recordFrequency) == 0);
    }
    
    private int getNClass()
    {
        return this.distrib.getNClass();
    }
    
    @Override
    public void write(float time) {

        // Pre-writing
        for (Cell cell : getGrid().getCells()) {
            int i = cell.get_igrid();
            int j = cell.get_jgrid();
            // Set _FillValue on land cells
            if (cell.isLand()) {
                for (int iclass = 0; iclass < this.distrib.getNClass(); iclass++) {
                    for (int ispec = 0; ispec < getNSpecies(); ispec++) {
                        abundance[iclass][ispec][j][i] = FILLVALUE;
                    }
                }
            }
        }

        // Write into NetCDF file
        int nSpecies = getNSpecies();
        ArrayFloat.D5 arrAbundance = new ArrayFloat.D5(1, this.distrib.getNClass(), nSpecies, getGrid().get_ny(), getGrid().get_nx());
       
        for (int iclass = 0; iclass < this.distrib.getNClass(); iclass++) {
            for (int kspec = 0; kspec < nSpecies; kspec++) {
                for (int j = 0; j < getGrid().get_ny(); j++) {
                    for (int i = 0; i < getGrid().get_nx(); i++) {
                        if (!getGrid().getCell(i, j).isLand()) {
                             arrAbundance.set(0, iclass, kspec, j, i, abundance[iclass][kspec][j][i] / (float) this.counter);
                        } else {
                            arrAbundance.set(0, iclass, kspec, j, i, abundance[iclass][kspec][j][i]);
                        }      
                    }
                }
            }
        }
        

        ArrayFloat.D1 arrTime = new ArrayFloat.D1(1);
        arrTime.set(0, (float) this.timeOut * 360 / (float) this.counter);

        int index = nc.getUnlimitedDimension().getLength();
        //System.out.println("NetCDF saving time " + index + " - " + time);
        try {
            nc.write("time", new int[]{index}, arrTime);        
            nc.write("abundance", new int[]{index, 0, 0, 0, 0}, arrAbundance);
        } catch (IOException ex) {
            Logger.getLogger(SpatialOutput.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidRangeException ex) {
            Logger.getLogger(SpatialOutput.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private String getFilename() {
        File path = new File(getConfiguration().getOutputPathname());
        StringBuilder filename = new StringBuilder(path.getAbsolutePath());
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_spatialized").append(this.distrib.getType()).append("Species_Simu");
        filename.append(getRank());
        filename.append(".nc.part");
        return filename.toString();
    }

    @Override
    public boolean isTimeToWrite(int iStepSimu) {
        // Always true, every time step should be written in the NetCDF file.
        return (((iStepSimu + 1) % recordFrequency) == 0);
    }
}
