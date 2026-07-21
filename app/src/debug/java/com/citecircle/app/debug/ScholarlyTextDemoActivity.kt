package com.citecircle.app.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.citecircle.app.core.designsystem.BrightScholarTheme
import com.citecircle.app.core.designsystem.CcCard
import com.citecircle.app.core.designsystem.ScholarlyText

/**
 * Debug-only harness for ScholarlyText. Not present in release builds.
 *
 * Exists because LaTeX rendering runs through a WebView and KaTeX assets, which
 * unit tests cannot exercise — the parser is testable, the rendering is not.
 *
 * adb shell am start -n com.citecircle.app.debug/com.citecircle.app.debug.ScholarlyTextDemoActivity
 */
class ScholarlyTextDemoActivity : ComponentActivity() {

    private val samples = listOf(
        "Plain prose with no math at all — this must never create a WebView.",
        "Inline math: the relation \$E = mc^2\$ underpins the derivation.",
        "Display math:\n\$\$\\int_{0}^{\\infty} e^{-x^2}\\,dx = \\frac{\\sqrt{\\pi}}{2}\$\$",
        "LaTeX-native delimiters: \\(\\alpha > \\beta\\) and \\[\\sum_{i=1}^{n} i = \\frac{n(n+1)}{2}\\]",
        "Reagents cost \$5 to \$10 per sample — currency must stay prose.",
        "Code with highlighting:\n```python\n# fit the model\nimport numpy as np\n\ndef gaussian(x, mu=0.0, sigma=1.0):\n    \"\"\"Return the pdf.\"\"\"\n    return np.exp(-((x - mu) ** 2) / (2 * sigma ** 2))\n```",
        "Mixed: given \$f(x)=x^2\$ we compute\n```kotlin\nval f = { x: Double -> x * x }\nprintln(f(3.0))  // 9.0\n```\nwhich yields \$f(3)=9\$.",
        "A matrix that must scroll rather than stretch the card:\n\$\$\\begin{pmatrix} a & b & c & d \\\\ e & f & g & h \\\\ i & j & k & l \\end{pmatrix}\$\$",
        "Malformed LaTeX should degrade, not crash: \$\\frac{1}{\$ and \$\\badcommand{x}\$",
        "Inline `code` spans stay monospace without a WebView.",
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BrightScholarTheme(darkTheme = false) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        samples.forEach { sample ->
                            CcCard(modifier = Modifier.fillMaxWidth()) {
                                ScholarlyText(
                                    text = sample,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
