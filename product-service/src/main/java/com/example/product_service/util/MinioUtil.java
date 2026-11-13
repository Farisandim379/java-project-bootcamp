package com.example.product_service.util;

import com.example.product_service.exception.FileStorageException;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioUtil {

    private final MinioClient minioClient;

    @Value("${minio.bucket.name}")
    private String bucketName;

    @Value("${minio.presigned-expiry-minutes:10}")
    private int presignedExpiryMinutes;

    private void pastikanBucketAda() throws Exception {
        boolean ada = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!ada) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            log.info("[MinIO] Bucket {} dibuat otomatis", bucketName);
        }
    }

    public void upload(String namaFile, byte[] isi, String contentType) {
        try {
            pastikanBucketAda();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(namaFile)
                    .stream(new ByteArrayInputStream(isi), isi.length, -1)
                    .contentType(contentType)
                    .build());
            log.info("[MinIO] Upload {} ke bucket {} berhasil", namaFile, bucketName);
        } catch (Exception e) {
            throw new FileStorageException("Gagal upload file ke MinIO: " + e.getMessage());
        }
    }

    public String linkDownload(String namaFile) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(namaFile)
                    .expiry(presignedExpiryMinutes, TimeUnit.MINUTES)
                    .build());
        } catch (Exception e) {
            throw new FileStorageException("Gagal membuat presigned link MinIO: " + e.getMessage());
        }
    }
}
