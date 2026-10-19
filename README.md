# BATTLESHIP – BẢN HOÀN CHỈNH TCP + SQL SERVER

## Kiến trúc
```text
Client 1 (Java Swing)
        \
         TCP :5050 -> BattleshipServer -> SQL Server BattleShipDB
        /
Client 2 (Java Swing)
```

## Chức năng đã có
- Đăng ký tài khoản vào SQL Server
- Đăng nhập bằng mật khẩu salt + SHA-256
- Chặn đăng nhập trùng
- Danh sách người chơi online
- Gửi lời mời thách đấu
- Chấp nhận / từ chối
- Tạo Match trong DB
- Phòng chờ
- Đặt 5 tàu trên bàn 10x10
- Server kiểm tra fleet hợp lệ
- READY hai phía
- Server chọn người đi trước
- ATTACK theo lượt
- MISS / HIT / SUNK
- HIT/SUNK giữ lượt, MISS đổi lượt
- GAME_OVER
- Cập nhật Wins / Losses / Rating
- Lưu MatchEvents
- Bảng xếp hạng
- Lịch sử 20 trận
- Xử lý đối thủ thoát kết nối
- UI Java Swing theo tông Figma Battleship

## 1. Tạo database
Mở SQL Server Management Studio và chạy:
`database/01_BattleShipDB.sql`

## 2. Cấu hình DB
Copy:
`config/db.properties.example`
thành:
`config/db.properties`

Sửa:
```properties
db.url=jdbc:sqlserver://localhost:1433;databaseName=BattleShipDB;encrypt=true;trustServerCertificate=true
db.user=sa
db.password=MAT_KHAU_CUA_BAN
```

## 3. Chạy Server
NetBeans:
- Main class: `battleship.server.BattleshipServer`

Terminal:
```bash
mvn clean compile
mvn -q exec:java -Dexec.mainClass=battleship.server.BattleshipServer
```

Mặc định TCP port: `5050`.

## 4. Chạy Client
Mở 2 client:
- Main class: `battleship.client.ClientApp`

Terminal:
```bash
mvn -q exec:java -Dexec.mainClass=battleship.client.ClientApp
```

Mở lệnh trên ở hai terminal để có Client 1 và Client 2.

## 5. Test đầy đủ
1. Client A kết nối 127.0.0.1:5050 và đăng ký A
2. Client B đăng ký B
3. Cả hai đăng nhập
4. A chọn B trong Online -> Thách đấu
5. B Accept
6. Hai bên vào Bố trí -> Đặt ngẫu nhiên -> Ready
7. Server gửi GAME_START và người đi trước
8. Bắn trên bảng đối thủ
9. Khi tất cả tàu chìm, Server cập nhật DB
10. Vào Ranking / History để kiểm tra

## Ghi chú
- Server là nguồn quyết định trạng thái trận đấu.
- Client không tự quyết định HIT/MISS/SUNK.
- Không commit `config/db.properties` nếu chứa mật khẩu thật.
