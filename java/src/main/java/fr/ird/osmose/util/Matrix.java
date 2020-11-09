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

package fr.ird.osmose.util;

import au.com.bytecode.opencsv.CSVReader;
import fr.ird.osmose.IAggregation;
import fr.ird.osmose.stage.ClassGetter;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 * Class that manages the reading and use of accesibility matrix.
 *
 * @author Nicolas Barrier
 */
public class Matrix extends OsmoseLinker {

    /**
     * Number of preys (lines in the file).
     */
    private int nPreys;

    /**
     * Number of predators (columns in the file).
     */
    private int nPred;

    /**
     * Accessibility values of dimension (nprey, nclass).
     */
    private double[][] accessibilityMatrix;

    /**
     * Accessibility filename.
     */
    private final String filename;

    /**
     * Upper bounds of the prey size class. Read in the file.
     */
    private float[] classPrey;

    /**
     * Names of the preys.
     */
    private String[] namesPrey;

    /**
     * Upper bounds of the pred size class.
     */
    private float[] classPred;

    /**
     * Names of the predators.
     */
    private String[] namesPred;

    private final ClassGetter classGetter;

    /*public abstract int getIndexPred(String namePred);

    public abstract int getIndexPrey(String namePrey);

    public abstract int getIndexPrey(IAggregation pred);

    public abstract int getIndexPred(IAggregation prey);
     */
    /**
     * Class constructor.The reading of the file is done here
     *
     * @param filename
     * @param classGetter
     */
    public Matrix(String filename, ClassGetter classGetter) {
        this.filename = filename;
        this.classGetter = classGetter;
        this.read();
    }

    /**
     * Reads the accessibility file. The first column and the header are now
     * used to reconstruct the upper size class
     */
    public void read() {

        try (CSVReader reader = new CSVReader(new FileReader(filename), Separator.guess(filename).getSeparator())) {

            // Read all the lines
            List<String[]> lines = reader.readAll();

            // extract the  number of preys (removing the header)
            nPreys = lines.size() - 1;

            namesPrey = new String[nPreys];
            classPrey = new float[nPreys];

            // process the header, i.e defines characteristics for predators.
            String[] header = lines.get(0);

            // extracts the number of pred (nheaders minus first element)
            nPred = header.length - 1;

            classPred = new float[nPred];
            namesPred = new String[nPred];

            // Process the header to extract the properties of predator (class, etc.)
            for (int ipred = 0; ipred < header.length - 1; ipred++) {
                String predString = header[ipred + 1];
                int index = predString.lastIndexOf('<');
                if (index < 0) {
                    classPred[ipred] = Float.MAX_VALUE;
                    namesPred[ipred] = predString.trim();
                } else {
                    namesPred[ipred] = predString.substring(0, index - 1).trim();
                    classPred[ipred] = Float.valueOf(predString.substring(index + 1, predString.length()));
                }
            }

            // Initialize the data matrix
            this.accessibilityMatrix = new double[nPreys][nPred];

            // Loop over all the lines of the file, avoiding header
            for (int iprey = 0; iprey < lines.size() - 1; iprey++) {

                // Read the line for the given prey
                String[] lineStr = lines.get(iprey + 1);

                // Recovering the column name to get prey names and class
                String preyString = lineStr[0];
                int index = preyString.lastIndexOf('<');
                if (index < 0) {
                    classPrey[iprey] = Float.MAX_VALUE;
                    namesPrey[iprey] = preyString.trim();
                } else {
                    namesPrey[iprey] = preyString.substring(0, index - 1).trim();
                    classPrey[iprey] = Float.valueOf(preyString.substring(index + 1, preyString.length()));
                }

                for (int ipred = 0; ipred < lineStr.length - 1; ipred++) {
                    this.accessibilityMatrix[iprey][ipred] = Double.valueOf(lineStr[ipred + 1]);
                }

            }

        } catch (IOException ex) {
            error("Error loading accessibility matrix from file " + filename, ex);
        }
    }

    /**
     * Recovers the name of the accessibility file.
     * @return 
     */
    public String getFile() {
        return this.filename;
    }

    public double getValue(int iprey, int ipred) {
        return this.accessibilityMatrix[iprey][ipred];
    }

    public int getNPred() {
        return this.nPred;
    }

    public int getNPrey() {
        return this.nPreys;
    }

    public String getPreyName(int i) {
        return namesPrey[i];
    }

    public String getPredName(int i) {
        return namesPred[i];
    }

    public double getPreyClass(int i) {
        return classPrey[i];
    }

    public double getPredClass(int i) {
        return classPred[i];
    }

    /**
     * Extracts the matrix column for the given predator.
     *
     * Based on full correspondance of the name (class < thres).
     *
     * @param pred
     * @return
     */
    public int getIndexPred(IAggregation pred) {

        String predname = pred.getSpeciesName();

        if (this.classGetter == null) {
            for (int i = 0; i < this.getNPred(); i++) {
                if (predname.equals(this.getPredName(i))) {
                    return i;
                }
            }
        } else {
            double classVal = classGetter.getVariable(pred);
            for (int i = 0; i < this.getNPred(); i++) {
                if (predname.equals(this.getPredName(i)) && (classVal < this.getPredClass(i))) {
                    return i;
                }
            }
        }
        String message = String.format("No accessibility found for predator %s class %f", pred.getSpeciesName(), classGetter.getVariable(pred));
        error(message, new IllegalArgumentException());
        return -1;
    }

    /**
     * Extracts the matrix column for the given prey.
     *
     * Based on full correspondance of the name (class < thres).
     *
     * @param prey
     * @return
     */
    public int getIndexPrey(IAggregation prey) {
        
        String preyname = prey.getSpeciesName();
        
        if (this.classGetter == null) {
            for (int i = 0; i < this.getNPrey(); i++) {
                if (preyname.equals(this.getPreyName(i))) {
                    return i;
                }
            }
        } else {
            double classVal = classGetter.getVariable(prey);
            for (int i = 0; i < this.getNPrey(); i++) {
                if (preyname.equals(this.getPreyName(i)) && (classVal < this.getPreyClass(i))) {
                    return i;
                }
            }
        }
        String message = String.format("No accessibility found for prey %s class %f", prey.getSpeciesName(), classGetter.getVariable(prey));
        error(message, new IllegalArgumentException());
        return -1;

    }
    
    /** *  Extracts the matrix column for the given predator.Based on full correspondance of the name (class < thres).
     * 
     *
     * @param name 
     * @param pred
     * @return 
     */
    public int getIndexPred(String name) { 
        for (int i = 0; i < this.getNPred(); i++) {
            if (name.equals(this.getPredName(i))) {
                return i;
            }
        }
        String message = String.format("No catchability found for fishery %s", name);
        error(message, new IllegalArgumentException());       
        return -1;
    }

    /** *  Extracts the matrix column for the given prey.Based on full correspondance of the name (class < thres).
     * 
     *
     * @param name 
     * @param prey
     * @return 
     */
    public int getIndexPrey(String name) {
        for (int i = 0; i < this.getNPrey(); i++) {
            if (name.equals(this.getPreyName(i))) {
                return i;
            }
        }
        String message = String.format("No catchability found for prey %s", name);
        error(message, new IllegalArgumentException());       
        return -1;
    }

}
