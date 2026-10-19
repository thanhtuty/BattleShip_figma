package battleship.dao;

import battleship.config.DbConfig;
import java.sql.*;
import java.util.*;

public class FriendDAO {
    public FriendDAO() {}
    private void ensure() throws SQLException {
        try(Connection c=DbConfig.getConnection();Statement s=c.createStatement()){
            s.execute("IF OBJECT_ID('dbo.Friendships','U') IS NULL CREATE TABLE dbo.Friendships (UserA INT NOT NULL REFERENCES Users(UserId), UserB INT NOT NULL REFERENCES Users(UserId), CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(), PRIMARY KEY(UserA,UserB))");
        }
    }
    public boolean add(int userId,String other) throws SQLException {
        ensure();
        try(Connection c=DbConfig.getConnection();PreparedStatement p=c.prepareStatement("SELECT UserId FROM Users WHERE Username=?")){
            p.setString(1,other);
            try(ResultSet r=p.executeQuery()){
                if(!r.next())return false;
                int id=r.getInt(1);
                if(id==userId)return false;
                int a=Math.min(id,userId),b=Math.max(id,userId);
                try(PreparedStatement insert=c.prepareStatement("IF NOT EXISTS(SELECT 1 FROM Friendships WHERE UserA=? AND UserB=?) INSERT INTO Friendships(UserA,UserB) VALUES(?,?)")){
                    insert.setInt(1,a);insert.setInt(2,b);insert.setInt(3,a);insert.setInt(4,b);insert.executeUpdate();
                }
                return true;
            }
        }
    }
    public List<String> list(int userId) throws SQLException {
        ensure();
        List<String> names=new ArrayList<>();
        String sql="SELECT u.Username FROM Friendships f JOIN Users u ON u.UserId=CASE WHEN f.UserA=? THEN f.UserB ELSE f.UserA END WHERE f.UserA=? OR f.UserB=? ORDER BY u.Username";
        try(Connection c=DbConfig.getConnection();PreparedStatement p=c.prepareStatement(sql)){
            p.setInt(1,userId);p.setInt(2,userId);p.setInt(3,userId);
            try(ResultSet r=p.executeQuery()){while(r.next())names.add(r.getString(1));}
        }
        return names;
    }
}
