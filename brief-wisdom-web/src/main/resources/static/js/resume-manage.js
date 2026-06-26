/**
 * 简历数据管理页面 JS
 */

const API_BASE = '/api/resume/manage';

// 缓存数据
let experiencesCache = [];
let projectsCache = [];

// ===== 初始化 =====
document.addEventListener('DOMContentLoaded', () => {
  initTabs();
  loadExperiences();
  loadProjects();
  loadAchievements();
  loadStacks();
});

// ===== Tab 切换 =====
function initTabs() {
  document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
      document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
      btn.classList.add('active');
      document.getElementById(`${btn.dataset.tab}-tab`).classList.add('active');
    });
  });
}

// ===== 通用 API 请求 =====
async function apiRequest(url, method = 'GET', body = null) {
  try {
    const options = {
      method,
      headers: { 'Content-Type': 'application/json' }
    };
    if (body) options.body = JSON.stringify(body);
    const res = await fetch(API_BASE + url, options);
    const data = await res.json();
    if (!data.success) throw new Error(data.error || '请求失败');
    return data.data;
  } catch (err) {
    console.error('API请求失败:', err);
    alert('操作失败: ' + err.message);
    throw err;
  }
}

// ===== 工作经历 =====
async function loadExperiences() {
  const container = document.getElementById('experiences-list');
  container.innerHTML = '<div class="loading">加载中...</div>';
  try {
    const data = await apiRequest('/experiences');
    experiencesCache = data;
    renderExperiences(data);
    updateFilterOptions();
  } catch (e) {
    container.innerHTML = '<div class="empty-state"><p>加载失败</p></div>';
  }
}

function renderExperiences(list) {
  const container = document.getElementById('experiences-list');
  if (!list || list.length === 0) {
    container.innerHTML = '<div class="empty-state"><p>暂无工作经历数据</p><p>点击"+ 新增工作经历"添加</p></div>';
    return;
  }
  container.innerHTML = list.map(exp => `
    <div class="data-card">
      <div class="data-card-header">
        <div>
          <div class="data-card-title">${escapeHtml(exp.title)}</div>
          <div class="data-card-subtitle">${escapeHtml(exp.job)}</div>
        </div>
        <div class="data-card-actions">
          <button class="btn btn-edit" onclick="editExperience(${exp.id})">编辑</button>
          <button class="btn btn-delete" onclick="deleteExperience(${exp.id})">删除</button>
        </div>
      </div>
      <div class="data-card-body">
        <p>${escapeHtml(exp.description || '')}</p>
      </div>
      <div class="data-card-meta">
        <span class="meta-item">排序: ${exp.sortOrder}</span>
        <span class="meta-item">
          <span class="badge ${exp.isVisible === 1 ? 'badge-visible' : 'badge-hidden'}">
            ${exp.isVisible === 1 ? '显示' : '隐藏'}
          </span>
        </span>
      </div>
    </div>
  `).join('');
}

function showExperienceForm(data = null) {
  const isEdit = !!data;
  openModal(isEdit ? '编辑工作经历' : '新增工作经历', `
    <div class="form-group">
      <label>职位标题 *</label>
      <input type="text" id="f-title" value="${escapeAttr(data?.title || '')}" required>
    </div>
    <div class="form-group">
      <label>岗位角色 *</label>
      <input type="text" id="f-job" value="${escapeAttr(data?.job || '')}" required>
    </div>
    <div class="form-group">
      <label>整体描述</label>
      <textarea id="f-description" rows="4">${escapeHtml(data?.description || '')}</textarea>
    </div>
    <div class="form-group">
      <label>排序序号</label>
      <input type="number" id="f-sortOrder" value="${data?.sortOrder ?? 0}">
    </div>
    <div class="form-group">
      <label>是否显示</label>
      <select id="f-isVisible">
        <option value="1" ${data?.isVisible === 1 ? 'selected' : ''}>显示</option>
        <option value="0" ${data?.isVisible === 0 ? 'selected' : ''}>隐藏</option>
      </select>
    </div>
    <div class="form-actions">
      <button type="button" class="btn btn-cancel" onclick="closeModal()">取消</button>
      <button type="submit" class="btn btn-primary">${isEdit ? '保存' : '创建'}</button>
    </div>
  `, async (formData) => {
    const payload = {
      title: formData.get('title'),
      job: formData.get('job'),
      description: formData.get('description'),
      sortOrder: parseInt(formData.get('sortOrder')) || 0,
      isVisible: parseInt(formData.get('isVisible'))
    };
    if (isEdit) {
      await apiRequest(`/experiences/${data.id}`, 'PUT', payload);
    } else {
      await apiRequest('/experiences', 'POST', payload);
    }
    closeModal();
    loadExperiences();
  });

  document.getElementById('modal-form').onsubmit = async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const payload = {
      title: fd.get('title'),
      job: fd.get('job'),
      description: fd.get('description'),
      sortOrder: parseInt(fd.get('sortOrder')) || 0,
      isVisible: parseInt(fd.get('isVisible'))
    };
    if (isEdit) {
      await apiRequest(`/experiences/${data.id}`, 'PUT', payload);
    } else {
      await apiRequest('/experiences', 'POST', payload);
    }
    closeModal();
    loadExperiences();
  };
}

