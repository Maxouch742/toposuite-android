package ch.hgdev.toposuite.utils;

/**
 * MathUtils provides static helpers for mathematical operation/conversion.
 * 
 * @author HGdev
 */
public class MathUtils {
    /**
     * The machine precision used to perform logical operation on doubles.
     */
    public final static double EPSILON = Double.MIN_VALUE;

    /**
     * Check if a double is zero.
     * 
     * @param d
     *            a double
     * @return true if d is equal to 0, false otherwise.
     */
    public static boolean isZero(double d) {
        return (d < MathUtils.EPSILON) && (d > -MathUtils.EPSILON);
    }

    /**
     * Check if a double is strictly positive.
     * 
     * @param d
     *            a double
     * @return true if d is bigger than 0, false otherwise.
     */
    public static boolean isPositive(double d) {
        return d > MathUtils.EPSILON;
    }

    /**
     * Check if a double is strictly negative.
     * 
     * @param d
     *            a double
     * @return true if d is smaller than 0, false otherwise.
     */
    public static boolean isNegative(double d) {
        return d < -MathUtils.EPSILON;
    }

    /**
     * Convert an angle in radian to its equivalent in gradian.
     * 
     * @param rad
     *            angle in radian
     * @return angle in gradian
     */
    public static double radToGrad(double rad) {
        return (rad / Math.PI) * 200;
    }

    /**
     * Convert an angle in gradian to its equivalent radian.
     * 
     * @param grad
     *            angle in gradian
     * @return angle in radian
     */
    public static double gradToRad(double grad) {
        return (grad * Math.PI) / 200;
    }

    /**
     * Convert an angle in gradian to its equivalent in degree.
     * 
     * @param grad
     *            The angle to convert.
     * @return The angle in degree.
     */
    public static double gradToDeg(double grad) {
        return grad * ((Math.PI / 200) / (Math.PI / 180));
    }

    /**
     * Convert an angle in degree to its equivalent in gradian.
     * 
     * @param deg
     *            The angle to convert.
     * @return The angle in gradian.
     */
    public static double degToGrad(double deg) {
        return deg * ((Math.PI / 180) / (Math.PI / 200));
    }

    /**
     * Modulate an angle in gradian. This ensures that the angle is between 0
     * and 400 gradian.
     * 
     * @param angle
     *            Angle in gradian unit.
     * @return The angle with a value between 0 and 400 gradians.
     */
    public static double modulo400(double angle) {
        double m = angle;
        if (m < 0) {
            while (m < 0) {
                m += 400;
            }
        } else if (m >= 400) {
            while (m >= 400) {
                m -= 400;
            }
        }
        return m;
    }
}