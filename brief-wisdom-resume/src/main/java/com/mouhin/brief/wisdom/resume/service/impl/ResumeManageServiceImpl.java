package com.mouhin.brief.wisdom.resume.service.impl;

import com.mouhin.brief.wisdom.common.resume.ProjectAchievementDTO;
import com.mouhin.brief.wisdom.common.resume.ProjectDTO;
import com.mouhin.brief.wisdom.common.resume.WorkExperienceDTO;
import com.mouhin.brief.wisdom.common.resume.WorkExperienceStackDTO;
import com.mouhin.brief.wisdom.constants.CachePrefix;
import com.mouhin.brief.wisdom.persistence.model.Project;
import com.mouhin.brief.wisdom.persistence.model.ProjectAchievement;
import com.mouhin.brief.wisdom.persistence.model.WorkExperience;
import com.mouhin.brief.wisdom.persistence.model.WorkExperienceStack;
import com.mouhin.brief.wisdom.persistence.repository.ProjectAchievementRepository;
import com.mouhin.brief.wisdom.persistence.repository.ProjectRepository;
import com.mouhin.brief.wisdom.persistence.repository.WorkExperienceRepository;
import com.mouhin.brief.wisdom.persistence.repository.WorkExperienceStackRepository;
import com.mouhin.brief.wisdom.resume.service.ResumeManageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 简历数据管理服务实现（CRUD）
 *
 * @author Brief-Wisdom
 * @date 2026-07-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeManageServiceImpl implements ResumeManageService {

    private final WorkExperienceRepository workExperienceRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAchievementRepository projectAchievementRepository;
    private final WorkExperienceStackRepository workExperienceStackRepository;

    // ========== 工作经历 ==========

    @Override
    public List<WorkExperienceDTO> listExperiences() {
        return workExperienceRepository.findAllOrderBySortOrderAsc().stream().map(this::toExpDTO).toList();
    }

    @Override
    public WorkExperienceDTO getExperience(Long id) {
        WorkExperience e = workExperienceRepository.findById(id);
        return e != null ? toExpDTO(e) : null;
    }

    @Override
    @CacheEvict(value = CachePrefix.RESUME_EXPERIENCES_CACHE, allEntries = true)
    @Transactional
    public WorkExperienceDTO createExperience(WorkExperience experience) {
        workExperienceRepository.save(experience);
        return toExpDTO(experience);
    }

    @Override
    @CacheEvict(value = CachePrefix.RESUME_EXPERIENCES_CACHE, allEntries = true)
    @Transactional
    public WorkExperienceDTO updateExperience(WorkExperience experience) {
        workExperienceRepository.update(experience);
        return toExpDTO(workExperienceRepository.findById(experience.getId()));
    }

    @Override
    @CacheEvict(value = CachePrefix.RESUME_EXPERIENCES_CACHE, allEntries = true)
    @Transactional
    public void deleteExperience(Long id) {
        workExperienceRepository.deleteById(id);
    }

    // ========== 项目 ==========

    @Override
    public List<ProjectDTO> listProjects() {
        return projectRepository.findAllOrderBySortOrderAsc().stream().map(this::toProjDTO).toList();
    }

    @Override
    public List<ProjectDTO> listProjectsByExperienceId(Long experienceId) {
        return projectRepository.findByExperienceIdOrderBySortOrderAsc(experienceId).stream().map(this::toProjDTO).toList();
    }

    @Override
    public ProjectDTO getProject(Long id) {
        Project p = projectRepository.findById(id);
        return p != null ? toProjDTO(p) : null;
    }

    @Override
    @CacheEvict(value = CachePrefix.RESUME_EXPERIENCES_CACHE, allEntries = true)
    @Transactional
    public ProjectDTO createProject(Project project) {
        projectRepository.save(project);
        return toProjDTO(project);
    }

    @Override
    @CacheEvict(value = CachePrefix.RESUME_EXPERIENCES_CACHE, allEntries = true)
    @Transactional
    public ProjectDTO updateProject(Project project) {
        projectRepository.update(project);
        return toProjDTO(projectRepository.findById(project.getId()));
    }

    @Override
    @CacheEvict(value = CachePrefix.RESUME_EXPERIENCES_CACHE, allEntries = true)
    @Transactional
    public void deleteProject(Long id) {
        projectRepository.deleteById(id);
    }

    // ========== 项目成果 ==========

    @Override
    public List<ProjectAchievementDTO> listAchievements() {
        return projectAchievementRepository.findAllOrderBySortOrderAsc().stream().map(this::toAchDTO).toList();
    }

    @Override
    public List<ProjectAchievementDTO> listAchievementsByProjectId(Long projectId) {
        return projectAchievementRepository.findByProjectIdOrderBySortOrderAsc(projectId).stream().map(this::toAchDTO).toList();
    }

    @Override
    public ProjectAchievementDTO getAchievement(Long id) {
        ProjectAchievement a = projectAchievementRepository.findById(id);
        return a != null ? toAchDTO(a) : null;
    }

    @Override
    @CacheEvict(value = CachePrefix.RESUME_EXPERIENCES_CACHE, allEntries = true)
    @Transactional
    public ProjectAchievementDTO createAchievement(ProjectAchievement achievement) {
        projectAchievementRepository.save(achievement);
        return toAchDTO(achievement);
    }

    @Override
    @CacheEvict(value = CachePrefix.RESUME_EXPERIENCES_CACHE, allEntries = true)
    @Transactional
    public ProjectAchievementDTO updateAchievement(ProjectAchievement achievement) {
        projectAchievementRepository.update(achievement);
        return toAchDTO(projectAchievementRepository.findById(achievement.getId()));
    }

    @Override
    @CacheEvict(value = CachePrefix.RESUME_EXPERIENCES_CACHE, allEntries = true)
    @Transactional
    public void deleteAchievement(Long id) {
        projectAchievementRepository.deleteById(id);
    }

    // ========== 技术栈 ==========

    @Override
    public List<WorkExperienceStackDTO> listStacks() {
        return workExperienceStackRepository.findAllOrderBySortOrderAsc().stream().map(this::toStackDTO).toList();
    }

    @Override
    public List<WorkExperienceStackDTO> listStacksByExperienceId(Long experienceId) {
        return workExperienceStackRepository.findByExperienceIdOrderBySortOrderAsc(experienceId).stream().map(this::toStackDTO).toList();
    }

    @Override
    public WorkExperienceStackDTO getStack(Long id) {
        WorkExperienceStack s = workExperienceStackRepository.findById(id);
        return s != null ? toStackDTO(s) : null;
    }

    @Override
    @CacheEvict(value = CachePrefix.RESUME_EXPERIENCES_CACHE, allEntries = true)
    @Transactional
    public WorkExperienceStackDTO createStack(WorkExperienceStack stack) {
        workExperienceStackRepository.save(stack);
        return toStackDTO(stack);
    }

    @Override
    @CacheEvict(value = CachePrefix.RESUME_EXPERIENCES_CACHE, allEntries = true)
    @Transactional
    public WorkExperienceStackDTO updateStack(WorkExperienceStack stack) {
        workExperienceStackRepository.update(stack);
        return toStackDTO(workExperienceStackRepository.findById(stack.getId()));
    }

    @Override
    @CacheEvict(value = CachePrefix.RESUME_EXPERIENCES_CACHE, allEntries = true)
    @Transactional
    public void deleteStack(Long id) {
        workExperienceStackRepository.deleteById(id);
    }

    // ========== 转换方法 ==========

    private WorkExperienceDTO toExpDTO(WorkExperience e) {
        WorkExperienceDTO dto = new WorkExperienceDTO();
        dto.setId(e.getId());
        dto.setTitle(e.getTitle());
        dto.setJob(e.getJob());
        dto.setDescription(e.getDescription());
        dto.setSortOrder(e.getSortOrder());
        dto.setIsVisible(e.getIsVisible());
        dto.setCreateTime(e.getCreateTime());
        dto.setUpdateTime(e.getUpdateTime());
        return dto;
    }

    private ProjectDTO toProjDTO(Project p) {
        ProjectDTO dto = new ProjectDTO();
        dto.setId(p.getId());
        dto.setExperienceId(p.getExperienceId());
        dto.setName(p.getName());
        dto.setLifecycle(p.getLifecycle());
        dto.setBackground(p.getBackground());
        dto.setDuty(p.getDuty());
        dto.setSortOrder(p.getSortOrder());
        dto.setCreateTime(p.getCreateTime());
        dto.setUpdateTime(p.getUpdateTime());
        return dto;
    }

    private ProjectAchievementDTO toAchDTO(ProjectAchievement a) {
        ProjectAchievementDTO dto = new ProjectAchievementDTO();
        dto.setId(a.getId());
        dto.setProjectId(a.getProjectId());
        dto.setContent(a.getContent());
        dto.setSortOrder(a.getSortOrder());
        dto.setCreateTime(a.getCreateTime());
        dto.setUpdateTime(a.getUpdateTime());
        return dto;
    }

    private WorkExperienceStackDTO toStackDTO(WorkExperienceStack s) {
        WorkExperienceStackDTO dto = new WorkExperienceStackDTO();
        dto.setId(s.getId());
        dto.setExperienceId(s.getExperienceId());
        dto.setTechName(s.getTechName());
        dto.setSortOrder(s.getSortOrder());
        dto.setCreateTime(s.getCreateTime());
        dto.setUpdateTime(s.getUpdateTime());
        return dto;
    }
}
