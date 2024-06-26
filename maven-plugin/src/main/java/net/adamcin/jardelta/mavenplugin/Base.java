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

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

/**
 * Copied from {@link <a href="https://github.com/bndtools/bnd/blob/master/maven-plugins/bnd-baseline-maven-plugin/src/main/java/aQute/bnd/maven/baseline/plugin/Base.java">bnd-baseline-maven-plugin</a>}.
 */
public class Base {
    private String	groupId;

    private String	artifactId;

    private String	version;

    private String	classifier;

    private String	extension;

    public void setFromCoordinates(String coordinates) {
        Artifact artifact = new DefaultArtifact(coordinates);
        setGroupId(artifact.getGroupId());
        setArtifactId(artifact.getArtifactId());
        setVersion(artifact.getVersion());
        setClassifier(artifact.getClassifier());
        setExtension(artifact.getExtension());
    }

    public String getGroupId() {
        return groupId;
    }

    /**
     * Set the groupId of the baseline artifact.
     *
     * @param groupId The groupId of the baseline artifact. This defaults to the
     *            groupId of the project.
     */
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    /**
     * Set the artifactId of the baseline artifact.
     *
     * @param artifactId The artifactId of the baseline artifact. This defaults
     *            to the artifactId of the project.
     */
    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    /**
     * Set the version or version range of the baseline artifact.
     *
     * @param version The version or version range of the baseline artifact.
     *            This defaults to the highest version less than the version of
     *            the project's artifact.
     */
    public void setVersion(String version) {
        this.version = version;
    }

    public String getClassifier() {
        return classifier;
    }

    /**
     * Set the classifier of the baseline artifact.
     *
     * @param classifier The classifier of the baseline artifact. This defaults
     *            to the classifier of the project's artifact.
     */
    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public String getExtension() {
        return extension;
    }

    /**
     * Set the extension of the baseline artifact.
     *
     * @param extension The extension of the baseline artifact. This defaults to
     *            the extension of the project's artifact.
     */
    public void setExtension(String extension) {
        this.extension = extension;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder().append(groupId)
                .append(':')
                .append(artifactId)
                .append(':')
                .append(extension);
        if ((classifier != null) && !classifier.isEmpty()) {
            result.append(':')
                    .append(classifier);
        }
        return result.append(':')
                .append(version)
                .toString();
    }
}
