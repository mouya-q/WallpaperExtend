package com.wallpaperextend.ui

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.OnGloballyPositionedModifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toIntSize
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.ceil

private val GlassFill = Color(0xFFFFFFFF)
private val GlassReflect = Color(0xFFFFFFFF)
private val GlassShade = Color(0xFF1C1C1E)
private val GlassAccent = Color(0xFF0A84FF)

private const val SDF = """
float radiusAt(float2 p, float4 r) {
    if (p.x >= 0.0) {
        if (p.y <= 0.0) return r.y;
        else return r.z;
    } else {
        if (p.y <= 0.0) return r.x;
        else return r.w;
    }
}
float sdRoundedRect(float2 p, float2 half, float rad) {
    float2 c = abs(p) - (half - float2(rad));
    float o = length(max(c, 0.0)) - rad;
    float i = min(max(c.x, c.y), 0.0);
    return o + i;
}
float2 gradSdRoundedRect(float2 p, float2 half, float rad) {
    float2 c = abs(p) - (half - float2(rad));
    if (c.x >= 0.0 || c.y >= 0.0) {
        return sign(p) * normalize(max(c, 0.0));
    } else {
        float gx = step(c.y, c.x);
        return sign(p) * float2(gx, 1.0 - gx);
    }
}"""

internal val RefractionShader = """
uniform shader content;
uniform float2 size;
uniform float2 offset;
uniform float4 cornerRadii;
uniform float refractionHeight;
uniform float refractionAmount;
uniform float depthEffect;
$SDF
float circleMap(float x) {
    return 1.0 - sqrt(1.0 - x * x);
}
half4 main(float2 coord) {
    float2 halfSize = size * 0.5;
    float2 centered = (coord + offset) - halfSize;
    float rad = radiusAt(coord, cornerRadii);
    float sd = sdRoundedRect(centered, halfSize, rad);
    if (-sd >= refractionHeight) {
        return content.eval(coord);
    }
    sd = min(sd, 0.0);
    float d = circleMap(1.0 - -sd / refractionHeight) * refractionAmount;
    float gradRad = min(rad * 1.5, min(halfSize.x, halfSize.y));
    float2 grad = normalize(gradSdRoundedRect(centered, halfSize, gradRad) + depthEffect * normalize(centered));
    return content.eval(coord + d * grad);
}"""

private val HighlightShader = """
uniform float2 size;
uniform float4 cornerRadii;
layout(color) uniform half4 color;
uniform float angle;
uniform float falloff;
$SDF
half4 main(float2 coord) {
    float2 halfSize = size * 0.5;
    float2 centered = coord - halfSize;
    float rad = radiusAt(coord, cornerRadii);
    float gradRad = min(rad * 1.5, min(halfSize.x, halfSize.y));
    float2 grad = gradSdRoundedRect(centered, halfSize, gradRad);
    float2 normal = float2(cos(angle), sin(angle));
    float d = dot(grad, normal);
    float intensity = pow(abs(d), falloff);
    return color * intensity;
}"""

