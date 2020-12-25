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
import java.nio.IntBuffer;
import java.util.ArrayList;

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
	public static float PIXEL_SIZE = 0.2f; 
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
		//slicer();
		Obj3DModel obj = new Obj3DModel(OBJ_PATH + NAME + ".obj");
		int numSlice = 113;
		Slice slice = getSlice(numSlice, obj);
		Vec3f size = obj.getMax().sub(obj.getMin());
		
		slice.remap(OFFSET, PIXEL_SIZE);

		//slicerCPU(slice, 5);
		int[][] pixels = slicerGPU(slice, numSlice);
		
	

		/*
		Vec2i pos = getCorner(pixels);
		int x = pos.x;
		int y = pos.y;
		pixels[x][y] = 0xFF;
		pixels[x+1][y] = 0xFF;
		pixels[x-1][y] = 0xFF;
		pixels[x][y+1] =0xFF;
		pixels[x][y-1] = 0xFF;
*/
		ArrayList<Vec2i> path = getPath(erode(pixels, 1));
		
		BufferedImage img = new BufferedImage(WIDTH, HEIGHT,BufferedImage.TYPE_INT_RGB);
		BufferedImage img2 = new BufferedImage(WIDTH, HEIGHT,BufferedImage.TYPE_INT_RGB);
		
		setData(img, pixels);
		setData(img2, pixels);
		Graphics2D ctx = img2.createGraphics();
		for(int i = 0; i < path.size() - 1; ++i) {
	

			ctx.draw(new Line2D.Float(path.get(i).x, path.get(i).y, path.get(i+1).x, path.get(i+1).y));
		}
		saveImage(img, RESULT_PATH + NAME  + "GPU" + numSlice + ".png");
		saveImage(img2, RESULT_PATH + NAME  + "GPUPath" + numSlice + ".png");
	
		
		setData(img, erode(pixels, 1));
		saveImage(img, RESULT_PATH + NAME  + "Erode" + numSlice + ".png");

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
	public static Vec2i getCorner(int[][] pixels) {
			

		int minx = Integer.MAX_VALUE;
		int m = -1;
		Vec2i pos = new Vec2i(0,0);
		for(int y = 0; y < pixels[0].length && m < 0 ; ++y) {
			for(int x = 0; x < pixels.length; ++x) {
				int samp = pixels[x][y];
				int d =x + y;//(float) new Vec2i(x, y).length();
				if(((samp  >> 16) & 0xFF) > 0 && minx > d) {
					System.out.println("position: " + x + " " + y);
					minx = d;
					m = x;
					pos.x = x;
					pos.y = y;	
				}
			}
		}
		
		return pos;
	}
	
	public static ArrayList<Vec2i> getPath(int[][] pixels) {
		
		ArrayList<Vec2i> path = new ArrayList<>();
		Vec2i pos = getCorner(pixels), tmp = new Vec2i(0, 0);
		path.add(new Vec2i(pos.x, pos.y));
		int x = 0, y = 1, j= 0;

		Vec2i neighbor = new Vec2i(0, 0);
		Vec2i noon = new Vec2i(0,0);
		boolean found = false;
		boolean val = false;
		boolean nval = false;
		do {
			found = false;
		
			boolean sens = false;
			noon.x = pos.x;
			noon.y = pos.y + 1;
			
			x = 0; y = 1;
			j = 0;
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
				
				val = ((pixels[noon.x][noon.y] >> 16) & 0xFF) > 0; // pix courant
				nval = ((pixels[neighbor.x][neighbor.y]  >> 16) & 0xFF) > 0; // pix suivant
				/*
				if(neighbor.x < 0 || neighbor.y < 0 || neighbor.x > WIDTH || neighbor.y > HEIGHT) {
				 
					neighbor.x = pos.x + x;
					neighbor.y = pos.y + y;
					
					tmp.x = neighbor.x;
					tmp.y = neighbor.y;
					continue;
				}
				*/
				if(val != nval) {
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
						//pixels[pos.x][pos.y] = 0x00FF00;
						found = true;
						break;
					}
					
				}
		
				noon.x = neighbor.x;
				noon.y = neighbor.y;
				
				val = nval;
			}
		}while(!pos.equals(path.get(0)) && found);
		
		return path;
	}
	
	public static int[][] erode(int[][] pixels, int nbErosion) {
		
		int[][] erodePixs = copy(pixels);//img.getSubimage(0, 0, img.getWidth(), img.getHeight());
		
		int kernel = (int) ((BUSE_SIZE * 0.5f) / PIXEL_SIZE);
		for(int i = nbErosion; i > 0; --i) {
			int[][] tmp = copy(erodePixs);
			clearData(erodePixs);
			
			for(int y = 0; y < tmp[0].length; ++y) {
				for(int x = 0; x < tmp.length; ++x) {
					int samp = tmp[x][y] & 0XFFFFFF;
					if(samp> 0) {
						boolean inc = true;
						for(int x1 = -kernel; x1 <= kernel && inc; ++x1) {
							for(int y1 = -kernel; y1 <= kernel && inc; ++y1) {
								int samp1 = tmp[x+x1][y + y1] & 0xFFFFFF;
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
	
			Slice slice = new Slice(plane.m_Point.z);
			// intersection plane - triangles + raccordement ----------
			for (int[] face : obj.faces) {
				obj.getSlice(plane, face, edges);
			}
	
			slice.makeEdge(edges);
			 slice.fill(ctx);
			// Printing section-------------------------- 
			// Graphics2D ctx = image.createGraphics();
			//System.out.println("frame " + num + " nb d'edge: " + edges.size() + " z height: "+ plane.m_Point.z);
			
			Vec2f offset = new Vec2f(50.f, 50.f);
			slice.printEdge(ctx, offset, 5.f);
	
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
