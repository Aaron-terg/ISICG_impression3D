package fc.PrintingApplication.TP4;

import java.io.File;
import java.util.ArrayList;

import com.owens.oobjloader.builder.Build;
import com.owens.oobjloader.builder.Face;
import com.owens.oobjloader.builder.FaceVertex;
import com.owens.oobjloader.parser.Parse;

import fc.PrintingApplication.TP4.Plane;
import fc.Math.Vec2f;
import fc.Math.Vec3f;

public class Obj3DModel {

	public ArrayList<Vec3f> vertices;
	public ArrayList<int[]> faces;

	private Vec3f boundingBox[];

	public Obj3DModel(String path) {
		this.boundingBox = new Vec3f[] {
				new Vec3f(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
				new Vec3f(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY) };
		this.faces = new ArrayList<int[]>();
		this.vertices = new ArrayList<Vec3f>();

		parseObjFile(path);

	}

	public Vec3f getMin() {
		return this.boundingBox[0];
	}

	public Vec3f getMax() {
		return this.boundingBox[1];
	}

	private Vec2f intersect(Plane plan, Vec3f a, Vec3f b){

			Vec3f edge = b.sub(a);

			float z = Math.abs(plan.m_Point.z - b.z);

			// th. Thales: (z / edge.z) = (distIntersect / edge.length)
			float ratioz = z / edge.z;
			float distIntersect = edge.length() * ratioz;

			// intersection point
			Vec3f IntersectionOnEdge = b.add(edge.norm().mul(-distIntersect));

			return new Vec2f(IntersectionOnEdge.x, IntersectionOnEdge.y);
			

	}

	public void getSlice(Plane plan, int[] triangle, ArrayList<EdgeSliceData> slice) {


		Vec2f edgePlan[] = new Vec2f[] { new Vec2f(0.f, 0.f), new Vec2f(0.f, 0.f) };
		int count = 0;

		// TODO coplanar triangle a.z = b.z = c.z = plan.z
		Vec3f a = vertices.get(triangle[0]);
		Vec3f b = vertices.get(triangle[1]);
		Vec3f c = vertices.get(triangle[2]);
		
		Vec3f ab = b.sub(a);
		Vec3f ca = a.sub(c);

		// test si le triangle est coplanaires et à la même hauteur du plan
		if (a.z == plan.m_Point.z && Math.abs(plan.m_Normal.dot(ab.cross(ca.neg()))) == 1.f) {
			
			return;
			// slice.addEdge(new Vec2f[] { new Vec2f(a.x, a.y), new Vec2f(b.x, b.y) });
			// slice.addEdge(new Vec2f[] { new Vec2f(b.x, b.y), new Vec2f(c.x, c.y) });
			// slice.addEdge(new Vec2f[] { new Vec2f(c.x, c.y), new Vec2f(a.x, a.y) });
		}

		// for each edges
		for (int i = 0; i <= 2 && count < 2; i++) {
			a = vertices.get(triangle[i]);
			b = vertices.get(triangle[(i + 1) % 3]);
		
			// arrete colinéaires au plan
			if(a.z == plan.m_Point.z && b.z == plan.m_Point.z){
				if((a.x > b.x) || (a.x == b.x && a.y > b.y)){
					Vec3f tmp = b;
					b = a;
					a = tmp;
				}
				//slice.addEdge(new Vec2f[]{new Vec2f(a.x, a.y), new Vec2f(b.x, b.y)});
				slice.add(new EdgeSliceData(new Vec2f(a.x, a.y), new Vec2f(b.x, b.y)));
				return;
			}

			if (a.z > b.z) {
				Vec3f tmp = b;
				b = a;
				a = tmp;
			}
			// premature exit: edge below/above of plane
			if (b.z < plan.m_Point.z || a.z > plan.m_Point.z)
				continue;

			Vec2f hit = intersect(plan, a, b);
			edgePlan[count].x = hit.x;
			edgePlan[count].y = hit.y;
			
			count++;
		}

		if(count == 2) {
			slice.add(new EdgeSliceData(new Vec2f(edgePlan[0].x, edgePlan[0].y), new Vec2f(edgePlan[1].x, edgePlan[1].y)));
		//	slice.addEdge(edgePlan);
		}

	}

	private void enlarge(Vec3f[] boundingBox, Vec3f v) {
		boundingBox[0].x = Math.min(boundingBox[0].x, v.x);
		boundingBox[0].y = Math.min(boundingBox[0].y, v.y);
		boundingBox[0].z = Math.min(boundingBox[0].z, v.z);

		boundingBox[1].x = Math.max(boundingBox[1].x, v.x);
		boundingBox[1].y = Math.max(boundingBox[1].y, v.y);
		boundingBox[1].z = Math.max(boundingBox[1].z, v.z);
	}

	private static void addMargin(Vec3f[] boundingBox, Vec3f margin) {
		boundingBox[0] = boundingBox[0].sub(margin);
		boundingBox[1] = boundingBox[1].add(margin);
	}

	// Dans ce TP, lire et parser un fichier OBJ
	// puis realiser des coupes par plan Z.
	// Une fois les perimetres extraits:
	// - effectuer une triangulation de Delaunay contrainte.
	// - rasteriser dans une bitmap. Se r�f�rer � l'�nonc� du TP pour tous les
	// d�tails.
	//
	public void parseObjFile(String filename) {
		try {
			Build builder = new Build();
			Parse obj = new Parse(builder, new File(filename).toURI().toURL());

			// Enumeration des sommets

			for (FaceVertex vertex : builder.faceVerticeList) {
				float x = vertex.v.x;
				float y = vertex.v.y;
				float z = vertex.v.z;

				vertices.add(new Vec3f(x, y, z));
				enlarge(boundingBox, vertices.get(vertex.index));

				// ...
			}

			addMargin(boundingBox, new Vec3f(10.f, 10.f, 0.f));
			// Enumeration des faces (souvent des triangles, mais peuvent comporter plus de
			// sommets dans certains cas)

			for (Face face : builder.faces) {
				// Parcours des triangles de cette face

				for (int i = 1; i <= (face.vertices.size() - 2); i++) {
					int vertexIndex1 = face.vertices.get(0).index;
					int vertexIndex2 = face.vertices.get(i).index;
					int vertexIndex3 = face.vertices.get(i + 1).index;

					FaceVertex vertex1 = builder.faceVerticeList.get(vertexIndex1);
					FaceVertex vertex2 = builder.faceVerticeList.get(vertexIndex2);
					FaceVertex vertex3 = builder.faceVerticeList.get(vertexIndex3);

					// ...
					faces.add(new int[] { vertexIndex1, vertexIndex2, vertexIndex3 });
				}
			}
			System.out.println("nb triangles: " + faces.size() + ", nb vertices: " + vertices.size());
		} catch (java.io.FileNotFoundException e) {
			System.out.println("FileNotFoundException loading file " + filename + ", e=" + e);
			e.printStackTrace();
		} catch (java.io.IOException e) {
			System.out.println("IOException loading file " + filename + ", e=" + e);
			e.printStackTrace();
		}
	}
}
