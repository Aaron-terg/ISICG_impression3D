package fc.PrintingApplication.TP4;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;

import fc.Math.Vec2f;

public class Slice {

    public float z = 0;
    public ArrayList<Vec2f> edges = new ArrayList<>(); // graphe des arrêtes
    public ArrayList<Integer> islands = new ArrayList<>(); // graphe des arrêtes
    // TODO massCenter foreach island for gradient rasterization

    public Slice() {
    }

    public Slice(float z) {
        this.z = z;
    }

    // met à jour les ESD dans esds ou l'ajoute si point nouveau
    // ajoute l'arrête former dans edges
    public void makeEdge(ArrayList<EdgeSliceData> _edges) {
    	
    	if(_edges.isEmpty()) return;
    	islands.add(0);
    	EdgeSliceData edge = _edges.get(0);
		edges.add(edge.a);
		edges.add(edge.b);
		_edges.remove(0);
		
		Vec2f p = edges.get(1);
		Vec2f begin = edges.get(islands.get(0));
		for(int i = 0; i < _edges.size();) {
			Vec2f p1 = _edges.get(i).a;
			Vec2f p2 = _edges.get(i).b;

			if(p1.equals(p)) {
				_edges.remove(i);
				if(p2.equals(begin)) {
					if(_edges.size() != 0) {
						islands.add(edges.size());
						 edge = _edges.get(0);
						edges.add(edge.a);
						edges.add(edge.b);
						_edges.remove(0);
						p = edges.get(edges.size() - 1);
						begin = edges.get(edges.size() - 2);
					}
				}else {						
					edges.add(p2);
					p = edges.get(edges.size() - 1);
				}
				i = 0;

			}else if( p2.equals(p)) {
				_edges.remove(i);
				if(p1.equals(begin)) {

					if(_edges.size() != 0) {
						islands.add(edges.size());
						 edge = _edges.get(0);
						edges.add(edge.a);
						edges.add(edge.b);
						_edges.remove(0);
						p = edges.get(edges.size() - 1);
						begin = edges.get(edges.size() - 2);
					}
				}else {

					edges.add(p1);
					p = edges.get(edges.size() - 1);
				}
				i = 0;

			}else
				++i;

		}
		islands.add(edges.size());
		
    }

    public void remap(Vec2f offset, float pixSize) {
    	for(int i = 0; i < edges.size(); ++i) {
    		Vec2f v = edges.get(i);
    		//v = v.add(offset).mul(pixSize);
    	//	edges.get(i).x = (v.x + offset.x) * pixSize;
    	//	edges.get(i).y = (v.y + offset.y) * pixSize;
    		edges.get(i).x = (v.x / pixSize ) + offset.x;
    		edges.get(i).y = (v.y / pixSize) + offset.y;
    		
    	
    	}
    }
    // -------------------------------------------
    // Graphic
    // affiche les arrêtes en appliquant un décalage et une mise à l'échelle
    public void printEdge(Graphics2D ctx, Vec2f offset, float pixSize) {
        ctx.setColor(Color.RED);
        Vec2f e1, e2;
    	for(int j = 0; j < islands.size()-1; ++j) {

			
			for (int i =  islands.get(j); i < islands.get(j+1)-1; ++i) {
				e1 = edges.get(i);//.add(offset).mul(pixSize);
				e2 = edges.get((i+1));//.add(offset).mul(pixSize);
				ctx.draw(new Line2D.Float(e1.x, e1.y, e2.x, e2.y));
			}
			e1 = edges.get(islands.get(j + 1) - 1);//.add(offset).mul(pixSize);
			e2 = edges.get(islands.get(j));//.add(offset).mul(pixSize);

			ctx.draw(new Line2D.Float(e1.x, e1.y, e2.x, e2.y));
		}
        ctx.setColor(Color.WHITE);
    }

    // rempli les contours de la tranche
    public void fill(Graphics2D ctx) {

        SortedSet<Float> ys = new TreeSet();
        // Détemination des zones
        for (Vec2f v : edges) {
            ys.add(v.y);

        }
        // zones.sort(null);
        Float yss[] = new Float[ys.size()];
        ys.toArray(yss);
        ArrayList<FillingZone> zones = new ArrayList<>();
        for(int i = 0; i < ys.size() - 1; ++i) {
        	zones.add(new FillingZone(yss[i], yss[i+1]));
        }
   //     System.out.println("nb zones: " + zones.size());

        // remplissage première zone
        if(zones.isEmpty())return;
        FillingZone fz0 = zones.get(0);
        Vec2f a, b;
        for(int j = 0; j < islands.size() -1; ++j) {
        	
        	int islandSize =islands.get(j +1);
	        for (int i = islands.get(j); i < islandSize - 1; ++i) {
	        	a = edges.get(i);
	        	b = edges.get((i+1));
	            if (b.y == fz0.yMin || a.y == fz0.yMin) {
	                fz0.lines.add(new EdgeSliceData(a, b));
	            }
	        }
	        a = edges.get(islandSize -1);
        	b = edges.get(islands.get(j));
            if (b.y == fz0.yMin || a.y == fz0.yMin) {
                fz0.lines.add(new EdgeSliceData(a, b));
            }
        }
        fz0.fillLines(ctx);
        if (zones.size() < 2)
            return;
        FillingZone zs[] = new FillingZone[zones.size()];
        zones.toArray(zs);

        // remplissages des zones
        for (int i = 1; i < zones.size(); i++) {
            FillingZone fzPrev = zs[i - 1];
            FillingZone fz = zs[i];
            
            for(int j = 0; j < islands.size() -1; ++j) {      	
            	int islandSize =islands.get(j +1);
    	        for (int k = islands.get(j); k < islandSize - 1; ++k) {
    	        
    	        	EdgeSliceData edge = new EdgeSliceData(edges.get(k),edges.get(k+1));
	                if (fzPrev.lines.contains(edge)) {
	                    if (edge.a.y > fz.yMin || edge.b.y > fz.yMin) {
	                        fz.lines.add(edge);
	                    }
	
	                } else if (edge.a.y == fz.yMin || edge.b.y == fz.yMin && edge.a.y != edge.b.y) {
	                    fz.lines.add(edge);
	                }
    	        }
    	        
    	        EdgeSliceData edge = new EdgeSliceData(edges.get(islandSize -1 ),edges.get(islands.get(j)));
                if (fzPrev.lines.contains(edge)) {
                    if (edge.a.y > fz.yMin || edge.b.y > fz.yMin) {
                        fz.lines.add(edge);
                    }

                } else if (edge.a.y == fz.yMin || edge.b.y == fz.yMin && edge.a.y != edge.b.y) {
                    fz.lines.add(edge);
                }
            }
        

            // arrete plus grand que yMin zone courante
            // for (EdgeSliceData edge : zones.get(i - 1).lines) {
            // }

            // calcul arrete par arrete
            fz.fillLines(ctx);
        }

    }

    
}
