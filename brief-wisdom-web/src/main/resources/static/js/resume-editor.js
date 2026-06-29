/**
 * 简历在线编辑器
 * 分步式编辑：Step1 工作经历&技术栈，Step2 项目&成果
 */

const EDITOR_API = '/api/resume/manage';

// 编辑器状态
let editorInitialized = false;
let editorCurrentStep = 1;

// 数据缓存
let editorExperiences = [];
let editorStacks = [];
let editorProjects = [];
let editorAchievements = [];

// 当前选中项
let selectedExpId = null;
let selectedProjId = null;

// ===== 初始化 =====
async function initEditor() {
  if (editorInitialized) return;
  editorInitialized = true;
  await loadAllEditorData();
  renderStep1();
}

async function loadAllEditorData() {
  try {
    const [exps, stacks, projs, achs] = await Promise.all([
      apiRequest('/experiences'),
      apiRequest('/stacks'),
      apiRequest('/projects'),
      apiRequest('/achievements')
    ]);
    editorExperiences = exps || [];
    editorStacks = stacks || [];
    editorProjects = projs || [];
    editorAchievements = achs || [];
  } catch (e) {
    console.error('加载编辑器数据失败:', e);
  }
}

// ===== 步骤切换 =====
function goToStep(step) {
  editorCurrentStep = step;
  document.querySelectorAll('.step-btn').forEach(btn => {
    btn.classList.toggle('active', parseInt(btn.dataset.step) === step);
  });
  if (step === 1) {
    selectedProjId = null;
    renderStep1();
  } else {
    selectedExpId = null;
    renderStep2();
  }
}

// ===== Step 1: 工作经历 & 技术栈 =====
function renderStep1() {
  renderExpList();
  if (selectedExpId) {
    renderExpForm(selectedExpId);
  } else if (editorExperiences.length > 0) {
    selectExperience(editorExperiences[0].id);
  } else {
    showEmptyForm();
  }
}

function renderExpList() {
  const panel = document.getElementById('editor-list-panel');
  const sorted = [...editorExperiences].sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0));
  panel.innerHTML = `
    <div class="editor-list-header">
      <h3>工作经历</h3>
      <button class="editor-btn editor-btn-primary" onclick="addNewExperience()" style="padding:6px 12px;font-size:13px;">+ 新增</button>
    </div>
    <div class="editor-list-body">
      ${sorted.length === 0 ? '<div class="editor-list-empty">暂无工作经历，点击上方新增</div>' :
      sorted.map(exp => `
        <div class="editor-list-item ${exp.id === selectedExpId ? 'active' : ''}"
             onclick="selectExperience(${exp.id})">
          <div class="editor-list-item-title">${esc(exp.title)}</div>
          <div class="editor-list-item-meta">${esc(exp.job)} · 排序:${exp.sortOrder || 0}</div>
        </div>
      `).join('')}
    </div>
  `;
}

function selectExperience(id) {
  selectedExpId = id;
  renderExpList();
  renderExpForm(id);
}

