package ags.ui.graphics;

import java.util.ArrayList;
import java.util.List;

public abstract class PaletteYIQ {
    List<ColorYIQ> colors;
    
    public PaletteYIQ() {
        colors = new ArrayList<ColorYIQ>();
        initPalette();
    }
    
    protected abstract void initPalette();
    
    public ColorYIQ getColor(int col) {
        return colors.get(col);
    }
    
    public void addColor(ColorYIQ col) {
        colors.add(col);
    }
    
    public void addColor(double y, double i, double q) {
        ColorYIQ col = new ColorYIQ(y,i,q);
        addColor(col);
    }
    
    public int findColor(ColorYIQ search) {
//        double lastDist = Double.MAX_VALUE;
        double lastDist = 0;
        ColorYIQ bestFit = colors.get(0);
        for(ColorYIQ test : colors) {
            double dist  = Math.abs(test.distance(search));
//            if(dist < lastDist) {
            if(dist > lastDist) {
                lastDist = dist;
                bestFit = test;
            }
        }
        
        return colors.indexOf(bestFit);
    }    
}