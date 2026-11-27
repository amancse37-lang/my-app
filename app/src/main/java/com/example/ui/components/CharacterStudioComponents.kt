package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CharacterBitmapCodec
import com.example.data.CharacterImageGenState
import com.example.data.CharacterReferenceSheet
import com.example.data.StudioCharacter
import com.example.data.StudioPresets
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioAmberDim
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioBorderSubtle
import com.example.ui.theme.StudioCharcoal
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioCoralDim
import com.example.ui.theme.StudioElevated
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioPanel
import com.example.ui.theme.StudioTeal
import com.example.ui.theme.StudioTealDim
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.StudioVoid

@Composable
fun CharacterReferenceImage(
    base64Image: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    colorFilter: ColorFilter? = null
) {
    val decoded = remember(base64Image) {
        if (base64Image.isNotBlank()) CharacterBitmapCodec.decodeBase64ToBitmap(base64Image) else null
    }
    if (decoded != null) {
        Image(
            bitmap = decoded.asImageBitmap(),
            contentDescription = contentDescription,
            contentScale = contentScale,
            colorFilter = colorFilter,
            modifier = modifier
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.img_char_narrator_1795597592267),
            contentDescription = contentDescription,
            contentScale = contentScale,
            colorFilter = colorFilter,
            modifier = modifier
        )
    }
}

