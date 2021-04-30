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
package org.jreleaser.tools;

import org.jreleaser.model.Artifact;
import org.jreleaser.model.Distribution;
import org.jreleaser.model.Docker;
import org.jreleaser.model.JReleaserContext;
import org.jreleaser.model.Project;
import org.jreleaser.model.Registry;
import org.jreleaser.model.releaser.spi.Releaser;
import org.jreleaser.model.tool.spi.ToolProcessingException;
import org.jreleaser.util.Constants;
import org.jreleaser.util.PlatformUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.jreleaser.templates.TemplateUtils.trimTplExtension;
import static org.jreleaser.util.MustacheUtils.applyTemplate;
import static org.jreleaser.util.MustacheUtils.passThrough;
import static org.jreleaser.util.StringUtils.isNotBlank;

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
public class DockerToolProcessor extends AbstractToolProcessor<Docker> {
    public DockerToolProcessor(JReleaserContext context) {
        super(context);
    }

    @Override
    protected boolean doPackageDistribution(Distribution distribution, Map<String, Object> props) throws ToolProcessingException {
        try {
            // copy files
            Path workingDirectory = prepareAssembly(distribution, props);

            int i = 0;
            for (String imageName : getTool().getImageNames()) {
                imageName = applyTemplate(imageName, props, "image" + (i++));

                // command line
                List<String> cmd = createBuildCommand(props);
                if (!cmd.contains("-q") && !cmd.contains("--quiet")) {
                    cmd.add("-q");
                }
                cmd.add("-f");
                cmd.add(workingDirectory.resolve("Dockerfile").toAbsolutePath().toString());
                cmd.add("-t");
                cmd.add(imageName);
                cmd.add(workingDirectory.toAbsolutePath().toString());
                context.getLogger().debug(String.join(" ", cmd));

                context.getLogger().info(" - {}", imageName);
                // execute
                executeCommand(cmd);
            }
        } catch (IOException e) {
            throw new ToolProcessingException(e);
        }

        return true;
    }

    private Path prepareAssembly(Distribution distribution, Map<String, Object> props) throws IOException, ToolProcessingException {
        Path packageDirectory = (Path) props.get(Constants.KEY_PACKAGE_DIRECTORY);
        copyPreparedFiles(distribution, props);
        Path assemblyDirectory = packageDirectory.resolve("assembly");

        Set<String> fileExtensions = tool.getSupportedExtensions();
        List<Artifact> artifacts = distribution.getArtifacts().stream()
            .filter(artifact -> fileExtensions.stream().anyMatch(ext -> artifact.getPath().endsWith(ext)))
            .collect(Collectors.toList());

        for (Artifact artifact : artifacts) {
            Path artifactPath = artifact.getEffectivePath(context);
            Files.copy(artifactPath, assemblyDirectory.resolve(artifactPath.getFileName()), REPLACE_EXISTING);
        }

        return packageDirectory;
    }

    private List<String> createBuildCommand(Map<String, Object> props) {
        List<String> cmd = createCommand("build");
        for (int i = 0; i < getTool().getBuildArgs().size(); i++) {
            String arg = getTool().getBuildArgs().get(i);
            if (arg.contains("{{")) {
                cmd.add(applyTemplate(arg, props, "arg" + i));
            } else {
                cmd.add(arg);
            }
        }
        return cmd;
    }

    private List<String> createCommand(String name) {
        List<String> cmd = new ArrayList<>();
        cmd.add("docker" + (PlatformUtils.isWindows() ? ".exe" : ""));
        cmd.add("-l");
        cmd.add("error");
        cmd.add(name);
        return cmd;
    }

    @Override
    public boolean uploadDistribution(Distribution distribution, Releaser releaser, Map<String, Object> props) throws ToolProcessingException {
        if (tool.getRegistries().isEmpty()) {
            context.getLogger().info("no configured registries. Skipping");
            return false;
        }
        return super.uploadDistribution(distribution, releaser, props);
    }

    @Override
    protected boolean doUploadDistribution(Distribution distribution, Releaser releaser, Map<String, Object> props) throws ToolProcessingException {
        for (Registry registry : getTool().getRegistries()) {
            login(registry);
            int i = 0;
            for (String imageName : getTool().getImageNames()) {
                publish(registry, imageName, props, i++);
            }
            logout(registry);
        }

        return true;
    }

