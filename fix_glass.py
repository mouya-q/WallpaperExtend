import re
p = 'app/src/main/java/com/wallpaperextend/ui/GlassSurface.kt'
s = open(p, encoding='utf-8').read()
s = re.sub(r'import com\.kyant\.backdrop\.[^\n]*\n', '', s)
s = re.sub(r'import androidx\.compose\.ui\.draw\.drawBehind\n', '', s)
s = s.replace('    backdrop: Backdrop? = null,', '    backdropAlpha: Float = 0.5f,')
old = '                if (backdrop != null) {\n                    Modifier.drawBackdrop(\n                        backdrop = backdrop,\n                        effect = blur(20f) + vibrancy()\n                    )\n                } else {\n                    Modifier\n                }'
new = '                Modifier.background(Color.Black.copy(alpha = backdropAlpha))'
s = s.replace(old, new)
open(p, 'w', encoding='utf-8').write(s)
print('GlassSurface patched')
