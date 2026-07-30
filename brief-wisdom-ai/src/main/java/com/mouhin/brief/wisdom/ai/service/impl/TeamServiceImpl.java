package com.mouhin.brief.wisdom.ai.service.impl;

import com.mouhin.brief.wisdom.ai.service.TeamService;
import com.mouhin.brief.wisdom.common.knowledge.KnowledgeBaseDTO;
import com.mouhin.brief.wisdom.common.team.KbShareDTO;
import com.mouhin.brief.wisdom.common.team.TeamDTO;
import com.mouhin.brief.wisdom.common.team.TeamMemberDTO;
import com.mouhin.brief.wisdom.exception.AIException;
import com.mouhin.brief.wisdom.persistence.model.KbShare;
import com.mouhin.brief.wisdom.persistence.model.KbTeam;
import com.mouhin.brief.wisdom.persistence.model.KnowledgeBase;
import com.mouhin.brief.wisdom.persistence.model.Team;
import com.mouhin.brief.wisdom.persistence.model.TeamMember;
import com.mouhin.brief.wisdom.persistence.repository.KbShareRepository;
import com.mouhin.brief.wisdom.persistence.repository.KbTeamRepository;
import com.mouhin.brief.wisdom.persistence.repository.KnowledgeBaseRepository;
import com.mouhin.brief.wisdom.persistence.repository.TeamMemberRepository;
import com.mouhin.brief.wisdom.persistence.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 团队管理服务实现
 *
 * @author Brief-Wisdom
 * @date 2026-07-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeamServiceImpl implements TeamService {

    private static final String ROLE_OWNER = "owner";
    private static final String ROLE_ADMIN = "admin";
    private static final String ROLE_MEMBER = "member";
    private static final String OWNER_TYPE_PERSONAL = "personal";
    private static final String OWNER_TYPE_TEAM = "team";
    private static final String TARGET_TYPE_TEAM = "team";
    private static final String TARGET_TYPE_USER = "user";
    private static final String PERMISSION_READ = "read";

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final KbShareRepository kbShareRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KbTeamRepository kbTeamRepository;

    // ===== 团队 CRUD =====

    @Override
    @Transactional
    public TeamDTO createTeam(String name, String description, String avatar, String creatorUserId) {
        if (name == null || name.isBlank()) {
            throw new AIException("团队名称不能为空");
        }

        Team team = new Team();
        team.setName(name.trim());
        team.setDescription(description);
        team.setAvatar(avatar != null ? avatar : "👥");
        team.setOwnerId(creatorUserId);
        teamRepository.save(team);

        // 创建者自动成为 owner 成员
        TeamMember ownerMember = new TeamMember();
        ownerMember.setTeamId(team.getId());
        ownerMember.setUserId(creatorUserId);
        ownerMember.setRole(ROLE_OWNER);
        teamMemberRepository.save(ownerMember);

        log.info("[团队] 创建团队: id={}, name={}, owner={}", team.getId(), name, creatorUserId);
        return toTeamDTO(team, creatorUserId, true);
    }

    @Override
    public TeamDTO getTeam(Long teamId, String currentUserId) {
        Team team = teamRepository.findById(teamId);
        if (team == null) {
            throw new AIException("团队不存在");
        }
        checkTeamMembership(teamId, currentUserId);
        return toTeamDTO(team, currentUserId, true);
    }

    @Override
    public List<TeamDTO> listMyTeams(String userId) {
        List<TeamMember> memberships = teamMemberRepository.findByUserId(userId);
        List<TeamDTO> result = new ArrayList<>(memberships.size());
        for (TeamMember membership : memberships) {
            Team team = teamRepository.findById(membership.getTeamId());
            if (team != null) {
                result.add(toTeamDTO(team, userId, false));
            }
        }
        return result;
    }

    @Override
    @Transactional
    public TeamDTO updateTeam(Long teamId, String name, String description, String avatar, String currentUserId) {
        Team team = teamRepository.findById(teamId);
        if (team == null) {
            throw new AIException("团队不存在");
        }
        checkTeamAdmin(teamId, currentUserId);

        if (name != null && !name.isBlank()) {
            team.setName(name.trim());
        }
        if (description != null) {
            team.setDescription(description);
        }
        if (avatar != null) {
            team.setAvatar(avatar);
        }
        teamRepository.update(team);
        log.info("[团队] 更新团队: id={}, operator={}", teamId, currentUserId);
        return toTeamDTO(team, currentUserId, false);
    }

    @Override
    @Transactional
    public void deleteTeam(Long teamId, String currentUserId) {
        Team team = teamRepository.findById(teamId);
        if (team == null) {
            throw new AIException("团队不存在");
        }
        checkTeamOwner(teamId, currentUserId);

        // 删除团队及其成员
        teamRepository.deleteById(teamId);
        teamMemberRepository.deleteByTeamId(teamId);
        log.info("[团队] 删除团队: id={}, operator={}", teamId, currentUserId);
    }

    // ===== 成员管理 =====

    @Override
    @Transactional
    public TeamMemberDTO addMember(Long teamId, String userId, String role, String operatorUserId) {
        checkTeamAdmin(teamId, operatorUserId);

        // 检查是否已是成员
        TeamMember existing = teamMemberRepository.findByTeamIdAndUserId(teamId, userId);
        if (existing != null) {
            throw new AIException("该用户已是团队成员");
        }

        // 不允许直接添加 owner
        String effectiveRole = ROLE_OWNER.equals(role) ? ROLE_MEMBER : (role != null ? role : ROLE_MEMBER);

        TeamMember member = new TeamMember();
        member.setTeamId(teamId);
        member.setUserId(userId);
        member.setRole(effectiveRole);
        teamMemberRepository.save(member);

        log.info("[团队] 添加成员: teamId={}, userId={}, role={}, operator={}", teamId, userId, effectiveRole, operatorUserId);
        return toMemberDTO(member);
    }

    @Override
    @Transactional
    public void removeMember(Long teamId, String targetUserId, String operatorUserId) {
        checkTeamAdmin(teamId, operatorUserId);

        TeamMember target = teamMemberRepository.findByTeamIdAndUserId(teamId, targetUserId);
        if (target == null) {
            throw new AIException("该用户不是团队成员");
        }
        if (ROLE_OWNER.equals(target.getRole())) {
            throw new AIException("不可移除团队拥有者");
        }

        teamMemberRepository.deleteById(target.getId());
        log.info("[团队] 移除成员: teamId={}, userId={}, operator={}", teamId, targetUserId, operatorUserId);
    }

    @Override
    @Transactional
    public void changeMemberRole(Long teamId, String targetUserId, String newRole, String operatorUserId) {
        checkTeamOwner(teamId, operatorUserId);

        TeamMember target = teamMemberRepository.findByTeamIdAndUserId(teamId, targetUserId);
        if (target == null) {
            throw new AIException("该用户不是团队成员");
        }
        if (ROLE_OWNER.equals(target.getRole())) {
            throw new AIException("不可变更拥有者角色");
        }
        if (!ROLE_ADMIN.equals(newRole) && !ROLE_MEMBER.equals(newRole)) {
            throw new AIException("无效的角色: " + newRole);
        }

        target.setRole(newRole);
        teamMemberRepository.update(target);
        log.info("[团队] 变更角色: teamId={}, userId={}, newRole={}, operator={}", teamId, targetUserId, newRole, operatorUserId);
    }

    @Override
    public List<TeamMemberDTO> listMembers(Long teamId, String currentUserId) {
        checkTeamMembership(teamId, currentUserId);
        return teamMemberRepository.findByTeamId(teamId).stream()
                .map(this::toMemberDTO)
                .toList();
    }

    // ===== 团队知识库 =====

    @Override
    @Transactional
    public KnowledgeBaseDTO createTeamKnowledgeBase(Long teamId, String name, String description, String icon, String creatorUserId) {
        Team team = teamRepository.findById(teamId);
        if (team == null) {
            throw new AIException("团队不存在");
        }
        checkTeamAdmin(teamId, creatorUserId);

        if (name == null || name.isBlank()) {
            throw new AIException("知识库名称不能为空");
        }

        KnowledgeBase kb = new KnowledgeBase();
        kb.setName(name.trim());
        kb.setDescription(description);
        kb.setIcon(icon != null ? icon : "📚");
        kb.setParentId(0L);
        kb.setSortOrder(0);
        kb.setIsPublic(0);
        kb.setOwnerType(OWNER_TYPE_TEAM);
        kb.setOwnerId(String.valueOf(teamId));
        kb.setCreatorId(creatorUserId);
        knowledgeBaseRepository.save(kb);

        // 写入知识库-团队关联表
        KbTeam link = new KbTeam();
        link.setKbId(kb.getId());
        link.setTeamId(teamId);
        kbTeamRepository.save(link);

        log.info("[团队] 创建团队知识库: teamId={}, kbId={}, name={}", teamId, kb.getId(), name);
        return toKnowledgeBaseDTO(kb);
    }

    @Override
    public List<KnowledgeBaseDTO> listTeamKnowledgeBases(Long teamId, String currentUserId) {
        checkTeamMembership(teamId, currentUserId);
        // 通过关联表查询该团队关联的所有知识库
        List<Long> kbIds = kbTeamRepository.findByTeamId(teamId).stream()
                .map(KbTeam::getKbId)
                .toList();
        List<KnowledgeBase> bases = knowledgeBaseRepository.findByIds(kbIds);
        return bases.stream().map(this::toKnowledgeBaseDTO).toList();
    }

    // ===== 知识库共享 =====

    @Override
    @Transactional
    public KbShareDTO shareKnowledgeBase(Long kbId, String targetType, String targetId, String operatorUserId) {
        KnowledgeBase kb = knowledgeBaseRepository.findById(kbId);
        if (kb == null) {
            throw new AIException("知识库不存在");
        }
        // 校验操作者权限：KB 所属团队的 admin/owner，或 KB 的个人所有者
        checkKbManagePermission(kb, operatorUserId);

        if (!TARGET_TYPE_TEAM.equals(targetType) && !TARGET_TYPE_USER.equals(targetType)) {
            throw new AIException("无效的授权目标类型: " + targetType);
        }

        // 检查是否已存在
        KbShare existing = kbShareRepository.findByKbIdAndTarget(kbId, targetType, targetId);
        if (existing != null) {
            throw new AIException("该知识库已共享给此目标");
        }

        KbShare share = new KbShare();
        share.setKbId(kbId);
        share.setTargetType(targetType);
        share.setTargetId(targetId);
        share.setPermission(PERMISSION_READ);
        share.setSharedBy(operatorUserId);
        kbShareRepository.save(share);

        log.info("[共享] 共享知识库: kbId={}, targetType={}, targetId={}, operator={}", kbId, targetType, targetId, operatorUserId);
        return toShareDTO(share, kb.getName());
    }

    @Override
    @Transactional
    public void revokeShare(Long shareId, String operatorUserId) {
        KbShare share = kbShareRepository.findById(shareId);
        if (share == null) {
            throw new AIException("共享记录不存在");
        }
        KnowledgeBase kb = knowledgeBaseRepository.findById(share.getKbId());
        if (kb != null) {
            checkKbManagePermission(kb, operatorUserId);
        }
        kbShareRepository.deleteById(shareId);
        log.info("[共享] 撤销共享: shareId={}, operator={}", shareId, operatorUserId);
    }

    @Override
    public List<KbShareDTO> listShares(Long kbId, String currentUserId) {
        KnowledgeBase kb = knowledgeBaseRepository.findById(kbId);
        if (kb == null) {
            throw new AIException("知识库不存在");
        }
        checkKbManagePermission(kb, currentUserId);

        return kbShareRepository.findByKbId(kbId).stream()
                .map(s -> toShareDTO(s, kb.getName()))
                .toList();
    }

    @Override
    public List<KnowledgeBaseDTO> listSharedWithMe(String userId) {
        List<KnowledgeBaseDTO> result = new ArrayList<>();

        // 1. 直接授权给我个人的
        List<KbShare> userShares = kbShareRepository.findByTarget(TARGET_TYPE_USER, userId);
        for (KbShare share : userShares) {
            KnowledgeBase kb = knowledgeBaseRepository.findById(share.getKbId());
            if (kb != null) {
                result.add(toKnowledgeBaseDTO(kb));
            }
        }

        // 2. 授权给我所在团队的
        List<TeamMember> memberships = teamMemberRepository.findByUserId(userId);
        for (TeamMember membership : memberships) {
            List<KbShare> teamShares = kbShareRepository.findByTarget(TARGET_TYPE_TEAM, String.valueOf(membership.getTeamId()));
            for (KbShare share : teamShares) {
                KnowledgeBase kb = knowledgeBaseRepository.findById(share.getKbId());
                if (kb != null) {
                    // 去重（避免个人和团队同时被授权时重复）
                    boolean alreadyAdded = result.stream().anyMatch(r -> Objects.equals(r.getId(), kb.getId()));
                    if (!alreadyAdded) {
                        result.add(toKnowledgeBaseDTO(kb));
                    }
                }
            }
        }

        return result;
    }

    // ===== 权限校验 =====

    @Override
    public boolean canViewKnowledgeBase(Long kbId, String userId) {
        KnowledgeBase kb = knowledgeBaseRepository.findById(kbId);
        if (kb == null) {
            return false;
        }
        // 公开的
        if (kb.getIsPublic() != null && kb.getIsPublic() == 1) {
            return true;
        }
        // 个人所有
        if (OWNER_TYPE_PERSONAL.equals(kb.getOwnerType()) && userId.equals(kb.getOwnerId())) {
            return true;
        }
        // 团队所有：检查 KB 关联的任一团队是否为当前用户所在团队
        if (OWNER_TYPE_TEAM.equals(kb.getOwnerType())) {
            List<Long> linkedTeamIds = kbTeamRepository.findByKbId(kbId).stream()
                    .map(KbTeam::getTeamId)
                    .toList();
            for (Long teamId : linkedTeamIds) {
                TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, userId);
                if (member != null) {
                    return true;
                }
            }
        }
        // 被共享（个人级）
        KbShare userShare = kbShareRepository.findByKbIdAndTarget(kbId, TARGET_TYPE_USER, userId);
        if (userShare != null) {
            return true;
        }
        // 被共享（团队级）
        List<TeamMember> memberships = teamMemberRepository.findByUserId(userId);
        for (TeamMember membership : memberships) {
            KbShare teamShare = kbShareRepository.findByKbIdAndTarget(kbId, TARGET_TYPE_TEAM, String.valueOf(membership.getTeamId()));
            if (teamShare != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<Long> getVisibleKbIds(String userId) {
        Set<Long> visibleIds = new HashSet<>();

        // 1. 公开的知识库
        visibleIds.addAll(knowledgeBaseRepository.findIdsByIsPublic(1));

        // 2. 个人所有的知识库
        List<KnowledgeBase> personalKbs = knowledgeBaseRepository.findByOwner(OWNER_TYPE_PERSONAL, userId);
        for (KnowledgeBase kb : personalKbs) {
            visibleIds.add(kb.getId());
        }

        // 3. 所属团队的知识库（通过关联表批量查询）
        List<TeamMember> memberships = teamMemberRepository.findByUserId(userId);
        List<Long> myTeamIds = memberships.stream()
                .map(TeamMember::getTeamId)
                .toList();
        visibleIds.addAll(kbTeamRepository.findKbIdsByTeamIds(myTeamIds));

        // 4. 被共享的知识库（个人级）
        List<KbShare> userShares = kbShareRepository.findByTarget(TARGET_TYPE_USER, userId);
        for (KbShare share : userShares) {
            visibleIds.add(share.getKbId());
        }

        // 5. 被共享的知识库（团队级）
        for (TeamMember membership : memberships) {
            List<KbShare> teamShares = kbShareRepository.findByTarget(
                    TARGET_TYPE_TEAM, String.valueOf(membership.getTeamId()));
            for (KbShare share : teamShares) {
                visibleIds.add(share.getKbId());
            }
        }

        return visibleIds;
    }

    // ===== 私有辅助方法 =====

    private void checkTeamMembership(Long teamId, String userId) {
        TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, userId);
        if (member == null) {
            throw new AIException("您不是该团队的成员");
        }
    }

    private void checkTeamAdmin(Long teamId, String userId) {
        TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, userId);
        if (member == null) {
            throw new AIException("您不是该团队的成员");
        }
        if (!ROLE_OWNER.equals(member.getRole()) && !ROLE_ADMIN.equals(member.getRole())) {
            throw new AIException("权限不足，需要管理员或拥有者角色");
        }
    }

    private void checkTeamOwner(Long teamId, String userId) {
        TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, userId);
        if (member == null || !ROLE_OWNER.equals(member.getRole())) {
            throw new AIException("权限不足，需要拥有者角色");
        }
    }

    private void checkKbManagePermission(KnowledgeBase kb, String userId) {
        if (OWNER_TYPE_TEAM.equals(kb.getOwnerType())) {
            // 多团队归属：只要是任一关联团队的管理员/拥有者即可管理
            List<Long> linkedTeamIds = kbTeamRepository.findByKbId(kb.getId()).stream()
                    .map(KbTeam::getTeamId)
                    .toList();
            for (Long teamId : linkedTeamIds) {
                TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, userId);
                if (member != null && (ROLE_OWNER.equals(member.getRole()) || ROLE_ADMIN.equals(member.getRole()))) {
                    return;
                }
            }
            throw new AIException("权限不足，需要知识库所属团队的管理员或拥有者角色");
        } else if (OWNER_TYPE_PERSONAL.equals(kb.getOwnerType())) {
            if (!userId.equals(kb.getOwnerId()) && !userId.equals(kb.getCreatorId())) {
                throw new AIException("权限不足，仅知识库所有者可管理共享");
            }
        }
    }

    private TeamDTO toTeamDTO(Team team, String currentUserId, boolean includeMembers) {
        String myRole = null;
        TeamMember myMembership = teamMemberRepository.findByTeamIdAndUserId(team.getId(), currentUserId);
        if (myMembership != null) {
            myRole = myMembership.getRole();
        }

        int memberCount = (int) teamMemberRepository.countByTeamId(team.getId());
        int kbCount = kbTeamRepository.findByTeamId(team.getId()).size();

        List<TeamMemberDTO> members = null;
        if (includeMembers) {
            members = teamMemberRepository.findByTeamId(team.getId()).stream()
                    .map(this::toMemberDTO)
                    .toList();
        }

        return new TeamDTO(
                team.getId(),
                team.getName(),
                team.getDescription(),
                team.getAvatar(),
                team.getOwnerId(),
                myRole,
                memberCount,
                kbCount,
                team.getCreateTime(),
                members
        );
    }

    private TeamMemberDTO toMemberDTO(TeamMember member) {
        return new TeamMemberDTO(
                member.getId(),
                member.getTeamId(),
                member.getUserId(),
                member.getRole(),
                member.getCreateTime()
        );
    }

    private KbShareDTO toShareDTO(KbShare share, String kbName) {
        String targetName = share.getTargetId();
        if (TARGET_TYPE_TEAM.equals(share.getTargetType())) {
            try {
                Team team = teamRepository.findById(Long.parseLong(share.getTargetId()));
                if (team != null) {
                    targetName = team.getName();
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return new KbShareDTO(
                share.getId(),
                share.getKbId(),
                kbName,
                share.getTargetType(),
                share.getTargetId(),
                targetName,
                share.getPermission(),
                share.getSharedBy(),
                share.getCreateTime()
        );
    }

    private KnowledgeBaseDTO toKnowledgeBaseDTO(KnowledgeBase kb) {
        KnowledgeBaseDTO dto = new KnowledgeBaseDTO();
        dto.setId(kb.getId());
        dto.setName(kb.getName());
        dto.setDescription(kb.getDescription());
        dto.setIcon(kb.getIcon());
        dto.setParentId(kb.getParentId());
        dto.setSortOrder(kb.getSortOrder());
        dto.setIsPublic(kb.getIsPublic());
        dto.setOwnerType(kb.getOwnerType());
        dto.setOwnerId(kb.getOwnerId());
        dto.setCreatorId(kb.getCreatorId());
        dto.setCreateTime(kb.getCreateTime());
        dto.setUpdateTime(kb.getUpdateTime());
        return dto;
    }
}