function renderExpForm(id) {
  const exp = editorExperiences.find(e => e.id === id);
  if (!exp) return;
  const expStacks = editorStacks
    .filter(s => s.experienceId === id)
    .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0));

  const formPanel = document.getElementById('editor-form-panel');
  formPanel.innerHTML = `
    <div class="editor-form-title">编辑工作经历</div>
    <div class="editor-form-group">
      <label>职位标题 *</label>
      <input type="text" id="ef-title" value="${escAttr(exp.title)}" placeholder="如：高级前端工程师">
    </div>
    <div class="editor-form-group">
      <label>岗位角色 *</label>
      <input type="text" id="ef-job" value="${escAttr(exp.job)}" placeholder="如：前端技术负责人">
    </div>
    <div class="editor-form-group">
      <label>整体描述</label>
      <textarea id="ef-desc" rows="4" placeholder="描述这段工作经历...">${esc(exp.description || '')}</textarea>
    </div>
    <div class="editor-form-group">
      <label>排序序号</label>
      <input type="number" id="ef-sort" value="${exp.sortOrder || 0}" min="0">
    </div>
    <div class="editor-form-group">
      <label>是否显示</label>
      <select id="ef-visible">
        <option value="1" ${exp.isVisible === 1 ? 'selected' : ''}>显示</option>
        <option value="0" ${exp.isVisible === 0 ? 'selected' : ''}>隐藏</option>
      </select>
    </div>
    <div class="editor-form-actions">
      <button class="editor-btn editor-btn-primary" onclick="saveExperience(${id})">保存</button>
      <button class="editor-btn editor-btn-danger" onclick="deleteExperience(${id})">删除</button>
    </div>

    <!-- 技术栈管理 -->
    <div class="sub-items-section">
      <div class="sub-items-header">
        <h4>技术栈 (${expStacks.length})</h4>
      </div>
      <div id="stack-list">
        ${expStacks.map(s => `
          <div class="sub-item">
            <div class="sub-item-content">${esc(s.techName)}</div>
            <div class="sub-item-actions">
              <button class="sub-item-btn sub-item-btn-delete" onclick="deleteStack(${s.id})">删除</button>
            </div>
          </div>
        `).join('')}
      </div>
      <div class="sub-item-inline-form">
        <input type="text" id="new-stack-input" placeholder="输入技术名称，如：Spring Boot">
        <button class="editor-btn editor-btn-primary" onclick="addStack(${id})" style="padding:8px 14px;">添加</button>
      </div>
    </div>
  `;
}

function showEmptyForm() {
  document.getElementById('editor-form-panel').innerHTML = `
    <div class="editor-empty">
      <div style="text-align:center;">
        <p style="font-size:48px;margin-bottom:16px;">📝</p>
        <p>点击左侧选择或新增工作经历开始编辑</p>
      </div>
    </div>
  `;
}

async function addNewExperience() {
  try {
    const payload = { title: '新工作经历', job: '未设置', description: '', sortOrder: editorExperiences.length, isVisible: 0 };
    const newExp = await apiRequest('/experiences', 'POST', payload);
    editorExperiences.push(newExp);
    selectedExpId = newExp.id;
    renderStep1();
  } catch (e) { /* handled by apiRequest */ }
}

async function saveExperience(id) {
  const payload = {
    title: document.getElementById('ef-title').value.trim() || '未设置',
    job: document.getElementById('ef-job').value.trim() || '未设置',
    description: document.getElementById('ef-desc').value.trim(),
    sortOrder: parseInt(document.getElementById('ef-sort').value) || 0,
    isVisible: parseInt(document.getElementById('ef-visible').value)
  };
  try {
    const updated = await apiRequest(`/experiences/${id}`, 'PUT', payload);
    const idx = editorExperiences.findIndex(e => e.id === id);
    if (idx >= 0) editorExperiences[idx] = updated;
    renderExpList();
    alert('保存成功！');
  } catch (e) { /* handled */ }
}

async function deleteExperience(id) {
  if (!await showConfirmDialog('确定删除该工作经历？关联的技术栈也将被删除。', '🗑️')) return;
  try {
    await apiRequest(`/experiences/${id}`, 'DELETE');
    editorExperiences = editorExperiences.filter(e => e.id !== id);
    editorStacks = editorStacks.filter(s => s.experienceId !== id);
    selectedExpId = editorExperiences.length > 0 ? editorExperiences[0].id : null;
    renderStep1();
  } catch (e) { /* handled */ }
}

// ===== 技术栈管理 =====
async function addStack(expId) {
  const input = document.getElementById('new-stack-input');
  const name = input.value.trim();
  if (!name) { input.focus(); return; }
  try {
    const newStack = await apiRequest('/stacks', 'POST', {
      experienceId: expId, techName: name, sortOrder: 0
    });
    editorStacks.push(newStack);
    renderExpForm(expId);
  } catch (e) { /* handled */ }
}

