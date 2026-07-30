package com.mouhin.brief.wisdom.ai.service;

import com.mouhin.brief.wisdom.common.knowledge.KnowledgeBaseDTO;
import com.mouhin.brief.wisdom.common.team.KbShareDTO;
import com.mouhin.brief.wisdom.common.team.TeamDTO;
import com.mouhin.brief.wisdom.common.team.TeamMemberDTO;

import java.util.List;

/**
 * 团队管理服务接口
 * <p>
 * 提供团队 CRUD、成员管理、团队知识库管理和知识库共享授权能力。
 *
 * @author Brief-Wisdom
 * @date 2026-07-25
 */
public interface TeamService {

    // ===== 团队 CRUD =====

    /**
     * 创建团队（创建者自动成为 owner）
     */
    TeamDTO createTeam(String name, String description, String avatar, String creatorUserId);

    /**
     * 获取团队详情（含成员列表）
     */
    TeamDTO getTeam(Long teamId, String currentUserId);

    /**
     * 获取我加入的所有团队
     */
    List<TeamDTO> listMyTeams(String userId);

    /**
     * 更新团队信息（仅 owner/admin）
     */
    TeamDTO updateTeam(Long teamId, String name, String description, String avatar, String currentUserId);

    /**
     * 删除团队（仅 owner）
     */
    void deleteTeam(Long teamId, String currentUserId);

    // ===== 成员管理 =====

    /**
     * 添加成员（仅 owner/admin）
     */
    TeamMemberDTO addMember(Long teamId, String userId, String role, String operatorUserId);

    /**
     * 移除成员（仅 owner/admin，不可移除 owner）
     */
    void removeMember(Long teamId, String targetUserId, String operatorUserId);

    /**
     * 变更成员角色（仅 owner）
     */
    void changeMemberRole(Long teamId, String targetUserId, String newRole, String operatorUserId);

    /**
     * 获取团队成员列表
     */
    List<TeamMemberDTO> listMembers(Long teamId, String currentUserId);

    // ===== 团队知识库 =====

    /**
     * 创建团队知识库（仅 owner/admin）
     */
    KnowledgeBaseDTO createTeamKnowledgeBase(Long teamId, String name, String description, String icon, String creatorUserId);

    /**
     * 获取团队的知识库列表
     */
    List<KnowledgeBaseDTO> listTeamKnowledgeBases(Long teamId, String currentUserId);

    // ===== 知识库共享 =====

    /**
     * 共享知识库给团队或个人（仅 KB 所属团队的 owner/admin）
     */
    KbShareDTO shareKnowledgeBase(Long kbId, String targetType, String targetId, String operatorUserId);

    /**
     * 撤销共享
     */
    void revokeShare(Long shareId, String operatorUserId);

    /**
     * 获取知识库的共享列表
     */
    List<KbShareDTO> listShares(Long kbId, String currentUserId);

    /**
     * 获取共享给我的知识库列表（我所在团队被授权的 + 直接授权给我个人的）
     */
    List<KnowledgeBaseDTO> listSharedWithMe(String userId);

    // ===== 权限校验 =====

    /**
     * 判断用户是否有权查看指定知识库
     * <p>
     * 可见条件：公开(is_public=1) OR 个人所有 OR 所属团队成员 OR 被共享（团队级/个人级）
     */
    boolean canViewKnowledgeBase(Long kbId, String userId);

    /**
     * 获取用户可见的所有知识库 ID 集合
     * <p>
     * 包含：公开KB + 个人所有KB + 所属团队KB + 被共享KB（团队级/个人级）
     *
     * @param userId 用户ID
     * @return 可见知识库 ID 集合（Set，用于高效 contains 判断）
     */
    java.util.Set<Long> getVisibleKbIds(String userId);
}
