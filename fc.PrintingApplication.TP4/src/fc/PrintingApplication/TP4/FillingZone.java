package fc.PrintingApplication.TP4;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;

import fc.Math.Vec2f;

//classe des zones former par le yMin-yMax
public class FillingZone implements Comparable {
  public float yMin, yMax;
  public ArrayList<EdgeSliceData> lines;
  public Color color;
  public static int nbZone = 0;
  public FillingZone(float yMin, float yMax) {
      this.yMax = yMax;
      this.yMin = yMin;
      lines = new ArrayList<>();
  
      if((++nbZone & 1) == 1) color = Color.BLUE;
      else color = Color.WHITE;
      
  }

  @Override
  public int compareTo(Object o) {
      FillingZone other = (FillingZone) o;
      return this.yMin > other.yMin ? 1
              : this.yMin < other.yMin ? -1 : this.yMax > other.yMax ? 1 : this.yMax < other.yMax ? -1 : 0;
  }

  @Override
  public boolean equals(Object o) {
      if (!(o instanceof FillingZone))
          return false;
      FillingZone fz = (FillingZone) o;
      return this.yMin == fz.yMin && this.yMax == fz.yMax;
  }

  // intersection thales droite-segment
  private float intersect(float height, Vec2f a, Vec2f b) {

      Vec2f edge = b.sub(a);

      float h = Math.abs(height - b.y);

      float ratioy = h / edge.y;
      float distIntersection = edge.length() * ratioy;
      return b.x + (edge.x / edge.length()) * (-distIntersection);
  }
  
  //scanline filling
  public void fillLines(Graphics2D ctx) {
      SortedSet<Float> in = new TreeSet<>();
      float u =0f, v = 0f, ydist = Math.abs(yMax - yMin);
      for (float y = yMin; y <= yMax; y += 0.05f) {
          for (EdgeSliceData e : lines) {
              Vec2f a = e.a, b = e.b;
              if (a.y == b.y) {
            	  in.add(a.x);
            	  in.add(b.x);
                  ctx.draw(new Line2D.Float(a.x, y, b.x, y));
                  continue;
              }
              if (a.y > b.y) {
                  a = e.b;
                  b = e.a;
              }

              if (b.y < y || a.y > y)
                  continue;

              in.add(intersect(y, a, b));
          }
          
          Float intersections[] = new Float[in.size()];
          in.toArray(intersections);
          for (int i = 0; i < intersections.length - 1; i += 2) {
        		  ctx.draw(new Line2D.Float(intersections[i], y, intersections[i+1], y));
          }

          in.clear();
      }
  }
}