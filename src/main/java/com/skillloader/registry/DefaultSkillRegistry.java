package com.skillloader.registry;

import com.skillloader.config.PathEntry;
import com.skillloader.config.SkillLoaderConfig;
import com.skillloader.model.Skill;
import com.skillloader.model.SkillSource;
import com.skillloader.scanner.SkillScanner;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 默认 Skill 注册表实现。
 */
public class DefaultSkillRegistry implements SkillRegistry {
    
    private final SkillScanner scanner;
    private final Map<String, Skill> skillCache;
    private volatile boolean initialized = false;
    
    public DefaultSkillRegistry(SkillLoaderConfig config) {
        this.scanner = new SkillScanner(
            config.paths(),
            config.security(),
            config.parser().markerFile()
        );
        this.skillCache = new ConcurrentHashMap<>();
    }
    
    @Override
    public List<Skill> discover() {
        ensureInitialized();
        return List.copyOf(skillCache.values());
    }
    
    @Override
    public Optional<Skill> find(String name) {
        ensureInitialized();
        return Optional.ofNullable(skillCache.get(name));
    }
    
    @Override
    public List<Skill> findBySource(SkillSource source) {
        ensureInitialized();
        return skillCache.values().stream()
            .filter(s -> s.source() == source)
            .toList();
    }
    
    @Override
    public List<String> getSkillNames() {
        ensureInitialized();
        return List.copyOf(skillCache.keySet());
    }
    
    @Override
    public boolean hasSkill(String name) {
        ensureInitialized();
        return skillCache.containsKey(name);
    }
    
    /**
     * 刷新注册表（重新扫描）。
     */
    public void refresh() {
        List<Skill> scanned = scanner.scanAll();
        
        // 按优先级去重：同名 skill，高优先级覆盖低优先级
        Map<String, Skill> newCache = new HashMap<>();
        for (Skill skill : scanned) {
            Skill existing = newCache.get(skill.name());
            if (existing == null || skill.priority() < existing.priority()) {
                newCache.put(skill.name(), skill);
            }
        }
        
        skillCache.clear();
        skillCache.putAll(newCache);
        initialized = true;
    }
    
    private void ensureInitialized() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    refresh();
                }
            }
        }
    }
}
