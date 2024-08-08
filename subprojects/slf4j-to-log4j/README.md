# slf4j-to-log4j

Poor man's version of [log4j-slf4j-impl] without any dependency on log4j2 implementation details.

The real one does depend on implementation details, so we can't really use it because we have no control over which log4j version is in use in whatever environment our mod happens to be executed (e.g. MultiMC bumps older versions to modern log4j versions to mitigate log4shell; archloom bumps the version on 1.8.9 to have support for colors; etc).

Additionally we sidestep class loading issues we'd run into with the original one because it uses the org.apache.logging package which is excluded by default on LaunchWrapper.

To keep things simple, this implementation does not support any class loader shenanigans, nor LoggerContext, nor markers.

[log4j-slf4j-impl]: https://github.com/apache/logging-log4j2/tree/b1e6f2654ec3cdedbe926ddc02b1d74058ec6739/log4j-slf4j-impl