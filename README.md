# Product Platform — Final Project

Platform produk end-to-end berbasis Spring Boot Microservices: Service Registry (Eureka), API Gateway, Authentication (JWT & BCrypt), Product CRUD & Aturan Bisnis, serta Otomasi Impor DummyJSON menggunakan Spring Cloud OpenFeign dan Scheduler.

---

## 🏛️ Arsitektur Sistem

```
                      Client (Postman / Frontend)
                                   │
                                   ▼
                            api-gateway :8080
                    (Verifikasi JWT, lb:// Routing)
                    ┌──────────────┼──────────────┐
                    │              │              │
                    ▼              ▼              ▼
              auth-service  product-service  scheduler-service
                 :8081          :8082             :8083
                   │              ▲                 │
                   │              │ (Internal Feign)│ (External Feign)
                   │              └─────────────────┤
                   │                                ▼
                   │                            DummyJSON
                   │                           (smartphones)
                   ▼
          PostgreSQL (:5432)
```

| Service | Port | Deskripsi & Tanggung Jawab |
| :--- | :---: | :--- |
| `eureka-server` | 8761 | Service Registry & Discovery |
| `api-gateway` | 8080 | Single entry point, validasi JWT token, routing `lb://` |
| `auth-service` | 8081 | Registrasi user (BCrypt), login, dan penerbitan JWT |
| `product-service` | 8082 | CRUD produk, aturan bisnis, validasi duplikasi, caching & laporan |
| `scheduler-service`| 8083 | Sinkronisasi data awal & berkala dari DummyJSON via OpenFeign |

---

## 📑 Kontrak Endpoint (Melalui Gateway :8080)

| Method | Endpoint | Auth | Deskripsi | Kategori |
| :--- | :--- | :---: | :--- | :---: |
| `POST` | `/auth/register` | Publik | Mendaftarkan user baru | W1 |
| `POST` | `/auth/login` | Publik | Login & memperoleh JWT token | W1 |
| `POST` | `/product` | JWT | Menambah produk baru (Status: 201) | W2 |
| `GET` | `/product/{id}` | JWT | Mendapatkan detail produk (Redis Cached) | W2, B1 |
| `GET` | `/product?huruf=a` | JWT | Daftar produk dengan filter nama (case-insensitive) | W2 |
| `PUT` | `/product/{id}` | JWT | Memperbarui produk & evict cache | W2, B1 |
| `DELETE` | `/product/{id}` | JWT | Menghapus produk & evict cache | W2, B1 |
| `POST` | `/scheduler/import` | JWT | Memicu impor manual dari DummyJSON | W5 |
| `POST` | `/internal/products/import` | **Internal** | Menerima data batch impor (**Ditolak oleh Gateway**) | W5 |
| `POST` | `/product/report` | JWT | Generate file laporan CSV ke MinIO | B3 |

### Format Base Response
```json
{
  "status": "success",
  "message": "Produk berhasil dibuat",
  "data": { ... }
}
```
```json
{
  "status": "failed",
  "message": "Nama produk wajib diisi",
  "data": null
}
```

---

## 🚀 Cara Menjalankan Project

### 1. Prasyarat
* Java 21 (JDK 21)
* Maven 3.9+
* Docker & Docker Compose

### 2. Menjalankan Docker Container
Jalankan middleware pendukung:
```bash
# Database PostgreSQL
docker compose -f docker-compose-db.yaml up -d

# Redis (untuk Caching B1)
docker compose -f docker-compose-redis.yaml up -d

# Kafka (untuk Event-Driven B2)
docker compose -f docker-compose-kafka.yaml up -d

# MinIO (untuk Object Storage CSV B3)
docker compose -f docker-compose-minio.yaml up -d
```

### 3. Urutan Menjalankan Microservices
Wajib dijalankan secara berurutan:
1. **`eureka-server`** (tunggu hingga siap di `http://localhost:8761`)
2. **`auth-service`** (port 8081)
3. **`product-service`** (port 8082)
4. **`scheduler-service`** (port 8083)
5. **`api-gateway`** (port 8080)

