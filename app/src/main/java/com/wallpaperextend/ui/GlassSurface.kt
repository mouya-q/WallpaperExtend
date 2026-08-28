package com.wallpaperextend.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    color: Color = Color(0x80FFFFFF),
    cornerRadius: Float = 16f,
    backdrop: Backdrop? = null,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(
                color = color,
                shape = RoundedCornerShape(cornerRadius.dp)
            )
            .then(
                if (backdrop != null) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedCornerShape(cornerRadius.dp) },
                        effects = {
                            blur(8f.dp.toPx())
                            vibrancy()
                        }
                    )
                } else {
                    Modifier
                }
            )
    ) {
        content()
    }
}
