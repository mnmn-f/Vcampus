package edu.seu.vcampus.server.ai.tool;

import edu.seu.vcampus.common.ai.AiMode;
import edu.seu.vcampus.server.ai.model.AiModel;
import edu.seu.vcampus.server.ai.model.AiTextSink;

import java.time.LocalDateTime;
import java.util.Map;

/** 用生成模型补充固定规则未覆盖的自然语言表达，输出仍受工具白名单约束。 */
public final class ModelToolIntentResolver {
    private final AiModel model;
    private final AiToolRegistry tools;

    public ModelToolIntentResolver(AiModel model, AiToolRegistry tools) {
        this.model = model; this.tools = tools;
    }

    public AiToolInvocation resolve(String requestId, String text, String history, AiMode mode) {
        if (!model.isConfigured() || mode == AiMode.CHAT) return null;
        final StringBuilder response = new StringBuilder();
        try {
            model.generate(requestId + "-intent", prompt(text, history, mode), new AiTextSink() {
                public void onText(String value) { response.append(value); }
            });
            return parse(response.toString(), mode);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt(); return null;
        } catch (Exception ex) { return null; }
    }

    private String prompt(String text, String history, AiMode mode) {
        StringBuilder out = new StringBuilder();
        out.append("你是校园系统意图路由器，只判断是否需要调用一个实时业务工具。\n")
                .append("当前时间：").append(LocalDateTime.now()).append("。\n")
                .append("当前模式：").append(mode.name()).append("。QA 只能选只读工具，TASK 可选全部工具。\n")
                .append("如果只是知识解释、操作方法、闲聊或无法确定，输出 NONE。\n")
                .append("若命中，严格只输出一行：TOOL<TAB>工具名<TAB>参数JSON<TAB>中文摘要。")
                .append("用户明确要求预约、报名、添加、借还、支付等动作时，必须优先选择对应写工具，")
                .append("不能因为缺少参数而改成查询工具。即使缺少部分参数，只要业务意图明确也要")
                .append("选择写工具并输出已知参数，系统会继续澄清。结合最近对话补全指代和已提供参数。")
                .append("名称优先放 name，时间使用 yyyy-MM-ddTHH:mm。不得猜测编号。\n")
                .append("工具白名单：\n");
        for (Map.Entry<String, AiTool> entry : tools.all().entrySet()) {
            AiTool tool = entry.getValue();
            if (mode == AiMode.QA && tool.isWriteOperation()) continue;
            out.append(tool.getName()).append("：").append(tool.getDescription())
                    .append(tool.isWriteOperation() ? "（写操作）" : "（只读）")
                    .append("；参数：").append(tool.getParameterGuide()).append('\n');
        }
        if (history != null && !history.isEmpty()) out.append("最近对话：\n").append(history).append('\n');
        return out.append("当前用户话语：").append(text == null ? "" : text).toString();
    }

    private AiToolInvocation parse(String value, AiMode mode) {
        if (value == null) return null;
        String line = value.trim();
        int start = line.indexOf("TOOL\t");
        if (start < 0) return null;
        String[] parts = line.substring(start).split("\\t", 4);
        if (parts.length < 4) return null;
        AiTool tool = tools.get(parts[1].trim());
        if (tool == null || (mode == AiMode.QA && tool.isWriteOperation())) return null;
        String json = parts[2].trim();
        if (!safeArguments(json)) return null;
        String summary = parts[3].replace('\n', ' ').replace('\r', ' ').trim();
        return new AiToolInvocation(tool.getName(), json,
                summary.isEmpty() ? tool.getDescription() : summary);
    }

    private boolean safeArguments(String json) {
        if (json == null || json.length() > 500 || !json.startsWith("{") || !json.endsWith("}")) {
            return false;
        }
        String keys = json.replaceAll("\\\"(id|name|keyword|quantity|roomId|startAt|endAt|category|description|priority|leaveType|reason|code|classroomId|purpose|orderId|allocationId)\\\"\\s*:", "")
                .replaceAll("\\\"(?:\\\\.|[^\\\"])*\\\"", "")
                .replaceAll("[-0-9{}:,\\s]", "");
        return keys.isEmpty();
    }
}
