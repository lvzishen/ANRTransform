package com.timing.plugin.asm;


import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

/**
 * Created by lvzishen on 11/07/2019.
 */
public final class TimingWeaver extends BaseWeaver {

    private static final String PLUGIN_LIBRARY = "com.time.timeplugin.block";


    @Override
    public boolean isWeavableClass(String fullQualifiedClassName) {
        boolean superResult = super.isWeavableClass(fullQualifiedClassName);
//        return superResult;
        //对class进行筛选
        boolean isByteCodePlugin = fullQualifiedClassName.startsWith(PLUGIN_LIBRARY);
//        if(timingHunterExtension != null) {
//            //whitelist is prior to to blacklist
//            if(!timingHunterExtension.whitelist.isEmpty()) {
//                boolean inWhiteList = false;
//                for(String item : timingHunterExtension.whitelist) {
//                    if(fullQualifiedClassName.startsWith(item)) {
//                        inWhiteList = true;
//                    }
//                }
//                return superResult && !isByteCodePlugin && inWhiteList;
//            }
//            if(!timingHunterExtension.blacklist.isEmpty()) {
//                boolean inBlackList = false;
//                for(String item : timingHunterExtension.blacklist) {
//                    if(fullQualifiedClassName.startsWith(item)) {
//                        inBlackList = true;
//                    }
//                }
//                return superResult && !isByteCodePlugin && !inBlackList;
//            }
//        }
        return superResult && !isByteCodePlugin;
    }

    @Override
    protected ClassVisitor wrapClassWriter(ClassWriter classWriter) {
        return new TimingClassAdapter(classWriter);
    }

}
