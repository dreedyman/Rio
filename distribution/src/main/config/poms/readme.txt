This directory contains POM files for third party jars included in the Rio distribution that will be
installed into your local Maven repository. This will allow the corresponding services that use these
jars as their codebase to in turn use the artifact URL to annotate their codebase.

This approach is taken to avoid configuring a remote repository, and downloading the artifact(s) remotely.