async function deleteStack(stackId) {
  if (!await showConfirmDialog('确定删除该技术栈？', '🗑️')) return;
  try {
    await apiRequest(`/stacks/${stackId}`, 'DELETE');
    editorStacks = editorStacks.filter(s => s.id !== stackId);
    if (selectedExpId) renderExpForm(selectedExpId);
  } catch (e) { /* handled */ }
}

// ===== Step 2: 项目 & 成果 =====
function renderStep2() {
  renderProjList();
  if (selectedProjId) {
    renderProjForm(selectedProjId);
  } else if (editorProjects.length > 0) {
    selectProject(editorProjects[0].id);
  } else {
    showEmptyProjForm();
  }
}

function renderProjList() {
  const panel = document.getElementById('editor-list-panel');
  const sorted = [...editorProjects].sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0));
  panel.innerHTML = `
    <div class="editor-list-header">
      <h3>项目列表</h3>
      <button class="editor-btn editor-btn-primary" onclick="addNewProject()" style="padding:6px 12px;font-size:13px;">+ 新增</button>
    </div>
    <div class="editor-list-body">
      ${sorted.length === 0 ? '<div class="editor-list-empty">暂无项目，点击上方新增</div>' :
      sorted.map(p => {
        const exp = editorExperiences.find(e => e.id === p.experienceId);
        return `
          <div class="editor-list-item ${p.id === selectedProjId ? 'active' : ''}"
               onclick="selectProject(${p.id})">
            <div class="editor-list-item-title">${esc(p.name)}</div>
            <div class="editor-list-item-meta">${esc(exp ? exp.title.substring(0, 20) : '未关联')} · ${esc(p.lifecycle || '-')}</div>
          </div>
        `;
      }).join('')}
    </div>
  `;
}

function selectProject(id) {
  selectedProjId = id;
  renderProjList();
  renderProjForm(id);
}

function renderProjForm(id) {
  const proj = editorProjects.find(p => p.id === id);
  if (!proj) return;
  const projAchs = editorAchievements
    .filter(a => a.projectId === id)
    .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0));

  const expOptions = editorExperiences.map(e =>
    `<option value="${e.id}" ${proj.experienceId === e.id ? 'selected' : ''}>${esc(e.title.substring(0, 30))}</option>`
  ).join('');

  const formPanel = document.getElementById('editor-form-panel');
  formPanel.innerHTML = `
    <div class="editor-form-title">编辑项目</div>
    <div class="editor-form-group">
      <label>关联工作经历 *</label>
      <select id="pf-expId">${expOptions}</select>
    </div>
    <div class="editor-form-group">
      <label>项目名称 *</label>
      <input type="text" id="pf-name" value="${escAttr(proj.name)}" placeholder="项目名称">
    </div>
    <div class="editor-form-group">
      <label>项目周期</label>
      <input type="text" id="pf-lifecycle" value="${escAttr(proj.lifecycle || '')}" placeholder="如：2024.01 - 2024.12">
    </div>
    <div class="editor-form-group">
      <label>项目背景</label>
      <textarea id="pf-background" rows="3" placeholder="项目背景描述...">${esc(proj.background || '')}</textarea>
    </div>
    <div class="editor-form-group">
      <label>职责描述</label>
      <textarea id="pf-duty" rows="3" placeholder="你在项目中的职责...">${esc(proj.duty || '')}</textarea>
    </div>
    <div class="editor-form-group">
      <label>排序序号</label>
      <input type="number" id="pf-sort" value="${proj.sortOrder || 0}" min="0">
    </div>
    <div class="editor-form-actions">
      <button class="editor-btn editor-btn-primary" onclick="saveProject(${id})">保存</button>
      <button class="editor-btn editor-btn-danger" onclick="deleteProject(${id})">删除</button>
    </div>

    <!-- 项目成果管理 -->
    <div class="sub-items-section">
      <div class="sub-items-header">
        <h4>项目成果 (${projAchs.length})</h4>
      </div>
      <div id="achievement-list">
        ${projAchs.map(a => `
          <div class="sub-item">
            <div class="sub-item-content">${esc(a.content)}</div>
            <div class="sub-item-actions">
              <button class="sub-item-btn sub-item-btn-delete" onclick="deleteAchievement(${a.id})">删除</button>
            </div>
          </div>
        `).join('')}
      </div>
      <div class="sub-item-inline-form">
        <input type="text" id="new-ach-input" placeholder="输入成果内容，如：提升了30%的性能">
        <button class="editor-btn editor-btn-primary" onclick="addAchievement(${id})" style="padding:8px 14px;">添加</button>
      </div>
    </div>
  `;
}

