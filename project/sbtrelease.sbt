// To set the version, just strip -SNAPSHOT from the version.
// "0.0.4-PRE1-SNAPSHOT" -> "0.0.4-PRE1"
releaseVersion := { _.replace("-SNAPSHOT", "") }

// To set the next version, increment the last number and append -SNAPSHOT.
// "0.0.4-PRE1" -> "0.0.4-PRE2-SNAPSHOT"
releaseNextVersion := { v: String =>
  "[0-9]+".r
    .findAllMatchIn(v)
    .toList
    .lastOption
    .map(m => v.take(m.start) ++ s"${m.toString.toInt + 1}" ++ v.drop(m.`end`) + "-SNAPSHOT")
    .getOrElse(v)
}
