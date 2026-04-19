package com.ccj.campus.chat.service.impl;

import com.ccj.campus.chat.common.BusinessException;
import com.ccj.campus.chat.common.ResultCode;
import com.ccj.campus.chat.config.MinioConfig;
import com.ccj.campus.chat.service.FileService;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 文件存储实现。对齐论文 4.1 文件存储层 + 3.2 即时通讯模块：
 *   - 图片/视频/文件统一走 MinIO 中转
 *   - 按日期分目录存储，避免单目录文件过多
 *   - 文件名用 UUID 防止重名和路径遍历
 *   - 支持预签名 URL，聊天界面直接加载，不走后端代理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /** 服务启动时确保 bucket 存在 */
    @PostConstruct
    public void init() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(minioConfig.getBucket()).build());
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(minioConfig.getBucket()).build());
                log.info("MinIO bucket '{}' created", minioConfig.getBucket());
            }
        } catch (Exception e) {
            log.warn("MinIO bucket init failed: {}", e.getMessage());
        }
    }

    @Override
    public String upload(MultipartFile file, String directory) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "文件不能为空");
        }

        String objectName = buildObjectName(directory, file.getOriginalFilename());
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getBucket())
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "文件上传失败");
        }

        // 返回相对路径，前端拼接 endpoint 或走预签名
        return objectName;
    }

    @Override
    public List<String> uploadBatch(List<MultipartFile> files, String directory) {
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            urls.add(upload(file, directory));
        }
        return urls;
    }

    @Override
    public InputStream download(String filePath) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(minioConfig.getBucket())
                    .object(filePath)
                    .build());
        } catch (Exception e) {
            log.error("文件下载失败: path={}, err={}", filePath, e.getMessage());
            throw new BusinessException(ResultCode.NOT_FOUND, "文件不存在");
        }
    }

    @Override
    public void delete(String filePath) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioConfig.getBucket())
                    .object(filePath)
                    .build());
        } catch (Exception e) {
            log.warn("文件删除失败: path={}, err={}", filePath, e.getMessage());
        }
    }

    @Override
    public String getPresignedUrl(String filePath, int expireMinutes) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(minioConfig.getBucket())
                    .object(filePath)
                    .method(Method.GET)
                    .expiry(expireMinutes, TimeUnit.MINUTES)
                    .build());
        } catch (Exception e) {
            log.error("获取预签名 URL 失败: {}", e.getMessage());
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "获取文件链接失败");
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 构建对象路径：{directory}/{yyyy/MM/dd}/{uuid}.{ext}
     * UUID 防止重名、日期分目录防止单目录爆炸
     */
    private String buildObjectName(String directory, String originalFilename) {
        String datePath = LocalDate.now().format(DATE_FMT);
        String ext = extractExtension(originalFilename);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String dir = (directory == null || directory.isEmpty()) ? "default" : directory;
        return dir + "/" + datePath + "/" + uuid + ext;
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}