package battleship.dao;

import battleship.config.DbConfig;
import java.sql.*;
import java.util.*;

public class UserDAO {
    public record User(int id, String username, int wins, int losses, int rating) {}
    public record RankRow(String username, int wins, int losses, int rating, double winRate) {}

    public User register(String username, String email, String password) throws SQLException {
        String salt = PasswordUtil.newSalt();
        String hash = PasswordUtil.hash(password, salt);
        String sql = "INSERT INTO Users(Username,Email,PasswordHash,PasswordSalt) OUTPUT INSERTED.UserId VALUES(?,?,?,?)";
        try (Connection c = DbConfig.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            if (email == null || email.isBlank()) ps.setNull(2, Types.NVARCHAR); else ps.setString(2, email);
            ps.setString(3, hash);
            ps.setString(4, salt);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new User(rs.getInt(1), username, 0, 0, 1000);
            }
        }
        return null;
    }

    public User login(String username, String password) throws SQLException {
        String sql = "SELECT UserId,Username,PasswordHash,PasswordSalt,Wins,Losses,Rating FROM Users WHERE Username=?";
        try (Connection c = DbConfig.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                String expected = rs.getString("PasswordHash");
                String actual = PasswordUtil.hash(password, rs.getString("PasswordSalt"));
                if (!expected.equalsIgnoreCase(actual)) return null;
                return new User(rs.getInt("UserId"), rs.getString("Username"),
                        rs.getInt("Wins"), rs.getInt("Losses"), rs.getInt("Rating"));
            }
        }
    }


    public User profile(int userId) throws SQLException {
        try(Connection c=DbConfig.getConnection();
            PreparedStatement ps=c.prepareStatement("SELECT UserId,Username,Wins,Losses,Rating FROM Users WHERE UserId=?")){
            ps.setInt(1,userId);
            try(ResultSet rs=ps.executeQuery()){
                if(rs.next())return new User(rs.getInt(1),rs.getString(2),rs.getInt(3),rs.getInt(4),rs.getInt(5));
            }
        }
        return null;
    }

    public List<RankRow> ranking(int limit) throws SQLException {
        List<RankRow> out = new ArrayList<>();
        String sql = "SELECT TOP (?) Username,Wins,Losses,Rating,WinRate FROM vw_Ranking ORDER BY Rating DESC,Wins DESC";
        try (Connection c = DbConfig.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new RankRow(rs.getString(1),rs.getInt(2),rs.getInt(3),rs.getInt(4),rs.getDouble(5)));
            }
        }
        return out;
    }
}
