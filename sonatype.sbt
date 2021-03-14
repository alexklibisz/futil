// Your profile name of the sonatype account. The default is the same with the organization value
sonatypeProfileName := "com.klibisz.elastiknn"

// To sync with Maven central, you need to supply the following information:
publishMavenStyle := true

// Open-source license of your choice
licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

// Where is the source code hosted?
homepage := Some(url("https://github.com/alexklibisz/futil"))
scmInfo := Some(
  ScmInfo(
    url("https://github.com/alexklibisz/futil"),
    "scm:git@github.com:alexklibisz/futil.git"
  )
)
developers := List(
  Developer(id="alexklibisz", name="Alex Klibisz", email="aklibisz@gmail.com", url=url("https://alexklibisz.com"))
)