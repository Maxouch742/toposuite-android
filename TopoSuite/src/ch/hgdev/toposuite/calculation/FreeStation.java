package ch.hgdev.toposuite.calculation;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ch.hgdev.toposuite.App;
import ch.hgdev.toposuite.R;
import ch.hgdev.toposuite.SharedResources;
import ch.hgdev.toposuite.calculation.activities.freestation.FreeStationActivity;
import ch.hgdev.toposuite.points.Point;
import ch.hgdev.toposuite.utils.MathUtils;

public class FreeStation extends Calculation {
    private static final String     STATION_NUMBER = "station_number";
    private static final String     MEASURES       = "measures";
    private static final String     INSTRUMENT     = "instrument";

    private int                     stationNumber;
    private ArrayList<Measure>      measures;
    /** Height of the instrument (I). */
    private double                  i;

    private Point                   stationResult;
    private final ArrayList<Result> results;
    private double                  unknownOrientation;
    /**
     * Mean east error.
     */
    private double                  sE;

    /**
     * Mean north error.
     */
    private double                  sN;

    /**
     * Mean altitude error.
     */
    private double                  sA;

    /**
     * Mean FS.
     */
    private double                  meanFS;

    public FreeStation(int _stationNumber, double _i, boolean hasDAO) {
        super(CalculationType.FREESTATION, App.getContext().getString(
                R.string.title_activity_free_station), hasDAO);

        this.stationNumber = _stationNumber;
        this.i = _i;
        this.measures = new ArrayList<Measure>();
        this.results = new ArrayList<Result>();

        if (hasDAO) {
            SharedResources.getCalculationsHistory().add(0, this);
        }
    }

    public FreeStation(int _stationNumber, boolean hasDAO) {
        this(_stationNumber, 0.0, hasDAO);
    }

    public FreeStation(boolean hasDAO) {
        this(0, hasDAO);
    }

    public FreeStation(long id, Date lastModification) {
        super(id, CalculationType.FREESTATION, App.getContext().getString(
                R.string.title_activity_free_station),
                lastModification, true);

        this.measures = new ArrayList<Measure>();
        this.results = new ArrayList<Result>();
    }

