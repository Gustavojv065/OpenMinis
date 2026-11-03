package com.openminis.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.graphics.drawable.toBitmap
import com.openminis.app.R

/**
 * The installed variant's own launcher logo: ALTIV on the ALTIV flavor, Minis
 * on the upstream variant. No hard-coded upstream icon resource is used.
 */
@Composable
fun AppBrandLogo(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val icon = remember(context.packageName) {
        val drawable = context.packageManager.getApplicationIcon(context.packageName)
        BitmapPainter(drawable.toBitmap(width = 256, height = 256).asImageBitmap())
    }
    Image(
        painter = icon,
        contentDescription = stringResource(R.string.app_name),
        modifier = modifier,
    )
}
