USE BattleShipDB;
GO
-- Tài khoản được tạo qua màn hình Đăng ký để mật khẩu có salt/hash đúng định dạng.
SELECT TOP 20 * FROM dbo.vw_Ranking ORDER BY Rating DESC, Wins DESC;
