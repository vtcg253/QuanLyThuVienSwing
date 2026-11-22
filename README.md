# Quản lý thư viện — Java Swing MVC

Ứng dụng desktop Java Swing tách biệt với dự án Spring Boot trong thư mục khác. Mã nguồn được chia thành `model`, `view` và `controller`; JDBC kết nối Oracle Database.

## Cấu trúc mã nguồn

Source được đặt trực tiếp dưới `src` theo mô hình thư mục đã chọn:

```text
src/
├── Main.java
├── model/
├── view/
├── controller/
├── dao/
└── database/
```

`src` được cấu hình làm source root trong Maven. Các model hiện tại chứa dữ liệu dùng chung giữa các tầng; các DAO trong `dao/` phụ trách JDBC, còn `AppController` điều phối các màn hình nghiệp vụ.

## Các lớp DAO

| File | Trách nhiệm |
| --- | --- |
| `DaoTaiKhoan.java` | Đăng nhập BCrypt và lưu tài khoản trên connection của nhân viên |
| `DaoSach.java` | Danh sách, biểu mẫu, kiểm tra và quản lý sách |
| `DaoDocGia.java` | Danh sách, biểu mẫu và quản lý độc giả |
| `DaoNhanVien.java` | Quản lý nhân viên; lưu hồ sơ và tài khoản trong cùng transaction |
| `DaoDanhMuc.java` | Thể loại, tác giả, nhà xuất bản |
| `DaoPhieuMuon.java` | Danh sách phiếu, lập phiếu nhiều sách, trả sách và báo mất |
| `DaoThongKe.java` | Số liệu tổng quan và biểu đồ |
| `DaoHoSo.java` | Lớp cha dùng chung thao tác CRUD hồ sơ và transaction |
| `HoTroDao.java` | Các hàm JDBC nội bộ: query, tham số, sinh mã và chuyển kiểu |

`AppController` gọi trực tiếp các DAO theo chức năng; `KetNoiOracle` tiếp tục mở kết nối Oracle. `DaoPhieuMuon` giữ toàn bộ transaction lập phiếu và chi tiết trên cùng connection. `DaoNhanVien` truyền connection của transaction vào `DaoTaiKhoan` khi lưu tài khoản.

## Yêu cầu

- JDK 17 trở lên
- Maven 3.8 trở lên
- Oracle Database đã được tạo schema ứng dụng, gồm các bảng `THELOAI`, `TACGIA`, `NHAXUATBAN`, `DOCGIA`, `NHANVIEN`, `TAIKHOAN`, `SACH`, `PHIEUMUON`, `CT_PHIEUMUON` và `PHIEUPHAT`
- Tài khoản Oracle có quyền đọc/ghi các bảng trên

## Chuẩn bị Oracle

Schema Oracle được quản lý ngoài repository này. Trước khi chạy ứng dụng, bảo đảm bảng `SACH` có các cột `SO_TAP` và `GIABAN`, bảng `CT_PHIEUMUON` có `SOLUONGMAT`, `TIENPHAT` và `TRANGTHAI`, và bảng `PHIEUPHAT` đã được tạo.

Trigger Oracle tự cập nhật tồn kho khi mượn/trả; DAO khóa và kiểm tra tồn kho rồi để trigger cập nhật đúng một lần. Không thêm một lớp cập nhật tồn kho khác.

## Cấu hình và chạy

Không lưu mật khẩu trong source code. Đặt thông tin Oracle trong môi trường chạy:

```bash
export DB_URL='jdbc:oracle:thin:@localhost:1521/FREEPDB1'
export DB_USERNAME='thuthu'
printf 'Oracle password: '
read -s DB_PASSWORD
printf '\n'
export DB_PASSWORD
mvn clean compile exec:java
```

Mở thư mục gốc dự án `QuanLyThuVien` trong VS Code. Cấu hình workspace trong `.vscode/settings.json` cho Code Runner: bấm **Run Code** trên file Java bất kỳ sẽ hỏi mật khẩu Oracle trong terminal rồi khởi chạy ứng dụng bằng Maven. Không chạy trực tiếp `javac <file>.java`, vì `javac` không nạp các thư viện BCrypt và Oracle JDBC được khai báo trong Maven. Cũng có thể chọn **Run QuanLyThuVien (Maven)** trong tab Run and Debug sau khi đã đặt `DB_PASSWORD` trong môi trường VS Code.

