package fc.PrintingApplication.TP4;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class Test {

public static void testCorner(Obj3DModel obj, int numSlice) {
		
		Slice slice = new Slice(numSlice, obj);

		slice.remap(Main.OFFSET, Main.PIXEL_SIZE);
		int[][] pixels = Rasterer.rasterGPU(slice, numSlice);
		
		BufferedImage img = new BufferedImage(Main.WIDTH, Main.HEIGHT,BufferedImage.TYPE_INT_RGB);
		Main.setData(img, pixels);
		
		
	  	ArrayList<Vec2[]> boxes = new ArrayList<>();
	  	ArrayList<Vec2[]> holeBoxes = new ArrayList<>();

	  	ArrayList<Vec2> points = Main.getCorners(pixels, boxes, holeBoxes); 
	  		  	
	  	BufferedImage img2 = new BufferedImage(Main.WIDTH, Main.HEIGHT,BufferedImage.TYPE_INT_RGB);
	  	Main.setData(img2, pixels);

	  	Graphics2D ctx = img2.createGraphics();
	  	
		for(int i = 0; i < boxes.size(); ++i) {
			ctx.draw(new Line2D.Float(boxes.get(i)[0].x, boxes.get(i)[0].y, boxes.get(i)[1].x, boxes.get(i)[0].y));
			ctx.draw(new Line2D.Float(boxes.get(i)[1].x, boxes.get(i)[0].y, boxes.get(i)[1].x, boxes.get(i)[1].y));
			ctx.draw(new Line2D.Float(boxes.get(i)[0].x, boxes.get(i)[1].y, boxes.get(i)[1].x, boxes.get(i)[1].y));
			ctx.draw(new Line2D.Float(boxes.get(i)[0].x, boxes.get(i)[0].y, boxes.get(i)[0].x, boxes.get(i)[1].y));

		}
		Color c = ctx.getColor();
		ctx.setColor(Color.YELLOW);
		for(int i = 0; i < holeBoxes.size(); ++i) {
			ctx.draw(new Line2D.Float(holeBoxes.get(i)[0].x, holeBoxes.get(i)[0].y, holeBoxes.get(i)[1].x, holeBoxes.get(i)[0].y));
			ctx.draw(new Line2D.Float(holeBoxes.get(i)[1].x, holeBoxes.get(i)[0].y, holeBoxes.get(i)[1].x, holeBoxes.get(i)[1].y));
			ctx.draw(new Line2D.Float(holeBoxes.get(i)[0].x, holeBoxes.get(i)[1].y, holeBoxes.get(i)[1].x, holeBoxes.get(i)[1].y));
			ctx.draw(new Line2D.Float(holeBoxes.get(i)[0].x, holeBoxes.get(i)[0].y, holeBoxes.get(i)[0].x, holeBoxes.get(i)[1].y));

		}
		
		ctx.setColor(c);

	  	for(int i = 0; i < points.size(); ++i) {
	  		Vec2 pos = points.get(i);
			for(int yd = -5; yd <= 5; ++yd)
				for(int xd = -5; xd <= 5; ++xd) {
					int x = pos.x + xd;
					int y = pos.y + yd;
					if(x < 0 || y < 0 || x >= Main.WIDTH || y >= Main.HEIGHT) continue;
					img2.setRGB(x, y, 0x00FF00);
					pixels[x][y] = 0xFF;
				}
			
	  		img2.setRGB(pos.x, pos.y, 0x00FF00);
	  	
	  	}
	  	BufferedImage img3 = new BufferedImage(Main.WIDTH, Main.HEIGHT,BufferedImage.TYPE_INT_RGB);
	  	Main.setData(img3, pixels);
	  	//saveImage(img, RESULT_PATH + NAME  + "Ref" + numSlice + ".png");
	  	Main.saveImage(img2, Main.RESULT_PATH + Main.NAME  + "CornerBox" + numSlice + ".png");
	  	Main.saveImage(img3, Main.RESULT_PATH + Main.NAME  + "Corners" + numSlice + ".png");

	
	}
}
