/**
 * Helpers for the Nexus 2 staging API used by the Apache repository at repository.apache.org.
 * Gradle's maven-publish plugin just PUTs to the implicit staging endpoint - it has no notion
 * of the staging repository Nexus creates behind the scenes, so we drive the lifecycle
 * (close in phase1, release in phase2) via the staging REST API.
 */
class NexusUtil {
    private NexusUtil() {}

    static String stagingBase(nexusBase) {
        "$nexusBase/service/local/staging"
    }

    /**
     * Finds the staging repository of the given type ('open', 'closed', ...) for the given groupId.
     * Nexus creates these implicitly, so we can't label them at deploy time - instead we match on
     * the staging profile, since the implicit repository id is the profile name minus the dots,
     * e.g. profile 'org.apache.groovy' gives 'orgapachegroovy-1118'. That is enough to ignore other
     * projects releasing at the same time; only if we have two of our own releases in flight do we
     * look at the repository contents to work out which one holds the version being released.
     * Pass a repoId to skip the search altogether.
     */
    static findRepo(client, nexusBase, user, password, String type, String groupId, String version, repoId = null) {
        def staging = stagingBase(nexusBase)
        if (repoId) {
            def response = HttpUtil.send(client, HttpUtil.getJsonRequest("$staging/repository/$repoId", user, password))
            assert response.statusCode() == 200, "Staging repository $repoId not found"
            return response.json
        }
        def response = HttpUtil.send(client, HttpUtil.getJsonRequest("$staging/profile_repositories", user, password))
        assert response.statusCode() == 200
        def matching = response.json.data.findAll { it.type == type && !it.transitioning }
        println "${type.capitalize()} staging repositories: ${matching*.repositoryId.join(', ') ?: 'none'}"

        def prefix = groupId.replace('.', '')
        def ours = matching.findAll { it.profileName == groupId || it.repositoryId?.startsWith(prefix + '-') }
        assert ours, "No $type staging repository found for $groupId " +
                "(if it is no longer $type, rerun with -PstagingRepoId=the_repo_id)"
        if (ours.size() == 1) return ours[0]

        // more than one of our own releases in flight, so pick the repo actually holding this version
        def candidates = ours.findAll { containsRelease(client, it.repositoryURI, groupId, version, user, password) }
        assert candidates.size() == 1, "Cannot tell which $type staging repository holds groovy $version: " +
                "${ours*.repositoryId} - rerun with -PstagingRepoId=the_repo_id"
        candidates[0]
    }

    /** Checks whether a staging repository holds the core pom for the version being released. */
    static boolean containsRelease(client, repoURI, String groupId, String version, user, password) {
        def url = "$repoURI/${groupId.replace('.', '/')}/groovy/$version/groovy-${version}.pom"
        // staging content is readable anonymously - only fall back to our credentials if it isn't,
        // otherwise a rejected login would look just like a repository without the artifacts
        def status = HttpUtil.send(client, HttpUtil.headRequest(url)).statusCode()
        if (status in [401, 403]) {
            status = HttpUtil.send(client, HttpUtil.headRequest(url, user, password)).statusCode()
        }
        status == 200
    }

    /**
     * Polls until the given repository stops transitioning and has the expected type.
     * A released repository is dropped automatically, so a repository which has vanished
     * also counts as done. Returns false if we gave up waiting.
     */
    static boolean awaitTransition(client, nexusBase, user, password, repoId, String expectedType,
                                   int delay = 15000, int numTries = 40) {
        def staging = stagingBase(nexusBase)
        while (numTries-- > 0) {
            sleep delay
            def response = HttpUtil.send(client, HttpUtil.getJsonRequest("$staging/repository/$repoId", user, password))
            if (response.statusCode() == 404) return true
            assert response.statusCode() == 200
            def current = response.json
            if (!current.transitioning) {
                assert current.type == expectedType,
                        "Staging repository $repoId is '$current.type' not '$expectedType' - " +
                                "check the Activity tab at $nexusBase/#stagingRepositories"
                return true
            }
        }
        false
    }
}
