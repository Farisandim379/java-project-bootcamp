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
## 🌟 Fitur Tambahan

Beberapa fitur tambahan yang sudah diimplementasikan pada aplikasi:

- **Redis Cache:** Menyimpan detail produk di Redis menggunakan key `product-platform::product::{id}` dengan TTL 10 menit. Jika data tersedia di cache, aplikasi tidak perlu mengambilnya kembali dari database. Cache otomatis dihapus ketika produk di-update atau dihapus menggunakan `@CacheEvict`.

- **Kafka Event:** Setiap produk baru berhasil dibuat, aplikasi mengirimkan event ke topic `product-created`. Event tersebut diterima oleh `ProductEventConsumer` dan ringkasan datanya dicatat melalui log.

- **Laporan CSV ke MinIO:** Endpoint `POST /product/report` digunakan untuk membuat laporan produk dalam format CSV berdasarkan periode tertentu. File kemudian diunggah ke MinIO dan aplikasi mengembalikan presigned URL yang dapat digunakan untuk mengunduh file selama 10 menit.

- **Feign DummyJSON Write Simulation:** Operasi create, update, dan delete produk juga diteruskan ke DummyJSON melalui Feign Client menggunakan endpoint `/products/add` dan `/products/{id}`. Kegagalan request ke DummyJSON tidak memengaruhi perubahan data di database lokal.

- **AOP Logging Durasi:** Menggunakan `LoggingAspect` untuk mencatat aktivitas method pada layer service, meliputi nama method, argumen (dengan masking data sensitif), durasi eksekusi, dan exception sebelum ditangani oleh Global Exception Handler.

---

## 📋 Catatan Implementasi dan Keputusan Teknis

Bagian ini menjelaskan beberapa keputusan yang digunakan selama pengembangan, termasuk alasan pemilihan pendekatan dan penanganan kondisi tertentu.

### 1. Masa Berlaku JWT Token (W1.3)

JWT diatur agar berlaku selama **24 jam (86.400.000 ms)**. Durasi ini digunakan agar proses pengujian dan demo tidak terganggu karena harus login berulang kali. Meskipun demikian, token tetap memiliki waktu kedaluwarsa sehingga tidak berlaku selamanya.

### 2. Pencegahan Data Duplikat saat Impor DummyJSON (W5.5)

Setiap produk yang diambil dari DummyJSON memiliki `dummyJsonId` sebagai ID referensi. Sebelum menyimpan produk, `product-service` akan memeriksa apakah produk dengan `dummyJsonId` yang sama sudah ada. Pengecekan juga dilakukan berdasarkan kombinasi `nama` dan `brand` untuk menghindari data yang sama masuk lebih dari sekali.

Dengan pengecekan ini, proses impor dapat dijalankan berulang kali tanpa membuat data produk menjadi duplikat.

### 3. Validasi Rating Produk (W5.4)

Produk dengan rating di bawah 3 tidak akan disimpan ke database. Validasi ini ditempatkan di layer `product-service`, bukan di scheduler, agar aturan tersebut tetap berlaku untuk semua jalur pemasukan data.

Pada pengujian menggunakan 16 data smartphone dari DummyJSON, terdapat 2 produk yang ditolak karena rating-nya di bawah 3, yaitu `iPhone 5s` (2.83) dan `iPhone X` (2.51). Dengan demikian, **14 dari 16 produk berhasil disimpan**.

### 4. Penanganan Error pada Scheduler (W5.3 & W5.6)

`Scheduler-service` tidak memiliki akses langsung ke PostgreSQL. Semua komunikasi dengan `product-service` dilakukan melalui Feign Client agar tanggung jawab masing-masing service tetap terpisah.

Pemanggilan API juga dilengkapi dengan exception handling. Jadi, ketika DummyJSON atau `product-service` tidak merespons karena timeout maupun masalah jaringan, scheduler tetap dapat berjalan dan tidak langsung berhenti karena error tersebut.

### 5. Penanganan Kegagalan Sinkronisasi DummyJSON (B4.2)

Request ke DummyJSON saat create, update, dan delete hanya digunakan untuk simulasi komunikasi dengan layanan eksternal. Data utama tetap disimpan dan dikelola di database lokal.

Jika request ke DummyJSON gagal, operasi pada database lokal tetap dilanjutkan dan tidak dibatalkan. Dengan begitu, gangguan pada layanan eksternal tidak ikut mengganggu proses utama aplikasi.
