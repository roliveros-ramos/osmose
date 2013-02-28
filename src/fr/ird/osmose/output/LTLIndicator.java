package fr.ird.osmose.output;

import fr.ird.osmose.Cell;
import fr.ird.osmose.util.SimulationLinker;
import fr.ird.osmose.util.IOTools;
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
public class LTLIndicator extends SimulationLinker implements  Indicator {

    /**
     * _FillValue attribute for cells on land
     */
    private final float FILLVALUE = -99.f;
    /**
     * Object for creating/writing netCDF files.
     */
    private NetcdfFileWriteable nc;
    /**
     * LTL biomass array
     */
    private float[][][] ltlbiomass;
    
     public LTLIndicator(int replica) {
        super(replica);
    }

    @Override
    public void init() {
        String filename = getFilename();
        IOTools.makeDirectories(filename);
        createNCFile(filename);
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
        } catch (Exception ex) {
            Logger.getLogger(LTLIndicator.class.getName()).log(Level.WARNING, "Problem closing the NetCDF output file ==> {0}", ex.toString());
        }
    }

    @Override
    public void initStep() {
        // nothing to do
    }

    @Override
    public void reset() {
        int nx = getGrid().getNbColumns();
        int ny = getGrid().getNbLines();
        ltlbiomass = new float[getConfiguration().getNPlankton()][ny][nx];
    }

    @Override
    public void update() {
        // Loop over the cells
        for (Cell cell : getGrid().getCells()) {
            if (!cell.isLand()) {
                for (int iltl = 0; iltl < getConfiguration().getNPlankton(); iltl++) {
                    ltlbiomass[iltl][cell.get_igrid()][cell.get_jgrid()] = getSimulation().getPlankton(iltl).getBiomass(cell);
                }
            }
        }
    }

    @Override
    public boolean isEnabled() {
        return getConfiguration().outputPlanktonBiomass;
    }

    @Override
    public void write(float time) {
        // Pre-writing
        for (Cell cell : getGrid().getCells()) {
            int i = cell.get_igrid();
            int j = cell.get_jgrid();
            // Set _FillValue on land cells
            if (cell.isLand()) {
                for (int iltl = 0; iltl < getConfiguration().getNPlankton(); iltl++) {
                    ltlbiomass[iltl][i][j] = FILLVALUE;
                }
            }
        }

        // Write into NetCDF file
        ArrayFloat.D4 arrLTL = new ArrayFloat.D4(1, getConfiguration().getNPlankton(), getGrid().getNbLines(), getGrid().getNbColumns());
        int nl = getGrid().getNbLines() - 1;
        for (int kltl = 0; kltl < getConfiguration().getNPlankton(); kltl++) {
            for (int i = 0; i < getGrid().getNbLines(); i++) {
                for (int j = 0; j < getGrid().getNbColumns(); j++) {
                    arrLTL.set(0, kltl, nl - i, j, ltlbiomass[kltl][i][j]);
                }
            }
        }

        ArrayFloat.D1 arrTime = new ArrayFloat.D1(1);
        arrTime.set(0, time);

        int index = nc.getUnlimitedDimension().getLength();
        //System.out.println("NetCDF saving time " + index + " - " + time);
        try {
            nc.write("ltl_biomass", new int[]{index, 0, 0, 0}, arrLTL);
        } catch (IOException ex) {
            Logger.getLogger(SpatialIndicator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidRangeException ex) {
            Logger.getLogger(SpatialIndicator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void createNCFile(String ncfile) {
        /*
         * Create NetCDF file
         */
        try {
            nc = NetcdfFileWriteable.createNew("");
            nc.setLocation(ncfile);
        } catch (IOException ex) {
            Logger.getLogger(LTLIndicator.class.getName()).log(Level.SEVERE, null, ex);
        }
        /*
         * Create dimensions
         */
        Dimension ltlDim = nc.addDimension("ltl", getConfiguration().getNPlankton());
        Dimension columnsDim = nc.addDimension("columns", getGrid().getNbColumns());
        Dimension linesDim = nc.addDimension("lines", getGrid().getNbLines());
        Dimension timeDim = nc.addUnlimitedDimension("time");
        /*
         * Add variables
         */
        nc.addVariable("time", DataType.FLOAT, new Dimension[]{timeDim});
        nc.addVariableAttribute("time", "units", "year");
        nc.addVariableAttribute("time", "description", "time ellapsed, in years, since the begining of the simulation");
        nc.addVariable("ltl_biomass", DataType.FLOAT, new Dimension[]{timeDim, ltlDim, linesDim, columnsDim});
        nc.addVariableAttribute("ltl_biomass", "units", "tons per cell");
        nc.addVariableAttribute("ltl_biomass", "description", "plankton biomass in osmose cell, in tons integrated on water column, per group and per cell");
        nc.addVariableAttribute("ltl_biomass", "_FillValue", -99.f);
        nc.addVariable("latitude", DataType.FLOAT, new Dimension[]{linesDim, columnsDim});
        nc.addVariableAttribute("latitude", "units", "degree");
        nc.addVariableAttribute("latitude", "description", "latitude of the center of the cell");
        nc.addVariable("longitude", DataType.FLOAT, new Dimension[]{linesDim, columnsDim});
        nc.addVariableAttribute("longitude", "units", "degree");
        nc.addVariableAttribute("longitude", "description", "longitude of the center of the cell");
        /*
         * Add global attributes
         */
        StringBuilder str = new StringBuilder();
        for (int kltl = 0; kltl < getConfiguration().getNPlankton(); kltl++) {
            str.append(kltl);
            str.append("=");
            str.append(getSimulation().getPlankton(kltl));
            str.append(" ");
        }
        nc.addGlobalAttribute("dimension_ltl", str.toString());
        try {
            /*
             * Validates the structure of the NetCDF file.
             */
            nc.create();
            /*
             * Writes variable longitude and latitude
             */
            ArrayFloat.D2 arrLon = new ArrayFloat.D2(getGrid().getNbLines(), getGrid().getNbColumns());
            ArrayFloat.D2 arrLat = new ArrayFloat.D2(getGrid().getNbLines(), getGrid().getNbColumns());
            for (Cell cell : getGrid().getCells()) {
                arrLon.set(getGrid().getNbLines() - cell.get_igrid() - 1, cell.get_jgrid(), cell.getLon());
                arrLat.set(getGrid().getNbLines() - cell.get_igrid() - 1, cell.get_jgrid(), cell.getLat());
            }
            nc.write("longitude", arrLon);
            nc.write("latitude", arrLat);
        } catch (IOException ex) {
            Logger.getLogger(LTLIndicator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidRangeException ex) {
            Logger.getLogger(LTLIndicator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private String getFilename() {
        File path = new File(getConfiguration().getOutputPathname() + getConfiguration().getOutputFolder());
        StringBuilder filename = new StringBuilder(path.getAbsolutePath());
        filename.append(File.separatorChar);
        filename.append("planktonBiomass");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getOutputPrefix());
        filename.append("_ltlbiomass_integrated_");
        filename.append("Simu");
        filename.append(getSimulation().getReplica());
        filename.append(".nc.part");
        return filename.toString();
    }
}
