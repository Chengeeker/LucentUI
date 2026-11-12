pluginManagement {
    resolutionStrategy { eachPlugin { if (requested.id.id.startsWith("com.android.")) useModule("com.android.tools.build:gradle:${requested.version}") } }
    repositories { maven("https://maven.aliyun.com/repository/google"); maven("https://maven.aliyun.com/repository/public");  google(); mavenCentral(); maven("https://jitpack.io"); gradlePluginPortal(); maven("https://maven.aliyun.com/repository/google"); maven("https://maven.aliyun.com/repository/gradle-plugin") }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral(); maven("https://jitpack.io"); maven("https://maven.aliyun.com/repository/google"); maven("https://maven.aliyun.com/repository/public") }
}
rootProject.name="LucentUI"
include(":lucent-ui",":demo")
