package com.aitranslator.ui.history

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.aitranslator.R
import com.aitranslator.databinding.ActivityHistoryBinding
import com.aitranslator.utils.copyToClipboard
import com.aitranslator.utils.hide
import com.aitranslator.utils.show
import com.aitranslator.viewmodel.HistoryViewModel

/**
 * History screen that shows all previously saved translations.
 *
 * Displays a [RecyclerView] driven by [HistoryAdapter] and observes
 * [HistoryViewModel.allHistory] to stay up-to-date in real time.
 */
class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private val viewModel: HistoryViewModel by viewModels()
    private lateinit var historyAdapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        observeHistory()
    }

    // -------------------------------------------------------------------------
    // Options menu (clear all)
    // -------------------------------------------------------------------------

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_history, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            R.id.action_clear_all -> {
                confirmClearAll()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // -------------------------------------------------------------------------
    // Setup helpers
    // -------------------------------------------------------------------------

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.title_history)
        }
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(
            onDeleteClick = { item ->
                viewModel.deleteItem(item)
            },
            onCopyClick = { item ->
                copyToClipboard(item.translatedText, "Translation")
            }
        )

        binding.recyclerViewHistory.apply {
            layoutManager = LinearLayoutManager(this@HistoryActivity)
            adapter = historyAdapter
            // Smooth item animations
            itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()
        }
    }

    // -------------------------------------------------------------------------
    // ViewModel observer
    // -------------------------------------------------------------------------

    private fun observeHistory() {
        viewModel.allHistory.observe(this) { historyList ->
            if (historyList.isNullOrEmpty()) {
                binding.recyclerViewHistory.hide()
                binding.layoutEmpty.show()
            } else {
                binding.recyclerViewHistory.show()
                binding.layoutEmpty.hide()
                historyAdapter.submitList(historyList)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Confirm dialog for clearing all history
    // -------------------------------------------------------------------------

    private fun confirmClearAll() {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_clear_title)
            .setMessage(R.string.dialog_clear_message)
            .setPositiveButton(R.string.dialog_clear_confirm) { _, _ ->
                viewModel.clearAll()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
