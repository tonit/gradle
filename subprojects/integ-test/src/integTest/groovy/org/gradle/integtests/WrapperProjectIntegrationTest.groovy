/*
 * Copyright 2007 the original author or authors.
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

package org.gradle.integtests

import org.gradle.integtests.fixtures.ExecutionResult
import org.gradle.integtests.fixtures.GradleDistribution
import org.gradle.integtests.fixtures.GradleDistributionExecuter
import org.junit.Rule
import org.junit.Test
import static org.hamcrest.Matchers.*
import static org.junit.Assert.*
import org.gradle.integtests.fixtures.Sample
import org.gradle.integtests.fixtures.ExecutionFailure

/**
 * @author Hans Dockter
 */
class WrapperProjectIntegrationTest {
    @Rule public final GradleDistribution dist = new GradleDistribution()
    @Rule public final GradleDistributionExecuter executer = new GradleDistributionExecuter()
    @Rule public final Sample sample = new Sample('wrapper-project')

    @Test
    public void hasNonZeroExitCodeOnBuildFailure() {
        File wrapperSampleDir = sample.dir

        executer.inDirectory(wrapperSampleDir).withTasks('wrapper').run()

        ExecutionFailure failure = executer.usingExecutable('gradlew').inDirectory(wrapperSampleDir).withTasks('unknown').runWithFailure()
        failure.assertHasDescription("Task 'unknown' not found in root project 'wrapper-project'.")
    }

    @Test
    public void wrapperSample() {
        File wrapperSampleDir = sample.dir

        executer.inDirectory(wrapperSampleDir).withTasks('wrapper').run()

        ExecutionResult result = executer.usingExecutable('gradlew').inDirectory(wrapperSampleDir).withTasks('hello').run()
        assertThat(result.output, containsString('hello'))
    }
}
