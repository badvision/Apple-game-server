/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ags.ui.graphics;

/**
 *
 * @author brobert
 */
public class ColorYIQ {
    // y Range [0,1]
    private double y;
    public static double MIN_Y = 0;
    public static double MAX_Y = 1;
    // i Range [-0.5957, 0.5957]
    private double i;
    public static double MAX_I = 0.5957;
    // q Range [-0.5226, 0.5226]
    private double q;
    public static double MAX_Q = 0.5226;
    public ColorYIQ(double y, double i, double q, boolean scale) {
        if (scale) {
            y *= MAX_Y;
            i *= MAX_I;
            q *= MAX_Q;
        }
        this.y = normalize(y, MIN_Y, MAX_Y);
        this.i = normalize(i, -MAX_I, MAX_I);
        this.q = normalize(q, -MAX_Q, MAX_Q);
    }

    public ColorYIQ(double y, double i, double q) {
        this(y,i,q, true);
    }

    public static int getR(int rgb) {
        int r = rgb >> 16 & 0x0ff;
        return r;
    }
    public static int getG(int rgb) {
        int g = rgb >> 8 & 0x0ff;
        return g;
    }
    public static int getB(int rgb) {
        int b = rgb & 0x0ff;
        return b;
    }

    public static ColorYIQ fromRGB(int rgb) {
        return fromRGB(getR(rgb),getG(rgb),getB(rgb));
    }

    public static ColorYIQ fromRGB(double r, double g, double b) {
        double R = r / 255.0;
        double G = g / 255.0;
        double B = b / 255.0;
// From RGB to YIQ
        double Y = 0.299 * R + 0.587 * G + 0.114 * B;
        double I = 0.596 * R - 0.275 * G - 0.321 * B;
        double Q = 0.212 * R - 0.523 * G + 0.311 * B;

        return new ColorYIQ(Y, I, Q, false);
    }

//Formulas borrowed from:
    public int toRGB() {
        // YIQ to RGB
		/*
		 * [R]   [ 1   0.956   0.621] [Y]
		 * [G] = [ 1  -0.272  -0.647] [I]
		 * [B]   [ 1  -1.105   1.702] [Q]
		 */
        int r = (int) (normalize((getY() + 0.956 * getI() + 0.621 * getQ()),0,1) * 255);
        int g = (int) (normalize((getY() - 0.272 * getI() - 0.647 * getQ()),0,1) * 255);
        int b = (int) (normalize((getY() - 1.105 * getI() + 1.702 * getQ()),0,1) * 255);
        return (r << 16) | (g << 8 ) | b;
    }

    double distanceWeighted(ColorYIQ search) {
        // Borrowed color distance formulas from here:
        // http://www.compuphase.com/cmetric.htm
        int c1 = toRGB();
        int c2 = search.toRGB();
        double r = (getR(c1) + getR(c2)) / 2.0;
        double g = getG(c1) - getG(c2);
        double b = getB(c1) - getB(c2);
        double distance=Math.pow(
                (2.0 + (r/256))*r*r +
                4*g*g +
                (2.0 + (255-r)/256)*b*b
                ,-2);
        return distance;
    }
    public double distance(ColorYIQ search) {
        double distY = Math.pow(Math.abs(search.getY() - getY()), 2.0) / MAX_Y;
        double distI = Math.pow(Math.abs(search.getI() - getI()), 2.0) / MAX_I;
        double distQ = Math.pow(Math.abs(search.getQ() - getQ()), 2.0) / MAX_Q;

        double yiqDistance = Math.abs(Math.pow(distY + distI + distQ, -2.0));
//        return yiqDistance * (distanceWeighted(search));
        return yiqDistance;
    }

    public static double normalize(double x, double minX, double maxX) {
        if (x < minX) return minX;
        if (x > maxX) return maxX;
        return x;
    }

    /**
     * @return the y
     */
    public double getY() {
        return y;
    }

    /**
     * @return the i
     */
    public double getI() {
        return i;
    }

    /**
     * @return the q
     */
    public double getQ() {
        return q;
    }
}