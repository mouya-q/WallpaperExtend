package com.wallpaperextend.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val GlassMaterialLight = Color(0xFFFFFFFF)
private val GlassStrokeLight = Color(0xFFFFFFFF)
private val GlassAccent = Color(0xFF0A84FF)

fun Modifier.liquidGlass(
    cornerRadius: Dp = 16.dp,
    blurRadiusDp: Dp = 28.dp,
    materialAlpha: Float = 0.55f,
    strokeAlpha: Float = 0.7f
): Modifier {
    val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    return this
        .graphicsLayer {
            if (supportsBlur) {
                renderEffect = android.graphics.RenderEffect
                    .createBlurEffect(
                        blurRadiusDp.value,
                        blurRadiusDp.value,
                        android.graphics.Shader.TileMode.CLAMP
                    )
                    .asComposeRenderEffect()
            }
            compositingStrategy = CompositingStrategy.Offscreen
        }
        .drawWithContent {
            drawContent()
            if (supportsBlur) {
                drawRect(
                    color = GlassMaterialLight.copy(alpha = materialAlpha),
                    blendMode = BlendMode.SrcOver
                )
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            GlassStrokeLight.copy(alpha = strokeAlpha),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = size.height * 0.5f
                    ),
                    blendMode = BlendMode.SrcOver
                )
            }
        }
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    blurRadiusDp: Dp = 28.dp,
    materialAlpha: Float = 0.6f,
    strokeAlpha: Float = 0.7f,
    content: @Composable BoxScope.() -> Unit
) {
    val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (supportsBlur) {
                    Modifier.liquidGlass(
                        cornerRadius = cornerRadius,
                        blurRadiusDp = blurRadiusDp,
                        materialAlpha = materialAlpha,
                        strokeAlpha = strokeAlpha
                    )
                } else {
                    Modifier
                        .background(GlassMaterialLight.copy(alpha = 0.82f))
                        .border(
                            width = 1.dp,
                            color = GlassAccent.copy(alpha = 0.35f),
                            shape = shape
                        )
                }
            ),
        content = content
    )
}