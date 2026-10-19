package battleship.game;

import java.util.*;

public class Board {
    public enum Shot { MISS, HIT, SUNK, ALREADY }
    private final int size;
    private final List<Ship> ships = new ArrayList<>();
    private final boolean[][] fired;
    private String lastSunkShip="";

    public Board(int size){ this.size=size; this.fired=new boolean[size][size]; }

    public boolean canPlace(int r,int c,int len,boolean horizontal){
        for(int i=0;i<len;i++){
            int rr=r+(horizontal?0:i), cc=c+(horizontal?i:0);
            if(rr<0||rr>=size||cc<0||cc>=size) return false;
            for(Ship s:ships) if(s.contains(rr,cc)) return false;
        }
        return true;
    }

    public boolean place(String name,int len,int r,int c,boolean horizontal){
        if(!canPlace(r,c,len,horizontal)) return false;
        Ship s=new Ship(name,len);
        for(int i=0;i<len;i++) s.add(r+(horizontal?0:i),c+(horizontal?i:0));
        ships.add(s); return true;
    }

    public Shot attack(int r,int c){
        if(r<0||c<0||r>=size||c>=size) throw new IllegalArgumentException("OUT_OF_BOARD");
        if(fired[r][c]) return Shot.ALREADY;
        fired[r][c]=true;
        lastSunkShip="";
        for(Ship s:ships) if(s.hit(r,c)) {
            if(s.sunk()){lastSunkShip=s.name();return Shot.SUNK;}
            return Shot.HIT;
        }
        return Shot.MISS;
    }

    public boolean allSunk(){ return !ships.isEmpty() && ships.stream().allMatch(Ship::sunk); }
    public boolean hasShip(int r,int c){ return ships.stream().anyMatch(s->s.contains(r,c)); }
    public boolean fired(int r,int c){ return fired[r][c]; }
    public int shipCount(){ return ships.size(); }
    public long remainingShips(){ return ships.stream().filter(s->!s.sunk()).count(); }
    public String lastSunkShip(){ return lastSunkShip; }
}