function showEmptyProjForm() {
  document.getElementById('editor-form-panel').innerHTML = `
    <div class="editor-empty">
      <div style="text-align:center;">
        <p style="font-size:48px;margin-bottom:16px;">📋</p>
        <p>点击左侧选择或新增项目开始编辑</p>
      </div>
    </div>
  `;
}

async function addNewProject() {
  if (editorExperiences.length === 0) {
    alert('请先在"工作经历&技术栈"步骤中添加工作经历');
    return;
  }
  try {
    const payload = {
      experienceId: editorExperiences[0].id,
      name: '新项目', lifecycle: '', background: '', duty: '', sortOrder: editorProjects.length
    };
    const newProj = await apiRequest('/projects', 'POST', payload);
    editorProjects.push(newProj);
    selectedProjId = newProj.id;
    renderStep2();
  } catch (e) { /* handled */ }
}

async function saveProject(id) {
  const payload = {
    experienceId: parseInt(document.getElementById('pf-expId').value),
    name: document.getElementById('pf-name').value.trim() || '未设置',
    lifecycle: document.getElementById('pf-lifecycle').value.trim(),
    background: document.getElementById('pf-background').value.trim(),
    duty: document.getElementById('pf-duty').value.trim(),
    sortOrder: parseInt(document.getElementById('pf-sort').value) || 0
  };
  try {
    const updated = await apiRequest(`/projects/${id}`, 'PUT', payload);
    const idx = editorProjects.findIndex(p => p.id === id);
    if (idx >= 0) editorProjects[idx] = updated;
    renderProjList();
    alert('保存成功！');
  } catch (e) { /* handled */ }
}

async function deleteProject(id) {
  if (!await showConfirmDialog('确定删除该项目？关联的项目成果也将被删除。', '🗑️')) return;
  try {
    await apiRequest(`/projects/${id}`, 'DELETE');
    editorProjects = editorProjects.filter(p => p.id !== id);
    editorAchievements = editorAchievements.filter(a => a.projectId !== id);
    selectedProjId = editorProjects.length > 0 ? editorProjects[0].id : null;
    renderStep2();
  } catch (e) { /* handled */ }
}

// ===== 项目成果管理 =====
async function addAchievement(projId) {
  const input = document.getElementById('new-ach-input');
  const content = input.value.trim();
  if (!content) { input.focus(); return; }
  try {
    const newAch = await apiRequest('/achievements', 'POST', {
      projectId: projId, content: content, sortOrder: 0
    });
    editorAchievements.push(newAch);
    renderProjForm(projId);
  } catch (e) { /* handled */ }
}

async function deleteAchievement(achId) {
  if (!await showConfirmDialog('确定删除该成果？', '🗑️')) return;
  try {
    await apiRequest(`/achievements/${achId}`, 'DELETE');
    editorAchievements = editorAchievements.filter(a => a.id !== achId);
    if (selectedProjId) renderProjForm(selectedProjId);
  } catch (e) { /* handled */ }
}

// ===== 工具函数 =====
function esc(str) {
  if (!str) return '';
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}

