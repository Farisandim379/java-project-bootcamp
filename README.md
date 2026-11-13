# Product Platform — Final Project

Platform produk end-to-end berbasis Spring Boot Microservices: Service Registry (Eureka), API Gateway, Authentication (JWT & BCrypt), Product CRUD & Business Rules, serta Otomasi Impor DummyJSON menggunakan Spring Cloud OpenFeign dan Scheduler.

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

## 🚀 Cara Menjalankan Project

### 1. Prasyarat
* Java 21 (JDK 21)
* Maven 3.9+
* Docker & Docker Compose

### 2. Menjalankan Docker Container
Jalankan dependensi database dan middleware melalui docker compose:
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

## 📋 Catatan Keputusan (Architecture Decision Records)

1. **Masa Berlaku JWT Token (W1.3):**
   * Token JWT diatur dengan masa aktif (expiration) selama **24 jam (86.400.000 ms)**. Durasi ini dipilih sebagai titik tengah yang optimal untuk pengujian dan demo tanpa perlu sering login ulang, namun tetap memiliki batas kedaluwarsa untuk memitigasi risiko pencurian token.
2. **Pencegahan Data Duplikat pada Impor DummyJSON (W5.5):**
   * Setiap produk dari DummyJSON memiliki ID referensi atau nama unik. Pengecekan duplikasi dilakukan di layer `product-service` sebelum entity disimpan ke database dengan memeriksa kecocokan nama dan brand, sehingga eksekusi impor berulang tidak menimbulkan redundansi data.
3. **Penolakan Rating < 3 di Layer Service (W5.4):**
   * Aturan bisnis penolakan produk dengan rating < 3 diimplementasikan di `product-service` (bukan di scheduler), untuk memastikan integritas data tetap terjaga konsisten baik produk dimasukkan lewat scheduler maupun lewat endpoint lainnya.
4. **Resiliency Feign & Isolasi Database Scheduler (W5.3 & W5.6):**
   * `scheduler-service` dirancang stateless tanpa akses langsung ke PostgreSQL untuk menjaga prinsip loose coupling antar service. Pemanggilan ke DummyJSON dan `product-service` di-handle dengan defensive exception handling agar scheduler tetap tangguh dan tidak crash saat service target mengalami gangguan sementara.