function editExperience(id) {
  const exp = experiencesCache.find(e => e.id === id);
  if (exp) showExperienceForm(exp);
}

async function deleteExperience(id) {
  if (!confirm('确定删除该工作经历？删除后关联的项目、成果、技术栈也将被删除。')) return;
  await apiRequest(`/experiences/${id}`, 'DELETE');
  loadExperiences();
}

// ===== 项目 =====
async function loadProjects() {
  const container = document.getElementById('projects-list');
  container.innerHTML = '<div class="loading">加载中...</div>';
  const expId = document.getElementById('project-experience-filter').value;
  try {
    const url = expId ? `/projects?experienceId=${expId}` : '/projects';
    const data = await apiRequest(url);
    projectsCache = data;
    renderProjects(data);
  } catch (e) {
    container.innerHTML = '<div class="empty-state"><p>加载失败</p></div>';
  }
}

function renderProjects(list) {
  const container = document.getElementById('projects-list');
  if (!list || list.length === 0) {
    container.innerHTML = '<div class="empty-state"><p>暂无项目数据</p><p>点击"+ 新增项目"添加</p></div>';
    return;
  }
  container.innerHTML = list.map(p => `
    <div class="data-card">
      <div class="data-card-header">
        <div>
          <div class="data-card-title">${escapeHtml(p.name)}</div>
          <div class="data-card-subtitle">周期: ${escapeHtml(p.lifecycle || '未设置')}</div>
        </div>
        <div class="data-card-actions">
          <button class="btn btn-edit" onclick="editProject(${p.id})">编辑</button>
          <button class="btn btn-delete" onclick="deleteProject(${p.id})">删除</button>
        </div>
      </div>
      <div class="data-card-body">
        <p><strong>背景:</strong> ${escapeHtml(p.background || '')}</p>
        <p><strong>职责:</strong> ${escapeHtml(p.duty || '')}</p>
      </div>
      <div class="data-card-meta">
        <span class="meta-item">经历ID: ${p.experienceId}</span>
        <span class="meta-item">排序: ${p.sortOrder}</span>
      </div>
    </div>
  `).join('');
}

async function showProjectForm(data = null) {
  const isEdit = !!data;
  await loadExperiencesForFilter();
  const expOptions = experiencesCache.map(e =>
    `<option value="${e.id}" ${data?.experienceId === e.id ? 'selected' : ''}>${escapeHtml(e.title.substring(0, 40))}</option>`
  ).join('');

  openModal(isEdit ? '编辑项目' : '新增项目', `
    <div class="form-group">
      <label>关联工作经历 *</label>
      <select id="f-experienceId" required>${expOptions}</select>
    </div>
    <div class="form-group">
      <label>项目名称 *</label>
      <input type="text" id="f-name" value="${escapeAttr(data?.name || '')}" required>
    </div>
    <div class="form-group">
      <label>项目周期</label>
      <input type="text" id="f-lifecycle" value="${escapeAttr(data?.lifecycle || '')}" placeholder="如: 2024.12 - 2025.10">
    </div>
    <div class="form-group">
      <label>项目背景</label>
      <textarea id="f-background" rows="3">${escapeHtml(data?.background || '')}</textarea>
    </div>
    <div class="form-group">
      <label>职责描述</label>
      <textarea id="f-duty" rows="3">${escapeHtml(data?.duty || '')}</textarea>
    </div>
    <div class="form-group">
      <label>排序序号</label>
      <input type="number" id="f-sortOrder" value="${data?.sortOrder ?? 0}">
    </div>
    <div class="form-actions">
      <button type="button" class="btn btn-cancel" onclick="closeModal()">取消</button>
      <button type="submit" class="btn btn-primary">${isEdit ? '保存' : '创建'}</button>
    </div>
  `);

  document.getElementById('modal-form').onsubmit = async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const payload = {
      experienceId: parseInt(fd.get('experienceId')),
      name: fd.get('name'),
      lifecycle: fd.get('lifecycle'),
      background: fd.get('background'),
      duty: fd.get('duty'),
      sortOrder: parseInt(fd.get('sortOrder')) || 0
    };
    if (isEdit) {
      await apiRequest(`/projects/${data.id}`, 'PUT', payload);
    } else {
      await apiRequest('/projects', 'POST', payload);
    }
    closeModal();
    loadProjects();
  };
}

