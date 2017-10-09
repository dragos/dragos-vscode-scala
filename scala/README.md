# An Ensime-based Scala Language Server

This extension installs an Ensime-based Scala language server. At the moment, the following features are supported:

- errors as you type
- code completion
- goto definition (F12 and CMD-F12)
- hover
- file structure (definitions)

If you find this useful, please file tickets and contribute fixes. I'm working on this in my spare time and fix those issues that prevent me from using it, but I can't guarantee more than that.

If you can't contribute your time but would like to donate, please [donate to the Ensime project](http://ensime.org/sponsor/) instead. This plugin delegates to Ensime for most of its features.

# Setting up

This extension is based on [Ensime](http://ensime.org), so you need to create an Ensime configuration file before you can use it. This file lists source directories, classpath entries and compiler arguments. If you have an Sbt project simply add the [sbt-ensime](http://ensime.org/build_tools/sbt/) plugin and follow the guide (other build tools are [supported](http://ensime.org/build_tools/)). Then just run `sbt ensimeConfig` and voilà! You're all set up.

>Remember to regenerate this file everytime you change your build (adding/removing dependencies, compiler arguments, etc.).

If you already started Code, it should detect that a new `.ensime` file was created and pick up the project.

# Setting the JDK

The path to the Java Development Kit is searched in the following order:

- the `JDK_HOME` environment variable
- the `JAVA_HOME` environment variable
- on the current system path

# Configuration

If VSCode is running behind a proxy add the following standard VSCode proxy settings (File -> Preferences -> Settings):

```
{
    "http.proxy": "http://host:port/"
}
```

This setting is translated as Coursier's vm arguments: -Dhttp.proxyHost=host -Dhttps.proxyHost=host -Dhttp.proxyPort=port -Dhttps.proxyPort=port.

Language server setttings:

```
{
    "scalaLanguageServer.logLevel" : "DEBUG"
    "scalaLanguageServer.heapSize" : "768M"
}
```

These settings are passed to the Language Server affecting the log level on the server, with possible values "DEBUG", "ERROR", "INFO", "WARN". The heap size used by the Scala language server, for example `512m` or `4G`, can also be configured. By default it will use 768M, which is probably insufficient for larger projects.
