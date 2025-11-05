plugins {
    id("studio.o7.remora") version "0.3.6"
}

group = "studio.o7"

information {
    artifactId = "gentle"
    description = "A small, ergonomic Java utility library providing " +
            "functional programming primitives and a Go-inspired Context system " +
            "for value propagation, deadlines and cancellation in concurrent and asynchronous workflows."
    url = "https://www.o7.studio"

    developers {
        developer {
            id = "julian-siebert"
            name = "Julian Siebert"
            email = "mail@julian-siebert.de"
        }
    }

    scm {
        connection = "scm:git:git://github.com/o7studios/gentle.git"
        developerConnection = "scm:git:git@https://github.com/o7studios/gentle.git"
        url = "https://github.com/o7studios/gentle"
        tag = "HEAD"
    }

    ciManagement {
        system = "GitHub Actions"
        url = "https://github.com/o7studios/gentle/actions"
    }

    licenses {
        license {
            name = "MIT License"
            url = "https://raw.githubusercontent.com/o7studios/gentle/refs/heads/main/LICENSE"
        }
    }
}