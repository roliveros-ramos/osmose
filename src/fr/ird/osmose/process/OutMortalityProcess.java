/*
 # * To change this template, choose Tools | Templates
 # * and open the template in the editor.
 Copyright IRD (2013)
 Contributors: Philippe VERLEY

 philippe.verley@ird.fr

 This software is a computer program whose purpose is to [describe
 functionalities and technical features of your software].

 This software is governed by the CeCILL-B license under French law and
 abiding by the rules of distribution of free software.  You can  use, 
 modify and/ or redistribute the software under the terms of the CeCILL-B
 license as circulated by CEA, CNRS and INRIA at the following URL
 "http://www.cecill.info". 

 As a counterpart to the access to the source code and  rights to copy,
 modify and redistribute granted by the license, users are provided only
 with a limited warranty  and the software's author,  the holder of the
 economic rights,  and the successive licensors  have only  limited
 liability. 

 In this respect, the user's attention is drawn to the risks associated
 with loading,  using,  modifying and/or developing or reproducing the
 software by the user in light of its specific status of free software,
 that may mean  that it is complicated to manipulate,  and  that  also
 therefore means  that it is reserved for developers  and  experienced
 professionals having in-depth computer knowledge. Users are therefore
 encouraged to load and test the software's suitability as regards their
 requirements in conditions enabling the security of their systems and/or 
 data to be ensured and,  more generally, to use and operate it in the 
 same conditions as regards security. 

 The fact that you are presently reading this means that you have had
 knowledge of the CeCILL-B license and that you accept its terms.
 */
package fr.ird.osmose.process;

import fr.ird.osmose.School;

/**
 * This class controls mortality for schools that are temporarilly out of the
 * simulated domain.
 * @author pverley
 */
public class OutMortalityProcess extends AbstractProcess {

    /*
     * Out mortality rate [dt-1]
     */
    private float[] Z;

    public OutMortalityProcess(int indexSimulation) {
        super(indexSimulation);
    }

    @Override
    public void init() {

        int nSpecies = getConfiguration().getNSpecies();
        int nStepYear = getConfiguration().getNStepYear();
        Z = new float[nSpecies];
        for (int i = 0; i < nSpecies; i++) {
            if (!getConfiguration().isNull("mortality.out.rate.sp" + i)) {
                Z[i] = getConfiguration().getFloat("mortality.out.rate.sp" + i) / nStepYear;
            }
        }
    }
    
    public float getZ(School school) {
        return Z[school.getSpeciesIndex()];
    }

    @Override
    public void run() {
        // Apply Z mortality on schools out of the simulated domain
        for (School school : getSchoolSet().getOutSchools()) {
                double nDead = school.getInstantaneousAbundance() * (1 - Math.exp(-getZ(school)));
                if (nDead > 0.d) {
                    school.setNdeadOut(nDead);
                }
        }
    }
}
