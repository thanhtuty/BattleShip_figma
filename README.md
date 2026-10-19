# BATTLESHIP – TCP + SQL SERVER + FIGMA UI

Giao diện Java Swing được dựng lại theo file Figma:
https://www.figma.com/design/4R48TgNdyG1eQt7p0eIeI0/Battleship---Complete-Desktop-Game-UI

## Kiến trúc
```text
Client 1 (Java Swing)
        \
         TCP :5050 -> BattleshipServer -> SQL Server BattleShipDB
        /
Client 2 (Java Swing)
```

## 12 màn hình UI
1. Chào mừng
2. Đăng nhập
3. Đăng ký
4. Sảnh chính
5. Tìm trận và thách đấu
6. Lời mời và bạn bè
7. Phòng chờ
8. Đặt tàu
9. Chiến đấu
10. Kết quả
11. Bảng xếp hạng
12. Lịch sử và cài đặt

Mỗi màn hình dùng canvas 1200x760, tông navy/cyan/orange giống bản Figma.

## Chức năng
- Đăng ký tài khoản vào SQL Server
- Đăng nhập bằng salt + SHA-256
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
- Lịch sử trận đấu
- Xử lý đối thủ thoát kết nối

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
NetBeans main class:

`battleship.server.BattleshipServer`

Terminal:

```bash
mvn clean compile
mvn -q exec:java -Dexec.mainClass=battleship.server.BattleshipServer
```

Mặc định port TCP là `5050`.

## 4. Chạy Client
NetBeans main class:

`battleship.client.ClientApp`

Terminal:

```bash
mvn -q exec:java -Dexec.mainClass=battleship.client.ClientApp
```

Client mặc định kết nối `127.0.0.1:5050`.

Muốn đổi host/port mà không sửa UI:

```bash
mvn -q exec:java -Dexec.mainClass=battleship.client.ClientApp -Dbattleship.host=192.168.1.10 -Dbattleship.port=5050
```

Hoặc dùng biến môi trường:

```text
BATTLESHIP_HOST
BATTLESHIP_PORT
```

## 5. Test 2 Client
1. Chạy Server
2. Mở Client A và Client B
3. Đăng ký hai tài khoản
4. Cả hai đăng nhập
5. Vào Tìm đối thủ hoặc Thách đấu
6. Người nhận Accept
7. Hai bên bố trí tàu và Ready
8. Server phát GAME_START
9. Bắn theo lượt cho tới GAME_OVER
10. Kiểm tra Ranking và History

## Ghi chú
- Server là nguồn quyết định trạng thái trận đấu.
- Client không tự quyết định HIT/MISS/SUNK.
- Không commit `config/db.properties` nếu chứa mật khẩu thật.

## Chức năng mở rộng TCP (cập nhật 2026-10)

- **LOGOUT**: phiên online đóng trên Server, Client quay về đăng nhập
- **LEAVE**: rời phòng chờ sẽ hủy trận; rời trận đang đấu sẽ bị xử thua
- **Tìm đối thủ**: chọn username trực tiếp trong danh sách online từ Server
- **Bố trí thủ công**: chọn tàu, chọn ô, chọn hướng ngang/dọc; kiểm tra trùng và vượt biên; nút xếp lại hoặc bố trí ngẫu nhiên
- **TIMER**: Server phát giây còn lại, mỗi lượt tối đa 15 giây
- **TURN_TIMEOUT**: hết giờ chuyển lượt; từ lần bỏ thứ tư xử thua
- **FLEET_STATE**: Server gửi số tàu còn lại của cả hai bên
- **ATTACK_RESULT**: gồm thông tin tàu bị chìm; UI cập nhật nhật ký bắn
- **SoundFX**: âm thanh tạo trong Java cho bắn, trúng, trượt, chìm tàu, lời mời, thắng/thua; có nút bật/tắt tại Cài đặt

**Chạy lại cả Server và Client sau khi cập nhật** vì protocol đã thay đổi.

## Kiểm tra chất lượng và trạng thái đồng bộ

GitHub Actions `Java Maven Build` chạy `mvn clean verify` trên Java 17 cho mỗi commit.
Các test JUnit ở `src/test/java` kiểm tra kiểm tra đội tàu hợp lệ, chồng biên, số tàu chìm và bộ đếm lượt trễ.
Xem kết quả: https://github.com/thanhtuty/BattleShip_figma/actions

- `MATCH_STATE`: Server gửi matchId, lượt hiện tại, thời gian còn lại, username/số tàu/số lượt bỏ của mỗi phía sau mọi lần đổi lượt.
- `TURN`: tên người chơi và số giây còn lại trên Server (không phải epoch trên máy Client).
- `FLEET_STATE`: số tàu sống sau phát bắn.
- `GAME_OVER`: server đã commit SQL trước khi thông báo thắng/thua, kể cả thua do rời trận hoặc quá giờ.
- `THÁCH ĐẤU NGẪU NHIÊN`: mời một tài khoản đang online ngẫu nhiên, **không phải** hàng đợi matchmaking.
- `Friendships` được khai báo trong `database/01_BattleShipDB.sql`. Chạy lại script (có kiểm tra tồn tại bảng) nếu database cũ chưa có bảng này.

### Giới hạn kiểm thử
GitHub Actions chỉ kiểm tra Java/Maven và unit test, chưa chạy hai Client thực cùng SQL Server.
Để kiểm tra tích hợp: chạy SQL Server, một Server TCP, hai Client, rồi thử Login, Challenge, Ready, ATTACK, timeout, LEAVE, Logout và kiểm tra lịch sử cùng bảng xếp hạng.
