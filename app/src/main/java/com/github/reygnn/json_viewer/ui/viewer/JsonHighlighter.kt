package com.github.reygnn.json_viewer.ui.viewer

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.github.reygnn.json_viewer.core.parser.JsonTokenizer
import com.github.reygnn.json_viewer.ui.theme.JsonTokenColors

/**
 * Renders one already-formatted JSON line into a coloured [AnnotatedString]
 * via the per-line [JsonTokenizer]. This is the Compose replacement for the
 * old `SpannableString` / `ForegroundColorSpan` path.
 *
 * Deliberately NOT `@Composable` and free of any Compose runtime, so it is
 * unit-testable on the plain JVM (see JsonHighlighterTest).
 *
 * Token end indices are clamped to the line length: a malformed, unterminated
 * string could otherwise yield an end past the line and crash `addStyle`.
 */
fun highlightJsonLine(
    line: String,
    colors: JsonTokenColors,
    defaultColor: Color,
    tokenizer: JsonTokenizer,
): AnnotatedString = buildAnnotatedString {
    append(line)
    if (line.isNotEmpty()) {
        addStyle(SpanStyle(color = defaultColor), 0, line.length)
    }
    tokenizer.tokenize(line).forEach { token ->
        val end = token.end.coerceAtMost(line.length)
        if (token.start in 0 until end) {
            addStyle(SpanStyle(color = colors.forType(token.type)), token.start, end)
        }
    }
}