    @Override
    public void compute() {
        if (this.measures.size() < 1) {
            return;
        }

        this.results.clear();
        this.sE = 0.0;
        this.sN = 0.0;
        this.sA = 0.0;
        this.meanFS = 0.0;

        double unknOrient = 0.0;

        // fictive coordinates centroid
        double centroidYFict = 0.0;
        double centroidXFict = 0.0;
        Point centroidFict;

        // cadastral coordinates centroid
        double centroidYCadast = 0.0;
        double centroidXCadast = 0.0;
        Point centroidCadast;

        // altitude related stuff
        double totalWeights = 0.0;
        double meanAltitude = 0.0;
        int nbAltitudes = 0;

        for (Measure m : this.measures) {
            Point res = new Point(m.getPoint().getNumber(), m.getPoint().getEast(),
                    m.getPoint().getNorth(), m.getPoint().getAltitude(),
                    false, false);

            double horizDist = m.getDistance() * Math.sin(
                    MathUtils.gradToRad(m.getZenAngle()));
            double gis = MathUtils.modulo400(unknOrient + m.getHorizDir());

            if (!MathUtils.isZero(m.getLatDepl())) {
                double angle = Math.asin(Math.abs(m.getLatDepl() / horizDist));
                horizDist = Math.abs(m.getLatDepl()) / Math.tan(angle);

                // correction of the direction [g]
                gis += (MathUtils.isPositive(m.getLatDepl()))
                        ? MathUtils.radToGrad(angle) : -MathUtils.radToGrad(angle);
            }

            res.setEast(MathUtils.pointLanceEast(0, gis, horizDist));
            res.setNorth(MathUtils.pointLanceNorth(0, gis, horizDist));

            double weight = 0.0;
            if (MathUtils.isPositive(m.getPoint().getAltitude())) {
                res.setAltitude(res.getAltitude() - MathUtils.nivellTrigo(
                        horizDist, m.getZenAngle(), this.i, m.getS(), 0.0));
                weight = 1 / Math.pow((horizDist / 1000), 2);
                totalWeights += weight;
                meanAltitude += weight * res.getAltitude();
                nbAltitudes++;
            }

            this.results.add(new Result(res, weight));

            // centroid calculation
            centroidYFict += res.getEast();
            centroidXFict += res.getNorth();

            centroidYCadast += m.getPoint().getEast();
            centroidXCadast += m.getPoint().getNorth();
        }

        int n = this.results.size();
        centroidFict = new Point(0, centroidYFict / n, centroidXFict / n,
                0.0, false, false);
        centroidCadast = new Point(0, centroidYCadast / n, centroidXCadast / n,
                0.0, false, false);

        List<IntermediateResults> intermRes = new ArrayList<IntermediateResults>();
        double meanRotations = 0.0;
        double meanConstants = 0.0;

        for (int i = 0; i < this.results.size(); i++) {
            Gisement g1 = new Gisement(
                    centroidFict, this.results.get(i).getPoint(), false);
            Gisement g2 = new Gisement(
                    centroidCadast, this.measures.get(i).getPoint(), false);

            intermRes.add(
                    new IntermediateResults(
                            g1.getGisement(), g1.getHorizDist(),
                            g2.getGisement(), g2.getHorizDist()));

            // calculation of the rotation between fictive and cadastral coordinates
            // according to the following formula:
            //      mod400(gis_cadastral - gis_fictive)
            double rotation = MathUtils.modulo400(g2.getGisement() - g1.getGisement());
            intermRes.get(i).rotation = rotation;
            meanRotations += rotation;

            // calculation of the multiplication constants between fictive and
            // cadastral coordinates according to the following formula:
            //      dist_cadastral / dist_fictive 
            double constant = g2.getHorizDist() / g1.getHorizDist();
            intermRes.get(i).constant = constant;
            meanConstants += constant;
        }

        meanRotations /= n;
        meanConstants /= n;

        // calculation of the gisement/distance between the fictive centroid and the
        // station (which is actually at the coordinates 0;0).
        this.stationResult = new Point(this.stationNumber, 0.0, 0.0, 0.0, false, false);
        Gisement g = new Gisement(centroidFict, this.stationResult, false);
        double gisFictiveGToSt = g.getGisement(); // gisement g-St
        double distFictiveGToSt = g.getHorizDist(); // distance g-St

        // calculation of the station coordinates
        double tmp1 = MathUtils.gradToRad(gisFictiveGToSt + meanRotations);
        double tmp2 = distFictiveGToSt * meanConstants;
        this.stationResult.setEast((Math.sin(tmp1) * tmp2) + centroidCadast.getEast());
        this.stationResult.setNorth((Math.cos(tmp1) * tmp2) + centroidCadast.getNorth());
        double altitude = (!MathUtils.isZero(meanAltitude)) ? meanAltitude / totalWeights : 0.0;
        this.stationResult.setAltitude(altitude);

        double diffAlt = 0.0;

        for (int i = 0; i < this.results.size(); i++) {
            double newGis = MathUtils.modulo400(
                    meanRotations + this.measures.get(i).getHorizDir());
            double newDist = meanConstants * this.measures.get(i).getDistance();

            // vE [cm]
            double newE = MathUtils.pointLanceEast(
                    this.stationResult.getEast(), newGis, newDist);
            double vE = (newE - this.measures.get(i).getPoint().getEast()) * 100;
            this.results.get(i).setvE(vE);

            // vN [cm]
            double newN = MathUtils.pointLanceNorth(
                    this.stationResult.getNorth(), newGis, newDist);
            double vN = (newN - this.measures.get(i).getPoint().getNorth()) * 100;
            this.results.get(i).setvN(vN);

            // vA [cm]
            double vA = (altitude - this.results.get(i).getPoint().getAltitude()) * 100;
            this.results.get(i).setvA(vA);

            // FS [cm]
            double fS = Math.sqrt(Math.pow(vE, 2) + Math.pow(vN, 2));
            this.results.get(i).setfS(fS);

            this.sE += vE;
            this.sN += vN;
            diffAlt += this.results.get(i).getWeight() * Math.pow(vA, 2);
            this.meanFS += fS;
        }

        this.sE /= this.results.size();
        this.sN /= this.results.size();
        this.sA = Math.sqrt(diffAlt / ((nbAltitudes - 1) * totalWeights));
        this.meanFS /= this.results.size();

        this.unknownOrientation = meanRotations;

        this.updateLastModification();
        this.notifyUpdate(this);
    }

    @Override
    public String exportToJSON() throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put(FreeStation.STATION_NUMBER, this.stationNumber);
        jo.put(FreeStation.INSTRUMENT, this.i);

        JSONArray measuresArray = new JSONArray();
        for (Measure m : this.measures) {
            measuresArray.put(m.toJSONObject());
        }
        jo.put(FreeStation.MEASURES, measuresArray);

