package com.example.gamehub.fragment

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gamehub.GameDetailsActivity
import com.example.gamehub.R
import com.example.gamehub.adapters.SearchGameAdapter
import com.example.gamehub.models.Game
import com.example.gamehub.network.RetrofitInstance
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {

    private lateinit var adapter: SearchGameAdapter
    private var searchJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val searchInput = view.findViewById<EditText>(R.id.searchInput)
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerSearch)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        adapter = SearchGameAdapter(emptyList()) { game ->
            val gameJson = RetrofitInstance.moshi.adapter(Game::class.java).toJson(game)
            val intent = Intent(requireContext(), GameDetailsActivity::class.java)
            intent.putExtra("game_json", gameJson)
            startActivity(intent)
        }
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val keyword = s.toString().trim()
                if (keyword.length < 2) return

                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(500)
                    progressBar.visibility = View.VISIBLE
                    try {
                        val results = RetrofitInstance.api.searchGames(keyword)
                        adapter.updateData(results)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        progressBar.visibility = View.GONE
                    }
                }
            }
        })

        searchInput.requestFocus()

        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(searchInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }
}