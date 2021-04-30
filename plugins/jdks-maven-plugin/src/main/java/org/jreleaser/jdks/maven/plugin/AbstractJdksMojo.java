/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020-2021 Andres Almiray.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jreleaser.jdks.maven.plugin;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.jreleaser.util.Errors;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

/**
 * @author Andres Almiray
 * @since 0.3.0
 */
abstract class AbstractJdksMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Parameter(required = true)
    protected List<Jdk> jdks;

    @Parameter(property = "jdks.output.directory", defaultValue = "${project.build.directory}/jdks")
    protected File outputDirectory;

    @Parameter(defaultValue = "${session}")
    protected MavenSession session;

    @Component
    protected BuildPluginManager pluginManager;

    protected void validate() throws MojoFailureException {
        if (jdks == null || jdks.isEmpty()) return;

        Errors errors = new Errors();
        jdks.forEach(jdk -> jdk.validate(errors));

        if (errors.hasErrors()) {
            StringWriter s = new StringWriter();
            PrintWriter w = new PrintWriter(s, true);
            errors.logErrors(w);
            throw new MojoFailureException(s.toString());
        }
    }
}
