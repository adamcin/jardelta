/*
 * Copyright 2024 Adobe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.adamcin.jardelta.mavenplugin;

import aQute.bnd.version.MavenVersion;
import net.adamcin.jardelta.core.Context;
import net.adamcin.jardelta.core.Delta;
import net.adamcin.jardelta.core.Jars;
import net.adamcin.jardelta.core.Plan;
import net.adamcin.streamsupport.Fun;
import net.adamcin.streamsupport.Result;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.version.Version;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Formatter;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import static net.adamcin.streamsupport.Fun.result0;

/**
 * Executes a {@link net.adamcin.jardelta.core.Plan} to produce a jardelta.
 */
@Mojo(name = "jardelta", defaultPhase = LifecyclePhase.INTEGRATION_TEST)
public class JarDeltaMojo extends AbstractMojo {
    private static final Logger logger = LoggerFactory.getLogger(JarDeltaMojo.class);
    @Parameter(defaultValue = "${project}", readonly = true, required = false)
    private MavenProject project;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
    private RepositorySystemSession session;

    @Component
    private RepositorySystem system;

    @Parameter(property = "jardelta.skip", defaultValue = "false")
    private boolean skip;

    @Parameter(property = "jardelta.includeDistributionManagement", defaultValue = "true")
    private boolean includeDistributionManagement;

    /**
     * The Maven coordinates of the base artifact in the format
     * {@code <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>}. If
     * set, takes precedence over {@link #base}.
     */
    @Parameter(property = "jardelta.baseCoordinates")
    private String baseCoordinates;

    @Parameter(required = false)
    private Base base;

    @Parameter(property = "jardelta.releaseVersions", defaultValue = "false")
    private boolean releaseVersions;

    @Parameter
    private String comparisonClassifier;

    @Parameter(property = "jardelta.leftHandFile")
    private File leftHandFile;

