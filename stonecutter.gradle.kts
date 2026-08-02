plugins {
	id("dev.kikugie.stonecutter")
}

stonecutter active "26.2-fabric"

stonecutter registerChiseled tasks.register("chiseledBuild", stonecutter.chiseled) {
	group = "project"
	ofTask("build")
}
