package edu.seu.vcampus.server.ai.service;

import edu.seu.vcampus.server.ai.model.AiTextSink;

/** 在不破坏流式输出的前提下移除模型常见的 Markdown 和数字引用噪声。 */
final class AiPlainTextFilter implements AiTextSink {
    private final AiTextSink target;
    private final StringBuilder citation = new StringBuilder();
    private boolean pendingBackslash;

    AiPlainTextFilter(AiTextSink target) {
        this.target = target;
    }

    public void onText(String text) {
        if (text == null || text.isEmpty()) return;
        StringBuilder clean = new StringBuilder(text.length());
        for (int index = 0; index < text.length(); index++) {
            accept(text.charAt(index), clean);
        }
        emit(clean);
    }

    void finish() {
        StringBuilder clean = new StringBuilder();
        flushCitation(clean);
        if (pendingBackslash) {
            clean.append('\\'); pendingBackslash = false;
        }
        emit(clean);
    }

    private void accept(char value, StringBuilder clean) {
        if (citation.length() > 0) {
            if (Character.isDigit(value)) {
                citation.append(value); return;
            }
            if (value == ']' && citation.length() > 1) {
                citation.setLength(0); return;
            }
            flushCitation(clean);
        }
        if (pendingBackslash) {
            pendingBackslash = false;
            if (value == '[') { citation.append(value); return; }
            if (value == '*' || value == '`' || value == '#') return;
            if (value == '-' || value == '.' || value == ']') {
                clean.append(value); return;
            }
            clean.append('\\');
        }
        if (value == '\\') { pendingBackslash = true; return; }
        if (value == '[') { citation.append(value); return; }
        if (value == '*' || value == '`' || value == '□'
                || value == '☐' || value == '☑') return;
        clean.append(value);
    }

    private void flushCitation(StringBuilder clean) {
        if (citation.length() > 0) {
            clean.append(citation); citation.setLength(0);
        }
    }

    private void emit(StringBuilder clean) {
        if (clean.length() > 0) target.onText(clean.toString());
    }
}
