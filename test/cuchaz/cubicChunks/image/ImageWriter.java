/*******************************************************************************
 * Copyright (c) 2014 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.cubicChunks.image;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class ImageWriter
{
    //just convenience methods for debug

    public static void greyWriteImage(double[][] data){
        //this takes and array of doubles between 0 and 1 and generates a grey scale image from them

        BufferedImage image = new BufferedImage(data.length,data[0].length, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < data[0].length; y++)
        {
        	for (int x = 0; x < data.length; x++)
        	{
	            if (data[x][y]>1)
	            {
	                data[x][y]=1;
	            }
	            if (data[x][y]<0)
	            {
	                data[x][y]=0;
	            }
	             
	            Color col=new Color((float)data[x][y],(float)data[x][y],(float)data[x][y]); 
	            image.setRGB(x, y, col.getRGB());
        	}
        }

        try
        {
            // retrieve image
            File outputfile = new File("saved.png");
            outputfile.createNewFile();

            ImageIO.write(image, "png", outputfile);
        } 
        catch (IOException e)
        {
            //o no! Blank catches are bad
            throw new RuntimeException("I didn't handle this very well");
        }
    }
}
