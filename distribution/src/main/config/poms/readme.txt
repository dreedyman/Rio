This directory contains POM files for jars included in the Rio distribution. This will allow the corresponding services
that use these jars as their codebase to in turn use the artifact URL to annotate their codebase. The Rio resolver
has been configured to use this directory as a flat repository

This approach is taken to avoid configuring a remote repository, and downloading the artifact(s) remotely.
