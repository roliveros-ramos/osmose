package fr.ird.osmose;

/********************************************************************************
 * <p>Title : Grid class</p>
 *
 * <p>Description : grid of Osmose model, divided into cells (Cell) 
 * Include a function defining neighbors of each cell </p>
 *
 * <p>Copyright : Copyright (c) may 2009</p>
 *
 * <p>Society : IRD, France </p>
 *
 * @author Yunne Shin, Morgane Travers
 * @version 2.1 
 ******************************************************************************** 
 */
public class OriginalGrid extends AbstractGrid {

    /*
     * ********
     * * Logs *
     * ********
     * 2011/04/18 phv
     * Added a getCells() function
     * 2011/04/11 phv
     * Deprecated identifySpatialGroups since it looks Benguela specific
     * function.
     * ***
     * 2011/04/07 phv
     * Encapsulated all the variables and propagated the changes to the other
     * classes.
     * Deleted the identifyNeighbors function since neighbors variable has been
     * deleted in Cell.java.
     * Added function getNeighborCells(Cell cell) that is called by Osmose when
     * doing the random sorting of the cells for the random spatial distribution
     * ***
     */
////////////////////////////
// Definition of the methods
////////////////////////////
    /*
     * 
     */
    @Override
    public void readParameters() {
        int numSerie = getOsmose().numSerie;
        /* grid dimension */
        setNbLines(getOsmose().gridLinesTab[numSerie]);
        setNbColumns(getOsmose().gridColumnsTab[numSerie]);

        /* geographical extension of the grid */
        setLatMax(getOsmose().upLeftLatTab[numSerie]);
        setLatMin(getOsmose().lowRightLatTab[numSerie]);
        setLongMax(getOsmose().lowRightLongTab[numSerie]);
        setLongMin(getOsmose().upLeftLongTab[numSerie]);
    }

    /*
     * Create a regular orthogonal grid and specify latitude and longitude
     * of each cell.
     */
    @Override
    public Cell[][] makeGrid() {

        float dLat = (getLatMax() - getLatMin()) / (float) getNbLines();
        float dLong = (getLongMax() - getLongMin()) / (float) getNbColumns();

        Cell[][] grid = new Cell[getNbLines()][getNbColumns()];
        float latitude, longitude;
        for (int i = 0; i < getNbLines(); i++) {
            latitude = getLatMax() - (float) (i + 0.5f) * dLat;
            for (int j = 0; j < getNbColumns(); j++) {
                longitude = getLongMin() + (float) (j + 0.5) * dLong;
                grid[i][j] = new Cell(i, j, latitude, longitude);
            }
        }
        return grid;
    }

    @Override
    public int getStride() {
        return 1;
    }
}

