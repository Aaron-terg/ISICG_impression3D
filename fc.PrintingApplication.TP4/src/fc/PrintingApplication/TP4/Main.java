package fc.PrintingApplication.TP4;

import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL20.glBindAttribLocation;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Stack;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import fc.GLObjects.GLProgram;
import fc.GLObjects.GLRenderTarget;
import fc.GLObjects.GLShaderMatrixParameter;
import fc.Math.Matrix;
import fc.Math.Plane;
import fc.Math.Vec2f;
import fc.Math.Vec2i;
import fc.Math.Vec3f;




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
	public static String NAME = "yoda";
	
	public static float[][][] readBackAsFloat(int id, int format, int type) // each [height][width][4] (4: R,G,B,A. Each value is between [0.0f,1.0f])
	{
	
		GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
		GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
		int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
		int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
		int numComponents = 4; //rt.getNumPixelFormatComponents();
		IntBuffer buffer = BufferUtils.createByteBuffer(width * height * 4 */* getSizeOfOneComponentsInBytes() */ numComponents).asIntBuffer();
		GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, format, type, buffer);
		float[][][] data = new float[height][width][numComponents];
		for (int y = 0; y < height; y++)
		{
			for (int x = 0; x < width; x++)
			{
				// TODO: this only works when one component = 32 bits, as we have a IntBuffer
				int i = (x + (width * y)) * numComponents;
				for (int j=0; j < numComponents; j++)
				{
					int val = buffer.get(i + j);
					data[y][x][j] = Float.intBitsToFloat(val);
				}
			}
		}
		return data;
	}
	
	public static void main(String[] args)
	{
		slicer();
		Obj3DModel obj = new Obj3DModel(OBJ_PATH + NAME + ".obj");
		int numSlice = 113;
		Slice slice = getSlice(numSlice, obj);
		Vec3f size = obj.getMax().sub(obj.getMin());
		int h_size = (int) Math.ceil((obj.getMax().x - obj.getMin().x) / PIXEL_SIZE);
		int v_size = (int) Math.ceil((obj.getMax().y - obj.getMin().y) / PIXEL_SIZE);

		OFFSET.x = h_size;
		OFFSET.y = v_size;
		slice.remap(OFFSET, PIXEL_SIZE);

		//slicerCPU(slice, 5);
		int[][] pixels = slicerGPU(slice, numSlice);
	

		/*
		Vec2i pos = getCorners(pixels).get(0);
		int x = pos.x;
		int y = pos.y;
		pixels[x][y] = 0xFF;
		pixels[x+1][y] = 0xFF;
		pixels[x-1][y] = 0xFF;
		pixels[x][y+1] =0xFF;
		pixels[x][y-1] = 0xFF;
*/
		
		BufferedImage img = new BufferedImage(WIDTH, HEIGHT,BufferedImage.TYPE_INT_RGB);
		BufferedImage img2 = new BufferedImage(WIDTH, HEIGHT,BufferedImage.TYPE_INT_RGB);
		
		setData(img, pixels);
	
	  	ArrayList<Vec2i[]> boxes = new ArrayList<>();
	  	ArrayList<Vec2i> points = getCorners(pixels, boxes); 
	  		  	
	  		//pixels[points.get(i).x][points.get(i).y] = 0x00FF00;

	  	setData(img2, pixels);

	  	Graphics2D ctx = img2.createGraphics();
		for(int i = 0; i < boxes.size(); ++i) {
			ctx.draw(new Line2D.Float(boxes.get(i)[0].x, boxes.get(i)[0].y, boxes.get(i)[1].x, boxes.get(i)[0].y));
			ctx.draw(new Line2D.Float(boxes.get(i)[1].x, boxes.get(i)[0].y, boxes.get(i)[1].x, boxes.get(i)[1].y));
			ctx.draw(new Line2D.Float(boxes.get(i)[0].x, boxes.get(i)[1].y, boxes.get(i)[1].x, boxes.get(i)[1].y));
			ctx.draw(new Line2D.Float(boxes.get(i)[0].x, boxes.get(i)[0].y, boxes.get(i)[0].x, boxes.get(i)[1].y));

		}
	  	for(int i = 0; i < points.size(); ++i) 
	  		img2.setRGB(points.get(i).x, points.get(i).y, 0x00FF00);
	
		ArrayList<ArrayList<Vec2i>> paths;// = getPaths(erode(pixels, 1));
		setData(img2, pixels);
	
		Graphics2D ctxpath = img.createGraphics();
		int k = 0;
		int nberode = 1;
		while(k < nberode) {
			paths = getPaths(erode(pixels, k++));
			for(ArrayList<Vec2i> path : paths) {
				for(int i = 0; i < path.size() - 1; ++i)
					ctxpath.draw(new Line2D.Float(path.get(i).x, path.get(i).y, path.get(i+1).x, path.get(i+1).y));
			}
		}
		
		saveImage(img, RESULT_PATH + NAME  + "GPU" + numSlice + ".png");
		saveImage(img2, RESULT_PATH + NAME  + "GPUPath" + numSlice + ".png");
	
		
	//	setData(img, erode(pixels, 1));
	//	saveImage(img, RESULT_PATH + NAME  + "Erode" + numSlice + ".png");

	}
	// IMG buffer - int[][] pixel
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
	
	// Generate path
	public static ArrayList<Vec2i> getCorners(int[][] pixels) {		
		return getCorners(pixels, null);
	}
	
	
	public static ArrayList<Vec2i> getCorners(int[][] pixels, ArrayList<Vec2i[]> BoundaryBoxes){
		
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
							bboxes = allboxes[hole ? 0 : 1];
						}else
							mins.pop();
					}
					
				}
				/*
				int d =x + y;//(float) new Vec2i(x, y).length();
				if(((samp  >> 16) & 0xFF) > 0 && minx > d) {
					minx = d;
					m = x;
					pos.x = x;
					pos.y = y;	
				}
				*/
			}
			for(Vec2i[] bbox: upd) bbox[1].y = y;
			upd.clear();

			
		}

		
		for (int i = 0; i < allboxes.length; i++) {
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
			if(BoundaryBoxes != null)
				BoundaryBoxes.addAll(allboxes[i]);
		}
		return positions;
	}
	
	public static ArrayList<ArrayList<Vec2i>> getPaths(int[][] pixels) {
		ArrayList<ArrayList<Vec2i>> paths = new ArrayList<>();
		ArrayList<Vec2i> corners = getCorners(pixels);
		for(Vec2i pos: corners) {
			ArrayList<Vec2i> path= getPath(pixels, pos), npath = new ArrayList<>();
			// smooth path			
		//	Deque<Vec2i> smPath = smoothPath(path, 0, path.size());
		//	while(!smPath.isEmpty()) npath.add(smPath.poll());
			paths.add(path);
		}
		
		return paths;
	}
	
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
			if(neighbor.x < 0 || neighbor.x > WIDTH || neighbor.y < 0 || neighbor.y > HEIGHT) continue;
	
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
				int i = 1;
					do {
						pos.x = path.get(path.size() - i).x;
						pos.y = path.get(path.size() - i++).y;
						
						found = next(pixels, pos, path, prev);
					
	
					}while(!found && !pos.equals(path.get(0)));
				}

				
			}
			prev.clear();
			
			
		}while(!pos.equals(path.get(0)) && found);
		
		return path;
	}
	
	public static Deque<Vec2i> smoothPath(ArrayList<Vec2i> path, int start, int end){
		
		int mid = end / 2;
		float dist = 0f;
		Deque<Vec2i> smoothedPath = new LinkedList<>();
		
		
		Vec2i segment = path.get(mid).sub(path.get(start));
		Vec2f N = new Vec2f(segment.y,- segment.x);
		float nl = N.length();
		
		N.x /= nl;
		N.y /= nl;
		Vec2f max = new Vec2f(Float.MIN_NORMAL, -1);
		for(int p = start; p < mid; ++p) {
			Vec2f PS = new Vec2f(path.get(p).x - path.get(mid).x, path.get(p).y - path.get(mid).y);
			dist = Math.abs(N.dot(PS));
			if(dist > max.x) {
				max.x = dist;
				max.y = p;
			}
		}
		if(max.x > 1.5f) {
			smoothedPath.addAll(smoothPath(path, start, (int)max.y));
			smoothedPath.addAll(smoothPath(path, (int)max.y, mid));
		}
		else if(max.y >= 0f)		
			smoothedPath.addLast(path.get((int)max.y));
		segment = path.get(end-1).sub(path.get(mid));
		N.x  = segment.y;
		N.y = -segment.x;
		nl = N.length();
		
		N.x /= nl;
		N.y /= nl;
		max.x = Float.MIN_NORMAL;
		max.y = -1;
		for(int p = mid+1; p < end; ++p) {
			Vec2f PS = new Vec2f(path.get(p).x - path.get(mid).x, path.get(p).y - path.get(mid).y);
			dist = Math.abs(N.dot(PS));
			if(dist > max.x) {
				max.x = dist;
				max.y = p;
			}
		}
		
		if(max.y >= 0f)		
			smoothedPath.addLast(path.get((int)max.y));

		if(max.x > 1.5f) {
			smoothedPath.addAll(smoothPath(path, mid, (int)max.y));
			smoothedPath.addAll(smoothPath(path, (int)max.y, end));
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
							if(x+x1 < 0 || x+x1 > WIDTH) continue;
							for(int y1 = -kernel; y1 <= kernel && inc; ++y1) {
								if(y + y1 < 0 || y + y1 > HEIGHT) continue;
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

	
	// Rasterisation / Tranchage
	public static Slice getSlice(int numSlice, Obj3DModel obj) {
		int nbSlice = (int)((obj.getMax().z - obj.getMin().z) / VERTICAL_STEP);
		numSlice = Math.min(nbSlice, numSlice);

		Plane plane = new Plane(obj.getMin(), new Vec3f(0.f, 0.f, 1.f));
		ArrayList<EdgeSliceData> edges = new ArrayList<>();
		
		plane.m_Point.z += VERTICAL_STEP * numSlice;
		Slice slice = new Slice(plane.m_Point.z);
		// intersection plane - triangles + raccordement ----------
		for (int[] face : obj.faces) {
			obj.getSlice(plane, face, edges);
		}
		slice.makeEdge(edges);
		
		return slice;
	}
	
	public static int[][] slicerGPU(Slice slice, int numSlice) {
		glfwInit();
		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		long window = glfwCreateWindow(WIDTH, HEIGHT, "Dummy", NULL, NULL);
		
		glfwMakeContextCurrent(window);
		GL.createCapabilities();

		int interal_format = GL30.GL_RGBA32I,
				format = GL30.GL_RGBA_INTEGER,
				type = GL11.GL_INT;
		GLRenderTargetStencil rt = new GLRenderTargetStencil(WIDTH, HEIGHT, interal_format, format, type);
		
		GLProgram shader = new GLProgram()
		{
			@Override
			protected void preLinkStep()
			{
				glBindAttribLocation(m_ProgramId, 0, "in_Position");
			}
		};
		shader.init(new MyVShader(), new MyFShader());
		
		GLShaderMatrixParameter matParam = new GLShaderMatrixParameter("u_mvpMatrix");
		matParam.init(shader);
		int nbIsland = slice.islands.size() -1;
		ArrayList<GLVec2fTriangle> tris = new ArrayList<>();
		//GLVec2iTriangle tri = new GLVec2iTriangle(new Vec2i[]{new Vec2i(0,0), new Vec2i(599,0), new Vec2i(300,300)});
		GLVec2fTriangle tri = new GLVec2fTriangle(slice);
		for(int i = 0; i < slice.islands.size() -1; ++i) {
			int beg = slice.islands.get(i);
			int end = slice.islands.get(i+1);
			Vec2f a[] = new Vec2f[end - beg];
			tris.add(new GLVec2fTriangle(slice.edges.subList(beg, end)));
			
			
		}
		
		GL11.glViewport(0, 0, WIDTH, HEIGHT);
		rt.bind();
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL11.GL_STENCIL_BUFFER_BIT);
    	GL11.glEnable(GL11.GL_STENCIL_TEST);		
		GL11.glStencilMask(0xFF);
		GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_INVERT);
		GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
		GL11.glColorMask(false,false,false,true);
		shader.begin();
		matParam.set(Matrix.createOrtho(0, WIDTH, 0, HEIGHT, -1, 1));
		
		//for(GLVec2fTriangle tr : tris) tr.render();

		tri.render();
		shader.end();
	
		GL11.glStencilFunc(GL11.GL_NOTEQUAL, 0, 0xFF);
		GL11.glStencilMask(0x00);

		GL11.glColorMask(true,true,true,true);
		shader.begin();
		matParam.set(Matrix.createOrtho(0, WIDTH, 0, HEIGHT, -1, 1));
		tri.render();
		shader.end();
		
		rt.unbind();
		
		float[][][] pixels = readBackAsFloat(rt.getFBOId(), format, type);
		
		int[][] imgPix = new int[WIDTH][HEIGHT];
		for (int y=0; y < pixels.length; y++)
		{
			for (int x=0; x < pixels[0].length; x++)
			{
				int r = (int)(pixels[y][x][0] * 255.0f);
				int g = (int)(pixels[y][x][1] * 255.0f);
				int b = (int)(pixels[y][x][2] * 255.0f);

				imgPix[x][y] = (r<<16) | (g<<8) | b;
			}
		}
		rt.dispose();
		
		return imgPix;
	}
	
	public static int[][] slicerCPU(Slice slice, int numSlice) {

		BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		Graphics2D ctx = image.createGraphics();
		ctx.clearRect(0, 0, WIDTH, HEIGHT);
	
	/*	Vec2f offset = new Vec2f(WIDTH / 2, HEIGHT / 2);
		slice.remap(OFFSET, PIXEL_SIZE);
	*/
		 slice.fill(ctx);
		// Printing section-------------------------- 

		slice.printEdge(ctx, OFFSET, PIXEL_SIZE);			
			
		return getData(image);
	}
	
	// -------------------------------------------
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

	// -------------------------------------------
	// MainApp
	public static void slicer(){
		Obj3DModel obj = new Obj3DModel(OBJ_PATH + NAME + ".obj");
		Plane plane = new Plane(obj.getMin(), new Vec3f(0.f, 0.f, 1.f));
		BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		Graphics2D ctx = image.createGraphics();
		int num = 0;
		int nbSlice = (int)((obj.getMax().z - obj.getMin().z) / VERTICAL_STEP);
	
		// for each plane along z
		ArrayList<EdgeSliceData> edges = new ArrayList<>();
		for (; plane.m_Point.z <= obj.getMax().z; plane.m_Point.z += VERTICAL_STEP) {
			edges.clear();
			ctx.clearRect(0, 0, WIDTH, HEIGHT);
	
			//Slice slice = getSlice(num, obj);
			
			Slice slice = new Slice(plane.m_Point.z);
			// intersection plane - triangles + raccordement ----------
			for (int[] face : obj.faces) {
				obj.getSlice(plane, face, edges);
			}
			
			slice.makeEdge(edges);
			slice.remap(OFFSET, PIXEL_SIZE);
			int[][] pixels = slicerGPU(slice, num);
			
			setData(image, pixels);
			
			ArrayList<ArrayList<Vec2i>> paths = getPaths(erode(pixels, 1));
			int k = 0;
			int nberode = 10;
			while(k < nberode) {
				paths = getPaths(erode(pixels, k++));
				for(ArrayList<Vec2i> path : paths) {
					for(int i = 0; i < path.size() - 1; ++i)
						ctx.draw(new Line2D.Float(path.get(i).x, path.get(i).y, path.get(i+1).x, path.get(i+1).y));
				}
			}
			
			 //slice.fill(ctx);
			// Printing section-------------------------- 
			// Graphics2D ctx = image.createGraphics();
			//System.out.println("frame " + num + " nb d'edge: " + edges.size() + " z height: "+ plane.m_Point.z);
			
			//Vec2f offset = new Vec2f(50.f, 50.f);
			//slice.printEdge(ctx, offset, 5.f);
	
			if(saveImage(image, RESULT_PATH +"cpu/" + NAME  + num + ".png"))
				num++;
	
		}
	}
	

	//
	// Dans ce TP
	// - rasteriser les perimetres (remplissage de polygone arbitraire)
	// - rasteriser avec la methode de Guthe et al.
	//
	
	public static void checkGLErrorState()
	{
		int err = GL11.glGetError();
		if (err != 0)
			throw new IllegalStateException("OpenGL is in error state " + err);
	}
}