        return jo.toString();
    }

    @Override
    public void importFromJSON(String jsonInputArgs) throws JSONException {
        JSONObject jo = new JSONObject(jsonInputArgs);
        this.stationNumber = jo.getInt(FreeStation.STATION_NUMBER);
        this.i = jo.getDouble(FreeStation.INSTRUMENT);

        for (int i = 0; i < jo.getJSONArray(FreeStation.MEASURES).length(); i++) {
            JSONObject measureObject = (JSONObject) jo.getJSONArray(
                    FreeStation.MEASURES).get(i);

            Point st = SharedResources.getSetOfPoints().find(
                    measureObject.getInt(Measure.ORIENTATION_NUMBER));

            Measure m = new Measure(
                    st,
                    measureObject.getDouble(Measure.HORIZ_DIR),
                    measureObject.getDouble(Measure.ZEN_ANGLE),
                    measureObject.getDouble(Measure.DISTANCE),
                    measureObject.getDouble(Measure.S),
                    measureObject.getDouble(Measure.LAT_DEPL),
                    measureObject.getDouble(Measure.LON_DEPL),
                    measureObject.getDouble(Measure.I),
                    measureObject.getDouble(Measure.UNKNOWN_ORIENTATION));
            this.measures.add(m);
        }
    }

    @Override
    public Class<?> getActivityClass() {
        return FreeStationActivity.class;
    }

    public final int getStationNumber() {
        return this.stationNumber;
    }

    public final void setStationNumber(int _stationNumber) {
        this.stationNumber = _stationNumber;
    }

    public final ArrayList<Measure> getMeasures() {
        return this.measures;
    }

    public final void setMeasures(ArrayList<Measure> _measures) {
        this.measures = _measures;
    }

    public final Point getStationResult() {
        return this.stationResult;
    }

    public final void setStationResult(Point _stationResult) {
        this.stationResult = _stationResult;
    }

    public final double getUnknownOrientation() {
        return this.unknownOrientation;
    }

    public final void setUnknownOrientation(double _unknownOrientation) {
        this.unknownOrientation = _unknownOrientation;
    }

    public final double getI() {
        return this.i;
    }

    public final void setI(double i) {
        this.i = i;
    }

    public final double getsE() {
        return this.sE;
    }

    public final double getsN() {
        return this.sN;
    }

    public final double getsA() {
        return this.sA;
    }

    public final double getMeanFS() {
        return this.meanFS;
    }

    public final ArrayList<Result> getResults() {
        return this.results;
    }

    public static class Result {
        /** The target point. */
        private Point  point;

        /**
         * Difference between the east coordinate of the original point and the
         * new one.
         */
        private double vE;

        /**
         * Difference between the north coordinate of the original point and the
         * new one.
         */
        private double vN;

        /**
         * Difference between the altitude of the original point and the new
         * one.
         */
        private double vA;

        /**
         * FS.
         */
        private double fS;

        /**
         * Weight.
         */
        private double weight;

        public Result(Point _point, double _vE, double _vN, double _vA,
                double _weight) {
            this.point = _point;
            this.vE = _vE;
            this.vN = _vN;
            this.vA = _vA;
            this.weight = _weight;
        }

        public Result(Point _point) {
            this(_point, 0.0, 0.0, 0.0, 0.0);
        }

        public Result(Point _point, double _weight) {
            this(_point, 0.0, 0.0, 0.0, _weight);
        }

        public final Point getPoint() {
            return this.point;
        }

        public final void setPoint(Point point) {
            this.point = point;
        }

        public final double getvE() {
            return this.vE;
        }

        public final void setvE(double vE) {
            this.vE = vE;
        }

        public final double getvN() {
            return this.vN;
        }

        public final void setvN(double vN) {
            this.vN = vN;
        }

        public final double getvA() {
            return this.vA;
        }

        public final void setvA(double vA) {
            this.vA = vA;
        }

        public final double getfS() {
            return this.fS;
        }

        public final void setfS(double fS) {
            this.fS = fS;
        }

        public final double getWeight() {
            return this.weight;
        }

        public final void setWeight(double weight) {
            this.weight = weight;
        }
    }

    /**
     * This nested class is just used for holding intermediate results.
     * 
     * @author HGdev
     */
    private class IntermediateResults {
        /**
         * Fictive gisement. (this attribute is used from the outside)
         */
        @SuppressWarnings("unused")
        private final double gisFict;

        /**
         * Fictive distance. (this attribute is used from the outside)
         */
        @SuppressWarnings("unused")
        private final double distFict;

        /**
         * Cadastral gisement. (this attribute is used from the outside)
         */
        @SuppressWarnings("unused")
        private final double gisCadast;

        /**
         * Cadastral distance. (this attribute is used from the outside)
         */
        @SuppressWarnings("unused")
        private final double distCadast;

        /**
         * Rotation between fictive and cadastral coordinates. (this attribute
         * is used from the outside)
         */
        @SuppressWarnings("unused")
        private double       rotation;

        /**
         * Multiplication constants between fictive and cadastral coordinates.
         * (this attribute is used from the outside)
         */
        @SuppressWarnings("unused")
        private double       constant;

        private IntermediateResults(double _gisFict, double _distFict,
                double _gisCadast, double _distCadast) {
            this.gisFict = _gisFict;
            this.distFict = _distFict;

            this.gisCadast = _gisCadast;
            this.distCadast = _distCadast;

            this.rotation = 0.0;
            this.constant = 0.0;
        }
    }
}