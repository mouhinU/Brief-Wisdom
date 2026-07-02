#!/usr/bin/env python3
"""
将 Repository 层重构为 MyBatis-Plus IService/ServiceImpl 模式。
批量生成 Service 接口和 Impl 实现类。
"""
import os

SVC_DIR = "brief-wisdom-persistence/src/main/java/com/mouhin/brief/wisdom/persistence/service"
PKG = "com.mouhin.brief.wisdom.persistence.service"
MODEL_PKG = "com.mouhin.brief.wisdom.persistence.model"
MAPPER_PKG = "com.mouhin.brief.wisdom.persistence.mapper"
DATE = "2026-06-30"
AUTHOR = "Brief-Wisdom"

# Each entry: (Entity, Mapper, interface_methods, impl_methods)
# interface_methods: list of (signature, ) — just the method declaration
# impl_methods: list of (signature, body) — full implementation
# signature = "ReturnType methodName(params)"
# We keep the SAME method names as the old Repository to minimize caller changes.

ENTRIES = [
    # ==================== AiModel ====================
    ("AiModel", "AiModelMapper",
     # interface custom methods
     [
         "List<AiModel> findAll()",
         "List<AiModel> findByEnabledOrderBySortOrderAsc()",
         "List<AiModel> findAllOrderBySortOrderAsc()",
         "AiModel findActiveModel()",
         "AiModel findByModelName(String modelName)",
         "void deactivateAll()",
     ],
     # impl custom methods
     [
         ("List<AiModel> findAll()",
          """return this.list();"""),
         ("List<AiModel> findByEnabledOrderBySortOrderAsc()",
          """return this.list(new LambdaQueryWrapper<AiModel>()
                .eq(AiModel::getIsEnabled, 1)
                .orderByAsc(AiModel::getSortOrder));"""),
         ("List<AiModel> findAllOrderBySortOrderAsc()",
          """return this.list(new LambdaQueryWrapper<AiModel>()
                .orderByAsc(AiModel::getSortOrder));"""),
         ("AiModel findActiveModel()",
          """return this.getOne(new LambdaQueryWrapper<AiModel>()
                .eq(AiModel::getIsActive, 1)
                .eq(AiModel::getIsEnabled, 1));"""),
         ("AiModel findByModelName(String modelName)",
          """return this.getOne(new LambdaQueryWrapper<AiModel>()
                .eq(AiModel::getModelName, modelName));"""),
         ("void deactivateAll()",
          """baseMapper.deactivateAll();"""),
     ]
    ),
    # ==================== ChatMessage ====================
    ("ChatMessage", "ChatMessageMapper",
     [
         "List<ChatMessage> findBySessionIdOrderByTimestampAsc(String sessionId)",
         "Page<ChatMessage> findBySessionIdOrderByTimestampDesc(String sessionId, int page, int size)",
         "long countByUserId(String userId)",
         "long countBySessionId(String sessionId)",
         "List<ChatMessage> findRecentMessages(String sessionId, int limit)",
         "java.time.LocalDateTime findLastMessageTime(String sessionId)",
         "List<Map<String, Object>> findMessageCountsByUserId(String userId)",
         "List<Map<String, Object>> findLastMessageTimesByUserId(String userId)",
     ],
     [
         ("List<ChatMessage> findBySessionIdOrderByTimestampAsc(String sessionId)",
          """return this.list(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
                .orderByAsc(ChatMessage::getTimestamp));"""),
         ("Page<ChatMessage> findBySessionIdOrderByTimestampDesc(String sessionId, int page, int size)",
          """Page<ChatMessage> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ChatMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatMessage::getSessionId, sessionId)
                .orderByDesc(ChatMessage::getTimestamp);
        return this.page(pageParam, queryWrapper);"""),
         ("long countByUserId(String userId)",
          """return this.count(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getUserId, userId));"""),
         ("long countBySessionId(String sessionId)",
          """return this.count(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId));"""),
         ("List<ChatMessage> findRecentMessages(String sessionId, int limit)",
          """return this.list(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
                .orderByDesc(ChatMessage::getTimestamp)
                .last("LIMIT " + limit));"""),
         ("java.time.LocalDateTime findLastMessageTime(String sessionId)",
          """LambdaQueryWrapper<ChatMessage> qw = new LambdaQueryWrapper<>();
        qw.select(ChatMessage::getTimestamp)
                .eq(ChatMessage::getSessionId, sessionId)
                .orderByDesc(ChatMessage::getTimestamp)
                .last("LIMIT 1");
        ChatMessage msg = this.getOne(qw);
        return msg != null ? msg.getTimestamp() : null;"""),
         ("List<Map<String, Object>> findMessageCountsByUserId(String userId)",
          """return baseMapper.selectMessageCountsByUserId(userId);"""),
         ("List<Map<String, Object>> findLastMessageTimesByUserId(String userId)",
          """return baseMapper.selectLastMessageTimesByUserId(userId);"""),
     ]
    ),
    # ==================== ChatSession ====================
    ("ChatSession", "ChatSessionMapper",
     [
         "List<ChatSession> findByUserIdOrderByUpdateTimeDesc(String userId)",
         "Page<ChatSession> findByUserIdOrderByUpdateTimeDesc(String userId, int page, int size)",
         "List<ChatSession> findByUserIdsOrderByUpdateTimeDesc(List<String> userIds)",
         "long countByUserId(String userId)",
         "ChatSession findBySessionId(String sessionId)",
         "List<ChatSession> findRecentByUserIdAndPageContext(String userId, String pageContext, int limit)",
         "void deleteBySessionId(String sessionId)",
     ],
     [
         ("List<ChatSession> findByUserIdOrderByUpdateTimeDesc(String userId)",
          """return this.list(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUserId, userId)
                .orderByDesc(ChatSession::getUpdateTime));"""),
         ("Page<ChatSession> findByUserIdOrderByUpdateTimeDesc(String userId, int page, int size)",
          """Page<ChatSession> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ChatSession> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatSession::getUserId, userId)
                .orderByDesc(ChatSession::getUpdateTime);
        return this.page(pageParam, queryWrapper);"""),
         ("List<ChatSession> findByUserIdsOrderByUpdateTimeDesc(List<String> userIds)",
          """return this.list(new LambdaQueryWrapper<ChatSession>()
                .in(ChatSession::getUserId, userIds)
                .orderByDesc(ChatSession::getUpdateTime));"""),
         ("long countByUserId(String userId)",
          """return this.count(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUserId, userId));"""),
         ("ChatSession findBySessionId(String sessionId)",
          """return this.getOne(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getSessionId, sessionId));"""),
         ("List<ChatSession> findRecentByUserIdAndPageContext(String userId, String pageContext, int limit)",
          """return this.list(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUserId, userId)
                .eq(ChatSession::getPageContext, pageContext)
                .orderByDesc(ChatSession::getUpdateTime)
                .last("LIMIT " + limit));"""),
         ("void deleteBySessionId(String sessionId)",
          """LambdaQueryWrapper<ChatSession> qw = new LambdaQueryWrapper<>();
        qw.eq(ChatSession::getSessionId, sessionId);
        this.remove(qw);"""),
     ]
    ),
    # ==================== ChatUser ====================
    ("ChatUser", "ChatUserMapper",
     [
         "ChatUser findByUserId(String userId)",
         "ChatUser findByUsername(String username)",
         "ChatUser findByUserIdIncludeDeleted(String userId)",
         "List<ChatUser> findAllOrderByCreateTimeDesc()",
         "List<ChatUser> findByUserLevelOrderByCreateTimeDesc(String userLevel)",
         "List<ChatUser> findByUserLevel(String userLevel)",
         "Page<ChatUser> findPage(int page, int size, LambdaQueryWrapper<ChatUser> query)",
         "void hardDeleteByUserId(String userId)",
     ],
     [
         ("ChatUser findByUserId(String userId)",
          """return this.getOne(new LambdaQueryWrapper<ChatUser>()
                .eq(ChatUser::getUserId, userId));"""),
         ("ChatUser findByUsername(String username)",
          """return this.getOne(new LambdaQueryWrapper<ChatUser>()
                .eq(ChatUser::getUsername, username));"""),
         ("ChatUser findByUserIdIncludeDeleted(String userId)",
          """// 绕过 @TableLogic，直接查询包含已删除记录
        return baseMapper.selectOne(new LambdaQueryWrapper<ChatUser>()
                .eq(ChatUser::getUserId, userId));"""),
         ("List<ChatUser> findAllOrderByCreateTimeDesc()",
          """return this.list(new LambdaQueryWrapper<ChatUser>()
                .orderByDesc(ChatUser::getCreateTime));"""),
         ("List<ChatUser> findByUserLevelOrderByCreateTimeDesc(String userLevel)",
          """LambdaQueryWrapper<ChatUser> query = new LambdaQueryWrapper<ChatUser>()
                .orderByDesc(ChatUser::getCreateTime);
        if (userLevel != null && !userLevel.isEmpty()) {
            query.eq(ChatUser::getUserLevel, userLevel);
        }
        return this.list(query);"""),
         ("List<ChatUser> findByUserLevel(String userLevel)",
          """return this.list(new LambdaQueryWrapper<ChatUser>()
                .eq(ChatUser::getUserLevel, userLevel));"""),
         ("Page<ChatUser> findPage(int page, int size, LambdaQueryWrapper<ChatUser> query)",
          """Page<ChatUser> pageParam = new Page<>(page, size);
        return this.page(pageParam, query);"""),
         ("void hardDeleteByUserId(String userId)",
          """baseMapper.hardDeleteByUserId(userId);"""),
     ]
    ),
    # ==================== KnowledgeBase ====================
    ("KnowledgeBase", "KnowledgeBaseMapper",
     [
         "List<KnowledgeBase> findAll()",
         "List<KnowledgeBase> findByParentId(Long parentId)",
         "long countByParentId(Long parentId)",
         "Page<KnowledgeBase> findTopLevelPaged(int page, int size)",
     ],
     [
         ("List<KnowledgeBase> findAll()",
          """return this.list(new LambdaQueryWrapper<KnowledgeBase>()
                .orderByAsc(KnowledgeBase::getSortOrder));"""),
         ("List<KnowledgeBase> findByParentId(Long parentId)",
          """return this.list(new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getParentId, parentId)
                .orderByAsc(KnowledgeBase::getSortOrder));"""),
         ("long countByParentId(Long parentId)",
          """LambdaQueryWrapper<KnowledgeBase> qw = new LambdaQueryWrapper<>();
        qw.eq(KnowledgeBase::getParentId, parentId);
        return this.count(qw);"""),
         ("Page<KnowledgeBase> findTopLevelPaged(int page, int size)",
          """Page<KnowledgeBase> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<KnowledgeBase> qw = new LambdaQueryWrapper<>();
        qw.eq(KnowledgeBase::getParentId, 0L)
                .orderByAsc(KnowledgeBase::getSortOrder)
                .orderByDesc(KnowledgeBase::getCreateTime);
        return this.page(pageParam, qw);"""),
     ]
    ),
    # ==================== KnowledgeDocument ====================
    ("KnowledgeDocument", "KnowledgeDocumentMapper",
     [
         "List<KnowledgeDocument> findByBaseId(Long baseId)",
         "Page<KnowledgeDocument> findByBaseIdPaged(Long baseId, int page, int size)",
         "Page<KnowledgeDocument> findByBaseIdAndTypePaged(Long baseId, String docType, int page, int size)",
         "long countByBaseId(Long baseId)",
         "void incrementViewCount(Long id)",
         "Page<KnowledgeDocument> searchByTitle(String keyword, int page, int size)",
     ],
     [
         ("List<KnowledgeDocument> findByBaseId(Long baseId)",
          """return this.list(new LambdaQueryWrapper<KnowledgeDocument>()
                .eq(KnowledgeDocument::getBaseId, baseId)
                .orderByAsc(KnowledgeDocument::getSortOrder)
                .orderByDesc(KnowledgeDocument::getUpdateTime));"""),
         ("Page<KnowledgeDocument> findByBaseIdPaged(Long baseId, int page, int size)",
          """Page<KnowledgeDocument> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<KnowledgeDocument> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(KnowledgeDocument::getBaseId, baseId)
                .orderByAsc(KnowledgeDocument::getSortOrder)
                .orderByDesc(KnowledgeDocument::getUpdateTime);
        return this.page(pageParam, queryWrapper);"""),
         ("Page<KnowledgeDocument> findByBaseIdAndTypePaged(Long baseId, String docType, int page, int size)",
          """Page<KnowledgeDocument> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<KnowledgeDocument> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(KnowledgeDocument::getBaseId, baseId);
        if (docType != null && !docType.isBlank()) {
            queryWrapper.eq(KnowledgeDocument::getDocType, docType);
        }
        queryWrapper.orderByAsc(KnowledgeDocument::getSortOrder)
                .orderByDesc(KnowledgeDocument::getUpdateTime);
        return this.page(pageParam, queryWrapper);"""),
         ("long countByBaseId(Long baseId)",
          """LambdaQueryWrapper<KnowledgeDocument> qw = new LambdaQueryWrapper<>();
        qw.eq(KnowledgeDocument::getBaseId, baseId);
        return this.count(qw);"""),
         ("void incrementViewCount(Long id)",
          """KnowledgeDocument doc = this.getById(id);
        if (doc != null) {
            doc.setViewCount(doc.getViewCount() + 1);
            this.updateById(doc);
        }"""),
         ("Page<KnowledgeDocument> searchByTitle(String keyword, int page, int size)",
          """Page<KnowledgeDocument> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<KnowledgeDocument> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(KnowledgeDocument::getTitle, keyword)
                .orderByDesc(KnowledgeDocument::getUpdateTime);
        return this.page(pageParam, queryWrapper);"""),
     ]
    ),
    # ==================== Project ====================
    ("Project", "ProjectMapper",
     [
         "List<Project> findAllOrderBySortOrderAsc()",
         "List<Project> findByExperienceIdOrderBySortOrderAsc(Long experienceId)",
         "List<Project> findByExperienceIdInOrderBySortOrderAsc(Collection<Long> experienceIds)",
     ],
     [
         ("List<Project> findAllOrderBySortOrderAsc()",
          """return this.list(new LambdaQueryWrapper<Project>()
                .orderByAsc(Project::getSortOrder));"""),
         ("List<Project> findByExperienceIdOrderBySortOrderAsc(Long experienceId)",
          """return this.list(new LambdaQueryWrapper<Project>()
                .eq(Project::getExperienceId, experienceId)
                .orderByAsc(Project::getSortOrder));"""),
         ("List<Project> findByExperienceIdInOrderBySortOrderAsc(Collection<Long> experienceIds)",
          """return this.list(new LambdaQueryWrapper<Project>()
                .in(Project::getExperienceId, experienceIds)
                .orderByAsc(Project::getSortOrder));"""),
     ]
    ),
    # ==================== ProjectAchievement ====================
    ("ProjectAchievement", "ProjectAchievementMapper",
     [
         "List<ProjectAchievement> findAllOrderBySortOrderAsc()",
         "List<ProjectAchievement> findByProjectIdOrderBySortOrderAsc(Long projectId)",
         "List<ProjectAchievement> findByProjectIdInOrderBySortOrderAsc(Collection<Long> projectIds)",
     ],
     [
         ("List<ProjectAchievement> findAllOrderBySortOrderAsc()",
          """return this.list(new LambdaQueryWrapper<ProjectAchievement>()
                .orderByAsc(ProjectAchievement::getSortOrder));"""),
         ("List<ProjectAchievement> findByProjectIdOrderBySortOrderAsc(Long projectId)",
          """return this.list(new LambdaQueryWrapper<ProjectAchievement>()
                .eq(ProjectAchievement::getProjectId, projectId)
                .orderByAsc(ProjectAchievement::getSortOrder));"""),
         ("List<ProjectAchievement> findByProjectIdInOrderBySortOrderAsc(Collection<Long> projectIds)",
          """return this.list(new LambdaQueryWrapper<ProjectAchievement>()
                .in(ProjectAchievement::getProjectId, projectIds)
                .orderByAsc(ProjectAchievement::getSortOrder));"""),
     ]
    ),
    # ==================== RoleMenu ====================
    ("RoleMenu", "RoleMenuMapper",
     [
         "List<RoleMenu> findByRoleId(Long roleId)",
         "List<Long> findMenuIdsByRoleId(Long roleId)",
         "void save(Long roleId, Long menuId)",
         "void deleteByRoleId(Long roleId)",
         "void deleteByMenuId(Long menuId)",
     ],
     [
         ("List<RoleMenu> findByRoleId(Long roleId)",
          """return this.list(new LambdaQueryWrapper<RoleMenu>()
                .eq(RoleMenu::getRoleId, roleId));"""),
         ("List<Long> findMenuIdsByRoleId(Long roleId)",
          """return findByRoleId(roleId).stream().map(RoleMenu::getMenuId).toList();"""),
         ("void save(Long roleId, Long menuId)",
          """RoleMenu roleMenu = new RoleMenu();
        roleMenu.setRoleId(roleId);
        roleMenu.setMenuId(menuId);
        baseMapper.insert(roleMenu);"""),
         ("void deleteByRoleId(Long roleId)",
          """this.remove(new LambdaQueryWrapper<RoleMenu>()
                .eq(RoleMenu::getRoleId, roleId));"""),
         ("void deleteByMenuId(Long menuId)",
          """this.remove(new LambdaQueryWrapper<RoleMenu>()
                .eq(RoleMenu::getMenuId, menuId));"""),
     ]
    ),
    # ==================== SysMenu ====================
    ("SysMenu", "SysMenuMapper",
     [
         "List<SysMenu> findVisibleOrderBySortOrderAsc()",
         "List<SysMenu> findPublicVisibleMenus()",
         "List<SysMenu> findAllOrderBySortOrderAsc()",
         "List<SysMenu> findByIds(List<Long> ids)",
         "List<SysMenu> findHiddenChildrenByParentIds(List<Long> parentIds)",
         "List<SysMenu> findByParentId(Long parentId)",
     ],
     [
         ("List<SysMenu> findVisibleOrderBySortOrderAsc()",
          """return this.list(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getIsVisible, 1)
                .orderByAsc(SysMenu::getSortOrder));"""),
         ("List<SysMenu> findPublicVisibleMenus()",
          """return this.list(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getIsVisible, 1)
                .eq(SysMenu::getRequireLogin, 0)
                .and(w -> w.isNull(SysMenu::getPermission).or().eq(SysMenu::getPermission, ""))
                .orderByAsc(SysMenu::getSortOrder));"""),
         ("List<SysMenu> findAllOrderBySortOrderAsc()",
          """return this.list(new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getSortOrder));"""),
         ("List<SysMenu> findByIds(List<Long> ids)",
          """if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return this.list(new LambdaQueryWrapper<SysMenu>()
                .in(SysMenu::getId, ids)
                .orderByAsc(SysMenu::getSortOrder));"""),
         ("List<SysMenu> findHiddenChildrenByParentIds(List<Long> parentIds)",
          """if (parentIds == null || parentIds.isEmpty()) {
            return List.of();
        }
        return this.list(new LambdaQueryWrapper<SysMenu>()
                .in(SysMenu::getParentId, parentIds)
                .eq(SysMenu::getIsVisible, 0)
                .orderByAsc(SysMenu::getSortOrder));"""),
         ("List<SysMenu> findByParentId(Long parentId)",
          """return this.list(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getParentId, parentId)
                .eq(SysMenu::getIsVisible, 1)
                .orderByAsc(SysMenu::getSortOrder));"""),
     ]
    ),
    # ==================== SysRole ====================
    ("SysRole", "SysRoleMapper",
     [
         "List<SysRole> findAll()",
         "List<SysRole> findAllEnabled()",
         "SysRole findByRoleKey(String roleKey)",
     ],
     [
         ("List<SysRole> findAll()",
          """return this.list(new LambdaQueryWrapper<SysRole>()
                .orderByAsc(SysRole::getId));"""),
         ("List<SysRole> findAllEnabled()",
          """return this.list(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getStatus, 1)
                .orderByAsc(SysRole::getId));"""),
         ("SysRole findByRoleKey(String roleKey)",
          """return this.getOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleKey, roleKey));"""),
     ]
    ),
    # ==================== UserOauth ====================
    ("UserOauth", "UserOauthMapper",
     [
         "UserOauth findByProviderAndOpenid(String provider, String openid)",
     ],
     [
         ("UserOauth findByProviderAndOpenid(String provider, String openid)",
          """return this.getOne(new LambdaQueryWrapper<UserOauth>()
                .eq(UserOauth::getProvider, provider)
                .eq(UserOauth::getOpenid, openid));"""),
     ]
    ),
    # ==================== UserRole ====================
    ("UserRole", "UserRoleMapper",
     [
         "List<UserRole> findByUserId(String userId)",
         "void save(String userId, Long roleId)",
         "void deleteByUserId(String userId)",
         "void deleteByUserIdAndRoleId(String userId, Long roleId)",
         "long countByRoleId(Long roleId)",
     ],
     [
         ("List<UserRole> findByUserId(String userId)",
          """return this.list(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId));"""),
         ("void save(String userId, Long roleId)",
          """UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        baseMapper.insert(userRole);"""),
         ("void deleteByUserId(String userId)",
          """this.remove(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId));"""),
         ("void deleteByUserIdAndRoleId(String userId, Long roleId)",
          """this.remove(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)
                .eq(UserRole::getRoleId, roleId));"""),
         ("long countByRoleId(Long roleId)",
          """return this.count(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getRoleId, roleId));"""),
     ]
    ),
    # ==================== WorkExperience ====================
    ("WorkExperience", "WorkExperienceMapper",
     [
         "List<WorkExperience> findVisibleOrderBySortOrderAsc()",
         "List<WorkExperience> findAllOrderBySortOrderAsc()",
         "List<WorkExperience> findByIdsOrderBySortOrderAsc(Collection<Long> ids)",
     ],
     [
         ("List<WorkExperience> findVisibleOrderBySortOrderAsc()",
          """return this.list(new LambdaQueryWrapper<WorkExperience>()
                .eq(WorkExperience::getIsVisible, 1)
                .orderByAsc(WorkExperience::getSortOrder));"""),
         ("List<WorkExperience> findAllOrderBySortOrderAsc()",
          """return this.list(new LambdaQueryWrapper<WorkExperience>()
                .orderByAsc(WorkExperience::getSortOrder));"""),
         ("List<WorkExperience> findByIdsOrderBySortOrderAsc(Collection<Long> ids)",
          """return this.list(new LambdaQueryWrapper<WorkExperience>()
                .in(WorkExperience::getId, ids)
                .orderByAsc(WorkExperience::getSortOrder));"""),
     ]
    ),
    # ==================== WorkExperienceStack ====================
    ("WorkExperienceStack", "WorkExperienceStackMapper",
     [
         "List<WorkExperienceStack> findAllOrderBySortOrderAsc()",
         "List<WorkExperienceStack> findByExperienceIdOrderBySortOrderAsc(Long experienceId)",
         "List<WorkExperienceStack> findByExperienceIdInOrderBySortOrderAsc(Collection<Long> experienceIds)",
     ],
     [
         ("List<WorkExperienceStack> findAllOrderBySortOrderAsc()",
          """return this.list(new LambdaQueryWrapper<WorkExperienceStack>()
                .orderByAsc(WorkExperienceStack::getSortOrder));"""),
         ("List<WorkExperienceStack> findByExperienceIdOrderBySortOrderAsc(Long experienceId)",
          """return this.list(new LambdaQueryWrapper<WorkExperienceStack>()
                .eq(WorkExperienceStack::getExperienceId, experienceId)
                .orderByAsc(WorkExperienceStack::getSortOrder));"""),
         ("List<WorkExperienceStack> findByExperienceIdInOrderBySortOrderAsc(Collection<Long> experienceIds)",
          """return this.list(new LambdaQueryWrapper<WorkExperienceStack>()
                .in(WorkExperienceStack::getExperienceId, experienceIds)
                .orderByAsc(WorkExperienceStack::getSortOrder));"""),
     ]
    ),
]

