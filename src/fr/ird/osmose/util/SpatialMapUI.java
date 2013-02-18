/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.util;

import fr.ird.osmose.Cell;
import fr.ird.osmose.GridMap;
import fr.ird.osmose.Osmose;
import fr.ird.osmose.grid.IGrid;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 *
 * @author pverley
 */
public class SpatialMapUI extends JPanel {

    ///////////////////////////////
// Declaration of the variables
///////////////////////////////
    /**
     * Dimension of the component.
     */
    private static int hi, wi;
    /**
     * Minimum latitude of the domain to display
     */
    private static double latmin;
    /**
     * Maximum latitude of the domain to display
     */
    private static double latmax;
    /**
     * Minimum longitude of the domain to display
     */
    private static double lonmin;
    /**
     * Maximum longitude of the domain to display
     */
    private static double lonmax;
    /**
     * BufferedImage in which the background (cost + bathymetry) has been
     * drawn.
     */
    private static BufferedImage background;
    /**
     * Associated {@code RenderingHints} object
     */
    private static RenderingHints hints = null;
    private static final double ONE_DEG_LATITUDE_IN_METER = 111138.d;
    private int height = 800, width = 600;
    private boolean isGridVisible = false;
    private static GridMap map;

    private static int numMap = 5;

///////////////
// Constructors
///////////////
    /**
     * Constructs an empty <code>SimulationUI</code>, intializes the range of
     * the domain and the {@code RenderingHints}.
     */
    public SpatialMapUI() {

        hi = -1;
        wi = -1;

        hints = new RenderingHints(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        hints.put(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        hints.put(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON);

    }

    @Override
    public void paintComponent(Graphics g) {

        int h = getHeight();
        int w = getWidth();

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHints(hints);

        /** Clear the graphics */
        g2.clearRect(0, 0, w, h);

        /* Redraw the background when size changed */
        if (hi != h || wi != w) {
            drawBackground(g2, w, h);
            hi = h;
            wi = w;
        }
        /* Draw the background into the graphics */
        g2.drawImage(background, 0, 0, this);
    }

    private void drawBackground(Graphics2D g2, int w, int h) {

        background = g2.getDeviceConfiguration().createCompatibleImage(w, h);
        Graphics2D graphic = background.createGraphics();
        graphic.setColor(new Color(223, 212, 200));
        graphic.fillRect(0, 0, w, h);

        CellUI cell = new CellUI();
        for (int i = getGrid().getNbLines() - 1; i-- > 0;) {
            for (int j = getGrid().getNbColumns() - 1; j-- > 0;) {
                cell.draw(i, j, w, h);
                graphic.setColor(cell.getColor(i, j));
                graphic.fillPolygon(cell);
                if (isGridVisible) {
                    graphic.setColor(Color.LIGHT_GRAY);
                    graphic.drawPolygon(cell);
                }
            }
        }
    }

    public void setGridVisible(boolean visible) {
        isGridVisible = visible;
        repaintBackground();
    }

    /**
     * Forces the background to repaint.
     */
    public void repaintBackground() {

        hi = -1;
        wi = -1;
        repaint();
    }

    /**
     * Transforms particle (x, y) coordinates into a screen point.
     *
     * @param xgrid a double, the particle x-coordinate
     * @param ygrid a double, the particle y-coordinate
     * @param w the width of the component
     * @param h the height of the component
     * @return an int[], the corresponding (x-sreen, y-screen) coordinates.
     */
    private int[] grid2Screen(double xgrid, double ygrid, int w, int h) {

        int[] point = new int[2];
        int igrid, jgrid;
        double dx, dy;
        double[] p1, p2, p3, p4;

        igrid = (int) xgrid;
        jgrid = (int) ygrid;
        dx = xgrid - igrid;
        dy = ygrid - jgrid;
        p1 = grid2Screen(igrid, jgrid, w, h);
        p2 = grid2Screen(igrid + 1, jgrid, w, h);
        p3 = grid2Screen(igrid, jgrid + 1, w, h);
        p4 = grid2Screen(igrid + 1, jgrid + 1, w, h);

        for (int n = 0; n < 2; n++) {
            double interp = (1.d - dx) * (1.d - dy) * p1[n] + dx * (1.d - dy) * p2[n] + (1.d - dx) * dy * p3[n] + dx * dy * p4[n];
            point[n] = Double.isNaN(interp)
                    ? -1
                    : (int) interp;
        }
        return point;
    }

    /**
     * Transforms a grid cell coordinate (i, j) into a screen point.
     *
     * @param igrid an int, the i-grid coordinate
     * @param jgrid an int, the j-grid coordiante
     * @param w the width of the component
     * @param h the height of the component
     * @return a double[], the corresponding (x-sreen, y-screen) coordinates.
     */
    private double[] grid2Screen(int igrid, int jgrid, int w, int h) {

        double[] point = new double[2];

        point[0] = w * ((getGrid().getCell(igrid, jgrid).getLon() - lonmin)
                / Math.abs(lonmax - lonmin));
        point[1] = h * (1.d - ((getGrid().getCell(igrid, jgrid).getLat() - latmin)
                / Math.abs(latmax - latmin)));

        return (point);
    }

    public void init() {

        latmin = getGrid().getLatMin();
        latmax = getGrid().getLatMax();
        lonmin = getGrid().getLongMin();
        lonmax = getGrid().getLongMax();

        double avgLat = 0.5d * (latmin + latmax);

        double dlon = Math.abs(lonmax - lonmin) * ONE_DEG_LATITUDE_IN_METER * Math.cos(Math.PI * avgLat / 180.d);
        double dlat = Math.abs(latmax - latmin) * ONE_DEG_LATITUDE_IN_METER;

        double ratio = dlon / dlat;
        width = (int) (height * ratio);
        /*if (ratio > 1) {
        width = (int) (height * ratio);
        } else if (ratio != 0.d) {
        height = (int) (width / ratio);
        }*/
        //setPreferredSize(new Dimension(width, height));
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    /**
     * Computes the geodesic distance between the two points
     * (lat1, lon1) and (lat2, lon2)
     * @param lat1 a double, the latitude of the first point
     * @param lon1 a double, the longitude of the first point
     * @param lat2 double, the latitude of the second point
     * @param lon2 double, the longitude of the second point
     * @return a double, the curvilinear absciss s(A[lat1, lon1]B[lat2, lon2])
     */
    public static double geodesicDistance(double lat1, double lon1, double lat2, double lon2) {

        double d = 0.d;
        double lat1_rad = Math.PI * lat1 / 180.d;
        double lat2_rad = Math.PI * lat2 / 180.d;
        double lon1_rad = Math.PI * lon1 / 180.d;
        double lon2_rad = Math.PI * lon2 / 180.d;

        d = 2 * 6367000.d
                * Math.asin(Math.sqrt(Math.pow(Math.sin((lat2_rad - lat1_rad) / 2), 2)
                + Math.cos(lat1_rad) * Math.cos(lat2_rad) * Math.pow(Math.sin((lon2_rad - lon1_rad) / 2), 2)));

        return d;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    public static IGrid getGrid() {
        return Osmose.getInstance().getGrid();
    }

    public static Osmose getOsmose() {
        return Osmose.getInstance();
    }

    private boolean isInMap(int i, int j) {
        return map.getValue(i, j) > 0.f;
    }

    private class CellUI extends Polygon {

        ///////////////////////////////
        // Declaration of the constants
        ///////////////////////////////
        /**
         * Color at the bottom.
         */
        final private Color bottom = new Color(0, 0, 150);
        /**
         * Color at the surface.
         */
        final private Color surface = Color.CYAN;
        ///////////////////////////////
        // Declaration of the variables
        ///////////////////////////////
        /**
         * The (x-screen, y-screen) coordinates of the quadrilateral.
         * point[0:3][0:1] first dimension refers to the number of points (4
         * in this case) and the second dimension, the (x, y) coordinates.
         */
        private int[][] points;

        ///////////////
        // Constructors
        ///////////////
        /**
         * Constructs an empty <code>CellUI</code>
         */
        public CellUI() {

            points = new int[4][2];
        }

        ////////////////////////////
        // Definition of the methods
        ////////////////////////////
        /**
         * Computes the coordinates of the quadrilateral, around the specified
         * grid point(i, j).
         *
         * @param i an int, the i-coordinate of the cell
         * @param j an int, the j-coordinate of the cell
         * @param w the width of the component
         * @param h the height of the component
         */
        public void draw(int i, int j, int w, int h) {

            this.reset();
            points[0] = grid2Screen(i - 0.5f, j - 0.5f, w, h);
            points[1] = grid2Screen(i + 0.5f, j - 0.5f, w, h);
            points[2] = grid2Screen(i + 0.5f, j + 0.5f, w, h);
            points[3] = grid2Screen(i - 0.5f, j + 0.5f, w, h);

            for (int n = 0; n < 4; n++) {
                if (points[n][0] < 0 || points[n][1] < 0) {
                    reset();
                    return;
                }
                addPoint(points[n][0], points[n][1]);
            }
        }

        private Color getColor(int i, int j) {

            if (getGrid().getCell(i, j).isMPA()) {
                return Color.GREEN;
            } else if (isInMap(i, j)) {
                return Color.MAGENTA;
            } else if (getGrid().getCell(i, j).isLand()) {
                return Color.DARK_GRAY;
            } else {
                return Color.CYAN;
            }
        }
        //---------- End of class CellUI
    }

    private static void getCellSize(int i, int j) {

        double lat1 = getGrid().getCell(i, j).getLat();
        double lon1 = getGrid().getCell(i, j).getLon();
        double lat2 = getGrid().getCell(i, j + 1).getLat();
        double lon2 = getGrid().getCell(i, j + 1).getLon();
        double gd = geodesicDistance(lat1, lon1, lat2, lon2);

        System.out.println("gd1: " + gd + " meters");

        lat1 = getGrid().getCell(i, j).getLat();
        lon1 = getGrid().getCell(i, j).getLon();
        lat2 = getGrid().getCell(i + 1, j).getLat();
        lon2 = getGrid().getCell(i + 1, j).getLon();
        gd = geodesicDistance(lat1, lon1, lat2, lon2);

        System.out.println("gd2: " + gd + " meters");
    }

    private static void writeGridCSV() {
        FileWriter fw = null;

        try {
            fw = new FileWriter("grid_osmose_stride" + getOsmose().getGrid().getStride() + ".csv");
            PrintWriter pw = new PrintWriter(fw);
            pw.println("lat , lon, mask (land = 0 water = 1)");
            int nbL = getOsmose().getGrid().getNbLines();
            int nbC = getOsmose().getGrid().getNbColumns();
            for (int l = 0; l < nbL; l++) {
                for (int c = 0; c < nbC; c++) {
                    Cell cell = getOsmose().getGrid().getCell(l, c);
                    pw.print(cell.getLat());
                    pw.print(" , ");
                    pw.print(cell.getLon());
                    pw.print(" , ");
                    pw.println(cell.isLand() ? 0 : 1);
                }
            }
            //Flush the output to the file
            pw.flush();
            //Close the Print Writer
            pw.close();
            //Close the File Writer
            fw.close();
        } catch (IOException ex) {
            Logger.getLogger(GridUI.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fw.close();
            } catch (IOException ex) {
                Logger.getLogger(GridUI.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }

    public static void main(String args[]) {

        getOsmose().loadArgs(args);
        getOsmose().init();
        getOsmose().loadMPAs();
        getOsmose().getSimulation().init();
        map = getOsmose().getMap(numMap);

        SpatialMapUI grid = new SpatialMapUI();
        grid.init();
        grid.setGridVisible(true);
        //1. Create the frame.
        JFrame frame = new JFrame(getOsmose().getMapDetails(numMap));

        //2. Optional: What happens when the frame closes?
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //3. Create components and put them in the frame.
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(grid);
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);

        //4. Size the frame.
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        scrollPane.setPreferredSize(grid.getSize());
        scrollPane.revalidate();
        frame.pack();
        frame.setLocationRelativeTo(null);

        //5. Show it.
        frame.setVisible(true);
    }
}