@Composable
fun CharacterAvatarThumbnail(
    character: StudioCharacter,
    size: Dp,
    borderColor: Color,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(StudioVoid)
            .border(1.5.dp, borderColor, shape)
    ) {
        CharacterReferenceImage(
            base64Image = character.referenceImageBase64,
            contentDescription = character.name,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun CharacterLibraryPanel(
    characters: List<StudioCharacter>,
    selectedCharacter: StudioCharacter?,
    selectedCharacterReferences: List<CharacterReferenceSheet>,
    assignedSceneCharacterIds: Set<String>,
    onSelectCharacter: (String) -> Unit,
    onAddCharacter: () -> Unit,
    onEditCharacter: (StudioCharacter) -> Unit,
    onRenameCharacter: (StudioCharacter) -> Unit,
    onRequestDeleteCharacter: (StudioCharacter) -> Unit,
    onOpenGenerator: (StudioCharacter) -> Unit,
    onAddCharacterToScene: (String) -> Unit,
    onRemoveCharacterFromScene: (String) -> Unit,
    onSetReferenceAsPrimary: (String, String) -> Unit,
    onDeleteReference: (String, String) -> Unit
) {
    var showProfileInspector by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header + Add Character CTA
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                Text(
                    text = "CHARACTERS (${characters.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = StudioAmber
                )
                Text(
                    text = "Consistent 2D cast library",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = StudioTextMuted
                )
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = StudioAmberDim,
                border = BorderStroke(1.dp, StudioAmber.copy(alpha = 0.6f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onAddCharacter)
                    .testTag("add_character_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Character",
                        tint = StudioAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text("Add", style = MaterialTheme.typography.labelSmall, color = StudioAmber)
                }
            }
        }

        // Sub-View Switcher: Cast List vs Selected Character Details & Reference Sheets
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 2.dp)
        ) {
            val listSelected = !showProfileInspector
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (listSelected) StudioElevated else StudioCharcoal,
                border = BorderStroke(1.dp, if (listSelected) StudioAmber else StudioBorder),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { showProfileInspector = false }
                    .testTag("char_library_list_tab")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    Text(
                        text = "Cast Library",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (listSelected) StudioAmber else StudioTextSecondary
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (!listSelected) StudioElevated else StudioCharcoal,
                border = BorderStroke(1.dp, if (!listSelected) StudioTeal else StudioBorder),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(enabled = selectedCharacter != null) { showProfileInspector = true }
                    .testTag("char_library_details_tab")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    Text(
                        text = selectedCharacter?.charCode?.let { "$it Profile" } ?: "Profile & Refs",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (!listSelected) StudioTeal else StudioTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        if (characters.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = null,
                        tint = StudioTextMuted,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "No characters yet. Generate scenes from a script or tap Add.",
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioTextSecondary
                    )
                }
            }
        } else if (!showProfileInspector || selectedCharacter == null) {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(characters, key = { it.id }) { character ->
                    val isInScene = character.id in assignedSceneCharacterIds
                    val isSelected = character.id == selectedCharacter?.id
                    val accent = parseHexColor(character.accentColorHex, StudioAmber)
                    val hasAiRef = character.referenceImageBase64.isNotBlank()

                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = if (isSelected) StudioElevated else StudioCharcoal,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) accent else StudioBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectCharacter(character.id)
                                showProfileInspector = true
                            }
                            .testTag("character_card_${character.charCode.lowercase()}")
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CharacterAvatarThumbnail(
                                    character = character,
                                    size = 46.dp,
                                    borderColor = accent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Spacer(Modifier.width(9.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Surface(
                                            color = accent.copy(alpha = 0.18f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = character.charCode,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                color = accent,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                        if (hasAiRef) {
                                            Surface(
                                                color = StudioTealDim,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "2D REF",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                                    color = StudioTeal,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = character.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = StudioTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = listOfNotNull(
                                            character.role.takeIf { it.isNotBlank() },
                                            character.visualAge.takeIf { it.isNotBlank() }
                                        ).joinToString(" · ").ifBlank { "Documentary Character" },
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = StudioTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (character.appearanceNotes.isNotBlank() || character.clothing.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = character.appearanceNotes.ifBlank { character.clothing },
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                    color = StudioTextMuted,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(Modifier.height(8.dp))
                            // Primary Action Row: Generate Character & Use in Scene
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = StudioAmberDim,
                                    border = BorderStroke(1.dp, StudioAmber.copy(alpha = 0.6f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            onSelectCharacter(character.id)
                                            onOpenGenerator(character)
                                        }
                                        .testTag("generate_character_btn_${character.charCode.lowercase()}")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = "Generate Character Design",
                                            tint = StudioAmber,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "Generate",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = StudioAmber,
                                            maxLines = 1
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isInScene) StudioTealDim else StudioPanel,
                                    border = BorderStroke(1.dp, if (isInScene) StudioTeal else StudioBorder),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            if (isInScene) {
                                                onRemoveCharacterFromScene(character.id)
                                            } else {
                                                onAddCharacterToScene(character.id)
                                            }
                                        }
                                        .testTag("assign_scene_btn_${character.charCode.lowercase()}")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isInScene) Icons.Default.CheckCircle else Icons.Default.PersonAdd,
                                            contentDescription = if (isInScene) "In Scene" else "Use in Scene",
                                            tint = if (isInScene) StudioTeal else StudioTextPrimary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = if (isInScene) "In Scene" else "Use in Scene",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isInScene) StudioTeal else StudioTextPrimary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(4.dp))
                            // Secondary Row: Edit, Rename, Delete
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = character.visualStyle,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = StudioTextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Row {
                                    IconButton(
                                        onClick = {
                                            onSelectCharacter(character.id)
                                            onEditCharacter(character)
                                        },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .testTag("edit_character_btn_${character.charCode.lowercase()}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit character profile",
                                            tint = StudioTextSecondary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onRenameCharacter(character) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DriveFileRenameOutline,
                                            contentDescription = "Rename character",
                                            tint = StudioTextSecondary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onRequestDeleteCharacter(character) },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .testTag("delete_character_btn_${character.charCode.lowercase()}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete character",
                                            tint = StudioCoral,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            CharacterDetailsInspectorView(
                character = selectedCharacter,
                references = selectedCharacterReferences,
                isInScene = selectedCharacter.id in assignedSceneCharacterIds,
                onEdit = { onEditCharacter(selectedCharacter) },
                onRename = { onRenameCharacter(selectedCharacter) },
                onGenerateDesign = { onOpenGenerator(selectedCharacter) },
                onToggleSceneCast = {
                    if (selectedCharacter.id in assignedSceneCharacterIds) {
                        onRemoveCharacterFromScene(selectedCharacter.id)
                    } else {
                        onAddCharacterToScene(selectedCharacter.id)
                    }
                },
                onDelete = { onRequestDeleteCharacter(selectedCharacter) },
                onSetReferencePrimary = { base64 ->
                    onSetReferenceAsPrimary(selectedCharacter.id, base64)
                },
                onDeleteReference = { refId ->
                    onDeleteReference(selectedCharacter.id, refId)
                }
            )
        }
    }
}

@Composable
private fun CharacterDetailsInspectorView(
    character: StudioCharacter,
    references: List<CharacterReferenceSheet>,
    isInScene: Boolean,
    onEdit: () -> Unit,
    onRename: () -> Unit,
    onGenerateDesign: () -> Unit,
    onToggleSceneCast: () -> Unit,
    onDelete: () -> Unit,
    onSetReferencePrimary: (String) -> Unit,
    onDeleteReference: (String) -> Unit
) {
    val accent = parseHexColor(character.accentColorHex, StudioAmber)

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        // Primary Reference Preview Card
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = StudioCharcoal,
            border = BorderStroke(1.dp, accent.copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(StudioVoid)
                        .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                ) {
                    CharacterReferenceImage(
                        base64Image = character.referenceImageBase64,
                        contentDescription = "${character.name} Reference",
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        color = StudioVoid.copy(alpha = 0.84f),
                        shape = RoundedCornerShape(5.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "${character.charCode} · IDENTITY LOCK",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                color = accent
                            )
                        }
                    }
                    Surface(
                        color = StudioVoid.copy(alpha = 0.82f),
                        shape = RoundedCornerShape(5.dp),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = if (character.referenceImageBase64.isNotBlank()) "AI 2D Reference Active" else "Default Placeholder · Generate 2D Ref",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                            color = if (character.referenceImageBase64.isNotBlank()) StudioTeal else StudioAmber,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = character.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = StudioTextPrimary
                )
                Text(
                    text = "${character.role.ifBlank { "Documentary Character" }} · ${character.visualStyle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioTextSecondary
                )

                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onGenerateDesign,
                    colors = ButtonDefaults.buttonColors(containerColor = StudioAmber, contentColor = StudioVoid),
                    shape = RoundedCornerShape(7.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("details_generate_character_button")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Generate 2D Reference", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onEdit,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        border = BorderStroke(1.dp, StudioBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Edit Profile", style = MaterialTheme.typography.labelSmall, color = StudioTextPrimary)
                    }
                    OutlinedButton(
                        onClick = onToggleSceneCast,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        border = BorderStroke(1.dp, if (isInScene) StudioTeal else StudioBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (isInScene) "In Scene" else "+ Scene",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isInScene) StudioTeal else StudioAmber
                        )
                    }
                }
            }
        }

        // Complete 16-Field Character Consistency Profile Summary
        Surface(
            shape = RoundedCornerShape(9.dp),
            color = StudioCharcoal,
            border = BorderStroke(1.dp, StudioBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "CHARACTER CONSISTENCY PROFILE",
                        style = MaterialTheme.typography.labelSmall,
                        color = StudioAmber,
                        fontWeight = FontWeight.Bold
                    )
                    Row {
                        TextButton(onClick = onRename, contentPadding = PaddingValues(horizontal = 4.dp)) {
                            Text("Rename", style = MaterialTheme.typography.labelSmall, color = StudioTeal)
                        }
                        TextButton(onClick = onDelete, contentPadding = PaddingValues(horizontal = 4.dp)) {
                            Text("Delete", style = MaterialTheme.typography.labelSmall, color = StudioCoral)
                        }
                    }
                }

                ProfileSpecRow("Character ID", character.charCode)
                ProfileSpecRow("Visual Age", character.visualAge.ifBlank { "Not specified" })
                ProfileSpecRow("Face", character.faceDescription.ifBlank { "Not specified" })
                ProfileSpecRow(
                    "Hair",
                    listOf(character.hairStyle, character.hairColor).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Not specified" }
                )
                ProfileSpecRow("Clothing", character.clothing.ifBlank { "Not specified" })
                ProfileSpecRow("Clothing Colors", character.clothingColors.ifBlank { character.wardrobePalette }.ifBlank { "Not specified" })
                ProfileSpecRow("Proportions", character.bodyProportions.ifBlank { "Not specified" })
                ProfileSpecRow("Accessories", character.accessories.ifBlank { "None" })
                ProfileSpecRow("Personality", character.personality.ifBlank { "Not specified" })
                ProfileSpecRow("Default Emotion", character.expressionDefault.ifBlank { "Calm" })
                ProfileSpecRow("Visual Style", character.visualStyle.ifBlank { "Painterly 2D Documentary" })
                ProfileSpecRow("Consistency Notes", character.appearanceNotes.ifBlank { "Keep exact identity across all scenes." })
            }
        }

        // Saved Turnaround / Expression Reference Gallery
        Surface(
            shape = RoundedCornerShape(9.dp),
            color = StudioCharcoal,
            border = BorderStroke(1.dp, StudioBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(10.dp)
            ) {
                Text(
                    text = "REFERENCE TURNAROUNDS (${references.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = StudioTeal,
                    fontWeight = FontWeight.Bold
                )
                if (references.isEmpty()) {
                    Text(
                        text = "No turnaround sheets generated yet. Tap \"Generate 2D Reference\" to create front, side, three-quarter, full-body, or expression sheets.",
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioTextMuted
                    )
                } else {
                    references.forEach { sheet ->
                        val isPrimary = sheet.imageBase64.isNotBlank() && sheet.imageBase64 == character.referenceImageBase64
                        Surface(
                            shape = RoundedCornerShape(7.dp),
                            color = StudioPanel,
                            border = BorderStroke(1.dp, if (isPrimary) StudioAmber else StudioBorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(7.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(StudioVoid)
                                ) {
                                    CharacterReferenceImage(
                                        base64Image = sheet.imageBase64,
                                        contentDescription = sheet.viewType,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sheet.viewType,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = StudioTextPrimary,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${sheet.expression} · ${sheet.poseAction}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        color = StudioTeal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = sheet.visualStyle,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                        color = StudioTextMuted,
                                        maxLines = 1
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    IconButton(
                                        onClick = { onSetReferencePrimary(sheet.imageBase64) },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Set as primary reference",
                                            tint = if (isPrimary) StudioAmber else StudioTextMuted,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteReference(sheet.id) },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete reference sheet",
                                            tint = StudioCoral,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ProfileSpecRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
            color = StudioTextMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
            color = StudioTextPrimary
        )
    }
}

@Composable
fun CharacterProfileEditorDialog(
    initial: StudioCharacter?,
    suggestedCharCode: String,
    onDismiss: () -> Unit,
    onSave: (StudioCharacter) -> Unit
) {
    var charCode by remember(initial?.id) { mutableStateOf(initial?.charCode?.ifBlank { suggestedCharCode } ?: suggestedCharCode) }
    var name by remember(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var role by remember(initial?.id) { mutableStateOf(initial?.role.orEmpty()) }
    var visualAge by remember(initial?.id) { mutableStateOf(initial?.visualAge.orEmpty()) }
    var faceDescription by remember(initial?.id) { mutableStateOf(initial?.faceDescription.orEmpty()) }
    var hairStyle by remember(initial?.id) { mutableStateOf(initial?.hairStyle.orEmpty()) }
    var hairColor by remember(initial?.id) { mutableStateOf(initial?.hairColor.orEmpty()) }
    var clothing by remember(initial?.id) { mutableStateOf(initial?.clothing.orEmpty()) }
    var clothingColors by remember(initial?.id) { mutableStateOf(initial?.clothingColors?.ifBlank { initial.wardrobePalette }.orEmpty()) }
    var bodyProportions by remember(initial?.id) { mutableStateOf(initial?.bodyProportions.orEmpty()) }
    var accessories by remember(initial?.id) { mutableStateOf(initial?.accessories.orEmpty()) }
    var personality by remember(initial?.id) { mutableStateOf(initial?.personality.orEmpty()) }
    var expressionDefault by remember(initial?.id) { mutableStateOf(initial?.expressionDefault?.ifBlank { "Calm" } ?: "Calm") }
    var visualStyle by remember(initial?.id) { mutableStateOf(initial?.visualStyle?.ifBlank { StudioPresets.characterVisualStyles.first() } ?: StudioPresets.characterVisualStyles.first()) }
    var consistencyNotes by remember(initial?.id) { mutableStateOf(initial?.appearanceNotes.orEmpty()) }
    var accentColorHex by remember(initial?.id) { mutableStateOf(initial?.accentColorHex?.ifBlank { "#F5A623" } ?: "#F5A623") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioPanel,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Badge, contentDescription = null, tint = StudioAmber, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (initial == null) "Create Character Profile" else "Edit ${initial.charCode} Profile",
                    style = MaterialTheme.typography.headlineSmall,
                    color = StudioTextPrimary
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = charCode,
                        onValueChange = { charCode = it.uppercase().take(16) },
                        label = { Text("Character ID") },
                        singleLine = true,
                        colors = charDialogFieldColors(),
                        modifier = Modifier.width(112.dp)
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(100) },
                        label = { Text("Character Name *") },
                        singleLine = true,
                        colors = charDialogFieldColors(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("character_name_input")
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = role,
                        onValueChange = { role = it.take(100) },
                        label = { Text("Role in Story") },
                        singleLine = true,
                        colors = charDialogFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = visualAge,
                        onValueChange = { visualAge = it.take(60) },
                        label = { Text("Visual Age") },
                        placeholder = { Text("e.g. Mid 40s", color = StudioTextMuted) },
                        singleLine = true,
                        colors = charDialogFieldColors(),
                        modifier = Modifier.width(118.dp)
                    )
                }

                OutlinedTextField(
                    value = faceDescription,
                    onValueChange = { faceDescription = it.take(400) },
                    label = { Text("Face Description") },
                    placeholder = { Text("Eyes, skin tone, facial features, expression lines", color = StudioTextMuted) },
                    minLines = 2,
                    maxLines = 3,
                    colors = charDialogFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hairStyle,
                        onValueChange = { hairStyle = it.take(160) },
                        label = { Text("Hair Style") },
                        singleLine = true,
                        colors = charDialogFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = hairColor,
                        onValueChange = { hairColor = it.take(80) },
                        label = { Text("Hair Color") },
                        singleLine = true,
                        colors = charDialogFieldColors(),
                        modifier = Modifier.width(118.dp)
                    )
                }

                OutlinedTextField(
                    value = clothing,
                    onValueChange = { clothing = it.take(400) },
                    label = { Text("Clothing / Wardrobe") },
                    minLines = 2,
                    maxLines = 3,
                    colors = charDialogFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = clothingColors,
                        onValueChange = { clothingColors = it.take(200) },
                        label = { Text("Clothing Colors") },
                        singleLine = true,
                        colors = charDialogFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = bodyProportions,
                        onValueChange = { bodyProportions = it.take(200) },
                        label = { Text("Body Proportions") },
                        singleLine = true,
                        colors = charDialogFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = accessories,
                    onValueChange = { accessories = it.take(300) },
                    label = { Text("Accessories") },
                    placeholder = { Text("e.g. Round spectacles, leather satchel", color = StudioTextMuted) },
                    singleLine = true,
                    colors = charDialogFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = personality,
                    onValueChange = { personality = it.take(300) },
                    label = { Text("Personality Description") },
                    singleLine = true,
                    colors = charDialogFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Default Emotional State", style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    StudioPresets.characterEmotions.forEach { emotion ->
                        FilterChip(
                            selected = expressionDefault == emotion,
                            onClick = { expressionDefault = emotion },
                            label = { Text(emotion, style = MaterialTheme.typography.labelSmall) },
                            colors = charDialogChipColors()
                        )
                    }
                }

                Text("Character Visual Style", style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    StudioPresets.characterVisualStyles.forEach { style ->
                        FilterChip(
                            selected = visualStyle == style,
                            onClick = { visualStyle = style },
                            label = { Text(style, style = MaterialTheme.typography.labelSmall) },
                            colors = charDialogChipColors()
                        )
                    }
                }

                Text("Identity Accent Badge Color", style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StudioPresets.characterAccentPalette.forEach { hex ->
                        val selected = accentColorHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(hex, StudioAmber))
                                .border(
                                    width = if (selected) 2.5.dp else 1.dp,
                                    color = if (selected) Color.White else StudioBorder,
                                    shape = CircleShape
                                )
                                .clickable { accentColorHex = hex }
                        )
                    }
                }

                OutlinedTextField(
                    value = consistencyNotes,
                    onValueChange = { consistencyNotes = it.take(1000) },
                    label = { Text("Character Consistency Notes") },
                    placeholder = { Text("Rules to keep identity locked across all scenes", color = StudioTextMuted) },
                    minLines = 2,
                    maxLines = 4,
                    colors = charDialogFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val saved = (initial ?: StudioCharacter()).copy(
                        charCode = charCode.trim().ifBlank { suggestedCharCode },
                        name = name.trim().ifBlank { "Unnamed Character" },
                        role = role.trim(),
                        visualAge = visualAge.trim(),
                        faceDescription = faceDescription.trim(),
                        hairStyle = hairStyle.trim(),
                        hairColor = hairColor.trim(),
                        clothing = clothing.trim(),
                        clothingColors = clothingColors.trim(),
                        wardrobePalette = clothingColors.trim(),
                        bodyProportions = bodyProportions.trim(),
                        accessories = accessories.trim(),
                        personality = personality.trim(),
                        expressionDefault = expressionDefault,
                        visualStyle = visualStyle,
                        appearanceNotes = consistencyNotes.trim(),
                        accentColorHex = accentColorHex
                    )
                    onSave(saved)
                    onDismiss()
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = StudioAmber, contentColor = StudioVoid),
                modifier = Modifier.testTag("save_character_profile_button")
            ) {
                Text("Save Profile", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = StudioTextSecondary)
            }
        }
    )
}

