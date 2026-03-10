package com.skillloader.concurrency;

import com.skillloader.api.SkillLoader;
import com.skillloader.model.Skill;
import com.skillloader.model.SkillContent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

/**
 * 线程安全测试。
 */
class ThreadSafetyTest {

    @TempDir
    Path tempDir;

    private void createMultipleSkills(Path dir, int count) throws Exception {
        for (int i = 0; i < count; i++) {
            Path skillDir = dir.resolve("skill-" + i);
            Files.createDirectories(skillDir);
            Files.writeString(skillDir.resolve("SKILL.md"), """
                ---
                name: skill-%d
                description: Skill number %d
                ---
                # Skill %d
                Content for skill %d
                """.formatted(i, i, i, i));
        }
    }

    @Test
    void shouldSupportConcurrentDiscover() throws Exception {
        createMultipleSkills(tempDir, 10);

        SkillLoader loader = SkillLoader.builder()
            .addFilesystemPath("test", tempDir.toString())
            .build();

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        try {
            List<Future<List<Skill>>> futures = IntStream.range(0, threadCount)
                .mapToObj(i -> executor.submit(loader::discover))
                .toList();

            // 所有线程应该返回相同的结果
            for (Future<List<Skill>> future : futures) {
                List<Skill> skills = future.get(5, TimeUnit.SECONDS);
                assertThat(skills).hasSize(10);
            }
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void shouldSupportConcurrentLoad() throws Exception {
        createMultipleSkills(tempDir, 5);

        SkillLoader loader = SkillLoader.builder()
            .addFilesystemPath("test", tempDir.toString())
            .build();

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        try {
            List<Future<SkillContent>> futures = IntStream.range(0, threadCount)
                .mapToObj(i -> {
                    String skillName = "skill-" + (i % 5);
                    return executor.submit(() -> loader.load(skillName));
                })
                .toList();

            // 所有线程应该成功加载
            for (Future<SkillContent> future : futures) {
                SkillContent content = future.get(5, TimeUnit.SECONDS);
                assertThat(content).isNotNull();
                assertThat(content.metadata()).isNotNull();
            }
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void shouldSupportConcurrentMixedOperations() throws Exception {
        createMultipleSkills(tempDir, 10);

        SkillLoader loader = SkillLoader.builder()
            .addFilesystemPath("test", tempDir.toString())
            .build();

        int threadCount = 30;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        try {
            List<Future<Object>> futures = IntStream.range(0, threadCount)
                .mapToObj(i -> executor.submit(() -> {
                    try {
                        // 混合操作：discover、load、getMetadata、generateAgentsMd
                        switch (i % 4) {
                            case 0 -> {
                                List<Skill> skills = loader.discover();
                                assertThat(skills).isNotEmpty();
                            }
                            case 1 -> {
                                SkillContent content = loader.load("skill-" + (i % 10));
                                assertThat(content).isNotNull();
                            }
                            case 2 -> {
                                var metadata = loader.getMetadata("skill-" + (i % 10));
                                assertThat(metadata).isPresent();
                            }
                            case 3 -> {
                                String agentsMd = loader.generateAgentsMd();
                                assertThat(agentsMd).isNotEmpty();
                            }
                        }
                    } finally {
                        latch.countDown();
                    }
                    return null;
                }))
                .toList();

            // 等待所有任务完成
            boolean completed = latch.await(10, TimeUnit.SECONDS);
            assertThat(completed).isTrue();

            // 检查没有异常
            for (Future<?> future : futures) {
                assertThatCode(() -> future.get(1, TimeUnit.SECONDS)).doesNotThrowAnyException();
            }
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void shouldHandleHighConcurrency() throws Exception {
        createMultipleSkills(tempDir, 5);

        SkillLoader loader = SkillLoader.builder()
            .addFilesystemPath("test", tempDir.toString())
            .build();

        int requestCount = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(50);

        try {
            List<Future<SkillContent>> futures = IntStream.range(0, requestCount)
                .mapToObj(i -> executor.submit(() -> loader.load("skill-" + (i % 5))))
                .toList();

            // 所有请求应该成功
            int successCount = 0;
            for (Future<SkillContent> future : futures) {
                try {
                    SkillContent content = future.get(10, TimeUnit.SECONDS);
                    if (content != null && content.metadata() != null) {
                        successCount++;
                    }
                } catch (Exception e) {
                    // 计数失败
                }
            }

            assertThat(successCount).isEqualTo(requestCount);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void shouldReturnConsistentResultsUnderConcurrency() throws Exception {
        createMultipleSkills(tempDir, 3);

        SkillLoader loader = SkillLoader.builder()
            .addFilesystemPath("test", tempDir.toString())
            .build();

        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        try {
            // 并发执行 discover
            List<Future<List<Skill>>> futures = IntStream.range(0, threadCount)
                .mapToObj(i -> executor.submit(loader::discover))
                .toList();

            // 收集所有结果
            Set<String> allSkillNames = futures.stream()
                .map(f -> {
                    try {
                        return f.get(5, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        return List.<Skill>of();
                    }
                })
                .flatMap(List::stream)
                .map(Skill::name)
                .collect(Collectors.toSet());

            // 所有线程应该看到相同的 skills
            assertThat(allSkillNames).containsExactlyInAnyOrder("skill-0", "skill-1", "skill-2");
        } finally {
            executor.shutdown();
        }
    }
}
