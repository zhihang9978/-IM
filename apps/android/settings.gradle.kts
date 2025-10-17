pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        // 腾讯云TRTC SDK Maven仓库
        maven { url = uri("https://mirrors.tencent.com/repository/maven/") }
    }
}

rootProject.name = "LanxinIM"
include(":app")

