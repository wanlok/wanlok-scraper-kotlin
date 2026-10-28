package com.wanlok.scraper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.wanlok.scraper.ui.navigation.AppNavigation
import com.wanlok.scraper.ui.theme.ScraperTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScraperTheme {
                AppNavigation()
            }
        }
    }
}