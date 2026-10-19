package battleship.server;

import battleship.dao.UserDAO;
import org.junit.jupiter.api.Test;
import java.net.Socket;
import static org.junit.jupiter.api.Assertions.*;

class MatchRoomTest {
    private static PlayerSession session(int id,String name){
        PlayerSession s=new PlayerSession(new Socket(),new BattleshipServer(0));
        s.user(new UserDAO.User(id,name,0,0,1000));
        return s;
    }

    @Test void turnDeadlineAndTimeoutCountersArePerPlayer(){
        PlayerSession a=session(1,"a"),b=session(2,"b");
        MatchRoom room=new MatchRoom(10,a,b);
        room.start();
        assertTrue(room.turnDeadline>System.currentTimeMillis());
        room.nextTurn(a);
        assertEquals(a.userId(),room.currentUserId);
        assertEquals(1,room.incrementMisses(a));
        assertEquals(2,room.incrementMisses(a));
        assertEquals(0,room.misses(b));
        assertEquals(1,room.incrementMisses(b));
    }
}
