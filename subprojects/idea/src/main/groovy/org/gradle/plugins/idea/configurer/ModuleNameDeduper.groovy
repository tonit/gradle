/*
 * Copyright 2010 the original author or authors.
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
package org.gradle.plugins.idea.configurer

import org.gradle.plugins.idea.IdeaModule

/**
 * @author Szczepan Faber, @date 03.03.11
 */
public class ModuleNameDeduper {

    def dedupeModuleNames(Collection<IdeaModule> ideaModules) {
        def allNames = []
        ideaModules.each { module ->
            def name = module.candidateNames.find { !allNames.contains(it) }
            if (name) {
                allNames << name
                module.moduleName = name
            }
        }
    }
}