internal fun cornerRadiiOf(radiusPx: Float, width: Float, height: Float): FloatArray {
    val maxRadius = minOf(width, height) / 2f
    val r = radiusPx.coerceAtMost(maxRadius)
    return floatArrayOf(r, r, r, r)
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp = 16.dp,
    blurRadiusDp: androidx.compose.ui.unit.Dp = 22.dp,
    backdrop: GlassBackdrop? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current
    val radiusPx = with(density) { cornerRadius.toPx() }
    val blurPx = with(density) { blurRadiusDp.toPx() }
    val shape: Shape = RoundedCornerShape(cornerRadius)
    val supportsRuntime = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    val enterScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 380f),
        label = "glassEnterScale"
    )
    val enterAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 320),
        label = "glassEnterAlpha"
    )
    val refraction by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 480),
        label = "glassRefraction"
    )

    val highlightLayer = rememberGraphicsLayer()
    var pressed by remember { mutableStateOf(false) }
    var pressX by remember { mutableFloatStateOf(0f) }
    var pressY by remember { mutableFloatStateOf(0f) }

    val pressAmount by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 600f),
        label = "glassPress"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = enterScale
                scaleY = enterScale
                alpha = enterAlpha
                clip = true
                this.shape = shape
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        pressX = offset.x
                        pressY = offset.y
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    }
                )
            },
        contentAlignment = Alignment.TopStart
    ) {
        val dynamicRefraction = (blurPx * 0.65f + 10f) * refraction +
            pressAmount * blurPx * 0.5f
        val dynamicHeight = blurPx * 0.85f + 6f + pressAmount * blurPx * 0.4f

        val materialLayer: ContentDrawScope.() -> Unit = {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                drawRect(color = GlassFill.copy(alpha = 0.82f))
            } else {
                drawRect(
                    color = GlassFill.copy(alpha = 0.46f),
                    blendMode = BlendMode.SrcOver
                )
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            GlassReflect.copy(alpha = 0.72f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = size.height * 0.45f
                    ),
                    blendMode = BlendMode.SrcOver
                )
                drawRect(
                    brush = Brush.radialGradient(
                        center = Offset(size.width * 0.5f, size.height * 1.15f),
                        radius = size.height * 1.1f,
                        colors = listOf(
                            Color.Transparent,
                            GlassReflect.copy(alpha = 0.16f)
                        )
                    ),
                    blendMode = BlendMode.SrcOver
                )
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            GlassShade.copy(alpha = 0.08f)
                        ),
                        startY = size.height * 0.6f,
                        endY = size.height
                    ),
                    blendMode = BlendMode.SrcOver
                )
            }
        }

        val innerModifier = if (backdrop != null) {
            Modifier.drawGlassBackdrop(
                state = backdrop,
                shape = shape,
                cornerRadiusPx = radiusPx,
                blurPx = blurPx,
                refractionHeightPx = dynamicHeight,
                refractionAmountPx = dynamicRefraction,
                depthEffect = 0.4f
            ) { materialLayer() }
        } else {
            Modifier
                .graphicsLayer {
                    clip = false
                    this.shape = shape
                    compositingStrategy = CompositingStrategy.Offscreen
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val blur = android.graphics.RenderEffect
                            .createBlurEffect(blurPx, blurPx, android.graphics.Shader.TileMode.CLAMP)
                        if (supportsRuntime) {
                            val shader = RuntimeShader(RefractionShader)
                            shader.setFloatUniform("size", size.width, size.height)
                            shader.setFloatUniform(
                                "offset",
                                -pressAmount * (pressX - size.width / 2f),
                                -pressAmount * (pressY - size.height / 2f)
                            )
                            shader.setFloatUniform(
                                "cornerRadii",
                                cornerRadiiOf(radiusPx, size.width, size.height)
                            )
                            shader.setFloatUniform("refractionHeight", dynamicHeight)
                            shader.setFloatUniform("refractionAmount", -dynamicRefraction)
                            shader.setFloatUniform("depthEffect", 0.4f)
                            val refr = android.graphics.RenderEffect
                                .createRuntimeShaderEffect(shader, "content")
                            renderEffect = android.graphics.RenderEffect
                                .createChainEffect(refr, blur)
                                .asComposeRenderEffect()
                        } else {
                            renderEffect = blur.asComposeRenderEffect()
                        }
                    }
                }
                .drawWithContent { materialLayer() }
        }

        Box(
            modifier = Modifier.matchParentSize().then(innerModifier)
        ) {}

        if (supportsRuntime) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        clip = false
                        this.shape = shape
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        val safeSize = androidx.compose.ui.unit.IntSize(
                            ceil(size.width).toInt() + 2,
                            ceil(size.height).toInt() + 2
                        )
                        val outline = shape.createOutline(
                            size,
                            layoutDirection,
                            this@drawWithContent
                        )
                        val shader = RuntimeShader(HighlightShader)
                        shader.setFloatUniform("size", size.width, size.height)
                        shader.setFloatUniform(
                            "cornerRadii",
                            cornerRadiiOf(radiusPx, size.width, size.height)
                        )
                        shader.setColorUniform(
                            "color",
                            android.graphics.Color.valueOf(
                                GlassReflect.red,
                                GlassReflect.green,
                                GlassReflect.blue,
                                1f
                            )
                        )
                        shader.setFloatUniform("angle", 45f * (PI / 180f).toFloat())
                        shader.setFloatUniform("falloff", 1f)
                        val paint = Paint().apply {
                            style = PaintingStyle.Stroke
                            strokeWidth = 1.4f + pressAmount * 0.9f
                            blendMode = BlendMode.Plus
                            asFrameworkPaint().shader = shader
                        }
                        highlightLayer.record(safeSize) {
                            translate(1f, 1f) {
                                drawContext.canvas.drawOutline(outline, paint)
                            }
                        }
                        translate(-1f, -1f) {
                            drawLayer(highlightLayer)
                        }
                    }
            ) {}
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        clip = false
                        this.shape = shape
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        val outline = shape.createOutline(
                            size,
                            layoutDirection,
                            this@drawWithContent
                        )
                        drawOutline(
                            outline,
                            brush = SolidColor(GlassReflect),
                            style = Stroke(width = 1.4f),
                            blendMode = BlendMode.Plus
                        )
                    }
            ) {}
        }

        Box(
            modifier = Modifier.matchParentSize(),
            content = content
        )
    }
}

