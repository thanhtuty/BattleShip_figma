package battleship.game;

import java.util.*;

public final class FleetCodec {
    private FleetCodec(){}

    public record Placement(String name,int len,int row,int col,boolean horizontal){}

    public static String encode(List<Placement> list){
        StringJoiner j=new StringJoiner(";");
        for(Placement p:list) j.add(p.name()+","+p.len()+","+p.row()+","+p.col()+","+(p.horizontal()?"H":"V"));
        return j.toString();
    }

    public static List<Placement> decode(String s){
        List<Placement> out=new ArrayList<>();
        if(s==null||s.isBlank()) return out;
        for(String item:s.split(";")){
            String[] p=item.split(",");
            if(p.length!=5) throw new IllegalArgumentException("BAD_FLEET");
            out.add(new Placement(p[0],Integer.parseInt(p[1]),Integer.parseInt(p[2]),Integer.parseInt(p[3]),p[4].equals("H")));
        }
        return out;
    }

    public static Board toBoard(String s){
        List<Placement> ps=decode(s);
        if(ps.size()!=5) throw new IllegalArgumentException("FLEET_MUST_HAVE_5_SHIPS");
        int[] lens=ps.stream().mapToInt(Placement::len).sorted().toArray();
        if(!Arrays.equals(lens,new int[]{2,2,3,4,5})) throw new IllegalArgumentException("INVALID_SHIP_LENGTHS");
        Map<String,Integer> required=Map.of("Carrier",5,"Battleship",4,"Cruiser",3,"Destroyer",2,"Patrol",2);
        Set<String> names=new HashSet<>();
        for(Placement p:ps){
            if(!required.containsKey(p.name())||required.get(p.name())!=p.len()||!names.add(p.name()))
                throw new IllegalArgumentException("INVALID_SHIP_IDENTITY");
        }
        Board b=new Board(10);
        for(Placement p:ps) if(!b.place(p.name(),p.len(),p.row(),p.col(),p.horizontal()))
            throw new IllegalArgumentException("INVALID_PLACEMENT");
        return b;
    }
}
