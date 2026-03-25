# **Thiết Kế UI/UX Cho MQTT Edge Simulator**

## **1\. Cấu Trúc Phân Cấp Màn Hình**

Ứng dụng sử dụng kiến trúc Single Activity với 2 Tabs chính hoặc Drawer Menu:

* **Dashboard:** Màn hình điều khiển chính (Chạy/Dừng, Trigger Pin).  
* **Settings:** Cấu hình Broker, Device ID và danh sách DI Pins.

## **2\. Chi Tiết Giao Diện**

### **2.1. Màn Hình Dashboard (Trang Chủ)**

* **Trạng thái kết nối:** Đèn LED ảo (Xanh: Connected, Đỏ: Disconnected). Hiển thị chỉ số RSSI (vd: RSSI: \-65dBm).  
* **Nút điều khiển tổng:** \- CONNECT / DISCONNECT  
  * START / STOP SIMULATION (Bật/tắt các luồng gửi tin tự động).  
* **Danh sách DI Pins (Dạng Card):**  
  * Mỗi card hiển thị: Pin ID, Mode, Current Count.  
  * Nếu là **Manual Mode**: Có nút \[ TRIGGER \] lớn, dễ bấm.  
  * Nếu là **Auto Mode**: Hiển thị đếm ngược đến lần gửi tiếp theo.  
* **Console Log (Phía dưới):** Một cửa sổ text nhỏ hiển thị các bản tin MQTT vừa được gửi đi (Real-time).

### **2.2. Màn Hình Cấu Hình (Settings)**

* **Section Broker:** Nhập Host, Port, User, Pass.  
* **Section Device:** Nhập Device ID, Device Type. Hiển thị Client ID (Read-only, lấy từ MAC).  
* **Section Pin Management:**  
  * Nút \[ \+ Add New Pin \].  
  * Danh sách các Pin hiện có với nút Edit/Delete.  
  * **Popup/Bottom Sheet Add Pin:** Chọn ID, chọn Mode (Toggle), nhập Timer.

## **3\. Tương Tác Người Dùng (UX)**

* **Phản hồi xúc giác (Haptic Feedback):** Rung nhẹ khi người dùng nhấn nút "Trigger" ở chế độ thủ công để xác nhận hành động.  
* **Lưu trạng thái:** Nếu ứng dụng đang ở trạng thái "Running", khi người dùng thoát ra và mở lại, ứng dụng nên tự động kết nối lại (nếu cấu hình cho phép).  
* **Validation:** Kiểm tra định dạng IP/Port và không cho phép trùng lặp pin\_number.

## **4\. Màu Sắc & Typography**

* **Primary Color:** Xanh dương đậm (Vibe công nghiệp/kỹ thuật).  
* **Secondary Color:** Xanh lá (cho các trạng thái hoạt động).  
* **Font:** Roboto hoặc JetBrains Mono (cho các phần hiển thị payload/log) để tạo cảm giác chuyên nghiệp cho kỹ sư.

## **5\. Mockup Bố Cục (Wireframe)**

\+---------------------------------------+  
|  \[Status: Connected\]    \[RSSI: \-70\]   |  
|---------------------------------------|  
|  \[ CONNECT \] \[ DISCONNECT \] \[ SETTING\]|  
|---------------------------------------|  
| DI PINS:                              |  
| \+-----------------------------------+ |  
| | Pin 01 (Auto \- 5s)                | |  
| | Count: 1250          \[Running...\] | |  
| \+-----------------------------------+ |  
| \+-----------------------------------+ |  
| | Pin 02 (Manual)                   | |  
| | Count: 88          \[ TRIGGER \]    | |  
| \+-----------------------------------+ |  
|---------------------------------------|  
| LOGS:                                 |  
| \> devices/DEV-001/log/info sent...    |  
\+---------------------------------------+  
