package com.timing.plugin;

import javassist.ClassPool
import javassist.CtClass
import javassist.CtField
import javassist.CtMethod
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.logging.Logger

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.regex.Matcher
import java.util.zip.ZipEntry

class SensorsAnalyticsInject {
    private static ClassPool pool = ClassPool.getDefault()
    private static final String PLUGIN_LIBRARY = "com.time.timeplugin.block"

    static void appendClassPath(String libPath) {
        pool.appendClassPath(libPath);
    }

    /**
     * 这里需要将jar包先解压，注入代码后再重新生成jar包
     *
     * @path jar包的绝对路径
     */
    static File injectJar(String path, Project project) {
        appendClassPath(path);
        if (path.endsWith(".jar")) {
            pool.appendClassPath(project.android.bootClasspath[0].toString());
            File jarFile = new File(path);
            // jar包解压后的保存路径
            String jarZipDir = jarFile.getParent() + File.separator + jarFile.getName().replace(".jar", "");

            // 解压jar包, 返回jar包中所有class的完整类名的集合（带.class后缀）
            List<File> classNameList = unzipJar(path, jarZipDir);

            // 删除原来的jar包
            jarFile.delete();

            // 注入代码
            pool.appendClassPath(jarZipDir);
            for (File classFile : classNameList) {
//                injectClass(classFile, jarZipDir);
            }

            // 重新打包jar
            zipJar(jarZipDir, path);

            // 删除目录
            try {
                FileUtils.deleteDirectory(new File(jarZipDir))
            } catch (IOException e) {
                e.printStackTrace();
            }

            return jarFile;
        }

        return null;
    }

    private static void injectClass(Logger logger, File classFile, String path) {
        String filePath = classFile.absolutePath
        if (!filePath.endsWith(".class")) {
            return
        }

        if (!filePath.contains('R$')
                && !filePath.contains('R2$')
                && !filePath.contains('R.class')
                && !filePath.contains('R2.class')
                && !filePath.contains('BuildConfig.class')) {
            String className = filePath.substring(path.length() + 1, filePath.length() - 6).replaceAll(Matcher.quoteReplacement(File.separator), ".");
            if (!className.startsWith("android") && !className.startsWith(PLUGIN_LIBRARY)) {
                CtClass ctClass = pool.getCtClass(className);
                //解冻
                if (ctClass.isFrozen()) {
                    ctClass.defrost();
                }
                for (CtMethod currentMethod : ctClass.getDeclaredMethods()) {
                    logger.warn(" currentMethod name: " + currentMethod.getName())
                    //给类添加计时变量
//                    CtField startTime = new CtField(CtClass.longType, "sStart", c);
//                    startTime.setModifiers(Modifier.STATIC);
//                    c.addField(startTime);

                    currentMethod.addLocalVariable("start", CtClass.longType)
                    currentMethod.insertBefore("start = System.currentTimeMillis();")

//                    currentMethod.addLocalVariable("methodName", CtClass.voidType)
//                    currentMethod.insertBefore("methodName = " + currentMethod.getName() + ";")
                    StringBuilder endInjectStr = new StringBuilder();
                    endInjectStr.append("com.time.timeplugin.block.BlockManager.timingMethod(")
                    endInjectStr.append("\"" + currentMethod.getName() + "\"")
                    endInjectStr.append(",start);")
                    logger.warn(" currentMethod name: " +endInjectStr)

                    currentMethod.insertAfter(endInjectStr.toString())
                }
                ctClass.writeFile(path)
                ctClass.detach()//释放
            }
        }
    }

    static void injectDir(Logger logger, String path, Project project) {
        try {
            pool.appendClassPath(path);
            /**加入android.jar，不然找不到android相关的所有类*/
            pool.appendClassPath(project.android.bootClasspath[0].toString());

            File dir = new File(path);
            if (dir.isDirectory()) {
                for (File file : com.android.utils.FileUtils.getAllFiles(dir)) {
                    logger.warn((getName() + " file name: " + file.getName()))
                    injectClass(logger, file, path)
                }
            }
        } catch (Exception e) {
            e.printStackTrace()
        }
    }

    /**
     * 将该jar包解压到指定目录
     *
     * @param jarPath jar包的绝对路径
     * @param destDirPath jar包解压后的保存路径
     * @return List < File >
     */
    static List<File> unzipJar(String jarPath, String destDirPath) {
        List<File> fileList = new ArrayList<>();
        if (jarPath.endsWith(".jar")) {
            JarFile jarFile = null;
            try {
                jarFile = new JarFile(jarPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Enumeration<JarEntry> jarEntries = jarFile.entries();
            while (jarEntries.hasMoreElements()) {
                JarEntry jarEntry = jarEntries.nextElement();
                if (jarEntry.isDirectory()) {
                    continue;
                }
                try {
                    String entryName = jarEntry.getName();
                    String outFileName = destDirPath + File.separator + entryName;
                    File outFile = new File(outFileName);
                    fileList.add(outFile);
                    outFile.getParentFile().mkdirs();
                    InputStream inputStream = jarFile.getInputStream(jarEntry);
                    FileOutputStream fileOutputStream = new FileOutputStream(outFile);
                    byte[] buffer = new byte[1024];
                    int len = inputStream.read(buffer);
                    while (len != -1) {
                        fileOutputStream.write(buffer, 0, len);
                        len = inputStream.read(buffer);
                    }
                    fileOutputStream.close();
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                jarFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return fileList;
    }

    /**
     * 重新打包jar
     *
     * @param packagePath 将这个目录下的所有文件打包成jar
     * @param destPath 打包好的jar包的绝对路径
     */
    static void zipJar(String packagePath, String destPath) {
        File file = new File(packagePath);
        JarOutputStream outputStream = null;
        try {
            outputStream = new JarOutputStream(new FileOutputStream(destPath));
            for (File f : file.listFiles()) {
                String entryName = f.getAbsolutePath().substring(file.getAbsolutePath().length() + 1);
                outputStream.putNextEntry(new ZipEntry(entryName));
                if (!f.isDirectory()) {
                    InputStream inputStream = new FileInputStream(f);
                    byte[] buffer = new byte[1024];
                    int len = inputStream.read(buffer);
                    while (len != -1) {
                        outputStream.write(buffer, 0, len);
                        len = inputStream.read(buffer);
                    }
                    inputStream.close();
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}