# Determine extra imports needed for interfaces and impls
def get_interface_imports(entity, methods):
    imports = set()
    imports.add("com.baomidou.mybatisplus.extension.service.IService")
    imports.add(f"{MODEL_PKG}.{entity}")
    for m in methods:
        if "List<" in m:
            imports.add("java.util.List")
        if "Page<" in m:
            imports.add("com.baomidou.mybatisplus.extension.plugins.pagination.Page")
        if "Collection<" in m:
            imports.add("java.util.Collection")
        if "Map<" in m:
            imports.add("java.util.Map")
        if "LambdaQueryWrapper" in m:
            imports.add("com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper")
    return sorted(imports)

def get_impl_imports(entity, mapper, methods):
    imports = set()
    imports.add(f"{MAPPER_PKG}.{mapper}")
    imports.add(f"{MODEL_PKG}.{entity}")
    imports.add("com.baomidou.mybatisplus.extension.service.impl.ServiceImpl")
    imports.add("org.springframework.stereotype.Service")
    for sig, body in methods:
        if "List<" in sig or "List<" in body:
            imports.add("java.util.List")
        if "Page<" in sig or "Page<" in body:
            imports.add("com.baomidou.mybatisplus.extension.plugins.pagination.Page")
        if "Collection<" in sig:
            imports.add("java.util.Collection")
        if "Map<" in sig:
            imports.add("java.util.Map")
        if "LambdaQueryWrapper" in body or "LambdaQueryWrapper" in sig:
            imports.add("com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper")
    return sorted(imports)


