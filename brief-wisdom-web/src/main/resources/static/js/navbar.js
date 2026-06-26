/**
 * 公共导航栏组件
 * 从 /api/menu/list 接口动态加载菜单并渲染
 */

async function initNavbar() {
  try {
    const res = await fetch('/api/menu/list');
    const result = await res.json();
    if (!result.success) {
      console.error('加载菜单失败:', result.error);
      return;
    }
    const menus = result.data;
    renderNavbar(menus);
  } catch (err) {
    console.error('加载菜单异常:', err);
  }
}

function renderNavbar(menus) {
  // 创建导航栏元素
  const navbar = document.createElement('nav');
  navbar.className = 'navbar';

  // 品牌名
  const brand = document.createElement('a');
  brand.href = '/';
  brand.className = 'navbar-brand';
  brand.textContent = 'Brief Wisdom';
  navbar.appendChild(brand);

  // 菜单列表
  const menuList = document.createElement('ul');
  menuList.className = 'navbar-menu';

  const currentPath = window.location.pathname;

  menus.forEach(menu => {
    const li = document.createElement('li');
    const a = document.createElement('a');
    a.href = menu.url;
    if (menu.target && menu.target !== '_self') {
      a.target = menu.target;
    }

    // 判断当前页面是否匹配
    const menuPath = menu.url.split('#')[0]; // 去掉 hash
    if (menuPath === '/' && (currentPath === '/' || currentPath === '/index.html')) {
      a.classList.add('active');
    } else if (menuPath !== '/' && currentPath === menuPath) {
      a.classList.add('active');
    }

    // 图标
    if (menu.icon) {
      const iconSpan = document.createElement('span');
      iconSpan.className = 'menu-icon';
      iconSpan.textContent = menu.icon;
      a.appendChild(iconSpan);
    }

    // 文字
    const textSpan = document.createElement('span');
    textSpan.className = 'menu-text';
    textSpan.textContent = menu.name;
    a.appendChild(textSpan);

    // 移动端也显示文字（不再隐藏）
    li.appendChild(a);
    menuList.appendChild(li);
  });

  navbar.appendChild(menuList);

  // 插入到 body 最前面
  document.body.insertBefore(navbar, document.body.firstChild);
}

// 自动初始化
document.addEventListener('DOMContentLoaded', () => {
  initNavbar();
});
