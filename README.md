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
| `AccountDAO.java` | Đăng nhập BCrypt và lưu tài khoản trên connection của nhân viên |
| `BookDAO.java` | Danh sách, biểu mẫu, kiểm tra và quản lý sách |
| `ReaderDAO.java` | Danh sách, biểu mẫu và quản lý độc giả |
| `EmployeeDAO.java` | Quản lý nhân viên; lưu hồ sơ và tài khoản trong cùng transaction |
| `CatalogDAO.java` | Thể loại, tác giả, nhà xuất bản |
| `LoanDAO.java` | Danh sách phiếu, lập phiếu nhiều sách, trả sách và báo mất |
| `StatisticsDAO.java` | Số liệu tổng quan và biểu đồ |
| `RecordDAO.java` | Lớp cha dùng chung thao tác CRUD hồ sơ và transaction |
| `DaoSupport.java` | Các hàm JDBC nội bộ: query, tham số, sinh mã và chuyển kiểu |

`AppController` gọi trực tiếp các DAO theo chức năng; `DBConnection` tiếp tục mở kết nối Oracle. `LoanDAO` giữ toàn bộ transaction lập phiếu và chi tiết trên cùng connection. `EmployeeDAO` truyền connection của transaction vào `AccountDAO` khi lưu tài khoản.

## Yêu cầu

- JDK 17 trở lên
- Maven 3.8 trở lên
- Oracle Database có các bảng `THELOAI`, `TACGIA`, `NHAXUATBAN`, `DOCGIA`, `NHANVIEN`, `TAIKHOAN`, `SACH`, `PHIEUMUON`, `CT_PHIEUMUON`
- Tài khoản Oracle có quyền đọc/ghi các bảng trên

## Chuẩn bị Oracle

Chạy file SQL tổng hợp [Quan Ly Thu Vien.sql](../../oracle/Quan%20Ly%20Thu%20Vien.sql) bằng Oracle SQL Developer. File này tạo các bảng cần thiết và bổ sung `SOLUONGMAT`, `TRANGTHAI` cùng các ràng buộc trong `CT_PHIEUMUON`.

Trigger trong SQL tự cập nhật tồn kho khi mượn/trả; DAO khóa và kiểm tra tồn kho rồi để trigger cập nhật đúng một lần. Không thêm một lớp cập nhật tồn kho khác. Script có dữ liệu mẫu, vì vậy không chạy lại trên schema đã có các mã mẫu.

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

Thông tin kết nối tương ứng Oracle SQL Developer: hostname `localhost`, port `1521`, service name `FREEPDB1`, username `thuthu`. Lệnh trên đọc mật khẩu mà không hiển thị khi gõ và ứng dụng không lưu mật khẩu vào source code. Đừng thêm mật khẩu vào giá trị mặc định trong `DBConnection.java`. Tài khoản đăng nhập giao diện là tài khoản ứng dụng trong bảng `TAIKHOAN` (ví dụ `admin` nếu đã tạo dữ liệu mẫu); đây là tài khoản riêng với tài khoản kết nối Oracle.

## Chức năng

- Đăng nhập bằng `TAIKHOAN` và BCrypt, phân quyền màn hình nhân viên cho `ADMIN`.
- Quản lý sách, độc giả, tác giả, thể loại, nhà xuất bản và hồ sơ/tài khoản nhân viên.
- Tìm kiếm danh sách, thêm/sửa/xóa và kiểm tra ràng buộc lịch sử.
- Lập phiếu với nhiều sách, kiểm tra tồn kho; trả sách và báo mất.
- Cập nhật tồn kho bằng trigger Oracle trong transaction mượn/trả.

## Giao diện

- Các màn hình dùng chung nền kem, thanh điều hướng xanh, thẻ bo góc và nút thao tác trên từng dòng.
- Thêm độc giả, tác giả, thể loại và nhà xuất bản bằng popup. Lưu thành công đóng popup và tải lại danh sách; lỗi giữ nguyên dữ liệu để sửa và thử lại. Các form sửa và form sách/thủ thư dùng vùng nội dung chính.
- Form sách chọn thể loại, tác giả và nhà xuất bản theo tên; có nút thêm nhanh danh mục ngay trên form.
- Trang mượn/trả gom các sách theo phiếu, hiển thị thống kê và lọc trạng thái. Chi tiết mở trong popup có thể đổi kích thước, hỗ trợ trả/báo mất theo từng sách. Sau khi lưu, popup và danh sách cập nhật, giữ bộ lọc và trang hiện tại.
- Form lập phiếu chọn độc giả, sách và số lượng; hỗ trợ nhiều dòng sách, kiểm tra ngày và tồn kho trước khi gửi.

