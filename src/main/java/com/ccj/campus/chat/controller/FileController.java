package com.ccj.campus.chat.controller;

import com.ccj.campus.chat.common.R;
import com.ccj.campus.chat.service.FileService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文件上传下载接口。对齐论文 3.2 + 4.1：
 *   "图片与文件经由文件存储服务中转，确保聊天界面加载速度不受大文件影响"
 *
 * 前端发消息时：
 *   1. 先调 /file/upload 上传文件，拿到 filePath
 *   2. 再通过 STOMP send 消息，type=2(图片)/3(文件)/4(语音)，
 *      extInfo 里带上 filePath、fileName、fileSize 等
 *   3. 接收方展示时，用 filePath 调 /file/presign 拿预签名 URL 直接加载
 */
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    // ==================== 聊天文件（图片/视频/语音/文件） ====================

    /**
     * 上传聊天文件（图片/视频/语音/普通文件）
     * 前端 uni.uploadFile 的 name 参数设为 "file"
     */
    @PostMapping("/upload")
    public R<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                          @RequestParam(value = "type", defaultValue = "chat") String type) {
        // type 作为目录前缀：chat / avatar / leave-attachment / checkin 等
        String filePath = fileService.upload(file, type);

        // 同时返回预签名 URL，前端可直接用于预览
        String presignedUrl = fileService.getPresignedUrl(filePath, 60);

        Map<String, Object> result = new HashMap<>();
        result.put("filePath", filePath);
        result.put("url", presignedUrl);
        result.put("fileName", file.getOriginalFilename());
        result.put("fileSize", file.getSize());
        result.put("contentType", file.getContentType());
        return R.ok(result);
    }

    /**
     * 批量上传（一次选多张图片）
     */
    @PostMapping("/upload/batch")
    public R<List<Map<String, Object>>> uploadBatch(@RequestParam("files") List<MultipartFile> files,
                                                      @RequestParam(value = "type", defaultValue = "chat") String type) {
        List<Map<String, Object>> results = files.stream().map(file -> {
            String filePath = fileService.upload(file, type);
            String presignedUrl = fileService.getPresignedUrl(filePath, 60);
            Map<String, Object> item = new HashMap<>();
            item.put("filePath", filePath);
            item.put("url", presignedUrl);
            item.put("fileName", file.getOriginalFilename());
            item.put("fileSize", file.getSize());
            item.put("contentType", file.getContentType());
            return item;
        }).collect(Collectors.toList());
        return R.ok(results);
    }

    // ==================== 头像上传 ====================

    /**
     * 上传头像（单独接口，方便限制大小和格式）
     */
    @PostMapping("/avatar")
    public R<Map<String, Object>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        // 头像限制 2MB
        if (file.getSize() > 2 * 1024 * 1024) {
            return R.fail(com.ccj.campus.chat.common.ResultCode.BAD_REQUEST, "头像不能超过 2MB");
        }
        String filePath = fileService.upload(file, "avatar");
        String presignedUrl = fileService.getPresignedUrl(filePath, 1440); // 头像 URL 有效期 24 小时

        Map<String, Object> result = new HashMap<>();
        result.put("filePath", filePath);
        result.put("url", presignedUrl);
        return R.ok(result);
    }

    // ==================== 获取预签名 URL ====================

    /**
     * 获取预签名访问 URL（前端展示聊天图片/文件时调用）
     * 返回的 URL 可直接用于 <image :src="url" /> 或浏览器下载
     */
    @GetMapping("/presign")
    public R<String> presign(@RequestParam String filePath,
                              @RequestParam(defaultValue = "60") int expireMinutes) {
        return R.ok(fileService.getPresignedUrl(filePath, expireMinutes));
    }

    // ==================== 文件下载（代理模式，不走预签名） ====================

    /**
     * 文件下载/预览（后端代理，适用于需要鉴权的场景）
     */
    @GetMapping("/download")
    public ResponseEntity<byte[]> download(@RequestParam String filePath,
                                            @RequestParam(required = false) String fileName) {
        try (InputStream is = fileService.download(filePath)) {
            byte[] bytes = is.readAllBytes();

            String downloadName = (fileName != null && !fileName.isEmpty())
                    ? fileName
                    : filePath.substring(filePath.lastIndexOf("/") + 1);
            String encoded = URLEncoder.encode(downloadName, StandardCharsets.UTF_8);

            // 根据文件类型决定是预览还是下载
            String contentType = guessContentType(filePath);
            boolean isPreviewable = contentType.startsWith("image/") || contentType.equals("application/pdf");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            if (!isPreviewable) {
                headers.set(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encoded + "\"");
            }

            return ResponseEntity.ok().headers(headers).body(bytes);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ==================== 内部工具 ====================

    private String guessContentType(String filePath) {
        String lower = filePath.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png"))  return "image/png";
        if (lower.endsWith(".gif"))  return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".mp4"))  return "video/mp4";
        if (lower.endsWith(".mp3"))  return "audio/mpeg";
        if (lower.endsWith(".wav"))  return "audio/wav";
        if (lower.endsWith(".pdf"))  return "application/pdf";
        if (lower.endsWith(".doc") || lower.endsWith(".docx")) return "application/msword";
        if (lower.endsWith(".xls") || lower.endsWith(".xlsx")) return "application/vnd.ms-excel";
        return "application/octet-stream";
    }
}