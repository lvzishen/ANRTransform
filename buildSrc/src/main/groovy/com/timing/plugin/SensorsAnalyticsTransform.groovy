package com.timing.plugin

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.logging.Logger

import java.nio.file.Files
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class SensorsAnalyticsTransform extends Transform {
    private Project project;
    private final Logger logger;

    SensorsAnalyticsTransform(Project project) {
        this.project = project
        this.logger = project.getLogger()
    }

    @Override
    String getName() {
        return "SensorsAnalyticsTransform"
    }

    /**
     * 需要处理的数据类型，有两种枚举类型
     * CLASSES 代表处理的 java 的 class 文件，RESOURCES 代表要处理 java 的资源
     * @return
     */
    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    /**
     * 指 Transform 要操作内容的范围，官方文档 Scope 有 7 种类型：
     * 1. EXTERNAL_LIBRARIES        只有外部库
     * 2. PROJECT                   只有项目内容
     * 3. PROJECT_LOCAL_DEPS        只有项目的本地依赖(本地jar)
     * 4. PROVIDED_ONLY             只提供本地或远程依赖项
     * 5. SUB_PROJECTS              只有子项目。
     * 6. SUB_PROJECTS_LOCAL_DEPS   只有子项目的本地依赖项(本地jar)。
     * 7. TESTED_CODE               由当前变量(包括依赖项)测试的代码
     * @return
     */
    @Override
    Set<QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, IOException {
        Collection<TransformInput> inputs = transformInvocation.getInputs()
        TransformOutputProvider outputProvider = transformInvocation.getOutputProvider()
        boolean isIncremental = transformInvocation.isIncremental()
        //如果非增量，则清空旧的输出内容
        if (!isIncremental) {
            outputProvider.deleteAll()
        }
        logger.warn(getName() + " isIncremental = " + isIncremental)
        long startTime = System.currentTimeMillis()

        /**Transform 的 inputs 有两种类型，一种是目录，一种是 jar 包，要分开遍历 */
        inputs.each { TransformInput input ->
//            /**遍历 jar*/
//            input.jarInputs.each { JarInput jarInput ->
//                /**重命名输出文件（同目录copyFile会冲突）*/
//                String destName = jarInput.file.name
//
//                /**截取文件路径的 md5 值重命名输出文件,因为可能同名,会覆盖*/
//                def hexName = DigestUtils.md5Hex(jarInput.file.absolutePath).substring(0, 8)
//                /** 获取 jar 名字*/
//                if (destName.endsWith(".jar")) {
//                    destName = destName.substring(0, destName.length() - 4)
//                }
//
//                File copyJarFile = SensorsAnalyticsInject.injectJar(jarInput.file.absolutePath, project)
//                def dest = outputProvider.getContentLocation(destName + hexName, jarInput.contentTypes, jarInput.scopes, Format.JAR)
//                FileUtils.copyFile(copyJarFile, dest)
//
//                transformInvocation.getContext().getTemporaryDir().deleteDir()
//            }
            String jarsDir
            //对类型为jar文件的input进行遍历
            input.jarInputs.each { JarInput jarInput ->
                //jar文件一般是第三方依赖库jar文件 输出表明 还包括了自建的依赖lib库的jar文件
                // （也就是主项目build.gradle中 dependencies下 compile的东西）
                // println("jarInput.file.getAbsolutePath() === " + jarInput.file.getAbsolutePath())
                // 重命名输出文件（同目录copyFile会冲突）
                def jarName = jarInput.name
                def md5Name = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())
                if (jarName.endsWith(".jar")) {
                    jarName = jarName.substring(0, jarName.length() - 4)
                    // println("jarName substring is " + jarName)
                }
                //生成输出路径
                def dest = transformInvocation.outputProvider.getContentLocation(jarName + md5Name,
                        jarInput.contentTypes, jarInput.scopes, Format.JAR)

                //AppMethodTime\app\build\intermediates\transforms\MyTrans\debug\jars 拼凑这个目录
                jarsDir = dest.absolutePath.split("jars")[0] + "jars";
                // println("dest === " + dest.absolutePath)

                //将输入内容复制到输出
                FileUtils.copyFile(jarInput.file, dest)
            }

            /**遍历目录*/
            input.directoryInputs.each { DirectoryInput directoryInput ->
                logger.warn((getName() + " directoryInput.file.absolutePath " + directoryInput.file.absolutePath))
                SensorsAnalyticsInject.injectDir(logger,directoryInput.file.absolutePath, project)

                def dest = outputProvider.getContentLocation(directoryInput.name,
                        directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
                /**将input的目录复制到output指定目录*/
                FileUtils.copyDirectory(directoryInput.file, dest)
            }
        }
        long costTime = System.currentTimeMillis() - startTime;
        logger.warn((getName() + " costed " + costTime + "ms"));
    }

    boolean isWeavableClass(String fullQualifiedClassName) {
        return fullQualifiedClassName.endsWith(".class") && !fullQualifiedClassName.contains('R$') && !fullQualifiedClassName.contains("R.class") && !fullQualifiedClassName.contains("R2.class") && !fullQualifiedClassName.contains('R2$') && !fullQualifiedClassName.contains("BuildConfig.class")
    }
}