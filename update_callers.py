#!/usr/bin/env python3
"""
批量更新调用方：将 Repository 引用替换为新的 Service 引用。
同时处理方法名映射：findById→getById, update→updateById, deleteById→removeById
"""
import os

SRC_DIR = "brief-wisdom-persistence/src/main/java/com/mouhin/brief/wisdom/persistence/service"
PKG = "com.mouhin.brief.wisdom.persistence.service"
MAPPER_PKG = "com.mouhin.brief.wisdom.persistence.mapper"
MODEL_PKG = "com.mouhin.brief.wisdom.persistence.model"
DATE = "2026-06-30"
AUTHOR = "Brief-Wisdom"

# ===== 替换映射 =====

# 类型名替换 (PascalCase)
TYPE_REPLACEMENTS = {
    "AiModelRepository": "AiModelService",
    "ChatMessageRepository": "ChatMessageService",
    "ChatSessionRepository": "ChatSessionService",
    "ChatUserRepository": "ChatUserService",
    "KnowledgeBaseRepository": "KnowledgeBaseService",
    "KnowledgeDocumentRepository": "KnowledgeDocumentService",
    "ProjectRepository": "ProjectService",
    "ProjectAchievementRepository": "ProjectAchievementService",
    "RoleMenuRepository": "RoleMenuService",
    "SysMenuRepository": "SysMenuService",
    "SysRoleRepository": "SysRoleService",
    "UserOauthRepository": "UserOauthService",
    "UserRoleRepository": "UserRoleService",
    "WorkExperienceRepository": "WorkExperienceService",
    "WorkExperienceStackRepository": "WorkExperienceStackService",
}

# 变量名替换 (camelCase) - 按长度降序排列，避免子串冲突
VAR_REPLACEMENTS = [
    ("knowledgeDocumentRepository", "knowledgeDocumentService"),
    ("workExperienceStackRepository", "workExperienceStackService"),
    ("workExperienceRepository", "workExperienceService"),
    ("projectAchievementRepository", "projectAchievementService"),
    ("knowledgeBaseRepository", "knowledgeBaseService"),
    ("chatMessageRepository", "chatMessageService"),
    ("chatSessionRepository", "chatSessionService"),
    ("chatUserRepository", "chatUserService"),
    ("aiModelRepository", "aiModelService"),
    ("userOauthRepository", "userOauthService"),
    ("userRoleRepository", "userRoleService"),
    ("sysRoleRepository", "sysRoleService"),
    ("sysMenuRepository", "sysMenuService"),
    ("roleMenuRepository", "roleMenuService"),
    ("projectRepository", "projectService"),
    # 短别名（AiAgentService 中使用的缩写变量名）
    ("messageRepository", "messageService"),
    ("userRepository", "userService"),
    ("sessionRepository", "sessionService"),
]

# 方法名替换（IService 标准方法映射）
METHOD_REPLACEMENTS = [
    (".findById(", ".getById("),
    (".update(", ".updateById("),
    (".deleteById(", ".removeById("),
]


def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    original = content

    # 1. 替换类型名 (PascalCase, 不影响 camelCase 变量名)
    for old, new in TYPE_REPLACEMENTS.items():
        content = content.replace(old, new)

    # 2. 替换变量名 (已按长度降序排列，避免子串冲突)
    for old, new in VAR_REPLACEMENTS:
        content = content.replace(old, new)

    # 3. 替换方法名
    for old, new in METHOD_REPLACEMENTS:
        content = content.replace(old, new)

    if content != original:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        return True
    return False


def main():
    base = "src/main/java"
    updated = []
    skipped = []

    for root, dirs, files in os.walk("."):
        for f in files:
            if not f.endswith(".java"):
                continue
            filepath = os.path.join(root, f)

            # 跳过 target 目录
            if "/target/" in filepath:
                continue

            # 跳过旧的 Repository 文件（将被删除）
            if "/persistence/repository/" in filepath:
                continue

            # 跳过新生成的 Service 文件
            if "/persistence/service/" in filepath:
                continue

            # 处理文件
            if process_file(filepath):
                updated.append(filepath)
            else:
                # 检查是否包含旧的 Repository import
                with open(filepath, 'r', encoding='utf-8') as fh:
                    if "persistence.repository" in fh.read():
                        skipped.append(filepath)

    print(f"已更新 {len(updated)} 个文件:")
    for f in sorted(updated):
        print(f"  ✓ {f}")

    if skipped:
        print(f"\n⚠ 以下文件仍包含 Repository 引用（可能需要手动检查）:")
        for f in sorted(skipped):
            print(f"  ? {f}")


if __name__ == "__main__":
    main()
