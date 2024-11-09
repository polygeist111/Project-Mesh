package com.greybox.projectmesh.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.greybox.projectmesh.ViewModelFactory
import com.greybox.projectmesh.buttonStyle.GradientButton
import com.greybox.projectmesh.ui.theme.AppTheme
import com.greybox.projectmesh.viewModel.SettingsScreenViewModel
import org.kodein.di.compose.localDI


@Composable
fun SettingsScreen(
    viewModel: SettingsScreenViewModel = viewModel(
        factory = ViewModelFactory(
            di = localDI(),
            owner = LocalSavedStateRegistryOwner.current,
            vmFactory = { SettingsScreenViewModel(it) },
            defaultArgs = null,
        )),
    onThemeChange: (AppTheme) -> Unit
) {
    val currTheme = viewModel.theme.collectAsState()

    var deviceName by remember { mutableStateOf("Samsung S24 Ultra") }
    var autoFinish by remember { mutableStateOf(false) }
    // for Language setting
    var langExpanded by remember { mutableStateOf(false) } // Track menu visibility
    val langMenuItems = listOf("System", "English", "Spanish", "French", "简体中文") // Menu items
    var langSelectedOption by remember { mutableStateOf("System") } // Track selected item

    Column(modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState()))
    {
        Spacer(modifier = Modifier.height(36.dp))
        Text(
            "Settings", style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
            modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(modifier = Modifier.height(18.dp))
        Column(modifier = Modifier.padding(36.dp)) {
            Text("General", style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold))
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp, 10.dp),
                thickness = 2.dp,
                color = Color.Red
            )
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp, 16.dp))
            {
                Text(
                    "Language", style = TextStyle(fontSize = 18.sp), modifier = Modifier
                        .align(Alignment.CenterVertically))
                Spacer(modifier = Modifier.weight(1f))
                Box(modifier = Modifier.align(Alignment.CenterVertically))
                {
                    GradientButton(text = langSelectedOption,
                        onClick = { langExpanded = true })
                    DropdownMenu(expanded = langExpanded,
                        onDismissRequest = { langExpanded = false },
                        properties = PopupProperties(true))
                    {
                        langMenuItems.forEach{ item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    langSelectedOption = item
                                    langExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp, 16.dp))
            {
                Text(
                    text = "Theme", style = TextStyle(fontSize = 18.sp), modifier = Modifier
                        .align(Alignment.CenterVertically))
                Spacer(modifier = Modifier.weight(1f))
                ThemeSetting(currentTheme = currTheme.value,
                    onThemeSelected = { selectedTheme ->
                        viewModel.saveTheme(selectedTheme)
                        onThemeChange(selectedTheme) })
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text("Network", style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold))
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp, 10.dp),
                thickness = 2.dp,
                color = Color.Red
            )
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp, 16.dp))
            {
                Text(
                    "Server", style = TextStyle(fontSize = 18.sp), modifier = Modifier
                        .align(Alignment.CenterVertically))
                Spacer(modifier = Modifier.weight(1f))
                GradientButton(text = "Restart", onClick = {  })
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp, 16.dp))
            {
                Text(
                    text = "Device Name", style = TextStyle(fontSize = 18.sp), modifier = Modifier
                        .align(Alignment.CenterVertically)
                )
                Spacer(modifier = Modifier.weight(1f))
                TextField(
                    value = deviceName,
                    onValueChange = { deviceName = it },
                    modifier = Modifier
                        .width(120.dp)
                        .height(50.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text("Receive", style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold))
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp, 10.dp),
                thickness = 2.dp,
                color = Color.Red
            )
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp, 16.dp))
            {
                Text(
                    "Auto Finish", style = TextStyle(fontSize = 18.sp), modifier = Modifier
                        .align(Alignment.CenterVertically))
                Spacer(modifier = Modifier.weight(1f))
                Box(modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .width(130.dp)
                    .height(70.dp))
                {
                    Switch(checked = autoFinish, onCheckedChange = { autoFinish = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            uncheckedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF4CAF50),
                            uncheckedTrackColor = Color.LightGray,
                        ),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .scale(1.3f))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp, 16.dp))
            {
                Text(
                    text = "Save to folder", style = TextStyle(fontSize = 18.sp), modifier = Modifier
                        .align(Alignment.CenterVertically))
                Spacer(modifier = Modifier.weight(1f))
                GradientButton(text = "Download", onClick = { })
            }
        }
    }
}

@Composable
fun ThemeSetting(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit)
{
    // for Theme setting
    var expanded by remember { mutableStateOf(false) } // Track menu visibility
    val themes = listOf("System", "Light", "Dark") // Menu items
    Box()
    {
        GradientButton(text = themes[currentTheme.ordinal],
            onClick = { expanded = true })
        DropdownMenu(expanded = expanded,
            onDismissRequest = { expanded = false },
            properties = PopupProperties(true))
        {
            AppTheme.entries.forEach{ theme ->
                DropdownMenuItem(
                    text = { Text(themes[theme.ordinal]) },
                    onClick = {
                        onThemeSelected(theme)
                        expanded = false
                    }
                )
            }
        }
    }
}