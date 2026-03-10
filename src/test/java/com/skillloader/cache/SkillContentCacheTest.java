package com.skillloader.cache;

import com.skillloader.config.CacheConfig;
import com.skillloader.model.SkillContent;
import com.skillloader.model.SkillMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * Skill 内容缓存测试。
 */
class SkillContentCacheTest {

    @TempDir
    Path tempDir;

    private SkillContent createMockContent(String name) {
        return new SkillContent(
            new SkillMetadata(name, "Description", null, List.of(), java.util.Map.of()),
            "# Content",
            tempDir,
            List.of()
        );
    }

    @Test
    void shouldCacheContent() {
        CacheConfig config = CacheConfig.withSize(10);
        SkillContentCache cache = new SkillContentCache(config);

        AtomicInteger loadCount = new AtomicInteger(0);
        Path skillPath = tempDir.resolve("skill1");

        // 第一次加载
        SkillContent content1 = cache.get(skillPath, path -> {
            loadCount.incrementAndGet();
            return createMockContent("skill1");
        });

        // 第二次加载（应该从缓存获取）
        SkillContent content2 = cache.get(skillPath, path -> {
            loadCount.incrementAndGet();
            return createMockContent("skill1");
        });

        assertThat(loadCount.get()).isEqualTo(1);
        assertThat(content1).isSameAs(content2);
    }

    @Test
    void shouldRespectMaxSize() throws InterruptedException {
        CacheConfig config = CacheConfig.withSize(2);
        SkillContentCache cache = new SkillContentCache(config);

        Path path1 = tempDir.resolve("skill1");
        Path path2 = tempDir.resolve("skill2");
        Path path3 = tempDir.resolve("skill3");

        cache.put(path1, createMockContent("skill1"));
        cache.put(path2, createMockContent("skill2"));
        cache.put(path3, createMockContent("skill3")); // 应该淘汰 skill1

        // Caffeine 的淘汰是异步的，等待一小段时间
        Thread.sleep(100);

        assertThat(cache.size()).isLessThanOrEqualTo(2);
    }

    @Test
    void shouldDisableCache() {
        CacheConfig config = CacheConfig.disabled();
        SkillContentCache cache = new SkillContentCache(config);

        AtomicInteger loadCount = new AtomicInteger(0);
        Path skillPath = tempDir.resolve("skill1");

        // 多次加载（缓存禁用，每次都执行 loader）
        cache.get(skillPath, path -> {
            loadCount.incrementAndGet();
            return createMockContent("skill1");
        });
        cache.get(skillPath, path -> {
            loadCount.incrementAndGet();
            return createMockContent("skill1");
        });

        assertThat(loadCount.get()).isEqualTo(2);
        assertThat(cache.size()).isEqualTo(0);
    }

    @Test
    void shouldInvalidateSpecificEntry() {
        CacheConfig config = CacheConfig.withSize(10);
        SkillContentCache cache = new SkillContentCache(config);

        Path path1 = tempDir.resolve("skill1");
        Path path2 = tempDir.resolve("skill2");

        cache.put(path1, createMockContent("skill1"));
        cache.put(path2, createMockContent("skill2"));

        cache.invalidate(path1);

        assertThat(cache.getIfPresent(path1)).isEmpty();
        assertThat(cache.getIfPresent(path2)).isPresent();
    }

    @Test
    void shouldInvalidateAll() {
        CacheConfig config = CacheConfig.withSize(10);
        SkillContentCache cache = new SkillContentCache(config);

        cache.put(tempDir.resolve("skill1"), createMockContent("skill1"));
        cache.put(tempDir.resolve("skill2"), createMockContent("skill2"));

        cache.invalidateAll();

        assertThat(cache.size()).isEqualTo(0);
    }

    @Test
    void shouldReturnCacheStats() {
        CacheConfig config = CacheConfig.withSize(10);
        SkillContentCache cache = new SkillContentCache(config);

        Path skillPath = tempDir.resolve("skill1");

        // 未命中
        cache.get(skillPath, path -> createMockContent("skill1"));

        // 命中
        cache.get(skillPath, path -> createMockContent("skill1"));
        cache.get(skillPath, path -> createMockContent("skill1"));

        SkillContentCache.CacheStats stats = cache.getStats();

        assertThat(stats.hitCount()).isEqualTo(2);
        assertThat(stats.missCount()).isEqualTo(1);
        assertThat(stats.hitRate()).isGreaterThan(0.6);
        assertThat(stats.size()).isEqualTo(1);
    }

    @Test
    void shouldReturnEmptyStatsWhenDisabled() {
        CacheConfig config = CacheConfig.disabled();
        SkillContentCache cache = new SkillContentCache(config);

        SkillContentCache.CacheStats stats = cache.getStats();

        assertThat(stats.hitCount()).isEqualTo(0);
        assertThat(stats.missCount()).isEqualTo(0);
        assertThat(stats.hitRate()).isEqualTo(0.0);
        assertThat(stats.size()).isEqualTo(0);
    }

    @Test
    void shouldSupportDefaultConfiguration() {
        SkillContentCache cache = SkillContentCache.createDefault();

        assertThat(cache.isEnabled()).isTrue();

        Path skillPath = tempDir.resolve("skill1");
        cache.put(skillPath, createMockContent("skill1"));

        assertThat(cache.getIfPresent(skillPath)).isPresent();
    }
}
