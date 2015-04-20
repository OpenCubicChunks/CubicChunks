
import os
import sys


# settings
PathSsjb = "../../../minecraft/ssjb"
DirLib = "lib"
DirBin = "bin"
DirBuild = "build"
Name = "tallWorlds"
Version = "0.1b"


# import ssjb
sys.path.insert(0, PathSsjb)
import ssjb
import ssjb.ivy


# dependencies
Deps = [
	ssjb.ivy.Dep("org.mapdb:mapdb:1.0.7"),
	ssjb.ivy.Dep("com.flowpowered:flow-noise:1.0.0")
]
TestDeps = [
	ssjb.ivy.Dep("junit:junit:4.12"),
	ssjb.ivy.Dep("org.hamcrest:hamcrest-all:1.3")
]


# tasks

def taskGetDeps():
	ssjb.file.mkdir(DirLib)
	ssjb.ivy.makeLibsJar(os.path.join(DirLib, "libs.jar"), Deps)
	ssjb.ivy.makeLibsJar(os.path.join(DirLib, "test-libs.jar"), TestDeps)

def taskBuild():
	ssjb.file.delete(DirBuild)
	ssjb.file.mkdir(DirBuild)
	with ssjb.file.TempDir(os.path.join(DirBuild, "tmp")) as dirTemp:
		ssjb.file.copyTree(dirTemp, DirBin, ssjb.file.find(DirBin))
		for path in ssjb.ivy.getJarPaths(Deps):
			ssjb.jar.unpackJar(dirTemp, path)
		ssjb.file.delete(os.path.join(dirTemp, "LICENSE.txt"))
		ssjb.file.delete(os.path.join(dirTemp, "META-INF/maven"))
		ssjb.jar.makeJar(os.path.join(DirBuild, "%s-v%s.jar" % (Name, Version)), dirTemp)


ssjb.registerTask("getDeps", taskGetDeps)
ssjb.registerTask("build", taskBuild)
ssjb.run()


