package com.wanlok.scraper.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import com.wanlok.scraper.data.IpAddress
import com.wanlok.scraper.data.IpAddresses
import com.wanlok.scraper.ui.theme.ScraperTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val sampleAddresses = listOf(
    IpAddress("wlan0", "192.168.1.5"),
    IpAddress("rmnet_data0", "10.20.30.40")
)

/** The phone's own IP addresses, so you know where to reach the web server. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerIpAddressesScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    // The network isn't available in the @Preview renderer, so show a sample there.
    val isPreview = LocalInspectionMode.current
    // Null until the addresses are read, so "No IP addresses" doesn't flash up first.
    val addresses by produceState<List<IpAddress>?>(initialValue = null) {
        value = if (isPreview) sampleAddresses else withContext(Dispatchers.IO) {
            IpAddresses.list()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Server IP Addresses") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        val list = addresses ?: return@Scaffold
        if (list.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No IP addresses",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                items(list) { item ->
                    ListItem(
                        headlineContent = { Text(item.address) },
                        supportingContent = { Text(item.interfaceName) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ServerIpAddressesScreenPreview() {
    ScraperTheme {
        ServerIpAddressesScreen(onBack = { })
    }
}