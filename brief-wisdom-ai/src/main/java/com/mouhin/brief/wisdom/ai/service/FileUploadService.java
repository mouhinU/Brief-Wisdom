package com.mouhin.brief.wisdom.ai.service;

import com.mouhin.brief.wisdom.ai.storage.StorageService;
import com.mouhin.brief.wisdom.ai.storage.StoredObject;
import com.mouhin.brief.wisdom.common.ai.AttachmentDTO;
import com.mouhin.brief.wisdom.exception.AIException;
import com.mouhin.brief.wisdom.persistence.model.ChatAttachment;
import com.mouhin.brief.wisdom.persistence.repository.ChatAttachmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * 文件上传服务
 * <p>
 * 处理多模态文件上传：校验文件类型与大小，调用 {@link StorageService} 存储文件，
 * 并将附件元信息登记到 chat_attachment 表（发送消息时再回填 session_id / message_id）。
 *
 * @author Brief-Wisdom
 * @date 2026-07-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {

    /**
     * 允许的图片扩展名
     */
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");

    /**
     * 允许的文档扩展名
     */
    private static final Set<String> DOCUMENT_EXTENSIONS = Set.of("pdf", "doc", "docx", "txt", "md");

    /**
     * 允许的音频扩展名
     */
    private static final Set<String> AUDIO_EXTENSIONS = Set.of("wav", "mp3", "m4a", "ogg");

    /**
     * 附件分类：图片
     */
    private static final String CATEGORY_IMAGE = "image";

    /**
     * 附件分类：文档
     */
    private static final String CATEGORY_DOCUMENT = "document";

    /**
     * 附件分类：音频
     */
    private static final String CATEGORY_AUDIO = "audio";

    private final StorageService storageService;
    private final ChatAttachmentRepository chatAttachmentRepository;

    /**
     * 上传文件并登记附件元信息
     *
     * @param file   上传的文件
     * @param userId 上传者用户 ID
     * @return 附件传输对象
     */
    public AttachmentDTO upload(MultipartFile file, String userId) {
        if (file == null || file.isEmpty()) {
            throw new AIException("上传文件不能为空");
        }
        String fileName = file.getOriginalFilename();
        String extension = extractExtension(fileName);
        String category = resolveCategory(extension);

        StoredObject stored;
        try (InputStream input = file.getInputStream()) {
            stored = storageService.upload(input, fileName, file.getContentType(), file.getSize());
        } catch (IOException e) {
            log.error("[上传] 读取上传文件流失败 - fileName: {}, error: {}", fileName, e.getMessage(), e);
            throw new AIException("文件上传失败");
        }

        ChatAttachment attachment = new ChatAttachment();
        attachment.setUserId(userId);
        attachment.setFileName(fileName);
        attachment.setFileKey(stored.getKey());
        attachment.setFileUrl(stored.getUrl());
        attachment.setMediaType(file.getContentType());
        attachment.setFileSize(stored.getSize());
        attachment.setCategory(category);
        chatAttachmentRepository.save(attachment);

        log.info("[上传] 文件上传成功 - userId: {}, attachmentId: {}, category: {}, fileName: {}",
                userId, attachment.getId(), category, fileName);
        return toDTO(attachment);
    }

    /**
     * 根据扩展名解析附件分类，不支持的类型抛出异常
     *
     * @param extension 文件扩展名（小写，不含点号）
     * @return 附件分类
     */
    private String resolveCategory(String extension) {
        if (IMAGE_EXTENSIONS.contains(extension)) {
            return CATEGORY_IMAGE;
        }
        if (DOCUMENT_EXTENSIONS.contains(extension)) {
            return CATEGORY_DOCUMENT;
        }
        if (AUDIO_EXTENSIONS.contains(extension)) {
            return CATEGORY_AUDIO;
        }
        throw new AIException("不支持的文件类型: " + extension);
    }

    /**
     * 提取文件扩展名（小写，不含点号），无扩展名时返回空字符串
     *
     * @param fileName 原始文件名
     * @return 扩展名
     */
    private String extractExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase();
    }

    /**
     * 实体转传输对象
     *
     * @param attachment 附件实体
     * @return 附件传输对象
     */
    private AttachmentDTO toDTO(ChatAttachment attachment) {
        return new AttachmentDTO(
                attachment.getId(),
                attachment.getFileName(),
                attachment.getFileUrl(),
                attachment.getMediaType(),
                attachment.getFileSize(),
                attachment.getCategory()
        );
    }
}
