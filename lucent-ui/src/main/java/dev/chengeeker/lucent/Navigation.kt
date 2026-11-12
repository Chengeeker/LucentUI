package dev.chengeeker.lucent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.Box

data class LucentDestination(val label:String,val icon:ImageVector,val activeIcon:ImageVector=icon)
@Composable fun LucentNavigation(items:List<LucentDestination>,selected:Int,onSelect:(Int)->Unit,modifier:Modifier=Modifier) {
 require(items.size==3)
 val style=LocalLucentStyle.current
 Box(modifier) { dev.chengeeker.lucent.review.ReviewBottomBar(selected,style.floating,style.glass,style.translucent,items.map{it.label},items.map{it.icon},items.map{it.activeIcon},onSelect) }
}