@Composable
fun CharacterDesignGeneratorDialog(
    character: StudioCharacter,
    references: List<CharacterReferenceSheet>,
    genState: CharacterImageGenState,
    onDismiss: () -> Unit,
    onGenerate: (
        viewType: String,
        expression: String,
        poseAction: String,
        visualStyle: String,
        useAnchorReference: Boolean,
        setAsPrimary: Boolean
    ) -> Unit,
    onSetPrimary: (String) -> Unit,
    onDeleteReference: (String) -> Unit
) {
    var selectedView by remember(character.id) { mutableStateOf(StudioPresets.characterViews.first()) }
    var selectedExpression by remember(character.id) {
        mutableStateOf(
            character.expressionDefault.takeIf { it in StudioPresets.characterEmotions } ?: "Calm"
        )
    }
    var selectedAction by remember(character.id) { mutableStateOf("Standing") }
    var selectedStyle by remember(character.id) {
        mutableStateOf(
            character.visualStyle.takeIf { it in StudioPresets.characterVisualStyles }
                ?: StudioPresets.characterVisualStyles.first()
        )
    }
    val hasPrimaryRef = character.referenceImageBase64.isNotBlank()
    var useAnchorReference by remember(character.id, hasPrimaryRef) { mutableStateOf(hasPrimaryRef) }
    var setAsPrimary by remember(character.id, hasPrimaryRef) { mutableStateOf(!hasPrimaryRef) }

    val isGenerating = genState is CharacterImageGenState.Generating
    val accent = parseHexColor(character.accentColorHex, StudioAmber)

    AlertDialog(
        onDismissRequest = { if (!isGenerating) onDismiss() },
        containerColor = StudioPanel,
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = StudioAmber, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = "2D Character Design Studio",
                        style = MaterialTheme.typography.headlineSmall,
                        color = StudioTextPrimary
                    )
                }
                Text(
                    text = "${character.charCode} · ${character.name}",
                    style = MaterialTheme.typography.labelMedium,
                    color = accent
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // Current Primary Identity Preview
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StudioCharcoal, RoundedCornerShape(8.dp))
                        .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    CharacterAvatarThumbnail(
                        character = character,
                        size = 56.dp,
                        borderColor = accent,
                        shape = RoundedCornerShape(7.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (hasPrimaryRef) "Identity Anchor Ready" else "No Primary Reference Yet",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (hasPrimaryRef) StudioTeal else StudioAmber
                        )
                        Text(
                            text = character.appearanceNotes.ifBlank {
                                "${character.faceDescription} ${character.clothing}".trim()
                                    .ifBlank { "Uses saved 16-field profile attributes for consistent 2D character generation." }
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                            color = StudioTextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // 1. View & Framing Selector (5 views)
                Text("VIEW & FRAMING", style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    StudioPresets.characterViews.forEach { view ->
                        FilterChip(
                            selected = selectedView == view,
                            onClick = { selectedView = view },
                            label = { Text(view, style = MaterialTheme.typography.labelSmall) },
                            colors = charDialogChipColors()
                        )
                    }
                }

                // 2. Facial Expression Selector (11 emotions)
                Text("FACIAL EXPRESSION", style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    StudioPresets.characterEmotions.forEach { exp ->
                        FilterChip(
                            selected = selectedExpression == exp,
                            onClick = { selectedExpression = exp },
                            label = { Text(exp, style = MaterialTheme.typography.labelSmall) },
                            colors = charDialogChipColors()
                        )
                    }
                }

                // 3. Body Pose / Action Selector (16 actions)
                Text("BODY POSE / ACTION", style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    StudioPresets.characterActions.forEach { action ->
                        FilterChip(
                            selected = selectedAction == action,
                            onClick = { selectedAction = action },
                            label = { Text(action, style = MaterialTheme.typography.labelSmall) },
                            colors = charDialogChipColors()
                        )
                    }
                }

                // 4. Visual Style Selector
                Text("DOCUMENTARY VISUAL STYLE", style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    StudioPresets.characterVisualStyles.forEach { style ->
                        FilterChip(
                            selected = selectedStyle == style,
                            onClick = { selectedStyle = style },
                            label = { Text(style, style = MaterialTheme.typography.labelSmall) },
                            colors = charDialogChipColors()
                        )
                    }
                }

                // Identity Consistency Options
                if (hasPrimaryRef) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Lock Visual Identity to Primary Ref", style = MaterialTheme.typography.labelMedium, color = StudioTextPrimary)
                            Text("Reuses primary reference image to preserve exact face & outfit", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), color = StudioTextMuted)
                        }
                        Switch(
                            checked = useAnchorReference,
                            onCheckedChange = { useAnchorReference = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = StudioVoid, checkedTrackColor = StudioTeal)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Set as Primary Character Preview", style = MaterialTheme.typography.labelMedium, color = StudioTextPrimary)
                        Text("Displays on library card and 16:9 stage canvas", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), color = StudioTextMuted)
                    }
                    Switch(
                        checked = setAsPrimary,
                        onCheckedChange = { setAsPrimary = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = StudioVoid, checkedTrackColor = StudioAmber)
                    )
                }

                // Real Generation Status / Error Banner (zero-mock transparency)
                when (genState) {
                    is CharacterImageGenState.Generating -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = StudioAmberDim,
                            border = BorderStroke(1.dp, StudioAmber.copy(alpha = 0.7f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = StudioAmber,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "Generating ${genState.viewType} illustration for ${genState.characterName} via Firebase AI Logic... This may take 30–60s.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StudioTextPrimary
                                )
                            }
                        }
                    }
                    is CharacterImageGenState.Error -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = StudioCoralDim,
                            border = BorderStroke(1.dp, StudioCoral),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = genState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = StudioCoral,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                    is CharacterImageGenState.Success -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = StudioTealDim,
                            border = BorderStroke(1.dp, StudioTeal),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Saved ${genState.viewType} reference sheet to cloud library.",
                                style = MaterialTheme.typography.bodySmall,
                                color = StudioEmerald,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                    CharacterImageGenState.Idle -> Unit
                }

                // Generated Turnaround Gallery Preview
                if (references.isNotEmpty()) {
                    Text(
                        text = "GENERATED SHEETS (${references.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = StudioTeal
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        references.forEach { sheet ->
                            val isPrimary = sheet.imageBase64 == character.referenceImageBase64
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = StudioCharcoal,
                                border = BorderStroke(1.dp, if (isPrimary) StudioAmber else StudioBorder),
                                modifier = Modifier.width(116.dp)
                            ) {
                                Column(modifier = Modifier.padding(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(StudioVoid)
                                    ) {
                                        CharacterReferenceImage(
                                            base64Image = sheet.imageBase64,
                                            contentDescription = sheet.viewType,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = sheet.viewType,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = StudioTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${sheet.expression} · ${sheet.poseAction}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                        color = StudioTextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        TextButton(
                                            onClick = { onSetPrimary(sheet.imageBase64) },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text(
                                                text = if (isPrimary) "Primary" else "Use",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                color = if (isPrimary) StudioAmber else StudioTeal
                                            )
                                        }
                                        IconButton(
                                            onClick = { onDeleteReference(sheet.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Delete sheet",
                                                tint = StudioCoral,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onGenerate(
                        selectedView,
                        selectedExpression,
                        selectedAction,
                        selectedStyle,
                        useAnchorReference && hasPrimaryRef,
                        setAsPrimary
                    )
                },
                enabled = !isGenerating,
                colors = ButtonDefaults.buttonColors(containerColor = StudioAmber, contentColor = StudioVoid),
                modifier = Modifier.testTag("run_generate_character_design")
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (isGenerating) "Generating..." else "Generate Character", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isGenerating) {
                Text("Close", color = StudioTextSecondary)
            }
        }
    )
}

@Composable
fun CharacterRenameDialog(
    character: StudioCharacter,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var updatedName by remember(character.id) { mutableStateOf(character.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioPanel,
        title = {
            Text("Rename ${character.charCode}", style = MaterialTheme.typography.headlineSmall, color = StudioTextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Permanent Character ID (${character.charCode}) and all saved references remain intact across scenes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioTextSecondary
                )
                OutlinedTextField(
                    value = updatedName,
                    onValueChange = { updatedName = it.take(100) },
                    label = { Text("New Character Name") },
                    singleLine = true,
                    colors = charDialogFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(updatedName.trim())
                    onDismiss()
                },
                enabled = updatedName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = StudioAmber, contentColor = StudioVoid)
            ) {
                Text("Rename", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = StudioTextSecondary)
            }
        }
    )
}

@Composable
fun ConfirmDeleteCharacterDialog(
    character: StudioCharacter,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioPanel,
        title = {
            Text(
                text = "Delete Character?",
                style = MaterialTheme.typography.headlineSmall,
                color = StudioCoral
            )
        },
        text = {
            Text(
                text = "Permanently delete ${character.charCode} · ${character.name} and all saved reference sheets from your library? This character will also be removed from assigned scenes.",
                style = MaterialTheme.typography.bodySmall,
                color = StudioTextPrimary
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmDelete()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = StudioCoral, contentColor = Color.White),
                modifier = Modifier.testTag("confirm_delete_character_button")
            ) {
                Text("Delete Character", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = StudioTextSecondary)
            }
        }
    )
}

@Composable
private fun charDialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = StudioCharcoal,
    unfocusedContainerColor = StudioCharcoal,
    focusedBorderColor = StudioAmber,
    unfocusedBorderColor = StudioBorder,
    focusedTextColor = StudioTextPrimary,
    unfocusedTextColor = StudioTextPrimary
)

@Composable
private fun charDialogChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = StudioAmberDim,
    selectedLabelColor = StudioAmber
)
