package battleship.server;

import battleship.game.Board;
import java.util.concurrent.ThreadLocalRandom;

public class MatchRoom {
    public final long id;
    public final PlayerSession p1,p2;
    public Board b1,b2;
    public boolean r1,r2;
    public int currentUserId;
    public boolean started,finished;
    public long turnDeadline;
    public int misses1,misses2;
    public int misses(PlayerSession p){return p==p1?misses1:misses2;}
    public int incrementMisses(PlayerSession p){return p==p1?++misses1:++misses2;}
    public void nextTurn(PlayerSession p){currentUserId=p.userId();turnDeadline=System.currentTimeMillis()+15000;}

    public MatchRoom(long id,PlayerSession p1,PlayerSession p2){
        this.id=id;this.p1=p1;this.p2=p2;
    }
    public boolean contains(PlayerSession p){return p==p1||p==p2;}
    public PlayerSession other(PlayerSession p){return p==p1?p2:p1;}
    public Board board(PlayerSession p){return p==p1?b1:b2;}
    public Board enemyBoard(PlayerSession p){return p==p1?b2:b1;}
    public boolean ready(PlayerSession p){return p==p1?r1:r2;}
    public void setReady(PlayerSession p,Board b){
        if(p==p1){b1=b;r1=true;}else{b2=b;r2=true;}
    }
    public void start(){
        started=true;
        currentUserId=ThreadLocalRandom.current().nextBoolean()?p1.userId():p2.userId();
        turnDeadline=System.currentTimeMillis()+15000;
    }
}
