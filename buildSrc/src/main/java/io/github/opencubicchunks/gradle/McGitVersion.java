package io.github.opencubicchunks.gradle;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Supplier;

public class McGitVersion implements Plugin<Project> {

    @Override public void apply(Project target) {
        McGitVersionExtension extension = new McGitVersionExtension();
        target.getExtensions().add("mcGitVersion", extension);
        extension.configured = true;
        target.setVersion(lazyString(() -> {
            String version = getVersion(extension, target, false);
            target.getLogger().lifecycle("Auto-detected version {} for project(\"{}\")", version, target.getPath());
            return version;
        }));
        target.getExtensions().getExtraProperties().set("mavenProjectVersion", lazyString(() -> {
            String version = getVersion(extension, target, true);
            target.getLogger().lifecycle("Auto-detected version for maven {} for project(\"{}\")", version, target.getPath());
            return version;
        }));
    }

    private String getVersion(McGitVersionExtension extension, Project target, boolean maven) {
        if (extension.getForceVersionString() != null) {
            return extension.getForceVersionString();
        }
        if (!extension.configured) {
            throw new IllegalStateException("Accessing project version before mcGitVersion is configured! Configure mcGitVersion first!");
        }
        try {
            Git git = openRepository(target.getProjectDir().toPath());
            target.getLogger().info("Found git repository " + git.getRepository().getDirectory() + " for project " + target);
            GitVersionInfo describe = manualDescribe(target, git, extension.getCommitVersions());
            String branch = getGitBranch(git);
            String snapshotSuffix = extension.isSnapshot() ? "-SNAPSHOT" : "";
            return getModVersion(target, extension, describe, branch, maven) + snapshotSuffix;
        } catch (RuntimeException | IOException ex) {
            target.getLogger().error("Unknown error when accessing git repository! Are you sure the git repository exists?", ex);
            return String.format("%s-%s.%s.%s%s%s", getMcVersion(extension), "9999", "9999", "9999", "", "NOVERSION");
        }
    }

    private GitVersionInfo manualDescribe(Project project, Git git, Map<String, String> commitVersions) throws IOException {
        Repository repository = git.getRepository();
        int shortest = Integer.MAX_VALUE;
        String shortestVersion = null;
        String shortestCommit = null;
        commitVersionLoop:
        for (Map.Entry<String, String> entry : commitVersions.entrySet()) {
            String commit = entry.getKey();
            String version = entry.getValue();
            project.getLogger().lifecycle("Finding version string: attempting base version " + version);
            RevWalk revWalk = new RevWalk(repository);
            RevCommit oldCommit = revWalk.parseCommit(repository.resolve(commit));
            RevCommit current = revWalk.parseCommit(repository.resolve("HEAD"));
            revWalk.markStart(current);
            revWalk.markUninteresting(oldCommit);
            revWalk.setRetainBody(false);
            int commitCount = 0;
            for (RevCommit revCommit : revWalk) {
                if (revCommit.getParentCount() == 0 && !revCommit.equals(oldCommit)) {
                    project.getLogger().info("Reached the end of commit tree for version " + version);
                    revWalk.close();
                    continue commitVersionLoop;
                }
                commitCount++;
            }
            if (commitCount < shortest) {
                shortest = commitCount;
                shortestVersion = version;
                shortestCommit = commit;
            } else if (commitCount == shortest) {
                project.getLogger().warn("Potentially ambiguous version detection: The same amount of commits since " +
                        commit + "(version=" + version + ") as since " + shortestCommit + " (version=" + shortestVersion + ")");
            }
            revWalk.close();
        }
        if (shortestVersion == null) {
            throw new RuntimeException("No version for current commit!");
        }
        return new GitVersionInfo(shortestVersion, shortest);
    }

    private Git openRepository(Path path) throws RepositoryNotFoundException {
        while (path.getParent() != null) {
            try {
                return Git.open(path.toFile());
            } catch (RepositoryNotFoundException ignored) {
                path = path.getParent();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        throw new RepositoryNotFoundException(path.toFile());
    }

    private String getMcVersion(McGitVersionExtension ext) {
        return ext.getForgeVersion().split("-")[0];
    }

    private String getGitBranch(Git git) throws IOException {
        String branch = git.getRepository().getBranch();
        if (branch.equals("HEAD")) {
            branch = firstNonEmpty(
                    () -> new RuntimeException("Found HEAD branch! This is most likely caused by detached head state! Will assume unknown version!"),
                    System.getenv("TRAVIS_BRANCH"),
                    System.getenv("GIT_BRANCH"),
                    System.getenv("BRANCH_NAME"),
                    System.getenv("GITHUB_HEAD_REF")
            );
        }

        if (branch.startsWith("origin/")) {
            branch = branch.substring("origin/".length());
        }
        return branch;
    }


    private String getModVersion(Project target, McGitVersionExtension extension, GitVersionInfo describe, String branch, boolean mvn) {
        String mcVersion = getMcVersion(extension);
        if (branch.startsWith("MC_")) {
            String branchMcVersion = branch.substring("MC_".length());
            if (!mcVersion.startsWith(branchMcVersion)) {
                target.getLogger().warn("Branch version different than project MC version! MC version: " +
                        mcVersion + ", branch: " + branch + ", branch version: " + branchMcVersion);
            }
        }

        //branches "master" and "MC_something" are not appended to version string, everything else is
        //only builds from "master" and "MC_version" branches will actually use the correct versioning
        //but it allows to distinguish between builds from different branches even if version number is the same
        String branchSuffix = (branch.equals("master") || branch.startsWith("MC_")) ? "" :
                ("-" + branch.replaceAll("[^a-zA-Z0-9.-]", "_"));
        String versionSuffix = extension.getVersionSuffix();
        String modAndApiVersion = describe.baseVersion;

        int minor = describe.commitsSinceBase;
        int patch = 0;

        return (mvn) ? String.format("%s-%s%s", mcVersion, modAndApiVersion, versionSuffix)
                : String.format("%s-%s.%d.%d%s%s", mcVersion, modAndApiVersion, minor, patch, versionSuffix, branchSuffix);
    }

    private static String firstNonEmpty(Supplier<RuntimeException> exception, String... items) {
        for (String i : items) {
            if (i != null && !i.isEmpty()) {
                return i;
            }
        }
        throw exception.get();
    }

    private Object lazyString(Supplier<String> getString) {
        return new Object() {
            private String cached;

            @Override public String toString() {
                if (cached == null) {
                    cached = getString.get();
                }
                return cached;
            }
        };
    }

    private static class GitVersionInfo {
        String baseVersion;
        int commitsSinceBase;

        public GitVersionInfo(String baseVersion, int commitsSinceBase) {
            this.baseVersion = baseVersion;
            this.commitsSinceBase = commitsSinceBase;
        }
    }
}