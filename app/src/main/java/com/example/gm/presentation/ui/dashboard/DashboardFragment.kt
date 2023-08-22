package com.example.gm.presentation.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.GuardedBy
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.gm.R
import com.example.gm.common.Constants.getSolanaExplorerUrl
import com.example.gm.databinding.FragmentDashboardBinding
import com.example.gm.presentation.ui.extensions.copyToClipboard
import com.example.gm.presentation.ui.extensions.openInBrowser
import com.example.gm.presentation.ui.extensions.showSnackbar
import com.example.gm.presentation.ui.extensions.showSnackbarWithAction
import com.example.gm.presentation.utils.Nft
import com.example.gm.presentation.utils.StartActivityForResultSender
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()

    private val activityResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            intentSender.onActivityComplete()
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.fetchNftList()

        observeViewModel()
    }


    private fun observeViewModel() {
        lifecycleScope.launch {
            with(viewModel) {
                uiState.collect { uiState ->
                    uiState.nftList?.let {
                        val dataList = it.results
                        val adapter = NftAdapter(requireContext(), dataList) // Replace with your data
                        val spanCount = 2 // Number of columns in the grid
                        val layoutManager = GridLayoutManager(context, spanCount)

                        binding.nftRecyclerView.layoutManager = layoutManager
                        binding.nftRecyclerView.adapter = adapter

                        adapter.setOnItemClickListener(object : NftAdapter.OnItemClickListener {
                            override fun onItemClick(nft: Nft) {
                                val bundle = bundleOf("projectId" to nft.projectId, "nftId" to nft.id)
                                requireView().findNavController().navigate(R.id.action_navigation_dashboard_to_nftFragment, bundle)
                            }
                        })
                    }
                    uiState.isLoading.let {
                        binding.progressBar.visibility = if (it) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    private val intentSender = object : StartActivityForResultSender {
        @GuardedBy("this")
        private var callback: (() -> Unit)? = null

        override fun startActivityForResult(
            intent: Intent,
            onActivityCompleteCallback: () -> Unit,
        ) {
            synchronized(this) {
                check(callback == null) {
                    "Received an activity start request while another is pending"
                }
                callback = onActivityCompleteCallback
            }
            activityResultLauncher.launch(intent)
        }

        fun onActivityComplete() {
            synchronized(this) {
                callback?.let { it() }
                callback = null
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
