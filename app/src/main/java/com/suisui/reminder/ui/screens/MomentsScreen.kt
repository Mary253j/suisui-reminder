package com.suisui.reminder.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.suisui.reminder.data.MomentEntity
import com.suisui.reminder.ui.BackupHelper
import com.suisui.reminder.ui.ImageStore
import com.suisui.reminder.ui.MainViewModel
import com.suisui.reminder.ui.Routes
import com.suisui.reminder.ui.components.BottomNav
import java.io.File
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomentsScreen(navController: NavHostController) {
    val vm: MainViewModel = viewModel(factory = MainViewModel.Factory)
    val moments by vm.moments.collectAsState()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var addNote by remember { mutableStateOf("") }
    var addImagePaths by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedMoment by remember { mutableStateOf<MomentEntity?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(9)
    ) { uris ->
        if (uris.isNotEmpty()) {
            addImagePaths = uris.mapNotNull { ImageStore.copyUriToStorage(context, it) }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = BackupHelper.backupMoments(context, uri, moments)
                snackbarHostState.showSnackbar(result.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("美好瞬间") },
                actions = {
                    TextButton(onClick = { backupLauncher.launch(null) }) { Text("备份到云盘") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { BottomNav(navController, Routes.MOMENTS) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "记录瞬间")
            }
        }
    ) { padding ->
        if (moments.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("还没有美好瞬间，点右下角 + 记录第一张照片吧", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "点右上角「备份到云盘」可把照片复制到你选择的云盘文件夹（如 Google 云盘 / OneDrive）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(moments, key = { it.id }) { m ->
                    MomentCard(m, onClick = { selectedMoment = m })
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                addNote = ""
                addImagePaths = emptyList()
            },
            title = { Text("记录美好瞬间") },
            text = {
                Column {
                    OutlinedTextField(
                        value = addNote,
                        onValueChange = { addNote = it },
                        label = { Text("想说的话（可选）") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            imagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (addImagePaths.isEmpty()) "选择照片" else "重新选择（${addImagePaths.size} 张）")
                    }
                    if (addImagePaths.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            addImagePaths.take(4).forEach { path ->
                                AsyncImage(
                                    model = File(path),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .width(64.dp)
                                        .height(64.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (addImagePaths.isNotEmpty() || addNote.isNotBlank()) {
                        vm.addMoment(addNote.ifBlank { null }, addImagePaths)
                    }
                    showAddDialog = false
                    addNote = ""
                    addImagePaths = emptyList()
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    addNote = ""
                    addImagePaths = emptyList()
                }) { Text("取消") }
            }
        )
    }

    selectedMoment?.let { m ->
        Dialog(onDismissRequest = { selectedMoment = null }) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                Column(
                    Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(m.note ?: "美好瞬间", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    if (m.imagePaths.isEmpty()) {
                        Text("（无照片）", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        m.imagePaths.forEach { path ->
                            AsyncImage(
                                model = File(path),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row {
                        Button(
                            onClick = {
                                m.imagePaths.forEach { ImageStore.deleteImage(it) }
                                vm.deleteMoment(m)
                                selectedMoment = null
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.weight(1f)
                        ) { Text("删除这条瞬间") }
                        Spacer(Modifier.width(10.dp))
                        TextButton(onClick = { selectedMoment = null }, modifier = Modifier.weight(1f)) {
                            Text("关闭")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MomentCard(moment: MomentEntity, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Box {
            if (moment.imagePaths.isNotEmpty()) {
                AsyncImage(
                    model = File(moment.imagePaths.first()),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
            } else {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) { Text("无照片") }
            }
            if (!moment.note.isNullOrEmpty()) {
                Text(
                    moment.note,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(8.dp),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
