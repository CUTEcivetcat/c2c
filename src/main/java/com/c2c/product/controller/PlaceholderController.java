package com.c2c.product.controller;

import com.c2c.common.constant.ApiPath;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品占位图端点：把文字（通常是商品名）渲染成 SVG 图片。
 *
 * <p>不上传图片的商品也有带标题的封面，避免列表出现空白/破图。
 * 自托管而非使用第三方占位图网站（placehold 等）：无外网依赖、无小程序合法域名问题、
 * 商品标题不泄露给第三方。</p>
 */
@RestController
@Tag(name = "占位图", description = "按文字生成 SVG 占位图（商品无图时的默认封面）")
public class PlaceholderController {

    private static final int W_DEFAULT = 600;
    private static final int H_DEFAULT = 400;

    @Operation(summary = "生成 SVG 占位图", description = "text 为图上文字（一般是商品名），w/h 限制 100~1200")
    @GetMapping(value = ApiPath.PLACEHOLDER, produces = "image/svg+xml;charset=UTF-8")
    public String placeholder(@Parameter(description = "图上文字") @RequestParam(defaultValue = "青禾集市") String text,
                              @Parameter(description = "宽度") @RequestParam(defaultValue = "600") int w,
                              @Parameter(description = "高度") @RequestParam(defaultValue = "400") int h) {
        w = clamp(w, 100, 1200, W_DEFAULT);
        h = clamp(h, 100, 1200, H_DEFAULT);
        String safe = escape(text.trim().isEmpty() ? "青禾集市" : text.trim());

        // 长标题按每行 8 字折行，最多 3 行
        String[] lines = wrap(safe, 8, 3);
        int fontSize = Math.min(48, Math.max(22, w / (maxLineLen(lines) + 2)));
        int lineHeight = (int) (fontSize * 1.35);
        int startY = h / 2 - (lines.length - 1) * lineHeight / 2;

        StringBuilder tspans = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            tspans.append(String.format(
                    "<tspan x=\"50%%\" y=\"%d\" text-anchor=\"middle\">%s</tspan>",
                    startY + i * lineHeight, lines[i]));
        }

        return String.format(
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 %d %d\">"
                        + "<defs><linearGradient id=\"g\" x1=\"0\" y1=\"0\" x2=\"1\" y2=\"1\">"
                        + "<stop offset=\"0\" stop-color=\"#fff4ec\"/><stop offset=\"1\" stop-color=\"#ffe3d1\"/>"
                        + "</linearGradient></defs>"
                        + "<rect width=\"100%%\" height=\"100%%\" fill=\"url(#g)\"/>"
                        + "<text font-family=\"'PingFang SC','Microsoft YaHei',sans-serif\" font-size=\"%d\" "
                        + "font-weight=\"600\" fill=\"#ff6b35\">%s</text>"
                        + "<text x=\"50%%\" y=\"%d\" text-anchor=\"middle\" font-size=\"%d\" fill=\"#c0c8d2\" "
                        + "font-family=\"'PingFang SC','Microsoft YaHei',sans-serif\">青禾集市 · 二手好物</text>"
                        + "</svg>",
                w, h, w, h, fontSize, tspans.toString(), h - 24, Math.max(12, fontSize / 3));
    }

    private int clamp(int value, int min, int max, int def) {
        return value < min || value > max ? def : value;
    }

    /** 按每行 n 字折行，最多 maxLines 行，超出省略 */
    private String[] wrap(String text, int perLine, int maxLines) {
        int len = text.length();
        int count = Math.min(maxLines, (len + perLine - 1) / perLine);
        String[] lines = new String[count];
        for (int i = 0; i < count; i++) {
            int end = Math.min(len, (i + 1) * perLine);
            lines[i] = i == maxLines - 1 && end < len
                    ? text.substring(i * perLine, Math.max(i * perLine, end - 1)) + "…"
                    : text.substring(i * perLine, end);
        }
        return lines;
    }

    private int maxLineLen(String[] lines) {
        int max = 1;
        for (String line : lines) {
            max = Math.max(max, line.length());
        }
        return max;
    }

    /** XML 转义，防止标题中的特殊字符破坏 SVG */
    private String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
