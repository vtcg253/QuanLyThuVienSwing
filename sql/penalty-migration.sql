-- Chạy bằng F5 trong Oracle SQL Developer trên schema ứng dụng đã tồn tại.
-- Tạo bảng PHIEUPHAT nếu bảng này chưa có.
DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count
    FROM USER_TABLES
    WHERE TABLE_NAME = 'PHIEUPHAT';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE q'[
            CREATE TABLE PHIEUPHAT (
                MAPP            VARCHAR2(10) CONSTRAINT PK_PHIEUPHAT PRIMARY KEY,
                MAPM            VARCHAR2(10) NOT NULL,
                MASACH          VARCHAR2(10) NOT NULL,
                MANV            VARCHAR2(10) NOT NULL,
                NGAYLAPPHAT     DATE DEFAULT SYSDATE NOT NULL,
                SOLUONGMAT      NUMBER(10) NOT NULL,
                DONGIAPHAT      NUMBER(15,0) NOT NULL,
                TONGTIEN        NUMBER(15,0) NOT NULL,
                TRANGTHAI       VARCHAR2(20) DEFAULT 'CHUA_THANH_TOAN' NOT NULL,
                NGAYTHANHTOAN   DATE,
                CONSTRAINT FK_PP_CTPM FOREIGN KEY (MAPM, MASACH)
                    REFERENCES CT_PHIEUMUON(MAPM, MASACH),
                CONSTRAINT FK_PP_NHANVIEN FOREIGN KEY (MANV) REFERENCES NHANVIEN(MANV),
                CONSTRAINT CK_PP_SOLUONGMAT CHECK (SOLUONGMAT > 0),
                CONSTRAINT CK_PP_DONGIAPHAT CHECK (DONGIAPHAT > 0),
                CONSTRAINT CK_PP_TONGTIEN CHECK (TONGTIEN > 0),
                CONSTRAINT CK_PP_TRANGTHAI CHECK (TRANGTHAI IN ('CHUA_THANH_TOAN', 'DA_THANH_TOAN')),
                CONSTRAINT CK_PP_THANHTOAN CHECK (
                    (TRANGTHAI = 'CHUA_THANH_TOAN' AND NGAYTHANHTOAN IS NULL)
                    OR (TRANGTHAI = 'DA_THANH_TOAN' AND NGAYTHANHTOAN IS NOT NULL)
                )
            )
        ]';
    END IF;
END;
/
