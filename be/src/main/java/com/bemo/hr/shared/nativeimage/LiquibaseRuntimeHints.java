package com.bemo.hr.shared.nativeimage;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;

import java.io.IOException;

/**
 * Registers Liquibase change setters that are selected from YAML at runtime.
 */
public final class LiquibaseRuntimeHints implements RuntimeHintsRegistrar {

    private static final String LIQUIBASE_CLASSES = "classpath*:liquibase/**/*.class";

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(classLoader);
        CachingMetadataReaderFactory metadata = new CachingMetadataReaderFactory(classLoader);
        try {
            for (Resource resource : resolver.getResources(LIQUIBASE_CLASSES)) {
                String className = metadata.getMetadataReader(resource).getClassMetadata().getClassName();
                if (className.endsWith(".package-info") || className.endsWith(".module-info")) {
                    continue;
                }
                hints.reflection().registerType(
                        TypeReference.of(className),
                        MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                        MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                        MemberCategory.INVOKE_DECLARED_METHODS,
                        MemberCategory.INVOKE_PUBLIC_METHODS,
                        MemberCategory.ACCESS_DECLARED_FIELDS,
                        MemberCategory.ACCESS_PUBLIC_FIELDS);
            }
        } catch (IOException error) {
            throw new IllegalStateException("Could not inspect Liquibase change classes", error);
        }
    }
}
