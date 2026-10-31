package com.wanlok.scraper.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

private var stopIcon: ImageVector? = null

/**
 * Material's Stop icon. The ready-made one is only in material-icons-extended, a very large
 * library, so this draws the same square instead.
 */
val Icons.Filled.Stop: ImageVector
    get() = stopIcon ?: materialIcon(name = "Filled.Stop") {
        materialPath {
            moveTo(6f, 6f)
            horizontalLineToRelative(12f)
            verticalLineToRelative(12f)
            horizontalLineTo(6f)
            close()
        }
    }.also { stopIcon = it }
