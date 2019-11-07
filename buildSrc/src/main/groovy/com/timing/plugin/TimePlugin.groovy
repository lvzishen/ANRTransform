package com.timing.plugin

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class TimePlugin implements Plugin<Project> {
    void apply(Project project) {
        //自定义Extension
        def extension = project.extensions.create("timingExtension", TimingHunterExtension);
        project.afterEvaluate {
            println extension.isDebug
        }
////        TimingHunterExtension timingHunterExtension = (TimingHunterExtension) project.getExtensions().getByName("timingExt");
////        timingHunterExtension.isDebug;
//
//        拿到gradle中android的Extension，将Transform注册进去
//        def transform = new TimingHunterTransform(project)
//        def baseExtension = project.extensions.getByType(BaseExtension.class)
//        baseExtension.registerTransform(transform)
        def android = project.extensions.getByType(AppExtension.class);
        android.registerTransform(new TimingHunterTransform(project))
    }
}