function editProject(id) {
  const p = projectsCache.find(e => e.id === id);
  if (p) showProjectForm(p);
}

async function deleteProject(id) {
  if (!confirm('确定删除该项目？删除后关联的项目成果也将被删除。')) return;
  await apiRequest(`/projects/${id}`, 'DELETE');
  loadProjects();
}

// ===== 项目成果 =====
async function loadAchievements() {
  const container = document.getElementById('achievements-list');
  container.innerHTML = '<div class="loading">加载中...</div>';
  const projectId = document.getElementById('achievement-project-filter').value;
  try {
    const url = projectId ? `/achievements?projectId=${projectId}` : '/achievements';
    const data = await apiRequest(url);
    renderAchievements(data);
  } catch (e) {
    container.innerHTML = '<div class="empty-state"><p>加载失败</p></div>';
  }
}

function renderAchievements(list) {
  const container = document.getElementById('achievements-list');
  if (!list || list.length === 0) {
    container.innerHTML = '<div class="empty-state"><p>暂无项目成果数据</p><p>点击"+ 新增项目成果"添加</p></div>';
    return;
  }
  container.innerHTML = list.map(a => `
    <div class="data-card">
      <div class="data-card-header">
        <div>
          <div class="data-card-title">${escapeHtml(a.content)}</div>
        </div>
        <div class="data-card-actions">
          <button class="btn btn-edit" onclick="editAchievement(${a.id})">编辑</button>
          <button class="btn btn-delete" onclick="deleteAchievement(${a.id})">删除</button>
        </div>
      </div>
      <div class="data-card-meta">
        <span class="meta-item">项目ID: ${a.projectId}</span>
        <span class="meta-item">排序: ${a.sortOrder}</span>
      </div>
    </div>
  `).join('');
}

async function showAchievementForm(data = null) {
  const isEdit = !!data;
  await loadProjectsForFilter();
  const projOptions = projectsCache.map(p =>
    `<option value="${p.id}" ${data?.projectId === p.id ? 'selected' : ''}>${escapeHtml(p.name)}</option>`
  ).join('');

  openModal(isEdit ? '编辑项目成果' : '新增项目成果', `
    <div class="form-group">
      <label>关联项目 *</label>
      <select id="f-projectId" required>${projOptions}</select>
    </div>
    <div class="form-group">
      <label>成果内容 *</label>
      <textarea id="f-content" rows="3" required>${escapeHtml(data?.content || '')}</textarea>
    </div>
    <div class="form-group">
      <label>排序序号</label>
      <input type="number" id="f-sortOrder" value="${data?.sortOrder ?? 0}">
    </div>
    <div class="form-actions">
      <button type="button" class="btn btn-cancel" onclick="closeModal()">取消</button>
      <button type="submit" class="btn btn-primary">${isEdit ? '保存' : '创建'}</button>
    </div>
  `);

  document.getElementById('modal-form').onsubmit = async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const payload = {
      projectId: parseInt(fd.get('projectId')),
      content: fd.get('content'),
      sortOrder: parseInt(fd.get('sortOrder')) || 0
    };
    if (isEdit) {
      await apiRequest(`/achievements/${data.id}`, 'PUT', payload);
    } else {
      await apiRequest('/achievements', 'POST', payload);
    }
    closeModal();
    loadAchievements();
  };
}

async function editAchievement(id) {
  try {
    const data = await apiRequest(`/achievements/${id}`);
    showAchievementForm(data);
  } catch (e) { /* error handled */ }
}

async function deleteAchievement(id) {
  if (!confirm('确定删除该项目成果？')) return;
  await apiRequest(`/achievements/${id}`, 'DELETE');
  loadAchievements();
}

// ===== 技术栈 =====
async function loadStacks() {
  const container = document.getElementById('stacks-list');
  container.innerHTML = '<div class="loading">加载中...</div>';
  const expId = document.getElementById('stack-experience-filter').value;
  try {
    const url = expId ? `/stacks?experienceId=${expId}` : '/stacks';
    const data = await apiRequest(url);
    renderStacks(data);
  } catch (e) {
    container.innerHTML = '<div class="empty-state"><p>加载失败</p></div>';
  }
}

