package fc.PrintingApplication.TP4;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import javax.imageio.ImageIO;

import fc.Math.Vec2f;
import fc.Math.Vec2i;




public class Main
{
	
	public static int WIDTH = 800;
	public static int HEIGHT = 600;
	public static float BUSE_SIZE = 0.4f;
	public static float PIXEL_SIZE = 0.05f; 
	public static float VERTICAL_STEP = 0.2f; 
	public static Vec2f OFFSET = new Vec2f(WIDTH / 2, HEIGHT / 2);

	final public static String OBJ_PATH = "obj/"; 
	final public static String RESULT_PATH = "results/"; 
	public static String NAME = "skull";
	
	public static void testCorner(Obj3DModel obj, int numSlice) {
		
		Slice slice = new Slice(numSlice, obj);

		slice.remap(OFFSET, PIXEL_SIZE);
		int[][] pixels = Rasterer.rasterGPU(slice, numSlice);
		
		BufferedImage img = new BufferedImage(WIDTH, HEIGHT,BufferedImage.TYPE_INT_RGB);
		setData(img, pixels);
		
		
	  	ArrayList<Vec2i[]> boxes = new ArrayList<>();
	  	ArrayList<Vec2i[]> holeBoxes = new ArrayList<>();

	  	ArrayList<Vec2i> points = getCorners(pixels, boxes, holeBoxes); 
	  		  	
	  	BufferedImage img2 = new BufferedImage(WIDTH, HEIGHT,BufferedImage.TYPE_INT_RGB);
	  	setData(img2, pixels);

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
	  		Vec2i pos = points.get(i);
			for(int yd = -5; yd <= 5; ++yd)
				for(int xd = -5; xd <= 5; ++xd) {
					int x = pos.x + xd;
					int y = pos.y + yd;
					if(x < 0 || y < 0 || x >= WIDTH || y >= HEIGHT) continue;
					img2.setRGB(x, y, 0x00FF00);
					pixels[x][y] = 0xFF;
				}
			
	  		img2.setRGB(pos.x, pos.y, 0x00FF00);
	  	
	  	}
	  	BufferedImage img3 = new BufferedImage(WIDTH, HEIGHT,BufferedImage.TYPE_INT_RGB);
	  	setData(img3, pixels);
	  	//saveImage(img, RESULT_PATH + NAME  + "Ref" + numSlice + ".png");
		saveImage(img2, RESULT_PATH + NAME  + "CornerBox" + numSlice + ".png");
		saveImage(img3, RESULT_PATH + NAME  + "Corners" + numSlice + ".png");

	
	}
	
	public static void main(String[] args)
	{
		NAME = "skull";
		NAME = "yoda";
		NAME = "CuteOcto";
	//	NAME = "fawn";
		int numSlice = 5;
		
		Obj3DModel obj = new Obj3DModel(OBJ_PATH + NAME + ".obj");
		int h_size = (int) Math.ceil((obj.getMax().x - obj.getMin().x) / PIXEL_SIZE);
		int v_size = (int) Math.ceil((obj.getMax().y - obj.getMin().y) / PIXEL_SIZE);
		WIDTH = h_size;
		HEIGHT = v_size;
		OFFSET.x = h_size *0.5f;
		OFFSET.y = v_size *0.5f;

		
		testCorner(obj, numSlice);
		//Rasterer.rasterCPU(slice, 5);
		//Rasterer.rasterGPU(slice, 5);
		Rasterer.slicer(null); // <-- main application
		

	}
	
	// IMG buffer - int[][] pixel --------------------------------------
	public static int[][] getData(BufferedImage img) {
		int pixels[][] = new int[img.getWidth()][img.getHeight()];
		for(int y = 0; y < img.getHeight(); ++y) {
			for(int x = 0; x < img.getWidth(); ++x) {
				pixels[x][y] = img.getRGB(x, y);
			}
		}
		
		return pixels;
		
	}	
	public static void setData(BufferedImage img, int[][] pixels) {
		img.setRGB(0, 0, WIDTH, HEIGHT, compact(pixels), 0, WIDTH);
	}
	public static int[] compact(int[][] pixels) {
		int pix[] = new int[pixels.length * pixels[0].length];
		for(int y = 0; y < pixels[0].length; ++y) 
			for(int x = 0; x < pixels.length; ++x)
				pix[x + y * pixels.length] = pixels[x][y];
		
		return pix;
	}	
	public static int[][] copy(int[][] pixels) {
		int pix[][] = new int[pixels.length][pixels[0].length];
		for(int y = 0; y < pixels[0].length; ++y) 
			for(int x = 0; x < pixels.length; ++x)
				pix[x][y] = pixels[x][y];
		
		return pix;
	}
	public static void clearData(int[][] pixels) {
		
		for(int y = 0; y < pixels[0].length; ++y) 
			for(int x = 0; x < pixels.length; ++x) 
				pixels[x][y] = 0x000000;	
	}
	public static boolean saveImage(BufferedImage image, String name){
		System.out.println("printing in " + name);
		//   File outputFile = new File(System.getProperty("user.dir"), RESULT_PATH + NAME  + "GPU" + numSlice + ".png");
		
		try {
			ImageIO.write(image, "png", new File(name));
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			//System.out.println("Error, IOException caught: " + e.toString());
			return false;
		}
	}
	
	// Path Generator ---------------------------------------------------
	public static boolean next(int[][] pixels, Vec2i pos, ArrayList<Vec2i> path, Deque<Vec2i> neighbors) {
		Vec2i neighbor = new Vec2i(0, 0);
		Vec2i noon = new Vec2i(0,0);

		boolean found = false;
		boolean val = false;
		boolean nval = false;
		boolean sens = false;


		int x = 0, y = 1;
		noon.x = pos.x + x;
		noon.y = pos.y + y;
		int j = 0;
		while(j++ < 8) { // rotation horaire 8 connexe
			if(sens) {
				if(x > 0) --y;
				else if(x < 0) ++y;
			}else {
				if(y > 0) ++x;
				else if(y < 0) --x;
			}
			sens ^= ((x+y) & 1) == 0;
			
			neighbor.x = pos.x + x;
			neighbor.y = pos.y + y;
			if(neighbor.x < 0 || neighbor.x >= WIDTH || neighbor.y < 0 || neighbor.y >= HEIGHT) continue;
			if(noon.x < 0 || noon.x >= WIDTH || noon.y < 0 || noon.y >= HEIGHT) continue;

			
			val = ((pixels[noon.x][noon.y] >> 16) & 0xFF) > 0; // pix courant
			nval = ((pixels[neighbor.x][neighbor.y]  >> 16) & 0xFF) > 0; // pix suivant

			if(val != nval) {
				Vec2i tmp = new Vec2i(0, 0);
				if(nval) {
					tmp.x = neighbor.x;
					tmp.y = neighbor.y;
				}else {
					
					tmp.x = noon.x;
					tmp.y = noon.y;
				}
				int id = path.indexOf(tmp);
				if(id < 0) {
					
					pos.x = tmp.x;
					pos.y = tmp.y;
					path.add(new Vec2i(pos.x, pos.y));
					
					return true;
				}else
					neighbors.addLast(tmp);
					
				
			}
			
			noon.x = neighbor.x;
			noon.y = neighbor.y;
			
			val = nval;
		}
		return false;
	}
	
	public static ArrayList<Vec2i> getPath(int[][] pixels,Vec2i pos) {
		
		ArrayList<Vec2i> path = new ArrayList<>();
		path.add(new Vec2i(pos.x, pos.y));
		
		Deque<Vec2i> prev = new LinkedList<>();
		boolean found = false;
		boolean startpos = false;
		do {
			found = next(pixels, pos, path, prev);
			// begin read back
			if(!found) {
				//System.out.println("didn't find next path");
				while(!prev.isEmpty()) {
					Vec2i p = prev.poll();
					if(p.equals(path.get(0))) {
						pos.x = p.x;
						pos.y = p.y;
						prev.clear();
						found = next(pixels, pos, path, prev);
						startpos = true;
						//System.out.println("found start");
					}
				}
				if(!startpos) {
					int i = path.size() - 1;
					do {
						pos.x = path.get(i).x;
						pos.y = path.get(i).y;
						path.add(new Vec2i(pos.x, pos.y));
						found = next(pixels, pos, path, prev);
						--i;
	
					}while(!found && !pos.equals(path.get(0)));
				}

				
			}
			prev.clear();
			
			
		}while(!pos.equals(path.get(0)) && found);
		
		return path;
	}
	
	public static ArrayList<ArrayList<Vec2i>> getPaths(int[][] pixels) {
		ArrayList<ArrayList<Vec2i>> paths = new ArrayList<>();
		ArrayList<Vec2i> corners = getCorners(pixels);
		for(Vec2i pos: corners) {
			ArrayList<Vec2i> path= getPath(pixels, pos), npath = new ArrayList<>();
			// smooth path			
			ArrayList<Vec2i> smpath = smoothPath(path);			    
			paths.add(smpath);
		}
		
		return paths;
	}
	
	public static ArrayList<Vec2i> smoothPath(ArrayList<Vec2i> path){
		
		int mid = path.size() / 2;
		ArrayList<Vec2i> smoothedPath = new ArrayList<>();
		
		float epsilon = (float)Math.sqrt(2);
		
		ArrayList<Integer> ids = new ArrayList<>();
		ids.add(0);
		ids.add(path.size() - 1);
		Stack<List<Vec2i>> lists = new Stack<>();
		Stack<Integer> ofsts = new Stack<>();
		ofsts.push(0);
		lists.push(path);
		float dist1 = Float.MIN_VALUE, dist2 = Float.MIN_VALUE;
		int id1 = -1, id2 = -1;
		while(!lists.isEmpty()) {
			List<Vec2i> l = lists.pop();
			int ofst = ofsts.pop();
			mid = l.size() / 2;
			dist1 = Float.MIN_VALUE;
			id1 = -1;
			Vec2f seg = new Vec2f(-(l.get(mid).y - l.get(0).y), (l.get(mid).x - l.get(0).x));
			float len = seg.length();
			seg.mul(1f / len);
			for(int i = 0; i < mid; ++i) {
				Vec2f p = new Vec2f(l.get(i).x - l.get(0).x , l.get(i).y - l.get(0).y);
				float d = Math.abs(p.dot(seg));// / len;
				
				if(dist1 < d) {
					dist1 = d;
					id1 = i;
				}
				
			}
			
			dist2 = Float.MIN_VALUE;
			id2 = -1;
			for(int i = mid; i < l.size(); ++i) {
				Vec2f p = new Vec2f(l.get(i).x - l.get(0).x , l.get(i).y - l.get(0).y);
				float d =Math.abs(p.dot(seg));
				if(dist2 < d) {
					dist2 = d;
					id2 = i;
				}
				
			}
			if(id1 >= 0 ) {
				
				ids.add((ofst + id1));
				if(dist1 > epsilon) {
					lists.push(l.subList(0, id1));
					lists.push(l.subList(id1, mid));
					
					ofsts.add(ofst);
					ofsts.add(ofst + id1);
				}
			}
			if(id2 < 0) continue;
			ids.add((ofst + id2));
			if(dist2 > epsilon) {
				lists.push(l.subList(id2, l.size()));
				lists.push(l.subList(mid, id2));
				
				ofsts.add(ofst + id2);
				ofsts.add(ofst + mid);
			}
			
		}
		
		Collections.sort(ids);
		for(int i = 0; i <ids.size(); ++i) {
			smoothedPath.add(path.get(ids.get(i)));
		}
		return smoothedPath;

	}
	
	public static int[][] erode(int[][] pixels, int nbErosion) {
		
		int[][] erodePixs = copy(pixels);//img.getSubimage(0, 0, img.getWidth(), img.getHeight());
		
		int kernel = (int) ((BUSE_SIZE) / PIXEL_SIZE);
		for(int i = nbErosion; i > 0; --i) {
			int[][] tmp = copy(erodePixs);
			clearData(erodePixs);
			
			for(int y = 0; y < tmp[0].length; ++y) {
				for(int x = 0; x < tmp.length; ++x) {
					int samp = (tmp[x][y] >> 16) & 0XFF;
					if(samp> 0) {
						boolean inc = true;
						for(int x1 = -kernel; x1 <= kernel && inc; ++x1) {
							if(x+x1 < 0 || x+x1 >= WIDTH) {
								inc = false;
								break;
							}
							for(int y1 = -kernel; y1 <= kernel && inc; ++y1) {
								if(y + y1 < 0 || y + y1 >= HEIGHT) {
									inc = false;
									break;
								}
								int samp1 = (tmp[x+x1][y + y1] >> 16) & 0xFF;
								inc &= samp1 > 0;
							}
						}
						if(inc)	erodePixs[x][y] = 0xFF0000;
					}
						
				}
			}
		}
		
		return erodePixs;
	}