    @Parameter(property = "jardelta.rightHandFile")
    private File rightHandFile;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            logger.debug("skip project as configured");
            return;
        }

        final Context.ContextBuilder contextBuilder = Context.builder();

        if (leftHandFile != null && rightHandFile != null) {
            try {
                contextBuilder.jars(Jars.from(leftHandFile, rightHandFile));
                executePlan(contextBuilder);
                return;
            } catch (Exception e) {
                throw new MojoFailureException(e);
            }
        } else if (project == null) {
            throw new MojoFailureException("Unable to resolve artifacts for jardelta comparison outside of maven project");
        } else if ("pom".equals(project.getPackaging())) {
            logger.debug("skip project as packaging is pom and either jardelta.leftHandFile and jardelta.rightHandFile are unset");
            return;
        }

        Result<File> resolvedLeft = Optional.ofNullable(leftHandFile).map(Result::success).orElseGet(() -> Fun.result0(() -> {
            Artifact baseArtifact = findRightHandArtifact().findFirst()
                    .orElse(RepositoryUtils.toArtifact(project.getArtifact()));
            this.setupBase(baseArtifact);
            final List<RemoteRepository> aetherRepos = getRepositories(project);
            findBaseArtifact(aetherRepos);
            if (base.getVersion() != null && !base.getVersion().isEmpty()) {
                ArtifactResult artifactResult = locateBaseJar(aetherRepos);
                if ( !artifactResult.isMissing() ) {
                    return artifactResult.getArtifact().getFile();
                }
            }
            throw new MojoFailureException("Unable to locate a previous version of the artifact");
        }).get());

        try {
            contextBuilder.jars(Jars.from(resolvedLeft.getOrThrow(MojoFailureException.class), findRightHandFile()));
            executePlan(contextBuilder);
        } catch (MojoFailureException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException(e);
        }
    }

    void findBaseArtifact(@NotNull List<RemoteRepository> aetherRepos) throws VersionRangeResolutionException {
        logger.info("Determining the jardelta base version for {} using repositories {}", base, aetherRepos);

        Artifact toFind = new DefaultArtifact(base.getGroupId(), base.getArtifactId(), base.getClassifier(),
                base.getExtension(), base.getVersion());

        VersionRangeRequest request = new VersionRangeRequest(toFind, aetherRepos, "jardelta");

        VersionRangeResult versions = system.resolveVersionRange(session, request);

        List<Version> found = versions.getVersions();
        logger.debug("Found versions {}", found);

        boolean onlyreleaseversions = releaseVersions && (base.getVersion()
                .startsWith("[")
                || base.getVersion()
                .startsWith("("));

        base.setVersion(null);
        for (ListIterator<Version> li = found.listIterator(found.size()); li.hasPrevious(); ) {
            String highest = li.previous()
                    .toString();
            if (toFind.setVersion(highest)
                    .isSnapshot()) {
                continue;
            }
            if (onlyreleaseversions) {
                MavenVersion mavenVersion = MavenVersion.parseMavenString(highest);
                if (mavenVersion.compareTo(mavenVersion.toReleaseVersion()) < 0) {
                    logger.debug("Version {} not considered since it is not a release version", highest);
                    continue; // not a release version
                }
            }
            base.setVersion(highest);
            break;
        }

        logger.info("The base version was found to be {}", base.getVersion());
    }

    private ArtifactResult locateBaseJar(List<RemoteRepository> aetherRepos) throws ArtifactResolutionException {
        Artifact toFind = new DefaultArtifact(base.getGroupId(), base.getArtifactId(), base.getClassifier(),
                base.getExtension(), base.getVersion());

        return system.resolveArtifact(session, new ArtifactRequest(toFind, aetherRepos, "jardelta"));
    }

    void executePlan(@NotNull Context.ContextBuilder contextBuilder) {
        final Plan plan = new Plan();
        final Delta delta = plan.execute(contextBuilder.build());
        delta.getResults().stream().forEachOrdered(diff -> logger.info("{}", diff));
    }

    static Result<URL> fileToURL(@NotNull File file) {
        return result0(() -> file.toURI().toURL()).get();
    }

    File findRightHandFile() throws MojoFailureException {
        return Stream.concat(Stream.ofNullable(rightHandFile), findRightHandArtifact().map(Artifact::getFile))
                .findFirst().orElseThrow(() ->
                        new MojoFailureException("Unable to find matching attached artifact for right-hand jardelta comparison"));
    }

    private void setupBase(Artifact artifact) {
        if (base == null) {
            base = new Base();
        }
        if (baseCoordinates != null && !baseCoordinates.isBlank()) {
            base.setFromCoordinates(baseCoordinates);
        }
        if (base.getGroupId() == null || base.getGroupId()
                .isEmpty()) {
            base.setGroupId(project.getGroupId());
        }
        if (base.getArtifactId() == null || base.getArtifactId()
                .isEmpty()) {
            base.setArtifactId(project.getArtifactId());
        }
        if (base.getClassifier() == null || base.getClassifier()
                .isEmpty()) {
            base.setClassifier(artifact.getClassifier());
        }
        if (base.getExtension() == null || base.getExtension()
                .isEmpty()) {
            base.setExtension(artifact.getExtension());
        }
        if (base.getVersion() == null || base.getVersion()
                .isEmpty()) {
            base.setVersion("(," + artifact.getVersion() + ")");
        }

        logger.debug("Computing delta against {}", base);
    }

    Stream<Artifact> findRightHandArtifact() {
        return Stream.ofNullable(project)
                .flatMap(mavenProject -> comparisonClassifier == null || comparisonClassifier.isEmpty()
                        ? Stream.ofNullable(mavenProject.getArtifact())
                        : mavenProject.getAttachedArtifacts().stream()
                        .filter(artifact -> comparisonClassifier.equals(artifact.getClassifier())))
                .map(RepositoryUtils::toArtifact);
    }

    private List<RemoteRepository> getRepositories(@NotNull MavenProject project) {
        List<RemoteRepository> aetherRepos = RepositoryUtils.toRepos(project.getRemoteArtifactRepositories());

        if (includeDistributionManagement) {
            RemoteRepository releaseDistroRepo;
            if (project.getArtifact().isSnapshot()) {
                MavenProject tmpClone = project.clone();
                tmpClone.getArtifact()
                        .setVersion("1.0.0");
                releaseDistroRepo = RepositoryUtils.toRepo(tmpClone.getDistributionManagementArtifactRepository());
            } else {
                releaseDistroRepo = RepositoryUtils.toRepo(project.getDistributionManagementArtifactRepository());
            }

            if (releaseDistroRepo != null) {
                aetherRepos.add(0, releaseDistroRepo);
            }
        }

        return aetherRepos;
    }
}
