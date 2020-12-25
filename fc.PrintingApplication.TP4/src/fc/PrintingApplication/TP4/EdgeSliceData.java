package fc.PrintingApplication.TP4;

import fc.Math.Vec2f;

public class EdgeSliceData {
    public Vec2f a, b;
 

    public EdgeSliceData(){
    	a =null;
    	b =null;
    };

    public EdgeSliceData(Vec2f hitpoint, Vec2f next){
        this.a = hitpoint;
        this.b = next;
    }

    public EdgeSliceData(EdgeSliceData other){
        if(other != null && other.a != null){
            this.a = other.a;
            this.b = other.b; 
        }
    }

   
    @Override
    public boolean equals(Object o){
        if(!(o instanceof EdgeSliceData))
            return false;
        EdgeSliceData esd = (EdgeSliceData) o;
        Vec2f a = esd.a;
        Vec2f b = esd.b;
        Vec2f c = a;
        Vec2f d = b;

        return (a.equals(c) && b.equals(d)) || (a.equals(d) && c.equals(b));
    }
}
