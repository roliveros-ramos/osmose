package fr.ird.osmose;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.logging.Level;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import fr.ird.osmose.process.mortality.FishingMortality;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestMPA {
    
    private Configuration cfg;
    FishingMortality mortality;
    
    /** Prepare the input data for the test. */
    @BeforeAll
    public void prepareData() throws Exception {
        
        Osmose osmose = Osmose.getInstance();
        osmose.getLogger().setLevel(Level.SEVERE);
        String configurationFile = this.getClass().getClassLoader().getResource("osmose-eec/eec_all-parameters.csv").getFile();
        
        // Adding HashMap to overwrite default setting
        HashMap<String, String> cmd = new HashMap<>();
        
        // define MPA
        cmd.put("mpa.start.year.mpa0", "0");
        cmd.put("mpa.end.year.mpa0", "1");
        Path mpaPath = Paths.get(this.getClass().getClassLoader().getResource("osmose-eec").getPath());
        String mpaFileName = mpaPath.resolve("mpa").resolve("full_mpa.csv").toString();
        cmd.put("mpa.file.mpa0", mpaFileName);
        
        // deactivate new fisheries and use old ones instead
        cmd.put("fisheries.enabled", "false");
        cmd.put("mortality.fishing.rate.sp0", "0.07");
        cmd.put("mortality.fishing.rate.sp1", "0.07");
        cmd.put("mortality.fishing.rate.sp2", "0.07");
        cmd.put("mortality.fishing.rate.sp3", "0.07");
        cmd.put("mortality.fishing.rate.sp4", "0.07");
        cmd.put("mortality.fishing.rate.sp5", "0.07");
        cmd.put("mortality.fishing.rate.sp6", "0.07");
        cmd.put("mortality.fishing.rate.sp7", "0.07");
        cmd.put("mortality.fishing.rate.sp8", "0.07");
        cmd.put("mortality.fishing.rate.sp9", "0.07");
        cmd.put("mortality.fishing.rate.sp10", "0.07");
        cmd.put("mortality.fishing.rate.sp11", "0.07");
        cmd.put("mortality.fishing.rate.sp12", "0.07");
        cmd.put("mortality.fishing.rate.sp13", "0.07");
        
        // Test the standard configuration
        osmose.readConfiguration(configurationFile, cmd);
        cfg = osmose.getConfiguration();
        cfg.init();
        
        mortality = new FishingMortality(0);
        mortality.init();
        
    }
    
    @Test
    public void test1() {
        assertEquals(0, 0);      
    }
    
}
