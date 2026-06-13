# API Documentation — Job Exchange & Recruitment Platform

> **Base URL:** `http://localhost:8080`
> **API Version:** `v1`
> **Content-Type:** `application/json` _FR-09 dùng `multipart/form-data`)_
> **Auth Header:** `Authorization: Bearer <accessToken>`


## Cấu trúc Response chuẩn

Mọi API đều trả về cùng 1 cấu trúc:

```json
{
  "status":  200,
  "message": "Mô tả kết quả",
  "data":    { }
}
```

---

## Ma trận phân quyền

| Prefix | Role yêu cầu |
| `/api/v1/auth/**` | Public — không cần token |
| `/api/v1/admin/**` | `ADMIN` |
| `/api/v1/employer/**` | `EMPLOYER` |
| `/api/v1/candidate/**` | `CANDIDATE` |

## Mục lục

- [FR-01 — Đăng nhập](#fr-01--đăng-nhập-hệ-thống)
- [FR-02 — Refresh Token](#fr-02--xoay-vòng-token)
- [FR-03 — Đăng xuất](#fr-03--đăng-xuất)
- [FR-04 — Đăng ký tài khoản](#fr-04--đăng-ký-tài-khoản)
- [FR-05 — Quản lý User & Duyệt tin (Admin)](#fr-05--quản-lý-người-dùng--duyệt-tin-tuyển-dụng)
- [FR-06 — Đăng tin tuyển dụng (Employer)](#fr-06--đăng-tin-tuyển-dụng)
- [FR-07 — Tìm kiếm & Nộp hồ sơ (Candidate)](#fr-07--tìm-kiếm--nộp-hồ-sơ-ứng-tuyển)
- [FR-08 — Cập nhật trạng thái hồ sơ (Employer)](#fr-08--cập-nhật-trạng-thái-hồ-sơ)
- [FR-09 — Upload CV](#fr-09--tải-lên-cv)
- [FR-10 — Đổi & Quên mật khẩu](#fr-10--đổi-mật-khẩu--quên-mật-khẩu)

---

## FR-01 — Đăng nhập hệ thống

- **Method:** `POST`
- **Endpoint:** `/api/v1/auth/login`
- **Phân quyền:** Public
- **Mô tả:** Xác thực thông tin đăng nhập. Trả về `AccessToken` (hạn 30 phút) và `RefreshToken` (hạn 7 ngày).

### Request Body

```json
{
  "email":    "nguyenvana@gmail.com",
  "password": "matkhau123"
}
```

| Field      | Type   | Bắt buộc | Ràng buộc            |
|------------|--------|----------|----------------------|
| `email`    | String | ✅        | Đúng định dạng email |
| `password` | String | ✅        | Không được để trống  |

### Response — Thành công `200 OK`

```json
{
  "status": 200,
  "message": "Đăng nhập thành công",
  "data": {
    "accessToken":  "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType":    "Bearer",
    "email":        "nguyenvana@gmail.com",
    "role":         "CANDIDATE"
  }
}
```

### Response — Thất bại

- **`400 Bad Request`** — Sai định dạng email hoặc thiếu trường:
```json
{
  "status": 400,
  "message": "Dữ liệu đầu vào không hợp lệ",
  "data": { "email": "Email không hợp lệ" }
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

- **`403 Forbidden`** — Tài khoản bị khóa:
```json
{
  "status": 403,
  "message": "Tài khoản đã bị khóa",
  "data": null
}
```

---

## FR-02 — Xoay vòng Token

- **Method:** `POST`
- **Endpoint:** `/api/v1/auth/refresh`
- **Phân quyền:** Public
- **Mô tả:** Dùng `RefreshToken` để nhận cặp token mới. `RefreshToken` cũ bị thu hồi ngay sau khi dùng (Rotation Strategy).

### Request Body

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

| Field          | Type   | Bắt buộc | Ràng buộc           |
|----------------|--------|----------|---------------------|
| `refreshToken` | String | ✅        | Không được để trống |

### Response — Thành công `200 OK`

```json
{
  "status": 200,
  "message": "Cấp lại token thành công",
  "data": {
    "accessToken":  "eyJhbGciOiJIUzI1NiJ9...<mới>",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...<mới>",
    "tokenType":    "Bearer",
    "email":        "nguyenvana@gmail.com",
    "role":         "CANDIDATE"
  }
}
```

### Response — Thất bại

- **`400 Bad Request`** — Thiếu trường `refreshToken`:
```json
{
  "status": 400,
  "message": "Dữ liệu đầu vào không hợp lệ",
  "data": { "refreshToken": "Refresh token không được để trống" }
}
```

- **`401 Unauthorized`** — Token hết hạn, không hợp lệ hoặc đã bị thu hồi:
```json
{
  "status": 401,
  "message": "Refresh token không hợp lệ hoặc đã hết hạn",
  "data": null
}
```

- **`401 Unauthorized`** — Gửi AccessToken thay vì RefreshToken:
```json
{
  "status": 401,
  "message": "Token không đúng loại, yêu cầu Refresh Token",
  "data": null
}
```

- **`403 Forbidden`** — Tài khoản bị khóa:
```json
{
  "status": 403,
  "message": "Tài khoản đã bị khóa",
  "data": null
}
```

---

## FR-03 — Đăng xuất

- **Method:** `POST`
- **Endpoint:** `/api/v1/auth/logout`
- **Phân quyền:** Authenticated — yêu cầu `AccessToken` hợp lệ
- **Mô tả:** Đưa `AccessToken` hiện tại vào Blacklist. Mọi request tiếp theo dùng token này đều bị từ chối, 
kể cả khi chưa hết hạn.

### Request Header

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

### Request Body

_Không có_

### Response — Thành công `200 OK`

```json
{
  "status": 200,
  "message": "Đăng xuất thành công",
  "data": null
}
```

### Response — Thất bại

- **`401 Unauthorized`** — Không có token hoặc sai định dạng Header:
```json
{
  "status": 401,
  "message": "Token không hợp lệ",
  "data": null
}
```

- **`401 Unauthorized`** — Token đã bị thu hồi trước đó:
```json
{
  "status": 401,
  "message": "Token đã bị thu hồi trước đó",
  "data": null
}
```

---

## FR-04 — Đăng ký tài khoản

- **Method:** `POST`
- **Endpoint:** `/api/v1/auth/register`
- **Phân quyền:** Public
- **Mô tả:** Tạo tài khoản mới với role `CANDIDATE` hoặc `EMPLOYER`. Không thể đăng ký tài khoản `ADMIN` qua endpoint này.

### Request Body

```json
{
  "fullName": "Nguyen Van A",
  "email":    "nguyenvana@gmail.com",
  "password": "matkhau123",
  "role":     "CANDIDATE"
}
```

| Field      | Type   | Bắt buộc | Ràng buộc                               |
|------------|--------|----------|-----------------------------------------|
| `fullName` | String | ✅        | Không được để trống                     |
| `email`    | String | ✅        | Đúng định dạng email, chưa tồn tại     |
| `password` | String | ✅        | Tối thiểu 6 ký tự                      |
| `role`     | Enum   | ✅        | Chỉ chấp nhận `CANDIDATE`, `EMPLOYER`  |

### Response — Thành công `201 Created`

```json
{
  "status": 201,
  "message": "Đăng ký thành công",
  "data": {
    "accessToken":  "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType":    "Bearer",
    "email":        "nguyenvana@gmail.com",
    "role":         "CANDIDATE"
  }
}
```

### Response — Thất bại

- **`400 Bad Request`** — Thiếu hoặc sai định dạng trường:
```json
{
  "status": 400,
  "message": "Dữ liệu đầu vào không hợp lệ",
  "data": {
    "fullName": "Họ tên không được để trống",
    "password": "Mật khẩu phải có ít nhất 6 ký tự"
  }
}
```

- **`403 Forbidden`** — Cố đăng ký role ADMIN:
```json
{
  "status": 403,
  "message": "Không thể đăng ký tài khoản Admin",
  "data": null
}
```

- **`409 Conflict`** — Email đã tồn tại:
```json
{
  "status": 409,
  "message": "Email đã tồn tại trong hệ thống",
  "data": null
}
```

---

## FR-05 — Quản lý Người dùng & Duyệt tin tuyển dụng

> Tất cả endpoint trong mục này yêu cầu Role **ADMIN**

---

### 5.1 — Lấy danh sách người dùng

- **Method:** `GET`
- **Endpoint:** `/api/v1/admin/users`
- **Mô tả:** Lấy danh sách người dùng có phân trang, hỗ trợ tìm kiếm theo tên/email và lọc theo role.

### Request Params

| Param     | Type   | Bắt buộc | Mô tả                                         |
|-----------|--------|----------|-----------------------------------------------|
| `keyword` | String | ❌        | Tìm theo tên hoặc email                       |
| `role`    | Enum   | ❌        | Lọc theo `ADMIN`, `EMPLOYER`, `CANDIDATE`     |
| `page`    | int    | ❌        | Số trang, bắt đầu từ `0` (default: `0`)      |
| `size`    | int    | ❌        | Số bản ghi mỗi trang (default: `10`)          |

### Ví dụ Request

```
GET /api/v1/admin/users?keyword=nguyen&role=CANDIDATE&page=0&size=10
Authorization: Bearer <adminToken>
```

### Response — Thành công `200 OK`

```json
{
  "status": 200,
  "message": "Lấy danh sách người dùng thành công",
  "data": {
    "content": [
      {
        "id":       1,
        "fullName": "Nguyen Van A",
        "email":    "nguyenvana@gmail.com",
        "role":     "CANDIDATE",
        "isActive": true
      }
    ],
    "pageNumber":    0,
    "pageSize":      10,
    "totalElements": 1,
    "totalPages":    1,
    "last":          true
  }
}
```

### Response — Thất bại

- **`403 Forbidden`** — Không phải ADMIN:
```json
{
  "status": 403,
  "message": "Không đủ quyền truy cập",
  "data": null
}
```

---

### 5.2 — Lấy chi tiết người dùng

- **Method:** `GET`
- **Endpoint:** `/api/v1/admin/users/{id}`

### Ví dụ Request

```
GET /api/v1/admin/users/1
Authorization: Bearer <adminToken>
```

### Response — Thành công `200 OK`

```json
{
  "status": 200,
  "message": "Lấy thông tin người dùng thành công",
  "data": {
    "id":       1,
    "fullName": "Nguyen Van A",
    "email":    "nguyenvana@gmail.com",
    "role":     "CANDIDATE",
    "isActive": true
  }
}
```

### Response — Thất bại

- **`404 Not Found`** — ID không tồn tại:
```json
{
  "status": 404,
  "message": "Không tìm thấy người dùng với ID: 99",
  "data": null
}
```

---

### 5.3 — Cập nhật người dùng

- **Method:** `PUT`
- **Endpoint:** `/api/v1/admin/users/{id}`

### Request Body

```json
{
  "fullName": "Nguyen Van B",
  "isActive": false,
  "role":     "EMPLOYER"
}
```

| Field      | Type    | Bắt buộc | Ràng buộc                    |
|------------|---------|----------|------------------------------|
| `fullName` | String  | ❌        | Bỏ trống → giữ nguyên       |
| `isActive` | Boolean | ✅        | `true` hoặc `false`          |
| `role`     | Enum    | ❌        | Bỏ trống → giữ nguyên       |

### Response — Thành công `200 OK`

```json
{
  "status": 200,
  "message": "Cập nhật người dùng thành công",
  "data": {
    "id":       1,
    "fullName": "Nguyen Van B",
    "email":    "nguyenvana@gmail.com",
    "role":     "EMPLOYER",
    "isActive": false
  }
}
```

---

### 5.4 — Vô hiệu hóa người dùng

- **Method:** `DELETE`
- **Endpoint:** `/api/v1/admin/users/{id}`
- **Mô tả:** Soft delete — đặt `isActive = false`, không xóa dữ liệu khỏi DB.

### Response — Thành công `200 OK`

```json
{
  "status": 200,
  "message": "Vô hiệu hóa người dùng thành công",
  "data": null
}
```

---

### 5.5 — Lấy danh sách tin tuyển dụng (Admin)

- **Method:** `GET`
- **Endpoint:** `/api/v1/admin/jobs`
- **Mô tả:** Lấy tất cả tin tuyển dụng của hệ thống, hỗ trợ lọc theo trạng thái và tìm kiếm theo tiêu đề.

### Request Params

| Param     | Type   | Bắt buộc | Mô tả                                                                         |
|-----------|--------|----------|-------------------------------------------------------------------------------|
| `keyword` | String | ❌        | Tìm theo tiêu đề tin                                                          |
| `status`  | Enum   | ❌        | `DRAFT`, `PENDING_APPROVAL`, `APPROVED`, `REJECTED`, `CLOSED`                |
| `page`    | int    | ❌        | Số trang (default: `0`)                                                       |
| `size`    | int    | ❌        | Số bản ghi mỗi trang (default: `10`)                                          |

### Ví dụ Request

```
GET /api/v1/admin/jobs?status=PENDING_APPROVAL&page=0&size=10
Authorization: Bearer <adminToken>
```

### Response — Thành công `200 OK`

```json
{
  "status": 200,
  "message": "Lấy danh sách tin tuyển dụng thành công",
  "data": {
    "content": [
      {
        "id":           1,
        "title":        "Backend Developer Java",
        "description":  "Yêu cầu 1-2 năm kinh nghiệm Spring Boot",
        "salaryRange":  "15,000,000 - 25,000,000 VND",
        "status":       "PENDING_APPROVAL",
        "employerEmail":"employer@test.com",
        "employerName": "Công ty ABC",
        "createdAt":    "2025-01-01T08:00:00",
        "updatedAt":    null
      }
    ],
    "pageNumber":    0,
    "pageSize":      10,
    "totalElements": 1,
    "totalPages":    1,
    "last":          true
  }
}
```

---

### 5.6 — Duyệt / Từ chối tin tuyển dụng

- **Method:** `PUT`
- **Endpoint:** `/api/v1/admin/jobs/{id}/status`
- **Mô tả:** Admin chỉ được phép chuyển trạng thái `PENDING_APPROVAL` → `APPROVED` hoặc `REJECTED`.

### Request Body

```json
{
  "status": "APPROVED"
}
```

| Field    | Type | Bắt buộc | Ràng buộc                         |
|----------|------|----------|-----------------------------------|
| `status` | Enum | ✅        | Chỉ chấp nhận `APPROVED`, `REJECTED` |

### Response — Thành công `200 OK`

```json
{
  "status": 200,
  "message": "Cập nhật trạng thái tin tuyển dụng thành công",
  "data": {
    "id":           1,
    "title":        "Backend Developer Java",
    "description":  "Yêu cầu 1-2 năm kinh nghiệm Spring Boot",
    "salaryRange":  "15,000,000 - 25,000,000 VND",
    "status":       "APPROVED",
    "employerEmail":"employer@test.com",
    "employerName": "Công ty ABC",
    "createdAt":    "2025-01-01T08:00:00",
    "updatedAt":    "2025-01-02T10:00:00"
  }
}
```

### Response — Thất bại

- **`400 Bad Request`** — Tin không ở trạng thái `PENDING_APPROVAL`:
```json
{
  "status": 400,
  "message": "Chỉ có thể duyệt tin đang ở trạng thái PENDING_APPROVAL",
  "data": null
}
```

- **`400 Bad Request`** — Gửi status không hợp lệ:
```json
{
  "status": 400,
  "message": "Admin chỉ được phép APPROVED hoặc REJECTED",
  "data": null
}
```

---

## FR-06 — Đăng tin tuyển dụng

> Tất cả endpoint trong mục này yêu cầu Role **EMPLOYER**

---

### 6.1 — Đăng tin mới

- **Method:** `POST`
- **Endpoint:** `/api/v1/employer/jobs`
- **Mô tả:** Tạo tin tuyển dụng mới với trạng thái mặc định `PENDING_APPROVAL`. Tin cần Admin duyệt trước khi hiển thị với Candidate.

### Request Body

```json
{
  "title":       "Senior Java Developer",
  "description": "Yêu cầu 3 năm kinh nghiệm Java Spring Boot, microservices.",
  "salaryRange": "30,000,000 - 50,000,000 VND"
}
```

| Field         | Type   | Bắt buộc | Ràng buộc           |
|---------------|--------|----------|---------------------|
| `title`       | String | ✅        | Không được để trống |
| `description` | String | ✅        | Không được để trống |
| `salaryRange` | String | ❌        | Tuỳ chọn            |

### Response — Thành công `201 Created`

```json
{
  "status": 201,
  "message": "Đăng tin tuyển dụng thành công",
  "data": {
    "id":           1,
    "title":        "Senior Java Developer",
    "description":  "Yêu cầu 3 năm kinh nghiệm Java Spring Boot, microservices.",
    "salaryRange":  "30,000,000 - 50,000,000 VND",
    "status":       "PENDING_APPROVAL",
    "employerEmail":"employer@test.com",
    "employerName": "Công ty ABC",
    "createdAt":    "2025-01-01T08:00:00",
    "updatedAt":    null
  }
}
```

### Response — Thất bại

- **`400 Bad Request`** — Thiếu trường bắt buộc:
```json
{
  "status": 400,
  "message": "Dữ liệu đầu vào không hợp lệ",
  "data": { "title": "Tiêu đề không được để trống" }
}
```

---

### 6.2 — Lấy danh sách tin của Employer

- **Method:** `GET`
- **Endpoint:** `/api/v1/employer/jobs`

### Request Params

| Param  | Type | Bắt buộc | Mô tả                         |
|--------|------|----------|-------------------------------|
| `page` | int  | ❌        | Số trang (default: `0`)       |
| `size` | int  | ❌        | Số bản ghi mỗi trang (default: `10`) |

### Response — Thành công `200 OK`

```json
{
  "status": 200,
  "message": "Lấy danh sách tin thành công",
  "data": {
    "content":       [ ],
    "pageNumber":    0,
    "pageSize":      10,
    "totalElements": 0,
    "totalPages":    0,
    "last":          true
  }
}
```

---

### 6.3 — Cập nhật tin tuyển dụng

- **Method:** `PUT`
- **Endpoint:** `/api/v1/employer/jobs/{id}`
- **Mô tả:** Chỉ được sửa khi tin ở trạng thái `DRAFT` hoặc `REJECTED`.

### Request Body

```json
{
  "title":       "Junior Java Developer",
  "description": "Yêu cầu 1 năm kinh nghiệm.",
  "salaryRange": "15,000,000 - 20,000,000 VND"
}
```

### Response — Thành công `200 OK`

```json
{
  "status": 200,
  "message": "Cập nhật tin tuyển dụng thành công",
  "data": { }
}
```

### Response — Thất bại

- **`400 Bad Request`** — Tin đang ở trạng thái không cho phép sửa:
```json
{
  "status": 400,
  "message": "Không thể sửa tin đang ở trạng thái APPROVED",
  "data": null
}
```

- **`403 Forbidden`** — Tin không thuộc Employer này:
```json
{
  "status": 403,
  "message": "Bạn không có quyền thao tác với tin tuyển dụng này",
  "data": null
}
```

---

### 6.4 — Nộp tin lên duyệt

- **Method:** `PATCH`
- **Endpoint:** `/api/v1/employer/jobs/{id}/submit`
- **Mô tả:** Chuyển trạng thái `DRAFT` hoặc `REJECTED` → `PENDING_APPROVAL` để Admin xét duyệt.

### Response — Thành công `200 OK`

```json
{
  "status": 200,
  "message": "Nộp tin lên duyệt thành công",
  "data": { }
}
```

---

### 6.5 — Đóng tin tuyển dụng

- **Method:** `PATCH`
- **Endpoint:** `/api/v1/employer/jobs/{id}/close`
- **Mô tả:** Chuyển trạng thái tin sang `CLOSED`, ngừng nhận hồ sơ.

### Response — Thành công `200 OK`

```json
{
  "status": 200,
  "message": "Đóng tin tuyển dụng thành công",
  "data": { }
}
```

---

### 6.6 — Xóa tin tuyển dụng

- **Method:** `DELETE`
- **Endpoint:** `/api/v1/employer/jobs/{id}`
- **Mô tả:** Xóa cứng tin khỏi DB. Chỉ được xóa khi tin ở trạng thái `DRAFT`.

### Response — Thành công `200 OK`

```json
{
  "status": 200,
  "message": "Xóa tin tuyển dụng thành công",
  "data": null
}
```

### Response — Thất bại

- **`400 Bad Request`** — Tin không ở trạng thái DRAFT:
```json
{
  "status": 400,
  "message": "Chỉ có thể xóa tin ở trạng thái DRAFT",
  "data": null
}
```

---

## FR-07 — Tìm kiếm & Nộp hồ sơ ứng tuyển

> Tất cả endpoint trong mục này yêu cầu Role **CANDIDATE**

---

### 7.1 — Tìm kiếm việc làm

- **Method:** `GET`
- **Endpoint:** `/api/v1/candidate/jobs`
- **Mô tả:** Tìm kiếm tin tuyển dụng. Chỉ trả về tin có trạng thái `APPROVED`.

### Request Params

| Param     | Type   | Bắt buộc | Mô tả                                  |
|-----------|--------|----------|----------------------------------------|
| `keyword` | String | ❌        | Tìm theo tiêu đề tin                   |
| `page`    | int    | ❌        | Số trang (default: `0`)                |
| `size`    | int    | ❌        | Số bản ghi mỗi trang (default: `10`)   |

### Ví dụ Request

```
GET /api/v1/candidate/jobs?keyword=java&page=0&size=10
Authorization: Bearer <candidateToken>
```

### Response — Thành công `200 OK`

```json
{
  "status": 200,
  "message": "Tìm kiếm việc làm thành công",
  "data": {
    "content": [
      {
        "id":           1,
        "title":        "Backend Developer Java",
        "description":  "Yêu cầu 1-2 năm kinh nghiệm Spring Boot",
        "salaryRange":  "15,000,000 - 25,000,000 VND",
        "status":       "APPROVED",
        "employerEmail":"employer@test.com",
        "employerName": "Công ty ABC",
        "createdAt":    "2025-01-01T08:00:00",
        "updatedAt":    null
      }
    ],
    "pageNumber":    0,
    "pageSize":      10,
    "totalElements": 1,
    "totalPages":    1,
    "last":          true
  }
}
```

---

### 7.2 — Nộp hồ sơ ứng tuyển

- **Method:** `POST`
- **Endpoint:** `/api/v1/candidate/applications`
- **Mô tả:** Nộp hồ sơ vào một tin tuyển dụng. Hệ thống tự động ghi log qua AOP `@AfterReturning`. Chỉ nộp được vào tin `APPROVED` và chưa từng nộp trước đó.

### Request Body

```json
{
  "jobId":       1,
  "coverLetter": "Tôi rất quan tâm đến vị trí này...",
  "cvUrl":       "https://res.cloudinary.com/demo/raw/upload/job_portal/cv/cv_1.pdf"
}
```

| Field         | Type   | Bắt buộc | Ràng buộc              |
|---------------|--------|----------|------------------------|
| `jobId`       | Long   | ✅        | Không được để trống    |
| `coverLetter` | String | ❌        | Tuỳ chọn              |
| `cvUrl`       | String | ❌        | Tuỳ chọn              |

### Response — Thành công `201 Created`

```json
{
  "status": 201,
  "message": "Nộp hồ sơ thành công",
  "data": {
    "id":               1,
    "jobId":            1,
    "jobTitle":         "Backend Developer Java",
    "employerName":     "Công ty ABC",
    "candidateId":      2,
    "candidateName":    "Nguyen Van A",
    "candidateEmail":   "nguyenvana@gmail.com",
    "coverLetter":      "Tôi rất quan tâm đến vị trí này...",
    "cvUrl":            "https://res.cloudinary.com/...",
    "status":           "PENDING",
    "employerFeedback": null,
    "appliedAt":        "2025-01-01T09:00:00",
    "updatedAt":        null
  }
}
```

### Response — Thất bại

- **`404 Not Found`** — Tin tuyển dụng không tồn tại:
```json
{
  "status": 404,
  "message": "Không tìm thấy tin tuyển dụng với ID: 99",
  "data": null
}
```

- **`409 Conflict`** — Tin đã đóng hoặc không ở trạng thái APPROVED:
```json
{
  "status": 409,
  "message": "Tin tuyển dụng này hiện không nhận hồ sơ",
  "data": null
}
```

- **`409 Conflict`** — Đã nộp hồ sơ vào tin này trước đó:
```json
{
  "status": 409,
  "message": "Bạn đã nộp hồ sơ vào tin tuyển dụng này rồi",
  "data": null
}
```

---

### 7.3 — Xem danh sách hồ sơ đã nộp

- **Method:** `GET`
- **Endpoint:** `/api/v1/candidate/applications`

### Request Params

| Param    | Type | Bắt buộc | Mô tả                                                                  |
|----------|------|----------|------------------------------------------------------------------------|
| `status` | Enum | ❌        | `PENDING`, `REVIEWING`, `INTERVIEWING`, `ACCEPTED`, `REJECTED`        |
| `page`   | int  | ❌        | Số trang (default: `0`)                                                |
| `size`   | int  | ❌        | Số bản ghi mỗi trang (default: `10`)                                   |

### Response — Thành công `200 OK`

```json
{
  "status": 200,
  "message": "Lấy danh sách hồ sơ thành công",
  "data": {
    "content":       [ ],
    "pageNumber":    0,
    "pageSize":      10,
    "totalElements": 0,
    "totalPages":    0,
    "last":          true
  }
}
```

---

### 7.4 — Xem chi tiết hồ sơ đã nộp

- **Method:** `GET`
- **Endpoint:** `/api/v1/candidate/applications/{id}`

### Response — Thành công `200 OK`

```json
{
  "status": 200,
  "message": "Lấy chi tiết hồ sơ thành công",
  "data": {
    "id":               1,
    "jobId":            1,
    "jobTitle":         "Backend Developer Java",
    "employerName":     "Công ty ABC",
    "candidateId":      2,
    "candidateName":    "Nguyen Van A",
    "candidateEmail":   "nguyenvana@gmail.com",
    "coverLetter":      "Tôi rất quan tâm đến vị trí này...",
    "cvUrl":            "https://res.cloudinary.com/...",
    "status":           "REVIEWING",
    "employerFeedback": "Hồ sơ phù hợp, mời phỏng vấn.",
    "appliedAt":        "2025-01-01T09:00:00",
    "updatedAt":        "2025-01-02T10:00:00"
  }
}
```

### Response — Thất bại

- **`403 Forbidden`** — Hồ sơ không thuộc Candidate này:
```json
{
  "status": 403,
  "message": "Bạn không có quyền xem hồ sơ này",
  "data": null
}
```

---

## FR-08 — Cập nhật trạng thái hồ sơ

> Tất cả endpoint trong mục này yêu cầu Role **EMPLOYER**

---

### 8.1 — Xem tất cả hồ sơ ứng tuyển vào job của mình

- **Method:** `GET`
- **Endpoint:** `/api/v1/employer/applications`

### Request Params

| Param    | Type | Bắt buộc | Mô tả                                                           |
|----------|------|----------|-----------------------------------------------------------------|
| `status` | Enum | ❌        | `PENDING`, `REVIEWING`, `INTERVIEWING`, `ACCEPTED`, `REJECTED` |
| `page`   | int  | ❌        | Số trang (default: `0`)                                         |
| `size`   | int  | ❌        | Số bản ghi mỗi trang (default: `10`)                            |

### Response — Thành công `200 OK`

```json
{
  "status": 200,
  "message": "Lấy danh sách hồ sơ thành công",
  "data": {
    "content":       [ ],
    "pageNumber":    0,
    "pageSize":      10,
    "totalElements": 0,
    "totalPages":    0,
    "last":          true
  }
}
```

---

### 8.2 — Xem hồ sơ theo từng Job cụ thể

- **Method:** `GET`
- **Endpoint:** `/api/v1/employer/jobs/{jobId}/applications`

### Ví dụ Request

```
GET /api/v1/employer/jobs/1/applications?status=PENDING&page=0&size=10
Authorization: Bearer <employerToken>
```

### Response — Thành công `200 OK`

```json
{
  "status": 200,
  "message": "Lấy danh sách hồ sơ theo job thành công",
  "data": {
    "content":       [ ],
    "pageNumber":    0,
    "pageSize":      10,
    "totalElements": 0,
    "totalPages":    0,
    "last":          true
  }
}
```

### Response — Thất bại

- **`403 Forbidden`** — Job không thuộc Employer này:
```json
{
  "status": 403,
  "message": "Bạn không có quyền xem hồ sơ của tin này",
  "data": null
}
```

---

### 8.3 — Cập nhật trạng thái hồ sơ

- **Method:** `PATCH`
- **Endpoint:** `/api/v1/employer/applications/{id}/status`
- **Mô tả:** Cập nhật trạng thái hồ sơ theo State Machine. Hệ thống ghi log tự động qua AOP `@AfterReturning`.

### State Machine hợp lệ

```
PENDING → REVIEWING → INTERVIEWING → ACCEPTED
                  ↘               ↘
                  REJECTED        REJECTED
PENDING → REJECTED (từ chối sớm)
```

### Request Body

```json
{
  "status":   "REVIEWING",
  "feedback": "Hồ sơ phù hợp, chúng tôi sẽ liên hệ sớm."
}
```

| Field      | Type   | Bắt buộc | Ràng buộc                                                           |
|------------|--------|----------|---------------------------------------------------------------------|
| `status`   | Enum   | ✅        | Phải đúng luồng State Machine                                       |
| `feedback` | String | ❌        | Phản hồi kèm theo cho Candidate                                     |

### Response — Thành công `200 OK`

```json
{
  "status": 200,
  "message": "Cập nhật trạng thái hồ sơ thành công",
  "data": {
    "id":               1,
    "jobId":            1,
    "jobTitle":         "Backend Developer Java",
    "employerName":     "Công ty ABC",
    "candidateId":      2,
    "candidateName":    "Nguyen Van A",
    "candidateEmail":   "nguyenvana@gmail.com",
    "coverLetter":      "Tôi rất quan tâm đến vị trí này...",
    "cvUrl":            "https://res.cloudinary.com/...",
    "status":           "REVIEWING",
    "employerFeedback": "Hồ sơ phù hợp, chúng tôi sẽ liên hệ sớm.",
    "appliedAt":        "2025-01-01T09:00:00",
    "updatedAt":        "2025-01-02T10:00:00"
  }
}
```

### Response — Thất bại

- **`400 Bad Request`** — Vi phạm luồng State Machine:
```json
{
  "status": 400,
  "message": "Không thể chuyển trạng thái từ PENDING sang ACCEPTED",
  "data": null
}
```

- **`403 Forbidden`** — Hồ sơ không thuộc job của Employer này:
```json
{
  "status": 403,
  "message": "Bạn không có quyền cập nhật hồ sơ này",
  "data": null
}
```

---

## FR-09 — Tải lên CV

- **Method:** `POST`
- **Endpoint:** `/api/v1/candidate/cv/upload`
- **Phân quyền:** `CANDIDATE`
- **Content-Type:** `multipart/form-data`
- **Mô tả:** Upload file CV định dạng PDF lên Cloudinary. URL trả về được lưu vào DB và dùng để đính kèm khi nộp hồ sơ.

### Request

```
POST /api/v1/candidate/cv/upload
Authorization: Bearer <candidateToken>
Content-Type: multipart/form-data

file: [binary PDF file]
```

| Field  | Type          | Bắt buộc | Ràng buộc                        |
|--------|---------------|----------|----------------------------------|
| `file` | MultipartFile | ✅        | Định dạng PDF, tối đa **15MB**   |

### Response — Thành công `200 OK`

```json
{
  "status": 200,
  "message": "Tải lên CV thành công",
  "data": {
    "cvUrl":    "https://res.cloudinary.com/your-cloud/raw/upload/job_portal/cv/cv_1.pdf",
    "publicId": "job_portal/cv/2_1700000000",
    "email":    "nguyenvana@gmail.com",
    "fullName": "Nguyen Van A"
  }
}
```

### Response — Thất bại

- **`400 Bad Request`** — File không phải PDF:
```json
{
  "status": 400,
  "message": "Chỉ chấp nhận file định dạng PDF",
  "data": null
}
```

- **`400 Bad Request`** — File vượt quá 15MB:
```json
{
  "status": 400,
  "message": "Dung lượng file không được vượt quá 15MB",
  "data": null
}
```

- **`503 Service Unavailable`** — Lỗi kết nối Cloudinary:
```json
{
  "status": 503,
  "message": "Lỗi kết nối dịch vụ lưu trữ đám mây: ...",
  "data": null
}
```

---

## FR-10 — Đổi mật khẩu / Quên mật khẩu

---

### 10.1 — Đổi mật khẩu (khi đã đăng nhập)

- **Method:** `PUT`
- **Endpoint:** `/api/v1/candidate/change-password`
- **Phân quyền:** `CANDIDATE`
- **Mô tả:** Đổi mật khẩu khi người dùng còn nhớ mật khẩu cũ và đang đăng nhập.

### Request Body

```json
{
  "currentPassword": "matkhau123",
  "newPassword":     "matkhauMoi456",
  "confirmPassword": "matkhauMoi456"
}
```

| Field             | Type   | Bắt buộc | Ràng buộc                      |
|-------------------|--------|----------|--------------------------------|
| `currentPassword` | String | ✅        | Không được để trống            |
| `newPassword`     | String | ✅        | Tối thiểu 6 ký tự              |
| `confirmPassword` | String | ✅        | Phải khớp với `newPassword`    |

### Response — Thành công `200 OK`

```json
{
  "status": 200,
  "message": "Đổi mật khẩu thành công",
  "data": null
}
```

### Response — Thất bại

- **`400 Bad Request`** — Mật khẩu hiện tại sai:
```json
{
  "status": 400,
  "message": "Mật khẩu hiện tại không chính xác",
  "data": null
}
```

- **`400 Bad Request`** — Mật khẩu mới và xác nhận không khớp:
```json
{
  "status": 400,
  "message": "Mật khẩu mới và xác nhận mật khẩu không khớp",
  "data": null
}
```

- **`400 Bad Request`** — Mật khẩu mới trùng mật khẩu cũ:
```json
{
  "status": 400,
  "message": "Mật khẩu mới không được trùng mật khẩu hiện tại",
  "data": null
}
```

---

### 10.2 — Quên mật khẩu — Bước 1: Gửi OTP

- **Method:** `POST`
- **Endpoint:** `/api/v1/auth/forgot-password`
- **Phân quyền:** Public
- **Mô tả:** Gửi mã OTP 6 số về email. OTP có hiệu lực trong **5 phút**. Luôn trả về thông báo chung để tránh lộ thông tin email tồn tại hay không.

### Request Body

```json
{
  "email": "nguyenvana@gmail.com"
}
```

| Field   | Type   | Bắt buộc | Ràng buộc            |
|---------|--------|----------|----------------------|
| `email` | String | ✅        | Đúng định dạng email |

### Response — Thành công `200 OK`

```json
{
  "status": 200,
  "message": "Nếu email tồn tại trong hệ thống, mã OTP sẽ được gửi đến hộp thư của bạn",
  "data": null
}
```

_Lưu ý: Luôn trả về 200 dù email có tồn tại hay không — đây là thiết kế bảo mật có chủ ý._

---

### 10.3 — Quên mật khẩu — Bước 2: Xác nhận OTP & Đặt lại mật khẩu

- **Method:** `POST`
- **Endpoint:** `/api/v1/auth/reset-password`
- **Phân quyền:** Public
- **Mô tả:** Xác nhận mã OTP và đặt lại mật khẩu mới. OTP chỉ được dùng **một lần**.

### Request Body

```json
{
  "email":           "nguyenvana@gmail.com",
  "otpCode":         "123456",
  "newPassword":     "matkhauMoi789",
  "confirmPassword": "matkhauMoi789"
}
```

| Field             | Type   | Bắt buộc | Ràng buộc                      |
|-------------------|--------|----------|--------------------------------|
| `email`           | String | ✅        | Đúng định dạng email           |
| `otpCode`         | String | ✅        | Mã 6 số nhận qua email         |
| `newPassword`     | String | ✅        | Tối thiểu 6 ký tự              |
| `confirmPassword` | String | ✅        | Phải khớp với `newPassword`    |

### Response — Thành công `200 OK`

```json
{
  "status": 200,
  "message": "Đặt lại mật khẩu thành công",
  "data": null
}
```

### Response — Thất bại

- **`400 Bad Request`** — OTP không tồn tại hoặc đã sử dụng:
```json
{
  "status": 400,
  "message": "Mã OTP không hợp lệ hoặc đã được sử dụng",
  "data": null
}
```

- **`400 Bad Request`** — OTP sai mã:
```json
{
  "status": 400,
  "message": "Mã OTP không chính xác",
  "data": null
}
```

- **`400 Bad Request`** — OTP hết hạn:
```json
{
  "status": 400,
  "message": "Mã OTP đã hết hạn, vui lòng yêu cầu mã mới",
  "data": null
}
```

- **`400 Bad Request`** — Mật khẩu mới và xác nhận không khớp:
```json
{
  "status": 400,
  "message": "Mật khẩu mới và xác nhận mật khẩu không khớp",
  "data": null
}
```

---

## Tổng hợp tất cả Endpoints

| STT | Method   | Endpoint                                      | Role      | FR    | HTTP Success   |
|-----|----------|-----------------------------------------------|-----------|-------|----------------|
| 1   | `POST`   | `/api/v1/auth/login`                          | Public    | FR-01 | `200 OK`       |
| 2   | `POST`   | `/api/v1/auth/refresh`                        | Public    | FR-02 | `200 OK`       |
| 3   | `POST`   | `/api/v1/auth/logout`                         | Any Auth  | FR-03 | `200 OK`       |
| 4   | `POST`   | `/api/v1/auth/register`                       | Public    | FR-04 | `201 Created`  |
| 5   | `GET`    | `/api/v1/admin/users`                         | ADMIN     | FR-05 | `200 OK`       |
| 6   | `GET`    | `/api/v1/admin/users/{id}`                    | ADMIN     | FR-05 | `200 OK`       |
| 7   | `PUT`    | `/api/v1/admin/users/{id}`                    | ADMIN     | FR-05 | `200 OK`       |
| 8   | `DELETE` | `/api/v1/admin/users/{id}`                    | ADMIN     | FR-05 | `200 OK`       |
| 9   | `GET`    | `/api/v1/admin/jobs`                          | ADMIN     | FR-05 | `200 OK`       |
| 10  | `PUT`    | `/api/v1/admin/jobs/{id}/status`              | ADMIN     | FR-05 | `200 OK`       |
| 11  | `POST`   | `/api/v1/employer/jobs`                       | EMPLOYER  | FR-06 | `201 Created`  |
| 12  | `GET`    | `/api/v1/employer/jobs`                       | EMPLOYER  | FR-06 | `200 OK`       |
| 13  | `PUT`    | `/api/v1/employer/jobs/{id}`                  | EMPLOYER  | FR-06 | `200 OK`       |
| 14  | `PATCH`  | `/api/v1/employer/jobs/{id}/submit`           | EMPLOYER  | FR-06 | `200 OK`       |
| 15  | `PATCH`  | `/api/v1/employer/jobs/{id}/close`            | EMPLOYER  | FR-06 | `200 OK`       |
| 16  | `DELETE` | `/api/v1/employer/jobs/{id}`                  | EMPLOYER  | FR-06 | `200 OK`       |
| 17  | `GET`    | `/api/v1/candidate/jobs`                      | CANDIDATE | FR-07 | `200 OK`       |
| 18  | `POST`   | `/api/v1/candidate/applications`              | CANDIDATE | FR-07 | `201 Created`  |
| 19  | `GET`    | `/api/v1/candidate/applications`              | CANDIDATE | FR-07 | `200 OK`       |
| 20  | `GET`    | `/api/v1/candidate/applications/{id}`         | CANDIDATE | FR-07 | `200 OK`       |
| 21  | `GET`    | `/api/v1/employer/applications`               | EMPLOYER  | FR-08 | `200 OK`       |
| 22  | `GET`    | `/api/v1/employer/jobs/{jobId}/applications`  | EMPLOYER  | FR-08 | `200 OK`       |
| 23  | `PATCH`  | `/api/v1/employer/applications/{id}/status`   | EMPLOYER  | FR-08 | `200 OK`       |
| 24  | `POST`   | `/api/v1/candidate/cv/upload`                 | CANDIDATE | FR-09 | `200 OK`       |
| 25  | `PUT`    | `/api/v1/candidate/change-password`           | CANDIDATE | FR-10 | `200 OK`       |
| 26  | `POST`   | `/api/v1/auth/forgot-password`                | Public    | FR-10 | `200 OK`       |
| 27  | `POST`   | `/api/v1/auth/reset-password`                 | Public    | FR-10 | `200 OK`       |