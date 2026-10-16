package com.sally.zlauncher

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sally.zlauncher.core.ApkImporter
import com.sally.zlauncher.core.LaunchController
import com.sally.zlauncher.core.LaunchLog
import com.sally.zlauncher.core.McInstallState
import com.sally.zlauncher.core.PlayStoreValidator
import com.sally.zlauncher.data.GameVersion
import com.sally.zlauncher.data.VersionStore
import com.sally.zlauncher.databinding.ActivityMainBinding
import com.sally.zlauncher.ui.VersionAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var store: VersionStore
    private lateinit var controller: LaunchController
    private lateinit var adapter: VersionAdapter

    private val pickFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importUri(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        store = VersionStore(this)
        controller = LaunchController(applicationContext, store)
        adapter = VersionAdapter(onLaunch = ::onLaunch, onDelete = ::onDelete)
        binding.versionList.layoutManager = LinearLayoutManager(this)
        binding.versionList.adapter = adapter
        binding.btnImport.setOnClickListener { onImportClicked() }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val state = PlayStoreValidator.state(this)
        binding.tvStatus.setText(
            when (state) {
                McInstallState.NOT_INSTALLED -> R.string.status_not_installed
                McInstallState.PLAY_STORE -> R.string.status_play_store
                McInstallState.OTHER_SOURCE -> R.string.status_other_source
            }
        )

        val items = ArrayList<GameVersion>()
        if (state != McInstallState.NOT_INSTALLED) {
            items.add(
                GameVersion(
                    id = GameVersion.INSTALLED_ID,
                    displayName = getString(R.string.installed_entry_name),
                    versionName = PlayStoreValidator.installedVersionName(this) ?: "?",
                    isInstalled = true
                )
            )
        }
        items.addAll(store.list())
        adapter.submitList(items)
        binding.emptyView.isVisible = items.isEmpty()
    }

    private fun onImportClicked() {
        if (PlayStoreValidator.state(this) != McInstallState.PLAY_STORE) {
            showMessage(getString(R.string.license_title), getString(R.string.license_msg))
            return
        }
        pickFile.launch(arrayOf("*/*"))
    }

    private fun importUri(uri: Uri) {
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.importing)
            .setMessage(R.string.please_wait)
            .setCancelable(false)
            .show()

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { ApkImporter(applicationContext, store).import(uri) }
            }
            dialog.dismiss()
            result.onSuccess { version ->
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.import_done, version.versionName),
                    Toast.LENGTH_LONG
                ).show()
                refresh()
            }.onFailure { error ->
                showMessage(
                    getString(R.string.import_failed),
                    error.message ?: error.javaClass.simpleName
                )
            }
        }
    }

    private fun onLaunch(version: GameVersion) {
        binding.btnImport.isEnabled = false
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { controller.launch(version) }
            binding.btnImport.isEnabled = true

            val intent = result.intent
            if (result.ok && intent != null) {
                startActivity(intent)
                return@launch
            }
            val title = getString(
                if (result.ok) R.string.launch_prepared_title else R.string.launch_failed_title
            )
            showMessage(title, result.message + "\n\n" + LaunchLog.dump())
        }
    }

    private fun onDelete(version: GameVersion) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_title)
            .setMessage(getString(R.string.delete_message, version.displayName))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                store.delete(version.id)
                refresh()
            }
            .show()
    }

    private fun showMessage(title: String, message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.ok, null)
            .show()
    }
}
