/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.wso2.am.integration.cucumbertests.runners.block;

import io.cucumber.testng.CucumberOptions;

/**
 * Runner for the comment plane-parity suite: the full read surface of a comment thread on both entry planes and
 * across them, then the edit contract, cascade deletion and the moderation/authorisation model.
 * <p>
 * The three feature files are ORDER-DEPENDENT and their filenames encode that order, because cucumber-testng runs
 * a runner's features in lexicographic filename order (not the order of this array): {@code _setup_} builds the
 * fixture first, {@code comments_1_thread_reads} asserts against it while pristine, and
 * {@code comments_2_thread_mutations} edits and deletes it last.
 * <p>
 * Kept SEPARATE from {@link DevPortalCommentsRunner} on purpose: that runner's feature is tagged {@code @cleanup},
 * whose per-scenario hook sweeps every registered resource in scope — sharing a runner would delete this suite's
 * shared fixture mid-run. Teardown here is {@link BaseBlockRunner}'s AfterClass sweep, so none of the three
 * features is {@code @cleanup}.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/devportal/_setup_comment_threads.feature",
                "src/test/resources/features/devportal/comments_1_thread_reads.feature",
                "src/test/resources/features/devportal/comments_2_thread_mutations.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/devportal-comment-threads.html"}
)
public class DevPortalCommentThreadsRunner extends BaseBlockRunner {
}
