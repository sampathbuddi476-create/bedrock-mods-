package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Base64
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BedrockMod
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.model.OpenSourceMod
import com.example.data.repository.OpenSourceModsCatalog
import com.example.data.repository.ModRepository
import com.example.service.exporter.BedrockExporter
import com.example.service.gemini.GeminiService
import com.example.service.validator.BedrockAddonValidator
import com.example.service.validator.ValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.util.UUID

enum class AppScreen {
    LIBRARY,
    MODS,
    CHAT,
    EDITOR,
    DOWNLOADS
}

sealed class PendingExport {
    data class TestInMinecraft(val mod: BedrockMod) : PendingExport()
    data class ExportToUri(val treeUri: Uri, val mod: BedrockMod) : PendingExport()
}

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ModRepository(application)
    val savedMods: StateFlow<List<BedrockMod>> = repository.mods
    val chatSessions: StateFlow<List<ChatSession>> = repository.chatSessions

    // Open Source Mods Library is default screen on launch as requested
    private val _currentScreen = MutableStateFlow(AppScreen.LIBRARY)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Active Chat Session State (Gemini App-like history & switching)
    private val _activeSessionId = MutableStateFlow(UUID.randomUUID().toString())
    val activeSessionId: StateFlow<String> = _activeSessionId.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _selectedModel = MutableStateFlow(GeminiService.MODEL_FLASH)
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _isHighThinkingEnabled = MutableStateFlow(false)
    val isHighThinkingEnabled: StateFlow<Boolean> = _isHighThinkingEnabled.asStateFlow()

    private val _isSearchGroundingEnabled = MutableStateFlow(false)
    val isSearchGroundingEnabled: StateFlow<Boolean> = _isSearchGroundingEnabled.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    // Mod Editor State
    private val _activeMod = MutableStateFlow<BedrockMod?>(null)
    val activeMod: StateFlow<BedrockMod?> = _activeMod.asStateFlow()

    private val _selectedFilePath = MutableStateFlow<String?>(null)
    val selectedFilePath: StateFlow<String?> = _selectedFilePath.asStateFlow()

    private val _fileEditorContent = MutableStateFlow("")
    val fileEditorContent: StateFlow<String> = _fileEditorContent.asStateFlow()

    // Video Analysis State
    private val _showVideoDialog = MutableStateFlow(false)
    val showVideoDialog: StateFlow<Boolean> = _showVideoDialog.asStateFlow()

    // Validation State
    private val _validationResult = MutableStateFlow<ValidationResult?>(null)
    val validationResult: StateFlow<ValidationResult?> = _validationResult.asStateFlow()

    private val _validationPendingMod = MutableStateFlow<BedrockMod?>(null)
    val validationPendingMod: StateFlow<BedrockMod?> = _validationPendingMod.asStateFlow()

    private val _pendingExport = MutableStateFlow<PendingExport?>(null)
    val pendingExport: StateFlow<PendingExport?> = _pendingExport.asStateFlow()

    private val _showValidationDialog = MutableStateFlow(false)
    val showValidationDialog: StateFlow<Boolean> = _showValidationDialog.asStateFlow()

    // Feedback message
    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    init {
        // Initialize default chat session
        val defaultMessages = listOf(
            ChatMessage(
                role = "model",
                content = """
👋 **Welcome to Bedrock Studio!** I'm your dedicated Minecraft Bedrock Modding Assistant.

I can create and configure complete **Behavior Packs (BP)**, **Resource Packs (RP)**, and **Bedrock Script API (`@minecraft/server`)** mods for Minecraft Bedrock 1.20 - 1.21+.

✨ **What would you like to build?**
• "Create a Ruby Dagger with custom damage and crafting recipe"
• "Make a Frost Katana that freezes mobs with Script API"
• "Make a Void Dragon boss mob with custom attacks and loot"
• "Browse and download verified addons in the Open Source Mods Library"

Every generated mod is **strictly validated against Mojang Bedrock requirements** before 1-tap game folder export!
                """.trimIndent(),
                modelUsed = GeminiService.MODEL_FLASH
            )
        )
        _chatMessages.value = defaultMessages

        // Create or load session
        val initialSessions = repository.chatSessions.value
        if (initialSessions.isNotEmpty()) {
            val first = initialSessions.first()
            _activeSessionId.value = first.id
            if (first.messages.isNotEmpty()) {
                _chatMessages.value = first.messages
            }
        } else {
            val newSession = ChatSession(
                id = _activeSessionId.value,
                title = "Welcome to Bedrock Studio",
                messages = defaultMessages
            )
            repository.saveChatSessions(listOf(newSession))
        }
    }

    // --- Gemini App Chat Switching & History Management ---

    fun createNewChatSession() {
        val newId = UUID.randomUUID().toString()
        val welcomeMsg = ChatMessage(
            role = "model",
            content = "✨ New session started. What Minecraft Bedrock mod, weapon, entity, or script would you like to create?",
            modelUsed = _selectedModel.value
        )
        val newSession = ChatSession(
            id = newId,
            title = "New Mod Creation",
            messages = listOf(welcomeMsg)
        )
        _activeSessionId.value = newId
        _chatMessages.value = listOf(welcomeMsg)

        val updated = listOf(newSession) + repository.chatSessions.value
        repository.saveChatSessions(updated)
        showBanner("Started new Gemini chat session.")
    }

    fun switchChatSession(sessionId: String) {
        val session = repository.chatSessions.value.find { it.id == sessionId } ?: return
        _activeSessionId.value = session.id
        _chatMessages.value = session.messages
        showBanner("Switched to \"${session.title}\"")
    }

    fun deleteChatSession(sessionId: String) {
        val current = repository.chatSessions.value.filterNot { it.id == sessionId }
        repository.saveChatSessions(current)
        if (_activeSessionId.value == sessionId) {
            if (current.isNotEmpty()) {
                switchChatSession(current.first().id)
            } else {
                createNewChatSession()
            }
        }
        showBanner("Chat deleted.")
    }

    private fun persistCurrentSessionMessages(newMessages: List<ChatMessage>) {
        val currentSessions = repository.chatSessions.value.toMutableList()
        val index = currentSessions.indexOfFirst { it.id == _activeSessionId.value }
        val derivedTitle = newMessages.firstOrNull { it.role == "user" }?.content?.take(28)?.trim() ?: "Mod Creation"

        if (index >= 0) {
            val updated = currentSessions[index].copy(
                title = if (currentSessions[index].title == "New Mod Creation" || currentSessions[index].title == "Welcome to Bedrock Studio") derivedTitle else currentSessions[index].title,
                messages = newMessages,
                lastModified = System.currentTimeMillis()
            )
            currentSessions[index] = updated
        } else {
            currentSessions.add(
                0,
                ChatSession(
                    id = _activeSessionId.value,
                    title = derivedTitle,
                    messages = newMessages,
                    lastModified = System.currentTimeMillis()
                )
            )
        }
        repository.saveChatSessions(currentSessions)
    }

    fun setScreen(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun setSelectedModel(model: String) {
        _selectedModel.value = model
        if (model != GeminiService.MODEL_PRO) {
            _isHighThinkingEnabled.value = false
        }
    }

    fun toggleHighThinking() {
        if (_selectedModel.value == GeminiService.MODEL_PRO) {
            _isHighThinkingEnabled.value = !_isHighThinkingEnabled.value
        } else {
            _selectedModel.value = GeminiService.MODEL_PRO
            _isHighThinkingEnabled.value = true
        }
    }

    fun toggleSearchGrounding() {
        _isSearchGroundingEnabled.value = !_isSearchGroundingEnabled.value
        if (_isSearchGroundingEnabled.value && _selectedModel.value != GeminiService.MODEL_FLASH) {
            _selectedModel.value = GeminiService.MODEL_FLASH
        }
    }

    fun setShowVideoDialog(show: Boolean) {
        _showVideoDialog.value = show
    }

    fun dismissValidationDialog() {
        _showValidationDialog.value = false
        _validationResult.value = null
        _validationPendingMod.value = null
        _pendingExport.value = null
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank() || _isGenerating.value) return

        // Check for built-in versioning commands in chat
        if (handleVersioningChatCommand(userText)) return

        val userMsg = ChatMessage(
            role = "user",
            content = userText,
            modelUsed = _selectedModel.value
        )
        val updatedHistory = _chatMessages.value + userMsg
        _chatMessages.value = updatedHistory
        persistCurrentSessionMessages(updatedHistory)
        _isGenerating.value = true

        viewModelScope.launch {
            val response = GeminiService.sendChat(
                history = updatedHistory,
                userPrompt = userText,
                modelName = _selectedModel.value,
                enableHighThinking = _isHighThinkingEnabled.value,
                enableSearchGrounding = _isSearchGroundingEnabled.value
            )

            val modelMsg = ChatMessage(
                role = "model",
                content = response.text,
                modelUsed = _selectedModel.value,
                thinkingProcess = response.thinking,
                groundingSources = response.groundingSources,
                generatedMod = response.generatedMod
            )

            val finalMessages = _chatMessages.value + modelMsg
            _chatMessages.value = finalMessages
            persistCurrentSessionMessages(finalMessages)
            _isGenerating.value = false

            response.generatedMod?.let { mod ->
                repository.saveMod(mod)
                showBanner("✨ Created \"${mod.name}\"! Ready for 1-Tap Minecraft import.")
            }
        }
    }

    private fun handleVersioningChatCommand(cmd: String): Boolean {
        val trimmed = cmd.trim().lowercase()
        val active = _activeMod.value ?: savedMods.value.firstOrNull()

        if (trimmed.startsWith("/version save") || trimmed.startsWith("save version")) {
            if (active != null) {
                val note = cmd.substringAfter("version").substringAfter("save").trim()
                val updated = createVersionSnapshot(active.id, note.ifBlank { "Chat checkpoint" })
                val resp = ChatMessage(
                    role = "model",
                    content = "✅ **Saved Version Snapshot!**\nMod **${active.name}** updated to version **${updated?.version}** with note: \"${note.ifBlank { "Chat checkpoint" }}\"."
                )
                _chatMessages.value = _chatMessages.value + ChatMessage(role = "user", content = cmd) + resp
                persistCurrentSessionMessages(_chatMessages.value)
                return true
            }
        } else if (trimmed.startsWith("duplicate mod") || trimmed.startsWith("/duplicate")) {
            if (active != null) {
                val newName = cmd.substringAfter("as").trim()
                val forked = duplicateMod(active.id, newName.ifBlank { "${active.name} (Copy)" })
                val resp = ChatMessage(
                    role = "model",
                    content = "📋 **Mod Duplicated!** Created independent clone **${forked?.name}** with fresh Bedrock UUIDs."
                )
                _chatMessages.value = _chatMessages.value + ChatMessage(role = "user", content = cmd) + resp
                persistCurrentSessionMessages(_chatMessages.value)
                return true
            }
        }
        return false
    }

    fun analyzeVideoConcept(prompt: String, videoUri: Uri?, videoUrl: String?) {
        val userPrompt = if (!videoUrl.isNullOrBlank()) {
            "Analyze this Minecraft video concept: $videoUrl\n$prompt\nCreate a complete Bedrock Addon based on the mechanics shown in this video."
        } else {
            "Analyze video and generate a Bedrock Addon based on it:\n$prompt"
        }

        setSelectedModel(GeminiService.MODEL_PRO)
        setShowVideoDialog(false)

        val userMsg = ChatMessage(
            role = "user",
            content = userPrompt,
            modelUsed = GeminiService.MODEL_PRO,
            isVideoAnalysis = true
        )
        _chatMessages.value = _chatMessages.value + userMsg
        _isGenerating.value = true

        viewModelScope.launch {
            var videoBase64: String? = null
            var mimeType = "video/mp4"

            if (videoUri != null) {
                try {
                    val cr = getApplication<Application>().contentResolver
                    mimeType = cr.getType(videoUri) ?: "video/mp4"
                    cr.openInputStream(videoUri)?.use { stream ->
                        val bytes = stream.readBytes()
                        if (bytes.size < 15 * 1024 * 1024) {
                            videoBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val response = GeminiService.sendChat(
                history = _chatMessages.value,
                userPrompt = userPrompt,
                modelName = GeminiService.MODEL_PRO,
                enableHighThinking = _isHighThinkingEnabled.value,
                enableSearchGrounding = false,
                videoBase64 = videoBase64,
                videoMimeType = mimeType
            )

            val modelMsg = ChatMessage(
                role = "model",
                content = response.text,
                modelUsed = GeminiService.MODEL_PRO,
                thinkingProcess = response.thinking,
                groundingSources = response.groundingSources,
                generatedMod = response.generatedMod,
                isVideoAnalysis = true
            )

            val finalMessages = _chatMessages.value + modelMsg
            _chatMessages.value = finalMessages
            persistCurrentSessionMessages(finalMessages)
            _isGenerating.value = false

            response.generatedMod?.let { mod ->
                repository.saveMod(mod)
                showBanner("✨ Video analyzed! Created \"${mod.name}\" addon.")
            }
        }
    }

    // --- Mod Versioning & Management System ---

    fun createVersionSnapshot(modId: String, changeNote: String): BedrockMod? {
        val updated = repository.createVersionSnapshot(modId, changeNote)
        if (updated != null && _activeMod.value?.id == modId) {
            _activeMod.value = updated
        }
        showBanner("Saved version snapshot ${updated?.version}")
        return updated
    }

    fun revertToSnapshot(modId: String, snapshotIndex: Int): BedrockMod? {
        val updated = repository.revertToSnapshot(modId, snapshotIndex)
        if (updated != null && _activeMod.value?.id == modId) {
            _activeMod.value = updated
            val first = updated.files.keys.firstOrNull() ?: "items/custom_item.json"
            selectEditorFile(first)
        }
        showBanner("Reverted mod to snapshot #${snapshotIndex + 1}")
        return updated
    }

    fun duplicateMod(modId: String, newName: String): BedrockMod? {
        val dup = repository.duplicateMod(modId, newName)
        showBanner("Duplicated mod as \"${dup?.name}\"")
        return dup
    }

    fun openModInEditor(mod: BedrockMod) {
        _activeMod.value = mod
        val firstFile = mod.files.keys.firstOrNull() ?: "items/custom_item.json"
        _selectedFilePath.value = firstFile
        _fileEditorContent.value = mod.files[firstFile] ?: ""
        _currentScreen.value = AppScreen.EDITOR
    }

    fun selectEditorFile(path: String) {
        _selectedFilePath.value = path
        _fileEditorContent.value = _activeMod.value?.files?.get(path) ?: ""
    }

    fun updateEditorContent(content: String) {
        _fileEditorContent.value = content
    }

    fun saveCurrentEditorFile() {
        val mod = _activeMod.value ?: return
        val path = _selectedFilePath.value ?: return
        val updatedFiles = mod.files.toMutableMap()
        updatedFiles[path] = _fileEditorContent.value
        val updatedMod = mod.copy(files = updatedFiles)
        _activeMod.value = updatedMod
        repository.saveMod(updatedMod)
        showBanner("Saved changes to $path")
    }

    fun addNewFileToMod(fileName: String, initialTemplate: String) {
        val mod = _activeMod.value ?: return
        val updatedFiles = mod.files.toMutableMap()
        updatedFiles[fileName] = initialTemplate
        val updatedMod = mod.copy(files = updatedFiles)
        _activeMod.value = updatedMod
        repository.saveMod(updatedMod)
        selectEditorFile(fileName)
        showBanner("Added new file $fileName")
    }

    fun deleteFileFromMod(path: String) {
        val mod = _activeMod.value ?: return
        val updatedFiles = mod.files.toMutableMap()
        updatedFiles.remove(path)
        val updatedMod = mod.copy(files = updatedFiles)
        _activeMod.value = updatedMod
        repository.saveMod(updatedMod)
        val nextFile = updatedFiles.keys.firstOrNull()
        if (nextFile != null) {
            selectEditorFile(nextFile)
        } else {
            _selectedFilePath.value = null
            _fileEditorContent.value = ""
        }
        showBanner("Removed $path")
    }

    // Direct 1-Tap Import to Minecraft with Validation Layer
    fun testInMinecraft(mod: BedrockMod) {
        val validation = BedrockAddonValidator.validate(mod)
        if (!validation.isValid) {
            _validationResult.value = validation
            _validationPendingMod.value = mod
            _pendingExport.value = PendingExport.TestInMinecraft(mod)
            _showValidationDialog.value = true
            return
        }

        executeDirectMinecraftLaunch(mod)
    }

    private fun executeDirectMinecraftLaunch(mod: BedrockMod) {
        val context = getApplication<Application>()
        val result = BedrockExporter.openInMinecraft(context, mod)
        result.onSuccess { msg ->
            showBanner(msg)
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }.onFailure { err ->
            showBanner("Failed to launch Minecraft: ${err.message}")
            Toast.makeText(context, "Error: ${err.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Direct SAF Export to Minecraft or Games folder with Validation Layer
    fun exportToDirectoryUri(treeUri: Uri, mod: BedrockMod) {
        val validation = BedrockAddonValidator.validate(mod)
        if (!validation.isValid) {
            _validationResult.value = validation
            _validationPendingMod.value = mod
            _pendingExport.value = PendingExport.ExportToUri(treeUri, mod)
            _showValidationDialog.value = true
            return
        }

        executeSafExport(treeUri, mod)
    }

    private fun executeSafExport(treeUri: Uri, mod: BedrockMod) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            try {
                val cr = context.contentResolver
                val docId = DocumentsContract.getTreeDocumentId(treeUri)
                val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                val newDocUri = DocumentsContract.createDocument(
                    cr,
                    parentDocUri,
                    "application/octet-stream",
                    "${mod.name}.mcaddon"
                )
                if (newDocUri != null) {
                    val mcaddonFile = BedrockExporter.createMcaddonFile(context, mod)
                    cr.openOutputStream(newDocUri)?.use { out ->
                        mcaddonFile.inputStream().use { inp ->
                            inp.copyTo(out)
                        }
                    }
                    showBanner("✅ Exported ${mod.name}.mcaddon to game folder!")
                } else {
                    showBanner("Failed to create file in selected folder.")
                }
            } catch (e: Exception) {
                showBanner("Export error: ${e.message}")
            }
        }
    }

    fun autoFixAndProceed() {
        val mod = _validationPendingMod.value ?: return
        val fixedMod = BedrockAddonValidator.autoFix(mod)
        repository.saveMod(fixedMod)
        _activeMod.value = fixedMod

        val pending = _pendingExport.value
        dismissValidationDialog()

        when (pending) {
            is PendingExport.TestInMinecraft -> {
                executeDirectMinecraftLaunch(fixedMod)
            }
            is PendingExport.ExportToUri -> {
                executeSafExport(pending.treeUri, fixedMod)
            }
            null -> {
                showBanner("Auto-fixed manifest requirements!")
            }
        }
    }

    // Open Source Mods Library Direct Download System
    fun downloadOpenSourceMod(openSourceMod: OpenSourceMod, openInEditorAfter: Boolean = false) {
        val mod = openSourceMod.templateMod
        repository.saveMod(mod)
        val context = getApplication<Application>()
        viewModelScope.launch {
            try {
                BedrockExporter.createMcaddonFile(context, mod)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        showBanner("📥 Downloaded \"${openSourceMod.name}\" directly to app!")
        if (openInEditorAfter) {
            openModInEditor(mod)
        }
    }

    fun downloadAllStarterMods() {
        val context = getApplication<Application>()
        viewModelScope.launch {
            OpenSourceModsCatalog.curatedMods.forEach { item ->
                repository.saveMod(item.templateMod)
                try {
                    BedrockExporter.createMcaddonFile(context, item.templateMod)
                } catch (e: Exception) {
                    // ignore
                }
            }
            showBanner("🎉 All 10 Open Source Mods downloaded directly to app!")
        }
    }

    fun remixOpenSourceMod(openSourceMod: OpenSourceMod) {
        val prompt = "I want to remix and enhance the open-source mod \"${openSourceMod.name}\" (${openSourceMod.category}). Add new items, abilities, or gameplay mechanics to it."
        sendMessage(prompt)
        setScreen(AppScreen.CHAT)
    }

    fun importCustomMod(name: String, description: String, category: String, files: Map<String, String>) {
        val mod = BedrockMod(
            name = name,
            description = description,
            category = category,
            files = files
        )
        repository.saveMod(mod)
        val context = getApplication<Application>()
        viewModelScope.launch {
            try {
                BedrockExporter.createMcaddonFile(context, mod)
            } catch (e: Exception) {
                // ignore
            }
        }
        showBanner("✅ Imported custom mod \"$name\"!")
    }

    fun deleteMod(modId: String) {
        repository.deleteMod(modId)
        if (_activeMod.value?.id == modId) {
            _activeMod.value = null
            _currentScreen.value = AppScreen.MODS
        }
        showBanner("Mod removed.")
    }

    private fun showBanner(msg: String) {
        _statusBanner.value = msg
    }
}
