package com.kun.format;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiFile;


/**
 * @author kun
 */
public class FormatJSONCore extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        // TODO: insert action logic here
        //获取当前项目名称
//        Project project = e.getData(PlatformDataKeys.PROJECT);
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        //获取当前操作的类文件
        PsiFile psiFile = e.getData(PlatformDataKeys.PSI_FILE);
        //获取当前操作的类文件的路径
        assert psiFile != null;
        String fileTypeName = psiFile.getFileType().getDefaultExtension();
        if (!fileTypeName.endsWith("java") ) {
            Messages.showErrorDialog(project, "不支持此类型文件", "文件错误");
        }

        String path = psiFile.getVirtualFile().getPath();
        int begin = path.indexOf("/java/");
        String subFileName = path.substring( begin == -1 ? 0 : begin + 6, path.indexOf(".java"));
        String replace = subFileName.replace("\\", ".");
        String fullClassName = replace.replace("/", ".");
        String basePath = project.getBasePath();
        String mavenConn = "/target/classes/";
        String gradleConn = "/build/classes/java/main/";
        String fullClassPath = basePath + mavenConn + subFileName + ".class";

        LocalClassLoader classLoader = null;
        try {
            classLoader = new LocalClassLoader(fullClassPath);
        } catch (Exception ee) {
            ee.printStackTrace();
            try {
                classLoader = new LocalClassLoader(basePath + gradleConn + subFileName + ".class");
            } catch (Exception exception) {
                exception.printStackTrace();
                Messages.showErrorDialog(project, "不支持当前的项目目录结构，建议使用标准的maven或者gradle项目目录结构。", "class加载错误");
                return;
            }
        }
        String istr = "format fail: only support basic type entity now.";
        try {
            Class<?> aClass = classLoader.loadClass(fullClassName);
            Object instance = aClass.newInstance();
            istr = JSON.toJSONString(instance, SerializerFeature.WriteMapNullValue);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }


//        Class.forName(path)
        String title = "解析结果";
        Messages.showMessageDialog(project, istr, title, Messages.getInformationIcon());
    }


    @Override
    public void update(AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        FileType fileType = psiFile.getFileType();
        boolean b = (fileType!=null && "JAVA".equals(fileType.getName()));
        e.getPresentation().setEnabled(editor != null && psiFile != null && psiFile.isValid() && b);
    }
}
