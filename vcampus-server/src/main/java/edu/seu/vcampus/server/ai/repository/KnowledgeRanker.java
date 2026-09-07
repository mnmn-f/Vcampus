package edu.seu.vcampus.server.ai.repository;

import edu.seu.vcampus.common.ai.AiKnowledgeChunk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** 对知识候选执行关键词分值与轻量文本向量余弦分值的混合排序。 */
final class KnowledgeRanker {
    List<AiKnowledgeChunk> rank(String query, List<AiKnowledgeChunk> candidates, int topK) {
        final String expanded = expand(normalize(query));
        final Set<String> queryTerms = terms(expanded);
        final Map<String, Integer> queryVector = vector(expanded);
        List<Scored> scored = new ArrayList<Scored>();
        if (queryTerms.isEmpty() || candidates == null) return new ArrayList<AiKnowledgeChunk>();
        for (AiKnowledgeChunk chunk : candidates) {
            String title = normalize(chunk.getTitle());
            String content = normalize(chunk.getContent());
            if (!hasAnchor(queryTerms, title + " " + content)) continue;
            double score = keywordScore(title, content, queryTerms)
                    + cosine(queryVector, vector(title + " " + content)) * 8.0d;
            if (!normalize(query).isEmpty() && content.contains(normalize(query))) score += 12.0d;
            if (score > 0.0d) scored.add(new Scored(chunk, score));
        }
        Collections.sort(scored, new Comparator<Scored>() {
            public int compare(Scored left, Scored right) {
                int byScore = Double.compare(right.score, left.score);
                if (byScore != 0) return byScore;
                if (left.chunk.getUpdatedAt() == right.chunk.getUpdatedAt()) return 0;
                return left.chunk.getUpdatedAt() < right.chunk.getUpdatedAt() ? 1 : -1;
            }
        });
        List<AiKnowledgeChunk> out = new ArrayList<AiKnowledgeChunk>();
        int limit = Math.min(Math.max(1, topK), scored.size());
        for (int i = 0; i < limit; i++) out.add(scored.get(i).chunk);
        return out;
    }

    private double keywordScore(String title, String content, Set<String> queryTerms) {
        double score = 0.0d;
        for (String term : queryTerms) {
            if (title.contains(term)) score += 5.0d;
            if (content.contains(term)) score += 2.0d;
        }
        return score;
    }

    private boolean hasAnchor(Set<String> queryTerms, String candidate) {
        for (String term : queryTerms) {
            if (term.length() >= 2 && candidate.contains(term)) return true;
        }
        return false;
    }

    private double cosine(Map<String, Integer> left, Map<String, Integer> right) {
        if (left.isEmpty() || right.isEmpty()) return 0.0d;
        double dot = 0.0d; double leftNorm = 0.0d; double rightNorm = 0.0d;
        for (Map.Entry<String, Integer> item : left.entrySet()) {
            int value = item.getValue().intValue(); leftNorm += value * value;
            Integer other = right.get(item.getKey());
            if (other != null) dot += value * other.intValue();
        }
        for (Integer value : right.values()) rightNorm += value.intValue() * value.intValue();
        return dot == 0.0d ? 0.0d : dot / Math.sqrt(leftNorm * rightNorm);
    }

    private Map<String, Integer> vector(String value) {
        Map<String, Integer> out = new HashMap<String, Integer>();
        for (String term : terms(value)) {
            Integer count = out.get(term);
            out.put(term, Integer.valueOf(count == null ? 1 : count.intValue() + 1));
        }
        return out;
    }

    private Set<String> terms(String value) {
        Set<String> out = new LinkedHashSet<String>();
        String[] words = normalize(value).split("[^\\p{L}\\p{N}]+");
        for (String word : words) {
            if (word.length() >= 2 && ascii(word)) out.add(word);
            if (!ascii(word)) {
                for (int i = 0; i < word.length(); i++) {
                    out.add(word.substring(i, i + 1));
                    if (i + 1 < word.length()) out.add(word.substring(i, i + 2));
                }
            }
        }
        return out;
    }

    private String expand(String value) {
        StringBuilder out = new StringBuilder(value);
        if (contains(value, "添加课程", "加入课表", "选课")) {
            out.append(" 选课中心 课程 课表 操作步骤");
        }
        if (contains(value, "排课", "添加时段", "课程时段")) {
            out.append(" 课程与排课 新建时段 保存时段");
        }
        if (contains(value, "校纪", "校规", "纪律", "规定")) {
            out.append(" 校纪校规 校园规定 纪律 规范");
        }
        if (contains(value, "处分", "作弊", "打架", "申诉", "违纪")) {
            out.append(" 学生违纪 处分 条例 调查 告知 申辩 申诉");
        }
        if (contains(value, "电器", "电池", "充电", "消防", "大功率")) {
            out.append(" 公寓 宿舍 用电 安全 禁用电器 800瓦 电池充电 消防");
        }
        if (contains(value, "访客", "晚归", "夜不归宿", "门禁")) {
            out.append(" 学生公寓 访客 会客 门禁 请假 秩序");
        }
        if (contains(value, "借书", "借阅", "图书")) out.append(" 图书馆 借阅 归还");
        if (contains(value, "宿舍", "水电", "报修", "请假")) out.append(" 宿舍 住宿 水电 报修 请假");
        if (contains(value, "商店", "商品", "购物", "买过", "订单", "付款")) out.append(" 商店 商品 购物车 订单 支付 优惠券");
        if (contains(value, "自习室", "研讨室")) out.append(" 图书馆 自习室 预约 时段");
        if (contains(value, "教室", "场地")) out.append(" 校园 教室 申请 预约 时段");
        return out.toString();
    }

    private boolean contains(String value, String... terms) {
        for (String term : terms) if (value.contains(term)) return true;
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    private boolean ascii(String value) {
        for (int i = 0; i < value.length(); i++) if (value.charAt(i) > 127) return false;
        return true;
    }

    private static final class Scored {
        private final AiKnowledgeChunk chunk;
        private final double score;
        private Scored(AiKnowledgeChunk chunk, double score) {
            this.chunk = chunk; this.score = score;
        }
    }
}
