# Sample Play! Deb Packaging #

This repo is a sample Play! 2.1 project that packages itself according to Coursera's typical best practices. For an overview, please see: [Deploying Play! Apps](http://betacs.pro/blog/2013/08/03/deploying-play-apps/). To package this Play! app, simply run `deb` from the Play! interactive console. You will find the generated deb inside the `target` folder.

Note: Within Coursera, packaging, helpers and more are shared between projects using the "PlayCour" plugin. e.g. A typical `project/plugins.sbt` file would have the following line appended:

```sbt
// Include the playcour tooling
addSbtPlugin("org.coursera.playcour" % "sbt-plugin" % "2.11.0")
```

Instead in this project, a subset of the plugin is copied directly into the `project` folder. The settings are found in both `Build.scala` and `DebHelpers.scala`.

Note: the SBT Native Packager relies on tools such as `fakeroot` to be installed. If you see an error that looks like:

```
[error] (debian:package-bin) java.io.IOException: Cannot run program "fakeroot" (in directory "/Users/saeta/src/personal/play-deb-packaging/target/coursera-play-deb-packaging-1.0.0"): error=2, No such file or directory
[error] Total time: 2 s, completed Aug 27, 2013 1:45:52 PM
[play-deb-packaging] $
```

you probably don't have the appropriate tools installed. Consider running this command inside an Ubuntu-based machine. If you are having trouble, let me know!
