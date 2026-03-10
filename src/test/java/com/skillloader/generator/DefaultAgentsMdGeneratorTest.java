package com.skillloader.generator;

import com.skillloader.model.Skill;
import com.skillloader.model.SkillSource;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class DefaultAgentsMdGeneratorTest {
    
    private DefaultAgentsMdGenerator createGenerator() {
        return new DefaultAgentsMdGenerator();
    }
    
    private Skill createSkill(String name, String description, SkillSource source, int priority) {
        return new Skill(name, description, source, Path.of("/test/" + name), priority);
    }
    
    @Test
    void shouldGenerateSkillsSystem() {
        DefaultAgentsMdGenerator generator = createGenerator();
        List<Skill> skills = List.of(
            createSkill("pdf", "PDF toolkit", SkillSource.PROJECT, 10),
            createSkill("weather", "Weather query", SkillSource.GLOBAL, 20)
        );
        
        String result = generator.generate(skills);
        
        assertThat(result).contains("<skills_system");
        assertThat(result).contains("</skills_system>");
        assertThat(result).contains("<available_skills>");
        assertThat(result).contains("<!-- SKILLS_TABLE_START -->");
        assertThat(result).contains("<!-- SKILLS_TABLE_END -->");
    }
    
    @Test
    void shouldContainSkillEntries() {
        DefaultAgentsMdGenerator generator = createGenerator();
        List<Skill> skills = List.of(
            createSkill("pdf", "PDF toolkit", SkillSource.PROJECT, 10)
        );
        
        String result = generator.generate(skills);
        
        assertThat(result).contains("<name>pdf</name>");
        assertThat(result).contains("<description>PDF toolkit</description>");
        assertThat(result).contains("<location>project</location>");
    }
    
    @Test
    void shouldGenerateEmptySkillsList() {
        DefaultAgentsMdGenerator generator = createGenerator();
        String result = generator.generate(List.of());
        
        assertThat(result).contains("<available_skills>");
        assertThat(result).contains("</skills_system>");
    }
    
    @Test
    void shouldContainUsageBlock() {
        DefaultAgentsMdGenerator generator = createGenerator();
        String result = generator.generate(List.of());
        
        assertThat(result).contains("<usage>");
        assertThat(result).contains("</usage>");
        assertThat(result).contains("How to use skills");
    }
    
    @Test
    void shouldUpdateExistingContent() {
        DefaultAgentsMdGenerator generator = createGenerator();
        String existing = """
            # Project Documentation
            
            Some content here.
            
            <!-- SKILLS_TABLE_START -->
            Old skills content
            <!-- SKILLS_TABLE_END -->
            
            More content.
            """;
        
        List<Skill> skills = List.of(
            createSkill("new-skill", "New skill", SkillSource.PROJECT, 10)
        );
        
        String result = generator.updateExisting(existing, skills);
        
        assertThat(result).contains("# Project Documentation");
        assertThat(result).contains("<name>new-skill</name>");
        assertThat(result).doesNotContain("Old skills content");
    }
    
    @Test
    void shouldUpdateExistingContentWithMultiline() {
        DefaultAgentsMdGenerator generator = createGenerator();
        // 包含多行的复杂内容
        String existing = """
            # Project Documentation
            
            <!-- SKILLS_TABLE_START -->
            <skill>
            <name>old</name>
            <description>Old\nmultiline\ndescription</description>
            </skill>
            <!-- SKILLS_TABLE_END -->
            
            End content.
            """;
        
        List<Skill> skills = List.of(
            createSkill("new", "New skill", SkillSource.PROJECT, 10)
        );
        
        String result = generator.updateExisting(existing, skills);
        
        assertThat(result).contains("# Project Documentation");
        assertThat(result).contains("<name>new</name>");
        assertThat(result).doesNotContain("<name>old</name>");
        assertThat(result).contains("<!-- SKILLS_TABLE_START -->");
        assertThat(result).contains("<!-- SKILLS_TABLE_END -->");
    }
    
    @Test
    void shouldAppendWhenNoMarkers() {
        DefaultAgentsMdGenerator generator = createGenerator();
        String existing = "# Project Documentation\n\nSome content.";
        
        List<Skill> skills = List.of(
            createSkill("skill", "Description", SkillSource.PROJECT, 10)
        );
        
        String result = generator.updateExisting(existing, skills);
        
        assertThat(result).contains("# Project Documentation");
        assertThat(result).contains("<skills_system");
        assertThat(result).contains("<name>skill</name>");
    }
    
    @Test
    void shouldEscapeXmlCharacters() {
        DefaultAgentsMdGenerator generator = createGenerator();
        List<Skill> skills = List.of(
            createSkill("test", "Use <script> \"alert(1)\"", SkillSource.PROJECT, 10)
        );
        
        String result = generator.generate(skills);
        
        assertThat(result).doesNotContain("<script>");
        assertThat(result).contains("&lt;script&gt;");
        assertThat(result).contains("&quot;");
    }
    
    @Test
    void shouldHandleNullDescription() {
        DefaultAgentsMdGenerator generator = createGenerator();
        Skill skill = new Skill("test", null, SkillSource.PROJECT, Path.of("/test"), 10);
        
        String result = generator.generate(List.of(skill));
        
        assertThat(result).contains("<name>test</name>");
        assertThat(result).contains("<description></description>");
    }
    
    @Test
    void shouldContainCorrectLocation() {
        DefaultAgentsMdGenerator generator = createGenerator();
        List<Skill> skills = List.of(
            createSkill("s1", "Desc", SkillSource.PROJECT, 10),
            createSkill("s2", "Desc", SkillSource.GLOBAL, 20),
            createSkill("s3", "Desc", SkillSource.CLASSPATH, 30)
        );
        
        String result = generator.generate(skills);
        
        assertThat(result).contains("<location>project</location>");
        assertThat(result).contains("<location>global</location>");
        assertThat(result).contains("<location>classpath</location>");
    }
}
