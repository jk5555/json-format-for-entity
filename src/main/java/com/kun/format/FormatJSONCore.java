package com.kun.format;

import com.google.gson.GsonBuilder;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.PsiUtil;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * 选定当前java文件，解析文件结构，获取到所有可能的字段类型，赋值到map中，赋予初始值，然后转为json
 * 1，当光标在类外时，提示正确的操作 ok
 * 2，当类的引用类型的字段过多，过深时，递归会堆栈溢出，故决定增加10层的限制，并给与提示 ok
 * 3，转化的json不直接复制到剪贴板，而是给予弹窗预览，并提供复制选项 ok
 *
 * @author kun
 */
public class FormatJSONCore extends AnAction {


    @Override
    public void actionPerformed(AnActionEvent e) {
        //获取当前项目名称
//        Project project = e.getData(PlatformDataKeys.PROJECT);
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        //获取当前操作的类文件
        PsiFile psiFile = e.getData(PlatformDataKeys.PSI_FILE);
        assert psiFile != null;
        String fileTypeName = psiFile.getFileType().getDefaultExtension();
        String fileName = psiFile.getName().split("\\.")[0];
        if (!fileTypeName.endsWith("java")) {
            Messages.showErrorDialog(project, "不支持此类型文件", "文件错误");
            return;
        }

        int offset = psiFile.getText().indexOf(fileName);
        PsiElement classElement = psiFile.findElementAt(offset);
        PsiClass psiClass = PsiTreeUtil.getContextOfType(classElement, PsiClass.class);
        if (Objects.isNull(psiClass)) {
            Messages.showErrorDialog(project, "解析失败", "提示");
            return;
        } else if (psiClass.isAnnotationType() || psiClass.isEnum() || psiClass.isInterface()) {
            Messages.showErrorDialog(project, "调皮！！！", "提示");
            return;
        }

        Map<String, Object> map = getFields(psiClass);
        String json = new GsonBuilder().setPrettyPrinting().create().toJson(map);

        String[] options = {"取消", "复制"};
        int indexOptions = Messages.showDialog(json, "解析预览", options, 1, Messages.getInformationIcon());
        if (indexOptions == 1) {
            Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
            Clipboard systemClipboard = defaultToolkit.getSystemClipboard();
            StringSelection selection = new StringSelection(json);
            systemClipboard.setContents(selection, selection);
        }

    }

    private Map<String, Object> getFields(PsiClass psiClass) {

        Map<String, Object> map = new LinkedHashMap<>();

        PsiField[] fields = psiClass.getAllFields();
        if (fields.length == 0) {
            return map;
        }

        for (PsiField field : fields) {
            String fieldName = field.getName();
            PsiType fieldType = field.getType();

            if (isUpperCase(fieldName)) {
                continue;
            }

            //基本类型
            if (fieldType instanceof PsiPrimitiveType) {
                map.put(fieldName, PsiTypesUtil.getDefaultValue(fieldType));
                //引用类型
            } else {
                //获取字段类型文本
                String presentableText = fieldType.getPresentableText();
                //基本引用类型
                if (FieldType.isNormalType(presentableText)) {
                    map.put(fieldName, FieldType.getDefaultValue(presentableText));
                    //数组类型
                } else if (fieldType instanceof PsiArrayType) {
                    //数组的类型
                    PsiType deepComponentType = fieldType.getDeepComponentType();
                    String deepComponentTypePresentableText = deepComponentType.getPresentableText();
                    List<Object> list = new ArrayList<>();
                    if (deepComponentType instanceof PsiPrimitiveType) {
                        list.add(PsiTypesUtil.getDefaultValue(deepComponentType));
                    } else if (FieldType.isNormalType(deepComponentTypePresentableText)) {
                        list.add(FieldType.getDefaultValue(presentableText));
                    } else {
                        list.add(getFields(Objects.requireNonNull(PsiUtil.resolveClassInType(deepComponentType))));
                    }
                    map.put(fieldName, list);
                    //枚举
                } else if (Objects.requireNonNull(PsiUtil.resolveClassInType(fieldType)).isEnum()) {
                    PsiClass typeClass = Objects.requireNonNull(PsiUtil.resolveClassInType(fieldType));
                    List<Object> list = new ArrayList<>();
                    for (PsiField psiField : typeClass.getAllFields()) {
                        if (psiField instanceof PsiEnumConstant) {
                            list.add(psiField.getName());
                        }
                    }
                    map.put(fieldName, list);
                    //List类型
                } else if (FieldType.getDeepStartType(fieldType.getPresentableText()).endsWith("List") || FieldType.getDeepStartType(fieldType.getPresentableText()).endsWith("Set")) {
                    PsiType iterableType = PsiUtil.extractIterableTypeParameter(fieldType, false);
                    PsiClass typeClass = PsiUtil.resolveClassInClassTypeOnly(iterableType);
                    if (typeClass != null) {
                        List<Object> list = new ArrayList<>();
                        if (FieldType.isNormalType(typeClass.getName())) {
                            list.add(FieldType.getDefaultValue(typeClass.getName()));
                        } else {
                            list.add(getFields(typeClass));
                        }
                        map.put(fieldName, list);
                    }
                } else if (FieldType.getDeepStartType(fieldType.getPresentableText()).endsWith("Map")) {
                    map.put(fieldName, new HashMap<>(1));
                } else {
                    map.put(fieldName, getFields(Objects.requireNonNull(PsiUtil.resolveClassInType(fieldType))));
                }
            }


        }
        return map;

    }

    private boolean isUpperCase(String fieldName) {
        for (char c : fieldName.toCharArray()) {
            if (Character.isLowerCase(c)) {
                return false;
            }
        }
        return true;
    }


    @Override
    public void update(AnActionEvent e) {

        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        FileType fileType = psiFile.getFileType();
        boolean b = (fileType != null && "JAVA".equals(fileType.getName()));
        e.getPresentation().setEnabled(editor != null && psiFile != null && psiFile.isValid() && b);
    }
}
