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
package fr.ird.osmose;

import fr.ird.osmose.util.OsmoseLinker;

/**
 * This class represents a species. It is characterized by the following
 * variables:
 * <ul>
 * <li>name</li>
 * <li>lifespan</li>
 * <li>Von Bertalanffy growth parameters</li>
 * <li>Threshold age for applying Von Bertalanffy growth model</li>
 * <li>Allometric parameters</li>
 * <li>Egg weight and size</li>
 * <li>Size at maturity</li>
 * <li>Threshold age for age class zero</li>
 * </ul>
 *
 * @author P.Verley (philippe.verley@ird.fr)
 * @version 3.0b 2013/09/01
 */
public class Species extends OsmoseLinker {

///////////////////////////////
// Declaration of the variables
///////////////////////////////
    /**
     * Trophic level of an egg.
     */
    public static final float TL_EGG = 3f;
    /**
     * Index of the species. [0 : numberTotalSpecies - 1]
     */
    private final int index;
    /**
     * Name of the species. Parameter <i>species.name.sp#</i>
     */
    private final String name;
    /**
     * Lifespan expressed in number of time step. A lifespan of 5 years means
     * that a fish will die as soon as it turns 5 years old. Parameter
     * <i>species.lifespan.sp#</i>
     */
    private final int lifespan;
    /**
     * Allometric parameters. Parameters
     * <i>species.length2weight.condition.factor.sp#</i> and
     * <i>species.length2weight.allometric.power.sp#</i>
     */
    private final float c, bPower;
    /**
     * Size (cm) at maturity. Parameter <i>species.maturity.size.sp#</i>
     */
    private final float sizeMaturity;
    /**
     * Age (year) at maturity. Parameter <i>species.maturity.age.sp#</i>
     */
    private final float ageMaturity;
    /**
     * Threshold age (year) for age class zero. It is the age from which target
     * biomass should be considered as eggs and larvae stages are generally not
     * considered. Parameter <i>output.cutoff.age.sp#</i>
     */
    private final int ageClassZero;
    /**
     * Size (cm) of eggs. Parameter <i>species.egg.size.sp#</i>
     */
    private final float eggSize;
    /**
     * Weight (gram) of eggs. Parameter <i>species.egg.weight.sp#</i>
     */
    private final float eggWeight;

//////////////
// Constructor
//////////////
    /**
     * Create a new species
     *
     * @param index, an integer, the index of the species
     * {@code [0, nbTotSpecies - 1]}
     */
    public Species(int index) {

        this.index = index;
        // Initialization of parameters
        name = getConfiguration().getString("species.name.sp" + index);
        if (!name.matches("^[a-zA-Z0-9]*$")) {
                error("Species name must contain alphanumeric characters only. Please rename " + name, null);
            }
        c = getConfiguration().getFloat("species.length2weight.condition.factor.sp" + index);
        bPower = getConfiguration().getFloat("species.length2weight.allometric.power.sp" + index);
        if (!getConfiguration().isNull("species.maturity.size.sp" + index)) {
            sizeMaturity = getConfiguration().getFloat("species.maturity.size.sp" + index);
            ageMaturity = Float.MAX_VALUE;
        } else {
            ageMaturity = getConfiguration().getFloat("species.maturity.age.sp" + index);
            sizeMaturity = Float.MAX_VALUE;
        }
        float age0 = getConfiguration().getFloat("output.cutoff.age.sp" + index);
        ageClassZero = (int) Math.ceil(age0 * getConfiguration().getNStepYear());
        eggSize = getConfiguration().getFloat("species.egg.size.sp" + index);
        eggWeight = getConfiguration().getFloat("species.egg.weight.sp" + index);
        float agemax = getConfiguration().getFloat("species.lifespan.sp" + index);
        lifespan = (int) Math.round(agemax * getConfiguration().getNStepYear());
    }

//////////////////////////////
// Definition of the functions
//////////////////////////////
    /**
     * Computes the weight, in gram, corresponding to the given length, in
     * centimetre.
     *
     * @param length, the length in centimetre
     * @return the weight in gram for this {@code length}
     */
    public float computeWeight(float length) {
        return (float) (c * (Math.pow(length, bPower)));
    }

    /**
     * Returns the lifespan of the species. Parameter
     * <i>species.lifespan.sp#</i>
     *
     * @return the lifespan, in number of time step
     */
    public int getLifespanDt() {
        return lifespan;
    }

    /**
     * Returns the index of the species.
     *
     * @return the index of the species
     */
    public int getIndex() {
        return index;
    }

    /**
     * Returns the name of the species. Parameter <i>species.name.sp#</i>
     *
     * @return the name of the species
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the size of an egg. Parameter <i>species.egg.size.sp#</i>
     *
     * @return the size of an egg in centimeter
     */
    public float getEggSize() {
        return eggSize;
    }

    /**
     * Returns the weight of an egg in gram. Parameter
     * <i>species.egg.weight.sp#</i>
     *
     * @return the weight of an egg in gram
     */
    public float getEggWeight() {
        return eggWeight;
    }

    /**
     * Returns the threshold of age class zero, in number of time step. This
     * parameter allows to discard schools younger that this threshold in the
     * calculation of the indicators when parameter <i>output.cutoff.enabled</i>
     * is set to {@code true}. Parameter <i>output.cutoff.age.sp#</i>
     *
     * @return the threshold age of class zero, in number of time step
     */
    public int getAgeClassZero() {
        return ageClassZero;
    }

    public boolean isSexuallyMature(School school) {
        return (school.getLength() >= sizeMaturity) || (school.getAge() >= ageMaturity);
    }

    /**
     * Just keep it as a reminder for a future vulnerability function
     *
     * @param biomass
     * @return
     */
    private boolean isVulnerable(double biomass) {
        double Bv = 0.d;
        double Sv = 1.d;
        return (Math.random() > (1.d / (1.d + Math.exp(Sv * (Bv - biomass)))));
    }
}