function renderStacks(list) {
  const container = document.getElementById('stacks-list');
  if (!list || list.length === 0) {
    container.innerHTML = '<div class="empty-state"><p>暂无技术栈数据</p><p>点击"+ 新增技术栈"添加</p></div>';
    return;
  }
  container.innerHTML = list.map(s => `
    <div class="data-card">
      <div class="data-card-header">
        <div>
          <div class="data-card-title">${escapeHtml(s.techName)}</div>
        </div>
        <div class="data-card-actions">
          <button class="btn btn-edit" onclick="editStack(${s.id})">编辑</button>
          <button class="btn btn-delete" onclick="deleteStack(${s.id})">删除</button>
        </div>
      </div>
      <div class="data-card-meta">
        <span class="meta-item">经历ID: ${s.experienceId}</span>
        <span class="meta-item">排序: ${s.sortOrder}</span>
      </div>
    </div>
  `).join('');
}

async function showStackForm(data = null) {
  const isEdit = !!data;
  await loadExperiencesForFilter();
  const expOptions = experiencesCache.map(e =>
    `<option value="${e.id}" ${data?.experienceId === e.id ? 'selected' : ''}>${escapeHtml(e.title.substring(0, 40))}</option>`
  ).join('');

  openModal(isEdit ? '编辑技术栈' : '新增技术栈', `
    <div class="form-group">
      <label>关联工作经历 *</label>
      <select id="f-experienceId" required>${expOptions}</select>
    </div>
    <div class="form-group">
      <label>技术名称 *</label>
      <input type="text" id="f-techName" value="${escapeAttr(data?.techName || '')}" required>
    </div>
    <div class="form-group">
      <label>排序序号</label>
      <input type="number" id="f-sortOrder" value="${data?.sortOrder ?? 0}">
    </div>
    <div class="form-actions">
      <button type="button" class="btn btn-cancel" onclick="closeModal()">取消</button>
      <button type="submit" class="btn btn-primary">${isEdit ? '保存' : '创建'}</button>
    </div>
  `);

  document.getElementById('modal-form').onsubmit = async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const payload = {
      experienceId: parseInt(fd.get('experienceId')),
      techName: fd.get('techName'),
      sortOrder: parseInt(fd.get('sortOrder')) || 0
    };
    if (isEdit) {
      await apiRequest(`/stacks/${data.id}`, 'PUT', payload);
    } else {
      await apiRequest('/stacks', 'POST', payload);
    }
    closeModal();
    loadStacks();
  };
}

async function editStack(id) {
  try {
    const data = await apiRequest(`/stacks/${id}`);
    showStackForm(data);
  } catch (e) { /* error handled */ }
}

async function deleteStack(id) {
  if (!confirm('确定删除该技术栈？')) return;
  await apiRequest(`/stacks/${id}`, 'DELETE');
  loadStacks();
}

// ===== 筛选器选项更新 =====
async function loadExperiencesForFilter() {
  if (experiencesCache.length === 0) {
    experiencesCache = await apiRequest('/experiences');
  }
}

async function loadProjectsForFilter() {
  if (projectsCache.length === 0) {
    projectsCache = await apiRequest('/projects');
  }
}

function updateFilterOptions() {
  // 项目筛选-工作经历
  const projFilter = document.getElementById('project-experience-filter');
  const projVal = projFilter.value;
  projFilter.innerHTML = '<option value="">全部</option>' +
    experiencesCache.map(e => `<option value="${e.id}">${escapeHtml(e.title.substring(0, 30))}</option>`).join('');
  projFilter.value = projVal;

  // 技术栈筛选-工作经历
  const stackFilter = document.getElementById('stack-experience-filter');
  const stackVal = stackFilter.value;
  stackFilter.innerHTML = '<option value="">全部</option>' +
    experiencesCache.map(e => `<option value="${e.id}">${escapeHtml(e.title.substring(0, 30))}</option>`).join('');
  stackFilter.value = stackVal;

  // 项目成果筛选-项目
  loadProjectsForFilter().then(() => {
    const achFilter = document.getElementById('achievement-project-filter');
    const achVal = achFilter.value;
    achFilter.innerHTML = '<option value="">全部</option>' +
      projectsCache.map(p => `<option value="${p.id}">${escapeHtml(p.name)}</option>`).join('');
    achFilter.value = achVal;
  });
}

// ===== Modal 通用 =====
function openModal(title, formHtml, onSubmit = null) {
  document.getElementById('modal-title').textContent = title;
  document.getElementById('modal-form').innerHTML = formHtml;
  document.getElementById('modal').style.display = 'flex';
  if (onSubmit) {
    document.getElementById('modal-form').onsubmit = onSubmit;
  }
}

function closeModal() {
  document.getElementById('modal').style.display = 'none';
}

// 点击遮罩关闭
document.addEventListener('click', (e) => {
  if (e.target.id === 'modal') closeModal();
});

// ESC 关闭
document.addEventListener('keydown', (e) => {
  if (e.key === 'Escape') closeModal();
});

// ===== 工具函数 =====
function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function escapeAttr(str) {
  if (!str) return '';
  return str.replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}
