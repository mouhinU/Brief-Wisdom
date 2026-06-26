/**
 * About 页面交互逻辑
 * - 深色/浅色主题切换（localStorage 持久化）
 * - 项目经历从 JSON 动态加载 + 折叠/展开
 * - PDF 导出（基于浏览器打印）
 */

// ===== 主题切换 =====
function initTheme() {
  const savedTheme = localStorage.getItem('theme') || 'light';
  document.documentElement.setAttribute('data-theme', savedTheme);
  updateThemeIcon(savedTheme);
}

function toggleTheme() {
  const current = document.documentElement.getAttribute('data-theme');
  const next = current === 'light' ? 'dark' : 'light';
  document.documentElement.setAttribute('data-theme', next);
  updateThemeIcon(next);
  localStorage.setItem('theme', next);
}

function updateThemeIcon(theme) {
  const btn = document.getElementById('themeToggle');
  if (btn) {
    btn.textContent = theme === 'dark' ? '☀️' : '🌙';
  }
}

// ===== PDF 导出 =====
function exportPDF() {
  alert('提示：在打印设置中，建议选择"背景图形"选项以获得最佳效果。建议缩放 50%~70%。');

  const currentTheme = document.documentElement.getAttribute('data-theme');
  document.documentElement.setAttribute('data-theme', 'light');

  // 展开所有折叠内容
  document.querySelectorAll('.phase-content.collapsed').forEach(el => {
    el.classList.remove('collapsed');
    el.style.maxHeight = el.scrollHeight + 'px';
  });
  document.querySelectorAll('.project-content.collapsed').forEach(el => {
    el.classList.remove('collapsed');
    el.style.maxHeight = el.scrollHeight + 'px';
  });

  setTimeout(() => {
    window.print();
    setTimeout(() => {
      document.documentElement.setAttribute('data-theme', currentTheme);
    }, 1000);
  }, 500);
}

// ===== 项目数据加载 =====
async function loadProjects() {
  try {
    const response = await fetch('/api/resume/experiences');
    const result = await response.json();
    if (!result.success) {
      throw new Error(result.error || '接口返回失败');
    }
    const phases = result.data;
    const container = document.getElementById('projects-container');
    if (!container) return;
    container.innerHTML = '';

    phases.forEach((phase) => {
      container.appendChild(createPhaseElement(phase));
    });

    // 初始化折叠状态（项目层折叠，阶段层展开）
    initCollapseState();
    console.log(`已加载 ${phases.length} 个阶段的项目经历`);
  } catch (error) {
    console.error('加载项目数据失败:', error);
    const container = document.getElementById('projects-container');
    if (container) {
      container.innerHTML = '<div class="loading"><p>项目数据加载失败，请刷新页面重试。</p></div>';
    }
  }
}

function createPhaseElement(phase) {
  const div = document.createElement('div');
  div.className = 'item';

  const dateMatch = phase.title.match(/\d{4}\.\d{2}\s*-\s*\d{4}\.\d{2}/);
  const date = dateMatch ? dateMatch[0] : '';
  const cleanTitle = phase.title.replace(/\d{4}\.\d{2}\s*-\s*\d{4}\.\d{2}/g, '').trim();

  // 项目区块
  let projectsHtml = '';
  if (phase.projects && phase.projects.length > 0) {
    projectsHtml = phase.projects.map(project => {
      const achievementsHtml = project.achievements && project.achievements.length > 0
        ? `<p><strong>项目成果:</strong></p>
           <ul class="project-achievements">
             ${project.achievements.map(a => `<li>${a}</li>`).join('')}
           </ul>`
        : '';

      return `
        <div class="project-section">
          <div class="project-header" onclick="toggleProject(this)">
            <div class="project-title-container">
              <h4>${project.name}</h4>
              <span class="project-lifecycle">${project.lifecycle}</span>
            </div>
            <span class="toggle-icon">▶</span>
          </div>
          <div class="project-content collapsed">
            <p><strong>项目背景:</strong> ${project.background}</p>
            <p><strong>工作职责:</strong> ${project.duty}</p>
            ${achievementsHtml}
          </div>
        </div>`;
    }).join('');
  }

  // 技术栈
  let stacksHtml = '';
  if (phase.stacks && phase.stacks.length > 0) {
    stacksHtml = `<div class="tech-stack">${
      phase.stacks.map(s => {
        let cls = 'tech-tag';
        if (s.toLowerCase().includes('java')) cls += ' java-tag';
        if (s.toLowerCase().includes('spring')) cls += ' spring-tag';
        return `<span class="${cls}">${s}</span>`;
      }).join('')
    }</div>`;
  }

  div.innerHTML = `
    <div class="phase-header" onclick="togglePhase(this)">
      <div class="phase-title-container">
        <div>
          <div class="item-title">${cleanTitle}</div>
          <div class="item-subtitle">${phase.job}</div>
        </div>
        <div style="display:flex;align-items:center;gap:15px;">
          ${date ? `<div class="item-date">${date}</div>` : ''}
          <span class="phase-toggle-icon">▼</span>
        </div>
      </div>
    </div>
    ${phase.description ? `<div class="phase-summary"><p><strong>阶段性总结:</strong> ${phase.description}</p></div>` : ''}
    <div class="phase-content">
      ${projectsHtml}
      ${stacksHtml}
    </div>`;

  return div;
}

// ===== 折叠/展开 =====
function toggleProject(header) {
  const section = header.closest('.project-section');
  const content = section.querySelector('.project-content');
  const icon = header.querySelector('.toggle-icon');

  if (content.classList.contains('collapsed')) {
    content.classList.remove('collapsed');
    content.style.maxHeight = content.scrollHeight + 'px';
    icon.textContent = '▼';
  } else {
    content.classList.add('collapsed');
    content.style.maxHeight = '0';
    icon.textContent = '▶';
  }
}

function togglePhase(header) {
  const phaseEl = header.closest('.item');
  const content = phaseEl.querySelector('.phase-content');
  const icon = header.querySelector('.phase-toggle-icon');

  if (content.classList.contains('collapsed')) {
    content.classList.remove('collapsed');
    content.style.maxHeight = content.scrollHeight + 'px';
    icon.textContent = '▼';
  } else {
    content.classList.add('collapsed');
    content.style.maxHeight = '0';
    icon.textContent = '▶';
  }
}

function initCollapseState() {
  document.querySelectorAll('.project-content').forEach(el => {
    el.classList.add('collapsed');
    el.style.maxHeight = '0';
  });
  document.querySelectorAll('.toggle-icon').forEach(icon => {
    icon.textContent = '▶';
  });
}

// ===== 页脚日期 =====
function updateFooterDate() {
  const yearEl = document.getElementById('currentYear');
  const monthEl = document.getElementById('currentMonth');
  if (!yearEl || !monthEl) return;

  const now = new Date();
  yearEl.textContent = now.getFullYear();
  monthEl.textContent = now.getMonth() + 1;
}

// ===== 键盘快捷键 =====
document.addEventListener('keydown', (e) => {
  if (e.ctrlKey && e.key === 'p') {
    e.preventDefault();
    exportPDF();
  }
});

// ===== 初始化 =====
document.addEventListener('DOMContentLoaded', () => {
  initTheme();
  loadProjects();
  updateFooterDate();
});
