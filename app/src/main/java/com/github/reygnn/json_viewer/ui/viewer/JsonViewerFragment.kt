package com.github.reygnn.json_viewer.ui.viewer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.reygnn.json_viewer.R
import com.github.reygnn.json_viewer.databinding.FragmentJsonViewerBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

@AndroidEntryPoint
class JsonViewerFragment : Fragment() {

    private var _binding: FragmentJsonViewerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: JsonViewerViewModel by viewModels()

    // NEU: Der Adapter für die Syntax-Highlighting Liste
    private val jsonAdapter = JsonAdapter()

    // Der moderne Weg, den SAF-Datei-Picker zu starten
    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.loadJsonFromUri(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJsonViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupWindowInsets()
        setupRecyclerView() // NEU: RecyclerView initialisieren
        setupListeners()
        observeViewModel()
    }

    private fun setupWindowInsets() {
        // Stellt sicher, dass die UI (vor allem die App Bar) nicht mit den System-Bars überlappt
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // NEU: RecyclerView Konfiguration
    private fun setupRecyclerView() {
        binding.jsonRecyclerView.apply {
            adapter = jsonAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true) // Performance-Boost, da Zeilenhöhe fix sein kann
        }
    }

    private fun setupListeners() {
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_open_file -> {
                    openJsonFile()
                    true
                }
                else -> false
            }
        }
    }

    private fun openJsonFile() {
        // Startet den SAF-Picker und filtert nach JSON-Dateien
        openDocumentLauncher.launch(arrayOf("application/json"))
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUi(state)
                }
            }
        }
    }

    private fun updateUi(state: JsonViewerUiState) {
        binding.progressBar.isVisible = state.isLoading

        // NEU: RecyclerView Sichtbarkeit steuern (statt ScrollView)
        // Zugriff auf binding.jsonRecyclerView setzt voraus, dass du die ID im XML geändert hast!
        binding.jsonRecyclerView.isVisible = state.jsonData != null && !state.isLoading
        binding.emptyView.isVisible = state.jsonData == null && !state.isLoading

        state.jsonData?.let { data ->
            // NEU: Daten an den Adapter übergeben (Liste von Strings)
            jsonAdapter.submitList(data.jsonLines)

            binding.toolbar.title = data.fileName
            binding.toolbar.subtitle = formatFileSize(data.fileSize)
        } ?: run {
            // Wenn keine Daten da sind, Reset
            if (!state.isLoading) {
                binding.toolbar.title = getString(R.string.app_name)
                binding.toolbar.subtitle = null
                jsonAdapter.submitList(emptyList()) // Liste leeren
            }
        }

        // Fehler-Handling
        state.errorMessage?.let { message ->
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
                .setAnchorView(binding.bottomGuideline)
                .show()
            viewModel.onErrorShown()
        }
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "${(kb * 10).roundToLong() / 10.0} KB"
        val mb = kb / 1024.0
        return "${(mb * 10).roundToLong() / 10.0} MB"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Adapter-Referenz muss hier nicht gelöscht werden, aber Binding schon
        _binding = null
    }
}