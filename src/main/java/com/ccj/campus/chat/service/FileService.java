package com.ccj.campus.chat.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * 文件存储业务接口。对齐论文 3.2：
 *   "图片与文件经由文件存储服务中转，确保聊天界面加载速度不受大文件影响"
 */
public interface FileService {

    /**
     * 上传单个文件
     * @return 文件访问 URL
     */
    String upload(MultipartFile file, String directory);

    /**
     * 批量上传
     * @return 文件访问 URL 列表
     */
    List<String> uploadBatch(List<MultipartFile> files, String directory);

    /**
     * 获取文件流（下载/预览）
     */
    InputStream download(String filePath);

    /**
     * 删除文件
     */
    void delete(String filePath);

    /**
     * 获取文件的预签名访问 URL（有效期内可直接访问，无需鉴权）
     */
    String getPresignedUrl(String filePath, int expireMinutes);
}