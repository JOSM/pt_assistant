# Contributing to this project

Contributions to this project are always welcome! You can either contribute translations of the texts used in the plugin, or contribute to the code.

## Translation

Translations are managed at Launchpad: https://josm.openstreetmap.de/wiki/Translations

You can contribute translations for the texts used in this project at the link above.

🇬🇧 English is used as the baseline language by the developers, so changes to English texts need to be made in the code (see next section about code changes).

## Code

> **Quick note beforehand:** Before you start working on a big new feature, you are welcome to [open a new issue](https://github.com/JOSM/pt_assistant/issues/new/choose) describing the planned changes.
>
> In case there is already a ticket open in [the JOSM trac](https://josm.openstreetmap.de/query?status=assigned&status=needinfo&status=new&status=reopened&component=Plugin+pt_assistant&col=id&col=summary&col=status&col=component&col=type&col=priority&col=milestone&col=time&col=changetime&report=1&desc=1&order=changetime) or on [GitHub](https://github.com/JOSM/pt_assistant/issues), just comment there that you intend to work on this.
>
> That way the other developers are aware you are already working on that and could also give feedback on your plans from the start.


To get started with contributing code, make sure you have [Git](https://git-scm.com) and also a [Java Development Kit (JDK)](https://adoptopenjdk.net) of version 8 or later.

---

Clone the repository by going to the directory into which the project directory should go and use **one of** these lines in the command line:
```shell script
git clone https://github.com/JOSM/pt_assistant.git
git clone https://gitlab.com/JOSM/plugin/pt_assistant.git
```

---

Then you can enter the newly created project directory `pt_assistant` and should be able to build the project.

We are using Gradle as build system, so you can run the following to see which tasks are available to run on the project:

On Windows:
```shell script
gradlew.bat tasks
```
On other OSes:
```shell script
./gradlew tasks
```

---

To then run a task of your choice, run the following on the command line (replace `‹task_name›` with the name of the task, of course 😉):

On Windows:
```shell script
gradlew.bat ‹task_name›
```
On other OSes:
```shell script
./gradlew ‹task_name›
```

These are some tasks that will probably be useful for development:
* `runJosm`: compiles the plugin and starts a clean JOSM instance (independent of any JOSM installations on your system) with just the current state of the plugin loaded
* `build`: runs a complete build including running unit tests
* `test`: runs just the unit tests with reports in `$projectDir/build/reports/`
* `dist`: builds the distribution *.jar file in `$projectDir/build/dist/`
* `compileJava_minJosm`, `compileJava_testedJosm`, `compileJava_latestJosm`: compiles the plugin against the JOSM version set as minimum compatible version, or the current `latest` or `tested` JOSM versions
* …

The `gradle-josm-plugin` also has some documentation on how to develop Gradle-based JOSM plugins: [https://gitlab.com/floscher/gradle-josm-plugin](https://gitlab.com/floscher/gradle-josm-plugin/-/blob/master/README.md)

---

Feel free to use the IDE of your choice, usually when the IDE supports it (e.g. IntelliJ idea or Eclipse do) you should be able to import as a Gradle project relatively easily. For IntelliJ idea and Eclipse there are also some files with settings already present that should make it easier to open the project in those IDEs.

---

If you want to submit your code contribution, fork [our GitHub repository](https://github.com/JOSM/pt_assistant), push your changes to the fork and [open a pull request](https://github.com/JOSM/pt_assistant/compare). We will then review the code and merge the pull request, if it looks good.
