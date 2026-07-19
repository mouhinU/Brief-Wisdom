/**
 * Brief-Wisdom Service Worker
 * 提供基础离线缓存和 PWA 支持
 *
 * @author Brief-Wisdom
 * @date 2026-07-19
 */

const CACHE_NAME = 'brief-wisdom-v1';
const STATIC_ASSETS = [
    '/',
    '/index.html',
    '/login.html',
    '/css/',
    '/js/',
    '/components/',
    '/i18n/',
    '/manifest.json'
];

// 安装阶段：缓存静态资源
self.addEventListener('install', (event) => {
    event.waitUntil(
        caches.open(CACHE_NAME)
            .then((cache) => {
                console.log('[SW] 缓存静态资源');
                return cache.addAll(STATIC_ASSETS);
            })
            .catch((err) => {
                console.warn('[SW] 缓存失败，跳过离线支持:', err);
            })
    );
    // 立即激活，不等旧 SW 退出
    self.skipWaiting();
});

// 激活阶段：清理旧缓存
self.addEventListener('activate', (event) => {
    event.waitUntil(
        caches.keys().then((cacheNames) => {
            return Promise.all(
                cacheNames
                    .filter((name) => name !== CACHE_NAME)
                    .map((name) => caches.delete(name))
            );
        })
    );
    // 接管所有客户端
    self.clients.claim();
});

// 请求拦截：网络优先策略（API 请求不走缓存）
self.addEventListener('fetch', (event) => {
    const url = new URL(event.request.url);

    // API 请求直接走网络，不缓存
    if (url.pathname.startsWith('/api/')) {
        return;
    }

    // 静态资源：缓存优先，回退网络
    event.respondWith(
        caches.match(event.request)
            .then((cached) => {
                if (cached) {
                    return cached;
                }
                return fetch(event.request).then((response) => {
                    // 只缓存成功的 GET 请求
                    if (response.ok && event.request.method === 'GET') {
                        const responseClone = response.clone();
                        caches.open(CACHE_NAME).then((cache) => {
                            cache.put(event.request, responseClone);
                        });
                    }
                    return response;
                });
            })
            .catch(() => {
                // 离线时返回离线页面（如果是导航请求）
                if (event.request.mode === 'navigate') {
                    return caches.match('/index.html');
                }
            })
    );
});
