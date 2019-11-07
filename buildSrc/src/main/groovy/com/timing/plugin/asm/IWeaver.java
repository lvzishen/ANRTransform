package com.timing.plugin.asm;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by lvzishen on 11/07/2019
 */
public interface IWeaver {

    /**
     * 检查当前的类是否是可以被修改字节码格式的
     */
    public boolean isWeavableClass(String filePath) throws IOException;

    /**
     * 将一个Class以byte形式输出
     */
    public byte[] weaveSingleClassToByteArray(InputStream inputStream) throws IOException;


}