function escAttr(str) {
  if (!str) return '';
  return str.replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/'/g, '&#39;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

// ===== 简历预览 =====
async function previewResume() {
  const modal = document.getElementById('resume-preview-modal');
  modal.style.display = 'flex';
  await refreshPreview();
}

function closePreview() {
  document.getElementById('resume-preview-modal').style.display = 'none';
}

function openFullResume() {
  window.open('/about.html', '_blank');
}

async function refreshPreview() {
  const body = document.getElementById('resume-preview-body');
  body.innerHTML = '<div class="editor-empty">正在加载简历数据...</div>';
  try {
    const res = await fetch('/api/resume/experiences');
    const result = await res.json();
    if (!result.success) throw new Error(result.error || '加载失败');
    const experiences = result.data || [];
    renderPreview(experiences);
  } catch (e) {
    body.innerHTML = '<div class="preview-empty"><div class="preview-empty-icon">❗</div><p>加载失败: ' + esc(e.message) + '</p></div>';
  }
}

function renderPreview(experiences) {
  const body = document.getElementById('resume-preview-body');
  if (!experiences || experiences.length === 0) {
    body.innerHTML = '<div class="preview-empty"><div class="preview-empty-icon">📄</div><p>暂无简历数据，请先添加工作经历</p></div>';
    return;
  }

  let html = '<div class="preview-header"><div class="preview-name">个人简历</div>';
  html += '<div class="preview-title">工作经历与项目经验</div></div>';

  // 工作经历部分
  html += '<div class="preview-section">';
  html += '<div class="preview-section-title">💼 工作经历与项目经验</div>';

  experiences.forEach(exp => {
    html += '<div class="preview-exp-item">';
    html += '<div class="preview-exp-header">';
    html += '<div><div class="preview-exp-title">' + esc(exp.title) + '</div>';
    html += '<div class="preview-exp-job">' + esc(exp.job) + '</div></div>';
    html += '</div>';

    if (exp.description) {
      html += '<div class="preview-exp-desc">' + esc(exp.description) + '</div>';
    }

    // 项目经验
    if (exp.projects && exp.projects.length > 0) {
      exp.projects.forEach(proj => {
        html += '<div class="preview-project">';
        html += '<div class="preview-project-name">' + esc(proj.name) + '</div>';
        if (proj.lifecycle) {
          html += '<div class="preview-project-lifecycle">📅 ' + esc(proj.lifecycle) + '</div>';
        }
        if (proj.background) {
          html += '<div class="preview-project-bg"><strong>项目背景:</strong> ' + esc(proj.background) + '</div>';
        }
        if (proj.duty) {
          html += '<div class="preview-project-duty"><strong>工作职责:</strong> ' + esc(proj.duty) + '</div>';
        }
        if (proj.achievements && proj.achievements.length > 0) {
          html += '<div><strong>项目成果:</strong></div>';
          html += '<ul class="preview-achievements">';
          proj.achievements.forEach(a => {
            html += '<li>' + esc(a) + '</li>';
          });
          html += '</ul>';
        }
        html += '</div>';
      });
    }

    // 技术栈
    if (exp.stacks && exp.stacks.length > 0) {
      html += '<div class="preview-stacks">';
      exp.stacks.forEach(s => {
        html += '<span class="preview-stack-tag">' + esc(s) + '</span>';
      });
      html += '</div>';
    }

    html += '</div>'; // close preview-exp-item
  });

  html += '</div>'; // close preview-section
  body.innerHTML = html;
}

// 点击遮罩关闭预览
document.addEventListener('click', (e) => {
  if (e.target.id === 'resume-preview-modal') closePreview();
});

// ESC 关闭预览
document.addEventListener('keydown', (e) => {
  if (e.key === 'Escape') {
    const modal = document.getElementById('resume-preview-modal');
    if (modal && modal.style.display !== 'none') closePreview();
  }
});
