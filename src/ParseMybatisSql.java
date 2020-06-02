import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.JBColor;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.util.TextUtils;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ParseMybatisSql extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        final Editor mEditor = e.getData(PlatformDataKeys.EDITOR);
        if (null == mEditor) {
            return;
        }
        SelectionModel model = mEditor.getSelectionModel();
        final String selectedText = model.getSelectedText();
        if (TextUtils.isEmpty(selectedText)) {
            return;
        }
        String result = parseSql(selectedText);
        if (!TextUtils.isBlank(result)) {
            ApplicationManager.getApplication().invokeLater(() -> {
                JBPopupFactory factory = JBPopupFactory.getInstance();
                factory.createHtmlTextBalloonBuilder(StringEscapeUtils.escapeHtml(result), null, new JBColor(new Color(78, 29, 76), new Color(119, 52, 96)), null)
                        .setFadeoutTime(10000)
                        .createBalloon()
                        .show(factory.guessBestPopupLocation(mEditor), Balloon.Position.below);
            });
        } else {
            Messages.showInfoMessage("can't parse by now select text", "ParseSql");
        }
    }

    private String parseSql(String selectedText) {
        try {
            Pattern p = Pattern.compile("\\n");
            String s = p.matcher(selectedText).replaceAll(" ");
            Pattern p1 = Pattern.compile("\\d{4}-\\d{2}-\\d{2}\\W\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\W\\[[\\w|\\-]+]\\WDEBUG\\W");
            String s1 = p1.matcher(s).replaceAll("\n");
            String[] split = s1.split("\\n");
            List<String> collect = Arrays.stream(split)
                    .filter(Objects::nonNull)
                    .filter(ParseMybatisSql::nonEmpty)
                    .map(ParseMybatisSql::formatStr)
                    .collect(Collectors.toList());
            String s2 = collect.get(0);
            List<String> sqls = Arrays.stream(s2.split("\\?"))
                    .filter(Objects::nonNull)
                    .filter(ParseMybatisSql::nonEmpty)
                    .collect(Collectors.toList());
            String s3 = collect.get(1);
            List<String> strings = splitParam(s3);
            assert strings != null;
            if (sqls.size() - strings.size() == 1) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < strings.size(); i++) {
                    String s4 = strings.get(i);
                    String type = getType(s4);
                    if (type.equalsIgnoreCase("(integer)")) {
                        sb.append(sqls.get(i))
                                .append(" ")
                                .append(getValue(s4))
                                .append(" ");
                    } else {
                        sb.append(sqls.get(i)).append(" '").append(getValue(s4)).append("' ");
                    }
                }
                sb.append(sqls.get(sqls.size() - 1));
                return sb.toString();
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public static String getType(String input) {
        return input.substring(input.lastIndexOf("("), input.lastIndexOf(")") + 1);
    }

    public static String getValue(String input) {
        return input.substring(0, input.lastIndexOf("("));
    }

    public static String formatStr(String input) {
        Pattern compile = Pattern.compile("^.+[Preparing|Parameters]:\\W");
        return compile.matcher(input).replaceAll(" ");
    }

    public static List<String> splitParam(String input) {
        String[] split = input.split(",");
        if (split.length > 0) {
            return Arrays.stream(split)
                    .filter(Objects::nonNull)
                    .map(s -> s.substring(1))
                    .filter(ParseMybatisSql::nonEmpty)
                    .collect(Collectors.toList());
        }
        return null;
    }

    public static boolean nonEmpty(String input) {
        return !input.isEmpty();
    }

    public static String replaceParam(String input) {
        return input.replaceAll("\\(\\w+\\)", "");
    }
}
