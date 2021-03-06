/*
 * Copyright 2011 the original author or authors.
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
package org.gradle.tooling.internal.provider;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.tooling.internal.DefaultTask;
import org.gradle.tooling.internal.protocol.eclipse.EclipseProjectVersion3;
import org.gradle.tooling.internal.protocol.eclipse.EclipseTaskVersion1;

import java.util.ArrayList;
import java.util.List;

public class TasksFactory {
    public List<EclipseTaskVersion1> create(Project project, EclipseProjectVersion3 eclipseProject) {
        List<EclipseTaskVersion1> tasks = new ArrayList<EclipseTaskVersion1>();
        for (final Task task : project.getTasks()) {
            tasks.add(new DefaultTask(eclipseProject, task.getPath(), task.getName(), task.getDescription()));
        }
        return tasks;
    }

}

