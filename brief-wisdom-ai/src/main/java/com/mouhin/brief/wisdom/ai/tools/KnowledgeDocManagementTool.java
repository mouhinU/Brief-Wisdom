package com.mouhin.brief.wisdom.ai.tools;

import com.mouhin.brief.wisdom.ai.service.KnowledgeService;
import com.mouhin.brief.wisdom.common.knowledge.KnowledgeBaseDTO;
import com.mouhin.brief.wisdom.common.knowledge.KnowledgeDocumentBO;
import com.mouhin.brief.wisdom.common.knowledge.KnowledgeDocumentDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 知识库文档管理工具
 * <p>
 * 允许 AI 助手在对话中直接管理知识库文档：创建新文档、搜索已有文档。
 * 用户可以说"把这个保存到知识库"来触发。
 *
 * @author Brief-Wisdom
 * @date 2026-07-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeDocManagementTool {

    private final KnowledgeService knowledgeService;

    /**
     * 创建新的知识文档
     *
     * @param title   文档标题
     * @param content 文档内容
     * @param baseId  目标知识库 ID
     * @return 操作结果
     */
    @Tool(description = "在知识库中创建新的文档。当用户说'把这个保存到知识库'、'创建一条知识记录'、'添加到知识库'时调用。")
    public String createKnowledgeDoc(
            @ToolParam(description = "文档标题") String title,
            @ToolParam(description = "文档内容（支持 Markdown 格式）") String content,
            @ToolParam(description = "目标知识库 ID，如果不确定可以先调用 listKnowledgeBases 查看") Long baseId) {

        log.info("[Tool] createKnowledgeDoc 被调用: title={}, baseId={}", title, baseId);

        if (title == null || title.isBlank()) {
            return "文档标题不能为空。";
        }
        if (content == null || content.isBlank()) {
            return "文档内容不能为空。";
        }
        if (baseId == null) {
            return "请指定目标知识库 ID。可以先调用 listKnowledgeBases 查看所有知识库。";
        }

        try {
            KnowledgeDocumentBO bo = new KnowledgeDocumentBO();
            bo.setBaseId(baseId);
            bo.setTitle(title);
            bo.setContent(content);
            bo.setDocType("INTERNAL");
            bo.setStatus(1);

            KnowledgeDocumentDTO created = knowledgeService.createDocument(bo);
            return "已成功创建知识文档：\n- ID: " + created.getId()
                    + "\n- 标题: " + created.getTitle()
                    + "\n- 知识库 ID: " + created.getBaseId()
                    + "\n文档已自动向量化，可用于后续的 RAG 检索。";
        } catch (Exception e) {
            log.error("[Tool] createKnowledgeDoc 执行失败: {}", e.getMessage(), e);
            return "创建知识文档失败: " + e.getMessage();
        }
    }

    /**
     * 列出所有知识库
     *
     * @return 知识库列表
     */
    @Tool(description = "列出所有知识库及其文档数量。当需要知道有哪些知识库、知识库 ID 时调用。")
    public String listKnowledgeBases() {
        log.info("[Tool] listKnowledgeBases 被调用");

        try {
            List<KnowledgeBaseDTO> bases = knowledgeService.listBases();
            if (bases.isEmpty()) {
                return "当前没有知识库。";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("知识库列表（共 ").append(bases.size()).append(" 个）：\n\n");
            for (KnowledgeBaseDTO base : bases) {
                sb.append("- ID: ").append(base.getId())
                        .append(", 名称: ").append(base.getName())
                        .append(", 文档数: ").append(base.getDocumentCount() != null ? base.getDocumentCount() : 0)
                        .append("\n");
                if (base.getDescription() != null && !base.getDescription().isBlank()) {
                    sb.append("  描述: ").append(base.getDescription()).append("\n");
                }
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("[Tool] listKnowledgeBases 执行失败: {}", e.getMessage(), e);
            return "查询知识库列表失败: " + e.getMessage();
        }
    }
}