    private void login(Registry registry) throws ToolProcessingException {
        List<String> cmd = createCommand("login");
        if (isNotBlank(registry.getServer())) {
            cmd.add(registry.getServer());
        }
        cmd.add("-u");
        cmd.add(registry.getUsername());
        cmd.add("-p");
        cmd.add(registry.getResolvedPassword());

        ByteArrayInputStream in = new ByteArrayInputStream((registry.getResolvedPassword() + System.lineSeparator()).getBytes());

        context.getLogger().debug("login into {}{}",
            registry.getServerName(),
            (isNotBlank(registry.getServer()) ? " (" + registry.getServer() + ")" : ""));
        if (!context.isDryrun()) executeCommandWithInput(cmd, in);
    }

    private void publish(Registry registry, String imageName, Map<String, Object> props, int index) throws ToolProcessingException {
        imageName = applyTemplate(imageName, props, "image" + index);

        String tag = imageName;
        String serverName = registry.getServerName();
        String server = registry.getServer();
        String repositoryName = registry.getRepositoryName();

        // if serverName == DEFAULT
        //   tag: repositoryName/imageName
        // else
        //   tag: server/repositoryName/imageName

        if (Registry.DEFAULT_NAME.equals(serverName)) {
            if (!tag.startsWith(repositoryName)) {
                int pos = tag.indexOf("/");
                if (pos < 0) {
                    tag = repositoryName + "/" + tag;
                } else {
                    tag = repositoryName + tag.substring(pos);
                }
            }
        } else {
            if (!tag.startsWith(server)) {
                int pos = tag.indexOf("/");
                if (pos < 0) {
                    tag = server + "/" + repositoryName + "/" + tag;
                } else {
                    tag = server + "/" + repositoryName + tag.substring(pos);
                }
            }
        }

        if (!tag.equals(imageName)) {
            List<String> cmd = createCommand("tag");
            cmd.add(imageName);
            cmd.add(tag);

            context.getLogger().debug("tagging {} as {}", imageName, tag);
            if (!context.isDryrun()) executeCommand(cmd);
        }

        List<String> cmd = createCommand("push");
        cmd.add("-q");
        cmd.add(tag);

        context.getLogger().info(" - {}", tag);
        context.getLogger().debug("pushing {} to {}{}",
            tag,
            registry.getServerName(),
            (isNotBlank(registry.getServer()) ? " (" + registry.getServer() + ")" : ""));
        if (!context.isDryrun()) executeCommand(cmd);
    }

    private void logout(Registry registry) throws ToolProcessingException {
        List<String> cmd = createCommand("logout");
        if (isNotBlank(registry.getServer())) {
            cmd.add(registry.getServerName());
        }

        context.getLogger().debug("logout from {}{}",
            registry.getServerName(),
            (isNotBlank(registry.getServer()) ? " (" + registry.getServer() + ")" : ""));
        if (!context.isDryrun()) executeCommand(cmd);
    }

    @Override
    protected void fillToolProperties(Map<String, Object> props, Distribution distribution) throws ToolProcessingException {
        props.put(Constants.KEY_DOCKER_BASE_IMAGE,
            applyTemplate(getTool().getBaseImage(), props, "baseImage"));

        List<String> labels = new ArrayList<>();
        getTool().getLabels().forEach((label, value) -> labels.add(passThrough("\"" + label + "\"=\"" +
            applyTemplate(value, props, label) + "\"")));
        props.put(Constants.KEY_DOCKER_LABELS, labels);
        props.put(Constants.KEY_DOCKER_PRE_COMMANDS, tool.getPreCommands().stream()
            .map(c -> passThrough(applyTemplate(c, props)))
            .collect(Collectors.toList()));
        props.put(Constants.KEY_DOCKER_POST_COMMANDS, tool.getPostCommands().stream()
            .map(c -> passThrough(applyTemplate(c, props)))
            .collect(Collectors.toList()));
    }

    @Override
    protected void writeFile(Project project, Distribution distribution, String content, Map<String, Object> props, String fileName)
        throws ToolProcessingException {
        fileName = trimTplExtension(fileName);

        Path outputDirectory = (Path) props.get(Constants.KEY_PREPARE_DIRECTORY);
        Path outputFile = "executable".equals(fileName) ?
            outputDirectory.resolve("assembly").resolve(distribution.getExecutable()) :
            outputDirectory.resolve(fileName);

        if (distribution.getType() == Distribution.DistributionType.NATIVE_IMAGE &&
            outputFile.getFileName().toString().equals(distribution.getExecutable())) {
            return;
        }

        writeFile(content, outputFile);
    }
}
