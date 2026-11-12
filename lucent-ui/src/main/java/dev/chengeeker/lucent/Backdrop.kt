package dev.chengeeker.lucent
import androidx.compose.runtime.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.*
import dev.chengeeker.lucent.review.recordBackdrop
import dev.chengeeker.lucent.review.frostedBackdrop as referenceFrost

typealias LucentBackdrop = dev.chengeeker.lucent.review.ReviewBackdrop
val LocalBackdrop = dev.chengeeker.lucent.review.LocalBackdrop
@Composable fun BackdropHost(modifier:Modifier=Modifier,overlay:@Composable BoxScope.()->Unit,content:@Composable BoxScope.()->Unit) {
 val layer=rememberGraphicsLayer();val backdrop=remember(layer){LucentBackdrop(layer)}
 CompositionLocalProvider(LocalBackdrop provides backdrop) {
  Box(modifier.fillMaxSize()) {
   Box(Modifier.fillMaxSize().recordBackdrop(backdrop).background(MaterialTheme.colorScheme.background),content=content)
   overlay()
  }
 }
}
@Composable fun Modifier.frostedBackdrop(color:Color,panel:Boolean=false,blur:Dp=20.dp,panelAlpha:Float=.82f,showBorder:Boolean=true):Modifier = referenceFrost(color,panel,blur,panelAlpha,showBorder)
