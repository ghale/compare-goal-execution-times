# Gradle Enterprise API Samples

This repository demonstrates using the Gradle Enterprise API and generating client code from its OpenAPI specification.

The main class captures Maven builds from two different groups and compares the average goal execution time between the groups.  The groups are determined by specifying two build scan ids - the scan id to start the first group, and the scan id to start the second group (as well as project name and requested goals).  Then it will grab up to 20 builds from each group, average the goal execution times and do a comparison.  For a large number of goals, you can specify a threshold such that only goal execution differences larger than the threshold will be shown.

## How to build

Execute:

```
$ ./gradlew install
```

This builds and installs the program into `build/install/compare-goal-execution-times`.
You can use the `build/install/compare-goal-execution-times/bin/compare-goal-execution-times` script to run the sample.

## How to run

A Gradle Enterprise access key with the “Export build data via the API” permission is required.

To create an access key:

1. Sign in to Gradle Enterprise.
2. Access "My settings" from the user menu in the top right-hand corner of the page.
3. Access "Access keys" from the left-hand menu.
4. Click "Generate" on the right-hand side and copy the generated access key.

The access key should be saved to a file, which will be supplied as a parameter to the program.

Next, execute:

```
$ build/install/compare-goal-execution-times/bin/compare-goal-execution-times --server-url=«serverUrl» --access-key-file=«accessKeyFile» --project-name=«projectName» --requested-goals=«listOfGoals» --threshold=«timeInMs» --first-scan-id=«firstScanId» --second-scan-id=«secondScanId»
```

- `«serverUrl»`: The address of your Gradle Enterprise server (e.g. `https://ge.example.com`)
- `«accessKeyFile»`: The path to the file containing the access key
- `«projectName»`: The name of the project to limit reporting to
- `«requested-goals»`: A comma-separated list of the goals to filter builds for
- `«threshold»`: A threshold in ms for displaying goal execution differences.  Differences below the threshold will not be displayed
- `«firstScanId»`: A scan id that represents the start of the first group of builds
- `«secondScanId»`: A scan id that represents the start of the second group of builds

## About the code generation

This sample uses [`openapi-generator`](https://openapi-generator.tech) `5.4.0` to generate client code from the Gradle Enterprise API specification.
This version has [a bug that causes incorrect client code to be generated](https://github.com/OpenAPITools/openapi-generator/issues/4808).

To work around this issue, this sample [customizes the generator](openApi/openapi-generator-config.json) to use a [custom template](openApi/api.mustache) for code generation.

This [has been fixed](https://github.com/OpenAPITools/openapi-generator/pull/11682) for an upcoming release.
Once the fix is released, this sample will be updated and the workaround removed.

## Further documentation

The Gradle Enterprise API manual and reference documentation for each version of the API can be found [here](https://docs.gradle.com/enterprise/api-manual).


## License

This project is open-source software released under the [Apache 2.0 License][apache-license].

[apache-license]: https://www.apache.org/licenses/LICENSE-2.0.html
