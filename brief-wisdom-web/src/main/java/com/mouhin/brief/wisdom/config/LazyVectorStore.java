package com.mouhin.brief.wisdom.config;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * 惰性 VectorStore 代理
 * <p>
 * 在首次实际操作时才尝试初始化底层的 RedisVectorStore。
 * 如果 RediSearch 模块不可用，所有操作变为 no-op（add/delete 静默跳过，search 返回空列表）。
 * <p>
 * 这样应用可以在没有 RediSearch 的 Redis 环境下正常启动，
 * 向量检索功能自动降级为不可用，不影响其他功能。
 *
 * @author Brief-Wisdom
 * @date 2026-07-12
 */
public class LazyVectorStore implements VectorStore {

    private final AtomicReference<VectorStore> delegate;
    private final AtomicReference<Boolean> initAttempted;
    private final Supplier<VectorStore> initializer;
    private volatile boolean available;

    public LazyVectorStore(AtomicReference<VectorStore> delegate,
                           AtomicReference<Boolean> initAttempted,
                           Supplier<VectorStore> initializer) {
        this.delegate = delegate;
        this.initAttempted = initAttempted;
        this.initializer = initializer;
        this.available = false;
    }

    /**
     * 确保底层 VectorStore 已初始化（仅尝试一次）
     */
    private VectorStore getDelegate() {
        if (!initAttempted.get()) {
            initAttempted.set(true);
            VectorStore store = initializer.get();
            if (store != null) {
                delegate.set(store);
                available = true;
            }
        }
        return delegate.get();
    }

    /**
     * 向量检索是否可用
     */
    public boolean isAvailable() {
        if (!initAttempted.get()) {
            getDelegate();
        }
        return available;
    }

    @Override
    public void add(List<Document> documents) {
        VectorStore store = getDelegate();
        if (store != null) {
            store.add(documents);
        }
    }

    @Override
    public void delete(List<String> idList) {
        VectorStore store = getDelegate();
        if (store != null) {
            store.delete(idList);
        }
    }

    @Override
    public void delete(Filter.Expression expression) {
        VectorStore store = getDelegate();
        if (store != null) {
            store.delete(expression);
        }
    }

    @Override
    public List<Document> similaritySearch(SearchRequest request) {
        VectorStore store = getDelegate();
        if (store != null) {
            return store.similaritySearch(request);
        }
        return List.of();
    }

    @Override
    public String getName() {
        return "LazyVectorStore";
    }
}
