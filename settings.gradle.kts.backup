rootProject.name = "OmniMessage-Pro"

// 应用模块
include(":app")

// 核心库模块
include(":core")

// 功能模块
include(":feature:contact")
include(":feature:excelimport")
include(":feature:voice")
include(":feature:quickactions")
include(":feature:search")
include(":feature:notification")
include(":feature:messaging")
include(":feature:command")
include(":feature:team")
include(":feature:transfer")
include(":feature:workflow")
include(":feature:analytics")
include(":feature:settings")
include(":feature:emotion")
include(":feature:scene")

// 扩展模块
include(":extension:channels")
include(":extension:plugins")
include(":extension:integrations")

// 共享模块
include(":shared")

// 启用功能模块
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// 配置仓库
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
    
    versionCatalogs {
        create("libs") {
            from(files("gradle/libs.versions.toml"))
        }
    }
}