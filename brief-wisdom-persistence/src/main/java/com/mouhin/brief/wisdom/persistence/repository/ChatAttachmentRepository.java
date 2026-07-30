package com.mouhin.brief.wisdom.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mouhin.brief.wisdom.persistence.mapper.ChatAttachmentMapper;
import com.mouhin.brief.wisdom.persistence.model.ChatAttachment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 聊天附件数据访问层
 *
 * @author Brief-Wisdom
 * @date 2026-07-25
 */
@Repository
@RequiredArgsConstructor
public class ChatAttachmentRepository {

    private final ChatAttachmentMapper chatAttachmentMapper;

    /**
     * 保存附件
     *
     * @param attachment 附件实体
     */
    public void save(ChatAttachment attachment) {
        chatAttachmentMapper.insert(attachment);
    }

    /**
     * 根据 ID 查询附件
     *
     * @param id 附件 ID
     * @return 附件实体，不存在返回 null
     */
    public ChatAttachment findById(Long id) {
        return chatAttachmentMapper.selectById(id);
    }

    /**
     * 批量根据 ID 查询附件
     *
     * @param ids 附件 ID 列表
     * @return 附件列表
     */
    public List<ChatAttachment> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return chatAttachmentMapper.selectList(
                new LambdaQueryWrapper<ChatAttachment>()
                        .in(ChatAttachment::getId, ids)
        );
    }

    /**
     * 查询指定消息的附件列表（按 ID 升序）
     *
     * @param messageId 消息 ID
     * @return 附件列表
     */
    public List<ChatAttachment> findByMessageId(Long messageId) {
        return chatAttachmentMapper.selectList(
                new LambdaQueryWrapper<ChatAttachment>()
                        .eq(ChatAttachment::getMessageId, messageId)
                        .orderByAsc(ChatAttachment::getId)
        );
    }

    /**
     * 批量查询多条消息的附件列表（用于历史消息回填）
     *
     * @param messageIds 消息 ID 列表
     * @return 附件列表
     */
    public List<ChatAttachment> findByMessageIds(List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return List.of();
        }
        return chatAttachmentMapper.selectList(
                new LambdaQueryWrapper<ChatAttachment>()
                        .in(ChatAttachment::getMessageId, messageIds)
                        .orderByAsc(ChatAttachment::getId)
        );
    }

    /**
     * 更新附件（回填 session_id / message_id 等）
     *
     * @param attachment 附件实体（必须包含 ID）
     */
    public void update(ChatAttachment attachment) {
        chatAttachmentMapper.updateById(attachment);
    }
}
