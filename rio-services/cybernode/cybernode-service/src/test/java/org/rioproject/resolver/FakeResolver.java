package org.rioproject.resolver;

import java.io.File;
import java.net.URL;
import java.util.Collection;

public class FakeResolver implements Resolver {

    @Override
    public String[] getClassPathFor(String artifact,
                                    File pom,
                                    boolean download)  {
        return new String[0];
    }

    @Override
    public String[] getClassPathFor(String artifact)  {
        return new String[0];
    }

    @Override
    public String[] getClassPathFor(String artifact,
                                    RemoteRepository[] repositories)  {
        return new String[0];
    }

    @Override
    public URL getLocation(String artifact,
                           String artifactType)  {
        return null;
    }

    @Override
    public URL getLocation(String artifact,
                           String artifactType,
                           RemoteRepository[] repositories)  {
        return null;
    }

    @Override
    public Collection<RemoteRepository> getRemoteRepositories() {
        return null;
    }
}
