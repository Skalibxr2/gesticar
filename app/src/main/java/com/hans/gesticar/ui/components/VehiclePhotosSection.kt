package com.hans.gesticar.ui.components

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VehiclePhotosSection(
    modifier: Modifier = Modifier,
    receptionTitle: String = "Imágenes de recepción del vehículo",
    completionTitle: String = "Imágenes de reparación o entrega"
) {
    val context = LocalContext.current
    val receptionPhotos = remember { mutableStateListOf<Uri>() }
    val repairPhotos = remember { mutableStateListOf<Uri>() }
    val receptionCameraPhotos = remember { mutableStateListOf<Uri>() }
    val repairCameraPhotos = remember { mutableStateListOf<Uri>() }

    var pendingReceptionCameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingRepairCameraUri by remember { mutableStateOf<Uri?>(null) }

    val takeReceptionPhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingReceptionCameraUri
        if (success && uri != null) {
            receptionPhotos.add(uri)
            receptionCameraPhotos.add(uri)
        } else if (uri != null) {
            context.contentResolver.delete(uri, null, null)
        }
        pendingReceptionCameraUri = null
    }

    val takeRepairPhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingRepairCameraUri
        if (success && uri != null) {
            repairPhotos.add(uri)
            repairCameraPhotos.add(uri)
        } else if (uri != null) {
            context.contentResolver.delete(uri, null, null)
        }
        pendingRepairCameraUri = null
    }

    val pickReceptionGallery = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        uris.forEach { uri ->
            if (uri !in receptionPhotos) {
                receptionPhotos.add(uri)
            }
        }
    }

    val pickRepairGallery = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        uris.forEach { uri ->
            if (uri !in repairPhotos) {
                repairPhotos.add(uri)
            }
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Registro fotográfico",
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            text = "Agrega evidencia visual del estado inicial del vehículo y del resultado de la reparación.",
            style = MaterialTheme.typography.bodySmall
        )

        PhotoPickerSection(
            title = receptionTitle,
            photos = receptionPhotos,
            onRemove = { uri ->
                receptionPhotos.remove(uri)
                if (receptionCameraPhotos.remove(uri)) {
                    context.contentResolver.delete(uri, null, null)
                }
            },
            onCamera = {
                val uri = createImageUri(context, "recepcion")
                if (uri != null) {
                    pendingReceptionCameraUri = uri
                    takeReceptionPhoto.launch(uri)
                } else {
                    Toast.makeText(context, "No se pudo abrir la cámara", Toast.LENGTH_SHORT).show()
                }
            },
            onGallery = {
                pickReceptionGallery.launch(ActivityResultContracts.PickVisualMedia.ImageOnly)
            }
        )

        PhotoPickerSection(
            title = completionTitle,
            photos = repairPhotos,
            onRemove = { uri ->
                repairPhotos.remove(uri)
                if (repairCameraPhotos.remove(uri)) {
                    context.contentResolver.delete(uri, null, null)
                }
            },
            onCamera = {
                val uri = createImageUri(context, "reparacion")
                if (uri != null) {
                    pendingRepairCameraUri = uri
                    takeRepairPhoto.launch(uri)
                } else {
                    Toast.makeText(context, "No se pudo abrir la cámara", Toast.LENGTH_SHORT).show()
                }
            },
            onGallery = {
                pickRepairGallery.launch(ActivityResultContracts.PickVisualMedia.ImageOnly)
            }
        )
    }
}

@Composable
private fun PhotoPickerSection(
    title: String,
    photos: List<Uri>,
    onRemove: (Uri) -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCamera) {
                Icon(imageVector = Icons.Default.AddAPhoto, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Cámara")
            }
            OutlinedButton(onClick = onGallery) {
                Icon(imageVector = Icons.Default.Collections, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Galería")
            }
        }
        if (photos.isEmpty()) {
            Text("Aún no hay imágenes agregadas", style = MaterialTheme.typography.bodySmall)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(photos, key = { it.toString() }) { uri ->
                    Box(modifier = Modifier.size(96.dp)) {
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { onRemove(uri) },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar imagen")
                        }
                    }
                }
            }
        }
    }
}

private fun createImageUri(context: Context, prefix: String): Uri? {
    val resolver = context.contentResolver
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    } else {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "gesticar_${prefix}_$timeStamp.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Gesticar")
        }
    }
    return resolver.insert(collection, contentValues)
}
