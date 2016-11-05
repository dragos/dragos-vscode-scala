# Scala language server for VS Code

![demo](code.gif "Demo")


This is an experiment for building a Language Server for Scala, in Scala.

- language server: A Scala-based implementation of the [language server protocol](https://github.com/Microsoft/language-server-protocol/blob/master/protocol.md)
- scala: A Typescript-based Scala extension (language client). Ideally it will be ported to Scala.js
- ensime-server: An implementation of the Language Server based on Ensime

The language server may be backed up by [ensime](http://ensime.github.io/) or directly by the presentation compiler. Ideally, the language server ca be used as a basis for implementing support for any language, not just Scala.

# How to try it out

tl;dr

```bash
$ sbt publishExtension
$ cd scala
$ vsce package
```

You shuld see a file `ensime-scala-0.0.3.vsix` (or whatever version you are building). Now install it in Code by choosing `Install from VSIX` in the Extensions view.

## Building

The root Sbt project controls all the Scala parts of the build. The client is written in Typescript (it's really minimal) and lives under scala/. This one is built using Code's tools.

- languageserver/ contains the language-independent server implementation. It does not implement the full protocol yet. Features are added by-need, when the reference implementation in ensime-server/ needs it
- ensime-server/ implements an Ensime based Scala language server
- scala/ The typescript extension (eventually should migrate to Scala.js)

The ensime-server is what you will want to build most of the times. It's using `assembly` to build a fat jar, so the client can launch it as simply as possible.

You should use `sbt publishExtension` which copies the fat jar into a directory under scala/server, so the client finds it easily.

## Running

You can open code inside the `scala/` directory and use `F5` to debug the extension. This picks up the changes in the server (make sure you copied the fat jar using `sbt publishExtension`!) and allows quick iteration.