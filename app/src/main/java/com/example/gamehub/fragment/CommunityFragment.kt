package com.example.gamehub.fragment

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.gamehub.GroupChatActivity
import com.example.gamehub.R
import com.example.gamehub.models.CommunityServer
import com.example.gamehub.models.CreateServerRequest
import com.example.gamehub.network.RetrofitInstance
import com.example.gamehub.network.TokenManager
import kotlinx.coroutines.launch

class CommunityFragment : Fragment() {

    private lateinit var containerServers: LinearLayout
    private var allServers: List<CommunityServer> = emptyList()
    private lateinit var textError: TextView
    private lateinit var editSearchServer: EditText
    private var currentTab = "my_groups"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_community, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        containerServers = view.findViewById(R.id.containerServers)
        textError = view.findViewById(R.id.textError)
        editSearchServer = view.findViewById(R.id.editSearchServer)

        val buttonTabMyGroups = view.findViewById<Button>(R.id.buttonTabMyGroups)
        val buttonTabDiscover = view.findViewById<Button>(R.id.buttonTabDiscover)

        buttonTabMyGroups.setOnClickListener {
            currentTab = "my_groups"
            editSearchServer.visibility = View.GONE
            renderServers()
        }

        buttonTabDiscover.setOnClickListener {
            currentTab = "discover"
            editSearchServer.visibility = View.VISIBLE
            renderServers()
        }

        editSearchServer.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                renderServers()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        loadServers()
    }

    private fun loadServers() {
        lifecycleScope.launch {
            try {
                allServers = RetrofitInstance.api.getServers()
                Log.d("GameHub", "Loaded ${allServers.size} total servers")
                renderServers()
            } catch (e: Exception) {
                Log.e("GameHub", "Load servers failed: ${e.message}")
            }
        }
    }

    private fun renderServers() {
        val myUserId = TokenManager.getUserId() ?: return
        containerServers.removeAllViews()

        if (currentTab == "my_groups") {
            val myServers = allServers.filter { it.userIds.contains(myUserId) }

            if (myServers.isEmpty()) {
                showEmptyState()
            } else {
                for (server in myServers) {
                    val itemView = layoutInflater.inflate(
                        android.R.layout.simple_list_item_1,
                        containerServers,
                        false
                    )
                    itemView.findViewById<TextView>(android.R.id.text1).text = server.name
                    itemView.setOnClickListener {
                        openGroupChat(server)
                    }
                    containerServers.addView(itemView)
                }
            }

        } else {
            val searchQuery = editSearchServer.text.toString()
            val availableServers = allServers.filter { !it.userIds.contains(myUserId) }
                .filter { it.name.contains(searchQuery, ignoreCase = true) }

            if (availableServers.isEmpty()) {
                val emptyText = TextView(requireContext())
                emptyText.text = "No groups found"
                emptyText.setPadding(12, 24, 12, 12)
                containerServers.addView(emptyText)
            }

            for (server in availableServers) {
                val itemView = layoutInflater.inflate(
                    android.R.layout.simple_list_item_2,
                    containerServers,
                    false
                )
                itemView.findViewById<TextView>(android.R.id.text1).text = server.name
                itemView.findViewById<TextView>(android.R.id.text2).text =
                    "${server.userIds.size} members - Tap to join"
                itemView.setOnClickListener {
                    joinServer(server)
                }
                containerServers.addView(itemView)
            }
        }
    }

    private fun showEmptyState() {
        val emptyView = layoutInflater.inflate(R.layout.item_empty_community, containerServers, false)
        emptyView.findViewById<Button>(R.id.buttonCreateGroup).setOnClickListener {
            showCreateGroupDialog()
        }
        containerServers.addView(emptyView)
    }

    private fun showCreateGroupDialog() {
        val input = EditText(requireContext())
        input.hint = "Group name"

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Create a Group Chat")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    createServer(name)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createServer(name: String) {
        val myUserId = TokenManager.getUserId()
        if (myUserId == null) {
            textError.text = "You are not logged in"
            return
        }

        lifecycleScope.launch {
            try {
                val request = CreateServerRequest(name, "A community server", listOf(myUserId))
                RetrofitInstance.api.createServer(request)
                textError.text = ""
                Log.d("GameHub", "Server created: $name")
                loadServers()
            } catch (e: Exception) {
                Log.e("GameHub", "Create server failed: ${e.message}")
                textError.text = "Failed to create server"
            }
        }
    }

    private fun joinServer(server: CommunityServer) {
        val myUserId = TokenManager.getUserId() ?: return

        lifecycleScope.launch {
            try {
                RetrofitInstance.api.joinServer(server.id, myUserId)
                Log.d("GameHub", "Joined server: ${server.name}")
                loadServers()
            } catch (e: Exception) {
                Log.e("GameHub", "Join server failed: ${e.message}")
            }
        }
    }

    private fun openGroupChat(server: CommunityServer) {
        val intent = Intent(requireContext(), GroupChatActivity::class.java)
        intent.putExtra("server_id", server.id)
        intent.putExtra("server_name", server.name)
        startActivity(intent)
    }
}