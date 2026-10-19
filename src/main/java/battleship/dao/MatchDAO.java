package battleship.dao;

import battleship.config.DbConfig;
import java.sql.*;
import java.util.*;

public class MatchDAO {
    public record HistoryRow(long matchId, String opponent, String result, Timestamp finishedAt) {}

    public long create(int p1, int p2) throws SQLException {
        String sql = "INSERT INTO Matches(Player1Id,Player2Id) OUTPUT INSERTED.MatchId VALUES(?,?)";
        try (Connection c = DbConfig.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1,p1); ps.setInt(2,p2);
            try (ResultSet rs=ps.executeQuery()) { rs.next(); return rs.getLong(1); }
        }
    }

    public void cancel(long matchId) {
        try(Connection c=DbConfig.getConnection();
            PreparedStatement ps=c.prepareStatement(
                "UPDATE Matches SET Status='CANCELLED',FinishedAt=SYSDATETIME() WHERE MatchId=? AND Status <> 'FINISHED'")){
            ps.setLong(1,matchId);
            ps.executeUpdate();
        }catch(SQLException e){System.err.println("Failed to cancel match "+matchId+": "+e.getMessage());}
    }

    public void start(long matchId) throws SQLException {
        try (Connection c=DbConfig.getConnection(); PreparedStatement ps=c.prepareStatement(
                "UPDATE Matches SET Status='IN_GAME',StartedAt=SYSDATETIME() WHERE MatchId=?")) {
            ps.setLong(1,matchId); ps.executeUpdate();
        }
    }

    public void event(long matchId, Integer actorId, String type, Integer row, Integer col, String value) {
        try (Connection c=DbConfig.getConnection(); PreparedStatement ps=c.prepareStatement(
                "INSERT INTO MatchEvents(MatchId,ActorUserId,EventType,RowNo,ColNo,EventValue) VALUES(?,?,?,?,?,?)")) {
            ps.setLong(1,matchId);
            if(actorId==null) ps.setNull(2,Types.INTEGER); else ps.setInt(2,actorId);
            ps.setString(3,type);
            if(row==null) ps.setNull(4,Types.INTEGER); else ps.setInt(4,row);
            if(col==null) ps.setNull(5,Types.INTEGER); else ps.setInt(5,col);
            ps.setString(6,value);
            ps.executeUpdate();
        } catch (SQLException e) {System.err.println("Match event write failed: "+e.getMessage());}
    }

    public void finish(long matchId, int winner, int loser) throws SQLException {
        try (Connection c=DbConfig.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps=c.prepareStatement(
                    "UPDATE Matches SET WinnerId=?,Status='FINISHED',FinishedAt=SYSDATETIME() WHERE MatchId=?")) {
                ps.setInt(1,winner);ps.setLong(2,matchId);ps.executeUpdate();
            }
            try (PreparedStatement ps=c.prepareStatement("UPDATE Users SET Wins=Wins+1,Rating=Rating+30 WHERE UserId=?")) {
                ps.setInt(1,winner);ps.executeUpdate();
            }
            try (PreparedStatement ps=c.prepareStatement("UPDATE Users SET Losses=Losses+1,Rating=CASE WHEN Rating>=20 THEN Rating-20 ELSE 0 END WHERE UserId=?")) {
                ps.setInt(1,loser);ps.executeUpdate();
            }
            c.commit();
        } catch(SQLException e){throw e;}
    }

    public List<HistoryRow> history(int userId) throws SQLException {
        List<HistoryRow> out=new ArrayList<>();
        String sql = """
            SELECT TOP 20 m.MatchId,
                   CASE WHEN m.Player1Id=? THEN u2.Username ELSE u1.Username END Opponent,
                   CASE WHEN m.WinnerId=? THEN 'WIN' ELSE 'LOSS' END Result,
                   m.FinishedAt
            FROM Matches m
            JOIN Users u1 ON u1.UserId=m.Player1Id
            JOIN Users u2 ON u2.UserId=m.Player2Id
            WHERE (m.Player1Id=? OR m.Player2Id=?) AND m.Status='FINISHED'
            ORDER BY m.FinishedAt DESC
            """;
        try(Connection c=DbConfig.getConnection();PreparedStatement ps=c.prepareStatement(sql)){
            ps.setInt(1,userId);ps.setInt(2,userId);ps.setInt(3,userId);ps.setInt(4,userId);
            try(ResultSet rs=ps.executeQuery()){
                while(rs.next()) out.add(new HistoryRow(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getTimestamp(4)));
            }
        }
        return out;
    }
}
