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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import fc.GLObjects.GLProgram;
import fc.GLObjects.GLShaderMatrixParameter;
import fc.Math.Matrix;
import fc.PrintingApplication.TP4.Plane;
import fc.Math.Vec2f;
import fc.PrintingApplication.TP4.Vec2;
import fc.Math.Vec3f;

public class Rasterer {
	
	public static long window = 0;
	public static GLProgram shader = null;
	public static GLShaderMatrixParameter matParam = null;
	
	// Rasterisation / Tranchage ---------------------------------------		
		public static float[][][] readBackAsFloat(int id, int format, int type) // each [Main.HEIGHT][Main.WIDTH][4] (4: R,G,B,A. Each value is between [0.0f,1.0f])
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
		
		public static void initGPU() {
			glfwInit();
			glfwDefaultWindowHints();
			glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
			window = glfwCreateWindow(Main.WIDTH, Main.HEIGHT, "Dummy", NULL, NULL);
			
			glfwMakeContextCurrent(window);
			GL.createCapabilities();

			
			shader = new GLProgram()
			{
				@Override
				protected void preLinkStep()
				{
					glBindAttribLocation(m_ProgramId, 0, "in_Position");
				}
			};
			shader.init(new MyVShader(), new MyFShader());
			
			matParam = new GLShaderMatrixParameter("u_mvpMatrix");
			matParam.init(shader);
			GL11.glViewport(0, 0, Main.WIDTH, Main.HEIGHT);
			int interal_format = GL30.GL_RGBA32I,
					format = GL30.GL_RGBA_INTEGER,
					type = GL11.GL_INT;
			
			
		}
		// Guth et al methode rasterisation of the slice at numSLice * VERTICAL_STEP
		public static int[][] rasterGPU(Slice slice, int numSlice) {
			
			if(shader == null || matParam == null)
				initGPU();
			
			// Fan tesselation section-------------------------- 
			int nbIsland = slice.islands.size() -1;
			ArrayList<GLVec2fTriangle> tris = new ArrayList<>();
			GLVec2fTriangle tri = new GLVec2fTriangle(slice);
			for(int i = 0; i < slice.islands.size() -1; ++i) {
				int beg = slice.islands.get(i);
				int end = slice.islands.get(i+1);
				Vec2f a[] = new Vec2f[end - beg];
				tris.add(new GLVec2fTriangle(slice.edges.subList(beg, end)));
				
				
			}
			int interal_format = GL30.GL_RGBA32I,
					format = GL30.GL_RGBA_INTEGER,
					type = GL11.GL_INT;
			GLRenderTargetStencil rt = new GLRenderTargetStencil(Main.WIDTH, Main.HEIGHT, interal_format, format, type);

			
			// Printing section-------------------------- 
			rt.bind();
	        
			glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
	        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL11.GL_STENCIL_BUFFER_BIT);
	        
	   
	        // Stencil test section-------------------------- 
	    	GL11.glEnable(GL11.GL_STENCIL_TEST);		
			GL11.glStencilMask(0xFF);
			GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_INVERT);
			GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
			GL11.glColorMask(false,false,false,true);
			
			shader.begin();
				matParam.set(Matrix.createOrtho(0, Main.WIDTH, 0, Main.HEIGHT, -1, 1));
				tri.render();
			shader.end();
		
			// Pass 2 -------------------------- 

			GL11.glStencilFunc(GL11.GL_NOTEQUAL, 0, 0xFF);
			GL11.glStencilMask(0x00);
			GL11.glColorMask(true,true,true,true);

			shader.begin();
				matParam.set(Matrix.createOrtho(0, Main.WIDTH, 0, Main.HEIGHT, -1, 1));
				tri.render();
			shader.end();
			
			rt.unbind();
			

			float[][][] pixels;
	
			
			pixels = rt.readBackAsFloat();//readBackAsFloat(rt.getFBOId(), format, type);
			
			int[][] imgPix = new int[Main.WIDTH][Main.HEIGHT];
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
		
		// scanline rasterisation of the slice numSlice *VERTICAL_STEP
		public static int[][] rasterCPU(Slice slice, int numSlice) {

			BufferedImage image = new BufferedImage(Main.WIDTH, Main.HEIGHT, BufferedImage.TYPE_INT_RGB);
			Graphics2D ctx = image.createGraphics();
			ctx.clearRect(0, 0, Main.WIDTH, Main.HEIGHT);

			// Printing section-------------------------- 
			slice.fill(ctx);
			slice.printEdge(ctx);			
				
			return Main.getData(image);
		}

		// -------------------------------------------
		// MainApp
		// Render every slice using Gth et al methode and compute path of the buse
		public static void slicer(Obj3DModel obj){
			if(obj == null) {
				obj = new Obj3DModel(Main.OBJ_PATH + Main.NAME + ".obj");
				int h_size = (int) Math.ceil((obj.getMax().x - obj.getMin().x) / Main.PIXEL_SIZE);
				int v_size = (int) Math.ceil((obj.getMax().y - obj.getMin().y) / Main.PIXEL_SIZE);
				Main.WIDTH = h_size;
				Main.HEIGHT = v_size;
				Main.OFFSET.x = h_size *0.5f;
				Main.OFFSET.y = v_size *0.5f;
			}

			BufferedImage image = new BufferedImage(Main.WIDTH, Main.HEIGHT, BufferedImage.TYPE_INT_RGB);
			Graphics2D ctx = image.createGraphics();
						
			int kernel = (int) ((Main.BUSE_SIZE) / Main.PIXEL_SIZE);
			int kernelPerimeter = (int) ((Main.BUSE_SIZE * 0.5) / Main.PIXEL_SIZE);
			int[][] shells;
			
			int num = 0, max, n;
			int nbSlice = (int)((obj.getMax().z - obj.getMin().z) / Main.VERTICAL_STEP);
			
			// for each plane along z
			Plane plane = new Plane(obj.getMin(), new Vec3f(0.f, 0.f, 1.f));
			Slice slice = new Slice(plane, obj, true);
			Deque<int[][]> slices = new LinkedList<>();
			int[][] pixels = rasterGPU(slice, 0);
			slices.add(pixels);
			
			int[][] dist = new int[Main.WIDTH][Main.HEIGHT];
			int[][] distTop = new int[Main.WIDTH][Main.HEIGHT];

			for(int i = 0; i< Main.WIDTH; ++i)
				for(int j = 0; j < Main.HEIGHT; ++j) {
					if( ((pixels[i][j] >> 16) & 0xFF)  == 0) dist[i][j] = -1;
					else dist[i][j] = 0;
				}
			
			plane.m_Point.z +=  Main.VERTICAL_STEP;

			slice.setSlice(plane, obj, true);
			pixels = rasterGPU(slice, 1);

			// set distTop to num slice if pix black else -1
			for(int i = 0; i< Main.WIDTH; ++i)
				for(int j = 0; j < Main.HEIGHT; ++j) {
					if( ((pixels[i][j] >> 16) & 0xFF)  == 0) distTop[i][j] = 1;
					else distTop[i][j] = -1;
				}
			slices.add(pixels);

			num = 0;
			do {
		
				
				// Color z axe ---------------------------------------
				
				pixels = slices.poll();

				max = -1;
			
				for(int x = 0; x < Main.WIDTH; ++x) {
					for(int y = 0; y < Main.HEIGHT; ++y) {
						if(dist[x][y] >= 0) {

							if(dist[x][y] < 4) pixels[x][y] = 0xFF0000;
							else {
								float u = ((float) dist[x][y]) / (float)nbSlice;
								int val = ((int)(255f * (1f -u) + 64f * u));
								pixels[x][y] =  val << 16;
							}
						}else pixels[x][y] = 0x000000;
						
						//if(((slices.peek()[x][y] >> 16) & 0xFF) > 0) ++dist[x][y];
						//else dist[x][y] = -1;
						n = distTop[x][y]  - num;
						if(n > 0) {
							dist[x][y] = Math.min(n, dist[x][y]);
						}else {
							++dist[x][y];
							int npixs[][];
							n = -1;
							for(Iterator<int[][]>it = slices.iterator(); it.hasNext() && n < dist[x][y];  ++n) {
								npixs = it.next();
								int sl =num + n +1;
								if(((npixs[x][y] >> 16) & 0xFF) == 0 || sl >= nbSlice ) {
									dist[x][y] = Math.min(dist[x][y], n);
									distTop[x][y] = num + n +1;
									break;
								}
							}
						}
						max = Math.max(max, dist[x][y]);
					}
				}
				//ctx.clearRect(0, 0, Main.WIDTH, Main.HEIGHT);
				Main.setData(image, pixels);
				Main.saveImage(image, Main.RESULT_PATH +"raw/" + Main.NAME  + num + ".png");

				// path tracing --------------------------------------
				
				Color c = ctx.getColor();
				ArrayList<ArrayList<Vec2>> paths;	
				
				ctx.setColor(Color.BLUE);
				shells = Main.erode(pixels, kernelPerimeter);
				if(shells != null) {
					
					paths = Main.getPaths(shells);
					for(ArrayList<Vec2> path : paths) {
						for(int i = 0; i < path.size() - 1; ++i)
							ctx.draw(new Line2D.Float(path.get(i).x, path.get(i).y, path.get(i+1).x, path.get(i+1).y));
					}
					while((shells = Main.erode(shells, kernel)) != null) {
						paths = Main.getPaths(shells);
						for(ArrayList<Vec2> path : paths) {
							for(int i = 0; i < path.size() - 1; ++i)
								ctx.draw(new Line2D.Float(path.get(i).x, path.get(i).y, path.get(i+1).x, path.get(i+1).y));
						}
					}
				}
				ctx.setColor(c);	
				
				Main.saveImage(image, Main.RESULT_PATH +"path/" + Main.NAME  + num + ".png");
				++num;
				do {
					
					n = num + slices.size();	
					if(n < nbSlice) {
						
						plane.m_Point.z = obj.getMin().z + n * Main.VERTICAL_STEP;
						slice.setSlice(plane, obj, true);
						slices.add(rasterGPU(slice, n));
					}else break;
				}while(slices.size() < max);
		
			}while(!slices.isEmpty());
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
