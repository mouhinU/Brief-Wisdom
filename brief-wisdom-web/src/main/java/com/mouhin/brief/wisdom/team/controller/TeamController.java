package com.mouhin.brief.wisdom.team.controller;

import com.mouhin.brief.wisdom.ai.service.TeamService;
import com.mouhin.brief.wisdom.common.Result;
import com.mouhin.brief.wisdom.common.knowledge.KnowledgeBaseDTO;
import com.mouhin.brief.wisdom.common.team.KbShareDTO;
import com.mouhin.brief.wisdom.common.team.TeamDTO;
import com.mouhin.brief.wisdom.common.team.TeamMemberDTO;
import com.mouhin.brief.wisdom.system.service.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 团队管理控制器
 * <p>
 * 提供团队 CRUD、成员管理、团队知识库和知识库共享授权 API。
 *
 * @author Brief-Wisdom
 * @date 2026-07-25
 */
@Slf4j
@RestController
@RequestMapping("/api/team")
@RequiredArgsConstructor
@Tag(name = "团队管理", description = "团队 CRUD、成员管理、团队知识库、知识库共享")
public class TeamController {

    private final TeamService teamService;
    private final UserContextHolder userContextHolder;

    // ===== 团队 CRUD =====

    @Operation(summary = "创建团队")
    @PostMapping
    public Result<TeamDTO> createTeam(@RequestBody Map<String, String> body) {
        String userId = userContextHolder.getCurrentUserId();
        TeamDTO team = teamService.createTeam(
                body.get("name"),
                body.get("description"),
                body.get("avatar"),
                userId
        );
        return Result.success(team);
    }

    @Operation(summary = "获取团队详情")
    @GetMapping("/{teamId}")
    public Result<TeamDTO> getTeam(@PathVariable Long teamId) {
        String userId = userContextHolder.getCurrentUserId();
        return Result.success(teamService.getTeam(teamId, userId));
    }

    @Operation(summary = "获取我的团队列表")
    @GetMapping("/my")
    public Result<List<TeamDTO>> listMyTeams() {
        String userId = userContextHolder.getCurrentUserId();
        return Result.success(teamService.listMyTeams(userId));
    }

    @Operation(summary = "更新团队信息")
    @PutMapping("/{teamId}")
    public Result<TeamDTO> updateTeam(@PathVariable Long teamId, @RequestBody Map<String, String> body) {
        String userId = userContextHolder.getCurrentUserId();
        TeamDTO team = teamService.updateTeam(
                teamId,
                body.get("name"),
                body.get("description"),
                body.get("avatar"),
                userId
        );
        return Result.success(team);
    }

    @Operation(summary = "删除团队")
    @DeleteMapping("/{teamId}")
    public Result<Void> deleteTeam(@PathVariable Long teamId) {
        String userId = userContextHolder.getCurrentUserId();
        teamService.deleteTeam(teamId, userId);
        return Result.success(null);
    }

    // ===== 成员管理 =====

    @Operation(summary = "添加团队成员")
    @PostMapping("/{teamId}/members")
    public Result<TeamMemberDTO> addMember(@PathVariable Long teamId, @RequestBody Map<String, String> body) {
        String userId = userContextHolder.getCurrentUserId();
        TeamMemberDTO member = teamService.addMember(
                teamId,
                body.get("userId"),
                body.get("role"),
                userId
        );
        return Result.success(member);
    }

    @Operation(summary = "移除团队成员")
    @DeleteMapping("/{teamId}/members/{targetUserId}")
    public Result<Void> removeMember(@PathVariable Long teamId, @PathVariable String targetUserId) {
        String userId = userContextHolder.getCurrentUserId();
        teamService.removeMember(teamId, targetUserId, userId);
        return Result.success(null);
    }

    @Operation(summary = "变更成员角色")
    @PutMapping("/{teamId}/members/{targetUserId}/role")
    public Result<Void> changeMemberRole(@PathVariable Long teamId, @PathVariable String targetUserId,
                                         @RequestBody Map<String, String> body) {
        String userId = userContextHolder.getCurrentUserId();
        teamService.changeMemberRole(teamId, targetUserId, body.get("role"), userId);
        return Result.success(null);
    }

    @Operation(summary = "获取团队成员列表")
    @GetMapping("/{teamId}/members")
    public Result<List<TeamMemberDTO>> listMembers(@PathVariable Long teamId) {
        String userId = userContextHolder.getCurrentUserId();
        return Result.success(teamService.listMembers(teamId, userId));
    }

    // ===== 团队知识库 =====

    @Operation(summary = "创建团队知识库")
    @PostMapping("/{teamId}/knowledge-bases")
    public Result<KnowledgeBaseDTO> createTeamKb(@PathVariable Long teamId, @RequestBody Map<String, String> body) {
        String userId = userContextHolder.getCurrentUserId();
        KnowledgeBaseDTO kb = teamService.createTeamKnowledgeBase(
                teamId,
                body.get("name"),
                body.get("description"),
                body.get("icon"),
                userId
        );
        return Result.success(kb);
    }

    @Operation(summary = "获取团队知识库列表")
    @GetMapping("/{teamId}/knowledge-bases")
    public Result<List<KnowledgeBaseDTO>> listTeamKbs(@PathVariable Long teamId) {
        String userId = userContextHolder.getCurrentUserId();
        return Result.success(teamService.listTeamKnowledgeBases(teamId, userId));
    }

    // ===== 知识库共享 =====

    @Operation(summary = "共享知识库")
    @PostMapping("/knowledge-bases/{kbId}/shares")
    public Result<KbShareDTO> shareKb(@PathVariable Long kbId, @RequestBody Map<String, String> body) {
        String userId = userContextHolder.getCurrentUserId();
        KbShareDTO share = teamService.shareKnowledgeBase(
                kbId,
                body.get("targetType"),
                body.get("targetId"),
                userId
        );
        return Result.success(share);
    }

    @Operation(summary = "撤销共享")
    @DeleteMapping("/shares/{shareId}")
    public Result<Void> revokeShare(@PathVariable Long shareId) {
        String userId = userContextHolder.getCurrentUserId();
        teamService.revokeShare(shareId, userId);
        return Result.success(null);
    }

    @Operation(summary = "获取知识库共享列表")
    @GetMapping("/knowledge-bases/{kbId}/shares")
    public Result<List<KbShareDTO>> listShares(@PathVariable Long kbId) {
        String userId = userContextHolder.getCurrentUserId();
        return Result.success(teamService.listShares(kbId, userId));
    }

    @Operation(summary = "获取共享给我的知识库")
    @GetMapping("/knowledge-bases/shared-with-me")
    public Result<List<KnowledgeBaseDTO>> listSharedWithMe() {
        String userId = userContextHolder.getCurrentUserId();
        return Result.success(teamService.listSharedWithMe(userId));
    }
}