Các danh sách có phân trang mặc định 10 dòng (hoặc 10 phiếu mượn) mỗi trang, có thể chọn 5/10/20 và chuyển trang bằng số trang hoặc nút Trước/Sau.

Ngày sinh của độc giả và thủ thư nhập dạng `dd/MM/yyyy` có sẵn dấu `/`, chỉ cần điền số. Biểu mẫu kiểm tra ngày hợp lệ trước khi lưu; có thể để trống ngày sinh.

## Dữ liệu demo bổ sung

File [sql/demo-data.sql](sql/demo-data.sql) thêm dữ liệu vào schema đã có, dùng mã chuẩn `TL`, `TG`, `NXB`, `DG`, `NV`, `S`, `PM`:

- 6 thể loại, 16 tác giả, 4 nhà xuất bản.
- 30 sách, 16 độc giả có ngày sinh, số điện thoại và địa chỉ minh họa.
- 2 thủ thư và 2 tài khoản `thuthu_lan`, `thuthu_minh`, mật khẩu `123456`, quyền `THU_THU`.
- 30 phiếu / 60 dòng sách, gồm đang mượn, quá hạn, đã trả, mất toàn bộ, mất một phần và phiếu đã trả một số sách.

Chạy file bằng **Run Script (F5)** trong Oracle SQL Developer, trên kết nối/schema ứng dụng. Hai trigger mượn/trả phải đang bật và hợp lệ. Script dùng chúng để cập nhật tồn kho, không cập nhật kho thêm lần nữa.

Các mã đã tồn tại sẽ được bỏ qua khi chạy lại; không xóa dữ liệu đang có. Mã mẫu đã được đối chiếu với schema hiện tại: sách S08–S37, thể loại TL09–TL14, tác giả TG16–TG31, NXB05–NXB08, độc giả DG05–DG20, nhân viên NV03–NV04, phiếu PM09–PM38. Khi nạp vào schema khác, cần kiểm tra mã để tránh bỏ qua nhầm bản ghi. Bảng đổi mã cũ/mới nằm tại `bao-cao/bang-doi-ma-demo.csv`; tiện ích `tools/NormalizeDemoCodes.java` chuyển mã cũ trong một transaction và kiểm tra dữ liệu trước khi commit. Script cũng bỏ hậu tố `(Demo)` trong tên thuộc bộ mẫu đã nạp trước đây và đổi hai tên đăng nhập mẫu cũ thành `thuthu_lan`, `thuthu_minh`. Các ngày mượn/hạn trả được tính theo ngày Oracle tại lần tạo đầu tiên, không tự dịch sang ngày mới khi chạy lại. Đây là thông tin minh họa để thử giao diện, phân trang và nghiệp vụ; tên, số điện thoại, địa chỉ và thông tin xuất bản không dùng làm dữ liệu thực.

Trong bộ mẫu có 14 phiếu quá hạn (4 phiếu ban đầu và 10 phiếu bổ sung, trễ hạn 1–10 ngày tại lúc tạo).

## Kiểm tra transaction DAO sau khi tách lớp

Chạy kiểm tra với JDBC mô phỏng, không kết nối hoặc ghi vào Oracle:

```bash
mvn -o compile
javac --release 17 -cp target/classes -d target/dao-check tools/VerifyDaoTransactions.java
java -cp "target/dao-check:target/classes:$HOME/.m2/repository/org/mindrot/jbcrypt/0.4/jbcrypt-0.4.jar" VerifyDaoTransactions
```

Chín tình huống kiểm tra: commit phiếu và chi tiết, rollback khi thiếu sách, từ chối trả lặp, báo mất một phần/toàn bộ/vượt số lượng, lưu nhân viên cùng tài khoản, rollback hồ sơ khi tài khoản không hợp lệ và chặn xóa chính nhân viên đang đăng nhập. Kiểm tra này không thay thế kiểm thử tích hợp Oracle/trigger.
