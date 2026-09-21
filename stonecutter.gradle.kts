plugins {
	id("dev.kikugie.stonecutter")
}

stonecutter active "26.3-fabric"

stonecutter registerChiseled tasks.register("chiseledBuild", stonecutter.chiseled) {
	group = "project"
	ofTask("build")
}
