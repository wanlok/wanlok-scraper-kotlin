package com.wanlok.scraper.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.wanlok.scraper.ui.theme.ScraperTheme

/**
 * A list row that does something when tapped: the text on the left and a [trailing] part on the
 * right, a right arrow unless something else (or null, for nothing) is given. It has no
 * background of its own.
 */
@Composable
fun WListItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = {
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
    }
) {
    ListItem(
        headlineContent = { Text(text) },
        trailingContent = trailing,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier.clickable(onClick = onClick)
    )
}

@Preview(showBackground = true)
@Composable
fun WListItemPreview() {
    ScraperTheme {
        WListItem(text = "10 November 2026", onClick = { })
    }
}