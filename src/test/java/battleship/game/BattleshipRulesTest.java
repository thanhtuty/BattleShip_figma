package battleship.game;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class BattleshipRulesTest {
    private static List<FleetCodec.Placement> fleet(){
        return List.of(
            new FleetCodec.Placement("Carrier",5,0,0,true),
            new FleetCodec.Placement("Battleship",4,1,0,true),
            new FleetCodec.Placement("Cruiser",3,2,0,true),
            new FleetCodec.Placement("Destroyer",2,3,0,true),
            new FleetCodec.Placement("Patrol",2,4,0,true));
    }
    @Test void validFleetHasFiveLiveShips(){
        Board b=FleetCodec.toBoard(FleetCodec.encode(fleet()));
        assertEquals(5,b.shipCount());
        assertEquals(5,b.remainingShips());
    }
    @Test void eachSunkShipDecrementsCount(){
        Board b=FleetCodec.toBoard(FleetCodec.encode(fleet()));
        assertEquals(Board.Shot.HIT,b.attack(3,0));
        assertEquals(5,b.remainingShips());
        assertEquals(Board.Shot.SUNK,b.attack(3,1));
        assertEquals("Destroyer",b.lastSunkShip());
        assertEquals(4,b.remainingShips());
        assertEquals(Board.Shot.ALREADY,b.attack(3,1));
    }
    @Test void duplicatedShipIdentityIsInvalid(){
        var copy=new ArrayList<>(fleet());
        copy.set(4,new FleetCodec.Placement("Destroyer",2,4,0,true));
        assertThrows(IllegalArgumentException.class,()->FleetCodec.toBoard(FleetCodec.encode(copy)));
    }
    @Test void boundaryAndOverlapAreRejected(){
        Board b=new Board(10);
        assertFalse(b.place("Carrier",5,0,8,true));
        assertTrue(b.place("Carrier",5,0,0,true));
        assertFalse(b.place("Patrol",2,0,3,false));
        assertThrows(IllegalArgumentException.class,()->b.attack(10,0));
    }
    @Test void allSunkOnlyAfterEveryShipIsSunk(){
        Board b=FleetCodec.toBoard(FleetCodec.encode(fleet()));
        for(var p:fleet()){
            for(int i=0;i<p.len();i++){
                b.attack(p.row(),p.col()+i);
            }
        }
        assertEquals(0,b.remainingShips());
        assertTrue(b.allSunk());
    }
}
