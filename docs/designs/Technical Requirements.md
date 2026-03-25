# **Bản Đặc Tả Kỹ Thuật MQTT Edge Device Simulator**

Tài liệu này quy định các yêu cầu kỹ thuật cho ứng dụng Android giả lập thiết bị Edge truyền tin qua giao thức MQTT.

## **1\. Cấu Hình Kết Nối (Broker Settings)**

* **Broker Host:** Địa chỉ IP hoặc Domain.  
* **Port:** Mặc định 1883 (TCP) hoặc 8883 (SSL).  
* **Authentication:** Hỗ trợ Username/Password.  
* **Client ID:** Tự động lấy MAC Address của thiết bị (yêu cầu quyền Access Wi-Fi State).  
* **Lưu trữ:** Toàn bộ cấu hình phải được lưu vào Local Database (Room/DataStore) để tự động nạp lại khi khởi động ứng dụng.

## **2\. Thông Tin Thiết Bị (Device Info)**

* **Field device\_id:** Người dùng nhập thủ công (vd: DEV-001).  
* **Field device\_type:** Người dùng nhập (vd: GATEWAY-V3).  
* **Network Status:** Định kỳ đọc chỉ số RSSI từ Wi-Fi Manager của Android.

## **3\. Quản Lý DI Pin (Digital Input Management)**

Ứng dụng cho phép thêm không giới hạn các Pin ảo để giả lập tín hiệu số.

* **Thuộc tính Pin:**  
  * pin\_number: ID định danh của Pin (dùng trong Topic).  
  * mode: Manual (Thủ công) hoặc Auto (Tự động).  
  * timer: Khoảng thời gian gửi tin (chỉ dùng cho mode Auto, đơn vị: mili giây).  
  * shoot\_count: Biến đếm tích lũy.  
  * pulse\_time: Thời gian xung (cố định hoặc cấu hình theo Pin).

## **4\. Đặc Tả Message & Topics**

### **4.1. Khởi tạo (Device Init)**

* **Topic:** device/init  
* **Tần suất:** Gửi ngay sau khi kết nối MQTT thành công.  
* **Payload:**

{  
  "time\_stamp": "ISO-8601-Format",  
  "device\_type": "{cấu hình}"  
}

### **4.2. Nhật ký (Device Logs)**

* **Topic:** devices/{device\_id}/log/info  
* **Payload:**

{  
  "message": "{nội dung log}",  
  "time\_stamp": "ISO-8601-Format"  
}

### **4.3. Nhịp tim (Uplink Heartbeat)**

* **Topic:** uplink/heartbeat/v1/{device\_id}  
* **Attribute Name (Enum):** counter, status, temp, humidity.  
* **Payload:**

{  
  "serial\_no": "{device\_id}",  
  "attribute\_name": "counter",  
  "device\_status": "online",  
  "time\_stamp": "ISO-8601-Format",  
  "rssi": \-76  
}

### **4.4. Dữ liệu Pin (DI Data)**

* **Topic:** uplink/v3/di/{pin\_number}  
* **Payload:**

{  
  "time\_stamp": "ISO-8601-Format",  
  "shoot\_count": 331245,  
  "pulse\_time": 1000  
}

## **5\. Logic Xử Lý**

* **Auto Mode:** Sử dụng ScheduledExecutorService hoặc Handler để kích hoạt gửi tin theo chu kỳ.  
* **Manual Mode:** Mỗi khi người dùng nhấn nút "Trigger" trên UI, tăng shoot\_count và gửi message ngay lập tức.  
* **Timestamp:** Phải luôn được cập nhật theo thời gian thực tại thời điểm gửi tin.