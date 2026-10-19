package battleship.game;

import java.util.*;

public class Ship {
    private final String name;
    private final int length;
    private final Set<Integer> cells = new HashSet<>();
    private final Set<Integer> hits = new HashSet<>();

    public Ship(String name, int length) { this.name=name; this.length=length; }
    public String name(){ return name; }
    public int length(){ return length; }
    public void add(int r,int c){ cells.add(r*100+c); }
    public boolean contains(int r,int c){ return cells.contains(r*100+c); }
    public boolean hit(int r,int c){ if(!contains(r,c)) return false; hits.add(r*100+c); return true; }
    public boolean sunk(){ return hits.size()>=length; }
}
