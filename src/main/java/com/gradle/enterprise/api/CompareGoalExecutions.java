package com.gradle.enterprise.api;

import com.gradle.enterprise.api.client.ApiClient;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Command(
    name = "gradle-enterprise-api-samples",
    description = "A sample program that demonstrates using the Gradle Enterprise API to extract build data about build cache performance",
    synopsisHeading = "%n@|bold Usage:|@ ",
    optionListHeading = "%n@|bold Options:|@%n",
    commandListHeading = "%n@|bold Commands:|@%n",
    parameterListHeading = "%n@|bold Parameters:|@%n",
    descriptionHeading = "%n",
    synopsisSubcommandLabel = "COMMAND",
    usageHelpAutoWidth = true,
    usageHelpWidth = 120
)
public final class CompareGoalExecutions implements Callable<Integer> {

    @Option(
        names = "--server-url",
        description = "The address of the Gradle Enterprise server",
        required = true,
        order = 0
    )
    String serverUrl;

    @Option(
        names = "--access-key-file",
        description = "The path to the file containing the access key",
        required = true,
        order = 1
    )
    String accessKeyFile;

    @Option(
        names = "--project-name",
        description = "The name of the project to show the builds of",
        required = true,
        order = 2
    )
    String projectName;

    @Option(
        names = "--requested-goals",
        description = "Comma-separated list of goals to match on",
        order = 3
    )
    String requestedGoals;

    @Option(
        names = "--threshold",
        description = "Minimum amount of time (in ms) for a difference to be shown",
        order = 3
    )
    int threshold;

    @Option(
        names = "--first-scan-id",
        description = "The first build scan to compare",
        required = true,
        order = 4
    )
    String firstScanId;

    @Option(
        names = "--second-scan-id",
        description = "The second build scan to compare",
        required = true,
        order = 5
    )
    String secondScanId;

    public static void main(String[] args) {
        System.exit(new CommandLine(new CompareGoalExecutions()).execute(args));
    }

    @Override
    public Integer call() throws Exception {
        String serverUrl = this.serverUrl.endsWith("/")
            ? this.serverUrl.substring(0, this.serverUrl.length() - 1)
            : this.serverUrl;

        String accessKey = Files.readString(Paths.get(accessKeyFile)).trim();

        Set<String> requestedGoalSet = Arrays.stream(requestedGoals.split(",")).collect(Collectors.toSet());

        var apiClient = new ApiClient();
        apiClient.updateBaseUri(serverUrl);
        apiClient.setRequestInterceptor(request -> request.setHeader("Authorization", "Bearer " + accessKey));

        GradleEnterpriseApi api = new GradleEnterpriseApi(apiClient);
        BuildProcessor buildProcessor = new CompareGoalExecutionTimes(api, projectName, secondScanId, requestedGoalSet);
        BuildsProcessor buildsProcessor = new BuildsProcessor(api, buildProcessor);

        System.out.println("Processing builds ...");

        buildsProcessor.process(firstScanId, secondScanId);
        buildProcessor.compare(threshold);

        return 0;
    }

}
