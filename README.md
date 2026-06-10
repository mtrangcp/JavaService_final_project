# API Documentation 

> **Base URL:** `http://localhost:8080/api/v1/auth`
> **Content-Type:** `application/json`
> **Auth Header (khi cần):** `Authorization: Bearer <accessToken>`

---

## FR-04 — Đăng ký tài khoản

- **Method:** `POST`
- **Endpoint:** `/api/v1/auth/register`
- **Phân quyền:** Public (không cần token)
- **Mô tả:** Tạo tài khoản mới cho Ứng viên (CANDIDATE) hoặc Nhà tuyển dụng (EMPLOYER). Tài khoản Admin không thể được tạo qua endpoint này.

### Request Body

```json
{
  "fullName": "Nguyen Van A",
  "email": "nguyenvana@gmail.com",
  "password": "matkhau123",
  "role": "CANDIDATE"
}
```

| Field      | Type   | Bắt buộc | Ràng buộc                              |
|------------|--------|----------|----------------------------------------|
| `fullName` | String | ✅        | Không được để trống                    |
| `email`    | String | ✅        | Đúng định dạng email, chưa tồn tại    |
| `password` | String | ✅        | Tối thiểu 6 ký tự                     |
| `role`     | Enum   | ✅        | Chỉ chấp nhận `CANDIDATE`, `EMPLOYER` |

### Response — Thành công `201 Created`

```json
{
  "status": 201,
  "message": "Đăng ký thành công",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "email": "nguyenvana@gmail.com",
    "role": "CANDIDATE"
  }
}
```

### Response — Thất bại

- **`400 Bad Request`** — Thiếu hoặc sai định dạng trường bắt buộc:
```json
{
  "status": 400,
  "message": "Dữ liệu đầu vào không hợp lệ",
  "data": {
    "email": "Email không hợp lệ",
    "password": "Mật khẩu phải có ít nhất 6 ký tự"
  }
}
```

- **`403 Forbidden`** — Cố gắng đăng ký với role ADMIN:
```json
{
  "status": 403,
  "message": "Không thể đăng ký tài khoản Admin",
  "data": null
}
```

- **`409 Conflict`** — Email đã tồn tại trong hệ thống:
```json
{
  "status": 409,
  "message": "Email đã tồn tại trong hệ thống",
  "data": null
}
```

---

## FR-01 — Đăng nhập hệ thống (Cấp phát JWT)

- **Method:** `POST`
- **Endpoint:** `/api/v1/auth/login`
- **Phân quyền:** Public (không cần token)
- **Mô tả:** Xác thực thông tin đăng nhập. Nếu hợp lệ, hệ thống cấp phát `AccessToken` (hạn ngắn) và `RefreshToken` (hạn dài) để Client sử dụng cho các request tiếp theo.

### Request Body

```json
{
  "email": "nguyenvana@gmail.com",
  "password": "matkhau123"
}
```

| Field      | Type   | Bắt buộc | Ràng buộc              |
|------------|--------|----------|------------------------|
| `email`    | String | ✅        | Đúng định dạng email   |
| `password` | String | ✅        | Không được để trống    |

### Response — Thành công `200 OK`

```json
{
  "status": 200,
  "message": "Đăng nhập thành công",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "email": "nguyenvana@gmail.com",
    "role": "CANDIDATE"
  }
}
```

### Response — Thất bại

- **`400 Bad Request`** — Thiếu trường bắt buộc hoặc sai định dạng email:
```json
{
  "status": 400,
  "message": "Dữ liệu đầu vào không hợp lệ",
  "data": {
    "email": "Email không hợp lệ"
  }
}
```

- **`401 Unauthorized`** — Sai email hoặc mật khẩu:
```json
{
  "status": 401,
  "message": "Email hoặc mật khẩu không chính xác",
  "data": null
}
```

- **`403 Forbidden`** — Tài khoản bị khóa (isActive = false):
```json
{
  "status": 403,
  "message": "Tài khoản đã bị khóa",
  "data": null
}
```

---

## FR-03 — Đăng xuất (Revoke Token)

- **Method:** `POST`
- **Endpoint:** `/api/v1/auth/logout`
- **Phân quyền:** Authenticated — yêu cầu `AccessToken` hợp lệ trong Header
- **Mô tả:** Vô hiệu hóa `AccessToken` hiện tại bằng cách đưa vào danh sách đen (`TokenBlacklist`). Sau khi logout, mọi request dùng token này đều bị từ chối, kể cả khi token chưa hết hạn.

### Request Header

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

### Request Body

_(Không có — toàn bộ thông tin lấy từ Header)_

### Response — Thành công `200 OK`

```json
{
  "status": 200,
  "message": "Đăng xuất thành công",
  "data": null
}
```

### Response — Thất bại

- **`401 Unauthorized`** — Không có token hoặc token sai định dạng:
```json
{
  "status": 401,
  "message": "Token không hợp lệ",
  "data": null
}
```

- **`401 Unauthorized`** — Token đã bị thu hồi trước đó (đã logout rồi):
```json
{
  "status": 401,
  "message": "Token đã bị thu hồi trước đó",
  "data": null
}
```

---

## FR-02 — Xoay vòng Token (Refresh Token)

- **Method:** `POST`
- **Endpoint:** `/api/v1/auth/refresh`
- **Phân quyền:** Public (không cần AccessToken, chỉ cần RefreshToken còn hạn)
- **Mô tả:** Khi `AccessToken` hết hạn, Client gửi `RefreshToken` để nhận cặp token mới mà không cần đăng nhập lại. `RefreshToken` cũ sẽ bị thu hồi ngay sau khi dùng (Rotation Strategy) để chống tái sử dụng.

### Request Body

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

| Field          | Type   | Bắt buộc | Ràng buộc              |
|----------------|--------|----------|------------------------|
| `refreshToken` | String | ✅        | Không được để trống    |

### Response — Thành công `200 OK`

```json
{
  "status": 200,
  "message": "Cấp lại token thành công",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...<mới>",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...<mới>",
    "tokenType": "Bearer",
    "email": "nguyenvana@gmail.com",
    "role": "CANDIDATE"
  }
}
```

### Response — Thất bại

- **`400 Bad Request`** — Body thiếu trường `refreshToken`:
```json
{
  "status": 400,
  "message": "Dữ liệu đầu vào không hợp lệ",
  "data": {
    "refreshToken": "Refresh token không được để trống"
  }
}
```

- **`401 Unauthorized`** — RefreshToken hết hạn, không hợp lệ, hoặc đã bị thu hồi:
```json
{
  "status": 401,
  "message": "Refresh token không hợp lệ hoặc đã hết hạn",
  "data": null
}
```

- **`403 Forbidden`** — Tài khoản bị khóa trong khi dùng RefreshToken:
```json
{
  "status": 403,
  "message": "Tài khoản đã bị khóa",
  "data": null
}
```

-----------------