class GlassBackdrop {
    internal var graphicsLayer: GraphicsLayer? = null
    internal var coordinates: LayoutCoordinates? = null
    internal var selfCoordinates: LayoutCoordinates? = null
}

@Composable
fun rememberGlassBackdrop(): GlassBackdrop {
    val layer = rememberGraphicsLayer()
    return remember(layer) { GlassBackdrop().apply { graphicsLayer = layer } }
}

private class BackdropLayerNode(
    val state: GlassBackdrop
) : Modifier.Node(), DrawModifierNode, OnGloballyPositionedModifier {
    override fun ContentDrawScope.draw() {
        drawContent()
        val layer = state.graphicsLayer
        if (layer != null && isAttached) {
            layer.record(size.toIntSize()) {
                drawContent()
            }
        }
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (coordinates.isAttached) {
            state.coordinates = coordinates
        }
    }

    override fun onAttach() {
        if (state.graphicsLayer == null) {
            state.graphicsLayer = requireGraphicsContext().createGraphicsLayer()
        }
    }

    override fun onDetach() {
        state.graphicsLayer?.let { requireGraphicsContext().releaseGraphicsLayer(it) }
        state.graphicsLayer = null
        state.coordinates = null
    }
}

private class BackdropLayerElement(
    val state: GlassBackdrop
) : ModifierNodeElement<BackdropLayerNode>() {
    override fun create() = BackdropLayerNode(state)
    override fun update(node: BackdropLayerNode) {}
    override fun InspectorInfo.inspectableProperties() {
        name = "backdropLayer"
    }

    override fun equals(other: Any?) = other is BackdropLayerElement && other.state === state
    override fun hashCode() = state.hashCode()
}

fun Modifier.backdropLayer(state: GlassBackdrop): Modifier =
    this then BackdropLayerElement(state)

fun Modifier.drawGlassBackdrop(
    state: GlassBackdrop,
    shape: Shape,
    cornerRadiusPx: Float = 0f,
    blurPx: Float = 22f,
    refractionHeightPx: Float = 18f,
    refractionAmountPx: Float = 22f,
    depthEffect: Float = 0.4f,
    content: ContentDrawScope.() -> Unit
): Modifier {
    val supportsRuntime = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    return this
        .onGloballyPositioned { state.selfCoordinates = it }
        .graphicsLayer {
            clip = false
            this.shape = shape
            compositingStrategy = CompositingStrategy.Offscreen
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val blur = android.graphics.RenderEffect
                    .createBlurEffect(blurPx, blurPx, android.graphics.Shader.TileMode.CLAMP)
                if (supportsRuntime) {
                    val shader = RuntimeShader(RefractionShader)
                    shader.setFloatUniform("size", size.width, size.height)
                    shader.setFloatUniform("offset", 0f, 0f)
                    shader.setFloatUniform(
                        "cornerRadii",
                        cornerRadiiOf(cornerRadiusPx, size.width, size.height)
                    )
                    shader.setFloatUniform("refractionHeight", refractionHeightPx)
                    shader.setFloatUniform("refractionAmount", -refractionAmountPx)
                    shader.setFloatUniform("depthEffect", depthEffect)
                    val refr = android.graphics.RenderEffect
                        .createRuntimeShaderEffect(shader, "content")
                    renderEffect = android.graphics.RenderEffect
                        .createChainEffect(refr, blur)
                        .asComposeRenderEffect()
                } else {
                    renderEffect = blur.asComposeRenderEffect()
                }
            }
        }
        .drawWithContent {
            val layer = state.graphicsLayer
            val coords = state.coordinates
            val selfCoords = state.selfCoordinates
            if (layer != null && coords != null && coords.isAttached && selfCoords != null && selfCoords.isAttached) {
                val offset = coords.positionInWindow() - selfCoords.positionInWindow()
                translate(-offset.x, -offset.y) {
                    drawLayer(layer)
                }
            }
            content()
        }
}