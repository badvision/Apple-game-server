package ags.ui.graphics;
/* Copyright (c) 2013 the authors listed at the following URL, and/or
 the authors of referenced articles or incorporated external code:
 http://en.literateprograms.org/Floyd-Steinberg_dithering_(Java)?action=history&offset=20080201121723

 Permission is hereby granted, free of charge, to any person obtaining
 a copy of this software and associated documentation files (the
 "Software"), to deal in the Software without restriction, including
 without limitation the rights to use, copy, modify, merge, publish,
 distribute, sublicense, and/or sell copies of the Software, and to
 permit persons to whom the Software is furnished to do so, subject to
 the following conditions:

 The above copyright notice and this permission notice shall be
 included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 Retrieved from: http://en.literateprograms.org/Floyd-Steinberg_dithering_(Java)?oldid=12476
 * Original code by Spoon! (Feb 2008)
 * Modified and adapted to work with Apple Game Server by Brendan Robert (2013)
 * Some of the original code of this class was migrated over to the Palette class which already manages colors in AGS.
 */

import java.awt.Image;
import java.awt.image.BufferedImage;

public class FloydSteinbergDither {

    public static BufferedImage floydSteinbergDither(Image img, PaletteYIQ palette) {
        BufferedImage source = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_RGB);
        source.getGraphics().drawImage(img, 0, 0, null);
        BufferedImage dest = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int currentPixel = source.getRGB(x, y);
                int closestColor = palette.getColor(palette.findColor(ColorYIQ.fromRGB(currentPixel))).toRGB();
                dest.setRGB(x, y, closestColor);

                for (int i = 0; i < 3; i++) {
                    int error = Palette.getComponent(currentPixel, i) - Palette.getComponent(closestColor, i);
                    if (x + 1 < source.getWidth()) {
                        int c = source.getRGB(x + 1, y);
                        source.setRGB(x + 1, y, Palette.addError(c, i, (error * 7) >> 4));
                    }
                    if (y + 1 < source.getHeight()) {
                        if (x - 1 > 0) {
                            int c = source.getRGB(x - 1, y + 1);
                            source.setRGB(x - 1, y + 1, Palette.addError(c, i, (error * 3) >> 4));
                        }
                        {
                            int c = source.getRGB(x, y + 1);
                            source.setRGB(x, y + 1, Palette.addError(c, i, (error * 5) >> 4));
                        }
                        if (x + 1 < source.getWidth()) {
                            int c = source.getRGB(x + 1, y + 1);
                            source.setRGB(x + 1, y + 1, Palette.addError(c, i, error >> 4));
                        }
                    }
                }
            }
        }
        return dest;
    }

    public static BufferedImage floydSteinbergDither(Image img, Palette palette) {
        BufferedImage source = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_RGB);
        source.getGraphics().drawImage(img, 0, 0, null);
        BufferedImage dest = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int currentPixel = source.getRGB(x, y);
                int closestColor = palette.getColorInt(palette.findColor(currentPixel));
                dest.setRGB(x, y, closestColor);

                for (int i = 0; i < 3; i++) {
                    int error = Palette.getComponent(currentPixel, i) - Palette.getComponent(closestColor, i);
                    if (x + 1 < source.getWidth()) {
                        int c = source.getRGB(x + 1, y);
                        source.setRGB(x + 1, y, Palette.addError(c, i, (error * 7) >> 4));
                    }
                    if (y + 1 < source.getHeight()) {
                        if (x - 1 > 0) {
                            int c = source.getRGB(x - 1, y + 1);
                            source.setRGB(x - 1, y + 1, Palette.addError(c, i, (error * 3) >> 4));
                        }
                        {
                            int c = source.getRGB(x, y + 1);
                            source.setRGB(x, y + 1, Palette.addError(c, i, (error * 5) >> 4));
                        }
                        if (x + 1 < source.getWidth()) {
                            int c = source.getRGB(x + 1, y + 1);
                            source.setRGB(x + 1, y + 1, Palette.addError(c, i, error >> 4));
                        }
                    }
                }
            }
        }
        return dest;
    }
}