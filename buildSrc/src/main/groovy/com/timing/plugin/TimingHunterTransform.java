package com.timing.plugin;

import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Status;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.api.transform.TransformOutputProvider;
import com.android.build.gradle.internal.pipeline.TransformManager;
import com.android.ide.common.internal.WaitableExecutor;
import com.timing.plugin.asm.BaseWeaver;
import com.timing.plugin.asm.TimingWeaver;


import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 创建日期：2019/10/31 on 9:48
 * 描述:
 * 作者: lvzishen
 */
public class TimingHunterTransform extends Transform {
    private ThreadPoolExecutor executor;
    private final Logger logger;
    protected BaseWeaver bytecodeWeaver;
    private final Object lock = new Object();
    private int allRunnableSize;
    private int curRunnableSize;

    public TimingHunterTransform(Project project) {
        this.logger = project.getLogger();
        this.executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
        this.bytecodeWeaver = new TimingWeaver();
    }

    /**
     * Transform 标识名
     * 比如我在 app module 下依赖了这个 Plugin
     * 那么在 app/build/intermediates/transforms/
     * 下，就能看到我们的自定义 DemoTransform
     */
    @Override
    public String getName() {
        return "TimingHunterTransform";
    }

    /**
     * 设置文件输入类型
     * 类型在 TransformManager 下有定义
     * 这里我们获取 class 文件类型
     */
    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }

    /**
     * 设置文件所属域
     * 同样在 TransformManager 下有定义
     * 这里指定为当前工程
     */
    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT;
    }

    /**
     * 是否支持增量编译
     */
    @Override
    public boolean isIncremental() {
        return true;
    }


    @Override
    public void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        Collection<TransformInput> inputs = transformInvocation.getInputs();
        TransformOutputProvider outputProvider = transformInvocation.getOutputProvider();

        boolean isIncremental = transformInvocation.isIncremental();
        //如果非增量，则清空旧的输出内容
        if (!isIncremental) {
            outputProvider.deleteAll();
        }
        logger.warn(getName() + " isIncremental = " + isIncremental);
        long startTime = System.currentTimeMillis();
        for (TransformInput input : inputs) {
            for (JarInput jarInput : input.getJarInputs()) {
                Status status = jarInput.getStatus();
                File dest = outputProvider.getContentLocation(
                        jarInput.getName(),
                        jarInput.getContentTypes(),
                        jarInput.getScopes(),
                        Format.JAR);
                if (isIncremental) {
                    switch (status) {
                        case NOTCHANGED:
                            break;
                        case ADDED:
                        case CHANGED:
                            transformJar(jarInput.getFile(), dest);
                            break;
                        case REMOVED:
                            if (dest.exists()) {
                                FileUtils.forceDelete(dest);
                            }
                            break;
                    }
                } else {
                    transformJar(jarInput.getFile(), dest);
                }
            }
            for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                File dest = outputProvider.getContentLocation(directoryInput.getName(),
                        directoryInput.getContentTypes(), directoryInput.getScopes(),
                        Format.DIRECTORY);
                FileUtils.forceMkdir(dest);
                if (isIncremental) {
                    String srcDirPath = directoryInput.getFile().getAbsolutePath();
                    String destDirPath = dest.getAbsolutePath();
                    Map<File, Status> fileStatusMap = directoryInput.getChangedFiles();
                    for (Map.Entry<File, Status> changedFile : fileStatusMap.entrySet()) {
                        Status status = changedFile.getValue();
                        File inputFile = changedFile.getKey();
                        String destFilePath = inputFile.getAbsolutePath().replace(srcDirPath, destDirPath);
                        File destFile = new File(destFilePath);
                        switch (status) {
                            case NOTCHANGED:
                                break;
                            case REMOVED:
                                if (destFile.exists()) {
                                    FileUtils.forceDelete(destFile);
                                }
                                break;
                            case ADDED:
                            case CHANGED:
                                FileUtils.touch(destFile);
                                transformSingleFile(inputFile, destFile, srcDirPath);
                                break;
                        }
                    }
                } else {
                    transformDir(directoryInput.getFile(), dest);
                }
            }
        }
        //等待所有任务完成
        synchronized (lock) {
            while (curRunnableSize != allRunnableSize) {
                lock.wait();
            }
        }
//        executor.waitForTasksWithQuickFail(true);
        long costTime = System.currentTimeMillis() - startTime;
        logger.warn((getName() + " costed " + costTime + "ms"));
    }

    private void transformSingleFile(final File inputFile, final File outputFile, final String srcBaseDir) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                synchronized (lock) {
                    allRunnableSize++;
                }
                try {
                    bytecodeWeaver.weaveSingleClassToFile(inputFile, outputFile, srcBaseDir);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    synchronized (lock) {
                        curRunnableSize++;
                        lock.notify();
                    }
                }
            }
        });
    }

    private void transformJar(final File srcJar, final File destJar) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
//                if (emptyRun) {
//                org.apache.commons.io.FileUtils.copyFile(srcJar, destJar);
//                return null;
//            }
                synchronized (lock) {
                    allRunnableSize++;
                }
                try {
                    bytecodeWeaver.weaveJar(srcJar, destJar);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    synchronized (lock) {
                        curRunnableSize++;
                        lock.notify();
                    }
                }
            }
        });
    }

    private void transformDir(final File inputDir, final File outputDir) throws IOException {
//        //Debug模式下不处理
//        if(emptyRun) {
//            org.apache.commons.io.FileUtils.copyDirectory(inputDir, outputDir);
//            return;
//        }
        final String inputDirPath = inputDir.getAbsolutePath();
        final String outputDirPath = outputDir.getAbsolutePath();
        if (inputDir.isDirectory()) {
            for (final File inputFile : com.android.utils.FileUtils.getAllFiles(inputDir)) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (lock) {
                            allRunnableSize++;
                        }
                        String filePath = inputFile.getAbsolutePath();
                        File outputFile = new File(filePath.replace(inputDirPath, outputDirPath));
                        try {
                            bytecodeWeaver.weaveSingleClassToFile(inputFile, outputFile, inputDirPath);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            synchronized (lock) {
                                curRunnableSize++;
                                lock.notify();
                            }
                        }
                    }
                });
            }
        }
    }
}
