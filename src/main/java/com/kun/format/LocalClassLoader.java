package com.kun.format;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * 从本地加载class文件
 */
public class LocalClassLoader extends ClassLoader {

    private File classFile;

    public LocalClassLoader (String classPath) throws Exception {
        File classFile = new File(classPath);
        if (!classFile.exists() || !classFile.getName().endsWith(".class")) {
            throw new Exception("[" + classPath + "] file not exists. ");
        }
        this.classFile = classFile;
    }

    /**
     * 加载自定义类文件
     * @return
     */
    @Override
    public Class<?> findClass(String fullClassName) throws ClassNotFoundException {

        try {
            FileInputStream is = new FileInputStream(classFile);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int len;
            try {
                while ((len = is.read()) != -1) {
                    bos.write(len);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            byte[] data = bos.toByteArray();
            is.close();
            bos.close();
            return defineClass(fullClassName, data, 0, data.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return super.findClass(fullClassName);
    }


}

