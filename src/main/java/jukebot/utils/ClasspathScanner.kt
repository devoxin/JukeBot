package jukebot.utils

import java.io.File

public fun getClasses(packageDir: String): List<Class<*>> {
    val classLoader = Thread.currentThread().contextClassLoader ?: return emptyList()
    val path = packageDir.replace(".", "/")

    val resources = classLoader.getResources(path)
    val directories: MutableList<File> = ArrayList()
    val classes: MutableList<Class<*>> = ArrayList()

    while (resources.hasMoreElements()) {
        val resource = resources.nextElement()
        directories.add(File(resource.file))
    }

    for (directory in directories) {
        classes.addAll(findClassesInDirectory(directory, packageDir))
    }

    return classes
}

private fun findClassesInDirectory(directory: File, packageDir: String): List<Class<*>> {
    if (!directory.exists()) {
        return emptyList()
    }

    val classes: MutableList<Class<*>> = ArrayList()

    for (file in directory.listFiles()) {
        if (file.isDirectory && !file.name.contains(".")) {
            classes.addAll(findClassesInDirectory(file, "$packageDir.${file.name}"))
        } else if (file.name.endsWith(".class")) {
            classes.add(Class.forName("$packageDir.${file.name.substring(0, file.name.length - 6)}"))
        }
    }

    return classes
}
