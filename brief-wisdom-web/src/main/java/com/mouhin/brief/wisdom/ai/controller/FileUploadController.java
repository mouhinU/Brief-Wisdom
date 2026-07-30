package com.mouhin.brief.wisdom.ai.controller;

import com.mouhin.brief.wisdom.ai.service.FileUploadService;
import com.mouhin.brief.wisdom.common.ai.AttachmentDTO;
import com.mouhin.brief.wisdom.system.service.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传控制器
 * <p>
 * 多模态文件上传入口，采用「上传先行」模式：前端先上传文件拿到附件 ID，
 * 再随聊天消息发送 attachmentIds，规避流式 GET 接口无法携带文件二进制的问题。
 *
 * @author Brief-Wisdom
 * @date 2026-07-25
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI对话", description = "多模态文件上传相关接口")
public class FileUploadController {

    private final FileUploadService fileUploadService;
    private final UserContextHolder userContextHolder;

    /**
     * 上传多模态文件（图片/文档/音频）
     *
     * @param file 上传的文件
     * @return 附件传输对象（含附件 ID 与可访问 URL）
     */
    @Operation(summary = "上传文件", description = "上传图片/文档/音频文件，返回附件元信息，供后续聊天消息引用")
    @PostMapping("/upload")
    public AttachmentDTO upload(@RequestParam("file") MultipartFile file) {
        String userId = userContextHolder.getCurrentUserId();
        log.info("收到文件上传请求 - userId: {}, fileName: {}, size: {}",
                userId, file.getOriginalFilename(), file.getSize());
        return fileUploadService.upload(file, userId);
    }
}
