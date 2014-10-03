/*
 * Copyright to the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rioproject.resolver.aether;

import org.eclipse.aether.repository.RepositoryPolicy;
import org.junit.Test;
import org.rioproject.config.Constants;
import org.rioproject.resolver.RemoteRepository;

import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * @author Dennis Reedy
 */
public class ResolverRepositoryTest {
    @Test
    public void testGetSetRepositories() {
        RemoteRepository r1 = create("R1");
        RemoteRepository r2 = create("R2");

        AetherResolver ar = new AetherResolver();
        List<org.eclipse.aether.repository.RemoteRepository> transformed =
            ar.transformRemoteRepository(new RemoteRepository[]{r1, r2});
        for(org.eclipse.aether.repository.RemoteRepository r : transformed) {
            RepositoryPolicy snapShotPolicy = r.getPolicy(true);
            RepositoryPolicy releasePolicy = r.getPolicy(false);
            assertTrue(snapShotPolicy.getChecksumPolicy().equals(RemoteRepository.CHECKSUM_POLICY_IGNORE));
            assertTrue(snapShotPolicy.getUpdatePolicy().equals(RemoteRepository.UPDATE_POLICY_ALWAYS));
            assertTrue(releasePolicy.getChecksumPolicy().equals(RemoteRepository.CHECKSUM_POLICY_IGNORE));
            assertTrue(releasePolicy.getUpdatePolicy().equals(RemoteRepository.UPDATE_POLICY_ALWAYS));
        }


        for(org.eclipse.aether.repository.RemoteRepository r : transformed) {
            RemoteRepository rr = ar.transformAetherRemoteRepository(r);
            assertTrue(rr.getSnapshotChecksumPolicy().equals(RemoteRepository.CHECKSUM_POLICY_IGNORE));
            assertTrue(rr.getSnapshotUpdatePolicy().equals(RemoteRepository.UPDATE_POLICY_ALWAYS));
            assertTrue(rr.getReleaseChecksumPolicy().equals(RemoteRepository.CHECKSUM_POLICY_IGNORE));
            assertTrue(rr.getReleaseUpdatePolicy().equals(RemoteRepository.UPDATE_POLICY_ALWAYS));
        }

    }

    RemoteRepository create(String id) {
        RemoteRepository r = new RemoteRepository();
        r.setId(id);
        r.setUrl(System.getProperty(Constants.CODESERVER));
        r.setSnapshotChecksumPolicy(RemoteRepository.CHECKSUM_POLICY_IGNORE);
        r.setReleaseChecksumPolicy(RemoteRepository.CHECKSUM_POLICY_IGNORE);
        r.setReleaseUpdatePolicy(RemoteRepository.UPDATE_POLICY_ALWAYS);
        r.setSnapshotUpdatePolicy(RemoteRepository.UPDATE_POLICY_ALWAYS);
        return r;
    }
}
