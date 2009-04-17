/*
 * Copyright 2007-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.artifacts.dependencies;

import groovy.lang.Closure;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencyArtifact;
import org.gradle.api.artifacts.ExcludeRule;
import org.gradle.api.artifacts.ExcludeRuleContainer;
import org.gradle.api.internal.artifacts.DefaultExcludeRuleContainer;
import org.gradle.util.ConfigureUtil;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
* @author Hans Dockter
*/
public abstract class AbstractDependency implements Dependency {
    private State state = State.UNRESOLVED;
    private List<File> resolvedFiles;

    private ExcludeRuleContainer excludeRuleContainer = new DefaultExcludeRuleContainer();

    private String dependencyConfiguration = Dependency.DEFAULT_CONFIGURATION;
    private Set<DependencyArtifact> artifacts = new HashSet<DependencyArtifact>();

    protected AbstractDependency() {
    }

    public Dependency exclude(Map<String, String> excludeProperties) {
        excludeRuleContainer.add(excludeProperties);
        return this;
    }

    public Set<ExcludeRule> getExcludeRules() {
        return excludeRuleContainer.getRules();
    }

    public void setExcludeRuleContainer(ExcludeRuleContainer excludeRuleContainer) {
        this.excludeRuleContainer = excludeRuleContainer;
    }

    public Set<DependencyArtifact> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(Set<DependencyArtifact> artifacts) {
        this.artifacts = artifacts;
    }

    public AbstractDependency addArtifact(DependencyArtifact artifact) {
        artifacts.add(artifact);
        return this;
    }
    
    public DependencyArtifact artifact(Closure configureClosure) {
        DependencyArtifact artifact =  (DependencyArtifact) ConfigureUtil.configure(configureClosure, new DefaultDependencyArtifact());
        artifacts.add(artifact);
        return artifact;
    }

    public String getDependencyConfiguration() {
        return dependencyConfiguration;
    }

    public AbstractDependency setDependencyConfiguration(String dependencyConfiguration) {
        if (dependencyConfiguration == null || dependencyConfiguration.length() == 0) {
            throw new InvalidUserDataException("The dependency configuration can't be empty or null!");
        }
        this.dependencyConfiguration = dependencyConfiguration;
        return this;
    }

    @Override
    public int hashCode() {
        int result = getGroup() != null ? getGroup().hashCode() : 0;
        result = 31 * result + getName().hashCode();
        result = 31 * result + (getVersion() != null ? getVersion().hashCode() : 0);
        return result;
    }

//    public void setResolveData(List<File> resolvedFiles) {
//        assert state != State.UNRESOLVED;
//
//        if (resolvedFiles == null) {
//            state = State.UNRESOLVABLE;
//        } else {
//            state = State.RESOLVED;
//            this.resolvedFiles = resolvedFiles;
//        }
//    }
//
//    public State getState() {
//        return state;
//    }
//
//    public List<File> getFiles() {
//        if (state == State.UNRESOLVABLE || state == State.RESOLVED) {
//            throw new InvalidUserDataException("Files can't be accessed from an unresolved or unresolvable dependency. State=" + state);
//        }
//        return resolvedFiles;
//    }
}