def gen_interface(entity, mapper, methods):
    imports = get_interface_imports(entity, methods)
    lines = []
    lines.append(f"package {PKG};")
    lines.append("")
    for imp in imports:
        lines.append(f"import {imp};")
    lines.append("")
    lines.append("/**")
    lines.append(f" * {entity} 数据服务接口")
    lines.append(" *")
    lines.append(f" * @author {AUTHOR}")
    lines.append(f" * @date {DATE}")
    lines.append(" */")
    lines.append(f"public interface {entity}Service extends IService<{entity}> {{")
    lines.append("")
    for m in methods:
        lines.append(f"    {m};")
        lines.append("")
    lines.append("}")
    lines.append("")
    return "\n".join(lines)


def gen_impl(entity, mapper, methods):
    imports = get_impl_imports(entity, mapper, methods)
    lines = []
    lines.append(f"package {PKG};")
    lines.append("")
    for imp in imports:
        lines.append(f"import {imp};")
    lines.append("")
    lines.append("/**")
    lines.append(f" * {entity} 数据服务实现类")
    lines.append(" *")
    lines.append(f" * @author {AUTHOR}")
    lines.append(f" * @date {DATE}")
    lines.append(" */")
    lines.append("@Service")
    lines.append(f"public class {entity}ServiceImpl extends ServiceImpl<{mapper}, {entity}> implements {entity}Service {{")
    lines.append("")
    for sig, body in methods:
        lines.append(f"    @Override")
        lines.append(f"    public {sig} {{")
        # indent body lines
        for bline in body.split("\n"):
            lines.append(f"        {bline}")
        lines.append("    }")
        lines.append("")
    lines.append("}")
    lines.append("")
    return "\n".join(lines)


def main():
    os.makedirs(SVC_DIR, exist_ok=True)
    count = 0
    for entity, mapper, iface_methods, impl_methods in ENTRIES:
        # Generate interface
        iface_content = gen_interface(entity, mapper, iface_methods)
        iface_path = os.path.join(SVC_DIR, f"{entity}Service.java")
        with open(iface_path, "w") as f:
            f.write(iface_content)
        print(f"  + {entity}Service.java")
        count += 1

        # Generate impl
        impl_content = gen_impl(entity, mapper, impl_methods)
        impl_path = os.path.join(SVC_DIR, f"{entity}ServiceImpl.java")
        with open(impl_path, "w") as f:
            f.write(impl_content)
        print(f"  + {entity}ServiceImpl.java")
        count += 1

    print(f"\n完成！共生成 {count} 个文件。")


if __name__ == "__main__":
    main()