//  Corners-------------------------------------------
	public static ArrayList<Vec2i> getCorners(int[][] pixels) {		
		return getCorners(pixels, null, null);
	}
	
	public static ArrayList<Vec2i> getCorners(int[][] pixels, ArrayList<Vec2i[]> BoundaryBoxes){
		return getCorners(pixels, BoundaryBoxes, null);
	}

	public static ArrayList<Vec2i> getCorners(int[][] pixels, ArrayList<Vec2i[]> BoundaryBoxes, ArrayList<Vec2i[]> BoundaryBoxesHole){
		
		ArrayList<Vec2i> positions = new ArrayList<>();
		
		// allboxes[0] = island, allboxes[1] = hole
		ArrayList<Vec2i[]> allboxes[] = new ArrayList[]{new ArrayList<>(), new ArrayList<>()};
		ArrayList<Vec2i[]> bboxes = allboxes[0]; // ref of one of the allboxes 
		ArrayList<Vec2i[]> upd = new ArrayList<>(); // ref of one of the allboxes 

		Stack<Integer> mins = new Stack<>();
		
		int minx = Integer.MAX_VALUE, max = -1, min;
		boolean hole = false, stillin = false, added = false;
		int m = -1;
		boolean quit = false;
		Vec2i pos = new Vec2i(0,0);
			
		int prevupsamp = 0;
		
		for(int y = 0; y < pixels[0].length; ++y) {	
			mins.clear();
			for(int x = 0; x < pixels.length; ++x) {
				int samp = (pixels[x][y] >> 16) & 0xFF;
			
				if(mins.empty()) {
					if(samp > 0) {
						mins.push(x);
						bboxes = allboxes[0];
						hole = false;
					}
				}else {
					if((hole && (samp == 0)) || (!hole && (samp > 0))) continue;
					else {
						min = mins.peek();
						max = x -1;
						Vec2i[] box;
						added = false;
						for (Vec2i[] bbox : bboxes) {
							
							// test if we are still in the current shape
								
							// test is part of bbox
							if(y == (bbox[1].y + 1)) {								
								stillin |= (x <= bbox[1].x && x >= bbox[0].x);
								if(min <= bbox[1].x && max >= bbox[0].x) {
									//bbox[1].y = y;
									box = bbox;
									upd.add(bbox);
									added = true;
									bbox[0].x = Math.min(min,  bbox[0].x);
									bbox[1].x = Math.max(max,  bbox[1].x);
									
								}
								//stillin = false;
							}
						}
						if(!added) 
							bboxes.add(new Vec2i[] {new Vec2i(min, y), new Vec2i(max, y)});
					
							
						// changement de forme mais dans le même ilôt
						if(stillin) {
							mins.add(x);
							// si dans un trou alors on reprend les bbox des ilots
							// sinon ceux des trous
							hole = ((mins.size() & 1) == 0);
							bboxes = allboxes[hole ? 1 : 0];
						}else
							mins.pop();
					}
				
				}
			
			}
			for(Vec2i[] bbox: upd) bbox[1].y = y;
			upd.clear();

			
		}

		
		for (int i = 0; i < allboxes.length; i++) {
			
			// si boite de trou dans une boite d'ile on conserve
			if(i == 1) {
				for(int j = 0; j < allboxes[1].size(); ++j) {
					boolean in = false;
					Vec2i[] b2 = allboxes[1].get(j);
					for(int k = 0; k < allboxes[0].size(); ++k) {
						Vec2i[] b1 = allboxes[0].get(k);
						int vmin = 0, vmax = 0;
						if(b2[0].x < b1[0].x -1) vmin += 1;
						else if(b2[0].x > b1[1].x +1) vmin += 4;
						if(b2[0].y < b1[0].y -1) vmin += 2;
						else if(b2[0].y > b1[1].y +1) vmin += 8;
						
						if(b2[1].x < b1[0].x -1) vmax += 1;
						else if(b2[1].x > b1[1].x +1) vmax += 4;
						if(b2[1].y < b1[0].y -1) vmax += 2;
						else if(b2[1].y > b1[1].y + 1) vmax += 8;
						
						if((vmax | vmin) == 0) {
							in = true;
							break;
						}								
					}
					if(!in) {
						Collections.swap(allboxes[1], j, allboxes[1].size() -1);
						allboxes[1].remove(allboxes[1].size() -1);
					}
				}
			}
			
			// fusion des bbox superposé
			for(int j = 0; j < allboxes[i].size(); ++j) {
				Vec2i[] b1 = allboxes[i].get(j);
				for(int k = j+1; k < allboxes[i].size(); ) {
					Vec2i[] b2 = allboxes[i].get(k);
					int vmin = 0, vmax = 0;
					if(b2[0].x < b1[0].x -1) vmin += 1;
					else if(b2[0].x > b1[1].x +1) vmin += 4;
					if(b2[0].y < b1[0].y -1) vmin += 2;
					else if(b2[0].y > b1[1].y +1) vmin += 8;
					
					if(b2[1].x < b1[0].x -1) vmax += 1;
					else if(b2[1].x > b1[1].x +1) vmax += 4;
					if(b2[1].y < b1[0].y -1) vmax += 2;
					else if(b2[1].y > b1[1].y + 1) vmax += 8;
					
					if((vmax & vmin) == 0) {
						b1[0].x = Math.min(b1[0].x, b2[0].x);
						b1[0].y = Math.min(b1[0].y, b2[0].y);
						
						b1[1].x = Math.max(b1[1].x, b2[1].x);
						b1[1].y = Math.max(b1[1].y, b2[1].y);
						Collections.swap(allboxes[i], k, allboxes[i].size() -1);
						allboxes[i].remove(allboxes[i].size() -1);
						k = j +1;	
					}else
						++k;	
				}
				hole = i == 1;
				// premier point en haut à gauche
				for(int x = b1[0].x; x < b1[1].x; ++x) {
					int samp = (pixels[x][b1[0].y] >> 16) & 0xFF;
					
					if((hole && (samp == 0)) || (!hole && (samp > 0))) {
						positions.add(new Vec2i(x, b1[0].y));
						break;
					}
				}
			}
			if(i == 0) {
				if(BoundaryBoxes != null)
					BoundaryBoxes.addAll(allboxes[i]);
			}else {
				if(BoundaryBoxesHole != null)
					BoundaryBoxesHole.addAll(allboxes[i]);
				else if(BoundaryBoxes != null)
					BoundaryBoxes.addAll(allboxes[i]);			
			}
		}
		return positions;
	}
	
	
	
	/*
	 
	 	// trou
					if(hole) {
						if( (samp == 0) ) {
							int upsamp = (pixels[x][y-1] >> 16) & 0xFF;
							// si pixel au dessus est uniform (tout rouge /noir )
							// alors on est encore peutetre dans un trou
							if(prevupsamp == upsamp || (upsamp > 0)) {
								prevupsamp = upsamp;
								continue;
							}else if(upsamp == 0) {
								// test bboxe
								stillin = false;
								for(Vec2i[] bbox : bboxes) {
									if(bbox[1].y +1 == y && (x <= bbox[1].x && x >= bbox[0].x)) {
										stillin = true;
										upd.add(bbox);
									}
								}
								if(!stillin) {
									bboxes.removeAll(upd);
									hole = false;
									added = false;

									mins.pop();
									int i = upd.size() -1;
									while(i-- > minx)
										upd.remove(i);
								}else continue;
							}
						}
						else {
							min = mins.peek();
							max = x -1;
							Vec2i[] box;
							added = false;
							hole = false;
							if(upd.size() > minx) added = true;
							else bboxes.add(new Vec2i[] {new Vec2i(min, y), new Vec2i(max, y)});								
							mins.pop();							
						}
					}else { // island
						if( (samp > 0) ) continue;
						else {
							min = mins.peek();
							max = x -1;
							Vec2i[] box;
							added = false;
							for (Vec2i[] bbox : bboxes) {
								
								// test if we are still in the current shape
									
								// test is part of bbox
								if(y == (bbox[1].y + 1)) {								
									stillin |= (x <= bbox[1].x && x >= bbox[0].x);
									if(min <= bbox[1].x && max >= bbox[0].x) {
										//bbox[1].y = y;
										box = bbox;
										upd.add(bbox);
										added = true;
										bbox[0].x = Math.min(min,  bbox[0].x);
										bbox[1].x = Math.max(max,  bbox[1].x);
										
									}
									//stillin = false;
								}
							}
							if(!added) 
								bboxes.add(new Vec2i[] {new Vec2i(min, y), new Vec2i(max, y)});
						
								
							// changement de forme mais dans le même ilôt
							if(stillin) {
								mins.add(x);
								minx = upd.size();
								prevupsamp = (pixels[x][y-1] >> 16) & 0xFF;
								// si dans un trou alors on reprend les bbox des ilots
								// sinon ceux des trous
								hole = ((mins.size() & 1) == 0);
								bboxes = allboxes[hole ? 1 : 0];
							}else
								mins.pop();
						}
					
					}
					
	 
	 */
	
}
