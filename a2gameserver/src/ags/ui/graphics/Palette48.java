/*
 * Palette48.java
 *
 * Created on June 23, 2006, 3:21 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package ags.ui.graphics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Administrator
 */
//public class Palette48 extends PaletteYIQ {
public class Palette48 extends Palette {

    private Map<Integer, Integer[]> appleColorIndex;

    protected void initPalette() {
        // We have more colors to play with, so tighten up the tolerance.
//        MATCH_TOLERANCE = 16;
        appleColorIndex = new HashMap<Integer, Integer[]>();
        PaletteYIQ p16 = new Palette16();
        List<String> patterns = getBlendPatterns();
        int max_diff = COLOR_DISTANCE_MAX / 3;
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
//                if (patterns.get(y).charAt(x) == '#') {
                if (x <= y && distance(asColor(p16.getColor(y)), asColor(p16.getColor(x))) <= max_diff) {
                    appleColorIndex.put(colors.size(), new Integer[]{x,y});
                    addColor(blend(asColor(p16.getColor(y)), asColor(p16.getColor(x))));
//                    ColorYIQ c = blendYIQ(p16.getColor(y), p16.getColor(x));
//                    addColor(c.getY(), c.getI(), c.getQ());
                }
            }
        }
        System.out.println("Built a palette with " + colors.size() + " colors");
    }

    public Integer[] getAppleColors(int color) {
        return appleColorIndex.get(color);
    }

    private List<String> getBlendPatterns() {
        List<String> patterns = new ArrayList<String>();
        patterns.add("#...............");
        patterns.add(".#..............");
        patterns.add("###.............");
        patterns.add("...#............");
        patterns.add("###.#...........");
        patterns.add("###.##..........");
        patterns.add(".######.........");
        patterns.add("...#.#.#........");
        patterns.add("#.#.###.#.......");
        patterns.add("...#...#.#......");
        patterns.add("###.###.#.#.....");
        patterns.add("...#...#.#.#....");
        patterns.add("...#..##.#.##...");
        patterns.add("...#..#..#.###..");
        patterns.add("...#..##.#.####.");
        patterns.add(".......#.....#.#");
        return patterns;
    }

    private int[] blend(int[] c1, int[] c2) {
        int[] newColor = new int[3];
        newColor[0] = (c1[0] + c2[0]) / 2;
        newColor[1] = (c1[1] + c2[1]) / 2;
        newColor[2] = (c1[2] + c2[2]) / 2;
        return newColor;
    }

    private ColorYIQ blendYIQ(ColorYIQ c1, ColorYIQ c2) {
        double y = (c1.getY() + c2.getY()) / 2.0;
        double i = (c1.getI() + c2.getI()) / 2.0;
        double q = (c1.getQ() + c2.getQ()) / 2.0;

        ColorYIQ newColor = new ColorYIQ(y, i, q, false);
        return newColor;
    }

    private int[] asColor(ColorYIQ c) {
        int[] newColor = new int[3];
        int rgb = c.toRGB();
        newColor[0] = ColorYIQ.getR(rgb);
        newColor[1] = ColorYIQ.getG(rgb);
        newColor[2] = ColorYIQ.getB(rgb);
        return newColor;
    }
}
