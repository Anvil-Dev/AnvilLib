package dev.anvilcraft.lib.v2.integration;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;

@Slf4j
@Getter
@ApiStatus.Internal
public class ModVersionRange {
    public static final ModVersionRange ANY = new ModVersionRange(null) {
        @Override
        public boolean containsVersion(ArtifactVersion version) {
            return true;
        }

        @Override
        public String toString() {
            return "*";
        }
    };
    @Nullable
    private final VersionRange range;

    protected ModVersionRange(@Nullable VersionRange range) {
        this.range = range;
    }

    public static ModVersionRange of(String spec) {
        try {
            if ("*".equals(spec)) return ModVersionRange.ANY;
            return new ModVersionRange(VersionRange.createFromVersionSpec(spec));
        } catch (InvalidVersionSpecificationException e) {
            log.error("Invalid version specification: {}", spec);
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean containsVersion(ArtifactVersion version) {
        return range == null || range.containsVersion(version);
    }

    @Override
    public String toString() {
        return range == null ? "*" : range.toString();
    }
}