---

## 🌟 Fitur Bonus yang Diimplementasikan (+15 Poin)

* **B1 — Redis Cache (+3):** Detail produk disimpan di Redis dengan key `product-platform::product::{id}` dan TTL 10 menit. Query database hanya terjadi saat *cache miss*, dan otomatis dibersihkan (`@CacheEvict`) saat update atau delete.
* **B2 — Kafka Event (+3):** Setiap produk baru menerbitkan event ke topic `product-created`. `ProductEventConsumer` membaca event dan mencatat ringkasan ke log.
* **B3 — Laporan CSV ke MinIO (+3):** Endpoint `POST /product/report` menghasilkan file CSV berisi daftar produk dalam periode tertentu, meng-upload ke bucket MinIO, dan mengembalikan presigned URL unduh berdurasi 10 menit.
* **B4 — Feign DummyJSON Write Simulation (+3):** Saat produk dibuat, diubah, atau dihapus, perubahan juga dikirim ke DummyJSON (`/products/add`, `/products/{id}`). Kegagalan DummyJSON tidak membatalkan transaksi DB lokal.
* **B5 — AOP Logging Durasi (+3):** `LoggingAspect` mencatat nama method di layer service, argumen (dengan penyembunyian/masking data sensitif), durasi eksekusi dalam milidetik, serta exception sebelum ditangani Global Exception Handler.

---

## 📋 Catatan Keputusan (Architecture Decision Records)

1. **Masa Berlaku JWT Token (W1.3):**
   * Token JWT diatur dengan masa aktif (expiration) selama **24 jam (86.400.000 ms)**. Durasi ini dipilih sebagai titik tengah yang optimal untuk kemudahan pengujian dan sesi demo tanpa perlu sering login ulang, namun tetap memiliki batas waktu kedaluwarsa untuk memitigasi risiko keamanan.
2. **Pencegahan Data Duplikat pada Impor DummyJSON (W5.5):**
   * Setiap produk dari DummyJSON memiliki ID referensi unik (`dummyJsonId`). Pengecekan duplikasi dilakukan di layer `product-service` sebelum entity disimpan ke database dengan memeriksa kecocokan `dummyJsonId` atau kombinasi `nama` dan `brand`. Eksekusi impor berulang akan mengabaikan produk yang sudah ada sehingga data tidak pernah ganda.
3. **Penolakan Rating < 3 di Layer Service (W5.4):**
   * Aturan bisnis penolakan produk dengan rating < 3 diimplementasikan di `product-service` (bukan di scheduler), untuk memastikan integritas data tetap terpusat dan konsisten baik produk dimasukkan melalui scheduler, impor manual, maupun jalur lainnya. Dari 16 produk smartphone DummyJSON, 2 produk (`iPhone 5s` rating 2.83 dan `iPhone X` rating 2.51) berhasil ditolak sehingga tepat **14 dari 16 produk tersimpan**.
4. **Resiliency Feign & Isolasi Database Scheduler (W5.3 & W5.6):**
   * `scheduler-service` dirancang stateless tanpa akses langsung ke PostgreSQL untuk mematuhi prinsip *loose coupling* dan *single responsibility*. Pemanggilan Feign dibungkus dengan defensive exception handling agar scheduler tetap tangguh dan tidak crash saat DummyJSON atau `product-service` mengalami *network timeout*.
5. **Ketahanan Kegagalan DummyJSON Write Simulation (B4.2):**
   * Sinkronisasi ke DummyJSON pada operasi create, update, dan delete bersifat simulasi eksternal. Apabila DummyJSON gagal merespon atau terjadi gangguan internet, transaksi database lokal tetap dianggap berhasil. Keputusan ini diambil agar ketersediaan (*availability*) sistem utama kita tidak terblokir oleh layanan pihak ketiga.
