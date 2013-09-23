HelloWorld: Accessing the Box API from Java
===========================================

A basic hello world type sample Java app.  You should be able to put this into
your java IDE and with a few mods have a directory listing.

* Change the public-static-finals at the top of `HelloWorld.java` to be your
  API-key and Secret
* Go into your app settings at <https://app.box.com/developers/services> and
  set your OAuth2 `redirect_url` to <http://localhost:4000> in your
  Box-application settings like <https://www.box.com/s/k7qn4ls8kiyvvkslrcb8>
  Note that non SSL connections will only work to localhost.  If you want to
  use some other URL, you'll need to setup SSL certs
* Compile and run from the command line with `ant run`

Note that the Ant build file takes care of setting up your Java classpath. If
you want to use this sample code as a starting point for your own project, make
sure you have `java.awt.Desktop`, plus all of the jar files in
`BoxJavaLibraryV2/libs`, in your classpath (see [this
post](http://stackoverflow.com/questions/17277917/where-do-i-get-the-missing-resources)
if you don't know what this means).