Thông tin kết nối tương ứng Oracle SQL Developer: hostname `localhost`, port `1521`, service name `FREEPDB1`, username `thuthu`. Lệnh trên đọc mật khẩu mà không hiển thị khi gõ và ứng dụng không lưu mật khẩu vào source code. Đừng thêm mật khẩu vào giá trị mặc định trong `KetNoiOracle.java`. Tài khoản đăng nhập giao diện là tài khoản ứng dụng trong bảng `TAIKHOAN` (ví dụ `admin` nếu đã tạo dữ liệu mẫu); đây là tài khoản riêng với tài khoản kết nối Oracle.

## Chức năng

- Đăng nhập bằng `TAIKHOAN` và BCrypt, phân quyền màn hình nhân viên cho `ADMIN`.
- Quản lý sách, độc giả, tác giả, thể loại, nhà xuất bản và hồ sơ/tài khoản nhân viên.
- Quản lý số tập trên từng đầu sách; mỗi tập có mã, tồn kho và lịch sử mượn/trả riêng.
- Tìm kiếm danh sách, thêm/sửa/xóa và kiểm tra ràng buộc lịch sử.
- Lập phiếu với nhiều sách, kiểm tra tồn kho; trả sách và báo mất.
- Cập nhật tồn kho bằng trigger Oracle trong transaction mượn/trả.
- Báo mất sách lập một phiếu phạt riêng, lưu đơn giá tại thời điểm ghi nhận, nhân viên xử lý và trạng thái thanh toán.
- Mục **Phiếu phạt** cho phép tra cứu, xem chi tiết và xác nhận đã thu tiền. Độc giả còn phiếu phạt chưa thanh toán không thể mượn thêm.

Trường `SO_TAP` để trống với sách lẻ; các tập trong một bộ được lưu thành từng mã sách riêng. Giá sách được lưu trong `GIABAN` và dùng để tính tiền phạt khi ghi nhận mất sách.

## Giao diện

- Các màn hình dùng chung nền kem, thanh điều hướng xanh, thẻ bo góc và nút thao tác trên từng dòng.
- Thêm độc giả, tác giả, thể loại và nhà xuất bản bằng popup. Lưu thành công đóng popup và tải lại danh sách; lỗi giữ nguyên dữ liệu để sửa và thử lại. Các form sửa và form sách/thủ thư dùng vùng nội dung chính.
- Form sách chọn thể loại, tác giả và nhà xuất bản theo tên; có nút thêm nhanh danh mục ngay trên form.
- Trang mượn/trả gom các sách theo phiếu, hiển thị thống kê và lọc trạng thái. Chi tiết mở trong popup có thể đổi kích thước, hỗ trợ trả/báo mất theo từng sách. Sau khi lưu, popup và danh sách cập nhật, giữ bộ lọc và trang hiện tại.
- Form lập phiếu chọn độc giả, sách và số lượng; hỗ trợ nhiều dòng sách, kiểm tra ngày và tồn kho trước khi gửi.

Các danh sách có phân trang mặc định 10 dòng (hoặc 10 phiếu mượn) mỗi trang, có thể chọn 5/10/20 và chuyển trang bằng số trang hoặc nút Trước/Sau.

Ngày sinh của độc giả và thủ thư nhập dạng `dd/MM/yyyy` có sẵn dấu `/`, chỉ cần điền số. Biểu mẫu kiểm tra ngày hợp lệ trước khi lưu; có thể để trống ngày sinh.

## Kiểm tra transaction DAO sau khi tách lớp

Chạy kiểm tra với JDBC mô phỏng, không kết nối hoặc ghi vào Oracle:

```bash
mvn -o compile
javac --release 17 -cp target/classes -d target/dao-check tools/KiemTraGiaoDichDao.java
java -cp "target/dao-check:target/classes:$HOME/.m2/repository/org/mindrot/jbcrypt/0.4/jbcrypt-0.4.jar" KiemTraGiaoDichDao
```

Chín tình huống kiểm tra: commit phiếu và chi tiết, rollback khi thiếu sách, từ chối trả lặp, báo mất một phần/toàn bộ/vượt số lượng, lưu nhân viên cùng tài khoản, rollback hồ sơ khi tài khoản không hợp lệ và chặn xóa chính nhân viên đang đăng nhập. Kiểm tra này không thay thế kiểm thử tích hợp Oracle/trigger.
