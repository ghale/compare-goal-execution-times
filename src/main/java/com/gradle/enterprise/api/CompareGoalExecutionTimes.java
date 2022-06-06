package com.gradle.enterprise.api;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.gradle.enterprise.api.client.ApiException;
import com.gradle.enterprise.api.model.Build;
import com.gradle.enterprise.api.model.BuildQuery;
import com.gradle.enterprise.api.model.MavenBuildCachePerformance;
import com.gradle.enterprise.api.model.MavenBuildCachePerformanceGoalExecutionEntry;

import java.util.*;

import static com.gradle.enterprise.api.model.MavenBuildCachePerformanceGoalExecutionEntry.AvoidanceOutcomeEnum.EXECUTED_NOT_CACHEABLE;

public class CompareGoalExecutionTimes implements BuildProcessor {
    enum Group { GROUP_1, GROUP_2 }

    private final Map<String, GoalExecutions> executions = Maps.newHashMap();
    private final Map<Group, Integer> buildCount = Maps.newHashMap();
    private final GradleEnterpriseApi api;
    private final String projectName;
    private final String signalBuild;
    private final Set<String> requestedGoals;
    private Group currentGroup = Group.GROUP_1;

    public CompareGoalExecutionTimes(GradleEnterpriseApi api, String projectName, String signalBuild, Set<String> requestedGoals) {
        this.api = api;
        this.projectName = projectName;
        this.signalBuild = signalBuild;
        this.requestedGoals = requestedGoals;
    }

    @Override
    public void process(Build build) {
        try {
            processMavenBuild(build);
        } catch (ApiException e) {
            reportError(build, e);
        }
    }

    private void processMavenBuild(Build build) throws ApiException {
        if (build.getId().equals(signalBuild)) {
            currentGroup = Group.GROUP_2;
        }

        int groupBuildCount = buildCount.getOrDefault(currentGroup, 0);
        if (groupBuildCount >= 20) {
            return;
        }

        System.out.println("Retrieving details for build " + build.getId() + "...");
        var attributes = api.getMavenAttributes(build.getId(), new BuildQuery());
        if (projectName != null && projectName.equals(attributes.getTopLevelProjectName()) && attributes.getRequestedGoals().containsAll(requestedGoals)) {
            var model = api.getMavenBuildCachePerformance(build.getId(), new BuildQuery());
            buildCount.put(currentGroup, groupBuildCount + 1);
            addBuild(model, currentGroup);
        }
    }

    private void reportError(Build build, ApiException e) {
        System.err.printf("API Error %s for Build Scan ID %s%n%s%n", e.getCode(), build.getId(), e.getResponseBody());
        ApiProblemParser.maybeParse(e).ifPresent(apiProblem -> {
            // Types of API problems can be checked as following
            if (apiProblem.getType().equals("urn:gradle:enterprise:api:problems:build-deleted")) {
                // Handle the case when the Build Scan is deleted.
                System.err.println(apiProblem.getDetail());
            }
        });
    }

    public void addBuild(MavenBuildCachePerformance build, Group group) {
        build.getGoalExecution()
            .stream().filter(goal -> goal.getAvoidanceOutcome() == EXECUTED_NOT_CACHEABLE)
            .forEach(goal -> executions.computeIfAbsent(getUniqueKeyForGoal(goal), s -> new GoalExecutions()).addDuration(goal.getDuration(), group));
    }

    public void compare(int threshold) {
        System.out.println("Found " + buildCount.get(Group.GROUP_1) + " builds before " + signalBuild + " and " + buildCount.get(Group.GROUP_2) + " after...");
        executions.entrySet().stream()
            .filter(entry -> entry.getValue().getDifference() > threshold)
            .sorted(Comparator.comparingDouble(entry -> entry.getValue().getDifference()))
            .forEach(CompareGoalExecutionTimes::showGoalDifference);
    }

    private static void showGoalDifference(Map.Entry<String, GoalExecutions> entry) {
        System.out.printf("%s: %.2fms, %.2fms => %.2fms\n", entry.getKey(), entry.getValue().getBuild1Average(), entry.getValue().getBuild2Average(), entry.getValue().getDifference());
    }

    private static String getUniqueKeyForGoal(MavenBuildCachePerformanceGoalExecutionEntry goal) {
        return goal.getGoalName() + " (" + goal.getGoalExecutionId() + ") @ " + goal.getGoalProjectName();
    }

    private static class GoalExecutions {
        List<Long> buildDurations1 = Lists.newArrayList();
        List<Long> buildDurations2 = Lists.newArrayList();
        double build1Average = Double.NaN;
        double build2Average = Double.NaN;

        void addDuration(Long duration, Group group) {
            switch(group) {
                case GROUP_1:
                    build1Average = Double.NaN;
                    buildDurations1.add(duration);
                    break;
                case GROUP_2:
                    build2Average = Double.NaN;
                    buildDurations2.add(duration);
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        }

        Double getDifference() {
             return getBuild2Average() - getBuild1Average();
        }

        Double getBuild1Average() {
            if (Double.isNaN(build1Average)) {
                build1Average = buildDurations2.stream().mapToDouble(value -> (double) value).average().orElse(Double.NaN);
            }
            return build1Average;
        }

        Double getBuild2Average() {
            if (Double.isNaN(build2Average)) {
                build2Average = buildDurations1.stream().mapToDouble(value -> (double) value).average().orElse(Double.NaN);
            }
            return build2Average;
        }
    }
}
